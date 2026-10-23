# R20 independence-row live projection

`run90ca-projection.edn` projects only `:participants` and the emitted
independence row from
`data/wm-runs/tick-run-record-2026-10-23-90ca5944-3d52-4bd6-940c-fe2f6c2c8c6b.edn`
at futon2 `41e719d97e42a920b9d5fd91f913da83c964e9c4`.

The source file SHA-256 is
`976d1bfc7983b3cc234dbc5a25ae848e0c73b018fa2076940b31bbfc5dcdc5b9`.
Values are copied verbatim. In particular, the fixture retains each role
receipt's `:status` and complete `:identity` string. It does not relabel the
identities as “author” and “reviewer”, authenticate them, or claim their
producers are independent.

The corresponding formal contract is
`DarkTower.WarMachine.R20IndependenceRowCorrespondence` at the companion
mathlib4 commit recorded in the test receipt. Its encoding is local to this
supplied pair and preserves equality: identical strings receive the same
`Fin 2` value; unequal strings receive different values.
