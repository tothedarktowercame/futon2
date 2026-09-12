# WM Lean attestation audit — 2026-09-12

Discovery report for Joe's Ruling 5, requested by claude-15 (`invoke-1789227735286-20409-75703650`). No Lean/runtime changes, gate execution, replay, live admission, or attestation renaming was performed. Checks below were read-only enumeration, strict single-form EDN parsing, source inspection and byte hashing. Proposed changes are not implemented by this report.

## Finding

Addendum (2026-09-12): separate Ruling-5 design-validity assessments for [U88 successor v1](runs/RUN4-design-validity-assessments-2026-09-12/run4-u88-zai-successor-20260912-v1.edn) and [v4](runs/RUN4-design-validity-assessments-2026-09-12/run4-u88-zai-successor-20260912-v4.edn) pin the original checkpoints and preserve their grounded-change outcomes while recording that the required deliverable chain is not established.

**Confirmed class-(a) Lean attestations over the twelve records: 0. This is not a finding that RUN4 satisfied the design.** The inspected Lean corpus contains no theorem instantiating full construction validity on these twelve executions. The named run-route certificates concern September 1 and September 4 fixtures, and the remaining generic/model theorems do not assert their Clojure correspondence. The route battery is a runtime checker of route properties; its green result is not a Lean proof of a cascade, typed holes, or construction wiring.

There is a concrete production evidence gap: two of the twelve records close as `:grounded-change` while their ordinary construction judgment has `:wiring nil`. They are U88 Zai successor v1 and v4. Under Ruling 5 these records do not establish the required G → cascade → holes → wiring chain. Existing outcome labels must not be presented as proved-AIF-valid. That is a liability even without a false Lean theorem. No retrospective wiring or runtime proof may be fabricated.

The statement “all twelve carried the same cascade” is not supported by the actual population. The scoring report's exact glob yields twelve files: **three ordinary selected-policy constructions, seven historical-repair revalidations, and two not-reached construction cells**. Ten have an explicit nil wiring field (three plus seven); the two sorry cells have no judgment. A lookup of an absent field also returns nil, which obscures this distinction. The three ordinary constructions have the reported two-pattern cascade and -0.703 score. The historical admissions have zero patterns and no cascade score; they explicitly do not claim task success. The table below is the corrected population. The scoring report also names a v3 successor for which this glob has no construction checkpoint in the audit snapshot.

This report does not reinterpret an ambiguous claim generously: words such as “conformant”, “closed-by-record”, “certified”, and “wiring” have different scopes here. The exact declaration types, not those words alone, determine the classification. In particular, route wiring is not construction/fold wiring.

## Scope and pins

Read Rulings 1–5 and V4 before inspecting the corpus. All 132 top-level `.lean` files in `DarkTower/WarMachine` were enumerated; declarations and file pins are recorded in Appendix A. The appendix inventories theorem statements (whitespace normalized, bodies omitted), plus relevant carrier/opaque declarations. It is a source audit, not a fresh elaboration or independent proof check. Nested negative fixtures, if any, are not claims about these runs. No new axiom-clean build is claimed. Futon2 advanced during the audit from `463c970516f135c2e64721e7adf17c53056d1700` to `79eb907d3dd63034ac5d787c654a58245c1fe996` by a note-only update recording the tightened gate; the final contextual pins include that update.

Repository snapshots: `mathlib4` `a9a24a3b9e070550ede41ebd10621c5bb9b843f0`, `futon2` `79eb907d3dd63034ac5d787c654a58245c1fe996`, `futon3c` `6d5bcb69834cab494142529e8441a26efb66726c`.

## Attestations and their actual joins

### A. Named run certificates and open world-level claims

`Holes.wmRunConformsToWiring : Prop := sorry` is an **open proposition placeholder**, registered with `mkHole`, not a proof of that proposition. Its comment says OPEN/RUN-GATED and requires Joe's acceptance over a qualifying run. `wmRunsOnce : Prop := sorry` likewise names an external event obligation; its documentation points to the bounded August 30 stub record. Neither is evidence of a September 11–12 construction. Their presence must not be converted into “Lean validated this run.” These are class (c), with explicit unresolved obligations, not successful attestations. The `sorry` defines a proposition; it does not automatically supply a proof of that proposition.

`Holes.runConformsToDrawnWiring routes` requires nonempty routes, no empty member route, and no hop classified unmapped or refutation. It allows route-measured edges, dependency exclusions and ruling-unrealised edges. It contains **no construction, holes, G evaluation or fold input**. `wmS5RunConformsToDrawnWiring` and `wmRe5RunConformsToDrawnWiring` prove it by `decide` for literal `s5Routes` and `re5Routes`. Their census theorems report four routes, 36 hops, nine distinct hops, only two drawn and five measured hops, and nineteen unfired drawn edges. Thus even the route claim is not that every architectural edge executed.

The mechanical historical join is outside Lean: `u49_route_transcribe.bb` / `run3_conformance.bb` produce pinned literal blocks; `run4_readiness.bb` checks the blocks occur verbatim, the preregistration artifact bytes, trace/control-map hashes and recorded axiom-probe freshness. The readiness checker was source-inspected, not executed here; the generator relationships are recorded in the pinned Lean documentation. Some producers write outputs. Pins in the Lean manifest:

- Control map: `161d0abffd21551078ac2d7a87427e6cacafbfca0695c09a496260be21fafdec`.
- September 1 S5 extracted trace: `c3480955286e548be6fd4bbde80e5081446cfaaac1500294e2de1fa44e2d7c67`.
- September 4 RE5 extracted trace: `f343432d772986ddc2f7fe38107913afc997201ebae9b6240dff152e235b1120`.
- U49 contract commit: `11c2e44affa167bf85b0a2d7c29d8705f89a8d08`; RE5: `581034b67e47240b7a4003dc39bdd7dd545e1daf`.

`Run4Preregistration.run4CertifiedS5Conformance`, `run4CertifiedRe5Conformance`, the two census aliases, `wmRun4PreregCensus`, and invalidator partition concern that same predeclared set. The module explicitly says **“It accepts nothing and closes nothing.”** Both candidate runs are `mintedAwaitingAcceptance`. “RUN4” in the filename does not bind future RUN4 records. All these instantiated assertions are class (c) for the twelve.

`wmS5SelectionDiscrimination` actually proves **¬ selectionDiscriminates s5SelectionTies**; its RE5 counterpart and tie censuses concern their own decision tables. They must not be counted as generic positive selection validation. `wmTraceR2` / `r2ContractCensusWmTrace` describe an August 31 snapshot of 801 rows (two incomplete); its content watermark is `b2c3aeb408cc4de59947ad93f9c1ea17b735fc0da26e188ada7c24609bffbca1`. R8 literal censuses, July selected/enacted comparisons, the preference-stack record, find-snatch receipts and ablation table likewise have finite historical domains. Their comments name adapters/fixtures; Lean itself does not hash filesystem bytes. None imports these twelve checkpoints. See Appendix A for each declaration rather than conflating the whole `Holes` file with a proof.

### B. F12 organise and F11 find

`F12Conformance.ConformantOrganiseSelected` / `ConformantOrganiseNodes` state O1–O3; their module explicitly does not state O4 for the old carrier. `organiseSelectedOnlyConformant` and `organiseUpClosureConformant` prove properties of explicitly defined Lean functions. `F12DischargeArm.organiseDischargeExistsSelected` proves **∃ f, ConformantOrganiseSelected f**, not that runtime `organise` is that f. The same file proves a type inhabitant can be nonconformant and conformance need not identify a unique function.

`F12RuledCarrier.ConformantOrganiseRuled` has seven clauses: selected set and authored edge preservation, admission attribution, node union, organised-edge reachability, no-bootstrap fast-forward correspondence, and the implication that changed precedence entails changed acting order or score. `organiseRuledConformant` proves them for `organiseRuled`. Its O4 antecedent is false for the recorded zaif instantiation (both precedence lists empty); the source states this. This is no proof that the run computed a beneficial order or a G-derived outline.

Admitting, attribution, support, CascadeDiff, O3, D1 and discharge arms test alternative signatures/readings. Ants/mining/six snatch exemplars instantiate named data, including negative results where recorded edges do not survive the ruled no-bootstrap condition. `F12SixthSnatchExemplar.snatchG5SharerNoRecordedEdgeSurvives`, for example, proves the filtered recorded edge list is empty. Those are informative discrepancies, not generic runtime certification.

The joins are fixture transcriptions and external checks under `futon2/holes/labs/wm-contract/f12_*`, often sourced from `futon3/checks` recorded experiments. `f12_ruled_carrier_check.bb`, inspected here, checks clause strings, signatures, forbidden constructs and mutation controls and writes `runs/F12-organise/17-ruled-carrier.edn`; it does not read RUN4 construction checkpoints or establish execution equivalence. Lean's equality to literal fixture data is mechanical; identification of that Lean function with a production invocation is not. No digest chain from any of the twelve construction files to an F12 theorem was found.

F11 conformance/receipt/ruled/amended/discharge families similarly concern find-result obligations. `F11AppliedConformance` explicitly restricts itself to a three-pattern Snatch round-2 model and disclaims opaque-find correspondence, full 24-pattern replay, cryptographic hashing and live source loading. The original `wmFindSnatchF1Containment`/F2/F3/F4 concern 34 recorded rows and source fixture SHA `c11673ea7164e90b10cc378ab6b2dfe14e545449d85e0dde70d5c2282e2430ce`. These finite fixtures are class (c). Generic mathematical conformance/existence results remain valid independently of wiring, class (b), but cannot be promoted to run validity.

### C. Fold, G, observation, lifecycle and contract carriers

`Holes.Fold Wiring PolicyHole` has `wiring : Wiring`, `coverageScoreDelta : Option ℝ`, `policyHoles : List PolicyHole`. It does not constrain `Wiring` to a nonempty graph or demand nonempty holes. Choosing a nullable or trivial carrier is not excluded by this type. `FoldWitness.recordedFoldFields` proves literal counts 5 nodes/5 hyperedges/1 terminal, delta -1, 3 policy holes. It does not deserialize a RUN4 fold. `FoldEscrowRecordWitness.matching_is_reconstructible` and `mismatch_is_not_reconstructible` use Nat values 7/14/15 and arithmetic digest functions; they do not prove SHA or runtime escrow completeness. `FoldCWitness` proves identity of the empty layer fold and the observable order difference 8 versus 7 for toy preference layers. These are class (c) fixtures; their generic definitions are class (b), insufficient for the newly demanded construction chain.

`Holes.G_eq_expectedFreeEnergy` explicitly assumes risk equals kernel-derived KL and ambiguity equals negative epistemic gain. It does not prove those bridge assumptions for controller scores. MachinePolicyFreeEnergy, ExpectedFreeEnergy, kernel, belief, precision, observation and other witness modules prove equations or finite reference cases. Their external Clojure readbacks measure numeric deltas, often with 1e-12 tolerances, against those fixtures. Example: `machineTwoChannelTotal` is a two-channel reference with fixed means/variances; `stackPctIsNotClamped` is deliberately a negative boundedness result. Readback agreement does not pin a later run's actual inputs to those hypotheses. No theorem application to the twelve turn-specific G records was found.

`RunLifecycleContractDraft` explicitly disclaims certification of Clojure, serialization and live runs. It proves duplicate local attempts are permitted by the old carrier and proposes paired keys, no redispatch of started states and no completion from missing/foreign evidence. Its simple cohort/local key is not the later runtime versioned physical-root identity. It is class (b) model reasoning, not evidence that runtime lifecycle correspondence is closed.

`ContractEmitter` emits requirement metadata, deliberately **no `holds` field**. `CoverageReport` proves distinctions among scored, uncovered and absent criterion reports, explicitly not score quality or whether a criterion is right. GainChain and related family modules describe model predicates and controls. `Holes.registry`/`holes-contract.json` dispositions are evidence-obligation metadata; `CLOSED-BY-RECORD` is not a universal proof over subsequent runs. These are class (b) contract statements, and any UI treating them as per-run validation would overstate them.

### D. Chip-board witnesses: separate runtime, explicit boundary

`ChipBoardWitness` and `CascadeVerifierBoardWitness` concern the two-wire chip executor and two pinned September 12 board traces, not RUN4's fold checkpoint. The repaired hazard theorem assumes declared verb semantics; the no-act theorem also depends on the declared registry, not merely absence of a zap-labelled chip. Both retain counterexamples showing why weaker assumptions are inadequate. Board and input digest strings are carriers; they are not Lean proofs of cryptographic identity or arbitrary function semantics.

The readback scripts compare literal trace projections with Clojure replay. Inbox board digest is `sha256:a09756448d0dfb11eb1cdd8e11ef03c2be473c836cd4e8f82b6e222e5fff52b4`; cascade verifier board digest is `sha256:3a806b34dd46e94c4b91adead7b86105d7e0c682452392375a3cfbf90b863664`. Runtime replay binds full effect traces and the registry snapshot's in-process function-identity digest. It does not prove the implementations obey the modeled semantics. Producer metadata at commit `52856d8f` explicitly carries `:scope :lean-model`, `:assumption :registered-verbs-implement-declared-semantics`, `:runtime-correspondence :not-proven`, and `:semantic-approval? false`. Witness commits are `a9a24a3b9e070550ede41ebd10621c5bb9b843f0` and `e407ec20cbedb8667070dee89cd45f27a52207a0`. Class (c) for RUN4; no weakening of this status is proposed.

### E. Actual RUN4 runtime report boundary

`futon2.aif.run4-route-conformance/verdict` recovers a contiguous node route from a run record and applies U49 edge classification. `conforms-routes?` mirrors the Lean route predicate but there is no extracted Lean execution engine. `run4-battery/produce` emits four route checks (nonempty, no-empty, no-unmapped, no-refutation), joined by series/control-map digests and run/trial IDs. Neither consumes fold wiring. `run4-acceptance-report` joins terminal bundles, recordings and that battery; it always emits `:accepted? false` and reserves the operator decision. Its green route result can legitimately hold with missing fold wiring, class (b) **only as a route result**. It is not a certificate of Ruling 5 compliance.

Historical qualification/verification artifacts are another distinct authority: executed checks + source pins + independent review, then immutable repair-store admission; awaiting-validation is not repair resolution. Their schemas/reader names containing “verification” do not make them Lean attestations. They must retain their original scope and bytes. A separate design-validity assessment can say a historical run is incomplete without rewriting it.

## Per-turn persisted G inventory

The exact twelve-file glob is `/home/joe/run4/*/cohort/*/attempt-*/003-construction.edn`. Every row below uses local `attempt-001`; absolute paths and byte hashes are in Appendix B. H = historical revalidation (one stop-line-ranked candidate, no pattern outline); P = ordinary selected-policy construction; N = not reached. For each row, selection and construction were strictly parsed as one EDN form. `controller-score` below is the **ordinary decision's** score, not proof it selected the enacted task. Historical stop-line selection and authenticated U88 pin selection override that interpretation.

| Cohort | Kind | Ordinary decision score | Selection ranked count | Cascade score | Close outcome |
|---|---|---:|---:|---:|---|
| `run4-u88-codex20-20260911-v1` | N | absent | 0 | absent | `no close judgment found` |
| `run4-u88-codex20-20260911-v2` | P | 1.7426769851658865 | 10 | -0.703 | `build-failed` |
| `run4-u88-zai-successor-20260912-v4` | P | 4.551083233140574 | 10 | -0.703 | `grounded-change` |
| `run4-u88-zai-successor-20260912-v1` | P | 5.371450289905667 | 10 | -0.703 | `grounded-change` |
| `run4-ea1-artifact-binding-admission-20260912-v2` | H | 6.168821524173083 | 1 | absent | `historical-verification-awaiting-validation` |
| `run4-ea1-artifact-binding-admission-20260912-v1` | N | absent | 0 | absent | `agent-unavailable` |
| `run4-initialization-close-admission-20260911-v1` | H | -0.0767462996119117 | 1 | absent | `historical-verification-awaiting-validation` |
| `run4-initialization-collision-admission-20260911-v1` | H | 2.8677963102075528 | 1 | absent | `historical-verification-awaiting-validation` |
| `run4-initialization38690-admission-20260911-v1` | H | 4.5635444742605245 | 1 | absent | `historical-verification-awaiting-validation` |
| `run4-repair-pinned-selection-admission-20260911-v1` | H | -0.06407451815466869 | 1 | absent | `historical-verification-awaiting-validation` |
| `run4-repair058-admission-20260911-v1` | H | 9.185755878650085 | 1 | absent | `historical-verification-awaiting-validation` |
| `run4-successor-v2-selection-admission-20260911-v1` | H | 7.117582122885196 | 1 | absent | `historical-verification-awaiting-validation` |

For the three P rows, `002-selection :payload :judgment` preserves selected mission/action, ten ranked candidates with `:G-efe`, `:controller-score`, habit bias, and selection reasons. `:payload :ground :decision` additionally preserves controller ranking, softmax weights, selected policy ID, selection-law provenance, decision explanation including `:top-G`, tau source and strategic-memory metadata. The top-G example in all three names **M-expressions-of-interest**, while the enacted pinned mission is U88. The `:run4/operator-selection` provenance is therefore essential; a score in the checkpoint is not evidence G chose the U88 outline.

In all three P records the F-pi posterior says `:status :absent`, `:reason :flag-off`, `:candidate-count 147`, `:applied? false`. Their selected-policy cascade has two patterns (43-guai, 44-gou), reciprocal descent entries, empty co_app, and -0.703. The reciprocal relation must not be identified with a proved acyclic F12 cascade without an explicit interpretation. No boxes, interfaces, typed policy holes or discharged wiring are carried by their construction judgments. The -0.703 cascade score is not the controller G score and is not evidence a positive fold delta gate passed. `cascade_lane.clj` itself calls nonpositive cascade scores pattern gaps.

For H rows, selection judgment records the explicit stop-line candidate and repair reason; the ground retains the ordinary decision as contextual provenance. These have no scored pattern construction. For N rows, the payload is a sorry/not-reached cell, not an executed empty construction. A truthful admission or refusal must stay distinguishable from a task doing construction work.

Producer source explains the missing join: default `construct-selected-action` calls `cascade-lane` on the selected entry; that function constructs a pattern cascade from the target's circumstance **after** the mission/action decision. The checkpoint selects a subset of construction fields. Neither a candidate-cascade population scored by G before selection nor an immutable selected-cascade digest joined through typed holes to wiring is present in these P records. Trace paths are also persisted, but a shared trace-file path is not itself an immutable per-turn digest. This audit did not execute the strict terminal readers or reconstruct all daily trace rows; it does not claim missing checkpoints imply all daily trace information is absent.

**PROPOSED minimal new evidence:** retain the exact candidate policy/cascade representations and scores with source/model/input pins; identify which decision (G or explicit operator authority) selects which candidate; persist the selected cascade digest, typed interfaces/hole obligations and fold wiring; validate each adjacent identity/correspondence join; instantiate a theorem over that captured object with assumptions and runtime correspondence status. Preserve historical outcomes and add a separate validity assessment. New 61ad7453 and subsequent runtime gates are not retroactive proof of prior runs.

## Closing classification and PROPOSED repair directions

“(b)” means the stated mathematical/route claim survives missing construction legitimately; it does not mean this audit executed a checker on every run. “(c)” means no application to these runs is present. There are **zero confirmed (a) statements**, and a demonstrated missing design-validity chain, not an assurance that the architecture is valid.

| Attestation (individual declarations enumerated in Appendix A) | Class | PROPOSED direction |
|---|---|---|
| wmRunConformsToWiring / wmRunsOnce open proposition placeholders | c | Keep open; never display registry presence as a run proof. State full deliverable-chain criterion for future closure. |
| S5/RE5 route and census theorems; RUN4 preregistration aliases/census | c | Preserve finite claims; surface dates, trace and map pins and route-only scope. |
| Generic runConformsToDrawnWiring and runtime route battery | b | Retain route check; add separate construction-chain validity, do not rename route-green into overall conformance. |
| S5/RE5 selection discrimination; older R2/R8, find, ablation, preference and enacted/selected fixture censuses | c | Preserve exact finite population and polarity; no transfer to new runs. |
| Generic F11/F12 conformance/existence/arms and mathematical laws | b | State Lean-function scope; establish separate runtime implementation and input joins. |
| F11 applied and F12 zaif/ants/mining/snatch literal results | c | Preserve fixtures and negative results; do not use fixture equality as production correspondence. |
| Fold / escrow carrier laws | b | Strengthen future concrete fold interface to require actual deliverable structure and correspondence. |
| FoldWitness/FoldCWitness/FoldEscrowRecordWitness literals | c | Label fixture scope; no claim of RUN4 fold validity. |
| G/expected-free-energy/kernel/observation laws | b | Keep explicit hypotheses; prove or measure turn-specific bridges rather than assuming controller score = theoretical G. |
| Machine numeric readback/reference witnesses | c | Bind future runtime inputs/results to reference/domain checks; retain model-only scope. |
| RunLifecycleContractDraft | b | Keep proposed/model status; explicitly separate runtime versioned identity proof obligation. |
| ContractEmitter/CoverageReport/GainChain/registry metadata | b | No blanket holds/validated-run projection from contract metadata. |
| ChipBoardWitness / CascadeVerifierBoardWitness and their runtime statuses | c | No change; retain declared-registry assumption and runtime-correspondence not-proven. |

## Appendix A — exact source inventory and declaration signatures

All paths below are relative to `mathlib4/DarkTower/WarMachine`. Each commit is the last commit touching that file; its SHA-256 pins audited bytes. Statement signatures are whitespace-normalized source excerpts, not newly proved assertions. Proof bodies are intentionally omitted. Auxiliary theorem statements are included to avoid silently hiding negative claims. Types/definitions and comments remain available at the pinned file; the narrative above interprets the relevant ones. The list is enumeration, not a claim that every declaration is a RUN4 attestation.


### ActGateNegative.lean

Commit `af58bec5ed639b6fefc62ec230e4b29d13aeec07`; SHA-256 `59f6b0b71258459ed17a13bf9624a89629e611d8da1f9c7d5adafe1cf3d3b396`.

Declared names: none matched the top-level declaration syntax (negative elaboration fixtures may use anonymous `example`/guard commands).

### AlivenessNegative.lean

Commit `af58bec5ed639b6fefc62ec230e4b29d13aeec07`; SHA-256 `3b95b009843abf7d5118ba79aba911654c40b9724ca2dce54f401b8ae143dfb2`.

Declared names: `badFactor`.

### AmbiguityWitness.lean

Commit `3f5654ce21cd809400f0a4780acfa603e155419b`; SHA-256 `9d86334b618aaaa2c5bb43473ba91affecf3965570bf331659c963c5d87d956f`.

Declared names: `Policy`, `State`, `Observation`, `predictedState`, `observationModel`, `AmbiguityReference`, `ambiguityReference`, `pointMassAmbiguity`.

```lean
theorem pointMassAmbiguity : ambiguity predictedState observationModel .inspect = ambiguityReference.expectedAmbiguity
```

### BayesFactorThresholdNegative.lean

Commit `f0da9cbe83ae8edfed844d30ee121e3d388b3785`; SHA-256 `66eef43d0e35a5241bf89d22e49d33b216c919b7baed23532782ce3d4c5dae4c`.

Declared names: `perTickF`, `badThreshold`.

### BayesFactorThresholdWitness.lean

Commit `45f3e4eeb7fd61234ad0b5732c7c2730c8c03cc5`; SHA-256 `cc2bba1639dac8f620188d15bab7fcc3732067e8bfd498f6ba7c14c793075808`.

Declared names: `ThresholdReference`, `thresholdReference`, `substantialReductionPasses`, `weakReductionFails`.

```lean
theorem substantialReductionPasses : bayesFactorThreshold ⟨thresholdReference.passingChange⟩
theorem weakReductionFails : ¬ bayesFactorThreshold ⟨thresholdReference.failingChange⟩
```

### BayesianModelReductionNegative.lean

Commit `af58bec5ed639b6fefc62ec230e4b29d13aeec07`; SHA-256 `e1d0fb7cbccb111e6cb33e74b620f483655b8fb067ade3dbc4ef3864df26dc3e`.

Declared names: none matched the top-level declaration syntax (negative elaboration fixtures may use anonymous `example`/guard commands).

### BayesianModelReductionWitness.lean

Commit `45f3e4eeb7fd61234ad0b5732c7c2730c8c03cc5`; SHA-256 `e429d6e8a98fa83745f2d7cacf25de1d187bdedd19dbe06a6a03f03ac5fd0497`.

Declared names: `CountReductionReference`, `countReductionReference`, `countPreservingReduction`.

```lean
theorem countPreservingReduction : bayesianModelReduction countReductionReference.oldPosterior countReductionReference.reducedPrior countReductionReference.oldPrior = countReductionReference.reducedPosterior
```

### BeliefStateWitness.lean

Commit `a33f1625e18c62c48a0f53d25494ca6fb20cabbf`; SHA-256 `494d8d50b66df51d43fd504f3bc4be9176c156b7532ecbe636a6784942714061`.

Declared names: `BeliefReference`, `beliefReference`, `reference`, `recordedPosterior`.

```lean
theorem recordedPosterior (k : Channel) : reference.mean k = 1 ∧ (reference.variance k).value = 1
```

### BeliefUpdateFalsifier.lean

Commit `8078028d24528d172cc91697f84e52c42021b7e1`; SHA-256 `2ef3bc319518b468ae7af549e0e17305b593f9df7fef902d7ac69ae11fe5ec29`.

Declared names: `kernel`, `learningRate`, `sensorNoiseFloor`, `evidenceWeight`, `precision`, `prior`, `observation`, `posterior`, `unresponsiveVariance`.

### CascadeOrder.lean

Commit `9b65adc32812ff2a438e38ceb22313fcefd57e57`; SHA-256 `b69147a891a1805844d33f415267bb7923452b34490fc8d997898ea77bf052e7`.

Declared names: `Reach`, `acyclicDescent`, `Below`, `IsMeet`, `hasMeets`, `reach_increases_rank`, `acyclic_of_increasing_rank`, `Attempt001Pattern`, `attempt001Descent`, `attempt001OneWay`, `attempt001_one_way_edge`, `attempt001_2026_07_14_two_cycle_is_refused`, `attempt001_one_way_reach`, `dropping_reverse_edge_is_acyclic_but_not_a_semilattice`, `Attempt008Pattern`, `attempt008Descent`, `attempt008_2026_07_16_descent_is_accepted`.

```lean
theorem reach_increases_rank {α : Type u} (r : α → α → Prop) (rank : α → Nat) (edge_increases : ∀ a b, r a b → rank a < rank b) {a b : α} (path : Reach r a b) : rank a < rank b
theorem acyclic_of_increasing_rank {α : Type u} (r : α → α → Prop) (rank : α → Nat) (edge_increases : ∀ a b, r a b → rank a < rank b) : acyclicDescent r
theorem attempt001_one_way_edge {a b : Attempt001Pattern} (edge : attempt001OneWay a b) : a = hexagram43Guai ∧ b = hexagram44Gou
theorem attempt001_2026_07_14_two_cycle_is_refused : ¬ acyclicDescent attempt001Descent
theorem attempt001_one_way_reach {a b : Attempt001Pattern} (path : Reach attempt001OneWay a b) : a = hexagram43Guai ∧ b = hexagram44Gou
theorem dropping_reverse_edge_is_acyclic_but_not_a_semilattice : acyclicDescent attempt001OneWay ∧ ¬ hasMeets attempt001OneWay
theorem attempt008_2026_07_16_descent_is_accepted : acyclicDescent attempt008Descent
```

### CascadeVerifierBoardWitness.lean

Commit `e407ec20cbedb8667070dee89cd45f27a52207a0`; SHA-256 `fe9d349a0c4fb92b2f9d329a41e94c8ace769e9cc42c725d62067a76c95b9a25`.

Declared names: `Verb`, `Effect`, `Chip`, `Row`, `Registry`, `allowed`, `declared`, `board`, `NoZap`, `boardHasNoZap`, `declaredEffectsAllowed`, `Executes`, `traceEffectsAllowed`, `cascadeHasNoActEffects`, `replacedRegistry`, `replacementRow`, `noZapAloneIsInsufficient`, `boardDigest`, `firstRequest`, `requestPointer`, `requestBasis`, `debtCount`, `fixtureRows`, `fixtureExecutes`, `followsWires`, `fixtureWired`, `fixtureEffectsAllowed`, `deltas`, `fixtureDeltasZero`, `verbTag`, `effectTag`.

```lean
theorem boardHasNoZap : NoZap board
theorem declaredEffectsAllowed (chips : List Chip) (h : NoZap chips) (c : Chip) (hc : c ∈ chips) (branch : Bool) (e : Effect) (he : e ∈ declared c.verb branch) : allowed e
theorem traceEffectsAllowed (chips : List Chip) (h : NoZap chips) (trace : List Row) (run : Executes declared chips trace) : ∀ row ∈ trace, ∀ e ∈ row.effects, allowed e
theorem cascadeHasNoActEffects (trace : List Row) (run : Executes declared board trace) : ∀ row ∈ trace, Effect.commit ∉ row.effects ∧ Effect.zap ∉ row.effects
theorem noZapAloneIsInsufficient : NoZap board ∧ Executes replacedRegistry board [replacementRow] ∧ Effect.commit ∈ replacementRow.effects
theorem fixtureExecutes : Executes declared board fixtureRows
theorem fixtureWired : followsWires fixtureRows = true
theorem fixtureEffectsAllowed : ∀ row ∈ fixtureRows, ∀ e ∈ row.effects, allowed e
theorem fixtureDeltasZero : deltas = [0, 0, 0, 0, 0]
```

### ChannelWitness.lean

Commit `5df605d2529d4b21cdcc949f0138cf2bb36dc938`; SHA-256 `a0f500f7319e8cfae5b07edc1445b3d4dcaa862cf19a964e731816b946bdea21`.

Declared names: `declaredVocabulary`.

```lean
theorem declaredVocabulary : Channel.all = [.loopHealth, .supportCoverage, .attackCoverage, .missionHealth, .stackPct, .consultingPct, .portfolioPct, .mathematicsPct, .activeRepoRatio, .sorryCountNorm, .couplingDensity, .ticksFiringRatio, .depositingSignal, .annotationHealth]
```

### ChipBoardWitness.lean

Commit `a9a24a3b9e070550ede41ebd10621c5bb9b843f0`; SHA-256 `8861f52c71951c7857fd38d6a4026f24be853293ca695937fe1fadea91323be1`.

Declared names: `Repo`, `Verb`, `Effect`, `Chip`, `Board`, `Row`, `Terminal`, `End`, `Certificate`, `observation`, `wire`, `checkTwoWires`, `checkedObservationHasEitherWire`, `endAt`, `terminalBeforeCaps`, `stepCapBeforeFuel`, `fuelEnd`, `negativeFuelDoesNotExhaust`, `Step`, `Executes`, `HasFeel`, `Certified`, `NamedSafe`, `hasFeel_append`, `stepPreservesCertified`, `stepNamedCommit`, `executorNamedHazard`, `nilZap`, `nilZapExecutesWithoutFeel`, `replayView`, `forgedZap`, `replayDoesNotCertifyEffects`, `boardDigest`, `inputsDigest`, `fixtureRows`, `fixtureBoard`, `followsWires`, `fixtureFollowsWires`, `fixtureTwoWires`, `fixture`, `fixtureExecutes`, `fixtureNamedHazard`, `fixtureDeltas`, `fixtureDeltasZero`, `verbTag`, `effectTag`, `rowLine`, `chipLine`, `Step`, `Step.original`, `Step.noNilCommit`, `Executes`, `Safe`, `repairedHazard`, `nilZapNowImpossible`, `nilRefusal`, `nilNowRefuses`, `replayView`, `replayCertifiesModeledEffects`, `alteredEffectsNowRefuse`, `compareMove`, `compareMoveReference`, `fixtureStillExecutes`, `fixtureFullHazard`.

