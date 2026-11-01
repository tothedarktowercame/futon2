# L5-15-D — Completion baseline on the excursion selection path

Read-only discovery; report only. futon2 base 956cb1101; futon3c source
89173aa2e965ec417296506fda91753b22cd597f (also the retained run's manifest git-sha).
No source changes, tests, producer executions, data writes or reloads.

## 1. Meaning and producer

The value is a source-discovery description, not a completed-wants count, date,
or comparison with a previous Giraffe run.

Actual producer: futon2 src/futon2/aif/task_completion_surface.clj:8-36,
describe. Its namespace/docstring explicitly says "Source-pinned completion
discovery, not A4 baseline certification" and "Never fills a baseline".
It checks source bytes against the registry SHA (12-18), discovers checkbox/
criteria/prose locators (19-27), then emits schema :wm/completion-surface-v1:
target, source-pin, completion-class, checkbox-count, criteria-count,
located/unlocated-criteria-count, producer, and **unconditionally**
:baseline {:status :absent :reason :selected-task-baseline-not-retained} (32).
The producer label is the next needed capability, not proof it has run.

Giraffe's combined M/E/T census calls describe for excursions at
futon3c src/futon3c/giraffe/excursions.clj:12-32 and tickets at 35-64.
combined-pass (166-209) calls pass/pass, then carries completion from the input
item into its ordered output row at 205. Dependency-reading readiness and its
unknown cases are computed separately (175-207).
The mission adapter, giraffe/missions.clj:62-84, does not attach completion.

Documentation: holes/missions/M-giraffe.md:221-227 describes the source-adapter →
item-record → readiness/cone/unblocking pass, not an A4 baseline computation.
The Giraffe observation-series README's Step 3 describes ticket provenance,
also not baseline certification. The executable row contract and precise
completion schema are the describe return map above; no baseline-value schema
is defined there beyond the typed placeholder.

The existing *real* baseline is a different carrier:
futon2 src/futon2/aif/mission_observation.clj:62-103, capture-before.
It binds selected wants and locator results to an action/occurrence, repository,
immutable pre-author revision and digest. It is neither a date nor an aggregate
count. full_loop_runner.clj:9252-9286 captures and durably appends it before
authoring. Reusing that machinery requires a selected dispatch and supported
locators; Giraffe's source discovery has neither that dispatch nor its receipt.

## 2. Retained run census

Click 11's selection record:
data/wm-runs/selection-record-2026-11-01-wm-click-7adb0f36-8dce-43b1-933b-95eaedd52f2a.edn.

Its construction reading pin equals the SHA of pass.edn in:
storage/giraffe/20261101T182437.866Z-bb46b649/.
File hashes independently computed and matched to the manifest:
- pass: 8bae4187852784c697c949ac5578cee94ad16e185031b679b7055aa6c876e872
- input: bb46b64943604766aba0f178ad51b80b90cec3fe0c9a07420dcce6d7929bf4d6
- manifest: 85c48e1c9e9bfb2e429f64a6c0d84faeb8b98e0ea7b3673f4f0440ea5d1082fb

Structural EDN read of :order (607 rows; not a textual grep count):

| Prefix | Rows | Baseline key present | Completion without baseline key | Neither |
|---|---:|---:|---:|---:|
| M | 179 | 0 | 0 | 179 |
| E | 238 | 238 | 0 | 0 |
| T | 190 | 190 | 0 | 0 |
| Total | 607 | 428 | 0 | 179 |

**All 428 present baseline keys contain the typed absence; zero contain a measured
baseline.** Key presence must not be reported as baseline availability.

E classes: 139 checkbox-locators-with-retained-prose, 98 completion-definition-not-found,
1 completion-prose-needs-locators.
T classes: 188 completion-definition-not-found, 2 completion-prose-needs-locators.

E-comprehension-foundation has four checkboxes, zero parsed criteria, class
checkbox-locators-with-retained-prose, producer selected-task-revision-baseline.
Source pin: futon6/holes/excursions/E-comprehension-foundation.md,
6863756378922c1aa289e75c0c750f04bc66e797e04dae721f3c9e39b1afb663.
Four checkboxes is not four completed wants, or a baseline value of four/zero.

Earlier retained pass 20261101T044121.422Z-730407c5 contains all 428 of these E/T
ids. The selected excursion already has the identical completion description,
source pin and absent baseline there. This directly rejects "first seen this run"
for this population without assuming anything about previous execution.

## 3. Classification and honest values

This is intentional discovery-only producer behavior combined with a missing
measured-baseline handoff. It is general to the E/T completion producer, not
specific to excursions, first observation, or a failure reading this source.
M rows have the separate omission, not evidence of successful baselines.

- Current case: retain selected-task-baseline-not-retained as an absence until
  an actual selected-revision measurement is bound. A previous Giraffe pass
  cannot supply that A4 receipt.
- Genuine first observation in a future temporal-comparison feature: retain a
  typed no-prior-observation, scoped to that comparison. Do not substitute it
  for the current reason, since describe never consults history.
- Unreadable/drifted source: retain/refuse the reading failure, not zero.
  describe throws completion/source-pin-mismatch for mismatched bytes; slurp
  can also throw on missing/unreadable files. Neither is the observed branch.
- Unknown completion definition or missing locators: keep the existing
  completion-class and producer request. Text discovery does not establish
  false before-values. Only a successfully measured zero is zero.

Q7's observed path is an exporter projection: wm_run_facts.clj:1142-1174 copies
the chosen member's field fact into :pick-fact and recursively counts typed
absences. :pick-fact is not a separately produced Giraffe row.
Consequently M rows' omitted completion is not counted, whereas honest E/T
placeholders are. That asymmetry is not evidence that mission baselines exist.

## 4. Smallest honest next change (not implemented)

Do not exclude E/T tasks for lacking an A4 pre-author receipt, and do not erase
or zero-fill the placeholder to make Q7 green. outer_observation/giraffe-field
(src/futon2/aif/outer_observation.clj:192-206) currently copies the discovery
description faithfully into :field-facts.

Two distinct obligations must be decided explicitly:

1. If completion measurement is required to choose a task, build and pin a real
   task-revision baseline with supported locators before selecting it. Reuse
   mission-observation/capture-before's binding/reader semantics where applicable;
   supply the missing selected-task contract rather than inventing an aggregate.
   The source is in futon2, so this is not solely a futon3c producer fix.
2. If this is discovery metadata and the baseline belongs to work/pre-author
   execution, give it a separate typed discovery carrier and an explicit phase
   contract. Update selection-absence-paths to inspect actual selection
   dependencies, retaining the discovery deficit elsewhere. This needs a declared
   Q7 boundary, not an ad hoc exception for this task. Keep required work-baseline
   checks intact. Do not retrospectively certify click 11.

Giraffe-side consistency belongs in excursions/census-input, missions/mission-items
and combined-pass: align M/E/T discovery coverage and carry an honest schema.
Computing a baseline by consulting a previous pass is not the missing operation.
Futon2-side typing belongs in task-completion-surface/describe and the field/
export phase boundary. No change to the closed task-exclusion reasons is justified
by the evidence in this packet.
