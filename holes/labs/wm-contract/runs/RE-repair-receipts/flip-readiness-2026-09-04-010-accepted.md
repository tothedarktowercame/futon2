# Repair receipt — `:flip-readiness` for `2026-09-04-010-accepted`

**This is a receipt, not a row.** The deposited row stands unchanged:
`:flip-readiness` / `2026-09-04-010-accepted` / **`:typed-absence`**, deposited
2026-09-04T16:40:56Z, artifact
`holes/labs/wm-contract/runs/RE3-check-deposits/flip-readiness-2026-09-04-010-accepted.edn`.
Nothing here mutates it, supersedes it, or mints a run-id. See `README.md` in
this directory for the convention and its reversal path.

**Why the row is a typed absence.** The run store holds no flip-readiness
artifact, so no verdict contemporaneous with the run was ever recorded, and the
deposit declined to substitute the deposit-time tree's answer for the run's
(the row's own `:row/notes` says exactly this).

**Why this receipt exists.** `C511-repair-or-elaborate.md:62-111` demonstrated
that the absence is *repairable* for this run — `step.edn` and `world-before.edn`
already carry enough to source all six of the check's inputs at the state the run
was taken at. `:U57` then gave `flip_readiness_check.bb` an `--as-of` mode, and
that mode **refuses this run**: the record predates the capture, so `--as-of` on
`runs/2026-09-04-010-accepted/world-before.edn` exits 2. The derivation below is
therefore C511 §6's hand-assembled env-block route, which is the only route that
reaches this run.

## The six sources, at the run's state

The run records `:step/futon2-sha "b1246f214315d56f6185bb810b1829115e2de5a7"`
(`runs/2026-09-04-010-accepted/step.edn:11`).

| line's source | as-of provenance | how it is fixed |
|---|---|---|
| U36 catalog | `git show b1246f21:holes/labs/wm-contract/runs/RUNTIME-VALIDATION-CATALOG.edn` | futon2 at the run sha |
| accounting | `git show b1246f21:holes/labs/wm-contract/variable-situation-accounting.edn` | futon2 at the run sha |
| U27 audit | `git show b1246f21:holes/labs/wm-contract/runs/U27-hole-closability/audit.edn` | futon2 at the run sha |
| FLIP-READINESS.md | `git show b1246f21:holes/labs/wm-contract/runs/FLIP-READINESS.md` | futon2 at the run sha |
| U32 flip-readiness.edn | `git show b1246f21:holes/labs/wm-contract/runs/U32-flip-readiness/flip-readiness.edn` | futon2 at the run sha |
| p4ng tally | p4ng `e508eceb1751`, 2026-09-03T23:50:15Z | last commit touching it before the run |
| p4ng status receipt | p4ng `90d58c02264a`, 2026-09-03T22:50:47Z | last commit touching it before the run |
| mathlib4 contract JSON | mathlib4 `4bbc7111c4b6`, 2026-09-04T14:14:10Z | **content-pinned**, not sha-pinned: sha256 `4e1feed965e962dd3f4c033feaaaceeccda9890048c8e4487a65036fb3c7e5d8`, which is the value `world-before.edn` recorded |

The contract JSON is the one source pinned by content rather than by a repo sha,
and C511 records why that matters: the first extraction used `69721b12`, the
commit the run's rationale names as the CONTRACT AUTHORITY (the last commit
touching `Holes.lean`), but the JSON was re-emitted in the *next* commit. The
hash rejected it. A repo sha alone would not have.

## The derivation

Re-run 2026-09-05 by `:AD1`; the command is `C511-repair-or-elaborate.md:524-542`
verbatim. Output in
`flip-readiness-2026-09-04-010-accepted-asof-summary.txt`:

```
GATE: 7 flips | 0 READY | 7 BLOCKED | lines per flip: contract-pin box2-holes figure5-partials mission-gauges per-node-tests flag-chain
```

**The verdict this reaches is `PASS with 0 of 7 flips READY`** — identical to the
live derivation the seq-21 deposit recorded in the artifact it points at, and
identical to today's live `--summary`
(`flip-readiness-2026-09-04-010-accepted-live-summary.txt`, byte-identical to the
as-of one).

**That identity is not by itself evidence the as-of read happened**, and saying so
is the point of the third file. Because the two summaries agree today, an as-of
mode that silently ignored its overrides would produce the same output. The
discrimination control is what separates them: substituting the pre-RE7 contract
emission (mathlib4 `c4ccafed`) adds `contract-pin` to **every** flip's blocked-on
list (`flip-readiness-2026-09-04-010-accepted-control-old-contract.txt`), so the
harness demonstrably reads the overridden inputs and the answer moves with them.

## What this receipt does NOT establish

- **It does not change the fold.** `2026-09-04-010-accepted` remains
  `:incomplete` on four typed absences; this receipt removes none of them.
  `run_era_ledger.bb --check` is unchanged by it, and must be.
- **It does not close residual (a).** `:step/futon2-tree-dirty?` is `true` and is
  a boolean (`wm_step.sh:123` reduces the porcelain to true/false), so a
  dirty-then-reverted-without-a-commit path is not excluded by the record. The
  three futon2 sources are provably unchanged by any commit since `b1246f21` and
  equal to the worktree today, which is the strongest the record supports.
  `:U57` closed this residual for steps taken from then on, not retroactively.
- **It does not close residual (b).** The `:per-node-tests` line reads live git
  with no as-of seam in the hand-assembled route. It appears in no flip's
  blocked-on list in either derivation, so it does not decide this verdict; it
  would decide it in a tree where the other five lines pass.
- **It says nothing about `2026-09-01-s5`.** C511 refuted the repair there and
  not for want of a datum: the check and four of its five futon2 sources first
  appear 37-43 h after that run (`C511-repair-or-elaborate.md:113-131`). There is
  nothing to point an as-of derivation at, so s5's absence is a check that did
  not exist yet, and no receipt belongs here for it.
