# RUN4 hand-selected execution-path reconciliation — 2026-09-10

Status: **NOT EXECUTED**. This is a read-only path audit at futon2
`f2819e92abc3378e96560f2413d80c66567a17c0`. No tick, click, lock, readiness
regeneration, registry/worklist edit, data write, or sealed-holdout read was
performed. The commands below are proposals, not evidence of a run.

## 2026-09-10 scope and carrier update (supersedes three-trial wording below)

RUN4 now contains **four** ordered trials.  `SERIES.edn:7-31` preserves the
original three and adds `:outer-loop-aif-replacement`; Joe prioritizes that
fourth, progressive-development workload.  Its bounded packet is
`TRIAL-PACKETS.md:210-329`.  References below to a three-trial series describe
the earlier audit state and must not be used as the current series cardinality.

### Carrier decision: blocked, no honest existing action

The existing carrier cannot represent any of these packet-defined tasks
without inventing semantics:

- The forward model admits only its enumerated action types and requires a
  target for ordinary actions (`src/futon2/aif/forward_model.clj:26-45`).
- Ordinary mission actions are executable only when their target resolves to
  a live file-backed mission (`src/futon2/aif/mission_registry.clj:282-320`).
- The runner takes its entry from the ordinary judgment and derives target,
  mission and construction from that same entry
  (`src/futon2/aif/full_loop_runner.clj:2584-2609,2678-2692`).
- The default constructor is a cascade over an already meaningful action;
  special semantics exist only through explicit action-type methods
  (`src/futon2/aif/full_loop_runner.clj:1006-1089`).  Its own contract says a
  meta-action must not masquerade as an ordinary mission by renaming fields.
- Default mission construction resolves the action target through the mission
  registry (`src/futon2/aif/full_loop_runner.clj:1091-1107`), and the eventual
  author dispatch consumes that derived mission and construction
  (`src/futon2/aif/full_loop_runner.clj:2803-2824`).

In particular, Trial 4 is one fixture-scoped decision → execution → outcome →
next-use experiment with three still-explicit semantic prerequisites
(`TRIAL-PACKETS.md:271-320`).  It is not “advance” of any existing mission.
Likewise the math, caption and feedback packets have no explicit current
mission/action bindings.  Similar mission titles do not establish mappings.
Therefore no guarded entry injection is implemented in this slice: injecting
one of these tasks as `:advance-mission` would bypass the carrier contract, and
injecting a novel type would fail the current forward-model/admissibility
boundary.

### Smallest explicit extension contract (PROPOSED, NOT IMPLEMENTED)

The task-pin mapping must become a tagged union.  Existing behavior remains:

```clojure
{:carrier :mission-action
 :mission-id "M-existing"
 :action {:type :advance-mission :target "M-existing"}}
```

The new arm is an explicit action and immutable task definition, not a mission
alias:

```clojure
{:carrier :pinned-task-action
 :action {:type :execute-pinned-task
          :target :outer-loop-aif-replacement
          :series-id "run4-2026-09-10"
          :trial-id :outer-loop-aif-replacement
          :pin-sha256 "<exact-byte task-pin digest>"}
 :task-definition
 {:schema :wm/pinned-task-definition-v1
  :id :outer-loop-aif-replacement
  :kind :bounded-development
  :packet {:path "holes/labs/wm-contract/runs/RUN4-preparation-2026-09-10/TRIAL-PACKETS.md"
           :sha256 "<frozen whole-file or extracted-packet digest>"}
  :source-pins [{:path "<isolated ledger fixture>" :sha256 "<digest>"}
                {:path "<outcome fixture>" :sha256 "<digest>"}]
  :target-repository "/home/joe/code/futon2"
  :scope {:objective :one-decision-execution-outcome-next-use-cycle
          :live-ledger? false :scheduler? false}
  :semantic-contracts
  {:selection-equation "<frozen definition>"
   :outcome-mapping "<frozen definition>"
   :learned-state-consumption "<frozen definition>"}
  :outcomes #{:succeeded :failed :blocked}}}
```

That arm is not addressable until all of the following land together and are
independently reviewed:

1. `:execute-pinned-task` is registered in the forward model with an explicit
   prediction arm and task-definition-backed `can-propose?`/`can-execute?`.
   Unknown, stale or incomplete definitions refuse before selection.
