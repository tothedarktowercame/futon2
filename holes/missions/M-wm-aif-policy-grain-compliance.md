# Mission: War Machine AIF policy-grain compliance

**Date:** 2026-07-23  
**Status:** INSTANTIATE — Slices 0, 1a, and 2 kernel dark; persistence/live seams remain open  
**Owner:** Joe + Codex  
**Cross-references:** `M-aif-faithfulness`, `M-wm-policies`,
`M-G-over-cascades`, `M-wm-strategic-mission-selection`, p4ng
`main-2026.tex` R5/R6/R12/R13/R15/R16/R17

## Problem

The War Machine is the reference implementation for the paper, but its live
habit prior and its paper-level policy do not currently have the same grain.
The scheduler selects an action first, from categories
`[type, target-or-target-class]`. Only after selection does the production
runner construct one pattern cascade. The tactical cascade lane therefore has
neither a same-circumstance candidate set nor an `E(pi)` input.

The audit found a second grain mismatch at the constructor boundary. When the
coverage-saturated cascade is longer than the live budget,
`cascade_serve.py` truncates `:shown` and recomputes its semilattice, but emits
the full untruncated cascade's `:cascade-score`, `:coverage-reward`,
`:prior-cost`, and wholeness. Thus the score may not describe the policy that
is folded and enacted. Slice 1 must expose prefix-local scores (additively at
first); live selection cannot be compliant while policy identity and policy
score refer to different objects.

The present scheduler prior is real and algebraically well seated:

```
Q(a) proportional-to exp(ln E_scheduler(a) - G_scheduler(a)/tau)
```

It is not, however, the paper's complete cascade policy `pi`. Renaming it would
hide the mismatch. Compliance requires an explicit hierarchy:

```
choose strategic scheduler action a
  using E_scheduler(a), G_scheduler(a)

construct admissible Pi_cascade(a) with at least two diverse candidates

choose tactical cascade pi in Pi_cascade(a)
  using E_cascade(pi | a), S_cascade(pi | a)

fold -> gate -> enact -> observe
```

The strategic line is not merely a preliminary scalar chooser. Its candidate
landscape and factor semantics are specified separately in
`M-wm-strategic-mission-selection`: p4ng control patterns organize typed
mission relations, attached memories supply episode-level warrants, and a
separate `E_scheduler` records habit without absorbing strategy, centrality, or
feasibility.

`S_cascade` is used above deliberately. The live cascade score remains an
engineering coverage/complexity quantity, not canonical expected free energy.
The prior can occupy its canonical algebraic seat without upgrading that score's
faithfulness badge.

## Invariants

1. Strategic and tactical menus never mix. `E_cascade` normalizes only over
   admissible cascades for one already-selected mission.
2. A complete policy identity includes mission, ordered pattern construction,
   and semilattice wiring. Equal pattern bags with different wiring are not the
   same policy.
3. Pattern retrieval, GFlowNet proposal reward, pattern reliability, cascade
   engineering score, and habit `E_cascade` remain separately named fields.
4. Habit records selection frequency, not goodness. Outcome/reliability learning
   remains a different return channel.
5. No singleton candidate menu may be described as cascade selection. It is
   construction only.
6. No new path may bypass fold, act-gate, consent, or no-self-certification.
7. Missing or mixed-grain identity fails closed. Feature-off production remains
   byte-identical until an operator flip.

## Slice 0 — complete-policy identity and prior (built dark)

`src/futon2/aif/cascade_prior.clj` now provides a pure symmetric-Dirichlet
posterior predictive over complete cascade identities. It attaches
`:cascade-prior-bias` and `:policy-grain :pattern-cascade`; it performs no live
selection or persistence. Reduction tests pin cold-start uniformity, frequency
learning, duplicate-mass conservation, topology-sensitive identity, and
same-mission fail-closure.

This slice is necessary but intentionally insufficient. Wiring a prior to the
current singleton constructor would manufacture formal-looking telemetry with
no choice behind it.

## Slice 1 — candidate-set contract (1a built dark)

