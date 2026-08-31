# C155 — census of glossary entries without named Lean definitions

Date: 2026-08-31

## Scope and classification rule

This partitions the fourteen entries identified by C151.  `Definable now`
means the glossary states a proposition or carrier precisely enough to encode
without choosing new machine behaviour.  `Blocked` means that an encoding
would decide a model, boundary, or refused implementation that the text leaves
open.  `Not a formal term` means the entry is explanatory vocabulary: giving it
a Lean name would add taxonomy without making a mathematical or operational
claim checkable.

This is a census only.  It adds no Lean declarations and no witness bindings.

## Partition

| glossary entry | class | reason / required definition |
|---|---|---|
| Ambiguity | **definable now** | Define expected observation entropy from the existing normalized observation and predictive kernels.  It needs finite-support entropy (including the zero-mass convention) and the state/outcome weighting already named by the glossary; `expectedFreeEnergy`'s unconstrained `PolicyIndex → ℝ` argument is not that definition. |
| Control states `U` and policy vocabulary | **definable now** | Define a finite control vocabulary, allowable finite-horizon policies over it, and vocabulary extension `U' = U ∪ {u*}` with the new action's transition kernel.  `Cascade` and `TransitionKernel` supply the carriers; the declaration must preserve the paper's statement that pattern/control-schema correspondence is functional, not literal. |
| Aliveness `L=T·H` | **definable now** | Define the two stated factors and their product in a nonnegative value type, including the zero-factor behaviour.  No missing carrier or algorithm is needed; calibration of `T` and `H` would be a separate empirical obligation. |
| Fold | **definable now** | Define the checked plan carrier the prose enumerates—steps, typed wires, terminals, and explicit policy holes—and a structural well-formedness predicate.  Existing fold EDN and escrow validation provide the schema; this does not require defining the refused `organise`. |
| Act-gate | **definable now** | Define the conjunction `cascadeScore > 0 ∧ coverageDelta < 0` over distinct score types, with either missing input producing abstention.  The prose already decides the law and the live producer emits both inputs. |
| Clicks, attempts, and cohorts | **definable now** | Define distinct identifiers/records plus the stated relations: one click starts one forward attempt, bounded repair/review rounds remain inside it, and a cohort is a preregistered semantic-epoch window.  The append-only cohort implementation supplies a source object for later witnesses. |
| Embedding space | **blocked** | The glossary names a possible proposal mechanism but specifies neither vector carrier, similarity/metric, embedding producer, nor pinning semantics.  Choosing those would create the proposal model the text deliberately calls optional. |
| GFlowNet “slush” | **blocked** | A formal term needs a normalized proposal distribution, trajectory/reward semantics, and the boundary between trusted trainer and live candidate lookup.  The current prose points to an experimental implementation but does not license one canonical mathematical object. |
| Substrate and Drawbridge | **blocked** | `Layer.L1/L2` is adjacent but insufficient.  A definition needs typed L1/L2 objects, the projection relation between them, Drawbridge capabilities, and the independent-read/write guard.  Those access semantics are implementation and authority decisions, not fixed by the glossary paragraph. |
| Demonstration Foundry and have–want arrows | **blocked** | Formalization needs arrow endpoint types, composition/match law, and accepted outcome taxonomy across the external futon7/futon3a boundary.  The paragraph describes the mission but does not choose those carriers or laws. |
| Strategic mission selection | **blocked** | The paragraph explicitly says the outer-loop generative structure is not yet represented.  A Lean definition would require strategic outcome carriers, `G_S`, `E_S`, conditional tactical policies, and their evidence boundary; encoding the live additive surrogate would bless the quantity the prose rejects as canonical. |
| Active Inference Framework | **not a formal term** | This is an expository umbrella and a statement of operational intent.  Its substantive components are separately formalizable; an `ActiveInferenceModel` bundle could be useful architecture, but absence of that convenience container is not an undefined mathematical term. |
| EDN | **not a formal term** | EDN is an external serialization language.  Lean need only define the decoded domain objects and validate the parser/fixture boundary; reproducing the EDN grammar in the War Machine theory would not strengthen an AIF claim. |
| A shared experimental substrate | **not a formal term** | This is an architectural thesis about two applications sharing storage, retrieval, and calibration discipline.  Its individual boundaries can have contracts, but the paragraph does not denote one mathematical object whose definition is missing. |

Totals: **6 definable now / 5 blocked / 3 not formal**.  Thus the genuine
remaining vocabulary-definition backlog among these fourteen is six, not
fourteen.  The five blocked entries first need the named modelling or authority
decision; the three expository entries should not inflate the formal backlog.

## Paper omission finding

The seven C151 omissions remain a paper defect, independently of this
partition.  A reader needs all seven because they are the domains and
distributions used by the glossary's own displayed AIF equations:

1. `Outcome` (a vertex-tagged observation);
2. predictive `Q(o∣π)`;
3. transition model `B`;
4. policy prior `E` / `P(π)` as a normalized carrier;
5. preference distribution `C` (distinct from vertex-local pragmatic cost);
6. parameter prior `Q(θ∣π)`; and
7. parameter posterior `Q(θ∣o,π)`.

These are reader-facing semantic nouns, unlike `ProbabilityKernel` row-mass
helpers and value wrappers.  This census does not edit `sec-glossary.tex`; the
paper amendment belongs to the evidence/editorial sequence.
