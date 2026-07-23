# Mission: Pattern-organized strategic mission selection

**Date:** 2026-07-23  
**Status:** DERIVE — typed candidate-projection kernel built dark; direct memory/cascade route now primary  
**Owner:** Joe + Codex  
**Cross-references:** `M-wm-aif-policy-grain-compliance`,
`M-wm-three-factor-mission-value`, `M-action-vocabulary`, p4ng R5/R6/R12/R13/R15/R17''',
futon3c `M-typed-memories`, futon3c `M-shared-memory-control-build-test`

## Finding from the live implementation

The current mission enrichment is not the remembered product
`strategy × central × feasible`. It is the additive blend

```
0.25 central + 0.45 strategic + 0.30 phase-doable
```

followed by completion and operator gates and non-progress decay. The 2026-07-18
handoff explicitly rejected raw multiplication: missions on the declared
strategy spine can have measured centrality zero, so a product erases the very
strategic signal the model was meant to introduce.

That was a correct local repair, but it leaves a theoretical gap. The three
numbers are hand-set engineering features with different meanings; adding them
does not make a generative model, while multiplying them does not make them
probabilities. Neither construction is canonical `E`.

The zero-centrality failure is also diagnostic. It suggests that `central`,
`strategic`, and `doable` were scalar surrogates for outer-loop objects that the
WM had not represented: control concerns, mission dependencies, recalled
episodes, and strategic cascades. The principled replacement should therefore
first reuse the cascade and memory machinery at the slower grain, not seek a
better arithmetic combination of the same three numbers.

## Three policy grains

The reference implementation needs three explicitly different objects:

1. **Strategic landscape:** typed relations among open missions, the p4ng
   control patterns, current control deficits, and evidence-bearing WM
   memories in the Zaif episodic style.
2. **Strategic policy `pi_S`:** a control-pattern cascade that retrieves and
   organizes candidate missions, or a resulting mission/short mission cascade,
   selected from that landscape at the slow R15 timescale.
3. **Tactical policy `pi_T`:** a pattern cascade constructed for the selected
   mission, at the fast R13/R15 timescale.

The selection seams may share algebra,

```
Q(pi_l) proportional-to E_l(pi_l) exp(-G_l(pi_l)/tau_l),  l in {S,T}
```

but they must not share category identities, learned counts, candidate support,
or score semantics.

## Where the present factors retract

| Current field | Principled future role | Not this |
|---|---|---|
| `strategic` | context-conditioned proposal/support from typed mission-to-control-pattern relations | habit `E_S` |
| `central` | structural relevance evidence or a proposal potential | preference by itself |
| phase `doable` | provisional predictor of successful progress; ultimately a likelihood/outcome model | hard feasibility |
| operator/completion gate | reason-bearing policy-support exclusion | a numeric zero in a product |
| non-progress decay | history-sensitive engineering control pending an explicit transition model | negative evidence silently folded into habit |
| selected-mission frequency | strategic habit `E_S(pi_S)` | mission quality |

This resolves the product/additive tension. A future product of experts is
meaningful only if each factor is a positive, calibrated likelihood or declared
potential. Logical feasibility remains a hard support relation. Unknown is
represented as unknown/uncertain, not the number zero; contradiction is an
observed negative. Until those distinctions exist, the live additive blend
keeps its honest engineering label.

## The p4ng control-pattern landscape

Use the paper's high-level patterns as a small control ontology over the build:
R1 belief representation, R2 observation, R3 update, R4 forward model, R5
evaluation, R6 candidate construction, R7 precision, R8 present fit, R9
independent witness, R10 liveness, R11 resource hierarchy, R12 calibration,
R13 temporal depth, R14 commitment, R15 hierarchy, R16 grounded actuation, R17
structure learning and pattern genesis, and R20 interoception.

These are not embedded merely to ask which prose paragraphs are semantically
near. They organize missions by typed relations such as:

- mission `requires-control` R6;
- mission `repairs-control` R15;
- mission `instantiates-control` R16;
- mission `produces-evidence-for` R12;
- mission `blocked-by-control` R9.

The relation itself carries provenance and optional supporting/challenging
memory ids. The resulting hypergraph is the authoritative organization.
Pattern endpoints give direct recall without an embedding: an active
control-pattern cascade retrieves relevant WM episodes, which in turn expose
missions, dependencies, prior outcomes, and counterexamples. A joint embedding
over only missions plus p4ng control-pattern texts is an optional exploratory
candidate-edge generator, not a required part of the architecture. The earlier
observation that the full pattern library clusters tightly in a generic
embedding is therefore not a failure: semantic nearness was being asked to do
typed control-role classification.

`src/futon2/aif/mission_control_graph.clj` is the first dark kernel. It validates
the typed edge shape and projects reason-bearing strategic candidates from
active control patterns. Only `:witnessed` support edges admit candidates;
embedding-generated `:proposed` edges remain visible in audit counts. A
witnessed block removes a mission from support instead of assigning it a
misleading factor zero.

## Direct WM memory and outer-loop cascade

The WM should write its own Zaif-style episodic memories. Each record preserves
the concrete observation or intervention, provenance, time, outcome/witness
status, and endpoints for the mission and control patterns involved. These are
not cached mission scores. They are episodes from which proposal support,
transition evidence, challenges, and pattern genesis can later be derived.

The direct retrieval and selection path is:

```
current WM observation / tripwire / policy-hole
  -> construct an outer-loop cascade of p4ng control patterns
  -> retrieve WM memories at each pattern endpoint
  -> follow witnessed memory/mission/control hyperedges
  -> bounded strategic mission candidates with reasons
  -> predict / score / select pi_S
  -> construct and enact pi_T
  -> independent outcome memory
  -> revise edge confidence, model, and eventually E_S / E_T
```

Pattern-conditioned recall is doing real architectural work here: the p4ng
pattern is the stable retrieval handle, memories are concrete episodes, and
missions are current opportunities to act on that control concern. Memory count
does not become value, and an agent's own attachment does not self-certify it.
The existing cascade mechanisms supply composition, ordering, holes, and
explanations at the outer level; they should be reused before introducing a
parallel scalar mission-value vocabulary.

## Shared substrate: Zaif runners as nomadic workers

This is one infrastructure serving several domains, not a WM memory theory plus
a separate mathematics-memory theory. Zaif runners and the WM should use the
same hypergraph memory contract, bitemporal store, endpoint retrieval,
retraction, provenance, and use/outcome accounting. A mathematics episode and
a WM episode differ in their domain endpoints and witnesses, not in the
mechanics by which they become memories or are recalled through patterns.

In the Deleuze--Guattari idiom, Zaif runners can be treated as nomadic workers:
they move through different problem territories, apply and challenge patterns,
and leave typed traces in the shared substrate. The useful engineering content
of the analogy is mobility without schema fragmentation. Every memory must
still retain author, domain, subject, mission/session, pattern endpoints,
evidence, and witness status, so shared storage does not mean indiscriminate
cross-domain evidence.

This gives a staged experimental ladder:

1. pure schema/projection tests;
2. frequent, relatively cheap mathematics runs by Zaif runners;
3. dark or shadow WM mission-selection runs over the same APIs;
4. comparatively expensive live WM clicks.

The mathematics tier can test domain-general assumptions: whether memories are
written at useful moments, retrieved by later pattern use, cited in action,
corrected or superseded, and composed by cascades. It cannot by itself certify
a WM-specific transition model, mission preference, safety claim, or outcome;
those require independent WM-domain witnesses. Transfer is therefore at the
level of mechanism and calibration method, not automatic transfer of value.

## Build plan

The authoritative cross-repository build/test promotion gates are in futon3c
`holes/missions/M-shared-memory-control-build-test.md`. The stages below state
the WM theory dependencies; they do not independently authorize a live flip.
Shared-regime Phase 1 was accepted live on 2026-07-23: a dark WM episode was
written to the common backend, attached to `p4ng/R15`, and recalled through the
same bounded, domain-isolated client used for a fresh mathematics episode. This
does not change live mission ordering.

### S0 — current-model audit (done)

- Pin the live additive formula and each field's actual consumer.
- Record the historic reason raw multiplication was rejected.
- Keep `mission-value-factor` out of `E_S` and `G_efe`.

### S1 — typed control graph and WM memories (kernel done; corpus open)

- Give each p4ng control pattern a stable id and version tied to the paper.
- Reuse the same backend API and base memory schema as Zaif; add domain-specific
  endpoints and relations rather than a second WM store or forked memory type.
- Define a WM memory projection compatible with the Zaif episode contract:
  concrete body, provenance, observed/valid time, witness state, mission
  endpoints, control-pattern endpoints, and optional predecessor/successor.
- Materialize mission-to-control-pattern edges with relation, status,
  provenance, supporting/challenging memory ids, and timestamps.
- Backfill a small gold corpus beginning with the WM compliance, typed-memory,
  policy-conditioned-EIG, and tripwire missions.
- Store edges in the hypergraph after the file-backed gold is reviewed.

Acceptance: selecting an outer control pattern retrieves at least one concrete
WM episode and exposes its related mission without vector search; a held-out
reviewer agrees with relation type on a preregistered sample; retraction removes
support on the next projection. The same endpoint query and memory-use receipt
must work in both a mathematics run and a dark WM run.

Phase 4 dark acceptance landed on 2026-07-23. The strict three-argument
`mission_control_graph/candidate-projection` now requires concrete, reviewed,
current, independently witnessed War Machine memory bodies for a witnessed
relation to admit a mission. It carries those bodies into the reason rather
than stopping at ids, and audits cross-domain and unwitnessed exclusions.
The two-argument kernel remains unchanged for its earlier schema callers.

The live R15 trace `phase4-live-dark-warm-20260723` retrieved independently
witnessed episode `e-phase4-wm-r15-live-20260723`, projected this mission with
its observation/intervention body and provenance, and normalized a
pending-outcome use receipt through the same Phase 1 contract as Zaif. It
reported `:live-ordering-changed? false`. The reviewed four-area corpus and
blocking-counterexample reductions live in futon3c under
`holes/labs/M-typed-memories/phase4-wm-corpus.edn`.

### S2 — outer-loop control cascades

- Construct p4ng control-pattern cascades from current deficits and policy
  holes using the existing cascade representation and checks.
- Use the cascade as a sequence of typed retrieval queries over WM memories.
- Project reason-bearing mission candidates from the retrieved subgraph.
- Preserve holes explicitly when no witnessed episode or relation supplies the
  needed transition; a hole is not a zero score.

Acceptance: the outer cascade explains why each mission was proposed, which
episodes were recalled, and where evidence or control structure is missing.

### S2b — optional embedding experiment

Only after the direct path works, compare generic embedding, dedicated
mission-plus-control-pattern embedding, and typed lexical/structural proposal
baselines on held-out gold edges. Measure relation recall and precision, not
cluster separation. An embedding may propose edges only; evidence/review
disposes. Failure to beat the non-vector route does not block the architecture.

### S3 — factor semantics and strategic forward model

- Split executable support from probability of useful progress.
- Replace phase-doable lookup with an outcome model learned from independently
  witnessed mission transitions, with uncertainty.
- Test whether centrality contributes anything after control cascades, typed
  relations, retrieved episodes, and explicit dependencies exist. The default
  hypothesis is that it is a lossy surrogate and can be retired.
- Convert strategy fit into a relation-conditioned likelihood/potential with
  explicit missingness.
- Compare the current additive controller against a calibrated log-linear
  product of experts. Do not add epsilon floors to conceal unknowns.

### S4 — strategic policies and `E_S`

- First compare length-1 mission policies honestly.
- Then admit short mission cascades when dependencies make ordering meaningful.
- Learn a separate Dirichlet habit over complete strategic policy identities.
- Report `ln E_S`, strategic score, support reasons, memories used, and the
  counterfactual score-only winner.

### S5 — R15 return path

Tactical outcomes update the next strategic belief/calibration state, not the
same decision retrospectively. Distinguish:

- selection frequency -> `E_S` habit;
- independently witnessed usefulness -> strategic outcome model;
- changed mission-control relation -> hypergraph evidence;
- recurring unrepresented control need -> R17''' pattern genesis.

## Live gate

No live mission-selection change occurs until the typed relation corpus and
factor ablations exist. The current additive model remains the baseline. A
future flip must demonstrate better held-out ranking of independently judged
mission choices, preserve operator/completion exclusions, and keep every term
separately visible in the decision explanation.
