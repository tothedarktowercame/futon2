Good—this is exactly the right moment to switch into **IDENTIFY mode**.

I’ll write a futonic mission in that style, grounded in your three-phase programme and the requirements we just derived, but keeping it strictly at IDENTIFY (no design commitments yet).

---

# Mission: Evaluating Open Research Training as a Learning System

**Date:** 2026-03-31
**Status:** IDENTIFY (mission proposal)
**Cross-ref:**

* UKRN Working Paper draft (ORP T3 evaluation) 
* `notebooks/ukrn_population_model.clj` (institution model) 
* `notebooks/ukrn_learning_system.clj` (scenario DAG model) 
* `notebooks/traversable_argument.clj` (graph + waypoint prototype)

---

## 1. Motivation

The ORP T3 evaluation produced qualitative findings (five patterns, coordination gradient, implementation modes) that explain when training translates into sustained practice .

However, these findings are currently expressed in:

* narrative form (working paper)
* pattern form (design patterns)
* partial logic-model form (implicit causal structure)
* early computational form (population + scenario notebooks)

There is a discrepancy:

* **Ideal:** a unified learning-system representation where evidence, patterns, interventions, and outcomes are structurally explicit and testable
* **Actual:** multiple partially aligned representations (text, patterns, models) with no single substrate

Additionally:

* UKRN-S is in a design phase and requires **projective insight**, not just retrospective evaluation
* Existing models already go beyond the original logic model but are not yet integrated with the argument structure
* The working paper does not yet demonstrate that its logic is **internally coherent, reconstructable, and extensible**

This mission exists to close that gap.

---

## 2. Theoretical anchoring

This mission draws on:

* **Design patterns** (Alexander, Kohls): qualitative regularities expressed as reusable structures
* **Normalisation Process Theory (NPT):** institutional embedding and coordination dynamics
* **Capability approach (Sen):** focus on realisable capability, not nominal provision
* **Commons governance (Ostrom):** coordination, stewardship, and multi-actor systems
* **Bayesian learning systems:** prior → intervention → measurement → update loop
* **Futonic stack principles:**

  * evidence over assertion
  * self-representing systems
  * explicit invariants
  * agent/human co-observable capability development 

The central theoretical move is:

> Treat open research training not as a delivery mechanism, but as a **distributed learning system** whose structure can be reconstructed, simulated, and compared.

---

## 3. Scope

### In scope

**Phase 1: Reconstruction (empirical)**

* Reconstruct the ORP evaluation as a structured system:

  * patterns
  * coordination gradient
  * implementation modes
* Validate whether the qualitative logic is internally coherent when made explicit
* Compare original implicit logic model vs enriched pattern/Bayesian model

**Phase 2: Projection (UKRN-S design)**

* Represent UKRN-S design questions as interventions (levers)
* Generate “real but non-actual” projections for:

  * pattern activation
  * implementation modes
  * coordination complexity
* Address concrete design questions (e.g., CoP role)

**Phase 3: Generalisation (architectural) — partial only**

* Identify broader learning-system design space:

  * alternative topologies
  * coupling vs separation
  * spawning substructure
* Provide limited worked examples
* Do not attempt full general theory

---

### Out of scope (for this mission)

* Full posterior inference over real UKRN-S deployment data
* General-purpose learning-system framework
* Production-grade UI or platform
* Multi-domain generalisation beyond illustrative cases
* Replacement of qualitative analysis (this is augmentation, not substitution)

---

## 4. Completion criteria

This mission is complete when:

### C1. Reconstruction is explicit

* The ORP evaluation can be represented as a structured system:

  * entities (patterns, institutions, levers, etc.)
  * relations (dependencies, aggregation, coordination)
* The qualitative logic is testable (not only described)

### C2. Logic model is validated or revised

* The original implicit logic model is either:

  * confirmed as coherent, or
  * shown to be insufficient (e.g., too linear)
* The enriched model is demonstrably a **strict improvement**

### C3. UKRN-S projections are produced

* At least 2–3 real design questions are encoded as scenarios
* Each produces:

  * pattern-level projections
  * implementation-mode projections
  * uncertainty or sensitivity indication

### C4. Update loop is represented

* The system includes an explicit prior → measurement → update cycle
* The T1 instrument is integrated as a measurement layer

### C5. Argument and model are unified

* The working paper can be rendered as:

  * a linear narrative (route)
  * backed by a structured representation (graph / hypergraph)
* At least one section of the paper is directly supported by executable computation

### C6. Epistemic status is preserved

All outputs are explicitly tagged as:

* empirical (ORP)
* reconstructed
* projected (UKRN-S)
* speculative (general learning systems)

### C7. Minimal working system exists

* Runs locally
* Produces:

  * narrative output
  * scenario summaries
  * at least one diagnostic comparison

---

## 5. Relationship to other missions

### Depends on

* ORP T3 evaluation (completed)
* Existing notebooks (population + scenario models)
* FutonZero infrastructure principles (self-representing systems)

### Enables

* UKRN-S design decision support
* A second research paper on learning-system architecture
* Future missions:

  * posterior updating with real UKRN-S data
  * topology-sensitive learning-system comparison
  * integration with FUTON argument graph / AIF² work

---

## 6. Source material

Primary:

* Working paper draft (patterns, evaluation, design implications) 

Computational:

* `ukrn_population_model.clj` (institution heterogeneity, levers) 
* `ukrn_learning_system.clj` (scenario DAG, forward sampling) 
* `traversable_argument.clj` (graph + waypoint prototype)

Conceptual:

* NPT coding and role analysis
* COM-B survey data
* T1 instrument design

---

## 7. Owner and dependencies

**Owner:** Joe

**Execution agents (later phases):**

* Codex (code generation, refactoring, integration)
* Claude Code (analysis, model design, simulation interpretation)

**Repositories:**

* current `ukrn-demo` repo (local)
* FUTON stack (for longer-term integration)

---

## 8. Plain-language statement

This mission takes the findings from an open research training programme and turns them into a system that can be inspected, tested, and used to explore future designs. It checks whether the original explanation of what worked is actually coherent when made explicit, then uses that structure to project how new programme designs might behave. The goal is not just to describe what happened, but to build a tool that helps design better learning systems.

---

## Exit criterion (IDENTIFY)

A human (you) reads this and agrees:

* the gap is real
* the three-phase structure is correct
* the scope is appropriate
* the completion criteria match what “done” should mean

---

If you want to refine this before handing it off, the next move would be to tighten either:

* the completion criteria (make them more test-like), or
* the phase boundaries (especially how far Phase 3 is allowed to go)

before moving to MAP.
