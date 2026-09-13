# Row 22 declaration-edge repair specification (2026-09-13)

Status: adopted for bounded implementation by codex-26; no edge firing or admission.
This document refines the reviewed discovery
in `TN-row22-declaration-edge-audit-2026-09-13.md`; it changes neither the edge
ledger nor production.  Every qualification below requires retained records from
one joined run.  A unit test or redirected fixture can commission a refusal but
cannot establish that a production edge fired.

## Common record identity

Every delivered value must carry `:model/id`, `:model/revision`, `:run/id`,
`:tick/index`, producer declaration and source revision.  Candidate-bearing
records additionally carry a stable occurrence-level `:candidate/id` and the ordered candidate
support. Equal semantic actions at different occurrences remain distinct; neither
action-key deduplication nor posterior renormalization may hide missing candidates.  A transformation output repeats the input pins and names its function
and source hash.  Same-tick edges require equal run and tick.  Feedback edges
require the source tick `t`, the destination tick `t+1`, and a predecessor link.
Missing, duplicate, reordered, cross-run, stale, or revision-mismatched joins
refuse; they never become an empty value or a default.

## E1 — replace the conflated R6 -> R11 edge

**Source declaration and field.** R6 `machinePolicySet`, field `pi`: the ordered
ranked candidate list consumed by `select-action`, projected to stable candidate
identity.  R6 `softmaxWithFPi`, field `Q-pi`, is explicitly *not* the source.
`select-ranked-proposal-fields` expects field id/budget and proposals containing
`:id`, `:rank`, `:action`, `:cost`, and `:utility`
(`hierarchical_budget_adapter.clj:74-96`).

**Target declaration and field.** R11 `hierarchical-budget/arbitrate`, input
`:fields[*].proposals`; output is the portfolio `:selected`, `:selected-ids`,
`:node-usage`, `:node-budgets`, and `:within-all-budgets?`
(`hierarchical_budget.clj:157-207`).

**Join and transformation.** For one run/tick and ordered R6 support, a declared
adapter maps each ranked candidate to exactly one R11 proposal.  It preserves
candidate id, action bytes, and rank; `cost` and `utility` must name independent
authorities rather than being inferred from posterior mass.  Field membership
and budgets are versioned inputs.  The output retains a bijection table from
candidate ids to proposal ids.

**State.** The arbiter and generic adapter are implemented and tested, but no
production caller supplies this declared mapping.  The drawn edge is conflated
because it does not say whether `pi`, ranks, or `Q-pi` crosses it.

**Qualification.** A real tick retains the complete ordered R6 support, mapping,
R11 request and replay-identical response; every selected id is in R6 support,
every R6 candidate is accounted for as selected or rejected, and all charged
costs satisfy every ancestor budget.

**Discriminating control.** Swap two ranks while keeping posterior masses fixed:
the retained mapping/replay must change or refuse.  A consumer that instead feeds
`Q-pi` as utility fails this control.  Also refuse a dropped candidate and an
unattributed cost.

## E2 — split R11 -> R16 into portfolio constraint and exact enactment

The phrase “portfolio to action” spans two different boundaries and is replaced
by two edges; both preserve the original budget-before-action obligation.

### E2a R11 portfolio -> R6 constrained candidate domain

**Source.** R11 arbitration output `:selected-ids`, `:selected`, budget proof and
replay receipt.  **Target.** R6 `machinePolicySet`, field `pi-approved`, a subset
of the same tick's original `pi`.  **Law.** Stable-id restriction:
`pi-approved = [c in pi | c.id in selected-ids]`, preserving R6 order and action
bytes.  The run/tick/model and E1 mapping pins must agree.

**State.** Unimplemented: no R11 production caller and no constrained selector
input exist.  **Qualification.** The recorded selector ranges over exactly
`pi-approved`, and every rejected R11 proposal is absent.  **Control.** Inject a
high-scoring unapproved candidate; selection must refuse it rather than choose it.

### E2b R6 selected action -> R16 approved enactment

**Source.** R6/R16 `machineAction` result `u`, including selected stable id and
the E2a approval proof.  **Target.** R16 `close-loop!`/`enact!`, exact candidate
identity and action bytes.  **Law.** Enactment is authorized only when selected
id is a member of the approved portfolio and the enacted action equals that
candidate.  A typed selection/enactment divergence is evidence, not success.

