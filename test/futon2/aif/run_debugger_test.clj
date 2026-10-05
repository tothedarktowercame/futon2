(ns futon2.aif.run-debugger-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.full-loop-runtime]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.registered-run-telemetry :as telemetry]
            [futon2.aif.tripwire :as tripwire]
            [futon2.aif.wm.debugger :as debugger]
            [futon2.test-support.runner-fixture :as fixture]))

(use-fixtures :once hermetic/with-hermetic-stores fixture/with-hermetic-traces)

(defn repairable-callee []
  (throw (ex-info "selection broke" {:kind :test/selection-broke :detail :bad-wire})))

(defn repairable-judge [_]
  (throw (ex-info "selection broke" {:kind :test/selection-broke :detail :bad-wire})))

(def repaired-judgement
  {:decision {:status :abstained :refusals []}
   :belief {} :belief-pre {} :observation {} :free-energy {}
   :prediction-errors {} :precision-state {} :micro-step-trace [] :mode :maintain})

(def recorded-terminal-abstention
  {:decision
   {:status :abstained
    :refusals [{:target "E-kimi-task-10"
                :kind :universe-not-admitted
                :missing :universes}]}})

(def selected-judgement
  {:decision {:status :selected
              :selection-law {:applied :cascade-selection-posterior}
              :action {:id :C1 :target "M-self-documenting-stack"}}})

(defn- terminal-selection-condition [judgement]
  (runner/selection-terminal-condition :selection judgement))

