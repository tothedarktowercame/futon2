**Status:** DERIVE (2026-03-15)

# M-aif-head: AIF Heads for Every Peripheral (Mission Peripheral First)

## 1. IDENTIFY

### Motivation

The FUTON stack has organs but no body. Each futon is well-specified
(structural law inventory, AIF+ invariants, CT DSL, cycle machines,
obligation DAGs, gate stacks) but they are organs-without-a-body — no
shared generative model, no circulation connecting them, no capacity for
the stack to refuse a transition that violates its own structural laws.

The Mission Peripheral in futon3c is the best candidate for the second
AIF head (after the ants proof-of-concept in futon2). It already has:

- A 9-phase cycle machine with constrained action envelopes per phase
- Required outputs per phase (rudimentary prediction-observation loop)
- An obligation DAG with evidence-gated status transitions
- A gate review stack (G0–G5, GF, GD) that submits to futon3b
- Portfolio Inference as a slow-timescale dual

What it lacks — and what this mission supplies — is:

1. An explicit, queryable generative model (the structural law inventory
   as data, not hardcoded checks)
2. A default mode tier (what happens between cycles besides "wait for Joe")
3. Cross-phase prediction error tracking (did `:execute` match what
   `:propose` predicted?)

These three additions turn the Mission Peripheral from a tool (does what
Joe says) into an organism (maintains its own viability). The Mission
Peripheral is the first target because it has the richest existing
infrastructure (cycle engine, obligations, gates, Portfolio Inference),
but the end-state invariant applies to the whole stack: **each
peripheral has an AIF head.** The reusable interface produced here
(completion criterion C6) is what makes the generalization mechanical
rather than a second design effort.

The stack's three conceptual pillars map directly onto AIF components:
- **The Argument** (holistic-argument-sketch) = generative model
  (beliefs about what the stack is)
- **The Invariants** (structural-crystallization + inventory.sexp) =
  precision (operational = high tau, candidate = low, liquid = negotiable)
- **Futonic Missions** (futonic-missions.md) = policy (action selection
  to minimize free energy: IDENTIFY→MAP→DERIVE→ARGUE→VERIFY→INSTANTIATE)

This is not analogy. futon2 already has a working AIF engine with the
`AifAdapter` protocol (`select-pattern` + `update-beliefs`), a full
`FulabAdapter` with softmax/G-score/tau dynamics, and an ant-level AIF
stack (observe→perceive→affect→policy). But this infrastructure is
disconnected from the peripherals where agents actually do work. The gap:
**peripherals constrain but don't learn.**

### Theoretical anchoring

- **AIF+ (chapter0-aif-as-wiring-diagram.md):** The six invariants
  I1–I6. The Mission Peripheral already satisfies I1 (phase-gated
  boundary), I2 (observe/act asymmetry across phases), and partially
  I4 (obligation status transitions protect slow-timescale structure).
  This mission targets I3 (timescale separation via Portfolio Inference
  coupling), I5 (model adequacy via structural law consultation), and
  I6 (compositional closure via default mode tier).

- **Structural law inventory (structural-law-inventory.sexp):** The
  candidate invariant families — particularly `budgeted-action-selection`,
  `atomic-inspectable-units`, `failure-locality`, and
  `human-visible-inspectability` — become the content of the generative
  model. The operational families (graph-symmetry, status-discipline,
  phase-ordering, etc.) are already enforced; the AIF head makes the
  candidate families enforceable.

- **Friston / Sen / Ostrom triptych:**
  - Friston: action selection minimises divergence from the generative
    model (structural laws). Invariant-violating transitions register
    as surprise → high free energy → selected against.
  - Sen: mana gating distinguishes formal capability (agent has the
    function) from substantive capability (agent has function + budget +
    invariants permit). The AIF head computes the real action space.
  - Ostrom: the structural law inventory is commons governance. The AIF
    head is the local monitor, accountable to the resource users
    (the stack itself).

- **Noether's theorem (by analogy):** The structural law inventory
  characterises what is conserved across transitions. The actions fall
  out as the symmetry group of the invariants. Futon2 doesn't enumerate
  actions; it characterises conservation laws and derives legal
  transitions.

- **Default Mode Network (AIF+ §6.2–6.5):** Three-tier architecture:
  reflex (hard refusal, futon1a-style), default mode (structural law
  consultation, inter-cycle housekeeping), deliberative (AIF-inferred
  action selection within a cycle). The Mission Peripheral currently
  has only the deliberative tier.

- **Preference exogeneity applied to the operator (I4):** The mature
  stack can refuse Joe-initiated transitions that violate structural
  laws. "I'm sorry, Joe, I can't do that." The structural law inventory
  is the constitution; the AIF head enforces it against all actors,
  including the sovereign.

- **Hierarchical generative models:** The stack has structure at multiple
  scales — portfolio (which mission?), mission (which phase?), peripheral
  (which action?). Each level has its own generative model facet, but
  they compose into a single coherent model. Like an ant following local
  pheromone gradients that encode global colony state.

