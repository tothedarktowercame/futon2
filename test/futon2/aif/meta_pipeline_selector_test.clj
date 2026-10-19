(ns futon2.aif.meta-pipeline-selector-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.meta-live-outer-selector :as live]
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
   :graph {:section-status {:lineage {:status :ok}
                            :clusters {:status :ok}
                            :arrows {:status :ok}
                            :held {:status :ok}
                            :patterns {:status :ok}}
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

(defn- complete-field [rows _]
  {:schema :wm/pre-choice-policy-field-v1
   :source-pin (pin "wm://p20-field" "d")
   :coverage {:targets (count rows) :current (count rows)}
   :families (mapv (fn [{:keys [id]}]
                     {:target-id id :status :computed
                      :policies [{:policy-id (str id "/policy-1")}]
                      :failures []}) rows)})

(defn- sealed-menu [field _]
  {:schema :wm/sealed-outer-policy-menu-v1
   :status :sealed
   :field-source-pin (:source-pin field)
   :menu-pin (pin "wm://p20-menu" "e")
   :entries [{:policy-id "fixture/policy"
              :cascade-digest (apply str (repeat 64 "1"))}]})

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

(deftest missing-and-stale-evidence-stay-out-of-comparison
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
  (testing "a failed section is measured while available sections retain nodes"
    (let [partial (assoc-in snapshot [:graph :section-status :patterns]
                            {:status :failed :error :timeout})
          membership (selector/pipeline-membership partial)]
      (is (= :verified (:status (selector/validate-snapshot partial))))
      (is (= :partial (get-in membership [:section-availability :status])))
      (is (= [:patterns] (:unavailable-structural-sections membership)))
      (is (contains? (:ids membership) "M-a")))))

(deftest unavailable-structural-population-is-not-an-observed-empty-map
  (let [unavailable (assoc snapshot :graph
                            {:section-status {:clusters {:status :failed :error :timeout}
                                              :patterns {:status :failed :error :timeout}}
                             :lineage [] :clusters [] :arrows [] :held []
                             :tickets {:items []} :patterns {:edges []}})
        membership (selector/pipeline-membership unavailable)]
    (is (= :verified (:status (selector/validate-snapshot unavailable))))
    (is (= :partial (get-in membership [:section-availability :status])))
    (is (= #{:clusters :patterns}
           (set (:unavailable-structural-sections membership))))
    (is (false? (:membership-complete? membership)))
    (is (empty? (:ids membership)))))

(deftest meta-selection-cannot-use-a-row-from-a-failed-section
  (let [failed (assoc-in snapshot [:graph :section-status :clusters]
                         {:status :failed :error :timeout})
        receipt (selector/select {:snapshot failed
                                  :candidates [(candidate "M-a" 0.2)]})]
    (is (= :candidate-invalid (:reason receipt)))
    (is (= [:not-in-pipeline-cascade]
           (get-in receipt [:details :candidate-errors "M-a"])))
    (is (nil? (:selected receipt)))))

(deftest live-adapter-forms-fields-from-available-sections-only
  (let [tasks [{:id "M-a" :kind :mission :priority 2 :source (pin "M-a.md" "c")}
               {:id "M-b" :kind :mission :priority 8 :source (pin "M-b.md" "d")}]
        field-calls (atom 0)
        unavailable (live/select-live
                     {:tasks tasks :policy-field-config {:authority :fixture}
                      :policy-field-fn (fn [& _] (swap! field-calls inc))
                      :fetch-snapshot #(assoc-in snapshot
                                                 [:graph :section-status :clusters :status]
                                                 :failed)})]
    (is (= :pipeline-target-data-unavailable
           (get-in unavailable [:policy :meta-selection :reason])))
    (is (zero? @field-calls))
    (is (= #{"M-a" "M-b"}
           (set (map :id (:unknown-targets unavailable)))))
    (is (nil? (:action unavailable)))
    (let [field-input (atom nil)
          receipt (live/select-live
                   {:tasks tasks :policy-field-config {:authority :fixture}
                    :policy-field-fn (fn [rows config]
                                       (reset! field-input (mapv :id rows))
                                       (complete-field rows config))
                    :policy-menu-fn sealed-menu
                    :fetch-snapshot #(assoc-in snapshot
                                               [:graph :section-status :agency]
                                               {:status :failed :error :timeout})})]
      (is (= ["M-a" "M-b"] @field-input))
      (is (= :partial
             (get-in receipt [:policy :pipeline-section-availability :status])))
      (is (= :agency
             (get-in receipt [:policy :pipeline-section-availability
                              :unavailable-sections 0 :section])))
      (is (= :outer-policy-evidence-provider-required
             (get-in receipt [:policy :meta-selection :reason])))
      (is (nil? (:action receipt))))))

