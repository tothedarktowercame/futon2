(ns futon2.aif.outer-canonical-g-frontier-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.cascade-coapply-g :as canonical]
            [futon2.aif.outer-canonical-g :as sut]))

;; Live pin: b5's input record (fc464172), sealed menu entries projected to the
;; keys produce reads and the policy objective it ran beside, values verbatim.
;; Provenance is under :source in the fixture.
(def fixture (edn/read-string (slurp "test/fixtures/outer-canonical-g/b5-input-fc464172.edn")))

(def wiring "M-war-machine-wiring/policy-2629917c87a61978be8f")
(def bindings "M-recommendation-bindings/policy-7e8a0806fea29fa47914")
(def self-improvement "M-self-improvement-loop/policy-1bbcf6a06eb2766b5e77")
(def stack-geometry "M-stack-geometry/policy-c7d7518e1e185c2e2318")

(defn- by-id [frontier] (into {} (map (juxt :policy-id identity)) (:entries frontier)))

(deftest b5-canonical-g-separates-what-the-objective-tied
  (let [menu (:sealed-menu fixture)
        objective (:policy-objective fixture)
        frontier (sut/produce menu)
        rows (by-id frontier)]
    (testing "the live menu supplies every input: all four entries are scored at one H"
      (is (= :produced (:status frontier)))
      (is (= 10 (get-in frontier [:horizon-choice :horizon])))
      (is (= {self-improvement 8 stack-geometry 19 wiring 27 bindings 12}
             (get-in frontier [:horizon-choice :unit-counts])))
      (is (= {self-improvement 8 stack-geometry 10 wiring 10 bindings 10}
             (get-in frontier [:horizon-choice :entry-horizon-steps])))
      (is (every? #(= :scored (:status %)) (vals rows)))
      (is (= :compareAtHorizonN (get-in frontier [:comparison :lean-reduction]))))
    (testing "canonical ranks on the live menu"
      (is (= {wiring 1 bindings 1 self-improvement 2 stack-geometry 3}
             (into {} (map (fn [[id r]] [id (:rank r)])) rows)))
      (is (= (:G (rows wiring)) (:G (rows bindings))))
      (is (< (:G (rows wiring)) (:G (rows self-improvement)) (:G (rows stack-geometry)))))
    (testing "the objective the run used tied all four and drew stack-geometry"
      (is (= :gamma-times-G-minus-lnE (get-in objective [:comparison :objective])))
      (is (= 1 (count (set (vals (get-in objective [:precision-row :G]))))))
      (is (= 1 (count (set (vals (get-in objective [:precision-row :E]))))))
      (is (= stack-geometry (get-in objective [:selected :policy-id]))))
    (testing "so the two disagree on the live menu: the drawn policy is canonically last"
      (is (= 3 (:rank (rows (get-in objective [:selected :policy-id]))))))
    (testing "each row pins the bound action digest it scored"
      (doseq [e (:entries menu)]
        (is (= (get-in e [:action :cascade-digest])
               (get-in (rows (:policy-id e)) [:action-pin :cascade-digest])))))
    (testing "produce does not alter the menu it reads"
      (is (= menu (:sealed-menu fixture))))))

(deftest missing-class-model-is-a-retained-unscored-refusal
  (let [menu (update (:sealed-menu fixture) :entries
                     #(assoc % 0 (update (first %) :class-emission dissoc :model)))
        rows (by-id (sut/produce menu))
        refusal (rows self-improvement)]
    (is (= self-improvement (get-in menu [:entries 0 :policy-id])))
    (is (= :refused (:status refusal)))
    (is (= :class-emission-model-unavailable (:reason refusal)))
    (is (not (contains? refusal :G)))
    (is (not (contains? refusal :rank)))
    (is (= (get-in menu [:entries 0 :action :cascade-digest])
           (get-in refusal [:action-pin :cascade-digest])))
    (testing "the remaining three are still scored, and ranked among themselves"
      (is (every? #(= :scored (:status (rows %))) [wiring bindings stack-geometry]))
      (is (= #{1 2} (set (map #(:rank (rows %)) [wiring bindings stack-geometry])))))))

(deftest scorer-exception-is-a-typed-refusal-not-a-thrown-selection
  (with-redefs [canonical/compare-at-horizon
                (fn [& _] (throw (ex-info "boom" {})))]
    (let [frontier (sut/produce (:sealed-menu fixture))]
      (is (every? #(= :canonical-score-threw (:reason %)) (:entries frontier)))
      (is (every? #(not (contains? % :G)) (:entries frontier))))))
