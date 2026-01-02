(ns ants.aif.pattern-efe
  "Pattern contribution to expected free energy.

   Adds pattern-aware terms to the EFE calculation:
   - Pattern action risk: penalty for actions that violate pattern constraints
   - Pattern info gain: reward for actions that gather pattern-relevant information

   These terms allow patterns to influence action selection beyond initial
   config, e.g., a 'cargo-return-discipline' pattern penalizes thrashing.")

;; -----------------------------------------------------------------------------
;; Pattern-specific action risk

(defn- cargo-return-risk
  "Risk for cargo-return-discipline pattern.
   Penalizes empty returns and thrashing behavior."
  [action observation]
  (let [cargo (or (:cargo observation) 0.0)
        mode (or (:mode observation) :outbound)
        home-prox (or (:home-prox observation) 0.0)]
    (cond
      ;; Penalize returning with very little cargo
      (and (= action :return)
           (< cargo 0.15)
           (not= mode :homebound))
      0.4

      ;; Penalize foraging when already loaded
      (and (= action :forage)
           (>= cargo 0.5)
           (= mode :homebound))
      0.3

      ;; Slight penalty for holding near home with cargo
      (and (= action :hold)
           (>= cargo 0.3)
           (> home-prox 0.7))
      0.15

      :else 0.0)))

(defn- white-space-risk
  "Risk for white-space-scout pattern.
   Penalizes foraging in non-sparse areas, rewards scouting."
  [action observation]
  (let [food (or (:food observation) 0.0)
        pher (or (:pher observation) 0.0)
        novelty (or (:novelty observation) 0.5)
        white? (>= (or (:white? observation) 0.0) 0.5)]
    (cond
      ;; In white space: penalize pointless foraging
      (and white?
           (= action :forage)
           (< food 0.05))
      0.3

      ;; Not in white space but pattern active: slight penalty for scouting
      (and (not white?)
           (= action :pheromone)
           (< novelty 0.3))
      0.1

      :else 0.0)))

(defn- hunger-coupling-risk
  "Risk for hunger-precision-coupling pattern.
   Penalizes ignoring hunger signals."
  [action observation]
  (let [h (or (:h observation) (:hunger observation) 0.4)
        home-prox (or (:home-prox observation) 0.0)
        cargo (or (:cargo observation) 0.0)]
    (cond
      ;; Very hungry but not returning: penalty
      (and (> h 0.7)
           (not= action :return)
           (> cargo 0.1))
      0.35

      ;; Starving but foraging far from home: high penalty
      (and (> h 0.85)
           (= action :forage)
           (< home-prox 0.3))
      0.5

      :else 0.0)))

(defn- pheromone-tuner-risk
  "Risk for pheromone-trail-tuner pattern.
   Penalizes laying trails when inappropriate."
  [action observation]
  (let [home-prox (or (:home-prox observation) 0.0)
        novelty (or (:novelty observation) 0.5)
        reserve-home (or (:reserve-home observation) 0.5)]
    (cond
      ;; Laying pheromone on home: wasteful
      (and (= action :pheromone)
           (> home-prox 0.9))
      0.25

      ;; Laying pheromone during reserve crisis: should prioritize return
      (and (= action :pheromone)
           (< reserve-home 0.2))
      0.2

      ;; Not laying in novel territory: slight penalty
      (and (not= action :pheromone)
           (> novelty 0.7)
           (< home-prox 0.5))
      0.1

      :else 0.0)))

(defn pattern-action-risk
  "Compute risk of action given active pattern constraints.
   Higher values discourage the action."
  [pattern-id action observation]
  (case pattern-id
    :cyber/baseline 0.0  ; baseline has no additional constraints
    :cyber/cargo-return (cargo-return-risk action observation)
    :cyber/white-space (white-space-risk action observation)
    :cyber/hunger-coupling (hunger-coupling-risk action observation)
    :cyber/pheromone-tuner (pheromone-tuner-risk action observation)
    0.0))  ; unknown patterns: no risk

;; -----------------------------------------------------------------------------
;; Pattern-specific information gain

(defn- cargo-return-info-gain
  "Info gain for cargo-return pattern.
   Value completing the return cycle."
  [action observation]
  (let [cargo (or (:cargo observation) 0.0)
        home-prox (or (:home-prox observation) 0.0)]
    (cond
      ;; Returning with cargo near home: high info value (completing cycle)
      (and (= action :return)
           (>= cargo 0.3)
           (> home-prox 0.6))
      0.2

      :else 0.0)))

(defn- white-space-info-gain
  "Info gain for white-space pattern.
   Value exploring sparse areas."
  [action observation]
  (let [novelty (or (:novelty observation) 0.5)
        white? (>= (or (:white? observation) 0.0) 0.5)]
    (cond
      ;; Exploring novel territory in white space
      (and white?
           (or (= action :forage) (= action :hold))
           (> novelty 0.6))
      0.15

      ;; Laying breadcrumb trail in sparse area
      (and white?
           (= action :pheromone)
           (> novelty 0.5))
      0.1

      :else 0.0)))

(defn pattern-info-gain
  "Compute information value of action for pattern learning.
   Higher values encourage the action."
  [pattern-id action observation]
  (case pattern-id
    :cyber/baseline 0.0
    :cyber/cargo-return (cargo-return-info-gain action observation)
    :cyber/white-space (white-space-info-gain action observation)
    ;; Other patterns: default to 0
    0.0))

;; -----------------------------------------------------------------------------
;; Main API

(defn pattern-efe
  "Compute pattern-specific EFE term for an action.

   Returns a map with:
   - :G - the total pattern contribution (risk - info-gain, weighted by lambda)
   - :pattern-risk - raw risk value
   - :pattern-info - raw info gain value

   If lambda.pattern is 0 (default), returns zero contribution for backward compat."
  [pattern-id action observation config]
  (let [lambda-pattern (get-in config [:efe :lambda :pattern] 0.0)]
    (if (zero? lambda-pattern)
      {:G 0.0 :pattern-risk 0.0 :pattern-info 0.0}
      (let [risk (pattern-action-risk pattern-id action observation)
            info (pattern-info-gain pattern-id action observation)
            ;; G = lambda * (risk - info)
            ;; Higher risk = higher G = less preferred
            ;; Higher info = lower G = more preferred
            g (* lambda-pattern (- risk info))]
        {:G g
         :pattern-risk risk
         :pattern-info info}))))
