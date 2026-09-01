#!/usr/bin/env bash
# RUN12 negative control -- a lock that cannot refuse proves nothing.
#
# WHAT IS REAL HERE: the second starter is the real entrypoint,
# `clojure -M -m futon2.run-tick-once 14`, and it must exit non-zero naming the
# holder BEFORE it reads the store or writes a trace record or a receipt.
# WHAT IS A STAND-IN: the first holder is `futon2.wm-run-lock hold`, which takes
# the lock by the same acquire! path a tick does but runs no tick. A real first
# runner would append to today's trace file and contaminate run selection, which
# is the thing RUN11/RUN3 are trying to stop doing by accident.
# WHAT IS DISPLACED: FUTON_WM_RUN_LOCK points at a temp file, so this control
# never touches the live data/wm-trace/.run-lock. Case 4 covers the default path.
# Case 1 is also the run-script-scope test: the holder is what wm_run.sh runs to
# hold the lock across a whole run, so a second agent starting mid-run refuses.
# NOT COVERED HERE: that a tick carrying the run's own FUTON_WM_RUN_LOCK_TOKEN
# passes through -- proving that cross-process means running a real tick, so it
# is asserted in test/wm_run_lock_test.clj instead.
#
# Usage: bash holes/labs/wm-contract/run_lock_negative_control.sh
set -uo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/../../.." || exit 2   # futon2 root
ROOT="$PWD"
LOCK="$(mktemp -u /tmp/futon2-run-lock-negctl-XXXXXX.lock)"
export FUTON_WM_RUN_LOCK="$LOCK"
FAIL=0
ok()   { echo "  PASS  $*"; }
bad()  { echo "  FAIL  $*"; FAIL=1; }
cleanup() { [ -n "${HOLDER_PID:-}" ] && kill -9 "$HOLDER_PID" 2>/dev/null; rm -f "$LOCK"; }
trap cleanup EXIT

echo "RUN12 negative control -- lock=$LOCK  sha=$(git -C "$ROOT" rev-parse --short HEAD)"

# ---- 1. a live holder makes the real runner refuse -------------------------
echo "[1] real run-tick-once against a held lock"
clojure -M -m futon2.wm-run-lock hold 180 negctl-holder > /tmp/negctl-holder.out 2>&1 &
HOLDER_PID=$!
for _ in $(seq 1 120); do grep -q ':pid' "$LOCK" 2>/dev/null && break; sleep 1; done
if ! grep -q ':pid' "$LOCK" 2>/dev/null; then bad "holder never took $LOCK"; exit 1; fi
HELD_PID=$(sed -n 's/.*:pid \([0-9]*\).*/\1/p' "$LOCK")
ok "holder took the lock, pid $HELD_PID"

RECEIPTS_BEFORE=$(ls "$ROOT"/holes/labs/wm-contract/tick-run-record-*.edn 2>/dev/null | wc -l)
TRACE_BEFORE=$(date -u +%Y-%m-%d); TRACE_FILE="$ROOT/data/wm-trace/wm-trace-$TRACE_BEFORE.edn"
TRACE_SIZE_BEFORE=$(stat -c %s "$TRACE_FILE" 2>/dev/null || echo 0)

START=$(date +%s)
clojure -M -m futon2.run-tick-once 14 > /tmp/negctl-second.out 2>/tmp/negctl-second.err
RC=$?
ELAPSED=$(( $(date +%s) - START ))

[ "$RC" -ne 0 ] && ok "second runner exited non-zero (rc=$RC)" || bad "second runner exited 0"
grep -q "refused" /tmp/negctl-second.err && ok "stderr says refused" || bad "stderr does not say refused"
grep -q "pid $HELD_PID" /tmp/negctl-second.err && ok "refusal names the holder's pid $HELD_PID" \
  || bad "refusal does not name pid $HELD_PID: $(head -c 400 /tmp/negctl-second.err)"
grep -q "negctl-holder" /tmp/negctl-second.err && ok "refusal names the holding agent" \
  || bad "refusal does not name the holding agent"
grep -q "will not wait and will not" /tmp/negctl-second.err && ok "refusal states it neither waits nor proceeds" \
  || bad "refusal does not state the policy"

RECEIPTS_AFTER=$(ls "$ROOT"/holes/labs/wm-contract/tick-run-record-*.edn 2>/dev/null | wc -l)
TRACE_SIZE_AFTER=$(stat -c %s "$TRACE_FILE" 2>/dev/null || echo 0)
[ "$RECEIPTS_BEFORE" = "$RECEIPTS_AFTER" ] && ok "refused runner wrote no receipt ($RECEIPTS_AFTER)" \
  || bad "receipt count moved $RECEIPTS_BEFORE -> $RECEIPTS_AFTER"
