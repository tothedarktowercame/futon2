# R6 worked through the glossary — nouns, their Gate-0 class, and what a formal R6 would have to state

**Date:** 2026-08-30. **Author:** claude-15, from `facts-R6.md` (gathered 2026-08-30, spot-checked: 0 `:abstain` decisions in 791 records; decision types `{:address-sorry 459, :learn-action-class 146, :open-mission 96, :advance-mission 90}`; `:ranked-actions` 4–218; the live `:strategic-recommendation` branch at `policy.clj:383` returns before the abstain check; `compose-proposers` never calls `proposer-id`; `CandidateSpace.lean` absent; `ContractEmitter.lean:198` reserves family 9 `.designedUnbuilt`).
**Status:** worksheet, not specification. Format and classes as `R8-glossary-formalisation.md`.
**Sources:** `p4ng/sec-glossary.tex`; `p4ng/sec-catalog.tex:62–66, 237, 351`; `futon-aif-completeness.md:147–173`; `promotion-tests.edn:148–176`; `futon2/src/futon2/aif/{action_proposer,policy,pattern_registry}.clj`; `scripts/futon2/report/war_machine.clj:4362–4397`; `futon2/data/wm-trace/`; `data/wm-full-loop/` (07-15); `E-R6-red-ring-fill.md`.


> **Line numbers in `policy.clj` re-mapped 2026-08-30** after merging `origin/main` (16 commits of 08-20/21) into local `main` as `5471f91`; that merge shifted `policy.clj` by +11 lines. Pointers into `facts-R14.md` / `facts-R6.md` and the excursions are pre-merge and are left as dated.

---

## 0. First finding: the paper says "R6" means two things, and the ring is a third

The catalogue records the drift itself (`sec-catalog.tex:62–66`): *"At R6 the
contract means softmax selection with abstain, where the catalogue means the
candidate action space."*

| | completeness-contract R6 | catalogue R6 | the red ring (WR-19) |
|---|---|---|---|
| noun | **abstain**: "when the predictive distribution is too uncertain to discriminate among candidates, the agent declines to act" | **candidate space**: "a bounded, reason-bearing choice set" of patterns | **generation**: "the select stage GENERATES a candidate that would move [a demonstrated option] … rather than re-ranking the menu it already has" (`promotion-tests.edn:148`) |
| status claimed | "Satisfied as of v0.5"; pattern map `✓ Real.` | — | `wr-overlay.edn:40` `:holds false` — "tension-proposer unbuilt" |
| what the excursion found | not one mechanism but **four**, sharing one property: *"Nothing distinguishes 'the space is genuinely empty' from 'the generator did not run'"* | | |

Joe's fear as opened (`E-R6:4`): *"a selection over a small pre-ordained
whitelist, or a 'wired' component that just no-ops."* The excursion's answer:
"The fear is correct … The ring's own note describes none of them accurately."

## 1. R6's closure in the glossary, classified (Gate 0)

| glossary noun | what the glossary says | class | code · trace · Lean |
|---|---|---|---|
| **Softmax** (selection) | formula; "the temperature τ sets how decisively" | **theory-defined as a formula** | `policy.clj:82` · scores in `:decision` · `CommitmentTemperature` |
| **abstain** | **the word does not occur in the glossary** | **undefined** in the glossary; **stack-defined** in the completeness doc: fires when no candidates, or `:no-op` present and best is not `abstain-epsilon` below it | `policy.clj:363,390,419` · `{:action :abstain :reason …}` — **0 occurrences in 791 records** · none |
| **Pattern language / cascade** | "the operational name for a pattern language in use: a staged composition of patterns" | **borrowed name** (see R8 §1, and `M-formal-war-machine` §2.1d) | — |
| **Control states U / control schema** | "a design pattern is treated as an elementary or temporally extended control schema … The correspondence is functional rather than literal" | **borrowed name, by the paper's own statement** | the live candidate types are `:address-sorry :learn-action-class :open-mission :advance-mission` — scheduler actions, not patterns |
| **candidate / proposer** | (in *G*, *Embedding*, *Slush*): "action candidate or warrant"; no entry defines the candidate space | **undefined** as a noun; **stack-defined** as `compose-proposers = (mapcat propose) → distinct → vec` | `action_proposer.clj:63` · `:ranked-actions` · none |
| **GFlowNet slush** | "proposes diverse pattern compositions — cascades — for the selected mission"; offline: "beats a greedy baseline on diversity … information term does not steer" | **stack-defined, honestly bounded** — and its paper footnote records the preregistered negative | `slush-demo/` · not in `wm-trace` · none |
| **Embedding space** | "a generator of hypotheses … nearness alone is not proof" | **stack-defined, honestly bounded** | — |
| **gap-actions** / `:learn-action-class` | not in the glossary; completeness: "one `:learn-action-class` action for every action-type whose forward model can't propose" | **stack-defined**; 146 of 791 decisions | `action_proposer.clj:34` |
| **surveyedSpace** (family 9) | not in the glossary; excursion: "the ordering step consumes only a space in which every registered contributor is accounted for, each contribution carrying its input's as-of" | **theory-defined in prose**, **unbuilt** in Lean; reserved | · · `ContractEmitter.lean:198 "candidate-space-membership" .designedUnbuilt` |

