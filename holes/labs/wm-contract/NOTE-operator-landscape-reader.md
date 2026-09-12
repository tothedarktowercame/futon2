# NOTE: the operator-landscape reader — the third zaif crew member

Joe with zai-7 (zai), 2026-09-12. Joe's framing: read the evidence
landscape — associate free text through the embedding to mined patterns,
elaborate patterns and turns with free-text search results (as the APM
loop does) — to model what is going on in operator turns, because the
operator turns currently represent ALL non-automated stakeholders
(everything the War Machine does not yet automate). Better pattern
cascades out of that reading feed improved automation workflows. This is
the "closing the AIF over the operator" thread from the What Problems
Are We Solving paper (p4ng/futon-2026.tex).

Prior work this builds on (and must not repeat):
`E-operator-as-attached-agent/FINDINGS-embedding-pipeline.md` (2026-09-04)
already measured the guesser.

## What the measured substrate licenses (R7 rows, pre-earned)

| channel | measured property | precision row |
|---|---|---|
| MiniLM top-3 tags | top-1 changes 77% under word-shuffle; salad ≥ real text in score; no floor exists | low; seeder only, never authority (P2's ruling, restated) |
| coverage 86% | below random-vector null; hub-dominated; 196 never-fired | coverage is volume, not signal |
| FTS text search | the cross-check that found the scan defect | different failure modes than embedding — the elaboration arm |
| declared marks (operator rulings) | precision 1 by construction | the only raiser |

Consequence for design: this agent's SMELL is the embedding (cheap, broad,
wrong often) and its LOOK is FTS + hydration + re-check (the
candidate-generator-never-authority stack). Elaboration goes THROUGH the
text, not through the vector: the embedding proposes pattern candidates
for a turn; the reader then pulls the pattern's text and the turn's
full text and records an association with the passages that justify it.
That fixes the found defect that the embedded input is not recorded: the
association record must carry the full input text, the corpus version
stamp, and the justification excerpt.

## The agent: b-landscape-reader (a new family — cartography, not homeostasis)

Not a coherence keeper: nothing here is dirty or stale; the landscape
is being MAPPED. Board sketch:

- SMELL operator-turn window (author joe, since T) → candidate turns.
- LOOK embedding-tags for each turn (top-3, scores recorded, floorless
  BY MEASUREMENT — the scores are data, not filters).
- LOOK' FTS elaboration: for each tagged pattern, its text + the turn's
  full text; record association {:turn-id :pattern-id :score
  :corpus-version :justification-excerpt}.
- FEEL the cold families: associations that would reach the 196
  never-fired patterns are worth more EIG than another hit on
  hexagram-24 (hub avoidance as a felt-for thing, priced in).
- ZAP forbidden (N1-class safety): the reader writes association
  records, never cascades. Cascade construction from associations is a
  separate, authored act (L4/L3), because a cascade IS a policy.

## What the reading is FOR (Joe's stakeholder point)

The operator turn is the proxy-C for every stakeholder not yet on the
mesh: when the WM automates nothing, everything external arrives through
Joe's voice. So the landscape map is the C-belief substrate the ask arm
prices against: clusters of turns that associate with the same cold
patterns are candidate NEW cascade families — recurring operator concerns
that no cascade yet serves. The map's growth rate is the automation
frontier moving. When a stakeholder (Rebecca, UKRN) gets their own
channel later, this same reader binds to it with a per-author precision
row — the board does not change.

## Sequencing and honesty

- Corpus version stamps and full-input recording are PRECONDITIONS (the
  findings name both as holes). Land them before the first association
  record, or the map inherits the unstamped-corpus defect.
- Duplicate-slot fix (dedupe by id in the ranker) is a one-line repair
  upstream in notions_search.py — do it, don't route around it.
- The 32 disagreeing days (scan vs FTS) stay listed, not averaged: the
  reader consumes the day-windowed producer only.

Row links: NOTE-agent-needs-from-issue-board (this is the N2/C-belief
complement the hierarchy was missing); FINDINGS-embedding-pipeline (the
measurements); SPEC-chip-boards-v0 §3½ (the reader sits at L1-L2 and
feeds L4); M-zaif-harness (the C-belief arm this operationalizes).
