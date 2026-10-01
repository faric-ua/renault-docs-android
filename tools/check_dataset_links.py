#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import sys
from dataclasses import dataclass, asdict
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import unquote, urlsplit

REPO_ROOT = Path(__file__).resolve().parents[1]
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

from core.volumes import discover_volumes

HTML_EXTENSIONS = {".htm", ".html"}
CSS_EXTENSIONS = {".css"}
SKIP_SCHEMES = {
    "http",
    "https",
    "mailto",
    "javascript",
    "data",
    "tel",
    "about",
}
HTML_LINK_ATTRIBUTES = {
    "href",
    "src",
    "background",
    "action",
    "data",
    "poster",
}
CSS_URL_RE = re.compile(
    r'''url\(\s*(["']?)(.*?)\1\s*\)''',
    re.IGNORECASE,
)
CSS_IMPORT_RE = re.compile(
    r'''@import\s+(?:url\()?\s*(["'])(.*?)\1''',
    re.IGNORECASE,
)


@dataclass(frozen=True)
class MissingLink:
    source: str
    reference: str
    resolved: str


class LinkParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.references: list[str] = []

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        self._collect(attrs)

    def handle_startendtag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        self._collect(attrs)

    def _collect(self, attrs: list[tuple[str, str | None]]) -> None:
        for name, value in attrs:
            if value is None:
                continue
            if name.lower() in HTML_LINK_ATTRIBUTES:
                self.references.append(value)


def decode_legacy_text(path: Path) -> str:
    raw = path.read_bytes()
    for encoding in ("utf-8", "cp1252", "latin-1"):
        try:
            return raw.decode(encoding)
        except UnicodeDecodeError:
            continue
    return raw.decode("latin-1", errors="replace")


def normalize_reference(reference: str) -> str | None:
    value = reference.strip()
    if not value or value.startswith("#"):
        return None

    value = value.replace("\\", "/")

    try:
        parts = urlsplit(value)
    except ValueError:
        return None

    if parts.scheme.lower() in SKIP_SCHEMES:
        return None

    if parts.netloc:
        return None

    path = unquote(parts.path).strip()
    if not path:
        return None

    return path


def resolve_reference(root: Path, source: Path, reference: str) -> Path | None:
    normalized = normalize_reference(reference)
    if normalized is None:
        return None

    if normalized.startswith("/"):
        candidate = root / normalized.lstrip("/")
    else:
        candidate = source.parent / normalized

    try:
        resolved = candidate.resolve(strict=False)
        root_resolved = root.resolve(strict=True)
        resolved.relative_to(root_resolved)
    except (OSError, ValueError):
        return None

    return resolved


def extract_html_references(path: Path) -> list[str]:
    parser = LinkParser()
    parser.feed(decode_legacy_text(path))
    parser.close()
    return parser.references


def extract_css_references(path: Path) -> list[str]:
    text = decode_legacy_text(path)
    refs = [match.group(2) for match in CSS_URL_RE.finditer(text)]
    refs.extend(match.group(2) for match in CSS_IMPORT_RE.finditer(text))
    return refs


def iter_source_files(root: Path):
    for path in root.rglob("*"):
        if not path.is_file():
            continue
        suffix = path.suffix.lower()
        if suffix in HTML_EXTENSIONS or suffix in CSS_EXTENSIONS:
            yield path


def manifest_references(root: Path) -> list[tuple[Path, str]]:
    manifest_path = root / "renault-dataset.json"
    if not manifest_path.is_file():
        return []

    try:
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return []

    keys = (
        "entrypoint",
        "catalog_entrypoint",
        "legacy_entrypoint",
        "modern_index",
        "modern_sections",
        "runtime_tree",
        "runtime_ir_index",
        "runtime_ir_coverage",
        "fast_pack",
    )

    refs: list[tuple[Path, str]] = []
    for key in keys:
        value = manifest.get(key)
        if isinstance(value, str) and value.strip():
            refs.append((manifest_path, value))

    volumes = manifest.get("volumes")
    if isinstance(volumes, list):
        for volume in volumes:
            if not isinstance(volume, dict):
                continue
            for key in ("entrypoint", "open_entrypoint"):
                value = volume.get(key)
                if isinstance(value, str) and value.strip():
                    refs.append((manifest_path, value))

    return refs