- **Structure learning (deferred but named):** The futon2 ants run for
  200 ticks in a fixed world — they do parameter updating but not model
  restructuring. The futon stack evolves: missions complete, invariants
  crystallize, new missions emerge. This requires a second, slower AIF
  loop that reads the evidence landscape and proposes generative model
  revisions. This mission wires the heads; structure learning is a
  follow-on that builds on the connectivity established here.

### Scope in

- Equip the Mission Peripheral with an AIF head that consults the
  structural law inventory as its generative model
- Implement a default mode tier for inter-cycle behaviour
- Add cross-phase prediction error tracking
- Wire Portfolio Inference as the slow-timescale prior
- Demonstrate refusal of at least one structurally illegal transition
- Produce a reusable AIF head interface and roll it out to all peripherals
  that agents inhabit (chat, explore, edit, deploy, reflect, mentor,
  proof, discipline, etc.). The Mission Peripheral is Phase 1; remaining
  peripherals are Phase 2, expected to be mechanical once the interface
  is proven
- Connect the evidence landscape to the belief-update pathway: currently
  evidence is write-mostly; this mission makes it readable by AIF heads —
  accumulated evidence feeds back into precision estimates and model
  confidence (counts, existence checks — not full provenance graphs)
- Define an epistemic action repertoire as data: the verification
  spectrum (convention → property tests → core.logic → formal proof) as
  named slots with precision contributions, queryable by adapters
  deciding whether a candidate invariant is ready for promotion

### Scope out

- Central futon2 "service" or scheduler (the body emerges from organs
  sharing a generative model, not from a coordinator)
- Full mana-gating economics (M-cyder territory; this mission uses
  budget constraints as one invariant among many)
- Rewriting the 9-phase cycle engine (it stays; the AIF head wraps it)
- Deep per-peripheral customization (each peripheral gets the same
  interface; domain-specific tuning of G-scores, observation channels,
  and default-mode behavior is follow-on work)
- Resolving the Stage 6 / superpod JSON closure problem (separate fix,
  upstream with Rob)
- Full structure learning (automated model restructuring from evidence) —
  this mission wires the heads; structure learning builds on the evidence
  connectivity established here
- Lean integration — the epistemic action repertoire *names* formal
  verification as a slot; filling it with an actual Lean bridge is
  separate work
- Modifying the futon2 `AifAdapter` protocol itself — if the current
  two-method protocol (`select-pattern` + `update-beliefs`) proves
  insufficient, that's a futon2-internal change gated on evidence from
  this mission

### Completion criteria

1. **Generative model consultation:** The Mission Peripheral loads the
   structural law inventory (or a compiled subset) as a queryable data
   structure and consults it before phase transitions.

2. **Prediction error signal:** At `:validate`, the peripheral computes
   a divergence measure between `:propose` outputs and `:execute`
   outputs. High divergence biases toward `:classify` → failed approach
   rather than `:integrate` → commit.

3. **Default mode demonstrated:** When a cycle completes, the peripheral
   autonomously performs: (a) generate PAR for the completed cycle,
   (b) consult Portfolio Inference for next blocker/assignment,
   (c) check structural law compliance across mission repos,
   (d) either begin next cycle or signal blocked/complete. Not "wait
   for Joe."

4. **Refusal demonstrated:** A test case where a Joe-initiated or
   agent-initiated transition would violate a structural law (e.g.,
   writing solver code to the wrong repo, committing without
   falsification cycle, advancing past `:gate-review` with open
   honest intervals) and the peripheral refuses with a
   diagnosis referencing the specific invariant violated.

5. **Three-tier architecture operational:** Reflex tier (hard refusal
   of foundational invariant violations), default mode tier (structural
   law consultation + inter-cycle housekeeping), deliberative tier
   (existing 9-phase cycle with AIF-informed action selection).

6. **Reusable AIF head interface:** A protocol or shape definition
   that specifies: generative model source, prediction error
   computation, default mode behaviour, refusal surface, and
   timescale coupling. The Proof Peripheral should be adoptable
   as a follow-on without redesigning the interface.

7. **Evidence emitted:** All AIF head decisions (consultations,
   prediction errors, refusals, default mode actions) emit evidence
   to the evidence landscape, maintaining the "all turns are logged"
   property.

8. **Evidence landscape queryable:** An AIF adapter can ask "how much
   evidence supports this mission/invariant/claim?" and get a count
   that feeds into G-score computation. The evidence store becomes
   readable by heads, not just writable by agents.

9. **Self-enforcing invariant:** "Every peripheral has an AIF head" is
   itself testable — a function enumerates registered peripherals and
   checks each has an adapter binding. No peripheral can be registered
   without one. All agent-inhabited peripherals have bindings by mission
   completion.

### Relationship to other missions

