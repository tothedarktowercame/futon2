# E3-OUTER-REPEAT discovery (2026-10-29)

Scope: canonical `/home/joe/code/futon2` only. I read the runner/runtime,
outer-state carrier, retained run records, and the three cited criteria notes.
I made no serving-JVM call, reload, click, or live-data write. E3 says that
“re-picking a refused task on unchanged inputs is a violation”
(`holes/labs/wm-contract/GRID-wm-criteria-completeness-v2.md:53`); the proposal
also says this prevention is not implemented (`holes/labs/wm-contract/PROPOSAL-GRID-v2-outer-loop-2026-10-28.md:31,48-49`).

## 1. Detection boundary and supported stop

The outer loop chooses at `outer-select/select`, then constructs a retained
selection containing the Giraffe run and manifest digest, makeup digest,
focus, selected relation, scores and chosen candidate
(`src/futon2/aif/full_loop_runtime.clj:650-667`). It writes that selection as
`:pending`, binds the target and `:outer/selection` into the runner options,
and enters the existing with-task path (`full_loop_runtime.clj:669-680`).
The wrapper preserves those options through `runner/run-opportunity!`
(`full_loop_runtime.clj:691-741`).

The latest safe automatic comparison point is inside `run-opportunity!`,
after construction and after the exact pre-author identities have been
captured, but before `run-phase! :author-dispatch`:

- construction produces `mission` and `construction` at
  `full_loop_runner.clj:8034-8042`;
- the runner observes the repository/head at `:8292-8311`, captures the D-task
  dispatch at `:8329-8341`, captures and checks the mission before-state at
  `:8342-8384`, and durably appends that baseline at `:8385-8389`;
- prompt construction follows at `:8390-8391`; the first author dispatch is
  the next expression, `run-phase! :author-dispatch`, at `:8393-8403`.

Thus an implementation can compare the selected target plus the retained
outer inputs and the just-captured source/before-state immediately before
line 8393. Comparing earlier in `full_loop_runtime.clj` would lack reading,
construction, repository head and mission baseline; comparing in or after
`:author-dispatch` is too late.

There is an existing supported **manual** stop, exactly the safeguard named by
the disposition (`holes/labs/wm-contract/run001-v2-disposition.md:37-41`):
attach and arm `[:after :construction]` using
`wm.debugger/attach!`/`arm-breakpoint!` (`src/futon2/aif/wm/debugger.clj:24-39`).
`run-phase!` throws the breakpoint with the computed construction retained at
`:debugger/result` (`full_loop_runner.clj:400-416`). `stopped` and `stop-value`
expose it (`src/futon2/aif/wm/debugger.clj:88-109`), while `continue!` supports
`:continue` or `:abort` (`wm/debugger.clj:111-142`); abort is rethrown without
reaching author dispatch (`full_loop_runner.clj:442-493`). This is manual
observation, not implementation closure. Searches of the ordinary runtime,
runner and HTTP route found no preview, dry-run, or `:stop-before` option; the
earlier canonical discovery records the same negative result
(`holes/labs/wm-contract/DISCOVERY-run001-e3-r28-2026-10-28.md:11-22`).

## 2. Durable refusal history

The authoritative ordinary-run history is the terminal run record written by
`persist-run-record!` (`src/futon2/aif/full_loop_runner.clj:1243-1471`). It
retains `:outer/selection` when supplied in the runner options (`:1469-1470`),
the selected event/world (`:1328-1440`), and terminal failure. The current
outer state is not a refusal index: `read-state` validates one current carrier
and makeup pin (`src/futon2/aif/outer_state.clj:226-243`), and `write-state!`
atomically replaces it (`:245-260`). Its `:pending` is the immediately prior
selection (`full_loop_runtime.clj:671-677`), not cross-run refusal history.
The META injury ledger concerns repair injuries, not ordinary task refusals;
searches for an ordinary target/refusal lookup in `outer_state.clj`,
`outer_select.clj`, and `meta_repair_coordinator.clj` found none.

One actual refused record is
`data/wm-runs/tick-run-record-2026-10-24-d0608eec-cca3-45de-9c19-14d6db26577d.edn:1`.
Copied from that file's actual EDN shape (projected read-only with
`clojure.edn/read-string`):

```clojure
{:run/id "2026-10-24-d0608eec-cca3-45de-9c19-14d6db26577d"
 :selection-event {:target "M-recommendation-bindings"}
 :failure {:kind :already-satisfied
           :stage :author-wait
           :target "M-recommendation-bindings"}
 :world-at-selection
 {:selection-input-digest
  "f1b9c963798f1c5c134b99b7df8849d1ca3841eafaf02c71e2c4c147256f8585"
  :pattern-library
  {:file-count 1456
   :digest "a30ecf319e782c3bbe600b6f7865528eda0119ffa38b8d09cd647c5ab83772e7"}}}
```

