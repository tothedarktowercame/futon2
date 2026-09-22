#!/usr/bin/env bash
# :F11 slice 7 negative controls for the F3 non-self-certification measurements.
#
# Every plant is located by DECLARATION NAME, never by line number, and the
# planted text is verified PRESENT and the replaced text verified ABSENT on the
# planted line before the build -- these strings recur across the module, so a
# bare grep would pass on an unmodified copy.
set -euo pipefail

src=/home/joe/code/mathlib4/DarkTower/WarMachine/F11NonSelfCertifying.lean
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

# M1: the assertion-only receipt is the whole point -- it must assert BOTH
# propositions. Setting scoreAlone true makes today's F3 unprovable of it.
plant m1-asserted-receipt assertedNonSelfCertifyingReceipt \
  'scoreAlone := False' \
  'scoreAlone := True'

# M3: the separating witness must PASS the two-conjunct reading; dropping its
# file makes F3Two false and the first conjunct of the separation unprovable.
# (The first version of this control planted `ifLines := some (15, 16)` and did
# NOT fail -- F3Four still requires `ifText`, so the witness still separated the
# readings. A plant that does not fail tests nothing; it was discarded and
# replaced by this one.)
plant m3-two-only-warrant twoOnlyWarrant \
  'file := some "recorded-pattern.md"' \
  'file := none'

# M3: the four-conjunct reading must ADD to the two-conjunct one; making it a
# synonym destroys the non-equivalence.
plant m3-four-conjunct F3Four \
  'F3Two w && w.ifLines.isSome && w.ifText.isSome' \
  'F3Two w'

# M5: the F2-not-F3 finder must issue a SCORE-ALONE receipt; a citing receipt
# satisfies F3 and the refutation fails.
plant m5-score-alone findF2NotF3 \
  'some { citesTextOrEdges := False, scoreAlone := True } else none' \
  'some { citesTextOrEdges := True, scoreAlone := False } else none'

# M5: the F3-not-F2 finder satisfies F3 only VACUOUSLY, by returning no receipt;
# returning one makes the F2 refutation fail.
plant m5-no-receipts findF3NotF2 \
  'receipts := fun _ => none' \
  'receipts := fun _ => some { citesTextOrEdges := True, scoreAlone := False }'

# REVIEW ADDITIONS (:F11 slice 7 review). One plant per added declaration.

# R1: the misciting finder must cite ANOTHER pattern; citing its own makes the
# refutation of `CitesTheSelectedPattern` false.
plant r1-misciting findCMisciting \
  'citedText := misattributedReceiptOwner p' \
  'citedText := p'

# R2: F3's WHICH must be read off the finder's OWN returned receipt. Comparing
# the citation to itself is the degenerate shape slice 4 recorded
# (F11ReceiptCarrier.lean:126-128) and holds of every finder, so the misciting
# refutation stops closing.
plant r2-own-output CitesTheSelectedPattern \
  '∃ r, (f t repo).receipts p = some r ∧ r.citedText = p' \
  '∃ r, (f t repo).receipts p = some r ∧ r.citedText = r.citedText'

# R3: the citation erasure must DROP the citation. Keeping a citation-dependent
# receipt makes the two erasures unequal.
plant r3-citation-erasure FindResultC.erase \
  'receipts := fun p => (r.receipts p).map (·.toLegacyReceipt)' \
  'receipts := fun p => (r.receipts p).map (fun c => { citesTextOrEdges := c.citedText = p, scoreAlone := False })'

# R6/floor: the faithful citing witness must SELECT on the recorded repository.
plant r6-citing-floor findCFaithful_nonempty \
  '⟨.askForSurplusNotSurrender, by' \
  '⟨.consultTheRemedyBeforeExiting, by'

# R5: the column equality must be DECIDED against the rows, not closed trivially.
# Negating it must fail. (The first version planted `row.receipted =
# row.receipted`, which WEAKENS the theorem and therefore compiles -- a plant that
# does not fail tests nothing. Discarded and replaced by this one.)
plant r5-column-equality recordedRounds_f2_f3_columns_equal \
  'row.receipted = row.nonSelfCertifying' \
  'row.receipted ≠ row.nonSelfCertifying'

# R5b: the three-column equality is decided the same way; negating its first
# conjunct must fail.
plant r5b-three-columns recordedRounds_all_three_columns_equal \
  'row.selected = row.receipted ∧ row.receipted = row.nonSelfCertifying' \
  'row.selected ≠ row.receipted ∧ row.receipted = row.nonSelfCertifying'

echo 'ALL 11 CONTROLS FAILED AS REQUIRED'
