(ns futon2.aif.action-proposer
  "Action proposers for the WM AIF apparatus.

   An ActionProposer surfaces candidate action maps for a given state.
   `rank-actions` / `compute-efe` take any seq of action maps — proposers
   are the adapter layer that decides *which* actions are available given
   the current substrate.

   The bootstrap-proposer is the always-available default: it surfaces
   `:no-op` (for abstain comparison) plus one `:learn-action-class` action
   per action-type whose `forward-model/can-propose?` returns false. This
   is the 'need to learn' move (per Joe 2026-05-17): when no concrete
   actions exist for an action class, the highest-priority recommendation
   is to enable the class itself. Substrate-aware proposers (e.g. a
   future substrate-2-enumerator-proposer) plug in via the protocol; each
   activates only when its target substrate is actually addressable.

   Contract: this namespace doesn't move R-criteria on its own; it's
   compositional infrastructure for R6 (action selection). As of the R12
   narrow-take-up landing, `:learn-action-class` recommendations carry an
   `:intrinsic-value` sourced from `futon2.aif.intrinsic-values/credit-for`
   (atom-backed posterior) rather than a static 0.1 — see
   `~/code/futon0/holes/missions/M-the-futon-stack-Q6-r12-design-choices.md`."
  (:require [futon2.aif.forward-model :as fm]
            [futon2.aif.intrinsic-values :as iv]))

(defprotocol ActionProposer
  (propose [_ state]
    "Return a seq of candidate action maps the proposer can offer for
     this state. May be empty if the proposer has nothing to offer.")
  (proposer-id [_]
    "Stable keyword identifier for tracing / logging."))

(defn- gap-actions
  "For each action-type that is NOT proposable in the current state
   (excluding :no-op and :learn-action-class themselves), emit a
   :learn-action-class action carrying the rationale."
  [state]
  (let [base-types (disj fm/action-types :no-op :learn-action-class)
        gaps (remove #(fm/can-propose? state %) base-types)
        raw-actions
        (for [target-class gaps]
          {:type :learn-action-class
           :target-class target-class
           :intrinsic-value (iv/credit-for target-class)
           :rationale (str "no addressable entities for " target-class
                           " in current substrate")})]
    ;; Deduplicate by intrinsic-value: when multiple gap-actions share the
    ;; same intrinsic-value (the common case at Beta(1,1) prior, where every
    ;; class starts at 0.5), they produce an identical EFE and trip the
    ;; policy-nondiscrimination gate. Keep only the first representative per
    ;; intrinsic-value band; the omitted classes are still modelled as gaps
    ;; (their absence is the signal) but do not inject degenerate duplicates
    ;; into the ranked-action set. When the outer loop has differentiated the
    ;; classes (different Beta posteriors), each gets its own action.
    (->> raw-actions
         (sort-by :target-class)
         (reduce (fn [acc action]
                   (if (some #(= (:intrinsic-value action)
                                 (:intrinsic-value %))
                             acc)
                     acc
                     (conj acc action)))
                 []))))

(def bootstrap-proposer
  "The always-available default proposer. Surfaces:
   - `:no-op` (for the abstain comparison in `policy/select-action`)
   - one `:learn-action-class` action per declared action-type whose
     `forward-model/can-propose?` returns false in the current state.

   This is the 'actionable self-model' layer: the WM models its own
   capability boundary and surfaces gaps as actions, rather than faking
   actions over non-existent entities."
  (reify ActionProposer
    (propose [_ state]
      (concat [{:type :no-op}]
              (gap-actions state)))
    (proposer-id [_] :bootstrap)))

(defn compose-proposers
  "Concatenate proposals from multiple proposers in order; de-duplicate
   by action-map equality. Returns a vector."
  [proposers state]
  (->> proposers
       (mapcat #(propose % state))
       distinct
       vec))
