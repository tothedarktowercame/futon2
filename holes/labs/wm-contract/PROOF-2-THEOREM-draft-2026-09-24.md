# PROOF-2 — theorem in live-record witness form

Date: 2026-09-24

Status: draft; no click, code, data, bundle, or Lean file was changed for this packet.

## Verification of decisions D1–D4

D1–D3 agree with the named Lean files. D4 is correct that neither `PolicyVariationalFreeEnergy` nor `DirichletLearning` has an r12 contract, and correct that learned B has no runtime consumer: `learning_trial_ledger.clj:238` defines `pattern-theta`, but no production source calls it. D4's unqualified statement that “the runtime has no consumer” is false for F. `cascade_free_energy.clj:105` computes the exact-posterior equality value `−log P(o|π)`; `efe.clj:1126–1204` attaches a finite result; `cascade_selection.clj:51–125` subtracts it. The defect is sharper: the runtime/certificate does not retain `q`, so it cannot yet witness the full `variationalFreeEnergy lik prior q` definition, and the producer/consumer is not bound by a contract.

The accepted ASSUME file is amended in this commit at A12–A14 to use D1's F and D2's Dirichlet B/unconditional next-click rule.

## Theorem

Let `L = (c₀, …, cₙ)` be the finite, globally ordered sequence of ordinary live clicks declared under A21. Let `extract(c)` mean only exact consumed values and immutable provenance read at the key paths named below; it is not a recomputation. Let `lean(extract(c))` be a generated concrete Lean witness module containing numerals/finite carriers transcribed from that extract. No theorem below may contain `sorry`, an axiom introduced for the click, or a proposition of the form `x = x`.

**THEOREM (PROOF-2).** The implementation is not fake with respect to the named War Machine Lean model iff, for every click required by clauses 0–6, the corresponding provenance proposition `P_k` identifies the values actually consumed, the concrete Lean proposition `W_k` checks on those values, and the independently constructed bad extract `X_k` makes the same proposition fail. All seven clauses hold on the single linked sequence `L`; fixtures and hand-built inputs establish none of them.

### Clause 0 — an ordinary, semantically nontrivial field exists

**W₀.** For the finite candidate extract `Π`, `2 ≤ Π.length`; every candidate's slots satisfy `CascadeTransition.interpret slots = some precedence`; every recorded transition row equals `CascadeTransition.cascadeKernel precedence`; candidates are pairwise distinct under the fixed equivalence in the Step 0 CHECK below; and at least two candidates have different first enabled `InterpretedPattern`s at the recorded initial state, with different `(consumes, forbids, produces)` or a different resulting `patternKernel`. Used declarations: `CascadeTransition.InterpretedPattern`, `interpret`, `guard`, `firstEnabled`, `patternKernel`, and `cascadeKernel` in `CascadeTransition.lean`. The equalities are pointwise over the finite token-state carrier and are decided/proved on concrete values.

**P₀.** `Π` is read from `[:decision :selection-law :per-policy-argmax :action]` together with the complete field at `[:decision :selection-certificate :candidates]`, `[:decision :selection-certificate :policies]`, and their candidate ids into `[:decision :selection-certificate :scoring]`; source enumeration comes from `[:decision :enumeration-completeness]` and candidate derivations must be added at `[:decision :selection-certificate :candidate-derivations]`. The exemplar lacks that last field and its existing actions say `[:construction-receipt :kind] :hand-admitted`, so it cannot instantiate P₀.

**X₀.** Two negative extracts are mandatory: (a) clone one cascade under fresh ids while preserving its normalized slots; pairwise distinctness must be false; (b) retain semantically different cascades but set construction/interpretation provenance to `:hand-admitted`; P₀'s admissibility proposition must be false before W₀ is credited. Full CHECK below.

### Clause 1 — A is measured and is the consumed observation kernel

**W₁.** On each candidate universe, the concrete rate extract constructs `TokenObservation.AdjudicationRates`; for every state `s` and outcome `o`, the consumed entry equals `TokenObservation.tokenLikelihood rates s o`; every row is nonnegative and sums to one as in `observationKernelOK`; and the scorer's predicted outcome equals `PolicyRollout.predictedOutcome`. The measured-rate side proposition states that every rate equals the preregistered estimator applied to the extracted accepted outcome rows and is not supplied by the identity default. Used declarations: `TokenObservation.tokenLikelihood`, `observationKernelOK` in `TokenObservation.lean`, and `PolicyRollout.predictedOutcome` in `PolicyRollout.lean`.

