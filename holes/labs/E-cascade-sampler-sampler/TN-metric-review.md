# TN-metric-review — why the metric-fed GFlowNet isn't separating (it's the reward, not the embedding)

**Date:** 2026-06-12 · author: claude-6 (this session) · **for: claude-6 (cascade-sampler thread)**
**Re:** six-arm contest v1, checkpoint 6 — metric-fed GFlowNet did NOT separate from flat-proxy
(median G −9.53 vs −9.49, within noise); greedy-ε still holds at −10.73; 114–125 distinct
selections per 128 draws even after metric training.

## TL;DR

The contest's own diagnosis ("the 1.06–2.74 reward range was too weak to concentrate the TB
policy") is right, but the cause is **not** a flat substrate metric and **not** the embedding. The
underlying metric is genuinely varied. The reward is flat because the **set-aggregation collapses
it** (`sum` of intensities × `mean` of pairwise coherence → central-limit narrowing). The lever is
a **reward temperature / peaked aggregation**, which the checkpoint already gestured at — *test it
before switching the embedding to BGE*, because BGE won't fix an aggregation collapse.

## Evidence (measured from `s4/metric-matrix-v0.json`)

The reward is `C = T × H` (`s4/metric_c_proxy.py`):
- **T = sum** of per-target `action_intensity` (substrate-metric v1 curvature).
- **H = mean** of `4·s·(1−s)` over pairwise `s = 1/(1+hop-distance)`.

The factors are **not flat**:
- `action_intensity`: n=85, mean 0.511, **stdev 0.199, 71 distinct** values on [0, 0.853] (~40% rel. spread).
- coherence: hop-distances 1–9 → `4s(1−s)` spans **0.36 → 1.0** (mode at hop=3).

So the metric the reward is built on has real structure. But:
- `C ≈ (k · meanT) · meanH ≈ 0.38·k` — the reward range **1.06–2.74 is set mostly by `k` (#
  targets), not by *which* targets.** Summing T and averaging H washes the per-element contrast out
  (a central-limit collapse). The ~20–30% per-set signal that survives is far too shallow for a
  Trajectory-Balance target `R/Z` to concentrate — which is **exactly** the 114–125-distinct /
  128-draws symptom (a near-uniform policy ⇔ a near-flat reward).

## The lever: steepen the reward, don't (only) sharpen the metric

The TB loss matches the policy to `R(x)/Z`. A linear `C ∈ [1, 2.7]` gives almost no gradient.
Concrete, cheap-to-test changes (in rough order):
1. **Reward temperature:** use `R = exp(β·C)` (or `C^β`). β≈3–5 turns a 2.6× range into hundreds×;
   the TB target peaks, distinct-selections drops, the metric arm should clear flat-proxy.
2. **Peaked aggregation:** replace `mean`/`sum` with `max` / top-k pair / softmax over targets, so
   the reward responds to *which* targets, not just *how many*.
3. (secondary) more TB steps and/or anneal β over training.

## On the embedding (the thing Joe is rebuilding to BGE)

BGE sharpens the *substrate* — the graph (→ hop-distances) and the Ollivier–Ricci curvature
(→ `action_intensity`) — so it gives the reward *more* to amplify, and is worth doing for its own
sake. **But it is the substrate lever, not the bottleneck lever here.** If you switch to BGE
expecting the GFlowNet to separate, the most likely outcome is the *same* flat result, because the
collapse is in the `sum/mean` + linearity, not the embedding floor. Do both; **test the reward
temperature first** — it isolates "reward vs embedding" empirically.

## Suggested experiment (tiny — no model load, no OOM risk)

Re-run the contest's metric arm with `R = exp(β·C)` swept over β ∈ {1, 2, 3, 5, 8}, holding
everything else fixed. Report median G + distinct-selections/128 per β. Prediction: distinct count
falls and median G separates from flat-proxy somewhere around β≈3–5; if it does NOT even at β=8,
*then* the metric/embedding is genuinely too flat and BGE becomes the next lever.

## Files
- `s4/metric_c_proxy.py` — the `C = T×H` reward (the place to add β / change aggregation).
- `s4/metric-matrix-v0.json` — `intensity` (T) + `distances` (H); the measured ranges above.
- `s4/contest_run_v1.clj`, `contest-verdicts-v1.edn` — the contest harness + result.
