# E-kimi-task-168 — pattern-stage reading kimi-1-1790890022 (10 patterns)

**Requisition:** completed — 2026-10-01T21:28:34Z, job invoke-1790890022090-29866-b46459fc, state done

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

Answer: write /tmp/claude17/pattern-stage-read/kimi-1-1790890022.answer.json as a JSON array, one object per pattern:
  {"id": ..., "kind": ..., "stage": ..., "stage-mode": ..., "confidence": ..., "node": optional, "quote-field": ..., "quote": ..., "rationale": ...}
Rewrite the file after each pattern and check it parses: python3 -c "import json;print(len(json.load(open('/tmp/claude17/pattern-stage-read/kimi-1-1790890022.answer.json'))))"
Do all 10. Do not write anywhere else, do not commit, do not publish. Nobody needs belling; end with one line: the answer path and the count.


---
## problems/devmap-self-consistency-standards
source: futon3/library/problems/devmap-self-consistency-standards.flexiarg

```
@flexiarg problems/devmap-self-consistency-standards
@title Problem node: Devmap self-consistency standards
@keywords problem-node, section-grain, devmap-coherence
@audience futon stack operators, library authors
@tone plain
@style pattern
@how no mechanism claimed at problem grain; the section's own patterns are the mechanisms

! conclusion: This is a problem-stating node at section grain for the devmap-coherence section, from the section's own committed documentation: keeping the FUTON devmaps self-consistent and structurally sound.

  + context: Minted by library-loop row L18, targeting the L17 advisory report's served-refused sets (devmap-coherence members appear refused in the zaif/construct cascades). Source: futon3 library/devmap-coherence/README.md, opening paragraph.

  + IF:
    Work happens in or consumes the devmap-coherence section's patterns.

  + HOWEVER:
    The section's own documentation states the problem: "keeping the FUTON devmaps self-consistent and structurally sound ... machine-checkable standards for devmap quality" (source: futon3 library/devmap-coherence/README.md, opening paragraph)

  + THEN:
    This node is the landing point for the shared @why edges from the devmap-coherence patterns (mechanical pass, row L18); per-pattern questions route to the section documentation, not to invention here.

  + BECAUSE:
    A section whose documentation states one shared problem should carry one problem node, not per-pattern copies (the L11/L15 cost finding).

    + evidence: futon3 library/devmap-coherence/README.md, opening paragraph; L17 report runs/L17-advisory-gate-report.md.
```


---
## problems/r11-hierarchical-shared-budget
source: futon3/library/problems/r11-hierarchical-shared-budget.flexiarg

```
@flexiarg problems/r11-hierarchical-shared-budget
@title Problem node R11: Hierarchical shared budget
@keywords problem-node, R11, dossier, hierarchical-shared-budget
@audience futon stack operators, library authors
@tone plain
@style pattern
@holds-at R11
@how no mechanism claimed at problem grain; what solving requires is the dossier's residual-problems list (source: futon2 holes labs wm-contract PROBLEMS-r15-r11-r17-batch5.md, R11)

! conclusion: This is a problem-stating node for control-map node R11 (Hierarchical shared budget): R11 solves how locally factored proposals share a finite resource without silently oversubscribing it: the roster places **“Hierarchical shared budget”** in SELECT (`p4ng/empirics-futon/control-stages.edn:27`; control-stage numbering).

  + context: Minted by library-loop row L5 from the twenty-node problem dossiers; it exists so @why edges from mechanism patterns have a problem node to land on.

  + IF:
    A pattern's rationale is that it answers the problem this node states.

  + HOWEVER:
    The problem, verbatim from the dossier: R11 solves how locally factored proposals share a finite resource without silently oversubscribing it: the roster places **“Hierarchical shared budget”** in SELECT (`p4ng/empirics-futon/control-stages.edn:27`; control-stage numbering). The operational criterion says that when multiple AIF agents act on shared state, a coordination layer must make their actions compose coherently (`docs/futon-aif-completeness.md:280-284`; contract R11). The catalogue makes the resource invariant concrete: local agents propose inside sub-budgets and a coordinator arbitrates the shared consumable budget at every level (`p4ng/sec-catalog.tex:243`; catalogue R11). The equation registry contains no R11 equation and classifies it as plumbing (`holes/labs/wm-contract/aif-equations.edn:472-473`; control-stage R11). R11 therefore solves globally feasible selection without erasing local factoring: compare cheap local and expensive strategic moves on one budget, refuse over-subscription, and record the arbitration boundary. (source: futon2 holes labs wm-contract PROBLEMS-r15-r11-r17-batch5.md, R11, The problem it solves)

  + THEN:
    Treat this node as the landing point for @why edges from patterns that answer it; solving the problem itself is scoped by the dossier's residual-problems list, not by this node.

  + BECAUSE:
    The census (library-loop L1) found only six problem-stating nodes for 1256 patterns, so most @why edges had nothing to land on; minting the dossier problems as nodes is the recorded-corpus repair.
    + evidence: futon2 holes labs wm-contract PROBLEMS-r15-r11-r17-batch5.md, section "## R11 — Hierarchical shared budget", subsection "The problem it solves".
```


