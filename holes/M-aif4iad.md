# M-aif4iad: Equip Institutional Systems with AIF Heads

**Date:** 2026-03-31
**Status:** IDENTIFY (mission proposal)
**Cross-ref:**

* `M-aif-head` (Mission Peripheral AIF head) 
* Corneli (2016) “institutional approach to computational social creativity” 
* UKRN learning system mission (in IDENTIFY)
* futon2 ants AIF proof-of-concept

---

## 1. IDENTIFY

### Motivation

The FUTON stack has begun to equip individual peripherals with AIF heads (e.g., `M-aif-head`), allowing them to:

* consult an explicit generative model
* detect prediction error
* regulate their own transitions
* refuse invalid actions 

Separately, prior work (Corneli 2016) developed a computational interpretation of Ostrom’s Institutional Analysis and Development (IAD) framework, including:

* explicit rule grammars
* monitoring and testing structures
* collective-choice arrangements
* nested enterprises
* the capacity for participants to modify institutions 

However, a discrepancy remains:

* **IAD (2016 work):** rich institutional structure, but weak adaptive dynamics
* **AIF head (current FUTON work):** strong adaptive dynamics, but applied only to individual peripherals

What is missing is a unified system in which:

> institutions themselves possess an explicit generative model, monitor their own performance, and revise their structure when necessary.

This mission exists to close that gap.

---

### Theoretical anchoring

* **Ostrom / IAD:**
  Institutions are defined by rules, roles, action situations, monitoring, and nested governance structures. Viability depends on boundary definition, participation in rule-making, monitoring, and conflict resolution. 

* **Corneli (2016):**
  IAD principles can be rendered computationally via tests, rule grammars, and producer–place–process–product relationships, including the possibility of institution-building by participants. 

* **AIF+ (Friston / FUTON):**
  Systems maintain viability by:

  * forming expectations (generative model)
  * comparing prediction with observation
  * minimising divergence via action or model revision
  * maintaining bounded identity (Markov blanket) 

* **Synthesis premise:**
  IAD provides **institutional topology** (boundaries, roles, rules);
  AIF provides **adaptive dynamics** (prediction, error, update).

* **Core claim:**
  Institutions can be treated as bounded adaptive systems whose topology (rules, roles, channels, nested structures) can be monitored and revised using AIF-like mechanisms.

---

### Scope in

* Define a combined IAD + AIF representation of institutions as:

  * bounded systems with differentiated permeability
  * rule-governed action situations
  * adaptive generative models

* Equip such systems with:

  * explicit expectations about outcomes (capability, coordination, resource use)
  * observation mechanisms (monitoring, tests, feedback channels)
  * prediction error signals
  * revision mechanisms

* Extend beyond parameter tuning to include:

  * **channel adaptation** (open/close/reweight flows)
  * **topology adaptation** (introduce/remove roles, peripherals, or nested structures)

* Demonstrate (at least in simulation or toy systems):

  * detection of institutional failure modes
  * proposal or instantiation of new local structure
  * evaluation of resulting system behaviour

---

### Scope out

* Full general theory of institutions across all domains
* Complete automated institution design
* Integration with all FUTON peripherals
* Economic modelling of incentives (M-cyder territory)
* Full posterior inference on real-world institutional data

---

### Completion criteria

1. **Unified representation**

   * A system that represents:

     * boundaries (who/what is inside/outside)
     * channels (access, contribution, maintenance, feedback)
     * rules-in-use
     * monitoring structures
     * nested enterprises

2. **Generative model for institutions**

   * The system encodes expectations such as:

     * expected capability gains
     * expected coordination behaviour
     * expected resource flows

3. **Prediction–observation loop**

   * For at least one worked example:

     * expected outcomes are specified
     * observed outcomes are computed or simulated
     * divergence is calculated

4. **Adaptive response**

   * The system can:

     * adjust rules or gates
     * adjust channel permeability
     * identify failure modes

5. **Topology revision (core requirement)**

   * At least one example where:

     * failure is diagnosed as structural (not parametric)
     * a new local structure is proposed or instantiated, e.g.:

       * new role (steward, reviewer, coordinator)
       * new channel (feedback loop, contribution path)
       * new nested enterprise

6. **Viability framing**

   * The system distinguishes:

     * viable configurations
     * non-viable configurations (overload, fragmentation, non-learning)

7. **Epistemic clarity**

   * All outputs distinguish:

     * structural description (IAD)
     * adaptive dynamics (AIF)
     * simulated vs empirical vs speculative results

---

### Relationship to other missions

**Depends on:**

* `M-aif-head` (AIF head pattern) 
* 2016 computational IAD work (institutional substrate) 

**Enables:**

* Adaptive institutional design across domains
* UKRN learning-system extension (Phase 3)
* Multi-peripheral AIF systems
* Self-representing institutional architectures

**Successor to:**

* `M-aif-head` (extends from single peripheral → institutional system)

**Sibling:**

* UKRN learning-system mission (domain-specific instance)

---

### Source material

* Corneli (2016): computational IAD and creativity design principles 
* FUTON AIF head work (`M-aif-head`) 
* UKRN working paper and models (patterns, levers, scenarios)
* Ostrom (1990, 2009): institutional design principles

---

### Owner and dependencies

* **Owner:** Joe
* **Primary repo:** futon2 (AIF and modelling layer)
* **Secondary repos:** futon3c (peripherals), futon3b (gates), UKRN demo repo

---

## 2. MAP

Survey questions for the MAP phase:

* [ ] **Q1:** Which elements of the 2016 paper already correspond to:

  * boundaries
  * channels
  * rules
  * monitoring
  * topology change?

* [ ] **Q2:** What is the minimal interface of an AIF head (from `M-aif-head`) that can be lifted from peripheral → institutional scale?

* [ ] **Q3:** How should institutional topology be represented computationally:

  * graph / hypergraph
  * role–rule grammar
  * channel-permeability model?

* [ ] **Q4:** How do we distinguish:

  * parameter failure (tune existing structure)
  * topology failure (missing structure)?

* [ ] **Q5:** What classes of new structure are admissible:

  * roles
  * channels
  * monitoring surfaces
  * nested enterprises?

* [ ] **Q6:** What constraints govern topology revision:

  * boundary integrity
  * role coherence
  * resource constraints
  * conflict resolution?

* [ ] **Q7:** What is a minimal worked example:

  * computational creativity system (from 2016)
  * UKRN learning system (candidate)
  * toy institutional system?

* [ ] **Q8:** How is viability defined:

  * stability of participation
  * persistence of capability
  * absence of overload or collapse?

* [ ] **Q9:** How are prediction and observation defined in institutional terms:

  * expected capability change
  * observed behaviour / outcomes?

* [ ] **Q10:** What is the smallest demonstration of “institution builds new structure”?

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

---

## Compact summary

This mission extends the AIF-head pattern from individual peripherals to institutional systems. It combines IAD’s structural account of institutions with AIF’s adaptive dynamics, enabling systems to monitor their own performance, diagnose structural failures, and revise their topology by introducing new roles, channels, or nested structures when necessary.