**Depends on (ready):**
- M-structural-law (the structural law inventory must be at least at
  Phase 1 IDENTIFY, which it is — the sexp exists)
- Mission Peripheral operational (it is — README-mission-peripheral.md
  confirms)
- Portfolio Inference operational (it is — portfolio/core.clj exists)
- futon2 AIF engine — already built, `AifAdapter` protocol is stable

**Depends on (partial):**
- M-self-representing-stack — the evidence landscape as readable
  substrate. This mission needs basic evidence queryability (counts,
  existence checks); M-self-representing-stack goes further (provenance
  graphs, causal tracing). We can build C8 with what exists now and
  deepen it later.

**Enables:**
- Automated mission selection — the portfolio head replaces ad-hoc queue
  management (bell/whistle/conductor) with principled free energy
  minimization
- Invariant promotion pipeline — epistemic action repertoire + precision
  tracking creates a formal pathway from candidate → operational
- M-aif-head-proof: port the AIF head to the Proof Peripheral
- M-futon2-resolution: the resolution theorem prover that uses the
  AIF head pattern across all peripherals (the Eric-finder, the
  mission selector, and the law enforcer as three queries on one engine)
- M-self-representing-stack: the AIF head consulting the structural
  law inventory is the stack describing itself in its own terms
- Future consulting engagement onboarding: each new "Eric" gets a
  futon7 peripheral with an AIF head, structurally identical to the
  mission peripheral but domain-projected

**Sibling:**
- M-structural-law: the axiom set this mission's AIF head operates
  against
- M-cyder: the cybernetic ops surface that would provide the mana
  economics layer
- M-futon3x-e2e: end-to-end gate traversal, which this mission
  extends with AIF-informed gate decisions

### Source material

**futon2 (AIF engine — the pattern source):**
- `src/futon2/aif/adapter.clj` — `AifAdapter` protocol (2 methods:
  `select-pattern`, `update-beliefs`)
- `src/futon2/aif/engine.clj` — domain-agnostic engine wrapper
- `src/futon2/aif/adapters/fulab.clj` — working adapter with
  softmax/G-score/tau/evidence-tracking (the reference implementation)
- `src/futon2/aif/adapters/futon5_mca.clj` — stub adapter (reference
  shape for new adapters)
- `src/ants/aif/` — 8-file ant AIF stack (observe/perceive/affect/policy)
- `doc/aif-engine.md` — engine contract and API surface
- `doc/stack-baseline.md` — ant AIF loop documentation with trace format

**futon3c (peripherals — the wiring target):**
- `src/futon3c/peripheral/adapter.clj` — tool mapping + prompt generation
- `src/futon3c/peripheral/runner.clj` — `PeripheralRunner` protocol
- `src/futon3c/peripheral/registry.clj` — peripheral registration
- `src/futon3c/peripheral/mission.clj` — Mission Peripheral implementation
- `src/futon3c/peripheral/cycle.clj` — generic cycle engine
- `src/futon3c/peripheral/mission_backend.clj` — domain logic
- `src/futon3c/peripheral/mission_shapes.clj` — state shapes
- `src/futon3c/portfolio/core.clj` — Portfolio Inference

**Conceptual anchors:**
- `structural-law-inventory.sexp` — the generative model content
- `chapter0-aif-as-wiring-diagram.md` — the AIF+ invariant framework
- `futon3/holes/holistic-argument-sketch.md` — the Argument
- `futon3c/docs/structural-crystallization.md` — crystallization narrative
- `futon3c/docs/futonic-missions.md` — mission methodology

**Other repos:**
- `futon5/src/.../ct/dsl.clj` — CT DSL for diagram composition
- `futon5/src/.../wiring/compose.clj` — wiring composition
- `futon3b/src/futon3/gate/canon.clj` — gate canonisation logic

### Owner and dependencies

- **Primary repo:** futon3c (Mission Peripheral lives here)
- **Secondary repos:** futon2 (AIF head pattern source, ants),
  futon3b (gate pipeline), futon5 (CT DSL / wiring validation)
- **Owner:** Joe
- **Structural law inventory:** currently a standalone sexp; may need
  to be ingested into futon1a or futon4 (Arxana) as a hypergraph
  for proper queryability — this is a MAP-phase question.

---

## 2. MAP

Survey questions for the MAP phase:

- [x] **Q1:** What is the current interface between the Mission Peripheral
  and Portfolio Inference?

  **Answer: All three — function call, shared data, and evidence-mediated.**
  Mission Control Backend builds a `PortfolioReview` map via
  `build-portfolio-review` (scans all repos → missions, devmaps, coverage,
  mana, gaps). Portfolio Inference reads this via `observe/gather-mc-state`
  → normalizes 15 channels → feeds into its own AIF step. Portfolio emits
  three evidence types (observation, belief, policy) to the evidence store.

  **Key gap:** The action loop is open. Portfolio inference produces an
  `:action` keyword but nothing consumes it to modify mission state or
  dispatch agents. The observation→inference path works; the
  inference→effect path is missing.

