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
                     :controller-score -2.0}
                    {:rank 2 :action selected-action :controller-score -1.0}]
   :decision {:action selected-action :rank 2 :source :habit-prior}
   :belief {} :belief-pre {} :observation {} :free-energy {}
   :prediction-errors {} :precision-state {} :micro-step-trace []
   :ranked-actions-extra [] :mode :maintain})

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
                   (assoc finding :repair/class :system-actuation-failure))
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
    (is (= :construction-failed (:outcome result)))
    (is (empty? @traces) "unsupported selection must not train the habit trace")
    (is (= :system-actuation-failure
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

(deftest stop-line-obligation-overrides-ordinary-selection-and-closes-after-grounding
  (let [dispatches (atom [])
        resolutions (atom [])
        stop-line {:repair/id "repair-failed-1"
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
          :repair-resolve-fn (fn [obligation resolution]
                               (swap! resolutions conj [obligation resolution]))
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
    (is (= [:sorry/g2 :sorry/g2] (mapv :target @dispatches)))
    (is (every? #(re-find #"STOP-THE-LINE" (:prompt %)) @dispatches))
    (is (every? #(re-find #"caller-controlled identity is spoofable" (:prompt %))
                @dispatches))
    (is (every? #(not (re-find #"unrelated finding" (:prompt %))) @dispatches))
    (is (= [stop-line follow-up] (mapv first @resolutions)))
    (is (= "good456" (get-in @resolutions [0 1 :commit])))))

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

(deftest polling-stalled-job-requests-interrupt-and-fails-typed
  (let [posts (atom [])
        old-event (str (.minusSeconds (Instant/now) 120))]
    (with-redefs [http/get
                  (fn [_ _]
                    {:status 200
                     :body (json/generate-string
                            {:job {:job-id "job-1" :agent-id "zai-5"
                                   :state "running"
                                   :events [{:at old-event}]}})})
                  http/post
                  (fn [url opts]
                    (swap! posts conj {:url url :opts opts})
                    {:status 200 :body "{\"ok\":true}"})]
      (try
        (runner/poll-job! {:agency-base "http://agency"
                           :inactivity-timeout-ms 1000
                           :poll-ms 1}
                          "job-1")
        (is false "stalled jobs must not remain in the polling loop")
        (catch clojure.lang.ExceptionInfo e
          (is (= :agent-job-stalled (:outcome (ex-data e))))
          (is (= 200 (get-in (ex-data e) [:interrupt :status])))))
      (is (= ["http://agency/api/alpha/agents/zai-5/interrupt-invoke"]
             (mapv :url @posts))))))
