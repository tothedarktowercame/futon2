# SPEC-F — observed-prefix F and an F-ablation diagnostic

2026-09-24. Discovery/specification, proposed for independent review; no clause
standing is awarded. Scope: strategy row 2, serving clause 3 and clause 6's F
input. No production edit, click, record write, JVM evaluation, or Lean build.

Source revision inspected: futon2 `93531d415a115c7b426f37e06d7d5542ecb5dae4`;
mathlib4 `77fdbda5b5629b3c8f6c7f9bbb027da0436ba1b3`. Code anchors below are
relative to those repositories (Lean paths have the `mathlib4/` prefix).
The strategy §4 D/R contract applies: this packet specifies proofs, it does not
claim that the Lean statements below have been compiled or proved.

## 1. Definition and its evidence boundary

For an admitted history of the **same policy** π, let n > 0 and let the ordered
steps be indexed by i : Fin n (record `:tau` = i.val + 1). Each step has its own
finite-state observation kernel Aᵢ, transition kernel Bᵢ, previous belief
sPrevᵢ, actually observed event oᵢ, and posterior qᵢ. Use a fixed finite state
carrier S and outcome carrier O within a compatible prefix; a carrier change
requires an explicitly proved transport, not a join by similarly spelled tokens.

Define

    pᵢ(x) = predictedState Bᵢ sPrevᵢ x
    Zᵢ = observationProbability Aᵢ Bᵢ oᵢ sPrevᵢ
    fᵢ = variationalFreeEnergy (fun x => Aᵢ x oᵢ) pᵢ qᵢ : EReal
    Fprefix(π) = ∑ i : Fin n, fᵢ : EReal.

This is an unweighted finite **sum**, in nats: no division by n, no discount,
no selection-temperature factor, and no replacement by fₙ. n is observed
history length, not the future rollout horizon. A length-one witness is the
special case `∑ i : Fin 1, fᵢ = f₀`, not the definition of F.

Admission additionally requires: normalized nonnegative A/B rows and beliefs;
measured-A provenance (A10); the executed-policy/action→occurrence→observation
join; unique occurrence identities; strictly ordered steps; each input fixed
before its update; and `sPrev_(i+1) = qᵢ` on adjacent steps. Initial sPrev is
an explicitly admitted boundary belief, not necessarily the present click's D.
Each step proves `exactUpdate Aᵢ Bᵢ oᵢ sPrevᵢ = some qᵢ`. A/B versions may
vary only under a declared compatibility rule, with their actual historical
values retained; this definition does not authorize rescoring old observations
under today's A/B. The fixed-model case is a specialization. A disconnected
set of past attempts cannot be concatenated into a prefix without this boundary
and continuity evidence. Selecting a policy is not executing every prospective
step of it.

Evidence for these choices and limitations:

- `src/futon2/aif/policy_prefix_evidence.clj:17–53` defines a fixed-model,
  synthetic-only evaluator. Lines 27–37 require per-step redraw, a nonempty
  prefix, distinct occurrences, consecutive tau, and normalized transition
  rows on supported states. Lines 38–49 predict from the previous q, condition,
  retain prior/prediction/result, feed the posterior onward, and **add** each F;
  line 53 returns the total. This is arithmetic precedent, not live admission.
- `src/futon2/aif/observation_model.clj:301–313` computes the observed event's
  predictive probability and posterior; probability zero yields contradiction.
  The event kernel must be extracted as actually queried, including partial
  event semantics, not replaced with an unrelated full-state observation.
- `src/futon2/aif/policy_prefix_evidence.clj:56–70` removes `:f` and emits
  `:not-supplied`. The judge invokes it at
  `scripts/futon2/report/war_machine.clj:6475`; thus this source does not provide
  an admitted production prefix. Absence is a producer defect, not a desired
  semantics or proof of zero F.
- `src/futon2/aif/cascade_free_energy.clj:90–103,105–135` rolls forward to one
  future horizon and computes one `−log p`. Its `:q0/:rates/:observed/:theta`
  params are not the n historical step tuples. Production's flag skips that
  call at `src/futon2/aif/efe.clj:1125–1126`; reviving it is not H4 supply.
