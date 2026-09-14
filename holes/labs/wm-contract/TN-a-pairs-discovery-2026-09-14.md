# a-pairs retained-close discovery — 2026-09-14

Status: **read-only discovery**.  This note creates no join, label, count
offered as licensed coverage, or A matrix.  Counts below are a filesystem
census of retained records at futon2 tree
`7370f5fca22c2a85e306b60542b2494ff68defe4`; reconstruction is always called
reconstruction, not retention.

## Result

There are **three retained record kinds which bind a close**: the cohort event
record (the authoritative `:closed` subtype and its same-attempt sibling
events), the tick-run receipt which points to a runner execution, and the
morning-brief item named by a close.  Only the first is the close itself.
The corpus has 86 physical close files: 58 in the current canonical root, 24
in its archive, and four redirected machinery closes.  The archive is not 24
additional unambiguous observations: it reuses the same cohort/attempt keys as
the current root, with 23 conflicting identities and one byte-identical copy.

No retained close supplies a complete a-pair.  The four newest machinery
closes retain entity, run and a typed **absent** state-at-close; older closes
do not retain a state-at-close object at all.  No close retains the required
immutable evidence cutoff, post-action-at-close point, transition id, action
id, or model revision.  The close disposition is retained on all 86 event
files and belongs to the ruled twelve-outcome support, but that fact supplies
only the outcome arm.

## 1. Where closes live and what binds them

### K1 — append-only cohort event files (the close authority)

The event schema is written at `src/futon2/aif/full_loop_cohort.clj:317-325`:
`{:event/schema-version :cohort/id :attempt/id :attempt/ordinal
:event/sequence :checkpoint/type :recorded-at :payload}`.  The fixed sequence
is declared at lines 23-24 and `close-attempt!` validates the preceding
checkpoints and appends `:closed` at lines 442-460.  `write-new!` is used by
the append at lines 421-426; the intended store semantics are stated as
immutable events, not an authoritative mutable summary, at lines 1-6.

Physical roots at census time:

| root | physical `*-closed.edn` | close-time range |
|---|---:|---|
| `data/wm-full-loop/wm-outer-loop-*/attempt-*/007-closed.edn` | 58 | 2026-07-14T20:27:28.840534153Z — 2026-07-27T10:59:53.609069187Z |
| `data/wm-full-loop/archives/stop-line-2026-07-15/wm-outer-loop-40-v1/attempt-*/007-closed.edn` | 24 | 2026-07-14T20:27:28.840534153Z — 2026-07-15T19:00:19.086830269Z |
| `data/wm-full-loop-machinery-47/.../attempt-*/007-closed.edn` | 2 | 2026-09-13T17:54:43.219095652Z — 2026-09-13T22:22:47.252590249Z |
| `data/wm-full-loop-machinery-48/.../attempt-*/007-closed.edn` | 1 | 2026-09-13T22:39:29.054525008Z |
| `data/wm-full-loop-machinery-49/.../attempt-*/007-closed.edn` | 1 | 2026-09-14T03:35:02.169966008Z |
| `data/wm-full-loop-canary` | 0 | absent |

The r8 run receipt points through
`:runner-execution/provenance` to cohort
`:wm-contract-machinery-49-v1`, attempt `attempt-002`; its authoritative close
is `data/wm-full-loop-machinery-49/wm-contract-machinery-49-v1/attempt-002/007-closed.edn`.
The close is SHA-256
`6fcbfe85954ca8994a080025d78385684c1d3549719d9ed48598c53578852352`.
Its sibling `001-time-step` through `006-adjudication` records bind by exact
`:cohort/id`, `:attempt/id`, ordinal and event sequence.  They are the same K1
schema, not separate close authorities.

### K2 — tick-run receipts (close pointers/projections)

`holes/labs/wm-contract/tick-run-record-*.edn` contains 152 files at census
time.  Eighteen carry a `:runner-execution/identity`, but only the four named
machinery receipts r3, r4, r5 and r8 join to the four redirected close files
above.  The other fourteen were short isolated/preflight executions and no
matching durable cohort close was found; they are not silently counted as
closes.  The r8 receipt is
`holes/labs/wm-contract/tick-run-record-wm-machinery-test-2026-09-14-claude-15-r8.edn`,
SHA-256 `5cf73cde9904437c4101e2643856a30d3926948990dece1e186f24620dcdf501`.
It retains run id, click id, start time, terminal projection, cohort identity
and runner-execution identity.  It does not retain the close timestamp,
checkpoint sequence, cutoff, state-at-close object, model revision, or a
post-action observation point; those require following the pointer to K1.

### K3 — morning-brief items (close-referenced delivery projection)

