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

(deftest mission-mismatch-is-unbacked-not-absent
  ;; Review fix (claude-2): a recorded-but-different mission is a false claim
  ;; (:u8/claim-unbacked); only a missing record/clock is attribution-absent.
  (let [other-decision (assoc-in decision [:evidence/body :mission] "M-other")
        mismatch-sources (-> fixture-sources
                             (assoc-in [:decisions :records] [other-decision])
                             (assoc-in [:clocks :records]
                                       [{:join join :mission "M-other"}]))
        result (adjudicate [{:claim/type :mission-attribution
                             :value "M-fixture" :join join}]
                           mismatch-sources)
        absent-decision (assoc-in decision [:evidence/body :mission] nil)
        absent-sources (-> fixture-sources
                           (assoc-in [:decisions :records] [absent-decision])
                           (assoc-in [:clocks :records] []))
        absent-result (adjudicate [{:claim/type :mission-attribution
                                    :value "M-fixture" :join join}]
                                  absent-sources)]
    (is (= [:u8/claim-unbacked] (:failures result)))
    (is (= [:u8/decision-mission-attribution-absent]
           (:failures absent-result)))))

(deftest chat-derived-status-fails-despite-older-commit-signal
  ;; Review fix (claude-2): only the deriving (newest) signal can vouch for
  ;; :derived-status. An older commit-subject signal further down the list
  ;; must not launder a chat-derived status.
  (let [sources (assoc fixture-sources :status
                       {:query {:view :mission-status :mission "M-fixture"}
                        :derived-status :complete
                        :derived-from [{:source :chat-turn
                                        :text-preview "parked dependencies complete (1)"}
                                       {:source :commit-subject
                                        :subject "M-fixture work COMPLETE earlier"}]})
        result (adjudicate [{:claim/type :status :value :complete}] sources)]
    (is (= [:u8/mission-status-signal-overbroad] (:failures result)))))

(deftest real-recorded-decision-exposes-known-findings
  ;; History of this expectation: at U8b delivery (33f7a46's predecessors) the
  ;; live store derived a false :complete from chat prose, so this test pinned
  ;; BOTH :u8/decision-mission-attribution-absent AND
  ;; :u8/mission-status-signal-overbroad. D11 (33f7a46) tightened the oracle's
  ;; chat predicate; the store now derives a typed absence (nil, 0 signals),
  ;; which the gate treats as backed. The overbroad path stays pinned by
  ;; chat-derived-status-fails-despite-older-commit-signal above. What remains
  ;; live-exposed is the mission-attribution gap, until D10 lands.
  (let [{:keys [report sources decision round]}
        (real-case "M-zaif-harness-v1")
        result (adjudicate report sources)
        status-row (first (filter #(= :status (get-in % [:claim :claim/type]))
                                  (:results result)))]
    (is (= :zaif-arm-choice (get-in decision [:evidence/body :event])))
    (is (= :turn-round (get-in round [:evidence/body :event])))
    (is (= #{:u8/decision-mission-attribution-absent}
           (set (:failures result))))
    (is (= 1 (count (filter #(= :failed (:verdict %)) (:results result)))))
    (is (= :backed (:verdict status-row))
        "typed absence from the oracle backs an absence-reporting claim")))

(when (= *file* (System/getProperty "babashka.file"))
  (let [result (t/run-tests 'report-gate-test)]
    (when (pos? (+ (:fail result) (:error result)))
      (System/exit 1))))
