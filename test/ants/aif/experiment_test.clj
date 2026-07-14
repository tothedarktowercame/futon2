(ns ants.aif.experiment-test
  "Slice 5 tests: seed reproducibility (R4 golden) + experiment sanity."
  (:require [clojure.test :refer [deftest is testing]]
            [ants.aif.experiment :as exp]
            [ants.aif.policy :as ants.aif.policy]
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
    (let [result (exp/run-full-experiment 3 [10 10] 50 0.02)]
      (is (= 9 (count (:results result))) "9 cells (3 arms × 3 scenarios)")
      (is (every? #(> (:n-runs %) 0) (:results result)) "all cells have runs")
      (is (every? #(number? (:yield-mean %)) (:results result)))
      (is (= 3 (count (:contrast result)))))))

(deftest format-results-produces-output
  (testing "format-results produces readable output"
    (let [result (exp/run-full-experiment 2 [8 8] 30 0.02)
          formatted (exp/format-results result)]
      (is (string? formatted))
      (is (re-find #"PER-ARM RESULTS" formatted))
      (is (re-find #"PRE-REGISTERED CONTRAST" formatted))
      (is (re-find #"HYPOTHESIS VERDICT" formatted)))))

(deftest eig-term-is-action-differentiated
  (testing "epistemic value differs across actions (not constant)"
    (let [mu {:h 0.5 :cargo 0.1 :goal [4 4]
              :var (into {} (for [k [:food :pher :food-trace :pher-trace :home-prox :enemy-prox :h :ingest
                                      :friendly-home :trail-grad :novelty :dist-home :reserve-home :cargo]]
                              [k 0.5]))}
          prec {:tau 1.0 :Pi-o {:food 1.0 :pher 0.8}}
          obs {:food 0.3 :pher 0.2 :food-trace 0.15 :pher-trace 0.2
               :home-prox 0.4 :enemy-prox 0.3 :h 0.5 :hunger 0.5 :ingest 0.1
               :friendly-home 0.0 :trail-grad 0.1 :novelty 0.8
               :dist-home 0.6 :reserve-home 0.5 :cargo 0.1 :mode :outbound}
          cfg {:actions [:forage :hold :pheromone]
               :efe {:lambda {:pragmatic 1.0 :ambiguity 0.5 :info 0.4 :epistemic 0.5
                              :colony 0.4 :survival 1.2}}}
          r (ants.aif.policy/eval-policy mu prec obs cfg)
          gs (map :G (:ranking r))]
      (is (apply not= gs) "G values differ across actions (EIG is action-differentiated)"))))