**P₁.** Read per-policy A from the to-be-built `[:decision :selection-certificate :model-inputs <candidate-id> :A]`, including `:rates`, `:estimator`, `:population`, `:window`, `:counts`, `:source-row-identities`, `:value-sha256`, and `:consumed-at`; cross-check the term at `[:decision :selection-certificate :g-term-decomposition :policies <i> :terms :A]`. The exemplar only records that term as missing/identity-default and has no complete consumed A; this must be built.

**X₁.** Replace one measured rate by the zero identity default while leaving its source rows and estimator fixed, and separately corrupt one likelihood row sum. The estimator equality and kernel proposition respectively must fail.

### Clause 2 — D is the exact posterior, not copied facts

**W₂.** For the ordinary click chosen under amended step 2, the extracted `q` satisfies `ExactBeliefTrajectory.exactUpdate A B o sPrev = some q`, equivalently the pointwise equation `q x = A x o * predictedState B sPrev x / observationProbability A B o sPrev`; `q` is normalized; and `q ≠ TokenState.observedBelief sFacts` because this click's measured non-identity A and evidence imply nonzero uncertainty. On all other clicks the witness records whether degeneracy follows mathematically. Used declarations: `predictedState`, `observationProbability`, `exactUpdate`, `exactUpdate_dist`, `tokenBeliefAt` in `ExactBeliefTrajectory.lean`, and `TokenState.observedBelief` in `TokenState.lean`.

**P₂.** Read `sPrev`, `o`, A/B identities and `q` from the to-be-built `[:decision :selection-certificate :model-inputs <candidate-id> :D]`; link it to `[:decision :selection-certificate :token-belief-input]` and `[:decision :selection-certificate :token-belief-stage]`, and cross-check `[:decision :selection-certificate :g-term-decomposition :policies <i> :terms :D]`. The exemplar's `:token-belief-stage :initialization :value` is a point mass and its conditioning status is awaiting admission, so it fails rather than witnesses W₂.

**X₂.** Substitute the facts point mass for `q` while holding A, B, `o`, and `sPrev` fixed on the nondegenerate click. `exactUpdate = some q` or the required inequality must fail.

### Clause 3 — selection consumes Lean F and F changes an action

**W₃.** For each policy `π`, with extracted `lik x = A x o`, `prior = ExactBeliefTrajectory.predictedState B sPrev`, and posterior `q`, the consumed scalar equals `PolicyVariationalFreeEnergy.variationalFreeEnergy lik prior q`. Where `q` is the exact update, `ExactBeliefTrajectory.exactUpdate_minimises_vfe` proves it equals `−Real.log (observationProbability A B o sPrev)`. The extracted full posterior additionally equals `PolicySelection.selectionPosterior temperature E F G`, and on the designated live field `argmax(selectionPosterior E F G) ≠ argmax(selectionPosterior E 0 G)`, with action marginalization by `ActionMarginal.IsBayesAction`. Used files/declarations: `PolicyVariationalFreeEnergy.lean` (`variationalFreeEnergy`), `ExactBeliefTrajectory.lean` (`exactUpdate_minimises_vfe`), `PolicySelection.lean` (`selectionPosterior`), and `ActionMarginal.lean` (`IsBayesAction`).

**P₃.** Read consumed F from `[:decision :selection-certificate :candidates <i> :f]`, `[:decision :selection-certificate :policies <i> :f]`, and `[:decision :selection-certificate :g-term-decomposition :policies <i> :terms :F]`; read E/G/beta and posterior from the same certificate and `[:decision :selection-law]`. The inputs `A`, `B`, `sPrev`, `o`, and `q` must be stored at `[:decision :selection-certificate :model-inputs <candidate-id> :F]`. The exemplar has F missing/not consumed and no q; current `cascade-free-energy` records evidence inputs but not q, so the witness carrier must be built.

**X₃.** Change one stored q mass or consumed F while preserving the generative inputs; the VFE equality must fail. Separately feed a certificate whose logged F differs from selection's F; P₃ must fail even if both numbers independently satisfy some Lean instance.

### Clause 4 — Q is an end-to-end consumed conditioning chain

**W₄.** For each adjacent link in the designated closed-loop subsequence, the selected action's cascade kernel gives the predicted state; `PolicyRollout.predictedOutcome` gives Q over outcomes; the realized observation enters `ExactBeliefTrajectory.exactUpdate`; and the resulting posterior is the next stored/consumed belief. Pointwise equalities are checked for every finite state/outcome, and the sequence is the corresponding prefix of `ExactBeliefTrajectory.exactBeliefAt`/`tokenBeliefAt`. Used declarations: `CascadeTransition.cascadeKernel`, `PolicyRollout.rolloutState` and `predictedOutcome`, and `ExactBeliefTrajectory.exactUpdate`, `exactBeliefAt`, `tokenBeliefAt`.

