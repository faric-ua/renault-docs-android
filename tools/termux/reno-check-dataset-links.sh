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

SOURCE_ROOT="$(
  python - "$CONFIG" <<'PY'
import json
import sys
from pathlib import Path

data = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
print(data.get("source_root", ""))
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
echo "Source:"
echo "  $SOURCE_ROOT"
echo
echo "Dataset:"
echo "  $BUILD_ROOT"
echo
echo "Звіт:"
echo "  $REPORT"
echo

checker_args=(
  python
  "$REPO/tools/check_dataset_links.py"
  "$BUILD_ROOT"
  --json-out "$REPORT"
)

if [ -n "$SOURCE_ROOT" ] && [ -d "$SOURCE_ROOT" ]; then
  checker_args+=(--source-root "$SOURCE_ROOT")
fi

"${checker_args[@]}"

status=$?
echo
if [ "$status" -eq 0 ]; then
  echo "PASS · посилання, томи та package metadata узгоджені."
elif [ "$status" -eq 1 ]; then
  echo "FAIL · integrity gate знайшов розбіжність. Нічого не змінено."
else
  echo "ERROR · checker не завершив перевірку."
fi

exit "$status"
