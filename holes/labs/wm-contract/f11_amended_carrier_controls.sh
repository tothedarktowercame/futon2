#!/usr/bin/env bash
# :F11 slice 6 negative controls for the joint amended-carrier measurements.
#
# Every plant is located by DECLARATION NAME, never by line number, and the
# planted text is verified PRESENT and the replaced text verified ABSENT on the
# planted line before the build -- these strings recur across the module, so a
# bare grep would pass on an unmodified copy.
set -euo pipefail

src=/home/joe/code/mathlib4/DarkTower/WarMachine/F11AmendedCarrier.lean
repo=/home/joe/code/mathlib4
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

locate() {
  local file=$1 decl=$2 needle=$3 start
  start=$(grep -n "^\(noncomputable \)\?\(theorem\|def\|structure\) $decl" "$file" |
    head -1 | cut -d: -f1)
  if [ -z "$start" ]; then echo "declaration not found: $decl" >&2; exit 1; fi
  awk -v n="$start" -v pat="$needle" 'NR>=n && index($0,pat){print NR; exit}' "$file"
}

plant() {
  local name=$1 decl=$2 old=$3 new=$4 line file
  file="$tmp/$name.lean"
  cp "$src" "$file"
  line=$(locate "$file" "$decl" "$old")
  if [ -z "$line" ]; then echo "$name: target absent in $decl" >&2; exit 1; fi
  python3 - "$file" "$line" "$old" "$new" <<'PY'
import sys
path, line, old, new = sys.argv[1], int(sys.argv[2]), sys.argv[3], sys.argv[4]
rows = open(path).readlines()
rows[line - 1] = rows[line - 1].replace(old, new, 1)
open(path, "w").writelines(rows)
PY
  if ! sed -n "${line}p" "$file" | grep -qF -- "$new"; then
    echo "$name: planted text absent on line $line" >&2; exit 1
  fi
  if sed -n "${line}p" "$file" | grep -qF -- "$old"; then
    echo "$name: replaced text remains on line $line" >&2; exit 1
  fi
  if (cd "$repo" && lake env lean "$file") >"$tmp/$name.out" 2>&1; then
    echo "CONTROL $name UNEXPECTED PASS" >&2; exit 1
  fi
  echo "CONTROL $name declaration=$decl line=$line plant=$old -> $new"
  grep -m1 -A3 'error:' "$tmp/$name.out" | sed 's/^/    /'
}

# M1: the joint F4 conjunct must be the RETURNED designation. Reading it off the
# repository instead makes the reading-B implication unprovable.
plant m1-returned-designation ConformantAmendedFind \
  'f4ReturnedZeroMass : ∀ t repo p, p ∈ (f t repo).zeroMass →' \
  'f4ReturnedZeroMass : ∀ t repo p, p ∈ repo.patterns →'

# M1b: pinning the returned field to the recorded designation is what carries
# `amendedF4ImpliesRecordedReadingB`; drop the hypothesis to its negation and the
# proof cannot close.
plant m1-pinning-hypothesis amendedF4ImpliesRecordedReadingB \
  '(hz : ∀ t, (f t findSnatchRepository).zeroMass = findSnatchZeroMassSet t.context)' \
  '(hz : ∀ t, (f t findSnatchRepository).zeroMass = (∅ : Set SnatchPattern))'

# M2: the receipt separation is by CARRIED CLAUSE. Making the misattributor
# faithful destroys it.
plant m2-receipt-separation amendedMisattributing \
  'acknowledgedClause := misattributedReceiptOwner p,' \
  'acknowledgedClause := p,'

# M2b: the amended faithful finder must erase to the RECORDED replay -- that tie
# is what makes its reading-A refutation and its reading-B/C satisfaction facts
# about the record rather than about a finder of the module's own invention.
plant m2-erasure-is-the-record eraseAmendedFaithful_selected \
  '(findSnatchReplay t repo).selected := rfl' \
  '(findIdentity t repo).selected := rfl'

# M3: the zero-mass-only pair must agree on receipts. Changing its receipts too
# would make the independence claim vacuous.
plant m3-independence amendedDifferentZeroMass \
  '{ r with zeroMass := ∅ }' \
  '{ r with zeroMass := ∅, receipts := fun _ => none }'

# M4/floor: the reverse-A witness must SELECT on the recorded repository.
plant m4-reverse-a-floor amendedAllButBadSelectionNonempty \
  '⟨.askForSurplusNotSurrender, by' \
  '⟨.consultTheRemedyBeforeExiting, by'

# REVIEW ADDITIONS (:F11 slice 6 review). One plant per added declaration, on the
# same terms as the six above.

# R1: the receipt pair's erasure equality is proved of the two named finders;
# pointing it at the identity finder is not the benchmark and does not hold.
plant r1-receipt-erasure amendedReceiptPairErasuresAreEqual \
  'eraseAmendedFinder amendedFaithful = eraseAmendedFinder amendedMisattributing' \
  'eraseAmendedFinder amendedFaithful = eraseAmendedFinder amendedIdentity'

# R2: the zero-mass pair's erasure equality holds by `rfl` only because erasure
# drops the designation; asserting it of the receipt pair does not close by rfl.
plant r2-zero-mass-erasure amendedZeroMassPairErasuresAreEqual \
  'eraseAmendedFinder amendedFaithful = eraseAmendedFinder amendedDifferentZeroMass := rfl' \
  'eraseAmendedFinder amendedFaithful = eraseAmendedFinder amendedMisattributing := rfl'

# R3: the price witness must return the EMPTY designation. Returning the
# recorded one makes it satisfy reading B and the refutation fails.
plant r3-price-witness amendedIdentity \
  'zeroMass := ∅' \
  'zeroMass := findSnatchZeroMassSet t.context ∩ repo.patterns'

# R4: the price witness must SELECT the whole repository; a refusing selection
# would refute reading A for the wrong reason and satisfy B and C.
plant r4-price-witness-selects amendedIdentityNotReadingB \
  '.consultTheRemedyBeforeExiting (by simp [findSnatchZeroMass])' \
  '.askForSurplusNotSurrender (by simp [findSnatchZeroMass])'

echo 'ALL 10 CONTROLS FAILED AS REQUIRED'
