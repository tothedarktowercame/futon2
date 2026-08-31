# C244 — independent review of the workflow report

Date: 2026-08-31

Reviewed artefacts:

- `holes/labs/wm-contract/workflow-report.edn` (`6224ef9`)
- `p4ng/empirics-futon/gen_workflow_report.bb` (`f08a6c9`)
- pinned case ledger `p4ng` `3635cc6`, SHA-256
  `546fbd90b6754035a51cab32181956389678184b947976ebb064b2966270f49e`
- pinned lane registry `futon2` `46b81eb`, SHA-256
  `8453c229b0f0534f905d980afad877141bece63e4b5dd616f4bbd1bc39e153fd`

## Verdict

**Do not publish the workflow table in its present form.** The generated
snapshot is reproducible under its parser, but the parser does not implement
the report's stated semantics. At the pinned source revisions it overstates
the evidence lane's completed count by three and calls a discontinuous
attribution population continuous.

This is an evidence-boundary defect, not a request to hand-correct the table.
The generator and its source contract must be repaired, then the snapshot and
paper view regenerated.

## 1. The latest-heading rule is not implemented

The generator says that the last occurrence of a case supersedes earlier
occurrences. In fact, it first discards every heading that does not match
`heading-re`, then applies last-wins only to the surviving subset. A later
un-attributed amendment therefore cannot supersede an earlier attributed
heading.

At pinned ledger `3635cc6`, an independent latest-heading recount gives these
completed counts:

| lane | generated | latest heading |
|---|---:|---:|
| wm-nouns | 6 | 6 |
| wm-verbs | 4 | 4 |
| wm-organization | 5 | 5 |
| wm-evidence | 7 | 4 |

With the pinned registry holdings added, the report should therefore not claim
`wm-evidence 8 dispatched / 7 completed`; under its stated rule the derived
figures are `5 / 4`. The three surplus completions are historical attributed
headings for C23, C32, and C33 whose later headings are outside the attribution
grammar.

## 2. Partial parse failure is silent

A fixture containing one valid heading, one heading without attribution, and
one combined `C3, C4` heading exited 0 and reported:

```
cases-in-ledger 3; cases-attributed 1; frontier 1
```

The two invisible cases did not make generation fail. Thus a new heading shape
can silently leave the frontier and lane counts stale. This is the stale-
baseline class at the parser boundary.

The real ledger already contains this shape. `## C232, C236 — ...
(wm-nouns)` is not parsed, so both bindings are invisible to attribution. The
claim `continuously from C209` is also false as computed: the algorithm permits
numeric gaps of up to four, and the real parsed population omits C232, C233,
and C236.

Decision parsing has the same partial-failure property: once one `### ` heading
exists, a differently shaped decision heading is silently ignored. Moreover,
the parser counts every level-three heading, not a machine-delimited decision
population.

## 3. Loud-failure controls

Independent temporary-fixture runs produced:

| mutation | exit | result |
|---|---:|---|
| invalid EDN lane registry | 1 | `ERROR unreadable lane registry` |
| case ledger with zero parseable attributed headings | 1 | `ERROR case ledger yielded no parseable headings` |
| decisions file with zero parseable `### ` headings | 1 | `ERROR decision ledger yielded no parseable items` |
| partially malformed case ledger with one surviving heading | 0 | malformed/invisible headings ignored |

The generator therefore fails loudly on total unreadability, but not on the
more likely failure: a source grows one heading the grammar cannot see.

## 4. Claim scope

`:dispatched` is not a dispatch count. It is completed-attribution count plus
the current registry holding. The caveat correctly admits that there is no
dispatch log, but the table heading and caption still present the derived value
as cases dispatched. The evidence supports “latest lane-attributed completions
plus current holding,” not a historical dispatch population. A completed case
does imply that lane work occurred, but the source cannot establish its
dispatch event, retries, abandonment, or reassignment.

## Required acceptance before publication

1. Parse every case heading first, then attach optional attribution; apply
   last-wins over all headings, not only attributed ones.
2. Fail when a heading in the declared systematic window is not classifiable,
   including combined case identifiers, or explicitly type it as absent.
3. Compute continuity as exact coverage, not gaps of at most four.
4. Give decisions a machine-delimited current population and fail on unmatched
   decision-like headings.
5. Rename `dispatched` to its actual derived meaning, or add an authoritative
   dispatch log before retaining that label.
6. Add negative controls for one invisible case heading and one invisible
   decision heading in otherwise valid sources.

Until those conditions hold, the artefact is suitable as a diagnostic draft,
not as a reader-facing build-state table.
