# A4 recording implementation and adoption boundary

Implementation provenance: claude-1 continuation
`invoke-1788971284841-16610-41950f7a`, applying Joe's Item 23 delegation in
`holes/labs/wm-contract/RULINGS-walkthrough-2026-09-09.md` and the field table
and clauses in `holes/labs/wm-contract/DRAFT-realized-outcome-recording-19b-2026-09-09.md`.
This is implementation evidence for review, not a run-start authorization.

## Supplied choices

The continuation supplies all five choices: supervised `wm_step.sh` wrapping
`scripts/futon2/run_tick_once.clj`; next-accepted-step window version 1; one
attempt per accepted step, identified by run-id/step-index; Joe as acceptance
reviewer; immutable follow-up corrections, with latest revision replacing the
prior sample at the next derivation. These are not inferred from categories.
`RUN4-execution-runbook-2026-09-09.md` identifies that runner; Item 19e identifies
acceptance inspection as the retrospective. The scheduled runner is unchanged.

## Implementation and consumers

- `src/futon2/aif/realized_recording.clj`: the stronger marker retains the v1
  schema. `validate!` checks the field table, status/projection consistency,
  evidence references, paired identities, preference reading versions and
  process occurrence identity. Only observed wrappers carry values; unknown,
  inadmissible and not-applicable are distinct. `adapter-error` throws with
  evidence. `persist!` refuses replacement of a different existing record.
- `holes/labs/wm-contract/wm_step_observe.bb`: builds the envelope from the
  accepted predecessor and next step, with a content digest and trace locators.
  Channel capture uses `:observation-envelope` provenance, not defaulted numeric
  zeros. Checkpoints, missing timestamps, subject revisions, execution receipts
  and preference readings remain explicitly unknown when not captured. Neither
  observed category nor designated reviewer fabricates closure or completed
  review. The first step's no-predecessor receipt remains unmarked absence:
  there is no earlier attempt to invent. Missing ranked decisions also retain
  their existing explicit absence receipts.
- `src/futon2/aif/fold_realized.clj`: both enactor producers support explicit
  `:recording/context`; absent context retains legacy output. The actual legs,
  grounded dial, snapshot and expected-source survive wrapping. Context cannot
  turn the producer's perfection target into a forecast. These other paths do
  not acquire complete capture automatically and are not RUN4's chosen producer.
- `src/futon2/aif/realized_outcome.clj`: normalization never grants the marker;
  marked category lookup requires observed step classification. The retrospective
  `outcome-leg` uses that accessor and does not label unknown as measured.
- `src/futon2/aif/selection_gain.clj`: marked calibration requires explicit
  prediction provenance, compatible quantities/units/methods/windows and admitted
  observed legs. Stepped G-core re-evaluation is not admitted. The journal retains
  originals; correction replay replaces a sample at its original position,
  including retraction when the latest revision is inadmissible. Interleaved
  legacy samples are retained. Legacy-only behavior stays on its original path.
- Preference diagnostics use `preference-readings` with `:realized`; forecasts
  require their separate model/horizon/domain/pre-decision receipt. The existing
  preference assessor checks realized readings; assessments carry version/mode.
  No channel-to-disposition model, C_int/channel identity, registry adoption,
  terminal checkpoint inference or preference-strength mutation is implemented.

## Deliberately unclaimed guarantees

**Strategic-habit correction/retraction is NOT YET CLAIMED**, using the
continuation's explicitly permitted fallback. `src/futon2/aif/strategic_habit.clj`
`accumulate` deduplicates strategic selection events by event ID and rejects
conflicts; it does not consume the run/decision/attempt/measurement revision
chain. Its forward selection counts are not outcome-calibration samples.
No strategic reader change or E default change is included here.

The retrospective's `run-observations` still indexes one observation directory
by observed tick; cross-store correction discovery/retraction is likewise not
claimed. The latest-revision API expects predecessor-first input. Corrections
must have distinct paths and explicit predecessor identities; this packet adds
no automatic correction authoring, cross-store discovery or review UI. Do not
claim universal consumer replay conformance from the gain-reader test.

An existing legacy observation is never rewritten into a marked one: replay
with persistence against that path refuses if its bytes represent a different
record. Use `--print` for historical inspection. Unknown checkpoints/preferences
are honest partial capture, not evidence that the missing observations occurred.

## Reproduction and results

`test/fixtures/realized-outcome-u59-live.edn` is a verbatim capture of
`holes/labs/wm-contract/runs/2026-09-05-u59-b/observation/realized-outcome-2026-09-05-u59-a.edn`
at source commit `3e9dd1fd44b954c258829ecb70a5ba816c7b8be4`; the test checks byte
equality. Actual script replay uses the recorded U59 traces, a temporary pin and
`--print`, then persists/reads/consumes only in a temporary directory.

Focused command:

```sh
clojure -M:test -e "(require 'futon2.aif.realized-recording-test 'futon2.aif.realized-outcome-test 'futon2.aif.selection-gain-test)(let [r (clojure.test/run-tests 'futon2.aif.realized-recording-test 'futon2.aif.realized-outcome-test 'futon2.aif.selection-gain-test)] (System/exit (+ (:fail r) (:error r))))"
```

39 tests, 177 assertions, zero failures/errors. Includes absent-vs-zero,
checked-empty, false feedback, warrant removal, method/scale/source mismatch,
unknown category, step-vs-terminal, same tick/different runs, revision retraction,
preference provenance, repeated occurrences, both enactor opt-ins and real script
replay. Two read-only retrospective outcome-leg controls also passed.

Broader fold tests did not pass: the combined four-namespace run had 40 tests,
186 assertions, zero failures and three errors; a narrower three-test run had
20 passing assertions and one error in `staging-gates-on-live-wire`.
Both encountered existing deposit provenance refusals via
`src/futon2/aif/actuator_a3.clj:150` (stored/reconstructed prompt pins differ).
No repair of those deposits is included. Focused enactor tests supply an explicit
deposit and controlled snapshot, avoiding those unrelated live inputs.

Gates: clj-kondo separately on the two bb scripts (both use `user`, so a combined
lint creates cross-file redefinition warnings), and together on touched src/test
namespaces; zero errors/warnings. Futon4 check-parens and `git diff --check`
passed. No live run, lock, data write, ledger, registry, worklist or frontier edit.
