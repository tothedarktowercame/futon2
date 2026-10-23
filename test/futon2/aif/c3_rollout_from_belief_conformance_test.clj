(ns futon2.aif.c3-rollout-from-belief-conformance-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-model-manifest :as model]
            [futon2.aif.cascade-policy :as policy]))

(deftest rollout-starts-at-the-supplied-current-belief
  ;; RolloutAtMachine.rolloutFromBelief_changes_the_outcome:
  ;; Bool false/true are the token states #{} / #{:true}. Zero observation
  ;; error gives the identity likelihood. A theta=0 pattern keeps both states:
  ;; at false it fires with no change; at true it is already achieved.
  (let [q0 {#{} 1}
        mu {#{:true} 1}
        keep-pattern (assoc (policy/token-interpretation
                             :keep {:guard {:needs #{} :forbids #{}}
                                    :produces #{:true}})
                            :theta 0)
        action (constantly [keep-pattern])
        rates {:true {:false-neg 0 :false-pos 0}}
        from-q0 (model/rollout action q0 1)
        from-mu (model/rollout action mu 1)
        out-q0 (model/predict-observations rates from-q0)
        out-mu (model/predict-observations rates from-mu)]
    (is (= q0 (model/pattern-kernel keep-pattern #{})))
    (is (= mu (model/pattern-kernel keep-pattern #{:true})))
    (is (= q0 from-q0))
    (is (= mu from-mu))
    (is (= 0 (get out-q0 #{:true} 0)))
    (is (= 1 (get out-mu #{:true} 0)))
    (is (not= out-q0 out-mu))
    (is (= from-mu (:belief (model/rollout-evaluation action mu 1))))))
