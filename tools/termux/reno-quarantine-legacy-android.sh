#!/data/data/com.termux/files/usr/bin/bash
set -u

ROOT="${RENAULT_STORAGE_ROOT:-/storage/emulated/0/Documents/Renault}"
QUARANTINE="${RENAULT_LEGACY_QUARANTINE:-$ROOT/legacy-quarantine}"
MANIFEST="$QUARANTINE/manifest.tsv"
MODE="${1:-interactive}"

print_header() {
  echo "========================================"
  echo " Renault Docs · Legacy quarantine"
  echo " НІЧОГО НЕ ВИДАЛЯЄТЬСЯ"
  echo "========================================"
  echo
  echo "Renault root:"
  echo "  $ROOT"
  echo "Quarantine:"
  echo "  $QUARANTINE"
  echo
}

collect_sources() {
  shopt -s nullglob
  SOURCES=("$ROOT"/*_android)
}

collect_archived() {
  shopt -s nullglob
  ARCHIVED=("$QUARANTINE"/*_android)
}

show_plan() {
  collect_sources
  echo "План переміщення:"
  if [ "${#SOURCES[@]}" -eq 0 ]; then
    echo "  top-level *_android не знайдено."
  else
    for src in "${SOURCES[@]}"; do
      [ -d "$src" ] || continue
      base="$(basename "$src")"
      size="$(du -sh -- "$src" 2>/dev/null | awk '{print $1}')"
      files="$(find "$src" -type f -print 2>/dev/null | wc -l | tr -d ' ')"
      echo "  $base"
      echo "    from: $src"
      echo "    to:   $QUARANTINE/$base"
      echo "    size: ${size:-?} · files: ${files:-?}"
    done
  fi
  echo
  echo "Це тільки MOVE у quarantine. rm/rmdir/delete тут немає."
}

move_to_quarantine() {
  collect_sources
  if [ "${#SOURCES[@]}" -eq 0 ]; then
    echo "Немає top-level *_android для перенесення."
    return 0
  fi

  mkdir -p -- "$QUARANTINE"
  if [ ! -f "$MANIFEST" ]; then
    printf 'timestamp\taction\tfrom\tto\n' > "$MANIFEST"
  fi

  for src in "${SOURCES[@]}"; do
    [ -d "$src" ] || continue
    base="$(basename "$src")"
    dst="$QUARANTINE/$base"

    if [ -e "$dst" ]; then
      echo "SKIP: destination already exists:"
      echo "  $dst"
      continue
    fi

    before_files="$(find "$src" -type f -print 2>/dev/null | wc -l | tr -d ' ')"
    before_bytes="$(du -sb -- "$src" 2>/dev/null | awk '{print $1}')"

    echo "MOVE:"
    echo "  $src"
    echo "  → $dst"
    mv -- "$src" "$dst"

    after_files="$(find "$dst" -type f -print 2>/dev/null | wc -l | tr -d ' ')"
    after_bytes="$(du -sb -- "$dst" 2>/dev/null | awk '{print $1}')"

    if [ "$before_files" != "$after_files" ] || [ "$before_bytes" != "$after_bytes" ]; then
      echo "VERIFY FAIL: count/size changed. Restoring..."
      mv -- "$dst" "$src"
      return 1
    fi

    printf '%s\tMOVE\t%s\t%s\n' "$(date -Iseconds)" "$src" "$dst" >> "$MANIFEST"
    echo "  OK · ${after_files:-?} files · ${after_bytes:-?} bytes"
  done
}

restore_from_quarantine() {
  collect_archived
  if [ "${#ARCHIVED[@]}" -eq 0 ]; then
    echo "У quarantine немає *_android для restore."
    return 0
  fi

  for src in "${ARCHIVED[@]}"; do
    [ -d "$src" ] || continue
    base="$(basename "$src")"
    dst="$ROOT/$base"

    if [ -e "$dst" ]; then
      echo "SKIP: original path already exists:"
      echo "  $dst"
      continue
    fi

    echo "RESTORE:"
    echo "  $src"
    echo "  → $dst"
    mv -- "$src" "$dst"
    printf '%s\tRESTORE\t%s\t%s\n' "$(date -Iseconds)" "$src" "$dst" >> "$MANIFEST"
  done
}

print_header

case "$MODE" in
  plan|--plan)
    show_plan
    ;;
  move|--move)
    show_plan
    echo "Для підтвердження введи MOVE:"
    read -r confirm
    [ "$confirm" = "MOVE" ] || {
      echo "Скасовано. Нічого не переміщено."
      exit 0
    }
    move_to_quarantine
    ;;
  restore|--restore)
    echo "Restore поверне archived *_android у корінь Renault."
    echo "Для підтвердження введи RESTORE:"
    read -r confirm
    [ "$confirm" = "RESTORE" ] || {
      echo "Скасовано."
      exit 0
    }
    restore_from_quarantine
    ;;
  interactive)
    show_plan
    echo
    echo "1 — Тільки показати план"
    echo "2 — Перенести *_android у quarantine"
    echo "3 — Повернути *_android з quarantine"
    echo "0 — Скасувати"
    echo
    printf "Вибір: "
    read -r choice
    case "$choice" in
      1) ;;
      2)
        echo
        echo "Для підтвердження введи MOVE:"
        read -r confirm
        [ "$confirm" = "MOVE" ] && move_to_quarantine || echo "Скасовано."
        ;;
      3)
        echo
        echo "Для підтвердження введи RESTORE:"
        read -r confirm
        [ "$confirm" = "RESTORE" ] && restore_from_quarantine || echo "Скасовано."
        ;;
      0) echo "Скасовано." ;;
      *) echo "Невідомий вибір: $choice"; exit 2 ;;
    esac
    ;;
  *)
    echo "Usage: $0 [plan|move|restore]"
    exit 2
    ;;
esac

echo
echo "Готово."
