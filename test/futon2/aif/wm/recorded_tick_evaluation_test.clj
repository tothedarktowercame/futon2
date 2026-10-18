(ns futon2.aif.wm.recorded-tick-evaluation-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.wm.recorded-tick-evaluation :as sut]))

(def certified {:status :certified})
(def replayed {:enacted #{["p/x" 0]}})

(deftest effects-alone-do-not-claim-a-production-tick
  (is (= :production-close-evidence-missing
         (:kind (sut/evaluate {:source-run-id "run"
                               :journal-validation certified
                               :journal-replay replayed})))))

(deftest close-must-bind-the-run-and-published-trace
  (is (= :production-close-run-mismatch
         (:kind (sut/evaluate {:source-run-id "run"
                               :journal-validation certified
                               :journal-replay replayed
                               :production-close-record
                               {:run/id "other" :trace-written? true}}))))
  (is (= :production-trace-not-written
         (:kind (sut/evaluate {:source-run-id "run"
                               :journal-validation certified
                               :journal-replay replayed
                               :production-close-record
                               {:run/id "run" :trace-written? false}}))))
  (is (= {:status :carried :ticks-carried 1 :source-run-id "run"
          :enacted-count 1}
         (sut/evaluate {:source-run-id "run"
                        :journal-validation certified
                        :journal-replay replayed
                        :production-close-record
                        {:run/id "run" :trace-written? true}}))))
