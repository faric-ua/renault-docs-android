#!/usr/bin/env python3
"""#162 stage 2 — add phone-exported volumes to the public catalog.

Inputs (all read-only):
  --catalog        current renault-docs-catalog.json (the published one)
  --report         RenaultDocs-<project>-<time>-export.json written by the app
                   («Експортувати томи окремо», schema 2+, v0.5.123+)
  --drive-listing  metadata of the Drive folder the phone exported into:
                   {"files": [{"id": "...", "title": "...", "fileSize": "123"}]}
Output:
  --output         a NEW catalog file (the current one is never modified)

Rules (owner, #162): one volume = one .rdpkg on Drive; a person downloads one or
a few. A volume enters the catalog only when
  * the phone verified the copy (status EXPORTED_SHA256_VERIFIED, sha256 read
    back through the Drive provider; v0.5.125: or EXISTING_SHA256_VERIFIED — the
    file was already in the folder from an earlier run and its sha256 matched),
  * exactly ONE Drive file has that exact name and the same size,
  * its package id / file name do not clash with a different published volume.
A same file name is never proof by itself (#143). Distinct NT codes stay
distinct (NT8340A ≠ NT8342A). Nothing is removed from the catalog. Any blocking
finding → no output file, exit 2.
"""
from __future__ import annotations

import argparse
import copy
import json
import re
import sys
from pathlib import Path
from typing import Any

SHA256 = re.compile(r"^[0-9a-f]{64}$")
DATE = re.compile(r"^\d{4}-\d{2}-\d{2}$")
VERIFIED = "EXPORTED_SHA256_VERIFIED"
# v0.5.125: a file left by an earlier (stopped) run, re-read and matched by sha256.
VERIFIED_STATUSES = (VERIFIED, "EXISTING_SHA256_VERIFIED")


class Finding(dict):
    pass


def _finding(status: str, file_name: str, reason: str, **extra: Any) -> Finding:
    item = Finding(status=status, file_name=file_name, reason=reason)
    item.update(extra)
    return item


def _load(path: Path) -> dict[str, Any]:
    value = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(value, dict):
        raise ValueError(f"{path.name}: очікується JSON-об'єкт")
    return value


def _drive_index(listing: dict[str, Any]) -> dict[str, list[dict[str, Any]]]:
    files = listing.get("files")
    if not isinstance(files, list):
        raise ValueError("drive listing: немає списку files")
    index: dict[str, list[dict[str, Any]]] = {}
    for item in files:
        if not isinstance(item, dict):
            continue
        title = str(item.get("title") or item.get("name") or "")
        if title:
            index.setdefault(title, []).append(item)
    return index


def _size(item: dict[str, Any]) -> int | None:
    raw = item.get("fileSize", item.get("size"))
    try:
        return int(str(raw))
    except (TypeError, ValueError):
        return None


def _published(catalog: dict[str, Any]) -> tuple[dict[str, dict], dict[str, dict]]:
    by_id: dict[str, dict] = {}
    by_name: dict[str, dict] = {}
    for project in catalog.get("projects") or []:
        for volume in project.get("volumes") or []:
            by_id[str(volume.get("id"))] = volume
            by_name[str(volume.get("file_name")).lower()] = volume
    return by_id, by_name


