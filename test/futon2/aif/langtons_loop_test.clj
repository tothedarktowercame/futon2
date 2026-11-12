(ns futon2.aif.langtons-loop-test
  "T-ladder-02-langton-loop acceptance: each claim of the ticket's single
  acceptance entry is measured here against the pinned inputs, and the
  entry's built-in falsifier — 'A test rejects a deliberately altered
  transition' — is exercised by altering one rotate4 rule image family and
  requiring the simulation to diverge.  A passing suite therefore rules out
  wrong implementations of the table parse, the seed parse, the rotation
  expansion and the canonical comparison, not merely a value replay."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.langtons-loop :as ll]))

(def table (ll/parse-table (slurp ll/table-path)))
(def seed (ll/parse-rle (slurp ll/rle-path)))
(def placed (ll/place-seed seed ll/grid-size ll/origin))

(deftest inputs-match-provenance-pins
  (testing "both ticket inputs match PROVENANCE.md before any measurement"
    (is (map? (ll/verify-inputs!)))))

(deftest table-has-219-active-rows
  (testing "the table recount receipts exactly 219 active rows"
    (is (= 219 (:active-rows table)))))

(deftest seed-has-86-non-quiescent-cells-under-declared-threshold
  (testing "the 86-cell count is reported with its quiescence threshold"
    (is (= 86 (ll/non-quiescent-cells seed #{0})))
    (is (= 25 (ll/non-quiescent-cells seed #{0 2})))))

(deftest test-rejects-a-deliberately-altered-transition
  (testing "altering rule 000012's output diverges the evolution"
    (let [altered (ll/altered-table table)]
      (is (= 7 (get-in altered [:rules [0 0 0 0 1]])))
      (is (pos? (ll/first-divergence (:rules altered) (:rules table)
                                     placed 151))))))

(deftest daughter-equal-to-parent-modulo-symmetry-at-generation-151
  (testing "replication judged on canonical representatives with digests"
    (let [gen151 (ll/evolve (:rules table) placed 151)
          pattern (ll/crop seed)
          copies (ll/find-canonical-copies gen151 pattern)
          seed-digest (ll/canonical-digest placed)]
      (is (= 1 (count copies)) "exactly one complete daughter at 151")
      (is (every? #(= seed-digest %)
                  (ll/replication-digests gen151 copies pattern))))))
