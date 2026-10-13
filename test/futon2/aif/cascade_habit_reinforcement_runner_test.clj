(ns futon2.aif.cascade-habit-reinforcement-runner-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.cascade-habit-store :as habit]
            [futon2.aif.d-predecessor-task-authority :as d-task]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runner-test :as fixture]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.report.cascade-habit-read-test :as stores]))

(use-fixtures :once hermetic/with-hermetic-stores fixture/with-hermetic-traces)
(use-fixtures :each (fn [f] (binding [runner/*wm-status-reporting?* false] (f))))

(defn- close-through-current-evidence [path]
  (#'fixture/run-token-feature-card-attempt
   (#'fixture/retention-cohort "cascade-habit-retirement")
   (#'fixture/token-loop-exercised-decision)
   {:runner-options {:cascade-habit-path path
                     :run-record-dir (str (.getParentFile (io/file path)))}}))

(deftest a-grounded-close-leaves-the-legacy-habit-store-alone
  ;; Was grounded-close-without-comparison-does-not-reinforce, which pinned the
  ;; close's receipt shape (:reinforcement :none, :reason
  ;; :no-outcome-comparison, :rule/id, :delta 0). WM-HABIT-STORE-RETIRE-I: the
  ;; runner no longer calls habit-reinforcement/close! at all, so there is no
  ;; receipt to shape — the close records the typed absence instead, and the
  ;; store is untouched on EVERY close and not only on a declining one.
  ;; Owner runs on canonical main; do not bypass the source-authority guard.
  (stores/with-store
   (fn [path]
     (spit path (pr-str (habit/read-state path)))
     (let [before (slurp path)
           {:keys [result]} (close-through-current-evidence path)
           record (edn/read-string (slurp (:run-record result)))
           receipt (:habit-reinforcement result)]
       (is (= :grounded-progress (:outcome result))
           "the authoritative fixture records a real grounded increment")
       (is (= {:absent :legacy-habit-store-retired} receipt))
       (is (nil? (:reinforcement receipt)) "no receipt at all: the rule did not run, as against running and declining")
       (is (= receipt (:habit-reinforcement record)) "the run record carries the same absence")
       (is (= :none (get-in record [:selection-event :reinforcement]))
           "the selection event is a separate, pure reading and is unchanged")
       (is (= before (slurp path)) "the store is byte-unchanged")))))

(deftest reviewer-falsifier-refusal-cannot-touch-the-legacy-store
  (stores/with-store
   (fn [path]
     (spit path (pr-str (habit/read-state path)))
     (let [before (slurp path)
           {:keys [result]}
           (with-redefs [d-task/artifact-tokens (fn [& _] [])]
             (close-through-current-evidence path))]
       (is (= :build-failed (:outcome result)))
       (is (= :reviewer-falsifier-failed
              (get-in result [:data :failure-kind])))
       (is (= :reviewer-wait (get-in result [:data :failure-stage])))
       (is (= before (slurp path))
           "a refused review cannot reach or mutate the retired store")))))
