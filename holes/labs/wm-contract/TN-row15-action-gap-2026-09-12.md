# TN row 15: action-proof gap at schema 28

Read-only discovery at futon2 HEAD `0b90518e` and mathlib4 HEAD on
2026-09-12. No production source or live trace was changed.

## Binding and required values

The registry binds `:id :action` to `:lean "machineAction"` at
`holes/labs/wm-contract/aif-equations.edn:183-190`. The Lean carrier is not
simply `argmax Q(pi)`: `DarkTower/WarMachine/MachineAction.lean:49-57` takes:

- selection boundary (`strategicRecommendation` or `actuation`);
- requested strategic law (`controllerHead` or `fullScorePosterior`);
- `fPiEntered` and `anyHabitPrior` booleans;
- an ordered `ranked` candidate list and an ordered `scored` list, each member
  carrying candidate identity, score and `noOp`.

The branches need different evidence. Controller-head needs candidate order,
no-op classification and the selected result. Full-score selection needs the
score order and its first-max tie rule. Habit actuation needs whether any habit
prior is nonzero, its scored list and the last-max tie rule. The abstain claim
additionally needs the no-op and chosen G values plus epsilon
(`MachineAction.lean:196-207`). Reconstructing the score/posterior itself needs
per candidate G, ln E, F_pi, tau and the partitioned softmax result
(`MachineAction.lean:93-127`).

## Schema-28 retention, field by field

With policy details on, the following is retained:

| Needed input | Schema-28 carrier | Verdict |
|---|---|---|
| Candidate identity and ordered candidate set | complete `:action` plus `:rank` in every stripped ranked action, `src/futon2/aif/trace.clj:158-171` | retained; rank is a tick-local join key and action is the durable identity |
| Per-candidate G/controller score | `:G-risk`, `:G-ambiguity`, `:G-core`, `:G-efe`, `:controller-score` in `trace.clj:158-171` | retained |
| Tau | decision survives `strip-decision`, which removes only two keys, `trace.clj:207-219`; decision `:tau` is therefore retained | retained once per tick |
| Habit/log-prior | ranked `:habit-prior-bias` and `:habit-prior-source`, `trace.clj:168-171`; carried sufficient-statistic `:habit-prior-state`, `trace.clj:744-747` | ln E used per candidate is retained; the source state is retained |
| Exact combined selection score | decision's `:habit-adjusted-ranking` survives the two-key dissociation at `trace.clj:207-219` and carries action/rank/G/lnE/selection-score | retained for the habit counterfactual; ranked action rows alone do not carry `:selection-score` |
| F_pi entered and values | decision `:f-pi-posterior` survives `strip-decision`; top-level `:f-pi-by-candidate-id` and provenance are present-only at `trace.clj:763-768` | retained when the producer emits them; explicit absent status when flag off |
| Selection boundary, requested/applied law, result | decision is retained at `trace.clj:625-627`; `strip-decision` keeps `:selection-boundary`, `:selection-law`, `:action`, rank and score (`trace.clj:207-219`) | retained |
| No-op comparison | full action maps identify `:no-op`; decision retains `:no-op-comparison` through the same strip | retained except epsilon |
| Abstain epsilon | no trace field records the configured/constant epsilon used by the decision | **omitted**; source reconstruction is not an identical runtime pin |
| Policy exclusions | top-level `:policy-support-exclusions`, `trace.clj:624-625` | retained, including reasons, but these describe the pre-score domain rather than the strategic selector's later subset |
| Selection-gain input/state | top-level `:selection-gain`, `trace.clj:600-603`, plus decision tau/source | retained |
| Q(pi) | details-on `:softmax-weights-by-candidate-id`, constructed at `trace.clj:197-219` | retained |
| Horizon/depth | ranked `:horizon-steps`, `trace.clj:171`; top-level present-only input/effective depth at `trace.clj:628-633` | schema 28 supports it; presence still depends on the post-row-15 producer |
| Machine Q(o\|pi)/C | complete present-only `:machine-q`, validated and retained at `trace.clj:84-114,126-173` | retained only once row-14 live wiring supplies it |