```lean
theorem checkedObservationHasEitherWire (b : Board) (h : checkTwoWires b = true) (c : Chip) (hc : c ∈ b.chips) (ho : observation c.verb = true) (arm : Bool) : ∃ target, wire c arm = some target
theorem terminalBeforeCaps : endAt (some .yield) 64 64 0 = some (.terminal .yield)
theorem stepCapBeforeFuel : endAt none 64 64 0 = some .stepCap
theorem fuelEnd : endAt none 3 64 0 = some .fuelExhausted
theorem negativeFuelDoesNotExhaust : endAt none 3 64 (-1) = none
lemma hasFeel_append {trace : List Row} {r : Repo} (h : HasFeel trace r) (row : Row) : HasFeel (trace ++ [row]) r
lemma stepPreservesCertified {s t : Repo} {trace : List Row} {row : Row} (h : Certified s trace) (step : Step s row t) : Certified t (trace ++ [row])
lemma stepNamedCommit {s t : Repo} {trace : List Row} {row : Row} (h : Certified s trace) (step : Step s row t) : ∀ r : String, Effect.commit (some r) ∈ row.effects → HasFeel trace (some r)
theorem executorNamedHazard {s : Repo} {trace : List Row} (h : Executes s trace) : Certified s trace ∧ NamedSafe trace
theorem nilZapExecutesWithoutFeel : Step none nilZap none ∧ Effect.commit none ∈ nilZap.effects ∧ ¬ HasFeel [] none
theorem replayDoesNotCertifyEffects : replayView nilZap = replayView forgedZap ∧ nilZap.effects ≠ forgedZap.effects
theorem fixtureFollowsWires : fixtureRows.head?.map Row.chip = some fixtureBoard.entry ∧ followsWires fixtureBoard fixtureRows = true
theorem fixtureTwoWires : checkTwoWires fixtureBoard = true
theorem fixtureExecutes : Executes (some "futon5a") fixtureRows
theorem fixtureNamedHazard : NamedSafe fixtureRows
theorem fixtureDeltasZero : fixtureDeltas = [0, 0, 0, 0, 0]
lemma Step.original {s t : Repo} {row : Row} (h : Step s row t) : ChipBoardWitness.Step s row t
lemma Step.noNilCommit {s t : Repo} {row : Row} (h : Step s row t) : Effect.commit none ∉ row.effects
theorem repairedHazard {s : Repo} {trace : List Row} (run : Executes s trace) : Certified s trace ∧ Safe trace
theorem nilZapNowImpossible : ¬ Step none nilZap none
theorem nilNowRefuses : Step none nilRefusal none
theorem replayCertifiesModeledEffects (a b : Row) (h : replayView a = replayView b) : a.effects = b.effects
theorem alteredEffectsNowRefuse : replayView nilZap ≠ replayView forgedZap
theorem compareMoveReference : compareMove ["forward"] (some "forward") = true ∧ compareMove ["forward"] (some "back") = false ∧ compareMove [] none = true ∧ compareMove ["forward"] none = false
theorem fixtureStillExecutes : Executes (some "futon5a") fixtureRows
theorem fixtureFullHazard : Safe fixtureRows
```

### CohortNegative.lean

Commit `af58bec5ed639b6fefc62ec230e4b29d13aeec07`; SHA-256 `781e4df61b44145e46592b63cfe8e10fd28e6d40053f85216a26261b4248e25e`.

Declared names: `bad`.

### CommitmentTemperature.lean

Commit `b78ebc428b74fb99fd83349d6c86dfa192707bc6`; SHA-256 `4b32b550aacaa72807690aa53cad2e6e38571683f82cd29df38bbcf1625937ee`.

Declared names: `Action`, `Entry`, `Temperature`, `Selector`, `governs`, `SelectionRecord`, `RecordEmitter`, `governsTheRecord`, `temperatureValue`, `commitmentScore`, `argmaxBy`, `modeOnly`, `argmaxScore`, `habitPrior`, `cautiousAction`, `habitualAction`, `switchingEntries`, `liveRecord`, `temperatureInvariant`, `mode_only_ignores_temperature`, `live_selector_does_not_govern`, `commitmentScore_zero_prior`, `argmax_score_temperature_invariant`, `single_term_argmax_annihilates_temperature`, `repairing_r8_changes_no_action`, `live_gain_repair_changes_no_action`, `default_branch_gain_repair_changes_no_action`, `record_sensitivity_is_not_governance`, `scores_move_action_does_not`, `habit_prior_governs`, `factorsThroughDiscard`, `factorsThroughDiscard_iff_temperatureInvariant`, `not_governs_iff_factorsThroughDiscard`, `discard_absorbs_upstream`, `live_selector_factors_through_discard`.

```lean
theorem mode_only_ignores_temperature : ∀ τ₁ τ₂ entries, modeOnly τ₁ entries = modeOnly τ₂ entries
theorem live_selector_does_not_govern : ¬ governs modeOnly
theorem commitmentScore_zero_prior (τ : Temperature) (entry : Entry) : commitmentScore τ { entry with l
theorem argmax_score_temperature_invariant : temperatureInvariant argmaxScore
theorem single_term_argmax_annihilates_temperature : ¬ governs argmaxScore
theorem repairing_r8_changes_no_action (s : Selector) (hs : temperatureInvariant s) (effectiveTemperature : Nat → Temperature) (g₁ g₂ : Nat) (entries : List Entry) : s (effectiveTemperature g₁) entries = s (effectiveTemperature g₂) entries
theorem live_gain_repair_changes_no_action (effectiveTemperature : Nat → Temperature) (g₁ g₂ : Nat) (entries : List Entry) : modeOnly (effectiveTemperature g₁) entries = modeOnly (effectiveTemperature g₂) entries
theorem default_branch_gain_repair_changes_no_action (effectiveTemperature : Nat → Temperature) (g₁ g₂ : Nat) (entries : List Entry) : argmaxScore (effectiveTemperature g₁) entries = argmaxScore (effectiveTemperature g₂) entries
theorem record_sensitivity_is_not_governance : governsTheRecord liveRecord ∧ ¬ governs modeOnly
theorem scores_move_action_does_not : (liveRecord 0 switchingEntries).reportedScores ≠ (liveRecord 2 switchingEntries).reportedScores ∧ (liveRecord 0 switchingEntries).action = (liveRecord 2 switchingEntries).action
theorem habit_prior_governs : governs habitPrior
theorem factorsThroughDiscard_iff_temperatureInvariant (s : Selector) : factorsThroughDiscard s ↔ temperatureInvariant s
theorem not_governs_iff_factorsThroughDiscard (s : Selector) : ¬ governs s ↔ factorsThroughDiscard s
theorem discard_absorbs_upstream (s : Selector) (h : factorsThroughDiscard s) (effectiveTemperature : Nat → Temperature) : ∃ c : List Entry → Option Action, ∀ g entries, s (effectiveTemperature g) entries = c entries
theorem live_selector_factors_through_discard : factorsThroughDiscard modeOnly
```

### ContractEmitter.lean

Commit `4f399fd05436a9de9b4d3d30ed79900e10e054e9`; SHA-256 `ef2fb515642592a6b8952d277ed1c62205f9591e88dc1882137eec9d84b10a8e`.

Declared names: `stringArray`, `predName`, `predNames`, `predArray`, `familyJson`, `familiesJson`, `chainPropertyJson`, `compliancePropertyJson`, `coverageClauseJson`, `policyGradeClauseJson`, `OutsideReason`, `outsideReasonName`, `reservedJson'`, `reservedJson`, `contractJson`, `emit`, `main`.

### ControlVocabularyNegative.lean

Commit `af58bec5ed639b6fefc62ec230e4b29d13aeec07`; SHA-256 `c3ce20d7568e82ca2843f36901edc1d5e8f8d0413edaf854547c754ed2578fe8`.

Declared names: `Control`, `U`, `bad`.

### CoverageReport.lean

Commit `b78ebc428b74fb99fd83349d6c86dfa192707bc6`; SHA-256 `dc85b66f6217c6f395c7d6dd81e2d4337f7919c3dc665bc1942b7b45698bb937`.

Declared names: `CriterionSet`, `Report`, `declaresCoverage`, `criterionSelection`, `reportOccurrence`, `outsideIsTyped`, `coverageReported`, `Verdict`, `markWithoutForce`, `CustomerOutcome`, `customerCriteria`, `silentCustomerEvaluation`, `warm_customer_pays_uncovered_and_unrecorded_is_refused`, `oneChannelLarger`, `oneChannelLargerEvaluation`, `adding_a_channel_does_not_satisfy_coverage`, `ProblemOutcome`, `InstrumentFinding`, `defectiveVerdict`, `voidProblem`, `void_retains_the_instrument_finding`, `reportingEvaluation`, `coverage_reported_nonvacuous`.

```lean
theorem warm_customer_pays_uncovered_and_unrecorded_is_refused : ¬ coverageReported customerCriteria silentCustomerEvaluation
theorem adding_a_channel_does_not_satisfy_coverage : ¬ outsideIsTyped oneChannelLarger oneChannelLargerEvaluation
theorem void_retains_the_instrument_finding : markWithoutForce voidProblem defectiveVerdict
theorem coverage_reported_nonvacuous : coverageReported customerCriteria reportingEvaluation
```

### DirichletConcentrationsEmptyNegative.lean

Commit `af58bec5ed639b6fefc62ec230e4b29d13aeec07`; SHA-256 `eab731c363d979905244e6eaef3fd564c777253fd346d5d7b7aeccd71a580121`.

Declared names: `emptyConcentrations`.

### DirichletConcentrationsNegative.lean

Commit `af58bec5ed639b6fefc62ec230e4b29d13aeec07`; SHA-256 `ae69f5b71601ec31474050d99b2f1359a066426d070ccbe629dcf57f822216ed`.

Declared names: `negativeConcentration`.

### DirichletConcentrationsWitness.lean

Commit `e924102c3b5df57d0e6c848b5b7256350c198e42`; SHA-256 `25de7ff72809e8a14afe44aa7340276c71883fed0ed94f653bac289753bcc2d1`.

Declared names: `alpha21`, `recordedValues`.

```lean
theorem recordedValues : alpha21.val = [2, 1]
```

### DirichletConcentrationsZeroNegative.lean

Commit `af58bec5ed639b6fefc62ec230e4b29d13aeec07`; SHA-256 `90fd6f7df9fe306994a19cabc3144e96de714d6936768b6dac3a41c0798781c2`.

Declared names: `zeroConcentration`.

### ExpectedFreeEnergyWitness.lean

Commit `3f5654ce21cd809400f0a4780acfa603e155419b`; SHA-256 `78bb25d82153da064271614aad4c7a1d83d8905f79ea0d0a40b4033d0c4fdcfa`.

Declared names: `TestObservation`, `TestPolicy`, `datum`, `Q`, `Cdist`, `positivePreference`, `EFEReference`, `efeReference`, `onePointFixture`, `decompositionFixture`.

```lean
private theorem positivePreference : ∀ π o, o ∈ Q.support π → 0 < Cdist.mass () o
theorem onePointFixture : expectedFreeEnergy Q Cdist positivePreference (fun _ => efeReference.ambiguity) .inspect = ⟨efeReference.expectedFreeEnergy⟩
theorem decompositionFixture : G (fun _ : TestPolicy => efeReference.risk) (fun _ => efeReference.epistemicGain) .inspect = expectedFreeEnergy Q Cdist positivePreference (fun _ => efeReference.ambiguity) .inspect
```

### ExpectedInformationGainWitness.lean

Commit `3f5654ce21cd809400f0a4780acfa603e155419b`; SHA-256 `3efcca86105ba7ad4a33a12009bd538f621cbf867aa4d481235b6ba31d0d5d65`.

Declared names: `TestObservation`, `TestPolicy`, `Parameter`, `outcome`, `Q`, `prior`, `posterior`, `positivePrior`, `EIGReference`, `eigReference`, `binaryFixture`.

```lean
private theorem positivePrior : ∀ π o θ, θ ∈ posterior.support (π, o) → 0 < prior.mass π θ
theorem binaryFixture : expectedInformationGain Q prior posterior positivePrior .inspect = ⟨eigReference.expectedInformationGain⟩
```

### F10RuledCarrier.lean

Commit `fcd1261c303c2beca08a6812eba4a7ce2e83d722`; SHA-256 `f43c36fa3b1fc03efde981c118f5688af836700c050cf3fd948a949d9662ff34`.

Declared names: `FlightDisposition`, `FlightDisposition.all`, `FlightDisposition.mem_all`, `NounObservation`, `VerbObservation`, `SeedEvidenceObservation`, `Obs`, `SeedObs`, `organisationOutcome`, `observedDispositions`, `namedZeroDispositions`, `serendipityOccasionSupport`, `seed`, `seedMass_zero_of_mem_namedZeros`, `seedMass_pos_of_mem_observed`, `PositivePreferenceOnSupport`, `seed_positivePreference_iff_support_avoids_namedZeros`, `exists_ruledPreferenceDistribution`.

```lean
theorem FlightDisposition.mem_all (d : FlightDisposition) : d ∈ FlightDisposition.all
theorem seedMass_zero_of_mem_namedZeros (d : FlightDisposition) (hd : d ∈ namedZeroDispositions) : seed.mass () (organisationOutcome d) = 0
theorem seedMass_pos_of_mem_observed (d : FlightDisposition) (hd : d ∈ observedDispositions) : 0 < seed.mass () (organisationOutcome d)
theorem seed_positivePreference_iff_support_avoids_namedZeros {PolicyIndex : Type*} (Q : PredictiveOutcomeKernel PolicyIndex SeedObs) : PositivePreferenceOnSupport Q seed ↔ ∀ π d, organisationOutcome d ∈ Q.support π → d ∉ namedZeroDispositions
theorem exists_ruledPreferenceDistribution : ∃ _ : PreferenceDistribution SeedObs, True
```

### F11AmendedCarrier.lean

Commit `65d15a8d569b2409c2b14a43342b96536dbfe7c7`; SHA-256 `3eca0148b51fa0555c46deb6b0538818f58ec6ce45bb41acf2828dd7f8a0b189`.

Declared names: `AmendedReceipt`, `AmendedFindResult`, `AmendedFindType`, `ConformantAmendedFind`, `AmendedFindResult.erase`, `eraseAmendedFinder`, `amendedF4ImpliesRecordedReadingB`, `amendedF4ImpliesReadingC`, `recordedReadingBImpliesAmendedF4AtRecord`, `recordedReadingCImpliesAmendedF4AtRecord`, `amendedFaithful`, `amendedMisattributing`, `amendedFaithfulConformant`, `eraseAmendedFaithful_selected`, `amendedF4DoesNotImplyReadingA`, `amendedFaithfulReadingB`, `amendedFaithfulReadingC`, `amendedAllButBad`, `amendedAllButBadSelectionNonempty`, `readingADoesNotImplyAmendedF4`, `amendedFaithfulSelectionNonempty`, `amendedMisattributingSelectionNonempty`, `amendedMisattributingNotConformant`, `amendedReceiptDataSeparates`, `amendedDifferentZeroMass`, `amendedDifferentZeroMassSelectionNonempty`, `amendedDifferentZeroMassConformant`, `amendedZeroMassSeparates`, `amendedReceiptPairAgreesOnZeroMass`, `amendedZeroMassPairAgreesOnReceipts`, `amendedRecordedDesignationNonempty`, `amendedRefusing`, `amendedRefusingConformant`, `amendedRefusingRecordedDesignationNonempty`, `amendedReceiptPairErasuresAreEqual`, `amendedZeroMassPairErasuresAreEqual`, `amendedIdentity`, `amendedIdentityConformant`, `amendedIdentitySelectionNonempty`, `amendedIdentityNotReadingA`, `amendedIdentityNotReadingB`, `amendedIdentityNotReadingC`, `amendedF4AloneBuysNoReadingOfF4`.

```lean
theorem amendedF4ImpliesRecordedReadingB {Route AsOf : Type*} {f : AmendedFindType FindSnatchScenario SnatchPattern SnatchPattern Route AsOf} (hf : ConformantAmendedFind f) (hz : ∀ t, (f t findSnatchRepository).zeroMass = findSnatchZeroMassSet t.context) : FindExcludesRecordedZeroMass (eraseAmendedFinder f)
theorem amendedF4ImpliesReadingC {State P Route AsOf : Type*} {zm : State → Set P} {f : AmendedFindType State P P Route AsOf} (hf : ConformantAmendedFind f) (hz : ∀ t repo, (f t repo).zeroMass = zm t.context) : FindRespectsZeroMass zm (eraseAmendedFinder f)
theorem recordedReadingBImpliesAmendedF4AtRecord {Route AsOf : Type*} {f : AmendedFindType FindSnatchScenario SnatchPattern SnatchPattern Route AsOf} (hz : ∀ t, (f t findSnatchRepository).zeroMass = findSnatchZeroMassSet t.context) (hb : FindExcludesRecordedZeroMass (eraseAmendedFinder f)) : ∀ t p, p ∈ (f t findSnatchRepository).zeroMass → p ∈ findSnatchRepository.patterns ∧ p ∉ (f t findSnatchRepository).selected
theorem recordedReadingCImpliesAmendedF4AtRecord {Route AsOf : Type*} {f : AmendedFindType FindSnatchScenario SnatchPattern SnatchPattern Route AsOf} (hz : ∀ t, (f t findSnatchRepository).zeroMass = findSnatchZeroMassSet t.context) (hc : FindRespectsZeroMass findSnatchZeroMassSet (eraseAmendedFinder f)) : ∀ t p, p ∈ (f t findSnatchRepository).zeroMass → p ∈ findSnatchRepository.patterns ∧ p ∉ (f t findSnatchRepository).selected
theorem amendedFaithfulConformant : ConformantAmendedFind amendedFaithful
theorem eraseAmendedFaithful_selected (t) (repo) : (eraseAmendedFinder amendedFaithful t repo).selected = (findSnatchReplay t repo).selected
theorem amendedF4DoesNotImplyReadingA : ConformantAmendedFind amendedFaithful ∧ ¬ FindFalsifiable (eraseAmendedFinder amendedFaithful)
theorem amendedFaithfulReadingB : FindExcludesRecordedZeroMass (eraseAmendedFinder amendedFaithful)
theorem amendedFaithfulReadingC : FindRespectsZeroMass findSnatchZeroMassSet (eraseAmendedFinder amendedFaithful)
theorem amendedAllButBadSelectionNonempty : ((amendedAllButBad .consultTheRemedyBeforeExiting { context
theorem readingADoesNotImplyAmendedF4 : FindFalsifiable (eraseAmendedFinder (amendedAllButBad .consultTheRemedyBeforeExiting)) ∧ ¬ ConformantAmendedFind (amendedAllButBad .consultTheRemedyBeforeExiting)
theorem amendedFaithfulSelectionNonempty : ((amendedFaithful { context
theorem amendedMisattributingSelectionNonempty : ((amendedMisattributing { context
theorem amendedMisattributingNotConformant : ¬ ConformantAmendedFind amendedMisattributing
theorem amendedReceiptDataSeparates : amendedFaithful ≠ amendedMisattributing
theorem amendedDifferentZeroMassSelectionNonempty : ((amendedDifferentZeroMass { context
theorem amendedDifferentZeroMassConformant : ConformantAmendedFind amendedDifferentZeroMass
theorem amendedZeroMassSeparates : amendedFaithful ≠ amendedDifferentZeroMass
theorem amendedReceiptPairAgreesOnZeroMass (t) (repo) : (amendedFaithful t repo).zeroMass = (amendedMisattributing t repo).zeroMass
theorem amendedZeroMassPairAgreesOnReceipts (t) (repo) : (amendedFaithful t repo).receipts = (amendedDifferentZeroMass t repo).receipts
theorem amendedRecordedDesignationNonempty : ((amendedFaithful { context
theorem amendedRefusingConformant {State P : Type*} : ConformantAmendedFind (amendedRefusing (State
theorem amendedRefusingRecordedDesignationNonempty : ((amendedRefusing { context
theorem amendedReceiptPairErasuresAreEqual : eraseAmendedFinder amendedFaithful = eraseAmendedFinder amendedMisattributing
theorem amendedZeroMassPairErasuresAreEqual : eraseAmendedFinder amendedFaithful = eraseAmendedFinder amendedDifferentZeroMass
theorem amendedIdentityConformant : ConformantAmendedFind amendedIdentity
theorem amendedIdentitySelectionNonempty : ((amendedIdentity { context
theorem amendedIdentityNotReadingA : ¬ FindFalsifiable (eraseAmendedFinder amendedIdentity)
theorem amendedIdentityNotReadingB : ¬ FindExcludesRecordedZeroMass (eraseAmendedFinder amendedIdentity)
theorem amendedIdentityNotReadingC : ¬ FindRespectsZeroMass findSnatchZeroMassSet (eraseAmendedFinder amendedIdentity)
theorem amendedF4AloneBuysNoReadingOfF4 : ConformantAmendedFind amendedIdentity ∧ ¬ FindFalsifiable (eraseAmendedFinder amendedIdentity) ∧ ¬ FindExcludesRecordedZeroMass (eraseAmendedFinder amendedIdentity) ∧ ¬ FindRespectsZeroMass findSnatchZeroMassSet (eraseAmendedFinder amendedIdentity)
```

### F11AppliedConformance.lean

Commit `ab8e4b8762dd10487b018ac5db5053d19ee861dd`; SHA-256 `c1fcffc1d033a61b1cdf98ebb0e6d4ac52e875417806ce6618a646915a5a1759`.

Declared names: `Pattern`, `Clause`, `SourcePin`, `AsOf`, `TextSpan`, `TextCitation`, `State`, `eligible`, `emittedClauses`, `emittedCitations`, `expectedClauses`, `authoredCitations`, `expected`, `validText`, `model`, `modelContainment`, `modelTypedAbsence`, `modelContentF2`, `modelCitationF3`, `modelClausesNonempty`, `repository`, `recordedStamp`, `recordedState`, `recordedQuery`, `designation`, `modelSelectsAsk`, `modelNonempty`, `modelExclusionF4`, `modelDiscriminatingF4`, `modelVacuous`, `modelVacuousEarnsNothing`, `conformantImplementationExists`, `badModel`, `badModelRefuted`.

```lean
theorem modelContainment (q : FindQuery State Pattern) (R : Repository Pattern) : (model q R).selected ⊆ R.patterns
theorem modelTypedAbsence (q : FindQuery State Pattern) (R : Repository Pattern) (h : (model q R).selected = ∅) : (model q R).absence = some .noPatternAddressesThisTension
theorem modelContentF2 (q : FindQuery State Pattern) (R : Repository Pattern) : (model q R).contentF2 (expected q.tension.context)
theorem modelCitationF3 (q : FindQuery State Pattern) (R : Repository Pattern) : (model q R).citationF3 validText
theorem modelClausesNonempty (p : Pattern) : emittedClauses p ≠ []
theorem modelSelectsAsk : Pattern.ask ∈ (model recordedQuery repository).selected
theorem modelNonempty : (model recordedQuery repository).selected.Nonempty
theorem modelExclusionF4 (R : Repository Pattern) : (model recordedQuery R).exclusionF4 designation
theorem modelDiscriminatingF4 : (model recordedQuery repository).discriminatingF4 designation
theorem modelVacuous : (model recordedQuery repository).exclusionF4 ∅
theorem modelVacuousEarnsNothing : ¬ (model recordedQuery repository).discriminatingF4 ∅
theorem conformantImplementationExists : ∃ f : (q : FindQuery State Pattern) → (R : Repository Pattern) → FindResult Pattern Clause AsOf TextCitation R, (∀ q R, (f q R).contentF2 (expected q.tension.context)) ∧ (∀ q R, (f q R).citationF3 validText) ∧ (f recordedQuery repository).discriminatingF4 designation ∧ (f recordedQuery repository).selected.Nonempty
theorem badModelRefuted : ¬ badModel.exclusionF4 designation
```

### F11CitationField.lean

Commit `65d15a8d569b2409c2b14a43342b96536dbfe7c7`; SHA-256 `86ef5bc54cfb1df15e7690ae47ee6161b9bc796b3b7623740a526cfec840c4e5`.

Declared names: `AmendedCitesSelected`, `amendedFaithful_citesSelected`, `amendedMisattributing_not_citesSelected`, `amendedCitationPair_nonempty`, `amendedCitationBuy`, `DualReceipt`, `DualResult`, `DualFind`, `eraseDual`, `DualAcknowledgesSelected`, `DualCitesSelected`, `dualReceipt`, `dualFinder`, `dualFinders_nonempty`, `citation_separates_without_clause`, `clause_separates_without_citation`, `DualContentF2`, `dualAcknowledgesIsContentF2AtId`, `clauseFailureIsRelativeToTheExpectation`, `dualPairErasuresEqual`.

```lean
theorem amendedFaithful_citesSelected : AmendedCitesSelected amendedFaithful
theorem amendedMisattributing_not_citesSelected : ¬ AmendedCitesSelected amendedMisattributing
theorem amendedCitationPair_nonempty : (amendedFaithful { context
theorem amendedCitationBuy : amendedFaithful ≠ amendedMisattributing ∧ eraseAmendedFinder amendedFaithful = eraseAmendedFinder amendedMisattributing
theorem dualFinders_nonempty : ∀ mode : Fin 3, (dualFinder mode { context
theorem citation_separates_without_clause : DualAcknowledgesSelected (dualFinder 0) ∧ DualAcknowledgesSelected (dualFinder 1) ∧ DualCitesSelected (dualFinder 0) ∧ ¬ DualCitesSelected (dualFinder 1)
theorem clause_separates_without_citation : DualCitesSelected (dualFinder 0) ∧ DualCitesSelected (dualFinder 2) ∧ DualAcknowledgesSelected (dualFinder 0) ∧ ¬ DualAcknowledgesSelected (dualFinder 2)
theorem dualAcknowledgesIsContentF2AtId (f : DualFind) : DualAcknowledgesSelected f ↔ DualContentF2 f id
theorem clauseFailureIsRelativeToTheExpectation : DualContentF2 (dualFinder 2) misattributedReceiptOwner ∧ ¬ DualContentF2 (dualFinder 2) id ∧ DualCitesSelected (dualFinder 2)
theorem dualPairErasuresEqual : eraseDual (dualFinder 0) = eraseDual (dualFinder 1) ∧ eraseDual (dualFinder 0) = eraseDual (dualFinder 2)
```

### F11Conformance.lean

Commit `65d15a8d569b2409c2b14a43342b96536dbfe7c7`; SHA-256 `fb48e0c7b74bd323ba73b18d696ceb91ffc3632813fd5ca5dcc40800c6908102`.

Declared names: `FindType`, `ConformantFind`, `FindFalsifiable`, `findSnatchRepository`, `findSnatchSelected`, `findSnatchReceipted`, `findSnatchSelectedSubsetReceipted`, `findStructuredReceipt`, `findSnatchReplay`, `findSnatchReplayConformant`, `findSnatchReplayReceiptsArePartial`, `findRefusing`, `findRefusingConformant`, `findRefusingFalsifiable`, `findIdentity`, `findIdentityConformant`, `findIdentityNotFalsifiable`, `findSnatchReplayExcludesDeclaredZeroMass`, `findSnatchZeroMassNonempty`, `findSnatchReplayFalsifiableOnRecordedRepository`, `findSnatchReplayNotFalsifiable`, `findSnatchReplaySelectsNamedPattern`, `findConformantImplementationsDifferOnSnatch`.

```lean
theorem findSnatchSelectedSubsetReceipted (scenario : FindSnatchScenario) : findSnatchSelected scenario ⊆ findSnatchReceipted scenario
theorem findSnatchReplayConformant : ConformantFind findSnatchReplay
theorem findSnatchReplayReceiptsArePartial : (findSnatchReplay { context
theorem findRefusingConformant {State P : Type*} : ConformantFind (findRefusing (State
theorem findRefusingFalsifiable {State P : Type*} : FindFalsifiable (findRefusing (State
theorem findIdentityConformant {State P : Type*} : ConformantFind (findIdentity (State
theorem findIdentityNotFalsifiable : ¬ FindFalsifiable (findIdentity (State
theorem findSnatchReplayExcludesDeclaredZeroMass (t : Tension FindSnatchScenario) : ∀ p ∈ findSnatchZeroMass t.context, p ∈ findSnatchRepository.patterns ∧ p ∉ (findSnatchReplay t findSnatchRepository).selected
theorem findSnatchZeroMassNonempty (scenario : FindSnatchScenario) : (findSnatchZeroMass scenario) ≠ []
theorem findSnatchReplayFalsifiableOnRecordedRepository (t : Tension FindSnatchScenario) : ∃ p ∈ findSnatchRepository.patterns, p ∉ (findSnatchReplay t findSnatchRepository).selected
theorem findSnatchReplayNotFalsifiable : ¬ FindFalsifiable findSnatchReplay
theorem findSnatchReplaySelectsNamedPattern (want however : Prop) : SnatchPattern.askForSurplusNotSurrender ∈ (findSnatchReplay { context
theorem findConformantImplementationsDifferOnSnatch : (findSnatchReplay { context
```

### F11DischargeArm.lean

Commit `65d15a8d569b2409c2b14a43342b96536dbfe7c7`; SHA-256 `f6bde288bfb76c77dec0d2d3ce886cfdfd3fd87324002e302f43206c05616462`.

Declared names: `findTypeNonempty`, `findRefusingMeetsConformanceAndReadingA`, `findSilent`, `findSilentNotConformant`, `findSnatchRepositoryNonempty`, `findDischargeExists`, `findDischargeExistsReadingA`, `FindExcludesRecordedZeroMass`, `findDischargeExistsReadingB`, `findAllBut`, `findAllButConformant`, `findAllButFalsifiable`, `findDischargeNotUniqueReadingA`, `findRefusingExcludesRecordedZeroMass`, `findDischargeNotUniqueReadingB`, `findReadingBWitnessesAgreeOnFloor`, `findReadingAWitnessesAgreeOnFloor`, `findAllButFailsReadingB`, `findAllButPairDisagreesExactlyOnTheTwoZeroMassMembers`, `findOpaqueRefusing`, `findOpaqueNoBody`.

```lean
theorem findTypeNonempty {State P : Type*} : Nonempty (FindType State P)
theorem findRefusingMeetsConformanceAndReadingA {State P : Type*} : ConformantFind (findRefusing (State
theorem findSilentNotConformant : ¬ ConformantFind (findSilent (State
theorem findSnatchRepositoryNonempty : findSnatchRepository.patterns.Nonempty
theorem findDischargeExists {State P : Type*} : ∃ f : FindType State P, ConformantFind f
theorem findDischargeExistsReadingA {State P : Type*} : ∃ f : FindType State P, ConformantFind f ∧ FindFalsifiable f
theorem findDischargeExistsReadingB : ∃ f : FindType FindSnatchScenario SnatchPattern, ConformantFind f ∧ FindExcludesRecordedZeroMass f
theorem findAllButConformant {State P : Type*} (q : P) : ConformantFind (findAllBut (State
theorem findAllButFalsifiable {State P : Type*} (q : P) : FindFalsifiable (findAllBut (State
theorem findDischargeNotUniqueReadingA : ∃ f g : FindType FindSnatchScenario SnatchPattern, ConformantFind f ∧ FindFalsifiable f ∧ ConformantFind g ∧ FindFalsifiable g ∧ (f { context
theorem findRefusingExcludesRecordedZeroMass : FindExcludesRecordedZeroMass (findRefusing (State
theorem findDischargeNotUniqueReadingB : ∃ f g : FindType FindSnatchScenario SnatchPattern, ConformantFind f ∧ FindExcludesRecordedZeroMass f ∧ ConformantFind g ∧ FindExcludesRecordedZeroMass g ∧ (f { context
theorem findReadingBWitnessesAgreeOnFloor : FindExcludesRecordedZeroMass findSnatchReplay ∧ FindExcludesRecordedZeroMass (findRefusing (State
theorem findReadingAWitnessesAgreeOnFloor : let q
theorem findAllButFailsReadingB : ¬ FindExcludesRecordedZeroMass (findAllBut (State
theorem findAllButPairDisagreesExactlyOnTheTwoZeroMassMembers : let q
opaque findOpaqueRefusing : FindType Unit SnatchPattern
opaque findOpaqueNoBody : FindType Unit SnatchPattern end end DarkTower.WarMachine.Holes
```

### F11F4Reading.lean

Commit `7060ad361ef1e87ffb8e67266d01d217b97b0ae8`; SHA-256 `3650ab7e8d9f8e6bb2b42aef0b1bfd8bb3a09e0e1257d0c0397f313ab14ec728`.

Declared names: `FindRespectsZeroMass`, `findSnatchZeroMassSet`, `findReadingCImpliesReadingB`, `findSnatchReplayRespectsRecordedZeroMass`, `findReplayRecordedElseIdentity`, `findG1ZeroMassSingleton`, `findReadingBDoesNotImplyReadingC`, `findReplayRecordedElseIdentitySelectionNonempty`, `findSnatchReplaySelectionNonempty`, `findReadingCDoesNotImplyReadingA`, `findRecordedReadingCDoesNotImplyReadingA`, `findReadingADoesNotImplyRecordedReadingC`, `findAllButConsultSelectionNonempty`, `findSnatchReplayNotBothReadings`, `findRefusingSatisfiesBothReadings`, `findIdentitySatisfiesNeitherReading`, `findAllButConsultNotBothReadings`, `findSilentSatisfiesBothReadings`, `findReplayRecordedElseRefuse`, `findReplayRecordedElseRefuseSelectionNonempty`, `bothReadingFinderReproducesEveryRecordedSelection`, `findSnatchRecordedSelectionsAreInRepository`, `findSnatchSelectedSubsetRepository`, `bothReadingFinderSelectionIsTheRecordedSelection`, `recordedReadingBCarriesItsDeclaredWitness`, `readingAAdmitsNothingFinder`, `readingBAdmitsNothingFinder`, `readingCAdmitsNothingFinder`.

