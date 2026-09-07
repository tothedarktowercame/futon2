#!/usr/bin/env bash
# Negative controls for f11_conformance_check.bb.  Each plants ONE defect the
# check claims to catch and requires a nonzero exit naming it.
set -u
LAB="$HOME/code/futon2/holes/labs/wm-contract"
CONF="$HOME/code/mathlib4/DarkTower/WarMachine/F11Conformance.lean"
PIN="$HOME/code/futon3/checks/find-snatch.edn"
T=$(mktemp -d); trap 'rm -rf "$T"' EXIT
fail=0

run () { # name expected-problem env...
  local name="$1" expect="$2"; shift 2
  local out
  out=$(env "$@" F11_OUT="$T/out.edn" bb "$LAB/f11_conformance_check.bb" 2>&1)
  if [ $? -eq 0 ]; then
    echo "CONTROL $name: FAIL -- check passed on a planted defect"; fail=1
  elif echo "$out" | grep -q "$expect"; then
    echo "CONTROL $name: ok -- rejected with $expect"
  else
    echo "CONTROL $name: FAIL -- rejected for the wrong reason: $out"; fail=1
  fi
}

# 1. signature drift
sed 's/abbrev FindType (State P : Type\*) :=.*/abbrev FindType (State P : Type*) := Tension State → Repository P → Option (FindResult P)/' "$CONF" > "$T/drift.lean"
run signature-drift ":signature-drift" "F11_CONF=$T/drift.lean"

# 2. a pattern name the record does not carry
sed 's/SnatchPattern\.askForSurplusNotSurrender/SnatchPattern.askForEverythingAtOnce/g' "$CONF" > "$T/invented.lean"
run invented-pattern ":pattern-name-not-in-the-record" "F11_CONF=$T/invented.lean"

# 3. a planted sorry in code
sed 's/^  cases scenario <;> simp \[findSnatchZeroMass\]$/  sorry/' "$CONF" > "$T/sorry.lean"
run planted-sorry ":sorry-or-axiom-in-conformance-file" "F11_CONF=$T/sorry.lean"

# 4. a required declaration removed
grep -v "theorem findSnatchReplayNotFalsifiable" "$CONF" > "$T/missing.lean"
run missing-declaration ":missing-declarations" "F11_CONF=$T/missing.lean"

# 5. the record mutated so a declared zero-mass pattern IS selected
bb -e '(let [r (clojure.edn/read-string (slurp (first *command-line-args*)))
             s (update r :scenarios
                       (fn [ss] (vec (cons (update (first ss) :selected-union
                                                   conj :consult-the-remedy-before-exiting)
                                           (rest ss)))))]
         (spit (second *command-line-args*) (pr-str s)))' "$PIN" "$T/pin.edn"
run zero-mass-selected ":zero-mass-was-selected" "F11_PIN=$T/pin.edn"

# 6. determinism: two runs over the unchanged tree are byte-identical
env F11_OUT="$T/a.edn" bb "$LAB/f11_conformance_check.bb" >/dev/null
env F11_OUT="$T/b.edn" bb "$LAB/f11_conformance_check.bb" >/dev/null
if cmp -s "$T/a.edn" "$T/b.edn"; then
  echo "CONTROL determinism: ok -- $(sha256sum < "$T/a.edn" | cut -c1-12)"
else
  echo "CONTROL determinism: FAIL -- two runs differ"; fail=1
fi

exit $fail
