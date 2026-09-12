# Row 23 — R6 to R16 correspondence verdict from retained runs

**Date:** 2026-09-12  
**Scope:** WORK-REMAINING row 23; retained records only  
**Overall verdict:** `:insufficient-retention`

## 1. Question and declared bar

The theory-accounting entry calls `[:R6 :R16]` / `:action`
`:path-dependent` and records the historical `close-loop!` divergence
(`aif-equations.edn:1038-1041`).  The later ruling requires, from F11 onward,
each run record to carry either enacted-equals-selected evidence or a typed
reason why not (`aif-equations.edn:215-216`).  Row 23 asks whether the retained
build-phase runs now settle that correspondence
(`WORK-REMAINING.md:852-854`).

They do not.  There is enough evidence to preserve a historical
`:diverges-with-named-cause` result and one F11 `:corresponds-at-pins` result,
but not enough current, independently retained per-run evidence to choose
either as the build-phase-wide verdict.

## 2. The per-run capture

`futon2.aif.full-loop-runner/selection-enaction-record` was introduced by
`679746a30b4159f02ccc9485f9d0b20780e3ee98` at
2026-09-12T17:04:13Z.  It compares the selected and enacted action by exact
Clojure equality and emits `:match` or `:typed-divergence`, retaining both
values and evidence (`src/futon2/aif/full_loop_runner.clj:1419-1426`).  The
construction checkpoint calls it with:

- selected = the chosen ranked entry's `:action`;
- enacted = the authenticated operator pin's
  `[:provenance :enacted-candidate-action]`, or that same selected action when
  no pin is present; and
- evidence source = `:authenticated-operator-task-pin` or
  `:runner-selection`

(`src/futon2/aif/full_loop_runner.clj:3304-3315`).  Thus the capture lives in
the cohort **construction checkpoint**, not the WM trace.  Its direct positive
and divergence behavior is unit-tested
(`test/futon2/aif/full_loop_runner_test.clj:1752-1760`).

### Is it running?

Not in the retained production corpus at this pin.  A read-only census found:

| Corpus | Records inspected | `:selection-enaction` records |
|---|---:|---:|
| `data/wm-full-loop/**/*-construction.edn` | 86 | 0 |
| `holes/labs/wm-contract/tick-run-record-*.edn` | 144 | 0 |
| `holes/labs/wm-contract/runs/**/*construction*.edn` | all matches | 4 files, one underlying run/pin |

The last—and only—retained positive instance is the F11 scratch construction
at `runs/certificate-v1-emitter-controls-2026-09-12/scratch/003-construction.edn`
(SHA-256 `8edf937cfdb08ce4620507178c2dbb4e99e954d7a802076fdd3a941d8aa6f198`),
recorded at `2026-09-12T17:10:18.455894673Z` for cohort
`:run4-f11-production-successor-20260912-v1`, attempt `attempt-001`.  It says
`:match` for selected and enacted
`{:type :advance-mission :target "M-f11-find-production-successor"}`, with
`:authenticated-operator-task-pin` evidence.

This is a certificate-emitter **scratch input**, not an independently retained
production construction corpus.  Its digest-mismatch sibling is a mutated copy
of the same pin, not a second run.  The other two siblings are explicit
negative controls with `{:type :a}` versus `{:type :b}` and classes `:fixture`
and `:fixture-override`; they commission detection but are not observed
divergences.  Therefore the retained count is: one unique positive F11 pin,
one copied positive row, two induced controls, and zero deployed production
rows after capture introduction.

## 3. Evidence inventory

