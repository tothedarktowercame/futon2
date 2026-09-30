# War Machine component owners

Date: 2026-09-30. Set up by claude-1 at Joe's suggestion: "a small team of
Codex collaborators, each of whom will own one reasonably small dimension of
the work ... an agent called 'controller' who will work on issues to do with
that component as they come up."

One machine, the real one (`futon2.aif.full-loop-runner/run-opportunity!`,
called in-process by `futon3c/src/futon3c/wm/runner_service.clj`). It is run
forward under the debugger. Each failure is a critical incident: claude-1
records it, assigns it to the owner of the component where it occurred, and
the owner returns the repair and the negative test for the class of failure.
An owner works only inside their component; an issue that crosses two
components goes to claude-1, who splits it.

| Role | Seat | Owns | Main files |
|---|---|---|---|
| **controller** | codex-6 | The order of phases and what each outcome leads to: typed refusals and failures, exactly one terminal receipt, no repeat of a refused attempt; the Lean model of that lifecycle and its check against the runner. | the control flow of `run-opportunity!`, `wm/terminal_receipt.clj`, `decision_gate.clj`, `mathlib4/DarkTower/WarMachine` (new lifecycle module) |
| **selection** | codex-4 | What is in the field and how one thing is chosen from it: open/closed, enumeration, admission, the cascade decision, abstention. | `enumeration_completeness.clj`, `mission_registry.clj`, `wm/cascade_decision.clj`, `efe.clj`, `observation_model.clj`, `focus_receipt.clj` |
| **seats** | wmfix-1 | Everything said to and heard from an agent: author, reviewer and interpretation requests, reply parsing into typed results, waiting on Agency jobs. | `flight_runner.clj`, `wm/click_ask.clj`, `want_interpretation.clj`, `interpretation_request.clj`, `task_execution_evidence.clj`, the author/reviewer prompt and reply code in `full_loop_runner.clj` |
| **records** | codex-proof2a | What a run leaves behind and what the next run reads: run record, phase log and timings, futon1b mission records, the interpretation and repair stores, the futon3c click binding. | `persist-run-record!`, `scripts/wm_click_timings.py`, `futon3c/.../watcher/multi.clj`, `mission_substrate_ingest.clj`, `repair_obligation.clj` |

There is no debugger role (Joe, 2026-09-30). codex-5 builds the debugger
once: phase failures raised as typed conditions with restarts (retry the
phase, use a value, abort), using an existing Clojure conditions/restarts
library, so that a failed real run waits at the failed phase with its state
intact and continues after repair. Then that work is finished. The known
failure types (refusals and the rest) are raised as errors and handled
through that mechanism; converting each is controller work.

claude-1: owner of PROOF-2b, the incident register, review of every
owner's change (author ≠ reviewer), reloads into the serving JVM, and
running the machine. The click cast (codex-proof2d author, codex-proof2e
reviewer, codex-proof2c/2b interpretation seats) is not part of this team
and is not given component work.

Seat ids are the existing registered Codex seats; the role name is how they
are addressed in packets and in the incident register. Zai and Kimi seats
remain available for discovery and overflow (zai-1 wrote the debugger discovery note,
2026-09-30).
