(ns futon2.aif.ruled-outcome-c-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.set :as set]
            [clojure.string :as str]
            [futon2.aif.full-loop-cohort :as cohort]
            [futon2.aif.ruled-outcome-c :as ruled]))

(deftest canonical-vertices-preserve-specialization-carriers
  (is (= #{:nouns :verbs :organization :evidence}
         (set (keys ruled/ruled-vertices))))
  (doseq [v [:nouns :verbs]]
    (is (= :named-empty (get-in ruled/ruled-vertices [v :status])))
    (is (= #{} (get-in ruled/ruled-vertices [v :carrier]))))
  (is (= {:status :ruled :carrier ruled/disposition-outcomes}
         (:organization ruled/ruled-vertices)))
  (is (= :unruled (get-in ruled/ruled-vertices [:evidence :status])))
  (is (= :owed (get-in ruled/ruled-vertices [:evidence :carrier]))))

(deftest ruled-seed-is-wide-and-exact
  (testing "support and exact mass"
    (is (= ruled/disposition-outcomes (:support ruled/seeded-c)))
    (is (= 1 (reduce + (vals (:mass ruled/seeded-c))))))
  (testing "derived zeros remain supported"
    (is (= (set/difference ruled/disposition-outcomes
                                   (set (keys ruled/seeded-positive-masses)))
           ruled/named-zero-dispositions))
    (is (every? (:support ruled/seeded-c) ruled/named-zero-dispositions))
    (is (every? zero? (map (:mass ruled/seeded-c)
                           ruled/named-zero-dispositions))))
  (testing "all recorded positives are positive"
    (is (every? pos? (vals ruled/seeded-positive-masses)))))

(deftest historical-verification-is-not-a-disposition
  (is (= #{:historical-verification-awaiting-validation
           :historical-verification-refused}
         ruled/non-disposition-outcomes))
  (is (= ruled/disposition-outcomes
         (set/difference cohort/outcome-kinds ruled/non-disposition-outcomes)))
  (is (empty? (set/intersection ruled/non-disposition-outcomes
                                (:support ruled/seeded-c)))
      "administrative historical-verification states correctly have no disposition mass"))

(deftest fold-declaration-is-explicit
  (is (= #{:ruled-outcome-c :c-int :c-ser :c-mis}
         (set (map :layer/id ruled/fold-declaration))))
  (is (every? #(and (string? (:basis %)) (not (str/blank? (:basis %))))
              ruled/fold-declaration))
  (is (= :no (:in-ruled-sum
              (first (filter #(= :c-int (:layer/id %)) ruled/fold-declaration)))))
  (let [layer (first (filter #(= :c-mis (:layer/id %)) ruled/fold-declaration))]
    (is (= :no (:in-ruled-sum layer)))
    (is (string? (:composition-law layer)))
    (is (re-find #"AUTH-C-bridge" (:reason layer))))
  (let [layer (first (filter #(= :ruled-outcome-c (:layer/id %))
                             ruled/fold-declaration))]
    (is (true? (:folded? layer)))
    (is (= "futon2:holes/labs/wm-contract/RULINGS-walkthrough-2026-09-08.md:101-112"
           (:ruling layer)))))
