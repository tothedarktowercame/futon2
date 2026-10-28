# DISCOVERY — inner loop on a caller-supplied task (2026-10-28)

Discovery only. No edits, no reloads, no clicks fired. Compiled by zai-3 for
claude-11. futon2 HEAD during discovery: `af7ff3fd80331cccb71c45e746d159f679807d74`
(inbox-zero: promote 1 path(s) for claude-11); futon2 tree clean except
untracked `evidence/*.source` blobs.

Context: `a04b157d253361a0b435af230f6a9fff58bd4c1b` (2026-10-28 04:43:46Z,
"Remove outer selection machinery; require a caller-supplied task") deleted the
outer selector and friends; `5429f0b2cb480000addd3e877069d5f5c7bf112d`
(2026-10-28 04:50:26Z, "Arbitrate META injuries before the no-task absence")
moved the no-task guard into the ordinary path.

## 1. Entry — can a caller pass a task id through POST /api/alpha/wm/click?

**No.** The route is wired at `futon3c/src/futon3c/transport/http.clj:12990-12991`
(`(= "/api/alpha/wm/click" uri)` → `handle-wm-click-start`, defined at
http.clj:10056). `handle-wm-click-start` builds `legacy-opts` from the JSON
payload in one `cond->` at **http.clj:10076-10109**. The payload keys it
transfers are exactly:

- `:interpretation-seats` (10077-10079), `:g-law` (10081)
- `:author` (10084-10085), `:reviewer` (10087-10088), `:repair-reviewer`
  (10090-10091), `:run-id` (10093-10094), `:trigger` (10096-10097)
- `:measured-acquisition` (10000-10001 region, `(true? ...)`)
- `:flight` from `:flight-edn` (10104-10110) — a flight is a `{:target … :wants …}`
  map, not `:task`.

**There is no clause for `:task`.** A JSON body `{"task":{"id":"…"}}` is silently
ignored at the payload→opts mapping, http.clj:10076-10109. Downstream nothing
re-adds it:

- `base-opts`'s `:acceptance-payload` select-keys (http.clj:10153-10156) lists
  `[:author :reviewer :repair-reviewer :run-id :trigger :measured-acquisition
  :flight-edn :issuing-caller :g-law :interpretation-seats]` — no `:task`.
- `futon3c.wm.runner-service/run-click!` (runner_service.clj:1408-1416) passes
  `(configured-runner-opts opts)` straight to
  `futon2.aif.full-loop-runtime/run-opportunity!`; `configured-runner-opts`
  (runner_service.clj:675-693) only applies machinery-cohort binding and
  `futon2.aif.full-loop-runner/config` (full_loop_runner.clj:326-341, env-driven
  defaults) — neither injects `:task`.

So today the with-task path is reachable only from in-process callers that hand
`{:task {:id …}}` in opts directly (e.g. tests, or a caller of
`futon2.aif.full-loop-runtime/run-opportunity!`), never via the HTTP click route.
The HTTP boundary is where a task id is dropped: http.clj:10076-10109.

## 2. Loaded code — is the serving JVM post-deletion?

Asked the JVM read-only via `futon3c/scripts/proof-eval.sh` (Drawbridge, port 6768).

- `(find-ns 'futon2.aif.meta-live-outer-selector)` → **nil** (deleted selector ns
  not loaded). `futon2.aif.meta-live-outer-selector-test` IS still loaded (a test
  namespace; its src counterpart is gone).
- Of the src namespaces deleted by `a04b157d2` (assessed-policy-basis,
  integrated-controlled-sut, meta-adapter-discovery, meta-decision-manifest,
  meta-live-outer-selector, meta-outer-policy, meta-policy-constructor,
  outer-canonical-g/-selection, outer-cascade, outer-learning-runtime, outer-loop,
  outer-objective-certificate/-sut, outer-policy-evidence/-provider/-objective/
  provider-runtime, outer-selection-trace, outer-target-belief/-score,
  outer-task-selection, q10-arrangement-census, q8-frontier/-certificate,
  q9-factorization, selected-outer-cascade-policy, target-reading-preparation):
  **none remain loaded** except `futon2.aif.wm.family-selection` — which is NOT
  actually deleted (its file `src/futon2/aif/wm/family_selection.clj` exists on
  disk; the commit's 136-line diff was a rewrite, and inner family selection was
  retained per the commit message). My earlier "still has
  `retained-outer-selector`" var probe was a false positive: `ns-resolve` via
  `ns-map` returns vars left over from pre-reload loads; var presence is not
  evidence of loaded version.
