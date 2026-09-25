(ns futon2.aif.publication-unreachable-live-test
  "H-PUBLISH-A2 acceptance over the LIVE store: reads
  data/wm-repair-obligations in the shared futon2 checkout. Run it only
  there. Never register it from a pinned worktree: data/ is gitignored, so
  a pinned run has no store and either fails or passes vacuously. The
  registrable form is synthetic-store-catch-up-reports-markers-not-refusals
  in futon2.aif.publication-unreachable-test.

  Read-only against data/: every marked id's derive refuses before any git
  operation, so catch-up! writes nothing here."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.repair-discharge-receipt :as receipt]))

(deftest store-wide-catch-up-reports-markers-not-refusals
  ;; H-PUBLISH-A2 acceptance: after the one-shot write, a full catch-up!
  ;; over the real store reports every one of the 68 H-PUBLISH-D ids as
  ;; :publication-unreachable with its class, and re-refuses none of them.
  ;; Read-only against data/: every marked id's derive refuses before any
  ;; git operation, so catch-up! writes nothing here.
  (let [results (receipt/catch-up! "data/wm-repair-obligations" "/home/joe/code/futon2")
        by-status (group-by :status results)
        by-class (frequencies (map :class (:publication-unreachable by-status)))]
    (is (= 68 (count results)))
    (is (= 68 (count (:publication-unreachable by-status))))
    (is (nil? (:publication-refused by-status))
        "no re-refusals of the marked ids")
    (is (= {:legacy-a 44 :late-script-b 9 :no-implementation-c 15} by-class))
    (is (every? #(= "H-PUBLISH-D 5cbf0031" (:ground %))
                (:publication-unreachable by-status)))))
