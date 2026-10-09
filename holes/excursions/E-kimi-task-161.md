# E-kimi-task-161 — pattern-stage reading kimi-1-1790889916 (10 patterns)

**Requisition:** completed — 2026-10-01T21:26:43Z, job invoke-1790889918489-29858-524d64e4, state done

**VERDICT (2026-10-09, provisional):** DONE — Requisition header states completed with state done. _(WM status classification by zai-2, high confidence; not yet confirmed by the author.)_

Clocked in by pattern-stage-read-loop for kimi-1 on 2026-10-01 (one Kimi task, one excursion, so the seat's
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

Answer: write /tmp/claude17/pattern-stage-read/kimi-1-1790889916.answer.json as a JSON array, one object per pattern:
  {"id": ..., "kind": ..., "stage": ..., "stage-mode": ..., "confidence": ..., "node": optional, "quote-field": ..., "quote": ..., "rationale": ...}
Rewrite the file after each pattern and check it parses: python3 -c "import json;print(len(json.load(open('/tmp/claude17/pattern-stage-read/kimi-1-1790889916.answer.json'))))"
Do all 10. Do not write anywhere else, do not commit, do not publish. Nobody needs belling; end with one line: the answer path and the count.


---
## test-registry/bind-the-subject
source: futon3/library/test-registry/bind-the-subject.flexiarg

```
@flexiarg test-registry/bind-the-subject
@title Bind Subjects To Warrants; Close Incidents Only With Fresher Ones
@keywords subject-index, binding, revalidation, incident, conformance, evidence-not-gate, freshness
@audience validation-service operators, reviewers consuming conformance readouts
@tone technical
@style pattern
@status draft 2026-09-19 (claude-12; implementation futon3c.test-registry.validation, commits 5a4f1a7e, d41f03eb, 224da745; four subject bindings live on the day of writing)
@references [test-registry/warrant-only-the-wires test-registry/rerun-when-the-warrant-fails test-registry/outlive-the-process hygiene/route-to-who-can-act apparatus/evidence-to-disposition-once]
@see-also inbox-zero/escalate-by-who-can-act
@how an append-only subject index maps each wire-subject to its current warrant (rebinding appends; history is kept). An observed failure of warranted behaviour is enqueued as an incident against the subject — at observation time, by the observer, never by a sweep. The incident does not revoke anything: conformance readout reports the subject :revalidation-open, alongside :current, :stale (pinned shas drifted, drift paths named), :unverifiable and :no-warrant. Closing the incident requires a warrant that is (a) the subject's currently bound one and (b) minted strictly after the incident — a rerun from before the observation cannot answer it, and an unbound fresh run hasn't been accepted. All of this is readout, never a gate: nothing in the service halts a machine run (burden of proof sits on any guard, per standing ruling).
@violation-signature an incident that flips a verdict with no fresh run; a close accepted with the old warrant or an unbound one; a conformance readout wired as a precondition of execution; rebinding that rewrites rather than appends.
! conclusion: Subjects bind to warrants in an append-only index; incidents open revalidation that only a currently-bound warrant minted after the incident can close; the whole readout is evidence, never a gate.

  + context: Warrants exist per run; operators need per-wire currency, and failures of warranted behaviour do get observed.

  + IF:
    Someone observes warranted behaviour failing, or asks whether a wire is currently warranted.

  + HOWEVER:
    Without a freshness rule, the cheapest close is to re-cite the warrant that just failed; without the evidence-not-gate rule, the readout drifts into a veto that halts tuning runs.

  + THEN:
    Enqueue the incident, keep the binding, report :revalidation-open, and accept only a fresher currently-bound warrant as closure.

  + BECAUSE:
    The binding records acceptance and the incident records observation; only a new run, registered and accepted, carries information that postdates both.

  + evidence: futon3c data/test-registry-validation/subjects.ednlog (four bindings by 2026-09-19, e.g. WM-01-numerical-operations, EV-uniform-run-record); close-revalidation! refuses :fresh-warrant-not-bound and :warrant-not-fresh with the compared instants in the details.
```


---
## translation/test-by-reproducing-behaviour
source: futon3/library/translation/test-by-reproducing-behaviour.flexiarg

```
@flexiarg translation/test-by-reproducing-behaviour
@title Test A Translation By Whether It Reproduces The Source's Behaviour
@keywords behavioural-equivalence, fixture, recorded-run, falsifier, attribution
@audience translators, reviewers
@tone technical
@style pattern
@status draft 2026-09-16 (claude-7; shared by contracts and pattern-interpretation; the test registry is the identity case, where reproduction is the spot-check)
@references [contracts/every-entry-has-a-falsifier pattern-interpretation/reproduce-the-recorded-run test-registry/spot-check-at-the-declared-rate translation/choose-the-equivalence-check]
@see-also hygiene/settle-with-meters
@how run the translation on fixtures and, where they exist, recorded runs of the source system; compare on the declared preserved properties; include falsifier inputs a wrong translation would fail; attribute each disagreement explicitly to the translation or the source-side implementation. In the identity case, reproduction shrinks to a spot-check that the record reproduces.
@violation-signature a fixture copied from the translation's own output; disagreement assumed to be the translator's fault; a spot-check presented as a correctness verdict.
! conclusion: A translation is tested by running it where the source's behaviour is known, with falsifier inputs, and each disagreement is attributed to one side.

  + context: Lean theorems and fixtures meet Clojure tests; interpreted cascades meet recorded runs.

  + IF:
    A translation must be shown to preserve behaviour.

  + HOWEVER:
    Easy fixtures confirm rather than test, and a disagreement does not say which side erred.

  + THEN:
    Test on known behaviour with falsifiers and attribute disagreements.

  + BECAUSE:
    A test the wrong translation would also pass shows nothing.

  + evidence: WM-11 +F sign bug caught by a behavioural falsifier (74a118c5); inbox-zero interpretation disagreeing with the implementation and naming three implementation defects; zai-8's countercheck that one spot-check moves P(clean) only from 0.5 to 0.526.
```


---
## math-strategy/proof-architecture
source: futon3/library/math-strategy/proof-architecture.flexiarg

```
@flexiarg math-strategy/proof-architecture
@title Decide the Proof's Architecture Before Proving Any of It
@keywords architecture, layers, reduction, decomposition, bridge, convention, problem-faithful, proof-shape, api-location
@audience lean-provers, formalizers, analysts
@tone strategic
@difficulty advanced
@cross-list [CA FA]
@why math-informal/separate-into-independent-pieces

! conclusion:
  Before proving anything, name the layers the proof will have and the reduction
  between each pair — including the layer that reconciles the problem's own
  statement with the library's conventions.

  + context: A theorem that mixes several kinds of work — a decomposition, an
    estimate, a transport between two notions, a library API whose definitions
    differ from the problem's — and where it is tempting to start at the first
    line and see where it goes.

  + IF:
      The proof decomposes into layers each of which can be stated and closed
      independently — regularity inside a disk and singular behaviour on its
      boundary; a strictly monotone subsequence extracted before an almost-
      everywhere argument; a contraction reduced to an inequality the library
      already has — then naming the layers first turns one intractable proof
      into several tractable ones, and the reductions between them become the
      things to look up rather than to invent.

  + HOWEVER:
      The layer that gets skipped is the one nobody counts as mathematics: the
      bridge between the problem's statement and the library's. A problem
      phrased with strict bad sets `{x | ε < |fₙ x - g x|}` meets an API phrased
      with closed ones `{x | ε ≤ dist (fₙ x) (g x)}`, and the cheap move is to
      silently replace `>` by `≤` and carry on. That is not a reduction, it is a
      change of theorem. The same applies to tail conventions (`N₀ < n` versus
      `N₀ + 1 ≤ n`) and to threshold choices that look like bookkeeping. Skipping
      the bridge does not remove the work; it relocates it to a place where no
      gate will look at it.

      The opposite failure is building machinery before checking whether the
      general case needs it. Verify the degenerate or equality case first: it is
      cheap, and it sometimes closes the problem or shows the heavy route is the
      wrong one.

  + THEN:
      Write the layers down as named statements before proving any. For each
      adjacent pair, say which reduction connects them and whether it exists in
      the library or must be proved. Keep a problem-faithful statement of the
      goal alongside the library-shaped one and make the translation an explicit
      lemma with its own proof — including the threshold arithmetic
      (`δ = min ε b.toReal / 2` and the `b = ⊤` case) rather than eliding it.
      Check the degenerate case before building for the general one.

  + BECAUSE:
      A proof's difficulty is rarely uniform across its layers, and the layer
      that fails is seldom the one that looked hard. Naming the layers first
      makes the hard one visible while it is still cheap to change route, and
      makes the translation layer — the one that silently changes what is being
      proved — an object with a name, a statement and a proof instead of a step
      nobody wrote down.

  + NEXT-STEPS:
    next[List the layers as named statements before proving any of them.]
    next[For each adjacent pair, name the reduction and say whether the library has it.]
    next[State the problem-faithful goal and the library-shaped goal separately; prove the bridge.]
    next[Do the threshold arithmetic explicitly, including the infinite/degenerate cases.]
    next[Check the degenerate or equality case before building general machinery.]
```


---
## test-registry/bind-warrant-to-the-diff
source: futon3/library/test-registry/bind-warrant-to-the-diff.flexiarg

```
@flexiarg test-registry/bind-warrant-to-the-diff
@title Check The Warrant Against The Diff Under Review
@keywords record-check, manifest, diff-scope, environment, refusal, mismatch-fields
@audience reviewers, registry check implementers
@tone technical
@style pattern
@status draft 2026-09-16 (claude-7; implementation futon3c test_registry.clj `check`; independent refusal and correction 2026-09-14 on LC_ALL); updated 2026-09-19 (claude-12: `check-record!` returns typed :stale-sha with a closure-diff naming exactly which pinned paths drifted; claude-4's r111 self-correction — the scope compared is the warrant's DECLARED scope, not the whole repo)
@references [test-registry/warrant-rides-the-handoff test-registry/rerun-when-the-warrant-fails hygiene/observe-the-authority translation/bind-to-the-source translation/choose-the-equivalence-check]
@see-also inbox-zero/measure-against-the-remote
@how the reviewer declares the diff scope (changed paths) and runs the record check: complete chain, intent-result binding, timestamps, exact current code and test manifests against the recorded ones, diff coverage (every changed path is inside the warrant's manifests), resolved environment against the fingerprint, log hash, parsed result envelope. A mismatch refuses with :warrant? false, the expected and observed differing fields, and a next action; sha drift refuses as typed :stale-sha with the closure-diff listing the drifted paths, so the rerun that follows is priced by what actually moved. The scope compared is the warrant's declared scope (code paths, test paths, load closure) — a dirty file elsewhere in a shared checkout is not this warrant's business. A stale verdict has three sources that :changed-files (plus each path's commit status) distinguishes: superseded (committed drift — re-mint), under construction (uncommitted drift — the owning lane is mid-edit; wait, don't re-dispatch), and environment (fingerprint, not files). Declared test-environment values (locale, timezone) are inputs, never waived comparisons.
@violation-signature a check that passes on HEAD when the working tree differs; a changed path outside the warrant accepted; an environment mismatch refused without saying which field; a locale difference waived to make a check pass; a store read's recorded :warrant? true treated as validity-now (the mint verdict is a lookup; only a fresh record check answers "does it still hold" — 2026-09-19, both real defects of the day were visible only as a check refusing while the stored payload said true).
! conclusion: The reviewer verifies the warrant covers exactly the reviewed diff and environment, and any mismatch refuses with the differing fields named.

  + context: A reviewer holds a warrant id and a diff.

  + IF:
    The reviewer wants to rely on the author's run instead of repeating it.

  + HOWEVER:
    Code may have moved since the run, the diff may touch paths the run did not cover, and environments differ in ways (locale, JVM, dependency jars) that change results.

  + THEN:
    Check chain, manifests, diff coverage and environment; accept only a complete match; refuse typedly otherwise.

  + BECAUSE:
    A record check costs a sha comparison; trusting a warrant for different code costs a wrong review.

  + evidence: TN-test-registry-2026-09-14 "Independent refusal and correction" (LC_ALL); record check measured at 795 ms against 1008 ms for the author's full namespace run.
```


---
## inbox-zero/escalate-by-who-can-act
source: futon3/library/inbox-zero/escalate-by-who-can-act.flexiarg

```
@flexiarg inbox-zero/escalate-by-who-can-act
@title Escalate To Whoever Can Act, And Never Build A Queue That Waits
@keywords escalation, tier-ladder, outlier, responsible-seat, operator, deferral, decay
@audience inbox-zero policy authors, followup-queue consumers
@tone technical
@style pattern
@status draft 2026-09-16 (claude-7; from futon0/README-inbox-zero.md "The anomaly is the thing worth a human", Joe's refinement 2026-08-24, and "The Street Sweeper already solved this"; implementation: futon3 inbox-zero-lib escalation.clj route)
@references [inbox-zero/push-the-declared inbox-zero/promote-at-turn-end buffer-cleaner/receipt-then-gate hygiene/route-to-who-can-act]
@see-also inbox-zero/gate-fails-loudly
@how route by who can act, trying each tier before the next: tier 0 nobody (ordinary accumulation, auto-push); tier 1 the responsible seats found by session–commit links and claims (outlier over the declared ahead threshold, held plan, stale base), delivered exact-seat through the followup queue; tier 2 the street-sweeper peripheral when the tier-1 seat cannot be found; tier 3 Joe, only as a hold with a question for judgement only a human can make (sensitive content, an agent declaring it cannot decide). Volume is never by itself a tier-3 signal. Every escalation has a deadline after which it acts or is re-routed, never waits indefinitely.
@violation-signature operator queues that grow; escalations to Joe by volume; an outlier that reaches no one because its seat rotated; a proposal that needs a yes and has no deadline.
! conclusion: Outliers go to the nearest party able to act, down a tier ladder ending at Joe only for human judgement; nothing is routed to a queue that waits.

  + context: Hygiene findings arrive continuously; the operator's attention is the scarcest resource in the stack.

  + IF:
    A finding is not ordinary (an outlier, a held plan, a stale base, a sensitive hit).

  + HOWEVER:
    Routing it to the operator builds the queue that silts up; routing it nowhere lets 110 commits sit on one disk; and the seat that made the commits may already be gone.

  + THEN:
    Resolve the responsible seat, deliver exact-seat with a deadline, fall back to the sweeper, and hold with a question for Joe only when judgement requires a human.

  + BECAUSE:
    Does its output act, or does it wait? If it waits, it silts up at exactly the rate the work arrives, and the silt is worthless by the time anyone looks.

  + evidence: Street Sweeper 2026-05-25 → 2026-08-19 (2,551 patches, 0/60 applicable); Zone 110 diverged commits 2026-08-19; Joe 2026-08-24 tier refinement; the 2026-09-16 21:01 gate run shows p4ng at 291 unpushed commits (oldest 151h), far above the default threshold of 10, with no tier-1 delivery observed.
```


---
## cascades/the-slice-is-the-unit-of-use
source: futon3/library/cascades/the-slice-is-the-unit-of-use.flexiarg

```
@flexiarg cascades/the-slice-is-the-unit-of-use
@title The Slice Is the Unit of Use
@sigils [🔪/片]
@keywords cascade, slice, view, skeleton, filigree, provenance, retrieval, measurement, union
@audience knowledge-base designers, agent-system builders
@tone design
@factor Concentration (samādhi)
@provenance M-apm-demonstration W.70; P16 eligibility-provenance idiom; Moran 1971 (AA queries the project model); no grounding source found for this pattern; deterministic search receipt: runs/L6-no-source-check.edn (L6-L10 sweep, 2026-09-05)

! conclusion:
  Seats never need "the cascade"; they need the slice for one problem at one moment: declared
  skeleton ∪ derived filigree, computed against the current state, delivered with provenance
  that labels which edges are load-bearing declarations and which are today's conjecture.

  + context: The declared/derived tension looks like an either/or about what the cascade IS;
    it dissolves once acting and measuring are recognized as different views of it.

  + IF:
      Both a measurable store structure (spectral series, review navigation) and dynamic
      contextual retrieval are required of the same pattern base.

  + HOWEVER:
      The union must never blur its sources: a seat treating conjectured edges as settled, or
      a measurement counting conjecture as structure, silently corrupts both halves. The
      provenance labels are load-bearing, not decoration.

  + THEN:
      Make the queryable object always a slice with labeled edge classes (the same idiom as
      eligible = snapshot ∪ cycle-promoted with sources labeled); measure the skeleton and the
      accumulated receipted edges; act on the whole slice.

  + BECAUSE:
      The slice-for-acting / skeleton-for-measuring split lets on-the-fly dynamism and witnessed
      structure coexist, connected by the promotion economy (cascades/edges-earn-permanence) —
      neither view has to lose for the other to work.

  + NEXT-STEPS:
    next[Define the slice's provenance schema before the derivation engine.]
    next[Point the spectral series at skeleton + receipted edges only.]
```


---
## coordination/session-durability-check
source: futon3/library/coordination/session-durability-check.flexiarg

```
@flexiarg coordination/session-durability-check
@title Session Durability Check (G0)
@keywords durability, persistence, proof-path, receipt
@audience coordination maintainers, futon3b builders
@tone formal-analytic
@references [futon-theory/durability-first futon-theory/proof-path futon-theory/error-hierarchy]
@theory-grounding [A1(Auditability) I1(Boundary)]

! conclusion: 
  If you want coordinated work to compound, gate successful responses on durable persistence of the full proof-path and its evidence records.

  + context: If evidence is not durable, coordination is theater.

  + IF:
    you want an audit trail that can be replayed after the session ends

  + HOWEVER:
    acknowledging success before persistence creates silent loss modes (accepted-but-not-stored)

  + THEN:
    verify durability at G0 and reject success when persistence fails

  + BECAUSE:
    a proof-path is only real if it can be reconstructed later
```


---
## test-registry/warrant-only-the-wires
source: futon3/library/test-registry/warrant-only-the-wires.flexiarg

```
@flexiarg test-registry/warrant-only-the-wires
@title Warrant Only The Wires The Machine Runs On
@keywords wires, needed-behaviour, subjects, wire-types, wire-instances, scope-discipline, excess-bookkeeping
@audience subject-taxonomy authors, warrant commissioners
@tone technical
@style pattern
@status draft 2026-09-19 (claude-12; Joe's ruling same day: "The warrants that the registry will produce will certify only needed behaviour, required for running the machine"; wire register futon2 15641d7d)
@references [test-registry/bind-the-subject test-registry/register-the-run apparatus/one-authority-per-question hygiene/observe-the-authority]
@see-also apparatus/repairs-name-defects-not-neighborhoods
@how subjects come from a reviewable wire register, not from whatever tests exist. A wire is either an instance (one producer→consumer seam, e.g. [:find :candidate-set], [:live-c :preference-spec]) or a type (a duty holding across every wire of a class: retention, recording, identity, independence). The register joins the theory-side edge classification (drawn+equation-justified, theory-demanded-but-unrealised, plumbing, unexplained) with the requirement list and the live subject bindings, so "what is tested and warranted" is one query, and so is its complement, the unwarranted gap list. A proposed subject that names no wire is challenged before it is bound. Field contracts between endpoints (the producer writes :g-terms where the consumer reads it) are wires too: one contract artifact, its sha pinned by both endpoint warrants.
@violation-signature a warrant for behaviour no wire carries; a subject whose name is a namespace rather than a seam; the gap list maintained by hand instead of queried; two endpoint warrants green while the field they disagree about drifts between them.
! conclusion: Every registry subject names a wire (instance or type) from the queriable register; behaviour on no wire gets no warrant.

  + context: The registry accepts subject bindings and the codebase holds far more tests than the machine has wires.

  + IF:
    Someone proposes a subject for warranting.

  + HOWEVER:
    Tests are easy to write and green tests feel like progress, so the subject list grows by accretion unless admission asks what wire the behaviour rides.

  + THEN:
    Admit subjects only from the wire register; keep the register a live join of theory edges, requirements and bindings, so both the warranted list and the gap list are queries.

  + BECAUSE:
    The registry's economy (no rerun while code is unchanged) only pays if the warranted set is the needed set; warranting the unneeded funds its persistence.

  + evidence: futon2 15641d7d (wire-requirements.edn: 27 ARGUE'd requirements classified as 8 wire-types + instances; wire_register.bb joining them with aif-conformance.edn: 42 edge wires {conformant 15, missing 13, plumbing 8, unexplained 6}, 2 of 27 requirement wires warranted on the day of writing); the 2026-09-19 field-drift specimen (producer wrote :g-term-decomposition at top level, consumer read :g-terms under :decision — both sides individually plausible, the seam untested).
```


---
## agent/hypothetical-proof-architecture
source: futon3/library/agent/hypothetical-proof-architecture.flexiarg

```
@flexiarg agent/hypothetical-proof-architecture
@title Hypothetical Proof Architecture
@sigils [🏗/乾]
@keywords hypothetical, architecture, wiring, diagram, triage, path, blocked, open, solid, proof, kill, sequential, parallel, obstacle
@audience proof engineers, mathematical agents, research coordinators
@tone strategic
@factor Keen investigation (dhammavicaya)
@references [math-informal/split-into-cases math-strategy/technique-landscape-map]
@provenance no grounding source found for this pattern; deterministic search receipt: runs/L10-no-source-check.edn (L6-L10 sweep, 2026-09-05)

! conclusion: 
  Before investing effort in a proof path, sketch multiple complete proof architectures as
  wiring diagrams with explicit node status (solid/open/blocked). Triage immediately to
  focus effort on the most promising path.

  + context: You have a theorem to prove with multiple plausible approaches. Each approach
    has several steps, some established and some open. The temptation is to start with the
    "obvious" or "natural" approach and push until stuck.

  + IF:
      There are 3+ plausible proof paths, each with multiple steps. Some steps are
      established results, some are open problems, and some may be blocked by known
      obstructions. The risk profile of each path (number of open steps, their independence,
      known difficulties) varies significantly.

  + HOWEVER:
      Sketching architectures is upfront cost. For problems with one clear approach, it's
      overkill. Only invest in this when the proof landscape is genuinely multi-path and the
      cost of pursuing a dead end is high (weeks of work, not hours).

    + FAILURE-MODES:
        - Architecture bias: sketching only 2-3 paths and missing the best one. Cast the
          net wide initially; narrow after triage.
        - False "blocked" assessment: marking a path as blocked based on a surface reading
          of an obstruction, when the obstruction has a workaround. Verify blocks carefully.
        - Triage paralysis: spending too long comparing paths instead of working on the
          best one. The triage should take hours, not days.

  + THEN:
      1. For each plausible proof path, draw a complete wiring diagram from hypotheses to
         conclusion.
      2. Mark each node as: solid (established), open (needs work), or blocked (known
         obstruction).
      3. Kill blocked paths immediately.
      4. Compare open paths on: (a) number of open nodes, (b) whether open nodes are
         sequential (each depends on the previous) or parallel (independent), (c) whether
         any open node is a known hard problem.
      5. Prioritize paths with fewer open nodes, parallel structure, and no known-hard
         sub-problems.

  + BECAUSE:
      Sequential open obstacles multiply risk (if any one fails, the entire path fails).
      Parallel open sub-goals reduce risk (any one success may suffice). A path with one
      open node is strictly better than a path with three sequential open nodes, even if
      the single node is harder, because success is binary: one hard question has the same
      risk profile as three easy questions in series but is easier to focus on.

    + EVIDENCE:
        P7 lattices with 2-torsion (futon6, Feb 2026): 5 hypothetical proof architectures
        (H1-H5) were sketched for the S obligation. Two (H3, H5) were immediately killed
        (codim-2 gap fails for reflections; Gauss-Bonnet kills Fowler in odd dim). One (H2)
        was deprioritized (3 sequential obstacles with structural headwinds). The winner (H1,
        rotation route) was not the "obvious" continuation but had the best risk profile:
        one open question (lattice existence) with no known blocker, plus two parallel
        S-branch options.

  + NEXT-STEPS:
    next[Sketch 3-5 complete proof architectures as wiring diagrams.]
    next[Mark node status: solid, open, blocked.]
    next[Kill blocked paths, deprioritize sequential-obstacle paths.]
    next[Focus on the path with fewest independent open nodes.]
```


---
## buffer-cleaner/observe-categories
source: futon3/library/buffer-cleaner/observe-categories.flexiarg

```
@flexiarg buffer-cleaner/observe-categories
@title Observe Categories Before Acting
@keywords observation, channel, category, decisiveness, read-only, structural-kind
@audience buffer-cleaner adapter, G-over-cascades consumers
@tone technical
@style pattern
@status staged (field source: futon-buffer-cleaner.el candidate-kind predicates; live evaluation: futon2/holes/labs/wm-contract/buffer-cleaner-adapter/adapter/dump-state.el, read-only; first real packet 2026-09-15, 138 buffers)
@references [buffer-cleaner/classify-staleness hygiene/observe-the-authority]
@see-also apparatus/done-is-observed-running
@how emit per buffer the structural kind plus raw safety signals — name, kind, file, modified (normalized: any Emacs-truthy value including :autosaved is true), has-process, visible, active-agent, server-clients, display-age-seconds (buffer-local read, -1 if never displayed) — and nothing else: no threshold logic, no kill decisions, read-only.
@violation-signature A packet whose file rows all share one age (the eval buffer's display-time leaked through); preserved buffers invisible because kind absorbed the threshold's verdict; a modified file whose modified flag reads false.
! conclusion: Observation tags each buffer with its structural category and raw signals; every decision is left to the wiring that consumes the packet.

  + context: A live Emacs holds 100+ buffers across hidden HTTP outputs, invoke lanes, temp modes, Dired sessions and file visits; the cleaner runs unattended on a timer.

  + IF:
    Any consumer needs to know what is in the session before deciding what to clean.

  + HOWEVER:
    Deciding staleness at collection time bakes one wiring's threshold into every downstream consumer; and Emacs truthiness is wider than booleans — buffer-modified-p can return symbols (:autosaved) that JSON-encode as strings and read false to naive checkers.

  + THEN:
    Collect structural kind only (no threshold), normalize truthiness at the source, read display age per-buffer via buffer-local-value, guard each row against buffers dying mid-iteration (live sessions mutate during capture), and write the packet to file from Emacs to avoid shell escaping.

  + BECAUSE:
    Per-channel honesty begins at observation: a decisive channel must be declared decisive where it is observed, and an uncertain one must reach the score with its uncertainty intact — the collector that pre-decides has already chosen a policy.

  + evidence: The relocation history: the first collector read display-time from the eval buffer (every age identical — a real measurement bug caught by consumer review); the second conflated preserved with unknown until the structural split; the third leaked :autosaved as "autosaved" and a modified file died in the kill set (codex-26 finding, fixed with a regression test at futon2 a87d27e6).

  + NEXT-STEPS:
    - Keep the collector dumber than every consumer: any new signal is emitted raw, never interpreted.
```
