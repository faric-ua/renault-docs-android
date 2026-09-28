#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

KIT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
PAYLOAD="$KIT_DIR/application"
DEST="${RENAULT_REPO_DIR:-$HOME/renault-docs-android}"
RENAULT_ROOT="/storage/emulated/0/Documents/Renault"

echo "Renault Browser Transfer Kit"
echo "============================"
echo

if ! command -v pkg >/dev/null 2>&1; then
  echo "Цей installer потрібно запускати в Termux."
  exit 1
fi

echo "[1/6] Termux packages..."
pkg update -y
pkg install -y python git gh unzip

echo
echo "[2/6] Storage access..."
if [ ! -d "$HOME/storage/shared" ]; then
  termux-setup-storage || true
  echo "Якщо Android показав запит доступу до файлів — дозволь його."
  for _ in $(seq 1 20); do
    [ -d "$HOME/storage/shared" ] && break
    sleep 1
  done
fi

if [ ! -d "/storage/emulated/0/Documents" ]; then
  echo "Termux не бачить /storage/emulated/0/Documents."
  echo "Надай Termux доступ до файлів і запусти installer ще раз."
  exit 1
fi

echo
echo "[3/6] Renault application files..."
mkdir -p "$RENAULT_ROOT"

if [ -d "$DEST" ] && find "$DEST" -mindepth 1 -maxdepth 1 -print -quit | grep -q .; then
  if [ ! -f "$DEST/.renault-browser-transfer-kit" ]; then
    echo "Папка вже існує і не схожа на install цього transfer-kit:"
    echo "  $DEST"
    echo "Нічого не перезаписано."
    exit 1
  fi
fi

mkdir -p "$DEST"
cp -a "$PAYLOAD/." "$DEST/"
printf '%s\n' "Renault Browser Transfer Kit" > "$DEST/.renault-browser-transfer-kit"

echo
echo "[4/6] Dataset auto-detection..."
python "$DEST/tools/configure_dataset_from_manifest.py" --auto || true

echo
echo "[5/6] PDF.js..."
cd "$DEST"
python tools/install_pdfjs.py

echo
echo "[6/6] Termux shortcuts..."
bash tools/install_termux_aliases.sh
bash tools/install_termux_widget.sh

echo
echo "========================================"
echo "ГОТОВО"
echo "========================================"
echo
echo "Application:"
echo "  $DEST"
echo
echo "Якщо dataset уже скопійований у Documents/Renault, спробуй:"
echo "  source ~/.bashrc"
echo "  reno-browser"
echo
echo "На головному екрані Android онови Termux:Widget."
echo "Якщо dataset ще не перенесений — скопіюй його у Documents/Renault,"
echo "потім виконай:"
echo "  python \"$DEST/tools/configure_dataset_from_manifest.py\" --auto"
