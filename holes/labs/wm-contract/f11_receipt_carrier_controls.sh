#!/usr/bin/env bash
set -euo pipefail
src=/home/joe/code/mathlib4/DarkTower/WarMachine/F11ReceiptCarrier.lean
repo=/home/joe/code/mathlib4
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

run() {
  local name=$1 planted=$2 absent=$3
  grep -nF "$planted" "$tmp/$name.lean"
  if grep -qF "$absent" "$tmp/$name.lean"; then echo "replaced text remains: $absent"; exit 1; fi
  if (cd "$repo" && lake env lean "$tmp/$name.lean") >"$tmp/$name.out" 2>&1; then
    echo "CONTROL $name UNEXPECTED PASS"; exit 1
  fi
  echo "CONTROL $name"; grep -A8 'error:' "$tmp/$name.out"
}

cp "$src" "$tmp/owner.lean"
sed -i '15s/\.consultTheRemedyBeforeExiting/\.askForSurplusNotSurrender/' "$tmp/owner.lean"
sed -n '14,16p' "$tmp/owner.lean"
if sed -n '15p' "$tmp/owner.lean" | grep -qF '.consultTheRemedyBeforeExiting'; then exit 1; fi
if (cd "$repo" && lake env lean "$tmp/owner.lean") >"$tmp/owner.out" 2>&1; then exit 1; fi
echo 'CONTROL owner'; grep -A8 'error:' "$tmp/owner.out"

cp "$src" "$tmp/assertion.lean"
sed -i '0,/hasAsOf := True/s//hasAsOf := False/' "$tmp/assertion.lean"
run assertion 'hasAsOf := False' 'hasAsOf := True'

cp "$src" "$tmp/relation.lean"
sed -i '0,/acknowledgedClause := .askForSurplusNotSurrender/s//acknowledgedClause := .consultTheRemedyBeforeExiting/' "$tmp/relation.lean"
run relation 'acknowledgedClause := .consultTheRemedyBeforeExiting' 'acknowledgedClause := .askForSurplusNotSurrender'
