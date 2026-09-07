#!/usr/bin/env bash
# :F11 slice 7 negative controls AGAINST THE CHECKER.  Each plant is made in a
# COPY of a file the checker reads, pointed at by the checker's own env var, and
# each must move EXACTLY ONE of the checker's per-check verdicts.
set -euo pipefail
lab=/home/joe/code/futon2/holes/labs/wm-contract
tmp=$(mktemp -d); trap 'rm -rf "$tmp"' EXIT

base=$(cd "$lab" && bb f11_non_self_certifying_check.bb || true)

verdicts() { echo "$1" | sed -n '/:per-check/,/}/p' | grep -oP ':[a-z-]+ \d+' | sort; }

control() {
  local name=$1 var=$2 src=$3 old=$4 new=$5 file out moved
  file="$tmp/$name.$(basename "$src")"
  cp "$src" "$file"
  if ! grep -qF -- "$old" "$file"; then echo "$name: target absent in $src" >&2; exit 1; fi
  python3 - "$file" "$old" "$new" <<'PY'
import sys
path, old, new = sys.argv[1], sys.argv[2], sys.argv[3]
s = open(path).read()
open(path, "w").write(s.replace(old, new, 1))
PY
  if ! grep -qF -- "$new" "$file"; then echo "$name: planted text absent" >&2; exit 1; fi
  out=$(cd "$lab" && env "$var=$file" bb f11_non_self_certifying_check.bb || true)
  moved=$(diff <(verdicts "$base") <(verdicts "$out") | grep -c '^>' || true)
  if [ "$moved" != "1" ]; then
    echo "CHECKER CONTROL $name MOVED $moved VERDICTS (expected 1)" >&2
    diff <(verdicts "$base") <(verdicts "$out") >&2 || true
    exit 1
  fi
  echo "CHECKER CONTROL $name var=$var moved=1 finding=$(echo "$out" | grep -oP '\[:[a-z-]+' | head -1 | tr -d '[')"
}

# 1. Receipt grows a citation field: the blindness result would be describing a
#    carrier that no longer exists.
control receipt-gains-citation F11NSC_HOLES \
  /home/joe/code/mathlib4/DarkTower/WarMachine/Holes.lean \
  '  citesTextOrEdges : Prop' \
  '  citesTextOrEdges : Prop
  citedText : Nat'

# 2. The row literal grows a warrant column: the 34 rounds could then decide the
#    two readings, and the REVIEW FINDING would be false.
control row-gains-warrant F11NSC_HOLES \
  /home/joe/code/mathlib4/DarkTower/WarMachine/Holes.lean \
  '  nonSelfCertifying : List SnatchPattern
  absence : Option TypedAbsence' \
  '  nonSelfCertifying : List SnatchPattern
  warrant : Option String
  absence : Option TypedAbsence'

# 3. RelationalReceipt grows a citation: slice 4's finders would be the right F3
#    witnesses and this slice's carrier redundant.
control relational-gains-citation F11NSC_RECEIPT \
  /home/joe/code/mathlib4/DarkTower/WarMachine/F11ReceiptCarrier.lean \
  '  acknowledgedClause : Clause' \
  '  acknowledgedClause : Clause
  citedText : Clause'

# 4. find_organise's predicate loses its two extra conjuncts: the two readings
#    converge and M3's non-equivalence stops being about the code.
control organise-converges F11NSC_ORGANISE \
  /home/joe/code/futon3/checks/find_organise.clj \
  '       (vector? (get-in receipt [:warrant :if-lines]))
       (string? (get-in receipt [:warrant :if-text]))))' \
  '       ))'

# 5. The transcriber switches to find_organise's reading: the Lean column would
#    no longer be the check's predicate.
control transcriber-switches F11NSC_TRANSCRIBE \
  /home/joe/code/futon2/holes/labs/wm-contract/u46_find_transcribe.bb \
  '                           (string? (get-in receipt [:warrant :file])))' \
  '                           (string? (get-in receipt [:warrant :file]))
                           (vector? (get-in receipt [:warrant :if-lines])))'

# 6. A receipt cites another pattern's file: the citation-ownership fact the
#    misciting finder departs from would already be false on the record.
control fixture-miscites F11NSC_FIXTURE \
  /home/joe/code/futon3/checks/find-snatch.edn \
  '"library/snatch/ask-for-surplus-not-surrender.flexiarg"' \
  '"library/snatch/consult-the-remedy-before-exiting.flexiarg"'

echo 'ALL 6 CHECKER CONTROLS MOVED EXACTLY ONE VERDICT'
