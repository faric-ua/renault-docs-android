#!/data/data/com.termux/files/usr/bin/bash
set -eu

HOST="127.0.0.1"
PORT="${RENO_PORT:-8080}"
STATE_DIR="$HOME/.cache/renault-docs"
PIDFILE="$STATE_DIR/server.pid"
LOGFILE="$STATE_DIR/server.log"
URLFILE="$STATE_DIR/server.url"

if [ -f "$PIDFILE" ]; then
  PID="$(cat "$PIDFILE" 2>/dev/null || true)"
  if [ -n "$PID" ] && kill -0 "$PID" 2>/dev/null; then
    echo "Renault server: RUNNING"
    echo "PID: $PID"
    if [ -f "$URLFILE" ]; then
      echo "URL: $(cat "$URLFILE")"
    else
      echo "URL: http://$HOST:$PORT/"
    fi
    echo "Log: $LOGFILE"
    exit 0
  fi
fi

echo "Renault server: not running under launcher control."
echo "If it was started manually, check the Termux session where web/serve.py is running."
