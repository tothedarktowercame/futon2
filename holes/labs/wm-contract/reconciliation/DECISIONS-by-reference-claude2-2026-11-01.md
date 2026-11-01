# Open questions answered by reference (claude-2, 2026-11-01 ~14:50Z)

Joe, 2026-11-01: "answer questions with reference to theory and existing simpler reference
implementations where possible." References used: Parr, Pezzulo & Friston, *Active Inference* (MIT
Press 2022; `~/downloads/book_9780262369978.pdf`); pymdp (`~/code/refs-external/pymdp/pymdp`,
`control.py` `update_posterior_policies` and `sample_action`/`get_marginals`, `learning.py`); the
Lean modules under `mathlib4/DarkTower/WarMachine/`. Each item ends with the decision taken and
what, if anything, is left for Joe.

## 1. The habit prior E is record-only after the inner-law change

Theory. The discrete-time scheme scores a policy by expected free energy G and forms the policy
posterior q(π) = σ(ln E − γG); E is the prior over policies ("habits", learned from the
empirical distribution of past selections). pymdp: `update_posterior_policies(…, E, gamma)`
returns `q_pi` and `neg_efe`; `get_marginals`/`sample_action` then choose by the marginal over
first actions. So in the reference scheme E does bear on selection.

Our ruling. Joe ruled "minimize G over cascades" and Q11 compares G. With E flat and F flat
(all 25 scored era-2 runs), argmax q(π) = argmin G, so the two agree on everything observed.
They part only when E is learned. pymdp's `learning.py` learns A and B Dirichlet parameters;
it has no E-learning function in this version, so the reference implementation we hold is also
"E as given, not learned".

Decision. Keep Q11 on G. When habit learning produces a non-flat E, admit it as a *term of the
compared objective* carried per policy with its provenance: compare `G − (1/γ) ln E` (the
negative log of the unnormalised policy posterior), with `ln E` and `γ` recorded beside risk,
ambiguity and novelty and projected at the declared scale. That keeps "the chosen is the strict
minimiser of what was compared" and makes the habit's influence a recorded number rather than a
hidden re-ranking. This is a Lean carrier change when E is first learned (Q11's `policyG` becomes
the objective; a `policyHabit` carrier holds ln E). Not now: one known gap at a time. Nothing for
Joe unless he wants E to bear on choice before it is learned, which the theory does not ask for.

## 2. Does an embedding-derived relation count as measured?

Theory. In the generative model an observation is whatever the likelihood maps to hidden
states; its reliability lives in the likelihood's precision, not in whether it is admitted.
A relation derived from pinned embeddings and a hand-reviewed row through a declared function is
reproducible from pinned inputs, so it is an observation with recorded provenance; its weak
cosine (−0.014, 0.073 on the two 11-01 picks) is low precision, which belongs in the ambiguity
term of the policies that depend on it.

Decision. Observed, with `:derived-via` disclosed (done in L5-4d-4/5 and the witness). Two
follow-ups, both implementation: (a) the focus inputs declare a cosine floor (the code already
provides for "a floor applies only when the inputs declare"); below it the relation is unknown,
and the floor is an empirical parameter recorded in the kernel carrier; (b) the relation's
reliability enters the C/A model as reduced precision for derived relations, later. Nothing for
Joe except the floor's value, which is empirical.

## 3. Relations for excursions and tickets (445 of 626 unknown)

Theory. Same observation; the producer's sources are the model's likelihood for "relation to
focus". `classify-target` already has stated-relation hops through task text, the ticket parent,
and the embedding neighbour; `m-derivation` is gated to M- targets by prefix only.

Decision. Extend the derivation to E- targets (their texts state relations and parents like
missions do) and route T- through the ticket parent then the parent's derivation; record
`:derived-via` chains. Implementation packet L5-4g. Nothing for Joe.

## 4. Q5 (selection before interpretation) on a selection run

Theory. Policy selection is on current beliefs; observing a task (reading its structure,
classifying its relation) before choosing is observation, not interpretation. "Interpretation" in
the War Machine is the authoring of occurrence squares for the selected target. Q5 for the
selection kind therefore reads: no occurrence-square authoring for any task before the selection
decision. The runtime's phase order supplies it.

Decision. Project `interpretationOrder` for the selection run from the phase log (selection phase
closed before any interpretation phase opened), typed absent when the phase log is not retained.
Implementation, L5-4h. Nothing for Joe.

## 5. Q9 (factorized preference) on a selection run

Theory. The Lean law (`FactorizedPreferenceSemantics.lean`): completion is the primary dimension;
relevance is conditional on truthful completion and cannot trade against it; unmeasured is not a
failure class. The outer preference is a product C(ending, gain) = P_ending × P_gain with
ending ∈ {focused, related, unrelated, stop-the-line, not-yet-evaluated} and gain ∈ {none, one,
several}. Mapping: `ending` is the relevance dimension (focused/related/unrelated) with the
stop-the-line class standing for not completed; `gain` is a second conditional dimension. The
law's primary dimension is completion; the outer kit's primary factor is relevance. So the outer
C is factorized, but not with the law's order: on the outer path "completion" of a selection is
whether the selected work ran to a terminal receipt (the ending class), and relevance is
conditional on that only if the kit puts the stop class first. The current kit (55/35/5/5/0) does
not separate completion from relevance ordinally.

Decision. Q9 for the selection run is a genuine design question: define the outer factorized
certificate as completion (ran vs stopped: the stop-the-line mass) then relevance (focused >
related > unrelated) then gain, with calibration and factorization authorizations as the inner
run has; the current kit needs restating in that order. I will write the Lean statement
(L5-4L-4) from the existing module and the kit from the kernel carrier, then put the restated kit
to Joe as a parameter, since the order is the law and the numbers are his. Partly for Joe: the
numbers only.

## 6. The live makeup migration

Fact. Since L5-4d-2 the selector constructs the menu from the library and the makeup's
`:cascades` key is dead data. The pure migration removes only that key.

Decision. Apply the migration to the live file myself as a committed change if the file is
tracked, else as a logged data edit, after L5-4f lands (so the pending-replay path is settled).
Nothing for Joe beyond notice.

## 7. The executable-steps mapping file

Fact. It is an interpretation of Joe's README prose into gates and orders, reviewed by me
line by line (report of 14:20Z). It is the one artefact that remains Joe's to look at when he
likes; it blocks nothing.

## 8. Direct :task entry and meta-self-heal bypass outer selection (codex-11, L5-4f)

Theory. One loop, two runs per click: every click has a selection run, even when the selection
is "the operator named the task" (a selection with a declared source, not an inference). A
self-heal is the machine selecting repair work; it too is a selection, by the repair-goes-to-
the-front cascade of the README.

Decision. For now each bypass writes a typed `not-exercised` selection record (L5-4f). Whether
those paths become ordinary selections (operator-named task as a declared-pick cascade; self-heal
as the repair cascade) is the shape I will propose in L5-5 with reference to the README's row 3.
Nothing for Joe yet.
