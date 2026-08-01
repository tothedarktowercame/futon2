(ns futon2.aif.operational-witness-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.operational-witness :as witness]))

(defn- run-record
  [yield]
  {:food-seed 11 :move-seed 12 :choice-seed 13 :yield yield})

(deftest reproduction-relation-catches-one-cell-mismatch
  (testing "mandatory positive control: the checker demonstrably fails"
    (let [committed {:cells [{:scenario :patchy :arm :a0
                              :runs [(run-record 4.0)]}]}
          rerun (assoc-in committed [:cells 0 :runs 0 :yield] 5.0)
          report (witness/verify-artifact-reproduction committed rerun)]
      (is (false? (:verified? report)))
      (is (= 1 (:committed-row-count report)))
      (is (= 1 (:rerun-row-count report)))
      (is (= [{:key [:patchy :a0
                     {:food-seed 11 :move-seed 12 :choice-seed 13}]
               :kind :different-run-record
               :committed (run-record 4.0)
               :rerun (run-record 5.0)}]
             (:mismatches report))))))

(deftest committed-pilot-artifact-row-counts-reconcile
  (doseq [[path expected]
          [["holes/labs/ants-faithfulness/slice5.edn" 540]
           ["holes/labs/ants-faithfulness/authority.edn" 360]]]
    (let [artifact (edn/read-string (slurp path))
          ;; A re-run need not emit cells or runs in the committed order. Reverse
          ;; both to ensure reconciliation is by the full identity, not position.
          rerun (update artifact :cells
                        (fn [cells]
                          (mapv #(update % :runs (comp vec reverse))
                                (reverse cells))))
          report (witness/verify-artifact-reproduction artifact rerun)]
      (is (:verified? report) path)
      (is (= expected (:committed-row-count report)) path)
      (is (= expected (:rerun-row-count report)) path)
      (is (= expected (:matched-row-count report)) path))))