(defn- wait-for-stop
  ([run-id] (wait-for-stop run-id nil))
  ([run-id running]
  ;; The real selection path reads the complete cascade field before reaching
  ;; its debugger boundary; its pinned graph walk can exceed twenty seconds.
   (loop [remaining 12000]
     (if-let [stop (first (filter #(= run-id (:run-id %)) (debugger/stopped)))]
       stop
       (if (pos? remaining)
         (do (Thread/sleep 5) (recur (dec remaining)))
         (throw (ex-info "Debugger stop did not appear"
                         (cond-> {:run-id run-id}
                           (and running (realized? running))
                           (assoc :run-result @running)))))))))

(defn- wait-for-condition [run-id kind]
  (loop [remaining 4000]
    (let [stop (first (filter #(= run-id (:run-id %)) (debugger/stopped)))]
      (if (= kind (get-in stop [:condition :kind]))
        stop
        (if (pos? remaining)
          (do (Thread/sleep 5) (recur (dec remaining)))
          (throw (ex-info "Debugger condition did not appear"
                          {:run-id run-id :kind kind :last-stop stop})))))))

(defn- phase-opts [run-id events]
  {:run-id run-id :phase-log-fn #(swap! events conj %) :phase-events events})

(defn- miniature-run
  "The H1 mechanism boundary: fixed run/attempt/opportunity identity, phases,
  one terminal receipt, and today's failure projection on abort."
  [run-id calls events]
  (let [opts (phase-opts run-id events)
        context {:opportunity-id "op-1" :attempt-id "attempt-1" :trigger :test}]
    (try
      (runner/run-phase! opts context :agent-readiness
                         #(do (swap! calls update :agent-readiness (fnil inc 0)) :ready))
      (let [selection (runner/run-phase!
                       opts context :selection
                       #(do (swap! calls update :selection (fnil inc 0))
                            (repairable-callee)))]
        (swap! calls update :terminal-receipt (fnil inc 0))
        {:run-id run-id :opportunity-id "op-1" :attempt-id "attempt-1"
         :outcome :complete :selection selection :terminal-receipts 1})
      (catch Throwable e
        {:run-id run-id :opportunity-id "op-1" :attempt-id "attempt-1"
         :outcome :incomplete
         :failure {:kind (or (:kind (ex-data e)) :untyped-failure)
                   :stage :selection :error (.getMessage e)
                   :data (ex-data e)}}))))

(use-fixtures
  :each
  (fn [f]
    (debugger/detach!)
    (try
      (binding [runner/*wm-status-reporting?* false]
        (with-redefs [tripwire/observe! (fn [& _])]
          (f)))
      (finally (debugger/detach!)))))

(deftest armed-after-phase-continues-with-the-computed-value
  (debugger/attach!)
  (debugger/arm-breakpoint! [:after :phase-x])
  (let [calls (atom 0)
        result {:answer 42 :detail (apply str (repeat 1000 "large"))}
        running (future
                  (runner/run-phase!
                   (phase-opts "after-continue" (atom []))
                   {:opportunity-id "op" :attempt-id "attempt"}
                   :phase-x #(do (swap! calls inc) result)))]
    (try
      (let [stop (wait-for-condition "after-continue" :wm/phase-breakpoint)]
        (is (= :phase-x (:phase stop)))
        (is (= {:top-level-keys [:answer :detail]} (:result-summary stop)))
        (is (nil? (get-in stop [:ex-data :debugger/result])))
        (is (= result (debugger/stop-value "after-continue")))
        (debugger/continue! "after-continue" :continue)
        (is (= result (deref running 2000 ::timeout)))
        (is (= 1 @calls)))
      (finally
        (debugger/clear-breakpoint! [:after :phase-x])))))

(deftest after-phase-retry-reruns-and-use-value-overrides
  (debugger/attach!)
  (debugger/arm-breakpoint! [:after :phase-x])
  (let [calls (atom 0)
        running (future
                  (runner/run-phase!
                   (phase-opts "after-retry" (atom []))
                   {:opportunity-id "op" :attempt-id "attempt"}
                   :phase-x #(swap! calls inc)))]
    (try
      (wait-for-stop "after-retry")
      (debugger/clear-breakpoint! [:after :phase-x])
      (debugger/continue! "after-retry" :retry)
      (is (= 2 (deref running 2000 ::timeout)))
      (is (= 2 @calls))
      (debugger/arm-breakpoint! [:after :phase-x])
      (let [override (future
                       (runner/run-phase!
                        (phase-opts "after-use-value" (atom []))
                        {:opportunity-id "op" :attempt-id "attempt"}
                        :phase-x (constantly :computed)))]
        (wait-for-stop "after-use-value")
        (debugger/continue! "after-use-value" [:use-value :operator])
        (is (= :operator (deref override 2000 ::timeout))))
      (finally
        (debugger/clear-breakpoint! [:after :phase-x])))))

(deftest after-phase-guard-requires-attachment-and-exact-name
  (debugger/arm-breakpoint! [:after :phase-x])
  (try
    (is (= :detached
           (runner/run-phase!
            (phase-opts "after-detached" (atom [])) {} :phase-x
            (constantly :detached))))
    (debugger/attach!)
    (debugger/clear-breakpoint! [:after :phase-x])
    (debugger/arm-breakpoint! [:after :other-phase])
    (is (= :unarmed
           (runner/run-phase!
            (phase-opts "after-unarmed" (atom [])) {} :phase-x
            (constantly :unarmed))))
    (finally
      (debugger/clear-breakpoint! [:after :phase-x])
      (debugger/clear-breakpoint! [:after :other-phase]))))

(deftest continue-is-refused-for-a-failure-without-a-computed-value
  (debugger/attach!)
  (let [running (future
                  (try
                    (runner/run-phase!
                     (phase-opts "failure-continue" (atom [])) {}
                     :phase-x repairable-callee)
                    (catch Throwable e e)))]
    (wait-for-stop "failure-continue")
    (is (= :debugger/invalid-choice-for-stop
           (:kind (ex-data
                   (try
                     (debugger/continue! "failure-continue" :continue)
                     (catch clojure.lang.ExceptionInfo e e))))))
    (debugger/continue! "failure-continue" :abort)
    (is (instance? Throwable (deref running 2000 ::timeout)))))

(deftest after-phase-dwell-is-recorded-and-excluded-from-active-time
  (debugger/attach!)
  (debugger/arm-breakpoint! [:after :phase-x])
  (let [run-id "after-dwell" clock (atom 0) events (atom [])
        ledger (debugger/new-dwell-ledger run-id)
        opts (assoc (dissoc (phase-opts run-id events) :phase-log-fn)
                    :nano-time-fn #(long @clock)
                    :debugger-dwell/state ledger)
        running (future (runner/run-phase! opts {} :phase-x (constantly :ok)))]
    (try
      (wait-for-stop run-id)
      (reset! clock 5000000000)
      (debugger/continue! run-id :continue)
      (is (= :ok (deref running 2000 ::timeout)))
      (is (= 5000 (get-in @ledger [:receipts 0 :duration-ms])))
      (is (= 0 (get-in (telemetry/phase-timings @events ledger run-id)
                       [:phase-timings-ms :phase-x])))
      (finally
        (debugger/clear-breakpoint! [:after :phase-x])))))

(deftest retry-repairs-only-the-failed-phase-on-the-same-run-thread
  ;; Plant: restarting miniature-run here would increment :agent-readiness to
  ;; two. The assertion fixes the restart boundary at run-phase!.
  (let [original @#'repairable-callee
        calls (atom {}) events (atom []) run-id "debug-retry"
        _ (debugger/attach!)
        running (future (miniature-run run-id calls events))]
    (try
      (let [stop (wait-for-stop run-id)]
        (is (= {:run-id run-id :attempt-id "attempt-1" :phase :selection}
               (select-keys stop [:run-id :attempt-id :phase])))
        (is (= :test/selection-broke (get-in stop [:condition :kind])))
        (is (= {:kind :test/selection-broke :detail :bad-wire} (:ex-data stop)))
        ;; This is the live-reload analogue: the retry calls the Var again.
        (alter-var-root #'repairable-callee (constantly (fn [] :selected)))
        (debugger/continue! run-id :retry)
        (let [result (deref running 2000 ::timeout)]
          (is (not= ::timeout result))
          (is (= :complete (:outcome result)))
          (is (= :selected (:selection result)))
          (is (= "attempt-1" (:attempt-id result)))
          (is (= 1 (:terminal-receipts result)))
          (is (= {:agent-readiness 1 :selection 2 :terminal-receipt 1} @calls))))
      (finally
        (alter-var-root #'repairable-callee (constantly original))))))

(deftest retry-continues-the-real-run-with-one-attempt-and-one-terminal-receipt
  (let [original @#'repairable-judge
        calls (atom {:roster 0 :code-state 0 :judge 0})
        run-id "debug-real-run"
        base (fixture/isolated-runner-opts)
        opts (assoc base
                    :run-id run-id
                    :roster-fn (fn [agency-base]
                                 (swap! calls update :roster inc)
                                 ((:roster-fn base) agency-base))
                    :code-state-fn (fn []
                                     (swap! calls update :code-state inc)
                                     ((:code-state-fn base)))
                    :judge-fn (fn [days]
                                (swap! calls update :judge inc)
                                (repairable-judge days)))
        _ (debugger/attach!)
        running (future (runner/run-opportunity! opts))]
    (try
      (let [stop (wait-for-stop run-id running)]
        (is (= :selection (:phase stop)))
        (is (string? (:attempt-id stop)))
        (alter-var-root #'repairable-judge
                        (constantly (fn [_] {:judgement repaired-judgement})))
        (debugger/continue! run-id :retry)
        (let [abstention-stop
              (wait-for-condition run-id :wm/selection-terminal-abstention)
              _ (is (= :abstained (get-in abstention-stop [:ex-data :outcome])))
              _ (debugger/continue! run-id :abort)
              result (deref running 5000 ::timeout)
              record (when-not (= ::timeout result)
                       (edn/read-string (slurp (:run-record result))))]
          (is (not= ::timeout result))
          (is (= :abstained (:outcome result)))
          (is (= (:attempt-id stop) (:attempt-id result)))
          (is (= :failure (get-in record [:terminal-receipt :kind])))
          (is (string? (:terminal-receipt-digest record))
              "the completed run record has exactly one attached terminal receipt")
          (is (= {:roster 1 :code-state 1 :judge 2} @calls)
              "retry repeats selection only, never the earlier real phases")))
      (finally
        (alter-var-root #'repairable-judge (constantly original))))))

(deftest abort-preserves-the-detached-failure
  (let [original @#'repairable-callee
        detached (miniature-run "detached" (atom {}) (atom []))
        _ (debugger/attach!)
        running (future (miniature-run "attached" (atom {}) (atom [])))]
    (try
      (wait-for-stop "attached")
      (debugger/continue! "attached" :abort)
      (let [attached (deref running 2000 ::timeout)]
        (is (= (:failure detached) (:failure attached))))
      (finally
        (alter-var-root #'repairable-callee (constantly original))))))

(deftest use-value-continues-the-phase
  (debugger/attach!)
  (let [events (atom [])
        running (future
                  (runner/run-phase!
                   (phase-opts "use-value" events)
                   {:opportunity-id "op" :attempt-id "attempt"}
                   :selection repairable-callee))]
    (wait-for-stop "use-value")
    (debugger/continue! "use-value" [:use-value {:selected :operator-value}])
    (is (= {:selected :operator-value} (deref running 2000 ::timeout)))
    (is (= :use-value (:debugger/restart (last @events))))))

(deftest use-value-retains-monotonic-dwell-without-retaining-the-value
  (debugger/attach!)
  (let [run-id "use-value-dwell"
        clock (atom 0)
        events (atom [])
        ledger (debugger/new-dwell-ledger run-id)
        opts (assoc (dissoc (phase-opts run-id events) :phase-log-fn)
                    :nano-time-fn #(long @clock)
                    :debugger-dwell/state ledger)
        secret {:large-value (apply str (repeat 10000 "do-not-retain"))}
        running (future
                  (runner/run-phase! opts
                                     {:opportunity-id "op" :attempt-id "attempt"}
                                     :selection repairable-callee))]
    (wait-for-stop run-id)
    (reset! clock (* 24 60 1000000000))
    (debugger/continue! run-id [:use-value secret])
    (is (= secret (deref running 2000 ::timeout)))
    (let [receipt (first (:receipts @ledger))
          timing (telemetry/phase-timings @events ledger run-id)]
      (is (= {:schema :wm/debugger-dwell-v1
              :run-id run-id :phase :selection
              :condition-kind :test/selection-broke
              :stopped-at-monotonic-ns 0
              :resumed-at-monotonic-ns (* 24 60 1000000000)
              :restart-choice :use-value
              :duration-ms (* 24 60 1000)}
             receipt))
      (is (= 0 (get-in timing [:phase-timings-ms :selection])))
      (is (not (.contains (pr-str receipt) "do-not-retain"))))))

(deftest abort-retains-dwell-before-the-phase-closes
  (debugger/attach!)
  (let [run-id "abort-dwell"
        clock (atom 10)
        events (atom [])
        ledger (debugger/new-dwell-ledger run-id)
        running (future
                  (try
                    (runner/run-phase!
                     (assoc (dissoc (phase-opts run-id events) :phase-log-fn)
                            :nano-time-fn #(long @clock)
                            :debugger-dwell/state ledger)
                     {:opportunity-id "op" :attempt-id "attempt"}
                     :delivery-qa repairable-callee)
                    (catch Throwable e e)))]
    (wait-for-stop run-id)
    (reset! clock 100000010)
    (debugger/continue! run-id :abort)
    (is (instance? Throwable (deref running 2000 ::timeout)))
    (is (= :abort (get-in @ledger [:receipts 0 :restart-choice])))
    (is (= :end (:transition (last @events))))
    (is (= :abort (:debugger/restart (last @events))))))

(deftest typed-refusal-stops-when-debugger-is-attached
  (debugger/attach!)
  (let [refusal (ex-info "War Machine abstained: cascade decision refused"
                         {:outcome :abstained
                          :judge-refusal {:kind :class-unknown-no-scalar-g}})
        running (future
                  (try
                    (runner/run-phase!
                     (phase-opts "typed-refusal" (atom []))
                     {:opportunity-id "op" :attempt-id "attempt"}
                     :selection #(throw refusal))
                    (catch clojure.lang.ExceptionInfo e e)))
        stop (wait-for-stop "typed-refusal")]
    (is (= :selection (:phase stop)))
    (is (= :class-unknown-no-scalar-g
           (get-in stop [:condition :kind])))
    (debugger/continue! "typed-refusal" :abort)
    (is (identical? refusal (deref running 2000 ::timeout))
        "abort preserves the ordinary outer refusal handling")))

(deftest typed-refusal-retains-ordinary-behaviour-when-detached
  (let [refusal (ex-info "War Machine abstained: cascade decision refused"
                         {:outcome :abstained
                          :judge-refusal {:kind :class-unknown-no-scalar-g}})
        caught (try
                 (runner/run-phase!
                  (phase-opts "detached-refusal" (atom []))
                  {:opportunity-id "op" :attempt-id "attempt"}
                  :selection #(throw refusal))
                 (catch clojure.lang.ExceptionInfo e e))]
    (is (identical? refusal caught))
    (is (empty? (debugger/stopped)))))

(deftest recorded-terminal-abstention-stops-with-decision-context
  (debugger/attach!)
  (let [running
        (future
          (try
            (runner/run-phase!
             (phase-opts "recorded-abstention" (atom []))
             {:opportunity-id "op" :attempt-id "attempt"}
             :selection (constantly recorded-terminal-abstention)
             nil terminal-selection-condition)
            (catch clojure.lang.ExceptionInfo e e)))
        stop (wait-for-stop "recorded-abstention")]
    (is (= :wm/selection-terminal-abstention
           (get-in stop [:condition :kind])))
    (is (= {:failure-kind :abstained
            :failure-stage :selection
            :outcome :abstained
            :target "E-kimi-task-10"
            :targets ["E-kimi-task-10"]
            :refusals [{:target "E-kimi-task-10"
                        :kind :universe-not-admitted
                        :missing :universes}]
            :decision (:decision recorded-terminal-abstention)}
           (select-keys (:ex-data stop)
                        [:failure-kind :failure-stage :outcome :target
                         :targets :refusals :decision])))
    (debugger/continue! "recorded-abstention" :abort)
    (let [aborted (deref running 2000 ::timeout)]
      (is (instance? clojure.lang.ExceptionInfo aborted))
      (is (= :abstained (:outcome (ex-data aborted)))))))

(deftest recorded-terminal-abstention-is-unchanged-when-detached
  (let [events (atom [])
        result (runner/run-phase!
                (phase-opts "detached-recorded-abstention" events)
                {:opportunity-id "op" :attempt-id "attempt"}
                :selection (constantly recorded-terminal-abstention)
                nil terminal-selection-condition)]
    (is (identical? recorded-terminal-abstention result))
    (is (= :ok (:outcome (last @events))))
    (is (empty? (debugger/stopped)))))

(deftest armed-outer-selection-breakpoint-stops-and-continues-the-same-value
  (debugger/attach!)
  (debugger/arm-breakpoint! :outer-selection-complete)
  (let [judgement (assoc selected-judgement
                         :outer-task-selection
                         {:schema :wm/outer-task-selection-v1
                          :chosen {:id "M-self-documenting-stack"}})
        running (future
                  (runner/run-phase!
                   (phase-opts "outer-selection-stop" (atom []))
                   {:opportunity-id "op" :attempt-id "attempt"}
                   :selection (constantly judgement)
                   nil terminal-selection-condition))
        stop (wait-for-condition "outer-selection-stop"
                                 :wm/outer-selection-complete)]
    (is (= "M-self-documenting-stack" (get-in stop [:ex-data :selected])))
    (is (= judgement (get-in stop [:ex-data :judgement])))
    (debugger/clear-breakpoint! :outer-selection-complete)
    (debugger/continue! "outer-selection-stop"
                        [:use-value (get-in stop [:ex-data :judgement])])
    (is (= judgement (deref running 2000 ::timeout)))))

(deftest terminal-abstention-retry-reruns-only-the-same-selection-phase
  (debugger/attach!)
  (let [calls (atom 0)
        events (atom [])
        running
        (future
          (runner/run-phase!
           (phase-opts "abstention-retry" events)
           {:opportunity-id "op" :attempt-id "same-attempt"}
           :selection
           #(if (= 1 (swap! calls inc))
              recorded-terminal-abstention
              selected-judgement)
           nil terminal-selection-condition))
        stop (wait-for-stop "abstention-retry")]
    (is (= {:attempt-id "same-attempt" :phase :selection}
           (select-keys stop [:attempt-id :phase])))
    (debugger/continue! "abstention-retry" :retry)
    (is (= selected-judgement (deref running 2000 ::timeout)))
    (is (= 2 @calls))
    (is (= [[:selection :start] [:selection :end]
            [:selection :start] [:selection :end]]
           (mapv (juxt :phase :transition) (take-nth 2 @events))))))
