(ns futon2.aif.meta-live-outer-selector-test
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.meta-field-observation :as field]
            [futon2.aif.meta-live-outer-selector :as live]
            [futon2.aif.meta-pipeline-selector :as selector]
            [futon2.aif.outer-target-belief :as outer-belief]
            [futon2.aif.mission-registry :as registry]))

(defn pin [path ch] {:path path :sha256 (apply str (repeat 64 ch))})
(def snapshot
  {:schema :wm/pipeline-cascade-snapshot-v1
   :summary-source (pin "summary" "a") :graph-source (pin "graph" "b")
   :summary {:consistent? true
             :standards {:s1-regenerates true :s2-evidence true
                         :s3-reconstitution true :s4-honest-holes true
                         :s5-composed true}}
   :graph {:section-status {:lineage {:status :ok}
                            :clusters {:status :ok}
                            :arrows {:status :ok}
                            :held {:status :ok}
                            :patterns {:status :ok}}
           :lineage [] :clusters [{:mission "M-a"} {:mission "M-b"}]
           :arrows [] :held []
           :tickets {:items [{:stem "M-a" :mtime-ms 200}
                             {:stem "M-b" :mtime-ms 100}]}
           :patterns {:edges []}}})
(def tasks
  [{:id "M-a" :kind :mission :priority 2 :source (pin "M-a.md" "c")}
   {:id "M-b" :kind :mission :priority 8 :source (pin "M-b.md" "d")}])

(defn- complete-policy-field [task-rows _]
  {:schema :wm/pre-choice-policy-field-v1
   :source-pin (pin "wm://test-policy-menu" "e")
   :coverage {:targets (count task-rows) :current (count task-rows)}
   :families (mapv (fn [{:keys [id]}]
                     {:target-id id :status :computed
                      :policies [{:policy-id (str id "/policy-1")}]
                      :failures []}) task-rows)})

(def selection-as-of "2026-10-22T12:34:56Z")

(defn- frozen-focus
  "A frozen focus every on-map target classes as :focus under, so tests of
  the outer boundary do not depend on the live relation corpus."
  [opts]
  (merge {:selection-as-of selection-as-of
          :focus-inputs {}
          :focus-context {:as-of selection-as-of :status :retained :focus "WM"}
          :focus-classify-fn (fn [_ _ _ target _] {:target target :class :focus})}
         opts))

(defn- select-ordinary [opts]
  (live/select-live (assoc (frozen-focus opts)
                           :policy-field-config {:authority :test-fixture}
                           :policy-field-fn complete-policy-field
                           ;; These tests exercise the outer admission boundary,
                           ;; not executable-cascade scoring.  Stop at the new
                           ;; explicit evidence-provider boundary with a sealed
                           ;; menu-shaped fixture.
                           :policy-menu-fn
                           (fn [field _]
                             {:schema :wm/sealed-outer-policy-menu-v1
                              :status :sealed
                              :field-source-pin (:source-pin field)
                              :menu-pin (pin "wm://test-sealed-menu" "f")
                              :entries [{:policy-id "fixture/policy"
                                         :cascade-digest (apply str (repeat 64 "1"))}]})
                           )))

(def admitted-algorithm
  {:id "A-self-heal" :kind :algorithm :source (pin "A-self-heal.md" "e")
   :repairs-capability :wm-click-completes-with-reviewable-receipts
   :algorithm-admission
   {:injury-observation
    {:status :active
     :capability :wm-click-completes-with-reviewable-receipts
     :injury-verification {:status :verified}}}
   :resource-admission {:required {:ordinary-clicks 1}}})

