#!/usr/bin/env python3
"""Check a published catalog against Google Drive, byte for byte (runs in GitHub Actions).

Owner 2026-10-18 «давай уже заменим те томи» (catalog v6). Claude's cloud copy cannot
reach Drive downloads, and the Drive connector cannot give a 70 MB file. The CI runner
can: it downloads through the same public link the app uses and compares.

  1. --manifest-id: the catalog file on Drive must be EXACTLY the committed --catalog file.
  2. every volume of --project (or all projects): the Drive file must have the size and
     SHA-256 the catalog promises — the same check the app does on every download.

Read-only: nothing is uploaded, changed or deleted. Exit 0 — all match, 2 — a mismatch.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import sys
import urllib.request
from pathlib import Path

URL = "https://drive.usercontent.google.com/download?id={id}&export=download&confirm=t"
CHUNK = 1024 * 1024


def fetch(file_id: str) -> tuple[int, str, bytes | None, bool]:
    """(size, sha256, first bytes, looks_like_html)."""
    digest = hashlib.sha256()
    size = 0
    head = b""
    request = urllib.request.Request(URL.format(id=file_id), headers={"User-Agent": "renodocs-ci-check"})
    with urllib.request.urlopen(request, timeout=120) as response:
        for chunk in iter(lambda: response.read(CHUNK), b""):
            if len(head) < 256:
                head += chunk[: 256 - len(head)]
            digest.update(chunk)
            size += len(chunk)
    html = head.lstrip().lower().startswith((b"<!doctype html", b"<html"))
    return size, digest.hexdigest(), head, html


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--catalog", type=Path, required=True)
    parser.add_argument("--manifest-id", default="")
    parser.add_argument("--project", default="", help="only this project id (default: all)")
    args = parser.parse_args(argv)

    local = args.catalog.read_bytes()
    catalog = json.loads(local.decode("utf-8"))
    failed = 0

    if args.manifest_id:
        size, sha, _, html = fetch(args.manifest_id)
        same = sha == hashlib.sha256(local).hexdigest() and size == len(local)
        print(("OK  " if same else "BAD ") + f"каталог на Диску ({size} байт) " +
              ("= файлу в репо" if same else "≠ файлу в репо" + (" (сторінка Google замість файлу)" if html else "")))
        failed += 0 if same else 1

    for project in catalog.get("projects") or []:
        if args.project and project.get("id") != args.project:
            continue
        for volume in project.get("volumes") or []:
            name = volume.get("file_name")
            try:
                size, sha, _, html = fetch(str(volume["drive_file_id"]))
            except Exception as error:  # network / 404
                print(f"BAD {name}: не скачався ({error})")
                failed += 1
                continue
            ok = size == volume.get("size_bytes") and sha == str(volume.get("sha256")).lower()
            print(("OK  " if ok else "BAD ") + f"{name}  {size} байт" + ("" if ok else f" (очікувалось {volume.get('size_bytes')}" + (", сторінка Google" if html else "") + ")"))
            failed += 0 if ok else 1

    print(f"Невідповідностей: {failed}")
    return 0 if failed == 0 else 2


if __name__ == "__main__":
    sys.exit(main())
