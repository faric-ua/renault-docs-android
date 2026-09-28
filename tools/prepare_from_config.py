#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

from core.dataset_package import build_dataset_package


def load_config(path: Path) -> dict:
    data = json.loads(path.read_text(encoding="utf-8"))
    for key in ("source_root", "build_root"):
        if not data.get(key):
            raise SystemExit(f"Missing required config key: {key}")
    return data


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Prepare a normalized Renault documentation dataset using a saved profile."
    )
    parser.add_argument(
        "--config",
        type=Path,
        default=Path("config/current-device.json"),
        help="JSON file with source_root, build_root and optional dataset metadata",
    )
    parser.add_argument(
        "--report",
        type=Path,
        default=Path("analysis-out/conversion-report.json"),
        help="Where to write the conversion report",
    )
    args = parser.parse_args()

    config_path = args.config.resolve()
    config = load_config(config_path)

    source = Path(config["source_root"])
    output = Path(config["build_root"])

    if not source.exists():
        raise SystemExit(
            "Source folder does not exist or is not accessible:\n"
            f"  {source}\n\n"
            "On Android/Termux make sure storage permission is granted."
        )

    if output.exists():
        raise SystemExit(
            "Build folder already exists. Rename/delete it first so the original build is not mixed "
            f"with a new one:\n  {output}"
        )

    converter = REPO_ROOT / "core" / "convert_paths.py"
    args.report.parent.mkdir(parents=True, exist_ok=True)

    cmd = [
        sys.executable,
        str(converter),
        str(source),
        "--output",
        str(output),
        "--report",
        str(args.report),
    ]

    print("Source:")
    print(f"  {source}")
    print("Output:")
    print(f"  {output}")
    print()
    print("Starting conversion...")

    completed = subprocess.run(cmd, check=False)
    if completed.returncode != 0:
        return completed.returncode

    dataset = config.get("dataset")
    if dataset:
        summary = json.loads(args.report.read_text(encoding="utf-8"))
        package = build_dataset_package(dataset, output, summary)
        print()
        print("Dataset package:")
        print(f"  Manifest: {package['manifest_path']}")
        print(f"  Catalog:  {package['catalog_path']}")
        print(f"  Help:     {package['readme_path']}")
        print(f"  Modern:   {package['modern_index_path']}")
        print(f"  Fast:     {package['fast_pack_path']}")
        print(f"  Volumes:  {len(package['volumes'])}")
    else:
        print()
        print("No dataset metadata in config; renault-dataset.json was not created.")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
