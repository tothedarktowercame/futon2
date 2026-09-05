# `:AD1` — two machine-adopted defaults, and what they moved

**Row:** `worklist.edn :AD1`, class `:I`. **Date:** 2026-09-05.
**Under:** Joe's autonomy ruling, 2026-09-05 — *prefer reversible,
evidence-backed self-decision plus accounting, over escalation.* Both adoptions
are `:adopted-by :machine`, not Joe rulings, and both are reversible by
re-recording the choice.

Registry entries: `aif-equations.edn :choices :tensions-cashed-reading` and
`:choices :deposited-run-repair-semantics`. **No `control-map-edges.edn
:decisions` entry, and no regeneration into a publish** — `gen_aif_dag.bb` was
run only with `AIF_OUT` redirected to a scratch directory, to confirm the
registry still validates (TN §9a).

## (1) `:tensions-cashed` reads as (A)-strict

`u41_tension_ledger.bb`'s `:verdict-deposited` read reading **(B)** until today:
the run id or one of its tick ids appearing anywhere in the ledger *text*, so a
run was green for being **mentioned**. It now reads **(A)-strict**: green iff
every tension the ledger *attributes* to the run has been **cashed**; `:red` if
any has not; the typed absence only where nothing is attributed.

**Why `:red` and not `:typed-absence` for an uncashed tension.** The absence
would assert that cashing is unobservable. It is observable — the ledger can see
the uncashed tension and name it. It has simply never happened: **0 `:cashed`
events exist in the whole ledger**. `:red` is this vocabulary's word for a check
that found what it looks for, and tension debt is a finding.

### What it moves, measured (`verdict-series.txt`)

```
run                        (A)-strict     (B)-as-coded   attributed uncashed
2026-09-01-s5              :red           :green         1          [:wm-ladder/s5-4e35e740-zero-support]
2026-09-04-re5             :red           :green         1          [:wm-ladder/re5-8ae111bc-zero-support]
2026-09-04-010-accepted    :typed-absence :typed-absence 0          nil
2026-09-05-u59-a           :typed-absence :typed-absence 0          nil
2026-09-05-u59-b           :typed-absence :typed-absence 0          nil
2026-09-05-u60             :red           :green         1          [:wm-ladder/u60-3416e82b-zero-support]
```

This is C511's scenario **S1a** exactly (`C511-repair-or-elaborate.md:458`).
`fold-consequences.txt` is `u56_fold_consequences.bb` re-run today: under S1a
`2026-09-04-re5` moves `:incomplete → :red` and `2026-09-01-s5`'s existing
`:red` gains a second cause.

**The reading that best matches the check's name is the one that makes the fold
worse, and no reading greens any run** — only S5 does, and that takes all four
standing absences. Settling this unblocks nothing, which is why it could be
settled on the evidence rather than escalated.

**What ruled (B) out is not looseness in the abstract.** Two things. (i) S1b —
(A) read as a typed absence — reproduces the deposited ledger *exactly*, while
(B) is the one reading under which re5's deposited row would have been green, so
(B) is the reading inconsistent with deposits already made. (ii) Since `:U63` the
curated ledger carries a tension that **declares** run `2026-09-05-u60` at
`:tension/provenance :records` and is uncashed — so (B) greens a run for a
tension that says it is about that run and has not been cashed, which is the
case (A) exists to catch.

### The deposited rows are NOT repaired

`receipt-drift-after-a-strict.txt` (`u63_receipt_drift.bb`, re-derivable):

```
2026-09-01-s5              deposited :typed-absence replays :red
                           fields-moved [:live-derivation :run-provenance :verdict-deposited :why-the-ledger-cannot-be-run-scoped]
2026-09-04-re5             deposited :typed-absence replays :red
                           fields-moved [:live-derivation :run-provenance :verdict-deposited :why-the-ledger-cannot-be-run-scoped]
2026-09-04-010-accepted    deposited :typed-absence replays :typed-absence
                           fields-moved [:live-derivation :run-provenance :why-the-ledger-cannot-be-run-scoped]
```

