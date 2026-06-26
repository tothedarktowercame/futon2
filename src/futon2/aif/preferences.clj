(ns futon2.aif.preferences
  "Preferences (C) and avoided states for the War Machine's AIF inference.

   Sourced from `war-machine-terminal-vocabulary.edn` :C/preferred,
   :C/avoided, :C/mode-prior, :G/pragmatic-fn. The data here is the
   substrate the free-energy computation reads against.")

(def preferences
  "Expected observation ranges from war-machine-terminal-vocabulary.edn :C/preferred.
   Each channel maps to [lo hi] — the range where things are healthy."
  {:loop-health        [0.8 1.0]
   :support-coverage   [0.8 1.0]
   :attack-coverage    [0.8 1.0]
   :mission-health     [0.5 1.0]
   :stack-pct          [0.15 0.25]
   :consulting-pct     [0.20 0.35]
   :portfolio-pct      [0.20 0.35]
   :mathematics-pct    [0.15 0.25]
   :active-repo-ratio  [0.5 1.0]
   :sorry-count-norm   [0.0 0.3]
   :coupling-density   [0.1 0.3]
   :ticks-firing-ratio [0.0 0.0]
   :annotation-health  [0.7 1.0]})

(def avoided-states
  "States the system should not be in. From :C/avoided."
  {:strategic-mode     :hermit
   :stack-pct          [0.7 1.0]
   :consulting-pct     [0.0 0.0]
   :ticks-firing-ratio [0.5 1.0]
   :sorry-count-norm   [0.8 1.0]
   :active-repo-ratio  [0.0 0.2]})

(def mode-prior
  "Prior probability over strategic modes. From :C/mode-prior.
   :stop-the-line is an override-mode (see :μ/override-modes in
   war-machine-strategic-vocabulary.edn) — included here with prior 0.0
   so it never appears as an equilibrium-classification choice; the
   override-check in war-machine/judge sets it directly when the
   metabolic-balance tripwire fires."
  {:multiplied       0.35
   :depositing       0.25
   :foraging-trapped 0.15
   :hermit           0.10
   :stagnant         0.10
   :dark             0.05
   :stop-the-line    0.0})

(def pragmatic-weights
  "Per-channel weights for pragmatic free energy.
   From :G/pragmatic-fn in terminal vocabulary."
  {:stack-pct          0.25
   :consulting-pct     0.25
   :portfolio-pct      0.15
   :mission-health     0.15
   :ticks-firing-ratio 0.10
   :sorry-count-norm   0.10})

(defn current-C
  "The CURRENT channel-preference component of C — the per-channel `[lo hi]`
   ranges EFE's risk measures predicted outcomes against. Indirection seam for
   E-C-vector-live (§4.5): today it returns the static `preferences` floor, so
   it is behaviour-identical to reading `preferences` directly (regression-safe,
   the augment-don't-rip-out floor). The LIVE goal-OUTCOME half of C is
   delivered separately by `futon2.aif.c-vector` (derived, freshness-guarded,
   atom-backed); a future channel-liveness can override here without touching
   the consumers."
  []
  preferences)

(def channel-health-signs
  "v0.16: per-channel sign convention for R3d multi-channel aggregation.
   `+1` = higher observed value is HEALTHIER (positive prediction-error
        indicates the graph is healthier than belief expected → push
        entity beliefs toward :strengthened).
   `-1` = higher observed value is UNHEALTHIER (positive prediction-error
        indicates the graph has MORE undesirable signal than belief
        expected → push toward :foreclosed).

   Used by `futon2.report.war-machine/judge`'s R3 inner-loop to combine
   per-channel weighted-errors into a single signed scalar that drives
   the synthetic global belief-update event.

   Channels not in this map contribute 0 to the aggregate (no
   directional information available for them yet)."
  {:annotation-health  +1   ; high = more in-range entities, fewer anomalies
   :sorry-count-norm   -1   ; high = many open sorrys (unhealthier)
   :mission-health     +1   ; high = mission triage healthy
   :active-repo-ratio  +1}) ; high = entities non-dormant (active)
