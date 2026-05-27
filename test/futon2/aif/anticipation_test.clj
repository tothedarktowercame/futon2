(ns futon2.aif.anticipation-test
  "Tests for futon2.aif.anticipation — v0.13 read-only anticipation reader."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.anticipation :as anticipation])
  (:import (java.time Duration Instant)))

(deftest load-anticipations-default-path-test
  (testing "default-path load returns a document with :meta + :events + :resolutions"
    (let [doc (anticipation/load-anticipations)]
      (when doc
        (is (map? doc))
        (is (contains? doc :events))
        (is (sequential? (:events doc)))))))

(deftest load-anticipations-missing-path-returns-nil-test
  (testing "missing/unreadable path returns nil (not exception)"
    (is (nil? (anticipation/load-anticipations "/nonexistent/path.edn")))))

(deftest events-in-horizon-shape-test
  (testing "returns vec of events with at-instant assoc'd"
    (let [now (Instant/parse "2026-05-19T00:00:00Z")
          horizon (Duration/ofDays 30)
          doc {:events [{:event/id "ev-near"
                         :event/at (Instant/parse "2026-05-25T00:00:00Z")}
                        {:event/id "ev-far"
                         :event/at (Instant/parse "2026-12-01T00:00:00Z")}
                        {:event/id "ev-past"
                         :event/at (Instant/parse "2025-01-01T00:00:00Z")}]}
          result (anticipation/events-in-horizon doc now horizon)]
      (is (= 1 (count result))
          "only the near-future event lies within 30-day horizon")
      (is (= "ev-near" (-> result first :event/id))))))

(deftest events-in-horizon-sorts-ascending-test
  (testing "events sorted ascending by :event/at"
    (let [now (Instant/parse "2026-01-01T00:00:00Z")
          horizon (Duration/ofDays 365)
          doc {:events [{:event/id "ev-c"
                         :event/at (Instant/parse "2026-07-01T00:00:00Z")}
                        {:event/id "ev-a"
                         :event/at (Instant/parse "2026-03-01T00:00:00Z")}
                        {:event/id "ev-b"
                         :event/at (Instant/parse "2026-05-01T00:00:00Z")}]}
          ids (mapv :event/id (anticipation/events-in-horizon doc now horizon))]
      (is (= ["ev-a" "ev-b" "ev-c"] ids)))))

(deftest events-in-horizon-handles-malformed-at-test
  (testing "events with missing or unparseable :event/at are dropped silently"
    (let [now (Instant/parse "2026-05-19T00:00:00Z")
          horizon (Duration/ofDays 30)
          doc {:events [{:event/id "ev-good"
                         :event/at (Instant/parse "2026-05-25T00:00:00Z")}
                        {:event/id "ev-missing-at"}
                        {:event/id "ev-bad-at" :event/at "not-a-date"}]}
          ids (set (map :event/id (anticipation/events-in-horizon doc now horizon)))]
      (is (= #{"ev-good"} ids)))))

(deftest summarise-event-strips-verbose-fields-test
  (testing "summarise keeps typed fields, drops prose-heavy fields"
    (let [event {:event/id "ev-x"
                 :event/kind :lifecycle-deadline
                 :event/at (Instant/parse "2026-05-28T23:59:59Z")
                 :event/at-precision :hard-deadline
                 :event/p-fires 0.4
                 :event/p-fires-rationale "lots of prose here"
                 :event/basin :postdoc-academic
                 :event/mission "M-EOI"
                 :event/lifecycle-status :drafted-awaiting-reply
                 :event/next-gate :submitted-or-not-submitted
                 :event/visibility :hyperreal-only
                 :event/efe-sketch {:if-submit {:lots :of-nested-prose}}
                 :event/notes "more prose"}
          s (anticipation/summarise-event event)]
      (is (contains? s :event/id))
      (is (contains? s :event/kind))
      (is (contains? s :event/p-fires))
      (is (contains? s :event/basin))
      (is (not (contains? s :event/p-fires-rationale))
          "verbose rationale dropped")
      (is (not (contains? s :event/efe-sketch))
          "nested efe-sketch dropped (R5 work will re-introduce when it composes)"))))

(deftest anticipation-snapshot-shape-test
  (testing "snapshot has documented keys"
    (let [snap (anticipation/anticipation-snapshot)]
      (is (contains? snap :events-loaded?))
      (is (contains? snap :path))
      (is (contains? snap :horizon-days))
      (is (contains? snap :events))
      (is (boolean? (:events-loaded? snap)))
      (is (vector? (:events snap))))))

(deftest anticipation-snapshot-fallback-on-missing-source-test
  (testing "snapshot returns :events-loaded? false when source unreadable"
    (let [snap (anticipation/anticipation-snapshot {:path "/nonexistent"})]
      (is (false? (:events-loaded? snap)))
      (is (empty? (:events snap))))))

;; ---------------------------------------------------------------------------
;; v0.14: time-pressure
;; ---------------------------------------------------------------------------

(deftest time-pressure-empty-snapshot-test
  (testing "no events ⇒ time-pressure 0"
    (is (zero? (anticipation/time-pressure
                {:events-loaded? false :horizon-days 30 :events []}
                (Instant/parse "2026-05-19T00:00:00Z"))))
    (is (zero? (anticipation/time-pressure
                {:events-loaded? true :horizon-days 30 :events []}
                (Instant/parse "2026-05-19T00:00:00Z"))))))

(deftest time-pressure-far-event-low-test
  (testing "event near horizon edge ⇒ low pressure"
    (let [snap {:events-loaded? true
                :horizon-days 30
                :events [{:event/id "ev-far"
                          :event/at "2026-06-18T00:00:00Z"}]}
          pressure (anticipation/time-pressure
                    snap (Instant/parse "2026-05-19T00:00:00Z"))]
      (is (< pressure 0.1)
          (str "30 days out of 30-day horizon → ≈0 pressure; got " pressure)))))

(deftest time-pressure-near-event-high-test
  (testing "event very near now ⇒ high pressure"
    (let [snap {:events-loaded? true
                :horizon-days 30
                :events [{:event/id "ev-tomorrow"
                          :event/at "2026-05-20T00:00:00Z"}]}
          pressure (anticipation/time-pressure
                    snap (Instant/parse "2026-05-19T00:00:00Z"))]
      (is (> pressure 0.95)
          (str "1 day out of 30-day horizon → ≈0.967 pressure; got " pressure)))))

(deftest time-pressure-closest-event-dominates-test
  (testing "multi-event snapshot: closest event dominates"
    (let [snap {:events-loaded? true
                :horizon-days 30
                :events [{:event/id "ev-near" :event/at "2026-05-21T00:00:00Z"}
                         {:event/id "ev-far" :event/at "2026-06-15T00:00:00Z"}]}
          pressure (anticipation/time-pressure
                    snap (Instant/parse "2026-05-19T00:00:00Z"))]
      (is (> pressure 0.9)
          "closest event (2 days) drives high pressure, not far event"))))

(deftest anticipation-snapshot-finds-canonical-glasgow-event-test
  (testing "smoke test: the seed Glasgow event is reachable from default-path"
    (let [snap (anticipation/anticipation-snapshot
                {:now (Instant/parse "2026-05-19T00:00:00Z")
                 :horizon-days 14})]
      (when (:events-loaded? snap)
        (is (some #(= "ev-2026-05-28-glasgow-cogito-submit-or-not" (:event/id %))
                  (:events snap))
            "Glasgow seed event should be within 14 days of 2026-05-19")))))
