#!/usr/bin/env bash
# :F11 slice 6 negative controls AGAINST f11_amended_carrier_check.bb itself.
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
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

run_plant() {
  local name=$1 verdict=$2; shift 2
  local out="$tmp/$name.out"
  if env "$@" F11AC_OUT="$tmp/$name.edn" bb "$lab/f11_amended_carrier_check.bb" >"$out" 2>&1; then
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

# 1. The amendment is TAKEN: `Receipt` grows the clause field, so the arm is no
#    longer pricing an option.
edit "$dark/WarMachine/Holes.lean" "$tmp/holes-receipt.lean" \
  '  scoreAlone : Prop' '  acknowledgedClause : Prop'
run_plant amendment-taken receipt-carrier-is-no-longer-the-two-proposition-carrier \
  F11AC_HOLES="$tmp/holes-receipt.lean"

# 2. `zeroMass` leaves the ROW carrier, which is the whole reason F4 is not a
#    conjunct of today's `ConformantFind`.
edit "$dark/WarMachine/Holes.lean" "$tmp/holes-row.lean" \
  '  zeroMass : Set P' '  zeroMassMoved : Set P'
run_plant zero-mass-off-the-row zero-mass-no-longer-on-the-row-carrier \
  F11AC_HOLES="$tmp/holes-row.lean"

# 3. Today's F2 stops stating presence only, which is the baseline the receipt
#    half's buy is measured against.
edit "$dark/WarMachine/F11Conformance.lean" "$tmp/conf-f2.lean" \
  '    ((f t repo).receipts p).isSome' '    ((f t repo).receipts p).isNone'
run_plant f2-not-presence todays-f2-no-longer-states-presence-only \
  F11AC_CONF="$tmp/conf-f2.lean"

# 4. Slice 4's erasure-equality benchmark goes, so M2's separation stops being a
#    buy and becomes a difference.
edit "$dark/WarMachine/F11ReceiptCarrier.lean" "$tmp/receipt-benchmark.lean" \
  '    eraseFinder findRFaithful = eraseFinder findRMisattributing' \
  '    eraseFinder findRFaithful = eraseFinder findRFaithful'
run_plant benchmark-gone slice-4-erasure-equality-benchmark-missing-or-changed \
  F11AC_RECEIPT="$tmp/receipt-benchmark.lean"

# 5. Reading C stops taking its designation as a parameter, so the arm's
#    "implies C when externally pinned" result is about a different predicate.
edit "$dark/WarMachine/F11F4Reading.lean" "$tmp/f4-c.lean" \
  'def FindRespectsZeroMass {State P : Type*} (zm : State → Set P)' \
  'def FindRespectsZeroMass {State P : Type*} (zm : Unit → Set P)'
run_plant reading-c-changed reading-c-missing-or-changed \
  F11AC_F4="$tmp/f4-c.lean"

# 6. The sorry the entry prices is discharged, so there is nothing left to price.
edit "$dark/WarMachine/Holes.lean" "$tmp/holes-find.lean" \
  'def find {State P : Type*} : Tension State → Repository P → FindResult P := sorry' \
  'def find {State P : Type*} : Tension State → Repository P → FindResult P := fun _ _ => { selected := ∅, receipts := fun _ => none, absence := none }'
run_plant discharged-sorry find-not-declared-as-a-sorry \
  F11AC_HOLES="$tmp/holes-find.lean"

echo 'ALL 6 CHECKER CONTROLS FAILED AS REQUIRED'
