#!/data/data/com.termux/files/usr/bin/bash
set -eu

REPO="faric-ua/renault-docs-android"
SOURCE_PATH="tools/termux/phone-diagnostics-menu.sh"
BASE_DIR="/storage/emulated/0/Documents/PhoneDiagnostics"
INSTALL_DIR="$BASE_DIR/app"
MENU_PATH="$INSTALL_DIR/menu.sh"
SHORTCUT_DIR="$HOME/.shortcuts"
SHORTCUT_PATH="$SHORTCUT_DIR/Phone Diagnostics"
TMP_PATH="$INSTALL_DIR/menu.sh.tmp"

mkdir -p "$INSTALL_DIR" "$BASE_DIR/logs" "$BASE_DIR/state" "$SHORTCUT_DIR"
touch "$BASE_DIR/.nomedia"
chmod 700 "$SHORTCUT_DIR"

if ! command -v gh >/dev/null 2>&1; then
  echo "Не знайдено GitHub CLI (gh)."
  echo "Встанови його: pkg install gh"
  exit 1
fi

echo "Завантажую Phone Diagnostics..."
gh api "repos/$REPO/contents/$SOURCE_PATH" --jq .content   | tr -d '\n'   | base64 -d > "$TMP_PATH"

if ! grep -q "Phone Diagnostics Menu" "$TMP_PATH"; then
  echo "Помилка: отриманий файл не схожий на Phone Diagnostics Menu."
  rm -f "$TMP_PATH"
  exit 1
fi

mv "$TMP_PATH" "$MENU_PATH"

cat > "$SHORTCUT_PATH" <<EOF
#!/data/data/com.termux/files/usr/bin/bash
exec bash "$MENU_PATH"
EOF
chmod 700 "$SHORTCUT_PATH"

echo
echo "Готово."
echo
echo "Phone Diagnostics встановлено окремо від Renault:"
echo "  $BASE_DIR"
echo
echo "Меню:"
echo "  $MENU_PATH"
echo
echo "Логи:"
echo "  $BASE_DIR/logs"
echo
echo "Termux:Widget shortcut:"
echo "  Phone Diagnostics"
echo
echo "Для оновлення Phone Diagnostics просто запусти цей installer ще раз."
echo "Renault Menu для цього не потрібне."
