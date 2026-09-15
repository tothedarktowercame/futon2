(ns futon2.aif.machine-belief-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.edn :as edn]
            [futon2.aif.machine-belief :as mb]
            [futon2.aif.machine-model :as model]
            [futon2.aif.machine-model-test :as model-test]))

(def entity "arxana/stack/futon-v1/leaf/2/2")
(def row {:spawned 1/10 :refined 1/10 :strengthened 2/5 :addressed 1/10
          :falsified 1/10 :foreclosed 1/10 :reopened 1/10})
(def context {:entity/id entity :model {:id "wm" :revision "captured"}
              :state-support mb/state-support :mode :single-entity
              :policy-entities [entity]})

(deftest constructs-contract-belief-input-without-rewriting
  (let [result (mb/belief-state-distribution context {entity row})]
    (is (:ok result))
    (is (= {:mode :single-entity :posteriors {entity row}}
           (:belief-input result)))
    (is (identical? row (get-in result [:belief-input :posteriors entity])))
    (is (= (:model context) (:model result)))
    (is (= {:entity/id entity} (:context result)))
    (is (= mb/state-support (:state-support result)))))

(deftest commissioned-refusals
  (doseq [[label expected c b]
          [[:missing :missing-entity context {}]
           [:deleted :missing-entity context (dissoc {entity row} entity)]
           [:reordered :state-support-order-mismatch
            (update context :state-support #(vec (reverse %))) {entity row}]
           [:invalid-mass :invalid-mass context {entity (assoc row :spawned -1/10 :strengthened 3/5)}]
           [:unnormalised :invalid-mass context {entity (assoc row :spawned 1/5)}]
           [:unnormalised-float :invalid-mass context
            {entity (update (zipmap mb/state-support (repeat (/ 1.0 7.0)))
                            :spawned + 0.001)}]
           [:multi-entity :joint-construction-required
            (assoc context :policy-entities [entity "other"]) {entity row}]]]
    (testing (name label)
      (is (= expected (get-in (mb/belief-state-distribution c b)
                              [:refusal :kind]))))))

(deftest reader-retains-shared-admission-and-support-identity
  (let [result (mb/belief-state-distribution context {entity row})
        admission (:numeric-admission result)
        reordered (into (array-map) (reverse row))
        permuted (assoc row :spawned (:strengthened row) :strengthened (:spawned row))
        other (:numeric-admission (mb/belief-state-distribution context {entity permuted}))]
    (is (= (model/distribution-admission row mb/state-support) admission))
    (is (= row (:values admission)))
    (is (= mb/state-support (:support admission)))
    (is (= (:model context) (:model result)))
    (is (identical? row (get-in result [:belief-input :posteriors entity])))
    (is (= admission (:numeric-admission (mb/belief-state-distribution context {entity reordered}))))
    (is (= (:exact-total admission) (:exact-total other)))
    (is (= (:support admission) (:support other)))
    (is (not= (:values admission) (:values other)))
    (let [changed-support (model/distribution-admission row (vec (reverse mb/state-support)))]
      (is (= (:values admission) (:values changed-support)))
      (is (not= (:support admission) (:support changed-support))))))

(deftest reader-full-boundary-rejects-invalid-masses
  (doseq [[kind r]
          [[:posterior-support-mismatch (assoc (dissoc row :spawned) :foreign 1/10)]
           [:posterior-support-mismatch (assoc row :extra 0)]
           [:invalid-mass (assoc row :spawned -1/10 :refined 3/10)]
           [:invalid-mass (assoc row :spawned Double/NaN)]
           [:invalid-mass (assoc row :spawned Double/POSITIVE_INFINITY)]
           [:invalid-mass (assoc row :spawned Double/NEGATIVE_INFINITY)]
           [:unsupported-numeric-type (assoc row :spawned (java.util.concurrent.atomic.AtomicInteger. 1))]]]
    (is (= kind (get-in (mb/belief-state-distribution context {entity r}) [:refusal :kind])))))

(deftest reader-exposes-approximate-production-row-admission
  (let [cases (edn/read-string (slurp (str model-test/numeric-receipt-root "cases.edn")))
        production (:float-seven cases)
        stored (zipmap mb/state-support (map production [:a :b :c :d :e :f :g]))
        result (mb/belief-state-distribution context {entity stored})
        admission (:numeric-admission result)]
    (is (:ok result))
    (is (identical? stored (get-in result [:belief-input :posteriors entity])))
    (is (= stored (:values admission)))
    (is (= :float-carried (:admission admission)))
    (is (false? (:exactly-normalized? admission)))
    (is (= 36028797018963969/36028797018963968 (:exact-total admission)))
    (is (= 1/36028797018963968 (:exact-deviation admission)))
    (is (= result (edn/read-string (pr-str result))))))

(deftest reader-support-shape-entrypoint
  (doseq [[support kind] [[(conj mb/state-support :spawned) :state-support-order-mismatch]
                         [nil :state-support-order-mismatch]
                         [[] :state-support-order-mismatch]
                         [(set mb/state-support) :state-support-order-mismatch]
                         ;; Sequential equality allows the canonical-order list
                         ;; through the order guard; the shared shape check refuses it.
                         [(apply list mb/state-support) :missing-support]]]
    (is (= kind (get-in (mb/belief-state-distribution
                        (assoc context :state-support support) {entity row}) [:refusal :kind]))))
  (is (:ok (mb/belief-state-distribution context {entity row}))))
