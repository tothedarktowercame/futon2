#!/usr/bin/env bash
# Negative controls for runtime_validation_check.bb (row :U36).
#
# A checker that cannot refuse proves nothing, so each control plants ONE
# defect into a TEMP COPY of the catalog and asserts that the check refuses
# AND that it refuses for the planted reason -- not merely that it exited 1.
# A check that refused for a different reason would pass a control that only
# looked at the exit code (the failure mode run3_conformance_controls.bb
# guards against by asserting the class word alongside the exit).
#
# The shared registries are never touched: every mutation is to a file under
# a temp directory, so a mid-run failure cannot leave a mutated source behind
# for a concurrent lane.
#
# Usage: bash holes/labs/wm-contract/runtime_validation_controls.sh
set -uo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CAT="$HERE/runs/RUNTIME-VALIDATION-CATALOG.edn"
MD="$HERE/runs/RUNTIME-VALIDATION-CATALOG.md"
T="$(mktemp -d /tmp/u36-controls-XXXXXX)"
trap 'rm -rf "$T"' EXIT
FAIL=0
pass() { echo "  PASS $1"; }
fail() { echo "  FAIL $1"; FAIL=1; }

run_check() { CATALOG="$1" CATALOG_MD="$MD" bb "$HERE/runtime_validation_check.bb" 2>&1; }

# Each control: name, sed program, substring the refusal must contain.
control() {
  local name="$1" prog="$2" want="$3"
  local f="$T/c.edn"
  sed "$prog" "$CAT" > "$f"
  if cmp -s "$f" "$CAT"; then
    fail "$name (the plant changed nothing -- the control is vacuous)"; return
  fi
  local out; out="$(run_check "$f")"; local code=$?
  if [ "$code" -eq 0 ]; then
    fail "$name (check exited 0 on a planted defect)"
  elif ! grep -qF -e "$want" <<<"$out"; then
    fail "$name (refused, but not for the planted reason; wanted: $want)"
    sed -n '1,6p' <<<"$out" | sed 's/^/      /'
  else
    pass "$name"
  fi
}

echo "runtime_validation_controls: planting into temp copies under $T"

# 1. an invented pointer must fail naming it
control "invented pointer is refused by name" \
  '0,/:pointer "futon2\/test/s||:pointer "futon2/test/futon2/aif/no_such_file_test.clj:1-4"\n   :ignored "|' \
  "UNRESOLVED futon2/test/futon2/aif/no_such_file_test.clj:1-4"

# 2. a status outside the declared vocabulary must fail naming the row
control "unknown :status is refused by name" \
  's/:status :named-gap/:status :probably-fine/' \
  "is not in the declared vocabulary"

# 3. a red run typed as coverage must fail
control "a red run typed :red->:exists is refused" \
  's/:evidence-kind :test-green :status :red/:evidence-kind :test-green :status :exists/' \
  "-- a red run is :red"

# 4. a :gates-flip naming a flip that does not exist must fail
control "an unknown flip reference is refused" \
  's/:gates-flip \[:mission-c\]/:gates-flip [:flip-that-does-not-exist]/' \
  "which is not a key of :flips"

# 5. a :test-green row naming a namespace with no recorded run must fail
control "a test row with no recorded run is refused" \
  's/:ns "futon2.aif.efe-test"/:ns "futon2.aif.efe-test-that-was-never-run"/' \
  "is not in :test-runs"

# 6. POSITIVE CONTROL. Without this, "refuses everything" would pass all five
#    controls above. The catalog as it stands must pass, exit 0.
out="$(run_check "$CAT")"; code=$?
if [ "$code" -ne 0 ]; then
  fail "positive control (the real catalog does not pass)"
  sed -n '1,8p' <<<"$out" | sed 's/^/      /'
elif ! grep -q "runtime_validation_check: PASS" <<<"$out"; then
  fail "positive control (exit 0 without a PASS line)"
else
  pass "positive control: the catalog as it stands passes"
fi

# 7. POSITIVE CONTROL on the narrative binding: the COUNTS line the checker
#    computes must be the one the md carries. A drifted narrative is caught.
if grep -qF "$(CATALOG_MD="$MD" bb "$HERE/runtime_validation_check.bb" --summary)" "$MD"; then
  pass "positive control: the narrative carries the computed COUNTS line"
else
  fail "positive control (the md does not carry the computed COUNTS line)"
fi

if [ "$FAIL" -eq 0 ]; then
  echo "runtime_validation_controls: PASS (5 negative, 2 positive) exit-convention=0-pass/1-fail"
else
  echo "runtime_validation_controls: FAIL exit-convention=0-pass/1-fail"
fi
exit "$FAIL"
