# E-kimi-task-163 — pattern-stage reading kimi-3-1790889926 (10 patterns)

**Requisition:** completed — 2026-10-01T21:27:24Z, job invoke-1790889926894-29860-0e85bbdb, state done

**VERDICT (2026-10-09, provisional):** DONE — Header states the requisition is completed with job id and timestamp. _(WM status classification by zai-5, high confidence; not yet confirmed by the author.)_

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

Answer: write /tmp/claude17/pattern-stage-read/kimi-3-1790889926.answer.json as a JSON array, one object per pattern:
  {"id": ..., "kind": ..., "stage": ..., "stage-mode": ..., "confidence": ..., "node": optional, "quote-field": ..., "quote": ..., "rationale": ...}
Rewrite the file after each pattern and check it parses: python3 -c "import json;print(len(json.load(open('/tmp/claude17/pattern-stage-read/kimi-3-1790889926.answer.json'))))"
Do all 10. Do not write anywhere else, do not commit, do not publish. Nobody needs belling; end with one line: the answer path and the count.


---
## math-formalization/separate-proof-transfer-from-artifact-replay
source: futon3/library/math-formalization/separate-proof-transfer-from-artifact-replay.flexiarg

```
@flexiarg math-formalization/separate-proof-transfer-from-artifact-replay
@title Separate Proof Transfer from Artifact Replay
@sigils [🧱/门]
@keywords proof transfer, artifact replay, benchmark leakage, commit pointer, shared object store, independent derivation, recompile, evaluation validity, provenance
@audience formalizers, benchmark designers, proof reviewers
@tone protocol
@factor Discernment (paññā)
@provenance no grounding source found for this pattern; deterministic search receipt: runs/L8-no-source-check.edn (L6-L10 sweep, 2026-09-05)

! conclusion:
  Classify reuse of an already-proved diff as artifact replay, not as evidence that a proof strategy was independently understood or reconstructed.

  + context:
      A solver memory names both a proof route and a reachable commit or patch,
      and a later formalizer closes the same target after retrieving it.

  + IF:
      The later artifact is content-identical to the earlier proof, or the
      transcript shows that the earlier diff was applied wholesale.

  + HOWEVER:
      Recompiling and auditing the replayed artifact is genuine verification:
      it establishes that the proof still typechecks, preserves the statement,
      and meets the axiom and scope gates. It does not establish that the route
      description alone was sufficient for independent derivation.

  + THEN:
      Record two separate outcomes. Credit artifact transfer and independent
      verification as successful. Mark strategy-learning or independent-solve
      conclusions as unmeasured. If the evaluation aims to test proof transfer,
      withhold direct commit or patch locators, isolate object stores, or score
      reconstruction separately from replay.

  + BECAUSE:
      A reachable proof object collapses a search-and-formalization task into a
      patch-application task. Both can be useful, but they measure different
      capabilities and costs.

  + NEXT-STEPS:
    next[Compare the resulting tree or target file against the source proof artifact.]
    next[Inspect the transcript for wholesale diff, cherry-pick, or commit-pointer use.]
    next[Report verification success separately from independent derivation evidence.]
```


---
## pattern-mining/probe-the-claimed-property-not-the-acceptance-proxy
source: futon3/library/pattern-mining/probe-the-claimed-property-not-the-acceptance-proxy.flexiarg

