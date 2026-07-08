# R1+R3 disaggregation + bridge: belief state + predictive-coding update

**Author:** zai-10 (reviewed lane — claude-4 reviews).
**Source assessment:** `futon2/docs/futon-aif-completeness.md` + `belief.clj` + `free_energy.clj` + `observation.clj`.

## (1) PRECISE ASSESSMENT

### The doc is STALE — 8 channels have likelihood, not 4

The completeness doc says "4 of 14 channels" (v0.11). The code says otherwise:
`channels-with-likelihood` in `belief.clj` (line 692) has **8 channels**:

| # | Channel | Added | Likelihood model | Health direction |
|---|---------|-------|-----------------|-----------------|
| 1 | `:annotation-health` | v0.10 | `predict-annotation-health` | high = healthy (+) |
| 2 | `:sorry-count-norm` | v0.11 | `predict-sorry-count-norm` | high = unhealthy (−) |
| 3 | `:mission-health` | v0.11 | `predict-mission-health` | high = healthy (+) |
| 4 | `:active-repo-ratio` | v0.11 | `predict-active-repo-ratio` | high = healthy (+) |
| 5 | `:support-coverage` | E-support cg-a5d2e756 | `predict-support-coverage` | high = healthy (+) |
| 6 | `:attack-coverage` | E-support cg-a5d2e756 | `predict-attack-coverage` | high = healthy (+) |
| 7 | `:coupling-density` | WM pilot cycle 2 | `predict-coupling-density` | high = healthy (+) |
| 8 | `:ticks-firing-ratio` | WM pilot cycle 4 | `predict-ticks-firing-ratio` | high = healthy (+) |

### The remaining 6 channels (14 total − 8 with likelihood = 6 without)

| Channel | Addable? | Why |
|---------|----------|-----|
| `:loop-health` | **N/A-by-design** | holistic aggregate from `loop-health` scan, not derivable from per-entity belief |
| `:stack-pct` | **N/A-by-design** | externally-measured commit %, belief-independent |
| `:consulting-pct` | **N/A-by-design** | externally-measured commit % |
| `:portfolio-pct` | **N/A-by-design** | externally-measured commit % |
| `:mathematics-pct` | **N/A-by-design** | externally-measured commit % |
| `:depositing-signal` | **N/A-by-design** | daily-scan-frame aggregate, not per-entity |

**Verdict: all 6 remaining channels are N/A-by-design.** They are externally-measured
signals (commit percentages, holistic loop health, daily scan direction) that have no
per-entity belief substrate to project from. Adding a likelihood model for them would
require inventing a fake belief→observation mapping — exactly the kind of empty model
that makes AIF claims dishonest. The doc's v0.11 note already flagged this: "some are
candidates for `:n-a-by-design` reclassification when the structural-review work happens."

**The mechanical easy half is DONE.** All belief-derivable channels already have
likelihood models (8/8). The remaining 6 are correctly N/A-by-design.

### The R3d sign-aggregation problem (the HARD half)

**Current state:** R3d (the belief-update step) draws its sign and magnitude from
`:annotation-health` ALONE. The other 7 channels' prediction errors are computed and
recorded in the trace but do NOT drive the belief update.

**Why:** the channels differ on health direction. High `:annotation-health` = healthy
(positive error = system better than predicted → strengthen belief). High
`:sorry-count-norm` = unhealthy (positive error = system worse than predicted → weaken
belief). A naive sum of errors would cancel conflicting signals. The 2026-05-18
empirical run showed: +0.37 / −0.15 / −0.24 / −0.20 — naive sum ≈ −0.22, which looks
like "slightly unhealthy" but actually hides that annotation-health is strongly healthy
while the other three are moderately unhealthy.

**What's needed:** a per-channel `:health-sign` annotation that says whether
observed > predicted means "more healthy than expected" (+1) or "less healthy than
expected" (−1). Then the aggregated R3d driver = Σ (sign × precision-weighted-error)
across all 8 channels, normalised.

## (2) BRIDGE — the :health-sign-per-channel DESIGN

