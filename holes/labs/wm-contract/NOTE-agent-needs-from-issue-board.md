# NOTE: agent needs, read off the Cascade Live issue board

Joe with zai-7 (zai), 2026-09-12. Task: derive the crew's needs not from
demo taste but from the historical register — the issue board behind
`pipeline-pattern-cascade-live` (served from
`/var/www/zone.hyperreal.enterprises/wip/issue-board.edn`, refreshed
2026-09-10 with projection-hash checks). Counts: 324 issues / 133 subjects;
301 done; live backlog 23 — 14 needs-verification, 12 disagreements
(unowned), 4 blocked-on-prerequisite, 3 ready-for-lane, 1 run-gated,
1 in-flight; 20 unowned ids; 124 no-carrying-packet.

The board's own discipline frames everything (its compliant-run artifact):
"this artifact performs no acceptance and writes no ruling. READY means the
evidence an acceptance needs is intact and current; it is not permission."
Every agent below inherits that clause — agents prepare and surface, never
accept or rule.

## The hierarchy, base to top (each level presupposes the one below)

**N0 — coherence keepers.** The record must be trustworthy before anything
reads it. Agents: inbox-zero board (landed, `chip_board.clj` +
`inbox_zero_board_live.clj`); **packet-writers** for the 124 no-carrying-
packet items (curate evidence into the carrying packet the board's own
schema defines — mostly mechanical, exactly L0 work). Without N0, every
higher read inherits stale-authority and unowned-id blindness.

**N1 — verifiers.** The 14 needs-verification items plus every
`:dated-not-revalidated` freshness state: re-run the evidence check the
item names, re-derive the projection, report typed green/stale/conflict.
This is the ledger-keeper board generalized from "report claims" to "the
board's freshness column is a standing query". Pure read; ZAP forbidden.

**N2 — brokers and triage.** The 12 disagreements (all unowned,
`:source-disagreement`, producer-vocabulary/consumer-requires conflicts
like "consumer requires :ready; producer cannot emit it") and the 20
unowned ids. Agent: a disagreement-broker board that holds both sides
without asserting either (the register already records "no side
asserted"), names the exact seam, and routes to N4 — plus an ownership
*proposer* that classifies and suggests, never assigns (assignment is a
ruling). SMELL/LOOK/report/ask chips only.

**N3 — executors.** The 3 ready-for-lane items and the 4 blocked ones once
unblocked: the act-arm crew (zaif profiles; the coding-handoff lanes
already exist as bells). Gated on N1 verification passing and N2 surfacing
no live conflict on the same subject.

**N4 — the War Machine itself.** The run-gated item, the ruling-needed
column, `wm-choice/learning` ("decision-owner joe; complete the proposed
evidence/update/next-consumer design for review"). The only level with
decision rights, and its characteristic act is assembling the certificate
for Joe at ruling time — the clerk work (candidate runs, selection
discrimination, censuses) is N1/N2 output composed into an agenda. The
WM is one agent-need among the register's own rows, not above the
hierarchy's law: it ratifies, it does not pilot.

## What the reading buys

1. **Need is measured, not vibes**: the crew's shape is the backlog's
   column histogram. Right now the binding constraint is N1/N2 (26 of 23+
   live items are verify-or-broker work; only 3 are executor-ready) — the
   system is verification-starved, not execution-starved.
2. **Every need level is a board over the same chip set**: N0 sweeper
   (landed), N1 ledger-keeper, N2 broker (smell/look/ask), N3 zaif lanes,
   N4 the WM's ratification compose. No new verbs needed for N0-N2.
3. **The no-carrying-packet mass (124) is mostly historical done-work**
   needing curation — the right first N0/N1 mission, and it trains the
   packet-writer board on forgiving material before it touches live rows.
4. **The hierarchy's law generalizes inbox-zero's**: each level may lower
   its own precisions; only the level above (Joe at top) raises them. The
   board's "accepts-nothing" clause is N4's statement of it.

Row links: SPEC-chip-boards-v0 (§3½ levels, §4 task list — this note
re-grounds it in the register); NOTE-inbox-zero-aif (N0's theorization);
issue-board.edn (the substrate); M-zaif-harness-v1 (the crew edition).
