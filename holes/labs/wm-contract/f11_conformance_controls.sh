#!/usr/bin/env bash
set -u
LAB="$HOME/code/futon2/holes/labs/wm-contract"; CONF="$HOME/code/mathlib4/DarkTower/WarMachine/F11AppliedConformance.lean"
HOLES="$HOME/code/mathlib4/DarkTower/WarMachine/Holes.lean"; LIVE="$HOME/code/futon2/holes/labs/M-f11-find-production-successor/applied-find.edn"
T=$(mktemp -d); trap 'rm -rf "$T"' EXIT; fail=0
run () { local name="$1" expect="$2"; shift 2; local out rc; out=$(env "$@" F11_OUT="$T/out.edn" bb "$LAB/f11_conformance_check.bb" 2>&1); rc=$?;
  if [ "$rc" -eq 0 ]; then echo "CONTROL $name: FAIL -- check passed on a planted defect"; fail=1
  elif grep -q "$expect" <<<"$out"; then echo "CONTROL $name: ok -- rejected with $expect"
  else echo "CONTROL $name: FAIL -- rejected for the wrong reason: $out"; fail=1; fi; }
sed 's/opaque find /def find /' "$HOLES" > "$T/holes.lean"; run signature-drift ':signature-drift' "F11_HOLES=$T/holes.lean"
sed 's/d4b59fe05b98d0a3faa041714e0ea05a9ac11feaa1997efcff1da17db7288733/deadbeef/' "$CONF" > "$T/pin.lean"; run source-pin-drift ':source-pin-drift' "F11_CONF=$T/pin.lean"
sed 's/^theorem modelContainment/def planted : True := by sorry\n\ntheorem modelContainment/' "$CONF" > "$T/sorry.lean"; run planted-sorry ':sorry-or-axiom-in-conformance-file' "F11_CONF=$T/sorry.lean"
sed 's/theorem conformantImplementationExists/theorem removedConformantImplementationExists/' "$CONF" > "$T/missing.lean"; run missing-declaration ':missing-declarations' "F11_CONF=$T/missing.lean"
bb -e '(let [r (clojure.edn/read-string (slurp (first *command-line-args*))) z (get-in r [:scenarios 0 :f4 :zero-mass-pattern])] (spit (second *command-line-args*) (pr-str (update-in r [:scenarios 0 :selected-union] conj z))))' "$LIVE" "$T/live.edn"
run zero-mass-selected ':zero-mass-was-selected' "F11_LIVE=$T/live.edn"
env F11_OUT="$T/a.edn" bb "$LAB/f11_conformance_check.bb" >/dev/null; env F11_OUT="$T/b.edn" bb "$LAB/f11_conformance_check.bb" >/dev/null
if cmp -s "$T/a.edn" "$T/b.edn"; then echo "CONTROL determinism: ok -- $(sha256sum < "$T/a.edn" | cut -c1-12)"; else echo "CONTROL determinism: FAIL -- two runs differ"; fail=1; fi
exit $fail
