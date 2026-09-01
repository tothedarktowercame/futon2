(ns run-tick-once-test
  (:require [babashka.http-client :as http]
            [clojure.test :refer [deftest is]]
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

(deftest diagnostic-entrypoint-suppresses-portfolio-step-test
  (let [opts (#'tick/diagnostic-judge-opts :selector {:version :test})]
    (is (false? (:step-portfolio? opts)))
    (is (false? (:step-mission-detail-portfolio? opts)))
    (is (false? (:eval-invariant-fallback? opts)))))

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
          (is (false? (:eval-invariant-fallback? @generated-opts))))))))
