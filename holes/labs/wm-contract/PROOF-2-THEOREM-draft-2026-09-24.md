# PROOF-2 — theorem in live-record witness form

Date: 2026-09-24

Status: draft; no click, code, data, bundle, or Lean file was changed for this packet.

## Verification of decisions D1–D4

D1–D3 agree with the named Lean files. D4 is correct that neither `PolicyVariationalFreeEnergy` nor `DirichletLearning` has an r12 contract. D4 was WRONG that learned B has no runtime consumer: `learning_trial_ledger.clj:238` defines `pattern-theta` and the selection judge at `scripts/futon2/report/war_machine.clj:6380` calls it before scoring (corrected 2026-09-24; an earlier version of this sentence said no production source calls it, and survived two rounds of correction because each round fixed an enumerated list). D4's unqualified statement that “the runtime has no consumer” is also false for F, but in a way that misleads either direction — see the F sentence below. `cascade_free_energy.clj:105` computes the exact-posterior equality value `−log P(o|π)`; `efe.clj:1126–1204` attaches a finite result; `cascade_selection.clj:51–125` subtracts it. The defect is sharper: the runtime/certificate does not retain `q`, so it cannot yet witness the full `variationalFreeEnergy lik prior q` definition, and the producer/consumer is not bound by a contract.

**The F sentence (claude-5, 2026-09-24, verified by claude-8 at the branch).**
`cascade_selection.clj:112` reads
`(if (= :not-supplied (:f-status c)) 0.0 (- (double (:f c))))`. Every
recorded certificate carries `:f-status :not-supplied`, so the consumer has
run on every click and subtracted zero every time. "F has no consumer" and
"F is consumed" both mislead a builder: the first implies wiring that exists,
the second implies a term that participated. What is owed for clause 3 is a
producer (the admitted prefix, per the F discovery note) AND a change at
this branch so that an absent F is recorded as absent on the certificate
and in the selection law's own record — the law that ran was
`σ(log E − γG)` and the record must say so — rather than silently read as
zero free energy. Under Joe's 2026-09-19 ruling (nothing halts runs during
tuning; evidence yes, vetoes no) this is a typed absence on the record, not
a refusal to select. It is the day's recurring shape: a fallback that makes
absence look like a value (futon1b's rescue reported success while
stringifying props; this branch reports a posterior while a missing term
reads as no contribution), with nothing in the record saying which happened
until someone read the branch.

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

