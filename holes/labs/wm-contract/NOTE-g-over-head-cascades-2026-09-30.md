# G over the eight mission-HEAD policy sets (2026-09-30)

S3c reported 81 rows in `mission-head-cascades-2026-09-30/`, but those are
only **33 structurally distinct policies**. A rejection fork can repeat the
same nodes and edges with a different annotation, and alternatives/overlap
modes can emit the same structure. `futon2.aif.cascade-shape-g` deduplicates
on mission, node sequence, and edges before scoring. All 33 returned
`:computed` and a finite
numeric `:g`; **cascades without G: 0**.

## What is modelled, and what is inherited

The adapter's arrangement rule is in
`src/futon2/aif/cascade_shape_g.clj`: each pattern produces its own target-
qualified done token; a directed edge makes its destination require the
source token; and an overlap edge gives both endpoints one shared token which
both advance. The terminal nodes supply the want. `pattern-theta` supplies a
learned probability when trials exist and records their identities and
targets (`learning_trial_ledger.clj:579-625`). Otherwise the adapter states a
Jeffreys Beta(1/2,1/2) prior. Those operators and that prior are claude-1's
modelling choice, delegated by Joe; they are not read from an attested
target-specific interpretation.

The existing scorer fixes the base rollout. It rolls the generated precedence through
the existing forward model, queries C at every tau, and sums the returned G
(`cascade_observation_scoring.clj:91-150`). Its certificate records the whole
step-indexed C schedule (`:126-145`). Risk and observation ambiguity are the
sums of the scorer's step terms. Expected parameter information is computed
with the existing Beta kernel (`parameter_novelty.clj:21-29`) and reported for
every policy. The current scorer does **not** consume that parameter term; the
adapter retains it as a reported term and adds only the fit ambiguity described
below to the scorer's risk + observation ambiguity. A later decision is
needed if Joe intends parameter information, rather than the scorer's existing
observation uncertainty, to enter the numeric G.

The fit amendment uses the evidence the committed analysis actually contains.
An accepted occurrence carries its exact fragment and rationale and receives
preliminary likelihood 0.9; a recorded rejection gets 0.1; a connector never
read against this HEAD gets 0.5, maximum Bernoulli uncertainty. Fragment
coverage is Jeffreys-smoothed. Their joint negative log likelihood is attached
as finite `:f`, `:f-status :computed`, and `:computed-f`.
This uses the existing F convention `F = -ln p` from
`cascade_free_energy.clj:90-106` and its shared surprisal function rather than
treating “unknown” as neutral zero.
`cascade_selection.clj:182-215` therefore reports the full
`sigma(log E - F - gamma*G)` law with no omitted F, using the existing carrier
defined at `policy.clj:228-254`.

Fit uncertainty enters G as `sum(H(p) + (1-p))`, added to the scorer's
ambiguity. Entropy makes an unread connector uncertain; `(1-p)` makes a poor
fit costly rather than confidently cheap. The likelihoods, coverage
smoothing, and this fit-ambiguity expression are claude-1's preliminary
modelling choices. The stored 象 artifacts do not carry numeric retriever
relevance for accepted refs, so every receipt records that term as
`:absent :numeric-scores-not-in-analysis-artifact`; no score is reconstructed
or invented.

The preference is present at every bounded horizon step. Before the terminal
step it prefers `:ending/not-yet-evaluated`; at the terminal step it prefers
the target's completion class. The generated B still contains each node's
progress token, but the existing class-emission observation route collapses
those tokens to a class before C. Thus this packet does not yet consume a
graded preference over individual progress tokens. The token observation
route cannot represent the largest policy here because it is capped at ten
tokens. This is a precise remaining gap against the requested “progress
tokens present” reading of C, rather than an invented claim that class C is
the same thing.

This is also not yet the recursive catamorphism in
`NOTE-g-as-fold-2026-09-30.md`. It compiles edges into guards, then lets the
existing list-based rollout fold the resulting transitions. The shape is
therefore observable to G, but shared substructure is still replayed through
a list.

