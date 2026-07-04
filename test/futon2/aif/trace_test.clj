(ns futon2.aif.trace-test
  "Tests for R8 per-call trace persistence."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.belief :as belief]
            [futon2.aif.trace :as trace])
  (:import (java.io File)
           (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)
           (java.time LocalDate)))

(def ^:dynamic *tmpdir* nil)

(defn- with-tmpdir [f]
  (let [dir (Files/createTempDirectory "wm-trace-test" (into-array FileAttribute []))]
    (binding [*tmpdir* (str dir)]
      (try (f)
           (finally
             (doseq [^File child (reverse (file-seq (io/file (str dir))))]
               (.delete child)))))))

(use-fixtures :each with-tmpdir)

(def ^:private sample-judge-output
  "Minimal judge-style output covering the trace-record fields."
  {:belief (belief/initial-belief-state [:m1])
   :observation {:loop-health 0.7 :stack-pct 0.2}
   :free-energy {:G-pragmatic 0.05 :G-epistemic 0.10 :G-total 0.075
                 :per-channel {:loop-health {:value 0.7 :gap 0.0 :in-range? false}}
                 :avoided-active []}
   :ranked-actions [{:action {:type :no-op}
                     :G-risk 0.05 :G-ambiguity 0.0 :G-structural-pressure 0.0
                     :G-total 0.05 :rank 1
                     :prediction {:huge :nested :map :that-should-be-stripped}}
                    {:action {:type :address-sorry :target :sorry/x}
                     :G-risk 0.03 :G-ambiguity 0.015 :G-structural-pressure 0.7
                     :G-total 0.045 :rank 2
                     :prediction {:also :stripped}}]
   :decision {:action {:type :no-op}
              :rank 1 :G-total 0.05 :tau 0.2
              :softmax-weights {:will-be-stripped :for-trace}}
   :mode :multiplied})

(deftest trace-record-shape-test
  (testing "trace-record extracts all documented fields"
    (let [r (trace/trace-record sample-judge-output)]
      (is (string? (:timestamp r)) "ISO-8601 timestamp")
      (is (contains? r :mu-pre))
      (is (contains? r :mu-post))
      (is (contains? r :observation))
      (is (contains? r :free-energy))
      (is (contains? r :ranked-actions))
      (is (contains? r :decision))
      (is (contains? r :mode)))))