```lean
theorem findReadingCImpliesReadingB {f : FindType FindSnatchScenario SnatchPattern} (h : FindRespectsZeroMass findSnatchZeroMassSet f) : FindExcludesRecordedZeroMass f
theorem findSnatchReplayRespectsRecordedZeroMass : FindRespectsZeroMass findSnatchZeroMassSet findSnatchReplay
theorem findReadingBDoesNotImplyReadingC : FindExcludesRecordedZeroMass findReplayRecordedElseIdentity ∧ ¬ FindRespectsZeroMass findSnatchZeroMassSet findReplayRecordedElseIdentity
theorem findReplayRecordedElseIdentitySelectionNonempty : ((findReplayRecordedElseIdentity { context
theorem findSnatchReplaySelectionNonempty : ((findSnatchReplay { context
theorem findReadingCDoesNotImplyReadingA : FindRespectsZeroMass (fun _ : Unit => (∅ : Set SnatchPattern)) findIdentity ∧ ¬ FindFalsifiable (findIdentity (State
theorem findRecordedReadingCDoesNotImplyReadingA : FindRespectsZeroMass findSnatchZeroMassSet findSnatchReplay ∧ ¬ FindFalsifiable findSnatchReplay
theorem findReadingADoesNotImplyRecordedReadingC : FindFalsifiable (findAllBut (State
theorem findAllButConsultSelectionNonempty : ((findAllBut (State
theorem findSnatchReplayNotBothReadings : FindExcludesRecordedZeroMass findSnatchReplay ∧ ¬ FindFalsifiable findSnatchReplay
theorem findRefusingSatisfiesBothReadings : FindFalsifiable (findRefusing (State
theorem findIdentitySatisfiesNeitherReading : ¬ FindFalsifiable (findIdentity (State
theorem findAllButConsultNotBothReadings : FindFalsifiable (findAllBut (State
theorem findSilentSatisfiesBothReadings : FindFalsifiable (findSilent (State
theorem findReplayRecordedElseRefuseSelectionNonempty : ((findReplayRecordedElseRefuse { context
theorem bothReadingFinderReproducesEveryRecordedSelection : ConformantFind findReplayRecordedElseRefuse ∧ FindFalsifiable findReplayRecordedElseRefuse ∧ FindExcludesRecordedZeroMass findReplayRecordedElseRefuse ∧ ∀ t, (findReplayRecordedElseRefuse t findSnatchRepository).selected = (findSnatchReplay t findSnatchRepository).selected
theorem findSnatchRecordedSelectionsAreInRepository : ∀ row ∈ findSnatchScenarios, ∀ p ∈ row.selected, p ∈ snatchRepository
theorem findSnatchSelectedSubsetRepository (scenario : FindSnatchScenario) : findSnatchSelected scenario ⊆ findSnatchRepository.patterns
theorem bothReadingFinderSelectionIsTheRecordedSelection (t : Tension FindSnatchScenario) : (findReplayRecordedElseRefuse t findSnatchRepository).selected = findSnatchSelected t.context
theorem recordedReadingBCarriesItsDeclaredWitness (t : Tension FindSnatchScenario) : ∀ p ∈ findSnatchZeroMass t.context, p ∈ findSnatchRepository.patterns ∧ p ∉ (findSnatchReplay t findSnatchRepository).selected
theorem readingAAdmitsNothingFinder : ConformantFind (findRefusing (State
theorem readingBAdmitsNothingFinder : ConformantFind (findRefusing (State
theorem readingCAdmitsNothingFinder {State P : Type*} (zm : State → Set P) : ConformantFind (findRefusing (State
```

### F11LegacyCarrier.lean

Commit `65d15a8d569b2409c2b14a43342b96536dbfe7c7`; SHA-256 `cacebc7f3389fa8984a50bac0b010ca4c48bb9e6799afda0d713721d8e915a73`.

Declared names: `LegacyReceipt`, `LegacyReceipt.nonSelfCertifying`, `LegacyFindResult`.

### F11NonSelfCertifying.lean

Commit `65d15a8d569b2409c2b14a43342b96536dbfe7c7`; SHA-256 `8ebcbf9f6a15bee0bb1199e4a49b0be8ab4ba7ed0f11067f825347bbc9dd88cd`.

Declared names: `FinderF3`, `FinderF2`, `assertedNonSelfCertifyingReceipt`, `findAssertingF3`, `findAssertingF3_nonempty`, `findAssertingF3_conformant`, `findRFaithful_nonempty`, `findRMisattributing_nonempty`, `faithfulErasure_conformant`, `miscitingErasure_conformant`, `faithful_misciting_erasure_eq`, `WarrantData`, `F3Two`, `F3Four`, `twoOnlyWarrant`, `f3Two_not_equivalent_f3Four`, `recordedWarrant`, `recordedWarrant_passes_both_f3_readings`, `recordedRounds_f2_f3_columns_equal`, `recordedRounds_all_three_columns_equal`, `ClauseAttributedF2`, `faithful_clauseAttributedF2`, `misattributing_not_clauseAttributedF2`, `CitingReceipt`, `FindResultC`, `FindTypeC`, `FindResultC.erase`, `eraseFinderC`, `CitesTheSelectedPattern`, `findCFaithful`, `findCMisciting`, `findCFaithful_nonempty`, `findCMisciting_nonempty`, `findCFaithfulCites`, `findCMiscitingFailsCitation`, `findCErasuresAreEqual`, `findCFaithfulErasureConformant`, `findCMiscitingErasureConformant`, `erasure_loses_f3_attribution`, `citationErasureLosesF3`, `findF2NotF3`, `findF2NotF3_nonempty`, `findF2NotF3_satisfies_f2`, `findF2NotF3_refutes_f3`, `findF3NotF2`, `findF3NotF2_nonempty`, `findF3NotF2_satisfies_f3`, `findF3NotF2_refutes_f2`.

```lean
theorem findAssertingF3_nonempty : (findAssertingF3 { context
theorem findAssertingF3_conformant : ConformantFind findAssertingF3
theorem findRFaithful_nonempty : (findRFaithful { context
theorem findRMisattributing_nonempty : (findRMisattributing { context
theorem faithfulErasure_conformant : ConformantFind (eraseFinder findRFaithful)
theorem miscitingErasure_conformant : ConformantFind (eraseFinder findRMisattributing)
theorem faithful_misciting_erasure_eq : eraseFinder findRFaithful = eraseFinder findRMisattributing
theorem f3Two_not_equivalent_f3Four : F3Two twoOnlyWarrant = true ∧ F3Four twoOnlyWarrant = false
theorem recordedWarrant_passes_both_f3_readings : F3Two recordedWarrant = true ∧ F3Four recordedWarrant = true
theorem recordedRounds_f2_f3_columns_equal : ∀ row ∈ findSnatchRounds, row.receipted = row.nonSelfCertifying
theorem recordedRounds_all_three_columns_equal : ∀ row ∈ findSnatchRounds, row.selected = row.receipted ∧ row.receipted = row.nonSelfCertifying
theorem faithful_clauseAttributedF2 : ClauseAttributedF2 findRFaithful
theorem misattributing_not_clauseAttributedF2 : ¬ ClauseAttributedF2 findRMisattributing
theorem findCFaithful_nonempty : (findCFaithful { context
theorem findCMisciting_nonempty : (findCMisciting { context
theorem findCFaithfulCites : CitesTheSelectedPattern findCFaithful
theorem findCMiscitingFailsCitation : ¬ CitesTheSelectedPattern findCMisciting
theorem findCErasuresAreEqual : eraseFinderC findCFaithful = eraseFinderC findCMisciting
theorem findCFaithfulErasureConformant : ConformantFind (eraseFinderC findCFaithful)
theorem findCMiscitingErasureConformant : ConformantFind (eraseFinderC findCMisciting)
theorem erasure_loses_f3_attribution : FinderF3 (eraseFinder findRFaithful) ∧ FinderF3 (eraseFinder findRMisattributing)
theorem citationErasureLosesF3 : FinderF3 (eraseFinderC findCFaithful) ∧ FinderF3 (eraseFinderC findCMisciting)
theorem findF2NotF3_nonempty : (findF2NotF3 { context
theorem findF2NotF3_satisfies_f2 : FinderF2 findF2NotF3
theorem findF2NotF3_refutes_f3 : ¬ FinderF3 findF2NotF3
theorem findF3NotF2_nonempty : (findF3NotF2 { context
theorem findF3NotF2_satisfies_f3 : FinderF3 findF3NotF2
theorem findF3NotF2_refutes_f2 : ¬ FinderF2 findF3NotF2
```

### F11ReceiptCarrier.lean

Commit `65d15a8d569b2409c2b14a43342b96536dbfe7c7`; SHA-256 `8f888d142b3faa59d857b841aea9f9eed2b2a5f5d5c6ee3cc8d6f73268b00cae`.

Declared names: `misattributedReceiptOwner`, `findSnatchMisattributing`, `findSnatchMisattributingConformant`, `findSnatchMisattributingWitness`, `ReceiptContentAttributed`, `findSnatchMisattributingFailsContentF2`, `ReceiptWithAssertions`, `misattributedReceiptWithAssertions`, `assertionFieldsDoNotFixAttribution`, `RelationalReceipt`, `handCarriedReceipt`, `handCarriedReceiptHasRightClause`, `receiptContentAttributedHoldsOfEveryFinder`, `FindResultR`, `FindTypeR`, `FindResultR.erase`, `eraseFinder`, `ContentF2`, `findRFaithful`, `findRMisattributing`, `findRErasuresAreEqual`, `findRFaithfulErasureConformant`, `findRMisattributingErasureConformant`, `findRFaithfulContentF2`, `findRMisattributingFailsContentF2`.

```lean
theorem findSnatchMisattributingConformant : ConformantFind findSnatchMisattributing
theorem findSnatchMisattributingWitness : SnatchPattern.askForSurplusNotSurrender ∈ (findSnatchMisattributing { context
theorem findSnatchMisattributingFailsContentF2 : ¬ ReceiptContentAttributed findSnatchMisattributing (fun _ _ p => misattributedReceiptOwner p)
theorem assertionFieldsDoNotFixAttribution : misattributedReceiptWithAssertions.acknowledgesClause ∧ misattributedReceiptWithAssertions.hasRoute ∧ misattributedReceiptWithAssertions.hasAsOf ∧ misattributedReceiptOwner .askForSurplusNotSurrender ≠ SnatchPattern.askForSurplusNotSurrender
theorem handCarriedReceiptHasRightClause : handCarriedReceipt.acknowledgedClause = SnatchPattern.askForSurplusNotSurrender
theorem receiptContentAttributedHoldsOfEveryFinder {State P : Type*} (f : FindType State P) : ReceiptContentAttributed f (fun _ _ p => p)
theorem findRErasuresAreEqual : eraseFinder findRFaithful = eraseFinder findRMisattributing
theorem findRFaithfulErasureConformant : ConformantFind (eraseFinder findRFaithful)
theorem findRMisattributingErasureConformant : ConformantFind (eraseFinder findRMisattributing)
theorem findRFaithfulContentF2 : ContentF2 findRFaithful id
theorem findRMisattributingFailsContentF2 : ¬ ContentF2 findRMisattributing id
```

### F11RuledReadings.lean

Commit `93ade5be78ab7bc79f3ac7a4ac3bb087edf5bc6e`; SHA-256 `42ca516ec2a110b4d3698878624f27b7b48f0cc663f85a118b9b043cd18889bf`.

Declared names: `repository`, `result`, `expected`, `validText`, `contentMatches`, `citationMatches`, `independentExpectation`, `citationSwapPreservesContent`, `clauseSwapPreservesCitation`, `finderCanNominateExcluded`, `emptyDesignationVacuous`, `emptyDesignationEarnsNothing`, `noApplicableDesignationEarnsNothing`, `noApplicableDesignationVacuous`.

```lean
theorem contentMatches : (result 7 1 false).contentF2 (expected 7 1)
theorem citationMatches : (result 7 1 false).citationF3 validText
theorem independentExpectation : (result 9 1 false).contentF2 (expected 9 1)
theorem citationSwapPreservesContent : (result 7 1 true).contentF2 (expected 7 1)
theorem clauseSwapPreservesCitation : (result 9 1 false).citationF3 validText
theorem finderCanNominateExcluded : ∃ p ∈ repository.patterns, p ∉ (result 7 1 false).selected
theorem emptyDesignationVacuous : (result 7 1 false).exclusionF4 ∅
theorem emptyDesignationEarnsNothing : ¬ (result 7 1 false).discriminatingF4 ∅
theorem noApplicableDesignationEarnsNothing {P Clause AsOf TextCitation : Type*} {R : Repository P} (r : FindResult P Clause AsOf TextCitation R) (d : Set P) (h : d ∩ R.patterns = ∅) : ¬ r.discriminatingF4 d
theorem noApplicableDesignationVacuous {P Clause AsOf TextCitation : Type*} {R : Repository P} (r : FindResult P Clause AsOf TextCitation R) (d : Set P) (h : d ∩ R.patterns = ∅) : r.exclusionF4 d
```

### F12AdmittingArm.lean

Commit `acb4e95bcfaa23cd8f33406748d68729086214ac`; SHA-256 `2fe080f7052ced10d4c57cca737e5db267cce7a90af1bd5c009bd88605f6e450`.

Declared names: `AdmittingOrganiseType`, `ConformantOrganiseAdmitting`, `admittingArmO2AtZaifRepo`, `admittingArmO3AtZaifRepo`, `zaifAdmittedFor`, `organiseAdmitting`, `organiseAdmittingConformant`, `organiseAdmittingZaifAdmitsNine`, `organiseAdmittingZaifNodes`, `organiseAdmittingMirror`, `organiseAdmittingMirrorConformant`, `admittingAgreeOnNodes`, `admittingAgreeOnEdges`, `admittingSplitUnderdetermined`, `admittingThirdOriginUnconstrainedGeneral`, `admittingZaifEdgeNonVacuous`, `ConformantOrganiseAdmittingSelected`, `organiseAdmittingSelectedReading`, `organiseAdmittingSelectedReadingMirror`, `organiseAdmittingSelectedReadingConformant`, `organiseAdmittingSelectedReadingMirrorConformant`, `admittingSplitUnderdeterminedSelectedReading`, `d1Rank_eq_zero_of_selected`, `admittingSelectedReadingEdgesEmptyOnRecorded`.

```lean
theorem admittingArmO2AtZaifRepo {Policy : Type*} {f : AdmittingOrganiseType Policy Nat} (hf : ConformantOrganiseAdmitting f) : ∀ t sel u v, (f t sel d1Repo).edges u v → Reach d1Authored u v
theorem admittingArmO3AtZaifRepo {Policy : Type*} {f : AdmittingOrganiseType Policy Nat} (hf : ConformantOrganiseAdmitting f) : ∀ t sel u v, (f t sel d1Repo).edges u v ↔ fastForward (f t sel d1Repo).nodes d1Authored u v
theorem organiseAdmittingConformant {Policy : Type*} : ConformantOrganiseAdmitting (organiseAdmitting (Policy
theorem organiseAdmittingZaifAdmitsNine : (organiseAdmitting (Policy
theorem organiseAdmittingZaifNodes : (organiseAdmitting (Policy
theorem organiseAdmittingMirrorConformant {Policy : Type*} : ConformantOrganiseAdmitting (organiseAdmittingMirror (Policy
theorem admittingAgreeOnNodes : (organiseAdmitting (Policy
theorem admittingAgreeOnEdges : (organiseAdmitting (Policy
theorem admittingSplitUnderdetermined : (organiseAdmitting (Policy
theorem admittingThirdOriginUnconstrainedGeneral {Policy : Type*} (f : AdmittingOrganiseType Policy Nat) (hf : ConformantOrganiseAdmitting f) (t : Cascade Policy) (hnodes : (f t d1Selected d1Repo).nodes = d1Nodes) : d1Nodes = d1Selected ∪ (f t d1Selected d1Repo).addedByOrganise ∪ (f t d1Selected d1Repo).admittedBy ∧ (∃ g : AdmittingOrganiseType Unit Nat, ConformantOrganiseAdmitting g ∧ (g trivialPolicyCascade d1Selected d1Repo).nodes = d1Nodes ∧ (g trivialPolicyCascade d1Selected d1Repo).admittedBy = ∅) ∧ (∃ g : AdmittingOrganiseType Unit Nat, ConformantOrganiseAdmitting g ∧ (g trivialPolicyCascade d1Selected d1Repo).nodes = d1Nodes ∧ (g trivialPolicyCascade d1Selected d1Repo).admittedBy = d1Admitted)
theorem admittingZaifEdgeNonVacuous : (organiseAdmitting (Policy
theorem organiseAdmittingSelectedReadingConformant {Policy : Type*} : ConformantOrganiseAdmittingSelected (organiseAdmittingSelectedReading (Policy
theorem organiseAdmittingSelectedReadingMirrorConformant {Policy : Type*} : ConformantOrganiseAdmittingSelected (organiseAdmittingSelectedReadingMirror (Policy
theorem admittingSplitUnderdeterminedSelectedReading : (organiseAdmittingSelectedReading (Policy
theorem d1Rank_eq_zero_of_selected {v : Nat} (hv : v ∈ d1Selected) : d1Rank v = 0
theorem admittingSelectedReadingEdgesEmptyOnRecorded : ∀ u v, ¬ (organiseAdmittingSelectedReading (Policy
```

### F12AntsExemplar.lean

Commit `6a23b33c74fb331d14425a600b59fc3b06502459`; SHA-256 `4ae999694cab8127808f78c38d72825ac73e41a85730943597715cdd5cc473aa`.

Declared names: `antsSelected`, `antsAdmitted`, `antsRepo`, `antsPrecedenceBefore`, `antsPrecedenceAfter`, `antsActingOrderBefore`, `antsActingOrderAfter`, `antsScore`, `organiseAnts`, `organiseAntsConformant`, `organiseAntsPrecedenceMoves`, `organiseAntsActingMovesScoreFlat`, `antsPrecedencePerm`, `organiseAntsNoOrganisedEdges`, `zaifAntsComplementary`, `organiseAntsFlatActingOrder`, `organiseAntsFlatActingOrderSansO4`, `organiseAntsFlatActingOrderNotConformant`, `organiseAntsScoreMovesInstead`, `organiseAntsScoreMovesInsteadConformant`.

```lean
theorem organiseAntsConformant : ConformantOrganiseRuled organiseAnts
theorem organiseAntsPrecedenceMoves : (organiseAnts trivialPolicyCascade antsSelected antsRepo antsAdmitted).precedenceBefore ≠ (organiseAnts trivialPolicyCascade antsSelected antsRepo antsAdmitted).precedenceAfter
theorem organiseAntsActingMovesScoreFlat : (organiseAnts trivialPolicyCascade antsSelected antsRepo antsAdmitted).actingOrderBefore ≠ (organiseAnts trivialPolicyCascade antsSelected antsRepo antsAdmitted).actingOrderAfter ∧ (organiseAnts trivialPolicyCascade antsSelected antsRepo antsAdmitted).scoreBefore = (organiseAnts trivialPolicyCascade antsSelected antsRepo antsAdmitted).scoreAfter
theorem antsPrecedencePerm : antsPrecedenceBefore.Perm antsPrecedenceAfter
theorem organiseAntsNoOrganisedEdges (u v : Nat) : ¬ (organiseAnts trivialPolicyCascade antsSelected antsRepo antsAdmitted).organisedEdges u v
theorem zaifAntsComplementary : (organiseRuled (Policy
theorem organiseAntsFlatActingOrderSansO4 : ConformantOrganiseRuledSansO4 organiseAntsFlatActingOrder
theorem organiseAntsFlatActingOrderNotConformant : ¬ ConformantOrganiseRuled organiseAntsFlatActingOrder
theorem organiseAntsScoreMovesInsteadConformant : ConformantOrganiseRuled organiseAntsScoreMovesInstead
```

### F12AttributionArm.lean

Commit `4930505dc15731239ba79d0c12c992bfe7116601`; SHA-256 `27a797725b623689e29e77af2b09ff93fcf2b28291bb220a27404b25c64cd787`.

Declared names: `ZaifPolicyRule`, `zaifProvenance`, `zaifProvenanceSupport`, `zaifProvenanceSilentOnSelected`, `zaifProvenanceNeverTheStopRule`, `AttributingOrganiseType`, `ConformantOrganiseAttributing`, `organiseAttributing`, `organiseAttributingConformant`, `organiseAttributingZaifAdmitsNine`, `organiseAttributingZaifNodes`, `organiseAttributingMirror`, `attributingMirrorNotConformant`, `attributingThirdOriginDetermined`, `attributingRecordedAdmissions`, `ConformantOrganiseAttributingSansAttr`, `organiseAttributingConformantSansAttr`, `organiseAttributingMirrorConformantSansAttr`, `attributingAgreeOnNodes`, `attributingAgreeOnEdges`, `attributingZaifEdgeNonVacuous`, `attributingSplitSurvivesWithoutOattr`, `zaifPolicyCascade`, `attributionNotFixedByAnyTemperament`, `attributionNotFixedByTemperament`.

```lean
theorem zaifProvenanceSupport : {n | (zaifProvenance n).isSome} = d1Admitted
theorem zaifProvenanceSilentOnSelected : ∀ n ∈ d1Selected, zaifProvenance n = none
theorem zaifProvenanceNeverTheStopRule : ∀ n, zaifProvenance n ≠ some ZaifPolicyRule.haltOnBudget
theorem organiseAttributingConformant {Policy P Rule : Type*} : ConformantOrganiseAttributing (organiseAttributing (Policy
theorem organiseAttributingZaifAdmitsNine : (organiseAttributing (Policy
theorem organiseAttributingZaifNodes : (organiseAttributing (Policy
theorem attributingMirrorNotConformant : ¬ ConformantOrganiseAttributing (organiseAttributingMirror (Policy
theorem attributingThirdOriginDetermined {Policy P Rule : Type*} {f g : AttributingOrganiseType Policy P Rule} (hf : ConformantOrganiseAttributing f) (hg : ConformantOrganiseAttributing g) (t : Cascade Policy) (sel : Set P) (repo : Repository P) (prov : P → Option Rule) : (f t sel repo prov).admittedBy = (g t sel repo prov).admittedBy
theorem attributingRecordedAdmissions {Policy : Type*} {f : AttributingOrganiseType Policy Nat ZaifPolicyRule} (hf : ConformantOrganiseAttributing f) (t : Cascade Policy) : (f t d1Selected d1Repo zaifProvenance).admittedBy = d1Admitted
theorem organiseAttributingConformantSansAttr {Policy P Rule : Type*} : ConformantOrganiseAttributingSansAttr (organiseAttributing (Policy
theorem organiseAttributingMirrorConformantSansAttr {Policy P Rule : Type*} : ConformantOrganiseAttributingSansAttr (organiseAttributingMirror (Policy
theorem attributingAgreeOnNodes {Policy P Rule : Type*} (t : Cascade Policy) (sel : Set P) (repo : Repository P) (prov : P → Option Rule) : (organiseAttributing t sel repo prov).nodes = (organiseAttributingMirror t sel repo prov).nodes
theorem attributingAgreeOnEdges {Policy P Rule : Type*} (t : Cascade Policy) (sel : Set P) (repo : Repository P) (prov : P → Option Rule) : (organiseAttributing t sel repo prov).edges = (organiseAttributingMirror t sel repo prov).edges
theorem attributingZaifEdgeNonVacuous : (organiseAttributing (Policy
theorem attributingSplitSurvivesWithoutOattr : ConformantOrganiseAttributingSansAttr (organiseAttributing (Policy
theorem attributionNotFixedByAnyTemperament : ∀ t : Cascade ZaifPolicyRule, (organiseAdmitting t d1Selected d1Repo).admittedBy ≠ (organiseAdmittingMirror t d1Selected d1Repo).admittedBy
theorem attributionNotFixedByTemperament : ConformantOrganiseAdmitting (organiseAdmitting (Policy
```

### F12CascadeDiffArm.lean

Commit `bea1e26583b20926a1da23513614bcb207c2f559`; SHA-256 `47f1257b3bbfac7b7f447ec5e29ef61bcf88e0dc5f90c8c95a6b438904902cda`.

Declared names: `ConformantOrganiseCascadeDiff`, `organiseCascadeDiff`, `organiseCascadeDiffConformant`, `organiseCascadeDiffZaifNodes`, `organiseCascadeDiffZaifSelected`, `organiseCascadeDiffZaifAdmitted`, `organiseCascadeDiffZaifEdge`, `organiseCascadeDiffNoUnauthoredEdge`, `o4VacuousWhenPrecedenceFlat`, `ConformantOrganiseCascadeDiffSansO4`, `organiseCascadeDiffConformantSansO4`, `organiseCascadeDiffRecordedVariant`, `organiseCascadeDiffRecordedVariantConformant`, `organiseCascadeDiffRecordedVariantConformantSansO4`, `recordedVariantsExposeO4Vacuity`, `organiseCascadeDiffO4Counterexample`, `o4CounterexampleOtherClauses`, `o4CounterexampleNotConformant`, `ConformantOrganiseCascadeDiffSansOAuth`, `sansOAuthStillForcesRepositoryReachability`, `organiseCascadeDiffUnpinnedAuthored`, `organiseCascadeDiffUnpinnedAuthoredSansOAuth`, `unpinnedAuthoredFieldDriftsAtRecordedEdge`, `unpinnedAuthoredNotFullyConformant`, `cascadeDiffRecordedAllLaws`, `cascadeDiffArmIsEmptyAtAnEmptyScore`, `organiseTypeIsInhabitedAtTheSameArguments`, `pinAuthored`, `pinAuthoredAgreesOutsideAuthoredEdges`, `pinAuthoredIsConformant`, `pinAuthoredRepairsTheDriftingWitness`.

```lean
theorem organiseCascadeDiffConformant {Policy Score : Type*} [Inhabited Score] : ConformantOrganiseCascadeDiff (organiseCascadeDiff (Policy
theorem organiseCascadeDiffZaifNodes : (organiseCascadeDiff (Policy
theorem organiseCascadeDiffZaifSelected : (organiseCascadeDiff (Policy
theorem organiseCascadeDiffZaifAdmitted : (organiseCascadeDiff (Policy
theorem organiseCascadeDiffZaifEdge : (organiseCascadeDiff (Policy
theorem organiseCascadeDiffNoUnauthoredEdge : ¬ (organiseCascadeDiff (Policy
theorem o4VacuousWhenPrecedenceFlat {Policy P Score : Type*} (f : armTwoOrganiseType Policy P Score) (t : Cascade Policy) (sel : Set P) (repo : Repository P) (hflat : (f t sel repo).precedenceBefore = (f t sel repo).precedenceAfter) : (f t sel repo).precedenceBefore ≠ (f t sel repo).precedenceAfter → (f t sel repo).actingOrderBefore ≠ (f t sel repo).actingOrderAfter ∨ (f t sel repo).scoreBefore ≠ (f t sel repo).scoreAfter
theorem organiseCascadeDiffConformantSansO4 {Policy Score : Type*} [Inhabited Score] : ConformantOrganiseCascadeDiffSansO4 (organiseCascadeDiff (Policy
theorem organiseCascadeDiffRecordedVariantConformant {Policy : Type*} : ConformantOrganiseCascadeDiff (organiseCascadeDiffRecordedVariant (Policy
theorem organiseCascadeDiffRecordedVariantConformantSansO4 {Policy : Type*} : ConformantOrganiseCascadeDiffSansO4 (organiseCascadeDiffRecordedVariant (Policy
theorem recordedVariantsExposeO4Vacuity : let a
theorem o4CounterexampleOtherClauses {Policy : Type*} : ConformantOrganiseCascadeDiffSansO4 (organiseCascadeDiffO4Counterexample (Policy
theorem o4CounterexampleNotConformant : ¬ ConformantOrganiseCascadeDiff (organiseCascadeDiffO4Counterexample (Policy
theorem sansOAuthStillForcesRepositoryReachability {Policy P Score : Type*} {f : armTwoOrganiseType Policy P Score} (hf : ConformantOrganiseCascadeDiffSansOAuth f) : ∀ t sel repo u v, (f t sel repo).organisedEdges u v → Reach repo.standsOn u v
theorem organiseCascadeDiffUnpinnedAuthoredSansOAuth {Policy : Type*} : ConformantOrganiseCascadeDiffSansOAuth (organiseCascadeDiffUnpinnedAuthored (Policy
theorem unpinnedAuthoredFieldDriftsAtRecordedEdge : ¬ (organiseCascadeDiffUnpinnedAuthored (Policy
theorem unpinnedAuthoredNotFullyConformant : ¬ ConformantOrganiseCascadeDiff (organiseCascadeDiffUnpinnedAuthored (Policy
theorem cascadeDiffRecordedAllLaws : ConformantOrganiseCascadeDiff (organiseCascadeDiff (Policy
theorem cascadeDiffArmIsEmptyAtAnEmptyScore : ¬ Nonempty (armTwoOrganiseType Unit Nat Empty)
theorem organiseTypeIsInhabitedAtTheSameArguments : Nonempty (OrganiseType Unit Nat)
theorem pinAuthoredAgreesOutsideAuthoredEdges {Policy P Score : Type*} (f : armTwoOrganiseType Policy P Score) (t : Cascade Policy) (sel : Set P) (repo : Repository P) : (pinAuthored f t sel repo).selected = (f t sel repo).selected ∧ (pinAuthored f t sel repo).nodes = (f t sel repo).nodes ∧ (pinAuthored f t sel repo).addedByOrganise = (f t sel repo).addedByOrganise ∧ (pinAuthored f t sel repo).admittedBy = (f t sel repo).admittedBy ∧ (pinAuthored f t sel repo).organisedEdges = (f t sel repo).organisedEdges ∧ (pinAuthored f t sel repo).precedenceBefore = (f t sel repo).precedenceBefore ∧ (pinAuthored f t sel repo).precedenceAfter = (f t sel repo).precedenceAfter ∧ (pinAuthored f t sel repo).actingOrderBefore = (f t sel repo).actingOrderBefore ∧ (pinAuthored f t sel repo).actingOrderAfter = (f t sel repo).actingOrderAfter ∧ (pinAuthored f t sel repo).scoreBefore = (f t sel repo).scoreBefore ∧ (pinAuthored f t sel repo).scoreAfter = (f t sel repo).scoreAfter
theorem pinAuthoredIsConformant {Policy P Score : Type*} {f : armTwoOrganiseType Policy P Score} (hf : ConformantOrganiseCascadeDiffSansOAuth f) : ConformantOrganiseCascadeDiff (pinAuthored f)
theorem pinAuthoredRepairsTheDriftingWitness {Policy : Type*} : ConformantOrganiseCascadeDiff (pinAuthored (organiseCascadeDiffUnpinnedAuthored (Policy
```

### F12Conformance.lean

Commit `0691f74e48ae22dbc733f974780e9bd72a58e281`; SHA-256 `657637c650a205c751ee5a512b30830a44f64ea96543ef4be639f1b0fe218f35`.

Declared names: `OrganiseType`, `ConformantOrganiseSelected`, `ConformantOrganiseNodes`, `reach_trans`, `fastForward_acyclic`, `organiseSelectedOnly`, `organiseSelectedOnlyConformant`, `organiseUpClosure`, `organiseUpClosureConformant`, `trivialPolicyCascade`, `organiseWitnessesDifferOnZaif`, `organiseUpClosureAddsSomething`, `admittedIndistinguishableFromAdded`, `selectedReadingCannotYieldZaifEdge`, `selectedOnlyYieldsZaifEdgeWhenAdmittedSupplied`, `organiseSelectedOnlyConformantNodes`, `organiseUpClosureNotConformantNodes`, `d1_not_reachable_of_rank_zero`, `organiseUpClosureOmitsAdmittedWitness`.

```lean
theorem reach_trans {P : Type*} {r : P → P → Prop} {a b c : P} : Reach r a b → Reach r b c → Reach r a c
theorem fastForward_acyclic {P : Type*} (sel : Set P) (r : P → P → Prop) (h : acyclicDescent r) : acyclicDescent (fastForward sel r)
theorem organiseSelectedOnlyConformant {Policy P : Type*} : ConformantOrganiseSelected (organiseSelectedOnly (Policy
theorem organiseUpClosureConformant {Policy P : Type*} : ConformantOrganiseSelected (organiseUpClosure (Policy
theorem organiseWitnessesDifferOnZaif : (organiseSelectedOnly (Policy
theorem organiseUpClosureAddsSomething : (26 : Nat) ∈ (organiseUpClosure (Policy
theorem admittedIndistinguishableFromAdded {Policy : Type*} (f : OrganiseType Policy Nat) (hf : ConformantOrganiseSelected f) (t : Cascade Policy) (hnodes : (f t d1Selected d1Repo).nodes = d1Nodes) : d1Admitted ⊆ (f t d1Selected d1Repo).addedByOrganise
theorem selectedReadingCannotYieldZaifEdge {Policy : Type*} (f : OrganiseType Policy Nat) (hf : ConformantOrganiseSelected f) (t : Cascade Policy) : ¬ ((f t d1Selected d1Repo).edges 18 19)
theorem selectedOnlyYieldsZaifEdgeWhenAdmittedSupplied : (organiseSelectedOnly (Policy
theorem organiseSelectedOnlyConformantNodes {Policy P : Type*} : ConformantOrganiseNodes (organiseSelectedOnly (Policy
theorem organiseUpClosureNotConformantNodes : ¬ ConformantOrganiseNodes (organiseUpClosure (Policy
theorem d1_not_reachable_of_rank_zero {v : Nat} (hv : d1Rank v = 0) (u : Nat) : ¬ Reach d1Authored u v
theorem organiseUpClosureOmitsAdmittedWitness : (11 : Nat) ∉ (organiseUpClosure (Policy
```

### F12D1Arms.lean

