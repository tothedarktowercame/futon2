#!/usr/bin/env bash
# Negative and positive controls for the U37 enumeration-completeness replay.
#
# The defect this whole row exists to catch is a producer that quietly narrows
# its domain (holes/NOTE-the-whitelist-provenance.md: four missions, arrived at
# in one working day, recorded nowhere). So the first control PLANTS that
# narrowing into the recorded candidate list and requires the replay to refuse
# it, by name -- an exit code alone would also be produced by a checker that
# refuses everything, which is what the positive controls exclude.
#
# Every plant is an environment variable read by the replay; no file under
# version control is mutated, so a mid-run failure cannot leave a narrowed
# record behind for another lane.
#
# Usage: bash holes/labs/wm-contract/u37_enumeration_controls.sh
set -uo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../../.." && pwd)"
REPLAY="holes/labs/wm-contract/u37_enumeration_replay.clj"
MD="$HERE/runs/U37-enumeration-completeness/REPLAY-2026-09-02-RECORDS.md"
T="$(mktemp -d /tmp/u37-controls-XXXXXX)"
trap 'rm -rf "$T"' EXIT
FAIL=0
pass() { echo "  PASS $1"; }
fail() { echo "  FAIL $1"; FAIL=1; }

cd "$ROOT" || exit 2

# The replay writes its catalogue under runs/; controls send theirs to a temp
# dir so a planted catalogue is never left beside the real one.
run_replay() { ( cd "$ROOT" && env U37_OUT_DIR="$T/catalogues" "$@" clojure -M:test "$REPLAY" 2>&1 ); }

# name, want-substring, then the env assignments for the plant.
control() {
  local name="$1" want="$2"; shift 2
  local out code
  out="$(run_replay "$@")"; code=$?
  if [ "$code" -eq 0 ]; then
    fail "$name (replay exited 0 on a planted defect)"
  elif ! grep -qF -e "$want" <<<"$out"; then
    fail "$name (refused, but not for the planted reason; wanted: $want)"
    grep -E "VERDICT|MISSING|PHANTOM" <<<"$out" | sed -n '1,4p' | sed 's/^/      /'
  else
    pass "$name"
  fi
}

echo "u37_enumeration_controls: planting into the replay's environment only"

# 1. THE INCIDENT ITSELF. Four missions enumerated out of the 133 available.
control "a four-mission whitelist is refused, by count" \
  "MISSING, no typed reason (129)" U37_PLANT_WHITELIST=4

# 2. A narrowing that leaves almost everything in place must still fail: the
#    check must not have a tolerance below which a silent loss passes.
control "losing a single mission is refused" \
  "MISSING, no typed reason (1)" U37_PLANT_WHITELIST=132

# 3. A candidate for a mission that is not on disk is a phantom, not coverage.
control "an enumerated mission with no doc is refused" \
  "PHANTOM, enumerated but not on disk (1)" \
  U37_PLANT_PHANTOM=M-this-mission-has-no-doc

# 4. Claiming a proposer for a kind that has none must turn that kind's whole
#    population from a typed absence into untyped missing members.
control "a claimed proposer for an unenumerated kind is refused" \
  "MISSING, no typed reason (157)" U37_PLANT_KIND_ENUMERATOR=excursion

# 5. THE SCAN IS A MEASUREMENT, NOT AN ECHO. Point the scan at an empty code
#    root: the available population goes to zero and every recorded candidate
#    becomes a phantom. A check that merely re-read the record would pass this.
mkdir -p "$T/empty-root"
control "an empty code root turns every recorded candidate into a phantom" \
  "PHANTOM, enumerated but not on disk (133)" "U37_CODE_ROOT=$T/empty-root"

# 6. POSITIVE CONTROL. Five refusals would all be produced by a check that
#    refuses everything. The unplanted replay must pass.
out="$(run_replay U37_CONTROL=positive)"; code=$?
if [ "$code" -ne 0 ]; then
  fail "positive control (the unplanted replay does not pass)"
  grep -E "VERDICT|MISSING|PHANTOM" <<<"$out" | sed -n '1,6p' | sed 's/^/      /'
elif ! grep -q "VERDICT: 3/3 records complete" <<<"$out"; then
  fail "positive control (exit 0 without the 3/3 verdict line)"
else
  pass "positive control: the three recorded ticks pass unplanted"
fi

# 7. POSITIVE CONTROL on the narrative binding: the COUNTS line the replay
#    computes NOW must be the line the committed narrative carries. A narrative
#    that drifted from the population it describes is caught here.
if [ ! -f "$MD" ]; then
  fail "positive control (no narrative at $MD)"
elif grep -qF "$(clojure -M:test "$REPLAY" --summary)" "$MD"; then
  pass "positive control: the narrative carries the computed COUNTS line"
else
  fail "positive control (the narrative does not carry the computed COUNTS line)"
fi

if [ "$FAIL" -eq 0 ]; then
  echo "u37_enumeration_controls: PASS (5 negative, 2 positive) exit-convention=0-pass/1-fail"
else
  echo "u37_enumeration_controls: FAIL exit-convention=0-pass/1-fail"
fi
exit "$FAIL"
