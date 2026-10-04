#!/data/data/com.termux/files/usr/bin/bash
set -u

REPO="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
ROOT="${RENAULT_STORAGE_ROOT:-/storage/emulated/0/Documents/Renault}"
CONFIG="$REPO/config/current-device.json"

echo "========================================"
echo " Renault Docs · Legacy *_android audit"
echo " READ-ONLY"
echo "========================================"
echo
echo "Root:"
echo "  $ROOT"
echo
echo "Цей аудит нічого не видаляє, не перейменовує і не переміщує."
echo

if [ ! -d "$ROOT" ]; then
  echo "Папка Renault не знайдена."
  exit 0
fi

BUILD_ROOT=""
if [ -f "$CONFIG" ]; then
  BUILD_ROOT="$(
    python - "$CONFIG" <<'PY' 2>/dev/null || true
import json
import sys
from pathlib import Path

try:
    data = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
    print(data.get("build_root", ""))
except Exception:
    pass
PY
  )"
fi

canon_path() {
  python - "$1" <<'PY' 2>/dev/null
import os
import sys
print(os.path.realpath(sys.argv[1]))
PY
}

BUILD_ROOT_CANON=""
if [ -n "$BUILD_ROOT" ]; then
  BUILD_ROOT_CANON="$(canon_path "$BUILD_ROOT" || true)"
  echo "Активний build_root з config:"
  echo "  $BUILD_ROOT"
  echo
fi

shopt -s nullglob
dirs=("$ROOT"/*_android "$ROOT/legacy-quarantine"/*_android)

if [ "${#dirs[@]}" -eq 0 ]; then
  echo "Папок *_android не знайдено ні в корені, ні в legacy-quarantine."
  exit 0
fi

keep_count=0
legacy_count=0
archived_count=0
safe_count=0

for dir in "${dirs[@]}"; do
  [ -d "$dir" ] || continue

  base="$(basename "$dir")"
  canon="$(canon_path "$dir" || printf '%s' "$dir")"
  size="$(du -sh -- "$dir" 2>/dev/null | awk '{print $1}')"
  [ -n "$size" ] || size="?"
  files="$(find "$dir" -type f -print 2>/dev/null | wc -l | tr -d ' ')"
  [ -n "$files" ] || files="?"
  modified="$(stat -c '%y' -- "$dir" 2>/dev/null | cut -d. -f1)"
  [ -n "$modified" ] || modified="?"

  has_dataset="ні"
  [ -f "$dir/renault-dataset.json" ] && has_dataset="так"

  has_nomedia="ні"
  [ -f "$dir/.nomedia" ] && has_nomedia="так"

  role="legacy converted/normalized Renault dataset"
  case "$base" in
    *_NT*_android)
      role="legacy per-volume converted Renault dataset"
      ;;
  esac
  if [ "$has_dataset" = "так" ]; then
    role="prepared Renault dataset (renault-dataset.json present)"
  fi

  repo_refs="$(
    {
      git -C "$REPO" grep -n -F -- "$dir" -- . 2>/dev/null || true
      git -C "$REPO" grep -n -F -- "$base" -- . 2>/dev/null || true
    } | awk '!seen[$0]++' | head -n 5
  )"

  status="LEGACY"
  reason="не є активним build_root; перед видаленням потрібна окрема перевірка runtime/SAF reference"
  case "$canon" in
    "$ROOT/legacy-quarantine/"*)
      status="ARCHIVED"
      reason="переміщено в reversible legacy-quarantine; не видалено"
      ;;
  esac
  if [ -n "$BUILD_ROOT_CANON" ] && [ "$canon" = "$BUILD_ROOT_CANON" ]; then
    status="KEEP"
    reason="це активний build_root з config/current-device.json (може бути всередині quarantine)"
  fi

  case "$status" in
    KEEP) keep_count=$((keep_count + 1)) ;;
    LEGACY) legacy_count=$((legacy_count + 1)) ;;
    ARCHIVED) archived_count=$((archived_count + 1)) ;;
    SAFE_TO_REMOVE) safe_count=$((safe_count + 1)) ;;
  esac

  echo "----------------------------------------"
  echo "$base"
  echo "Шлях:        $dir"
  echo "Розмір:      $size"
  echo "Файлів:      $files"
  echo "Змінено:     $modified"
  echo "Dataset:     $has_dataset"
  echo ".nomedia:    $has_nomedia"
  echo "Ймовірна роль: $role"
  echo "Статус:      $status"
  echo "Причина:     $reason"

  if [ -n "$repo_refs" ]; then
    echo "Згадки в repo (до 5):"
    while IFS= read -r line; do
      echo "  $line"
    done <<< "$repo_refs"
  else
    echo "Згадки в repo: не знайдено"
  fi
  echo
done

echo "========================================"
echo "Підсумок"
echo "========================================"
echo "KEEP:           $keep_count"
echo "LEGACY:         $legacy_count"
echo "ARCHIVED:       $archived_count"
echo "SAFE TO REMOVE: $safe_count"
echo
echo "Важливо:"
echo "  LEGACY ≠ SAFE TO REMOVE."
echo "  Termux не може сам довести відсутність app-private/SAF reference."
echo "  Нічого не видаляй до окремого підтвердження після аналізу цього звіту."
