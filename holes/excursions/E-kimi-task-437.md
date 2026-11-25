# E-kimi-task-437 — G' wiring part A: discovery of the canonical G path (read-only)

**Requisition:** in-progress — dispatched 2026-11-25T05:52:39Z to kimi-1 as invoke-1795585959362-29042-4e5a1669

Clocked in by claude-14 for kimi-1 on 2026-11-25 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# G′ wiring, part A: discovery only (part B, the build, follows my review of this)

Assigned by Joe (2026-11-25): run the G′ model in the War Machine, aligned with Lean, no shadow implementation ("send it after due review"). Part A is READ-ONLY: no edits to any file except your report.

Reviewed and in place: futon2 `futon2.aif.want-free-energy` (c6e9a7fd0, readback ad2c42c77, registry fe7410bd7, report 7f96cbb42; review note d8235c2cc). It is a REFERENCE PORT: correct math, certified against the Lean witnesses, but outside the canonical G's contracts (authority, model revision, provenance, rational enclosures). G′ must run INSIDE the canonical code path, which both lanes already share: futon2 src/futon2/aif/cascade_coapply_g.clj `compare-at-horizon`, called by selection_canonical_g.clj `score` (line ~217) and by wm/family_canonical_g.clj (lines ~100, ~157). One implementation for selection and execution (Joe).

Answer, each with file:line:
1. Where compare-at-horizon computes the pragmatic term (risk) per step, and the ambiguity and novelty; which helper does the arithmetic (cascade-g/step-g? its own?), and whether it uses rational enclosures (cascade-g/log-bounds).
2. Where the preference C comes from on each call path (selection: shape/terminal-class-preference schedule; execution family: ?), its shape, and whether any C mass can be 0 on a predicted outcome (G′ needs c in (0,1]; G refuses :infinite-risk today).
3. Where Requirements' GNormalization is applied (perStepCapacityAndPattern: risk / (H·log|O|), ambiguity / H, information gain / pattern count) and which code path reads it. G′'s normalisation (Joe): want term / H.
4. Every consumer of the G value compare-at-horizon returns (selection pick, stopping comparison, tie detection :tie-at-minimum, abstention, trace fields, Requirements Q-checks in code) — so part B knows what changes when risk is replaced.
5. Which existing tests pin compare-at-horizon's G numerically (names), so part B knows which expectations will move and must be re-derived from Lean, not edited to pass.
6. A proposed minimal change for part B: G′ = want-term + ambiguity − novelty computed in compare-at-horizon with cascade-g's enclosure discipline, old risk still reported as a field (one code path, not a second scorer), selection and execution reading G′; and the readback extension (compare-at-horizon's G′ on a fixture against WantFreeEnergyWitness values).
Write holes/labs/wm-contract/C-gprime-wiring-discovery.md and commit only that file. Bell claude-14 back with a 15-line summary. Do NOT start part B.
