(ns futon2.aif.portfolio-action-proposer
  "W-proposer-portfolio-actions (cards-II gap 5): the candidate generator
  proposes only advance moves — close/survey/apply-cascade never exist to
  price. This proposer adds the missing families.

  DARK by default (*portfolio-proposer-active?* false). When off,
  propose returns [] — the WM is byte-identical to the pre-portfolio
  composition. When on (operator ruling or dry-run), it surfaces:

  - :close-mission — one per mission whose status-class is :complete or
    :verified (candidate for closure)
  - :survey-mission — one per open mission (gathering information before
    committing to advance)
  - :apply-cascade — one per open mission that has a replayable deposit
    in escrow (the fold-turn's construction applies to this mission)

  All candidates carry :weight, :target, :rationale — the same shape as
  mission-enumerator-proposer's :advance-mission candidates, so the
  existing rank-actions / compute-efe / predict-effects pipeline prices
  them without modification.

  NOT live-enactable: the action-types already exist in forward-model
  with predict-effects arms, but can-execute? defaults to true only for
  actions a proposer surfaced. The enact path is gated separately by the
  WM's act-gate. This proposer makes the candidates VISIBLE and PRICEABLE;
  enactment remains the operator's call.

  U24 adds two things here and nothing else. (1) The `can-propose?` /
  `can-execute?` arms for `:survey-mission`, which READ THE SAME DARK FLAG.
  `can-propose?` returns false while it is false, which is exactly what the
  `:default` arm returned, so loading this namespace changes that answer for
  nobody. `can-execute?` is the one answer that moves: the `:default` arm
  returns TRUE for any action type, and this arm returns false while dark.
  That is strictly more conservative, and it is unreachable while dark,
  because nothing proposes a `:survey-mission` action for it to be asked
  about. What the arms buy is that arming the proposer also stops the
  bootstrap proposer emitting the `:learn-action-class` gap for
  `:survey-mission` -- otherwise
  the field would carry both a real survey candidate and a candidate saying
  survey is unavailable. On 2026-09-02 that gap candidate was in the live
  field with `:G-ambiguity 0.25` (the Beta-predictive-variance collision U4
  section 5 measured, 125 nats away from the entropy scale), so it is not a
  harmless duplicate. (2) The epistemic payload: when state carries
  `:survey-readings`, each survey candidate is enriched with
  `:survey-eig-nats` by `futon2.aif.survey-mission-value`. Without those
  readings the candidates are emitted exactly as before."
  (:require [futon2.aif.action-proposer :as ap]
            [futon2.aif.forward-model :as fm]
            [futon2.aif.survey-mission-value :as smv]))

(def ^:dynamic *portfolio-proposer-active?*
  "When false (DEFAULT), the portfolio proposer is dark — propose returns
  [] and the WM is byte-identical to the pre-portfolio composition. When
  true, close/survey/apply-cascade candidates are surfaced for pricing.
  Armed by operator ruling or dry-run dispatch (W-proposer-portfolio-actions,
  cards-II gap 5)."
  false)

(defn- close-candidates
  "One :close-mission candidate per mission whose status-class indicates
  completion or verification (candidate for closure)."
  [missions]
  (for [m missions
        :when (#{:complete :verified :pass} (:status-class m))]
    {:type :close-mission
     :target (:id m)
     :weight 0.5
     :rationale (str "portfolio: mission " (:id m)
                     " status " (name (:status-class m))
                     " — close-mission candidate")}))

(defn- survey-candidates
  "One :survey-mission candidate per open mission (information-gathering
  before committing to advance). Cheaper than advance; surfaces the
  option to look before leaping.

  U24: enriched with the epistemic payload when READINGS is non-nil. The
  forward model ignores `:target` for this type, so G-core is the same number
  for every one of these candidates; `:survey-eig-nats` is the only thing that
  tells two of them apart."
  ([missions] (survey-candidates missions nil))
  ([missions readings]
   (smv/enrich-survey-candidates
    readings
    (for [m missions]
      {:type :survey-mission
       :target (:id m)
       :weight 0.3
       :rationale (str "portfolio: survey " (:id m)
                       " before advance — information-gathering option")}))))

(defn- apply-cascade-candidates
  "One :apply-cascade candidate per mission that has a replayable deposit
  in escrow. State may carry :escrow-missions (a set of mission targets
  with deposits). When absent, no apply-cascade candidates are emitted."
  [missions escrow-missions]
  (when escrow-missions
    (for [m missions
          :when (contains? escrow-missions (:id m))]
      {:type :apply-cascade
       :target (:id m)
       :weight 0.7
       :rationale (str "portfolio: apply-cascade from escrow deposit for "
                       (:id m))})))

(def portfolio-action-proposer
  "The portfolio proposer. DARK by default (*portfolio-proposer-active?*
  false). When active, emits close/survey/apply-cascade candidates from
  the mission state so the WM can price portfolio-shaped value — not just
  advance moves."
  (reify ap/ActionProposer
    (propose [_ state]
      (if-not *portfolio-proposer-active?*
        []
        (let [missions (:missions state)
              escrow-missions (:escrow-missions state)]
          (concat (close-candidates missions)
                  (survey-candidates missions (:survey-readings state))
                  (apply-cascade-candidates missions escrow-missions)))))
    (proposer-id [_] :portfolio-action)))

;; ---------------------------------------------------------------------------
;; U24: :survey-mission addressability. Both arms are gated on the SAME dark
;; flag as `propose`, so loading this namespace with the flag false returns
;; exactly what `can-propose? :default` (false) and `can-execute? :default`
;; (true) already returned. Registering them here rather than in
;; mission_registry keeps the flag and the addressability claim in one place:
;; the class is addressable exactly when the proposer that addresses it is on.
;; ---------------------------------------------------------------------------

(defmethod fm/can-propose? :survey-mission
  [state _action-type]
  (boolean (and *portfolio-proposer-active?* (seq (:missions state)))))

(defmethod fm/can-execute? :survey-mission
  [state action]
  (boolean (and *portfolio-proposer-active?*
                (contains? (set (map :id (:missions state []))) (:target action)))))

(defn dry-run-portfolio
  "Return a map of action-type → candidate count for a given state,
  WITHOUT depending on *portfolio-proposer-active?*. Used for dry-run
  tests that show the portfolio contains advance + the new families.

  State must carry :missions (as the WM does). Optionally :escrow-missions
  for apply-cascade candidates."
  [state]
  (let [missions (:missions state)
        escrow-missions (:escrow-missions state)
        advance-count (count missions)
        close-count (count (close-candidates missions))
        survey-count (count (survey-candidates missions (:survey-readings state)))
        cascade-count (count (apply-cascade-candidates missions escrow-missions))]
    {:advance-mission advance-count
     :close-mission close-count
     :survey-mission survey-count
     :apply-cascade cascade-count}))