def plan_additions(
    catalog: dict[str, Any],
    report: dict[str, Any],
    listing: dict[str, Any],
) -> tuple[list[dict[str, Any]], list[Finding]]:
    """Returns (new catalog volume entries, findings). Never mutates inputs."""
    if report.get("format") != "renault-docs-folder-export-v1":
        raise ValueError("report: невідомий формат")
    if int(report.get("schema_version") or 0) < 2:
        raise ValueError("report: потрібна schema 2+ (експорт з v0.5.123 або новішої)")

    drive = _drive_index(listing)
    by_id, by_name = _published(catalog)
    additions: list[dict[str, Any]] = []
    findings: list[Finding] = []
    seen_ids: set[str] = set()
    seen_codes: dict[tuple[str, str], str] = {}

    for volume in report.get("volumes") or []:
        name = str(volume.get("file_name") or "")
        status = str(volume.get("status") or "")
        if status not in VERIFIED_STATUSES:
            findings.append(_finding("BLOCKED", name, "телефон не перевірив копію (" + status + ")"))
            continue

        package_id = str(volume.get("package_id") or "").strip()
        sha = str(volume.get("sha256") or "").lower()
        size = volume.get("size_bytes")
        code = str(volume.get("document_code") or "").strip()
        date = str(volume.get("date") or "").strip()

        problems = []
        if not package_id:
            problems.append("немає package_id")
        if not SHA256.match(sha):
            problems.append("немає sha256")
        if not isinstance(size, int) or size <= 0:
            problems.append("немає size_bytes")
        # Some genuine volumes have no date (Megane II NT8206A, NT8220A, …); the app
        # catalog accepts date = null. A date that IS given must be well-formed.
        if not code:
            problems.append("немає NT-коду")
        if date and not DATE.match(date):
            problems.append("неправильна дата")
        if not name.endswith(".rdpkg") or "/" in name or "\\" in name:
            problems.append("неправильна назва файлу")
        elif code and (code not in name or (date and date not in name)):
            problems.append("назва файлу не відповідає NT-коду/даті")
        if problems:
            findings.append(_finding("BLOCKED", name, "; ".join(problems)))
            continue

        if package_id in seen_ids:
            findings.append(_finding("BLOCKED", name, "package_id повторюється у звіті"))
            continue
        seen_ids.add(package_id)
        key = (code, date)
        if key in seen_codes:
            findings.append(_finding("BLOCKED", name, "той самий NT і дата двічі у звіті"))
            continue
        seen_codes[key] = name

        published = by_id.get(package_id)
        if published is not None:
            if str(published.get("sha256")).lower() == sha:
                findings.append(_finding("ALREADY_PUBLISHED", name, "уже в каталозі, той самий sha256"))
            else:
                findings.append(_finding("BLOCKED", name, "у каталозі цей том з іншим sha256 — потрібна звірка"))
            continue
        same_name = by_name.get(name.lower())
        if same_name is not None:
            findings.append(_finding("BLOCKED", name, "у каталозі інший том з такою самою назвою файлу"))
            continue

        candidates = drive.get(name, [])
        if not candidates:
            findings.append(_finding("BLOCKED", name, "на Drive немає файлу з такою назвою"))
            continue
        if len(candidates) > 1:
            findings.append(_finding("REVIEW_REQUIRED", name, f"на Drive {len(candidates)} файли з такою назвою"))
            continue
        remote = candidates[0]
        if _size(remote) != size:
            findings.append(_finding("BLOCKED", name, "розмір на Drive не збігається зі звітом"))
            continue
        drive_id = str(remote.get("id") or "")
        if not drive_id:
            findings.append(_finding("BLOCKED", name, "немає Drive id"))
            continue

        additions.append({
            "id": package_id,
            "document_code": code,
            "date": date or None,
            "vehicle_codes": [str(c) for c in (volume.get("vehicle_codes") or [])],
            "document_type": volume.get("document_type"),
            "document_version": volume.get("document_version"),
            "region": volume.get("region"),
            "file_name": name,
            "drive_file_id": drive_id,
            "size_bytes": size,
            "sha256": sha,
        })
        findings.append(_finding("READY", name, "перевірено: звіт телефона + одна назва і розмір на Drive"))

    return additions, findings


def merge_catalog(
    catalog: dict[str, Any],
    report_project: dict[str, Any],
    additions: list[dict[str, Any]],
    catalog_version: int,
    generated_at: str,
) -> dict[str, Any]:
    result = copy.deepcopy(catalog)
    result["catalog_version"] = catalog_version
    result["generated_at"] = generated_at
    projects = result.setdefault("projects", [])
    project_id = str(report_project.get("id") or "")
    project = next((p for p in projects if p.get("id") == project_id), None)
    if project is None:
        project = {
            "id": project_id,
            "title": report_project.get("title") or project_id,
            "vehicle_codes": [],
            "volumes": [],
        }
        projects.append(project)
    volumes = project.setdefault("volumes", [])
    volumes.extend(additions)
    volumes.sort(key=lambda v: (str(v.get("date") or ""), str(v.get("document_code") or "")))
    codes = set(project.get("vehicle_codes") or [])
    for volume in volumes:
        codes.update(volume.get("vehicle_codes") or [])
    project["vehicle_codes"] = sorted(codes)
    years = [int(str(v["date"])[:4]) for v in volumes if DATE.match(str(v.get("date") or ""))]
    if years:
        project["document_year_from"] = min(years)
        project["document_year_to"] = max(years)
    return result


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--catalog", required=True, type=Path)
    parser.add_argument("--report", required=True, type=Path)
    parser.add_argument("--drive-listing", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--generated-at", required=True)
    args = parser.parse_args(argv)

    catalog = _load(args.catalog)
    report = _load(args.report)
    listing = _load(args.drive_listing)
    additions, findings = plan_additions(catalog, report, listing)

    counts: dict[str, int] = {}
    for item in findings:
        counts[item["status"]] = counts.get(item["status"], 0) + 1
    blocking = [f for f in findings if f["status"] in ("BLOCKED", "REVIEW_REQUIRED")]
    summary = {"counts": counts, "new_volumes": len(additions),
               "findings": [{k: v for k, v in f.items()} for f in findings]}

    if blocking or not additions:
        summary["written"] = False
        print(json.dumps(summary, ensure_ascii=False, indent=2))
        return 2

    if args.output.resolve() == args.catalog.resolve():
        raise SystemExit("output must be a new file, not the current catalog")
    merged = merge_catalog(
        catalog,
        report.get("project") or {},
        additions,
        int(catalog.get("catalog_version") or 0) + 1,
        args.generated_at,
    )
    args.output.write_text(json.dumps(merged, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    summary["written"] = True
    summary["catalog_version"] = merged["catalog_version"]
    print(json.dumps(summary, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
