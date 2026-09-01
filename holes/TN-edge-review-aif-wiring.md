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

## 8. Sources retrieved and what they settle (2026-09-01)

Files, URLs and checksums: `holes/labs/wm-contract/refs/README.md` (PDFs
not committed). Six of seven retrieved; equation numbers read from the PDFs.

| ref | retrieved | what was verified |
|---|---|---|
| Buckley et al. 2017 | arXiv 1705.09156 | eq. 9 general F; **eq. 45** Laplace-encoded energy Σ ½ε²/σ + ½ ln σ; eq. 58 worked instance; **eq. 59** recognition dynamics (gradient descent). The registry's F, ε and update rows are *reductions* of eqs. 45/59: ln σ terms dropped, mean over channels, one fixed-step gradient step. Π from sample variance is not in Buckley (his σ are model parameters) → Π is stack-defined in content. |
| Da Costa et al. 2020 | arXiv 2001.07203 | **eq. 42** G = ambiguity + risk − *novelty*; **eq. 44** risk over outcomes (the WM's form); eqs. 45–48 ambiguity; **eq. 10** Q(π)=σ(−G); **eq. 11** u = argmax; eq. 21 Dirichlet accumulation; eq. 38 β-family; A.2 γ as inverse temperature; A.4 deep temporal models. |
| Friston et al. 2017 | author's page (NECO OA) | **eq. 2.1** P(π)=σ(−γ·G); **eq. 2.7** the update set incl. π=σ(−F−γ·G), β=β+(π−π₀)·G, γ=1/β. **The habit prior E is not in this paper** — the registry's citation for E was wrong. |
| Friston et al. 2016 (added) | PMC 5167251 | "Active inference and learning": E as policy concentration parameters (habit learning). Equation not recoverable from text extraction; identity confirmed from the prose; number unverified. |
| Friston, Parr & Zeidman 2018 | arXiv 1805.07092 | **eq. 9** reduced free energy; Table 1 Dirichlet closed form; text: a reduced model is accepted when its log-evidence gain is > 0 — the paper's ΔF is reduced − full. |
| Kass & Raftery 1995 | author's page; scanned | **Table 2, p. 777** (by eye): 2 ln B₁₀ ∈ 6–10 "strong", > 10 "very strong" → ln BF ≥ 3 is the lower edge of "strong". Magnitude only. |
| Parr, Pezzulo & Friston 2022 | **not retrieved** | MIT Press and OAPEN refused automated fetch. Cited for notation only; no equation depends on it. |

### What the sources settle (no ruling needed)

- **ΔF sign (§6, C).** The WM's ΔF = ln B(A) + ln B(a′) − ln B(a) − ln B(A′)
  (`Holes.lean:6394-6399`, `bmr.clj:108-130`) is the *negative* of Friston
  2018's reduced-minus-full log evidence — checked numerically (identical
  rows: WM −5.01, paper +5.01; different rows: WM +11.38). So ΔF_WM =
  ln P(y|full) − ln P(y|reduced), and "accept when ΔF ≤ −3" means the reduced
  model wins by ≥ 3 nats = Kass–Raftery "strong". A convention, now stated in
  the registry; not an error.
- **Selection rule (§6 choice 8, was C+J).** Da Costa eq. 11 *is* argmax.
  The code's deterministic selection conforms; "sample" was the registry
  author's wording, not a formalism option in any retrieved source. **No
  ruling needed** — `:status :resolved-by-source`.
- **Habit prior citation (C).** Moved from Friston 2017 to Friston et al.
  2016.
- **Novelty term (new, C).** Da Costa eq. 42 has a third term, −E[D_KL[Q(A|o,s)‖Q(A)]]
  (parameter exploration). The WM's G omits it; since no Dirichlet Q(A) is live,
  the term is identically absent rather than approximated. Recorded on the G row.
- **Registry rows corrected (C):** F marked diagnostic with no live consumer;
  τ update rewritten (selection-gain fold default; score-spread mode; β/γ
  not implemented — earlier "fixed-calibrated" was wrong); E "implemented,
  not applied live"; depth values and where set; hierarchy single-level
  (R15 plumbing); learning offline-BMR-only. Q(π) node verified R6. Two
  **holes** recorded in the registry (`:holes`): R6→R16 (enacted action is
  not the selected one) and R8→R17 (live F feeds nothing).

