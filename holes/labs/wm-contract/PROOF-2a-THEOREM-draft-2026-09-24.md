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
| H-A | observation error rates measured per check kind | 22 ledger rows; declared rates were used first |
| H-publish | carry the repair id and resolution context through to publication, so a close never refuses to publish an obligation the machine itself produced (`:resolution-context-unavailable`, nil-id binder refusal) | 09-24 record `:repair/publication`: every entry `:publication-refused`; walkthrough 06 row 5 (claude-10 and claude-8, 2026-09-24) |
| H-witness | (i) complete the witness mission (M-futon-seams) with library-warranted steps: six of eight exits met (HEAD, IDENTIFY, MAP at futon3c 5fe889bc, DERIVE, VERIFY at bfe16c2a, on corrected evidence at 63acc6d8; read the witness from 63acc6d8 or later, INSTANTIATE); ARGUE and DOCUMENT open, and ARGUE closes when DOCUMENT does (its outsider account is DOCUMENT's product) (claude-1, 2026-09-24 evening); (ii) someone other than claude-1 checks each step's warrant against the rewrite reading, and the checks are rows in the check ledger | the hypothesis witness above was written by the hand that built its cascades; today it is a partial witness (claude-10 and claude-8, 2026-09-24) |

### Owners and what is in flight (claude-8, project lead from 2026-09-24 evening)

| hole | owner | in flight / next | blocked on |
|---|---|---|---|
| H-interp | claude-10 (E-cascade-real D11) after the tick wiring; Kimi seats supply interpretation content once the request/validate/publish seam exists | D17 landed (canonical ids), D15 landed (partial wants) | D4 wiring |
| H-order | Lean: claude-8, landed (69c2432f, 81b5b774); Clojure order over units: claude-10 | constructor's order over units | D4 wiring |
| H-grain | claude-10 (flight loop) | none | flight loop |
| H-exits | claude-10 (flight loop unit 4 and 4b) | D8 landed; H-EXITS-D landed (32ff4f8c, kimi-6): the first flight target states 11 prose completion criteria and `mission-hole-wants` reads none of them (one checkbox shadow item, unfaithfully); M-futon-seams' 9 exits are prose with inline verdicts, 0 read; amendment A-exits (line-anchored locators for prose criteria and phase-exit lines; falsifier: a mid-line token invariant under the verdict) is 4b's spec; whether 4b precedes the loop is claude-10's call | flight loop |
| H-C | claude-10's finding (mission-C at click time, with cues); implementer unassigned | none | H-exits |
| H-value | unassigned (needs the time/degree value term) | none | Clause T record |
| H-E | unassigned; one enactment record exists | none | Clause C records |
| H-A | kimi-3 landed A-S.md (4fa360f9) and `check_error_rates.clj` (cc831860: Jeffreys rates, Wilson intervals, min count 5, typed insufficient for :layout n=4); claude-8's review found eligibility vacuous (any free-text truth source counts as independent); kimi-4 fixing: kind-based eligibility + per-row classification fixture | A-S-r2 running. The gap is now exactly the independence question: the ledger is measured (22 rows with truth and source, 19 distinct sources; M-futon-seams clause 1 reads `:measured-from-check-ledger` at futon3c 63acc6d8), and what stands between its rates and a fully measured A is whether each truth source is independent of the check it judges, which is what A-S-r2's per-row classification decides; that classification routes to claude-1 | consumption by the scorer (owed after) |
| H-publish | discovery done: `proof2/packets/H-PUBLISH-D.md` (5cbf0031, kimi-2; census reproduced by claude-8: 68 resolutions, 0 with a discharge context, 15 with no implementation, all 68 re-refused every tick by `catch-up!`; classes A 44 legacy, B 9 written after the schema by the lab discharge script because writers make the context optional, C 15 no implementation). A1 (context mandatory at write) dispatched to kimi-2. A2 (typed terminal disposition for the 68: dismissal or `:publication-unreachable`) is an operator decision | A1 running | A2: Joe |
| H-witness | (i) claude-1 (ARGUE, DOCUMENT); (ii) done: `proof2/packets/H-WITNESS-check.md` (0cb6396d, kimi-4): 14 steps, 10 patterns, all flexiargs exist with matching pins, 14/14 warranted under the rewrite reading, no met exit rests on an unwarranted step; claude-8 re-hashed one pattern against its pin | (i) ARGUE and DOCUMENT | (i) |

Critical path to the first flight (claude-10's plan, 2026-09-24 evening): unit 1 done (cascade-problems and interpretation-construction tests pass at b32ac3be, 17/94); unit 2 D4 tick wiring in progress (~2h; acceptance on a Kimi-built replay fixture of CLICK2-D Part 2); unit 3 D12 inside unit 2 (the lane's real G on each constructed candidate, no injected G; if G does not score the empty plan worst that is a G defect to report, not pin around); unit 4 minimal flight loop (half a day+): target fixed, stop-lines first, wants = `- [ ]` lines the code already reads plus carried `:unreached-wants` and `:missing`, D16 inside it; unit 4b prose completion criteria (H-exits proper) from the H-EXITS-D discovery; unit 5 D11. First flight: M-aif-policy-conditioned-eig (declared interpretations); M-futon-seams cannot fly before D11. Kimi in parallel: D4 fixture landed (02202c67: all four targets' constructor inputs at f25d0880, pre-D4 refusals pinned, M-aif-policy-conditioned-eig constructs directly, edited-observation falsifier refuses; warrant test-registry-898b5ac4), H-EXITS-D landed (32ff4f8c; unit 4 cannot start from checkbox wants for the first flight target, see H-exits row); D14-HORIZON-D landed (6b662a25: the judge's T=2 is a literal else-branch behind a max-lift of per-file declarations, only T-repair declares 4; assemble's own `:horizon-not-declared` refusal is pre-empted by it; amendment H1 deletes the fallback and carries `:cascade-horizon {:value :authority}` on the certificate; D16 hook is the judge-opt feeding the `cascade-horizon` binding, per-family, shared with the constructor). Clicks held until the loop exists.

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
