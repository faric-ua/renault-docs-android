#!/data/data/com.termux/files/usr/bin/bash
set -u

REPO="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
ROOT="/storage/emulated/0/Documents/Renault"
OUTPUT_DIR="$ROOT/packages/rdpkg"

mkdir -p "$OUTPUT_DIR"

choose_number() {
  local max="$1"
  local answer
  local number

  while true; do
    printf "Вибір: "
    if ! read -r answer; then
      exit 0
    fi

    if [ "$answer" = "0" ]; then
      echo "Скасовано."
      exit 0
    fi

    case "$answer" in
      ''|*[!0-9]*)
        echo "Введи номер зі списку."
        continue
        ;;
    esac

    number=$((10#$answer))
    if (( number >= 1 && number <= max )); then
      CHOICE_INDEX=$((number - 1))
      return 0
    fi

    echo "Немає такого пункту. Вибери 1–$max або 0."
  done
}

parse_volume_line() {
  VOLUME_CODE=""
  VOLUME_DATE=""
  VOLUME_ID=""
  VOLUME_SOURCE=""
  VOLUME_SELECTOR=""
  VOLUME_LABEL=""

  IFS=$'\t' read -r     VOLUME_CODE     VOLUME_DATE     VOLUME_ID     VOLUME_SOURCE     <<< "$1"

  if [ -n "$VOLUME_CODE" ]; then
    VOLUME_SELECTOR="$VOLUME_CODE"
  elif [ -n "$VOLUME_ID" ]; then
    VOLUME_SELECTOR="$VOLUME_ID"
  else
    VOLUME_SELECTOR="$VOLUME_SOURCE"
  fi

  VOLUME_LABEL="$VOLUME_SELECTOR"
  if [ -n "$VOLUME_DATE" ]; then
    VOLUME_LABEL="$VOLUME_LABEL · $VOLUME_DATE"
  fi
}

echo "============================================================"
echo " Renault Docs · Build .rdpkg"
echo "============================================================"
echo
echo "Один .rdpkg = один том Renault Docs."
echo

dataset_paths=()
shopt -s nullglob
for candidate in "$ROOT"/*_android; do
  if [ -d "$candidate" ] && [ -f "$candidate/renault-dataset.json" ]; then
    dataset_paths+=("$candidate")
  fi
done
shopt -u nullglob

if [ "${#dataset_paths[@]}" -eq 0 ]; then
  echo "Не знайдено жодного готового *_android dataset"
  echo "з файлом renault-dataset.json у:"
  echo "  $ROOT"
  exit 1
fi

if [ "${#dataset_paths[@]}" -eq 1 ]; then
  SOURCE="${dataset_paths[0]}"
  echo "Dataset:"
  echo "  $(basename -- "$SOURCE")"
  echo "  (вибрано автоматично)"
else
  echo "Вибери dataset номером:"
  for i in "${!dataset_paths[@]}"; do
    printf "  %d — %s\n"       "$((i + 1))"       "$(basename -- "${dataset_paths[$i]}")"
  done
  echo "  0 — Назад"
  echo

  choose_number "${#dataset_paths[@]}"
  SOURCE="${dataset_paths[$CHOICE_INDEX]}"

  echo
  echo "Dataset:"
  echo "  $(basename -- "$SOURCE")"
fi

echo
echo "Читаю список томів..."

volume_output="$(python "$REPO/tools/build_rdpkg.py" --source "$SOURCE" --list)"
list_status=$?
if [ "$list_status" -ne 0 ]; then
  exit "$list_status"
fi

volume_lines=()
while IFS= read -r line; do
  if [ -n "$line" ]; then
    volume_lines+=("$line")
  fi
done <<< "$volume_output"

if [ "${#volume_lines[@]}" -eq 0 ]; then
  echo "У dataset не знайдено томів, які можна запакувати."
  exit 1
fi

BUILD_ALL=0

if [ "${#volume_lines[@]}" -eq 1 ]; then
  parse_volume_line "${volume_lines[0]}"
  echo
  echo "Том:"
  echo "  $VOLUME_LABEL"
  echo "  (вибрано автоматично)"
else
  echo
  echo "Вибери том номером:"
  for i in "${!volume_lines[@]}"; do
    parse_volume_line "${volume_lines[$i]}"
    printf "  %d — %s\n" "$((i + 1))" "$VOLUME_LABEL"
  done
  echo "  A — Усі томи окремими .rdpkg"
  echo "  0 — Назад"
  echo

  while true; do
    printf "Вибір: "
    if ! read -r answer; then
      exit 0
    fi

    case "$answer" in
      a|A)
        BUILD_ALL=1
        break
        ;;
      0)
        echo "Скасовано."
        exit 0
        ;;
      ''|*[!0-9]*)
        echo "Введи номер, A або 0."
        ;;
      *)
        number=$((10#$answer))
        if (( number >= 1 && number <= ${#volume_lines[@]} )); then
          CHOICE_INDEX=$((number - 1))
          parse_volume_line "${volume_lines[$CHOICE_INDEX]}"
          break
        fi
        echo "Немає такого пункту."
        ;;
    esac
  done

  echo
  if [ "$BUILD_ALL" -eq 1 ]; then
    echo "Томи:"
    echo "  усі ${#volume_lines[@]} · кожен окремим .rdpkg"
  else
    echo "Том:"
    echo "  $VOLUME_LABEL"
  fi
fi

args=(
  python
  "$REPO/tools/build_rdpkg.py"
  --source "$SOURCE"
  --output-dir "$OUTPUT_DIR"
)

if [ "$BUILD_ALL" -eq 1 ]; then
  args+=(--all)
else
  if [ -z "$VOLUME_SELECTOR" ]; then
    echo "Не вдалося визначити ідентифікатор вибраного тому."
    exit 1
  fi
  args+=(--volume "$VOLUME_SELECTOR")
fi

echo
echo "Буде створено:"
echo "  Dataset: $(basename -- "$SOURCE")"
if [ "$BUILD_ALL" -eq 1 ]; then
  echo "  Томи:    усі ${#volume_lines[@]} окремими пакетами"
else
  echo "  Том:     $VOLUME_LABEL"
fi
echo "  Папка:   $OUTPUT_DIR"
echo

while true; do
  printf "Enter — створити .rdpkg; 0 — назад: "
  if ! read -r confirm; then
    exit 0
  fi

  case "$confirm" in
    '')
      break
      ;;
    0)
      echo "Скасовано."
      exit 0
      ;;
    *)
      echo "Для створення просто натисни Enter або 0 для виходу."
      ;;
  esac
done

echo
"${args[@]}"
status=$?

echo
if [ "$status" -eq 0 ]; then
  if [ "$BUILD_ALL" -eq 1 ]; then
    echo "Усі пакети готові."
  else
    echo "Пакет готовий."
  fi
  echo "Папка:"
  echo "  $OUTPUT_DIR"
else
  echo "Не вдалося створити .rdpkg."
fi

exit "$status"
