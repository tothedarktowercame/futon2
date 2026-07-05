# M-fold-self-play — close R2: the deposits become the curriculum

**Status: IDENTIFY (2026-07-05, operator-directed). Owner: TBD (operator
assigns). Ratification: Joe. Lineage: futonzero-alphazero.md §1/§5
(R2 = the defining gap), E-live-loop-2 (the supervised deposits),
M-differentiable-substrate (R2 = distill from visit-counts).**

## HEAD (operator, 2026-07-05, verbatim in substance)

"With self-play, there's gotta be a way to make this work — our
supervised runs with claude-20 and zai-10 are heading in that
direction. We also created a Calculemus AIF+/AIF² protocol for
evaluating AIF life-forms through structured argument — a clear example
of a contest form, though we're not applying it here. And beyond that,
futon5 is full of other evolutionary computing ideas we could get
inspiration from."

## IDENTIFY — the tension

FutonZero's own honest caveat: the prior is not trained from outcomes;
v1 is AlphaTensor-shaped, a single-agent MDP. But as of 2026-07-05 the
missing ingredients exist AS SUPERVISED PRACTICE:

- **Games are being played and recorded.** Fold-turn deposits
  (ft-*.edn) are complete game records: position (sorry-grain ψ),
  policy (cascade), move (wiring), value (ΔG), and — uniquely — a
  BLIND SCORE against sealed ground truth (the A-next gold corpus).
  Two exist; the pipeline for more is armed under blanket consent.
- **The contest form exists and has run once.** The Calculemus bout
  (futon6/holes/missions/E-mission-head.md §"Calculemus": AIF
  life-forms contending through structured argument; one real bout,
  verdict `indeterminate`) is a working adversarial-evaluation
  protocol — currently unapplied to fold-turns.
- **The selection machinery has prior art.** futon5's evolutionary
  material (docs/meta_patterns.md mutation/selection intensity;
  M-differentiable-code / M-categorical-code) + the arguing-worlds and
  cascade-sampler harnesses (measured negatives, but the HARNESSES
  stand and the yardstick caveats are recorded).

The self-play reframe this mission tests: **the adversary doesn't have
to be a second player on a board — it can be the sealed corpus (the
exogenous anchor) + Calculemus bouts between competing fold-turns for
the same sorry (the endogenous curriculum).** Two agents (e.g. a
claude and a zai, per the supervised runs) author competing deposits
for one sorry; a bout adjudicates; the loser's miss-classes and the
winner's structure feed the recipe/prior. That is prior→search→score→
train with every step already demonstrated separately.

## Completion criteria

- P1: a curriculum format — deposit pairs + bout verdict + blind score
  — specified and instantiated on ≥3 sorries from the gold corpus.
- P2: ONE measurable prior update from outcomes: any of (a) ψ-recipe
  revision adopted from a systematic miss-class (the v2 :hungry-for
  upgrade is retroactively the first instance — document it as such);
  (b) pattern-prior/phylogeny weights adjusted from win/loss evidence;
  (c) constructor parameter (α, ε, λ) re-fit — each with a before/after
  measurement on held-out sorries.
- P3: the Calculemus protocol applied to at least one fold-turn pair,
  bout record filed, verdict consumed by P2's update.
- E1: an honest lineage verdict — after P1-P3, is this self-play or
  curriculum learning? Update futonzero-alphazero.md §1 accordingly.

## Scope out (hard)

- No neural training runs; the "prior" being trained is the recipe +
  phylogeny weights + constructor constants (the futon-native prior).
- Enactment stays gated (WM-I4); this mission trains the PROPOSER.
- The peradam certification question belongs to
  M-peradam-mechanization; here the sealed corpus is the anchor.

## Kill criteria

- Two full curriculum cycles with NO measurable prior update candidate
  → the deposits are not informative enough at this grain; stop and
  report what additional signal a deposit must carry.
- Bout verdicts persistently `indeterminate` (as the first Calculemus
  bout was) → the protocol needs its own mission before this one can
  proceed; park with the finding.
