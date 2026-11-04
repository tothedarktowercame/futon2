(ns futon2.aif.outer-pending-observation
  "Completed-run adapter for R20Step.Observation. Only the four terminal
  EndingClass values are completed learning observations. An absent terminal
  class, and missing or corrupt evidence, defer consumption."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.previous-run :as previous]
            [futon2.aif.outer-refusal-index :as durable]
            [futon2.aif.reading-policy-class-history-index :as history]
            [futon2.aif.load-identity :as load-identity]))
(load-identity/register! *ns* *file*)
(def terminal-classes #{:focused :related :unrelated :stop-the-line})
(defn- defer [reason & [detail]] (merge {:status :deferred :reason reason} detail))

(defn observe
  "Read a completed, content-verified run. The existing publication index pins
  the whole record, while the sealed run-stream terminal proves completion.
  Index paths may name canonical files; lookup uses run identity, so copies
  can be verified without opening canonical records."
  [dir run-id]
  (try
    (let [stream (previous/read-stream dir)
          events (filter #(= run-id (:run/id %)) (:events stream))
          closed (filter #(= :closed (:event %)) events)
          file (io/file dir (str "tick-run-record-" run-id ".edn"))]
      (cond
        (not= :present (:status stream)) (defer :run-stream-unreadable)
        (empty? closed) (defer (if (some #(= :started (:event %)) events)
                                :run-in-flight :run-completion-absent))
        (not= 1 (count closed)) (defer :run-completion-ambiguous)
        (not (.isFile file)) (defer :run-record-absent)
        :else
        (let [terminal (:q6 (first closed))
              record (edn/read-string (slurp file))
              index (io/file dir history/index-name)
              rows (when (.isFile index)
                     (->> (str/split-lines (slurp index))
                          (remove str/blank?) (map edn/read-string)
                          (filter #(= run-id (:run-id %)))))
              pins (set (keep :record-sha256 rows))
              digest (durable/sha256-file file)
              classification (:run-ending-classification record)
              class (:class classification)]
          (cond
            (not= run-id (or (:run/id record) (:run-id record)))
            (defer :run-record-identity-mismatch)
            (not (and (= previous/terminal-schema (:schema terminal))
                      (previous/intact? terminal)
                      (= run-id (:run/id terminal))
                      (= terminal (get-in record [:q6 :terminal]))))
            (defer :run-record-tampered {:cause :completion-pin-mismatch})
            (not= 1 (count pins)) (defer :run-record-pin-unavailable)
            (not= digest (first pins)) (defer :run-record-tampered {:cause :record-sha-mismatch})
            (and classification
                 (not (and (= :wm/run-ending-classification-receipt-v1 (:schema classification))
                           (= :recorded (:status classification)))))
            (defer :run-ending-classification-invalid)
            :else
            (let [verified {:run-id run-id
                            :record-pin {:path (str file) :sha256 digest}
                            :completion-pin (:sha256 terminal)}]
              (if (contains? terminal-classes class)
                (assoc verified :status :ready :ending-class class)
                (defer :terminal-ending-class-absent
                       (assoc verified :classification-absence
                              {:reason (if classification :terminal-ending-class-unavailable
                                                          :classification-receipt-absent)
                               :recorded-class class
                               :completed true
                               :failure (select-keys (:failure record)
                                                     [:kind :stage :error])}))))))))
    (catch Exception e
      (defer :run-record-unreadable {:message (.getMessage e)}))))
