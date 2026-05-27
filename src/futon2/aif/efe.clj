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

;; v0.14: anticipation-driven scaling. When opts carries :time-pressure
;; in [0,1], G-risk and G-survival are multiplied by
;; (1 + time-pressure × default-time-pressure-scale). G-info is NOT
;; scaled — urgency makes risk and survival dominate, reducing the
;; relative weight of exploration.
(def default-time-pressure-scale 1.0)

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
      :G-total       <G-risk + G-ambiguity − info-weight×G-info + survival-weight×G-survival>
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

   Pure: same (state, action, opts) → same output."
  ([state action] (compute-efe state action {}))
  ([state action {:keys [info-weight survival-weight time-pressure
                         time-pressure-scale horizon-steps]
                  :or {info-weight default-info-weight
                       survival-weight default-survival-weight
                       time-pressure 0.0
                       time-pressure-scale default-time-pressure-scale}}]
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
         urgency (+ 1.0 (* (double time-pressure) (double time-pressure-scale)))
         g-risk (* g-risk-base urgency)
         g-survival (* g-survival-base urgency)
         g-total (+ g-risk
                    g-ambig
                    (- (* (double info-weight) g-info))
                    (* (double survival-weight) g-survival))]
     {:action action
      :prediction prediction
      :G-risk g-risk
      :G-ambiguity g-ambig
      :G-info g-info
      :G-survival g-survival
      :G-total g-total
      :time-pressure (double time-pressure)
      :horizon-steps (when multi (:horizon-steps multi))
      :per-channel (:per-channel fe-on-predicted)})))

(defn rank-actions
  "Score a sequence of candidate actions and order them by G-total
   ascending. Returns a vec of `compute-efe` outputs each carrying
   `:rank` (1 = most preferred). Empty input returns `[]`.

   v0.14: optional `opts` map threaded to `compute-efe` for every
   candidate — supports `:info-weight`, `:survival-weight`,
   `:time-pressure`, `:time-pressure-scale`."
  ([state candidate-actions] (rank-actions state candidate-actions {}))
  ([state candidate-actions opts]
   (->> candidate-actions
        (map #(compute-efe state % opts))
        (sort-by :G-total)
        (map-indexed (fn [i e] (assoc e :rank (inc i))))
        vec)))
