# E-kimi-task-165 — pattern-stage reading kimi-5-1790889936 (10 patterns)

**Requisition:** completed — 2026-10-01T21:26:21Z, job invoke-1790889936890-29862-c8a06381, state done

**VERDICT (2026-10-09, provisional):** DONE — Requisition completed with state done; pattern-stage reading delivered. _(WM status classification by zai-4, high confidence; not yet confirmed by the author.)_

Clocked in by pattern-stage-read-loop for kimi-5 on 2026-10-01 (one Kimi task, one excursion, so the seat's
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

Answer: write /tmp/claude17/pattern-stage-read/kimi-5-1790889936.answer.json as a JSON array, one object per pattern:
  {"id": ..., "kind": ..., "stage": ..., "stage-mode": ..., "confidence": ..., "node": optional, "quote-field": ..., "quote": ..., "rationale": ...}
Rewrite the file after each pattern and check it parses: python3 -c "import json;print(len(json.load(open('/tmp/claude17/pattern-stage-read/kimi-5-1790889936.answer.json'))))"
Do all 10. Do not write anywhere else, do not commit, do not publish. Nobody needs belling; end with one line: the answer path and the count.


---
## iching/hexagram-04-meng
source: futon3/library/iching/hexagram-04-meng.flexiarg

```
@flexiarg iching/hexagram-04-meng
@title ䷃ 蒙 (Méng) - Youthful Folly / Envelopment
@sigils [👈/少]
@binary 010001
@trigrams [mountain/water]
@number 4
@audience improvisers, system designers, CT practitioners
@tone foundational
@style pattern

! conclusion: Immature energy needs guidance; learning requires discipline and patience.

  + context: A system is active but naive, prone to error.

  + IF:
    Signals are noisy and actions are misaligned.

  + HOWEVER:
    Excess correction discourages growth.

  + THEN:
    Provide clear constraints and allow iterative learning.

  + BECAUSE:
    Early systems grow through guidance, not through force.

    + evidence: Traditional: "Youthful folly. It is not I who seek the young fool; the young fool seeks me."

  + NEXT-STEPS:
    - Define a small set of clear rules.
    - Encourage feedback without harsh punishment.
    - Pair with ䷂ (屯) to stabilize learning.

@ct-interpretation
  :vision
  {:objects #{:naive :guided}
   :morphism {:id :teach
              :source :naive
              :target :guided
              :type :constraint}}

  :as-morphism
  {:domain :noisy-state
   :codomain :disciplined-state
   :preservation :moderate
   :composition :supports-learning}

  :adapt
  {:trigger :stagnant-learning
   :evidence [:repetition :no-improvement]
   :target :hexagram-03-zhun}

@ant-interpretation
  :policy
  {:mode-bias :maintain
   :policy-priors {:forage 0.3 :return 0.3 :hold 0.25 :pheromone 0.15}
   :description "Guided exploration with constraints"}

  :precision
  {:Pi-o 0.7
   :tau 0.5
   :description "Balanced guidance and exploration"}

  :pattern-sense
  {:trail-follow 0.6
   :gradient-use 0.5
   :novelty-seek 0.3
   :description "Follow hints; allow modest exploration"}

  :adapt-trigger
  {:condition :no-learning
   :interpretation "Need to re-seed turbulence"
   :switch-to :hexagram-03-zhun}

  :failure-mode
  "Over-constraint suppresses learning"

@mmca-interpretation
  :sigil-encoding
  {:sigil "蒙"
   :bits "01000100"
   :interpretation "Guided mixing with constraints"}

  :exotype-params
  {:rotation 0
   :match-threshold 0.65
   :invert-on-phenotype? false
   :update-prob 0.45
   :mix-mode :majority
   :description "Constraint-biased coordination"}

  :tensor-interpretation
  {:operation :guided-projection
   :description "Mixing constrained toward structure"}

  :regime-prediction
  {:expected :learning
   :dynamics "Emergent structure with corrections"
   :eoc-relevance "EoC-adjacent if guidance stays light"}

@domain-transfer-notes
  "蒙 is the pattern of GUIDED LEARNING across domains:
   - In ants: constrained exploration
   - In CA: structured emergence"
```


---
## iching/hexagram-12-pi
source: futon3/library/iching/hexagram-12-pi.flexiarg

```
@flexiarg iching/hexagram-12-pi
@title ䷋ 否 (Pǐ) - Obstruction / Standstill
@sigils [🎑/天]
@binary 111000
@trigrams [heaven/earth]
@number 12
@audience improvisers, system designers, CT practitioners
@tone foundational
@style pattern

! conclusion: When heaven and earth separate, circulation stops; progress stalls until the poles meet again.

  + context: A system has lost exchange between initiative and support; movement continues but no longer connects.

  + IF:
    The strong withdraws upward, the receptive sinks downward, and the middle empties.

  + HOWEVER:
    Standstill is not the end; it is a sign that the channel is broken, not that force is absent.

  + THEN:
    Restore circulation by re-linking the poles. Reduce domination, invite reciprocity, and reopen the path.

  + BECAUSE:
    Sustained dynamics require exchange. When the channel breaks, energy persists but coherence fades.

    + evidence: Traditional: "Heaven and earth do not unite: the image of Obstruction."

  + NEXT-STEPS:
    - Identify the broken channel (where exchange stopped).
    - Create a small conduit that forces interaction.
    - Pair with hexagram 11 (泰) to re-establish balance.

@ct-interpretation
  :vision
  {:objects #{:upper :lower :separation}
   :morphisms [{:id :withdraw :source :lower :target :separation}
               {:id :ascend :source :upper :target :separation}]}

  :as-morphism
  {:domain :coupled-state
   :codomain :decoupled-state
   :preservation :low
   :composition :breaks-cycle}

  :adapt
  {:trigger :stagnation
   :evidence [:no-exchange :drift :local-collapse]
   :target :hexagram-11-tai}

@ant-interpretation
  :policy
  {:mode-bias :maintain
   :policy-priors {:forage 0.2 :return 0.2 :hold 0.4 :pheromone 0.2}
   :description "Hold position; low circulation between outbound/homebound"}

  :precision
  {:Pi-o 0.7
   :tau 0.3
   :description "Cautious, low-switching regime; prefers local stability"}

  :pattern-sense
  {:trail-follow 0.6
   :gradient-use 0.3
   :novelty-seek 0.1
   :description "Follow local traces, avoid new commitments"}

  :adapt-trigger
  {:condition :no-progress
   :interpretation "Standstill detected; re-open circulation"
   :switch-to :hexagram-11-tai}

  :failure-mode
  "Stuck ants oscillate locally; neither gather nor return reliably"

@mmca-interpretation
  :sigil-encoding
  {:sigil "否"
   :bits "11100000"
   :interpretation "Separated pressure: strong top, weak bottom"}

  :exotype-params
  {:rotation 2
   :match-threshold 0.85
   :invert-on-phenotype? false
   :update-prob 0.15
   :mix-mode :none
   :description "Low intervention with strict matching; decoupled flow"}

  :tensor-interpretation
  {:operation :blocked-coupling
   :description "Partial projection with suppressed exchange"
   :matrix-form "M = diag([1, 1, 0, 0])"
   :eigenstructure "Two stable subspaces with weak coupling"}

  :regime-prediction
  {:expected :stagnation
   :dynamics "Separated domains; weak or absent exchange"
   :eoc-relevance "Anti-EoC: breaks circulation needed for sustained heterogeneity"}

@morphisms-to-other-hexagrams
  :inverse
  {:hexagram :hexagram-11-tai
   :relationship :inverse
   :transformation "When circulation resumes, obstruction dissolves"}

  :degenerations
  [{:target :hexagram-02-kun
    :mechanism "Receptive dominates; initiative withdraws"}
   {:target :hexagram-01-qian
    :mechanism "Creative dominates; receptivity withdraws"}]

@domain-transfer-notes
  "否 is the pattern of SEPARATION across domains:
   - In ants: stalled circulation between gather and return
   - In CA: regions that decouple and freeze
   - In argument: parties talk past each other
   - In code: components no longer exchange state"
```


---
## iching/hexagram-21-shihe
source: futon3/library/iching/hexagram-21-shihe.flexiarg

```
@flexiarg iching/hexagram-21-shihe
@title ䷔ 噬嗑 (Shì Hé) - Biting Through
@sigils [🌆/亏]
@binary 101100
@trigrams [thunder/fire]
@number 21
@audience improvisers, system designers, CT practitioners
@tone foundational
@style pattern

! conclusion: Obstacles require decisive action; clarity guides cutting through.

  + context: A system faces a blockage or corruption.

  + IF:
    Progress is stopped by a hard obstacle.

  + HOWEVER:
    Excess force can damage the system.

  + THEN:
    Act decisively and precisely to remove the obstruction.

  + BECAUSE:
    Clarity plus force resolves blockages without chaos.

    + evidence: Traditional: "Biting through. Success."

  + NEXT-STEPS:
    - Identify the obstruction.
    - Apply targeted force.
    - Pair with ䷕ (賁) to restore form.

@ct-interpretation
  :vision
  {:objects #{:blocked :cleared}
   :morphism {:id :cut
              :source :blocked
              :target :cleared
              :type :decisive}}

  :as-morphism
  {:domain :obstructed
   :codomain :cleared
   :preservation :low
   :composition :cuts-through}

  :adapt
  {:trigger :overdamage
   :evidence [:excess-force :instability]
   :target :hexagram-22-bi}

@ant-interpretation
  :policy
  {:mode-bias :adaptive
   :policy-priors {:forage 0.4 :return 0.25 :hold 0.1 :pheromone 0.25}
   :description "Targeted clearing via exploration"}

  :precision
  {:Pi-o 0.7
   :tau 0.6
   :description "Decisive but responsive"}

  :pattern-sense
  {:trail-follow 0.3
   :gradient-use 0.5
   :novelty-seek 0.6
   :description "Break through blockages"}

  :adapt-trigger
  {:condition :overdamage
   :interpretation "Restore form"
   :switch-to :hexagram-22-bi}

  :failure-mode
  "Overforce damages structure"

@mmca-interpretation
  :sigil-encoding
  {:sigil "噬嗑"
   :bits "10110000"
   :interpretation "Targeted disruptive intervention"}

  :exotype-params
  {:rotation 2
   :match-threshold 0.45
   :invert-on-phenotype? true
   :update-prob 0.6
   :mix-mode :xor-neighbor
   :description "Cut-through mixing"}

  :tensor-interpretation
  {:operation :cut
   :description "Remove obstruction by selective disruption"}

  :regime-prediction
  {:expected :clearing
   :dynamics "Disruption followed by new structure"
   :eoc-relevance "Can seed EoC after blockage"}

@domain-transfer-notes
  "噬嗑 is the pattern of CUTTING THROUGH across domains:
   - In ants: break out of stuck loops
   - In CA: targeted disruption"
```


---
## iching/hexagram-29-kan
source: futon3/library/iching/hexagram-29-kan.flexiarg

```
@flexiarg iching/hexagram-29-kan
@title ䷜ 坎 (Kǎn) - The Abysmal (Water)
@sigils [🌊/水]
@binary 010010
@trigrams [water/water]
@number 29
@audience improvisers, system designers, CT practitioners
@tone foundational
@style pattern

! conclusion: Danger repeats; persistence and careful steps are required to pass through.

  + context: A system faces repeated hazards or uncertainty.

  + IF:
    Each move risks loss; signals are noisy and survival is threatened.

  + HOWEVER:
    Fear can paralyze; overreaction amplifies the danger.

  + THEN:
    Proceed with steady, cautious movement; avoid sharp swings.

  + BECAUSE:
    In deep uncertainty, small reliable steps sustain the path.

    + evidence: Traditional: "The Abysmal repeated. If you are sincere, you have success in your heart."

  + NEXT-STEPS:
    - Reduce amplitude of interventions.
    - Maintain situational awareness.
    - Pair with ䷝ (離) for illumination.

@ct-interpretation
  :vision
  {:objects #{:hazard :safe-passage}
   :morphism {:id :navigate
              :source :hazard
              :target :safe-passage
              :type :careful}}

  :as-morphism
  {:domain :uncertain-state
   :codomain :survivable-state
   :preservation :moderate
   :composition :stabilizes-under-risk}

  :adapt
  {:trigger :panic
   :evidence [:oscillation :overreaction]
   :target :hexagram-58-dui}

@ant-interpretation
  :policy
  {:mode-bias :maintain
   :policy-priors {:forage 0.2 :return 0.35 :hold 0.3 :pheromone 0.15}
   :description "Cautious movement; prioritize survival"}

  :precision
  {:Pi-o 0.8
   :tau 0.3
   :description "High observation precision; conservative switching"}

  :pattern-sense
  {:trail-follow 0.7
   :gradient-use 0.7
   :novelty-seek 0.1
   :description "Follow known safe paths"}

  :adapt-trigger
  {:condition :panic-detected
   :interpretation "Overreaction to danger"
   :switch-to :hexagram-58-dui}

  :failure-mode
  "Frozen caution prevents resource acquisition"

@mmca-interpretation
  :sigil-encoding
  {:sigil "坎"
   :bits "01001000"
   :interpretation "Cautious partial intervention"}

  :exotype-params
  {:rotation 0
   :match-threshold 0.75
   :invert-on-phenotype? false
   :update-prob 0.25
   :mix-mode :none
   :description "Low intervention, high selectivity"}

  :tensor-interpretation
  {:operation :stability-filter
   :description "Suppress volatility under risk"}

  :regime-prediction
  {:expected :cautious
   :dynamics "Slow, stable motion"
   :eoc-relevance "Stabilizes EoC when volatility rises"}

@domain-transfer-notes
  "坎 is the pattern of DANGER NAVIGATION across domains:
   - In ants: cautious foraging/return
   - In CA: stable filtering under turbulence
   - In dialogue: careful progression through uncertainty"
```


---
## iching/hexagram-34-dazhuang
source: futon3/library/iching/hexagram-34-dazhuang.flexiarg

```
@flexiarg iching/hexagram-34-dazhuang
@title ䷡ 大壯 (Dà Zhuàng) - Great Power
@sigils [🎶/己]
@binary 111100
@trigrams [thunder/heaven]
@number 34
@audience improvisers, system designers, CT practitioners
@tone foundational
@style pattern

! conclusion: Great power must be disciplined; strength without restraint leads to harm.

  + context: A system has accumulated strong momentum.

  + IF:
    Power is high and action is tempting.

  + HOWEVER:
    Unchecked force creates backlash or collapse.

  + THEN:
    Apply strength with restraint and timing.

  + BECAUSE:
    Power is sustainable only when governed.

    + evidence: Traditional: "Great Power. Perseverance furthers."

  + NEXT-STEPS:
    - Identify the safe channel for strength.
    - Restrain excess.
    - Pair with ䷠ (遯) when overreach appears.

@ct-interpretation
  :vision
  {:objects #{:powerful :disciplined}
   :morphism {:id :govern
              :source :powerful
              :target :disciplined
              :type :restraint}}

  :as-morphism
  {:domain :high-energy
   :codomain :disciplined-energy
   :preservation :moderate
   :composition :channels-power}

  :adapt
  {:trigger :overreach
   :evidence [:backlash :instability]
   :target :hexagram-33-dun}

@ant-interpretation
  :policy
  {:mode-bias :outbound
   :policy-priors {:forage 0.55 :return 0.2 :hold 0.1 :pheromone 0.15}
   :description "Strong outward push with restraint"}

  :precision
  {:Pi-o 0.6
   :tau 0.4
   :description "Directed strength"}

  :pattern-sense
  {:trail-follow 0.4
   :gradient-use 0.6
   :novelty-seek 0.4
   :description "Advance with control"}

  :adapt-trigger
  {:condition :overreach
   :interpretation "Retreat to preserve strength"
   :switch-to :hexagram-33-dun}

  :failure-mode
  "Overpowering action causes collapse"

@mmca-interpretation
  :sigil-encoding
  {:sigil "大壯"
   :bits "11110000"
   :interpretation "Strong intervention with potential overreach"}

  :exotype-params
  {:rotation 2
   :match-threshold 0.5
   :invert-on-phenotype? true
   :update-prob 0.65
   :mix-mode :scramble
   :description "Strong intervention"}

  :tensor-interpretation
  {:operation :power
   :description "High-energy transformation"}

  :regime-prediction
  {:expected :high-energy
   :dynamics "Strong motion with risk of instability"
   :eoc-relevance "Can push beyond EoC if unchecked"}

@domain-transfer-notes
  "大壯 is the pattern of GREAT POWER across domains:
   - In ants: strong exploration with restraint
   - In CA: high-energy dynamics"
```


---
## iching/hexagram-44-gou
source: futon3/library/iching/hexagram-44-gou.flexiarg

```
@flexiarg iching/hexagram-44-gou
@title ䷫ 姤 (Gòu) - Coming to Meet
@sigils [🌚/见]
@binary 011111
@trigrams [heaven/wind]
@number 44
@audience improvisers, system designers, CT practitioners
@tone foundational
@style pattern

! conclusion: Encountering a powerful influence requires restraint; one must not be seduced blindly.

  + context: A system meets a strong new force.

  + IF:
    A sudden influence appears.

  + HOWEVER:
    Immediate attachment can lead to loss of control.

  + THEN:
    Meet the force with caution; integrate only what is stable.

  + BECAUSE:
    Encounters can transform the system if handled with restraint.

    + evidence: Traditional: "Coming to meet. The maiden is powerful."

  + NEXT-STEPS:
    - Identify the new influence.
    - Limit exposure.
    - Pair with ䷪ (夬) if decisive action is required.

@ct-interpretation
  :vision
  {:objects #{:unknown :encountered}
   :morphism {:id :encounter
              :source :unknown
              :target :encountered
              :type :contact}}

  :as-morphism
  {:domain :unmet
   :codomain :met
   :preservation :low
   :composition :initiates-contact}

  :adapt
  {:trigger :overinfluence
   :evidence [:loss-of-control :capture]
   :target :hexagram-43-guai}

@ant-interpretation
  :policy
  {:mode-bias :adaptive
   :policy-priors {:forage 0.35 :return 0.25 :hold 0.2 :pheromone 0.2}
   :description "Cautious encounter"}

  :precision
  {:Pi-o 0.6
   :tau 0.6
   :description "Responsive to new signals"}

  :pattern-sense
  {:trail-follow 0.4
   :gradient-use 0.5
   :novelty-seek 0.5
   :description "Explore new influence with caution"}

  :adapt-trigger
  {:condition :capture
   :interpretation "Break through"
   :switch-to :hexagram-43-guai}

  :failure-mode
  "Over-attachment to new influence"

@mmca-interpretation
  :sigil-encoding
  {:sigil "姤"
   :bits "01111100"
   :interpretation "Sudden encounter"}

  :exotype-params
  {:rotation 2
   :match-threshold 0.5
   :invert-on-phenotype? true
   :update-prob 0.55
   :mix-mode :rotate-right
   :description "Encounter-driven mixing"}

  :tensor-interpretation
  {:operation :encounter
   :description "Introduce new influence"}

  :regime-prediction
  {:expected :encounter
   :dynamics "New influence alters dynamics"
   :eoc-relevance "Can disrupt or enrich EoC"}

@domain-transfer-notes
  "姤 is the pattern of ENCOUNTER across domains:
   - In ants: cautious engagement with new signals
   - In CA: new influence injection"
```


---
## iching/hexagram-47-kun
source: futon3/library/iching/hexagram-47-kun.flexiarg

```
@flexiarg iching/hexagram-47-kun
@title ䷮ 困 (Kùn) - Oppression / Exhaustion
@sigils [😄/平]
@binary 010110
@trigrams [lake/water]
@number 47
@audience improvisers, system designers, CT practitioners
@tone foundational
@style pattern

! conclusion: Exhaustion constrains movement; inner clarity preserves resilience.

  + context: A system is under pressure and energy is low.

  + IF:
    Resources are depleted and paths are limited.

  + HOWEVER:
    Despair accelerates collapse.

  + THEN:
    Preserve inner coherence and conserve resources.

  + BECAUSE:
    Endurance in adversity keeps the system alive.

    + evidence: Traditional: "Oppression. Success. Perseverance."

  + NEXT-STEPS:
    - Conserve energy.
    - Maintain internal clarity.
    - Pair with ䷯ (井) to restore resources.

@ct-interpretation
  :vision
  {:objects #{:strained :enduring}
   :morphism {:id :endure
              :source :strained
              :target :enduring
              :type :conserving}}

  :as-morphism
  {:domain :depleted
   :codomain :enduring
   :preservation :high
   :composition :conserves}

  :adapt
  {:trigger :resource-found
   :evidence [:relief :replenish]
   :target :hexagram-48-jing}

@ant-interpretation
  :policy
  {:mode-bias :return
   :policy-priors {:forage 0.2 :return 0.5 :hold 0.2 :pheromone 0.1}
   :description "Conserve and return"}

  :precision
  {:Pi-o 0.8
   :tau 0.3
   :description "Cautious endurance"}

  :pattern-sense
  {:trail-follow 0.6
   :gradient-use 0.4
   :novelty-seek 0.1
   :description "Avoid waste"}

  :adapt-trigger
  {:condition :relief
   :interpretation "Restore resources"
   :switch-to :hexagram-48-jing}

  :failure-mode
  "Exhaustion leads to collapse"

@mmca-interpretation
  :sigil-encoding
  {:sigil "困"
   :bits "01011000"
   :interpretation "Energy constrained"}

  :exotype-params
  {:rotation 0
   :match-threshold 0.8
   :invert-on-phenotype? false
   :update-prob 0.2
   :mix-mode :none
   :description "Low intervention, high constraint"}

  :tensor-interpretation
  {:operation :conserve
   :description "Preserve under pressure"}

  :regime-prediction
  {:expected :exhausted
   :dynamics "Low activity, constrained"
   :eoc-relevance "Anti-EoC unless relief arrives"}

@domain-transfer-notes
  "困 is the pattern of EXHAUSTION across domains:
   - In ants: conserving energy under strain
   - In CA: low-activity constraint"
```


---
## iching/hexagram-53-jian
source: futon3/library/iching/hexagram-53-jian.flexiarg

```
@flexiarg iching/hexagram-53-jian
@title ䷴ 漸 (Jiàn) - Gradual Progress
@sigils [🌜/引]
@binary 001011
@trigrams [wind/mountain]
@number 53
@audience improvisers, system designers, CT practitioners
@tone foundational
@style pattern

! conclusion: Gradual progress accumulates; small steps build lasting change.

  + context: A system needs steady development without disruption.

  + IF:
    Rapid change would destabilize.

  + HOWEVER:
    Too much caution can stall growth.

  + THEN:
    Advance steadily with patient accumulation.

  + BECAUSE:
    Durable change is built incrementally.

    + evidence: Traditional: "Gradual progress. The maiden is given in marriage."

  + NEXT-STEPS:
    - Identify the next small step.
    - Maintain continuity.
    - Pair with ䷵ (歸妹) if imbalance appears.

@ct-interpretation
  :vision
  {:objects #{:emerging :advancing}
   :morphism {:id :increment
              :source :emerging
              :target :advancing
              :type :gradual}}

  :as-morphism
  {:domain :slow-change
   :codomain :steady-advance
   :preservation :high
   :composition :accumulates}

  :adapt
  {:trigger :stall
   :evidence [:plateau :imbalance]
   :target :hexagram-54-guimei}

@ant-interpretation
  :policy
  {:mode-bias :maintain
   :policy-priors {:forage 0.35 :return 0.3 :hold 0.2 :pheromone 0.15}
   :description "Gradual expansion"}

  :precision
  {:Pi-o 0.7
   :tau 0.4
   :description "Steady progress"}

  :pattern-sense
  {:trail-follow 0.6
   :gradient-use 0.5
   :novelty-seek 0.2
   :description "Incremental exploration"}

  :adapt-trigger
  {:condition :imbalance
   :interpretation "Correct misalignment"
   :switch-to :hexagram-54-guimei}

  :failure-mode
  "Progress stalls due to overcaution"

@mmca-interpretation
  :sigil-encoding
  {:sigil "漸"
   :bits "00101100"
   :interpretation "Gradual alignment"}

  :exotype-params
  {:rotation 1
   :match-threshold 0.65
   :invert-on-phenotype? false
   :update-prob 0.35
   :mix-mode :rotate-right
   :description "Stepwise alignment"}

  :tensor-interpretation
  {:operation :increment
   :description "Gradual progression"}

  :regime-prediction
  {:expected :gradual
   :dynamics "Slow stable advancement"
   :eoc-relevance "Stabilizes EoC by smoothing"}

@domain-transfer-notes
  "漸 is the pattern of GRADUAL PROGRESS across domains:
   - In ants: steady expansion
   - In CA: gradual alignment"
```


---
## iiching/exotype-092
source: futon3/library/iiching/exotype-092.flexiarg

```
@flexiarg iiching/exotype-092
@title Exotype 092 (0x5C)
@sigils [🌹/己]
@bits 01011100
@number 92
@hex 0x5C
@hamming-weight 4
@audience exotype implementers, mmca users
@tone reference
@style pattern
@futon5-ref futon5/resources/exotype-program-manifest.edn; futon5/resources/exotype-xenotype-lift.edn; futon5/src/futon5/ct/exotype_programs.clj; futon5/src/futon5/mmca/exotype.clj
@futon5-manifest-path futon5/resources/exotype-program-manifest.edn
@futon5-lift-path futon5/resources/exotype-xenotype-lift.edn
@why [problems/exotype-encoding-programme]

! conclusion: Exotype 092 is encoded; program/params mapped from futon5; lift mapping not yet in registry.

  + context: Canonical 8-bit encoding for exotype 092.

  + IF:
    You need an explicit, searchable exotype record.

  + HOWEVER:
    The lift registry has no mapping for this exotype yet.

  + THEN:
    Use the encoding fields for identification and the futon5 manifest for
    program/params until lift mappings land.

  + BECAUSE:
    Explicit encodings make exotype knowledge inspectable and comparable.

  + NEXT-STEPS:
    - blocked-by[futon5-exotype-lift]

@exotype-encoding
  :bit-order :msb->lsb
  :bits-b7..b0 [0 1 0 1 1 1 0 0]

@exotype-program
  :program-id :contextual-mutate+mix
  :program-name "contextual-mutate+mix"
  :program-kind :stochastic-kleisli
  :tier :super
  :scope :kernel-transformer
  :inputs {:kernel-transformer [:kernel-spec :context :params :rng], :context-step [:history :rng]}
  :outputs [:kernel-spec]
  :invariants [:normalize :probabilistic-update]
  :word-variation :by-tier
  :notes "From futon5/resources/exotype-program-manifest.edn."


@exotype-lift
  :pattern-ids []
  :ct-template nil
  :lift-rules nil
  :evidence []
  :notes "No lift mapping for bits 01011100 in futon5/resources/exotype-xenotype-lift.edn."


@exotype-params
  :rotation 1
  :match-threshold 0.4444444444444444
  :invert-on-phenotype? true
  :update-prob 0.25
  :mix-mode :xor-neighbor
  :mix-shift 2
  :notes "From futon5/resources/exotype-program-manifest.edn."
```


---
## iiching/exotype-179
source: futon3/library/iiching/exotype-179.flexiarg

```
@flexiarg iiching/exotype-179
@title Exotype 179 (0xB3)
@sigils [🌹/己]
@bits 10110011
@number 179
@hex 0xB3
@hamming-weight 5
@audience exotype implementers, mmca users
@tone reference
@style pattern
@futon5-ref futon5/resources/exotype-program-manifest.edn; futon5/resources/exotype-xenotype-lift.edn; futon5/src/futon5/ct/exotype_programs.clj; futon5/src/futon5/mmca/exotype.clj
@futon5-manifest-path futon5/resources/exotype-program-manifest.edn
@futon5-lift-path futon5/resources/exotype-xenotype-lift.edn
@why [problems/exotype-encoding-programme]

! conclusion: Exotype 179 is encoded; program/params mapped from futon5; lift mapping not yet in registry.

  + context: Canonical 8-bit encoding for exotype 179.

  + IF:
    You need an explicit, searchable exotype record.

  + HOWEVER:
    The lift registry has no mapping for this exotype yet.

  + THEN:
    Use the encoding fields for identification and the futon5 manifest for
    program/params until lift mappings land.

  + BECAUSE:
    Explicit encodings make exotype knowledge inspectable and comparable.

  + NEXT-STEPS:
    - blocked-by[futon5-exotype-lift]

@exotype-encoding
  :bit-order :msb->lsb
  :bits-b7..b0 [1 0 1 1 0 0 1 1]

@exotype-program
  :program-id :contextual-mutate+mix
  :program-name "contextual-mutate+mix"
  :program-kind :stochastic-kleisli
  :tier :super
  :scope :kernel-transformer
  :inputs {:kernel-transformer [:kernel-spec :context :params :rng], :context-step [:history :rng]}
  :outputs [:kernel-spec]
  :invariants [:normalize :probabilistic-update]
  :word-variation :by-tier
  :notes "From futon5/resources/exotype-program-manifest.edn."


@exotype-lift
  :pattern-ids []
  :ct-template nil
  :lift-rules nil
  :evidence []
  :notes "No lift mapping for bits 10110011 in futon5/resources/exotype-xenotype-lift.edn."


@exotype-params
  :rotation 2
  :match-threshold 0.7777777777777778
  :invert-on-phenotype? false
  :update-prob 1.0
  :mix-mode :reverse
  :mix-shift 3
  :notes "From futon5/resources/exotype-program-manifest.edn."
```
