# Recipe reconstruction: the WM's design choices against Parr/Pezzulo/Friston ch. 6

Date: 2026-09-13. Author: claude-15. Source: Parr, Pezzulo & Friston,
*Active Inference* (MIT Press 2022), ISBN 9780262369978, chapter 6 "A
Recipe for Designing Active Inference Models" (pp. 105-121), read from
Joe's PDF copy (~/downloads/book_9780262369978.pdf; Joe also holds the
paper copy). The registry already cites this book (:ref :parr2022 on
:policy-posterior); this note is the first read of the actual chapter
against our recorded choices. Nothing here edits aif-equations.edn --
candidate registry entries are PROPOSED at the end for Joe's ruling per
the TN 9a gate.

The chapter's recipe is four questions (6.2). Below, each question, the
War Machine's answer as actually recorded, and its status.

## Q1. Which system are we modeling? (6.3: the Markov blanket)

The book requires identifying internal states, sensory states, active
states, and the external generative process, "so we know what is being
inferred and what is doing the inferring."

RECONSTRUCTED (no single recorded statement exists):
- Internal states: the tick model's belief state mu (R1), precision Pi
  (R7), policy posterior Q(pi) (R6), temperature tau (R14) -- the
  quantities the trace records carry per tick.
- Sensory states: the fourteen-channel observation o (R2),
  obs/observe over scan-data (observation.clj:103-146).
- Active states: the enacted action -- the strategic selector's pick
  replacing :action on the live path (war_machine.clj:5152-5155).
- External states / generative process: the futon ecosystem itself
  (repos, agents, run stores), reaching the machine only through the
  scan pipeline.

