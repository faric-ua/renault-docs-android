#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import shutil
import sys
import time
import unicodedata
import re
from collections import Counter, defaultdict
from pathlib import Path
from typing import Iterable

REPO_ROOT = Path(__file__).resolve().parents[1]
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

from core.convert_paths import TEXT_EXTENSIONS, patch_text
from core.dataset_manifest import load_manifest
from core.dataset_package import build_dataset_package
from core.volumes import discover_volumes


def slugify(value: str) -> str:
    normalized = unicodedata.normalize("NFKD", value)
    ascii_value = normalized.encode("ascii", "ignore").decode("ascii")
    slug = re.sub(r"[^a-zA-Z0-9]+", "-", ascii_value).strip("-").lower()
    return slug or "renault-dataset"


def log(message: str) -> None:
    print(message, flush=True)


def elapsed(started: float) -> str:
    seconds = int(time.monotonic() - started)
    minutes, seconds = divmod(seconds, 60)
    hours, minutes = divmod(minutes, 60)
    if hours:
        return f"{hours}h {minutes:02d}m {seconds:02d}s"
    if minutes:
        return f"{minutes}m {seconds:02d}s"
    return f"{seconds}s"


def ensure_nomedia(output: Path) -> None:
    marker = output / ".nomedia"
    if not marker.exists():
        marker.write_text(
            "Renault Docs dataset: keep technical media out of Android media indexing.\n",
            encoding="utf-8",
        )


def copy_new_dataset(source: Path, output: Path) -> list[str]:
    if output.exists():
        raise SystemExit(
            "Output already exists. Use --merge to add missing volumes, "
            f"or choose another output:\n  {output}"
        )

    log("Copy: створюю output і .nomedia...")
    output.mkdir(parents=True)
    ensure_nomedia(output)

    started = time.monotonic()
    log("Copy: пряме файлове копіювання (без SAF)...")
    shutil.copytree(
        source,
        output,
        dirs_exist_ok=True,
        copy_function=shutil.copy2,
    )
    ensure_nomedia(output)
    log(f"Copy: готово за {elapsed(started)}.")
    return []


def copy_missing_volumes(source: Path, output: Path) -> list[str]:
    if not output.is_dir():
        raise SystemExit(
            "--merge requires an existing output directory:\n"
            f"  {output}"
        )

    ensure_nomedia(output)

    source_volumes = discover_volumes(source)
    if not source_volumes:
        raise SystemExit(
            "У source не знайдено окремих top-level томів з INDEX/ACCUEIL."
        )

    copied_roots: list[str] = []
    skipped: list[str] = []

    started = time.monotonic()
    for volume in source_volumes:
        folder = str(volume["source_folder"])
        src = source / folder
        dst = output / folder

        if dst.exists():
            skipped.append(folder)
            continue

        log(f"Merge: копіюю том: {folder}")
        temp = output / f".renault-merge-{slugify(folder)}"
        if temp.exists():
            shutil.rmtree(temp)
        shutil.copytree(
            src,
            temp,
            copy_function=shutil.copy2,
        )
        temp.rename(dst)
        copied_roots.append(folder)

    log(
        "Merge: готово за "
        + elapsed(started)
        + f" · додано томів: {len(copied_roots)}"
        + f" · вже були: {len(skipped)}"
    )

    return copied_roots


def all_files(root: Path) -> list[Path]:
    return [path for path in root.rglob("*") if path.is_file()]


def normalize_paths(
    output: Path,
    only_roots: Iterable[str] | None = None,
) -> dict:
    started = time.monotonic()
    files = all_files(output)
    relative_files = [path.relative_to(output).as_posix() for path in files]
    exact_files = set(relative_files)

    lower_map: dict[str, list[str]] = defaultdict(list)
    for relative in relative_files:
        lower_map[relative.lower()].append(relative)

    roots = tuple(
        root.rstrip("/") + "/"
        for root in (only_roots or [])
    )

    text_files: list[Path] = []
    for path in files:
        if path.suffix.lower() not in TEXT_EXTENSIONS:
            continue
        relative = path.relative_to(output).as_posix()
        if roots and not relative.startswith(roots):
            continue
        text_files.append(path)

    log(
        "Normalize: "
        f"{len(text_files)} text files із {len(files)} total..."
    )

    changed_files = 0
    all_changes: list[dict] = []
    kind_counts: Counter[str] = Counter()

    for index, path in enumerate(text_files, start=1):
        relative = path.relative_to(output).as_posix()
        raw = path.read_bytes()
        text = raw.decode("latin-1")
        patched, changes = patch_text(
            relative,
            text,
            exact_files,
            lower_map,
        )

        if changes:
            path.write_bytes(patched.encode("latin-1"))
            changed_files += 1
            for change in changes:
                item = dict(change)
                item["file"] = relative
                all_changes.append(item)
                kind_counts[item["kind"]] += 1

        if index == 1 or index % 1000 == 0 or index == len(text_files):
            log(
                f"Normalize: {index}/{len(text_files)}"
                f" · changed: {changed_files}"
            )

    log(f"Normalize: готово за {elapsed(started)}.")

    return {
        "files_total": len(files),
        "changed_files": changed_files,
        "changes_total": len(all_changes),
        "changes_by_kind": dict(kind_counts),
        "changes": all_changes,
    }