Commit `e77f73b67988007098df88c56de7d5c1503b5d63`; SHA-256 `236b7888f52cb6c73dd801f428d72b9246c1e5d63bd76139ef412fb543e72f42`.

Declared names: `d1Selected`, `d1Admitted`, `d1Nodes`, `d1Authored`, `d1Closure`, `d1Rank`, `d1_authored_increases_rank`, `d1AuthoredAcyclic`, `d1_reachOutside_to_reach`, `d1_fastForward_increases_rank`, `d1OrganisedAcyclic`, `ArmOneCascade`, `armOneOrganiseType`, `armOneZaif`, `armOneO1`, `armOneO2`, `armOneO3`, `armTwoOrganiseType`, `armTwoO4AntecedentUnsatisfiable`, `armTwoZaifActingOrderFlat`, `armTwoZaifScoreFlat`, `d1Repo`, `d1CascadeZaif`, `armThreeO2`, `armThreeO3`, `armThreeOrganisedEdgeExists`, `armThreeNoUnauthoredEdge`, `armThreeO1PatternsSubstitutionFails`.

```lean
theorem d1_authored_increases_rank : ∀ u v, d1Authored u v → d1Rank u < d1Rank v
theorem d1AuthoredAcyclic : acyclicDescent d1Authored
theorem d1_reachOutside_to_reach {P : Type*} {selected : Set P} {r : P → P → Prop} {u v : P} : ReachOutside selected r u v → Reach r u v
theorem d1_fastForward_increases_rank : ∀ u v, fastForward d1Nodes d1Authored u v → d1Rank u < d1Rank v
theorem d1OrganisedAcyclic : acyclicDescent (fastForward d1Nodes d1Authored)
theorem armOneO1 : armOneZaif.nodes = armOneZaif.selected ∪ armOneZaif.addedByOrganise ∪ armOneZaif.admittedBy
theorem armOneO2 (repo : Repository Nat) (hrepo : repo.standsOn = d1Authored) : ∀ u v, armOneZaif.edges u v → Reach repo.standsOn u v
theorem armOneO3 (repo : Repository Nat) (hrepo : repo.standsOn = d1Authored) : ∀ u v, armOneZaif.edges u v ↔ fastForward armOneZaif.nodes repo.standsOn u v
theorem armTwoO4AntecedentUnsatisfiable : ¬ (wmZaifCascadeDiffFixture.precedenceBefore ≠ wmZaifCascadeDiffFixture.precedenceAfter)
theorem armTwoZaifActingOrderFlat : wmZaifCascadeDiffFixture.actingOrderBefore = wmZaifCascadeDiffFixture.actingOrderAfter
theorem armTwoZaifScoreFlat : wmZaifCascadeDiffFixture.scoreBefore = wmZaifCascadeDiffFixture.scoreAfter
theorem armThreeO2 : ∀ u v, d1CascadeZaif.edges u v → Reach d1Repo.standsOn u v
theorem armThreeO3 : ∀ u v, d1CascadeZaif.edges u v ↔ fastForward d1CascadeZaif.nodes d1Repo.standsOn u v
theorem armThreeOrganisedEdgeExists : d1CascadeZaif.edges 18 19
theorem armThreeNoUnauthoredEdge : ¬ d1CascadeZaif.edges 0 1
theorem armThreeO1PatternsSubstitutionFails : d1CascadeZaif.nodes ≠ d1Repo.patterns ∪ d1CascadeZaif.addedByOrganise
```

### F12DischargeArm.lean

Commit `38a34f286a3fa6d4e677ec846fc83ae07cd6cf53`; SHA-256 `d06ca0fc968d8d8411bc9bf0ea41f0a6de0c1ec19bd2665edcf6aeb8e1d90696`.

Declared names: `organiseEmptyCascade`, `organiseEmpty`, `organiseTypeNonempty`, `organiseEmptyNotConformantSelected`, `organiseEmptyNotConformantNodes`, `organiseDischargeExistsSelected`, `organiseDischargeExistsNodes`, `upClosureNodesSet`, `organiseNodesUpClosure`, `organiseNodesUpClosureConformantNodes`, `organiseDischargeNotUniqueSelected`, `organiseDischargeNotUniqueNodes`, `organiseDischargeWitnessesAgreeOnSelected`, `organiseNodesUpClosureAddsZaifVertex`, `organiseNodesUpClosureEdgeExists`, `organiseSelectedOnlyNoSuchEdge`, `organiseNodesUpClosureNoUnauthoredEdge`, `nonTrivialPolicyCascade`, `organiseByTemperamentSelected`, `organiseByTemperamentSelectedConformant`, `organiseByTemperamentSelectedReadsTemperament`, `organiseByTemperamentNodes`, `organiseByTemperamentNodesConformant`, `organiseByTemperamentNodesReadsTemperament`, `organiseSelectedOnlyIgnoresTemperament`, `organiseOpaqueZaif`, `organiseOpaqueNoBody`.

```lean
theorem organiseTypeNonempty {Policy P : Type*} : Nonempty (OrganiseType Policy P)
theorem organiseEmptyNotConformantSelected : ¬ ConformantOrganiseSelected (organiseEmpty (Policy
theorem organiseEmptyNotConformantNodes : ¬ ConformantOrganiseNodes (organiseEmpty (Policy
theorem organiseDischargeExistsSelected {Policy P : Type*} : ∃ f : OrganiseType Policy P, ConformantOrganiseSelected f
theorem organiseDischargeExistsNodes {Policy P : Type*} : ∃ f : OrganiseType Policy P, ConformantOrganiseNodes f
theorem organiseNodesUpClosureConformantNodes {Policy P : Type*} : ConformantOrganiseNodes (organiseNodesUpClosure (Policy
theorem organiseDischargeNotUniqueSelected : ∃ f g : OrganiseType Unit Nat, ConformantOrganiseSelected f ∧ ConformantOrganiseSelected g ∧ (f trivialPolicyCascade d1Selected d1Repo).nodes ≠ (g trivialPolicyCascade d1Selected d1Repo).nodes
theorem organiseDischargeNotUniqueNodes : ∃ f g : OrganiseType Unit Nat, ConformantOrganiseNodes f ∧ ConformantOrganiseNodes g ∧ (f trivialPolicyCascade d1Selected d1Repo).nodes ≠ (g trivialPolicyCascade d1Selected d1Repo).nodes
theorem organiseDischargeWitnessesAgreeOnSelected : d1Selected.Nonempty ∧ d1Selected ⊆ (organiseSelectedOnly trivialPolicyCascade d1Selected d1Repo).nodes ∧ d1Selected ⊆ (organiseNodesUpClosure trivialPolicyCascade d1Selected d1Repo).nodes
theorem organiseNodesUpClosureAddsZaifVertex : (26 : Nat) ∈ (organiseNodesUpClosure trivialPolicyCascade d1Selected d1Repo).addedByOrganise
theorem organiseNodesUpClosureEdgeExists : (organiseNodesUpClosure trivialPolicyCascade d1Selected d1Repo).edges 6 26
theorem organiseSelectedOnlyNoSuchEdge : ¬ (organiseSelectedOnly trivialPolicyCascade d1Selected d1Repo).edges 6 26
theorem organiseNodesUpClosureNoUnauthoredEdge : ¬ (organiseNodesUpClosure trivialPolicyCascade d1Selected d1Repo).edges 0 1
theorem organiseByTemperamentSelectedConformant {Policy P : Type*} : ConformantOrganiseSelected (organiseByTemperamentSelected (Policy
theorem organiseByTemperamentSelectedReadsTemperament : (organiseByTemperamentSelected trivialPolicyCascade d1Selected d1Repo).nodes ≠ (organiseByTemperamentSelected nonTrivialPolicyCascade d1Selected d1Repo).nodes
theorem organiseByTemperamentNodesConformant {Policy P : Type*} : ConformantOrganiseNodes (organiseByTemperamentNodes (Policy
theorem organiseByTemperamentNodesReadsTemperament : (organiseByTemperamentNodes trivialPolicyCascade d1Selected d1Repo).nodes ≠ (organiseByTemperamentNodes nonTrivialPolicyCascade d1Selected d1Repo).nodes
theorem organiseSelectedOnlyIgnoresTemperament : ∀ (t t' : Cascade Unit) (sel : Set Nat) (repo : Repository Nat), organiseSelectedOnly t sel repo = organiseSelectedOnly t' sel repo
opaque organiseOpaqueZaif : OrganiseType Unit Nat
opaque organiseOpaqueNoBody : OrganiseType Unit Nat end end DarkTower.WarMachine.Holes
```

### F12FifthSnatchExemplar.lean

Commit `a058052dd10b46cdd3cd54a6f713868e65145a12`; SHA-256 `87e42933e257b4a4463327355c7edf81b840b787bb59af171ac2a87be9158838`.

Declared names: `snatchG1SnatcherSelected`, `snatchG1SnatcherAdded`, `organiseSnatchG1Snatcher`, `organiseSnatchG1SnatcherConformant`, `snatchG1SnatcherRecordedEdges`, `snatchG1SnatcherInAdded`, `snatchG1SnatcherNoRecordedEdgeSurvives`, `snatchG1SnatcherRecordedEdgeCount`, `organiseSnatchG1SnatcherNotEdge1018`.

```lean
theorem organiseSnatchG1SnatcherConformant : ConformantOrganiseRuled organiseSnatchG1Snatcher
theorem snatchG1SnatcherNoRecordedEdgeSurvives : snatchG1SnatcherRecordedEdges.filter (fun e => !(snatchG1SnatcherInAdded e.1 || snatchG1SnatcherInAdded e.2)) = []
theorem snatchG1SnatcherRecordedEdgeCount : snatchG1SnatcherRecordedEdges.length = 2
theorem organiseSnatchG1SnatcherNotEdge1018 : ¬ (organiseSnatchG1Snatcher trivialPolicyCascade snatchG1SnatcherSelected snatchRepo ∅).organisedEdges 10 18
```

### F12FourthSnatchExemplar.lean

Commit `a8ee097fc022ac42dbc8bd8c6be4b8f1bd98bce8`; SHA-256 `4c57c7c5bbe56c77262a06313524a5bfc1409db7a9b4f5137061bd39eaca567c`.

Declared names: `snatchG1SharerSelected`, `snatchG1SharerAdded`, `organiseSnatchG1Sharer`, `organiseSnatchG1SharerConformant`, `snatchG1SharerRecordedEdges`, `snatchG1SharerInAdded`, `snatchG1SharerNoRecordedEdgeSurvives`, `snatchG1SharerRecordedEdgeCount`, `organiseSnatchG1SharerNotEdge618`.

```lean
theorem organiseSnatchG1SharerConformant : ConformantOrganiseRuled organiseSnatchG1Sharer
theorem snatchG1SharerNoRecordedEdgeSurvives : snatchG1SharerRecordedEdges.filter (fun e => !(snatchG1SharerInAdded e.1 || snatchG1SharerInAdded e.2)) = []
theorem snatchG1SharerRecordedEdgeCount : snatchG1SharerRecordedEdges.length = 3
theorem organiseSnatchG1SharerNotEdge618 : ¬ (organiseSnatchG1Sharer trivialPolicyCascade snatchG1SharerSelected snatchRepo ∅).organisedEdges 6 18
```

### F12MiningExemplar.lean

Commit `167c32668e842378a3d88b5d0dbacdd7a3b3e0d7`; SHA-256 `890a5e36dc4a911244b1feafc0c7b924749c0810da1498a0a27db24430aadd60`.

Declared names: `miningSelected`, `miningAdmitted`, `miningRepo`, `miningPrecedence`, `miningActingOrder`, `organiseMining`, `miningScoreCannotMove`, `organiseMiningConformant`, `organiseMiningEdge01`, `organiseMiningEdge12`, `organiseMiningNotEdge02`, `organiseMiningHasOrganisedEdge`, `organiseMiningTransitiveClosure`, `organiseMiningTransitiveClosureSansO3`, `organiseMiningTransitiveClosureNotConformant`, `organiseMiningInventedEdge`, `organiseMiningInventedEdgeFailsO2`, `organiseMiningInventedEdgeNotConformant`, `organiseMiningPrecedenceMoves`, `organiseMiningPrecedenceMovesSansO4`, `organiseMiningPrecedenceMovesNotConformant`.

```lean
theorem miningScoreCannotMove (f : RuledOrganiseType Unit Nat Unit) (t : Cascade Unit) (sel : Set Nat) (repo : Repository Nat) (adm : Set Nat) : ¬ ((f t sel repo adm).scoreBefore ≠ (f t sel repo adm).scoreAfter)
theorem organiseMiningConformant : ConformantOrganiseRuled organiseMining
theorem organiseMiningEdge01 : (organiseMining trivialPolicyCascade miningSelected miningRepo miningAdmitted).organisedEdges 0 1
theorem organiseMiningEdge12 : (organiseMining trivialPolicyCascade miningSelected miningRepo miningAdmitted).organisedEdges 1 2
theorem organiseMiningNotEdge02 : ¬ (organiseMining trivialPolicyCascade miningSelected miningRepo miningAdmitted).organisedEdges 0 2
theorem organiseMiningHasOrganisedEdge : ∃ u v, (organiseMining trivialPolicyCascade miningSelected miningRepo miningAdmitted).organisedEdges u v
theorem organiseMiningTransitiveClosureSansO3 : ConformantOrganiseRuledSansO3 organiseMiningTransitiveClosure
theorem organiseMiningTransitiveClosureNotConformant : ¬ ConformantOrganiseRuled organiseMiningTransitiveClosure
theorem organiseMiningInventedEdgeFailsO2 : ¬ Reach miningRepo.standsOn 2 1
theorem organiseMiningInventedEdgeNotConformant : ¬ ConformantOrganiseRuled organiseMiningInventedEdge
theorem organiseMiningPrecedenceMovesSansO4 : ConformantOrganiseRuledSansO4 organiseMiningPrecedenceMoves
theorem organiseMiningPrecedenceMovesNotConformant : ¬ ConformantOrganiseRuled organiseMiningPrecedenceMoves
```

### F12O3FieldArm.lean

Commit `28c518612ac6ebda7ac78e5f7355a1e513afb171`; SHA-256 `ada6a61ee6be11d5f447258f0af95ce5fcbcbd1d3eddaef30f66ffbe26fb9fcd`.

Declared names: `ConformantOrganiseNoBootstrap`, `c59AddedByOrganiseEmpty`, `c59NodesDiffAdded_eq_nodes`, `c59SelectedUnionAdmitted_eq_nodes`, `zaifAddedByOrganiseEmpty`, `zaifNodesDiffAdded_eq_nodes`, `zaifSelectedUnionAdmitted_eq_nodes`, `organiseO3NoBootstrapZaif`, `organiseNodeClosureEdges`, `organiseNodeClosureEdgesConformantNodes`, `organiseNodeClosureEdgesOutside_eq_selected`, `nodeClosureAddsTwentySixAndTwentyFive`, `nodeClosureNodeReadingEdgeTwentySixTwentyFive`, `nodeClosureNoBootstrapRejectsTwentySixTwentyFive`, `organiseNodeClosureEdgesNotConformantNoBootstrap`, `recordedNoBootstrapFastForwardEmpty`, `organiseSelectedOnlyConformantNoBootstrap`, `noBootstrapRelationInhabitedOnConstructedInput`, `noBootstrapCannotYieldZaifEdge`, `zaifRecordedEdgeNonVacuous`, `noBootstrapYieldsZaifEdgeAtValueCarrier`.

```lean
theorem c59AddedByOrganiseEmpty : wmCascadeDiffFixture.addedByOrganise = ∅
theorem c59NodesDiffAdded_eq_nodes : wmCascadeDiffFixture.nodes \ wmCascadeDiffFixture.addedByOrganise = wmCascadeDiffFixture.nodes
theorem c59SelectedUnionAdmitted_eq_nodes : wmCascadeDiffFixture.selected ∪ wmCascadeDiffFixture.admittedBy = wmCascadeDiffFixture.nodes
theorem zaifAddedByOrganiseEmpty : wmZaifCascadeDiffFixture.addedByOrganise = ∅
theorem zaifNodesDiffAdded_eq_nodes : wmZaifCascadeDiffFixture.nodes \ wmZaifCascadeDiffFixture.addedByOrganise = wmZaifCascadeDiffFixture.nodes
theorem zaifSelectedUnionAdmitted_eq_nodes : wmZaifCascadeDiffFixture.selected ∪ wmZaifCascadeDiffFixture.admittedBy = wmZaifCascadeDiffFixture.nodes
theorem organiseO3NoBootstrapZaif : ∀ u v, wmZaifCascadeDiffFixture.organisedEdges u v ↔ fastForward (wmZaifCascadeDiffFixture.nodes \ wmZaifCascadeDiffFixture.addedByOrganise) wmZaifCascadeDiffFixture.authoredEdges u v
theorem organiseNodeClosureEdgesConformantNodes {Policy P : Type*} : ConformantOrganiseNodes (organiseNodeClosureEdges (Policy
theorem organiseNodeClosureEdgesOutside_eq_selected {Policy P : Type*} (t : Cascade Policy) (sel : Set P) (repo : Repository P) : (organiseNodeClosureEdges t sel repo).nodes \ (organiseNodeClosureEdges t sel repo).addedByOrganise = sel
theorem nodeClosureAddsTwentySixAndTwentyFive : (26 : Nat) ∈ (organiseNodeClosureEdges trivialPolicyCascade d1Selected d1Repo).addedByOrganise ∧ (25 : Nat) ∈ (organiseNodeClosureEdges trivialPolicyCascade d1Selected d1Repo).addedByOrganise
theorem nodeClosureNodeReadingEdgeTwentySixTwentyFive : (organiseNodeClosureEdges trivialPolicyCascade d1Selected d1Repo).edges 26 25
theorem nodeClosureNoBootstrapRejectsTwentySixTwentyFive : ¬ fastForward ((organiseNodeClosureEdges trivialPolicyCascade d1Selected d1Repo).nodes \ (organiseNodeClosureEdges trivialPolicyCascade d1Selected d1Repo).addedByOrganise) d1Repo.standsOn 26 25
theorem organiseNodeClosureEdgesNotConformantNoBootstrap : ¬ ConformantOrganiseNoBootstrap (organiseNodeClosureEdges (Policy
theorem recordedNoBootstrapFastForwardEmpty : ∀ u v, ¬ fastForward ((organiseNodeClosureEdges trivialPolicyCascade d1Selected d1Repo).nodes \ (organiseNodeClosureEdges trivialPolicyCascade d1Selected d1Repo).addedByOrganise) d1Repo.standsOn u v
theorem organiseSelectedOnlyConformantNoBootstrap {Policy P : Type*} : ConformantOrganiseNoBootstrap (organiseSelectedOnly (Policy
theorem noBootstrapRelationInhabitedOnConstructedInput : fastForward ((organiseSelectedOnly (Policy
theorem noBootstrapCannotYieldZaifEdge {Policy : Type*} (f : OrganiseType Policy Nat) (hf : ConformantOrganiseNoBootstrap f) (t : Cascade Policy) : ¬ (f t d1Selected d1Repo).edges 18 19
theorem zaifRecordedEdgeNonVacuous : wmZaifCascadeDiffFixture.organisedEdges 18 19
theorem noBootstrapYieldsZaifEdgeAtValueCarrier : fastForward (wmZaifCascadeDiffFixture.selected ∪ wmZaifCascadeDiffFixture.admittedBy) wmZaifCascadeDiffFixture.authoredEdges 18 19
```

### F12RuledCarrier.lean

Commit `738cae3a540ac03a4d77e1ebe399c7c6dccd8cb6`; SHA-256 `c8cff86e20348c02f8dbb41eaaa516bbf57529f6bc7bc525f547f58c5fbc2270`.

Declared names: `CandidateActionSpace`, `RuledOrganiseType`, `ConformantOrganiseRuled`, `noBootstrapCarrier_eq_selected_admitted`, `organiseRuled`, `organiseRuledConformant`, `exists_conformantOrganiseRuled`, `organiseRuledZaifNodes`, `organiseRuledZaifSelected`, `organiseRuledZaifAdmitted`, `organiseRuledZaifEdge`, `organiseRuledZaifO4AntecedentFalse`, `organiseRuledZaifPrecedenceIsTheRecordedOne`, `ruledO4UnexercisedOnTheRecordedRun`, `ConformantOrganiseRuledSansO4`, `organiseRuledO4Counterexample`, `organiseRuledO4CounterexampleOtherClauses`, `organiseRuledO4CounterexampleNotConformant`, `organiseRuledMisfiled`, `organiseRuledMisfiledNotConformant`, `organiseRuledIgnoresAttribution`, `organiseRuledIgnoresAttributionNotConformant`, `organiseRuledOwnBootstrap`, `ConformantOrganiseRuledSansO3`, `organiseRuledOwnBootstrapOtherClauses`, `organiseRuledOwnBootstrapNotConformant`.

```lean
theorem noBootstrapCarrier_eq_selected_admitted {Policy P Score : Type*} {f : RuledOrganiseType Policy P Score} (hf : ConformantOrganiseRuled f) (t : Cascade Policy) (sel : Set P) (repo : Repository P) (adm : Set P) (hempty : (f t sel repo adm).addedByOrganise = ∅) : (f t sel repo adm).nodes \ (f t sel repo adm).addedByOrganise = sel ∪ adm
theorem organiseRuledConformant {Policy P Score : Type*} [Inhabited Score] : ConformantOrganiseRuled (organiseRuled (Policy
theorem exists_conformantOrganiseRuled (Policy P Score : Type*) [Inhabited Score] : ∃ f : RuledOrganiseType Policy P Score, ConformantOrganiseRuled f
theorem organiseRuledZaifNodes : (organiseRuled (Policy
theorem organiseRuledZaifSelected : (organiseRuled (Policy
theorem organiseRuledZaifAdmitted : (organiseRuled (Policy
theorem organiseRuledZaifEdge : (organiseRuled (Policy
theorem organiseRuledZaifO4AntecedentFalse : ¬ ((organiseRuled (Policy
theorem organiseRuledZaifPrecedenceIsTheRecordedOne : (organiseRuled (Policy
theorem ruledO4UnexercisedOnTheRecordedRun : ¬ (wmZaifCascadeDiffFixture.precedenceBefore ≠ wmZaifCascadeDiffFixture.precedenceAfter)
theorem organiseRuledO4CounterexampleOtherClauses {Policy : Type*} : ConformantOrganiseRuledSansO4 (organiseRuledO4Counterexample (Policy
theorem organiseRuledO4CounterexampleNotConformant : ¬ ConformantOrganiseRuled (organiseRuledO4Counterexample (Policy
theorem organiseRuledMisfiledNotConformant : ¬ ConformantOrganiseRuled (organiseRuledMisfiled (Policy
theorem organiseRuledIgnoresAttributionNotConformant : ¬ ConformantOrganiseRuled (organiseRuledIgnoresAttribution (Policy
theorem organiseRuledOwnBootstrapOtherClauses {Policy : Type*} : ConformantOrganiseRuledSansO3 (organiseRuledOwnBootstrap (Policy
theorem organiseRuledOwnBootstrapNotConformant : ¬ ConformantOrganiseRuled (organiseRuledOwnBootstrap (Policy
```

### F12SecondSnatchExemplar.lean

Commit `b1c0f998d20bfbcb54877a9ea7897f8ecd3ac610`; SHA-256 `3338c71f2e0121f0fe4af21156149850baff07010c31bc10243bd999d771fade`.

Declared names: `snatchG2Selected`, `snatchG2Added`, `organiseSnatchG2`, `organiseSnatchG2Conformant`, `snatchG2RecordedEdges`, `snatchG2InAdded`, `snatchG2InAdded_iff`, `snatchG2NoRecordedEdgeSurvives`, `snatchG2RecordedEdgeCount`, `organiseSnatchG2NotEdge39`.

```lean
theorem organiseSnatchG2Conformant : ConformantOrganiseRuled organiseSnatchG2
theorem snatchG2InAdded_iff (n : Nat) : snatchG2InAdded n = true ↔ n ∈ snatchG2Added
theorem snatchG2NoRecordedEdgeSurvives : snatchG2RecordedEdges.filter (fun e => !(snatchG2InAdded e.1 || snatchG2InAdded e.2)) = []
theorem snatchG2RecordedEdgeCount : snatchG2RecordedEdges.length = 5
theorem organiseSnatchG2NotEdge39 : ¬ (organiseSnatchG2 trivialPolicyCascade snatchG2Selected snatchRepo ∅).organisedEdges 3 9
```

### F12SixthSnatchExemplar.lean

Commit `fc8dde98e1949ddfae84f2d8a3f1df39ad16aa37`; SHA-256 `2ce9202b6d87d12d80da2b3f72c79bea91ab5fbfb00dc4fc2cebf6f355ff6633`.

Declared names: `snatchG5SharerSelected`, `snatchG5SharerAdded`, `organiseSnatchG5Sharer`, `organiseSnatchG5SharerConformant`, `snatchG5SharerRecordedEdges`, `snatchG5SharerInAdded`, `snatchG5SharerNoRecordedEdgeSurvives`, `snatchG5SharerRecordedEdgeCount`, `organiseSnatchG5SharerNotEdge618`.

```lean
theorem organiseSnatchG5SharerConformant : ConformantOrganiseRuled organiseSnatchG5Sharer
theorem snatchG5SharerNoRecordedEdgeSurvives : snatchG5SharerRecordedEdges.filter (fun e => !(snatchG5SharerInAdded e.1 || snatchG5SharerInAdded e.2)) = []
theorem snatchG5SharerRecordedEdgeCount : snatchG5SharerRecordedEdges.length = 3
theorem organiseSnatchG5SharerNotEdge618 : ¬ (organiseSnatchG5Sharer trivialPolicyCascade snatchG5SharerSelected snatchRepo ∅).organisedEdges 6 18
```

### F12SnatchExemplar.lean

Commit `3e6f6044576d83a46d5e7e6c066cf2908286b995`; SHA-256 `2b63816c1f0d343f408624b5eb9b9032fbc090147492aa8d1d1ec446bd5648f1`.

Declared names: `snatchSelected`, `snatchAdded`, `snatchAdmitted`, `snatchRepo`, `snatchPrecedenceBefore`, `snatchPrecedenceAfter`, `snatchActingOrderBefore`, `snatchActingOrderAfter`, `snatchScoreBefore`, `snatchScoreAfter`, `organiseSnatch`, `organiseSnatchConformant`, `organiseSnatchPrecedenceMoves`, `organiseSnatchActingAndScoreMove`, `organiseSnatchEdge02`, `snatchPrecedencePerm`, `snatchActingOrderNotPerm`, `organiseSnatchRecordedEdges`, `organiseSnatchRecordedEdgesSansO3`, `organiseSnatchRecordedEdge1822`, `organiseSnatchNotEdge1822`, `organiseSnatchNotEdge1018`, `organiseSnatchRefusesAddedEndpoint`, `snatchOrganisedEdgeList`, `snatchInAdded`, `snatchInAdded_iff`, `snatchOnlyEdge02SurvivesTheSubtraction`, `snatchOrganisedEdgeListLength`, `organiseSnatchRecordedEdgesNotConformant`, `organiseSnatchActingFlat`, `organiseSnatchActingFlatConformant`, `organiseSnatchActingFlatScoreFlat`, `organiseSnatchActingFlatScoreFlatSansO4`, `organiseSnatchActingFlatScoreFlatNotConformant`.

```lean
theorem organiseSnatchConformant : ConformantOrganiseRuled organiseSnatch
theorem organiseSnatchPrecedenceMoves : (organiseSnatch trivialPolicyCascade snatchSelected snatchRepo snatchAdmitted).precedenceBefore ≠ (organiseSnatch trivialPolicyCascade snatchSelected snatchRepo snatchAdmitted).precedenceAfter
theorem organiseSnatchActingAndScoreMove : (organiseSnatch trivialPolicyCascade snatchSelected snatchRepo snatchAdmitted).actingOrderBefore ≠ (organiseSnatch trivialPolicyCascade snatchSelected snatchRepo snatchAdmitted).actingOrderAfter ∧ (organiseSnatch trivialPolicyCascade snatchSelected snatchRepo snatchAdmitted).scoreBefore = 3 ∧ (organiseSnatch trivialPolicyCascade snatchSelected snatchRepo snatchAdmitted).scoreAfter = -5 ∧ (organiseSnatch trivialPolicyCascade snatchSelected snatchRepo snatchAdmitted).scoreBefore ≠ (organiseSnatch trivialPolicyCascade snatchSelected snatchRepo snatchAdmitted).scoreAfter
theorem organiseSnatchEdge02 : (organiseSnatch trivialPolicyCascade snatchSelected snatchRepo snatchAdmitted).organisedEdges 0 2
theorem snatchPrecedencePerm : snatchPrecedenceBefore.Perm snatchPrecedenceAfter
theorem snatchActingOrderNotPerm : ¬ snatchActingOrderBefore.Perm snatchActingOrderAfter
theorem organiseSnatchRecordedEdgesSansO3 : ConformantOrganiseRuledSansO3 organiseSnatchRecordedEdges
theorem organiseSnatchRecordedEdge1822 : (organiseSnatchRecordedEdges trivialPolicyCascade snatchSelected snatchRepo snatchAdmitted).organisedEdges 18 22
theorem organiseSnatchNotEdge1822 : ¬ (organiseSnatch trivialPolicyCascade snatchSelected snatchRepo snatchAdmitted).organisedEdges 18 22
theorem organiseSnatchNotEdge1018 : ¬ (organiseSnatch trivialPolicyCascade snatchSelected snatchRepo snatchAdmitted).organisedEdges 10 18
theorem organiseSnatchRefusesAddedEndpoint (t : Cascade Unit) (sel adm : Set Nat) (repo : Repository Nat) (u v : Nat) (h : u ∈ snatchAdded ∨ v ∈ snatchAdded) : ¬ (organiseSnatch t sel repo adm).organisedEdges u v
theorem snatchInAdded_iff (n : Nat) : snatchInAdded n = true ↔ n ∈ snatchAdded
theorem snatchOnlyEdge02SurvivesTheSubtraction : snatchOrganisedEdgeList.filter (fun e => !(snatchInAdded e.1 || snatchInAdded e.2)) = [(0, 2)]
theorem snatchOrganisedEdgeListLength : snatchOrganisedEdgeList.length = 10
theorem organiseSnatchRecordedEdgesNotConformant : ¬ ConformantOrganiseRuled organiseSnatchRecordedEdges
theorem organiseSnatchActingFlatConformant : ConformantOrganiseRuled organiseSnatchActingFlat
theorem organiseSnatchActingFlatScoreFlatSansO4 : ConformantOrganiseRuledSansO4 organiseSnatchActingFlatScoreFlat
theorem organiseSnatchActingFlatScoreFlatNotConformant : ¬ ConformantOrganiseRuled organiseSnatchActingFlatScoreFlat
```

### F12SupportArm.lean

Commit `60cab98ab305a1546013224754889307d9216216`; SHA-256 `da30160e05bacd8f30c0a3e9b424fdc2267de23e9693808416a236d729831561`.

Declared names: `provSupport`, `mem_provSupport_iff`, `SupportOrganiseType`, `ConformantOrganiseSupport`, `organiseSupport`, `organiseSupportConformant`, `organiseSupportMirror`, `supportMirrorNotConformant`, `supportThirdOriginDetermined`, `organiseSupportZaifAdmitsNine`, `organiseSupportZaifNodes`, `supportRecordedAdmissions`, `ConformantOrganiseSupportSansAttr`, `supportSplitSurvivesWithoutOattr`, `attributingOfSupport`, `attributingOfSupport_conformant`, `supportIndicator`, `provSupport_supportIndicator`, `supportOfAttributing`, `supportOfAttributing_conformant`, `attributingBlindToRuleIdentity`, `zaifProvenanceStopRelabelled`, `stopRelabelledSameSupport`, `stopRelabelledDiffersAtEleven`, `stopRelabelledAdmissionsIndistinguishable`, `organiseAttributingStopMarked`, `organiseAttributingStopMarkedConformant`, `stopMarkedRecordedFlatCounterfactualVisible`, `supportZaifEdgeNonVacuous`, `supportRecordedAdmissionsFromProvenance`, `supportIndicatorAt`, `provSupport_supportIndicatorAt`, `supportOfAttributingAt`, `supportOfAttributingAt_conformant`, `supportOfAttributingAtRecordedRule_conformant`, `stopMarkedStopSubsetSupport`, `stopMarkedAgreeOnNodes`.

