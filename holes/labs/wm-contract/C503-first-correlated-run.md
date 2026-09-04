# C503 — the first correlated run (`:RE5`, `EPIC-run-era.md`)

Date: 2026-09-04. Row `:RE5`, the last of the five run-era rows.
Run: `runs/2026-09-04-re5`. Certificate: `runs/RE5-run-conformance`.

## What the row asked for and what it got

| acceptance clause | where |
|---|---|
| run directory committed with the trace | `runs/2026-09-04-re5/` — 4 tick receipts, `wm-trace-re5.edn` (by-`:run/id`), `conformance.edn`, `rationale/` ×4, `retrospective.edn`, `README.md` |
| one ledger row per wired check for this run-id | `run-era-ledger.edn` seq 4 `:flip-readiness :typed-absence`, seq 5 `:contract-pin :green`, seq 6 `:run-conformance :green` |
| rationale records queryable by the RE4 retrospective | `retrospective.edn`, `{:refuted 3, :undecidable 1}`, all six controls pass |
| every `:artifact` pointer resolves; validator green | `bb run_era_ledger.bb --check` exit 0; `--self-test` exit 0 (20 controls); `--report` exit 0 |
| a fresh `certificate.edn`, `:minted-awaiting-acceptance`, both theorems `:axioms []` | `runs/RE5-run-conformance/certificate.edn`; mathlib4 `581034b67e` |
| the run lock taken and released, shown | below |

## The run

    clojure -M:test holes/labs/wm-contract/r6_zero_post_preflight.clj   # exit 0
    bash holes/labs/wm-contract/wm_run.sh 4 14 wm-build-loop            # rc=0

Pre-flight: **0 POSTs attempted, 0 `.admintoken` reads** over 1663 paths read
on the real diagnostic tick (34.3 s), and the lock it takes was released. One
run lock for the whole run (RUN12): taken 07:46:20.654627066Z by
`{:pid 1589668, :agent "wm-build-loop", :host "zone", :sha e05529437c…}`,
released after tick 4 by `wm_run.sh`'s EXIT trap; `data/wm-trace/.run-lock`
does not exist afterwards. Four ticks, 07:46:21 → 07:51, about 79 s each.

`data/wm-trace/wm-trace-2026-09-04.edn` held exactly four forms and all four
carry one of the four receipts' `:run/id`. **No orphan record**, unlike S5,
whose README had to exclude one.

## The finding the row exists to produce: the contract pin is contemporaneous

RE3 deposited a **typed absence** for s5's contract pin, and its evidence said
why in as many words: flip readiness and the contract pin are properties of the
TREE at the moment of asking, a ledger row is about a named RUN, and no file in
the s5 store carried the contract's recorded authority. It also said where that
would be repaired — "RE5 is where these two become contemporaneous."

Half of that came true. Every one of this run's four RE4 rationale records
carries, written at decision time,

    :rationale/contract-sha {:status :present,
                             :git-sha "11c2e44affa167bf85b0a2d7c29d8705f89a8d08",
                             :contract-id "wm-holes",
                             :path ".../DarkTower/WarMachine/holes-contract.json"}

so the run store names the authority the pin is evaluated against and
`:contract-pin` deposits **`:green`** — the first green that check has ever put
on the series. `run_era_ledger --report` now prints the correlation the epic was
built for:

    :contract-pin  2 runs [[… "2026-09-01-s5" :typed-absence] [… "2026-09-04-re5" :green]]

**Flip readiness did NOT become contemporaneous, and is still a typed absence.**
The run store holds no flip-readiness artifact (`:flip-readiness-artifacts []`),
so flip readiness *at this run* is no more reconstructible than it was for s5;
the live derivation (0 of 6 READY, all six blocked on `:box2-holes` and
`:figure5-partials`) is recorded in the receipt under `:not-what-this-says`
rather than deposited. What would fix it is not a deposit-time change: the run
protocol would have to emit a flip-readiness artifact **into the run store as
part of the run**, the way `run3_conformance.bb` writes `conformance.edn`. That
is a build row, not something this row could have done after the fact without
attaching a deposit-time tree property to a run — the exact reading hazard RE3
named.