(deftest verified-injury-routes-meta-to-the-algorithm-arm
  (let [receipt (live/select-live
                 {:tasks (conj tasks admitted-algorithm)
                  :fetch-snapshot (constantly snapshot)})]
    (is (= {:type :run-algorithm :target "A-self-heal"}
           (:action receipt)))
    (is (= "A-self-heal" (get-in receipt [:chosen :id])))
    (is (= :wm-click-completes-with-reviewable-receipts
           (get-in receipt [:algorithm-admission :repairs-capability])))
    (is (= #{"M-a" "M-b"}
           (->> (:excluded receipt)
                (filter #(= :machine-injury-active (:ineligible-reason %)))
                (map :id) set)))))

(deftest unverified-algorithm-admission-is-refused
  (let [receipt (live/select-live
                 {:tasks (conj tasks
                               (assoc-in admitted-algorithm
                                         [:algorithm-admission :injury-observation
                                          :injury-verification :status]
                                         :refused))
                  :fetch-snapshot (constantly snapshot)})]
    (is (= :candidate-invalid
           (get-in receipt [:policy :meta-selection :reason])))
    (is (= [:algorithm-injury-admission-invalid]
           (get-in receipt [:policy :meta-selection :details
                            :candidate-errors "A-self-heal"])))))

(deftest production-adapter-refuses-a-self-signed-meta-field
  (let [forged {:schema :wm/meta-field-observation-v1
                :status :observed
                :rows [admitted-algorithm]
                :source-pin (pin "wm://meta-field-observation-v1" "f")}
        receipt (live/selector
                 {:tasks tasks
                  :selection-as-of "2026-10-22T12:34:56Z"
                  :meta-field-observation forged
                  :meta-field-source-pin (pin "wm://different-authority" "a")})]
    (is (= :meta-field-observation-unverified
           (get-in receipt [:policy :meta-selection :reason])))
    (is (nil? (:action receipt)))))

(deftest production-selector-refuses-without-selection-as-of
  ;; Only the production adapter is fail-closed here.  Direct select-live
  ;; calls remain available to controlled SUT fixtures that do not model a
  ;; live click clock.
  (let [receipt (live/selector {:tasks tasks})]
    (is (= :selection-as-of-required
           (get-in receipt [:policy :meta-selection :reason])))
    (is (= {:absent :selection-as-of-required} (:chosen receipt)))))

(deftest live-receipt-retains-the-supplied-selection-as-of
  (let [selection-as-of "2026-10-22T12:34:56Z"
        receipt (live/select-live {:tasks []
                                   :selection-as-of selection-as-of
                                   :fetch-snapshot (constantly snapshot)})]
    (is (= selection-as-of (:selection-as-of receipt)))))

(defn- authorized-snapshot [snapshot]
  (let [agency (or (:agency snapshot) {:agents {}})
        summary-bytes (json/generate-string (:summary snapshot))
        graph-bytes (json/generate-string (:graph snapshot))
        agency-bytes (json/generate-string agency)
        source (fn [path bytes]
                 {:path path
                  :sha256 (field/sha256 (.getBytes bytes "UTF-8"))})]
    (assoc snapshot
           :agency agency
           :summary-source (source "summary" summary-bytes)
           :graph-source (source "graph" graph-bytes)
           :agency-source (source "agency" agency-bytes)
           :source-bytes {:summary summary-bytes
                          :graph graph-bytes
                          :agency agency-bytes})))

(deftest live-adapter-stops-at-the-complete-policy-menu
  (let [receipt (select-ordinary {:tasks tasks :fetch-snapshot (constantly snapshot)})]
    (is (= :wm/outer-task-selection-v1 (:schema receipt)))
    (is (= :cascade-live-policy-menu-v1 (get-in receipt [:policy :kind])))
    (is (= :refused (get-in receipt [:policy :status])))
    (is (= (pin "wm://test-policy-menu" "e")
           (get-in receipt [:policy :field-source-pin])))
    (is (nil? (:action receipt)))
    (is (= {:absent :deterministic-meta-policy} (:draw receipt)))
    (is (= {:status :retired :consumed false}
           (get-in receipt [:policy :legacy-target-shadow])))
    (is (= :outer-policy-evidence-provider-required
           (get-in receipt [:q8-frontier :reason])))))

(deftest learned-outcomes-run-in-shadow-without-changing-live-action
  (let [belief (outer-belief/build
                {:prior (outer-belief/prior 1 :operator-provisional)
                 :dataset {:status :produced
                           :source-pin (pin "run-manifest" "9")
                           :rows [{:run/id "r1" :click/id "c1"
                                   :task {:id "M-a"}
                                   :terminal-outcome {:class :grounded-progress}
                                   :selected-want-effects
                                   {:status :present
                                    :counts {:reached 0 :progressed 0
                                             :blocked 0 :untouched 1}}
                                   :collateral-progress {:status :present}
                                   :blocker {:status :absent}}]}})
        receipt (select-ordinary
                 {:tasks tasks :fetch-snapshot (constantly snapshot)
                  :outer-learning
                  {:mode :shadow :belief belief
                   :preference-authority :operator-provisional
                   :preference-transform
                   {:kind :softmax-negative-cost :inverse-temperature 1.0}
                   :controller {:missing-channel-cost 1.0
                                :recent-selection-cost 0.25
                                :continuation-credit 0.35}
                   :preference-costs
                   {:closure 0.0 :selected-want-progress 0.2
                    :grounded-collateral-progress 0.5 :typed-blocker 0.9
                    :abstention-or-failure 1.0}}})]
    (is (nil? (:action receipt)))
    (is (= :refused (get-in receipt [:policy :status])))
    (is (= {:status :retired :consumed false}
           (get-in receipt [:policy :legacy-target-shadow])))
    (is (nil? (get-in receipt [:outer-learning :scores])))))

(deftest unavailable-learning-state-is-a-typed-certificate-refusal
  (let [receipt (live/select-live
                 {:tasks tasks :fetch-snapshot (constantly snapshot)
                  :outer-learning
                  {:mode :shadow
                   :belief {:status :refused :reason :manifest-unavailable}
                   :controller {:missing-channel-cost 1.0
                                :re-arm-condition "restore manifest"}}})]
    (is (= :refused (get-in receipt [:outer-learning :status])))
    (is (= :outer-learning-state-unavailable
           (get-in receipt [:outer-learning :reason])))
    (is (= "restore manifest"
           (get-in receipt [:outer-learning :re-arm-condition])))))

(deftest admitted-live-mode-uses-the-learned-selection
  (let [prior (outer-belief/prior 1 :operator)
        belief (outer-belief/build
                {:prior prior
                 :dataset {:status :produced :source-pin (pin "runs" "6")
                           :rows [{:run/id "r" :click/id "c" :task {:id "M-a"}
                                   :terminal-outcome {:class :grounded-progress}
                                   :selected-want-effects
                                   {:status :present
                                    :counts {:reached 0 :progressed 0
                                             :blocked 0 :untouched 1}}
                                   :collateral-progress {:status :present}
                                   :blocker {:status :absent}}]}})
        receipt
        (select-ordinary
         {:tasks tasks :fetch-snapshot (constantly snapshot)
          :outer-learning
          {:mode :live :belief belief :preference-authority :operator
           :preference-transform
           {:kind :softmax-negative-cost :inverse-temperature 1.0}
           :preference-costs
           {:closure 0.0 :selected-want-progress 0.2
            :grounded-collateral-progress 0.5 :typed-blocker 0.9
            :abstention-or-failure 1.0}
           :controller {:missing-channel-cost 1.0
                        :recent-selection-cost 0.5
                        :continuation-credit 0.7}}})]
    (is (nil? (:action receipt)))
    (is (= :refused (get-in receipt [:policy :status])))
    (is (= {:status :retired :consumed false}
           (get-in receipt [:policy :legacy-target-shadow])))
    (is (nil? (get-in receipt [:outer-learning :scores])))))

(deftest injury-score-is-advisory-until-an-engineering-prior-conversion-is-declared
  (let [algorithm {:id "A-heal" :kind :algorithm :source (pin "A" "f")
                   :repairs-capability :wm-click-completes-with-reviewable-receipts
                   :algorithm-admission
                   {:injury-observation {:status :active
                    :capability :wm-click-completes-with-reviewable-receipts
                    :injury-verification {:status :verified}}}
                   :resource-admission {:required {:ordinary-clicks 1}}}
        belief (outer-belief/build
                {:prior (outer-belief/prior 1 :operator)
                 :dataset {:status :produced :source-pin (pin "runs" "6") :rows []}})
        learning (fn [mode costs]
                   {:mode mode :belief belief :preference-authority :operator
                    :preference-transform {:kind :softmax-negative-cost :inverse-temperature 1.0}
                    :preference-costs costs
                    :controller {:missing-channel-cost 1.0 :resource-weight 2.0}})
        select (fn [outer-learning]
                 (live/select-live {:tasks (conj tasks algorithm)
                                    :fetch-snapshot (constantly snapshot)
                                    :outer-learning outer-learning}))]
    (doseq [mode [:live :shadow]]
      (let [receipt (select (learning mode
                                      {:closure 0.0 :selected-want-progress 0.2
                                       :grounded-collateral-progress 0.5 :typed-blocker 0.9
                                       :abstention-or-failure 1.0}))]
        (is (= "A-heal" (get-in receipt [:action :target])))
        (is (= :advisory (get-in receipt [:outer-learning :status])))
        (is (= mode (get-in receipt [:outer-learning :mode])))
        (is (false? (get-in receipt [:outer-learning :scores-consumed])))
        (is (nil? (get-in receipt [:outer-learning :consumption-path])))
        (is (= :policy-prior-conversion-unresolved
               (get-in receipt [:outer-learning :injury-score-comparison :reason])))
        (is (= :machine-injury-repair-cost
               (get-in receipt [:outer-learning :injury-score-comparison :selection-basis])))))
    (let [receipt (select (learning :live {}))]
      (is (= "A-heal" (get-in receipt [:action :target]))
          "score absence is advisory; the baseline injury selection remains usable")
      (is (= :advisory (get-in receipt [:outer-learning :status])))
      (is (= {:refused 1} (get-in receipt [:outer-learning :score-statuses])))
      (is (false? (get-in receipt [:outer-learning :scores-consumed]))))))

(deftest continuation-evidence-can-justify-reselecting-a-recent-target
  (let [belief (outer-belief/build
                {:prior (outer-belief/prior 1 :operator-provisional)
                 :dataset {:status :produced :source-pin (pin "runs" "8")
                           :rows [{:run/id "r1" :click/id "c1" :task {:id "M-a"}
                                   :terminal-outcome {:class :grounded-progress}
                                   :selected-want-effects
                                   {:status :present
                                    :counts {:reached 0 :progressed 0
                                             :blocked 0 :untouched 1}}
                                   :collateral-progress {:status :present}
                                   :blocker {:status :absent}}]}})
        base {:mode :shadow :belief belief
              :preference-authority :operator
              :preference-transform
              {:kind :softmax-negative-cost :inverse-temperature 1.0}
              :preference-costs
              {:closure 0.0 :selected-want-progress 0.2
               :grounded-collateral-progress 0.5 :typed-blocker 0.9
               :abstention-or-failure 1.0}
              :controller {:missing-channel-cost 1.0
                           :recent-selection-cost 0.5
                           :continuation-credit 0.7}}
        equal-candidates
        (fn [task-rows _]
          (mapv (fn [{:keys [id kind]}]
                  {:id id :kind kind
                   :support {:automated-feasibility :supported}
                   :channels {:equal-cost {:value 0.5 :freshness :current
                                           :source (pin "equal" "7")}}})
                task-rows))
        without (select-ordinary {:tasks tasks :fetch-snapshot (constantly snapshot)
                                   :candidate-fn equal-candidates
                                   :outer-learning base})
        continuation {:status :present :next-step "Complete the next criterion"
                      :acceptance-condition "Pinned criterion becomes observed"
                      :evidence {:path "M-a.md" :sha256 (apply str (repeat 64 "a"))}}
        with (select-ordinary
              {:tasks tasks :fetch-snapshot (constantly snapshot)
               :candidate-fn equal-candidates
               :outer-learning (assoc-in base [:controller :continuations "M-a"]
                                         continuation)})]
    (is (nil? (:action without)))
    (is (nil? (:action with)))
    (is (= (get-in without [:policy :field-source-pin])
           (get-in with [:policy :field-source-pin])))
    (is (= {:status :retired :consumed false}
           (get-in with [:policy :legacy-target-shadow])))))

(deftest ordinary-policy-menu-never-invokes-target-candidate-selection
  (testing "missing is absence"
    (let [calls (atom 0)
          receipt (select-ordinary {:tasks (mapv #(dissoc % :priority) tasks)
                                     :fetch-snapshot (constantly snapshot)
                                     :candidate-fn
                                     (fn [task-rows _]
                                       (swap! calls inc)
                                       (mapv (fn [{:keys [id kind]}]
                                               {:id id :kind kind
                                                :support {:automated-feasibility :unknown}
                                                :channels {}})
                                             task-rows))})]
      (is (zero? @calls))
      (is (= :refused (get-in receipt [:policy :status])))
      (is (= 2 (count (:support receipt))))
      (is (nil? (:action receipt)))))
  (testing "target equality cannot decide before the policy port"
    (let [equal-snapshot (assoc-in snapshot [:graph :tickets :items 1 :mtime-ms] 200)
          receipt (select-ordinary {:tasks (mapv #(assoc % :priority 1) tasks)
                                     :fetch-snapshot (constantly equal-snapshot)})]
      (is (= :refused (get-in receipt [:policy :status])))
      (is (nil? (:action receipt)))
      (is (= {:status :retired :invoked false :configured true}
             (get-in receipt [:policy :target-candidate-selector]))))))

(deftest current-pipeline-centrality-discriminates-with-pinned-reasons
  (let [central (-> snapshot
                    (assoc-in [:graph :tickets :items]
                              [{:stem "M-a" :mtime-ms 100}
                               {:stem "M-b" :mtime-ms 100}])
                    (assoc-in [:graph :patterns :edges]
                              [{:mission "repo-d/mission/a" :pattern "p/x"}]))
        candidates (live/task-state-candidates
                    (mapv #(dissoc % :priority) tasks) central)
        receipt (selector/select {:snapshot central :candidates candidates})
        a-channel (get-in candidates [0 :channels
                                      :pipeline-structural-centrality-cost])]
    (is (= "M-a" (:selected receipt)))
    (is (= (/ 1.0 3.0) (:value a-channel)))
    (is (= 2 (get-in a-channel [:observation :value])))
    (is (= (:graph-source central) (:source a-channel)))
    (is (= 0.0 (:epistemic-value-nats receipt)))))

(deftest complete-ticket-inventory-cannot-enlarge-live-field
  (let [with-inventory-only
        (update-in snapshot [:graph :tickets :items]
                   conj {:stem "M-inventory-only" :mtime-ms 300})
        receipt (select-ordinary
                 {:tasks (conj tasks
                               {:id "M-inventory-only" :kind :mission
                                :priority 0
                                :source (pin "M-inventory-only.md" "e")})
                  :fetch-snapshot (constantly with-inventory-only)})]
    (is (= ["M-a" "M-b"] (mapv :id (:support receipt))))
    (is (= :pipeline/not-on-current-map
           (->> (:excluded receipt)
                (filter #(= "M-inventory-only" (:id %)))
                first :ineligible-reason)))
    (is (= :refused (get-in receipt [:policy :status])))
    (is (nil? (:action receipt)))))

(deftest conflicting-mission-standing-fails-closed-with-exact-authority
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                      "mission-standing-conflict"
                      (make-array java.nio.file.attribute.FileAttribute 0)))
        path (.getAbsolutePath (io/file dir "M-conflict.md"))
        text (str "# Mission: M-conflict\n\n"
                  "## Acceptance checklist\n\n"
                  "- [ ] Record the manual walk-through.\n\n"
                  "**Final lifecycle stamp**: **POC COMPLETE — shipped.**\n")
        _ (spit path text)
        task {:id "M-conflict" :kind :mission :priority 1
              :source {:path path
                       :sha256 (field/sha256 (.getBytes text "UTF-8"))}}
        conflict-snapshot (assoc-in snapshot [:graph :clusters]
                                    [{:mission "M-conflict"}])
        receipt (live/select-live {:tasks [task]
                                   :fetch-snapshot (constantly conflict-snapshot)})
        excluded (first (:excluded receipt))
        evidence (:ineligibility-evidence excluded)]
    (is (empty? (:support receipt)))
    (is (= :mission-standing-conflict (:ineligible-reason excluded)))
    (is (= (:source task) (:source evidence)))
    (is (= [{:kind :terminal-lifecycle-declaration :line 7
             :text "**Final lifecycle stamp**: **POC COMPLETE — shipped.**"}
            {:kind :unchecked-acceptance :line 5
             :text "- [ ] Record the manual walk-through."
             :heading {:line 3 :text "## Acceptance checklist"}}]
           (:declarations evidence)))))

(deftest current-self-documenting-mission-exposes-standing-conflict
  (let [path "/home/joe/code/futon7/holes/M-self-documenting-stack.md"
        text (slurp path)
        source {:path path :sha256 (field/sha256 (.getBytes text "UTF-8"))}
        observation (registry/mission-standing-observation
                     {:id "M-self-documenting-stack" :kind :mission
                      :source source})]
    (is (= :conflict (:status observation)))
    (is (= source (:source observation)))
    (is (some #(= "- [ ] A recorded manual browser walk-through shows the LC1 mission-search surface returning and opening ranked mission results."
                  (:text %))
              (:open-acceptance-declarations observation)))
    (is (some #(str/includes? (:text %) "M-self-documenting-stack POC COMPLETE")
              (:terminal-declarations observation)))))

(deftest mutated-mission-standing-source-is-refused-not-reinterpreted
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                      "mission-standing-mutation"
                      (make-array java.nio.file.attribute.FileAttribute 0)))
        path (.getAbsolutePath (io/file dir "M-mutated.md"))
        original "# Mission\n\n## Acceptance\n- [ ] walk through it\n"
        _ (spit path original)
        task {:id "M-mutated" :kind :mission
              :source {:path path
                       :sha256 (field/sha256 (.getBytes original "UTF-8"))}}
        _ (spit path (str original "\n**Final lifecycle stamp**: COMPLETE\n"))
        receipt (live/select-live
                 {:tasks [task]
                  :fetch-snapshot
                  #(assoc-in snapshot [:graph :clusters]
                             [{:mission "M-mutated"}])})]
    (is (empty? (:support receipt)))
    (is (= :mission-standing-source-mismatch
           (get-in receipt [:excluded 0 :ineligible-reason])))
    (is (= (:source task)
           (get-in receipt [:excluded 0 :ineligibility-evidence :source])))))

