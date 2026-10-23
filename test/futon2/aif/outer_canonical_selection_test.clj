(ns futon2.aif.outer-canonical-selection-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.outer-canonical-g :as frontier]
            [futon2.aif.outer-canonical-selection :as sut]))

(def fixture (edn/read-string (slurp "test/fixtures/outer-canonical-g/b5-input-fc464172.edn")))
(def menu (:sealed-menu fixture))
(def wiring "M-war-machine-wiring/policy-2629917c87a61978be8f")
(def bindings "M-recommendation-bindings/policy-7e8a0806fea29fa47914")
(def self-improvement "M-self-improvement-loop/policy-1bbcf6a06eb2766b5e77")
(def stack "M-stack-geometry/policy-c7d7518e1e185c2e2318")

(deftest b5-canonical-draw-replaces-the-objective-tie-only-for-choice
  (let [f (frontier/produce menu) first (sut/evaluate f menu 0.0) last (sut/evaluate f menu 0.9)]
    (is (= wiring (get-in first [:action :policy-id])))
    (is (= bindings (get-in last [:action :policy-id])))
    (is (= ["3a5228e7942579f3e0095ee4a902b1b08a0a9d0bf7a11c966c52bddbbb4e4475"
            "13c0068ddbc12266370e59d82812d9b10472a38831e7e61f0e9aef6a9e690e11"]
           (get-in first [:draw :candidates])))
    (is (= 0.0 (get-in first [:draw :u])))
    (is (= stack (get-in fixture [:policy-objective :selected :policy-id])))
    (is (not= stack (get-in first [:action :policy-id])))))

(deftest ln-e-does-not-change-a-retained-canonical-choice
  (let [f (frontier/produce menu)
        a (sut/evaluate f menu 0.0)
        ;; The old evaluator's gamma*G-lnE would change with this value; the
        ;; canonical frontier intentionally has no lnE input.
        b (sut/evaluate f menu 0.0)]
    (is (= (:action a) (:action b)))
    (is (= :canonical-G-min-over-scored-frontier (:basis a)))))

(deftest refused-frontier-rows-never-enter-the-minimum
  (let [broken (update menu :entries
                       #(mapv (fn [e] (if (contains? #{wiring bindings} (:policy-id e))
                                        (update e :class-emission dissoc :model) e)) %))
        chosen (sut/evaluate (frontier/produce broken) broken 0.0)]
    (is (= self-improvement (get-in chosen [:action :policy-id])))
    (is (not (contains? #{wiring bindings} (get-in chosen [:action :policy-id]))))))
