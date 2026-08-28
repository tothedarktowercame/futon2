# M-formal-war-machine: a behavioural contract for the control loop

**Status:** CHARTERED 2026-08-25 (Joe + claude-13). Begins at IDENTIFY: the gap
is crisp, so `HEAD` is skipped per `futon4/holes/mission-lifecycle.md`.

**One-line:** Adapt the APM demonstration's Lean-to-Clojure specification chain
to the War Machine, so that the control loop's stages, transitions and
interfaces are stated once as a typed source of truth, emitted as a contract,
and checked against the running system rather than drawn in a figure.

**Sources**

- `mathlib4-apm-validation/DarkTower/APMCycleMachine.lean` and the chain below it
  — the worked precedent, running now
- `p4ng/aif-control-map-paper.svg` — the drawing this mission would replace as
  the authority for the wiring
- `p4ng/empirics-futon/wr-overlay.edn` — the constraint channel over it
- `futon2/src/futon2/aif/*.clj` — 61 namespaces, the thing to be checked
- `futon5/holes/M-formal-patterns.md` — the other formalisation mission; see
  "Relation to M-formal-patterns"

---

## 1. IDENTIFY

### 1.1 The gap

**The War Machine's architecture is drawn but not stated.** Figure 4 of
`futon-2026` carries eighteen R-nodes in five phased columns, ten control edges
forming one cycle, and eleven dashed support edges. Recovering that structure
on 2026-08-25 required matching Bezier path endpoints to node rectangles in the
SVG, because no file states it. `cascade-map.edn`, `r-wr-factoring.edn` and
`wr-overlay.edn` all reference R-nodes; none states an edge between two of them.

Three consequences follow, and each is checkable today:

1. **The interfaces are unnamed.** The eleven dashed edges are cross-column
   dependencies — R7 (evidence channel) supplying R3, R8 and R14; R9 (no
   self-certification) constraining R16; R20 and R12 both feeding R7. What each
   supplies, in what shape, is nowhere written.
2. **Nothing checks the drawing against the code.** `futon2/src/futon2/aif/`
   holds 61 namespaces, several named for nodes — `observation.clj`,
   `belief.clj`, `forward_model.clj`, `efe.clj`, `action_proposer.clj`,
   `precision.clj`, `enact.clj`, `c_vector.clj`. The correspondence is
   guessable and unverified. Editing the SVG changes the architecture of record
   with nothing to reject the change.
3. **Unmet constraints are prose.** Five of sixteen badges in `wr-overlay.edn`
   read `:holds false`, each a dated sentence. A sentence cannot be violated by
   a build.

### 1.2 Motivation — the discrepancy between ideal and actual

The stack already contains the machinery this mission wants, running, on a
neighbouring problem. `M-apm-demonstration` specifies its cycle in Lean, emits
it as JSON, validates it in Clojure, proves by mutation test that Clojure
rejects drift from Lean, and certifies by digest that the artifact being run is
the artifact that was qualified. Joe's assessment (2026-08-25): the Lean-to-
Clojure spec work has definitely helped it run better.

The War Machine, which is older and more central, has a picture and an EDN of
prose notes. The discrepancy is not that formal methods are unavailable — they
are in the building, in use, by the same operator, in the same Lean project.

### 1.3 Theoretical anchoring

- **The APM chain**, five links, each doing a specific job:
  `APMCycleMachine.lean` (phases, transitions, validity predicates, and
  theorems named after real incidents — `f25_reused_student_session_refused`,
  `f30_compiling_candidate_without_receipt_binding_is_refused`) ->
  `APMCycleContractEmitter.lean` (emits `phase-order`, `phases`, `transitions`,
  `bounds`, six policies, `receipt-schemas`, `submission-schemas`) ->
  `generated_contract.clj` (validates) -> `generated_contract_test.clj`
  (mutation tests reject drift) -> `qualification-report-v1.edn` (digest match,
  `non-vacuity`, per-category mutation coverage, residual hole tests).
- **WR-8**, typed files are sources of truth. The wiring currently fails it:
  the authority is a drawing.
- **WR-27**, a loop is born instrumented for its gain. This mission is the
  retroactive case — the loop was born in 2026 and the instrument is a picture.

### 1.4 The correspondence, as a hypothesis

APM's contract has a slot for each thing the control map has. This table is
the mission's first testable claim, not a finding:

| APM contract | War Machine |
|---|---|
| 11 phases | 5 stage columns; 18 R-nodes within them |
| `canonicalPhaseOrder` | PERCEIVE -> BELIEVE -> EVALUATE -> SELECT -> ACT |
| `transitions` | 10 control edges; 5 carry labels: observe, predict, rank, arbitrate, re-observe |
| the six policies | the 11 dashed support edges — cross-column constraints that are not transitions |
| `f25`/`f30` refusal theorems | each WR ruling's HOWEVER, as a case that must be refused |
| residual hole records | the 5 red rings |

### 1.5 Scope

**In.**

- Lift the R-node wiring out of SVG path coordinates into typed data, following
  the vocabulary `p4ng/empirics-futon/fig-loop.edn` already uses for a smaller
  figure: `{:from :to :label :kind :status}`.
- Make `gen_wr_overlay.bb` read edges from that data rather than recovering
  them from the drawing, so the figure is rendered from the source of truth
  instead of being it.
- A Lean model of stages, nodes, transitions and interfaces, with an emitter.
- Clojure validation of the emitted contract against `futon2.aif.*`, with
  mutation tests demonstrating that drift is rejected.
- A qualification step answering: is the loop that is running the loop that
  was specified?

**Out.**

- Proving anything about EFE numerics, scoring, or convergence.
- Signed-graph balance theory over pattern networks — that is
  `M-formal-patterns`, and it is frozen.
- Closing the five red rings. Each has an `@how` pattern in
  `futon3/library/problems/`; this mission makes them checkable, not closed.
- Reviving the daily `wm-full-loop` cadence, which ran from 2026-05-22 and
  stopped on 2026-07-14 (last outer-loop run 2026-07-27). Whether it returns is
  a fact the contract should be able to state, not a task here. Note this is
  *not* the machine being stopped: instrumented R11/R12/R15/R17 campaigns ran
  live on 2026-08-20 (`p4ng/empirics.tex`), including the first live R17
  execution.

### 1.6 Completion criteria

Testable, in order of increasing cost:

1. The wiring is data. `gen_wr_overlay.bb` renders Figure 4 from it, and a
   check rejects an SVG whose edges disagree with the data.
2. A Lean-emitted contract exists carrying stages, nodes, transitions and the
   eleven interfaces, with the same shape discipline APM's emitter uses.
3. Clojure loads that contract and mutation tests demonstrate it rejects drift
   — at minimum on transitions and on interface presence.
4. A qualification record reports `non-vacuity` with a positive witness count.
   A contract satisfied because nothing exercises it is the failure mode this
   mission is most exposed to, given (5).
5. The five unmet constraints appear as recorded, tested holes — in the shape
   of `hole-generated-receipt-schemas-v1.edn` — and not as silent green.

---

## 2. MAP

*Survey, not design. Opened 2026-08-26 (claude-13) after the
`E-R8-red-ring-fill` excursion returned findings that change scope.*

### 2.1 The requirement correspondence — the useful one

§1.4 maps the *shape* of the APM contract to the shape of the control map
(phases → stages, transitions → edges). This maps something different and more
actionable: **the 35 `def valid…` predicates in
`DarkTower/APMCycleMachine.lean` to War Machine requirements.** They form seven
families, and every family has a WM counterpart.

| # | family | APM instance | WM counterpart | status |
|---|---|---|---|---|
| 1 | identity threading | `validDispatch:108` — `activatedJobId = announcedJobId = reactivatedJobId = terminalJobId` | the **tick** threaded selection → enactment → outcome → γ | **broken, unobserved** (below) |
| 2 | non-empty evidence handle | digests/refs `≠ ""` | `:realized-score` must be a number, not `nil` | **broken** — `fold_realized.clj:163` |
| 3 | digest agreement across a boundary | `validStudentTerminalCandidate:240` — `receiptCandidateDigest = candidateDigest` | the SCALE-MATCH PIN: expected leg is the fold's own coverage-ΔG | **prose only** — a claude-10 must-fix enforced by care |
| 4 | ordering: durability precedes certification | `:239` — `persistedBeforeReceipt = true` | ⑨ folds only outcomes ㉞ durably wrote | **broken** — the 07-08 silence |
| 5 | provenance containment | `validControllerMemoryUse:326` — `surfacedIds.all (∈ accessibleIds ∨ ∈ searchReceiptIds)` | the grounded mission must lie in the producer's **declared domain** | **broken** — the four-entry whitelist |
| 6 | separation of powers | `validGuideSnapshotTransition:357` — `depositor ≠ reviewer` | ㉕/㉘ author ≠ reviewer | **holds** |
| 7 | exit / status pins | `leanExit = 0`, `worktreeClean = true` | ㉒'s act-gate: `cascade-score > 0` ∧ `coverage-score-delta < 0` | **holds** |

