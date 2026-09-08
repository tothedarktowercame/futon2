(ns futon2.aif.ruled-outcome-c-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.set :as set]
            [clojure.string :as str]
            [futon2.aif.full-loop-cohort :as cohort]
            [futon2.aif.ruled-outcome-c :as ruled]))

(deftest ruled-seed-is-wide-and-exact
  (testing "support and exact mass"
    (is (= cohort/outcome-kinds (:support ruled/seeded-c)))
    (is (= 1 (reduce + (vals (:mass ruled/seeded-c))))))
  (testing "derived zeros remain supported"
    (is (= (set/difference cohort/outcome-kinds
                                   (set (keys ruled/seeded-positive-masses)))
           ruled/named-zero-dispositions))
    (is (every? (:support ruled/seeded-c) ruled/named-zero-dispositions))
    (is (every? zero? (map (:mass ruled/seeded-c)
                           ruled/named-zero-dispositions))))
  (testing "all recorded positives are positive"
    (is (every? pos? (vals ruled/seeded-positive-masses)))))

(deftest fold-declaration-is-explicit
  (is (= #{:ruled-outcome-c :c-int :c-ser :c-mis}
         (set (map :layer/id ruled/fold-declaration))))
  (is (every? #(and (string? (:basis %)) (not (str/blank? (:basis %))))
              ruled/fold-declaration))
  (is (= :no (:in-ruled-sum
              (first (filter #(= :c-int (:layer/id %)) ruled/fold-declaration)))))
  (let [layer (first (filter #(= :ruled-outcome-c (:layer/id %))
                             ruled/fold-declaration))]
    (is (true? (:folded? layer)))
    (is (= "futon2:holes/labs/wm-contract/RULINGS-walkthrough-2026-09-08.md:101-112"
           (:ruling layer)))))
