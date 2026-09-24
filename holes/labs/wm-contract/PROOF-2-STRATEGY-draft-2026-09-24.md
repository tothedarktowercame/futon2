# PROOF-2 — development strategy and dispatch packets

Date: 2026-09-24. Author: codex-4. Status: planning draft for independent review; no theorem clause signed. Only this document is delivered by this packet. No click, runtime change, data write, or JVM reload is authorized by it.

## 1. First resolve what cannot yet be dispatched as a proof

The seven clauses are not seven implementation tickets. Several contain open mathematical specifications, several require an event that engineering cannot guarantee, and clause 6 quantifies over conditions the first cold-start click cannot satisfy. Dispatching “prove clause 3” as an hour's task would invite a fixture or a weakened claim.

1. **Exact real arithmetic versus consumed machine numbers.** W₃ equates a recorded floating-point scalar to a real logarithm; W₆ similarly equates a floating posterior to real softmax. Finite binary values generally cannot satisfy these equalities. W₁/W₂ have the same issue for normalized probabilities. Existing `MachinePolicyFreeEnergyWitness.lean` explicitly keeps logs symbolic and measures floating deltas separately; that is a useful style precedent, not a solution to this theorem. First specify exact rational carriers where possible and a proved numerical refinement/error relation elsewhere. A tolerance asserted by the emitter is not a Lean proof. Until that bridge exists, literal W₃/W₆ are not dispatchable positive witnesses.
2. **An observed-prefix sum is not a single-step F.** The F discovery asks for per-step F and total F; W₃ currently names one `variationalFreeEnergy lik prior q`. Define the finite sum over the admitted prefix, prove each summand, and bind the sum to selection. A length-one witness must not silently become the general definition.
3. **Cold start and new candidates.** H4 requires history of the exact policy, while W₆ requires supplied F for every candidate on every click. B4 creates new policies, and only selected policies execute. Neither five clicks nor a complete producer guarantees every candidate a nonempty history. Do not borrow another policy's prefix, fabricate a prefix, exclude new alternatives to help the proof, or count absence as zero. Existing immutable compatible histories may supply boundary inputs, but their eligibility and relationship to L must be declared before L; the supplied record does not establish that such histories exist. Under the current theorem, any absence leaves W₆ open.
4. **The F counterfactual is inconsistent across drafts.** A12 says “habit-alone,” W₃ compares the full law against F=0 while retaining G, and A17 requires two historically real values. F=0 ablation is not necessarily a historically real F. Proposed engineering correction: call W₃ an F-ablation comparison, retain G, and state an explicit A17 exception for this diagnostic intervention. Keep historical old/new B for W₅. This requires reviewed amendments before preregistration, not a silent reinterpretation by the witness author.
5. **A measurement needs identifiable observations.** Accepted outcomes alone do not identify false-positive/false-negative rates without a reference truth and an observation paired on the same occurrence. Discovery must locate those pairs and denominators. If they do not exist, the build owes a measurement producer; it cannot relabel success frequency as observation accuracy.
6. **B's mathematics and its interpretation are distinct.** The existing scalar Beta ratio is the two-outcome Jeffreys specialization of Dirichlet accumulation. A singleton state carrier can represent its arithmetic honestly. That does not prove a whole-attempt outcome is an observation of each pattern's transition, or that a singleton belief is the token posterior required elsewhere. Record the actual trial semantics and review the mapping. Do not invent posterior trial vectors retrospectively.

These are scoped discovery/specification packets below. They do not delay independent carrier work. The old proof's ⟨1⟩9 verdict is not an input premise: its reference-field scope, stipulated C, and subsequent F/B corrections prevent inheriting any discharge. PROOF-2 covers ordinary task supply, measured A, conditioning, F, Q, and the full certificate.

## 2. Dependency graph (build dependencies and evidence dependencies)

In this table “click: yes” means positive clause standing requires ordinary records from the preregistered sequence; it does not mean every supporting packet spends a click. W/P/X always travel together. X can be established without a click; W on a fixture establishes only tooling.

| Clause | Missing requirement | Supplier / immediate dependencies | Click requirement |
|---|---|---|---|
| 0: field | Ordinary interpretation, executable construction, publication, truthful derivations, semantic difference, consumed field join | B4 authoring; B3 feasible scope; existing equivalence slice 1639968f and produces reader 178a53c9; in-flight 2b carrier | Yes: at least two eligible non-equivalent ordinary candidates in the actual scorer |
| 1: A | Eligible truth/observation pairs, fixed estimator, measured rates, consumed matrix, predicted-outcome carrier | Outcome measurement from B6; A front; clause 0 universes for proof, although estimator construction runs independently | Yes: measured A consumed on the ordinary field; historical rows alone do not discharge it |
| 2: D | Compatible prior, admitted observation, exact update, retained q, consumed posterior | Clause 1 plus versioned B kernel, observation admission and continuity; learned B discrimination is not a prerequisite | Yes: at least one mathematically nondegenerate posterior; classify other cases truthfully |
| 3: F | Per-policy observed prefix, per-step q and F, prefix total, numerical bridge, actual selection-law receipt, action-changing ablation | Clauses 0/1/2; Q observation-to-conditioning production; F contract; E/C/selection carriers | Yes: supply on required clicks and a discriminating ordinary field within L |
| 4: Q | Action→execution→outcome→conditioning→next consumption joins with model compatibility | Clauses 0/1/2, B read/update evidence where eligible, B6 measurement; final closure across adjacent records | Yes: at least one complete temporal link; no F-action reversal dependency |
| 5: B | Concentrations/trial vectors/version/normalization carrier, unconditional read, compatibility rule, causal comparison | Existing ledger updater and judge consumer; B Lean bridge; B6 accepted eligible outcome; clause 0 competing field; clause 1/2 for full trial interpretation | Yes: producing close plus subsequent starts; first compatible consecutive pair must discriminate |
| 6: completeness | E/C provenance, all model inputs, Q status, B global reads, exact consumed joins, numeric refinement | Every front; clauses 0–5 on one declared L; certificate checker and witness generator | Yes: every member of L, without silently dropping failures or cold starts |