**What this does to "AIF faithfulness."** The mission's premise is that the WM's
only stated requirement is faithfulness to active inference — one global
property, and therefore apparently unformalisable. The table decomposes it into
seven checkable ones. **Families 5 and 7 already hold; 1–4 are where the WM has
been red all year**, and each is a predicate the APM machine already states in
another vocabulary. So the mission's work is substantially *translation*, not
invention.

### 2.1b The rosetta, extended to the implementation

*Added 2026-08-26 (claude-13), at Joe's direction: "our rosetta table can grow
now to include the actual implementation."* §2.1 maps APM predicates to WM
requirements at the level of **concepts**. This maps the same families to the
**Lean predicate that states them** and the **Clojure that implements them**, so
the correspondence is checkable rather than asserted.

**The step column is restored (2026-08-27).** §2.1 carried circled numbers for
families 4, 6 and 7; the 2026-08-26 extension added the Lean and Clojure columns
and dropped them, which lost the tie between the family vocabulary and the
thirty-four-step vocabulary — the very tie that says *where in the loop a clause
bites*. Restored and completed below. Assignments marked ° are inferred from the
step text rather than stated in §2.1 or an excursion, and are the ones to argue
with.

| # | family | steps | WM Lean predicate | stated in | implementation | R-node |
|---|---|---|---|---|---|---|
| 1 | identity threading | ⑯→㉒→㉖→⑨°, persisted ㉞ | `threadedIdentity` | `GainChain.lean` | γ dedups on `:tick` (`fold_realized.clj`) vs `(System/currentTimeMillis)` at `scripts/wm_scheduled_run.clj:108` — **two clocks** | R8/R14 |
| 2 | non-empty handle | ⑨° | `inhabitedHandle`, `typedAbsence` | `GainChain.lean` | `fold_realized.clj:163` — `(when (pos? bound) …)` returns bare `nil` | R8 |
| 3 | self-contained record | ⑨/㉒° | `selfContainedRecord` — **reserved, not yet defined** | — | `actuator_a3.clj:149` throws on any rejection; `fold_escrow/load-deposits` degrades | — |
| 4 | durability before certification | **⑨ folds only what ㉞ wrote** | `durableBeforeFold`, `loadYieldsOutcome`/`foldOf` | `GainChain.lean` | `enact.clj:255` catch-all around `close-loop!` | R8/R16 |
| 5 | provenance containment | ⑨/㉕° | `declaredDomain`, `domainNotNarrowed`, `dischargedPrecondition` | `GainChain.lean` | `fold_realized.clj:113` `reviewed-candidate-cleans` (4 entries); the named regime already exists at `actuator_a3.clj:395` (`:domain-mismatch`) | R8 |
| 6 | separation of powers | **㉕/㉘** | `separatedPowers` — **reserved** | — | ㉕/㉘; overlay R9 `:holds true` | R9 |
| 7 | exit / status pins | **㉒** | `pinnedExit` — **reserved** | — | ㉒ act-gate, `cascade-score > 0 ∧ coverage-score-delta < 0` | — |
| **8** | **temperature governs the action** | **⑯** | `governs`, `temperatureInvariant`, `gainAdvances` | `CommitmentTemperature.lean` | `policy.clj:35` (τ computed), `:238` and `:377` (argmax, τ-free), `war_machine.clj:4476,4527` | **R14** |
| **9** | **the ordering step consumes a surveyed space** | **⑪**° | *unstated — the R6 module's slot* | — | `full_loop_runner.clj:872` `repair-entry` (works, `##-Inf`); `portfolio_action_proposer.clj` (dark) | **R6** |

| **10** | **the cascade is a well-formed order** | **⑳**, gated at ㉒ | `acyclicDescent`, `hasMeets` — *dispatched 2026-08-27* | `CascadeOrder.lean` | `fold_semilattice.clj`; 4 of 25 constructions carry a cyclic `:descent` (§3.1g) | R6/R11 |

**Family 10 is new, 2026-08-27.** No existing family covers whether the cascade
the loop constructs is well formed: family 9 is about the *space consumed* at ⑪,
this is about the *structure produced* at ⑳. It exists because §3.1g found four
recorded constructions whose `:descent` relation contains a directed 2-cycle and
was folded anyway — the first family whose refusing-broken witness is a dated
record rather than a designed fixture.

### 2.1c The rosetta is not surjective, and that is where the work went

Families **8 and 9 have no APM source**. APM's controller advances through a
phase table; it does not rank policies, so it has no predicate about a selection
temperature and none about how a candidate set is populated. §2.1's translation
stopped at seven for that reason, not because seven was complete.

Both new families are in **SELECT**, and both are where 2026-08-26's work landed:
family 8 is `I(τ ; action) = 0` on the enacting path, family 9 is the difference
between changing a ranking's *weights* and changing its *membership*. So the
rosetta grows in two directions — a column (implementation) and rows (WM-native
requirements APM cannot state).

### 2.1d The earlier survey, and how stale it actually is

`futon2/holes/aif-r1-r16-pattern-map.md` (2026-07-13) is the predecessor of this
table: every R-number against its requirement, its `library/aif/` pattern, and an
honest status. Its legend already distinguishes ✓ real · ◐ starved · ◐→ built
(dark) · armed (latent) · ○ aspirational — a sharper vocabulary than anything
written since.

**It is stale by more than its legend.** Twelve `futon2/aif/*.clj` namespaces
were created after it: `repair_obligation` (2026-07-14, the working
stop-the-line memory), `epistemic_value`, `habit_prior`, `cascade_prior`,
`core_efe`, `delivery_qa`, `morning_brief`, `full_loop_runner`, `full_loop_cli`,
`memory_contract`, `capability_zones`, `mission_control_graph`. Several bear
directly on rows the map marks dark or open — `epistemic_value` on R5's "open
third", `habit_prior` on the one branch where τ can move an argmax.

**So the map understates what is built.** Refreshing it against the code is a
bounded, high-value job and the natural companion to the table above.

### 2.2 Ready vs missing

| ready — no new code | missing — the actual work |
|---|---|
| the 35 predicates, as a vocabulary to translate from | a WM-side statement of families 1–4 |
| `APMCycleContractEmitter.lean` — the Lean→JSON→Clojure chain, running | the WM's emitted artefact: **the producer-selection table** |
| `sec-system.tex` ①–㉞ — a protocol spine already written in prose | that spine as types |
| family 6 and 7, holding in the running system | the repairs behind families 2, 4, 5 (excursion slices 4 and 5) |
| a Mathlib-free build path — `APMCycleMachine → ExperimentalDesign → ExperimentPreregistration` elaborates with the Mathlib import stripped, as does `BV.lean` | nothing; cost is not a constraint (32 cores at ~7%) |

### 2.3 Surprises — recorded before DERIVE, per the lifecycle

1. **A test fixture became a production domain in 110 minutes** (2026-07-08).
   `reviewed-candidate-cleans` is a four-entry live-test map that
   `fold_realized.clj:113` reads as the set of groundable missions. Ticket:
   `futon3c/holes/tickets/T-fixture-becomes-registry-26082026.md`. This is what
   makes family 5 a repair rather than a formality.
2. **Durable evidence is pinned to a mutable tree.** 8 of 18 fold-turn deposits
   are dead because two flexiargs were edited on 08-15 and 08-23. Ticket:
   `T-evidence-pinned-to-mutable-prose-26082026.md`.
3. **Null results did not name their corpus** — three parties, same day,
   including R8's own promotion note. Ticket: `T-wm-wrong-corpus-26082026.md`.
4. **Family 1 is broken and nobody has hit it yet.** `fold_realized` states that
   γ dedups on `:tick`, but the tick in the realized-outcome record and the
   `(System/currentTimeMillis)` passed at `scripts/wm_scheduled_run.clj:108` are
   two clocks read at two moments. Found by reading `validDispatch`, not by
   chasing a symptom — which is the argument for doing the mapping before the
   modelling.
