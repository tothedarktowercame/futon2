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
        ;; outcome-risk is downstream of Q(o | action); use a full-support
        ;; preference solely to prove the real risk consumer accepts both rows.
        preference {state-false 1/2 observed-true 1/2}]
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
      (is (number? (manifest/outcome-risk keep-observation preference)))
      (is (number? (manifest/outcome-risk flip-observation preference))))))
