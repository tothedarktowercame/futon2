# Item 19b: realized-outcome recording draft — 2026-09-09

Author: codex-12. **Draft for independent review and Joe's adoption before
RUN4 starts.** No runtime change, schema adoption, run authorization or registry
write is made here. Source pins: C-19-source-pins-2026-09-09.json.

## Joe's direction, verbatim

> So yes, I can accept C as a family of preference distributions. The family
> view is my answer. Yes. The one thing I would add to that is that with...
> Codex 12, we were developing an Ostrom interpretation of that family, but
> that doesn't change the ruling. I would say that... The work with Codex 12
> is definitive here, but So my ruling should be checked off against the work
> we were just doing there. But basically I agree with your suggestion. Okay,
> and as for realized outcome schema, we can also pass that to Codex 12. And
> draft. The schema, that's fine. I don't agree that E should be off. Or... I
> think you should be on, and we should pass that task to Codex 17. I can
> agree with your suggestion for Item 5 that the acceptance inspection is the
> retrospective for Run 4. That's okay. Go. Let's deal with that Block A set
> of issues now and come back to the Block B and Block C. And D later.

The delegation is to draft. Item 19b records adoption as its own reviewed step.
The design below extends the existing record instead of creating separate
competing outcome records for calibration, checkpoints and C.

## What exists, and what it does not guarantee

`src/futon2/aif/realized_outcome.clj` defines `:wm/realized-outcome-v1`, numeric
vocabulary accessors, historical normalization and categorical lookup. Its
docstring lists schema, policy, tick, expected-score, realized-score, outcome
and scale. Its actual `conforms?` requires the schema tag, two numeric legs,
and presence of policy/tick keys; it checks neither scale nor category, nor
non-nil identity values. It is not a complete recording-contract validator.
`normalize` likewise adds a schema tag to readable numeric legs without
establishing the missing semantic facts.

The producers are materially different:

| Producer | Present record | Draft obligation |
|---|---|---|
| fold_realized/realized-outcome-of | Policy/tick and coverage-score legs, possibly nil; no schema, scale or categorical field in its returned map | Wrap with explicit coverage scale and observation status; never invent a category or convert absent enactment into measured zero. |
| fold_realized/realized-outcome-grounded | Endpoint-count scale, substrate dial/snapshot and expected-source; may use a perfection-target fallback; no schema/category in its returned map | Preserve dial evidence and expected-source; a target is not a predictive measurement. |
| holes/labs/wm-contract/wm_step_observe.bb | Explicit v1 schema, g-core legs, category and outcome basis, joined to the earlier accepted run | Preserve its accepted-step window and mission-specific attribution; re-evaluated G-core is a scoring readback, not automatically realized utility or terminal disposition. |

`trace/trace-record` persists the enactor map as supplied. `selection-gain/
fold-realized-outcome` currently admits readable numeric legs and deduplicates
by the last tick; it does not enforce matching measurement contracts or the
stronger evidence requirements below. Categorical lookup serves the non-progress
walk and tripwire; historical numeric accessibility does not reconstruct absent
categories. These findings qualify “one-schema-three-vocabularies-repaired”:
the spelling/accessor repair exists; complete producer conformance does not
follow from that repair. This is source inspection, not a new live-path audit.

## Proposed single envelope and recording contract

Retain `:schema :wm/realized-outcome-v1` and the canonical numeric names.
Add `:recording-contract :wm/realized-recording-v1` for the stronger capture
contract. Old records remain readable as legacy partial evidence and do not
acquire this marker through normalization. The marker is proposed, not an
already implemented validator or a change to the existing `conforms?` meaning.

Every new conforming envelope has the following fields. A missing measurement
is represented explicitly, so recording a real but unmeasured attempt does not
require fabricating values.

