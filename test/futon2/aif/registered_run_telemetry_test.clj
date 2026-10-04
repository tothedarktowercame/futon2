(ns futon2.aif.registered-run-telemetry-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.registered-run-telemetry :as sut])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- terminal-job [run-id click-id job-id usage]
  {:job-id job-id :state "done"
   :harness {:kind :war-machine :basis :producer-context
             :execution-id run-id :source-ref click-id}
   :usage usage})

(defn- usage-ledger [run-id click-id jobs]
  (let [ledger (atom {:order [] :jobs {}})]
    (doseq [[job-id usage] jobs]
      (sut/register-job! ledger {:run-id run-id :click-id click-id :job-id job-id
                                 :role :author :phase :enactment})
      (sut/retain-terminal-job! ledger (terminal-job run-id click-id job-id usage)))
    ledger))

(deftest aggregates-monotonic-phase-ends
  (is (= {:select 7 :wait 11}
         (:phase-timings-ms
          (sut/phase-timings [{:phase :select :transition :start}
                              {:phase :select :transition :end :duration-ms 7}
                              {:phase :wait :transition :end :duration-ms 11}])))))

(defn- dwell [run-id phase start-ns end-ns restart]
  {:schema :wm/debugger-dwell-v1 :run-id run-id :phase phase
   :condition-kind :test/breakpoint
   :stopped-at-monotonic-ns start-ns :resumed-at-monotonic-ns end-ns
   :restart-choice restart :duration-ms (quot (- end-ns start-ns) 1000000)})

(deftest debugger-dwell-is-separated-from-machine-active-phase-time
  (let [minute-ns (* 60 1000000000)
        ledger (atom {:run-id "run" :errors []
                      :receipts [(dwell "run" :selection 0 (* 24 minute-ns)
                                        :use-value)]})
        timing (sut/phase-timings
                [{:phase :selection :transition :end
                  :duration-ms (+ (* 24 60 1000) 125)}]
                ledger "run")]
    (is (= (* 24 60 1000) (:debugger-stopped-ms timing)))
    (is (= 125 (get-in timing [:phase-timings-ms :selection])))
    (is (= (+ (* 24 60 1000) 125)
           (get-in timing [:phase-wall-timings-ms :selection])))
    (is (= :complete (:debugger-dwell-status timing)))))

(deftest repeated-stops-sum-and-subtraction-has-a-zero-floor
  (let [ledger (atom {:run-id "run" :errors []
                      :receipts [(dwell "run" :selection 0 100000000 :retry)
                                 (dwell "run" :selection 200000000 500000000
                                        :abort)]})
        timing (sut/phase-timings
                [{:phase :selection :transition :end :duration-ms 150}
                 {:phase :selection :transition :end :duration-ms 200}]
                ledger "run")]
    (is (= 400 (:debugger-stopped-ms timing)))
    (is (= 0 (get-in timing [:phase-timings-ms :selection])))
    (is (= 400 (get-in timing [:debugger-stopped-by-phase-ms :selection])))))

(deftest foreign-dwell-ledger-is-typed-missing-and-never-subtracted
  (let [ledger (atom {:run-id "other" :errors []
                      :receipts [(dwell "other" :selection 0 90000000 :retry)]})
        timing (sut/phase-timings
                [{:phase :selection :transition :end :duration-ms 100}]
                ledger "run")]
    (is (= :typed-missing (:debugger-dwell-status timing)))
    (is (= 100 (get-in timing [:phase-timings-ms :selection])))
    (is (= 0 (:debugger-stopped-ms timing)))
    (is (= :debugger-dwell-run-mismatch
           (get-in timing [:debugger-dwell-errors 0 :reason])))))

(deftest aggregates-provider-usage-without-imputation
  (let [r (sut/model-usage
           (usage-ledger "run" "click"
                         [["a" {:input_tokens 10 :output_tokens 4 :total_tokens 14}]
                          ["r" {:prompt_tokens 7 :completion_tokens 3}]])
           {:run-id "run" :click-id "click"})]
    (is (= :complete (:status r)))
    (is (= [17 7 24] ((juxt :input-tokens :output-tokens :total-tokens) r)))))

