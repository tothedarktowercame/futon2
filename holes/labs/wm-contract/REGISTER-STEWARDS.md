# Register stewards — an owner-agent per growing backlog (design, 2026-09-07)

Joe, 2026-09-07: "if some of the register is growing in terms of a
backlog, then we need a strategy for dealing with that … maybe we need to
start to install an agent for each of the red register items. So that
rather than having tasks grow, we need to start to grow. Those registers
could be owned by an agent that would work through them as new items are
coming into the queue on those separate backlogs."

## The contract every steward signs

A steward is a standing agent seat that OWNS one register. Its contract:

1. **Work the queue down as items arrive.** One small slice per
   invocation (the wm-build-loop discipline); receipts written per batch;
   the register's count is the steward's scoreboard and it is expected to
   trend to zero and stay there.
2. **Never present Joe a raw queue.** Operator attention is spent on
   class-level rulings and bounded exception lists only (the
   morning-brief reduction, MORNING-BRIEF-reduction-2026-09-07.md, is the
   template: 73 items → 4 rulings). Escalations are written as
   `:blocker`-shaped decision text so the surfacing machinery renders a
   decision sheet, not a count.
3. **Honest numbers only.** A steward regenerates through the register's
   own machinery (generators, probes, linters), never hand-edits a
   rendered artifact, and reports deltas rather than smoothing them.
4. **Author ≠ reviewer.** Steward output is reviewed by the claude owner
   between batches (the mining-loop role-spec pattern,
   library-loop/MINING-historical-cascades.md); substantial fixes are
   belled with small packets, per the handoff protocol.
5. **Rate, not just level.** Each steward reports its register's growth
   rate alongside its size. A register that grows faster than its steward
   drains is the signal to grow capacity (Joe: "we need to start to
   grow") — add a seat, don't let the queue absorb the difference.

## The steward map (register → owner → first batch)

| register | backlog today | steward | first batch |
|---|---|---|---|
| morning-brief decisions | 73 items (→ 4 rulings + exceptions) | **MB steward — install first** | apply Joe's class rulings; audit class A |
| strict-lint bindings (+ the workspace-gate checks downstream of them) | 37 stale, growing ~5/day — the measured grower | **rebind steward** | rerun-and-rebind sweep; then hold at 0 each publish |
| referent-drift (paper citations) | 12 drifted + 1 unvetted, regrows with every loop edit | **vetting steward** | re-vet the 12; standing re-vet on new drift |
| test pins (futon2 receipt, futon3 aif_sampling post-AC7, library-lint pins) | 3 stale clusters | **pin steward** | live-pin refresh per the live-pin rule; AC7 refusal cases added, not deleted |
| library graph debt | 683 unresolved why-targets, 5 why-cycles (futon3) | **library steward** (library-loop lab, feeds the mining loop) | cycle break-up + unresolved-target triage |

Not steward-shaped, deliberately:
- **gauges (M-eoi 0/6 no-producer)** — needs producers built or criteria
  revised: a decision then a build row, not a queue.
- **sorry-category, workspace mid-work dirt** — accepted-red register
  entries awaiting Joe's nod (RED-COMPONENTS items 6–7).
- **repair-gate finding, Box 2/Box 7** — one-shots, already rows
  (U66–U68) or trivially rowable.

## Install mechanics

- Seats come from the Agency: the zai pool (quota resets 2026-09-08
  02:04Z) for grindable sweeps (rebind, pin refresh, library triage),
  idle codex seats for audit-shaped work (MB class-A audit, drift
  vetting). Check the roster before every bell; verify running before
  parking; park on every dispatch.
- Loop scripts fork from the newest existing loop template and diff
  against the others (build-loop fork discipline); edits to a running
  loop go via rename.
- Each steward gets a role-spec file next to its register (target per
  invocation, gates, review cadence, stop conditions), in the style of
  the mining loop's spec.
- Board-write discipline binds stewards too: shared registries are edited
  only in the post-publish idle gap.

## Sequencing

1. MB steward first — its reduction sheet is already in front of Joe;
   on his rulings it has a fully-specified first batch, and it retires
   the largest operator-facing number on the board.
2. Rebind steward second — the one register measurably growing, and its
   sweep clears two Figure 3 components at once (strict-lint +
   workspace-gate's check family).
3. Pin, vetting, library stewards as seats free up, in that order —
   pin and vetting are small; library is the long grind.