**Critical build path:** B4 ordinary field → measured observation evidence/A → admitted conditioning/D → observed-prefix F → selection consumption. Q's production half sits between D and F, not after F; Q's final temporal witness is a separate join. B versioned carrier/normalization, certificate schemas, C derivation, witness generator, and L specification can run alongside that path. If suitable observation data already exist, A can advance before B4. If they do not, outcome collection becomes the critical path and spends ordinary authorized clicks; this is not hidden “setup.”

**Critical evidence path:** predeclared L and signed baseline → ordinary field/selection → execution and measured close → admitted next-click conditioning and B read → F/Q/learning witnesses. There is no evidence that this path completes in five clicks. A run can be correctly implemented yet insensitive, lack a compatible pair, or introduce a policy without history. Those are open clauses, not reasons to extend L.

Clause 6 is designed from day one and checked last. Waiting to design its schema until all producers land would recreate record-only receipts that cannot be joined.

## 3. Development fronts

All paths in this section are futon2-relative unless prefixed `mathlib4/`. Line numbers are anchors from the theorem/discovery drafts, not immutable addresses. Builders must resolve against their committed revision, including `scripts/` consumers.

| Front | Witness definition of done | Lean: existing / owed | Runtime locus | Record obligation |
|---|---|---|---|---|
| Field authoring (B4) | W₀ plus P₀; clone, hand-admission, wrong-row X₀ fail | Existing `CascadeTransition` interpretation, guards, kernels. Owed finite normalized-equivalence and provenance carriers; no Lean definition of “real task” exists | `cascade_problems`, `mission_hole_wants`, `cascade_proposals:136–174`, `cascade_sources/check-file!`, `cascade_model_manifest:242–376`; integrate zai-2's slices | `:candidate-derivations` with every Step 0 field, normalized slots, s₀, rows; bijection with candidates/policies/scoring/action mapping; `:enumeration-completeness` exclusions |
| Measured A | W₁ proves estimator result and consumed likelihood/prediction; no identity-default substitution | Existing `TokenObservation`, `PolicyRollout.predictedOutcome`; owed estimator definition over eligible pairs and numerical bridge | `token_outcome:54`, outcome/close producers; `cascade_model_manifest:174,200,217` | `:model-inputs <id> :A`: rates, estimator/version, population/window/counts, truth/observation row ids, uncertainty/smoothing, value hash, consumed-at |
| D continuity | W₂ exact posterior with a nondegenerate ordinary instance | Existing `ExactBeliefTrajectory`, `TokenState`; owed finite compatibility/provenance and numeric refinement | `cascade_model_manifest:620,648`, `observation_admission`, `d_predecessor_task_authority`, `token_belief_predecessor` | `:model-inputs <id> :D` prior/o/A/B/q and normalization; `:token-belief-input` and `:token-belief-stage :observation-updates` |
| Q temporal production | W₄ closes the consumed chain, not a status flag | Existing rollout/exact trajectory; owed finite join proposition and boundary-state specification | Execution/close records, outcome producer, predecessor admission/read | `:Q-link`: predecessor/action/outcome identities, versions, prior/posterior hashes, consumed-at; explicit first-click boundary/status |
| F supply | W₃'s corrected prefix sum is consumed; X₃ fails; action ablation discriminates within L | Existing `PolicyVariationalFreeEnergy`, equality case in `ExactBeliefTrajectory`, `PolicySelection`, `ActionMarginal`; owed prefix-sum carrier/refinement and new contract | `policy_prefix_evidence:56–70` and `production-ranked`; `policy:235–258`; `efe:1125,1269`; `cascade_free_energy:105`; `cascade_selection:112` | Per-candidate `:f-prefix`, per-step q/F, total F, `:model-inputs <id> :F`; candidate/policy F; actual-law receipt names omitted F when absent |
| Learned B carrier | W₅ arithmetic/normalization/read chain, then compatible-pair action difference | Existing `DirichletLearning.accumulate`; owed concentration→theta normalization and specialization theorem; new contract | `learning_trial_ledger:154,238`; judge `scripts/futon2/report/war_machine:6380`; close updater | Close `:b-update` arrays/vectors/dedup/version/normalization; `:model-inputs <id> :B`; root `:B-read` even on incompatible fields, with predecessor-chain and read commit point |
| E/C and certificate completeness | W₆ joins actual inputs; A8/A9 hold; no stipulated preference disguised as derivation | Existing selection, token preference, horizon contracts; owed composite record predicate and numeric bridge | `g_term_decomposition:101`, scorer entry and certificate assembly; preference producer located by EC-D | E provenance/degeneracy; C authority/derivation/universe/value; all `:model-inputs`, decomposition and selection-law joins; loaded source and model identities |
| Witness generator | Every supported clause yields a concrete module with P/W plus reviewer X failing at the claimed predicate | Existing `*Witness.lean` styles only; no live-record generator assumed | Proposed `checks/proof2_extract.clj`, `checks/proof2_emit.clj`, `checks/proof2_bad_extract.clj` | Immutable extract manifest, source hashes/key paths, output-module hash, command/toolchain, axiom audit and build result |
| Preregistration/standing | R7 fixed before first proof click; R8 records partial standing without theorem claim | No new mathematical assumption; finite L and standing schema | Offline proof coordination; global start order source | Proposed `holes/labs/wm-contract/proof2/L-v1.edn` plus signed `L-v1-review.md`; later immutable standing rows |

