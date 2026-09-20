# MEASUREMENT — dispatch-premise refusal rates, counted denominators (2026-09-20)

Purpose: the A programme's point-5 calibration needs rates with both
ends counted (Joe's counted-denominator rule; his observation that
codex agents notice corrections and "the underlying data is
available"). This computes the codex-refusal-at-intake channel from the
Agency jobs ledger. It extends claude-4's hand-counted 9/46 with the
full ledger window.

## Population and rule (mechanical, reproducible)

- Source: GET /api/alpha/invoke/jobs?limit=1000 (ledger window
  2026-09-16T17:59Z to 2026-09-20T17:40Z), filtered to agent-id codex-*,
  state done, mode work. n = 112.
- Rule (claude-4's, mechanized): a job counts as a PREMISE-STOP when
  its result opens with Stopped/Stopping AND the opening 300 chars name
  a premise marker (premise / does not exist / contradicts / stale /
  refus- / cannot find / no such / is not the case).

## Table

| caller | dispatches | stopped-opening | premise-stops | premise rate |
|---|---|---|---|---|
| claude-4 | 64 | 17 | 9 | 0.14 |
| claude-12 | 40 | 0 | 0 | see caveat 3 |
| wm-full-loop | 6 | 0 | 0 | — |
| claude-1 | 2 | 0 | 0 | — |

The 9 premise-stops match claude-4's hand-classified set (hash-as-read,
issuing-caller-not-reaching-runner, J-token-inadmissible, refused-fact
wire conflict, candidate-assembly ambiguity, invalid warrant, ...).
Stops that are NOT premise-stops (8 of 17) are stale pins/ambiguity —
the world moving, not false claims.

## Caveats, in the same breath

1. LOWER BOUND: an unstopped dispatch is unrefused, not confirmed
   correct (the judge-reload error shipped unstopped and cost a click).
2. ONE CHANNEL: this measures Codex refusal-at-intake only. Claude
   (refused handoffs) and Zai (in-turn self-correction) channels need
   their own instruments (Joe's per-model channel structure).
3. CONVENTION CONFOUND — the claude-12 row is NOT evidence of better
   premises: refusal-at-intake is PACKET-INSTALLED. claude-4's packets
   instruct lanes to stop on false premises; claude-12's mostly did
   not, so those lanes could not emit countable refusals. A zero in an
   uninstrumented lane is a missing instrument, not a clean record.
   Design consequence for point 5: the calibration collection must
   install the refusal convention UNIFORMLY or denominators are not
   comparable across dispatchers.
4. Scope: one workspace, four days, mode=work only; failed/cancelled
   jobs excluded (10 failed were mostly today's executor wedge).

## What this supports

A DECLARED experimental rate for the codex-intake self-credit channel:
0.14 premise-stop (9/64, complete denominator), labeled with this
record as basis per the declared-as-such precedent. Adoption into any
live A declaration remains Joe's call; nothing here rates the J locator
by itself.
