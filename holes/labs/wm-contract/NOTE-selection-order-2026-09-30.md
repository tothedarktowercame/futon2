# Selection order: construct from the library, select, then interpret

Date: 2026-09-30. Scope: proposal and executable negative tests only. No
production behavior changes in this packet.

Joe's ruling is the order of operations: retrieve applicable patterns, build
and select a cascade, then obtain the target-specific interpretation needed to
enact it. Selection must not require that interpretation to pre-exist.

## Where interpretation currently precedes selection

| Stage | Enforcement | Authority |
|---|---|---|
| Retrieval | `interpretation-request` declares the embedding and tier0 retrievers over the library (`src/futon2/aif/interpretation_request.clj:15-20`), pins the library and executes both ranked queries (`:245-277`), but puts the slice only in an interpretation request (`:283-295`). | The query-time slice agrees with `futon3/library/cascades/on-the-fly-cascade.flexiarg:18-29`. Routing it only to a request does not. |
| Assembly | `base-problem` refuses an empty target-specific pattern map as `:no-admitted-interpretation` (`src/futon2/aif/cascade_problems.clj:178-202`). `assemble-one` repeats that refusal before candidate checks (`:204-253`). | This pre-construction admission gate is **not** in the specification. Stage 0 requires problem-conditioned construction (`SPEC-cascade-policy-semantics-2026-09-15.md:33-43`). |
| Construction | Construction is called only when the admitted interpretation map is nonempty (`src/futon2/aif/cascade_problems.clj:218-229`), and its pool is exactly that map plus its receipts (`:117-135`). The construction-input horizon is likewise computed from admitted interpretations or already-declared orders (`src/futon2/aif/wm/construction_inputs.clj:58-83`). | Per-problem construction is specified (`SPEC…:35-43`); restricting its pool to prior interpretations is not. |
| Candidate admission | A candidate containing any pattern absent from the interpretation map is refused (`src/futon2/aif/cascade_problems.clj:233-253`). Later admission also requires paired interpretation receipts before scoring (`src/futon2/aif/wm/cascade_decision.clj:1230-1281`, through `admit-cascade-problem`). | The spec does say every firing pattern needs interpreted B to be *scoreable* (`SPEC…:23-31`, especially `:25`), but does not authorize removing the target before construction. Joe's newer ruling changes this order. |
| Prediction | `cascade-lane` converts every interpretation to a token operator and uses those operators in each candidate (`src/futon2/aif/wm/cascade_decision.clj:165-195`). The joint decision repeats that conversion for admitted candidates (`:730-760`). | This is the existing realization of §4 A1. It honestly cannot predict an uninterpreted operator; it should remain honest rather than synthesize one. |
| Scoring | R5 ranks only candidates for which R4 produced predictions (`src/futon2/aif/wm/cascade_decision.clj:217-270`). The specification says missing B is typed missing input, never invented (`SPEC…:15,25,37`). | The typed absence is specified. Treating it as exclusion from the policy menu conflicts with Joe's select-then-interpret ruling and is the decision still owed below. |

The Lean boundary records the old order rather than solving the new one.
`ObservedInterpretation` requires a published pattern map and preserves an
absence as `Except` (`mathlib4/DarkTower/WarMachine/Proof2/ObservedInterpretation.lean:24-60`).
`cascadePolicySet` merely turns a supplied menu into a set
(`mathlib4/DarkTower/WarMachine/Proof2/CascadePolicySet.lean:46-50`); it says
nothing about library retrieval or menu coverage.

## Small handoffs on the existing decision path

1. **Expose one query-time slice producer.** Extract the already-pinned,
   normalized result of `captured-request!` into a pure result usable by both
   request preparation and selection. Preserve target, query, ranks, retriever
   runs, source revisions, failures and library pins. Bad case: an unresolved
   or ambiguous pattern id remains a typed retrieval failure and never enters
   the pool.
2. **Carry slices through `construction-inputs`.** Add the per-target slice to
   the existing assembly input; do not create another decision function.
   `assemble-cascade-problems` must retain it even when target-specific
   interpretations are empty. Bad case: five targets with slices and no
   interpretations do not become five `:no-admitted-interpretation` assembly
   refusals. T1 in `selection_order_incident_test.clj` fixes that boundary.
3. **Construct identities before operators.** Change the existing constructor
   so its structural pool is the slice's pattern identities and provenance.
   Its receipt records parent, delta, retrieval evidence and which chosen nodes
   lack B. Bad case: a pattern outside the pinned slice cannot appear in a
   construction.
