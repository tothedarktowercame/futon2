# R5 Snatch re-examination

**Date:** 2026-08-30  
**Reader:** codex-22  
**Repository commits read:** `futon2` `5471f91e9213f6806ea7bc92eb6151b90680912e`; `futon3` `f6ae9ccab69cca916fc0c140f383a8df12716d5d`; `p4ng` `492ec2af9b2f637090558a28b89b4beedede11e2`; `mathlib4` `27bad5170ce5a76d155e26ae9c8684d6f96d2293`.

This note answers from the checked artefacts. It treats the framing in
`P-validated-R5.md` as a hypothesis, not as evidence for itself.

## Q1. Is Policy precedence alone, or precedence plus a reactive rule?

### Answer

Neither description is general enough for both policies in the worked example.
Precedence is a component of the pattern-driven implementation; it is not the
whole type of `Policy`. Grim trigger is explicitly a history-sensitive rule and
the collection cannot reach it. The pattern-driven implementation filters by the
current antecedents and uses precedence to resolve simultaneous applicability.
The common interface therefore has to read an information state containing at
least the current game state and enough history for reactive policies, and return
an action (or a distribution over actions). A cascade and precedence may be
inputs captured by one implementation of that interface, but cannot be required
inputs of every policy without excluding grim trigger.

The open item makes the same typing choice directly: its policy is “a rule from
history to action,” not an action. `PolicyGrade.lean` does not settle the input
type: it records completed action sequences and scores, and its wiring family
only checks whether rewiring can change a score.

### Pointers

- `p4ng/app-snatch.tex:70-78` defines grim trigger from prior defection and says
  the collection cannot reach it; it separately defines pattern selection by
  antecedent, precedence, and actionable `THEN`.
- `p4ng/app-snatch.tex:117-126` calls precedence a collection-level
  conflict-resolution commitment absent from individual patterns.
- `futon3/checks/item-s001.edn:12-18` records a policy as a history-to-action
  rule.
- `mathlib4/DarkTower/WarMachine/PolicyGrade.lean:77-82` grades a finished run
  against a wiring-indexed score; `:91-114` supplies the hardcoded grim runs and
  `:135-148` the two pattern wirings. None defines policy execution.

### What would settle the remaining choice

An operational interface shared by both harness branches, for example
`Policy : InformationState -> Action` (or a stochastic kernel), with
`InformationState` explicitly stating whether it includes full history. A
cascade-backed constructor could then have the more specific type
`Cascade -> Precedence -> Policy`.

## Q2. Kleisli composition along the DAG, or a runtime chain?

### Answer

The artefacts establish neither Kleisli composition nor the claim that the
runtime precedence is a linear extension of the `@why` DAG.

One pattern acts per round, so the observed actor sequence is linear. But the
cascade is a different object: the upward closure of those actors in the
authored `@why` authority graph. Runtime precedence orders currently applicable
production rules. `@why` records what a pattern stands on, not what must become
true before it fires. Consequently the graph does not, in the present harness,
describe a sequence of transition kernels to compose. Nor is there evidence
that the hand-authored precedence respects a topological order. The mission
explicitly calls reconciliation between authority order and firing order open.

Thus the semilattice constrains/describes the reached authority structure. It
does not currently constrain admissible runtime linear extensions. “Work from
the bottom up” is proposed as a way to *derive* precedence from the DAG, not
reported as what the Snatch runner already does.

`how_witness_snatch.clj` also does not compose patterns. It joins fixed treatment
facts—action availability and guaranteed floors—to attest one claimed method
edge. Its `l/all` is conjunction inside that one witness, not Kleisli composition
of per-pattern outcome kernels.

### Pointers

- `p4ng/app-snatch.tex:128-135` separates the linear acting sequence from the
  reached `@why` subgraph; `:150-159` says authored edges plus run reachability
  determine its shape.
- `futon2/holes/missions/M-formal-war-machine.md:219-224` defines the cascade as
  a finite dependency DAG and a run's cascade as upward closure.
- `futon2/holes/missions/M-formal-war-machine.md:1441-1450` says bottom-up
  traversal would derive the currently hand-authored precedence.
- `futon2/holes/missions/M-formal-war-machine.md:1452-1462` distinguishes firing
  preconditions from authority edges and leaves their reconciliation open.
- `futon3/checks/how_witness_snatch.clj:38-58` shows the actual relation: two
  treatment rows, action availability, two floors, and a strict comparison.

### What would settle it

A runner semantics that (1) assigns a transition kernel to every acting
pattern, (2) types source and target state consistently, (3) defines branch and
join composition, and (4) requires the conflict-resolution order to be a
topological extension of a specified graph. None is present in the inspected
record.

## Q3. What is `C` for Snatch?

### Answer

