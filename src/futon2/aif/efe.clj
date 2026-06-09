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
            [futon2.aif.preferences :as pref]))

(defn- ambiguity
  "Sum of per-channel predicted variances. Higher = more uncertain
   predicted outcome → higher EFE contribution → less preferred action."
  [variance-map]
  (reduce + (vals variance-map)))

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
   ascent progress toward the pre-registered goal is a credit."
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
           :graph/applicable? applicable?
           :graph/single-cycle-leaf? (mission-single-cycle-leaf? graph mission-id)})
        {:G-applicability 0.0
         :G-body-size 0.0
         :G-ascent-progress 0.0
         :G-graph-pragmatic (double graph-off-map-penalty)})
      {:G-applicability 0.0
       :G-body-size 0.0
       :G-ascent-progress 0.0
       :G-graph-pragmatic 0.0})))

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
                    :let [pref (get pref/preferences ch)]
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
      :G-total       <G-risk + G-ambiguity − info-weight×G-info
                       + survival-weight×G-survival
                       − structural-pressure-weight×G-structural-pressure
                       − G-gap>
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

   `:graph-off-map-penalty` defaults to 0.0 and applies only to off-map
   `:open-mission` actions when graph + goal are present. `:graph-body-mode`
   defaults to `:whole`; `:leaf` scores a bounded next-step body term. When
   `:graph-ascent-status-aware?` is true, ascent credit ignores produced
   capabilities already marked `:satisfied`.

   Pure: same (state, action, opts) → same output."
  ([state action] (compute-efe state action {}))
  ([state action {:keys [info-weight survival-weight structural-pressure-weight time-pressure
                         time-pressure-scale horizon-steps capability-graph
                         pre-registered-goal graph-applicability-penalty
                         graph-body-weight graph-ascent-weight graph-off-map-penalty
                         graph-body-mode graph-ascent-status-aware? mission-gap-view
                         gap-weight]
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
                       gap-weight default-gap-weight}}]
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
         g-risk-base (- (:G-pragmatic fe-on-predicted) intrinsic)
         g-ambig (ambiguity next-var)
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
         urgency (+ 1.0 (* (double time-pressure) (double time-pressure-scale)))
         g-risk (* g-risk-base urgency)
         g-survival (* g-survival-base urgency)
         g-total (+ g-risk
                    g-ambig
                    (- (* (double info-weight) g-info))
                    (* (double survival-weight) g-survival)
                    (- (* (double structural-pressure-weight)
                          g-structural-pressure))
                    (:G-graph-pragmatic graph-terms)
                    (- (:G-gap gap-terms)))]
     (cond->
      (merge
       {:action action
        :prediction prediction
        :G-risk g-risk
        :G-ambiguity g-ambig
        :G-info g-info
        :G-survival g-survival
        :G-structural-pressure g-structural-pressure
        :G-total g-total
        :time-pressure (double time-pressure)
        :horizon-steps (when multi (:horizon-steps multi))
        :per-channel (:per-channel fe-on-predicted)}
       graph-terms
       gap-terms)
       (star-map-contribution? graph-terms)
       (assoc :star-map? true)

       (gap-contribution? gap-terms)
       (assoc :gap? true)))))

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
