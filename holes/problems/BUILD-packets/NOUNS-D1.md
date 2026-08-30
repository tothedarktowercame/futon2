# NOUNS-D1 — visibility census: cascade / typed hole / wiring diagram in the PLoP-2026 machine

Owner: claude-15. Builder: codex-2. Mode: work. Discovery only; no code, tex, or Lean edits. Consumer: Joe's
morning read + the §0.16 nouns seed (futon4/holes/delivery-lifecycle.md — read it first for why these three nouns).

## Joe's question, verbatim in spirit
The most recent model: a CASCADE progressively develops a TYPED HOLE and a WIRING DIAGRAM that goes inside it —
replacing the old "try harder, pick a pattern, get a warrant." These units were developed in great detail. What he
does not have: visibility into the extent to which they are actually used in the current PLoP-2026 war machine —
paper and running system — especially since cascades are now far better specified than in the earlier paper.

## Goal: one visibility table per noun, four sites each
For each of {cascade, typed hole, wiring diagram}:
1. **The paper** (`p4ng/plop-2026.tex` + its `sec-*.tex` inputs): where the term appears, which section, and
   whether the usage carries the NEW specification (cascades as scored/ordered structures; holes as typed with
   discharge conditions; wiring as boxes+holes+wires) or an older/looser sense. If git history shows the earlier
   PLoP version, one line on the delta.
2. **The tick path** (`scripts/futon2/run_tick_once.clj`'s 9-hop route, plus `cascade_lane.clj`, `close_loop.clj`,
   `enact.clj`): does the tick construct or consume any of the three? Note `enact.clj`'s fold engine returns
   `:boxes`/`:policy-holes` — the typed-hole wiring machinery lives THERE; say whether the tick reaches it and
   under what gate.
3. **The fold/diffsub world** (futon6 + the psi artefacts with boxes b1..bN, holes h1..hN, wires — see
   `futon2/holes/overnight-flights-2026-07-06.md` for the shape): the fullest existing implementation — last-write
   dates and current consumers. Reuse `futon2/holes/labs/wm-contract/NOTE-cascade-consumers-census.md`; extend,
   do not duplicate.
4. **The Lean side** (`mathlib4/DarkTower/WarMachine/` Cascade/CascadeOrder; Holes.lean itself is a typed-hole
   registry; `p4ng/empirics-futon/control-map-edges.edn` + `hyper-edge-schema.edn` are wiring-as-data): which of
   the three nouns has a formal carrier, and does anything in sites 1–3 cite it.
Per cell: used / mentioned-only / absent, with file:line for every "used" claim. ≤40 rows total.

## The verdict Joe wants
One paragraph: to what extent does the PLoP-2026 machine actually RUN on cascade→typed-hole→wiring-diagram, versus
describing it? The strongest live chain (file:line to file:line), and the biggest gap between the paper's claim
and the code's practice. Honest "absent" cells are the point, not a failure.

## Acceptance (row-11 first)
- Three tables + verdict in `futon2/holes/labs/wm-contract/NOUNS-D1-visibility.md`; every "used" cell
  spot-checkable; the earlier census cited where reused. Refuse cells you cannot classify honestly (say what you
  looked at). No edits outside the one findings file; commit only your path.

## Report
Bell claude-15 back with: sha, the verdict paragraph verbatim, per-noun used/mentioned/absent counts, the
strongest live chain, the biggest gap, refusals.
