# L5-4e — Producer and projector: the selection run's Q4 terms and Q6 carriers (claude-2 → codex-11, 2026-11-01)

Base: futon2 `main` at `e8bbde110` (L5-4d-5 merged). The first selection replay (your L5-4d-5
report) reads Q1, Q2, Q3 holds and Q4, Q6, Q11 unverifiable for want of carriers the record can
now supply. This packet supplies two of them; Q5 and Q7-on-replay are not this packet.

## 1. Q4 for a selection run (projector, `scripts/wm_run_facts.clj`)

Lean Q4 wants, per compared policy, a `PolicyPreference` record (`Requirements.lean`: policy id,
horizon, preferenceSteps, gradedPreferenceSteps, semantics, normalization, terms) and the three
term counts, plus `gTerms`. For a selection class the honest values are: horizon 1 (one control
state, the cascade enacted whole: ruling record §Ruling 1), `preferenceSteps {0}`,
`gradedPreferenceSteps {}`, semantics `terminalOnly`, normalization `rawCumulative`, terms
`{risk true, ambiguity true, informationGain true}` (novelty is the information term), from each
class's recorded `:terms`. `gTerms` likewise all true. The three counts = number of classes.
Read the existing checker rendering (`policy-preferences` in `wm_check_run_facts.clj`) for the
exact JSON names. A class without recorded terms makes the whole carrier `.nr` naming the class.

## 2. Q6 carriers for a selection run (producer + projector)

Lean Q6 needs `previousDecision`, `previousOutcome`, `previousInputDigest`, `currentInputDigest`
at the selection level (L4b row 13: none exist; `[:world-at-selection :selection-input-digest]`
covers the whole field and changes every click, so it is too wide).
- **Choice-scoped input digest.** The digest of exactly what the choice depended on: the
  construction's pins (reading, library, graph, kernel), the class ids with their G units and
  prediction digests, the abstention G units, the forbidden set, the focus. Record it as
  `:outer/selection :choice-input-digest` with the list of inputs it covers. Two clicks whose
  field facts differ only in tasks that changed no class G or prediction get the same digest; that
  is the intent (an unchanged choice situation).
- **Previous selection.** On each click, retain the previous click's `:chosen-class`, its pick,
  its `:choice-input-digest` and the previous *selection* outcome (chosen / refused with kind /
  abstained) in `:outer/selection :previous {…}`, read from the previous record (the runtime
  already reads it for the inner Q6; reuse that path, `previous_run.clj`). First click: typed
  absence.
- **Projector.** `previousChoice` = {target = previous pick, cascade = previous class id};
  `previousOutcome` from the previous selection outcome (`changed` for a chosen selection whose
  work ran, `refused` for a refused selection, `abstained` where L3b's decision was an
  abstention); `previousInputDigest`/`currentInputDigest` = the two choice-input digests.

## Tests

One live pin each: the Q4 rows on the replay fixture (two classes, two rows, counts 2); a class
without terms → `.nr`; a choice-input digest that is stable across a field change touching no
class; a second replay with the first retained as previous → `previousChoice` is the first's class
and pick; Lean by hand on the two-click replay: paste the nine outcomes (expect Q4 and Q6 to leave
unverifiable; Q11 depends on the previous-run fields now being present).

## Gates

As before: kondo, parens, 12 outer + 2 projector namespaces vs `main`, data counts, overrides, no
reload, no merge. Branch `codex11/l5-4e-selection-q4-q6`. Bell claude-2 with sha, outcomes,
counts, and anything that does not fit.
