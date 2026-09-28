#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

from tools.export_runtime_section_bundle import (
    load_runtime_tree,
    pick_section,
    pick_volume,
)
from tools.prepare_from_config import load_config


def build_section_payload(
    runtime_tree: dict,
    volume: dict,
    section: dict,
) -> dict:
    return {
        "runtime_schema_version":
            runtime_tree.get("schema_version"),
        "compiler_phase":
            runtime_tree.get("compiler_phase"),
        "volume": {
            key: volume.get(key)
            for key in (
                "id",
                "title",
                "document_code",
                "date",
                "kind",
                "source_folder",
            )
        },
        "section": section,
    }


def main() -> int:
    parser = argparse.ArgumentParser(
        description=(
            "Export one compiled Runtime IR section as a small JSON file."
        )
    )
    parser.add_argument(
        "--config",
        type=Path,
        default=Path("config/current-device.json"),
    )
    parser.add_argument(
        "--volume",
        default="NT8183A",
    )
    parser.add_argument(
        "--section",
        default="101",
    )
    parser.add_argument(
        "--output",
        type=Path,
    )
    args = parser.parse_args()

    config = load_config(args.config.resolve())
    dataset_root = Path(config["build_root"]).resolve()

    runtime_tree = load_runtime_tree(dataset_root)
    volume = pick_volume(runtime_tree, args.volume)
    section = pick_section(volume, args.section)

    output = args.output
    if output is None:
        packages = dataset_root.parent / "packages"
        document_code = str(
            volume.get("document_code")
            or "volume"
        )
        output = (
            packages
            / (
                "Runtime-IR-"
                + document_code
                + "-"
                + str(section["code"])
                + "-section.json"
            )
        )

    output = output.resolve()
    output.parent.mkdir(
        parents=True,
        exist_ok=True,
    )

    payload = build_section_payload(
        runtime_tree=runtime_tree,
        volume=volume,
        section=section,
    )

    output.write_text(
        json.dumps(
            payload,
            ensure_ascii=False,
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )

    print("Runtime IR section export готовий:")
    print(f"  {output}")
    print()
    print(
        "Schema: "
        f"{payload['runtime_schema_version']}"
    )
    print(
        "Compiler phase: "
        f"{payload['compiler_phase']}"
    )
    print(
        "Controls: "
        f"{len(section.get('controls', []))}"
    )
    print(
        "Actions: "
        f"{len(section.get('actions', []))}"
    )
    print(
        "Documents: "
        f"{len(section.get('documents', []))}"
    )
    print(
        "Panels: "
        f"{len(section.get('panels', []))}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
