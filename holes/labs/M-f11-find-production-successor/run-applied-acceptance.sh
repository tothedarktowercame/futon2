#!/usr/bin/env bash
set -euo pipefail

futon2_root="$(cd "$(dirname "$0")/../../.." && pwd)"
futon3_root=/home/joe/code/futon3
out="$futon2_root/holes/labs/M-f11-find-production-successor"
scratch="$(mktemp -d)"
trap 'rm -rf "$scratch"' EXIT

# find-snatch has fixed relative input/output paths.  Give it an isolated
# layout so the applied run reads futon3's committed library but cannot rewrite
# futon3's pinned fixture.
cp -a "$futon3_root/checks" "$scratch/checks"
ln -s "$futon3_root/library" "$scratch/library"
(
  cd "$scratch"
  GIT_DIR="$futon3_root/.git" GIT_WORK_TREE="$futon3_root" \
    bb -cp "$scratch/checks" -m find-snatch \
    > "$out/applied-find.stdout" 2> "$out/applied-find.stderr"
)
cp "$scratch/checks/find-snatch.edn" "$out/applied-find.edn"

bb -e '
  (require (quote [clojure.edn :as edn])
           (quote [clojure.java.io :as io])
           (quote [clojure.pprint :as pprint]))
  (import (quote [java.security MessageDigest]))
  (defn digest [p]
    (let [md (MessageDigest/getInstance "SHA-256")]
      (with-open [in (io/input-stream p)] (.update md (.readAllBytes in)))
      (apply str (map #(format "%02x" (bit-and 255 %)) (.digest md)))))
  (let [[out repo] *command-line-args*
        result (edn/read-string (slurp (str out "/applied-find.edn")))
        receipt {:schema :wm/f11-applied-acceptance-v1
                 :mode :runtime-validation
                 :interface :find-snatch/-main
                 :repository {:path (str repo "/library/snatch")
                              :commit (:as-of result)
                              :pattern-count (count (:repository result))}
                 :observation {:scenario-count (count (:scenarios result))
                               :round-count (reduce + (map #(count (:round-results %))
                                                          (:scenarios result)))
                               :f4-passes (count (filter #(true? (get-in % [:f4 :holds]))
                                                        (:scenarios result)))
                               :drift-mismatches (get-in result [:drift :mismatch-count])}
                 :artifacts {:result-sha256 (digest (str out "/applied-find.edn"))
                             :stdout-sha256 (digest (str out "/applied-find.stdout"))
                             :stderr-sha256 (digest (str out "/applied-find.stderr"))}}
        measured (merge (select-keys (:repository receipt) [:pattern-count])
                        (select-keys (:observation receipt)
                                     [:scenario-count :f4-passes :drift-mismatches]))
        receipt (assoc receipt :status
                       (if (= {:pattern-count 24 :scenario-count 6 :f4-passes 6
                               :drift-mismatches 0} measured)
                         :passed :failed))]
    (when-not (= :passed (:status receipt)) (System/exit 1))
    (spit (str out "/acceptance.edn")
          (with-out-str (pprint/pprint receipt))))' "$out" "$futon3_root"