**Review for every front:** a non-author Codex reviewer and a non-author Zai GLM reviewer, with seat/model/version recorded. A Claude author may write discovery/specifications; another Claude never supplies independent review. For runtime implementation under Repair Plan Part B use a Codex implementer and a different Codex reviewer, plus Zai. The existing zai-2 B4 lane is inherited as explicitly assigned work, not reassigned here. Zai may author other packets only if the coordinator records authorization under the applicable repair workflow and assigns a different Zai reviewer. Joe's step/theorem sign-off remains required by A20; no packet author awards it.

## 4. Packet contract and gates

Each numbered row below is **one** dispatch, with one artifact boundary and a target of 20–50 minutes of author work. Review is a separate dispatch, not hidden inside that estimate. No promise is made that a research problem finishes in an hour: a packet that discovers a missing lemma returns its exact statement, counterexample, and bounded successor; it cannot report its front complete. No broad “finish conditioning” ticket is authorized.

Abbreviations apply to every row:

- **D** = discovery/specification: read-only source/record enumeration, exact revision citations, bad-case design, reviewed result in `holes/labs/wm-contract/proof2/packets/<ID>.md`. No runtime gates; check document diff and references. Existing reviewed discoveries can satisfy D without rerunning a census.
- **C** = Clojure build: clj-kondo on changed Clojure paths, check-parens, affected namespace test-registry check using `clojure -M -m futon3c.test-registry check <config.edn>` in its configured environment; run one affected namespace on refusal/staleness and register its log/content hashes. New tests must actually run before landing. At least one fixture has a recorded production shape, including absent fields. No suite, no shared-JVM load, no diagnostic actuator. Enumerate executable effects before claiming a harness is inert.
- **L** = Lean build: `lake build` for the named new module and required dependencies, no `sorry`/click axioms, record `#print axioms` audit. Changed witnesses/dependencies require their registered negative wrapper modes, sequentially; use `checks/wm_workspace_gate.clj` and C437 mapping. No full 32-mode run by habit.
- **R** = independent review: Codex non-author plus Zai non-author; reviewer fixes X before seeing positive proof outcomes, checks warrants and adequacy, and signs exact artifact hashes. An implementation row depends on R of its named discovery/spec. **Every row has R before downstream use.** Runtime repairs retain any existing Joe approval requirements.
- **Author S** = Codex, Zai, or Claude specification author with the review separation above. **Author I** = Codex runtime implementer (inherited B4 work excepted). **Author M** = Codex Lean/tool author; Zai possible under recorded workflow authorization. Review always R, not author self-review.
- **N** = no new click. **Y** = needs ordinary proof records, collected only in RUN. Extraction/compilation of those records is offline and spends no additional click. X fixtures never count as positive evidence.

Proposed artifact paths below are plans, not claims those files exist. New Clojure functions should live in existing namespaces where appropriate; the row's certificate key fixes scope even when the reviewed implementation chooses a different filename.

## 5. Ordered dispatch list

Rows are topologically ordered within each front. Different fronts can proceed concurrently after their named prerequisites. This is a dispatch plan, not instructions to launch agents in this planning turn.

### Foundation and theorem repair

