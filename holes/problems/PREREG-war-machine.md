# PREREG-war-machine — the whole-system preregistration: nouns, verbs, organisation, and the evidence statement

**Status:** DRAFT 2026-08-30 (claude-15, at Joe's direction, in parallel with codex-22's
R5 reading). Prose, in the shape of `DarkTower/APMDemonstrationPreregistration.lean`
so that the Lean port is mechanical: every heading below is a Lean section there.
Nothing here is a measurement; it is what is promised *before* one.
**Gate:** operator-acceptance — Joe.

> *Joe, 2026-08-30: "develop the high-level pre-registration for this whole war
> machine system where we would again talk about the nouns, the verbs, and their
> organization, and the evidence statement that we would expect the overall
> system to conform to — so we could work at both the top level and the node
> level in parallel."*

**The APM form, and what it commits us to.** `APMDemonstrationPreregistration.lean`
declares `Module`, `Invariant`, `RuntimeInvariant`, a `SystemDesign` with
`enforcedBy : Invariant → List Module` and a *proof that every named enforcer is
installed*; a recorded `Trace` with registered measurement fields; observables
F1–F9 as probe-backed predicates over a trace; a round-one registration whose
unknowns (cost, budget, teardown, stop rules, decision rule) are **explicit
arguments, not invented constants** — "the formal underspecification result of
this pass"; and the gate `no_round1_witness_of_failed_invariant`: a run cannot
count as a witness if any runtime invariant fails on the smoke trace. The War
Machine version keeps all of that, and adds what APM lacked (v2 §0.5, second
condition): at least one **external** term with a falsifier the apparatus cannot
satisfy by construction.

---

## 1. Modules — the nouns (R-nodes), with their Gate-0 class

```lean
inductive Module
  | R1 | R2 | R3 | R4 | R5 | R6 | R7 | R8 | R9 | R10 | R11 | R12 | R13 | R14 | R15 | R16 | R17 | R20   -- on Figure 4
  | R18 | R19                                                                                            -- off the figure
```

| module | column | noun (paper) | Gate-0 class today | source |
|---|---|---|---|---|
| R2 | PERCEIVE | structured observation vector | stack-defined (14 channels; docstring says 13) | R2 worksheet |
| R3 | BELIEVE | predictive-coding update μ ← μ + αΠε | theory-defined formula over stack-defined μ, o | glossary |
| R1 | BELIEVE | belief state μ | stack-defined | glossary |
| R7 | BELIEVE (support) | evidence precision Π | theory-defined at channel level | badge `:derived-from-FEP` |
| R8 | BELIEVE (support) | **two nouns**: F (present-fit per tick); g (gain reading) | F theory-defined; g stack-defined, honestly | R8 worksheet |
| R4 | EVALUATE | predictive forward model | stack-defined | completeness doc |
| R5 | EVALUATE | **two nouns**: G_core = risk + ambiguity; the coverage report | formulas theory-defined; coverage Lean-defined, no caller | R5 worksheet |
| R6 | SELECT | **three nouns**: candidate space; softmax-with-abstain; generation | borrowed / stack-defined / undefined | R6 worksheet |
| R13 | SELECT | temporal depth / rollout | stack-defined (sequences, not cascades) | `f7aa044` |
| R14 | SELECT | **one noun renamed**: γ → g, "commitment temperature" | stack-defined, honestly; γ borrowed then retired | R14 worksheet |
| R11 | SELECT | budget arbitration | stack-defined, campaign-witnessed | `sec-catalog` "Current evidence" |
| R16 | ACT | grounded actuation, not re-observation | stack-defined; the whitelist lived here | T-fixture-becomes-registry |
| R15 | ACT (support) | hierarchical / temporal depth | stack-defined | |
| R9 | assurance | no self-certification (L1 / L2) | **theory-defined as a rule** | glossary |
| R10 | assurance | scheduled observer entrypoint | stack-defined; off since 07-14 | |
| R12 | assurance | two-layer calibration | stack-defined | |
| R17 | learning | structure learning by BMR | theory-defined formulas (ΔF, Bayes factor) | glossary |
| R20 | learning | interoceptive tripwires | stack-defined; watches the coding runner, not this loop | v1 §0 |
| R18 | (off figure) | faithfulness meta-criterion | the apex's ancestor, inside the triangle | v2 §0.8 |
| R19 | (off figure) | prior preferences C | **undefined for the WM** | R5 worksheet |

Gate-0 count: theory-defined nouns — R3's formula, R7, R8-F, R5's formulas, R9,
R17's formulas; borrowed or undefined — R6's space, R14's γ, R19, every
occurrence of "policy" and "cascade"; the rest stack-defined. **The gate does
not pass at the system level**, which is the honest starting state of a
preregistration and is why Lean-side definition (P-validated-R5 §2b) precedes
any round one.

## 1b. Are the paper's patterns pegged to the library and the figure? — the peg table (2026-08-30)

