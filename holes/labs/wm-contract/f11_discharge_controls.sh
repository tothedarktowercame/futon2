#!/usr/bin/env bash
# f11_discharge_controls.sh -- `:F11` slice 3 negative controls against
# `mathlib4/DarkTower/WarMachine/F11DischargeArm.lean`.
#
# Each control plants ONE defect into a COPY, VERIFIES the plant is present in
# that copy before building (and, where the planted text is a substring of the
# original, verifies the removed text is GONE -- otherwise the "verification"
# passes on an unplanted file and checks nothing), then elaborates the copy and
# requires a failure.  The source file is never modified.
#
# Review note (2026-09-07): as dispatched, controls 3 and 4 grepped for a string
# that the UNPLANTED file also contains, so neither established its plant.  Both
# now assert the removal.  Controls 5 and 6 are new, for the review additions.
set -euo pipefail

src=/home/joe/code/mathlib4/DarkTower/WarMachine/F11DischargeArm.lean
repo=/home/joe/code/mathlib4
tmpdir=$(mktemp -d)
trap 'rm -rf "$tmpdir"' EXIT

# run_control NAME PRESENT-STRING [ABSENT-STRING]
run_control() {
  local name=$1 planted=$2 removed=${3-}
  echo "== CONTROL $name"
  echo "-- plant present:"
  grep -nF "$planted" "$tmpdir/$name.lean" | head -3
  if [ -n "$removed" ]; then
    local n
    n=$(grep -cF "$removed" "$tmpdir/$name.lean" || true)
    echo "-- removed text \"$removed\": $n occurrences (must be 0)"
    [ "$n" -eq 0 ] || { echo "PLANT NOT ESTABLISHED"; return 1; }
  fi
  if (cd "$repo" && lake env lean "$tmpdir/$name.lean") >"$tmpdir/$name.out" 2>&1; then
    echo "UNEXPECTED PASS"; cat "$tmpdir/$name.out"; return 1
  fi
  grep -E '^[^ ].*error:' -A4 "$tmpdir/$name.out" | head -14
  echo
}

# 1. findSilent given the typed absence F1 requires -> its refutation must fail.
cp "$src" "$tmpdir/silent.lean"
sed -i '0,/absence := none/s//absence := some .noPatternAddressesThisTension/' "$tmpdir/silent.lean"
run_control silent 'absence := some .noPatternAddressesThisTension' \
  'receipts := fun _ => none, absence := none'

# 2. findAllBut returning the whole repository -> reading-A falsifiability must fail.
cp "$src" "$tmpdir/all.lean"
sed -i 's/let selected := if q ∈ repo.patterns then repo.patterns \\ {q} else ∅/let selected := repo.patterns/' "$tmpdir/all.lean"
run_control all 'let selected := repo.patterns' \
  'let selected := if q ∈ repo.patterns then repo.patterns \ {q} else ∅'

# 3. findOpaqueRefusing's body removed -> synthesis fails, and the instance
#    declared LATER in the file does not rescue it.
cp "$src" "$tmpdir/opaque_body.lean"
sed -i 's/opaque findOpaqueRefusing : FindType Unit SnatchPattern := findRefusing/opaque findOpaqueRefusing : FindType Unit SnatchPattern/' "$tmpdir/opaque_body.lean"
run_control opaque_body 'opaque findOpaqueRefusing : FindType Unit SnatchPattern' \
  'opaque findOpaqueRefusing : FindType Unit SnatchPattern := findRefusing'

# 4. the local Nonempty instance removed -> the bodiless opaque must fail.
cp "$src" "$tmpdir/no_instance.lean"
sed -i '/R3.2:/,+2d' "$tmpdir/no_instance.lean"
run_control no_instance 'opaque findOpaqueNoBody : FindType Unit SnatchPattern' \
  'findTypeNonemptyInstance'

# 5. REVIEW ADDITION. findAllButFailsReadingB aimed at g1Snatcher instead of
#    g4Snatcher -> must fail, because at g1 the declared zero-mass member IS the
#    one findAllBut excludes.  This is what keeps the theorem a claim about the
#    second scenario's zero-mass member rather than about zero-mass in general.
cp "$src" "$tmpdir/reading_b.lean"
sed -i 's/have := (h { context := FindSnatchScenario.g4Snatcher, want := True, however := True }/have := (h { context := FindSnatchScenario.g1Snatcher, want := True, however := True }/' "$tmpdir/reading_b.lean"
run_control reading_b 'FindSnatchScenario.g1Snatcher, want := True, however := True }
    SnatchPattern.forcedPlayNeedsALossFloor' \
  'FindSnatchScenario.g4Snatcher, want := True, however := True }'

# 6. REVIEW ADDITION. the located reading-A disagreement pointed at ONE pattern
#    twice -> must fail, since a finder cannot both select and exclude it.  This
#    is what keeps the floor a statement about two DIFFERENT finders.
cp "$src" "$tmpdir/pair.lean"
sed -i "s/let q' := SnatchPattern.forcedPlayNeedsALossFloor/let q' := SnatchPattern.consultTheRemedyBeforeExiting/" "$tmpdir/pair.lean"
run_control pair "let q' := SnatchPattern.consultTheRemedyBeforeExiting" \
  "let q' := SnatchPattern.forcedPlayNeedsALossFloor"

echo "ALL 6 CONTROLS REJECTED THEIR PLANTS"
