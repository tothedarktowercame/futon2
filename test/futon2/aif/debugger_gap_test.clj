(ns futon2.aif.debugger-gap-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.tripwire :as tripwire]
            [futon2.aif.wm.debugger :as debugger]
            [futon2.test-support.runner-fixture :as fixture]))

(use-fixtures :once hermetic/with-hermetic-stores fixture/with-hermetic-traces)

(use-fixtures
  :each
  (fn [test-fn]
    (debugger/detach!)
    (try
      (binding [runner/*wm-status-reporting?* false]
        (with-redefs [tripwire/observe! (fn [& _])]
          (test-fn)))
      (finally
        (debugger/detach!)))))

(defn- wait-for-stop [run-id running]
  (loop [remaining 4000]
    (if-let [stop (first (filter #(= run-id (:run-id %))
                                 (debugger/stopped)))]
      stop
      (if (pos? remaining)
        (do (Thread/sleep 5) (recur (dec remaining)))
        (throw (ex-info "Debugger stop did not appear"
                        {:run-id run-id
                         :run-realized? (realized? running)}))))))

(defn- authoring-result [published?]
  {:record {:kind (when-not published? :occurrence-square-answer-invalid)
            :published published?}})

(deftest unpublished-square-authoring-stops-and-retry-reruns-phase
  (debugger/attach!)
  (let [run-id "debugger-gap-square-authoring"
        calls (atom 0)
        published? (atom false)
        running
        (future
          (runner/run-phase!
           {:run-id run-id}
           {:opportunity-id "op-square"}
           :occurrence-square-authoring
           #(do (swap! calls inc) (authoring-result @published?))
           nil
           runner/occurrence-square-authoring-terminal-condition))
        stop (wait-for-stop run-id running)]
    (is (= run-id (:run-id stop)))
    (is (= :occurrence-square-authoring (:phase stop)))
    (is (= :occurrence-square-answer-invalid
           (get-in stop [:condition :kind])))
    (reset! published? true)
    (debugger/continue! run-id :retry)
    (is (= true (get-in (deref running 5000 ::timeout) [:record :published])))
    (is (= 2 @calls))))

(deftest detached-square-authoring-keeps-existing-terminal-result
  (let [result (authoring-result false)
        returned
        (runner/run-phase!
         {:run-id "detached-square-authoring"}
         {:opportunity-id "op-square"}
         :occurrence-square-authoring
         (constantly result)
         nil
         runner/occurrence-square-authoring-terminal-condition)]
    (is (= result returned))
    (is (empty? (debugger/stopped)))))

(defn- exercise-attached-handoff-wait [phase failed-value expected-kind]
  (debugger/attach!)
  (let [run-id (str "debugger-gap-" (name phase))
        calls (atom 0)
        repaired? (atom false)
        phase-events (atom [])
        running
        (future
          (runner/run-phase!
           {:run-id run-id
            :phase-log-fn (fn [_])
            :phase-events phase-events}
           {:opportunity-id "op-handoff"}
           phase
           #(do (swap! calls inc)
                (if @repaired?
                  {:job-id "job-1" :state "done"}
                  failed-value))
           nil
           #(runner/handoff-wait-terminal-condition phase %)))
        stop (wait-for-stop run-id running)]
    (is (= phase (:phase stop)))
    (is (= expected-kind (get-in stop [:condition :kind])))
    (reset! repaired? true)
    (debugger/continue! run-id :retry)
    (is (= "done" (:state (deref running 5000 ::timeout))))
    (is (= 2 @calls))))

(defn- exercise-detached-handoff-wait [phase failed-value]
  (let [result failed-value
        phase-events (atom [])
        returned
        (runner/run-phase!
         {:run-id (str "detached-" (name phase))
          :phase-log-fn (fn [_])
          :phase-events phase-events}
         {:opportunity-id "op-handoff"}
         phase
         (constantly result)
         nil
         #(runner/handoff-wait-terminal-condition phase %))]
    (is (= result returned))
    (is (empty? (debugger/stopped)))))

(deftest attached-author-wait-stops-and-retries
  (exercise-attached-handoff-wait :author-wait "not a job map" :untyped-failure))

(deftest detached-author-wait-is-unchanged
  (exercise-detached-handoff-wait :author-wait "not a job map"))

(deftest attached-author-retry-wait-stops-and-retries
  (exercise-attached-handoff-wait :author-retry-wait nil :untyped-failure))

(deftest detached-author-retry-wait-is-unchanged
  (exercise-detached-handoff-wait :author-retry-wait nil))

(deftest attached-reviewer-wait-stops-and-retries
  (exercise-attached-handoff-wait
   :reviewer-wait
   {:job-id "job-1" :state "done" :failure-kind :artifact-binding-mismatch}
   :artifact-binding-mismatch))

(deftest detached-reviewer-wait-is-unchanged
  (exercise-detached-handoff-wait
   :reviewer-wait
   {:job-id "job-1" :state "done" :failure-kind :artifact-binding-mismatch}))

(deftest attached-reviewer-retry-wait-stops-and-retries
  (exercise-attached-handoff-wait
   :reviewer-retry-wait {:job-id "job-1" :state "timed-out"} :untyped-failure))

(deftest detached-reviewer-retry-wait-is-unchanged
  (exercise-detached-handoff-wait
   :reviewer-retry-wait {:job-id "job-1" :state "timed-out"}))

(defn- exercise-named-condition-attached
  ([phase condition-fn failed-value expected-kind]
   (exercise-named-condition-attached
    phase condition-fn failed-value expected-kind {:job-id "job-1" :state "done"}))
  ([phase condition-fn failed-value expected-kind repaired-value]
  (debugger/attach!)
  (let [run-id (str "debugger-gap-3-" (name phase))
        calls (atom 0)
        repaired? (atom false)
        events (atom [])
        running (future
                  (runner/run-phase!
                   {:run-id run-id :phase-log-fn (fn [_]) :phase-events events}
                   {:opportunity-id "op-gap-3"} phase
                   #(do (swap! calls inc)
                        (if @repaired? repaired-value
                            failed-value))
                   nil condition-fn))
        stop (wait-for-stop run-id running)]
    (is (= phase (:phase stop)))
    (is (= expected-kind (get-in stop [:condition :kind])))
    (reset! repaired? true)
    (debugger/continue! run-id :retry)
    (is (= repaired-value (deref running 5000 ::timeout)))
    (is (= 2 @calls)))))

(defn- exercise-named-condition-detached [phase condition-fn failed-value]
  (let [events (atom [])
        returned (runner/run-phase!
                  {:run-id (str "detached-gap-3-" (name phase))
                   :phase-log-fn (fn [_]) :phase-events events}
                  {:opportunity-id "op-gap-3"} phase
                  (constantly failed-value) nil condition-fn)]
    (is (= failed-value returned))
    (is (empty? (debugger/stopped)))))

(def gap-3-waits
  [{:phase :author-response-correction-wait
    :condition runner/author-response-correction-wait-terminal-condition
    :kind :author-response-correction-refused}
   {:phase :unmeasured-claim-correction-wait
    :condition runner/unmeasured-claim-correction-wait-terminal-condition
    :kind :unmeasured-claim-correction-refused}
   {:phase :review-evidence-correction-wait
    :condition runner/review-evidence-correction-wait-terminal-condition
    :kind :review-evidence-correction-refused}
   {:phase :revision-wait :condition runner/revision-wait-terminal-condition
    :kind :untyped-failure}
   {:phase :re-review-retry-wait
    :condition runner/re-review-retry-wait-terminal-condition
    :kind :untyped-failure}
   {:phase :build-cure-wait :condition runner/build-cure-wait-terminal-condition
    :kind :untyped-failure}])

(doseq [{:keys [phase condition kind]} gap-3-waits]
  (eval
   `(deftest ~(symbol (str "attached-" (name phase) "-stops-and-retries"))
      (exercise-named-condition-attached
       ~phase ~condition {:job-id "job-1" :state "timed-out"} ~kind)))
  (eval
   `(deftest ~(symbol (str "detached-" (name phase) "-is-unchanged"))
      (exercise-named-condition-detached
       ~phase ~condition {:job-id "job-1" :state "timed-out"}))))

(deftest re-review-first-wait-auto-retry-does-not-stop
  (debugger/attach!)
  (is (nil? (runner/re-review-wait-terminal-condition
             {:job-id "job-1" :state "failed" :terminal-code "invoke-error"})))
  (is (empty? (debugger/stopped))))

(deftest attached-claim-reviewer-wait-stops-and-retries
  (exercise-named-condition-attached
   :reviewer-wait #(runner/claim-reviewer-wait-terminal-condition "job-1" %)
   {:job-id "wrong-job" :state "done"} :review-job-binding-mismatch))

(deftest detached-claim-reviewer-wait-is-unchanged
  (exercise-named-condition-detached
   :reviewer-wait #(runner/claim-reviewer-wait-terminal-condition "job-1" %)
   {:job-id "wrong-job" :state "done"}))

(deftest attached-build-resolution-stops-and-retries
  (exercise-named-condition-attached
   :build-resolution #(runner/build-resolution-terminal-condition {:repo "/expected"} %)
   {:repo "/wrong" :files []} :artifact-binding-mismatch
   {:repo "/expected" :files []}))

(deftest detached-build-resolution-is-unchanged
  (exercise-named-condition-detached
   :build-resolution #(runner/build-resolution-terminal-condition {:repo "/expected"} %)
   {:repo "/wrong" :files []}))

(def ^:private invocation-failure
  {:job-id "job-1" :state "failed" :terminal-code "invoke-error"})

(deftest automatically-retried-invocation-failures-do-not-stop
  (debugger/attach!)
  (is (true? (runner/author-infrastructure-failure? invocation-failure)))
  (is (nil? (runner/author-wait-terminal-condition
             {:author-infrastructure-retries 1} invocation-failure))
      "the author wait leaves a retried invocation failure to the runner")
  (is (nil? (runner/reviewer-wait-terminal-condition invocation-failure))
      "the reviewer wait always retries an invocation failure once")
  (is (= :untyped-failure
         (:failure-kind
          (ex-data (runner/author-wait-terminal-condition
                    {:author-infrastructure-retries 0} invocation-failure))))
      "with no retry budget the same failure stops")
  (is (= :artifact-binding-mismatch
         (:failure-kind
          (ex-data (runner/reviewer-wait-terminal-condition
                    {:job-id "job-1" :state "done"
                     :failure-kind :artifact-binding-mismatch}))))))
