#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

REPO="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
ROOT="/storage/emulated/0/Documents/Renault"
PACKAGES_DIR="$ROOT/packages/rdpkg"
PLAN="$REPO/config/catalog-publish-plan.v4.json"
OUTPUT_DIR="$ROOT/packages/catalog"
OUTPUT="$OUTPUT_DIR/renault-docs-catalog.json"

mkdir -p "$OUTPUT_DIR"

echo "============================================================"
echo " Renault Docs · Public Catalog v4"
echo "============================================================"
echo
echo "Пакети:"
echo "  $PACKAGES_DIR"
echo
echo "Publish plan:"
echo "  $PLAN"
echo
echo "Вихід:"
echo "  $OUTPUT"
echo
echo "Перевіряю 15 canonical .rdpkg і рахую SHA-256..."
echo

cd "$REPO"

python tools/build_drive_catalog.py \
  --packages-dir "$PACKAGES_DIR" \
  --publish-plan "$PLAN" \
  --output "$OUTPUT"

echo

python - "$OUTPUT" <<'PY'
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

path = Path(sys.argv[1])
catalog = json.loads(path.read_text(encoding="utf-8"))

if catalog.get("catalog_version") != 4:
    raise SystemExit("Catalog verification failed: catalog_version != 4")

projects = catalog.get("projects")
if not isinstance(projects, list):
    raise SystemExit("Catalog verification failed: projects is not a list")

expected = {
    "laguna-ii": ("Laguna II", 10),
    "megane-ii": ("Megane II", 5),
}

actual = {}
for project in projects:
    project_id = str(project.get("id") or "")
    title = str(project.get("title") or "")
    volumes = project.get("volumes")
    if not isinstance(volumes, list):
        raise SystemExit(
            f"Catalog verification failed: {project_id} volumes is not a list"
        )
    actual[project_id] = (title, len(volumes))

    for volume in volumes:
        codes = volume.get("vehicle_codes")
        if not isinstance(codes, list) or not codes:
            raise SystemExit(
                "Catalog verification failed: missing vehicle_codes for "
                + str(volume.get("document_code") or volume.get("id") or "?")
            )

        digest = str(volume.get("sha256") or "")
        if not re.fullmatch(r"[0-9a-f]{64}", digest):
            raise SystemExit(
                "Catalog verification failed: missing/invalid SHA-256 for "
                + str(volume.get("document_code") or volume.get("id") or "?")
            )

if actual != expected:
    raise SystemExit(
        "Catalog verification failed: expected "
        + repr(expected)
        + ", got "
        + repr(actual)
    )

print("Перевірка catalog: PASS")
print("  Laguna II: 10 томів")
print("  Megane II: 5 томів")
print("  Vehicle codes: 15/15")
print("  SHA-256: 15/15")
PY

echo
echo "Catalog SHA-256:"
sha256sum "$OUTPUT"
echo
echo "Готово."
echo "Файл:"
echo "  $OUTPUT"
echo
echo "Цей крок нічого не завантажує в Google Drive."
