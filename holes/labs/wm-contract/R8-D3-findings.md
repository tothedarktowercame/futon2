# R8-D3 findings — generated Lean fixture

Date: 2026-08-30. Predecessor: R8-D2 at `be3a77d`.

## Generator and literal

- **Observed.** `checks/r8_f_contract.clj:17` selects only regular files named
  `wm-trace-*.edn`; `checks/r8_f_contract.clj:24` reads every top-level form
  with repeated `clojure.edn/read`, an EOF sentinel, and a tagged-literal
  default. It emits the sibling Lean fixture from the same parsed records
  (`checks/r8_f_contract.clj:360`).
- **Observed.** Each generated row contains only option presence for
  prediction errors, precision state, stored F, and selection gain; raw
  `:controller-score`/`:G-total` key-presence booleans; and file date
  (`checks/r8_f_contract.clj:275`). It contains no disposition, derived shape,
  boundary verdict, or violation count. `some 0 : Option ℝ` is explicitly a
  stored-field presence witness, not a transcription of F's value
  (`holes/labs/wm-contract/R8-D3-report.lean:7`).
- **Observed.** The generated header records 53 files, 792 forms, algorithm
  `sha256-over-newline-joined-sorted-form-sha256`, and digest
  `c9add16ac96c973ba4fd9a0c61f3b7319780c304424e2d14ea7b477309947880`
  (`holes/labs/wm-contract/R8-D3-report.lean:3`).

## Computed laws

- **Observed.** The generated literal has 792 entries. Lean's own closed
  `r8Census` reduces its census to `(755, 32, 5)` by `native_decide`
  (`holes/labs/wm-contract/R8-D3-report.lean:5561`). This is the stated fixture
  theorem corresponding to `r8CensusWmTrace`.
- **Observed.** The generated era theorem reduces all three biconditionals by
  `native_decide` (`holes/labs/wm-contract/R8-D3-report.lean:5565`). The report
  gives conjunct violation counts `0 / 0 / 0`; conjuncts 1–2 are the one-write-
  site identity at `scripts/futon2/report/war_machine.clj:4664`,
  `scripts/futon2/report/war_machine.clj:4665`, and
  `scripts/futon2/report/war_machine.clj:4687`; conjunct 3 is the contingent
  non-interleaving observation (`holes/labs/wm-contract/R8-D3-report.edn:1`).
- **Observed.** Raw shape facts classify as gMap 760, controllerMap 32,
  unknown 0, both-keys 0, neither-key 0. The date margin is latest pre-boundary
  `20260709` to earliest post-boundary `20260714`
  (`holes/labs/wm-contract/R8-D3-report.edn:1`). The instrument searched all
  792 generated key-presence pairs; its negative result is limited to this
  content-pinned corpus.

## Compilation and interface status

- **Observed.** The canonical `DarkTower.WarMachine.Holes` module does not
  currently build for reasons after the R8 declarations: untyped binder `s`
  at `/home/joe/code/mathlib4/DarkTower/WarMachine/Holes.lean:445`, unknown
  `NNReal` at `/home/joe/code/mathlib4/DarkTower/WarMachine/Holes.lean:461`,
  and a namespace-close error at
  `/home/joe/code/mathlib4/DarkTower/WarMachine/Holes.lean:609`. Therefore its `.olean` is unavailable
  and the generated file's normal import cannot yet be checked end-to-end.
- **Observed.** To distinguish that module failure from this fixture, I piped
  the exact committed R8 declaration block
  (`/home/joe/code/mathlib4/DarkTower/WarMachine/Holes.lean:377` through
  `:421`) followed by the generated fixture into `lake env lean /dev/stdin`.
  It exited 0, proving both generated theorems elaborate and reduce against
  the actual R8 interface. This is validation evidence, not a replacement for
  the blocked canonical import.
- **Observed.** `git -C /home/joe/code/mathlib4 status --short` reports no
  changes. I did not edit `Holes.lean`.

## Attribution

The fixture establishes the boundary and non-interleaving only. The cause of
the 2026-07-14 change remains **inferred, untested**; no reset, recalibration,
or instrumentation-selection cause is asserted.