**P₅.** The producing close must record `[:b-update]` with prior/posterior concentration arrays, trial vectors, dedup identity, version/hash, and normalization. Every later run record must carry the unconditional read at `[:decision :selection-certificate :model-inputs <candidate-id> :B]` and the exact theta at the candidate precedence plus `:g-term-decomposition ... :terms :B`. The global order and every intervening click are listed in `[:decision :selection-certificate :B-read :predecessor-chain]`. Existing scalar `learning_trial_ledger/b-update` and `pattern-theta` compute a Beta-smoothed success ratio, which is the two-cell Jeffreys special case of the Dirichlet accumulation (see the Correction below); `pattern-theta` IS consumed, by the selection judge at `scripts/futon2/report/war_machine.clj:6380`. What must be built is the certificate carrier (concentrations, trial identities, version, normalization, the pre-selection read record), not a new learner or consumer.

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
  - **clojure-locus:** `futon2/src/futon2/aif/learning_trial_ledger.clj:154` (`b-update`, the accumulation, scalar two-cell form), `:238` (`pattern-theta`, the normalization), and `scripts/futon2/report/war_machine.clj:6380` (the selection judge's read, which stamps theta onto each precedence pattern before scoring). Required addition: record concentration/version/normalization and the pre-selection read under the selection certificate.`
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
| `DirichletLearning.DirichletParams`, `step`, `accumulate` | **NONE; proposed above** | scalar Beta update `learning_trial_ledger.clj:154` = two-cell Jeffreys special case of `accumulate`; certificate carrier (concentrations, version, normalization) **ABSENT** | 5 |
| concentration→pattern-theta normalization | **NONE; not defined in Lean** | scalar `pattern-theta` at `learning_trial_ledger.clj:238`, consumed by the selection judge `war_machine.clj:6380` | 5 |
| unconditional latest-B pre-selection read/version | **NONE** | **ABSENT** | 5, 6 |
| certificate-wide exact consumed-value proposition | local contracts only; no composite contract | partial `g_term_decomposition.clj:101`; `:model-inputs`, Q-link, B-read **ABSENT** | 6 |

## Adversarial conclusion

Witness form exposes that “the value numerically agrees” is not enough. Step 0 lacks an ordinary authoring/provenance carrier; A lacks a measured estimator; D/Q lack live conditioning; F has a live evidence-value path but not the q-bearing extract required by its Lean definition; and B is a scalar Beta recount that the selection judge does consume — a special case of the Dirichlet accumulation D2 names, lacking only its certificate carrier. These are must-build obligations. None can be converted into an assumption or discharged by the existing exemplar, which remains useful precisely because it fails the proposed witnesses in visible ways.

## Correction (claude-8, 2026-09-24): B does have a production consumer

The gap inventory's row "`DirichletLearning` … scalar Beta update … Dirichlet
producer ABSENT" and the D4 verification sentence "learned B has no runtime
consumer" are wrong on the consumer half, and I relayed them before checking.
The consumer is the selection judge, `scripts/futon2/report/war_machine.clj:6380-6393`
(`generate-war-machine`), which calls `learning-ledger/pattern-theta` per
pattern family and stamps `:theta` and `:theta-source :recorded-trials` onto
each precedence pattern before scoring; `with-pattern-theta` passes a present
`:theta` through and only defaults when it is absent. The miss was a grep
scoped to `src/`: the judge lives under `scripts/`. (An earlier version of
this correction cited `full_loop_runner.clj:34`; that namespace requires the
ledger for the write side, `record!` and `b-update`, not the read.) Live evidence: run
`2026-09-23-1790199409` carries `:theta-source :recorded-trials` on 85
pattern occurrences, including the selected action's precedence (θ = 3/4 on
`:apparatus/done-is-observed-running`, 1/4 on
`:apparatus/evidence-to-disposition-once`, 1/16 on
`:contracts/holder-states-the-claim`), and claude-5's ⟨1⟩8 table in
`PROOF-wm-works-2026-09-22.md` (futon2 7c67574a) shows the decision θ
advancing one trial per click from 1/8 to 1/16.

What stands from the inventory: the update rule is the scalar
`(successes + 1/2)/(trials + 1)`. That is the posterior mean of
`DirichletLearning.accumulate` with a two-cell outcome carrier
{achieved, not} per pattern and Jeffreys concentrations (1/2, 1/2), followed
by normalization — a special case of the Lean definition, not a different
rule. Clause 5's must-build therefore shrinks to the certificate carrier
(prior/posterior concentrations, trial identities, version hash, the
normalization, and the unconditional pre-selection read record) and the
witness that the recorded theta equals the normalized accumulated cell. The
bad cases in X₅ are unchanged. The proposed
`wm-dirichlet-transition-learning` contract should bind its clojure-locus to
`war_machine.clj:6380` (read) and `learning_trial_ledger.clj:154/238`
(write and rule), not `ABSENT`.

## Review amendments (claude-5 as reviewer, 2026-09-24; accepted by claude-8)

claude-5 confirmed the F finding independently on all eight attempts across
machinery-73 to 76, marked ⟨1⟩3 and ⟨1⟩5 of PROOF-wm-works not established
as written, and downgraded ⟨1⟩8 from discharged to evidence. Three review
objections, all accepted:

1. The stale "no production caller" text in P₅ and the gap table is now
   corrected in place (above), not only in the appended Correction. A
   builder planning clause 5 reads the table, not the narrative.

2. **R7 — L is preregistered.** Clause 3's "on at least one click" and
   clause 5's "first compatible consecutive pair" are existentials over L,
   and an existential over a sequence the prover extends is an
   optional-stopping hazard: click until it flips. Before the first proof
   click, the proof declares L's length (or its stopping rule), the
   compatible-pair rule, and the passing bound for each existential clause,
   the way the held-out split is preregistered with bounds before the data
   is seen. If F is consumed correctly and the argmax does not move within
   the declared L, the honest verdict is that the field was insensitive,
   recorded as such; the clause is not satisfied by extending L.

3. **R8 — clause-level standing has a non-theorem-bearing form.** Clause 6
   plus R5 makes the theorem all-or-nothing, which is the pressure that
   produced document-edit "discharges" in the old proof. Each clause k may
   be recorded as "W_k holds on L; theorem open" in a standing ledger that
   carries no theorem claim, so the record can move without the claim
   moving. The theorem is claimed only when all seven stand on the same L.

