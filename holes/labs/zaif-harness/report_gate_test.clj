#!/usr/bin/env bb
(ns report-gate-test
  (:require [babashka.fs :as fs]
            [clojure.test :as t :refer [deftest is]]))

(def here (str (fs/parent (fs/absolutize *file*))))
(load-file (str (fs/path here "report_gate.clj")))
(load-file (str (fs/parent here) "/M-zaif-harness/z1_views.clj"))
(def adjudicate (resolve 'report-gate/adjudicate))
(def real-case (resolve 'report-gate/real-case))

(def join ["sid-fixture" "turn-fixture" 1 "turn-fixture:r1"])
(def decision
  {:evidence/session-id "sid-fixture"
   :evidence/body {:event :zaif-arm-choice :turn-id "turn-fixture" :round 1
                   :pairing-key "turn-fixture:r1" :arm :retrieve
                   :g-terms {:retrieve 0.7 :act 0.1 :ask 0.0 :yield 0.0}
                   :mission "M-fixture"}})
(def round-record
  {:evidence/session-id "sid-fixture"
   :evidence/body {:event :turn-round :turn-id "turn-fixture" :round 1
                   :calls [{:tool "memory_search" :args "{:limit 1}"}]}})
(def fixture-report
  [{:claim/type :arm-chosen :value :retrieve :join join}
   {:claim/type :g-terms
    :value {:retrieve 0.7 :act 0.1 :ask 0.0 :yield 0.0} :join join}
   {:claim/type :tool-calls
    :value [{:tool "memory_search" :args "{:limit 1}"}] :join join}
   {:claim/type :mission-attribution :value "M-fixture" :join join}
   {:claim/type :status :value :open}])
(def fixture-sources
  {:decisions {:query {:view :zaif-decisions} :records [decision]}
   :rounds {:query {:session-id "sid-fixture"} :records [round-record]}
   :clocks {:query {:view :mission-attributed}
            :records [{:join join :mission "M-fixture"}]}
   :status {:query {:view :mission-status :mission "M-fixture"}
            :derived-status :open
            :derived-from [{:source :commit-subject
                            :subject "M-fixture OPEN"}]}})

(deftest exactly-shaped-decision-report-passes
  (let [result (adjudicate fixture-report fixture-sources)]
    (is (:ok result))
    (is (empty? (:failures result)))
    (is (every? #(= :backed (:verdict %)) (:results result)))
    (is (every? :backing-query (:results result)))))

(deftest planted-unbacked-claim-fails-and-names-claim
  (let [claim {:claim/type :arm-chosen :value :ask :join join}
        result (adjudicate (conj fixture-report claim) fixture-sources)
        failed (filter #(= :failed (:verdict %)) (:results result))]
    (is (false? (:ok result)))
    (is (= [:u8/claim-unbacked] (:failures result)))
    (is (= claim (:claim (first failed))))))

(deftest real-recorded-decision-exposes-both-known-findings
  (let [{:keys [report sources decision round]}
        (real-case "M-zaif-harness-v1")
        result (adjudicate report sources)]
    (is (= :zaif-arm-choice (get-in decision [:evidence/body :event])))
    (is (= :turn-round (get-in round [:evidence/body :event])))
    (is (= #{:u8/decision-mission-attribution-absent
             :u8/mission-status-signal-overbroad}
           (set (:failures result))))
    (is (= 2 (count (filter #(= :failed (:verdict %)) (:results result)))))))

(when (= *file* (System/getProperty "babashka.file"))
  (let [result (t/run-tests 'report-gate-test)]
    (when (pos? (+ (:fail result) (:error result)))
      (System/exit 1))))
