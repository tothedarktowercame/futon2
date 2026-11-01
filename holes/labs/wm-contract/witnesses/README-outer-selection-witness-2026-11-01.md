# Outer selection witness (claude-2, by hand, 2026-11-01)

Joe, 2026-11-01: "construct (by hand) example witnesses, so future clojure implementation is
not guessing about the conformant shape or the possibility of creating it. I will expect to
see real cascades, proof of due consideration of open tasks, etc."

Files: `outer-selection-conformant-2026-11-01.edn` (the witness),
`build-outer-selection-witness.bb` (rebuilds it; scoring replicates `outer_score.clj`),
`witnesses-open-tasks-2026-11-01.edn` (the 626 open task ids, pinned by sha).

## What is real in it

- **Field.** The 626 open tasks the registry enumerated for run ac5bc477 (181 missions,
  255 excursions, 190 tickets) and the Giraffe pass 730407c5 of 2026-11-01 (609 items:
  500 ready, 57 not ready, 52 undetermined). The 17 open tasks not in the pass are listed
  with the pass's own reason (`unread-new` drift). Every open task is accounted for.
- **Cascades.** Three of the six evidenced selection cascades in
  `futon3/library/task-selection/README.md` (rows 1, 3, 5), each as a cascade of library
  patterns cited by path and sha, at library commit f3b8b32. Nothing hand-listed; the menu
  in `outer-makeup.edn` is not used.
- **Application.** Each cascade applied to the field as its patterns are written. Giraffe
  order picks M-pattern-retrieval-calibration (rank 1, ready, 2 unblocks). The repair click
  finds no open repair obligation on the path of the next candidate and passes the field
  through, so it picks the same target. Uncertain design gates to the 52 undetermined items
  and picks a bounded discovery task: resolve M-interim-director's out-of-scope
  prerequisite in futon4.
- **Relations.** From the real producer, `focus-receipt/classify-target`, run against the
  retained focus WM at 06:02Z: both picks are `:associated`. E-comprehension-foundation,
  the façade's click-12 target, is `:unknown` (relation-not-declared).
- **Reach.** Observed, not defaulted: 2 of 4 prior clicks on M-pattern-retrieval-calibration
  grounded; dependency readings complete for 609 of 615 members.

## What it shows

| cascade | pick | G | note |
|---|---|---|---|
| uncertain-design | discovery on M-interim-director | 2.9949 | strict minimum |
| giraffe-order | M-pattern-retrieval-calibration | 3.4841 | ties with repair-click |
| repair-click | M-pattern-retrieval-calibration | 3.4841 | same pick, disclosed tie |
| abstention | none | 4.7875 | every cascade must beat this |
| façade (click 12 as recorded) | E-comprehension-foundation | 4.7875 | equals abstention |

A conforming selection run can be built from today's data, with a strict minimum, a
reason stated against the minimising cascade, and ties disclosed rather than broken by
position. The façade's score equals abstention to the last digit, which is what the
restored requirements must refuse.

## What it does not show

- **Q9.** The preference C is the makeup kit's product of ending and gain marginals: a
  flat table with no authorized factorization certificate. The witness says so in
  `:lean-shape`. Meeting Q9 for the selection run needs the factorized preference
  (completion first, relevance conditional) with its authorization records, which do not
  exist for selection yet.
- **Learning.** State counts are the starter kit because no classified ending has ever
  updated them. The witness uses them as a prior and says where they came from.
- **The gain of a discovery task.** Scored `:none`; the information it yields is carried by
  the novelty term, not by items made ready. This is a modelling decision to review.

## Lean counterpart

The `:lean-shape` section lists the `RunFacts` field values this witness supplies. Once
packet L1 lands, `selectionGood` in Requirements.lean should be re-stated from these values
(ids as digests) so the Lean witness and this record are the same run.