**State.** Unimplemented and presently contradicted by the source: `close-loop!`
reads `:ranked-actions` and enacts the first passing gate, not the recorded
selected `:action` (`enact.clj:294-339`).  **Qualification.** A real record joins
portfolio, selected action, gate verdict, and enacted action, with equality at
the chosen boundary.  **Control.** Make an unapproved earlier-ranked action pass;
the boundary must refuse rather than substitute it.

## E3 — R9 independence verdict -> R16 pre-enact verifier

**Source declaration and field.** R9 independence verdict
`{:claim ..., :witness {:id ... :producer ... :layer ...}, :verdict ...}`.
**Target declaration and field.** A new pre-enact authorization field on the
exact E2b candidate/construction, consumed before `enact!`; only
`:independent` authorizes.

**Join and law.** Claim subject/artifact, candidate/construction id, producer,
run, trace, model revision and review receipt must join the pending enactment.
The verifier emits a pinned authorization record.  Missing/unjoinable identity,
`:self`, `:unknown`, or a verdict for another candidate refuses closed.

**State.** Specified in the edge ledger but unimplemented; neither `enact.clj`
nor the close-loop path consumes an R9 verdict.  Witness-registry review must
not be reinterpreted as a per-action verdict.

**Qualification.** One real independent action reaches enactment through the
verifier and commissioned real-boundary controls refuse self, unknown, missing,
and cross-candidate verdicts.  **Discriminating control.** Reuse a valid verdict
with only the candidate id changed; authorization must refuse.

## E4 — decompose R10 -> R8; do not invent an F-pi operand

**Source declaration and field.** R10 has no mathematical value declaration.
Its actual value is a scheduled-dispatch receipt: commission id, dispatch id,
node `R10`, accepted/running/terminal times and execution outcome.
**Target declaration and field.** R8 `machinePolicyFreeEnergy` consumes the
current observation and the immediately preceding per-candidate prediction;
it consumes no scheduler value.

**Replacement route and law.** Retire the direct data edge and conserve the
scheduling obligation as:

1. `R10 scheduled-dispatch receipt -> run/tick-entry identity`: one successful
   dispatch authorizes one idempotent run launch and its explicitly declared
   bounded tick plan. A single-tick commission permits exactly one tick; a
   multi-tick commission retains its ordered tick identities and does not
   collapse them into a one-job/one-tick fiction;
2. `tick-entry -> R2 observation record`; and
3. `R2 observation + predecessor prediction -> R8 F-pi`, joined by run/tick,
   predecessor tick, candidate action identity, model revision and complete
   coverage.

This is causal sequence plus explicit identity transfer, not `R10 -> F-pi`.
If scheduler metadata is later intended to change F-pi, that requires a new
declaration and theorem before an edge can exist.

**State.** R10 can start work, and R8 is implemented, but the direct drawn edge
has no value and has never fired.  **Qualification.** A real scheduled receipt
joins the exact tick, whose retained R2 and predecessor records feed every R8
candidate with complete-or-off coverage.  **Control.** Cross-map a valid receipt
to another run, or delete the predecessor prediction; the route must refuse.

## E5 — replace R15 -> R13 with forward prior shaping

**Source declaration and field.** R15 slow state `:slow/mode` and its pinned
`:slow/intrinsics`.  **Target declaration and field.** The fast candidate/move
prior and cost fields consumed before R6 scoring: `:prior` and
`:step-score-delta`.  `apply-slow-prior` retains the base cost and applies
`prior' = prior * weight(mode,class)` and
`delta' = delta - ln(weight)` (`temporal_hierarchy.clj:111-163`).

**Replacement edge and joins.** Replace R15 -> R13 with
`R15 slow state at t -> fast prior shaping at t -> R6 ranked candidates at t`.
Join slow-state revision, run/tick, move class and candidate id.  R13
`machineDepth` remains independently configured; it is not derived from the
slow mode.

**State.** The pure shaping and `hierarchical-rollout` composition are built,
but have no production caller or retained live hop.  The original R15 -> R13
stroke is a specification error: the implementation changes desirability, not
horizon.  **Qualification.** A real tick retains unshaped and shaped candidates,
mode weights, exact transformation, changed/unchanged classes, and downstream
selection.  R13's horizon pin is separately retained and unchanged.
**Discriminating control.** Change slow mode with fixed depth and demonstrate
the shaped priors/costs change; change depth with fixed mode and demonstrate
the shaping table does not.  Any implementation that rewrites horizon fails.