def dataset_metadata(
    source: Path,
    output: Path,
    *,
    title: str | None,
    model: str | None,
    dataset_id: str | None,
    project_id: str | None,
    platform: str | None,
    years_from: int | None,
    years_to: int | None,
    merge: bool,
) -> dict:
    manifest_path = output / "renault-dataset.json"

    if merge and manifest_path.is_file():
        existing = load_manifest(manifest_path)
        return {
            "id": existing["id"],
            "title": existing["title"],
            "manufacturer": existing.get("manufacturer", "Renault"),
            "model": existing["model"],
            "project_id": existing.get("project_id")
            or project_id
            or slugify(existing["model"]),
            "platform": existing.get("platform"),
            "years": existing.get("years"),
            "content_type": existing.get(
                "content_type",
                "technical-documentation",
            ),
            "entrypoint": existing.get("legacy_entrypoint")
            or existing.get("entrypoint"),
            "viewer_profile": existing.get(
                "viewer_profile",
                "renault-legacy-web-v1",
            ),
        }

    model_value = (model or source.name).strip()
    title_value = (title or model_value).strip()
    metadata = {
        "id": dataset_id or slugify(model_value),
        "title": title_value,
        "manufacturer": "Renault",
        "model": model_value,
        "project_id": project_id or slugify(model_value),
        "content_type": "technical-documentation",
        "entrypoint": None,
        "viewer_profile": "renault-legacy-web-v1",
    }

    if platform:
        metadata["platform"] = platform

    if years_from or years_to:
        years: dict[str, int] = {}
        if years_from:
            years["from"] = years_from
        if years_to:
            years["to"] = years_to
        metadata["years"] = years

    return metadata


def write_report(
    path: Path,
    *,
    source: Path,
    output: Path,
    merge: bool,
    summary: dict,
    copied_roots: list[str],
    total_elapsed: str,
) -> None:
    report = {
        "tool": "fast-direct-filesystem-converter",
        "source": str(source),
        "output": str(output),
        "mode": "merge" if merge else "new",
        "copied_volume_roots": copied_roots,
        "files_total": summary["files_total"],
        "changed_files": summary["changed_files"],
        "changes_total": summary["changes_total"],
        "changes_by_kind": summary["changes_by_kind"],
        "elapsed": total_elapsed,
        "changes": summary["changes"],
    }

    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )


def main() -> int:
    parser = argparse.ArgumentParser(
        description=(
            "Fast Renault converter for Termux/Linux using direct filesystem "
            "access instead of Android SAF."
        )
    )
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--title")
    parser.add_argument("--model")
    parser.add_argument("--dataset-id")
    parser.add_argument(
        "--project-id",
        help="Target Renault project id, e.g. megane-ii.",
    )
    parser.add_argument("--platform")
    parser.add_argument("--years-from", type=int)
    parser.add_argument("--years-to", type=int)
    parser.add_argument(
        "--merge",
        action="store_true",
        help="Add missing top-level volumes to an existing dataset.",
    )
    parser.add_argument(
        "--skip-package",
        action="store_true",
        help="Only copy/normalize; do not rebuild Renault package metadata.",
    )
    parser.add_argument(
        "--report",
        type=Path,
        help="Report path; defaults to <output>/conversion-report.json.",
    )
    args = parser.parse_args()

    source = args.source.expanduser().resolve()
    output = args.output.expanduser().resolve()

    if not source.is_dir():
        raise SystemExit(f"Source directory not found:\n  {source}")

    if source == output:
        raise SystemExit("Source and output must be different directories.")

    total_started = time.monotonic()

    log("============================================================")
    log("Renault Fast Converter · direct filesystem")
    log("============================================================")
    log(f"Source: {source}")
    log(f"Output: {output}")
    log(f"Mode:   {'MERGE' if args.merge else 'NEW'}")
    log("")

    if args.merge:
        copied_roots = copy_missing_volumes(source, output)
    else:
        copied_roots = copy_new_dataset(source, output)

    summary = normalize_paths(
        output,
        only_roots=copied_roots if args.merge else None,
    )

    metadata = dataset_metadata(
        source,
        output,
        title=args.title,
        model=args.model,
        dataset_id=args.dataset_id,
        project_id=args.project_id,
        platform=args.platform,
        years_from=args.years_from,
        years_to=args.years_to,
        merge=args.merge,
    )

    report_path = args.report or (output / "conversion-report.json")

    if not args.skip_package:
        package_started = time.monotonic()
        log("Package: будую manifest / Modern / Runtime IR / Fast Pack...")
        package = build_dataset_package(
            metadata,
            output,
            {
                "files_total": summary["files_total"],
                "changed_files": summary["changed_files"],
                "changes_total": summary["changes_total"],
            },
            progress=log,
        )
        log(
            f"Package: готово за {elapsed(package_started)}"
            f" · томів: {len(package['volumes'])}"
        )

    total_elapsed = elapsed(total_started)

    write_report(
        report_path,
        source=source,
        output=output,
        merge=args.merge,
        summary=summary,
        copied_roots=copied_roots,
        total_elapsed=total_elapsed,
    )

    ensure_nomedia(output)

    log("")
    log("============================================================")
    log("ГОТОВО")
    log("============================================================")
    log(f"Output: {output}")
    log(f"Report: {report_path}")
    log(f"Files:  {summary['files_total']}")
    log(f"Changed files: {summary['changed_files']}")
    log(f"Fixes:  {summary['changes_total']}")
    log(f"Time:   {total_elapsed}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
