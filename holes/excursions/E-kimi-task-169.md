# E-kimi-task-169 — pattern-stage reading kimi-3-1790890049 (10 patterns)

**Requisition:** in-progress — dispatched 2026-10-01T21:27:29Z to kimi-3 as invoke-1790890049818-29867-973fd4df

Clocked in by pattern-stage-read-loop for kimi-3 on 2026-10-01 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

You are labelling design patterns for a count of what stage of an Active Inference control loop Joe's work touched. Label each pattern below from ITS OWN TEXT only (IF / HOWEVER / THEN / BECAUSE, else the `! conclusion`), never from its filename or directory.

kind (one):
- practice: a way of working a person or agent performs (review, plan, record, route, check).
- subject: what the work is about; content, a design decision, a domain technique (math formalisation techniques are always subject, even if imperative).
- mixed: genuinely both.

stage (one), with the gloss the existing 689 labels use:
- perceive: observes or exposes state
- believe: records, represents, or revises what is known
- evaluate: weighs consequences, options, or limits
- select: sets a choice, priority, or admission condition
- act: implements or executes the work
- assurance: makes a claim independently checkable or preserves its trace
- coordination: routes work, roles, or communication between parties
- none: the text states no control-stage operation (a lookup/encoding record, a bare meta-tag)

stage-mode: functional (the pattern PERFORMS that stage), topical (it is ABOUT that stage), or functional-and-topical.
confidence: high (clear from the text), medium, low.
node: optional, only if the text itself names an R-node (e.g. "R9"); else omit.
quote-field: IF, HOWEVER, THEN, BECAUSE or CONCLUSION.
quote: copy a span of 6-40 words EXACTLY from that field of the source shown (it is checked character for character after collapsing whitespace). Do not paraphrase, do not add ellipses.
rationale: one sentence: why this stage, citing the quote.

Answer: write /tmp/claude17/pattern-stage-read/kimi-3-1790890049.answer.json as a JSON array, one object per pattern:
  {"id": ..., "kind": ..., "stage": ..., "stage-mode": ..., "confidence": ..., "node": optional, "quote-field": ..., "quote": ..., "rationale": ...}
Rewrite the file after each pattern and check it parses: python3 -c "import json;print(len(json.load(open('/tmp/claude17/pattern-stage-read/kimi-3-1790890049.answer.json'))))"
Do all 10. Do not write anywhere else, do not commit, do not publish. Nobody needs belling; end with one line: the answer path and the count.


---
## software-design/agent-command-pattern
source: futon3/library/software-design/agent-command-pattern.flexiarg

```
@flexiarg software-design/agent-command-pattern
@title Agent Command Pattern
@audience futon developers, CS students, pattern agents
@tone analytic
@style design-pattern
@sigils [📥/井 👘/示]

! conclusion: 
  Encapsulate every such operation as a Command value that carries `:id`, parameters, provenance, capability tags, and optional `undo!`, and run it only through an execution pipeline (local worker, remote service, or threaded pool). Commands can be persisted, reordered, decorated, and routed to specialised executors such as a git agent or math agent.

  + context: You are defining a reliable execution pipeline for agent actions.

  + if:
    Agents perform repo scans, graph updates, and external API calls by invoking ad hoc functions with no shared interface, so actions cannot be queued, audited, or retried safely.

  + however:
    Higher-layer orchestration needs side-effectful work to travel through queues, logs, schedulers, and undo ledgers without each caller reinventing a protocol.

  + then:
    Encapsulate every such operation as a Command value that carries `:id`, parameters, provenance, capability tags, and optional `undo!`, and run it only through an execution pipeline (local worker, remote service, or threaded pool). Commands can be persisted, reordered, decorated, and routed to specialised executors such as a git agent or math agent.

  + because:
    A uniform command abstraction gives the stack a shared action language: histories become replayable, undo/redo is tractable, thread pools can execute arbitrary work safely, and schedulers can move labour across machines without touching implementation internals.

  + next-steps:
    next[Log one concrete instance of this pattern in the futon3 ledger.]
```


---
## storage/startup-integrity-gate
source: futon3/library/storage/startup-integrity-gate.flexiarg

