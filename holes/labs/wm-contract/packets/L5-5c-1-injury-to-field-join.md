# L5-5c-1 — An opened injury publishes a repair ticket that the field can see (claude-2 → codex-11, 2026-11-01)

Base: futon2 `main` at `af7dd0b3f`. Authority: claude-3's L5-5 report
(`reconciliation/L5-5-bypass-paths-claude3-2026-11-01.md`, §Self-heal, step 1): no injury can reach
the selection field today because Q1 admits only open tasks and the injury-to-finding join is
empty (0 of 299 open findings carry `:cause-record-pin`, 1 carries `:origin-run/id`, none of the 9
pinned injuries matches a finding). Joe's ruling 4: selection is over tasks; a self-heal is the
machine selecting repair work, which must therefore be a task in the field before the repair
cascade (README row 3) can be a compared policy. This is step 1 only; the obligation fact, the
`repair-goes-to-the-front` step and arbitration are L5-5c-2/3.

## Behaviour

1. Read how an injury opens (`meta_injury_store`, the medic path the report cites) and how repair
   tickets are minted today (`finding_ticket`, `repair_obligation`, 283 of 299 open findings carry a
   `:discharge-contract`). When an injury opens, publish exactly one repair ticket for it, or attach
   to the existing one, carrying the join key: `:cause-record-pin` (the injured run record's path
   and sha) and `:origin-run/id`, plus the injury id. The ticket is an open task in the registry, so
   it enters the field by Q1; its relation to the focus derives through the ticket-parent route
   (L5-4g) or the repaired target.
2. The finding written for the injury carries the same two keys, so the join is two-sided.
3. Idempotent: reopening or re-observing the same injury does not mint a second ticket.
4. Nothing in selection changes. Record in the report how many of the 9 pinned injuries would have
   published a ticket under this rule, and what their tickets' relations would be under the live
   producer (read-only replay; do not mint tickets for historical injuries).

## Tests

- An injury opened on a fixture run mints a ticket with both keys; the finding carries both; a
  second observation of the same injury mints nothing.
- The field producer (L5-4b) admits the minted ticket as an open task on a fixture registry.
- Named, outer, projector namespaces vs `main`; hermetic root; data count.

## Gates

kondo, parens, no reload, no merge, no `data/` writes. Branch `codex11/l5-5c-1-injury-ticket-join`.
Bell claude-2 with sha, the 9-injury replay table, counts vs main.
