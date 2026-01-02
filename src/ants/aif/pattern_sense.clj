(ns ants.aif.pattern-sense
  "Pattern observation for AIF agents.

   Computes pattern-relevant features that can influence AIF decision-making:
   - Whether the current mode aligns with the pattern's expected behavior
   - Whether pattern constraints are satisfied
   - Cost of switching to a different pattern

   These features can be used by the policy layer to incorporate pattern
   awareness into expected free energy calculations.")

;; -----------------------------------------------------------------------------
;; Pattern-mode alignment

(def ^:private pattern-mode-affinity
  "Map of pattern-id to expected mode(s).
   nil means all modes are acceptable."
  {:cyber/baseline       nil  ; all modes ok
   :cyber/hunger-coupling nil  ; applies in all modes
   :cyber/cargo-return   #{:homebound}  ; primarily for return phase
   :cyber/pheromone-tuner #{:outbound :maintain}  ; trail laying modes
   :cyber/white-space    #{:outbound}})  ; scouting mode

(defn mode-aligned?
  "Check if current mode aligns with pattern's expected mode(s)."
  [mode pattern-id]
  (let [expected (get pattern-mode-affinity pattern-id)]
    (or (nil? expected)  ; nil means all modes ok
        (contains? expected mode))))

;; -----------------------------------------------------------------------------
;; Pattern constraint checking

(defn- hunger-constraint-ok?
  "For hunger-coupling pattern: check if hunger dynamics are active."
  [ant observation]
  (let [h (or (:h observation) (get-in ant [:mu :h]) 0.4)]
    ;; Constraint satisfied if hunger is non-trivial (agent is "feeling" something)
    (or (< h 0.3) (> h 0.5))))

(defn- cargo-constraint-ok?
  "For cargo-return pattern: check if cargo discipline applies."
  [ant observation]
  (let [cargo (or (:cargo observation) 0.0)
        mode (or (:mode ant) :outbound)]
    ;; Constraint satisfied if:
    ;; - No cargo (discipline doesn't apply) OR
    ;; - Cargo present and mode is homebound (correct behavior)
    (or (< cargo 0.05)
        (and (>= cargo 0.05) (= mode :homebound)))))

(defn- white-space-constraint-ok?
  "For white-space pattern: check if we're actually in white space."
  [_ant observation]
  (let [food (or (:food observation) 0.0)
        pher (or (:pher observation) 0.0)
        food-trace (or (:food-trace observation) 0.0)]
    ;; Constraint satisfied if we're in sparse territory
    (and (< food 0.1)
         (< pher 0.2)
         (< food-trace 0.15))))

(defn- pheromone-constraint-ok?
  "For pheromone-tuner pattern: check if trail laying is warranted."
  [ant observation]
  (let [novelty (or (:novelty observation) 0.5)
        trail-grad (or (:trail-grad observation) 0.0)
        mode (or (:mode ant) :outbound)]
    ;; Constraint satisfied if:
    ;; - In exploration mode with novelty OR
    ;; - Trail gradient suggests following behavior
    (or (and (= mode :outbound) (> novelty 0.4))
        (> trail-grad 0.2))))

(defn constraint-satisfied?
  "Check if the active pattern's key constraint is satisfied."
  [ant observation pattern-id]
  (case pattern-id
    :cyber/baseline true  ; always satisfied
    :cyber/hunger-coupling (hunger-constraint-ok? ant observation)
    :cyber/cargo-return (cargo-constraint-ok? ant observation)
    :cyber/white-space (white-space-constraint-ok? ant observation)
    :cyber/pheromone-tuner (pheromone-constraint-ok? ant observation)
    true))  ; unknown patterns: assume ok

;; -----------------------------------------------------------------------------
;; Pattern switch cost

(defn switch-cost
  "Compute cost of switching away from current pattern.
   Higher values discourage pattern switching."
  [ant]
  (let [ticks-active (or (get-in ant [:cyber-pattern :ticks-active]) 0)]
    ;; Sigmoid-like function: low cost early, stabilizes after ~10 ticks
    (min 1.0 (* 0.1 (Math/log (+ 1 ticks-active))))))

;; -----------------------------------------------------------------------------
;; Main API

(defn pattern-features
  "Compute pattern-relevant observation features for an ant.

   Returns a map with:
   - :pattern/active - the active pattern id (or nil)
   - :pattern/mode-aligned? - whether current mode matches pattern expectation
   - :pattern/constraint-ok? - whether pattern's key constraint is satisfied
   - :pattern/switch-cost - cost of switching to a different pattern"
  [ant observation]
  (let [pattern-id (get-in ant [:cyber-pattern :id])]
    (if pattern-id
      {:pattern/active pattern-id
       :pattern/mode-aligned? (mode-aligned? (:mode ant) pattern-id)
       :pattern/constraint-ok? (constraint-satisfied? ant observation pattern-id)
       :pattern/switch-cost (switch-cost ant)}
      ;; No pattern active
      {:pattern/active nil
       :pattern/mode-aligned? true
       :pattern/constraint-ok? true
       :pattern/switch-cost 0.0})))

(defn increment-ticks-active
  "Increment the ticks-active counter for the current pattern."
  [ant]
  (if (get-in ant [:cyber-pattern :id])
    (update-in ant [:cyber-pattern :ticks-active] (fnil inc 0))
    ant))
