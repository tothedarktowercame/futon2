(ns futon2.aif.meta-pipeline-selector-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.meta-pipeline-selector :as selector]))

(defn pin [path ch] {:path path :sha256 (apply str (repeat 64 ch))})

(def snapshot
  {:schema :wm/pipeline-cascade-snapshot-v1
   :summary-source (pin "cascade-summary.edn" "a")
   :graph-source (pin "cascade-graph.edn" "b")
   :summary {:consistent? true
             :standards {:s1-regenerates true :s2-evidence true
                         :s3-reconstitution true :s4-honest-holes true
                         :s4-evidence {:checked "cascade/hole-target"}
                         :s5-composed true}}
   :graph {:section-status {:clusters {:status :ok} :patterns {:status :ok}}
           :lineage []
           :clusters [{:cluster "c" :mission "M-a"}
                      {:cluster "c" :mission "M-b"}
                      {:cluster "c" :mission "M-c"}]
           :arrows [] :held [] :tickets {:items []} :patterns {:edges []}}})

(defn candidate [id cost]
  {:id id :kind :mission
   :support {:automated-feasibility :supported}
   :channels {:current-priority-cost
              {:value cost :freshness :current
               :source (pin (str id ".edn") "c")}}})

(deftest selects-a-unique-pipeline-node-with-zero-information-gain
  (let [receipt (selector/select {:snapshot snapshot
                                  :candidates [(candidate "M-a" 0.2)
                                               (candidate "M-b" 0.8)]})]
    (is (= :selected (:status receipt)))
    (is (= "M-a" (:selected receipt)))
    (is (= :minimum-pairwise-task-state-G (:reason receipt)))
    (is (= 0.0 (:epistemic-value-nats receipt)))
    (is (= [:current-priority-cost]
           (get-in receipt [:pairwise 0 :shared-channels])))))

(deftest missing-stale-and-partial-evidence-fail-closed
  (testing "no shared current channel is unrankable, not zero"
    (let [other (-> (candidate "M-b" 0.1)
                    (assoc :channels
                           {:different-cost {:value 0.1 :freshness :current
                                             :source (pin "other.edn" "d")}}))
          receipt (selector/select {:snapshot snapshot
                                    :candidates [(candidate "M-a" 0.2) other]})]
      (is (= :shared-current-channel-unavailable (:reason receipt)))))
  (testing "stale values cannot enter comparison"
    (let [stale (assoc-in (candidate "M-a" 0.2)
                          [:channels :current-priority-cost :freshness] :stale)]
      (is (= :candidate-invalid
             (:reason (selector/select {:snapshot snapshot :candidates [stale]}))))))
  (testing "a failed cascade section invalidates the snapshot"
    (let [partial (assoc-in snapshot [:graph :section-status :patterns]
                            {:status :failed :error :timeout})]
      (is (= :pipeline-snapshot-invalid
             (:reason (selector/select {:snapshot partial
                                        :candidates [(candidate "M-a" 0.2)]})))))))

(deftest feasibility-is-support-and-map-membership-is-required
  (let [infeasible (assoc-in (candidate "M-a" 0.0)
                             [:support :automated-feasibility] :infeasible)
        selected (selector/select {:snapshot snapshot
                                   :candidates [infeasible (candidate "M-b" 0.9)]})]
    (is (= "M-b" (:selected selected)))
    (is (= ["M-a"] (:typed-exclusions selected))))
  (let [receipt (selector/select {:snapshot snapshot
                                  :candidates [(candidate "M-not-on-map" 0.0)]})]
    (is (= :candidate-invalid (:reason receipt)))
    (is (= [:not-in-pipeline-cascade]
           (get-in receipt [:details :candidate-errors "M-not-on-map"])))))

(deftest qualified-work-identities-join-canonical-registry-identities
  (let [qualified (-> snapshot
                      (assoc-in [:graph :clusters]
                                [{:cluster "c" :mission "futon3c-d/mission/war-machine-pilot"}])
                      (assoc-in [:graph :lineage]
                                [{:mission "futon2-d/excursion/outer-loop-improvement"}])
                      (assoc-in [:graph :tickets :items]
                                [{:stem "futon3c-d/ticket/repair-observation"}]))
        ids (selector/pipeline-node-ids (:graph qualified))]
    (is (every? ids ["M-war-machine-pilot"
                     "E-outer-loop-improvement"
                     "T-repair-observation"]))
    (is (= :selected
           (:status (selector/select
                     {:snapshot qualified
                      :candidates [(candidate "M-war-machine-pilot" 0.2)]}))))))

(deftest ambiguous-and-malformed-qualified-identities-fail-closed
  (testing "the same canonical id from two authorities is ambiguous"
    (let [colliding (assoc-in snapshot [:graph :lineage]
                              [{:mission "futon2-d/mission/same"}
                               {:mission "futon3c-d/mission/same"}])
          receipt (selector/select {:snapshot colliding
                                    :candidates [(candidate "M-same" 0.2)]})]
      (is (not (contains? (selector/pipeline-node-ids (:graph colliding))
                          "M-same")))
      (is (= :candidate-invalid (:reason receipt)))
      (is (= [:pipeline-identity-collision]
             (get-in receipt [:details :candidate-errors "M-same"])))))
  (testing "a work-looking identity with an extra path component is not guessed"
    (let [malformed (assoc-in snapshot [:graph :lineage]
                              [{:mission "futon3c-d/mission/nested/name"}])
          receipt (selector/select {:snapshot malformed
                                    :candidates [(candidate "M-name" 0.2)]})]
      (is (not (contains? (selector/pipeline-node-ids (:graph malformed))
                          "M-name")))
      (is (= [:not-in-pipeline-cascade]
             (get-in receipt [:details :candidate-errors "M-name"]))))))

(deftest ties-refuse-rather-than-selecting-first
  (let [receipt (selector/select {:snapshot snapshot
                                  :candidates [(candidate "M-a" 0.5)
                                               (candidate "M-b" 0.5)]})]
    (is (= :no-unique-task-state-minimum (:reason receipt)))
    (is (= #{"M-a" "M-b"} (set (get-in receipt [:details :undefeated]))))))
