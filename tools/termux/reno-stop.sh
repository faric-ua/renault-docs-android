#!/data/data/com.termux/files/usr/bin/bash
set -eu

STATE_DIR="$HOME/.cache/renault-docs"
PIDFILE="$STATE_DIR/server.pid"

if [ ! -f "$PIDFILE" ]; then
  echo "No Renault server started by reno-docs is recorded."
  echo "If a server is running in a visible Termux session, stop it there with Ctrl+C."
  exit 0
fi

PID="$(cat "$PIDFILE" 2>/dev/null || true)"

if [ -z "$PID" ]; then
  rm -f "$PIDFILE"
  echo "Removed empty Renault server PID file."
  exit 0
fi

if kill -0 "$PID" 2>/dev/null; then
  kill "$PID"
  echo "Stopped Renault documentation server (PID $PID)."
else
  echo "Recorded Renault server process is no longer running."
fi

rm -f "$PIDFILE"
