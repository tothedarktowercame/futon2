# TN: close-retention operand discovery (2026-09-14)

Status: **read-only discovery**. No source, test, record, schema, or runtime was
changed. This note locates what the live full-loop tick actually has when it
closes an attempt; it does not create a label or measured-A pair.

## Pins

| File | Lines | SHA-256 |
|---|---:|---|
| `runs/row-14-measured-a-validator-2026-09-14/SPEC-measured-a-annotation-v1.md` | 10-38 | `6815b48da949ee9b3b1aa87b5f4221f5852602910ac241ffcbc39454810522ec` |
| `src/futon2/aif/full_loop_runner.clj` | 222-260, 593-596, 2756-3058, 3157-3402, 3555-3889 | `062cdbcce6d184d46029fc560c84d80ec6c0310ab73fbffc68e8b2d195ce32cc` |
| `src/futon2/aif/full_loop_cohort.clj` | 184-230, 317-325, 382-425, 442-460 | `f36bffb4e081df76d925ba8d2b2501d0e95190845e7c81934a052f857bed72cf` |
| `src/futon2/aif/trace.clj` | 570-601, 865-919 | `858a4ae3a837fa4755da1c307732bd4b7b887eff3e04bd2d97e139cc70afb356` |
| `src/futon2/aif/machine_model.clj` | 1-19, 150-181 | `203d1fb6942a898bfbbe05d174fd9deecb94e1bcd5dc2901a19a9fc46c15b6a1` |
| `src/futon2/aif/measured_a_annotation.clj` | 80-144 | `6bbf9dff4b35253031e566f5b166e2e454f8a89c793f8948b2dd75e8f3205a85` |

Inspected at futon2 `f7e156c7a5db1c67c363527db98ca43bf1ae248c`.
The governing validator requires an exact closed subject with state time and
model revision, plus `:post-action-at-close`, transition/action ids and action
time, disposition/close time, and evidence cutoff
(`SPEC-measured-a-annotation-v1.md:15-23`). Its law is
`action-at <= state-at <= closed-at <= evidence-cutoff`, and disposition or a
model posterior may not supply the label (`:34-38`).

## Tick and close dataflow

`run-opportunity-core!` creates `phase-events`, `checkpoints`, and
`selected-entity-belief` atoms (`full_loop_runner.clj:2756-2776`). It starts a
cohort attempt and receives the attempt id at `:2819-2832`. Every checkpoint is
durably appended before being installed in the in-memory checkpoint map
(`:2833-2843`). Selection puts the selected target, the judgment's **current
selection belief**, and run id in `selected-entity-belief` (`:3223-3229`).

Construction writes the WM trace and then persists selection and construction
checkpoints (`:3350-3402`). Subsequent author dispatch, build/review, grounding,
and adjudication may take place (`:3555-3579,3659-3879`). None updates
`selected-entity-belief`. `close!` then reads that atom, creates
`:entity-state-at-close`, creates the close term, and calls `close-attempt!`
(`:3008-3052`). The cohort writer validates prerequisites and delegates to
`append-checkpoint!` (`full_loop_cohort.clj:442-460`), whose `event-record`
creates the outer closed event's `:recorded-at` (`:317-325`).

That sequence matters: the existing field called `entity-state-at-close` is an
in-hand selection snapshot, not an observation made after the selected action.
Its function explicitly calls the belief row the observation and derives only
an argmax (`full_loop_runner.clj:2458-2496`); the annotation contract forbids a
model posterior as label source.

## Per-operand verdicts