Every one of the four machinery closes has a `:morning-brief-ref`; all four
referenced files presently exist below `data/wm-morning-brief/items/`.  These
items carry the external execution attempt id, selected target, outcome,
queued/evidence times, author/reviewer and delivery material.  They neither
identify the close checkpoint nor carry the state/cutoff/model fields and are
not close authority.  The r8 item is SHA-256
`ffd0f1aa8436d7d7e975247ef536fb54b175490f836e254bd94b175dd4a35853`.

`full_loop_cohort/attempt-summary` (`full_loop_cohort.clj:462-484`) and
`ledger` are derived readers, not retained fourth record kinds.  Likewise the
`:wm/realized-recording-v1` envelope describes step-level realized outcomes;
its legacy adapter explicitly marks closure unknown and checkpoint events not
recorded (`realized_recording.clj:271-303`).  It does not bind these closes.

## 2. Field inventory by retained kind

Legend: **yes** means literal in that kind; **pointer** means another retained
record must be read; **absent** means not retained.  A value inferable from a
neighboring event is still absent from the record under examination.

| a-pairs field | K1 close event | K2 tick-run | K3 morning brief |
|---|---|---|---|
| exact close event/id | cohort + attempt + sequence, but no single `event/id` | pointer for four machinery runs | absent |
| close time | `:recorded-at` | absent | absent (has other times) |
| close disposition | `[:payload :judgment :outcome]` | terminal projection for joined machinery runs | `:outcome` projection |
| immutable evidence cutoff | absent | absent | absent |
| state at close | old 82: absent; machinery four: object present but `:status :absent` | absent | absent |
| post-action-at-close point/time | absent | absent | absent |
| transition id / action id | absent; selected action data may occur in sibling selection, without either required id | absent | absent |
| model revision | absent | absent | absent |
| entity id | 41 of old 82 had a legacy target-like id in the earlier census; all four machinery closes have exact `:outcome-entity`; not universal | only inside projected terminal for machinery runs | selected target, not the close subject contract |
| run id | only the four machinery state objects, under `:belief-source :run/id` | yes | absent as a dedicated run key |
| cohort / attempt | yes | pointer/provenance for machinery | external execution attempt only |
| outcome | yes | projection | projection |

The state writer was added explicitly without promoting its argmax:
`full_loop_runner.clj:2458-2478` says the in-force belief row is the
observation and unavailable rows stay typed.  All four machinery closes are
`:status :absent, :reason :in-force-belief-row-unavailable`; therefore their
state arm is absent, not a usable seven-state value.

The production close-outcome vocabulary is the twelve values in
`full_loop_cohort.clj:26-36`:
`:grounded-change`, `:grounded-no-change`, `:artifact-only`, `:abstained`,
`:no-selection`, `:agent-unavailable`, `:guardrail-refusal`,
`:dispatch-failed`, `:build-failed`, `:substrate-unavailable`, `:incomplete`,
and `:cancelled`.  The close validator refuses values outside it at lines
428-440.  `realized_outcome.clj:21-35,67-71,108-119` distinguishes this
categorical value from numeric realized-score legs and defines the historical
locations a trace reader checks.  Every K1 close retains its categorical
outcome; that does not supply the missing state label.

## 3. Exact annotation join-key comparison

The measured-A annotation specification requires the subject to bind entity,
run, cohort, attempt, closed checkpoint, Status, state time and model revision,
and conditioning to bind post-action-at-close point, transition/action ids and
time, close disposition/time, and immutable evidence cutoff
(`SPEC-measured-a-annotation-v1.md:15-23`).  Its temporal law is at lines
34-38.

K1 always gives cohort, attempt, checkpoint type/sequence and close time.  In
the four machinery closes it additionally gives entity, run and a state-record
time, but the state is explicitly unavailable.  It lacks the spec's usable
Status, model revision, an explicit immutable close reference, action id,
transition id, action time/post-action point, and evidence cutoff.  Older
closes additionally lack run and state time, and entity is not universal.
K2 and K3 can corroborate execution and target projections but do not fill any
of those missing subject/conditioning keys.

The existing attachment code demonstrates the narrower join currently
possible: it demands cohort, attempt, event sequence, disposition time,
entity, and run equality at
`categorical_state_close_attachment.clj:56-80`.  Its discovery result refuses
missing annotation and context authority at lines 30-54.  It does not license
reconstructing the absent annotation keys from sibling records.

## 4. Volume, outcomes, and gaps

### K1 volumes

Current canonical 58 outcomes: 19 `:grounded-change`, 22 `:build-failed`,
eight `:agent-unavailable`, seven `:incomplete`, two
`:substrate-unavailable`.  Archive 24: one `:grounded-change`, 22
`:no-selection`, one `:agent-unavailable`.  Machinery four: one
`:grounded-change`, two `:build-failed`, one `:incomplete`.

