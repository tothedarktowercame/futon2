(ns futon2.aif.efe
  "Expected Free Energy computation for the WM AIF apparatus.

   `compute-efe` scores a single `(state, action)` pair by composing R4
   (predictive forward model) with the R3c free-energy decomposition.
   `rank-actions` scores a candidate-action sequence and orders them by
   EFE ascending (lower = more preferred).

   Contract: contributes to R5 (EFE with at least two principled terms)
   per `futon2/docs/futon-aif-completeness.md`. The two principled
   terms required by R5:

     R5a — pragmatic / risk     = G-risk
     R5b — epistemic / ambiguity = G-ambiguity

   Both are computed against R4's forward-model output, NOT against the
   current observation. This is the structural difference from
   `futon2.aif.free-energy/compute-free-energy` — which scores VFE on
   the current observation; EFE scores predicted next-observation under
   each candidate action.

   Cross-maps to F4 (bounded self-balance) and F6 (operator inhabitation)
   at stack scope: F4 via the predicted-distance-from-preferences signal
   the risk term provides; F6 via the ranked recommendations the
   operator reads and acts on.

   Theory: AIF Expected Free Energy decomposition. The ambiguity term
   here is the expected entropy of the observation distribution under
   the action (sum of per-channel predicted variances — a simple
   Gaussian-flavoured proxy). The proper information-gain epistemic
   (expected entropy reduction in belief, not observation) is a future
   R7-related deliverable; ambiguity is the principled R5b minimum."
  (:require [futon2.aif.forward-model :as fm]
            [futon2.aif.free-energy :as fe]
            [futon2.aif.preferences :as pref]
            [futon2.aif.c-vector :as cv]
            [futon2.aif.move-class-intensity :as move-intensity]))