---
## problems/r3a-prediction-error-projection
source: futon3/library/problems/r3a-prediction-error-projection.flexiarg

```
@flexiarg problems/r3a-prediction-error-projection
@title Problem node R3a: Prediction-error projection
@keywords problem-node, R3a, dossier, prediction-error-projection
@audience futon stack operators, library authors
@tone plain
@style pattern
@holds-at R3a
@how no mechanism claimed at problem grain; what solving requires is the dossier's residual-problems list (source: futon2 holes labs wm-contract PROBLEMS-r1-r3-r3a-batch3.md, R3a)

! conclusion: This is a problem-stating node for control-map node R3a (Prediction-error projection): R3a solves the join between an observation and the belief-derived prediction it can contradict: the roster places **“Prediction-error projection”** in BELIEVE and types it `:mediator` (`p4ng/empirics-futon/control-stages.edn:19`; control-stage numbering).

  + context: Minted by library-loop row L5 from the twenty-node problem dossiers; it exists so @why edges from mechanism patterns have a problem node to land on.

  + IF:
    A pattern's rationale is that it answers the problem this node states.

  + HOWEVER:
    The problem, verbatim from the dossier: R3a solves the join between an observation and the belief-derived prediction it can contradict: the roster places **“Prediction-error projection”** in BELIEVE and types it `:mediator` (`p4ng/empirics-futon/control-stages.edn:19`; control-stage numbering). Its placement basis says it **“reads the BELIEF and not only the world”**, is recomputed inside the R3 micro-step as belief moves, and has R7 precision update as its measured consumer (`p4ng/empirics-futon/control-stages.edn:19`; control-stage R3a basis). The R3 operational section names prediction error per channel as its first sub-property and reports four likelihood-backed channels (`docs/futon-aif-completeness.md:80-98`; contract R3 sub-property, not a standalone R3a criterion). The equation registry assigns `eps_k := o_k - mu_k` to R8 rather than R3a (`holes/labs/wm-contract/aif-equations.edn:77-80`; control-stage R8 equation ownership), so R3a is the runtime projection mediator and not a second owner of `eps`. There is no catalogue R3a paragraph or standalone contract R3a row; this dossier does not infer one from neighbouring patterns. (source: futon2 holes labs wm-contract PROBLEMS-r1-r3-r3a-batch3.md, R3a, The problem it solves)

  + THEN:
    Treat this node as the landing point for @why edges from patterns that answer it; solving the problem itself is scoped by the dossier's residual-problems list, not by this node.

  + BECAUSE:
    The census (library-loop L1) found only six problem-stating nodes for 1256 patterns, so most @why edges had nothing to land on; minting the dossier problems as nodes is the recorded-corpus repair.
    + evidence: futon2 holes labs wm-contract PROBLEMS-r1-r3-r3a-batch3.md, section "## R3a — Prediction-error projection", subsection "The problem it solves".
```


---
## realtime/learn-as-you-go
source: futon3/library/realtime/learn-as-you-go.flexiarg

```
@flexiarg realtime/learn-as-you-go
@title Capture Realtime Learnings as Patterns
@sigils [🌂/日]
@audience fulab agents, musn server
@tone technical
@style pattern

! conclusion: Log what works and what fails as patterns; otherwise the loop stalls.

  + context: New workflows surface edge cases and implicit assumptions.

  + IF:
    Realtime deviations are handled ad hoc and not recorded.

  + HOWEVER:
    Early iterations can be noisy and imperfect.

  + THEN:
    Record brief realtime patterns capturing “works well / doesn’t work,” then refine with evidence.

  + BECAUSE:
    Explicit learnings reduce repeated mistakes and speed calibration.

  + NEXT-STEPS:
    - evidence: add a realtime pattern after each notable failure or success.
    - evidence-shape: {:works-well [:string], :doesnt-work [:string], :evidence [:string]}
```


