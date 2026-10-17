#!/data/data/com.termux/files/usr/bin/bash
set -u

# TODO №26 (власник 2026-10-18): у томах Laguna II зіпсоване лого стартової сторінки
# (ICONES/LOGO.GIF, 2023). Пишемо НОВІ файли з нашим лого в папку logo-fixed поруч;
# оригінали не змінюються.
REPO="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
LOGO="$REPO/docs/brand/renodocs-logo.gif"

echo "============================================================"
echo " Renault Docs · Замінити зіпсоване лого (Laguna II)"
echo "============================================================"
echo
echo "Спочатку лише шукаю і показую — нічого не змінюю."
echo

cd "$REPO" || exit 1
python tools/fix_volume_logo.py --logo "$LOGO"
status=$?
[ "$status" -eq 0 ] || exit "$status"

echo
printf "Enter — зробити нові файли з новим лого; 0 — назад: "
read -r confirm
case "$confirm" in
  "") ;;
  *)
    echo "Скасовано."
    exit 0
    ;;
esac

echo
python tools/fix_volume_logo.py --logo "$LOGO" --apply