**Gate-0 verdict for R6.** The selection formula is the theory's; every noun
that names *what is selected from* is either borrowed (pattern as control
schema, cascade as policy), undefined (candidate, abstain — in the glossary),
or stack-defined as three lines of `mapcat`. R6 is the node where the paper
itself wrote down the substitution ("functional rather than literal") and the
number kept its ✓.

## 2. What the corpus holds — checked 2026-08-30

- **Zero abstains in 791 records.** The four abstain trigger conditions are tested (`policy_test.clj:180–221`) on the `:actuation` path with fixtures. The live judge passes `:selection-boundary :strategic-recommendation` (`war_machine.clj:4476`, since `191e168` 2026-07-23), and that branch returns `chosen` before any abstain check (`policy.clj:383`). Pre-07-23 records also show zero abstains under the actuation path. So the abstain that the completeness doc "satisfied" is a branch the live loop cannot reach.
- **No record says which proposers ran.** `compose-proposers` writes no attestation; `proposer-id` is implemented by six proposers and called only from tests; it survives as a *stamp* on emitted items (`:s1/tension` 06-01..07-09, `:pattern-enumerator` 07-15..07-21). A proposer that emitted nothing leaves no trace at all.
- **07-15, the excursion's incident:** 24 attempts, 205 occurrences of "no addressable entities" across seven action classes, 22 `:learn-action-class` and 1 `:address-sorry` selections, and no selection file names a proposer.
- **`:policy-support-exclusions`** (31 records, 07-14 on): 124 exclusions, all `:mission-absent-from-capability-graph`, all tension-proposer `:open-mission` candidates — the one place a candidate's *removal* is recorded, and it records a feasibility mask, not a proposer's absence.

## 3. What a formal R6 would have to state — signatures, in dependency order

**Definable now:**

```
Candidate      : Type                                   -- today: {type, target, …}; a scheduler action (stack-defined, enumerable)
Proposer       := { id : Id, propose : State → List Candidate }
Attestation    := { proposer : Id, ran : Bool, inputRef : Ref, asOf : Time, emitted : Nat }
Space          := { candidates : List Candidate, attestations : List Attestation }
surveyedSpace  : Registry → Space → Prop
               -- ∀ p ∈ registry, ∃! a ∈ attestations, a.proposer = p.id      (Requirement A, E-R6:137)
               -- ∧ ∀ a, a.asOf is stated and a.ran → a.emitted = |candidates from p|   (Requirement B)
abstain        : List Scored → ε → Bool
               -- candidates = [] ∨ (noOp ∈ candidates ∧ best.score > noOp.score − ε)   [policy.clj:390]
reachable      : Selector → Prop
               -- ∃ input, select input = abstain                              (refused by the live boundary today)
```
Record contract, statable now: `∀ tick, surveyedSpace registry tick.space` —
refused by every record (no attestations exist). Refusing witnesses the
excursion names by date: **2026-06-03** (a curvature signal read as current
three months on — Requirement B) and **2026-07-15** (24 attempts, no
attestation — Requirement A). Plausible-fix refusal: adding an
`:attestations []` field that is always empty satisfies the shape and not the
property; the module standard's third polarity.

