(ns futon2.aif.terminal-kind-holds-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.wm.debugger :as debugger]))

(use-fixtures
  :each
  (fn [test-fn]
    (debugger/detach!)
    (try (test-fn)
         (finally (debugger/detach!)))))

(defn- wait-for-stop [run-id running]
  (loop [remaining 100]
    (if-let [stop (first (filter #(= run-id (:run-id %)) (debugger/stopped)))]
      stop
      (if (and (pos? remaining) (not (realized? running)))
        (do (Thread/sleep 5) (recur (dec remaining)))
        nil))))

(defn- attached-stop [run-id phase value condition-fn]
  (debugger/attach!)
  (let [running (future
                  (runner/run-phase!
                   {:run-id run-id :phase-log-fn (fn [_])}
                   {:opportunity-id "stop-holds-1"}
                   phase (constantly value) nil condition-fn))
        stop (wait-for-stop run-id running)]
    (when stop
      (debugger/continue! run-id :abort)
      (try (deref running 1000 ::timeout)
           (catch java.util.concurrent.ExecutionException _)))
    stop))

(deftest attached-resume-protocol-violation-holds
  (let [stop (attached-stop
              "repairable-resume" :cascade-resume
              {:status :error :kind :resume-protocol-violation}
              nil)]
    (is (= :repairable-terminal-kind (get-in stop [:condition :kind])))
    (is (= :resume-protocol-violation (get-in stop [:ex-data :original-kind])))))

(deftest attached-close-exception-holds
  (let [stop (attached-stop
              "repairable-close" :close
              {:failure-kind :close-exception :error "close failed"}
              nil)]
    (is (= :repairable-terminal-kind (get-in stop [:condition :kind])))
    (is (= :close-exception (get-in stop [:ex-data :original-kind])))))

(deftest attached-self-retried-infrastructure-result-does-not-hold
  (debugger/attach!)
  (let [value {:job-id "job-1" :state "failed" :terminal-code "invoke-error"
               :failure-kind :agent-unavailable}]
    (is (= value
           (runner/run-phase!
            {:run-id "self-retried" :phase-log-fn (fn [_])}
            {:opportunity-id "stop-holds-1"}
            :reviewer-wait (constantly value) nil
            runner/reviewer-wait-terminal-condition)))
    (is (empty? (debugger/stopped)))))

(deftest attached-aif-valid-and-recorded-defect-do-not-hold
  (debugger/attach!)
  (doseq [value [{:status :abstained :case :empty-field}
                 {:failure-kind :guardrail-refusal}]]
    (is (= value
           (runner/run-phase!
            {:run-id "non-repairable" :phase-log-fn (fn [_])}
            {:opportunity-id "stop-holds-1"}
            :selection (constantly value)))))
  (is (empty? (debugger/stopped))))

(deftest detached-repairable-result-is-unchanged
  (let [value {:status :error :kind :resume-protocol-violation}]
    (is (= value
           (runner/run-phase!
            {:run-id "detached-repairable" :phase-log-fn (fn [_])}
            {:opportunity-id "stop-holds-1"}
            :cascade-resume (constantly value))))
    (is (empty? (debugger/stopped)))))