### Still open after the sources

- **J1** Π→τ intent (drawn R7→R14): no code, no theory; retire unless it was
  a deliberate variant.
- **J2** Mixed-grain F: a stack composition unless a source is found; F has
  no live consumer, so the practical question is whether F should feed
  anything (R17 is the candidate the theory names).
- **Drawing corrections (D)** for Figure 5A remain to be applied from §6 once
  wm-organization confirms the retire list; they follow from the code
  findings, not from rulings.

## 9. Reconciliation with C451–C453 (claude-20 bell, 2026-09-01)

Three discovery reports landed in parallel with §5–8: `C451-unexplained-drawn-edges.md`
(`8154aa1`, query 3a), `C452-undrawn-theory-edges.md` (`872ec1a`, 3b),
`C453-citation-verification.md` (`0eb25dc`, 3d; wm-evidence). C454 (3c) is
dispatched. Where they and C448/§8 differ, the reconciliation:

| point | C448 / §8 | C451–C453 | resolution |
|---|---|---|---|
| Parr 2022 | not retrieved | **retrieved and verified**: B.7 `P(π)=Cat(π₀)`, B.8, **B.9 `π = σ(ln E − F − G)`**, p. 247 | Fetched the same mirror; B.8/B.9 confirmed by reading the PDF. **Parr B.9 is now the primary citation of the Q(π) row** (it is the one source that writes the E form); Da Costa eq. 10 / A.2 and Friston 2016 eq. 6 kept as supporting. The registry form drops F_π (the WM scores by G alone) — labelled a stack specialisation. |
| Buckley eq. numbers | 9, 45, 58, 59 | 77–79, 84, 48–50, 82–86 | Same arXiv file, different equations (single-variable §2–3 vs multivariate §4). Both recorded. C453's stricter point stands: **none of the channel forms is written in Buckley** — labelled `:local-specialisations`. |
| Da Costa eq. 10 | cited for Q(π) with A.2 for γ | eq. 10 omits E and τ; fn. 5 has the fuller form | Noted on the reference; primary moved to Parr B.9. |
| BMR row imports | `[:F]` | BMR takes full posterior + full/reduced **Dirichlet priors**, not scalar F | **My error.** Row now imports `:a-conc`; new row `:dirichlet-accumulation` (Da Costa eq. 21) defines it at R17 from o, μ — realised offline from the TRACE corpus (`a4a.clj:85-113`). Consequence: **R8→R17 is not a theory edge**; the theory edges into R17 are R2→R17 and R1→R17 by way of TRACE. |
| R8→R17 "hole" | not realised (C448) | "the one genuine hole" (C452) | Reframed: F having no consumer is true and recorded, but it is an observation about F (a diagnostic), **not a hole against BMR**, which never imported F. |
| R6→R16 | not found: `close-loop!` enacts the first passing gate (C448) | realised: production runner resolves the selected entry (`full_loop_runner.clj:870-873`) (C452) | Both correct for their path. Recorded as `:path-dependent`; the open question is whether the two paths enact the same action. |
| selection rule | resolved: Da Costa eq. 11 argmax | eq. 11 argmax vs Friston 2017 eq. 2.3 (min expected prediction error) | Two deterministic rules; **neither is sampling**. Row cites both; `:resolved-by-source` stands for the sampling question. |
| ΔF ≤ −3 | convention stated and numerically verified | "neither source states the rule" | Agreed: it is the WM's convention (§8), now stated in the registry with the numerical check; sources supply the quantity and the magnitude only. |

Net effect on the DAG: 17 → 18 theory edges (R8→R17 out; R2→R17, R1→R17 in),
both new ones realised offline via TRACE and undrawn.

### 9a. Correction to §9 (claude-20 check, 2026-09-01)

§9 said the new `:dirichlet-accumulation` row was "realised offline from the
TRACE corpus". **That was wrong**, and the error was mine: I read
`corpus->concentration`'s shape and not its feeder. Checked and confirmed:
the only non-test feeder is `a4a-substrate/read-corpus`
(`a4a_substrate.clj:46-60`), which reads capability entities, mission docs,
`:capability/*` hyperedges and discharges from the **substrate over
Drawbridge**; `trace` occurs zero times in `a4a.clj`, `a4a_substrate.clj`,
`r17_offline.clj`; and no writer of capability hyperedges from WM runs was
found, so no upstream ingest rescues the claim.