- [x] **Q2:** What does the ants AIF head actually look like in code?

  **Answer: Five pure functions composed in `aif-step`, plus an extracted
  protocol.** The ant pipeline is: observe (16-dim normalized vector) →
  perceive (5 predictive coding micro-steps → mu/prec/errors/free-energy)
  → affect (hunger→precision→tau coupling) → policy (EFE over 4 actions,
  softmax, 5-component scoring) → default-mode (tropism-based fallback,
  same output shape as policy for drop-in substitution).

  **Already extracted:** `futon2.aif.adapter/AifAdapter` protocol with
  2 methods (`select-pattern`, `update-beliefs`). `FulabAdapter` is a
  working reference (170 lines). `Futon5McaAdapter` is a stub. The ant
  pipeline's perceive/affect/policy are domain-agnostic; only observe
  and default-mode need domain-specific implementations.

- [x] **Q3:** How is the structural law inventory best represented for
  runtime consultation?

  **Answer: As core.logic relations (pldb).** Five domains already use
  this exact pattern: portfolio, tickle, agency, proof, mission — each
  with `build-db` → pldb facts → goals → `query-violations`. Shared
  combinators in `futon3c.logic.structural-law` (paired-edge-mismatches,
  dangling-targets, invalid-enums, missing-phase-outputs). The sexp
  inventory (17 families) loads trivially as pldb facts with family-id,
  status, scope, kind, home, implemented-in fields. ~200 lines of new
  code, all following proven patterns.

- [x] **Q4:** Where in the cycle engine are the natural interception
  points?

  **(a) Pre-transition invariant checking:** `validate-phase-advance` in
  `mission_backend.clj:273-285` — already blocks advancement if required
  outputs missing. This is the hook for structural law consultation.

  **(b) Cross-phase prediction error:** The condition
  `(= new-phase last-phase)` at `cycle.clj:159` detects completion.
  Before the state transformation that clears `:current-phase`, we could
  inject cross-phase analysis. **No hook currently exists** — error
  computation would need to be injected into `dispatch-step`.

  **(c) Default mode entry:** **NO CURRENT MECHANISM.** When cycle
  completes, `:current-phase` and `:current-cycle-id` are dissoc'd,
  `:cycles-completed` incremented, and the peripheral reverts to setup
  mode. No callback, no fire-on-idle, no automatic next action. What
  happens next is the caller's responsibility.

- [x] **Q5:** What prediction is available at `:propose` vs `:execute`?

  **Answer: Not enough for divergence — yet.** `:propose` produces only
  `{:approach "strategy description"}` (a narrative). `:execute` produces
  `{:artifacts ["file1.clj" "file2.clj"]}` (concrete paths). These are
  type-incompatible. To compute divergence, `:propose` needs enrichment:
  predicted artifacts, predicted scope, predicted file count, measurable
  success criteria. Then set-theoretic artifact divergence, scope
  divergence, and criteria satisfaction become computable. This is a
  DERIVE decision: enrich `:propose` required outputs.

- [x] **Q6:** What refusal mechanisms already exist?

  **Answer: Three layers, converging on `{:ok false ...}`.**

  | Layer | Mechanism | Shape | Short-circuit |
  |-------|-----------|-------|---------------|
  | futon1a L3 | Penholder auth | Throws `ex-info` → HTTP 403 | Exception handler |
  | futon3b G0–G5 | Gate pipeline | `{:ok false :type :gate/reject :error/key KW}` | `ok?` check |
  | futon3c | Status/mode/DAG | `{:ok false :error {:code KW :message STR}}` | Tool return |

  Common pattern: predefined error catalog, returned `{:ok false}` value,
  short-circuit on failure, proof-of-rejection persisted. futon3b's gate
  pipeline (33+ error keys) is the most mature model.

- [x] **Q7:** What is the current "end of cycle" code path?

  **Answer:** `dispatch-step` detects `(= new-phase last-phase)` →
  dissocs `:current-phase` and `:current-cycle-id` → increments
  `:cycles-completed` → emits step evidence → returns to setup mode.
  No automatic save (agent must call `:mission-save`), no automatic
  dispatch, no bell, no default mode entry. The peripheral just stops
  accepting cycle-advance and waits.

  **This is the primary gap for default mode.** The hook point is clear
  (the completion branch in `dispatch-step`), but the machinery to
  invoke default-mode behavior doesn't exist.

- [x] **Q8:** Can candidate families load as core.logic relations?

  **Answer: Yes, directly.** Relations are additive. Status field
  (`:operational` vs `:candidate`) distinguishes them. The existing
  `invariant_runner.clj` tracks load profiles. Cross-domain queries
  like "which candidates implicate failure-locality?" become
  straightforward. No architectural change needed — just new facts
  in the same pldb databases.

