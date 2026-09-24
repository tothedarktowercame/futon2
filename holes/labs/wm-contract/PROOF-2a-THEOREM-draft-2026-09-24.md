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

**THEOREM (PROOF-2a).** The implementation is not fake with respect to the
named War Machine Lean model iff, for every click required by clauses T and
0–6, the provenance proposition `P_k` identifies the values actually
consumed, the concrete Lean proposition `W_k` checks on those values, and the
independently constructed bad extract `X_k` makes the same proposition fail.
All eight clauses hold on the single linked sequence `L` of PROOF-2
(ordinary live clicks under A21); fixtures and hand-built inputs establish
none of them.

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

## Finding from the worked example: Clause T needs a mission-level C that no mission record carries

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
records in futon6, 0 record outcomes or wants. So what each historical
mission needs is extraction of its stated outcomes into a typed C (with cues
into the mission text), plus the served-by links from targets to outcomes,
not new authoring. Until a mission has that, Clause T's value term is a typed
absence and the choice of target rests on cost alone, which is the case in
every click recorded so far.

The same holds for E: the corpora of past pattern USE (mission citations,
operator turns) disagree in sign on the worked example and neither records
which cascades were enacted. E's input is enacted-cascade records (Clause C),
which exist for one click.

Test of the extraction: M-futon-seams mission-C (commissioned from its owner,
claude-1), checked by whether the computed value reproduces the mission's own
4-5-7 ranking or says where it departs.
