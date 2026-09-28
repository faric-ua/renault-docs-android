#!/data/data/com.termux/files/usr/bin/bash
set -e

REPO="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
RC_FILE="$HOME/.bashrc"

case "${SHELL:-}" in
  */zsh)
    RC_FILE="$HOME/.zshrc"
    ;;
esac

touch "$RC_FILE"

add_alias() {
  NAME="$1"
  COMMAND="$2"
  LINE="alias $NAME='$COMMAND'"

  # Replace any older Renault alias, including the legacy shared-storage path.
  sed -i "/^alias ${NAME}=/d" "$RC_FILE"
  printf '\n%s\n' "$LINE" >> "$RC_FILE"
  echo "Configured $NAME in $RC_FILE"
}

add_alias "reno-code" "cd \"$REPO\""
add_alias "reno-menu" "bash \"$REPO/menu.sh\""
add_alias "reno-docs" "bash \"$REPO/tools/termux/reno-docs.sh\""
add_alias "reno-browser" "bash \"$REPO/browser.sh\""
add_alias "reno-stop" "bash \"$REPO/tools/termux/reno-stop.sh\""
add_alias "reno-status" "bash \"$REPO/tools/termux/reno-status.sh\""
add_alias "reno-pdf-setup" "cd \"$REPO\" && python tools/install_pdfjs.py"
add_alias "reno-apk" "bash \"$REPO/tools/termux/reno-build-apk.sh\""
add_alias "reno-apk-latest" "bash \"$REPO/tools/termux/reno-download-apk.sh\""
add_alias "reno-modern" "cd \"$REPO\" && python tools/package_dataset.py"
add_alias "reno-fast" "bash \"$REPO/tools/termux/reno-fast-convert.sh\""

echo
echo "Activate the shortcuts now with:"
echo "  source \"$RC_FILE\""
echo
echo "Commands:"
echo "  reno-code       - open the Renault repository"
echo "  reno-menu       - open Renault Docs Menu"
echo "  reno-docs       - start/reuse server and open documentation"
echo "  reno-browser    - prepare everything and open browser version"
echo "  reno-stop       - stop the launcher-managed server"
echo "  reno-status     - show server status"
echo "  reno-pdf-setup  - install/update the local PDF.js runtime"
echo "  reno-apk        - build, download, verify and unpack the APK"
echo "  reno-apk-latest - download the latest successful APK build"
echo "  reno-modern     - rebuild package + Modern/Fast metadata"
echo "  reno-fast       - open the direct-filesystem fast converter"
