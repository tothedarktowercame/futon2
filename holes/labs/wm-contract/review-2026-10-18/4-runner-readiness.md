# Reviewer 4 — runner and pre-click readiness

Reviewed at futon2 HEAD `0975c7df7` (2026-10-18 03:13 UTC). Serving JVM state read by GET only
(`/api/alpha/wm/click`, `/api/alpha/agents`). No proof-eval, no tests run unless stated.

## Findings

### F1. Typed failure outcomes outside `cohort/outcome-kinds` still degrade to `:build-failed / :invalid-close-outcome` and lose the real failure kind (ee7275ffc fixed one member of the class)

- Where: `full_loop_runner.clj:4334-4342` (`outcome-from`) maps only `:pinned-selection-refused`,
  `:agent-job-stalled :construction-failed :grounding-failed :policy-nondiscrimination :incomplete`;
  everything else passes through raw to `close!` (`:7638`). `full_loop_cohort.clj:690,722-727`
  rejects any outcome not in `outcome-kinds` (`:38-48`) with `:invalid-close-outcome`; the
  `close!` fallback (`full_loop_runner.clj:6044-6160`) then takes `refusal-kind` from the
  *close* exception's ex-data, i.e. `:invalid-close-outcome`, not from the original failure.
- Raw outcomes thrown on the click path that are not in `outcome-kinds` and not mapped:
  - `:selection-validation-failed` — `wm/loop_node_exercise.clj:153` (`require-complete!`, the
    `:required-loop-node-unexercised` gate);
  - `:tripwire-tripped` — `tripwire.clj:1066`;
  - `:breakpoint` — `full_loop_runner.clj:2262` (`:wm/outer-selection-complete`, if the operator
    aborts rather than continues).
- Confirmed on real data: click 1's run record
  `data/wm-runs/tick-run-record-2026-10-17-0daab1e4-….edn` has
  `:failure {:kind :invalid-close-outcome … :cause {:absent :close-without-exception} :detail {:absent :no-error-data}}`
  and `:terminal-receipt … :failure-kind :invalid-close-outcome`. The real held condition,
  `:required-loop-node-unexercised`, is not the recorded failure kind. The TN reports click 1
  as "build-failed", which is this fallback, not the machine's classification.
- ee7275ffc is the same defect for `:refused`; its regression (`reading_refusal_terminal_test.clj`)
  pins only that one value. There is no test that the set of `:outcome` values the runner and
  its callees throw is a subset of `outcome-kinds` ∪ the keys `outcome-from` maps.
- Severity: likely-later (it fires whenever the loop-node gate, a tripwire, or an aborted
  breakpoint ends a click; it does not stop a click that otherwise succeeds). It degrades the
  record and the repair finding the debugger loop receives (finding kind `:invalid-close-outcome`),
  so a Zai repairer is sent to fix the close ledger instead of the gate that held.
