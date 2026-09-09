# FoldC representation sitting — 2026-09-09

Joe to codex-17, emacs-repl, verbatim:

> So the separation makes sense for sure. There are different things. I think that... We had discussed using Markov categories for everything to do with. Probability and Prediction. So this sounds like an interesting place to possibly bring in a Markov category. Representation. But in terms of your recommendation, yes, we should keep them separate. In order to do the calculation, I think that's basically part of the definition, and I'll leave it to you to sort out exactly the implementation.

## Ruling and implementation interpretation

Preference distributions and scalar KL risk remain separate. Joe delegates the
implementation of the calculation. His suggestion to consider Markov categories
is recorded as a direction to investigate; it does not assert an existing
category-level integration or close the observation-model bridge.

Use three typed stages: a prediction kernel Q from policies to observations;
a conditional K from observations to dispositions; and comparison of the
resulting disposition distribution with a separately supplied preference C.
The final KL is a scalar risk contribution. Do not insert that scalar into
PreferenceLayer.prefers, infer an arbitrary base value, or treat a risk sum as a
normalised preference distribution.

For RUN4's explicit constant adapter (futon2 02b317f5), K ignores its observation
input. Finite kernel composition then returns the same disposition distribution
for every normalised upstream Q. This is the discard/constant factorisation
already discussed in holes/problems/P-markov-category-spec.md §0; it is useful
here precisely because it states what the adapter cannot discriminate. A local
finite-kernel theorem does not claim an implementation of Mathlib's Stoch
category. The existing PreferenceLadderDraft.dispositionPredictiveMass supplies
the finite marginalisation expression; LocalPreferenceModule supplies exact
preference-table embedding. Neither licenses an observation-domain substitution.

Grounded-change-only prediction against seed mass 1/2 gives ln 2. Its source is
the measured checkpoint-conditioned cohort and the explicit constant-adapter
assumption, not a learned channel model. E-C-realization.md §1b remains open;
positive predicted mass at an explicit preferred zero must refuse, never clamp.

The find-marker draft remains parked until the FoldC witness/checker/receipt
reconciliation reflects this separation. No registry or worklist is changed
by this session record. Further rulings will be appended verbatim.

## Follow-up: reconciliation or a new era

Joe to codex-17, emacs-repl, verbatim:

> Okay, my opinion is if you can reconcile the Old Witness and Receipt with this separation, great. Otherwise, we could mark it as a new era and just point out that they're not directly comparable. But it's worth looking at least at an analogy or discussion of how they relate to each other, even if we can't. Recompute them and get a new certificate with a new system directly. So let's sort that out.

Implementation finding: retain the old ordered-fold certificate under its actual
claim and distinguish the probability/risk era. The arithmetic is not a
recomputation of the same quantity. See
[FOLDC-era-comparison-2026-09-09.md](runs/FOLDC-era-comparison-2026-09-09.md)
for the checked correspondence, limits, and remaining certification work.