Its failure detail additionally carries `:grounded-commit {:repo "futon5a",
:sha "68120423443eab3243c400563cc9c66f45fea6e7"}` in the same record (`:1`).
This record predates the current outer selector, so `:outer/selection` is nil;
that absence must not be treated as equality with a later outer selection.

## 3. “Relevant inputs unchanged”

The comparison key should be explicit and versioned, not “same target” alone:

1. selected task id (`[:selection-event :target]`, or current
   `[:outer/selection :selection :selected :target]`);
2. current outer decision identity: Giraffe run id and manifest SHA, makeup
   SHA, focus and selected relation (`full_loop_runtime.clj:651-662`), plus
   cascade id/index/scores/prediction already returned by `outer-select/select`
   and retained by the same expression (`:650-667`);
3. selected-reading/request/result pins and the constructed mission/action;
   these exist only after the chosen-task reading and construction, which is
   why the detector belongs after construction (`full_loop_runner.clj:8034-8042`);
4. source identity: target repository and pre-author head
   (`full_loop_runner.clj:8294-8311`), D-task dispatch (`:8329-8341`), and the
   captured mission baseline (`:8342-8389`);
5. for legacy records, `:world-at-selection :selection-input-digest` plus its
   component digests. The real refusal above carries pattern-library digest;
   its same `:world-at-selection` map carries enumerated-task, seat-roster and
   open-task-category identities (`...d0608eec...edn:1`).

A detector should refuse only when the previous terminal class is refused /
failed / already-satisfied **and** the available comparison schema and every
required identity agree. Missing legacy fields mean “not comparable”, not
“unchanged.” This follows the disposition's instruction to compare target,
relevant inputs and terminal outcome (`run001-v2-disposition.md:37-46`).

## 4. Retained repeated-target evidence

There is a real re-pick after refusal. The 2026-10-24 record above selected
`M-recommendation-bindings` and ended `:already-satisfied`. The task was then
selected again in these retained records:

- `data/wm-runs/tick-run-record-2026-10-26-f444287e-51e6-445b-a85d-25d45cc52a43.edn:1`
  (`:invalid-author-refusal`, input digest `e0d86a55...`);
- `data/wm-runs/tick-run-record-2026-10-27-39eb74f5-9db6-4f1d-8d28-03694c6f1fba.edn:1`
  (`:invalid-author-refusal`, input digest `91acc400...`);
- `data/wm-runs/tick-run-record-2026-10-27-8c8ee35f-54da-41f9-8e51-1d51e523a98f.edn:1`
  (`:invalid-author-refusal`, input digest `92d06a03...`);
- `data/wm-runs/tick-run-record-2026-10-27-fd5d1d5f-efe7-4d3e-aae2-1f860f342c40.edn:1`
  (`:invalid-author-refusal`, input digest `0b83b95a...`).

This is discriminating evidence for “same refused target can be selected
again,” but **not** for “unchanged inputs”: all retained selection-input
digests differ from the 2026-10-24 digest, and the legacy records lack current
outer-selection identities. I searched all `data/wm-runs/*.edn` records
naming `M-recommendation-bindings`; I found no pair that both has a refusal,
the current outer identity fields, and equal relevant-input pins. The row
therefore still needs a controlled cross-run test and a live observation to
close, as its criterion says; manual comparison alone does not close it.

## Runner sections touched

An implementation needs these ownership surfaces, and no selector rewrite:

1. `futon2.aif.full-loop-runner/run-opportunity!` — add the automatic
   cross-run comparison after durable pre-author baseline capture and before
   `run-phase! :author-dispatch` (`src/futon2/aif/full_loop_runner.clj:8288-8403`).
2. `futon2.aif.full-loop-runner/persist-run-record!` — ensure the comparison
   identity/verdict and all current `:outer/selection` inputs are retained for
   the next run (`full_loop_runner.clj:1243-1471`).
3. A new read-only refusal-history helper may live in a separate namespace;
   it should read terminal `data/wm-runs` records. Do not overload
   `outer-state/read-state`/`write-state!`, whose contract is the current
   learned carrier (`src/futon2/aif/outer_state.clj:226-260`).

`full_loop_runtime.clj/run-ordinary-opportunity!` already supplies the target
and outer identities (`src/futon2/aif/full_loop_runtime.clj:621-680`); it only
needs alteration if the implementation chooses to add a separately computed
comparison-input digest before calling the runner. `outer-select/select` does
not need to change: prevention belongs at the pre-author boundary, after the
chosen-task inputs exist.