| Field | Required meaning |
|---|---|
| `:record/id`, `:revision`, `:supersedes` | Immutable record identity/version; first revision has nil supersedes, correction points to its predecessor. Preserve the original observation. |
| `:run/id`, `:decision/ref`, `:attempt/id`, `:policy`, `:tick` | Exact selected decision and execution identity. Tick retains its legacy enactment meaning and is not globally unique. No selection uses an explicit not-applicable policy status rather than an invented policy. |
| `:window` | Decision time, enactment/start time, observation cutoff and observed-at time with clock basis; unavailable times explicitly unknown. Name the next-accepted-step or other observation rule and pin its version. |
| `:subject` | Mission/artifact identity and before/after revisions; distinguish the action's subject from unrelated repository activity. |
| `:execution` | Whether selected, dispatched, attempted, completed, refused or unknown, with receipt refs. Observation completion does not itself mean execution completed. |
| `:expected-score`, `:realized-score`, `:scale`, `:measurement` | Preserve numeric projections, nil when unavailable. Measurement pins quantity, units, sign convention, computation version, expected-source, both sampling windows and each leg's status/evidence. Equal units alone are insufficient when algorithms or quantities differ. |
| `:outcome`, `:classification` | A known category uses the existing cohort vocabulary; otherwise outcome is nil and classification states unknown/inadmissible/not-applicable. Classification also pins classifier, basis, grain (step/attempt/mission) and evidence. Category is not derived merely from the sign of a score. |
| `:closure` | Open/closed state, close checkpoint, judgment and reviewer refs when closed. An observed next-step category does not imply a terminal attempt disposition. |
| `:observations` | Versioned checkpoint and channel observations, with event identities, times and declared domains; see below. |
| `:preferences` | Pinned module/context, criterion readings and assessments with provenance; see below. |
| `:evidence` | Reference table: each ref resolves to repo/path/revision or content digest, locator, capture/adapter version and actor where known. Preserve raw receipts or immutable pointers. |
| `:review` | Proposed/reviewed state, reviewer identity, review scope and evidence; review of a reading does not adopt a preference or amend a rule. |

An observation wrapper has status observed, unknown, inadmissible or
not-applicable. Only observed carries a value in its declared domain. Unknown
names missing evidence and how to obtain it; inadmissible names failed warrant;
not-applicable names the unmet scope condition. Adapter execution failure is an
error with its evidence, not a negative observation. Absence, checked-empty and
observed zero remain distinct. These rules extend DERIVE's status contract.

## Record channels alongside checkpoints

Choose paired capture as this draft's recording design. Keep the checkpoint
sequence in its native vocabulary, with event receipts/timestamps alongside it.
Keep actual observed channels in a separate domain-tagged field, at pre-action
and post-action windows where available, retaining raw snapshot refs and the
observation adapter version. Each channel retains its own status and units;
one missing channel does not turn the other readings into zeros. The 14-channel
observation domain and 13-channel C_int preference domain are not equated.

Associate samples by attempt/decision identity and explicit temporal alignment,
not by list position or nearest timestamp without an alignment rule. Preserve
late arrivals and partial windows as such. When a before sample was not taken,
record unknown; do not retrospectively label an after sample as before.

`checks/disposition_kernel/observation-summary` currently consumes ordered
checkpoints. A future cohort adapter may project the native checkpoint sequence
only for records with compatible checkpoint definitions and genuinely closed,
reviewed attempt dispositions. Step classifications and C readings do not enter
that fit as substitute attempt outcomes. Record exclusions and reasons.

Paired capture supplies evidence for a model; **it does not supply the model**.
A future mapping record must name source and target domains/versions, direction,
function or stochastic kernel, support/uncertainty assumptions, training evidence,
validation and temporal availability. It must exclude outcome/future leakage.
A terminal `:closed` checkpoint can be retrospective conditioning evidence but
cannot be silently supplied as a pre-action observation. Until admitted, the
mapping is unresolved and §1b remains open. The required predictive calculation
remains the marginal sum_o Q(o|policy)P(d|o) on compatible domains, not evaluation
at a channel mean. No smoothing or synthetic coverage is introduced.

## Preference readings and institutional observations

`:preferences` pins the module id/version/context and source digest, and records
three separate things: decision-time predicted readings (or unknown), realized
readings, and the derived assessment. A predicted reading pins the forecast
model, decision horizon, declared outcome domain and its pre-decision receipt.
It stays a prediction even after the run. A realized reading does not acquire
`:status :predicted` so that a ranker can accept it.