(defn- ambiguity
  "R5b epistemic term over per-channel predicted variances.

   Modes (M-evaluate-policies D5c — dark flag, nothing flips it on):
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

;; ---------------------------------------------------------------------------
;; v0.13 R5 augmentation: info-gain + survival-cost terms ported from
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

;; B-0a tick provenance (M-aif-faithfulness §2.0): exposed as a def — not an
;; inline :or literal — so the :wm-version stamp records the value compute-efe
;; actually resolves when the caller passes no :kl-channel-weights, instead of
;; a re-typed literal that could silently drift. {} ⇒ every channel weight 1.0
;; (UNIFORM — the joint KL under channel independence, the canonical config;
;; :pragmatic-parity is a comparability preset only).
(def default-kl-channel-weights {})

;; v0.14: anticipation-driven scaling. When opts carries :time-pressure
;; in [0,1], G-risk and G-survival are multiplied by
;; (1 + time-pressure × default-time-pressure-scale). G-info is NOT
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

(defn graph-efe-terms
  "Graph-functional EFE terms for a mission action. Lower total remains better:
   unbound :requires adds a high applicability gate; body-size is a penalty;
   ascent progress toward the pre-registered goal is a credit.

   B-2b struct split (M-aif-faithfulness §2.2; D8/§3.5): the applicability
   penalty (1000·[not-applicable], and the off-map penalty) is a DOMAIN
   RESTRICTION — canonical seat Π_feasible, a mask on the policy domain, not a
   value judgment — while body/ascent are a PRAGMATIC PROXY (value-flavoured).
   The split is exposed as `:G-graph-feasibility` (mask component) +
   `:G-graph-pragmatic-proxy` (value component); `:G-graph-pragmatic` keeps its
   historical expression and value BYTE-IDENTICALLY (it still carries both,
   summed, into :G-total — actually unbundling the mask into a feasibility
   filter is a semantic change and stays a flagged future flip, not this
   relabel)."
  [graph goal action {:keys [graph-applicability-penalty graph-body-weight graph-ascent-weight
                             graph-off-map-penalty graph-body-mode
                             graph-ascent-status-aware?]
                      :or {graph-applicability-penalty default-graph-applicability-penalty
                           graph-body-weight default-graph-body-weight
                           graph-ascent-weight default-graph-ascent-weight
                           graph-off-map-penalty default-graph-off-map-penalty
                           graph-body-mode :whole
                           graph-ascent-status-aware? false}}]
  (let [mission-id (:target action)
        mission (mission-node graph mission-id)]
    (if (and graph goal (= :open-mission (:type action)))
      (if mission
        (let [applicable? (mission-applicable? graph mission-id)
              body-size (double (or (:open-hole-count mission) 0))
              progress (double (mission-ascent-progress graph goal mission-id
                                                        graph-ascent-status-aware?))
              applicability (if applicable? 0.0 (double graph-applicability-penalty))
              body (case graph-body-mode
                     :leaf (* (double graph-body-weight)
                              (if (mission-single-cycle-leaf? graph mission-id) 0.0 1.0))
                     :whole (* (double graph-body-weight) body-size)
                     (* (double graph-body-weight) body-size))
              ascent (* (double graph-ascent-weight) progress)]
          {:G-applicability applicability
           :G-body-size body
           :G-ascent-progress ascent
           :G-graph-pragmatic (+ applicability body (- ascent))
           ;; B-2b: mask vs value, split (see docstring). Additive keys only —
           ;; :G-graph-pragmatic's expression above is untouched (byte-identity).
           :G-graph-feasibility applicability
           :G-graph-pragmatic-proxy (- body ascent)
           :graph/applicable? applicable?
           :graph/single-cycle-leaf? (mission-single-cycle-leaf? graph mission-id)})
        {:G-applicability 0.0
         :G-body-size 0.0
         :G-ascent-progress 0.0
         :G-graph-pragmatic (double graph-off-map-penalty)
         ;; B-2b: the off-map penalty is feasibility-class (a domain gate on
         ;; "not on the star map"), not a value — proxy carries none of it.
         :G-graph-feasibility (double graph-off-map-penalty)
         :G-graph-pragmatic-proxy 0.0})
      {:G-applicability 0.0
       :G-body-size 0.0
       :G-ascent-progress 0.0
       :G-graph-pragmatic 0.0
       :G-graph-feasibility 0.0
       :G-graph-pragmatic-proxy 0.0})))

(defn- star-map-contribution?
  [graph-terms]
  (or (contains? graph-terms :graph/applicable?)
      (not (zero? (double (:G-graph-pragmatic graph-terms 0.0))))))

(defn- action-target-id
  [action]
  (let [target (:target action)]
    (cond
      (keyword? target) (if-let [ns-part (namespace target)]
                          (str ns-part "/" (name target))
                          (name target))
      (string? target) target
      :else (some-> target str))))

(defn gap-efe-terms
  "Mission-fold gap credit for open-mission actions.

   The fold view maps mission id -> intrinsic gap-score in [0,1].  High gap
   means announced-but-unfilled structure, i.e. epistemic room to grow.  The
   weighted credit is subtracted from G-total by compute-efe, mirroring the
   info/ascent direction: higher gap makes the mission more preferred."
  [mission-gap-view action {:keys [gap-weight]
                            :or {gap-weight default-gap-weight}}]
  (let [mission-id (action-target-id action)
        gap-score (double (or (get mission-gap-view mission-id) 0.0))
        gap (* (double gap-weight) gap-score)]
    (if (= :open-mission (:type action))
      {:G-gap gap
       :gap-score gap-score}
      {:G-gap 0.0
       :gap-score 0.0})))

(defn- gap-contribution?
  [gap-terms]
  (pos? (double (:G-gap gap-terms 0.0))))

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

(defn- info-gain
  "Information-gain term ported from ants/aif/policy.clj.

   Rewards actions whose predicted next-state has LOW variance (the
   agent will gain information by taking them — uncertainty resolves).
   Returns a non-negative scalar; higher = more informative action.

   For v0.13: `info-gain = Σ max(0, 1 − predicted-variance) over channels`.
   The 1.0 ceiling reflects that variances in this contract are in [0, 1]
   (per channel-bounded observations). When variance is 0 (deterministic
   prediction like `:no-op`), info-gain is N-channels — the most-informative
   action by this measure. When variance is high, info-gain is small.

   In EFE, info-gain enters with a NEGATIVE sign (subtracted from
   G-total) so informative actions are PREFERRED."
  [variance-map]
  (reduce + (for [v (vals variance-map)]
              (max 0.0 (- 1.0 (double v))))))

(defn- survival-cost
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
      :G-info        <info-gain term (v0.13, negative weight in G-total)>
      :G-survival    <survival hinge-loss term (v0.13)>
      :G-structural-pressure <candidate-local structural-pressure term>
      :G-gap        <mission-fold gap credit; negative weight in G-total>
      :G-goal-outcome <E-C-vector-live: additive risk = divergence of this
                       action's PREDICTED goal-outcomes from the LIVE C-vector
                       (the belly); advancing a goal lowers it ⇒ re-ranks
                       policies; 0 when no live C ⇒ reduces to the static floor>
      :G-total       <G-risk + G-ambiguity − info-weight×G-info
                       + survival-weight×G-survival
                       − structural-pressure-weight×G-structural-pressure
                       − G-gap + G-goal-outcome>
      :per-channel   <per-channel risk decomposition (from compute-free-energy)>}

   Lower :G-total = more preferred action.

   v0.13 added two principled terms ported from ants/aif/policy.clj:
   - G-info (information-gain) — rewards actions that REDUCE predicted
     variance; enters with negative weight so informative actions are
     preferred.
   - G-survival (survival hinge-loss) — penalises predicted-state
     channels that lie OUTSIDE their preference range; differs from
     G-risk (continuous Gaussian-flavoured) by being a pure threshold
     penalty. Only acts on the 4 R3a-covered channels.

   Optional `opts`:
     :info-weight         — default `default-info-weight` (0.4)
     :survival-weight     — default `default-survival-weight` (1.2)
     :structural-pressure-weight — default
                            `default-structural-pressure-weight` (0.35)
     :time-pressure       — v0.14 anticipation-driven urgency in [0,1];
                            scales G-risk + G-survival by
                            `(1 + time-pressure × time-pressure-scale)`.
                            Default 0 (no anticipation-driven scaling).
     :time-pressure-scale — default `default-time-pressure-scale` (1.0).
     :horizon-steps       — v0.15 opt-in multi-horizon scoring. When
                            >= 2, uses `predict-multi-horizon` and
                            scores against the FINAL-state observation
                            of the trajectory rather than the immediate
                            next-state. Default `nil` (single-step).

   `:intrinsic-value` on the action map represents an *intrinsic credit*
   for actions whose value isn't captured by observation-vector changes.
   Default 0.

   `:structural-pressure-per-action` on the action map represents a
   candidate-local structural-pressure signal. Higher values reduce
   `:G-total` via the weighted subtraction in the decomposition, making
   structurally load-bearing actions more preferred.

   `:mission-gap-view` in opts maps mission id -> gap-score in [0,1].  For
   `:open-mission` actions, the weighted gap credit is subtracted from
   `:G-total`, making announced-but-unfilled missions more preferred.

   `:goal-outcome-entries` in opts overrides the live C-vector source (defaults
   to `c-vector/current-c-vector`); `:goal-outcome-weight` scales the
   mean-normalised goal-outcome risk (W4). With no live C the term is 0.0 —
   `compute-efe` is then identical to its pre-E-C-vector-live behaviour.

   `:graph-off-map-penalty` defaults to 0.0 and applies only to off-map
   `:open-mission` actions when graph + goal are present. `:graph-body-mode`
   defaults to `:whole`; `:leaf` scores a bounded next-step body term. When
   `:graph-ascent-status-aware?` is true, ascent credit ignores produced
   capabilities already marked `:satisfied`.

   Result shape (B-2a honest labelling, M-aif-faithfulness §2.2): the output
   is a MULTI-OBJECTIVE ACTION SCORE WITH AN EFE CORE. `:G-core` (= risk +
   ambiguity, invariant I3) is the canonical G; `:augmentation-terms` names
   the six demoted non-core contributions exactly as they enter `:G-total`
   (signed, weighted), and `:G-augmentation` is their sum. `:G-total` — the
   ranking key — is the historical blend, byte-identical across this
   relabelling; do not read it as canonical EFE (R18/D8).

   Pure: same (state, action, opts) → same output."
  ([state action] (compute-efe state action {}))
  ([state action {:keys [info-weight survival-weight structural-pressure-weight time-pressure
                         time-pressure-scale horizon-steps capability-graph
                         pre-registered-goal graph-applicability-penalty
                         graph-body-weight graph-ascent-weight graph-off-map-penalty
                         graph-body-mode graph-ascent-status-aware? mission-gap-view
                         gap-weight goal-outcome-weight goal-outcome-entries goal-outcome-prob-fn
                         goal-outcome-mode
                         ambiguity-mode risk-mode kl-channel-weights c-temperature
                         structural-pressure-mode move-class-intensity-mode
                         move-class-intensity-weight]
                  :or {info-weight default-info-weight
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
                       gap-weight default-gap-weight
                       goal-outcome-weight cv/default-goal-outcome-weight
                       ambiguity-mode :variance-sum
                       risk-mode :hinge
                       goal-outcome-mode :hinge
                       structural-pressure-mode :g-summand
                       move-class-intensity-mode :off
                       move-class-intensity-weight 1.0
                       kl-channel-weights default-kl-channel-weights
                       c-temperature pref/default-c-temperature}}]
   (let [single-prediction (fm/predict state action)
         ;; v0.15: opt-in multi-horizon trajectory; final-state observation
         ;; drives G-risk + G-survival. Ambiguity + info still use the
         ;; FIRST-step prediction's variance (the agent's immediate
         ;; uncertainty about taking the action).
         multi (when (and horizon-steps (>= horizon-steps 2))
                 (fm/predict-multi-horizon state action horizon-steps))
         prediction single-prediction
         next-mean (if multi
                     (get-in multi [:final-state :observation])
                     (get-in single-prediction [:next-observation :mean]))
         next-var (get-in single-prediction [:next-observation :variance])
         fe-on-predicted (fe/compute-free-energy next-mean)
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
         g-risk-base (case risk-mode
                       :kl
                       (- (reduce +
                                  0.0
                                  (for [[ch spec] (pref/current-C)
                                        :let [mu (get next-mean ch)
                                              s2 (get next-var ch)]
                                        :when (and mu s2)]
                                    (* (double (get kcw ch kcw-default))
                                       (pref/kl {:kind :gaussian :mu mu :sigma2 s2}
                                                (pref/c-distribution spec
                                                                     :temperature (ch-temp ch))))))
                          intrinsic)
                       (- (:G-pragmatic fe-on-predicted) intrinsic))
         g-ambig (ambiguity next-var ambiguity-mode)
         g-info (info-gain next-var)
         g-survival-base (survival-cost next-mean)
         g-structural-pressure (double (or (:structural-pressure-per-action action) 0.0))
         graph-terms (graph-efe-terms capability-graph pre-registered-goal action
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
                                       :graph-ascent-status-aware?
                                       graph-ascent-status-aware?})
         gap-terms (gap-efe-terms mission-gap-view action
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
         g-goal-outcome (let [entries (or goal-outcome-entries (cv/current-c-vector))
                              prob-fn (or goal-outcome-prob-fn cv/credit-satisfy-prob)]
                          (case goal-outcome-mode
                            :kl (cv/predictive-goal-outcome-risk-kl
                                 entries action capability-graph goal-outcome-weight
                                 prob-fn (if (map? c-temperature)
                                           pref/default-c-temperature
                                           c-temperature))
                            (cv/predictive-goal-outcome-risk
                             entries action capability-graph goal-outcome-weight
                             prob-fn)))
         move-class-intensity (when (= :v1 move-class-intensity-mode)
                                (move-intensity/intensity action))
         move-class-contribution (when move-class-intensity
                                   (- (* (double move-class-intensity-weight)
                                         (double (:value move-class-intensity)))))
         urgency (+ 1.0 (* (double time-pressure) (double time-pressure-scale)))
         g-risk (* g-risk-base urgency)
         g-survival (* g-survival-base urgency)
         ;; HONESTY (D8 reconciliation; C6 wording; B-2a struct split): what
         ;; follows is a MULTI-OBJECTIVE SCORE WITH AN EFE CORE, not canonical
         ;; EFE. The core (risk + ambiguity — :G-core, invariant I3) is the
         ;; canonical G; the six remaining contributions are the augmentation
         ;; layer — a flattened generative model (C-terms, E-term, Π-mask
         ;; projected into one sum; argue-exhibit pp. 8–9). :G-total keeps its
         ;; historical summation order and value BYTE-IDENTICALLY; the layer
         ;; below only NAMES the same quantities (float associativity means
         ;; :G-core + :G-augmentation matches :G-total to ~1e-15, not to the
         ;; bit — asserted at 1e-9 in efe_struct_split_test).
         ;; D-1d (M-aif-faithfulness §2.1, relocation RATIFIED 2026-07-04, flip
         ;; = Joe's): G-structural-pressure is a documented EXOGENOUS WEIGHT
         ;; whose canonical seat is the habit prior ln E(π) (R12), not a
         ;; G-summand. :structural-pressure-mode:
         ;;   :g-summand (DEFAULT) — the historical position: the (negative,
         ;;     preference-increasing) contribution stays in :G-total and in
         ;;     the :structural-pressure slot of the augmentation layer.
         ;;     Byte-identical.
         ;;   :habit-prior (DARK) — the term LEAVES :G-total and the layer;
         ;;     :habit-prior-bias (= +w·sp, in ln-E-units-by-declaration) is
         ;;     emitted instead, consumed by policy/select-action's log-prior
         ;;     seam (the R12 seat). Semantic difference, documented for the
         ;;     flip memo: as ln E the term stops being scaled by 1/τ_eff (γ)
         ;;     — precision no longer modulates habit; and it stops moving
         ;;     :G-total, so census/argmin views of G no longer see it.
         sp-contribution (- (* (double structural-pressure-weight)
                               g-structural-pressure))
         habit-prior? (= structural-pressure-mode :habit-prior)
         augmentation-terms (cond-> {:info (- (* (double info-weight) g-info))
                                     :survival (* (double survival-weight) g-survival)
                                     :structural-pressure sp-contribution
                                     :graph-pragmatic (:G-graph-pragmatic graph-terms)
                                     :gap (- (:G-gap gap-terms))
                                     :goal-outcome g-goal-outcome}
                              habit-prior? (dissoc :structural-pressure)
                              move-class-contribution
                              (assoc :move-class-intensity move-class-contribution))
         g-total-base (+ g-risk
                         g-ambig
                         (- (* (double info-weight) g-info))
                         (* (double survival-weight) g-survival)
                         (if habit-prior? 0.0 sp-contribution)
                         (:G-graph-pragmatic graph-terms)
                         (- (:G-gap gap-terms))
                         g-goal-outcome)
         g-total (if move-class-contribution
                   (+ g-total-base move-class-contribution)
                   g-total-base)]
     (cond->
      (merge
       {:action action
        :prediction prediction
        :G-risk g-risk
        :G-ambiguity g-ambig
        :G-info g-info
        :G-survival g-survival
        :G-structural-pressure g-structural-pressure
        :G-goal-outcome g-goal-outcome
        ;; D2 (M-evaluate-policies §8.3): the canonical EFE CORE, reported
        ;; separately from the multi-objective blend — :G-core = risk +
        ;; ambiguity exactly (invariant I3). Pure addition; :G-total unchanged.
        :G-core (+ g-risk g-ambig)
        ;; B-2a (M-aif-faithfulness §2.2): the multi-objective augmentation
        ;; layer, named — the six non-core contributions AS THEY ENTER
        ;; :G-total (signed, weighted). :G-augmentation is their sum. Additive
        ;; keys; whitelisted in trace.clj AT BIRTH (:score-provenance lesson);
        ;; trace-schema-version bumped 2→3.
        :G-augmentation (reduce + 0.0 (vals augmentation-terms))
        :augmentation-terms augmentation-terms
        ;; D5a: which risk functional produced :G-risk — whitelisted in
        ;; trace.clj AT BIRTH (the :score-provenance lesson).
        :risk-mode risk-mode
        ;; D-1e: which functional produced :G-goal-outcome — whitelisted in
        ;; trace.clj AT BIRTH, same lesson as :risk-mode.
        :goal-outcome-mode goal-outcome-mode
        ;; D-1d: where structural pressure sits — whitelisted at birth.
        :structural-pressure-mode structural-pressure-mode
        :G-total g-total
        :time-pressure (double time-pressure)
        :horizon-steps (when multi (:horizon-steps multi))
        :per-channel (:per-channel fe-on-predicted)}
       graph-terms
       gap-terms)
       (star-map-contribution? graph-terms)
       (assoc :star-map? true)

       (gap-contribution? gap-terms)
       (assoc :gap? true)

       ;; D-1d dark lane: the relocated term, as a log-prior bias for the R12
       ;; habit-prior seam in policy/select-action. Positive = preference-
       ;; increasing (it was SUBTRACTED from G; ln E is ADDED to the score).
       habit-prior?
       (assoc :habit-prior-bias (- sp-contribution))

       move-class-intensity
       (assoc :move-class-intensity-mode move-class-intensity-mode
              :move-class-intensity move-class-intensity
              :G-move-class-intensity move-class-contribution)))))

(defn rank-actions
  "Score a sequence of candidate actions and order them by G-total
   ascending. Returns a vec of `compute-efe` outputs each carrying
   `:rank` (1 = most preferred). Empty input returns `[]`.

   v0.14: optional `opts` map threaded to `compute-efe` for every
   candidate — supports `:info-weight`, `:survival-weight`,
   `:structural-pressure-weight`, `:time-pressure`,
   `:time-pressure-scale`."
  ([state candidate-actions] (rank-actions state candidate-actions {}))
  ([state candidate-actions opts]
   (->> candidate-actions
        (map #(compute-efe state % opts))
        (sort-by :G-total)
        (map-indexed (fn [i e] (assoc e :rank (inc i))))
        vec)))

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
