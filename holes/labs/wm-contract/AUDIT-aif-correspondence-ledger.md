# AIF correspondence ledger — theory ↔ Lean ↔ Clojure ↔ live runtime

Opened 2026-09-18 by claude-12 (Lean-layer owner), under Joe's commission the
same day: "a complete audit of what is in the definitions of AIF and how they
correspond, if at all, to any implementations in Clojure ... whatever we
implement needs to both match the requirements of the AIF theory and the
requirements of this domain ... the only place where we can actually evaluate
that is in Lean. And there are some dozens of equations that need to be
checked in the same manner."

The precipitating example, in Joe's words: "G had been written down at one
time in a completely inappropriate way that did not match my mental model of
what it does. And we've reworked it now fairly completely. But what we
haven't done, to my knowledge, is checked that that reworking actually
matches AIF."

## Method: three legs per equation

Every row of `aif-equations.edn` (18 rows, plus 3 new categorical rows) must
clear three checks, each with a named executable artifact — an unexecuted
claim is a holder, not a verdict:

- **Leg A — theory ↔ Lean.** The Lean declaration is the equation the
  registry cites (Da Costa / Buckley / Friston / Parr), with every reduction
  declared. Ran 2026-09-16: `AUDIT-lean-aif-equations-2026-09-16.md`
  (codex-10, read-only, 3 MATCHES / 1 MATCHES-WITH-UNDECLARED-REDUCTION /
  11 DIVERGES / 3 CANNOT-TELL), then rebuilds + re-audits same day flipped
  all 18 to `:matches` (`:lean-reaudit` fields in the registry).
  **Standing caveat:** several re-audit verdicts were recorded by the agent
  that rebuilt the module (e.g. :expected-free-energy — rebuilt by claude-4,
  re-audited by claude-4). Author-≠-reviewer was the point of the 09-16
  audit's independence; those rows carry an asterisk until an independent
  eye confirms. Also: verdicts pin mathlib4 states now behind `darktower`
  HEAD; drift since 09-16 is unaudited.
- **Leg B — Lean ↔ current Clojure producer.** A MachineContracts-style
  entry binding the *production* function (by name, not file:line — Joe's
  ruling 2026-09-18), plus a behavioural fixture replaying Lean-proved
  values AND the falsifier through that function. Current state: contracts
  bind 10 of 169 `futon2.aif` namespaces, 25/37 entries point at the
  `cascade_model_manifest` re-implementation rather than production
  namespaces, holders predate the 2026-09-17 flat-selector removal, and 8
  file:line pointers are already stale.
- **Leg C — Lean ↔ live runtime data.** A witness over *captured run data*
  elaborated in Lean (`checks/*_witness.clj` → `lake env lean`), re-runnable,
  where staleness is a failure not a silence. Current state: the witness
  registry (~40 witnesses) is a 2026-09-08/12 snapshot; its pinned
  `Holes.lean` sha no longer matches; every admission predates the cascade
  rework; **no witness covers G over cascades at all**.

## Ledger

Verdict vocabulary per leg: OK (executable check exists, passed, current) /
STALE (passed once, inputs have since moved) / PARTIAL / OPEN / — (ruled
out of scope). "Producer" = what the live path calls today.

