#!/usr/bin/env bash
set -uo pipefail

ROOT=/home/joe/code/futon2
LOCK="$ROOT/data/wm-full-loop/hourly.lock"

mkdir -p "$(dirname "$LOCK")"
exec 9>"$LOCK"

if ! flock -n 9; then
  printf '[wm-cron] at=%s outcome=skipped-overlap lock=%s\n' \
    "$(date --utc --iso-8601=seconds)" "$LOCK"
  exit 0
fi

cd "$ROOT"
exec /usr/local/bin/clojure -M:wm-scheduled
