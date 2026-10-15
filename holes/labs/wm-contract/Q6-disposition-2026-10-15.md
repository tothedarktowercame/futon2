# Q6 disposition: repeat after refusal on the reading-family route (2026-10-15)

Requirement (`DarkTower/WarMachine/Requirements.lean`, `Q6`): if
`previousOutcome = refused` and `previousInputDigest = currentInputDigest`,
then `previousChoice ≠ currentChoice`.

Branch `claude-10-q5-family`, from `bcac46503`. Requested by codex-68
(invoke-1792103011271-14760-1501bf3f).

## Diagnosis: what ran before this change

**Runtime: no Q6 at all.** `src/futon2/aif/previous_run.clj` (added
764051460, 2026-10-12) declared `*excluded-pair*`, `exclusion-decision` and
`excluded-ranked`, and its docstring said the runner binds the pair around the
judge. Nothing bound it and nothing called them: the only importer was
`scripts/wm_run_facts.clj`. The runner never read a predecessor before
selecting, so a refused choice could be chosen again on identical inputs.

**Choice shape could not name a reading policy.** The helpers compared
`{:target :precedence}` from `chosen-summary`, where `:precedence` is
`(mapv #(or (:id %) %) (:precedence action))`. A reading policy's
`:precedence` is the map `{:co-apply {...}}`, so this maps over map entries.
Two policies with the same co-apply units and different arrangement edges
compare equal, and the target, policy id and arrangement in the structural
key (`policy-prefix-admission/reading-policy-key`) are not represented.

**The digest was taken after selection.** `:world-at-selection
:selection-input-digest` is computed by `selection-world/capture` after the
decision is final. It hashes decision outputs (`:controller-ranking`,
`:proposal-supply`, `:mission-hole-coverage`), the seat roster and the pattern
library. It cannot be used to decide an exclusion before the selector
(excluding a policy changes the outputs it hashes). It also changes with things
the choice does not depend on, such as seats.

**The predecessor was found by filename.** `previous-record-file` kept only
records whose filename sorted before the current run's. Run ids are
`<date>-<uuid>`, so a same-day predecessor whose UUID sorts after the current
one was invisible. Start instants were compared as strings.

**The exporter searched for its own predecessor.** `history-rows` passed the
previous file in filename order, and the report card used `lookup-previous`
(the same filename filter). `previousChoice` was `pr-str` of the precedence;
`previousOutcome` mapped `:abstained` to `refused`. A reading-route click
closes `:no-selection` with a downstream refusal, which exported as
`invalid`. `currentInputDigest` was the post-selection census digest.

**Lean** needs all five fields; any NR field makes Q6 `unverifiable`.

## Repair

`previous_run.clj` is rewritten (the old helpers and `*excluded-pair*` are
removed).

- **Run stream.** Every run appends `{:event :started :run/id :startedAt}` to
  `run-stream.edn` in its run-record directory before selection
  (`run-opportunity!`), and `{:event :closed :q6 <sealed terminal carrier>
  :record-file}` when `persist-run-record!` writes its record. The run records
  are not parsed (the live directory holds records up to 270 MB).
- **Predecessor.** The `:started` event with the latest start instant strictly
  before this run's, compared as `Instant`s. Its `:closed` event supplies
  choice, outcome and digest. Typed non-certifying cases:
  - no earlier start: `:no-earlier-started-run`;
  - started but never closed: `:previous-run-not-closed`;
  - unparseable stream line: `:run-stream-unreadable`;
  - unparseable start: `:run-stream-start-unreadable`;
  - tied starts: `:previous-run-ambiguous`;
  - closed twice: `:previous-run-closed-twice`;
  - seal broken: `:previous-terminal-carrier-tampered`;
  - id or start mismatch: `:previous-terminal-carrier-foreign`.

  The carrier is sealed and bound to `previous-run/*predecessor*` around
  `run-opportunity-core!`. Unbound, the decision records
  `:predecessor-carrier-not-supplied`.