Adapt the already validated diversity suppliers (`arguing-worlds` and the
offline slush read point) into a producer of concrete pattern cascades, not only
move buildouts or constellation sets. For one selected mission it must emit:

```
{:mission ...
 :shown [pattern-id ...]
 :semilattice {:descent [...] :co_app [...]}
 :candidate-source ...
 :inclusion-reasons [...]
 :exclusion-reasons [...]}
```

Acceptance:

- at least two stable, foldable candidates for each admitted shadow case;
- diversity is measured over both pattern membership and wiring;
- candidate generation is deterministic under a recorded seed/deposit;
- zero or one admissible candidate is reported as `:no-policy-choice`, never
  padded or duplicated;
- the current production constructor remains the incumbent candidate, so the
  comparison can reveal rather than assume improvement;
- every candidate's score components are recomputed over exactly its own
  `:shown` and `:semilattice`; full-construction telemetry is kept under
  separately named keys and never used as the candidate's score.

Slice 1a adds the read-only `cascade-policy-menu-for` frontier. It varies the
constructor's coverage-saturation threshold around the incumbent value and
admits only complete, untruncated, distinct policies at the constructor's pool
ceiling. This provides a concrete same-mission menu without changing the live
lane. It is an initial diversity source, not the final one: Slice 1b must compare
its support with arguing-worlds/slush candidates and retain the stated pattern
and wiring diversity tests.

## Slice 2 — dark hierarchical selection (pure kernel built)

For each shadow case, attach `ln E_cascade` to the same-mission menu and use the
existing unscaled-prior softmax algebra against a separately named cascade
score. Record every term, normalization support, identity, and counterfactual
winner with the prior disabled. Do not write the production habit state.

Required reduction tests:

- uniform `E_cascade` reproduces score-only ordering;
- changing only volatile prose or telemetry leaves identity and prior fixed;
- changing semilattice topology changes identity;
- a mixed-mission menu fails closed;
- a flat cascade score exposes habit governance rather than attributing the
  choice to value;
- feature-off scheduled judgement, construction, and trace remain identical.

`cascade-prior/shadow-rank` now applies the existing unscaled-prior softmax
kernel to a non-degenerate complete-policy menu. Because `:cascade-score` is
higher-is-better, it enters through the lower-is-better cost
`-cascade-score`; the trace vocabulary keeps both names explicit. The pure
result reports the score-only winner, prior-conditioned shadow winner, and
which term governed. It is not called by the scheduled runner and writes no
state.

## Slice 3 — persistence and return event

Add a distinct `:cascade-prior-state`; never reuse `:habit-prior-state`. The
trace must pin the full selected cascade identity after successful construction.
The exact event that increments habit must be preregistered. Initial
recommendation: increment on a selected cascade that passes structural
construction validation, matching the scheduler precedent; record fold,
enactment, and adjudicated outcome separately for reliability learning. A failed
constructor must not reinforce the prior.

Historical traces lacking complete cascade identity are not silently projected
into the new state. Cold start is honest unless a separately reviewed migration
can reconstruct identity from pinned deposits.

## Slice 4 — operator-gated live hierarchy

Flip only after a shadow corpus shows:

- non-degenerate candidate support on a useful fraction of eligible ticks;
- no invariant bypass and no increase in construction failures;
- decision explanations correctly distinguish `E_scheduler`, cascade score,
  and `E_cascade`;
- replay determinism from the trace/deposit;
- baseline and schema documentation updated in the same parcel.

The operator decision is whether to arm tactical selection, not whether to call
the current singleton cascade a policy distribution.

## Relation to R17''' pattern genesis

This mission selects over `Pi_H(U)` and therefore assumes a control vocabulary
`U`. R17''' is the adjoining operation that proposes `U' = U union {u*}` and a
forward-model contract for `u*`. New patterns enter candidate generation with
uniform reliability/proposal priors, but they do not inherit fabricated
historical mass in `E_cascade`. Once cascades containing them are genuinely
selected, the ordinary cascade-habit update applies.
