(ns futon2.aif.forward-model
  "Predictive forward model for the WM AIF apparatus.

   Pure function: `(state, action) → next-state-distribution`. Used by
   the EFE scoring step (R5, Checkpoint 3) to evaluate candidate
   actions *before* taking them. Will be shared with the live step
   (`judge` extended with action) when that lands — shared-kernel
   discipline per `ukrn-services-simulation/notebooks/ukrn_v3_kernel.clj`.

   Contract: contributes to R4 (predictive forward model) per
   `futon2/docs/futon-aif-completeness.md`. Cross-maps to F5 (adaptive
   response) at stack scope — predicted-vs-realised delta becomes
   measurable once a step function calls this same predict.

   Theory: deterministic predictive forward model (Active Inference R4).
   For v0.3 of the contract, prediction is point-mean + per-channel
   variance for the observation, and a properly-updated discrete
   posterior for the belief. The variance model is hand-tuned per
   action type — a placeholder for a learned variance model that would
   ship with R7 (adaptive precision) work."
  (:require [futon2.aif.belief :as belief]))

(declare predict)  ; v0.15: predict-multi-horizon (below can-execute?) calls predict (further below).

(def action-types
  "The set of action types this forward model recognises. Extending the
   set means adding both a `predict-effects` multimethod arm (below)
   and a `can-propose?` arm if the action requires substrate
   addressability."
  #{:no-op :address-sorry :open-mission :advance-mission :fire-pattern
    :learn-action-class :pursue :decompose})

(defn- valid-action?
  "An action is a map carrying :type (one of action-types). Most actions
   require a :target (entity-id); :no-op needs no target; :learn-action-class
   requires :target-class (an action-type keyword the proposer wants enabled)."
  [{:keys [type target target-class]}]
  (and (contains? action-types type)
       (case type
         :no-op true
         :learn-action-class (some? target-class)
         :decompose (some? target)
         (some? target))))

(defn- merge-obs-delta
  "Apply a delta map to an observation, clamping channels to [0,1]."
  [observation delta]
  (reduce-kv (fn [acc k dv]
               (let [v (+ (double (get acc k 0.0)) (double dv))]
                 (assoc acc k (max 0.0 (min 1.0 v)))))
             observation
             delta))

(defmulti ^:private predict-effects
  "Return per-action predicted effects:
     {:obs-delta  <map channel→signed delta in observation space>
      :obs-variance <map channel→non-negative variance>
      :events     <seq of belief events to apply to target entity>}
   Variance is a hand-tuned action-type constant in v0.3; will become
   learned in R7."
  (fn [_state action] (:type action)))

(defmethod predict-effects :no-op [_ _]
  {:obs-delta {}
   :obs-variance {}
   :events []})

(defmethod predict-effects :address-sorry
  [_state {:keys [target weight] :or {weight 1.0}}]
  ;; addressing a sorry reduces sorry-count-norm by ~0.1
  ;; (sorry-count-norm = open-sorrys / 10); raises mission-health slightly
  {:obs-delta {:sorry-count-norm -0.1
               :mission-health 0.02}
   :obs-variance {:sorry-count-norm 0.01
                  :mission-health 0.005}
   :events [{:entity-id target :type :addressed :weight weight}]})

(defmethod predict-effects :open-mission
  [_state {:keys [target weight] :or {weight 1.0}}]
  ;; opening a mission raises mission-health and active-repo-ratio
  ;; modestly; introduces uncertainty about the target's future status
  {:obs-delta {:mission-health 0.05
               :active-repo-ratio 0.02}
   :obs-variance {:mission-health 0.02
                  :active-repo-ratio 0.01}
   :events [{:entity-id target :type :spawned :weight weight}]})

