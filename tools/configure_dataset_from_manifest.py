#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from pathlib import Path

DEFAULT_RENAULT_ROOT = Path("/storage/emulated/0/Documents/Renault")
REPO_ROOT = Path(__file__).resolve().parents[1]
CONFIG_PATH = REPO_ROOT / "config" / "current-device.json"


def dataset_candidates(root: Path) -> list[Path]:
    if not root.is_dir():
        return []

    result: list[Path] = []
    for manifest in root.glob("*/renault-dataset.json"):
        if not manifest.is_file():
            continue
        if "application" in manifest.parts:
            continue
        result.append(manifest.parent.resolve())
    return sorted(result, key=lambda p: p.name.casefold())


def load_manifest(root: Path) -> dict:
    path = root / "renault-dataset.json"
    if not path.is_file():
        raise SystemExit(f"Manifest not found: {path}")
    data = json.loads(path.read_text(encoding="utf-8"))

    required = ("id", "title", "manufacturer", "model", "entrypoint")
    missing = [key for key in required if not data.get(key)]
    if missing:
        raise SystemExit("Invalid dataset manifest; missing: " + ", ".join(missing))
    return data


def source_guess(build_root: Path) -> str:
    name = build_root.name
    if name.endswith("_android"):
        candidate = build_root.with_name(name[:-8])
        if candidate.is_dir():
            return str(candidate)
    return ""


def write_config(build_root: Path, manifest: dict) -> None:
    current = {}
    if CONFIG_PATH.is_file():
        current = json.loads(CONFIG_PATH.read_text(encoding="utf-8"))

    dataset = {
        "id": manifest["id"],
        "title": manifest["title"],
        "manufacturer": manifest.get("manufacturer", "Renault"),
        "model": manifest["model"],
        "content_type": manifest.get("content_type", "technical-documentation"),
        "entrypoint": manifest["entrypoint"],
        "viewer_profile": manifest.get("viewer_profile", "renault-legacy-web-v1"),
    }

    for key in ("platform", "years", "preview"):
        if key in manifest:
            dataset[key] = manifest[key]

    current.update(
        {
            "profile": "transferred-android-device",
            "repository_root": str(REPO_ROOT),
            "source_root": source_guess(build_root),
            "build_root": str(build_root),
            "dataset": dataset,
        }
    )

    CONFIG_PATH.parent.mkdir(parents=True, exist_ok=True)
    CONFIG_PATH.write_text(
        json.dumps(current, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    print("Browser dataset configured:")
    print(f"  {build_root}")
    print(f"  {manifest['title']}")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("dataset", nargs="?", type=Path)
    parser.add_argument("--auto", action="store_true")
    parser.add_argument("--root", type=Path, default=DEFAULT_RENAULT_ROOT)
    args = parser.parse_args()

    if args.dataset:
        build_root = args.dataset.resolve()
    elif args.auto:
        candidates = dataset_candidates(args.root)
        if not candidates:
            print("Dataset not found yet.")
            print("Copy a ready dataset under:")
            print(f"  {args.root}")
            return 2
        if len(candidates) > 1:
            print("Found multiple datasets; choose one explicitly:")
            for item in candidates:
                print(f"  {item}")
            print()
            print("Run:")
            print("  python tools/configure_dataset_from_manifest.py \"/path/to/dataset\"")
            return 3
        build_root = candidates[0]
    else:
        parser.error("provide dataset path or --auto")

    manifest = load_manifest(build_root)
    write_config(build_root, manifest)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
