(ns futon2.aif.full-loop-runner-test
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.full-loop-cli :as cli]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.pattern-registry :as patterns]
            [futon2.aif.repair-obligation :as repair]
            [futon2.aif.tripwire :as tripwire]
            [futon2.report.cascade-lane :as cascade])
  (:import [java.time Instant]))

(use-fixtures :once hermetic/with-hermetic-stores)

(deftest production-repair-root-is-unreachable-during-runner-suite
  (is (not= hermetic/production-repair-root repair/default-root))
  (is (not= hermetic/production-trip-root tripwire/default-trip-root)))

(def selected-action {:type :open-mission :target "M-selected"})

(def judgement
  {:ranked-actions [{:rank 1 :action {:type :open-mission :target "M-rank-head"}
                     :G-efe -2.0 :controller-score -2.0}
                    {:rank 2 :action selected-action
                     :G-efe -1.0 :controller-score -1.0}]
   :decision {:action selected-action :rank 2 :source :habit-prior}
   :belief {} :belief-pre {} :observation {} :free-energy {}
   :prediction-errors {} :precision-state {} :micro-step-trace []
   :ranked-actions-extra [] :mode :maintain})

(defn synthetic-artifact-binding [_repo before author-job]
  {:fresh-author? true
   :repo "/repo"
   :pre-dispatch-head (:head before)
   :observed-head (:artifact-ref author-job)
   :author-window-start-ms 1000
   :author-window-end-ms 2000
   :corroborates? true
   :disagreement? false
   :commit (:artifact-ref author-job)})

(defn isolated-runner-opts []
  {:cohort? false
   :phase-log-fn (fn [_])
   :roster-fn (fn [_] {:zai-5 {:status "idle" :invoke-ready? true}
                       :codex-7 {:status "idle" :invoke-ready? true}
                       :codex-1 {:status "idle" :invoke-ready? true}})
   :judge-fn (fn [_] {:judgement judgement})
   :refresh-fn (fn [])
   :substrate-preflight-fn (fn [_] {:route :test})
   :code-state-fn (fn [] {:repo "/futon2" :git-sha "head"
                          :git-dirty? false :repo-heads {}})
   :mode-flags-fn (fn [] {})
   :version-stamp-fn identity
   :mission-fn (fn [target] {:id target})
   :construct-fn runner/construct-for-decision
   :author-artifact-observer-fn synthetic-artifact-binding
   :queue-fn identity})

(def feature-card-claim
  {:built "Build-time feature cards now survive grounding into Morning Brief."
   :want-coverage "The grounded attempt carries the author's feature story and replay steps."
   :matches-intent? true
   :things-to-try ["clojure -X:test :nses '[futon2.aif.full-loop-runner-test]' -> all runner tests pass"
                   "clojure -M:wm-full-loop feature <attempt-id> -> the grounded feature card is printed"]})

(def successful-execution
  {:executed true :tool-events 3 :command-events 3})

