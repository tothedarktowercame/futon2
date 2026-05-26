(ns futon2.aif.belief
  "Per-entity belief state for the WM AIF apparatus.

   The belief state is a map of entity-id → posterior distribution over
   a tagged status set, carried across update steps and revised in
   response to typed evidence events. The status set is the M-INC
   per-entity vocabulary (`state/*` events from the M-interest-network-
   coupling event vocabulary v1); the event shape here is M-INC-
   compatible but does not depend on M-INC step (b) landing — events can
   be synthesised locally during development, then sourced from the
   typed-event substrate once step (b) commits.

   As of v0.9 of the contract (2026-05-18), this namespace also supplies
   `bootstrap-from-stack-annotations` — reads entity-ids from
   `~/code/futon5a/holes/stack-annotations.edn :sections[] :id` so the
   WM-side belief spans the same entity domain VSATARCS-side bootstraps
   from (`arxana-vsatarcs-belief-bootstrap-from-stack-annotations` in
   `~/code/futon4/dev/arxana-vsatarcs-belief.el`). This is the WM-side
   half of the v0.9 ↔ v0.2.5 bilateral milestone for
   `M-stack-essay-code-alignment`.

   Contract: contributes to R1 (explicit belief state) per
   `futon2/docs/futon-aif-completeness.md`. Cross-maps to F1 (explicit
   fitness state) at stack scope.

   Theory: Active Inference belief-tracking, discrete-state variant.
   The posterior is updated via multiplicative likelihood: an event
   of type t with weight w multiplies p(t) by (1 + w) and renormalises.
   This is the soft-Bayesian discrete analogue of the predictive-coding
   continuous update used in `ukrn-services-simulation/notebooks/
   ukrn_v3_belief.clj` — same shape, discrete status set instead of
   continuous (D, A) coordinates."
  (:require [clojure.edn :as edn]))

