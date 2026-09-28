#!/data/data/com.termux/files/usr/bin/bash
set -eu

RENAULT_REPO="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
YTM_REPO="/storage/emulated/0/Documents/YTM"
SHORTCUT_DIR="$HOME/.shortcuts"

mkdir -p "$SHORTCUT_DIR"
chmod 700 "$SHORTCUT_DIR"

# Remove previous Renault and YTM shortcuts created by this project.
rm -f   "$SHORTCUT_DIR/Renault-Menu"   "$SHORTCUT_DIR/Renault-Docs"   "$SHORTCUT_DIR/Renault-Stop"   "$SHORTCUT_DIR/Renault/Code"   "$SHORTCUT_DIR/Renault/Menu"   "$SHORTCUT_DIR/Renault/Browser"   "$SHORTCUT_DIR/Renault/Stop"   "$SHORTCUT_DIR/YTM Importer/Code"   "$SHORTCUT_DIR/YTM Importer/Build APK"   "$SHORTCUT_DIR/YTM Importer/Download APK"

rmdir "$SHORTCUT_DIR/Renault" 2>/dev/null || true
rmdir "$SHORTCUT_DIR/YTM Importer" 2>/dev/null || true

cat > "$SHORTCUT_DIR/Renault" <<EOF
#!/data/data/com.termux/files/usr/bin/bash
exec bash "$RENAULT_REPO/menu.sh"
EOF

cat > "$SHORTCUT_DIR/YTM Importer" <<EOF
#!/data/data/com.termux/files/usr/bin/bash
exec bash "$YTM_REPO/tools/termux/ytm-menu.sh"
EOF

chmod 700   "$SHORTCUT_DIR/Renault"   "$SHORTCUT_DIR/YTM Importer"

echo "Termux:Widget shortcuts installed:"
echo
echo "  Renault"
echo "  YTM Importer"
echo
echo "Phone Diagnostics має власний окремий installer"
echo "і не керується Renault/YTM installer."
echo
echo "Tap a project name to open its own menu."
echo "Press the refresh button on the Termux:Widget."