```lean
theorem mem_provSupport_iff {P Rule : Type*} (prov : P → Option Rule) (p : P) : p ∈ provSupport prov ↔ (prov p).isSome
theorem organiseSupportConformant {Policy P : Type*} : ConformantOrganiseSupport (organiseSupport (Policy
theorem supportMirrorNotConformant : ¬ ConformantOrganiseSupport (organiseSupportMirror (Policy
theorem supportThirdOriginDetermined {Policy P : Type*} {f g : SupportOrganiseType Policy P} (hf : ConformantOrganiseSupport f) (hg : ConformantOrganiseSupport g) (t : Cascade Policy) (sel : Set P) (repo : Repository P) (adm : Set P) : (f t sel repo adm).admittedBy = (g t sel repo adm).admittedBy
theorem organiseSupportZaifAdmitsNine : (organiseSupport (Policy
theorem organiseSupportZaifNodes : (organiseSupport (Policy
theorem supportRecordedAdmissions {Policy : Type*} {f : SupportOrganiseType Policy Nat} (hf : ConformantOrganiseSupport f) (t : Cascade Policy) : (f t d1Selected d1Repo d1Admitted).admittedBy = d1Admitted
theorem supportSplitSurvivesWithoutOattr : ConformantOrganiseSupportSansAttr (organiseSupport (Policy
theorem attributingOfSupport_conformant {Policy P Rule : Type*} {f : SupportOrganiseType Policy P} (hf : ConformantOrganiseSupport f) : ConformantOrganiseAttributing (attributingOfSupport (Rule
theorem provSupport_supportIndicator {P : Type*} (adm : Set P) : provSupport (supportIndicator adm) = adm
theorem supportOfAttributing_conformant {Policy P : Type*} {f : AttributingOrganiseType Policy P Unit} (hf : ConformantOrganiseAttributing f) : ConformantOrganiseSupport (supportOfAttributing f)
theorem attributingBlindToRuleIdentity {Policy P Rule : Type*} {f : AttributingOrganiseType Policy P Rule} (hf : ConformantOrganiseAttributing f) (t : Cascade Policy) (sel : Set P) (repo : Repository P) (prov1 prov2 : P → Option Rule) (hs : provSupport prov1 = provSupport prov2) : (f t sel repo prov1).admittedBy = (f t sel repo prov2).admittedBy
theorem stopRelabelledSameSupport : provSupport zaifProvenanceStopRelabelled = provSupport zaifProvenance
theorem stopRelabelledDiffersAtEleven : zaifProvenanceStopRelabelled 11 ≠ zaifProvenance 11
theorem stopRelabelledAdmissionsIndistinguishable {Policy : Type*} {f : AttributingOrganiseType Policy Nat ZaifPolicyRule} (hf : ConformantOrganiseAttributing f) (t : Cascade Policy) : (f t d1Selected d1Repo zaifProvenanceStopRelabelled).admittedBy = d1Admitted ∧ (f t d1Selected d1Repo zaifProvenance).admittedBy = d1Admitted
theorem organiseAttributingStopMarkedConformant {Policy P : Type*} : ConformantOrganiseAttributing (organiseAttributingStopMarked (Policy
theorem stopMarkedRecordedFlatCounterfactualVisible : (organiseAttributingStopMarked (Policy
theorem supportZaifEdgeNonVacuous : (organiseSupport (Policy
theorem supportRecordedAdmissionsFromProvenance {Policy : Type*} {f : SupportOrganiseType Policy Nat} (hf : ConformantOrganiseSupport f) (t : Cascade Policy) : (f t d1Selected d1Repo (provSupport zaifProvenance)).admittedBy = d1Admitted
theorem provSupport_supportIndicatorAt {P Rule : Type*} (r : Rule) (adm : Set P) : provSupport (supportIndicatorAt r adm) = adm
theorem supportOfAttributingAt_conformant {Policy P Rule : Type*} (r : Rule) {f : AttributingOrganiseType Policy P Rule} (hf : ConformantOrganiseAttributing f) : ConformantOrganiseSupport (supportOfAttributingAt r f)
theorem supportOfAttributingAtRecordedRule_conformant {Policy P : Type*} {f : AttributingOrganiseType Policy P ZaifPolicyRule} (hf : ConformantOrganiseAttributing f) : ConformantOrganiseSupport (supportOfAttributingAt ZaifPolicyRule.widenTheCascadeOnlyOnEvidence f)
theorem stopMarkedStopSubsetSupport {P : Type*} (prov : P → Option ZaifPolicyRule) : {p | prov p = some ZaifPolicyRule.haltOnBudget} ⊆ provSupport prov
theorem stopMarkedAgreeOnNodes {Policy P : Type*} (t : Cascade Policy) (sel : Set P) (repo : Repository P) (prov : P → Option ZaifPolicyRule) : (organiseAttributingStopMarked t sel repo prov).nodes = (organiseAttributing t sel repo prov).nodes
```

### F12ThirdSnatchExemplar.lean

Commit `e51b4ea542428512824549b04b606fbb855e24d2`; SHA-256 `450eac5940b909059afc96ed55fb3733d2ea339fecada54065e690a9c0d05607`.

Declared names: `snatchG1CautiousSelected`, `snatchG1CautiousAdded`, `organiseSnatchG1Cautious`, `organiseSnatchG1CautiousConformant`, `snatchG1CautiousRecordedEdges`, `snatchG1CautiousInAdded`, `snatchG1CautiousNoRecordedEdgeSurvives`, `snatchG1CautiousRecordedEdgeCount`, `organiseSnatchG1CautiousNotEdge518`.

```lean
theorem organiseSnatchG1CautiousConformant : ConformantOrganiseRuled organiseSnatchG1Cautious
theorem snatchG1CautiousNoRecordedEdgeSurvives : snatchG1CautiousRecordedEdges.filter (fun e => !(snatchG1CautiousInAdded e.1 || snatchG1CautiousInAdded e.2)) = []
theorem snatchG1CautiousRecordedEdgeCount : snatchG1CautiousRecordedEdges.length = 3
theorem organiseSnatchG1CautiousNotEdge518 : ¬ (organiseSnatchG1Cautious trivialPolicyCascade snatchG1CautiousSelected snatchRepo ∅).organisedEdges 5 18
```

### FindDraft.lean

Commit `287a2d3d3655209c7db774a044136f4805a10a44`; SHA-256 `e8190de7c4d6428ebb56fd301972b460fb5342dff59fe00263181d4ed340b49a`.

Declared names: `Route`, `FindQuery`, `Warrant`, `Receipt`, `FindResult`, `find`, `find_containment`, `find_receipted`, `find_warrant_standsOn`, `find_falsifiable`.

```lean
axiom find {State : Type u} {P : Type v} {Extra : Type w} (q : FindQuery State P) (R : Repository P) : FindResult q R Extra
theorem find_containment {State : Type u} {P : Type v} {Extra : Type w} (q : FindQuery State P) (R : Repository P) : ∀ p ∈ (find (Extra
theorem find_receipted {State : Type u} {P : Type v} {Extra : Type w} (q : FindQuery State P) (R : Repository P) : ∀ p ∈ (find (Extra
theorem find_warrant_standsOn {State : Type u} {P : Type v} {Extra : Type w} (q : FindQuery State P) (R : Repository P) : ∀ p (hp : p ∈ (find (Extra
theorem find_falsifiable {State : Type u} {P : Type v} {Extra : Type w} (q : FindQuery State P) (R : Repository P) : ∀ p ∈ (find (Extra
```

### FoldCFoldedNegative.lean

Commit `0091ca89aa3b926dfd8105c1443dae071171988e`; SHA-256 `2ba43ff4f48d301b2987ded41c4b41686e0aa4f6a6ee51a588e1ccbd779330a9`.

Declared names: `foldC_unfolded_layer_leaves_base`.

```lean
theorem foldC_unfolded_layer_leaves_base : foldC ruledBase [addLayer] = ruledBase
```

### FoldCOrderNegative.lean

Commit `0091ca89aa3b926dfd8105c1443dae071171988e`; SHA-256 `b174fc1a38696257a0169521d59811e36990123ebfd9dd16cccbf21f8e6a3d8c`.

Declared names: `foldC_order_insensitive`.

```lean
theorem foldC_order_insensitive : foldC ruledBase [addLayer, doubleLayer] = foldC ruledBase [doubleLayer, addLayer]
```

### FoldCWitness.lean

Commit `fcd1261c303c2beca08a6812eba4a7ce2e83d722`; SHA-256 `786ef673e884b6783682dd0856fc6670cf814dfacdc13c7f4c772bc2ddfdfb04`.

Declared names: `ruledBase`, `runtimeFoldedLayers`, `foldC_runtimeFoldedLayers_eq_base`, `addLayer`, `doubleLayer`, `foldC_order_is_observable`.

```lean
theorem foldC_runtimeFoldedLayers_eq_base : foldC ruledBase runtimeFoldedLayers = ruledBase
theorem foldC_order_is_observable : foldC ruledBase [addLayer, doubleLayer] (organisationOutcome .groundedChange) = 8 ∧ foldC ruledBase [doubleLayer, addLayer] (organisationOutcome .groundedChange) = 7
```

### FoldEscrowRecordWitness.lean

Commit `e48a3158efa23533c42c53b0d7b5205e9a36b59a`; SHA-256 `066f126fb05e0c556158e97430f4ed16be9c54e92c173854839837995c1ccf09`.

Declared names: `matching`, `matching_is_reconstructible`, `mismatching`, `mismatch_is_not_reconstructible`.

```lean
theorem matching_is_reconstructible : matching.reconstructible (fun n => n + 0) (fun n => n * 2)
theorem mismatch_is_not_reconstructible : ¬ mismatching.reconstructible (fun n => n + 0) (fun n => n * 2)
```

### FoldNegative.lean

Commit `af58bec5ed639b6fefc62ec230e4b29d13aeec07`; SHA-256 `aa7d008c8936632aed81ff53d12263ae21edb30340d81adb589efd2bd642673f`.

Declared names: `malformed`.

### FoldWitness.lean

Commit `ba845ea7b1bc76c81e835641470c9d4605cf82ca`; SHA-256 `d5792b8ab5ea48ca9d61a4ed0e01828aef910d77302f74f35700e3e0315409ca`.

Declared names: `RecordedWiring`, `RecordedPolicyHole`, `recordedFold`, `recordedFoldFields`.

```lean
theorem recordedFoldFields : recordedFold.wiring.nodeCount = 5 ∧ recordedFold.wiring.hyperedgeCount = 5 ∧ recordedFold.wiring.terminalCount = 1 ∧ recordedFold.coverageScoreDelta = some (-1) ∧ recordedFold.policyHoles.length = 3
```

### GainChain.lean

Commit `b78ebc428b74fb99fd83349d6c86dfa192707bc6`; SHA-256 `d8fec82564a5a05fdc427e8c2cd16113537a6c842ad5a27640d712a0f33f4c3f`.

Declared names: `Tick`, `Mission`, `Producer`, `Measurement`, `CorpusLoadPolicy`, `RealizedOutcome`, `FoldOccurrence`, `ProducerSelection`, `threadedIdentity`, `inhabitedHandle`, `durableBeforeFold`, `declaredDomain`, `dischargedPrecondition`, `domainNotNarrowed`, `typedAbsence`, `gainAdvances`, `gainChainSound`, `foldCompliant`, `bayesianMission`, `groundedMission`, `coverageSelection`, `groundedSelection`, `substitution_2026_07_08_narrows_domain_is_refused`, `inert_until_data_is_not_a_discharged_precondition`, `loadYieldsOutcome`, `foldOf`, `pendingOutcome`, `strict_load_swallowed_gives_a_silent_success`, `clockSplitOutcome`, `clockSplitOccurrence`, `clockSplitSelection`, `two_clocks_break_threaded_identity`, `domainMismatchOutcome`, `domainMismatchOccurrence`, `domainMismatchSelection`, `domain_mismatch_is_a_record_not_a_silence`, `soundOutcome`, `soundOccurrence`, `soundSelection`, `gain_chain_sound_nonvacuous`.

```lean
theorem substitution_2026_07_08_narrows_domain_is_refused : ¬ domainNotNarrowed coverageSelection groundedSelection
theorem inert_until_data_is_not_a_discharged_precondition (occurrence : FoldOccurrence) (selection : ProducerSelection) (h : selection.preconditionDischarged = false) : ¬ gainChainSound occurrence selection
theorem strict_load_swallowed_gives_a_silent_success : ¬ inhabitedHandle (foldOf .strict 1 pendingOutcome) ∧ inhabitedHandle (foldOf .degrading 1 pendingOutcome)
theorem two_clocks_break_threaded_identity : ¬ gainChainSound clockSplitOccurrence clockSplitSelection
theorem domain_mismatch_is_a_record_not_a_silence : foldCompliant domainMismatchOccurrence domainMismatchSelection ∧ ¬ gainChainSound domainMismatchOccurrence domainMismatchSelection
theorem gain_chain_sound_nonvacuous : gainChainSound soundOccurrence soundSelection
```

### GenerativeModelNegative.lean

Commit `af58bec5ed639b6fefc62ec230e4b29d13aeec07`; SHA-256 `242e34a0b296e143eb33bb397780a2dfbb312ff75ac584b4eea63d75f6edb369`.

Declared names: `Observation`, `State`, `OtherState`, `Action`, `Policy`, `passOutcome`, `wrongObservation`, `transition`, `policyPrior`, `miswired`.

### GenerativeModelWitness.lean

Commit `45f3e4eeb7fd61234ad0b5732c7c2730c8c03cc5`; SHA-256 `62dc0cdf74629d031fd6ee9a2ec397a4932ca3ba1b13015aa6a29ebac324d88f`.

Declared names: `TestObservation`, `TestState`, `OtherState`, `TestAction`, `TestPolicy`, `passOutcome`, `failOutcome`, `observation`, `wrongObservation`, `transition`, `policyPrior`, `model`, `FactorReference`, `factorReference`, `factorFixture`.

```lean
theorem factorFixture : generativeFactorMass model .ready .run .ready passOutcome .inspect = factorReference.jointFactorMass
```

### HaveWantArrowNegative.lean

Commit `af58bec5ed639b6fefc62ec230e4b29d13aeec07`; SHA-256 `c03e27b3b10399000ad380e128ec87a3576ebb1f901892f343cbf7a60cf886a2`.

Declared names: `Endpoint`, `left`, `malformedRight`, `malformed`.

### HaveWantArrowWitness.lean

Commit `ba845ea7b1bc76c81e835641470c9d4605cf82ca`; SHA-256 `92fcf238543ffaa13a61e7a751b073bfe14bb130469e14bed7c9bd6190322a00`.

Declared names: `Endpoint`, `first`, `second`, `recordedComposition`, `recordedCompositionEndpoints`.

```lean
theorem recordedCompositionEndpoints : first.target = second.source
```

### Holes.lean

Commit `bf79f988b3131701ff9ed257d371cc45dd5eea5b`; SHA-256 `4dc0a76b9999d09b2ab49c932117e5b7dcfec523e5e61735b3a84191229cd02b`.

Declared names: `Pattern`, `Cascade`, `HaveWantArrowState`, `HaveWantArrow`, `HaveWantArrowComposition`, `Fold`, `FoldEscrowRecord`, `FoldEscrowRecord.reconstructible`, `ControlVocabulary`, `ControlPolicy`, `AlivenessFactor`, `aliveness`, `ActGateVerdict`, `actGate`, `Click`, `Attempt`, `Cohort`, `Repository`, `Tension`, `InformationState`, `DecisionRule`, `Vertex`, `Outcome`, `C`, `ExpectedFreeEnergyValue`, `G`, `IsArgminOn`, `nonDegenerate`, `AblationRow`, `AblationTable`, `nonDegenerateAblationLaw`, `RecordedSnatchPolicy`, `recordedSnatchPolicies`, `recordedSnatchG`, `recordedSnatchRisk`, `wmRecordedAblationNonDegenerate`, `TypedAbsence`, `FindRoute`, `FindClauseKind`, `FindQuery`, `FindWarrant`, `FindCitation`, `Receipt`, `Receipt.nonSelfCertifying`, `FindResult`, `FindReceiptRow`, `FindReceiptTable`, `find`, `findF1ContainmentLaw`, `FindReceiptExpectation`, `FindResult.contentF2`, `FindCitation.validates`, `FindResult.citationF3`, `FindResult.exclusionF4`, `FindResult.discriminatingF4`, `findF2ReceiptedLaw`, `findF3NonSelfCertifyingLaw`, `findF4FalsifiableLaw`, `findF1Containment`, `findF2Receipted`, `findF3NonSelfCertifying`, `findF4Falsifiable`, `SnatchPattern`, `snatchRepository`, `FindSnatchScenario`, `findSnatchZeroMass`, `FindSnatchRowLit`, `FindSnatchRowLit.toRow`, `findSnatchRounds`, `findSnatchScenarios`, `listNil_of_setOf_mem_eq_empty`, `f1Ok`, `findF1Containment_toRow`, `f2Ok`, `findF2Receipted_toRow`, `f3Ok`, `findF3NonSelfCertifying_toRow`, `f4Ok`, `findF4Falsifiable_toRow`, `wmFindSnatchF1Containment`, `wmFindSnatchF2Receipted`, `wmFindSnatchF3NonSelfCertifying`, `wmFindSnatchF4Falsifiable`, `ReachOutside`, `fastForward`, `CascadeDiff`, `organise`, `cascadeFixtureSelected`, `cascadeFixtureAuthored`, `wmCascadeDiffFixture`, `reachOutside_to_reach`, `organiseO1NodesRecorded`, `organiseO2AuthoredReachability`, `organiseO3FastForward`, `organiseO4PrecedenceGovernance`, `zaifSelected`, `zaifAdmitted`, `zaifNodes`, `zaifAuthored`, `wmZaifCascadeDiffFixture`, `organiseO1NodesRecordedZaif`, `organiseO2AuthoredReachabilityZaif`, `organiseO3FastForwardZaif`, `organiseO3FastForwardOverSelectedFails`, `Layer`, `Claim`, `Witness`, `independent`, `IndependenceVerdict`, `independenceVerdict`, `r9CheckerSound`, `r9VerdictConsultsChecker`, `DeclarationSource`, `VerdictRow`, `VerdictRow.inDeclaredPart`, `VerdictTable`, `r9VerdictsSound`, `r9PerRowDeclarations`, `wmVerdictRowIds`, `wmVerdictsLedgerAlone`, `declaredVerdictRow`, `wmVerdictsDeclared`, `r9WmVerdictsSound`, `r9WmPerRowDeclarations`, `r9TwoRunCensus`, `ValueEvidencePolicy`, `valueEvidenceRequiresL2`, `DeliveryGuarantee`, `Retry`, `Delivery`, `Handoff`, `Workflow`, `R2Tick`, `r2WellFormed`, `r2ContractCensus`, `Channel`, `Channel.all`, `R2TickLit`, `wmTraceR2ContentPin`, `r2CompleteTick`, `r2MissingAnnotationTick`, `wmTraceR2`, `r2ContractCensusWmTrace`, `FreeEnergyShape`, `R8Tick`, `R8Tick.freeEnergyShape`, `R8Disposition`, `r8Disposition`, `r8Census`, `R8TickLit`, `wmTraceR8`, `r8CensusWmTrace`, `r8EraBoundary`, `ProbabilityKernel`, `PredictiveOutcomeKernel`, `ParameterPriorKernel`, `ParameterPosteriorKernel`, `TransitionKernel`, `PolicyPriorKernel`, `PreferenceDistribution`, `NonnegativeReal`, `GenerativeModel`, `generativeFactorMass`, `observationKernel`, `BeliefState`, `ObservationVector`, `predictionError`, `PrecisionMap`, `VariationalFreeEnergyValue`, `variationalFreeEnergy`, `observationKernelRowMass`, `beliefUpdate`, `predictiveOutcomeRisk`, `observationEntropy`, `ambiguity`, `expectedFreeEnergy`, `G_eq_expectedFreeEnergy`, `ExpectedInformationGainValue`, `parameterInformationGain`, `expectedInformationGain`, `DirichletConcentrations`, `logMultivariateBeta`, `ModelReductionFreeEnergyChange`, `modelReductionFreeEnergyChange`, `bayesFactorThreshold`, `softmax`, `bayesianModelReduction`, `modelUncertaintyBonus`, `EIGCounterPolicy`, `EIGCounterParameter`, `EIGCounterObs`, `eigCounterOutcome`, `eigCounterPredictive`, `eigCounterPrior`, `eigCounterPosterior`, `eigCounterPositivePrior`, `modelUncertaintyAndEIG`, `cascadeGrainPi`, `PreferenceSource`, `PreferenceLayerRecord`, `wmPreferenceStack2026_08_30`, `wmStackDeclaredPurpose`, `preferenceStackRecorded`, `PreferenceLayer`, `foldC`, `PreferenceStack`, `PreferenceSpineDeclaration`, `PreferenceConstantCensusRow`, `preferenceConstantCensus`, `freePreferenceConstants`, `freePreferenceConstants_eq`, `preferenceStackLiveRecorded`, `machineHasNoC`, `TickRunRecord`, `wmRunsOnce`, `RouteHop`, `wmRunConformsToWiring`, `RouteNode`, `WiringEdge`, `RetirementGrounds`, `RetiredWiringEdge`, `figureDrawnEdges`, `figureMeasuredEdges`, `figureRetiredEdges`, `edgeMem`, `retirementGroundsOf`, `HopClass`, `classifyHop`, `routeHops`, `s5Routes`, `s5Hops`, `s5UnfiredDrawnEdges`, `wmS5RunConformsToDrawnWiring`, `wmS5RouteCensus`, `re5Routes`, `re5Hops`, `re5UnfiredDrawnEdges`, `wmRe5RunConformsToDrawnWiring`, `wmRe5RouteCensus`, `SelectionTie`, `SelectionTie.chosenByTiebreak`, `s5SelectionTies`, `wmS5SelectionDiscrimination`, `wmS5SelectionTieCensus`, `re5SelectionTies`, `wmRe5SelectionDiscrimination`, `wmRe5SelectionTieCensus`, `EnactedVsSelected`, `EnactedVsSelected.agrees`, `firstFlightsVsBayesianStructureLearning`, `wmTrace20260704EnactedVsSelected`, `wmTrace20260705EnactedVsSelected`, `enactedVsSelectedComparable`, `enactedActionEqualsSelected`, `enactedEqualsSelectedWhenRankOneGated`, `dirichletAccumulationImportAbsent`, `policyPrecisionIsGammaFromBeta`, `policyPosteriorImportsPolicyF`, `WitnessLayerRow`, `WitnessLayerTable`, `IllFormedTick`, `IllFormedList`, `R8DispositionEvidence`, `Era`, `ShapeTally`, `EraSummary`, `EraSummary.meanPrecision`, `EraSummary.uniform`, `EraTable`, `mkClosed`, `mkWitnessedClosed`, `mkHole`, `mkRefutedByRecord`, `mkClosedUnderCriterion`, `mkRefused`, `closedDeclarations`, `holeDeclarations`, `registry`, `main`.

```lean
theorem wmRecordedAblationNonDegenerate : nonDegenerateAblationLaw recordedSnatchPolicies (fun _ : Unit => recordedSnatchG) (fun _ : Unit => recordedSnatchRisk)
opaque find {State P Clause AsOf TextCitation : Type*} : FindQuery State P → (R : Repository P) → FindResult P Clause AsOf TextCitation R
private theorem listNil_of_setOf_mem_eq_empty {α : Type*} {l : List α} (h : {a | a ∈ l} = (∅ : Set α)) : l = []
theorem findF1Containment_toRow {r : FindSnatchRowLit} (h : r.f1Ok = true) : findF1Containment r.toRow
theorem findF2Receipted_toRow {r : FindSnatchRowLit} (h : r.f2Ok = true) : findF2Receipted r.toRow
theorem findF3NonSelfCertifying_toRow {r : FindSnatchRowLit} (h : r.f3Ok = true) : findF3NonSelfCertifying r.toRow
theorem findF4Falsifiable_toRow {r : FindSnatchRowLit} (h : r.f4Ok = true) : findF4Falsifiable r.toRow
theorem wmFindSnatchF1Containment : ∀ row ∈ findSnatchRounds, findF1Containment row.toRow
theorem wmFindSnatchF2Receipted : ∀ row ∈ findSnatchRounds, findF2Receipted row.toRow
theorem wmFindSnatchF3NonSelfCertifying : ∀ row ∈ findSnatchRounds, findF3NonSelfCertifying row.toRow
theorem wmFindSnatchF4Falsifiable : ∀ row ∈ findSnatchScenarios, findF4Falsifiable row.toRow
private theorem reachOutside_to_reach {P : Type*} {selected : Set P} {r : P → P → Prop} {u v : P} : ReachOutside selected r u v → Reach r u v
theorem G_eq_expectedFreeEnergy {PolicyIndex : Type*} {Obs : Vertex → Type*} (risk eig : PolicyIndex → ℝ) (Q : PredictiveOutcomeKernel PolicyIndex Obs) (Cdist : PreferenceDistribution Obs) (positivePreference : ∀ π o, o ∈ Q.support π → 0 < Cdist.mass () o) (ambiguity : PolicyIndex → ℝ) (risk_eq : ∀ π, risk π = predictiveOutcomeRisk Q Cdist positivePreference π) (ambiguity_eq : ∀ π, ambiguity π = -eig π) : ∀ π, G risk eig π = expectedFreeEnergy Q Cdist positivePreference ambiguity π
private theorem eigCounterPositivePrior : ∀ π o θ, θ ∈ eigCounterPosterior.support (π, o) → 0 < eigCounterPrior.mass π θ
theorem preferenceStackRecorded : (wmPreferenceStack2026_08_30.all fun l => l.author != "" && l.basis != "") = true
theorem freePreferenceConstants_eq : freePreferenceConstants = [.vertexLocalC]
theorem wmS5RunConformsToDrawnWiring : runConformsToDrawnWiring s5Routes
theorem wmS5RouteCensus : s5Routes.length = 4 ∧ s5Hops.length = 36 ∧ s5Hops.dedup.length = 9 ∧ (s5Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.drawn))).length = 2 ∧ (s5Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.routeMeasured))).length = 5 ∧ (s5Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.excludedDependencyGrain))).length = 1 ∧ (s5Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.rulingUnrealised))).length = 1 ∧ (s5Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.refutation))).length = 0 ∧ (s5Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.unmapped))).length = 0 ∧ figureDrawnEdges.length = 22 ∧ s5UnfiredDrawnEdges.length = 19
theorem wmRe5RunConformsToDrawnWiring : runConformsToDrawnWiring re5Routes
theorem wmRe5RouteCensus : re5Routes.length = 4 ∧ re5Hops.length = 36 ∧ re5Hops.dedup.length = 9 ∧ (re5Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.drawn))).length = 2 ∧ (re5Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.routeMeasured))).length = 5 ∧ (re5Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.excludedDependencyGrain))).length = 1 ∧ (re5Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.rulingUnrealised))).length = 1 ∧ (re5Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.refutation))).length = 0 ∧ (re5Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.unmapped))).length = 0 ∧ figureDrawnEdges.length = 22 ∧ re5UnfiredDrawnEdges.length = 19
theorem wmS5SelectionDiscrimination : ¬ selectionDiscriminates s5SelectionTies
theorem wmS5SelectionTieCensus : s5SelectionTies.length = 4 ∧ (s5SelectionTies.filter (fun t => t.chosenByTiebreak)).length = 4 ∧ (s5SelectionTies.map (fun t => t.tieCount)).foldl max 0 = 55 ∧ (s5SelectionTies.map (fun t => t.chosenRank)).foldl max 0 = 123 ∧ (s5SelectionTies.map (fun t => t.fieldSize)).foldl max 0 = 145 ∧ (s5SelectionTies.map (fun t => t.widestPlateauNotChosen)).foldl max 0 = 6
theorem wmRe5SelectionDiscrimination : selectionDiscriminates re5SelectionTies
theorem wmRe5SelectionTieCensus : re5SelectionTies.length = 4 ∧ (re5SelectionTies.filter (fun t => t.chosenByTiebreak)).length = 0 ∧ (re5SelectionTies.map (fun t => t.tieCount)).foldl max 0 = 1 ∧ (re5SelectionTies.map (fun t => t.chosenRank)).foldl max 0 = 1 ∧ (re5SelectionTies.map (fun t => t.fieldSize)).foldl max 0 = 146 ∧ (re5SelectionTies.map (fun t => t.widestPlateauNotChosen)).foldl max 0 = 56
```

### LocalPreferenceModule.lean

Commit `b5870b73844b6d1fa585b588a175d83767be6d6d`; SHA-256 `187ed6bf353ef696700823db2e0ec6eee5b80d883499e2283f5ff4a55f51c9ba`.

Declared names: `ExactTable`, `ExactTable.kernel`, `Module`, `Module.C`, `preserves_named_zero`, `BinaryObs`, `yes`, `no`, `binaryTable`, `softBinary`, `softBinary_positive`.

```lean
theorem preserves_named_zero {O : Type} (t : ExactTable O) (o : O) (h : t.mass o = 0) : t.kernel.mass () o = 0
theorem softBinary_positive (p : ℚ) (hlo : 1 / 2 < p) (hhi : p < 1) (o : Outcome BinaryObs) (ho : o ∈ (softBinary p hlo hhi).support ()) : 0 < (softBinary p hlo hhi).mass () o
```

### LogMultivariateBetaWitness.lean

Commit `b068089813f5873170b1cf78d7739d40851b90db`; SHA-256 `5bfc43eaa475cb94063459c1665c592d2b5abe290d77ccb5630d3b1b342e847a`.

Declared names: `alpha11`, `alpha21`, `BetaReference`, `betaReference`, `unit_pair`, `asymmetric_pair`.

```lean
theorem unit_pair : logMultivariateBeta alpha11 = betaReference.firstExpectedLog
theorem asymmetric_pair : logMultivariateBeta alpha21 = betaReference.secondExpectedLog
```

### MachineAction.lean

Commit `461720489008ecadc5eef01f4cc1c0c323e7b86f`; SHA-256 `473182ceb96aba566ff2803d1fbf6f13154d3713d3c95d314180a676574bc692`.

Declared names: `Candidate`, `StrategicLaw`, `SelectionBoundary`, `firstArgmax`, `lastArgmax`, `strategicCandidates`, `actuationCandidates`, `machineAction`, `headAndPosteriorDisagree`, `strategicAndActuationDisagree`, `tiedArgmaxesDisagree`, `requestedPosteriorCanBecomeHead`, `selectionScore`, `posteriorWeight`, `posteriorOrderIsScoreOrder`, `fullScoreIsPosteriorArgmax`, `noOpCandidateSetsDiffer`, `enactedAction`, `selectedAndEnactedDisagree`, `productionDefaultBoundary`, `productionDefaultLaw`, `registryLaw`, `defaultRuleIsNotTheRegistryRule`, `defaultSelectionIsTheHead`, `habitPriorAloneMovesTheChoice`, `noOpExclusionMovesTheChoice`, `abstains`, `noOpArgmaxAbstains`, `requestedPosteriorDependsOnFPiEntered`.

```lean
theorem headAndPosteriorDisagree : let ranked
theorem strategicAndActuationDisagree : machineAction .strategicRecommendation .fullScorePosterior true true [⟨0, 0, false⟩, ⟨1, 0, false⟩] [⟨0, 1, false⟩, ⟨1, 1, false⟩] ≠ machineAction .actuation .controllerHead true true [⟨0, 0, false⟩, ⟨1, 0, false⟩] [⟨0, 1, false⟩, ⟨1, 1, false⟩]
theorem tiedArgmaxesDisagree : firstArgmax [⟨0, 1, false⟩, ⟨1, 1, false⟩] = some ⟨0, 1, false⟩ ∧ lastArgmax [⟨0, 1, false⟩, ⟨1, 1, false⟩] = some ⟨1, 1, false⟩
theorem requestedPosteriorCanBecomeHead : machineAction .strategicRecommendation .fullScorePosterior false false [⟨0, 0, false⟩, ⟨1, 0, false⟩] [⟨0, 0, false⟩, ⟨1, 3, false⟩] = some ⟨0, 0, false⟩
theorem posteriorOrderIsScoreOrder {z s t : ℝ} (hz : 0 < z) : posteriorWeight z s < posteriorWeight z t ↔ s < t
theorem fullScoreIsPosteriorArgmax {z : ℝ} (hz : 0 < z) : selectionScore 0 0 0 1 < selectionScore 1 3 0 1 ∧ posteriorWeight z (selectionScore 0 0 0 1) < posteriorWeight z (selectionScore 1 3 0 1)
theorem noOpCandidateSetsDiffer : strategicCandidates [⟨0, 1, true⟩, ⟨1, 0, false⟩] = [⟨1, 0, false⟩] ∧ actuationCandidates [⟨0, 1, true⟩, ⟨1, 0, false⟩] = [⟨0, 1, true⟩, ⟨1, 0, false⟩]
theorem selectedAndEnactedDisagree : let ranked
theorem defaultRuleIsNotTheRegistryRule : productionDefaultLaw ≠ registryLaw ∧ productionDefaultBoundary ≠ SelectionBoundary.strategicRecommendation
theorem defaultSelectionIsTheHead (ranked scored : List Candidate) : machineAction productionDefaultBoundary productionDefaultLaw true false ranked scored = ranked.head?
theorem habitPriorAloneMovesTheChoice : let ranked
theorem noOpExclusionMovesTheChoice : let ranked
theorem noOpArgmaxAbstains : abstains 1 1 (1/100) = true
theorem requestedPosteriorDependsOnFPiEntered : let ranked
```

### MachineActionWitness.lean

Commit `461720489008ecadc5eef01f4cc1c0c323e7b86f`; SHA-256 `f89dacbfcc37b1e293683ba34f9a2d7834a83e990a2374d0372ba8bfca9fa114`.

Declared names: `controllerHeadReference`, `posteriorReference`, `posteriorFallbackReference`, `scoreVectorReference`.

