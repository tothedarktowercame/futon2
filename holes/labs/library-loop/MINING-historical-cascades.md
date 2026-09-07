# MINING-historical-cascades — commissioned 2026-09-07

**Commissioned by Joe** (session, 2026-09-07, during the fundamentals rulings
walkthrough, item 9): "alongside your structural proposition, we need to start
up a new mining loop that actually goes and produces some historical cascades
that we can use as a source of data."

## Why (the data-availability problem)

Three measured bounds all trace to the same absence:
- the whole recorded cascade corpus carries **one distinct rule id**
  (`:widen-the-cascade-only-on-evidence`), so no run can distinguish
  rule-grain attribution from a boolean flag (C558 §6);
- the maximum rule-carrying member count on any recorded run is **1**, so O4
  is unexercised except by plants (C558 §4–5);
- the sole producer writes `:added-by-organise #{}` and
  `:precedence-before []` as literals, so O1's second origin and O4's
  antecedent cannot appear in data it emits.

Joe's ruling on `:organise-third-origin` (arm 4) is structurally sound but,
per his rider, *meaningful only once the corpus can exercise the grain* —
"that limitation needs to be addressed in order for any decision here to be
meaningful."

## What (Joe's vision, near-verbatim)

Cascades are meant to correspond to **production rule systems**, and real ones
are *complex*. Patterns have authored THEN statements that adapt into
production rules "through an attested interpretation, or, if it's not
attested, a documented interpretation." The mining loop upgrades what the
library loop and the why-how work were doing:

- **Reconstitute the Cascade Live problem hierarchies as cascades that solved
  them.** The W2 pushout grid organized the solved problems into clusters
  under an apex; those hierarchies become cascade records.
- **The mission-cloth precedent**: in the predecessor to the Cascade Live
  page, each mission cloth was aligned with a (simplified) cascade naming
  exactly which patterns were involved in creating that solution. Same move,
  now at full fidelity.

## Sources already in hand

- `runs/W1-witness-survey.md` (this lab): 11 witnessed solutions, 6 mapped to
  authored problem nodes — the mapped six are the first mining targets.
- `futon3c/holes/excursions/apex-thesis.json`: the four clusters + apex.
- `futon3 library/problems/` (60 nodes) and the fresh @why/@how edges from
  rounds A1–A5: the pattern-to-problem linkage the cascades will cite.
- `futon3:checks/*.edn` cascade records: the target format (nodes, authored
  edges, selected/admitted, rules, precedence, transcript).

## Product shape (per mined cascade)

One EDN cascade record per historical solved problem, in the
`futon3:checks` format, where:
- **nodes** are the patterns actually involved in the solution;
- **edges** are authored (@why/@how-backed, cited to spans);
- **rules** cite an authored THEN with the span inside the THEN block (the
  `:rule-does-not-encode-an-authored-then` gate failure is the cautionary
  example), each with an *attested* interpretation where a source attests it
  and a *documented* one otherwise, the class marked in-band;
- **selected vs admitted** distinguished honestly, with provenance — this is
  what populates the third-origin data the arm-4 ruling needs;
- at least some records carrying **two or more rule-bearing members**, which
  is the single scarcest thing in the current corpus.

## Discipline

- Mining, not fitting: a cascade describes the solution that *happened*
  (commits, run records, mission documents as witnesses), and is never tuned
  to make a law pass. Tuning to a law's *exercisability* — constructing so
  that two rules act — is admissible per the `:organise-o4-after-the-law-encoding`
  ruling, marked as constructed where done.
- Same evidentiary shape as every ruling today: **first one exemplar** (one
  well-witnessed W1 solution → one cascade record, reviewed), then loop.
- Zai quota returns 2026-09-08 02:04Z; role-fidelity mining seats can move
  there once the exemplar sets the format. Until then, codex.

## Status

- 2026-09-07: commissioned; exemplar dispatch prepared (see walkthrough
  item 9 and dispatch record below when it lands).