```
@flexiarg storage/startup-integrity-gate
@title Startup Integrity Gate
@sigils [🌅/久]
@keywords startup, integrity, availability, diagnostics
@audience storage maintainers, futon1a builders
@tone formal-analytic
@factor Keen investigation (dhammavicaya)
@references [stack-coherence/evidence-ledger storage/all-or-nothing-startup]
@status [status[proposed] evidence[holes/missions/M-futon1a-evidence.md#L91] evidence[holes/missions/M-futon1a-evidence.md#L137]]
@provenance no grounding source found for this pattern; deterministic search receipt: runs/L7-no-source-check.edn (L6-L10 sweep, 2026-09-05)

! conclusion: 
  if you want fast availability but also integrity, gate startup on full rehydration and explicit diagnostics

  + context: futon1 added XTDB sync and startup diagnostics to prevent stale state from serving.

  + IF:
    you want the system to start quickly

  + HOWEVER:
    you also need integrity and full rehydration correctness

  + THEN:
    block startup until rehydration and invariant checks pass, and emit precise diagnostics on failure

  + BECAUSE:
    partial startup created hidden inconsistencies and required repairs

  + NEXT-STEPS:
    next[Define the rehydration checklist and required diagnostics output.]
    next[Add failure-mode tests for incomplete rehydration.]
```


---
## system-coherence/argue-empirically-not-persuasively
source: futon3/library/system-coherence/argue-empirically-not-persuasively.flexiarg

```
@flexiarg system-coherence/argue-empirically-not-persuasively
@title Argue Empirically, Not Persuasively
@keywords argue-phase, empirical-claim, must-follow, reasonable-assumptions, bootstrap-discipline, futon-mission-lifecycle, may-fostering
@audience system designers, mission authors, working-paper drafters, ARGUE-phase reviewers
@tone technical
@style pattern
@references [futon-theory/futonic-logic, futon-theory/mission-lifecycle, writing-coherence/unearned-essentially, writing-coherence/hedged-lift]


@see-also system-coherence/single-seed-results-need-multi-seed-validation
! conclusion: At any stage where claims are first being put on the record — bootstrap stages of a system, ARGUE-phase missions, foundational sections of papers — claims must be derived from reasonable assumptions and present-tense observations, not speculated forward as downstream effects.  "May foster X" is not a claim; it is a hope dressed as one.  The discipline is: a claim belongs at the bootstrap if it follows from the bootstrap's own action under reasonable assumptions, not if it depends on a chain of effects the bootstrap has not yet produced.  The remedy when such a claim is detected is to delete it (most often), demote it to an open question or future-work note (sometimes), or replace it with the what-must-follow restatement it was gesturing at (rarely — usually the speculation was masking the absence of any derivable claim).

  + context: AI drafts and human first-drafts both tend to dress speculation as claim.  The hedge "may", the modifier "potentially", the verb "foster" or "could lead to" — each does argumentative work without paying for it.  In ARGUE-phase missions and in bootstrap-stage system descriptions, speculative claims pile up because the writer is working at the edge of what the system has done so far, and the speculation is filling space that empirical claim cannot yet fill.  Distinct from `unearned-essentially` (about unsupported strength claims) and `hedged-lift` (about hedging that does the lifting): here the failure is *temporal* — the claim depends on stages the document has not yet brought into being.

  + IF:
    The pattern operates on the axis: claim ambition ↔ what the action under reasonable assumptions actually delivers.
    Irreducible: a claim either follows from the action under reasonable assumptions, or it depends on a chain the document has not yet built.  No amount of hedging changes which of these is true.

  + HOWEVER:
    Real costs: a strong document may say less in early sections than the writer wants — empirically derivable claims at bootstrap are often modest and obvious.  Restraining the claim to the empirical scope at hand can feel anticlimactic; permitting speculation feels generous, but creates load-bearing reliance on claims the document has not paid for.
    Active dynamic: the temptation grows whenever the writer wants the early section to feel weighty.  The cure for the section feeling thin is not speculation; it is moving more empirically derivable content into the early section, or accepting that the section is short.

    + FAILURE-MODES:
      - May-fostering: "may foster" / "could lead to" / "has the potential to" — each is the present participle of a verb the document has not earned.
      - Downstream borrowing: claims at stage N depend on stages N+1 or N+2 having occurred; when challenged, the writer points forward rather than at what the action has produced.
      - Hedge as warrant: a hedge ("perhaps", "in principle") is doing the work an argument would do — the reader is supposed to read the hedge as care rather than as the absence of derivation.
      - Persuasion-by-aspiration: the claim is rhetorically attractive and the writer treats this attractiveness as warrant.

  + THEN: Delete or restrict the claim; replace with what must follow; or demote to open question.
    + COMPOSITIONS:
      (1) Delete and stop: the simplest move.  If the claim does not follow from what the action does, remove it.  The remaining sentence is often short and sufficient.  "This produces trainer-level visibility." is an entire claim.
      (2) Restate what must follow: replace "may foster X" with the empirical claim it was reaching for.  If sharing evidence "may foster ground-up demand for community infrastructure", what *must* follow under reasonable assumptions is that trainers see findings; the demand-for-infrastructure claim depends on additional unbuilt machinery and should not be made here.
      (3) Demote to open question: if the speculation flags a genuine downstream variable the work needs to track, move it to a separate "open questions" or "what we will be watching" section.  The demotion preserves the writer's intuition without letting the speculation pose as a derived claim.
      (4) Audit by must-follow test: read the claim and ask, "what would have to be the case for this to follow from the action just described, under reasonable assumptions?"  If the answer requires additional steps the document has not specified, the claim is speculative.  If the answer is "only the action just described, and nothing more", the claim is empirical and stays.

    + CHECK:
      Engaged when every claim in the section can be derived from the action just described, under reasonable assumptions, with no appeal to stages or machinery the document has not yet built.  Failing when claims use "may", "could", "has the potential to", or "may foster" as warrant, or when the writer would, if challenged, defend the claim by pointing forward rather than by pointing at what the action makes inevitable.
```


