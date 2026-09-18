# R12 final disposition — 2026-09-18

Owner: zai-35. Coordinator claude-4 ruled (invoke-1789742920712-22124) that
clause 3 cannot be met before the deadline and that this disposition, not
further work, closes out the seat. This file records the disposition so
nobody re-opens R12 from scratch.

Acceptance text (checklist `p4ng/CHECKLIST-fundamentals.md:216`, verbatim,
not reworded): reconcile the historical Campaign S claims (P121/P125/P130)
with original records; retain current producer/consumer plus independently
grounded Layer 2 evidence; Layer 1 must remain labelled never-value-evidence;
Catalogue R12 is not the hyperparameter-inference contract.

## Clause 1 — historical reconciliation: MET

Commit `db23dec8`, report `00-reconciliation.md` (this directory).

- P121 / P125 / P130 each adjudicated **UNAVAILABLE**: no original record on
  this machine carries any Campaign S identifier (receipt id, Tick A canary,
  inter-tick run id, corpus SHA), under a whole-tree search wider than the
  2026-09-05 sweep (`VERIFY-r-nodes.edn:1805`). Not refuted — nothing
  contradicts the prose — but nothing substantiates it; retained as historical
  narrative, never upgraded to evidenced.
- V7 census scope confirmed and not stretched: 892 records / 69 R12 routes,
  zero calibration-field or process-identity records
  (`runs/V7-R12-node-sim/00-r12.edn`, re-read during the reconciliation).
- Layer 1 never-value-evidence label pinned (`p4ng/sec-catalog.tex:387`); no
  record anywhere in the trace corpus promotes an L1 result to value
  evidence. Catalogue R12 shown distinct from the hyperparameter-inference
  contract (`docs/futon-aif-completeness.md:286-294`,
  `p4ng/R-concordance.md:63-67`).

## Clause 2 — current producer/consumer: MET

Commit `c77c802b`, report `01-producer-consumer-wiring.md` (this directory).

- Producer: `war_machine.clj` `scan-r12-apparatus` (route-tagged R12).
- Admission cycle: `src/futon2/aif/calibration_cycle.clj` (commission-tied
  return with standing L1 label and explicit L2 not-available status,
  structural check, `calibration-admission/admit!`).
- Consumer reachability (verified by the coordinator 2026-09-18):
  `war_machine.clj:7018` calls `admit-apparatus!` inside
  `generate-war-machine` (defn at :6968), which
  `full_loop_runner.clj:3739` calls on the live tick — a genuine production
  caller, not a caller inside a dead function. The admitted receipt enters
  `scan-data` as `:r12-admission` and is surfaced by the render layer.
- Gates: clj-kondo 0/0, check-parens clean, 11 tests / 32 assertions
  (0 failures). Test Registry warrant
  `test-registry-e496524d5d086a6b8c1dadf07f51fb3c412ce18ad1bdaa150dcefadd1be102dd`
  (`:warrant? true`, postcheck matched), scoped to the three changed paths.

## Clause 3 — independently grounded Layer 2 evidence: BLOCKED-ON-ABSENT-EVIDENCE

The evidence itself does not exist, and this was **checked, not assumed**:

- Whole-tree search of `/home/joe/code` (slice 1, `00-reconciliation.md`
  §C1/C3) found zero eligible independent prediction/outcome pairs; the 62
  wm-trace files contain no structured two-layer calibration records at all
  (the campaign-date trace file is absent from the sequence), and the 892
  historical V7 records are route hops, not pairs — the cascade's
  distinguishing controls forbid counting them as pairs.
- The WM-04 provenance packet (2026-09-16, independently reviewed) reached
  the same conclusion from its own side: no eligible pair, no admitted label.

The pending eligibility rule is **downstream** of this absence, not the cause
of it: settling the rule would determine which pairs would count, and the
answer would still be that none exist. No labelling choice converts an
absence into "retained … independently grounded Layer 2 evidence"; only
evidence does.

**What would unblock it:** a prospective study that generates at least one
eligible independent prediction/outcome pair under a reviewed eligibility
rule. That is a data-generation effort, out of scope for this deadline, and
it is the sole remaining path; there is no documentary route to clause 3.

## Standing controls, unchanged

Layer 1 remains labelled never-value-evidence on the admitted artifact
(`calibration_cycle.clj` `layer-1-label`; asserted by
`layer-1-is-never-promoted`). Catalogue R12 remains distinct from the
differently numbered hyperparameter-inference contract.
