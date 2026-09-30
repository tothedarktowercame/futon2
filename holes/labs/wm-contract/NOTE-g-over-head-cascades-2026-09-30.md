# G over the eight mission-HEAD policy sets (2026-09-30)

`futon2.aif.cascade-shape-g` scores the 81 policies materialised in
`mission-head-cascades-2026-09-30/`. All 81 returned `:computed` and a finite
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

The existing scorer fixes the rest. It rolls the generated precedence through
the existing forward model, queries C at every tau, and sums the returned G
(`cascade_observation_scoring.clj:91-150`). Its certificate records the whole
step-indexed C schedule (`:126-145`). Risk and observation ambiguity are the
sums of the scorer's step terms. Expected parameter information is computed
with the existing Beta kernel (`parameter_novelty.clj:21-29`) and reported for
every policy. The current scorer does **not** consume that parameter term; its
G remains the scorer's risk + observation ambiguity. This preserves the
existing EFE instead of silently creating a second score. A later decision is
needed if Joe intends parameter information, rather than the scorer's existing
observation uncertainty, to enter the numeric G.

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

## Results

The “initial” row is the first distinct fragment-derived arrangement in the
stable artifact order. Lower G is preferred.

| mission | policies | initial G | minimum G | minimizing adjustment | ΔG |
|---|---:|---:|---:|---|---:|
| M-象-2000 | 15 | 4.426403 | 1.037486 | retraction-3 | 3.388917 |
| M-metric-harness | 4 | 4.637124 | 4.637124 | alternatives-4 | 0.000000 |
| M-distributed-proofreaders | 9 | 0.387251 | 0.387251 | alternatives-6 | 0.000000 |
| M-web-arxana-ui-improvements | 12 | 1.203973 | 0.544266 | overlap-3 | 0.659707 |
| M-self-documenting-stack | 4 | 4.110322 | 1.625415 | retraction-3 | 2.484907 |
| M-war-machine-aif-completion | 27 | 4.531764 | 3.562360 | retraction-3 | 0.969404 |
| M-essays-diachronic-model | 5 | 0.544266 | 0.544266 | alternatives-4 | 0.000000 |
| M-value-creation-loop | 5 | 3.899600 | 2.955834 | retraction-2 | 0.943766 |

The 81 policies contain **0** same-pattern, different-edge pairs, so the
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

## Reproduction

`futon2.aif.cascade-shape-g-test` pins the 81-policy count, finite G for every
policy, the per-step preference schedule, overlap-token semantics, and the
same-order/different-edge falsifier. The scorer's own namespace remains the
independent regression gate.
