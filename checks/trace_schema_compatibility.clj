#!/usr/bin/env bb
(ns checks.trace-schema-compatibility
  (:require [clojure.java.io :as io]
            [futon2.aif.trace :as trace])
  (:import (java.io File)
           (java.time LocalDate)))

(def ^:private trace-dir
  (str (System/getProperty "user.home") "/code/futon2/data/wm-trace"))

(def ^:private fallback-start
  "Used only when the store has no daily file at all; the readers then all
   return nothing and :pass? fails on `(seq records)` rather than on a date."
  (LocalDate/parse "2026-05-18"))

(defn- store-dates
  "Every date the store holds a daily trace file for, ascending. Read from the
   directory rather than fixed in this file: the assertion below is that
   read-all-traces, read-trace-range, reduce-traces, recent-trace-records and
   latest-trace-record agree, and a window fixed when the check was written
   turns that into `no tick has run since that date`. It did: the range reader
   saw 803 of 885 records and :latest-agrees? went false on 2026-09-01, when
   wm-trace-2026-09-01.edn (79 records) landed outside the 2026-08-31 bound,
   with zero field-evidence failures."
  []
  (let [root (io/file trace-dir)]
    (->> (if (.isDirectory root) (.listFiles root) (make-array File 0))
         (keep #(second (re-matches #"wm-trace-(\d{4}-\d{2}-\d{2})\.edn"
                                    (.getName ^File %))))
         sort
         (mapv #(LocalDate/parse %)))))

(defn result []
  (let [dates (store-dates)
        start-date (or (first dates) fallback-start)
        end-date (or (peek dates) start-date)
        records (trace/read-all-traces)
        range-records (trace/read-trace-range start-date end-date)
        reduced-count (trace/reduce-traces (fn [n _] (inc n)) 0)
        recent (trace/recent-trace-records 1)
        latest (trace/latest-trace-record :end-date end-date
                                          :lookback-days 120)
        ;; Deriving the window makes read-all vs read-range a near-identity:
        ;; both enumerate the same daily files, so on its own it no longer
        ;; states anything the store can violate. This sub-window keeps the
        ;; range reader under a claim that can fail: read the last two dates
        ;; the store holds and require the result to equal BOTH (a) those two
        ;; daily files concatenated, read one at a time through read-trace,
        ;; and (b) the tail of the corpus. (a) pins inclusivity of both bounds
        ;; -- a start read as exclusive returns the newest file only and the
        ;; counts part -- and (b) pins ordering across the file seam. (b)
        ;; alone would not catch the start case, since a shorter tail is
        ;; still a tail.
        tail-dates (vec (take-last 2 dates))
        tail-records (if (seq tail-dates)
                       (trace/read-trace-range (first tail-dates) (peek tail-dates))
                       [])
        tail-by-file (vec (mapcat #(trace/read-trace :date-str (str %)) tail-dates))
        tail-agrees? (and (= tail-records tail-by-file)
                          (= tail-records
                             (vec (take-last (count tail-records) records))))
        versions (frequencies
                  (map #(or (get-in % [:wm-version :trace-schema-version])
                            :unversioned)
                       records))
        oldest (first records)
        evidence
        (for [[field _] trace/trace-evidence-fields
              :let [entry (trace/trace-field-evidence oldest field)]]
          [field entry])
        failures
        (vec
         (for [record records
               [field {:keys [introduced]}] trace/trace-evidence-fields
               :let [version (get-in record [:wm-version :trace-schema-version])
                     evidence (trace/trace-field-evidence record field)]
               :when (and (or (nil? version) (< version introduced))
                          (not= :predates-field (:reason evidence))
                          (not= :present (:status evidence)))]
           {:field field :version (or version :unversioned)
            :evidence evidence}))]
    {:pass? (and (seq records) (empty? failures)
                 (= (count records) (count range-records) reduced-count)
                 tail-agrees?
                 (= (last records) (first recent) latest))
     :records (count records)
     :window {:start (str start-date) :end (str end-date)
              :derived-from :store-daily-files
              :daily-files (count dates)}
     :versions versions
     :reader-census {:read-all (count records)
                     :read-range (count range-records)
                     :reduce reduced-count
                     :recent-last-agrees? (= (last records) (first recent))
                     :latest-agrees? (= (last records) latest)
                     :range-tail-dates (mapv str tail-dates)
                     :range-tail (count tail-records)
                     :range-tail-by-file (count tail-by-file)
                     :range-tail-agrees? tail-agrees?}
     :oldest {:timestamp (:timestamp oldest)
              :schema-version (or (get-in oldest [:wm-version :trace-schema-version])
                                  :unversioned)
              :field-evidence (into {} evidence)}
     :failures failures}))

(defn -main [& args]
  (let [negative? (some #{"--negative"} args)
        base (result)
        control (trace/trace-field-evidence
                 {:wm-version {:trace-schema-version trace/trace-schema-version}}
                 :observation-envelope)
        rejected? (= :malformed (:reason control))]
    (if negative?
      (if rejected?
        (do (println "trace-schema-compatibility: negative-control PASS (current-version missing field rejected as malformed) exit-convention=0-pass/1-fail/2-mutation-slipped")
            (System/exit 0))
        (do (println "trace-schema-compatibility: FAIL mutation slipped exit-convention=0-pass/1-fail/2-mutation-slipped")
            (System/exit 2)))
      (do
        (println (pr-str base))
        (println (str "trace-schema-compatibility: " (if (:pass? base) "PASS" "FAIL")
                      " exit-convention=0-pass/1-fail/2-mutation-slipped"))
        (System/exit (if (:pass? base) 0 1))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
