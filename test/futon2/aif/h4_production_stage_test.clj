(ns futon2.aif.h4-production-stage-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.cascade-problems :as problems]
            [futon2.aif.cascade-habit-store :as habit]
            [futon2.aif.d-predecessor-task-authority :as task]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.locator-fixtures :as locators]
            [futon2.report.cascade-decision-test :as fixture]
            [futon2.report.cascade-habit-read-test :as stores]
            [futon2.aif.wm.cascade-decision :as wm-cd]))

(use-fixtures :once hermetic/with-hermetic-stores)

(deftest actual-production-decision-refuses-missing-prefix
  (stores/with-store
   (fn [path]
     (with-redefs [habit/default-path path task/default-root (str path "-missing-task-store")]
       (let [assembled (problems/assemble
                        {:targets [fixture/tick-1-target]
                         :sources (locators/locate-all fixture/tick-1-sources)})
             refusal (try
                       (wm-cd/cascade-decision
                        assembled (assoc fixture/live-c-opts :cascade-habit-path path))
                       nil
                       (catch clojure.lang.ExceptionInfo e (ex-data e)))]
         (is (= :free-energy-not-supplied
                (get-in refusal [:refusal :kind])))
         (is (= 3 (get-in refusal [:refusal :detail :n-candidates])))
         (is (= 3 (get-in refusal [:refusal :detail :n-without-f])))
         (is (= 3 (count (get-in refusal [:refusal :detail :candidate-ids])))))))))