- `mathlib4/DarkTower/WarMachine/PolicyVariationalFreeEnergy.lean:42–50`
  defines support-aware EReal VFE, including ⊤ for posterior mass on zero
  joint support. `ExactBeliefTrajectory.lean:46–60,103–115` defines prediction,
  evidence, exact update, and the per-step equality characterization. Its
  declared reduction at `:31–39` is filtering, not smoothed trajectory inference.

With exact updates and Zᵢ > 0,

    Fprefix = ↑(∑ i, −Real.log Zᵢ).

The product `∏ i Zᵢ` is the sequential predictive evidence under the declared
conditional models. Calling it the probability of a single joint trajectory
requires an additional joint-model/conditioning correspondence theorem. In
particular, summing filtering VFEs does not identify the product of filtering
marginals with a smoothed trajectory posterior.

Zero evidence is a typed impossible-observation/zero-support case, not a finite
F. Missing history is `:not-supplied`, not the algebraic empty sum 0. The sum can
be defined at n=0 for induction, but the production admission predicate excludes
it. The real-valued selection input requires a proved finite value; never use
`EReal.toReal` to turn ⊤ into 0.

**Record limit.** This packet does not independently recensus run records. The
fully read `PROOF-2-F-discovery-2026-09-24.md` §2 reports absence in runs
1790033693, 1790053967, and 1790131591 at
`[:decision :selection-certificate :candidates i :f-prefix]`, joined against
`[:decision :selection-certificate :token-belief-input :observation-updates]`
and `[:decision :selection-certificate :token-belief-stage :observation-updates]`.
Its §3 reports missing q. Those are discovery findings, not positive witnesses
supplied here. No record with an admitted two-step prefix is exhibited by this
packet, and no existing record is claimed to satisfy the proposed schema.

## 2. Exact Lean obligation: PrefixFreeEnergy.lean (F-SUM / F-L)

Proposed module: `mathlib4/DarkTower/WarMachine/Proof2/PrefixFreeEnergy.lean`.
The following is the exact signature specification, not implementation code.
Use the indicated aliases and binders throughout:

```lean
namespace DarkTower.WarMachine.Proof2.PrefixFreeEnergy
open scoped BigOperators
open DarkTower.WarMachine.PolicyVariationalFreeEnergy
open DarkTower.WarMachine.ExactBeliefTrajectory
variable {S O : Type*} [Fintype S] [DecidableEq S] {n : ℕ}
variable (A : Fin n → S → O → ℝ) (B : Fin n → S → S → ℝ)
  (o : Fin n → O) (sPrev q : Fin n → S → ℝ)

noncomputable def prefixVFE : EReal :=
  ∑ i : Fin n, variationalFreeEnergy (fun x => A i x (o i))
    (predictedState (B i) (sPrev i)) (q i)

variable (hA : ∀ i x, 0 ≤ A i x (o i))
  (hB : ∀ i s x, 0 ≤ B i s x)
  (hB1 : ∀ i s, ∑ x, B i s x = 1)
  (hp : ∀ i s, 0 ≤ sPrev i s)
  (hp1 : ∀ i, ∑ s, sPrev i s = 1)
  (hZ : ∀ i, 0 < observationProbability (A i) (B i) (o i) (sPrev i))
  (hq0 : ∀ i x, 0 ≤ q i x)
  (hq1 : ∀ i, ∑ x, q i x = 1)

-- Required theorem type; proof body owed:
-- prefixVFE_ge_neg_log_evidence :
(↑(∑ i : Fin n, -Real.log
  (observationProbability (A i) (B i) (o i) (sPrev i))) : EReal)
  ≤ prefixVFE A B o sPrev q

-- Required equality-case theorem type; proof body owed:
-- prefixVFE_eq_neg_log_evidence_iff :
prefixVFE A B o sPrev q =
  (↑(∑ i : Fin n, -Real.log
    (observationProbability (A i) (B i) (o i) (sPrev i))) : EReal)
  ↔ ∀ i, exactUpdate (A i) (B i) (o i) (sPrev i) = some (q i)
```

Bare proposition lines specify theorem result types under the displayed
binders; this block is deliberately not offered as a compilable Lean module.
All displayed hypotheses are binders of both theorem obligations.

Required corollary `prefixVFE_exactUpdate_eq`: under the same hypotheses and
`hupdate : ∀ i, exactUpdate (A i) (B i) (o i) (sPrev i) = some (q i)`, prove
the equality on the left of the iff. This is the summed equality case of
`ExactBeliefTrajectory.exactUpdate_minimises_vfe` (`:103–129`).