---
## sidecar/per-id-audit-timeline-linkage
source: futon3/library/sidecar/per-id-audit-timeline-linkage.flexiarg

```
@flexiarg sidecar/per-id-audit-timeline-linkage
@title Per-ID Audit Timeline Linkage
@keywords timeline, audit, history, link, trace, narrative, per-id, provenance, chronology, view
@audience musn architects, governance
@tone technical
@style pattern

! conclusion: Resolve per-id timelines across linked semantic records.

  + context: Audits must show how a proposal becomes a promotion and fact, not just isolated records.

  + IF:
    Per-id queries only return exact id matches and ignore related records.

  + HOWEVER:
    Operators need a contiguous narrative for each claim.

  + THEN:
    Link timelines across relationships (proposal->promotion->fact, evidence targets, chain steps)
    and surface all related events in a single per-id view.

  + BECAUSE:
    Cross-entity linkage turns append-only logs into inspectable histories.

    + EVIDENCE: src/sidecar/store.clj; test/sidecar/store_test.clj

    + EVIDENCE: src/sidecar/inspect.clj; scripts/sidecar-inspect

    + EVIDENCE: src/sidecar/store.clj (latest-active-state); src/sidecar/cli.clj; src/sidecar/inspect.clj

  + NEXT-STEPS:
    - Add explicit ordering rules when timestamps collide.
    - Document latest active state queries in docs/sidecar-usage.md.

    + use: docs/sidecar-usage.md; docs/sidecar-demo.md
```


---
## snatch/an-unmodelled-response-stops-the-line
source: futon3/library/snatch/an-unmodelled-response-stops-the-line.flexiarg

```
@flexiarg snatch/an-unmodelled-response-stops-the-line
@title An Unmodelled Response Stops the Line
@audience players, model authors
@tone cautionary
@style pattern
@why [snatch/protect-the-unprotected-move problems/snatch-play-theory-gaps]

! conclusion: When the counterpart does something your model assigned no probability, stop and re-pose the model rather than play on.

  + context: **Play grain.** Written 2026-08-27 from a playout that produced
    exactly this: item `S-001` predicts accept or snatch, a *cautious* counterpart
    refused, and the outcome carried zero predicted mass.

  + IF:
    The counterpart produces an outcome to which your model assigned no
    probability.

  + HOWEVER:
    Play continues whether or not you notice, and the cheapest next move is
    always to repeat the policy that just met a surprise — which is trying harder
    inside a model you now know is wrong.

  + THEN:
    Stop. Name the disposition that would explain the response, add it, and
    re-derive the prediction before the next round.

  + BECAUSE:
    An outcome with zero predicted mass is not a bad draw, it is a refutation. A
    model that cannot be surprised has no falsifier; one that is surprised and
    plays on has wasted its only informative event.
    + evidence: `futon3/checks/playout_snatch.clj` — a cautious P2 fires `S-001`'s
      falsifier `O3` in round 1 of G1.

  + NEXT-STEPS:
    - Where a prediction is stated, name in advance which observation would
      refute it; where none would, the prediction is not one.
```


---
## snatch/exchange-when-both-sides-gain
source: futon3/library/snatch/exchange-when-both-sides-gain.flexiarg

```
@flexiarg snatch/exchange-when-both-sides-gain
@title Exchange When Both Sides Gain
@audience players
@tone practical
@style pattern
@see-also snatch/ask-for-surplus-not-surrender snatch/accept-an-offer-that-beats-holding
@why [problems/snatch-play-theory-gaps]
@provenance no grounding source found for this pattern; deterministic search receipt: runs/L7-no-source-check.edn (L6-L10 sweep, 2026-09-05)

! conclusion: Offer an exchange when each side can turn one-point holdings into two-point acquisitions.

  + context: **Play grain.** A Snatch or Share round in which both players still
    hold tokens and P1 can make an offer.

  + IF:
    Both players have something the other values twice as highly as its holder,
    and an exchange remains available this round.

  + HOWEVER:
    Attention to seizure risk can make abstention look like the objective, even
    though holding every token leaves the game's available surplus unrealised.

  + THEN:
    Propose a non-zero exchange whose give and ask leave both players better off
    if accepted.

  + BECAUSE:
    Each transferred token replaces one point for its giver with two points for
    its receiver. A completed bilateral exchange therefore creates value for
    both sides; protection is useful only insofar as it makes that gain reachable.
```


