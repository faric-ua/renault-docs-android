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
        description="Create renault-dataset.json for an existing normalized dataset."
    )
    parser.add_argument("--config", type=Path, default=Path("config/current-device.json"))
    parser.add_argument("--report", type=Path, default=Path("analysis-out/conversion-report.json"))
    args = parser.parse_args()

    config = load_config(args.config.resolve())
    dataset = config.get("dataset")
    if not dataset:
        raise SystemExit("Config does not contain a dataset section")

    output = Path(config["build_root"])
    if not output.is_dir():
        raise SystemExit(f"Normalized dataset does not exist: {output}")

    summary = None
    if args.report.is_file():
        summary = json.loads(args.report.read_text(encoding="utf-8"))

    package = build_dataset_package(dataset, output, summary)
    print(f"Dataset manifest written: {package['manifest_path']}")
    print(f"Catalog written: {package['catalog_path']}")
    print(f"Help written: {package['readme_path']}")
    print(f"Volumes detected: {len(package['volumes'])}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
