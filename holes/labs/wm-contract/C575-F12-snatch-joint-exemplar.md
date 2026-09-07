# C575 — F12 slice 8: the snatch CascadeDiff rows carry edges and a moved precedence together

## What this corrects

Slice 5 asked whether any recorded cascade carries a real cascade edge and an
O4 before/after precedence pair over the same members, and its artifact
answered `false` over ten record files
(`futon2:holes/labs/wm-contract/runs/F12-organise/20-o4-edges-census.edn:238-249`
for snatch, scope at
`futon2:holes/labs/wm-contract/f12_o4_edges_census.bb:8-22`). **That scoped
claim stands and is not reopened here**: no *record file* carries both keys.
What this row then wrote from it — that no single run exercises O2, O3 and O4
together, so what comes next is a choice rather than a measurement — does not
follow, and is false at the corpus grain.

## The rows are computed, not recorded

`futon3:checks/find_organise.clj:603` `cascade-diff-table` builds one
`CascadeDiff` per treatment/disposition out of the twelve scenarios of
`futon3:checks/snatch-cascade.edn`, taking the `:patterns` policy row as before
(`futon3:checks/find_organise.clj:616`) and the `:exchange-first` row as after
(`futon3:checks/find_organise.clj:617`). The cascade itself comes from
`organise` over the snatch repository
(`futon3:checks/find_organise.clj:619-622`), so its edges are fast-forward over
the authored `@why` relation, and the six O4 fields are attached at
`futon3:checks/find_organise.clj:628-633`. **Neither the edges nor the
before/after pair is a key of any record file**, which is exactly why a census
that reads record files reports their absence.

The movement was already in a committed artifact under a key that census did
not read: `futon3:checks/find-organise.edn:62-64` reports
`:laws [:O1 :O2 :O3 :O4]` with `:failures []` over `:diffs 6`,
`:o4-moved-the-score [[:g4 :snatcher]]`, and `:reproduces-record []`.

## Measured

At futon3 pin `cdb5e8a56fd907beb6a99f8b88af9de50ff93126`, all six rows carry
organised edges (3, 3, 2, 5, 10, 3) **and** a moved precedence
(`futon2:holes/labs/wm-contract/runs/F12-organise/23-snatch-joint.edn:162-169`,
headline at `:1`). All six also move the acting order, satisfy all four O-laws
through the namespace's own predicates
(`futon2:holes/labs/wm-contract/runs/F12-organise/23-snatch-joint.edn:51`,
`:69`, `:85`, `:107`, `:136`, `:154`), and reproduce their record through
`futon3:checks/find_organise.clj:635`. One row, `[:g4 :snatcher]`, also moves
the score: `3` at `futon3:checks/snatch-cascade.edn:84` to `-5` at
`futon3:checks/snatch-cascade.edn:211`
(`futon2:holes/labs/wm-contract/runs/F12-organise/23-snatch-joint.edn:170`).

So O2 and O3 have a true antecedent and O4's antecedent is true **on the same
row** — the conjunction slice 5 searched the records for.

Nothing here re-spells `organise`, `fast-forward` or the O-laws. The checker
loads `find-organise` in a subprocess and calls its own functions
(`futon2:holes/labs/wm-contract/f12_snatch_joint_check.bb:52-58`); every
boolean in a row is computed from those values at
`futon2:holes/labs/wm-contract/f12_snatch_joint_check.bb:67-88`.

## The qualification, stated rather than glossed

The after side is a **re-wiring of one recorded collection, not a second
construction**. `futon3:checks/find_organise.clj:619-622` organises over
`(:acting before)` alone, so a row's edges are the `:patterns` cascade's; the
`:exchange-first` row supplies only the after precedence, acting order and
score, and its own `:nodes` is a single node
(`futon3:checks/snatch-cascade.edn:210`). That is the `CascadeDiff` shape the
Lean laws are stated of, so it is a joint instance of them. Whether it is
**also** Joe's item-4 exemplar — a real recorded run whose precedence moves and
carries a score, `futon2:holes/labs/wm-contract/RULINGS-walkthrough-2026-09-07.md:214-223` — is his and is
not decided here. What the earlier reading offered him as a choice between
authoring a second rule carrier and keeping the O4 witness split now has a
third state on the table, and it needs no authoring.

## Controls

Five plants, each perturbing an **input** and re-running the pipeline, none of
them writing the verdict's own field
(`futon2:holes/labs/wm-contract/f12_snatch_joint_check.bb:165-202`, results at
`futon2:holes/labs/wm-contract/runs/F12-organise/23-snatch-joint.edn:14-38`):
the fixture's `[:g4 :snatcher]` after-precedence flattened onto its before, both
policy rows of `[:g1 :cautious]` dropped from the fixture, the first derived
row's edge set emptied before the booleans are computed, the census row's
`:carries-both?` flipped to true, and the census pointer repointed. Each lands
and each turns the verdict false.

Two further plants were run in review and are **not** in the checker's list:
emptying `[:g1 :cautious]`'s `:acting` in the fixture drops that row's edges and
fails the check (exit 1, `both=5`), while changing an unrelated `:rounds` field
leaves the verdict true (exit 0) — so the check discriminates rather than
refusing everything.
