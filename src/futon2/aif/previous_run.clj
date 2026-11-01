(ns futon2.aif.previous-run
  "Requirements Q6 at the production runner: a run must not choose what was
  refused on its selection inputs, whichever earlier run refused it.

  The record stream. Every run appends two events to run-stream.edn in its
  run-record directory: :started (its id and recorded start instant) before
  selection, and :closed (its sealed terminal carrier) when its run record is
  persisted. The previous run is the :started event with the latest recorded
  start instant strictly before this run's, compared as Instants: not the
  filename, not the run id. Its :closed event supplies the previous choice,
  outcome and input digest; a run that started and never closed is a typed
  absence. The inner carrier does not parse run records. The outer selection reader
  prefers the immutable decision-chain index, with sealed tick fallback for
  histories predating that index.

  The choice is the structural reading-policy key
  (policy-prefix-admission/reading-policy-key), the identity Prefix-F, habit
  E, the flight writer and the decision gate use. A choice that is not a
  reading policy, or whose key does not match its action, is typed foreign
  or malformed and never compared.

  Enforcement happens before the one G selector: the decision computes the
  selection-input digest over what the selector consumes (the scored
  family, its F carriers, beta, the precision identity and the selector's
  other inputs). Every refused policy stays compared; its prediction class cannot
  be chosen. The predecessor carrier retains every refused run in the stream, and
  the selector forbids those on the current digest for this task. With nothing left
  the decision abstains typed :repeat-choice-after-refusal, and that refusal is
  carried forward until the selection inputs change.

  Every carrier is sealed (value-digest under :sha256). A tampered carrier
  is not-recomputable, never a pass."
  (:require [futon2.aif.action-identity :as action-identity]
            [futon2.aif.cascade-selection :as selection]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.selection-record :as selection-record]
            [futon2.aif.policy-prefix-admission :as prefix-admission])
  (:import [java.time Instant]))

(def stream-name "run-stream.edn")
(def predecessor-schema :wm/q6-predecessor-v1)
(def terminal-schema :wm/q6-terminal-v1)
(def exclusion-schema :wm/q6-exclusion-v1)
(def inputs-schema :wm/q6-selection-inputs-v1)
(def canonical-inputs-schema :wm/q6-canonical-selection-inputs-v1)

(defn seal [value] (assoc value :sha256 (evidence/value-digest value)))

(defn intact? [value]
  (and (map? value) (string? (:sha256 value))
       (= (:sha256 value) (evidence/value-digest (dissoc value :sha256)))))

(defn stream-file [dir] (io/file dir stream-name))

(defn- instant [s]
  (when (string? s)
    (try (Instant/parse s) (catch Exception _ nil))))

;; -- the record stream --------------------------------------------------------

