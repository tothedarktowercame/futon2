# L5-23 — Relation coverage: three producer gaps (claude-2 → codex-15, 2026-11-01)

Basis: `reconciliation/L5-23-D-relation-coverage-codex15-2026-11-01.md` (merged). Lean: no change.
futon2 `main`; worktree `~/worktrees/futon2/codex15-l5-23`, branch `codex15/l5-23`; never check out a
branch in `/home/joe/code/futon2`. Mode: work. Three behaviours, one bell; a behaviour whose producer lives in another repo is delivered by one commit per repo (Behaviour 1: the futon6 producer `scripts/mission_structure_embed.py` plus the futon2 re-pin, two commits for one behaviour; clarified 23:35Z after codex-15 stopped on the count). The 0.2
embedding floor, graph bound 3, link kinds and basis stay exactly as declared (Joe's parameters).

Census on the landed record: 160 of 588 known (6 focus, 154 associated); unknown 428, of which
`:embedding-node-not-retained` 232, `:repair-target-absent` 100, `:embedding-below-floor` 44,
`:no-parent-relation` 33, `:repaired-target-unknown` 18. Stated-relation hops 2→4 buy nothing.

## Behaviour 1 — the pinned structure embedding covers every admitted target

232 admitted targets have no retained node in the pinned structure embedding, so the embedding route
cannot even ask. Extend the embedding producer so every admitted target (missions, excursions,
tickets) has a node, re-pin it (sha on the record as now), and keep the floor at 0.2. Report the
census before/after on the same record replay; the discovery predicts about +43 known.

## Behaviour 2 — a repair ticket inherits its repaired target's relation

118 rows (`:repair-target-absent` 100, `:repaired-target-unknown` 18) are `T-repair-occ-*` tickets.
Their relation to the focus is their repaired target's relation. Resolve the target against the
**whole retained population** (completed and cancelled items included, with their pins), not only
the admitted field; derive the ticket's relation from the target's (via `:derived-via {:kind
:repaired-target …}`), typed `:repaired-target-unrelatable` only when the target itself has none.

## Behaviour 3 — the file lookup reads `holes/excursions/`

The producer's text lookup tries `<repo>/holes/missions/<id>.md` then `<repo>/holes/<id>.md`; 372
targets had no text found, excursions among them. Add `<repo>/holes/excursions/<id>.md` (and
`holes/tickets/` if tickets carry Relations sections) in the same pinned way. Report how many of
the 372 now resolve.

## Tests (live pins from the landed record's `:per-task-rating` rows)

1. A target in the 232 set gets an embedding node and a typed outcome (`:associated` or
   `:embedding-below-floor`), never `:embedding-node-not-retained`.
2. A `T-repair-occ-*` whose target is completed and has a known relation inherits it with the
   derivation recorded; one whose target has none → `:repaired-target-unrelatable`.
3. An excursion with a Relations section is read; the relation edge count on the record rises
   accordingly.
4. Known relations before the change are byte-equal after it (no re-derivation of existing rows).
5. Producer wall time per click reported (was 1.0 s warm / 2.1 s cold).

Gates: clj-kondo, check-parens, `clojure -X:test :nses` for touched namespaces. Commit each
behaviour; bell claude-2 with the three shas, receipts, and the census table after.
