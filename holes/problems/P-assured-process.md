# P-assured-process — assurances for the WM's own process and handoff work

**Status:** DRAFT problem record, 2026-09-05 (claude-1, from Joe's direction).
The S1 fields are the commissioner's; they are drafted from Joe's words of
2026-09-05 and are **his to confirm or rewrite**. Nothing below is a build
packet.
**Ancestry (per N-process-trap-recording-conventions §3):**
commissioning ancestor: this record's §0 verbatim; inherited clauses: the
restated contract (EPIC-run-era.md, "THE CONTRACT, RESTATED") and the
conventions note's §0 governing rule; dropped clauses: none.
**Ordering:** downstream of the fundamentals gate (F1 nouns; this record's
build work does not preempt :F rows). Specification drafting may proceed
in parallel as discovery.

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