(defn read-stream
  "The run stream's events, or a typed unreadable stream naming the lines
  that did not parse as event maps. A missing stream is empty."
  [dir]
  (let [f (stream-file dir)]
    (if-not (.exists f)
      {:status :present :events []}
      (let [parsed (keep-indexed
                    (fn [i line]
                      (when-not (str/blank? line)
                        (let [e (try (edn/read-string line) (catch Exception _ ::bad))]
                          (if (and (map? e) (#{:started :closed} (:event e)))
                            {:event e}
                            {:line (inc i)}))))
                    (str/split-lines (slurp f)))
            bad (keep :line parsed)]
        (if (seq bad)
          {:status :unreadable :lines (vec bad)}
          {:status :present :events (mapv :event parsed)})))))

(defn- append! [dir event]
  (let [f (stream-file dir)]
    (io/make-parents f)
    (spit f (str (pr-str event) "\n") :append true)))

(defn cascade-identity
  "A refused policy's name in a history entry: the hash Lean's ids are taken from."
  [policy-key]
  (action-identity/digest policy-key))

(defn refusal-history
  "Every run started before NOW in EVENTS whose sealed terminal carrier says it
  was refused on a chosen policy, oldest first. Read from the sealed carriers only:
  a tampered terminal is no evidence of a refusal, and is counted, not trusted.
  Returns {:entries [{:cascade :input-digest :run-id :target}…] :unsealed n}."
  [events run-id now]
  (let [started (into {} (for [e events :when (= :started (:event e))]
                           [(:run/id e) (instant (:startedAt e))]))
        closed (for [e events
                     :let [at (get started (:run/id e))]
                     :when (and (= :closed (:event e)) (not= run-id (:run/id e)) at
                                (.isBefore ^Instant at now))]
                 [at (:q6 e)])
        {sealed true broken false} (group-by (comp boolean intact? second) closed)
        refused (filter (fn [[_ t]] (and (= :refused (get-in t [:outcome :outcome]))
                                         (= :present (get-in t [:choice :status]))
                                         (string? (:input-digest t))))
                        sealed)]
    {:entries (vec (for [[_ t] (sort-by (fn [[at _]] (.toEpochMilli ^Instant at)) refused)]
                     {:cascade (cascade-identity (get-in t [:choice :policy-key]))
                      :input-digest (:input-digest t)
                      :run-id (:run/id t)
                      :target (get-in t [:choice :target])}))
     :unsealed (count broken)}))

(defn predecessor
  "The sealed Q6 predecessor carrier for run RUN-ID started at STARTED-AT in
  DIR's run stream: {:status :present :previous <its sealed terminal
  carrier>} or a typed absence / not-recomputable naming why."
  [dir run-id started-at]
  (let [now (instant started-at)
        stream (read-stream dir)
        base {:schema predecessor-schema :run/id run-id :startedAt started-at
              :stream stream-name}
        found
        (cond
          (nil? now) {:status :not-recomputable :reason :current-start-unreadable}
          (not= :present (:status stream))
          {:status :not-recomputable :reason :run-stream-unreadable :lines (:lines stream)}
          :else
          (let [starts (filter #(= :started (:event %)) (:events stream))
                unreadable (remove #(instant (:startedAt %)) starts)
                earlier (filter #(and (not= run-id (:run/id %))
                                      (.isBefore ^Instant (instant (:startedAt %)) now))
                                (remove (set unreadable) starts))]
            (cond
              (seq unreadable)
              {:status :not-recomputable :reason :run-stream-start-unreadable
               :runs (mapv :run/id unreadable)}
              (empty? earlier)
              {:status :absent :reason :no-earlier-started-run}
              :else
              (let [latest (reduce (fn [a b] (if (.isAfter ^Instant (instant (:startedAt b))
                                                           (instant (:startedAt a)))
                                               b a))
                                   earlier)
                    at (instant (:startedAt latest))
                    tied (filter #(= at (instant (:startedAt %))) earlier)
                    closed (filter #(and (= :closed (:event %))
                                         (= (:run/id latest) (:run/id %)))
                                   (:events stream))
                    terminal (:q6 (first closed))
                    who {:previous-run/id (:run/id latest)
                         :previous-startedAt (:startedAt latest)}]
                (merge
                 who
                 (cond
                   (< 1 (count (set (map :run/id tied))))
                   {:status :not-recomputable :reason :previous-run-ambiguous
                    :runs (vec (sort (map :run/id tied)))}
                   (empty? closed)
                   {:status :absent :reason :previous-run-not-closed}
                   (< 1 (count closed))
                   {:status :not-recomputable :reason :previous-run-closed-twice}
                   (not (and (= terminal-schema (:schema terminal)) (intact? terminal)))
                   {:status :not-recomputable :reason :previous-terminal-carrier-tampered}
                   (not= [(:run/id latest) (:startedAt latest)]
                         [(:run/id terminal) (:startedAt terminal)])
                   {:status :not-recomputable :reason :previous-terminal-carrier-foreign}
                   :else
                   (cond-> {:status :present :previous terminal}
                     ;; Requirements Q8: the frontier this run persisted,
                     ;; the prior of the next run's refresh audit
                     (:q8-frontier (first closed))
                     (assoc :frontier (:q8-frontier (first closed))))))))))]
    (seal (cond-> (merge base found)
            (and now (= :present (:status stream)))
            (assoc :refusal-history (refusal-history (:events stream) run-id now))))))

(defn selection-from-record
  "Summarize the outer selection of a retained tick, independently of its
  inner work outcome. Chosen plus a recorded work decision means work ran."
  [record]
  (let [s (:outer/selection record)
        status (or (get-in record [:selection-outcome :status]) (get-in s [:outcome :status]))
        choice (:chosen-class s) pick (get-in s [:selection :selected :target])
        digest (get-in s [:choice-input-digest :sha256])]
    (cond
      (not (map? s)) {:status :absent :reason :previous-selection-unretained}
      (= :not-exercised status) {:status :not-exercised :reason :previous-selection-bypassed
                               :selection-outcome (:selection-outcome record)}
      (and (nil? digest) (not= selection-record/schema (:schema record)))
      {:status :absent :reason :previous-choice-input-digest-unretained}
      :else
      {:status :present :run/id (:run/id record) :chosen-class choice :pick pick
       :choice-input-digest digest
       :selection-outcome (or (:selection-outcome record) (:outcome s))
       :outcome (cond
                  (= :refused status) :refused
                  (= :abstained status) :abstained
                  (and (= :chosen status) (map? (:decision record))) :changed
                  :else {:status :absent :reason :selection-work-entry-unretained})})))

(defn- legacy-selection-predecessor
  "Read the predecessor chosen by the existing sealed run stream. This covers
  completed ticks only; outer refusals currently leave no tick record. No scan
  by filename, no stream append, and no fabricated first-click predecessor."
  [dir run-id started-at]
  (let [p (predecessor dir run-id started-at)]
    (if-not (= :present (:status p))
      (select-keys p [:status :reason :previous-run/id])
      (let [event (first (filter #(and (= :closed (:event %))
                                      (= (:previous-run/id p) (:run/id %)))
                                (:events (read-stream dir))))
            file (when (:record-file event) (io/file (:record-file event)))]
        (if-not (and file (.isFile file))
          {:status :absent :reason :previous-selection-record-missing :run/id (:previous-run/id p)}
          (try
            (let [record (edn/read-string {:default tagged-literal} (slurp file))]
              (if (and (= (:previous-run/id p) (:run/id record))
                       (= (:previous p) (get-in record [:q6 :terminal])))
                (assoc (selection-from-record record) :record-file (str file)
                       :predecessor-sha256 (:sha256 p) :scope :previous-completed-tick
                       :retention-gap {:status :absent :reason :refused-selection-left-no-record})
                {:status :not-recomputable :reason :previous-selection-record-identity-mismatch}))
            (catch Exception _ {:status :not-recomputable :reason :previous-selection-record-unreadable})))))))

(defn selection-predecessor
  "Prefer the outer decision stream, including refused and not-exercised runs.
  Historical tick fallback is only used where no selection stream exists."
  [dir run-id started-at]
  (let [p (selection-record/latest dir run-id started-at)]
    (cond
      (= :no-selection-record-history (:reason p))
      (legacy-selection-predecessor dir run-id started-at)
      (= :present (:status p))
      (try
        (let [record (:record p)
              summary (selection-from-record record)
              tick (:tick-record (selection-record/read-completion (:reference p)))]
          (cond-> (assoc summary :selection-record (:reference p) :scope :selection-decision :chain-length (:chain-length p))
            (and (:path tick) (= :chosen (get-in record [:selection-outcome :status])))
            (assoc :outcome :changed)))
        (catch Exception _ {:status :not-recomputable :reason :selection-completion-invalid}))
      :else p)))

(defn start!
  "Before selection: the Q6 predecessor of this run, then this run's
  :started event appended to DIR's stream. Returns the predecessor."
  [dir run-id started-at]
  (let [p (predecessor dir run-id started-at)]
    (append! dir {:event :started :run/id run-id :startedAt started-at})
    p))

(defn close!
  "When the run record is written: TERMINAL (this run's sealed terminal
  carrier) appended to DIR's stream with the record file it was written to,
  and FRONTIER (its sealed Q8 frontier) when it has one."
  ([dir terminal record-file] (close! dir terminal record-file nil))
  ([dir terminal record-file frontier]
   (append! dir (cond-> {:event :closed :run/id (:run/id terminal)
                         :record-file (str record-file) :q6 terminal}
                  frontier (assoc :q8-frontier frontier)))))

