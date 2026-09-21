(ns futon2.aif.cascade-habit-reinforcement-runner-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.cascade-habit-reinforcement :as reinforcement]
            [futon2.aif.cascade-habit-store :as habit]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runner-test :as fixture]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.report.cascade-habit-read-test :as stores]))

(use-fixtures :once hermetic/with-hermetic-stores fixture/with-hermetic-traces)
(use-fixtures :each (fn [f] (binding [runner/*wm-status-reporting?* false] (f))))

(deftest grounded-close-without-comparison-does-not-reinforce
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
       (is (= :none (:reinforcement receipt)))
       (is (= :no-outcome-comparison (:reason receipt)))
       (is (= reinforcement/rule-id (:rule/id receipt)))
       (is (= 0 (:delta receipt)))
       (is (= receipt (:habit-reinforcement record)))
       (is (= :none (get-in record [:selection-event :reinforcement])))
       (is (= before (slurp path)))))))
