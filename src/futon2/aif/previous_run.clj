(ns futon2.aif.previous-run
  "Q6 carry and enforcement support.

  A tick's run record carries what the PREVIOUS run record in the same
  run-record directory holds for Requirements Q6: the previous choice
  (target + precedence), the previous typed terminal outcome, and the
  previous selection-input digest. The lookup is explicit and typed: no
  previous record, an unreadable record, or a previous record that chose
  nothing each become their own typed absence, never nil and never a
  guessed value.

  Ordering: run ids are <date>-<random uuid>, so filenames of same-day
  clicks sort arbitrarily. The previous run is the one whose RECORDED start
  time (:startedAt — the instant persist-run-record! stamps on the record
  and the PROOF-2b terminal receipt cites as the run's own :at) is latest
  among the records before this run; runs that never started (no record,
  or a record without :startedAt) are excluded.

  Enforcement: when the previous run's typed outcome was a refusal and its
  selection-input digest matches this run's, the previous (target, cascade)
  pair is EXCLUDED from the admissible candidates and selection re-decides
  among the rest (binding *excluded-pair* around the judge); only when no
  admissible alternative remains does the click refuse, typed
  :repeat-choice-after-refusal."
  (:require [clojure.java.io :as io]
            [futon2.aif.run-record-io :as run-record-io]))

(def record-name-re #"tick-run-record-(\d{4}-\d{2}-\d{2})-([^.\s]+)\.edn$")

(defn read-record [^java.io.File file]
  (try
    {:status :present
     :record (run-record-io/read-record file)}
    (catch Exception e
      {:status :unreadable :error (ex-message e)})))

(defn- read-started-at
  "The record's :startedAt from its head line (one line read), falling back
  to the whole record for records written before head lines existed."
  [^java.io.File file]
  (try
    {:status :present :startedAt (:startedAt (run-record-io/read-head file))}
    (catch Exception e
      {:status :unreadable :error (ex-message e)})))

(defn- record-name [^java.io.File file] (.getName file))

(defn- candidate-files
  "Every run record in DIR whose name sorts strictly before the current
  run's own record name (tick-run-record-<run-id>.edn), newest date first."
  [dir current-run-id]
  (let [current-name (str "tick-run-record-" current-run-id ".edn")]
    (->> (io/file dir)
         file-seq
         (filter #(.isFile ^java.io.File %))
         (filter #(re-matches record-name-re (record-name %)))
         (filter #(neg? (compare (record-name %) current-name)))
         (sort-by record-name)
         reverse)))

(defn previous-record-file
  "The File of the run before CURRENT-RUN-ID: among the candidate records,
  the one with the latest recorded :startedAt, searching date groups from
  newest backwards so at most one day's records are parsed. A record with
  no :startedAt never started and is excluded. nil when none."
  [dir current-run-id]
  (loop [groups (group-by #(second (re-matches record-name-re (record-name %)))
                          (candidate-files dir current-run-id))
         dates (sort (keys groups))]
    (when (seq dates)
      (let [group (groups (last dates))
            started (keep (fn [^java.io.File f]
                            (let [read (read-started-at f)]
                              (when (= :present (:status read))
                                (let [at (:startedAt read)]
                                  (when (and at (not (map? at)))
                                    [(str at) (record-name f) f])))))
                          group)]
        (if (seq started)
          (let [[_ _ file] (last (sort-by (juxt first second) started))]
            file)
          (recur groups (butlast dates)))))))

(defn- previous-choice [record]
  (let [chosen (get-in record [:decision :chosen])]
    (if (and (map? chosen) (:target chosen) (not (:status chosen)))
      {:status :present
       :target (:target chosen)
       :precedence (vec (:precedence chosen))}
      {:status :absent :reason :previous-run-chose-nothing})))

(defn- previous-outcome [record]
  (let [receipt (:terminal-receipt record)
        outcome (when (map? receipt)
                  (or (when (contains? receipt :outcome) (:outcome receipt))
                      (:failure-kind receipt)))]
    (if (some? outcome)
      {:status :present :outcome outcome}
      {:status :absent :reason :previous-run-terminal-outcome-absent})))

(defn- previous-digest [record]
  (let [d (get-in record [:world-at-selection :selection-input-digest])]
    (if (and d (not (map? d)))
      {:status :present :digest d}
      {:status :absent :reason :previous-run-selection-input-digest-absent})))

(defn- previous-focus [record]
  (let [discovery (get-in record [:decision :selection-certificate
                                  :focus-receipt :discovery])]
    (if (and (map? discovery)
             (contains? #{:discovered :retained} (:status discovery))
             (some? (:focus discovery)))
      {:status :present
       :focus (:focus discovery)
       :as-of (:as-of discovery)
       :retained-evidence-as-of (:retained-evidence-as-of discovery)
       :source :previous-run-focus-receipt}
      {:status :absent :reason :previous-run-focus-absent})))

(defn carrier
  "The typed Q6 carrier for the run record found at FILE, or the typed
  absence when READ (from read-record) failed or FILE is nil."
  [file read]
  (cond
    (nil? file) {:schema :wm/previous-run-v1
                 :status :absent :reason :previous-run-record-absent}
    (not= :present (:status read))
    {:schema :wm/previous-run-v1
     :status :absent :reason :previous-run-record-unreadable
     :error (:error read)}
    :else (let [record (:record read)]
            {:schema :wm/previous-run-v1
             :status :present
             :run/id (:run/id record)
             :record-file (record-name file)
             :startedAt (:startedAt record)
             :choice (previous-choice record)
             :outcome (previous-outcome record)
             :input-digest (previous-digest record)
             :focus (previous-focus record)})))

(def ^:dynamic *carrier*
  "The typed previous-run carrier bound while the runner asks selection to
  decide. This is the same carrier persisted on the current run record."
  nil)

(defn focus-context
  "The focus context retained by a present previous-run carrier, in the
  shape focus-receipt/discover consumes. nil for every typed absence."
  [carrier]
  (when (= :present (get-in carrier [:focus :status]))
    (select-keys (:focus carrier)
                 [:focus :as-of :retained-evidence-as-of])))

(defn lookup
  "Default carrier lookup for the runner: the run before CURRENT-RUN-ID in
  DIR, ordered by recorded start time. DIR is the runner's run-record dir;
  hermetic tests rebind the data root or inject :previous-run-fn."
  [dir current-run-id]
  (let [file (previous-record-file dir current-run-id)]
    (carrier file (when file (read-record file)))))

(def ^:private refused-outcomes
  "Outcome vocabulary the exporter maps to `refused` (Requirements Q6)."
  #{:guardrail-refusal :refused :abstained})

(defn refused? [outcome-carrier]
  (and (= :present (:status outcome-carrier))
       (contains? refused-outcomes (:outcome outcome-carrier))))

(defn same-choice?
  "True when CHOICE-SUMMARY (full-loop-runner/chosen-summary shape) names
  the same (target, cascade) as the carrier's :choice."
  [carrier choice-summary]
  (and (= :present (get-in carrier [:choice :status]))
       (map? choice-summary)
       (= (str (:target choice-summary)) (str (get-in carrier [:choice :target])))
       (= (mapv #(or (:id %) %) (:precedence choice-summary))
          (mapv #(if (map? %) (:id %) %)
                (get-in carrier [:choice :precedence])))))

(defn exclusion-decision
  "Requirements Q6, decided at selection: when the previous run's typed
  terminal outcome was a refusal, the previous selection-input digest is
  present and equals CURRENT-DIGEST, and CHOICE-SUMMARY (the choice this
  tick is about to bank) names the same (target, cascade), return the
  exclusion evidence {:excluded-by :q6-repeat-after-refusal :pair ...
  :previous-run/id ... :selection-input-digest ...} — the runner re-decides
  with that pair excluded. nil when Q6 does not trigger."
  [carrier choice-summary current-digest]
  (when (and (map? carrier)
             (= :present (:status carrier))
             (refused? (:outcome carrier))
             (= :present (get-in carrier [:input-digest :status]))
             current-digest
             (= (get-in carrier [:input-digest :digest]) current-digest)
             (same-choice? carrier choice-summary))
    {:excluded-by :q6-repeat-after-refusal
     :pair {:target (get-in carrier [:choice :target])
            :precedence (get-in carrier [:choice :precedence])}
     :previous-run/id (:run/id carrier)
     :selection-input-digest current-digest}))

(def ^:dynamic *excluded-pair*
  "Bound by the full-loop runner around a Q6 re-decision: the judge must
  not admit the (target, cascade) pair it names. nil on ordinary clicks."
  nil)

(defn action-pair
  "The comparable (target, precedence-ids) pair of a ranked entry's action."
  [entry]
  (let [action (:action entry)]
    (when (map? action)
      {:target (:target action)
       :precedence (mapv #(if (map? %) (:id %) %) (:precedence action))})))

(defn excluded-ranked
  "Split RANKED (the judge's ranked candidate entries) around a bound
  *excluded-pair*: the entries whose action names that (target, cascade)
  are excluded, the rest are kept. {:kept [...] :excluded [...] :pair ...}
  with :excluded empty and :pair nil when nothing is bound or no entry
  matches."
  [ranked]
  (if-let [pair *excluded-pair*]
    (let [{kept false excluded true}
          (group-by (fn [entry]
                      (let [p (action-pair entry)]
                        (boolean (and p
                                      (= (str (:target pair)) (str (:target p)))
                                      (= (mapv str (:precedence pair))
                                         (mapv str (:precedence p)))))))
                    ranked)]
      {:kept (vec kept) :excluded (vec excluded) :pair pair})
    {:kept (vec ranked) :excluded [] :pair nil}))
