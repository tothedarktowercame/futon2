(ns futon2.aif.mission-c
  "C_mis — the mission-grain half of C (DESIGN-c-vector.md §1-§3, row U11).

   THE SEPARATION THIS NS EXISTS FOR. `futon2.aif.preferences/preferences` is
   C_int: preferences over the machine's own proprioceptive channels
   (`:sorry-count-norm`, `:support-coverage`, ...). `futon2.aif.c-vector` is a
   third thing again — preferences over the GOAL/HOLE corpus in substrate-2.
   Neither is a preference over the outcomes of the CLOCKED MISSION, which is
   what the recorded S1 refusal (\"a C over channels offered as a C over
   outcomes\") named. C_mis is that: the mission's own IDENTIFY completion
   criteria, read as a factored preference density over declared observables.

   THREE THINGS IT DOES, and it does no more (U11's acceptance):
   1. READS criteria — from an IDENTIFY ingest EDN (the hand exemplar
      `holes/labs/zaif-harness/runs/S4-identify-ingest.edn`) or from a mission
      doc's completion-criteria prose.
   2. BUILDS C_mis as `{observable -> (pref/c-distribution spec)}` — the SAME
      constructor C_int is built by, not a copy (pinned by
      `mission_c_test/c-distribution-is-the-pinned-constructor`).
   3. SCORES risk_mis under the v0 status-quo forward model.

   MEASURABILITY IS TWO TESTS, NOT ONE, and the reason a criterion fails is
   part of the record because the two failures want different repairs:
   - `:no-declared-measurement` — the criterion does not say how it is
     measured at all. The repair belongs to whoever writes the mission's
     IDENTIFY.
   - `:unresolved-observable` — the criterion DOES say how it is measured, but
     in prose (`:measurable-by \"count of live zaif decisions ...\"`,
     `:carrier \"U8's gate test\"`), which names a measurement for a HUMAN and
     no key this machine can read a current value of. The repair is a declared
     `:observable <keyword>` on the criterion, resolving into a supplied
     observable vocabulary.
   Both are `:status :unmeasurable`: a criterion that cannot be read
   contributes NOTHING to C_mis, and says so on every read (design §2's C130
   discipline). There is no silent massless criterion here, and no flat
   preference standing in for an absent one.

   MEASURED ON THE TWO FIXTURES U11 NAMES: 0 of 3 (zaif ingest) and 0 of 6
   (M-expressions-of-interest) criteria are measurable. That is this ns's
   first result, not a defect in it — the lifecycle's completion criteria are
   written for a reader, and the machine says exactly where the chain breaks.

   THE FORWARD-MODEL HOLE IS NAMED, NOT PAPERED (design §3). Q(o_k|π) at
   mission grain does not exist. v0 therefore scores CRITERION DISTANCE: the
   surprisal of the CURRENT measured value under C_k, i.e. Q(o|π) = status quo
   for every π. Consequence, stated here so no reader has to infer it from a
   record: **risk_mis is constant across the candidates of one tick.** It
   discriminates mission from non-mission actions and nothing finer. U12 is
   the row that measures whether that is enough."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.preferences :as pref])
  (:import [java.nio.file Files]
           [java.security MessageDigest]))

(def ^:const version 1)

(def default-spec
  "The spec a completion criterion gets when it declares none. A completion
   criterion is, by the lifecycle's own definition, a condition that should
   HOLD — `{:becomes 1}` is that as a c-distribution (design §2: \"'U-rows
   green' is literally a Bernoulli target\"). Never silently substituted: a row
   built from this carries `:spec-source :default-becomes-1`, so a declared
   spec and an assumed one are distinguishable on the record."
  {:becomes 1})

(def measurement-fields
  "Where a criterion may declare how it is measured, in precedence order.
   `:observable` is the only one that can RESOLVE (it names a key); the other
   two are prose fields the corpus actually uses — `:measurable-by` is the
   field design §2 names, `:carrier` is the field the hand exemplar's
   `:preferences/c` rows actually carry. Accepting both is what lets the
   record distinguish `:no-declared-measurement` from `:unresolved-observable`
   on the exemplar; collapsing them would report the zaif criteria as
   undeclared, which is not true of them."
  [:observable :measurable-by :carrier])

;; ---------------------------------------------------------------------------
;; Source pointers

(defn- line-of
  "1-based line number of the first line of `text` containing `needle`, or nil.
   Every criterion row carries a `file:line` pointer built from this; a nil
   line yields a bare path rather than a fabricated line number."
  [text needle]
  (when (and text needle)
    (->> (str/split-lines text)
         (keep-indexed (fn [i line] (when (str/includes? line needle) (inc i))))
         first)))

(defn- pointer
  [path line]
  (if line (str path ":" line) (str path)))

;; ---------------------------------------------------------------------------
;; Criterion rows

(defn- declared-measurement
  "The criterion's measurement declaration as `[field value]`, or nil."
  [entry]
  (some (fn [k]
          (let [v (get entry k)]
            (when (and (some? v) (not (and (string? v) (str/blank? v))))
              [k v])))
        measurement-fields))

(defn- resolve-measurement
  "Two-test measurability (see ns docstring). `observables` is the declared
   observable vocabulary — a set or map whose keys are observables this machine
   can read a current value of."
  [[field value] observables]
  (cond
    (nil? field)
    {:status :unmeasurable :reason :no-declared-measurement}

    (keyword? value)
    (if (contains? (set (if (map? observables) (keys observables) observables)) value)
      {:status :measurable :observable value
       :measurement-field field :measurement value}
      {:status :unmeasurable :reason :undeclared-observable
       :measurement-field field :measurement value})

    :else
    {:status :unmeasurable :reason :unresolved-observable
     :measurement-field field :measurement value}))

(defn criterion-row
  "One typed criterion. `entry` is `{:criterion :statement :observable
   :measurable-by :carrier :spec}` with everything but `:criterion` optional."
  [entry observables source]
  (let [spec-declared? (contains? entry :spec)
        base {:criterion (:criterion entry)
              :statement (:statement entry)
              :spec (if spec-declared? (:spec entry) default-spec)
              :spec-source (if spec-declared? :declared :default-becomes-1)
              :source source}]
    (merge base (resolve-measurement (declared-measurement entry) observables))))

;; ---------------------------------------------------------------------------
;; Reader (a) — the IDENTIFY ingest EDN

(defn criteria-from-ingest
  "Criteria from an IDENTIFY ingest map's `:preferences/c` (the hand exemplar's
   shape, S4-identify-ingest.edn). A missing key is a typed absence, not []."
  [ingest {:keys [observables path text]}]
  (let [entries (:preferences/c ingest)]
    (cond
      (nil? entries)
      {:version version :mission (:ingest/mission ingest) :source path
       :shape :ingest-edn :criteria []
       :status :absent :reason :no-preferences-c-key}

      (not (sequential? entries))
      {:version version :mission (:ingest/mission ingest) :source path
       :shape :ingest-edn :criteria []
       :status :absent :reason :malformed-preferences-c}

      :else
      {:version version :mission (:ingest/mission ingest) :source path
       :shape :ingest-edn :status :present
       :criteria (mapv (fn [e]
                         (criterion-row
                          e observables
                          (pointer path (line-of text (str (:criterion e))))))
                       entries)})))

;; ---------------------------------------------------------------------------
;; Reader (b) — a mission doc's completion criteria

(def ^:private heading-re #"(?i)^#{2,6}\s+completion\s+criteria\s*$")
(def ^:private inline-re #"(?i)^\*\*completion\s+criteria:?\*\*\s*(.*)$")
(def ^:private item-re #"^(\d+)\.\s+(.*)$")

(defn- positional-criterion
  "Completion criteria in a mission doc are not NAMED by their doc. Positional
   ids record that: `:criterion-3` claims only \"the third criterion at this
   pointer\", where a slug derived from the prose would claim a name the
   mission never gave it."
  [i]
  (keyword (str "criterion-" (inc i))))

(defn- section-items
  "Shape 1: a `### Completion criteria` heading followed by a numbered list.
   Continuation lines (indented, non-blank) join their item. Returns
   `[[line-number text] ...]`."
  [lines]
  (when-let [start (first (keep-indexed (fn [i l] (when (re-matches heading-re l) i)) lines))]
    (let [body (->> (drop (inc start) lines)
                    (take-while #(not (re-find #"^#{1,6}\s" %))))]
      (->> body
           (map-indexed (fn [i l] [(+ start 2 i) l]))
           (reduce (fn [acc [ln l]]
                     (cond
                       (re-matches item-re l)
                       (conj acc [ln (nth (re-matches item-re l) 2)])

                       (and (seq acc) (re-find #"^\s+\S" l))
                       (update-in acc [(dec (count acc)) 1] str " " (str/trim l))

                       :else acc))
                   [])
           (mapv (fn [[ln t]] [ln (str/trim t)]))))))

(defn- inline-items
  "Shape 2: a `**Completion criteria:** a; b; c` paragraph (the shape
   `M-zaif-harness-v1.md:76` uses). Continuation lines join; clauses split on
   `;`. Both shapes occur in the live corpus, so both are read and the record
   says which one answered."
  [lines]
  (when-let [start (first (keep-indexed (fn [i l] (when (re-find inline-re l) i)) lines))]
    (let [head (str/trim (or (second (re-find inline-re (nth lines start))) ""))
          tail (->> (drop (inc start) lines)
                    (take-while #(and (not (str/blank? %))
                                      (not (re-find #"^\s*\*\*" %))
                                      (not (re-find #"^#{1,6}\s" %))))
                    (map str/trim))
          para (str/trim (str/join " " (cons head tail)))]
      (when-not (str/blank? para)
        (->> (str/split (str/replace para #"\.\s*$" "") #";")
             (map str/trim)
             (remove str/blank?)
             (mapv (fn [c] [(inc start) c])))))))

(defn criteria-from-markdown
  "Criteria from a mission doc. Shape 1 (heading + numbered list) is tried
   first, then shape 2 (inline bold paragraph). Neither present is a typed
   absence — a mission with no stated completion criteria has NO C_mis, and
   that must not read as a mission whose criteria are all met."
  [text {:keys [observables path mission]}]
  (let [lines (str/split-lines text)
        [shape items] (if-let [xs (seq (section-items lines))]
                        [:markdown-numbered-list xs]
                        (if-let [xs (seq (inline-items lines))]
                          [:markdown-inline-paragraph xs]
                          [nil nil]))]
    (if (nil? shape)
      {:version version :mission mission :source path :shape :markdown
       :criteria [] :status :absent :reason :no-completion-criteria-section}
      {:version version :mission mission :source path :shape shape :status :present
       :criteria (vec (map-indexed
                       (fn [i [ln t]]
                         (criterion-row {:criterion (positional-criterion i)
                                         :statement t}
                                        observables (pointer path ln)))
                       items))})))

(defn- sha256-hex
  "Hex SHA-256 of the bytes handed in. Same shape the tripwire's source digest
   uses, so a recorded hash here and one recorded there compare directly."
  [^bytes bytes]
  (apply str (map #(format "%02x" (bit-and 0xff %))
                  (.digest (MessageDigest/getInstance "SHA-256") bytes))))

(defn read-criteria
  "Read a mission's completion criteria from `path`. `.edn` is read as an
   IDENTIFY ingest, anything else as a mission doc. An unreadable path is a
   typed absence with the exception message, never an empty criteria list.

   U15 (from U12 clause (a)): the result carries `:source-sha256`, the digest
   of THE BYTES THIS CALL DECODED — one read, hashed and parsed, so there is
   no window in which the hash and the criteria come from different content.
   Without it a record naming only the PATH cannot be replayed: the criteria
   arrive from a mutable file, and an edit between the run and the replay
   changes every number with nothing on the record to show it did. The field
   is present exactly when bytes were read: a path that is not there
   (`:source-not-found`) or that threw (`:source-unreadable`) carries no
   digest rather than a nil standing in for one."
  [path & {:keys [observables mission]}]
  (let [f (io/file path)]
    (if-not (.exists f)
      {:version version :mission mission :source (str path) :criteria []
       :status :absent :reason :source-not-found}
      (try
        (let [bytes (Files/readAllBytes (.toPath f))
              ;; UTF-8 rather than the platform charset because that is what
              ;; `slurp` (clojure.java.io's default encoding) decoded with
              ;; before this read was split into bytes-then-decode.
              text (String. ^bytes bytes "UTF-8")]
          (assoc (if (str/ends-with? (str path) ".edn")
                   (criteria-from-ingest (edn/read-string text)
                                         {:observables observables :path (str path) :text text})
                   (criteria-from-markdown text
                                           {:observables observables :path (str path)
                                            :mission mission}))
                 :source-sha256 (sha256-hex bytes)))
        (catch Exception e
          {:version version :mission mission :source (str path) :criteria []
           :status :absent :reason :source-unreadable :message (ex-message e)})))))

;; ---------------------------------------------------------------------------
;; C_mis — the factored density

(defn c-mis
  "C_mis as the factored density `{observable -> (pref/c-distribution spec)}`,
   built by the same constructor C_int is built by.

   `:weights` default to uniform over the MEASURABLE criteria and sum to 1.
   Uniform-over-measurable rather than uniform-over-all because an unmeasurable
   criterion must contribute nothing — including nothing to the denominator.
   `:criterion-weights` overrides per criterion and must cover every measurable
   criterion; a partial override throws rather than filling gaps.

   Every read carries `:unmeasurable` — the typed records, possibly empty."
  [{:keys [criteria mission source status reason] :as read-result}
   & {:keys [temperature criterion-weights]
      :or {temperature pref/default-c-temperature}}]
  (let [measurable (filterv #(= :measurable (:status %)) criteria)
        unmeasurable (mapv #(select-keys % [:criterion :status :reason :statement
                                            :measurement-field :measurement :source])
                           (remove #(= :measurable (:status %)) criteria))
        n (count measurable)
        weights (if criterion-weights
                  (reduce (fn [acc {:keys [criterion]}]
                            (if-let [w (get criterion-weights criterion)]
                              (assoc acc criterion (double w))
                              (throw (ex-info "c-mis: :criterion-weights covers no weight for a measurable criterion"
                                              {:criterion criterion
                                               :declared (set (keys criterion-weights))}))))
                          {} measurable)
                  (into {} (map (fn [{:keys [criterion]}] [criterion (/ 1.0 n)]) measurable)))]
    (cond-> {:version version
             :mission mission
             :source source
             :temperature temperature
             :criteria-status (or status (when reason :absent) :present)
             :criterion-count (count criteria)
             :measurable-count n
             :weight-basis (if criterion-weights :declared :uniform-over-measurable)
             :factors (into {} (map (fn [{:keys [observable spec]}]
                                      [observable (pref/c-distribution spec :temperature temperature)])
                                    measurable))
             :observable-of (into {} (map (juxt :criterion :observable) measurable))
             :spec-of (into {} (map (juxt :criterion :spec) measurable))
             :weights weights
             :unmeasurable unmeasurable
             :read read-result}
      ;; Present-only: an absence reason appears when there is one, never as nil.
      reason (assoc :criteria-reason reason))))

(defn- term
  "One criterion's ln C_k(o_k), typed. With no `outcome-semantics` this is the
   shipped call to `pref/log-preference` wrapped in a `:present` map and cannot
   refuse; with an arm named it is `pref/log-preference-under`, which refuses on
   a value the arm declines to read (U16). Keeping both on one shape means the
   composition below has one code path, not two."
  [factors observable value outcome-semantics]
  (if outcome-semantics
    (pref/log-preference-under (get factors observable) value outcome-semantics)
    {:status :present :log-c (pref/log-preference (get factors observable) value)}))

(defn log-c-mis
  "ln C_mis(o) in nats — the weighted log-sum composition of design §2:
   Σ_k w_k · ln C_k(o_k). `outcomes` maps observable -> value. Returns a typed
   absence rather than a number when C_mis has no measurable factor or the
   reading does not cover one.

   `:outcome-semantics` (U16, DEFAULT NIL AND NOTHING SUPPLIES IT) names one of
   `pref/bernoulli-outcome-arms` for the Bernoulli branch. Nil is the shipped
   path, byte for byte. Under an arm, a criterion whose value the arm refuses to
   read makes the WHOLE number absent (`:unread-outcome`, with the refusals
   listed) — the same discipline `:unreadable-observable` already applies, since
   a partial sum over the criteria the arm happened to accept would be a
   different mission's risk wearing this one's name."
  [{:keys [factors weights observable-of unmeasurable]} outcomes
   & {:keys [outcome-semantics]}]
  (if (empty? factors)
    {:status :absent :reason :no-measurable-criteria :unmeasurable unmeasurable}
    (let [missing (vec (remove #(contains? outcomes %) (vals observable-of)))]
      (if (seq missing)
        {:status :absent :reason :unreadable-observable :missing missing
         :unmeasurable unmeasurable}
        (let [terms (mapv (fn [[criterion observable]]
                            (assoc (term factors observable (get outcomes observable)
                                         outcome-semantics)
                                   :criterion criterion :observable observable
                                   :value (get outcomes observable)))
                          observable-of)
              refused (filterv #(= :absent (:status %)) terms)]
          (if (seq refused)
            {:status :absent :reason :unread-outcome
             :outcome-semantics outcome-semantics
             :refused (mapv #(select-keys % [:criterion :observable :value :reason]) refused)
             :unmeasurable unmeasurable}
            (cond-> {:status :present
                     :log-c (reduce + 0.0 (map #(* (get weights (:criterion %)) (:log-c %))
                                               terms))
                     :unmeasurable unmeasurable}
              outcome-semantics (assoc :outcome-semantics outcome-semantics))))))))

(defn risk-mis
  "risk_mis under the v0 status-quo forward model (design §3).

   Q(o_k|π) = δ at the CURRENT measured value of o_k for every π, so the
   per-criterion term is the SURPRISAL −ln C_k(o_k) and
   risk_mis = Σ_k w_k · (−ln C_k(o_k)) = −(log-c-mis).

   WHY SURPRISAL AND NOT `pref/kl`: for a Bernoulli C, KL(δ_b ‖ C) = −ln C(b)
   exactly — the two agree, and `mission_c_test/surprisal-is-the-point-mass-kl`
   pins that against `pref/kl` numerically. For a range C the point mass has no
   density, so KL is the cross-entropy −ln C(x) with the point mass's
   (divergent) differential entropy dropped. Named here rather than hidden
   behind a σ² small enough to look like a KL.

   Refuses rather than partially scores: any measurable criterion whose
   observable the reading does not cover makes the whole number absent. The
   typed `:unmeasurable` records ride on every return.

   `:outcome-semantics` (U16) is passed through to `log-c-mis`; nil — what every
   caller supplies today — is the shipped path unchanged."
  [c reading & {:keys [outcome-semantics]}]
  (let [{:keys [factors weights observable-of unmeasurable mission] :as _c} c
        composed (log-c-mis c reading :outcome-semantics outcome-semantics)]
    (merge
     {:version version
      :forward-model :status-quo-v0
      :mission mission
      :measurable-count (count factors)
      :unmeasurable unmeasurable}
     (when outcome-semantics {:outcome-semantics outcome-semantics})
     (if (= :present (:status composed))
       {:status :measured
        :risk (- (:log-c composed))
        :per-criterion
        (mapv (fn [[criterion observable]]
                (let [value (get reading observable)
                      t (term factors observable value outcome-semantics)
                      log-c (:log-c t)
                      w (get weights criterion)]
                  (cond-> {:criterion criterion :observable observable :value value
                           :log-c log-c :surprisal (- log-c)
                           :weight w :contribution (* w (- log-c))}
                    (contains? t :outcome) (assoc :outcome (:outcome t)))))
              observable-of)}
       (select-keys composed [:status :reason :missing :refused])))))
