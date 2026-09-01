(ns futon2.aif.full-loop-runner-test
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.full-loop-cli :as cli]
            [futon2.aif.delivery-qa :as delivery-qa]
            [futon2.aif.full-loop-cohort :as cohort]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.pattern-registry :as patterns]
            [futon2.aif.repair-obligation :as repair]
            [futon2.aif.tripwire :as tripwire]
            [futon2.report.cascade-lane :as cascade]
            [futon2.report.war-machine :as wm])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]
           [java.time Instant]))

(use-fixtures :once hermetic/with-hermetic-stores)

(defn- without-live-wm-status
  [f]
  (binding [runner/*wm-status-reporting?* false]
    (f)))

(defn- with-field-desk-stub
  [f]
  (with-redefs [delivery-qa/emit!
                (fn [_ item]
                  {:morning-brief/addendum-id
                   (str "qa-" (:attempt-id item))})]
    (f)))

(use-fixtures :each with-field-desk-stub without-live-wm-status)

(deftest run-opportunity-surfaces-stable-run-identity
  (let [record-dir (.getPath
                    (.toFile
                     (Files/createTempDirectory
                      "wm-run-record-test-" (make-array FileAttribute 0))))]
    (with-redefs-fn
      {#'runner/run-opportunity-core!
       (fn [_] {:attempt-id "attempt-run-port" :outcome :grounded-change
                :trace-path "/tmp/observed-trace"
                :wm/route [{:node :R20 :via "scan" :at "2026-08-31T00:00:00Z"}
                           {:node :R12 :via "observe" :at "2026-08-31T00:00:01Z"}]})}
      (fn []
       (let [assigned (runner/run-opportunity! {:run-id "run-assigned"
                                                :click-id "click-assigned"
                                                :run-record-dir record-dir})
            generated (runner/run-opportunity! {:run-record-dir record-dir})
            generated-again (runner/run-opportunity! {:run-record-dir record-dir})
            record (edn/read-string (slurp (:run-record assigned)))]
        (is (= "run-assigned" (:run/id assigned)))
        (is (string? (:run/id generated)))
        (is (not (str/blank? (:run/id generated))))
        (is (not= (:run/id generated) (:run/id generated-again))
            "identical production-shaped invocations receive distinct occurrence ids")
        (is (= :present (:run-record-status assigned)))
        (is (= "run-assigned" (:run/id record)))
        (is (= "click-assigned" (:click/id record)))
        (is (= [{:fromNode "R20" :toNode "R12"
                 :via "observe" :at_ "2026-08-31T00:00:01Z"}]
               (:route record))))))))

(deftest production-repair-root-is-unreachable-during-runner-suite
  (is (not= hermetic/production-repair-root repair/default-root))
  (is (not= hermetic/production-trip-root tripwire/default-trip-root)))

(deftest standalone-runner-uses-the-serving-strategic-selector
  (let [seen (atom nil)
        expected {:status :verified-live-selection
                  :selected-mission-ids ["M-shared-memory-control-build-test"]}]
    (with-redefs
      [http/post
       (fn [url opts]
         (reset! seen {:url url
                       :body (json/parse-string (:body opts) true)})
         {:status 200
          :body (json/generate-string {:ok true :selection expected})})]
      (is (= expected
             (runner/strategic-selection!
              {:agency-base "http://127.0.0.1:7070"}
              {:scheduler-habit-ranking ["M-a"]})))
      (is (= "http://127.0.0.1:7070/api/alpha/war-machine/strategic-selection"
             (:url @seen)))
      (is (= ["M-a"]
             (get-in @seen [:body :scheduler-habit-ranking]))))))

(deftest cancellation-command-uses-agency-single-finalizer-vocabulary
  (let [seen (atom nil)]
    (with-redefs [http/post
                  (fn [url opts]
                    (reset! seen {:url url
                                  :body (json/parse-string (:body opts) true)})
                    {:status 200
                     :body (json/generate-string
                            {:ok true :job-id "job-cancel-control"
                             :state "cancelled"
                             :terminal-code "operator-cancelled"})})]
      (is (= "cancelled"
             (:state (runner/cancel-job! {:agency-base "http://agency"}
                                         "job-cancel-control" "joe" "stop"))))
      (is (= "http://agency/api/alpha/invoke/jobs/job-cancel-control/cancel"
             (:url @seen)))
      (is (= {:caller "joe" :reason "stop"} (:body @seen))))))

(deftest cancelled-and-failed-jobs-remain-distinct
  (let [cancelled (try
                    (runner/throw-if-cancelled!
                     {:job-id "job-cancel-control" :state "cancelled"
                      :terminal-code "operator-cancelled"}
                     :author-wait)
                    nil
                    (catch clojure.lang.ExceptionInfo e e))]
    (is (= :cancelled (:outcome (ex-data cancelled))))
    (is (= :operator-cancelled (:failure-kind (ex-data cancelled))))
    (is (= {:job-id "job-failure-control" :state "failed"
            :terminal-code "invoke-exception"}
           (runner/throw-if-cancelled!
            {:job-id "job-failure-control" :state "failed"
             :terminal-code "invoke-exception"}
            :author-wait)))))

(deftest strategic-selection-escalates-and-pins-retry-budgets
  (let [timeouts (atom [])
        sleeps (atom [])
        failure
        (with-redefs
          [http/post
           (fn [_ opts]
             (swap! timeouts conj (:timeout opts))
             (throw (ex-info "selection timed out" {})))]
          (try
            (runner/strategic-selection!
             {:agency-base "http://test"
              :strategic-selection-sleep-fn #(swap! sleeps conj %)}
             {:scheduler-habit-ranking ["M-a"]})
            nil
            (catch clojure.lang.ExceptionInfo e e)))]
    (is (= [150000 210000 270000] @timeouts))
    (is (= [5000 5000] @sleeps))
    (is (= :strategic-selection-unavailable
           (:failure-kind (ex-data failure))))
    (is (= :transient-exhausted (:failure-detail (ex-data failure)))))
  (let [timeouts (atom [])]
    (with-redefs
      [http/post
       (fn [_ opts]
         (swap! timeouts conj (:timeout opts))
         (throw (ex-info "selection timed out" {})))]
      (try
        (runner/strategic-selection!
         {:agency-base "http://test"
          :strategic-selection-timeout-ms 120000
          :strategic-selection-sleep-fn (fn [_])}
         {:scheduler-habit-ranking ["M-a"]})
        (catch clojure.lang.ExceptionInfo _)))
    (is (= [120000 120000 120000] @timeouts)
        "an explicit selection timeout pins all attempts")))

(deftest strategic-selection-invoke-seam-retries-without-http
  (let [calls (atom [])
        sleeps (atom [])
        request {:scheduler-habit-ranking ["M-a"] :trace-id "trace-a"}
        expected {:status :verified-live-selection
                  :selected-mission-ids ["M-a"]}
        invoke-fn
        (fn [payload]
          (swap! calls conj payload)
          (if (= 1 (count @calls))
            (throw (ex-info "in-process timeout" {}))
            {:ok true :selection expected}))
        recovered
        (with-redefs [http/post
                      (fn [& _]
                        (throw (ex-info "HTTP must not be called" {})))]
          (runner/strategic-selection!
           {:strategic-selection-invoke-fn invoke-fn
            :strategic-selection-sleep-fn #(swap! sleeps conj %)}
           request))]
    (is (= [request request] @calls)
        "the injected seam receives the unchanged HTTP request payload")
    (is (= [5000] @sleeps))
    (is (true? (:readiness/selection-transient recovered))))
  (let [result
        (runner/strategic-selection!
         {:strategic-selection-invoke-fn
          (fn [_]
            {:ok true
             :selection {:status :verified-live-selection
                         :selected-mission-ids ["M-a"]}})
          :strategic-selection-sleep-fn
          (fn [_] (throw (ex-info "first success must not sleep" {})))}
         {:scheduler-habit-ranking ["M-a"]})]
    (is (not (contains? result :readiness/selection-transient))
        "first-attempt success does not acquire the transient marker")))

(deftest strategic-selection-invoke-seam-enforces-budgets
  (let [calls (atom 0)
        failure
        (try
          (runner/strategic-selection!
           {:strategic-selection-timeout-ms 100
            :strategic-selection-invoke-fn
            (fn [_]
              (swap! calls inc)
              (Thread/sleep 60000))
            :strategic-selection-sleep-fn (fn [_])}
           {:scheduler-habit-ranking ["M-a"]})
          nil
          (catch clojure.lang.ExceptionInfo e e))]
    (is (= 3 @calls))
    (is (= :strategic-selection-unavailable
           (:failure-kind (ex-data failure))))
    (is (= 100 (get-in (ex-data failure) [:timeout-ms]))
        "the explicit per-attempt budget applies to the injected path")))

(deftest attempt-052-recheck-503-is-transient-and-carries-evidence
  ;; attempt-052 (2026-07-25): the serving projection cache failed its
  ;; immediate recheck, the endpoint returned a typed 503, and the click died
  ;; on the single shot. The 503 is transient — retried on the ladder — and
  ;; an exhausted ladder carries each attempt's typed evidence rather than
  ;; only the last throw.
  (let [recheck-503
        {:status 503
         :body (json/generate-string
                {:ok false
                 :err "strategic-selection-failed"
                 :message
                 "serving projection cache failed its immediate recheck"})}
        ok {:status 200
            :body (json/generate-string
                   {:ok true
                    :selection {:status "verified-live-selection"
                                :selected-mission-ids ["M-a"]}})}
        calls (atom 0)]
    (testing "the recheck 503 recovers on a later ladder attempt"
      (let [result
            (with-redefs [http/post (fn [_ _]
                                      (swap! calls inc)
                                      (if (< @calls 2) recheck-503 ok))]
              (runner/strategic-selection!
               {:agency-base "http://test"
                :strategic-selection-sleep-fn (fn [_])}
               {:scheduler-habit-ranking ["M-a"]}))]
        (is (= 2 @calls))
        (is (true? (:readiness/selection-transient result)))
        (is (= :verified-live-selection (:status result)))))
    (testing "an exhausted 503 ladder carries per-attempt evidence"
      (let [failure
            (with-redefs [http/post (fn [_ _] recheck-503)]
              (try
                (runner/strategic-selection!
                 {:agency-base "http://test"
                  :strategic-selection-sleep-fn (fn [_])}
                 {:scheduler-habit-ranking ["M-a"]})
                nil
                (catch clojure.lang.ExceptionInfo e (ex-data e))))]
        (is (= :transient-exhausted (:failure-detail failure)))
        (is (= [503 503 503] (mapv :status (:attempt-failures failure))))
        (is (= [150000 210000 270000]
               (mapv :timeout-ms (:attempt-failures failure))))
        (is (= "serving projection cache failed its immediate recheck"
               (:message (first (:attempt-failures failure)))))))))

(deftest deterministic-selection-rejection-fails-fast
  ;; A request the endpoint deterministically rejects (non-transient HTTP
  ;; status, or a malformed response body) must not replay across the
  ;; escalating ladder: the identical request fails identically, and the
  ;; ladder burns up to ~10.5 minutes reproducing one known failure. Fail
  ;; closed immediately with the typed detail.
  (testing "a 4xx rejection is not retried"
    (let [calls (atom 0)
          failure
          (with-redefs [http/post
                        (fn [_ _]
                          (swap! calls inc)
                          {:status 400
                           :body (json/generate-string
                                  {:ok false :err "bad-request"})})]
            (try
              (runner/strategic-selection!
               {:agency-base "http://test"
                :strategic-selection-sleep-fn
                (fn [_] (throw (ex-info "must not sleep" {})))}
               {:scheduler-habit-ranking ["M-a"]})
              nil
              (catch clojure.lang.ExceptionInfo e (ex-data e))))]
      (is (= 1 @calls))
      (is (= :strategic-selection-unavailable (:failure-kind failure)))
      (is (= :deterministic-rejection (:failure-detail failure)))
      (is (= 1 (:attempts failure)))
      (is (= [400] (mapv :status (:attempt-failures failure))))))
  (testing "an invalid response shape is not retried"
    (let [calls (atom 0)
          failure
          (try
            (runner/strategic-selection!
             {:strategic-selection-invoke-fn
              (fn [_]
                (swap! calls inc)
                {:ok true :selection "not-a-map"})
              :strategic-selection-sleep-fn
              (fn [_] (throw (ex-info "must not sleep" {})))}
             {:scheduler-habit-ranking ["M-a"]})
            nil
            (catch clojure.lang.ExceptionInfo e (ex-data e)))]
      (is (= 1 @calls))
      (is (= :deterministic-rejection (:failure-detail failure)))
      (is (true? (:selection-response-invalid failure))))))

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
   :delivery-qa-fn
   (fn [_ item]
     {:morning-brief/addendum-id
      (str "qa-" (:attempt-id item))})
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
           reviewer-events cure-card cure-summary cure-commit build-cure-retries
           cure-observed-commit
           initial-author-job operator-actions delivery-qa-fn
           judgement-transform-fn]
    :or {grounded? true artifacts? false
         cure-observed-commit ::from-artifact-ref}}]
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                       "wm-feature-card-" (make-array java.nio.file.attribute.FileAttribute 0)))
        mission-file (io/file root "M-selected.md")
        fold-file (io/file root "M-selected.executed.edn")
        proof-file (io/file root "logic/feature-witness.edn")
        queued (atom [])
        queued-operator-actions (atom [])
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
               :judge-fn (fn [_]
                           {:judgement (assoc judgement
                                              :operator-actions
                                              (vec operator-actions))})
               :operator-gate-queue-fn
               (fn [operator-action]
                 (swap! queued-operator-actions conj operator-action)
                 {:status :queued :mission (:mission operator-action)})
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
                  :commit (if (and (= cure-id (:job-id author-job))
                                   (not= ::from-artifact-ref
                                         cure-observed-commit))
                            cure-observed-commit
                            (:artifact-ref author-job))})
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
                   (or initial-author-job
                       (cond-> {:job-id job-id :state "done" :artifact-ref commit
                                :result-summary (or author-summary
                                                    (str "FULL_LOOP_AUTHOR: DONE " commit))
                                :execution successful-execution}
                         author-card (assoc :feature-card author-card)))
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
                {:build-cure-retries build-cure-retries})
              (when delivery-qa-fn
                {:delivery-qa-fn delivery-qa-fn})
              (when judgement-transform-fn
                {:judgement-transform-fn judgement-transform-fn}))
        result (runner/run-opportunity! opts)]
    {:result result :item (first @queued)
     :queued-operator-actions @queued-operator-actions
     :dispatches @dispatches
     :fold-file fold-file :proof-file proof-file}))

(deftest judge-operator-actions-queue-at-the-runner-persistence-boundary
  (let [gate {:type :mission-gate
              :mission "M-learning-loop"
              :gate-kind "operator-acceptance"
              :gate-text "Joe accepts the rendered graph"
              :date "2026-07-22"}
        {:keys [result queued-operator-actions]}
        (run-feature-card-attempt
         {:author-card feature-card-claim :operator-actions [gate]})]
    (is (= :grounded-change (:outcome result)))
    (is (= [gate] queued-operator-actions))))

(deftest opt-in-judgement-transform-runs-inside-the-selection-phase
  (let [seen (atom nil)
        {:keys [result]}
        (run-feature-card-attempt
         {:author-card feature-card-claim
          :judgement-transform-fn
          (fn [incoming]
            (reset! seen incoming)
            (assoc incoming :instrumented/campaign? true))})]
    (is (= (assoc judgement :operator-actions []) @seen))
    (is (= :grounded-change (:outcome result)))))

(deftest delivered-commit-cannot-close-without-field-desk-qa
  (is (thrown-with-msg?
       clojure.lang.ExceptionInfo
       #"Delivery closed without Field Desk QA notes"
       (run-feature-card-attempt
        {:author-card feature-card-claim
         :delivery-qa-fn
         (fn [& _]
           (throw (ex-info "Field Desk unavailable" {})))}))))

(deftest invoke-exception-author-job-is-retried-once
  ;; Replays attempt-043: Agency rejected transcript persistence after tool
  ;; use. The failed job cannot count as work; a fresh dispatch must still
  ;; produce a repository-observed commit before review can proceed.
  (let [{:keys [result dispatches]}
        (run-feature-card-attempt
         {:author-card feature-card-claim
          :cure-card feature-card-claim
          :initial-author-job
          {:job-id "feature-author"
           :state "failed"
           :artifact-ref nil
           :terminal-code "invoke-exception"
           :terminal-message "ZAI transcript persistence was rejected"
           :events [{:type "failed" :code "invoke-exception"}]}})]
    (is (= :grounded-change (:outcome result)))
    (is (= ["zai-5" "zai-5" "codex-7"] dispatches)
        "one replacement author is dispatched before independent review")
    (is (= "feature-author"
           (get-in result [:data :author-job :author-retries 0 :job-id])))))

(deftest retry-prompt-and-artifact-gate-share-the-fresh-head
  (let [head-observations (atom ["base-initial" "base-retry"])
        author-prompts (atom [])
        artifact-observer-inputs (atom [])
        findings (atom [])
        result
        (runner/run-opportunity!
         (merge
          (isolated-runner-opts)
          {:repair-open-fn (constantly [])
           :trace-fn (constantly "/tmp/retry-repository-binding-trace.edn")
           :construct-fn (fn [_] {:shown [] :policy-holes []})
           :target-repo-fn (fn [& _] "/repo")
           :repo-head-observation-fn
           (fn [repo]
             (let [head (first @head-observations)]
               (swap! head-observations subvec 1)
               {:repo repo :head head :observed-at-ms 1000}))
           :author-artifact-observer-fn
           (fn [repo before author-job]
             (swap! artifact-observer-inputs conj
                    {:repo repo :before before :author-job author-job})
             {:fresh-author? true :repo repo
              :pre-dispatch-head (:head before)
              :observed-head (:head before)
              :text-artifact-ref (:artifact-ref author-job)
              :corroborates? false :disagreement? false :commit nil})
           :dispatch-fn
           (fn [_ agent _caller _target prompt]
             (if (= agent "zai-5")
               (let [n (inc (count @author-prompts))]
                 (swap! author-prompts conj prompt)
                 {:job-id (str "author-" n)})
               {:job-id "unexpected-reviewer"}))
           :poll-fn
           (fn [_ job-id]
             (case job-id
               "author-1" {:job-id job-id :state "failed"
                            :terminal-code "invoke-exception"
                            :artifact-ref nil}
               ;; sha-SHAPED but from the wrong repo: this test is about
               ;; observation failing to validate a claimed commit, not about a
               ;; ref that is not a commit at all (see
               ;; agency-artifact-ref-that-is-not-a-commit-is-typed-malformed).
               "author-2" {:job-id job-id :state "done"
                            :artifact-ref "beefcafe1234567890abcdef1234567890abcdef"}
               {:job-id job-id :state "done"}))
           :repair-system-record-fn
           (fn [finding]
             (swap! findings conj finding)
             (assoc finding :repair/id "repair-retry-binding"))}))]
    (is (= :build-failed (:outcome result)))
    (is (= :artifact-binding-mismatch
           (get-in result [:data :failure-kind])))
    (is (= 2 (count @author-prompts)))
    (is (re-find #"TARGET REPOSITORY BASE HEAD: \"base-initial\""
                 (first @author-prompts)))
    (is (re-find #"TARGET REPOSITORY BASE HEAD: \"base-retry\""
                 (second @author-prompts)))
    (is (= "base-retry"
           (get-in @artifact-observer-inputs [0 :before :head])))
    (is (= :artifact-binding-mismatch
           (:failure-kind (first @findings))))))

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

(deftest complete-durable-result-cures-summary-truncation
  ;; attempt-051 (2026-07-25): the author's valid card exceeded the 220-char
  ;; whitespace-collapsed :result-summary window, and the cure reply put
  ;; prose before the marker. Agency durably stores and serves the complete
  ;; reply in :result; the parser now falls back to a line-anchored search
  ;; there before reporting a typed failure.
  (let [card-line (str "FULL_LOOP_FEATURE_CARD: "
                       "{:built \"cohort stopping-rule exception now returns "
                       ":cohort-complete instead of a spurious repair obligation\" "
                       ":want-coverage \"stopping rule fires without depositing "
                       "a repair obligation or morning-brief item\" "
                       ":matches-intent? true "
                       ":things-to-try [\"exhaust cohort -> :cohort-complete\"]}")
        truncated-summary (subs card-line 0 200)]
    (testing "verbose-but-valid card recovered from :result despite summary truncation"
      (let [{:keys [card source]}
            (#'runner/feature-card-validation
             {:result-summary truncated-summary
              :result (str card-line "\nFULL_LOOP_AUTHOR: DONE e8b24fa")})]
        (is (some? card))
        (is (= :result source))
        (is (str/starts-with? (:built card) "cohort stopping-rule"))))
    (testing "cure-shaped reply with prose before a line-anchored marker"
      (let [{:keys [card source]}
            (#'runner/feature-card-validation
             {:result-summary "The commit is already valid. Re-emitting a shorter card:"
              :result (str "The commit is already valid. Re-emitting a shorter card:\n"
                           card-line "\nFULL_LOOP_AUTHOR: DONE e8b24fa")})]
        (is (some? card))
        (is (= :result source))))
    (testing "a marker quoted mid-sentence is not line-anchored and does not match"
      (is (= :marker-not-at-durable-prefix
             (:reason (#'runner/feature-card-validation
                       {:result-summary "prose only"
                        :result (str "the validator looks for "
                                     "FULL_LOOP_FEATURE_CARD: {:built \"x\"} "
                                     "in the reply")})))))))

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

(deftest attempt-051-verbose-card-grounds-without-a-cure-bounce
  ;; Successor validation for repair-attempt-051-feature-card-missing-or-invalid:
  ;; replay the failing author-job shape end to end — a valid card whose closing
  ;; brace fell past the 200-char summary window, with the complete reply
  ;; durable in :result. The attempt must ground without burning a cure bounce.
  (let [card-line (str "FULL_LOOP_FEATURE_CARD: "
                       "{:built \"cohort stopping-rule exception now returns "
                       ":cohort-complete instead of a spurious repair obligation\" "
                       ":want-coverage \"stopping rule fires without depositing a "
                       "repair obligation or morning-brief item\" "
                       ":matches-intent? true "
                       ":things-to-try [\"exhaust cohort -> :cohort-complete\"]}")
        reply (str card-line "\nFULL_LOOP_AUTHOR: DONE feature123")
        {:keys [result item dispatches]}
        (run-feature-card-attempt
         {:initial-author-job
          {:job-id "feature-author" :state "done" :artifact-ref "feature123"
           :result-summary (subs reply 0 200)
           :result reply
           :execution successful-execution}})]
    (is (= :grounded-change (:outcome result)))
    (is (empty? (get-in result [:data :build-retries])))
    ;; Author dispatched once, reviewer once — no cure dispatch.
    (is (= ["zai-5" "codex-7"] dispatches))
    (is (str/starts-with? (get-in item [:feature-card :built])
                          "cohort stopping-rule"))))

(deftest cure-claiming-an-unresolvable-commit-fails-closed
  ;; A cure turn that claims a NEW commit the runner cannot resolve to any
  ;; repository must not be accepted: new-repo/new-files used to fall back to
  ;; the PRIOR file list, so a card-only validation would declare the bounce
  ;; cured and bind the phantom sha downstream. The bounce is rejected
  ;; wholesale and retries exhaust fail-closed on the original failure.
  (let [good-card (str "FULL_LOOP_FEATURE_CARD: "
                       "{:built \"cure\" :want-coverage \"bound\" "
                       ":matches-intent? true :things-to-try [\"a -> b\"]}")
        cure-prompts (atom [])
        opts {:build-cure-retries 1
              :phase-log (str (System/getProperty "java.io.tmpdir")
                              "/cure-unresolved-test-phase.log")
              :dispatch-fn (fn [_ _ _ _ prompt]
                             (swap! cure-prompts conj prompt)
                             {:job-id "cure-1"})
              :poll-fn (fn [_ _] {:job-id "cure-1" :state "done"
                                  :artifact-ref "deadbee1234567890abcdef1234567890abcdef1"
                                  :result-summary
                                  (str good-card
                                       "\nFULL_LOOP_AUTHOR: DONE deadbee1234567890abcdef1234567890abcdef1")})
              :resolve-build-fn (fn [_] nil)}
        thrown (try
                 (#'runner/build-cure-loop
                  opts {} "author-x" (atom 0)
                  "target-x" "orig123" "/repo" ["src/x.clj"]
                  {:job-id "author-1" :state "done"
                   :result-summary "FULL_LOOP_AUTHOR: DONE orig123"
                   :events [{:type "text" :text "narrated event prose"}]}
                  false
                  {:repo "/repo" :pre-dispatch-head "base"})
                 nil
                 (catch clojure.lang.ExceptionInfo e (ex-data e)))]
    (is (some? thrown))
    (is (= :feature-card-missing-or-invalid (:failure-kind thrown)))
    (is (= "orig123" (:commit thrown))
        "the phantom sha must not replace the original commit in the failure")
    (is (= false (get-in thrown [:build-retries 0 :cured?])))
    (is (= :cure-commit-unresolved
           (get-in thrown [:build-retries 0 :cure-rejected])))
    (is (= "deadbee1234567890abcdef1234567890abcdef1" (get-in thrown [:build-retries 0 :claimed-commit])))
    ;; The cure prompt quotes the actual durable prefix, not job-text: event
    ;; narration must not be presented as what Agency preserved.
    (is (str/includes? (first @cure-prompts)
                       (pr-str "FULL_LOOP_AUTHOR: DONE orig123")))
    (is (not (str/includes? (first @cure-prompts) "narrated event prose")))
    (is (str/includes? (first @cure-prompts) "no prose before it"))))

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
                {:author "author" :reviewer "reviewer"
                 :target-repository "/repo" :target-repository-head "base123"}
                "target" {:id "target"} {} [])]
    (is (re-find #"BEGIN your response with one compact" prompt))
    (is (re-find #"at most 200 characters" prompt))
    (is (re-find #"closing brace is inside the 200-character limit" prompt))))

(deftest author-contract-binds-work-to-the-observed-target-repository
  (let [prompt (#'runner/author-prompt
                {:author "author" :reviewer "reviewer"
                 :target-repository "/home/joe/code/futon2"
                 :target-repository-head "base123"}
                "repair-artifact-binding"
                {:selected-entry
                 {:action
                  {:mission-path
                   "/home/joe/code/futon5a/holes/missions/M-learning-loop.md"}}}
                {} [])]
    (is (re-find #"TARGET REPOSITORY: \"/home/joe/code/futon2\"" prompt))
    (is (re-find #"TARGET REPOSITORY BASE HEAD: \"base123\"" prompt))
    (is (re-find #"nested in the mission record are context" prompt))
    (is (re-find #"commit elsewhere is an artifact-binding mismatch" prompt))
    (is (re-find #"make no commit and REFUSE" prompt))))

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
            :grounding :grounding :delivery-qa :delivery-qa :opportunity]
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

(deftest stop-line-repair-contract-never-carries-a-nil-discharge
  ;; attempt-054's repair cascade ran with :discharge nil because schema-1
  ;; review-failure findings minted no discharge contract. Construction now
  ;; floors a contract-less obligation at the class-typed discharge, and a
  ;; minted contract stays authoritative.
  (testing "legacy review-failure finding gets the class-typed discharge"
    (let [construction
          (runner/construct-selected-action
           {:action {:type :repair-machine-failure
                     :target "repair-attempt-054"
                     :repair-obligation
                     {:repair/id "repair-attempt-054"
                      :repair/class :independent-review-failure
                      :attempt-id "attempt-054"
                      :failed-commit "bad123"}}})]
      (is (= [:distinct-repair-commit :independent-review
              :grounded-repair :distinct-production-shaped-successor]
             (get-in construction [:repair-contract :discharge :requires])))
      (is (= :code-commit
             (get-in construction
                     [:repair-contract :discharge :artifact-shape])))))
  (testing "a minted discharge contract on the obligation is authoritative"
    (let [construction
          (runner/construct-selected-action
           {:action {:type :repair-machine-failure
                     :target "repair-x"
                     :repair-obligation
                     {:repair/id "repair-x"
                      :repair/class :machine-failure
                      :attempt-id "attempt-x"
                      :discharge-contract
                      {:requires [:cleared-precondition]
                       :artifact-shape :code-commit}}}})]
      (is (= [:cleared-precondition]
             (get-in construction
                     [:repair-contract :discharge :requires]))))))

(deftest reviewer-receives-a-bounded-repair-contract-without-the-backtrace
  (let [contract
        {:construction-kind :machine-stop-line-repair
         :selected-action
         {:type :repair-machine-failure
          :target "repair-attempt-047-review-execution-evidence-missing"
          :repair-obligation
          {:repair/id "repair-attempt-047-review-execution-evidence-missing"
           :repair/class :machine-failure
           :repair/status :open
           :attempt-id "attempt-047"
           :failure-stage :reviewer-wait
           :failure-error "Independent review lacks execution evidence"
           :discharge-contract
           {:requires [:distinct-repair-commit :independent-review]}
           :backtrace {:unbounded-marker (apply str (repeat 100000 "x"))}}}
         :repair-contract
         {:repair-id "repair-attempt-047-review-execution-evidence-missing"
          :failure-kind :review-execution-evidence-missing}}
        prompt (#'runner/reviewer-prompt
                {:author "codex-1" :reviewer "codex-2"}
                "repair-attempt-047-review-execution-evidence-missing"
                contract "/repo" "abc123" {:job-id "author-job"} [])]
    (is (< (count prompt) 10000))
    (is (re-find #":machine-stop-line-repair" prompt))
    (is (re-find #":distinct-repair-commit" prompt))
    (is (re-find #"repair-attempt-047-review-execution-evidence-missing" prompt))
    (is (not (re-find #":backtrace|unbounded-marker" prompt)))))

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
          :revision-rounds 0
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

(defn- run-revision-attempt
  [review-verdicts & [{:keys [revision-rounds]}]]
  (let [phases (atom [])
        dispatches (atom [])
        queued (atom [])
        deliveries (atom [])
        grounded (atom [])
        author-count (atom 0)
        reviewer-count (atom 0)
        construction {:construction-kind :revision-test
                      :capability-contract {:must :preserve-contract}
                      :shown [:P1]
                      :semilattice []}
        opts
        (cond->
         (merge
          (isolated-runner-opts)
          {:phase-log-fn #(swap! phases conj %)
           :repair-open-fn (constantly [])
           :repair-record-fn
           (fn [finding] (assoc finding :repair/id "review-finding"))
           :repair-system-record-fn
           (fn [finding] (assoc finding :repair/id "system-finding"))
           :trace-fn (constantly "/tmp/revision-round-trace.edn")
           :construct-fn (constantly construction)
           :target-repo-fn (fn [& _] "/repo")
           :repo-head-observation-fn
           (fn [repo] {:repo repo :head "observed-head"
                       :observed-at-ms 1000})
           :author-artifact-observer-fn
           (fn [repo before author-job]
             {:fresh-author? true
              :repo repo
              :pre-dispatch-head (:head before)
              :observed-head (:artifact-ref author-job)
              :author-window-start-ms 1000
              :author-window-end-ms 2000
              :corroborates? true
              :commit (:artifact-ref author-job)})
           :dispatch-fn
           (fn [_ agent _ _ prompt]
             (let [job-id
                   (if (= agent "zai-5")
                     (str "author-" (swap! author-count inc))
                     (str "review-" (swap! reviewer-count inc)))]
               (swap! dispatches conj {:agent agent :job-id job-id
                                       :prompt prompt})
               {:job-id job-id}))
           :poll-fn
           (fn [_ job-id]
             (case job-id
               "author-1"
               {:job-id job-id :state "done" :artifact-ref "abc123"
                :feature-card feature-card-claim
                :execution successful-execution
                :result-summary "FULL_LOOP_AUTHOR: DONE abc123"}

               "author-2"
               {:job-id job-id :state "done" :artifact-ref "def456"
                :execution successful-execution
                :result-summary "FULL_LOOP_AUTHOR: DONE def456"}

               "review-1"
               {:job-id job-id :state "done"
                :execution successful-execution
                :result-summary (first review-verdicts)}

               "review-2"
               {:job-id job-id :state "done"
                :execution successful-execution
                :result-summary (second review-verdicts)}))
           :resolve-build-fn
           (fn [_] {:repo "/repo" :files ["src/revision.clj"]})
           :ground-fn
           (fn [& args]
             (swap! grounded conj args)
             {:before {:implementation-entity nil}
              :after {:implementation-entity {:id "revision-impl"}}
              :resolved? true :dial-moved? true
              :implementation-id "revision-impl"})
           :queue-fn #(swap! queued conj %)
           :delivery-qa-fn
           (fn [_ item]
             (swap! deliveries conj item)
             {:morning-brief/addendum-id "revision-qa"})})
          (some? revision-rounds)
          (assoc :revision-rounds revision-rounds))
        result (runner/run-opportunity! opts)]
    {:result result
     :phases @phases
     :dispatches @dispatches
     :queued @queued
     :deliveries @deliveries
     :grounded @grounded}))

(deftest rejected-review-is-revised-and-approved-once
  (let [first-review
        (str "FULL_LOOP_REVIEW: REQUEST_CHANGES preserve the typed gate\n"
             "Finding: the negative path is untested.")
        {:keys [result phases dispatches deliveries grounded]}
        (run-revision-attempt
         [first-review "FULL_LOOP_REVIEW: APPROVE\nAmendment verified."])
        build (get-in result [:checkpoints :build :judgment])
        revision-phases
        (->> phases
             (filter #(and (= :start (:transition %))
                           (#{:revision-dispatch :revision-wait :revision-build
                              :re-review-dispatch :re-review-wait}
                            (:phase %))))
             (mapv :phase))]
    (is (= :grounded-change (:outcome result)))
    (is (= 2 (get-in build [:revision :round])))
    (is (= ["abc123" "def456"] (get-in build [:revision :commits])))
    (is (= :approve (get-in build [:revision :review :verdict])))
    (is (= [:request-changes :approve]
           (mapv :verdict (:reviews build))))
    (is (= [:revision-dispatch :revision-wait :revision-build
            :re-review-dispatch :re-review-wait]
           revision-phases))
    (is (= ["zai-5" "codex-7" "zai-5" "codex-7"]
           (mapv :agent dispatches))
        "revision and re-review use the same author and reviewer seams")
    (is (= 1 (count grounded))
        "approved amendment proceeds through adjudication")
    (is (= (:reviews build) (:reviews (first deliveries)))
        "both verdicts reach the delivery-QA body")
    (let [revision-prompt (:prompt (nth dispatches 2))]
      (is (str/includes? revision-prompt "SELECTED TARGET: \"M-selected\""))
      (is (str/includes? revision-prompt
                         "CONSTRUCTION CONTRACT: {:construction-kind :revision-test"))
      (is (str/includes? revision-prompt "YOUR PRIOR COMMIT SHAS: [\"abc123\"]"))
      (is (str/includes? revision-prompt first-review)
          "review findings are included verbatim")
      (is (str/includes? revision-prompt "do not force-push")))))

(deftest second-rejection-closes-with-both-review-records
  (let [{:keys [result deliveries]}
        (run-revision-attempt
         ["FULL_LOOP_REVIEW: REQUEST_CHANGES first defect"
          "FULL_LOOP_REVIEW: REJECT amendment still violates the contract"])
        build (get-in result [:checkpoints :build :judgment])]
    (is (= :build-failed (:outcome result)))
    (is (= [:request-changes :reject]
           (mapv :verdict (:reviews build))))
    (is (= [:request-changes :reject]
           (mapv :verdict (get-in result [:data :reviews]))))
    (is (= (:reviews build) (:reviews (first deliveries))))))

(deftest zero-revision-rounds-preserve-single-shot-records
  (let [{:keys [result phases dispatches]}
        (run-revision-attempt
         ["FULL_LOOP_REVIEW: REQUEST_CHANGES no revision permitted"]
         {:revision-rounds 0})
        build (get-in result [:checkpoints :build :judgment])]
    (is (= :build-failed (:outcome result)))
    (is (= ["abc123"] (:commits build)))
    (is (not (contains? build :revision)))
    (is (not (contains? build :reviews)))
    (is (= ["zai-5" "codex-7"] (mapv :agent dispatches)))
    (is (empty? (filter #(str/includes? (name (:phase %)) "revision")
                        phases)))))

(deftest first-review-approval-skips-revision-machinery
  (let [{:keys [result phases dispatches]}
        (run-revision-attempt ["FULL_LOOP_REVIEW: APPROVE"])
        build (get-in result [:checkpoints :build :judgment])]
    (is (= :grounded-change (:outcome result)))
    (is (= ["abc123"] (:commits build)))
    (is (not (contains? build :revision)))
    (is (= ["zai-5" "codex-7"] (mapv :agent dispatches)))
    (is (empty? (filter #(#{:revision-dispatch :revision-wait :revision-build
                            :re-review-dispatch :re-review-wait}
                          (:phase %))
                        phases)))))

(deftest negative-review-without-findings-does-not-spend-revision-round
  (let [{:keys [result phases dispatches]}
        (run-revision-attempt ["FULL_LOOP_REVIEW: REQUEST_CHANGES"])]
    (is (= :build-failed (:outcome result)))
    (is (= ["zai-5" "codex-7"] (mapv :agent dispatches)))
    (is (empty? (filter #(#{:revision-dispatch :revision-wait :revision-build
                            :re-review-dispatch :re-review-wait}
                          (:phase %))
                        phases)))))

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
               {:job-id job-id :state "done" :artifact-ref "facade01234567890abcdef1234567890abcdef1"
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
                   (fn [_ commit] (when (= commit "facade01234567890abcdef1234567890abcdef1") "old123"))
                   :ancestor-fn (fn [_ ancestor descendant]
                                  (and (= ancestor "base000")
                                       (= descendant "observed456")))
                   :commit-time-ms-fn (fn [& _] 1500)}
        before {:repo "/repo" :head "base000" :observed-at-ms 1000}
        binding (runner/fresh-artifact-binding
                 base-opts "/repo" before
                 {:artifact-ref "facade01234567890abcdef1234567890abcdef1"})]
    (is (= "observed456" (:commit binding)))
    (is (:descendant? binding))
    (is (:in-author-window? binding))
    (is (:disagreement? binding))
    (is (nil? (:commit
               (runner/fresh-artifact-binding
                (assoc base-opts :commit-time-ms-fn (fn [& _] 500000))
                "/repo" before {:artifact-ref "facade01234567890abcdef1234567890abcdef1"})))
        "a changed descendant outside the tolerated author window is rejected")))

(deftest narrated-artifact-without-new-repo-head-stops-before-review
  (let [dispatches (atom [])
        author-prompts (atom [])
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
           (fn [_ agent _caller _target prompt]
             (swap! dispatches conj agent)
             (swap! author-prompts conj prompt)
             {:job-id "author-job"})
           :poll-fn
           (fn [& _] {:job-id "author-job" :state "done"
                      :artifact-ref "facade01234567890abcdef1234567890abcdef1"})
           :repair-system-record-fn
           (fn [finding]
             (swap! findings conj finding)
             (assoc finding :repair/id "repair-artifact-binding"))}))]
    (is (= :build-failed (:outcome result)))
    (is (= :artifact-binding-mismatch
           (get-in result [:data :failure-kind])))
    (is (= ["zai-5"] @dispatches)
        "no reviewer is dispatched for an unobserved narrated artifact")
    (is (re-find #"TARGET REPOSITORY: \"/repo\""
                 (first @author-prompts))
        "dispatch tells the author which repository the artifact gate observes")
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

(deftest non-infrastructure-author-failure-is-not-retried
  (let [dispatches (atom [])
        findings (atom [])
        result
        (runner/run-opportunity!
         (no-commit-author-opts
          dispatches findings
          {:job-id "author-job" :state "failed"
           :terminal-code "agent-error"
           :terminal-message "author process failed"}))]
    (is (= :build-failed (:outcome result)))
    (is (= ["zai-5"] @dispatches))
    (is (= :build-failed (get-in result [:data :failure-kind])))))

(deftest cancelled-author-closes-distinctly-from-build-failure
  (let [dispatches (atom [])
        findings (atom [])
        result
        (runner/run-opportunity!
         (no-commit-author-opts
          dispatches findings
          {:job-id "author-job" :state "cancelled"
           :terminal-code "operator-cancelled"
           :terminal-message "cancelled by joe"}))]
    (is (= :cancelled (:outcome result)))
    (is (= :operator-cancelled (get-in result [:data :failure-kind])))
    (is (= :author-wait (get-in result [:data :failure-stage])))
    (is (= :environmental-hold (:repair-class (first @findings))))
    (is (= ["zai-5"] @dispatches))))

(deftest machine-stop-line-preempts-ordinary-selection-and-awaits-successor-validation
  (let [dispatches (atom [])
        implementations (atom [])
        transform-called? (atom false)
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
          :judgement-transform-fn
          (fn [incoming]
            (reset! transform-called? true)
            (assoc incoming :campaign/selected? true))
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
    (is (false? @transform-called?)
        "stop-line precedence must bypass optional ordinary-selection transforms")
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
    (is (= :unreachable (get-in result [:data :failure-detail])))
    (is (= :environmental-hold (:repair-class (first @findings))))
    (is (= :none (get-in (first @queued) [:achievement :tier])))
    (is (= #{:selection :construction :dispatch :build :adjudication}
           (set (keys (:checkpoints result)))))))

(defn- readiness-run-opts [phases roster-fn]
  {:cohort? false
   :phase-log-fn #(swap! phases conj %)
   :roster-fn roster-fn
   :code-state-fn (fn [] {:repo "/futon2" :git-sha "head"
                          :git-dirty? false :repo-heads {}})
   :refresh-fn (fn [])
   :repair-open-fn (constantly [])
   :repair-system-record-fn
   (fn [finding] (assoc finding :repair/id "repair-readiness"))
   :queue-fn identity
   :judge-fn
   (fn [_]
     {:judgement (assoc judgement
                        :ranked-actions []
                        :admissible-actions []
                        :decision {:action :abstain})})})

(deftest exhausted-selection-retries-close-with-typed-kind
  (let [phases (atom [])
        calls (atom 0)
        selection-opts
        {:strategic-selection-invoke-fn
         (fn [_]
           (swap! calls inc)
           (throw (ex-info "selection unavailable" {})))
         :strategic-selection-sleep-fn (fn [_])}
        opts
        (merge
         (readiness-run-opts
          phases
          (fn [_] {:zai-5 {:status "idle" :invoke-ready? true}
                   :codex-7 {:status "idle" :invoke-ready? true}}))
         {:substrate-preflight-fn (fn [_] {:route :test})
          :judge-fn
          (fn [_]
            (runner/strategic-selection!
             selection-opts
             {:scheduler-habit-ranking ["M-a"]}))})
        result (runner/run-opportunity! opts)]
    (is (= :incomplete (:outcome result)))
    (is (= :strategic-selection-unavailable
           (get-in result [:data :failure-kind])))
    (is (= :transient-exhausted
           (get-in result [:data :failure-detail])))
    (is (= :selection (get-in result [:data :failure-stage])))
    (is (= 3 @calls))))

(deftest selection-checkpoint-records-only-late-success-marker
  (letfn [(run [failures-before-success]
            (let [calls (atom 0)
                  opts
                  (-> (isolated-runner-opts)
                      (dissoc :judge-fn)
                      (assoc
                       :repair-open-fn (constantly [])
                       :repair-system-record-fn
                       (fn [finding]
                         (assoc finding :repair/id "repair-selection-marker"))
                       :strategic-selection-invoke-fn
                       (fn [_]
                         (if (<= (swap! calls inc) failures-before-success)
                           (throw (ex-info "transient selection timeout" {}))
                           {:ok true
                            :selection
                            {:status :verified-live-selection
                             :selected-mission-ids ["M-selected"]}}))
                       :strategic-selection-sleep-fn (fn [_])
                       :construct-fn
                       (fn [& _]
                         (throw (ex-info "stop after selection"
                                         {:outcome :incomplete})))))
                  result
                  (with-redefs
                    [wm/generate-war-machine
                     (fn [_ {:keys [strategic-selection-fn]}]
                       (strategic-selection-fn
                        {:scheduler-habit-ranking ["M-selected"]})
                       {:judgement judgement})]
                    (runner/run-opportunity! opts))]
              (get-in result [:checkpoints :selection :judgment])))]
    (is (true? (:readiness/selection-transient (run 1)))
        "retry success is stamped in the durable selection judgment")
    (is (not (contains? (run 0) :readiness/selection-transient))
        "first-try success preserves the original judgment shape")))

(deftest restored-agent-is-woken-once-before-readiness-proceeds
  (let [phases (atom [])
        roster-calls (atom 0)
        wake-calls (atom [])
        restored {:zai-5 {:status "restored" :invoke-ready? true}
                  :codex-7 {:status "idle" :invoke-ready? true}}
        idle (assoc-in restored [:zai-5 :status] "idle")
        opts (merge
              (readiness-run-opts
               phases
               (fn [_]
                 (if (= 1 (swap! roster-calls inc)) restored idle)))
              {:wake-agent-fn
               (fn [_ agent]
                 (swap! wake-calls conj agent)
                 {:ok true})
               :substrate-preflight-fn (fn [_] {:route :test})})
        result (runner/run-opportunity! opts)
        readiness-end (first (filter #(and (= :agent-readiness (:phase %))
                                            (= :end (:transition %)))
                                     @phases))]
    (is (= :abstained (:outcome result))
        "woken author passes readiness and reaches the injected abstention")
    (is (= ["zai-5"] @wake-calls) "exactly one whistle is attempted")
    (is (= 2 @roster-calls) "the roster is observed once, then rechecked once")
    (is (= :ok (:outcome readiness-end)))
    (is (true? (:readiness/wake-attempted readiness-end)))
    (is (= :woken (:readiness/wake-result readiness-end)))))

(deftest restored-agent-no-reply-retains-unavailable-vocabulary-and-detail
  (let [phases (atom [])
        roster-calls (atom 0)
        wake-calls (atom 0)
        restored {:zai-5 {:status "restored" :invoke-ready? true}
                  :codex-7 {:status "idle" :invoke-ready? true}}
        opts (merge
              (readiness-run-opts
               phases
               (fn [_] (swap! roster-calls inc) restored))
              {:wake-agent-fn (fn [& _] (swap! wake-calls inc) nil)})
        result (runner/run-opportunity! opts)
        readiness-end (first (filter #(and (= :agent-readiness (:phase %))
                                            (= :end (:transition %)))
                                     @phases))]
    (is (= :agent-unavailable (:outcome result)))
    (is (= :agent-unavailable (get-in result [:data :failure-kind])))
    (is (= :restored-unwoken (get-in result [:data :failure-detail])))
    (is (= 1 @wake-calls))
    (is (= 2 @roster-calls))
    (is (= :no-reply (:readiness/wake-result readiness-end)))))

(deftest substrate-readiness-has-two-fixed-retries
  (let [calls (atom 0)
        sleeps (atom [])
        phases (atom [])
        opts {:phase-log-fn #(swap! phases conj %)
              :substrate-preflight-fn
              (fn [_]
                (if (< (swap! calls inc) 3)
                  (throw (ex-info "transient" {}))
                  {:route :test}))
              :readiness-sleep-fn #(swap! sleeps conj %)}
        recovered (runner/run-phase!
                   opts {} :substrate-preflight
                   #(runner/substrate-readiness! opts)
                   #(select-keys % [:readiness/substrate-transient]))
        phase-end (last @phases)]
    (is (= {:route :test :readiness/substrate-transient true} recovered))
    (is (= 3 @calls))
    (is (= [5000 5000] @sleeps))
    (is (= :ok (:outcome phase-end)))
    (is (true? (:readiness/substrate-transient phase-end))))
  (let [calls (atom 0)
        sleeps (atom [])
        failure (try
                  (runner/substrate-readiness!
                   {:substrate-preflight-fn
                    (fn [_]
                      (swap! calls inc)
                      (throw (ex-info "still down" {})))
                    :readiness-sleep-fn #(swap! sleeps conj %)})
                  nil
                  (catch clojure.lang.ExceptionInfo e e))]
    (is (= :substrate-unavailable (:outcome (ex-data failure))))
    (is (= :transient-exhausted (:failure-detail (ex-data failure))))
    (is (= 3 @calls))
    (is (= [5000 5000] @sleeps))))

(deftest substrate-readiness-escalates-retry-timeouts
  (let [timeouts (atom [])
        failure (try
                  (runner/substrate-readiness!
                   {:substrate-preflight-fn
                    (fn [opts]
                      (swap! timeouts conj (:substrate-preflight-timeout-ms opts))
                      (throw (ex-info "busy" {})))
                    :readiness-sleep-fn (fn [_])})
                  nil
                  (catch clojure.lang.ExceptionInfo e e))]
    (is (= [15000 30000 60000] @timeouts))
    (is (= :substrate-unavailable (:outcome (ex-data failure)))))
  (let [timeouts (atom [])]
    (try
      (runner/substrate-readiness!
       {:substrate-preflight-timeout-ms 7000
        :substrate-preflight-fn
        (fn [opts]
          (swap! timeouts conj (:substrate-preflight-timeout-ms opts))
          (throw (ex-info "busy" {})))
        :readiness-sleep-fn (fn [_])})
      (catch clojure.lang.ExceptionInfo _))
    (is (= [7000 7000 7000] @timeouts)
        "an explicit timeout pins all attempts")))

(deftest substrate-exhaustion-classifies-liveness
  (letfn [(exhaust [liveness-fn]
            (try
              (runner/substrate-readiness!
               (cond-> {:substrate-preflight-fn
                        (fn [_] (throw (ex-info "busy" {})))
                        :readiness-sleep-fn (fn [_])}
                 liveness-fn (assoc :substrate-liveness-fn liveness-fn)))
              nil
              (catch clojure.lang.ExceptionInfo e (ex-data e))))]
    (let [d (exhaust (fn [_] {:alive? true :latency-ms 3}))]
      (is (= :alive-but-slow (:substrate-state d)))
      (is (= {:alive? true :latency-ms 3} (:substrate-liveness d)))
      (is (= :substrate-unavailable (:outcome d))
          "classification refines the record; it does not rescue the attempt"))
    (let [d (exhaust (fn [_] {:alive? false :error "connect refused"}))]
      (is (= :unreachable (:substrate-state d))))
    (let [d (exhaust nil)]
      (is (= :liveness-unknown (:substrate-state d))
          "stubbed preflight without a liveness stub must not probe the real store"))
    (let [d (exhaust (fn [_] (throw (ex-info "classifier broke" {}))))]
      (is (= :substrate-unavailable (:outcome d))
          "a throwing classifier must not mask the authoritative failure")
      (is (= :liveness-unknown (:substrate-state d)))
      (is (re-find #"classifier broke"
                   (str (:classifier-error (:substrate-liveness d))))
          "the classifier failure is retained as diagnostic data"))))

(deftest exhausted-substrate-retries-close-with-existing-kind-and-new-detail
  (let [phases (atom [])
        calls (atom 0)
        findings (atom [])
        opts (merge
              (readiness-run-opts
               phases
               (fn [_] {:zai-5 {:status "idle" :invoke-ready? true}
                        :codex-7 {:status "idle" :invoke-ready? true}}))
              {:substrate-preflight-fn
               (fn [_]
                 (swap! calls inc)
                 (throw (ex-info "substrate down" {})))
               :readiness-sleep-fn (fn [_])
               :repair-system-record-fn
               (fn [finding]
                 (swap! findings conj finding)
                 (assoc finding :repair/id "repair-substrate"))})
        result (runner/run-opportunity! opts)]
    (is (= :substrate-unavailable (:outcome result)))
    (is (= :substrate-unavailable (get-in result [:data :failure-kind])))
    (is (= :transient-exhausted (get-in result [:data :failure-detail])))
    (is (= :transient-exhausted
           (get-in (first @findings) [:failure-data :failure-detail])))
    (is (= 3 @calls))))

(deftest invoking-agent-remains-busy-without-a-wake-attempt
  (let [phases (atom [])
        wake-calls (atom 0)
        result
        (runner/run-opportunity!
         (merge
          (readiness-run-opts
           phases
           (fn [_] {:zai-5 {:status "invoking" :invoke-ready? true}
                    :codex-7 {:status "idle" :invoke-ready? true}}))
          {:wake-agent-fn (fn [& _] (swap! wake-calls inc))}))]
    (is (= :agent-unavailable (:outcome result)))
    (is (= :busy (get-in result [:data :failure-detail])))
    (is (zero? @wake-calls))))

(deftest healthy-readiness-paths-retain-original-values
  (let [roster {:zai-5 {:status "idle" :invoke-ready? true}}
        roster-calls (atom 0)
        wake-calls (atom 0)
        agent-result (runner/agent-readiness!
                      {:agency-base "http://test"
                       :roster-fn (fn [_] (swap! roster-calls inc) roster)
                       :wake-agent-fn (fn [& _] (swap! wake-calls inc))}
                      "zai-5")
        healthy {:route :test}
        substrate-calls (atom 0)
        substrate-result
        (runner/substrate-readiness!
         {:substrate-preflight-fn
          (fn [_] (swap! substrate-calls inc) healthy)
          :readiness-sleep-fn
          (fn [_] (throw (ex-info "healthy path must not sleep" {})))})]
    (is (= roster (:roster agent-result)))
    (is (= :not-needed (:readiness/wake-result agent-result)))
    (is (= 1 @roster-calls))
    (is (zero? @wake-calls))
    (is (= healthy substrate-result))
    (is (= 1 @substrate-calls))))

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

(deftest single-candidate-still-requires-finite-scores
  ;; controller-score must be finite
  (is (false? (:passes? (runner/selection-discrimination
                          [{:action selected-action
                            :controller-score nil :G-efe 1.0}]))))
  (is (false? (:passes? (runner/selection-discrimination
                          [{:action selected-action
                            :controller-score ##NaN :G-efe 1.0}]))))
  ;; G-efe must also be finite (canonical EFE audit)
  (is (false? (:passes? (runner/selection-discrimination
                          [{:action selected-action
                            :controller-score -1.0 :G-efe nil}]))))
  (is (false? (:passes? (runner/selection-discrimination
                          [{:action selected-action
                            :controller-score -1.0 :G-efe ##NaN}]))))
  ;; Both finite → pass
  (is (true? (:passes? (runner/selection-discrimination
                         [{:action selected-action
                           :controller-score -1.0 :G-efe 0.5}])))))

(deftest discrimination-uses-controller-score-not-g-efe
  ;; Flat G-efe but distinct controller-score: the policy CAN discriminate
  ;; via intrinsic-value. Should pass.
  (is (true? (:passes?
              (runner/selection-discrimination
               [{:action {:type :learn-action-class :target-class :survey}
                 :G-efe 7.907 :controller-score 10.048}
                {:action {:type :learn-action-class :target-class :close}
                 :G-efe 7.907 :controller-score 10.049}
                {:action {:type :learn-action-class :target-class :survey-mission}
                 :G-efe 7.907 :controller-score 10.050}
                {:action {:type :learn-action-class :target-class :apply-cascade}
                 :G-efe 7.907 :controller-score 10.051}
                {:action {:type :learn-action-class :target-class :close-hole}
                 :G-efe 7.907 :controller-score 10.055}]))))
  ;; Flat controller-score: genuine near-tie. Should fail.
  (is (false? (:passes?
               (runner/selection-discrimination
                [{:action {:type :advance-mission :target "M-flat-a"}
                  :G-efe 4.0 :controller-score 4.0}
                 {:action {:type :advance-mission :target "M-flat-b"}
                  :G-efe 4.0 :controller-score 4.0}]))))
  ;; Finite controller-score but invalid G-efe → fail closed
  (is (false? (:passes?
               (runner/selection-discrimination
                [{:action {:type :learn-action-class}
                  :G-efe nil :controller-score 10.0}
                 {:action {:type :learn-action-class}
                  :G-efe nil :controller-score 11.0}])))))

(deftest discharge-contracts-declare-backward-compatible-artifact-shape
  (doseq [repair-class [:machine-failure :environmental-hold
                        :incomplete-recoverable :unknown]]
    (is (= :code-commit
           (:artifact-shape (#'runner/discharge-contract repair-class))))))

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
                          :artifact-ref "1a7e1234567890abcdef1234567890abcdef1234"
                          :feature-card feature-card-claim
                          :execution successful-execution
                          :result-summary "FULL_LOOP_AUTHOR: DONE 1a7e1234567890abcdef1234567890abcdef1234"})
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
                    :artifact-ref "1a7e1234567890abcdef1234567890abcdef1234"
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
                                  :commit "1a7e1234567890abcdef1234567890abcdef1234"
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
                {:revision-rounds 0
                 :repair-open-fn (constantly [stop-line])
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

(deftest wm-phase-status-is-visible-and-opportunity-end-clears-it
  (is (= {:source "wm-full-loop"
          :status "invoking"
          :activity "selection attempt-052 (duree-click-on-demand)"}
         (#'runner/wm-status-payload
          {:attempt-id "attempt-052" :trigger :duree-click-on-demand}
          {:phase :selection :transition :start})))
  (is (= {:source "wm-full-loop" :status "idle"}
         (#'runner/wm-status-payload
          {:attempt-id "attempt-052" :trigger :duree-click-on-demand}
          {:phase :opportunity :transition :end}))))

(deftest poll-job-heartbeats-the-current-wait-phase
  (let [reports (atom [])
        phase-state (atom {:phase :author-wait
                           :context {:attempt-id "attempt-052"}})]
    (with-redefs-fn
      {#'runner/read-job!
       (fn [_ _] {:job-id "job-123" :state "done"})
       #'runner/post-wm-status!
       (fn [_ payload] (swap! reports conj payload))}
      (fn []
        (binding [runner/*wm-status-reporting?* true]
          (runner/poll-job! {:agent-budget-ms 1000
                             :poll-ms 1
                             :wm-phase-state phase-state}
                            "job-123"))))
    (is (= "invoking" (:status (first @reports))))
    (is (= "author-wait attempt-052 job job-123 0s"
           (:activity (first @reports))))))

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

(deftest card-cure-uses-terminal-commit-when-agency-ref-is-a-path
  ;; Live shape from canary-da9681ce: the substantive author commit was already
  ;; observed, the cure reply began with a valid feature card, but Agency
  ;; extracted "/eoi_network_test.clj" as its artifact-ref.  A metadata-only
  ;; card cure must use the valid terminal claim rather than treating the path
  ;; as a replacement commit.  The feature-card gate itself remains unchanged.
  (let [{:keys [result]}
        (run-feature-card-attempt
         {:author-card nil
          :cure-card feature-card-claim
          :cure-commit "/eoi_network_test.clj"
          :cure-observed-commit nil
          :cure-summary "FULL_LOOP_AUTHOR: DONE feature123"
          :artifacts? true})]
    (is (= :grounded-change (:outcome result)))
    (is (= "feature123" (get-in result [:data :commit])))
    (is (= true (get-in result [:data :build-retries 0 :cured?])))
    (is (nil? (get-in result [:data :build-retries 0 :cure-rejected])))))

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

(defn- tmp-cohort-root []
  (.getPath (.toFile (Files/createTempDirectory
                      "wm-runner-cohort-test"
                      (make-array FileAttribute 0)))))

(defn- tiny-target-prereg [path]
  ;; Write a minimal preregistration with stopping-rule target 1.
  (let [prereg {:cohort/id :test-cohort-exhaustion
                :protocol/version 4
                :status :preregistered
                :registered-on "2026-07-25"
                :purpose "Test fixture: target 1 cohort for stopping-rule verification."
                :activation :separate-append-only-marker
                :population "Test"
                :stopping-rule
                {:unit :trigger-opportunity
                 :target 1
                 :rule "Count 1 distinct eligible trigger opportunity."
                 :counts-as-attempt
                 [:selected-and-dispatched :abstained :no-selection
                  :agent-unavailable :substrate-unavailable :guardrail-refusal
                  :dispatch-failed :build-failed :grounded-no-change
                  :grounded-change]
                 :excluded [:manual-test :replay :duplicate-opportunity :pre-activation]
                 :replacement "None."}
                :allowed-triggers #{:duree-click-on-demand :duree-click-continuous
                                    :test-trigger}
                :checkpoint-order [:time-step :selection :construction :dispatch
                                   :build :adjudication :closed]
                :required-before-close #{:selection :construction :dispatch :build
                                         :adjudication}
                :checkpoint-contract {}
                :grounded-success {:outcome :grounded-change
                                   :requires []
                                   :artifact-only-counts? false}
                :machine-evolution {:allowed? true :rule "" :per-attempt-provenance []
                                    :epoch-boundary "" :analysis ""}
                :analysis {:primary [] :secondary [] :reporting ""}}]
    (io/make-parents path)
    (spit path (pr-str prereg))
    path))

(defn- exhaust-cohort! [prereg-path data-root]
  ;; Activate the cohort and open 1 attempt (exhausting target 1).
  ;; Returns the attempt-id of the sole attempt.
  (cohort/activate! prereg-path data-root)
  (let [term {:judgment {:opportunity-id "test/exhausting-attempt"
                         :trigger :test-trigger
                         :machine-state {:tick 1}
                         :agent-roster []
                         :code-state {:git-sha "abc" :git-dirty? false
                                      :resolved-mode-flags {}
                                      :configuration-digest "test"}
                         :semantic-epoch :test}
              :ground {:kind :test-witness}}]
    (:attempt/id (cohort/start-attempt! prereg-path data-root term))))

(deftest cohort-stopping-rule-returns-cohort-complete-not-repair
  "When the cohort stopping rule is reached, run-opportunity! must return
  :cohort-complete — NOT create a repair obligation. This was the root cause
  of repair-initialization-6d5da36a: the stopping rule exception was caught
  by the initialization-failure handler and turned into a spurious repair.

  This test constructs a genuine exhausted cohort (target 1, 1 attempt already
  opened) so that the NEXT run-opportunity! call hits start-attempt!'s
  stopping rule. The assertions are unconditional — not gated on the result."
  (let [data-root (tmp-cohort-root)
        prereg-path (str data-root "/cohort.edn")
        _ (tiny-target-prereg prereg-path)
        _ (exhaust-cohort! prereg-path data-root)
        repair-calls (atom [])
        queue-calls (atom [])]
    (with-redefs [cohort/default-preregistration prereg-path
                  cohort/default-data-root data-root]
      (let [result (runner/run-opportunity!
                    {:trigger :test-trigger
                     :cohort? true
                     :opportunity-id "test/post-exhaustion"
                     :semantic-epoch :test
                     :author "zai-1"
                     :reviewer "codex-1"
                     :repair-system-record-fn
                     (fn [m] (swap! repair-calls conj m)
                       {:repair/id "should-not-fire"})
                     :queue-fn (fn [m] (swap! queue-calls conj m)
                                  {:morning-brief/addendum-id "should-not-queue"})})]
        (is (= :cohort-complete (:outcome result))
            "exhausted cohort must return :cohort-complete")
        (is (empty? @repair-calls)
            "stopping-rule-reached must not create a repair obligation")
        (is (empty? @queue-calls)
            "stopping-rule-reached must not queue a morning-brief item")))))

(deftest stopping-rule-recognition-is-typed-not-textual
  "Independent review of 6657f4c (codex-1): recognition must be typed
  cause-chain handling. Positive: the marker survives a wrapper that buries
  the ex-data. Negative: an untyped failure whose message merely mentions the
  phrase must NOT be classified as normal completion."
  (let [phases (atom [])
        base (assoc (readiness-run-opts
                     phases
                     (fn [_] {:zai-1 {:status "idle" :invoke-ready? true}
                              :codex-1 {:status "idle" :invoke-ready? true}}))
                    :cohort? true
                    :trigger :test-trigger
                    :opportunity-id "test/typed-recognition"
                    :semantic-epoch :test
                    :author "zai-1"
                    :reviewer "codex-1")]
    ;; positive: typed marker buried one level down a cause chain
    (let [repair-calls (atom [])
          opts (assoc base
                      :repair-system-record-fn
                      (fn [m] (swap! repair-calls conj m)
                        {:repair/id "should-not-fire"})
                      :queue-fn identity)]
      (with-redefs [cohort/start-attempt!
                    (fn [& _]
                      (throw (ex-info "cohort cell write failed"
                                      {:io :telemetry-wrapper}
                                      (ex-info "cohort stopping rule reached"
                                               {:cohort/error :stopping-rule-reached
                                                :target 2 :attempted 2}))))]
        (let [result (runner/run-opportunity! opts)]
          (is (= :cohort-complete (:outcome result))
              "typed marker in the cause chain is normal completion")
          (is (= 2 (get-in result [:data :target]))
              "target/attempted come from the typed cause, not the wrapper")
          (is (empty? @repair-calls)))))
    ;; negative: phrase in the message, no typed marker anywhere
    (let [repair-calls (atom [])
          opts (assoc base
                      :repair-system-record-fn
                      (fn [m] (swap! repair-calls conj m)
                        {:repair/id "repair-untyped"})
                      :queue-fn identity)]
      (with-redefs [cohort/start-attempt!
                    (fn [& _]
                      (throw (ex-info
                              "failed to record telemetry for: cohort stopping rule reached"
                              {:io :unrelated})))]
        (let [result (runner/run-opportunity! opts)]
          (is (not= :cohort-complete (:outcome result))
              "message text alone must never classify as completion")
          (is (seq @repair-calls)
              "an untyped initialization failure must open a repair obligation"))))))

(deftest cohort-complete-is-exempt-from-t3-not-in-outcome-kinds
  ":cohort-complete is a scheduler-level signal, NOT an attempt outcome.
  It must NOT be in outcome-kinds (which validates close checkpoints),
  but T3 tripwire must accept it without emitting :unknown-outcome or
  :missing-durable-stop-line (since no attempt was opened)."
  (is (not (contains? cohort/outcome-kinds :cohort-complete))
      ":cohort-complete must NOT pollute outcome-kinds (attempt outcomes)")
  (let [obs {:phase :opportunity :transition :end
             :outcome :cohort-complete :cohort? true
             :attempt-id "cohort-complete-test"}]
    (is (empty? (tripwire/evaluate-wire :T3 obs))
        "T3 must not fire on :cohort-complete (no unknown-outcome, no missing-stop-line)")))

;; --- transport failure typing (repair-attempt-057-untyped-failure) ----------
;; attempt-057 died at :selection with java.net.http.HttpTimeoutException after
;; 100s. Raw transport exceptions carry no ex-data, so failure-kind-from fell to
;; :untyped-failure and repair-class-for sent that down :else to :machine-failure
;; — demanding :distinct-repair-commit and :independent-review for a network
;; condition no code change can fix. These pin the typing both ways.

(def ^:private failure-kind-from* #'runner/failure-kind-from)
(def ^:private repair-class-for* #'runner/repair-class-for)

(deftest transport-exceptions-are-typed-environmental
  (testing "timeouts type as :transport-timeout and hold, not blame the machine"
    (doseq [e [(java.net.http.HttpTimeoutException. "request timed out")
               (java.net.SocketTimeoutException. "read timed out")]]
      (is (= :transport-timeout (failure-kind-from* e))
          (str (.getName (class e)) " must type as :transport-timeout"))
      (is (= :environmental-hold (repair-class-for* (failure-kind-from* e)))
          "a transport timeout is an environmental hold, not a machine failure")))
  (testing "connect-side failures type as :transport-unavailable"
    (doseq [e [(java.net.ConnectException. "connection refused")
               (java.net.UnknownHostException. "no such host")]]
      (is (= :transport-unavailable (failure-kind-from* e)))
      (is (= :environmental-hold (repair-class-for* (failure-kind-from* e))))))
  (testing "the cause chain is walked — these arrive wrapped in ex-info"
    (let [wrapped (ex-info "strategic selection failed" {}
                           (java.net.http.HttpTimeoutException. "request timed out"))]
      (is (= :transport-timeout (failure-kind-from* wrapped))
          "attempt-057's shape: a typed throw wrapping a transport cause"))))

(deftest transport-typing-is-fail-closed
  (testing "an unrecognised exception STAYS untyped and keeps the heavy contract"
    (let [e (RuntimeException. "boom")]
      (is (= :untyped-failure (failure-kind-from* e))
          "narrowing the untyped bucket must never widen the escape hatch")
      (is (= :machine-failure (repair-class-for* (failure-kind-from* e))))))
  (testing "explicit ex-data typing still wins over the transport cause"
    (let [e (ex-info "build failed" {:failure-kind :build-failed}
                     (java.net.http.HttpTimeoutException. "request timed out"))]
      (is (= :build-failed (failure-kind-from* e))
          "a phase that classified its own failure keeps that classification")
      (is (= :machine-failure (repair-class-for* (failure-kind-from* e)))))))

(deftest bare-socket-closure-is-narrowly-typed-environmental
  (testing "the measured peer closure is transport-unavailable"
    (let [e (java.net.SocketException. "Socket closed")]
      (is (= :transport-unavailable (failure-kind-from* e)))
      (is (= :environmental-hold (repair-class-for* (failure-kind-from* e))))))
  (testing "other bare SocketException messages remain fail-closed"
    (let [e (java.net.SocketException. "Socket option failed")]
      (is (= :untyped-failure (failure-kind-from* e)))
      (is (= :machine-failure (repair-class-for* (failure-kind-from* e)))))))

;; --- revision round 2 (reviewer findings) ----------------------------------

(deftest generic-timeout-is-not-a-transport-escape
  ;; java.util.concurrent.TimeoutException is raised by ANY bounded wait —
  ;; future/get, executor shutdown, an internal poll — not only by transport.
  ;; Typing it environmental would let a genuinely hung machine path escape the
  ;; machine-failure contract, which is the widening this repair must not do.
  (let [e (java.util.concurrent.TimeoutException. "timed out")]
    (is (= :untyped-failure (failure-kind-from* e))
        "a generic bounded-wait timeout is NOT a transport condition")
    (is (= :machine-failure (repair-class-for* (failure-kind-from* e)))
        "it keeps the heavy contract")
    (is (= :transport-timeout
           (failure-kind-from* (java.net.SocketTimeoutException. "read timed out")))
        "while a socket-specific timeout still types as transport")))

(deftest explicit-typing-wins-at-any-depth-in-the-cause-chain
  (testing "typed ex-info wrapped by an untyped throw, over a transport cause"
    ;; RuntimeException (untyped) -> ex-info :build-failed -> HttpTimeoutException.
    ;; Walking only the TOP throwable would miss :build-failed and let the
    ;; transport cause outrank a real machine failure.
    (let [e (RuntimeException.
             "wrapper"
             (ex-info "build failed" {:failure-kind :build-failed}
                      (java.net.http.HttpTimeoutException. "request timed out")))]
      (is (= :build-failed (failure-kind-from* e)))
      (is (= :machine-failure (repair-class-for* (failure-kind-from* e)))
          "a typed machine failure must not be downgraded to environmental")))
  (testing ":outcome typing is honoured at depth too"
    (let [e (RuntimeException.
             "wrapper"
             (ex-info "no selection" {:outcome :no-selection}
                      (java.net.ConnectException. "connection refused")))]
      (is (= :no-selection (failure-kind-from* e)))
      (is (= :environmental-hold (repair-class-for* (failure-kind-from* e))))))
  (testing "with no explicit typing anywhere, the transport cause still types"
    (let [e (RuntimeException.
             "wrapper"
             (RuntimeException.
              "inner"
              (java.net.http.HttpTimeoutException. "request timed out")))]
      (is (= :transport-timeout (failure-kind-from* e))
          "attempt-057 remains fixed through a deeper untyped chain"))))

;; --- outer-boundary transport typing (repair-attempt-058-untyped-failure) ---
;; attempt-058 repeated attempt-057's shape: HttpTimeoutException at :selection,
;; recorded :untyped-failure. The classifier repair covered close! inside
;; run-opportunity-core!, but run-opportunity!'s outer boundary HARDCODED
;; :machine-failure / :initialization-failed for every throwable — so the same
;; network fault arriving a moment earlier, before an attempt can own it, was
;; still charged to the machine.

(deftest initialization-transport-failure-is-environmental
  (let [findings (atom [])
        queued (atom [])
        result
        (runner/run-opportunity!
         {:cohort? false
          :phase-log nil
          :phase-log-fn (fn [_]
                          (throw (java.net.http.HttpTimeoutException.
                                  "request timed out")))
          :repair-system-record-fn
          (fn [finding]
            (swap! findings conj finding)
            (assoc finding :repair/id "repair-initialization-transport"))
          :queue-fn #(swap! queued conj %)})]
    (is (= :incomplete (:outcome result)))
    (is (= :transport-timeout (get-in result [:data :failure-kind]))
        "a transport timeout at the outer boundary is typed, not :initialization-failed")
    (is (= :environmental-hold (:repair-class (first @findings)))
        "and it must not open a machine-failure repair")
    (is (= [:cleared-precondition :grounded-production-shaped-successor]
           (:requires (:discharge-contract (first @findings))))
        "the discharge follows the corrected class")
    (is (= :initialization (:failure-stage (first @findings)))
        "the stage is still honestly :initialization")
    (is (= :transport-timeout (get-in (first @queued) [:failure :kind]))
        "the morning brief carries the same typed kind")))

(deftest initialization-socket-closure-is-environmental
  (let [findings (atom [])
        result
        (runner/run-opportunity!
         {:cohort? false
          :phase-log nil
          :phase-log-fn (fn [_]
                          (throw (java.net.SocketException. "Socket closed")))
          :repair-system-record-fn
          (fn [finding]
            (swap! findings conj finding)
            (assoc finding :repair/id "repair-initialization-socket-closed"))
          :queue-fn (fn [_])})]
    (is (= :incomplete (:outcome result)))
    (is (= :transport-unavailable (get-in result [:data :failure-kind])))
    (is (= :environmental-hold (:repair-class (first @findings))))
    (is (= :initialization (:failure-stage (first @findings))))))

(deftest initialization-non-transport-failure-stays-machine-failure
  ;; Fail-closed twin of the above, and of
  ;; initialization-failure-opens-emergency-stop-line: anything that is not a
  ;; recognised transport condition keeps the full machine-failure contract.
  (let [findings (atom [])
        result
        (runner/run-opportunity!
         {:cohort? false
          :phase-log nil
          :phase-log-fn (fn [_] (throw (RuntimeException. "sink exploded")))
          :repair-system-record-fn
          (fn [finding]
            (swap! findings conj finding)
            (assoc finding :repair/id "repair-initialization-unknown"))
          :queue-fn (fn [_])})]
    (is (= :initialization-failed (get-in result [:data :failure-kind])))
    (is (= :machine-failure (:repair-class (first @findings))))
    (is (= [:distinct-repair-commit :independent-review
            :grounded-repair :distinct-production-shaped-successor]
           (:requires (:discharge-contract (first @findings)))))))

;; --- revision round 2 (reviewer finding) ------------------------------------
;; Consulting transport typing FIRST at the outer boundary was a fail-open: an
;; ex-info{:failure-kind :build-failed} wrapping any transport cause had that
;; cause found by the chain walk and was downgraded to :environmental-hold, so a
;; real typed machine failure escaped its own contract. Precedence at this
;; boundary now matches failure-kind-from exactly.

(deftest outer-boundary-does-not-downgrade-typed-machine-failures
  (testing "explicit typing wins over a transport cause deeper in the chain"
    (let [findings (atom [])
          result
          (runner/run-opportunity!
           {:cohort? false
            :phase-log nil
            :phase-log-fn (fn [_]
                            (throw (ex-info "build failed"
                                            {:failure-kind :build-failed}
                                            (java.net.http.HttpTimeoutException.
                                             "request timed out"))))
            :repair-system-record-fn
            (fn [finding]
              (swap! findings conj finding)
              (assoc finding :repair/id "repair-initialization-typed"))
            :queue-fn (fn [_])})]
      (is (= :build-failed (get-in result [:data :failure-kind]))
          "a typed machine failure must keep its own kind")
      (is (= :machine-failure (:repair-class (first @findings)))
          "and must NOT be downgraded to environmental by its transport cause")
      (is (= [:distinct-repair-commit :independent-review
              :grounded-repair :distinct-production-shaped-successor]
             (:requires (:discharge-contract (first @findings)))))))
  (testing "a bare transport failure is still typed environmental"
    ;; attempt-058's own case must survive the precedence change.
    (let [findings (atom [])
          result
          (runner/run-opportunity!
           {:cohort? false
            :phase-log nil
            :phase-log-fn (fn [_]
                            (throw (java.net.http.HttpTimeoutException.
                                    "request timed out")))
            :repair-system-record-fn
            (fn [finding]
              (swap! findings conj finding)
              (assoc finding :repair/id "repair-initialization-transport-2"))
            :queue-fn (fn [_])})]
      (is (= :transport-timeout (get-in result [:data :failure-kind])))
      (is (= :environmental-hold (:repair-class (first @findings)))))))

;; --- class-based transport typing --------------------------------------------
;; repair-initialization-6dd14f9e: SocketException "Socket closed" at
;; :initialization. The transport map was keyed on EXACT class name, so every
;; subclass fell through — java.net.BindException took the machine-failure
;; contract, and the bare-SocketException carve-out was keyed on one literal
;; message, so the identical condition worded "Socket is closed" landed on the
;; opposite side. Matching is now by class, with the message gate enumerated.

(deftest transport-typing-matches-by-class-not-name
  (testing "subclasses of a recognised condition are covered without enumeration"
    (doseq [e [(java.net.http.HttpConnectTimeoutException. "connect timed out")
               (java.net.BindException. "address already in use")
               (java.net.NoRouteToHostException. "no route")]]
      (is (= :environmental-hold (repair-class-for* (failure-kind-from* e)))
          (str (.getName (class e)) " is a transport condition"))))
  (testing "the recorded initialization failure types environmental"
    (let [e (java.net.SocketException. "Socket closed")]
      (is (= :transport-unavailable (failure-kind-from* e)))
      (is (= :environmental-hold (repair-class-for* (failure-kind-from* e)))))))

(deftest transport-message-typing-is-enumerated-not-fuzzy
  (testing "known JDK wordings for a channel that went away are all typed alike"
    ;; Before this, only "socket closed" was listed, so "Socket is closed" —
    ;; thrown by several JDK call sites for the SAME condition — took the
    ;; machine-failure contract. That was a coin-flip on wording, not a policy.
    (doseq [m ["Socket closed" "socket closed"
               "Connection reset" "Broken pipe" "  CONNECTION RESET BY PEER  "]]
      (is (= :transport-unavailable
             (failure-kind-from* (java.net.SocketException. m)))
          (str "SocketException " (pr-str m) " must type as transport"))))
  (testing "FAIL-CLOSED: a bare SocketException outside the set stays heavy"
    ;; Not fuzzy-matched: an unrecognised wording keeps the machine-failure
    ;; contract, so a genuine use-after-close bug is not laundered into an
    ;; environmental hold.
    (doseq [e [(java.net.SocketException.)
               (java.net.SocketException. "some unrelated socket problem")]]
      (is (= :untyped-failure (failure-kind-from* e)))
      (is (= :machine-failure (repair-class-for* (failure-kind-from* e))))))
  (testing "\"Socket is closed\" is LOCAL use-after-close and stays a machine fault"
    ;; One word from a listed entry, and on the opposite side on purpose. On this
    ;; JDK, calling getInputStream/getOutputStream/setSoTimeout/bind on a closed
    ;; java.net.Socket throws SocketException "Socket is closed" — that is our
    ;; own code misusing a socket, not a channel that went away, so it must NOT
    ;; discharge as :environmental-hold.
    (let [e (java.net.SocketException. "Socket is closed")]
      (is (= :untyped-failure (failure-kind-from* e)))
      (is (= :machine-failure (repair-class-for* (failure-kind-from* e)))))
    (is (= :transport-unavailable
           (failure-kind-from* (java.net.SocketException. "Socket closed")))
        "while \"Socket closed\" — a blocking op aborted by channel loss — is transport")))

(deftest ineligible-trigger-reaches-the-boundary-as-environmental-hold
  "Independent review of 4fea3b3 (codex-1): the cohort test proves the throw
  carries :failure-kind :trigger-ineligible, but stops before the runner mapping
  it was changed to feed. This drives the WHOLE path — a real cohort refusal,
  produced by the real start-attempt! guard, through run-opportunity!'s outer
  boundary — and asserts the obligation that actually gets recorded.

  Grounds repair-initialization-1f894133, where exactly this refusal was
  recorded as :initialization-failed / :machine-failure."
  (let [data-root (tmp-cohort-root)
        prereg-path (str data-root "/cohort.edn")
        _ (tiny-target-prereg prereg-path)
        _ (cohort/activate! prereg-path data-root)
        repair-calls (atom [])
        queue-calls (atom [])]
    (with-redefs [cohort/default-preregistration prereg-path
                  cohort/default-data-root data-root]
      (let [result (runner/run-opportunity!
                    {;; NOT in :allowed-triggers — the real guard refuses this
                     :trigger :instrumented-campaign-repair
                     :cohort? true
                     :opportunity-id "test/ineligible-trigger"
                     :semantic-epoch :test
                     :author "zai-1"
                     :reviewer "codex-1"
                     :repair-system-record-fn
                     (fn [m] (swap! repair-calls conj m)
                       (assoc m :repair/id "repair-ineligible-trigger"))
                     :queue-fn (fn [m] (swap! queue-calls conj m)
                                 {:morning-brief/addendum-id "brief-ineligible"})})
            finding (first @repair-calls)]
        (is (= :incomplete (:outcome result)))
        (is (= 1 (count @repair-calls))
            "the refusal must still open exactly one stop-line obligation")
        (is (= :trigger-ineligible (get-in result [:data :failure-kind]))
            "the typed kind must survive to the boundary, not become :initialization-failed")
        (is (= :trigger-ineligible (:failure-kind finding)))
        (is (= :environmental-hold (:repair-class finding))
            "an unpreregistered trigger is a precondition, not a machine defect")
        (is (= [:cleared-precondition :grounded-production-shaped-successor]
               (:requires (:discharge-contract finding)))
            "so the discharge is: preregister the trigger, or use an eligible one")
        (is (= :trigger-ineligible (get-in (first @queue-calls) [:failure :kind]))
            "and the morning brief carries the same typed kind")))))

(deftest eligible-trigger-does-not-open-a-trigger-ineligible-obligation
  "Fail-closed twin: the guard is not disabled. An ELIGIBLE trigger on the same
  cohort must not produce a :trigger-ineligible finding."
  (let [data-root (tmp-cohort-root)
        prereg-path (str data-root "/cohort.edn")
        _ (tiny-target-prereg prereg-path)
        _ (cohort/activate! prereg-path data-root)
        repair-calls (atom [])]
    (with-redefs [cohort/default-preregistration prereg-path
                  cohort/default-data-root data-root]
      (let [result (runner/run-opportunity!
                    {:trigger :test-trigger
                     :cohort? true
                     :opportunity-id "test/eligible-trigger"
                     :semantic-epoch :test
                     :author "zai-1"
                     :reviewer "codex-1"
                     ;; This test exercises trigger admission only. An empty
                     ;; injected roster bounds it immediately after the real
                     ;; cohort guard, before substrate, selection, or a live
                     ;; Agency author can be reached.
                     :roster-fn (fn [_] {})
                     :repair-system-record-fn
                     (fn [m] (swap! repair-calls conj m)
                       (assoc m :repair/id "repair-eligible"))
                     :queue-fn (fn [_] {:morning-brief/addendum-id "brief"})})]
        (is (not= :trigger-ineligible (get-in result [:data :failure-kind]))
            "an eligible trigger must never be typed :trigger-ineligible")
        (is (empty? (filter #(= :trigger-ineligible (:failure-kind %)) @repair-calls))
            "and must not open a trigger-ineligible obligation")))))

;; --- claimed-commit shape (repair-canary-067cd51a) ---------------------------
;; A cure turn had its claimed commit extracted as "/test_fm001_budgeted_solve.py"
;; — a file path — and the bounce was rejected :cure-commit-unresolved. That
;; blames the author for naming a commit that is not there, when in fact nothing
;; ever extracted a commit. find-commit-repo shells `git cat-file -e <x>^{commit}`
;; for any string, so the two faults were indistinguishable.

(deftest cure-claiming-a-non-commit-is-typed-malformed-not-unresolved
  "The measured case from repair-canary-067cd51a: a cure turn whose claimed
  commit was extracted as a FILE PATH. It must still be rejected — nothing is
  loosened — but typed as an extraction fault, not as the author naming a
  commit that is not there."
  (let [cure-prompts (atom [])
        opts {:build-cure-retries 1
              :phase-log (str (System/getProperty "java.io.tmpdir")
                              "/cure-malformed-test-phase.log")
              :dispatch-fn (fn [_ _ _ _ prompt]
                             (swap! cure-prompts conj prompt)
                             {:job-id "cure-1"})
              :poll-fn (fn [_ _]
                         {:job-id "cure-1" :state "done"
                          :artifact-ref "/test_fm001_budgeted_solve.py"
                          :result-summary
                          (str "FULL_LOOP_FEATURE_CARD: {:built \"x\" :want-coverage \"y\""
                               " :matches-intent? true :things-to-try [\"a -> b\"]}"
                               "\nFULL_LOOP_AUTHOR: DONE /test_fm001_budgeted_solve.py")})
              :resolve-build-fn (fn [_] nil)}
        thrown (try
                 (#'runner/build-cure-loop
                  opts {} "author-x" (atom 0)
                  "target-x" "orig123" "/repo" ["src/x.clj"]
                  {:job-id "author-1" :state "done"
                   :result-summary "FULL_LOOP_AUTHOR: DONE orig123"
                   :events [{:type "text" :text "prose"}]}
                  false
                  {:repo "/repo" :pre-dispatch-head "base"})
                 nil
                 (catch clojure.lang.ExceptionInfo e (ex-data e)))]
    (is (some? thrown))
    (is (= false (get-in thrown [:build-retries 0 :cured?]))
        "still rejected: the guard narrows the diagnosis, not the gate")
    (is (= :cure-commit-malformed
           (get-in thrown [:build-retries 0 :cure-rejected]))
        "a file path is not an unresolvable commit; it is not a commit")
    (is (= "/test_fm001_budgeted_solve.py"
           (get-in thrown [:build-retries 0 :claimed-commit]))
        "the offending value is preserved so the extractor can be found")))

(deftest commit-ish-accepts-real-object-names
  (testing "full and abbreviated shas, either case"
    (doseq [c ["7ed10a45534bb135afb26cf494a3d0f240092535"
               "5818c67"
               "c0798dfd8a8200123fe93a845c8547beebe23de0"
               "ABCDEF1234"]]
      (is (runner/commit-ish? c) (str (pr-str c) " is a git object name")))))

(deftest commit-ish-rejects-things-that-are-not-commits
  (testing "the measured case: a file path fragment"
    (is (not (runner/commit-ish? "/test_fm001_budgeted_solve.py"))))
  (testing "other non-commits"
    (doseq [c [nil "" "   " "abc123" "not-a-sha"
               "tests/test_fm001_budgeted_solve.py"
               "7ed10a45534bb135afb26cf494a3d0f240092535extra"
               "7ed10a4 " "zzzzzzz" :keyword 42]]
      (is (not (runner/commit-ish? c))
          (str (pr-str c) " must not pass as a commit")))))

(deftest resolve-build-refuses-non-commit-without-touching-git
  "Guard at the source, so every caller is protected — not just the cure path.

  Asserting only that the result is nil would NOT test the guard: git rejects a
  bogus ref anyway, so deleting the guard leaves the return value unchanged.
  (Mutation testing showed exactly that.) What the guard actually buys is that a
  value which is not a commit never reaches the repository probe at all, so this
  asserts find-commit-repo is not called."
  (let [probes (atom [])]
    (with-redefs [futon2.aif.full-loop-runner/find-commit-repo
                  (fn [c] (swap! probes conj c) nil)]
      (doseq [bad ["/test_fm001_budgeted_solve.py" nil "not-a-sha" "" "abc123"]]
        (is (nil? (runner/resolve-build bad))))
      (is (empty? @probes)
          "a non-commit must never be probed against the repositories")
      ;; and a well-formed sha DOES reach the probe — the guard is a filter,
      ;; not a blanket refusal.
      (is (nil? (runner/resolve-build "deadbee1234567890abcdef1234567890abcdef1")))
      (is (= ["deadbee1234567890abcdef1234567890abcdef1"] @probes)))))


;; --- revision prompts must name the AUTHORED commit -------------------------
;; repair-canary-067cd51a: the revision prompt carried prior-commits ["c0798df"]
;; — the pre-dispatch head, a trigger-classification test commit — instead of
;; the repair actually under review. The reviewer read an unrelated commit and
;; rejected on artifact-binding grounds.

(deftest revision-prompt-names-the-observed-authored-commit
  (let [prompts (atom [])
        authored "f0e7778d324a6560582ac231c45ab2085b8bae73"
        base "c0798dfd8a8200123fe93a845c8547beebe23de0"
        opts {:revision-rounds 1
              :phase-log (str (System/getProperty "java.io.tmpdir")
                              "/revision-binding-test-phase.log")
              :dispatch-fn (fn [_ _ _ _ prompt]
                             (swap! prompts conj prompt)
                             {:job-id "rev-1"})
              :poll-fn (fn [_ _] {:job-id "rev-1" :state "error"})
              :repo-head-observation-fn (fn [_] {:head base :observed-at-ms 0})}]
    (try
      (#'runner/run-revision-round
       opts {} "claude-7" "codex-1" (atom 0) "target-x" {}
       "/home/joe/code/futon2" base ["src/x.clj"]
       {:job-id "author-1" :state "done"}
       ;; artifact-binding observed a VALID authored commit
       {:fresh-author? true :repo "/home/joe/code/futon2" :commit authored}
       {:job-id "review-1" :state "done"
        :result-summary "FULL_LOOP_REVIEW: REQUEST_CHANGES do the thing"}
       nil [])
      (catch clojure.lang.ExceptionInfo _ nil))
    (is (seq @prompts) "a revision must have been dispatched")
    (let [prompt (first @prompts)]
      (is (str/includes? prompt authored)
          "the reviewer must be pointed at the commit the author made")
      (is (not (str/includes? prompt base))
          "and NOT at the pre-dispatch head, which is an unrelated commit"))))

(deftest revision-prompt-falls-back-when-no-valid-authored-commit
  "artifact-binding/:commit is nil unless the observation was valid. With no
  authored commit observed, the prompt falls back rather than inventing one —
  and a malformed binding is never sent as a commit."
  (let [prompts (atom [])
        base "c0798dfd8a8200123fe93a845c8547beebe23de0"
        run (fn [binding]
              (reset! prompts [])
              (let [opts {:revision-rounds 1
                          :phase-log (str (System/getProperty "java.io.tmpdir")
                                          "/revision-fallback-test-phase.log")
                          :dispatch-fn (fn [_ _ _ _ p]
                                         (swap! prompts conj p) {:job-id "rev-1"})
                          :poll-fn (fn [_ _] {:job-id "rev-1" :state "error"})
                          :repo-head-observation-fn (fn [_] {:head base :observed-at-ms 0})}]
                (try
                  (#'runner/run-revision-round
                   opts {} "claude-7" "codex-1" (atom 0) "target-x" {}
                   "/home/joe/code/futon2" base ["src/x.clj"]
                   {:job-id "author-1" :state "done"}
                   binding
                   {:job-id "review-1" :state "done"
                    :result-summary "FULL_LOOP_REVIEW: REQUEST_CHANGES x"}
                   nil [])
                  (catch clojure.lang.ExceptionInfo _ nil))
                (first @prompts)))]
    (testing "no observed commit -> fall back to the bound commit"
      (is (str/includes? (run {:fresh-author? true :commit nil}) base)))
    (testing "a non-commit binding is NOT sent as a commit"
      (let [prompt (run {:fresh-author? true :commit "/tests/x_test.clj"})]
        (is (not (str/includes? prompt "/tests/x_test.clj")))
        (is (str/includes? prompt base))))))


;; --- artifact-ref shape at the author-wait boundary (repair-canary-de75cee9) --
;; The obligation records :artifact-ref "/eoi_network_test.clj" — a file path
;; scraped from author text. Treating that as a claimed artifact reported
;; :artifact-binding-mismatch, which says Agency named a commit that observation
;; could not validate. It named no commit at all.

(deftest unvalidated-artifact-failure-separates-malformed-from-mismatch
  (testing "a file path is not a commit Agency claimed — it is not a commit"
    ;; The measured value from canary-de75cee9.
    (let [f (runner/unvalidated-artifact-failure true "/eoi_network_test.clj" nil)]
      (is (= :artifact-ref-malformed (:failure-kind f)))
      (is (= "/eoi_network_test.clj" (:artifact-ref f))
          "the offending value travels so the extractor can be found")))
  (testing "a well-formed sha observation could not validate IS a mismatch"
    (let [f (runner/unvalidated-artifact-failure
             true "deadbee1234567890abcdef1234567890abcdef1" nil)]
      (is (= :artifact-binding-mismatch (:failure-kind f)))
      (is (nil? (:artifact-ref f)))))
  (testing "nothing to report when observation validated a commit"
    (is (nil? (runner/unvalidated-artifact-failure
               true "/eoi_network_test.clj" "abc1234")))
    (is (nil? (runner/unvalidated-artifact-failure
               true "deadbee1234567890abcdef1234567890abcdef1" "abc1234"))))
  (testing "and nothing to report when the author is not fresh"
    ;; The non-fresh path legitimately carries an Agency ref through;
    ;; resolve-build refuses a non-commit downstream.
    (is (nil? (runner/unvalidated-artifact-failure false "/eoi_network_test.clj" nil)))
    (is (nil? (runner/unvalidated-artifact-failure true nil nil)))))

;; --- card recovery from the author's own text events ------------------------
;; repair-canary-de75cee9: Agency concatenates the author's separate text
;; blocks into :result with NO separator, so a card that began its own block
;; ends up abutting the previous block's last word:
;;   "...doing it precisely:FULL_LOOP_FEATURE_CARD: {...}"
;; The line-anchored search over :result cannot match, and the run was reported
;; :marker-not-at-durable-prefix — blaming the author's ordering for what the
;; concatenation destroyed, and discarding a recoverable card.

(def ^:private canary-card-text
  (str "FULL_LOOP_FEATURE_CARD: {:built \"a carrier\" :want-coverage \"tension identity\""
       " :matches-intent? true :things-to-try [\"bb test -> 4 tests\"]}"))

(deftest card-is-recovered-from-the-event-that-begins-with-it
  (testing "the measured shape: blocks concatenated with no separator"
    (let [job {:result-summary "Found a genuinely witnessed join: ..."
               :result (str "Third malformed attempt; doing it precisely:" canary-card-text)
               :events [{:type "text" :text "Found a genuinely witnessed join: ..."}
                        {:type "text" :text "Third malformed attempt; doing it precisely:"}
                        {:type "text" :text (str canary-card-text "\n\nStep (e) is ...")}]}
          {:keys [card source reason]} (#'runner/feature-card-validation job)]
      (is (nil? reason) "a card that began its own block must not be discarded")
      (is (= :events source))
      (is (= "a carrier" (:built card)))
      (is (= ["bb test -> 4 tests"] (:things-to-try card))))))

(deftest event-recovery-does-not-widen-the-gate
  (testing "a marker quoted MID-event never matches"
    (let [job {:result-summary "prose"
               :result "prose"
               :events [{:type "text"
                         :text (str "I was asked to emit " canary-card-text " at the top.")}]}]
      (is (= :marker-not-at-durable-prefix
             (:reason (#'runner/feature-card-validation job)))
          "quoting the marker inside a sentence must not supply a card")))
  (testing "non-text events are never read"
    (let [job {:result-summary "prose"
               :result "prose"
               :events [{:type "tool_use" :text canary-card-text}
                        {:type "done" :text canary-card-text}]}]
      (is (= :marker-not-at-durable-prefix
             (:reason (#'runner/feature-card-validation job))))))
  (testing "no events, no card: the typed reason is unchanged"
    (is (= :marker-not-at-durable-prefix
           (:reason (#'runner/feature-card-validation
                     {:result-summary "prose" :result "prose"}))))
    (is (= :missing-marker
           (:reason (#'runner/feature-card-validation
                     {:result-summary "" :result ""}))))))

(deftest earlier-recovery-paths-still-win
  (testing "a leading card in the summary is still the fast path"
    (let [job {:result-summary canary-card-text
               :events [{:type "text" :text canary-card-text}]}]
      (is (= :text (:source (#'runner/feature-card-validation job))))))
  (testing "a line-anchored card in :result is still preferred over events"
    (let [job {:result-summary "prose"
               :result (str "prose\n" canary-card-text)
               :events [{:type "text" :text canary-card-text}]}]
      (is (= :result (:source (#'runner/feature-card-validation job)))))))

(deftest recovery-artifact-failure-separates-malformed-from-missing
  (testing "a completed recovery with no artifact-ref is still reported missing"
    (is (= :recovery-artifact-missing
           (:failure-kind (runner/recovery-artifact-failure
                           :author-wait {:job-id "j" :state "done"})))))
  (testing "a path scraped into artifact-ref is malformed, not missing"
    ;; The measured value from canary-da9681ce's author job, whose real commit
    ;; was f285e40 in futon2.
    (let [failure (runner/recovery-artifact-failure
                   :author-wait {:job-id "j" :state "done"
                                 :artifact-ref "/eoi_network_test.clj"})]
      (is (= :recovery-artifact-ref-malformed (:failure-kind failure)))
      (is (= "/eoi_network_test.clj" (:artifact-ref failure))
          "the offending value is carried so the fault is locatable")))
  (testing "a commit-shaped ref has nothing to report"
    (is (nil? (runner/recovery-artifact-failure
               :author-wait {:job-id "j" :state "done"
                             :artifact-ref "f285e40f6cd150410d66c7f8c555660dba9d4003"}))))
  (testing "the gate does not widen past a completed author-wait recovery"
    (is (nil? (runner/recovery-artifact-failure :author-wait nil)))
    (is (nil? (runner/recovery-artifact-failure
               :reviewer-wait {:job-id "j" :state "done"
                               :artifact-ref "/eoi_network_test.clj"})))
    (is (nil? (runner/recovery-artifact-failure
               :author-wait {:job-id "j" :state "running"
                             :artifact-ref "/eoi_network_test.clj"})))))

(deftest done-author-recovery-with-a-non-commit-artifact-is-refused-before-dispatch
  ;; Outer boundary: the typed refusal must reach run-opportunity!'s result,
  ;; not stop at the predicate. Without the shape check the snapshot was adopted
  ;; as the authored turn and its path became the reviewed commit.
  (let [supersessions (atom [])
        dispatches (atom [])
        findings (atom [])
        stop-line {:repair/id "repair-path-ref" :repair/status :open
                   :repair/class :incomplete-recoverable
                   :attempt-id "attempt-path-ref" :failure-stage :author-wait
                   :failure-data {:job-id "path-ref-job"}}
        result
        (runner/run-opportunity!
         (merge (isolated-runner-opts)
                {:repair-open-fn (constantly [stop-line])
                 :read-job-fn (fn [& _] {:job-id "path-ref-job" :state "done"
                                         :artifact-ref "/eoi_network_test.clj"})
                 :repair-system-record-fn
                 (fn [finding]
                   (let [finding (assoc finding :repair/id
                                        "repair-path-ref-successor")]
                     (swap! findings conj finding)
                     finding))
                 :repair-supersede-fn
                 (fn [& args] (swap! supersessions conj args))
                 :dispatch-fn (fn [& args] (swap! dispatches conj args))}))]
    (is (= :recovery-artifact-ref-malformed (get-in result [:data :failure-kind])))
    (is (= "/eoi_network_test.clj"
           (get-in result [:data :error-data :artifact-ref]))
        "the offending value survives into the durable failure record")
    (is (= "/eoi_network_test.clj"
           (get-in @findings [0 :failure-data :artifact-ref]))
        "and into the successor obligation, so the fault is locatable later")
    (is (= 1 (count @supersessions)))
    (is (empty? @dispatches)
        "no replacement turn is dispatched on a typed recovery refusal")))
