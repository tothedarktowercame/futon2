# C590 — F12 snatch reproducibility warm-up

This slice performs the first step of Joe's 2026-09-08 F12 sequence: make
snatch reproduce the ruled organised edge set before looking for a new
naturalistic exemplar. The ruling and sequence are recorded at
`holes/labs/wm-contract/RULINGS-walkthrough-2026-09-08.md:66-83`.

The single producer correction is at
`futon3:checks/find_organise.clj:330-344`: organised edges are now
fast-forwarded over `nodes` minus `added-by-organise`. The executable O3
predicate reads the same carrier at `futon3:checks/find_organise.clj:528-534`.
This is the already-ruled no-bootstrap clause at
`mathlib4:DarkTower/WarMachine/F12RuledCarrier.lean:33-35`, not a new ruling.

The committed snatch fixture's `[:g4 :snatcher]` row is reconstructed in
`futon3:test/futon3/find_organise_test.clj:25-33`. It now produces exactly
one edge and passes the ruled O3 predicate. A separate control rejects the
former bootstrap edge set at `futon3:test/futon3/find_organise_test.clj:18-23`.
The run receipt is
`holes/labs/wm-contract/runs/F12-organise/31-snatch-reproducibility.edn:1-17`.

No registry, decision, Lean source, or machine runtime was changed. The
naturalistic exemplar remains the next F12 slice.