No deposited receipt file was rewritten, no run-era row was touched, no
`--deposit` was run. `run_era_ledger.bb --check` exits 0, 24 rows / 8 checks / 0
defects, and the deposited `:tensions-cashed` series is still
`[s5 :typed-absence] [010-accepted :typed-absence] [re5 :typed-absence]` —
asserted by negative control 8t, which now reads it out of the committed ledger.

### Controls

- **8t** (`p4ng/empirics-futon/negative_controls.sh`) previously asserted **0**
  disagreements between the shipped verdict and the pre-`:U60` condition. That
  assertion is now the wrong shape: the two disagree by construction, and
  pinning agreement would pin that the adoption had not happened. It now pins
  the **shipped verdict per run** and the **named disagreement set** — 3 runs on
  the curated ledger, 5 on the planted one — plus the deposited rows unmoved.
- **8z**, new: the discrimination. Two plants differing by **one**
  schema-conforming `:cashed` event, `:red` without it and `:green` with it,
  both validating with **0 defects** so neither verdict is a broken tree, and
  both verdicts taken from the shipped `deposit-receipt` rather than a
  re-implementation. Without 8z, (A)-strict would be pinned only on the side the
  curated ledger always takes.
- In `u41` itself: `:positive/a-planted-cashing-makes-the-attributed-set-all-cashed`
  and `:negative/without-the-cashing-the-same-attribution-stays-uncashed` pin the
  input the verdict reads. **The verdict is deliberately not recomputed inside
  `controls`**: `deposit-receipt` reds on any failing control, so a verdict
  computed there would be either circular or a second copy of the rule it is
  meant to guard.
- 8u is unchanged and still passes: `2026-09-04-010-accepted`'s verdict does not
  move, its own scan of itself does not move, and the fields that did move are
  still exactly the three whole-ledger fold fields.

## (2) Repair semantics for deposited runs: receipt-only

`run_era_ledger.bb:241-243` throws on a second, divergent row for an existing
`(run-id, check-id)`, so a demonstrated repair of an already-deposited run has
nowhere to land as a row — the finding `C511-repair-or-elaborate.md:499-521`
records for all four standing absences at once. Of the three exits C511 names,
**exit 1 (receipts only)** is adopted; the reasons for refusing the other two are
in `runs/RE-repair-receipts/README.md`.

**Precedent instance committed:**
`runs/RE-repair-receipts/flip-readiness-2026-09-04-010-accepted.md` — U56's as-of
re-derivation of `:flip-readiness` for `2026-09-04-010-accepted`, re-run today,
7 flips / 0 READY / 7 BLOCKED, with its discrimination control beside it. The
deposited row stands at `:typed-absence` and the run stays `:incomplete`.

`flip_readiness_check.bb`'s refusal to compose `--as-of` with `--deposit` or
`--emit` is unchanged in behaviour; its comment no longer says the question is
unsettled, because it is now settled the way that refusal already behaved.

## Not done, stated

- **No `control-map-edges.edn :decisions` entry.** These are choice registrations,
  not code-backed corrections to a drawn edge.
- **`gen_aif_dag.bb` not run into a publish** (TN §9a); nothing regenerated into
  p4ng. It was run once with `AIF_OUT=/tmp/ad1-gen/` purely to confirm the
  registry still validates, and p4ng's tree carries only the
  `negative_controls.sh` edit afterwards.
- **No file under `src/` or `scripts/futon2/` changed**, so no machine behaviour
  on the tick path moved. No tick, no run lock, no substrate call, no network,
  nothing written under `data/`.
- **`2026-09-01-s5` gets no repair receipt.** C511 refuted its repair, and not
  for want of a datum: the check and four of its five futon2 sources first appear
  37–43 h after that run.
- **One pre-existing unresolved pointer left alone**: `u41_tension_ledger.bb`
  carries a bare `README.md` pointer (to lines 265-288 of U39's README, with no
  directory) inside the transcribed U39 mint-payload docstring, identical on HEAD before this row.
- **C511's own pointers into `u41_tension_ledger.bb` (`:448-452`, `:467-484`,
  `:500-505`, `:521-523`) were already stale at HEAD** — they cite the pre-`:U60`
  file — and are left standing rather than rewritten, because C511 is a discovery
  record of a state that was true when it was read.
