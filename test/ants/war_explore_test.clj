(ns ants.war-explore-test
  (:require [clojure.test :refer [deftest is testing]]
            [ants.war :as war]))

(defn world-with-white-ring
  []
  (let [world (war/new-world {:size [7 7]
                              :ants-per-side 1})
        ant-id :aif-0
        loc [3 3]
        neighbours [[2 3] [4 3] [3 2] [3 4] [2 2] [2 4] [4 2] [4 4]]]
    (reduce (fn [w cell]
              (assoc-in w [:grid :cells cell :food] 0.0))
            (-> world
                (assoc-in [:grid :cells (get-in world [:ants ant-id :loc]) :ant] nil)
                (assoc-in [:ants ant-id :loc] loc)
                (assoc-in [:grid :cells loc :ant] ant-id)
                (assoc-in [:grid :cells loc :food] 0.0)
                (assoc-in [:ants ant-id :white-streak] 3))
            neighbours)))

(deftest forced-move-kicks-in-after-white-streak
  (let [world (world-with-white-ring)
        ant (get-in world [:ants :aif-0])
        result ((deref (resolve 'ants.war/random-wander)) world ant {})]
    (testing "ant moves after white streak"
      (is (:moved? result))
      (is (not= [3 3] (:loc (:ant result)))))))
