#!/usr/bin/env bash
# wm-inbox-drain.sh [seat] -- print, then ACK, every bell waiting in a loop
# seat's Agency inbox. Default seat: wm-build-work.
#
# Why this exists (U65, EPIC-run-era.md:923): the work seat is a fresh
# `claude -p` per iteration with no Agency session, so before this it had no
# id to dispatch from and borrowed the owner's (`--from claude-1`). The
# bail-out bellback then landed in claude-1's session, which dispatched a
# continuation nobody had asked for. `wm-build-work` and `wm-build-loop` are
# now registered with delivery-mode inbox, so a bellback for the seat is
# written as JSON under ~/.claude/agency-inbox/<seat>/ and this drains it into
# the next iteration's prompt.
#
# The ACK is also the seat's only liveness signal: a pull-only seat never runs
# an invoke, so its :agent/last-active only moves when it acks, and the idle
# reaper reads last-active (http.clj handle-ack-invoke-job).
set -uo pipefail
SEAT="${1:-wm-build-work}"
BASE="${AGENCY_BASE:-http://localhost:7070}"
DIR="${FUTON3C_AGENCY_INBOX_DIR:-$HOME/.claude/agency-inbox}/$SEAT"
[ -d "$DIR" ] || exit 0
shopt -s nullglob
FILES=("$DIR"/*.json)
[ ${#FILES[@]} -eq 0 ] && exit 0
for f in "${FILES[@]}"; do
  jid="$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["job-id"])' "$f" 2>/dev/null)" || continue
  [ -n "$jid" ] || continue
  python3 - "$f" <<'PY'
import json, sys
m = json.load(open(sys.argv[1]))
print("--- inbox bell %s (from %s, in-reply-to %s, %s) ---"
      % (m["job-id"], m.get("from"), m.get("in-reply-to"), m.get("created-at")))
print(m.get("prompt", ""))
print("--- end inbox bell ---")
PY
  curl -s -X POST "$BASE/api/alpha/invoke/jobs/$jid/ack" \
       -H 'Content-Type: application/json' \
       -d '{"note":"drained by wm-inbox-drain.sh"}' > /dev/null
done
