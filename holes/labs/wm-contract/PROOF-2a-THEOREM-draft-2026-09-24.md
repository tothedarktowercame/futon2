# PROOF-2a — the PROOF-2 theorem with the 2026-09-24 architectural adaptations

Date: 2026-09-24. Author: claude-10, at Joe's direction ("make a PROOF-2a that
includes our adaptations"). Status: DRAFT.

**Base.** `PROOF-2-THEOREM-draft-2026-09-24.md` at futon2 `cafe94b3`. Every
clause, key path, falsifier and contract entry there carries over unchanged
unless this file restates it. Where this file restates a clause, this file
governs PROOF-2a. The rationale for each change is in
`PROOF-2-ARCH-draft-2026-09-24.md` (register rows AR-32 to AR-36); the
findings behind them are in `holes/E-outer-loop.md`, `holes/E-cascade-real.md`
and `holes/E-flight-aif.md`.

## Scope statement (AR-36)

Clauses T and 0–6 certify the **rewrite reading** of a design pattern: an
`InterpretedPattern` with consumes, forbids and produces over a finite token
state. A passing PROOF-2a does not certify a pattern's forces (its HOWEVER)
or its conditioning of other patterns. Those readings have no clause yet
(AR-35 is the first step toward the conditioning one).

## Theorem

"Not fake" means not a fake War Machine: a real one, which does the work.
This file is a theorem about that machine. A typed absence, an open clause or
a stop is a HOLE in the proof, not an outcome it permits: the theorem is not
proved while any remains, and each is listed under "Holes" below with the
work on the machine that closes it.

**THEOREM (PROOF-2a).** Let `M` be a mission, excursion or task whose
completion criteria are stated in its text and are reached by the field of
cascades warranted by the pattern library. Then a flight of the War Machine
on `M` completes `M`: every completion criterion is met, each with evidence
that a check confirms (Clause C and clauses 1–6 at mission grain), and the
completion is the machine's own, which is what the per-click conditions below
establish.

