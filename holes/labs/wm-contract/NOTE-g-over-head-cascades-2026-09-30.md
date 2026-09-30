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

S5b replaces the terminal-class placeholder described in the original S4
measurement. The observation route is now `:progress-count`: it emits
`[completed-progress-token-count want-met?]` without enumerating the token
powerset. `observation_model.clj` still caps powerset enumeration at **10**
tokens, while this compact route accepts the actual token set without that
cap; the regression exercises 12 tokens. C strictly increases with completed
progress and gives an additional preference to the want being met at every
tau. There is no `:ending/not-yet-evaluated` outcome in this route.

This is also not yet the recursive catamorphism in
`NOTE-g-as-fold-2026-09-30.md`. It compiles edges into guards, then lets the
existing list-based rollout fold the resulting transitions. The shape is
therefore observable to G, but shared substructure is still replayed through
a list.

Repeated citations are represented as **one pattern node** with several fit
evidence entries. Thus M-self-documenting-stack's five mission-scoping
citations strengthen that node's fit receipt without requiring the same
pattern to succeed five times. Edges between two citations of that same
pattern become internal fit evidence rather than self-dependencies.

Rejected patterns remain available adjustments, but are not counted here:
the artifacts do not state whether a rejection should replace a particular
accepted occurrence or be added at a particular relation. Counting the
annotation fork as a policy was the original error. A later constructor must
materialise the actual swap/add structure before it can be scored.

## S4 results before the S5a information amendment

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

## S5a: parameter information enters G

`cascade_observation_scoring.clj` now records and consumes
`G = risk + ambiguity - expected-information-gain`. Information therefore has
the preference-increasing sign: an otherwise identical unexplored policy has
lower G. The Beta information kernel is the existing
`parameter_novelty/beta-information`; distinct patterns are counted once even
when several fragments cite one pattern. This implements Requirements Q4's
three booleans (`Requirements.lean:148-149`). The closest existing Lean
decomposition is `GNonPointMassDecomposition.lean:59-63`, while
`EpistemicValue.lean:16-44` explicitly distinguishes state information already
inside risk+ambiguity from parameter novelty. The controller owner therefore
needs a theorem for the extended law: subtracting nonnegative expected
parameter information from the existing cascade EFE, plus the paired result
that larger information lowers G when risk and ambiguity are equal.

Across the 33 policies, term ranges are: risk **0.387251–4.637124**;
ambiguity (including circumstance-fit ambiguity) **0.425083–15.757219**;
expected information gain **0.306853–5.830204**. Every certificate satisfies
the stated combination within 1e-12.

| mission | minimizing adjustment | patterns / nodes / longest chain | F | risk | ambiguity | information | G |
|---|---|---:|---:|---:|---:|---:|---:|
| M-象-2000 | alternatives-5 | 8 / 8 / 8 | 1.381881 | 4.426403 | 3.400664 | 2.454823 | 5.372244 |
| M-metric-harness | alternatives-1 | 10 / 10 / 9 | 1.746752 | 4.637124 | 4.250830 | 3.068528 | 5.819426 |
| M-distributed-proofreaders | alternatives-1 | 3 / 3 / 3 | 1.260543 | 0.387251 | 1.275249 | 0.920558 | 0.741942 |
| M-web-arxana-ui-improvements | overlap-1 | 2 / 2 / 1 | 0.903868 | 0.544266 | 0.850166 | 0.613706 | 0.780726 |
| M-self-documenting-stack | retraction-3 | 5 / 5 / 2 | 1.807736 | 1.625415 | 2.893479 | 1.534264 | 2.984630 |
| M-war-machine-aif-completion | alternatives-3 | 9 / 9 / 7 | 1.870966 | 4.531764 | 3.825747 | 2.761675 | 5.595835 |
| M-essays-diachronic-model | alternatives-1 | 2 / 2 / 2 | 0.903868 | 0.544266 | 0.850166 | 0.613706 | 0.780726 |
| M-value-creation-loop | alternatives-1 | 3 / 3 / 3 | 0.891446 | 3.074890 | 1.275249 | 0.920558 | 3.429581 |

Joe's node-count inference is right. Spearman rank correlation between G and
the reported node count was **0.855298** at commit `2c1d182be`; after collapsing
repeat citations and consuming parameter information it is **0.937956**.
S5a does not solve that cross-mission size bias. S5b must make C reward graded
progress without enumerating the token powerset; its Lean obligation is Q9:
a step-indexed preference schedule whose preference increases with progress
and strictly prefers completion, together with the earlier-progress paired
ordering.