**P₄.** Join click/attempt/action/outcome identities from `[:decision :selection-law]`, the immutable execution/close record, and the next click's `[:decision :selection-certificate :token-belief-input]`; store the join at `[:decision :selection-certificate :Q-link]` with predecessor click, action hash, outcome-row identity, A/B versions, prior/posterior hashes, and `:consumed-at`. Cross-check the Q term in `:g-term-decomposition`. The exemplar says observation initialization is refused/pending and has no Q link, so it fails.

**X₄.** Break exactly one identity edge (action→outcome or outcome→next posterior) while leaving all numeric rows unchanged. W₄'s chained equality/provenance proposition must fail; a `:closed-loop true` flag cannot rescue it.

### Clause 5 — Dirichlet B is accumulated once and read before every next selection

**W₅.** Each accepted deduplicated trial extracts an outcome vector `oτ` and posterior state belief `sτ`; the prior and posterior concentration arrays inhabit `DirichletLearning.DirichletParams`, satisfy `posterior = DirichletLearning.accumulate prior trial nonneg`, and pointwise satisfy `accumulate_conc`. The extracted theta equals a separately recorded normalization of the relevant posterior concentration row, and substituting it in `CascadeTransition.InterpretedPattern.theta` reproduces the consumed `cascadeKernel`. Every next click's pre-selection B version equals the latest accumulated version. For the first consecutive compatible pair, selection under old and new B has different Bayes actions with all non-B inputs equal. Used declarations: `DirichletParams`, `step`, `accumulate`, `accumulate_conc`, `accumulate_append` in `DirichletLearning.lean`; `patternKernel`/`cascadeKernel`; `selectionPosterior` and `IsBayesAction`.

**P₅.** The producing close must record `[:b-update]` with prior/posterior concentration arrays, trial vectors, dedup identity, version/hash, and normalization. Every later run record must carry the unconditional read at `[:decision :selection-certificate :model-inputs <candidate-id> :B]` and the exact theta at the candidate precedence plus `:g-term-decomposition ... :terms :B`. The global order and every intervening click are listed in `[:decision :selection-certificate :B-read :predecessor-chain]`. Existing scalar `learning_trial_ledger/b-update` and `pattern-theta` compute a Beta-smoothed success ratio, not a recorded Dirichlet `(outcome,state)` array; moreover `pattern-theta` has no production caller. This must be built, not declared equivalent.

**X₅.** Duplicate one trial identity (posterior must not change), alter one state-belief component (the pointwise accumulated cell must change), and feed a next-click certificate with the old/default B version (P₅ must fail). A scalar theta with no concentration array and normalization must fail W₅ even if its numeric value is `3/4`.

### Clause 6 — every required value is certified on every click

**W₆.** For each `c ∈ L`, every per-click input E, C, A, D, F, and B has a concrete typed Lean carrier and all equalities required by W₁–W₅; the selection posterior equals `PolicySelection.selectionPosterior` on exactly those values. Each click has a Q-link/status, and the declared subsequence satisfies W₄. No extracted status is `:consumed-value-not-recorded`, `:not-consumed`, `:missing`, or record-only.

**P₆.** The root is `[:decision :selection-certificate]`; values are joined through `:candidates`, `:policies`, `:scoring`, `:g-term-decomposition :policies`, `:model-inputs`, `:Q-link`, and `:B-read`, with content hashes and `:consumed-at` loci. The exemplar demonstrates the intended root but fails W₆ because A/D/Q are missing and F is not consumed/fully witnessed.

**X₆.** Replace any single term's value by `nil` plus `:consumed-value-not-recorded`, or leave a correct logged value but change its `:consumed-at` hash. W₆/P₆ must fail and no later clause may cite that click.

## Two proposed r12-schema contract entries

These are proposed JSON objects in the existing bundle schema; they are not edits to the bundle.

### `wm-policy-variational-free-energy`

- **contract-id:** `wm-policy-variational-free-energy`
- **declarations:**
  - **clojure-locus:** `futon2/src/futon2/aif/cascade_free_energy.clj:105` (producer), `futon2/src/futon2/aif/efe.clj:1126` (attachment), `futon2/src/futon2/aif/cascade_selection.clj:51` (consumer)
  - **decided:** `2026-09-24`
  - **evidence:** `mathlib4/DarkTower/WarMachine/PolicyVariationalFreeEnergy.lean:35`; equality-case composition at `mathlib4/DarkTower/WarMachine/ExactBeliefTrajectory.lean:103`
  - **falsifier:** A candidate's consumed F differs from `variationalFreeEnergy (fun x => A x o) (predictedState B sPrev) q`; or an exact-update q does not give `−log (observationProbability A B o sPrev)`; or the certificate omits any of A, B, `sPrev`, o, q, F, and the consumed-at identity.