```lean
theorem controllerHeadReference : machineAction .strategicRecommendation .controllerHead true false [⟨0, 0, false⟩, ⟨1, 1, false⟩] [⟨0, 0, false⟩, ⟨1, 2, false⟩] = some ⟨0, 0, false⟩
theorem posteriorReference : machineAction .strategicRecommendation .fullScorePosterior true false [⟨0, 0, false⟩, ⟨1, 1, false⟩] [⟨0, 0, false⟩, ⟨1, 2, false⟩] = some ⟨1, 2, false⟩
theorem posteriorFallbackReference : machineAction .strategicRecommendation .fullScorePosterior false false [⟨0, 0, false⟩, ⟨1, 1, false⟩] [⟨0, 0, false⟩, ⟨1, 2, false⟩] = some ⟨0, 0, false⟩
theorem scoreVectorReference : selectionScore 0 0 0 1 = 0 ∧ selectionScore 1 3 0 1 = 2
```

### MachineBeliefState.lean

Commit `3a8e26f61e2ef3f4e96c92c96d39743442133db9`; SHA-256 `da2c648860c1a4ee48f58106c5f7fc60a5150d914bc2089a2440eea801a7521e`.

Declared names: `Status`, `Status.all`, `Entity`, `Posterior`, `machineBeliefState`, `uniformPrior`, `reconcileBeliefCarry`, `survivorKeepsCarried`, `newEntityKeepsFresh`, `carriedOnlyEntityIsDropped`, `coldStartReturnsFresh`, `Normalised`, `uniformPriorIsNormalised`, `carryPreservesNormalisation`, `statusIndexDiffersFromChannelIndex`, `entropy`.

```lean
theorem survivorKeepsCarried (fresh carried : machineBeliefState) (e : Entity) (prior posterior : Posterior) (hf : fresh e = some prior) (hc : carried e = some posterior) : reconcileBeliefCarry fresh carried e = some posterior
theorem newEntityKeepsFresh (fresh carried : machineBeliefState) (e : Entity) (prior : Posterior) (hf : fresh e = some prior) (hc : carried e = none) : reconcileBeliefCarry fresh carried e = some prior
theorem carriedOnlyEntityIsDropped (fresh carried : machineBeliefState) (e : Entity) (posterior : Posterior) (hf : fresh e = none) (hc : carried e = some posterior) : reconcileBeliefCarry fresh carried e = none ∧ carried e ≠ none
theorem coldStartReturnsFresh (fresh : machineBeliefState) : reconcileBeliefCarry fresh (fun _ => none) = fresh
theorem uniformPriorIsNormalised : Normalised uniformPrior
theorem carryPreservesNormalisation (fresh carried : machineBeliefState) (hf : ∀ e p, fresh e = some p → Normalised p) (hc : ∀ e p, carried e = some p → Normalised p) : ∀ e p, reconcileBeliefCarry fresh carried e = some p → Normalised p
theorem statusIndexDiffersFromChannelIndex : Status.all.length ≠ Channel.all.length
```

### MachineBeliefStateWitness.lean

Commit `3a8e26f61e2ef3f4e96c92c96d39743442133db9`; SHA-256 `192c28d164982c476c29dd2e8e5dc164fe7ee29f20b5081be1ae0962ac121d90`.

Declared names: `peaked`, `fresh01`, `carried02`, `carryEqualsNeitherInput`, `only0`, `empty`, `reentryLosesHistory`, `collisionA`, `collisionB`, `collisionDistinct`, `collisionAIsNormalised`, `collisionBIsNormalised`, `peakedIsNormalised`, `collisionSpawnedIsStrictArgmax`, `collisionSameEntropy`, `collisionEntropyReference`, `uniformEntropyReference`, `uniformCoordinates`, `momentPairIsNotSufficient`.

```lean
theorem carryEqualsNeitherInput : reconcileBeliefCarry fresh01 carried02 ≠ fresh01 ∧ reconcileBeliefCarry fresh01 carried02 ≠ carried02
theorem reentryLosesHistory : let t0 : machineBeliefState
theorem collisionDistinct : collisionA ≠ collisionB
theorem collisionAIsNormalised : Normalised collisionA
theorem collisionBIsNormalised : Normalised collisionB
theorem peakedIsNormalised : Normalised peaked
theorem collisionSpawnedIsStrictArgmax (s : Status) (hs : s ≠ Status.spawned) : collisionA s < collisionA .spawned ∧ collisionB s < collisionB .spawned
theorem collisionSameEntropy : entropy collisionA = entropy collisionB
theorem collisionEntropyReference : entropy collisionA = -(1/2 * Real.log (1/2) + 3/10 * Real.log (3/10) + 1/5 * Real.log (1/5))
theorem uniformEntropyReference : entropy uniformPrior = -(7 * (1/7 * Real.log (1/7)))
theorem uniformCoordinates : ∀ s, uniformPrior s = 1/7
theorem momentPairIsNotSufficient : Normalised collisionA ∧ Normalised collisionB ∧ collisionA ≠ collisionB ∧ (∀ s, s ≠ Status.spawned → collisionA s < collisionA .spawned ∧ collisionB s < collisionB .spawned) ∧ entropy collisionA = entropy collisionB
```

### MachineBeliefUpdate.lean

Commit `0e89cc1cb5622b252b43ced54dbfa65959043a5b`; SHA-256 `344afaf5e9c103de711e00989f9be3ccf000d91740a65b840eb56a1b3984bb70`.

Declared names: `ChannelContribution`, `signedWeightedError`, `multichannelDriver`, `singleChannelDriver`, `annealFactor`, `baseWeight`, `eventWeight`, `BeliefEventType`, `eventType`, `inconsistency`, `attributionNorm`, `attributedWeight`, `normalise`, `temperedLikelihood`, `categoricalUpdate`, `kappa`, `kappa_zero`, `kappaZeroIsNoOp`, `zeroTotalInconsistencyMovesNothing`, `driverMagnitudeSaturates`, `annealTerminates`, `productionStepsDoNotReachAnnealTermination`, `unknownDriverAppliesNoEvent`, `precisionDividesOutInMultichannel`, `precisionScalesSingleChannel`, `registryAdditiveFormIsNotGeneral`, `machineBeliefUpdate`, `machineBeliefUpdateZeroInconsistencyIsNoOp`.

```lean
theorem kappa_zero : kappa 0 = 0
theorem kappaZeroIsNoOp {n : Nat} [NeZero n] (likelihood prior : Fin n → ℝ) (hprior : (∑ i, prior i) = 1) : categoricalUpdate 0 likelihood prior = prior
theorem zeroTotalInconsistencyMovesNothing (eventWeight entityCount health : ℝ) (kind : BeliefEventType) : attributedWeight eventWeight entityCount 0 health kind = 0
theorem driverMagnitudeSaturates : eventWeight 1 0 3 = eventWeight 50 0 3
theorem annealTerminates : annealFactor 3 3 = 0
theorem productionStepsDoNotReachAnnealTermination (step : Nat) (h : step < 3) : step ≠ 3
theorem unknownDriverAppliesNoEvent (step maxSteps : ℝ) : eventWeight ((none : Option ℝ).getD 0) step maxSteps = 0
theorem precisionDividesOutInMultichannel (cs : List ChannelContribution) (scale : ℝ) (hscale : 0 < scale) (hprecision : 0 < (cs.map (·.precision)).sum) : multichannelDriver (cs.map fun c => { c with precision
theorem precisionScalesSingleChannel (c : ChannelContribution) (scale : ℝ) : singleChannelDriver { c with precision
theorem registryAdditiveFormIsNotGeneral : let prior : Fin 2 → ℝ
theorem machineBeliefUpdateZeroInconsistencyIsNoOp {n : Nat} [NeZero n] (eventWeight entityCount health : ℝ) (kind : BeliefEventType) (likelihood prior : Fin n → ℝ) (hprior : (∑ i, prior i) = 1) : machineBeliefUpdate eventWeight entityCount 0 health kind likelihood prior = prior
```

### MachineBeliefUpdateWitness.lean

Commit `d15325c004314547f2186f998ea33464f198549b`; SHA-256 `2cc10af28c60e77b8ea7914cf6a3574a8f54ea9290737909f2b4238f6bb52f90`.

Declared names: `positiveChannels`, `multichannelReference`, `scaledMultichannelReference`, `singleChannelReference`, `scaledSingleChannelReference`, `eventWeightAtZero`, `eventWeightAtOne`, `eventWeightAtTwo`, `saturatedFifty`, `zeroInconsistencyReference`, `strengthenedLikelihood`, `foreclosedLikelihood`, `uniformSeven`, `rawHealth`, `expectedHealth`, `strengthenedPosterior`, `foreclosedPosterior`, `uniformHealth`, `strengthenedHealth`, `foreclosedHealth`, `updateIsNotSignSymmetric`.

```lean
theorem multichannelReference : multichannelDriver positiveChannels = some (1/4)
theorem scaledMultichannelReference : multichannelDriver (positiveChannels.map fun c => { c with precision
theorem singleChannelReference : singleChannelDriver ⟨1, 2, 1/2⟩ = 1
theorem scaledSingleChannelReference : singleChannelDriver ⟨1, 8, 1/2⟩ = 4
theorem eventWeightAtZero : eventWeight 1 0 3 = 1/10
theorem eventWeightAtOne : eventWeight 1 1 3 = 1/15
theorem eventWeightAtTwo : eventWeight 1 2 3 = 1/30
theorem saturatedFifty : eventWeight 50 0 3 = 1/10
theorem zeroInconsistencyReference : attributedWeight (1/10) 2 0 0 .strengthened = 0
theorem uniformHealth : expectedHealth uniformSeven = 4/7
theorem strengthenedHealth : expectedHealth strengthenedPosterior = 1431362/2193329
theorem foreclosedHealth : expectedHealth foreclosedPosterior = 10772591/21198491
theorem updateIsNotSignSymmetric : expectedHealth strengthenedPosterior - expectedHealth uniformSeven ≠ -(expectedHealth foreclosedPosterior - expectedHealth uniformSeven)
```

### MachineDepth.lean

Commit `9eef38b6a1117924fcf6476671ec21c3d4dd8a6b`; SHA-256 `ba3b7ca7fb5eb8ba405db3c78d41a91a3f916b5c1e391b49387700a2e262871d`.

Declared names: `trajectory`, `finalState`, `predictMultiHorizon`, `predictMultiHorizon_length`, `predictMultiHorizon_final`, `forwardModelDefaultHorizon`, `rolloutDefaultHorizon`, `liveTickHorizon`, `cascadeLaneHorizon`, `declaredDepthsDisagree`, `forwardDefaultAgreesWithLiveTick`, `effectiveDepth`, `EfeDepths`, `efeDepths`, `machineDepth`, `efeDepthsIsMachineDepth`, `gTermsDisagreeOnDepth`, `someOneEqualsNone`, `obsVariance`, `obsVarianceIgnoresState`, `trajectoryVarianceIsConstant`, `epistemicTermsAreHorizonBlind`, `GaussianAtDepth`, `mixedKlDensity`, `klRiskIsNotAnySingleDepthDensity`, `mixedDensityValuesCollapse`, `actionSensitiveStep`, `varyingPolicyNotConstantAction`, `liveDepth`, `noLoadedEventsMeansDepthOne`, `loadedEventsMeansLiveTickHorizon`, `machineHasNoSingleDepth`.

```lean
theorem predictMultiHorizon_length {State Action : Type} (step : State → Action → State) (state : State) (action : Action) (k : Nat) : (predictMultiHorizon step state action k).1.length = k
theorem predictMultiHorizon_final {State Action : Type} (step : State → Action → State) (state : State) (action : Action) (k : Nat) : (predictMultiHorizon step state action k).2 = finalState step state action k
theorem declaredDepthsDisagree : rolloutDefaultHorizon ≠ liveTickHorizon ∧ cascadeLaneHorizon ≠ liveTickHorizon ∧ cascadeLaneHorizon ≠ rolloutDefaultHorizon
theorem forwardDefaultAgreesWithLiveTick : forwardModelDefaultHorizon = liveTickHorizon
theorem efeDepthsIsMachineDepth : (efeDepths : machineDepth) = efeDepths
theorem gTermsDisagreeOnDepth (k : Nat) (hk : 2 ≤ k) : efeDepths (some k) = ⟨k, k, 1, 1⟩ ∧ k ≠ 1
theorem someOneEqualsNone : effectiveDepth (some 1) = effectiveDepth none
theorem obsVarianceIgnoresState {State Action Var : Type} (effects : Option State → Action → Var) (s s' : State) (action : Action) : obsVariance effects s action = obsVariance effects s' action
theorem trajectoryVarianceIsConstant {State Action Var : Type} (effects : Option State → Action → Var) (step : State → Action → State) (state : State) (action : Action) (k : Nat) : ∀ s ∈ trajectory step state action k, obsVariance effects s action = obsVariance effects state action
theorem epistemicTermsAreHorizonBlind {State Action Var Score : Type} (effects : Option State → Action → Var) (epistemic : Var → Score) (step : State → Action → State) (state : State) (action : Action) (j k : Nat) : epistemic (obsVariance effects (finalState step state action j) action) = epistemic (obsVariance effects (finalState step state action k) action)
theorem klRiskIsNotAnySingleDepthDensity (k : Nat) (hk : 2 ≤ k) : ∀ depth : Nat, mixedKlDensity k ≠ ⟨depth, depth⟩
theorem mixedDensityValuesCollapse {State Action Var M : Type} (effects : Option State → Action → Var) (step : State → Action → State) (meanAt : Nat → M) (state : State) (action : Action) (k : Nat) : (meanAt k, obsVariance effects (finalState step state action 1) action) = (meanAt k, obsVariance effects (finalState step state action k) action)
theorem varyingPolicyNotConstantAction : ∀ action : Bool, finalState actionSensitiveStep 0 action 2 ≠ actionSensitiveStep (actionSensitiveStep 0 false) true
theorem noLoadedEventsMeansDepthOne : liveDepth false true = 1
theorem loadedEventsMeansLiveTickHorizon : liveDepth true true = liveTickHorizon
theorem machineHasNoSingleDepth : (efeDepths (some liveTickHorizon)).risk = liveTickHorizon ∧ (efeDepths (some liveTickHorizon)).ambiguity = 1 ∧ (∀ depth : Nat, mixedKlDensity liveTickHorizon ≠ ⟨depth, depth⟩) ∧ effectiveDepth (some 1) = effectiveDepth none ∧ rolloutDefaultHorizon ≠ liveTickHorizon ∧ cascadeLaneHorizon ≠ liveTickHorizon ∧ liveDepth false true = 1
```

### MachineDepthWitness.lean

Commit `a4f5f77e9603c49e2af0bfc9d20e389a5527285a`; SHA-256 `4b6179e1e73702aa86f87cce4914c6ef8c72aa9f8e383037286e2b59c2f1ca73`.

Declared names: `incrementStep`, `depthThreeTrajectory`, `depthThreeDiffersFromFirst`, `depthThreeMinusFirst`, `efeDepthThreeReference`, `nilDepthReference`, `oneDepthReference`, `forwardDefaultReference`, `rolloutDefaultReference`, `cascadeLaneReference`, `addressSorryEffects`, `trajectoryVarianceIsOne`, `epistemicAtThreeEqualsAtOne`, `mixedDensityAtLiveDepth`, `varyingPlanReachesThree`, `liveDepthWitness`.

```lean
theorem depthThreeTrajectory : predictMultiHorizon incrementStep 0 () 3 = ([1, 2, 3], 3)
theorem depthThreeDiffersFromFirst : (predictMultiHorizon incrementStep 0 () 3).2 ≠ (predictMultiHorizon incrementStep 0 () 3).1.head!
theorem depthThreeMinusFirst : (predictMultiHorizon incrementStep 0 () 3).2 - (predictMultiHorizon incrementStep 0 () 3).1.head! = 2
theorem efeDepthThreeReference : efeDepths (some liveTickHorizon) = ⟨3, 3, 1, 1⟩
theorem nilDepthReference : effectiveDepth none = 1
theorem oneDepthReference : effectiveDepth (some 1) = 1
theorem forwardDefaultReference : forwardModelDefaultHorizon = 3
theorem rolloutDefaultReference : rolloutDefaultHorizon = 2
theorem cascadeLaneReference : cascadeLaneHorizon = 5
theorem trajectoryVarianceIsOne : ∀ s ∈ trajectory incrementStep 0 () 3, obsVariance addressSorryEffects s () = 1
theorem epistemicAtThreeEqualsAtOne : (fun v => 2 * v) (obsVariance addressSorryEffects (finalState incrementStep 0 () 3) ()) = (fun v => 2 * v) (obsVariance addressSorryEffects (finalState incrementStep 0 () 1) ())
theorem mixedDensityAtLiveDepth : ∀ depth : Nat, mixedKlDensity liveTickHorizon ≠ ⟨depth, depth⟩
theorem varyingPlanReachesThree : actionSensitiveStep (actionSensitiveStep 0 false) true = 3 ∧ finalState actionSensitiveStep 0 false 2 = 2 ∧ finalState actionSensitiveStep 0 true 2 = 4
theorem liveDepthWitness : liveDepth false true = 1 ∧ liveDepth true true = 3
```

### MachineDirichletAccumulation.lean

Commit `4d89779d08ba625b8a9e1bc0a52cb42f19c567dc`; SHA-256 `ab0aeed8a4b2d27df8694b90333dbab030c180ee083fdf3ddafd3f168880afc1`.

Declared names: `a4aPrior`, `a4aIncrement`, `CorpusRecord`, `matchesCell`, `machineDirichletAccumulation`, `declaredAccumulation`, `machineAppendRaisesItsOwnCell`, `machineAppendMovesNoOtherCell`, `noSingleRecordProducesASoftUpdate`, `oneHotDeclaredUpdateIsTheUnitIncrement`, `softDeclaredUpdateRaisesTwoCells`, `outcomePosition`, `machineCoordinatesAreNotStable`, `declaredCoordinatesAreFixedAndMachineCoordinatesAreNot`, `RealisesDeclaredAccumulation`, `declaredAccumulationRealisesItself`, `recountShaped`, `recountShapedDoesNotRealise`.

```lean
theorem machineAppendRaisesItsOwnCell (corpus : List CorpusRecord) (record : CorpusRecord) : machineDirichletAccumulation (record :: corpus) record.capability record.mission = machineDirichletAccumulation corpus record.capability record.mission + a4aIncrement
theorem machineAppendMovesNoOtherCell (corpus : List CorpusRecord) (record : CorpusRecord) (capability mission : Nat) (h : ¬ (capability = record.capability ∧ mission = record.mission)) : machineDirichletAccumulation (record :: corpus) capability mission = machineDirichletAccumulation corpus capability mission
theorem noSingleRecordProducesASoftUpdate (corpus : List CorpusRecord) (record : CorpusRecord) (c₀ m₀ c₁ m₁ : Nat) (hne : ¬ (c₀ = c₁ ∧ m₀ = m₁)) (h₀ : machineDirichletAccumulation (record :: corpus) c₀ m₀ ≠ machineDirichletAccumulation corpus c₀ m₀) (h₁ : machineDirichletAccumulation (record :: corpus) c₁ m₁ ≠ machineDirichletAccumulation corpus c₁ m₁) : False
theorem oneHotDeclaredUpdateIsTheUnitIncrement (a : Channel → Status → ℝ) (c₀ : Channel) (s₀ : Status) : declaredAccumulation a [(fun c => if c = c₀ then 1 else 0, fun x => if x = s₀ then 1 else 0)] c₀ s₀ = a c₀ s₀ + a4aIncrement ∧ ∀ c x, ¬ (c = c₀ ∧ x = s₀) → declaredAccumulation a [(fun c => if c = c₀ then 1 else 0, fun x => if x = s₀ then 1 else 0)] c x = a c x
theorem softDeclaredUpdateRaisesTwoCells (a : Channel → Status → ℝ) (c₀ c₁ : Channel) (hne : c₀ ≠ c₁) (s₀ : Status) : a c₀ s₀ < declaredAccumulation a [(fun c => if c = c₀ ∨ c = c₁ then 1 / 2 else 0, fun x => if x = s₀ then 1 else 0)] c₀ s₀ ∧ a c₁ s₀ < declaredAccumulation a [(fun c => if c = c₀ ∨ c = c₁ then 1 / 2 else 0, fun x => if x = s₀ then 1 else 0)] c₁ s₀
theorem machineCoordinatesAreNotStable : outcomePosition "m1" ["m1", "m2"] = some 0 ∧ outcomePosition "m1" ["aaa", "m1", "m2"] = some 1
theorem declaredCoordinatesAreFixedAndMachineCoordinatesAreNot : Status.all.length ≠ Channel.all.length ∧ outcomePosition "m1" ["m1", "m2"] ≠ outcomePosition "m1" ["aaa", "m1", "m2"]
theorem declaredAccumulationRealisesItself : RealisesDeclaredAccumulation declaredAccumulation
theorem recountShapedDoesNotRealise : ¬ RealisesDeclaredAccumulation recountShaped
```

### MachineDirichletAccumulationWitness.lean

Commit `4d89779d08ba625b8a9e1bc0a52cb42f19c567dc`; SHA-256 `4bb818548167b6fccbf0c402360501633e8b6e9b070401a44bb03bc3507335f1`.

Declared names: `priorReference`, `oneEdgeReference`, `twoEdgesReference`, `twoMissionRowReference`, `recountReference`, `bmrThresholdReference`.

```lean
theorem priorReference : a4aPrior = 1 / 10
theorem oneEdgeReference : machineDirichletAccumulation [⟨0, 0⟩] 0 0 = 11 / 10
theorem twoEdgesReference : machineDirichletAccumulation [⟨0, 0⟩, ⟨0, 0⟩] 0 0 = 21 / 10
theorem twoMissionRowReference : machineDirichletAccumulation [⟨0, 0⟩] 0 0 = 11 / 10 ∧ machineDirichletAccumulation [⟨0, 0⟩] 0 1 = 1 / 10
theorem recountReference : recountShaped (fun _ _ => 1) [] Channel.loopHealth Status.spawned = 0 ∧ declaredAccumulation (fun _ _ => 1) [] Channel.loopHealth Status.spawned = 1
theorem bmrThresholdReference (change : ModelReductionFreeEnergyChange) : bayesFactorThreshold change ↔ change.value ≤ -3
```

### MachineObservation.lean

Commit `a4c2276d515730a32db27f97f1fd955a9bae1433`; SHA-256 `f864610a2d794b57fa18d8b3b457faf7c426015f4c01ec5c989df292adbb10a0`.

Declared names: `MeasurementVariant`, `ObservationEnvelope`, `envelopeOf`, `machineObservation`, `BoundedObservation`, `upperClamp`, `upperClamp_le_one`, `clampedChannels`, `clampAtClamped`, `clampedChannelsAreBoundedAbove`, `EnvelopePromises`, `PromiseKept`, `activeRepoReading`, `couplingDensityReading`, `incompleteActiveRepoSummaryBreaksTheCoercionPromise`, `EnvelopeMatches`, `senseToVector`.

```lean
theorem upperClamp_le_one (x : ℝ) : upperClamp x ≤ 1
theorem clampedChannelsAreBoundedAbove (raw : Channel → ℝ) (c : Channel) (hc : c ∈ clampedChannels) : (clampAtClamped raw).value c ≤ 1
theorem incompleteActiveRepoSummaryBreaksTheCoercionPromise (envelope : ObservationEnvelope) (reading : Channel → Option ℝ) (hAbsent : envelope.variant .activeRepoRatio = .absent) (hCoercedTo : envelope.value .activeRepoRatio = 0) (hNoReading : reading .activeRepoRatio = none) : EnvelopePromises envelope .activeRepoRatio ∧ ¬ PromiseKept reading envelope .activeRepoRatio
```

### MachineObservationWitness.lean

Commit `a4c2276d515730a32db27f97f1fd955a9bae1433`; SHA-256 `2498ed198408a6ef991de1da6d2cd43dfccac612629bcacaabc8d90b9ef5a86e`.

Declared names: `emptyValues`, `absentVariants`, `emptyObservationHasFourteenZeros`, `clampAtCap`, `clampAboveCapTwo`, `clampAboveCapFive`, `sorryCountNormIsBoundedAbove`, `couplingDensityIsBoundedAbove`, `stackPctIsNotClamped`, `stackSeventy`, `loopHealthNegativeThree`, `activeRepoFive`, `stackSeventyIsUnbounded`, `loopHealthNegativeThreeIsUnbounded`, `activeRepoFiveIsUnbounded`, `incompleteActiveRepoSummaryHasNoReading`, `incompleteCouplingSummaryReadsZero`, `readbackCoercionPromiseIsBroken`, `observedVariantReference`, `absentVariantReference`, `emptyEnvelope`, `loopHealthObservedEnvelope`, `readbackRefusalPairAgreesNumerically`, `readbackRefusalPairIsRefused`, `matchingEnvelopeVectorLength`.

```lean
theorem emptyObservationHasFourteenZeros : (Channel.all.map (machineObservation emptyValues absentVariants).1.value) = List.replicate 14 (0 : ℝ)
theorem clampAtCap : upperClamp 1 = 1
theorem clampAboveCapTwo : upperClamp 2 = 1
theorem clampAboveCapFive : upperClamp 5 = 1
theorem sorryCountNormIsBoundedAbove (raw : Channel → ℝ) : (clampAtClamped raw).value .sorryCountNorm ≤ 1
theorem couplingDensityIsBoundedAbove (raw : Channel → ℝ) : (clampAtClamped raw).value .couplingDensity ≤ 1
theorem stackPctIsNotClamped (raw : Channel → ℝ) : (clampAtClamped raw).value .stackPct = raw .stackPct
theorem stackSeventyIsUnbounded : ¬ BoundedObservation stackSeventy
theorem loopHealthNegativeThreeIsUnbounded : ¬ BoundedObservation loopHealthNegativeThree
theorem activeRepoFiveIsUnbounded : ¬ BoundedObservation activeRepoFive
theorem incompleteActiveRepoSummaryHasNoReading (active : ℝ) : activeRepoReading (some active) none = none
theorem incompleteCouplingSummaryReadsZero (edges : ℝ) : couplingDensityReading (some edges) none = some 0
theorem readbackCoercionPromiseIsBroken (envelope : ObservationEnvelope) (reading : Channel → Option ℝ) (hAbsent : envelope.variant .activeRepoRatio = .absent) (hCoercedTo : envelope.value .activeRepoRatio = 0) (hNoReading : reading .activeRepoRatio = none) : EnvelopePromises envelope .activeRepoRatio ∧ ¬ PromiseKept reading envelope .activeRepoRatio
theorem observedVariantReference : (machineObservation emptyValues (fun _ => .observed)).2.variant .loopHealth = .observed
theorem absentVariantReference : (machineObservation emptyValues absentVariants).2.variant .loopHealth = .absent
theorem readbackRefusalPairAgreesNumerically : emptyEnvelope.value = loopHealthObservedEnvelope.value
theorem readbackRefusalPairIsRefused : ¬ EnvelopeMatches emptyEnvelope loopHealthObservedEnvelope
theorem matchingEnvelopeVectorLength : (senseToVector ⟨emptyValues⟩ emptyEnvelope emptyEnvelope ⟨rfl, rfl⟩).length = 14
```

### MachinePolicyFreeEnergy.lean

Commit `d7a45a358acbc7680b44267032607216bf2a4b12`; SHA-256 `be7de30b672bb937889f84d94b7e98d205066e788c4e922e32b019965a98eb57`.

Declared names: `VarianceStatus`, `AbsentVarianceMode`, `PolicyFreeEnergyError`, `ChannelDatum`, `channelPolicyFreeEnergy`, `machinePolicyFreeEnergy`, `varianceTrichotomy`, `FPiScaling`, `selectionScore`, `policyFreeEnergyEntersUnscaled`.

```lean
theorem varianceTrichotomy (r tolerance floor : ℝ) (hfloor : 0 < floor) : channelPolicyFreeEnergy ⟨r, -1, .present⟩ tolerance floor .reject = .error .invalidVariance ∧ channelPolicyFreeEnergy ⟨r, 1, .present⟩ tolerance floor .reject = .ok ((Real.log (2 * Real.pi) + r ^ 2) / 2) ∧ (channelPolicyFreeEnergy ⟨r, 0, .present⟩ tolerance floor .floor = if |r| ≤ tolerance then .ok 0 else .error .deterministicMismatch) ∧ channelPolicyFreeEnergy ⟨r, 0, .absent⟩ tolerance floor .floor = .ok ((Real.log (2 * Real.pi * floor) + r ^ 2 / floor) / 2)
theorem policyFreeEnergyEntersUnscaled (lp g f t₁ t₂ : ℝ) : selectionScore lp g f t₁ .unscaled - selectionScore lp g 0 t₁ .unscaled = -f ∧ selectionScore lp g f t₂ .unscaled - selectionScore lp g 0 t₂ .unscaled = -f ∧ selectionScore lp g f t₁ .byTau - selectionScore lp g 0 t₁ .byTau = -f / t₁
```

### MachinePolicyFreeEnergyWitness.lean

Commit `d7a45a358acbc7680b44267032607216bf2a4b12`; SHA-256 `78b7071f151d70bde35b6fa880a97066ec4614026485b80cb94d72b80359d3b4`.

Declared names: `present_ne_absent`, `reject_ne_floor`, `positiveVarianceContribution`, `toleratedDeterministicZero`, `rejectedDeterministicZero`, `absentZeroFloored`, `bareZeroStillRejectsUnderFloor`, `negativeVarianceRejects`, `machineTwoChannelTotal`, `unscaledTauPair`, `scaledTauPair`.

```lean
theorem present_ne_absent : VarianceStatus.present ≠ .absent
theorem reject_ne_floor : AbsentVarianceMode.reject ≠ .floor
theorem positiveVarianceContribution : channelPolicyFreeEnergy ⟨1/2, 1/4, .present⟩ 0 (1/100) .reject = .ok ((Real.log (2 * Real.pi * (1/4)) + 1) / 2)
theorem toleratedDeterministicZero : channelPolicyFreeEnergy ⟨1/1000, 0, .present⟩ (1/1000) (1/100) .reject = .ok 0
theorem rejectedDeterministicZero : channelPolicyFreeEnergy ⟨1/100, 0, .present⟩ (1/1000) (1/100) .reject = .error .deterministicMismatch
theorem absentZeroFloored : channelPolicyFreeEnergy ⟨1/10, 0, .absent⟩ 0 (1/100) .floor = .ok ((Real.log (2 * Real.pi * (1/100)) + 1) / 2)
theorem bareZeroStillRejectsUnderFloor : channelPolicyFreeEnergy ⟨1/10, 0, .present⟩ 0 (1/100) .floor = .error .deterministicMismatch
theorem negativeVarianceRejects : channelPolicyFreeEnergy ⟨0, -1, .present⟩ 0 (1/100) .reject = .error .invalidVariance
theorem machineTwoChannelTotal : machinePolicyFreeEnergy [⟨1/2, 1/4, .present⟩, ⟨1/2, 1, .present⟩] 0 (1/100) .reject = .ok ((Real.log (2 * Real.pi * (1/4)) + 1) / 2 + (Real.log (2 * Real.pi * 1) + 1/4) / 2)
theorem unscaledTauPair : selectionScore 0 4 3 2 .unscaled = -5 ∧ selectionScore 0 4 3 4 .unscaled = -4
theorem scaledTauPair : selectionScore 0 4 3 2 .byTau = -7/2 ∧ selectionScore 0 4 3 4 .byTau = -7/4
```

### MachinePolicySet.lean

Commit `738cae3a540ac03a4d77e1ebe399c7c6dccd8cb6`; SHA-256 `104fab58f500a7bad3b11de3cd482cd1f790d70d7cb976905561a5ca773b3d05`.

Declared names: `machinePolicySet`, `selectionPolicySet`, `productionFixtureReadback`, `productionRulesReadback`.

```lean
theorem productionFixtureReadback : machinePolicySet [⟨0, 0, false⟩, ⟨1, 1, false⟩] = ({⟨0, 0, false⟩, ⟨1, 1, false⟩} : CandidateActionSpace Candidate)
theorem productionRulesReadback : let ranked
```

### MachinePrecision.lean

Commit `e2e8ee9649908a5564da6237b7d8def80a4bbaf5`; SHA-256 `d8eb677df9e402b93ee7e7c67eef19840dcb20f6000527a2a3defa6c7b21d1de`.

Declared names: `PrecisionParameters`, `windowOf`, `regularizedErrorVariance`, `varianceComponent`, `machinePrecision`, `windowOf_length_le`, `windowOf_replicate`, `regularizedErrorVariance_replicate_zero`, `regularizedErrorVariance_pos`, `regularizedErrorVariance_ge`, `floor_le_machinePrecision`, `registryFormOfUnclamped`, `machinePrecisionMap`, `machinePrecisionMap_value`, `defaults`, `defaultsVarianceGe`, `defaultsMinVarianceInert`, `defaultsVarianceComponentLe`, `defaultsCapInert`, `defaultsLeTwentyOne`, `defaultsRange`.