*Joe: "the standard definition of what this machine is meant to be is in the PLoP paper
and has both the wiring diagram and pattern statements in it. So they're all supposed to
be pegged together in a coherent order … missing patterns is rather concerning."*

Checked, four pegs per node: (1) a catalogue paragraph in `sec-catalog.tex`; (2) a
library flexiarg with that pattern's title; (3) an `@holds-at R<n>` directive in that
file naming its node; (4) a row in the R1–R16 pattern map that cites the file. Plus
whether the node is on Figure 4.

| node | catalogue paragraph | library pattern | `@holds-at` | map row cites file | on figure |
|---|---|---|---|---|---|
| R1 | Belief State as Operational Hypotheses | `aif/belief-state-operational-hypotheses` | no | row, not cited | yes |
| R2 | Structured Observation Vector | `aif/structured-observation-vector` | no | yes | yes |
| R3 | Predictive-Coding Belief Update | `aif/predictive-coding-belief-update` | no | row, not cited | yes |
| R4 | Shared Kernel for the Predictive Forward Model | `aif/shared-kernel-predictive-forward-model` | no | yes | yes |
| R5 | EFE Inside an Auditable Controller | `aif/expected-free-energy-scorecard` | no | row, not cited | yes |
| R6 | Candidate Pattern Action Space | `aif/candidate-pattern-action-space` | no | yes | yes |
| R7 | Evidence Precision Registry | `aif/evidence-precision-registry` | no | yes | yes |
| R8 | Present-Fit Mismatch as a Per-Tick Scalar | `aif/free-energy-as-tick-scalar` | no | yes | yes |
| R9 | No Self-Certification | `aif/no-self-certification` | no | yes | yes |
| R10 | Scheduled Observer Entrypoint | `aif/scheduled-observer-entrypoint` | no | yes | yes |
| R11 | Hierarchical, Budget-Aware Action Selection | `aif/hierarchical-budget-aware-action-selection` | no | yes | yes |
| R12 | Two-Layer Calibration | `aif/two-layer-calibration` | no | row, not cited | yes |
| R13 | Temporal Depth Beyond the Greedy Step | `aif/temporal-depth-beyond-greedy` | no | row, not cited | yes |
| R14 | Selection Gain as Commitment Temperature | `aif/policy-precision-commitment-temperature`; `problems/commitment-temperature-is-instrumented-as-gain` (`@holds-open R14`) | via `problems/` only | row, not cited | yes |
| R15 | Hierarchical and Temporal Depth | **none** (title-matches R13's file) | — | row, not cited | yes |
| R16 | Grounded Actuation / Semilattice-Rollout Witness | `aif/grounded-actuation-not-reobservation` | no | row, not cited | yes |
| R17 | Structure Learning by BMR | `aif/structure-learning-by-model-reduction` | no | **no row** | yes, isolated |
| R20 | Interoceptive Tripwires | **none** | — | **no row** | yes |
| R18, R19 | (no paragraph) | — | — | — | off figure |

**Findings.** (i) **"Missing pattern" was my method's artefact for R11 and R17** — §2e
assigned patterns to nodes only via `@holds-at` and via map rows that cite a file, and
neither node had either; both have library patterns. Corrected. (ii) **Two nodes
genuinely have no library pattern: R15 and R20.** The paper has paragraphs for both; the
library has neither. (iii) **No `aif/` pattern declares its own node.** Zero of the 31
carry `@holds-at`; the thirteen files that do are `war-room/` (6), `problems/` (5),
`features/` (2) — rulings and problem statements point at the map, the mechanism
patterns do not. So the peg from library to figure exists only as (a) title similarity
and (b) a markdown table in `futon2/holes/` that cites the file for 8 of 16 rows and has
no row for R17 or R20. (iv) The catalogue itself carries the peg in its numbering and
nowhere else — a `\paragraph{… (R<n>)}` header; the figure carries it as a `node-id`
text; the library carries it in 13 files, none of them the mechanism's own.

**Done 2026-08-30 (Joe: "get the R numbers coherent with the library … Go") — futon3
`2fbb58a`.** `@holds-at R<n>` added to the 16 existing `aif/` mechanism patterns, one
line each, naming the catalogue paragraph that numbers it; the two missing patterns
written from their paragraphs — `aif/hierarchical-and-temporal-depth` (R15) and
`aif/interoceptive-tripwires` (R20) — with the paragraph's IF/HOWEVER/THEN/BECAUSE and
the paper's own evidence and counterfactual under BECAUSE; their digests and `@see-also`
edges added to the spider pilot's baseline so `aif/` stays green (linter: pass on both
pilot sections after the change). The 07-13 pattern map was left as a dated document.

The four checks, re-run after the commit:
1. *every catalogue R-paragraph has a library pattern whose `@holds-at` names its node* —
   **holds** for all 18 figure nodes (R18, R19 have no paragraph and remain off-figure).
2. *every `@holds-at` names a figure node* — **holds**: 32 `@holds-at` directives and 4
   `@holds-open`, every target on Figure 4.