2. A proposer emits the exact digest-bound action into the ordinary candidate
   field.  Operator selection may choose it without matching unconstrained
   rank, but the exact action must occur in the recorded admissible set.  The
   unconstrained ranking remains a labelled counterfactual; there is no silent
   selector override.
3. `construct-selected-action :execute-pinned-task` and
   `mission-for-decision` consume the verified task definition directly.  They
   do not look up or fabricate a mission.  The constructor yields the same
   author prompt/build/review/grounding route used after ordinary construction.
4. Series ID, trial ID, exact-byte pin digest, task-definition digest and
   operator-selection provenance propagate through selection, construction,
   dispatch, review, build, grounding, delivery QA and the append-only result.
5. The option is absent by default.  With no validated pinned-task envelope,
   candidate generation, selected entry, checkpoints, prompts and run records
   remain byte/value identical to the existing path.  Validation alone remains
   non-executable and grants no launch permission.

The concrete Trial 4 example above is presently **blocked**, not executable:
its selection equation, outcome mapping and learned-state consumption contract
are still intentionally unset, and its isolated fixture paths/digests have not
been frozen (`TRIAL-PACKETS.md:309-320,341-344`).  This is the exact next
implementation boundary; an HTTP route, dispatch activation and RUN4 accept
bridge remain later reviewed slices.

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

## Joe's selected three-trial series

Joe has now selected all three prepared tasks, so the input is no longer a
shortlist from which the runner chooses one. The authoritative preparation
record is `SERIES.edn:1-40`; the bounded task contracts are
`TRIAL-PACKETS.md:28-220`:

1. memory-assisted m96J04 endpoint uniqueness, deliberately imperfect WM fit;
2. substantive review/admission of the three fixed captions;
3. an isolated feedback-obligation monitor under frozen experimental rules.

Success is not a series invariant. Each trial must terminate independently as
`:succeeded`, `:failed`, `:blocked`, or `:not-attempted`; infrastructure and
wiring outcomes remain separate fields. In particular, failure on the math
trial is evidence about this task/runner encounter, not grounds to erase it or
claim that mathematical proving was the WM's original remit. Trial 2 may have
mixed admit/reject verdicts and still succeed under its packet. Trial 3 must
block if the experimental rules/corpus have not been frozen.

Three ordinary `/wm/click` requests—or three direct Agency bells—would not
meet this instruction. The existing click record has no series identity,
ordinal, frozen task-packet digest, predecessor link, or series stopping rule.
Nothing prevents the ordinary selector from choosing a different mission on
each click. Such jobs would be three unrelated opportunities later grouped by
prose, not one preregistered RUN4 inner-loop series.

## Smallest missing seam

Add one reviewed, fail-closed **RUN4 series adapter** around the loaded
full-loop runner, plus one acceptance bridge; do not add a second JVM or a
manual Agency dispatch loop. This is implementation work required before
launch.

The series-start request should accept only a path or digest-bound manifest
with this minimum shape (the actual three task packets and their source hashes
replace the ellipses):

```clojure
{:schema :wm/run4-series-pin-v1
 :run4-id "RUN4-..."
 :selection {:mode :operator-selected :selector "Joe"
             :selected-all? true
             :source-ref "PREPARATION.md:120-135"}
 :trials [{:ordinal 1 :trial-id :memory-assisted-mathematics
           :packet-ref "TRIAL-PACKETS.md:28-88" :packet-sha256 "..."}
          {:ordinal 2 :trial-id :caption-review-and-admission
           :packet-ref "TRIAL-PACKETS.md:92-146" :packet-sha256 "..."}
          {:ordinal 3 :trial-id :feedback-obligation-prototype
           :packet-ref "TRIAL-PACKETS.md:150-204" :packet-sha256 "..."}]
 :order :ordinal
 :stop-rule :attempt-each-once-even-after-fail-or-block
 :casting {:author "zai-5" :reviewer "codex-17"
           :repair-reviewer "codex-1"}
 :source-pins {:futon2 "<sha>" :readiness "<sha256>"}
 :sha256 "<digest-of-canonical-record-without-this-field>"}
```

Required behavior is deliberately narrow:

- validate all three packet/source hashes before attempt 1 and refuse the
  series before dispatch on missing, ambiguous, stale, or unordered inputs;