- **Choice.** The selected action's `:policy-key`, checked against
  `reading-policy-key` recomputed from the action. A non-reading action is
  `:foreign`, a mismatched key `:malformed`, and no action `:absent`.
- **Outcome.** A downstream refusal after selection (now always
  `:reading-policy-execution-semantics-owed`) or the decision's own Q6 refusal
  is `:refused`. A decision that chose nothing is `:abstained`. Close outcomes
  map to `RunOutcome`; anything else is `:invalid`.
- **Digest** (`selection-inputs-digest`). Taken over what the one selector
  consumes, before exclusion:
  - each policy's structural key, G, G terms and F carrier;
  - beta;
  - the reading-precision identity (scorer, kernel, graph, readings,
    arrangements, preference authority);
  - the selector options (history pin, enactment fold, pattern feedback,
    ticket queue, novelty inputs).
- **Exclusion before the selector.** `select-over-families` takes an
  `:exclude` hook. It runs after `:certify`, so the precision identity is that
  of the whole scored family, and before `policy/select-action-cascades`.
  - When the predecessor closed refused with the same digest, exactly its
    structural key is removed from the menu. The selector runs once over the
    rest, and the excluded policy is never interpreted.
  - With nothing left, the decision abstains typed
    `:repeat-choice-after-refusal` (added to the gate's refusal kinds) and the
    selector is not called. The re-arm condition is written:
    `{:condition :selection-inputs-change :input-digest d :excluded-key k}`.
    The refusal travels in the terminal carrier as `:q6-refusal`, and later
    runs on the same digest keep the key excluded
    (`:repeat-refusal-rearm-not-met`).
- **Record.** `:q6 {:predecessor … :terminal …}`; the certificate carries the
  sealed exclusion carrier at `[:selection-certificate :q6]`.
- **Exporter.** `wm-run-facts/q6-facts` reads only the record's `:q6`. It
  verifies both seals and that the terminal belongs to the record.
  - Absent, tampered or typed-absent parts are NR.
  - `:abstained` is NR, because it is not a `RunOutcome`.
  - `lookup-previous` and the previous threading in `history-rows`,
    `build-card` and `controlled-report-card/produce` are removed, and
    `facts-for-record` takes `[record path snap]`.

## Evidence

`test/futon2/aif/wm/q6_repeat_after_refusal_test.clj`: 7 tests, 91
assertions, 0 failures, 0 errors. All runs go through the production runner
(`run-opportunity!`) or the production decision, with the real reading-family
fixture.

- **Repeat after refusal on the same inputs.**
  - Run 1 selects, then closes refused (`:reading-policy-execution-semantics-owed`).
  - Run 2's predecessor is run 1 (read by the runner), and the digests are
    equal.
  - Exactly run 1's key is excluded (menu 2, removed 1). One selector call
    picks a different reading key, and the excluded key is absent from the
    certificate's candidates.
  - Exported Q6 holds.
- **Only candidate.** The one-policy family (`["p/a"]`):
  - run 2: 0 selector calls; abstains `:repeat-choice-after-refusal` with the
    written re-arm; the choice is absent and the outcome refused;
  - run 3, same inputs: still refused (`:repeat-refusal-rearm-not-met`);
  - run 4, after a theta change: 1 selector call and the same policy chosen.
    Q6 is NR there, because run 3 chose nothing.
- **Changed digest.** After a theta change the status is
  `:not-triggered :selection-inputs-changed`, removed is 0, and Q6 holds.
- **Successful previous outcome** (`:grounded-change`): `:not-triggered
  :previous-outcome-not-refused`, removed 0.
- **Ordering.** The ids sort opposite to their start instants, and a run
  started after the current one is present. The predecessor is the latest
  earlier start (`…-0000` at 11:00), not the name that sorts last.
- **Non-certifying carriers.** Seven cases: no stream, not closed, unreadable
  line, tampered close, tampered predecessor, unbound, foreign legacy choice.
  Each gives `:not-recomputable` with its reason, removes nothing, and calls
  the selector once. Exported Q6 is NR.
- **Identity.** A sealed predecessor naming the same key with a different
  arrangement, target or policy id, or an old `[target precedence-vector]`
  pair, removes nothing (both policies stay). The exact key removes one.
- **Exporter.**
  - Without `:q6` or without its predecessor, Q6 is NR; a tampered terminal
    is NR.
  - `facts-for-record` carries exactly `q6-facts`.
  - `wm-run-facts-test` now asserts that the census digest is not used as
    `currentInputDigest`.

Planted bugs (each applied alone, the test namespace run, then reverted;
scripts in `~/notes/q6/plants/`):

| plant | effect | failures |
|---|---|---|
| P1 dead wiring | runner does not bind `*predecessor*` | 17 (4 tests) |
| P2 filename order | predecessor = last by run id | 4 |
| P3 precedence vector | exclusion compares `[target precedence]` | 14 (4 tests) |
| P4 exporter fallback | absent predecessor → uses own terminal | 14 (3 tests) |

**Lean.** `test/fixtures/q6-lean/generate.clj` generated the fixtures from
real runner exports. Each was checked with `lake env lean` in the canonical
mathlib4 checkout, with no mathlib changes:

- `Q6First.lean`: Q6 `unverifiable` (no earlier run);
- `Q6Repeat.lean`: Q6 holds (absent from `alert`);
- P1 applied (`~/notes/q6/lean/Q6DeadWiring.lean`): Q6 `violated`, the same
  choice on the same digest after a refusal.

**Affected controls.** Fifteen Q5 and exporter namespaces plus the Q6 namespace:

- the Q5 namespaces: family-selection, reading-family-decision,
  reading-model-contracts, reading-policy-habit, selected-reading-runner,
  target-policy-field-decision, terminal-receipt, report.cascade-decision,
  decision-gate, certified-zero-history, h4-production-stage;
- the exporter namespaces: wm-run-facts, wm-report-card,
  controlled-run-persistence, integrated-controlled-sut.

Result: 148 tests, 1366 passes, 0 failures, 1 error. Base `bcac46503` on the
fifteen gave 141 tests, 1275 passes, 0 failures, 1 error. The error is the
same in both: `wm-run-facts-test/q9-historical-click-record-has-no-authorized-factorization`
reads `data/wm-runs/…c9d25d6a….edn`, which is not in the worktree.

**Static gates.**
- clj-kondo: on the changed source and test files, 0 errors and 5 warnings,
  the same 5 as base.
- The two in-ns generator scripts (Q5's existing one and the new Q6 one) do
  not resolve under kondo, by construction.
- check-parens OK; `git diff --check` clean.

Not run: full CI, live click.

## Open

- **Live behaviour.** Q6 is one-back, as in Lean. Every live reading click
  closes refused until reading-policy execution semantics are ruled, so with
  two policies on unchanged inputs production alternates between them. Each
  run is Q6-compliant.
- **No records before the stream.** The 248 existing records predate
  `run-stream.edn`. The first run after deployment has
  `:no-earlier-started-run`, which is NR.
- **Runs that never close.** A run that throws before `persist-run-record!`
  never closes, so the next run is NR with `:previous-run-not-closed`.
- **After a repeat refusal.** The next run has no previous choice, so Lean Q6
  is NR there, while the runtime keeps the key excluded until the digest
  changes.
- **What the digest covers.** It includes the novelty inputs, the enactment
  fold and the history pin, so a ledger write between runs counts as changed
  inputs.
- **Two digests remain.** `:world-at-selection :selection-input-digest` (the
  census) still exists. Q6 no longer reads it.
- **Concurrency.** Writes to the stream rely on the run lock (one runner).
