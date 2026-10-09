(ns futon2.aif.meta-live-outer-selector-test
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.meta-field-observation :as field]
            [futon2.aif.meta-live-outer-selector :as live]
            [futon2.aif.meta-pipeline-selector :as selector]
            [futon2.aif.mission-registry :as registry]))

(defn pin [path ch] {:path path :sha256 (apply str (repeat 64 ch))})
(def snapshot
  {:schema :wm/pipeline-cascade-snapshot-v1
   :summary-source (pin "summary" "a") :graph-source (pin "graph" "b")
   :summary {:consistent? true
             :standards {:s1-regenerates true :s2-evidence true
                         :s3-reconstitution true :s4-honest-holes true
                         :s5-composed true}}
   :graph {:section-status {:clusters {:status :ok}}
           :lineage [] :clusters [{:mission "M-a"} {:mission "M-b"}]
           :arrows [] :held []
           :tickets {:items [{:stem "M-a" :mtime-ms 200}
                             {:stem "M-b" :mtime-ms 100}]}
           :patterns {:edges []}}})
(def tasks
  [{:id "M-a" :kind :mission :priority 2 :source (pin "M-a.md" "c")}
   {:id "M-b" :kind :mission :priority 8 :source (pin "M-b.md" "d")}])

(deftest live-adapter-selects-and-translates-without-a-draw
  (let [receipt (live/select-live {:tasks tasks :fetch-snapshot (constantly snapshot)})]
    (is (= :wm/outer-task-selection-v1 (:schema receipt)))
    (is (= :meta-live-pipeline-task-state (get-in receipt [:policy :kind])))
    (is (= "M-a" (get-in receipt [:chosen :id])))
    (is (= {:type :advance-mission :target "M-a"} (:action receipt)))
    (is (= {:absent :deterministic-meta-policy} (:draw receipt)))
    (is (= :minimum-pairwise-task-state-G
           (get-in receipt [:policy :meta-selection :reason])))
    (is (contains? (set (get-in receipt [:policy :meta-selection
                                         :pairwise 0 :shared-channels]))
                   :pipeline-freshness-cost))
    (is (= ["M-a" "M-b"]
           (mapv :id (get-in receipt [:policy :meta-selection :ranking]))))))

(deftest missing-or-equal-evidence-refuses-with-complete-support
  (testing "missing is absence"
    (let [receipt (live/select-live {:tasks (mapv #(dissoc % :priority) tasks)
                                     :fetch-snapshot (constantly snapshot)
                                     :candidate-fn
                                     (fn [task-rows _]
                                       (mapv (fn [{:keys [id kind]}]
                                               {:id id :kind kind
                                                :support {:automated-feasibility :unknown}
                                                :channels {}})
                                             task-rows))})]
      (is (= :shared-current-channel-unavailable
             (get-in receipt [:policy :meta-selection :reason])))
      (is (= 2 (count (:support receipt))))
      (is (nil? (:action receipt)))))
  (testing "ties do not acquire a hidden first-item rule"
    (let [equal-snapshot (assoc-in snapshot [:graph :tickets :items 1 :mtime-ms] 200)
          receipt (live/select-live {:tasks (mapv #(assoc % :priority 1) tasks)
                                     :fetch-snapshot (constantly equal-snapshot)})]
      (is (= :no-unique-task-state-minimum
             (get-in receipt [:policy :meta-selection :reason])))
      (is (nil? (:action receipt))))))

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
        receipt (live/select-live
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
    (is (= "M-a" (get-in receipt [:chosen :id])))))

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

(deftest partial-live-snapshot-remains-a-typed-outer-refusal
  (let [receipt (live/select-live
                 {:tasks tasks
                  :fetch-snapshot #(assoc-in snapshot
                                             [:graph :section-status :clusters :status]
                                             :failed)})]
    (is (= :pipeline-snapshot-invalid
           (get-in receipt [:policy :meta-selection :reason])))
    (is (= 2 (count (:support receipt))))
    (is (nil? (:action receipt)))))

(deftest off-map-tasks-are-accounted-as-typed-exclusions
  (let [off-map {:id "M-z" :kind :mission :priority 0
                 :source (pin "M-z.md" "e")}
        receipt (live/select-live {:tasks (conj tasks off-map)
                                   :fetch-snapshot (constantly snapshot)})]
    (is (= "M-a" (get-in receipt [:chosen :id])))
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
    (is (= #{:repair-finding/root-missing :repair-finding/root-ambiguous}
           reasons))
    (is (every? #(get-in % [:ineligibility-evidence :ticket-source :sha256])
                (:excluded receipt)))
    (is (= ["M-a" "M-b"] (mapv :id (:support receipt))))))

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

(deftest registry-task-census-uses-one-pinned-root
  (let [calls (atom [])]
    (with-redefs [registry/load-missions
                  (fn [root]
                    (swap! calls conj [:missions root])
                    {:missions [{:id "M-current" :status-class :active}]})
                  registry/load-excursions
                  (fn [root]
                    (swap! calls conj [:excursions root])
                    {:excursions [{:id "E-current" :status-class :open}]})
                  registry/load-tickets
                  (fn [root]
                    (swap! calls conj [:tickets root])
                    {:tickets [{:id "T-current" :status-class :live}]})]
      (is (= #{"M-current" "E-current" "T-current"}
             (set (map :id (live/live-registry-tasks "/pinned/code")))))
      (is (= #{[:missions "/pinned/code"]
               [:excursions "/pinned/code"]
               [:tickets "/pinned/code"]}
             (set @calls))))))

(deftest ^:slow current-data-composition-is-structurally-bounded
  (let [snapshot (live/fetch-pipeline-snapshot)
        tasks (live/live-registry-tasks)
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

(deftest ^:slow production-vector-retains-decision-authority-without-metadata
  (let [tasks (vec (live/live-registry-tasks))
        receipt (live/select-live {:tasks tasks :retain-manifest? true})]
    (is (= :captured (get-in receipt [:decision-input-manifest :status])))
    (is (= :pending-independent-replay
           (:decision-input-verification receipt)))
    (is (string? (:decision-input-manifest-sha256 receipt)))
    (is (some? (:action receipt)))))