(defn- run-feature-card-attempt
  [{:keys [author-card author-summary grounded? artifacts? reviewer-execution
           reviewer-events cure-card cure-summary cure-commit build-cure-retries]
    :or {grounded? true artifacts? false}}]
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                       "wm-feature-card-" (make-array java.nio.file.attribute.FileAttribute 0)))
        mission-file (io/file root "M-selected.md")
        fold-file (io/file root "M-selected.executed.edn")
        proof-file (io/file root "logic/feature-witness.edn")
        queued (atom [])
        dispatches (atom [])
        _ (spit mission-file "# test mission\n")
        _ (when artifacts?
            (io/make-parents fold-file)
            (spit fold-file (pr-str {:boxes [:author :store]
                                     :want-coverage [:feature-card]}))
            (io/make-parents proof-file)
            (spit proof-file (pr-str {:witness :feature-card-persisted})))
        commit "feature123"
        cure-id "feature-cure"
        opts (merge
              (isolated-runner-opts)
              {:repair-open-fn (constantly [])
               :target-repo-fn (fn [& _] (.getPath root))
               :repair-system-record-fn
               (fn [finding] (assoc finding :repair/id "test-repair"))
               :mission-fn (fn [target]
                             {:id target :path (.getPath mission-file)})
               :trace-fn (constantly (.getPath (io/file root "trace.edn")))
               :author-artifact-observer-fn
               (fn [repo before author-job]
                 {:fresh-author? true
                  :repo repo
                  :pre-dispatch-head (:head before)
                  :observed-head (:artifact-ref author-job)
                  :corroborates? true
                  :commit (:artifact-ref author-job)})
               :dispatch-fn
               (fn [_ agent _ _ _]
                 (swap! dispatches conj agent)
                 {:job-id (if (= agent "zai-5")
                            (if (> (count (filter #(= "zai-5" %) @dispatches)) 1)
                              cure-id
                              "feature-author")
                            "feature-review")})
               :poll-fn
               (fn [_ job-id]
                 (condp = job-id
                   "feature-author"
                   (cond-> {:job-id job-id :state "done" :artifact-ref commit
                            :result-summary (or author-summary
                                                (str "FULL_LOOP_AUTHOR: DONE " commit))
                            :execution successful-execution}
                     author-card (assoc :feature-card author-card))
                   cure-id
                   (cond-> {:job-id job-id :state "done"
                            :artifact-ref (or cure-commit commit)
                            :result-summary (or cure-summary
                                                (str "FULL_LOOP_AUTHOR: DONE "
                                                     (or cure-commit commit)))
                            :execution successful-execution}
                     cure-card (assoc :feature-card cure-card))
                   ;; reviewer
                   (cond-> {:job-id job-id :state "done"
                            :execution (or reviewer-execution successful-execution)
                            :result-summary (str "FULL_LOOP_REVIEW: APPROVE\n"
                                                 "FULL_LOOP_REVIEWER_NOTE: Replay steps verified.")}
                     reviewer-events (assoc :events reviewer-events))))
               :resolve-build-fn
               (fn [_]
                 {:repo (.getPath root)
                  :files ["src/feature.clj" "logic/feature-witness.edn"]})
               :ground-fn
               (fn [& _]
                 {:before {:implementation-entity nil}
                  :after {:implementation-entity {:id "feature-impl"}}
                  :resolved? grounded?
                  :dial-moved? grounded?
                  :implementation-id "feature-impl"
                  :discharge-id "feature-discharge"})
               :queue-fn #(swap! queued conj %)}
              (when (some? build-cure-retries)
                {:build-cure-retries build-cure-retries}))
        result (runner/run-opportunity! opts)]
    {:result result :item (first @queued)
     :dispatches @dispatches
     :fold-file fold-file :proof-file proof-file}))

(deftest agency-prefix-contract-preserves-a-text-feature-card
  (let [summary (str "FULL_LOOP_FEATURE_CARD: "
                     "{:built \"compact repair\" :want-coverage \"card survives\" "
                     ":matches-intent? true :things-to-try [\"feature id -> card\"]}\n"
                     "FULL_LOOP_AUTHOR: DONE feature123")
        durable-prefix (subs summary 0 (min 200 (count summary)))
        {:keys [result item]}
        (run-feature-card-attempt {:author-summary durable-prefix})]
    (is (<= (count (first (str/split-lines summary))) 200))
    (is (= :grounded-change (:outcome result)))
    (is (= "compact repair" (get-in item [:feature-card :built])))
    (is (= ["feature id -> card"]
           (get-in item [:feature-card :things-to-try])))))

(deftest newline-squashed-agency-prefix-still-yields-the-card
  ;; Observed on attempt-026: the durable prefix replaced the author's newline
  ;; with a space, leaving trailing prose ON the card line. The parser must
  ;; read the one EDN form and ignore what follows.
  (let [author-response
        (str "FULL_LOOP_FEATURE_CARD: "
             "{:built \"Durable compact author-card contract\" "
             ":want-coverage \"Cards survive Agency prefix\" "
             ":matches-intent? true "
             ":things-to-try [\"runner tests -> green\"]} "
             "Repaired the Agency 200-character prefix contract and added "
             "regression coverage for the full-loop runner.")
        squashed (subs author-response 0 200)
        {:keys [result item]}
        (run-feature-card-attempt {:author-summary squashed})]
    (is (= 200 (count squashed)))
    (is (str/ends-with? squashed "Repaired the Agency "))
    (is (= :grounded-change (:outcome result)))
    (is (= "Durable compact author-card contract"
           (get-in item [:feature-card :built])))
    (is (= ["runner tests -> green"]
           (get-in item [:feature-card :things-to-try])))))

(deftest feature-card-validation-rejects-the-attempt-024-shape
  ;; attempt-024 used a large nested map as :built. Besides being a poor
  ;; replayable claim, it pushed the closing brace outside Agency's durable
  ;; prefix. Both the structured and text forms now fail with typed reasons.
  (let [nested {:built {:commit "abc" :feature :witness-bearing-edges}
                :want-coverage "cross-mission witnesses"
                :matches-intent? true
                :things-to-try ["runner tests -> green"]}
        truncated (str "FULL_LOOP_FEATURE_CARD: "
                       "{:built \"compact\" :want-coverage \""
                       (apply str (repeat 180 "x")))]
    (is (= :built-must-be-a-nonblank-string
           (:reason (#'runner/feature-card-validation
                     {:feature-card nested}))))
    (is (= :truncated-or-over-durable-limit
           (:reason (#'runner/feature-card-validation
                     {:result-summary truncated}))))))

(deftest feature-card-validation-requires-replayable-observations
  (let [bad-step (assoc feature-card-claim
                        :things-to-try ["run the runner tests"])
        blank-command (assoc feature-card-claim
                             :things-to-try [" -> tests pass"])
        blank-observation (assoc feature-card-claim
                                 :things-to-try ["run tests -> "])
        empty-steps (assoc feature-card-claim :things-to-try [])]
    (is (= :things-to-try-must-be-observation-shaped
           (:reason (#'runner/feature-card-validation
                     {:feature-card bad-step}))))
    (is (= :things-to-try-must-be-observation-shaped
           (:reason (#'runner/feature-card-validation
                     {:feature-card blank-command}))))
    (is (= :things-to-try-must-be-observation-shaped
           (:reason (#'runner/feature-card-validation
                     {:feature-card blank-observation}))))
    (is (= :things-to-try-must-be-nonempty
           (:reason (#'runner/feature-card-validation
                     {:feature-card empty-steps}))))))

(deftest text-feature-card-must-be-the-durable-response-prefix
  (let [card (str "FULL_LOOP_FEATURE_CARD: "
                  "{:built \"prefix authority\" :want-coverage \"no echoes\" "
                  ":matches-intent? true :things-to-try [\"tests -> green\"]}")]
    (is (= :marker-not-at-durable-prefix
           (:reason (#'runner/feature-card-validation
                     {:result-summary (str "Completed work. " card)}))))
    (is (= :missing-marker
           (:reason (#'runner/feature-card-validation
                     {:terminal-message card}))))
    (is (= :missing-marker
           (:reason (#'runner/feature-card-validation
                     {:events [{:type "done" :text card}]}))))))

(deftest fresh-author-cure-rebinds-against-a-fresh-pre-cure-snapshot
  ;; Review finding on the cure loop: the first implementation passed the
  ;; original binding's bare :pre-dispatch-head sha where fresh-artifact-binding
  ;; expects a {:head :observed-at-ms} snapshot, silently degrading cured
  ;; commits to narrated artifact-refs. Pin the repaired contract: a FRESH
  ;; snapshot is taken before the cure dispatch, handed to the observer, and
  ;; the returned binding (not the stale one) rides the cured author-job.
  (let [observer-calls (atom [])
        good-card (str "FULL_LOOP_FEATURE_CARD: "
                       "{:built \"cure\" :want-coverage \"bound\" "
                       ":matches-intent? true :things-to-try [\"a -> b\"]}")
        opts {:build-cure-retries 1
              :phase-log (str (System/getProperty "java.io.tmpdir")
                              "/cure-rebind-test-phase.log")
              :repo-head-observation-fn
              (fn [repo] {:repo repo :head "fresh-head" :observed-at-ms 42})
              :author-artifact-observer-fn
              (fn [repo before job]
                (swap! observer-calls conj {:repo repo :before before :job job})
                {:fresh-author? true :repo repo :commit "cured123"})
              :dispatch-fn (fn [& _] {:job-id "cure-1"})
              :poll-fn (fn [_ _] {:job-id "cure-1" :state "done"
                                  :artifact-ref "cured123"
                                  :result-summary
                                  (str good-card "\nFULL_LOOP_AUTHOR: DONE cured123")})
              :resolve-build-fn (fn [_] {:repo "/repo" :files ["src/x.clj"]})}
        result (#'runner/build-cure-loop
                opts {} "author-x" (atom 0)
                "target-x" "orig123" "/repo" ["src/x.clj"]
                {:job-id "author-1" :state "done"
                 :result-summary "FULL_LOOP_AUTHOR: DONE orig123"}
                true
                {:repo "/repo" :pre-dispatch-head "stale-sha"})]
    (is (= 1 (count @observer-calls)))
    (is (= {:repo "/repo" :head "fresh-head" :observed-at-ms 42}
           (select-keys (:before (first @observer-calls))
                        [:repo :head :observed-at-ms]))
        "the observer must receive a fresh snapshot map, never the stale sha")
    (is (= "cured123" (:commit result)))
    (is (= "cured123" (get-in result [:author-job :repo-observed-artifact-ref])))
    (is (= "cured123" (get-in result [:author-job :artifact-binding :commit]))
        "the cured author-job carries the NEW binding, not the stale one")
    (is (true? (get-in result [:build-retries 0 :cured?])))))

(deftest author-contract-names-the-durable-feature-card-boundary
  (let [prompt (#'runner/author-prompt
                {:author "author" :reviewer "reviewer"}
                "target" {:id "target"} {} [])]
    (is (re-find #"BEGIN your response with one compact" prompt))
    (is (re-find #"at most 200 characters" prompt))
    (is (re-find #"closing brace is inside the 200-character limit" prompt))))

(deftest grounded-author-feature-card-is-persisted-and-rendered
  (let [{:keys [result item fold-file proof-file]}
        (run-feature-card-attempt {:author-card feature-card-claim
                                   :artifacts? true})
        rendered (-> {:attempt item}
                     cli/feature-acceptance
                     cli/render-feature-acceptance)
        absent-artifacts
        (:item (run-feature-card-attempt {:author-card feature-card-claim}))]
    (is (= :grounded-change (:outcome result)))
    (is (= (.getPath fold-file) (get-in item [:feature-card :fold-ref])))
    (is (= (.getPath proof-file) (get-in item [:feature-card :proof-ref])))
    (is (= "Replay steps verified."
           (get-in item [:feature-card :reviewer-note])))
    (is (re-find #"4. THE SORRY / PROOF-HOLE[\s\S]*Want coverage:" rendered))
    (is (re-find #"5. WIRING DIAGRAM[\s\S]*Fold:" rendered))
    (is (re-find #"7. THE FEATURE[\s\S]*Build-time feature cards" rendered))
    (is (re-find #"8. THINGS TO TRY[\s\S]*full-loop-runner-test" rendered))
    (is (nil? (get-in absent-artifacts [:feature-card :fold-ref])))
    (is (nil? (get-in absent-artifacts [:feature-card :proof-ref])))))

(deftest grounded-attempt-without-author-card-is-an-incomplete-deliverable
  (let [{:keys [result item]} (run-feature-card-attempt {})]
    (is (= :build-failed (:outcome result)))
    (is (= :feature-card-missing-or-invalid
           (get-in result [:data :failure-kind])))
    (is (= "feature123" (:commit item)))
    (is (nil? (:feature-card item)))
    (is (= :partial-authored (get-in item [:achievement :tier])))))

(deftest approving-code-review-without-execution-evidence-is-rejected
  (let [{:keys [result item]}
        (run-feature-card-attempt
         {:author-card feature-card-claim
          :reviewer-execution {:executed false :tool-events 0 :command-events 0}})
        gate (get-in result [:checkpoints :build :judgment :validation
                             :review-gate])]
    (is (= :build-failed (:outcome result)))
    (is (= :review-execution-evidence-missing
           (get-in result [:data :failure-kind])))
    (is (false? (:passed? gate)))
    (is (false? (get-in result [:checkpoints :build :judgment :validation
                                :approved?])))
    (is (= :review-execution-evidence-missing
           (get-in item [:failure :kind])))))

(deftest reviewer-tool-ledger-corroborates-a-stale-execution-summary
  (let [{:keys [result item]}
        (run-feature-card-attempt
         {:author-card feature-card-claim
          :reviewer-execution {:executed false :tool-events 0 :command-events 0}
          :reviewer-events [{:seq 4 :type "text" :text "Inspecting."}
                            {:seq 5 :type "tool_use" :tools ["Bash"]
                             :previews ["Bash clojure -X:test"]}
                            {:seq 6 :type "tool_use" :tools ["Read"]}]})
        validation (get-in result [:checkpoints :build :judgment :validation])
        gate (:review-gate validation)]
    (is (= :grounded-change (:outcome result)))
    (is (:passed? gate))
    (is (:executed? gate))
    (is (= 2 (:tool-events gate)))
    (is (= {:executed true :tool-events 2 :command-events 1}
           (:reviewer validation)))
    (is (= (:reviewer validation) (:execution gate)))
    (is (= :job-events (:execution-source gate)))
    (is (= :fully-grounded (get-in item [:achievement :tier])))))

(deftest non-grounded-attempt-never-persists-author-card
  (let [{:keys [result item]}
        (run-feature-card-attempt {:author-card feature-card-claim
                                   :grounded? false
                                   :artifacts? true})
        rendered (-> {:attempt item}
                     cli/feature-acceptance
                     cli/render-feature-acceptance)]
    (is (= :grounded-no-change (:outcome result)))
    (is (nil? (:feature-card item)))
    (is (re-find #"not rendered for this attempt ⟵ build-time gap" rendered))
    (is (re-find #"build-time feature card pending" rendered))))

(defn fire-pattern-action []
  (merge {:type :fire-pattern
          :proposer-id :pattern-enumerator
          :target "coordination/capability-gate"
          :pattern-title "Capability Gate"
          :pattern-summary "Make the capability boundary explicit"
          :evidence-ids ["ctx-1"]}
         (patterns/pattern-artifact-receipt
          "coordination/capability-gate"
          "/home/joe/code/futon3/library/coordination/capability-gate.flexiarg")))

(deftest real-opportunity-pins-construction-and-separates-review
  (let [constructed (atom nil)
        dispatches (atom [])
        queued (atom [])
        phases (atom [])
        result
        (runner/run-opportunity!
         {:cohort? false
          :batch-id "overnight-2026-07-16"
          :phase-log-fn #(swap! phases conj %)
          :repair-open-fn (constantly [])
          :roster-fn (fn [_] {:zai-5 {:status "idle" :invoke-ready? true}
                              :codex-7 {:status "idle" :invoke-ready? true}
                              :codex-1 {:status "idle" :invoke-ready? true}})
          :judge-fn (fn [_] {:judgement judgement})
          :refresh-fn (fn [] nil)
          :substrate-preflight-fn (fn [_] {:route :test})
          :code-state-fn (fn [] {:repo "/futon2" :git-sha "head"
                                 :git-dirty? false :repo-heads {}})
          :mode-flags-fn (fn [] {:likelihood-mode :aif})
          :version-stamp-fn identity
          :mission-fn (fn [target] {:id target :path "/mission.md"})
          :trace-fn (fn [_] "/tmp/test-trace.edn")
          :construct-fn (fn [entry]
                          (reset! constructed entry)
                          {:shown [:P1] :psi :psi :cascade-score 1.0
                           :semilattice [] :policy-holes []})
          :author-artifact-observer-fn synthetic-artifact-binding
          :dispatch-fn (fn [_ agent _ _ prompt]
                         (swap! dispatches conj {:agent agent :prompt prompt})
                         {:job-id (if (= agent "zai-5") "author-job" "review-job")})
          :poll-fn (fn [_ job-id]
                     (if (= job-id "author-job")
                       {:job-id job-id :state "done" :artifact-ref "abc123"
                        :feature-card feature-card-claim
                        :execution successful-execution
                        :events [{:text "FULL_LOOP_AUTHOR: DONE abc123"}]}
                       {:job-id job-id :state "done"
                        :execution successful-execution
                        :result-summary "FULL_LOOP_REVIEW: APPROVE\nLooks good."
                        :events [{:type "prompt"
                                  :text "FULL_LOOP_REVIEW: REQUEST_CHANGES"}]}))
          :resolve-build-fn (fn [_] {:repo "/repo" :files ["src/real.clj"]})
          :ground-fn (fn [& _]
                       {:before {:implementation-entity nil}
                        :after {:implementation-entity {:id "impl"}}
                        :resolved? true :dial-moved? true
                        :implementation-id "impl" :discharge-id "discharge"})
          :queue-fn #(swap! queued conj %)})]
    (is (= :grounded-change (:outcome result)))
    (is (= selected-action (:action @constructed))
        "construction follows the selected action, not the raw rank head")
    (is (= ["zai-5" "codex-7"] (mapv :agent @dispatches)))
    (is (re-find #"FULL_LOOP_FEATURE_CARD:"
                 (:prompt (first @dispatches))))
    (is (re-find #"command or action -> expected observation"
                 (:prompt (first @dispatches))))
    (is (re-find #"Do not edit or commit" (:prompt (second @dispatches))))
    (is (= :grounded-change (:outcome (first @queued))))
    (is (= "overnight-2026-07-16" (:batch-id (first @queued))))
    (is (= selected-action
           (get-in (first @queued) [:selection-review :selected-action])))
    (is (= "Was this the best available selection?"
           (get-in (first @queued) [:selection-review :question])))
    (is (= [:opportunity :agent-readiness :agent-readiness :code-state :code-state
            :substrate-preflight :substrate-preflight
            :preference-refresh :preference-refresh
            :stop-line-memory :stop-line-memory :selection :selection
            :construction :construction :author-dispatch :author-dispatch
            :author-wait :author-wait :build-resolution :build-resolution
            :reviewer-dispatch :reviewer-dispatch :reviewer-wait :reviewer-wait
            :grounding :grounding :opportunity]
           (mapv :phase @phases)))
    (is (= #{:selection :construction :dispatch :build :adjudication}
           (set (keys (:checkpoints result)))))))

(deftest reviewer-prompt-cannot-supply-its-own-approval
  (let [job {:result-summary "FULL_LOOP_REVIEW: REQUEST_CHANGES live seam remains optional"
             :events [{:type "prompt" :text "FULL_LOOP_REVIEW: APPROVE"}]}]
    (is (not (re-find #"FULL_LOOP_REVIEW:\s*APPROVE"
                      (#'runner/job-text job))))))

(deftest agency-dispatch-explicitly-selects-work-mode
  (let [request (atom nil)]
    (with-redefs-fn
      {#'runner/post-json!
       (fn [url body]
         (reset! request {:url url :body body})
         {:job-id "work-job"})}
      #(runner/dispatch! {:agency-base "http://agency"}
                         "codex-7" "wm" :target "do the work"))
    (is (= "work" (get-in @request [:body :mode])))
    (is (= "request" (get-in @request [:body :type])))))

(deftest reviewer-receives-the-capability-construction-contract
  (let [contract {:construction-kind :capability-gap-repair
                  :selected-action {:type :learn-action-class
                                    :target-class :fire-pattern}
                  :capability-contract
                  {:action-class :fire-pattern
                   :required-components [:action-proposer-registration]}}
        prompt (#'runner/reviewer-prompt
                {:author "codex-7" :reviewer "claude-7"}
                :fire-pattern contract "/repo" "abc123"
                {:job-id "author-job"} [])]
    (is (re-find #"CONSTRUCTION CONTRACT" prompt))
    (is (re-find #":capability-gap-repair" prompt))
    (is (re-find #":action-proposer-registration" prompt))
    (is (re-find #"clj-kondo" prompt))
    (is (re-find #"check-parens\.el" prompt))
    (is (re-find #"relevant tests in a fresh JVM" prompt))
    (is (re-find #"Report the exact commands" prompt))))

(deftest code-review-execution-gate-requires-tools-not-just-an-executed-flag
  (let [gate (#'runner/review-execution-gate
              ["src/example.clj"]
              {:execution {:executed true :tool-events 0 :command-events 0}})]
    (is (:required? gate))
    (is (false? (:passed? gate)))
    (is (= :review-execution-evidence-missing (:failure-kind gate)))))

(deftest capability-gap-action-has-a-typed-production-construction
  (with-redefs [cascade/cascade-lane
                (fn [& _]
                  (throw (ex-info "capability repair entered ordinary cascade" {})))]
    (let [selected {:action {:type :learn-action-class
                             :target-class :fire-pattern
                             :rationale "no addressable patterns"}}
          construction (runner/construct-for-decision selected)]
      (is (= :capability-gap-repair (:construction-kind construction)))
      (is (= (:action selected) (:selected-action construction)))
      (is (= :fire-pattern
             (get-in construction [:capability-contract :action-class])))
      (is (= [:addressable-substrate-enumerator
              :action-proposer-registration
              :instance-executability-check
              :production-actuation-path]
             (get-in construction
                     [:capability-contract :required-components])))
      (is (= #{:proposer-support :candidate-shape :execution-support
               :boundary-regression}
             (set (map :check
                       (get-in construction
                               [:capability-contract :acceptance]))))))))

(deftest fire-pattern-has-a-typed-production-actuation-construction
  (with-redefs [cascade/cascade-lane
                (fn [& _]
                  (throw (ex-info "fire-pattern entered ordinary cascade" {})))]
    (let [action (fire-pattern-action)
          construction (runner/construct-for-decision {:action action})]
      (is (= :fire-pattern-actuation (:construction-kind construction)))
      (is (= action (:selected-action construction)))
      (is (= "coordination/capability-gate"
             (get-in construction [:actuation-contract :target])))
      (is (= [:author-dispatch :independent-review :grounded-implementation]
             (get-in construction
                     [:actuation-contract :production-route]))))))

(deftest fire-pattern-production-construction-reaches-full-loop-actuation
  (let [action (fire-pattern-action)
        fire-judgement (-> judgement
                           (assoc :ranked-actions
                                  [{:rank 1 :action action
                                    :G-efe -2.0
                                    :controller-score -2.0}])
                           (assoc :decision {:action action :rank 1}))
        dispatches (atom [])
        grounded-construction (atom nil)
        result
        (runner/run-opportunity!
         {:cohort? false
          :phase-log-fn (fn [_])
          :repair-open-fn (constantly [])
          :roster-fn (fn [_] {:zai-5 {:status "idle" :invoke-ready? true}
                              :codex-7 {:status "idle" :invoke-ready? true}})
          :judge-fn (fn [_] {:judgement fire-judgement})
          :refresh-fn (fn [])
          :substrate-preflight-fn (fn [_] {:route :test})
          :code-state-fn (fn [] {:repo "/futon2" :git-sha "head"
                                 :git-dirty? false :repo-heads {}})
          :mode-flags-fn (fn [] {})
          :version-stamp-fn identity
          :mission-fn (fn [_] nil)
          :trace-fn (fn [_] "/tmp/fire-pattern-trace.edn")
          :author-artifact-observer-fn synthetic-artifact-binding
          :dispatch-fn (fn [_ agent _ _ prompt]
                         (swap! dispatches conj {:agent agent :prompt prompt})
                         {:job-id (if (= agent "zai-5")
                                    "author-job" "review-job")})
          :poll-fn (fn [_ job-id]
                     (if (= job-id "author-job")
                       {:job-id job-id :state "done" :artifact-ref "fire123"
                        :feature-card feature-card-claim
                        :execution successful-execution}
                       {:job-id job-id :state "done"
                        :execution successful-execution
                        :result-summary "FULL_LOOP_REVIEW: APPROVE"}))
          :resolve-build-fn (fn [_] {:repo "/repo" :files ["src/fire.clj"]})
          :ground-fn (fn [_ _ _ _ _ _ _ construction _ _]
                       (reset! grounded-construction construction)
                       {:resolved? true :dial-moved? true
                        :implementation-id "fire-pattern-impl"})
          :queue-fn identity})]
    (is (= :grounded-change (:outcome result)))
    (is (= ["zai-5" "codex-7"] (mapv :agent @dispatches)))
    (is (re-find #":fire-pattern-actuation"
                 (:prompt (first @dispatches))))
    (is (re-find #":retrieval-provenance"
                 (:prompt (first @dispatches))))
    (is (re-find #":artifact-integrity"
                 (:prompt (first @dispatches))))
    (is (re-find #":grounded-implementation"
                 (:prompt (second @dispatches))))
    (is (= (:pattern-sha256 action)
           (get-in @grounded-construction
                   [:actuation-contract :pattern-sha256])))))

(defn- substrate-fixture []
  (let [docs (atom {})
        reserved #{:xt/id :entity/type :entity/name :entity/source}]
    {:docs docs
     :opts {:entity-by-id-fn
            (fn [id]
              (when-let [doc (get @docs id)]
                {:id id :props (apply dissoc doc reserved)}))
            :put-doc-fn
            (fn [doc]
              (swap! docs assoc (:xt/id doc) doc)
              {:ok true})}}))

(deftest fire-pattern-grounding-persists-content-bound-provenance
  (let [action (fire-pattern-action)
        construction (runner/construct-for-decision {:action action})
        {:keys [docs opts]} (substrate-fixture)
        result (runner/ground-commit!
                "attempt-fire" "coordination/capability-gate"
                "codex-6" "claude-7" "/repo" "fireabc"
                ["src/fire.clj"] construction {:job-id "review-fire"} opts)
        implementation (get @docs "full-loop/implementation/fireabc")]
    (is (:resolved? result))
    (is (= :fire-pattern-actuation
           (:implementation/construction-kind implementation)))
    (is (= "coordination/capability-gate"
           (:implementation/pattern-id implementation)))
    (is (= (:pattern-path action)
           (:implementation/pattern-path implementation)))
    (is (= (:pattern-sha256 action)
           (:implementation/pattern-sha256 implementation)))
    (is (= ["ctx-1"]
           (:implementation/pattern-evidence-ids implementation)))
    (is (= (:actuation-contract construction)
           (:implementation/actuation-contract implementation)))))

(deftest fire-pattern-grounding-revalidates-before-writing
  (let [action (fire-pattern-action)
        construction (-> (runner/construct-for-decision {:action action})
                         (assoc-in [:selected-action :pattern-sha256]
                                   (apply str (repeat 64 "0"))))
        {:keys [docs opts]} (substrate-fixture)]
    (try
      (runner/ground-commit!
       "attempt-stale" "coordination/capability-gate"
       "codex-6" "claude-7" "/repo" "staleabc"
       ["src/fire.clj"] construction {:job-id "review-stale"} opts)
      (is false "stale pattern content must not reach the substrate")
      (catch clojure.lang.ExceptionInfo e
        (is (= :grounding-failed (:outcome (ex-data e))))))
    (is (empty? @docs) "failed revalidation writes neither implementation nor discharge")))

(deftest construction-failure-opens-system-stop-line-and-does-not-write-trace
  (let [findings (atom [])
        traces (atom [])
        gap-action {:type :learn-action-class :target-class :fire-pattern}
        gap-judgement (-> judgement
                          (assoc :ranked-actions [{:rank 1 :action gap-action
                                                   :G-efe -2.0
                                                   :controller-score -2.0}])
                          (assoc :decision {:action gap-action :rank 1}))
        result (runner/run-opportunity!
                {:cohort? false
                 :phase-log-fn (fn [_])
                 :repair-open-fn (constantly [])
                 :repair-system-record-fn
                 (fn [finding]
                   (swap! findings conj finding)
                   (assoc finding :repair/class :machine-failure))
                 :roster-fn (fn [_] {:zai-5 {:status "idle" :invoke-ready? true}
                                     :codex-7 {:status "idle" :invoke-ready? true}})
                 :judge-fn (fn [_] {:judgement gap-judgement})
                 :refresh-fn (fn [])
                 :substrate-preflight-fn (fn [_] {:route :test})
                 :code-state-fn (fn [] {:repo "/futon2" :git-sha "head"
                                        :git-dirty? false :repo-heads {}})
                 :mode-flags-fn (fn [] {})
                 :version-stamp-fn identity
                 :mission-fn (fn [_] nil)
                 :trace-fn #(swap! traces conj %)
                 :construct-fn (constantly nil)
                 :queue-fn identity})]
    (is (= :incomplete (:outcome result)))
    (is (= :construction-failed
           (get-in result [:data :failure-kind])))
    (is (empty? @traces) "unsupported selection must not train the habit trace")
    (is (= :machine-failure
           (:repair/class (:repair-obligation (:data result)))))
    (is (= :construction (:failure-stage (first @findings))))))

(deftest rejected-review-preserves-authored-commit-in-morning-brief
  (let [queued (atom [])
        findings (atom [])
        result
        (runner/run-opportunity!
         {:cohort? false
          :phase-log-fn (fn [_])
          :repair-open-fn (constantly [])
          :repair-record-fn #(swap! findings conj %)
          :roster-fn (fn [_] {:zai-5 {:status "idle" :invoke-ready? true}
                              :codex-7 {:status "idle" :invoke-ready? true}})
          :judge-fn (fn [_] {:judgement judgement})
          :refresh-fn (fn [])
          :substrate-preflight-fn (fn [_] {:route :test})
          :code-state-fn (fn [] {:repo "/futon2" :git-sha "head"
                                 :git-dirty? false :repo-heads {}})
          :mode-flags-fn (fn [] {})
          :version-stamp-fn identity
          :mission-fn (fn [target] {:id target})
          :trace-fn (fn [_] "/tmp/test-trace.edn")
          :construct-fn (fn [_] {:shown [:P1] :psi :psi :cascade-score 1.0
                                 :semilattice [] :policy-holes []})
          :author-artifact-observer-fn synthetic-artifact-binding
          :dispatch-fn (fn [_ agent _ _ _]
                         {:job-id (if (= agent "zai-5")
                                    "author-job" "review-job")})
          :poll-fn (fn [_ job-id]
                     (if (= job-id "author-job")
                       {:job-id job-id :state "done" :artifact-ref "abc123"
                        :feature-card feature-card-claim
                        :execution successful-execution}
                       {:job-id job-id :state "done"
                        :execution successful-execution
                        :result-summary
                        "FULL_LOOP_REVIEW: REQUEST_CHANGES fail closed"}))
          :resolve-build-fn (fn [_] {:repo "/repo" :files ["src/real.clj"]})
          :queue-fn #(swap! queued conj %)})]
    (is (= :build-failed (:outcome result)))
    (is (= "abc123" (get-in result [:data :commit])))
    (is (= "abc123" (:commit (first @queued))))
    (is (= :request-changes (:review-verdict (first @findings))))))

(deftest fresh-author-prefers-repo-observed-head-over-narrated-artifact
  (let [events (atom [])
        resolved (atom nil)
        dispatches (atom [])
        result
        (runner/run-opportunity!
         (merge
          (isolated-runner-opts)
          {:repair-open-fn (constantly [])
           :trace-fn (constantly "/tmp/artifact-binding-trace.edn")
           :construct-fn (fn [_] {:shown [] :policy-holes []})
           :target-repo-fn (fn [& _] "/repo")
           :repo-head-observation-fn
           (fn [repo]
             (swap! events conj :pre-dispatch-head)
             {:repo repo :head "base000" :observed-at-ms 1000})
           :author-artifact-observer-fn
           (fn [repo before author-job]
             (swap! events conj :post-author-head)
             {:fresh-author? true :repo repo
              :pre-dispatch-head (:head before)
              :observed-head "observed456"
              :author-window-start-ms 1000
              :author-window-end-ms 2000
              :text-artifact-ref (:artifact-ref author-job)
              :corroborates? false :disagreement? true
              :commit "observed456"})
           :dispatch-fn
           (fn [_ agent _ _ prompt]
             (swap! events conj (if (= agent "zai-5")
                                  :author-dispatch :reviewer-dispatch))
             (swap! dispatches conj {:agent agent :prompt prompt})
             {:job-id (if (= agent "zai-5") "author-job" "review-job")})
           :poll-fn
           (fn [_ job-id]
             (if (= job-id "author-job")
               {:job-id job-id :state "done" :artifact-ref "narrated123"
                :feature-card feature-card-claim
                :execution successful-execution}
               {:job-id job-id :state "done"
                :execution successful-execution
                :result-summary "FULL_LOOP_REVIEW: APPROVE"}))
           :resolve-build-fn
           (fn [commit]
             (reset! resolved commit)
             {:repo "/repo" :files ["src/observed.clj"]})
           :ground-fn
           (fn [& _] {:resolved? true :dial-moved? true
                      :implementation-id "observed-impl"})}))]
    (is (= :grounded-change (:outcome result)))
    (is (= "observed456" @resolved))
    (is (= [:pre-dispatch-head :author-dispatch :post-author-head
            :reviewer-dispatch]
           @events))
    (is (re-find #"authored commit observed456"
                 (:prompt (second @dispatches))))
    (is (true? (get-in result [:checkpoints :build :judgment :validation
                               :artifact-binding :disagreement?])))))

(deftest fresh-artifact-observation-validates-delta-ancestry-and-time-window
  (let [base-opts {:repo-head-observation-fn
                   (fn [repo] {:repo repo :head "observed456"
                               :observed-at-ms 2000})
                   :resolve-commit-sha-fn
                   (fn [_ commit] (when (= commit "narrated123") "old123"))
                   :ancestor-fn (fn [_ ancestor descendant]
                                  (and (= ancestor "base000")
                                       (= descendant "observed456")))
                   :commit-time-ms-fn (fn [& _] 1500)}
        before {:repo "/repo" :head "base000" :observed-at-ms 1000}
        binding (runner/fresh-artifact-binding
                 base-opts "/repo" before
                 {:artifact-ref "narrated123"})]
    (is (= "observed456" (:commit binding)))
    (is (:descendant? binding))
    (is (:in-author-window? binding))
    (is (:disagreement? binding))
    (is (nil? (:commit
               (runner/fresh-artifact-binding
                (assoc base-opts :commit-time-ms-fn (fn [& _] 500000))
                "/repo" before {:artifact-ref "narrated123"})))
        "a changed descendant outside the tolerated author window is rejected")))

(deftest narrated-artifact-without-new-repo-head-stops-before-review
  (let [dispatches (atom [])
        findings (atom [])
        result
        (runner/run-opportunity!
         (merge
          (isolated-runner-opts)
          {:repair-open-fn (constantly [])
           :trace-fn (constantly "/tmp/artifact-binding-mismatch-trace.edn")
           :construct-fn (fn [_] {:shown [] :policy-holes []})
           :target-repo-fn (fn [& _] "/repo")
           :repo-head-observation-fn
           (fn [repo] {:repo repo :head "base000" :observed-at-ms 1000})
           :author-artifact-observer-fn
           (fn [repo before author-job]
             {:fresh-author? true :repo repo
              :pre-dispatch-head (:head before)
              :observed-head (:head before)
              :author-window-start-ms 1000
              :author-window-end-ms 2000
              :text-artifact-ref (:artifact-ref author-job)
              :corroborates? false :disagreement? false :commit nil})
           :dispatch-fn
           (fn [_ agent & _]
             (swap! dispatches conj agent)
             {:job-id "author-job"})
           :poll-fn
           (fn [& _] {:job-id "author-job" :state "done"
                      :artifact-ref "narrated123"})
           :repair-system-record-fn
           (fn [finding]
             (swap! findings conj finding)
             (assoc finding :repair/id "repair-artifact-binding"))}))]
    (is (= :build-failed (:outcome result)))
    (is (= :artifact-binding-mismatch
           (get-in result [:data :failure-kind])))
    (is (= ["zai-5"] @dispatches)
        "no reviewer is dispatched for an unobserved narrated artifact")
    (is (= :machine-failure (:repair-class (first @findings))))
    (is (= :artifact-binding-mismatch
           (:failure-kind (first @findings))))))

(deftest machine-repair-binds-to-failed-machine-repository
  (let [original-action {:type :advance-mission
                         :target "M-learning-loop"
                         :mission-path "/home/joe/code/futon5a/holes/missions/M-learning-loop.md"}
        obligation {:repair/id "repair-attempt-021-artifact-binding-mismatch"
                    :repair/class :machine-failure
                    :selected-entry {:action original-action}
                    :backtrace {:code-state {:repo "/home/joe/code/futon2"}}}
        entry {:action {:type :repair-machine-failure
                        :target (:repair/id obligation)
                        :repair-obligation obligation}}]
    (is (= "/home/joe/code/futon2"
           (#'runner/target-repository
            {} entry obligation {:repo "/home/joe/code/futon2"})))
    (is (= "/explicit-machine-repo"
           (#'runner/target-repository
            {} (assoc-in entry [:action :repair-obligation :machine-repo]
                         "/explicit-machine-repo")
            obligation {:repo "/home/joe/code/futon2"})))
    (is (= "/home/joe/code/futon5a"
           (#'runner/target-repository
            {} {:action original-action} nil {:repo "/home/joe/code/futon2"})))
    (is (= "/home/joe/code/futon5a"
           (#'runner/target-repository
            {} (assoc-in entry [:action :repair-obligation :repair/class]
                         :independent-review-failure)
            obligation {:repo "/home/joe/code/futon2"})))))

(defn- no-commit-author-opts
  "Runner opts whose author job completes without any observable commit;
  the job's text is supplied by the caller. Mirrors attempt-020's shape."
  [dispatches findings author-job]
  (merge
   (isolated-runner-opts)
   {:repair-open-fn (constantly [])
    :trace-fn (constantly "/tmp/author-refusal-trace.edn")
    :construct-fn (fn [_] {:shown [] :policy-holes []})
    :target-repo-fn (fn [& _] "/repo")
    :repo-head-observation-fn
    (fn [repo] {:repo repo :head "base000" :observed-at-ms 1000})
    :author-artifact-observer-fn
    (fn [repo before _author-job]
      {:fresh-author? true :repo repo
       :pre-dispatch-head (:head before)
       :observed-head (:head before)
       :author-window-start-ms 1000
       :author-window-end-ms 2000
       :corroborates? false :disagreement? false :commit nil})
    :dispatch-fn (fn [_ agent & _]
                   (swap! dispatches conj agent)
                   {:job-id "author-job"})
    :poll-fn (fn [& _] author-job)
    :repair-system-record-fn
    (fn [finding]
      (swap! findings conj finding)
      (assoc finding :repair/id "repair-under-test"))}))

(deftest typed-author-refusal-is-an-environmental-hold-not-a-machine-failure
  ;; Replays the attempt-020 misclassification (2026-07-16): the author ended
  ;; with the contract's own legal no-commit marker — REFUSE with a typed
  ;; reason, line-anchored mid-message, not in :result-summary — and the
  ;; runner filed it as a :build-failed machine failure demanding a repair
  ;; commit. A typed refusal is an agent declining: environmental hold.
  (let [dispatches (atom [])
        findings (atom [])
        result
        (runner/run-opportunity!
         (no-commit-author-opts
          dispatches findings
          {:job-id "author-job" :state "done" :artifact-ref nil
           :result-summary "The loop is escalating — declining at the protocol level..."
           :events [{:type "text"
                     :text (str "Reasoning about why this must be declined.\n\n"
                                "FULL_LOOP_AUTHOR: REFUSE operator-engaged-concurrent-identity"
                                " — author is under live operator direction\n\n"
                                "Joe — this is your call and easily reversed.")}]}))]
    (is (= :guardrail-refusal (:outcome result)))
    (is (= :guardrail-refusal (get-in result [:data :failure-kind])))
    (is (= ["zai-5"] @dispatches)
        "no reviewer is dispatched for a refusal")
    (is (= :environmental-hold (:repair-class (first @findings)))
        "a typed refusal must not open a machine-failure repair")
    (is (= :guardrail-refusal (:failure-kind (first @findings))))
    (is (= (str "operator-engaged-concurrent-identity"
                " — author is under live operator direction")
           (get-in result [:data :error-data :refusal-reason]))
        "the typed reason is preserved for the morning brief")))

(deftest bare-refusal-without-typed-reason-stays-build-failed
  ;; Fail-closed: the contract demands REFUSE <typed reason>; a bare marker
  ;; is unverifiable and keeps the machine-failure classification.
  (let [dispatches (atom [])
        findings (atom [])
        result
        (runner/run-opportunity!
         (no-commit-author-opts
          dispatches findings
          {:job-id "author-job" :state "done" :artifact-ref nil
           :events [{:type "text" :text "FULL_LOOP_AUTHOR: REFUSE"}]}))]
    (is (= :build-failed (:outcome result)))
    (is (= :machine-failure (:repair-class (first @findings))))))

(deftest done-claim-without-verifiable-commit-stays-build-failed
  ;; Fail-closed: a DONE claim that repository observation cannot validate is
  ;; exactly the failure the machine-failure line exists for.
  (let [dispatches (atom [])
        findings (atom [])
        result
        (runner/run-opportunity!
         (no-commit-author-opts
          dispatches findings
          {:job-id "author-job" :state "done" :artifact-ref nil
           :events [{:type "text"
                     :text "FULL_LOOP_AUTHOR: DONE abc123 (narrated, never pushed)"}]}))]
    (is (= :build-failed (:outcome result)))
    (is (= :machine-failure (:repair-class (first @findings))))
    (is (= ["zai-5"] @dispatches))))

(deftest machine-stop-line-preempts-ordinary-selection-and-awaits-successor-validation
  (let [dispatches (atom [])
        implementations (atom [])
        stop-line {:repair/id "repair-failed-1"
                   :repair/status :open
                   :repair/class :machine-failure
                   :attempt-id "failed-1"
                   :failed-commit "bad123"
                   :review-verdict :request-changes
                   :review-text "trusted provenance is mandatory"
                   :selected-entry {:action {:type :address-sorry
                                             :target :sorry/g2}
                                    :controller-score 4.0}}
        follow-up (assoc stop-line
                         :repair/id "repair-failed-2"
                         :attempt-id "failed-2"
                         :failed-commit "bad789"
                         :review-text "caller-controlled identity is spoofable")
        unrelated (assoc stop-line
                         :repair/id "repair-other"
                         :attempt-id "failed-other"
                         :target :sorry/other
                         :review-text "unrelated finding")
        result
        (runner/run-opportunity!
         {:cohort? false
          :phase-log-fn (fn [_])
          :repair-open-fn (constantly [stop-line follow-up unrelated])
          :repair-implement-fn (fn [obligation implementation]
                                 (swap! implementations conj
                                        [obligation implementation]))
          :roster-fn (fn [_] {:zai-5 {:status "idle" :invoke-ready? true}
                              :codex-7 {:status "idle" :invoke-ready? true}
                              :codex-1 {:status "idle" :invoke-ready? true}})
          :judge-fn (fn [_] {:judgement judgement})
          :refresh-fn (fn [])
          :substrate-preflight-fn (fn [_] {:route :test})
          :code-state-fn (fn [] {:repo "/futon2" :git-sha "head"
                                 :git-dirty? false :repo-heads {}})
          :mode-flags-fn (fn [] {})
          :version-stamp-fn identity
          :mission-fn (fn [target] {:id target})
          :trace-fn (fn [_] "/tmp/test-trace.edn")
          :construct-fn (fn [entry]
                          {:mission (get-in entry [:action :target])
                           :shown [:P1] :psi :psi :cascade-score 1.0
                           :semilattice [] :policy-holes []})
          :author-artifact-observer-fn synthetic-artifact-binding
          :dispatch-fn (fn [_ agent _ target prompt]
                         (swap! dispatches conj {:agent agent :target target
                                                :prompt prompt})
                         {:job-id (if (= agent "zai-5")
                                    "author-job" "review-job")})
          :poll-fn (fn [_ job-id]
                     (if (= job-id "author-job")
                       {:job-id job-id :state "done" :artifact-ref "good456"
                        :feature-card feature-card-claim
                        :execution successful-execution}
                       {:job-id job-id :state "done"
                        :execution successful-execution
                        :result-summary "FULL_LOOP_REVIEW: APPROVE"}))
          :resolve-build-fn (fn [_] {:repo "/repo" :files ["src/real.clj"]})
          :ground-fn (fn [& _]
                       {:resolved? true :dial-moved? true
                        :implementation-id "impl"})
          :queue-fn identity})]
    (is (= :grounded-change (:outcome result)))
    (is (= ["repair-failed-1" "repair-failed-1"]
           (mapv :target @dispatches)))
    (is (= ["zai-5" "codex-1"] (mapv :agent @dispatches))
        "stop-line repair review routes to standing Ground Control")
    (is (every? #(re-find #"STOP-THE-LINE" (:prompt %)) @dispatches))
    (is (every? #(re-find #"trusted provenance is mandatory" (:prompt %))
                @dispatches))
    (is (every? #(not (re-find #"caller-controlled identity is spoofable"
                               (:prompt %)))
                @dispatches))
    (is (= [stop-line] (mapv first @implementations)))
    (is (= "good456" (get-in @implementations [0 1 :commit])))))

(deftest job-activity-prefers-the-latest-parseable-agency-event
  (let [started "2026-07-14T10:00:00Z"
        latest "2026-07-14T10:02:03.456Z"]
    (is (= (.toEpochMilli (Instant/parse latest))
           (runner/job-last-activity-ms
            {:created-at "not-a-timestamp"
             :started-at started
             :events [{:at "2026-07-14T10:01:00Z"}
                      {:at latest}
                      {:at nil}]})))
    (is (nil? (runner/job-last-activity-ms {:events [{:at "bad"}]})))))

(deftest polling-budget-expiry-suspends-without-interrupting-live-work
  (let [posts (atom [])
        old-event (str (.minusSeconds (Instant/now) 120))]
    (with-redefs [http/get
                  (fn [_ _]
                    {:status 200
                     :body (json/generate-string
                            {:job {:job-id "job-1" :agent-id "zai-5"
                                   :state "running"
                                   :started-at old-event
                                   :events [{:at old-event}]}})})
                  http/post
                  (fn [url opts]
                    (swap! posts conj {:url url :opts opts})
                    {:status 200 :body "{\"ok\":true}"})]
      (let [failure
            (try
              (runner/poll-job! {:agency-base "http://agency"
                                 :agent-budget-ms 1000
                                 :poll-ms 1}
                                "job-1")
              nil
              (catch clojure.lang.ExceptionInfo e e))]
        (is (= :incomplete (:outcome (ex-data failure))))
        (is (= :agent-budget-expired (:failure-kind (ex-data failure))))
        (is (empty? @posts)
            "an untrusted timeout must never destroy live work")))))

(deftest readiness-observation-failure-is-closed-and-remembered
  (let [findings (atom [])
        queued (atom [])
        result
        (runner/run-opportunity!
         {:cohort? false
          :phase-log-fn (fn [_])
          :roster-fn (fn [_] (throw (ex-info "agency unavailable" {})))
          :code-state-fn (fn [] {:repo "/futon2" :git-sha "head"
                                 :git-dirty? false :repo-heads {}})
          :repair-system-record-fn
          (fn [finding]
            (swap! findings conj finding)
            (assoc finding :repair/id "repair-readiness"))
          :queue-fn #(swap! queued conj %)})]
    (is (= :agent-unavailable (:outcome result)))
    (is (= :agent-readiness-failed (get-in result [:data :failure-kind])))
    (is (= :environmental-hold (:repair-class (first @findings))))
    (is (= :none (get-in (first @queued) [:achievement :tier])))
    (is (= #{:selection :construction :dispatch :build :adjudication}
           (set (keys (:checkpoints result)))))))

(deftest flat-leading-g-stops-before-spending-an-agent-turn
  (let [dispatches (atom [])
        findings (atom [])
        flat-action {:type :advance-mission :target "M-flat-a"
                     :open-hole-count 8}
        flat-admissible
        [{:rank 1 :action flat-action :G-efe 4.0
          :controller-score 4.0}
         {:rank 2
          :action {:type :advance-mission :target "M-flat-b"
                   :open-hole-count 9}
          :G-efe 4.0
          :controller-score 4.0}]
        flat-judgement
        (-> judgement
            (assoc :ranked-actions
                   (conj flat-admissible
                         {:rank 3
                          :action {:type :inadmissible :target "mask"}
                          :G-efe 9.0 :controller-score 9.0}))
            (assoc :admissible-actions flat-admissible)
            (assoc :decision {:action flat-action :rank 1
                              :controller-score 4.0}))
        result
        (runner/run-opportunity!
         {:cohort? false
          :phase-log-fn (fn [_])
          :repair-open-fn (constantly [])
          :repair-system-record-fn
          (fn [finding]
            (swap! findings conj finding)
            (assoc finding :repair/id "repair-flat" :repair/status :open))
          :roster-fn (fn [_] {:zai-5 {:status "idle" :invoke-ready? true}
                              :codex-7 {:status "idle" :invoke-ready? true}})
          :judge-fn (fn [_] {:judgement flat-judgement})
          :refresh-fn (fn [])
          :substrate-preflight-fn (fn [_] {:route :test})
          :code-state-fn (fn [] {:repo "/futon2" :git-sha "head"
                                 :git-dirty? false :repo-heads {}})
          :mode-flags-fn (fn [] {})
          :version-stamp-fn identity
          :dispatch-fn (fn [& args] (swap! dispatches conj args))
          :queue-fn identity})]
    (is (= :incomplete (:outcome result)))
    (is (= :policy-nondiscrimination
           (get-in result [:data :failure-kind])))
    (is (empty? @dispatches))
    (is (= :machine-failure (:repair-class (first @findings))))))

(deftest single-candidate-still-requires-finite-g
  (is (false? (:passes? (runner/selection-discrimination
                          [{:action selected-action :G-efe nil}]))))
  (is (false? (:passes? (runner/selection-discrimination
                          [{:action selected-action :G-efe ##NaN}]))))
  (is (true? (:passes? (runner/selection-discrimination
                         [{:action selected-action :G-efe -1.0}])))))

(deftest recoverable-late-author-completion-skips-second-author-turn
  (let [dispatches (atom [])
        resolutions (atom [])
        stop-line {:repair/id "repair-attempt-006-recovery"
                   :repair/status :open
                   :repair/class :incomplete-recoverable
                   :attempt-id "attempt-006"
                   :failure-stage :author-wait
                   :failure-kind :agent-budget-expired
                   :failure-data {:job-id "late-author-job"}}
        result
        (runner/run-opportunity!
         {:cohort? false
          :phase-log-fn (fn [_])
          :repair-open-fn (constantly [stop-line])
          :repair-resolve-fn (fn [obligation resolution]
                               (swap! resolutions conj [obligation resolution]))
          :read-job-fn (fn [_ job-id]
                         {:job-id job-id :state "done"
                          :artifact-ref "late123"
                          :feature-card feature-card-claim
                          :execution successful-execution
                          :result-summary "FULL_LOOP_AUTHOR: DONE late123"})
          :roster-fn (fn [_] {:zai-5 {:status "idle" :invoke-ready? true}
                              :codex-7 {:status "idle" :invoke-ready? true}
                              :codex-1 {:status "idle" :invoke-ready? true}})
          :judge-fn (fn [_] {:judgement judgement})
          :refresh-fn (fn [])
          :substrate-preflight-fn (fn [_] {:route :test})
          :code-state-fn (fn [] {:repo "/futon2" :git-sha "head"
                                 :git-dirty? false :repo-heads {}})
          :mode-flags-fn (fn [] {})
          :version-stamp-fn identity
          :mission-fn (fn [target] {:id target})
          :construct-fn runner/construct-for-decision
          :dispatch-fn
          (fn [_ agent _ _ _]
            (swap! dispatches conj agent)
            {:job-id "review-job"})
          :poll-fn (fn [_ job-id]
                     {:job-id job-id :state "done"
                      :execution successful-execution
                      :result-summary "FULL_LOOP_REVIEW: APPROVE"})
          :resolve-build-fn (fn [_] {:repo "/repo" :files ["src/real.clj"]})
          :ground-fn (fn [& _]
                       {:before {:implementation-entity nil}
                        :after {:implementation-entity {:id "impl"}}
                        :resolved? true :dial-moved? true
                        :implementation-id "impl"})
          :queue-fn identity})]
    (is (= :grounded-change (:outcome result)))
    (is (= ["codex-1"] @dispatches)
        "recovery dispatches only the standing Ground Control reviewer")
    (is (= stop-line (ffirst @resolutions)))
    (is (= :recovered-existing-artifact
           (get-in @resolutions [0 1 :validation :kind])))))

(deftest recoverable-late-review-completion-skips-both-replacement-turns
  (let [dispatches (atom [])
        resolutions (atom [])
        author-job {:job-id "author-job" :state "done"
                    :artifact-ref "late123"
                    :feature-card feature-card-claim
                    :execution successful-execution}
        stop-line {:repair/id "repair-attempt-007-review-recovery"
                   :repair/status :open
                   :repair/class :incomplete-recoverable
                   :attempt-id "attempt-007"
                   :failure-stage :reviewer-wait
                   :failure-kind :agent-budget-expired
                   :failure-data {:job-id "late-review-job"
                                  :author-job author-job
                                  :commit "late123"
                                  :repository "/repo"
                                  :files ["src/real.clj"]}}
        result
        (runner/run-opportunity!
         {:cohort? false
          :phase-log-fn (fn [_])
          :repair-open-fn (constantly [stop-line])
          :repair-resolve-fn (fn [obligation resolution]
                               (swap! resolutions conj [obligation resolution]))
          :read-job-fn (fn [_ job-id]
                         {:job-id job-id :state "done"
                          :execution successful-execution
                          :result-summary "FULL_LOOP_REVIEW: APPROVE"})
          :roster-fn (fn [_] {:zai-5 {:status "idle" :invoke-ready? true}
                              :codex-7 {:status "idle" :invoke-ready? true}
                              :codex-1 {:status "idle" :invoke-ready? true}})
          :judge-fn (fn [_] {:judgement judgement})
          :refresh-fn (fn [])
          :substrate-preflight-fn (fn [_] {:route :test})
          :code-state-fn (fn [] {:repo "/futon2" :git-sha "head"
                                 :git-dirty? false :repo-heads {}})
          :mode-flags-fn (fn [] {})
          :version-stamp-fn identity
          :mission-fn (fn [target] {:id target})
          :construct-fn runner/construct-for-decision
          :dispatch-fn (fn [& args] (swap! dispatches conj args))
          :resolve-build-fn (fn [_] {:repo "/repo" :files ["src/real.clj"]})
          :ground-fn (fn [& _]
                       {:before {:implementation-entity nil}
                        :after {:implementation-entity {:id "impl"}}
                        :resolved? true :dial-moved? true
                        :implementation-id "impl"})
          :queue-fn identity})]
    (is (= :grounded-change (:outcome result)))
    (is (empty? @dispatches))
    (is (= stop-line (ffirst @resolutions)))))

(deftest reviewer-recovery-without-author-provenance-fails-before-dispatch
  (let [dispatches (atom [])
        stop-line {:repair/id "repair-legacy-review"
                   :repair/status :open
                   :repair/class :incomplete-recoverable
                   :attempt-id "legacy-attempt"
                   :failure-stage :reviewer-wait
                   :failure-kind :agent-budget-expired
                   :failure-data {:job-id "late-review-job"}}
        result
        (runner/run-opportunity!
         {:cohort? false
          :phase-log-fn (fn [_])
          :repair-open-fn (constantly [stop-line])
          :repair-system-record-fn
          (fn [finding]
            (assoc finding :repair/id "repair-recovery-provenance"))
          :repair-supersede-fn (fn [& _] {:repair/status :superseded})
          :read-job-fn (fn [_ job-id]
                         {:job-id job-id :state "done"
                          :result-summary "FULL_LOOP_REVIEW: APPROVE"})
          :roster-fn (fn [_] {:zai-5 {:status "idle" :invoke-ready? true}
                              :codex-7 {:status "idle" :invoke-ready? true}
                              :codex-1 {:status "idle" :invoke-ready? true}})
          :judge-fn (fn [_] {:judgement judgement})
          :refresh-fn (fn [])
          :substrate-preflight-fn (fn [_] {:route :test})
          :code-state-fn (fn [] {:repo "/futon2" :git-sha "head"
                                 :git-dirty? false :repo-heads {}})
          :mode-flags-fn (fn [] {})
          :version-stamp-fn identity
          :mission-fn (fn [target] {:id target})
          :construct-fn runner/construct-for-decision
          :dispatch-fn (fn [& args] (swap! dispatches conj args))
          :queue-fn identity})]
    (is (= :incomplete (:outcome result)))
    (is (= :recovery-provenance-missing
           (get-in result [:data :failure-kind])))
    (is (empty? @dispatches))))

(deftest terminal-recovery-job-transitions-to-machine-repair
  (let [successors (atom [])
        supersessions (atom [])
        dispatches (atom [])
        stop-line {:repair/id "repair-dead-job" :repair/status :open
                   :repair/class :incomplete-recoverable
                   :attempt-id "attempt-dead" :failure-stage :author-wait
                   :failure-data {:job-id "dead-job"}}
        result
        (runner/run-opportunity!
         (merge (isolated-runner-opts)
                {:repair-open-fn (constantly [stop-line])
                 :read-job-fn (fn [& _] {:job-id "dead-job"
                                         :state "timed-out"})
                 :repair-system-record-fn
                 (fn [finding]
                   (let [finding (assoc finding :repair/id "repair-dead-successor")]
                     (swap! successors conj finding)
                     finding))
                 :repair-supersede-fn
                 (fn [old successor reason]
                   (swap! supersessions conj [old successor reason]))
                 :dispatch-fn (fn [& args] (swap! dispatches conj args))}))]
    (is (= :incomplete (:outcome result)))
    (is (= :recovery-job-terminal (get-in result [:data :failure-kind])))
    (is (= :machine-failure (:repair-class (first @successors))))
    (is (= :recovery-job-terminal (last (first @supersessions))))
    (is (empty? @dispatches))))

(deftest recovered-review-rejection-hands-line-to-one-review-finding
  (let [findings (atom [])
        supersessions (atom [])
        author-job {:job-id "author-job" :state "done" :artifact-ref "bad123"
                    :feature-card feature-card-claim
                    :execution successful-execution}
        stop-line {:repair/id "repair-review-wait" :repair/status :open
                   :repair/class :incomplete-recoverable
                   :attempt-id "attempt-review-wait"
                   :failure-stage :reviewer-wait
                   :failure-data {:job-id "rejecting-review"
                                  :author-job author-job}}
        result
        (runner/run-opportunity!
         (merge (isolated-runner-opts)
                {:repair-open-fn (constantly [stop-line])
                 :read-job-fn
                 (fn [& _] {:job-id "rejecting-review" :state "done"
                            :execution successful-execution
                            :result-summary
                            "FULL_LOOP_REVIEW: REQUEST_CHANGES\nreal defect"})
                 :resolve-build-fn
                 (fn [_] {:repo "/repo" :files ["src/real.clj"]})
                 :repair-record-fn
                 (fn [finding]
                   (let [finding (assoc finding :repair/id "repair-review-reject")]
                     (swap! findings conj finding)
                     finding))
                 :repair-supersede-fn
                 (fn [& args] (swap! supersessions conj args))
                 :dispatch-fn
                 (fn [& _] (throw (ex-info "must not dispatch" {})))}))]
    (is (= :build-failed (:outcome result)))
    (is (= 1 (count @findings)))
    (is (= 1 (count @supersessions)))
    (is (= "repair-review-reject"
           (get-in result [:data :repair-obligation :repair/id])))))

(deftest done-author-recovery-without-artifact-transitions-before-dispatch
  (let [supersessions (atom [])
        dispatches (atom [])
        stop-line {:repair/id "repair-refusal" :repair/status :open
                   :repair/class :incomplete-recoverable
                   :attempt-id "attempt-refusal" :failure-stage :author-wait
                   :failure-data {:job-id "refusal-job"}}
        result
        (runner/run-opportunity!
         (merge (isolated-runner-opts)
                {:repair-open-fn (constantly [stop-line])
                 :read-job-fn (fn [& _] {:job-id "refusal-job" :state "done"})
                 :repair-system-record-fn
                 #(assoc % :repair/id "repair-refusal-successor")
                 :repair-supersede-fn
                 (fn [& args] (swap! supersessions conj args))
                 :dispatch-fn (fn [& args] (swap! dispatches conj args))}))]
    (is (= :recovery-artifact-missing (get-in result [:data :failure-kind])))
    (is (= 1 (count @supersessions)))
    (is (empty? @dispatches))))

(deftest first-line-review-verdict-cannot-be-overridden-by-later-prose
  (is (= :request-changes
         (#'runner/review-verdict
          {:result-summary
           (str "FULL_LOOP_REVIEW: REQUEST_CHANGES\n"
                "Do not replace this with FULL_LOOP_REVIEW: APPROVE")})))
  (is (= :unverifiable
         (#'runner/review-verdict
          {:result-summary "prose mentions FULL_LOOP_REVIEW: APPROVE only"}))))

(deftest missing-agency-timestamps-still-obey-wall-clock-budget
  (with-redefs [http/get
                (fn [& _]
                  {:status 200
                   :body (json/generate-string
                          {:job {:job-id "job-no-clock" :state "running"}})})]
    (let [failure (try
                    (runner/poll-job! {:agency-base "http://agency"
                                       :agent-budget-ms 1 :poll-ms 1}
                                      "job-no-clock")
                    nil
                    (catch clojure.lang.ExceptionInfo e e))]
      (is (= :agent-budget-expired (:failure-kind (ex-data failure)))))))

(deftest initialization-failure-opens-emergency-stop-line
  (let [findings (atom [])
        queued (atom [])
        result
        (runner/run-opportunity!
         {:cohort? false
          :phase-log nil
          :phase-log-fn (fn [_] (throw (ex-info "phase sink failed" {})))
          :repair-system-record-fn
          (fn [finding]
            (swap! findings conj finding)
            (assoc finding :repair/id "repair-initialization"))
          :queue-fn #(swap! queued conj %)})]
    (is (= :incomplete (:outcome result)))
    (is (= :initialization-failed (get-in result [:data :failure-kind])))
    (is (= :machine-failure (:repair-class (first @findings))))
    (is (= :none (get-in (first @queued) [:achievement :tier])))))

;; ---------------------------------------------------------------------------
;; Bounded build-cure loop: a mechanically-detectable, author-curable build
;; failure bounces back to the SAME author with the exact error instead of
;; burning the whole click. The card gate and review gate are NOT weakened:
;; a bounce is a bounded cure window inside the attempt, not fail-open.
;; ---------------------------------------------------------------------------

(deftest card-failure-cured-on-first-bounce-grounds-the-change
  (let [{:keys [result]}
        (run-feature-card-attempt
         {:author-card nil
          :cure-card feature-card-claim
          :artifacts? true})]
    (is (= :grounded-change (:outcome result)))
    (is (= :feature-card-missing-or-invalid
           (get-in result [:data :build-retries 0 :failure-kind])))
    (is (= true (get-in result [:data :build-retries 0 :cured?])))
    (is (= 1 (count (:build-retries (:data result)))))))

(deftest card-failure-not-cured-fails-after-exhausting-retries
  (let [{:keys [result]}
        (run-feature-card-attempt
         {:author-card nil
          :cure-card nil})]
    (is (= :build-failed (:outcome result)))
    (is (= :feature-card-missing-or-invalid
           (get-in result [:data :failure-kind])))
    (is (= :marker-not-at-durable-prefix
           (get-in result [:data :feature-card-invalid-reason])))
    (is (= :text
           (get-in result [:data :feature-card-source])))
    (is (= false (get-in result [:data :build-retries 0 :cured?])))
    (is (= 1 (count (:build-retries (:data result)))))))

(deftest reviewer-request-changes-is-not-bounced
  ;; A reviewer REQUEST_CHANGES must NOT trigger a cure dispatch. The cure
  ;; loop wraps only artifact-only and feature-card checks — never the
  ;; reviewer gate.
  (let [{:keys [result dispatches]}
        (run-feature-card-attempt
         {:author-card feature-card-claim
          :reviewer-execution {:executed false :tool-events 0
                               :command-events 0}})]
    (is (= :build-failed (:outcome result)))
    (is (= :review-execution-evidence-missing
           (get-in result [:data :failure-kind])))
    ;; Author dispatched once, reviewer dispatched once — no cure dispatch.
    (is (= 2 (count dispatches)))
    (is (= ["zai-5" "codex-7"] dispatches))))

(deftest build-cure-retries-zero-reproduces-todays-behavior
  (let [{:keys [result dispatches]}
        (run-feature-card-attempt
         {:author-card nil
          :build-cure-retries 0})]
    (is (= :build-failed (:outcome result)))
    (is (= :feature-card-missing-or-invalid
           (get-in result [:data :failure-kind])))
    ;; No cure dispatch — only the initial author dispatch.
    (is (= 1 (count dispatches)))
    (is (= ["zai-5"] dispatches))
    ;; No build-retries recorded when retries is 0.
    (is (empty? (:build-retries (:data result))))))

(deftest artifact-only-failure-bounces-and-substantive-cure-commits-grounds
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                       "wm-artifact-cure-"
                       (make-array java.nio.file.attribute.FileAttribute 0)))
        queued (atom [])
        dispatches (atom [])
        _ (spit (io/file root "M-selected.md") "# test mission\n")
        mission-file (io/file root "M-selected.md")
        fold-file (io/file root "M-selected.executed.edn")
        proof-file (io/file root "logic/feature-witness.edn")
        _ (io/make-parents fold-file)
        _ (spit fold-file (pr-str {:boxes [:author :store]
                                   :want-coverage [:feature-card]}))
        _ (io/make-parents proof-file)
        _ (spit proof-file (pr-str {:witness :feature-card-persisted}))
        resolve-call (atom 0)
        result
        (runner/run-opportunity!
         (merge
          (isolated-runner-opts)
          {:repair-open-fn (constantly [])
           :target-repo-fn (fn [& _] (.getPath root))
           :repair-system-record-fn
           (fn [finding] (assoc finding :repair/id "test-repair"))
           :mission-fn (fn [target] {:id target :path (.getPath mission-file)})
           :trace-fn (constantly (.getPath (io/file root "trace.edn")))
           :author-artifact-observer-fn
           (fn [repo before author-job]
             {:fresh-author? true
              :repo repo
              :pre-dispatch-head (:head before)
              :observed-head (:artifact-ref author-job)
              :corroborates? true
              :commit (:artifact-ref author-job)})
           :dispatch-fn
           (fn [_ agent _ _ _]
             (swap! dispatches conj agent)
             {:job-id (if (= agent "zai-5")
                        (if (> (count (filter #(= "zai-5" %) @dispatches)) 1)
                          "cure-job"
                          "author-job")
                        "review-job")})
           :poll-fn
           (fn [_ job-id]
             (case job-id
               "author-job"
               {:job-id job-id :state "done" :artifact-ref "bad-commit"
                :feature-card feature-card-claim
                :result-summary "FULL_LOOP_AUTHOR: DONE bad-commit"
                :execution successful-execution}
               "cure-job"
               {:job-id job-id :state "done" :artifact-ref "good-commit"
                :feature-card feature-card-claim
                :result-summary "FULL_LOOP_AUTHOR: DONE good-commit"
                :execution successful-execution}
               ;; reviewer
               {:job-id job-id :state "done"
                :execution successful-execution
                :result-summary
                (str "FULL_LOOP_REVIEW: APPROVE\n"
                     "FULL_LOOP_REVIEWER_NOTE: Replay steps verified.")}))
           :resolve-build-fn
           (fn [_]
             ;; First resolution (bad-commit): artifact-only files.
             ;; Second resolution (good-commit): substantive files.
             (swap! resolve-call inc)
             (if (= @resolve-call 1)
               {:repo (.getPath root)
                :files ["data/fold-turns/selection-authoring-flights.edn"]}
               {:repo (.getPath root)
                :files ["src/feature.clj" "logic/feature-witness.edn"]}))
           :ground-fn
           (fn [& _]
             {:before {:implementation-entity nil}
              :after {:implementation-entity {:id "feature-impl"}}
              :resolved? true :dial-moved? true
              :implementation-id "feature-impl"
              :discharge-id "feature-discharge"})
           :queue-fn #(swap! queued conj %)}))]
    (is (= :grounded-change (:outcome result)))
    (is (= :artifact-only
           (get-in result [:data :build-retries 0 :failure-kind])))
    (is (= true (get-in result [:data :build-retries 0 :cured?])))
    (is (= 1 (count (:build-retries (:data result)))))))
