#!/data/data/com.termux/files/usr/bin/bash
set -u

REPO="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
PACKAGE_DIR="/storage/emulated/0/Documents/Renault/packages/rdpkg"

echo "============================================================"
echo " Renault Docs · Міграція назв .rdpkg"
echo "============================================================"
echo
echo "Папка:"
echo "  $PACKAGE_DIR"
echo
echo "Спочатку лише перевіряю, що можна безпечно перейменувати."
echo

python "$REPO/tools/migrate_rdpkg_filenames.py"   --directory "$PACKAGE_DIR"
status=$?

if [ "$status" -eq 2 ]; then
  echo
  echo "Є невалідні/невідомі пакети. Автоматичне перейменування зупинено."
  exit 2
fi

if [ "$status" -eq 3 ]; then
  echo
  echo "Є конфлікти назв. Автоматичне перейменування зупинено."
  exit 3
fi

echo
printf "Enter — застосувати безпечні перейменування; 0 — назад: "
read -r confirm

case "$confirm" in
  "")
    ;;
  0)
    echo "Скасовано."
    exit 0
    ;;
  *)
    echo "Скасовано. Для запуску потрібно натиснути тільки Enter."
    exit 0
    ;;
esac

echo
python "$REPO/tools/migrate_rdpkg_filenames.py"   --directory "$PACKAGE_DIR"   --apply
status=$?

echo
if [ "$status" -eq 0 ]; then
  echo "Готово. Дані пакетів не змінювались — змінені лише безпечні імена файлів."
else
  echo "Міграція завершилась не повністю. Перевір повідомлення вище."
fi

exit "$status"