```
@flexiarg pattern-mining/probe-the-claimed-property-not-the-acceptance-proxy
@title Probe the Claimed Property, Not the Acceptance Proxy
@keywords gate, acceptance, proxy, trust, dependency-closure, vacuity, axioms, transitive, validation
@audience pipeline authors, proof engineers, artifact reviewers
@tone corrective
@style pattern
@grade technique
@provenance slice-1 compression mining (2026-08-12): 22 marks, 17 read transcripts, 7 distinct problems (b97A02 b98A03 b97J01 b95J01 b00J02 b96J01 b01J02) — but slice-1 reads were NOT blind (all shared one accumulating zai invoke-fn context; runbook slice-2 root-cause correction), so those counts are inflated and are NOT what this candidate rests on. LOAD-BEARING EVIDENCE: slice 3 — the first clean-context run — independently re-surfaced this cluster ("probe the transitive axiom/trust closure") from 5 distinct math problems across 9 transcripts.
@review claude-2 2026-08-12 APPROVE, on the slice-3 corroboration rather than the slice-1 counts. HOWEVER is self-aware in the way that matters ("calling an incomplete probe complete merely creates a new proxy"). Dedupe spot-checked by reading both claimed neighbours: agent/evidence-over-assertion is about grounding claims at all; war-machine/operational-not-decorative requires validation — neither names the proxy-vs-property error. Genuine gap.
@class process
@domain process
@family-note Moved to pattern-mining/ 2026-08-13 on Joe's instruction, from process-coherence/ (and earlier mislabelled "agency"). It is a PROCESS pattern — about acceptance gates in the pipeline we ourselves run — and must NOT enter the mathematics experiment's retrieval pool. Rationale is measurement, not tidiness: the V3 programme's P2 falsifier turns on the ratio of regulative to substitutive fingerprinted uses, so a math runner surfacing this pattern would be a regulative hit caused by pool composition rather than load-bearingness, and P2 would miscount it. See futon3c/docs/pattern-retrieval-architecture.md section 9.
@status registered into pattern-mining/, review-approved by claude-2 2026-08-12. NOT assayed — and the stage-3 teachability assay as defined (re-run a SOLVED math problem on zai-1 with and without the deposit) is INAPPLICABLE to a process pattern. The pattern-mining family's admission standard is an OPEN QUESTION for Joe; see the family README. It is out of the math experiment by construction, so the assay gate's purpose — protecting the experiment from unvalidated supply — is met regardless.

! conclusion:
  Before accepting an artifact, translate the promised quality into a direct
  probe and run that probe over the whole scope on which the promise depends;
  do not accept a convenient local proxy as evidence for a transitive or
  semantic property.

  + context:
    A pipeline has a cheap success signal: a file compiles, a source contains
    zero placeholders, a hash matches, a row says approved, or a declaration
    was not skipped.  The actual promise is stronger: the proof is axiom-clean,
    the statement is non-vacuous, dependencies are clean, or the approved
    declaration is still the one under review.

  + IF:
    The acceptance signal checks a representation of the property rather than
    the property itself, especially when imports, definitions, historical
    state, or human verdicts contribute to the claim.

  + HOWEVER:
    Direct probes can be expensive, and some properties have no complete
    decision procedure.  A dependency-closure audit may require building
    previously unbuilt artifacts; a semantic vacuity check remains heuristic.
    Calling an incomplete probe complete merely creates a new proxy.  State the
    probe's coverage, retain explicit unknown/skipped outcomes, and reserve
    acceptance for the portion actually checked.

  + THEN:
    Write the promised property as a checkable question; identify its trust
    closure; run the strongest available direct probe on every member of that
    closure; test the probe against known-good, known-bad, and multi-defect
    fixtures; and make uncovered or skipped members visible non-success states.
    Keep cheap proxies only as triage filters before this gate.

  + BECAUSE:
    Local success and global trust coincide only when the property is local.
    A compiler run cannot by itself establish axiom cleanliness, source grep
    cannot see imported placeholders, and a matching top-level hash cannot see
    unhashed meaning changes.  Probing the promised property prevents the gate
    from certifying a different, easier claim.

  + NEXT-STEPS:
    next[Write down the exact property each current acceptance signal claims to certify.]
    next[For each non-local property, enumerate the dependency closure and explicit unknown states.]
    next[Seed the direct probe with one known-good, one known-bad, and one multi-defect fixture.]
```


---
## plos-npt-with-small-n/quote-table-not-quote-flood
source: futon3/library/plos-npt-with-small-n/quote-table-not-quote-flood.flexiarg

