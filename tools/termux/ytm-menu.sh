#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

YTM_MENU="/storage/emulated/0/Documents/YTM/tools/termux/ytm-menu.sh"

[ -f "$YTM_MENU" ] || {
  echo "YTM menu has moved to the YTM repository." >&2
  echo "Sync /storage/emulated/0/Documents/YTM first." >&2
  echo "Expected: $YTM_MENU" >&2
  exit 1
}

exec bash "$YTM_MENU"
