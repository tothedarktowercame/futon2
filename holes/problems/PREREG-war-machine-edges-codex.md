# Preregistered derivation of War Machine R-node edges

**Date:** 2026-08-30  
**Reader:** codex-22  
**Source commits:** `p4ng` `492ec2af9b2f637090558a28b89b4beedede11e2`; `futon2` `5471f91e9213f6806ea7bc92eb6151b90680912e`.

## Method and counting rule

I derived immediate data/control dependencies only. An edge `Rx -> Ry` means
that the text says `Ry` consumes something `Rx` produces, or explicitly says
that `Rx` feeds/controls `Ry`. I did not add edges merely because one quantity
could be useful to another, nor transitive edges. “Produces [none]” means “the
specified sources do not name another R-node as its consumer,” not that the
pattern has no output.

`control-map-edges.edn:14-35` contains 22 records: 21 have status `:drawn` and
one has status `:unresolved`. I classify all 22 records below. Thus A+B=22,
while the number of resolved drawn edges is 21. The provisional unresolved
record is included in B and called out explicitly.

R6 has two readings. **R6-C** is the catalogue's constructed candidate action
space (`sec-catalog.tex:237`). **R6-H** is the older contract's softmax selector
with abstention (`sec-catalog.tex:61-66`; glossary “Softmax and controller
calibration,” `sec-glossary.tex:31`). Edges marked R6-H do not follow from the
catalogue meaning.

## Per-node consumes / produces derivation

