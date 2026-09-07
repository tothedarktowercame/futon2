#!/usr/bin/env bash
set -euo pipefail

lab_dir=$(cd "$(dirname "$0")" && pwd)
artifact="$lab_dir/runs/F11-find/14-remainder.edn"

bb "$lab_dir/f11_remainder_check.bb" >/dev/null
bb -e '(let [x (clojure.edn/read-string (slurp (first *command-line-args*)))]
         (assert (>= (count (:controls x)) 6))
         (doseq [c (:controls x)]
           (assert (:plant-verified? c))
           (assert (or (contains? c :after) (contains? c :after-hard-failure))))
         (assert (some #(false? (get-in % [:after :remainder-fully-gated?])) (:controls x)))
         (assert (some #(and (:after-hard-failure %)
                            (re-find #"substring not found" (:after-hard-failure %)))
                       (:controls x))))' "$artifact"
echo "f11 remainder controls: PASS"