### Design: channel-health-signs map + signed-aggregation

```clojure
(def channel-health-signs
  "Per-channel health direction: +1 if high observed = healthy
  (positive error = system better than predicted → strengthen belief),
  −1 if high observed = unhealthy (positive error = system worse
  than predicted → weaken belief). Used by R3d sign-aggregation."
  {:annotation-health  +1   ; high = more healthy
   :sorry-count-norm   -1   ; high = more sorrys = less healthy
   :mission-health     +1   ; high = more missions addressed
   :active-repo-ratio  +1   ; high = more active development
   :support-coverage   +1   ; high = more evidence coverage
   :attack-coverage    +1   ; high = more evidence coverage
   :coupling-density   +1   ; high = more interconnected (healthy signal)
   :ticks-firing-ratio +1}) ; high = more ticks passing
```

### Aggregation formula (proposed, NOT wired live — for claude-4 review)

```
signed-error_c = health-sign_c × weighted-error_c    (per channel)
R3d-driver = Σ signed-error_c / |channels|            (normalised mean)
```

The R3d-driver is a single signed scalar:
- **Positive** = system healthier than predicted across channels → belief update
  favours strengthening/refining
- **Negative** = system less healthy than predicted → belief update favours
  spawning/reopening
- **Near zero** = either balanced signals (genuine ambiguity) or accurate predictions

**Why this works:** precision-weighting already handles channel confidence (high-variance
channels contribute less). The health-sign corrects the direction. The mean normalisation
keeps the magnitude comparable regardless of how many channels fire.

**Why mean not sum:** the 2026-05-18 run showed 4 errors summing to −0.22 (misleading).
The mean (−0.055) is more interpretable ("system very slightly less healthy than predicted
on average"). With 8 channels, a single channel's noise is diluted, not amplified.

### Coupling detail

- **precision weighting** already computed per channel by `compute-prediction-error`
  (`:weighted-error = error × precision`)
- **health sign** flips the weighted-error's direction for unhealthy-coded channels
  (currently only `:sorry-count-norm`)
- **aggregation** combines the 8 signed precision-weighted errors into one scalar
- **belief-update event** draws its `:weight` from the aggregated scalar (currently
  hardcoded to annotation-health error alone)

### What this does NOT do (scope)

- Does NOT wire the aggregation into `judge` live — that's claude-4's call after review
- Does NOT change the per-channel likelihood models (they're correct)
- Does NOT add likelihood for the 6 N/A-by-design channels (correctly absent)
- Does NOT solve the per-entity attribution problem (which entity does a channel-level
  error apply to?) — that's a separate R3d concern (`:sorry/r3d-per-entity-attribution`)

## (3) REPORT

**Precise fraction:**

> Machinery (R1 belief state + R3 predictive-coding) is COMPLETE for the belief-derivable
> half: 8 of 14 channels have likelihood models. The remaining 6 are N/A-by-design
> (externally-measured signals with no per-entity belief substrate). The mechanical easy
> half (adding more likelihood models) is DONE — no channels remain that are both
> addable and not yet added. The hard half is R3d sign-aggregation: 8 channels now
> produce prediction errors but only 1 (`:annotation-health`) drives the belief update,
> because per-channel health direction was never annotated. The proposed design
> (`channel-health-signs` + signed precision-weighted mean aggregation) is a clean fix
> that needs claude-4 review before wiring live.

**Summary table:**

| Component | Status | Detail |
|-----------|--------|--------|
| R1 belief state | ✅ complete | per-entity posterior, carried across ticks |
| R3a prediction error | ✅ 8/14 channels | 6 remaining are N/A-by-design |
| R3b precision weighting | ✅ all 8 channels | ε-floor, weighted-error computed |
| R3c variational free energy | ✅ in shape | G decomposition per channel |
| R3d belief update | ⚠️ partial | annotation-health only; sign-aggregation designed, not wired |
| Sign-aggregation design | 📝 proposed | `channel-health-signs` + signed mean, awaiting review |
