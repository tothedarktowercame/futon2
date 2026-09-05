#!/usr/bin/env bash
# library-build-loop.sh -- one library-theory row, then independent review.
# Forked from zaif-build-loop.sh and reconciled with wm-build-loop.sh on
# 2026-09-05: two-tree preflight/no publish from zaif; current unblock,
# bulletin, flock, MAX_ITER, and exit-notification behavior from both lanes.
set -uo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG="$HERE/runs/build-loop.log"; mkdir -p "$HERE/runs"
exec 9>"$HERE/runs/build-loop.lock"
if ! flock -n 9; then echo "another library-build-loop holds runs/build-loop.lock; exiting"; exit 0; fi
WORK_SEAT="${WORK_SEAT:-zai}"; REVIEW_SEAT="${REVIEW_SEAT:-codex}"
SLEEP="${SLEEP:-20}"; MAX_ITER="${MAX_ITER:-40}"
FUTON3="$HOME/code/futon3"
log() { echo "[$(date -u '+%H:%M:%S')] $*" | tee -a "$LOG"; }
NOTIFIED=0
notify() {
  [ "$NOTIFIED" = 1 ] && return 0; NOTIFIED=1
  local note; note="$(mktemp /tmp/library-build-notify.XXXXXX.md)"
  { echo "From library-build-loop.sh, $(date -u '+%Y-%m-%d %H:%M:%S') UTC. STOPPED: $1"
    echo "Board counts: $(bb "$HERE/library_step.bb" counts 2>/dev/null)"
    echo "Last 40 log lines ($LOG):"; tail -40 "$LOG"
    echo; echo "To restart after fixing: cd $HERE && nohup ./library-build-loop.sh > /tmp/library-build-nohup.out 2>&1 &"
  } > "$note"
  (cd "$HOME/code" && python3 futon3c/scripts/agency_send.py --from library-build-loop --to claude-1 --kind bell < "$note" >> "$LOG" 2>&1) || log "notify: bell to claude-1 failed"
  rm -f -- "$note"
}
trap 'rc=$?; notify "exit code $rc (trap)"' EXIT
STALL_ID=""; STALL_N=0
stall_check() {
  if [ "$1" = "$STALL_ID" ]; then STALL_N=$((STALL_N+1)); else STALL_ID="$1"; STALL_N=1; fi
  if [ "$STALL_N" -ge 3 ] && [ "$1" != NONE ]; then
    log "stalled: $1 unchanged for $STALL_N iterations"; notify "stalled on $1"; exit 3
  fi
}
run_seat() {
  local seat="$1" prompt="$2" label="$3" rc
  log "$label: $seat starting"
  case "$seat" in
    zai)    (cd "$HOME/code" && timeout 7200 python3 futon3c/scripts/agency_send.py --from library-build-loop --to zai-1 --kind whistle < "$prompt" >> "$LOG" 2>&1) ;;
    claude) (cd "$HOME/code" && timeout 7200 claude -p --permission-mode bypassPermissions "$(cat "$prompt")" >> "$LOG" 2>&1) ;;
    codex)  (cd "$HOME/code" && timeout 5400 codex exec --skip-git-repo-check --sandbox danger-full-access "$(cat "$prompt")" >> "$LOG" 2>&1) ;;
    *) log "unknown seat $seat"; return 2 ;;
  esac
  rc=$?; log "$label: $seat exit=$rc"; return "$rc"
}
ledger_ok() {
  local out rc; out="$(mktemp /tmp/library-build-check.XXXXXX)"
  (cd "$HERE" && bb worklist_check.bb worklist.edn) > "$out" 2>&1; rc=$?
  tail -1 "$out" | tee -a "$LOG"; rm -f -- "$out"; return "$rc"
}
preflight() {
  local dirty2
  dirty2="$(cd "$HOME/code/futon2" && git status --porcelain holes/labs/library-loop/ | grep -v '^??' || true)"
  if [ -n "$dirty2" ]; then
    log "pre-flight: library-loop has tracked modifications: $dirty2"
    notify "library-loop mid-edit; refusing to overlap uncommitted board edits"; exit 4
  fi
  MIDEDIT="$(cd "$FUTON3" && git status --porcelain | grep -v '^??' || true)"
  if [ -n "$MIDEDIT" ]; then log "pre-flight WARNING: futon3 tracked modifications (do-not-touch): $(echo "$MIDEDIT" | tr '\n' ' ')"; fi
}
bulletin() { bash "$HOME/code/futon2/holes/labs/wm-contract/write-bulletin.sh" "$LOG"; }

log "=== library-build-loop start (work=$WORK_SEAT review=$REVIEW_SEAT max=$MAX_ITER) ==="
i=0
while [ "$i" -lt "$MAX_ITER" ]; do
  i=$((i+1))
  ledger_ok || { log "ledger invalid; stopping"; notify "ledger invalid before work"; exit 1; }
  UNBLOCKED="$(bb "$HERE/library_step.bb" unblock)"
  [ -n "$UNBLOCKED" ] && echo "$UNBLOCKED" | tee -a "$LOG"
  if [ -n "$UNBLOCKED" ]; then
    (cd "$HOME/code/futon2" && git add -- holes/labs/library-loop/worklist.edn && git commit -q -m "library-loop: unblock rows whose dependencies are done" -- holes/labs/library-loop/worklist.edn)
  fi
  next="$(bb "$HERE/library_step.bb" next-open)"; unrev="$(bb "$HERE/library_step.bb" unreviewed)"
  log "iteration $i: next-open=$next unreviewed=[$unrev] counts=$(bb "$HERE/library_step.bb" counts)"
  [ -z "$unrev" ] && stall_check "$(bb "$HERE/library_step.bb" stall-key)"
  if [ "$next" = NONE ] && [ -z "$unrev" ]; then
    log "nothing open or unreviewed; done"; bulletin; notify "DONE: library-loop has no open or unreviewed rows"; break
  fi
  if [ "$next" != NONE ]; then
    preflight
    prompt="$(mktemp /tmp/library-build-work.XXXXXX.md)"
    { echo "ROW TO DO THIS INVOCATION: $next. Do this row and no other."
      if [ -n "${MIDEDIT:-}" ]; then echo; echo "TRACKED FILES MID-EDIT in /home/joe/code/futon3 (DO NOT TOUCH):"; echo "$MIDEDIT"; fi
      echo; cat "$HERE/work-prompt.md"; } > "$prompt"
    run_seat "$WORK_SEAT" "$prompt" "work($next)"; rc=$?; rm -f -- "$prompt"
    [ "$rc" -eq 0 ] || { notify "work seat failed on $next (exit=$rc)"; exit "$rc"; }
    ledger_ok || { notify "ledger invalid after work"; exit 1; }
  fi
  unrev="$(bb "$HERE/library_step.bb" unreviewed)"
  if [ -n "$unrev" ]; then
    prompt="$(mktemp /tmp/library-build-review.XXXXXX.md)"
    { echo "ROWS AWAITING REVIEW: $unrev. Review the FIRST only."; echo; cat "$HERE/review-prompt.md"; } > "$prompt"
    run_seat "$REVIEW_SEAT" "$prompt" "review($unrev)"; rc=$?; rm -f -- "$prompt"
    [ "$rc" -eq 0 ] || { notify "review seat failed on $unrev (exit=$rc)"; exit "$rc"; }
    ledger_ok || { notify "ledger invalid after review"; exit 1; }
  fi
  sleep "$SLEEP"
done
log "=== library-build-loop end after $i iterations ==="; bulletin; notify "ended after $i iterations (MAX_ITER=$MAX_ITER or done)"
