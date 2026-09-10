# RUN4 hand-selected execution-path reconciliation — 2026-09-10

Status: **NOT EXECUTED**. This is a read-only path audit at futon2
`f2819e92abc3378e96560f2413d80c66567a17c0`. No tick, click, lock, readiness
regeneration, registry/worklist edit, data write, or sealed-holdout read was
performed. The commands below are proposals, not evidence of a run.

## Finding

There is currently **no supported single path** that simultaneously:

1. pins a hand-selected task from a recorded shortlist;
2. dispatches that task to a real Codex or Zai worker; and
3. produces the RUN4 stepped pin plus wiring-conformance deposits.

The available paths each cover only part of that contract. They must not be
run consecutively and described as one joined execution.

### Certificate step path: pinned and depositable, but non-enacting

The RUN4 runbook identifies `wm_step.sh` as the supervised **non-enacting**
path that writes `:pin/accepted-steps`
(`runs/F2-run4-readiness/RUN4-execution-runbook-2026-09-09.md:26-31`). Its
`step` implementation accepts only label/evidence/pin-drift controls
(`wm_step.sh:215-223`), takes the run lock, and invokes
`futon2.run-tick-once` (`wm_step.sh:240-265`). That producer stamps
`:live-wire? false` (`scripts/futon2/run_tick_once.clj:277-291`); its advisory
cascades are held after the decision and nothing selects or enacts them
(`scripts/futon2/run_tick_once.clj:254-269`). Acceptance copies this diagnostic
step into a run store and advances the pin (`wm_step.sh:499-525,556-569`), then
the battery emits/transcribes the conformance deposits
(`wm_step.sh:450-479`). It has no worker or task-selection argument.

### Real worker path: actuating, but selector-owned and outside RUN4 stepping

The actual production click is in the one serving JVM. Starting
`clojure -M:wm-full-loop ...` as a click process is retired; the supported
entry is `POST :7070/api/alpha/wm/click` (`CLAUDE.md:6-22`; `deps.edn:13-21`).
The public POST accepts only `author`, `reviewer`, `repair-reviewer`, and
`trigger` (`../futon3c/src/futon3c/transport/http.clj:8221-8243`). Thus this is
a valid casting shape, including Zai author and distinct Codex reviewer:

```bash
# NOT EXECUTED — supported real-click shape, but AUTO-SELECTED, not RUN4 joined
curl --fail-with-body -X POST http://127.0.0.1:7070/api/alpha/wm/click \
  -H 'Content-Type: application/json' \
  --data '{"author":"zai-5","reviewer":"codex-17","repair-reviewer":"codex-1","trigger":"duree-click-on-demand"}'
```

The loaded runner really dispatches the selected mission to Agency
(`src/futon2/aif/full_loop_runner.clj:803-815,2813-2838`) and records
`:real-actuation? true` with the chosen roles
(`src/futon2/aif/full_loop_runner.clj:2595-2605`). However selection is made by
the War Machine judgment and validated strategic selector before the target is
derived (`src/futon2/aif/full_loop_runner.clj:2553-2586,2605-2649`). The CLI
likewise exposes role/batch/budget flags but no task/target/shortlist input
(`src/futon2/aif/full_loop_cli.clj:26-48,625-645`). Its run record is written
through its own route (`src/futon2/aif/full_loop_runner.clj:278-301`), not by
`wm_step.sh accept`; no present interface binds its click/run/attempt identity
to a RUN4 step pin or acceptance battery.

The older direct `wm-scheduled-run` path is now honestly named
`:wm-judgement-only`, while `:wm-scheduled` names the real full-loop tick
(`deps.edn:13-21`). The 2026-09-09 runbook's distinction remains substantively
right, but its direct-process launch discussion predates the one-serving-JVM
rule and is not a current launch recipe.

## Smallest missing seam

Add one reviewed, fail-closed **RUN4 task-pin adapter** to the serving click,
plus one acceptance bridge; do not add a second runner.

The click request should accept `task-pin` only as a path or digest-bound
record with this minimum shape:

```clojure
{:schema :wm/run4-task-pin-v1
 :run4-id "RUN4-..."
 :shortlist [{:task-id "..." :mission-id "..." :source-ref "..."}]
 :selected-task-id "..."
 :selection {:mode :operator-selected :selector "joe" :reason "..."}
 :casting {:author "zai-5" :reviewer "codex-17"
           :repair-reviewer "codex-1"}
 :source-pins {:futon2 "<sha>" :readiness "<sha256>"}
 :sha256 "<digest-of-canonical-record-without-this-field>"}
```