(deftest task-state-channels-refuse-stale-pins-and-preserve-absence
  (let [candidate (first (live/task-state-candidates [(dissoc (first tasks) :priority)]
                                                      snapshot))
        repinned (assoc snapshot :graph-source (pin "graph-new" "f"))
        receipt (selector/select {:snapshot repinned :candidates [candidate]})]
    (is (= :candidate-invalid (:reason receipt)))
    (is (some #{:task-state-source-mismatch}
              (get-in receipt [:details :candidate-errors "M-a"])))
    (is (not (contains? (:channels candidate) :declared-priority-cost)))
    (is (some #{:declared-priority-cost} (:unsupported-channels candidate)))))

(deftest unsupported-kind-channels-are-reported-not-zeroed
  (let [rows [{:id "E-no-surface" :kind :excursion :source (pin "e" "1")}
              {:id "T-no-surface" :kind :ticket :source (pin "t" "2")}
              {:id "A-no-surface" :kind :algorithm :source (pin "a" "3")}]
        candidates (live/task-state-candidates rows snapshot)]
    (doseq [candidate candidates]
      (is (= {} (:channels candidate)))
      (is (= #{:declared-priority-cost :pipeline-structural-centrality-cost
               :pipeline-freshness-cost}
             (set (:unsupported-channels candidate)))))))

(deftest unavailable-structural-section-measures-no-data-without-inventing-targets
  (let [field-calls (atom 0)
        receipt (live/select-live
                 {:tasks tasks
                  :policy-field-config {}
                  :policy-field-fn (fn [& _] (swap! field-calls inc))
                  :fetch-snapshot #(assoc-in snapshot
                                             [:graph :section-status :clusters :status]
                                             :failed)})]
    (is (= :pipeline-target-data-unavailable
           (get-in receipt [:policy :meta-selection :reason])))
    (is (empty? (:support receipt)))
    (is (= #{{:id "M-a" :ineligible-reason :pipeline/membership-unavailable}
             {:id "M-b" :ineligible-reason :pipeline/membership-unavailable}}
           (set (map #(select-keys % [:id :ineligible-reason])
                     (:unknown-targets receipt)))))
    (is (= [:clusters]
           (get-in receipt [:policy :pipeline-membership
                            :unavailable-structural-sections])))
    (is (zero? @field-calls))
    (is (nil? (:action receipt)))))

(deftest omitted-structural-status-keeps-stale-rows-unknown
  (let [field-calls (atom 0)
        stale-task {:id "M-stale" :kind :mission :priority 2
                    :source (pin "M-stale.md" "e")}
        receipt (live/select-live
                 {:tasks [stale-task]
                  :policy-field-config {}
                  :policy-field-fn (fn [& _] (swap! field-calls inc))
                  :fetch-snapshot
                  #(assoc snapshot :graph
                          {:section-status {:agency {:status :ok}}
                           :lineage [] :clusters [{:mission "M-stale"}]
                           :arrows [] :held [] :tickets {:items []}
                           :patterns {:edges []}})})]
    (is (= :pipeline-target-data-unavailable
           (get-in receipt [:policy :meta-selection :reason])))
    (is (empty? (:support receipt)))
    (is (= [{:id "M-stale"
             :eligibility-status :unknown
             :ineligible-reason :pipeline/membership-unavailable}]
           (mapv #(select-keys % [:id :eligibility-status :ineligible-reason])
                 (:unknown-targets receipt))))
    (is (= #{:lineage :clusters :arrows :held :patterns}
           (set (get-in receipt [:policy :pipeline-membership
                                 :unknown-structural-sections]))))
    (is (zero? @field-calls))
    (is (nil? (:action receipt)))))