| Required operand | Verdict at `close-attempt!` | Exact finding |
|---|---|---|
| post-action categorical state / `state-at` | **Never minted** | The only belief in hand is copied from `judgement` during selection (`:3223-3229`) and is never refreshed after construction, dispatch, grounding, or adjudication. `close-state` merely timestamps reading that old map (`:3010-3016`). Neither its row nor its argmax is independently observed categorical state. |
| transition id | **Never minted** | Phase telemetry has keywords `:phase` and `:transition :start/:end` (`:240-260`), but no stable transition identity. Cohort `:event/sequence`, attempt id, execution identity, and click id identify other objects; coercing any of them would violate the explicit `:transition/id` field in the conditioning subject (`SPEC:17-19`; validator source contract reflected at `measured_a_annotation.clj:103-115`). |
| action id | **Never minted** | The action value is in the selection cell (`full_loop_runner.clj:3236-3269`) and repeated in construction's selection-enaction record (`:3375-3386`), but neither has an occurrence id. Attempt, job, click, candidate, and selection-entry identities are not an action occurrence id. |
| action time | **Never minted as such** | Timed phase and checkpoint events exist, but no instant is bound to the action occurrence. Construction-end, trace-write, author-dispatch, grounding, and adjudication times describe different operations. Selecting one after the fact would silently choose action semantics. |
| close disposition | **In hand** | `close!` receives `outcome`; it is put directly into the closed judgment at `:3019-3029`, before `cohort/close-attempt!` at `:3049-3052`. |
| closed-at | **Minted inside the close writer, not in hand in the submitted cell** | `event-record` creates the closed checkpoint's outer `:recorded-at` (`full_loop_cohort.clj:317-325`). The returned event contains it, but `close!` ignores the return from `close-attempt!`. The inner close judgment has no `:closed-at`. |
| immutable evidence cutoff | **Never minted** | No cutoff variable or frozen evidence-set identity is threaded. `close-state :recorded-at` is a newly sampled clock value, not an evidence freeze (`full_loop_runner.clj:3010-3016`). The cohort close timestamp is sampled later inside the writer and likewise does not identify which evidence bytes were permitted. |
| model revision | **Never minted in the required model sense** | Selection receives a `:wm-version` containing futon2 git identity, resolved flags, and trace schema (`full_loop_runner.clj:3198-3208`; `trace.clj:586-595`). The time-step also retains stack code-state/configuration digest (`full_loop_runner.clj:2798-2817`). Both are reachable through in-memory checkpoints at close, but neither is a declared machine `:model/revision`. The machine-model module has its own declared model authority; no loader result or model revision is threaded into this close path. Code SHA, cohort pin, and config digest must not be renamed model revision. |

The entity id, run id, cohort id, attempt id, and `:closed` checkpoint identity
do exist: entity/run are in `selected-entity-belief`; cohort/attempt/sequence are
created by the cohort event (`full_loop_cohort.clj:317-325`). They do not repair
the missing semantic operands above.

## What minting would require

### Transition and action identity

At the point the selected action becomes the action the runner will enact—after
selection discrimination and before construction begins—the runner would have
to mint a versioned occurrence subject containing run, cohort, attempt,
transition id, action id, exact action value, and action time. The same subject
would then have to be carried unchanged through the construction
selection-enaction record, trace, later checkpoints, and close. Minting at close
would be retrospective and cannot prove which earlier occurrence was enacted.

No existing id is an exact substitute:

* click id identifies a whole runner invocation;
* attempt id identifies a whole full-loop attempt;
* cohort event sequence identifies a checkpoint record;
* Agency job id identifies an author/reviewer dispatch;
* a selected action map is a value and can recur, so it is not an occurrence.

The spec independently requires transition and action ids (`SPEC:17-19`), and
the validator requires both to be nonblank (`measured_a_annotation.clj:103-115`).
Reusing one of those ids without a declared equality would be coercion.

### Post-action state

An independent state observation must be acquired after the identified action
and before closure, with entity/run/attempt/action subject and observation time.
The current grounding witness records substrate before/after and whether a dial
moved (`full_loop_runner.clj:3826-3879`), but it is not a seven-state categorical
observation. The selection belief cannot be relabeled post-action, and the close
disposition cannot select the state (`SPEC:34-38`). If the action has no state
observer, the correct retained value is typed absence.

### Model revision

