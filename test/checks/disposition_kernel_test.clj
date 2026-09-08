(ns checks.disposition-kernel-test
  (:require [checks.disposition-kernel :as kernel]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-cohort :as cohort]))

(def pinned-ledger
  "One live pin for this test, captured from the closed cohort ledger. A ledger
  change must be inspected and deliberately re-pinned; the test never learns a
  new empirical conditional silently."
  {:path "holes/labs/M-aif-full-loop-46/ledger.edn"
   :sha256 "1f195e907fe9ab77b576a9973a058ada6e9c61510ef70309a8803744b13f8044"})

(deftest t4-kernel-from-records
  (testing "the pinned ledger reproduces the hand-counted 3/3 conditional"
    (is (= (:sha256 pinned-ledger) (kernel/sha256 (:path pinned-ledger))))
    (let [fitted (kernel/read-kernel (:path pinned-ledger))
          state (first (:states fitted))
          expected-trajectory
          [:time-step :selection :construction :dispatch :build :adjudication :closed]]
      (is (= cohort/outcome-kinds (set (:support fitted))))
      (is (= 12 (count (:support fitted))))
      (is (= 3 (:sample-size fitted)))
      (is (= 1 (count (:states fitted))))
      (is (= {:checkpoint-trajectory expected-trajectory}
             (:observation-summary state)))
      (is (= 3 (:sample-size state)))
      (is (= 3 (get-in state [:counts :grounded-change])))
      (is (= 1 (get-in state [:probability :grounded-change])))
      (is (= 11 (count (:unsupported-outcomes fitted))))
      (is (every? zero? (map (:counts state) (:unsupported-outcomes fitted))))
      (is (every? zero? (map (:probability state) (:unsupported-outcomes fitted)))))))