### `wm-dirichlet-transition-learning`

- **contract-id:** `wm-dirichlet-transition-learning`
- **declarations:**
  - **clojure-locus:** `ABSENT — required consumer: before every ordinary selection, read the latest persisted Dirichlet concentration version, normalize the relevant outcome row to pattern theta, attach that exact theta to the candidate's InterpretedPattern, and record concentration/version/normalization under the selection certificate; the existing scalar producer is futon2/src/futon2/aif/learning_trial_ledger.clj:154 and the currently uncalled scalar reader is :238.`
  - **decided:** `2026-09-24`
  - **evidence:** `mathlib4/DarkTower/WarMachine/DirichletLearning.lean:20` (`DirichletParams`), `:46` (`step`), `:58` (`accumulate`), `:77` (`accumulate_conc`)
  - **falsifier:** For any accepted deduplicated trial, a posterior concentration cell differs from its prior plus the sum of outcome×state-belief outer products; a duplicate changes the posterior; the next ordinary click reads any version other than the latest before selection; or its consumed theta differs from the recorded normalization of that posterior row.

## Step 0 CHECK — candidate-field existence and semantic difference

### Predeclared equivalence relation

For a recorded state `s₀`, normalize a candidate cascade to the tuple:

`N(π) = (target-authority-hash, ordered [(consumes, forbids, produces, theta, authority-hash)] after CascadeTransition.interpret, observation-locator hashes, acceptance-predicate hash, feasible-scope hash)`.

Sets are sorted by the canonical printed token representation; maps are sorted by key; ids, timestamps, author names, prose readings, storage paths, and presentation order are discarded except where a path is itself an acceptance locator. Two candidates are equivalent iff their `N` tuples are equal. They are **effect-equivalent at s₀** iff `firstEnabled` is none for both or the two resulting `cascadeKernel` rows are pointwise equal for every next token state. “Distinct with differing declared effects” requires both `N(π₁) ≠ N(π₂)` and non-effect-equivalence at `s₀`; differing theta alone counts only when it changes a kernel row. This relation is fixed before any proof click.

### W₀

The witness module transcribes the token carrier, `s₀`, pattern slots, bounded theta values, and candidate list. It proves by `decide`/`norm_num`/finite extensionality:

1. at least two admitted candidates;
2. `interpret slots = some precedence` for every candidate;
3. no two candidates are equivalent under `N`;
4. at least one pair has unequal first actions and unequal `cascadeKernel` rows at `s₀`;
5. each emitted runtime transition row equals the corresponding Lean kernel row.

The real-task/provenance predicates are finite data equalities over content hashes and enums in the extract; Lean does not presently define “registry task” or “hand-admitted,” so the witness module must introduce only the record-carrier predicate (not assume its truth) and prove it by reduction on the extracted values.

### P₀

Required paths in the run record:

- field and scores: `[:decision :selection-certificate :candidates]`, `:policies`, `:scoring`;
- selected action: `[:decision :selection-law :per-policy-argmax :action]` and the action-marginal choice;
- discovery completeness/exclusions: `[:decision :enumeration-completeness]`;
- to be built, for every candidate: `[:decision :selection-certificate :candidate-derivations <id>]`, containing `:source-kind`, `:source-id`, `:source-revision`, `:source-content-sha256`, `:discovered-at`, `:interpretation`, `:construction`, `:review-publication`, `:admission`, `:acceptance`, `:locators`, `:scope`, `:normalized-cascade-sha256`, and the emitted transition rows;
- initial state and model values: `[:decision :selection-certificate :token-belief-stage :initialization :value]` and `[:decision :selection-certificate :model-inputs <id>]`.

P₀ asserts those were the candidates supplied to the scorer, not a parallel census. Candidate ids must join bijectively across derivations, candidates, policies, scoring, and action mapping.

### X₀

The independent reviewer constructs both bad extracts before the positive click is evaluated:

1. **Clone case:** copy one candidate exactly, change ids and prose only, and include both. W₀(3) must reduce to false because `N` discards those fields.
2. **Hand-admitted case:** use genuinely different kernels but set either `:interpretation :kind` or `:construction :kind` to `:hand-admitted`, or omit the discovery→publication chain. The provenance predicate in P₀ must reduce to false. Kernel difference cannot compensate.

