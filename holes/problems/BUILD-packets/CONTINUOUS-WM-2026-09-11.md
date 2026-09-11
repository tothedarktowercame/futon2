# Feed the existing build backlog through WM

Preparation, 2026-09-11. Commissioned by Joe: review earlier empirics and make a dependency-ordered list of extensions and mistake-repair work. This is a proposed order over existing work, not an activated RUN4 manifest or a change to ledger status.

## What the earlier runs actually establish

`p4ng/empirics.tex` describes a working feedback mechanism: a failed actuation or independent review creates a durable repair obligation; selection gives open stop-lines precedence; a reviewed, grounded implementation moves the obligation to awaiting validation; a distinct production-shaped execution must validate the repair. Its July canary is evidence that this path operated, not evidence that every current repair is resolved. Later campaigns Q/R explicitly still required successors after grounded repair implementation.

The same paper distinguishes coding outcomes from the flown-fold stream that trained tactical structure belief. Coding a repair therefore must not be reported as updating the pattern library, or as demonstrating AIF learning. Its report of expensive tripwire serialization also makes performance a real engineering task, not a new speculative objective.

Current code retains the mechanism:

- `src/futon2/aif/repair_obligation.clj`: `record-system-failure!`, `open-obligations`, `record-implementation!`, `resolve!` preserve findings and require evidence for transitions.
- `src/futon2/aif/full_loop_runner.clj`: open non-environmental stop-lines precede ordinary selection; `validation-lines` takes **one** awaiting-validation/environmental obligation. Historical verification and ordinary implementation are distinct paths.
- `futon3c/src/futon3c/wm/run4_historical_successor.clj`: historical resolution requires strict historical and successor bundles, closed cohort authority, distinct executions and a grounded successful successor.
- `futon3c/src/futon3c/wm/run4_series_queue.clj`: runs already frozen entries; it does not turn arbitrary backlog prose into eligible missions. Unknown/refused evidence holds the queue.

Thus feedback exists, but neither automatic completion of the whole backlog nor one-run discharge of all pending repairs is established.

## Current execution boundary

Read directly from `/home/joe/run4/U88-codex20-20260911/queue/queue-state.edn`: exact click `wm-click-a6856c5f-1526-4dc8-8271-341aea4beb67`, cursor 0, no completed entries, held with `:series-step-refused` at `2026-09-11T19:17:17.383386798Z`. This is queue evidence, not a diagnosis of task failure. First inspect this click's binding, record, controller refusal and actual worker result. Do not retry it or infer mission success from launch. Preserve every historical admission and consumed cohort.

## Dependency order

Each row is a bounded deliverable. Dependencies mean evidence needed before executing that row; investigation and drafting can proceed earlier. Existing ledger rows keep their own acceptance criteria and status. A `blocked` ledger label is not itself proof that work is impossible.

| Order | Work and existing basis | Depends on | Completion evidence |
|---|---|---|---|
| 1 | Inspect the exact U88 run and isolate the reason for its held queue (RUN4, U88) | — | Join actual click/worker/record/cohort/controller evidence; name the first failing boundary, or verify a genuine terminal result. No manufactured close or second admission. |
| 2 | Repair that concrete boundary, if needed; validate pending repairs individually (U83 and canonical repair store) | 1 | A specific reproduction, changed consumer behavior, narrow independent review and distinct successor where required. Existing valid tests/receipts remain reusable; no default whole-suite rerun. |
| 3 | Freeze the next eligible existing build task through the existing mission/manifest path (RUN4, U83) | 2 | Preserve original task acceptance and dependency IDs; actual ranked/admissible selection must contain the pin. Reject unmet dependencies, changed sources and an earlier open stop-line before spending capacity. Keep execution sequential. |
| 4 | Account for node and edge fidelity on the resulting records (RUN4, RUN13) | 3 | For each claimed R node/edge, link Lean declaration, Clojure consumer and matching runtime witness; record missing/refuted correspondence. A route visit alone is not node semantics or whole-architecture conformance. Convergence only where accepted-run certificates actually exist. |
| 5 | Finish the current fundamental extension frontier: find (F11), organise (F12), contextual preferences (U88/F10) | 4 | Reconcile current row progress before editing. Find's F1–F4 and organise's O1–O4 need actual consumers and falsifiers; C must derive from endorsed contextual requirements, not invented weights. U88 output is input to this work, not presumed completion. These three branches can be prepared separately; no unsupported dependency among them is asserted here. |
| 6 | Implement reviewed pattern/cascade revision between episodes (existing LEARNING-pattern-cascade packet) | 5 | One diagnosed episode → exact reviewed library revision → a distinct episode that really consumes it. Reuse Cascade/CascadeDiff/organise structures and preserve authored relations. Retrieval/citation, execution and demonstrated usefulness remain separate. No permanent TA role is required. |
| 7 | Produce the next empirics slice and revisit evidence-dependent rows (RUN13, U80, U83, U84) | 4, 6 | Original task, predicted behavior, exact revisions, R-node/edge witnesses, repair transitions, result, cost and uncertainty. U84's 20 reasoned-record threshold is measured, not assumed; U80's dependency on F12 is preserved. Failed and unknown runs stay in the population. |

Parallel efficiency task: reduce repeated trip-report payload serialization (`tripwire.clj/write-trip-report!`) without changing witness evaluation. Measure bytes/time before and after on retained-shaped observations, retain the complete relevant witness and verify identical trip decisions. This can be developed after row 1 identifies current evidence; it is not a reason to discard or rerun previous qualification.

## How to feed the queue without creating another protocol

Use `holes/labs/wm-contract/worklist.edn` as the task authority, `BUILD-PLAN-0831.md` as design context and the canonical repair store as runtime failure authority. At this read the unfinished ledger IDs are RUN4, RUN13, F10, F12, F11, U80, U83, U84 and U88 (nine rows). Historical prose in BUILD-status must not reopen completed rows.

For the next task only: check predecessors against evidence, retain the original problem and acceptance, freeze a mission/manifest entry using existing machinery, then run it with the chosen worker and distinct reviewer. Codex-20 is Joe's requested worker inside the current run; Zai is preferred for focused review. Do not recast historical evidence. A runtime failure goes through the existing repair store; an extension stays an ordinary mission. A queue hold requires inspection of the same execution, not a reset or automatic retry.

For learning, follow `BUILD-packets/LEARNING-pattern-cascade.md` in its existing five slices. For formal claims use `mathlib4/DarkTower/WarMachine/holes-contract.json` and the current graph registry `p4ng/empirics-futon/control-map-edges.edn`; this preparation does not declare any additional Lean hole closed.

Validation of this preparation: unfinished IDs read from the EDN ledger; explicit dependencies are acyclic and ordered; referenced implementation and design files exist. No live state or worklist status is changed.