(deftest joins-real-agency-json-enums
  ;; Shape pinned from GET /api/alpha/invoke/jobs/
  ;; invoke-1791074118947-31367-c368e0c8 on 2026-10-04. JSON parsing keeps
  ;; harness enum values as strings even though its map keys are keywordized.
  (let [run-id "2026-10-04-d197fe96-53e4-498b-b870-15b8c541e3b1"
        click-id "wm-click-998f4b67-58c7-48d4-88c7-fc063c8db4c8"
        job-id "invoke-1791074118947-31367-c368e0c8"
        ledger (atom {:order [] :jobs {}})
        job {:job-id job-id :state "done"
             :harness {:kind "war-machine" :basis "producer-context"
                       :execution-id run-id :source-ref click-id}
             :usage {:input_tokens 2493187 :cached_input_tokens 2303232
                     :output_tokens 5303 :source "codex"
                     :model "gpt-5.6-sol"}}]
    (sut/register-job! ledger {:run-id run-id :click-id click-id :job-id job-id
                               :role :author :phase :author-dispatch})
    (sut/retain-terminal-job! ledger job)
    (let [usage (sut/model-usage ledger {:run-id run-id :click-id click-id})]
      (is (= :complete (:status usage)))
      (is (= [2493187 5303 2498490]
             ((juxt :input-tokens :output-tokens :total-tokens) usage)))
      (is (= 2303232 (:cached-input-tokens usage))))))

(deftest missing-provider-receipt-stays-typed
  (let [ledger (usage-ledger "run" "click" [["a" {:input_tokens 1
                                                       :output_tokens 2}]])
        _ (sut/register-job! ledger {:run-id "run" :click-id "click" :job-id "r"
                                     :role :reviewer :phase :review})
        r (sut/model-usage ledger {:run-id "run" :click-id "click"})]
    (is (= :partial (:status r)))
    (is (= [{:job-id "r" :reason :terminal-receipt-missing}] (:missing r)))))

(deftest separates-cached-from-uncached-provider-input
  (let [r (sut/model-usage
           (usage-ledger "run" "click"
                         [["a" {:input_tokens 100 :cached_input_tokens 80
                                :output_tokens 5 :source :codex}]])
           {:run-id "run" :click-id "click"})]
    (is (= [100 80 20]
           ((juxt :input-tokens :cached-input-tokens :uncached-input-tokens) r)))
    (is (= {:input-tokens 100 :cached-input-tokens 80 :uncached-input-tokens 20
            :output-tokens 5 :total-tokens 105 :provider :codex
            :job-id "a" :run-id "run" :click-id "click" :role :author
            :phase :enactment :status :complete :unit :tokens}
           (first (:jobs r))))))

(deftest aggregates-zai-cost-schema-and-retains-model
  (let [r (sut/model-usage
           (usage-ledger "run" "click"
                         [["z" {:cost/input-tokens 9 :cost/output-tokens 4
                                :cost/total-tokens 13 :cost/source :zai
                                :cost/model "glm-4.6"}]])
           {:run-id "run" :click-id "click"})]
    (is (= :complete (:status r)))
    (is (= 13 (:total-tokens r)))
    (is (= {:input-tokens 9 :output-tokens 4 :total-tokens 13
            :model "glm-4.6" :provider :zai :job-id "z"
            :run-id "run" :click-id "click" :role :author
            :phase :enactment :status :complete :unit :tokens}
           (first (:jobs r))))))

(deftest ledger-rejects-foreign-joins-invalid-usage-and-result-contamination
  (let [ledger (atom {:order [] :jobs {}})
        _ (sut/register-job! ledger {:run-id "run" :click-id "click"
                                     :job-id "joined" :role :author :phase :build})
        _ (sut/retain-terminal-job!
           ledger (terminal-job "other-run" "click" "joined"
                                {:input_tokens 1 :output_tokens 2 :total_tokens 3}))
        _ (sut/register-job! ledger {:run-id "run" :click-id "click"
                                     :job-id "bad-sum" :role :reviewer :phase :review})
        _ (sut/retain-terminal-job!
           ledger (terminal-job "run" "click" "bad-sum"
                                {:input_tokens 1 :output_tokens 2 :total_tokens 99}))
        result (sut/model-usage ledger {:run-id "run" :click-id "click"})]
    (is (= :typed-missing (:status result)))
    (is (= [{:job-id "joined" :reason :run-click-job-join-mismatch}
            {:job-id "bad-sum" :reason :provider-usage-missing-or-invalid}]
           (:missing result)))
    ;; This exact foreign historical shape is not in the ledger and cannot
    ;; enter the projection merely by appearing in arbitrary result evidence.
    (is (not-any? #(= "invoke-1790901102236-29976-7e3f6d9c" (:job-id %))
                  (:jobs result)))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"more than once"
                          (sut/register-job! ledger
                                             {:run-id "run" :click-id "click"
                                              :job-id "joined"})))))