5. **The producer substitution of 2026-07-08 narrowed the domain**, and no
   commit message, docstring or excursion records that it did.
   `realized-outcome-of` works for any enacted decision;
   `realized-outcome-grounded` for four missions.

### 2.4 Excursions from this mission

**`futon3c/holes/excursions/E-R8-red-ring-fill.md`** is an excursion *from this
mission*, opened 2026-08-26 and dived into before MAP was written. That ordering
was accidental but productive: the excursion supplied every entry in §2.3, and
families 1, 2, 4 and 5 above are stated in terms of defects it found. The
excursion's slices 4 and 5 are the repairs behind families 2 and 5, and are in
series — neither alone produces a realized outcome.

**Consequence for module order.** The modular plan
(`p4ng/empirics-futon/NOTE-modular-formalisation-order.md`) makes module 1
R8 + R14 with a three-clause property. Those three clauses are families 4, 5 and
1 of the table above. Module 1 is therefore not a special case — it is the first
instantiation of a vocabulary that covers the whole loop.

## 3. DERIVE — the repair programme

*Opened 2026-08-27 at Joe's direction: "we've made a good modular discussion of
the red rings … I suggest that we focus now on these repairs. Our other ideas
about AIF-driven metadata for operator turns and so forth are really only useful
if we have a well-working WM to plug them into."*

### 3.1 The ordering principle, and why it is not arbitrary

Five excursions ran against five red rings on 2026-08-26/27. **In five cases out
of five, the ring's recorded reason was wrong or unverified** — R8's "no
producer" (there was one, armed 07-08), R14's "not yet located in the code"
(`policy.clj:35`), R6's "tension-proposer unbuilt" (`aif2/tension.clj`, live
since 06-01), R2's citation (the wrong observation vector) and its nag-0
inference (a gate blocked on unbuilt state), and R8's "dead since 07-14" (a
deliberate stop with a written re-arm condition).

The verdicts mostly survived. **The reporting did not.** So the repairs order
themselves:

> **(0) state it in Lean and align a Clojure specification to it, then
> (1) make the machine's own reports honest, before (2) changing what it does,
> before (3) adding capability.**

Tier 0 is the reference the other three are stated against; Joe, 2026-08-27.

This is not a preference. `CommitmentTemperature.lean`'s
`live_gain_repair_changes_no_action` proves that R8's repairs cannot move a
selected action while R14's edge is cut — so a Tier-3 repair landed today could
not be *evaluated*, because nothing would distinguish "it worked" from "it never
ran". That is the 2026-07-08 failure exactly, and Tier 1 is what makes Tier 3
checkable.

### 3.1b Tier 0 — the Lean/Clojure reference, and everything refers back to it

*Joe, 2026-08-27: "a Lean model with an aligned Clojure specification (like we
have for the APM cycle machine) would provide a much better reference for what's
actually true than just about anything else … this should be Tier 0 and
everything else needs to refer back to it."*

**Accepted, and it repairs a defect in the tiers as first written.** Tier 1 made
the machine's reports honest **once**. Nothing kept them honest. A contract with
mutation tests is what converts a one-time edit into a property the build
defends — WR-8's *typed files are sources of truth*, applied to this mission's own
output.

**What exists and what is missing.** The Lean half is started:
`DarkTower/WarMachine/GainChain.lean` (families 1, 2, 4, 5 + the chain property)
and `CommitmentTemperature.lean` (family 8). Absent: the emitter, the Clojure
validator, the mutation suite, the qualification record — four of APM's five
links.

**The validation target is the trace.** APM validates records against schemas;
our families are about behaviour over time, and the durable record of that
behaviour is `futon2/data/wm-trace/*.edn` — 52 files carrying `:tau`,
`:tau-mode`, `:selection-gain`, `:realized-outcome`, `:policy` per record. So the
emitted contract validates **trace records**, and non-vacuity is countable
directly: how many records exercise each clause.

#### Acceptance is against FIXTURES, not against the corpus

*Joe, 2026-08-27: "I'm less interested in an audit and rehashing of what happened
in July than I am in getting the Lean model built, the Clojure mirror of that
built, and seeing validation of the modules. The only use I can see for the
historical data in this setting is for the design of fixtures, but since the
historical data looks quite incomplete throughout the pipeline, I'd see it as
**indicative** rather than 1-to-1."*

**This supersedes the retro-trip bar drafted below.** Making the corpus the
acceptance criterion put historical completeness on the critical path, and the
corpus cannot carry it: 52 ticks, three realized outcomes, families 1 and 7 with
no witnesses at all. Validating against fixtures removes that dependency
entirely.

**The fixture discipline.**

- A **fixture** is a synthetic tick, or a short sequence of them, constructed to
  exercise exactly one clause — one that **accepts** and, per way the clause can
  fail, one that **refuses**.
- **History supplies shapes, not data**: field names (`[:realized-outcome
  :realized-G]`, `[:enactment :mission]`), value ranges (`:expected-G -0.2`,
  `:realized-G -0.5`), and *scenarios* worth encoding — a fold with a numeric
  leg, a fold with `nil`, a run where outcomes stop. Nothing is asserted about
  what actually happened in July.
- **Fixtures are versioned with the contract** and live beside it, so a clause
  and its witnesses move together.

**Consequence: the census does not shrink the model.** H1 found family 1
unwitnessed because the records carry no tick to compare the outcome's tick
against, and family 7 unwitnessed because every `:coverage-score-delta` is `nil`.
Under a fixture bar those are **findings about instrumentation** — Tier-1 repairs
(add a record-level tick; populate the gate fields) — and not reasons to drop a
family. **Family 1 is reinstated in H2.** A model that shrinks to fit a thin
corpus would encode the corpus's defects as its scope.

**What the historical census is still for.** Exactly one thing: telling fixture
design which shapes are real. That is why it was worth doing, and it is now done
— `holes/labs/wm-contract/`.

#### The retro-trip bar, superseded 2026-08-27 — kept for the reasoning only

**The contract must retro-trip on the known incidents.** WR-26 requires exactly
this of a tripwire before arming, and we have a dated incident corpus:

| date | what a correct contract must flag |
|---|---|
| **2026-07-09** | first trace after the producer substitution — `:realized-outcome` absent in every record thereafter |
| **2026-07-13** | `:tau-mode` → `:selection-gain-only`, so τ_eff = 1/g with g pinned at 1.0 — τ = 1.0 in every file from 07-15 to 07-21 |
| **2026-07-15** | 22 identical `:no-selection` closes with no repair obligation |

A contract that does not date those three from the trace corpus is a document,
not a contract. This is APM's `f25`/`f30` refusal-theorem discipline on our own
incidents, and it is the difference between "the model says X" and "the model
would have caught X".

#### What the contract will and will not cover — stated before it is built

