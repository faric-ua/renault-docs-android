#!/data/data/com.termux/files/usr/bin/bash
set -eu

REPO="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
CONFIG="$REPO/config/current-device.json"
HOST="127.0.0.1"
PORT="${RENO_PORT:-8080}"

STATE_DIR="$HOME/.cache/renault-docs"
PIDFILE="$STATE_DIR/server.pid"
LOGFILE="$STATE_DIR/server.log"
URLFILE="$STATE_DIR/server.url"

mkdir -p "$STATE_DIR"

DOCS="$(python - "$CONFIG" <<'PY'
import json
import sys
from pathlib import Path

data = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
print(data["build_root"])
PY
)"

if [ ! -d "$DOCS" ]; then
  echo "Normalized documentation not found:"
  echo "  $DOCS"
  echo
  echo "Run conversion first:"
  echo "  reno-code"
  echo "  python convert.py"
  exit 1
fi

ENTRY="$(python - "$DOCS" <<'PY'
import json
import sys
from pathlib import Path
from urllib.parse import quote

root = Path(sys.argv[1])
manifest = root / "renault-dataset.json"

entry = None
if manifest.is_file():
    try:
        data = json.loads(manifest.read_text(encoding="utf-8"))
        entry = data.get("catalog_entrypoint") or data.get("entrypoint")
    except Exception:
        pass

if not entry:
    for name in ("INDEX.HTM", "index.htm", "INDEX.HTML", "index.html", "ACCUEIL.HTM", "accueil.htm"):
        if (root / name).is_file():
            entry = name
            break

print(quote(entry or "", safe="/"))
PY
)"

if [ -n "$ENTRY" ]; then
  URL="http://$HOST:$PORT/$ENTRY"
else
  URL="http://$HOST:$PORT/"
fi

if ! python "$REPO/tools/install_pdfjs.py" --check >/dev/null 2>&1; then
  echo "PDF viewer runtime is not installed yet."
  echo "HTML will still work; before testing PDF run once:"
  echo "  reno-pdf-setup"
  echo
fi

port_open() {
  python - "$HOST" "$PORT" <<'PY'
import socket
import sys

host = sys.argv[1]
port = int(sys.argv[2])

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
    sock.settimeout(0.25)
    result = sock.connect_ex((host, port))

raise SystemExit(0 if result == 0 else 1)
PY
}

managed_server_running() {
  [ -f "$PIDFILE" ] || return 1
  PID="$(cat "$PIDFILE" 2>/dev/null || true)"
  [ -n "$PID" ] || return 1
  kill -0 "$PID" 2>/dev/null
}

if managed_server_running; then
  echo "Renault documentation server is already running (PID $(cat "$PIDFILE"))."
elif port_open; then
  echo "Port $PORT is already in use. Reusing the existing local server."
else
  echo "Starting Renault documentation server..."
  nohup python "$REPO/web/serve.py" "$DOCS" \
    --host "$HOST" \
    --port "$PORT" \
    >"$LOGFILE" 2>&1 </dev/null &

  PID=$!
  echo "$PID" > "$PIDFILE"

  sleep 1

  if ! kill -0 "$PID" 2>/dev/null; then
    rm -f "$PIDFILE"
    echo "Server failed to start."
    echo
    if [ -f "$LOGFILE" ]; then
      cat "$LOGFILE"
    fi
    exit 1
  fi
fi

printf '%s\n' "$URL" > "$URLFILE"

echo "Opening:"
echo "  $URL"

if command -v termux-open-url >/dev/null 2>&1; then
  termux-open-url "$URL"
elif [ -x /system/bin/am ]; then
  /system/bin/am start \
    -a android.intent.action.VIEW \
    -d "$URL" >/dev/null 2>&1
else
  echo
  echo "Could not open the browser automatically."
  echo "Open this URL manually:"
  echo "  $URL"
fi
