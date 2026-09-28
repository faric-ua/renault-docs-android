#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

PACKAGES_DIR="/storage/emulated/0/Documents/Renault/packages"
REQUESTED_DIR="${1:-}"

if [ -n "$REQUESTED_DIR" ]; then
  LATEST_DIR="$REQUESTED_DIR"
else
  LATEST_DIR="$(
    ls -1dt "$PACKAGES_DIR"/Renault-Docs-v*-build 2>/dev/null       | head -n 1       || true
  )"
fi

if [ -z "$LATEST_DIR" ] || [ ! -d "$LATEST_DIR" ]; then
  echo "Папку Renault Docs APK не знайдено."
  echo "Спочатку запусти Renault Menu → 8."
  exit 1
fi

APK="$(
  find "$LATEST_DIR"     -maxdepth 1     -type f     -name 'Renault-Docs-v*-debug.apk'     | head -n 1
)"

echo "Відкриваю папку останнього APK:"
echo "  $LATEST_DIR"

if [ -n "$APK" ]; then
  echo
  echo "APK:"
  echo "  $APK"
fi

BASENAME="$(basename "$LATEST_DIR")"
DOC_URI="content://com.android.externalstorage.documents/document/primary%3ADocuments%2FRenault%2Fpackages%2F$BASENAME"

# Prefer the system DocumentsUI/file manager at the exact build folder.
if am start   -a android.intent.action.VIEW   -d "$DOC_URI"   -t "vnd.android.document/directory"   >/dev/null 2>&1; then
  exit 0
fi

# Fallback: open the Android folder picker already positioned at the build folder.
if am start   -a android.intent.action.OPEN_DOCUMENT_TREE   --eu android.provider.extra.INITIAL_URI "$DOC_URI"   >/dev/null 2>&1; then
  exit 0
fi

# Last fallback: ask Android to open the APK itself, which also avoids manual search.
if [ -n "$APK" ] && command -v termux-open >/dev/null 2>&1; then
  termux-open "$APK"
  exit $?
fi

echo
echo "Android не зміг автоматично відкрити папку."
echo "Шлях:"
echo "  $LATEST_DIR"
exit 1