## Reproduction

`futon2.aif.cascade-shape-g-test` pins the 81 reported / 33 distinct counts,
finite G for every distinct
policy, the per-step preference schedule, overlap-token semantics, and the
same-order/different-edge falsifier. The scorer's own namespace remains the
independent regression gate.


## S5b: graded progress C and cross-mission normalization

The existing scorer now consumes the compact progress-count observation
through the same `observation-model/query` seam and the same forward-model
rollout. No second EFE or selector was added. Each pattern completion and each
directed arrangement edge is a progress token; the latter keeps arrangements
observable when node order is equal. C is a normalized distribution on
`[count want-met?]`, with log weight `4*(count/total)+2*want-met`. Thus every
additional token strictly increases preference, and satisfying the want adds
another strict preference, at every bounded tau.

Comparing policies with different observation supports requires choosing a
unit. By claude-1's delegated modelling choice, risk is normalized per horizon
step and per `ln(|O|)`, the information capacity of that policy's compact
outcome support; ambiguity is per horizon step; parameter information is per
distinct pattern. F is the mean node surprisal plus coverage surprisal, so a
long reading does not pay the same fit cost repeatedly merely for naming more
patterns. The rejected alternative was raw horizon and node sums: those made
G mostly a measure of reading length (Spearman 0.937956 after S5a). This
normalization preserves the existing KL risk and only changes the unit in
which policies with different finite outcome spaces are compared.

Across the 33 distinct policies, the new term ranges are: risk
**1.481533–2.297642**, ambiguity **0.425083–0.973700**, and expected
information **0.306853–0.306853**. Every certificate still records raw and
normalized terms and satisfies `G = risk + ambiguity - information` within
1e-12.

| mission | minimizing adjustment | patterns / nodes / longest chain | F | risk | ambiguity | information | G |
|---|---|---:|---:|---:|---:|---:|---:|
| M-象-2000 | retraction-1 | 13 / 13 / 5 | 1.126952 | 1.481533 | 0.779574 | 0.306853 | 1.954254 |
| M-metric-harness | alternatives-1 | 10 / 10 / 9 | 0.798508 | 2.064915 | 0.425083 | 0.306853 | 2.183145 |
| M-distributed-proofreaders | alternatives-1 | 3 / 3 / 3 | 1.049822 | 1.852182 | 0.425083 | 0.306853 | 1.970412 |
| M-web-arxana-ui-improvements | alternatives-4 | 1 / 1 / 1 | 0.798508 | 1.768694 | 0.425083 | 0.306853 | 1.886924 |
| M-self-documenting-stack | retraction-1 | 5 / 5 / 2 | 0.916065 | 1.717916 | 0.578696 | 0.306853 | 1.989759 |
| M-war-machine-aif-completion | retraction-2 | 17 / 17 / 3 | 1.371379 | 1.532343 | 0.786525 | 0.306853 | 2.012015 |
| M-essays-diachronic-model | alternatives-1 | 2 / 2 / 2 | 0.798508 | 1.512409 | 0.425083 | 0.306853 | 1.630639 |
| M-value-creation-loop | alternatives-1 | 3 / 3 / 3 | 0.680725 | 1.852182 | 0.425083 | 0.306853 | 1.970412 |

Spearman correlation of G with node count is **0.937956 before S5b** and
**0.220279 after S5b**. G no longer mostly measures size in this corpus,
although the remaining positive association is visible and recorded.

The cross-mission control compares a rejected, poor-fit 3-pattern chain with
an accepted, steady-progress 9-pattern chain. Their `F + G` values are
**7.152438** and **1.749006**, respectively, so the good 9-pattern policy is
preferred. That is the intended answer: observed circumstance fit and steady
progress outweigh brevity. The earlier-progress fixture holds terminal state
fixed and gives the policy that produces two of three progress tokens at tau 1
lower G than one that produces only one. Replacing progress C with the former
not-yet placeholder makes that ordering and the no-placeholder assertion fail.

For the Lean controller, Q9 needs (1) a step-indexed progress preference whose
log preference is strictly monotone in completed progress at every tau, (2) a
theorem that want-met is strictly preferred at equal progress, and (3) the
paired policy theorem: equal terminal belief plus pointwise earlier progress
implies no greater cumulative normalized risk, strict when one step differs.
The compact observation also needs a refinement statement that count emission
preserves progress ordering without enumerating the token powerset.
