# CLEANUP C7/C8 — belief update (2026-08-31)

Automatability score: **7/7**. Both carrier ports and the posterior relation are
typed; the positive fixture and inert-update mutation are executable; the
contract and holder registry consume the result; reads are pinned by the two
repository commits; the change is additive/reversible; the paper fixes the only
decision encountered, `μ ← μ + α Π ε`.

`BeliefState` is now a channel-indexed mean plus nonnegative variance.
`observationKernel` is a finite-support kernel whose row masses are actually
summed and proved equal to one. This corrects a latent defect in the pre-existing
`ProbabilityKernel`: its old `normalised` field merely asserted that *some real
number* equalled one and never mentioned `mass`.

`beliefUpdate` is a relation between learning rate, kernel, prior, observation,
precision, and posterior. It composes the closed `predictionError` and
`variationalFreeEnergy` declarations. A valid posterior must apply the paper's
precision-weighted correction, retain the variance carrier, and not increase
the declared present-tense mismatch. The kernel row mass occurs in the
correction; normalization makes it one, without making the observation model an
unused ceremonial argument.

The executable fixture proves both directions needed by the gate: a corrected
posterior passes, while the shape-correct unchanged prior is not a belief update.
The C16 runner mutates the latter theorem into the false positive assertion and
requires Lean to reject it (`0=pass`, `1=ordinary failure`, `2=mutation slipped`).

C8 needs no licensing threshold or declaration. Evidence enters as observation;
its consequence is the posterior relation above. Expected information gain and
Bayesian-model-reduction free-energy change remain intentionally disconnected.
