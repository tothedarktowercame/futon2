(ns futon2.aif.registered-run-series-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.registered-run-series :as series]))

(def registration
  (:registration
   (series/read-registration
    "holes/labs/wm-contract/runs/reflective-runtime-2026-v1/PREREG.edn")))

(def certs
  (into {} (map (fn [kind] [kind {:status :compiled :mutation :caught}])
                series/certificate-kinds)))

(def complete-row
  {:schema :wm/registered-run-opportunity-v1
   :opportunity/id "reflective-001"
   :admitted-opportunity? true
   :entity-kind :mission
   :selection {:authority :live-outer-loop
               :target "M-selected-by-the-machine"}
   :terminal-outcome :grounded-progress
   :selection-reached? true
   :verified-applied-patterns [:aif/no-self-certification]
   :before-after-want-evidence :present
   :review {:independence :independent}
   :timing {:wall-clock-ms 1200 :phase-timings-ms {:select 50 :enact 1150}}
   :model-usage {:input-tokens 100 :output-tokens 25 :total-tokens 125}
   :certificates certs
   :plain-language-achievement "Closed one stated want with a reviewed commit."
   :plop-pattern-anchors [:R9 :R16]})

(deftest committed-registration-is-valid
  (let [loaded (series/read-registration
                "holes/labs/wm-contract/runs/reflective-runtime-2026-v1/PREREG.edn")]
    (is (= :valid (:status loaded)))
    (is (= 64 (count (:sha256 loaded))))))

(deftest complete-grounded-row-is-running-example-eligible
  (let [result (series/evaluate-opportunity registration complete-row)]
    (is (:counts-in-denominator? result))
    (is (:running-example-eligible? result))
    (is (empty? (:missing result)))))

(deftest missing-costs-and-certificate-mutations-still-count-but-cannot-qualify
  (let [row (-> complete-row
                (assoc :timing {:status :missing})
                (assoc :model-usage {:status :missing})
                (assoc-in [:certificates :g :mutation] :not-run))
        result (series/evaluate-opportunity registration row)]
    (is (:counts-in-denominator? result))
    (is (false? (:running-example-eligible? result)))
    (is (= :typed-missing (get-in result [:measurement-status :timing])))
    (is (= :typed-missing (get-in result [:measurement-status :model-usage])))
    (is (contains? (:missing result) :certificate-mutation-caught))))

(deftest target-balancing-retry-is-rejected-at-registration
  (let [bad (assoc-in registration [:sampling :no-target-retries-for-balance] false)]
    (is (= :balance-retries-must-be-forbidden
           (:kind (series/validate-registration bad))))))

(deftest ten-click-budget-and-change-chronology-are-fixed
  (is (= 10 (get-in registration [:sampling :maximum-opportunities])))
  (is (= :exactly-ten-admitted-clicks
         (get-in registration [:sampling :opportunity-budget])))
  (is (true? (get-in registration
                     [:between-opportunity-change-policy :chronology-required])))
  (is (= :ten-click-budget-required
         (:kind (series/validate-registration
                 (assoc-in registration [:sampling :maximum-opportunities] 9)))))
  (is (= :intervening-change-chronology-required
         (:kind (series/validate-registration
                 (assoc-in registration
                           [:between-opportunity-change-policy
                            :chronology-required]
                           false))))))

(deftest caller-supplied-target-is-rejected-even-when-the-run-would-otherwise-qualify
  (let [result (series/evaluate-opportunity
                registration
                (assoc complete-row :caller-supplied-target "M-hand-picked"))]
    (is (= :refused (:status result)))
    (is (= :caller-supplied-target-forbidden (:kind result)))))

(deftest missing-live-outer-loop-receipt-is-rejected
  (let [result (series/evaluate-opportunity registration
                                            (dissoc complete-row :selection))]
    (is (= :outer-loop-selection-receipt-required (:kind result)))))