Proof decomposition: derive each lower bound from
`PolicyVariationalFreeEnergy.vfe_ge_neg_log_evidence` (`:271`) and each equality
iff from `ExactBeliefTrajectory.exactUpdate_minimises_vfe` (`:103`). Summing
lower bounds gives the first theorem. Forward direction of the iff additionally
requires showing that equality of the total forces every nonnegative gap to
vanish: no cancellation is possible; a ⊤ summand cannot equal the finite bound.
On finite branches use the KL decomposition
`PolicyVariationalFreeEnergy.vfeReal_eq` (`:159`) and nonnegativity of each KL
(`:191`); equality characterization is at `:344`. Reverse direction substitutes
the step equalities and commutes finite sum with real-to-EReal coercion.
The author must prove the needed EReal finite-sum facts, not assume finiteness
from printed decimals.

Also require singleton reduction and append/additivity (splitting the finite
index set). These pin down the aggregation independently of the equality case.
For a real scalar f used by selection, the separate consumed-value proposition
must identify f with the real finite total (through SPEC-N's numerical relation).
The theorem holds on fixed step inputs; it does **not** claim global optimality
when changing qᵢ also changes the later sPrev. History continuity/admission is a
separate checked predicate, not an unproved consequence of summation.

## 3. Proposed precise amendments: A12, A17, W₃

**Replace A12's opening and asserts/stub-risk text with:**

> For each candidate policy π, F denotes the finite sum Fprefix(π) in SPEC-F §1
> over its nonempty, admitted, occurrence-identified observed prefix. Each
> summand is `variationalFreeEnergy (Aᵢ · oᵢ) (predictedState Bᵢ sPrevᵢ) qᵢ`;
> each exact-update posterior gives `−log Zᵢ`. Recorded means that the ordered
> step tuples, admission/continuity evidence, per-step values and total are
> retained under the certificate's `:model-inputs <id> :F`, with the total
> bound to every consumed F occurrence. Consumed means the total enters
> `log E − Fprefix − gamma G` without length or temperature scaling. Missing
> history remains typed absence and the actual reduced law is named; it does
> not discharge F supply. On the preregistered ordinary field within L, the
> F-ablation diagnostic must change the selected action after action
> marginalization, retaining E, G, temperature, candidate eligibility,
> action mapping, tie rule, and code identity. Comparing against habit alone
> cannot establish this, because it also removes G.

Keep A12's exclusions (prediction-error alias, G alias, constant substitution,
post-choice diagnostic), and add last-summand substitution and empty-prefix zero
as falsifiers. Dependencies additionally name A16, A17 and A21; measured A and
admitted D remain prerequisites. Unequal F values alone do not imply an action
change.

**Replace A17's opening/asserts/stub-risk text with:**

> Sensitivity claims use a single ordinary recorded candidate field with
> immutable consumed inputs. Clause 5 compares two historically real, compatible
> B versions under its declared first-consecutive-pair rule. Clause 3 instead
> uses the preregistered diagnostic intervention `do(F-vector := 0)`, retaining
> the recorded G vector and every non-F scorer input byte-identically. This
> one intervention is exempt from the historical-alternate-value requirement:
> zero is a declared ablation, not a measured historical F and not a missing
> prefix. Both diagnostic posteriors and action marginals are retained, along
> with input hashes and the changed action. Replay evidence establishes
> sensitivity of the recorded selection law only; it cannot substitute for
> proof that the ordinary click consumed the baseline F. No diagnostic
> dispatches work or supplies evidence to later learning. The intervention,
> tie rule and passing bound must be fixed before L; an insensitive L leaves
> clause 3 open and does not authorize extending L.

Keep A17's prohibition on changed candidates/E/C/A/D/temperature/tie/code;
for F also explicitly freeze G and eligibility. Exempt only the declared
all-zero F intervention from its fabricated-alternate falsifier, not arbitrary
winner-separating replacements. No change to historical B provenance or the
unconditional read requirement in A14. This is a scorer-input intervention,
not an intervention on observations that recomputes D, B, or G.

**Replace W₃ with:**

