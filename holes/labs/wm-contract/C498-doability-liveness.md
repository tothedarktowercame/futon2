# C498 — U44: the doability factor, plumbed to the phase that was already there

**Row:** `worklist.edn :U44` (class I), J7's named prerequisite.
**Artifacts:** `runs/U44-doability-liveness/` (README.md, J7-REVISIT-PACKET.md,
nine EDN files), produced by `u44_doability_liveness.clj`.
**Author:** claude (wm-edge worklist lane), 2026-09-03. **No ruling is made here.**

## 1. What the row asked for, and what it got

| acceptance clause | where |
|---|---|
| live phase read behind a declared input | `war_machine.clj:221-248` (the input), `:1506` + `:1517-1522` (the read), `:2537-2539` (the use) |
| default byte-identical, **pinned** | `runs/U44-doability-liveness/05-default-pin.edn` — 133 of 133 rows identical to the ranking the pre-repair code produced; plus `war_machine_test.clj:2461-2470` |
| S2-style step-through, before/after under the live factor | `02-default-arm.edn`, `03-live-arm.edn`, `04-rank-moves.edn` |
| the 123-agree/0-disagree check re-run through the new path | `06-phase-agreement.edn` — `{:agree 123, :doability-phase-absent 10}`, zero disagreements |
| C492 4b updated | `C492-mission-epistemic-value.md` section 4b, paragraph added |
| J7 revisit packet, not ruled | `runs/U44-doability-liveness/J7-REVISIT-PACKET.md` |

## 2. The repair is a read, not a new source

`compute-delta-t-mission` (`war_machine.clj:2254-2263`) resolves phase through
`futon3c.aif.mission-delta-t`, which is not on futon2's classpath; the
`{:delta-T 0.0}` fallback carries no `:mission-phase`; `phase-doable`
(`:2485-2487`) therefore returns the `"unknown"` 0.3 for every candidate.
`mission-doc-index` (`:1480-1509`) was already fetching the hyperedge that
carries `:mission/phase` and discarding the prop. It now keeps it (`:1506`),
`mission-index-phase` reads it (`:1517-1522`), and the doability factor takes it
**only** under the declared input (`:2537-2539`).

Three properties worth stating because they are what make it reviewable:

- **The delta-t carrier is kept as the fallback**, not replaced. Where the
  hyperedge has no phase and delta-t does, delta-t wins and the record says so
  (`war_machine_test.clj:2487-2500`). The repair adds a source; it does not
  retire one.
- **`:phase-source` is recorded** (`:2566-2569`) and appears only under the
  declared input. `"unknown"`-0.3 from an unreadable phase and 0.3 from a phase
  that is genuinely `"map"` are the same scalar and different facts; without the
  field a reader could not tell the repaired record from the broken one.
- **The fiat table is untouched.** `phase-doability` (`:2341-2351`) is the same
  ten numbers. This row changes what is fed to it, not what it says.

## 3. The finding the row did not go looking for: the completion gate

The completion gate is `(if (= "complete" phase) 0.0 1.0)`
(`war_machine.clj:2545`) and a nil phase never equals `"complete"`. So the same
nil that flattened doability **also disabled the gate**, on the same date and
by the same mechanism. Measured on the 133 pinned candidates: 0 completion-gated
in the default arm, 8 in the live arm, and that set is exactly the set of
`"complete"`-phase rows — M-case-studies, M-essay-corpus-substrate,
M-essays-edit-cycle, M-futonzero-grounding, M-memory-retrieval,
M-run-produces-its-own-brief, M-shared-memory-control-build-test,
M-typed-holes-lean-handoffs. Since 2026-07-19 those eight have been ranked as
ordinary live candidates. `04-rank-moves.edn :gates`, control C9,
`war_machine_test.clj:2502-2513`.

This is worth separating from the doability half when the flip is discussed:
pricing an `instantiate` mission above a `head` mission is a preference the
fiat table encodes, but ranking a mission the field marks complete is not a
preference at any weighting.

## 4. What the arms measured

133 candidates pinned from the S2 baseline (the set U22 used), weights held at
Joe's 2026-09-02 declaration `{:central 0.20 :strategic 0.50 :doable 0.30}`, so
the only thing that varies is the declared input.

