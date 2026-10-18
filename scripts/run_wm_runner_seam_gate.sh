#!/usr/bin/env bash
set -euo pipefail

# Integration-shaped gate: seven runner scenarios, each capturing the live
# selection world. Bound it and retain elapsed time so slow is not confused
# with deadlocked.
exec timeout "${WM_RUNNER_SEAM_TIMEOUT:-60s}" \
  /usr/bin/time -f 'wm-runner-seam elapsed=%e exit=%x' \
  clojure -M:test -m cognitect.test-runner \
  -n futon2.aif.wm.cascade-runner-seam-test