| Node | Consumes from | Produces to | Textual basis |
|---|---|---|---|
| R1 | R3, R16 | R3, R4, R8, R14 | R1 is updated from observations and outcomes (`sec-catalog.tex:188-192`); R3 moves the belief (`:196`); R16's witness feeds the next belief (`:309`). R4 consumes state (`:202`), R8 consumes belief (`:200`), and R14 consumes belief uncertainty (`:239`). |
| R2 | R16 | R3, R7, R8 | R2 normalises session/world evidence (`sec-catalog.tex:194`); R3 consumes what was observed (`:196`), R7 is keyed to observation features (`:198`), and R8 consumes observation (`:200`). R16 changes the world for next-tick re-observation (`:309`). |
| R3 | R1, R2, R7 | R1 | The update moves the current belief toward observation as far as trust permits (`sec-catalog.tex:196`; glossary “Prediction error,” `sec-glossary.tex:15`). |
| R4 | R1, R6-C | R5, R12 | Its kernel consumes state and action and produces predicted next state with uncertainty (`sec-catalog.tex:202`). R5 consumes predicted outcomes (`:235`); R12 compares predictions with later scores/outcomes (`:295`). |
| R5 | R4, R6-C, R7 | R14, R12; R6-H only | R5 scores predicted outcomes against preferences and ambiguity (`sec-catalog.tex:235`; glossary “Expected free energy G,” `sec-glossary.tex:21-25`). The candidate set makes downstream scoring meaningful (`sec-catalog.tex:237`), and precision applies wherever scoring occurs (`:198`). Scores are inputs to temperature-governed softmax (`sec-glossary.tex:31`) and later calibration (`sec-catalog.tex:295`). Under R6-H only, G feeds R6's selector. |
| R6-C | R17 | R4, R5, R11 | R6-C constructs the bounded candidate action set for downstream prediction/scoring (`sec-catalog.tex:237`); R11 arbitrates proposals (`:241`). R17 reorganises the model and its closing R16-to-R6 pattern trains proposal (`:338`, `:351`). |
| R6-H | R5 | R13 | Softmax consumes G scores and selects from allowable policies (`sec-glossary.tex:31`, “Control states U,” `:35`). The chronological text then constructs a cascade after the choice (`sec-catalog.tex:245-254`). Abstention itself has no R-node producer. |
| R7 | R2, R20 | R3, R5, R8 | R7 keeps per-channel trust keyed to observation features and applies it in prediction error, scoring and gating (`sec-catalog.tex:198`). R8 explicitly consumes precision (`:200`). R20 says its trip stream is precision evidence (`:366`). R12-to-R7 is only a plausible calibration loop, not stated; see B. |
| R8 | R1, R2, R7 | R14 | R8 is a pure function of belief, observation and precision (`sec-catalog.tex:200`). R14 takes diagnostic signals including recent regressions/instability (`:239`); interpreting the named present-fit alarm as such a diagnostic is direct but not the drawn R8-to-R5 direction. |
| R9 | independent evidence records | R12, R16 | R9 admits only evidence the maker did not manufacture (`sec-catalog.tex:177`). R12 applies exactly that rule to external Layer 2 (`:295`; glossary “No self-certification,” `sec-glossary.tex:72`), and R16 requires an external ungameable witness (`sec-catalog.tex:309`). The text does not identify an R-node that produces independence tags. |
| R10 | R16 | R2 / loop entry | R10 gates liveness on evidence that a tick changed observable state (`sec-catalog.tex:159`); R16 is the node that writes a witness and makes the next observation materially different (`:309`). The scheduled entrypoint initiates observation/deliberation/action (`:159`), represented here as R10-to-R2; it does not compute R8's mismatch. |
| R11 | R6-C | R13 | R11 arbitrates proposals within shared budgets (`sec-catalog.tex:241`). The selected target/feasible allocation then enters cascade construction, whose text begins “having chosen what to do” (`:245-254`). No text says R11 directly actuates. |
| R12 | R4, R5, R9, R16 | none stated | R12 consumes predictions, the model's later scores, and independently witnessed outcomes (`sec-catalog.tex:295`). R9 supplies the independence rule. The sources do not say that calibration updates R7 or another R-node. |
| R13 | R6-H or R11, R15 | R14, R16 | R13 scores whole cascades, using a slow/fast hierarchy and temporal discount (`sec-catalog.tex:254`); R15 supplies the strategic target and distinct discount (`:243`). A scored cascade goes to temperature-governed selection and witnessed enactment (`:239`, `:256`, `:309`). |
| R14 | R1, R5, R8, R20 (chartered) | R16 | R14 controls how decisively the best-scoring candidate wins and responds to belief uncertainty and diagnostic signals (`sec-catalog.tex:239`; glossary “Softmax,” `sec-glossary.tex:31`). R20-to-R14 is expressly chartered, not implemented (`sec-catalog.tex:370,373-376`). The resulting pick is what actuation receives. |
| R15 | R16 | R13 | Strategic selection fixes the tactical target and its temporal discount; witnessed tactical outcomes update the next strategic state (`sec-catalog.tex:243`). Hence R15-to-R13 for target/discount and R16-to-R15 for feedback—not R15-to-R16. |
| R16 | R9, R13, R14 | R1, R2, R10, R12, R15, R17 | R16 consumes the selected/constructed action and demands an external witness (`sec-catalog.tex:309`); the independent rollout variant consumes a cascade (`:256`). Its witness feeds belief and re-observation (`:309`), supplies R10's state-change evidence (`:159`), R12's external outcome (`:295`), R15's tactical feedback (`:243`), and the accumulated outcomes R17 re-reads (`:325-338`). |
| R17 | R16 (accumulated outcomes/counts) | R6-C | R17 runs BMR over accumulated counts and changes model structure (`sec-catalog.tex:338`; glossary “Bayesian Model Reduction,” `sec-glossary.tex:54-60`). The closing proposal pattern explicitly trains candidate cascades from R16-realised outcome and feeds R6 (`sec-catalog.tex:351`). R17-prime variants are internal refinements, not separate figure nodes. |
| R18 | not determinable | none stated | There is no R18 pattern paragraph in `sec-catalog.tex`; R18 is absent from the figure's node list (`control-map-edges.edn:11`). The permitted sources do not define an R18 computation. |
| R19 | not determinable | none stated | There is no R19 pattern paragraph. The catalogue note says prior preferences are labelled `C` rather than R19 (`sec-catalog.tex:41-50`). The glossary defines C as the preference distribution inside G (“Expected free energy G,” `sec-glossary.tex:21-23`), but does not license an R19 node or edge. |
| R20 | live phase trajectories/incidents | R7, R14 (chartered) | R20 monitors phase-boundary invariants and emits durable trips (`sec-catalog.tex:365-366`). The same paragraph says trips are precision evidence for R7; R20-to-R14 is explicitly chartered but not implemented (`:370,373-376`). |