;; -- what the selector consumes -----------------------------------------------

(defn reading-key? [k]
  (and (vector? k) (= :wm/reading-policy-v1 (first k))))

(defn- stable-order
  "Order VALUES by their value digests: an order that is a function of the
  values alone. Sorting by pr-str read the ambient printer (*print-length*
  truncates, *print-namespace-maps* reshapes), so the same keys could digest
  differently in two processes (R18 sibling, 2026-10-26)."
  [values]
  (vec (sort-by evidence/value-digest values)))

(defn selection-inputs-digest
  "The digest of every input the one selector call consumes, taken over the
  whole scored family before any exclusion: each policy's structural key,
  G, G terms and F carrier; beta; the reading-precision identity (scorer,
  kernel, graph, readings, arrangements, preference authority); and the
  selector's other inputs (history pin, enactment fold, feedback, queue,
  novelty)."
  [ranked selector-opts beta precision-identity]
  (evidence/value-digest
   {:schema inputs-schema
    :policies (stable-order
               (map (fn [e] {:policy-key (get-in e [:action :policy-key])
                             :g (:G-cascade e)
                             :g-terms (get-in e [:certificate :g-terms])
                             :f-prefix (:f-prefix e)})
                    ranked))
    :beta beta
    :precision-identity precision-identity
    :selector-opts (select-keys selector-opts
                                [:policy-history-pin :enactment-fold :pattern-feedback
                                 :ticket-queue :ticket-queue-refusals :novelty-inputs])}))

