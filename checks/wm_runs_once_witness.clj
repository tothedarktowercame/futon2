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
  (doseq [field [:startedAt :basisMaxAt :selectorSeam]]
    (when-not (nonblank? (get record field))
      (fail! :blank-string {:field field :value (get record field)})))
  (doseq [field [:basisCount :inputsRead :inputIssues :preferenceLayers]]
    (when-not (nat? (get record field))
      (fail! :non-nat {:field field :value (get record field)})))
  (when-not (pos? (:basisCount record))
    (fail! :basis-count-nonpositive {:basisCount (:basisCount record)}))
  (when-not (pos? (:inputsRead record))
    (fail! :inputs-read-nonpositive {:inputsRead (:inputsRead record)}))
  (when-not (= 5 (:preferenceLayers record))
    (fail! :preference-layer-count {:expected 5 :actual (:preferenceLayers record)}))
  (when-not (true? (:traceWritten record))
    (fail! :trace-not-written {:traceWritten (:traceWritten record)}))
  record)

(defn -main [& args]
  (try
    (let [negative? (some #{"--negative"} args)
          path (or (some #(when (not= "--negative" %) %) args)
                   (default-record-path))
          record (edn/read-string (slurp (io/file path)))
          record (if negative? (dissoc record :traceWritten) record)]
      (validate! record)
      (println (str "wm-runs-once-witness: PASS path=" path
                    " selectorSeam=" (:selectorSeam record)
                    " preferenceLayers=" (:preferenceLayers record)))
      (System/exit 0))
    (catch Exception e
      (println (.getMessage e))
      (System/exit 1))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