```
@flexiarg plos-npt-with-small-n/quote-table-not-quote-flood
@title Use Extract Tables When Themes Are Parallel
@keywords quotations, extract-table, thematic-analysis, results, training, qualitative
@audience paper authors, qualitative researchers, education researchers
@tone technical
@style pattern
@source-dois [10.1371/journal.pone.0312943 10.1371/journal.pone.0192224 10.1371/journal.pone.0239181]
@references [writing-coherence/one-paragraph-per-typed-claim plos-npt-with-small-n/site-case-data-display]
@provenance no grounding source found for this pattern; deterministic search receipt: runs/L10-no-source-check.edn (L6-L10 sweep, 2026-09-05)

! conclusion: When a Results section presents several parallel themes, use illustrative extract tables to carry representative participant voice and keep the prose available for interpretation.  The table should not be a quote dump; each row should align theme, subtheme or claim, participant/source, and what the extract demonstrates.

  + context: The training/education PLOS exemplar uses one table of illustrative data extracts per theme, while other qualitative exemplars use participant and theme tables to prevent prose from becoming a long chain of quotations.  This is especially useful when the findings are role features, barriers, or implementation factors rather than a narrative case.

  + IF:
    The pattern operates on the axis: quotation as evidence display <-> quotation as prose interruption.
    Irreducible: qualitative claims need participant voice, but too many long quotes inside prose can hide the analytic claim they are meant to support.

  + HOWEVER:
    Real costs: tables can make qualitative findings feel schematic, and moving quotes out of prose can reduce narrative texture.  Keep decisive or surprising quotes in prose; move parallel confirmatory extracts to tables.
    Active dynamic: authors often add quotes whenever a claim feels under-supported.  Past a point, each quote adds length but not clarity.

    + FAILURE-MODES:
      - Quote-flood: each theme paragraph becomes claim, quote, quote, quote, with little analytic synthesis.
      - Anonymous-evidence: extract tables omit participant role or case context needed to interpret the quote.
      - Decorative-extract: the quote illustrates the topic but not the specific analytic claim.
      - Table-without-prose: the table carries all evidence, while Results prose merely restates theme names.

  + THEN: Separate interpretation from parallel evidence display.
    + COMPOSITIONS:
      (1) Theme-extract table.
          Columns: theme, analytic claim, participant/source, extract, interpretive note.
          Effect: lets the prose state and compare findings.
      (2) Role-balanced extracts.
          Columns: theme, role/group, extract, contrast.
          Effect: useful when professional role or participant type matters.
      (3) One-prose-quote-per-turning-point.
          Schema: {:theme <x> :kept-in-prose <quote that changes interpretation> :moved-to-table <parallel support>}
          Effect: preserves qualitative texture without flooding paragraphs.

    + CHECK:
      Engaged when quotes are easy to inspect and the prose still performs analysis.  Failing when the Results section becomes a transcript collage or when extract tables lack analytic labels.
```


---
## vsat-next/narrative-arc-infographics
source: futon3/library/vsatlas/vsat-vsatlas.multiarg

```
@arg vsat-next/narrative-arc-infographics
@title Narrative Arc Infographics
@sigils [👓/只]
! conclusion: Add lightweight infographics that visualise how a VSAT story moves through beats, enabling rapid critique or coaching.
  + context: You are giving mentors a quick visual read on story pacing and structure. This does not apply when mentors already have time for full review.
  + IF:
    Mentors need to review many stories quickly.
  + HOWEVER:
    Reading each scene graph manually is slow.
  + THEN:
    Auto-generate arc diagrams (tension, pacing, scene types) from the story graph.
  + BECAUSE:
    Visual summaries surface strengths and gaps without flattening the work.
  + STATUS:
    status[ready]
    evidence[https://github.com/BrookesUniversityLearningResources/vsat/issues/207]
  + NEXT-STEPS:
    next[Capture a short before/after walkthrough showing how facilitators use the generated arc view.]
    next[Define a success signal (e.g., mentor review time cut by half) and a falsifier (e.g., arc view misleads critiques).]
```


---
## vsatelier/projection-independence
source: futon3/library/vsatelier/projection-independence.flexiarg