(deftest trace-record-strips-prediction-field-test
  (testing "ranked-actions in trace drop the heavy :prediction field"
    (let [r (trace/trace-record sample-judge-output)
          rs (:ranked-actions r)]
      (is (every? #(not (contains? % :prediction)) rs)
          "trace ranked-actions don't carry :prediction"))
    (let [r (trace/trace-record sample-judge-output)
          rs (:ranked-actions r)]
      (is (= [0.0 0.7] (mapv :G-structural-pressure rs))
          "trace preserves the structural-pressure term in ranked-actions"))))

(deftest trace-record-strips-softmax-weights-test
  (testing "decision in trace drops :softmax-weights (non-stringable keys)"
    (let [r (trace/trace-record sample-judge-output)]
      (is (not (contains? (:decision r) :softmax-weights))
          "decision in trace doesn't carry :softmax-weights"))))

(deftest trace-record-pure-test
  (testing "trace-record is pure (modulo timestamp): same input → same shape"
    (let [r1 (trace/trace-record sample-judge-output)
          r2 (trace/trace-record sample-judge-output)]
      (is (= (dissoc r1 :timestamp) (dissoc r2 :timestamp))))))

(deftest write-trace-creates-file-test
  (testing "write-trace! creates the daily file under the given dir"
    (let [path (trace/write-trace! sample-judge-output
                                   :dir *tmpdir*
                                   :date-str "2026-05-17")]
      (is (str/ends-with? path "wm-trace-2026-05-17.edn"))
      (is (.exists (io/file path))))))

(deftest write-trace-appends-test
  (testing "two writes produce two records in the file"
    (trace/write-trace! sample-judge-output :dir *tmpdir* :date-str "2026-05-17")
    (trace/write-trace! sample-judge-output :dir *tmpdir* :date-str "2026-05-17")
    (let [records (trace/read-trace :dir *tmpdir* :date-str "2026-05-17")]
      (is (= 2 (count records))))))

(deftest read-trace-roundtrip-test
  (testing "write then read returns the same records (modulo timestamp)"
    (trace/write-trace! sample-judge-output :dir *tmpdir* :date-str "2026-05-17")
    (let [[r] (trace/read-trace :dir *tmpdir* :date-str "2026-05-17")]
      (is (= (:observation sample-judge-output) (:observation r)))
      (is (= (:mode sample-judge-output) (:mode r)))
      (is (= 2 (count (:ranked-actions r)))
          "both ranked actions preserved"))))

(deftest read-trace-missing-file-returns-empty-test
  (testing "read-trace on a non-existent file returns empty vec"
    (is (= [] (trace/read-trace :dir *tmpdir* :date-str "1999-01-01")))))

(deftest read-trace-records-are-clojure-types-test
  (testing "edn-lines preserve keyword keys and clojure-native types on read"
    (trace/write-trace! sample-judge-output :dir *tmpdir* :date-str "2026-05-17")
    (let [[r] (trace/read-trace :dir *tmpdir* :date-str "2026-05-17")]
      (is (keyword? (:mode r)))
      (is (= :multiplied (:mode r)))
      (is (map? (:observation r))))))

(deftest trace-record-propagates-policy-precision-test
  (testing "R14 γ-state propagates through trace-record from judge output"
    (let [gamma-state {:policy-precision 1.6 :error-history [0.2 0.1]
                       :mean-error 0.15 :samples 7}
          r (trace/trace-record (assoc sample-judge-output
                                       :policy-precision gamma-state))]
      (is (= gamma-state (:policy-precision r)))))
  (testing "absent γ-state reconstructs the prior (γ=1.0), never nil"
    (let [r (trace/trace-record sample-judge-output)]
      (is (= 1.0 (get-in r [:policy-precision :policy-precision]))
          "trace always carries a usable γ-state for the next tick's read-back"))))

(deftest policy-precision-roundtrips-through-trace-test
  (testing "γ-state survives write → read so the next tick continues the window"
    (let [gamma-state {:policy-precision 0.75 :error-history [0.6 0.7 0.65]
                       :mean-error 0.65 :samples 12}
          out (assoc sample-judge-output :policy-precision gamma-state)]
      (trace/write-trace! out :dir *tmpdir* :date-str "2026-06-26")
      (let [record (trace/latest-trace-record :dir *tmpdir*
                                              :end-date (LocalDate/parse "2026-06-26")
                                              :lookback-days 1)]
        (is (= gamma-state (:policy-precision record)))))))

(deftest realized-outcome-present-only-passthrough-test
  (testing "R16 :realized-outcome is propagated when the enactor supplies it"
    (let [outcome {:policy :p/x :expected-G 0.2 :realized-G 0.05 :tick 41}
          r (trace/trace-record (assoc sample-judge-output :realized-outcome outcome))]
      (is (= outcome (:realized-outcome r)))))
  (testing "absent today (enactment not live-wired) ⇒ key not present (not nil)"
    (let [r (trace/trace-record sample-judge-output)]
      (is (not (contains? r :realized-outcome))
          "present-only: no noisy nil seam in ordinary records"))))

(deftest latest-trace-record-spans-midnight-utc-test
  (testing "latest-trace-record falls back to yesterday when today's bucket is empty"
    (let [yesterday-output (assoc sample-judge-output
                                  :precision-state {:annotation-health
                                                    {:precision 42.0
                                                     :error-history [0.1 0.2]}})]
      (trace/write-trace! yesterday-output :dir *tmpdir* :date-str "2026-05-17")
      (let [record (trace/latest-trace-record :dir *tmpdir*
                                              :end-date (LocalDate/parse "2026-05-18")
                                              :lookback-days 2)]
        (is (= 42.0 (get-in record [:precision-state :annotation-health :precision])))
        (is (= [0.1 0.2]
               (get-in record [:precision-state :annotation-health :error-history])))))))

;; ---------------------------------------------------------------------------
;; M-evaluate-policies D1a (2026-07-03) — whitelist covers the blend's terms
;; ---------------------------------------------------------------------------

(deftest strip-ranked-action-whitelist-test
  (testing "I4: every term entering :G-total survives the trace strip"
    (let [entry {:action {:type :no-op}
                 :G-risk 1.0 :G-ambiguity 2.0 :G-info 0.1 :G-survival 0.2
                 :G-structural-pressure 0.3 :G-goal-outcome 0.4
                 :G-gap 0.5 :G-graph-pragmatic 0.6 :G-core 3.0
                 :G-total 7.1 :rank 1
                 :prediction {:dropme true}}
          rec (trace/trace-record {:belief {} :observation {} :free-energy {}
                                   :ranked-actions [entry]
                                   :decision {:action :abstain} :mode :test})
          kept (first (:ranked-actions rec))]
      (doseq [k [:G-gap :G-graph-pragmatic :G-core :G-goal-outcome :G-total]]
        (is (contains? kept k) (str k " must survive the strip")))
      (is (not (contains? kept :prediction)) "the deep :prediction still drops"))))

;; ---------------------------------------------------------------------------
;; B-0a (M-aif-faithfulness §2.0) — tick provenance stamp
;; ---------------------------------------------------------------------------

(def ^:private sample-resolved-flags
  "A resolved mode/flag set as the scheduled runner assembles it (the arena
   fns + the live-wire switch); values here are fixtures, not env reads."
  {:risk-mode :kl
   :ambiguity-mode :gaussian-entropy
   :goal-outcome-mode :kl
   :kl-channel-weights {}
   :c-temperature 0.1
   :live-wire? true})