---
## snatch/mark-without-force
source: futon3/library/snatch/mark-without-force.flexiarg

```
@flexiarg snatch/mark-without-force
@title Mark Without Force
@audience institution designers, evidence-surface authors
@tone analytical
@style pattern
@why [snatch/protect-the-unprotected-move snatch/institutions-vary-by-position-and-force problems/snatch-play-theory-gaps]
@see-also snatch/revert-then-invert

! conclusion: Let the wronged party attach a durable visible mark that accumulates and changes nothing in the current round.

  + context: Snatch or Share, variant G3 ("token de repudio/vergüenza"). After a
    snatch, and only then, P1 chooses whether to assign a shame token. If
    assigned, `P2.shameTokens += 1` and the count is visible in the UI. No
    tokens move; the current round's payoff is untouched.

  + IF:
    You want a defection to have consequences and you have no enforcement
    apparatus, or you do not want one.

  + HOWEVER:
    A sanction that alters the current payoff needs an adjudicator, a standard of
    proof, and a way to be wrong — machinery that is expensive and that itself
    becomes something to game.

  + THEN:
    Give the wronged party the ability to attach a durable, visible mark to the
    actor. The mark accumulates across rounds and alters nothing now.

  + BECAUSE:
    A record that persists changes future play even when it changes nothing
    present. The cost of defecting becomes what the mark will mean later, which
    requires no enforcement, only memory and visibility.
    + evidence: in G3 the mark is the entire mechanism — the variant adds no
      other change to G1.

  + NEXT-STEPS:
    - Distinguish, in any sanctioning design, the mark from the penalty; they
      have different failure modes and only one needs an adjudicator.
```


---
## snatch/price-the-final-round-as-final
source: futon3/library/snatch/price-the-final-round-as-final.flexiarg

```
@flexiarg snatch/price-the-final-round-as-final
@title Price the Final Round as Final
@audience players
@tone practical
@style pattern
@see-also snatch/a-free-mark-is-always-worth-assigning snatch/accept-an-offer-that-beats-holding
@provenance no grounding source found for this pattern; deterministic search receipt: runs/L7-no-source-check.edn (L6-L10 sweep, 2026-09-05)

! conclusion: In round five, choose by the current exchange and enforceable remedy, not by a mark's future deterrence.

  + context: **Play grain.** The fifth and known-final round of Snatch or Share,
    with an offer available.

  + IF:
    No later round remains in which reputation, a shame mark, or promised future
    exchange can change this counterpart's action.

  + HOWEVER:
    A policy learned in rounds one through four may count future deterrence or
    future cooperation that the horizon has reduced to zero.

  + THEN:
    Recompute the move from this round's exchange payoff and any remedy that
    acts now; do not count a merely visible mark as a final-round return.

  + BECAUSE:
    The last round removes continuation value. G4's immediate revert-and-invert
    can still matter, while G3's effect on later play cannot.
```


---
## snatch/use-talk-to-make-a-testable-offer
source: futon3/library/snatch/use-talk-to-make-a-testable-offer.flexiarg

```
@flexiarg snatch/use-talk-to-make-a-testable-offer
@title Use Talk to Make a Testable Offer
@audience players
@tone practical
@style pattern
@why snatch/non-binding-talk-still-moves-play
@see-also snatch/probe-before-committing

! conclusion: In G5, name the next give-and-ask pair before play and compare the response with what was said.

  + context: **Play grain.** G5's pre-round channel is open before either player
    has observed the other's response this game.

  + IF:
    Cheap talk is available and the next offer and response have not yet occurred.

  + HOWEVER:
    General assurances such as “I will cooperate” neither specify a move nor
    produce an observation that can revise trust.

  + THEN:
    State a concrete give-and-ask pair and invite a concrete accept/refuse reply,
    then retain the message alongside the action that followed it.

  + BECAUSE:
    Non-binding talk creates information when it makes a prediction specific
    enough to be contradicted. The later comparison prices the speaker's next message.
```