- [x] **Q9:** Wiring diagram: Mission Peripheral as AIF+ agent.

  **Mapping the 9 phases to AIF roles:**

  | AIF Role | Mission Peripheral Component |
  |----------|------------------------------|
  | **Environment** | Codebase + evidence store + other repos |
  | **Sensory** | `:observe` + `:orient` phases (read state) |
  | **Internal** | `:propose` + `:classify` (model, decide) |
  | **Active** | `:execute` + `:integrate` (write code, commit) |
  | **Preferences** | Structural law inventory + mission completion criteria |

  **Invariant status:**

  | Invariant | Status | Evidence |
  |-----------|--------|----------|
  | **I1 Boundary** | ✓ Satisfied | Phase-gated tool envelopes (read-only in observe, write in execute) |
  | **I2 Obs/Act asymmetry** | ✓ Satisfied | Different tool sets per phase; observe≠execute |
  | **I3 Timescale separation** | ○ Partial | Portfolio Inference is slower, but not formally coupled |
  | **I4 Preference exogeneity** | ○ Partial | Obligation transitions are gated, but structural laws aren't consulted |
  | **I5 Model adequacy** | ✗ Missing | No explicit generative model; no prediction error tracking |
  | **I6 Compositional closure** | ✗ Missing | No default mode; cycle completion → dead stop |

  **This mission targets I3 (wire Portfolio Inference as slow prior),
  I5 (structural law inventory as generative model + prediction error),
  and I6 (default mode tier for inter-cycle viability).**

- [x] **Q10:** What would a minimal "I'm sorry, Joe" look like?

  **Answer: A returned error map, following the futon3c pattern.**
  Specifically: `{:ok false :error {:code :structural-law-violation
  :message "Cannot advance: budgeted-action-selection requires..."
  :law-family :budgeted-action-selection :law-status :candidate
  :context {...}}}`. This fits the existing `mission-error` helper in
  `mission_backend.clj`. The refusal would be emitted as evidence
  (coordination evidence type) and returned as the tool result. No
  exception — the pipeline continues, but the transition is blocked.

  For the reflex tier (foundational invariant violations), throwing
  `ex-info` is appropriate since these represent "impossible states"
  rather than "unwise transitions."

### Ready vs Missing

**Ready (no new code needed):**

| Component | Location | What it provides |
|-----------|----------|-----------------|
| AIF engine + adapter protocol | `futon2/src/futon2/aif/` | Domain-agnostic engine, 2-method protocol |
| FulabAdapter reference impl | `futon2/src/futon2/aif/adapters/fulab.clj` | Working softmax/G/tau/evidence adapter |
| 9-phase cycle engine | `futon3c/peripheral/cycle.clj` | Phase transitions, tool dispatch, evidence emission |
| Mission Peripheral | `futon3c/peripheral/mission*.clj` | Full mission lifecycle (phases, obligations, gates) |
| Portfolio Inference | `futon3c/portfolio/{core,observe,policy,logic}.clj` | 15-channel observation, AIF step, evidence emission |
| core.logic infrastructure | 5 domain `*_logic.clj` files + `structural_law.clj` | Proven pldb pattern, shared combinators |
| Structural law inventory | `futon3c/docs/structural-law-inventory.sexp` | 9 operational + 8 candidate families as data |
| Refusal patterns | futon1a/futon3b/futon3c | `{:ok false}` convention, gate error catalogs |
| Pre-transition hook | `mission_backend.clj:validate-phase-advance` | Existing interception point for invariant checking |
| AIF+ invariant spec | `futon5/docs/chapter0-aif-as-wiring-diagram.md` | I1–I6 framework with formal definitions |

**Missing (the actual work):**

| Component | What needs building | Estimated size |
|-----------|-------------------|----------------|
| Inventory loader | `logic/inventory.clj` — load sexp → pldb facts | ~200 lines |
| Mission AIF adapter | New `AifAdapter` impl for the Mission Peripheral | ~300 lines |
| Default mode tier | Cycle-completion hook + inter-cycle behavior | ~200 lines |
| Prediction enrichment | Extend `:propose` required outputs for divergence | ~100 lines |
| Prediction error compute | Cross-phase divergence in `dispatch-step` | ~150 lines |
| Refusal surface | Structural law consultation before transitions | ~150 lines |
| Portfolio effect sink | Wire Portfolio `:action` → mission state changes | ~100 lines |
| Reflex tier | Hard refusal for foundational invariant violations | ~100 lines |
| AIF head protocol | Reusable shape: gen-model, pred-error, default-mode, refusal, coupling | ~100 lines |
| Evidence read-back | Query evidence counts by mission/invariant/claim | ~150 lines |

**Total estimated new code: ~1,550 lines across futon2 + futon3c.**

### Surprises

1. **Portfolio Inference is further along than expected.** It already has
   a full AIF step with 15 observation channels, softmax policy, and
   evidence emission. The gap is only the effect loop (action → state).