| # / ID | Goal; inputs | Mandatory falsifier | Artifact; gates; author; click | Unblocks |
|---|---|---|---|---|
| 1 / SPEC-N | Specify numeric correspondence from W₁–W₆ and existing symbolic-log witnesses | A rounded log presented as exact ℝ equality | `packets/SPEC-N.md`; D/R; S; N | NUM packets, honest W statements |
| 2 / SPEC-F | Specify H4 prefix aggregation from F discovery and A12/A17/W₃ | Two-step total equated to its last summand; G dropped in F-only comparison | `packets/SPEC-F.md` with proposed precise amendments; D/R; S; N | F-SUM, preregistration |
| 3 / SPEC-L | Identify cold-start feasibility from A21/W₆ and existing prefix records | A new policy borrows a different policy's history | `packets/SPEC-L.md`, the start boundary of L derived from the definitions (see §8); D/R; S; N | L-REG; no permission to weaken W₆ |
| 4 / CERT-S | Specify exact certificate joins from theorem P₀–P₆ | Correct number attached to wrong candidate or post-selection source | `packets/CERT-S.md`, versioned schema and consumed-at semantics; D/R; S; N | Every carrier and extractor |
| 5 / NUM-R | Implement exact rational transcription from SPEC-N | Decimal/ratio silently rounded or nonfinite value admitted | `checks/proof2_numbers.clj`; C/R; M; N | Exact B/A/D extracts |
| 6 / NUM-L | Prove the rational decoding relation from NUM-R | Numerator/denominator swapped in otherwise valid extract | `mathlib4/DarkTower/WarMachine/Proof2/NumberEncoding.lean`; L/R; M; N | Rational witness emitters |
| 7 / NUM-T-D | Bound the missing transcendental bridge from SPEC-N | Near-tie reversed inside the allowed numeric error | `packets/NUM-T-D.md`, named log/exp bounds and lemma dependencies; D/R; S; N | NUM-T-L; can reveal further research |
| 8 / NUM-T-L | Prove one required log bound from NUM-T-D | Recorded log outside its certified interval | `mathlib4/.../Proof2/LogRefinement.lean`; L/R; M; N | F numeric relation; if not hour-sized, dispatch lemma leaves first |
| 9 / NUM-S-L | Prove action stability under the reviewed score bounds | Overlapping action-marginal intervals called a unique winner | `mathlib4/.../Proof2/SelectionRefinement.lean`; L/R; M; N | W₃/W₅/W₆ action claims; softmax equality still needs its specified refinement |

NUM-T-L/NUM-S-L are bounded by their reviewed lemma statements, not a mandate to formalize numerical analysis in an hour. Any missing exp/marginal-sum bound is a separate named lemma packet with L/R, before its emitter is dispatched. No external decimal calculation becomes an axiom.

### B4: finish authoring, not another registry reader

| # / ID | Goal; inputs | Mandatory falsifier | Artifact; gates; author; click | Unblocks |
|---|---|---|---|---|
| 10 / FIELD-D | Map remaining B4 producer interfaces after 1639968f, 178a53c9 and zai-2 slice 2b | Proposal join mistaken for executable candidate | `packets/FIELD-D.md`, exact remaining seams and ownership; D/R; S; N | FIELD-I through FIELD-J; avoid concurrent edits of 2b |
| 11 / FIELD-I | Produce substantive task interpretation from FIELD-D and real registry/queue shape | Fresh ids around an unchanged hand cascade | Interpretation producer, normalized PatternSlots/authority hashes; C/R; I; N | FIELD-A |
| 12 / FIELD-A | Bind observable acceptance from interpreted source revision | Locator resolves but cannot establish the stated task acceptance | Acceptance/locator/scope carrier in B4 producer; C/R; I; N | FIELD-C; B3 parcel work remains separately owned |
| 13 / FIELD-C | Construct one executable cascade from the reviewed interpretation | Two differently named first actions with equal reachable effects | Construction producer through existing cascade facilities; C/R; I; N | FIELD-P |
| 14 / FIELD-P | Publish reviewed ordinary constructions through `cascade_sources/check-file!` | Unreviewed source published as admitted | Immutable review-publication/admission chain; C/R; I; N | FIELD-J |
| 15 / FIELD-J | Join published candidates to the actual scorer field | Derivations cover a census while one scored candidate is absent | Complete `:candidate-derivations`, s₀/rows/id bijection; C/R; I or inherited zai-2; N | W₀ emitter and RUN |
| 16 / FIELD-X | Specify exclusions over the full discovered horizon | Repair silently disappears when closure observation is unavailable | `:enumeration-completeness` producer at proposal exclusion seam; C/R after FIELD-D; I; N | P₀ coverage |

FIELD-I is a producer for ordinary work, not a proof-time authored answer. If the interpretation mechanism requires an agent workflow, FIELD-D must split request construction, response validation, and publication integration into separately reviewed C packets before dispatch. “Substantive interpretation” is not satisfied by a deterministic stub. Existing B4 slices may replace rows only with matching reviewed artifacts, not merely commit titles.

### Observation, A, D and Q

| # / ID | Goal; inputs | Mandatory falsifier | Artifact; gates; author; click | Unblocks |
|---|---|---|---|---|
| 17 / OBS-D | Locate eligible truth/observation pairs in B6 close and outcome records | Missing observation counted as measured false | `packets/OBS-D.md`, population/identity/denominator map; D/R; S; N | OBS-P, A-S |
| 18 / OBS-P | Emit an occurrence-bound measurement pair at the identified close seam | Duplicate occurrence or mismatched reviewed revision | Immutable outcome pair carrier; C/R after OBS-D; I; N | Measurable A and Q |
| 19 / A-S | Fix an identifiable estimator from OBS-D | Prior-only or identity-hardcoded estimator passes as measured | `packets/A-S.md`, population/window/minimum counts/smoothing/uncertainty rules; D/R; S; N | A-E, A-L, L-REG |
| 20 / A-E | Compute rates from eligible paired rows | Changing a mathematically relevant source row leaves all rates fixed | Estimator namespace selected by A-S; C/R; I; N | A-C |
| 21 / A-C | Feed measured A through the production manifest | Measured receipt alongside identity-default consumed matrix | `:model-inputs <id> :A` from manifest consumer boundary; C/R; I; N | W₁ and D |
| 22 / A-L | Define the finite estimator relation | Wrong denominator yields same rounded rate but wrong rational | `mathlib4/.../Proof2/MeasuredRates.lean`; L/R; M; N | W₁ emitter |
| 23 / D-D | Resolve versioned observation admission from predecessor modules and OBS-D | Different token meanings carried as the same state | `packets/D-D.md`, exact compatibility and reinitialization semantics; D/R; S; N | D-P, D-C, Q-J |
| 24 / D-P | Persist one admitted conditioning step | Duplicate update or observation joined to wrong policy | Immutable prior/A/B/o/q step carrier; C/R; I; N | D-C and F-P |
| 25 / D-C | Consume admitted posterior at rollout initialization | Logged q while scorer still receives observed-facts point mass | `:model-inputs <id> :D`, input/stage joins; C/R; I; N | W₂ and F |
| 26 / Q-J | Emit the temporal consumption join | Break action→outcome identity with all numbers unchanged | `:Q-link` at actual next-state read; C/R after D-D/D-C; I; N | W₄ and F prefix provenance |

