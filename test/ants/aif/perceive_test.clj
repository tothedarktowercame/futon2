(ns ants.aif.perceive-test
  (:require [clojure.test :refer [deftest is testing]]
            [ants.aif.perceive :as perceive]))

(def world
  {:grid {:size [5 5]
          :max-food 5.0
          :max-pher 5.0
          :cells {[2 2] {:food 3.0 :pher 2.5}
                  [2 1] {:food 4.0 :pher 3.4}
                  [2 3] {:food 1.0 :pher 0.1}
                  [1 2] {:food 0.0 :pher 0.0}
                  [3 2] {:food 0.0 :pher 0.0}}}
   :homes {:aif [4 4]
           :classic [0 0]}
   :colonies {:aif {:reserves 1.2}
              :classic {:reserves 1.0}}})

(def observation
  {:food 0.6
   :pher 0.5
   :food-trace 0.3
   :pher-trace 0.2
   :home-prox 0.4
   :enemy-prox 0.1
   :h 0.7
   :ingest 0.15
   :friendly-home 0.0
   :trail-grad 0.2
   :novelty 0.9
   :dist-home 0.35
   :reserve-home 0.5
   :cargo 0.2})

(def ant
  {:species :aif
   :loc [2 2]
   :mu {:pos [2 2]
        :goal [4 4]
        :h 0.6
        :sens {:food 0.2
               :pher 0.7
               :food-trace 0.4
               :pher-trace 0.4
               :home-prox 0.1
               :enemy-prox 0.2
               :h 0.6}}
   :prec {:Pi-o {:food 1.2 :pher 0.9}
          :tau 1.4}})

(deftest perceive-updates-hunger
  (let [{:keys [mu prec trace errors]} (perceive/perceive world ant observation
                                                         {:max-steps 3
                                                          :alpha 0.4
                                                          :beta 0.2})]
    (testing "sensory predictions adapt"
      (is (not= (:sens (:mu ant)) (:sens mu))))
    (testing "hunger stays normalized"
      (is (<= 0.0 (:h mu) 1.0)))
    (testing "precision carries tau"
      (is (number? (:tau prec))))
    (testing "trace records steps"
      (is (= 3 (count trace))))
    (testing "errors keyed by sensory modalities"
      (is (= (set [:food :pher :food-trace :pher-trace :home-prox :enemy-prox :h :ingest
                   :friendly-home :trail-grad :novelty :dist-home :reserve-home :cargo])
             (set (keys errors)))))))

(deftest perceive-defaults-lazy-state
  (let [{:keys [mu prec]} (perceive/perceive world {:species :aif :loc [1 1]} observation)]
    (testing "mu seeds position, goal and hunger"
      (is (= [1 1] (:pos mu)))
      (is (vector? (:goal mu)))
      (is (<= 0.0 (:h mu) 1.0)))
    (testing "prec merges defaults"
      (is (= #{:Pi-o :tau} (set (keys prec)))))))