The accumulation envelope is irrelevant to action selection and is not needed.

## Real-record readback

I parsed, read-only, the latest record after 11:29 in
`data/wm-trace/wm-trace-2026-09-12.edn`: timestamp
`2026-09-12T17:28:09.498448087Z`. The file contains four records, all after
11:29. This record identifies its producer as schema **27**, not 28: it was
written before the reviewed schema-28 reload, despite the current file mtime.
It nevertheless has policy details on:

- 148 ranked actions and 148 `rank/N` softmax entries; their rank-key sets are
  exactly equal, and all 148 retained action maps are distinct;
- per-row action, rank, controller score, G terms, habit-prior bias/source,
  predicted mean/variance/status and `:horizon-steps` (nil per row);
- decision action, tau/source, selection law, boundary, no-op comparison,
  `:habit-adjusted-ranking`, controller ranking and the rank-keyed posterior;
- top-level habit state, selection gain, four policy-support exclusions and an
  explicit F_pi `:status :absent`;
- no top-level row-15 depth fields and zero `:machine-q` rows, as expected for a
  pre-schema-28/pre-live-wiring record.

The record also exposes a semantic, not capture, issue. Its requested/applied
law is `:controller-head`, whose internal controller head is rank 1, but the
reason-bearing live selector chose rank 139; the record explicitly says
`:consulted-ranking :live-selector-id` and `:moved-from-controller-head? true`.
Thus this live decision is not an instance of Lean `machineAction`'s
controller-head branch. More fields cannot turn that disagreement into a
positive correspondence proof; a measurement must retain it as a typed
divergence or the Lean carrier must be extended to the live selector boundary.

## R6 softmax reconstruction

Rank re-keying does **not** lose candidate identity within one record. The
writer derives `rank/N` from each ranked action (`trace.clj:190-205`), while the
same stripped entry retains that rank and the complete action. The observed
record has a bijection: 148 rank keys, 148 ranked actions, equal key sets, and
distinct action maps. Joining `softmax-weights-by-candidate-id["rank/N"]` to
the ranked entry with rank N reconstructs the original action-keyed posterior
exactly for that tick. This is stronger than merely preserving posterior order.

What is not yet enforced at the trace boundary is that every ranked action has
exactly one weight and no extra weight; `stringable-softmax-weights` silently
uses `keep` at `trace.clj:197-205`. The real record is complete, but a general
measurement proof cannot infer completeness from the schema. Also, an action
map is the policy identity used by `softmax-weights`; there is no separate
versioned policy id unless the action itself carries one. The rank join is
lossless for exact action-key reconstruction, but not a substitute for a
cross-tick policy identity.

## Verdicts and minimal packets

**R6 softmax posterior: needs one additive boundary check, then provable from
retained pins.** Add one behavior in `trace.clj`: when policy details are on,
refuse before append unless rank keys are unique and the softmax rank-key set
equals the ranked-action rank-key set. Retain the current maps unchanged. Its
witness mutates a missing, extra and duplicate rank and proves no append; a
real readback reconstructs all action-keyed weights exactly.

**Machine action, internal `policy/select-action` branches: almost provable, but
needs additive capture of `:abstain-epsilon` (or a versioned selection-options
record containing it).** The single behavior belongs on the decision producer,
then passes present-only through the existing decision strip. The witness must
cover controller-head, full-score first-max, habit last-max, no-op abstain and
requested-posterior-with-F_pi-absent, retaining ties rather than avoiding them.

**Machine action as the actual live selected action: not positively provable
against the current Lean declaration.** Existing pins already suffice to show
the counterexample: controller head rank 1 versus live selected rank 139. The
next proof packet should record `:diverged-from-machineAction` with both values;
the positive path requires a separately reviewed Lean extension for the
reason-bearing selector (or an explicit narrowing to the internal policy
decision, which Row 14's “no facade” discipline warns against). This is not a
request for more capture.

A fresh post-reload schema-28 tick is still required as the production pin for
any proof relying on the new depth or machine-Q fields. The inspected live
record proves details-on retention, but it must not be relabelled schema 28.
