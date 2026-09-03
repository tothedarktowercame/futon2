# FLIP-READINESS — the gate that arms a flip, worklist row `:U32`

Joe, 2026-09-03: *"if we are gonna flip the machine on, we should get a
reasonably confident state ... so it is gathering empirical data against the
best effort system, not some kind of partial system."*

This file makes "best effort" a thing a script can check. For each J-gated flip
the runtime-validation catalog declares, `flip_readiness_check.bb` derives one
line per blocking red and reports **READY** or **BLOCKED-ON [lines]**.

**Nothing here flips anything, and nothing here is a ruling.** A green checklist
is what a flip goes to Joe *with*; it is not permission, and no line in this
file was written by hand — the block below is emitted by the checker and the
checker refuses if the committed text and its own derivation disagree.

## The verdicts, as of the commit that carries this file

<!-- BEGIN flip_readiness_check -->
```
FLIP fpi-posterior    BLOCKED-ON [box2-holes figure5-partials]
FLIP guide-gate       BLOCKED-ON [box2-holes figure5-partials flag-chain]
FLIP mission-c        BLOCKED-ON [box2-holes figure5-partials mission-gauges]
FLIP selection-law    BLOCKED-ON [box2-holes figure5-partials flag-chain]
FLIP tau-variational  BLOCKED-ON [box2-holes figure5-partials flag-chain]
FLIP zaif-u14e        BLOCKED-ON [box2-holes figure5-partials flag-chain]
GATE: 6 flips | 0 READY | 6 BLOCKED | lines per flip: contract-pin box2-holes figure5-partials mission-gauges per-node-tests flag-chain
```
<!-- END flip_readiness_check -->

Reproduce: `bb futon2/holes/labs/wm-contract/flip_readiness_check.bb`
(`--summary` for the block alone, `--emit` to regenerate this file and the
sidecar `runs/U32-flip-readiness/flip-readiness.edn`, which carries the full
per-line derivation including every pointer named below).

## The six lines and where each is read from

| line | feed | live source | today |
|---|---|---|---|
| `:contract-pin` | U26 | `mathlib4/DarkTower/WarMachine/holes-contract.json` `source.git-sha` vs the last commit touching `Holes.lean` | **green**, both `6dabfb686f` |
| `:box2-holes` | U27 | `variable-situation-accounting.edn` `:open-hole` rows, cross-read against `runs/U27-hole-closability/audit.edn` | **blocked**, 16 open, 0 typed |
| `:figure5-partials` | U30/U31 | `p4ng/empirics-futon/defect-repair-tally.edn` | **blocked**, 60 `:repaired` / 1 `:partial` |
| `:mission-gauges` | U28 | `p4ng/empirics-futon/wm-status-receipt.json` `mission-criteria-gauges` | **blocked for `mission-c`**, 3/9; `:n/a` elsewhere |
| `:per-node-tests` | Joe's standing per-box requirement | `runs/RUNTIME-VALIDATION-CATALOG.edn` (U36) + a freshness check of each named test file | **green for all six**, with caveats |
| `:flag-chain` | the flip's own `:requires` | the catalog's `:flips` | green for 2, blocked for 1, **not derivable for 3** |

### `:contract-pin` — the comparand matters

The check is the contract's recorded authority against **the last commit that
touched `DarkTower/WarMachine/Holes.lean`**, not against mathlib HEAD. C175
settled that, and `checks/contract_authority_current.clj:22` uses it: a correct
regeneration moves HEAD itself, so a HEAD comparison can never read
`:current` after the very act that makes it current. Both are
`6dabfb686f9754454d7689dc5b5bc39ae08dcf0b` today, so the line is green.

The status receipt still records the *previous* fresh pair
(`6de47bd050…`, receipt timestamp `2026-09-03T16:40:51Z`), because U29's
mathlib4 commit landed after it. The receipt's value is recorded in the sidecar
as context and is **not** the verdict — deriving live is what keeps this line
from inheriting a receipt's age.