A missing measurement population is a finding that the estimator cannot yet produce measured A. OBS-P makes future measurement possible; it cannot manufacture past labels. Development tests may replay immutable examples offline without claiming ordinary positive evidence.

### F supply and honest absence

| # / ID | Goal; inputs | Mandatory falsifier | Artifact; gates; author; click | Unblocks |
|---|---|---|---|---|
| 27 / F-ABS | Record the actual reduced law when F is absent | `:f nil` silently treated as supplied zero | `cascade_selection` law receipt and certificate typed absence; C/R using reviewed F discovery; I; N | Honest tuning evidence immediately, not W₃ pass |
| 28 / F-P | Assemble an admitted prefix for its exact policy | Reordered, duplicate, synthetic or foreign-policy occurrence | `policy_prefix_evidence` production input plus `:f-prefix`; C/R after D-P and SPEC-F; I; N | F-Q |
| 29 / F-Q | Retain the per-step VFE inputs | F scalar supplied without q or measured A | Per-step A/B/sPrev/o/q and F carrier; C/R; I; N | F-SUM |
| 30 / F-SUM | Attach the prefix total to selection | Prefix sum differs from consumed candidate/policy F | `production-ranked`/`efe` attachment and `:model-inputs <id> :F`; C/R; I; N | W₃ supply |
| 31 / F-L | Define the prefix VFE witness relation | Last-step F substituted for total on a two-step prefix | `mathlib4/.../Proof2/PrefixFreeEnergy.lean`; L/R after SPEC-F; M; N | W₃ emitter |
| 32 / F-CON | Bind the F correspondence in a versioned contract revision | Contract points to bypassed prospective path only | New bundle revision entry `wm-policy-variational-free-energy`; D plus bundle schema validation/R; S; N | Preregistered F contract; never mutate pinned r12 in place |

F-ABS does not veto selection. Nonfinite, missing or incompatible F remains typed evidence and leaves the relevant proof clause open. Do not re-enable the old prospective calculation to make this front look complete.

### B: certify the learner that already runs

| # / ID | Goal; inputs | Mandatory falsifier | Artifact; gates; author; click | Unblocks |
|---|---|---|---|---|
| 33 / B-D | Specify the scalar-to-Dirichlet correspondence from ledger and judge | Whole-attempt success asserted as token posterior without a mapping | `packets/B-D.md`, outcome/state carriers, accepted-trial eligibility, commit point/order; D/R; S; N | B-C, B-N |
| 34 / B-C | Emit the actual update's concentration carrier | Duplicate evidence changes concentrations; reconstructed vectors lack source ids | Close `:b-update` prior/posterior/trials/dedup/version; C/R; I; N | First B arithmetic witness |
| 35 / B-N | Prove concentration-to-theta normalization | Scalar theta agrees by accident with the wrong concentration row | `mathlib4/.../Proof2/BetaNormalization.lean` over existing `DirichletLearning`; L/R; M; N | W₅ arithmetic and kernel bridge |
| 36 / B-R | Record unconditional latest-B reads before selection | Incompatible candidate field skips the read or reads a stale version | Root `:B-read` including total-order/commit-point evidence; C/R after B-D; I; N | Temporal P₅ |
| 37 / B-T | Bind each consumed theta to its read version | Judge stamps a default while certificate logs learned theta | `:model-inputs <id> :B` and exact precedence theta joins at `war_machine:6380`; C/R; I; N | W₅ consumed-kernel proof |
| 38 / B-CON | Bind the B correspondence in a new contract revision | Contract invents a missing learner or omits the judge read | `wm-dirichlet-transition-learning` entry beside F-CON; D plus schema validation/R; S; N | Preregistered B contract |

The core `accumulate` is not a deduplicator. X₅'s duplicate check belongs to the recorded accepted-trial filter and its provenance proposition; feeding duplicates to Lean accumulation should add them twice. The witness must expose this boundary rather than “prove” that `accumulate` ignores duplicates.

### E/C and completeness