| Equation (`:id`) | Node | A theory↔Lean | B Lean↔producer | C live witness | Current producer / notes |
|---|---|---|---|---|---|
| :observe | R2 | OK* (09-16 reaudit) | STALE (model-transcription-only, old producer) | STALE (09-12) | rework not rebound |
| :prediction-error | R3a | OK* | STALE | STALE (reference-model witnesses 09-12) | Gaussian row legacy per ruling P9; categorical successor below |
| :precision | R7 | OK* | STALE | STALE | ζ (likelihood precision) declared 09-17, separate row below |
| :free-energy | — | OK (MATCHES 09-16, independent) | OPEN | STALE (VFE witness 09-12) | |
| :policy-free-energy | R8 | OK* | OPEN | STALE (s4-run admission 09-01) | |
| :belief-update | R3 | OK* | OPEN (ALIGNMENT waiting-row) | STALE | Gaussian legacy per P9; exact categorical update is the successor |
| :belief-state | R1 | OK* | STALE | STALE (production-trace rows 09-12) | |
| :forward-model | R4 | OK* | OPEN (waiting-row, PolicyRollout.predictedOutcome) | STALE (float-carried pins) | |
| :risk | R5a | OK (MATCHES-w-reduction, declared) | PARTIAL (fixture only) | OPEN | in horizon-g-sparse via stepRisk |
| :ambiguity | R5b | OK (MATCHES 09-16, independent) | PARTIAL (fixture only) | STALE (one hand point) | in horizon-g-sparse via stepAmbiguity |
| :expected-free-energy | R5 | OK* (rebuilt+reaudited by same agent) | PARTIAL — **the exemplar row, expanded below** | **OPEN — no witness** | `cascade-model-manifest/horizon-g-sparse`, LIVE since 09-17 |
| :policy-set | R6 | OK (limited claim, declared) | STALE (production-trace admission 09-12) | STALE | |
| :depth | R13 | OK* | STALE (machinery-capture 09-12) | STALE | effective-horizon wiring changed 09-09 |
| :temperature | R14 | OK* | STALE (trace row 09-04) | STALE | β now caller-DECLARED in select-action-cascades — post-dates all evidence |
| :policy-posterior | R6 | OK* | STALE | STALE | selection-posterior σ(ln E − F − G/β) is the current law; old admission binds prior producer |
| :action | R16 | OK* | STALE — 09-12 witness REFUTES old live selector | STALE | bayes-choice over first acting pattern is the current law; the refuting witness is about retired code |
| :dirichlet-accumulation | R17 | OK* | OPEN (waiting-row, DirichletLearning.accumulate) | STALE (3-tick IEEE 09-12) | |
| :model-reduction | R12 | OK* | OPEN | STALE (BMR witness) | |
| :state-prediction-error | R3a-cat | OPEN — new Lean row needed | OPEN | OPEN | categorical successor, eq. 4.13 |
| :state-belief-update | R3-cat | OPEN — new Lean row needed | OPEN | OPEN | exact categorical update |
| :likelihood-precision | R7-ζ | OK (2026-09-18: `AIF.Terms.temperedLikelihood`, book B.2.4, sum-to-one + ζ=1 recovery theorems) | OPEN (declared 09-17, fe55a1a0) | OPEN | Gibbs inverse temperature on A |

