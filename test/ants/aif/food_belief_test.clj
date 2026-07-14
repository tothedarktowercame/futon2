(ns ants.aif.food-belief-test
  "Tests for the hidden-state food-location belief + directed EIG."
  (:require [clojure.test :refer [deftest is testing]]
            [ants.aif.food-belief :as fb]))

(deftest food-belief-updates-from-observation
  (testing "visiting a food-rich cell sets high food-prob, low uncertainty"
    (let [belief (-> (fb/initial-food-belief)
                     (fb/update-food-belief
                       {:grid {:size [5 5]
                               :cells (into {} (for [x (range 5) y (range 5)]
                                                 [[x y] {:food 0.0 :pher 0.0}]))}}
                       {:loc [2 2] :food 0.8 :food-trace 0.1}))
          cell (fb/food-belief-at belief [2 2])]
      (is (> (:food-prob cell) 0.5) "food-prob is high after seeing food")
      (is (< (:uncertainty cell) 0.5) "uncertainty is low after visiting"))))

(deftest directed-eig-rewards-uncertain-plausible
  (testing "directed EIG is higher for uncertain-plausible-food cell than known-empty"
    (let [;; Cell A: visited, no food → low prob, low uncertainty
          belief-a {[1 1] {:food-prob 0.1 :uncertainty 0.1 :visits 3}}
          ;; Cell B: unvisited but plausibly has food → moderate prob, high uncertainty
          belief-b {[1 1] {:food-prob 0.4 :uncertainty 0.9 :visits 0}}
          eig-a (fb/directed-eig belief-a [1 1])
          eig-b (fb/directed-eig belief-b [1 1])]
      (is (> eig-b eig-a)
          (str "uncertain-plausible cell has higher EIG than known-empty: "
               "B=" eig-b " > A=" eig-a)))))

(deftest directed-eig-rewards-food-over-foodless
  (testing "directed EIG is higher for plausible-food cell than equally-novel foodless cell"
    (let [;; Cell with food seen nearby → high prob, high uncertainty (novel but plausible)
          belief-food {[1 1] {:food-prob 0.8 :uncertainty 0.9 :visits 0}}
          ;; Cell with no food evidence → low prob, high uncertainty (novel but foodless)
          belief-empty {[1 1] {:food-prob 0.1 :uncertainty 0.9 :visits 0}}
          eig-food (fb/directed-eig belief-food [1 1])
          eig-empty (fb/directed-eig belief-empty [1 1])]
      (is (> eig-food eig-empty)
          (str "plausible-food cell has higher EIG than foodless cell: "
               "food=" eig-food " > empty=" eig-empty)))))

(deftest directed-eig-from-prediction
  (testing "directed EIG from predicted observation uses predicted food × uncertainty"
    (let [belief {[2 2] {:food-prob 0.6 :uncertainty 0.7 :visits 1}}
          eig (fb/directed-eig-from-prediction belief {:loc [2 2] :food 0.5})]
      (is (pos? eig) "EIG is positive for uncertain-plausible prediction"))))
