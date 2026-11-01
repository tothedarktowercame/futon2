# L5-14-L — A run with no interpretation has a typed interpretation order; Q5 holds vacuously (claude-2 → codex-5, 2026-11-01)

Lean owner: claude-2 (this packet is the specification; the Lean change is small and reviewed by me).
Base: mathlib4 `darktower` (≥ `221f060818`), futon2 `main` (≥ c123d097b). Worktrees only:
`git -C /home/joe/code/mathlib4 worktree add ~/worktrees/mathlib4-l5-14 -b l5-14-no-interpretation darktower`
(never `~/worktrees/mathlib4/<x>`: that path is the primary checkout). A fresh mathlib worktree has
no `.lake`; symlink `.lake -> /home/joe/code/mathlib4/.lake` and **delete the changed modules'
oleans before the review build** so proofs actually run.

## Why

Every selection-only click (refused after scoring, or abstained) reads Q5 unverifiable with
"no interpretation in this run" (L5-13 made that typed). `InterpretationOrder` has two cases,
`selectionBeforeInterpretation | interpretationBeforeSelection`; Q5 is
`r.interpretationOrder = .selectionBeforeInterpretation`. A run in which no interpretation
happened violates nothing Q5 forbids: Q5 forbids interpreting before selecting.

## Behaviour

1. `DarkTower/WarMachine/Requirements.lean`: add `| noInterpretation` to `InterpretationOrder`
   with a docstring: "the run ended before any interpretation (a selection refused after scoring,
   an abstention, a bypass); Q5 has nothing to forbid". `Q5 r := decide (r.interpretationOrder ≠
   .interpretationBeforeSelection)`, or equivalently a match with both other cases true. Add a
   theorem that the old definition and the new agree on the two old cases. Every other
   requirement unchanged. `lake build DarkTower.WarMachine.Requirements` (and the reader module)
   after deleting their oleans.
2. `scripts/wm_check_run_facts.clj`: render `interpretationOrder` `"noInterpretation"` as
   `.noInterpretation`; keep the existing two renderings. `scripts/wm_run_facts.clj`: where
   L5-13 writes the typed NR "no interpretation in this run" for a refused selection, write the
   value `"noInterpretation"` instead (the fact is known, not absent); the same for a selection
   record with `:selection-outcome :abstained` and for a not-exercised record's selection kind
   where every other carrier is NR (leave those NR if the packet owner's L5-5a reasoning says a
   bypass has no order at all; say which you chose and why).
3. Nothing else changes.

## Tests

- Lean: the agreement theorem compiles; a `#eval` on a RunFacts with `.noInterpretation` shows
  Q5 true and Q11/Q6 unaffected (`RequirementsReader` round-trips the new constructor).
- Clojure: click 12's and 13's records (fixtures from L5-13) export `interpretationOrder`
  `"noInterpretation"`, the checker renders it, and the Lean outcome for Q5 is `holds`; a work
  run's order is unchanged; `wm_check_run_facts_test`, `wm_run_facts_test` green.
- Named/projector namespaces vs main; hermetic; data count.

## Gates

kondo, parens, no reload, no merge (either repo), no `data/` writes. Branches `l5-14-no-interpretation`
(mathlib4) and `codex5/l5-14` (futon2). Bell claude-2 with both shas, the lake build log tail (a
real build, not a sub-second replay), and the two exports' Q5 lines.
