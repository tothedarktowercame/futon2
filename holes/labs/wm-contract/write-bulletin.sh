#!/usr/bin/env bash
# write-bulletin.sh -- generate the day's morning bulletin and commit it.
#
# Called at a build loop's natural end-of-session stop (wm-build-loop.sh and
# zaif-harness/zaif-build-loop.sh), never on a timer: worklist row :B1 rules
# out a new daemon, and a digest of a session is due when the session ends.
#
# Idempotent by construction: futon2.aif.bulletin rewrites BULLETIN-<date>.md
# only when the day's content changed, so a second stop on the same day
# generates the same bytes and commits nothing.
#
# Never exits non-zero: a missed bulletin must not turn a finished loop into a
# failed one. Whatever happened is logged.
set -uo pipefail
LOG="${1:-/dev/null}"
BULLETINS="holes/labs/wm-contract/bulletins"
cd "$HOME/code/futon2" || { echo "[bulletin] no futon2 checkout" >> "$LOG"; exit 0; }

# The gate's exit code is read from the command itself, before any pipe --
# a piped status is the pipe's, not the generator's (futon2/AGENTS.md).
out="$(clojure -M -m futon2.aif.bulletin 2>&1)"; rc=$?
printf '%s\n' "$out" | tee -a "$LOG"
if [ "$rc" -ne 0 ]; then
  echo "[bulletin] generator exit=$rc -- nothing committed" | tee -a "$LOG"
  exit 0
fi

# Stage the bulletin path and nothing else: this checkout is shared between
# seats, so a bare `git commit -a` would sweep another seat's work into it.
if [ -z "$(git status --porcelain -- "$BULLETINS")" ]; then
  echo "[bulletin] no change under $BULLETINS -- nothing to commit" | tee -a "$LOG"
  exit 0
fi
git add -- "$BULLETINS" \
  && git commit -q -m "bulletin: the day's digest (build loop end-of-session, :B1)" -- "$BULLETINS" \
  && echo "[bulletin] committed $(git rev-parse --short HEAD)" | tee -a "$LOG"
exit 0
