#!/usr/bin/env bash
# f11_receipt_carrier_controls_checker.sh -- `:F11` slice 4.
#
# Controls against f11_receipt_carrier_check.bb itself.  The check is what keeps
# the `:find-f2-receipt-carrier` arms from going stale, so it has to be shown to
# FAIL when each premise it guards is broken -- and to fail on THAT check and
# not merely somewhere.  Five plants; each must move its own verdict and no
# other.
set -euo pipefail
lab=/home/joe/code/futon2/holes/labs/wm-contract
checker=$lab/f11_receipt_carrier_check.bb
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

# run NAME EXPECTED-CHECK -- env already set by the caller
run() {
  local name=$1 expected=$2 out
  out=$tmp/$name.txt
  if bb "$checker" >"$out" 2>&1; then
    echo "CONTROL $name UNEXPECTED PASS -- the checker did not notice" >&2; exit 1; fi
  if ! grep -q "$expected" "$out"; then
    echo "CONTROL $name failed on the WRONG check:" >&2; cat "$out" >&2; exit 1; fi
  local others
  others=$(sed 's/[][]//g;s/^FAIL //' "$out" | tr ' ' '\n' | grep -c ':' || true)
  echo "CONTROL $name -> $(cat "$out")  [checks failing: $others]"
}

export F11R_OUT=$tmp/out.edn

# 1. Receipt gains a field: every arm is priced against the two-field carrier.
cp /home/joe/code/mathlib4/DarkTower/WarMachine/Holes.lean "$tmp/holes-field.lean"
python3 - "$tmp/holes-field.lean" <<'PY'
import sys
p=sys.argv[1]; ls=open(p).readlines()
i=next(n for n,l in enumerate(ls) if l.startswith('structure Receipt where'))
ls.insert(i+3, '  acknowledgedClause : Prop\n')
open(p,'w').writelines(ls)
PY
grep -q 'acknowledgedClause : Prop' "$tmp/holes-field.lean"
F11R_HOLES=$tmp/holes-field.lean run receipt-gains-a-field ':receipt-field-list-changed'

# 2. The transcription starts carrying a warrant: the data arm's transcription
#    cost is priced on FindSnatchRowLit holding none.
cp /home/joe/code/mathlib4/DarkTower/WarMachine/Holes.lean "$tmp/holes-warrant.lean"
python3 - "$tmp/holes-warrant.lean" <<'PY'
import sys
p=sys.argv[1]; ls=open(p).readlines()
i=next(n for n,l in enumerate(ls) if l.startswith('structure FindSnatchRowLit where'))
ls.insert(i+1, '  warrantText : String\n')
open(p,'w').writelines(ls)
PY
grep -q 'warrantText : String' "$tmp/holes-warrant.lean"
F11R_HOLES=$tmp/holes-warrant.lean run transcription-carries-a-warrant ':transcription-now-carries-warrants'

# 3. The record grows a per-receipt as-of: the entry prices the as-of as
#    available once per fixture only.
python3 - /home/joe/code/futon3/checks/find-snatch.edn "$tmp/fixture-asof.edn" <<'PY'
import sys, re
src, dst = sys.argv[1], sys.argv[2]
s = open(src).read()
s = s.replace(':route :structured-antecedent', ':as-of "planted", :route :structured-antecedent', 1)
open(dst,'w').write(s)
PY
grep -q ':as-of "planted"' "$tmp/fixture-asof.edn"
F11R_FIXTURE=$tmp/fixture-asof.edn run record-grows-a-per-receipt-as-of ':record-now-carries-a-per-receipt-as-of'

# 4. A warrant key disappears: the data arm is priced on transcribing exactly
#    the five the record carries.
python3 - /home/joe/code/futon3/checks/find-snatch.edn "$tmp/fixture-warrant.edn" <<'PY'
import sys
src, dst = sys.argv[1], sys.argv[2]
s = open(src).read().replace(':however-lines', ':however-lines-renamed')
open(dst,'w').write(s)
PY
grep -q ':however-lines-renamed' "$tmp/fixture-warrant.edn"
F11R_FIXTURE=$tmp/fixture-warrant.edn run warrant-key-renamed ':warrant-shape-changed'

# 5. A declaration the entry cites by name is renamed: EXACT names, because a
#    prefix test passes when the shorter name goes and a longer neighbour stays.
sed 's/^theorem findRErasuresAreEqual/theorem findRErasuresAreEqualRenamed/' \
  /home/joe/code/mathlib4/DarkTower/WarMachine/F11ReceiptCarrier.lean > "$tmp/arm-renamed.lean"
grep -q 'findRErasuresAreEqualRenamed' "$tmp/arm-renamed.lean"
if grep -q '^theorem findRErasuresAreEqual$' "$tmp/arm-renamed.lean"; then
  echo 'replaced declaration name remains' >&2; exit 1; fi
F11R_ARM=$tmp/arm-renamed.lean run required-declaration-renamed ':required-declaration-missing'

echo "ALL 5 CHECKER CONTROLS FAILED AS REQUIRED"