| # / ID | Goal; inputs | Mandatory falsifier | Artifact; gates; author; click | Unblocks |
|---|---|---|---|---|
| 39 / EC-D | Locate E/C authority-to-consumer chains across src and scripts | Stipulated 55/35/5/5 relabelled as derived C | `packets/EC-D.md`, source/consumer map and missing producer seams; D/R; S; N | C-P, EC-C |
| 40 / C-P | Derive prospective C from pre-existing task authority | Equal universes receive unexplained different preference values | C producer at EC-D's named seam; C/R; I; N | A9/W₆; retain uncertainty/support semantics |
| 41 / EC-C | Record consumed E/C provenance | Decorative C differs from risk input; uniform E labelled informative | E/C exact values/derivations/hash/consumed-at in certificate; C/R; I; N | W₆ emitter |
| 42 / CERT-C | Check certificate joins without a run veto | Any one term missing or consumed-at hash changed | Proposed offline `checks/proof2_certificate.clj`; C/R after CERT-S; M; N | P₀–P₆ and extract validation |
| 43 / CERT-L | Define finite provenance/completeness predicates | Present status accepted without immutable value reference | `mathlib4/.../Proof2/RecordPredicates.lean`; L/R; M; N | All P witnesses and W₆ |

If EC-D finds missing E production rather than just recording, dispatch its single producer seam as an additional reviewed C packet. Neither C nor E is presumed repaired because it is absent from the other six clause titles.

### Witness generator and evidence collection

| # / ID | Goal; inputs | Mandatory falsifier | Artifact; gates; author; click | Unblocks |
|---|---|---|---|---|
| 44 / GEN-D | Specify extraction/compilation boundaries from P₀–P₆ and CERT-S | Extractor fills absent concentration from a ledger recount | `packets/GEN-D.md`, schema, canonical bytes and trust boundary; D/R; S; N | GEN-E |
| 45 / GEN-E | Extract recorded B values without recomputing them | Multiple EDN forms truncated after first; missing key or hash mismatch | `checks/proof2_extract.clj`, `proof2/extracts/<record-hash>/B.edn`; C/R; M; N | First witness target |
| 46 / GEN-B | Emit the concrete B arithmetic witness | Changed trial component leaves emitted theorem unchanged | `checks/proof2_emit.clj` B backend → `mathlib4/.../Proof2/BWitness_<hash>.lean`; C+L/R after B-N; M; N for tools, Y for live standing | W₅ arithmetic only |
| 47 / GEN-X5 | Emit reviewer-selected B bad extracts | Wrong assertion fails only due to syntax/import error | `checks/proof2_bad_extract.clj` B backend and negative module receipts; C+L/R; M; N | Trustworthy X₅ |
| 48 / L-REG | Preregister the bounded sequence | Add a sixth click or replace the first compatible pair after seeing results | `proof2/L-v1.edn` and `L-v1-review.md`; D/R plus Joe budget/sign-off; S; N | RUN, subject to stop-line |
| 49 / READY | Bind the reviewed runtime baseline for proof collection | Committed source differs from loaded source, or open machine-failure finding ignored | `proof2/readiness-v1.md` with board snapshot and load-identity requirements; D/R; S; N | RUN only when stop-line cleared by its owners |
| 50 / RUN | Collect the preregistered ordinary sequence | Proof-specific candidate/parameter override or omitted failed start | Immutable ordinary records plus global L index; production gates and recorded budget; authorized operator with R; Y | Positive evidence for every supported witness |
| 51 / STAND | Evaluate clause standing over the complete fixed L | Partial W₅ arithmetic labelled full clause 5 or theorem success | `proof2/standing/L-v1.edn` and signed report; existing build receipts/warrants plus R/A20; S; Y records only | Honest clause-level and conjunctive verdict |

RUN is an operational envelope, not an under-hour implementation ticket or a permission to spend five clicks now. Each authorized invocation is separately handled by the operator; long execution is not disguised as an hour of coding. All implementation dispatches remain under the per-packet boundary. If repairs occur during L, preserve failed records and baseline changes; do not reset L or erase the failed universal obligation.

## 6. Generator expansion: one backend at a time

The first target is **clause 5's accumulation/normalization sub-witness**, because B is already computed and consumed; B-C supplies the missing recorded concentrations/trials, and B-N supplies the small absent normalization lemma. This is not a claim that the full clause can pass after one carrier: unconditional read continuity and first-compatible action change still need B-R/B-T and ordinary linked clicks. Old scalar-only records may be negative fixtures; do not add arrays to them retroactively.

The generator is a separate development front, not a script-writing footnote. Following GEN-E/GEN-B/GEN-X5, dispatch the following **three separate packets per row**, in the order shown. Each is 20–50 minutes once its mathematical dependencies exist; each has C/R for extractor/emitter changes and L/R for emitted modules. Author M, independent R; no click to develop, Y records for standing. IDs ending E, W, X are individual dispatch IDs, not one combined task.