**Hypothesis witness.** A mission that an agent has completed using only
library-warranted steps, each with a receipt, satisfies the hypothesis. For
such a mission a flight that does not complete it is a counterexample.
M-futon-seams is the first such mission (below, "What the worked example
teaches").

**Per-click conditions** (what rules out a fake: fixtures, hand-built inputs,
values that were never consumed). The implementation is not fake with respect to the
named War Machine Lean model iff, for every click required by clauses T and
0–6, the provenance proposition `P_k` identifies the values actually
consumed, the concrete Lean proposition `W_k` checks on those values, and the
independently constructed bad extract `X_k` makes the same proposition fail.
All eight clauses hold on the single linked sequence `L` of PROOF-2
(ordinary live clicks under A21); fixtures and hand-built inputs establish
none of them.

### A flight: the clicks on one mission, ending in closure

A flight takes a mission (or other task) to closure. It may be one click or
several; that is an engineering choice. What is not a choice:

1. **The sequence closes the mission.** The flight's wants are the mission's
   completion criteria (for a mission document, its phase exits), read from
   the mission text. The flight ends when every criterion is met with checked
   evidence. A sequence that ends short is a counterexample to the theorem.
2. **Every click makes concrete progress.** Clicks carry a heavy overhead
   (selection alone has run to minutes). A click that meets no criterion and
   produces no token a later click in the flight consumes is overhead with no
   progress, and counts against the machine as a stop would.
3. **The machine does not refuse its own work.** If a mission is well
   specified and the library has patterns that let the work proceed, a
   missing input the machine could compute (an interpretation, a candidate,
   an observation, the mission's outcomes) is computed within the flight, not
   returned as a refusal. A refusal is admissible only where no warranted
   pattern applies, and then it is a hole (below).
4. **The mission stays the target across the flight.** Later clicks continue
   the chosen mission; they do not re-select among all targets.

Clause 4's attempts count across the flight. This is the grain at which
`Q(o|π)` is read in `futon4/holes/mission-lifecycle-wm-alignment.md` §3c ("at
mission grain … the catalog IS the playout record").

Worked example against this grain: click-001 (M-futon-seams) targeted one
instance's three wants; the rest of the mission was carried by claude-1, with
each step warranted by a library pattern and a receipt. That record is what a
flight on this mission consists of; the steps claude-1 supplied that the
machine does not yet do are listed as the gaps below.

### Clause T — the machine chooses its target (new; AR-32)

**Wₜ.** On each click the extracted target field `T` lists every target the
tick considered and partitions it into a feasible support `T_f` and
exclusions, each exclusion with a typed reason. The chosen target maximises
`PolicySelection.selectionPosterior` over `T_f` at target grain, with
feasibility acting as policy support (membership in `T_f`) and not as a term
of G. If `T_f` is empty, each excluded target's record states what would make
it feasible; for a construction failure this is the constructor's typed
finding (the unproduced want, or the missing interpretation).

**Pₜ.** To be built at `[:decision :target-field]`: `:considered`,
`:feasible`, `:exclusions` (target, reason, what-would-make-feasible), the
per-target score inputs, and `:chosen`. `:chosen` must equal the target of
`[:decision :selection-law]`. The 09-23 and 09-24 records have no target
field; the target list assembled at `scripts/futon2/report/war_machine.clj`
(substrate targets, declared targets, proposal supply, ticket queue) is the
input the field must record.

**Xₜ.** (a) Remove one feasible target from the extract while keeping the
choice; Wₜ must fail. (b) An abstaining record with a non-empty `T_f`; Wₜ must
fail. (c) An abstaining record whose exclusions omit what-would-make-feasible;
the empty-support branch of Wₜ must fail.

Wₜ is a witness condition, not a gate: a click that fails it still runs; it
is not credited.

**Missing definition.** Target-grain scoring needs a G per target. The
natural candidate is the best candidate cascade's G for that target, which
makes Clause T depend on clause 0 being satisfiable for every feasible
target. This is stated here, not settled.

**Declared cost ordering (from the worked example).** M-futon-seams chooses
its target ("Pick one instance and declare its interface — not all eight")
by a declared cost ordering (declare first: free; retrofit in code:
mechanical; retrofit in prompt text: worst), which is neither a feasibility
exclusion nor a G. `Pₜ` therefore also carries `:cost-ordering` (the declared
ranking, its source span, and how it entered the choice: as a prior over
targets, a tie-break, or not at all). Wₜ adds: if a cost ordering is
declared, the record states how it entered, and the chosen target is
consistent with that statement.

### Clause 0 — an ordinary, semantically nontrivial, machine-constructed field exists (restated; AR-33, AR-34)

**W₀.** As in PROOF-2, plus:
- (construction) every candidate's `:construction-receipt` has `:kind
  :machine-constructed` and names the interpretations it used: pattern id,
  guard, produces, and receipt (author, source path, source sha). Replaying
  the constructor on exactly those interpretations and the recorded initial
  state reproduces the candidate. Agent authorship of an interpretation is
  allowed and recorded.
- (carrier) the candidate's pattern structure is extracted as a containment
  order `r` on its patterns (a unit sits above another exactly when it
  contains it; overlap and "sits above" are the same order) with
  `CascadeOrder.acyclicDescent r`, and the semilattice condition restricted
  to OVERLAPPING pairs: whenever two patterns share a descendant, their
  common part is itself a pattern of the cascade (a greatest common
  descendant). Disjoint pairs need no meet. `CascadeOrder.hasMeets`, which
  demands a meet for every pair, is too strong and is not required. A
  precedence list is the chain case of `r`.
- (missing meet) an overlapping pair without a meet is recorded as a typed
  finding naming the pair and the maximal units of their common part. It is
  a finding, not a failure of W₀.

**P₀.** As in PROOF-2, plus `[:decision :selection-certificate
:candidate-derivations <id>]` carrying the interpretations used (with their
receipts), the constructor's version identity, and the descent/co-application
edges.

**X₀.** (a), (b) as in PROOF-2, plus:
- (c) a candidate whose structure cannot be reproduced by replaying the
  constructor on its recorded interpretations; W₀ must fail.
- (d) a target refused because one want has no producer, where admission
  would accept a candidate that newly satisfies another want; W₀'s
  construction condition must fail (E-cascade-real D15).
- (e) recompute the kernel and score under the list kernel for EVERY linear
  extension of `r` (capped, cap recorded), at the click's recorded horizon,
  and record the spread. The finding is stated relative to both: if the
  horizon exceeds the cascade's depth, equal results say nothing about the
  structure; and a single flattening is not a comparison, since on the
  worked example the list kernel's p(all wants) ranged 0.168–0.797 across 48
  extensions of one cascade (instance 5, horizon 8, θ 0.8). Where the
  co-application result lies outside the spread, the record says so. This is
  a finding, not a failure.
- (f) a cascade whose frontier conflicts (below) must carry the conflict
  flag; a record that omits it where the extract shows a conflicting
  frontier fails W₀.

**The kernel: co-application of the enabled frontier.** At state `s`, a
pattern is enabled when `CascadeTransition.guard` holds. The *enabled
frontier* is the set of enabled patterns with no enabled pattern above them
in `r`. Every frontier pattern fires independently, succeeding with its own
θ; the next state adds the produced tokens of those that succeed:

  K(s, s′) = Σ_{S ⊆ F(s)} ∏_{p∈S} θ_p ∏_{p∈F(s)∖S} (1−θ_p) · [s′ = s ∪ ⋃_{p∈S} produces_p]

and K(s, ·) = δ_s when the frontier is empty. On a chain the frontier is
`firstEnabled`, so K is `cascadeKernel` there. Guards are evaluated before
the step. A *conflicting frontier* is one containing p, q with
`produces_p ∩ forbids_q ≠ ∅`; the kernel still co-applies, and the
certificate carries `:frontier-conflict` naming the pairs and the tokens.

Why this and not one-at-a-time (interleaving, or a list over an arbitrary
linear extension), on the worked example: (1) on instances 4 and 7 at
horizon 6, co-application reaches all wants with p 0.26 and 0.85 where list
and interleaving give 0; the difference is rate, and it closes by horizon
14. (2) On instance 6 two incomparable patterns conflict (realtime/mode-gate
keeps the original prompt text authoritative; wr-8 makes typed files
canonical with prose generated). List and interleaving never reach all wants
at any horizon, because whichever fires first disables the other;
co-application does, through the state holding both tokens, which is the
state the mission records the system as being in (both prompt versions live
behind a mode flag). (3) In the same instance the one overlapping pair
without a meet has exactly those two patterns as the maximal units of its
common part: the missing meet and the conflict are the same fact. Evidence:
`futon3c/holes/labs/M-futon-seams/` (annotations at futon3c `b0ffc7d1`; cascades `instance-{4,5,6,7}.edn`; kernels `proto/kernels.clj`).

**Remaining missing definitions.** The Lean module: K as above, its
nonnegativity and row sum 1, `K = cascadeKernel` on chains, the frontier
conflict predicate, and the restricted meet condition over a containment
order. Until that module exists, W₀'s kernel equalities are checkable only
when `r` is a chain, and a passing clause 0 is recorded as the chain case.
The forbids that create a conflict are interpretation claims, not pattern
text (on instance 6 they are claude-1's, stated in its receipts); W₀'s
record of each interpretation's author is what makes a conflict
attributable.

### Clause C — the enactment is the chosen candidate (new; from the worked example)

**W_c.** For the click's selected candidate, the enactment record lists every
attempt at a pattern step: the pattern, the token it claims to produce, the
commit, and the check that observes the token. Every pattern of the selected
candidate has a successful attempt; no attempt names a pattern outside it;
an attempt claims only a token its pattern produces; each successful attempt
names its check. Deviations from the candidate's order or scope are recorded
with a typed `:kind`.

**P_c.** To be built at `[:enactment]` beside the click record
(worked-example form: futon3c
`holes/labs/M-futon-seams/exemplar/click-001-enactment.edn`), joined to
`[:decision :selection-law]` by candidate id.

**X_c.** (a) The first attempt alone (a change at another grain than the one
chosen); W_c must fail. (b) Successful attempts with their checks removed;
W_c must fail. (c) An untyped deviation; W_c must fail. On the worked example
all three are caught (futon3c `401469fd`).

Why: clause 4 joins action → outcome → next belief, where "action" is the
selected candidate. Without W_c a click can pass every other clause while the
change actually made is something else, as click-001's first attempt did.

### Clauses 1–6

Unchanged from PROOF-2, with two reading rules. (1) Wherever they use
`cascadeKernel precedence`, read it as the kernel of the candidate's
structure `r`, which is the list kernel when `r` is a chain (AR-34). (2)
Clause 4's prediction and outcome are stated in one unit, the enactment's
attempts: the Q-link scores the observed number of attempts to completion
against the predicted distribution of that number, not a probability at a
chosen kernel horizon. On the worked example, scoring at a 6-step horizon
gave p(all wants) 0.26 against an outcome with every want met, which read as
a miscalibration; in attempts the prediction's mean is 8.75 (7 patterns at
θ 0.8) and the observed 8 has P(≤ 8) 0.50 (futon3c `356573c1`). Kernel
choice changes elapsed steps, not attempts.

## Not yet a clause (AR-35)

A prior over lower-level patterns conditioned by higher-level ones (the
priming reading). Needs a Lean definition first; open whether it is E, a
hierarchical prior on B, or new. Falsifier to carry forward: remove one
priming edge; the recorded prior must change.

## Work this opens, in order

1. **Lean: the co-application kernel and the restricted meet condition**, as
   specified under clause 0 (definitions settled on the worked example;
   proofs owed). Unblocks W₀'s general case.
   *Landed (claude-8, 2026-09-24): mathlib4 `69c2432f` on `darktower`,
   `DarkTower/WarMachine/Proof2/CoApplicationKernel.lean`. Defines
   `enabledFrontier`, `coApplyKernel`, `frontierConflict`, `Overlap`,
   `hasRestrictedMeets`, `MaximalCommon`; proves the kernel nonnegative with
   unit row sums, δ on an empty frontier, `patternKernel` on a singleton
   frontier, and equal to `cascadeKernel` under an explicit `chainCondition`
   (stated as a hypothesis, not derived from a list); `hasMeets` implies
   the restricted condition. Instance-6 fixture: frontier `{p, q}`,
   conflict flagged, co-application reaches `{a, b}` with `θ_p·θ_q` where
   the list kernel gives 0 in either order; the restricted condition holds
   where `hasMeets` fails. Missing-meet fixture needs four units, not three
   (with three, whatever sits below both is their meet); its maximal common
   units are the two conflicting parts, as clause 0 says. Rebuilt by claude-8;
   36 axiom audits standard, no sorry. Unreviewed under A20. Chain case
   closed by mathlib4 `81b5b774`, `Proof2/ChainOrder.lean`: `listChain n i j
   := j < i` (earlier in the list sits above; so "below" is later, and the
   meet of two indices is `max`), `chainCondition_of_list` proved from the
   list, hence `coApplyKernel = cascadeKernel` for every precedence list with
   no hypothesis; the list order is acyclic and has all meets. The only
   cascades the machine produces today are therefore covered by the kernel
   without assumption.*
2. **Record: `[:decision :target-field]`.** Unblocks Clause T.
3. **Record: interpretations and edges in `:candidate-derivations`.** With the
   constructor wired in (E-cascade-real D4), unblocks W₀'s construction
   condition.
4. **Constructor: partial-want construction** (D15), so X₀(d) has something
   to pass.
5. **Record: `:cost-ordering` in the target field**, and the linear-extension
   spread for X₀(e).

## Checkpoint 1 — M-futon-seams worked example (2026-09-24, not complete)

A dated record of what the by-hand exemplar has shown so far. It is a
checkpoint, not a result: clauses 1–6 have barely been exercised, and the
exemplar's own clause 0 fails. Evidence lives in futon3c
`holes/labs/M-futon-seams/` (page: https://zone.hyperreal.enterprises/wip/seams.html).

Settled by examples (already in the clauses above):
1. Kernel = co-application of the enabled frontier. One-at-a-time kernels are
   slower where patterns are independent (instance 7, horizon 6: 0.853 vs 0),
   and where they conflict (instance 6) never reach the wants; co-application
   passes through the both-versions-live state the mission records.
2. A precedence list silently picks a number: instance 5's p(all wants) ranges
   0.168–0.797 over its linear extensions; X₀(e) compares the spread and states
   the horizon.
3. Carrier = containment order, meets required only for overlapping pairs. Only
   instance 6 fails, and its missing meet's maximal common units are its two
   conflicting patterns.

Corrected along the way:
4. "Below" must be reflexive (CascadeOrder's `a = b ∨ Reach r a b`); a strict
   version produced four false missing meets (futon3c `4d748660`).
5. Cascade (argument, containment) and wiring (construction, token flow) are
   different objects; deriving one from the other's edges drew false pictures.
   Token flow exposes dangling outputs (instance 5: three tokens nobody wants).
6. Validators must accept what the loader accepts (`@arg` id lines; futon3c
   `3a486cdc`), or they manufacture unproduced wants.

Gaps in PROOF-2a exposed:
7. No clause checks that the enacted change IS the chosen candidate. click-001
   chose a role registry; the enactment (futon3c `8e5c431e`) built a
   provider-grain fix; every clause as written would pass it
   (`exemplar/click-001-outcome.edn`).
8. Clause T has no target value: the mission's stated cost ordering did not pick
   the target and is ambiguous in direction; the chosen target had the lowest
   predicted p(all wants) of four, so p(all wants) is not a target's worth.
9. Clause 0's construction condition is unmet: every candidate is hand-built;
   `interpretation_construction.clj` has not run on these interpretations.
   Conflicts come from interpretation forbids, not pattern text, so the
   interpretation author must be recorded.
10. Clauses 1–6: A observed (a re-runnable redirect test); D, F, Q-link
    recorded as typed absences; B trials not usable because the enactment
    deviated from the plan. [Superseded after the checkpoint: the candidate
    was enacted as chosen and clauses 1–6 computed on it
    (`exemplar/click-001-clauses.edn`); see Clause C and the clause 4 reading
    rule above.]
11. Phase order: an INSTANTIATE step ran before MAP/DERIVE/ARGUE met their
    exits (`holes/labs/M-futon-seams/lifecycle.edn`).

Open next: enact the chosen candidate as chosen (role registry) with an
enactment-conformance record; clauses 1–6 on that enactment (A with stated
precision per check kind, D as an exact update, F from the same update, the
Q-link, B trials per pattern, and a second click that reads the updated B);
run the constructor on the recorded interpretations; the Lean module for the
kernel and the restricted meet condition.

## Finding from the worked example: Clause T needs a mission-level C, computed at click time

Clause T ranks targets. On M-futon-seams the cost of a target (expected
attempts to complete its cascade: 8.75–10 across instances 4–7) barely
separates them, while the mission's own ranking (4, then 5, then 7) is given
for value reasons: which outcomes each instance delivers, and for whom. Those
are mission-level preferences: the outcomes the mission wants, and which
instance wants serve them. The instance cascades carry wants, not outcomes,
so Clause T's value term has no input on any mission until that exists.

Survey (2026-09-24): 1672 mission files across the futon repos; 904 have a
Goal/Outcome/Motivation/Why heading and 361 an exit/success/acceptance
heading, so outcomes are usually STATED in prose; of the 81 mission-triple
records in futon6, 0 record outcomes or wants. So the outcomes are there
to be read, and the War Machine should read them when it needs them: C is
computed at click time from the mission text, as a step of the click with
its own receipt (each outcome cued to the span it came from; each served-by
link to the instance want it names), not required as a record prepared in
advance. A mission is never refused for lacking a precomputed C. If that step finds
no stated outcome, that is a hole (below), not a value of the term.

Consequence for W_t: the certificate carries the extracted C and its cues, so
a reviewer can check the extraction against the text. Consequence for the
constructor: its :observation-required refusal is the same shape (a missing
input that could be computed), and should become a step that computes it.

The same holds for E: the corpora of past pattern USE (mission citations,
operator turns) disagree in sign on the worked example and neither records
which cascades were enacted. E's input is enacted-cascade records (Clause C),
which exist for one click.

Not run: an extractor tested against the owner's hand-written C for
M-futon-seams (claude-1, futon3c `e99342ac`), which would be the reference.
The worked example stops at recording the requirement.

## What the worked example teaches (M-futon-seams, 2026-09-24)

The worked example is complete at this point; the fixes it names are not made
here. Artefacts: futon3c `holes/labs/M-futon-seams/exemplar/` and `item6/`.
Each item below is what went wrong or what was missing, and the change to
PROOF-2a it implies.

1. **The enactment can differ from the choice.** The first attempt at
   click-001 built the provider grain while the click chose the role grain;
   every other clause would still have passed. → Clause C (added above).
2. **Prediction and outcome were in different units.** p(all wants) at a
   6-step kernel horizon (0.26) against an enactment of 8 attempts read as a
   miscalibration; in attempts the prediction fits (mean 8.75, P(≤8) 0.50).
   → clause 4 reading rule (added above): state both in attempts.
3. **Declared inputs decided the answers.** Declared check error rates gave
   a posterior of 0.985; rates measured from 22 runs with known truth (a
   ledger that found a false pass in a check written the same day) give 0.77.
   The declared θ prior's effect is small (sensitivity table at 2/5/20
   pseudo-trials). → W₁ should require measured rates with their counts, and
   every constructed bad case is a ledger row.
4. **The constructor rebuilds candidates once given interpretations, and
   builds neither the interpretations nor the containment order.** Replayed
   on click-001's inputs it returns both hand-built candidates exactly.
   → Clause 0's construction condition can be met by replay; the carrier (the
   semilattice) is still authored.
5. **A target's value needs the mission's outcomes, and the outcomes are in
   the mission text.** → computed at click time from the text, with cues;
   never a refusal for lacking a precomputed list (finding above).
6. **Outcomes served, weighted any way, do not reproduce the mission's own
   ranking** (4 > 5 > 7 holds on at most 17% of weightings; instance 7 comes
   first most often). The mission's reasons use when an outcome lands and a
   cost that rises with delay ("immediately", "before a VS Code
   implementation exists"), and how fully an outcome is served. → Clause T's
   value term needs a time dimension and partial attainment; weights alone
   cannot supply them.
7. **E has no usable corpus.** Pattern-use frequencies from mission citations
   and from operator turns disagree in sign on the two candidates, each on 1–3
   citations; neither records applied cascades. → E is estimated from Clause C
   enactment records, filled forward.
0. **What stands between the War Machine and completing this mission.**
   claude-1 carried M-futon-seams with library-warranted steps
   (count-every-card-back for the sites, choose-the-grain-where-state-lives
   for the grain, test-by-reproducing-behaviour for the redirect test, ...),
   so the field reaches the mission's criteria. The steps the machine did not
   do, and would have to for the theorem to hold on this mission:
   (a) turning the mission text into interpreted patterns (guard, produces,
       receipt): done by claude-1 for every cascade (E-cascade-real D11);
   (b) the containment order the carrier is built on: authored; the
       constructor returns a precedence, not an order;
   (c) choosing the grain: by hand with no check before enactment (method
       step 3 in the mission's DERIVE);
   (d) the phase exits no instance's cascade asks for (MAP's table, ARGUE,
       DOCUMENT): produced outside any click, because the only click was at
       instance grain.
   Each is a place where the flight would today end in a stop that the
   falsifier above counts against the machine.
8. **Working out of order cost a wrong-grain enactment.** The mission gained
   artefacts for five later phases while at IDENTIFY; the first enactment
   worked from the defect description rather than a DERIVE that had met its
   exit. → the click record should name the phase exits its inputs rely on.
9. **Checks that pass need a constructed bad case.** In this example that
   found: a W₀ that accepted hand-built candidates, false missing meets from
   strict descendants, a grep regex that passed a tree with a branch left, a
   re-anchoring tool that reported moves it had not made, a layout check that
   measured only titles. Each is a row in the check ledger (item 3).

## Holes (2026-09-24)

The theorem is not proved while any of these remains. Each names the work on
the machine that closes it. On M-futon-seams each is a place where a flight
today does not complete the mission.

| hole | what the machine must do | where it showed |
|---|---|---|
| H-interp | turn mission text into interpreted patterns (guard, produces, receipt) | every cascade was written by claude-1 (E-cascade-real D11) |
| H-order | build the containment order from the interpretations, OVER UNITS: each node is one application of a pattern, with the pattern id as a unit attribute (D17's canonical id applies to the attribute, not the node identity); a cascade that applies one pattern twice has two units. Today's `:precedence` is a list of pattern ids and cannot carry that; the Lean order (`CoApplicationKernel`, mathlib4 69c2432f) is over unit indices (claude-10 and claude-8, 2026-09-24) | authored; the constructor returns a precedence only |
| H-grain | choose the grain, with a check before enactment | click-001's first enactment built the wrong grain |
| H-exits | take the mission's completion criteria as the flight's wants | MAP's table, ARGUE and DOCUMENT were done outside any click |
| H-C | read the mission's outcomes at click time, with cues | mission-C was written by hand (claude-1) |
| H-value | a value term with timing and degree of attainment | no weighting reproduces the mission's own ranking |
| H-E | a habit prior from enacted cascades | one enactment record exists |
| H-A | measured rates exist: A-S.md (4fa360f9, r2 fc90808d) and `check_error_rates.clj` (cc831860, r2 f758d702, warrant test-registry-c4ad4223, 12/82). Eligibility is by truth kind from a per-row classification fixture (`test/fixtures/check-ledger-classification/m-futon-seams-v1.edn`: 7 constructed-bad-case, 9 later-review, 6 independent-recomputation, none self-truthed, one reason each); an unclassified ledger yields all-insufficient; claude-8's self-truthed bad case is a verbatim test. Measured per kind on 33 runs: test fp .167 fn .125, grep fp .300 fn .167, validator fp .179 fn .750, layout insufficient (n 4) | next: the scorer consumes measured rates in place of declared ones (W1 amendment; PROOF-2a item 3 of the worked example) | consumer packet, after claude-10's wiring settles where A enters |
| H-publish | carry the repair id and resolution context through to publication, so a close never refuses to publish an obligation the machine itself produced (`:resolution-context-unavailable`, nil-id binder refusal) | 09-24 record `:repair/publication`: every entry `:publication-refused`; walkthrough 06 row 5 (claude-10 and claude-8, 2026-09-24) |
| H-witness | (i) complete the witness mission (M-futon-seams) with library-warranted steps: six of eight exits met (HEAD, IDENTIFY, MAP, DERIVE, VERIFY, INSTANTIATE; verdict lines exact since d05cb755, mission ee86811c; read the witness from ee86811c or later); ARGUE and DOCUMENT open; the ARGUE->DOCUMENT dependency was RETRACTED by claude-1 (futon3c 20959e4f, mission da2ac70e): ARGUE's checklist is done, it is not met only on its inevitability clause, which is a retained finding (defensible, not inevitable), not a gap; DOCUMENT is unrelated; (ii) someone other than claude-1 checks each step's warrant against the rewrite reading, and the checks are rows in the check ledger | the hypothesis witness above was written by the hand that built its cascades; today it is a partial witness (claude-10 and claude-8, 2026-09-24) |

### Owners and what is in flight (claude-8, project lead from 2026-09-24 evening)

| hole | owner | in flight / next | blocked on |
|---|---|---|---|
| H-interp | claude-10 (E-cascade-real D11) after the flight loop; Kimi seats supply interpretation content | D17, D15, D4 landed; F11-INTERP running (kimi-6): the one interpretation the first flight needs, validated offline, as a proposal | D11 (the machine asking for interpretations); promotion of the proposal |
| H-order | Lean: claude-8, landed (69c2432f, 81b5b774); Clojure order over units: claude-10 | constructor's order over units | D4 wiring |
| H-grain | claude-10 (flight loop) | none | flight loop |
| H-exits | claude-10 (flight loop unit 4 and 4b) | D8 landed; H-EXITS-D landed (32ff4f8c, kimi-6): the first flight target states 11 prose completion criteria and `mission-hole-wants` reads none of them (one checkbox shadow item, unfaithfully); M-futon-seams' 9 exits are prose with inline verdicts, 0 read; amendment A-exits (line-anchored locators for prose criteria and phase-exit lines; falsifier: a mid-line token invariant under the verdict) is 4b's spec; whether 4b precedes the loop is claude-10's call | flight loop |
| H-C | claude-10's finding (mission-C at click time, with cues); implementer unassigned | none | H-exits |
| H-value | unassigned (needs the time/degree value term) | AR-40: the lane's G charges instrumental tokens as deviations, so chains with 2+ unwanted intermediates are judged worse than idling (claude-10's D16 probe, unconfirmed cause) | Clause T record; a G fix or a preference model for instrumental tokens |
| H-E | unassigned; one enactment record exists | none | Clause C records |
| H-A | kimi-3 landed A-S.md (4fa360f9) and `check_error_rates.clj` (cc831860: Jeffreys rates, Wilson intervals, min count 5, typed insufficient for :layout n=4); claude-8's review found eligibility vacuous (any free-text truth source counts as independent); kimi-4 fixing: kind-based eligibility + per-row classification fixture | A-S-r2 running. The gap is now exactly the independence question: the ledger is measured (22 rows with truth and source, 19 distinct sources; M-futon-seams clause 1 reads `:measured-from-check-ledger` at futon3c 63acc6d8), and what stands between its rates and a fully measured A is whether each truth source is independent of the check it judges, which is what A-S-r2's per-row classification decides; that classification routes to claude-1 | consumption by the scorer (owed after) |
| H-publish | discovery done: `proof2/packets/H-PUBLISH-D.md` (5cbf0031, kimi-2; census reproduced by claude-8: 68 resolutions, 0 with a discharge context, 15 with no implementation, all 68 re-refused every tick by `catch-up!`; classes A 44 legacy, B 9 written after the schema by the lab discharge script because writers make the context optional, C 15 no implementation). A1 LANDED (f38e20b2, kimi-2, warrant test-registry-e0ee3ff6): `record-implementation!`, `resolve!` and `successor-resolution!` refuse `:discharge-context-missing` (naming phase and id) before writing; a present-but-wrong-phase context refuses with the validator's reason; `finalize!` unchanged (full-loop-discharge-test green); claude-8 rebuilt the bad cases in a temp root: no file written in any refusal. A2 (typed terminal disposition for the 68 legacy records: dismissal or `:publication-unreachable`) is an operator decision | fix done; the recurring 68 refusals per tick continue until A2 | A2: Joe |
| H-witness | (i) claude-1 (ARGUE, DOCUMENT); (ii) done: `proof2/packets/H-WITNESS-check.md` (0cb6396d, kimi-4): 14 steps, 10 patterns, all flexiargs exist with matching pins, 14/14 warranted under the rewrite reading, no met exit rests on an unwarranted step; claude-8 re-hashed one pattern against its pin | (i) ARGUE and DOCUMENT | (i) |

Critical path to the first flight (claude-10's plan, 2026-09-24 evening): unit 1 done (cascade-problems and interpretation-construction tests pass at b32ac3be, 17/94); units 2 and 3 landed (891b4af6: the tick's sources carry :construction; assemble constructs when declared candidates advance no open want; construction's refusal is no longer masked by idle declared candidates; G is the lane's own over a fixed family; choices in the commit: budget {:max-moves 4 :max-expansions 20000}, move cost 0, :unknown read as not established; acceptance: M-aif-policy-conditioned-eig constructs [:aif/two-layer-calibration] machine-constructed, G beats the empty cascade; warrants registered by kimi-5 at 891b4af6: test-registry-91393eb1 (war-machine-construction-test), test-registry-0bee76f0 (cascade-problems-test; a duplicate run 683366a4 also exists); the masking bad case deletes the producing interpretation and asserts `:no-constructed-candidate` at assemble's boundary, the carrier onto the tick record being pinned by the D8 test); D13 now bites: the constructed route on M-aif-policy-conditioned-eig is one withdrawn in a source comment (held-out evidence does not exist), which the machine cannot read, so that mission may not be a fair first flight (FLIGHT-TARGET-D discovery dispatched to kimi-6, with D13 withdrawals-as-data as its amendment); unit 4b LANDED (4dd54848, `futon2.aif.mission-criteria`): every `**Exit criterion:**` paragraph and every bullet under a completion-criteria heading is a flight want (`:exit/h<sha1-12>` of its first stated line, so the token survives a verdict change); a criterion with an inline verdict gets a C4 locator whose decl is the criterion's own text followed by `**Met.**`, so the sentence-only locator (H-EXITS-D §4's falsifier) is refuted and a partial verdict reads not met; a criterion with no verdict gets no locator (`:verdict-not-stated`) and assembly refuses the target naming it. Departure from H-EXITS-D §5 item 2 accepted by claude-8: no separate verdict artifact is required for the WANT; the verdict is the author's declaration observed by C4, exactly the existing receipts' observation-limit ("C4 observes the mission's checked completion declaration, not the truth of its supporting evidence"); the independent evidence leg the theorem's "met with checked evidence" needs is H-A / AR-1's eligible pair, not the want reader. At HEAD: M-futon-seams 6 exits located, MAP and VERIFY read met; M-aif-policy-conditioned-eig's C1-C11 read but none located (no verdicts stated), a second independent reason it cannot be the first flight until its author states verdicts (mission content). Warrants at 4dd54848: test-registry-86e02a4a (mission-criteria-test), test-registry-74526002 (flight-test); adequacy checked: the sentence-only bad case is on identical text. Live pins added (a78e7791): verbatim copies of M-futon-seams (futon3c 52dd90ec), M-aif-policy-conditioned-eig and M-f11 (futon2 22fa0da9) under test/fixtures/mission-criteria/, with tests pinning 6/2-met, 11/0-located, 6/0-located respectively; 22fa0da9 registered (test-registry-ba70496a; heading discrimination pinned both ways: exact `## Acceptance` reads, `## Acceptance tests elsewhere` does not); a78e7791 registered (test-registry-4151a31b). Second reader question sent to claude-10 (from claude-1): M-futon-seams keeps HEAD and IDENTIFY closures in lifecycle.edn only (no verdict line by design); the reader emits no want for them, so the flight's rule-1 closure is over the six stated criteria and silent on the two data-only phases; claude-1 then declared `:verdict-source` (:prose | :data-only) per phase in lifecycle.edn (futon3c d74a7c5a; the checker fails an undeclared phase), so the reader has a field, not a hard-coded exemption; claude-10's answer: neither today, because no existing check class can observe a keyed status in lifecycle.edn faithfully; a structured EDN check C7 is proposed as AR-38 (contract amendment, after the first flight); LANDED (7810cc9a): `mission-criteria/data-only-phases` reads a declared lifecycle file (`:lifecycle {:repo :path}` on the want source, no hard-coded name) and lists every `:verdict-source :data-only` phase as out of view with reason `:data-only-no-checkable-class` and its status as read, never a want, never met; a `:closed` flight records `:closure-scope {:criteria-in-view :want-source :out-of-view}`; test at futon3c d74a7c5a names 6 in view, HEAD and IDENTIFY out (reason `:data-only-no-checkable-class`), the want set pinned to the six so out-of-view phases are never wants; warrants test-registry-1c4e2fb6 (flight-test), test-registry-bc0b1232 (mission-criteria-test) at 7810cc9a, kimi-7; general form: a mission may keep judgements in data and in prose, and a one-eyed reader misreads it. Typed `:verdict-class` per criterion landed (66602747: :met only on an exact **Met.**; :verdict-partial and :verdict-not-met with their :qualifier; :verdict-not-started; :verdict-not-stated; :verdict-unrecognised), and the live pin at 52dd90ec asserts one class per exit and INSTANTIATE's qualifier text (warrant test-registry-74a54ecb at 66602747, kimi-2); a D11 request for a partial exit can carry the qualifier so the answer builds on the evidenced part. Re-pinned at the corrected mission (1116984f, futon3c d05cb755 / ee86811c; warrant test-registry-4b262d92, kimi-2): MAP, DERIVE, VERIFY, INSTANTIATE :met, ARGUE :verdict-not-met, DOCUMENT :verdict-not-started; the 52dd90ec pin stays as the pre-correction record. ARGUE's constraint (closes only through DOCUMENT's account; finding retained) is enforced by D11 part 2's validation (an ARGUE-producing interpretation must need DOCUMENT's exit) and is now written into the mission text under ARGUE's exit criterion (futon3c ea68c485, mission 119e4ede: closes only through DOCUMENT's outsider account, finding retained; an account that softens it has changed the criterion), and READ from the text since 0eb1b827: `mission-criteria/constraints` takes a paragraph opening "**This phase closes only through <PHASE>'s**" as a `:requires` edge (with `:by :mission-text`, line and clause) carried on the `:a-exits` source and validated in the ask step; the declared carrier is dropped and a declared edge is accepted only if the text states the same one, else `:want/declared-constraint-not-in-text`; a constraint naming a phase with no exit is `:unresolved`, not dropped; live pin at futon3c ea68c485 reads the ARGUE→DOCUMENT edge at line 503 with the exit tokens unchanged; the first flight's ask test rests on the text alone (no declared constraint supplied; a declared edge the text does not state refuses); warrants at 0eb1b827: test-registry-9c6172e3 (mission-criteria-test), test-registry-203cbcb6 (flight-ask-test), test-registry-0cdc9c74 (flight-test), kimi-7; `:by :mission-text` pinned at 9fabd5ac (warrant test-registry-735ba623, kimi-2; the ea68c485 fixture stays as the pre-retraction record); unit 4 minimal flight loop LANDED (95aa28b2, `futon2.aif.flight`: choose-target with the oldest open stop-line's repair target first, pluggable want sources (:checkbox; :operator-declared test-only and typed; A-exits to plug in), each click records advanced/open wants, carried `:unreached-wants` and the abstention's `:missing` as needs; `:closed` when every want holds, `:no-progress` stops the flight (rule 2), `:click-limit` is not closure; `flight-assembly-input` assembles only the flight's target (rule 4); no click fired; warrant test-registry-e9e02fb7 at 95aa28b2, kimi-5; adequacy checked: the no-progress case pins status, click count 2 and the per-click progress vector, and the carried want is asserted on click 2's judge input); 4b landed (4dd54848, 22fa0da9, a78e7791); (i) production adapters LANDED (c6d1e542, `futon2.aif.flight-runner`: click-fn over an injectable run-opportunity!, run id <flight>-click-<n>; click-summary reads :unreached-wants from the chosen action's receipt only when it is the flight's target, and on an abstained tick the target's decline from the D8 carrier, `:target-not-in-refusals` never read as clean; observe-fn through observation-checks, a refused check is :unknown; the default judge passes :flight so assembly sees only the flight's target; warrants test-registry-fd526723 (flight-runner-test) and test-registry-bb7f6c0a (flight-test) at c6d1e542, kimi-7; adequacy: the need is the flight's own target's :missing selected by id, other targets' refusals ignored, no refusal for the target gives :target-not-in-refusals); D11 part 1 LANDED (e08d0832, `futon2.aif.want-interpretation`: unproduced-wants; a `:wm/want-interpretation-request-v1` carrying the exit token, the criterion at its recorded lines, the target's facts and admitted interpretations, and the answer shape or a typed decline; request! pins a library retrieval whose query is the criterion and refuses `:want/criterion-moved` if the text at the recorded lines no longer begins with the stated criterion; warrant test-registry-0a8a9b78 at e08d0832, kimi-2; citation hardened at 4e77df7c: `citation-for` locates the criterion by its stated text, one occurrence cited at its current lines with `:relocated-from`, none refuses `:want/criterion-absent` (the criterion itself altered), more than one refuses `:want/criterion-ambiguous`; the old `:want/criterion-moved` is gone; warrant test-registry-a026f436 at 4e77df7c; gap closed at 88f760cd: ambiguity also asserted through request!); part 2 validation LANDED (60495989, `validate-response`: typed decline recorded not rejected; canonical id via the loader's own public `canonical-pattern-id`; receipt source exists with matching sha; produces contains the want; guard tokens known; owner constraints hold; construction uses it and reaches the want; admission accepts; all reasons reported together). On the real case (M-futon-seams pin, the 66a1779e proposals, real library bytes, real admission): DOCUMENT validates; ARGUE is rejected until DOCUMENT is admitted, then validates with precedence [meet-the-reader plain-language-thesis]; claude-1's condition is a declared constraint {:want ARGUE :requires DOCUMENT :by claude-1} until the mission text states it; bad cases: wrong produced token, sha mismatch, no library file, unknown guard token, un-namespaced id, incomplete receipt, ARGUE needing INSTANTIATE (:owner-constraint-violated); D17 sibling added at 56bf7212 (string-spelled id validates under the canonical keyword). Warrants at 60495989: test-registry-8463847e (want-interpretation-test), test-registry-0d9abee9 (cascade-sources-spelling-test), kimi-7; the real-case test reads the proposal file from proof2/proposals/, hashes the live library bytes at validation time (mutated-sha negative), and calls the tick's own admission; only the mission text is a pinned copy. Part 3 publication LANDED (6fd5e65f, before claude-8's answer arrived, and meeting it): `publish!` writes only a `:valid` result, into a machine-owned store (default `/home/joe/code/futon2/data/wm-interpretations`, one `:wm/machine-interpretations-v1` file per target, separate from `resources/wm/cascade-sources/`), keeping request and response whole under content ids and putting both ids, the checks passed and the answerer's receipt on the interpretation's receipt; a different reading of an already-published pattern refuses `:want/conflicting-publication`; the tick merges published interpretations into the sources before assembly (`assemble-cascade-problems-with-published`), a hand declaration winning on any id it names, and records the merged ids under `:machine-interpretations`. Decision recorded by claude-8 (principle, not ruling): a validated response to a machine-issued request publishes without an operator step; the click remains the only gate. Hardened at ddb81953: receipts carry `:validator {:ns :source-sha256}`; `issue!` records every request the machine issues under `data/wm-interpretations/requests/<id>.edn` and `publish!` refuses `:want/request-not-issued` for an unrecorded request id or one whose target/want differ from the validated result; the raw response is kept as a record, never merged; warrant test-registry-644a062a at ddb81953, kimi-2, the no-write asserted after both request-not-issued refusals. Registration with kimi-2; warrants at 6fd5e65f: test-registry-2973876c (want-interpretation-test), test-registry-bdb4893f (war-machine-construction-test), kimi-2; adequacy: the non-valid case asserts no file written via read-published; a hand-vs-published disagreement is dropped silently, AR-39; Part 4, the asking (claude-10, next): in the flight, before a click, for each unproduced want: request!, answer-fn, validate-response, publish!; a decline is a flight need. Dispatch decisions by claude-8: the production answer-fn is an Agency bell (`--mode work`, requisition = the target mission) followed by polling the job to a terminal state, not a whistle and not interpretation_job (post-selection, attempt-keyed); the reply must contain exactly one fenced EDN form of schema `:wm/want-interpretation-response-v1` or a typed decline, anything else is `:unparseable-response`, recorded as a need; the job id and answering seat go on the receipt; first flight answers from kimi-6 (holds the readings), never kimi-1 (claude-1's delegate); (ii) D16 LANDED (a4e2c34c, `resolve-cascade-horizon`: declared wins, else the largest number of admitted interpretations on any of the tick's targets or the longest declared order, at least 1, one value per family, resolved after the flight input and published interpretations merge; the T=2 literal and the judge-opts bypass removed; `:cascade-horizon {:value :authority :per-target}` and `:construction-parameters {:budget :move-cost}` as {:value :authority} on selection and abstention; bad case: the 4-step chain T=2 cut constructs at the computed 4 (declared 2 wins over computed 4; empty family resolves to 1, not 2); warrants test-registry-3df76a0a (war-machine-horizon-test), test-registry-12556bfb (war-machine-construction-test) at a4e2c34c, kimi-7; the bad case passes 2 as a declared value standing in for the removed literal). G finding from the same probe recorded as AR-40 under H-value (instrumental tokens penalised by G; does not affect the first flight). Part 4, the asking, LANDED (1669c2e6, plus 3e4c9c27 an inbox-zero auto-commit that took claude-10's in-progress flight.clj mid-work): `flight-runner/ask-fn` runs before each click; for every want no admitted or published interpretation produces: issue!, request! (criterion from the :a-exits source), answer-fn, parse, validate, publish; after any publication earlier-rejected answers are re-validated until nothing changes, so ARGUE settles after DOCUMENT in one step; every non-publication is a flight need with its job id (:no-criterion, :request-refused, :not-answered, :unparseable-response, :declined, :rejected), never retried silently; `agency-answer-fn` = the runner's dispatch! (bell, mode work, requisition = the flight's target) then poll-job!, seat named by the caller (kimi-6 first), receipts carry :answered-by {:seat :job-id}; reply grammar one fenced EDN form of :wm/want-interpretation-response-v1 or a decline. Found and fixed: M-futon-seams had no hand source so assembly refused :beta-not-declared; an undeclared flight target now gets :WM (or the flight's context), a source-given context winning. Real case on d05cb755 with kimi-6's readings as stubbed answers: both published, no needs; bad cases each a need. Warrants at 1669c2e6: test-registry-6b8fa7c8 (flight-ask-test), test-registry-75a2b5e7 (flight-test), kimi-2; gaps closed at 88f760cd (warrants test-registry-c2301ad2 want-interpretation-test, test-registry-6b15e09a flight-ask-test, kimi-2): the :declined need asserts its job id and read-published nil afterwards; ambiguity asserted through request!. DRIVER LANDED (d9c9fb57 + futon3c b7340968: `clojure -M -m futon2.aif.flight-driver <target> --seat <kimi-N> --repo --path --lifecycle-path [--run]`; plan by default, --run flies ONE flight; kimi-1 refused; clicks go through POST /api/alpha/wm/click with a flight-edn field, so each is an ordinary click with budget consume and cast-seat preflight; the run record now carries :decision :chosen; with --run it writes <store>/flights/<id>.edn naming every request, answer job, publication and click; driver tests 6/26; bad cases: no --run flies nothing, kimi-1 refused, an unstarted click is an abstention not a retry (exactly one POST, `:click-not-started`); warrant test-registry-61eb3b8c at d9c9fb57, kimi-7; the no-run case is guarded by a throwing stub on run-flight!, the delegate refusal is a message-matched ex-info). Live plan at futon3c HEAD: in view MAP, DERIVE, ARGUE (closed by hand by claude-1 at 071dee27, outside any flight, constraint paragraph removed), VERIFY, INSTANTIATE met, DOCUMENT not met; out of view HEAD, IDENTIFY; one open want DOCUMENT (:exit/h54d16050a9dc, request at line 889); constraints read []; horizon 4 declared; budget and move cost claude-10's with no ruling found. Gates: the runner-suite fix (below), reload of futon3c b7340968 and futon2's runner/war_machine/flight namespaces from master at the go, and Joe's click authorization (ordinary click budget). claude-1 asked to leave DOCUMENT to the flight. Runner-suite gate: 8 failures 2 errors at HEAD in two close tests. Bisect (RUNNER-SUITE-D, 795d4001, kimi-5): no futon2 commit broke them; they have failed since the commit that added them whenever live futon3's flexiarg bytes differ from the fixtures' pinned sha (42371c5d pinned, futon3 now at b367ff7d), because the close re-validates receipt sources under a hard-coded `/home/joe/code` root: fixture/environment coupling, the guard working; corroborated by 6e531aae's revert with the same signature. Fix dispatched (RUNNER-SUITE-F, kimi-5): injectable code root, the pinned flexiarg bytes owned under test/fixtures, no re-pin treadmill, sha check unweakened. Harness finding: plain worktree runs of that namespace trip the runner's drift guard (hard-coded canonical path); the bisect used a bind mount. Same exposure closed pre-emptively in the D11 tests (d8a1081b, claude-10): the two library files the first flight's readings cite are pinned verbatim under test/fixtures/want-interp-library/, passed as :code-root; production still reads the live library; warrants test-registry-8cf2397b (want-interpretation-test), test-registry-bf64074c (flight-ask-test) at d8a1081b, kimi-2, both pinned files hashing exactly to the receipts' shas at futon3 9c248d52. unit 5 D11. First flight target REOPENED (Joe, 2026-09-24 late evening): M-futon-seams is the worked example claude-1 carries and claude-10, claude-8 and Joe annotate, not a War Machine target; it stays the hypothesis witness (H-witness) and the reader's pins of it stay as the record. COMPLETE at futon3c 3f5f44dd (mission sha d13c5cfe), eight of eight exits met by claude-1; DOCUMENT produced packages/turn-seam (turn-record schema from 159 records, dispatch contract, conformance checker) plus three docbook entries; a ninth instance recorded, not fixed (the futon4 docbook's toc.json generated from a vanished index.org, 46 entry files against 37 TOC rows). claude-1 suggests a flight on instance 5 or 7 (cascade and wiring, no click) as a fuller exercise of steps 6-9 than DOCUMENT would have been; FLIGHT-TARGET-D2 considers them. The mission's anchors, cues and shas move together via scripts/repin.py; verdict_check.py holds every section's verdict to lifecycle.edn. H-witness shape (claude-1's own account): of eight exits five closed cleanly and three took a correction (DERIVE's verdict went stale silently and the reader found it; INSTANTIATE's was over-hedged; ARGUE took three verdicts, closing once the mission's own constraint, a judgement recorded in a form something else can contradict, made registry-first forced rather than defensible); two of the three were caught by the reader or its reviewer. Final pin of the finished state at 3f5f44dd in 051e209e (claude-10): Status COMPLETE, all six exits :met, constraints empty; the H-witness record is the sequence of pins 52dd90ec, d05cb755, ea68c485, 20959e4f, 071dee27, 3f5f44dd; warrant test-registry-39b1ae4c at 051e209e (kimi-7). FLIGHT-TARGET-D2 (kimi-6) surveys every mission file in futon2 and futon3c through the reader and ranks candidates by faithfulness and by a first want startable from current facts, NOT by precomputed locators: Joe (via the 924a6820 finding on C) rules that the machine takes any open mission and computes reasonable completion criteria on the fly, so the reader's :verdict-not-stated refusal and its silence on missions with no recognised criteria form are the same lazy shape as the :observation-required refusal, and become computing steps through the D11 ask channel (part 5, claude-10): criteria extracted from the mission text with cues, verdicts computed as observations with cited evidence (:verdict-computed, owner's stated verdict always winning), typed absence only when the step reads and finds nothing; and, Joe's third outcome, when the ask is genuinely unclear the step's product is questions to the owner, each anchored to the unclear span and naming the readings that would yield different criteria, logged as a need of kind :owner-question (the flight ends :not-a-target-yet), never a refusal and never work against a vague specification. FLIGHT-TARGET-D2 landed (8b073dca, kimi-6, before the correction reached it): over every mission file in 11 repos, exactly one mission has any located criterion (M-futon-seams, now complete); every other mission that states criteria states them without verdicts (100% :verdict-not-stated), so tier 1 (closeable faithfully today) is empty and the binding constraint on the first flight is verdict lines, not interpretations. Under Joe's ruling that is part 5's first job, not an owner pass. FLIGHT-TARGET-D2 §6 (f39da8fb, kimi-6) reverses on the startability property: M-f11 FIRST, since its first open want :hole/h2045faa0e7cc has a validated interpretation (39ace063) whose only guard fact :hole/h9ab212b3281d is observed true at HEAD today, while M-omni-wm-runner's first want is unknowable until its locators are computed (and may degenerate to zero open wants if the parcels are done); M-futon-seams' DOCUMENT was the only target passing both properties today, excluded by scope. FLIGHT-TARGET-D2 §7 (ed313646, kimi-6): property 1 retracted as a gate; per-mission computation report (extraction needed ~40 checkbox-only missions; verdict computation needed: M-f11, M-omni-wm-runner, M-essays-retraction-visibility, E-campaign-spec-grounding, the two AIF missions, M-war-machine-pilot; nothing to compute: M-futon-seams only); hand-extracted, cued criteria for the top three (M-f11 F1-F7, M-omni-wm-runner O1-O5, M-essays-retraction-visibility R1-R4) as the reference the reader is tested against. Findings the cues expose: a cross-criteria dependency stated only in a boundary sentence (M-f11 line 48: repair-024 resolved only after the ordinary gates, which the constraint reader's single recognised form does not see, and which conditions the flight's startable first want on F1-F6; sent to claude-10 for the plan); anchor data buried in prose (F4); a done-definition that never became a bullet (O4: the durée click in-process, tested by the bullets only under stubs, a false-closure risk for M-omni-wm-runner); negative criteria from scope-out lists (O5). Second target: M-omni-wm-runner (futon3c; three Acceptance bullets at lines 87/91/95 covering its done-definition, verified by claude-8 through the reader; owner claude-3; producers apparatus/done-is-observed-running, contracts/every-entry-has-a-falsifier); part 5's plan requested on it. M-f11 corrected from first-ranked in FLIGHT-TARGET-D to refusing assembly behind six unverdicted Acceptance bullets. PART 5 LANDED (81ff1315, claude-10, `futon2.aif.mission-reading`, same ask channel): one departure from the requested design, accepted: a reading step does NOT return met/not-met (a computed judgement is class J without an admitted rate, which WM-04 refuses, and a verdict recorded once goes stale); it returns a checkable locator (C3-C6) deciding the criterion, cued to the criterion's own words, and the criterion is then observed each click by that check like a stated verdict; asked only for criteria with no stated verdict, so the owner's verdict always wins. Criteria reading when the text states criteria in no recognised form: criteria come back cued to exact lines, one bad cue refuses the whole reply, published criteria used only while their cues resolve. A `:read-fn` runs in flight/run! before wants are read (criteria, then locators); anything unpublished is a need with its job id; the :a-exits source reports :criteria-from, :machine-located, :readings-needed; a checkbox want carries its own task line as its criterion; the driver's plan prints what was read and what it would compute. Bad cases rejected: class-J locator, missing fields, cue not in the criterion, unstated reading, refusing check. M-futon-seams pins unchanged. M-f11 plan: criteria from text, 7 open wants (six Acceptance plus the repair-024 checkbox), 6 locator readings requested at lines 32-39. Suites 69/242 and 102/560, kondo 0/0. Warrants at 81ff1315: test-registry-b978f739 (mission-reading-test), test-registry-506ffc4a (flight-test), test-registry-06a7f45a (flight-driver-test), test-registry-f38ddcb3 (mission-criteria-test), kimi-7; class-J refuses :class-not-checkable; a stated verdict is never asked (readings-needed empty over the 071dee27 pin); gaps: the bad-cue need asserts kind and missing but not its job id; the M-f11 pin asserts counts (6 wants, 6 locators, 6 published, 6 located), not :criteria-from :mission-text nor the lines 32-39. Computed judgements, if ever wanted, need a class-J amendment with measured rates (H-A). Third outcome landed (d91471a3): a criteria reply may carry :questions, each anchored to a span that must match the text at its lines and naming at least two alternative readings (a span-less, unresolving, or single-reading question refuses the whole reply); mixed replies allowed; each question is an :owner-question need sent by Agency bell to the mission's **Owner:** line, or recorded for the requisition's caller with no bell if none is named; a flight left with no wants ends :not-a-target-yet before any click with :open-questions on its record; a flight with clear criteria flies them and lists open questions in :closure-scope; the driver prints published questions. Suites 74/261, kondo 0/0. Warrants at d91471a3: test-registry-ddd80239 (mission-reading-test), test-registry-477b393f (flight-test), test-registry-5db58607 (flight-driver-test), kimi-7; the unclear-mission test pins :not-a-target-yet, a zero click counter, and the mission-named owner receiving the anchored question through an injected notify; a single-reading question refuses :question-without-alternatives. Earlier revision (claude-10 after extending the reader, 22fa0da9): M-futon-seams, the only candidate whose completion criteria are all observable under `:a-exits` (at futon3c 52dd90ec: 6 phase exits, MAP and VERIFY read met; claude-1 then found DERIVE's verdict stale and INSTANTIATE's partial and fixed both at d05cb755 / mission ee86811c, with a verdict checker that refuses to render disagreement; at ee86811c the reader should find 4 met and ARGUE, DOCUMENT open). The first flight's wants are ARGUE (`:exit/hac75428b9c97`) and DOCUMENT (`:exit/h54d16050a9dc`); the interpretations producing them exist as PROPOSALS (66a1779e, kimi-6, `proof2/proposals/M-futon-seams-interpretations.edn`): DOCUMENT by `writing-coherence/meet-the-reader-where-they-are` (needs INSTANTIATE, met), ARGUE by `writing-coherence/plain-language-thesis` (needs DOCUMENT), so ARGUE is reachable only through the account, as the owner requires; verified by claude-8 against the live mission through the reader (tokens match; four exits met) and by construction (candidate `[meet-the-reader plain-language-thesis]`, no unreached want; without plain-language-thesis ARGUE is `:no-producer`; without meet-the-reader the target refuses). They wait for D11's request; nothing promoted by hand. Follow-up 78439f58: DERIVE/INSTANTIATE interpretations kept and marked not first-flight wants; ARGUE's constraint stated in guard (needs DOCUMENT), reading, and scope-limit; the finding itself is not token-expressible (AR-37). RETRACTION (claude-1, futon3c 20959e4f, mission da2ac70e): there is no ARGUE->DOCUMENT dependency; ARGUE is not met because the design is defensible, not inevitable, a finding the owner retains, so NO honest interpretation produces ARGUE's exit and the first flight's only want is DOCUMENT; Follow-through landed (bfa73455, claude-10): pins at 20959e4f (constraints `{:requires [] :unresolved []}`, ARGUE :verdict-not-met) and at 071dee27 (claude-1 then closed ARGUE by hand: five exits met, DOCUMENT open); the ask test's ARGUE-skips-DOCUMENT case replaced by a declared ARGUE->DOCUMENT edge refusing `:want/declared-constraint-not-in-text` against 20959e4f, readings publishing without it; a `:verdict-not-met-retained` class (a verdict beginning "**Not met, retained as a finding**") that is never a want, returned under `:retained` with `:reason :retained-finding` and named in `:closure-scope :out-of-view`, plain "Not met" staying a want; the driver's plan pin at 20959e4f asserts `:constraints []`; D11 tests read a pinned copy of the proposals at 78439f58 (kimi-6's withdrawal of the ARGUE reading was uncommitted in the shared tree). Warrants at bfa73455: test-registry-82a2852c (mission-criteria-test), test-registry-216ba921 (flight-ask-test), test-registry-e644b98f (flight-driver-test), test-registry-09332656 (flight-test), kimi-7; the retained-finding control shows removing the retention clause returns the phase to a want. SEAMS-INTERP Revision 3 (8e1aa1b1, kimi-6): the ARGUE reading is withdrawn as data in the D13 shape (`:withdrawals [{:withdrawal/id :withdrawal/w2026-09-24-argue-plain-language-thesis :by :at :reason :owner-retracted-dependency-no-honest-reading :restore-when [] :restore-observed :not-applicable}]`, interpretation and receipt preserved verbatim); DOCUMENT's reading unchanged. Verified by claude-8 on the live mission at futon3c HEAD: constraints `{:requires [] :unresolved []}`, classes MAP/DERIVE/ARGUE/VERIFY/INSTANTIATE :met, DOCUMENT :verdict-not-started, open wants `[:exit/h54d16050a9dc]`, construction on the revised proposals `[[:writing-coherence/meet-the-reader-where-they-are]]` with nothing unreached. The retained finding itself still needs its own token (AR-37). The earlier constraint-in-text and validation notes below record the state before the retraction. M-f11 is NOT a faithful checkbox flight: its done-definition is a six-bullet Acceptance section with no verdicts, so the checkbox source would have closed it falsely; the reader now reads Acceptance / Success criteria / Exit criteria / Done when sections. Order: D11 before the production adapters. Earlier target, per FLIGHT-TARGET-D (0962ed9e): M-f11-find-production-successor, one open faithful checkbox want with no producer, needing exactly one interpretation with a receipt (PROPOSAL LANDED, 39ace063, kimi-6: `proof2/proposals/M-f11-interpretation.edn`, pattern `coordination/bind-promotion-to-post-repair-replay` at futon3 9c248d52 sha 915d4188, guard needs F11's ordinary acceptance and forbids the want, produces the want; five runners-up rejected with reasons in `F11-INTERP.md`; validated by claude-8 in a fresh process: fixture as-is refuses `:no-supported-order`, merged constructs `[:coordination/bind-promotion-to-post-repair-replay]` machine-constructed with no unreached want. Promotion into `resources/wm/cascade-sources/M-f11-find-production-successor.edn` is the explicit step reserved for Joe; D12's worth-taking under the lane's real G is decided at click time). M-aif-policy-conditioned-eig ranks third: its only producer is a route withdrawn in a comment and its held-out evidence files belong to the T-repair ticket (D9 settled `:evidence-scoped-to-other-target`); it needs D13 (withdrawals as data, amendment shape in the packet) and wall-clock evidence. T-repair and M-wm-08 have no open want. M-futon-seams cannot fly before D11. Kimi in parallel: D4 fixture landed (02202c67: all four targets' constructor inputs at f25d0880, pre-D4 refusals pinned, M-aif-policy-conditioned-eig constructs directly, edited-observation falsifier refuses; warrant test-registry-898b5ac4), H-EXITS-D landed (32ff4f8c; unit 4 cannot start from checkbox wants for the first flight target, see H-exits row); D14-HORIZON-D landed (6b662a25: the judge's T=2 is a literal else-branch behind a max-lift of per-file declarations, only T-repair declares 4; assemble's own `:horizon-not-declared` refusal is pre-empted by it; amendment H1 as corrected by claude-10: not a refusal on absence (rule 3) but a per-family computed horizon, declared value winning, recorded as `:cascade-horizon {:value :authority :computed-from}` on selection and abstention; the T=2 literal and the judge-opts bypass go; constructor already shares the judge's horizon since 891b4af6; done inside unit 4). Clicks held until the loop exists.

### Appendix to the Holes table: the PROOF-2 register rows (claude-8, 2026-09-24)

`PROOF-2-THEOREM-draft-2026-09-24.md` carries a register AR-1..AR-28 from the
2026-09-24 discovery packets, reviews and walkthroughs 01-05 (all
`:proposed`; Codex and Zai signatures blocked until 09-26 / 09-29). They carry
into PROOF-2a under "unchanged unless restated", but they are not one kind of
thing. Sorted here so a reader of the Holes table sees which rows are
evidence for a hole and which are the per-click layer.

**(a) Rows that name a hole, cited beside it.**

| register row | what it found | hole |
|---|---|---|
| AR-24 | on the recorded clicks the scorer consumed no token-level A: precision-family model is `:class-emission`, `:rates` a typed absence, the G decomposition's A term `:consumed-value-not-recorded` | H-A |
| AR-1 (OBS-D r2) | zero eligible truth/observation pairs on all 14 closes; 64 observations without an independent truth leg | H-A |
| AR-17, AR-26, B-R instance | the B commit point is the comparison-time append, so a refused close's row fed the consumed theta; the G decomposition has no `:B` term; the ledger moved after the 09-23 judge read it and nothing records which version was read | H-E (and the B commit point, which 2a's clause 5 inherits) |
| AR-27, AR-28 | the one close with `:accepted? true` is `:grounded-no-change` with grounding witness `:resolved? false` (stringified readback); no close of 14 is classified a known success | the theorem statement: completion, not an accepted-and-ungrounded close |
| AR-22 | two argmaxes with two tie rules disagree on an exact tie; the enacted candidate was the marginal's pick, the per-policy field recorded the other | Clause C, at candidate grain |
| AR-23 | the accepted-increment's acceptance conjunct observes the ticket at a floating HEAD, not at the artifact revision the comparison is pinned to | Clause C ("evidence that a check confirms") |
| walkthrough 06 row 6 (f539c896) | the 34 open stop-lines are the standing state a flight starts from; the 09-24 click selected over the whole universe with them open | H-exits on a T-repair target: a standing stop-line is a target whose discharge contract is the completion criteria, and it comes first (queue ruling bbae7593; flight rule 4) |
| CLICK2-D | no admitted target could construct: wants already true, or the declarations route no candidate at the false want; every recorded candidate hand-admitted | H-interp, H-exits |

**(b) Rows that are the per-click layer (P_k / W_k / X_k), subordinate to the
holes and reviewed after them:** AR-2, AR-3, AR-4 (trial vector and dedup
layers), AR-5, AR-18 (`rowTheta` axis), AR-6, AR-21 (hex-double decoding),
AR-7, AR-8 (F ablation retains G; F carrier is machine-double plus symbolic
prefix), AR-9, AR-10, AR-11, AR-19 (numeric refinement and strict action
stability), AR-12, AR-13, AR-14, AR-20 (carrier names, one refinement
authority, hash projections), AR-15 (`:pair-sha256` byte domain), AR-25
(`:accepted?` field domain). Landed code in this layer, unreviewed: NUM-R
(12523685), OBS-P (cabc8e67, 54295ca0), B-C (4a2ba931), F-L (mathlib4
41a3691f); walkthroughs 01-05 describe the machine as the records carry it.

**(c) AR-16, a carrier that a hole needs.** A decline on the abstention path
is a hole under this theorem, but the tick record carries no typed decline
(the reason survives only in the cohort's selection event and the scan
markdown). Keep the carrier: without it the hole cannot be seen from the
record.

Standing (agreed with claude-10, 2026-09-24): hole work goes first; A20
reviews of layer (b) wait for quota; clicks stay held until the flight loop
and the tick wiring of `cascade_problems/assemble`'s `:construction` exist
(5922c56e adds construction but the live tick in `war_machine.clj` does not
yet supply it, so a click today repeats click 2's abstention); when to fire
is a budget call for Joe.