The realized readings map is keyed by preference entry id and projects directly
to `preference-module/assess`: `:version`, `:criterion`, `:status`, `:evidence`,
plus Boolean `:value` for observed soft-binary entries, or the exact tagged
`:distribution` for observed finite entries. For a single witnessed finite
outcome that distribution is its point mass, explicitly empirical. Non-observed
readings carry `:reason` and no value/distribution. Evidence is a named ref
(string or keyword), as the current API requires; the envelope's reference
table supplies its substantive resolution. A recorded assessment also pins
assessor version and candidate/accepted mode; it is derived, not raw evidence.

Process readings additionally pin mission, lifecycle phase, occurrence and
transition. Keep separate assessments for repeated VERIFY/DERIVE occurrences;
do not overwrite them in an entry-id map or turn phase ordinals into progress.
Use one entry-id readings map per pinned occurrence. C_tau remains a declared
occurrence index, not an implemented time forecast or discounted sum.

If recording institutional facts, keep declared rule/version, actual enforcement,
observed compliance and the justifying preference as distinct referenced facts.
No new InstitutionSpec adoption is implied. A review label is not proof of
compliance, and an absent feedback carrier yields unknown, not “no feedback.”
The known pair “feedback delivered” / “feedback delivered: false” must either
be distinguished by a new adapter or explicitly refused as insufficient.

## Consumer admission and backwards compatibility

- Calibration consumes only an explicitly admissible pair for the same quantity,
  scale, method and aligned window, with warranted observed realization and a
  prediction source. A perfection target or re-evaluation must remain labelled
  as such and cannot silently masquerade as a calibrated forecast. This is a
  proposed stronger adapter gate, not a claim about the current gain reader.
- Category consumers use an observed classification with its declared grain;
  unknown is not a no-progress event. A terminal-disposition consumer additionally
  requires closed-attempt evidence and review. Do not rewrite historic categories.
- C diagnostics consume realized readings; local action ranking consumes valid
  forecasts. Missing readings remain unknown. No registry mutation or preference
  strength follows from a successful assessment.
- Corrections and replay must not create duplicate learning samples. Deduplicate
  using run/decision/attempt/measurement identity and revision, not tick alone.
  A correction to a consumed sample needs an explicit replay/retraction policy;
  it is not a second independent sample. This requires a reader change before
  this contract can claim that guarantee.
- Historical July legs remain readable under their marked vocabulary; preserve
  their original bytes. Missing scale, category, observation or warrant remains
  missing. Do not fabricate a complete modern envelope through normalization.

## Review and adoption boundary before RUN4

Joe can adopt this paired-capture contract after independent review, including
its explicit unknowns and stronger consumer admission. Adoption must name the
run's actual producer, sampling window, attempt framing and disposition reviewer.
This draft does not settle those run-specific choices by inventing them.

After adoption, an implementation packet must enumerate and update the three
producer paths and their actual consumers, add the stronger validator/adapter,
and demonstrate a producer-to-persisted-record-to-consumer replay. It must test:
missing channel versus observed zero; unchanged numerical error with warrant
removed; feedback denial; unequal scales or forecast/target confusion; unknown
category versus no-progress; step outcome versus closed disposition; same tick
in different runs; correction replay; and observed-versus-predicted C inputs.
At least one fixture must follow an actual recorded run's shape. Old-reader
replay should retain readable history without asserting new admissibility.

Schema review/adoption and implementation conformance are distinct gates. No
production observation-model closure, cohort restart, run readiness or
end-to-end Lean refinement is asserted by writing this document. The existing
C-module type proof remains applicable to admitted preference distributions;
this envelope's IO and observation adapters require their own verification.

## Checks performed on this draft

A standalone `bb -cp src` probe called the existing `ro/conforms?` with numeric
legs, nil policy/tick, and neither scale nor outcome: it returned true (exit 0),
confirming the validator discrepancy above. All 18 source digests and the full
Item 19 quotation in both notes were checked programmatically (exit 0). These
are source/provenance checks, not tests of an implemented recording extension.