Not determinable from the record. The game supplies scalar payoffs, not a
normalized preference distribution over terminal leaves. Indeed the library
README calls the payoff asymmetry “someone else's `C`,” which identifies its
provenance problem but does not construct `C`. `PolicyGrade.lean` explicitly
does not model preferences or expected free energy.

For `KL[Q(o|pi) || C]` to be computable, the record would have to declare:

1. one outcome carrier—either a common tagged union of treatment leaves or a
   separate carrier for each G1–G5 treatment;
2. a normalized `C` on that exact carrier;
3. whose preferences it represents; and
4. positive `C(o)` wherever `Q(o|pi) > 0`, unless infinite risk is intended.

One possible *new stipulation* would convert P1's leaf payoff `u(o)` into
`C_beta(o) = exp(beta u(o)) / Z`, but neither the exponential mapping nor
`beta` occurs in the artefacts. Treating raw scores as `C` would be ill-typed.
Moreover, the ambiguity term requires a likelihood/hidden-state model, not just
the marginal `Q` and `C`. Item S-001 declares a hidden-state prior, but only for
one G1 item.

### Pointers

- `futon3/library/snatch/README.md:11-17` states the payoff rule; `:57-63`
  refuses any conclusion about `G(pi)` and calls the incentive someone else's
  `C`.
- `p4ng/app-snatch.tex:83-97` reports scalar scores, not preferences.
- `futon3/checks/item-s001.edn:20-32` declares one hidden-state prior and one
  `Q`, but no `C`.
- `mathlib4/DarkTower/WarMachine/PolicyGrade.lean:13-16` excludes probability,
  preferences `C`, and expected free energy.
- `turn-2026-08-27T1456Z.txt:21` likewise says an outcome is not a reward and
  `C` over the terminal vocabulary remains a choice.

### What would settle it

A preregistered, normalized preference record over the chosen G1–G5 outcome
carrier, including actor, utility-to-probability mapping (if any), parameters,
and the generative likelihood needed for ambiguity.

## Q4. Which outcomes have zero predicted mass under each policy?

### Answer

Only Item S-001 is determinable, and it is not identical to any of the three
paper columns. For its G1 `probe-one-token` policy, `O1` and `O3` have zero
predicted mass; `O3` is the designated falsifier.

For grim trigger, the pattern-driven policy, and the re-wired policy, the
zero-mass sets are **not determinable from the record**. The paper gives
deterministic action sequences/scores for selected treatment-counterpart
scenarios, not a distribution over counterpart facts and terminal leaves.
`PolicyGrade.lean` repeats three measured sequences and scores but adds no
outcome distribution. A score table cannot establish which unobserved leaves
receive zero mass.

Therefore the record cannot determine whether those three policies satisfy the
T1512Z falsifiability rule. It would be incorrect to transfer S-001's `O1/O3`
zeros to grim trigger merely because S-001 also stops offering after a snatch:
S-001 specifies a one-token epistemic probe and its own prior.

### Pointers

- `futon3/checks/item-s001.edn:27-32` gives
  `{O1 0, O2 0.5, O3 0, O4 0.5}`; `:36-42` designates O3.
- `futon3/checks/score_item.clj:11-32` reads positive support from that stated
  map; `:34-45` reports O1 and O3 as the two zero-mass outcomes.
- `p4ng/app-snatch.tex:83-97` contains scores, not `Q` values.
- `mathlib4/DarkTower/WarMachine/PolicyGrade.lean:91-114` and `:141-148`
  contain actions/scores only.
- `turn-2026-08-27T1512Z.txt:22-28` states the zero-mass falsifiability rule.

### What would settle it

Before running each policy: a declared distribution over counterpart
dispositions and other exogenous facts, a deterministic or stochastic game
transition model, and a derivation that pushes those facts through each policy
to the treatment's terminal leaves.

## Q5. What would a whole-cascade witness be, and is it `score_item`'s relation?

### Answer

A whole-cascade behavioural witness would have to relate declared initial game
facts and histories, a policy/configuration, the round-by-round trajectory, the
acting patterns, and the terminal outcome. Separately, if every structural
claim is to be attested, it would need a witness appropriate to each authored
edge's type and a rule for composing those witnesses.

That is not the relation `score_item` reads. `how_witness_snatch` proves one
specific claim: removing abstention lowers P1's guaranteed floor. `score_item`
instead loads a hand-stated `Q`, tests whether one realized outcome lies in its
positive support or equals its named falsifier, and selects a posterior. It does
not read the flexiarg graph, the acting-pattern trace, or the `@how` witness.

Composition over “the 9 edges” is not well-defined as stated. The present
collection contains typed metadata with different meanings and directions:
`@why` authority, `@how` method, and symmetric/associative `@see-also`
cross-reference. For example, `protect-the-unprotected-move` names four methods
with `@how`, while those remedies point back to it with `@why`; treating both as
one untyped directed relation immediately confuses method expansion with
authority. The collection has also grown since the nine-edge measurement: the
current 18 files contain more metadata declarations. Counting all tags as one
graph would therefore be both semantically and historically wrong.

