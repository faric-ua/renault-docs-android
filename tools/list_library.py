#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

from core.library import scan_library


def main() -> int:
    parser = argparse.ArgumentParser(description="List converted Renault datasets in a library folder.")
    parser.add_argument(
        "root",
        nargs="?",
        type=Path,
        default=Path("/storage/emulated/0/Documents/Renault"),
    )
    args = parser.parse_args()

    datasets = scan_library(args.root)
    print(json.dumps(datasets, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
