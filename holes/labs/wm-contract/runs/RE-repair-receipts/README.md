# Repair receipts for deposited runs — the receipt-only convention

**Adopted by the machine 2026-09-05 under worklist `:AD1`**, on Joe's autonomy
ruling of the same day ("prefer reversible, evidence-backed self-decision plus
accounting, over escalation"). Recorded as
`aif-equations.edn :choices :deposited-run-repair-semantics`, `:adopted-by
:machine`. **Reversal: re-record the choice**, and delete or relabel this
directory's contents; nothing here is read by any check, so nothing depends on
it staying.

## The convention

A run-era row is append-only. `run_era_ledger.bb:241-243` throws on a second,
divergent row for an existing `(run-id, check-id)`, so a demonstrated
retroactive derivation for an already-deposited run **has nowhere to land as a
row** — the finding `C511-repair-or-elaborate.md:499-521` records for all four
standing absences at once.

C511 named three ways out, and this is the one adopted:

1. **Repairs are receipts only** — ADOPTED. The ledger keeps its absences; the
   fold keeps saying `:incomplete` for runs taken before the capture existed;
   the re-derivation lives here, beside the ledger, and a reader joins it to the
   absence row by hand.
2. A repair is deposited as a NEW run-id (`…-repaired`) — NOT adopted. It keeps
   append-only intact by adding to the fold a run that was never taken, so the
   run-era ledger would stop being a record of runs.
3. The schema gains `:row/supersedes` — NOT adopted. It is a change to the seam
   file and to the append API, and it makes every historical verdict provisional
   in a way no reader of an old row can see.

**What a receipt in this directory may therefore do:** re-derive a check at the
state a deposited run was taken at, state the verdict that derivation reaches,
and name the deposited verdict it differs from. **What it may not do:** mutate a
row, mint a run-id, add a supersedes field, or be cited as if it were a
deposited verdict. The row stands; the receipt is the accounting beside it.

## Instances

- `flip-readiness-2026-09-04-010-accepted.md` — the precedent instance
  (`:AD1`, 2026-09-05): U56's as-of re-derivation of `:flip-readiness` for
  `2026-09-04-010-accepted`, whose deposited verdict is `:typed-absence`.
