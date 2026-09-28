#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

YTM_TOOL="/storage/emulated/0/Documents/YTM/tools/termux/ytm-download-apk.sh"

[ -f "$YTM_TOOL" ] || {
  echo "YTM download helper has moved to the YTM repository." >&2
  echo "Expected: $YTM_TOOL" >&2
  exit 1
}

exec bash "$YTM_TOOL" "$@"