2. **The cycle engine has no completion callback.** This is the single
   biggest structural gap — there is literally nowhere for default mode
   to attach without modifying `dispatch-step`.

3. **`:propose` is unstructured.** It produces only a narrative string,
   not a prediction. Enriching it is the prerequisite for prediction
   error tracking — and this is a design decision, not just plumbing.

4. **core.logic is thoroughly established.** Five domains already use the
   same pldb pattern. Loading the sexp inventory is genuinely ~200 lines
   of straightforward code, not a research problem.

5. **Refusal conventions are convergent but not unified.** All three
   repos use `{:ok false}` with error catalogs, but the specific shapes
   differ. A common refusal type would be useful but isn't blocking.

---

## 3. DERIVE

### D-1: AIF Head Protocol (the reusable interface)

**Decision**: Extend `AifAdapter` with three additional capabilities,
expressed as a new protocol that composes with the existing one.

```clojure
(defprotocol AifHead
  "Extends AifAdapter with peripheral-specific AIF capabilities.
   Every peripheral that agents inhabit must implement this."

  (observe [head state context]
    "Gather observations from the peripheral's environment.
     Returns {:channels {kw → [0,1]} :raw {kw → any}}")

  (default-mode [head state observation]
    "Inter-cycle / inter-action fallback behavior.
     Called when the deliberative tier has nothing to do.
     Returns same shape as select-pattern for drop-in substitution.")

  (check-law [head state transition]
    "Consult structural law inventory before a transition.
     Returns {:ok true} or {:ok false :error {:code :structural-law-violation
                                               :law-family kw ...}}"))
```

**IF** we keep the existing `AifAdapter` protocol unchanged, **HOWEVER**
we need three capabilities it doesn't provide (domain-specific observation,
default mode for I6, structural law consultation for I4/I5), **THEN** we
add a separate `AifHead` protocol that composes with `AifAdapter`, **BECAUSE**:
1. Existing adapters (FulabAdapter, Futon5McaAdapter) continue to work
2. The 2-method core (`select-pattern` + `update-beliefs`) remains the
   minimal interface for simple adapters
3. `AifHead` is the "peripheral-grade" interface: observe + default-mode
   + check-law + the inherited select-pattern + update-beliefs = 5 methods
4. The ant default-mode already returns the same shape as policy — this
   formalizes that convention

**Three-tier mapping:**

| Tier | Method | When invoked |
|------|--------|-------------|
| Reflex | `check-law` | Before every transition; throws on foundational violations |
| Default mode | `default-mode` | When cycle completes or deliberative tier has no action |
| Deliberative | `select-pattern` | Normal AIF action selection within a cycle |

### D-2: Mission Peripheral Adapter

**Entity**: `MissionAifHead` — implements both `AifAdapter` and `AifHead`.

**Observation layer** (implements `observe`):

10 channels as specified in `peripheral-aif-vocabulary.sexp` under
`:peripheral/id :mission`. Sources:

| Channel | Source function | Already exists? |
|---------|----------------|-----------------|
| `:phase-progress` | `cycle/current-phase` → ordinal | Yes |
| `:obligation-satisfaction` | `mission-backend/obligation-summary` | Yes |
| `:required-outputs-present` | `mission-logic/missing-phase-outputs` | Yes |
| `:structural-law-compliance` | NEW: `logic/inventory/check-compliance` | No |
| `:prediction-divergence` | NEW: cross-phase compare | No |
| `:evidence-for-completion-criteria` | `estore/query*` with mission subject | Yes |
| `:gate-readiness` | `gate-pipeline/dry-run` or count passing gates | Partial |
| `:argument-claim-coverage` | NEW: argument → evidence lookup | No |
| `:cycle-count` | `state :cycles-completed` | Yes |
| `:days-since-last-activity` | `estore/query*` with mission subject | Yes |

6 of 10 channels read from existing functions. 3 need new code
(structural law, prediction divergence, argument claims). 1 is partial
(gate dry-run).

**Perception**: Reuse `portfolio/perceive.clj` pattern — predictive
coding micro-steps. Same `compute-errors`, `update-sens`, `update-urgency`
structure with mission-specific precision weights from the sexp.

**Policy** (implements `select-pattern`):

Action arena from sexp: `[:advance-phase :revise-approach :request-review
:save-state :signal-blocked :signal-complete]`. EFE computation follows
`portfolio/policy.clj` pattern: pragmatic + epistemic + effort terms,
softmax with tau, abstain threshold.

**Belief update** (implements `update-beliefs`):

On each tool call result or phase transition, update pattern evidence
counts and precision. Follows `FulabAdapter.update-beliefs` exactly.

**Default mode** (implements `default-mode`):

When cycle completes:
1. Generate PAR for the completed cycle
2. Consult Portfolio Inference for next assignment
3. Check structural law compliance across repos
4. Either begin next cycle or signal blocked/complete