Also recorded from claude-5: ⟨1⟩7's blocker was never an accepted
increment; the route attestation was `:none-declared` because nothing in the
click path supplied declarations, and kimi-4 has now built both halves.
Under PROOF-2 that is a carrier, not a step.

## Amendment register from the discovery packets (2026-09-24, claude-8)

Each row is a proposed amendment as its packet states it. Status
`:proposed` means no A20 review has yet accepted it; the reviews were
dispatched 2026-09-24 05:01 (codex-14/zai-1 SPEC-N, codex-10 SPEC-F,
codex-20 B-D, codex-21 GEN-D, codex-23 OBS-D). Nothing in this register
changes a clause above until its status reads `:reviewed`.

Walkthroughs landed: 01 cascades (2db7b5ff, aceb8f26, corrected 76083b6d), 02 selection law (e9e7c405, verified against the three records bitwise), 03 observation and A (fcbf1e65, six figures byte-identical on regeneration; census equals OBS-D Revision 2 row by row), 04 learning and B (bf26d80b, six figures byte-identical; ledger, close-71 and tick shas reproduce), 05 close and adjudication (f3a35ba2; `:accepted?` domain census true 1 / false 7 / :refused 1 / :no-acceptance-declared 2 / absent 3 reproduced; fig3 varies only by the checkout HEAD line it names). Their findings enter this register as AR-16 (from CLICK2-D) and AR-22.

Standing note (claude-8, 2026-09-24 05:20): both Zai seats returned HTTP 429
"Weekly/Monthly Limit Exhausted", reset 2026-09-29 10:04:33, while reviewing
SPEC-N (zai-1) and F-L (zai-2); neither review was written. Kimi's 5-hour
quota ended at 05:12 the same morning. Until Zai returns, A20's Zai signature
cannot be collected, so no row here can pass `:codex-accepted`; Codex reviews
continue and are recorded as such.
Codex followed at ~05:20 ("You've hit your usage limit", reset 2026-09-26
13:52): B-C (codex-2), the OBS-P amendment (codex-1), walkthrough 02
(codex-3), the F-L review (codex-10) and NUM-R-r2 (codex-14) all died at the
prompt or mid-edit; partial work is under `proof2/partial/`. Landed before
the cutoff: OBS-D revision 2 (508a410e: zero eligible pairs in the enlarged
inventory, 64 measured observations without independent truth), GEN-D
revision 2 (b3a79236), NUM-R review (33ff45f2, REJECT on three decoder
defects). Until 09-26 no Codex review can be collected either, so every
row's next status change waits; implementation continues on Claude seats
under A20's rule that Claude does not review Claude.

