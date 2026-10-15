(ns futon2.aif.outer-target-belief-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.outer-target-belief :as belief]))

(defn- row [run target terminal & {:keys [effects collateral blocker]}]
  {:run/id run :click/id (str run "-click") :task {:id target :kind :mission}
   :terminal-outcome {:class terminal}
   :selected-want-effects (or effects {:status :absent})
   :collateral-progress (or collateral {:status :absent})
   :blocker (or blocker {:status :absent})})

(defn- dataset [rows]
  {:status :produced :source-pin {:path "fixture" :sha256 "abc"} :rows rows})

(deftest folds-only-observed-outcomes-into-the-chosen-target
  (let [result
        (belief/build
         {:prior (belief/prior 1 :operator-provisional)
          :dataset
          (dataset
           [(row "r1" "M-a" :grounded-progress
                 :effects {:status :present
                           :counts {:reached 0 :progressed 0 :blocked 0 :untouched 4}}
                 :collateral {:status :present :kind :grounded-collateral-progress})
            (row "r2" "M-b" :closure)
            (row "r3" "M-a" :grounded-progress)])})]
    (is (= :produced (:status result)))
    (is (= {:rows 3 :applied 2 :unresolved 1} (:counts result)))
    (is (= 2 (get-in result [:targets "M-a" :alpha
                             :grounded-collateral-progress])))
    (is (= 1 (get-in result [:targets "M-a" :alpha :closure])))
    (is (= 2 (get-in result [:targets "M-b" :alpha :closure])))
    (is (= :categorical-negative-log-evidence
           (get-in result [:events 0 :present-fit :kind])))
    (is (= :before-parameter-update
           (get-in result [:events 0 :present-fit :computed])))
    (is (= 0.2 (get-in result [:events 0 :present-fit
                               :predictive-probability])))
    (is (< (Math/abs (- (Math/log 5.0)
                        (get-in result [:events 0 :present-fit :value])))
           1.0e-12))
    (is (= :outcome-evidence-insufficient
           (get-in result [:events 2 :observation :reason])))))

(deftest selected-want-progress-outranks-collateral-classification
  (let [result
        (belief/build
         {:prior (belief/prior 1)
          :dataset
          (dataset
           [(row "r1" "M-a" :grounded-progress
                 :effects {:status :present
                           :counts {:reached 0 :progressed 1 :blocked 0 :untouched 3}}
                 :collateral {:status :present :kind :grounded-collateral-progress})])})]
    (is (= :selected-want-progress
           (get-in result [:events 0 :observation :class])))))

(deftest refuses-implicit-or-malformed-authority
  (is (= :prior-invalid
         (:reason (belief/build {:dataset (dataset [])}))))
  (is (= :dataset-not-produced
         (:reason (belief/build {:dataset {:status :refused}
                                 :prior (belief/prior 1)})))))
