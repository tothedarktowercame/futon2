# P-validated-R5 — a validated R5, and everything else built around it

**Status:** DRAFT problem record, 2026-08-30 (claude-15, from Joe's direction).
The first record written under `futon4/holes/delivery-lifecycle.md` v2. The S1
fields (`problem`, `now`, `solved`) are the commissioner's; they are drafted here
from Joe's words of 2026-08-27..30 and are **his to confirm or rewrite**. Nothing
below is a build packet.
**Gate:** operator-acceptance — Joe confirms §1 before any §5 packet is dispatched.

---

## 0. Why R5, in Joe's words

> *"We need to start thinking about how we're going to organize that work so
> that we get the validation layer or the epistemic layer aligned with reality —
> on each node in the graph, and also get the behavior of the overall system to
> be validated as well. And we may need to dig deeper into R5 in order to come
> up with a definitive definition of what we mean by G, cascades and policy and
> how they relate to each other, in order to validate that one. Since we had
> some reasonable thinking about that already, maybe we should just start there
> and see if we can't build a validated R5 and build everything else around
> that."* (2026-08-30)

R5 is the right start for a reason the worksheets make measurable: it is the
node whose *formulas* are already the theory's (risk, ambiguity — Gate-0
theory-defined) and whose *arguments* are not (an action, not a policy; a
channel floor, not a C over outcomes). So R5 is where the three blocked nouns
that block every other node — **Policy**, **Q(o∣π)**, an **Outcome** space —
have to be defined first, and where defining them pays immediately.

## 1. The problem record (S1 — Joe's fields)

```
problem:   G is computed over single actions from a channel-range C; "policy" is a
           borrowed name for a cascade, and a cascade in the code is a bag of pattern
           ids. Nothing states what G over a cascade IS, so nothing can be shown
           faithful to it, and the evaluate stage cannot say what its criteria do
           not cover.                                              [Joe: confirm/rewrite]

now:       - efe.clj:808 rank-actions [state candidate-actions]; core_efe.clj:94 g-efe over an action
           - fold_escrow.clj:113 cascade = (vec (get-in d [:cascade :pattern-ids]))
           - glossary: "a policy π is the AIF name for a pattern language/cascade when that
             composition is being scored" — composition, not identity; "functional rather than literal"
           - wm-trace: ambiguity moved 0 winners / 674 ticks; 0 coverage keys in 791 records;
             coverage_check.clj has no caller
           - M-G-over-cascades (06-22): T1 "define cascade", T2 "define G over cascades" — never discharged
           - the 08-27 record (claude-13 turns): Q(o∣π) exists in three carriers, none conditioned on a cascade

solved:    (a property of the MODEL, checked before running — v2 §1)
           A Lean statement, with an emitted clause and a Clojure mirror, in which
             Policy, Cascade, Outcome, C and Q(o∣π) are each a declared type or function;
             Cascade is the §2.1d object (a DAG over patterns with authored edges);
             a Policy's relation to a Cascade is stated (identity, distribution over, or
               cascade + acting order — ONE of these, chosen);
             Q(o∣π) is DERIVED (a kernel composed along the cascade), not authored;
             G(π) = risk + ambiguity over Q(o∣π) and C, quantified over ANY mission;
           and the model REFUSES, as theorems: a list of ids offered as a cascade; a G over
           a single action offered as G(π); a C over channels offered as a C over outcomes;
           a prediction authored by hand (S-G3); a criterion set that cannot name an outcome
           it does not cover.                                       [Joe: confirm the choice at "ONE of these"]

facades:   type substitution (list for cascade; action for policy) — GainChain-style refusal theorem
           rename (G_core called G(π)) — PolicyGrade S-G1/S-G2/S-G4 already refuse two of three
           fixture (Snatch's six patterns offered as "any mission") — domain quantifier
           dark build (a kernel defined, never composed on a live cascade) — the mirror step
           wrong corpus (82 flights' frequencies offered as Q(o∣π)) — "a frequency over closed
             flights is not falsifiable; a guarantee on the next flight is" (E-R5)
           self-report (a coverage statement that covers everything) — declaresCoverage
           inert quantity (a term that never moves a decision — ambiguity today) — discriminates?

owner:     joe (commissioner); definition work by a Claude seat with Lean; validation by
           codex-22 per the excursion's dispatch note; witness ≠ author
status:    open
deliveries: none
```

## 2. Gate 0 over the goal sentence "a validated R5" — where each noun stands today