## A. Drawn and derived — 14 records (13 catalogue-current, one R6-H-only)

1. **R1 -> R4** (`predict`): R4's kernel consumes state (`sec-catalog.tex:202`).
2. **R2 -> R3** (`observe`): the belief update moves toward observation (`:196`).
3. **R3 -> R1**: R3 updates belief (`:196`).
4. **R4 -> R5**: predicted outcomes are R5's risk/ambiguity input (`:202,235`).
5. **R5 -> R6 [R6-H only]** (`rank`): softmax consumes `G` scores (`sec-glossary.tex:31`). Under R6-C the derived direction is R6 -> R5.
6. **R13 -> R14**: cascade scores enter temperature-governed choice (`sec-catalog.tex:239,254`; `sec-glossary.tex:31`).
7. **R14 -> R16**: the temperature-governed winning choice proceeds to actuation (`sec-catalog.tex:239,309`).
8. **R16 -> R2** (`re-observe`): external act changes the world observed next tick (`sec-catalog.tex:309`).
9. **R6 -> R11 [R6-C]**: candidate proposals enter budget arbitration (`sec-catalog.tex:237,241`).
10. **R7 -> R3**: precision scales belief update (`sec-catalog.tex:196,198`).
11. **R7 -> R8**: present mismatch consumes precision (`sec-catalog.tex:200`).
12. **R9 -> R16**: grounded actuation requires the independent-witness discipline (`sec-catalog.tex:177,309`).
13. **R15 -> R13**: strategic target and distinct temporal discount parameterise tactical rollout (`sec-catalog.tex:243,254`).
14. **R20 -> R7**: trips are precision evidence about internal channels (`sec-catalog.tex:366`).

## B. Drawn/lifted but not derivable — 8 records

1. **R5 -> R5** (unresolved support record): no R5 self-dependency is stated. The source itself says its start matched no node (`control-map-edges.edn:24`). Derivation would require R5 to feed a previous/future R5 estimate into its own computation.
2. **R6 -> R13**: under R6-H a selected action can lead to construction, but the drawn node is declared to use catalogue numbering, where R6-C merely supplies pattern candidates. Under R6-C, the text would need to say that this candidate set directly supplies R13's cascade rather than first entering scoring/arbitration.
3. **R11 -> R16** (`arbitrate`): R11 arbitrates budgets, while the text constructs/scores a cascade before enactment. A direct edge needs a statement that arbitration's output itself is an actuation command. The derived immediate route is R11 -> R13 -> R14 -> R16.
4. **R7 -> R14**: R14 consumes belief uncertainty and diagnostic pressure, not the evidence-precision registry itself (`sec-catalog.tex:239`). Derivation needs an explicit mapping from per-channel precision to commitment temperature; the glossary distinguishes precision Pi from selection gain g (`sec-glossary.tex:17`).
5. **R8 -> R5**: R8 expressly says the present-fit score is not the future action score (`sec-catalog.tex:200`). Derivation needs R5's formula to consume F/mismatch as an input; current R5 consumes predicted outcomes, preferences and ambiguity (`:235`).
6. **R10 -> R8**: R10 checks whether observable state changed; R8 computes belief/observation/precision mismatch. Derivation needs the scheduler or its liveness result to be an input to F. The text instead gives R16 -> R10 and R10 -> loop observation (`sec-catalog.tex:159,309`).
7. **R12 -> R7**: calibration compares predicted and realised values, but no sentence says its result updates the precision registry. That feedback rule would have to be stated, including which Layer can change which channel's precision (`sec-catalog.tex:295`).
8. **R15 -> R16**: the explicit feedback direction is reversed: witnessed tactical outcomes update strategic calibration (`sec-catalog.tex:243`). A forward direct edge needs R15 to issue an enactment command; as written it fixes R13's tactical target.

## C. Derived but not drawn — 22 edges

