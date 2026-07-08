(ns futon2.aif.temporal-hierarchy
  "R15 b3 — the fast/slow temporal HIERARCHY (the compositional-depth half).

  This is NOT rollout depth (horizon-H, b2, which is deeper lookahead at one
  timescale). This is TWO timescales: a SLOW (strategic) loop whose state
  PARAMETERIZES the priors/preferences of a FAST (tactical) loop.

  THE STRUCTURAL DIFFERENCE (load-bearing):
  - horizon-H (b2, DONE) changes how FAR the fast loop looks (H steps, γ^t).
  - hierarchy (b3, THIS) changes WHAT the fast loop WANTS (slow-loop state
    shapes the fast-loop's prior over which moves are desirable).

  In active inference terms: the slow loop maintains a higher-level generative
  model of the agent's strategic context (which phase of work it is in, what
  capability frontier is active). This context parameterizes the fast loop's
  priors — the per-move P(move) that `renormalize-priors` consumes. The fast
  loop then runs the existing rollout/act-gate machinery against those
  shaped priors.

  WHAT THIS SLICE IS (honest):
  - The two-loop PARAMETERIZATION plumbing: a slow-loop state value that
    visibly and mechanically shifts the fast-loop's move priors, changing
    which policy the rollout selects.
  - The slow-loop state is set by an explicit strategic context (operator
    or accumulated learning — R12 seam). The fast loop consumes it.

  WHAT THIS SLICE IS NOT (the research-level remainder):
  - A full nested generative model where the slow loop runs its OWN EFE
    minimization over a longer timescale, with message-passing between levels.
  - Learned slow-loop transitions (the slow-loop state is set explicitly, not
    learned from fast-loop outcomes — the feedback arc is named but not built).
  - Multi-level hierarchies (this is two levels: slow + fast).

  WIRING:
  - R12 (accumulated learning): the slow-loop state is WHERE accumulated
    learning lives — `intrinsic-values` (the R12 table) feeds the slow loop's
    strategic context. The seam is `slow-context-from-intrinsics`.
  - R19 (prior preferences): the slow-loop state parameterizes the fast-loop's
    PRIOR over moves (the `:prior` field), which is separate from the
    preference-based scoring in EFE. The slow loop shapes what the fast loop
    finds a priori attractive, before any EFE computation."
  (:require [futon2.aif.rollout :as rollout]
            [futon2.aif.intrinsic-values :as iv]))

;; ---------------------------------------------------------------------------
;; SLOW LOOP — strategic context that parameterizes fast-loop priors
;; ---------------------------------------------------------------------------

(def strategic-modes
  "The discrete strategic modes the slow loop can be in. Each mode shifts
  the fast-loop's prior over move classes. This is the upper-level state in
  the two-timescale hierarchy.

  These are NOT the same as `preferences/mode-prior` (which is the WM's
  equilibrium classification). These are the slow-loop's STRATEGIC PHASE —
  what kind of work the agent is currently oriented toward. The slow loop
  sets one of these; the fast loop's priors shift accordingly.

  The keys are regex patterns (strings) matched against the move's class
  name (also a string via `name`). This allows 'close' to match both
  :close-hole and any future close-prefixed class."
  {"exploitation"  {"close-hole" 0.7 "advance-capability" 0.2 "explore" 0.1}
   "exploration"   {"close-hole" 0.2 "advance-capability" 0.3 "explore" 0.5}
   "consolidation" {"close-hole" 0.5 "advance-capability" 0.4 "explore" 0.1}})

