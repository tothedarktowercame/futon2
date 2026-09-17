# R12 two-layer calibration — historical reconciliation (first slice)

Author: zai-35 (R12 item owner), 2026-09-17, dispatched by claude-4 under
codex-28's item-owner scheme. Read-only reconciliation plus this report; no
source, test, checklist or production change is made here.

## Scope narrowed first (acceptance control per clause)

Checklist item R12 (`p4ng/CHECKLIST-fundamentals.md:216`) decomposes into:

| # | Clause | Acceptance control |
|---|---|---|
| C1 | Reconcile P121/P125/P130 against ORIGINAL records | For each claim: named claim file:line, exhaustive identifier search over original-record surfaces, verdict ∈ {supported, refuted, unavailable} with named missing fields |
| C2 | Retain current producer/consumer | Named producer site, named consumer or "no production caller" with the search that establishes it |
| C3 | Independently grounded Layer 2 evidence retained | A record whose Layer-2 referent the model did not produce, with provenance; else named gap |
| C4 | Layer 1 labelled never-value-evidence | Pointer to the standing label and confirmation no record promotes L1 |
| C5 | Catalogue R12 ≠ hyperparameter-inference contract | Both documents pinned and shown distinct (already adjudicated; re-pinned) |

This slice executes C1, C2, C4, C5. C3 (acquiring independent L2 pairs via
R2/WM-04 joins) is a separately bounded acquisition and is reported as a plan,
not claimed.

## C1 — the three historical claims vs original records

The claims (all in `p4ng/sec-discussion-patterns.tex`, Campaign S, 2026-08-21):

- **P121** — `:26` "Layer~1 and the independent Layer~2 both passed" (Tick A).
- **P125** — `:33` "Exact replay reproduced the R11, R12, R15, and R17
  records" (inter-tick).
- **P130** — `:41` "Both calibration layers passed again" (Tick B).

### Search performed (this run, not restated from prior sweeps)

Campaign S hands out four identifiers (`p4ng/empirics.tex:50`): receipt
`e-8ae4e209-f13b-4722-80de-a7fc7d5d68fe`, Tick A attempt
`canary-0a33ac68-aa60-4018-a896-5643f259a2d4`, inter-tick run
`wm-instrumented-S-20260821/r17-intertick`, corpus SHA-256
`972661…51990`. I searched for all four plus `instrumented-S`:

1. Whole tree `/home/joe/code` (`rg`, excluding PDFs/SVGs/binaries,
   mathlib4, category-theory corpora, `.git`): every hit is (a) the paper's
   narrative prose (`empirics.tex`, `sec-discussion-patterns.tex`) or (b) the
   census sweeps' own bookkeeping (`worklist.edn`, `VERIFY-r-nodes.edn`,
   `V7-*-node-sim` receipts, worktree copies `wt-zai1-futon2-suite`,
   `futon2-p9-baseline.FKhOjF`). **No original record carries any identifier.**
2. The Agency evidence store reachable from this session (`memory_read` on the
   receipt id): no entry `e-8ae4e209-…`.
3. Home-dotfile stores (`~/.local`, `~/.config`, `~/.cache`): no hit.
4. The WM trace store `futon2/data/wm-trace/` (62 files): **the file sequence
   jumps from `wm-trace-2026-07-21.edn` to `wm-trace-2026-08-30.edn` — the
   campaign date 2026-08-21 has no trace file at all.** The only `R12` /
   calibration strings in the post-gap files are unrelated
   (`M-pattern-retrieval-calibration`, node-sim era rationale strings).
5. Field Desk store `futon2/data/wm-morning-brief/`, full-loop dirs
   (`wm-full-loop*`, `wm-runs`), `futon3c/data`, `storage/` roots: no hit.

This independently re-establishes, with a wider net, the 2026-09-05 sweep
recorded at `holes/labs/wm-contract/VERIFY-r-nodes.edn:1805` (1966 files, all
hits narrative).

### Verdicts (pattern: irrecoverable-history-stays-historical)