---
## t4r/exec-summary
source: futon3/library/t4r/exec-summary.flexiarg

```
@arg t4r/exec-summary
@sigils [🚴/了]
@title Executive Summary
@audience research funders, doctoral-training consortia, university R&I leads
@tone analytic-visionary
@style Clear, concrete, funding-application English
@keywords collective-intelligence, doctoral-training, reflexivity, infrastructure
@up t4r/main-case
@next t4r/rationale

! conclusion: talk4real reimagines doctoral training as a shared experimental learning infrastructure that continually refines itself through reflexive, collective practice. [🏡/工]

  + context: You are framing the executive summary as a compact, inspectable pattern.

  + IF:
    The proposal must persuade funders while staying aligned with the pattern canon.

  + HOWEVER:
    Narrative prose can obscure the reasoning and make claims drift.

  + THEN:
    Anchor the summary in explicit pattern logic before drafting the final prose.
    Apply a REPL-style cycle—read, evaluate, print, loop—so ethnography, computational analysis, design, and collective reflection remain linked.
    Run coordinated doctoral projects that instantiate the same learning architecture in distinct domains.
    Treat each doctoral scholar as both a researcher and an agent of collective intelligence.

  + BECAUSE:
    Most doctoral programmes operate within disciplinary or thematic silos, limiting cross-boundary learning and system-level improvement.
    Collective-intelligence frameworks can capture feedback between researchers, institutions, and communities so research systems learn from their own activity.
    The model is structured by ethical reflexivity (co-authored representation), lightweight infrastructure (portable method), and polyphonic governance (synthesised voices).
    Related precedents include initiatives such as realtalk@MIT, PlanetMath.org, and Peeragogy.

  + NEXT-STEPS:
    next[Refine the exemplars and verify each claim against the supporting pattern set.]
    next[Insert 1-2 current, funder-recognisable exemplars of collective-intelligence infrastructure.]
```


---
## translation/declare-what-is-lost
source: futon3/library/translation/declare-what-is-lost.flexiarg

