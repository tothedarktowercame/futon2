(ns ants.aif.experiment-test
  "Slice 5 tests: seed reproducibility (R4 golden) + experiment sanity."
  (:require [clojure.test :refer [deftest is testing]]
            [ants.aif.experiment :as exp]
            [ants.war :as war]))

(deftest seed-reproducibility-bit-identical
  (testing "same seed → bit-identical 50-tick trajectory (the R4 golden)"
    ;; Run the same seeded simulation twice and compare trajectories
    (let [run (fn [food-seed move-seed]
                (loop [w (exp/make-seeded-world :aif :patchy food-seed move-seed [12 12] 50)
                       n 0
                       acc []]
                  (if (>= n 50)
                    acc
                    (let [w (war/step w)]
                      (recur w (inc n)
                             (conj acc {:tick (:tick w)
                                        :scores (:scores w)
                                        :alive (count (:ants w))}))))))
          traj-1 (run 5001 6001)
          traj-2 (run 5001 6001)]
      (is (= traj-1 traj-2)
          "same seed must produce bit-identical trajectory"))))

(deftest different-seeds-differ
  (testing "different seeds produce different trajectories"
    (let [run (fn [food-seed move-seed]
                (loop [w (exp/make-seeded-world :aif :patchy food-seed move-seed [12 12] 50)
                       n 0
                       acc []]
                  (if (>= n 50)
                    acc
                    (let [w (war/step w)]
                      (recur w (inc n)
                             (conj acc {:tick (:tick w) :scores (:scores w)}))))))
          traj-a (run 5001 6001)
          traj-b (run 5002 6002)]
      (is (not= traj-a traj-b)
          "different seeds should produce different trajectories"))))

(deftest experiment-runs-and-produces-results
  (testing "experiment runs with all 3 arms × 3 scenarios (small N for test speed)"
    (let [result (exp/run-full-experiment 3 [10 10] 50)]
      (is (= 9 (count (:results result))) "9 cells (3 arms × 3 scenarios)")
      (is (every? #(> (:n-runs %) 0) (:results result)) "all cells have runs")
      ;; Every cell has a yield
      (is (every? #(number? (:yield-mean %)) (:results result)))
      ;; Contrast exists for all 3 scenarios
      (is (= 3 (count (:contrast result)))))))

(deftest format-results-produces-output
  (testing "format-results produces readable output"
    (let [result (exp/run-full-experiment 2 [8 8] 30)
          formatted (exp/format-results result)]
      (is (string? formatted))
      (is (re-find #"PER-ARM RESULTS" formatted))
      (is (re-find #"PRE-REGISTERED CONTRAST" formatted))
      (is (re-find #"HYPOTHESIS VERDICT" formatted)))))
