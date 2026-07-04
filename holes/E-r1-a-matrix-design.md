# E-r1-a-matrix-design — the explicit observation model A for R1 belief

**Date:** 2026-07-04 · **Parcel:** M-aif-faithfulness §2.3 B-3a (author claude-7,
reviewer claude-12) · **Status:** v0 BUILT DARK (`:likelihood-mode`, default
`:legacy` byte-identical; flip is the operator's, arena-*-mode idiom)
**Code:** `src/futon2/aif/belief.clj` (event block: `a-matrix-v0`,
`likelihood-vector`, opts-arity `update-entity-belief`/`update-belief`/
`update-belief-batch`; channel block: `channel-emission-matrix`)
**Tests:** `test/futon2/aif/belief_test.clj` (witness · reduction theorem ·
well-formedness · contradiction expressiveness · commutation · channel
consistency)
**Audit context:** E-r18-faithfulness-audit #3 — `belief-update` badged
`:principled-approximation`: "exact categorical Bayes structure with a hand-set
(1+w) likelihood; no A-matrix." This note is the design deliverable the parcel
required; re-badging is the reviewer's step, not claimed here.

## 1. Where does A come from?

**v0: A is hand-set, and says so.** The parcel scoped learned-A out. But
"hand-set" now means something different from before: previously the likelihood
was an *implicit* modelling commitment buried in an arithmetic shortcut; now it
is a *declared* observation model — a first-class data structure whose every
entry can be pointed at, argued with, and eventually learned (R7/R12 are the
natural learners; the outer loop already takes up hyperparameters daily).

The state space is the M-INC per-entity status set (7 statuses); the
observations come in two kinds, giving A two blocks:

- **Event block (7×7)** — `a-matrix-v0`. Rows: observed `state/*` event types.
  Columns: true statuses. This is the block the belief *update* uses; it is
  where the (1+w) shortcut lived.
- **Channel block (8×7)** — `channel-emission-matrix`. Rows: the observation
  channels that have likelihood models (`channels-with-likelihood`, 8 of the
  13–14 schema channels — the other channels have *no* emission model, which is
  now stated rather than hidden; callers fall back to preference-gap scoring).
  Columns: statuses. This block already existed *scattered* — it is exactly the
  arithmetic the `predict-*` fns compute their means from
  (`annotation-health-status-weights` + the open/healthy/nondormant mass
  helpers). v0 names it in one place, consistency-tested against the live
  helpers, WITHOUT rewiring them (prediction stays byte-identical; rewiring is
  a follow-on behind its own gate).

## 2. What does each entry mean?

An event-block entry `A[o][s]` is a **likelihood ratio**: how much more (or
less) likely we are to observe an event of type `o` when the entity's true
status is `s`, against an uninformative baseline of 1.0. Scale-free —
normalisation kills constants, so only ratios matter. Three entry classes:

- **Diagonal = 2.0** (`a-matrix-default-gain`): observing an event of type `s`
  is the strongest evidence for status `s`. The value 2.0 is chosen to equal
  the legacy gain at w=1, which is what makes the reduction in §3 exact.
- **Lifecycle-adjacent = 1.3** (5 entries): an event is mildly compatible with
  a neighbouring status, encoding belief-label lag. E.g.
  `A[:refined][:spawned] = 1.3` — refinement events routinely arrive while
  belief still says spawned. Full list with one-line justifications in the
  `a-matrix-overrides` docstring.
- **Contradictory = 0.7** (4 entries): an event is mild evidence *against* a
  semantically opposed closure — `:strengthened ⟂ :falsified` and
  `:addressed ⟂ :foreclosed`, both directions. **This is the expressive power
  the legacy form structurally lacks**: a diagonal-only likelihood can never
  say "this observation counts against that status," only "for this one."
  (Witness test: observing `:strengthened` now lowers `:falsified`'s posterior
  mass relative to the legacy update.)

The event weight `w` enters as a **precision exponent** (tempered likelihood),
not as a matrix entry: `L(s) = A[o][s]^κ(w)` with `κ(w) = log₂(1+w)`. So w=0 is
a no-op (κ=0), w=1 applies the A row as declared (κ=1), and heavier events
sharpen the whole row. Weight = observation confidence; A = observation
*structure*. Keeping them orthogonal is what lets A eventually be learned while
weights stay caller-supplied.

## 3. How does (1+w) fall out as a special case?

Exactly, not approximately. The legacy update is

    p'(s) ∝ p(s) · (1+w)^δ(o,s)

and since `(1+w)^δ = (2^δ)^{log₂(1+w)}`, this is precisely

    p'(s) ∝ p(s) · A₀[o][s]^κ(w),   A₀[o][s] = 2^δ(o,s),  κ(w) = log₂(1+w)

i.e. the a-matrix update with the identity-structured matrix
(`a-matrix-identity`: diagonal 2, off-diagonal 1). The legacy path was never
"no observation model" — it was the implicit assertion that *every event type
perfectly discriminates its own status and is silent about all others*. The
test `a-matrix-legacy-reduction-theorem-test` checks the equivalence
numerically (≤1e-12) across posterior shapes, weights, and event types; the
separate witness test checks the *default* path is the untouched legacy code.
Commutativity of batch updates (the docstring contract on
`update-belief-batch`) survives in both modes, since both are products of
per-event likelihoods under renormalisation — also tested.

## 4. What does R3's "gm: A" label now honestly claim?

Before: "A — likelihood P(o|s)" pointed at code where no object called A
existed; the likelihood was an arithmetic idiom (event block) plus scattered
per-channel weight tables (channel block).

Now, honestly: **"A exists as a declared, hand-set observation model in two
named blocks — `a-matrix-v0` (events; drives belief updates when the dark flag
flips) and `channel-emission-matrix` (channels; the named source of truth the
live predictors are consistency-tested against, not yet their executing
path)."** What the label may NOT yet claim: that A is learned, that it was
fitted to observed event↔status co-occurrence, or that the channel block is
load-bearing at runtime. Badge implication (reviewer's call): the update
retains `:principled-approximation` with the *gap moved* — from "no A-matrix"
to "A hand-set, not learned" — which is one named repair (learn A from the
trace corpus; R12's outer loop is sitting right there) from
`:derived-from-FEP` under the declared discrete-categorical model.

## 5. Flip considerations (for the operator, when the time comes)

- Default `:legacy` is byte-identical — no behaviour change has occurred.
- Flipping to `:a-matrix` with `a-matrix-v0` changes posteriors only via the 9
  off-diagonal overrides (adjacency 1.3, contradiction 0.7); with
  `a-matrix-identity` it changes nothing (the reduction theorem).
- The production caller is `forward_model.clj` (`update-belief-batch` in
  `predict`) — the flip site is one opts map there, after B-0a's provenance
  stamp is in the trace so before/after is attributable.
- E6-style shadow evidence (does the flip re-rank any live decision?) should be
  gathered the same way the :risk-mode flip's was, before deciding.