Returns `{:action :begin-next-cycle ...}` or `{:action :signal-complete ...}`
— same shape as `select-pattern` output.

**Law consultation** (implements `check-law`):

Before phase transitions, query the structural law inventory (loaded as
core.logic pldb facts) for violations. Returns `{:ok true}` or a
structured refusal. The refusal is emitted as evidence and returned to
the caller.

### D-3: Prediction Enrichment

**Decision**: Extend `:propose` required outputs from `{:approach str}`
to a structured prediction.

```clojure
;; Current (mission_shapes.clj):
:propose {:approach any?}

;; Proposed:
:propose {:approach any?
          :predicted-artifacts [string?]      ;; expected file paths
          :predicted-scope {:modules [kw?]    ;; affected modules
                            :file-count int?} ;; rough estimate
          :success-criteria [string?]}        ;; measurable outcomes
```

**IF** we want cross-phase prediction error (C2), **HOWEVER** `:propose`
currently produces only a narrative string, **THEN** we add optional
structured fields alongside the existing `:approach`, **BECAUSE**:
1. Backward compatible — `:approach` remains required, new fields optional
2. Agents naturally produce this information (they plan before executing)
3. Divergence becomes computable: set difference on artifacts, scope
   comparison, criteria checklist

**Divergence computation** (injected into `dispatch-step` at cycle
completion):

```clojure
(defn compute-prediction-divergence
  "Compare :propose prediction against :execute actuals.
   Returns [0,1] divergence score."
  [propose-data execute-data]
  (let [pred-artifacts (set (:predicted-artifacts propose-data))
        actual-artifacts (set (:artifacts execute-data))
        artifact-div (if (empty? pred-artifacts) 0.0
                       (/ (count (clojure.set/symmetric-difference
                                   pred-artifacts actual-artifacts))
                          (max 1 (count (clojure.set/union
                                          pred-artifacts actual-artifacts)))))
        criteria-met (count (filter (:success-criteria propose-data)
                                    ;; check against evidence
                                    ))
        criteria-total (count (:success-criteria propose-data))
        criteria-div (if (zero? criteria-total) 0.0
                       (- 1.0 (/ criteria-met criteria-total)))]
    ;; Weighted average
    (* 0.5 (+ artifact-div criteria-div))))
```

### D-4: Cycle Completion Hook (Default Mode Entry Point)

**Decision**: Add an `:on-cycle-complete` callback to the cycle engine.

**IF** the cycle engine has no completion callback (MAP Q4c, Q7),
**HOWEVER** default mode needs to attach somewhere, **THEN** add a
single hook point in `dispatch-step`'s completion branch, **BECAUSE**:
1. Minimal change to cycle.clj (one `when` clause)
2. The hook receives the completed state and returns a default-mode action
3. Backward compatible — nil callback means current behavior (stop and wait)

```clojure
;; In cycle.clj dispatch-step, completion branch:
(when-let [on-complete (:on-cycle-complete opts)]
  (on-complete completed-state))
```

The `MissionAifHead.default-mode` method IS the callback. It gets wired
in when the Mission Peripheral starts.

### D-5: Structural Law Consultation (Refusal Surface)

**Decision**: Inject law checking into `validate-phase-advance`.

**IF** structural laws aren't consulted before transitions (MAP Q4a),
**HOWEVER** the pre-transition hook already exists in `validate-phase-advance`,
**THEN** extend it to call `AifHead.check-law` before allowing advancement,
**BECAUSE**:
1. Single interception point — all phase transitions go through this
2. Returns `{:ok false}` on violation, same shape as existing errors
3. Violation is emitted as evidence (coordination type)
4. Reflex tier: foundational invariant violations throw `ex-info`
   (these are "impossible states" — data corruption, not policy disagreement)

**Error catalog** (new entries in mission-backend):

```clojure
:structural-law-violation      ;; candidate invariant would be violated
:foundational-invariant-breach ;; operational invariant violated (reflex tier)
:prediction-divergence-high    ;; propose/execute divergence above threshold
:default-mode-override         ;; default mode action conflicts with requested
```

### D-6: Portfolio Effect Sink

**Decision**: Wire Portfolio Inference's `:action` output to mission
state changes.

**IF** Portfolio produces actions but nothing consumes them (MAP Q1 gap),
**THEN** add an effect handler that maps Portfolio actions to mission
operations, **BECAUSE** the observation→inference path works and only
the inference→effect path is missing.

```clojure
;; portfolio/effect.clj (new file, ~100 lines)
(defn apply-portfolio-action
  "Translate portfolio inference action into mission state change."
  [action portfolio-result mission-state]
  (case action
    :work-on    {:effect :begin-cycle :mission (:focus portfolio-result)}
    :review     {:effect :portfolio-review}
    :consolidate {:effect :consolidate :targets (:consolidation-targets portfolio-result)}
    :upvote     {:effect :upvote :target (:upvote-target portfolio-result)}
    :wait       {:effect :none}))
```

