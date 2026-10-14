(ns futon2.aif.outer-target-score-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.outer-target-belief :as belief]
            [futon2.aif.outer-target-score :as score]))

(def costs
  {:closure 0.0 :selected-want-progress 0.2
   :grounded-collateral-progress 0.5 :typed-blocker 0.9
   :abstention-or-failure 1.0})

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
               :preference-authority :operator-provisional}
        a (score/score (assoc input :target "M-a"))
        unseen (score/score (assoc input :target "M-unseen"))]
    (is (= :scored (:status a)))
    (is (> (get-in a [:prediction :grounded-collateral-progress])
           (get-in unseen [:prediction :grounded-collateral-progress])))
    (is (= costs costs))
    (is (= 0.0 (get-in a [:terms :expected-information-gain])))))
