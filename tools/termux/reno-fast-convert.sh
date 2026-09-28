#!/data/data/com.termux/files/usr/bin/bash
set -u

REPO="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
ROOT="/storage/emulated/0/Documents/Renault"
LOG_DIR="$ROOT/packages/fast-converter-logs"

mkdir -p "$LOG_DIR"

echo "============================================================"
echo " Renault Fast Converter · direct filesystem"
echo "============================================================"
echo
echo "Цей режим НЕ використовує Android SAF."
echo "Він працює напряму з файлами через Termux/Python і має бути"
echo "значно швидшим на десятках тисяч дрібних файлів."
echo
echo "1 — Створити новий *_android dataset"
echo "2 — Додати відсутні томи в існуючий dataset"
echo "0 — Назад"
echo
printf "Вибір: "
read -r mode

case "$mode" in
  1|2) ;;
  0) exit 0 ;;
  *) echo "Невідомий режим."; exit 1 ;;
esac

echo
echo "Папки в Documents/Renault:"
find "$ROOT" -mindepth 1 -maxdepth 1 -type d -printf "  %f\n" 2>/dev/null | sort
echo
printf "Source folder name (наприклад Megane II): "
read -r source_name

if [ -z "$source_name" ]; then
  echo "Source не задано."
  exit 1
fi

SOURCE="$ROOT/$source_name"

if [ ! -d "$SOURCE" ]; then
  echo "Source не знайдено:"
  echo "  $SOURCE"
  exit 1
fi

default_output="${source_name}_android"
printf "Output folder name [%s]: " "$default_output"
read -r output_name
output_name="${output_name:-$default_output}"
OUTPUT="$ROOT/$output_name"

printf "Model/title [%s]: " "$source_name"
read -r model
model="${model:-$source_name}"

echo
echo "Проєкт для цього тому/dataset:"
echo "  1 — Megane II"
echo "  2 — Laguna II"
echo "  3 — Kangoo II"
echo "  4 — Інший / вручну"
printf "Вибір [4]: "
read -r project_choice
project_choice="${project_choice:-4}"

case "$project_choice" in
  1) project_id="megane-ii" ;;
  2) project_id="laguna-ii" ;;
  3) project_id="kangoo-ii" ;;
  4)
    printf "Project id [%s]: " "$(printf '%s' "$model" | tr '[:upper:]' '[:lower:]' | sed -E 's/[^a-z0-9]+/-/g; s/^-+|-+$//g')"
    read -r project_id
    if [ -z "$project_id" ]; then
      project_id="$(printf '%s' "$model" | tr '[:upper:]' '[:lower:]' | sed -E 's/[^a-z0-9]+/-/g; s/^-+|-+$//g')"
    fi
    ;;
  *)
    echo "Невідомий проєкт."
    exit 1
    ;;
esac

stamp="$(date '+%Y%m%d-%H%M%S')"
log_file="$LOG_DIR/fast-convert_${stamp}.log"

args=(
  python
  "$REPO/tools/fast_convert_dataset.py"
  --source "$SOURCE"
  --output "$OUTPUT"
  --model "$model"
  --title "$model"
  --project-id "$project_id"
)

if [ "$mode" = "2" ]; then
  args+=(--merge)
fi

echo
echo "Source:"
echo "  $SOURCE"
echo "Output:"
echo "  $OUTPUT"
echo "Project:"
echo "  $project_id"
echo "Mode:"
if [ "$mode" = "2" ]; then
  echo "  MERGE existing dataset"
else
  echo "  NEW dataset"
fi
echo "Log:"
echo "  $log_file"
echo
printf "Почати? [y/N]: "
read -r confirm

case "$confirm" in
  y|Y|yes|YES|так|Так|ТАК) ;;
  *) echo "Скасовано."; exit 0 ;;
esac

echo
echo "Запускаю..."
echo

"${args[@]}" 2>&1 | tee "$log_file"
status=${PIPESTATUS[0]}

echo
if [ "$status" -eq 0 ]; then
  echo "Fast Converter завершився успішно."
else
  echo "Fast Converter завершився з помилкою: $status"
fi
echo "Лог:"
echo "  $log_file"

exit "$status"
