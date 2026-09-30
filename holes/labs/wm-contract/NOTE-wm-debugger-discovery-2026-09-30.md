# NOTE: what it takes to run the real WM click under a debugger — discovery, 2026-09-30

Discovery only (zai-1, for claude-1 / PROOF-2b). Joe's ruling: run the real
machine forward until it works — "no different from the regular machine
being run in a debugger". No code was changed for this note. Line numbers
are as of futon2 @ c2331934 / 27709090 and futon3c working tree
2026-09-30; other agents edit futon2 in parallel, so re-check before use.

The cancelled F1 thin path (`futon2.aif.wm.forward`) is NOT resumed here.
Everything below is about `futon2.aif.full-loop-runner/run-opportunity!`,
called in-process by `futon3c/src/futon3c/wm/runner_service.clj`
(`run-click!`, runner_service.clj:486; the runner is resolved dynamically
at :493 `'futon2.aif.full-loop-runtime/run-opportunity!`).

---

## 1. The ordered phases of one real run

### Wrapper `run-opportunity!` (full_loop_runner.clj:6291)

| # | step | file:line | reads | writes / external effects | hands on |
|---|------|-----------|-------|---------------------------|----------|
| 0a | seat registration `ensure-dispatch-seat!` | 6316 (def 128) | Agency `/api/alpha/agents` | POST wm-full-loop seat (idempotent, 409 ignored) | — |
| 0b | source-drift check `refuse-on-runner-source-drift!` | 6320 (def 525) | this file's own source bytes | refuses on drift | `:loaded-code-identity` into opts |
| 0c | repair-discharge catch-up `discharge-receipt/catch-up!` | 6322 | repair store + repo | may record discharges | — |
| 0d | state atoms created | 6307–6312 | — | — | `participants/state`, `declaration-reads/state`, `habit-reads/state`, `job-liveness/state`, `scan-report/state`, `preference-refresh/state` (all atom nil at start; threaded INTO opts and read by later phases and by `persist-run-record!`) |
| 0e | core run, wrapped in binding of `cascade-sources/*read-occurrences*` and `input-receipts/*habit-reads*` | 6324–6327 | — | — | result map |
| 0f | catch → initialization close + repair finding | 6330–6385 | — | repair finding, brief item, phase :opportunity :end | — |
| 0g | `persist-run-record!` | 6440 (def 736) | everything below | **the run record file** `data/wm-runs/tick-run-record-<run-id>.edn` (only place most of the run's state is ever persisted, and only at the END) | — |

### Core `run-opportunity-core!` (full_loop_runner.clj:4217)

In-memory state created at 4220–4325: `phase-events`, `phase-context`
(opportunity-id, trigger, attempt-id), `checkpoints` (atom map
checkpoint→cell), `checkpoint-events`, `selected-entity-belief`,
`action-occurrence`, `d-task-context`, `pending-selection`,
`selection-persisted?`, `dispatched-turns`, `standing-readback-state`,
`effective-configuration`, `closing?`, `job-text-records` (4309). The rest
of the run is LET-BOUND VALUES, not atoms: `roster`, `code-state`,
`time-cell`, `start-event`, `attempt-id`, `open-stop-lines`,
`judgement0-base`/`judgement0`, `entry` (the selection), `target`,
`reviewer`, `construction`, `interpretation`, `author-job`, `review-job`,
`commit/repo/files`, `artifact-binding`, `witness`, `reviews`, `revision`.
These exist only on the click thread's stack.

Phases in order (each `run-phase!` emits a start/end line to stdout, the
phase log, the tripwire and futon3c's phase sink — see `emit-phase!` 323,
`run-phase!` 344):

1. `:agent-readiness` 4253 — reads the Agency roster.
2. `:code-state` 4260 — `git` over the primary repos.
3. cohort attempt start `cohort/start-attempt!` 4298 (NOT a run-phase;
   full_loop_cohort.clj:522) — **writes** the `001-time-step.edn` checkpoint
   into the cohort attempt dir and MINTS the attempt id
   (`data/wm-full-loop-machinery-*/<cohort>/<attempt-id>/`).
4. `:substrate-preflight` 5131.
5. `:preference-refresh` 5137 (c-vector refresh; state into
   `:preference-refresh/state`).
6. `:stop-line-memory` 5143 — repair/open-obligations read.
7. `:selection` (~5147–5200) — the judge (`judge-fn` →
   `futon2.report.war-machine/generate-war-machine` via
   full_loop_runtime.clj:9); since PROOF-2b may run the interpretation ask
   and re-run the decision in the same click. Emits the `:selection`
   checkpoint cell (via `persist-selection!` 4312 into `pending-selection`).
8. `:agent-readiness` (reviewer wake) 5434.
9. `:construction` 5499 (+ `:interpretation` 5190-area only in :receipt
   mode).
10. `:author-dispatch` 5764 → **Agency job** (job id on author-job);
    `:author-wait` 5793 (poll); on infrastructure failure
    `:author-retry-dispatch`/`:author-retry-wait` 5814/5822 (ONE retry,
    `default-author-infrastructure-retries`).
11. `:build-resolution` 5868 — `git` diff-tree over the author's commit;
    may run `:build-cure-dispatch`/`:build-cure-wait` 3022/3029
    (`default-build-cure-retries`).
12. revision (conditional, `run-revision-round` 2667):
    `:revision-dispatch`, `:revision-wait`, `:revision-build`,
    `:re-review-dispatch`, `:re-review-wait`.
13. `:reviewer-wait` 5929 (review job was dispatched inside build).
14. `:grounding` 6112 — `ground-commit!` writes into the substrate
    (futon1b) — an EXTERNAL mutation.
15. `close-core!` 4327 — emits remaining checkpoints (`:adjudication`),
    records the repair finding (unless grounded/already-satisfied), calls
    `cohort/close-attempt!` (writes `NNN-closed.edn`), queues the brief.
16. wrapper `persist-run-record!` 6440.
17. futon3c `close-click!` → `persist-click-run-binding!`
    (runner_service.clj:295) — writes
    `futon3c/data/wm-click-run-bindings/click-run-binding-<click-id>.edn`
    plus the run4 terminal/historical projections; then
    `publish-registry!` (runner_service.clj:98).

## 2. What is already persisted at each boundary — and what is NOT recoverable

Already persisted DURING the run (not only at close):

- Phase log lines: every `run-phase!` start/end, with phase, attempt-id,
  outcome (:ok/:error), duration, error class/message —
  `data/wm-full-loop-phases.edn.log` (and futon3c's phase sink → agent
  status). This is today's de-facto debugger trace.
- Tripwire observations (`tripwire/observe!` inside `emit-phase!` 323).
- Cohort attempt checkpoints (only when `:cohort? true`, which IS the
  config default, config :cohort? true at 331): `start-attempt!`,
  `append-checkpoint!` (full_loop_cohort.clj:602) and the `checkpoint!`
  closure (full_loop_runner.clj:4360) write each admitted cell
  (`selection`, `dispatch`, `build`, …) as numbered EDN files in the
  attempt dir. NOTE: "never publish it before an enabled durable append
  has succeeded" (4364) — the in-memory `checkpoints` atom mirrors the
  durable cohort cell.
- Agency job records (author/review jobs, their texts and artifact refs).
- Repair findings, discharge receipts, brief items.
- The run record + click binding — only at close.

NOT recoverable today (a stop-and-continue would need them):

1. The let-bound values listed in §1 — they reach a durable cell ONLY for
   the checkpoints that carry them, and only on cohort runs. There is no
   persisted image of, e.g., `construction` before the `:construction`
   checkpoint is written, and none at all of intermediate bindings
   (`open-stop-lines`, `ranked-for-review`, `discrimination`,
   `historical-admission`, `stop-line`).
2. `scan-report/state` — the judgement render data is captured in the
   atom (5205-area) and persisted only by `scan-report/retain!` at
   close. A mid-:selection stop loses it.
3. `effective-configuration` (partially in the selection cell),
   `dispatched-turns` (only in the record at close),
   `standing-readback-state`, `job-liveness/state`,
   `habit-reads`/`declaration-reads` receipts (run record only),
   `participants/state`.
4. The PROOF-2b interpretation-ask record (rides the selection cell only
   when selection completes; on a stop between ask and re-decide it is
   nowhere).
5. futon3c side: `!status`/`!completion` (runner_service) are in-memory;
   the click-id/run-id link is only written at close. A resumed click
   would need to pin the run id in opts (`:run-id` is accepted by the
   wrapper at 6295, so this part already works).
6. Non-cohort (canary) runs persist NO checkpoints at all mid-run.

So: the cohort attempt dir is already ~80% of the journal — the gaps are
the let-values between checkpoints, the state atoms, and non-cohort runs.

## 3. Existing resume/retry/re-enter mechanisms (and what each restarts from)

- **A new attempt (e.g. click 16's attempt-002)**: `cohort/start-attempt!`
  (full_loop_cohort.clj:522) claims a NEW ordinal in the cohort; every
  click begins at :agent-readiness with a fresh opportunity-id. Nothing is
  resumed mid-phase. Repair actions (`:repair-machine-failure`) are new
  attempts too.
- **author-infra retry** (`:author-retry-dispatch` 5814): same run, one
  re-dispatch of the author after an infrastructure failure. Restarts the
  dispatch, not the phase.
- **build-cure** (3022): same run, re-runs the BUILD step.
- **revision rounds** (`run-revision-round` 2667): same run, revise and
  re-review.
- **strategic-selection retries** (`strategic-selection!` 1210): retries
  the selection CALL on transport failures — again, redoes the phase.
- **wm_click_repair_loop.py** (scripts/, futon2 e1a9dd563 lineage):
  reloads committed click-path namespaces into the serving JVM, fires NEW
  clicks via `wm_click.sh`, writes abstention debugger pages
  (`wm_click_debugger.bb` reads the CLOSED run record). It repairs between
  clicks; it never continues one.
- **wm_click.sh**: preflight + fire + wait. No resume.
- **runner_service `await-click!` / `!status`** (runner_service.clj:506):
  in-process observability of the running click (phase, thread stack) —
  the closest thing today to "see where it stopped".

Conclusion: every existing mechanism restarts a phase or a whole run.
Nothing continues a stopped run from its phase with the same attempt id.

## 4. Phase idempotency on continue

Safe to re-execute (read-only or self-neutralizing): :agent-readiness,
:code-state, :substrate-preflight, :preference-refresh,
:stop-line-memory, :selection (mostly — caveats below), :construction,
:reviewer-wait/:author-wait when reduced to POLLING a saved job id,
interpretation publish (`wi/publish!` republishing the same
interpretation is a no-op by design).

NOT safe — must be skipped using the saved result:

- `:author-dispatch` / reviewer dispatch: an Agency job already exists;
  re-dispatching spends another author turn. Continue must reuse the job
  id and only poll.
- `:build-resolution` / build-cure / revision: the commit may already
  exist; `resolve-target-build` must run on the saved artifact ref, not
  re-derive from a pre-dispatch head.
- `:grounding` (`ground-commit!` 6112): mutates the substrate; re-running
  can double-insert. Must reuse the saved witness.
- `checkpoint!` appends (cohort): `append-checkpoint!` refuses
  order-regressions and would duplicate cells; on continue a checkpoint
  whose cell already exists in the attempt dir must be skipped, not
  re-appended.
- repair finding record, brief queue, terminal receipt
  (exactly-one-per-record contract), `persist-click-run-binding!`
  (has its own duplicate guard, runner_service.clj:306 `duplicate-clicks`).
- `cohort/start-attempt!`: on continue the attempt already exists; calling
  it again mints a new attempt id — exactly what "no new opportunity id"
  forbids.

## 5. The smallest change: stop-at-failure + continue-from-phase

All changes to `run-opportunity!` and its phases; no new runner. Ordered
handoffs (one behaviour + one named bad-case test each):

- **H1 journal** — `run-phase!` (344) already funnels every phase; add an
  optional `:journal-dir` opts key: on each phase's :ok end, write
  `data/wm-run-journal/<run-id>/<nn-phase>.edn` containing the phase's
  OUTPUT bindings and a deref of the state atoms (this is the cohort
  checkpoint cell content, generalized to non-cohort runs). Bad case: a
  run stopped after :selection has a journal whose :selection entry
  reproduces the selection cell byte-for-byte; a canary (non-cohort) run
  has one too.
- **H2 failure snapshot** — the phase :error path (361 in `run-phase!`) and
  the two close catches (outer 6168/6300) write the journal entry
  `{:failed-phase .. :failure-kind .. :error-data .. :atoms derefed}`
  BEFORE the repair finding. Bad case: a judge that throws at :selection
  leaves a journal naming :selection with the abstention state, and the
  repair finding cites the journal path.
- **H3 resume entry** — `run-opportunity!` accepts `:resume
  {:run-id .. :attempt-id .. :journal-dir ..}`; the wrapper reads the
  journal + the cohort attempt dir, rebuilds `checkpoints`/`pending-selection`
  /the let-bound inputs each skipped phase produced, and SKIPS phases
  whose checkpoint/journal entry exists (never re-append). Bad case:
  resume of a run stopped at :reviewer-wait polls the SAVED review job id
  and never dispatches (assert dispatch-fn is never called); resume
  stopped at :author-wait reuses the author job id.
- **H4 identity pinned** — on resume, opportunity-id, attempt-id and run-id
  come from the journal; `cohort/start-attempt!` is skipped;
  `append-checkpoint!` continues in the SAME attempt dir (its numbering
  continues from the last event). Bad case: resumed run's :closed
  checkpoint lands in the original attempt dir and `attempt-summary`
  shows one attempt, not two.
- **H5 futon3c surface** — `click!` (runner_service.clj:533) threads
  `:resume` into the runner opts; `persist-click-run-binding!` records the
  original run id (the duplicate guard keys on run id, so a resumed click
  must be exempted/reconciled there — explicit check in the handoff). Bad
  case: a resumed click's binding cites the ORIGINAL :run/id and is not
  read as a duplicate click.
- **H6 operator read-out** — `wm_click_debugger.bb` (or a sibling) gains a
  journal mode: `--journal <run-id>` prints phase timeline, failed phase,
  failure kind, saved cells, and the resume command line. Bad case: the
  page for a run failed at :grounding names :grounding and prints the
  saved witness.

## 6. What an operator would type

Today (no continue exists):

- start: `scripts/wm_click.sh --run` (or `POST :7070/api/alpha/wm/click`),
  or the loop: `scripts/wm_click_repair_loop.py run --clicks N`.
- see where it stopped and why: `tail data/wm-full-loop-phases.edn.log`
  (the `:outcome :error` line names the phase, attempt id, error class);
  the run record `data/wm-runs/tick-run-record-<run-id>.edn` `:failure`
  `{:kind :stage :error :cause :detail}`; the cohort attempt dir
  `data/wm-full-loop-machinery-*/<cohort>/<attempt>/*.edn` (checkpoint
  cells, in order); abstentions via `bb scripts/wm_click_debugger.bb
  <run-record>`.
- continue: NOT POSSIBLE today. The only move is a new click (new
  opportunity id, phases redone) after repairing — which is what
  wm_click_repair_loop.py automates.

After H1–H6:

- start: unchanged.
- see: `bb scripts/wm_click_debugger.bb --journal <run-id>` (H6) — or the
  same file by hand under `data/wm-run-journal/<run-id>/`.
- continue: `scripts/wm_click.sh --run --resume <run-id>` (H5 threads
  `{:resume ...}`), which reloads repaired code first exactly as the loop
  already does, then continues the SAME attempt from the failed phase.

## Not determined (say so, don't guess)

- Whether `scan-report/retain!` is safe to re-run on a resumed close (its
  freshness check was not traced); H2 snapshots the atom so the close can
  reuse it, but that path needs its own test.
- Whether the futon3c run4 projections (terminal/historical) tolerate a
  second write for the same run id; only the click-binding duplicate guard
  (runner_service.clj:268–306) was read.
- How the in-process strategic-selection seam (runner_service.clj:254
  `in-process-selection`) caches judge results across its internal
  retries; relevant only if :selection itself must become resumable
  mid-judge.
- Exact line drift: futon2 is edited in parallel; the line numbers above
  are the commit-time truth and may have moved by a few lines.
