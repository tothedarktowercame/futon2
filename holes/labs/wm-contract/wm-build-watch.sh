#!/usr/bin/env bash
# wm-build-watch.sh <loop-pid> -- watch a RUNNING wm-build-loop that predates the
# notify trap: bell claude-1 when it exits, or when its log goes quiet for 50 min.
PID="$1"; HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"; LOG="$HERE/runs/build-loop.log"
bell() { { echo "From wm-build-watch.sh, $(date -u '+%Y-%m-%d %H:%M:%S') UTC. $1"; echo "Last 40 log lines:"; tail -40 "$LOG"; } > /tmp/wm-build-watch.md
         (cd "$HOME/code" && python3 futon3c/scripts/agency_send.py --from wm-build-loop --to claude-1 --kind bell < /tmp/wm-build-watch.md >/dev/null 2>&1); }
while kill -0 "$PID" 2>/dev/null; do
  age=$(( $(date +%s) - $(stat -c %Y "$LOG") ))
  if [ "$age" -gt 3000 ]; then bell "QUIET: loop pid $PID alive but no log line for $((age/60)) min"; sleep 3000; fi
  sleep 60
done
bell "EXITED: loop pid $PID is gone (rc unknown -- it predates the notify trap). Check the log tail; restart with: cd $HERE && nohup ./wm-build-loop.sh > /tmp/wm-build-nohup.out 2>&1 &"
