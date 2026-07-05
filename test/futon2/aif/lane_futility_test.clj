(ns futon2.aif.lane-futility-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.lane-futility :as futility]))

(defn- trace-record
  [action-class target verdicts]
  {:decision {:action {:type action-class
                       :target target}
              :rank 1}
   :ranked-actions [{:rank 1
                     :action {:type action-class
                              :target target}
                     :G-goal-outcome 1.0}]
   :act-gate-verdicts verdicts})

(deftest futility-summary-credits-only-selected-target-passes
  (let [records [(trace-record :advance-mission "M-a"
                               [{:mission "M-b" :verdict :pass :delta-G -0.5}])
                 (trace-record :advance-mission "M-a"
                               [{:mission "M-a" :verdict :fail :delta-G 0.0}])
                 (trace-record :advance-mission "M-a"
                               [{:mission "M-a" :verdict :pass :delta-G -0.4}])
                 (trace-record :open-mission "M-b"
                               [{:mission "M-b" :verdict :fail :delta-G 0.0}])]
        summary (futility/futility-summary records)
        by-lane (into {} (map (juxt :lane identity) (:rows summary)))]
    (is (= 4 (:record-count summary)))
    (is (= 2 (:lane-count summary)))
    (is (= 3 (get-in by-lane ["advance-mission/M-a" :attempts])))
    (is (= 1 (get-in by-lane ["advance-mission/M-a" :successes])))
    (is (= 1 (get-in by-lane ["open-mission/M-b" :attempts])))
    (is (true? (get-in by-lane ["open-mission/M-b" :zero-for-n?])))))

(deftest hand-count-gate-agrees-with-summary
  (let [records [(trace-record :advance-mission "M-a" [])
                 (trace-record :advance-mission "M-a"
                               [{:mission "M-a" :verdict :pass :delta-G -0.5}])]
        summary (futility/futility-summary records)
        hand (futility/hand-counts records)]
    (is (true? (futility/summary-matches-hand-counts? summary hand)))))

(deftest gamma-simulation-separates-futile-and-paying-lanes
  (let [misses (mapv #(assoc (futility/simulated-outcome
                              (trace-record :advance-mission "M-a" []))
                             :tick %)
                     (range 12))
        miss-final (futility/fold-gamma misses)
        pay-final (futility/fold-gamma (futility/synthetic-paying-outcomes 12))]
    (is (< (:policy-precision miss-final) 0.75))
    (is (> (:policy-precision pay-final) 1.25))))

(deftest dry-run-bulletin-emits-nag-for-thresholded-zero-for-n
  (let [bulletins (futility/dry-run-bulletins
                   (futility/synthetic-futility-summary 7))]
    (is (= 1 (count bulletins)))
    (is (= :nag (:lane (first bulletins))))
    (is (true? (:dry-run? (first bulletins))))))
