(ns futon2.aif.selection-coapplication-scoring-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-evaluation-trace :as trace]
            [futon2.aif.cascade-observation-scoring :as scoring]))

(def fixture
  (edn/read-string (slurp "test/fixtures/selection-scoring/2026-10-05-c9d25d6a.edn")))

(def action
  (:action (edn/read-string
            (slurp "test/fixtures/selected-want-outcome/2026-10-05-c9d25d6a-action.edn"))))

(defn score
  ([candidate] (score candidate (:opts fixture)))
  ([candidate opts]
   (first (scoring/rank-cascade-actions (:state fixture) [candidate] opts))))

(defn list-action [candidate]
  (update candidate :construction-receipt dissoc :order))

(defn trace-record [candidate scored]
  {:decision {:selection-certificate
              {:candidates [{:id candidate}]
               :node-evaluation-traces
               [{:id candidate :horizon (:horizon-steps scored) :status :recorded
                 :evaluations (get-in scored [:certificate :node-evaluations])}]}}})

(defn mismatch-kinds [record]
  (set (map :kind (:errors (trace/validate-record record)))))

(deftest retained-list-inputs-reproduce-recorded-score
  (let [actual (score (list-action action))]
    (is (= (get-in fixture [:recorded :g]) (:controller-score actual)))
    (is (= (get-in fixture [:recorded :steps])
           (mapv #(select-keys % [:tau :risk :ambiguity])
                 (get-in actual [:certificate :steps]))))))

(deftest recorded-non-chain-uses-co-application
  (let [list-score (score (list-action action))
        co-score (score action)
        list-steps (get-in list-score [:certificate :steps])
        steps (get-in co-score [:certificate :steps])
        wants (set (mapcat :produces (:precedence action)))]
    (is (= {:order :co-application} (:order-use co-score)))
    (is (= :co-application-frontier-theta-v1
           (get-in steps [0 :node-evaluation :model :semantics])))
    (is (= [:ukrns/reader-run-path :war-machine/state-capture
            :orchestration/recorded-handoff]
           (get-in steps [0 :node-evaluation :states 0 :frontier])))
    (is (= [:measurement/warrant-travels-with-the-number]
           (get-in steps [1 :node-evaluation :states 0 :frontier])))
    (is (= 3 (count (first (keys (:belief (nth steps 0)))))))
    (is (= wants (first (keys (:belief (nth steps 1))))))
    (is (= [1 2] (mapv #(count (first (keys (:belief %)))) (take 2 list-steps))))
    ;; This preference says "not yet" before the horizon and looks only at the
    ;; target class at the horizon. With theta=1 both kernels reach that same
    ;; terminal class, so equal G here is not evidence that their kernels agree.
    (is (= 1.0498221244986776 (:controller-score co-score)
           (:controller-score list-score)))
    (is (= [0.0 0.0 0.0 1.0498221244986776]
           (mapv :risk steps)))))

(deftest half-theta-one-step-distinguishes-kernels
  (let [half (update action :precedence
                     #(mapv (fn [pattern] (assoc pattern :theta 1/2)) %))
        terminal (get-in fixture [:opts :observation-model :class-preference 4])
        opts (-> (:opts fixture)
                 (assoc :horizon-steps 1
                        :prediction-context {:occurrence-id "theta-half-control" :tau 1})
                 (assoc-in [:observation-model :horizon] 1)
                 (assoc-in [:observation-model :class-preference] {1 terminal}))
        list-risk (:controller-score (score (list-action half) opts))
        co-risk (:controller-score (score half opts))]
    (is (< (Math/abs (- list-risk
                        (+ (* 1/2 (Math/log (/ 1/2 0.35)))
                           (* 1/2 (Math/log (/ 1/2 0.05)))))) 1e-9))
    (is (< (Math/abs (- co-risk (Math/log (/ 5.0 2.0)))) 1e-9))
    (is (not= list-risk co-risk))))

(deftest chain-order-equals-list-order
  (let [ids (mapv :id (:precedence action))
        chain (assoc-in action [:construction-receipt :order :descent]
                        (mapv vec (partition 2 1 ids)))
        chain-score (score chain)
        list-score (score (list-action chain))]
    (is (= (:controller-score list-score) (:controller-score chain-score)))
    (is (= (mapv #(select-keys % [:risk :ambiguity :belief])
                 (get-in list-score [:certificate :steps]))
           (mapv #(select-keys % [:risk :ambiguity :belief])
                 (get-in chain-score [:certificate :steps]))))))

(deftest trace-validation-follows-candidate-order
  (let [co-score (score action)
        list-score (score (list-action action))
        honest (trace-record action co-score)
        altered (assoc-in honest
                          [:decision :selection-certificate :node-evaluation-traces
                           0 :evaluations 0 :model :descent]
                          [[:ukrns/reader-run-path :war-machine/state-capture]])
        list-for-non-chain (trace-record action list-score)]
    (is (= :valid (:status (trace/validate-record honest))))
    (is (contains? (mismatch-kinds altered) :model-candidate-mismatch))
    (is (contains? (mismatch-kinds list-for-non-chain) :model-candidate-mismatch))))

(def ^:private first-row
  [:decision :selection-certificate :node-evaluation-traces 0 :evaluations 0 :states 0])

(deftest co-application-rows-are-recomputed-not-trusted
  (let [honest (trace-record action (score action))
        row (get-in honest first-row)
        ;; a row that says nothing happened, with its contribution kept
        ;; consistent with that claim
        idle (assoc-in honest first-row
                       (assoc row :kernel {(:state row) 1}
                              :mass-contribution {(:state row) (:mass row)}))
        short-frontier (update-in honest (conj first-row :frontier) pop)]
    (is (contains? (mismatch-kinds idle) :applied-kernel-mismatch))
    (is (contains? (mismatch-kinds short-frontier) :frontier-mismatch))))

(deftest uncertain-co-application-trace-validates
  (let [half (update action :precedence
                     #(mapv (fn [pattern] (assoc pattern :theta 1/2)) %))
        scored (score half)
        states-per-step (mapv #(count (get-in % [:node-evaluation :states]))
                              (get-in scored [:certificate :steps]))]
    (is (< 1 (apply max states-per-step)) "the belief branches when success is uncertain")
    (is (= :valid (:status (trace/validate-record (trace-record half scored)))))))

(deftest order-naming-a-unit-outside-the-precedence-falls-back-to-the-list
  (let [broken (assoc-in action [:construction-receipt :order :units 0 :pattern]
                         :missing/pattern)
        scored (score broken)]
    (is (= {:order-not-used :units-not-mapped-to-precedence} (:order-use scored)))
    (is (= :first-enabled-union-theta-v1
           (get-in scored [:certificate :steps 0 :node-evaluation :model :semantics])))
    (is (= (:controller-score (score (list-action action)))
           (:controller-score scored)))))
