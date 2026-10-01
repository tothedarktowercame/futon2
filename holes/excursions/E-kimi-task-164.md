# E-kimi-task-164 — pattern-stage reading kimi-4-1790889931 (10 patterns)

**Requisition:** completed — 2026-10-01T21:28:07Z, job invoke-1790889931891-29861-1b6f6c44, state done

Clocked in by pattern-stage-read-loop for kimi-4 on 2026-10-01 (one Kimi task, one excursion, so the seat's
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

Answer: write /tmp/claude17/pattern-stage-read/kimi-4-1790889931.answer.json as a JSON array, one object per pattern:
  {"id": ..., "kind": ..., "stage": ..., "stage-mode": ..., "confidence": ..., "node": optional, "quote-field": ..., "quote": ..., "rationale": ...}
Rewrite the file after each pattern and check it parses: python3 -c "import json;print(len(json.load(open('/tmp/claude17/pattern-stage-read/kimi-4-1790889931.answer.json'))))"
Do all 10. Do not write anywhere else, do not commit, do not publish. Nobody needs belling; end with one line: the answer path and the count.


---
## cycle-machine/disruption-soak
source: futon3/library/cycle-machine/disruption-soak.flexiarg

```
@flexiarg cycle-machine/disruption-soak
@title Disruption Soak: Inject Every Known Disruption, Assert Same Phase and Same Intent
@keywords soak, chaos, resume, reload, job-cap, restart, revision-moved, regression, fixture
@audience cycle-machine builders, APM maintainers, reviewers
@tone formal-analytic
@grade technique
@why cycle-machine/step-machine war-room/wr-27-a-loop-is-born-instrumented-for-its-gain
@see-also coordination/cross-validation-protocol
@theory-grounding [A4(Degradation-detectable) A1(Auditability)]

! conclusion:
  If you want the list of cycle disruptions to stop growing one incident at a time, then maintain one soak test that starts a step machine on a fixture, injects each known disruption mid-step, and asserts the machine resumes to the same phase with the same pending-intent digest — and require that every new incident be added to the soak as a disruption before its guard is merged.

  + context: The step machine claims durability. A claim of durability is tested by disrupting, not by running. The known disruptions are finite and enumerable; the unknown ones become known exactly once each, at which point they belong in the list.

  + IF:
    a step machine is relied on to complete multi-phase runs unattended

  + HOWEVER:
    a guard added after an incident proves only that the specific failing site now handles the specific failing case; it does not prove the machine resumes, and it is silent about the other sites; the next incident is a different site with the same disruption

  + THEN:
    write test/futon3c/apm/disruption_soak_test.clj with a fixture coordinator (fake ports that record calls and can be told to fail) and one test per disruption, each of the form:
      start → tick to phase P with pending intent I → DISRUPT → restore → tick → assert phase = P, pending-intent digest = digest(I), no duplicate port call, no second coordinator registered.
    The initial disruption set:
      :namespace-reload      — re-require the coordinator ns between ticks
      :job-killed-at-cap     — job-port returns terminal {:state :killed} mid-await
      :process-restart       — drop all in-memory state; rebuild coordinator from registry + state file only
      :revision-moved        — corpus HEAD advances between decide and reconcile; expect :pre-state-digest mismatch, not a wrong-phase step
      :state-file-partial    — truncated state file (simulates crash mid-persist); expect refusal with a typed finding, not a silent restart from :preflight
      :duplicate-activation  — same intent reconciled twice; expect idempotence
      :memory-store-down     — student/share port unavailable; expect :awaiting, not :closed
    Each case is a fixture pair (state.edn + intent.edn) under test/resources/disruptions/<name>/ so an incident can be reproduced by dropping its captured state there.

    + VALIDATION:
      - [ ] disruption_soak_test.clj exists and runs in the fast test split.
      - [ ] Every disruption in the initial set above has a test; none are skipped.
      - [ ] Each test asserts phase, pending-intent digest, port-call count, and registry entry count — all four.
      - [ ] A captured-state fixture directory exists and the test loads from it.
      - [ ] Any commit whose message names an incident adds a disruption or a fixture, or says why not.

  + BECAUSE:
    "resumes to the same phase with the same intent" is the entire durability claim stated as one assertion; a test that makes it for every disruption is the only artefact that converts the week's incident log into a finite list with a known remaining size

    + evidence: the week's ~150 APM repairs arrived one incident at a time; each disruption source (JVM reload, job killed at Agency cap, futon1b OOM, corpus revision moved under a launch, process restart) was discovered live, once, and guarded at the site that failed. No test injects any of them.

    + RESULT:
      a reviewer can answer "is this converging?" by diffing the disruption list against the incident ledger: a new incident that is not a new disruption means a guard is masking a cause
```


---
## eight-gates/lie-split
source: futon3/library/eight-gates/lie-split.flexiarg

```
@flexiarg eight-gates/lie-split
@title Liè: Split
@sigils [✂️/分]
@audience pattern navigators, agent architects
@tone foundational
@style energy-pattern
@references ["agent/coordination-has-cost (recognizing coupling)" "agent/scope-before-action (identifying natural boundaries)"]
@why [problems/tensions-are-navigated-not-eliminated]

! conclusion: Apply opposing forces to separate; splitting reveals the joints in apparently unified structures.

  + context: An agent faces a situation that presents as monolithic but actually has internal tensions that can be separated.

  + IF:
    The situation contains coupled concerns, tangled dependencies, or conflated concepts that would benefit from separation.

  + HOWEVER:
    Splitting without identifying the natural joint damages the structure. The opposing forces must find where the thing wants to come apart, not force an arbitrary break.

  + THEN:
    Apply two forces in opposite directions—not to destroy but to reveal structure. Liè can use hands and feet simultaneously (different tools, different leverage points). "Flying diagonally" demonstrates: the split follows the natural diagonal where the structure separates cleanly.

  + BECAUSE:
    Tensions in patterns (IF/HOWEVER) often represent splits—conditions that pull in different directions. Recognizing where a situation naturally divides enables clean separation rather than messy tearing. Coordination has cost precisely because it couples things that might split.

  + NEXT-STEPS:
    - Look for the natural joints in monolithic situations.
    - Apply opposing forces at those joints, not arbitrary points.
    - Use different tools/resources for each direction of the split.
```


---
## eight-gates/press-mechanism
source: futon3/library/eight-gates/press-mechanism.flexiarg

```
@flexiarg eight-gates/press-mechanism
@title Press: Demand Mechanism Test
@sigils [👓/力]
@audience security reviewers, pattern auditors
@tone scrutinizing
@style security-pattern
@energy ji
@references ["eight-gates/ji-press (underlying energy)" "eight-gates/push-warrant (if mechanism passes, warrant is next)" "eight-gates/split-extract (if mechanism fails)"]
@why [problems/tensions-are-navigated-not-eliminated]

! conclusion: Focus scrutiny on the BECAUSE clause — is it actually explaining what causes what, or is it decorative rationalization?

  + context: A pattern is under review (eight-gates/roll-back-hold). We need to determine whether its BECAUSE clause contains genuine causal explanation or post-hoc decoration.

  + IF:
    A pattern's BECAUSE clause needs verification. We suspect it may be decorative — explaining why the pattern is attractive without explaining why it works.

  + HOWEVER:
    Genuine BECAUSE clauses can be hard to distinguish from decorative ones by surface inspection alone. Over-aggressive testing destroys speculative-but-genuine patterns. Under-testing lets fabrication templates through.

  + THEN:
    Press (Jǐ). Concentrate force on a specific point: the BECAUSE clause. Apply the mechanism test: "Is this BECAUSE actually explaining what causes what?"

    + TEST-PROCEDURE:
      1. State the pattern's BECAUSE clause explicitly.
      2. Ask: What outcome would disconfirm this BECAUSE?
      3. If no disconfirmation is possible → fail mechanism test.
      4. If disconfirmation is possible → ask: Has it been checked?
      5. If checked and pattern survived → pass mechanism test.
      6. If not checked → hold for evidence collection.

  + BECAUSE:
    Jǐ focuses energy on a single point. In security terms: we don't attack the whole pattern, we scrutinize the warrant. A decorative BECAUSE can justify any THEN equally well — it's post-hoc rationalization that sounds explanatory but carries no discriminative power. A genuine BECAUSE constrains which THEN clauses follow — it has causal teeth.

    + MECHANISM-TEST:
      The mechanism test asks: "Would this BECAUSE clause distinguish between success and failure?"
      - If BECAUSE could explain why the pattern succeeds AND why it fails with equal plausibility → decorative
      - If BECAUSE predicts specific failure modes that would disconfirm it → genuine mechanism
      - If BECAUSE changes based on outcome (heads I win, tails you lose) → self-sealing

  + NEXT-STEPS:
    - If pattern fails mechanism test: escalate to eight-gates/split-extract (remove from active consideration).
    - If pattern passes mechanism test: proceed to eight-gates/push-warrant.
    - If test is indeterminate: hold in eight-gates/roll-back-hold, collect more evidence.
```


---
## enrichment/indentation-as-complexity
source: futon3/library/enrichment/indentation-as-complexity.flexiarg

```
@flexiarg enrichment/indentation-as-complexity
@title Indentation as Universal Complexity Proxy
@sigils [引/节]
@keywords indentation, complexity, nesting, depth, universal, proxy, bootstrap
@audience futon developers, enrichment operators
@tone pragmatic
@factor Keen investigation (dhammavicaya)
@references [iching/hexagram-53-jian p4ng/reflect-in-layers enrichment/ARGUMENT]

! conclusion: 
  Use mean/max indentation depth as a cheap, language-agnostic complexity
  bootstrap; refine with language-specific analysis in later layers.

  + context: The reflection API provides rich structural metadata for Clojure
    (arglists, protocols, dispatch). But .el, .py, .edn, and .js files have
    no reflection endpoint. A universal complexity signal is needed for the
    cross-repo hotspot analysis in Layer 0.

  + IF:
    We need a complexity metric that works for .clj, .el, .py, .js, and .edn
    files — all languages in the futon stack.

  + HOWEVER:
    Indentation depth correlates with nesting but misses semantic complexity
    (a deeply-nested but simple cond vs. a flat but algorithmically dense
    function). It's crude.

  + THEN:
    Ingest mean-depth, max-depth, and line-count per file as
    code/indentation-complexity hyperedges. Treat this as a bootstrap signal
    — good enough to identify outliers ("why is this file nested 12 levels
    deep?"), refined in later layers with AST analysis or treesit.

  + BECAUSE:
    A crude universal metric that exists NOW is more useful than a precise
    language-specific metric that doesn't exist yet. Giugliano validated
    this at multi-million-line scale: "Indentation is good enough to apply
    in general. Even Lisp gets formatted in a standard way." The key is
    using it to FIND weird things, not to MEASURE exact complexity.

  + NEXT-STEPS:
    - evidence: outlier report — files with max-depth > 10 or mean-depth > 5
    - evidence: comparison with reflection-based complexity for Clojure files
      (do indentation outliers match protocol/multimethod complexity?)
    - falsifying-observation: if top-indentation files are all auto-generated
      or data files, the metric is noise — exclude those file patterns.
```


---
## eoi-engine/tension-foreclosure
source: futon3/library/eoi-engine/tension-foreclosure.flexiarg

```
@flexiarg eoi-engine/tension-foreclosure
@title EoI Tension Prompt — Foreclosure Honesty
@keywords eoi, foreclosure, honesty, address-now, no-foreclosure-fallacy, strawman-diagnostic
@audience operators authoring outward EoIs whose acceptance forecloses other active basins
@tone diagnostic-prompt
@references [eoi-engine/eoi-outward-one-shot, structure/unresolved-tensions-at-closure, writing-coherence/name-what-you-drop]


! conclusion:
  When an outward EoI's acceptance would foreclose other active basins
  (other employment, freelance work, ongoing relationships, geographic
  continuity, IP-scoped self-directed work), the EoI must name what is
  being let go rather than pretending the decision is foreclosure-free.
  Pretending "no foreclosure" is the failure mode the strawman
  discipline names; the EoI's Right Speech requires the drop list to
  be co-located with the inheritance list.

  + context:
    Applies during `address-now` tagging of an EoI Tension. The prompt
    asks the author: which of your other active basins would this
    engagement foreclose? Name the foreclosures explicitly in the EoI;
    do not let them remain implicit.

  + IF:
    The role's terms (salary, geography, time commitment, IP) would
    materially alter or end other active relationships, projects, or
    obligations the author is carrying.

  + HOWEVER:
    Naming foreclosures inside an outward EoI must be calibrated.
    Excessive foreclosure-listing reads as ambivalence rather than
    honesty. The pattern is: name the substantive foreclosures
    succinctly; do not pretend they do not exist; do not catalogue
    every minor adjustment.

  + THEN:
    Author writes a short foreclosure-honesty paragraph naming the
    substantive losses an acceptance would entail. Length: one
    paragraph or less.

  + BECAUSE:
    Per writing-coherence/name-what-you-drop: silent inheritance of
    scope is the failure mode. Per
    structure/unresolved-tensions-at-closure: tensions that should
    be preserved-and-named must not be forced into resolution by
    omission. The foreclosure-honesty paragraph is the EoI's
    sorry-preservation move at the institutional-scope level.

  + NEXT-STEPS:
    Author drafts the foreclosure paragraph in their own register;
    eoi-engine assembly inserts it; reviewer checks that the
    paragraph names the substantive foreclosures rather than
    catalogue-listing.

! source-note:
  Minted 2026-05-11 as part of the Arxana Essays annotation flow for
  the Anthropic Institute first-draft. Three tension-prompt flexiargs
  were created (foreclosure / free-solo-rule / cv-shape) as annotation
  sources because the existing eoi-engine VERIFY pass surfaced three
  `address-now`-tagged tensions whose content Joe had not yet supplied.
```


---
## exotic/graded-naturality
source: futon3/library/exotic/graded-naturality.flexiarg

```
@flexiarg exotic/graded-naturality
@title Graded Naturality with Curriculum Tightening
@sigils [🎴/乡]
@audience ADAPT implementers, naturality checker designers
@tone foundational
@style pattern

! conclusion: Naturality checking is graded (not boolean) and curriculum-tightened; commutativity residuals are logged for analysis, not hard-failed before M14.

  + context: Defining how strictly to enforce the naturality condition on ADAPT transformations.

  + IF:
    The naturality square (PLAN changes must commute with mission execution) is the core coherence check for adaptations.

  + HOWEVER:
    Boolean pass/fail is too strict early on (everything fails) and hides information; pure permissiveness loses the signal entirely.

  + THEN:
    Use graded metrics: mission-path edit distance as primary, evidence-vector distance as soft penalty/bonus. Log commutativity residuals (difference between PLAN(f;g) and PLAN(f);PLAN(g)) without hard-failing. Tighten tolerance as the curriculum advances; post-M14, naturality violations may become blocking.

  + BECAUSE:
    Graded checking provides signal during development (how close to commutative?) while logging builds the evidence base for later decisions. Curriculum tightening means early exploration isn't penalized but mature systems are held to standards.

    + evidence: exotic-programming-curriculum.md Basecamp decision log, Naturality strictness entry; Basecamp invariants section.

  + NEXT-STEPS:
    - Implement mission-path edit distance metric.
    - Add residual logging to adapt.clj.
    - Define curriculum schedule for tolerance tightening.
```


---
## futon-theory/baldwin-cycle
source: futon3/library/futon-theory/baldwin-cycle.flexiarg

```
@flexiarg futon-theory/baldwin-cycle
@title Baldwin Cycle
@sigils [🔃/三]
@keywords learn, assimilate, canalize, explore, compress, evolve, adapt, fix
@audience futon developers, pattern theorists, stack architects
@tone analytic-foundational
@factor Keen investigation (dhammavicaya)
@references [futon-theory/four-types futon-theory/interface-loop futon-theory/proof-path futon-theory/retrospective-stability]
@references-extra ["Baldwin exploration maps to: OBSERVE → PROPOSE_CLAIM" "Baldwin assimilation maps to: APPLY_CHANGE → VERIFY → PROOF_COMMIT" "Baldwin canalization maps to: Strengthen invariants, tighten exotype"]
@provenance no grounding source found for this pattern; deterministic search receipt: runs/L6-no-source-check.edn (L6-L10 sweep, 2026-09-05)

! conclusion: 
  The Baldwin cycle is the engineered form of evolutionary learning:
  explore (learn within constraints), assimilate (fix successful adaptations
  into genotype), canalize (remove unnecessary freedom, make success cheap).

  + context: Named after James Mark Baldwin's insight that learned behaviors
    can become genetically fixed. In engineering: what starts as runtime
    adaptation becomes compile-time structure. This is how systems accumulate
    capability without losing auditability.

  + HOWEVER: The structural tension above manifests in the following concrete shapes (see substructure).
    + ANTI-PATTERNS:
      - Learning without assimilating (ghost capabilities, unreplayable)
      - Assimilating without canalizing (complexity creep, unstable)
      - Canalizing prematurely (lock-in before exploration complete)

  + THEN: Operate an explicit move on the named axis (see substructure below); steward the pattern against the declared discipline.
    + CYCLE-OUTCOME:
      Before: System reaches goal through search
      After: System reaches goal structurally (no search needed)
      Evidence: Captured in genotype + invariants + proof trail

    + FUNCTIONAL-GAIN:
      1. Local learning reduces global search cost (divide and conquer)
      2. Assimilation converts exploration into structural capability (cumulative)
      3. Canalization makes repeated success automatic (efficiency)

    + PHASE 1: EXPLORATION (Learning)
      Within fixed exotype constraints, allow phenotype plasticity.
      - Search, adapt, try variants
      - Measure outcomes against success criteria
      - Preserve evidence of what was tried
      Operations: OBSERVE, PROPOSE_CLAIM, experimental APPLY_CHANGE

    + PHASE 2: ASSIMILATION (固化 / Fixation)
      Compress successful learned behavior back into genotype.
      - Make replayable, auditable internal representation
      - Generate pattern, update rule table, extract replayable patch
      - Attach evidence chain (what worked, why, proof)
      Operations: VERIFY, PROOF_COMMIT (packaging success as artifact)

    + PHASE 3: CANALIZATION (壓縮 / Compression)
      Remove unnecessary degrees of freedom.
      - Tighten invariants around success region
      - Make success cheaper and more reliable
      - What once required learning now works by default
      Operations: Strengthen INVARIANT_CHECK, narrow exotype

  + BECAUSE:
    Systems that only execute (no learning) cannot adapt. Systems that only
    learn (no fixation) cannot accumulate. Systems that only fix (no compression)
    become brittle. The Baldwin cycle integrates all three.

  + NEXT-STEPS:
    next[Implement Baldwin engine as reusable component for layer interfaces]
    next[Define metrics for exploration cost vs. canalization benefit]
```


---
## hdm/human-machine-dialogue
source: futon3/library/hdm/human-machine-dialogue.flexiarg

```
@flexiarg hdm/human-machine-dialogue
@title HDM - Human<->Machine Dialogue
@sigils [💬/口]

! conclusion: 
  make human-machine dialogue a core HDM workflow so ambiguous material can be clarified and formalised reliably.

  + context: You are embedding clarification dialogues as part of HDM ingestion and retrieval.

  + IF:
    Automated parsing/formalisation of mathematical text is brittle: ambiguous notation, implicit steps,
    and missing context routinely break fully automatic tooling.

  + HOWEVER:
    Purely manual curation does not scale, and purely automated parsing loses critical nuance.

  + THEN:
    Make interactive dialogue part of the HDM kernel: let the system ask humans for help, record exchanges as scholia,
    and treat resolved clarifications as reusable knowledge. Likewise, let humans query HDM entries and have agents respond
    via the same pattern base.
    Use human/LLM conversation to disambiguate notation and refine structure until it can be formalised.

  + BECAUSE:
      Dialogue is already how humans learn/teach mathematics; embedding it in HDM aligns with practice and leverages LLM
      strengths instead of fighting them.

  + NEXT-STEPS:
    next[Store clarification dialogues alongside affected entries.]
    next[Build dialogue agents that turn conversations into updated flexiarg or metadata.]
    next[Assume some imports require dialogue and design ingestion workflows accordingly.]
```


---
## hygiene/receipt-then-gate
source: futon3/library/hygiene/receipt-then-gate.flexiarg

```
@flexiarg hygiene/receipt-then-gate
@title Proposals Have Receipts; Acts Have Gates
@keywords proposal, act, receipt, warrant, gate, re-verify, refusal, hygiene
@audience hygiene-policy authors, adapter builders, consumer designers, readers learning the library's layers
@tone technical
@style pattern
@status draft 2026-09-16 (claude-7; promoted from buffer-cleaner/receipt-then-gate, which named itself shared across the three generations; that file now carries the buffer-cleaner instance)
@references [buffer-cleaner/receipt-then-gate inbox-zero/push-the-declared test-registry/register-the-run test-registry/judge-adequacy hygiene/exempt-the-in-use hygiene/route-to-who-can-act]
@see-also apparatus/every-wait-has-a-deadline
@how every generation must: (1) emit each proposal as a receipt carrying its full basis (classification, evidence, cost); (2) route execution through a gate that re-verifies eligibility, exemptions and liveness at act time rather than trusting the receipt; (3) refuse typedly when no gate exists — the refusal is the honest state; (4) keep the gate's actor distinct from the proposal's author where judgement is involved; (5) price actions in the generation's own units.
@violation-signature buffer cleaner: the :autosaved leak put a modified buffer in the kill set and was caught at the receipt, costing nothing, because execution was refused (`:execute-not-gated`). Inbox zero: futon-sync push called read-line, so the act could only happen with a person present, and nothing between proposal and act could refuse on the machine's own evidence. Test registry: a reviewer rerunning the author's tests is a gate that re-executes the proposal instead of verifying its receipt; the registered run is the receipt and record check plus adequacy is the gate.
! conclusion: A proposal is recorded with its full basis; an act passes a gate that re-verifies at execution time, and refuses typedly when no gate exists.

  + context: Each generation proposes dispositions (kills, pushes, test verdicts) that have been wrong at least once.

  + IF:
    A policy has classified items and is ready to act.

  + HOWEVER:
    Classification may be stale by execution time, wrong in a way not yet caught, or built on wider signals than expected.

  + THEN:
    Receipt the proposal, gate the act with re-verification, refuse when the gate is missing.

  + BECAUSE:
    A wrong receipt is corrected by a better record before anything happens; a wrong act needs compensation after damage.

  + evidence: As in the violation signatures. Gap: the buffer cleaner has receipts but no gate yet; inbox zero has a gate (compile check, sensitivity screen) but its escalation path is wired to the wrong trigger (futon0 README checkpoint 2026-09-16).
```


---
## iching/hexagram-03-zhun
source: futon3/library/iching/hexagram-03-zhun.flexiarg

```
@flexiarg iching/hexagram-03-zhun
@title ䷂ 屯 (Zhūn) - Difficulty at the Beginning
@sigils [🏃/功]
@binary 100010
@trigrams [water/thunder]
@number 3
@audience improvisers, system designers, CT practitioners
@tone foundational
@style pattern

! conclusion: Beginnings are chaotic; persistence and small structure enable growth.

  + context: A system has just initiated and lacks stable form.

  + IF:
    Movement is present but coordination is poor and obstacles are frequent.

  + HOWEVER:
    Forcing order too early collapses vitality.

  + THEN:
    Establish minimal scaffolds and advance in small steps.

  + BECAUSE:
    Early turbulence is normal; structure must emerge gradually.

    + evidence: Traditional: "Difficulty at the beginning brings success through perseverance."

  + NEXT-STEPS:
    - Identify the smallest stabilizing loop.
    - Protect the nascent structure from collapse.
    - Pair with ䷃ (蒙) to develop learning.

@ct-interpretation
  :vision
  {:objects #{:incipient :forming}
   :morphism {:id :scaffold
              :source :incipient
              :target :forming
              :type :bootstrapping}}

  :as-morphism
  {:domain :chaotic-start
   :codomain :proto-order
   :preservation :low
   :composition :initiates-stabilization}

  :adapt
  {:trigger :overconstraint
   :evidence [:stall :collapse]
   :target :hexagram-04-meng}

@ant-interpretation
  :policy
  {:mode-bias :adaptive
   :policy-priors {:forage 0.4 :return 0.2 :hold 0.2 :pheromone 0.2}
   :description "Exploration with light stabilization"}

  :precision
  {:Pi-o 0.4
   :tau 0.7
   :description "Exploratory, lightly guided"}

  :pattern-sense
  {:trail-follow 0.3
   :gradient-use 0.4
   :novelty-seek 0.7
   :description "Search for viable paths"}

  :adapt-trigger
  {:condition :stall
   :interpretation "Need learning/discipline"
   :switch-to :hexagram-04-meng}

  :failure-mode
  "Early chaos never stabilizes"

@mmca-interpretation
  :sigil-encoding
  {:sigil "屯"
   :bits "10001000"
   :interpretation "Turbulent start with weak structure"}

  :exotype-params
  {:rotation 2
   :match-threshold 0.4
   :invert-on-phenotype? true
   :update-prob 0.6
   :mix-mode :scramble
   :description "Active intervention to seed structure"}

  :tensor-interpretation
  {:operation :bootstrapping-mix
   :description "Mixing to seed emergent order"}

  :regime-prediction
  {:expected :turbulent
   :dynamics "Early churn with emerging motifs"
   :eoc-relevance "Pre-EoC; needs stabilization"}

@domain-transfer-notes
  "屯 is the pattern of BOOTSTRAPPED BEGINNINGS across domains:
   - In ants: early exploration with fragile loops
   - In CA: turbulent starts before structure"
```