The action occurrence and state observation must name the exact declared model
snapshot/revision that generated or conditions them. A later packet must either
thread the already-loaded model identity from the actual judge/model loader or
retain typed absence. The synchronously reachable `:wm-version`, code-state,
configuration digest, and execution-cohort pin are useful provenance but are not
that identity absent a reviewed equivalence.

## Evidence-cutoff candidates (not a ruling)

These are the clock points already present or naturally capturable between
selection and close. None currently freezes an evidence set.

1. **Selection phase end.** `run-phase!` emits an end event immediately after
   the selection thunk (`full_loop_runner.clj:240-254,3157-3165`). Earliest and
   outcome-independent, but pre-action and therefore cannot satisfy a cutoff
   after `closed-at`.
2. **Trace/construction boundary.** The trace is appended after construction
   succeeds, then selection and construction checkpoints are written
   (`:3350-3402`; `trace.clj:891-919`). It freezes useful selection bytes, but
   precedes author/reviewer action and close.
3. **Action/grounding completion.** The grounding phase end and adjudication
   checkpoint occur after the production effect (`full_loop_runner.clj:3826-3879`).
   This is the strongest existing post-action boundary for grounded changes,
   but is absent on early failure paths and still lacks a categorical observer.
4. **`close!` entry.** Outcome and data have already been chosen when callers
   invoke `close!` (`:3880-3889`, with failures routed at `:3936`). A clock here
   can freeze inputs before close assembly, but is disposition-aware and would
   be earlier than the actual closed event timestamp.
5. **Close-state clock sample.** `Instant/now` at `:3010-3016` is before the
   closed term and durable append. It could time a state observation only if
   that observation were actually acquired there; today it times rereading a
   selection belief. It cannot also be a cutoff satisfying `closed-at <= cutoff`
   when the writer closes later.
6. **Cohort closed-event `:recorded-at`.** Created atomically with the outer
   closed event (`full_loop_cohort.clj:317-325,421-425`). It is the best existing
   candidate for `closed-at`. Using the same instant as cutoff (equality is
   permitted) would require the writer to freeze and bind the permitted evidence
   bytes at that instant; today it does not.
7. **Immediately after successful close append.** The returned closed event
   could supply its writer-generated timestamp and prove persistence, but the
   runner currently discards that return. A later clock sampled after return is
   post-hoc and mutable unless it is part of a new immutable joined record.

The design packet must decide whether cutoff means an evidence-set freeze before
disposition reasoning or a closure-time upper bound. The existing temporal law
places cutoff at or after `closed-at`, while outcome-leakage discipline requires
the state evidence itself to have been frozen independently before disposition;
those are separate timestamps and should not be collapsed silently.

## Hazards

* Reading a daily trace or belief store after closure would be mutable temporal
  reconstruction. The runner's own comment already forbids substituting a later
  or nearest trace row (`full_loop_runner.clj:2458-2465`).
* The daemon runner can continue through long author/reviewer phases after the
  selection snapshot. Treating the atom's unchanged value as a fresh close-time
  observation is stale-by-construction, not race safety.
* Sampling timestamps separately in `close!` and `event-record` can invert an
  authored temporal story unless the exact returned event supplies `closed-at`.
* Trace append and cohort checkpoint append are separate writes. Their paths and
  timestamps do not establish a common occurrence without the new action subject.
* Model code identity is sampled from a working tree and `wm-version-stamp`
  documents that a long-running JVM can predate it (`trace.clj:570-584`). It is
  not safe to reconstruct loaded model revision from repository HEAD at close.

## Smallest implementation boundary exposed by this discovery

Before adding fields to `close-attempt!`, a design anchor must specify (1) the
action/transition occurrence minted before construction, (2) the independent
post-action categorical observation port and typed-absence behavior, (3) the
declared model snapshot identity, and (4) whether the closed event's single
writer timestamp also serves as cutoff or points to a separately frozen evidence
set. Only then can the runner carry those immutable operands and the cohort
writer retain them. Adding timestamps or renaming existing ids now would produce
a well-shaped but unlicensed annotation subject.