(deftest explicit-nil-constructor-injections-use-production-defaults
  (let [receipt (live/select-live
                 (frozen-focus
                  {:tasks tasks
                   :policy-field-config {:authority :test-fixture}
                   :policy-field-fn nil
                   :policy-menu-fn nil
                   :fetch-snapshot (constantly snapshot)}))]
    (is (= :refused (get-in receipt [:policy :status])))
    ;; :no-canonically-bound-policy is the production default menu's own
    ;; refusal (family_selection sealed-pre-choice-menu, empty entries), so
    ;; it shows the nil constructor fell back to the default.
    (is (contains? #{:incomplete-policy-menu-coverage
                     :no-canonically-bound-policy
                     :outer-policy-evidence-provider-required}
                   (get-in receipt [:policy :meta-selection :reason])))
    (is (nil? (:action receipt)))))

(deftest off-map-tasks-are-accounted-as-typed-exclusions
  (let [off-map {:id "M-z" :kind :mission :priority 0
                 :source (pin "M-z.md" "e")}
        receipt (select-ordinary {:tasks (conj tasks off-map)
                                   :fetch-snapshot (constantly snapshot)})]
    (is (= :refused (get-in receipt [:policy :status])))
    (is (nil? (:action receipt)))
    (is (= ["M-z"] (mapv :id (:excluded receipt))))
    (is (= :pipeline/not-on-current-map
           (get-in receipt [:excluded 0 :ineligible-reason])))
    (is (= 3 (+ (count (:support receipt)) (count (:excluded receipt)))))))

