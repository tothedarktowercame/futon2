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

---

## 5. Discovery results (wm-organization, C448, futon2 `96917d8`) and review

Report: `holes/labs/wm-contract/C448-TN-edge-review-discovery.md`. Code basis
inspected: futon2 `d9111a9`, p4ng `278226e`. Run records:
`tick-run-record-2026-08-30.edn`, `tick-run-record-2026-08-31.edn`
(run `00f4bf58`); both have the same nine-hop route.

**Review (claude-1, 2026-09-01).** Every row carries a `file:line`, a run
record, or an explicit "not found". Both registries are unchanged
(`aif-equations.edn` since `94493d7`; `control-map-edges.edn` and
`control-stages.edn` since `278226e`); neither lab directory was dirty.
Spot-checked six pointers against the code, all matching: F computed and
route-tagged at `war_machine.clj:4450-4452`, onward `wm-state` at
`:4458-4466` carries no F; τ from selection gain / score spread at
`policy.clj:242-243`; deterministic `(first controller-entries)` at `:249`;
`max-key` branch at `:416`; act-gate loop at `enact.clj:298`; BMR scoring
at `a4a.clj:126`.

### 5a. Six drawn edges no equation explains

| edge | code | recorded? | finding |
|---|---|---|---|
| R8→R5 | R5 never receives F: `wm-state` omits it; `efe/compute-efe` predicts from state and candidate action (`efe.clj:601-619`) | yes, both runs, as sequential control flow | drawn edge carries no data; a measured route hop is not an equation dependency |
| R2→R3 | R3 reads o only through ε and Π (`war_machine.clj:4368-4388`) | no (R2→R7→R3 recorded) | the theory path R2→R8→R3 drawn short |
| R13→R14 | not found; horizon and τ computed independently (`:4485-4487`; `policy.clj:242-245`) | no | no dependency in code |
| R14→R16 | not found; WM route ends R14→TRACE; `close-loop!` does not read τ (`enact.clj:287-316`) | no | no dependency in code |
| R6→R13 | not found; π and T supplied independently | no | no dependency in code |
| R7→R14 | not found; τ from score spread and gain, not Π (`policy.clj:242-245`, `:72-80`) | no (R6→R14 recorded) | no dependency in code; whether Π→τ was *intended* is the only open question |

### 5b. Ten theory edges not drawn

| edge | finding |
|---|---|
| R2→R8, R1→R8, R14→R6, R6→R4, R1→R3 | realised in code, undrawn (`war_machine.clj:4368-4379`, `:4289-4294`; `policy.clj:242-245`; `efe.clj:601-609`, `:903-921`) |
| R2→R7 | realised and recorded, undrawn |
| R8→R3 | realised at the equation level (`:4375-4388`, `:4429-4431`); route tags say R7→R3 then R3→R8 — the recorded route is not the dependency DAG |
| R13→R4 | realised when horizon ≥ 2 (`efe.clj:601-609`; `forward_model.clj:279-324`) |
| R6→R16 | **not found** as Q(π)→u: the run stops at TRACE with a recommendation (`war_machine.clj:4573-4584`, `:4804-4817`); `close-loop!` enacts the first passing act gate from ranked actions (`enact.clj:287-316`), not the recorded selection |
| R8→R17 | **not realised**: live F only stored (`:4753`); R17 is offline replay (`r17_offline.clj:1-6`, `:64-96`) computing BMR from Dirichlet models (`a4a.clj:126-159`) |

### 5c. Eight free choices, as observed

1. F form: Gaussian channel F is a diagnostic with **no live consumer**
   (`free_energy.clj:184-205`; `war_machine.clj:4450-4452`); policy side is
   discrete-style G (`efe.clj:601-619`, `:782-794`). No reference found that
   cites this mixed-grain composition as one formalism.
2. τ update: default `:selection-gain-only` (`war_machine.clj:238-248`),
   which folds realised outcomes (`selection_gain.clj:173-206`) — so the
   registry's "fixed-calibrated" is **wrong**; score-spread mode selectable
   via `FUTON_WM_TAU_MODE`; no β/γ update found.
3. Habit E: learned prior implemented and persisted (`habit_prior.clj`), but
   on the live path `:habit-prior-applied? false` (`policy.clj:234-271`) —
   present in form only. Run records carry no habit state.
4. Depth T: absent unless an anticipation snapshot loads, then fixed 3
   (`war_machine.clj:4485-4487`); rollout default 2 (`rollout.clj:166-171`);
   never varies per policy.
5. Hierarchy: "flat temporal rollout, not nested fast/slow hierarchy"
   (`rollout.clj:166-176`); budget code exists but no multi-level generative
   hierarchy wired — R15 is plumbing today.
6. Learning: offline BMR only; `DirichletConcentrations` is a Lean carrier
   (`Holes.lean:6380-6385`) with no live counterpart.
7. Q(π) node: `policy/select-action` is R6 (`policy.clj:300-438`; route tag
   `war_machine.clj:4584`); no other home exists.
8. Selection rule: **deterministic** — first admissible controller entry
   (`policy.clj:247-250`), or first G-ranked (`:387-410`), or `max-key` of
   ln E − G/τ (`:411-438`). Softmax weights computed and recorded, never
   sampled. R16 independently chooses the first passing act gate.

