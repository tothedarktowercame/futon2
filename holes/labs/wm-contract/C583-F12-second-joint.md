# C583 — F12 slice 12: a second snatch joint through the ruled signature

The second joint answers the next-slice question from F12 without taking the
registered exemplar-proviso choice.  At futon3 pin
`f8dd164bdc7f20a160b5e6b758d71532be65ca1e`, the `[:g2 :snatcher]` row has
three selected nodes, three non-empty `addedByOrganise` nodes, and five
computed edges
(`futon2:holes/labs/wm-contract/runs/F12-organise/26-second-joint.edn:54-82`).
Every one of those five edges has at least one endpoint in
`addedByOrganise`, so the ruled no-bootstrap subtraction retains zero
(`futon2:holes/labs/wm-contract/runs/F12-organise/26-second-joint.edn:71-72`).

This makes the first exemplar's one-of-ten result particular to
`[:g4 :snatcher]`, not characteristic of a second recorded scenario: its one
survivor is still recorded at
`futon2:holes/labs/wm-contract/runs/F12-organise/26-second-joint.edn:29-32`.
The second row is nevertheless not the candidate sought by arm (c): its added
set is non-empty, but the ruled result does not reproduce its five computed
edges.  No claim is made about the four scenarios not driven through the Lean
signature in this slice.

The independent Lean statement uses the ruled signature directly.  The
carrier and its seven-clause conformance proof are at
`mathlib4:DarkTower/WarMachine/F12SecondSnatchExemplar.lean:17-38`; the five
recorded edges and the mechanically checked empty survivor list are at
`mathlib4:DarkTower/WarMachine/F12SecondSnatchExemplar.lean:40-59`; and edge
3→9 is refused by the carrier because its destination is added at
`mathlib4:DarkTower/WarMachine/F12SecondSnatchExemplar.lean:61-66`.

The checker derives both rows from `cascade-diff-table`, then computes the
ruled subtraction from each row's own `:added-by-organise`; it does not copy
the edge counts from the earlier artifact
(`futon2:holes/labs/wm-contract/f12_second_joint_check.bb:17-48`).  Three
plants independently erase the second row's non-empty-added condition, invent
a survivor, and remove the Lean declaration condition; each moves the verdict
(`futon2:holes/labs/wm-contract/runs/F12-organise/26-second-joint.edn:43-52`).

No ruling or registry entry changed.  The `:organise-exemplar-proviso` choice
therefore remains open; this slice supplies one additional measured row for
Joe's existing choice.