4. **Decide how missing B is scored.** Implement Joe's chosen option from the
   next section in the existing R4/R5 path. Bad case: missing B is neither an
   invented attested operator nor silently equivalent to a known operator.
5. **Select and record debt.** The existing selection certificate records one
   `{:kind :interpretation-owed-after-selection, :target …, :pattern …,
   :attested? false, :retrieval …}` for every chosen uninterpreted pattern.
   T2 fixes this contract. Bad case: a selected debt cannot be serialized as
   `:documented-interpretation` or an interpretation receipt.
6. **Interpret the chosen nodes, then resume the same run.** The existing
   interpretation job consumes only the selected debts; its validated result
   supplies B to the same candidate and the existing prediction/enactment
   continuation. Bad case: interpreting a different pattern or target cannot
   discharge the debt.

Each handoff changes one boundary in `cascade-decision`; none adds a thin
selector, a second runner, or an alternate policy decision.

## What is G before the chosen pattern has B?

The current executable and Lean EFE require a transition kernel. Runtime R4
calls `forward-model/predict-multi-horizon` with concrete token operators
(`src/futon2/aif/wm/cascade_decision.clj:165-195`), and R5 ranks the resulting
predictive distributions (`:217-270`). The Lean `ObservedInterpretation`
explicitly prevents absence from becoming a published operator
(`ObservedInterpretation.lean:45-60`). Thus the existing code supports an
honest typed missing-B result; it does **not** currently support a scalar G for
that cascade.

Two coherent choices are visible, and Joe must choose explicitly:

1. **A declared uncertain transition prior.** Associate an uninterpreted
   library pattern with a real prior distribution over transition kernels and
   score the predictive mixture, including the information expected from
   obtaining its interpretation. This matches the active-inference meaning of
   epistemic value, but the current `forward-model/predict-multi-horizon`,
   `efe/rank-actions`, and Lean `CascadeTransition` take one concrete B; no
   distribution-over-B implementation was found. This is new model work, and
   the prior and its authority cannot be guessed.
2. **A pre-interpretation selection score using only defined terms.** Rank the
   retrieved structural candidates using terms that do not require B, record
   that this is not canonical G, select, then obtain B and compute canonical G
   before authorization. Existing retrieval ranks and provenance support a
   deterministic preselection, while the current EFE code supports the later
   canonical score. This preserves honest absence but means the first choice is
   not an EFE posterior unless the objective is separately specified.

A third apparent option—assigning identity, zero, infinity, or a hand-written
operator to absent B—is ruled out by the Stage-0 missing-input rule
(`SPEC…:37`) and by `ObservedInterpretation.absent_is_not_published`
(`ObservedInterpretation.lean:57-61`). The negative tests deliberately demand
selection and a typed debt without pretending that either unresolved option
has already been decided.

## Existing interpretation files after the pool changes

`resources/wm/cascade-sources/*.edn` cease to define the available pattern
pool. Their target facts, wants, preferences, locators, explicit precedences
and validated interpretations remain inputs where those fields are still
authoritative. Any hand-authored candidate list becomes a recorded proposal or
regression fixture, not the complete menu.

`data/wm-interpretations/<target>.edn` remains the cache and evidence for
validated target-specific operators. Entries can satisfy interpretation debt
immediately when their target, pattern, source revision and model identity
match. Absence no longer removes a pattern or target from selection; stale or
mismatched entries remain typed absences. New validated interpretations are
written only after selection and can be reused on later runs under their
existing revision checks.

The library slice is therefore the construction pool; these files supply
model semantics and other declared inputs. Neither is silently promoted to the
other role.

## Executable incident boundary

`test/futon2/aif/selection_order_incident_test.clj` uses
`wm.construction-inputs/assemble-cascade-problems` and
`wm.cascade-decision/cascade-decision`. T1 requires five uninterpreted targets
with several retrieved patterns each to yield at least five scored policies.
T2 requires a selected library pattern and explicit post-selection
interpretation debt. Both are tagged `:incident`, which the normal green-suite selection excludes.
Run the incident directly with:

```sh
clojure -M:test -m cognitect.test-runner \
  -n futon2.aif.selection-order-incident-test -i :incident
```

On the current implementation both fail because assembly produces five
`:no-admitted-interpretation` refusals and the decision abstains.
