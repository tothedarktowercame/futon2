(ns run-tick-once-test
  (:require [babashka.http-client :as http]
            [clojure.test :refer [deftest is testing]]
            [futon2.report.war-machine :as wm]
            [futon2.run-tick-once :as tick]))

(deftest tick-record-preserves-producer-issued-run-id
  (let [record (#'tick/tick-run-record
                "run-issued-before-tick"
                "2026-08-31T00:00:00Z"
                {:count 1 :max-at "2026-08-30T00:00:00Z"}
                {:entries-read 1 :entries-limit 1}
                {:input-status {:inputs-read 1 :issues []}
                 :preference-stack (vec (repeat 5 {}))
                 :wm/route []}
                "stub:test"
                true)]
    (is (= "run-issued-before-tick" (:run/id record)))
    (is (= "2026-08-31T00:00:00Z" (:startedAt record)))
    (is (true? (:traceWritten record)))))

(deftest receipt-path-is-unique-per-run-test
  (let [date "2026-09-01"
        first-path (#'tick/receipt-path-for-run date "run-one")
        second-path (#'tick/receipt-path-for-run date "run-two")]
    (is (not= first-path second-path))
    (is (.endsWith first-path "tick-run-record-2026-09-01-run-one.edn"))
    (is (.endsWith second-path "tick-run-record-2026-09-01-run-two.edn"))))

(deftest diagnostic-judge-opts-carry-the-run-id-test
  (testing "RUN11: the id the receipt will carry reaches the judge, and so the trace"
    (let [opts (#'tick/diagnostic-judge-opts :selector {:version :test} "run-42")]
      (is (= "run-42" (:run-id opts))))
    (is (not (contains? (#'tick/diagnostic-judge-opts :selector {:version :test})
                        :run-id))
        "no run id passed ⇒ no key, so the trace record makes no run claim")))

(deftest receipt-and-trace-name-the-same-run-test
  (testing "RUN11: the receipt's :run/id is the key the trace record is selected by"
    (let [run-id "run-42"
          receipt (#'tick/tick-run-record run-id "2026-09-01T00:00:00Z"
                                          {:count 1 :max-at "x"}
                                          {:entries-read 1 :entries-limit 2}
                                          {:input-status {:inputs-read 0 :issues []}
                                           :preference-stack []
                                           :wm/route []}
                                          {:seam :test} true)
          judge-opts (#'tick/diagnostic-judge-opts :selector {:version :test} run-id)]
      (is (= (:run/id receipt) (:run-id judge-opts))
          "one id is minted per tick and both sides read it"))))

(deftest diagnostic-entrypoint-suppresses-portfolio-step-test
  (let [opts (#'tick/diagnostic-judge-opts :selector {:version :test})]
    (is (false? (:step-portfolio? opts)))
    (is (false? (:step-mission-detail-portfolio? opts)))
    (is (false? (:eval-invariant-fallback? opts)))))

(deftest diagnostic-stub-consults-controller-head-test
  (let [controller-head {:type :advance-mission :target "M-controller"}
        result (#'tick/stub-selection
                {:controller-ranking [{:rank 1 :action controller-head}
                                      {:rank 2 :action {:type :advance-mission
                                                       :target "M-habit"}}]
                 :scheduler-habit-ranking ["M-habit" "M-controller"]})]
    (is (= :controller (:consulted-ranking result)))
    (is (= ["M-controller"] (:selected-mission-ids result)))
    (is (= "stub:controller-head" (:selected-policy-id result)))))

(deftest full-diagnostic-tick-issues-no-http-post-test
  ;; The generator seam keeps the test hermetic while exercising the complete
  ;; run-tick-once lifecycle. Its body deliberately invokes both formerly
  ;; effectful producers with the exact options received from the entrypoint.
  (let [posts (atom [])
        generated-opts (atom nil)]
    (with-redefs-fn
      {#'http/post (fn [& request]
                     (swap! posts conj request)
                     {:status 500})
       #'tick/store-basis (constantly {:count 0 :max-at ""})
       #'tick/evidence-sample (constantly {:entries-read 0 :entries-limit 0})
       #'tick/selector-seam (constantly {:selector identity
                                        :selector-seam "test"})
       #'tick/trace-stat (constantly nil)
       #'tick/write-receipt! (fn [path _] path)
       ;; RUN12: the entrypoint now takes data/wm-trace/.run-lock. Point it at a
       ;; temp file so the suite neither blocks on nor clobbers a live runner's.
       #'tick/lock-path (constantly
                         (str (System/getProperty "java.io.tmpdir")
                              "/futon2-run-tick-once-test-" (System/nanoTime)
                              ".run-lock"))
       #'wm/arena-mode-flags (constantly {})
       #'wm/http-get-json (constantly nil)
       #'wm/read-edn-file (constantly {:families [] :invariants []})
       #'wm/generate-war-machine
       (fn [_ opts]
         (reset! generated-opts opts)
         {:data {:mission-detail
                 (wm/scan-mission-detail
                  [{:mission/id "M-test"}]
                  (:step-mission-detail-portfolio? opts))}
          :judgement
          {:input-status {:inputs-read 0 :issues []}
           :wm/route []
           :invariants
           (wm/load-invariant-inventory
            (:eval-invariant-fallback? opts))}})}
      (fn []
        (let [run (tick/run-tick-once 14)]
          (is (empty? @posts)
              "the complete diagnostic entrypoint must issue no HTTP POST")
          (is (= {:status :absent
                  :reason :invariant-eval-fallback-suppressed}
                 (get-in run [:result :invariants :live-status])))
          (is (false? (:step-mission-detail-portfolio? @generated-opts)))
          (is (false? (:eval-invariant-fallback? @generated-opts)))
          ;; RUN11: on the real entrypoint path, the id minted for this tick
          ;; reaches the judge opts AND the receipt as one value. That is what
          ;; puts :run/id on the trace record the judge writes.
          (is (string? (:run-id @generated-opts)))
          (is (= (get-in run [:tick-run-record :run/id])
                 (:run-id @generated-opts))
              "receipt and judge opts name the same run"))))))

;; ---------------------------------------------------------------------------
;; U55 -- the stepper's sandbox seam. `sandbox` is pure in its getenv function,
;; so these assert the seam itself rather than a process environment.
;; ---------------------------------------------------------------------------

(deftest sandbox-unset-is-the-live-path-test
  (testing "no override ⇒ not sandboxed, live directories, and NO :trace-dir in the judge opts"
    (let [s (tick/sandbox (constantly nil))]
      (is (false? (:sandboxed? s)))
      (is (nil? (:trace-dir-override s)))
      (is (.endsWith (:trace-dir s) "/code/futon2/data/wm-trace"))
      (is (.endsWith (:receipt-dir s) "/code/futon2/holes/labs/wm-contract"))
      (is (not (contains? (#'tick/diagnostic-judge-opts :selector {:version :test}
                                                        "run-1" (:trace-dir-override s))
                          :trace-dir))
          "an unredirected tick passes no :trace-dir, so war-machine keeps its own default"))))

(deftest sandbox-empty-string-is-not-an-override-test
  (testing "an exported-but-empty variable is the live path, not a write into \"\""
    (let [s (tick/sandbox {"FUTON_WM_TRACE_DIR" "" "FUTON_WM_RECEIPT_DIR" ""})]
      (is (false? (:sandboxed? s)))
      (is (nil? (:trace-dir-override s))))))

(deftest sandbox-redirects-trace-and-receipt-test
  (let [s (tick/sandbox {"FUTON_WM_TRACE_DIR" "/tmp/step/wm-trace"
                         "FUTON_WM_RECEIPT_DIR" "/tmp/step/receipts"})]
    (is (true? (:sandboxed? s)))
    (is (= "/tmp/step/wm-trace" (:trace-dir-override s)))
    (is (= "/tmp/step/wm-trace/wm-trace-2026-09-04.edn"
           (#'tick/trace-path-for-date (:trace-dir s) "2026-09-04")))
    (is (= "/tmp/step/receipts/tick-run-record-2026-09-04-run-7.edn"
           (#'tick/receipt-path-for-run (:receipt-dir s) "2026-09-04" "run-7")))
    (is (= "/tmp/step/wm-trace"
           (:trace-dir (#'tick/diagnostic-judge-opts :selector {:version :test}
                                                     "run-7" (:trace-dir-override s))))
        "the redirect reaches the judge, which is what carries it to the habit-prior fold")))

(deftest sandbox-trace-dir-alone-still-sandboxes-test
  (testing "redirecting only the trace still marks the tick sandboxed -- a receipt beside the live ones would otherwise look live"
    (let [s (tick/sandbox {"FUTON_WM_TRACE_DIR" "/tmp/step/wm-trace"})]
      (is (true? (:sandboxed? s)))
      (is (.endsWith (:receipt-dir s) "/code/futon2/holes/labs/wm-contract")))))
