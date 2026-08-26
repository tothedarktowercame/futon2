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

| # | family | WM Lean predicate | stated in | implementation | R-node |
|---|---|---|---|---|---|
| 1 | identity threading | `threadedIdentity` | `GainChain.lean` | γ dedups on `:tick` (`fold_realized.clj`) vs `(System/currentTimeMillis)` at `scripts/wm_scheduled_run.clj:108` — **two clocks** | R8/R14 |
| 2 | non-empty handle | `inhabitedHandle`, `typedAbsence` | `GainChain.lean` | `fold_realized.clj:163` — `(when (pos? bound) …)` returns bare `nil` | R8 |
| 3 | self-contained record | `selfContainedRecord` — **reserved, not yet defined** | — | `actuator_a3.clj:149` throws on any rejection; `fold_escrow/load-deposits` degrades | — |
| 4 | durability before certification | `durableBeforeFold`, `loadYieldsOutcome`/`foldOf` | `GainChain.lean` | `enact.clj:255` catch-all around `close-loop!` | R8/R16 |
| 5 | provenance containment | `declaredDomain`, `domainNotNarrowed`, `dischargedPrecondition` | `GainChain.lean` | `fold_realized.clj:113` `reviewed-candidate-cleans` (4 entries); the named regime already exists at `actuator_a3.clj:395` (`:domain-mismatch`) | R8 |
| 6 | separation of powers | `separatedPowers` — **reserved** | — | ㉕/㉘; overlay R9 `:holds true` | R9 |
| 7 | exit / status pins | `pinnedExit` — **reserved** | — | ㉒ act-gate, `cascade-score > 0 ∧ coverage-score-delta < 0` | — |
| **8** | **temperature governs the action** | `governs`, `temperatureInvariant`, `gainAdvances` | `CommitmentTemperature.lean` | `policy.clj:35` (τ computed), `:238` and `:377` (argmax, τ-free), `war_machine.clj:4476,4527` | **R14** |
| **9** | **the ordering step consumes a surveyed space** | *unstated — the R6 module's slot* | — | `full_loop_runner.clj:872` `repair-entry` (works, `##-Inf`); `portfolio_action_proposer.clj` (dark) | **R6** |

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