- create a typed `:execute-pinned-trial` action and construction for each
  packet. The current runner cannot do this through public opts: its entry comes
  from `selected-entry` and its target/construction/mission are derived from
  that entry (`full_loop_runner.clj:2584-2605,2678-2692`). The adapter must
  inject the pinned entry *inside* that state machine, not dispatch around it;
- preserve the ordinary ranked WM selection as a counterfactual checkpoint,
  but label the enacted selection `:operator-selected-series`; never claim the
  strategic selector chose these tasks;
- retain the normal construction, real Agency author dispatch, independent
  review, build, grounding, and delivery-QA gates—task pinning must not bypass
  them;
- run the three ordinals sequentially under one series controller. A terminal
  task failure/block advances to the next ordinal; an infrastructure state that
  makes further execution unsafe stops the controller and records remaining
  trials `:not-attempted` rather than silently retrying or skipping;
- execute under the existing run lock and redirect/freeze every mutable input
  needed by the RUN4 pin contract;
- emit one append-only series record and one attempt record per ordinal,
  joining series-pin digest, series ID, ordinal/trial ID, predecessor attempt,
  click ID, attempt ID, full-loop run ID, selected action, author/reviewer jobs,
  commit or store admission receipt, trace, task outcome, infrastructure
  outcome, wiring outcome, and recording-completeness outcome;
- preserve every initial attempt and retry as separate records; a retry needs a
  new attempt ID and `:retry-of`, while the series ordinal remains fixed;
- teach `wm_step.sh accept` (or a new subcommand sharing its acceptance code)
  to validate the series/attempt bundle and feed those exact trace/run
  identities into the existing battery before advancing
  `:pin/accepted-steps`. Acceptance must report all three task outcomes and
  cannot reduce them to a single green series bit.

Concretely, the required code surfaces are:

- futon3c HTTP: a new `POST /api/alpha/wm/series` handler accepting
  `series-pin` only; ordinary `/wm/click` remains selector-owned;
- futon3c serving service: single-flight series lifecycle/status and sequential
  calls into the already loaded `full-loop-runner`;
- futon2 full-loop runner: validated pinned-entry injection, typed
  `:execute-pinned-trial` construction, selection-source provenance, and
  series/ordinal fields on checkpoints and run records;
- futon2 RUN4 tooling: `wm_step.sh accept-series` (shared acceptance/battery
  implementation, not copy/paste) and an identity/schema checker.

Proposed operator surface **after that seam exists and is independently
reviewed**:

```bash
# NOT EXECUTED — PROPOSED; unsupported at current HEAD
curl --fail-with-body -X POST http://127.0.0.1:7070/api/alpha/wm/series \
  -H 'Content-Type: application/json' \
  --data '{"series-pin":"/home/joe/code/futon2/data/wm-step/RUN4/pin/series.edn"}'

# NOT EXECUTED — PROPOSED; unsupported at current HEAD
bash holes/labs/wm-contract/wm_step.sh accept-series \
  /home/joe/code/futon2/data/wm-step/RUN4 \
  /home/joe/code/futon2/data/wm-step/RUN4/series-result.edn RUN4-ID
```

Until all four surfaces exist and are independently reviewed, the correct
disposition is **NO LAUNCH PATH**, not “run three clicks, then import their
outputs.” Importing after the fact would prove neither that the pinned packets
governed worker execution nor that one series controller preserved order,
failures, and stopping behavior.

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

1. Adopt a unique RUN4 ID/config and the three-trial series pin; record all
   three selected packets, their order, hashes, and per-trial stop conditions.
2. Record Joe's organise fork disposition and re-emit/inspect the readiness
   snapshot without treating process exit zero as the verdict.
3. Verify serving-code identity, roster availability, author != effective
   reviewer, source/task digests, lock availability, and no preempting stop line.
4. Start exactly one serving-JVM series controller through the reviewed seam;
   it owns the three sequential inner-loop opportunities.
5. Inspect each trial's worker/reviewer jobs, product-specific receipts,
   build/grounding where applicable, route identity, and terminal outcomes
   before series acceptance.
6. Run the acceptance bridge and existing battery; commit exact generated
   artifacts explicitly, then perform the existing deposit pass. A worker
   success is not itself RUN4 certificate acceptance.
