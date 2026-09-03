#!/usr/bin/env bash
# zaif-build-loop.sh -- continuous build of the zaif-harness lane from its board:
#   unblock -> work (one row, codex) -> review (one row, claude) -> repeat, until
#   nothing in this lane is open, unreviewed, or unblockable.
# Adapted from wm-contract/wm-build-loop.sh (2026-09-02, claude-2). Differences:
#   - work=codex review=claude by default (this lane's coding-handoff law:
#     codex authors, claude reviews; lane owner spot-checks what the loop signs);
#   - TWO shared trees: the board lives in futon2, rows build in futon3c (the
#     checkout serving the live JVM). Pre-flight warns on tracked modifications
#     in futon3c and passes the list into the work prompt as do-not-touch;
#     tracked modifications in the futon2 lab dir stop the loop (someone mid-edit);
#   - no publish step (this lane has no p4ng build);
#   - stop bells route to claude-2 (the lane owner), not claude-1.
set -uo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG="$HERE/runs/build-loop.log"; mkdir -p "$HERE/runs"
# Single-instance guard: two loops mean two review seats editing the same
# board row (happened 2026-09-03 05:46/05:49 UTC — a timed restart and a manual
# one raced). Refusing here, before the notify trap is installed, exits quietly:
# the healthy instance needs no bell about it.
exec 9>"$HERE/runs/build-loop.lock"
if ! flock -n 9; then echo "another zaif-build-loop holds runs/build-loop.lock; exiting"; exit 0; fi
WORK_SEAT="${WORK_SEAT:-codex}"; REVIEW_SEAT="${REVIEW_SEAT:-claude}"; SLEEP="${SLEEP:-20}"; MAX_ITER="${MAX_ITER:-40}"
FUTON3C="$HOME/code/futon3c"
log() { echo "[$(date -u '+%H:%M:%S')] $*" | tee -a "$LOG"; }
# Every way out bells claude-2 (lane owner) with the reason and the log tail,
# so a stopped loop is a message in that session, not a next-day discovery.
NOTIFIED=0
notify() { # $1 reason
  [ "$NOTIFIED" = 1 ] && return 0; NOTIFIED=1
  { echo "From zaif-build-loop.sh, $(date -u '+%Y-%m-%d %H:%M:%S') UTC. STOPPED: $1"
    echo "Board counts: $(bb "$HERE/zaif_step.bb" counts 2>/dev/null)"
    echo "Last 40 log lines ($LOG):"; tail -40 "$LOG"
    echo; echo "To restart after fixing: cd $HERE && nohup ./zaif-build-loop.sh > /tmp/zaif-build-nohup.out 2>&1 &"
  } > /tmp/zaif-build-notify.md
  (cd "$HOME/code" && python3 futon3c/scripts/agency_send.py --from zaif-build-loop --to claude-2 --kind bell < /tmp/zaif-build-notify.md >> "$LOG" 2>&1) || log "notify: bell to claude-2 failed"
}
trap 'rc=$?; notify "exit code $rc (trap)"' EXIT
STALL_ID=""; STALL_N=0
stall_check() { # $1 stall-key; three iterations same open row, no content change = stalled
  if [ "$1" = "$STALL_ID" ]; then STALL_N=$((STALL_N+1)); else STALL_ID="$1"; STALL_N=1; fi
  if [ "$STALL_N" -ge 3 ] && [ "$1" != "NONE" ]; then log "stalled: $1 unchanged for $STALL_N iterations"; notify "stalled on $1 (3 iterations, no change)"; exit 3; fi
}
run_seat() { # $1 seat, $2 prompt file, $3 label
  local seat="$1" prompt="$2" label="$3"
  log "$label: $seat starting"
  case "$seat" in
    claude) (cd "$HOME/code" && timeout 7200 claude -p --permission-mode bypassPermissions "$(cat "$prompt")" >> "$LOG" 2>&1) ;;
    codex)  (cd "$HOME/code" && timeout 5400 codex exec --skip-git-repo-check --sandbox danger-full-access "$(cat "$prompt")" >> "$LOG" 2>&1) ;;
    *) log "unknown seat $seat"; return 2 ;;
  esac
  local rc=$?; log "$label: $seat exit=$rc"; return $rc
}
ledger_ok() { (cd "$HERE" && bb worklist_check.bb worklist.edn) > /tmp/zaif-build-check.out 2>&1; local rc=$?; tail -1 /tmp/zaif-build-check.out | tee -a "$LOG"; return $rc; }
preflight() { # returns via globals: MIDEDIT (futon3c tracked mods, newline list)
  # futon2 lab dir: tracked modifications mean someone is mid-edit on the board -- stop.
  local dirty2
  dirty2="$(cd "$HOME/code/futon2" && git status --porcelain holes/labs/zaif-harness/ | grep -v '^??' || true)"
  if [ -n "$dirty2" ]; then
    log "pre-flight: futon2 lab dir has tracked modifications (mid-edit): $dirty2"
    notify "futon2 lab dir mid-edit; refusing to loop over someone's uncommitted board edits"
    exit 4
  fi
  # futon3c: tracked modifications are WARN + do-not-touch list (other lanes
  # commit here concurrently; refusing outright would false-stall this loop).
  MIDEDIT="$(cd "$FUTON3C" && git status --porcelain | grep -v '^??' || true)"
  if [ -n "$MIDEDIT" ]; then log "pre-flight WARNING: futon3c tracked modifications (do-not-touch for this iteration): $(echo "$MIDEDIT" | tr '\n' ' ')"; fi
}
log "=== zaif-build-loop start (work=$WORK_SEAT review=$REVIEW_SEAT max=$MAX_ITER) ==="
i=0
while [ $i -lt "$MAX_ITER" ]; do
  i=$((i+1))
  ledger_ok || { log "ledger invalid; stopping"; notify "ledger invalid before work"; exit 1; }
  # Capture unblock output ONCE: a second call finds nothing left to unblock,
  # so the edit was made but never committed (c427a68 swept it in mislabeled;
  # the wm template has the same double-call — flagged to claude-1).
  UNBLOCKED="$(bb "$HERE/zaif_step.bb" unblock)"
  [ -n "$UNBLOCKED" ] && echo "$UNBLOCKED" | tee -a "$LOG"
  if [ -n "$UNBLOCKED" ]; then (cd "$HOME/code/futon2" && git add holes/labs/zaif-harness/worklist.edn && git commit -q -m "zaif-harness: zaif-build-loop unblocked rows whose :depends-on are done" -- holes/labs/zaif-harness/worklist.edn); fi
  next="$(bb "$HERE/zaif_step.bb" next-open)"; unrev="$(bb "$HERE/zaif_step.bb" unreviewed)"
  log "iteration $i: next-open=$next unreviewed=[$unrev] counts=$(bb "$HERE/zaif_step.bb" counts)"
  [ -z "$unrev" ] && stall_check "$(bb "$HERE/zaif_step.bb" stall-key)"
  if [ "$next" = "NONE" ] && [ -z "$unrev" ]; then log "nothing open or unreviewed in this lane; done"; notify "DONE: nothing open or unreviewed in lane claude-2"; break; fi
  if [ "$next" != "NONE" ]; then
    preflight
    { echo "ROW TO DO THIS INVOCATION: $next -- the build loop chose it by priority; take this row and no other."
      if [ -n "${MIDEDIT:-}" ]; then echo; echo "FILES MID-EDIT BY ANOTHER AGENT in /home/joe/code/futon3c (do NOT touch; if your row needs one, stop and report):"; echo "$MIDEDIT"; fi
      echo; cat "$HERE/work-prompt.md"; } > /tmp/zaif-build-work-prompt.md
    run_seat "$WORK_SEAT" /tmp/zaif-build-work-prompt.md "work($next)"
    ledger_ok || { log "ledger invalid after work; stopping"; notify "ledger invalid after work"; exit 1; }
  fi
  unrev="$(bb "$HERE/zaif_step.bb" unreviewed)"
  if [ -n "$unrev" ]; then
    { echo "ROWS AWAITING REVIEW: $unrev -- review the FIRST of these."; echo; cat "$HERE/review-prompt.md"; } > /tmp/zaif-build-review-prompt.md
    run_seat "$REVIEW_SEAT" /tmp/zaif-build-review-prompt.md "review($unrev)"
    ledger_ok || { log "ledger invalid after review; stopping"; notify "ledger invalid after review"; exit 1; }
  fi
  sleep "$SLEEP"
done
log "=== zaif-build-loop end after $i iterations ==="; notify "ended after $i iterations (MAX_ITER=$MAX_ITER or done)"
