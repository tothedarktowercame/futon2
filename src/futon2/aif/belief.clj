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
  (:require [clojure.edn :as edn]
            [clojure.set :as set]
            [clojure.string :as str]))

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

;; ---------------------------------------------------------------------------
;; v0.24 (M-aif-faithfulness B-3a, 2026-07-04): explicit observation model A —
;; the EVENT BLOCK. Design note: holes/E-r1-a-matrix-design.md.
;;
;; The legacy update multiplies p(s) by (1+w) when s = event type, else 1.
;; That is ALREADY an A-matrix update in disguise:
;;
;;   p'(s) ∝ p(s) · (1+w)^δ(o,s)  =  p(s) · A₀[o][s]^κ(w)
;;
;; with A₀[o][s] = 2^δ(o,s) (diagonal 2, off-diagonal 1) and precision
;; exponent κ(w) = log₂(1+w). So "no A-matrix" was really "an IMPLICIT A
;; asserting every event type perfectly discriminates its own status and
;; says nothing about the others." v0 makes A explicit and hand-set
;; (learned-A is out of scope), which buys two things the diagonal-only
;; form cannot express: lifecycle-adjacent confusability (an event can be
;; mildly compatible with a neighbouring status) and contradiction (an
;; event can be mild evidence AGAINST an opposed status).
;;
;; HONESTY: A is HAND-SET in v0 — a declared observation model, not a
;; learned or derived one. Entries are likelihood RATIOS against an
;; uninformative baseline of 1.0 (scale-free: normalisation kills
;; constants). The update path is DARK behind :likelihood-mode (default
;; :legacy, byte-identical); the flip is the operator's (arena-*-mode
;; idiom, same discipline as efe.clj's :risk-mode). Badge disposition is
;; the reviewer's call, not claimed here.
;; ---------------------------------------------------------------------------

(def a-matrix-default-gain
  "Diagonal likelihood ratio of the v0 A-matrix. 2.0 matches the legacy
   gain at w = 1 ((1+w) = 2), so the identity-structured A reproduces the
   legacy update exactly under κ(w) = log₂(1+w) (see `likelihood-vector`)."
  2.0)

(def a-matrix-overrides
  "Sparse off-diagonal structure of the v0 hand-set A, as
   {[observed-event true-status] likelihood-ratio}. Unlisted off-diagonal
   pairs are 1.0 (uninformative). Two entry classes:

   ADJACENT (1.3) — the observed event is mildly compatible with a
   lifecycle-neighbouring true status (belief-label lag):
     [:refined :spawned]      refine events arrive while belief still says spawned
     [:strengthened :refined] strengthening surfaces mid-refinement
     [:addressed :refined]    the closing edit is itself a refinement
     [:reopened :addressed]   a reopen presupposes a recent addressed
     [:refined :reopened]     reopened entities emit refinements as rework begins

   CONTRADICTORY (0.7) — the observed event is mild evidence AGAINST a
   semantically opposed closure status (inexpressible in the legacy
   diagonal-only form):
     strengthened ⟂ falsified · addressed ⟂ foreclosed (both directions)."
  {[:refined :spawned]        1.3
   [:strengthened :refined]   1.3
   [:addressed :refined]      1.3
   [:reopened :addressed]     1.3
   [:refined :reopened]       1.3
   [:strengthened :falsified] 0.7
   [:falsified :strengthened] 0.7
   [:addressed :foreclosed]   0.7
   [:foreclosed :addressed]   0.7})

(defn- materialise-a-matrix
  "Build a dense {observed-event {true-status likelihood}} matrix over
   status-set from a diagonal gain and a sparse override map."
  [gain overrides]
  (into {}
        (for [o status-set]
          [o (into {}
                   (for [s status-set]
                     [s (double (get overrides [o s]
                                     (if (= o s) gain 1.0)))]))])))

