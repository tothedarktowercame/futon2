(ns futon2.aif.observation-test
  "Tests for the War Machine AIF observation layer.

   These tests verify normalised observation construction from raw scan
   data — moved from `futon2.report.war-machine-test` 2026-05-17 as
   part of the M-war-machine-aif-completion namespace carve."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.observation :as obs]))

(def ^:private sample-data
  "Minimal scan data for testing the observe function."
  {:loop-health {:overall 0.65
                 :arrows [{:arrow-id :work→proof :health 0.9}
                          {:arrow-id :proof→patterns :health 0.0}]
                 :healthy-count 4
                 :total-count 6
                 :loop-complete? false}
   :support-attack {:support-coverage 0.8
                    :attack-coverage 0.5
                    :claims []}
   :mission-triage {:health 0.4
                    :total 100
                    :active 20
                    :completed 50}
   :graph {:dynamics {:commit-percentages {:stack 0.7
                                           :consulting 0.1
                                           :portfolio 0.15
                                           :mathematics 0.05}
                      :ticks [{:id :hermit-warning :fired? true}
                              {:id :hobby-warning :fired? false}]}
           :summary {:total-repos 16
                     :active-repos 10
                     :total-sorrys 8
                     :coupling-edges 12
                     :ticks-firing 1}}})

(deftest observation-channels-test
  (testing "14 channels declared (v0.10 added :annotation-health), all keywords, no duplicates"
    (is (= 14 (count obs/observation-channels)))
    (is (every? keyword? obs/observation-channels))
    (is (= (count obs/observation-channels)
           (count (set obs/observation-channels))))
    (is (contains? (set obs/observation-channels) :annotation-health))))

(deftest observe-test
  (testing "observation vector has all expected channels"
    (let [o (obs/observe sample-data)]
      (is (= 0.65 (:loop-health o)))
      (is (= 0.8 (:support-coverage o)))
      (is (= 0.5 (:attack-coverage o)))
      (is (= 0.4 (:mission-health o)))
      (is (= 0.7 (:stack-pct o)))
      (is (= 0.1 (:consulting-pct o)))
      (is (= 0.15 (:portfolio-pct o)))
      (is (= 0.05 (:mathematics-pct o)))))

  (testing "active-repo-ratio normalized correctly"
    (let [o (obs/observe sample-data)]
      (is (= (/ 10.0 16) (:active-repo-ratio o)))))

  (testing "sorry-count-norm capped at 1.0"
    (let [data (assoc-in sample-data [:graph :summary :total-sorrys] 20)
          o (obs/observe data)]
      (is (= 1.0 (:sorry-count-norm o)))))

  (testing "ticks-firing-ratio"
    (let [o (obs/observe sample-data)]
      (is (= 0.5 (:ticks-firing-ratio o)))))

  (testing "sense->vector produces ordered vector"
    (let [o (obs/observe sample-data)
          v (obs/sense->vector o)]
      (is (= (count obs/observation-channels) (count v))
          "vector length matches declared channel count")
      (is (every? number? v) "all values should be numbers")
      (is (= (mapv #(get o % 0.0) obs/observation-channels) v)
          "vector order matches observation-channels order"))))

(deftest annotation-health-observation-test
  (testing ":annotation-health is sourced from (:annotation-graph data)"
    (let [data {:annotation-graph {:health 0.85 :anomaly-count 2 :section-count 14}}
          o (obs/observe data)]
      (is (= 0.85 (:annotation-health o)))))
  (testing "missing :annotation-graph yields 0.0"
    (let [o (obs/observe {})]
      (is (= 0.0 (:annotation-health o)))))
  (testing ":annotation-health appears in sense->vector at the last position"
    (let [data {:annotation-graph {:health 0.7}}
          o (obs/observe data)
          v (obs/sense->vector o)]
      (is (= 0.7 (last v))
          ":annotation-health is the last channel and matches its observed value"))))

(deftest observe-empty-data-test
  (testing "observe handles empty/nil data gracefully"
    (let [o (obs/observe {})]
      (is (every? #(= 0.0 (val %)) o)
          "all channels should be 0.0 for empty data"))))
