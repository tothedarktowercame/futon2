#!/usr/bin/env bash
# wm_load_identity.sh -- read-only load-identity readout for the WM click path.
#
# PROOF-wm-works <1>1: lists, for every namespace registered with (or required
# by) futon2.aif.load-identity, the captured source digest vs the canonical
# file: :current (equal) / :stale (differ) / :unregistered / :unavailable.
# Unknowns are listed, never omitted. READ-ONLY: it reads the serving JVM's
# registry through futon3c/scripts/proof-eval.sh and reloads NOTHING. The form
# RETURNS its value (proof-eval does not relay printed output).
#
# Usage: scripts/wm_load_identity.sh   (run from anywhere; proof-eval.sh must
# be invoked from its own directory, which this script handles).
set -uo pipefail

F3C="$HOME/code/futon3c"

cat > /tmp/wm_load_identity.clj <<'CLJ'
(do (require 'futon2.aif.load-identity)
    (let [r ((resolve 'futon2.aif.load-identity/report))
            lines (sort-by (fn [[n m]] [(:status m) (str n)]) r)]
      (clojure.string/join
       "\n"
       (concat
        (for [[n {:keys [status canonical-path]}] lines]
          (format "%-44s %-14s %s" (str n) (name status) (or canonical-path "-")))
        ["" (str "counts: "
                 (into (sorted-map)
                       (frequencies (map (comp name :status val) r))))]))))
CLJ

cat > /tmp/wm_load_identity_render.py <<'PY'
import sys, json, re

raw = sys.stdin.read()
m = re.search(r':value\s*"((?:[^"\\]|\\.)*)"', raw)
if m:
    print(json.loads('"' + m.group(1) + '"'))
else:
    sys.stdout.write(raw)
PY

( cd "$F3C" && ./scripts/proof-eval.sh -f /tmp/wm_load_identity.clj 2>&1 \
    | python3 /tmp/wm_load_identity_render.py )
