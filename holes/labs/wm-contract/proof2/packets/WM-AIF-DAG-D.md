# WM-AIF-DAG-D — the wiring map against the equation DAG (futon-2026 Figure 6A)

Discovery, read-only, claude-8, 2026-09-26 ~11:30Z. Joe, on the harness figure and the "each one typed and
fixed" correction: "This is why I wanted the map as a topological object. This is also why referring to
diagrams like Figure 6A in futon-2026 would be helpful. That figure shows how the components depend on
each other, i.e. we can make no claim to be implementing AIF without at least those dependencies."

Figure 6A (`p4ng/sec-operator.tex`, label `fig:aif-dag`, drawn by `p4ng/empirics-futon/gen_aif_dag.bb`)
is derived from the equation registry `futon2/holes/labs/wm-contract/aif-equations.edn` (as-of 2026-09-01,
21 equations): a theory edge Ra → Rb labelled s is a symbol s defined at node Ra and imported by an
equation at node Rb. There are 31 such edges. The paper already checks them against the drawn control map
(15 conformant, 3 conditional, 10 realised in code but undrawn, 2 not realised, 1 path-dependent).

This note checks them against the OTHER map, the wiring map `futon3c/holes/labs/M-wm-wiring/wm-flight-wiring.edn`
at ad3afb76 (75 boxes, 53 fields), by a generated join, `spike/wm_vs_equation_dag.bb` (futon3c 95f0f744), output
`wm-vs-equation-dag.edn`. Nothing here is asserted by hand; the two caveats below are the join's own limits.

## The join and its two limits

A box belongs to node R if its site file is one the registry's `:code` string for R names (file grain), or its
site var is a function that string names (var grain). Then a theory edge is :declared if a map field runs from a
box of Ra to a box of Rb, :boxed-no-field if both ends have boxes and no field joins them, :unboxed otherwise.

- **The registry names no code site for R1 (belief state, μ), R2 (structured observation, o) or R5 (expected free
  energy, G).** Their `:code` is empty. So by this join those three nodes have no box, and every edge touching
  them reads :unboxed whatever the map says. That is a registry gap, for its owner; the join cannot fill it.
- **File grain is coarse.** `war_machine.clj` hosts R3, R3a, R7, R8 and R13 at once, and `policy.clj` hosts R6,
  R8, R14 and R16, so a field between a war_machine box and a policy box counts for every pair they host. Var
  grain is precise and finds one node with a box: R14 (`select-action-cascades`).

## Result: the 31 edges of Figure 6A

| grain | declared | boxed, no field | unboxed |
|---|---|---|---|
| by file | 5 | 9 | 17 |
| by var | 0 | 0 | 31 |

The five "declared" at file grain are the generic keys `:target-class` and `:per-policy-argmax` between
`cascade-decision-admitted`, `class-observation-model` and `select-action-cascades`, counted for
R3a→R3, R3a→R7, R6→R8, R7→R3 and R8→R3. None of them is the symbol the edge carries (ε, π, Π, F_π). At var
grain the wiring map declares **no edge of the equation DAG**.

That is not a defect in the map so much as a statement of what it is: the wiring map boxes the FLIGHT (rows 0-11,
the loop that chooses a target, reads it, asks, constructs, selects, enacts, verifies, learns, publishes) and
pins its sites; the inference core that Figure 6A's nodes live in (belief, precision, forward model, EFE,
policy posterior) is drawn by the control map and checked there. The two maps share four boxes
(`rank-cascade-actions`, `order-use`, `cascade-lane`, `select-action-cascades` and the decision boxes in
war_machine) and no field.

## The edges that ARE the flight's to realise: the exogenous symbols and the world loop

Figure 6A imports five symbols the flight produces. By the mission's row table: C is row 2 (the read step),
A is row 6 (rates), E is row 7 (habit); u (R16's action) is row 0 (enactment) and o (R2's observation) is
row 10 (the observed publication). What leaves each row, and which nodes' boxes receive it:

| symbol | produced by | imported at (Figure 6A) | fields leaving the row | reaches a box of |
|---|---|---|---|---|
| **E** | row 7 `enactment-habit/fold` | R6 | `:enactment-records` → `select-action-cascades` (and → the outer cascade) | **R6** (policy.clj), by file and by var (R14) |
| **A** | row 6 `sourced-rates` | R4 host; R1 R2 R3 R3a R4 R5 R7 R8 | `:measurement` → `cascade-lane` | war_machine (R3 R3a R7 R8 R13 by file); not R4/R5 |
| **C** | row 2 `served-by-reading/proposal` | R5 | `:text-sha256`, `:want-span` → the read step's own verifier and read-fn | **no core box**: C never leaves row 2 as a declared field |
| **o** | row 10 `observe-publication-fn` | R1 R3 R3a R7 R8 R17 | `:publication-observed` → `enact-fn`, the outer cascade | the flight only; **no R2 or belief box** |
| **u** | row 0 `enact-fn` | R1 R2 | `:attempts`, `:grain-gate` → W_c checker, habit increment | the flight only; **no R1 or R2 box** |

So, of the five flight-to-core dependencies Figure 6A forces:

- **E → R6 is declared and precise**: the habit prior reaches policy selection as `:enactment-records`.
- **A → the lane is declared and coarse**: the measured rates reach the war_machine lane; which of its nodes
  consumes them the map cannot say.
- **C → R5 is not declared**: the read step's C stays inside row 2. In the code it travels as `:wants` through
  `flight/judge-opts` into the tick (WM-HANDOFFS-D H2, H7); the map has no box for that carrier.
- **o and u close the loop at the flight's grain, not the core's**: the enactment's attempts reach the W_c checker
  and the habit, and the observed publication reaches the next enactment and the outer cascade. Neither reaches
  a box of R1, R2 or R3: the map declares no path by which the world's answer to an action becomes the belief's
  observation. The paper's world edge R16 → R2 is drawn in the control map; in the wiring map it is
  `:publication-observed`, terminating at the flight.

## What this says, as a topological statement

Figure 6A's dependencies are necessary for a claim to implement AIF. The wiring map declares none of the core's
internal edges (they are the control map's), declares one of the five flight-to-core edges precisely (E), one
coarsely (A), and three not at all (C, o, u to the belief). The six red boundaries on the harness figure and
these three undeclared edges are the same fact seen twice: the hand-offs from reading through the click, and
from the world back into belief, run through carriers no box holds.

## For the record, not decided here

1. The registry's owner: `:code` for R1, R2 and R5, so the join can place them.
2. The map's owner: whether the core's boxes (belief, precision, forward model, EFE) belong on the wiring map at
   all, or whether the two maps are joined by a declared interface (the fields C, A, E, o, u) with the control map
   owning the inside. The join favours the second: it makes the five flight-to-core edges the wiring map's
   conformance target, checkable by the prover, and leaves Figure 6A's 31 to the map that already checks them.
3. WM-HANDOFFS-D's seven hops are the map edits that would make C, o and u declared.