```lean
theorem windowOf_length_le (n : Nat) (history : List ℝ) : (windowOf n history).length ≤ n
theorem windowOf_replicate (n m : Nat) (x : ℝ) : windowOf n (List.replicate m x) = List.replicate (min m n) x
theorem regularizedErrorVariance_replicate_zero (p : PrecisionParameters) (n : Nat) : regularizedErrorVariance p (List.replicate n (0:ℝ)) = p.priorStrength * p.priorVariance / (p.priorStrength + n)
theorem regularizedErrorVariance_pos (p : PrecisionParameters) (w : List ℝ) : 0 < regularizedErrorVariance p w
theorem regularizedErrorVariance_ge (p : PrecisionParameters) (w : List ℝ) (hw : w.length ≤ p.windowSize) : p.priorStrength * p.priorVariance / (p.priorStrength + p.windowSize) ≤ regularizedErrorVariance p w
theorem floor_le_machinePrecision (p : PrecisionParameters) (history : List ℝ) : p.floor ≤ machinePrecision p history
theorem registryFormOfUnclamped (p : PrecisionParameters) (history : List ℝ) (hlo : p.floor ≤ varianceComponent p history) (hhi : varianceComponent p history ≤ p.cap) : machinePrecision p history = 1 / max (regularizedErrorVariance p (windowOf p.windowSize history)) p.minVariance
theorem machinePrecisionMap_value (p : PrecisionParameters) (history : Channel → List ℝ) (k : Channel) : (machinePrecisionMap p history k).value = machinePrecision p (history k)
theorem defaultsVarianceGe (history : List ℝ) : (1:ℝ) / 21 ≤ regularizedErrorVariance defaults (windowOf defaults.windowSize history)
theorem defaultsMinVarianceInert (history : List ℝ) : max (regularizedErrorVariance defaults (windowOf defaults.windowSize history)) defaults.minVariance = regularizedErrorVariance defaults (windowOf defaults.windowSize history)
theorem defaultsVarianceComponentLe (history : List ℝ) : varianceComponent defaults history ≤ 21
theorem defaultsCapInert (history : List ℝ) : machinePrecision defaults history = max (varianceComponent defaults history) defaults.floor
theorem defaultsLeTwentyOne (history : List ℝ) : machinePrecision defaults history ≤ 21
theorem defaultsRange (history : List ℝ) : (1:ℝ) / 10 ≤ machinePrecision defaults history ∧ machinePrecision defaults history ≤ 21
```

### MachinePrecisionWitness.lean

Commit `e2e8ee9649908a5564da6237b7d8def80a4bbaf5`; SHA-256 `4bbde5dfa69660f1b478fe445b834fdf89f150fabebd7665868f76fda07cbb5f`.

Declared names: `oneStillError`, `threeStillErrors`, `sevenStillErrors`, `fifteenStillErrors`, `fullWindowOfStillErrors`, `windowBinds`, `unitError`, `halfError`, `tripleError`, `floorIsReached`, `registryFormFailsAtTheFloor`, `wideWindow`, `wideWindowReachesMinVariance`.

```lean
theorem oneStillError : machinePrecision defaults [0] = 2
theorem threeStillErrors : machinePrecision defaults (List.replicate 3 0) = 4
theorem sevenStillErrors : machinePrecision defaults (List.replicate 7 0) = 8
theorem fifteenStillErrors : machinePrecision defaults (List.replicate 15 0) = 16
theorem fullWindowOfStillErrors : machinePrecision defaults (List.replicate 20 0) = 21
theorem windowBinds : machinePrecision defaults (List.replicate 25 0) = machinePrecision defaults (List.replicate 20 0)
theorem unitError : machinePrecision defaults [1] = 1
theorem halfError : machinePrecision defaults [1/2] = 8/5
theorem tripleError : machinePrecision defaults [3] = 1/5
theorem floorIsReached : varianceComponent defaults (List.replicate 20 10) = 7/667 ∧ machinePrecision defaults (List.replicate 20 10) = 1/10
theorem registryFormFailsAtTheFloor : ¬ (defaults.floor ≤ varianceComponent defaults (List.replicate 20 10)) ∧ machinePrecision defaults (List.replicate 20 10) ≠ 1 / max (regularizedErrorVariance defaults (windowOf defaults.windowSize (List.replicate 20 10))) defaults.minVariance
theorem wideWindowReachesMinVariance : regularizedErrorVariance wideWindow (windowOf wideWindow.windowSize (List.replicate 200 0)) = 1/201 ∧ max (regularizedErrorVariance wideWindow (windowOf wideWindow.windowSize (List.replicate 200 0))) wideWindow.minVariance = 1/100 ∧ machinePrecision wideWindow (List.replicate 200 0) = 100
```

### MachinePredictionError.lean

Commit `1282b75e3223d3f94d536ebabbb5b4125989722d`; SHA-256 `86c68198ec18fb072c5035c989fdf8e3ef61965b6f12f8ef7994be59bd19014f`.

Declared names: `Field`, `Prediction`, `Member`, `Offence`, `memberOffence`, `observedOffence`, `offences`, `PresentRecord`, `Outcome`, `presentRecord`, `machineChannelPredictionError`, `refused_iff`, `refusalDominatesAbsence`, `absence_requires_wellFormedModel`, `missingObservationIsAbsent`, `malformedObservationIsRefused`, `present_eq`, `registryFormOfPresent`, `perCallPrecision_pos`, `weightedError_eq`, `negativeVarianceIsNotRefused`, `subMinVarianceIsFloored`, `defaultMinVariance`, `channelsWithLikelihood`, `channelsWithLikelihood_length`, `channelsWithoutLikelihood`, `channelsWithoutLikelihood_eq`, `channelsWithoutLikelihood_length`, `isPresent`, `isRefused`, `channelOutcomes`, `scoredErrors`, `oneRefusalEmptiesTheUpdate`, `scoredErrors_present`.

```lean
theorem refused_iff (minVariance : ℝ) (o : Field) (p : Prediction) : (∃ l, machineChannelPredictionError minVariance o p = Outcome.refused l) ↔ offences o p ≠ []
theorem refusalDominatesAbsence (minVariance : ℝ) (v : Field) : machineChannelPredictionError minVariance Field.missing ⟨Field.missing, v⟩ = Outcome.refused (offences Field.missing ⟨Field.missing, v⟩)
theorem absence_requires_wellFormedModel (minVariance : ℝ) (o : Field) (p : Prediction) (h : machineChannelPredictionError minVariance o p = Outcome.absent Member.observed) : o = Field.missing ∧ (∃ m, p.mean = Field.value m) ∧ (∃ v, p.variance = Field.value v)
theorem missingObservationIsAbsent (minVariance m v : ℝ) : machineChannelPredictionError minVariance Field.missing ⟨Field.value m, Field.value v⟩ = Outcome.absent Member.observed
theorem malformedObservationIsRefused (minVariance m v : ℝ) : machineChannelPredictionError minVariance Field.notFinite ⟨Field.value m, Field.value v⟩ = Outcome.refused [Offence.notFinite Member.observed]
theorem present_eq (minVariance ob m v : ℝ) : machineChannelPredictionError minVariance (Field.value ob) ⟨Field.value m, Field.value v⟩ = Outcome.present (presentRecord minVariance ob m v)
theorem registryFormOfPresent (minVariance : ℝ) (obs : ObservationVector) (beliefMean : Channel → ℝ) (variance : Channel → ℝ) (k : Channel) : machineChannelPredictionError minVariance (Field.value (obs.value k)) ⟨Field.value (beliefMean k), Field.value (variance k)⟩ = Outcome.present { observed
theorem perCallPrecision_pos (minVariance : ℝ) (hmv : 0 < minVariance) (ob m v : ℝ) : 0 < (presentRecord minVariance ob m v).perCallPrecision
theorem weightedError_eq (minVariance ob m v : ℝ) : (presentRecord minVariance ob m v).weightedError = (presentRecord minVariance ob m v).error * (presentRecord minVariance ob m v).perCallPrecision
theorem negativeVarianceIsNotRefused (minVariance ob m v : ℝ) : machineChannelPredictionError minVariance (Field.value ob) ⟨Field.value m, Field.value v⟩ = Outcome.present (presentRecord minVariance ob m v)
theorem subMinVarianceIsFloored (minVariance ob m v : ℝ) (hv : v ≤ minVariance) : (presentRecord minVariance ob m v).perCallPrecision = 1 / minVariance
theorem channelsWithLikelihood_length : channelsWithLikelihood.length = 8
theorem channelsWithoutLikelihood_eq : channelsWithoutLikelihood = [.loopHealth, .stackPct, .consultingPct, .portfolioPct, .mathematicsPct, .depositingSignal]
theorem channelsWithoutLikelihood_length : channelsWithoutLikelihood.length = 6
theorem oneRefusalEmptiesTheUpdate (minVariance : ℝ) (o : Channel → Field) (p : Channel → Prediction) (k : Channel) (hk : k ∈ channelsWithLikelihood) (h : isRefused (machineChannelPredictionError minVariance (o k) (p k)) = true) : scoredErrors minVariance o p = []
theorem scoredErrors_present (minVariance : ℝ) (o : Channel → Field) (p : Channel → Prediction) (x : Channel × Outcome) (hx : x ∈ scoredErrors minVariance o p) : isPresent x.2 = true
```

### MachinePredictionErrorWitness.lean

Commit `1282b75e3223d3f94d536ebabbb5b4125989722d`; SHA-256 `96783ef9bde9e12c8d302f6b7244de1c4c43f156814abfbaa43d6dc05dd81494`.

Declared names: `mv`, `basicTriple`, `unitVariance`, `emptyBeliefReadsTheObservation`, `negativeError`, `exactZeroError`, `eighthVariance`, `zeroVarianceFloored`, `negativeVarianceFloored`, `zeroAndNegativeVarianceAgree`, `unobservedIsOmitted`, `brokenModelRefuses`, `unobservedWithBrokenModelRefuses`, `malformedObservationRefuses`, `bothModelMembersNamed`, `perCallPrecisionIsNotMachinePrecision`.

```lean
theorem basicTriple : (presentRecord mv (3/4) (1/4) (1/4)).error = 1/2 ∧ (presentRecord mv (3/4) (1/4) (1/4)).perCallPrecision = 4 ∧ (presentRecord mv (3/4) (1/4) (1/4)).weightedError = 2
theorem unitVariance : (presentRecord mv (3/4) (1/4) 1).perCallPrecision = 1 ∧ (presentRecord mv (3/4) (1/4) 1).weightedError = 1/2
theorem emptyBeliefReadsTheObservation : (presentRecord mv (3/4) 0 1).error = 3/4 ∧ (presentRecord mv (3/4) 0 1).perCallPrecision = 1 ∧ (presentRecord mv (3/4) 0 1).weightedError = 3/4
theorem negativeError : (presentRecord mv (1/4) (3/4) (1/4)).error = -(1/2) ∧ (presentRecord mv (1/4) (3/4) (1/4)).weightedError = -2
theorem exactZeroError : (presentRecord mv (1/2) (1/2) (1/4)).error = 0 ∧ (presentRecord mv (1/2) (1/2) (1/4)).weightedError = 0
theorem eighthVariance : (presentRecord mv (3/4) (1/4) (1/8)).perCallPrecision = 8 ∧ (presentRecord mv (3/4) (1/4) (1/8)).weightedError = 4
theorem zeroVarianceFloored : (presentRecord mv (3/4) (1/4) 0).perCallPrecision = 100 ∧ (presentRecord mv (3/4) (1/4) 0).weightedError = 50
theorem negativeVarianceFloored : (presentRecord mv (3/4) (1/4) (-4)).perCallPrecision = 100 ∧ (presentRecord mv (3/4) (1/4) (-4)).weightedError = 50
theorem zeroAndNegativeVarianceAgree : (presentRecord mv (3/4) (1/4) 0).perCallPrecision = (presentRecord mv (3/4) (1/4) (-4)).perCallPrecision ∧ (presentRecord mv (3/4) (1/4) 0).weightedError = (presentRecord mv (3/4) (1/4) (-4)).weightedError ∧ (presentRecord mv (3/4) (1/4) 0).predictedVariance ≠ (presentRecord mv (3/4) (1/4) (-4)).predictedVariance
theorem unobservedIsOmitted : machineChannelPredictionError mv Field.missing ⟨Field.value (1/4), Field.value (1/4)⟩ = Outcome.absent Member.observed
theorem brokenModelRefuses : machineChannelPredictionError mv (Field.value (3/4)) ⟨Field.missing, Field.value (1/4)⟩ = Outcome.refused [Offence.missing Member.mean]
theorem unobservedWithBrokenModelRefuses : machineChannelPredictionError mv Field.missing ⟨Field.missing, Field.value (1/4)⟩ = Outcome.refused [Offence.missing Member.mean]
theorem malformedObservationRefuses : machineChannelPredictionError mv Field.notFinite ⟨Field.value (1/4), Field.value (1/4)⟩ = Outcome.refused [Offence.notFinite Member.observed]
theorem bothModelMembersNamed : machineChannelPredictionError mv (Field.value (3/4)) ⟨Field.missing, Field.notFinite⟩ = Outcome.refused [Offence.missing Member.mean, Offence.notFinite Member.variance]
theorem perCallPrecisionIsNotMachinePrecision : (presentRecord mv (3/4) (1/4) (1/4)).perCallPrecision = 4 ∧ MachinePrecision.machinePrecision MachinePrecision.defaults [1/2] = 8/5 ∧ (presentRecord mv (3/4) (1/4) (1/4)).perCallPrecision ≠ MachinePrecision.machinePrecision MachinePrecision.defaults [1/2]
```

### MachineQ.lean

Commit `824f67b5e2c9fd7b0ef96c3e1a2eea96841528d5`; SHA-256 `d30b1bfceab5cccc214f95e6593318b0bd53f3a838482ad3f6b2af4fb41a91ed`.

Declared names: `sum_swap`, `QReading`, `predictedStateMass`, `predictedStateMass_nonneg`, `predictedStateMass_sum`, `machinePredictedStateKernel`, `predictiveOutcomeMass`, `predictiveOutcomeMass_nonneg`, `predictiveOutcomeMass_sum`, `machinePredictiveOutcomeKernel`, `rowsEqualOfEqualPlans`, `plansDifferOfRowsDiffer`, `machineAmbiguity`, `machineExpectedFreeEnergy`.

```lean
private theorem sum_swap {α β : Type*} (xs : List α) (ys : List β) (f : α → β → ℝ) : (xs.map (fun x => (ys.map (f x)).sum)).sum = (ys.map (fun y => (xs.map (fun x => f x y)).sum)).sum
theorem predictedStateMass_nonneg (model : GenerativeModel Obs State Action PolicyIndex) (reading : QReading model) (belief : BeliefState) (π : PolicyIndex) (s' : State) : 0 ≤ predictedStateMass model reading belief π s'
theorem predictedStateMass_sum (model : GenerativeModel Obs State Action PolicyIndex) (reading : QReading model) (belief : BeliefState) (π : PolicyIndex) : (reading.states.map (predictedStateMass model reading belief π)).sum = 1
theorem predictiveOutcomeMass_nonneg (model : GenerativeModel Obs State Action PolicyIndex) (reading : QReading model) (belief : BeliefState) (π : PolicyIndex) (o : Outcome Obs) : 0 ≤ predictiveOutcomeMass model reading belief π o
theorem predictiveOutcomeMass_sum (model : GenerativeModel Obs State Action PolicyIndex) (reading : QReading model) (belief : BeliefState) (π : PolicyIndex) : (reading.outcomes.map (predictiveOutcomeMass model reading belief π)).sum = 1
theorem rowsEqualOfEqualPlans (model : GenerativeModel Obs State Action PolicyIndex) (reading : QReading model) (belief : BeliefState) (π ρ : PolicyIndex) (h : reading.plan π = reading.plan ρ) : (machinePredictiveOutcomeKernel model reading belief).mass π = (machinePredictiveOutcomeKernel model reading belief).mass ρ
theorem plansDifferOfRowsDiffer (model : GenerativeModel Obs State Action PolicyIndex) (reading : QReading model) (belief : BeliefState) (π ρ : PolicyIndex) (h : (machinePredictiveOutcomeKernel model reading belief).mass π ≠ (machinePredictiveOutcomeKernel model reading belief).mass ρ) : reading.plan π ≠ reading.plan ρ
```

### MachineQWitness.lean

Commit `824f67b5e2c9fd7b0ef96c3e1a2eea96841528d5`; SHA-256 `601445753f31a930acc5bc0f8b23d080a8c313e5f2c1b911d0578f97aeb587f3`.

Declared names: `EvidenceOutcome`, `EvidenceOutcome.all`, `alphabetIsClosed`, `DemoObs`, `out`, `demoAlphabet`, `DemoState`, `demoStates`, `DemoAction`, `DemoPolicy`, `aRow`, `demoObservation`, `bRow`, `demoTransition`, `demoPolicyPrior`, `demoModel`, `informativeWeight`, `informativeWeight_nonneg`, `informativeWeight_le_one`, `demoBeliefMass`, `demoReading`, `demoQ`, `demoRowsNormalised`, `demoOrdinaryRowMasses`, `demoPolicyConditionedDifference`, `demoPlansDiffer`, `flatReading`, `flatReadingRowsCoincide`.

```lean
theorem alphabetIsClosed : EvidenceOutcome.all.length = 6 ∧ ∀ e : EvidenceOutcome, e ∈ EvidenceOutcome.all
theorem informativeWeight_nonneg (b : BeliefState) : 0 ≤ informativeWeight b
theorem informativeWeight_le_one (b : BeliefState) : informativeWeight b ≤ 1
theorem demoRowsNormalised (b : BeliefState) (π : DemoPolicy) : (((demoQ b).support π).map ((demoQ b).mass π)).sum = 1
theorem demoOrdinaryRowMasses (b : BeliefState) : (demoQ b).mass .acquisition (out .ordinary) = 25/64 ∧ (demoQ b).mass .review (out .ordinary) = 11/64
theorem demoPolicyConditionedDifference (b : BeliefState) : (demoQ b).mass .acquisition ≠ (demoQ b).mass .review
theorem demoPlansDiffer (b : BeliefState) : demoReading.plan .acquisition ≠ demoReading.plan .review
theorem flatReadingRowsCoincide (b : BeliefState) : (machinePredictiveOutcomeKernel demoModel flatReading b).mass .acquisition = (machinePredictiveOutcomeKernel demoModel flatReading b).mass .review
```

### MachineTemperature.lean

Commit `b31e5db2e494a0f11c4c35077c25d7399b1d19c9`; SHA-256 `6809fb9cac3d43092b0a8e9eefd8cde5a823a7ff072d56d878de71291453b6ba`.

Declared names: `TauMode`, `MachineNumber`, `TemperatureOpts`, `TemperatureError`, `machineTemperature`, `gainOneReductions`, `betaIsNotFloored`, `missingBetaNeverFallsBack`, `policyFunctionDefault`, `liveArenaDefault`, `temperatureDefaultsDisagree`, `BetaSource`, `solvedThisTick`, `variationalModeCanCarryHeldBeta`, `temperatureScore`, `SelectionLaw`, `temperatureReachesChoice`, `selectionLawControlsTemperatureChannel`, `gRange`, `adaptiveTemperature`, `adaptiveTemperatureReference`, `emptySpreadIsTheFloor`, `degenerateSpreadIsTheFloor`, `probeOpts`, `threeLawsDisagree`, `machineGamma`, `variationalGammaIsInverseBeta`, `engineeringGammaIsNotInverseBeta`, `gainFloorPreventsDivisionByZero`, `selectionScore`, `largerTemperatureFlattensScores`, `zeroPriorOrderIsTemperatureInvariant`, `habitPriorMakesOrderTemperatureDependent`.

```lean
theorem gainOneReductions (spread tauMin : ℝ) (hmin : tauMin ≤ 1) : machineTemperature ⟨.spread, tauMin, spread, 1, none⟩ = .ok spread ∧ machineTemperature ⟨.selectionGainOnly, tauMin, spread, 1, none⟩ = .ok 1
theorem betaIsNotFloored (beta tauMin spread gain : ℝ) (hbeta : 0 < beta) : machineTemperature ⟨.variationalBetaGamma, tauMin, spread, gain, some (.finite beta)⟩ = .ok beta
theorem missingBetaNeverFallsBack (tauMin spread gain : ℝ) : machineTemperature ⟨.variationalBetaGamma, tauMin, spread, gain, none⟩ = .error .invalidVariationalBeta
theorem temperatureDefaultsDisagree : policyFunctionDefault ≠ liveArenaDefault
theorem variationalModeCanCarryHeldBeta : solvedThisTick .heldUnsolved = false ∧ solvedThisTick .heldAbsent = false ∧ solvedThisTick .initial = false
theorem selectionLawControlsTemperatureChannel : temperatureReachesChoice .controllerHead = false ∧ temperatureReachesChoice .fullScorePosterior = true
theorem adaptiveTemperatureReference : adaptiveTemperature (1/100) 5 [1, 2, 7/2, 1/2] = 3/5
theorem emptySpreadIsTheFloor (tauMin k : ℝ) : adaptiveTemperature tauMin k [] = tauMin
theorem degenerateSpreadIsTheFloor : adaptiveTemperature (1/100) 5 [2, 2, 2] = 1/100
theorem threeLawsDisagree : machineTemperature (probeOpts .spread) = .ok (3/10) ∧ machineTemperature (probeOpts .selectionGainOnly) = .ok (1/2) ∧ machineTemperature (probeOpts .variationalBetaGamma) = .ok (1/4) ∧ (3/10 : ℝ) ≠ 1/2 ∧ (3/10 : ℝ) ≠ 1/4 ∧ (1/2 : ℝ) ≠ 1/4
theorem variationalGammaIsInverseBeta (beta tauMin spread gain : ℝ) (hbeta : 0 < beta) : (machineTemperature ⟨.variationalBetaGamma, tauMin, spread, gain, some (.finite beta)⟩).map machineGamma = .ok (1 / beta)
theorem engineeringGammaIsNotInverseBeta : (machineTemperature (probeOpts .spread)).map machineGamma = .ok (10/3) ∧ (machineTemperature (probeOpts .selectionGainOnly)).map machineGamma = .ok 2 ∧ (10/3 : ℝ) ≠ 4 ∧ (2 : ℝ) ≠ 4
theorem gainFloorPreventsDivisionByZero (tauMin spread : ℝ) (h : 0 ≤ tauMin) : machineTemperature ⟨.selectionGainOnly, tauMin, spread, 0, none⟩ = .ok (1 / tauMin)
theorem largerTemperatureFlattensScores (g₁ g₂ τ₁ τ₂ : ℝ) (hg : g₁ < g₂) (hτ₁ : 0 < τ₁) (hτ : τ₁ < τ₂) : temperatureScore g₁ τ₂ - temperatureScore g₂ τ₂ < temperatureScore g₁ τ₁ - temperatureScore g₂ τ₁
theorem zeroPriorOrderIsTemperatureInvariant (g₁ g₂ τ₁ τ₂ : ℝ) (hτ₁ : 0 < τ₁) (hτ₂ : 0 < τ₂) : (selectionScore 0 g₁ τ₁ < selectionScore 0 g₂ τ₁) ↔ (selectionScore 0 g₁ τ₂ < selectionScore 0 g₂ τ₂)
theorem habitPriorMakesOrderTemperatureDependent : selectionScore 1 2 (1/2) < selectionScore 0 0 (1/2) ∧ selectionScore 0 0 8 < selectionScore 1 2 8
```

### MachineTemperatureWitness.lean

Commit `b31e5db2e494a0f11c4c35077c25d7399b1d19c9`; SHA-256 `99358152208755d796edbd2eb999ab1a8c2aa95a050f9c46f5d8a0c8f4cb9d69`.

Declared names: `productionFloorGainOne`, `belowFloorBetaReference`, `nonfiniteBetaReference`, `zeroBetaReference`, `negativeBetaReference`, `productionGainFloorReference`, `threeLawsReference`.

```lean
theorem productionFloorGainOne : machineTemperature ⟨.spread, 1/100, 3/5, 1, none⟩ = .ok (3/5) ∧ machineTemperature ⟨.selectionGainOnly, 1/100, 3/5, 1, none⟩ = .ok 1
theorem belowFloorBetaReference : machineTemperature ⟨.variationalBetaGamma, 1/100, 3/5, 2, some (.finite (1/1000))⟩ = .ok (1/1000)
theorem nonfiniteBetaReference : machineTemperature ⟨.variationalBetaGamma, 1/100, 3/5, 2, some .nonfinite⟩ = .error .invalidVariationalBeta
theorem zeroBetaReference : machineTemperature ⟨.variationalBetaGamma, 1/100, 3/5, 2, some (.finite 0)⟩ = .error .invalidVariationalBeta
theorem negativeBetaReference : machineTemperature ⟨.variationalBetaGamma, 1/100, 3/5, 2, some (.finite (-1/4))⟩ = .error .invalidVariationalBeta
theorem productionGainFloorReference : machineTemperature ⟨.selectionGainOnly, 1/100, 3/5, 0, none⟩ = .ok 100
theorem threeLawsReference : machineTemperature (probeOpts .spread) = .ok (3/10) ∧ machineTemperature (probeOpts .selectionGainOnly) = .ok (1/2) ∧ machineTemperature (probeOpts .variationalBetaGamma) = .ok (1/4)
```

### MachineVocabularyWitness.lean

Commit `a33f1625e18c62c48a0f53d25494ca6fb20cabbf`; SHA-256 `8198aab5dbbe6d2417aef5c3ff4f8d0d0bb4ed792f051aa87767c8c652bbb9ea`.

Declared names: `Control`, `U`, `recordedPolicy`, `recordedControlPolicy`, `temperature`, `harmony`, `AlivenessReference`, `alivenessReference`, `recordedAliveness`, `ActGateReference`, `actGateReference`, `recordedActGatePass`, `recordedActGateMissing`, `OutcomeClass`, `recordedCohort`, `recordedCohortFields`.

```lean
theorem recordedControlPolicy : recordedPolicy.controls = [Control.observe, Control.act]
theorem recordedAliveness : aliveness temperature harmony = alivenessReference.expectedAliveness
theorem recordedActGatePass : actGate (some actGateReference.passingCascade) (some actGateReference.passingCoverageDelta) = .pass
theorem recordedActGateMissing : actGate none (some actGateReference.missingCoverageDelta) = .abstainMissingLeg
theorem recordedCohortFields : recordedCohort.id = "wm-outer-loop-46-v1" ∧ recordedCohort.semanticEpoch = "omni-jvm" ∧ recordedCohort.stoppingTarget = 3 ∧ recordedCohort.attempts = [1, 2, 3]
```

### ModelReductionFreeEnergyChangeTypeNegative.lean

Commit `f0da9cbe83ae8edfed844d30ee121e3d388b3785`; SHA-256 `a35a35acc85d3c2eaed1f98fec9a92c45e294db16ec5b986cc1cd252aeaeef9a`.

Declared names: `perTickMismatch`, `badReductionChange`.

### ModelReductionFreeEnergyChangeValueNegative.lean

Commit `444c22e92c4fdb24a11c3921d7265a44e7ae1209`; SHA-256 `7a91a916c585710df01ad869f33f6d0cc4210835ce05d77fec3545dc006ed379`.

Declared names: none matched the top-level declaration syntax (negative elaboration fixtures may use anonymous `example`/guard commands).

### ModelReductionFreeEnergyChangeWitness.lean

Commit `45f3e4eeb7fd61234ad0b5732c7c2730c8c03cc5`; SHA-256 `e2db3dce6c5cf26b92aab789bbc0108d9353968679887263a59a44341dd4cd50`.

Declared names: `ReductionChangeReference`, `reductionChangeReference`, `gammaIdentityChange`.

```lean
theorem gammaIdentityChange : (modelReductionFreeEnergyChange reductionChangeReference.A reductionChangeReference.reducedPrior reductionChangeReference.prior reductionChangeReference.reducedPosterior).value = reductionChangeReference.expectedChange
```

### ModelUncertaintyEIGCollapsedNegative.lean

Commit `af58bec5ed639b6fefc62ec230e4b29d13aeec07`; SHA-256 `3f11494bf138983275f0341fe4118d99167ea4542186e9c018438f58d97c6477`.

Declared names: none matched the top-level declaration syntax (negative elaboration fixtures may use anonymous `example`/guard commands).

### ObservationKernelWitness.lean

Commit `ba845ea7b1bc76c81e835641470c9d4605cf82ca`; SHA-256 `45cd605d2c32e2a63a24308465fcf439f9c52f1df4c9b84654c4a449914f11bc`.

Declared names: `State`, `Observation`, `reference`, `referenceRowMass`.

```lean
theorem referenceRowMass : observationKernelRowMass reference .latent = 1
```

### ObservationVectorOutcomeNegative.lean

Commit `f0da9cbe83ae8edfed844d30ee121e3d388b3785`; SHA-256 `cc90bc05ba7527e0852649e02e7f462f78eb8352634779d9c7dff9ce142a16c3`.

Declared names: `Observation`, `Obs`, `oneOutcome`, `badObservation`.

### ObservationVectorPartialNegative.lean

Commit `f0da9cbe83ae8edfed844d30ee121e3d388b3785`; SHA-256 `c864c11ed8275f004e5dc85a4e0fbf9106761819fb5b87329d79b2115b35193a`.

Declared names: `partialMeasurement`, `badObservation`.

### ObservationVectorWitness.lean

Commit `8078028d24528d172cc91697f84e52c42021b7e1`; SHA-256 `0dcf34e615e55e2213a5dfad8fe52664cefda6edabe159f8338a705d75186fa8`.

Declared names: `observed`, `completeCoordinateValues`.

```lean
theorem completeCoordinateValues : Channel.all.map observed.value = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13]
```

### ParameterPosteriorKernelOutcomeNegative.lean

Commit `f0da9cbe83ae8edfed844d30ee121e3d388b3785`; SHA-256 `14e844f0c911bd933083ccbccdac883681ef74703018f027c37ab51731e3e662`.

Declared names: `outcomePrediction`, `badPosterior`.

### ParameterPosteriorKernelPriorNegative.lean

Commit `f0da9cbe83ae8edfed844d30ee121e3d388b3785`; SHA-256 `226181358af6cdfff572098cf6b1750b2450907c487d81cc61e00461f4a8e41c`.

Declared names: `parameterPrior`, `badPosterior`.

### ParameterPosteriorKernelWitness.lean

Commit `f8c58fffb4d2b32c49e3feb642c9c8c8c79b9a67`; SHA-256 `26646853804a2aac3b2c1b3db7f5c079376ebecc8b6bd92b6c960344c53b2632`.

Declared names: `Policy`, `Observation`, `Parameter`, `Obs`, `clear`, `blocked`, `posterior`, `observedPolicyRowMass`, `allPosteriorRowsNormalised`.

```lean
theorem observedPolicyRowMass : ((posterior.support (.inspect, clear)).map (posterior.mass (.inspect, clear))).sum = 1
theorem allPosteriorRowsNormalised (p : Policy) (o : Outcome Obs) : ((posterior.support (p, o)).map (posterior.mass (p, o))).sum = 1
```

### ParameterPriorKernelHabitNegative.lean

Commit `2dc0228267ab4212bc7ee6b91cfda739243bbdc2`; SHA-256 `3106d2194237a77a647c1222094420feec665a8f9c0e29724a121d48692bf88f`.

Declared names: `habit`, `badPrior`.

### ParameterPriorKernelOutcomeNegative.lean

Commit `2dc0228267ab4212bc7ee6b91cfda739243bbdc2`; SHA-256 `140d928ffaddde32d7b3833d491da14c21e0dfd193eb1636ac0ea3897d45a211`.

Declared names: `Observation`, `Obs`, `clear`, `outcomePrediction`, `badPrior`.

### ParameterPriorKernelWitness.lean

Commit `f8c58fffb4d2b32c49e3feb642c9c8c8c79b9a67`; SHA-256 `ffe8430504af573c1b04002af72a33cd7607e5c082817acb89335d95bd640d59`.

Declared names: `Policy`, `Parameter`, `prior`, `inspectRowMass`, `allPriorRowsNormalised`.

```lean
theorem inspectRowMass : ((prior.support .inspect).map (prior.mass .inspect)).sum = 1
theorem allPriorRowsNormalised (p : Policy) : ((prior.support p).map (prior.mass p)).sum = 1
```

### PolicyGrade.lean

Commit `b78ebc428b74fb99fd83349d6c86dfa192707bc6`; SHA-256 `33ba59591b6c3adaa4a5502265287644d8c25654d7a8405efb54a6d4eff9da7f`.

Declared names: `Run`, `sustainedSingleAction`, `wiringSensitive`, `wiringSensitive_needs_two_wirings`, `singleton_wiring_fails_sg4`, `earnsPolicyGrade`, `Action`, `grimTriggerSharer`, `hardcodedSharerScore`, `grim_trigger_sharer_refused_by_sg2`, `grimTriggerSnatcher`, `hardcodedSnatcherScore`, `grim_trigger_snatcher_passes_sg2_fails_sg4`, `PatternWiring`, `patternDrivenSnatcher`, `patternScoreUnder`, `pattern_driven_g4_snatcher_earns_policy_grade`.

