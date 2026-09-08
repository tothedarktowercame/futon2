#!/usr/bin/env bash
# wm-build-loop.sh -- continuous build of the war machine from the ledger:
#   unblock -> work (one row, seat A) -> review (one row, seat B != A) -> publish (when the
#   registries are clear) -> repeat, until nothing is open, unreviewed, or unblockable.
# Runs unattended. Log: runs/build-loop.log (tail it; voxterm shows the process tree).
# Seats: WORK_SEAT=claude|codex|zai, REVIEW_SEAT a different one. Author != reviewer
# is enforced by using different tools for the two phases within an iteration.
# Defaults flipped to work=codex review=zai on 2026-09-08 (Joe: Codex and zai
# quotas reset to 100%, Claude at 40% -- "use the Codex and Zai resources as
# much as possible within the WM build loop from here on. So that we stop
# burning Claude."). The zai seat is a blocking whistle to roster agent zai-1,
# the same invocation the library loop has run since 2026-09-05. claude stays
# available as an explicit override (WORK_SEAT=claude ./wm-build-loop.sh).
set -uo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG="$HERE/runs/build-loop.log"; mkdir -p "$HERE/runs"
WORK_SEAT="${WORK_SEAT:-codex}"; REVIEW_SEAT="${REVIEW_SEAT:-zai}"; SLEEP="${SLEEP:-20}"; MAX_ITER="${MAX_ITER:-60}"
log() { echo "[$(date -u '+%H:%M:%S')] $*" | tee -a "$LOG"; }
# U65: the loop's two identities are REGISTERED Agency seats with
# delivery-mode inbox, so a bellback addressed to either is written under
# ~/.claude/agency-inbox/<seat>/ instead of needing a live session. Before
# this, an unregistered --from got no bellback route at all, and the work seat
# borrowed the owner's id (--from claude-1); the bail-out then landed in the
# owner's session, which dispatched a continuation nobody asked for, and the
# seat's cancel of its own duplicate killed that continuation
# (EPIC-run-era.md:923). Registration is idempotent -- a duplicate answers 409
# and is ignored -- and re-run at start so a roster reset does not silently
# return the loop to a borrowed id.
AGENCY_BASE="${AGENCY_BASE:-http://localhost:7070}"
ensure_seats() {
  local seat
  for seat in wm-build-work wm-build-loop; do
    curl -s -X POST "$AGENCY_BASE/api/alpha/agents" \
      -H 'Content-Type: application/json' \
      -d "{\"agent-id\":\"$seat\",\"type\":\"claude\",\"delivery-mode\":\"inbox\"}" \
      >> "$LOG" 2>&1
    echo >> "$LOG"
  done
}

