#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import sys
import tempfile
import urllib.request
import zipfile
from datetime import datetime, timezone
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

from web.pdf_support import (
    PDFJS_ARCHIVE_SHA256,
    PDFJS_DIST_URL,
    PDFJS_VERSION,
    pdfjs_vendor_ready,
)


ALLOWED_PREFIXES = (
    "build/",
    "cmaps/",
    "standard_fonts/",
    "wasm/",
)
ALLOWED_FILES = {"LICENSE"}


def download(url: str, target: Path) -> str:
    request = urllib.request.Request(
        url,
        headers={"User-Agent": "Renault-Docs-PDFJS-Installer/1.0"},
    )
    digest = hashlib.sha256()

    with urllib.request.urlopen(request, timeout=60) as response, target.open("wb") as out:
        while True:
            chunk = response.read(1024 * 1024)
            if not chunk:
                break
            out.write(chunk)
            digest.update(chunk)

    return digest.hexdigest()


def extract_distribution(archive: Path, target: Path) -> None:
    target.mkdir(parents=True, exist_ok=True)

    with zipfile.ZipFile(archive) as zf:
        for member in zf.infolist():
            name = member.filename.replace("\\", "/").lstrip("/")
            if not name or name.endswith("/"):
                continue

            if name not in ALLOWED_FILES and not name.startswith(ALLOWED_PREFIXES):
                continue

            destination = (target / name).resolve()
            target_resolved = target.resolve()
            if target_resolved not in destination.parents and destination != target_resolved:
                raise RuntimeError(f"Unsafe archive member: {member.filename}")

            destination.parent.mkdir(parents=True, exist_ok=True)
            with zf.open(member) as source, destination.open("wb") as out:
                shutil.copyfileobj(source, out)


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Install the pinned local PDF.js runtime for Renault Docs browser mode."
    )
    parser.add_argument(
        "--target",
        type=Path,
        default=REPO_ROOT / "web" / "vendor" / "pdfjs",
    )
    parser.add_argument(
        "--check",
        action="store_true",
        help="Only verify whether the pinned PDF.js runtime is installed.",
    )
    parser.add_argument(
        "--force",
        action="store_true",
        help="Replace an existing local PDF.js runtime.",
    )
    args = parser.parse_args()

    target = args.target.resolve()

    if args.check:
        if pdfjs_vendor_ready(target):
            print(f"PDF.js {PDFJS_VERSION} is ready: {target}")
            return 0
        print(f"PDF.js is not ready: {target}")
        return 1

    if pdfjs_vendor_ready(target) and not args.force:
        print(f"PDF.js {PDFJS_VERSION} is already installed:")
        print(f"  {target}")
        return 0

    if target.exists() and args.force:
        shutil.rmtree(target)

    print(f"Downloading PDF.js {PDFJS_VERSION}...")
    print(f"  {PDFJS_DIST_URL}")

    with tempfile.TemporaryDirectory(prefix="renault-pdfjs-") as tmp:
        archive = Path(tmp) / "pdfjs.zip"
        sha256 = download(PDFJS_DIST_URL, archive)
        if sha256.lower() != PDFJS_ARCHIVE_SHA256.lower():
            raise SystemExit(
                "PDF.js archive SHA-256 mismatch. "
                f"Expected {PDFJS_ARCHIVE_SHA256}, got {sha256}. "
                "Nothing was installed."
            )
        extract_distribution(archive, target)

    if not pdfjs_vendor_ready(target):
        raise SystemExit(
            "PDF.js archive was downloaded but required build/pdf.mjs and "
            "build/pdf.worker.mjs were not found."
        )

    metadata = {
        "version": PDFJS_VERSION,
        "source_url": PDFJS_DIST_URL,
        "archive_sha256": sha256,
        "expected_archive_sha256": PDFJS_ARCHIVE_SHA256,
        "installed_at_utc": datetime.now(timezone.utc).isoformat(),
    }
    (target / "INSTALL.json").write_text(
        json.dumps(metadata, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    print()
    print("PDF.js installed:")
    print(f"  {target}")
    print(f"Archive SHA-256: {sha256}")
    print()
    print("Restart reno-docs and open a PDF link again.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
