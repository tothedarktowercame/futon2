# Finding: the relation producer covers 92 of 626 open tasks (claude-2, 2026-11-01 15:45Z)

Context. L5-4d-3's replay of the restored selection on the retained 11-01 field refused
`:tie-at-minimum` with all three cascades at G 4.7875, the abstention value: every pick's
relation read `:unknown`, so every pick landed on the stopped kernel. That replay called
`focus-receipt/classify-target` with four arguments. The producer derives an M- target's
relation (stated-relation hops, then the embedding neighbour) only when a relation context is
passed (`focus_receipt.clj`, `m-derivation`, "when a relation context is given (the scoring
path's)"); the live scoring path passes one (`outer_observation.clj:205-213`, `:relation-context`).
With the context, the two picks read `:associated`, derived through the embedding neighbour
`M-action-cost-modelling` (a codex-12 hand-reviewed row of 2026-09-21). The replay's refusal was
therefore an artefact of the call, not the field; the witness's relation classes stand and now
disclose the derivation.

Census, same producer, same context, focus retained "WM", as-of 2026-11-01T06:02Z, over the 626
open ids pinned in `witnesses/witnesses-open-tasks-2026-11-01.edn`:

| Prefix | Class | Source | Count |
|---|---|---|---|
| M- | focus | declared row | 5 |
| M- | associated | declared row | 3 |
| M- | associated | embedding neighbour | 84 |
| M- | unknown | `:embedding-node-not-retained` | 89 |
| E- | unknown | `:relation-not-declared` (no derivation for E-) | 255 |
| T- | unknown | `:no-parent-relation` | 190 |

So 92 tasks (14.7%) have a measured relation; 534 do not. Under the restored criteria a pick on
an unknown relation fails Q7 (relation absent on the path), lands on the stopped kernel and
fails Q11 (G equals abstention, prediction equals stopping). The field is the whole open set
(ruling 4), but only 92 of its members can be a certified pick until the relation producer
covers excursions, tickets and the 89 missions absent from the structure embedding. This is a
producer gap, not a reason to shrink the field; the declared-row corpus is 9 rows.

Open for Joe: whether an embedding-derived relation counts as measured (it is derived from
pinned embeddings and a hand-reviewed row, and the receipt records `:derived-via`), and how
relations for E- and T- targets are to be produced (ticket parent exists for T- but found no
parent relation for any of the 190; nothing derives for E-).