> For each required candidate π, extract the ordered admitted prefix and prove
> its nonempty identity/continuity predicate and the SPEC-F prefix-VFE relation.
> Where each qᵢ is the exact update, prove the sum equals `∑ᵢ −log Zᵢ` using
> `prefixVFE_exactUpdate_eq`. Relate the consumed machine F total to that exact
> value by the reviewed numerical-refinement proposition, not literal equality
> of a rounded double with a real logarithm. Prove the corresponding refinement
> of the actually consumed policy posterior against
> `PolicySelection.selectionPosterior t E Fprefix G`. On the preregistered
> ordinary field within L, the selected Bayes action under this posterior
> differs from the selected Bayes action under
> `PolicySelection.selectionPosterior t E (fun _ => 0) G`, using identical
> policy-to-action mapping and declared tie-break. Prove both Bayes-action
> predicates and the deterministic tie-break correspondence; numerical
> uncertainty must not manufacture a winner reversal.

`PolicySelection.selectionPosterior` returns ENNReal
(`mathlib4/DarkTower/WarMachine/PolicySelection.lean:29–31`), whereas
`ActionMarginal.actionMarginal/IsBayesAction` use real policy masses (`:21–27`).
The witness must justify the finite normalized posterior conversion, not erase
that type boundary. `IsBayesAction` alone permits ties; the actual runtime rule
is declared at `src/futon2/aif/cascade_selection.clj:125–129`. Policy argmax
inequality alone is insufficient: distinct winning policies may share an action.
The runtime subtracts unscaled F and G/beta at `:109–113`.

**Alternatives and claim limits.** A historical F-old/F-new comparison would
retain A17 literally, but needs two admitted histories for every affected policy
on the same compatible field. If histories change other scorer inputs, a replay
freezing those inputs is still a controlled scorer comparison, not two otherwise
identical live clicks. It tests the particular historical increment, not the
presence of the F term; an additive common increment can leave selection
unchanged. This packet recommends all-zero ablation because it isolates the
named term and matches W₃'s existing intended contrast. Neither option proves
that F improves outcomes, that inference is accurate, or that the selected action
succeeds. Habit-only can diagnose E versus the combined F/G contribution, but
cannot attribute a change to F. These are definitions of distinct experiments,
not interchangeable labels for one result.

## 4. Certificate contract (CERT-S §1 refinement proposal)

