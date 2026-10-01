# E-kimi-task-162 — pattern-stage reading kimi-2-1790889921 (10 patterns)

**Requisition:** completed — 2026-10-01T21:26:04Z, job invoke-1790889921896-29859-9b50b2cf, state done

Clocked in by pattern-stage-read-loop for kimi-2 on 2026-10-01 (one Kimi task, one excursion, so the seat's
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

Answer: write /tmp/claude17/pattern-stage-read/kimi-2-1790889921.answer.json as a JSON array, one object per pattern:
  {"id": ..., "kind": ..., "stage": ..., "stage-mode": ..., "confidence": ..., "node": optional, "quote-field": ..., "quote": ..., "rationale": ...}
Rewrite the file after each pattern and check it parses: python3 -c "import json;print(len(json.load(open('/tmp/claude17/pattern-stage-read/kimi-2-1790889921.answer.json'))))"
Do all 10. Do not write anywhere else, do not commit, do not publish. Nobody needs belling; end with one line: the answer path and the count.


---
## capability/capability-signature-vector
source: futon3/library/capability/capability-signature-vector.flexiarg

```
@flexiarg capability/capability-signature-vector
@title Capability signature as typed observation vector
@sigils [📐/相]
@keywords capability, signature, typed-observation, feature-map, cross-mission, mining, normalisation, articulation-floor, confidence
@audience the capability-mining pipeline (capability_mining.clj); downstream consumers scoring or comparing missions by capability; operators reviewing the capability matrix
@tone declarative
@factor Investigation (dhammavicaya)
@references [capability/capability-vocabulary-v0 capability/hinge-capability-extraction aif/structured-observation-vector]
@references-extra ["Mission of origin: ~/code/futon5a/holes/missions/M-learning-loop.md (§1 completion criterion 3, §5 INSTANTIATE capability-matrix)" "Producer: ~/code/futon5a/scripts/capability_matrix.clj" "Mining: ~/code/futon5a/scripts/capability_mining.clj (mission-signature function)"]

! conclusion:
  A mission's capability signature is a typed feature map —
  {capability-dimension × {channel × confidence}} — not a narrative
  description. Cross-mission scoring is comparable because the
  dimensions and confidence levels are shared across missions, while
  the specific combination is unique to each mission's hinge evidence.

  + context: Raw tag-activation counts measure recording cadence, not
    capability development. Normalise by baseline frequency: the no-shift
    dimension sets the denominator. Strong and weak tags are
    distinguishable; a mission with 3 strong pattern-articulation tags
    and 20 no-shift tags has a different profile from one with 3 strong
    and 5 no-shift, even though both have "3 pattern-articulation."

  + contrast: The meme miner assigned tags without confidence levels
    and without a baseline. Every input produced a tag. The typed-
    observation discipline says: if you cannot distinguish signal from
    baseline, your observation is noise.
```


---
## capability/hinge-capability-extraction
source: futon3/library/capability/hinge-capability-extraction.flexiarg

```
@flexiarg capability/hinge-capability-extraction
@title Hinge-level capability extraction with voice attribution
@sigils [🔬/抽]
@keywords capability, hinge, extraction, voice-attribution, agent-view, operator-turn, b/a-pair, mining, tagger, no-shift
@audience the capability-mining pipeline tagger; agents proposing capability tags at hinges; reviewers checking tag evidence
@tone declarative
@factor Investigation (dhammavicaya)
@references [capability/capability-vocabulary-v0 capability/capability-signature-vector structure/hinge-point]
@references-extra ["Mission of origin: ~/code/futon5a/holes/missions/M-learning-loop.md (§1 completion criterion 2, §5 INSTANTIATE voice-boundary result)" "Producer: ~/code/futon5a/scripts/capability_mining.clj (tag-hinge, tag-text, parse-hinge-log)" "Voice-boundary result: 14 entities from real corpus; old whole-block miner inflated cross-mission intersection from 1 to 3 dimensions"]

! conclusion:
  At a hinge, extract capability tags per attributed voice (agent-view
  vs operator-turn), not per block. Agent commentary and operator
  turns are different evidence types: an agent noting "we designed a
  pipeline" is not the same as an operator articulating "I learned to
  cut scope." Without voice separation, agent prose contaminates every
  operator reading.

  + context: The mining pipeline splits each hinge into agent-view and
    operator-turn entities (parse-hinge-log). Tags carry :voice so
    procedural evidence from an agent-only hinge remains distinguishable
    from an operator capability shift. A block with no attributed voices
    is a surfaced parse error, not a silent no-shift.

  + contrast: The pre-attribution miner tagged whole blocks. It
    assigned four capabilities to all 5 M-daily-scan hinges and six to
    all 9 M-trip-journal hinges because shared hinge prose and agent
    commentary triggered every keyword. The attributed pass found only
    two active dimensions in M-daily-scan and six variably-occurring
    dimensions in M-trip-journal. This is the articulation-floor filter
    working, not a tuned result.

  + discipline: The tagger is deliberately narrow. Only match when the
    keyword appears in a capability-claim context (near a capability
    verb: develop, learn, recognise, surface, cut, name), not just
    anywhere in the hinge text. Weak tags require the keyword near a
    capability verb; strong tags require the compound phrase. Default
    to no-shift when no contextual match fires.
```


---
## cascade-construction/order-by-what-each-step-needs
source: futon3/library/cascade-construction/order-by-what-each-step-needs.flexiarg

```
@flexiarg cascade-construction/order-by-what-each-step-needs
@title Order Patterns By What Each Step Needs, And Let The Guards Carry The Order
@keywords precedence, ordering, dependency, guard, observe-classify-act
@audience agents constructing cascades
@tone technical
@style pattern
@status draft 2026-09-17 (claude-7; the order used in all three 2026-09-16 cascades)
@execution deterministic once guards and effects are fixed (precedence follows from dependencies); the guards themselves come from interpretation.
@epistemic-value mostly pragmatic and deterministic. It exposes guards that cannot be checked (a need with no producing effect or observation), which turns a hidden unknown into a check candidate.
@done done when every guard's needs are produced by an earlier effect, observed at q0, or listed as a check candidate. Precedence then follows from those dependencies.
@references [cascade-construction/choose-the-grain-where-state-lives pattern-interpretation/compile-guards-exactly hygiene/receipt-then-gate]
@see-also cascades/declared-skeleton
@how for each pattern, write what it needs as its guard (classify needs the observation; act needs the receipt and the gate; route needs the classification) and what it establishes as its effect; take precedence from those dependencies (for hygiene cascades this gives observe → classify → exempt → propose → gate/act → route → settle); where two patterns need nothing from each other, say so and do not invent an order.
@violation-signature an act whose guard does not mention the receipt it depends on, so it fires out of order when the list changes; precedence copied from a document's section order; a blocked step hidden by ordering it last.
! conclusion: Put each step's needs in its guard and its contributions in its effect, and let precedence follow from those dependencies.

  + context: The patterns of a cascade have been mined and interpreted.

  + IF:
    Their order must be decided.

  + HOWEVER:
    Writing order and authored stands-on edges look like a ready-made order.

  + THEN:
    Derive order from guard and effect dependencies and record independence.

  + BECAUSE:
    With P10's continuing guards, a step that is ordered right but whose guard is wrong will still fire at the wrong time.

  + evidence: in the buffer-cleaner cascade the act half of receipt-then-gate is blocked by gate-exists⁻ while yield-settled still fires, which reproduces the recorded run; in the inbox-zero cascade push is blocked (outlier; sensitivity-screened unknown) while escalate and route fire.
```


---
## contracts/every-entry-has-a-falsifier
source: futon3/library/contracts/every-entry-has-a-falsifier.flexiarg

```
@flexiarg contracts/every-entry-has-a-falsifier
@title Every Correspondence Entry Names Its Falsifier
@keywords falsifier, fixture, behaviour, sign-bug, refutation
@audience contract authors, reviewers
@tone technical
@style pattern
@status draft 2026-09-16 (claude-7; Emit.Declaration falsifier field; ALIGNMENT.md)
@references [contracts/declare-the-reductions translation/test-by-reproducing-behaviour pattern-interpretation/reproduce-the-recorded-run]
@see-also hygiene/receipt-then-gate
@how each entry's falsifier names input classes and outcomes that would refute it, citing the Lean theorem and the deftest that exercise them (e.g. "p2/p1 ≠ exp(F1−F2) at equal G and habit"). At least one fixture must be able to fail for a plausible wrong implementation, not only for a crash.
@violation-signature a falsifier that restates the claim; a fixture table copied from the implementation's own output; a sign error that every existing test passes.
! conclusion: Each correspondence names concrete refuting behaviours, and a test exercises at least one that a plausible wrong implementation would fail.

  + context: Reviewers read many entries quickly.

  + IF:
    An entry claims a runtime function computes a Lean declaration.

  + HOWEVER:
    Value-replay fixtures are easy to write and weak.

  + THEN:
    Name the refuting behaviour and test it.

  + BECAUSE:
    The certificate's worth is the set of wrong implementations it rules out.

  + evidence: WM-11 selection posterior added F where the Lean subtracts it; the review added selection-posterior-f-and-habit-enter-with-lean-signs (futon2 74a118c5); exact belief falsifier on the input mean-field refuses (fixture_exact_accepts, 9/10).
```


---
## futon-theory/crime-relocates-to-a-scarcer-witness
source: futon3/library/futon-theory/crime-relocates-to-a-scarcer-witness.flexiarg

```
@flexiarg futon-theory/crime-relocates-to-a-scarcer-witness
@title The Crime Relocates to a Scarcer Witness (The Witness Ladder)
@sigils [↥/⌖]
@keywords laundering, self-certification, witness, ladder, cost-to-fake, pudding prover, three-witness, relocation, scarcity
@audience anyone hardening a measurement, reward, or certification against gaming
@tone foundational
@style pattern
@provenance no grounding source found for this pattern; deterministic search receipt: runs/L6-no-source-check.edn (L6-L10 sweep, 2026-09-05)

! conclusion: A laundering / self-certification failure is never killed by a single fix — it RELOCATES to a scarcer witness. Each fix that closes one shape exposes the next, harder-to-fake shape. So the defense is not one detector but a LADDER of progressively scarcer witnesses, terminating where the witness is too costly to forge (a witnessed real outcome / multi-witness certificate). Read each fix as "the crime moved up a rung," not "the crime is dead."

  + context: You have just closed a way the system could certify itself or launder a fake win, and
    it now looks honest.

  + IF:
      You fix a laundering / circularity / self-certification shape in a measurement or reward channel.

  + HOWEVER:
      The fix does not abolish the failure — it pushes it into a scarcer witness. Treating any single
      fix as "now it is honest" reopens the same crime one level up: the cheaper witness is gone, but
      a more expensive one is still forgeable, and the system will drift toward forging it.

  + THEN:
      Expect relocation. After each fix ask "where did the crime go? — what is the next-scarcest
      witness it can still hide behind?" Build the ladder explicitly: each rung a scarcer, harder-to-
      fake referent. Stop only where the witness is genuinely too expensive to fake — a witnessed real
      outcome, a three-witness certificate. Treat each relocation as progress: it costs the launderer
      more every rung.

  + BECAUSE:
      This is the Pudding-Prover rationale, derived from below: "the proof is in the pudding" means the
      only un-fakeable witness is the realized outcome; everything cheaper can be gamed. So the
      discipline is to keep raising the cost-to-fake until only the real thing passes — which is why a
      reward channel must end at a witnessed-outcome gate (`[[aif/two-layer-calibration]]` L2,
      pudding-G1), not at any cheaper internal check.

  + canonical-instance:
      The FutonZero calibration arc, three rungs each forced by catching the one below: the constant
      model's *state-blindness* exposed → made state-sensitive → the scaled model's
      *increment-circularity* exposed → the realised's *lack of external grounding* exposed → next
      witness is the outcome itself (pudding-G1). The forensic framing is `[[M-the-perfect-crime]]`;
      the structural half is `[[aif/no-self-certification]]`. Distilled WM-pilot 2026-06-11.
```


---
## hygiene/classify-by-remedy-judgement
source: futon3/library/hygiene/classify-by-remedy-judgement.flexiarg

```
@flexiarg hygiene/classify-by-remedy-judgement
@title Classify Items By How Much Judgement Their Remedy Needs
@keywords classification, judgement, remedy, prior, lane, role, hygiene
@audience hygiene-policy authors, readers learning how the library's layers relate
@tone technical
@style pattern
@status draft 2026-09-16 (claude-7; shared across three hygiene generations — see hygiene/README.md)
@references [buffer-cleaner/classify-staleness inbox-zero/classify-the-dirt inbox-zero/generated-by-role test-registry/rerun-when-the-warrant-fails hygiene/exempt-the-in-use hygiene/receipt-then-gate]
@see-also apparatus/evidence-to-disposition-once
@how every generation must: (1) assign each item exactly one class with recorded evidence for the assignment; (2) give each class its own remedy and the judgement it needs (none, once per kind, per item); (3) state uncertainty as a declared value with its source (a prior, a rate, a threshold) rather than hiding it in a class label; (4) keep an explicit unclassifiable class that is reported, never folded into another; (5) decide class by role or kind, not by an enumeration of instances or by file extension.
@violation-signature buffer cleaner: temp buffers labelled "decisive" while carrying a needed-later prior of 0.05, corrected at futon2 aa158c6. Inbox zero: fifty sweeps of one undifferentiated pile (2,610 files that were 35 unpushed commits, ~2,012 noise and ~68 real work, 2026-08-19); and a tracked-generated class the first three classes did not cover (p4ng 2026-09-16). Test registry: an extension rule would have ignored futon2's 268 tracked .log files, which are run cassettes (evidence), not build byproducts.
! conclusion: Split items into classes by the judgement their remedy needs, each with its own remedy, declared uncertainty and an explicit unclassifiable class.

  + context: Each generation faces a mixed population: buffers of seven kinds, dirt of several origins, test namespaces with and without changed tests.

  + IF:
    A policy must decide what to do with many items at once.

  + HOWEVER:
    One remedy for the whole population forces the slowest remedy on everything; and labels that look like facts ("decisive", "noise", ".log") can hide judgement or uncertainty.

  + THEN:
    Classify by remedy judgement with recorded evidence, declared uncertainty and an unclassifiable class.

  + BECAUSE:
    The size of the manual problem is the size of the per-item class, not of the pile.

  + evidence: As in the violation signatures. Gaps: in the test registry the classes are review lanes (full rerun or spot-check per namespace), so an item's class can change when its warrant fails; inbox zero needs two patterns (classify-the-dirt and generated-by-role) because tracked generated files split again by role.
```


---
## hygiene/settle-with-meters
source: futon3/library/hygiene/settle-with-meters.flexiarg

```
@flexiarg hygiene/settle-with-meters
@title Settle Each Cycle With Meters, Including What Is Not Known
@keywords meters, report, unknown, unavailable, change-not-state, saving, hygiene
@audience hygiene-policy authors, monitor designers, readers learning the library's layers
@tone technical
@style pattern
@status draft 2026-09-16 (claude-7; shared across three hygiene generations — see hygiene/README.md)
@references [buffer-cleaner/yield-settled inbox-zero/gate-fails-loudly test-registry/meter-the-saving hygiene/route-to-who-can-act]
@see-also apparatus/done-is-observed-running
@how every generation must: (1) end each cycle with typed meters of what was done; (2) state declared thresholds with their source labels; (3) report unavailable truth as its own value, never as false or zero; (4) report changes rather than repeating a steady state, so a change is visible; (5) measure claimed benefits against a recorded baseline, and say so when there is no benefit.
@violation-signature buffer cleaner: the real cleaner cannot know its false-kill rate, so every report carries :revisit-truth :unavailable rather than a success claim; both wirings missed the declared threshold (134 and 131 remaining against 16) and the report said so. Inbox zero: 248 consecutive hourly gate failures went to journald with no consumer (2026-09-12). Test registry: the only measured round trip saved no wall time (reviewer total 4,599 ms for one test; author full namespace 1,008 ms) and the technote says so.
! conclusion: Each cycle ends with typed meters, declared thresholds, unavailable truth as a legal value, changes rather than steady states, and measured rather than assumed benefit.

  + context: Each generation runs repeatedly and its reports feed tuning decisions.

  + IF:
    A hygiene cycle completes.

  + HOWEVER:
    Meters invite narrative, unknowns invite defaults, and constant alarms invite neglect.

  + THEN:
    Emit meters, thresholds with sources, explicit unavailable values, change-only alerts and measured benefit.

  + BECAUSE:
    Every later decision tuned on the report inherits whatever it overclaimed.

  + evidence: As in the violation signatures.
```


---
## iching/hexagram-15-qian
source: futon3/library/iching/hexagram-15-qian.flexiarg

```
@flexiarg iching/hexagram-15-qian
@title ䷎ 謙 (Qiān) - Modesty
@sigils [🌈/节]
@binary 000001
@trigrams [mountain/earth]
@number 15
@audience improvisers, system designers, CT practitioners
@tone foundational
@style pattern

! conclusion: Modesty levels extremes; it brings balance by lowering the high and raising the low.

  + context: A system needs balance and correction of excess.

  + IF:
    Extremes have formed and stability is threatened.

  + HOWEVER:
    Excess correction can suppress necessary drive.

  + THEN:
    Apply humility: reduce excess, support the weak.

  + BECAUSE:
    Balance is sustained when extremes are tempered.

    + evidence: Traditional: "Modesty creates success."

  + NEXT-STEPS:
    - Identify extremes.
    - Apply gentle equalization.
    - Pair with ䷐ (豫) for renewed energy.

@ct-interpretation
  :vision
  {:objects #{:extreme :balanced}
   :morphism {:id :level
              :source :extreme
              :target :balanced
              :type :equalizing}}

  :as-morphism
  {:domain :imbalanced
   :codomain :balanced
   :preservation :moderate
   :composition :equalizes}

  :adapt
  {:trigger :overcorrection
   :evidence [:suppression :stagnation]
   :target :hexagram-16-yu}

@ant-interpretation
  :policy
  {:mode-bias :maintain
   :policy-priors {:forage 0.25 :return 0.35 :hold 0.25 :pheromone 0.15}
   :description "Leveling cycles, avoid extremes"}

  :precision
  {:Pi-o 0.7
   :tau 0.4
   :description "Balanced response"}

  :pattern-sense
  {:trail-follow 0.6
   :gradient-use 0.5
   :novelty-seek 0.2
   :description "Steady equalization"}

  :adapt-trigger
  {:condition :stagnation
   :interpretation "Need renewed energy"
   :switch-to :hexagram-16-yu}

  :failure-mode
  "Over-leveling suppresses exploration"

@mmca-interpretation
  :sigil-encoding
  {:sigil "謙"
   :bits "00000100"
   :interpretation "Low intervention with balancing"}

  :exotype-params
  {:rotation 0
   :match-threshold 0.7
   :invert-on-phenotype? false
   :update-prob 0.3
   :mix-mode :majority
   :description "Gentle balancing"}

  :tensor-interpretation
  {:operation :equalizer
   :description "Reduce extremes, reinforce middle"}

  :regime-prediction
  {:expected :balanced
   :dynamics "Moderate stability"
   :eoc-relevance "Stabilizes EoC"}

@domain-transfer-notes
  "謙 is the pattern of MODEST BALANCE across domains:
   - In ants: level resource use
   - In CA: suppress extremes"
```


---
## iching/hexagram-20-guan
source: futon3/library/iching/hexagram-20-guan.flexiarg

```
@flexiarg iching/hexagram-20-guan
@title ䷓ 觀 (Guān) - Contemplation / Viewing
@sigils [📎/万]
@binary 000011
@trigrams [wind/earth]
@number 20
@audience improvisers, system designers, CT practitioners
@tone foundational
@style pattern

! conclusion: Observation clarifies; step back to see the whole before acting.

  + context: A system needs assessment and perspective.

  + IF:
    Action is uncertain and signals are ambiguous.

  + HOWEVER:
    Observation without action can lead to stagnation.

  + THEN:
    Pause to observe, then re-engage with clarity.

  + BECAUSE:
    Accurate perception guides effective action.

    + evidence: Traditional: "Contemplation. The ablution has been made, but not yet the offering."

  + NEXT-STEPS:
    - Gather a wide view.
    - Identify dominant patterns.
    - Pair with ䷒ (臨) to re-approach.

@ct-interpretation
  :vision
  {:objects #{:unclear :observed}
   :morphism {:id :observe
              :source :unclear
              :target :observed
              :type :perceptive}}

  :as-morphism
  {:domain :uncertain
   :codomain :clarified
   :preservation :high
   :composition :prepares-action}

  :adapt
  {:trigger :over-wait
   :evidence [:stall :missed-opportunity]
   :target :hexagram-19-lin}

@ant-interpretation
  :policy
  {:mode-bias :maintain
   :policy-priors {:forage 0.2 :return 0.3 :hold 0.35 :pheromone 0.15}
   :description "Pause and observe"}

  :precision
  {:Pi-o 0.85
   :tau 0.3
   :description "High attention, low exploration"}

  :pattern-sense
  {:trail-follow 0.6
   :gradient-use 0.5
   :novelty-seek 0.1
   :description "Observe existing structure"}

  :adapt-trigger
  {:condition :clarity
   :interpretation "Resume approach"
   :switch-to :hexagram-19-lin}

  :failure-mode
  "Observation without action leads to stagnation"

@mmca-interpretation
  :sigil-encoding
  {:sigil "觀"
   :bits "00001100"
   :interpretation "High observation, low intervention"}

  :exotype-params
  {:rotation 0
   :match-threshold 0.8
   :invert-on-phenotype? false
   :update-prob 0.2
   :mix-mode :none
   :description "Observe without rewriting"}

  :tensor-interpretation
  {:operation :identity
   :description "Preserve state while collecting signals"}

  :regime-prediction
  {:expected :stable
   :dynamics "Low-change observation"
   :eoc-relevance "Stabilizes but may reduce exploration"}

@domain-transfer-notes
  "觀 is the pattern of CONTEMPLATION across domains:
   - In ants: pausing to observe
   - In CA: low-change monitoring"
```


---
## iching/hexagram-57-xun
source: futon3/library/iching/hexagram-57-xun.flexiarg

```
@flexiarg iching/hexagram-57-xun
@title ䷸ 巽 (Xùn) - The Gentle (Wind)
@sigils [🌆/风]
@binary 011011
@trigrams [wind/wind]
@number 57
@audience improvisers, system designers, CT practitioners
@tone foundational
@style pattern

! conclusion: Gentle, repeated influence penetrates more deeply than force.

  + context: A system needs gradual alignment rather than abrupt change.

  + IF:
    Direct force fails or causes resistance.

  + HOWEVER:
    Excess gentleness can become indecision or drift.

  + THEN:
    Apply steady, low-amplitude influence until alignment appears.

  + BECAUSE:
    Wind shapes by persistence; gradual pressure can redirect flows.

    + evidence: Traditional: "The Gentle. Success through what is small."

  + NEXT-STEPS:
    - Choose a small, repeatable influence.
    - Track gradual alignment.
    - Pair with ䷲ (震) if momentum stalls.

@ct-interpretation
  :vision
  {:objects #{:unaligned :aligned}
   :morphism {:id :nudge
              :source :unaligned
              :target :aligned
              :type :gentle}}

  :as-morphism
  {:domain :misaligned-state
   :codomain :aligned-state
   :preservation :high
   :composition :slowly-guides}

  :adapt
  {:trigger :no-progress
   :evidence [:stasis :drift]
   :target :hexagram-51-zhen}

@ant-interpretation
  :policy
  {:mode-bias :maintain
   :policy-priors {:forage 0.3 :return 0.3 :hold 0.25 :pheromone 0.15}
   :description "Steady alignment with gentle nudges"}

  :precision
  {:Pi-o 0.7
   :tau 0.5
   :description "Attentive and steady"}

  :pattern-sense
  {:trail-follow 0.6
   :gradient-use 0.5
   :novelty-seek 0.2
   :description "Follow gradients, small exploration"}

  :adapt-trigger
  {:condition :no-shift
   :interpretation "Gentle influence stalled"
   :switch-to :hexagram-51-zhen}

  :failure-mode
  "Too much softness causes drift"

@mmca-interpretation
  :sigil-encoding
  {:sigil "巽"
   :bits "01101100"
   :interpretation "Low-amplitude guided intervention"}

  :exotype-params
  {:rotation 1
   :match-threshold 0.6
   :invert-on-phenotype? false
   :update-prob 0.35
   :mix-mode :rotate-right
   :description "Directional gentle mixing"}

  :tensor-interpretation
  {:operation :gentle-rotation
   :description "Small nudges that preserve structure"}

  :regime-prediction
  {:expected :aligned
   :dynamics "Slow convergence with preserved diversity"
   :eoc-relevance "Potentially stabilizing for EoC"}

@domain-transfer-notes
  "巽 is the pattern of GENTLE INFLUENCE across domains:
   - In ants: steady correction via mild trail following
   - In CA: slow rotation preserving structure
   - In dialogue: gradual persuasion"
```
