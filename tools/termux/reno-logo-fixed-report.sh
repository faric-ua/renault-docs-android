#!/data/data/com.termux/files/usr/bin/bash
set -u

# Власник 2026-10-18 19:21 «давай уже заменим те томи»: 10 томів Laguna II з новим лого
# (папка logo-fixed після пункту 26) ідуть на Диск замість старих → каталог v6.
# Тут лише рахуємо відбитки (розмір, SHA-256) і пишемо logo-fixed-report.json поруч.
# Томи не змінюються і не видаляються.
REPO="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"

echo "============================================================"
echo " Renault Docs · Звіт для Диска: томи Laguna II з новим лого"
echo "============================================================"
echo
echo "Рахую відбитки томів у папці logo-fixed (кілька хвилин)…"
echo

cd "$REPO" || exit 1
python tools/logo_fixed_report.py
status=$?
echo
if [ "$status" -eq 0 ]; then
  echo "Далі: на Диск у папку «Laguna-II logo-fixed» — усі .rdpkg і logo-fixed-report.json з цієї папки."
fi
exit "$status"