Keep CERT-S's root and joins: `[:decision :selection-certificate :model-inputs
<id> :F]`, keyed by the small candidate id with `:candidate-payload-sha256`;
never join by ordinal alone. Keep consumer totals at `:candidates i :f`,
`:policies i :f`, and `:g-term-decomposition :policies i :terms :F` below the
same certificate root, with equal value hashes. Their F-prefix identity must
also join `:candidates i :f-prefix`. Hash value payloads using CERT-S §3;
producer/admission and consumption metadata must not be circularly included
inside their own value hash.

Proposed term entry details (new obligations, not claims of existing fields):

| Under `:model-inputs <id> :F` | Required content |
|---|---|
| `:definition`, `:aggregation`, `:length`, `:units`, `:z-semantics` | versioned observed-prefix VFE definition; `:sum`; n > 0; `:nats`; `:per-step-redraw` |
| `:policy`, `:candidate-payload-sha256`, `:admission` | exact policy/target/interpretation identity, compatible model/carrier identity, immutable admission receipt and pre-selection cutoff |
| `:f-prefix` | ordered vector of n step maps, not just the terminal tuple |
| `:f-prefix i :tau/:occurrence-id/:execution-ref/:observation-ref` | consecutive index, unique event identity, immutable execution and subsequent observation source refs; policy ownership and temporal joins |
| `:f-prefix i :A/:B` | actual historical kernels or immutable content refs; versions, value hashes, measured-A source rows/estimator refs, B interpreted action/transition provenance |
| `:f-prefix i :sPrev/:o/:q` | normalized previous belief, exact observed event with carrier/context, actual conditioned posterior; boundary or previous-step hash proving continuity |
| `:f-prefix i :prediction/:evidence` | Bᵢ sPrevᵢ and Zᵢ as exact rational carriers when inputs allow; independent checks against the retained kernels |
| `:f-prefix i :f/:numeric-refinement` | actual per-step machine result, symbolic exact VFE expression and certified numerical relation; never a rational falsely claimed to equal log |
| `:total/:numeric-refinement` | actual consumed total, ordered floating accumulation semantics, exact symbolic sum and proved error relation including accumulation error |
| `:value-sha256/:consumed-at` | CERT-S consumption pair; source identity of the total's scorer reader plus matching consumer value hash, not merely evaluator identity |

The top-level `:A/:B/:sPrev/:o/:q` mentioned in CERT-S's F row denote the step
inputs above. For n>1 they must not be five single terminal values purporting to
specify the total. For n=1 any convenience aliases must equal step 0 and be
checked as such. F's historical step A/B need not equal the current click's
prospective A/B or its field-level D; their versions and compatibility joins
must say which is which. A16 forbids reconstructing an absent historical input
later from mutable current state.

**Necessary CERT-S amendment, not a silent schema reinterpretation:** its F row
calls for an “exact-rational prefix-sum dual-carrier”. Rational kernels and
beliefs do not make logarithms rational. Replace that phrase by “exact rational
input carriers plus the exact symbolic real/EReal prefix expression and a
proved numerical-refinement relation to the consumed machine total”. Rational
interval endpoints may certify the expression; they are not the expression.
Coordinate the value encoding with SPEC-N. Because CERT-S §5 versions changed
value forms, adopt the reviewed successor schema version before preregistration;
this packet does not relabel v1 records as compliant.

Missing prefixes keep CERT-S's `{:status :not-supplied :source
:policy-prefix-evidence :reason :no-admitted-policy-prefix}`. Refused admission
retains its reason/step; impossible evidence retains `:zero-support`, distinct
from missing history. The actual law must record omission of missing F, as the
theorem's F sentence requires. An ablated diagnostic is separately identified
as `:diagnostic-intervention`, with baseline certificate content ref, definition
`F := 0`, frozen-input hashes, both score/posterior/action-marginal vectors and
choices. It is never attached as an admitted candidate F or returned to learning.

## 5. Exact falsifiers, downstream use, and remaining amendments

**Strategy-row falsifier 1, concrete:** use S = Unit, O = Bool, both B rows = 1,
sPrev = q = point mass, and two distinct ordered occurrences of o=true. At step
1 set A(true)=1/2; at step 2 set A(true)=1/4 (complementary false masses make
both A rows normalized). Both exact updates return the point mass. F₁=log 2,
F₂=log 4, total=log 8. Replace only the recorded total with log 4 and preserve
both step tuples: the prefix-sum/consumption proposition must fail. This is a
synthetic negative control, not measured-A evidence. Also exercise the
fixed-model variant A(true)=1/2 twice: total=2 log 2, not log 2.

**Strategy-row falsifier 2, concrete:** two policies with different actions,
E=(1,1), beta=1, G=(0,2), F=(log 8,0). The full-law scores are (−log 8,−2), so
policy/action 2 wins; the correct F-ablation scores are (0,−2), so action 1
wins. A purported ablation that also sets G=0 has scores (0,0). It must fail
the frozen-G predicate regardless of which action its tie-break selects, even
if that choice happens to match the correct ablation winner. Thus a winner
inequality alone cannot detect the wrong experiment. Freeze mappings and code
hashes as well as numbers. Independent reviewers must instantiate these bad
extracts before inspecting positive outcomes (A18); this packet is the design,
not a claim that those tests have run.

**Unblocks:** after independent review, F-SUM/F-L's prefix theorem, the
per-step carrier/producer contract, the F-ablation checker, and L preregistration
with a single consistent clause-3 passing condition. It does not unblock live
F supply without measured A, admitted per-policy histories, retained q, numerical
refinement, and consumption joins. It does not remove the cold-start problem
or permit policy-history borrowing (SPEC-L).

**Missing definitions, proposed amendments:** adopt the prefix/admission and
continuity definition in §1; exact signatures and finite-real bridge in §2;
replace A12/A17/W₃ with §3 (also update the proposed F contract's single-step
wording and ASSUME's concluding step-3 restatement); version the symbolic
numeric carrier and per-step F layout in §4; and preregister the history-boundary,
model-compatibility, prefix-length/window, diagnostic action projection/tie,
and numerical-stability rules before L. Prefix lengths may differ naturally;
no post-hoc truncation, normalization, or favorable-window choice is allowed.
These are proposed theorem/schema amendments for review, not questions for the
operator and not satisfied premises.