## Two defects found by reading the first green rather than accepting it

**(1) The first green was carried by prose.** `run-store-scan`
(`checks/contract_authority_current.clj`) listed only the run directory's TOP
LEVEL, and the rationale records live in `rationale/`. So the files that
actually matched the authority sha were `README.md` — a sentence I had written
into it — and `retrospective.edn`. The machine's own records were invisible to
the check that was reporting on them. The scan is now recursive over the store.

**(2) `:recorded-authority-appears-in-store?` was a boolean with no witness.**
A README quoting the sha satisfies it exactly as a machine-written record does,
and nothing in the receipt let a reader tell those apart. The scan now also
reports `:recorded-authority-appears-in`, the files that carry it, and the
deposited notes name them and say the reader must judge which are records of
the run and which are narrative about it. For this run that list is six files:
`README.md`, the four rationale records, and `retrospective.edn`.

The new key is emitted only when something matched, so a typed-absence receipt
is unchanged. Checked, not assumed: re-running RE3's s5 deposit rewrites its
committed receipt **byte-identically** (`cmp`) and the ledger answers
`:already-present` at seq 2.

## A third defect, reported and NOT repaired here

**The contract-pin deposit is replay-stable only while mathlib4 stands still.**
Depositing this row, then committing the Lean certificate block (`581034b67e`)
and re-emitting `holes-contract.json` (`c4ccafed32`), then re-running the same
`--deposit 2026-09-04-re5`: the check now reads the NEW authority
`581034b67e47…`, does not find it in the run store, and computes
`:typed-absence` where it computed `:green` an hour earlier — for the same run.

It fails closed rather than lying: the rewritten receipt is dirty, the ledger
refuses with `:artifact-dirty`, and no divergent row is appended. But the shape
is wrong for a ledger of runs, and the check's own notes already name the
missing piece — "this check has no as-of-sha mode". A row about a run should
ask whether the pin held at the authority THE RUN RECORDED, which needs reading
the contract out of history at that sha. That is a build row, not a repair I
could make inside this one, and RE3's idempotence claim for `:contract-pin`
should be read as holding relative to a fixed mathlib4 authority.

The receipt this row committed records `:recorded-authority
11c2e44affa167bf85b0a2d7c29d8705f89a8d08` explicitly, so a later reader can see
which authority the green was about. Working tree restored with `git checkout`
after the experiment; the committed receipt and its row are untouched.

## The certificate

`mathlib4 581034b67e` adds `wmRe5RunConformsToDrawnWiring` and
`wmRe5RouteCensus`, both `by decide`, no `sorry`, no `native_decide`, both
reporting *"does not depend on any axioms"*; `lake build` green (2704 jobs) and
the module's sorry count unchanged at 10. `holes-contract.json` re-emitted at
the new authority in `c4ccafed32` — 124 declarations byte-identical, 114 closed
/ 10 holes, one field moving (the source git-sha).

`u49_route_transcribe.bb` became a producer **per run**: `U49_SLUG` names the
run inside Lean, `U49_EMIT_TABLES=0` suppresses the shared definitions so a
second run's block reuses U49's instead of redefining them. That reuse is a
claim — that the drawn map has not moved — so the producer gained **control
C7**, comparing this run's control-map sha256 against the one
`runs/U49-run-conformance/00-source.edn` records the reused tables were
generated from. Both `161d0abf…`, p4ng `e508ece`; C7 passes. C7 does *not*
check that the U49 block is unedited, and the certificate's `:limits` says so.

The default invocation is **byte-identical to what U49 committed**, all six
artifacts. Getting there caught a defect in my own first cut: the emitted
docstring pointed at the *outdir* for the exercised mutations, and the outdir is
an invocation argument, so two runs generating the same certificate emitted
different Lean — the same class of defect RE3's review caught in the RE2 report.
It is a literal again (`U49_CONTROLS_REF`).

**The census is identical to s5's, hop for hop, and that is not confirmation.**
4 routes, 36 hops, 9 distinct; 2 drawn, 5 route-measured, 1 excluded at
dependency grain (`R2→R7`), 1 ruling-unrealised (`R5→R6`), 0 refutations,
0 unmapped, 19 of 22 drawn edges unfired. Both runs walk the same nine-hop
route `R20 R12 R2 R7 R3 R8 R5 R6 R14 TRACE`; two runs agreeing about a route
they were always going to take is one measurement repeated. It is in `:limits`.

