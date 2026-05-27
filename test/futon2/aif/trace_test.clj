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
                     :G-risk 0.05 :G-ambiguity 0.0 :G-total 0.05 :rank 1
                     :prediction {:huge :nested :map :that-should-be-stripped}}
                    {:action {:type :address-sorry :target :sorry/x}
                     :G-risk 0.03 :G-ambiguity 0.015 :G-total 0.045 :rank 2
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
          "trace ranked-actions don't carry :prediction"))))

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
