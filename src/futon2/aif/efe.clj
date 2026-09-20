(ns futon2.aif.efe
  "Expected Free Energy computation for the WM AIF apparatus.

   `compute-efe` scores a single `(state, action)` pair by composing R4
   (predictive forward model) with the R3c free-energy decomposition.
   `rank-actions` scores a candidate-action sequence and orders it by the
   multi-objective controller score (lower = more preferred). The result also
   exposes `:G-efe` as the canonical risk-plus-ambiguity boundary.

   Contract: contributes to R5 (EFE with at least two principled terms)
   per `futon2/docs/futon-aif-completeness.md`. The two principled
   terms required by R5:

     R5a — pragmatic / risk     = G-risk
     R5b — epistemic / ambiguity = G-ambiguity

   Both are computed against R4's forward-model output, NOT against the
   current observation. This is the structural difference from
   `futon2.aif.free-energy/compute-controller-diagnostics` — which computes the legacy
   current-observation controller diagnostics; this namespace scores predicted
   next-observation under each candidate action.

   Cross-maps to F4 (bounded self-balance) and F6 (operator inhabitation)
   at stack scope: F4 via the predicted-distance-from-preferences signal
   the risk term provides; F6 via the ranked recommendations the
   operator reads and acts on.

   Theory: AIF Expected Free Energy decomposition. In the production default,
   ambiguity is Gaussian observation entropy and risk is outcome divergence
   from C. The separately named controller controls are not EFE terms."
  (:require [clojure.set :as set]
            [futon2.aif.cascade-free-energy :as cascade-free-energy]
            [futon2.aif.cascade-model-manifest :as cascade-manifest]
            [futon2.aif.forward-model :as fm]
            [futon2.aif.free-energy :as fe]
            [futon2.aif.preferences :as pref]
            [futon2.aif.preference-module :as c-module]
            [futon2.aif.c-vector :as cv]
            [futon2.aif.disposition-risk :as disposition]
            [futon2.aif.machine-q-risk :as machine-q-risk]
            [futon2.aif.move-class-intensity :as move-intensity]))

(defn- ^:clj-kondo/ignore ambiguity
  "R5b epistemic term over per-channel predicted variances.

   Modes (M-evaluate-policies D5c — compute-efe's default flipped to
   :gaussian-entropy on 2026-07-08; this fn's own arity-1 default stays
   :variance-sum):
     :variance-sum      (DEFAULT) — sum of per-channel predicted variance;
                        byte-identical to the historical behaviour.
     :gaussian-entropy  — Σ_ch ½·ln(2πe·σ²), the audit's repair toward the
                        canonical E_Q(s|π)[H[P(o|s)]] under a Gaussian channel
                        model (r18-badges :G-ambiguity :repair). Variance is
                        floored at 1e-9 so a zero-variance channel yields a
                        large-negative finite entropy, never -Inf.

   Higher = more uncertain predicted outcome → higher EFE contribution →
   less preferred action."
  ([variance-map] (ambiguity variance-map :variance-sum))
  ([variance-map mode]
   (case mode
     :gaussian-entropy
     (reduce + 0.0 (map (fn [v]
                          (* 0.5 (Math/log (* 2.0 Math/PI Math/E
                                              (max (double v) 1e-9)))))
                        (vals variance-map)))
     (reduce + (vals variance-map)))))

(defn- ambiguity-by-channel
  "The additive channel decomposition of `ambiguity`. Kept beside the
   aggregate so a shadow scorer can remove an absent channel without guessing
   how the aggregate was formed."
  [variance-map mode]
  (into {}
        (map (fn [[ch v]]
               [ch (case mode
                     :gaussian-entropy
                     (* 0.5 (Math/log (* 2.0 Math/PI Math/E
                                         (max (double v) 1e-9))))
                     (double v))]))
        variance-map))

;; ---------------------------------------------------------------------------
;; v0.13 controller augmentation: predictability and homeostatic controls ported from
;; ants/aif/policy.clj (the cyberants reference implementation).
;; Both compose with R5a (risk) + R5b (ambiguity); neither replaces them.
;; ---------------------------------------------------------------------------

(def default-info-weight 0.4)
(def default-survival-weight 1.2)
(def default-structural-pressure-weight 0.35)
(def default-graph-applicability-penalty 1000.0)
(def default-graph-body-weight 3.0)
(def default-graph-ascent-weight 20.0)
(def default-graph-off-map-penalty 0.0)
(def default-gap-weight 6.0)
(def default-model-uncertainty-weight 1.0)

(def default-survey-eig-weight
  "U24. Exchange rate between the epistemic payload a `:survey-mission`
   candidate carries (`:survey-eig-nats`, from
   `futon2.aif.survey-mission-value`) and G. ZERO BY DEFAULT: at 0.0 the
   `:survey-eig` key is absent from `:augmentation-terms` altogether and
   `:controller-score` is byte-identical, so no recorded number moves. The nats
   and G-risk/G-ambiguity are already in the same units, so this scalar is a
   pure exchange rate and not a unit conversion; declaring it non-zero on the
   default path is Joe's (worklist :J8)."
  0.0)

(def legacy-control-mode :controller-augmentation)
(def retired-control-mode :telemetry-only)
(def legacy-graph-feasibility-mode :score-penalty)
(def policy-support-mode :policy-support)

;; B-0a tick provenance (M-aif-faithfulness §2.0): exposed as a def — not an
;; inline :or literal — so the :wm-version stamp records the value compute-efe
;; actually resolves when the caller passes no :kl-channel-weights, instead of
;; a re-typed literal that could silently drift. {} ⇒ every channel weight 1.0
;; (UNIFORM — the joint KL under channel independence, the canonical config;
;; :pragmatic-parity is a comparability preset only).
(def default-kl-channel-weights {})

(def preference-stack-record
  "R19-D2a transcription of `holes/labs/wm-contract/R19-preference-stack.edn`
   at futon2 commit `17d779d`, reduced to the Lean `PreferenceLayerRecord`
   mirror `{:layer/id :source :author :basis :folded? :site}`."
  [{:layer/id :floor
    :source :operator-declared
    :author "Joseph Corneli"
    :basis "src/futon2/aif/preferences.clj sha256 22ae618a61cb3a4198b431cac214390337e29a01017ef302feb02618ef41e86a; ranges introduced by commit 135efdff5483dd297ce8b3670fe041b4283e6e92"
    :folded? true
    :site "src/futon2/aif/efe.clj:601-614,725-733"}
   {:layer/id :capability-zone-load
    :source :learned-from-operator
    :author "wm-outer-loop, implementation authored by Joseph Corneli"
    :basis "substrate-2 live probe 2026-08-30: 242 code/v05/wm-hyperparameter-update records, 14 classes, max :as-of 2026-07-18T22:33:41+01:00"
    :folded? true
    :site "src/futon2/aif/preferences.clj:140-173 and src/futon2/aif/efe.clj:586-600"}
   {:layer/id :live-goal-outcomes
    :source :corpus-derived
    :author "futon2.aif.c-vector/entries-from-corpus, authored by Joseph Corneli"
    :basis "substrate-2 :7071 live probe 2026-08-30: corpus signature -1131096431, 36 capabilities, 293 sorries"
    :folded? true
    :site "src/futon2/aif/c_vector.clj:160-184 and src/futon2/aif/efe.clj:655-665"}
   {:layer/id :c-vector-overlays
    :source :script-produced
    :author "futon6/scripts/c_vector.bb, authored by Joseph Corneli"
    :basis "2026-06-26 overlay snapshots: mess sha256 ffb40d5f9d3c026d41e1bd2b6047efcf9e5bddbf3dd1a8c7acc667e37fb2a4fd; incomplete sha256 f6c3e137d721f9a4936a176d4991373b18b64fbdcc58b0e600944c006ac3c554; yingvoice sha256 84a0ed72aa7c277e702ec5699de2ba94c7c7d4f00fc414d3aa858c91a47eec4f"
    :folded? true
    :site "src/futon2/aif/c_vector.clj:191-212"}
   {:layer/id :habit-prior
    :source :learned-from-operator
    :author "unknown operator whose selections are recorded in wm-trace"
    :basis "data/wm-trace/wm-trace-2026-08-30.edn sha256 6da3ccdab1dc4ef32d160d4b9ebcbe4bc6c654d529c2e0873971dd4f3aa06a05"
    :folded? false
    :site "Mode-dependent: :controller-augmentation keeps structural pressure additively in controller-score at src/futon2/aif/efe.clj:700-703,725-733; today's default :habit-prior replaces it with learned ln E(pi) at src/futon2/aif/habit_prior.clj:121-136 and scripts/futon2/report/war_machine.clj:4465-4472, then policy/select-action consumes that unscaled log prior at src/futon2/aif/policy.clj:368-380. OWNER AMENDMENT 2026-08-30 23:35Z (node-sim finding, verified policy.clj:234-270): in the branch the WM runs (:selection-boundary :strategic-recommendation, war_machine.clj:4503) the scores including ln E(pi) order an INSPECTABLE COUNTERFACTUAL only — chosen = (or (first controller-entries) ...), :habit-prior-applied? false; war_machine.clj:364 declares :scheduler-habit-authority :counterfactual-only. Computed and recorded at the seam; declared abstention from choosing."}])

