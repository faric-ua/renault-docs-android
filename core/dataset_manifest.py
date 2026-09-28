from __future__ import annotations

import json
from pathlib import Path
from typing import Any

MANIFEST_FILENAME = "renault-dataset.json"
SCHEMA_VERSION = 1

ENTRY_NAMES = (
    "INDEX.HTM",
    "index.htm",
    "INDEX.HTML",
    "index.html",
    "ACCUEIL.HTM",
    "accueil.htm",
)


def discover_entrypoint(root: Path) -> str | None:
    for name in ENTRY_NAMES:
        candidate = root / name
        if candidate.is_file():
            return candidate.relative_to(root).as_posix()

    matches: list[Path] = []
    for name in ENTRY_NAMES:
        matches.extend(root.rglob(name))

    matches = sorted({p for p in matches if p.is_file()})
    if not matches:
        return None
    return matches[0].relative_to(root).as_posix()


def validate_manifest(manifest: dict[str, Any]) -> None:
    required = ("schema_version", "id", "title", "manufacturer", "model", "entrypoint")
    missing = [key for key in required if not manifest.get(key)]
    if missing:
        raise ValueError("Missing manifest fields: " + ", ".join(missing))

    years = manifest.get("years")
    if years is not None:
        if not isinstance(years, dict):
            raise ValueError("years must be an object")
        for key in ("from", "to"):
            if key in years and not isinstance(years[key], int):
                raise ValueError(f"years.{key} must be an integer")


def build_manifest(
    dataset: dict[str, Any],
    output_root: Path,
    conversion_summary: dict[str, Any] | None = None,
) -> dict[str, Any]:
    entrypoint = dataset.get("entrypoint") or discover_entrypoint(output_root)
    if not entrypoint:
        raise ValueError("Could not detect dataset entrypoint")

    preview = dataset.get("preview") or {
        "type": "generated",
        "title": dataset.get("title"),
        "subtitle": _years_label(dataset.get("years")),
    }

    manifest: dict[str, Any] = {
        "schema_version": SCHEMA_VERSION,
        "id": dataset.get("id"),
        "title": dataset.get("title"),
        "manufacturer": dataset.get("manufacturer", "Renault"),
        "model": dataset.get("model"),
        "project_id": dataset.get("project_id"),
        "platform": dataset.get("platform"),
        "years": dataset.get("years"),
        "content_type": dataset.get("content_type", "technical-documentation"),
        "entrypoint": entrypoint,
        "catalog_entrypoint": dataset.get("catalog_entrypoint"),
        "legacy_entrypoint": dataset.get("legacy_entrypoint"),
        "modern_index": dataset.get("modern_index"),
        "modern_sections": dataset.get("modern_sections"),
        "runtime_tree": dataset.get("runtime_tree"),
        "runtime_ir_index": dataset.get("runtime_ir_index"),
        "runtime_ir_coverage": dataset.get("runtime_ir_coverage"),
        "fast_pack": dataset.get("fast_pack"),
        "volumes": dataset.get("volumes"),
        "viewer_profile": dataset.get("viewer_profile", "renault-legacy-web-v1"),
        "preview": preview,
        "capabilities": {
            "html_frames": True,
            "javascript": True,
            "pdf": True,
            "pdf_fragments": True,
        },
        "conversion": {
            "normalized": True,
            "tool": "renault-docs-android",
        },
    }

    if conversion_summary:
        manifest["conversion"]["files_total"] = conversion_summary.get("files_total")
        manifest["conversion"]["changed_files"] = conversion_summary.get("changed_files")
        manifest["conversion"]["changes_total"] = conversion_summary.get("changes_total")

    # Keep the file compact and avoid meaningless null fields.
    manifest = _drop_none(manifest)
    validate_manifest(manifest)
    return manifest


def write_manifest(
    dataset: dict[str, Any],
    output_root: Path,
    conversion_summary: dict[str, Any] | None = None,
) -> Path:
    manifest = build_manifest(dataset, output_root, conversion_summary)
    target = output_root / MANIFEST_FILENAME
    target.write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    return target


def load_manifest(path: Path) -> dict[str, Any]:
    manifest = json.loads(path.read_text(encoding="utf-8"))
    validate_manifest(manifest)
    return manifest


def _years_label(years: Any) -> str | None:
    if not isinstance(years, dict):
        return None
    start = years.get("from")
    end = years.get("to")
    if start and end:
        return f"{start}–{end}"
    if start:
        return str(start)
    if end:
        return str(end)
    return None


def _drop_none(value: Any) -> Any:
    if isinstance(value, dict):
        return {
            key: _drop_none(item)
            for key, item in value.items()
            if item is not None
        }
    if isinstance(value, list):
        return [_drop_none(item) for item in value]
    return value