(defn- decide-exclusion [predecessor digest]
  (let [prev (:previous predecessor)
        {:keys [choice outcome input-digest q6-refusal]} prev]
    (cond
      (nil? predecessor)
      {:status :not-recomputable :reason :predecessor-carrier-not-supplied}
      (not (and (= predecessor-schema (:schema predecessor)) (intact? predecessor)))
      {:status :not-recomputable :reason :predecessor-carrier-tampered}
      (not= :present (:status predecessor))
      {:status :not-recomputable :reason (:reason predecessor)}
      ;; the previous run itself refused to repeat: that refusal holds
      ;; until the selection inputs change
      (and q6-refusal (= digest (:input-digest q6-refusal)))
      {:status :excluded :reason :repeat-refusal-rearm-not-met
       :excluded-keys [(:excluded-key q6-refusal)]}
      (not= :present (:status outcome))
      {:status :not-recomputable :reason :previous-outcome-absent}
      (not= :refused (:outcome outcome))
      {:status :not-triggered :reason :previous-outcome-not-refused}
      (not= :present (:status choice))
      {:status :not-recomputable
       :reason (case (:status choice)
                 :foreign :previous-choice-foreign
                 :malformed :previous-choice-malformed
                 :previous-choice-absent)}
      (not (string? input-digest))
      {:status :not-recomputable :reason :previous-input-digest-absent}
      (not= input-digest digest)
      {:status :not-triggered :reason :selection-inputs-changed}
      :else
      {:status :excluded :reason :previous-choice-refused-on-same-inputs
       :excluded-keys [(:policy-key choice)]})))

(defn- exclusion-carrier
  [predecessor digest menu-count removed decided refused?]
  (seal (cond-> (merge {:schema exclusion-schema
                        :input-digest digest
                        :predecessor (select-keys predecessor
                                                  [:status :reason :sha256
                                                   :previous-run/id])
                        :menu menu-count
                        :removed removed}
                       decided)
          refused?
          (assoc :status :refused
                 :kind :repeat-choice-after-refusal
                 :rearm {:condition :selection-inputs-change
                         :input-digest digest
                         :excluded-key (first (:excluded-keys decided))}))))

(defn task-refusal-history
  "The retained refusals on TARGETS, oldest first, from an intact carrier only.
  Entries on other digests stay: the record is the history, and they forbid nothing."
  [predecessor targets]
  (if (intact? predecessor)
    (filterv #(contains? targets (:target %))
             (get-in predecessor [:refusal-history :entries]))
    []))

