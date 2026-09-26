# E-kimi-task-60 — Sidecar P6a: mark table + Q9 node join (futon1b)

**Requisition:** in-progress — dispatched 2026-09-26T14:11:42Z to kimi-8 as invoke-1790431902497-24868-9acea0ea

Clocked in by claude-12 for kimi-8 on 2026-09-26 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# Packet P6a — `mark` table + Q9 join (graph node → S1 mark + S4 scopes over the same span) (futon1b)

Design `futon1b/holes/DESIGN-hyperedge-scope-sidecar-2026-09-26.md` §1 (Q9), §3, §7 item 6.
Follows P5 (`futon1b_scopeindex.clj`, ad7468b; reviewed by claude-12: independent kind count
857/35 confirmed, a real `/`-truncating parser fails the per-kind test). Reviewer: claude-12.

## Data

The run is now durable at **`/home/joe/runs/mark7master-20260921/`** (copied from `/tmp/r7v`;
use this path). Relevant dirs: `artifacts/marks/` (e.g. `fable-0708.1921-dp-emacs.json`),
`artifacts/graphs/` (e.g. `0705.0102__p0.edn`, `…__p0.rung2.edn`), `artifacts/expo/`
(P5's source), `artifacts/paper-graphs/`. Read samples of each before designing the table;
decide which graph files are canonical (e.g. whether `.rung2` supersedes) and say why.

Q9 today: "join proof-graph node → S1 mark + S4 scopes over same span; manual
cross-referencing of artifacts/graphs + artifacts/marks + artifacts/expo (no index)".

## Build (in `futon1b_scopeindex.clj`)

1. A `mark` table (run, paper, the mark's id/kind, line span, file coordinate, provenance
   verbatim) with an index serving the span join; a table or columns mapping graph nodes to
   their line spans if the graph files carry them (read them first — if nodes carry no span,
   say how the span is obtained and do only what the data supports).
2. `index-run!` indexes marks (and node spans) with the same per-paper one-transaction
   replace and the same watermark as scopes, so re-indexing one paper replaces its marks,
   node spans and scopes together.
3. `node-join` `[run paper node-id]` → `{:node … :span [l0 l1] :marks [...] :scopes [...]}`
   using closed-interval overlap as P5's Q7.

## Tests (`test_scope_index.clj`)

- touching one paper's files (any of expo/marks/graphs) changes exactly that paper's rows in
  all tables; others untouched;
- `node-join` on a fixture returns exactly the expected marks/scopes, boundaries included;
- **bad case**: make the join half-open (or drop marks on re-index); a test fails. Say which.

## Acceptance (throwaway sidecar, separate process; live JVM untouched)

Index `/home/joe/runs/mark7master-20260921`, report per-table row counts and build time, and
for 3 nodes of one paper compare `node-join` with a manual cross-check of the three
artifact dirs (say how you did it, programmatically preferred).

## Constraints

- futon1b master; explicit-path commits; no stash, amend, branch switch,
  `git checkout -- <file>`; never revert others' changes. Do not edit `futon1b_server.clj`
  (kimi-1 is wiring the scope index into the server in parallel, packet P6b).
- Gates: clj-kondo (no new warnings), `futon4/dev/check-parens.el`, `test-scope-index`,
  `test-hx-index`, `test-hx-reads`, `test-hx-busy`, `test-fts-periodic`.

## Reply

Bell claude-12 back with sha, the graph-file decision, table design, test lines, bad case,
acceptance numbers and the cross-check method.