(deftest wm-version-stamp-shape-test
  (testing "stamp = git identity + resolved flags + schema version"
    (let [stamp (trace/wm-version-stamp sample-resolved-flags)]
      (is (or (= :unknown (:git-sha stamp))
              (and (string? (:git-sha stamp))
                   (re-matches #"[0-9a-f]{40}" (:git-sha stamp))))
          "full 40-char sha (or :unknown when git is unavailable)")
      (is (contains? stamp :git-dirty?))
      (is (= trace/trace-schema-version (:trace-schema-version stamp))
          "the record-shape version rides inside the stamp")
      (is (= :kl (:risk-mode stamp)))
      (is (true? (:live-wire? stamp))
          "caller-resolved flags pass through unmodified"))))

(deftest wm-version-roundtrips-through-trace-test
  (testing "acceptance: (wm-version-of tick) recovers sha+flags from a record"
    (let [stamp (trace/wm-version-stamp sample-resolved-flags)
          out (assoc sample-judge-output :wm-version stamp)]
      (trace/write-trace! out :dir *tmpdir* :date-str "2026-07-04")
      (let [[r] (trace/read-trace :dir *tmpdir* :date-str "2026-07-04")
            v (trace/wm-version-of r)]
        (is (= stamp v) "the stamp survives write → read intact")
        (is (some? (:git-sha v)) "which code — answerable from the record")
        (is (= :kl (:risk-mode v)) "which config — answerable from the record")))))

(deftest wm-version-absent-when-not-stamped-test
  (testing "purely additive: un-stamped records don't grow a nil :wm-version"
    (let [r (trace/trace-record sample-judge-output)]
      (is (not (contains? r :wm-version))
          "present-only, so bare judge calls and old records are unchanged")
      (is (nil? (trace/wm-version-of r))
          "the accessor answers nil, not a throw, for pre-B-0a records"))))