Repeated citations are represented as **separate node occurrences**. An
occurrence is identified by pattern, fragment, and position; each occurrence
gets its own done token while all occurrences of a pattern read the same
pattern-level theta. This retains the reading's nine nodes for
M-self-documenting-stack, including five mission-scoping occurrences. Treating
them as one node would shorten the rollout, collapse relations from distinct
fragments, and change G; the first implementation did that silently, and this
correction removes it.

Rejected patterns remain available adjustments, but are not counted here:
the artifacts do not state whether a rejection should replace a particular
accepted occurrence or be added at a particular relation. Counting the
annotation fork as a policy was the original error. A later constructor must
materialise the actual swap/add structure before it can be scored.

## Results

The “initial” row is the first distinct fragment-derived arrangement in the
stable artifact order. Lower G is preferred.

“>1-node isolated” counts policies whose node set differs from every other
policy for that mission by more than one node (for a one-policy set this is
vacuously one).

| mission | reported / distinct | initial G / F | minimum G / F | minimizing adjustment | ΔG | interpretation owed | >1-node isolated |
|---|---:|---:|---:|---|---:|---|---:|
| M-象-2000 | 15 / 6 | 5.461505 / 1.592602 | 5.461505 / 1.592602 | alternatives-5 | 0.000000 | none | 1 |
| M-metric-harness | 4 / 1 | 6.558794 / 2.062834 | 6.558794 / 2.062834 | alternatives-1 | 0.000000 | none | 1 |
| M-distributed-proofreaders | 9 / 4 | 1.662500 / 1.260543 | 1.662500 / 1.260543 | alternatives-1 | 0.000000 | none | 1 |
| M-web-arxana-ui-improvements | 12 / 6 | 1.629056 / 0.798508 | 1.394432 / 0.903868 | overlap-1 | 0.234624 | none | 3 |
| M-self-documenting-stack | 4 / 4 | 5.117283 / 1.530166 | 4.518894 / 1.807736 | retraction-3 | 0.598389 | `social/explicit-exit-over-abandonment` | 4 |
| M-war-machine-aif-completion | 27 / 6 | 7.413731 / 2.503129 | 7.413731 / 2.503129 | alternatives-3 | 0.000000 | none | 3 |
| M-essays-diachronic-model | 5 / 2 | 1.394432 / 0.903868 | 1.394432 / 0.903868 | alternatives-1 | 0.000000 | none | 0 |
| M-value-creation-loop | 5 / 4 | 5.247531 / 0.996806 | 5.247531 / 0.996806 | alternatives-1 | 0.000000 | none | 4 |

The 33 distinct policies contain **0** same-pattern-sequence,
different-edge pairs, so the
corpus count is 0 pairs and 0 separated. That is not evidence of failure or
success for Q10. The test therefore constructs the exact missing comparison:
three patterns in the same firing order, once as a chain and once with only
an edge from the first to the last. Their G values are 3.074890 and 1.414694.
Replacing the structural compiler with precedence-only operators makes that
test fail because the candidates become identical.

`coordination/mandatory-psr`, the pattern used in grounded change `891001d`,
is in the pinned graph but is in none of M-self-documenting-stack's four
materialised policies. It is therefore **unranked**, rather than assigned a
made-up position. The HEAD reading did not select it and the three k=3
retractions did not introduce it. This is evidence that the adjustment set
did not contain the pattern the machine actually used.

The fit falsifier holds structure fixed. When both nodes have accepted
fragment evidence, that policy has lower F and G than the same two-node edge
with its connector never read against the circumstance. The latter records
that connector under `:interpretation-owed`; it is the concrete post-selection
象 request, rather than a pre-selection admission condition.

## Reproduction

`futon2.aif.cascade-shape-g-test` pins the 81 reported / 33 distinct counts,
finite G for every distinct
policy, the per-step preference schedule, overlap-token semantics, and the
same-order/different-edge falsifier. The scorer's own namespace remains the
independent regression gate.
