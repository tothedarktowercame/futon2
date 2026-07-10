# TN — GFlowNets review (Fable): why the slice-2 nulls were guaranteed, and a path that can work

**Author:** Fable (session 2026-07-10) · **Status:** REVIEW + PROPOSED PLAN (Joe to approve dispatch)
**Scope:** review of the slush-demo GFN line — `futon2/holes/labs/slush-demo/` slices 1–2
(v1/v2 nulls, v3 spec) against the one positive run (`findings/slush_sweep_findings.md`).
**Relation to prior docs:** the v3 reward spec
(`labs/slush-demo/HANDOFF-slice2-v3-aliveness-selfevidencing.md`) stands unchanged; this
note explains why the *samplers* kept nulling and what to build so the next null (or pass)
is informative. Sibling context: `E-gflownets-fold.md`, `E-cascade-sampler-sampler.md`.

## Findings — three defects, each alone sufficient for the observed nulls

I read the slice-2 code and findings. **None of the three is a data-scale problem.**

### F1. The GFN was never trained (training-scale bug)
`train_policy` (`labs/slush-demo/slice2/slice2_discharge_gfn.py:263-291`) runs `steps`
gradient updates with **one trajectory per step, batch size 1**, and the eval configs used
`steps ∈ {6, 8, 12}` (default 12, line 617). With ~80 training missions, most missions
were *never sampled even once* during training. The one run in this lab that ever showed
life — the slush sweep — used **3000 steps**. Slice-2 used **250× less** training than the
configuration already known to work.

Corroborating detail: `PatternPolicy.scorer` is zero-initialized except for unit weights
on the (rel, bonus) scalar features (lines 66–95), so the *initial* policy is already a
Boltzmann ranking of `rel + bonus`. At 12 steps the "GFN" evaluated in slice-2 v1/v2 was
effectively that untrained prior — which is exactly what the numbers show (v1: GFN 1.2% ≈
retrieval beam 1.0%; v2: rel+aliveness 3.9% ≈ rel-only 3.6%).

### F2. Trajectory balance with a single shared scalar logZ is mis-specified for
### mission-conditioned rewards
The TB loss `(log_z + log_pf − log R(S|m))²` (line 287) uses one global `self.log_z`
(line 76), but the true partition function Z(m) = Σ_S R(S|m) **differs per mission**,
plausibly by orders of magnitude. One scalar cannot satisfy TB for two missions with
different Z; the gradients fight and the policy converges to nothing meaningful even with
ample steps. Standard fixes:
- logZ(m): a per-mission learned table (train missions) or a small MLP over mission
  features; or
- a logZ-free objective (Detailed Balance / Sub-Trajectory Balance), which also gives
  denser credit assignment on short trajectories like ours.

### F3. The v1/v2 rewards were additive over patterns — a GFN could not have helped
`log_reward = Σ_{p∈S} (α·rel(p) + β·bonus(p))` (lines 258–260). When log-reward is
additive over items, the Gibbs distribution over sets factorizes and **per-pattern ranking
is already the optimal sampler**. There is no compositional structure for a GFN to learn;
the null was structural, not empirical. The v3 reward is the first one that is genuinely
set-compositional: want-coverage is submodular (a pattern's marginal accuracy depends on
what is already selected), and the complexity term makes over-selection costly. **v3 is
the first reward where building a GFN is even a meaningful experiment.**

### F4. (Already recognized in v3) The eval metric was wrong
Recovery-of-`:applied` measures base-rate/mission-conditioning, which is why
popularity-only (8.9%) beat everything trained. v3's held-out-quality eval is the right
replacement; keep it.

### On "possibly this is a scale issue"
Not data scale — training scale (F1), objective correctness (F2), and reward structure
(F3). All three are cheap to fix; none requires more corpus, bigger models, or GPUs.
Data/model scale becomes the question only at Slice 3 below, after a sound trainer shows
signal on a validated reward.

## The plan — four slices, each with a hard gate

Ordering principle: **never debug the sampler and the reward at the same time.** Slice 0
validates the trainer on synthetic rewards with enumerable ground truth; Slice 1 (= the
v3 gate, unchanged) validates the reward with no sampler; only Slice 2 combines them.

