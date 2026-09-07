#!/usr/bin/env bash
# f11_receipt_carrier_controls.sh -- `:F11` slice 4 negative controls.
#
# Six plants against DarkTower/WarMachine/F11ReceiptCarrier.lean.  Each must
# make the module FAIL to elaborate; a plant that still builds means the
# theorem it targets was not measuring what it claims.
#
# Two disciplines, both from slice 3's review:
#   - every plant verifies BOTH that the planted text is present AND that the
#     text it replaced is gone, checked ON THE PLANTED LINE rather than over the
#     whole file, because these strings recur (`citesTextOrEdges := True` occurs
#     in four receipts, `.askForSurplusNotSurrender` in seven places);
#   - the plants are located by DECLARATION NAME and not by a fixed line number,
#     so appending to the module does not silently retarget them.
set -euo pipefail
src=/home/joe/code/mathlib4/DarkTower/WarMachine/F11ReceiptCarrier.lean
repo=/home/joe/code/mathlib4
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

# First line at or after the declaration `$2` that contains `$3`.
locate() {
  local file=$1 decl=$2 needle=$3 start
  start=$(grep -n "^\(noncomputable \)\?\(theorem\|def\) $decl" "$file" | head -1 | cut -d: -f1)
  if [ -z "$start" ]; then echo "declaration not found: $decl" >&2; exit 1; fi
  awk -v n="$start" -v pat="$needle" 'NR>=n && index($0,pat){print NR; exit}' "$file"
}

# plant NAME DECL OLD NEW -- replace OLD by NEW on the first line of DECL that
# holds OLD, prove the swap happened on that line, and require the file to fail.
plant() {
  local name=$1 decl=$2 old=$3 new=$4 line f
  f="$tmp/$name.lean"
  cp "$src" "$f"
  line=$(locate "$f" "$decl" "$old")
  if [ -z "$line" ]; then echo "$name: text not found in $decl: $old" >&2; exit 1; fi
  python3 - "$f" "$line" "$old" "$new" <<'PY'
import sys
path, line, old, new = sys.argv[1], int(sys.argv[2]), sys.argv[3], sys.argv[4]
ls = open(path).readlines()
ls[line-1] = ls[line-1].replace(old, new, 1)
open(path, 'w').writelines(ls)
PY
  if ! sed -n "${line}p" "$f" | grep -qF "$new"; then
    echo "$name: planted text absent from line $line" >&2; exit 1; fi
  if sed -n "${line}p" "$f" | grep -qF "$old"; then
    echo "$name: replaced text still on line $line" >&2; exit 1; fi
  if (cd "$repo" && lake env lean "$f") >"$tmp/$name.out" 2>&1; then
    echo "CONTROL $name UNEXPECTED PASS -- the plant did not break anything" >&2; exit 1; fi
  echo "CONTROL $name (line $line): $old -> $new"
  grep -m2 -A4 'error:' "$tmp/$name.out" | sed 's/^/    /'
}

# 1. The owner is no longer wrong: the misattribution witness, the relational
#    refutation and the proposition-arm counterexample all lose their content.
plant owner misattributedReceiptOwner \
  '.consultTheRemedyBeforeExiting' '.askForSurplusNotSurrender'

# 2. One of the three proposition fields is false: the proposition arm's
#    measurement is that all three can be true WHILE the owner is wrong.
plant assertion misattributedReceiptWithAssertions \
  'hasAsOf := True' 'hasAsOf := False'

# 3. The hand-carried clause names another pattern: the M3 inhabitance witness
#    is about the clause it carries, not about carrying something.
plant relation handCarriedReceipt \
  'acknowledgedClause := .askForSurplusNotSurrender' \
  'acknowledgedClause := .consultTheRemedyBeforeExiting'

# 4. The two data-carrying finders now differ in a field that SURVIVES erasure,
#    so `findRErasuresAreEqual` must fail -- which is what shows that theorem
#    tests the erasure rather than holding for any two finders.
plant erasure findRMisattributing \
  'citesTextOrEdges := True' 'citesTextOrEdges := False'

# 5. The faithful finder receipts the wrong pattern: `findRFaithfulContentF2`
#    must fail, so content-F2's inhabitance is about the carried datum.
plant faithful-content findRFaithful \
  'acknowledgedClause := p' 'acknowledgedClause := misattributedReceiptOwner p'

# 6. NON-VACUITY of the recorded witness: `consultTheRemedyBeforeExiting` is not
#    in g1Snatcher's recorded selection (`Holes.lean:628-630`, the first row of
#    `findSnatchScenarios`) -- it is that scenario's declared zero-mass pattern
#    (`futon3:checks/find-snatch.edn`, first scenario's `:f4`) -- so the
#    membership claim must fail.
plant witness findSnatchMisattributingWitness \
  'SnatchPattern.askForSurplusNotSurrender' 'SnatchPattern.consultTheRemedyBeforeExiting'

echo "ALL 6 CONTROLS FAILED AS REQUIRED"