(deftest production-dispatch-and-terminal-poll-own-the-ledger-join
  (let [ledger (atom {:order [] :jobs {}})
        posted (atom nil)
        opts {:agency-base "http://agency" :run-id "run-1" :click-id "click-1"
              :registered-run/job-ledger ledger
              :wm-phase-state (atom {:phase :review})
              :poll-ms 1 :poll-sleep-fn (fn [_])}]
    (with-redefs-fn {#'runner/post-json!
                     (fn [_ body] (reset! posted body) {:job-id "job-1"})}
      #(runner/dispatch! opts "review-seat" "wm-full-loop" "M-one" "review"))
    (is (= {:kind :war-machine :basis :producer-context
            :execution-id "run-1" :source-ref "click-1"}
           (:harness @posted)))
    (is (= ["job-1"] (:order @ledger)))
    (with-redefs [runner/read-job!
                  (fn [_ _] (terminal-job "run-1" "click-1" "job-1"
                                          {:input_tokens 4 :output_tokens 2
                                           :total_tokens 6}))]
      (runner/poll-job! opts "job-1"))
    (let [usage (sut/model-usage ledger {:run-id "run-1" :click-id "click-1"})]
      (is (= :complete (:status usage)))
      (is (= [4 2 6] ((juxt :input-tokens :output-tokens :total-tokens) usage)))
      (is (= :review (get-in usage [:jobs 0 :phase]))))
    (is (= :wm-job-ledger-unavailable
           (try
             (runner/dispatch! {:agency-base "http://unused" :run-id "run-1"
                                :click-id "click-1"}
                               "seat" "wm-full-loop" "M-one" "work")
             nil
             (catch clojure.lang.ExceptionInfo e (:failure-kind (ex-data e))))))))

(deftest chronology-cites-revisions-and-change
  (with-redefs [sut/source-revisions
                (fn [_] {"futon2" "after" "futon3-pattern-library" "patterns"})]
    (let [start {:prior-opportunity {:source-revisions-after {"futon2" "before"}}
                 :source-revisions-before {"futon2" "after"}}
          r (sut/chronology-finish start {} {:freshness {:missions 3}})]
      (is (= "patterns" (:pattern-library-digest r)))
      (is (= {:before "before" :after "after"}
             (get-in r [:intervening-change-receipts "futon2"])))
      (is (= 64 (count (:substrate-snapshot-digest r)))))))

(deftest production-record-boundary-retains-all-three-preflight-surfaces
  (let [dir (.toFile (Files/createTempDirectory
                      "registered-telemetry-" (make-array FileAttribute 0)))
        phases (atom [{:phase :selection :transition :end :duration-ms 9}])
        ledger (usage-ledger "telemetry-001" "click-001"
                             [["a" {:input_tokens 2 :output_tokens 1}]
                              ["r" {:input_tokens 3 :output_tokens 2}]])
        opts {:run-record-dir (.getPath dir)
              :click-id "click-001"
              :registered-run/job-ledger ledger
              :scan-render-fn (fn [& _] nil)
              :phase-events/state phases
              :run-timing/start-nanos 0
              :nano-time-fn (constantly 12000000)
              :registered-run/source-roots {"futon2" (.getPath dir)
                                             "futon3-pattern-library" (.getPath dir)}
              :registered-run/chronology-start
              {:schema :wm/registered-run-chronology-v1
               :source-revisions-before {"futon2" "a"
                                         "futon3-pattern-library" "p"}}}
        result {:outcome :offline-selection-replay
                :foreign-history {:job-id "invoke-1790901102236-29976-7e3f6d9c"
                                  :usage {:input_tokens 999 :output_tokens 999}}
                :checkpoints {}}]
    (with-redefs [sut/source-revisions
                  (fn [_] {"futon2" "b" "futon3-pattern-library" "q"})]
      (let [written (#'runner/persist-run-record!
                     opts "telemetry-001" "2026-10-01T00:00:00Z" result)
            record (edn/read-string (slurp (:run-record written)))]
        (is (= 12 (get-in record [:registered-run/timing :wall-clock-ms])))
        (is (= 9 (get-in record [:registered-run/timing
                                 :phase-timings-ms :selection])))
        (is (= 8 (get-in record [:registered-run/model-usage :total-tokens])))
        (is (= "q" (get-in record [:registered-run/chronology
                                    :pattern-library-digest])))
        (is (= 64 (count (get-in record [:registered-run/chronology
                                         :substrate-snapshot-digest]))))))))
