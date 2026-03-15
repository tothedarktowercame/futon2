**Status:** IDENTIFY (2026-03-15)

# M-aif-head: Give the Mission Peripheral an Active Inference Head

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
Joe says) into an organism (maintains its own viability). The one
invariant to rule them all: **each peripheral has an AIF head.**

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

### Scope in

- Equip the Mission Peripheral with an AIF head that consults the
  structural law inventory as its generative model
- Implement a default mode tier for inter-cycle behaviour
- Add cross-phase prediction error tracking
- Wire Portfolio Inference as the slow-timescale prior
- Demonstrate refusal of at least one structurally illegal transition
- Produce a reusable AIF head interface that the Proof Peripheral (and
  future peripherals) can adopt

### Scope out

- Central futon2 "service" or scheduler (the body emerges from organs
  sharing a generative model, not from a coordinator)
- Full mana-gating economics (M-cyder territory; this mission uses
  budget constraints as one invariant among many)
- Rewriting the 9-phase cycle engine (it stays; the AIF head wraps it)
- Porting to the Proof Peripheral (follow-on mission, but the interface
  should make it straightforward)
- Resolving the Stage 6 / superpod JSON closure problem (separate fix,
  upstream with Rob)

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

### Relationship to other missions

**Depends on:**
- M-structural-law (the structural law inventory must be at least at
  Phase 1 IDENTIFY, which it is — the sexp exists)
- Mission Peripheral operational (it is — README-mission-peripheral.md
  confirms)
- Portfolio Inference operational (it is — portfolio/core.clj exists)

**Enables:**
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

- `structural-law-inventory.sexp` — the generative model content
- `chapter0-aif-as-wiring-diagram.md` — the AIF+ invariant framework
- `README-mission-peripheral.md` — current Mission Peripheral state
- `src/futon3c/peripheral/mission.clj` — implementation
- `src/futon3c/peripheral/cycle.clj` — generic cycle engine
- `src/futon3c/peripheral/mission_backend.clj` — domain logic
- `src/futon3c/peripheral/mission_shapes.clj` — state shapes
- `src/futon3c/portfolio/core.clj` — Portfolio Inference
- `futon5/src/.../ct/dsl.clj` — CT DSL for diagram composition
- `futon5/src/.../wiring/compose.clj` — wiring composition
- `futon3b/src/futon3/gate/canon.clj` — gate canonisation logic
- futon2 ants demo (AIF head proof of concept)

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

- [ ] **Q1:** What is the current interface between the Mission Peripheral
  and Portfolio Inference? Is it a function call, a shared data structure,
  or evidence-mediated?

- [ ] **Q2:** What does the ants AIF head actually look like in code? What
  is the minimal interface (protocol, record, multimethod) that it
  exposes? Can it be extracted as a reusable shape?

- [ ] **Q3:** How is the structural law inventory best represented for
  runtime consultation — as an sexp loaded into memory, as core.logic
  relations, as Arxana hyperedges, or as a compiled gate configuration?

- [ ] **Q4:** Where in the cycle engine (`cycle.clj`) are the natural
  interception points for: (a) pre-transition invariant checking,
  (b) cross-phase prediction error, (c) default mode entry on cycle
  completion?

- [ ] **Q5:** What prediction is actually available at `:propose` that
  could be compared against `:execute` output? Is it structured enough
  for a computable divergence measure, or does it need to be?

- [ ] **Q6:** What refusal mechanisms already exist in the stack (e.g.,
  futon1a penholder refusal, futon3b gate rejection, obligation status
  enforcement) and what is their common interface shape?

- [ ] **Q7:** What is the current "end of cycle" code path? What happens
  after `:completed` clears the active phase — what state is the
  peripheral left in, and what triggers the next action?

- [ ] **Q8:** Can the structural law inventory's candidate families be
  loaded as core.logic relations alongside the existing operational
  families in proof_logic.clj / agency/logic.clj, or does the
  representation need a different approach?

- [ ] **Q9:** What is the wiring diagram shape for the Mission Peripheral
  as an AIF+ agent? Map its 9 phases, tool surfaces, and evidence
  emissions onto the five AIF roles (Environment, Sensory, Internal,
  Active, Preferences) and the six invariants (I1–I6).

- [ ] **Q10:** What would a minimal "I'm sorry, Joe" look like in the
  current Mission Peripheral infrastructure — a thrown exception, a
  returned error map, a gate failure, an evidence emission, or
  something else?

---

## 3. DERIVE

*Accretes after MAP.*

---

## 4. ARGUE

*Accretes after DERIVE.*

---

## 5. VERIFY

*Accretes after ARGUE.*

---

## 6. INSTANTIATE

*Accretes after VERIFY.*