(defn exclusion
  "Q6 before the one selector: PREDECESSOR (the run's sealed predecessor carrier,
  or nil) against RANKED (the certified scored family). A refused policy stays
  compared, so nothing leaves RANKED. What is forbidden is the prediction class of
  every refusal on these inputs, not only the previous run's: with inputs unchanged,
  A refused then B refused does not free A. Returns {:carrier <sealed carrier>
  :forbidden {:keys … :classes …}}, with :refusal set when no class is left."
  [predecessor ranked selector-opts beta precision-identity]
  (let [digest (selection-inputs-digest ranked selector-opts beta precision-identity)
        decided (decide-exclusion predecessor digest)
        rows (map selection/selection-row ranked)
        targets (set (map #(str (get-in % [:action :target])) ranked))
        history (task-refusal-history predecessor targets)
        on-inputs (filterv #(= digest (:input-digest %)) history)
        key-of (into {} (for [{k :policy-key} rows :when k] [(cascade-identity k) k]))
        refused-keys (vec (distinct (concat (:excluded-keys decided)
                                            (keep (comp key-of :cascade) on-inputs))))
        barring? (or (= :excluded (:status decided)) (seq refused-keys))
        forbidden (when barring?
                    {:keys refused-keys
                     :classes (selection/forbidden-classes rows refused-keys)})
        refused? (and barring? (seq rows)
                      (every? #(selection/forbidden-row? forbidden %) rows))
        carrier (seal
                 (cond-> (merge {:schema exclusion-schema
                                 :input-digest digest
                                 :predecessor (select-keys predecessor
                                                           [:status :reason :sha256
                                                            :previous-run/id])
                                 :menu (count ranked)
                                 :refusal-history history
                                 ;; L5-2L-3: terminals in the stream that could not be
                                 ;; verified, as the predecessor carrier counted them;
                                 ;; Lean Q6 requires 0 (a corrupt line is an absence on
                                 ;; the path, not a smaller history).
                                 :refusal-history-unsealed (get-in predecessor [:refusal-history :unsealed])}
                                (dissoc decided :excluded-keys))
                   barring? (assoc :status :forbidden
                                   :forbidden-keys refused-keys
                                   :forbidden-classes (:classes forbidden)
                                   :forbidden-runs (mapv :run-id on-inputs))
                   (and barring? (not= :excluded (:status decided)))
                   (assoc :reason :refused-on-these-inputs)
                   refused? (assoc :status :refused
                                   :kind :repeat-choice-after-refusal
                                   :no-admissible-class true
                                   :rearm {:condition :selection-inputs-change
                                           :input-digest digest
                                           :excluded-key (first refused-keys)})))]
    (cond-> {:carrier carrier}
      forbidden (assoc :forbidden forbidden)
      refused? (assoc :refusal carrier))))

(defn canonical-selection-inputs-digest
  "Digest the whole canonical population before Q6 exclusion. The draw is
  sealed entropy for resolving a tie, not an input to the rearm identity."
  [sealed-menu frontier]
  (evidence/value-digest
   {:schema canonical-inputs-schema
    :menu-pin (:menu-pin sealed-menu)
    :frontier-pin (:frontier-pin frontier)
    :policy-keys (stable-order (map #(get-in % [:action :policy-key])
                                    (:entries sealed-menu)))}))

(defn canonical-exclusion
  "Q6 for canonical selection. Preserve the sealed menu/frontier and return
  the keys the selector must omit. A refusal is present only when exclusion
  removes the last scored canonical entry."
  [predecessor sealed-menu frontier]
  (let [digest (canonical-selection-inputs-digest sealed-menu frontier)
        decided (decide-exclusion predecessor digest)
        excluded (set (:excluded-keys decided))
        entries (:entries sealed-menu)
        by-id (into {} (map (juxt :policy-id identity)) (:entries frontier))
        removed (count (filter #(contains? excluded
                                           (get-in % [:action :policy-key]))
                               entries))
        scored? (fn [entry] (= :scored (:status (get by-id (:policy-id entry)))))
        had-scored? (some scored? entries)
        scored-left? (some (fn [entry]
                             (and (not (contains? excluded
                                                  (get-in entry [:action :policy-key])))
                                  (scored? entry)))
                           entries)
        refused? (and (= :excluded (:status decided)) had-scored?
                      (not scored-left?))
        carrier (exclusion-carrier predecessor digest (count entries) removed
                                   decided refused?)]
    (cond-> {:carrier carrier :excluded-keys excluded}
      refused? (assoc :refusal carrier))))

;; -- this run's terminal carrier ----------------------------------------------

(defn choice
  "This run's choice: the selected reading policy's structural key, or a
  typed absence, foreign or malformed identity."
  [decision]
  (let [action (:action decision)
        k (when (map? action) (:policy-key action))]
    (cond
      (not (map? action)) {:status :absent :reason :chose-nothing}
      (not (reading-key? k)) {:status :foreign :reason :not-a-reading-policy-identity
                              :target (:target action)}
      (not= k (prefix-admission/reading-policy-key action))
      {:status :malformed :reason :policy-key-does-not-match-action}
      :else {:status :present :policy-key k :target (str (:target action))
             :policy-id (:id action)})))

(def ^:private lean-outcomes
  "Close outcomes that are a Requirements.RunOutcome."
  {:grounded-change :changed :grounded-progress :changed :changed :changed
   :grounded-no-change :already-satisfied :already-satisfied :already-satisfied
   :question :question
   :guardrail-refusal :refused :refused :refused
   :timed-out :timed-out :timeout :timed-out})

(defn typed-outcome
  "The run's terminal outcome. A typed refusal after selection (DOWNSTREAM,
  e.g. :reading-policy-execution-semantics-owed) or the decision's own Q6
  refusal is :refused; a decision that chose nothing is :abstained; a close
  outcome is mapped to the RunOutcome vocabulary, else :invalid."
  [outcome downstream decision]
  (cond
    (map? downstream)
    {:status :present :outcome :refused :kind (:kind downstream)}
    (= :refused (get-in decision [:selection-certificate :q6 :status]))
    {:status :present :outcome :refused :kind :repeat-choice-after-refusal}
    (and (not (map? (:action decision))) (= :abstained (:status decision)))
    {:status :present :outcome :abstained :raw outcome}
    (contains? lean-outcomes outcome)
    {:status :present :outcome (lean-outcomes outcome) :raw outcome}
    (some? outcome) {:status :present :outcome :invalid :raw outcome}
    :else {:status :absent :reason :terminal-outcome-absent}))

(defn terminal
  "This run's sealed terminal carrier: choice, typed outcome, the
  selection-input digest its decision computed before selecting, and its own
  Q6 refusal when it refused to repeat."
  [{:keys [run-id started-at decision outcome downstream valence-carry focus enactment]}]
  (let [q6 (get-in decision [:selection-certificate :q6])
        digest (when (intact? q6) (:input-digest q6))]
    (seal (cond-> {:schema terminal-schema
                   :run/id run-id
                   :startedAt started-at
                   :choice (choice decision)
                   :outcome (typed-outcome outcome downstream decision)
                   :input-digest digest}
            valence-carry (assoc :valence-carry valence-carry)
            ;; C5: presence and absence both cross the sealed terminal.  A
            ;; missing focus must not become a nil that a later discovery
            ;; silently mistakes for no predecessor.
            true (assoc :focus (or focus {:status :absent
                                           :reason :focus-not-retained}))
            ;; C5: the ordinary route has no pattern enactment producer yet.
            ;; Preserve that fact as a typed absence, never as a nil a later
            ;; habit reader could mistake for a no-enactment success.
            true (assoc :enactment (or enactment
                                       {:status :absent
                                        :reason :ordinary-pattern-enactment-not-observed}))
            (and digest (= :refused (:status q6)))
            (assoc :q6-refusal {:input-digest digest
                                :excluded-key (get-in q6 [:rearm :excluded-key])})))))

(def ^:dynamic *predecessor*
  "Bound by full-loop-runner/run-opportunity! to the run's sealed
  predecessor carrier, read once before selection. The cascade decision
  reads it; unbound (nil) is :predecessor-carrier-not-supplied."
  nil)
