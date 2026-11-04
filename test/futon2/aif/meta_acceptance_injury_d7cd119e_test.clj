(ns futon2.aif.meta-acceptance-injury-d7cd119e-test
  "Out-of-band acceptance for ledger entry 156, injury/d7cd119e… (R31).

  The injury was opened when the operator aborted run 561c5ec3 at the armed
  [:after :selection-world] breakpoint: the abort rethrew the bare breakpoint
  signal, the close read it :untyped-failure and the terminal classifier
  opened an injury. Fix: futon2 aa3d6bc38 (codex-12, R31-b), conforming to
  DarkTower.WarMachine.DebuggerAbortVerdict (mathlib4 d98636c8d1). Both
  deftests fail by assertion, with no error, on a tree whose src/ predates
  the fix: the abort then returns the signal itself (no cause, no
  :failure-kind) and the classifier returns :injury."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.meta-terminal-injury :as terminal]
            [futon2.aif.wm.debugger :as debugger]))

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

(deftest injury-d7cd119e-retained-value-abort-closes-typed
  (let [run-id "r31-acceptance-abort"
        phase :phase-x
        value {:answer 42}]
    (debugger/attach!)
    (debugger/arm-breakpoint! [:after phase])
    (try
      (let [running (future
                      (try
                        (runner/run-phase!
                         (phase-opts run-id (atom []))
                         {:opportunity-id "op" :attempt-id "attempt"}
                         phase (constantly value))
                        (catch clojure.lang.ExceptionInfo e e)))]
        (wait-for-condition run-id :wm/phase-breakpoint)
        (debugger/continue! run-id :abort :nothing-to-fix)
        (let [aborted (deref running 2000 ::timeout)
              original (ex-cause aborted)]
          (is (instance? clojure.lang.ExceptionInfo aborted))
          (is (= {:failure-kind :debugger/aborted
                  :outcome :cancelled
                  :debugger/disposition :nothing-to-fix}
                 (select-keys (ex-data aborted)
                              [:failure-kind :outcome :debugger/disposition])))
          (is (= :wm/phase-breakpoint (:kind (ex-data original)))
              "the breakpoint signal rides as the cause")))
      (finally
        (debugger/clear-breakpoint! [:after phase])))))

(def ^:private pin
  {:path "fixtures/r31-acceptance.edn"
   :sha256 "d7cd119eb38f3e48b54211372e64f4e07d562f73000000000000000000000000"})

(deftest injury-d7cd119e-abort-kind-is-a-non-injury
  (let [record {:run/id "2026-11-03-561c5ec3-6510-42a3-930b-331ef089feb4"
                :click/id "wm-click-2780abb9-f6f8-4db6-9b8f-95d156737774"
                :terminal-receipt {:outcome :cancelled}
                :failure {:kind :debugger/aborted :stage :selection-world}}
        classification (terminal/classify record pin)]
    (is (= :non-injury (:classification classification)))
    (is (= :operator-aborted-run (:non-injury-reason classification)))))
