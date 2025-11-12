(ns ants.pheromone-test
  (:require [clojure.test :refer [deftest is testing]]
            [ants.war :as war]))

(def pheromone-drop* (deref (resolve 'ants.war/pheromone-drop)))
(def richest-neighbour* (deref (resolve 'ants.war/richest-neighbour)))

(defn world-with-ant
  [loc]
  (let [world (war/new-world {:size [5 5]
                              :ants-per-side 1})
        ant-id :aif-0
        orig (get-in world [:ants ant-id :loc])]
    (-> world
        (assoc-in [:grid :cells orig :ant] nil)
        (assoc-in [:ants ant-id :loc] loc)
        (assoc-in [:grid :cells loc :ant] ant-id))))

(deftest pheromone-drop-splits-across-steps
  (let [world (world-with-ant [2 2])
        target [3 2]
        ahead [4 2]
        result (pheromone-drop* world (get-in world [:ants :aif-0]) 0.9 target)
        here (get-in result [:grid :cells [2 2] :pher])
        there (get-in result [:grid :cells target :pher])
        ahead-val (get-in result [:grid :cells ahead :pher])]
    (testing "deposit splits between current, target, and ahead cell"
      (is (<= (Math/abs (- here 0.3)) 1e-6))
      (is (<= (Math/abs (- there 0.6)) 1e-6))
      (is (<= (Math/abs (- ahead-val 0.3)) 1e-6)))))