(defn- repair-ticket [dir suffix target parent]
  (let [finding-id (str "repair-occ-" suffix)
        finding-path (.getAbsolutePath (io/file dir (str finding-id ".edn")))
        finding-text (pr-str {:repair/id finding-id :repair/status :open
                              :repair/class :environmental-hold
                              :failure-kind :agent-unavailable
                              :failure-stage :agent-readiness :target target})
        finding-sha (field/sha256 (.getBytes finding-text "UTF-8"))
        ticket-id (str "T-" finding-id)
        ticket-path (.getAbsolutePath (io/file dir (str ticket-id ".md")))
        ticket-text (str "# Repair\n\n**Status:** OPEN\n\n"
                         (when parent (str "Parent: " parent "\n\n"))
                         "## Provenance\n\nFinding: [" finding-id "](" finding-path ")\n\n"
                         "Finding SHA-256: `" finding-sha "`\n")]
    (spit finding-path finding-text)
    (spit ticket-path ticket-text)
    {:task {:id ticket-id :kind :ticket :path ticket-path :parent parent
            :source {:path ticket-path
                     :sha256 (field/sha256 (.getBytes ticket-text "UTF-8"))}}
     :finding-path finding-path}))

(deftest repair-findings-are-root-observations-not-competing-tasks
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                      "meta-repair-observation"
                      (make-array java.nio.file.attribute.FileAttribute 0)))
        suffix-a (apply str (repeat 64 "1"))
        suffix-b (apply str (repeat 64 "2"))
        a (repair-ticket dir suffix-a "M-a" "M-a")
        b (repair-ticket dir suffix-b "M-a" "M-a")
        run #(live/select-live {:tasks (into tasks [(:task a) (:task b)])
                                :fetch-snapshot (constantly snapshot)})
        first-receipt (run)
        root (first (:support first-receipt))]
    (is (= ["M-a" "M-b"] (mapv :id (:support first-receipt))))
    (is (= [(str "T-repair-occ-" suffix-a) (str "T-repair-occ-" suffix-b)]
           (mapv :id (:repair-observations root))))
    (is (every? #(= :repair-finding/attached-to-root (:ineligible-reason %))
                (:excluded first-receipt)))
    (testing "a finding rewrite changes evidence but cannot promote a ticket"
      (spit (:finding-path a) "{:repair/id \"changed\"}")
      (let [replayed (run)]
        (is (= ["M-a" "M-b"] (mapv :id (:support replayed))))
        (is (not-any? #(re-matches #"T-repair-occ-.*" (:id %))
                      (:support replayed)))))))