Required behavior is deliberately narrow:

- resolve the selected task to an already-addressable mission/action and
  refuse absent, ambiguous, stale, inadmissible, or selector-disagreeing
  mappings; record both the ranked machine choice and the operator exception;
- retain the normal construction, real Agency author dispatch, independent
  review, build, grounding, and delivery-QA gates—task pinning must not bypass
  them;
- execute under the existing run lock and redirect/freeze every mutable input
  needed by the RUN4 pin contract;
- emit one identity bundle joining task-pin digest, click ID, attempt ID,
  full-loop run ID, selected mission/action, author/reviewer jobs, commit,
  trace, and terminal outcome;
- teach `wm_step.sh accept` (or a new subcommand sharing its acceptance code)
  to validate that bundle and feed that exact trace/run identity into the
  existing battery before advancing `:pin/accepted-steps`.

Proposed operator surface **after that seam exists and is independently
reviewed**:

```bash
# NOT EXECUTED — PROPOSED; unsupported at current HEAD
curl --fail-with-body -X POST http://127.0.0.1:7070/api/alpha/wm/click \
  -H 'Content-Type: application/json' \
  --data '{"task-pin":"/home/joe/code/futon2/data/wm-step/RUN4/pin/task.edn"}'

# NOT EXECUTED — PROPOSED; unsupported at current HEAD
bash holes/labs/wm-contract/wm_step.sh accept-click \
  /home/joe/code/futon2/data/wm-step/RUN4 \
  /home/joe/code/futon2/data/wm-step/RUN4/click-result.edn RUN4-ID
```

Until both halves exist, the correct disposition is **NO LAUNCH PATH**, not
“run a click, then import its output.” Importing after the fact would not prove
that the pinned input governed the worker execution.

## READY versus organise/O4 commissioning

The current meter says `READY`, but defines that as intact/current acceptance
evidence—not permission and not a statement that a particular run qualifies
(`runs/F2-run4-readiness/READINESS.edn:135-153`). The proposed RUN4 config is
still `:PROPOSED-FOR-REVIEW`; its `:open-before-go` explicitly leaves Joe the
choice to run with organise open or commission it first
(`runs/F2-run4-readiness/RUN4-config-2026-09-09.edn:1-12,39-43`). The final
checklist records organise's naturalistic exemplar as executing and identifies
the still-needed basis/task choices (`RUN4-final-checklist-2026-09-09.md:28-38`).

The later artifacts sharpen the state: the post-repin authority-reader probe
observed all nine readiness lines green, while explicitly retaining O4 as
unexercised and organise closure unclaimed
(`runs/E-pre-go-live-post-repin-probe-2026-09-09.md:1-26`). The newest recorded
naturalistic construction also has `:o4 {:exercised? false}` with all four
before/after acting-order and score fields missing
(`runs/F12-organise/32-naturalistic-construction.edn:403-432`). Therefore:

- **READY is true as a meter result.**
- **O4 commissioning is incomplete as an organise witness.**
- Neither fact silently decides Joe's documented fork.
- If Joe requires organise/O4 before RUN4, the missing reviewed
  baseline/intervention/primary-score execution manifest must be completed
  first. If Joe authorizes RUN4 with organise open, the run must record that
  explicit disposition; READY alone is insufficient.

## Pre-launch checklist after implementation (still NOT EXECUTED)

1. Adopt a unique RUN4 ID/config and the hand-selected task-pin; record the
   complete shortlist, not only the winner.
2. Record Joe's organise fork disposition and re-emit/inspect the readiness
   snapshot without treating process exit zero as the verdict.
3. Verify serving-code identity, roster availability, author != effective
   reviewer, source/task digests, lock availability, and no preempting stop line.
4. Execute exactly one serving-JVM click through the reviewed task-pin seam.
5. Inspect worker/reviewer jobs, commit/build/grounding, route identity, and
   terminal outcome before acceptance.
6. Run the acceptance bridge and existing battery; commit exact generated
   artifacts explicitly, then perform the existing deposit pass. A worker
   success is not itself RUN4 certificate acceptance.