[ "$TRACE_SIZE_BEFORE" = "$TRACE_SIZE_AFTER" ] && ok "refused runner appended no trace record (${TRACE_SIZE_AFTER}B)" \
  || bad "trace file grew $TRACE_SIZE_BEFORE -> $TRACE_SIZE_AFTER"
echo "  note  refusal took ${ELAPSED}s (JVM start; the lock is taken before any store read)"

# ---- 2. the lock is released after the holder finishes ---------------------
echo "[2] release"
kill -TERM "$HOLDER_PID" 2>/dev/null; wait "$HOLDER_PID" 2>/dev/null
clojure -M -m futon2.wm-run-lock hold 1 negctl-brief > /tmp/negctl-brief.out 2>&1
grep -q ':released? true' /tmp/negctl-brief.out && ok "a completed hold releases the lock" \
  || bad "hold did not release: $(cat /tmp/negctl-brief.out)"
[ ! -e "$LOCK" ] && ok "lock file gone after release" || bad "lock file survives release"

# ---- 3. a lock whose pid is gone is reclaimed, and the reclaim is logged ----
echo "[3] stale reclaim"
clojure -M -m futon2.wm-run-lock hold 180 negctl-doomed > /tmp/negctl-doomed.out 2>&1 &
HOLDER_PID=$!
for _ in $(seq 1 120); do grep -q ':pid' "$LOCK" 2>/dev/null && break; sleep 1; done
DOOMED_PID=$(sed -n 's/.*:pid \([0-9]*\).*/\1/p' "$LOCK")
# kill the JVM that holds the lock (the pid recorded in the file) and its launcher
kill -9 "$DOOMED_PID" 2>/dev/null; pkill -9 -P "$HOLDER_PID" 2>/dev/null
kill -9 "$HOLDER_PID" 2>/dev/null; wait "$HOLDER_PID" 2>/dev/null
sleep 2
[ -e "$LOCK" ] && ok "SIGKILL left the lock behind (pid $DOOMED_PID)" || bad "lock vanished; nothing stale to reclaim"
clojure -M -m futon2.wm-run-lock hold 1 negctl-reclaimer > /tmp/negctl-reclaim.out 2>/tmp/negctl-reclaim.err
grep -q "reclaiming stale lock" /tmp/negctl-reclaim.err && ok "stale lock reclaimed" \
  || bad "stale lock not reclaimed: $(head -c 400 /tmp/negctl-reclaim.err)"
grep -q "$DOOMED_PID" /tmp/negctl-reclaim.err && ok "the reclaim log names what it took over" \
  || bad "reclaim log does not name pid $DOOMED_PID"
HOLDER_PID=""

# ---- 4. a TERMed holder releases (this is how wm_run.sh ends a run) --------
echo "[4] release on SIGTERM"
clojure -M -m futon2.wm-run-lock hold 180 negctl-termed > /tmp/negctl-termed.out 2>&1 &
HOLDER_PID=$!
for _ in $(seq 1 120); do grep -q ':pid' "$LOCK" 2>/dev/null && break; sleep 1; done
TERM_PID=$(sed -n 's/.*:pid \([0-9]*\).*/\1/p' "$LOCK")
kill -TERM "$TERM_PID" 2>/dev/null; kill -TERM "$HOLDER_PID" 2>/dev/null
wait "$HOLDER_PID" 2>/dev/null; sleep 2
[ ! -e "$LOCK" ] && ok "SIGTERM released the lock (shutdown hook)" \
  || bad "SIGTERM left the lock behind: $(cat "$LOCK" 2>/dev/null)"
HOLDER_PID=""

# ---- 5. the default path is the one beside the trace files -----------------
echo "[5] default path"
DEFAULT=$(cd "$ROOT" && env -u FUTON_WM_RUN_LOCK clojure -M -e \
  '(require (quote futon2.wm-run-lock)) (print (futon2.wm-run-lock/default-lock-path))' 2>/dev/null)
[ "$DEFAULT" = "$ROOT/data/wm-trace/.run-lock" ] && ok "default lock path is $DEFAULT" \
  || bad "default lock path is '$DEFAULT', expected $ROOT/data/wm-trace/.run-lock"

echo
[ "$FAIL" = 0 ] && echo "RUN12 negative control: PASS" || echo "RUN12 negative control: FAIL"
exit "$FAIL"