3. *map rows cite their files* — **not done**; superseded, since the peg now lives in the
   files themselves.
4. *the figure's node set equals the set of nodes some pattern holds at* — **holds**, in
   both directions.
One refinement the re-run forced: "exactly one pattern per node" is the wrong
condition — 11 nodes now have more than one holding pattern (R2 has four, R8 three),
because rulings (`war-room/`), problem statements (`problems/`) and features point at
the same node the mechanism holds at. The condition that fits the library's own
directive semantics is: **exactly one *mechanism* pattern in `aif/` per node, and any
number of rulings/problems/features `@holds-at` the same node** — which is what the
library now has, and what a spider's attestation of an `aif/` edge can be checked
against.

**What "pegged together in a coherent order" would require, as checks the linter can
run:** every catalogue R-paragraph has exactly one library pattern whose `@holds-at`
names that node (R15 and R20 to be written; the other 16 to gain the directive — a
spider-fleet job of the kind now piloting, with the paper as the attestation source);
every `@holds-at R<n>` names a node on the figure; the pattern map's rows cite the
file they describe; and the figure's node set equals the set of nodes some pattern
holds at. Today none of the four holds. The R-numbering is the peg, and it lives in
three artefacts that nothing joins.

## 2. Edges — the verbs, from Figure 4 as data

Lifted 2026-08-30 into `p4ng/empirics-futon/control-map-edges.edn` (criterion 1 of
`M-formal-war-machine` §1.6, in draft; vocabulary from `fig-loop.edn`).

**Ten control edges, one cycle plus one join.** The cycle:
`R2 →observe→ R3 → R1 →predict→ R4 → R5 →rank→ R6 → R13 → R14 → R16 →re-observe→ R2`;
the join: `R11 →arbitrate→ R16`. (Labels matched by sampling each curve, not by
chord midpoints — a first pass put "arbitrate" on R14 → R16; the figure puts it
on the budget-arbitration join, 6 px from the curve. Recorded because the
figure, not a reading of it, is the authority until the EDN replaces it.)

**Eleven support edges (a twelfth path has an unmatched start near R5, recorded
`:unresolved`):** R7 → R3, R7 → R8, R7 → R14 (precision supplies belief, the gain
reading, and the temperature); R8 → R5; R10 → R8; R12 → R7; R20 → R7; R9 → R16;
R15 → R13; R15 → R16; R6 → R11.

```lean
inductive EdgeKind | control | support
structure Edge where (from to : Module) (kind : EdgeKind) (label : Option String)
```

**A finding from the lift (2026-08-30): the learning band is disconnected.** R17 is in
Figure 4's node set and in **no edge**; nothing feeds R6 except R5's `rank`. The
catalogue names *R16 → R6, Discharge-Trained Cascade Proposal* and *R17, Structure
Learning by BMR* as patterns, and the figure draws neither edge. So the operation Joe
specifies in `P-organise-the-library.md` §7 — the War Machine reorganising the pattern
graph in the loop from certified outcomes — has no edge to live on as drawn. Registered
here as a **required, undrawn edge**: `R16 → R6` (via R17), kind `support`, status
`:missing`, whose `Delivery` contract is §7's laws R1–R5 (reachability and acyclicity
preserved; edges move only on a `Certification` from a realised outcome; certification as
the third attestation rung; the retriever falsifiable on certified-irrelevant patterns).
Invariant `I_R6_reorganise` is added to §4 accordingly.

Each edge, under Gate 1 (v2 §0.6), carries a `Delivery`: payload schema, guarantee,
`atomic-with`, retry, timeout, idempotence key, receipt. **None of the twenty-one
has one today.** The worksheets' §4 tables are the *internal* deliveries of five
nodes; this table is the *external* ones, and it is empty. That absence is
registered here as a finding, not filled.

## 2c. Theory-derived edges versus drawn edges — the audit (claude-15, 2026-08-30)

*Joe: "maybe there's other edges which are missing, or in the diagram but untyped so
that they're underspecified. And we should be able to derive all of that from the
theory, such as it's written down in the glossary."* Method: for each of the 18
R-nodes, take the catalogue paragraph's THEN clause (what the node computes from
what) and the glossary entry for its quantity, list what the node **consumes** and
**produces**, and diff the implied edges against `control-map-edges.edn`. One reader;
a blind second derivation was dispatched to codex-22 the same day for a diff of
derivations, not a confirmation.

**Consumes / produces, from the text**

