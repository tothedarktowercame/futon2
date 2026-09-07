#!/usr/bin/env bash
set -euo pipefail

src=/home/joe/code/mathlib4/DarkTower/WarMachine/F10RuledCarrier.lean
repo=/home/joe/code/mathlib4
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

(cd "$repo" && lake env lean "$src") >"$tmp/base.out" 2>&1

plant() {
  local name=$1 old=$2 new=$3 file="$tmp/$1.lean"
  cp "$src" "$file"
  if ! grep -qF -- "$old" "$file"; then echo "$name: target absent" >&2; exit 1; fi
  python3 - "$file" "$old" "$new" <<'PY'
import sys
p, old, new = sys.argv[1:]
s = open(p).read()
open(p, "w").write(s.replace(old, new, 1))
PY
  if ! grep -qF -- "$new" "$file"; then echo "$name: plant did not land" >&2; exit 1; fi
  if (cd "$repo" && lake env lean "$file") >"$tmp/$name.out" 2>&1; then
    echo "CONTROL $name UNEXPECTED PASS" >&2; exit 1
  fi
  echo "CONTROL $name REJECTED"
  grep -m1 -A2 'error:' "$tmp/$name.out" | sed 's/^/    /'
}

plant thirteenth-constructor \
  '| buildFailed | substrateUnavailable | incomplete | cancelled' \
  '| buildFailed | substrateUnavailable | incomplete | cancelled | planted'

plant zero-positive-without-renormalising \
  '| ⟨.organisations, .groundedNoChange⟩ => 0' \
  '| ⟨.organisations, .groundedNoChange⟩ => 1 / 8'

plant positivity-includes-named-zero \
  '.groundedChange, .incomplete, .noSelection]' \
  '.groundedChange, .groundedNoChange, .incomplete, .noSelection]'

echo 'ALL 3 CONTROLS REJECTED AS REQUIRED'
