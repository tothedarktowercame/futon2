(ns futon2.aif.machine-transition-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.machine-model-test :as model-test]
            [futon2.aif.machine-transition :as transition]))

(def states [:addressed :falsified :foreclosed :refined :reopened :spawned :strengthened])
(def actions [:advance-mission :apply-cascade])
(defn point [s] (assoc (zipmap states (repeat 0)) s 1))
(defn controlled-b []
  {:authority :declared-prior :name "wm-status-action-prior-v1"
   :rows (into {}
               (for [s states a actions]
                 [[s a] (point (if (= a :advance-mission)
                                 (case s :spawned :refined :refined :strengthened s)
                                 (case s :addressed :foreclosed :falsified :reopened s))) ]))})
(defn model [x]
  (-> x (assoc :actions actions :B (controlled-b))
      (assoc-in [:policies 0 :actions] [:advance-mission])))
(def params {:model/id "registered-test-model" :model/revision "v1"
             :previous-action nil
             :declared-distinct-action-pairs [[:advance-mission :apply-cascade]]
             :initial-action-semantics :use-initial-D-without-transition})

(deftest complete-controlled-construction
  (model-test/with-example
    (fn [x]
      (let [k (transition/controlled-transition-kernel (model x) actions params)]
        (is (:ok k))
        (is (= 14 (count (:rows k))))
        (is (= :declared-prior (:authority k)))
        (is (every? #(== 1 (reduce + (vals %))) (vals (:rows k))))
        (is (not= (get-in k [:rows [:spawned :advance-mission]])
                  (get-in k [:rows [:spawned :apply-cascade]])))
        (is (= (point :refined)
               (:distribution (transition/apply-belief k (point :spawned) :advance-mission))))))))

(deftest required-refusals
  (model-test/with-example
    (fn [x]
      (let [x (model x)
            refuse #(get-in (transition/controlled-transition-kernel %1 %2 %3) [:refusal :kind])]
        (is (= :support-order-mismatch (refuse x (vec (reverse actions)) params)))
        (is (= :unsupported-action (refuse x actions (assoc params :previous-action :foreign))))
        (is (= :previous-action-unspecified (refuse x actions (dissoc params :previous-action))))
        (is (= :initial-action-semantics-missing
               (refuse x actions (dissoc params :initial-action-semantics))))
        (let [identity-rows (into {} (for [s states a actions] [[s a] (point s)]))
              disguised (assoc x :B {:authority :observed-estimate :name "identity"
                                      :rows identity-rows})]
          (is (= :measurement-pointer-missing (refuse disguised actions params))))
        (let [same (assoc-in x [:B :rows]
                            (into {} (for [s states a actions] [[s a] (point s)])))
              k (transition/controlled-transition-kernel same actions params)]
          (is (= :controlled-actions-indistinguishable
                 (get-in k [:refusal :kind]))))))))