- Confidence: confirmed for `:selection-validation-failed` (code path + click 1's record);
  plausible for `:tripwire-tripped` and `:breakpoint` (read the throw sites and `outcome-from`, did
  not trace every catch between them).

### F2. Every post-selection stage runs outside `run-phase!`, so its failures close the click instead of holding in the debugger; ee7275ffc then reports machine errors from cascade execution as `:abstained`

- The restart debugger only stops inside `run-phase!` (`full_loop_runner.clj:380-478`;
  `debugger/stoppable-failure?` is `true` for everything, `wm/debugger.clj:44-53`). These
  post-selection stages are called directly, not through `run-phase!`:
  - selected-target reading repair: `selected-target-reading-step` (`:2079-2105`), called at
    `:6331`. A non-published record leaves the judgement entry-less. The first selection phase
    deliberately suppresses its terminal condition when a target reading is needed (`:6305-6317`,
    "not yet terminal"), and nothing re-evaluates it after the reading fails;
  - occurrence-square authoring failure: plain `throw` at `:6390-6399`;
  - cascade execution: `wm-cascade-decision/execute-selected-cascade!` at `:6503-6505`, and the D4
    `:open-cascade-resume-fn` at `:6511-6521`;
  - the no-entry throw at `:6681-6684`, uncast/reviewer/nondiscrimination throws at `:6685-6720`.
- Evidence: clicks 3, 4 and 5 (runs `a9af4bb8`, `9e3d4379`, `173c5a65`) all have
  `:debugger-dwell-receipts []` and `:failure {:kind :abstained …}`. Their real defects
  (`:selected-target-reading-answer-invalid`, `…-publication-refused`) were repaired *after* the
  click ended, by the repair loop, not by `:retry` of a held phase. That is the opposite of
  ground rule 4 ("repair the held phase live in the debugger and `:retry`; do not abort and
  re-fire"). With the current code, that rule cannot be followed for any failure after outer
  selection.
- Masking (answers the ee7275ffc question): `execute-selected-cascade!` reports its typed
  *machine* errors with `:status` set to the error class (`fail` helper,
  `wm/cascade_decision.clj:1505-1510`: `:cascade-materialisation-error`, `:cascade-validation-error`,
  `:cascade-compilation-error`, `:cascade-evidence-error`), never `:status :error`. In
  `no-entry-failure-data` (`full_loop_runner.clj:2211-2239`) those fall through both the
  `:open-cascade` branch and the `(= :error (:status …))` branch into the ee7275ffc branch and
  become `{:outcome :abstained :failure-kind <kind>}` with no `:held-phase` and no `:repairable`.
  So `:identity-forged`, `:identity-verification-failed`, a validation clause failure, a
  compilation refusal, `:journal-entry-invalid` and `:capability-route-missing` all close as
  abstentions. The run record's `:failure :kind` for clicks 3-5 is literally `:abstained`, so a
  reader of the record, the report card or the cohort ledger sees a clean abstention; the typed
  kind survives only in `:failure-kind` on the sorry cell. Before ee7275ffc the same values
  degraded to `:build-failed`/`:invalid-close-outcome` (F1), which was at least loud.
- Test adequacy: `reading_refusal_terminal_test.clj` (ee7275ffc) asserts the outcome is *in*
  `outcome-kinds`, which `:abstained` trivially is; it does not assert that a machine error is
  distinguishable from a decision to abstain, nor that a debugger hold occurs.
- Severity: blocks-next-click in the sense the TN cares about: the next click that fails after
  selection will again end, spend its click, and need a new authorization, rather than holding for
  `:retry`. Confidence: confirmed by reading the path and by the dwell receipts of clicks 3-5.

### F3. The D4 same-click resume cannot change the result it retries, and its tests stub both the retry and the patch loader

This is the stage a live click reaches next. Production supplies no `:cascade-operational-authoring`
(`grep` finds it only in `cascade_decision.clj`), so `seam-authoring` (`wm/cascade_decision.clj:1471-1481`)
always returns empty clauses, every occurrence compiles as an open hole, and
`execute-selected-cascade!` returns `:wm/open-cascade-execution-error` (`:1665-1709`). The recorded
flight got past this only by supplying an offline authoring artifact.

- `open-cascade-resume-handler` (`full_loop_runtime.clj:104-134`) then asks Zai-2 to "implement
  resolutions … commit the source" and return a `patch {namespace,var,source-digest}`, has Zai-6
  review it, and `patch-loader` (`:84-102`) does `(require n :reload)` on the namespace the agent
  named, inside the serving JVM, during the click.
- The digest check reads `@load-identity/registry` (`:95`). Only namespaces that call
  `load-identity/register!` have entries; a namespace Zai writes will not unless the author
  knows to add that call, and the prompt (`:71-76`) does not say so. Likely result:
  `:patch-source-digest-mismatch` → `:hot-load-failed`.
- If the load succeeds, `hot-load!` (`wm/open_cascade_resume.clj:258-283`) calls the patch fn and
  records its product only on the ledger. The retry is
  `(fn [_] (execute-selected-cascade! entry opts))` (`cascade_decision.clj:1677-1688`). It ignores its
  argument and reuses the same `opts`, which still have no operational authoring, so it
  recompiles the same holes. `resume!` (`open_cascade_resume.clj:285-336`) accepts any map with
  the same `:cascade-digest`, so an unchanged open cascade comes back as `:status :resumed`. The
  runner then sees the open cascade again and closes `:incomplete` (`full_loop_runner.clj:2217-2221`),
  outside the debugger (F2).
- Tests: `full_loop_runtime_test.clj:44-80` stubs `:open-cascade-patch-loader (constantly identity)` and
  a retry that returns `{:cascade-digest d :outcome :redone}`; `open_cascade_resume_test.clj` uses
  a local `patch-loader` stub. Neither the real loader nor the real retry is exercised, so
  "resumed" is shown only for a stubbed retry.
- Separately, an agent-chosen `require :reload` in the serving JVM during a click conflicts with
  the one-JVM policy (reload only reviewed master code) and with ground rule 1 (Codex owner
  decides what is built). I flag this as a question for codex-68. I make no proposal.
- Severity: blocks-next-click for any click that gets past reading publication and squares.
  Confidence: confirmed by reading the path end to end. Not run.

### F4. The "ambient `:no-selection` failures" in `full-loop-runner-test` hide something real: the runner's build → review → grounding → close tests have not reached those stages since 772590b0a (10-15)

- Ran one test narrowly (`-v …/open-repair-memory-preserves-selection-and-repair-requires-evaluator`,
  wrapped per the preamble): 20 assertions, 10 failures, 0 errors. The fixture decision
  `token-loop-exercised-decision` completes `:selection` with `:outcome :ok`, then the opportunity
  ends `:no-selection`. `:selected-action` is nil, no `:build` checkpoint, no review, no grounding.
  Expected values were `:grounded-progress`, `:verified`, `:build-failed`, etc.
- Cause (by reading): `selected-entry` (`full_loop_runner.clj:2140-2184`) now requires
  `canonical-f-consumed?` against the decision's `:policy-history-pin` and a discharged reading
  debt. 772590b0a (10-15 17:37, "Guard WM enactment on canonical prefix F") introduced that, and
  the pre-existing fixtures carry neither. The last passing warrant for this namespace is at
  `786ebd68c` (10-15 13:23, 223 tests / 0 failures). Every runner commit since then has had no
  passing end-to-end test of the post-selection half.
- Why it matters now: 0975c7df7 sends a `:cascade-ready` cascade into exactly this ordinary
  build/review/close path (`full_loop_runner.clj:6525-6527`). Its only regression
  (`cascade_runner_seam_test.clj` `authored-frontier-routes-into-the-existing-full-loop`) stops
  at a deliberate `:author-dispatch` fixture stop. So nothing tests a cascade through review,
  grounding, close and trace. The close path that the recorded flight names as the remaining
  frontier is the part with no live test.
- Also noted: on `:cascade-ready` the runner drops the cascade-execution result (routing, compiled
  digest, journal entries). `entry` is just `ordinary-entry` (`:6525-6527`), and
  `persisted-cascade-execution` is only written on the sorry branch (`:6640-6644`). Nothing on the
  build/close path appends `:verified-effect` entries to the execution journal that
  `recorded_tick_evaluation` requires. That is enactment/close territory, so I leave it to that
  reviewer. It is the next place I would look.
- Severity: likely-later as a test problem; blocks-next-click if a click does reach
  `:cascade-ready`, because that path is untested. Confidence: confirmed (one test run + reading).
  I did not run the other ~224 tests.

### F5. Serving-code readiness checks one file; this week's repairs are in namespaces no check covers

- `run_readiness.py` `serving_code_result` (`:210-221`) and `futon3c.wm.code-identity/status`
  (`code_identity.clj:77-97`) check only `src/futon2/aif/full_loop_runner.clj`. Tripwire T10's
  `runner-namespace-sources` (`tripwire.clj:43-48`) covers five namespaces. `load-identity/required-sources`
  (`load_identity.clj:10-92`) does not list any `futon2.aif.wm.*` namespace, `selected-target-reading`,
  `handoff-contract`, `occurrence-square-authoring`, `reading-policy-precision`, or
  `wm.cascade-capability-dispatch`. None of those files call `load-identity/register!`.
- Those are exactly the files repaired and hot-loaded for clicks 2-5 and after (6b9f02660,
  0a45789b8, c70495fc9, 03c8830af, 3037b082f, 0975c7df7). If one of them is committed but not
  reloaded, or reloaded from the wrong bytes, the readiness scan still says READY: the runner
  file matches (`355216cf…` loaded = disk at HEAD). Example at HEAD: 0975c7df7 (03:13 today)
  changed `wm/cascade_decision.clj` and `wm/cascade_capability_dispatch.clj`. Nothing that a GET
  can reach shows whether the serving JVM has them. I did not proof-eval to find out.
- Same point for warrants: `REQUIRED_WARRANTS` (`run_readiness.py:25-33`, unchanged since
  42ae8c179) omits `selected-target-reading-test`, `handoff-contract-test`,
  `wm.cascade-runner-seam-test`, `wm.open-cascade-resume-test`, `wm.cascade-capability-dispatch-test`
  and `full-loop-runner-test`. The TN asked for the selected-target-reading warrant to be in the
  click-4 scan. It is not in the list.
- Severity: likely-later (a stale load would produce yet another "new defect" click).
  Confidence: confirmed by reading.

### F6. The readiness scan checks only stages that have already failed; it probes nothing downstream

`run_readiness.py main()` (`:419-479`) checks: cast availability, contract freshness, trace-schema
readback, the 7 warrants, runner bytes, CTAU class emission (the click-1 failure), service
capacity, and a `make -n certify-run` dry run. It has no probe for selected-target reading,
occurrence squares, cascade execution/D4 (F3), capability routing, build/review, close or trace.
It does not read the click budget or the beta campaign status either. `wm_click.sh` reads the
budget and tripwires but by policy only prints findings (`wm_click.sh:9-17`). So the answer to
"does the scan check stages no click has reached" is no. A READY scan says nothing about whether
the click can get past cascade construction. Severity: blocks-next-click (as a readiness claim).
Confidence: confirmed.

### F7. Campaign authority: budget will stop the next click cleanly; beta will not stop, and the closed campaign's authority is still active

- Budget: GET `/api/alpha/wm/click` reports `allocated 86, consumed 86, available 0`. `wm_click.sh`
  exits 3 ("cannot launch") before any wake (`:66-79`). This is correct. The next click needs Joe's
  new allocation, which is an edit to `futon3c/src/futon3c/wm/ordinary_click_budget.clj` `allocated`
  plus a reload. `run_readiness.py` does not check the budget, so it can print READY at 0 available.
- Beta: `full_loop_runtime.clj:32-44` `ensure-reading-precision-campaign!` runs on every
  `production-defaults`. It loads the default campaign file `wm-readiness-2026-10-17-campaign.edn`
  if that file exists, and nothing marks the campaign closed. The TN closes the campaign and says
  "another live run … should be treated as a new readiness campaign". The code will still issue
  beta=1 declarations under the old campaign's authority for any new reading identity, up to the
  in-memory limit. So a new campaign will **not** stop on a missing declaration. It will run under
  the old authority without anyone having re-declared it.
- The limit is not durable. `!campaign` is a `defonce` atom (`reading_policy_precision.clj:34`),
  and `issued` resets to `{}` whenever the JVM restarts (the sha comparison then re-registers).
  "At most 4 identities" really means at most 4 per process lifetime.
- Slots are spent before they are needed. `cascade_decision.clj:1127-1135` calls
  `(precision/declaration-for identity)` eagerly on every `certify-reading-family` call, even when
  `carry-beta` will just carry the previous beta. Each selection, redecision or reading-redecision
  pass that scores a different identity uses up a slot.
- Severity: likely-later (it does not block a click, but it lets a click run under authority Joe
  closed). Rule 5 applies: whether beta is authorized for the next campaign bears on whether
  that click is valid. Confidence: confirmed by reading. Current issued count not observed (needs
  proof-eval).

### F8. Cast: three copies of the seat-readiness rule and two default casts (minor; explains the click-3 admission race)

- Seat-ready rules: `run_readiness.py:405-407` (status `idle` only); `wm_click.sh` `cast_report`
  and `runner_service.clj` `cast-preflight-refusal` (`idle` or `restored`); runner
  `available?` (`full_loop_runner.clj:1307-1309`, `idle` only, after a wake attempt for
  `restored`). A seat that just changed state can pass one check and fail the next. The worker
  then refuses before ration admission (`runner_service.clj:816-845`, `:wm-click-cast-not-admitted`).
  No click is spent, which matches the TN's account of click 3. This is handled safely.
- Default casts: `run_readiness.py` hardcodes Zai `zai-2/zai-6/zai-7` (`:34-35`).
  `wm_click.sh` defaults to `wm-author/wm-reviewer/wm-repair-reviewer` (`:37`), and
  `runner_service.clj:271-275` `automatic-cast` uses `wm-author/wm-reviewer`, which are Codex
  `gpt-5.6-sol` seats (`cast_registry.clj`). A click fired without explicit `--author/--reviewer`
  runs on a cast the readiness scan never checked. All six seats are currently idle and
  invoke-ready (GET roster). Severity: minor. Confidence: confirmed.

## What I checked and what I did not

Checked by reading: `full_loop_runner.clj` around `run-phase!` (380-478), `outcome-from`,
`no-entry-failure-data`, `selected-entry`, the selection → reading → squares → cascade-execution →
no-entry section (6270-6720), both close fallbacks (6044-6160, 7560-7660); `full_loop_cohort.clj`
`outcome-kinds`/`close-attempt!`; `full_loop_runtime.clj` in full; `wm/debugger.clj`;
`wm/open_cascade_resume.clj`; `wm/cascade_decision.clj` `execute-selected-cascade!` and
`certify-reading-family`; `reading_policy_precision.clj` campaign/declaration code;
`run_readiness.py`; `wm_click.sh`; `futon3c` `runner_service.clj` cast/admission/click!,
`code_identity.clj`, `cast_registry.clj`, `ordinary_click_budget.clj` (skim); `tripwire.clj` T10;
`load_identity.clj`; `A-self-heal.md`; the readiness TN; the recorded-flight design note. Commits
read as diffs: 42ae8c179, ee7275ffc, 0975c7df7. Live state through GETs only: click status/budget
and roster. Run records of clicks 1-5 grepped for failure kind and debugger dwell.

Ran: one test, `full-loop-runner-test/open-repair-memory-preserves-selection-and-repair-requires-evaluator`
(F4), with a temp invoke-jobs file.

Not checked: the current beta campaign issued count and which namespaces the serving JVM has
loaded (both need proof-eval); `wm_click_repair_loop.py` in detail; the rest of
`full-loop-runner-test`; enactment/close semantics past the routing hand-off (another reviewer);
the recorded-flight code; Requirements.lean clause by clause.

## Readiness verdict for my area

Not ready. The budget is 0, so the next click cannot start until Joe allocates more, and
`wm_click.sh` refuses cleanly (exit 3). Once the budget allows a click, the most likely next
failure is this: a click that gets past reading publication and square authoring reaches cascade
execution with empty operational authoring. It then goes through D4, which asks Zai-2 to commit
Clojure, hot-loads it through a digest check new namespaces cannot pass, and retries with
unchanged inputs (F3). The click then closes `:incomplete` without a debugger hold (F2). Any other
failure after selection also ends the click instead of holding it for `:retry` (F2), and
cascade-execution machine errors are recorded as `:abstained`, which makes them look like
deliberate abstentions in the record. The readiness scan would print READY through all of this:
it only probes the click-1 failure stage, checks one source file of the serving code (F5, F6),
and does not notice that the closed campaign's beta authority is still active (F7). The runner's
own end-to-end tests have not reached build/review/close since 10-15 (F4). The cascade-ready path
added at 03:13 today feeds into exactly that untested section.