- Authoritative check: `futon2.aif.load-identity` (src/futon2/aif/load_identity.clj).
  Its live registry reports `futon2.aif.full-loop-runtime` **:current**, captured
  **2026-10-28T04:50:55.366846946Z** (after both a04b157d2 04:43:46Z and
  5429f0b2c 04:50:26Z), sha256 `b7004970…fda` == fresh disk read. Also :current:
  full-loop-runner (04:50:54Z), wm.family-selection (04:50:54Z),
  futon2.report.war-machine (04:50:55Z), meta-pipeline-selector (2026-10-27),
  wm.family-canonical-g (2026-10-24). `li/report` shows **zero :stale** entries;
  `composition-status` → `{:status :current :defects {}}`. The new
  `futon2.aif.ticket-readiness-inputs` ns is loaded.

So: the serving JVM runs the post-deletion code. No changed namespace was found
whose loaded version predates a04b157d2 (the load-identity registry covers the
click-path set; a few namespaces touched by the commit, e.g. q2-construction,
selected-target-reading, cascade-decision, are outside `required-sources` and so
are not digest-checked by that mechanism — but cascade-decision's post-deletion
`:task` behavior is on disk and its caller full-loop-runtime is current).

## 3. Inputs the with-task path requires before choosing a tactical cascade

`run-ordinary-opportunity!` (full_loop_runtime.clj:504-512) requires
`(get-in opts [:task :id])`, else `{:absent :no-task-selection-policy}`. The
with-task body (full_loop_runtime.clj:514-570) installs defaults then hands to
`runner/run-opportunity!`. The tactical-cascade decision itself
(`futon2.aif.wm.cascade-decision/cascade-decision`, cascade_decision.clj:1346-1360)
re-checks `(get-in opts [:task :id])` and then:

Per-task (must exist for the supplied target id):

1. **A source row for the target** — `target-policy-field`
   (cascade_decision.clj:1006-1028) filters `(:target-sources assembled)` by
   `(:target-id row) = (get-in opts [:task :id])`. Rows are built by
   `construction-inputs/target-source-declarations` from missions/tickets/proposals
   (scripts/futon2/report/war_machine.clj:6628-6648); a target with no row gets
   `:target-source-row-absent` (cascade_decision.clj:1019-1022) → typed family
   failure.
2. **A current target reading** — `family-selection` reads
   `reading-registry/current-reading` under
   `/home/joe/code/storage/wm-target-readings/targets/<sha256(target-id)>`
   (target_reading_registry.clj:15, 87-92, 108-127), matched to the **current
   excerpt digest of the target's source file** (`excerpt-digest`,
   target_reading_registry.clj:84-94). Missing/stale →
   `:no-current-target-reading` / `:stale-target-reading` (per-task).
3. **Occurrence squares** — `:require-occurrence-squares?` is forced true at
   full_loop_runtime.clj:531; family_selection.clj:170-192 looks up
   `square-registry/current` under
   `/home/joe/code/storage/wm-occurrence-squares/<digest(target)>/<analysis-digest>.edn`
   (occurrence_square_registry.clj:10-15) pinned to the reading's
   `analysis-digest`. Absent squares with the requirement on → typed family
   failure (per-task).

Global (defaults; exist on disk):

4. **Pinned pattern graph** — `/home/joe/code/storage/operator-turns/mined-pattern-graph.json`
   (full_loop_runtime.clj:39-40; loaded once per field, cascade_decision.clj:1013).
   On disk, 1,467,615 B, mtime 2026-10-01.
5. **Cascade feedback ledger** — `futon2/data/wm-pattern-feedback/events.edn`
   (cascade_feedback.clj:19-21). On disk, 281,840 B, 2026-10-26.
6. **Outer-learning manifest** — `futon2/data/wm-learning/run-record-manifest.edn`
   (outer_learning_manifest.clj:11-13). On disk, 33,657 B, 2026-10-27.
7. **Ticket-queue declaration** — `ticket-queue/read-declaration`
   (cascade_decision.clj:1359-1361; ticket_queue.clj:44-47, live file or
   resource `wm/ticket-queue.edn`). Global; present (serving registry loads it).
8. **Pattern library root** `/home/joe/code/futon3/library`
   (selected_reading.clj:30, occurrence_square_registry.clj:11) — for the Q2
   census and square text resolution. Global.
9. Runtime-minted (not on-disk preconditions): `:run-id` (auto-minted,
   full_loop_runtime.clj:518-520), `:selection-timing/state`,
   `:click-budget`, `:selection-counter` atoms (full_loop_runtime.clj:535-549).

## 4. A first task from the live Giraffe ready set

