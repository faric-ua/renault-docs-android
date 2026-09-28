#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import os
import stat
import zipfile
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
KIT_ROOT = REPO_ROOT / "transfer" / "browser-version"

TOP_LEVEL_FILES = (
    "browser.sh",
    "menu.sh",
    "convert.py",
)

DIRECTORIES = (
    "config",
    "core",
    "web",
    "tools",
)

EXCLUDED_PARTS = {
    ".git",
    "__pycache__",
    "vendor",
    "build",
}

ALLOWED_SUFFIXES = {
    ".py",
    ".sh",
    ".json",
    ".md",
    ".txt",
}


def runtime_files() -> list[Path]:
    files: list[Path] = []

    for name in TOP_LEVEL_FILES:
        path = REPO_ROOT / name
        if path.is_file():
            files.append(path)

    for directory in DIRECTORIES:
        root = REPO_ROOT / directory
        if not root.is_dir():
            continue
        for path in root.rglob("*"):
            if not path.is_file():
                continue
            rel = path.relative_to(REPO_ROOT)
            if any(part in EXCLUDED_PARTS for part in rel.parts):
                continue
            if path.suffix.lower() not in ALLOWED_SUFFIXES:
                continue
            files.append(path)

    return sorted(set(files))


def write_zip(output: Path) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    prefix = "Renault-Browser-Transfer-Kit"

    with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as zf:
        zf.write(KIT_ROOT / "install.sh", f"{prefix}/install.sh")
        zf.write(KIT_ROOT / "README_UA.txt", f"{prefix}/README_UA.txt")

        for path in runtime_files():
            rel = path.relative_to(REPO_ROOT).as_posix()
            zf.write(path, f"{prefix}/application/{rel}")

    digest = hashlib.sha256(output.read_bytes()).hexdigest()
    sha_path = output.with_suffix(output.suffix + ".sha256")
    sha_path.write_text(f"{digest}  {output.name}\n", encoding="utf-8")

    print(f"Archive: {output}")
    print(f"SHA256:  {digest}")
    print(f"Files:   {len(runtime_files()) + 2}")


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Build the portable Renault browser transfer ZIP."
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("/storage/emulated/0/Documents/Renault/packages/Renault-Browser-Transfer-Kit.zip"),
    )
    args = parser.parse_args()

    write_zip(args.output.resolve())
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