- `:doable` goes from `{0.3 129, 0.0 4}` to nine distinct values spanning
  0.0–1.0; `:phase` from nil on 133 to readable on 128.
- **129 of 133 ranks move**; 128 of 133 at the shipped default weights (C7).
- Rank 1 is M-zaif-harness-v1 in both arms.
- Largest gains: M-wm-aif-policy-grain-compliance 125 → 36, M-sci-reproduction
  116 → 34, M-sci-reproduction-replay-ledger 117 → 35, M-omni-wm-runner
  110 → 40, M-kangaroo 101 → 33.
- For J7: the epistemic term at 0.15 moves **115** ranks against the inert
  factor and **62** against the live one. The J7 packet has the four-corner
  table.

## 5. What the cross-carrier check now means

C492 4b's "123 agree, 0 disagree" was produced by a `with-redefs` substitution
inside U22's producer — a measurement of what the shipped code *would* do. It
is now a measurement of what the shipped code *does*:
`{:agree 123, :doability-phase-absent 10}` through `:live-doability?`, against
`{:doability-phase-absent 133}` on the default path, with the disagreement list
empty. The two carriers remain two independent substrate reads —
`mission-epistemic-value/field-readings` and `mission-doc-index` — so the check
still has content; what changed is that one side is now the shipped path rather
than a substitution.

New detail the U22 artifact did not separate: the 10 absences split **5/5**.
Five missions carry the literal string `"unknown"` on the hyperedge (which
`readable-phase` refuses by name) and five carry no phase prop at all. Both
groups keep the 0.3 default, so the flip shrinks the constant from 133
candidates to 10 rather than removing it.

## 6. Gates

`clj-kondo` 0 errors / 0 warnings on each changed Clojure file individually
(`war_machine.clj`, `war_machine_test.clj`, `u44_doability_liveness.clj`);
`futon4/dev/check-parens.sh` OK on all three; 288 tests / 3939 assertions over
`futon2.report.war-machine-test`, `futon2.aif.mission-epistemic-value-test`,
`futon2.aif.epistemic-value-test`, `futon2.aif.full-loop-runner-test`,
`futon2.aif.mission-c-test` and `futon2.aif.survey-mission-value-test`, 0
failures 0 errors — the six namespaces that are the whole of what greps for
`enrich-candidates-with-mission-value`, `mission-doc-index` or `phase-doability`
plus the two suites C492 ran. The eight artifacts the producer writes are byte-identical on two consecutive
runs (the ninth, `00-preedit-baseline.edn`, is written once against the
pre-repair code and then only read). `negative_controls.sh` and `pointer_check.bb` run before the
commit; `gen_aif_dag.bb` deliberately NOT regenerated (TN §9a gate rule).

**One pre-existing failure, not this row's and not repaired here:**
`positive-proof-receipt-test/honest-positive-still-passes` fails against
`softmax-positive-receipt.edn` at HEAD. Verified pre-existing by stashing this
row's changes and re-running: it fails identically without them. That file also
calls `(System/exit …)` at load, which terminates a whole-suite
`clojure -X:test` run, so the suite is exercised by namespace here.

## 7. Not done, stated

- **No default flipped**: `*live-doability?*` is off, `:epistemic` is 0.0.
- **No registry edit.** `:choices :mission-phase-value` is signed at `df5f7bf`
  and TN §9a forbids amending a signed entry in place. Its `:adjacent-finding`
  still reads "NOT REPAIRED HERE … it wants its own row"; that sentence is now
  out of date and the correction belongs in a superseding row written by
  whoever records J7's answer, not in this one.
- **`structural-pressure-for-action` is untouched** (`war_machine.clj:2295-2312`).
  It reads `:mission-T` off the same absent delta-t result and still takes
  `(- 1.0 0.5)` for every related mission; repairing it needs the ΔT model, not
  a phase string.
- **`compute-delta-t-mission` is not repaired.** futon3c is still off futon2's
  classpath.
- **The ten unreadable phases are not chased to their writer** — an ingest
  question, the same class as C492 4a's 266 unresolvable cross-references.