Consequence: **R2→R17 and R1→R17 are not realised** — the same position
R8→R17 was in before the BMR correction. The correction replaced one
unrealised theory edge with two. What a4a actually runs BMR over is a
*different* generative model (capability × mission Dirichlet counts from
substrate graph structure), not the tick model's o and μ. Recorded in the
registry as `:realised false` on the row and a `:holes` entry.

Also from C454 (verified by claude-20): the τ fold runs every tick
(`war_machine.clj:4332-4361`) but returns the state unchanged unless a
well-formed unseen `:realized-outcome` is present, and that field is absent
on today's live path (`selection_gain.clj:187-193`). So **live τ holds at its
prior**: "fixed-calibrated" described the live behaviour, "folds realised
outcomes" the mechanism; the registry's `:temperature-update` now says both.

Conformance counts unchanged (18 theory edges; 7 drawn; 11 not drawn), since
theory edges are unchanged; what changed is that two of the eleven are now
known to be unrealised rather than realised-undrawn.

**Provenance named (claude-20, C456; verified 2026-09-01).** The substrate's
`:capability/*` hyperedges and `:discharge` entities are written by the A3
actuator (`actuator_a3.clj:31, :68, :486-487`); `a4a.clj:2-6` states that
R17's concepts are "demo-validated until real `:capability/*` production
writes flow". So the R17 feeder exists and its production writes do not yet.
Recorded on the row and in `:holes`.

**Process rule adopted (claude-20's point).** `6561d47` was written from
sound reports and regenerated into p4ng within the hour without a second
reader, and carried an unsound inference (the TRACE provenance). From here:
**an edit to `aif-equations.edn` that adds or changes a `:code`, `:realised`
or `:imports` field is read by another lane before p4ng regenerates from it**
— the same gate code gets. The provenance check is the standard step for
any such field: name the feeder, grep the store, find the writer.

### 9b. Two states in one list (claude-20, C457; fixed 2026-09-01)

`aif-conformance.edn` carried a single `:missing` list of eleven edges in
which nine drawing omissions and two things the machine does not do were
indistinguishable — the two-states-one-rendering defect, now in an artifact
rather than in prose. `gen_aif_dag.bb` now derives the split from the
registry's `:holes` and emits `:realised-undrawn` (8), `:not-realised` (2:
R2→R17, R1→R17) and `:path-dependent` (1: R6→R16) alongside `:missing`;
Figure 5B draws the three classes distinctly; the conformance table splits
the row and states the not-realised finding from the registry's own text;
the tetrahedron reads "11 not drawn (2 not realised)".

**Provenance control, mechanical half.** `pointer_check.bb` runs with the
negative controls on every publish: every `file:line` pointer in the
registry's `:code` and `:evidence` fields must resolve to an existing file
with at least that many lines (45 pointers, 0 unresolved; an invented pointer
fails naming it). It catches stale or invented pointers. It does **not**
check that the pointed code says what the field claims — that is the walk
(name the feeder, grep the store, find the writer), which stays a reading
step under the §9a gate rule.

**The substantive finding of this thread, stated plainly** (claude-20 asked
that it not become a footnote to the citation repair): **the War Machine's
model reduction runs, but not over the beliefs the loop forms.** R17 reduces
a capability × mission Dirichlet model built from substrate graph structure
written by the A3 actuator; the tick loop forms o and μ, and no path from
either into R17 was found. The machine learns structure about a different
model from the one it perceives and acts with.

**Pointer check corrected (claude-20, C458).** The first version parsed only
the end of a `file:A-B` range, so a drifted or invented start passed
(`war_machine.clj:99999-4379` → exit 0), and it scanned only `:code` and
`:evidence`, leaving five pointers unchecked — including the τ pointers in
`:temperature-update`'s `:statement`. It now requires the file to exist,
A ≤ B, and both within the file's line count, and scans every string value in
the registry rather than a field list, so coverage is total by construction
(50 pointers, 0 unresolved). Controls: an invented range start fails naming
the reason; a bad pointer inside a `:statement` fails. The boundary stands:
this catches stale or invented pointers, not wrong provenance.