`/home/joe/code/storage/giraffe/20261028T145255.009Z-cab4c7c6/pass.edn`
(`:counts {:open 24, :ready 13 …}`), the 13 `:status :ready` tickets:
T-devmap-forward-model-calibration, T-car3-queue-routing-design-note,
T-evidence-pinned-to-mutable-prose-26082026, T-fail-activation-authority-unavailable,
T-fail-agent-not-found, T-fail-invoke-interrupted, T-fail-operator-cancelled,
T-forward-model-vs-active-work, T-jvm-provenance-before-multi-jvm,
T-strategic-cascade-emits-disconnected-patterns, T-wm-wrong-corpus-26082026,
T-zai-chat-transient-timeout, T-futon1b-evidence-index-deep-bypass.

**None of the 13 has every per-task input on disk.** For every one of them the
missing inputs are the same two, and they are the gating ones:

- **Target reading**: `sha256(target-id)` has no directory under
  `/home/joe/code/storage/wm-target-readings/targets/` for any of the 13
  (verified by computing `sha256sum` of each id and checking the 29 existing
  dirs — all 29 belong to `M-*` mission ids, e.g. `003f55a8…/` →
  `:target-id "M-custom-harness"`). So `:no-current-target-reading`.
- **Occurrence squares**: no matching dir under
  `/home/joe/code/storage/wm-occurrence-squares/` for any of the 13 →
  `:absent` squares, and with `:require-occurrence-squares?` forced true that
  is a typed family failure.

Additionally, substrate visibility differs: asked the live JVM
(`mission-registry/load-tickets`, 176 live tickets), 10 of 13 ARE live tickets
(car3-queue-routing-design-note, evidence-pinned-to-mutable-prose-26082026,
fail-activation-authority-unavailable, fail-agent-not-found,
fail-invoke-interrupted, fail-operator-cancelled, jvm-provenance-before-multi-jvm,
strategic-cascade-emits-disconnected-patterns, wm-wrong-corpus-26082026,
zai-chat-transient-timeout). NOT live: T-devmap-forward-model-calibration,
T-forward-model-vs-active-work, T-futon1b-evidence-index-deep-bypass). The
non-live three also lack a source row path, so for them even step 1 of §3 fails.

All global inputs (§3 items 4-8) exist on disk with recent mtimes.

## 5. Last clicks

- **Last click of any kind**: click state
  `futon3c/data/wm-click-acceptances/click-wm-retry-repair_2f682638…-2-patch-87c10683….edn`,
  completed **2026-10-28T00:07:36.939Z**, `:status :completed`, `:outcome :unknown`,
  runtime-disposition `{:status :refused :reason :meta-self-heal/retry-attribution-refused}`
  — a META retry child, `:ordinary-selection-called? false` pattern; never
  reached the ordinary path. Its parent ordinary click
  `click-wm-click-0f7da372-e078-4845-96a6-3c6014a27662.edn` (started
  2026-10-27T23:35:33Z, completed 2026-10-28T00:07:28Z) also routed to
  `:dedicated-meta-self-heal` with `:ordinary-selection-called? false`, ending
  `{:report-card {:status :failed :reason :verified-run-record-unavailable}}`.
- **Last click that reached ordinary (with-a-selected-task) selection**: the
  tick run record `/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-10-27-fd5d1d5f-efe7-4d3e-aae2-1f860f342c40.edn`
  (started 2026-10-27T22:45:35.898Z, written ~23:04, `:status :written`, substrate
  outcome `:ok`). It carries a full `:selection {:selected {:task
  "M-recommendation-bindings" …}}` — i.e. it ran under the OLD outer selector
  (pre-deletion). It was produced inside click
  `click-wm-click-1d974b05-4221-42b8-b38c-5bf749efcf3c.edn` (completed
  2026-10-27T23:05Z), whose click-level verdict was `:outcome :incomplete`.
- **Any click since `a04b157d2` (04:43:46Z)?** **No.** The newest artifact in
  `futon3c/data/wm-click-acceptances/` is dated Oct 28 00:07, and the newest
  run record in `futon2/data/wm-runs/` is `tick-run-record-2026-10-27-fd5d1d5f…`
  (23:04). Both predate the deletion. The with-task path
  (`run-ordinary-opportunity-with-task!` with a caller-supplied task) has
  therefore never executed in production.

## Bottom line

Two things stand between today's machine and one working click on a
caller-supplied task: (1) the HTTP click boundary never transfers `:task` from
the request body into runner opts (http.clj:10076-10109), so no external caller
can supply one; (2) no candidate task has the per-task inputs the tactical
cascade requires — no Giraffe-ready ticket (indeed no `T-*` ticket at all) has a
target reading or occurrence squares under the default registries; only `M-*`
missions have readings, and the newest of those predates the deletion.
