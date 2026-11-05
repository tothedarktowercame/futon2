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

(defn- exercise-attached-handoff-wait [phase]
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
                  {:job-id "job-1" :state "timed-out"}))
           nil
           #(runner/handoff-wait-terminal-condition phase %)))
        stop (wait-for-stop run-id running)]
    (is (= phase (:phase stop)))
    (is (= :untyped-failure (get-in stop [:condition :kind])))
    (reset! repaired? true)
    (debugger/continue! run-id :retry)
    (is (= "done" (:state (deref running 5000 ::timeout))))
    (is (= 2 @calls))))

(defn- exercise-detached-handoff-wait [phase]
  (let [result {:job-id "job-1" :state "timed-out"}
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
  (exercise-attached-handoff-wait :author-wait))

(deftest detached-author-wait-is-unchanged
  (exercise-detached-handoff-wait :author-wait))

(deftest attached-author-retry-wait-stops-and-retries
  (exercise-attached-handoff-wait :author-retry-wait))

(deftest detached-author-retry-wait-is-unchanged
  (exercise-detached-handoff-wait :author-retry-wait))

(deftest attached-reviewer-wait-stops-and-retries
  (exercise-attached-handoff-wait :reviewer-wait))

(deftest detached-reviewer-wait-is-unchanged
  (exercise-detached-handoff-wait :reviewer-wait))

(deftest attached-reviewer-retry-wait-stops-and-retries
  (exercise-attached-handoff-wait :reviewer-retry-wait))

(deftest detached-reviewer-retry-wait-is-unchanged
  (exercise-detached-handoff-wait :reviewer-retry-wait))