(defmethod predict-effects :advance-mission
  [_state {:keys [target weight] :or {weight 1.0}}]
  ;; advancing an ALREADY-OPEN mission discharges open holes: mission-health
  ;; rises and remaining-work pressure falls. The event is :addressed, not
  ;; :spawned — an in-flight mission advanced is work discharged, not a new
  ;; entity. (:open-mission predicting :spawned for already-open missions
  ;; was the WM scoring a hole that wasn't there — pilot cycle #1, 2026-06-10.)
  {:obs-delta {:mission-health 0.04
               :sorry-count-norm -0.05}
   :obs-variance {:mission-health 0.015
                  :sorry-count-norm 0.01}
   :events [{:entity-id target :type :addressed :weight weight}]})

(defmethod predict-effects :fire-pattern
  [_state {:keys [target weight] :or {weight 1.0}}]
  ;; firing a pattern raises ticks-firing-ratio and shifts the target
  ;; toward :strengthened
  {:obs-delta {:ticks-firing-ratio 0.05}
   :obs-variance {:ticks-firing-ratio 0.02}
   :events [{:entity-id target :type :strengthened :weight weight}]})

(defmethod predict-effects :learn-action-class
  [_state _action]
  ;; A "need to learn" action (per Joe 2026-05-17): surfaces a capability
  ;; gap; no immediate stack-state change predicted. The action's intrinsic
  ;; value is carried on the action map (:intrinsic-value), respected by
  ;; `efe/compute-efe` — the predicted effects here are zero because the
  ;; action's value isn't in observation-space, it's in capability-space.
  {:obs-delta {}
   :obs-variance {}
   :events []})

(defmethod predict-effects :pursue
  [_state {:keys [target weight] :or {weight 1.0}}]
  {:obs-delta {}
   :obs-variance {}
   :events [{:entity-id target :type :pursued :weight weight}]})

(defmethod predict-effects :decompose
  [_state {:keys [target weight] :or {weight 1.0}}]
  {:obs-delta {}
   :obs-variance {}
   :events [{:entity-id target :type :decomposed :weight weight}]})

(defmulti can-propose?
  "Per-action-type capability check: can this action class be addressed
   against the current substrate? Default: false (the WM has no proposer
   for it). Specific arms override.

   `:no-op` and `:learn-action-class` are always proposable (the meta /
   default actions). Other actions require a substrate adapter to enumerate
   addressable targets; until such an adapter ships, the bootstrap proposer
   surfaces a `:learn-action-class` action for the gap."
  (fn [_state action-type] action-type))

(defmethod can-propose? :default [_ _] false)
(defmethod can-propose? :no-op [_ _] true)
(defmethod can-propose? :learn-action-class [_ _] true)
(defmethod can-propose? :pursue [_ _] true)
(defmethod can-propose? :decompose [_ _] true)

;; ---------------------------------------------------------------------------
;; v0.13: can-execute? — per-action-instance admissibility check.
;; Composes with can-propose? (which answers "is the action class
;; proposable at all?"); can-execute? answers "is this specific action
;; instance executable in this state?". Ports the admissible-actions
;; pattern from ants/aif/policy.clj.
;; ---------------------------------------------------------------------------

(defmulti can-execute?
  "Per-action-instance admissibility check. Default `true` — if a
   proposer surfaced the action, it's assumed executable unless an
   action-type-specific override says otherwise."
  (fn [_state action] (:type action)))

(defmethod can-execute? :default [_ _] true)
(defmethod can-execute? :no-op [_ _] true)
(defmethod can-execute? :learn-action-class [_ _] true)
(defmethod can-execute? :pursue [_ _] true)
(defmethod can-execute? :decompose [_ _] true)

(defmethod can-execute? :address-sorry
  [state action]
  ;; Executable only if the target sorry-id is in state's open sorrys.
  (boolean (some #(= (:target action) (:id %))
                 (:sorrys state []))))

;; ---------------------------------------------------------------------------
;; v0.15: multi-horizon predict — chain K single-step predictions for the
;; same action. The K-th step's state is the input to the (K+1)-th
;; prediction. Carries the trajectory + a final-state map suitable for
;; EFE composition.
;; ---------------------------------------------------------------------------

(def default-horizon-steps 3)

(defn predict-multi-horizon
  "v0.15: predict K steps ahead by chaining `predict` K times, assuming
   the same action repeats.

   Returns:
     {:trajectory     <vec of K single-step `predict` outputs>
      :final-state    {:observation <next-mean after K steps>
                       :belief <next-belief after K steps>}
      :horizon-steps  K}

   Each step's input state is the previous step's predicted state:
   `:observation` = previous prediction's `:next-observation :mean`;
   `:belief` = previous prediction's `:next-belief`. Other state fields
   (`:sorrys`, `:anticipation`) pass through unchanged."
  ([state action] (predict-multi-horizon state action default-horizon-steps))
  ([state action K]
   (loop [step 0
          current-state state
          trajectory []]
     (if (>= step K)
       {:trajectory trajectory
        :final-state current-state
        :horizon-steps K}
       (let [prediction (predict current-state action)
             next-obs (get-in prediction [:next-observation :mean])
             next-belief (:next-belief prediction)
             next-state (-> current-state
                            (assoc :observation next-obs)
                            (assoc :belief next-belief))]
         (recur (inc step) next-state (conj trajectory prediction)))))))

(defn predict
  "Pure forward model.

   Input:
     state  — {:observation <obs map> :belief <belief map>}
     action — {:type <keyword in action-types>
               :target <entity-id, required for non-:no-op>
               :weight <number, optional, default 1.0>}

   Output:
     {:next-observation {:mean <obs map> :variance <obs map>}
      :next-belief      <updated belief map (a distribution by construction)>
      :action           <the action passed in>
      :predicted-events <seq of belief events the action emits>}

   Throws ex-info on invalid action (unknown :type, missing :target for
   non-:no-op). Pure: same (state, action) → same output."
  [{:keys [observation belief] :as _state} action]
  (when-not (valid-action? action)
    (throw (ex-info "Invalid action for forward model"
                    {:action action :action-types action-types})))
  (let [{:keys [obs-delta obs-variance events]} (predict-effects nil action)
        next-mean (merge-obs-delta (or observation {}) obs-delta)
        ;; Variance is per-channel; channels not touched by the action
        ;; default to 0.0 (deterministic prediction).
        next-var (into {}
                       (for [k (keys next-mean)]
                         [k (double (get obs-variance k 0.0))]))
        next-belief (belief/update-belief-batch (or belief {}) events)]
    {:next-observation {:mean next-mean :variance next-var}
     :next-belief next-belief
     :action action
     :predicted-events events}))