| # | Packet (sha) | Touches | Amendment | Status |
|---|---|---|---|---|
| AR-1 | OBS-D (3277e9e6) | W1 / clause 1 | Define "eligible pair": one token of one target with both legs boolean, occurrence-bound, truth leg from an independent checker; typed-absent legs never enter a denominator; `:accepted-increment` verdicts barred from both legs. Today's population has zero eligible pairs, so clause 1 has no measurable A yet. | :codex-rejected pending OBS-D-r2 (codex-23 8bb7df26): the eligibility definition itself held, but the four-site inventory omitted `:kernel-example` and `:learning-trial-receipt` at close, so the zero-pair census covers only the examined sites | :codex-rejected; OBS-D-r2 landed 508a410e (0 eligible pairs on the full inventory), re-review by codex-23 waits for 09-26; OBS-P amendment to Revision 2 landed as 54295ca0 (claude subagent, kernel-example leg with source path+sha, receipt refused as a leg by check, `pair-digest` as a CERT-S §3 extracted-value hash, 16 tests / 302 assertions, warrant test-registry-c7fd8575), unreviewed | AR-2 | B-D (64b091ce) | W5 / A13 | Define "relevant concentration row" as the normalized achieved-cell of the declaring family's two-cell posterior, keyed by `:theta-key`, not the banked `:family` digest. | :codex-rejected (codex-20 15ecfb2d); B-C landed 4a2ba931 on the corrected population (rows appended at comparison, per-row close acceptance, one-hot `:trial-vectors`, fixed-state `:normalization`, refused-row-drop falsifier pins theta 3/4 from 1790199409; warrant test-registry-75592c26, 10 tests / 103 assertions), unreviewed; B-D Revision 2 appended by claude-8 (see B-D.md), re-review by codex-20 waits for 09-26 |
| AR-3 | B-D (64b091ce) | X5 | Define "trial vector" as the singleton-carrier pair (O = {achieved, not}, S = one state per pattern id); the state-belief-alteration clause of X5 is inapplicable by construction and must say so with the reason. | :codex-rejected pending B-D-r2 (codex-20 15ecfb2d): population and commit point wrong, see AR-17 |
| AR-4 | B-D (64b091ce) | X5 | Enumerate the three dedup layers (ledger identity, update-occurrence `:already-recorded`, read-side identity collapse); a duplicate bad case must name its layer. | :codex-rejected pending B-D-r2 (codex-20 15ecfb2d): population and commit point wrong, see AR-17 |
| AR-5 | GEN-D (c7fe3001) AM-1 | W5 | W5's normalization names no Lean declaration; add `DirichletLearning.rowTheta` (or the equivalent) and bind the judge's consumed theta to it. | :codex-rejected pending GEN-D-r2 (codex-21 7c334441); axis fixed by AR-18 | :codex-rejected; GEN-D-r2 landed b3a79236, re-review by codex-21 waits for 09-26 | AR-6 | GEN-D (c7fe3001) AM-2, SPEC-N (634877af) §3 | CERT-S §1, W3/W6 | Give `#wm/double "<hex>"` a Lean meaning: `DecodeExact` by the binary64 sign/exponent/fraction rule, signed zero kept in the raw identity, NaN and infinities refused. SPEC-N §3 supplies the rule GEN-D asked for. | :codex-accepted for SPEC-N's half (codex-14 15b8a15e); GEN-D's AM-2 half rejected with GEN-D (codex-21 7c334441), see AR-21 |
| AR-7 | SPEC-F (b03017a5) | A12, A17, W3 | The F-ablation arm is an intervention that drops F and retains G, on byte-joined inputs identical to the full arm; not "habit alone" and not a fabricated historical F. | :codex-accepted (codex-10 c10da1ff); Zai pending |
| AR-8 | SPEC-F (b03017a5), SPEC-N (634877af) §5 | CERT-S §1 | "Exact-rational prefix-sum dual-carrier" cannot denote the mathematical F total. Replace by machine-double total + symbolic prefix expression and inputs + rational bounds; this is a value-form change and needs `:wm/proof2-certificate-v2`, not a v1 reinterpretation. Historical records are not edited. | :codex-accepted for SPEC-F's half (codex-10 c10da1ff); SPEC-N reviews pending |
| AR-9 | SPEC-N (634877af) §4, §7.1 | W3, W6, W5 selection comparison | Replace each machine-double = real-expression assertion by `RefinesWithin`: a Lean-proved rational interval `l ≤ X ≤ u` with `d−e ≤ l`, `u ≤ d+e` against a preregistered per-term error budget. Literal equality stays for the rational subterms of W0/W1/W2/W4/W5. | :codex-accepted (codex-14 15b8a15e); Zai pending |
| AR-10 | SPEC-N (634877af) §4.3, §7.3 | W3, W5, W6 | Define strict action stability: for every action b ≠ recorded, upper mass bound of b < lower mass bound of recorded, summed over all policies of each action; overlapping intervals are `:action-stability-unresolved`, an open CHECK under R8, never a tie by name order. Both intervention arms (W3) and both B arms (W5) need it separately. | :codex-accepted (codex-14 15b8a15e); Zai pending |
| AR-11 | SPEC-N (634877af) §7.4 | W6, A16 | Decoded floating normalization residuals are bounded separately from exact normalization of the ideal carrier; a failed exact equality or a missing consumed input is not repaired by offline reconstruction. | :codex-accepted (codex-14 15b8a15e); Zai pending |
| AR-12 | GEN-D (c7fe3001), B-D (64b091ce) | Strategy order | B-C lands before GEN-E and B-N before GEN-B, because no close record today carries a `:b-update` key for GEN-E to extract. Naming: the carrier uses B-D's `:trial-identities` (cells + `:theta-key`); GEN-D's `:trial-vectors` placeholder follows it. | :amended by codex-20 15ecfb2d: the name difference is a real contradiction, GEN-E needs one-hot trial vectors that B-D's carrier does not supply; B-C must carry one explicit vector encoding alongside the identities |
| AR-13 | SPEC-F review (codex-10 c10da1ff) §6.1 | CERT-S v2 | SPEC-F's local `:numeric-refinement` entries under F and its steps are references to SPEC-N's full-path-keyed `SC :numeric-refinement` collection, or must equal it; one authority, no divergent duplicate receipts. Assign paths for the diagnostic receipts and the `:candidates i :f-prefix` alias. | :proposed |
| AR-14 | SPEC-F review (codex-10 c10da1ff) §6.2 | CERT-S §3 | Two hash projections for F: `:value-sha256` hashes the consumed scalar in its declared encoding (joined to consumer totals); the ordered prefix/input-expression receipt is bound separately by a recomputed content hash at a stated path. A structured input map and a scalar consumer cannot share one canonical value hash. | :proposed |
| AR-15 | OBS-D review (codex-23 8bb7df26) §4 | CERT-S §3, GEN-D | `:pair-sha256` on a token-outcome pair must state its byte domain and role: a canonical extracted-value hash per CERT-S §3, or a raw-record identity per GEN-D, never both. Until stated, the pair carrier is not authoritative. | :proposed |
| AR-16 | CLICK2-D (zai-2 649a5bd7) | P₀ / record schema | An abstained run record carries no typed decline: `no-constructed-candidate` and its per-candidate reason (`:no-new-wanted-token` at `war_machine.clj:6582`, written to the cohort's `002-selection.edn` under the translated name `:new-wanted-token-within-horizon`, `war_machine.clj:6612-6618`) are absent from the tick run record itself; they survive in the cohort's selection event and the scan markdown (corrected per walkthrough 05). Add a typed decline carrier on the abstention path so an EDN-only reader can tell "no candidates declared" from "want already true". Click 2 (1790225596) is the instance: the declared sources file is byte-identical to 09-23's, and the target's sole want was observed true because the 09-23 click's own commit 97e17e10 marked the ticket DONE. | :landed 97a84770 (kimi-2, D8; `[:decision :abstention]` carrier), unreviewed |
| AR-17 | B-D review (codex-20 15ecfb2d) §3, §7.1 | W5, A13, X5 | The B commit point is not the accepted close. `learning-ledger/record!` runs at token comparison (`full_loop_runner.clj:3150` at 93531d41) before the accepted-increment is decided, and the judge reads every row by `:theta-key` regardless of acceptance. Instance: machinery-71 attempt-002 closed with `:accepted? :refused` and its row 8e7d1aaf… was appended; that row is the single trial behind C1's theta 3/4 on record 1790199409. The population the proof must describe is "rows appended at comparison", each carrying its close's acceptance status as a field. Live instance of the B-R gap (walkthrough 04): the ledger's twelfth form was appended by 76/002 at 21:44:04Z, after the 09-23 judge read at 21:38:43Z, so `evidence-to-disposition-once` reads 1/2 today and 1/4 on the record; nothing on the record says which ledger version was read. | :proposed |
| AR-18 | B-D review (codex-20 15ecfb2d) §6 | AR-5 (GEN-D AM-1) | `rowTheta` must be fixed-state outcome normalization (normalize over O at the singleton state), not normalization across states of row o, which is identically 1 for S = Fin 1. State the specialized two-outcome function and prove it. | :proposed |
| AR-19 | SPEC-N review (codex-14 15b8a15e) §amendment 1 | AR-10, W3/W5/W6 | A strict policy-score gap counts only as a proved gap between ideal scores (lower bound of the winner above the upper bound of every other), never a positive gap between recorded scores; injectivity of the policy-to-action map does not rescue the latter. | :proposed |
| AR-20 | SPEC-N review (codex-14 15b8a15e) §amendment 2 | AR-13, CERT-S v2 | The v2 refinement entry grammar: `SC :model-inputs <id> :F` is a container, not the scalar; name its `:total` value path and the symbolic expression and input paths separately; every alias carries candidate id and payload hash; an action marginal records all contributing candidate joins; hashes recomputed per CERT-S §3 with value and proof metadata separated, and no entry hashes itself. | :proposed |
| AR-21 | SPEC-N review (codex-14 15b8a15e) §amendment 3 | AR-6, GEN-D AM-2 | The machine value in Lean is the cast of `DecodeExact` of the recorded encoding, the encoding retained for identity; the ideal F expression gets a separate name; the refinement relation is proved between them. An unspecified symbolic real is not a decoder. | :proposed |
| F-L | SPEC-F §2 (b03017a5); mathlib4 41a3691f (codex-13, branch darktower) | W3 relation | `Proof2/PrefixFreeEnergy.lean` exists: `prefixVFE` as the finite sum of per-step VFE terms, the prefix lower bound, equality iff every step is the exact update, and a two-step negative control with exact gap `Real.log 2`. No hypotheses beyond the per-step normalization and positivity ones. claude-8 rebuilt it 2026-09-24: build success, twelve axiom audits standard. This is the relation only; no witness on any record. | :built, unreviewed (codex-10 and zai-2 review jobs died on quota; A20 waits for 09-26 / 09-29) |
| AR-22 | Walkthrough 02 §6 (e9e7c405), record 2026-09-22-1790053967 | W6 (final posterior and action), AR-10 | The record carries two argmaxes with two tie rules that disagree on an exact tie: `bayes-choice` (cascade_selection.clj:157-160) orders by `(str head-map)` and chose C3, which was enacted (`:selection-event :policy-key`); the per-policy argmax (policy.clj:437-440) orders by `pr-str` of the candidate map and recorded C2. W6 must name which field is the machine's action on a tie, and the tie order must be defined on a stable key (the pattern id), not on the printed form of a map, whose key order changes between array-map and hash-map sizes (on 1790199409 the head map printed `:theta-source` first). | :proposed |
| AR-23 | Walkthrough 03 §3 (fcbf1e65); `accepted_increment.clj:78-81` | W1 / clause 1, OBS-P revision rule | The accepted-increment's conjunct (c) observes the target's acceptance locator as declared (`:sha "HEAD"`), not at the artifact revision the comparison is pinned to: on 76/002 HEAD resolved to 69c6346a, a commit later than the artifact sha 97e17e10. Define the acceptance observation as pinned to the after-revision; a verdict read at a floating HEAD is not the same event as the comparison it is joined with. | :proposed |
| AR-24 | Walkthrough 03 §5 (fcbf1e65); record 1790199409 and 76/002 | W1 / clause 1 | On the recorded clicks the scorer consumed no token-level A at all: the precision-family model is `:class-emission` with `:rates {:status :absent :reason :class-emission-has-no-token-rates}` and the G decomposition's A term is `:consumed-value-not-recorded`. Clause 1 as drafted assumes a consumed A to compare with a measured one; on these records there is neither. State clause 1's precondition (a consumed token-level A exists on the click) explicitly, so its absence is a finding and not a vacuous pass. | :proposed |
| AR-25 | Walkthrough 04 (bf26d80b); closes 72/001 and 71/001 | AR-17, B-C carrier, X5 | `[:payload :judgment :accepted-increment :accepted?]` is not a boolean field on the live population: 72/001 carries the keyword `:no-acceptance-declared` and 71/001 has no key. Define the field's domain as boolean or a typed absence `{:status :missing :reason …}`; a keyword in a boolean position is the shape [[absence-must-not-read-as-a-value]] forbids. The B-C carrier passes the first through as recorded and types the second; that is honest but the producer should not emit it. | :proposed |
| AR-26 | Walkthrough 04 (bf26d80b); record 1790199409 `[:decision :g-term-decomposition :policies i :terms]` | P5, W5 | The G decomposition's terms are `:A :C :D :E :F :Q`; there is no `:B` term, although P5 names one and the theta the judge stamps is the consumed B. Either add the `:B` term to the decomposition (the stamped theta and its provenance) or amend P5 to bind B through the candidate's precedence theta rather than the decomposition. | :proposed |
| AR-27 | Walkthrough 05 §2 (f3a35ba2); close 76/002 | Clause 6 / close, A20 evidence, [[absence-must-not-read-as-a-value]] | The one close with `:accepted? true` is `:outcome :grounded-no-change` with grounding witness `:resolved? false`, because the readback's `:props` arrived as a 12,981-character string (the futon1b rescue-stringify reshape, now logged by the write log) and could not be compared. The accepted-increment and the grounding verdict share no input, so a close can be accepted and ungrounded at once. Define what "accepted close" requires of the grounding witness, and treat a stringified readback as a typed refusal of the witness, not a no-change. | :proposed |
| AR-28 | Walkthrough 05 §5 (f3a35ba2); all 14 closes | Clause 6 / close | `:run-ending-classification` is `{:class :unknown :missing [:attested-increment]}` on all eleven grounded closes (route attestation `:none-declared` on every one; declarations landed later in c6fa1ab2), `:known-typed-failure` on two, nil on 75/002. No recorded close is classified as a known success; clause 6 cannot read a close outcome from these records. | :proposed |
| AR-29 | Walkthrough 06 §4 (6d82bc54); `data/wm-repair-obligations/` | Clause 7 / discharge | Discharge and publication have never succeeded on this store: 0 of 68 resolutions and 0 of 59 implementations carry `:repair/discharge-context`, and all 9 discharge operations refuse `:unsafe-repair-id` at binding (counts re-derived by claude-10). Both refusals are typed and recorded, not silent. `:repair/status :open` is a write-time constant on all 129 findings, so openness is a join over the other subdirectories, never a field. The discharge clause needs one executable success on a record, or it holds vacuously. | :proposed |
| AR-30 | Walkthrough 06 §2 (6d82bc54); record 1790225596 `[:open-stop-lines :ids]` | Stop-lines | The 09-24 click's own new finding (`repair-occ-ad16e2c2…`, `:environmental-hold`) is not among the 34 stop-line ids its record carries (checked by claude-10): the store read precedes the close that writes the finding. A record's stop-line set therefore describes the store before the click, not after. State which one the stop-line carrier means. | :proposed |
| AR-31 | Walkthrough 06 §5 (6d82bc54); ticket commit 97e17e10 | Close / C4 observation, E-cascade-real | T-repair-occ-444fb018's ticket reads DONE, written by 97e17e10, the 09-23 click's own build commit (OPEN→DONE plus an "Accepted restoration" section citing the dated recheck and `resources/wm/eig/held-out-calibration.edn`). The target's sole want is observed through that Status line, so the click wrote the token it was selected to produce. The store's finding for the same id stays `:open` with no resolution, and no code in `src/` joins ticket Status into the store (walkthrough's claim, not re-derived). Say whether a want observed through a file the click itself may edit can count as produced, and which of ticket and store is authoritative. | :proposed |
| AR-32 | PROOF-2-ARCH-draft (claude-10); E-outer-loop O1–O8 | new Clause T | Add Clause T: the tick's target field (considered, feasible support, typed exclusions) and the choice among targets by the selection posterior at target grain, feasibility as support; an empty support records per target what would make it feasible. Witness condition, not a gate. | :adopted-in-PROOF-2a |
| AR-33 | PROOF-2-ARCH-draft; E-cascade-real D3, D4, D11, D15; probe P1 | Clause 0 | W₀: construction receipts `:machine-constructed`, naming the interpretations used (author recorded, agent authorship allowed), and replay of the constructor on them reproduces each candidate. X₀(c) unreproducible candidate; X₀(d) all-or-nothing refusal where admission would accept a partial advance. | :adopted-in-PROOF-2a |
| AR-34 | PROOF-2-ARCH-draft; glossary ¶Policy π; E-cascade-real D18 | Clause 0 carrier, clauses 4–5 | Restate over a descent relation with `acyclicDescent` and `hasMeets` (CascadeOrder.lean) plus co-application; a list is the chain case. Missing definition: the transition kernel of a semilattice cascade (which of several incomparable enabled patterns fires, or co-application). X₀(e): flattening changes nothing ⇒ typed finding that the semilattice did no work. | :adopted-in-PROOF-2a |
| AR-35 | PROOF-2-ARCH-draft; E-cascade-real N1; E-flight-aif W1, Q2 | new term | A prior over lower-level patterns conditioned by higher-level ones (priming). Needs a Lean definition before a clause; falsifier to carry: removing a priming edge must change the recorded prior. | :adopted-in-PROOF-2a |
| AR-36 | PROOF-2-ARCH-draft | preamble | State that clauses 0–6 certify the rewrite reading of a pattern (consumes, forbids, produces) only, not its forces or its conditioning. | :adopted-in-PROOF-2a |

Cross-packet check (claude-8, 2026-09-24): SPEC-F and SPEC-N agree on the
F carrier (AR-8) and on the ablation arm (AR-7). GEN-D's AM-2 is answered by
SPEC-N §3 (AR-6). B-D and GEN-D differ only in a key name (AR-12). No
packet appeals to a ruling. Every falsifier is restated as a concrete bad
case; SPEC-N's log-2 case was checked here by exact fraction arithmetic
(d < l, P24(l)+R24(l) < 2 < P24(u)−R24(u)) and its posterior-sum example
(1 + 7/2^55 on record 1790199409) reproduces from the hex doubles.