### Slice 0 — trusted trainer harness (bell → codex)
Build `gfn_core.py`: batched TB **and** SubTB losses, conditional logZ(m), replay buffer
(optional), loss/reward/mode-coverage curves logged every run. Validate on synthetic
problems small enough to enumerate exactly:
- pool n=12–16, max_len 4 → full set-space enumerable; synthetic **submodular** reward
  (coverage-style, so it rehearses the v3 shape).
- **Gate G0a (correctness):** total-variation distance between the trained sampler's
  distribution and the exact Gibbs distribution < 0.05; learned logZ within 0.1 nat of
  the enumerated true logZ.
- **Gate G0b (bug demonstration):** ≥3 synthetic "missions" with Z differing by ≥e³;
  shared-scalar-logZ variant must *fail* G0a while conditional-logZ (or SubTB) passes.
  This pins F2 as real and fixed, auditable forever.
- Deterministic given seed; CPU; pytest green.

### Slice 1 — v3 reward gate (claude-1, exactly as specced in HANDOFF-slice2-v3)
Run the reward-before-generator gate as written (spread, LOO-AUC vs alive/mess beating
shuffle null, mandatory anti-`1=1` and bloated-shell controls). Two additions:
- **Precompute coverage bitsets:** run `rollout_execute` once per (mission, pattern) to
  get the covered want-atom set; store as bitmasks. Set-level accuracy = popcount of the
  OR. This makes reward evaluation ~µs, which is what makes 3000–10000 training steps
  affordable later. (If per-pattern discharge does not compose as union-of-atoms, measure
  the gap on a sample and decide: exact-but-slow with memoization vs bitset surrogate for
  training + exact reward for final eval.)
- **Gate G1-comp (compositionality):** verify marginal coverage gains actually interact
  (submodularity is doing work) on real missions. If accuracy turns out ~additive over
  patterns, STOP: ranking suffices and no sampler experiment is warranted (F3 lesson).

### Slice 2 — the real GFN (bell → codex; only after G0 and G1 both pass)
Train the Slice-0 trainer on the Slice-1 reward. Config floor: ≥3000 steps, batch 16–64
trajectories/step, conditional logZ or SubTB, lr sweep {1e-3, 3e-3}, β and λ ablations
per the v3 spec.
- **Baselines that matter:** (a) **greedy submodular maximization on the same reward** —
  the honest baseline for coverage rewards, not popularity; (b) simulated annealing /
  MCMC at *matched reward-evaluation budget*; (c) retrieval-prior beam; (d) popularity.