| Evidence | Count | What it establishes | R6 to R16 verdict at that scope |
|---|---:|---|---|
| Historical traces `wm-trace-2026-07-03.edn`, `-07-04.edn`, `-07-05.edn` | 3 with both broad halves | July 04 and 05 select rank-1 `M-first-flights`, while the first passing act gate enacts `M-bayesian-structure-learning`; July 03 has non-joinable action vocabularies. | 2 `:diverges-with-named-cause`, 1 not determinable |
| Other traces in the C460 census | 54 | Decision present, no enacted mission. | not determinable |
| F11 scratch construction pin above | 1 unique pin | The production-runner construction input's selected action equals the authenticated enacted-candidate action. | `:corresponds-at-pins` |
| Certificate-emitter derivatives | 1 positive copy + 2 induced controls | Copy does not add a run; controls show divergence is representable/detected. | no observational verdict |
| Current `data/wm-full-loop` closes/constructions | 82 closes / 86 constructions | No direct correspondence field is deployed in the retained corpus. | not determinable |
| Current top-level tick-run records | 144 | None carries `:selection-enaction`; 16 carry an `:enacted-action`, but the terminal-record producer emits only that action for repair-machine failure, not the selected/enacted pair (`full_loop_runner.clj:333-358`). | not determinable |

The historical counts and mechanism are retained in
`C460-enacted-vs-selected.md:15-41,43-58`: the selected policy was never gated,
and the first lower-ranked passing candidate was enacted.  That is a named
cause, not noise or a tie.  C460 also limits the conclusion to the old
`close-loop!` path and says no recorded full-loop-runner decision/enactment pair
was then available (`C460-enacted-vs-selected.md:77-92`).

The row-14 retention stack does not close this gap today.  The coverage record
marks `:status-at-close`, `:outcome-entity`, and `:cross-ledger` all
`:deployed? false`, with reload pending
(`runs/row-14-coverage-2026-09-12/coverage.edn:5-10`).  A census finds zero of
82 retained close records carrying `:outcome-entity` or
`:entity-state-at-close`.  Once deployed, those fields identify the closed
entity and its belief source; they do not replace the direct selected/enacted
action comparison in the construction checkpoint.

### Today's admitted selector divergence

The pinned trace fixture at
`runs/row-15-machine-action-divergence-2026-09-12/fixture.edn:1-27`
(SHA-256 `1a78c6fe3c211a1f713256001472d8c3f9136664beb132b0ddd68424ee3082a9`)
records controller head rank 1 (`M-expressions-of-interest`) and live-selector
choice rank 139 (`M-shared-memory-control-build-test`) at
2026-09-12T17:28:09.498448087Z.  It establishes a named substitution **before
enaction**: the reason-bearing live selector moved the controller's choice.
It does not record an R16 actuation or grounded outcome, so it is neither a
selected/enacted match nor a selected/enacted divergence.  It names a candidate
cause that a future per-run record must retain if the action reaching R16 is
not the controller head.

## 4. Typed verdict

**`:insufficient-retention`.**

The evidence is deliberately not pooled into a majority.  The two historical
joinable close-loop records give a real sub-verdict of
`:diverges-with-named-cause` (first-passing-gate substitution).  The one F11
scratch pin gives `:corresponds-at-pins` for the authenticated production-runner
construction input.  Neither supplies a current series of independently
retained build-phase runs, and the deployed corpus contains no direct capture.
Consequently a current build-phase correspondence claim would exceed the
records.

## 5. Smallest next packet

Run the already-implemented capture as a standing build-run capture after the
pending serving/relevant-runner reload, and retain the **actual construction
checkpoint** for each build-phase attempt.  Acceptance is at least one natural
match and every natural divergence (if any), each with selected action, enacted
action, evidence source, cohort/attempt identity, and exact run identity; no
synthetic substitute counts.  Confirm the record lands in the durable
`data/wm-full-loop` corpus rather than only a certificate scratch directory.
Then a witness packet can state per-pin correspondence or the retained typed
cause.  No new comparison function is required unless the first deployed run
shows the construction checkpoint itself is not being persisted.

## 6. Pin notes

- Repository HEAD used for this read-only inventory:
  `ae6ee63472a0c6d974b13209cc1336bb711f5e95`; unrelated concurrent working-tree artifacts were
  not read as authority except the enumerated retained record files.
- The control-map source still distinguishes the judge and actuation processes
  and records the derived control path through R16
  (`/home/joe/code/p4ng/empirics-futon/control-map-edges.edn:264,281-282`).
- Counts are observations at 2026-09-12 and will change as the standing capture
  begins retaining real runs.
