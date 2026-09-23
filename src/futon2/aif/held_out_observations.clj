(ns futon2.aif.held-out-observations
  "Pure hygiene boundary for a preregistered held-out observation window."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.held-out-split :as split]
            [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(import (java.nio.file Files) (java.security MessageDigest))

(def schema :wm/eig-held-out-observations-v1)
(def disposition 'HELD-OUT-OBSERVATIONS-COLLECTED)

(defn- parse-instant [value]
  (when (string? value)
    (try
      (java.time.Instant/parse value)
      (catch java.time.format.DateTimeParseException _ nil))))

(defn- valid-row?
  [declaration {:keys [run-id target recorded-at close-sha256 outcome-class]}]
  (let [observed-at (parse-instant recorded-at)
        registered-at (parse-instant (:registered-at declaration))]
    (and (string? run-id)
         (= (:ticket/id declaration) target)
         observed-at
         registered-at
         (.isAfter observed-at registered-at)
         (boolean (re-matches #"[0-9a-f]{64}" (or close-sha256 "")))
         (contains? (set (:outcome-classes declaration)) outcome-class))))


(defn- read-mapping
  "The registered outcome-class mapping (declared, never inferred here)."
  []
  (edn/read-string (slurp (io/resource "wm/eig/held-out-outcome-class-mapping.edn"))))

(defn- sha256-of [f]
  (let [bytes (Files/readAllBytes (.toPath (io/file f)))]
    (apply str (map #(format "%02x" %)
                    (.digest (java.security.MessageDigest/getInstance "SHA-256") bytes)))))

(defn- record-outcome
  "The run record's own recorded outcome, read from its decision outcome."
  [record]
  (let [m (re-find #":outcome :([a-z-]+)" (pr-str record))]
    (some-> m second keyword)))

(defn- record-recorded-at
  "The record's own recorded-at (its earliest durable timestamp), read from
  the record's :recorded-at when present, else the latest of its phase
  timestamps (a record is generated at its close)."
  [record]
  (or (some->> (re-find #":recorded-at \"([^\"]+)\"" (pr-str record)) second)
      (->> (re-seq #":at \"([^\"]+)\"" (pr-str record))
           (map second)
           sort
           last)))

(defn- verify-source!
  "Typed provenance check: the row's source file exists, its digest matches,
   and the row's own fields agree with the record's contents. Returns a
   reason keyword when the tie fails; nil when it holds."
  [{:keys [run-id target recorded-at outcome-class source]}]
  (cond
    (not (and (map? source) (string? (:path source)) (string? (:sha256 source))))
    :no-source
    (not (.exists (io/file (:path source))))
    :source-missing
    (not= (:sha256 source) (sha256-of (:path source)))
    :source-digest-mismatch
    :else
    (let [text (slurp (:path source))
          record (try (edn/read-string text) (catch Exception _ nil))
          record-run-id (second (re-find #":run/id \"([^\"]+)\"" text))
          record-target (or (second (re-find #":eligible-targets \[\"([^\"]+)\"\]" text))
                            (second (re-find #":selected-target \"([^\"]+)\"" text)))
          record-outcome-kw (record-outcome record)
          mapping (:mapping (read-mapping))
          mapped (get mapping record-outcome-kw)
          ;; the row's outcome-class must be what the mapping says for THIS
          ;; record's outcome — including the unclassified case
          outcome-ok? (if (nil? mapped)
                        (= (:unmapped-outcome (read-mapping)) outcome-class)
                        (= mapped outcome-class))]
      (cond
        (not= run-id record-run-id) :row-disagrees-with-source
        (not= target record-target) :row-disagrees-with-source
        (not outcome-ok?) :row-disagrees-with-source
        :else nil))))

(defn collect-window
  "Retain every supplied row and close only the first declared N distinct,
  valid, post-registration observations. Invalid and duplicate rows remain in
  :observations with a hygiene verdict; they never silently disappear."
  [declaration rows]
  (split/validate-v2 declaration)
  (let [seen (volatile! #{})
        retained (mapv (fn [row]
                         (let [duplicate? (contains? @seen (:run-id row))
                               _ (vswap! seen conj (:run-id row))
                               observed-at (parse-instant (:recorded-at row))
                               registered-at (parse-instant (:registered-at declaration))
                               source-reason (verify-source! row)
                               reason (cond
                                        duplicate? :duplicate-run-id
                                        (not= (:ticket/id declaration) (:target row)) :different-target
                                        (not (string? (:recorded-at row))) :missing-recorded-at
                                        (nil? observed-at) :malformed-recorded-at
                                        (nil? registered-at) :malformed-registration-instant
                                        (not (.isAfter observed-at registered-at)) :before-registration
                                        (not (re-matches #"[0-9a-f]{64}" (or (:close-sha256 row) ""))) :invalid-close-digest
                                        ;; an unmapped outcome never counts
                                        (:unmapped? row) :unclassified-outcome-not-counted
                                        ;; provenance: a row that cannot be tied to its record never counts
                                        (some? source-reason) source-reason
                                        (not (contains? (set (:outcome-classes declaration))
                                                        (:outcome-class row))) :unknown-outcome-class)]
                           (assoc row :hygiene (if (and (nil? reason)
                                                        (nil? source-reason)
                                                        (not (:unmapped? row))
                                                        (valid-row? declaration row))
                                                 :valid :invalid)
                                      :hygiene-reason reason)))
                       rows)
        valid (filterv #(= :valid (:hygiene %)) retained)
        n (get-in declaration [:window :next-n])
        closed? (>= (count valid) n)]
    (cond-> {:schema schema
             :split {:schema (:schema declaration)
                     :ticket/id (:ticket/id declaration)
                     :registered-at (:registered-at declaration)
                     :starting-point (:starting-point declaration)}
             :status (if closed? :closed :open)
             :required n
             :valid-count (count valid)
             :missing-count (max 0 (- n (count valid)))
             :observations retained
             :claims {:window-closed? closed?
                      :calibration-evidence-present? false
                      :restoration-accepted? false}}
      closed? (assoc :disposition disposition))))

;; ---------------------------------------------------------------------------
;; ⟨1⟩6 (claude-5 handoff): the rows come from real run records, with
;; provenance an auditor can re-check, and a row that cannot be tied to a
;; record cannot close the window.
;; ---------------------------------------------------------------------------
(defn rows-from-runs
  "One row per run record whose recorded decision target equals the
   declaration's :ticket/id. The outcome class comes from the REGISTERED
   mapping (held-out-outcome-class-mapping.edn) — never inferred; an
   unmapped outcome is :unclassified and does not count. Each row carries
   :source {:path :sha256} — the record file and its digest — so
   collect-window can verify the tie."
  ([] (rows-from-runs nil nil))
  ([declaration] (rows-from-runs declaration "data/wm-runs"))
  ([declaration run-root]
   (let [mapping-entry (read-mapping)
         mapping (:mapping mapping-entry)
         unmapped (:unmapped-outcome mapping-entry)
         ticket (:ticket/id declaration)
         files (->> (file-seq (io/file run-root))
                    (filter #(.isFile %))
                    (filter #(.endsWith (.getName %) ".edn"))
                    (filter #(.startsWith (.getName %) "tick-run-record-")))]
     (vec
      (for [f files
            :let [digest (sha256-of f)
                  text (slurp f)
                  record (try (edn/read-string text) (catch Exception _ nil))
                  target-match (and record
                                    (or (= ticket (second (re-find #":eligible-targets \[\"([^\"]+)\"\]" text)))
                                        (= ticket (second (re-find #":selected-target \"([^\"]+)\"" text)))))
                  outcome (record-outcome record)
                  mapped (get mapping outcome)]
            :when target-match]
        {:run-id (str (second (re-find #":run/id \"([^\"]+)\"" text)))
         :target ticket
         :recorded-at (record-recorded-at record)
         :close-sha256 digest
         :outcome-class (if (nil? mapped) unmapped mapped)
         :unmapped? (nil? mapped)
         :source {:path (.getPath f) :sha256 digest}})))))


