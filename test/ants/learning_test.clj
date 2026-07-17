(ns ants.learning-test
  (:require [ants.learning :as learning]
            [ants.learning-experiment :as experiment]
            [ants.war :as war]
            [clojure.test :refer [deftest is testing]]))

(deftest review-holds-success-and-revises-local-failure
  (let [ant (learning/ensure-state {:id :learning-0
                                    :brain :learning
                                    :cargo 0.0
                                    :h 0.4})
        success (learning/review ant ant {:tick 1
                                          :policy-bin :food-nearby
                                          :action :forage
                                          :moved true})
        failure (learning/review ant ant {:tick 2
                                          :policy-bin :at-water-edge
                                          :action :forage
                                          :moved true
                                          :water-death true})]
    (testing "success preserves the enacted entry"
      (is (= :success (:review success)))
      (is (= :forage (get-in success [:ant :learning-policy :food-nearby]))))
    (testing "failure revises only the active entry"
      (is (= :failure (:review failure)))
      (is (= :turn-back
             (get-in failure [:ant :learning-policy :at-water-edge])))
      (is (= :forage
             (get-in failure [:ant :learning-policy :food-nearby]))))))

(deftest default-worlds-preserve-existing-brains
  (let [world (war/new-world {:size [8 8]
                              :ants-per-side 1
                              :armies [:classic :aif]})]
    (is (= #{:classic :aif} (set (map :brain (vals (:ants world))))))
    (is (not-any? :terrain (vals (get-in world [:grid :cells]))))))

(deftest learning-clears-river-avoidance-bar
  (let [{:keys [classic learning learned-cascade-samples
                learning-beat-fixed?]}
        (experiment/run-poc {:ticks 60 :window-size 20 :seed 4242})
        classic-rates (mapv :water-death-rate (:rates classic))
        learning-rates (mapv :water-death-rate (:rates learning))]
    (is learning-beat-fixed?)
    (is (> (last classic-rates) 0.5))
    (is (pos? (first learning-rates)))
    (is (zero? (last learning-rates)))
    (is (every? #(= :turn-back (:at-water-edge %))
                learned-cascade-samples))))