(deftest unresolved-repair-roots-fail-closed-with-evidence
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                      "meta-repair-unresolved"
                      (make-array java.nio.file.attribute.FileAttribute 0)))
        missing (repair-ticket dir (apply str (repeat 64 "3")) nil nil)
        ambiguous (repair-ticket dir (apply str (repeat 64 "4")) "M-a" "M-b")
        receipt (live/select-live
                 {:tasks (into tasks [(:task missing) (:task ambiguous)])
                  :fetch-snapshot (constantly snapshot)})
        reasons (set (map :ineligible-reason (:excluded receipt)))]
    ;; a nil target is the machine-owned class since the 2026-10-25 split
    (is (= #{:repair-finding/machine-owned :repair-finding/root-ambiguous}
           reasons))
    (is (every? #(get-in % [:ineligibility-evidence :ticket-source :sha256])
                (:excluded receipt)))
    (is (= ["M-a" "M-b"] (mapv :id (:support receipt))))))

(deftest root-missing-splits-into-three-typed-reasons
  "The old single :repair-finding/root-missing covered three situations
  (Joe, 2026-10-25).  Each class keeps its ticket excluded — the excluded
  set is identical, only the reason is typed.  A machine-owned row records
  the finding's failure/hold kind alongside."
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                      "meta-repair-split"
                      (make-array java.nio.file.attribute.FileAttribute 0)))
        nil-target (repair-ticket dir (apply str (repeat 64 "5")) nil nil)
        agent-target (repair-ticket dir (apply str (repeat 64 "6"))
                                     "A-self-heal" nil)
        absent-target (repair-ticket dir (apply str (repeat 64 "7"))
                                     {:absent :outer-task-not-selected} nil)
        unresolved-target (repair-ticket dir (apply str (repeat 64 "8"))
                                         "repair-attempt-001" nil)
        receipt (live/select-live
                 {:tasks (into tasks [(:task nil-target) (:task agent-target)
                                      (:task absent-target)
                                      (:task unresolved-target)])
                  :fetch-snapshot (constantly snapshot)})
        by-id (into {} (map (juxt :id identity)) (:excluded receipt))]
    (is (= 4 (count (:excluded receipt)))
        "exactly the four repair tickets are excluded, same set as before")
    (is (= ["M-a" "M-b"] (mapv :id (:support receipt)))
        "no repair ticket enters support")
    (is (= :repair-finding/machine-owned
           (:ineligible-reason (get by-id (:id (:task nil-target))))))
    (is (= :repair-finding/machine-owned
           (:ineligible-reason (get by-id (:id (:task agent-target))))))
    (is (= :repair-finding/no-task-selected
           (:ineligible-reason (get by-id (:id (:task absent-target))))))
    (is (= :repair-finding/root-unresolved
           (:ineligible-reason (get by-id (:id (:task unresolved-target))))))
    (is (= :agent-unavailable
           (get-in (get by-id (:id (:task nil-target)))
                   [:ineligibility-evidence :failure-kind]))
        "machine-owned rows carry the finding's failure kind")
    (is (= :environmental-hold
           (get-in (get by-id (:id (:task nil-target)))
                   [:ineligibility-evidence :repair-class]))
        "machine-owned rows carry the hold/failure class")))

