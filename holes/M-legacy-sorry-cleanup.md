# M-legacy-sorry-cleanup — validate & improve the 23 pre-existing substrate-2 sorries

**Status:** IDENTIFY (2026-07-02, spawned from M-fold-ansatz / the A-next gold-corpus work).
**Owner:** Joe + Claude. **Input snapshot:** `futon2/holes/labs/M-legacy-sorry-cleanup/legacy-sorries-snapshot.edn`
(the 23 `code/v05/sorry` hyperedges pulled from `:7071` on 2026-07-02, sha256 `d242066d…`; XTDB originals untouched).

## HEAD (one line)
substrate-2's `code/v05/sorry` type is 100% occupied by **23 logged-issue entries** (single self-endpoint +
prose `:sorry/title`) — the "useful but not this" sense. The A-next work introduced a **second, load-bearing
sense**: the **interface-sorry** (a contract/HtDP oriented hyperedge — `:have→:want`+`:hungry` over real
substrate-2 nodes; schema at `labs/A-next-sorry-hyperedge-schema.md`). **Review each legacy entry carefully
and decide: improve→interface · keep-as-logged · evict** — so the type carries one coherent meaning and no
paid-for signal is lost.

## 1. IDENTIFY — the gap
- The two senses collide under one type. Landing the 15 A-next interface-sorries alongside 23 loggeds would
  leave the type ambiguous unless the loggeds are validated + discriminated.
- Blanket-retyping the 23 (the first proposal) would be a rubber-stamp. Some loggeds may actually **name a real
  interface** and be *improvable* into interface-sorries — worth the careful look. Others are genuine issue-ledger
  notes (keep, retyped `:logged`); others are resolved/n-a-by-design and evictable.
- **This is a validation-and-improvement pass, not a mass migration.**

## 2. MAP — the 23 catalogued (from the snapshot)
- **`:sorry/kind`** — `:prototyping-forward` 8 · `:n-a-by-design` 6 · `nil` 6 · `:meta` 2 · `:external-dependency` 1.
- **`:sorry/status`** — `:addressed` 11 · `:n-a-by-design` 6 · `:open` 4 · `:acknowledged-v1-in-force` 1 · `:constructed` 1.
- **Shape (all 23):** single self-endpoint (`["<repo>-d/sorry/<name>"]`), prose `:sorry/title`, `:sorry/related-missions`,
  resolution/status props. **Zero** have/want orientation, **zero** typed endpoints over real ns-vars.
- First-pass hypotheses (to verify per-item, not assume): the 4 `:open` are the most likely to hide a real
  interface; `:n-a-by-design` (6) lean evict/keep-logged; `:addressed` (11) lean archive/keep-logged.

## 3. Review protocol (per item — the mission's work)
For each of the 23, read its title + related-missions + resolution + the underlying code/git, then classify:
- **IMPROVE → interface** — it names a real interface (have-inputs → want-features over substrate-2 nodes).
  Recover its endpoints via the **A-next method** (doc+code+git+XTDB), re-mint as an interface-sorry
  (`:sorry/facet :interface`, oriented hyperedge), and it *joins the gold corpus*.
- **KEEP → logged** — a genuine issue-ledger note (bug / prototyping-forward / deferred). Retype
  `:sorry/facet :logged` (or move to a distinct `code/v05/logged-hole` type — DERIVE decides which).
- **EVICT** — resolved / `:n-a-by-design` / superseded, adds no ongoing value. Erase via the **gated**
  destructive path only (`futon1a/scripts/erase.bb`, penholder `joe`, `--reason`; dry-run first). Never raw evict.

## Scope
- **In:** per-item classification + the improve/keep/evict disposition; recovering endpoints for any
  improvable-to-interface ones; the retype/evict executions (gated).
- **Out:** the 15 A-next interface-sorries (their own landing, `land_A_next_sorries.clj`); the substrate-2
  enrichment backlog (Gap-F work); defining the interface-sorry schema (done — `A-next-sorry-hyperedge-schema.md`).

## Exit criteria
1. Every one of the 23 has a recorded disposition (improve / keep-logged / evict) with a one-line rationale.
2. Any improved-to-interface ones are minted + added to the gold corpus.
3. The keep-loggeds are discriminated (`:sorry/facet :logged` or retyped) so `code/v05/sorry` (interface sense)
   is unambiguous.
4. Evictions (if any) done via the gated erase path with reasons recorded; snapshot retained as the audit trail.

## Relationships
- **M-fold-ansatz / E-fold-embed-pipeline** — the interface-sorries are the fold's phase-2 target; this mission
  cleans the type so the fold reads one coherent sense.
- **A-next gold corpus** (`labs/A-next-gold-corpus.md`) — improvable loggeds can extend it.
- **E-futon1a-archivist** — the write/erase gate this mission's executions ride.
