(ns ants.aif.starvation-test
  (:require [clojure.test :refer [deftest is testing]]
            [ants.aif.policy :as policy]))

(def mu {:h 0.85})
(def prec {:tau 1.2})
(def observation {:h 0.85
                  :hunger 0.85
                  :cargo 0.5
                  :reserve-home 0.15
                  :food 0.0
                  :food-trace 0.0
                  :pher 0.1
                  :pher-trace 0.0
                  :trail-grad 0.05
                  :novelty 0.3
                  :home-prox 0.4
                  :dist-home 0.7
                  :friendly-home 0.96
                  :enemy-prox 0.2
                  :ingest 0.1})

(deftest starvation-clamps-tau-and-forces-return
  (let [{:keys [tau policies action]}
        (policy/choose-action mu prec observation {:actions [:forage :return :hold :pheromone]})]
    (testing "tau clamps when hungry"
      (is (<= tau 0.80)))
    (testing "forage pruned when starving near empty fields"
      (is (not (contains? policies :forage))))
    (testing "return preferred under starvation"
      (is (= :return action)))))
