#!/usr/bin/env bash
# :F11 slice 8 negative controls AGAINST THE CHECKER (reviewer half).
#
# Each plant is made in a COPY of a file `f11_remainder_check.bb` reads, pointed
# at by the checker's own env var, and each must move EXACTLY ONE of the
# checker's obligation verdicts.  This is the independent counterpart to
# `f11_remainder_controls.sh`, which asserts over the controls the checker
# reports about ITSELF: these plants are made from outside it.
set -euo pipefail
lab=/home/joe/code/futon2/holes/labs/wm-contract
mathlib=/home/joe/code/mathlib4/DarkTower/WarMachine
tmp=$(mktemp -d); trap 'rm -rf "$tmp"' EXIT

# The verdict set: one line per obligation's measured satisfaction, plus the F4
# reading count (the quantity F4's satisfaction is decided from) and the
# top-level gating verdict.  Adjudications are DERIVED from these and are
# deliberately not counted twice.
verdicts() {
  bb -e '
    (let [x (clojure.edn/read-string (slurp (first *command-line-args*)))
          obs (dissoc (:obligations x) :candidate-refuters)]
      (doseq [[k v] (sort-by key obs)]
        (println (str (name k) "=" (:satisfied-at-head? v))))
      (println (str "f4-readings-present-count="
                    (get-in obs [:f4-stated-in-lean :readings-present-count])))
      (println (str "fully-gated=" (:remainder-fully-gated? x))))' "$1"
}

run_checker() {  # run_checker <out> [ENV=VALUE ...]
  local out=$1; shift
  (cd "$lab" && env "F11_OUT=$out" "$@" bb f11_remainder_check.bb >/dev/null)
}

run_checker "$tmp/base.edn"
verdicts "$tmp/base.edn" > "$tmp/base.verdicts"

control() {  # control <name> <ENVVAR> <src> [<old> <new>]...
  local name=$1 var=$2 src=$3; shift 3
  local file="$tmp/$name.$(basename "$src")" out="$tmp/$name.edn" moved
  cp "$src" "$file"
  while [ $# -gt 0 ]; do
    local old=$1 new=$2; shift 2
    grep -qF -- "$old" "$file" || { echo "$name: target text absent in $src: $old" >&2; exit 1; }
    python3 - "$file" "$old" "$new" <<'PY'
import sys
path, old, new = sys.argv[1], sys.argv[2], sys.argv[3]
s = open(path).read()
open(path, "w").write(s.replace(old, new, 1))
PY
    grep -qF -- "$new" "$file" || { echo "$name: planted text absent" >&2; exit 1; }
    # An ADDITIVE plant keeps the old text inside the new; only a REPLACING plant
    # must leave no trace of what it replaced.
    case "$new" in
      *"$old"*) : ;;
      *) grep -qF -- "$old" "$file" && { echo "$name: replaced text still present" >&2; exit 1; } ;;
    esac
  done
  run_checker "$out" "$var=$file"
  verdicts "$out" > "$tmp/$name.verdicts"
  moved=$(diff "$tmp/base.verdicts" "$tmp/$name.verdicts" | grep -c '^>' || true)
  if [ "$moved" != "1" ]; then
    echo "CHECKER CONTROL $name MOVED $moved VERDICTS (expected 1)" >&2
    diff "$tmp/base.verdicts" "$tmp/$name.verdicts" >&2 || true
    exit 1
  fi
  echo "CHECKER CONTROL $name var=$var moved=1 $(diff "$tmp/base.verdicts" "$tmp/$name.verdicts" | grep '^>' | sed 's/^> //')"
}

# 1. `Receipt` grows a citation DATUM: F3's citation ask becomes statable and the
#    obligation the review made measurable must move.
control receipt-gains-citation F11_HOLES "$mathlib/Holes.lean" \
  '  citesTextOrEdges : Prop' \
  '  citesTextOrEdges : Prop
  citedPatternText : String'

# 2. `Receipt` grows F2's three asks as DATA (not as `Prop`, which slice 4
#    measured to carry no WHICH): only the F2 content obligation may move.
control receipt-gains-f2-data F11_HOLES "$mathlib/Holes.lean" \
  '  scoreAlone : Prop' \
  '  scoreAlone : Prop
  acknowledgedClause : Clause
  retrievalRoute : Route
  asOf : Nat'

# 3. One of the three F4 readings disappears: the count the F4 obligation is
#    decided from must move, and nothing else.
control reading-b-removed F11_DISCHARGE "$mathlib/F11DischargeArm.lean" \
  'def FindExcludesRecordedZeroMass' \
  'def ControlRemovedReadingB'

# 4. The conformant implementation loses its witness.
control replay-removed F11_CONFORMANCE "$mathlib/F11Conformance.lean" \
  'def findSnatchReplay ' \
  'def controlRemovedReplay '

# 5. The sorry is discharged: only the sorry obligation may move (it is gated, so
#    the verdict vector must NOT move with it).
control find-body-discharged F11_HOLES "$mathlib/Holes.lean" \
  'def find {State P : Type*} : Tension State → Repository P → FindResult P := sorry' \
  'def find {State P : Type*} : Tension State → Repository P → FindResult P := findSnatchReplay'

# 6. The pin catches up with the live library: the ungated library obligation
#    moves, which is the one that carries half the slice's result.
control library-pin-matches F11_RECONCILIATION "$lab/runs/F11-find/02-reconciliation.edn" \
  ':repository-count-live 24' ':repository-count-live 18' \
  ':receipts-differing 96' ':receipts-differing 0'

# 7. The slice-1 reconciliation reports a difference that is NOT line
#    coordinates alone: the F2 falsifier obligation must move.
control reconciliation-not-line-only F11_RECONCILIATION "$lab/runs/F11-find/02-reconciliation.edn" \
  ':difference-is-line-coordinates-only? true' \
  ':difference-is-line-coordinates-only? false'

echo "f11 remainder CHECKER controls: PASS (7 plants, each moved exactly 1 verdict)"