**PRIORITY REORDER (Joe, 2026-09-18, second ruling):** the theory layer
comes first; "there's no point in looking at the Clojure implementation"
until the terms and their types are down in Lean and the formalism is
declared. Legs B and C are deferred behind the term census. The census
module now exists: `mathlib4:DarkTower/AIF/Terms.lean` (darktower
`a528d14c74`, lake build green, zero sorries) — declared formalism (finite
discrete AIF; Markov categories considered and not adopted, upgrade path
recorded), A/B/D identified with audited carriers, **C defined**
(`Preference`, step-indexed log-preference family — T-C's definition half),
**E defined** (`Habit`, with `Habit.uniform` naming the E=1 reduction Joe
is dubious of), G and the policy posterior wrapped over census types. The
institutional half of C is recorded in the module as an OPEN typed
adjudication. Terms absent from the census are findings — the standing
example (claude-4's): G exists in Clojure but risk is never computed there
as its own term.

## Terms (fundamental AIF concepts and variables)

**Correction 2026-09-18 (Joe):** the first version of this ledger kept C off
the table as "a hole, not an equation." Wrong distinction. Lean needs a
definition for every fundamental AIF-approved concept and variable; C is
such a concept; it is a term, not an equation, but a term has a definition —
its type, its role in G, what makes a candidate admissible — and that
definition cannot be recorded as a hole. What stays under organized
discovery is C's *ruled content* (the masses), never its *definition*.

| Term | AIF role | Lean definition today | Status / next |
|---|---|---|---|
| A | observation/likelihood model | ForwardModel observation kernel (ProbabilityKernel since 480a666ad2); DirichletLearning over it | defined; Leg B/C per equation rows |
| B | transition model | ForwardModel transition; CascadeTransition.cascadeKernel (rowsum + blocked-identity theorems) | defined, cascade-structured |
| **C** | prior preference over outcomes (and, per Joe, process regulated by institutions — NOTE-joes-view-of-C §1) | **`Holes.lean:157`: `def C ... := sorry` — a type with no definition.** `horizonEFE` correctly takes C as a parameter, so the consuming shape exists | **T-C, the open definition task (mine):** replace the sorry'd global with an admissible-C definition — a structure stating what any C must be (per-PRAGMATIC-vertex, step-indexed family Cτ, log-preference entering stepRisk, provenance-carrying), with the census theorems reproven over it. The value stays parameterized and under discovery; the *concept* stops being a hole |
| D | prior over initial states | initial belief in ExactBeliefTrajectory — not independently audited this pass | survey with the :belief-state row |
| E | habit prior over policies | `habit` parameter in policyWeight/policyPosterior with sum/zero theorems | defined as parameter; Clojure passes constant 1 ("no habit prior exists yet" — a declared neutral input, should be stated in the contract entry) |
| β/γ, ζ | policy precision, likelihood precision | covered by :temperature and :likelihood-precision equation rows | see those rows |

## Book-verification pass (2026-09-18, per Joe's approved plan)

Census verified against the pinned book text (`refs/parr2022.txt`), module
`DarkTower/AIF/Terms.lean` at mathlib4 `fc5309ab59`/`f601c8aabf`, build
green, zero sorries. Findings the verification forced:

1. **C normalisation.** Book eq. 4.10 (`parr2022.txt:3764`):
   `P(oτ|C) = Cat(Cτ)` — canonical C is a normalised distribution per step.
   `Preference.IsCanonical` added. An unnormalised family shifts each
   step's risk by a policy-independent log-normaliser — rankings survive,
   G's value does not. The Clojure's log-preference spec (and its
   T·k·ln2 universe offset) must be re-read against this once Leg B
   resumes: is the offset exactly the log-normaliser accounting, declared?
2. **Quartet, not quintet.** Book base model = A/B/C/D
   (`parr2022.txt:3801`); base posterior `σ(−G−F)` (eq. 4.14) has no
   habit term. E is the *extension* (Friston 2016 eq. 7), recovered at
   `Habit.uniform` — which strengthens the case for treating the
   production E=1 as a declared reduction (Joe already dubious).
3. **F bound**: census `variationalFreeEnergy` identified with the audited
   `PolicyVariationalFreeEnergy` carrier (book B.1–B.2 / eq. 4.11).
4. **ζ defined**: `temperedLikelihood = A^ζ/Z(ζ)` (book B.2.4,
   `parr2022.txt:12738`, `:12779`), with `exists_likelihood_pos`,
   `temperedLikelihood_sum`, `temperedLikelihood_one`. The Terms-row gap
   for declared terms is closed; the *equation* rows for the categorical
   successors remain open.
5. **D audited**: consumed at t=0 by `ExactBeliefTrajectory.exactBeliefAt`.

The institutions adjudication is DONE (2026-09-18, third sitting):
`mathlib4:DarkTower/AIF/Institutions.lean` (darktower `7bd6e913de`, build
green, zero sorries), grounded in the Cascade Live apex thesis (pinned
2026-09-06). Verdict: **partial fit with a precise boundary** — IAD's
operational level fits inside the census (InstitutionalStatement with
typed deontics; norm-vs-rule typed by the sanction field; four landing
sites: policy boundary / E / C-via-sanction / guards); trajectory-grain
design requirements (apex clusters A/C) fit via Monitor augmentation
(bookkeeping automaton in state AND observation, conservativity theorems:
the institution observes, it does not change the physics), becoming
step-preference inside the book's G; collective-choice level is model
revision → the unruled :learning layer, OPEN; constitutional level does
not fit inside the model and correctly so — it is the operator's ruling
layer. Formal precondition surfaced: preference lives on observations, so
compliance must be OBSERVED to steer — cluster A ("records carry
warrant") is the enabling condition for all institutional steering.

Learning settled at the definition level (2026-09-18, fourth sitting;
Joe's ruling: learning is key AIF and cannot be a hole, same principle as
C): `mathlib4:DarkTower/AIF/Learning.lean` (darktower `c0909333c1`, build
green, zero sorries). Three timescales typed; `LearnedVariable` = the
2026-09-09 ruled design shape (evidence / update / schedule / persistent
state / next consumer) as a type; A-learning instantiated over the audited
Dirichlet carrier; E-learning defined (`Habit.accumulate`, never-weakens
lemma — decay is a ruling to take, not an accident); C-learning typed as
`PreferenceProposal` with deliberately NO adopt function (discovery
proposes, ruling fixes); `StructuralMove` types Joe's named targets — new
institutions (= learning's repository, per his computational-social-
creativity thesis), new patterns, pattern links, cascade reformation —
with BMR as the evaluator. Registry `:learning` stays unruled: the module
is the typed proposal for that ruling to accept, amend, or reject.

Remaining from the approved plan: the three categorical-successor
equation rows.

## The exemplar row worked: :expected-free-energy (G over cascades)

What exists, verified 2026-09-18:

- **Lean:** `PolicyHorizon.horizonEFE = Σₙ stepTerm`, with
  `stepTerm n = stepRisk n (C n) + stepAmbiguity n` — risk against Cₙ plus
  ambiguity, both read from the same predicted state; zero sorries; 48
  theorems across PolicyHorizon / OutcomeRiskKL / PolicySelection /
  ExactBeliefTrajectory. Structurally this *is* the AIF risk+ambiguity
  decomposition.
- **Clojure:** `cascade-model-manifest/horizon-g-sparse`, called from
  `efe/rank-cascade-actions`, selecting via
  `policy/select-action-cascades` — the production path since the
  2026-09-17 flat-selector removal.
- **Fixtures:** 71 tests / 738 assertions green (4 namespaces), including
  sparse-vs-enumerating equality and the Lean-fixture correspondence
  replay.

What "checked that the reworking actually matches AIF" concretely requires,
and does not yet exist:

1. **A3-independence (Leg A):** an independent re-audit of the rebuilt
   G chain (MachineExpectedFreeEnergyFidelity → PolicyHorizon), since the
   09-16 `:matches` verdict on this row was author-verified. Includes the
   declared reductions of the live shadow entry (zero adjudication rates,
   λ=μ=1, horizon 3 pending the AUTH-horizon-semantics ruling) being
   *stated in the contract* rather than absorbed silently.
2. **B-rebind (Leg B):** the three horizonEFE contract holders predate the
   flat-selector removal (two say notLivePath, one says liveShadow "never
   selecting") — false since 09-17. Rebind by name to
   `futon2.aif.cascade-model-manifest/horizon-g-sparse` +
   `futon2.aif.policy/select-action-cascades` with the falsifier inputs,
   coordinating the MachineContracts writer role with claude-7
   (ALIGNMENT.md).
3. **C-witness (Leg C):** a `HorizonGWitness` generator: take one captured
   production tick (the runner already records the G inputs and the enacted
   selection), emit a Lean file asserting `horizonEFE` over those inputs
   equals the recorded G within the declared float-correspondence bound
   (precedent: the row-12 IEEE-residual witnesses), elaborate with
   `lake env lean`, receipt. Re-runnable per run; stale = fail.

When 1–3 exist and pass, the G row is the template; the other rows close
"in the same manner", mostly by re-executing legs B and C against current
producers rather than new theory work — except the three categorical
successor rows, which need new Lean first.

## Standing rules for this ledger

- A verdict cites an executable artifact and its run date, or it is OPEN.
- Legs B and C bind by NAME (Joe's ruling 2026-09-18); no new file:line.
- Corrections are follow-up entries, dated; rows are never silently edited.
- The registry (`aif-equations.edn`) stays the per-equation source of truth
  for Leg A; this ledger tracks the two legs the registry does not yet
  carry, and proposes `:clojure-audit` / `:runtime-audit` keys for it once
  the shape has settled on the G row.
