(ns futon2.aif.held-out-observations
  "Pure hygiene boundary for a preregistered held-out observation window."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.held-out-split :as split]
            [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(import (java.nio.file Files))

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

(def ^:dynamic *repo-root*
  "The checkout a row's repo-relative :source path resolves against. Rows
   store repo-RELATIVE paths so the resource stays portable and its claims
   stay checkable; resolution needs a root, and the reader's working
   directory is not one -- verifying from the serving JVM (cwd futon3c)
   marked every row :source-missing (claude-5, reviewing 61c573a8)."
  "/home/joe/code/futon2")

(defn- resolve-source [path]
  (let [f (io/file path)]
    (if (.isAbsolute f) f (io/file *repo-root* path))))

(defn- read-record
  "The run record, parsed. Records carry tagged literals, so the reader needs
   a default; nil when it will not parse at all."
  [file]
  (try
    (edn/read-string {:default (fn [_tag value] value)} (slurp file))
    (catch Exception _ nil)))

;; The four fields below are read STRUCTURALLY. They were read by regex over
;; (pr-str record) -- over a megabyte of printed structure in which :outcome
;; and :recorded-at each occur many times, so the row took whichever happened
;; to print first rather than the run's own (claude-5, reviewing 61c573a8).

(defn- record-run-id [record] (:run/id record))

(defn- record-target
  "The recorded decision's target -- the field the split's own locator names
   as deciding membership."
  [record]
  (get-in record [:selection-event :target]))

(defn- record-instant
  "The run's START. The declaration holds out runs recorded after its
   registration instant; a run that was already UNDERWAY at registration
   could have had its outcome determined before it, so the start is the
   conservative boundary and the close is not."
  [record]
  (:startedAt record))

(defn- record-outcome
  "The run's ending, as the runner records it: the route edge into
   FULL_LOOP_CLOSE carries it under :via."
  [record]
  (some->> (:route record)
           (filter (fn [edge] (= "FULL_LOOP_CLOSE" (:toNode edge))))
           first
           :via))

(defn- verify-source!
  "Typed provenance check: the row's source file exists, its digest matches,
   and the row's own fields agree with the record's contents. Returns a
   reason keyword when the tie fails; nil when it holds.

   :recorded-at is checked too. It had been the one field a row could state
   freely, and it is the field that decides membership -- a doctored instant
   moves a run that happened BEFORE registration into the window, which is
   the single thing the held-out split exists to prevent (claude-5,
   reviewing 61c573a8)."
  [{:keys [run-id target recorded-at outcome-class source]}]
  (let [file (when (and (map? source) (string? (:path source)))
               (resolve-source (:path source)))]
    (cond
      (not (and (map? source) (string? (:path source)) (string? (:sha256 source))))
      :no-source
      (not (.exists file))
      :source-missing
      (not= (:sha256 source) (sha256-of file))
      :source-digest-mismatch
      :else
      (let [record (read-record file)
            mapping-entry (read-mapping)
            mapped (get (:mapping mapping-entry) (record-outcome record))
            ;; the row's outcome-class must be what the mapping says for THIS
            ;; record's outcome -- including the unclassified case
            outcome-ok? (if (nil? mapped)
                          (= (:unmapped-outcome mapping-entry) outcome-class)
                          (= mapped outcome-class))]
        (cond
          (nil? record) :source-unreadable
          (not= run-id (record-run-id record)) :row-disagrees-with-source
          (not= target (record-target record)) :row-disagrees-with-source
          (not= recorded-at (record-instant record)) :row-disagrees-with-source
          (not outcome-ok?) :row-disagrees-with-source
          :else nil)))))

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
         root (io/file run-root)
         root-abs (.getPath (if (.isAbsolute root) root (io/file *repo-root* run-root)))
         files (->> (file-seq (io/file root-abs))
                    (filter (fn [f] (.isFile f)))
                    (filter (fn [f] (.endsWith (.getName f) ".edn")))
                    (filter (fn [f] (.startsWith (.getName f) "tick-run-record-")))
                    (sort-by (fn [f] (.getName f))))]
     (vec
      (for [f files
            :let [record (read-record f)]
            :when (and record (= ticket (record-target record)))
            :let [mapped (get mapping (record-outcome record))
                  ;; repo-RELATIVE, so the resource stays portable and an
                  ;; auditor can re-verify it from any checkout
                  rel (let [full (.getPath f)
                            prefix (str *repo-root* "/")]
                        (if (.startsWith full prefix) (subs full (count prefix)) full))]]
        {:run-id (record-run-id record)
         :target ticket
         :recorded-at (record-instant record)
         :close-sha256 (sha256-of f)
         :outcome-class (if (nil? mapped) unmapped mapped)
         :unmapped? (nil? mapped)
         :source {:path rel :sha256 (sha256-of f)}})))))


