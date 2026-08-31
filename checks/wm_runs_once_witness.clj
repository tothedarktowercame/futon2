#!/usr/bin/env bb
(require '[babashka.classpath :as cp])
(cp/add-classpath "src")

(ns checks.wm-runs-once-witness
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn- today-date-string []
  (str (java.time.LocalDate/now (java.time.ZoneId/of "UTC"))))

(defn- default-record-path []
  (str "holes/labs/wm-contract/tick-run-record-" (today-date-string) ".edn"))

(defn- fail! [finding data]
  (throw (ex-info (str "wm-runs-once-witness: FAIL " (name finding))
                  (assoc data :finding finding))))

(defn- nonblank? [x]
  (and (string? x) (not (str/blank? x))))

(defn- nat? [x]
  (and (integer? x) (not (neg? x))))

(defn- validate! [record]
  (doseq [field [:startedAt :storeBasisMaxAt :selectorSeam]]
    (when-not (nonblank? (get record field))
      (fail! :blank-string {:field field :value (get record field)})))
  (doseq [field [:storeBasisCount :entriesRead :entriesLimit
                 :inputsRead :inputIssues :preferenceLayers]]
    (when-not (nat? (get record field))
      (fail! :non-nat {:field field :value (get record field)})))
  (when-not (pos? (:storeBasisCount record))
    (fail! :basis-count-nonpositive {:storeBasisCount (:storeBasisCount record)}))
  (when-not (pos? (:entriesLimit record))
    (fail! :entries-limit-nonpositive {:entriesLimit (:entriesLimit record)}))
  (when (> (:entriesRead record) (:entriesLimit record))
    (fail! :entries-exceed-limit {:entriesRead (:entriesRead record)
                                  :entriesLimit (:entriesLimit record)}))
  (when-not (pos? (:inputsRead record))
    (fail! :inputs-read-nonpositive {:inputsRead (:inputsRead record)}))
  (when-not (= 5 (:preferenceLayers record))
    (fail! :preference-layer-count {:expected 5 :actual (:preferenceLayers record)}))
  (when-not (true? (:traceWritten record))
    (fail! :trace-not-written {:traceWritten (:traceWritten record)}))
  (when-not (vector? (:route record))
    (fail! :route-not-vector {:route (:route record)}))
  record)

(defn -main [& args]
  (let [negative? (some #{"--negative"} args)]
   (try
    (let [
          path (or (some #(when (not= "--negative" %) %) args)
                   (default-record-path))
          record (edn/read-string (slurp (io/file path)))
          record (if negative? (dissoc record :traceWritten) record)]
      (validate! record)
      (if negative?
        (do (println "wm-runs-once-witness: FAIL negative mutation passed exit-convention=0-pass/1-fail")
            (System/exit 2))
        (do (println (str "wm-runs-once-witness: PASS path=" path
                          " selectorSeam=" (:selectorSeam record)
                          " preferenceLayers=" (:preferenceLayers record)
                          " exit-convention=0-pass/1-fail"))
            (System/exit 0))))
    (catch Exception e
      (if negative?
        (do (println (str "wm-runs-once-witness: PASS negative control rejected finding=" (.getMessage e)
                          " exit-convention=0-pass/1-fail"))
            (System/exit 0))
        (do (println (str (.getMessage e) " exit-convention=0-pass/1-fail"))
            (System/exit 1)))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
