(ns futon2.aif.run-debugger-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
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

(defn- wait-for-stop [run-id]
  (loop [remaining 4000]
    (if-let [stop (first (filter #(= run-id (:run-id %)) (debugger/stopped)))]
      stop
      (if (pos? remaining)
        (do (Thread/sleep 5) (recur (dec remaining)))
        (throw (ex-info "Debugger stop did not appear" {:run-id run-id}))))))

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
      (let [stop (wait-for-stop run-id)]
        (is (= :selection (:phase stop)))
        (is (string? (:attempt-id stop)))
        (alter-var-root #'repairable-judge
                        (constantly (fn [_] {:judgement repaired-judgement})))
        (debugger/continue! run-id :retry)
        (let [result (deref running 5000 ::timeout)
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

(deftest handled-refusal-does-not-enter-the-debugger
  (debugger/attach!)
  (let [refusal (ex-info "War Machine abstained: cascade decision refused"
                         {:outcome :abstained
                          :judge-refusal {:kind :class-unknown-no-scalar-g}})
        caught (try
                 (runner/run-phase!
                  (phase-opts "handled" (atom []))
                  {:opportunity-id "op" :attempt-id "attempt"}
                  :selection #(throw refusal))
                 (catch clojure.lang.ExceptionInfo e e))]
    (is (identical? refusal caught)
        "the existing outer refusal handler receives the original exception")
    (is (empty? (debugger/stopped)))))