```
@flexiarg translation/declare-what-is-lost
@title Declare What The Translation Does Not Preserve
@keywords reductions, loss, approximation, conditions, honesty
@audience translators, readers of translated artefacts
@tone technical
@style pattern
@status draft 2026-09-16 (claude-7; shared by contracts and pattern-interpretation)
@references [contracts/declare-the-reductions pattern-interpretation/put-uncertainty-in-theta translation/declare-what-is-preserved]
@see-also hygiene/settle-with-meters
@how list alongside every translation: carriers changed (reals to doubles); choices the source leaves open and the translation fixes (tie-breaks); content dropped (a pattern's BECAUSE, context and evidence; a proof's generality); and conditions under which the preserved property holds (closed-world facts). Losses found in review are added before acceptance.
@violation-signature a contract with no reductions; an interpretation presenting a guard as "the pattern"; agreement tests that quietly fix the conditions they need.
! conclusion: Each translation states what it changes, fixes, drops, and requires for its equivalence to hold.

  + context: Every translation in this stack loses something.

  + IF:
    A translated unit is published or used.

  + HOWEVER:
    Declaring losses reads as weakness.

  + THEN:
    Declare them with the translation.

  + BECAUSE:
    A reader can only rely on a translation whose limits they can see.

  + evidence: exact belief reductions (a)–(h); acting-order strong-Kleene closed-world condition (r12); interpretation drops BECAUSE and evidence, and routes priors into θ.
```


---
## vsat/reflective-container
source: futon3/library/vsatlas/vsat-vsatlas.multiarg

```
@arg vsat/reflective-container
@title Structured Reflective Container
@sigils [🍵/了]
! conclusion: VSAT’s scene + annotation structure doubles as a reflective container that nudges contributors toward meaning-making.
  + context: You are supporting reflective practice in workshop settings. This does not apply to sessions that explicitly avoid reflective practice.
  + IF: Workshops want participants to articulate significance, not just events.
  + HOWEVER: Unstructured tools yield diffuse, shallow narratives.
  + THEN: Use VSAT’s scaffolds to sequence key moments and their interpretations.
  + BECAUSE: Reflection improves coherence and depth.
  + STATUS: status[blocked]
    blocked-by[vsat/reflection-evidence]
  + NEXT-STEPS: next[Collect workshop notes showing reflection gains and log them in futon3.]
    next[Define a reflection-gain signal (e.g., pre/post reflection quality rubric).]
```


---
## vsatelier/cluster-as-agenda
source: futon3/library/vsatelier/cluster-as-agenda.flexiarg

```
@flexiarg vsatelier/cluster-as-agenda
@title Cluster as Agenda Item
@audience facilitators, community organisers, deliberation designers
@tone analytic
@style design-pattern
@sigils [📋/议]
@references [vsatlatarium/cluster-as-story vsatlas/facilitation-scaffolds vsatlas/community-problem-solving-substrate]
@status [status[sketch]]

! conclusion: 
  Allow participants to flag spatial clusters or arc concentrations as
  agenda items so that patterns visible in the constellation become
  inputs to structured deliberation.

  + context: You are bridging spatial perception and collective decision-making. This does not apply when the constellation is used purely for exhibition or archival purposes.

  + IF:
    Participants notice a recurring pattern — several stories linked by
    causal arcs, a dense cluster around a shared harm, an isolated story
    that should be connected.

  + HOWEVER:
    Noticing is not the same as acting. Without a mechanism to capture
    the observation and route it to a deliberation process, spatial
    insight dissipates when the viewer looks away.

  + THEN:
    Provide a "flag this pattern" gesture (click, long-press, voice,
    annotation) that captures the cluster, its constituent stories and
    links, and a free-text framing question. The flag becomes an agenda
    item in a facilitation queue visible to stewards.

  + BECAUSE:
    Agendas grounded in visible spatial patterns carry their evidence
    with them. The facilitator does not need to re-explain the
    observation — they can point at it.

  + NEXT-STEPS:
    next[Define the minimal data a flag must carry (cluster IDs, framing question, flagger identity).]
    next[Prototype a flag gesture in one projection (VR controller, web click, or terminal command).]
    next[Design how flags surface in the stewardship layer — queue, dashboard, or meeting agenda.]
```


---
## vsatlas/audience-shift
source: futon3/library/vsatlas/audience-shift.flexiarg