- **P121 — UNAVAILABLE.** Tick A calibration outcome has no original
  execution/replay record on this machine. Missing fields: the per-layer
  records themselves, layer verdict values, independent reviewer identity,
  commission/process identity, source model revision. The prose claim is
  retained as historical narrative; it cannot be upgraded to evidenced.
- **P125 — UNAVAILABLE.** No replay record, no comparison artifact, no corpus
  whose pinned SHA could be checked against `972661…51990`. Same retention.
- **P130 — UNAVAILABLE.** Same class as P121 for Tick B. Same retention.

None of the three is *refuted* — nothing contradicts the prose — but nothing
substantiates it either. Per the cascade's closure rule, later demonstrations
cannot recover absent originals; the honest disposition is a separately
labelled successor (C3), not retro-crediting.

The 892-record V7 census scope is confirmed and not stretched: 69 records
carry an R12 *route hop* only (e.g. `tick-run-record-2026-08-30.edn`:
`R20→R12 via scan-r12-apparatus`); zero carry calibration fields, zero carry
process identity (`runs/V7-R12-node-sim/00-r12.edn`, 7 checks / 4 plants,
re-read this run).

## C2 — current producer/consumer

- **Producer (route attribution only):** `scripts/futon2/report/war_machine.clj:6890`
  — `scan-route2 (route-tag scan-route1 :R12 "futon2.report.war-machine/scan-r12-apparatus")`.
- **Admission boundary (returned/checked cells):**
  `src/futon2/aif/calibration_admission.clj` (`admit!`, refuses
  `:r12/untied-return` / `:r12/unchecked-return`; futon2 `62a6cb5f`), tested at
  `test/futon2/aif/calibration_admission_test.clj`.
- **Production consumer: NONE.** `admit!` has no caller outside its own
  namespace and test (`rg` over `src/` + `scripts/`, this run). The ALIGN
  census's seven-cell absence for R12
  (`ALIGN-rnode-process-census.md:218`, adjudicated basis amendment
  2026-09-06) remains accurate at the returned/checked row for this boundary.
  Commissioned/dispatched/parked/recorded/surfaced cells remain absent.

## C4 — Layer 1 never-value-evidence

The label is standing in the catalogue
(`p4ng/sec-catalog.tex:387`, "permanently labelled never-value-evidence"; only
Layer 2 "clears value"). **No record anywhere in the trace corpus promotes an
L1 result to value evidence** — verified by searching all 62 wm-trace files
for `layer-1/layer-2/never-value` keys: zero hits. The distinguishing
controls hold vacuously on current data because the two-layer records do not
yet exist as structured data at all.

## C5 — catalogue R12 ≠ hyperparameter-inference contract

Re-pinned, unchanged from the adjudicated distinction: catalogue R12 is
`sec-catalog.tex:387` (two calibration layers); the differently numbered
hyperparameter-inference contract is `docs/futon-aif-completeness.md:286-294`;
concordance `p4ng/R-concordance.md:63-67` declares them different
(`runs/V7-R12-node-sim/00-r12.edn` checks 3–4).

## Remaining clauses (plan, not claimed)

- **C3 — independent L2 pairs.** Requires authorized criteria and exact
  prediction/outcome joins from R2/WM-04 (cross-item; route through codex-28).
  Nothing in the current corpus supplies an eligible pair; the 892 historical
  records are not eligible pairs.
- **Consumer connection (cascade R12-4).** Wiring `admit!` into a producing
  tick so a returned calibration artifact has a distinct consumer. This is a
  code change needing an agreed bounded proposal first, per the primer.

## Warrant status — honest gap

No source or test namespace was touched by this slice, so there is no
code/test run to register in `/home/joe/code/storage/test-registry`; the
evidence for this report is the pinned file:line pointers and the recorded
search scopes above, reproducible with the stated `rg` patterns. When the C3
slice adds a namespace, it will register with narrow code/test paths and
report the `evidence/id`.
