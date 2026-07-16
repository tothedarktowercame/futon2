(ns futon2.aif.full-loop-runner-test
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.pattern-registry :as patterns]
            [futon2.report.cascade-lane :as cascade])
  (:import [java.time Instant]))

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

(defn isolated-runner-opts []
  {:cohort? false
   :phase-log-fn (fn [_])
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
   :construct-fn runner/construct-for-decision
   :queue-fn identity})

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
                              :codex-7 {:status "idle" :invoke-ready? true}})
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
          :dispatch-fn (fn [_ agent _ _ prompt]
                         (swap! dispatches conj {:agent agent :prompt prompt})
                         {:job-id (if (= agent "zai-5") "author-job" "review-job")})
          :poll-fn (fn [_ job-id]
                     (if (= job-id "author-job")
                       {:job-id job-id :state "done" :artifact-ref "abc123"
                        :events [{:text "FULL_LOOP_AUTHOR: DONE abc123"}]}
                       {:job-id job-id :state "done"
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
    (is (re-find #":action-proposer-registration" prompt))))

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
          :dispatch-fn (fn [_ agent _ _ prompt]
                         (swap! dispatches conj {:agent agent :prompt prompt})
                         {:job-id (if (= agent "zai-5")
                                    "author-job" "review-job")})
          :poll-fn (fn [_ job-id]
                     (if (= job-id "author-job")
                       {:job-id job-id :state "done" :artifact-ref "fire123"}
                       {:job-id job-id :state "done"
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
          :dispatch-fn (fn [_ agent _ _ _]
                         {:job-id (if (= agent "zai-5")
                                    "author-job" "review-job")})
          :poll-fn (fn [_ job-id]
                     (if (= job-id "author-job")
                       {:job-id job-id :state "done" :artifact-ref "abc123"}
                       {:job-id job-id :state "done"
                        :result-summary
                        "FULL_LOOP_REVIEW: REQUEST_CHANGES fail closed"}))
          :resolve-build-fn (fn [_] {:repo "/repo" :files ["src/real.clj"]})
          :queue-fn #(swap! queued conj %)})]
    (is (= :build-failed (:outcome result)))
    (is (= "abc123" (get-in result [:data :commit])))
    (is (= "abc123" (:commit (first @queued))))
    (is (= :request-changes (:review-verdict (first @findings))))))

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
          :construct-fn (fn [entry]
                          {:mission (get-in entry [:action :target])
                           :shown [:P1] :psi :psi :cascade-score 1.0
                           :semilattice [] :policy-holes []})
          :dispatch-fn (fn [_ agent _ target prompt]
                         (swap! dispatches conj {:agent agent :target target
                                                :prompt prompt})
                         {:job-id (if (= agent "zai-5")
                                    "author-job" "review-job")})
          :poll-fn (fn [_ job-id]
                     (if (= job-id "author-job")
                       {:job-id job-id :state "done" :artifact-ref "good456"}
                       {:job-id job-id :state "done"
                        :result-summary "FULL_LOOP_REVIEW: APPROVE"}))
          :resolve-build-fn (fn [_] {:repo "/repo" :files ["src/real.clj"]})
          :ground-fn (fn [& _]
                       {:resolved? true :dial-moved? true
                        :implementation-id "impl"})
          :queue-fn identity})]
    (is (= :grounded-change (:outcome result)))
    (is (= ["repair-failed-1" "repair-failed-1"]
           (mapv :target @dispatches)))
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
        flat-judgement
        (-> judgement
            (assoc :ranked-actions
                   [{:rank 1 :action flat-action :G-efe 4.0
                     :controller-score 4.0}
                    {:rank 2
                     :action {:type :advance-mission :target "M-flat-b"
                              :open-hole-count 9}
                     :G-efe 4.0
                     :controller-score 4.0}])
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
                          :result-summary "FULL_LOOP_AUTHOR: DONE late123"})
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
          :construct-fn runner/construct-for-decision
          :dispatch-fn
          (fn [_ agent _ _ _]
            (swap! dispatches conj agent)
            {:job-id "review-job"})
          :poll-fn (fn [_ job-id]
                     {:job-id job-id :state "done"
                      :result-summary "FULL_LOOP_REVIEW: APPROVE"})
          :resolve-build-fn (fn [_] {:repo "/repo" :files ["src/real.clj"]})
          :ground-fn (fn [& _]
                       {:before {:implementation-entity nil}
                        :after {:implementation-entity {:id "impl"}}
                        :resolved? true :dial-moved? true
                        :implementation-id "impl"})
          :queue-fn identity})]
    (is (= :grounded-change (:outcome result)))
    (is (= ["codex-7"] @dispatches)
        "recovery dispatches only the independent reviewer")
    (is (= stop-line (ffirst @resolutions)))
    (is (= :recovered-existing-artifact
           (get-in @resolutions [0 1 :validation :kind])))))

(deftest recoverable-late-review-completion-skips-both-replacement-turns
  (let [dispatches (atom [])
        resolutions (atom [])
        author-job {:job-id "author-job" :state "done"
                    :artifact-ref "late123"}
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
                          :result-summary "FULL_LOOP_REVIEW: APPROVE"})
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
                              :codex-7 {:status "idle" :invoke-ready? true}})
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
        author-job {:job-id "author-job" :state "done" :artifact-ref "bad123"}
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
