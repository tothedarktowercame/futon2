#!/usr/bin/env bash
# RUN12 -- run N war-machine ticks under ONE run lock.
#
# The lock (data/wm-trace/.run-lock) is taken before the first tick and released
# after the last, so a second agent starting anywhere in the run refuses instead
# of interleaving records into the shared per-date trace file. Each tick sees
# the run's own token in FUTON_WM_RUN_LOCK_TOKEN, passes through, and does not
# release; this script does.
#
# Usage: bash holes/labs/wm-contract/wm_run.sh <ticks> [days] [agent]
#   e.g. FUTON_WM_TRACE_POLICY_DETAILS=1 bash .../wm_run.sh 20 14 claude-20
# Run the pre-flight first (holes/labs/wm-contract/r6_zero_post_preflight.clj).
set -uo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/../../.." || exit 2   # futon2 root
TICKS="${1:?usage: wm_run.sh <ticks> [days] [agent]}"; DAYS="${2:-14}"
AGENT="${3:-${FUTON_WM_AGENT:-$USER}}"
HOLD_SECONDS=$(( TICKS * 300 + 300 ))   # generous; the script releases early

clojure -M -m futon2.wm-run-lock hold "$HOLD_SECONDS" "$AGENT" > /tmp/wm-run-lock-holder.out 2>&1 &
HOLDER=$!
release() { kill -TERM "$HOLDER" 2>/dev/null; wait "$HOLDER" 2>/dev/null; }
trap release EXIT

for _ in $(seq 1 120); do grep -q ':token' /tmp/wm-run-lock-holder.out 2>/dev/null && break; sleep 1; done
if ! grep -q ':token' /tmp/wm-run-lock-holder.out 2>/dev/null; then
  echo "wm_run.sh: could not take the run lock:"; cat /tmp/wm-run-lock-holder.out; exit 3
fi
export FUTON_WM_RUN_LOCK_TOKEN
FUTON_WM_RUN_LOCK_TOKEN=$(sed -n 's/.*:token "\([^"]*\)".*/\1/p' /tmp/wm-run-lock-holder.out)
echo "wm_run.sh: run lock held by $AGENT, sha $(git rev-parse --short HEAD), $TICKS ticks"

RC=0
for i in $(seq 1 "$TICKS"); do
  echo "--- tick $i/$TICKS $(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  clojure -M -m futon2.run-tick-once "$DAYS" || { RC=$?; echo "tick $i exit=$RC"; break; }
done
echo "wm_run.sh: done rc=$RC"
exit "$RC"