- **Eval (v3's, made precise):** held-out mean aliveness_v3 of proposals AND diversity
  (# distinct above-threshold modes per mission). The GFN's value proposition is
  *matching greedy's reward while beating everything on diverse high-reward modes* —
  exactly what the slush sweep showed at 3000 steps (338–396 top-modes vs greedy's 1).
- **Nulls:** label-shuffle destroys the lift; anti-gaming controls (trivial cascade,
  bloated shell) score dead end-to-end.
- HONEST NULL reportable. But note: for the first time, a null here would actually be
  *informative about the idea*, because trainer and reward were validated independently.

### Slice 3 — scale, only now
If Slice 2 shows signal: grow pool_top_n (240 → full library), max_len, model dim, and
corpus; consider per-mission fine-tuning from a shared backbone. If Slice 2 nulls with a
validated trainer and reward: the compositional-cascade-proposal thesis itself is what
failed — write that up and stop; do not reach for scale as a rescue.

## Dispatch mapping (per CLAUDE.md handoff protocol)
- Slice 0 → idle codex agent, bell, with G0a/G0b acceptance bar (pure Python lab;
  pytest + deterministic-seed gate). Bell back with SHAs + gate table.
- Slice 1 → claude-1 (it authored the v3 spec and owns the reward gate); bitset
  precompute can be belled to a second codex in parallel — it is independent of the gate.
- Slice 2 → codex, after both gates; Claude owner reviews diff + re-runs the headline
  table before accepting.
- Reviews stay with the Claude owner; fix-don't-re-bell for findings.

## What was checked (audit trail)
- `labs/slush-demo/slice2/slice2_discharge_gfn.py:263-291` — train loop: 1
  trajectory/step, batch 1, default `--steps 12` (line 617); shared scalar `self.log_z`
  (line 76) in TB loss (line 287).
- `labs/slush-demo/slice2/slice2_discharge_gfn.py:258-260` — additive log-reward.
- `labs/slush-demo/slice2/slice2_discharge_gfn.py:66-95` — zero-init scorer ⇒ initial
  policy ≈ Boltzmann(rel+bonus).
- `labs/slush-demo/findings/slice2_recovery_findings.md`,
  `labs/slush-demo/findings/slice2_v2_recovery_findings.md` — the two honest nulls
  (steps 6–12, GFN ≈ untrained prior ≈ null).
- `labs/slush-demo/findings/slush_sweep_findings.md` — the 3000-step run that beat
  greedy on mode-spread across all 5 missions (the existence proof for training scale).

## Checkpoint — 2026-07-10 (claude-4)

**Slice 0 — DONE, both gates PASS.** Built `labs/slush-demo/slice2/gfn_core.py`
(pure-numpy tabular GFlowNet; no torch). Trajectory balance with **conditional
logZ(m)** and the standard **uniform backward policy P_B(parent)=1/|S|** for set
generation — the latter is the |S|!-orderings term the original slice-2 TB
dropped (a latent third defect, now surfaced). Trained deterministically
full-batch over the enumerable DAG; the terminal marginal is computed exactly by
DP (no Monte-Carlo noise in the gate).

- **G0a (trainer correctness):** conditional-logZ recovers the exact Gibbs
  sampler — max TV 0.0125 (gate 0.05), max |learned logZ − true logZ| 0.029 nat
  (gate 0.1) across 4 synthetic submodular-coverage missions.
- **G0b (F2 pinned):** the shared-scalar-logZ arm fails — max TV 0.986 vs the
  conditional arm's 0.012 — on missions whose true logZ spans 5.64 nats. The
  warp is largest on the low-Z missions and smallest on the high-Z one (the one
  scalar settles near the high-Z end), an interpretable signature of F2.
- Tests: `slice2/test_gfn_core.py` (9, green). Findings:
  `findings/slice0_trainer_findings.md`.
- **Design note vs the dispatch plan:** built numpy-tabular rather than reusing
  the torch `slice2/` harness *on purpose* — Slice 0 is a trainer-correctness
  test, so removing the function approximator makes a failed G0a diagnostic of
  the objective/logZ alone. The same core scales to Slice 2 (pool ≤ 240), which
  would let the whole line drop torch.

**Slice 1 — reward gate runs; anti-gaming PASS, discrimination INCONCLUSIVE.**
`futon3a/holes/labs/M-memes-arrows/aliveness_v3_gate.py` (graduated with
`rollout_execute.py`) now runs. The anti-`1=1` control PASSES (substantive
+0.414 > trivial −0.078 > bloated-shell −0.491 — the accuracy−λ·complexity
structure is sound). But discrimination against the shuffle null is **weak for
two independent reasons**, and this is robust to swapping in the canonical
1207-pattern `minilm_pattern_embeddings.json` (the gate's accuracy comes from
`rollout_execute` text move-chaining, not the embeddings):
  1. **Discharge accuracy is sparse** — median 0 want-coverage across records;
     most cascades score zero, so accuracy can't separate success from fail
     (accuracy AUC 0.750 == null-95 0.750). Matches the committed corpus-gate
     finding: semantic coverage is dense but is *relevance, not discharge*.
  2. **Ground truth is tiny/imbalanced** — 10 records, 8 success / 2 fail →
     AUC granularity 0.0625, null-95 sitting exactly on the observed value.
     Even a perfect reward could not demonstrate discrimination on this set.

**Slice 2 — correctly gated, NOT started.** Per the reward-before-generator /
G1-comp discipline ("if the reward has no teeth, STOP; do not run the sampler"),
training the GFN now would reproduce the v1/v2 null. `gfn_core.py` is ready to be
its trainer once the reward passes. **Prerequisites before Slice 2:** (a) a
discharge accuracy signal dense enough to have teeth without collapsing into
relevance; (b) a larger, better-balanced success/fail ground-truth set.

## Review verification — 2026-07-10 (Fable, reviewer per handoff protocol)

Checked the claude-4 checkpoint above; all claims verified.

- **Slice 0 tests:** re-ran `slice2/test_gfn_core.py` — 9 passed.
- **Slice 0 determinism + gate numbers:** re-ran `slice2/gfn_core.py --iters 700`
  (the findings config, seed 20260710) — exact reproduction: conditional max TV
  0.012509 / max logZ err 0.028944, shared-arm max TV 0.986547; G0a PASS, G0b
  PASS. Also passes at CLI defaults (800 iters: TV 0.0104, logZ err 0.0242).
- **Slice 1 gate:** re-ran `futon3a/holes/labs/M-memes-arrows/aliveness_v3_gate.py`.
  One environment fix was needed: this machine's futon3a checkout lacked the
  `resources/notions → ~/code/data/notions` symlink (per futon3a commit
  `6b6d9ad` / README-data.md); restored it. Gate then reproduces the checkpoint:
  10 records (8 success / 2 fail); accuracy AUC 0.750 == null-95; aliveness AUC
  0.875 == null-95; anti-`1=1` PASS with substantive +0.417 > trivial −0.078 >
  bloated-shell −0.350 (magnitudes shift slightly under the canonical
  1207-pattern embeddings vs the checkpoint's +0.414/−0.491; ordering and
  verdicts unchanged, consistent with the checkpoint's robustness claim).
- **Stale status corrected:** `labs/slush-demo/PLAN-gfn-development.md`'s Status
  said Slice 1 was BLOCKED on a missing `rollout_execute.py`; it exists on
  futon3a main (`8cf687e`, merged 2026-07-10) and the gate runs. Fixed in place.

**Standing verdict:** Slice 0 accepted. Slice 2 remains correctly gated — the
two prerequisites are reward science: (a) a *dense* discharge-accuracy signal
that stays discharge (not relevance); (b) a success/fail ground-truth set large
and balanced enough that discrimination is demonstrable even in principle
(10 records at 8/2 gives AUC granularity 0.0625 — no reward could pass).

## Checkpoint — 2026-07-10 (Fable: Slice 1.5 + Task B, direct)

**Slice 1.5 — accuracy limb rebuilt; validity mostly recovered; measure LOCKED.**
The v3 lexical limb's median-0 on known dischargers was a validity failure with
four miss classes (M1 missing-flexiarg / M2 morphology / M3 synonymy / M4
interface-underspecification). New `obligation_accuracy.py` + `aliveness_v3_gate2.py`
(futon3a lab): directional want-atom coverage, produces = THEN + conclusion
(`parse()["action"]`), tiers exact/stem/per-atom-MiniLM @ τ=0.60 a priori;
missing flexiargs = MISSING, never 0. Result: 6/7 measurable positives > 0
(was 4/7), per-atom witnesses auditable, anti-`1=1` still PASS, negatives held
at 0 under the THEN-only variant and gain one topical atom (`operator`) under
the locked variant — residuals R1 (interface-underspecified patterns: a library
authoring gap) and R2 (single-atom topical leakage ±0.1, expected to average
out at scale) documented in `labs/slush-demo/findings/slice1_5_validity_findings.md`.
Measure locked at n=9 precisely to stop variant-fishing.

**Task B — expansion attempted; mission grain proven INVALID; the real unlock
located.** The procedural expansion (wholeness labels × :applied, 83 records)
is dense at every tier (even strict T1: 82/83 nonzero) and discriminates at NO
tier (AUC 0.399–0.449 vs null-95 ≈ 0.63): grain mismatch, proven, not measure
failure. Valid ground truth is fold-grain only; current stock 16 closure-folds
records + 4 CH2 events. Bottleneck found: `meme.ch2`'s schema hardcodes
`:discharged? true` — failed folds are unrepresentable, violating closure-folds'
own recording discipline. Unlock = CH2 negative-event schema + live-loop
emission (operator decision; touches production code + gates_test), plus
optional retro-curation. `findings/ground_truth_expansion_findings.md`;
artifact `findings/ground_truth_mission_grain.json` kept as a
relevance/selection corpus.

**Net standing:** Slice 2 remains gated — now on exactly one thing: fold-grain
ground truth at n≈60+ (CH2 negatives + emission wiring). Measure design is done.

## Checkpoint — 2026-07-10 (Fable: fold-grain expansion, run-here pass)

Mined the 21 escrow deposits (`futon6/data/fold-turns/`) as fold-grain ground
truth: full census in `futon6/holes/fold-turn-adjudications.edn`. 19/21 are
plans whose outcomes were never realized (not ground truth — the census makes
the gap visible); 2 adjudicate with quoted evidence: ft-action-vocabulary-005
FAIL (P3 negative ×2, kill criterion) and ft-peradam-mechanization-006 PASS
(machinery landed per plan, refusal census correct). Merged loader
`fold_ground_truth.py`; gate2 now runs on n=12 (9/3).

**Decisive negative: the adjudicated FAILURE scores accuracy 0.45 — above 6 of
9 true positives.** Interface coverage measures plan–want vocabulary alignment,
which a well-authored doomed plan maximizes (witnesses: picks←pick,
targets←target). Text-side accuracy has a Goodhart ceiling that no measure
engineering clears — *works* vs *well-formed* is information the texts do not
contain. **Reward gate NOT PASSED; Slice 2 stays gated.** Route sharpened:
(1) labels from flown folds (CH2 wiring at the fold_escrow seam — schema
already landed, futon3a 32dcb09); (2) LEARN the reward from fold outcomes
(reliability posteriors, M-bayesian-structure-learning line) with Slice-1.5
coverage as a feature, not the reward; (3) coverage keeps the anti-gaming
controls (still PASS at n=12). Full analysis:
`labs/slush-demo/findings/fold_grain_expansion_findings.md`.

## Plan of record — 2026-07-10 (Joe ratified): Slice 2-live, the full loop

The reward-before-generator gating is retired for the live loop (it remains the
right rule for offline claims): the GFN goes on NOW as the loop's diverse
proposer at low β, with every flown proposal externally adjudicated. Full spec:
`labs/slush-demo/SPEC-full-loop-gfn.md` (loop diagram, learned reward R̂,
scoreboard S1–S4 with today's baselines, kill criterion, build list B1–B5,
mint lane for new patterns). Preregistered in `~/code/p4ng/main-2026.tex`
§Preregistration (dated 2026-07-10, S3 baseline AUC 0.542 @ n=12 vs null-95
0.854): the paper is the preregistration; the SPEC is its operational twin.

## Checkpoint — 2026-07-10 (Fable: B1–B3 BUILT AND RUNNING, local box)

The Slice-2-live loop's non-JVM organs are live. **B1** `reward_v0.py`
(futon3a lab): learned R̂ = reliability posteriors (Beta(2,1), flown-fold
outcomes only) + Slice-1.5 coverage feature + complexity; first preregistered
S3 point **0.630** LOO (null-95 0.870, n=12) — up from pure-coverage 0.542,
cosine-artifact negatives now rank bottom; hard anti-gaming checks PASS and sit
outside the learned weights. **B2** `slice2/gfn_live.py`: the slush proposes
live — per-mission top-24 cosine pool, memoized β·R̂ reward, gfn_core
conditional-logZ TB (3000 steps × batch 16, β=0.5); first blinded batch = 27
proposals over three OPEN missions (M-chipwitz-corps, M-learning-loop,
M-futonzero-generative), proposer identities sealed by proposal hash —
adjudicators read the blinded file only. **B3** `scoreboard.py`: S1–S4 one
command, append-only `findings/scoreboard-history.jsonl`; row 1: S1/S2
awaiting flown folds, S3 0.630 below null (the preregistered start), S4 = 8
modes/mission at mean Jaccard distance 0.88–0.90 vs incumbent's 1.

**Next:** fly proposals from the blinded batch (arming = Joe); B4 CH2 wiring at
the fold_escrow seam on JVM-window lift — S1/S2 light up from its stream; B5
mint hook. The loop is now waiting on folds, not on code.
