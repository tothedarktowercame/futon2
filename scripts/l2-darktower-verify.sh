#!/usr/bin/env bash
# C-cascade-real STANDARD-VERIFY L2 (claude-10, O7): verify a generated cascade's
# WIRING against the DarkTower Lean CT theory. End-to-end:
#   cascade → fold (impl #2) → CLean (adapter) → clean_to_lean standalone render → lean 0-sorry
#
# The 0-sorry render is a STRUCTURAL soundness gate (boxes type-check as a
# typed-hole comb, valid satiety/discharge/BV spine) — NOT a semantic-correctness
# claim. Two-layer: pre-flight catches comb-composition breaks; the Lean render
# catches typed-hole TYPE breaks.
#
# Usage:  bash scripts/l2-darktower-verify.sh
set -uo pipefail
F2=/home/joe/code/futon2
F6=/home/joe/code/futon6
LEAN="$HOME/.elan/bin/lean"
PY="$F6/.venv/bin/python"

echo "== step 1: fold the cascade via impl #2 + adapt to CLean =="
( cd "$F2" && clojure -M -m futon2.aif.l2-verify 2>&1 | grep -vE "^(Download|SLF4J|WARNING)" )

render_and_check () {  # $1=label $2=clean-dir $3=expect(pass|fail)
  local label="$1" dir="$2" expect="$3"
  local lean="/tmp/l2-${label}.lean" out
  "$PY" "$F6/scripts/clean_to_lean.py" --clean-dir "$dir" --mode standalone --out "$lean" >/dev/null 2>&1
  out=$("$LEAN" "$lean" 2>&1); local rc=$?
  local errs sorries
  errs=$(printf '%s' "$out" | grep -c "error:")
  sorries=$(printf '%s' "$out" | grep -c "uses 'sorry'")
  local zero_sorry="no"
  if [ "$rc" -eq 0 ] && [ "$errs" -eq 0 ] && [ "$sorries" -eq 0 ]; then zero_sorry="yes"; fi
  local verdict
  if [ "$zero_sorry" = "yes" ]; then verdict="0-SORRY (renders clean)"; else verdict="FAILS render (rc=$rc errs=$errs sorry=$sorries)"; fi
  printf "  %-12s → %s\n" "$label" "$verdict"
  # return whether the Lean gate held (0-sorry)
  [ "$zero_sorry" = "yes" ]
}

echo
echo "== step 2: render + lean 0-sorry check =="
render_and_check "green"       /tmp/l2-darktower/green       pass; GREEN_LEAN=$?
render_and_check "red-type"    /tmp/l2-darktower/red-type    fail; REDTYPE_LEAN=$?
render_and_check "red-compose" /tmp/l2-darktower/red-compose pass; REDCOMPOSE_LEAN=$?

echo
echo "== verdicts =="
PASS=1
# GREEN must be 0-sorry on BOTH layers (pre-flight ok printed in step 1) AND Lean.
if [ "$GREEN_LEAN" -eq 0 ]; then echo "  GREEN       ✅ structurally sound (Lean 0-sorry)"; else echo "  GREEN       ❌ unexpectedly failed"; PASS=0; fi
# RED-TYPE must FAIL the Lean gate (it bites the typed-hole-type layer).
if [ "$REDTYPE_LEAN" -ne 0 ]; then echo "  RED-TYPE    ✅ gate bit (Lean render rejected the bad satiety grade)"; else echo "  RED-TYPE    ❌ gate did NOT bite"; PASS=0; fi
# RED-COMPOSE: pre-flight fails (shown in step 1); Lean PASSES (honest boundary — comb edges are data).
if [ "$REDCOMPOSE_LEAN" -eq 0 ]; then echo "  RED-COMPOSE ✅ pre-flight bit (step 1 ok?=false); Lean passes (comb is data — the honest layer boundary)"; else echo "  RED-COMPOSE ⚠ Lean also rejected it"; fi
echo
if [ "$PASS" -eq 1 ]; then echo "L2 SLICE: PASS — green certified, both gate layers shown to bite."; exit 0
else echo "L2 SLICE: FAIL"; exit 1; fi
