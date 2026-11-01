# Major incident, outer selection: question 4, reach (claude-2, 2026-11-01)

Question (Joe's framing): every inner run is downstream of an outer choice, so every
cascade constructed, inner G computed, and grid or tracker row evidenced since the outer
path first chose a target rests on a choice never held to the criteria. Date the first such
choice and list what rests on the runs after it. Companion to the Q1-Q2 and Q3 records in
this directory. All counts below were read from `data/wm-runs`, `data/wm-learning`,
`data/wm-pattern-graph-diffs`, `data/wm-executable-cascades`, `data/wm-pattern-feedback`,
the deficits tracker, the P5 rollup, the target repositories' git logs, and mathlib4.

## Eras of target selection, from the 245 run records on disk

| era | dates | mechanism | runs | notes |
|---|---|---|---|---|
| 0 | 2026-09-18 to 10-27 | deleted selector (tactical families scored for every target) or caller-supplied task | ~200 | condemned by Joe's 10-28 ruling; not re-examined here |
| 1 | 2026-10-28 21:43Z to 21:46Z, and 10-29/10-31 | fixture runs, target `M-current`, placeholder patterns, no outer section | 8 + 4 + 6 = 18 | written into the production run directory; not in the manifest |
| 2 | **2026-10-29 00:15Z** to 11-01 03:48Z | R20 outer path (`:outer/selection` present) | **28** | the subject of this incident |

The first R20 outer choice is run f74df9cc at 2026-10-29 00:15Z (dispatch-failed), then
22539d3d at 02:16Z (abstained) and 9e2103c1 at 03:21Z, the first that reached a target.
Not 10-30: the outer-state click count starts at 10-30 only because every 10-29 and the
first 10-30 selection recorded `:outer/learning :status :deferred`.

**No run record in the directory has a conforming requirements projection.** All 73
projections say `:conforms false`. "Passing" was never Lean conformance; it was grid and
tracker rows.

## The 28 era-2 selections

Same shape throughout as in the Q1-Q2 record. Cascade index 0 in all 28. On 10-29, 13 of
15 selections had an `:associated` relation (M-futonzero-prelim-practice x6,
M-reachable-from-boot x4, M-repl-wins-over-cli x2, M-pattern-mining x1): a two-way tie of
cascades 0 and 1 against a worse no-pick cascade. The remaining 2 on 10-29, both on
10-30, and 6 of 11 on 10-31/11-01 had an unknown relation: three-way tie, chosen by menu
order, predicted outcome equal to abstention. Totals over 28: 18 two-way, 10 three-way.
`q` is 0.75/0.25 in all 28.

## What rests on the 28 runs

### Commits in other repositories (7)

Seven era-2 runs ended grounded-progress and committed, signed `wm-author`:

| run | target | repo | sha | changed |
|---|---|---|---|---|
| 376d76ff (10-29) | M-reachable-from-boot | futon3c | 48b920a4 | mission file: clears stop-the-line, closes the mission on issue #65 |
| ff4b4d5c (10-29) | M-peradam-grounding | futon7 | 8b46822b | mission file + `holes/peradam_cert.bb` |
| 3415b671 (10-30) | M-peradam-grounding | futon7 | de7bf835 | mission file + `peradam_cert.bb` ("close anti-laundering criterion 3") |
| 0be4fc7d (10-31) | M-peradam-grounding | futon7 | aa90d97c | `peradam_cert.bb` |
| d5b9b513 (10-31) | M-pattern-retrieval-calibration | futon3 | 263dfc26 | new `src/futon3/pattern_candidates.clj` + test (271 lines) |
| ebbbe42a (10-31) | M-typed-memories | futon3c | 4fd00fa6 | mission checkbox ticked + new supersession test (184 lines) |
| c5590f95 (11-01) | M-pattern-retrieval-calibration | futon3 | a12870e8 | new `src/futon3/pattern_retrieval_calibration.clj` + test (213 lines) |

Two of these edit mission files to mark wants done or missions closed. Two add source to
futon3, the pattern library's repository, under a target selected by menu order. The
futon7 target M-peradam-grounding was selected three times with an unknown relation.

### Inner G and constructed cascades

Inner G values recorded at the terminal receipt for the seven grounded runs:
1.4034, 1.4713, 2.2930, 1.6825, 1.6996, 1.7347, 1.6996. Each is G over policies

> Note, 2026-11-01 12:40Z (from L5-1, claude-3): these are the G values of the per-policy argmax that `wm/terminal_receipt.clj:64` reports as "selected"; in the multi-policy runs the enacted policy was chosen by the first-action marginal or by list order and may be a different policy with a higher G. Read them as the posterior mode's G, not the enacted cascade's.
constructed for a target the outer path chose. 22 era-2 projections report Q3 holds with
1 to 5 constructed cascades per run. The executable-cascade store holds 64 cascades; 10
are referenced by era-2 run records, and those 10 are exactly the store files modified
since 10-29.

### Learning stores

- `run-record-manifest.edn`: 111 entries, 27 from era 2 (24%). This manifest is the
  pinned source of `meta-outcome-dataset`, read by `outer_learning_manifest.clj`.
- `wm-pattern-feedback/events.edn`: 16 events in total, 7 written by era-2 runs, read by
  `cascade_feedback.clj` and feeding habit reinforcement. One of the 7 (line 21) records a
  provisional mid-run cascade revision.
- `outer-state.edn`: 12 consumptions, all era 2, discussed in the Q1-Q2 record.
- `wm-observation-labels/labels.edn`: 1 label since 10-29.
- Pattern graph: 16 era-2 diff files; every one has `nothing_to_add`, zero uses and zero
  edges, against an unchanged base (mined-pattern-graph.json, last modified 10-01). The
  shared pattern graph was not altered by era-2 runs.

### Grid, rollup and tracker

- GRID v2 / ROLLUP-P5: all 20 rows' evidence runs are era-2 runs (0ba3370c through
  f4b95abf). No row has evidence from anywhere else.