1. **R1 -> R3** — the update changes an existing belief (`sec-catalog.tex:188-196`).
2. **R1 -> R8** — R8 consumes belief (`sec-catalog.tex:200`).
3. **R2 -> R7** — precision entries are keyed to observation features (`sec-catalog.tex:198`).
4. **R2 -> R8** — R8 consumes observation (`sec-catalog.tex:200`).
5. **R6-C -> R4** — predictions require each candidate action (`sec-catalog.tex:202,237`).
6. **R6-C -> R5** — the constructed candidate set makes downstream scoring meaningful (`sec-catalog.tex:237`). This reverses the drawn R5 -> R6 under the catalogue reading.
7. **R7 -> R5** — precision applies wherever scoring occurs (`sec-catalog.tex:198`).
8. **R5 -> R14** — softmax/temperature consumes candidate scores (`sec-catalog.tex:239`; `sec-glossary.tex:31`).
9. **R1 -> R14** — commitment temperature consumes belief uncertainty (`sec-catalog.tex:239`).
10. **R8 -> R14** — present-fit jumps are diagnostic/instability signals, which R14 says govern commitment (`sec-catalog.tex:200,239`).
11. **R11 -> R13** — after arbitration/selection, the system constructs the cascade it will score (`sec-catalog.tex:241,245-254`).
12. **R13 -> R16** — the independently checked execution witness consumes the selected cascade (`sec-catalog.tex:254-256,309`).
13. **R4 -> R12** — calibration consumes model predictions (`sec-catalog.tex:202,295`).
14. **R5 -> R12** — Layer 1 compares predicted G with later model scores (`sec-catalog.tex:295`; glossary “No self-certification,” `sec-glossary.tex:72`).
15. **R9 -> R12** — only independent Layer 2 can certify value (`sec-catalog.tex:177,295`).
16. **R16 -> R1** — the external witness feeds the next belief (`sec-catalog.tex:309`).
17. **R16 -> R10** — liveness consumes evidence that a tick changed observable state (`sec-catalog.tex:159,309`).
18. **R16 -> R12** — Layer 2 consumes independently witnessed outcomes (`sec-catalog.tex:295,309`).
19. **R16 -> R15** — witnessed tactical outcomes update strategic calibration (`sec-catalog.tex:243`).
20. **R16 -> R17** — adjudicated outcomes accumulate for offline structural learning (`sec-catalog.tex:325-338,351`).
21. **R17 -> R6-C** — learned structure/proposal rewards change future candidate cascades (`sec-catalog.tex:338,351`).
22. **R10 -> R2** — the scheduled entrypoint initiates observation, deliberation and action (`sec-catalog.tex:159`); no scheduler output is an input to R8's formula.

One additional edge is explicitly specified but not yet implemented: **R20 ->
R14**, where a trip lowers commitment (`sec-catalog.tex:370,373-376`). I keep it
out of the 22 present-tense derived edges because the catalogue labels it
“chartered”; it should appear as a dashed planned edge if the figure represents
the theory's declared future wiring as well as current computation.

## What the figure gets right and wrong

The figure gets the central perception/belief/prediction spine right:
R2 -> R3 -> R1 -> R4 -> R5, plus precision into update/mismatch and grounded
actuation back to observation. It also correctly shows the no-self-certification
constraint at actuation and the strategic-to-tactical direction R15 -> R13.

Its largest error is that it mixes the two R6 meanings. R5 -> R6 is correct for
the historical softmax selector, while R6 -> R11 is correct for the catalogue's
candidate-set producer. A single box cannot support both directions without
declaring that it performs both computations. The current text instead warns
that the meanings differ (`sec-catalog.tex:61-66`).

The figure also skips the inputs that make several formulas typed. R8 has arrows
from precision but not from belief or observation; R5 has prediction but not the
candidate set or precision; calibration has no arrows from predictions or
grounded outcomes. Conversely, R8 -> R5 contradicts the catalogue's explicit
present-fit/future-score distinction, and R15 -> R16 reverses the stated
tactical-outcome feedback.

Finally, the learning loop is visually absent. The catalogue says grounded,
adjudicated outcomes accumulate into R17 and that learned proposal returns to
R6. Without R16 -> R17 -> R6, the figure depicts a controller that acts but does
not structurally learn. R18 supplies no repair because the permitted theory does
not define it; R19 has deliberately been replaced by the unnumbered preference
distribution C.
