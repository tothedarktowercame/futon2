(ns futon2.aif.preference-discovery-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.preference-discovery :as discovery]
            [futon2.aif.ruled-outcome-c :as ruled]))

(def fixture-path "test/fixtures/f10-rulings-fragment.edn")

(deftest t6-extractor-provenance-and-decision-sheet
  (let [seed-before ruled/seeded-c
        fragment (edn/read-string (slurp fixture-path))
        planted (update-in fragment [:claims 1] assoc :proposed-mass 1/4)
        result (discovery/extract (pr-str planted) seed-before)
        components (:proposed-components result)
        decisions (:decision-sheet result)]
    (testing "only explicit preference claims become proposed-C components"
      (is (= [:grounded-change :agent-unavailable]
             (mapv :disposition components)))
      (is (every? #(seq (:provenance %)) components)))
    (testing "agreement accumulates evidence and disagreement becomes a question"
      (is (= [:agent-unavailable] (mapv :disposition decisions)))
      (is (= :proposed-c-disagrees-with-ruled-c (:question (first decisions))))
      (is (= "holes/labs/wm-contract/runs/D1-evidence/kl-worked-example.edn:11"
             (get-in decisions [0 :provenance 0 :citation]))))
    (testing "extraction writes no preference and invents no missing component"
      (is (= seed-before ruled/seeded-c))
      (is (= 2 (count components)))
      (is (= (:support seed-before) (:support result))))))

(deftest t6-real-landscape-is-pinned-and-never-quantified-by-the-extractor
  (let [spec (edn/read-string
              (slurp "holes/labs/wm-contract/preference-landscape.edn"))
        result (discovery/extract-landscape "." spec ruled/seeded-c)
        component (first (:proposed-components result))]
    (is (= 4 (count (:landscape result))))
    (is (= :solved-apm-problems-and-diagnosable-errors
           (get-in component [:proposal :object])))
    (is (= :not-found (:ruled-mass component)))
    (is (= :which-ruled-disposition-carries-this-preference
           (get-in result [:decision-sheet 0 :question])))
    (is (= ruled/seeded-c (:seeded-c result)))
    (is (= "holes/labs/wm-contract/RULINGS-walkthrough-2026-09-08.md:276-278"
           (get-in component [:provenance 0 :citation])))))