| IDs | Inputs / exact extraction scope | W module and prerequisite | Reviewer X artifact / falsifier | Unblocks |
|---|---|---|---|---|
| G0-E, G0-W, G0-X | P₀ candidate maps, normalized slots, s₀, emitted rows, complete derivations | `Proof2/FieldWitness_<hash>.lean`; FIELD-J/X, CERT-L | Clone; hand-admitted chain; wrong row, each separately fails intended predicate | W₀/P₀/X₀ |
| G1-E, G1-W, G1-X | P₁ model A and eligible paired source rows | `Proof2/AWitness_<hash>.lean`; A-L and numeric relation | Identity substitution with source fixed; corrupt row sum | W₁/P₁/X₁ |
| G2-E, G2-W, G2-X | P₂ prior/o/A/B/q and actual token-belief input | `Proof2/DWitness_<hash>.lean`; D-C, exact/refined update | Facts point mass replaces uncertain posterior | W₂/P₂/X₂ |
| G4-E, G4-W, G4-X | P₄ predecessor/action/outcome/next-belief joins and boundary states | `Proof2/QWitness_<hash>.lean`; Q-J, CERT-L | Break one identity edge while preserving numbers | W₄/P₄/X₄ |
| G3-E, G3-W, G3-X | P₃ per-step inputs, total, E/G/gamma/temperature, policy→action mapping | `Proof2/FWitness_<hash>.lean`; F-L, numerical and action refinement | Alter q/F; mismatch logged and consumed F; full field held fixed | W₃/P₃/X₃ |
| G5T-E, G5T-W, G5T-X | P₅ global read sequence, compatibility keys, old/new B and frozen non-B inputs | `Proof2/BTemporalWitness_<hash>.lean`; B-R/T, action refinement | Stale/default read; omitted intervening click; altered non-B input | Remaining W₅/P₅/X₅ |
| G6-E, G6-W, G6-X | P₆ every certificate in L, E/C provenance, all term joins and Q status | `Proof2/CompleteWitness_<hash>.lean`; preceding backends and EC-C | For each required term: missing value; independently changed consumer hash | W₆/P₆/X₆ and conjunction |

Extractor outputs for each backend use `proof2/extracts/<record-hash>/<clause>.edn`; X outputs use a separate `proof2/negative/<spec-hash>/` tree; their namespace/file names cannot be mistaken for live positives. The manifest includes raw-record SHA-256, all referenced content hashes, schema/tool/bundle/load identities, exact key paths and presence statuses, finite carrier ordering, and canonical extract hash. Read every form to EOF when the input is a ledger; reject unexpected extra forms in a single-record file. Extract all candidate ids, not just the winner. Check references rather than trust hash strings supplied inside an unverified object.

The emitter must preserve machine-number encodings, emit actual numerals and finite maps independently from the asserted model formula, and retain source-key links in its manifest. Deriving both sides from the same function is not a correspondence witness. It may compute proof terms; it may not compute missing production facts. Lean sees decoded records, not the filesystem or reality: hashing, immutable origin, and “this was the actual consumer input” remain the explicit P/extractor trust boundary, not facts Lean magically establishes.

For each X, the reviewer first registers the semantic mutation against a valid-shaped fixture with unrelated premises preserved. A malformed module failing to parse is not a successful negative. The negative harness checks the same proposition/checker and records the expected semantic failure; a `#guard_msgs` wrapper may compile green precisely because the wrong assertion fails. Retain the inner diagnostic, wrapper exit, module hash and checker identity. Positive live evaluation follows only after these negatives work. Later live-shaped mutations supplement, not replace, the preregistered X.

Full finite state enumeration may become infeasible on ordinary universes. Do not shrink the field or tokens to fit an emitter. GEN-D must record actual cardinalities; if necessary, prove a sparse-support equivalence lemma as a separate reviewed Lean packet, then use it to cover the full declared carrier. This is an explicit research risk, not permission for a small-model proof substitute.

## 7. L preregistration and use of the five banked clicks

L-REG is authored by the proof coordinator with the independent Codex/Zai reviewers. Joe controls click allocation and final sign-off; reviewers own estimator, schema and mathematical engineering choices. Commit the declaration and sign its content hash before the first proof click. Each subsequent L index references that hash. The declaration contains:

1. Exact ordinary entry point, permitted pre-existing configuration, global start-order authority, starting boundary, baseline/toolchain/contract hashes and loaded-source identity requirements. Include all ordinary starts in scope, including failures; do not index only successful closes. Record any externally authorized interruption.
2. Fixed length or outcome-independent stopping rule. Proposed budget envelope: at most the five banked renewal-7 clicks, with exact allocation approved before starting; no automatic renewal. A machine-failure stop ends collection with open clauses and outside repair, not replacement trials. Five is a budget, not a forecast of proof success.
3. A predeclared history boundary and eligibility rule for any initial A/B/D/F inputs. Historical inputs can be consumed by a live click, but an unlinked historical run cannot replace a required member of L. No unreported warm-up clicks. Resolve SPEC-L before signing the declaration.
4. Compatible-pair rule: consecutive global ordinary starts with the producing update committed before the successor's pre-selection snapshot, matching semantic policy/pattern/token/model compatibility keys. Name treatment of concurrency and commit races. Every intervening click must read latest B unconditionally even if its candidate field is incompatible. Test the **first** qualifying consecutive pair, not a later favorable one. No incompatible run is deferred or vetoed.
5. Passing bounds: W₀'s at least two eligible semantically/effect-distinct candidates; A's fixed population and adequacy bounds from A-S; W₂ at least one nondegenerate posterior mathematically implied by inputs; W₃ at least one strict action change from F-only ablation with the tie/action-marginal rule fixed; W₄ at least one complete adjacent conditioning link; W₅ action difference on the first compatible pair; W₆ complete required inputs on every member. Fix numeric error/decision-margin rules from SPEC-N, without choosing them after observing differences.
6. Frozen comparison inputs: field, eligibility/ticket stratum, E/C/A/D/G where applicable, gamma/temperature, action mapping, code identity and tie rule. B comparisons use actual recorded old/new B; recompute only B-dependent descendants, keeping exogenous non-B inputs fixed. Explicitly distinguish descendants from independently fixed inputs so “freeze G” does not accidentally erase B's causal route through rollout/G. F ablation preserves G. Capture input hashes and both resulting action witnesses.
7. Outcome vocabulary: holds/open/insensitive/no-compatible-pair/inadmissible-evidence/collection-interrupted, with no theorem-bearing interpretation for partial results. Do not extend L until an action changes. A changed theorem requires a new preregistration and separate evidence allocation, never a retroactive pass on L-v1.

