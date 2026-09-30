#!/data/data/com.termux/files/usr/bin/bash
set -u

REPO="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
CONFIG="$REPO/config/current-device.json"
REPORT_DIR="/storage/emulated/0/Documents/Renault/reports"

if [ ! -f "$CONFIG" ]; then
  echo "Не знайдено config/current-device.json."
  exit 1
fi

BUILD_ROOT="$(
  python - "$CONFIG" <<'PY'
import json
import sys
from pathlib import Path

data = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
print(data.get("build_root", ""))
PY
)"

if [ -z "$BUILD_ROOT" ] || [ ! -d "$BUILD_ROOT" ]; then
  echo "Активний build_root не знайдено:"
  echo "  $BUILD_ROOT"
  exit 1
fi

mkdir -p "$REPORT_DIR"
STAMP="$(date +%Y%m%d-%H%M%S)"
REPORT="$REPORT_DIR/dataset-links-$STAMP.json"

echo "========================================"
echo " Renault Docs · Dataset link check"
echo " READ-ONLY"
echo "========================================"
echo
echo "Dataset:"
echo "  $BUILD_ROOT"
echo
echo "Звіт:"
echo "  $REPORT"
echo

python "$REPO/tools/check_dataset_links.py" \
  "$BUILD_ROOT" \
  --json-out "$REPORT"

status=$?
echo
if [ "$status" -eq 0 ]; then
  echo "PASS · локальні посилання не мають missing targets."
elif [ "$status" -eq 1 ]; then
  echo "FAIL · знайдено missing targets. Нічого не змінено."
else
  echo "ERROR · checker не завершив перевірку."
fi

exit "$status"