def check_dataset(root: Path) -> dict:
    root = root.resolve(strict=True)
    if not root.is_dir():
        raise ValueError(f"Dataset root is not a directory: {root}")

    scanned_files = 0
    checked_references = 0
    missing: list[MissingLink] = []
    skipped_outside_root = 0

    sources: list[tuple[Path, list[str]]] = []
    for source in iter_source_files(root):
        scanned_files += 1
        refs = (
            extract_html_references(source)
            if source.suffix.lower() in HTML_EXTENSIONS
            else extract_css_references(source)
        )
        sources.append((source, refs))

    sources.extend((source, [reference]) for source, reference in manifest_references(root))

    for source, references in sources:
        for reference in references:
            normalized = normalize_reference(reference)
            if normalized is None:
                continue

            checked_references += 1
            resolved = resolve_reference(root, source, reference)
            if resolved is None:
                skipped_outside_root += 1
                continue

            if not resolved.exists():
                missing.append(
                    MissingLink(
                        source=source.relative_to(root).as_posix(),
                        reference=reference,
                        resolved=resolved.relative_to(root).as_posix(),
                    )
                )

    unique_missing = sorted(
        {item for item in missing},
        key=lambda item: (item.source.lower(), item.reference.lower(), item.resolved.lower()),
    )

    return {
        "dataset_root": str(root),
        "scanned_files": scanned_files,
        "checked_references": checked_references,
        "missing_count": len(unique_missing),
        "skipped_outside_root": skipped_outside_root,
        "missing": [asdict(item) for item in unique_missing],
        "status": "PASS" if not unique_missing else "FAIL",
    }


def compare_volume_parity(source_root: Path, build_root: Path) -> dict:
    source_root = source_root.resolve(strict=True)
    build_root = build_root.resolve(strict=True)

    source_volumes = discover_volumes(source_root)
    build_volumes = discover_volumes(build_root)

    source_by_folder = {
        str(item["source_folder"]): item
        for item in source_volumes
    }
    build_by_folder = {
        str(item["source_folder"]): item
        for item in build_volumes
    }

    source_names = set(source_by_folder)
    build_names = set(build_by_folder)

    missing_in_build = sorted(
        source_names - build_names,
        key=str.casefold,
    )
    extra_in_build = sorted(
        build_names - source_names,
        key=str.casefold,
    )

    return {
        "source_root": str(source_root),
        "build_root": str(build_root),
        "source_volume_count": len(source_volumes),
        "build_volume_count": len(build_volumes),
        "missing_in_build_count": len(missing_in_build),
        "extra_in_build_count": len(extra_in_build),
        "missing_in_build": missing_in_build,
        "extra_in_build": extra_in_build,
        "status": (
            "PASS"
            if not missing_in_build and not extra_in_build
            else "FAIL"
        ),
    }



def _volume_names_from_metadata(
    volumes: object,
) -> tuple[set[str], int]:
    if not isinstance(volumes, list):
        return set(), 0

    names: set[str] = set()
    unidentified = 0

    for item in volumes:
        if not isinstance(item, dict):
            unidentified += 1
            continue

        source_folder = item.get("source_folder")
        if isinstance(source_folder, str) and source_folder.strip():
            names.add(source_folder.strip())
            continue

        entrypoint = item.get("entrypoint")
        if isinstance(entrypoint, str) and entrypoint.strip():
            normalized = entrypoint.replace("\\", "/").strip("/")
            first = normalized.split("/", 1)[0].strip()
            if first:
                names.add(unquote(first))
                continue

        unidentified += 1

    return names, unidentified


def compare_package_volume_parity(build_root: Path) -> dict:
    build_root = build_root.resolve(strict=True)
    live_volumes = discover_volumes(build_root)
    live_names = {
        str(item["source_folder"])
        for item in live_volumes
    }

    sources: dict[str, dict] = {}

    definitions = (
        (
            "manifest",
            build_root / "renault-dataset.json",
            lambda data: data.get("volumes") if isinstance(data, dict) else None,
        ),
        (
            "volumes",
            build_root / "_renault" / "volumes.json",
            lambda data: data if isinstance(data, list) else None,
        ),
        (
            "modern_index",
            build_root / "_renault" / "modern-index.json",
            lambda data: (
                data.get("navigation", {}).get("volumes")
                if isinstance(data, dict)
                and isinstance(data.get("navigation"), dict)
                else None
            ),
        ),
    )

    for key, path, extract in definitions:
        source = {
            "path": path.relative_to(build_root).as_posix(),
            "volume_count": None,
            "missing_from_metadata": sorted(live_names, key=str.casefold),
            "extra_in_metadata": [],
            "unidentified_entries": 0,
            "error": None,
            "status": "FAIL",
        }

        if not path.is_file():
            source["error"] = "missing file"
            sources[key] = source
            continue

        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as error:
            source["error"] = f"invalid JSON: {error}"
            sources[key] = source
            continue

        volumes = extract(data)
        if not isinstance(volumes, list):
            source["error"] = "volume list is missing or invalid"
            sources[key] = source
            continue

        metadata_names, unidentified = _volume_names_from_metadata(volumes)
        missing = sorted(
            live_names - metadata_names,
            key=str.casefold,
        )
        extra = sorted(
            metadata_names - live_names,
            key=str.casefold,
        )

        source.update(
            {
                "volume_count": len(volumes),
                "missing_from_metadata": missing,
                "extra_in_metadata": extra,
                "unidentified_entries": unidentified,
                "status": (
                    "PASS"
                    if (
                        len(volumes) == len(live_volumes)
                        and not missing
                        and not extra
                        and unidentified == 0
                    )
                    else "FAIL"
                ),
            }
        )
        sources[key] = source

    status = (
        "PASS"
        if all(
            source["status"] == "PASS"
            for source in sources.values()
        )
        else "FAIL"
    )

    return {
        "build_root": str(build_root),
        "live_volume_count": len(live_volumes),
        "live_volumes": sorted(live_names, key=str.casefold),
        "sources": sources,
        "status": status,
    }