| node | consumes (by its own THEN / glossary) | produces |
|---|---|---|
| R2 | session/scan state; *operator turns* (the ring) | o |
| R3 | o (R2), μ (R1), Π (R7) | μ′, and "a running average of the size of recent misses" — the variance that precision learns from |
| R1 | R3's update | μ, for R4 and R8 |
| R7 | prediction errors (R3), outcome volatility (R16/R12), trips (R20) | Π, "applied wherever prediction error, **scoring, or gating** occurs" → R3, R8, R14, **R5, R16** |
| R8 | "a pure function of belief, observation, and precision" → **R1, R2**, R7 | F, to the trace "for validation, diagnostics, and alarms" |
| R4 | μ (R1), **an action** ("kernel (state, action, seed) → next-state") → the candidates, R6 | predicted next-state distribution → R5, R12 |
| R5 | predictions (R4), **preferences C (R19)**, Π (R7) | G per candidate → R6/R13 |
| R6 | "retrieval plus gating rules" → the library (off-figure), the goal (R1), learning (R16/R17) | the candidate set → R4, R5, R13, R11 |
| R13 | cascades (R6), scores (R5), R15's coupling | S(π) → R14 |
| R14 | S(π) (R13); "diagnostic signals (recent regressions, **uncertainty in the belief state, contradiction rate**, time pressure)" → **R8**, R3/R1; the gain from realised outcomes → **R16**; trips → R20 (chartered) | the selection → R16 |
| R11 | proposals (R6), budgets | arbitration → R16 |
| R15 | strategic selection; "witnessed tactical outcomes" → **R16** | the tactical target → R13, R16 |
| R16 | selection (R14), arbitration (R11), R9's constraint, Π for gating (R7) | the witness → R2 (re-observe); realised outcome → **R14, R15, R12, R6/R17**; "a dial moved" → **R10** |
| R12 | predictions (**R4/R5**) and witnessed outcomes (**R16**); R9's tag rule | calibration → R7 |
| R9 | evidence records at birth | tags that gate → R16, **R12, R2** |
| R10 | the schedule; "evidence that a tick changed observable state" → **R16** | the tick → **R2** (the whole loop), R8 |
| R17 | accumulated counts (the trace: R8; outcomes: R16) | a reduced generative model → **R1/R4**; vocabulary → **R6** |
| R20 | machine trajectory at phase boundaries | trips → R7 (drawn), R14 (chartered) |

**The diff**

*Drawn and derived — consistent (13 of 21):* R2→R3, R3→R1, R1→R4, R4→R5, R6→R13, R13→R14, R14→R16, R11→R16, R16→R2, R7→R3, R7→R8, R7→R14, R9→R16, R6→R11, R12→R7, R15→R13, R20→R7.

*Drawn but not derived — underspecified (4):*
- **R5 → R6 "rank"** — the direction is the paper's own R6 drift, drawn: if R6 is the *candidate space*, it must precede R4/R5 (you cannot predict or score candidates you do not have), so the theory wants **R6 → R4** and **R6 → R5**; if R6 is *softmax-with-abstain*, R5 → R6 is right. The figure draws one edge for two nouns.
- **R8 → R5** — nothing in R5's THEN consumes F; R8's own THEN emits to the trace for "validation, diagnostics, and alarms". Either the edge means something the text does not say, or it belongs to R14 (below).
- **R10 → R8** — why the per-tick trace specifically, rather than the tick (R2)? R10's THEN triggers the loop and gates on state change.
- **R15 → R16** — "strategic selection fixes the tactical target": defensible, but the coupling's *return* leg is the one the THEN emphasises and it is not drawn.
- (the twelfth support path, `:unresolved`)

*Derived but not drawn — missing (12), grouped:*
- **The gain chain, R16 → R14** — realised outcomes feed g. This is the entire subject of `E-R8-red-ring-fill` and family 1–5 of the mission, and the figure does not draw it.
- **R8 → R14** — F is exactly the "uncertainty / contradiction" signal R14's THEN couples the dial to; the drawn R8 → R5 may be this edge misdirected.
- **R3 → R7** — precision learns from prediction errors (E-precision-over-policies: "learns per-channel precision from prediction-error rolling-variance"); only R7 → R3 is drawn.
- **R1 → R8, R2 → R8** — R8 is "a pure function of belief, observation, and precision"; only precision is drawn.
- **R7 → R5, R7 → R16** — R7's THEN: "wherever … scoring, or gating occurs".
- **R19 → R5** — risk is KL against C; C is R19 and is not on the figure at all.
- **R4 → R12, R16 → R12** — Layer 1 / Layer 2 compare predictions with realised outcomes; R12 has no inputs drawn.
- **R16 → R15** — "witnessed tactical outcomes update the next strategic calibration state".
- **R16 → R10** — R10 gates "live" on "a dial moved, a hole closed"; that evidence is R16's.
- **R10 → R2** — the schedule starts the tick at PERCEIVE.
- **R9 → R12, R9 → R2** — the tag-at-birth rule is what L2 gating and evidence records run on.
- **R16 → R6 / R17 → R6, R17 → R1/R4** — the learning band (already registered §2 as `:missing`).