(deftest complete-union-comparison-makes-missingness-explicit
  (let [a (candidate "M-a" 0.2)
        b (assoc (candidate "M-b" 0.3) :channels
                 {:learned-cost {:value 0.1 :freshness :current
                                 :source (pin "belief.edn" "e")}})
        missing-cost
        (selector/select {:snapshot snapshot :candidates [a b]
                          :comparison :complete-union
                          :missing-channel-cost 1.0})
        pair (first (:pairwise missing-cost))]
    (is (= :selected (:status missing-cost)))
    (is (= "M-b" (:selected missing-cost)))
    (is (= :complete-union (:comparison pair)))
    (is (= [:current-priority-cost] (:missing-right pair)))
    (is (= [:learned-cost] (:missing-left pair)))
    (is (= :missing-channel-cost-required
           (:reason (selector/select {:snapshot snapshot :candidates [a b]
                                      :comparison :complete-union}))))))

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
                                [{:mission "futon2-d/excursion/outer-loop-improvement"}
                                 {:target "futon3c-d/ticket/repair-observation"}]))
        ids (selector/pipeline-node-ids (:graph qualified))]
    (is (every? ids ["M-war-machine-pilot"
                     "E-outer-loop-improvement"
                     "T-repair-observation"]))
    (is (= :selected
           (:status (selector/select
                     {:snapshot qualified
                      :candidates [(candidate "M-war-machine-pilot" 0.2)]}))))))

(deftest ticket-inventory-is-evidence-not-pipeline-membership
  (let [inventory-only (assoc-in snapshot [:graph :tickets :items]
                                 [{:stem "M-off-cascade" :mtime-ms 1}
                                  {:stem "repo-d/excursion/off-cascade"
                                   :mtime-ms 2}])
        ids (selector/pipeline-node-ids (:graph inventory-only))]
    (is (not (contains? ids "M-off-cascade")))
    (is (not (contains? ids "E-off-cascade")))
    (is (= [:not-in-pipeline-cascade]
           (get-in (selector/select
                    {:snapshot inventory-only
                     :candidates [(candidate "M-off-cascade" 0.1)]})
                   [:details :candidate-errors "M-off-cascade"])))))

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

(deftest identity-tie-rule-cannot-override-equal-g
  (let [run (fn [candidates]
              (selector/select {:snapshot snapshot
                                :candidates candidates
                                :tie-breaker :canonical-identity}))
        forward (run [(candidate "M-b" 0.5) (candidate "M-a" 0.5)])
        reverse (run [(candidate "M-a" 0.5) (candidate "M-b" 0.5)])]
    (is (= :refused (:status forward)))
    (is (= :no-unique-task-state-minimum (:reason forward)))
    (is (nil? (:selected forward)))
    (is (= (set (get-in forward [:details :undefeated]))
           (set (get-in reverse [:details :undefeated])))
        "input order is not authority")
    (is (= {:verdict :fail
            :reason :equal-G
            :tie-count 2
            :contract {:module "DarkTower.WarMachine.Holes"
                       :definition "selectionDiscriminates"
                       :lean-rule "chosenByTiebreak iff 1 < tieCount"}}
           (get-in forward [:details :selection-discrimination])))
    (is (= :tie (get-in forward [:details :pairwise 0 :winner]))
        "the receipt preserves the equal-G comparison")))