```
@flexiarg vsatelier/projection-independence
@title Projection Independence
@audience platform architects, pattern library maintainers
@tone analytic
@style design-pattern
@sigils [🪞/映]
@references [vsatlatarium/dome-projection vsatlatarium/embodied-browsing vsatlatarium/sky-disk-continuum]
@status [status[sketch]]

! conclusion: 
  Deliberation patterns should be defined independently of any specific
  rendering technology so the same structures work across VR domes,
  AR overlays, 2D graphs, hypertext pages, and terminal interfaces.

  + context: You are designing for longevity across hardware generations and interaction paradigms. This does not apply when the system is permanently bound to a single platform.

  + IF:
    The patterns of connection, clustering, anthology, and deliberation
    outlive the specific technology used to render them.

  + HOWEVER:
    Coupling patterns to a particular renderer (A-Frame, Three.js, a
    specific headset SDK) means they die when the technology does. The
    Quest 2 is already dated; the Steam Frame is not yet shipping; AR
    glasses are nascent; Nelsonian hypertext requires no 3D at all.

  + THEN:
    Define the deliberation layer (flags, decisions, commitments, return
    loops) as data structures and interaction contracts, not as rendering
    code. Each projection surface implements the contract:
    — VR dome: flag by pointing, decision appears as a glowing node
    — AR glasses: flag by gaze, decision pins to a physical wall
    — 2D web: flag by click, decision appears in a sidebar
    — Terminal: flag by command, decision appears in a ledger
    The pattern library describes the contract; implementations vary.

  + BECAUSE:
    Projection independence is how a pattern library outlives its first
    implementation. The VSATLATARIUM flexiargs describe one projection;
    the VSATELIER flexiargs describe what must be true across all of them.

  + NEXT-STEPS:
    next[Extract the interaction contracts from the VSATLATARIUM patterns into projection-neutral specs.]
    next[Prototype one deliberation pattern (cluster-as-agenda) in two different projections to verify independence.]
    next[Evaluate futon4 (Nelsonian hypertext) as a second projection surface for the same pattern set.]
```


---
## vsatlas/askew-layer
source: futon3/library/vsatlas/askew-layer.flexiarg

```
@flexiarg vsatlas/askew-layer
@title Askew Layer
@audience software architects, narrative system designers
@tone analytic
@style design-pattern
@sigils [🍯/只 🙇/双]
@provenance no grounding source found for this pattern; deterministic search receipt: runs/L7-no-source-check.edn (L6-L10 sweep, 2026-09-05)

! conclusion: 
  Add an interpretive “askew” layer where contributors propose arcs, passages, and conceptual bridges without modifying originals.

  + context: You are designing a narrative system where contributors can explore interpretations and relationships without altering canonical stories, preserving authorial integrity while enabling sense-making. This does not apply when the canonical layer can be edited by mutual consent.

  + IF:
    Canonical stories must be preserved yet contributors need room to explore relations.

  + HOWEVER:
    Forcing links into the canonical layer violates narrative sovereignty and breeds incoherence.

  + THEN:
    Add an interpretive “askew” layer where contributors propose arcs, passages, and conceptual bridges without modifying originals.

  + BECAUSE:
    Interpretive overlays let relational meaning emerge while protecting authorial integrity.

  + NEXT-STEPS:
    next[Define a lightweight "askew" proposal schema and review workflow.]
    next[Prototype one askew bridge on an existing story pair.]
    next[Define apply/avoid signals (e.g., "canon locked" vs "canon editable").]
    next[Record one case where askew prevented a canonical conflict and one where it added noise.]
    next[Consolidate with vsatlas/non-destructive-relational-layers and retire this pattern once merged.]
```


---
## ai4ci/pilot-feasibility
source: futon3/library/ai4ci/pilot-feasibility.flexiarg

