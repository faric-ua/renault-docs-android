#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

from core.dataset_package import build_dataset_package
from tools.prepare_from_config import load_config


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Add catalog, instructions, volume inventory and manifest to an existing converted Renault dataset."
    )
    parser.add_argument(
        "--config",
        type=Path,
        default=Path("config/current-device.json"),
    )
    parser.add_argument(
        "--report",
        type=Path,
        default=Path("analysis-out/conversion-report.json"),
    )
    args = parser.parse_args()

    config = load_config(args.config.resolve())
    dataset = config.get("dataset")
    if not dataset:
        raise SystemExit("Config does not contain dataset metadata")

    output = Path(config["build_root"])
    if not output.is_dir():
        raise SystemExit(f"Converted dataset does not exist: {output}")

    summary = None
    if args.report.is_file():
        summary = json.loads(args.report.read_text(encoding="utf-8"))

    print("Оновлюю Fast/Modern package...", flush=True)
    result = build_dataset_package(
        dataset,
        output,
        summary,
        progress=lambda message: print(
            message,
            flush=True,
        ),
    )

    print(f"Dataset: {dataset.get('title')}")
    print(f"Volumes detected: {len(result['volumes'])}")
    for volume in result["volumes"]:
        print(f"  - {volume['title']} -> {volume['entrypoint']}")

    print()
    print("Ready package files:")
    print(f"  Manifest: {result['manifest_path']}")
    print(f"  Catalog:  {result['catalog_path']}")
    print(f"  Help:     {result['readme_path']}")
    print(f"  Volumes:  {result['volumes_path']}")
    print(f"  Modern:   {result['modern_index_path']}")
    print(f"  Sections: {result['modern_sections_path']}")
    print(f"  Runtime:  {result['runtime_tree_path']}")
    print(f"  IR index: {result['runtime_ir_index_path']}")
    print(f"  Coverage: {result['runtime_ir_coverage_path']}")
    print(f"  Fast:     {result['fast_pack_path']}")
    print(
        f"             {result['fast_pack']['file_count']} files · "
        f"{result['fast_pack']['bytes']} bytes"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