(def status-set
  "The tagged status set the WM tracks per entity. Aligns with M-INC
   event vocabulary v1 `state/*` event types (state/spawned,
   state/refined, etc.); the `link/*` events are relational and tracked
   separately, not within this posterior."
  #{:spawned :refined :strengthened :addressed :falsified :foreclosed :reopened})

(defn uniform-prior
  "Construct a uniform prior posterior over the status set."
  []
  (let [n (count status-set)
        p (/ 1.0 n)]
    (zipmap status-set (repeat p))))

(defn- normalise
  "Renormalise a posterior so probabilities sum to 1.0. If the input
   sums to zero (degenerate), returns a uniform prior — the apparatus
   should never produce a zero-sum posterior in normal operation, but
   this floor prevents NaN propagation."
  [posterior]
  (let [s (reduce + (vals posterior))]
    (if (zero? s)
      (uniform-prior)
      (into {} (for [[k v] posterior] [k (/ v s)])))))

(defn initial-belief-state
  "Construct a fresh belief state with uniform priors for the given
   entity ids."
  [entity-ids]
  (into {} (for [id entity-ids] [id (uniform-prior)])))

(defn update-entity-belief
  "Apply one evidence event to a single entity's posterior.

   Event shape (M-INC-compatible):
     {:type      <keyword in status-set>
      :weight    <number, default 1.0>
      :timestamp <inst, optional>}

   Returns a new normalised posterior. Events whose :type is not in
   status-set are ignored (posterior unchanged) — this keeps the
   update step total over a wider event stream (e.g. link/asserted
   events that aren't per-entity-status)."
  [posterior event]
  (let [{:keys [type weight]} event
        w (double (or weight 1.0))]
    (if (contains? status-set type)
      (normalise
       (into {} (for [[k v] posterior]
                  [k (if (= k type) (* v (+ 1.0 w)) v)])))
      posterior)))

(defn update-belief
  "Apply an evidence event (carrying :entity-id) to the full belief
   state. If the entity-id is not yet tracked, initialise it with a
   uniform prior before applying the event."
  [belief event]
  (let [eid (:entity-id event)
        current (get belief eid (uniform-prior))]
    (assoc belief eid (update-entity-belief current event))))

(defn update-belief-batch
  "Reduce a sequence of evidence events into the belief state. Order
   within a single entity's events is irrelevant for the final posterior
   because multiplicative likelihood updates commute under
   normalisation; order across entities is irrelevant because updates
   to different entities don't interact."
  [belief events]
  (reduce update-belief belief events))

(defn most-likely-status
  "Return the argmax status of a posterior. The discrete analogue of
   the belief 'mean'. Returns nil for an empty posterior."
  [posterior]
  (when (seq posterior)
    (key (apply max-key val posterior))))

(defn entropy
  "Shannon entropy of a posterior in nats. The discrete analogue of
   belief 'variance' / precision — uniform posteriors have maximal
   entropy (log n); peaked posteriors approach zero. Used by downstream
   R3 (predictive-coding) and R7 (adaptive precision) work."
  [posterior]
  (- (reduce + (for [v (vals posterior)
                     :when (pos? v)]
                 (* v (Math/log v))))))

;; ---------------------------------------------------------------------------
;; v0.9: symmetric bootstrap from the canonical projection source.
;; Mirrors VSATARCS-side `arxana-vsatarcs-belief-bootstrap-from-stack-
;; annotations` (per `~/code/futon4/dev/arxana-vsatarcs-belief.el`,
;; closure `hx:vsatarcs-align:v0-2-2:r1-bootstrap-from-stack-annotations`).
;; The WM half of the v0.9 ↔ v0.2.5 bilateral milestone — after both
;; sides bootstrap from the same `:sections[] :id` strings, per-entity
;; posterior comparison reduces to alist-lookup equality on shared
;; entity-ids (the substrate the alignment-drift check is built on).
;; ---------------------------------------------------------------------------

(def default-stack-annotations-path
  (str (System/getProperty "user.home") "/code/futon5a/holes/stack-annotations.edn"))

(defn section-ids-from-stack-annotations
  "Read `:sections[] :id` strings from `stack-annotations.edn`. Returns
   a vector of strings (entity-ids in the canonical projection source).

   Defaults to `~/code/futon5a/holes/stack-annotations.edn`; a custom
   path can be passed."
  ([] (section-ids-from-stack-annotations default-stack-annotations-path))
  ([path]
   (let [doc (edn/read-string (slurp path))]
     (mapv :id (:sections doc)))))

(defn bootstrap-from-stack-annotations
  "Construct a fresh belief state whose entity domain is the union of
   the section ids in `stack-annotations.edn` (string keys) and any
   `extra-ids` supplied (e.g. the meta-sorry's keyword id from
   `sorry-registry`). Uniform prior per entity.

   If the canonical source can't be read (missing file, parse error),
   falls back to `extra-ids` only — the WM remains operational under
   intermittent substrate availability.

   Caveat: keys are heterogeneous after this call — section-ids are
   strings, sorry-registry ids are keywords. They're disjoint by
   construction (no section-id collides with a sorry-id namespace) so
   alist-lookup equality with VSATARCS-side belief (which uses the
   same string section-ids) works directly on the string-keyed subset."
  ([] (bootstrap-from-stack-annotations []))
  ([extra-ids]
   (let [section-ids (try (section-ids-from-stack-annotations)
                          (catch Exception _ []))]
     (initial-belief-state (distinct (concat section-ids extra-ids))))))

;; ---------------------------------------------------------------------------
;; E-support-coverage Cycle 2 (cg-17bbaa01, 2026-05-26):
;; Entity-tag classification at bootstrap.  Decision (operator-approved
;; 2026-05-26): option (b) — parallel `entity-tags` map alongside `belief`,
;; bootstrap-derived from `stack-annotations.edn` `:ref` fields, read-only
;; after bootstrap.  Enables `predict-support-coverage` and
;; `predict-attack-coverage` (Cycle 3) to filter belief by S1-S5 /
;; A1-A4 support/attack claim membership without changing belief shape.
;; ---------------------------------------------------------------------------

(def ^:private support-claim-pattern
  "Matches :ref values like 'sorry|holistic-argument|S|S1' through S5.
   Capture group: the S<N> identifier."
  #"^sorry\|holistic-argument\|S\|(S[1-5])$")

(def ^:private attack-claim-pattern
  "Matches :ref values like 'sorry|holistic-argument|A|A1' through A4.
   Capture group: the A<N> identifier."
  #"^sorry\|holistic-argument\|A\|(A[1-4])$")

(defn- tag-for-ref
  "Return a tag keyword if `ref-str` matches a known S/A pattern, else nil.

   Examples:
     (tag-for-ref \"sorry|holistic-argument|S|S2\") => :supports-S2
     (tag-for-ref \"sorry|holistic-argument|A|A1\") => :attacks-A1
     (tag-for-ref \"sorry|holistic-argument|thesis\") => nil"
  [ref-str]
  (cond
    (not (string? ref-str)) nil

    :else
    (or (when-let [[_ sid] (re-matches support-claim-pattern ref-str)]
          (keyword (str "supports-" sid)))
        (when-let [[_ aid] (re-matches attack-claim-pattern ref-str)]
          (keyword (str "attacks-" aid))))))

(defn classify-entity-tags-from-stack-annotations
  "Read `stack-annotations.edn` and produce an entity-tags map.

   Returns `{entity-id #{tag-keywords}}` for sections whose `:ref` matches
   a known S/A pattern.  Sections without matching refs are absent from the
   map (callers should treat absence as 'no tags', not 'unknown' — the
   classification is total over the holistic-argument S/A vocabulary).

   The entity-id used is the `:id` field of the section (the same key the
   belief map uses).  This is the (b) shape from
   futon3c/holes/missions/E-support-coverage.md Cycle 1 outcome.

   Defaults to `default-stack-annotations-path`.

   Errors are caught and returned as an empty map — bootstrap should not
   fail if the substrate is temporarily unreachable."
  ([] (classify-entity-tags-from-stack-annotations default-stack-annotations-path))
  ([path]
   (try
     (let [doc (edn/read-string (slurp path))]
       (reduce (fn [acc section]
                 (let [eid (:id section)
                       ref-str (:ref section)]
                   (if-let [tag (tag-for-ref ref-str)]
                     (update acc eid (fnil conj #{}) tag)
                     acc)))
               {}
               (:sections doc)))
     (catch Throwable _ {}))))

;; ---------------------------------------------------------------------------
;; v0.10: likelihood model for :annotation-health (R3a — first belief-derived
;; observation channel).
;;
;; The likelihood maps a per-entity belief to a predicted distribution over
;; the :annotation-health channel value. Per-status weights are hand-tuned
;; (R7 will learn them); they encode "what does the canonical annotation
;; graph look like when belief is concentrated on this status?":
;;
;;   :strengthened →  1.0   (entity carries reinforcing evidence)
;;   :addressed    →  1.0   (open question closed)
;;   :refined      →  0.5   (entity has been improved)
;;   :spawned      →  0.0   (just exists; no work signal either way)
;;   :reopened     →  0.0   (was-addressed-now-questioned; ambiguous)
;;   :foreclosed   → -0.5   (closed without resolution)
;;   :falsified    → -1.0   (determined wrong)
;;
;; Per-entity expected health = Σ posterior[s] * weight[s], renormalised
;; from [-1, 1] to [0, 1]. Channel value = mean across entities. Channel
;; variance ∝ aggregate posterior entropy (sharper beliefs → smaller
;; predictive variance).
;; ---------------------------------------------------------------------------

(def annotation-health-status-weights
  "Per-status contribution to predicted :annotation-health. Hand-tuned
   in v0.10; replaceable by R7 (adaptive precision / learned weights)."
  {:strengthened 1.0
   :addressed    1.0
   :refined      0.5
   :spawned      0.0
   :reopened     0.0
   :foreclosed   -0.5
   :falsified    -1.0})

(defn- entity-expected-health
  "Compute predicted health contribution from one entity's posterior.
   Returns a value in [0, 1] after rescaling from the raw weighted-sum
   range [-1, 1]."
  [posterior]
  (let [raw (reduce + (for [[s p] posterior]
                        (* (double p)
                           (double (get annotation-health-status-weights s 0.0)))))]
    ;; rescale [-1, 1] → [0, 1]
    (max 0.0 (min 1.0 (/ (+ raw 1.0) 2.0)))))

(defn predict-annotation-health
  "Predict observation distribution for the `:annotation-health` channel
   given a belief state. Returns `{:mean :variance}`.

   `:mean` — average per-entity expected health across all tracked entities.
   `:variance` — proportional to mean posterior entropy across entities
   (uniform belief → high variance; peaked belief → low variance).

   Empty belief returns `{:mean 0.0 :variance 1.0}` — maximally uncertain;
   no entities to predict from."
  [belief]
  (if (empty? belief)
    {:mean 0.0 :variance 1.0}
    (let [n (count belief)
          per-entity (mapv (fn [[_ posterior]]
                             [(entity-expected-health posterior)
                              (entropy posterior)])
                           belief)
          mean (/ (reduce + (map first per-entity)) (double n))
          ;; variance scaled by mean entropy of posteriors, normalised
          ;; against log(|status-set|) so a uniform belief → variance ≈ 1.0
          max-entropy (Math/log (count status-set))
          mean-entropy (/ (reduce + (map second per-entity)) (double n))
          variance (if (pos? max-entropy)
                     (/ mean-entropy max-entropy)
                     0.0)]
      {:mean mean :variance variance})))

;; ---------------------------------------------------------------------------
;; v0.11: three additional likelihood models — `:sorry-count-norm`,
;; `:mission-health`, `:active-repo-ratio`. Same variance shape across all
;; four (mean posterior entropy normalised to [0,1]); means differ per
;; channel's semantic projection over the discrete status set.
;; ---------------------------------------------------------------------------

(defn- entity-open-mass
  "Probability the entity is in an 'open' (still-being-worked) state."
  [posterior]
  (+ (double (get posterior :spawned 0.0))
     (double (get posterior :refined 0.0))
     (double (get posterior :reopened 0.0))))

(defn- entity-healthy-mass
  "Probability the entity is in a healthy state (closed-with-strength)."
  [posterior]
  (+ (double (get posterior :strengthened 0.0))
     (double (get posterior :addressed 0.0))))

(defn- entity-nondormant-mass
  "Probability the entity is NOT in a dormant state (foreclosed/falsified)."
  [posterior]
  (- 1.0 (+ (double (get posterior :foreclosed 0.0))
            (double (get posterior :falsified 0.0)))))

(defn- mean-entropy-variance
  "Shared variance computation: mean posterior entropy across entities,
   normalised against log(|status-set|). Empty belief → 1.0."
  [belief]
  (if (empty? belief)
    1.0
    (let [n (count belief)
          max-entropy (Math/log (count status-set))
          mean-entropy (/ (reduce + (map (comp entropy second) belief)) (double n))]
      (if (pos? max-entropy) (/ mean-entropy max-entropy) 0.0))))

(defn predict-sorry-count-norm
  "Predict observation distribution for `:sorry-count-norm` (= open-sorrys / 10,
   capped at 1.0). From belief: sum of per-entity open-mass, divided by 10,
   capped. Higher belief mass on open statuses → higher predicted value."
  [belief]
  (if (empty? belief)
    {:mean 0.0 :variance 1.0}
    {:mean (min 1.0 (/ (reduce + (map (comp entity-open-mass second) belief)) 10.0))
     :variance (mean-entropy-variance belief)}))

(defn predict-mission-health
  "Predict observation distribution for `:mission-health`. From belief:
   mean per-entity healthy-mass — fraction of belief on `:strengthened`
   + `:addressed`. Higher concentration on healthy statuses → higher
   predicted mission-health."
  [belief]
  (if (empty? belief)
    {:mean 0.0 :variance 1.0}
    (let [n (count belief)]
      {:mean (/ (reduce + (map (comp entity-healthy-mass second) belief)) (double n))
       :variance (mean-entropy-variance belief)})))

(defn predict-active-repo-ratio
  "Predict observation distribution for `:active-repo-ratio`. From belief:
   mean per-entity non-dormant mass — fraction of belief NOT on
   `:foreclosed` + `:falsified`. Entities not yet declared dead count as
   active. Higher non-dormant mass → higher predicted ratio."
  [belief]
  (if (empty? belief)
    {:mean 0.0 :variance 1.0}
    (let [n (count belief)]
      {:mean (/ (reduce + (map (comp entity-nondormant-mass second) belief)) (double n))
       :variance (mean-entropy-variance belief)})))

(def channels-with-likelihood
  "Set of observation channels for which an R3a likelihood model exists.
   v0.11: 4 of 14 channels covered. Remaining 10 are logged as
   `:prototyping-forward` sorrys in `futon2/data/sorrys.edn`."
  #{:annotation-health :sorry-count-norm :mission-health :active-repo-ratio})

(defn predict-observation
  "Predict observation distributions across all channels for which a
   likelihood model exists. Returns a map of channel-id → `{:mean :variance}`.
   Channels in `channels-with-likelihood` are included; others are absent
   (callers using this must handle absence — typically by falling back to
   preference-gap-driven scoring for those channels)."
  [belief]
  {:annotation-health (predict-annotation-health belief)
   :sorry-count-norm (predict-sorry-count-norm belief)
   :mission-health (predict-mission-health belief)
   :active-repo-ratio (predict-active-repo-ratio belief)})
