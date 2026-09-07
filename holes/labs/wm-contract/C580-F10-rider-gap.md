# C580 — F10 live-run rider gap discovery

## 1. What the rider asks

The ruling requires “AT LEAST ONE NEW END-TO-END RUN of the relevant
subcomponents” that “computes the seeded C live” and demonstrates performance
and conformance (`futon2:holes/labs/wm-contract/aif-equations.edn:212`).  Its
dated correction fixes the organisations carrier at the twelve gated
`outcome-kinds` and seven named zeros
(`futon2:holes/labs/wm-contract/aif-equations.edn:214`).  In code, computing the
seed live therefore requires a production path to evaluate the declared
twelve-wide `seeded-c` (`futon2:src/futon2/aif/ruled_outcome_c.clj:43-50`) as
part of a run, not a lab checker loading that declaration after the run.

## 2. Gate A — nothing consumes the declaration

The exact scan was:

```text
rg -n 'ruled[-_]outcome[-_]c' src scripts test checks holes/labs/wm-contract
```

The deterministic result is projected at
`futon2:holes/labs/wm-contract/runs/F10-outcome-domain/04-rider-gap.edn:1`.
After excluding the declaration's own namespace, the hits classify as one
test, one check, ten lab files, and three artifacts; production callers: zero.
The sole `src/` hit is the declaration itself at
`futon2:src/futon2/aif/ruled_outcome_c.clj:1`, not a consumer.  Thus a cohort
restart alone would emit dispositions that no production code currently scores
against this declaration.

## 3. Gate B — no new terminal dispositions

The newest cohort directory is `wm-outer-loop-46-v1/attempt-061`, mtime
2026-07-27T10:59:53.607Z (`04-rider-gap.edn:1`).  Its closed event identifies
the attempt and terminal outcome `:grounded-change` (run record
`attempt-061`).  Contrary to C579's statement that its identity was not found
in committed source (`futon2:holes/labs/wm-contract/C579-F10-live-run.md:114-119`),
the scan finds committed mentions, including the generated cohort ledger at
`futon2:holes/labs/M-aif-full-loop-46/ledger.edn:76`.

The command wrapper exposes `activate`, `start`, checkpoint, and `close`
(`futon2:scripts/wm_full_loop_cohort.clj:7-12`) and dispatches them to the cohort
API (`futon2:scripts/wm_full_loop_cohort.clj:22-29`).  Starting validates the
preregistration and grounded time-step, allocates the next attempt directory,
and writes `001-time-step.edn`
(`futon2:src/futon2/aif/full_loop_cohort.clj:212-275`).  Closing requires the
preregistered checkpoints and appends a grounded closed event
(`futon2:src/futon2/aif/full_loop_cohort.clj:327-345`).  The runner constructs
the terminal outcome term and closes the cohort attempt at
`futon2:src/futon2/aif/full_loop_runner.clj:2395-2419`.  Restarting this cohort
is Joe's call; this discovery neither runs nor advocates the restart.

## 4. Gate C — routes needing neither gate

1. Folding `:ruled-outcome-c` into the EFE stack would make the seed a live
   preference layer, but contradicts its present `:folded? false` declaration
   (`futon2:src/futon2/aif/ruled_outcome_c.clj:56-62`).  Doing so without a
   ruling silently decides that this C belongs in the EFE fold and at what
   grain.
2. A read-only recorder at attempt close could compute `C(disposition)` without
   entering the EFE fold.  It would make a terminal record carry a seed lookup,
   but silently decide that C is computed at close rather than during policy
   scoring.  The current close boundary is
   `futon2:src/futon2/aif/full_loop_runner.clj:2395-2419`.
3. Building `Q(o|π)` over dispositions would provide the canonical object to
   compare with C, but F1 records that its machine-grain rows are constant in π
   because the available transition is not action-indexed
   (`futon2:holes/labs/wm-contract/worklist.edn:1262-1263`).  F1 is a completed
   row; its remaining limitation is not a Joe gate recorded there, but a
   separate missing controlled-transition/Q binding.

Manufacturing a terminal outcome inside the preregistered cohort merely to
satisfy a checker would contaminate the preregistered evidence.  The cohort
explicitly refuses an unlisted trigger rather than admitting it
(`futon2:src/futon2/aif/full_loop_cohort.clj:232-247`), so this is not a
candidate.

## 5. What Joe is actually being asked

1. Decide whether to restart the dormant cohort. This clears Gate B only; it
   does not create a production consumer of the seed.
2. Decide where the seed is computed: in the EFE fold or at terminal close.
   This clears Gate A for the selected boundary; it does not by itself create a
   fresh disposition run.
3. Decide whether the live-run rider requires canonical `Q(o|π)` scoring. This
   addresses Gate C but does not clear Gate A or Gate B by itself.

## 6. Recommendation — not a ruling

Recommendation, not a ruling: choose the computation boundary before deciding
whether to restart the cohort.  Otherwise a new cohort run cannot demonstrate
that the machine computed the seed and would need to be repeated after wiring.

## 7. Pointer drift found and not repaired

C579 says the `attempt-061` run identity is not found in committed source at
`futon2:holes/labs/wm-contract/C579-F10-live-run.md:117-119`; that is false at
the current commit because the committed cohort ledger names it at
`futon2:holes/labs/M-aif-full-loop-46/ledger.edn:76`, and C579 itself now names
it.  This slice records the drift and does not edit C579.  No other starting
pointer was copied without reopening it.

## 8. Scope limits

No production source, scorer, declaration, ruling, ledger, or cohort data was
changed.  The production-caller plant runs against a copied scan root, and the
newer-attempt plant runs against a copied data root
(`futon2:holes/labs/wm-contract/f10_rider_gap_controls.sh:14-42`).  The null
control proves the unchanged route is accepted byte-identically.  This is a
discovery record, not discharge of the rider.
