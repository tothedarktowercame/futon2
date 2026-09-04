(ns futon2.aif.selection-rationale
  "RE4 -- the selection/refusal rationale, written at decision time.

   WHY THIS EXISTS AND WHY IT IS NOT U39/U40. U39 and U40 PROJECT a rationale
   out of trace records after the fact; their design note says so in as many
   words (`u39_selection_retrospective.bb:14-15`, \"no new logging of what is
   already logged\"). That is enough to score history and not enough to make a
   run auditable: the projection is a later reader's reconstruction, it can
   drift from the producer, and it cannot record a decision the trace shape did
   not anticipate. This namespace writes the rationale in the same act that
   persists the decision -- one record per persisted decision, from the record
   that was just written, into the run store.

   TOTALITY IS THE POINT (worklist RE4 acceptance). `rationale-record` is a
   TOTAL function: for any argument whatsoever it returns a record. A decision
   it cannot read yields `:rationale/status :typed-absence` with a reason from
   the closed enum `absence-reasons`, and that record is written like any
   other. `emit!` has no branch on which it returns without writing. There is
   no `(when ...)` guard, no `or`-default and no swallowing `try` on this path:
   a store that cannot be written throws, because a rationale that silently
   did not land is the failure this row exists to exclude.

   REFUSALS ARE DECISIONS. `futon2.aif.policy/select-action` refuses by
   returning `{:action :abstain :reason ...}` (documented at policy.clj:736-739;
   the two branches at policy.clj:813-817 and policy.clj:843-847), and the
   admission stage refuses separately into `:policy-support-exclusions`. Both
   are recorded: the first as `:rationale/outcome :refused`, the second as
   entries in `:rationale/refused` on every record regardless of outcome.

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
   store across versions must branch on this rather than on key presence."
  1)

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
    :no-outcome-and-no-refusals
    :empty-candidate-set})