```lean
theorem wiringSensitive_needs_two_wirings {Wiring Score : Type} [DecidableEq Score] (scoreUnder : Wiring → Score) (h : wiringSensitive scoreUnder) : ∃ wiring₁ wiring₂ : Wiring, wiring₁ ≠ wiring₂
theorem singleton_wiring_fails_sg4 {Score : Type} [DecidableEq Score] (scoreUnder : Unit → Score) : ¬ wiringSensitive scoreUnder
theorem grim_trigger_sharer_refused_by_sg2 : grimTriggerSharer.score = 5 ∧ sustainedSingleAction grimTriggerSharer ∧ ¬ earnsPolicyGrade grimTriggerSharer () hardcodedSharerScore
theorem grim_trigger_snatcher_passes_sg2_fails_sg4 : grimTriggerSnatcher.score = -1 ∧ ¬ sustainedSingleAction grimTriggerSnatcher ∧ ¬ wiringSensitive hardcodedSnatcherScore ∧ ¬ earnsPolicyGrade grimTriggerSnatcher () hardcodedSnatcherScore
theorem pattern_driven_g4_snatcher_earns_policy_grade : patternDrivenSnatcher.score = 3 ∧ patternScoreUnder .onePromoted = -5 ∧ earnsPolicyGrade patternDrivenSnatcher .observed patternScoreUnder
```

### PolicyPriorKernelNegative.lean

Commit `2dc0228267ab4212bc7ee6b91cfda739243bbdc2`; SHA-256 `c26867de594b56adaff542b22ba5b3cd15ef129352b252ae5387db5325b21e6d`.

Declared names: `Policy`, `HiddenState`, `stateConditioned`, `badPrior`.

### PolicyPriorKernelWitness.lean

Commit `ba845ea7b1bc76c81e835641470c9d4605cf82ca`; SHA-256 `abeb762ac4266a17833ef26c3510a3a81f2db9d0d06457f5d9b44fc511da834d`.

Declared names: `Policy`, `reference`, `referenceRowMass`.

```lean
theorem referenceRowMass : ((reference.support ()).map (reference.mass ())).sum = 1
```

### PrecisionNegative.lean

Commit `2dc0228267ab4212bc7ee6b91cfda739243bbdc2`; SHA-256 `1941b78014cfe2a07599e1bd030080a7643d9ffff0bb26b6cc38ca8bca92ba12`.

Declared names: `signedError`, `badPrecision`.

### PrecisionWitness.lean

Commit `b068089813f5873170b1cf78d7739d40851b90db`; SHA-256 `cbb9f5dd0b5f663d1e285913602ca2837f4fc53bd49f32e4a5e1b42b2c918f51`.

Declared names: `precisionTwo`, `precisionOne`, `PrecisionReference`, `precisionReference`, `weightedReference`, `swappedReference`, `precisionAndErrorAreNotInterchangeable`.

```lean
theorem weightedReference : variationalFreeEnergy (fun k => (precisionTwo k).value) (fun _ => precisionReference.weightedError) = ⟨precisionReference.weightedF⟩
theorem swappedReference : variationalFreeEnergy (fun k => (precisionOne k).value) (fun _ => precisionReference.swappedError) = ⟨precisionReference.swappedF⟩
theorem precisionAndErrorAreNotInterchangeable : variationalFreeEnergy (fun k => (precisionTwo k).value) (fun _ => 1) ≠ variationalFreeEnergy (fun k => (precisionOne k).value) (fun _ => 2)
```

### PredictionErrorNegative.lean

Commit `af58bec5ed639b6fefc62ec230e4b29d13aeec07`; SHA-256 `4a86f6e2ed6af2e8c7010c7167fb62a946afcad1e1054b47b4758e65ce737a5e`.

Declared names: `reversedSignSlips`.

```lean
theorem reversedSignSlips (k : Channel) : predictionError observed predicted k = 2
```

### PredictionErrorWitness.lean

Commit `b068089813f5873170b1cf78d7739d40851b90db`; SHA-256 `f4fa3635760f1a41e132872b1e7b3ed514837d2c824153d6aa77863b345b556b`.

Declared names: `observed`, `predicted`, `ErrorReference`, `errorReference`, `signedDifferenceReference`, `differsFromObservation`, `differsFromPrediction`.

```lean
theorem signedDifferenceReference (k : Channel) : predictionError observed predicted k = errorReference.expectedError
theorem differsFromObservation (k : Channel) : predictionError observed predicted k ≠ observed.value k
theorem differsFromPrediction (k : Channel) : predictionError observed predicted k ≠ predicted k
```

### PredictiveOutcomeKernelSoftmaxNegative.lean

Commit `2dc0228267ab4212bc7ee6b91cfda739243bbdc2`; SHA-256 `65431ad0fbe0da42747477afcb74624a3f4a89b77cc05f83a1b128dfee25047b`.

Declared names: `policyPosterior`, `badPredictive`.

### PredictiveOutcomeKernelUnconditionalNegative.lean

Commit `2dc0228267ab4212bc7ee6b91cfda739243bbdc2`; SHA-256 `9c7beb51900ba65f2e4f2f646da26060b11073a28888b50bd225e1e0ee1b89bf`.

Declared names: `unconditional`, `badPredictive`.

### PredictiveOutcomeKernelWitness.lean

Commit `f8c58fffb4d2b32c49e3feb642c9c8c8c79b9a67`; SHA-256 `2834925bcd7424425e002e05cd6faddbc5e0f1601a478058d425247c8019be4d`.

Declared names: `Policy`, `Observation`, `Obs`, `clear`, `fixed`, `predictive`, `inspectRowMass`, `repairRowMass`, `allPolicyRowsNormalised`.

```lean
theorem inspectRowMass : ((predictive.support .inspect).map (predictive.mass .inspect)).sum = 1
theorem repairRowMass : ((predictive.support .repair).map (predictive.mass .repair)).sum = 1
theorem allPolicyRowsNormalised (p : Policy) : ((predictive.support p).map (predictive.mass p)).sum = 1
```

### PredictiveOutcomeRiskWitness.lean

Commit `3f5654ce21cd809400f0a4780acfa603e155419b`; SHA-256 `c49a36983a3d4f7a72d782a5ecf9825c95bac49c354cf9d80d574d522894e58a`.

Declared names: `Policy`, `Observation`, `Obs`, `oa`, `ob`, `predictive`, `preference`, `positivePreference`, `RiskReference`, `riskReference`, `pointMassAgainstUniform`.

```lean
theorem positivePreference : ∀ π o, o ∈ predictive.support π → 0 < preference.mass () o
theorem pointMassAgainstUniform : predictiveOutcomeRisk predictive preference positivePreference .chooseA = riskReference.expectedRisk
```

### PreferenceDistributionConditioningNegative.lean

Commit `2dc0228267ab4212bc7ee6b91cfda739243bbdc2`; SHA-256 `34dafb1039b865bc489281a4a26f5a9662076b52956add4ee1fce93cb0af9124`.

Declared names: `HiddenState`, `stateConditioned`, `badPreference`.

### PreferenceDistributionPragmaticCostNegative.lean

Commit `fcd1261c303c2beca08a6812eba4a7ce2e83d722`; SHA-256 `0b87caae2bf3f0abaccf1d9e4f84d00d0964cad43664e6bc679f85d9862dcbe9`.

Declared names: `pragmaticCost`, `badPreference`.

### PreferenceDistributionWitness.lean

Commit `0c0f2876a55c528629a43103df3d4c38e36472f8`; SHA-256 `2fc09f5fade7454388cd2fcb32c81b45813c048ff65c376483377a2b93efce80`.

Declared names: `Observation`, `Obs`, `good`, `bad`, `fair`, `fairMassesSumToOne`.

```lean
theorem fairMassesSumToOne : ((fair.support ()).map (fair.mass ())).sum = 1
```

### PreferenceLadderDraft.lean

Commit `fcd1261c303c2beca08a6812eba4a7ce2e83d722`; SHA-256 `f663932ebf7e2b3bd6031ecfe38d38c9a510e2bf707aec9c52ae2c9ac8c86396`.

Declared names: `PragmaticObservation`, `LadderOutcome`, `PreferenceFamily`, `DispositionKernel`, `dispositionPredictiveMass`, `SatisfiesDispositionBridge`, `dispositionRisk`, `SingleSupportRiskIsPolicyConstant`, `EvidentialFacet`, `OutcomeFacets`.

### PreferenceRiskBoundary.lean

Commit `ef8d39379c3a9052c4e8db1c00f4c78261653cc2`; SHA-256 `29c10485d3365030000658a96359d1c994b20b1789c47156f10f9486bc12cfaa`.

Declared names: `runtimeRiskContributionIds`, `addRisk`, `contribution_delta`, `zero_weight_preserves`, `separatedOutputs`, `preference_projection_unchanged`, `grounded_risk_sum`, `dropped_contribution_refused`.

```lean
theorem contribution_delta {Policy : Type*} (base : Policy → ℝ) (weight : ℝ) (risk : RiskContribution Policy) (p : Policy) : addRisk base weight risk p - base p = weight * risk.value p
theorem zero_weight_preserves {Policy : Type*} (base : Policy → ℝ) (risk : RiskContribution Policy) : addRisk base 0 risk = base
theorem preference_projection_unchanged {Policy Outcome : Type*} (preference : Outcome → ℝ) (layers : List (PreferenceLayer Outcome)) (baseRisk : Policy → ℝ) (weight : ℝ) (risk : RiskContribution Policy) : (separatedOutputs preference layers baseRisk weight risk).1 = foldC preference layers
theorem grounded_risk_sum (base : Unit → ℝ) (weight : ℝ) : addRisk base weight (scalarKL groundedPrediction F10RuledCarrier.seed grounded_admissible) () = base () + weight * Real.log 2
theorem dropped_contribution_refused : addRisk (fun _ : Unit => 0) 1 ⟨fun _ => 1⟩ () ≠ 0
```

### PreferenceRiskSeparation.lean

Commit `b76719cd23f185c3c1f73c3e052e5d36922ed2d6`; SHA-256 `b7317a54292bca223d912f771a9821865068a99060b0e338f81b6e0e261b05c0`.

Declared names: `constantConditional`, `predictiveMass`, `constant_absorbs_prediction`, `RiskContribution`, `riskAdmissible`, `scalarKL`, `preferred_zero_refuses`, `grounded_seed_risk`.

```lean
theorem constant_absorbs_prediction {Policy Observation Disposition : Type*} (Q : ProbabilityKernel Policy Observation) (distribution : ProbabilityKernel Unit Disposition) (p : Policy) (d : Disposition) : predictiveMass Q (constantConditional distribution) p d = distribution.mass () d
theorem preferred_zero_refuses {Policy Disposition : Type*} (Q : ProbabilityKernel Policy Disposition) (C : ProbabilityKernel Unit Disposition) (p : Policy) (d : Disposition) (hd : d ∈ Q.support p) (hq : Q.mass p d ≠ 0) (hc : C.mass () d = 0) : ¬ riskAdmissible Q C
theorem grounded_seed_risk : (1 : ℝ) * Real.log (1 / (1 / 2)) = Real.log 2
```

### PreferenceRiskWitness.lean

Commit `2af52ef97ebb7d52d8852483ae0ad06795ef0cba`; SHA-256 `f50d20c507b58a4d7f3d95e2f2a1816cde0fb1ca773e8b047ebcb788f3934cbf`.

Declared names: `groundedPrediction`, `grounded_admissible`, `concrete_scalarKL`, `same_twelve_support`, `abstained_refuses`.

```lean
theorem grounded_admissible : riskAdmissible groundedPrediction seed
theorem concrete_scalarKL : (scalarKL groundedPrediction seed grounded_admissible).value () = Real.log 2
theorem same_twelve_support : groundedPrediction.support () = seed.support () ∧ (groundedPrediction.support ()).length = 12
theorem abstained_refuses (Q : ProbabilityKernel Unit (Outcome SeedObs)) (hs : organisationOutcome .abstained ∈ Q.support ()) (hp : Q.mass () (organisationOutcome .abstained) ≠ 0) : ¬ riskAdmissible Q seed
```

### Run4Preregistration.lean

Commit `b9005f4f8b6f288981fd8aa7db206d671bb09695`; SHA-256 `880dfab4dc37ddb42953cd80e309deb857f53ef8a2afd8de0dd616b2c91bc6fa`.

Declared names: `CandidateRun`, `AcceptanceStatus`, `PreregAssertion`, `PinKind`, `PinnedAuthority`, `InvalidatorId`, `Invalidator`, `run4Assertions`, `run4Authorities`, `run4Invalidators`, `run4CertifiedS5Conformance`, `run4CertifiedS5Census`, `run4CertifiedRe5Conformance`, `run4CertifiedRe5Census`, `wmRun4PreregCensus`, `wmRun4InvalidatorsTouchingDefinitions`.

```lean
theorem run4CertifiedS5Conformance : runConformsToDrawnWiring s5Routes
theorem run4CertifiedS5Census : s5Routes.length = 4 ∧ s5Hops.length = 36 ∧ s5Hops.dedup.length = 9 ∧ (s5Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.drawn))).length = 2 ∧ (s5Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.routeMeasured))).length = 5 ∧ (s5Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.excludedDependencyGrain))).length = 1 ∧ (s5Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.rulingUnrealised))).length = 1 ∧ (s5Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.refutation))).length = 0 ∧ (s5Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.unmapped))).length = 0 ∧ figureDrawnEdges.length = 22 ∧ s5UnfiredDrawnEdges.length = 19
theorem run4CertifiedRe5Conformance : runConformsToDrawnWiring re5Routes
theorem run4CertifiedRe5Census : re5Routes.length = 4 ∧ re5Hops.length = 36 ∧ re5Hops.dedup.length = 9 ∧ (re5Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.drawn))).length = 2 ∧ (re5Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.routeMeasured))).length = 5 ∧ (re5Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.excludedDependencyGrain))).length = 1 ∧ (re5Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.rulingUnrealised))).length = 1 ∧ (re5Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.refutation))).length = 0 ∧ (re5Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.unmapped))).length = 0 ∧ figureDrawnEdges.length = 22 ∧ re5UnfiredDrawnEdges.length = 19
theorem wmRun4PreregCensus : run4Assertions.length = 4 ∧ (run4Assertions.filter (fun a => decide (a.status = AcceptanceStatus.mintedAwaitingAcceptance))).length = 4 ∧ run4Authorities.length = 5 ∧ run4Invalidators.length = 4 ∧ (run4Invalidators.filter (fun i => i.touchesCertificateDefinitions)).length = 1 ∧ (run4Invalidators.filter (fun i => !i.measured)).length = 0
theorem wmRun4InvalidatorsTouchingDefinitions : (run4Invalidators.filter (fun i => i.touchesCertificateDefinitions)).map (fun i => i.id) = [InvalidatorId.postPinControlMapDecisions]
```

### RunLifecycleContractDraft.lean

Commit `00eb0c045d947ed498491b11d99e0815d530324f`; SHA-256 `6d80419c3dc7c2bba0b8a7218a6da6840bdd0ee3be735b175bacd4b20ca36927`.

Declared names: `duplicateAttempts`, `current_cohort_does_not_require_unique_attempts`, `AttemptKey`, `distinct_cohorts_have_distinct_keys`, `local_projection_collides`, `Lifecycle`, `mayStart`, `mayInspect`, `exhausted_fresh_refuses`, `started_never_redispatches`, `consumed_attempt_remains_inspectable`, `observeTerminal`, `missing_evidence_stays_started`, `foreign_evidence_cannot_complete`.

```lean
theorem current_cohort_does_not_require_unique_attempts : ¬ duplicateAttempts.attempts.Nodup
theorem distinct_cohorts_have_distinct_keys (a b : AttemptKey) (h : a.cohort ≠ b.cohort) : a ≠ b
theorem local_projection_collides : (AttemptKey.mk 1 1).localOrdinal = (AttemptKey.mk 2 1).localOrdinal ∧ AttemptKey.mk 1 1 ≠ AttemptKey.mk 2 1
theorem exhausted_fresh_refuses : mayStart 0 .fresh = false
theorem started_never_redispatches (n : Nat) (key : AttemptKey) : mayStart n (.started key) = false
theorem consumed_attempt_remains_inspectable (key : AttemptKey) : mayStart 0 (.started key) = false ∧ mayInspect (.started key) = true
theorem missing_evidence_stays_started (key : AttemptKey) : observeTerminal key none = .started key
theorem foreign_evidence_cannot_complete (key observed : AttemptKey) (h : observed ≠ key) : observeTerminal key (some observed) = .started key
```

### SoftmaxNegative.lean

Commit `af58bec5ed639b6fefc62ec230e4b29d13aeec07`; SHA-256 `b11de63c680bb8b692070b1eea1a3832beda9af6fa0ad9c92ae8b04241fc03e8`.

Declared names: `invertedOrderSlips`.

```lean
theorem invertedOrderSlips : softmax Real.exp Real.log habit grade (1 / 3 : ℝ) [.lower, .higher] = [1 / 9, 8 / 9]
```

### SoftmaxWitness.lean

Commit `b068089813f5873170b1cf78d7739d40851b90db`; SHA-256 `c114ea5771c8f459409e384d43679632a5778689bb3f83701feb4d1f2392b177`.

Declared names: `TestPolicy`, `habit`, `grade`, `SoftmaxReference`, `softmaxReference`, `referenceWeights`, `referenceNormalised`, `lowerGradeHasHigherProbability`.

```lean
theorem referenceWeights : softmax Real.exp Real.log habit grade softmaxReference.temperature [.lower, .higher] = [softmaxReference.lowerProbability, softmaxReference.higherProbability]
theorem referenceNormalised : (8 / 9 : ℝ) + 1 / 9 = 1
theorem lowerGradeHasHigherProbability : (8 / 9 : ℝ) > 1 / 9
```

### TransitionKernelBetaNegative.lean

Commit `2dc0228267ab4212bc7ee6b91cfda739243bbdc2`; SHA-256 `0e97bd7f6440c5916c1f21af239a931a18df4e4c9a634d531f49c41bbb0fc75c`.

Declared names: `alpha`, `betaNormaliser`, `badTransition`.

### TransitionKernelUncontrolledNegative.lean

Commit `2dc0228267ab4212bc7ee6b91cfda739243bbdc2`; SHA-256 `2b131e95f9cd932d9d9a28b91c428f72d7284fcdeb3afc1d573319b23fb93efd`.

Declared names: `uncontrolled`, `badTransition`.

### TransitionKernelWitness.lean

Commit `f8c58fffb4d2b32c49e3feb642c9c8c8c79b9a67`; SHA-256 `f284359c5608affe050eadda3e90e9ce7289a403e134aa8aa3cf4227ab04e958`.

Declared names: `State`, `Action`, `controlled`, `startRowMass`, `allControlledRowsNormalised`.

```lean
theorem startRowMass : ((controlled.support (.idle, .start)).map (controlled.mass (.idle, .start))).sum = 1
theorem allControlledRowsNormalised (s : State) (a : Action) : ((controlled.support (s, a)).map (controlled.mass (s, a))).sum = 1
```

### VariationalFreeEnergyNegative.lean

Commit `f0da9cbe83ae8edfed844d30ee121e3d388b3785`; SHA-256 `d322688345a7d6b7de7e7772c85c51746c7369d956fb12706b0ed2f8490b641b`.

Declared names: `expectedValue`, `badVariationalValue`.

### VariationalFreeEnergyWitness.lean

Commit `600a21902a3fdc57b38f5abfc05f5b3c9a501a6e`; SHA-256 `d0447af4f1ca597cb02b7642ee612da720a950555dff27bf0376d4c443df3805`.

Declared names: `GaussianReference`, `gaussianReference`, `constantGaussianReference`.

```lean
theorem constantGaussianReference : variationalFreeEnergy (fun _ => gaussianReference.precision) (fun _ => gaussianReference.predictionError) = ⟨gaussianReference.expectedVariationalF⟩
```

## Appendix B — runtime record pins

The first twelve entries are the scoring report’s exact glob; three additional construction paths discovered outside that glob follow. Checkpoint bytes are not git-versioned here, so SHA-256 is their audit identity. Selection and close hashes bind the observations in the table.

- IN twelve: `/home/joe/run4/U88-codex20-20260911/cohort/run4-u88-codex20-20260911-v1/attempt-001/003-construction.edn`
  - `002-selection.edn`: `a3507ba5c1b3551762892d565d5cf36fd925aa539658db075f3147effc746f89`
  - `003-construction.edn`: `385bea6fb57b0a2ac4cbf45ac9324fbefed0f617e1564548b5e283b329f36ed0`
  - `007-closed.edn`: absent

- IN twelve: `/home/joe/run4/U88-codex20-v2-20260911/cohort/run4-u88-codex20-20260911-v2/attempt-001/003-construction.edn`
  - `002-selection.edn`: `022147854511591d44a86cfb6dca8ffa9564bbb4ab7858ab1222d561cf5d8fc3`
  - `003-construction.edn`: `6a7e284b69b0fbc8f744506ecd8fe5e22a3d10e94f6169774d861a856f870577`
  - `007-closed.edn`: `9d6ad9414ed284e966412a0ff457fee316605fabca052327615c8f8044bbfd30`

- IN twelve: `/home/joe/run4/U88-zai-successor-20260912-v4/cohort/run4-u88-zai-successor-20260912-v4/attempt-001/003-construction.edn`
  - `002-selection.edn`: `dde6e31335bc0a2273494cbb51871368c8351df771ad7cdd530d90645b0bd6de`
  - `003-construction.edn`: `af7ad4b55801cc7986a2be118a6e755ebe40f6cfa906c1037c12d9742d602f6e`
  - `007-closed.edn`: `8c8428fcbba78288cf7b97443b415f7c5eec9783283957cfdd3c4e1268dd4beb`

- IN twelve: `/home/joe/run4/U88-zai-successor-20260912/cohort/run4-u88-zai-successor-20260912-v1/attempt-001/003-construction.edn`
  - `002-selection.edn`: `e709a0aead5931c9669981cad1a269d02456f1979d836f36c36ade37dc9c5f5b`
  - `003-construction.edn`: `a3a30d182b8022ac50a96dac0a629fc594f963debaeeb064d6de4477014eeebc`
  - `007-closed.edn`: `2dc9d6747ad6a1a12895dded74f146551ec210084bbf13547238bc838701de6d`

- IN twelve: `/home/joe/run4/ea1-admission-20260912-v2/cohort/run4-ea1-artifact-binding-admission-20260912-v2/attempt-001/003-construction.edn`
  - `002-selection.edn`: `836cf030a4eaba456f32d29f4c1b28937eb774c62fbb00977defa16d9a178613`
  - `003-construction.edn`: `f74e3626caa55549d09044c147cc68dfc3b28d6c7a009c06d68365f551b64ddd`
  - `007-closed.edn`: `448dd2bded99e9d8706e489b717e2b5baeb590f9c409bb81821d0dfa6e63e9f9`

- IN twelve: `/home/joe/run4/ea1-admission-20260912/cohort/run4-ea1-artifact-binding-admission-20260912-v1/attempt-001/003-construction.edn`
  - `002-selection.edn`: `00cfeb933342d092dee3fd0ee336657e219b393ebdf4410c5d1c9f76b3e206c8`
  - `003-construction.edn`: `d1e4a8b0613fc8be34eba0a3bd370ad7248b53b2857ec84384e1061cd9ac5fb4`
  - `007-closed.edn`: `c226e20e8d5bef3edacc1455721a3622ec1eac58d7a2a820c7ac9a93872294ca`

- IN twelve: `/home/joe/run4/initialization-close-admission/cohort/run4-initialization-close-admission-20260911-v1/attempt-001/003-construction.edn`
  - `002-selection.edn`: `d3e28bf15f5a8e47d2fdb9d42f75ae233ef88ffd4d0eca35038f8e8885adb6dc`
  - `003-construction.edn`: `84c216b1d724c8f2a61d81c0f3edfabb58e07f50617e7c168f2f8fe51e4f736e`
  - `007-closed.edn`: `339082f25489196e798fdc2314fc59cfb4ba7a6b82e3ae31376f09e8936b2c13`

- IN twelve: `/home/joe/run4/initialization-collision-admission/cohort/run4-initialization-collision-admission-20260911-v1/attempt-001/003-construction.edn`
  - `002-selection.edn`: `fed5bb1ced92f60dacc7ba35625862f9cb810e2917789550f16bf0d63a7d585c`
  - `003-construction.edn`: `a2ab3a17994c1a74014b8616a840aca47037fee8b4abfd9d6b1c0f6e09603d39`
  - `007-closed.edn`: `fb7113e464823bb7546a8487514200e7fb6ca1004b5ab4c9dadc5b67c46759a4`

- IN twelve: `/home/joe/run4/initialization38690-admission/cohort/run4-initialization38690-admission-20260911-v1/attempt-001/003-construction.edn`
  - `002-selection.edn`: `fe6b44bc5f7a891d839a5ea72d42928ba1db4fafd365cedbfe78c5f3c51bfc9e`
  - `003-construction.edn`: `d865f2f27cfd017a93c23937b3d25dd19eed6b37a1e95a2d4c26a5702415a01b`
  - `007-closed.edn`: `2069f66e54e97fb3f83dc96316d42f3b588404505fa7c0fa11bde64e8786e6fd`

- IN twelve: `/home/joe/run4/repair-pinned-selection-admission/cohort/run4-repair-pinned-selection-admission-20260911-v1/attempt-001/003-construction.edn`
  - `002-selection.edn`: `e2bc662bf5ab366eb6a92439f52440ff625c2f48bd724c1d6b419fb3f8ae0050`
  - `003-construction.edn`: `d734daf6d1d9ddaabdcd12bb80596fb306a8e86a838021a339ad6d09980b673d`
  - `007-closed.edn`: `3062d4102b14c7d86b2555226f4b77efae88c36c21429cfa3170f3f27042e586`

- IN twelve: `/home/joe/run4/repair058-admission/cohort/run4-repair058-admission-20260911-v1/attempt-001/003-construction.edn`
  - `002-selection.edn`: `8dfa817e8cfbce871375ddca483d12fb6f7df92e3f0d9342194200bc9a494bf7`
  - `003-construction.edn`: `eab99164e003fa2a9387dc7670964f98222e45b60376a103f2f086620f708ece`
  - `007-closed.edn`: `a01e7617db7c3748fcc504cfcddd42538ae4e9b7c0e3a259cfcc68e37f083222`

- IN twelve: `/home/joe/run4/successor-v2-selection-admission/cohort/run4-successor-v2-selection-admission-20260911-v1/attempt-001/003-construction.edn`
  - `002-selection.edn`: `e728dad5c575b8d29da93e1e380dc0e96feb859b5532bd99ab93224b3d6a957d`
  - `003-construction.edn`: `4f896dd39df56cf8dc5380fe7ff3672bb4d0886211ce3f9f8753ed9d80bc7101`
  - `007-closed.edn`: `3f9347ec58c93c2963ce7f0b56b81acb8fcbedc58a724b2cabe8683cab2ea40a`

- OUTSIDE glob: `/home/joe/run4/U88-cohort-20260911/run4-u88-20260911-v1/attempt-001/003-construction.edn`
  - `002-selection.edn`: `5985ba84105ba0b6519a355738a0aef971734c504446b0745f0ffc362ee758a4`
  - `003-construction.edn`: `91e3a7a91ac442261b45a825ac7e40f267618d1a9ed21eac78ed3e55330fd888`
  - `007-closed.edn`: absent

- OUTSIDE glob: `/home/joe/run4/U88-production-successor-20260911-v2/cohort-data/run4-u88-production-successor-20260911-v2/attempt-001/003-construction.edn`
  - `002-selection.edn`: `6836c6caf4c9087e15ae2052ede8c848fd3b078aa5df90630d9d881784663221`
  - `003-construction.edn`: `dd72b3cb4029ea2d891aeb6373dea54872903d0b3c7625f237e069f3ebac60a2`
  - `007-closed.edn`: `b572358898e57271b788d897bb0fd975b1d93367d62fd9b2e0fa8cf82fef71b6`

- OUTSIDE glob: `/home/joe/run4/repair057-admission/cohort-data/run4-repair057-admission-20260911-v1/attempt-001/003-construction.edn`
  - `002-selection.edn`: `c0d51baf8a4318c21b6e9c430504e92b22e6ea69b2359d13b62ce2374b18ee4c`
  - `003-construction.edn`: `778511b2de41c7256f4ba4460fc90557a583cbebe2cf23719f37f5813a8f53ff`
  - `007-closed.edn`: `b3b37347534c138977c596908f3b99c36643a9311dee895813a05d5f3ba1988c`

## Appendix C — source and contextual evidence pins

- `futon2/holes/NOTE-cascade-structured-proof-validation.md`: commit `79eb907d3dd63034ac5d787c654a58245c1fe996`, SHA-256 `80ac71a84783ec1494bd96e8599cc694606f1bbd1d53ed8cc43bc43d03578d77`.
- `futon2/holes/NOTE-runtime-validation-invariants.md`: commit `313fb76b53739c4b915e17fa0036b60973f5db14`, SHA-256 `a519bfc9c7907261b3a9bd9d9ee5668d6133f5bcec3286e81539150d28d2d1b8`.
- `futon2/holes/labs/wm-contract/CASCADE-RUBRIC-SCORING-2026-09-12.md`: commit `f12e0fae1bc79b30c9beaf4fa6e0afa360277ce5`, SHA-256 `c6b1ed5239d07558683d62ba8ee5ec13c72e84a50ce4c5ee720ba3f8d4154fb8`.
- `futon2/src/futon2/aif/full_loop_runner.clj`: commit `9dd4fd8dc1f20a842b65b975762df60f4296e83d`, SHA-256 `581ceed026a955abbaf05458a7c6625a4a36a0b761da3a2817b1d79bd094bb18`.
- `futon2/scripts/futon2/report/cascade_lane.clj`: commit `4e76f94f074c3582f76efe961708a155bb6c3c0a`, SHA-256 `ef751c06b764f2b7c5fe0809326af9eeb177619fa188b363cab908a47d3cb8f2`.
- `futon2/src/futon2/aif/run4_route_conformance.clj`: commit `5c932112aa937f851e95dd241a79752e7a82f3bb`, SHA-256 `9556c16ac19d5453fe4c1f5e3d6c2bf37887f5652e84a7737b1d8fd753361de1`.
- `futon3c/src/futon3c/wm/run4_acceptance_report.clj`: commit `5bb5ae8745892bd80888618d461ce6d26d907d34`, SHA-256 `d7c8926829a0391969ee3903eccb69f56b15899889f26dbbd8455d4567cbc509`.
- `futon3c/src/futon3c/wm/run4_battery.clj`: commit `5bb5ae8745892bd80888618d461ce6d26d907d34`, SHA-256 `4ac8beffa1ca26f624e8dacafbf5058539f1ec4bf811b9856203e9e11cf4fc5c`.
- `futon3c/src/futon3c/wm/run4_historical_verification.clj`: commit `8bf149c5e75fb0b94897f0834b6dabf1f59593c6`, SHA-256 `903ad022c480a79d959401128da0993c1b5681d58be16a6b164bda3658c84443`.
- `futon3c/src/futon3c/agents/inbox_zero_board.clj`: commit `52856d8fe7530dfe0c38d06525bf8b9a1f49aee6`, SHA-256 `6cfbb7a61c5242af86b8920e7701981ee3b197521e4e1814603a30df70f98b65`.
- `futon3c/src/futon3c/agents/cascade_verifier_board.clj`: commit `52856d8fe7530dfe0c38d06525bf8b9a1f49aee6`, SHA-256 `096c73181a26b39691fff7549769b68575b30e54ea5299c10c380f9e0517619a`.
- `futon3c/holes/labs/wm-contract/runs/CHIP-BOARD-WITNESS-STATUS-2026-09-12.md`: commit `52856d8fe7530dfe0c38d06525bf8b9a1f49aee6`, SHA-256 `38d5b53b38c2c2a2e1d6b1920f1d7a08ecce0ab7066c209aaba7515bb908a038`.
- `futon2/holes/labs/wm-contract/run4_readiness.bb`: commit `b9208880a1ce70905089fafba594ae61f2483c11`, SHA-256 `79bd666a394390931fb6384acb9701b1e420941e1b7013ca80fbd9bdd6ccdd5e`.
- `futon2/holes/labs/wm-contract/u49_route_transcribe.bb`: commit `5c932112aa937f851e95dd241a79752e7a82f3bb`, SHA-256 `145ee06e359f30315a32fc8f958e7415d286dff0a3cb34cb2d69b5860e8495ec`.
- `futon2/holes/labs/wm-contract/run4_prereg_transcribe.bb`: commit `3b140d59413953d7feffa7d69464738d7c2d91c2`, SHA-256 `b6bc60daa10bc5f7a16a7b97e61d00da6972bf178b198b75c17cd0d14d7f54ed`.
- `futon2/holes/labs/wm-contract/f12_ruled_carrier_check.bb`: commit `26de81219ca41b5d83a5508b3d3a3a58ba6623ff`, SHA-256 `4faac8fdcb830917b0e4b0ac9abdc5c323bc6915a2435d68df97befd10900007`.

Validation of this report: strict EDN read of every enumerated selection/construction and each available close; SHA-256 pin enumeration; source signatures enumerated from all 132 Lean files. No production checker was run and no proof elaboration is claimed. Existing unrelated dirty files were left untouched.
