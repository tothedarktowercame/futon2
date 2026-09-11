# RUN4 repairs as formal contracts — PROPOSED, 2026-09-11

Joe's direction is to retain the invariants established by repairs in the Lean model, rather than allow runtime fixes and formal claims to diverge. This note and its Lean draft do not declare RUN4 accepted or change the registry/worklist.

## What the inspected model establishes today

`mathlib4/DarkTower/WarMachine/Holes.lean:112` defines `Cohort` with a positive stopping target and `attempts.length ≤ stoppingTarget`. `MachineVocabularyWitness.lean` instantiates the historical `wm-outer-loop-46-v1` cohort at target 3. Exhausting that cohort was compatible with this contract: the missing implementation connection was the selection of the correct cohort for RUN4.

The `Cohort` carrier does not require unique attempt identifiers, global qualification by cohort, or a series-to-cohort binding. The new Lean draft gives a compiling counterexample: a valid existing Cohort with attempts `[1, 1]`. This establishes a concrete omission in this carrier, not a claim that every module in the repository lacks related identity laws. `GainChain` already has identity-threading laws at another grain; those do not by themselves certify filesystem keys for RUN4.

The existing `wmRunConformsToWiring` obligation concerns the recorded route against wiring. A route certificate alone does not establish global attempt-key uniqueness, durable write behavior, or preservation of a cohort attempt when its closing consumer throws. These need additional contracts and implementation evidence.

## Contract-to-consumer obligations

| Requirement | Lean status | Runtime evidence required |
|---|---|---|
| Select the explicitly bound cohort, with fresh preregistration identity and capacity | Existing Cohort bounds; series binding still needs a formal relation | Serving preparation to real runner start/checkpoint/close, already exercised by the new unavailable-author fixture; wrong-cohort, stale pin, exhausted fresh admission refuse |
| Global attempt keys distinguish cohorts even when local ordinals repeat | Draft pair-key theorem plus counterexample for dropping cohort identity | Every repair, brief, run record and checkpoint consumer uses an injective encoding; two real separate cohort roots at local ordinal 1 cannot collide; old evidence untouched |
| Existing attempts remain inspectable at zero remaining capacity | Draft separates mayStart from mayInspect; started never starts again | Authenticated durable lifecycle joins allow terminal inspection after capacity consumption; no redispatch or caller-selected bypass flag |
| Missing or foreign terminal evidence cannot produce completion | Draft preserves started state for missing/foreign evidence | Existing strict bundle consumer and negative controls; absent projection/record remains unknown; operator acceptance stays separate |
| Closure failure preserves the original attempt identity and accumulated evidence | Proposed obligation, not proved in this draft | Inject a failing repair/brief/record sink after selection; return/persist original identity and six checkpoints with explicit closure failure rather than a new initialization identity |
| Handler reconfiguration preserves HTTP and WebSocket behavior | Proposed separate transport contract | Existing composition/migration tests and live connection checks; no inference from a Lean identity theorem alone |

The lifecycle draft's key-only terminal observation is deliberately incomplete: real completion additionally needs checkpoint, review/build/grounding, source-digest and durable-join checks. Its `mayInspect` predicate authorizes neither completion nor execution. Further formalization must strengthen these definitions before claiming full acceptance semantics.

## Completed bounded formal artifact

Mathlib4 commit `00eb0c045d`, `DarkTower/WarMachine/RunLifecycleContractDraft.lean`:

- Counterexample showing existing Cohort permits duplicate local identifiers.
- Distinct-cohort pair-key inequality, plus a collision witness for local-only projection.
- Exhausted fresh admission refusal; started attempts cannot redispatch; existing inspection remains available.
- Missing and foreign terminal identity leave an attempt started.

Executed `lake env lean DarkTower/WarMachine/RunLifecycleContractDraft.lean`: exit 0. Printed axiom dependencies for the counterexample and identity inequality are empty; the two other printed lifecycle theorems use only `propext`. No new axioms or sorry declarations were added. The draft is not imported into a production readiness gate and makes no claim that Clojure refines it.

## Evidence needed to promote the draft

Each repair should retain four linked artifacts: (1) the intended formal invariant, (2) a counterexample or refusing control corresponding to the observed failure, (3) the concrete consumer mapping and executable regression, and (4) the pinned runtime receipt establishing the stated scope. A generic theorem, a fixture execution, and a live run have different scopes and must remain distinguishable.

Runtime repair evidence remains in futon3c `eb6d91f3`: selected stop-line repair, global attempt-001 finding collision, missing closed checkpoint and run record, and finalization refused by the fresh-capacity gate. None has been rewritten as a successful run. The pending implementation repair can cite this contract draft; its tests must establish the encoding and consumer correspondence rather than assuming them from the pair-key theorem.
