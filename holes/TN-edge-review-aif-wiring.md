# TN-edge-review: the War Machine's wiring against the equations that force it

**Date:** 2026-09-01. **Author:** claude-1 (paper lane), at Joe's direction.
**Addressee:** wm-organization. **Status:** review with open queries; **no
rulings are made here**. Joe has said he is not prepared to rule on the
queries below without more information, so each query states the information
that would let him rule, and that gathering is the requested work.

**Artifacts.** Registry: `futon2/holes/labs/wm-contract/aif-equations.edn`
(futon2 `94493d7`). Generator and check: `p4ng/empirics-futon/gen_aif_dag.bb`
(p4ng `129001d`, `278226e`), emitting `aif-equation-dag.svg`,
`sec-aif-conformance-generated.tex`, `empirics-futon/aif-conformance.edn`.
Drawn map under review: `p4ng/empirics-futon/control-map-edges.edn`
(as-of 2026-08-30, 21 drawn edges) with `control-stages.edn`. Both figures
are in the futon-2026 paper's operator section (Figures 5A and 5B).

## 1. The standard this review applies

Joe (2026-09-01): the drawn control map "conflates all kinds of different
possible wirings", while "if we look at the formalism we'll see there's just
about one wiring that's actually conformant to active inference"; the R-boxes
"mostly correspond to terms and variables from the theory", and if that were
"written down clearly, for instance as a DAG, we could see which boxes need
which other variables to run"; where "we have a free hand to choose we should
write that down"; and "citations should be added to the formalisms we use",
which are not all from one paper.

The standard, stated so it can be applied again:

1. **An edge is justified by an equation or it is plumbing.** A *theory edge*
   from box A to box B exists iff a symbol defined by an equation at A (or an
   exogenous symbol supplied at A) is imported by an equation at B. Nothing
   else is a theory edge. A drawn edge with no equation across it is either
   *plumbing* (an endpoint runs no equation: scheduler, calibration,
   tripwires, budget, trace, self-certification) or *unexplained*.
2. **Every equation carries its citation.** The formalisms in use come from
   several sources (Laplace F from the continuous literature; G, softmax and
   learning from the discrete-state synthesis; BMR separately). Each row of
   the registry names its reference; the registry's `:references` map holds
   the citations. Citations entered 2026-09-01 from memory are marked for
   verification of equation and page numbers before publication.
3. **Where the framework leaves a free hand, the choice is written down** in
   `:choices`, in the same shape as `control-map-edges.edn :decisions`
   (`:value :by :at :statement`). Until Joe rules, an entry records what the
   War Machine is *observed* to do (`:status :observed-not-decided`). An
   observation is not a decision and must not be rendered as one.
4. **Derived means derived.** The `derived-undrawn` and `drawn-not-derivable`
   layers of Figure 5A were produced by two agents reading the definitions
   (`:by [:claude-15 :codex-22]`). Under this standard they are to be
   regenerated from the equations registry, so the amber layer of 5A and the
   amber edges of 5B agree by construction.
5. **Class 6a applies to the check itself.** The generator proves every
   import resolves, every node runs an equation or is listed as plumbing,
   every reference key exists, and every drawn-edge status is in vocabulary,
   before it counts anything. Negative controls run on every publish
   (`negative_controls.sh` 4c, 4d).

## 2. What the check found (as-of 2026-09-01)

17 theory edges at R-grain (16 equations, 7 exogenous symbols, world loop
entering at R16). 21 drawn edges.

| class | n | edges |
|---|---|---|
| theory edge, drawn | 7 | R1→R4, R3→R1, R4→R5, R5→R6, R7→R3, R7→R8, R16→R2 |
| theory edge, **not drawn** | 10 | R1→R3, R1→R8, R2→R7, R2→R8, R6→R4, R6→R16, R8→R3, R8→R17, R13→R4, R14→R6 |
| drawn, plumbing | 8 | R10→R8, R11→R16, R12→R7, R15→R13, and four more touching assurance boxes |
| drawn between equation boxes, **no equation across it** | 6 | R8→R5, R2→R3, R13→R14, R14→R16, R6→R13, R7→R14 |