(def a-matrix-v0
  "The v0 hand-set observation model A (event block): 7×7,
   {observed-event {true-status likelihood-ratio}}. Rows are observed
   M-INC state/* event types; columns are true statuses; each entry is
   P-ratio(observe o | true s) against the 1.0 uninformative baseline."
  (materialise-a-matrix a-matrix-default-gain a-matrix-overrides))

(def a-matrix-identity
  "The identity-structured A (diagonal `a-matrix-default-gain`, all
   off-diagonal 1.0) — the legacy update's implicit A. Kept as a def so
   the legacy-reduction theorem is directly testable:
   :a-matrix mode with THIS matrix ≡ the :legacy path, for every weight."
  (materialise-a-matrix a-matrix-default-gain {}))

;; ---------------------------------------------------------------------------
;; v0.25 (M-aif-a-matrix-faithfulness Stage 1): formal fidelity — exact
;; categorical Bayes under a declared, normalised AIF generative model.
;;
;; The legacy and :a-matrix modes use likelihood RATIOS (scale-free). The
;; formal AIF warrant requires column-normalised conditional distributions
;; P(o|s), an explicit transition model B, and an initial prior D. This
;; section provides those, plus validators and a categorical filter that
;; computes the explicit A × (B × q) prediction-update cycle.
;;
;; The :aif likelihood mode uses these normalised models. It is DARK
;; (default remains :legacy). The flip is the operator's, same discipline
;; as :risk-mode and :a-matrix.
;; ---------------------------------------------------------------------------

(defn- normalise-column
  "Normalise a likelihood-ratio column so it sums to 1.0, converting
   from the ratio representation to a proper P(o|s) distribution."
  [ratio-column]
  (let [total (reduce + (vals ratio-column))]
    (into {} (for [[k v] ratio-column]
               [k (/ (double v) total)]))))

(defn- normalise-a-matrix
  "Convert a likelihood-ratio A matrix {obs {status ratio}} to a
   column-normalised observation model {obs {status P(o|s)}}.
   Each column (fixed status s) is normalised independently so that
   Σ_o A[o,s] = 1."
  [ratio-matrix]
  (let [statuses (keys (first (vals ratio-matrix)))
        ;; transpose: group by status (column), normalise, transpose back
        columns (into {}
                      (for [s statuses]
                        [s (into {}
                                 (for [o (keys ratio-matrix)]
                                   [o (double (get-in ratio-matrix [o s]))]))]))
        normalised-columns (into {}
                                 (for [[s col] columns]
                                   [s (normalise-column col)]))
        ;; transpose back to {obs {status P(o|s)}}
        ]
    (into {}
          (for [o (keys ratio-matrix)]
            [o (into {}
                     (for [s statuses]
                       [s (double (get-in normalised-columns [s o]))]))]))))

(def observation-model-v1
  "The v1 normalised observation model A (event block): 7×7,
   {observed-event {true-status P(o|s)}}. Derived from `a-matrix-v0`
   by column normalisation, so each column s satisfies Σ_o A[o,s] = 1.
   The off-diagonal structure (lifecycle-adjacent, contradictory) is
   preserved in shape; only the scale changes."
  (normalise-a-matrix a-matrix-v0))

(def observation-model-identity
  "The identity-structured normalised observation model: the
   column-normalised form of `a-matrix-identity`. Equivalent to a
   near-uniform matrix with a slight diagonal boost — the normalised
   form of the legacy model."
  (normalise-a-matrix a-matrix-identity))

(def transition-model-v1
  "The v1 transition model B (event block): 7×7 identity.
   {predecessor-status {successor-status P(s_t|s_{t-1})}}.
   Each column s satisfies Σ_s' B[s',s] = 1.
   Identity means 'no explicit dynamics; the prior is the previous
   posterior.' This is the simplest justified B — it names what the code
   already does implicitly via *carry-belief?*. A lifecycle-constrained
   B is a candidate for later refinement."
  (into {}
        (for [s status-set]
          [s (into {}
                   (for [s' status-set]
                     [s' (if (= s s') 1.0 0.0)]))])))

(def initial-prior-v1
  "The v1 initial prior D: uniform over the status set.
   This is the starting distribution q(s_0) for a cold-start entity.
   Matches `uniform-prior` exactly."
  (uniform-prior))

;; --- Validators (C2, C3) ---

(defn valid-distribution?
  "True if m is a map from keywords to finite, non-negative doubles
   that sum to approximately 1.0."
  [m & {:keys [tolerance] :or {tolerance 1e-9}}]
  (and (map? m)
       (every? #(and (number? %)
                     (Double/isFinite (double %))
                     (>= (double %) 0.0))
               (vals m))
       (< (Math/abs (- (reduce + (vals m)) 1.0)) tolerance)))

(defn valid-observation-model?
  "True if A is a valid observation model: every column (status) is a
   proper distribution over observations."
  [A]
  (and (map? A)
       (= status-set (set (keys A)))
       (every? (fn [s]
                 (let [column (into {} (for [[o row] A] [o (double (get row s 0.0))]))]
                   (valid-distribution? column)))
               status-set)))

(defn valid-transition-model?
  "True if B is a valid transition model: every column (predecessor) is
   a proper distribution over successor states."
  [B]
  (and (map? B)
       (= status-set (set (keys B)))
       (every? (fn [s]
                 (valid-distribution? (get B s)))
               status-set)))

(defn valid-initial-prior?
  "True if D is a valid initial prior distribution over the status set."
  [D]
  (and (map? D)
       (= status-set (set (keys D)))
       (valid-distribution? D)))

(defn model-version
  "Compute a content hash for a model component (A, B, or D).
   Returns a string suitable for provenance stamping."
  [m]
  (format "0x%08x"
          (hash (into (sorted-map)
                      (for [[k v] m]
                        [k (if (map? v)
                             (into (sorted-map) v)
                             v)])))))

(defn model-manifest
  "Build a provenance manifest for the declared AIF model components.
   Returns a map with version hashes suitable for trace stamping (C9)."
  ([] (model-manifest observation-model-v1 transition-model-v1 initial-prior-v1))
  ([A B D]
   {:observation-model-hash (model-version A)
    :transition-model-hash  (model-version B)
    :initial-prior-hash     (model-version D)}))

;; --- The explicit categorical filter (C3, C12) ---

(defn predict-step
  "Prediction step of the categorical filter:
     q⁻(s_t) = Σ_s' B(s_t | s') q(s_{t-1})
   This is a matrix-vector product: B × q.
   With identity B, this returns q unchanged."
  [B q]
  (into {}
        (for [s' status-set]
          [s' (reduce +
                      (for [s status-set]
                        (* (double (get-in B [s s'] 0.0))
                           (double (get q s 0.0)))))])))

(defn update-step
  "Update step of the categorical filter:
     q(s_t) ∝ A(o_t | s_t)^κ(w) · q⁻(s_t)
   The tempered likelihood A[o|s]^κ(w) is applied as in the :a-matrix
   mode, but A is now the normalised observation model."
  [A observed w q-predicted]
  (let [kappa (/ (Math/log (+ 1.0 (double w))) (Math/log 2.0))
        likelihoods (into {}
                          (for [s status-set]
                            [s (Math/pow (double (get-in A [observed s] 0.0)) kappa)]))]
    (normalise
     (into {}
           (for [[s prior] q-predicted]
             [s (* (double prior) (double (get likelihoods s 0.0)))])))))

(defn categorical-filter-step
  "One complete prediction-update cycle of the categorical filter:
     q⁻ = B × q        (prediction)
     q  ∝ A(o|·)^κ(w) · q⁻   (tempered update)
   This is the explicit A × (B × q) computation. Returns the new
   posterior. Validates the model components once per call (fail-closed
   on malformed model data per C9)."
  ([q event] (categorical-filter-step q event
                                       observation-model-v1
                                       transition-model-v1
                                       {:weight (:weight event 1.0)}))
  ([q event A B {:keys [weight] :or {weight 1.0}}]
   (let [observed (:type event)
         w        (double (or weight 1.0))]
     (when-not (valid-observation-model? A)
       (throw (ex-info "Invalid observation model A (fail-closed)" {:A A})))
     (when-not (valid-transition-model? B)
       (throw (ex-info "Invalid transition model B (fail-closed)" {:B B})))
     (if (contains? status-set observed)
       (->> (predict-step B q)
            (update-step A observed w))
       q))))

(defn likelihood-vector
  "Likelihood over true statuses for one observed event of type
   `observed` with weight `w`, under observation model A:

     L(s) = A[observed][s]^κ(w),  κ(w) = log₂(1+w)

   κ is the precision exponent (tempered likelihood): w = 0 ⇒ κ = 0 ⇒
   L ≡ 1 (no-op, as legacy); w = 1 ⇒ κ = 1 ⇒ L = the A row as-is;
   larger w sharpens the row. With A = `a-matrix-identity` this equals
   the legacy (1+w)-on-the-diagonal likelihood exactly."
  [A observed w]
  (let [kappa (/ (Math/log (+ 1.0 (double w))) (Math/log 2.0))]
    (into {}
          (for [[s l] (get A observed)]
            [s (Math/pow (double l) kappa)]))))

(defn update-entity-belief
  "Apply one evidence event to a single entity's posterior.

   Event shape (M-INC-compatible):
     {:type      <keyword in status-set>
      :weight    <number, default 1.0>
      :timestamp <inst, optional>}

   Returns a new normalised posterior. Events whose :type is not in
   status-set are ignored (posterior unchanged) — this keeps the
   update step total over a wider event stream (e.g. link/asserted
   events that aren't per-entity-status).

   Opts:
     :likelihood-mode  :legacy (default) — the historical
                       (1+w)-on-the-diagonal update, byte-identical.
                       :a-matrix — Bayes with the explicit observation
                       model: p'(s) ∝ p(s)·A[o][s]^κ(w), κ(w) = log₂(1+w).
                       :aif — categorical filter with normalised A/B/D:
                       q⁻ = B×q; q ∝ A(o|·)^κ(w)·q⁻. Validates model
                       components on every call (fail-closed per C9).
     :a-matrix         observation model to use in :a-matrix mode
                       (default `a-matrix-v0`). With `a-matrix-identity`
                       the :a-matrix path reproduces :legacy exactly.
     :observation-model  normalised observation model A for :aif mode
                         (default `observation-model-v1`).
     :transition-model   transition model B for :aif mode
                         (default `transition-model-v1`).
   The flip to :a-matrix or :aif is the operator's (arena-*-mode idiom)."
  ([posterior event] (update-entity-belief posterior event {}))
  ([posterior event {:keys [likelihood-mode a-matrix
                            observation-model transition-model]
                     :or {likelihood-mode :legacy}}]
   (let [{:keys [type weight]} event
         w (double (or weight 1.0))]
     (if (contains? status-set type)
       (case likelihood-mode
         :legacy
         (normalise
          (into {} (for [[k v] posterior]
                     [k (if (= k type) (* v (+ 1.0 w)) v)])))
         :a-matrix
         (let [L (likelihood-vector (or a-matrix a-matrix-v0) type w)]
           (normalise
            (into {} (for [[k v] posterior]
                       [k (* v (double (get L k 1.0)))]))))
         :aif
         (categorical-filter-step posterior event
                                   (or observation-model observation-model-v1)
                                   (or transition-model transition-model-v1)
                                   {:weight w}))
       posterior))))

(defn update-belief
  "Apply an evidence event (carrying :entity-id) to the full belief
   state. If the entity-id is not yet tracked, initialise it with a
   uniform prior before applying the event. Opts (optional) are passed
   through to `update-entity-belief`."
  ([belief event] (update-belief belief event {}))
  ([belief event opts]
   (let [eid (:entity-id event)
         current (get belief eid (uniform-prior))]
     (assoc belief eid (update-entity-belief current event opts)))))

(defn update-belief-batch
  "Reduce a sequence of evidence events into the belief state. Order
   within a single entity's events is irrelevant for the final posterior
   because multiplicative likelihood updates commute under
   normalisation (this holds in BOTH likelihood modes — the :a-matrix
   path is also a product of per-event likelihoods); order across
   entities is irrelevant because updates to different entities don't
   interact. Opts (optional) are passed through to
   `update-entity-belief`."
  ([belief events] (update-belief-batch belief events {}))
  ([belief events opts]
   (reduce (fn [b e] (update-belief b e opts)) belief events)))

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

(defn reconcile-belief-carry
  "R8 belief carry: seed this tick's prior from the previous tick's posterior
   `carried-mu-post`, reconciled to the current entity domain given by
   `fresh-bootstrap` (a fresh uniform belief state over this tick's entities).
   Survivors (entities in both) keep their carried posterior; new entities (in
   `fresh-bootstrap` only) get the fresh uniform prior; vanished entities (in
   `carried-mu-post` only) are dropped. Pure and deterministic; a nil/empty
   `carried-mu-post` (cold start, no prior trace) returns `fresh-bootstrap`."
  [fresh-bootstrap carried-mu-post]
  (if (seq carried-mu-post)
    (into {} (for [[eid prior] fresh-bootstrap]
               [eid (get carried-mu-post eid prior)]))
    fresh-bootstrap))

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

(defn entity-expected-health
  "Compute predicted health contribution from one entity's posterior.
   Returns a value in [0, 1] after rescaling from the raw weighted-sum
   range [-1, 1].

   Public since R3d v0.17 (sorry/r3d-per-entity-attribution): the judge's
   per-entity belief-update attribution weights each entity by its
   health-deviation, so this per-entity quantity is needed at the judge site."
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

;; ---------------------------------------------------------------------------
;; E-support-coverage Cycle 3 (cg-a5d2e756, 2026-05-26):
;; Belief-filtered support/attack coverage likelihoods.  Filter belief by the
;; bootstrap-derived entity-tags map (Cycle 2) to compute mean healthy-mass
;; over the S1-S5 / A1-A4 tagged cohorts.
;; ---------------------------------------------------------------------------

(defn- entities-with-tag-prefix
  "Return the subset of belief entities whose entity-tags include any tag
   starting with PREFIX (e.g. 'supports-' or 'attacks-')."
  [belief entity-tags prefix]
  (filter (fn [[eid _]]
            (some #(and (keyword? %)
                        (str/starts-with? (name %) prefix))
                  (get entity-tags eid #{})))
          belief))

(defn- repo-from-source-file
  "Derive the repo label for a stack-annotation source path.

   Devmap story files are repo-specific even though they live under futon5a,
   e.g. `futon5a/.../devmap-futon3.aif.edn` describes the futon3 repo.
   Other paths use their first `futon*` path segment."
  [source-file]
  (when (string? source-file)
    (or (second (re-find #"devmap-(futon[0-9a-z]+)\.aif\.edn" source-file))
        (second (re-find #"^(futon[0-9a-z]+)(?:/|$)" source-file)))))

(defn classify-entity-repos-from-stack-annotations
  "Read `stack-annotations.edn` and produce `{entity-id repo-label}`.

   This is the repo bridge needed by repo-level temporal-coupling edges:
   belief is section/entity-level, while coupling is repo-level. Sections
   without a derivable source repo are omitted."
  ([] (classify-entity-repos-from-stack-annotations default-stack-annotations-path))
  ([path]
   (try
     (let [doc (edn/read-string (slurp path))]
       (into {}
             (keep (fn [section]
                     (let [eid (:id section)
                           source-file (or (get-in section [:provenance :source-file])
                                           (:ref section))
                           repo (repo-from-source-file source-file)]
                       (when (and eid repo)
                         [eid repo]))))
             (:sections doc)))
     (catch Throwable _ {}))))

(def ^:private tick-ref-pattern
  #"^tick\|logic-model\|([a-z0-9-]+)$")

(defn- tick-tag-for-section
  [section]
  (let [tick-name (or (some-> (:tick-id section) name)
                      (some->> (:ref section)
                               (re-matches tick-ref-pattern)
                               second))]
    (when tick-name
      (keyword "tick" tick-name))))

(defn classify-entity-ticks-from-stack-annotations
  "Read `stack-annotations.edn` and produce `{entity-id #{:tick/<id>}}`.

   Tick entities are first-class stack-annotation sections with refs like
   `tick|logic-model|hermit-warning`, sourced from the pocketwatch ticks in
   `stack-logic-model.edn`."
  ([] (classify-entity-ticks-from-stack-annotations default-stack-annotations-path))
  ([path]
   (try
     (let [doc (edn/read-string (slurp path))]
       (reduce (fn [acc section]
                 (let [eid (:id section)]
                   (if-let [tag (tick-tag-for-section section)]
                     (update acc eid (fnil conj #{}) tag)
                     acc)))
               {}
               (:sections doc)))
     (catch Throwable _ {}))))

(defn predict-support-coverage
  "Predict observation distribution for the `:support-coverage` channel.

   From belief filtered by entity-tags: mean healthy-mass over entities
   tagged with any `:supports-S<N>` tag.  Variance: mean posterior entropy
   over the same cohort, normalised against log(|status-set|).

   Empty cohort (no supports-tagged entities in belief) → `{:mean 0.0
   :variance 1.0}` mirroring the empty-belief branch of the existing
   4 predictors — maximally uncertain when no data.

   E-support-coverage Cycle 3."
  [belief entity-tags]
  (let [cohort (entities-with-tag-prefix belief entity-tags "supports-")]
    (if (empty? cohort)
      {:mean 0.0 :variance 1.0}
      (let [n (count cohort)]
        {:mean (/ (reduce + (map (comp entity-healthy-mass second) cohort)) (double n))
         :variance (mean-entropy-variance cohort)}))))

(defn predict-attack-coverage
  "Predict observation distribution for the `:attack-coverage` channel.

   Same shape as `predict-support-coverage` over the cohort tagged
   `:attacks-A<N>`.  Mean healthy-mass measures how confidently the
   attacking-evidence entities have been settled (closed-with-strength).

   E-support-coverage Cycle 3."
  [belief entity-tags]
  (let [cohort (entities-with-tag-prefix belief entity-tags "attacks-")]
    (if (empty? cohort)
      {:mean 0.0 :variance 1.0}
      (let [n (count cohort)]
        {:mean (/ (reduce + (map (comp entity-healthy-mass second) cohort)) (double n))
         :variance (mean-entropy-variance cohort)}))))

(defn- coupled-repos
  [coupling-edges]
  (into #{}
        (mapcat (fn [{:keys [from to]}]
                  (keep identity [from to])))
        coupling-edges))

(defn- entities-in-coupled-repos
  [belief entity-repos coupling-edges]
  (let [repos (coupled-repos coupling-edges)]
    (filter (fn [[eid _]]
              (contains? repos (get entity-repos eid)))
            belief)))

(defn predict-coupling-density
  "Predict observation distribution for `:coupling-density`.

   Temporal coupling is repo-level, while belief is entity-level. This
   bridges them by mapping stack-annotation entities to repos, selecting
   entities whose repo participates in a temporal-coupling edge, and
   averaging non-dormant belief mass over that cohort. Empty bridge/cohort
   returns maximal uncertainty."
  [belief entity-repos coupling-edges]
  (let [cohort (entities-in-coupled-repos belief entity-repos coupling-edges)]
    (if (empty? cohort)
      {:mean 0.0 :variance 1.0}
      (let [n (count cohort)]
        {:mean (/ (reduce + (map (comp entity-nondormant-mass second) cohort))
                  (double n))
         :variance (mean-entropy-variance cohort)}))))

(defn- tick-result-tags
  [tick-results]
  (into #{}
        (keep (fn [t]
                (some->> (:id t) name (keyword "tick"))))
        tick-results))

(defn- entities-with-tick-tags
  [belief entity-ticks tick-results]
  (let [wanted (tick-result-tags tick-results)]
    (filter (fn [[eid _]]
              (seq (set/intersection
                    wanted
                    (get entity-ticks eid #{}))))
            belief)))

(defn predict-ticks-firing-ratio
  "Predict observation distribution for `:ticks-firing-ratio`.

   Tick results are logic-model-level checks; belief is entity-level. This
   bridge selects first-class tick entities from stack annotations whose
   `:tick/<id>` tags correspond to evaluated tick results, then averages
   open/active belief mass over that cohort. Open tick entities predict
   firing constraint warnings."
  [belief entity-ticks tick-results]
  (let [cohort (entities-with-tick-tags belief entity-ticks tick-results)]
    (if (empty? cohort)
      {:mean 0.0 :variance 1.0}
      (let [n (count cohort)]
        {:mean (/ (reduce + (map (comp entity-open-mass second) cohort))
                  (double n))
         :variance (mean-entropy-variance cohort)}))))

(def channels-with-likelihood
  "Set of observation channels for which an R3a likelihood model exists.
   v0.11: 4 channels (annotation-health, sorry-count-norm, mission-health,
   active-repo-ratio).
   E-support-coverage Cycle 3 (2026-05-26): +2 channels (support-coverage,
   attack-coverage), bringing total to 6.
   WM pilot cycle 2 (2026-05-30): +1 channel (coupling-density), bridging
   repo-level temporal-coupling edges to entity-level belief by source repo.
   WM pilot cycle 4 (2026-05-30): +1 channel (ticks-firing-ratio), bridging
   logic-model tick checks to first-class tick entities in stack annotations.
   Remaining sorries in
   `futon2/data/sorrys.edn` stay `:prototyping-forward` pending their own
   prerequisites; the 6 reclassified on cg-18b7831b were `:n-a-by-design`."
  #{:annotation-health :sorry-count-norm :mission-health :active-repo-ratio
    :support-coverage :attack-coverage :coupling-density :ticks-firing-ratio})

;; ---------------------------------------------------------------------------
;; v0.24 (M-aif-faithfulness B-3a): the CHANNEL BLOCK of A.
;;
;; The predict-* likelihood models above ARE the channel half of the
;; observation model — each channel's mean is a per-entity dot product of
;; the posterior with a fixed status→emission row, aggregated over a
;; cohort. Those rows were scattered (annotation-health-status-weights +
;; the open/healthy/nondormant mass helpers); this matrix names them in
;; one place so R3's "gm: A" label has an explicit referent covering the
;; channel dimension too.
;;
;; HONESTY: this is a documentation-grade UNIFICATION, consistency-tested
;; against the live helpers (belief-test), NOT the executing path — the
;; predict-* fns keep their own arithmetic so live prediction is
;; byte-identical. Rewiring them to read from this matrix is a follow-on,
;; gated like any behaviour-touching change. Channels outside
;; `channels-with-likelihood` have no emission row (no likelihood model
;; exists — callers fall back to preference-gap scoring), which is why
;; the block is 8 of the 13–14 schema channels, stated rather than hidden.
;; ---------------------------------------------------------------------------

(def ^:private open-status-row
  {:spawned 1.0 :refined 1.0 :reopened 1.0
   :strengthened 0.0 :addressed 0.0 :falsified 0.0 :foreclosed 0.0})

(def ^:private healthy-status-row
  {:strengthened 1.0 :addressed 1.0
   :spawned 0.0 :refined 0.0 :reopened 0.0 :falsified 0.0 :foreclosed 0.0})

(def ^:private nondormant-status-row
  {:spawned 1.0 :refined 1.0 :reopened 1.0 :strengthened 1.0 :addressed 1.0
   :falsified 0.0 :foreclosed 0.0})

(def channel-emission-matrix
  "The channel block of the explicit observation model A:
   {channel {status emission-weight}} for every channel in
   `channels-with-likelihood`. Each row is the status→value projection
   the corresponding predict-* fn computes its mean from (per entity,
   dot product with the posterior; :annotation-health additionally
   rescales its raw [-1,1] row to [0,1], see `entity-expected-health`).
   Read-only view — see the HONESTY block above."
  {:annotation-health  annotation-health-status-weights
   :sorry-count-norm   open-status-row
   :mission-health     healthy-status-row
   :active-repo-ratio  nondormant-status-row
   :support-coverage   healthy-status-row
   :attack-coverage    healthy-status-row
   :coupling-density   nondormant-status-row
   :ticks-firing-ratio open-status-row})

(def ^:dynamic *r3d-multichannel?*
  "FLAG (default ON as of 2026-07-08, Joe-directed): when true, the R3d
   belief-update driver aggregates signed precision-weighted errors across all 8
   likelihood channels (using channel-health-signs). When false, the driver draws
   from :annotation-health alone — byte-identical to pre-v0.25 behavior (bind
   false to restore). Deliberation-side, world-inert: changes how belief updates
   when the ranker runs; latent until R10 (the live loop) runs."
  true)

(def ^:dynamic *carry-belief?*
  "FLAG (default ON): when true, the strategic belief map mu carries across
   ticks (R8) — this tick's prior is the previous tick's posterior, reconciled
   to the current entity domain via `reconcile-belief-carry`. When false, each
   tick starts from a fresh uniform bootstrap (bind false to restore that).
   Deterministic; deliberation-side."
  true)

(def channel-health-signs
  "Per-channel health direction: +1 if high observed = healthy (positive
   error = system better than predicted → strengthen belief), −1 if high
   observed = unhealthy (positive error = system worse than predicted →
   weaken belief).

   Used by r3d-aggregate-driver for the 8-channel signed aggregation.
   When *r3d-multichannel?* is OFF, only :annotation-health drives the
   update (this map is unused in that path).

   Design note: futon2/holes/r1-r3-disaggregation-bridge.md."
  {:annotation-health  1   ; high = more healthy
   :sorry-count-norm  -1   ; high = more sorrys = less healthy
   :mission-health     1   ; high = more missions addressed
   :active-repo-ratio  1   ; high = more active development
   :support-coverage   1   ; high = more evidence coverage
   :attack-coverage    1   ; high = more evidence coverage
   :coupling-density   1   ; high = more interconnected
   :ticks-firing-ratio 1}) ; high = more ticks passing

(defn r3d-aggregate-driver
  "Aggregate per-channel prediction errors into a single signed R3d driver.

   WEIGHTED-ERRORS is a map: channel-id → {:weighted-error <num> :precision <num> ...}
   (as produced by free-energy/compute-prediction-error + precision/weighted-error).

   Returns a single signed scalar:
   - Positive = system healthier than predicted across channels
   - Negative = system less healthy than predicted
   - Near zero = accurate predictions or balanced signals

   Uses the PRECISION-WEIGHTED AVERAGE (canonical predictive coding):
   driver = Σ(sign_c × precision_c × error_c) / Σ(precision_c)
   so low-precision channels dilute proportionally.

   When *r3d-multichannel?* is OFF, returns the :annotation-health
   weighted-error alone (the pre-v0.25 behavior, byte-identical)."
  [weighted-errors]
  (if *r3d-multichannel?*
    ;; 8-channel signed precision-weighted average
    (let [signed-sum (reduce +
                             (for [[ch err-map] weighted-errors
                                   :let [sign (double (get channel-health-signs ch 0))
                                         we (double (:weighted-error err-map 0.0))]]
                               (* sign we)))
          total-precision (reduce +
                                  (for [[ch err-map] weighted-errors
                                        :let [sign (double (get channel-health-signs ch 0))
                                              prec (double (:precision err-map 0.0))]
                                        :when (not (zero? sign))]
                                    prec))]
      (if (pos? total-precision)
        (/ signed-sum total-precision)
        0.0))
    ;; OFF: annotation-health weighted-error alone (pre-v0.25 byte-identical)
    (double (:weighted-error (get weighted-errors :annotation-health {}) 0.0))))

(defn predict-observation
  "Predict observation distributions across all channels for which a
   likelihood model exists. Returns a map of channel-id → `{:mean :variance}`.

   Single-arg form: returns predictions for the 4 entity-tag-independent
   channels (back-compatible with pre-E-support-coverage callers).

   Two-arg form: adds `:support-coverage` and `:attack-coverage`.

   Three-arg form also adds context-derived channels:
   - `:coupling-density` from `:entity-repos` + `:coupling-edges`
   - `:ticks-firing-ratio` from `:entity-ticks` + `:tick-results`

   CONTEXT is either a map or a legacy raw coupling-edge collection.

   Channels in `channels-with-likelihood` are included; others are absent
   (callers handle absence by falling back to preference-gap-driven scoring)."
  ([belief]
   {:annotation-health (predict-annotation-health belief)
    :sorry-count-norm (predict-sorry-count-norm belief)
    :mission-health (predict-mission-health belief)
    :active-repo-ratio (predict-active-repo-ratio belief)})
  ([belief entity-tags]
   (assoc (predict-observation belief)
          :support-coverage (predict-support-coverage belief entity-tags)
          :attack-coverage  (predict-attack-coverage belief entity-tags)))
  ([belief entity-tags context]
   (let [{:keys [entity-repos coupling-edges entity-ticks tick-results]}
         (if (map? context)
           context
           {:coupling-edges context})]
     (assoc (predict-observation belief entity-tags)
            :coupling-density
            (predict-coupling-density belief
                                      (or entity-repos {})
                                      (or coupling-edges []))
            :ticks-firing-ratio
            (predict-ticks-firing-ratio belief
                                        (or entity-ticks {})
                                        (or tick-results []))))))
