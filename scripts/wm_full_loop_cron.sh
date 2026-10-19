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
# The combined entrypoint requires FUTON_WM_RUN_CONFIG to name a sealed,
# deployment-owned config.  Missing authority is a typed nonzero refusal; it
# never falls back to the judgement-only diagnostic.
# U37 armed (Joe, 2026-09-19): every scheduled tick recomputes the available
# population independently of the proposers and attaches the verdict to the
# decision record. Read once at namespace load; costs ~130-200 ms per tick.
export FUTON_WM_ENUMERATION_ASSERT=1
exec /usr/local/bin/clojure -M:wm-scheduled