### 5d. Citations

Bibliographic identity verified for Buckley 2017 (JMP 81:55–79), Da Costa
2020 (JMP 99:102447), Friston 2017 (Neural Computation 29(1):1–49), Friston–
Parr–Zeidman 2018 (arXiv:1805.07092), Kass–Raftery 1995 (JASA 90:773–795);
Parr–Pezzulo–Friston 2022 not available to the discoverer. Equation/page
numbers unverified for all but Buckley (eq. 9 general F; eq. 45 Laplace
energy; eqs. 58–60 precision model; eq. 59 recognition dynamics). Two
corrections to the registry's own claims: the F, ε, Π and update rows are
**implementation reductions** of Buckley's equations, not verbatim ones; and
Kass–Raftery Table 2 gives **2 ln BF = 6–10 as "strong"**, i.e. ln BF ≥ 3 in
the favoured direction — it supports the magnitude of the −3 threshold but
not its sign, which must be stated as the convention `a4a.clj` actually uses.

### 5e. Two findings beyond the queries

- **The route record is not the dependency DAG.** R8→R5 is traversed in
  every run with no data crossing it; R8→R3 carries data while the route
  tags say R7→R3, R3→R8. `wmRunConformsToWiring` (route ⊆ wiring) and "the
  wiring is the one the equations force" are different properties; Figures
  5A and 5B measure different things, and the captions should say so.
- **Actuation is disconnected from selection.** R16 enacts the first passing
  act gate, not the recorded Q(π) choice. The loop as drawn closes; the loop
  as run does not, at that hop.

## 6. How each item is to be addressed

Joe (2026-09-01): "all of these items need addressing rigorously, very few
if any need rulings." Reclassified accordingly. **V** = verify against a
source or the code and record the result; **C** = correct the registry (an
error of mine); **D** = correct the drawing so 5A matches code and
equations; **H** = record a hole (an input the formalism requires that the
code does not supply); **J** = genuinely needs Joe.

| item | class | action |
|---|---|---|
| R8→R5 drawn | D | reclassify as sequence (control flow), not data; note "no data crosses" |
| R2→R3 drawn | D | redraw as R2→R8→R3 per code |
| R13→R14, R14→R16, R6→R13 drawn | D | retire: no dependency in code, none in theory |
| R7→R14 drawn | D + J | retire as drawn; **J:** was Π→τ intended as a variant? if yes, cite and declare under `:choices`; if no, nothing further |
| R2→R8, R1→R8, R14→R6, R6→R4, R1→R3, R2→R7 undrawn | D | draw them; they are realised |
| R8→R3 undrawn; route tags disagree | D + V | draw it; record that route tags are not a dependency record (5e) |
| R13→R4 undrawn | D | draw, annotated "when horizon ≥ 2" |
| R6→R16 not realised as Q(π)→u | H | record as a hole against the action equation; the enacted choice is not the selected one |
| R8→R17 not realised | H | record as a hole against the model-reduction equation; live F has no consumer |
| choice 1 F form | V + C | registry row F: mark "diagnostic; no live consumer"; the mixed grain stands as an observation until a reference or a decision covers it (**J**, but only after V) |
| choice 2 τ update | C | replace "fixed-calibrated" with "selection-gain fold (default) or score spread; no β/γ update" with pointers |
| choice 3 habit E | C | record "implemented, not applied on the live path" |
| choice 4 depth T | C | record the observed values and where set |
| choice 5 hierarchy | C | record R15 as plumbing; move it in the registry |
| choice 6 learning | C | record offline BMR; Dirichlet as Lean carrier only |
| choice 7 Q(π) node | V | confirmed R6; record the pointer; no ruling needed |
| choice 8 selection rule | C + J | record "deterministic (argmax / first admissible)"; **J:** is sampling intended? the formalism allows either |
| ΔF sign convention | C | state the convention `a4a.clj:126-159` implements; cite Kass–Raftery for magnitude only |
| Buckley rows | C | annotate F, ε, Π, update as implementation reductions of eqs. 9/45/58–60 |
| equation numbers, Da Costa / Friston 2017 / Parr 2022 / BMR | V | retrieve the papers (§7) and record equation numbers per row |
| mixed-grain composition cited nowhere | V | search the retrieved sources; if absent, it is declared as a stack choice, not a theory one |

Items marked **J**: three at most (Π→τ intent; sampling intent; whether the
mixed grain is accepted as a stack choice), and each only after the V step.

## 7. Papers to retrieve

| ref | open source |
|---|---|
| Buckley et al. 2017 | arXiv:1705.09156 |
| Da Costa et al. 2020 | arXiv:2001.07203 |
| Friston et al. 2017 | UCL Discovery eprint 1530701 |
| Parr, Pezzulo & Friston 2022 | MIT Press open access (direct.mit.edu, oa-monograph) |
| Friston, Parr & Zeidman 2018 | arXiv:1805.07092 |
| Kass & Raftery 1995 | stat.cmu.edu/~kass/papers/bayesfactors.pdf |