(defn- git! [dir & args]
  (let [{:keys [exit out err]} (apply shell/sh "git" "-C" (.getAbsolutePath dir) args)]
    (when-not (zero? exit) (throw (ex-info err {:args args})))
    out))

(defn- committed-task [repo id agent-id]
  (let [path (.getAbsolutePath (io/file repo (str id ".md")))
        text (str "# " id "\n")
        message (if agent-id
                  (str "write " id "\n\nAgent-Id: " agent-id
                       "\nAgency-Job: invoke-test\nDispatched-By: test-owner\n")
                  (str "write " id))]
    (spit path text)
    (git! repo "add" (str id ".md"))
    (git! repo "commit" "-m" message)
    {:id id :kind :mission :priority 2 :path path
     :source {:path path :sha256 (field/sha256 (.getBytes text "UTF-8"))}}))

(deftest active-ownership-and-exact-last-touch-are-distinct
  (let [repo (.toFile (java.nio.file.Files/createTempDirectory
                       "meta-attribution"
                       (make-array java.nio.file.attribute.FileAttribute 0)))]
    (git! repo "init")
    (git! repo "config" "user.email" "test@example.invalid")
    (git! repo "config" "user.name" "Test")
    (let [wm (committed-task repo "M-a" "wm-author")
          other (committed-task repo "M-b" "codex-18")
          agency-snapshot (assoc snapshot
                                 :agency-source (pin "agency" "e")
                                 :agency {:agents
                                          {:codex-9 {:mission-id "M-a"
                                                     :session-id "live-session"
                                                     :status :idle}}})
          held (live/select-live {:tasks [wm other]
                                  :fetch-snapshot (constantly agency-snapshot)})
          held-row (first (:excluded held))]
      (is (= ["M-b"] (mapv :id (:support held))))
      (is (= :ownership/actively-held (:ineligible-reason held-row)))
      (is (= "codex-9" (get-in held-row [:ownership :owner :agent-id])))
      (is (= :war-machine-authored (get-in held-row [:last-touch :state])))
      (is (= :agent-authored (get-in held [:support 0 :last-touch :state])))
      (testing "a historical agent trailer does not assert current ownership"
        (let [stale (live/select-live
                     {:tasks [other]
                      :fetch-snapshot #(assoc snapshot
                                              :agency-source (pin "agency" "e")
                                              :agency {:agents {}})})]
          (is (= :unowned (get-in stale [:support 0 :ownership :state])))
          (is (= :agent-authored (get-in stale [:support 0 :last-touch :state]))))))))

(deftest missing-commit-provenance-is-unknown-and-source-mutation-is-refused
  (let [repo (.toFile (java.nio.file.Files/createTempDirectory
                       "meta-attribution-unknown"
                       (make-array java.nio.file.attribute.FileAttribute 0)))]
    (git! repo "init")
    (git! repo "config" "user.email" "test@example.invalid")
    (git! repo "config" "user.name" "Test")
    (let [missing (committed-task repo "M-a" nil)
          mismatch (committed-task repo "M-b" "codex-18")
          agency-snapshot (assoc snapshot :agency-source (pin "agency" "e")
                                 :agency {:agents {}})]
      (spit (:path mismatch) "# changed after commit\n")
      (let [receipt (live/select-live {:tasks [missing mismatch]
                                       :fetch-snapshot (constantly agency-snapshot)})
            by-id (into {} (map (juxt :id identity)) (:support receipt))
            excluded-by-id (into {} (map (juxt :id identity)) (:excluded receipt))]
        (is (= :commit-trailers-missing-or-ambiguous
               (get-in by-id ["M-a" :last-touch :reason])))
        (is (= :mission-standing-source-mismatch
               (get-in excluded-by-id ["M-b" :ineligible-reason])))
        (is (every? #(= :unknown (get-in % [:last-touch :state]))
                    (:support receipt)))))))

