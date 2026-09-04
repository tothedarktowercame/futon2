# 2026-09-04-re5 — the first correlated run (worklist `:RE5`, `EPIC-run-era.md`)

Four ticks, one run lock (RUN12), each record carrying its `:run/id` (RUN11),
at futon2 sha `e0552943`.

    clojure -M:test holes/labs/wm-contract/r6_zero_post_preflight.clj   # PASS
    bash holes/labs/wm-contract/wm_run.sh 4 14 wm-build-loop            # rc=0

The pre-flight reported 0 POSTs attempted and 0 reads of `.admintoken` over
1663 paths read, and the run lock it takes was released before the run. The run
lock was then held once for the whole run — `wm_run.sh` takes it before tick 1
and releases it after tick 4 — with holder `{:pid 1589668, :agent
"wm-build-loop", :sha "e05529437c79da3f63d01518defd8de225850e4e", :host "zone",
:acquired-at "2026-09-04T07:46:20.654627066Z"}` and no lock file left behind.

`wm-trace-re5.edn` holds the four records, selected out of the shared per-date
trace file `data/wm-trace/wm-trace-2026-09-04.edn` by `:run/id` (8ae111bc,
308d1622, 67c72ac3, c149f9de). That file held exactly four forms and all four
are this run's: **no orphan record this time**, unlike S5.

## What is new here, and it is the whole point of the row

This is the first run born instrumented. Three things landed *while the machine
ran* rather than being reconstructed afterwards:

* **`rationale/`** — four RE4 records, one per persisted decision, written at
  decision time by the seam at `scripts/futon2/report/war_machine.clj:2250`.
  Copied here from the live store `data/wm-rationale/` (untracked), which is
  where the seam writes.
* **`conformance.edn`** — RUN3's pinned verdict for this run, `:conformant`,
  written when the run was taken.
* **the run-era ledger rows** — `run-era-ledger.edn` now carries this run's
  `:flip-readiness`, `:contract-pin` and `:run-conformance` rows.

**The contract pin is GREEN for this run, and that is the contemporaneity RE3
could not have.** RE3's s5 contract-pin row is a typed absence because no file
in the s5 store names the contract authority. Every one of this run's four
rationale records carries `:rationale/contract-sha {:status :present, :git-sha
"11c2e44affa167bf85b0a2d7c29d8705f89a8d08", :contract-id "wm-holes"}`, read
from `mathlib4 DarkTower/WarMachine/holes-contract.json` at decision time, so
the store names what the pin is evaluated against and the row is a verdict
about the run rather than about the tree.

## What the run shows

RUN3, `bb run3_conformance.bb runs/2026-09-04-re5`, exit 0:
**CONFORMANT** — 4 routes, 36 hops, 9 distinct; 2 drawn (`R7→R3`, `R8→R5`),
5 route-measured, 1 excluded at dependency grain (`R2→R7`), 1 ruling-unrealised
(`R5→R6`), 0 refutations, 0 unmapped, 19 of 22 drawn edges unfired. That census
is identical to S5's, hop for hop.

The route is `R20 R12 R2 R7 R3 R8 R5 R6 R14 TRACE`, nine hops, unchanged in
shape from S5.

**The selection changed and the record says so.** S5 ran the selector seam
`stub:first-ranked-authorized-mission`; these four ticks ran
`stub:controller-head`, and it shows in the rationales: the chosen action is
the controller head at rank 1 on all four (`:selection-law {:requested
:controller-head, :applied :controller-head, :moved-from-controller-head?
false}`), with `:controller-score-tie {:count 1, :rank-band [1 1]}` — no
plateau — and `:runner-up {:status :absent, :reason :chosen-is-controller-head}`.
RE4's s5 store, by contrast, recorded rank 123 inside a 55-wide score plateau
with the R14 strategic selection overwriting the R6 choice. The two runs
therefore exercise two different selection paths, which is worth saying because
the conformance census does not distinguish them.

Targets chosen, in `:rationale/at` order: `M-zaif-harness-v1`,
`M-expressions-of-interest`, `M-zaif-harness-v1`, `M-expressions-of-interest`
— 146 candidates each tick, 141 distinct `[type target]` keys, 4 admission
refusals each (all `:mission-absent-from-capability-graph`), τ held at 1.0.

`retrospective.edn` is the RE4 regret query over this run's own rationale
store.

## What is NOT here

No flip was taken (every flip stays J-gated; the epic measures readiness and
does not force it). No operational certificate and no bounded-suite receipt:
`make certify-run` is a separate operator act and this row does not ask for it.
The certificate this run does carry is the **run-conformance** certificate,
`runs/RE5-run-conformance/certificate.edn`, `:status
:minted-awaiting-acceptance` — accepting it is Joe's act (RUN4).

Full account: `../../C503-first-correlated-run.md`.
