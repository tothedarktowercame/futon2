(ns futon2.aif.provider-fail-class-test
  "PROVIDER-FAIL-3: provider-failure-class reads a job-like map and returns a
  typed class with the exact evidence substring. Every case below is a real
  envelope observed 2026-11-07 (or the capacity/timeout spellings named in the
  ruling), passed through as strings -- no stubs, the classifier runs for real."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.flight-runner :as flight-runner]))

(def usage-429
  "The exact Zai 429 body from 2026-11-07: HTTP 429 with error code 1308."
  {:state "failed"
   :terminal-code "error"
   :terminal-message "{\"message\":\"HTTP 429\",\"body\":{\"error\":{\"code\":\"1308\",\"message\":\"Usage limit reached for 5 hour. Your limit will reset at 2026-11-07 17:26:31\"}}}"
   :events []})

(def provider-500
  "The exact 500 body from 2026-11-07: HTTP 500, code 1234, internal network
  failure."
  {:state "failed"
   :terminal-code "error"
   :terminal-message "{\"message\":\"HTTP 500\",\"body\":{\"error\":{\"code\":\"1234\",\"message\":\"Internal network failure, error id: 20261107173257360c695ff77141f5, please try again later.\"}}}"
   :events []})

(deftest usage-limit-from-the-real-429-envelope
  (let [r (runner/provider-failure-class usage-429)]
    (is (= :usage-limit (:class r)))
    (is (= 429 (:http-status r)))
    (is (= "1308" (:provider-code r)))
    (is (= "HTTP 429" (:evidence r)))))

(deftest answer-fn-carries-terminal-code-and-message-on-a-non-done-state
  ;; The seam PROVIDER-FAIL-3 widens: a failed job's terminal-code/message
  ;; ride along on the answer fn's return (absent keys stay absent).
  (let [job {:state "failed" :job-id "j"
             :terminal-code "error"
             :terminal-message "{\"message\":\"HTTP 429\"}"
             :result nil}
        answer ((flight-runner/agency-answer-fn
                 {:seat "kimi-6" :opts {}
                  :dispatch! (fn [& _] {:job-id "j"})
                  :poll! (fn [& _] job)})
                {:target "t" :kind :interpretation :request-id "r1"})]
    (is (= "failed" (:state answer)))
    (is (= "error" (:terminal-code answer)))
    (is (= "{\"message\":\"HTTP 429\"}" (:terminal-message answer))))
  (let [answer ((flight-runner/agency-answer-fn
                 {:seat "kimi-6" :opts {}
                  :dispatch! (fn [& _] {:job-id "j2"})
                  :poll! (fn [& _] {:state "done" :job-id "j2" :result "ok"
                                    :terminal-code "irrelevant"})})
                {:target "t" :kind :interpretation :request-id "r2"})]
    (is (nil? (:terminal-code answer)) "a done job's terminal fields do not ride along")
    (is (nil? (:terminal-message answer)))))

(deftest provider-error-from-the-real-500-envelope
  (let [r (runner/provider-failure-class provider-500)]
    (is (= :provider-error (:class r)))
    (is (= 500 (:http-status r)))
    (is (= "1234" (:provider-code r)))
    (is (re-find #"Internal network failure" (:evidence r)))))

(deftest capacity-text-is-model-overloaded
  ;; The same spelling author-infrastructure-failure? retries once.
  (let [r (runner/provider-failure-class
           {:state "failed" :terminal-code "error"
            :terminal-message "The selected model is at capacity" :events []})]
    (is (= :model-overloaded (:class r)))
    (is (= "selected model is at capacity" (:evidence r)))))

(deftest http-529-is-model-overloaded
  (let [r (runner/provider-failure-class
           {:state "failed" :terminal-message "HTTP 529" :events []})]
    (is (= :model-overloaded (:class r)))
    (is (= 529 (:http-status r)))
    (is (= "HTTP 529" (:evidence r)))))

(deftest timeout-is-its-own-class
  ;; java.net.http.HttpTimeoutException "request timed out" seen on kimi jobs
  ;; 2026-11-07. Own class because the provider never answered at all.
  (let [r (runner/provider-failure-class
           {:state "timed-out"
            :terminal-code "invoke-error"
            :terminal-message
            "java.net.http.HttpTimeoutException: request timed out"
            :events []})]
    (is (= :timeout (:class r)))
    (is (contains? #{"HttpTimeoutException" "request timed out"} (:evidence r)))))

(deftest done-is-never-a-provider-failure
  (let [r (runner/provider-failure-class
           {:state "done" :terminal-message "HTTP 429" :events []})]
    (is (= :not-a-provider-failure (:class r)))
    (is (nil? (:evidence r)))))

(deftest cancelled-with-no-provider-signal-is-not-a-provider-failure
  (let [r (runner/provider-failure-class {:state "cancelled" :events []})]
    (is (= :not-a-provider-failure (:class r)))
    (is (nil? (:evidence r)))))

(deftest unparseable-429-body-still-classifies-by-string-match
  ;; A 429 whose JSON envelope is malformed: no code is extractable, but the
  ;; `HTTP 429' string alone must still say :usage-limit.
  (let [r (runner/provider-failure-class
           {:state "failed"
            :terminal-message "{\"message\":\"HTTP 429\",\"body\":{\"err" :events []})]
    (is (= :usage-limit (:class r)))
    (is (= 429 (:http-status r)))
    (is (nil? (:provider-code r)))
    (is (= "HTTP 429" (:evidence r)))))