```
@flexiarg ai4ci/pilot-feasibility
@title Feasibility — Six-Month Pilot Discipline
@sigils [👭/六 📐/工]
@audience EPSRC reviewers
@tone plain-analytic
@factor Keen investigation (dhammavicaya)
@references [repository-transition/invariants stack-coherence/stack-blocker-detection devmap-coherence/next-steps-to-done]
@why problems/ai4ci-scope-can-outgrow-six-month-pilot
@status [status[ready] evidence[library/repository-transition/invariants.flexiarg:1] evidence[library/stack-coherence/stack-blocker-detection.flexiarg:1]]

! conclusion: 
  A six-month evaluation is feasible because the project is deliberately
  scoped to test whether mid-level reasoning cues can be extracted and
  validated, not to build new AI systems or long-term infrastructure.

  + context: You are defending the six-month timeline by tying scope to specific, trackable activities.

  + IF:
    Repository-transition invariants and stack-blocker detection already
    describe what infrastructure exists and how to keep scope aligned with
    available capacity.

  + HOWEVER:
    Without explicit scope fences, a project framed around “AI and
    explanation” can drift into trying to develop storage, user interfaces,
    or new models, making the schedule unrealistic.

  + THEN:
    Freeze scope to: proof selection, dual-layer annotation (AI proposals
    plus human review), seminar-based evaluation, and reporting; track each
    clause with explicit next-steps so diverging work is visible as scope
    creep.

  + BECAUSE:
    Dhammavicaya (keen investigation) in this context demands evidence that
    the pilot answers a clear methodological question—whether a recoverable
    reasoning layer exists—rather than attempting production-grade systems.

  + NEXT-STEPS:
    next[Map the proof sample, annotation sprints, and seminars onto a simple timeline and publish it in the devmap.]
    next[Record blockers and clears weekly so stack-coherence patterns can demonstrate that the pilot stayed within its declared scope.]
```


---
## buffer-cleaner/exempt-the-in-use
source: futon3/library/buffer-cleaner/exempt-the-in-use.flexiarg

```
@flexiarg buffer-cleaner/exempt-the-in-use
@title Exempt The In-Use, Before And Independent Of Scoring
@keywords exemption, protection, in-use, structural-fact, hard-rule, disposition
@audience hygiene-policy authors, adapter builders, reviewer-gate designers
@tone technical
@style pattern
@status staged in the buffer-cleaner generation (live evaluation: futon2/holes/labs/wm-contract/buffer-cleaner-adapter; protections enforced and tested 2026-09-15); the buffer-cleaner INSTANCE of hygiene/exempt-the-in-use, to which the shared statement moved on 2026-09-16 when the third generation (test-registry) cited it
@references [buffer-cleaner/classify-staleness buffer-cleaner/receipt-then-gate hygiene/exempt-the-in-use]
@see-also apparatus/loudness-is-conserved
@how define the protected class by structural or witnessed facts only (never preferences, never parameters): the cleaner names *scratch*, minibuffers, *Arxana Browser* and checks visible, has-process, modified (any Emacs-truthy value), active-agent, server-clients; inbox zero exempts in-flight work (watched activity) and operator-owned dirt; the registry exempts witnessed warrants (replay, not rerun). Enforce ahead of wiring eligibility so every consumer inherits the exemptions; record the reason on every exemption; re-check at execution time — a receipt's exemption is evidence, not authority.
@violation-signature An exempt-class item appearing in an action receipt; protections enforced only in one collector so a second consumer misses them; an exemption parameterized (a preference weight on a structural fact); a kept item with no recorded reason, making deliberate-preserved indistinguishable from bug-missed; an execution trusting the receipt's exemption without re-checking.
! conclusion: Items demonstrably in use are exempt from a hygiene policy's action by structural or witnessed fact, before and independent of scoring — the exemption is recorded with its reason and re-checked at execution.

  + context: Three generations of the same hygiene neuron run in this stack: the buffer cleaner (generation one, Emacs), inbox zero (generation two, repositories), the test registry (generation three, evidence). Each acts unattended on items that are usually, but not always, disposable.

  + IF:
    A policy will act on items (kill buffers, commit dirt, trust test runs) and some items are demonstrably in use.

  + HOWEVER:
    Scoring the exemption invites boundary erosion: an in-use item with a low prior looks almost disposable, and the policy's threshold drifts toward eating it. And structural facts are wider than booleans — the cleaner learned that buffer-modified-p can return symbols (:autosaved), which naive truthiness reads as false.

  + THEN:
    Define the exempt class by structural or witnessed facts, non-parameterized; enforce it before eligibility so every wiring inherits it; record the reason per exemption (auditable: deliberate vs bug); re-check at execution time (receipts are evidence of a past state, not authority over the present).

  + BECAUSE:
    The score prices uncertainty; the in-use class has none. Folding certainty into a prior wastes the information and risks the one error each generation's history actually recorded — acting on an item someone was using.

  + evidence: Cleaner: the 2026-06-11 *Arxana Essay* near-miss (modified buffer = unsaved operator edits, never killable) named the class before the pattern existed; codex-26's control found active-agent and server-clients declared-but-unchecked until enforced (futon2 f27da45b), and the :autosaved leak (a87d27e6) showed truthiness width is part of the structural fact. Inbox zero: the in-flight discipline — a repo with watcher activity inside the window is never committed under (U59: the sweeper-committed-under-a-live-edit incident) and the compensating commit path re-verifies at execution time rather than trusting the proposal's stamp. Test registry: the witnessed warrant IS the exemption — a registered, sha-matched run is exempt from rerun (replay-not-rerun), and the reviewer's record-check plus spot-check re-verify the warrant rather than trusting the author's label.

  + NEXT-STEPS:
    - When inbox-zero and test-registry gain their own library entries, they cite this pattern (it is their shared spine); promotion out of buffer-cleaner/ follows the three-citations rule — see placement note below.
    - The gated kill consumer, when designed, must re-check every exemption at execution time: protections are not receipts.
```


