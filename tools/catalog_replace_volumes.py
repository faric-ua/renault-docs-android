#!/usr/bin/env python3
"""Catalog v6 — replace published volumes with corrected copies (Laguna II new logo).

Owner 2026-10-18 19:21 «давай уже заменим те томи». `tools/catalog_from_phone_export.py`
only ADDS volumes and blocks a volume whose sha256 changed («потрібна звірка»). This is
that звірка, for one known case: the same volume (same catalog id, same file name) whose
file was rewritten on purpose by `RENAULT DOCS: 26` (start-page logo).

Inputs (all read-only):
  --catalog        current catalog (config/renault-docs-catalog.v5.json)
  --report         logo-fixed-report.json written on the phone (tools/logo_fixed_report.py)
  --drive-listing  the Drive folder the owner uploaded into:
                   {"files": [{"id": "...", "title": "...", "fileSize": "123"}]}
Output:
  --output         a NEW catalog file (the current one is never modified)

A volume is replaced only when ALL hold:
  * the phone report says: no defaced logo left, package_id = catalog id;
  * the catalog has exactly that file name under that id;
  * the new sha256 differs from the published one;
  * exactly ONE file with that name in the Drive listing, with the same size.
Only drive_file_id, size_bytes and sha256 change; everything else is copied.
Any blocking finding → no output file, exit 2. Distinct NT codes stay distinct.
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
REPORT_FORMAT = "renodocs-logo-fixed-report-v1"


def _load(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def _size(item: dict[str, Any]) -> int | None:
    try:
        return int(item.get("fileSize") if item.get("fileSize") is not None else item.get("size"))
    except (TypeError, ValueError):
        return None


def plan_replacements(
    catalog: dict[str, Any],
    report: dict[str, Any],
    listing: dict[str, Any],
) -> tuple[dict[str, dict[str, Any]], list[dict[str, str]]]:
    """Returns ({catalog id: new fields}, findings). Never mutates inputs."""
    if report.get("format") != REPORT_FORMAT:
        raise ValueError("report: невідомий формат")

    by_id: dict[str, dict[str, Any]] = {}
    for project in catalog.get("projects") or []:
        for volume in project.get("volumes") or []:
            by_id[str(volume.get("id"))] = volume

    drive: dict[str, list[dict[str, Any]]] = {}
    for item in listing.get("files") or []:
        drive.setdefault(str(item.get("title") or item.get("name") or ""), []).append(item)

    plan: dict[str, dict[str, Any]] = {}
    findings: list[dict[str, str]] = []

    def finding(status: str, name: str, reason: str) -> None:
        findings.append({"status": status, "file_name": name, "reason": reason})

    for entry in report.get("files") or []:
        name = str(entry.get("file_name") or "")
        package_id = str(entry.get("package_id") or "")
        sha = str(entry.get("sha256") or "").lower()
        size = entry.get("size_bytes")

        if entry.get("bad_logo_places"):
            finding("BLOCKED", name, "у файлі ще є зіпсоване лого")
            continue
        if not SHA256.match(sha) or not isinstance(size, int) or size <= 0:
            finding("BLOCKED", name, "у звіті нема sha256 або розміру")
            continue
        old = by_id.get(package_id)
        if old is None:
            finding("BLOCKED", name, "у каталозі нема тому " + package_id)
            continue
        if old.get("file_name") != name:
            finding("BLOCKED", name, "у каталозі цей том має іншу назву файлу")
            continue
        if package_id in plan:
            finding("BLOCKED", name, "том двічі у звіті")
            continue
        if str(old.get("sha256")).lower() == sha:
            finding("ALREADY_PUBLISHED", name, "у каталозі вже цей самий файл")
            continue

        candidates = drive.get(name, [])
        if not candidates:
            finding("BLOCKED", name, "на Диску в папці нема файлу з такою назвою")
            continue
        if len(candidates) > 1:
            finding("BLOCKED", name, f"на Диску в папці {len(candidates)} файли з такою назвою")
            continue
        remote = candidates[0]
        if _size(remote) != size:
            finding("BLOCKED", name, "розмір на Диску не збігається зі звітом телефона")
            continue
        drive_id = str(remote.get("id") or "")
        if not drive_id or drive_id == old.get("drive_file_id"):
            finding("BLOCKED", name, "нема нового Drive id")
            continue

        plan[package_id] = {"drive_file_id": drive_id, "size_bytes": size, "sha256": sha}
        finding("READY", name, "звіт телефона + одна назва і той самий розмір на Диску")

    return plan, findings


def apply_plan(
    catalog: dict[str, Any],
    plan: dict[str, dict[str, Any]],
    catalog_version: int,
    generated_at: str,
) -> dict[str, Any]:
    result = copy.deepcopy(catalog)
    result["catalog_version"] = catalog_version
    result["generated_at"] = generated_at
    for project in result.get("projects") or []:
        for volume in project.get("volumes") or []:
            fields = plan.get(str(volume.get("id")))
            if fields:
                volume.update(fields)
    return result


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--catalog", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    parser.add_argument("--drive-listing", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--catalog-version", type=int, required=True)
    parser.add_argument("--generated-at", required=True)
    parser.add_argument("--expect", type=int, default=None, help="how many volumes must be replaced")
    args = parser.parse_args(argv)

    catalog = _load(args.catalog)
    if args.catalog_version <= int(catalog.get("catalog_version") or 0):
        print("Нова версія каталогу має бути більшою за поточну.")
        return 2
    plan, findings = plan_replacements(catalog, _load(args.report), _load(args.drive_listing))
    for item in findings:
        print(f"{item['status']:<18} {item['file_name']} — {item['reason']}")

    blocked = [f for f in findings if f["status"] == "BLOCKED"]
    if blocked or not plan or (args.expect is not None and len(plan) != args.expect):
        print(f"Не готово: замін {len(plan)}, заблоковано {len(blocked)}. Файл каталогу не створено.")
        return 2
    if args.output.exists():
        print(f"{args.output} уже існує — не перезаписую.")
        return 2
    result = apply_plan(catalog, plan, args.catalog_version, args.generated_at)
    args.output.write_text(json.dumps(result, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print(f"Готово: замінено томів {len(plan)} → {args.output}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
