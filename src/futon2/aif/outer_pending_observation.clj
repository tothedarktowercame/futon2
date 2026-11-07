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
            [futon2.aif.run-ending-classification :as ending-classification]
            [futon2.aif.load-identity :as load-identity]))
(load-identity/register! *ns* *file*)
(def terminal-classes #{:focused :related :unrelated :stop-the-line})
(defn- defer [reason & [detail]] (merge {:status :deferred :reason reason} detail))

(def ^:dynamic *jvm-start*
  "The serving JVM's start instant, overridable in tests. A run whose :started
  event predates this cannot be in flight: runs are fired by the serving JVM,
  so its process is gone."
  nil)

(defn- jvm-start []
  (or *jvm-start*
      (java.time.Instant/ofEpochMilli
       (.getStartTime (java.lang.management.ManagementFactory/getRuntimeMXBean)))))

(defn- instant [s]
  (when (string? s)
    (try (java.time.Instant/parse s) (catch Exception _ nil))))

(defn abandonment-evidence
  "Evidence that a started-not-closed run cannot still be running, or nil.
  Never elapsed time alone. Two kinds, both from the record itself:
  :superseded-by-later-closed-run — another run started after RUN-ID's start
  and has since closed in the same stream; :jvm-restart — the serving JVM
  started after RUN-ID's start, so the process that fired the run is gone."
  [events run-id]
  (let [start-events (filter #(= :started (:event %)) events)
        at (some #(when (= run-id (:run/id %)) (instant (:startedAt %)))
                 start-events)
        later-closed
        (some (fn [closed]
                (let [other (some #(when (= (:run/id closed) (:run/id %)) %)
                                  start-events)
                      other-at (some-> other :startedAt instant)]
                  (when (and (= :closed (:event closed))
                             (not= run-id (:run/id closed))
                             at other-at
                             (.isAfter ^java.time.Instant other-at at))
                    {:kind :superseded-by-later-closed-run
                     :later-run-id (:run/id closed)
                     :later-startedAt (:startedAt other)})))
              events)
        jvm (let [s (jvm-start)]
              (when (and at s (.isAfter ^java.time.Instant s at))
                {:kind :jvm-restart :jvm-started-at (str s)}))]
    (or later-closed jvm)))

(defn observe
  "Read a completed, content-verified run. The existing publication index pins
  the whole record, while the sealed run-stream terminal proves completion.
  Index paths may name canonical files; lookup uses run identity, so copies
  can be verified without opening canonical records."
  [dir run-id]
  (try
    (let [stream (previous/read-stream dir)
          all-events (:events stream)
          events (filter #(= run-id (:run/id %)) all-events)
          closed (filter #(= :closed (:event %)) events)
          file (io/file dir (str "tick-run-record-" run-id ".edn"))]
      (cond
        (not= :present (:status stream)) (defer :run-stream-unreadable)
        (empty? closed)
        (if (some #(= :started (:event %)) events)
          (if-let [evidence (abandonment-evidence all-events run-id)]
            (defer :run-abandoned {:run-id run-id :abandonment-evidence evidence})
            (defer :run-in-flight {:run-id run-id}))
          (defer :run-completion-absent {:run-id run-id}))
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
              stored-class (:class classification)
              derived (when (= :unknown stored-class)
                        (ending-classification/classify
                         {:close (:close-projection classification)
                          :occurrence (get-in classification
                                              [:close-projection :occurrence])
                          :route-attestation (get-in classification
                                                     [:close-projection :route-attestation])
                          :outer-selection (:outer/selection record)}))
              derived-class (when (and (= :recorded (:status derived))
                                       (contains? terminal-classes (:class derived)))
                              (:class derived))
              class (or derived-class stored-class)
              classification-provenance
              (when derived-class
                {:method :derived-on-read
                 :classifier 'futon2.aif.run-ending-classification
                 :record-pin {:path (str file) :sha256 digest}
                 :stored-class stored-class
                 :derived-class derived-class})]
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
                (cond-> (assoc verified :status :ready :ending-class class)
                  classification-provenance
                  (assoc :classification-provenance classification-provenance))
                (defer :terminal-ending-class-absent
                       (assoc verified :classification-absence
                              {:reason (if classification :terminal-ending-class-unavailable
                                                          :classification-receipt-absent)
                               :recorded-class stored-class
                               :derived-on-read (when derived
                                                  (select-keys derived
                                                               [:status :class :kind :missing]))
                               :completed true
                               :failure (select-keys (:failure record)
                                                     [:kind :stage :error])}))))))))
    (catch Exception e
      (defer :run-record-unreadable {:message (.getMessage e)}))))
