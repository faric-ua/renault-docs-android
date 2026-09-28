#!/data/data/com.termux/files/usr/bin/bash
set -eu

REPO="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
CONFIG="$REPO/config/current-device.json"

cd "$REPO"

if ! command -v python >/dev/null 2>&1; then
  echo "Python не знайдено."
  echo "Для browser development mode у Termux потрібен Python."
  exit 1
fi

if [ ! -f "$CONFIG" ]; then
  echo "Не знайдено config/current-device.json"
  exit 1
fi

BUILD_ROOT="$(python - "$CONFIG" <<'PY'
import json
import sys
from pathlib import Path

data = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
print(data["build_root"])
PY
)"

if [ ! -d "$BUILD_ROOT" ]; then
  echo "Готовий dataset не знайдено:"
  echo "  $BUILD_ROOT"
  echo
  echo "Спочатку виконай:"
  echo "  python convert.py"
  exit 1
fi

if [ ! -f "$BUILD_ROOT/renault-dataset.json" ] || [ ! -f "$BUILD_ROOT/_renault/START.html" ]; then
  echo "Оновлюю dataset package..."
  python tools/package_dataset.py
  echo
fi

if ! python tools/install_pdfjs.py --check >/dev/null 2>&1; then
  echo "PDF.js ще не встановлений. Встановлюю..."
  python tools/install_pdfjs.py
  echo
fi

exec bash "$REPO/tools/termux/reno-docs.sh"