## What the two runs DO differ in, and the route tags cannot see it

S5 ran the selector seam `stub:first-ranked-authorized-mission`; these four
ticks ran `stub:controller-head`. It shows only in the rationales:

|  | s5 (RE4's store) | 2026-09-04-re5 |
|---|---|---|
| chosen rank | 123 | 1 |
| score plateau at the chosen score | 55 wide, ranks 73–127 | 1 wide, rank 1 |
| runner-up | R14 head, G-core margin 0.1469 on all four | absent, `:reason :chosen-is-controller-head` |
| `:moved-from-controller-head?` | — | false on all four |
| targets | `M-aif-policy-conditioned-eig` / `M-wm-aif-policy-grain-compliance` | `M-zaif-harness-v1` / `M-expressions-of-interest` |

146 candidates each tick, 141 distinct `[type target]` keys, 4 admission
refusals each (all `:mission-absent-from-capability-graph`), τ held at 1.0.
The conformance certificate is blind to every row of that table, which is the
same grain point C5 makes about the drawn set — worth saying because "the
census is identical" would otherwise read as "the runs are the same run".

The RE4 retrospective over this run's own store returns `{:refuted 3,
:undecidable 1}`: the four decisions alternate between the two targets, so
every adjudicable record is refuted by the tick after it and the last has no
successor. Same shape as s5's, from a different selection path.

`re4_rationale_retrospective.clj` gained `--store` / `--out` so the query runs
over a run's own slice; the no-argument invocation still writes RE4's artifact
byte-identically (`cmp`).

## NOT DONE, stated

* **No `:choices` and no `:decisions` entry.** Whether either minted
  certificate qualifies is Joe's act (RUN4); `wmRunConformsToWiring` stays
  `mkHole` and there are now **two** certificates awaiting acceptance.
* **`gen_aif_dag.bb` not run**, nothing regenerated into the publish (TN §9a).
* **Both runs still fold to `:incomplete`.** Four catalogued checks —
  `:enumeration-completeness`, `:per-node-runtime-validation`,
  `:rationale-regret`, `:tensions-cashed` — have deposited nothing for either
  run. This run *has* the artifact a `:rationale-regret` row would point at
  (`retrospective.edn`), but the catalogue names `u39_selection_retrospective.bb`
  as that check's machinery, not RE4's script, so wiring it is a separate row
  rather than a pointer I could redirect.
* **No flip, no operational certificate, no bounded-suite receipt.** Flips stay
  J-gated; `make certify-run` is a separate operator act this row does not ask
  for. The tree was NOT quiescent in the RUNBOOK's sense — another lane
  committed `e0552943` between the pre-flight and the run start — which is
  precisely why no bounded receipt or operational certificate is claimed here.
* **No second host** for the run lock's `:host` fail-closed branch (RUN12's
  standing caveat, unchanged).

## Gates, bare exits

`run_era_ledger --check` 0 after each of the three appends and on the committed
result; `--self-test` 0 (20 controls, 13 negative); `--report` 0;
`bb flip_readiness_check.bb` 0 and byte-identical narrative (PASS, 0 of 6
READY); `bb checks/contract_authority_current.clj` 0 (PASS-CONTENT-ONLY at the
new authority); `bb run3_conformance.bb runs/2026-09-04-re5` 0 (CONFORMANT);
`lake build DarkTower.WarMachine.Holes` 0; `lake env lean` axiom probe 0, all
four theorems axiom-free; clj-kondo 0 errors 0 warnings on each touched file
linted individually; check-parens OK on all three; `bb -cp . checks.wm-workspace-gate/inventory-result`
`{:unknown (), :missing (), :exit 0}`; futon2 `selection-rationale-test` +
`trace-test` 62 tests / 281 assertions, 0 failures 0 errors;
`negative_controls.sh` PASS (33 negative, 16 positive); `pointer_check.bb`
1250 pointers / 0 unresolved.
