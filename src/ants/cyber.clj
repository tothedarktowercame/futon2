(ns ants.cyber
  "OLD CYBERANTS — EVIDENCE OF A FAILURE, NOT A COMPONENT. See ../../README-xeno-loop.md §0.

   *** DO NOT BUILD TRANSFER / PROPAGATOR / WIRING WORK ON THIS NAMESPACE. ***
   The current ants are `ants.aif.*` (the modern AIF forager, M-aif-ants-port Port 1).
   This file is alphabetically earlier, has 'cyber' in the name, and is what you find
   first — which is exactly the trap this notice exists to spring.

   WHY (verified 2026-07-14, M-aif-ants-port): `config->aif-delta` below merges ONLY
   `:precision` (`:tau`, `:Pi-o`) into the live ant. `:policy-priors`, `:pattern-sense`,
   `:adapt-config` are stored under `:cyber-pattern -> :config` via `select-keys` and are
   NEVER READ — only `:id` and `:ticks-active` are consumed anywhere. So the old domain-
   transfer experiment's `random-wiring` control permuted INERT FIELDS: it was
   operationally byte-identical to the treatment. That 'null' was a TAUTOLOGY, and it was
   read as refuting the hypothesis rather than the apparatus.

   If you are about to wire something into `:cyber-pattern`, stop and read
   ../../README-xeno-loop.md. Any new actuator must pass an authority gate FIRST
   (`scripts/ant_authority_gate.clj`) — prove the knob moves the ant before claiming
   anything about what moving it means.

   ---
   Local pattern bridge - reads from futon3 library.
   Replaces futon5 dependency.

   Provides pattern-based configuration for cyber-ants, loading
   pattern definitions from futon3/library/ants/*.flexiarg files."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

;; -----------------------------------------------------------------------------
;; Configuration

(def ^:private pattern-root
  "Path to futon3 pattern library (relative to futon2 project root)."
  "../futon3/library/ants")

(def default-pattern-id
  "Default pattern key for cyber ants."
  :cyber/baseline)

;; -----------------------------------------------------------------------------
;; Pattern name mapping

(def ^:private pattern-key->filename
  "Map pattern keywords to flexiarg filenames."
  {:cyber/baseline           "baseline-cyber-ant.flexiarg"
   :cyber/hunger-coupling    "hunger-precision-coupling.flexiarg"
   :cyber/cargo-return       "cargo-return-discipline.flexiarg"
   :cyber/pheromone-tuner    "pheromone-trail-tuner.flexiarg"
   :cyber/white-space        "white-space-scout.flexiarg"})

(def ^:private filename->pattern-key
  "Reverse mapping for discovery."
  (into {} (map (fn [[k v]] [v k]) pattern-key->filename)))

(def library-key->pattern-key
  "The futon3 library id of a pattern (`:ants/white-space-scout`, the id
   `checks/find_organise.clj` keys the repository by) -> the `:cyber/*` key the
   two consumers in this repository dispatch on
   (`ants.aif.pattern-sense/constraint-satisfied?:78`,
   `ants.aif.pattern-efe/pattern-action-risk:117`).

   Derived from `pattern-key->filename` read backwards through the filename, NOT
   written out a second time by hand: the two names denote the same five files,
   and a second hand-kept table would be a place for them to drift apart.

   This matters because both of those consumers `case` on the key and fall
   through to a 0.0 / `true` default for anything they do not recognise.  Handing
   them a library id would leave channel 2 silently inert -- a lambda sweep over
   an arm that contributes exactly zero at every lambda, which is the shape of
   the tautology README-xeno-loop.md §0 exists to warn about."
  (into {} (map (fn [[k filename]]
                  [(keyword "ants" (str/replace filename #"\.flexiarg$" "")) k]))
        pattern-key->filename))

;; -----------------------------------------------------------------------------
;; Flexiarg parsing

(defn- parse-field
  "Extract a field value from flexiarg content."
  [content field-name]
  (when-let [match (re-find (re-pattern (str "@" field-name "\\s+(.+?)(?=\\n@|\\n!|$)"))
                            content)]
    (str/trim (second match))))

(defn- parse-title [content]
  (parse-field content "title"))

(defn- parse-flexiarg-id [content]
  (parse-field content "flexiarg"))

(defn- parse-conclusion
  "Extract the conclusion text from flexiarg (text after ! conclusion:)."
  [content]
  (when-let [match (re-find #"!\s*conclusion:\s*\n\s+(.+?)(?=\n\s+\+)" content)]
    (str/trim (second match))))

(defn- parse-aif-delta
  "Extract @aif-delta EDN from flexiarg content.
   Returns nil if not present."
  [content]
  (when-let [match (re-find #"@aif-delta\s+(\{[\s\S]*?\})\s*(?=@|\n!|$)" content)]
    (try
      (edn/read-string (second match))
      (catch Exception _
        nil))))

;; -----------------------------------------------------------------------------
;; Default AIF config deltas per pattern

(def ^:private default-deltas
  "Default AIF config deltas for each pattern.
   Used when @aif-delta is not present in the flexiarg file."
  {:cyber/baseline
   {}  ; baseline uses core defaults

   :cyber/hunger-coupling
   {:precision {:need-gain 0.7
                :dhdt-gain 0.9}
    :efe {:lambda {:survival 1.4}}}

   :cyber/cargo-return
   {:modes {:cargo-high 0.55}
    :precision {:tau-cap 1.2}
    :actions {:return {:cargo-thresh 0.1}}}

   :cyber/pheromone-tuner
   {:precision {:pher-scale 1.3
                :trail-grad-scale 1.2}
    :efe {:lambda {:info 0.5}}}

   :cyber/white-space
   {:precision {:food-scale 1.8
                :novelty-scale 1.4}
    :efe {:lambda {:info 0.6
                   :ambiguity 0.4}}}})

;; -----------------------------------------------------------------------------
;; Pattern loading

(defn- pattern-filename
  "Resolve a pattern filename from a pattern key.
   Falls back to <keyword-name>.flexiarg when not in the mapping."
  [pattern-key]
  (or (get pattern-key->filename pattern-key)
      (str (name pattern-key) ".flexiarg")))

(defn- pattern-file
  "Get the File for a pattern key."
  [pattern-key]
  (let [filename (pattern-filename pattern-key)
        f (io/file pattern-root filename)]
    (when (.exists f) f)))

(defn- load-flexiarg
  "Load and parse a flexiarg file, returning pattern metadata."
  [pattern-key]
  (when-let [f (pattern-file pattern-key)]
    (let [content (slurp f)
          title (or (parse-title content) (name pattern-key))
          summary (or (parse-conclusion content) "")
          aif-delta (or (parse-aif-delta content)
                        (get default-deltas pattern-key {}))]
      {:id pattern-key
       :title title
       :summary summary
       :aif-delta aif-delta
       :source (.getPath f)})))

;; -----------------------------------------------------------------------------
;; Public API

(defn available-patterns
  "List available pattern definitions.
   Returns a seq of maps with :id for each pattern."
  []
  (let [dir (io/file pattern-root)]
    (if (.exists dir)
      (->> (.listFiles dir)
           (filter #(str/ends-with? (.getName %) ".flexiarg"))
           (keep (fn [f]
                   (let [name (.getName f)
                         pk (or (get filename->pattern-key name)
                                (keyword "cyber" (str/replace name #"\.flexiarg$" "")))]
                     {:id pk
                      :file (.getPath f)})))
           vec)
      ;; Fallback: return known patterns even if files missing
      (mapv (fn [pk] {:id pk}) (keys pattern-key->filename)))))

(defn cyber-config
  "Get the full config for a pattern.
   Returns a map with :id and :aif-delta."
  [pattern-key]
  (or (load-flexiarg pattern-key)
      (load-flexiarg default-pattern-id)
      {:id default-pattern-id
       :title "Baseline Cyber-AIF Ant"
       :summary "Default AIF configuration"
       :aif-delta {}}))

(defn describe-pattern
  "Return human-readable description of a pattern.
   Returns map with :pattern, :title, :summary, and optionally :excerpt."
  [pattern-key]
  (let [config (cyber-config pattern-key)]
    {:pattern (:id config)
     :title (:title config)
     :summary (:summary config)}))

(defn- merge-deep
  "Deep merge maps, with later values taking precedence."
  [& maps]
  (letfn [(merge* [a b]
            (if (and (map? a) (map? b))
              (merge-with merge* a b)
              b))]
    (reduce merge* {} (remove nil? maps))))

(defn attach-config
  "Attach pattern-specific AIF config to an ant.
   Called by war.clj at spawn time.

   If pattern-key is not found, falls back to default-pattern-id."
  [ant pattern-key]
  (let [config (cyber-config pattern-key)]
    (-> ant
        (assoc :cyber-pattern {:id (:id config)
                               :title (:title config)
                               :delta (:aif-delta config)})
        (update :aif-config merge-deep (:aif-delta config)))))

;; -----------------------------------------------------------------------------
;; Cascade attachment - the actuator takes a CASCADE, not one pattern
;;
;; futon3 worklist row :LA4.  `attach-config` above takes a single `pattern-key`,
;; so a cascade of @aif-delta patterns had no expression at all: futon2/holes/
;; cascade-ants.edn:148 records the consequence -- "`:pattern/active` is singular
;; -- one pattern at a time.  A cascade is by definition composed.  Composition
;; is not merely unimplemented: :cascade/contentions shows it is currently
;; INCOHERENT (info 0.5 vs 0.6).  Composition needs a resolution rule, and 'last
;; write wins' is not one."
;;
;; The resolution rule is the cascade's own PRECEDENCE field.  That is not a new
;; concept invented here: it is law O4 of P-validated-R5 §3e -- precedence is
;; recorded on the cascade as a collection-level field, so which of two
;; contending patterns takes a key is DATA on the cascade rather than a property
;; of either pattern or an accident of attachment order.

(defn ordered-members
  "The cascade's members in precedence order, MOST PRECEDENT FIRST.

   This recomputes futon3/checks/find_organise.clj:376 `ordered` from the two
   fields the cascade carries -- sort the members by the cascade's own
   `:precedence`, least number first, ties broken by `:authored-order` (the sort
   is stable).  It is recomputed rather than read off the artefact's own
   `:ordered` key so that the two derivations can be compared; the caller that
   cares is scripts/cascade_authority_gate.clj, which checks they agree."
  [{:keys [members precedence authored-order]}]
  (let [members (set members)
        base (or (seq (filter members authored-order)) (sort members))]
    (vec (sort-by #(get precedence % Integer/MAX_VALUE) base))))

(defn cascade-delta
  "Fold the members' `@aif-delta` maps into one delta, with PRECEDENCE deciding
   every key collision.

   Direction, stated because it is the one thing here a reader could get
   backwards.  `merge-deep` is last-wins (see above), and `ordered-members` is
   most-precedent-FIRST, so this fold runs that list BACKWARDS: the least
   precedent member writes first and the most precedent member writes last and
   therefore takes any contended key.  That matches how precedence is read
   everywhere else in this line of work -- find_organise.clj:396 `fire` consults
   the rules in `ordered` order and takes the FIRST one that yields, so the most
   precedent rule is the one that decides.  A fold that let the least precedent
   member win would make `attach` and `fire` disagree about what precedence means.

   The empty cascade folds to `{}`, which is the identity: `merge-deep` with no
   arguments returns `{}` and `ants/baseline-cyber-ant`'s delta is `{}` too, so
   the sham arm of the authority gate is the identity by two separate routes."
  [cascade]
  (let [ordered (ordered-members cascade)]
    (apply merge-deep (reverse (map #(:aif-delta (cyber-config %)) ordered)))))

(defn delta-paths
  "Every LEAF of a nested delta map, as `[[k ...] value]`.  A delta contends with
   another at a leaf, not at a top-level key: `pheromone-trail-tuner` and
   `white-space-scout` both write under `:efe`, but they collide only at
   `[:efe :lambda :info]`."
  [m]
  (mapcat (fn [[k v]]
            (if (map? v)
              (map (fn [[path leaf]] [(vec (cons k path)) leaf]) (delta-paths v))
              [[[k] v]]))
          m))

(defn cascade-contentions
  "Every config path that more than one member of the cascade writes, with the
   value each member writes and the member whose value won.

   This is reported rather than resolved silently because a contention is the
   thing cascade-ants.edn:120-126 says goes undetected: three patterns shape tau
   through DIFFERENT keys, so a merge sees no collision at all while they argue
   about one quantity.  This function finds the collisions a merge CAN see; it
   does not claim to find the other kind."
  [cascade]
  (let [ordered (ordered-members cascade)
        writes (for [p ordered
                     [path v] (delta-paths (:aif-delta (cyber-config p)))]
                 {:path path :value v :pattern p})
        winner (cascade-delta cascade)]
    (->> (group-by :path writes)
         (filter (fn [[_ ws]] (> (count ws) 1)))
         (map (fn [[path ws]]
                (let [won (get-in winner path)]
                  {:path path
                   :writers (mapv (fn [w] [(:pattern w) (:value w)]) ws)
                   :won won
                   ;; derived from the folded value, not from position, so a
                   ;; wrong fold direction shows up as a mismatch here instead
                   ;; of being restated as if it were the intent.
                   :won-by (:pattern (first (filter #(= won (:value %)) ws)))})))
         (sort-by :path)
         vec)))

(defn attach-cascade-config
  "Attach a CASCADE's folded AIF config to an ant.  The cascade-taking sibling of
   `attach-config`.

   `cascade` is `{:members [...] :precedence {pattern int} :authored-order [...]}`
   -- the fields futon3/checks/construct_ants_cascade.clj writes to
   checks/ants-cascade.edn.  Two things are set, and they are the ant's two
   pattern channels:

     :aif-config    channel 1 -- the folded @aif-delta, read by
                    ants.aif.core/aif-config:67-71 on every step.
     :cyber-pattern channel 2 -- read by ants.aif.pattern-sense:109 through a
                    SINGULAR `:id`.  A cascade has no single id, so the most
                    precedent member is what channel 2 is shown, under its
                    `:cyber/*` alias (`library-key->pattern-key`) because that is
                    the key channel 2's two `case` tables dispatch on.  Its
                    library id is kept beside it as `:library-id`, and the whole
                    cascade under :cascade/:ordered/:delta, so nothing about the
                    composition is lost to a reader of the ant."
  [ant cascade]
  (let [ordered (ordered-members cascade)
        configs (mapv cyber-config ordered)
        head (:id (first configs))
        delta (cascade-delta cascade)]
    (-> ant
        (assoc :cyber-pattern {:id (get library-key->pattern-key head head)
                               :library-id head
                               :title (:title (first configs))
                               :cascade (:id cascade)
                               :ordered (mapv :id configs)
                               :delta delta})
        (update :aif-config merge-deep delta))))

;; -----------------------------------------------------------------------------
;; External config loader (pattern programs)

(defn- tau->bounds
  [tau]
  (let [tau (double (or tau 1.0))
        floor (max 0.05 (- tau 0.5))
        cap (min 2.5 (+ tau 0.5))]
    {:tau-floor floor :tau-cap cap}))

(defn- config->aif-delta
  "Convert a cyberant config (from futon5) into an AIF delta map."
  [cfg]
  (let [precision (:precision cfg)
        tau (:tau precision)
        pi-o (:Pi-o precision)]
    (cond-> {}
      (number? tau) (assoc :precision (tau->bounds tau))
      (map? pi-o) (update :precision merge {:Pi-o pi-o}))))

(defn- apply-external-config
  [ant cfg]
  (let [aif-delta (config->aif-delta cfg)
        prec (cond-> {}
               (map? (:Pi-o (:precision cfg))) (assoc :Pi-o (get-in cfg [:precision :Pi-o]))
               (number? (get-in cfg [:precision :tau])) (assoc :tau (get-in cfg [:precision :tau])))]
    (-> ant
        (assoc :cyber-pattern {:id (or (:pattern-id cfg) :pattern-program)
                               :title (:pattern-title cfg)
                               :config (select-keys cfg [:policy-priors :precision
                                                         :pattern-sense :adapt-config
                                                         :pattern-program])})
        (update :aif-config merge-deep aif-delta)
        (update :prec merge-deep prec))))

(defn- pick-cyberant
  [data index]
  (cond
    (map? (:cyberant data)) (:cyberant data)
    (seq (:cyberants data)) (nth (:cyberants data) (max 0 (min (dec (count (:cyberants data))) (or index 0))))
    (map? data) data
    :else nil))

(defn load-cyber-edn
  "Load a cyberant EDN bundle from disk and return the selected config."
  [path & {:keys [index]}]
  (when (and path (.exists (io/file path)))
    (let [data (edn/read-string (slurp path))]
      (pick-cyberant data index))))

(defn attach-config*
  "Attach either a flexiarg pattern or an external cyberant config.
   Accepts:
   - keyword pattern ids (legacy)
   - {:pattern kw} (legacy)
   - {:config <cyberant-map>} (new)
   - {:config-path \"...\" :index N} (new)"
  [ant cyber]
  (cond
    (keyword? cyber)
    (attach-config ant cyber)

    (and (map? cyber) (keyword? (:pattern cyber)))
    (attach-config ant (:pattern cyber))

    (and (map? cyber) (:config cyber))
    (apply-external-config ant (:config cyber))

    (and (map? cyber) (:config-path cyber))
    (if-let [cfg (load-cyber-edn (:config-path cyber) :index (:index cyber))]
      (apply-external-config ant cfg)
      (attach-config ant default-pattern-id))

    :else
    (attach-config ant default-pattern-id)))