**Count:** 21 drawn; 13 consistent; 4 underspecified (one of them a direction error for one of R6's two nouns); **12 derived edges undrawn**, of which four — R16 → R14, R8 → R14, R19 → R5, R16 → R12 — are edges whose absence *is* a red ring or an open family. The two nodes with the most missing inbound edges are R12 (none drawn) and R14 (the gain leg); the node with the most missing outbound is R16 (five). The figure draws the forward cycle well and almost none of the returns.

**What this does and does not establish.** These are the edges the *text* implies; whether the *code* has them is a separate audit (`P-validated-R5` §3e's O2 applies: an edge is authored, not inferred — here the author is the catalogue). Two derivations will be diffed before any of this is drawn back into the SVG.

## 2d. Two derivations diffed — claude-15 (§2c) against codex-22 (blind), 2026-08-30

codex-22 derived independently from the same three sources without opening this file
(`PREREG-war-machine-edges-codex.md`, 137 lines; method: immediate data/control
dependencies only, no transitive edges; R6 split into **R6-C** candidate space and
**R6-H** softmax-with-abstain). Counts: codex A/B/C = 14 / 8 / 22; claude-15 = 17 / 4 / 12
(my §2c "13 consistent" undercounted its own list of seventeen). The lists are kept
apart; nothing below is a merge.

**Agreed — drawn and derived, both:** R2→R3, R3→R1, R1→R4, R4→R5, R13→R14, R14→R16,
R16→R2, R6→R11, R7→R3, R7→R8, R9→R16, R15→R13, R20→R7. (13)

**Agreed — drawn but not derivable, both:** R8→R5 (R8's own text: the present-fit
score "is no substitute" for the action score — `sec-catalog.tex:200`); R10→R8; R15→R16
(the stated feedback runs the other way — `:243`, checked); the `:unresolved` twelfth path.

**Disagreed on drawn edges — codex reclassifies four I accepted, with reasons I find good:**
| edge | claude-15 | codex-22 | verdict |
|---|---|---|---|
| R11→R16 "arbitrate" | consistent | not derivable: "the text constructs/scores a cascade before enactment" (`:245–254`); arbitration's output is not an actuation command; derived route is R11→R13→R16 | **codex** — its citation stands: `sec-catalog.tex:247`, *"Having chosen \emph{what} to do, the system assembled \emph{how}: a bounded cascade of patterns fitted to the selected target"* (the specimen-run narrative, under the `10:51:25` subsection at `:245`). I wrongly recorded it as "nowhere in the source" for about an hour: my grep was blind to the `\emph{}` macros inside the phrase. Corrected 2026-08-30 |
| R7→R14 | consistent (on the mission's "R7 supplies R14") | not derivable: R14 consumes *belief uncertainty and diagnostic pressure*, not the registry (`:239`) | **codex**; the mission's §1.1 sentence was the drawing read back, not the text |
| R12→R7 | consistent | not derivable: no sentence says calibration's result updates precision; "that feedback rule would have to be stated, including which Layer can change which channel's precision" | **codex** |
| R6→R13 | consistent | under R6-C not derivable: the candidate set feeds prediction/scoring (R4, R5), and the cascade R13 scores is constructed *after* selection | **open** — see below |
| R5→R6 "rank" | underspecified (direction) | derived under R6-H only; under R6-C reversed to R6→R5 | **agreed in substance** |

**Agreed — derived but not drawn, both (14):** R1→R8, R2→R8, R6-C→R4, R6-C→R5,
R7→R5, R8→R14, R4→R12, R9→R12, R16→R10, R16→R12, R16→R15, R10→R2, and the learning
band (mine as R16→R6 / R17→R6; codex as R16→R17 and R17→R6-C — same band, codex's
routing is the better-typed one).

**Only codex-22 derived (8) — what I missed:**
- **R5→R12** — Layer 1 compares predicted G with the model's *own later scores*
  (`:295`, checked; glossary *No self-certification*). I had only R4→R12.
- **R16→R1** — "the witness feeds the next belief" (`:309`), direct; I routed it through R2.
- **R11→R13, R13→R16** — the post-selection construction reading (below).
- **R5→R14, R1→R14** — scores and belief uncertainty as R14's inputs, stated separately
  from R13→R14 and R8→R14.
- R1→R3 (a belief is an input to its own update); R2→R7 (precision "keyed to observation
  features", `:198`).

**Only claude-15 derived (5) — what codex missed:**
- **R16→R14, the gain chain.** The glossary's *Softmax and controller calibration*:
  *"The effective temperature is set through the outcome-feedback selection gain g, as
  τ_eff = 1/g"* (checked) — g is fed by realised outcomes, which are R16's product. Codex
  derived R14 from its catalogue paragraph (the dial and its diagnostic signals) and did
  not take the glossary's definition of g as an edge. This is the edge `E-R8` and
  families 1–5 exist for; its absence from a 22-edge derivation is the clearest sign that
  the catalogue paragraph and the glossary entry for R14 describe two different nodes
  (R14 worksheet §0: "one noun renamed").
- R3→R7 — precision learns from prediction-error variance (E-precision-over-policies;
  the glossary's *Belief state* variance-EMA). Codex has R2→R7 instead.
- R7→R16 — R7's "wherever … gating occurs" (`:198`); codex stopped at scoring.
- C→R5 — codex rules R19 not determinable ("replaced by the unnumbered preference
  distribution C", `:41–50`) — correct as a node, and the *edge* from C into R5's risk
  term still has no source on the figure.
- R9→R2 — the tag-at-birth rule on evidence records.

**The one substantive disagreement, and it is the R6/R14 boundary.** Codex reads the
catalogue chronologically — selection (R11/R14) precedes construction (R13) precedes
the witness (R16): R11→R13→R16 — citing `sec-catalog.tex:247`: *"Having chosen what
to do, the system assembled how: a bounded cascade of patterns fitted to the selected
target."* The citation is exact (verified 2026-08-30, second attempt). Two errors of mine
on the way to verifying it are recorded in the lifecycle log, row 6: I wrote "checked"
before the check returned, then declared the phrase absent because my grep could not
see through `\emph{}`. The sentence is from the specimen-run narrative rather than a
pattern's THEN clause — which is exactly the kind of evidence the apex question asks
about, and here it is the right kind: it is the paper describing what the system did. The figure, and my §2c, put construction/scoring (R13)
*before* the temperature (R14): candidates → cascades scored → commit → act. These are
the two orderings of `find ∘ organise` relative to selection from `P-validated-R5` §3d–§3e
— does the policy build the cascade and then select, or select a target and then build
the cascade for it — and the text supports codex's reading for the *mission* grain
(select a mission, then construct its cascade) and the figure's reading for the *pattern*
grain (rank cascades, then commit). Two grains, one arrow. It is the boundary Joe named
as the decision to draw once, and both derivations have now located it independently.

**Decided (Joe, 2026-08-30): target first, then the cascade constructed to match.** The
R6/R14 ordering is codex-22's — selection precedes construction — as a *decision*, not
as the citation codex offered. Derived control path under the catalogue's R6:
`R6-C → R5 → R14 (commit to a target) → R13 (construct and score its cascade) → R16`,
with R11 → R14 (arbitration informs the target) replacing the drawn R11 → R16, and the
drawn `R5 →rank→ R6 → R13 → R14` retired for the catalogue reading. Recorded in
`control-map-edges.edn` as `:decisions {:r6-r14-order :target-first}`.

**Net.** Agreed edges: 13 drawn, 14 undrawn. The drawn figure has **4 edges both
readers refuse and 4 more codex refuses with textual reasons I accept**; the theory
implies **at least 14 undrawn edges both readers found, plus 8 only codex found and 5
only I found**, of which R16→R14 is the one whose omission from either list would be a
defect in the derivation rather than in the figure. The SVG is not redrawn; the next
artefact is a `:derived` status in `control-map-edges.edn` per edge, with the
derivation(s) that found it, and the R6/R14 ordering left as `:open` until Joe draws it.

## 2e. The library's map versus the wiring's map — computed 2026-08-30 (Joe's ask)

*"Compare the library-based map of how and why with the one implied by the wiring
diagram."* Both are data now, so this is a join, not a reading.

**Method.** Patterns were assigned to R-nodes two ways: by their own `@holds-at` /
`@holds-open` directives (13 files, all in `war-room/`, `features/`, `problems/`), and by
the R1–R16 pattern map's `aif/<name>` citations (21 files) — 34 patterns over 16 R-nodes.
Every `@why` / `@how` / `@see-also` edge between two assigned patterns was projected
onto the pair of R-nodes; the induced R-graph was diffed against the 47 wiring edges
(22 drawn, minus the unresolved path, plus 26 derived).

**Result 1 — the wiring has almost no pattern-level rationale behind it.** Of 47 wiring
edges, **4** have any library edge between the patterns at their endpoints:
`R8 → R5` (drawn), `R2 → R8`, `R8 → R14`, `R5 → R14` (all derived-undrawn). The other
43 — including every edge of the forward cycle except none — connect nodes whose
patterns cite nothing of each other. Among the 34 assigned patterns there are only
**11 edges**, inducing **7 R-level edges**, every one of which passes through a
`war-room/wr-*` ruling (WR-27, WR-16, WR-25): the R-level graph the library implies
is the *rulings'* graph, not the mechanisms'.

**Result 2 — where the two maps overlap, they run in opposite directions, and that is
the semantics, not an error.** The library says `R14 → R5` and `R14 → R8` (a pattern at
R5 or R8 `@why`-cites WR-27, which holds at R14); the wiring derives `R5 → R14` and
`R8 → R14`. `@why` is *authority* — "rests on" — and the wiring is *data* — "consumes
from". A node rests on the ruling of the node it feeds. So the library map, where it
exists, is approximately the **transpose** of the wiring map, and a check that
"library edges agree with wiring edges" would be wrong in both directions: agreement
means the two relations have been confused (§2.1e's distinction between authority
structure and trajectory; `README-flexiarg` §5a's "not inverses"). The correct
consistency condition is: *for a wiring edge `A → B` (B consumes A), the pattern at B
may `@why` a ruling that holds at A, and never the reverse.* Two library edges violate
nothing but also match nothing: `R2 → R14` (WR-27 rests on WR-16) and `R5 → R9`
(no-self-certification rests on WR-25) are authority edges with no data counterpart.

**Result 3 — corrected by §1b the same day.** My assignment method found no pattern for
R11 and R17; both exist in `aif/` and are simply unpegged (no `@holds-at`, not cited in
the map). The nodes with *no* library pattern are R15 and R20 (§1b). R17 remains the
node isolated in the wiring. Coverage per node is thin everywhere: R5 has five patterns,
R2 and R8 four, most nodes one or two.

**What this gives the two audits.** For the spider fleet: the sections that matter to
the War Machine are not only `aif/` but `war-room/`, `features/` and `problems/`, because
those are where `@holds-at` lives — and the fleet's edges will be *authority* edges, so
the consistency condition above is the check to run on them, not agreement with the
wiring. For the wiring: 43 of 47 edges have no rationale a reader can follow from a
pattern; each is a `@why` or `@how` that does not exist yet, and the derivation tables
of §2c–§2d are the list of what they would have to say.

## 3. Organisation — what the diagram gives, once it is data

- **Five columns** (PERCEIVE, BELIEVE, EVALUATE, SELECT, ACT) and a band
  (assurance, learning).
- **The cycle is the transition system**; the support edges are constraints on it
  — the §1.4 hypothesis that support edges play APM's "policies" is now
  *testable*, since both are data.
- **Which edges are transitions and which are constraints** is a typing of the
  edge set: `control` edges move the tick; `support` edges must hold *at* a tick.
  R7's three support edges say precision is read by three stages; R9 → R16 says
  the act is constrained by no-self-certification; R20 → R7 says trips are
  precision evidence (chartered, not built).
- **The recursion (v2 §0.9):** R8, R14, R5, R6, R2 have internal wiring and their
  own tetrahedra; the rest are vertices of this one until shown otherwise.

## 4. Invariants — what a conforming run may not do

**`I_data_current`** (Joe, 2026-08-30): every read of evidence, observations or patterns by an instrument or loop is against the live store and records the basis it read at; a negative produced from a dated export/snapshot is a violation (lifecycle §0.7 invariant; first instance row 25 — the spider's rung 1 over `migration-export`).

**`I_absent_is_loud`** (Joe, 2026-08-30, from AUD-D1): every read of a named input file reports absence or unparseability (fail closed, or an explicit `:missing`/`:unreadable` in the output) and never renders it as an empty result; optional inputs are declared at the read site. Falsifier: a `when-let`/`some->`/`(catch _ nil)` over a file read with no declared optionality (lifecycle §0.7; first instance row 26 — `stack-logic-model.edn`/`alignment.edn`, planned 05-03 in M-war-machine.md, never produced, read silently by `war_machine.clj` and `joe_hud.clj` since). Instrument: AUD-D2 source lint.

Per node, from the worksheets and the mission's families; each is either
*structural* (about the record's shape; checkable on any tick) or *runtime*
(about behaviour; checkable only on a run).

```lean
inductive Invariant
  | I_R2_schema      -- every tick's :observation has exactly the declared channels     (structural)
  | I_R2_readsTurns  -- an operator-turn channel exists and a ≥111-item window's inference
                     --   differs when it is held constant                                (runtime; EXTERNAL)
  | I_R8_F           -- every tick carries F = ½ mean(Π ε²) recomputable from its fields    (structural)
  | I_R8_gL2         -- g is updated only from outcomes the model did not produce (L2)     (runtime)
  | I_R8_domain      -- every producer's domain is declared; outside it → typed absence    (structural; family 5)
  | I_R8_thread      -- one tick identity threads outcome, fold and record                 (structural; family 1)
  | I_R14_governs    -- the selected action is a function of τ_eff (∃ τ₁ τ₂ with different picks) (runtime)
  | I_R5_coreApart   -- :G-core = risk + ambiguity, persisted beside :controller-score      (structural; holds)
  | I_R5_discrim     -- the EFE core changes some argmin across the corpus                  (runtime)
  | I_R5_coverage    -- every close carries a coverage statement; outside → :uncovered      (structural)
  | I_R6_surveyed    -- every registered proposer leaves an attestation per tick            (structural; family 9)
  | I_R6_abstainable -- the live selector can reach :abstain                                (runtime)
  | I_R9_noSelfCert  -- no status surface reports "observed" without a witness record       (structural; the apex)
  | I_R16_enactment  -- under live-wire, every tick carries :enactment or a typed reason     (structural)
  | I_loop_stops     -- k consecutive ticks with no valid G → the loop halts and says so     (runtime; §3.1k)
  | I_R6_reorganise   -- the pattern graph changes only on a Certification from a realised
                     --   outcome, preserving reachability and acyclicity                     (runtime; EXTERNAL via R16)
```

`enforcedBy : Invariant → List Module` — and **the proof that every named enforcer
is installed is exactly what the WM lacks**: today the only installed enforcer for
any of these is `ContractEmitter.lean`'s clause list (R8's families, R5's coverage
clause) with no Clojure consumer, so `enforcersInstalled` fails for every runtime
invariant. The APM prereg's theorem `every_enforcer_is_installed` is, for the WM,
the first thing that cannot be proved, and the preregistration says so.

## 5. Recorded trace — the registered measurement fields

The tick record as it exists (`wm-trace`, schema ≥ 8): `:timestamp :observation
:mu-pre :mu-post :prediction-errors :precision-state :variational-free-energy
:free-energy{:controller-score …} :ranked-actions :decision{:tau :type :target}
:selection-gain :habit-prior-state :policy-support-exclusions :enactment
:realized-outcome :act-gate-verdicts :wm-version`. Registered here **as found**;
each invariant above names which fields it reads. Fields the invariants need and
the record lacks — registered as *absent*, not invented: per-proposer
attestations (I_R6_surveyed); a `DomainMismatch` receipt (I_R8_domain); an
operator-turn channel (I_R2_readsTurns); a coverage statement on the close
(I_R5_coverage); a typed no-enactment reason (I_R16_enactment).

```lean
structure Trace where  -- one tick, as recorded; Option for fields that may be absent
  timestamp : String;  observation : List (String × Float);  … ;  enactment : Option Enactment; …
```

## 6. Observables — the evidence statement, as probe-backed predicates

Each invariant becomes `Observable Trace := { name, holds : Trace → Prop, decidable }`,
computed by a probe over the record — never by a flag the record sets about
itself (that is `claimPersisted`, refused in v2 §0). **Retro-trip: the observables
that fire today**, on the corpus as it stands, which is what makes this a
preregistration rather than a description:

| observable | fires on | evidence |
|---|---|---|
| I_R8_F | all 88 outcome records | `:free-energy` holds G; no F |
| I_R8_domain | every tick 07-09 → 07-21 | whitelist, bare `nil` |
| I_R14_governs | all 31 ticks since 07-14 | τ ≡ 1.0; `chosen = first controller-entries` |
| I_R5_discrim | 674 ticks | ambiguity within-tick sd 0.0039 |
| I_R5_coverage | every close | all `:unwitnessable` |
| I_R6_surveyed | every tick | no attestation exists |
| I_R6_abstainable | since 07-23 | live boundary bypasses the check |
| I_R2_schema | 2 ticks (05-18) | 13 keys, not 14 |
| I_R16_enactment | every tick 07-06 13:04Z → 07-21 | no `:enactment`, no reason |
| I_R9_noSelfCert | the pattern map, `r18` "repair: complete", `wr-overlay` 12 × `:holds true` | status without witness |

**The evidence statement the whole system must conform to**, in one sentence:
*on the next run, every structural observable holds on every tick, every runtime
observable has a positive witness and a named falsifier, and no status surface
says more than the observables returned.* The external term (v2 §0.5): I_R2_readsTurns
— the operator's turns, which the apparatus cannot fabricate — and, once R5's
Outcome and C exist, a zero-mass outcome under Q(o∣π) (T1512Z) that the run can
produce and the model forbids.

## 7. Round one — parameterised, with the unknowns as arguments

Following APM: one descriptive arm, no treatment axis; the pilot unit is **one live
click** on the enacting runner (`wm_scheduled_run`, not `wm_outer_loop` — the
wrong-corpus facade named). Left as **explicit arguments**, not chosen here:

```lean
noncomputable def round1Registration
    (Outcome : Type) (problem : ProblemUnit)          -- which mission the click selects; the Outcome carrier
    (estimatedCost budgetCap : ℝ) (teardownDeadline : Option ℝ)
    (stopRules : List (StopRule Trace)) (stopRulesNonempty : stopRules ≠ [])   -- I_loop_stops's k, at least
    (decision : DecisionRule Trace Outcome)           -- what the click's result means, decided before it runs
    : ProspectiveRegistration ProblemUnit Trace Outcome
```
The formal underspecification result, stated: the WM cannot register a round one
until `Outcome` exists (P-validated-R5 step 1), a stop rule is written (§3.1k), and
the decision rule is committed before the click — the three things that were
absent from every July run, which is why those runs could not be witnesses.

## 8. The gate

```lean
theorem no_round1_witness_of_failed_invariant :
  (∃ i : RuntimeInvariant, ¬ (observable i).holds smoke) → IsEmpty (ReadyToRun round1Base e smoke)
```
Read for the WM: **a run on which any runtime observable fails on the smoke trace
is not a witness of anything**, and no surface may report it as one. Applied
retroactively it voids every ✓ awarded between 07-08 and 07-21; applied
prospectively it is the sentence the July agents needed and did not have.

## 9. What this preregistration does not do

It does not define the nouns (P-validated-R5 does that, R5 first); it registers
their Gate-0 class. It does not pick Outcome, C, the stop rule or the decision
rule; it exposes them as arguments. It does not claim the twelfth support edge;
it records it unresolved. It is a document until its Lean port compiles and its
`enforcersInstalled` proof fails in the expected places — which is the first
test of it, and it is expected to fail.
