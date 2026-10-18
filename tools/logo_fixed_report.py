#!/usr/bin/env python3
"""Report for the Laguna II volumes with the new logo, before they go to Google Drive.

Owner 2026-10-18 19:21 «давай уже заменим те томи»: the 10 Laguna II volumes fixed by
`RENAULT DOCS: 26` (folder `logo-fixed/`) replace the old ones in the public catalog
(catalog v6). Claude in the cloud cannot read the phone and cannot download 70 MB files
through the Drive connector, so the phone writes the fingerprints here:

  logo-fixed/logo-fixed-report.json
    {format, generated_at, files: [{file_name, package_id, size_bytes, sha256,
                                    catalog_id, bad_logo_places}]}

The owner uploads the .rdpkg files together with this report to Drive; Claude builds
catalog v6 from it (tools/catalog_replace_volumes.py). The app checks SHA-256 of every
download anyway, so a wrong number can only stop a download, never install a bad file.

Read-only for the volume files: nothing is changed or deleted. Only the report is
(re)written. Exit codes: 0 — report written and every file is fine, 2 — a problem.
"""

from __future__ import annotations

import argparse
import json
import sys
from datetime import datetime, timezone
from pathlib import Path
from zipfile import BadZipFile, ZipFile

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import fix_volume_logo as fx  # noqa: E402

REPORT_NAME = "logo-fixed-report.json"
REPORT_FORMAT = "renodocs-logo-fixed-report-v1"
DEFAULT_CATALOG = ROOT / "config" / "renault-docs-catalog.v5.json"


def catalog_by_file_name(catalog_path: Path) -> dict[str, dict]:
    if not catalog_path.is_file():
        return {}
    data = json.loads(catalog_path.read_text(encoding="utf-8"))
    out: dict[str, dict] = {}
    for project in data.get("projects") or []:
        for volume in project.get("volumes") or []:
            name = str(volume.get("file_name") or "")
            if name:
                out[name] = volume
    return out


def package_id_of(path: Path) -> str:
    with ZipFile(path) as zf:
        manifest = json.loads(zf.read(fx.RDPKG_MANIFEST).decode("utf-8"))
    return str(manifest.get("package_id") or "").strip()


def find_folders(roots: list[Path]) -> list[Path]:
    """Every `logo-fixed` folder under the roots that holds .rdpkg files."""
    folders: list[Path] = []
    for root in roots:
        if not root.is_dir():
            continue
        candidates = [root] if root.name == fx.OUTPUT_FOLDER else []
        candidates += [p for p in root.rglob(fx.OUTPUT_FOLDER) if p.is_dir()]
        for folder in candidates:
            if folder not in folders and any(folder.glob("*.rdpkg")):
                folders.append(folder)
    return folders


def build_report(folder: Path, catalog: dict[str, dict]) -> tuple[dict, list[str]]:
    files: list[dict] = []
    problems: list[str] = []
    for path in sorted(folder.glob("*.rdpkg")):
        entry: dict = {"file_name": path.name}
        try:
            entry["package_id"] = package_id_of(path)
            entry["bad_logo_places"] = fx.count_bad(path)
        except (BadZipFile, KeyError, ValueError, OSError) as error:
            problems.append(f"{path.name}: не читається ({error})")
            continue
        entry["size_bytes"] = path.stat().st_size
        entry["sha256"] = fx.sha256_file(path)
        old = catalog.get(path.name)
        entry["catalog_id"] = old.get("id") if old else None

        if entry["bad_logo_places"]:
            problems.append(f"{path.name}: зіпсоване лого ще є ({entry['bad_logo_places']} місць)")
        if not entry["package_id"]:
            problems.append(f"{path.name}: немає package_id")
        if old is None:
            problems.append(f"{path.name}: такого файлу нема в каталозі v5")
        elif old.get("id") != entry["package_id"]:
            problems.append(f"{path.name}: package_id {entry['package_id']} ≠ каталог {old.get('id')}")
        elif str(old.get("sha256")).lower() == entry["sha256"]:
            problems.append(f"{path.name}: це той самий файл, що вже в каталозі (лого не замінено?)")
        files.append(entry)

    report = {
        "format": REPORT_FORMAT,
        "schema_version": 1,
        "generated_at": datetime.now(timezone.utc).astimezone().isoformat(timespec="seconds"),
        "folder": folder.name,
        "files": files,
    }
    return report, problems


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("directory", nargs="*", help="where to look (default: the same places as item 26)")
    parser.add_argument("--catalog", type=Path, default=DEFAULT_CATALOG)
    args = parser.parse_args(argv)

    roots = [Path(p) for p in (args.directory or fx.DEFAULT_ROOTS)]
    folders = find_folders(roots)
    if not folders:
        print("Папку «logo-fixed» з томами не знайдено.")
        print("Спершу пункт 26 — «Замінити зіпсоване лого в томах Laguna II».")
        return 2

    catalog = catalog_by_file_name(args.catalog)
    status = 0
    for folder in folders:
        print(f"Папка: {folder}")
        report, problems = build_report(folder, catalog)
        for entry in report["files"]:
            mark = "✓" if entry.get("catalog_id") and not entry.get("bad_logo_places") else "!"
            size_mb = entry.get("size_bytes", 0) / 1_000_000
            print(f"  {mark} {entry['file_name']}  ({size_mb:.1f} МБ)")
        target = folder / REPORT_NAME
        target.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print()
        print(f"Томів: {len(report['files'])}. Звіт записано: {target.name}")
        if problems:
            status = 2
            print("Є що перевірити:")
            for line in problems:
                print(f"  • {line}")
        else:
            print("Усе гаразд: зіпсованого лого нема, кожен том є в каталозі.")
        print()

    print("Томи не змінено і не видалено — записано лише звіт.")
    return status


if __name__ == "__main__":
    sys.exit(main())
