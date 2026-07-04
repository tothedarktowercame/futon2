#!/usr/bin/env bash
# wm_click_loop.sh — regulated "clicks" driver (README-clicks-and-ticks.md;
# M-aif-faithfulness evidence-acceleration, Joe 2026-07-04).
#
# One click = one full :wm-scheduled run. REGULATED means:
#   - SEQUENTIAL: the next click starts only when the previous one finished
#     (plus a small settle delay) — never concurrent with itself.
#   - BOUNDED: at most MAX_CLICKS runs, then the loop exits.
#   - STOPPABLE: touch the stop file and the loop exits before the next click.
#   - PROVENANCED: every click stamps :trigger :duree-click-regulated in its
#     :wm-version (B-0a), so C9 / the ledger can segment click-era rows.
# The hourly cron tick stays installed — ticks remain the calendar baseline
# (the README: neither clock alone is sufficient). A cron tick overlapping a
# click is the same (rare, tolerated) overlap as cron + manual runs today.
#
# Usage: scripts/wm_click_loop.sh [MAX_CLICKS] [SETTLE_SECONDS]
#   defaults: MAX_CLICKS=24, SETTLE_SECONDS=10
#   stop file: data/.wm-clicks-stop  (touch to halt; removed on loop start)
# Log: logs/wm-clicks.log (same line format as wm-scheduled.log)
# For campaigns > ~30 min prefer:  systemd-run --user --collect \
#   --unit=wm-clicks scripts/wm_click_loop.sh 24   (survives session teardown)

set -u
cd "$(dirname "$0")/.."   # futon2 root

MAX_CLICKS="${1:-24}"
SETTLE="${2:-10}"
STOP_FILE="data/.wm-clicks-stop"
LOG="logs/wm-clicks.log"

rm -f "$STOP_FILE"
echo "$(date -u +%FT%TZ) click-loop START max=$MAX_CLICKS settle=${SETTLE}s pid=$$" >> "$LOG"

for i in $(seq 1 "$MAX_CLICKS"); do
  if [ -e "$STOP_FILE" ]; then
    echo "$(date -u +%FT%TZ) click-loop STOPPED by stop-file after $((i-1)) clicks" >> "$LOG"
    exit 0
  fi
  echo "$(date -u +%FT%TZ) click $i/$MAX_CLICKS starting" >> "$LOG"
  FUTON_WM_TRIGGER=duree-click-regulated /usr/local/bin/clojure -M:wm-scheduled >> "$LOG" 2>&1
  sleep "$SETTLE"
done

echo "$(date -u +%FT%TZ) click-loop DONE ($MAX_CLICKS clicks)" >> "$LOG"