STATUS: IMPLICIT. The registry records the pieces but no partition.
Two of the campaign's hardest disputes were Markov-blanket boundary
questions conducted without that vocabulary: (a) whether R17's feeder
reading capability entities over Drawbridge is inside the blanket
(C456 measured: no production :capability/* writes flow -- dead
sensory tissue); (b) the R6 dual reading (candidate space vs Q(pi)
node, recorded on :r6-r4-pi-realised-undrawn). The book's Q1 would
have named both as boundary placements.

## Q2. What form for the generative model? (6.4: three sub-choices)

### 2a. Discrete or continuous variables (or both)? (6.4.1)
OBSERVED (code, no :choices entry): HYBRID, in exactly the book's
standard arrangement -- "high-level decision processes ... discrete
variables, whereas more fine-grained perception ... continuous": our
policy selection ranges over discrete candidate actions (softmax over
scores, selection deterministic argmax per :selection-rule,
:resolved-by-source) while beliefs and observations are continuous
channel values updated by prediction errors (predictive-coding side).
Discrete time throughout (the tick). The alphabet split the
:outcome-domain ruling resolved (Joe 2026-09-07: which Obs the C
ranges over, 7 M-INC events vs 13 channel ranges, disjoint alphabets)
is a hybrid-model seam the book's 2a predicts.

### 2b. Shallow or hierarchical? (6.4.2)
DECIDED -- :choices :hierarchy (Joe, 2026-09-09, RUN4-scoped):
single-level accepted for RUN4's inner loop; outer-loop use of the
inner loop "does not establish a coupled generative hierarchy"; NOT a
permanent design, hierarchical model "comes in due course." The
book's criterion (hierarchy needed when timescale separation exists
among STATES) supports the deferral: our second timescale is in
parameters (learning, Q3), which the book handles separately, not by
adding levels.

### 2c. Temporal depth for planning? (6.4.3)
DECIDED -- :choices :policy-depth (Joe, 2026-09-09): temporally deep,
fixed depth, "explicit three and three" (anticipation depth 3,
cascade rollout depth 3) in the run config. Realised conditionally:
predict-multi-horizon under horizon >= 2 (efe.clj:607-609), live only
when the anticipation snapshot has events (war_machine.clj:4485-4487)
-- the registry's only conditional edge, :r13-r4-depth. EFE over
candidate futures (G at R8/R16) is the book's stated reason a
temporally deep model is required; we have it.

## Q3. How to set up the generative model? (6.5)

### Variables and priors (6.5.1; POMDP elements A,B,C,D,E)
- A (likelihood): belief/predict-observation (belief.clj:1199-1238),
  hand-specified; eight of fourteen channels carry a likelihood model
  (:prediction-error row). FIXED.
- B (transitions): the forward model fm/predict (R4). FIXED.
- C (preferences): DECIDED by :outcome-domain (Joe 2026-09-07) -- the
  declaration of which Obs the machine's C ranges over was "the
  entire decision"; before it the risk term's C read alphabets no Q
  ranged over.
- D (initial state prior): wm-belief-pre bootstrapped from stack
  annotations, reconciled against the previous tick's mu-post under
  carry (war_machine.clj:4289-4294). OBSERVED, no ruling.
- E (habit prior over policies): DECIDED -- :choices :habit-prior
  (Joe 2026-09-09, Item 19c): E ON for RUN4, applied on the live
  strategic path; SUPERSEDED IN PART by Items 21a/22 (promotion of
  persisted E into the final selector REFUSED). The registry's
  :policy-posterior imports [:E :G :tau :pi :F-pi] -- the book's full
  complement.

### Fixed vs learned (6.5.2)
The book: learning is "a design choice rather than something
mandatory," cast as SLOW updates of parameter beliefs, updated "after
states have been inferred," against fast state inference.

THIS IS THE CAMPAIGN'S LIVE FRONTIER, and the chapter names it
exactly. Recorded state: :choices :learning is OBSERVED-NOT-DECIDED
-- :observed :offline-bmr-only, with Joe EXPLICITLY WITHHOLDING the
ruling (2026-09-09) pending exactly the clarification the book
provides scaffolding for; the noted missing evidence is "SUBSEQUENT
CONSUMPTION, not an online/offline distinction." The implementation
half is the R17 Dirichlet accumulation (Da Costa eq. 21): declared,
carrier-only, :realised false; the recount-shaped a4a implementation
was refuted as a carrier by the F8 slice's four propositions; U91/U92
(opened under Joe's 2026-09-12 :r17-target-semantics ruling) are the
build. In the book's terms: our fast loop (states: mu, Pi within the
tick) runs; our slow loop (parameters: concentrations across ticks)
is declared and unbuilt. Two campaign artifacts -- the withheld
:learning ruling and the U91/U92 repair pair -- are one checklist
line in the recipe.

## Q4. How to set up the generative process? (6.6)

The book's axis: EXPLICIT/environmental models (internal states
resemble the environment) vs ACTION-ORIENTED/parsimonious models
(encode the contingencies useful for control, no reconstruction).

OBSERVED (no recorded choice): the WM is firmly ACTION-ORIENTED. The
generative process is the real futon ecosystem -- never simulated,
never mimicked; the model's fourteen channels are control-relevant
summaries (sorry counts, coupling density, loop health), not a
reconstruction. The book's A-matrix semantics distinction (measurement
distribution of the process vs subjective likelihood of the model) is
enforced in our records: realized-outcome captures carry
:observation/basis and "world-context-not-the-basis" separations
(wm_step_observe.bb), and the model's likelihood lives separately in
belief.clj.

## Findings

1. CONFIRMATION, strongest: the recipe independently confirms the
   campaign's own diagnosis that the learning timescale is the
   missing half (Q3/6.5.2 <-> withheld :learning + U91/U92). Nothing
   in the chapter suggests the fast loop is misdesigned.
2. CONFIRMATION: :hierarchy (defer depth), :policy-depth (deep,
   fixed, discrete policies), and the hybrid discrete/continuous
   arrangement each match the book's stated default pattern for
   exactly our situation.
3. GAP: no recorded Markov-blanket partition (Q1). Two past disputes
   were boundary questions in disguise. PROPOSED: a :choices entry
   (:markov-blanket) recording the four-way partition above, for
   Joe's ruling.
4. GAP: the explicit-vs-action-oriented choice (Q4) is real, made,
   and unrecorded. PROPOSED: one :choices entry (:model-orientation,
   :observed :action-oriented) so the choice is citable.
5. VOCABULARY: the book is the registry's :parr2022; future
   TN/registry notes can now cite chapter/page against the PDF Joe
   holds (and the paper copy).