```
@flexiarg vsatlas/audience-shift
@title Audience Shift
@audience media theorists, civic technologists
@tone analytic
@style design-pattern
@sigils [💤/也 🐜/上]
@provenance no grounding source found for this pattern; deterministic search receipt: runs/L7-no-source-check.edn (L6-L10 sweep, 2026-09-05)

! conclusion: 
  Design for peer-to-peer audiences by enabling reciprocal storytelling loops and navigable inter-story connections.

  + context: You are rethinking platform design so audiences become active participants who respond to and connect with each other’s stories, rather than remaining passive viewers. This does not apply to one-way broadcast archives where participation is not a goal.

  + IF:
    The platform centres display logic so stories attract passive spectators.

  + HOWEVER:
    Communities require environments where participants recognise and respond to one another as peers.

  + THEN:
    Design for peer-to-peer audiences by enabling reciprocal storytelling loops and navigable inter-story connections.

  + BECAUSE:
    Shifting the audience from spectators to participants restores agency, dialogue, and collective problem-solving.

  + NEXT-STEPS:
    next[Prototype one reciprocal loop (respond, remix, or co-author) in the UI.]
    next[Measure how often participants move from viewing to contributing.]
    next[Define an apply signal (e.g., >60% sessions are view-only) and an avoid signal (e.g., high safety risk for public replies).]
```


---
## vsatlas/isolarion-drift
source: futon3/library/vsatlas/isolarion-drift.flexiarg

```
@flexiarg vsatlas/isolarion-drift
@title Isolarion Drift
@audience platform architects, collective narrative designers
@tone analytic
@style design-pattern
@sigils [🐊/田 🎶/白]
@provenance no grounding source found for this pattern; deterministic search receipt: runs/L7-no-source-check.edn (L6-L10 sweep, 2026-09-05)

! conclusion: 
  Introduce a manifold layer where canonical charts overlap voluntarily so resonances can emerge without rewriting originals.

  + context: You are considering introducing a voluntary overlap or annotation layer that enables shared meaning to be made without rewriting originals. This does not apply when stories are already intentionally merged into a single narrative.

  + IF:
    Contributors craft narrative dioramas in isolation and the stack offers no cross-story relation layer.

  + HOWEVER:
    Groups need shared meaning rather than atomised testimony.

  + THEN:
    Introduce a manifold layer where canonical charts overlap voluntarily so resonances can emerge without rewriting originals.

  + BECAUSE:
    Relational topology converts isolated narratives into a shared world with emergent coherence.

  + NEXT-STEPS:
    next[Define the minimal overlap annotation schema and one pilot use case.]
    next[Prototype an opt-in overlay view that preserves original charts.]
    next[Define a decision signal (e.g., repeated cross-story themes with no shared map) and a failure case to retire the overlay.]
    next[Consolidate with vsatlas/non-destructive-relational-layers and retire this pattern once merged.]
```


---
## vsatlas/offer-ladder
source: futon3/library/vsatlas/offer-ladder.flexiarg

```
@flexiarg vsatlas/offer-ladder
@title Offer Ladder
@audience funders, programme designers, institutional partners
@tone analytic
@style design-pattern
@sigils [🌞/六 🔹/只]
@provenance no grounding source found for this pattern; deterministic search receipt: runs/L7-no-source-check.edn (L6-L10 sweep, 2026-09-05)

! conclusion: 
  Define a simple offer ladder (intro workshop → multi-session pilot → annual programme) where each rung has a clear scope, price band, and expected outcome.

  + context: You are translating 'story manifold' capabilities into a small set of concrete (purchasable) offerings that institutions can understand, budget for, and adopt. This does not apply when procurement requires a single bundled offer.

  + IF:
    A story platform with rich theory but no articulated “offers” risks being seen as a research toy rather than a service.

  + HOWEVER:
    Communities, NGOs, and universities buy concrete packages—workshops, courses, facilitation, reporting—rather than abstract infrastructures.

  + THEN:
    Define a simple offer ladder (intro workshop → multi-session pilot → annual programme) where each rung has a clear scope, price band, and expected outcome.

  + BECAUSE:
    A visible ladder of offers turns VSATLAS from an idea into a portfolio of bookable engagements that can recover costs and demonstrate real-world demand.

  + NEXT-STEPS:
    next[Draft the three-rung ladder with scope, price band, and outcomes.]
    next[Test the ladder with one partner and refine based on feedback.]
    next[Define adoption signals (e.g., partner picks a rung without extra explanation) and revise if absent.]
    next[Define where interpretation sessions sit in the ladder (e.g., a mid-tier offer).]
```
