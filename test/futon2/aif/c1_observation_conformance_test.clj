(ns futon2.aif.c1-observation-conformance-test
  "C1 conformance for ObservationProcess.fixture_action_changes_observation.
  A pattern action first produces its successor token-state distribution;
  predict-observations then applies the manifest likelihood to every successor
  state, and outcome-risk consumes that resulting observation distribution."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.cascade-model-manifest :as manifest]))

(def state-false #{})
(def observed-true #{:true})

;; A(true | false) = false-positive = 1/10;
;; A(true | true) = 1 - false-negative = 9/10.
(def diagonal-nine-tenths
  {:true {:false-neg 1/10 :false-pos 1/10}})

(def keep-pattern {:id :keep :produces #{} :theta 1})
(def flip-pattern {:id :flip :produces #{:true} :theta 1})

(defn observation-after [pattern]
  (let [successor (manifest/pattern-kernel pattern state-false)]
    (manifest/predict-observations diagonal-nine-tenths successor)))

(deftest fixture-action-changes-observation
  (let [keep-observation (observation-after keep-pattern)
        flip-observation (observation-after flip-pattern)
        ;; outcome-risk is downstream of Q(o | action). A uniform preference
        ;; would give both actions the same risk by symmetry, so prefer the
        ;; true observation: the action must then change the risk too.
        preference {state-false 1/10 observed-true 9/10}]
    (testing "the real transition kernel makes the action's successor state"
      (is (= {state-false 1}
             (manifest/pattern-kernel keep-pattern state-false)))
      (is (= {observed-true 1}
             (manifest/pattern-kernel flip-pattern state-false))))
    (testing "the real likelihood turns those successor states into Lean's fixture probabilities"
      (is (= 1/10 (get keep-observation observed-true)))
      (is (= 9/10 (get flip-observation observed-true)))
      (is (not= keep-observation flip-observation)))
    (testing "outcome-risk consumes the resulting action-conditioned observation distributions"
      (let [keep-risk (manifest/outcome-risk keep-observation preference)
            flip-risk (manifest/outcome-risk flip-observation preference)]
        (is (number? keep-risk))
        (is (number? flip-risk))
        (is (< 1e-12 keep-risk))
        (is (> 1e-12 (Math/abs (double flip-risk))))
        (is (< flip-risk keep-risk))))))
