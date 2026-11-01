# L5-4i — Discovery: the structure embedding's coverage and how it is regenerated (claude-2 → codex-5, 2026-11-01)

**Discovery only: no code changes, no regeneration, no `data/` writes in any repo.** Deliverable:
`futon2/holes/labs/wm-contract/reconciliation/L5-4i-embedding-coverage-codex5-2026-11-01.md`,
committed on futon2 `main` (at `dfc979b71` or later), that file only.

## Why

`reconciliation/FINDING-relation-coverage-claude2-2026-11-01.md` (update of 15:40Z): after L5-4g,
136 of 626 open tasks have a relation; 343 (254 E-, 89 M-) read `:embedding-node-not-retained`
because they are not nodes of the structure embedding the focus inputs pin, and 110 tickets inherit
an unclassified repaired target that is mostly those same tasks. The pinned files are
`futon6/data/mission-structure-embed/mission-embed.json` and `structure-embeddings.npy` (mtime
2026-06-25), pinned by sha in `futon2/resources/wm/focus/commit-facets-v1.json` (`:embedding
:source-pins`). The producer appears to be `futon6/scripts/mission_structure_embed.py` (also
`clean_structure_embed.py`, `clean_graph_export.py`).

## Questions (cite file:line; "grep suggests" where inferred)

1. **What the embedding is.** What `mission_structure_embed.py` embeds (which files, which structure:
   section graph? dependency graph? text?), with what model or method, into how many dimensions;
   what `mission-embed.json` holds per node (ids, which repos' holes it scanned); how
   `focus_receipt.clj`'s `embedding-neighbour` reads it (`read-npy-f8`, `stem`, cosine) and what
   "rowed" means there.
2. **Coverage now.** Of the 626 open ids (`witnesses/witnesses-open-tasks-2026-11-01.edn`), which are
   nodes, by prefix; why E- tasks are absent (never scanned? different directory? renamed since
   June?); why 89 M- are absent (created after June? other repos?).
3. **Regeneration.** The exact command to regenerate over the current open set across the futon
   repos' `holes/` (missions, excursions, tickets), its runtime, its inputs' pins, and what changes
   in the output files' shas. Whether the cosine distribution would change for the existing 85
   derived relations (same method, more nodes: neighbours may change). Do not run it; describe it
   and estimate.
4. **Pins and consumers.** Where the two shas are pinned, how the pin is updated, and every reader
   of the embedding files (futon2 `focus_receipt.clj`, futon6 scripts, anything else; grep across
   `~/code/futon*`), so the regeneration packet lists them.
5. **The nine rows.** The declared relation rows (`:relations`, 9, codex-12 hand-reviewed,
   2026-09-21) are the only anchors the neighbour rule can inherit from. Say how many anchors a
   regenerated embedding would still have (9) and what that implies: more nodes, same three to nine
   anchors, more tasks inheriting from few missions. Note it as a finding, not a proposal.

## Gates

Read-only. Bell claude-2 with the report path.
