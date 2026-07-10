# SPEC — the full loop with the GFN proposer (Slice 2-live)

**Status:** PLAN OF RECORD (Joe ratified the reframe 2026-07-10: "turn the GFN on
knowing it will fail, feed it information until it succeeds, progress measurable").
**Author:** Fable, with Joe · **Date:** 2026-07-10
**Supersedes:** the reward-before-generator gating of Slice 2 (TN-gflownets-fable-review) —
that discipline remains correct for OFFLINE claims and is retired here only because every
GFN proposal in this loop receives EXTERNAL adjudication.
**Builds on:** E-close-the-loop (fold interface + live wiring 6d), the fold-turn escrow
(E-live-loop-2 2d), CH2 fold events (futon3a `32dcb09`), Slice-0 trusted trainer
(`slice2/gfn_core.py`), Slice-1.5 locked coverage measure, fold-turn-adjudications census.

## The loop (one revolution)

```
outer (WM)          inner (fold)                       learning
───────────         ────────────────────────────       ─────────────────────────
1 select mission ─► 2 offramp: have→want (the sorry)
                    3 PROPOSE  ◄────────────────────── 9 GFN policy retrained on R̂
                      GFN slush samples K diverse
                      cascades ∝ exp(β·R̂(S|m));
                      incumbent construct_cascade
                      runs alongside (blind A/B)
                    4 FOLD: cascade → construction
                      (classical / LLM / embedding —
                      any impl against the interface)
                    5 ESCROW: ft-*.edn deposit, pins, ΔG
                    6 ARM & FLY: operator arming;
                      act-gate :pass iff ΔF>0 ∧ ΔG<0
                    7 ADJUDICATE: external verdicts
                      (tests, gates, refusal census,
                      preregistered P3-style targets)
                    8 RECORD: CH2 fold event ─────────► 8' reliability posteriors
                      :discharged? true/false             update (cascade_learn β);
                      + :used + evidence                  R̂ retrains
                    3' MINT LANE (niche construction):
                      policy-holes with no grounding
                      pattern → candidate-pattern/*
                      :open → enters the action space
                      at the uniform prior
```

Steps 1–2 and 4–6 are the loop that already runs (live wiring 6d, scheduled
one-shot JVM). Steps 3, 7–9, and 3' are what this spec adds or makes load-bearing.

## The learned reward R̂ (not engineered — learned)

R̂(S|m) combines, with weights fit ONLY on flown-fold outcomes:
- **reliability posteriors** over patterns and co-applications (cascade_learn's
  α/β — `:success false` already moves β), entering at the uniform prior for
  newly minted patterns (the entity-belief carry rule);
- **coverage feature** — the locked Slice-1.5 obligation-unification measure
  (a FEATURE, demoted from reward: the fold-grain expansion proved a flown-and-
  killed cascade can out-cover 6/9 true dischargers — Goodhart ceiling);
- **complexity prior** — base-rate −log inclusion (unchanged).

Hard checks kept regardless of learned weights: anti-`1=1` and bloated-shell
controls must score dead (they pass at n=12 today); if a retrain breaks them,
the retrain is rejected.

## Scoreboard — preregistered progress metrics (all outcome-grounded)

| # | metric | definition | direction / target |
|---|---|---|---|
| S1 | discharge rate | flown GFN-proposed folds adjudicated `:discharged? true` / flown, per window | climbs; report weekly |
| S2 | blind A/B vs incumbent | same missions, proposer identity hidden from the adjudicator; discharge rate + operator-accept rate | GFN ≥ incumbent by the time n(flown)≈30 |
| S3 | discrimination curve | AUC(R̂ ranks success>fail) on ALL accumulated flown labels vs 200-shuffle null, recomputed per retrain | must climb above null-95 and stay; today's baseline: 0.542 at n=12 |
| S4 | diversity | distinct above-R̂-threshold modes per mission | GFN > greedy incumbent (the slush's proven property: 338–396 vs 1) |

**Kill criterion (M-action-vocabulary discipline):** if after two R̂ revisions
with n(flown) ≥ 40 S3 still does not clear its null, STOP — the finding routes
to the reward-structure question (operator-interest signal / Joe-HUD thread),
not to a third revision. **No tuning on adjudicated labels already counted in a
reported S3 point** (each retrain reports on held-out-in-time labels).

**Label-rate reality (the binding constraint):** 19/21 deposits were never
flown; labels accrued at ~2/5 days. The loop learns at the speed of
adjudication ⇒ prefer many SMALL folds with cheap external adjudicators
(tests, gates, refusal censuses) over mission-scale ones. Target: ≥5 flown
folds/week once CH2 wiring lands.

## The mint lane (new patterns — R17/A4a arriving on schedule)

Deposits already carry `:policy-holes` (boxes no library pattern grounds; ft-005
even carries an explicit AUTHORING RECOMMENDATION). Route: policy-hole →
`candidate-pattern/*` minted `:open` in the meme store (a new pattern is itself
a hole with a contract — the sorry/BHK lane) → flown under escrow like any
pattern → earns library status through fold outcomes, starting at the uniform
prior. Worked precedent, end-to-end: `structure/two-projections-of-one-quantity`
(cited-before-existing → caught → minted WM pilot Turn 3 → discharged its hole →
GROUND record `wm-flight/turn-4`). For the GFN the action space simply grows:
the policy is feature-based, new patterns enter prior-only.

## Build list

Non-JVM (can start now, this box):
- B1. R̂ v0: reliability-posterior + coverage-feature + complexity scorer over
  the existing 12 labels (fit = trivial at this n; the point is the plumbing
  and the S3 baseline harness).
- B2. Slice-2-live GFN: `gfn_core.py` conditional-logZ trainer over the real
  pattern space, sampling ∝ exp(β·R̂), K proposals/mission, LOW β; A/B harness
  vs `construct_cascade` (proposer identity recorded but blinded in the
  adjudication view).
- B3. Scoreboard harness: S1–S4 computed from CH2 events + adjudications file;
  one command, one table, append-only history.

JVM-window (first task when the quiet period lifts):
- B4. CH2 emission wiring at the `fold_escrow` verdict seam (schema landed
  `32dcb09`) — every pass AND reject verdict emits a fold event with the
  deposit's patterns as `:used`. This is the load-bearing piece: S1–S3 all
  read from its stream.
- B5. Mint-lane hook: policy-holes from new deposits surfaced as
  candidate-pattern stubs (authoring stays with agents/operator; the hook just
  stops them evaporating).

Operator-gated (unchanged):
- Arming stays with Joe (per-fold or blanket-interactive, the ft-005
  precedent); WM-I4 consent gate untouched; adjudicators are external by
  construction (no self-certification).

## Preregistration

`~/code/p4ng/main-2026.tex` is revised alongside this spec to state the loop,
R̂, S1–S4, and the kill criterion BEFORE the system runs — the paper is the
preregistration; this file is its operational twin. Any change to S1–S4
definitions after first flown-fold data = a new preregistration, noted as such.
