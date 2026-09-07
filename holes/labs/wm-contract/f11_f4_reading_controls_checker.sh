#!/usr/bin/env bash
# :F11 slice 5 negative controls AGAINST f11_f4_reading_check.bb itself.
#
# Each plant is made on a COPY of one input file, the checker is pointed at that
# copy through its env var, and the run must (a) fail and (b) report the ONE
# verdict the plant is for. Slice 3 recorded why the second half matters: a
# checker that recognised its subject by a hard-coded path reported three
# verdicts for every plant, so the plants established nothing about which
# verdict was moving.
set -euo pipefail

lab=/home/joe/code/futon2/holes/labs/wm-contract
dark=/home/joe/code/mathlib4/DarkTower
f3=/home/joe/code/futon3/checks
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

run_plant() {
  local name=$1 verdict=$2; shift 2
  local out="$tmp/$name.out"
  if env "$@" F11F4_OUT="$tmp/$name.edn" bb "$lab/f11_f4_reading_check.bb" >"$out" 2>&1; then
    echo "CONTROL $name UNEXPECTED PASS" >&2; cat "$out" >&2; exit 1
  fi
  if ! grep -q "$verdict" "$out"; then
    echo "CONTROL $name did not report $verdict" >&2; cat "$out" >&2; exit 1
  fi
  local n
  n=$(sed -n 's/.*-- \([0-9]*\) findings.*/\1/p' "$out" | head -1)
  if [ "${n:-0}" -ne 1 ]; then
    echo "CONTROL $name moved $n verdicts, expected exactly 1" >&2; cat "$out" >&2; exit 1
  fi
  echo "CONTROL $name -> $verdict (exactly 1 verdict moved)"
}

# Each replacement is verified PRESENT and the replaced text verified ABSENT
# before the checker runs -- these strings recur, so a bare grep would pass on an
# unmodified copy.
edit() {
  local src=$1 dst=$2 old=$3 new=$4
  cp "$src" "$dst"
  python3 - "$dst" "$old" "$new" <<'PY'
import sys
path, old, new = sys.argv[1], sys.argv[2], sys.argv[3]
s = open(path).read()
assert s.count(old) >= 1, "target absent: " + old
open(path, "w").write(s.replace(old, new, 1))
PY
  grep -qF -- "$new" "$dst" || { echo "plant absent in $dst" >&2; exit 1; }
  if [ "$(grep -cF -- "$old" "$dst")" -ge "$(grep -cF -- "$old" "$src")" ]; then
    echo "replaced text still present at its original count in $dst" >&2; exit 1
  fi
}

# 1. Reading A stops being the forall-over-inputs existential.
edit "$dark/WarMachine/F11Conformance.lean" "$tmp/conf-a.lean" \
  '∀ t repo, repo.patterns.Nonempty → ∃ p ∈ repo.patterns, p ∉ (f t repo).selected' \
  '∀ t repo, repo.patterns.Nonempty → True'
run_plant reading-a reading-a-is-no-longer-the-forall-over-inputs-existential \
  F11F4_CONF="$tmp/conf-a.lean"

# 2. Reading B stops being record-grain -- given the repository quantifier it IS C.
edit "$dark/WarMachine/F11DischargeArm.lean" "$tmp/discharge-b.lean" \
  '  ∀ t, ∀ p ∈ findSnatchZeroMass t.context,' \
  '  ∀ t repo, ∀ p ∈ findSnatchZeroMass t.context,'
run_plant reading-b reading-b-is-no-longer-record-grain \
  F11F4_DISCHARGE="$tmp/discharge-b.lean"

# 3. A scenario declares TWO zero-mass patterns. The distinct-value count stays 3,
#    so only the one-per-scenario verdict may move.
edit "$dark/WarMachine/Holes.lean" "$tmp/holes-zm.lean" \
  '  | .g1Snatcher => [.consultTheRemedyBeforeExiting]' \
  '  | .g1Snatcher => [.consultTheRemedyBeforeExiting, .forcedPlayNeedsALossFloor]'
run_plant designation zero-mass-designation-no-longer-one-pattern-per-scenario \
  F11F4_HOLES="$tmp/holes-zm.lean"

# 4. The record's F4 omission test is dropped.
edit "$f3/find_snatch.clj" "$tmp/find_snatch.clj" \
  '(not (contains? (set selected-union) zero-mass))' \
  '(some? zero-mass)'
run_plant record-f4 record-f4-omission-test-changed \
  F11F4_CHECK="$tmp/find_snatch.clj"

# 5. The sorry the entry prices is discharged, so there is nothing left to price.
edit "$dark/WarMachine/Holes.lean" "$tmp/holes-find.lean" \
  'def find {State P : Type*} : Tension State → Repository P → FindResult P := sorry' \
  'def find {State P : Type*} : Tension State → Repository P → FindResult P := fun _ _ => { selected := ∅, receipts := fun _ => none, absence := none }'
run_plant discharged-sorry find-not-declared-as-a-sorry \
  F11F4_HOLES="$tmp/holes-find.lean"

echo 'ALL 5 CHECKER CONTROLS FAILED AS REQUIRED'