### `:box2-holes` — the typing this line needs does not exist

The row asks for holes "closed or typed not-flip-blocking". Neither obtains:
`runs/U27-hole-closability/audit.edn` closed zero holes (its `:counts`
`:closed-by-this-audit 0`), and no hole row carries a `:flip-blocking` field —
the accounting's `:axes` declares `:closability` and `:readiness` (U27's
pre-run/run-gated fence) and nothing about flips.

So all 16 open holes block, and the checker says so rather than deciding
relevance itself. **Deciding which hole blocks which flip is exactly the typing
that is missing**, and inventing it here would be the plant U28 was corrected
for: a gauge that reads the artifact clause alone measures a different
criterion and scores it as this one. The follow-on is named below.

What the line does report, because it is derived and useful: 15 contract
declarations plus the one glossary-side hole U14 promoted; 9
`:pre-run-closable` and 7 `:run-gated`; and that the audit's row set and the
accounting's open-hole set are the same 16 names (`:audit-agrees? true`), so
the two artifacts are not drifting apart underneath the count.

### `:figure5-partials` — one instance, and its remainder is Joe's

61 tally instances, 60 `:repaired`, one `:partial`: `:r16-engine-wiring`. U31
narrowed its remainder to **one claim in two artifacts** — "R16 performs a
grounded outward act", stated at `futon2/holes/problems/P-R16.md:42-47` with
the Edges label at `:62`, and drawn as the box label at
`p4ng/aif-control-map-paper.svg:173` — forked at
`futon2/holes/problems/DECISIONS-PENDING.md:61-76`, which is Joe's
safety/authority call. This line cannot go green without that decision, and it
blocks every flip because the tally is a whole-tree property, not a per-flip
one.

### `:mission-gauges` — applicable to one flip, and derived that way

The line applies iff the flip's flag chain names `FUTON_WM_MISSION_C`, which is
the only flag under which a mission's completion criteria are read at all
(`war_machine.clj:86-100`). Today that is `mission-c` alone; for the other five
the line is `:n/a` with that derivation recorded, not silently dropped.

For `mission-c` it is blocked at 3 of 9: `M-zaif-harness-v1` measures 3/3,
`M-expressions-of-interest` measures 0 of 6, every criterion typed
`no-producer` by U28 — six criteria that name no measurement, five of which
conjoin a clause only Joe can discharge.

### `:per-node-tests` — green for all six, and what the caveats say

Joe's standing requirement is that the per-node unit tests for every R node the
flipped path exercises are green **separate from any full-run**. The catalog's
`:test-runs` are one namespace per JVM (`clojure -X:test :nses '[…]'`), which is
what separate means here; the rows used are `:axis :per-node` only, so a
`:global-run` row cannot stand in for a node.

Two things this line does that reading the catalog alone would not:

1. **The node → test file map is derived, not cited.** A row names a namespace;
   the checker turns that namespace into a path and **fails** if the file is not
   there. Citing `:pointer` instead would let a node's "test" be a source file
   or a coverage registry — `:n/R9-zaif-authorship`'s pointer is
   `zaif-harness/runs/U10-node-coverage.edn`, which is a matrix, not a test.
2. **A recorded green over a file that has since moved is stale, not fresh.**
   The catalog recorded its runs at futon2 `42a957c` / futon3c `17fde898`; the
   checker diffs each named test file against that head *and* the working tree.

Every exercised node still has at least one fresh green, so the line is green
for all six flips. The caveats it names are real and are in the sidecar:
`:n/R6-zaif-controller`, `:n/R2-zaif-envelope` and `:n/R16-zaif-witness` are
stale-green (their futon3c test files moved after the run); `:n/R9-zaif-authorship`
is the catalog's own `:exists-but-stale`; and R4 carries a named gap,
`:n/R4-zaif-observation-model`, owned by `:U11 :D8 :S7`.

