(ns futon2.aif.cascade-habit-reinforcement-runner-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.cascade-habit-store :as habit]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runner-test :as fixture]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.report.cascade-habit-read-test :as stores]))

(use-fixtures :once hermetic/with-hermetic-stores fixture/with-hermetic-traces)
(use-fixtures :each (fn [f] (binding [runner/*wm-status-reporting?* false] (f))))

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
           {:keys [result]} (#'fixture/run-feature-card-attempt
                             {:author-card fixture/feature-card-claim
                              :runner-options {:cascade-habit-path path
                                               :run-record-dir (str (.getParentFile (io/file path)))}})
           record (edn/read-string (slurp (:run-record result)))
           receipt (:habit-reinforcement result)]
       (is (= :grounded-change (:outcome result)))
       (is (= {:absent :legacy-habit-store-retired} receipt))
       (is (nil? (:reinforcement receipt)) "no receipt at all: the rule did not run, as against running and declining")
       (is (= receipt (:habit-reinforcement record)) "the run record carries the same absence")
       (is (= :none (get-in record [:selection-event :reinforcement]))
           "the selection event is a separate, pure reading and is unchanged")
       (is (= before (slurp path)) "the store is byte-unchanged")))))
