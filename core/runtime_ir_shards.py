from __future__ import annotations

import json
import re
import shutil
from pathlib import Path
from typing import Any


RUNTIME_IR_INDEX_FILENAME = "runtime-ir-index.json"
RUNTIME_IR_DIRNAME = "runtime-ir"


def write_runtime_ir_shards(
    runtime_tree: dict[str, Any],
    package_root: Path,
) -> Path:
    """
    Write runtime-optimized IR files.

    The full runtime-tree.json is intentionally kept as a compiler/debug
    artifact, but Android must not load it into memory. Instead it reads one
    small index plus exactly one selected section JSON.
    """
    runtime_root = package_root / RUNTIME_IR_DIRNAME

    if runtime_root.exists():
        shutil.rmtree(runtime_root)

    sections_root = runtime_root / "sections"
    sections_root.mkdir(
        parents=True,
        exist_ok=True,
    )

    documentation_root = (
        runtime_root / "documentation"
    )
    documentation_root.mkdir(
        parents=True,
        exist_ok=True,
    )

    schema_version = int(
        runtime_tree.get(
            "schema_version",
            0,
        )
        or 0
    )
    compiler_phase = str(
        runtime_tree.get(
            "compiler_phase",
        )
        or ""
    )

    index_volumes: list[dict[str, Any]] = []

    used_volume_keys: set[str] = set()

    for volume_index, volume in enumerate(
        runtime_tree.get(
            "volumes",
            [],
        ),
        start=1,
    ):
        volume_key = _unique_volume_key(
            volume=volume,
            fallback=f"volume-{volume_index}",
            used=used_volume_keys,
        )

        volume_dir = (
            sections_root / volume_key
        )
        volume_dir.mkdir(
            parents=True,
            exist_ok=True,
        )

        volume_meta = {
            key: value
            for key, value in {
                "id": volume.get("id"),
                "title": volume.get("title"),
                "document_code":
                    volume.get(
                        "document_code"
                    ),
                "date": volume.get("date"),
                "kind": volume.get("kind"),
                "source_folder":
                    volume.get(
                        "source_folder"
                    ),
                "classic_entrypoint":
                    (
                        volume
                        .get(
                            "classic",
                            {},
                        )
                        .get(
                            "entrypoint"
                        )
                    ),
            }.items()
            if value is not None
        }

        documentation_path: str | None = None

        documentation = (
            volume
            .get(
                "modern",
                {},
            )
            .get(
                "documentation"
            )
        )

        if isinstance(
            documentation,
            dict,
        ):
            documentation_file = (
                documentation_root
                / f"{volume_key}.json"
            )

            documentation_payload = {
                "runtime_schema_version":
                    schema_version,
                "compiler_phase":
                    compiler_phase,
                "volume":
                    volume_meta,
                "documentation":
                    documentation,
            }

            documentation_file.write_text(
                json.dumps(
                    documentation_payload,
                    ensure_ascii=False,
                    separators=(
                        ",",
                        ":",
                    ),
                )
                + "\n",
                encoding="utf-8",
            )

            documentation_path = (
                documentation_file
                .relative_to(
                    package_root.parent,
                )
                .as_posix()
            )

        section_paths: dict[
            str,
            str,
        ] = {}
        section_entries: list[
            dict[str, str]
        ] = []

        sections = (
            volume
            .get(
                "modern",
                {},
            )
            .get(
                "sections",
                [],
            )
        )

        used_section_keys: set[str] = set()

        for section_index, section in enumerate(
            sections,
            start=1,
        ):
            code = str(
                section.get("code")
                or ""
            ).strip()

            section_key = _safe_key(
                code
                or f"section-{section_index}"
            )

            if section_key in used_section_keys:
                section_key = (
                    section_key
                    + "-"
                    + str(section_index)
                )

            used_section_keys.add(
                section_key
            )

            section_file = (
                volume_dir
                / f"{section_key}.json"
            )

            payload = {
                "runtime_schema_version":
                    schema_version,
                "compiler_phase":
                    compiler_phase,
                "volume":
                    volume_meta,
                "section":
                    section,
            }

            section_file.write_text(
                json.dumps(
                    payload,
                    ensure_ascii=False,
                    separators=(
                        ",",
                        ":",
                    ),
                )
                + "\n",
                encoding="utf-8",
            )

            relative_path = (
                section_file
                .relative_to(
                    package_root.parent,
                )
                .as_posix()
            )

            entrypoint = str(
                section.get(
                    "legacy_entrypoint"
                )
                or section.get(
                    "entrypoint"
                )
                or ""
            ).strip()

            section_entries.append(
                {
                    "code": code,
                    "entrypoint":
                        entrypoint,
                    "path":
                        relative_path,
                }
            )

            # Backward-compatible lookup for older clients. Keep the first
            # occurrence of a display code; v0.5.26+ resolves duplicates by
            # code + legacy entrypoint via section_entries.
            section_paths.setdefault(
                code,
                relative_path,
            )

        index_volume = {
            **volume_meta,
            "section_count":
                len(section_entries),
            "sections":
                section_paths,
            "section_entries":
                section_entries,
        }

        if documentation_path is not None:
            index_volume[
                "documentation_path"
            ] = documentation_path

        index_volumes.append(
            index_volume
        )

    index = {
        "schema_version":
            schema_version,
        "format":
            "renault-runtime-ir-index",
        "compiler_phase":
            compiler_phase,
        "runtime_data_contract":
            "sharded-section-json",
        "volume_count":
            len(index_volumes),
        "section_count":
            sum(
                int(
                    item.get(
                        "section_count",
                        0,
                    )
                )
                for item in index_volumes
            ),
        "volumes":
            index_volumes,
    }

    target = (
        package_root
        / RUNTIME_IR_INDEX_FILENAME
    )

    target.write_text(
        json.dumps(
            index,
            ensure_ascii=False,
            separators=(
                ",",
                ":",
            ),
        )
        + "\n",
        encoding="utf-8",
    )

    return target


def _unique_volume_key(
    volume: dict[str, Any],
    fallback: str,
    used: set[str],
) -> str:
    base = _safe_key(
        str(
            volume.get("id")
            or volume.get(
                "document_code"
            )
            or volume.get(
                "source_folder"
            )
            or fallback
        )
    )

    candidate = base
    suffix = 2

    while candidate in used:
        candidate = (
            base
            + "-"
            + str(suffix)
        )
        suffix += 1

    used.add(candidate)
    return candidate


def _safe_key(
    value: str,
) -> str:
    cleaned = re.sub(
        r"[^A-Za-z0-9._-]+",
        "-",
        value.strip(),
    ).strip(
        "-."
    )

    return cleaned or "item"
