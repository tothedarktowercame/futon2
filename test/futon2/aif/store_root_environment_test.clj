(ns futon2.aif.store-root-environment-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.data-paths :as data-paths]
            [futon2.aif.d-predecessor-task-authority :as d-task]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.lane-futility :as lane]
            [futon2.aif.learning-trial-ledger :as learning-ledger]
            [futon2.aif.meta-injury-store :as meta-injury]
            [futon2.aif.morning-brief :as morning-brief]
            [futon2.aif.trace :as trace]
            [futon2.aif.tripwire :as tripwire]
            [futon2.aif.wm.on-prod-history :as on-prod]))

(deftest production-defaults-are-unchanged-without-environment-overrides
  (when-not (some #(System/getenv %)
                  ["FUTON_WM_TRACE_DIR" "FUTON_WM_RUN_RECORD_DIR"
                   "FUTON_WM_PHASE_REPLAY_INPUT_ROOT"
                   "FUTON_WM_LEARNING_TRIAL_LEDGER_ROOT"
                   "FUTON_WM_MORNING_BRIEF_ROOT" "FUTON_WM_META_INJURY_ROOT"])
    (is (= (data-paths/path "wm-trace") trace/default-trace-dir))
    (is (= (data-paths/path "wm-trace") lane/default-trace-dir))
    (is (= (data-paths/path "wm-runs") runner/default-run-record-dir))
    (is (= (data-paths/path "wm-phase-replay-inputs")
           runner/default-phase-replay-input-root))
    (is (= (data-paths/path "wm-learning-trials") learning-ledger/default-root))
    (is (= (data-paths/path "wm-morning-brief") morning-brief/default-root))
    (is (= (data-paths/path "wm-meta-injuries-live-2026-10-22")
           meta-injury/default-root))))

(deftest tests-cannot-reach-canonical-data
  ;; The :test alias sets -Dfuton2.data-root.test=true. Every default below then
  ;; resolves under one scratch root per JVM, and the shutdown guard in
  ;; futon2.data-paths halts with 86 if canonical data changed anyway.
  (is data-paths/test-mode?)
  (is (not= data-paths/production-data-root data-paths/*data-root*))
  (doseq [root [d-task/default-root runner/default-run-record-dir on-prod/default-root
                runner/default-phase-replay-input-root trace/default-trace-dir
                tripwire/default-trip-root]]
    (is (not (.startsWith ^String root data-paths/production-data-root)) root)))
