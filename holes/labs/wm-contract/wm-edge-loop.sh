#!/usr/bin/env bash
# wm-edge-loop.sh -- run a CLI agent over worklist.edn, one item per invocation,
# until no :open non-J item remains. Usage: wm-edge-loop.sh [claude|codex] [sleep-seconds]
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"; SEAT="${1:-claude}"; SLEEP="${2:-30}"
open_count() { bb -e "(require '[clojure.edn :as edn]) (let [w (edn/read-string (slurp \"$HERE/worklist.edn\"))] (println (count (filter #(and (= :open (:status %)) (not= :J (:class %))) (:items w)))))"; }
i=0
while :; do
  n=$(open_count); echo "[wm-edge-loop] open non-J items: $n"
  [ "$n" -eq 0 ] && { echo "[wm-edge-loop] done"; break; }
  i=$((i+1)); echo "[wm-edge-loop] iteration $i ($SEAT)"
  case "$SEAT" in
    claude) (cd "$HOME/code" && claude -p "$(cat "$HERE/worklist-prompt.md")") ;;
    codex)  (cd "$HOME/code" && codex exec "$(cat "$HERE/worklist-prompt.md")") ;;
    *) echo "unknown seat $SEAT"; exit 2 ;;
  esac
  bb "$HERE/worklist_check.bb" "$HERE/worklist.edn" || { echo "[wm-edge-loop] ledger invalid after iteration $i; stopping"; exit 1; }
  sleep "$SLEEP"
done
