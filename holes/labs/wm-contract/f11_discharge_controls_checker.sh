#!/usr/bin/env bash
# f11_discharge_controls_checker.sh -- `:F11` slice 3.  Five plants against
# `f11_discharge_check.bb` itself, each of which must MOVE ITS VERDICT.  A
# checker that passes on a planted tree is a checker that reads nothing.
#
# Every plant is made in a COPY of DarkTower; the real tree is never touched.
# The checker is pointed at the copy through F11D_DARK/F11D_ARM/F11D_HOLES and
# writes its record to a scratch F11D_OUT, so the committed run record is not
# overwritten by a control.
set -uo pipefail

lab=/home/joe/code/futon2/holes/labs/wm-contract
orig=/home/joe/code/mathlib4/DarkTower
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

fresh() {                     # fresh NAME -> echoes the planted tree's root
  local name=$1
  cp -r "$orig" "$tmp/$name"
  echo "$tmp/$name"
}

verdict() {                   # verdict NAME ROOT
  local name=$1 root=$2
  F11D_DARK="$root" \
  F11D_ARM="$root/WarMachine/F11DischargeArm.lean" \
  F11D_HOLES="$root/WarMachine/Holes.lean" \
  F11D_OUT="$tmp/$name.edn" \
    bb "$lab/f11_discharge_check.bb" >"$tmp/$name.log" 2>&1
  local rc=$?
  echo "-- exit $rc"
  head -2 "$tmp/$name.log"
  if [ $rc -eq 0 ]; then echo "UNEXPECTED PASS -- the plant moved nothing"; return 1; fi
  return 0
}

fails=0

echo "== PLANT 1: the sorry at Holes.lean:264 discharged"
r=$(fresh discharged)
sed -i 's|^def find {State P : Type\*} : Tension State → Repository P → FindResult P := sorry$|def find {State P : Type*} : Tension State → Repository P → FindResult P := fun _ _ => { selected := ∅, receipts := fun _ => none, absence := none }|' "$r/WarMachine/Holes.lean"
grep -c ':= sorry$' "$r/WarMachine/Holes.lean" >/dev/null
grep -q 'def find {State P : Type\*}.*:= fun' "$r/WarMachine/Holes.lean" || { echo "PLANT NOT ESTABLISHED"; fails=1; }
verdict discharged "$r" || fails=1
echo

echo "== PLANT 2: a term reference to \`find\` added, so it is no longer inert"
r=$(fresh notinert)
printf '\n/-- planted -/\nexample : True := by have _ := @find; trivial\n' >> "$r/WarMachine/F11DischargeArm.lean"
grep -q 'have _ := @find' "$r/WarMachine/F11DischargeArm.lean" || { echo "PLANT NOT ESTABLISHED"; fails=1; }
verdict notinert "$r" || fails=1
echo

echo "== PLANT 3: the hole registry's mkRefused \"find\" literal removed"
r=$(fresh noregistry)
sed -i '/mkRefused "find"/d' "$r/WarMachine/Holes.lean"
[ "$(grep -cF 'mkRefused "find"' "$r/WarMachine/Holes.lean")" -eq 0 ] || { echo "PLANT NOT ESTABLISHED"; fails=1; }
verdict noregistry "$r" || fails=1
echo

echo "== PLANT 4: the bodiless opaque removed, so \"opaque needs no body\" would read as false again"
r=$(fresh onebody)
sed -i '/^opaque findOpaqueNoBody : FindType Unit SnatchPattern$/d' "$r/WarMachine/F11DischargeArm.lean"
[ "$(grep -c '^opaque ' "$r/WarMachine/F11DischargeArm.lean")" -eq 1 ] || { echo "PLANT NOT ESTABLISHED"; fails=1; }
verdict onebody "$r" || fails=1
echo

echo "== PLANT 5: the prefix trap -- \`findAllBut\` renamed while \`findAllButConformant\` remains"
r=$(fresh prefix)
sed -i 's/^def findAllBut {State P : Type\*} (q : P)/def findAllButRenamed {State P : Type*} (q : P)/' "$r/WarMachine/F11DischargeArm.lean"
grep -q '^def findAllButRenamed' "$r/WarMachine/F11DischargeArm.lean" || { echo "PLANT NOT ESTABLISHED"; fails=1; }
grep -q '^theorem findAllButConformant' "$r/WarMachine/F11DischargeArm.lean" || { echo "PREFIX SIBLING GONE"; fails=1; }
verdict prefix "$r" || fails=1
echo

if [ $fails -eq 0 ]; then echo "ALL 5 PLANTS MOVED THE CHECKER'S VERDICT"; else echo "SOME PLANTS DID NOT"; exit 1; fi