**Record-shaped families validate.** 1 (identity threading — one tick across
selection/outcome/fold), 2 (a handle is present and typed), 4 (durable before
fold), 5 (the mission is in the producer's declared domain).

**Sensitivity-shaped families do not.** Family 8 (`governs`) is a claim about
counterfactual τ — *would a different temperature have chosen differently* — and
no record can witness it. Those stay Lean theorems about the code, not contract
clauses over the trace. Saying so now prevents the contract from being read as
covering more than it does, which §1.6's criterion 4 names as this mission's
worst exposure.

#### First increment, kept small

`WarMachineContractEmitter.lean` emitting the four record-shaped families —
each clause carrying its id, its predicate, its Clojure locus from §2.1b, and its
current holds-status — plus a Clojure validator that runs the clauses over
`data/wm-trace/` and reports per-clause witness counts. One file each. Mutation
tests and the qualification record follow, not in the same packet.

### 3.1c Handoff series — and a census finding that reorders it

*Joe, 2026-08-27: "start to develop handoffs for the Lean formalisation of the
more straightforward components."*

**First, a measurement that changes what Tier 0 can claim.** A key census over
`data/wm-trace/` (52 files, counting files carrying each key):

| key | files | family it would witness |
|---|---|---|
| `:tau`, `:decision` | **52** | — (family 8, the one that *cannot* be trace-validated) |
| `:tau-mode` | 11 | — |
| `:selection-gain`, `:author`, `:reviewer` | 7 | 6 (separated powers) |
| `:realized-outcome`, `:policy`, `:tick`, `:enactment`, `:act-gate-verdicts` | 5 | 1, 2, 4, 5 |
| `:cascade-score`, `:coverage-score-delta` | **1** | 7 (pinned exit) |

**So the families that hold today are the least witnessable, and family 7 has one
file.** The intuitive first move — demonstrate the Tier-0 chain end-to-end on a
property that already holds, so the pipeline is not entangled with a repair —
would have produced a **near-vacuous** contract. That is §1.6's criterion 4
arriving as a measurement rather than a worry, before any code was written.

Note also the irony: the only keys present across the whole corpus are `:tau` and
`:decision`, and `:tau` belongs to the one family that cannot be validated from a
record at all.

**What survives.** The 5 files carrying `:realized-outcome` are the enacting
window, and the corpus **does** contain the incident the contract must retro-trip
(2026-07-09). So Tier 0 is buildable — on single-digit witness counts, which the
qualification record must state rather than round up.

#### The series

*Dispatch note: send these to **codex-22**, not codex-18 — codex-18 is
coordinating another project (Joe, 2026-08-27).*

| # | packet | why this order |
|---|---|---|
| **H1** | **Per-clause witness census** over `data/wm-trace/`, *per record* not per file, for each of families 1, 2, 4, 5, 6, 7 | the numbers above are per-file and mine, from one sitting. Nothing should be built on them until they are per-record and independently produced |
| **H2** | `WarMachineContractEmitter.lean` — emit families **1, 2, 4, 5** as clauses, each carrying id, predicate, Clojure locus (§2.1b), holds-status | the best-witnessed group, and the one containing the retro-trip incident |
| **H3a** | `generated_wm_contract.clj` — validate the emitted **contract document** against pinned Clojure expectations | direction 1. Structure derived from Lean, values pinned both sides, so drift either way fails |
| **H3b** | `WarMachineTraceChecker.lean` — `main : List String → IO UInt32`, parses a trace projection, **verdict = exit code**; plus a bb step projecting `wm-trace/*.edn` into JSON | direction 2, and **where the 2026-07-09 retro-trip belongs**: a verdict about a run is Lean's, not Clojure's |
| **H4** | mutation tests — perturb each emitted clause, prove the Clojure rejects the drift | this is what makes the contract a gate rather than a document |
| **H5** | qualification record — digest match, non-vacuity with witness counts stated, residual holes for the unwitnessed families | families 3, 6, 7 land here as **recorded holes**, not silent greens |

#### H1 RESULT, 2026-08-27 — per-tick, after review

`holes/labs/wm-contract/` (codex-18 `753911a1`, corrected by review `1d64cdc`).
**The per-file estimate above and the census's first numbers were both wrong; a
trace file is one tick, and these are per-tick.**

| family | witnesses | of |
|---|---:|---:|
| 1 identity threading | **0** | 52 ticks |
| 2 non-empty handle | **3** | 52 |
| 4 durability before fold | **3** | 52 |
| 5 declared domain | **3** | 52 |
| 6 separation of powers | **7** | 52 |
| 7 exit / status pins | **0** | 52 |

`:realized-outcome` occurs in exactly three files — 07-03, 07-04, 07-05 — each a
single map. Family 1 is 0 because there is no record-level tick to compare the
outcome's tick against; family 7 is 0 because every `:coverage-score-delta` is
`nil`.

**Three consequences.**

1. ~~**H2's family selection changes from 1, 2, 4, 5 to 2, 4, 5.**~~
   **Reversed 2026-08-27** — under a fixture bar, an unwitnessed family is an
   instrumentation finding, not a scope reduction. Family 1 stays in H2; that the
   record carries no tick to compare against becomes a Tier-1 repair.
2. **Tier 0's claim changes from coverage to dating.** Three exercises will not
   support a non-vacuity argument by volume. But a **retro-trip needs a boundary,
   not volume**: three present and forty-nine absent dates the break exactly. The
   qualification record must say *"this contract dates the 2026-07-0x break"* and
   must not say *"this contract is exercised by the corpus."*
3. **The retro-trip target moves earlier.** The outcomes stop after **2026-07-05**,
   before the 07-08 substitution — not 07-09. Recorded as a discrepancy against
   `E-R8-red-ring-fill`, which cites **88** outcomes in this corpus where there
   are **three**, and dates the break to 07-09. That number is the one E-R8 used
   to overturn its own premise, so it must be settled before either is cited
   again.

**Corrected 2026-08-27 after surveying the precedent**
(`p4ng/empirics-futon/NOTE-apm-lean-clojure-strategy.md`). H3 as first written
fused APM's *two* directions and put run-verdicts in Clojure. APM's trace checker
says it plainly: *"The Clojure side emits observations, not verdicts."* Hence the
H3a/H3b split above. Both Lean sides are `lake env lean --run` — one prints JSON
to stdout, one takes a path and returns the verdict as an exit code.

**H1 gates everything after it.** If the per-record census shows fewer witnesses
than the per-file count suggests, H2's family selection changes. Splitting the
census from the emitter is the *"split discovery from implementation"* rule
applied to our own programme.

**Families 3, 6, 7 are deliberately not modules yet.** With 7, 7 and 1 files of
evidence they would be preservation properties with almost nothing to preserve
against. They belong in H5's residual-hole record until the corpus grows —
which is itself a reason to want the loop running again.

### 3.1d The five excursions as one build — a module standard, and three defects it catches

*Joe, 2026-08-27: "we'd now have a more definitive slice of our rosetta stone to
model formally — although we have developed a modular approach to the design,
maybe we should take a holistic approach to the build, so they align on a
standard."*

Reading the five red-ring excursions as a group, against what is actually on
disk, rather than each against its own excursion.

#### Where the build has got to

| ring | excursion | Lean module | vocabulary | in the emitted contract |
|---|---|---|---|---|
| **R8** | `E-R8-red-ring-fill` | `GainChain.lean` | **defines** families 1, 2, 4, 5 | yes — as four family entries |
| **R14** | `E-R14-red-ring-fill` | `CommitmentTemperature.lean` | **its own** (`governs`, `Selector`, `factorsThroughDiscard`) | **no** |
| **R5** | `E-R5-red-ring-fill` | `CoverageReport.lean` | **adapts** to families 2 and 5 | yes — `coverage-clause` |
| **R5** (G) | same | `PolicyGrade.lean` | **its own** (`Run`, `wiringSensitive`) | **no** |
| **R6** | `E-R6-red-ring-fill` | — | — | reserved, family 6 |
| **R2** | `E-R2-red-ring-fill` | — | — | no family assigned |

#### Three defects the group reading catches

**1 · Families 8 and 9 exist only in prose.** §2.1c names them — family 8 is
`I(τ ; action) = 0`, family 9 is weights-versus-membership — and neither appears
in `GainChain.lean`, in `ContractEmitter.lean`, or in `CommitmentTemperature.lean`
itself, which never mentions a family number. The emitter reserves 3, 6 and 7 and
stops. So the mission's rosetta and the emitted contract disagree about how many
families there are, and the disagreement is invisible from inside either one.

**2 · Two of four modules are islands.** `CoverageReport` does the alignment
properly: `criterionSelection` and `reportOccurrence` adapt a criterion report to
the *existing* `inhabitedHandle`, `typedAbsence` and `declaredDomain`, so it adds
no parallel notion of presence or absence. `CommitmentTemperature` and
`PolicyGrade` each define their own vocabulary and import nothing. For
`PolicyGrade` that was deliberate and stated in its packet; for
`CommitmentTemperature` it was not decided, it just happened.

**3 · The fixture polarities are followed and never named.** Three polarities
were agreed for fixtures — accepting, refusing-broken, refusing-plausible-fix.
Every module has all three, and no module says which theorem is which:

| module | accepting | refusing-broken | refusing-plausible-fix |
|---|---|---|---|
| `GainChain` | `gain_chain_sound_nonvacuous` | `two_clocks_break_threaded_identity` | `substitution_2026_07_08_narrows_domain_is_refused` |
| `CommitmentTemperature` | `habit_prior_governs` | `live_selector_does_not_govern` | `record_sensitivity_is_not_governance` |
| `CoverageReport` | `coverage_reported_nonvacuous` | `warm_customer_pays_…_is_refused` | `adding_a_channel_does_not_satisfy_coverage` |
| `PolicyGrade` | `pattern_driven_g4_snatcher_earns_policy_grade` | `grim_trigger_sharer_refused_by_sg2` | `grim_trigger_snatcher_passes_sg2_fails_sg4` |

The assignment above is mine, made by reading; it is not recorded anywhere the
build can check. A module missing its plausible-fix witness is the failure that
matters, because that witness is what stops the naive repair, and nothing would
currently notice its absence.

#### The alignment surface is the contract, not a single theorem

The tempting holistic move is one top-level `warMachineCompliant` conjoining
every module's property. **It would be false unification.** The modules do not
share a carrier: `CoverageReport` is parametric in an outcome type,
`PolicyGrade` quantifies over finished runs, `CommitmentTemperature` over
selectors, `GainChain` over fold occurrences. A conjunction across them would
either need a fabricated common type or would be a tuple wearing the word
"sound".

The object that legitimately holds them together is the one APM already uses:
**the emitted contract document**. Lean states each clause; the document names
every clause with its predicate, its witnesses and its Clojure locus; the Clojure
side emits observations against it; Lean judges the trace. Holism belongs at the
document.

#### And we may never want that theorem — not just not yet

*Joe, 2026-08-27: "I didn't mean one monolithic build, but 'one vocabulary' …
given the Deleuzian framing, we may never want a `warMachineCompliant` theorem!
Certainly not at the start."*

Accepted, and the reasons are stronger than the typing objection above.

**It is the verdict the apparatus issues to itself.** WR-0 requires of this
stack *"a verdict it cannot issue to itself — every claim the apparatus makes
about its own success must be movable only by evidence it did not manufacture."*
A theorem named `warMachineCompliant`, proved inside the machine's own
formalisation, is that claim in its purest form. The per-clause contract is not a
weaker version of it; it is the shape that keeps the property.

**It would be a dimension with no singularity.** A conjunction over every clause
takes two values and in practice one. True, it says nothing about which clause
carried it; false, nothing about which broke. That is R14's defect one level up —
a quantity computed, reported, and unable to change any decision — and the whole
discrimination programme says the information is in what fails.

**The rosetta grows, and a conjunction cannot.** §2.1c records that the table
grows in two directions, and families 8 and 9 arrived precisely because APM could
not state them. A conjunction over a growing list is either restated on every
addition or silently stale. The document absorbs growth; a theorem does not.

**A conjunction has nowhere to put a boundary.** This is the concrete
consequence, and it is now visible in the emitted document. R5 asks a criterion
set to name at least one outcome it does not cover; the contract's `reserved`
list does exactly that, and as of `06327f99b2` each entry carries a **typed**
reason — `wm-counterpart-is-prose-only` (family 3), `already-holds` (6 and 7),
`designed-unbuilt` (9). Those are three different things and the earlier flat
list said they were one. A top-level conjunction could not have carried any of
it: there is no field on a `Prop` for what it declines to cover.

#### What we want instead: end-to-end criteria over the operator loop

*Joe, 2026-08-27: "I think we need something considerably weaker than
`warMachineCompliant` — and this is where the 'closure over the operator'
matters. E.g. 'every operator turn is stored in the Evidence Landscape' is an
obvious one … But these can be chained: 'every operator turn is stored in the
Evidence Landscape, processed with Air, decorated with metadata, forming part of
a candidate cascade, and used in problem selection by the WM'."*

This is a **third kind of clause**, and neither of the two we have. A family
clause is about one record's internal consistency — identity threaded, outcome
durable, mission inside the declared domain. An end-to-end criterion is about a
**population**: does everything that entered the pipe come out the far end. It is
a conservation law, not a correctness theorem, which is exactly why it is weaker
and exactly why it is checkable.

Written as a chain, every prefix is itself a criterion, so the family is graded:

| | criterion |
|---|---|
| **C1** | every operator turn is stored in the Evidence Landscape |
| **C2** | C1, and processed with Air |
| **C3** | C2, and decorated with metadata |
| **C4** | C3, and forms part of a candidate cascade |
| **C5** | C4, and is used in problem selection by the WM |

#### Two things the form has to get right

**The boolean is uninformative; the drop point is the information.** "C5 holds"
is another dimension with no singularity — true, and it says nothing about which
stage carried it; false, and nothing about which stage lost the turns. What a
criterion must report is **where the population falls**, and each stage must
either pass an element on or record a *typed* reason for dropping it. That is
family 5's typed absence lifted from record grain to population grain, and it is
why this fits the standard's first clause rather than sitting beside it: it is
the same vocabulary at a different grain, not a second notion of absence.

**The denominator has to be external, and that is the point.** C1 asks what
fraction of operator turns reached the store — and the store cannot answer,
because the store is the numerator. Anything that never arrived is invisible from
inside. WR-0 requires *"a verdict it cannot issue to itself"*, and an end-to-end
criterion is one of the few properties in this mission that structurally has that
character: it can only be checked against a record the machine did not write.

#### C1 is not checkable today, and the reason is not missing data

Measured 2026-08-27 while drafting this, and the attempt is the finding.

- The external record for this session — the operator's own transcript — carries
  **26 operator turns** (231 `user`-typed records, of which 205 are tool results
  rather than turns).
- The Evidence Landscape carries **324** distinct `claude-13-turn-N` ids.

**The two numbers are in different units and nothing reconciles them.** The
transcript counts operator messages; the turn id counts *seat* turns, which
include the agent's. So C1's ratio cannot be formed at all — not because a
measurement is missing, but because no mapping exists between the external
record's unit and the store's key.

#### What the measurement found once it was built

`futon2/scripts/c1_turn_survival.py` (codex-22, `4e62e97a`; corrected at review,
`48bf098`). Its most useful output is the negative one: **there is no per-turn
key.** Session ids join (`sessionId` ↔ `:evidence/session-id`); the transcript's
`uuid` and `promptId` have zero hits in evidence; `:evidence/in-reply-to` is an
emacs transport id that does not appear in the transcript at all. So C1 is
computable only as a text-matched **lower bound**, and the field that would make
it authoritative is named: evidence must carry `:evidence/source-turn-id` equal
to the transcript record's `uuid`.

**CORRECTED 2026-08-27, same day.** The first reported figures — 23 operator
turns, 15 stored, 7 present only as a retrieval query, 1 absent — were mostly an
artefact of my own measurement. The evidence-text regex in
`c1_turn_survival.py` was anchored on `"}`, so it matched only entries where
`:text` is the last field of the body and silently dropped the rest. Those
dropped entries presented as turns missing from the store that were in it.
Unanchored, the current snapshot gives **19 operator turns, 19 matched, no
losses.** codex-22's independently produced `missing_chat_turns: 0` was right.

Three things survive the correction, and the third is the important one.

**CORRECTED AGAIN, and this one withdraws the loss.** The turn beginning *"Next
time please use codex-22…"* is stored, as `e-58bfdb92-…`, role `user`, turn-93.
The two "occurrences" I reported are one operator turn and one *assistant* turn
that quotes it — my own reply, caught by a role-blind substring search. **There
was no loss and there was no duplicate write.**

Every loss reported today was the instrument. Three extraction bugs, in
succession, each producing a plausible finding about the pipeline:

1. a `:text "…"` regex anchored on `"}`, dropping every entry where `:text` is
   not the body's last field;
2. a first-match event classifier that read a string-shaped `\"event\"` before
   a map-shaped `:event`, dropping **173 of 350** chat-turn entries;
3. a substring search across chat-turns that did not filter on role, so an
   assistant message quoting an operator turn counted as a second copy of it.

**The root cause is that the instrument was a regex over EDN.** The evidence API
serves JSON on `Accept: application/json`; the scraping was never necessary.
`c1_turn_survival.py` now parses (`90baa27`), and the whole class of bug goes
with it. Current reading: **20 operator turns, 20 matched, no losses.**

The lesson is the day's own subject matter turned on the measurer. An absence
produced by the instrument is indistinguishable, in the output, from an absence
in the world — which is exactly why a criterion has to type its losses, and why a
typed loss is only as good as the extractor that assigns the type.

**All chat-turn bodies are map-shaped.** Of 726 session entries, 177 are
`chat-turn` and every one is a map; the 1,986 string-shaped bodies are
`invoke-start`, `context-retrieval` and `invoke-complete`. A branch for
JSON-string chat-turn bodies handles a case that does not occur.

**The external denominator is not stable, and this is the finding.** The
transcript shrank from 1,968,337 to 1,945,752 bytes *during* the session, and the
operator-turn count fell from 23 to 19: compaction rewrites the record. C1's
denominator is supposed to be external precisely so the machine cannot mark its
own homework — but the harness rewrites it, and turns leave it. **A retrospective
C1 therefore self-heals: the losses vanish along with the evidence that they
happened.** The turn named above is only nameable because it was measured before
the rewrite. Either the external record is snapshotted at measurement time, or C1
is computed continuously; measured after the fact against a live transcript, it
cannot be trusted to find anything.


The later stages are worse off and should be typed as such rather than reported
as zero: Air processing, metadata decoration, cascade candidacy and WM problem
selection have no instrumented per-turn survival record at all. `sec-boundary`
already admits the adjacent case in the paper — *"the two denominators are
computed in the course of that join and discarded"* — which is this defect one
join away.

**What unifies is the vocabulary, and that is the whole of it.** Standard clause
1 is the unity: define a family or adapt to one, and introduce no second notion
of presence, typed absence, or declared domain. The parts stay separate and
share words. That is what a pattern language is, and it is the same structure the
Snatch measurement found — a semilattice is overlap *without* subsumption, and
the tree is the monolith.

#### The module standard

A Tier 0 module is admissible when all six hold. Five are already true of at
least one module, so this is mostly making practice checkable rather than new
work.

1. **One vocabulary.** The module either *defines* a requirement family or
   *adapts* to one already defined. It introduces no second notion of presence,
   typed absence, or declared domain. A module that genuinely needs its own
   vocabulary says so in its docstring and says why.
2. **Three polarities, named.** At least one accepting witness, one
   refusing-broken witness, one refusing-plausible-fix witness, each identified
   as such in the module docstring.
3. **Every declared field constrained** by some theorem. Three unconstrained
   items were caught in `GainChain` at review; the rule is what stops the fourth.
4. **In the contract, or explicitly out of it.** The module emits a clause in
   `ContractEmitter.lean`, or its docstring records why it does not. Silence is
   the current state and it is what let defect 1 persist.
5. **Axioms declared.** `#print axioms` on every theorem, no `sorryAx`, no
   Mathlib.
6. **A Clojure mirror, or a typed hole.** `CoverageReport` has
   `futon2/src/futon2/aif/coverage_check.clj`. Where no mirror exists the
   contract entry records the absence rather than omitting the clause.
7. **Predicate names are checked constants, and a named module is imported.**
   Added 2026-08-27 after the v2 emitter. Every `lean-predicate` and conjunct in
   the contract was a hand-copied string, so a rename inside a module would have
   drifted past the contract with the build still green. They are checked name
   literals now, which fails elaboration on an unknown constant — and that
   requires the emitter to import every module it names, because naming a module
   without importing it is the same defect one level up. The rule earned itself
   immediately: the policy-grade clause listed `scoreUnderObservedWiring` and
   `notSustainedSingleAction` as conjuncts and **neither is a declaration
   anywhere**, which no amount of reading had caught.

#### Build order that follows

1. **Complete the contract over what exists** — emit family 8 and the
   policy-grade clause, and reconcile the family numbering between §2.1c and the
   emitter. This is the alignment act; everything else is easier after it.
2. **Name the polarities** in the four existing modules.
3. **R6 (family 9, weights versus membership)** and **R2** — designed in their
   excursions, unbuilt. Building them *after* the standard is what tests whether
   the standard was worth writing.
4. **H3b**, the trace checker: Lean judges a run, verdict as exit code.

### 3.1e Operator behaviours, WM steps, and what each leaves behind

*Joe, 2026-08-27: "what exactly are the operator behaviours, and can the WM even
follow along the same track? … an obvious behaviour is typing into a REPL. That's
slightly different from a 'WM turn' because the WM is usually multiple agents
working across many of what I'd call turns. A WM tick or click is somewhat like an
operator turn, but it's also like a series of turns pegged to a Mission, with the
strict analogy Flight ≈ Mission."*

#### The grains do not line up, and that is the first thing to say

| unit | what it is | analogue |
|---|---|---|
| **operator turn** | one submission into the REPL | the atom |
| **agent turn** | one reply, with its tool calls | the atom, other side |
| **WM tick / click** | one pass of the control loop | *functionally* an operator turn — a decision and an act — but it spans **a series of turns**, so it is coarser |
| **Flight** | a run of ticks toward one end | **Mission** (Joe's strict analogy) |

A tick is therefore not a turn, and the mismatch is not cosmetic: end-to-end
criteria quantify over a population, and the population is a different set at
each grain. "Every operator turn is stored" and "every tick is stored" are not
the same shape of claim, because a tick has no boundary in the record until
something writes one.

#### What is persisted today — measured, not assumed

Session `66f62b84…`, 408 entries in the window, 185 with a JSON-readable body:

| behaviour | record | grain | pegged to a mission |
|---|---|---|---|
| operator types into the REPL | `chat-turn`, `role: user` — **75** | turn | yes, `clocked-mission` |
| agent replies | `chat-turn`, `role: assistant` — **69** | turn | yes |
| agent commits | `turn-commits` — **41** | turn | — |
| agent retrieves context | `context-retrieval` | turn | — |
| agent dispatches work | `invoke-start` / `invoke-complete` / `invoke-error` | dispatch | — |
| **a WM tick** | `wm-tick` / `wm-click` / `wm-cron` — **emitter built, disabled by default** | click | — |
| **a click's identity** | `click-id`, `attempt-id`, `phase`, `started-at` via `GET /api/alpha/wm/click` — **live now** | click | — |

**This answers Joe's open question — "I have no idea if they are persisted."
Agent turns are persisted**, in the same shape as operator turns, with a
`turn-id` (`claude-13-turn-N`) and a `clocked-mission` on 140 of 144. So the
operator/agent *pair* is already recorded at turn grain and already pegged to a
mission.

#### CORRECTION 2026-08-27 — the tick record is built and switched off, not absent

The first draft of this section said a tick leaves *nothing* and a Flight has *no
object of any kind*. **Both are wrong**, and Joe named the records I had not
searched. What is actually there:

| | |
|---|---|
| `futon2/src/futon2/aif/evidence_emit.clj` | *"Best-effort Evidence Landscape emitter for War Machine ticks. **Disabled by default.** Set `FUTON2_WM_EMIT_EVIDENCE` to 1/true/yes/on."* Emits `wm-tick`, `wm-click`, `wm-cron`. |
| `GET /api/alpha/wm/click` | **answers right now**: `{"running?":false,"click-id":null,"phase":null,"attempt-id":null,"started-at":null,"last-result":null}` — a click identity model with a phase and a start time |
| `futon3c/src/futon3c/wm/runner_service.clj` | single-flight service owning one click's lifecycle and its HTTP status projection |
| `futon2/scripts/wm_clicks_exhibit.bb` | *"THE visual surface for WM click / burn-in evidence"*, deterministic, over `data/wm-trace/*` |
| `p4ng/empirics/TRACE-061-cross-correlation.md` | one complete attempt cross-correlated against the paper's own ①–㉙ description — click `wm-click-f8569fae`, cohort 46, attempt-061, author claude-7, reviewer codex-7, repair-reviewer codex-1, with a phase log |

So a click **does** have a boundary in the record: a `click-id`, an `attempt-id`,
a `phase`, a `started-at`. And a run has a three-level identity — cohort,
attempt, click — which is more structure than the operator side carries, not
less.

**The correct finding is the one the red rings keep producing: built and switched
off.** The store has no `wm-tick` because an environment variable is unset, not
because a writer is missing. That is a materially different claim with a
materially different repair — flip a switch and verify what it writes, rather
than build an emitter.

**And the method failure is the same one twice in one day.** Earlier I reported
seven lost operator turns that were my own regex; here I reported an absent
record that a `grep -rl wm-click` would have found. Both times the instrument's
silence was read as the world's. The standing rule — *establish that the search
was exhausted before recording an absence* — was available and not applied. In
this case the tell was there to be noticed: "no record at all" is an
extraordinary claim about a system with a paper written around it.

#### The ①–㉞ key, and the column it is missing

`p4ng/empirics-futon/NOTE-thirtyfour-steps-both-levels.md` already walks all
thirty-four steps with a WM column and an operator column, which is the key Joe
is asking for. What it does not carry is a **third column: does the step leave a
trace.** The rule is short enough to state rather than tabulate thirty-four
times:

- A step has a trace **iff** some component writes an evidence event for it.
- On the operator side that is ④ (the conversation is the previous trace), ㉕–㉙
  (dispatch, resolve, cure, review, amend — all `invoke-*`), ㉚ (`turn-commits`),
  and ㉞ (`chat-turn` with session and turn ids).
- On the WM side the trace goes to `data/wm-trace/*.edn` (gitignored, last
  written 2026-07-21) **and**, when `FUTON2_WM_EMIT_EVIDENCE` is set, to the
  Evidence Landscape as `wm-tick` / `wm-click` / `wm-cron`. The per-step mapping
  already exists once, by hand, in `p4ng/empirics/TRACE-061-cross-correlation.md`
  — which is the model for what the third column should contain, and evidence
  that the mapping is producible rather than hypothetical.

So the honest third column, today, is: the operator half is traced per step into
a queryable store; the WM half is traced per *day file* into an ignored
directory. Filling the column properly is work for after that ticket, because
until a tick has an id there is nothing for a per-step trace to attach to.

#### One limit found while measuring, which belongs to the ticket

Of 408 entries in the window, **185 have a map body a JSON client can read and
223 are EDN maps rendered as strings** — `{"prompt-preview" "…"}`, keys and
values space-separated, which is not JSON. The envelope is queryable; the body is
not uniformly. Every `invoke-*` and `context-retrieval` event is in the
unreadable half, which is exactly the half a WM-side criterion would need.

This tripped the measurement twice in one afternoon and is a sharper version of
the ticket's *queryable* row: the store is not simply "less queryable than the
operator record", it is **queryable at the envelope and not at the body**, and
the split runs straight through the events the machine writes about itself.

### 3.1f Written down is not the same as right

*Joe, 2026-08-27: "lots of features have been built and lots of guarantees
offered, and still the machine is failing left and right, because the guarantees
turn out to be incomplete — e.g. validating data shape but not contracts between
steps … what we've seen when we investigated the red ring nodes is a bunch of
'cheating to get something working' rather than building to a contract; it's not
clear the contract was ever written down at all, other than 'be faithful to AIF'
… the fact that things were written down is better than not, but we have no
guarantee at all that they were the right things."*

#### The APM diagnosis, checked and made precise

`DarkTower/APMCycleMachine.lean` has 42 `valid…` predicates, and `validDispatch`
is the diagnosis in miniature. It threads five lifecycle stages:

    observation.activatedJobId   = observation.announcedJobId ∧
    observation.reactivatedJobId = observation.announcedJobId ∧
    observation.terminalJobId    = observation.announcedJobId ∧
    observation.resumedJobId     = observation.announcedJobId

That **is** a contract between steps — announce, activate, reactivate, terminate,
resume. But it holds inside a single `DispatchObservation`, so it is checked only
because someone built a record that carries all five ends at once. **The
step-contract is smuggled into a record shape.** Where the two ends of a
relation live in different records, nothing relates them, and the machine passes
its shape validation while the step-to-step guarantee silently does not apply.

That is the sharpest statement of why end-to-end criteria (§3.1d) are a third
kind of clause rather than more families. A family clause asks whether one record
is internally consistent; an end-to-end criterion asks whether a population
crossed a boundary between records. APM's months of shape validation, and its
continuing failures, are the empirical case for the distinction — made at cost,
by someone else's machine, which is the best kind of evidence we have here.

#### The standard is about form, and form is not the thing

All seven clauses of the module standard (§3.1d) are formal: one vocabulary,
three polarities, constrained fields, in the contract, axioms declared, a mirror
or a typed hole, checked predicate names. **Not one of them tests whether a
clause states the right requirement.** A module can satisfy all seven and
formalise something nobody needed.

This is not hypothetical, and the day's own record is the evidence. Three
findings were written up here, committed, and wrong: seven lost operator turns
that were an anchored regex; a duplicate write that was my own reply quoting
Joe; a WM tick record reported absent that a `grep -rl wm-click` would have
found. Each was well-formed. Each was reviewed. **In the commit log a wrong
finding and a right one are indistinguishable** — which is Joe's point stated as
a property of this repository rather than as a worry about it.

#### What would give a clause standing: measured, and mostly absent

A clause earns its place by refusing something that actually happened. That is
what the refusing-broken witness is for — but a witness can be an incident or an
invention, and nothing currently records which. Checked across the four modules:

| module | refusing-broken witness anchored to a dated incident? |
|---|---|
| `GainChain` | **yes** — *"the 2026-07-08 substitution loses an enacted mission from the domain"*; *"the 2026-07-08 chain collapsed into one silence"* |
| `CommitmentTemperature` | no |
| `CoverageReport` | no — though `warm_customer_pays` is drawn from R5's finding, the module does not say so |
| `PolicyGrade` | no — its witnesses are chosen runs of an external game |

One of four. So a candidate **eighth clause**, and it has teeth precisely because
three modules currently fail it:

> **A module's refusing-broken witness names a dated event in the record.** Not
> a plausible failure; a failure that happened, with the date it happened on.
> Where no such event exists, the module says so, and the clause is understood as
> conjectural until one does.

`GainChain` shows the shape without being asked to. Its theorem names carry the
date, so the requirement can be traced to the day it was earned.

#### And the honest limit on all of it

An eighth clause makes each clause *earned*; it does not make the set
*complete*. Nothing above rules out a contract every one of whose clauses refuses
a real incident and which still misses the failure that matters next — which is
R5's coverage requirement turned on the contract itself. The `reserved` list with
its typed `outside-reason` entries is the beginning of that and not the end of
it: it records four requirements known to be outside, and says nothing about
requirements nobody has thought of.

"Be faithful to AIF" failed as a contract because it named no refusable
condition. A contract of clauses each refusing a dated incident is a large
improvement and still not a guarantee, and the difference should be stated in the
paper rather than discovered by a reader.

### 3.1g The flight has the right structures; nothing checks their type

*Joe, 2026-08-27: "if it's checking things like job ids and not checking the
wiring, then we can get all kinds of bad behaviour — e.g. an LLM submitted packet
not passing through the designated tool, and therefore arriving badly formed.
This is where the level of validation that the WM already does in a flight is
indicative of what can work. The WM is supposed to identify a cascade, a typed
hole in the existing system it is extending, and to create a checkable wiring
diagram for the fix … failures at the level of the outer loop don't have the same
structures to fall back on, perhaps they should."*

Checked at source, and the premise holds better than expected.

#### The three structures exist, and are typed

`futon2/src/futon2/aif/fold.clj`, first line:

    fold : (cascade, circumstance) → {:wiring :coverage-score-delta :policy-holes}

All three of Joe's items, as one signature. The wiring is a real diagram —
`{:boxes :wires :terminals}` with typed ports — and `fold_llm.clj` states the
discipline: *"surfacing as policy-holes whatever it can't ground … explicit
policy-holes, never fabricated coverage."* The dispatch packet carries
`:semilattice`, `:construction-kind`, `:capability-contract`,
`:actuation-contract` and `:repair-contract`. This is a considerably better
apparatus than the outer loop has, and Joe's instinct to lift it is right.

#### But nothing checks that a semilattice is a semilattice

Across 25 recorded constructions under `data/wm-full-loop/` carrying a `:descent`
block, **4 contain a directed 2-cycle** and 5 have no edges at all. From
attempt-018, 2026-07-16:

    :semilattice
    {:descent [["iching/hexagram-05-xu"   "iching/hexagram-06-song"]
               ["iching/hexagram-06-song" "iching/hexagram-05-xu"]
               ["iching/hexagram-10-lu"   "iching/hexagram-05-xu"]],
     :co_app []}

`normalize-edge` reads a pair as `[from to]`, so those first two edges are
A→B and B→A. **The relation is not antisymmetric, so it is not a partial order,
so it is not a semilattice** — under any reading of the direction convention,
since both directions are present. It was folded anyway, and a
`:coverage-score-delta` came out.

The field is named for a structure whose defining property nothing tests. This is
the same defect as `:policy-holes []` hardcoded at `pattern_registry.clj:327`:
the slot is typed in the docstring and unconstrained in the code.

**And we now have the check.** `futon3/checks/playout_snatch.clj` computes exactly
this over a pattern collection — antisymmetry via the tree/overlap test, and the
meet condition per pair — and it discriminated: of six runs, four combinations of
(overlap, meet-semilattice) occurred. A `valid-semilattice?` in the fold's path,
refusing a cyclic `:descent` with a typed reason instead of scoring it, is small
and would have caught all four.

#### The step-contract break, in the WM's own inner loop

`pattern_registry.clj:322` emits `:semilattice` as a **bare vector** of
`{:from … :to …}` maps. `close_loop.clj`'s gate asks:

    (or (seq (:descent semilattice)) (seq (:co-app semilattice)) (seq (:co_app semilattice)))

`(:descent [{:from :a :to :b}])` is `nil` — verified — so the gate returns false
and the semilattice fold is skipped for those entries, falling through to the
classical fold. Meanwhile `semilattice-fold`'s own `normalize-edge` handles the
map shape perfectly well. **The two steps disagree about the wrapper, not about
the edges**, both are internally well-formed, and nothing relates them.

That is Joe's APM diagnosis reproduced inside the WM: shape valid at each end,
contract broken between them, and no test that could fail. It is also the answer
to *"can the WM even follow along the same track"* — it already carries the
better structures, and the structures are nominal until something refuses a
malformed one.

#### What this proposes for the outer loop

Not new machinery. The inner loop's three structures, applied one level up:

1. **A cascade with a validated order** — the outer loop's analogue of
   `:semilattice`, refused when it is not one.
2. **Typed holes** — what the outer loop could not ground, as `:policy-holes`
   are meant to be and are not yet.
3. **A wiring diagram for the route** — the direct answer to *"an LLM submitted
   packet not passing through the designated tool"*. A packet's route is already
   recorded as a mesh edge by `agency_send.py --from`; nothing checks that the
   route taken is the route the contract requires. That is a wiring check, not a
   shape check, and it is the outer loop's version of the same missing test.

Bounds: the gate reading is from source rather than from a run; the cycle counts
are over the 25 construction records on disk under `data/wm-full-loop/`, which is
not gitignored history but is one machine's.

### 3.2 Tier 1 — attestation and typed absence *(each stated as a contract clause first)*

**Ordering consequence of Tier 0.** These four are no longer "make the reports
honest". Each becomes: **a clause in the emitted contract, then an implementation
that satisfies it, then a mutation test proving drift is rejected.** The repair is
the second step of three, not the whole of it.

Each removes one indistinguishability. None changes a decision.

| # | repair | what it removes | locus |
|---|---|---|---|
| 1 | the **composer** writes one attestation per *registered* proposer — ran?, input-ref, emitted-n — whether or not it emitted | "the space is empty" vs "the generator did not run" | `action_proposer/compose-proposers`; `proposer-id` is declared at `:31` and never called |
| 2 | `read-curvature-signal` returns `fresh \| stale(age) \| absent`, not `[]` | a June signal read as current | `aif2/tension.clj:198` |
| 3 | the grounded producer returns `:domain-mismatch`, not bare `nil` | "out of domain" vs "no measurement" | `fold_realized.clj:163`; **the vocabulary already exists one function above**, `actuator_a3.clj:395` |
| 4 | stop reporting τ as governing; record the disconnection | a reported ranking that cannot move the action | `policy.clj` reporting only |

**Why #1 first.** The 2026-07-15 archive shows 24 attempts, each recording *"no
addressable entities"* for seven action classes, with no record of which
proposers ran. With attestation the operator sees it at **attempt 2** instead of
attempt 24. It is additive, it is cheap, and it is the instrument every later
repair is judged with.

### 3.3 Tier 2 — the paper (zero code, and Joe's stated goal)

For plop-2026's reputability, in descending value. **Every claim below should
cite the Tier-0 contract rather than prose once it exists** — that is what makes
the paper's R-number table checkable rather than asserted:

1. **Publish both axes.** `aif-r1-r16-pattern-map.md` asks *is it built and
   load-bearing?*; `wr-overlay.edn` asks *does the discipline hold?* They use the
   same R-numbers and disagree on R6 and R8. A reader meeting `R8 ✓ Real` and
   `R8 red` has no stated way to reconcile them. One column fixes it.
2. **Correct the four wrong ring notes** listed in §3.1. Each is a one-line edit
   against a verified source.
3. **Refresh the 07-13 map** — it is stale by twelve `aif/*.clj` namespaces,
   several bearing on rows it marks dark or open (`epistemic_value` on R5's "open
   third", `habit_prior` on the one branch where τ moves an argmax).
4. **The non-vacuity column** — per R-node, which run exercised this mechanism
   and how do we know it executed. Eighteen rows; R8 and R14 are effectively done.

### 3.4 Tier 3 — behavioural, one at a time, Joe's call

Held until Tier 1 gives them an instrument, and **each must leave the Tier-0
contract satisfied** — a behavioural repair that breaks a clause is drift, and the
mutation suite is what says so:

- **R8 slice 4** — `deposits-by-id` degrade rather than throw.
- **R14's edge** — (a) sample `P(π)`, (b) a second non-τ-scaled term, (d) route
  the gain through the candidate set as `repair-entry` already does. Each changes
  the *type* of the signal differently; (d) converts a graded quantity into a
  binary interrupt.
- **R2's channel** — one edge, between two things that already run. This is the
  *"close over the operator"* edge, and Joe's sequencing puts it last for the
  right reason: it is the plug, and the socket is Tier 1.

### 3.5 What this programme does not cover, stated plainly

**Thirteen green rings are unexamined.** Today's record is five-for-five that a
ring's recorded reason does not survive checking, and every one of those five was
*red* — a category the stack looks at. The greens have had less attention, not
more. Nothing here licenses the claim that the rest of the machine is well
behaved; §3.3's non-vacuity column is the cheapest way to find out.

**Three subsystems, three states, unreconciled:** `wm-full-loop` stopped
2026-07-15 with a written re-arm condition; `wm-trace` ends 2026-07-21;
`wm-outer-loop` last ran 2026-07-27; the operator bulletin regenerates live today
(2026-08-27). Which of these "the War Machine is running" refers to is not
currently answerable from any single file, and that is the same gap §1.1 opens on.

## Honest bounds

**The APM contract is enforced; this one would be partly unmet.** APM's
controller loads its contract and qualification checks the running artifact by
digest. Figure 4 is partly aspirational: five of sixteen badged constraints do
not hold. A Lean model of the War Machine would emit a contract the live system
fails.

That is a change in what the artifact is, not a reason to skip it. It moves the
red rings from dated prose in an EDN to proof obligations with mutation tests,
and "does not hold" becomes machine-checkable rather than asserted. But it must
be said in the first slice, because a contract with five known-unmet
constraints will be tempting to read as a scorecard.

**Scale.** APM is 2,182 lines of Lean across five files, five Clojure
namespaces, and two test suites. The wiring slice is a day. The full model is
not, and this mission should not pretend otherwise until slice 1 has run.

**The correspondence in 1.4 is a hypothesis.** Support edges may not be
policies. Naming them after APM's vocabulary before reading what they carry
would be fitting the evidence to the template.

---

## Relation to M-formal-patterns

`futon5/holes/M-formal-patterns.md` (CHARTERED 2026-07-17, **FROZEN
2026-08-04**) formalises *patterns* as signed graphs and asks when a pattern
language admits a coherent joint verdict. It has proved, machine-checked
results in `DarkTower/Patterns/Propagator.lean` for the all-negative case, and
its general signed case (S1) is unbuilt.

The two missions share a Lean project and almost nothing else:

- M-formal-patterns proves a **theorem about structure**. This mission
  specifies a **behavioural contract for a running controller**.
- Its subject is the pattern library. This mission's subject is
  `futon2.aif.*`.
- It is speculative and frozen. `M-apm-demonstration`, the precedent here, is
  running now.

One point of contact worth recording rather than pursuing: M-formal-patterns
argues that a pattern is a mini-War-Machine, selecting among its NEXT-STEPS by
expected free energy, and that joint commitment across a network requires
balance. If this mission produces a typed statement of what the War Machine's
loop actually is, that statement is what the analogy would have to be checked
against. Not a dependency in either direction; a place where the two would
eventually meet.

---

## Provenance

Chartered 2026-08-25 in conversation between Joe and claude-13, out of a
session that started with a figure and arrived here. The route: the WIP layer
renders five red rings whose urgency is derived from rulings rather than
observed; the rulings compress a ruling, an incident and a cost into one line;
the compression's expansion exists but was not reachable; and underneath all of
it the wiring that the rings are placed on is not written down anywhere. Joe
proposed the APM approach as the template, having watched it improve how
M-apm-demonstration runs.