**What this line deliberately does not decide.** For R9 the two fresh greens are
both WM-side, and `guide-gate` and `zaif-u14e` are zaif flips. A node green only
on the WM side is not the same evidence for a zaif flip — but typing that
difference into a verdict needs a per-flip substrate the catalog does not
declare, so it is *reported* (`:substrates` per node in the sidecar) and named as
a follow-on rather than ruled here.

### `:flag-chain` — three flips whose stated requirement cannot be checked

The chain is read from each flip's own `:requires`. `FUTON_…` tokens that name
another catalog flip make that flip a prerequisite; the rest are run-time
environment preconditions, recorded and not blocking.

- `mission-c` → `FUTON_WM_CLOCK_FOCUS`, not a gated flip: **green**.
- `fpi-posterior` → `FUTON_WM_FPI_DARK`, `FUTON_WM_TRACE_POLICY_DETAILS`, neither
  a gated flip: **green**.
- `selection-law` → `FUTON_WM_FPI_POSTERIOR`, which *is* the `fpi-posterior`
  flip, which is not READY: **blocked**, transitively.
- `tau-variational`, `guide-gate`, `zaif-u14e` → **`:not-derivable`**, which
  counts as blocking. A requirement nobody can check is not a requirement that
  passed, so the checker does not let it read as green; the declared text is
  carried verbatim in the sidecar.

`tau-variational` is the one where the catalog is repairable rather than the
world: its `:requires` reads "the beta/F_pi chain; the coupling is checked by
`variational-tau-preconditions!`", while the code names the three flags exactly
— `FUTON_WM_BETA_DARK`, `FUTON_WM_FPI_DARK`, `FUTON_WM_TRACE_POLICY_DETAILS` at
`futon2/scripts/futon2/report/war_machine.clj:337-340`. Writing those three into
the catalog's `:requires` would move this line to green on the same evidence,
and would make `tau-variational` chain on `fpi-posterior`'s own prerequisites
rather than on prose. That is a U36 catalog repair, not a U32 verdict, so it is
named and not taken.

For `guide-gate` and `zaif-u14e` the requirement is not a flag at all
("persisted `:receipt/depositor-seat` and `:receipt/reviewer-seat` on new
receipts"; "the U14e-1..4 packets landed and their census taken"). Those are
checkable in principle — the zaif ledger records e-4 parked behind U11f — but
not from any source this checker reads, and typing them green on that basis is
what the `:not-derivable` verdict exists to prevent.

## Not done here, named rather than implied

1. **The status receipt is not wired.** The acceptance allowed it "only if
   trivial", and it is not: `gen_status_infographic.py:62` asserts *exactly
   twelve* components and `:295` prints a pinned receipt SHA-256, so a
   thirteenth component moves a published figure and its pin. Follow-on row.
2. **The `:flip-blocking` typing on hole rows** — the input `:box2-holes` needs
   to say anything sharper than "all 16". It belongs on the hole's registry row
   beside U27's `:closability`, written by the same generator
   (`futon2/scripts/generate_variable_situation_accounting.bb`). Follow-on row.
3. **`tau-variational`'s `:requires`** (above) — a U36 catalog repair.
4. **Per-flip substrate** on the catalog's `:flips`, so a zaif flip is not
   satisfied by a WM-only green (above).
5. **No test was re-run by this row.** The per-node greens are U36's recorded
   2026-09-03 runs; this row adds the freshness check over them, not a re-run.
   Re-measuring is U36's refresh.

## Controls

`p4ng/empirics-futon/negative_controls.sh` section 8: a planted stale contract
pin must block (8a); a hole typed `:flip-blocking false` under an **undeclared**
axis must still block, and clear only once the axis is declared (8b/8c); a node
whose per-node runs are all red must block the flips that exercise it (8d); a
catalog row naming a namespace with no test file must fail rather than counting
as coverage (8e); and the committed verdict block must be what the checker
computes (8f, positive).
