(ns ants.aif.default-mode
  "Pre-deliberative baseline action selection — tropisms and gradients.

   The Default Mode provides a parallel path from observation to action
   that does not depend on EFE evaluation (policy/choose-action). This
   gives the ant system compositional closure (I6): if the deliberative
   policy fails or is unavailable, baseline behavior persists.

   Design constraints:
   - Accepts observations ONLY — no preferences, no EFE weights
   - Simpler and faster than full EFE evaluation
   - Returns the same shape as policy/choose-action for drop-in use
   - Tropism-based: follow gradients, respond to cargo state, head home

   See futon5/docs/chapter0-aif-as-wiring-diagram.md § 6.")

(def ^:private thresholds
  {:cargo-high  0.50    ; carry enough food → head home
   :trail-min   0.15    ; pheromone strong enough to follow
   :hunger-high 0.70    ; getting dangerously hungry
   :home-near   0.80})  ; close enough to count as "at home"

(defn select-action
  "Tropism-based action selection from observation alone.

   Returns the same shape as policy/choose-action:
   {:action keyword
    :policies {action {:G 0.0 :p 1.0 :source :default-mode}}
    :tau 1.0
    :source :default-mode}"
  [observation]
  (let [cargo     (double (or (:cargo observation) 0.0))
        hunger    (double (or (:h observation) (:hunger observation) 0.0))
        home-prox (double (or (:home-prox observation) 0.0))
        trail     (double (or (:trail-grad observation) 0.0))
        pher      (double (or (:pher observation) 0.0))
        food      (double (or (:food observation) 0.0))
        {:keys [cargo-high trail-min hunger-high home-near]} thresholds

        action
        (cond
          ;; Loaded with food → return to nest
          (>= cargo cargo-high)
          :return

          ;; Dangerously hungry and not near home → return for safety
          (and (>= hunger hunger-high) (< home-prox home-near))
          :return

          ;; On food → forage
          (> food 0.1)
          :forage

          ;; On a pheromone trail → follow it (forage along the gradient)
          (>= trail trail-min)
          :forage

          ;; Near home with no cargo → go find food
          (and (>= home-prox home-near) (< cargo 0.05))
          :forage

          ;; Nothing interesting happening → hold position
          :else
          :hold)]
    {:action   action
     :policies {action {:G 0.0 :p 1.0 :source :default-mode}}
     :tau      1.0
     :source   :default-mode}))