The 82 pre-machinery physical files represent 81 distinct byte records, but
their apparent `(cohort, attempt)` identity is unsafe: the archive/current
trees contain 23 pairs with conflicting time/outcome/entity signatures and
one byte-identical copy.  Adding the four machinery records yields 85 distinct
byte records, not 85 licensed observations.  The previous read-only census
that measured these conflicts is retained at
`runs/row-14-measured-a-sources-2026-09-12/counts.edn`; its one posterior-
argmax pairing is expressly a derived historical diagnostic, not a licensed
state label and is not carried forward here.

### K2/K3 volumes

Four tick-run receipts and four morning-brief items join the four machinery
closes.  Their time extent is the same machinery execution window,
2026-09-13T17:48:40Z through the r8 close on 2026-09-14T03:35:02Z.  They add
execution/delivery provenance but no qualifying state, cutoff, action/transition
ids, or model revision.

Everything suggested by temporal proximity, filename layout, selected target,
or the last prior trace is **reconstructable only and therefore not retained**.
No smoothing, nearest-time match, posterior argmax, or target-name coercion
changes these absences.

## 5. Hazards for a future join

1. **Identity collision across roots.**  `(cohort/id, attempt/id)` is not
   globally unique across the current and archive roots: 23 reused identities
   disagree.  A source-root plus byte pin is necessary, and conflicting copies
   must refuse rather than be merged.
2. **Mutable location versus immutable event creation.**  The writer creates a
   new event file and will not overwrite it, but files can later be moved or
   copied at the filesystem/repository level (the archive proves this).  An
   eventual join needs literal bytes, SHA-256, source root and revision; a path
   alone is not an immutable cutoff.
3. **Duplicate/checkpoint ambiguity.**  The event reader sorts matching files
   (`full_loop_cohort.clj:270-274`) and the closed-execution reader requires the
   exact seven filenames and sequences (`:620-662`).  A looser glob/count join
   would miss duplicates, partial attempts and the conflicting archive.
4. **Clock law cannot be established.**  For r8, retained state-record time
   `2026-09-14T03:35:02.153610930Z` precedes close time
   `2026-09-14T03:35:02.169966008Z`; this checks only one middle inequality.
   Action-at and evidence-cutoff are absent, so
   `action-at <= state-at <= closed-at <= evidence-cutoff` cannot be tested.
   No violation was observed because three required operands are not all
   retained; absence is the finding.
5. **Outcome is not state.**  Close disposition is validated against the
   twelve-value support.  Treating it, a selected target, a belief argmax, or
   a morning-brief judgment as the seven-state annotation would make the
   system label itself and violate the annotation contract.

The largest blocker is therefore not volume: **zero closes retain an
independently reviewed seven-state Status at the exact close together with the
immutable evidence cutoff and post-action identity/time required to license
the pair.**

## Reproducible byte pins

All hashes are SHA-256 over the cited file bytes, recomputed during this
discovery:

| bytes | SHA-256 |
|---|---|
| `src/futon2/aif/full_loop_cohort.clj` | `f36bffb4e081df76d925ba8d2b2501d0e95190845e7c81934a052f857bed72cf` |
| `src/futon2/aif/full_loop_runner.clj` | `062cdbcce6d184d46029fc560c84d80ec6c0310ab73fbffc68e8b2d195ce32cc` |
| `src/futon2/aif/realized_outcome.clj` | `5ab6330ce29f08a081a4c031e91054519227b8f02e9458d5cab1788f320619d0` |
| `src/futon2/aif/categorical_state_close_attachment.clj` | `74d2bc4ecdbf7e5e647f0f264eed051a57243a2caf282195fd39e38adeb9eebe` |
| annotation spec | `6815b48da949ee9b3b1aa87b5f4221f5852602910ac241ffcbc39454810522ec` |
| r8 tick-run receipt | `5cf73cde9904437c4101e2643856a30d3926948990dece1e186f24620dcdf501` |
| r8 close | `6fcbfe85954ca8994a080025d78385684c1d3549719d9ed48598c53578852352` |
| r8 morning brief | `ffd0f1aa8436d7d7e975247ef536fb54b175490f836e254bd94b175dd4a35853` |

The deterministic close manifests were computed as sorted
`sha256sum(path bytes)` lines, then hashed.  Their results were:
`data/wm-full-loop` `601d6e7c30032b304e89e61981cb7b018d48512a301037101bdd693179f24242`;
machinery-47 `727cd7a5af46d0ef8964a7f28ab2a1dd7aa9ed2c0b5bbad55f00152a6d20c196`;
machinery-48 `9170ffe5b03ff61ec6bf22794afc760d0513cbb35899000f4b7f3ed4c2e110c1`;
machinery-49 `b9ff08238148f5a2628071fef5631c82d8a02e4c8597c96c1f6ebd049a632d6d`.
The archive is included in the `data/wm-full-loop` manifest and is reported
separately above to expose duplication.
