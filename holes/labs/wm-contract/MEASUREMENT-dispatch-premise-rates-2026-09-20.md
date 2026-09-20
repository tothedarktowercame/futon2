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

## Second channel: Claude (corrected handoffs), same ledger, same window

Population: caller claude-12 -> agent claude-4, state done, n = 68.
Weak text rule (no installed convention exists between claude seats —
same instrument caveat as caveat 3): reply opening (500 chars)
contains a correction marker. Raw hits: 8/68. Hand-classified: ~6 are
genuine corrections of the SENDER's premise (stale claim, wrong
factor-health assertion, wrong sequencing fact, wrong record claim);
~2 are the receiver's own-error admissions miscaught by the rule. So:

| channel | rate | n | convention |
|---|---|---|---|
| codex refusal-at-intake | 0.14 (9/64) | 64 | installed (packet-instructed) |
| claude corrected-handoffs | ~0.09 (6/68) | 68 | none — ad hoc, lower bound |

Convergence worth noting at plausible grade: two independently
instrumented channels both land in a 0.09-0.15 self-credit error band.
A declared judgement-channel rate of ~0.1-0.15, labeled with these two
populations, is Landscape-grounded in exactly Joe's sense. Zai
(in-turn) channel still unmeasured; the coupling weight p (co-failure
clustering) is the remaining Landscape estimate.