(deftest browser-projection-preserves-order-and-census-without-proof-bulk
  (let [receipt (live/select-live {:tasks (conj tasks
                                                {:id "M-z" :kind :mission
                                                 :priority 0
                                                 :source (pin "M-z.md" "e")})
                                   :fetch-snapshot (constantly snapshot)})
        preview (live/browser-receipt receipt)]
    (is (= (mapv :id (:support receipt)) (mapv :id (:support preview))))
    (is (= (mapv :id (:excluded receipt)) (mapv :id (:excluded preview))))
    (is (= (get-in receipt [:policy :meta-selection :ranking])
           (get-in preview [:policy :meta-selection :ranking])))
    (is (nil? (get-in preview [:policy :meta-selection :pairwise])))
    (is (nil? (get-in preview [:excluded 0 :ineligibility-evidence])))))

(deftest fixture-composition-is-structurally-bounded
  (let [inventory-only-task {:id "M-inventory-only" :kind :mission
                             :priority 0
                             :source (pin "M-inventory-only.md" "e")}
        snapshot (update-in snapshot [:graph :tickets :items]
                            conj {:stem "M-inventory-only" :mtime-ms 300})
        tasks (conj tasks inventory-only-task)
        nodes (selector/pipeline-node-ids (:graph snapshot))
        inventory-ids (set (keep (comp selector/canonical-work-id :stem)
                                 (get-in snapshot [:graph :tickets :items])))
        inventory-only (some #(when (and (contains? inventory-ids (:id %))
                                         (not (contains? nodes (:id %)))) %)
                             tasks)
        started (System/nanoTime)
        receipt (live/select-live {:tasks tasks
                                   :fetch-snapshot (constantly snapshot)})
        elapsed-ms (quot (- (System/nanoTime) started) 1000000)
        support-ids (set (map :id (:support receipt)))
        excluded-by-id (into {} (map (juxt :id identity)) (:excluded receipt))]
    (is inventory-only "current inventory contains a registry file off the cascade")
    (is (every? nodes support-ids))
    (is (= :pipeline/not-on-current-map
           (get-in excluded-by-id [(:id inventory-only) :ineligible-reason])))
    (is (< (count (:support receipt)) (count inventory-ids)))
    (is (< elapsed-ms 30000)
        (str "bounded browser composition took " elapsed-ms "ms"))))

(deftest production-vector-retains-decision-authority-without-metadata
  (let [repo (.toFile (java.nio.file.Files/createTempDirectory
                       "meta-production-vector"
                       (make-array java.nio.file.attribute.FileAttribute 0)))]
    (git! repo "init")
    (git! repo "config" "user.email" "test@example.invalid")
    (git! repo "config" "user.name" "Test")
    (let [tasks (mapv #(dissoc % :priority)
                      [(committed-task repo "M-a" "codex-test")
                       (committed-task repo "M-b" "codex-test")])
          snapshot (authorized-snapshot snapshot)
          receipt (select-ordinary {:tasks tasks
                                     :fetch-snapshot (constantly snapshot)
                                     :retain-manifest? true})]
      (is (= :captured (get-in receipt [:decision-input-manifest :status])))
      (is (= :pending-independent-replay
             (:decision-input-verification receipt)))
      (is (string? (:decision-input-manifest-sha256 receipt)))
      (is (nil? (:action receipt)))
      (is (= :refused
             (get-in receipt [:policy :status]))))))

(deftest missing-policy-field-config-is-a-typed-stop
  (let [calls (atom 0)
        receipt (live/select-live
                 {:tasks tasks :fetch-snapshot (constantly snapshot)
                  :policy-field-fn (fn [& _] (swap! calls inc))})]
    (is (zero? @calls))
    (is (= :refused (get-in receipt [:policy :status])))
    (is (= :policy-field-config-required (get-in receipt [:policy :reason])))
    (is (nil? (:action receipt)))))

(deftest ordinary-menu-refuses-a-focus-read-at-another-time
  (let [menu-called (atom false)
        receipt (live/select-live
                 (frozen-focus
                  {:tasks tasks :fetch-snapshot (constantly snapshot)
                   :focus-context {:as-of "2026-10-22T00:00:00Z" :status :retained
                                   :focus "WM"}
                   :policy-field-config {:authority :test-fixture}
                   :policy-field-fn complete-policy-field
                   :policy-menu-fn (fn [& _] (reset! menu-called true) nil)}))]
    (is (= :focus-as-of-mismatch (get-in receipt [:policy :meta-selection :reason])))
    (is (false? @menu-called))
    (is (nil? (:action receipt)))))

(deftest ordinary-menu-receives-the-frozen-classes
  (let [join (atom nil)
        _ (live/select-live
           (frozen-focus {:tasks tasks :fetch-snapshot (constantly snapshot)
                            :policy-field-config {:authority :test-fixture}
                            :policy-field-fn complete-policy-field
                            :focus-classify-fn
                            (fn [_ _ as-of target _]
                              (if (= "M-a" target)
                                {:class :focus :derived-via {:as-of as-of}}
                                {:class :unknown
                                 :relation {:status :absent
                                            :reason :relation-not-declared}}))
                            :policy-menu-fn (fn [_ j] (reset! join j) nil)}))]
    (is (= {:status :present :as-of selection-as-of :focus "WM"
            :focus-status :retained :focus-origin nil
            :previous-focus {:status :absent :reason :predecessor-carrier-not-supplied}}
           (:focus-receipt @join)))
    (is (= {:class :focus :scorer-class :focused}
           (select-keys (get-in @join [:target-classes "M-a"]) [:class :scorer-class])))
    (is (= :relation-not-declared (get-in @join [:target-classes "M-b" :reason]))
        "the classifier's own reason is kept, not replaced by not-recorded")))