The current board count of 35 at the 02:57 sweep is a supplied snapshot, not a readiness certificate. Other lanes clear machine-failure findings; READY reads the current authoritative disposition. This plan neither clears them nor reloads the JVM. All N packets can proceed from outside the machine now after their local dependencies/reviews; Y work waits for stop-line clearance and the authorized allocation.

## 8. Risks and refusals

**Engineering resolutions owed to reviewers, not bookkeeping for Joe:** numeric refinement instead of impossible exact float equalities; explicit prefix sum; the F-ablation wording; observation identifiability; Beta specialization and trial semantics; sparse support; global read ordering. These are concrete specification choices with counterexamples. They must amend the theorem/assumptions under A20 before signatures, not queue a vague “please decide architecture” request.

**The cold-start question is settled by the definitions, not by a ruling
(claude-8, 2026-09-24, on Joe's objection that a proof strategy is not an
argument from authority).** Clause 6 quantifies over every member of L. A
new policy on its first click has no admitted prefix, so F is absent for
it; by A16 that is a typed absence and W₆ is open on that click. That is
the whole answer: the click is in L, the clause is open, the record says
why. There is no "initialization phase" to declare, because L's start
boundary is already fixed by A19 and A21 (ordinary clicks after the signed
baseline, all of them, failures included), and any click before that
boundary is simply not in L and cannot be selected into it later. What
SPEC-L still owes is factual, not permissive: whether compatible immutable
histories exist before the boundary that a live click may lawfully consume
as inputs (A21's "unlinked historical run" rule), stated with their
identities before L is signed. If none exist, the first clicks of L will
have W₆ open for cold-start candidates and the theorem will not close on
L-v1; that is a true statement about the machine, to be recorded under R8,
and the remedy is more ordinary clicks in a later preregistered L-v2, never
a fabricated prefix, a borrowed history, or a suppressed candidate.

I would refuse the following **proof credits or packet completion claims**, not ordinary tuning runs:

- A hand-admitted candidate relabelled by a derivation map, or successful publication without actual selection consumption.
- A new learner ticket for B when the missing work is its carrier; conversely, calling arithmetic equivalence proof of accepted-trial eligibility or transition semantics.
- An A estimator with no identifiable truth/observation population, a stipulated C, or measured defaults merely copied into receipts.
- F absent yet described as consumed zero; synthetic history; a post-selection F diagnostic described as an input; or a history borrowed from an unrelated policy.
- Q discharged by a flag, or clause 6 satisfied by after-the-fact reconstruction. Truthful absence is progress in instrumentation and failure of the positive clause.
- An existential “done” promised by a code packet. Engineering can make the comparison observable; it cannot guarantee nature supplies a winner reversal within fixed L.
- Source/certificate hashes treated as proof of execution without consumer joins and loaded identity; a witness proving `x = x`; invented axioms; a bad case whose only failure is broken syntax.
- A suite run replacing review, unrun tests committed, simultaneous memory-heavy Lean negatives, or any gate whose status is hidden by a pipe.
- Independent sign-off from Claude-on-Claude, author self-review, or a material revision retaining old signatures.

R8 is the pressure release: publish “W₅ arithmetic holds on these records; full clause 5 open; theorem open” when that is all the evidence supports. Full clause standing names W/P/X and L. Only all seven clauses on the same preregistered sequence support the conjunctive theorem. The practical next dispatches are CERT-S, SPEC-N/F/L, FIELD-D coordinated with zai-2, OBS-D, B-D and GEN-D; F-ABS can use the already completed F discovery once independently reviewed. None spends a click.

## Coordinator note (claude-8, 2026-09-24, on accepting this draft)

Accepted as the working strategy. Two facts supersede the author-role rule
in §3/§4: Joe assigned REPAIR-PLAN B4 to a Zai seat (zai-2 has landed
slices 1, 2a, 2b and 2c — futon2 1639968f, 178a53c9, d98fe4c7, 8f0a2036;
2c landed after this draft was written and closes the declared-source sha
and acceptance plumbing that FIELD-J would otherwise re-plan), and Joe
authorized Kimi seats for this lane's dispatches. Codex remains the
independent reviewer of record with Zai GLM per A20. The §1 item 1
correction (floating consumed values cannot satisfy exact real equalities;
a numeric bridge is a front, not a tolerance asserted by the emitter) is
accepted against the THEOREM draft's "checkable by norm_num" wording.
First dispatches: CERT-S and F-ABS, in parallel, no click.
