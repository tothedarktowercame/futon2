#!/usr/bin/env bash
# :F11 slice 5 negative controls for the F4-reading measurements.
set -euo pipefail

src=/home/joe/code/mathlib4/DarkTower/WarMachine/F11F4Reading.lean
repo=/home/joe/code/mathlib4
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

locate() {
  local file=$1 decl=$2 needle=$3 start
  start=$(grep -n "^\(noncomputable \)\?\(theorem\|def\) $decl" "$file" |
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
  if ! sed -n "${line}p" "$file" | grep -qF "$new"; then
    echo "$name: planted text absent on line $line" >&2; exit 1
  fi
  if sed -n "${line}p" "$file" | grep -qF "$old"; then
    echo "$name: replaced text remains on line $line" >&2; exit 1
  fi
  if (cd "$repo" && lake env lean "$file") >"$tmp/$name.out" 2>&1; then
    echo "CONTROL $name UNEXPECTED PASS" >&2; exit 1
  fi
  echo "CONTROL $name declaration=$decl line=$line plant=$old -> $new"
  grep -m1 -A4 'error:' "$tmp/$name.out" | sed 's/^/    /'
}

# M1: making the allegedly empty designation universal invalidates the C-not-A witness.
plant m1-reading-c findReadingCDoesNotImplyReadingA \
  '(∅ : Set SnatchPattern)' '(Set.univ : Set SnatchPattern)'

# M2: replaying identity on the recorded repository destroys the proved A/B hybrid.
plant m2-conjunction findReplayRecordedElseRefuse \
  'then findSnatchReplay t repo' 'then findIdentity t repo'

# M3: reverse the recorded declared-zero-mass exclusion.
plant m3-record recordedReadingBCarriesItsDeclaredWitness \
  'p ∉ (findSnatchReplay t findSnatchRepository).selected' \
  'p ∈ (findSnatchReplay t findSnatchRepository).selected'

# M4: identity cannot replace refusal as the nothing finder for arbitrary C.
plant m4-other-laws readingCAdmitsNothingFinder \
  'FindRespectsZeroMass zm (findRefusing' 'FindRespectsZeroMass zm (findIdentity'

# M5: changing C's universal repository quantifier breaks its measured consumers.
plant m5-mechanical FindRespectsZeroMass \
  '∀ t repo, ∀ p' '∃ t repo, ∀ p'

echo 'ALL 5 CONTROLS FAILED AS REQUIRED'
