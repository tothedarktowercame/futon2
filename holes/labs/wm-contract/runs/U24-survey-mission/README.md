# U24 step-through — the `:survey-mission` epistemic action, staged

claude (wm-edge worklist loop, any-lane), 2026-09-03. Row `:U24`, ported from
zaif-harness `:S6` half (b). Producer:
`holes/labs/wm-contract/u24_survey_mission.clj`, run from the futon2 root as

```
clojure -M holes/labs/wm-contract/u24_survey_mission.clj
```

No timestamp is written into any artifact, so "re-run it and diff" is a check
anyone can make — the standard `U4-AMBIGUITY-SWEEP.txt` / `ARMS.txt` set. Two
consecutive runs into different directories are byte-identical.

Read-only: no tick, no trace append, no run lock, nothing written under `data/`.
The only substrate traffic is the read of the `code/v05/mission-doc` family the
judge already performs, and its census is in `02-phase-readings.edn`.

## The field

`data/wm-trace/wm-trace-2026-09-02.edn` line 2, run
`4abad68c-5481-4402-8f0e-252add62c54b`, sha256 `afe6e76e9f24…` — the tick whose
`:decision` selected `M-zaif-harness-v1`. Its 146 `:ranked-actions` supply the
candidates and its own `:observation` and `:mu-pre` supply the state.

Two controls establish that the staged field IS the recorded field rather than a
lookalike, and both are exact:

| control | what it forecloses | result |
|---|---|---|
| **A** field reproduction | scoring a field the machine never scored | 146/146 candidates matched; max \|ΔG-risk\|, \|ΔG-ambiguity\|, \|Δcontroller-score\| all **0.000e+00** |
| **B** decision reproduction | claiming a selection the selection law would not make | `policy/select-action` on the staged ranking returns the record's own `:decision` — `:advance-mission M-zaif-harness-v1`, rank 1 |
| **C** default off | a term that moves numbers before anyone declared it | at `efe/default-survey-eig-weight` (0.0), **0 of 278** candidates carry a `:survey-eig` augmentation key and max \|Δcontroller-score\| against unenriched candidates is **0.000e+00** |

Control B's replay does not apply the record's own further
`:selection-boundary :reason-bearing-strategic-policy` filter. What is claimed is
that the two agree on this tick, not that the boundaries are the same object —
the same care C487 §7 took at its grain B.

## Three arms

| arm | candidates | `survey-eig-weight` | decision |
|---|---|---|---|
| recorded | 146 | — | `:advance-mission M-zaif-harness-v1` (5.418222) |
| survey-dark | 278 | 0.0 | `:advance-mission M-zaif-harness-v1` (5.418222) |
| survey-live | 278 | 0.02 | **`:survey-mission M-apm-capability-ratchet`** (5.302387) |

278, not 279: the bootstrap proposer's `:learn-action-class :survey-mission`
candidate is removed, because once `can-propose? :survey-mission` is true it is
no longer emitted (`action_proposer.clj:34-46`). Leaving it in would put a
candidate saying "survey is unavailable" beside 133 survey candidates.

**The break point is solved, not searched.** The field head is at 5.418222 and
the best survey candidate at 5.454879 with 7.6246 nats, so a survey candidate
takes rank 1 for any weight above **w_break = 0.004808**. The declared 0.02 is a
round number above that and above the planted demonstration's 0.018172; both are
reported so the weight's effect is legible rather than asserted.

## What the epistemic term measures (`03-survey-readings.edn`)

    survey-eig(M) = survey-availability(phase(M)) * SUM_q EIG(q)   [nats]

Over the 133 candidates: **123 `:measured`, 10 `:phase-unreadable`**. The nats
histogram:

| nats | candidates | what they are |
|---|---|---|
| 0.0 | 52 | availability 0.0 (verify/instantiate/complete) or an unreadable phase |
| 0.3466 | 7 | argue/document, 5 open carriers × ln 2 × 0.1 |
| 0.6931 | 14 | derive, × 0.2 |
| 1.0397 | 9 | head, × 0.3 |
| 2.0794 | 35 | **identify**, × 0.6 |
| 3.4657 | 15 | **map**, × 1.0, catalog gap only |
| 7.6246 | 1 | **M-apm-capability-ratchet**: map, 5 catalog carriers + 6 listed MAP questions |

**The MAP-question carrier exists in 1 of 133 docs.** C492 §4c named the numbered
list under a standalone `MAP must answer:` line as the obvious next question
kind; measured, exactly one primary mission doc carries it
(`futon0/holes/missions/M-apm-capability-ratchet.md:260`, six questions). Four
other docs contain the string `must answer` in constructions that are not a MAP
question list; they are recorded in `:near-miss-phrasings` with pointers and
deliberately not matched. Widening the regex to collect them is the move U22's
first run had to undo when 266 unresolvable cross-references bought survey
priority.

**Nothing joins a finding to the question it answers**, so every listed question
is open with basis `:no-per-question-answer-carrier` — an absence with a doc-line
pointer, not an assumption.

**The kin-catalog half is U23's eleven-carrier record, partitioned.** Six
carriers cannot be answered for any mission on this field (U23's own absence
reasons: `:no-typed-carrier` ×2, `:records-exist-not-keyed-by-mission` ×2,
`:subject-absent-from-carrier-key-space`, `:writer-exists-no-records`); they are
excluded and counted rather than paid ln 2 each. The five that remain are
mission-keyed and populated corpus-wide. A carrier the catalog already read is
settled and worth 0, so the three missions U23 surveyed score 0 on this half —
including M-zaif-harness-v1, which is the least informative survey target on the
catalog half precisely because it has already been surveyed.

