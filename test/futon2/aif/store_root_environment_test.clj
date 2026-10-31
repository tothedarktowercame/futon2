(ns futon2.aif.store-root-environment-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.lane-futility :as lane]
            [futon2.aif.learning-trial-ledger :as learning-ledger]
            [futon2.aif.meta-injury-store :as meta-injury]
            [futon2.aif.morning-brief :as morning-brief]
            [futon2.aif.trace :as trace]))

(deftest production-defaults-are-unchanged-without-environment-overrides
  (when-not (some #(System/getenv %)
                  ["FUTON_WM_TRACE_DIR" "FUTON_WM_RUN_RECORD_DIR"
                   "FUTON_WM_PHASE_REPLAY_INPUT_ROOT"
                   "FUTON_WM_LEARNING_TRIAL_LEDGER_ROOT"
                   "FUTON_WM_MORNING_BRIEF_ROOT" "FUTON_WM_META_INJURY_ROOT"])
    (is (= "/home/joe/code/futon2/data/wm-trace" trace/default-trace-dir))
    (is (= "/home/joe/code/futon2/data/wm-trace" lane/default-trace-dir))
    (is (= "/home/joe/code/futon2/data/wm-runs" runner/default-run-record-dir))
    (is (= "/home/joe/code/futon2/data/wm-phase-replay-inputs"
           runner/default-phase-replay-input-root))
    (is (= "/home/joe/code/futon2/data/wm-learning-trials" learning-ledger/default-root))
    (is (= "/home/joe/code/futon2/data/wm-morning-brief" morning-brief/default-root))
    (is (= "/home/joe/code/futon2/data/wm-meta-injuries-live-2026-10-22"
           meta-injury/default-root))))