## 10. The ledger

§6 is now data: `holes/labs/wm-contract/worklist.edn` (32 rows; validate with
`worklist_check.bb`), worked one row per invocation by a CLI agent under
`worklist-prompt.md`, driven by `wm-edge-loop.sh`. Statuses `:open →
:done-unreviewed → :done` (a second reader sets `:reviewed-by`); class J rows
are `:needs-joe` and never taken by the loop; rulings are never written by the
loop. The gate of §9a is a status, not a conversation: registries regenerate
into p4ng only when no row touching them is `:done-unreviewed`. As of
creation: 19 open (15 D, 2 H, 1 V, 1 C), 11 done-unreviewed (mine — awaiting
the second read), 2 needs-joe.

### 10a. The D items, done and reviewed (2026-09-01)

A CLI seat (`claude-cli`) worked the seventeen D rows one per invocation
under `worklist-prompt.md`; claude-1 read every diff, re-ran the gates, and
spot-checked the code claims before any regeneration (rows carry `:review`).
Figure 5A is now generated from the registries with every drawn edge
classified: 22 drawn (7 retired by decision, 1 carrying no data), 11
decision-added (8 on code evidence, 3 by Joe's ordering ruling, drawn apart),
and the theory layers derived from `aif-conformance.edn` (8 realised-undrawn,
2 not-realised, 1 path-dependent), stamped. The eight code-backed additions
equal the registry's `:realised-undrawn` set exactly — reached from the code
against C452 on one side and from the equations on the other.

Things the reviews settled or opened:
- **R6→R13**: two decisions retire it (Joe's ordering ruling; the code-backed
  phase-order argument). Both stand; the figure names both.
- **R14→R6** (D5c) is a reading of one code region the route tags twice —
  τ is computed inside `strategic-recommendation` (`policy.clj:243`) — kept
  because R6's scores import τ, with the note preserved.
- **R6→R4** (D5d) turns on which R6: the registry's `forward-model` imports
  π, the candidate set (`policy-set` at R6), not Q(π); realised.
- **R2→R7** (D5f) stands only because the registry's precision row imports
  `o`; `precision.clj:116-135` computes production precision from the
  *error* variance, `o` entering only the salience term. **C10** (registry:
  precision imports ε) opened, gated for a second read; **D10** (retire
  R2→R7, add R8→R7) blocked on it.
- **D9** opened: the one conditional addition (R13→R4, horizon ≥ 2) is drawn
  like the unconditional ones.
- The route-vs-dependency mechanism found in D6 — the `:R8` tag is stamped
  only at F, and the `:R7`/`:R3` tags are emitted after the loop returns —
  is the sentence the Figure 5A/5B captions need (C9).

The CLI seat found and fixed three defects in its own work with controls
before review (last-writer-wins attribution in D0; overprinted additions in
D5a; a stroke-blind control in D0b), and flagged the three rows it thought a
reviewer might reject. The reviewer's fixes were one layout defect (D8).

### 10b. The two rulings, and the second read (2026-09-01)

**Second read.** claude-20 read the twelve rows of claude-1's registry work
(C1–C8, C10, V2–V4) and signed all twelve (`b815800`), checking sources on
disk by checksum, the no-consumer claim for F exhaustively, the ΔF sign
against `bmr.clj:132-138`, and — the row asked to be doubted — every path by
which the observation could reach production precision in `precision.clj`
(none; C10 stands). The gate of §9a has now been exercised in both
directions: claude-1 read the CLI seat's D rows, claude-20 read claude-1's C
rows, and p4ng regenerated only after each.

**J1 — ruled "no."** Joe, 2026-09-01: the drawn R7→R14 was a conflation of
two precision-like scalars; the theory-aligned policy precision
(γ = 1/β with β updated from G and π, Friston 2017 eq. 2.7; Da Costa A.2) is
to be pursued instead of the selection-gain proxy. Recorded in
`control-map-edges.edn :decisions :j1-r7-r14-conflation` and
`aif-equations.edn :choices :temperature-update :ruling`. Opened **H3**
(hole: the machine's policy precision is not the formalism's γ) and **I1**
(implement the β/γ dynamics behind a flag).

**J2 — the Laplace F is a shortcut.** Joe, 2026-09-01: do not keep the
channel F as a diagnostic; retire it and replace it with free-energy
quantities the formalism actually consumes — the per-policy F_π of
π = σ(ln E − F − G) (Parr 2022 B.9), and a belief update that is the gradient
of the F the machine reports, at one grain. Recorded in
`:choices :free-energy-form :ruling`; the F row is `:status
:shortcut-to-retire` and stays in the registry and the figure until its
replacement is realised and consumed, so the drawing keeps matching the
code. Opened **H4** (hole: F_π not computed, F not consumed) and **I2**
(implement F_π into Q(π), then retire R8's scalar).

A new ledger class **I** carries the implementation track; it is separate
from this review, whose job was to find the wiring the equations force and
say where the code does otherwise. With J1 and J2 ruled, every drawn edge of
Figure 5A is now either forced by an equation, retired by a named decision,
added by a named decision with its grounds, marked as carrying no data, or
listed as plumbing — and every theory edge is drawn, realised-undrawn,
not-realised, or path-dependent, with the three unrealised ones held open as
Lean holes (H1, H2) or queued (H3, H4).

### 10c. Handover (2026-09-01)

Joe passed the collection to claude-20: `worklist.edn` carries `:steward
:claude-20` and a `:handover` note. What passes: stewardship of the ledger; the
second-read gate of §9a (claude-20 reads the CLI seat's and Codex's rows;
claude-1 remains a reader for rows claude-20 authors); regeneration and
publish of p4ng `futon-2026` from the registries, only when no registry row is
`:done-unreviewed`; dispatch of the remaining rows; the I track (I1, I2, held
on J3). State at handover: 42 done, 3 open (I1, I2, H1b), J3 `:needs-joe`
with claude-20's recommendation (c) then (a) and claude-1's reading that J1/J2
already exclude (b). Figures published through p4ng `41ec60c`.

### 10d. A methods rule, and I3 (2026-09-01)

**A file of forms is read with `edn/read` in a loop, never `read-string`.**
`clojure.edn/read-string` returns the FIRST form and stops. Both readers hit
this on the same files within an hour: C11's reproduction and its second read
each reported `wm-trace-2026-07-04.edn` as holding one record. It holds 38.
Read form by form, 07-04 has 37 records carrying both a selection and a
realized policy and 07-05 has 13 — and in **50 of those 50 the enacted action
differs from the selection, with no record anywhere in either file where they
agree**. The verdict did not change; its scale did, from two anecdotes to
every comparable record we have. H1b's `:statement` and `:acceptance` now
carry the number, with the constraint that the restated hole may state the
agreeing case as a bound to be tested but not as a claim believed true, since
nothing on record supports it.

This is the same shape as the pointer rule of §9a and the `find | head -1`
trap the CLI seat recorded under H2: **a tool that returns one thing where
there are many, read as if it had returned the thing.** The defence is the
same in each case — establish that the read was exhaustive before treating
its result as the whole.

**I3, the shared trace write** (`5febaee` + `941d243`, signed `1282248`).
Both I1 and I2 needed a write before they needed an equation:
`strip-ranked-action` dropped each candidate's `:prediction` and
`strip-decision` dropped Q(π). Behind `FUTON_WM_TRACE_POLICY_DETAILS=1`,
default OFF, a tick now carries per ranked action its predicted mean **and
variance** — F_π scores an observation under a distribution, so a mean alone
would have handed its consumer a precision to invent — plus Q(π) keyed by
`rank/N` and the live `*effects-mode*`, without which a flat per-candidate
field cannot be told from a machine that had no discrimination.

Two review points worth keeping. The delivered test for the identity of the
DEFAULT output — the flag-OFF record, which is the only one that can regress,
since the flag-ON path is new by construction and is covered by the tests —
re-listed
the 35-key whitelist and compared it to the function built from that
whitelist: a control that passes for any edit made in both places guards
nothing. It was replaced by a golden captured from the pre-I3 implementation
run against the current one in a single process (3714 = 3714), so what is
pinned is the old default bytes rather than a description of the new code. And the
measurement decided the design: +92,985 bytes/tick, 30.2% of a 307,910-byte
110-candidate record, 198,075 bytes (68%) under the naive form the C66
comment warns about — so the write won over the recompute route, which would
have needed the belief and the mode per candidate anyway.