---
## cascade-construction/separate-construction-from-meaning-review
source: futon3/library/cascade-construction/separate-construction-from-meaning-review.flexiarg

```
@flexiarg cascade-construction/separate-construction-from-meaning-review
@title Have The Owner Of The Model's Meaning Review The Constructed Cascade
@keywords review, meaning, semantics, author-is-not-reviewer, correction
@audience agents constructing cascades, owners of model semantics
@tone technical
@style pattern
@status draft 2026-09-17 (claude-7; claude-4's review of pattern-interpretation, 2026-09-16)
@execution human or independent agent review. Aleatoric in which errors are caught; its value is epistemic.
@epistemic-value resolves semantic claims the constructor cannot resolve alone (interpretation information from the meaning owner).
@done done when each named claim is confirmed or corrected. Reviewing again without new claims adds nothing.
@references [cascade-construction/run-it-on-a-real-case test-registry/judge-adequacy contracts/every-entry-has-a-falsifier]
@see-also hygiene/receipt-then-gate
@how send the constructed cascade and its interpretations to whoever owns the model's meaning, with the specific claims they should check; apply the corrections and record them; keep construction choices (which patterns, which grain) with the constructor unless the review shows they change meaning.
@violation-signature a constructor approving its own semantic claims; review asked for "a look" without naming the claims; corrections applied silently.
! conclusion: The owner of the model's meaning reviews the semantic claims in a constructed cascade, and corrections are applied and recorded.

  + context: Construction and model semantics are owned by different agents.

  + IF:
    A constructed cascade makes claims about what its tokens, guards or parameters mean.

  + HOWEVER:
    The constructor usually understands the design well enough to write plausible claims.

  + THEN:
    Name the claims, send them to the meaning owner, apply and record corrections.

  + BECAUSE:
    Plausible semantic errors survive self-review and propagate into every instance.

  + evidence: claude-4 corrected four claims in pattern-interpretation (futon3 02cbe0e): guards require rather than consume tokens; an effect producing f⁺ over observed f⁻ must be refused; exactness holds only on observed states; present-state uncertainty goes in q0, not C.
```


---
## corps/letting-the-trace-teach
source: futon3/library/corps/letting-the-trace-teach.flexiarg

```
@flexiarg corps/letting-the-trace-teach
@title Letting the Trace Teach (Private Learning → Public-Facing Identity)
@audience CORPS mentors, doctoral supervisors, research stewards
@tone analytic
@style design-pattern
@sigils [📤/文]

! conclusion: Letting the Trace Teach

  + context: You are helping a researcher move private learning into a public-facing identity.

  + if: Doctoral researchers accumulate tacit knowledge but keep it inside informal notes, calls, or unshared materials.

  + however: Without public artefacts their emerging expertise stays invisible to collaborators, employers, and interdisciplinary partners.

  + then: Encourage the release of small, well-scoped artefacts—annotated examples, brief reflections, walkthroughs, explanatory notes—into venues where relevant peers already circulate.

  + because: Public-facing traces signal identity and invite collaboration; they provide low-risk entry points for others to understand, support, or extend the researcher’s work while helping the researcher build confidence as a contributor.

  + next-steps:
+   next[Select one trace-worthy insight from the last month and publish it in a shared venue.]
+   next[Invite one collaborator to respond or extend the artefact.]
```
