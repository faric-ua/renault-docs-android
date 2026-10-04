from __future__ import annotations

import json
from pathlib import Path
from typing import Any


MODERN_INDEX_SCHEMA_VERSION = 1
MODERN_INDEX_FILENAME = "modern-index.json"


def build_modern_index(
    dataset: dict[str, Any],
    volumes: list[dict[str, Any]],
) -> dict[str, Any]:
    normalized_volumes: list[dict[str, Any]] = []

    for volume in volumes:
        normalized_volumes.append(
            {
                key: value
                for key, value in {
                    "id": volume.get("id"),
                    "title": volume.get("title"),
                    "document_code": volume.get("document_code"),
                    "date": volume.get("date"),
                    "vehicle_codes": volume.get("vehicle_codes"),
                    "document_type": volume.get("document_type"),
                    "document_version": volume.get("document_version"),
                    "region": volume.get("region"),
                    "kind": volume.get("kind"),
                    "source_folder": volume.get("source_folder"),
                    "entrypoint": volume.get("entrypoint"),
                }.items()
                if value is not None
            }
        )

    return {
        "schema_version": MODERN_INDEX_SCHEMA_VERSION,
        "dataset": {
            key: value
            for key, value in {
                "id": dataset.get("id"),
                "title": dataset.get("title"),
                "manufacturer": dataset.get("manufacturer", "Renault"),
                "model": dataset.get("model"),
                "platform": dataset.get("platform"),
                "years": dataset.get("years"),
                "content_type": dataset.get(
                    "content_type",
                    "technical-documentation",
                ),
            }.items()
            if value is not None
        },
        "navigation": {
            "level": "volumes",
            "volumes": normalized_volumes,
        },
    }


def write_modern_index(
    dataset: dict[str, Any],
    volumes: list[dict[str, Any]],
    package_root: Path,
) -> Path:
    target = package_root / MODERN_INDEX_FILENAME
    target.write_text(
        json.dumps(
            build_modern_index(dataset, volumes),
            ensure_ascii=False,
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )
    return target