# Every way out of this loop bells claude-1 (the Emacs seat) with the reason and
# the tail of the log, so a stopped loop is a message in that session, not a
# discovery the next day (Joe, 2026-09-01: "if it stops, I feel like you should
# get an error message sent through to this session so that you could fix it").
NOTIFIED=0
notify() { # $1 reason
  [ "$NOTIFIED" = 1 ] && return 0; NOTIFIED=1
  { echo "From wm-build-loop.sh, $(date -u '+%Y-%m-%d %H:%M:%S') UTC. STOPPED: $1"
    echo "Ledger counts: $(bb "$HERE/build_step.bb" counts 2>/dev/null)"
    echo "Last 40 log lines ($LOG):"; tail -40 "$LOG"
    echo; echo "To restart after fixing: cd $HERE && nohup ./wm-build-loop.sh > /tmp/wm-build-nohup.out 2>&1 &"
  } > /tmp/wm-build-notify.md
  (cd "$HOME/code" && python3 futon3c/scripts/agency_send.py --from wm-build-loop --to claude-1 --kind bell < /tmp/wm-build-notify.md >> "$LOG" 2>&1) || log "notify: bell to claude-1 failed"
}
trap 'rc=$?; notify "exit code $rc (trap)"' EXIT
STALL_ID=""; STALL_N=0
stall_check() { # $1 next-open id; three iterations on the same open row with nothing reviewed = stalled
  if [ "$1" = "$STALL_ID" ]; then STALL_N=$((STALL_N+1)); else STALL_ID="$1"; STALL_N=1; fi
  if [ "$STALL_N" -ge 3 ] && [ "$1" != "NONE" ]; then log "stalled: $1 has been next-open for $STALL_N iterations without changing status"; notify "stalled on row $1 (3 iterations, no status change)"; exit 3; fi
}
run_seat() { # $1 seat, $2 prompt file, $3 label
  local seat="$1" prompt="$2" label="$3"
  log "$label: $seat starting"
  case "$seat" in
    claude) (cd "$HOME/code" && timeout 7200 claude -p --permission-mode bypassPermissions "$(cat "$prompt")" >> "$LOG" 2>&1) ;;
    codex)  (cd "$HOME/code" && timeout 5400 codex exec --skip-git-repo-check --sandbox danger-full-access "$(cat "$prompt")" >> "$LOG" 2>&1) ;;
    zai)    (cd "$HOME/code" && timeout 5400 python3 futon3c/scripts/agency_send.py --from wm-build-work --to zai-1 --kind whistle < "$prompt" >> "$LOG" 2>&1) ;;
    *) log "unknown seat $seat"; return 2 ;;
  esac
  local rc=$?; log "$label: $seat exit=$rc"; return $rc
}
# Re-render the frontier before each gate check. Ticket-status flips
# (blocked/open/done) are routine bookkeeping: commit and continue. Any other
# render change (assurance-state degrade, ticket-count drift) is reverted so
# the U70 gate stops the loop and a human routes the re-witness -- that stop
# caught a true witness-staleness on 2026-09-08 and must stay loud.
refresh_frontier() {
  (cd "$HERE" && bb gen_dependency_frontier.bb >/dev/null 2>&1) || return 0
  local diff; diff=$(cd "$HOME/code/futon2" && git diff -- holes/labs/wm-contract/dependency-frontier.md)
  [ -z "$diff" ] && return 0
  if echo "$diff" | grep -E '^[+-][^+-]' | grep -qvE '^[+-]\| [0-9]+ \| .:U'; then
    (cd "$HOME/code/futon2" && git checkout -- holes/labs/wm-contract/dependency-frontier.md)
    log "frontier: non-status render change; left stale for the U70 gate to refuse"
    return 0
  fi
  (cd "$HOME/code/futon2" && git add holes/labs/wm-contract/dependency-frontier.md && git commit -q -m "frontier: status-flip re-render (wm-build-loop)")
  log "frontier: status-flip re-render committed"
}
ledger_ok() { refresh_frontier; bb "$HERE/worklist_check.bb" "$HERE/worklist.edn" > /tmp/wm-build-check.out 2>&1; local rc=$?; grep -vE 'WARNING|^worklist_check:\s+(M|\?\?) ' /tmp/wm-build-check.out | tail -1 | tee -a "$LOG"; return $rc; }
publish() {
  if [ "$(bb "$HERE/build_step.bb" registry-held)" = "0" ]; then
    log "publish: registries clear -> build-p4ng.sh futon-2026"
    (cd "$HOME/code/p4ng" && bash build-p4ng.sh futon-2026 > /tmp/wm-build-p4ng.log 2>&1); local rc=$?
    grep -E 'negative_controls|figure-4a-generator|gate' /tmp/wm-build-p4ng.log | tee -a "$LOG"
    if [ $rc -eq 0 ]; then
      (cd "$HOME/code/p4ng" && git add -A aif-control-map-live.svg aif-control-map-live.pdf empirics-futon/aif-conformance.edn aif-equation-dag.svg aif-equation-dag.pdf sec-*-generated.tex war-room-tetrahedron.svg war-room-tetrahedron.pdf defect-repair-tally.svg defect-repair-tally.pdf 2>/dev/null; git diff --cached --quiet || git commit -q -m "futon-2026: regenerate (wm-build-loop, ledger clear)" ) && log "publish: committed"
      (cd "$HOME/code/futon2" && git add holes/labs/wm-contract/workflow-report.edn holes/labs/wm-contract/variable-situation-accounting.edn holes/labs/wm-contract/PROPOSED-ROWS.md 2>/dev/null; git diff --cached --quiet || git commit -q -m "wm-contract: publish-generated snapshots (wm-build-loop)")
    else log "publish: build failed rc=$rc (see /tmp/wm-build-p4ng.log)"; fi
  else log "publish: held -- a registry row awaits review"; fi
}
# The morning bulletin (:B1). Generated HERE, at the loop's natural stop, and
# not by a daemon: the digest of a session is due when the session ends. It is
# idempotent, so both exits below may call it and only one writes.
bulletin() { bash "$HERE/write-bulletin.sh" "$LOG"; }
log "=== wm-build-loop start (work=$WORK_SEAT review=$REVIEW_SEAT) ==="
ensure_seats
i=0
while [ $i -lt "$MAX_ITER" ]; do
  i=$((i+1))
  ledger_ok || { log "ledger invalid; stopping"; notify "ledger invalid before work"; exit 1; }
  UNBLOCKED="$(bb "$HERE/build_step.bb" unblock)"
  # Template bug fix (claude-2, 2026-09-03): calling unblock twice meant the
  # second call saw nothing left and the flip was NEVER committed -- the edit
  # lingered as a tracked modification and could stall the pre-flight.
  if [ -n "$UNBLOCKED" ]; then
    echo "$UNBLOCKED" | tee -a "$LOG"
    (cd "$HOME/code/futon2" && git add holes/labs/wm-contract/worklist.edn && git commit -q -m "worklist: wm-build-loop unblocked rows whose :depends-on are done")
  fi
  next="$(bb "$HERE/build_step.bb" next-open)"; unrev="$(bb "$HERE/build_step.bb" unreviewed)"
  log "iteration $i: next-open=$next unreviewed=[$unrev] counts=$(bb "$HERE/build_step.bb" counts)"
  [ -z "$unrev" ] && stall_check "$(bb "$HERE/build_step.bb" stall-key)"
  if [ "$next" = "NONE" ] && [ -z "$unrev" ]; then log "nothing open or unreviewed; done"; publish; bulletin; notify "DONE: nothing open or unreviewed"; break; fi
  if [ "$next" != "NONE" ]; then
    # The loop chose the row (build_step.bb priorities); the prompt must say so,
    # or the seat takes the first open row in ledger order (iteration 1 took I1
    # when RUN12 was meant). The prompt is composed per iteration.
    # A bellback for the work seat landed in its inbox while no seat was
    # running; drain it into THIS prompt so the reply reaches the seat that
    # sent the bell, not the owner's session (U65).
    bash "$HERE/wm-inbox-drain.sh" wm-build-work > /tmp/wm-build-inbox.md 2>>"$LOG"
    { echo "ROW TO DO THIS INVOCATION: $next -- the build loop chose it by priority; take this row and no other. If it carries :loop-mode :one-slice-per-invocation, do its next slice."
      if [ -s /tmp/wm-build-inbox.md ]; then
        echo; echo "BELLS WAITING IN YOUR INBOX (seat wm-build-work), already acked -- read them before you dispatch anything new:"; echo
        cat /tmp/wm-build-inbox.md
      fi
      echo; cat "$HERE/worklist-prompt.md"; } > /tmp/wm-build-work-prompt.md
    run_seat "$WORK_SEAT" /tmp/wm-build-work-prompt.md "work($next)"
    ledger_ok || { log "ledger invalid after work; stopping"; notify "ledger invalid after work"; exit 1; }
  fi
  unrev="$(bb "$HERE/build_step.bb" unreviewed)"
  if [ -n "$unrev" ]; then
    { echo "ROWS AWAITING REVIEW: $unrev -- review the FIRST of these."; echo; cat "$HERE/review-prompt.md"; } > /tmp/wm-build-review-prompt.md
    run_seat "$REVIEW_SEAT" /tmp/wm-build-review-prompt.md "review($unrev)"
    ledger_ok || { log "ledger invalid after review; stopping"; notify "ledger invalid after review"; exit 1; }
  fi
  publish
  sleep "$SLEEP"
done
log "=== wm-build-loop end after $i iterations ==="; bulletin; notify "ended after $i iterations (MAX_ITER=$MAX_ITER or done)"