## E6 — split the two temporal directions formerly called R15 -> R16

### E6a forward: R15 state -> R6 shaping -> R16 selection/enactment

The source and transformation are E5; the target is E2b.  The identity chain
must retain slow state, shaped candidates, selected candidate, pre-enact checks,
and enacted result for the same run/tick.  It qualifies only if the selected
action demonstrably depends on the shaped candidate table; merely carrying a
mode label does not qualify.  Control: permute mode weights while holding all
other inputs fixed and require the predicted selection change or an honest
no-change result at that field; an unchanged selection is non-qualifying for
behavioral influence at that field. A consumer that ignores the table fails.

### E6b feedback: R16 witnessed outcome at t -> R15 state at t+1

**Source.** R16 outcome fields `:fast/action-class`, `:fast/witnessed?`,
`:fast/succeeded?` and evidence reference.  **Target.** R15 next
`:slow/intrinsics`, `:slow/mode`, `:slow/previous-mode`, and `:slow/feedback`
from `advance-slow-state` (`temporal_hierarchy.clj:190-237`).  **Law.** Exactly
one witnessed outcome updates the named action-class Beta entry once, derives
the next mode, and carries source run `r`, tick `t`, and destination tick `t+1`.

**State.** Pure update exists, but there is no production caller or retained
joined feedback.  **Qualification.** A real predecessor/successor pair replays
exactly and contains independent outcome evidence.  **Control.** Unwitnessed,
missing-predecessor, duplicate, cross-run, and same-tick feedback refuse.

The original direct R15 -> R16 label may remain only as a rendered shorthand
for E6a after E5 and E2 qualify.  E6b is a separately directed R16 -> R15 edge;
retiring the old label must not erase either obligation.

## Explicit retirement: R7 -> R14

Retire this edge as a conflation.  R7 `Pi_k` is observation-channel precision;
R14 `tau` is policy-selection temperature.  No transformation between them is
declared or implemented.  The replacement is the declaration-level chain
`policy-precision beta posterior -> machineTemperature variational arm
(tau = beta) -> softmaxWithFPi score (-G/tau = -gamma*G)` through
`converge-beta`, `carry-beta`, `effective-temperature`, and `selection-scores`
(`policy_precision.clj:95-180,497-560`; `policy.clj:77-146,157-219`).

The finite-support softmax normalization/alignment and positive-real beta
results recorded in `WORK-REMAINING.md` establish the chain under their stated
finite-field/exact-real assumptions.  They do not establish global solver
uniqueness, floating correspondence, a qualifying production run, or any
interoceptive influence.  Engineering modes `:spread` and
`:selection-gain-only` are not substitutes because neither implements
`gamma = 1/beta`.  Control: at one input where all three temperature laws are
distinct, only `:variational-beta-gamma` may satisfy the beta/gamma identity.

## Ordered implementation packets

1. **E1 mapping:** versioned R6 ranked-candidate to R11 proposal adapter,
   complete accounting and commissioned reorder/drop/cost controls.
2. **E2a portfolio restriction:** make R11 approval constrain the R6 domain.
3. **E2b exact enactment:** join selected and approved identity to R16 and
   refuse first-passing substitution.  Do not combine this with E2a review.
4. **E3 pre-enact independence:** insert the exact-candidate R9 verifier after
   selection and before enactment.
5. **E4 scheduled route evidence:** add the dispatch-to-tick identity record,
   then retain the existing R2/predecessor-to-R8 join.  No new R8 mathematics.
6. **E5 prior shaping:** integrate the existing pure transform before R6 while
   retaining R13 independently.
7. **E6b feedback:** persist one witnessed outcome into the next slow state;
   then commission E6a end to end over E5/E2/E3.
8. **Ledger/figure adoption:** only after independent reviews, replace the
   conflated strokes and assign fired/conditional/aspirational status from real
   evidence.  This packet does not authorize that edit.

Row 18's controller lease deployment and historical logical refusal remain
open and are outside these packets.  Nothing here qualifies that work, changes
gamma, or authorizes a node admission.
