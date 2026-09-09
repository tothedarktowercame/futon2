(ns futon2.aif.disposition-risk-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.disposition-risk :as disposition]
            [futon2.aif.efe :as efe]
            [futon2.aif.ruled-outcome-c :as ruled]))

(def state
  {:belief {:x 0.5}
   :observation {:mission-health 0.5}})

(defn kernel [observation]
  (let [preferred? (> (double (get observation :mission-health 0.0)) 0.52)]
    (merge (zipmap (:support ruled/seeded-c) (repeat 0.0))
           (if preferred?
             {:grounded-change 1.0}
             {:agent-unavailable 1.0}))))

(def opts
  {:ruled-outcome-c-enabled? true
   :disposition-kernel kernel
   :seeded-c ruled/seeded-c
   :ambiguity-mode :variance-sum
   :risk-mode :hinge})

(deftest t1-selection-moves-with-disposition-risk
  (let [positive {:type :open-mission :target :m1}
        negative {:type :no-op}
        ranked (efe/rank-actions state [negative positive] opts)]
    (is (= positive (:action (first ranked))))
    (is (< (:G-ruled-outcome-c (first ranked))
           (:G-ruled-outcome-c (second ranked))))))

(deftest t2-absent-seeded-c-refuses-selection
  (doseq [[label absent-opts key-present?]
          [["missing key" (dissoc opts :seeded-c) false]
           ["nil value" (assoc opts :seeded-c nil) true]]]
    (testing label
      (let [ranked (efe/rank-actions state [{:type :no-op}] absent-opts)
            [record :as events] (:disposition-risk-events (meta ranked))]
        (is (= [] ranked) "no policy is chosen or ranked")
        (is (true? (:refused? (meta ranked))))
        (is (= 1 (count events)))
        (is (= :absent (:status record)))
        (is (= :disposition-risk/v1 (:producer-contract record)))
        (is (= :seeded-c-not-supplied (:reason record)))
        (is (true? (:required? record)))
        (is (not (contains? record :value)) "no preference masses are invented")
        (is (= [{:field :seeded-c :key-present? key-present?}]
               (:absent record)))))))

(deftest t3-support-and-named-zeros-are-enforced
  (testing "zero Q mass over every named zero is a finite KL"
    (is (Double/isFinite
         (disposition/disposition-risk {:mission-health 1.0}
                                       kernel ruled/seeded-c))))
  (testing "removing a named zero from support refuses by name"
    (let [missing (first (sort ruled/named-zero-dispositions))
          planted (update ruled/seeded-c :support disj missing)]
      (try
        (disposition/disposition-risk {:mission-health 1.0} kernel planted)
        (is false "planted missing support was accepted")
        (catch clojure.lang.ExceptionInfo e
          (is (= :outcome-outside-disposition-support
                 (:reason (ex-data e))))
          (is (= [missing] (:outside (ex-data e)))))))))

(deftest t5-fold-layer-is-removable-and-nonzero
  (let [action {:type :no-op}
        with-layer (efe/compute-efe state action opts)
        without-layer (efe/compute-efe state action
                                       (assoc opts :ruled-outcome-c-enabled? false))]
    (is (pos? (:G-ruled-outcome-c with-layer)))
    (is (zero? (:G-ruled-outcome-c without-layer)))
    (is (< (Math/abs
            (- (:G-ruled-outcome-c with-layer)
               (- (:controller-score with-layer)
                  (:controller-score without-layer))))
           1.0e-12))))

(def constant-artifact
  {:schema :wm/disposition-kernel-v1
   :source {:ledger "synthetic-shaped-like-cohort" :sha256 "test-fixture"}
   :conditioning {:grain :checkpoint-trajectory}
   :support (vec (sort (:support ruled/seeded-c)))
   :states [{:observation-summary {:checkpoint-trajectory [:selected :closed]}
             :sample-size 2
             :probability (assoc (zipmap (:support ruled/seeded-c) (repeat 0))
                                 :grounded-change 1)}]})

(deftest constant-checkpoint-adapter-contract
  (let [adapter (disposition/constant-checkpoint-kernel constant-artifact)]
    (is (= (adapter {:mission-health 0.0}) (adapter {:mission-health 1.0})))
    (is (= 12 (count (adapter {}))))
    (is (= :open (:observation-model-bridge (meta adapter))))
    (is (= (:source constant-artifact) (:source (meta adapter))))
    (is (< (Math/abs (- (Math/log 2.0)
                       (disposition/disposition-risk {} adapter ruled/seeded-c))) 1.0e-12))
    (is (= (set (keys (adapter {}))) (:support ruled/seeded-c)))))

(deftest constant-checkpoint-adapter-refusals
  (doseq [artifact [(assoc constant-artifact :support [])
                    (assoc constant-artifact :states [])
                    (assoc constant-artifact :support [:grounded-change :grounded-change])
                    (assoc-in constant-artifact [:states 0 :probability :grounded-change] -1)
                    (update-in constant-artifact [:states 0 :probability] dissoc :no-selection)
                    (assoc-in constant-artifact [:states 0 :sample-size] 0)
                    (update constant-artifact :states conj
                            {:sample-size 1
                             :probability (assoc (zipmap (:support ruled/seeded-c) (repeat 0))
                                                 :agent-unavailable 1)})]]
    (try (disposition/constant-checkpoint-kernel artifact)
         (is false "invalid or nonconstant fit accepted")
         (catch clojure.lang.ExceptionInfo e
           (is (true? (:refused? (ex-data e))))
           (is (keyword? (:reason (ex-data e))))))))