| noun | class today | what moves it to theory-defined |
|---|---|---|
| risk, ambiguity | theory-defined (formulas) | nothing — but their *arguments* below |
| **Outcome** | undefined for the WM | choose the carrier: the 14 flight dispositions (E-R5, T1456Z) or a coverage value; declared as a Lean type |
| **C** over outcomes | undefined ("a further stateable choice. Not made") | a preference distribution over Outcome, declared — with at least one outcome carrying zero preferred mass? (T1512Z's rule applied to C) |
| **Cascade** | prose-defined (§2.1d) | `Cascade := DAG Pattern` with authored edges and `acyclicDescent` — CascadeOrder.lean is generic in α; instantiate it |
| **Policy** | borrowed | the one sentence Joe must supply: policy = cascade / distribution over cascades / cascade + order |
| **Q(o∣π)** | theory-defined as an object; no WM instance conditioned on a cascade | the kernel: node → D(Outcome); cascade = Kleisli composition (E-R5 step 3); derived, per S-G3 |
| **G(π)** | naming conditions only (PolicyGrade) | risk + ambiguity over the composed kernel and C; the domain quantifier ∀ mission |
| coverage report | Lean-defined, no caller | an Outcome carrier + the per-flight close (E-R5's guarantee) |

Order of definition is forced by the table: **Outcome → C → Cascade (instantiate) → Policy (one sentence) → Q(o∣π) (compose) → G(π) → coverage**. Each step is refusable: if a step's noun cannot be defined on the theory's terms, the delivery is that statement and the record stays `open` (v2 §2.4).

## 2a. The Outcome carrier — Joe's proposal (2026-08-30), and what it fixes

> *"In terms of the outcome carrier, this should be something we think about across
> various nodes using that tetrahedron model. At the tetrahedron level we have outcomes in
> the form of money flowing; outcomes for people in terms of people learning or developing;
> outcomes for organizations in terms of organizations being built; and outcomes at the
> epistemic layer in terms of gathering more evidence in a suitable form. There's not one
> simple form of outcome that solves all problems for all things. But in an active-inference
> sense, outcomes should boil down to either actions or learning — updating the model.
> There's only two kinds of outcomes in active inference, unless I'm mistaken."*

**The precision, and it is small:** in AIF the outcome space is the observation space —
`o` ranges over whatever the sensors report — and what has exactly two kinds is the
**value** a policy earns from an outcome: **pragmatic** (the outcome lies near the preferred
outcomes C — the risk term) and **epistemic** (the outcome reduces uncertainty — the
ambiguity / information-gain term). The epistemic kind is itself usually split: information
about the current *state* (inference: which situation am I in) and information about the
*parameters* (learning proper: the model changes). So "action or learning" is exact read as
*what a policy achieves*; one observation typically carries both, and "learning" is one half
of the epistemic half. Nothing in the proposal has to change; the carrier does.

**What the two together fix — the shape of Outcome, before any vertex's space is chosen:**

```
Vertex   := people | money | organisations | evidence            -- the tetrahedron (placemat)
Obs v    : Type                                                   -- that vertex's observation space, stack-defined per vertex
Outcome  := Σ (v : Vertex), Obs v                                 -- a tagged sum: every outcome says which vertex it is an outcome AT
C v      : Dist (Obs v)                                           -- preferences PER PRAGMATIC vertex (people, money, organisations); NOT the evidence vertex — see §2a′
value    : Outcome → { pragmatic : ℝ, epistemic : { state : ℝ, param : ℝ } }
                                                                   -- the two kinds, uniform across vertices
```

Consequences: (i) **there is no single Outcome and there does not need to be** — the sum
type is theory-defined by shape even while each `Obs v` is stack-defined, which moves
"Outcome" from *undefined* to *theory-defined at the type level, blocked per vertex* in the
Gate-0 inventory (§2b) — a real change of class; (ii) **C is per vertex**, which is why no
single C could be stated for the War Machine (R5 worksheet: "not made"): the machine acts
at the organisations vertex (missions advanced, code built — the fourteen flight
dispositions are `Obs organisations`), learns at the epistemic vertex (a certification that a
pattern was useful or irrelevant is `Obs evidence`; a precision or structure update is a
*parameter* outcome), and has no money vertex (that is VSAT's); (iii) the placemat's
invariant axis reads directly as outcome kinds — I0/I1 are pragmatic outcomes at a vertex,
I2 is an epistemic-vertex outcome ("evidence reaches the people who need it"), I3 is a
parameter outcome ("feedback improves practice"); (iv) for R5, risk and ambiguity are now
computable **per vertex** and the question "faithful to what" gets a per-vertex answer —
which is the Sierpiński recursion at the evaluate stage; (v) the Snatch carrier
(treatment-indexed leaves, codex-22 Q6) is `Obs organisations` for a game — the same shape.

**Status:** Joe's proposal, refined by one sentence; his to confirm. If confirmed, step 1 of
§5 ("Outcome carrier + C declared") becomes: declare `Vertex`, declare `Obs v` for the two
vertices the War Machine touches (organisations: the fourteen dispositions; evidence: the
certification and update records), and declare `C organisations` and `C evidence`
separately — with the falsifier rule per vertex (some `Obs v` carries zero preferred mass).
`P-markov-category-spec` step 1 and `P-organise-the-library` §7's `Certification` both
consume the same declaration.

### 2a′. Non-degeneracy: the outcome space must include learning, or it is a greedy optimiser (Joe, 2026-08-30)

> *"If we're going to call this thing active inference, we can't just concentrate the entire
> outcome space on actions. We have to include some learning in the model, or we enter that
> degenerate case where we call it active inference, but it's just a greedy optimizer."*

**The requirement, stated so it can be refused.** An EFE with a pragmatic term only — or
with an epistemic term that never changes a choice — is a greedy optimiser wearing the
name. So the model must satisfy, on any policy set it is offered:

```
nonDegenerate (policies) :=
  (∃ π₁ π₂, pragmatic π₁ < pragmatic π₂ ∧ epistemic π₁ > epistemic π₂)      -- the two values can disagree
  ∧ (∃ π, argmin G ≠ argmin pragmatic)                                        -- and the disagreement decides at least once
```
The second conjunct is the **ablation falsifier**: remove the epistemic term and the
selection must change *somewhere*, or the term is decoration. It is the "winner-changing
ablation" `sec-limitations.tex` names as the only evidence of causal activity — and it is
what the War Machine failed: `:G-ambiguity` "influence MEASURED 0% flips / 674 ticks"
(`r18-badges.edn`). By this test the July machine *was* the degenerate case, whatever its
badges said. `nonDegenerate` joins the facade list of §1 as the refusal of *"pragmatic-only
under an AIF label"*, and the R5 evidence contract's falsifier (R5 worksheet §5) is this
conjunct.

**For the Snatch pilot, the learning outcomes already exist in the record.** S-001's policy
`probe-one-token` is explicitly epistemic — *"low pragmatic value, high epistemic value: it
buys a clean read on P2's disposition for the price of one token"* (`item-s001.edn:12–18`) —
and its epistemic outcome is the posterior over the hidden state: spread `log 2` before,
`0` after O2 or O4 (`score_item.clj`). So the Snatch carrier is a sum over two vertices:

```
Obs organisations := treatment-indexed flowchart leaves (payoff outcomes)        -- codex-22 Q6
Obs evidence      := { posterior over P2's disposition (epistemic-STATE),
                       Beta update on the acting pattern's edge (epistemic-PARAMETER) }
```
and `nonDegenerate` has its accepting witness in the paper's own table: grim trigger versus
`probe-one-token` disagree on the two values, and the pilot must show the argmin moving when
the epistemic term is ablated — the second conjunct — before it may be called an AIF pilot.

**A correction to §2a that this forces.** I wrote `C v : Dist (Obs v)` for every vertex.
That is right for pragmatic vertices (organisations, money, people) and **wrong for the
evidence vertex**: in the canonical decomposition the epistemic value is not a divergence
from a preference, it is the expected information gain — a functional of the kernel
(posterior against prior), with no C. Putting a preference on "having learned" would be the
move that turns learning into another pragmatic term, which is one more way to be greedy.
So: `C v` for the pragmatic vertices; the evidence vertex is valued by `epistemic`, and the
Markov-category record's "ambiguity is the entropy of the channel" is how that value is
computed. §2a's type is amended accordingly:

```
value : Outcome → { pragmatic : ℝ  -- KL against C v, for v a pragmatic vertex
                  , epistemic : { state : ℝ, param : ℝ } }   -- information gain; no C; the evidence vertex's value
```

**Witness (2026-08-30, B′):** `nonDegenerate` holds computationally in the Snatch microcosm
at the declared prior {sharer .10, snatcher .90} under G1/G3/G5 (argmin-risk = always-abstain,
argmin-G = {grim, probe-one-token}); it does not hold at any prior with sharer mass ≥ 1/6, nor
under G2 (no abstention, EIG range 0) or G4 (remedy makes offering dominant at every prior).
Record and table: `P-snatch-microcosm.md` §1b "B′ RESULT"; artefact `futon3/checks/ablation-snatch.edn`.

## 2b. Lean audit — which of these terms is defined today (checked 2026-08-30, all of `DarkTower/`)

*Joe: "let's make sure the terms we're talking about are all defined in Lean … do we have a definition of cascade now that we're happy with? A definition of Outcome? … keeping in mind we could change it later."*

| term | in Lean today | verdict |
|---|---|---|
| **Pattern** | no WM type. `CascadeOrder` uses per-attempt enumerations (`Attempt001Pattern`, `Attempt008Pattern`); `FirstFlightsExample.lean:124 inductive Pattern` is an example; `Patterns/DistributiveLaw.lean` is M-formal-patterns' sense | **absent** — needs `Pattern` as a type carrying at least an id and its `standsOn` targets |
| **Cascade** | no named type. `CascadeOrder.lean` defines `Reach`, `acyclicDescent`, `Below`, `IsMeet`, `hasMeets` over an abstract `r : α → α → Prop`, with two fixtures proved (one refused as a 2-cycle, one accepted) | **implicit only** — one structure away: `structure Cascade (P : Type) := (standsOn : P → P → Prop) (acyclic : acyclicDescent standsOn)`; then §2.1d is a type, and `hasMeets` is a measured property of a value, as §2.1d says |
| **Policy** | `PolicyGrade.Run (Action Score)` is a *finished run* (actions + score); `CommitmentTemperature.Selector := Temperature → List Entry → Option Action` is an operation over a ranked list, not over a cascade | **absent** — the closest object (`Selector`) has the right *shape* (an operation) and the wrong operand |
| **Outcome** | `CoverageReport.CriterionSet (Outcome : Type)` is parametric; its docstring names three intended carriers (7 Snatch leaves, 14 dispositions, maths outcomes) and declares none. Precedent for declaring one: eleven `inductive Outcome` across the Baldwin/CLean preregistrations | **absent for the WM** — the carrier is a decision (step 1) |
| **C** (preferences) | none. `PolicyGrade.lean:13`: "no probability, no preferences C, and no expected free energy" | **absent** |
| **Action** | three unrelated: `PolicyGrade.Action` (`:84`, the Snatch moves), `CommitmentTemperature.Action` (an id string), `MetaCATokamakExample.Action` | **defined thrice, unrelated** — a Gate-0 finding in itself |
| **History / Facts / Trajectory / run** | none | **absent** |
| **Q(o∣π)**, **G(π)** | none; `PolicyGrade` gives naming conditions only (`sustainedSingleAction`, `wiringSensitive`, `earnsPolicyGrade`) | **absent** — by design, until the above exist |
| **wiring** (policy as precedence) | `PolicyGrade.PatternWiring` (inductive) + `patternScoreUnder : PatternWiring → Int` — the S-G4 fixture | **the one place the "operation on a cascade" already has a type**, as a fixture |

So the answer to "do we have a definition of cascade we are happy with" is: **not yet a definition; the theorems that would constrain one exist and are generic.** The order of work in §2 stands, with one refinement from this audit: `Cascade` and `Pattern` can be declared *today* from §2.1d without waiting on Outcome or C, because `CascadeOrder` already carries their theorems; Outcome and C are the decisions.

## 3. How G, cascades and policy relate — the candidate, stated so it can be refused

**Row 0 answered (Joe, 2026-08-30) — a fourth option, not one of the three:**

> *"Different temperaments of players could be called policies. So, for example,
> the grim policy organizes the cascade into a kind of direction of play. But in
> a sense that move over cascades shouldn't be thought of as entirely distinct
> from the cascade itself. It's just that that's an operation on cascades,
> whereas cascades are operations on other things."*

So the levels are: **patterns are operations on the game state** (production
rules); **a cascade is an operation on patterns** (the authored dependency DAG —
which may fire, standing on what); **a policy is an operation on the cascade** —
the temperament that turns a DAG into a direction of play. The paper already
says this without naming it (`app-snatch.tex`, *The wiring carries the score*):
the same twelve patterns in a different precedence order move G4 from +3 to −5,
and *"the conflict-resolution order … is a commitment about the collection
rather than a property of any member of it, and there is nowhere in a pattern
to record it … the place for it is a collection-level record."* That
collection-level record is the policy.

Typed, as a candidate for refusal:

```
Pattern   : GameState → Option Action                 -- IF ∧ HOWEVER → THEN; a guard may not read this round's action
Cascade   := DAG Pattern                              -- authored "stands on" edges; acyclic; §2.1d
Policy    : Cascade → (History → Option Pattern)       -- an operation ON the cascade: a precedence order over its
                                                      --   linear extensions plus a reactive rule (grim: "after one
                                                      --   defection, abstain"); "temperament"
run       : Policy → Cascade → Facts → Trajectory      -- the forward relation (how_witness_snatch.clj's third layer)
Q(o∣π)    := distribution of run's terminal over Facts  -- DERIVED, never authored (score_item.clj; S-G3)
G(π)      := D_KL[Q(o∣π) ‖ C] + 𝔼 H[P(o∣s)]           -- with π the policy-applied-to-the-cascade, not the bag
```
Consequences that are checkable now: **S-G4 (wiring-sensitivity) is the statement
that G depends on the policy and not only on the cascade's membership** — the
re-wired column of Table `tab:snatch-scores` is its accepting witness, and a
hardcoded grim trigger "the collection cannot reach" is its refusing witness
(`grim_trigger_sharer_refused_by_sg2`). A policy that is the identity on the
cascade (fire in authored order) is still a policy — the degenerate case, S-G2's
territory. And the habit prior `E(π)` becomes a prior over *operations on
cascades*, which is a smaller and better-typed space than "over cascades".

The earlier candidate below (`Policy := Cascade`) is kept for the record and is
**withdrawn**: it identified the operation with its operand.



From the record (E-R5 five steps; claude-13 T1402Z/T1431Z; §2.1d; the glossary's *Policy π*):

```
Pattern     -- a production rule: IF ∧ HOWEVER → THEN                       (settled, 872bc95)
Cascade     := a finite DAG over Pattern, edges = "stands on" (@why / :descent), acyclic   (§2.1d)
kernel      : Pattern → Dist Outcome            -- what enacting this pattern makes observable;
                                                --   DERIVED from a checkable realization (@how + core.logic), never authored
compose     : Cascade → Dist Outcome            -- Kleisli composition of kernels along the DAG   (E-R5 step 3)
Policy      := Cascade                          -- CANDIDATE; the glossary says "composed from", the record does not settle it
Q(o∣π)      := compose π                        -- so Q is a consequence of the cascade, not a field on it
G(π)        := D_KL[Q(o∣π) ‖ C] + 𝔼 H[P(o∣s)]   -- the glossary's formula with π in place of a
```

What this makes checkable before any run: `Policy` refuses a `List Pattern`
(no edges); `compose` is undefined on a cyclic descent (4 of 25 generated
cascades — the corpus repair E-R6/`T-strategic-cascade-emits-disconnected-patterns`
comes first); `G` on a single action is `G` on a one-node cascade and must be
*named* as such (S-G2); `discriminates?` (E-R5 step 4) states whether `Q(o∣π)`
is constant across the cascades on offer — the inert-ambiguity finding, made a
predicate. If Joe's answer at "Policy :=" is *distribution over cascades*, `G`
becomes an expectation over that distribution and the habit prior `E(π)` gets
its canonical grain for free; if *cascade + order*, BV's `seq` types the order
(§2.1e). Those are the three choices and the record contains arguments for
none of them — which is why the sentence is the commissioner's.

## 3c. The Codex re-examination (codex-22, 2026-08-30, 3 minutes) — where §3 is corrected

Full answers with pointers: `P-validated-R5-snatch-reexamination.md` (297 lines;
two pointers spot-checked by claude-15 and exact). Six answers, one line each,
and what each does to §3:

| Q | codex-22's answer | effect on §3 |
|---|---|---|
| **Q1** policy = precedence, or + reactive rule? | *Neither is general enough.* Grim trigger reads history; the pattern-driven policy reads current antecedents + precedence. The common type must read an information state (state + history) and return an action; a cascade and precedence are inputs to **one implementation**, not to every policy. `item-s001.edn:12` already says it: *"π — a rule from history to action, not an action."* | **Policy is retyped**: `Policy : InformationState → Action` (or kernel); Joe's "operation on a cascade" survives as the **constructor** `Cascade → Precedence → Policy` for the pattern-driven family, not as the type |
| **Q2** Kleisli along the DAG? | *Not evidenced.* Runtime actors form a chain; `@why` is a reached authority graph; nothing shows precedence is a linear extension of it; `how_witness_snatch`'s `l/all` is conjunction inside one witness, not composition of kernels. The mission itself leaves firing-order vs authority-order open (`:1452–1462`). | **E-R5 step 3 is withdrawn as a claim** and kept as a proposal: "what would settle it — a runner semantics assigning a kernel to every acting pattern, typing states, defining branch/join, and requiring precedence to be a topological extension of a specified graph" |
| **Q3** what is C? | *Unspecified.* Scalar payoffs are scores, not a normalised preference distribution; risk needs a normalised C on the leaf carrier and ambiguity needs a likelihood/hidden-state model. | confirms step 1 as the first decision; Snatch does not supply it either |
| **Q4** zero-mass outcomes per policy? | *Determinable only for S-001*: **O1 and O3** (not O3 alone). For grim, pattern-driven, re-wired: **not determinable** — the paper gives scores for selected scenarios, not distributions over counterpart facts. Transferring S-001's zeros to grim would be incorrect. | the three paper policies are **unfalsifiable as recorded** — T1512Z's rule applied to the paper's own table |
| **Q5** a whole-cascade witness? | The one `@how` witness and `score_item` are **different relations**; whole-cascade composition is undefined because `@why`, `@how`, `@see-also` have distinct semantics. | the "composed witness" is not one step from the one-edge witness; it needs the runner semantics of Q2 |
| **Q6** its own typing | Pattern: partial production rule over an information state, metadata separate from the firing predicate. Cascade: the acyclic `@why` subgraph upward-closed from the acting patterns — *finite data, "neither the acting sequence nor an operation on patterns"*; `hasMeets` a measured property. Policy: information-state → action. Outcome: **treatment-indexed** terminal leaf, a tagged sum `(treatment, leaf)` — do not identify O-labels across treatments. Q: a pushforward of a declared distribution over facts/dispositions through dynamics and policy — and **S-001 states its Q and derives only the receipt**, so it is not yet an instance of derivation. G: typed only after Q, C and the likelihood terms exist. | two corrections to §3 beyond Q1: (i) **Outcome is treatment-indexed**; (ii) **"Q derived, never authored" was ahead of its artefact** — `score_item.clj:5–6`: "The item states Q." The derivation S-G3 demands does not exist yet, in Snatch or anywhere |

**Where codex-22 disagrees with Joe's sentence, verbatim:** *"I disagree with the
unrestricted sentence 'a policy is an operation on a cascade.' It accurately
describes a possible constructor for the pattern-driven policy, but not the
common policy type evidenced by grim trigger. It also risks reversing the
dependency in the current record: the run selects acting patterns, and their
upward closure determines the run's cascade."* That last point is the sharpest:
in §2.1d the cascade *of a run* is derived from what acted — so the cascade is
downstream of the policy's execution, not its operand. Both readings are on the
record now; the choice is Joe's, and the typing that accommodates both is:

```
Policy            : InformationState → Action                    -- the type (Q1, Q6)
cascadeBacked     : Cascade → Precedence → Policy                 -- Joe's "operation on a cascade", as a constructor
cascadeOf         : Run → Cascade                                 -- §2.1d: upward closure of what acted (Q6)
```
with the identity `cascadeOf (run (cascadeBacked c p)) ⊆ c` as the first theorem
worth wanting — that a cascade-backed policy's run reaches only what its
cascade stands on.

**What this use of the lifecycle showed** (logged in v2 §7, row 5): a reading
dispatched to a different agent, told to treat our framing as a hypothesis,
caught a substitution *in our framing* — "derived" written where the artefact
says "stated" — in three minutes, by pointers we could verify. That is the S5
different-method rule working on the commissioner's side of the record.

## 3d. Joe's reply to the retyping (2026-08-30): the cascade is not given — the policy finds it

> *"One of the things I don't like about this formulation is that it takes the
> cascade as given, whereas I would think a policy might involve going to find
> the correct cascade and then doing these operations on it … Informally: what's
> the problem? Here's the context; here's something I want; however, here's
> something else going on which I don't want. How am I going to resolve that? So
> you look around for design patterns that might help, because design patterns
> have that overall shape — they acknowledge these kinds of tensions — and you
> look for some cascade of design patterns that would work that tension through.
> That looking is still a bit of a policy, and it's very similar to the agent
> player temperament … I do like your type on the policy, moving from
> information state to action. However, the information state has to include the
> kind of global knowledge that's implied by a pattern repository."*

This accepts codex-22's type and rejects its reading of the operand. The
reconciliation: **the cascade is downstream of the policy because the policy
constructs it** — from the repository, against a tension — and then organises
and acts on it. Codex's "the run selects acting patterns, and their upward
closure determines the run's cascade" and Joe's "an operation on cascades" are
the same fact seen from the two ends of one function.

```
Repository       := { patterns : Set Pattern, standsOn : Pattern → Pattern → Prop }   -- the library with its authored edges
Tension          := { context : State, want : Prop, however : Prop }                    -- the IF ∧ HOWEVER of the PROBLEM, not of a pattern
InformationState := { state : State, history : List Event, repo : Repository, tension : Tension }
find             : Tension → Repository → Set Pattern       -- "looking around": which patterns acknowledge this tension
organise         : Set Pattern → Repository → Cascade       -- the DAG from authored edges; precedence = the temperament
act              : Cascade → History → Action               -- fire the first applicable THEN (the Snatch runner)
Policy           : InformationState → Action
patternDriven    := act ∘ organise ∘ find                   -- the cascade-backed constructor, now with `find` in it
cascadeOf (run π) ⊆ organise (find is.tension is.repo) is.repo   -- the theorem to want: a run reaches only what its policy built
```

Three consequences, each checkable against the record:

1. **Snatch under-exercises `find`.** Its repository is the fixed eighteen-pattern
   collection, so `find` was the identity on the twelve playing patterns and
   the whole of the worked example's policy content sat in `organise` (precedence)
   and `act` (grim's reactive rule). That is why codex-22, reading Snatch alone,
   could not see the cascade as the policy's product: in Snatch it never had to
   be found. **The transfer test to a coding example must exercise `find`, or it
   is not a transfer.**
2. **`find` is R6's red ring.** WR-19's demand — *"the select stage GENERATES a
   candidate that would move it … rather than re-ranking the menu it already
   has"* — is `find` stated as a requirement; the tension-proposer
   (`aif2/tension.clj`, "unbuilt" per `wr-overlay.edn:40`) is its intended
   implementation; and the glossary already assigns the job: the slush *"proposes
   diverse pattern compositions — cascades — for the selected mission,"* and
   R17‴ (pattern genesis) is what `find` does when it finds nothing. So Joe's
   "unless that's supplied by some other R-number node" has an answer: **it is
   R6, and R6 is red precisely because `find` is unbuilt.** The division Joe
   names — whether `find ∘ organise` is R6's product and the policy at R14/R16
   consumes it, or the policy encompasses all three — is the R6/R14 boundary,
   and it should be drawn once, in the prereg's edge table (R6 →rank→ … is the
   control edge that carries a cascade if R6 builds one, and a bag if it does
   not).
3. **The policy is pattern-shaped.** Joe's description of what a policy does —
   context, want, however, look for what acknowledges the tension, compose,
   act — is IF/HOWEVER/THEN applied one level up, over the repository instead
   of over the game. That is the Sierpiński reading of the lifecycle: the
   `problem` field of this record *is* a Tension, `find` over the stack's
   patterns is what MAP does, and `organise` is DERIVE. The mission lifecycle
   and the policy have the same type, at different scales, which is either a
   coincidence or the point.

**Status of §3's typing after this exchange:** `Policy : InformationState → Action`
(codex-22) with `InformationState` carrying the repository and the tension
(Joe); `patternDriven = act ∘ organise ∘ find` as the constructor; `Cascade`
as §2.1d, produced by `organise`, reached by `cascadeOf`. The earlier
`Policy : Cascade → …` is withdrawn. Open, and now precisely stated: the
R6/R14 boundary (which node owns `find ∘ organise`), and whether `find` is a
function or a kernel (the slush samples).

## 3d′. The R6/R14 boundary drawn (Joe, 2026-08-30): target first, then the cascade to match

> *"It would have to select the target first, and then construct the cascade to match —
> in order to fit the logic that I have in mind."*

So the order is **select → construct → act**, not construct → select → act. Typed
against §3d/§3e: the strategic selection picks a *target* (a mission, a hole, a
tension); that target's `Tension` is the input to `find`; `organise` builds the cascade
for it; `act` runs it. The policy's composition is unchanged — `act ∘ organise ∘ find`
— but the `Tension` it starts from is the *selected* one, and the cascade is
constructed after the choice, to match it:

```
select     : InformationState → Target                    -- strategic grain: R5 scores candidates, R14 commits
tensionOf  : Target → Tension
construct  : Tension → Repository → Cascade                -- = organise ∘ find, tactical grain (R13 scores the result)
act        : Cascade → History → Action                    -- R16 witnesses
policy     := act ∘ (λ t. construct (tensionOf t) repo) ∘ select
```
Consequences: (i) the candidate space R6-C is a space of **targets**, not of cascades —
its `find` is over missions/holes, the cascade's `find` is over patterns; two `find`s,
one contract shape (§3e), different repositories; (ii) R13 scores the *constructed*
cascade for the *chosen* target, which is what R15's "strategic selection fixes the
tactical target" already says; (iii) the figure's `R5 →rank→ R6 → R13 → R14` is
therefore mis-ordered for the catalogue's R6: the derived control path is
`R6-C → R5 → R14 (target) → R13 (cascade) → R16`, with R11's arbitration feeding R14's
choice of target rather than R16 directly — codex-22's reading, now the decision
(PREREG §2d). (iv) The theorem to want changes shape: not `cascadeOf (run π) ⊆ organise
(find …)` alone, but that the cascade a run reaches is the one constructed *for the
selected target* — `cascadeOf (run π) ⊆ construct (tensionOf (select is)) repo`.

## 3e. The contract shape for `find` and `organise` — laws, receipts, and swappable implementations (Joe, 2026-08-30)

> *"Some of these choices are options that could be swapped in and they'd all be
> conformant to one overall contract shape … is it the slush sample, or a
> function, or some other thing that hasn't been created yet? If we get the
> specification right, we can do it different ways. The way I have in mind as
> the primary method … is the futon3 library … the high-level type here is:
> take a collection of design patterns and organize them into a cascade. And
> where do you get the design patterns? That's the find function. That needs a
> specification as well, and it should be based on a search operation … If we
> had our design patterns organized in something like a cascade already, to get
> a cascade back out all we'd have to do would be to subselect, refine, and
> maybe fast-forward some of the edges that didn't fit our current problem."*

**The repository, measured 2026-08-30** (`futon3/library`, path-shaped targets
only, comments stripped): 1,239 flexiargs; **77 `@why` edges and 10 `@how`
edges, every one resolving to a file; 85 patterns in the authored graph
(6.9%); zero cycles.** So the repository *is* a DAG wherever it is organised
at all — and 93% of it is not organised. Both halves matter below: the
first makes `organise` definable today; the second is the work.

**Lean two-line count, 2026-08-30 (LH-D1b, `mathlib4/DarkTower/WarMachine/Holes.lean` @ `b98b2500`;
`scripts/count-holes.sh`):** P-validated-R5 declared-with-body 9 / declared-with-sorry 12 (the twelve
laws F1–F4, O1–O4, the ablation law, plus `C`, `find`, `organise` as implementations); P-R9 1 / 1;
P-R2 0 / 1; P-R8 1 / 1; delivery-lifecycle 3 / 0. Total 14 / 15. These declarations are the contact points
between the Lean and Clojure children of each node (charter, "Interfaces are Lean declarations"); a lane
closes when its hole moves.

### The interface — what any implementation must satisfy

```
Repository := { patterns : Set Pattern, standsOn : Pattern → Pattern → Prop, acyclic : acyclicDescent standsOn }
Tension    := { context : State, want : Prop, however : Prop }

find     : Tension → Repository → FindResult
FindResult := { selected : Set Pattern, receipts : Pattern → Receipt, absence : Option TypedAbsence }

organise : Set Pattern → Repository → Cascade
Cascade  := { nodes : Set Pattern, edges : Pattern → Pattern → Prop, acyclic : … }        -- §2.1d
```

**Laws on `find`** (each is a facade refused, v2 §2):
- F1 *containment*: `selected ⊆ repo.patterns` — no minting inside `find`; when nothing fits, `absence = some :no-pattern-addresses-this-tension` and R17‴ (genesis) is a *separate* step. (Refuses the silent `[]` — R6's "empty vs didn't run".)
- F2 *receipted*: every selected pattern carries **why it matched** — the tension clause it acknowledges (IF/HOWEVER overlap), the retrieval route (edge-walk / recall / sample), and an as-of. (Refuses the unattested candidate set; E-R6 Requirements A and B.)
- F3 *non-self-certifying*: a receipt cites the pattern's text or authored edges, never the finder's score alone; "embedding nearness is a generator of hypotheses, not proof" (glossary). (Refuses similarity-as-warrant.)
- F4 *falsifiable*: for a given tension there is at least one pattern in the repository the finder must *not* return (a zero-mass pattern — T1512Z applied to retrieval); a finder that can return anything for anything is unfalsifiable.

**F2 falsifier amendment (2026-08-31, C61):** `findF2Receipted` currently says
*“selected pattern lacks receipt.”* That falsifier cannot decide F2 while the runner
fires on a separately maintained abbreviated antecedent and the receipt cites the
authored clause: a receipt is present, but the check cannot establish that it
receipts what fired. `find-snatch` now rejects the 21 antecedent mismatches rather
than treating receipt presence as sufficient. The four `findF*` bindings remain
stale until the representations are reconciled and the strengthened check passes.

**Laws on `organise`**:
- O1 *nodes are the input*: `cascade.nodes = selected` (with up-closure under `standsOn` recorded as *added by organise*, not as found — §2.1d's "the cascade of a run is the up-closure").
- O2 *edges are authored*: `cascade.edges ⊆ Reach repo.standsOn` restricted to `nodes` — never inferred from similarity, co-occurrence or prose (§2.1d, "edges are authored; the sub-graph is derived").
- O3 *fast-forward*: for `u v ∈ nodes`, `edges u v ↔ ∃ path u ⇝ v in repo.standsOn passing only through patterns ∉ nodes` — Joe's "fast-forward the edges that didn't fit" is the restriction of reachability to the selected set, which is exactly `CascadeOrder.Reach` (`:17`) cut down to a subset. **This is definable today** against the 85-pattern graph, and it is acyclic by construction because the repository is.
- O4 *precedence is data*: the conflict-resolution order (the temperament) is recorded on the cascade as a collection-level field, not derived from the patterns (`app-snatch`, *The wiring carries the score*) — and S-G4 is the law that the score must depend on it.

### Conformant implementations — the point of writing laws

| implementation | `find` | `organise` | status | notes |
|---|---|---|---|---|
| **library search (primary)** | `futon3c.peripheral.memory-recall`: "bounded, pattern-conditioned recall … compact projections support candidate search … proposed pattern attachments fail closed: an agent-authored endpoint is a curation proposal, not yet a warrant" (`memory_recall.clj:1–7`); `M-memory-retrieval` PROPOSED 07-27 | O3 over the authored graph | search exists for *memories*; the pattern-repository instance is the work | F2/F3 are what "fail closed" already means there |
| **want-magnet + halo** (futon3a, `E-fold-engine`) | the `(have→want)` meme is the magnet; "you cannot produce a cascade without the magnet" | the cascade as the magnet's *correlation halo* (`cascade_construct.py`) | languished; the precedent for "assemble an initial set, then organise" | O2 is where it must be checked: a correlation halo infers edges |
| **GFlowNet slush** | samples compositions ∝ exp(β R̂) | the sample *is* a composition | offline; "beats greedy on diversity"; preregistered negative on success | `find` as a **kernel**, not a function — conformant iff receipts and F4 hold per sample |
| **hand-authored** (Snatch) | identity on the twelve | precedence by hand | the worked example | `find` trivial — why Snatch under-exercises it (§3d) |

The laws are what let these be swapped: a cascade produced by any of them is
the same type, carries the same receipts, and is refused by the same theorems.
Whether `find` is a function or a kernel is then an implementation's business,
as long as F1–F4 hold of what it returns — which is Joe's "we can do it
different ways" made into a sentence a checker can read.

### What this changes in the order of work (§2)

`Cascade` and `organise` (O1–O4) are definable **today** over the 85-pattern
graph, before Outcome or C — `CascadeOrder` already carries `Reach` and
`acyclicDescent`. `find`'s laws are statable today; its primary implementation
is a search over a repository that is 7% connected. So the outcome of this way
of working that Joe names — *"things aren't organized that way yet, but that
doesn't mean they couldn't be, as one of the outcomes"* — has a measure:
**the fraction of the library in the authored DAG, 6.9% on 2026-08-30**, and a
direction: every `@why`/`@how` edge added is `find`'s search space growing
and `organise`'s O3 having more to fast-forward through.

## 3b′. The Snatch pilot as microcosm — extension in progress (2026-08-30)

The re-examination (§3b–§3c) found the pilot far more built than this record assumed: the
one-edge Markov step is done (`how_kernel_snatch.clj`), a 411-line runner plays three
policies (`playout_snatch.clj`), the outcome carrier is declared per treatment
(`snatch-outcomes.edn`), and an *item* already carries π, prior, Q and a falsifier before
the outcome. What it lacks is Q *derived* by composition and the epistemic term with its
ablation — Joe's two things. The extension is `P-snatch-microcosm.md`: packet A (Q derived
by playout under a declared prior; acceptance = reproduces S-001's stated Q), packet B (the
epistemic term and the `nonDegenerate` ablation), packet C (the finite two-readings theorem).

## 3b. The method: re-examine the Snatch worked example, then test transfer to a coding example (Joe, 2026-08-30)

> *"I think we basically can get everything that we need to create the theory
> of this system by carefully re-examining that worked example, and then seeing
> if those ideas transfer to another example related to coding rather than to
> game theory."*

What exists to re-examine, all on disk and all from 08-27:

| layer | artefact | what it fixes |
|---|---|---|
| the game | `futon3/library/snatch/` — 6 institution patterns (G1–G5 + the family) | the Facts and the Outcome space (the flowchart leaves) |
| how people play | the 12 playing-within patterns; 9 authored edges (`@why`, `@how`, `@see-also`) | the Cascade |
| policies | grim trigger (hardcoded, unreachable) vs pattern-driven (antecedent holds → precedence → first THEN); the re-wired variant | Policy as an operation; S-G2/S-G4 |
| the derived Q | `futon3/checks/how_witness_snatch.clj` (attests one `@how` edge by a forward `core.logic` relation); `checks/score_item.clj` + `item-s001.edn` (Q over leaves, falsifier O3, posterior) | Q(o∣π) derived; the falsifier rule (T1512Z) |
| the paper | `p4ng/app-snatch.tex` §§ *Two policies over one collection*, *The wiring carries the score*, *The cascade is a semilattice, not a chain*, *What running it found that reading it did not* | the prose that already states the relation |
| Lean | `PolicyGrade.lean` fixtures: `grimTriggerSharer`, `grimTriggerSnatcher`, `patternDrivenSnatcher`, `patternScoreUnder : PatternWiring → Int` | the three polarities, on this example |

**The re-examination, as questions the record must answer (not a build):**
1. Is the precedence order the whole of the policy, or precedence + a reactive
   rule? (Grim needs history; the pattern-driven policy needs only the current
   antecedents. If both are policies, `Policy` reads `History`.)
2. Does `run` factor as Kleisli composition along the DAG (E-R5 step 3), or does
   the precedence order — a linear extension — collapse the DAG to a chain at
   run time, so that the semilattice matters only for *which* extensions are
   admissible? (`app-snatch` §*semilattice, not a chain* is where to look.)
3. What is C here? The game's stipulated payoff is a *score*, not a preference
   over outcomes — the R5 worksheet's "C over outcomes: not made" applies to
   Snatch too, and Snatch is where it is cheapest to make.
4. Which of the game's outcomes carries zero predicted mass under each policy?
   (`item-s001.edn` has one; a policy with none is not falsifiable — T1512Z.)
5. `how_witness_snatch` attests one edge. What does the *composed* witness over
   the whole cascade look like, and is it the same relation `score_item` reads?

**Transfer test — the coding example.** The claim "this is the theory of the
system" is only supported if the same five types instantiate on a case where
the patterns are `library/aif/*` or `library/process-coherence/*`, the game
state is a repository, the outcomes are the fourteen flight dispositions, and
the policy is what the WM's selector does to a cascade. The record does not
choose the case; it notes that the 21 registry cascades are "a hardcoded
four-edge chain" (`2ec8494`) — Snatch's grim trigger, in coding clothes — and
that the 25 generated ones are mostly not DAGs. Either the transfer test picks
one of the two flights on 07-17/07-18 (`empirics.tex` "the first live clicks",
traces on disk) or it waits for the corpus repair. That is a decision for after
the re-examination, not before it.

## 4. Validation, per node and for the whole system — the two apexes

**Per node (R5's own tetrahedron, v2 §0.9).** Nouns: the §2 table. Edges: the
R5 worksheet §4 (predict → risk/ambiguity → gCore → controller-score → rank;
criterion → coverage → close). Apex: `CoverageReport.lean` + `PolicyGrade.lean`
today; after §2, a module that states §3 with the three polarities — accepting
(a Snatch cascade with known dynamics earns G(π)), refusing-broken (a bag of ids
does not), refusing-plausible-fix (a one-node cascade renamed G(π) does not;
S-G2). Its flow-up is one clause in `ContractEmitter.lean`.

**For the whole system (the big apex, v2 §0.8).** The node's clause enters the
emitted contract; the trace checker (the mission's H3b — Lean judging a run,
verdict as exit code) reads the next flight's close and the next tick's
ranked list and returns 0 or not. E-R5's chain is the template: *Lean states →
emitter → Clojure validates a NEW flight → mutation test proves omission is
refused*. The evidence contract (R5 worksheet §5) is what the big apex holds
against R5: artefact kind, domain, corpus, method, falsifier — with the
falsifier that already fires today (ambiguity sd ≈ 0; every close
`:unwitnessable`) as the retro-trip.

**Behaviour of the overall system** is then not a separate exercise: it is the
conjunction, over nodes, of "the node's clause is in the contract" and "the
trace checker passes on the next run" — and the rule that no status surface
may say more than the checker returned. The 82 flights and the 791 ticks are
fixture material for the falsifiers, never the validation (E-R5; T1512Z).

## 5. The work, organised — all definition and writing until §1 is confirmed

| # | step | who | deliverable | refusal allowed |
|---|---|---|---|---|
| 0 | Joe confirms §1 and chooses at "Policy :=" | Joe | the sentence | — |
| 1 | Outcome carrier + C declared | Claude seat | `Outcome`, `C` as Lean types + the one-line rationale | yes: "no C is stateable over dispositions" is a legitimate result |
| 2 | Cascade instantiated | Claude seat | `CascadeOrder` at `α := Pattern`; refuses cyclic descent | corpus repair (23/25) is a prerequisite, not this step |
| 3 | kernel + compose | codex-22 (E-R5 S1–S3, already drafted) | `support` → `mass` → `kernel` → `readouts`, on `library/snatch/` first | yes: "the derived check is too expensive at one edge" (T1431Z's price test) |
| 4 | G(π) stated; three polarities | Claude seat | the R5 module, clause emitted | S-G1..S-G4 as theorems |
| 5 | coverage on the next flight | codex-22 | `007-closed.edn` carries a coverage statement; mutation test refuses omission | — |
| 6 | trace checker reads it | codex-22 | H3b, verdict = exit code | — |

Each row is one packet under the handoff protocol (one behaviour, one
acceptance test), dispatched only after the row above has a witnessed
delivery, and each packet's acceptance predicate is the row's cell in §1
`solved`, verbatim — not a test count.

## 6. What this record does not decide

Which of the three `Policy` readings is right (Joe's); whether Outcome is the
fourteen dispositions or something finer (step 1 may refuse both); whether the
derived check is affordable (step 3 may refuse). It records that the relation
in §3 is the only candidate on disk that makes G, cascade and policy three
different things with stated types — and that every previous attempt to
"define G over cascades" substituted a measurement (recall, curvature,
aliveness) for the definition, which is the facade this record exists to
refuse.

## 1b. Joe's response to the §1 gate (2026-09-02) — not confirm/rewrite but a small architecture project

> *"LJ1 points to more recent thinking about cascades, developed, indeed, with
> Snatch as an example, but it is not yet complete. This isn't necessarily a
> 'decision', but a small architecture project. In Snatch, there were 2 layers
> of policy: one defined by player temperament that built a cascade, and one
> defined by the cascade itself that organized play. If we included
> 'temperament' as a design pattern, it would all unify, and G would be defined
> over policy."* (Joe, 2026-09-02, morning)

Read against §"Row 0 answered" (:228-257) and the :347-363 exchange: the Aug-30
record keeps the levels stratified (an operation ON cascades is a different
kind of thing from a cascade). Today's direction reifies the top level into the
library: a temperament is itself a pattern — one whose THEN operates on
cascades and precedence, not on turkeys — so both of Snatch's policy layers
(temperament builds the cascade; the cascade organises play) are pattern-made,
and G(π) is defined over policy rather than over a bag of pattern ids.
The §1 problem statement is therefore to be RESTATED through this
unification, not confirmed as it stands. Worked as library-contract row LA1
(futon3/holes/labs/library-contract/worklist.edn); §5 packets stay gated on
Joe confirming the restated §1 (row LJ1).

**§1b addendum (Joe, 2026-09-02, second exchange, reported):** a *family* of
temperaments — plausibly all five as `@how` of one have-a-temperament pattern,
one pattern each, each corresponding to a policy for building a cascade and
thence a style of play. The production-rule interpretation of patterns is
important to this thinking and must be kept at every level. Pointers outside
Snatch are interesting; some temperaments learn in a war-room-like way, and
such outside patterns could set (hyper)parameters in the policy playout.
Reactive play seems policy-grained; where it sits among the current Snatch
patterns is not remembered — a discovery question, worked as LA1 slice (a).

**§1b addendum 2 (Joe, 2026-09-02, third exchange, reported):** cascade
construction needs a stopping rule — a temperament alone selects too little,
"all patterns" too much. Because there is a policy for building policies, G
should update *iteratively during construction* until the correct policy is
selected — very different from ranking individual actions. There may be a
preferential-attachment method for deciding whether a given pattern enters the
cascade and where it fits. The standing idea: the how/why landscape gives a
prior that can be subselected to quickly assemble a suitable local policy, and
the same landscape is a substrate for learning over multiple runs. Not built
yet.

**§1b addendum 3 (Joe, 2026-09-02, fourth exchange, reported):** at least a few
comparison domains from the start — Ants? ALFWorld? more later — "it would be a
pity if we built something that could only play one economics game." So the
restated architecture must be stated over an abstract domain interface
(Repository, Tension, playout oracle), with Snatch and Ants instantiated on
paper (Ants is nearly free: library/ants 5 patterns, pattern_authority_gate.clj
as oracle, and the untested @aif-delta channel lives there) and one external
benchmark (ALFWorld) named as a stretch row. Ants has a useful property as a
control: its authority gate FAILED every real pattern on held-out yield
(2026-07-16), so a constructor that only ever confirms would be caught there.

**§1b addendum 4 (Joe, 2026-09-02, fifth exchange, reported):** after the
snatch/ants/ALFWorld pilots, a GENERALISATION step: focused tests and trials at
the R5 node itself — running through a list of missions, excursions, tickets,
not to work on them, but only to see whether policies could be constructed
that would plausibly allow working on them. Construction-only trials over real
work items; the dark-mode discipline (construct, persist, judge — do not
enact), applied to the constructor itself.