(defn slow-context-from-intrinsics
  "R12 SEAM: derive the slow-loop's strategic context from the accumulated
  learning in the intrinsic-values table (R12's per-action-class posteriors).

  This is the place where 'realized outcomes over a horizon update the priors
  the fast loop scores against' (per temporal-depth-beyond-greedy NEXT-STEPS).
  Today: reads the intrinsic-value table and classifies the strategic mode
  from the dominant action-class intensity. Returns a keyword in
  `strategic-modes` (or nil if no intrinsic data → the fast loop runs
  unparameterized, same as today).

  This is a REAL parameterization: the slow-loop state (which strategic mode)
  is DERIVED from accumulated learning (R12 intrinsics), not hardcoded."
  ([]
   (slow-context-from-intrinsics {}))
  ([{:keys [intrinsics]}]
   (let [table (or intrinsics (iv/current))
         modes (into {} (for [[k v] table]
                          [(name k) (double (:alpha v 0.0))]))]
     (cond
       (empty? modes) nil
       :else
       (let [sorted (sort-by val > modes)
             top-class (ffirst sorted)]
         (cond
           (re-find #"close" top-class) :exploitation
           (re-find #"capab" top-class) :consolidation
           (re-find #"explore" top-class) :exploration
           :else :exploitation))))))

(defn mode-prior-weights
  "Given a slow-loop strategic mode, return the prior weights over move
  classes that the fast loop should use. Returns nil if the mode is unknown
  or nil (→ the fast loop runs with its default/uniform priors, same as
  today — regression-safe)."
  [mode]
  (when mode
    (get strategic-modes (name mode))))

;; ---------------------------------------------------------------------------
;; FAST LOOP — rollout with slow-loop-parameterized priors
;; ---------------------------------------------------------------------------

(defn apply-slow-prior
  "PARAMETERIZATION (the core of the hierarchy): given a set of moves and a
  slow-loop strategic mode, shape each move's prior AND effective cost
  according to the mode's prior weights for that move's class.

  Two effects (both make the hierarchy REAL, not cosmetic):
  1. PRIOR shaping — the `:prior` field is multiplied by the mode weight
     (affects which moves the rollout explores via `renormalize-priors`).
  2. COST modulation — the effective `:delta-g` gets a prior-bonus term:
     `delta-g + ln(weight)` (negative bonus for favored classes). This is
     the AIF KL-divergence intuition: moves aligned with the prior get a
     free-energy bonus. The original `:delta-g` is preserved as
     `:delta-g-base` for traceability.

  A move whose class has no mode-weight is left untouched. This is honest:
  the slow loop shapes the classes it knows about, not all classes.

  ANTI-FAKING: this is NOT just relabeling rollout depth. The cost modulation
  changes WHAT the fast loop finds desirable (which move has lower G), not
  how FAR it looks. The proof is `hierarchy-is-not-rollout-depth`: the mode
  effect is independent of the horizon parameter."
  [moves mode]
  (let [weights (mode-prior-weights mode)]
    (if (or (nil? weights) (empty? moves))
      moves
      (mapv
       (fn [move]
         (let [cls (name (:move/class move :unknown))
               weight (some (fn [[pattern w]]
                              (when (re-find (re-pattern pattern) cls)
                                (double w)))
                            weights)]
           (if weight
             (let [base-prior (or (:prior move) 1.0)
                   base-dg (double (or (:delta-g move) 0.0))
                   ;; -ln(weight) is POSITIVE when weight < 1 (penalty for
                   ;; disfavored classes), zero at weight = 1. A HIGH prior
                   ;; weight → small penalty; a LOW prior weight → large
                   ;; penalty. This is the KL-divergence intuition: the cost
                   ;; of deviating from the slow loop's prior. Floor weight
                   ;; at a small epsilon to avoid ln(0).
                   prior-penalty (- (Math/log (max 1.0e-10 weight)))]
               (-> move
                   (assoc :prior (* base-prior weight)
                          :delta-g-base base-dg
                          ;; Effective delta-g gets the prior penalty:
                          ;; positive penalty → less negative delta-g →
                          ;; disfavored by the fast loop.
                          :delta-g (+ base-dg prior-penalty))))
             move)))
       moves))))

(defn hierarchical-rollout
  "The two-timescale fast loop: apply the slow-loop's prior parameterization,
  then run the standard rollout. The slow-loop mode SHAPES the fast-loop's
  priors before the rollout evaluates policies.

  This is the minimal composition: slow-loop state → fast-loop priors →
  rollout search. The fast loop is the existing `rollout/best-rollout` —
  unchanged except its inputs (the moves' priors) are now shaped by the
  slow loop.

  opts:
    :slow-mode  — keyword in strategic-modes (the slow-loop state). If nil,
                  the fast loop runs unparameterized (regression-safe).
    ...plus all standard rollout opts (horizon, top-k, etc.)."
  [state moves & {:keys [slow-mode] :as opts}]
  (let [shaped-moves (apply-slow-prior moves slow-mode)
        rollout-opts (->> (dissoc opts :slow-mode)
                          seq
                          (apply concat))]
    (apply rollout/best-rollout state shaped-moves rollout-opts)))