The reviewer also changes one emitted transition probability while leaving the normalized cascade fixed; W₀(5) must fail. A CHECK implementation that accepts any of these three cases is rejected before a live click.

### Must be built by REPAIR-PLAN B4 before W₀ can be attempted

1. Ordinary target discovery from registry and front-ordered repair queue, with a complete exclusion receipt.
2. A non-human, non-proof-specific interpretation producer that emits PatternSlots and authority hashes from the real task.
3. Observable acceptance and truthful locators bound to source revisions.
4. Construction of executable cascades, with feasible single- or multi-repository scope.
5. Independent review/publication through `cascade_sources/check-file!` and immutable publication identity.
6. Admission and consumption by the immediately following ordinary selection without a supplied candidate list.
7. At least two simultaneously eligible candidates with meaningfully different first enabled effects.
8. The `:candidate-derivations` certificate map and bijective ids described in P₀.
9. Runtime emission of normalized pattern slots, `s₀`, and transition rows sufficient to generate W₀ without rereading source.

B4's output shape is one `:candidate-derivations` map keyed by the exact candidate id used in `:candidates`; each value contains all fields listed in P₀, plus `:status :admitted`, and contains no `:hand-admitted`, proof-fixture, or reference-field provenance anywhere in its transitive source chain.

## Lean/runtime gap inventory

| Lean definition | Bound r12 contract | Runtime locus | Step |
|---|---|---|---|
| `CascadeTransition.interpret`, `firstEnabled`, `cascadeKernel` | `wm-cascade-transition` | `cascade_model_manifest.clj:242–376`; ordinary candidate authoring/provenance **ABSENT** | 0 |
| real task, source derivation, semantic candidate equivalence | **NONE; no Lean domain definition** | registry readers exist; B4 authoring/derivation certificate **ABSENT** | 0 |
| `TokenObservation.tokenLikelihood`, `observationKernelOK` | `wm-token-observation` | `cascade_model_manifest.clj:174,200`; measured-rate estimator/certificate **ABSENT** | 1 |
| `PolicyRollout.predictedOutcome` | `wm-policy-rollout` | `cascade_model_manifest.clj:217` | 1, 4 |
| `ExactBeliefTrajectory.exactUpdate`, `tokenBeliefAt` | `wm-exact-belief` | `cascade_model_manifest.clj:620,648`; live conditioning input is currently pending/refused | 2, 4 |
| `TokenState.observedBelief`, `independentBelief` | `wm-token-state` | `cascade_model_manifest.clj:114,124` | 2 |
| `PolicyVariationalFreeEnergy.variationalFreeEnergy` | **NONE; proposed above** | evidence-value producer `cascade_free_energy.clj:105`; full q-bearing witness extract **ABSENT** | 3 |
| `PolicySelection.selectionPosterior` | `wm-policy-selection` | `cascade_selection.clj:51` | 3, 5, 6 |
| `ActionMarginal.IsBayesAction` | `wm-action-marginal` | `cascade_selection.clj:104` | 3, 5 |
| `ExactBeliefTrajectory.exactBeliefAt` closed sequence | `wm-exact-belief` locally; no composition contract | Q-link/consumed closed-loop chain **ABSENT** | 4 |
| `DirichletLearning.DirichletParams`, `step`, `accumulate` | **NONE; proposed above** | scalar Beta update `learning_trial_ledger.clj:154` is not the definition; Dirichlet producer **ABSENT** | 5 |
| concentration→pattern-theta normalization | **NONE; not defined in Lean** | scalar `pattern-theta` at `learning_trial_ledger.clj:238`, with no production caller | 5 |
| unconditional latest-B pre-selection read/version | **NONE** | **ABSENT** | 5, 6 |
| certificate-wide exact consumed-value proposition | local contracts only; no composite contract | partial `g_term_decomposition.clj:101`; `:model-inputs`, Q-link, B-read **ABSENT** | 6 |

## Adversarial conclusion

Witness form exposes that “the value numerically agrees” is not enough. Step 0 lacks an ordinary authoring/provenance carrier; A lacks a measured estimator; D/Q lack live conditioning; F has a live evidence-value path but not the q-bearing extract required by its Lean definition; and B is presently a scalar Beta recount with an uncalled reader, not the Dirichlet outer-product accumulation D2 names. These are must-build obligations. None can be converted into an assumption or discharged by the existing exemplar, which remains useful precisely because it fails the proposed witnesses in visible ways.