**Blocked:**

```
Candidate := Pattern ∣ Cascade        -- BLOCKED: borrowed names (U, cascade); the live Candidate is a scheduler action
generate  : Demonstrated → Candidate   -- WR-19's "GENERATES a candidate that would move it": no producer; tension-proposer unbuilt
```

## 4. R6's internal wiring as typed deliveries

| edge | from → to | payload | guarantee as built | the undeclared field |
|---|---|---|---|---|
| e1 | each of 5 proposers → `compose-proposers` | `List Candidate` | `mapcat`; an empty list is indistinguishable from a proposer that did not run | `receipt`: none — Requirement A |
| e2 | registry → proposer set | the five, as a literal vector in `war_machine.clj:4370` | inventory in code, not data | `from`: WR-20 says the inventory is data; unimplemented at this stratum |
| e3 | composer → `enrich` → `rank-actions` | candidates + structural pressure + mission value | no as-of on any input | `payload.asOf`: Requirement B (the 2026-06-03 curvature artefact) |
| e4 | ranker → `select-action` | ranked list | **live boundary returns before the abstain check** (`policy.clj:383`, since 07-23) | `guarantee`: the abstain branch is at-most-never on this edge |
| e5 | feasibility masks → candidates | `:policy-support-exclusions` | recorded (124, all one reason) | — the one receipt R6 has, and it is about removal by mask, not by absence |

The composition: a proposer that no-ops (e1) into an inventory nobody can see
(e2) into a ranker that reads stale inputs (e3) into a selector that cannot
abstain (e4). Each link is a two-line function. The seven weeks of
`:learn-action-class` at bit-identical scores that the paper's *Capability
Zones* pattern describes ("fourteen such candidates … the non-discrimination
tripwire refused the pick") is what this wiring produces when the space is
empty and nothing says so.

## 5. R6's evidence contract — draft for the apex

```
subject      R6-space (surveyed)                              R6-abstain (reachable)
claim        every registered proposer leaves an attestation   the live selector can decline to act
             per tick, with as-of
artefact     decision record with :attestations               a decision record with :action :abstain, produced on
             (one row per registered proposer)                 the boundary the live judge actually uses
domain       every tick; every proposer in the registry       every tick where no candidate beats :no-op by ε
corpus       wm-trace (reader loop)                           wm-trace + the selector's live boundary setting
method       count registered − attested per tick             count abstains; inspect the boundary branch
falsifier    a tick with a registered proposer and no          the abstain branch is unreachable from the live
             attestation row  ← every tick today               boundary  ← true since 07-23 (policy.clj:383)
not-evidence a proposer-id stamp on an emitted item (says     policy_test.clj's four abstain tests (fixtures on the
             who emitted, never who did not); a 200-entry      :actuation path); "Satisfied as of v0.5"; "✓ Real."
             ranked list offered as a surveyed space
```

## 6. Glossary → specification: the `Formal:` line each entry would carry

- *Softmax and controller calibration*: `Formal: softmax definable; abstain predicate definable; reachable(liveSelector) refused`.
- *Pattern language / cascade*, *Control states U*: `Formal: no referent — borrowed (the paper says "functional rather than literal")`.
- *GFlowNet slush*, *Embedding space*: `Formal: proposers, honestly bounded; no attestation`.
- **missing entries** — *abstain*, *candidate space*, *proposer attestation*: `Formal: surveyedSpace (E-R6:203) — stated in prose, reserved in the contract, unbuilt`.

## 7. What this worksheet does not do

It does not build `CandidateSpace.lean`; it records that its two refusing
witnesses already have dates and that its accepting witness needs one tick
with attestations, which no run has produced. It does not settle whether the
live boundary *should* be able to abstain at strategic grain — only that the
completeness doc's ✓ rests on a branch that grain cannot reach. And it records
the same shape as R8's whitelist: a bounded space presented as a survey of the
possible, with no receipt for what was never proposed.
