## Active Trial five — selection held

Run `2026-10-22-02587641-d615-4a5b-8bc8-a3df48e2ab45` selected
`M-stack-geometry`, then stopped on `:wm/outer-learning-not-consumed`.
`ordinary-trial-5/selection-stop.edn.gz` freezes the complete failure; its
keyword encoding and SHA256 are recorded in `selection-stop.json`.
The source guard repair succeeded on the same run. Five project Trials remain.
Zai5 is repairing the legacy consumption certificate against the actual
retained ordinary-policy evidence. Do not start another Trial or bypass the
certificate. See CONTINUE.txt for the exact job and restart conditions.

## Deployment completed — supersedes the pending status below

Joe requested review and deployment. Futon2 `d443921e6` and futon3c `e65ef06be`
are deployed; `fresh-store-deployment/review-result.json` records review fixes,
focused validation and broader test limitations. Live preflight uses the new
empty store, source identity is current, and no Trial was issued. Six remain.
All 90 historical data files are unchanged; only `ARCHIVED.md` was updated.
The next step is one ordinary Trial under the debugger, freezing each boundary.

## Current frontier — 2026-10-22, supersedes historical sections below

One useful ordinary Trial remains unfinished. **Six of the ten project Trials
remain**; four admissions have been used. No further Trial has been issued by
codex-11 since the recovery admission recorded below.

- Recovery core `f446e99ed` and futon3c adapter `e19efbe7f` were deployed.
  Trial `wm-click-2a4789f3-70f6-4a87-9919-e03e5fa94af2` observed the failed child
  and exhausted its repair budget honestly (ledger ordinals 51/52). Commit
  `398695e09` freezes the replay evidence. This was not useful task completion.
- Test isolation `4dae9104e` is on main. The independent review recorded two
  preservation failures because a concurrent accidental whole-suite invocation
  appended observations 53–56. Zai3 confirmed attribution; `56f1f9fb5` preserves
  its log and report. Do not relabel the failed review as a pass.
- Joe's newer ruling, relayed by Claude2, supersedes ledger retraction and
  historical recovery: retain the old chain intact and start a fresh live store.
  See `fresh-store-supersession.json` and
  `/home/joe/notes/packets/2026-10-22-fresh-tripwire-ledger.md`.
- Zai4 job `invoke-1792641934169-2264-cdb96c5d` implements the new store;
  Claude2 owns review and canonical operational reload. At this update the job
  is still running with unresolved validation failures, not accepted/deployed.
  Do not touch its files or issue a Trial before review and deployment.
- Route repair `839fb6c01` is preserved in its sibling worktree and result JSON;
  codex-11 has not integrated or independently approved it. Do not resume the
  old transaction merely because an older park completes.

After Claude2 confirms review and deployment, verify runtime store defaults,
archive preservation, debugger state and admission budget; run one ordinary
Trial under the debugger and freeze successful boundaries before advancing.
The capture helper remains a draft without a live boundary capture.
Keep work order `wo-da02b427-e639-4ac2-b3ed-a7e6325d665b` open until useful
execution or an explicitly reported external blocker.

## Historical record (budgets and pending-work statements below are superseded)

# One-click frontier — 2026-10-22

Objective: one useful production click, retaining successful sections before
repairing the next failure. This directory does **not** establish that objective
as complete. Between-click learning/carry is a separate effort.

## Repaired recovery section

Core f446e99ed and adapter e19efbe7f are deployed. Trial
`wm-click-2a4789f3-70f6-4a87-9919-e03e5fa94af2` consumed the actual failed
child evidence: ordinal51 records retry-inconclusive; ordinal52 closes the
exhausted repair as abandoned. This fixes the observation-starved section;
it is **not** useful ordinary task completion. **Six project Trials remain.**

`recovery-section-snapshot.json` pins the before/after artifacts. Reproduce
this section without agents, live writes or a Trial:

```
clojure -M scripts/wm_replay_failed_child.clj
```

`recovery-replay.log` records matching live transition fields and zero
actuation calls. Other injuries remain active; the next routing step includes
an inactive persisted transaction and test-origin records requiring diagnosis.
Zai3 is checking/repairing transaction retirement; Zai1 is investigating the
production ledger's /tmp test-origin observations. Do not silently delete
these entries or treat abandonment as mission progress.

## Latest requested test

Joe requested another click after the initial freeze. It also stopped at
`:awaiting-retry`, without ordinary execution. **7 project clicks remain.**
See [requested-click-outcome.md](requested-click-outcome.md) for the subsequent
observation; the original frozen accounting below describes the first two.

## Observed admissions

Two of Joe's ten project clicks were charged:

- `wm-click-3041d9dc-2084-493a-8735-241ed1b5bd94`: dedicated META repair
  queued child `wm-requeue-7-4-repair/239cf98836afc977cd589141c224ec3f4bca83dc`.
  The child failed before its first attempt because committed canonical runner
  source changed after the serving JVM loaded it. `accepted.json` preserves the
  structured source mismatch; `failed-child.edn` preserves durable acceptance.
- `wm-click-d487dbd2-a56c-431e-b948-a7d7658b7feb`: after canonical reload,
  META returned `:awaiting-retry`. No ordinary selection ran. Durable result is
  `second-parent-acceptance.json`. No run record was returned.

Eight project clicks remain. Global consumed count after these admissions was
149 of 158 (one available click predates this project). No further admission
should be issued merely to repeat the awaiting-retry state.

## Source-entry repair

`check-runner-source-phase!` places the existing source guard inside the normal
restartable phase boundary, before attempt creation. A held source drift can
be rechecked after a recorded canonical reload via `:retry`. A fresh guard
check after the breakpoint prevents a supplied debugger value from becoming
code identity, and detects changes while a successful check was held.

`source-restart-test.log`: new focused namespace, **2 tests / 8 assertions,
0 failures / 0 errors**. The positive test reproduces the actual drift digest
shape, repairs the source observation, retries the same held run, and enters
the next section exactly once without repeating its preceding section.
This is a component test, not evidence of a live completed click.

## Remaining recovery work

The failed child has no run record, so the coordinator never ingested its
outcome. Its acceptance is terminal and idempotent; another parent admission
cannot relaunch it. Zai-8 is implementing a durable child-outcome observation
port in an isolated worktree (`recovery-repair-request.txt` and dispatch).
Existing retry limits and attribution must remain intact. The child is already
at the recorded requeue limit; a legitimate exhaustion outcome must be
reported as incomplete, never relabelled as success.

`wm_capture_held_section.py` is still an unverified live-capture draft; it has
not captured a live boundary and is not part of the verified source repair.
