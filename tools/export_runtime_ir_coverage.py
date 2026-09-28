#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import shutil
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

from core.runtime_ir_coverage import RUNTIME_IR_COVERAGE_FILENAME
from tools.prepare_from_config import load_config


def main() -> int:
    parser = argparse.ArgumentParser(
        description=(
            "Copy the generated Runtime IR coverage audit into Renault/packages."
        )
    )
    parser.add_argument(
        "--config",
        type=Path,
        default=Path("config/current-device.json"),
    )
    parser.add_argument(
        "--output",
        type=Path,
    )
    args = parser.parse_args()

    config = load_config(args.config.resolve())
    dataset_root = Path(config["build_root"]).resolve()

    source = (
        dataset_root
        / "_renault"
        / RUNTIME_IR_COVERAGE_FILENAME
    )
    if not source.is_file():
        raise SystemExit(
            "Runtime IR coverage не знайдено. "
            "Спочатку запусти Renault Menu → 9."
        )

    output = args.output
    if output is None:
        output = (
            dataset_root.parent
            / "packages"
            / "Runtime-IR-Coverage.json"
        )

    output = output.resolve()
    output.parent.mkdir(
        parents=True,
        exist_ok=True,
    )
    shutil.copyfile(source, output)

    data = json.loads(
        source.read_text(encoding="utf-8")
    )

    print("Runtime IR coverage export готовий:")
    print(f"  {output}")
    print()
    print(
        "Schema: "
        f"{data.get('runtime_schema_version')}"
    )
    print(
        "Volumes: "
        f"{data.get('volume_count')}"
    )
    print(
        "Sections: "
        f"{data.get('section_count')}"
    )
    print(
        "Unsupported actions: "
        f"{data.get('unsupported_action_count')}"
    )
    print(
        "Warnings: "
        f"{data.get('warning_count')}"
    )

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
