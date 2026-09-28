from __future__ import annotations

from pathlib import Path
from typing import Any

from core.dataset_manifest import MANIFEST_FILENAME, load_manifest


def scan_library(root: Path) -> list[dict[str, Any]]:
    datasets: list[dict[str, Any]] = []

    if not root.is_dir():
        return datasets

    for child in sorted(root.iterdir()):
        if not child.is_dir():
            continue

        manifest_path = child / MANIFEST_FILENAME
        if not manifest_path.is_file():
            continue

        try:
            manifest = load_manifest(manifest_path)
        except (OSError, ValueError):
            continue

        datasets.append({
            "id": manifest["id"],
            "title": manifest["title"],
            "manufacturer": manifest["manufacturer"],
            "model": manifest["model"],
            "platform": manifest.get("platform"),
            "years": manifest.get("years"),
            "entrypoint": manifest["entrypoint"],
            "catalog_entrypoint": manifest.get("catalog_entrypoint"),
            "volume_count": len(manifest.get("volumes", [])),
            "preview": manifest.get("preview", {}),
            "path": str(child),
        })

    return datasets
