(ns futon2.aif.controller-authority-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.controller-authority :as authority]
            [futon2.aif.mission-registry :as missions]
            [futon2.aif.policy :as policy]))

(def action {:type :advance-mission :target "M-outside-old-canary"})
(def decision {:action action :controller-score 0.25
               :selection-law {:applied :controller-head}})

(deftest actual-controller-decision-is-authorized
  (with-redefs [missions/mission-status #(hash-map :open? (= % (:target action)))]
    (let [result (authority/authorize decision [{:action action}])]
      (is (= action (:action result)))
      (is (= (:selection-law decision) (:selection-law result)))
      (is (true? (:actuation-authorized? result)))
      (is (false? (get-in result [:actuation :executed?])))
      (is (= :all-open-missions (get-in result [:actuation :admissible-set])))
      (is (= authority/scope-authority (:scope-authority result)))
      (is (= "futon3c e74c7e7" (get-in result [:rollback :recorded-fallback])))
      (is (= :not-applicable (get-in result [:actuation :machine-gates :serving-cache :status])))
      (is (= :required (get-in result [:actuation :machine-gates :tripwires :status]))))))

(deftest invalid-decisions-never-authorize
  (with-redefs [missions/mission-status #(hash-map :open? (= % (:target action)))]
    (doseq [[candidate domain reason]
            [[(assoc-in decision [:action :target] "M-closed") [{:action action}] :target-not-open]
             [(assoc-in decision [:action :target] "M-unknown") [{:action action}] :target-not-open]
             [decision [] :action-not-admissible]
             [(assoc-in decision [:action :type] :invented) [{:action action}] :action-not-admissible]
             [(dissoc decision :controller-score) [{:action action}] :controller-score-missing-or-invalid]
             [(assoc decision :controller-score ##NaN) [{:action action}] :controller-score-missing-or-invalid]
             [(dissoc decision :selection-law) [{:action action}] :selection-law-missing-or-invalid]]]
      (is (= reason
             (try (authority/authorize candidate domain)
                  (catch clojure.lang.ExceptionInfo e (:reason (ex-data e)))))))))

(deftest controller-selection-outside-old-three-keeps-its-law
  (let [ranked [{:action action :rank 1 :controller-score 0.25}
                {:action {:type :advance-mission :target "M-aif-policy-conditioned-eig"}
                 :rank 2 :controller-score 0.75}]
        selected (policy/select-action ranked {:selection-boundary :strategic-recommendation
                                               :selection-law :controller-head})]
    (with-redefs [missions/mission-status (constantly {:open? true})]
      (let [result (authority/authorize selected ranked)]
        (is (= action (:action result)))
        (is (= (:controller-score selected) (:controller-score result)))
        (is (= (:selection-law selected) (:selection-law result)))
        (is (true? (get-in result [:actuation :authorized?])))
        (is (not (contains? result :selected-policy-id)))
        (is (not (contains? result :strategic-memory)))))))