Two observations that follow directly and need no ruling: the registry's
judgment "R10→R8: the scheduler is not an input to F" is what the F equation
says; and the one place `glossary-formal-lines.md` refuses to collapse two
readings — G is `risk + ambiguity` in the glossary and `Holes.G := risk − eig`
in Lean — is a case of two wirings of one node at the equation level.

## 3. Queries: what Joe needs to know before ruling

Each query names the information that would let it be ruled. The requested
work is to **gather that information and report it**, not to decide.

### 3a. The six unexplained drawn edges

For each: (i) where in the running code the edge is realised (the function
that reads the source's output at the target; `control-map-edges.edn
:route-measured-drawn` already names `:via` functions for 8 edges — the same
form); (ii) whether any recorded run traverses it (`tick-run-record-2026-08-30.edn`
and later); (iii) which of three readings fits — an implementation shortcut
that the theory routes through another box, plumbing misfiled among equation
boxes, or a departure from the formalism.

- **R8→R5** (present-fit mismatch → EFE core). No equation routes F or ε
  into G; F and G share no import. What does R5 read from R8, and is it
  used in the score?
- **R2→R3** (observation → belief update). The theory reaches R3 only through
  ε at R8 (`μ ← μ + αΠε`). Does R3 read o directly, or is this the R8 path
  drawn short?
- **R13→R14, R14→R16, R6→R13, R7→R14** (SELECT-internal, and Π into τ). No
  equation imports across them. Are these control-flow plumbing between
  equation boxes (then reclassify), or does τ actually depend on Π (then a
  precision-modulated temperature is a formalism variant to be cited and
  declared under `:choices`)?

### 3b. The ten theory edges not drawn

For each: is the import realised in code but undrawn (then 5A is incomplete),
or not realised (then the equation at the target runs without an input the
formalism says it needs — a genuine hole, to be recorded as one)? Priority
order by consequence: R2→R8 and R1→R8 (without o and μ, R8 cannot form ε);
R8→R3 (without ε, R3 cannot update); R14→R6 (τ into selection); R6→R16
(selection into actuation); R8→R17 (F into BMR); R13→R4; R6→R4; R2→R7; R1→R3.

### 3c. The eight free choices (`:choices`)

For each, the ruling needs: what the code does now (with a pointer), what
each formulation offers, and what would change if the other option were
taken.

1. `:free-energy-form` — Laplace channel-grain F beside a discrete policy
   side. Is a discrete categorical F available or intended? Does anything
   downstream (R17's ΔF, R8's disposition) depend on which form?
2. `:temperature-update` — τ set by calibration vs the β/γ precision update
   of the discrete synthesis. Does any code update τ per tick?
3. `:habit-prior` — E present in the Lean softmax. Is E ever non-uniform in
   runs? If always uniform, it is present in form only.
4. `:policy-depth` — T (R13). Fixed or varied? Where set?
5. `:hierarchy` — R15 composes loops. Is more than one level realised
   anywhere? If not, R15 is plumbing today.
6. `:learning` — BMR present (R17); Dirichlet learning of A appeared in Lean
   on 2026-09-01 and is not wired. Which learning is intended to run?
7. `:policy-posterior-node` — Q(π) assigned to R6 by this registry because
   no box is named for it. Is there a better home, or should a box be named?
8. `:selection-rule` — argmax vs sample of Q(π): **not recorded anywhere
   found.** What does the actuation code do?

### 3d. Citations

Verify the six references in `:references` against the sources (equation or
section numbers), and add any formulation actually used that is not covered.

## 4. Requested handoff shape

Discovery, not implementation (CLAUDE.md: split discovery from
implementation). Deliverable: one report answering 3a–3d with pointers
(file:line, run record, or "not found"), so that Joe can rule; rulings then
go into `aif-equations.edn :choices` and, for edges, into
`control-map-edges.edn :decisions`, after which the derived layer of 5A is
regenerated from the registry. Do not edit the registries in the course of
discovery; the paper lane will apply rulings once made.