def main() -> int:
    parser = argparse.ArgumentParser(
        description="Read-only Renault dataset link integrity checker."
    )
    parser.add_argument("root", type=Path, help="Prepared Renault dataset root.")
    parser.add_argument(
        "--json-out",
        type=Path,
        help="Optional JSON report path outside or inside the dataset.",
    )
    parser.add_argument(
        "--source-root",
        type=Path,
        help="Optional original source root for top-level volume parity comparison.",
    )
    parser.add_argument(
        "--max-print",
        type=int,
        default=100,
        help="Maximum missing links to print to stdout.",
    )
    args = parser.parse_args()

    try:
        report = check_dataset(args.root)

        report["package_volume_parity"] = compare_package_volume_parity(
            args.root,
        )
        if report["package_volume_parity"]["status"] != "PASS":
            report["status"] = "FAIL"

        if args.source_root is not None:
            report["volume_parity"] = compare_volume_parity(
                args.source_root,
                args.root,
            )
            if report["volume_parity"]["status"] != "PASS":
                report["status"] = "FAIL"
    except (OSError, ValueError) as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 2

    print("Renault Docs · Dataset link check")
    print(f"Root: {report['dataset_root']}")
    print(f"Scanned HTML/CSS files: {report['scanned_files']}")
    print(f"Checked local references: {report['checked_references']}")
    print(f"Missing: {report['missing_count']}")
    print(f"Skipped outside-root refs: {report['skipped_outside_root']}")
    print(f"Status: {report['status']}")

    package_parity = report.get("package_volume_parity")
    if package_parity is not None:
        print(
            "Live build volumes: "
            f"{package_parity['live_volume_count']}"
        )
        for key, label in (
            ("manifest", "Manifest"),
            ("volumes", "volumes.json"),
            ("modern_index", "modern-index.json"),
        ):
            source = package_parity["sources"][key]
            count = (
                "?"
                if source["volume_count"] is None
                else str(source["volume_count"])
            )
            print(
                f"{label} volumes: {count} · "
                f"metadata parity: {source['status']}"
            )
            if source["error"]:
                print(
                    f"PACKAGE METADATA ERROR: "
                    f"{source['path']} · {source['error']}"
                )
            for folder in source["missing_from_metadata"]:
                print(
                    f"MISSING FROM {label.upper()}: {folder}"
                )
            for folder in source["extra_in_metadata"]:
                print(
                    f"EXTRA IN {label.upper()}: {folder}"
                )
            if source["unidentified_entries"]:
                print(
                    f"UNIDENTIFIED {label.upper()} ENTRIES: "
                    f"{source['unidentified_entries']}"
                )
        print(
            "Package metadata parity: "
            f"{package_parity['status']}"
        )

    parity = report.get("volume_parity")
    if parity is not None:
        print(f"Source volumes: {parity['source_volume_count']}")
        print(f"Build volumes: {parity['build_volume_count']}")
        print(f"Missing volumes in build: {parity['missing_in_build_count']}")
        print(f"Extra volumes in build: {parity['extra_in_build_count']}")
        print(f"Volume parity: {parity['status']}")
        for folder in parity["missing_in_build"]:
            print(f"MISSING VOLUME: {folder}")
        for folder in parity["extra_in_build"]:
            print(f"EXTRA VOLUME: {folder}")

    for item in report["missing"][: max(args.max_print, 0)]:
        print(
            "MISSING: "
            f"{item['source']} -> {item['reference']} "
            f"(resolved: {item['resolved']})"
        )

    if args.json_out:
        args.json_out.parent.mkdir(parents=True, exist_ok=True)
        args.json_out.write_text(
            json.dumps(report, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
        print(f"JSON report: {args.json_out}")

    return 0 if report["status"] == "PASS" else 1


if __name__ == "__main__":
    raise SystemExit(main())
