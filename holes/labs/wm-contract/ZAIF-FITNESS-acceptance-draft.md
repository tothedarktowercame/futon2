# Zaif-side acceptance checks for cascade-policy G — draft acceptance section (zai-7, for the joint review with zai-8, 2026-09-15)

Companion to SPEC-cascade-policy-semantics-2026-09-15.md and codex-26's
paired-constraint work (futon2 36f6c5e2, 9db538cd). These are the
ZAIF-HARNESS fitness checks: they run at board grain (authored panels,
the controller's select-not-edit discipline, fuel budgets), alongside
the mathematical controls in spec §3 — they do not replace them.
Corrections (e'), (f') from codex-26 accepted; both objections sound.

Where Joe's decision is load-bearing it is marked [JOE: ...] rather
than presumed.

## The checks

(a) **Two-board attention pricing.** Construct two boards over the
same target differing only in wiring: ask-first vs act-first. Price
operator attention high in C. PASS: the cascade-G posterior selects
the ask-first board when attention is expensive, act-first when cheap
— the authored panel semantics and the selector agree. FAIL modes:
selector indifferent to C (C not wired), or picks act-first under
expensive attention (G not seeing the ask path's risk).

(b) **Singleton.** A one-chip board scores exactly the one-step G
(mirror of spec §3.8 at board grain). PASS: board-G = chip-G. Guards
against panel-level bookkeeping drift.

(c) **Guard-source divergence.** Two boards identical except one
guard reads records (observed facts), the other reads operator-C
belief (hidden state — EXTRAPOLATION 3). PASS: the divergence between
their scores is nonzero, attributable, and *reported as the
extrapolation's measured cost*. If divergence is always zero the
extrapolation is silently free and the spec should say so; if it is
large and unattributed, the model is smuggling hidden-state claims.

(d) **Alexander scale-down.** [JOE: A/B pending — under A this is
both risk and ambiguity; under B risk only] Compose two
well-specified, aligned patterns. PASS: the composite's RISK does not
exceed either alone (codex-26's zero composite risk for aligned
composition). Under ruling A additionally: composite ambiguity does
not exceed the sharper pattern's justified model's.

(e') **Worsening-interaction control** (corrected from the false
universal): a CONSTRUCTED model where composition demonstrably
worsens the score — two patterns whose individual scores are
admissible but whose composition's intermediate states produce
predictive mass where C is zero (risk inadmissibility) or sharp
ambiguity loss. PASS: cascade-grain G scores the composite worse than
either alone in this concrete model. This is composition sensitivity
at fitness grain; same evidentiary standard as spec §3.6 — no
universal inequality claimed.

(f') **Justified-model attestation ordering** (corrected): identical
models and preferences give identical G — paperwork alone never moves
the score. The check: an ill-attested pattern must yield either a
vaguer JUSTIFIED model (flatter A or B^p) or admission refusal, and
THAT vagueness must measurably change G versus the well-attested
twin. Specification quality is a difference in the model you are
entitled to claim, not a label. PASS: ill-attested → refusal or
measurably-worse G via the vaguer justified model only.

(g) **Fuel-vs-G length division.** [JOE: PENDING — full Alexander (A:
ambiguity also derives from pattern quality, no length effects in G)
vs Alexander-in-risk-only (B: common-T comparison discipline).] The
zaif-side design requirement either way: G is length-neutral by
ruling; LENGTH IS PRICED BY FUEL — the board's declared fuel budget
and per-chip decrement are the designed governors (ChipWits panel
cost), with exhaustion as the typed terminal. Under B additionally,
cross-board comparison uses common-T. Fitness requirement on the
selector either way: selection must not be G-blind to fuel — an inert
board (identity transitions, ~ambiguity-only cost under A) must not
win selection merely by being long. Codex-26's FUEL selector
extension is the pending mechanism.

## Open items this section does not settle

- Mid-run board-transition modeling (A5 hierarchical step vs policy
  re-selection) — [JOE: pending].
- Yield pricing in C ("stopped early, handed back" as an outcome) —
  [JOE: pending]; interacts with (g): without yield outcomes, G is
  blind to hand-back timing.
- Π is the authored subset (boards the Workshop mints); confirmed in
  direction by the allow-list widening; pin explicitly in the spec.