- Deficits tracker: 24 rows closed. 17 cite an era-2 ordinary run id in their evidence:
  B1-TICKETS, B4-OUTER-PREDICTION, E2-OUTER-LEARNING, E3-OUTER-REPEAT,
  B5-INJURIES-HOLD-RUN001, B1-READINESS-PROMPT-LIMIT, C3-B2-ANSWER-DELIVERY,
  C3-AUTHOR-REPOSITORY-HISTORY, D1-SELECTED-WANT-REVISION-EVIDENCE,
  B2-SELECTED-TARGET-READING-RESPONSE, ALREADY-SATISFIED-WANTS-UNMEASURED,
  TEST-WRITES-CANONICAL-RUN-STREAM, Q9-COMPLETION-PAIRS-ABSENT,
  DISCHARGED-INJURY-TICKETS-LEFT-OPEN, DUPLICATE-TARGET-STILL-SELECTED,
  CLAIM-MATCHES-FALSE-UNCONSUMED, A2-ENVELOPE-RECEIPT-ABSENT. The other 7 cite medic or
  repair clicks, commits, or no run.
- 30 reconciliation documents (26 dated 10-31, 4 dated 11-01) and
  REPORT-wm-recovery-external-review.md were written from era-2 runs.
- 57 distinct click ids were issued from 10-29 to 11-01.

### Lean

- Written during era 2 from era-2 behaviour (11 commits, 10-29 and 10-30, 9 modules):
  ReadingRefusalReselection ("click-local Q6 exclusion before R20 reselection"),
  AlreadySatisfiedReview (pins run e5f08d91, era 2), OrdinaryAuthorRefusalCorrection,
  SelectedWantCarrier, ScopedAcceptance, HistoricalMisclassificationCorrection,
  OutOfBandFix, VerifiedAnswerDelivery. These model behaviours observed on runs whose
  targets the outer path chose; ReadingRefusalReselection models the R20 pick itself.
- Pinned to era-0 selector runs (7 modules): R20IndependenceRowCorrespondence,
  CloseOutcomeImage, ReadingEraClickPrediction, AlreadySatisfiedArtifactRefSeam,
  R20CostRowCorrespondence, HistoricalMisclassificationCorrection, SelfHealRoute. These
  are witness-mode defect clauses over records produced under the selector Joe condemned.

### Paper

`p4ng/plop-2026-alignment.json` is the only p4ng file citing an era-2 date or run id. No
other paper source does.

## What is not in reach

The pattern graph (unchanged). Medic and repair clicks (not ordinary selections). The
Lean requirement set itself, which never read the outer path and so neither certified nor
rejected it. The zai-3 Lean branch, unmerged.

## Summary line

28 selections, 7 commits in three repositories including two mission closures, 10 stored
cascades, 27 of 111 learning-manifest entries, 7 of 16 feedback events, all 20 grid rows,
17 of 24 closed tracker rows, 30 reconciliation documents, and 9 Lean modules written
from the behaviour, rest on outer choices made by menu order or by a two-way tie against
abstention, with the outer belief never updated from its starter kit.
