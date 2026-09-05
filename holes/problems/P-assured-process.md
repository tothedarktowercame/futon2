# P-assured-process — assurances for the WM's own process and handoff work

**Status:** DRAFT problem record, 2026-09-05 (claude-1, from Joe's direction).
The S1 fields are the commissioner's; they are drafted from Joe's words of
2026-09-05 and are **his to confirm or rewrite**. Nothing below is a build
packet.
**Ancestry (per N-process-trap-recording-conventions §3):**
commissioning ancestor: this record's §0 verbatim; inherited clauses: the
restated contract (EPIC-run-era.md, "THE CONTRACT, RESTATED") and the
conventions note's §0 governing rule; dropped clauses: none.
**Ordering:** PARALLEL TRACK (Joe's ruling, 2026-09-05 — see EPIC-run-era.md,
"The parallel-practicalities ruling"): this work progresses in parallel with
the fundamentals, outside the wm-build-loop (whose :F gate is untouched), via
parked dispatches under claude-1; the tracks merge when both are ready. The
merge criterion: the R-node <-> process-assurance alignment census names, per
node, the assurance it must carry, and the fundamentals build reaches the
rung where wiring decisions consume that census. Superseded wording:
"downstream of the fundamentals gate."

## 0. The commissioning question, in Joe's words

> "there's the important but somewhat quotidian question of how not to fall
> into these traps any more with the wm build... and the more fundamental
> question of how to supply assurances around the running of the wm itself,
> once it is ready, assuming that it has to do any nontrivial process and
> handoff work, which I believe to be the case" (2026-09-05)

## 1. The problem record (S1 — Joe's fields)

```
problem:   The WM in operation is an orchestrator: it commissions work,
           receives returns, closes records, surfaces results. Its assurance
           stack (run-era ledger, catalogued checks, conformance certificates,
           stepper) covers the CONTROL LOOP's decisions; nothing yet assures
           its PROCESS conduct — and the eight-trap catalogue
           (N-process-trap-recording-conventions §1) was harvested from
           precisely this class of activity, performed by this campaign's own
           agents with far more context than the machine will have.
                                                        [Joe: confirm/rewrite]

now:       - Handoff machinery exists un-assured: bells/parks/packets are
             conventions in CLAUDE.md prose, not typed objects any checker
             refuses on.
           - statement-faithfulness of returned work: checked by hand in both
             campaigns, aspirational in both rung tables.
           - The tick's closure boundary (C509) EXCLUDES the seven
             store-owning namespaces of the full-loop click path — the
             process/handoff surface has no stepper, no pin, no replay.
           - The Lean spec precedent (APMCycleMachine) lost jurisdiction over
             failure paths by running only at close; the WM has no process
             spec at all yet.
           - T7/T8 (catastrophic single response; narrating watcher) have no
             WM instances today and no checker that would refuse one.

solved:    (a property of the MODEL, checked before running)
           A Lean process specification of the WM's handoff lifecycle
           (commissioned -> dispatched -> parked -> returned -> checked ->
           recorded -> surfaced, with EVERY failure path a typed transition),
           whose checker runs at every transition, in-loop; handoff contracts
           as typed objects with mechanical statement-faithfulness on
           returns; author-never-closes-own-handoff as architecture; the
           full-loop click path brought inside a pinned/steppable boundary;
           the conventions note's rungs enforced by refusing checkers over
           the machine's own process records; T5 reflexivity: every such
           gate satisfies its own property or records an exemption.
                                                        [Joe: confirm/rewrite]
```

## 2. What exists to build on (inventory, not plan)

- Boundary census: C509's seven excluded namespaces = the surface to bring
  under the stepper discipline (pin/step/reset/accept for coordination).
- The conventions schema (rungs, ancestry+notify fields, checker-ownership
  rule) — binding on new records; this record is its first process-side
  consumer.
- The run-era ledger's deposit/verdict/:incomplete semantics, ready to
  generalize from (run-id, check) to (dispatch-id, check).
- The charter's interfaces-are-Lean-declarations instruction (2026-08-30) —
  boundary contracts for delegated work, now with its justification: T1-T8
  show internals cannot be trusted even with full context.
- B1 (bulletin) as the surfacing discharge channel; F2's readiness-meter
  shape for "is this handoff worth dispatching" preregistration.

## 3. Gate

Operator confirmation of §1, per the delivery lifecycle — with the
conventions note's own caveat applied to this record: until a checker
refuses process work that lacks these assurances, this document is prose.

## 4. Three lifecycles, three subjects (recorded observation, 2026-09-05)

Joe, verbatim: "it is interesting that you have invented a new lifecycle
that is not the mission lifecycle, nor the 5 AIF stages from the Figure 7A
in futon-2026. Not that these all need to be aligned themselves -- they may
relate to different problem classes."

Provenance owned: the seven stages transcribe this campaign's operating
practice (packet -> agency_send -> park -> wake -> review-as-gate -> record
-> report), promoted to a named lifecycle by this record. Until a checker
refuses on it, the lifecycle sits at rung "named" on its own scale.

The three structures have distinct subjects and should not be forced into
alignment: the five AIF stages decompose ONE TICK and recur (subject: the
control loop); the mission lifecycle governs campaign-grain candidacy
(subject: a mission); the handoff lifecycle governs one delegation
(subject: a dispatch). Two real relations to keep: (1) GRAIN — a handoff
runs inside a stage traversal (the census found R16's actuator implementing
5/7 handoff stages inside ACT) and a mission discharges through many
handoffs; parallel to the four-grain cascade correspondence on the action
side. (2) SHAPE — the handoff lifecycle is the mission lifecycle
miniaturized, and the trap catalogue (T1, T6) bites at both grains, which
is why checked and surfaced are separate stages.

Open question for the Lean spec: whether `parked` is essential conduct or
an implementation detail of "awaiting return" (the other six are
obligations; parked is our transport's waiting discipline). The two stages
R16 lacks (parked, surfaced) are the two to scrutinize before building to
them.