;; v0.14: anticipation-driven scaling. When opts carries :time-pressure
;; in [0,1], G-risk and homeostatic-pressure are multiplied by
;; (1 + time-pressure × default-time-pressure-scale). predictability-bonus is NOT
;; scaled — urgency makes risk and survival dominate, reducing the
;; relative weight of exploration.
(def default-time-pressure-scale 1.0)

(defn capability-satisfied?
  "True when a capability node is already bound/satisfied in the star-map."
  [graph cap-id]
  (= :satisfied (get-in graph [:capabilities cap-id :status])))

(defn mission-node [graph mission-id]
  (get-in graph [:missions mission-id]))

(defn mission-applicable?
  "INV-3 predicate for the graph scorer: every required capability is satisfied."
  [graph mission-id]
  (let [scope (:scope (mission-node graph mission-id))]
    (and (some? scope)
         (every? #(capability-satisfied? graph %) scope))))

(defn mission-single-cycle-leaf?
  "INV-4 slice predicate: applicable and one open hole."
  [graph mission-id]
  (and (mission-applicable? graph mission-id)
       (= 1 (long (or (:open-hole-count (mission-node graph mission-id)) 0)))))

(defn- goal-depths
  "capability-id -> distance from the pre-registered goal through :scope edges."
  [graph goal]
  (loop [frontier [[goal 0]]
         seen {}]
    (if-let [[cap-id depth] (first frontier)]
      (if (contains? seen cap-id)
        (recur (subvec (vec frontier) 1) seen)
        (let [parents (get-in graph [:capabilities cap-id :scope] [])]
          (recur (into (subvec (vec frontier) 1)
                       (map #(vector % (inc depth)) parents))
                 (assoc seen cap-id depth))))
      seen)))

(defn mission-ascent-progress
  "Credit for producing capabilities in the transitive scope of the operator goal.
   The goal is an input to the selector; this function never chooses it. When
   `status-aware?` is true, already-satisfied produced capabilities are skipped
   so ascent credit cannot be farmed from completed map nodes."
  ([graph goal mission-id] (mission-ascent-progress graph goal mission-id false))
  ([graph goal mission-id status-aware?]
   (let [depths (goal-depths graph goal)]
     (reduce
      + 0.0
      (for [cap-id (:produces (mission-node graph mission-id))
            :let [depth (get depths cap-id)]
            :when (and depth
                       (or (not status-aware?)
                           (not= :satisfied (get-in graph [:capabilities cap-id :status]))))]
        (/ 1.0 (inc (double depth))))))))

(defn graph-control-terms
  "Graph controller terms for a mission action. Lower total remains better:
   unbound :requires adds a high applicability gate; body-size is a penalty;
   ascent progress toward the pre-registered goal is a credit.

   B-2b struct split (M-aif-faithfulness §2.2; D8/§3.5): the applicability
   penalty (1000·[not-applicable], and the off-map penalty) is a DOMAIN
   RESTRICTION — canonical seat Π_feasible, a mask on the policy domain, not a
   value judgment — while body/ascent are a PRAGMATIC PROXY (value-flavoured).
   The split is exposed as `:graph-feasibility-penalty` (mask component) +
   `:graph-control-score-proxy` (value component). In historical
   `:score-penalty` mode, `:graph-control-score` retains the old sum. In
   `:policy-support` mode, `rank-actions` excludes infeasible policies before
   scoring and `:graph-control-score` contains only the pragmatic proxy."
  [graph goal action {:keys [graph-applicability-penalty graph-body-weight graph-ascent-weight
                             graph-off-map-penalty graph-body-mode graph-feasibility-mode
                             graph-ascent-status-aware? model-uncertainty-fn model-uncertainty-weight]
                      :or {graph-applicability-penalty default-graph-applicability-penalty
                           graph-body-weight default-graph-body-weight
                           graph-ascent-weight default-graph-ascent-weight
                           graph-off-map-penalty default-graph-off-map-penalty
                           graph-body-mode :whole
                           graph-feasibility-mode legacy-graph-feasibility-mode
                           graph-ascent-status-aware? false
                           model-uncertainty-fn (constantly 0.0)
                           model-uncertainty-weight default-model-uncertainty-weight}}]
  (let [mission-id (:target action)
        mission (mission-node graph mission-id)]
    (if (and graph goal (= :open-mission (:type action)))
      (if mission
        (let [applicable? (mission-applicable? graph mission-id)
              body-size (double (or (:open-hole-count mission) 0))
              progress (double (mission-ascent-progress graph goal mission-id
                                                        graph-ascent-status-aware?))
              model-uncertainty-bonus (* (double model-uncertainty-weight)
                         (double (model-uncertainty-fn mission-id mission)))
              applicability (if applicable? 0.0 (double graph-applicability-penalty))
              body (case graph-body-mode
                     :leaf (* (double graph-body-weight)
                              (if (mission-single-cycle-leaf? graph mission-id) 0.0 1.0))
                     :whole (* (double graph-body-weight) body-size)
                     (* (double graph-body-weight) body-size))
              ascent (* (double graph-ascent-weight) progress)
              proxy (- body ascent)
              score (if (= graph-feasibility-mode policy-support-mode)
                      proxy
                      (+ applicability proxy))]
          {:graph-applicability-penalty applicability
           :graph-body-penalty body
           :graph-ascent-credit ascent
           :model-uncertainty-bonus model-uncertainty-bonus
           :graph-control-score score
           ;; Preserve both sides of the split as telemetry even though only the
           ;; proxy enters the score in :policy-support mode.
           :graph-feasibility-penalty applicability
           :graph-control-score-proxy proxy
           :graph-feasibility-mode graph-feasibility-mode
           :graph/applicable? applicable?
           :graph/single-cycle-leaf? (mission-single-cycle-leaf? graph mission-id)})
        {:graph-applicability-penalty 0.0
         :graph-body-penalty 0.0
         :graph-ascent-credit 0.0
         :model-uncertainty-bonus 0.0
         :graph-control-score (if (= graph-feasibility-mode policy-support-mode)
                                0.0
                                (double graph-off-map-penalty))
         ;; B-2b: the off-map penalty is feasibility-class (a domain gate on
         ;; "not on the star map"), not a value — proxy carries none of it.
         :graph-feasibility-penalty (double graph-off-map-penalty)
         :graph-control-score-proxy 0.0
         :graph-feasibility-mode graph-feasibility-mode})
      {:graph-applicability-penalty 0.0
       :graph-body-penalty 0.0
       :graph-ascent-credit 0.0
       :model-uncertainty-bonus 0.0
       :graph-control-score 0.0
       :graph-feasibility-penalty 0.0
       :graph-control-score-proxy 0.0
       :graph-feasibility-mode graph-feasibility-mode})))

(defn policy-support-verdict
  "Classify an action before value scoring. In :policy-support mode the
  capability graph is authoritative for :open-mission policies: absent graph
  nodes and missions with unsatisfied scope are excluded, with an explicit
  reason. Other action classes retain their own proposer/guardrail gates."
  [graph action {:keys [graph-feasibility-mode]
                 :or {graph-feasibility-mode legacy-graph-feasibility-mode}}]
  (if (or (not= graph-feasibility-mode policy-support-mode)
          (nil? graph)
          (not= :open-mission (:type action)))
    {:feasible? true :reason :not-graph-gated}
    (let [mission (mission-node graph (:target action))]
      (cond
        (nil? mission)
        {:feasible? false :reason :mission-absent-from-capability-graph}

        (not (mission-applicable? graph (:target action)))
        {:feasible? false :reason :required-capabilities-unsatisfied
         :required (vec (:scope mission))}

        :else
        {:feasible? true :reason :graph-applicable}))))

(defn partition-policy-support
  "Return {:included actions :excluded [{:action :reason ...}]}. This is the
  Π_feasible boundary; excluded policies never receive a controller score."
  [graph actions opts]
  (reduce (fn [acc action]
            (let [verdict (policy-support-verdict graph action opts)]
              (if (:feasible? verdict)
                (update acc :included conj action)
                (update acc :excluded conj (assoc (dissoc verdict :feasible?)
                                                  :action action)))))
          {:included [] :excluded []}
          actions))

(defn- star-map-contribution?
  [graph-terms]
  (or (contains? graph-terms :graph/applicable?)
      (not (zero? (double (:graph-control-score graph-terms 0.0))))))

(defn- action-target-id
  [action]
  (let [target (:target action)]
    (cond
      (keyword? target) (if-let [ns-part (namespace target)]
                          (str ns-part "/" (name target))
                          (name target))
      (string? target) target
      :else (some-> target str))))

(defn gap-control-terms
  "Mission-fold gap credit for open-mission actions.

   The fold view maps mission id -> intrinsic gap-score in [0,1].  High gap
   means announced-but-unfilled structure, i.e. epistemic room to grow.  The
   weighted credit is subtracted from controller-score by compute-efe, mirroring the
   info/ascent direction: higher gap makes the mission more preferred."
  [mission-gap-view action {:keys [gap-weight]
                            :or {gap-weight default-gap-weight}}]
  (let [mission-id (action-target-id action)
        gap-score (double (or (get mission-gap-view mission-id) 0.0))
        gap (* (double gap-weight) gap-score)]
    (if (= :open-mission (:type action))
      {:gap-exploration-bonus gap
       :gap-score gap-score}
      {:gap-exploration-bonus 0.0
       :gap-score 0.0})))

(defn- gap-contribution?
  [gap-terms]
  (pos? (double (:gap-exploration-bonus gap-terms 0.0))))

(defn pre-registered-capability?
  [graph goal cap-id]
  (or (= goal cap-id)
      (true? (get-in graph [:capabilities cap-id :pre-registered?]))))

(defn safe-action?
  "INV-G selector boundary. Discovery can surface missing capability facts, but
   pursuit of non-pre-registered capabilities and goal-extending decompose moves
   require consent. Advancing past an operator-verify exit is also refused unless
   the gap is agreed or consented."
  [graph goal action]
  (let [consent? (true? (:consent-granted? action))]
    (case (:type action)
      :pursue
      (or (pre-registered-capability? graph goal (:target action)) consent?)

      :decompose
      (or (not (:extends-goal? action)) consent?)

      :open-mission
      (let [mission (mission-node graph (:target action))
            crosses? (or (:crosses-exit? action)
                         (:next-exit-operator-verify? mission))]
        (or (not crosses?) (:gap-agreed? action) consent?))

      true)))

(defn selection-trace-step
  "Translate a selected action into the abstract trace shape consumed by
   futon3c.logic.capability-star-map-invariants/q-buck and q-gate."
  ([graph goal action] (selection-trace-step graph goal action 1))
  ([graph goal action step]
   (let [mission-id (:target action)
         mission (mission-node graph mission-id)
         action-kind (case (:type action)
                       :pursue :pursue
                       :decompose :decompose
                       :open-mission :advance
                       (:type action))]
     (cond-> {:step step :action action-kind}
       (= :pursue action-kind)
       (assoc :capability (:target action)
              :pre-registered? (pre-registered-capability? graph goal (:target action)))

       (= :decompose action-kind)
       (assoc :mission mission-id
              :extends-goal? (true? (:extends-goal? action)))

       (= :advance action-kind)
       (assoc :mission mission-id
              :requires-sat? (mission-applicable? graph mission-id)
              :crosses-exit? (boolean (or (:crosses-exit? action)
                                          (:next-exit-operator-verify? mission)))
              :gap-agreed? (true? (:gap-agreed? action)))

       (:consent-granted? action)
       (assoc :consent-granted? true)))))

(defn- predictability-bonus
  "Variance-complement predictability control ported from ants/aif/policy.clj.

   Rewards actions whose predicted next-state has low variance. This is not
   expected information gain because it does not model posterior entropy
   reduction. Returns a non-negative scalar; higher = more predictable action.

   For v0.13: `predictability-bonus = Σ max(0, 1 − predicted-variance) over channels`.
   The 1.0 ceiling reflects that variances in this contract are in [0, 1]
   (per channel-bounded observations). When variance is 0 (deterministic
   prediction like `:no-op`), predictability-bonus is N-channels — the most predictable
   action by this measure. When variance is high, predictability-bonus is small.

   In EFE, predictability-bonus enters with a NEGATIVE sign (subtracted from
   controller-score) so predictable actions are preferred."
  [variance-map]
  (reduce + (for [v (vals variance-map)]
              (max 0.0 (- 1.0 (double v))))))

(defn- homeostatic-pressure
  "Survival-pressure term ported from ants/aif/policy.clj.

   Hinge-loss penalty over critical channels: for each channel that has
   a preference range, accumulate `max(0, gap-from-preference)`. Differs
   from G-risk (which uses pragmatic-weights × gap on the in-range
   criterion) by being a pure threshold-based penalty — only fires when
   a channel is OUT of preferred range, scaled uniformly by the survival
   weight.

   Higher = more strategic pressure → less preferred action.

   For v0.13: only acts on the 4 R3a-covered channels (those with
   likelihood models — `:annotation-health`, `:sorry-count-norm`,
   `:mission-health`, `:active-repo-ratio`). The other 10 channels
   contribute to G-risk via the existing pragmatic-weights but not to
   survival until their R3a likelihoods land."
  [observation-mean]
  (let [survival-channels #{:annotation-health :sorry-count-norm
                            :mission-health :active-repo-ratio}]
    (reduce + (for [[ch v] observation-mean
                    :when (contains? survival-channels ch)
                    :let [pref (get (pref/current-C) ch)]
                    :when pref
                    :let [[lo hi] pref
                          d (double v)
                          gap (cond (< d lo) (- lo d)
                                    (> d hi) (- d hi)
                                    :else 0.0)]]
                gap))))

(defn compute-efe
  "Score `(state, action)` via Expected Free Energy.

   Returns:
     {:action        <the action scored>
      :prediction    <full forward-model/predict output>
      :G-risk        <pragmatic term: gap from preferences on predicted mean
                       minus the action's :intrinsic-value (if any)>
      :G-ambiguity   <epistemic term: sum of per-channel predicted variance>
      :predictability-bonus        <predictability-bonus term (v0.13, negative weight in controller-score)>
      :homeostatic-pressure    <survival hinge-loss term (v0.13)>
      :structural-pressure <candidate-local structural-pressure term>
	     :gap-exploration-bonus        <mission-fold gap credit; negative weight in controller-score>
	     :G-goal-outcome <E-C-vector-live: additive risk = divergence of this
	                      action's PREDICTED goal-outcomes from the LIVE C-vector
	                      (the belly); advancing a goal lowers it ⇒ re-ranks
	                      policies; 0 when no live C ⇒ reduces to the static floor>
	     :preference-stack <the recorded five-layer preference stack mirrored
	                       from R19-D1; four layers fold into C-risk here and
	                       the habit prior remains recorded with `:folded? false`>
	     :controller-score       <G-risk + G-ambiguity − info-weight×predictability-bonus
	                      + survival-weight×homeostatic-pressure
	                      − structural-pressure-weight×structural-pressure
                       − gap-exploration-bonus + G-goal-outcome>
      :per-channel   <per-channel risk decomposition (from compute-controller-diagnostics)>}

   Lower :controller-score = more preferred action.

   v0.13 added two engineering controls ported from ants/aif/policy.clj:
   - predictability-bonus — rewards actions with lower predicted variance;
     this variance complement is not expected information gain.
   - homeostatic-pressure — penalises predicted-state
     channels that lie OUTSIDE their preference range; differs from
     G-risk (continuous Gaussian-flavoured) by being a pure threshold
     penalty. Only acts on the 4 R3a-covered channels.

   Optional `opts`:
     :info-weight         — default `default-info-weight` (0.4)
     :survival-weight     — default `default-survival-weight` (1.2)
     :structural-pressure-weight — default
                            `default-structural-pressure-weight` (0.35)
     :time-pressure       — v0.14 anticipation-driven urgency in [0,1];
                            scales G-risk + homeostatic-pressure by
                            `(1 + time-pressure × time-pressure-scale)`.
                            Default 0 (no anticipation-driven scaling).
     :time-pressure-scale — default `default-time-pressure-scale` (1.0).
     :survey-eig-weight   — U24. Exchange rate on a `:survey-mission`
                            candidate's `:survey-eig-nats` payload (see
                            `futon2.aif.survey-mission-value`). Default
                            `default-survey-eig-weight` (0.0), at which the
                            `:survey-eig` augmentation key is absent and
                            `:controller-score` is byte-identical. The leg is
                            SUBTRACTED (information gain is
                            preference-increasing), and is skipped entirely for
                            a candidate carrying no payload rather than
                            imputing 0.0 nats to it.
     :horizon-steps       — v0.15 opt-in multi-horizon scoring. When
                            >= 2, uses `predict-multi-horizon` and
                            scores against the FINAL-state observation
                            of the trajectory rather than the immediate
                            next-state. Default `nil` (single-step).
     :belief-update-opts  — options passed unchanged to the forward model's
                            belief filter at every rollout step. Default `{}`
                            preserves library compatibility; the live arena
                            supplies its resolved A/B/D mode.

   `:intrinsic-value` on the action map represents an *intrinsic credit*
   for actions whose value isn't captured by observation-vector changes.
   Default 0.

   `:structural-pressure-per-action` on the action map represents a
   candidate-local structural-pressure signal. Higher values reduce
   `:controller-score` via the weighted subtraction in the decomposition, making
   structurally load-bearing actions more preferred.

   `:mission-gap-view` in opts maps mission id -> gap-score in [0,1].  For
   `:open-mission` actions, the weighted gap credit is subtracted from
   `:controller-score`, making announced-but-unfilled missions more preferred.

   `:goal-outcome-entries` in opts overrides the live C-vector source (defaults
   to `c-vector/current-c-vector`); `:goal-outcome-weight` scales the
   mean-normalised goal-outcome risk (W4). With no live C the term is 0.0 —
   `compute-efe` is then identical to its pre-E-C-vector-live behaviour.

   `:graph-off-map-penalty` defaults to 0.0 and applies only to off-map
   `:open-mission` actions when graph + goal are present. `:graph-body-mode`
   defaults to `:whole`; `:leaf` scores a bounded next-step body term. When
   `:graph-ascent-status-aware?` is true, ascent credit ignores produced
   capabilities already marked `:satisfied`. `:model-uncertainty-fn` is called
   `(model-uncertainty-fn mission-id mission-node)` and returns that mission's
   posterior-spread bonus. This is an engineering exploration control, not
   expected information gain: at capability grain the injected fn reads
   `(:produces mission-node)`; at pattern grain it keys on `mission-id`
   (mission → patterns → constellation → stddev, resolve→distinct→sum
   internally). Defaults to `(constantly 0.0)` (variadic, ignores both).

   Result shape (B-2a honest labelling, M-aif-faithfulness §2.2): the output
   is a MULTI-OBJECTIVE ACTION SCORE WITH AN EFE CORE. `:G-core` (= risk +
   ambiguity, invariant I3) is the canonical G; `:augmentation-terms` names
   the eight non-core contributions exactly as they enter `:controller-score`
   (signed, weighted), and `:controller-augmentation` is their sum. `:controller-score` — the
   ranking key — is the historical blend, byte-identical across this
   relabelling; do not read it as canonical EFE (R18/D8).

   Pure: same (state, action, opts) → same output."
  ([state action] (compute-efe state action {}))
  ([state action {:keys [info-weight survival-weight structural-pressure-weight time-pressure
                         time-pressure-scale horizon-steps belief-update-opts capability-graph
                         pre-registered-goal graph-applicability-penalty
                         graph-body-weight graph-ascent-weight graph-off-map-penalty
                         graph-body-mode graph-ascent-status-aware? model-uncertainty-fn model-uncertainty-weight
                         mission-gap-view
                         gap-weight goal-outcome-weight goal-outcome-entries goal-outcome-prob-fn
                         goal-outcome-mode
                         ambiguity-mode risk-mode kl-channel-weights c-temperature
                         structural-pressure-mode move-class-intensity-mode
                         predictability-control-mode homeostatic-control-mode
                         graph-feasibility-mode
                         move-class-intensity-weight survey-eig-weight
                         ruled-outcome-c-enabled? disposition-kernel
                         ruled-outcome-c-weight seeded-c c-fold-provenance
                         machine-q]
                  :or {info-weight default-info-weight
                       survey-eig-weight default-survey-eig-weight
                       survival-weight default-survival-weight
                       structural-pressure-weight default-structural-pressure-weight
                       time-pressure 0.0
                       time-pressure-scale default-time-pressure-scale
                       graph-applicability-penalty default-graph-applicability-penalty
                       graph-body-weight default-graph-body-weight
                       graph-ascent-weight default-graph-ascent-weight
                       graph-off-map-penalty default-graph-off-map-penalty
                       graph-body-mode :whole
                       graph-ascent-status-aware? false
                       model-uncertainty-fn (constantly 0.0)
                       model-uncertainty-weight default-model-uncertainty-weight
                       gap-weight default-gap-weight
                       goal-outcome-weight cv/default-goal-outcome-weight
                       ;; 2026-07-08 (Joe-directed): code default aligned to the live-arena
                       ;; canonical modes — a WINNER-CHANGING faithfulness upgrade (real KL /
                       ;; Gaussian-entropy replace the analogical hinge / variance-sum). The
                       ;; arena (war_machine.clj) already defaulted here since 2026-07-03; env
                       ;; FUTON_WM_{RISK,AMBIGUITY}_MODE=hinge/variance-sum is the escape hatch.
                       ambiguity-mode :gaussian-entropy
                       risk-mode :kl
                       goal-outcome-mode :hinge
                       structural-pressure-mode :controller-augmentation
                       predictability-control-mode legacy-control-mode
                       homeostatic-control-mode legacy-control-mode
                       graph-feasibility-mode legacy-graph-feasibility-mode
                       move-class-intensity-mode :off
                       move-class-intensity-weight 1.0
                       ruled-outcome-c-enabled? false
                       ruled-outcome-c-weight 1.0
                       kl-channel-weights default-kl-channel-weights
                       c-temperature pref/default-c-temperature}}]
   (let [belief-update-opts (or belief-update-opts {})
         single-prediction (fm/predict state action belief-update-opts)
         ;; v0.15: opt-in multi-horizon trajectory; final-state observation
         ;; drives G-risk + homeostatic-pressure. Ambiguity + info still use the
         ;; FIRST-step prediction's variance (the agent's immediate
         ;; uncertainty about taking the action).
         multi (when (and horizon-steps (>= horizon-steps 2))
                 (fm/predict-multi-horizon state action horizon-steps
                                           belief-update-opts))
         prediction single-prediction
         next-mean (if multi
                     (get-in multi [:final-state :observation])
                     (get-in single-prediction [:next-observation :mean]))
         next-var (get-in single-prediction [:next-observation :variance])
         ;; C127: selection remains on the explicit legacy projection while
         ;; support-aware diagnostics are measured. Switching comparison
         ;; semantics is an operator decision, not a diagnostic migration.
         fe-on-predicted (fe/compute-controller-diagnostics
                          next-mean {:support-aware? false})
         intrinsic (double (or (:intrinsic-value action) 0))
         ;; D5a (M-evaluate-policies §8.6; contract E-C-vector-live.md:230):
         ;; :risk-mode :kl scores risk as Σ_ch w_ch · KL(N(μ_ch,σ²_ch) ‖ C_ch)
         ;; in nats over the preference densities (pref/c-distribution).
         ;; DARK by default (:hinge = byte-identical historical behaviour).
         ;; w_ch from :kl-channel-weights. A MAP (default {}) ⇒ per-channel weight,
         ;; missing = 1.0 (uniform) — byte-identical historical behaviour. The
         ;; keyword :pragmatic-parity (item 2, E-KL-refinements) ⇒ the SAME
         ;; `pref/pragmatic-weights` the hinge's g-pragmatic reduces over, with
         ;; missing channels contributing 0.0 (parity = zero-weight, not 1.0). This
         ;; preset lets E6-style hinge-vs-kl comparisons isolate the FUNCTIONAL
         ;; change from the WEIGHT change.
         parity? (= kl-channel-weights :pragmatic-parity)
         kcw (if parity? pref/pragmatic-weights kl-channel-weights)
         kcw-default (if parity? 0.0 1.0)
         ;; item 3 (E-KL-refinements) plumbing: :c-temperature is a SCALAR (the
         ;; default path — byte-identical) OR a map ch→T (per-channel; missing
         ;; channels fall back to pref/default-c-temperature). Enables the
         ;; per-channel-T candidate without committing to it; default stays 0.1.
         ch-temp (if (map? c-temperature)
                   (fn [ch] (get c-temperature ch pref/default-c-temperature))
                   (constantly c-temperature))
         learn-action? (= :learn-action-class (:type action))
         zone-evidence (when learn-action?
                         (pref/capability-zone-evidence (:target-class action)))
         zone-risk (if (:active? zone-evidence)
                     (case risk-mode
                       :kl (pref/kl
                            {:kind :bernoulli
                             :p (:predictive-probability zone-evidence)}
                            (pref/c-distribution
                             {:p1 (:load-weight zone-evidence)}
                             :temperature (ch-temp pref/c-zone-load-key)))
                       (Math/abs
                        (- (double (:predictive-probability zone-evidence))
                           (double (:load-weight zone-evidence)))))
                     0.0)
         channel-risk-terms
         (case risk-mode
           :kl
           (into {}
                 (for [[ch spec] (pref/current-C)
                       :let [mu (get next-mean ch)
                             s2 (get next-var ch)]
                       :when (and mu s2)]
                   [ch (* (double (get kcw ch kcw-default))
                          (pref/kl {:kind :gaussian :mu mu :sigma2 s2}
                                   (pref/c-distribution spec
                                                        :temperature (ch-temp ch))))]))
           (into {}
                 (for [[ch {:keys [gap]}] (:per-channel fe-on-predicted)]
                   [ch (* (double (get pref/pragmatic-weights ch 0.0))
                          (double gap))])))
         channel-risk (reduce + 0.0 (vals channel-risk-terms))
	         disposition-risk (when ruled-outcome-c-enabled?
	                            (disposition/disposition-risk
	                             next-mean disposition-kernel seeded-c))
	         ruled-outcome-c-contribution
	         (if disposition-risk
	           (* (double ruled-outcome-c-weight) disposition-risk)
	           0.0)
	         machine-q-evaluation
	         (when machine-q
	           (let [q ((:provider machine-q) state action)
	                 risk (machine-q-risk/risk q (:c machine-q))
	                 weight (double (get machine-q :weight 1.0))]
	             {:q q :c (:c machine-q) :risk risk :weight weight
	              :G-machine-q-risk (* weight (:risk risk))}))
	         machine-q-contribution
	         (if machine-q-evaluation (:G-machine-q-risk machine-q-evaluation) 0.0)
	         ;; foldC layer ids: :floor from channel-risk + :capability-zone-load
	         ;; from zone-risk and :ruled-outcome-c from the disposition bridge
	         ;; compose here as the recorded C-risk prefix.
	         g-risk (+ channel-risk zone-risk ruled-outcome-c-contribution
	                   machine-q-contribution)
         ambiguity-terms (if learn-action?
                           {}
                           (ambiguity-by-channel next-var ambiguity-mode))
         g-ambig (if learn-action?
                   (:predictive-variance zone-evidence)
                   (reduce + 0.0 (vals ambiguity-terms)))
         g-info (predictability-bonus next-var)
         g-survival-base (homeostatic-pressure next-mean)
         g-structural-pressure (double (or (:structural-pressure-per-action action) 0.0))
         graph-terms (graph-control-terms capability-graph pre-registered-goal action
                                      {:graph-applicability-penalty
                                       graph-applicability-penalty
                                       :graph-body-weight
                                       graph-body-weight
                                       :graph-ascent-weight
                                       graph-ascent-weight
                                       :graph-off-map-penalty
                                       graph-off-map-penalty
                                       :graph-body-mode
                                       graph-body-mode
                                       :graph-feasibility-mode graph-feasibility-mode
                                       :graph-ascent-status-aware?
                                       graph-ascent-status-aware?
                                       :model-uncertainty-fn model-uncertainty-fn
                                       :model-uncertainty-weight model-uncertainty-weight})
         gap-terms (gap-control-terms mission-gap-view action
                                  {:gap-weight gap-weight})
         ;; E-C-vector-live: the LIVE goal-outcome half of C contributes an
         ;; additive risk term — divergence of the policy's PREDICTED goal
         ;; outcomes from C (entries this action advances are predicted
         ;; satisfied ⇒ the action's risk drops ⇒ the belly re-ranks policies).
         ;; Source defaults to the maintained live C-vector; [] (never derived /
         ;; store down) ⇒ 0.0 ⇒ EFE reduces to the static floor (regression-
         ;; safe). Reduces to the static term when the action advances nothing.
         ;; D-1e (M-aif-faithfulness §2.1, operator flip 2026-07-04):
         ;; :goal-outcome-mode :kl scores the :becomes entries by the exact
         ;; Bernoulli KL against pref/c-distribution (nats; range entries keep
         ;; the hinge — their Gaussian Q is the channel lane above). :hinge =
         ;; byte-identical historical behaviour (the library default; the
         ;; ARENA resolves the live mode — arena-goal-outcome-mode in
         ;; war_machine.clj, FUTON_WM_GOAL_OUTCOME_MODE=hinge escape hatch).
         ;; The Bernoulli T is the scalar :c-temperature; a per-channel MAP
         ;; falls back to the default (goal-outcomes are not channels).
         goal-outcome-evaluation
         (let [entries (or goal-outcome-entries (cv/current-c-vector))
               prob-fn (or goal-outcome-prob-fn cv/credit-satisfy-prob)]
           (cv/goal-outcome-evaluation
            goal-outcome-mode entries action capability-graph goal-outcome-weight
            prob-fn (if (map? c-temperature)
                      pref/default-c-temperature
                      c-temperature)))
         g-goal-outcome (:score goal-outcome-evaluation)
         ;; U24: the epistemic leg of a :survey-mission candidate. NEGATIVE by
         ;; construction (information gain is preference-increasing, the sign
         ;; Holes.G := risk - eig gives it), and nil unless BOTH the weight is
         ;; positive and the candidate actually carries a measured payload — so
         ;; an unenriched survey candidate scores exactly as it did before this
         ;; key existed, rather than at an imputed zero that looks measured.
         survey-eig-contribution
         (when (and (pos? (double survey-eig-weight))
                    (number? (:survey-eig-nats action)))
           (- (* (double survey-eig-weight) (double (:survey-eig-nats action)))))
         move-class-intensity (when (= :v1 move-class-intensity-mode)
                                (move-intensity/intensity action))
         move-class-contribution (when move-class-intensity
                                   (- (* (double move-class-intensity-weight)
                                         (double (:value move-class-intensity)))))
         urgency (+ 1.0 (* (double time-pressure) (double time-pressure-scale)))
         ;; Preserve the historical controller behaviour while keeping the
         ;; canonical risk quantity unit-pure. Intrinsic credit and urgency are
         ;; controller controls, so their signed effect is explicit below.
         effective-risk (* (- g-risk intrinsic) urgency)
         risk-control (- effective-risk g-risk)
         g-survival (* g-survival-base urgency)
         predictability-active? (= predictability-control-mode legacy-control-mode)
         homeostatic-active? (= homeostatic-control-mode legacy-control-mode)
         info-contribution (if predictability-active?
                             (- (* (double info-weight) g-info))
                             0.0)
         survival-contribution (if homeostatic-active?
                                 (* (double survival-weight) g-survival)
                                 0.0)
         ;; HONESTY (D8 reconciliation; C6 wording; B-2a struct split): what
         ;; follows is a MULTI-OBJECTIVE SCORE WITH AN EFE CORE, not canonical
         ;; EFE. The core (risk + ambiguity — :G-core, invariant I3) is the
         ;; canonical G; the eight remaining contributions are the augmentation
         ;; layer — a flattened generative model (C-terms, E-term, Π-mask
         ;; projected into one sum; argue-exhibit pp. 8–9). :controller-score keeps its
         ;; historical summation order and value BYTE-IDENTICALLY; the layer
         ;; below only NAMES the same quantities (float associativity means
         ;; :G-core + :controller-augmentation matches :controller-score to ~1e-15, not to the
         ;; bit — asserted at 1e-9 in efe_struct_split_test).
	         ;; D-1d / R19-D2a: Joe flipped the live default on 2026-07-13.
	         ;; war_machine.clj:247-268 makes :habit-prior the default mode and
	         ;; :learned-frequency the default source; the env vars are rollback
	         ;; hatches only. The object's home is the policy seam consuming
	         ;; ln E(pi), with R14 supplying tau. policy.clj:234-270 records the
	         ;; seam's authority as :counterfactual-only: the habit prior is
	         ;; computed and recorded there, but abstains from choosing. This
	         ;; local branch still exposes both modes because
	         ;; :controller-augmentation is the historical rollback path.
	         sp-contribution (- (* (double structural-pressure-weight)
	                               g-structural-pressure))
         habit-prior? (= structural-pressure-mode :habit-prior)
         augmentation-terms (cond-> {:risk-control risk-control
                                     :info info-contribution
                                     :survival survival-contribution
                                     :structural-pressure sp-contribution
                                     :graph-control (:graph-control-score graph-terms)
                                     :model-uncertainty-bonus (- (:model-uncertainty-bonus graph-terms))
                                     :gap (- (:gap-exploration-bonus gap-terms))
                                     :goal-outcome g-goal-outcome}
                              habit-prior? (dissoc :structural-pressure)
                              move-class-contribution
                              (assoc :move-class-intensity move-class-contribution)
                              survey-eig-contribution
                              (assoc :survey-eig survey-eig-contribution))
	         g-total-base (+ effective-risk
	                         g-ambig
	                         info-contribution
	                         survival-contribution
	                         (if habit-prior? 0.0 sp-contribution)
	                         (:graph-control-score graph-terms)
	                         (- (:model-uncertainty-bonus graph-terms 0.0))
	                         (- (:gap-exploration-bonus gap-terms))
	                         ;; foldC layer id: :live-goal-outcomes composes here.
	                         g-goal-outcome)
         g-total-mci (if move-class-contribution
                       (+ g-total-base move-class-contribution)
                       g-total-base)
         g-total (if survey-eig-contribution
                   (+ g-total-mci survey-eig-contribution)
                   g-total-mci)
         ;; C108 shadow-only decomposition. These terms do not enter scoring;
         ;; they restate the already-computed sum per observation channel so
         ;; the trace boundary can omit absent channels exactly. Everything
         ;; not observation-channel-indexed is retained as one residual.
         survival-channels #{:annotation-health :sorry-count-norm
                             :mission-health :active-repo-ratio}
         channel-terms
         (into {}
               (for [ch (set (concat (keys channel-risk-terms)
                                     (keys ambiguity-terms)
                                     (keys next-var)))
                     :let [variance (get next-var ch)
                           info-term (if (and predictability-active? variance)
                                       (- (* (double info-weight)
                                             (max 0.0 (- 1.0 (double variance)))))
                                       0.0)
                           survival-gap (if (and homeostatic-active?
                                                 (contains? survival-channels ch)
                                                 (contains? next-mean ch)
                                                 (get (pref/current-C) ch))
                                          (let [[lo hi] (get (pref/current-C) ch)
                                                d (double (get next-mean ch))]
                                            (cond (< d lo) (- lo d)
                                                  (> d hi) (- d hi)
                                                  :else 0.0))
                                          0.0)]]
                 [ch (+ (* urgency (double (get channel-risk-terms ch 0.0)))
                        (double (get ambiguity-terms ch 0.0))
                        info-term
                        (* (double survival-weight) urgency survival-gap))]))
         channel-total (reduce + 0.0 (vals channel-terms))
         support-shadow-terms
         {:by-channel channel-terms
          :non-channel-contribution (- g-total channel-total)}]
     (cond->
      (merge
	       {:action action
	        :prediction prediction
	        :G-risk g-risk
	        :G-ruled-outcome-c ruled-outcome-c-contribution
	        :predicted-disposition-risk disposition-risk
	        :G-ambiguity g-ambig
        :predictability-bonus g-info
        :homeostatic-pressure g-survival
	        :structural-pressure g-structural-pressure
	        :G-goal-outcome g-goal-outcome
	        :goal-outcome-replay-inputs (:evidence goal-outcome-evaluation)
	        :preference-stack preference-stack-record
	        ;; D2 (M-evaluate-policies §8.3): the canonical EFE CORE, reported
        ;; separately from the multi-objective blend — :G-core = risk +
        ;; ambiguity exactly (invariant I3). Pure addition; :controller-score unchanged.
        :G-core (+ g-risk g-ambig)
        ;; Honest canonical boundary: risk + ambiguity is the implemented EFE
        ;; decomposition. The BMR posterior-spread signal is not an expectation
        ;; of posterior information gain, so it remains quarantined in the
        ;; controller augmentation and MUST NOT be presented as an EFE leg.
        :G-efe (+ g-risk g-ambig)
        ;; B-2a (M-aif-faithfulness §2.2): the multi-objective augmentation
        ;; layer, named — the eight non-core contributions AS THEY ENTER
        ;; :controller-score (signed, weighted). :controller-augmentation is their sum. Additive
        ;; keys; whitelisted in trace.clj AT BIRTH (:score-provenance lesson);
        ;; trace-schema-version bumped 2→3.
        :controller-augmentation (reduce + 0.0 (vals augmentation-terms))
        :augmentation-terms augmentation-terms
        ;; D5a: which risk functional produced :G-risk — whitelisted in
        ;; trace.clj AT BIRTH (the :score-provenance lesson).
        :risk-mode risk-mode
        ;; D5c: which ambiguity functional produced :G-ambiguity — same
        ;; provenance rule as :risk-mode; the arena may run the nats lane while
        ;; library defaults stay byte-identical.
        :ambiguity-mode ambiguity-mode
        ;; D-1e: which functional produced :G-goal-outcome — whitelisted in
        ;; trace.clj AT BIRTH, same lesson as :risk-mode.
        :goal-outcome-mode goal-outcome-mode
        ;; D-1d: where structural pressure sits — whitelisted at birth.
        :structural-pressure-mode structural-pressure-mode
        :predictability-control-mode predictability-control-mode
        :homeostatic-control-mode homeostatic-control-mode
        :graph-feasibility-mode graph-feasibility-mode
        :controller-score g-total
        :support-shadow-terms support-shadow-terms
        :time-pressure (double time-pressure)
        :horizon-steps (when multi (:horizon-steps multi))
        :per-channel (cond-> (:per-channel fe-on-predicted)
                       learn-action?
                       (assoc pref/c-zone-load-key
                              (assoc zone-evidence :risk zone-risk)))}
       graph-terms
       gap-terms)
       (star-map-contribution? graph-terms)
       (assoc :star-map? true)

       (gap-contribution? gap-terms)
       (assoc :gap? true)

       (and ruled-outcome-c-enabled? c-fold-provenance)
       (assoc :c-fold-provenance c-fold-provenance)

       machine-q-evaluation
       (assoc :machine-q machine-q-evaluation)

       learn-action?
       (assoc :c-zone-load (assoc zone-evidence :risk zone-risk)
              :g-ambiguity-source :beta-predictive)

	      ;; D-1d dark lane: the relocated term, as a log-prior bias for the
	      ;; habit-prior seam in policy/select-action. Positive = preference-
	      ;; increasing (it was SUBTRACTED from G; ln E is ADDED to the score).
	      habit-prior?
	      (assoc :habit-prior-bias (- sp-contribution))

       move-class-intensity
       (assoc :move-class-intensity-mode move-class-intensity-mode
              :move-class-intensity move-class-intensity
              :move-class-intensity-contribution move-class-contribution)

       ;; U24: recorded only when the leg is live, so the trace boundary of a
       ;; default tick is unchanged and a record carrying these keys is
       ;; evidence the leg actually ran.
       survey-eig-contribution
       (assoc :survey-eig-weight (double survey-eig-weight)
              :survey-eig-nats (double (:survey-eig-nats action))
              :survey-eig-contribution survey-eig-contribution)))))

;; ===== VM tick 1 R5 fix wave: cascade-candidate scoring =====
;; The virtual War Machine's R5 node scores cascade candidates
;; ({:kind :cascade-candidate …}, R4's shape) at a common declared horizon
;; with the aligned model function, not the channel-mean scorer above.

(defn- cascade-candidate-tokens
  "Every token named by one cascade candidate's pattern precedence: each
  pattern's :produces set and every guard-clause :present/:absent token."
  [precedence]
  (reduce
   (fn [acc pattern]
     (reduce conj acc
             (concat (:produces pattern)
                     (mapcat (fn [clause] (concat (:present clause) (:absent clause)))
                             (get-in pattern [:guard :clauses])))))
   #{}
   precedence))

(defn rank-cascade-actions
  "Score a sequence of cascade candidates ({:kind :cascade-candidate :id …
  :precedence [patterns…]}) by expected free energy and rank ascending.

  This is the R5 path of the virtual War Machine (tick 1 fix wave,
  VM-PROTOCOL step 7): G per candidate is `cascade-model-manifest/
  horizon-g-sparse` — the aligned Clojure of Lean PolicyHorizon.horizonEFE
  with risk by Lean OutcomeRiskKL.outcomeRisk — computed at ONE common
  horizon and over ONE common token universe for the whole candidate list
  (union of q0's support, the preference spec's :want and every token named
  by any candidate's patterns; per-candidate universes would shift G by
  T·k·ln 2 and are never used). Rates default to the zero adjudication
  kernel (the exact P5 identity-observation reduction) and may be DECLARED
  non-zero via :adjudication-rates on opts (R7, 2026-09-18; typed refusal
  :invalid-adjudication-rates when they do not cover the scored universe),
  with an optional declared FIXED :zeta tempering the likelihood
  (likelihood-precision).

  Inputs and where each parameter comes from:
  - T: `(:horizon-steps opts)` — the declared common horizon of the
    candidate family (R13). Typed refusal :missing-common-horizon if absent.
  - q0: `(:cascade-belief state)` — the token-state belief (R1/R4's
    :missing-cascade-belief convention). Typed refusal if absent.
  - spec: `(:cascade-spec opts)` — {:want … :lam … :mu … :evidence …
    :zeroed …}. :want is required (typed refusal :missing-cascade-want);
    λ, μ default 1 (R6's declared theta), :evidence/:zeroed default #{}.
  - precedence: each candidate's own :precedence.

  Each ranked entry carries :G-efe (= :G-cascade = :controller-score, lower
  more preferred), :rank, :cascade-id, :horizon-steps and :cascade true.
  EQUAL G IS A TIE: equal-G entries share one :rank (dense ranking over the
  distinct G values) and each carries :g-tie, the vector of cascade-ids at
  that G. The tie is recorded, never broken here — selection (R14)
  resolves it. The result's meta records the scoring parameters under
  :cascade-scoring.

  An explicit :observation-model selects the bounded model-query scorer,
  with occurrence/horizon-matched :observation and :prediction-context.
  That experimental route returns typed family refusals, never neutral F.

  Pure; the existing single-action channel scoring in `rank-actions` is
  unchanged. A mixed list (cascade candidates and :type actions together)
  is the typed refusal :mixed-candidate-kinds — no combined semantics is
  invented."
  [state candidate-actions opts]
  (if (contains? opts :observation-model)
    ((requiring-resolve 'futon2.aif.cascade-observation-scoring/rank-cascade-actions)
     state candidate-actions opts)
    (let [T (:horizon-steps opts)
        q0 (:cascade-belief state)
        spec-in (or (:cascade-spec opts) {})
        want (:want spec-in)]
    (cond
      (not (and (integer? T) (pos? T)))
      {:status :missing :kind :missing-common-horizon
       :horizon-steps T
       :limitation "cascade candidates are scored at the family's declared common horizon; pass :horizon-steps (R13's T)"}
      (not (map? q0))
      {:status :missing :kind :missing-cascade-belief
       :limitation "cascade scoring reads the token-state belief from (:cascade-belief state) (R1/R4 convention)"}
      ((complement seq) want)
      {:status :missing :kind :missing-cascade-want
       :limitation "cascade scoring needs the preference spec's :want (R1's want tokens); pass :cascade-spec {:want #{…}}"}
      :else
      (let [spec (cond-> {:want want
                          :evidence (or (:evidence spec-in) #{})
                          :lam (or (:lam spec-in) 1)
                          :mu (or (:mu spec-in) 1)
                          :zeroed (or (:zeroed spec-in) #{})
                          ;; WIRE-3 C provenance: the certificate must be
                          ;; able to say WHICH C the scoring used. A caller
                          ;; that derived the live C passes {:c {:status
                          ;; :derived …}}; everything else records the
                          ;; uniform declared-constant spec it always was.
                          :c (or (:c spec-in)
                                 {:status :uniform-declared-constant})}
                    ;; WIRE-3: the derived live C's per-token weights, when
                    ;; the caller carries them. Attached only when present —
                    ;; log-preference-fn validates :weights whenever the key
                    ;; exists (positive rationals over :want, typed refusal
                    ;; :invalid-preference-spec :field :weights otherwise).
                    (:weights spec-in)
                    (assoc :weights (:weights spec-in))
                    (contains? spec-in :c-schedule)
                    (assoc :c-schedule (:c-schedule spec-in)))
            universe (-> (cascade-candidate-tokens
                          (mapcat :precedence candidate-actions))
                         (into (reduce set/union #{} (keys q0)))
                         (into want))
            ;; R7 (2026-09-18): the adjudication rates are no longer
            ;; hardcoded to zero here. Absent :adjudication-rates the call
            ;; is EXACTLY what it always was — the all-zero identity kernel,
            ;; byte-identical. A declared rates map must carry an entry for
            ;; EVERY token of the scored universe (exact rationals in [0,1];
            ;; the scorer's own validation adds nothing weaker), else the
            ;; typed refusal :invalid-adjudication-rates below names the
            ;; missing tokens — never a silent projection onto zero.
            declared-rates (:adjudication-rates opts)
            rates (if (nil? declared-rates)
                    (zipmap universe (repeat {:false-neg 0 :false-pos 0}))
                    (if (and (map? declared-rates)
                             (every? #(contains? declared-rates %) universe))
                      declared-rates
                      {:status :missing
                       :kind :invalid-adjudication-rates
                       :missing (vec (sort (remove #(contains? declared-rates %)
                                                   universe)))
                       :limitation ":adjudication-rates must map EVERY token of the scored universe to {:false-neg fn :false-pos fp} (exact rationals in [0,1])"}))
            ;; WIRE-2: per-policy F_π on the tick. F comes from
            ;; cascade-free-energy/policy-free-energy (the aligned B.2
            ;; equality case), computed once for the whole candidate family
            ;; at the same q0, tau = the declared horizon, rates and
            ;; universe G was scored over. observed-tokens are the spec's
            ;; :evidence set — the observed evidence tokens the cascade
            ;; decision already carries (the same set the coverage term's
            ;; |evidence ∩ obs| reads); there is no other observed-token
            ;; source at this call site.
            f-source :cascade-free-energy/policy-free-energy
            observed-tokens (set (or (:evidence spec) #{}))
            fe (cascade-free-energy/policy-free-energy
                {:q0 q0
                 :candidates (vec candidate-actions)
                 :tau T
                 :observed-tokens observed-tokens
                 :rates rates})
            fe-refusal? (and (map? fe) (contains? fe :status))
            f-by-id (when-not fe-refusal? (:f fe))
            ;; A refused F is NEVER silently defaulted to 0: the candidate
            ;; whose F is a typed refusal is excluded from the ranking and
            ;; recorded with its reason under :f-exclusions. A global
            ;; producer refusal excludes the whole family the same way.
            f-exclusions
            (if fe-refusal?
              (mapv (fn [a] {:cascade-id (:id a) :source f-source :reason fe})
                    candidate-actions)
              (into []
                    (keep (fn [a]
                            (when-some [v (get f-by-id (:id a))]
                              (when (and (map? v) (contains? v :status))
                                {:cascade-id (:id a) :source f-source
                                 :reason v}))))
                    candidate-actions))
            excluded-ids (set (map :cascade-id f-exclusions))
            scored (map (fn [action]
                          (let [{:keys [g certificate]}
                                (cascade-manifest/horizon-g-sparse-cert
                                 (cond-> {:rates rates
                                  :q0 q0
                                  :precedence-fn (constantly (:precedence action))
                                  :horizon T
                                  :spec spec
                                  ;; R7: the declared FIXED zeta rides the
                                  ;; opts through to the scorer (default 1,
                                  ;; byte-identical when absent).
                                  ;; R7: get, not (:zeta opts) — an
                                  ;; explicit nil must never override
                                  ;; the declared default of 1.
                                  :zeta (get opts :zeta 1)
                                  :universe universe}
                                   ;; D produced the filtering result. Q checks
                                   ;; and consumes that exact receipt at tau=0;
                                   ;; F's evidence input remains independent.
                                   (contains? state :belief-update-receipt)
                                   (assoc :belief-update-receipt (:belief-update-receipt state))))
                                f-raw (when (and (not fe-refusal?)
                                                 (not (contains? excluded-ids (:id action))))
                                        (get f-by-id (:id action)))
                                ;; A computed but NON-FINITE F is not attached.
                                ;; Under the identity-A reduction the live tick
                                ;; runs, P(o|pi) is 0 for any cascade whose
                                ;; rollout state differs from the observation,
                                ;; so F = -ln 0 = Inf for every cascade that
                                ;; produces anything and only the do-nothing
                                ;; cascade is finite. Passing that to selection
                                ;; drives the posterior to NaN and rewards
                                ;; inaction -- a dark room reached through F.
                                ;; The value is still computed and still
                                ;; recorded on the certificate; it just does not
                                ;; reach the law until A stops being degenerate,
                                ;; at which point it starts flowing with no
                                ;; further change here.
                                f-finite? (and (number? f-raw)
                                               (Double/isFinite (double f-raw)))
                                f (when f-finite? f-raw)]
                            (cond-> {:action action
                                     :cascade true
                                     :cascade-id (:id action)
                                     :horizon-steps T
                                     :G-efe g
                                     :G-cascade g
                                     :controller-score (if (number? g) g ##Inf)}
                              ;; WIRE-2: the computed F_π reaches selection as
                              ;; :f (select-action-cascades' σ(ln E − F − G/β)).
                              ;; Attached exactly when it was computed for this
                              ;; candidate; a refused F excludes the candidate
                              ;; above instead of defaulting here.
                              (number? f)
                              (assoc :f f)
                              ;; WIRE-1 emission slice: the GCertificate
                              ;; record (Lean DarkTower/AIF/Certificates.lean)
                              ;; for this candidate's G — present exactly when
                              ;; the computation ran, never fabricated for a
                              ;; refusal. β is echoed when the caller declared
                              ;; it on opts (production declares it on the
                              ;; cascade problem and selection reads it there;
                              ;; R5 scoring opts do not carry it today, and
                              ;; that absence is recorded, not defaulted).
                              certificate
                              (assoc :certificate
                                     (assoc certificate
                                            :beta-declared
                                            {:value (:beta opts)
                                             :status (if (number? (:beta opts))
                                                       :declared
                                                       :not-in-scoring-opts)}
                                            :habit {:value 1 :status :declared-neutral}
                                            ;; WIRE-3: which C the scoring
                                            ;; used — the caller's derived
                                            ;; live C (with signature) or
                                            ;; the uniform declared-constant
                                            ;; spec. "C was derived and
                                            ;; happened to be near-uniform"
                                            ;; reads as :derived with the
                                            ;; weights echoed; "C was never
                                            ;; derived" reads as
                                            ;; :uniform-declared-constant.
                                            :c (assoc (:c spec)
                                                      :weights-echo
                                                      (:weights spec))
                                            ;; WIRE-5 rates provenance:
                                            ;; sourced vs declared vs
                                            ;; identity-default is readable
                                            ;; on the certificate itself,
                                            ;; not only the family meta.
                                            ;; NOT a contradiction: these
                                            ;; keys can legitimately appear
                                            ;; TOGETHER with :evaluation
                                            ;; :identity-A-zero-rates — an
                                            ;; all-checkable universe
                                            ;; SOURCES the exact-zero kernel
                                            ;; (tokenLikelihood_checkable),
                                            ;; which IS the identity kernel;
                                            ;; :rates-provenance says where
                                            ;; the rates came from,
                                            ;; :evaluation says which
                                            ;; evaluation path the numbers
                                            ;; took.
                                            :rates-provenance
                                            (or (:rates-provenance opts)
                                                {:status (if declared-rates
                                                           :declared-in-opts
                                                           :identity-default)})
                                            ;; WIRE-2 F provenance: the
                                            ;; certificate says WHERE F came
                                            ;; from, so "F computed = 0.0"
                                            ;; (:status :computed with the
                                            ;; source) is distinguishable from
                                            ;; "F not on this entry"
                                            ;; (:status :not-attached with the
                                            ;; reason). Never a bare 0.
                                            :f
                                            (cond
                                              (and (number? f-raw) (not f-finite?))
                                              {:value f-raw
                                               :status :computed-not-attached
                                               :reason :non-finite-under-identity-a
                                               :source f-source
                                               :tau T
                                               :observed-tokens observed-tokens}

                                              (number? f)
                                              {:value f
                                               :status :computed
                                               :source f-source
                                               :tau T
                                               :observed-tokens observed-tokens}
                                              :else
                                              {:value nil
                                               :status :not-attached
                                               :source f-source
                                               :reason
                                               (cond
                                                 fe-refusal? fe
                                                 (contains? excluded-ids (:id action))
                                                 (:reason (some #(when (= (:id action)
                                                                          (:cascade-id %))
                                                                   %)
                                                                f-exclusions))
                                                 :else
                                                 {:kind :no-f-entry
                                                  :limitation "F was not computed for this candidate"})}))))))
                        (remove #(contains? excluded-ids (:id %))
                                candidate-actions))
            sorted (sort-by :controller-score scored)
            rank-of (into {} (map-indexed (fn [i g] [g (inc i)]))
                          (distinct (map :controller-score sorted)))
            ranked (map (fn [entry]
                          (let [tied (filter #(= (:controller-score %)
                                                 (:controller-score entry))
                                             sorted)]
                            (cond-> (assoc entry
                                           :rank (get rank-of
                                                      (:controller-score entry)))
                              (< 1 (count tied))
                              (assoc :g-tie (mapv :cascade-id tied)))))
                        sorted)]
        (if (= :invalid-adjudication-rates (:kind rates))
          rates
          (with-meta (vec ranked)
          {:policy-support/excluded []
           :disposition-risk-events []
           :refused? false
           :f-exclusions f-exclusions
           :cascade-scoring {:universe universe
                             :horizon T
                             :spec spec
                             :rates (if (:rates-provenance opts)
                                      :sourced-adjudication-rates
                                      (if (every? (fn [t]
                                                    (and (zero? (:false-neg t))
                                                         (zero? (:false-pos t))))
                                                  (vals rates))
                                        :zero-adjudication-identity
                                        :declared-adjudication-rates))
                             ;; WIRE-5: the sourcing record itself (e.g.
                             ;; {:source … :basis {token k} :labels …}),
                             ;; echoed unchanged when present.
                             :rates-provenance (:rates-provenance opts)
                             ;; R7: the declared FIXED zeta the scorer was
                             ;; asked to temper at (default 1).
                             :zeta (get opts :zeta 1)
                             ;; WIRE-2: the F_π computation record — the
                             ;; producer's own declared params when it ran,
                             ;; its typed refusal when it did not.
                             :free-energy
                             (if fe-refusal?
                               {:status :refused :source f-source :reason fe}
                               {:status :computed :source f-source
                                :params (:params fe)})}})))))))

(defn rank-actions
  "Score a sequence of candidate actions and order them by controller-score
   ascending. Returns a vec of `compute-efe` outputs each carrying
   `:rank` (1 = most preferred). Empty input returns `[]`.

   v0.14: optional `opts` map threaded to `compute-efe` for every
   candidate — supports `:info-weight`, `:survival-weight`,
   `:structural-pressure-weight`, `:time-pressure`,
   `:time-pressure-scale`.

   v0.16 (VM tick 1 R5): if EVERY candidate is a cascade candidate
   ({:kind :cascade-candidate …}), scoring goes through
   `rank-cascade-actions` — G by `cascade-model-manifest/horizon-g-sparse`
   at the declared common horizon over one common universe, ranked
   ascending with equal G a recorded tie. Channel scoring is unchanged for
   the existing :type actions; a mixed list is the typed refusal
   :mixed-candidate-kinds."
  ([state candidate-actions] (rank-actions state candidate-actions {}))
  ([state candidate-actions opts]
   (when (:preference-module opts)
     (c-module/validate-module (:preference-module opts)))
   (cond
     (and (seq candidate-actions) (every? fm/cascade-candidate? candidate-actions))
     (rank-cascade-actions state candidate-actions opts)
     (some fm/cascade-candidate? candidate-actions)
     {:status :missing :kind :mixed-candidate-kinds
      :limitation "cascade candidates and :type actions cannot be scored in one list; score each kind separately"}
     :else
   (let [{:keys [included excluded]}
         (partition-policy-support (:capability-graph opts) candidate-actions opts)
         seed-required? (and (:ruled-outcome-c-enabled? opts) (seq included))
         seed-record (when (:ruled-outcome-c-enabled? opts)
                       (disposition/seeded-c-record opts seed-required?))
         refusal? (and seed-required? (= :absent (:status seed-record)))
         ranked (if refusal?
                  []
                  (->> included
                       (map (fn [action]
                              (cond-> (compute-efe state action opts)
                                (:preference-module opts)
                                (assoc :preference-module-diagnostic
                                       (c-module/assess
                                        (:preference-module opts)
                                        (get action :preference-readings {}))))))
                       (sort-by :controller-score)
                       (map-indexed (fn [i e] (assoc e :rank (inc i))))
                       vec))]
     (with-meta ranked
       {:policy-support/excluded (vec excluded)
        :disposition-risk-events (if seed-record
                                   (disposition/seeded-c-events seed-record)
                                   [])
        :refused? refusal?})))))

(defn rank-star-map-actions
  "Rank candidate actions after applying the INV-G selector gate. Unsafe pursuit,
   goal-extending decompose without consent, and unagreed operator-exit advances
   are refused before EFE ranking."
  [state candidate-actions {:keys [capability-graph pre-registered-goal] :as opts}]
  (rank-actions state
                (filter #(safe-action? capability-graph pre-registered-goal %) candidate-actions)
                opts))

(defn select-star-map-action
  "Return the EFE-top safe action for a pre-registered goal, or nil if no safe
   candidate remains."
  [state candidate-actions opts]
  (first (rank-star-map-actions state candidate-actions opts)))

(defn rank-local-preference-actions
  "WM support gate and existing scores, plus a separate one-entry local C-risk
   ordering. Caller supplies a finite :preference-module, entry id and predicted
   :preference-readings on each action. This does not invent an aggregate G."
  [state actions opts entry-id]
  (let [ranked (rank-actions state actions opts)
        candidates (mapv (fn [r] {:id (get-in r [:action :id])
                                 :readings (get-in r [:action :preference-readings] {})}) ranked)]
    {:wm-rankings ranked
     :policy-support (meta ranked)
     :local-preference-ranking
     (c-module/rank-local-risk (:preference-module opts) entry-id candidates)}))
