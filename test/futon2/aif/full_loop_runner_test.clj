(ns futon2.aif.full-loop-runner-test
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner])
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

(deftest real-opportunity-pins-construction-and-separates-review
  (let [constructed (atom nil)
        dispatches (atom [])
        queued (atom [])
        phases (atom [])
        result
        (runner/run-opportunity!
         {:cohort? false
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
        result
        (runner/run-opportunity!
         {:cohort? false
          :phase-log-fn (fn [_])
          :repair-open-fn (constantly [stop-line])
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
    (is (= stop-line (ffirst @resolutions)))
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
                           :timeout-ms 60000
                           :inactivity-timeout-ms 1000
                           :poll-ms 1}
                          "job-1")
        (is false "stalled jobs must not remain in the polling loop")
        (catch clojure.lang.ExceptionInfo e
          (is (= :agent-job-stalled (:outcome (ex-data e))))
          (is (= 200 (get-in (ex-data e) [:interrupt :status])))))
      (is (= ["http://agency/api/alpha/agents/zai-5/interrupt-invoke"]
             (mapv :url @posts))))))
