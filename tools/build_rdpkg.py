#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

from core.dataset_manifest import load_manifest
from core.rdpkg import (
    build_rdpkg,
    default_package_filename,
    list_packageable_volumes,
    select_volume,
)


def log(message: str) -> None:
    print(message, flush=True)


def main() -> int:
    parser = argparse.ArgumentParser(
        description=(
            "Build one portable Renault Docs .rdpkg file "
            "from one volume inside a prepared *_android dataset."
        )
    )
    parser.add_argument(
        "--source",
        type=Path,
        required=True,
        help="Prepared dataset root containing renault-dataset.json.",
    )
    parser.add_argument(
        "--volume",
        help=(
            "Volume selector: document code, id, source folder or exact title. "
            "May be omitted when the dataset contains only one volume."
        ),
    )
    parser.add_argument(
        "--output",
        type=Path,
        help="Exact .rdpkg output path.",
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        help="Directory for an automatically named .rdpkg file.",
    )
    parser.add_argument(
        "--list",
        action="store_true",
        help="List packageable volumes and exit.",
    )
    args = parser.parse_args()

    source = args.source.expanduser().resolve()
    volumes = list_packageable_volumes(
        source,
    )

    if args.list:
        for volume in volumes:
            print(
                "\t".join(
                    [
                        str(
                            volume.get(
                                "document_code",
                                "",
                            )
                        ),
                        str(
                            volume.get(
                                "date",
                                "",
                            )
                        ),
                        str(
                            volume.get(
                                "id",
                                "",
                            )
                        ),
                        str(
                            volume.get(
                                "source_folder",
                                "",
                            )
                        ),
                    ]
                )
            )
        return 0

    if args.output is not None and args.output_dir is not None:
        parser.error(
            "Use only one of --output or --output-dir."
        )

    selected = select_volume(
        volumes,
        args.volume,
    )
    dataset = load_manifest(
        source / "renault-dataset.json",
    )

    if args.output is not None:
        output = args.output.expanduser().resolve()
    else:
        output_dir = (
            args.output_dir.expanduser().resolve()
            if args.output_dir is not None
            else source.parent / "packages" / "rdpkg"
        )
        output = (
            output_dir
            / default_package_filename(
                dataset,
                selected,
            )
        )

    if output.suffix.lower() != ".rdpkg":
        output = output.with_suffix(
            ".rdpkg",
        )

    result = build_rdpkg(
        source,
        output,
        volume_selector=args.volume,
        progress=log,
    )

    print("")
    print("============================================================")
    print("READY · RDPKG")
    print("============================================================")
    print("Файл:")
    print(f"  {result['path']}")
    print("Том:")
    volume = result["volume"]
    label = " · ".join(
        str(part)
        for part in (
            volume.get("document_code")
            or volume.get("title"),
            volume.get("date"),
        )
        if part
    )
    print(f"  {label}")
    print("Project:")
    print(f"  {result.get('project_id') or '-'}")
    print("SHA-256:")
    print(f"  {result['sha256']}")
    print("Bytes:")
    print(f"  {result['bytes']}")
    print("Payload files:")
    print(f"  {result['payload_file_count']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(
        main(),
    )
