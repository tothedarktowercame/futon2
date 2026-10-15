(ns futon2.aif.outer-target-score-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.outer-target-belief :as belief]
            [futon2.aif.outer-target-score :as score]))

(def costs
  {:closure 0.0 :selected-want-progress 0.2
   :grounded-collateral-progress 0.5 :typed-blocker 0.9
   :abstention-or-failure 1.0})

(def transform {:kind :softmax-negative-cost :inverse-temperature 1.0})

(deftest outcome-evidence-changes-the-target-score-without-changing-preferences
  (let [prior (belief/prior 1 :operator-provisional)
        dataset {:status :produced :source-pin {:path "runs" :sha256 "abc"}
                 :rows [{:run/id "r1" :click/id "c1"
                         :task {:id "M-a"}
                         :terminal-outcome {:class :grounded-progress}
                         :selected-want-effects
                         {:status :present
                          :counts {:reached 0 :progressed 0 :blocked 0 :untouched 1}}
                         :collateral-progress {:status :present}
                         :blocker {:status :absent}}]}
        learned (belief/build {:dataset dataset :prior prior})
        input {:belief learned :preference-costs costs
               :preference-transform transform
               :preference-authority :operator-provisional}
        a (score/score (assoc input :target "M-a"))
        unseen (score/score (assoc input :target "M-unseen"))]
    (is (= :scored (:status a)))
    (is (> (get-in a [:prediction :grounded-collateral-progress])
           (get-in unseen [:prediction :grounded-collateral-progress])))
    (is (= costs costs))
    (is (= :categorical-kl-q-predicted-outcomes-to-c
           (get-in a [:terms :risk-kind])))
    (is (= :categorical-likelihood-entropy
           (get-in a [:terms :ambiguity-kind])))
    (is (= :present (get-in a [:terms :r8-present-fit :status])))
    (is (= :outer-target-score-diagnostic
           (get-in a [:terms :r8-consumer])))
    (is (= 0.0 (get-in a [:terms :expected-information-gain])))))

(deftest cadence-cost-requires-pinned-continuation-to-be-waived
  (let [learned (belief/build
                 {:prior (belief/prior 1)
                  :dataset {:status :produced :source-pin {:path "runs" :sha256 "abc"}
                            :rows [{:run/id "r" :click/id "c" :task {:id "M-a"}
                                    :terminal-outcome {:class :closure}
                                    :selected-want-effects {:status :absent}
                                    :collateral-progress {:status :absent}
                                    :blocker {:status :absent}}]}})
        base {:belief learned :target "M-a" :preference-costs costs
              :preference-transform transform
              :preference-authority :operator
              :controller {:recent-selection-cost 0.25
                           :continuation-credit 0.35}}
        without (score/score base)
        with (score/score
              (assoc-in base [:controller :continuations "M-a"]
                        {:status :present :next-step "Measure uptake"
                         :acceptance-condition "A pinned report exists"
                         :evidence {:path "mission.md"
                                    :sha256 (apply str (repeat 64 "a"))}}))]
    (is (= 0.25 (get-in without [:terms :recent-selection-opportunity-cost])))
    (is (= 0.0 (get-in with [:terms :recent-selection-opportunity-cost])))
    (is (= 0.35 (get-in with [:terms :continuation-credit])))
    (is (< (:g with) (:g without)))))