## The identity the design rests on (`04-arms.edn`)

Over the 133 survey candidates: **1 distinct `:G-core`, 7 distinct
`:survey-eig-nats`.** `predict-effects :survey-mission` delegates to `:survey`,
which ignores `:target`, so G-core carries no target information at all and every
per-target difference between two survey candidates is the epistemic leg. That is
what "G dominated by information gain" means here, exactly: not a claim about
magnitude — at 0.02 the leg is 0.15 of a score of 5.30 — but about where the
variation lives.

A corollary worth stating because it cuts the other way: no weight can reorder
two survey candidates. Their order is the nats order at every value of
`survey-eig-weight`. The weight decides only whether surveying beats advancing.

## The U4 re-run (`05-ambiguity-rerun.edn`)

U4 grain A: arg-best over `G-risk + G-ambiguity` against arg-best over `G-risk`
alone. C487's C4 positive control (drop **risk** instead) is run on both arms and
fires on both, so the detector is not reporting zeros because it is broken.

| | n | grain-A changed? | λ_break | overtaker at λ_break | positive control fires? |
|---|---|---|---|---|---|
| before (recorded 146) | 146 | **no** | **1.024081** | `:no-op` | yes |
| after (+133 survey) | 278 | **no** | **1.004631** | `:survey-mission M-zaif-harness-v1` | yes |

**The answer is no, and the number says how close it came.** Ambiguity does not
overturn the EFE-best candidate on either arm. What adding the survey family
changes is how nearly it does: from 2.41% of its own size short of flipping the
winner to **0.46% short**, and the candidate that would overtake changes from
`:no-op` to a survey candidate. The 1.024081 measured here is the 09-02 field's
version of the 1.0238 C487 measured on 2026-09-01/S2/S4.

**The value set of the ambiguity term is unchanged.** Five distinct values before
and after (four among entropy-carrying candidates), spread 125.4477 (16.3208
entropy-carrying). Survey candidates land at **−116.79209997800035**, which is
the level `:fire-pattern` already occupied — C487's worked instance. So the
family adds 133 candidates at an existing level rather than a new level; what
moves λ_break is the mass at the low level, not a new discrimination.

## The demonstrations (`06-planted-identify-demo.edn`)

**D0, a control the row did not go looking for and the sharpest thing in it.**
All 36 real IDENTIFY-phase candidates on the field: the survey option outranks
its own recorded advance option **with the epistemic leg off**, 36 of 36. That
ordering is bought by the forward model and not by information gain —
`predict-effects :survey` declares a variance on `:mission-health` and none on
the other thirteen channels, so those thirteen take the 1e-9 floor and its
ambiguity term is **7.915207 nats** better than `:advance-mission`'s, the same
one-channel figure C487 §5 measured on the `:fire-pattern` / `:advance-mission`
pair. Under `G = risk + ambiguity` with lower preferred, an action model that
declares it knows what will happen is rewarded for saying so.

**D1, the acceptance's planted demonstration**, built to be the harder case
because of D0. `M-planted-identify-demo`, phase `identify` (availability 0.6),
three planted MAP questions and no catalog record: 8 open questions, 3.3271 nats.
Its rival is an advance candidate with `:open-hole-count 1` — the value the
enumerator gives a live doc with one unchecked item, and the value the recorded
field's own head carries — which puts the advance **below** the survey candidate
at G-core (5.394419 against 5.454879), so the epistemic leg is the only thing
that can move it. `w_break = 0.018172`; at the declared 0.02, `select-action`
returns `:survey-mission M-planted-identify-demo`.

**D2, the same two candidates on the full field**, reported and not smoothed: the
planted survey lands at rank 17 of 280 and the field's decision stays
`:survey-mission M-apm-capability-ratchet`. That is the term working — the real
MAP-phase mission with six listed questions carries 7.6246 nats against the
plant's 3.3271.

## Discharge (`07-discharge-shape.edn`)

The row's statement says the discharge of a survey flight is "MAP answers as
typed observations at mission grain". That shape is written as data in
`survey-mission-value/discharge-shape`, `:status :declared-not-implemented`, with
the reason it cannot be emitted: U23 measured the flight-discharge carrier as
`:writer-exists-no-records` — the writer is reachable
(`futon3c/src/futon3c/aif/flight_record.clj:347`, called at
`war_machine_pilot.clj:585`) and there are zero `*.flight.edn` files anywhere
under `~/code`. **No flight record is written by this row and none exists to
write against.**

## Files

| file | what it holds |
|---|---|
| `01-field.edn` | the pinned record, its sha256, the arena opts read off it |
| `02-phase-readings.edn` | the substrate census and the phase distribution, incl. the 36 identify-phase candidates |
| `03-survey-readings.edn` | the carrier partition, the MAP-question carrier census, per-mission nats |
| `04-arms.edn` | controls A/B/C, the break point, the three arms, the target-invariance identity |
| `05-ambiguity-rerun.edn` | the U4 re-run, both arms, with C487's positive control |
| `06-planted-identify-demo.edn` | D0, D1, D2 |
| `07-discharge-shape.edn` | the declared discharge shape and what blocks it |