This closes the AIF loop for Portfolio Inference: observe → perceive →
policy → **effect** → observe. The effect changes mission state, which
changes what the next observation sees.

### D-7: Epistemic Action Repertoire

**Decision**: Define as data in the sexp vocabulary, not as code.

```clojure
;; In peripheral-aif-vocabulary.sexp, new top-level section:
(:epistemic-actions
 [{:id :convention
   :precision-contribution 0.1
   :description "We've been doing it this way"
   :verification :none
   :cost :negligible}

  {:id :property-test
   :precision-contribution 0.3
   :description "Generative tests probe the invariant boundary"
   :verification :test-suite
   :cost :low}

  {:id :core-logic-validation
   :precision-contribution 0.5
   :description "Declarative specification checked against codebase"
   :verification :pldb-query
   :cost :medium}

  {:id :formal-proof
   :precision-contribution 0.9
   :description "Machine-checked verification (Lean, Coq, etc.)"
   :verification :theorem-prover
   :cost :high}])
```

**IF** different invariants need different verification thresholds,
**THEN** define the repertoire as data with precision contributions,
**BECAUSE** an AIF adapter can then query: "what epistemic actions have
been applied to this invariant?" → sum precision contributions →
compare against promotion threshold. This is the crystallization gate
formalized.

### D-8: Wiring Diagram (optional per mission-lifecycle)

Not a futon5 exotype diagram (this mission doesn't define a new component
topology), but the AIF role mapping from MAP Q9 serves as the structural
reference:

```
Environment ← Codebase + evidence store + repos
    │
    ▼
Sensory ← :observe + :orient phases ──► OBSERVATION CHANNELS (10)
    │
    ▼
Internal ← :propose + :classify ──► BELIEFS (μ) + PERCEPTION (micro-steps)
    │                                     │
    ▼                                     ▼
Active ← :execute + :integrate ──► POLICY (EFE + softmax)
    │                                     │
    ▼                                     ▼
Preferences ← Structural law inventory + completion criteria
    │
    ▼
[check-law gate between Preferences and Active]
[default-mode between cycles]
[Portfolio Inference as slow-timescale prior on Preferences]
```

### D-9: File Layout

```
futon2/
  src/futon2/aif/
    head.clj              NEW — AifHead protocol definition

futon3c/
  src/futon3c/aif/
    mission_head.clj      NEW — MissionAifHead implementation
    observe.clj           NEW — mission observation channels
    default_mode.clj      NEW — inter-cycle behavior
  src/futon3c/logic/
    inventory.clj          NEW — sexp → pldb loader
  src/futon3c/portfolio/
    effect.clj             NEW — portfolio action → mission state
  src/futon3c/peripheral/
    cycle.clj              EDIT — add :on-cycle-complete hook
    mission_backend.clj    EDIT — extend validate-phase-advance
    mission_shapes.clj     EDIT — enrich :propose outputs
  docs/
    peripheral-aif-vocabulary.sexp  EDIT — add epistemic actions
```

### D-10: Handoff Boundaries (Codex-scoped tasks)

| Task | Scope | In (read-only) | Out (create) | Complexity |
|------|-------|----------------|--------------|------------|
| H-1: AifHead protocol | futon2 | adapter.clj | head.clj | Low |
| H-2: Inventory loader | futon3c | inventory.sexp, structural_law.clj | logic/inventory.clj | Medium |
| H-3: Mission observe | futon3c | mission_backend.clj, mission_shapes.clj, vocabulary.sexp | aif/observe.clj | Medium |
| H-4: Mission AIF head | futon3c | head.clj, observe.clj, portfolio/perceive.clj | aif/mission_head.clj | High |
| H-5: Default mode | futon3c | cycle.clj, portfolio/core.clj | aif/default_mode.clj, cycle.clj edit | Medium |
| H-6: Prediction enrichment | futon3c | mission_shapes.clj, mission_backend.clj | edits to both | Low |
| H-7: Refusal surface | futon3c | mission_backend.clj, inventory.clj | edit to mission_backend.clj | Medium |
| H-8: Portfolio effect | futon3c | portfolio/policy.clj, mission_backend.clj | portfolio/effect.clj | Low |

H-1 and H-2 have no dependencies. H-3 depends on H-2. H-4 depends on
H-1 + H-3. H-5 depends on H-4. H-6 and H-7 can run in parallel with
H-4. H-8 is independent.

Dependency graph:
```
H-1 ──┐
      ├──► H-4 ──► H-5
H-2 ──► H-3 ──┘
H-6 ──────────────► (parallel)
H-7 ──────────────► (parallel, needs H-2)
H-8 ──────────────► (independent)
```

---

## 4. ARGUE

*Accretes after DERIVE.*

---

## 5. VERIFY

*Accretes after ARGUE.*

---

## 6. INSTANTIATE

*Accretes after VERIFY.*
