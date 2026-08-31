#!/usr/bin/env bb
(ns checks.trace-schema-compatibility
  (:require [futon2.aif.trace :as trace])
  (:import (java.time LocalDate)))

(defn result []
  (let [records (trace/read-all-traces)
        range-records (trace/read-trace-range (LocalDate/parse "2026-05-18")
                                              (LocalDate/parse "2026-08-31"))
        reduced-count (trace/reduce-traces (fn [n _] (inc n)) 0)
        recent (trace/recent-trace-records 1)
        latest (trace/latest-trace-record :end-date (LocalDate/parse "2026-08-31")
                                          :lookback-days 120)
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
                 (= (last records) (first recent) latest))
     :records (count records)
     :versions versions
     :reader-census {:read-all (count records)
                     :read-range (count range-records)
                     :reduce reduced-count
                     :recent-last-agrees? (= (last records) (first recent))
                     :latest-agrees? (= (last records) latest)}
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
