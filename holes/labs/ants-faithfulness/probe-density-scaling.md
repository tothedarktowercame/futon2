# Probe: density-preserving scaling (2026-08-02)

**This is a PROBE, not a registered experiment.** Its only job is to locate a
viability window so the real experiment can be registered against a discriminating
environment. Exploratory by construction; 5 seeds, one scenario, two arms.
Script: `probe-density-scaling.clj`.

## Hypothesis

R-0 held food patch **count** fixed (4) while board area grew 13x, so food
**density** collapsed by 13x. The 36x36 colonies starved (0.900 patchy) because
there was nothing to find, not because the board was large. Fix: patches ∝ area.

## Result

| grid | patches | ticks | yield | starvation | `no-directed-eig` identical to `aif-full` |
|---|---:|---:|---:|---:|---:|
| 10x10 | 4 | 300 | 146.02 | 0.000 | **5/5** |
| 24x24 | 23 | 720 | 421.96 | 0.200 | **5/5** |
| 36x36 | 52 | 1080 | 534.66 | 0.400 | **5/5** |

### 1. The density hypothesis was right

Against R-0's fixed-patch patchy cells:

| | R-0 (4 patches) | probe (density-scaled) |
|---|---|---|
| 36x36 yield | 75.37 | **534.66** |
| 36x36 starvation | 0.900 | **0.400** |

Density collapse, not board size, is what killed the colonies. Correcting it
recovers a viable colony at 36x36.

### 2. And the epistemic term is still inert — more clearly than before

Ablating `directed-eig` produces **identical yields to two decimal places on 5/5
seeds at every grid size**. Not a small effect; no effect.

This is the result R-0 could not deliver, because its large grids were lethal and
a shared floor is not evidence. With density corrected the environment is viable
and the ablation still changes nothing.

**So the inertness finding is not an artifact of the 10x10 grid.** The confound
was real and correcting it strengthens the original conclusion rather than
overturning it.

## Residual, stated rather than buried

Starvation still climbs with scale: 0.000 → 0.200 → 0.400. Density-preserving
scaling is **necessary but not sufficient** — something else degrades with board
size, most likely the energy cost of travel between patches against fixed
metabolism and reserves. The 36x36 cell is viable but not healthy.

A registered experiment should therefore scale *energetics* with board diameter
too, and declare that as its assumption — with a `:breaks-when` naming the
**energetic** failure mode, which is precisely what R-0's tick-scaling assumption
got wrong.

## Limits

Five seeds, one scenario (patchy), two arms, no registration. It locates a window;
it does not measure an effect. R-0's snowdrift signal (identity 30/30 → 27/30 →
23/30) is untouched by this probe and remains the one place an epistemic term
showed any movement at all.