### Pointers

- `futon3/checks/how_witness_snatch.clj:2-16` limits itself to one `@how` edge;
  `:23-58` shows the treatment/floor relation it actually executes.
- `futon3/checks/score_item.clj:1-16` says the item states `Q` and defines
  support/falsifier checks; `:18-32` computes the receipt.
- `futon3/library/snatch/protect-the-unprotected-move.flexiarg:6-7` distinguishes
  its `@how` methods from its `@why` authority.
- `futon3/library/snatch/revert-then-invert.flexiarg:6-7` uses `@why` and
  `@see-also` for different relations.
- `turn-2026-08-27T1431Z.txt:3-18` records the one-edge state before the Snatch
  additions and assigns different meanings to the directive vocabulary;
  `:26-28` reports the then-nine structural edges and proposes only a one-edge
  check.

### What would settle it

A typed edge schema, one attestation relation per edge type, explicit
composition laws (including branch/join behavior), and an end-to-end runner
relation that derives rather than reads `Q`. The current single-edge witness is
evidence that one claim holds, not a definition of whole-cascade composition.

## Q6. My typing of Policy, Cascade, Outcome, `Q`, and `G`

### Answer

For the Snatch artefacts as they stand:

- **Pattern** is a named partial production rule over an information state:
  `(IF and HOWEVER)` guards an advisory `THEN`; it may yield no modeled action.
  Its `@why`, `@how`, and `@see-also` metadata are separate from its firing
  predicate.
- **Cascade** is finite data: the acyclic `@why` authority subgraph obtained by
  upward-closing the patterns that acted in one run. It is neither the acting
  sequence nor an operation on patterns. `hasMeets` is a measured property, not
  part of the object's type.
- **Policy** is an action-selection map (or kernel) from an information state
  containing current state and history. A pattern-driven policy is constructed
  from a collection/cascade-aware lookup plus a conflict-resolution precedence;
  grim trigger is a direct history-sensitive implementation.
- **Outcome** is a terminal flowchart leaf indexed by treatment. To compare
  across G1–G5 without conflation, use a dependent/tagged sum
  `(treatment, leaf-in-treatment)`. The artefacts do not justify identifying
  refined leaves across treatments merely because labels share an `O` prefix.
- **`Q(o|pi)`** is the pushforward of a declared distribution over exogenous
  facts/hidden dispositions through the game dynamics and policy. It must be
  derived for the chosen outcome carrier. Item S-001 currently *states* its
  numeric `Q` and derives only the subsequent support/falsifier receipt, so it
  is not yet an example of this full derivation.
- **`G(pi)`** can be typed as risk plus ambiguity only after `Q`, a normalized
  `C` on the same carrier, and the likelihood/hidden-state terms for ambiguity
  exist. No numeric Snatch `G(pi)` is determined by these artefacts.

I therefore disagree with the unrestricted sentence “a policy is an operation
on a cascade.” It accurately describes a possible constructor for the
pattern-driven policy, but not the common policy type evidenced by grim trigger.
It also risks reversing the dependency in the current record: the run selects
acting patterns, and their upward closure determines the run's cascade. The
paper does call different pattern wirings different policies, but that is an
identity claim about cascade-backed policies, not proof that every policy takes
a cascade as an operand.

### Pointers

- `p4ng/app-snatch.tex:39-61` gives the production-rule semantics and permits an
  advisory pattern to emit no modeled action.
- `p4ng/app-snatch.tex:70-78` exhibits the two different policy
  implementations.
- `p4ng/app-snatch.tex:106-126` shows precedence affecting policy identity and
  score; `:128-159` defines the reached authority graph separately.
- `futon2/holes/missions/M-formal-war-machine.md:219-246` defines cascade,
  acyclicity, and measured meet/overlap properties; `:248-255` says what it is
  not.
- `mathlib4/DarkTower/WarMachine/CascadeOrder.lean:17-32` types reach,
  acyclicity, and meets over an abstract relation but supplies no Snatch
  execution semantics.
- `turn-2026-08-27T1402Z.txt:10-20` identifies production rules as distinct
  from a generative model (its proposed successor-condition `Q` is explicitly
  bounded at `:20-26`).
- `futon3/checks/score_item.clj:5-6` exposes the remaining mismatch plainly:
  the comment calls the check derived, but “the item states Q.”

### What would settle the open parts

One executable semantics with explicit types for treatment-indexed states,
history, opponent dynamics, policy output, acting-pattern provenance, terminal
leaves, and probability. It should derive a `Q` for grim, pattern-driven, and
re-wired policies from the same declared fact distribution, then consume a
separately declared `C`. Until that exists, `G(pi)` is a well-motivated formula
with missing operands, not a computed property of the Snatch runs.
