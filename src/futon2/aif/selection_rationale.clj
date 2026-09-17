(ns futon2.aif.selection-rationale
  "RE4 -- the selection/refusal rationale, written at decision time.

   WHY THIS EXISTS AND WHY IT IS NOT U39/U40. U39 and U40 PROJECT a rationale
   out of trace records after the fact; their design note says so in as many
   words (`u39_selection_retrospective.bb:14-15`, \\\"no new logging of what
   is already logged\\\"). That is enough to score history and not enough to make a
   run auditable: the projection is a later reader's reconstruction, it can
   drift from the producer, and it cannot record a decision the trace shape did
   not anticipate. This namespace writes the rationale in the same act that
   persists the decision -- one record per persisted decision, from the record
   that was just written, into the run store.

   TOTALITY IS THE POINT (worklist RE4 acceptance). `rationale-record` is a
   TOTAL function: for any argument whatsoever it returns a record. A decision
   it cannot read yields `:rationale/status :typed-absence` with a reason from
   the closed enum `absence-reasons`, and that record is written like any
   other. `emit!` has no branch on which it returns without writing. There
   is no `(when ...)` guard, no `or`-default and no swallowing `try` on this path:
   a store that cannot be written throws, because a rationale that silently
   did not land is the failure this row exists to exclude.

   ABSTENTIONS ARE DECISIONS. Under the cascade-only tick (SPEC
   flat-removal-and-cascade-decision H4, Joe's 2026-09-17 ruling) the
   decision is a cascade selection by
   `futon2.aif.policy/select-action-cascades` or a typed abstention
   `{:status :abstained :refusals [...]}`. Both are recorded as
   `:rationale/status :recorded`: the first with outcome `:selected`, the
   second with outcome `:abstained` and its refusals grouped by kind — an
   abstention is a readiness state, not an error.

   DETERMINISM. Every field is read from the trace record or from a file on
   disk. No wall clock is read here: `:rationale/at` is the record's own
   `:timestamp`, so re-emitting the same record twice produces the same bytes
   and a replay over a recorded field is reproducible.

   INPUT SHAPE. The argument is a persisted trace record (`trace/trace-record`
   output), which is what the live seam has in hand after `write-trace!` and
   what a replay reads back off disk. Live and replay therefore run the same
   code over the same shape."
  (:require [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def schema-version
  "Bumped when a field is added or its meaning changes. Readers that fold a
   store across versions must branch on this rather than on key presence.
   2 — the flat decision rationale (controller rank, runner-up over
       :ranked-actions, :policy-support-exclusions) is replaced by the
       cascade-grain rationale (SPEC flat-removal H4, 2026-09-17): the
       chosen cascade, its enacted first acting pattern, the posterior mass
       and beta, or the abstention's refusals grouped by kind."
  2)

(def default-store-dir
  "Live destination: beside `data/wm-trace/`, one file per persisted decision,
   named by the tick's own `:run/id` so a rationale joins its trace record and
   its tick receipt by equality rather than by timestamp comparison (the RUN11
   rule, `scripts/futon2/run_tick_once.clj:208-212`). `data/` is untracked
   (`.gitignore:49`); a run-store slice under
   `holes/labs/wm-contract/runs/<run-id>/` is assembled from here exactly as
   `wm-trace-s5.edn` was assembled from `data/wm-trace/`."
  (str (System/getProperty "user.home") "/code/futon2/data/wm-rationale"))

(def contract-path
  "The mathlib4 hole contract whose `:source :git-sha` is the pin RE3's
   contract-pin check reads (`checks/contract_authority_current.clj:12-13`,
   `:32`). RE3 found that no file in the run store carries this sha, which is
   why its ledger row is a typed absence; a rationale record carries it."
  "/home/joe/code/mathlib4/DarkTower/WarMachine/holes-contract.json")

(def absence-reasons
  "Closed enum. A record whose `:rationale/absence-reason` is outside this set
   is a producer defect, not a new kind of absence -- `defects` says so."
  #{:record-not-a-map
    :malformed-record-fields
    :no-decision
    :decision-not-cascade-or-abstention
    :empty-posterior})

(def outcomes
  "Closed enum for a legible decision: a selected cascade or an abstention."
  #{:selected :abstained})

;; ---------------------------------------------------------------------------
;; Typed reads. Each returns a present/absent map; none returns nil.

(defn contract-identity
  "The contract pin as of this decision. Total: any failure to read is typed,
   named and located, never a nil sha."
  ([] (contract-identity contract-path))
  ([path]
   (let [f (io/file path)]
     (if-not (.isFile f)
       {:status :absent :reason :contract-file-missing :path path}
       (let [parsed (try (json/parse-string (slurp f) true)
                         (catch Exception e {::error (ex-message e)}))]
         (cond
           (::error parsed)
           {:status :absent :reason :contract-unparseable :path path
            :detail (::error parsed)}

           (not (string? (get-in parsed [:source :git-sha])))
           {:status :absent :reason :contract-carries-no-source-sha :path path}

           :else
           {:status :present
            :git-sha (get-in parsed [:source :git-sha])
            :contract-id (:contract-id parsed)
            :path path}))))))

;; ---------------------------------------------------------------------------
;; Cascade-decision projections (SPEC flat-removal H4, 2026-09-17).

(defn- cascade-decision?
  [decision]
  (= :cascade-selection-posterior (get-in decision [:selection-law :applied])))

(defn- abstention?
  [decision]
  (and (map? decision) (= :abstained (:status decision))))

(defn- pattern-id
  [p]
  (if (map? p) (str (or (:id p) (:cascade-id p))) (str p)))

(defn- first-acting-pattern
  "The enacted step of a cascade candidate: the first element of its
   :precedence."
  [action]
  (when (and (map? action) (seq (:precedence action)))
    (pattern-id (first (:precedence action)))))

(defn- refusals-by-kind
  "Refusals grouped by kind, each with count and targets. Kinds come from the
   decision gate's closed set; a kind outside it is preserved verbatim — this
   is a record, not a validator."
  [refusals]
  (into (sorted-map)
        (map (fn [[kind rs]]
               [kind {:count (count rs)
                      :targets (mapv :target rs)}]))
        (group-by :kind (vec refusals))))

(defn- chosen-summary
  "The chosen cascade as the decision recorded it: its identity, its enacted
   first acting pattern, its posterior mass, beta and G."
  [decision]
  (let [chosen (:action decision)]
    {:cascade-id (str (or (:cascade-id chosen) (:id chosen)))
     :first-acting-pattern (first-acting-pattern chosen)
     :precedence-count (count (:precedence chosen))
     :chosen-action-mass (:chosen-action-mass decision)
     :beta (:beta decision)
     :G (:controller-score decision)
     :selection-boundary (:selection-boundary decision)}))

(defn- runner-up
  "The highest-posterior candidate that is not the chosen one. This is the
   quantity that makes a cascade decision legible: how close the field was."
  [decision]
  (let [posterior (get-in decision [:selection-law :posterior])
        chosen (:cascade-id (chosen-summary decision))]
    (if (and (map? posterior) (seq posterior))
      (let [entries (sort-by val > (seq posterior))
            rival (first (remove (fn [[id _]] (= (str id) chosen)) entries))]
        (if rival
          {:status :present
           :cascade-id (str (key rival))
           :posterior (val rival)
           :margin-over-chosen
           (if (and (number? (val rival))
                    (number? (:chosen-action-mass decision)))
             (- (double (:chosen-action-mass decision)) (double (val rival)))
             {:status :absent :reason :margin-not-computable})}
          {:status :absent :reason :no-rival-candidate}))
      {:status :absent :reason :no-recorded-posterior})))

(defn- reason-text
  "One sentence, built from the fields beside it. Prose only; every number in
   it is also a field, so no reader has to parse this string."
  [{:rationale/keys [outcome chosen candidate-set-size]}]
  (case outcome
    :selected
    (str "selected cascade " (pr-str (:cascade-id chosen))
         " enacting " (pr-str (:first-acting-pattern chosen))
         " at posterior mass " (pr-str (:chosen-action-mass chosen))
         " over " candidate-set-size " candidate(s)")
    :abstained
    "abstained — readiness, not an error — with refusals recorded by kind"
    (str "no legible outcome over " candidate-set-size " candidate(s)")))

(defn- record-id
  "Deterministic store identity. `:run/id` when the caller threaded one (RUN11);
   otherwise a name derived from the record's own timestamp, so an unattributed
   decision still lands under a stable name instead of overwriting its
   neighbour."
  [record]
  (or (:run/id record)
      (str "no-run-id-"
           (str/replace (str (:timestamp record "undated")) #"[^0-9A-Za-z]" "-"))))

(defn- record-date
  "Date part of the record's own timestamp; `\\\"undated\\\"` when there is none.
   Never today's date -- a wall-clock read here would make replay irreproducible."
  [record]
  (let [ts (str (:timestamp record))]
    (if (re-find #"^\d{4}-\d{2}-\d{2}" ts) (subs ts 0 10) "undated")))

;; ---------------------------------------------------------------------------
;; The producer.

(defn- absence-record
  [record reason extra]
  (merge
   {:rationale/schema-version schema-version
    :rationale/status :typed-absence
    :rationale/absence-reason reason
    :rationale/run-id (:run/id record)
    :rationale/tick-id (:timestamp record)
    :rationale/at (:timestamp record)
    :rationale/emitted-at-decision? true}
   extra))

(defn rationale-record
  "TOTAL. Build the rationale record for one persisted decision.

   The decision is a cascade decision or a typed abstention (SPEC
   flat-removal H4, 2026-09-17); anything else is a typed absence with a
   reason from `absence-reasons`. It never returns nil and never throws on
   the shape of its argument.

   Opts:
     :contract  -- a `contract-identity` map (defaults to reading the pin)
     :trace-path -- the trace file this decision was persisted to, recorded as
                    the join back to the record the rationale is about."
  ([record] (rationale-record record {}))
  ([record {:keys [contract trace-path]}]
   (let [contract (or contract (contract-identity))]
     (if-not (map? record)
       {:rationale/schema-version schema-version
        :rationale/status :typed-absence
        :rationale/absence-reason :record-not-a-map
        :rationale/at {:status :absent :reason :record-not-a-map}
        :rationale/emitted-at-decision? true
        :rationale/contract-sha contract
        :rationale/trace-path trace-path}
       (let [decision (:decision record)
             posterior (get-in decision [:selection-law :posterior])
             problems (:cascade-problems record)
             common {:rationale/contract-sha contract
                     :rationale/trace-path trace-path
                     :rationale/producer-contract (:producer-contract record)
                     :rationale/wm-git-sha (get-in record [:wm-version :git-sha])}]
         (cond
           (not (map? decision))
           (absence-record record :no-decision common)

           (and (not (cascade-decision? decision))
                (not (abstention? decision)))
           (absence-record record :decision-not-cascade-or-abstention common)

           (and (cascade-decision? decision)
                (not (and (map? posterior) (seq posterior))))
           (absence-record record :empty-posterior common)

           (abstention? decision)
           (assoc
            (merge common
                   {:rationale/schema-version schema-version
                    :rationale/status :recorded
                    :rationale/outcome :abstained
                    :rationale/run-id (:run/id record)
                    :rationale/tick-id (:timestamp record)
                    :rationale/at (:timestamp record)
                    :rationale/candidate-set-size 0
                    :rationale/abstained-refusals (refusals-by-kind
                                                   (:refusals decision))
                    :rationale/cascade-problems-refusals
                    (refusals-by-kind (:refusals problems))
                    :rationale/refused []
                    :rationale/refused-count 0
                    :rationale/emitted-at-decision? true
                    :rationale/derived-from
                    ["[:decision :status]" "[:decision :refusals]"
                     "[:cascade-problems :refusals]" "[:timestamp]" "[:run/id]"
                     "[:producer-contract]" "[:wm-version :git-sha]"
                     contract-path]})
            :rationale/text (reason-text {:rationale/outcome :abstained}))

           :else
           (let [base (merge
                       common
                       {:rationale/schema-version schema-version
                        :rationale/status :recorded
                        :rationale/outcome :selected
                        :rationale/run-id (:run/id record)
                        :rationale/tick-id (:timestamp record)
                        :rationale/at (:timestamp record)
                        :rationale/candidate-set-size (count posterior)
                        :rationale/chosen (chosen-summary decision)
                        :rationale/runner-up (runner-up decision)
                        :rationale/refused []
                        :rationale/refused-count 0
                        :rationale/selection-law (:selection-law decision)
                        :rationale/emitted-at-decision? true
                        :rationale/assertion
                        (str "this decision is re-selectable: at the next persisted decision "
                             "whose posterior contains the chosen cascade, the machine chooses "
                             "it again. A later decision that carries the chosen cascade among its "
                             "candidates and chooses otherwise REFUTES this record.")
                        :rationale/derived-from
                        ["[:decision :action]" "[:decision :chosen-action-mass]"
                         "[:decision :beta]" "[:decision :selection-law]"
                         "[:decision :selection-law :posterior]"
                         "[:timestamp]" "[:run/id]"
                         "[:producer-contract]" "[:wm-version :git-sha]"
                         contract-path]})]
             (assoc base :rationale/text (reason-text base)))))))))

;; ---------------------------------------------------------------------------
;; Validation. A defect here is a producer bug; `emit!` refuses to write one.

(defn defects
  "Vector of defect keywords for a candidate rationale record. Empty is valid."
  [r]
  (let [status (:rationale/status r)]
    (cond-> []
      (not (map? r)) (conj :not-a-map)
      (not= schema-version (:rationale/schema-version r)) (conj :wrong-schema-version)
      (not (contains? #{:recorded :typed-absence} status)) (conj :status-out-of-vocabulary)
      (not (contains? r :rationale/at)) (conj :missing-at)
      (not (contains? r :rationale/contract-sha)) (conj :missing-contract-sha)
      (not (true? (:rationale/emitted-at-decision? r))) (conj :not-marked-decision-time)
      (and (= :typed-absence status)
           (not (contains? absence-reasons (:rationale/absence-reason r))))
      (conj :absence-reason-out-of-vocabulary)
      (and (= :recorded status)
           (not (contains? outcomes (:rationale/outcome r))))
      (conj :outcome-out-of-vocabulary)
      (and (= :recorded status)
           (not (nat-int? (:rationale/candidate-set-size r))))
      (conj :candidate-set-size-not-a-count)
      (and (= :recorded status) (not (string? (:rationale/text r))))
      (conj :missing-text)
      (and (= :recorded status) (not (vector? (:rationale/refused r))))
      (conj :refused-not-a-vector))))

;; ---------------------------------------------------------------------------
;; The write. One record, one file, no conditional return.

(defn store-path
  "Where a record's rationale lands. Pure."
  [dir record]
  (str dir "/rationale-" (record-date record) "-" (record-id record) ".edn"))

(defn emit!
  "Write exactly one rationale record for RECORD into the run store and return
   the path. Unconditional: an illegible decision produces a typed-absence
   RECORD and that record is written. Throws `ex-info` with
   `:stage :selection-rationale` when the record is defective or the store
   cannot be written -- never returns without having written.

   Opts:
     :dir        -- store directory (default `default-store-dir`)
     :contract   -- pre-read `contract-identity` map
     :trace-path -- the trace file this decision was persisted to"
  ([record] (emit! record {}))
  ([record {:keys [dir] :as opts}]
   (let [dir (or dir default-store-dir)
         rationale (rationale-record record opts)
         ds (defects rationale)]
     (when (seq ds)
       (throw (ex-info "selection-rationale: refusing to write a defective record"
                       {:stage :selection-rationale :defects ds})))
     (let [path (store-path dir (if (map? record) record {}))]
       (try
         (io/make-parents path)
         (spit path (str (pr-str rationale) "\n"))
         (catch Exception e
           (throw (ex-info (str "selection-rationale: store write failed: " (ex-message e))
                           {:stage :selection-rationale :path path} e))))
       path))))

;; ---------------------------------------------------------------------------
;; Read side, for the retrospective.

(def ^:private tolerant-readers
  {:default (fn [tag value] {:rationale/edn-tag tag :rationale/value value})})

(defn read-store
  "Every rationale record under DIR, in `:rationale/at` order. Records that do
   not parse are returned as typed unreadables rather than dropped."
  [dir]
  (let [d (io/file dir)
        files (if (.isDirectory d)
                (sort-by #(.getName ^java.io.File %)
                         (filter #(and (.isFile ^java.io.File %)
                                       (str/ends-with? (.getName ^java.io.File %) ".edn"))
                                 (.listFiles d)))
                [])]
    (->> files
         (mapv (fn [^java.io.File f]
                 (try (edn/read-string tolerant-readers (slurp f))
                      (catch Exception e
                        {:rationale/status :unreadable
                         :rationale/path (.getPath f)
                         :rationale/detail (ex-message e)}))))
         (sort-by #(str (:rationale/at %)))
         vec)))