(def outcomes
  "Closed enum for a legible decision."
  #{:selected :refused})

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

(defn- action-key
  "Categorical identity of an action, the key U39/U40 join on
   (`u39_selection_retrospective.bb:132`)."
  [a]
  (when (map? a) [(:type a) (:target a)]))

(defn- ranking-index
  "Action key -> BEST (lowest) controller rank. The join key is not unique on
   real fields -- the 2026-09-01-s5 records carry `[:learn-action-class nil]`
   six times in a 145-entry ranking -- so `into {}`'s last-wins would make the
   recorded rank depend on emission order. Minimum is stated and stable, and
   `:key-occurrences` beside it says when the reading is ambiguous."
  [record]
  (reduce (fn [m e]
            (if-let [k (action-key (:action e))]
              (update m k (fn [prev] (if (and prev (<= prev (:rank e))) prev (:rank e))))
              m))
          {}
          (get-in record [:decision :controller-ranking])))

(defn- candidates-by-key
  "Action key -> its FIRST scored entry, for the same reason."
  [record]
  (reduce (fn [m e]
            (if-let [k (action-key (:action e))]
              (if (contains? m k) m (assoc m k e))
              m))
          {}
          (:ranked-actions record)))

(defn- key-occurrences [record k]
  (count (filter #(= k (action-key (:action %))) (:ranked-actions record))))

(defn- controller-score-tie
  "How many candidates share the chosen candidate's controller score, and over
   which ranks. Without this a reader takes \"controller rank 123 of 145\" for a
   large score gap; on the s5 records ranks 120-125+ carry the SAME score to
   sixteen digits and the rank is a tie-break position, not a margin."
  [record score]
  (if-not (number? score)
    {:status :absent :reason :chosen-has-no-controller-score}
    (let [tied (filter #(= score (:controller-score %)) (:ranked-actions record))
          ranks (keep :rank tied)]
      {:status :present
       :count (count tied)
       :rank-band (if (seq ranks) [(apply min ranks) (apply max ranks)]
                      {:status :absent :reason :tied-entries-carry-no-rank})})))

(defn- countable
  "Size of a collection field, or a typed absence when the field is not a
   collection at all. Returning 0 for a malformed field would report an empty
   candidate set where the record is simply not a record."
  [x]
  (if (coll? x) (count x) {:status :absent :reason :field-not-a-collection}))

(defn- refusal-entries
  "Admission-stage refusals, recorded on every rationale regardless of outcome.
   `futon2.aif.policy` never scores these -- they are excluded before ranking
   -- so `:scored?` is false and stated, not inferred by a reader."
  [record]
  (mapv (fn [x]
          {:action (select-keys (:action x) [:type :target])
           :stage :admission
           :reason (:reason x)
           :scored? false})
        (:policy-support-exclusions record)))

(defn- chosen-summary
  "The chosen candidate as the ranking and the score table saw it. The
   controller rank is read from the ranking rather than from `[:decision :rank]`
   because the strategic selector may REPLACE the controller's choice
   (war_machine.clj:6362-6364) and leave `:rank` describing the controller head
   it replaced."
  [record chosen]
  (let [k (action-key chosen)
        entry (get (candidates-by-key record) k)
        rank (get (ranking-index record) k)]
    {:action (select-keys chosen [:type :target])
     :controller-rank (if rank rank {:status :absent :reason :chosen-absent-from-controller-ranking})
     :G-core (if (number? (:G-core entry)) (:G-core entry)
                 {:status :absent :reason :chosen-absent-from-ranked-actions})
     :controller-score (if (number? (:controller-score entry)) (:controller-score entry)
                           {:status :absent :reason :chosen-absent-from-ranked-actions})
     :controller-score-tie (controller-score-tie record (:controller-score entry))
     :key-occurrences (key-occurrences record k)
     :mission-value-factor (:mission-value-factor chosen)}))

(defn- runner-up
  "The controller's own head when it is not the chosen candidate. This is the
   quantity that makes the S5 records legible: the head and the choice differ,
   and by how much is the first thing a retrospective asks."
  [record chosen]
  (let [head (first (get-in record [:decision :controller-ranking]))
        head-key (action-key (:action head))
        chosen-key (action-key chosen)]
    (cond
      (nil? head) {:status :absent :reason :no-controller-ranking}
      (= head-key chosen-key) {:status :absent :reason :chosen-is-controller-head}
      :else
      (let [by-key (candidates-by-key record)
            head-g (get-in by-key [head-key :G-core])
            chosen-g (get-in by-key [chosen-key :G-core])]
        {:status :present
         :action (select-keys (:action head) [:type :target])
         :controller-rank (:rank head)
         :G-core head-g
         :G-core-margin-over-chosen
         (if (and (number? head-g) (number? chosen-g))
           (- (double chosen-g) (double head-g))
           {:status :absent :reason :margin-not-computable})}))))

(defn- reason-text
  "One sentence, built from the fields beside it. Prose only; every number in
   it is also a field, so no reader has to parse this string."
  [{:rationale/keys [outcome chosen candidate-set-size refused-count
                     selection-boundary decision-reason]}]
  (case outcome
    :selected
    (str "selected " (pr-str (:action chosen))
         " at controller rank " (pr-str (:controller-rank chosen))
         " of " candidate-set-size " candidates"
         " at boundary " (pr-str selection-boundary)
         " with reason " (pr-str decision-reason)
         "; " refused-count " candidate(s) refused before ranking")
    :refused
    (str "refused to act over " candidate-set-size " candidates"
         " with reason " (pr-str decision-reason)
         " at boundary " (pr-str selection-boundary)
         "; " refused-count " candidate(s) refused before ranking")
    (str "no legible outcome over " candidate-set-size " candidates")))

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
  "Date part of the record's own timestamp; `\"undated\"` when there is none.
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
    :rationale/candidate-set-size (countable (:ranked-actions record))
    :rationale/refused (if (coll? (:policy-support-exclusions record))
                         (refusal-entries record)
                         {:status :absent :reason :field-not-a-collection})
    :rationale/refused-count (countable (:policy-support-exclusions record))
    :rationale/emitted-at-decision? true}
   extra))

(defn rationale-record
  "TOTAL. Build the rationale record for one persisted decision.

   Returns `:rationale/status :recorded` when the decision is legible (a chosen
   action or an explicit refusal, over a non-empty candidate set), and
   `:typed-absence` with a reason from `absence-reasons` otherwise. It never
   returns nil and never throws on the shape of its argument.

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
             chosen (:action decision)
             candidates (:ranked-actions record)
             exclusions (:policy-support-exclusions record)
             malformed? (or (not (or (nil? candidates) (coll? candidates)))
                            (not (or (nil? exclusions) (coll? exclusions))))
             outcome (cond (map? chosen) :selected
                           (some? chosen) :refused
                           :else nil)
             common {:rationale/contract-sha contract
                     :rationale/trace-path trace-path
                     :rationale/producer-contract (:producer-contract record)
                     :rationale/wm-git-sha (get-in record [:wm-version :git-sha])}]
         (cond
           malformed?
           {:rationale/schema-version schema-version
            :rationale/status :typed-absence
            :rationale/absence-reason :malformed-record-fields
            :rationale/run-id (:run/id record)
            :rationale/tick-id (:timestamp record)
            :rationale/at (:timestamp record)
            :rationale/candidate-set-size (countable candidates)
            :rationale/refused-count (countable exclusions)
            :rationale/emitted-at-decision? true
            :rationale/contract-sha contract
            :rationale/trace-path trace-path}

           (not (map? decision))
           (absence-record record :no-decision common)

           (nil? outcome)
           (absence-record record :no-outcome-and-no-refusals common)

           (empty? candidates)
           (absence-record record :empty-candidate-set
                           (assoc common :rationale/attempted-outcome outcome))

           :else
           (let [chosen-part (if (= :selected outcome)
                               (chosen-summary record chosen)
                               {:status :absent :reason :refused})
                 base (merge
                       common
                       {:rationale/schema-version schema-version
                        :rationale/status :recorded
                        :rationale/outcome outcome
                        :rationale/run-id (:run/id record)
                        :rationale/tick-id (:timestamp record)
                        :rationale/at (:timestamp record)
                        :rationale/candidate-set-size (count candidates)
                        :rationale/candidate-keys (mapv #(action-key (:action %)) candidates)
                        ;; The join key is not injective on real fields. A
                        ;; retrospective that assumed it was would silently
                        ;; conflate candidates; the count is on the record so it
                        ;; cannot be assumed.
                        :rationale/distinct-candidate-keys
                        (count (distinct (map #(action-key (:action %)) candidates)))
                        :rationale/chosen chosen-part
                        :rationale/runner-up (if (= :selected outcome)
                                               (runner-up record chosen)
                                               {:status :absent :reason :refused})
                        :rationale/refused (refusal-entries record)
                        :rationale/refused-count (count exclusions)
                        :rationale/decision-reason (:reason decision)
                        :rationale/selection-boundary (:selection-boundary decision)
                        :rationale/selection-law (:selection-law decision)
                        :rationale/tau (:tau decision)
                        :rationale/tau-source (:tau-source decision)
                        :rationale/selected-policy-id (:selected-policy-id decision)
                        :rationale/emitted-at-decision? true
                        :rationale/assertion
                        (str "this decision is re-selectable: at the next persisted decision "
                             "whose candidate set contains the chosen action, the machine chooses "
                             "it again. A later decision that carries the chosen action among its "
                             "candidates and chooses otherwise REFUTES this record.")
                        :rationale/derived-from
                        ["[:decision :action]" "[:decision :controller-ranking]"
                         "[:decision :reason]" "[:decision :selection-boundary]"
                         "[:decision :selection-law]" "[:ranked-actions]"
                         "[:policy-support-exclusions]" "[:timestamp]" "[:run/id]"
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
