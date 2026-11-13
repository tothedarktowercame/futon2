(ns futon2.aif.readiness-wake-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]))

(def restored
  {:wm-author {:status "restored" :invoke-ready? true}})

(def invoking
  {:wm-author {:status "invoking" :invoke-ready? true}})

(def idle
  {:wm-author {:status "idle" :invoke-ready? true}})

(deftest fresh-session-wake-retains-its-receipt
  (let [rosters (atom [idle])
        result (#'runner/wake-restored-agent!
                {:wake-agent-fn (fn [_ _] {:ok true :job-id "wake-job-1"})
                 :roster-fn (fn [_]
                              (let [next-roster (first @rosters)]
                                (swap! rosters rest)
                                next-roster))}
                restored "wm-author")]
    (is (= :woken (:readiness/wake-result result)))
    (is (= {:ok true :job-id "wake-job-1"}
           (:readiness/wake-receipt result)))
    (is (= idle (:roster result)))))

(deftest persistently-invoking-wake-is-bounded
  (let [sleeps (atom [])
        result (#'runner/wake-restored-agent!
                {:wake-agent-fn (fn [_ _] {:ok true :job-id "wake-job-2"})
                 :roster-fn (constantly invoking)
                 :readiness-wake-sleep-fn #(swap! sleeps conj %)}
                restored "wm-author")]
    (is (= :no-reply (:readiness/wake-result result)))
    (is (= (repeat runner/readiness-wake-poll-attempts
                   runner/readiness-wake-poll-delay-ms)
           @sleeps))
    (is (= "wake-job-2" (get-in result [:readiness/wake-receipt :job-id])))))

(deftest wake-operation-has-a-three-minute-safety-bound
  (is (= (* 3 60 1000) runner/readiness-wake-timeout-ms)))
