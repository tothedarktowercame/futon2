# U44 — doability liveness: the phase the judge already fetched

**Row:** `worklist.edn :U44` (class I). **Producer:**
`holes/labs/wm-contract/u44_doability_liveness.clj`, run as

```
clojure -M holes/labs/wm-contract/u44_doability_liveness.clj \
        holes/labs/wm-contract/runs/U44-doability-liveness baseline   # once, at the pre-U44 code
clojure -M holes/labs/wm-contract/u44_doability_liveness.clj          # every arm
```

**Replay only.** No tick, no trace append, no run lock, no substrate write, no
weight or fiat table changed, no ruling written. The only substrate traffic is
the read of the `code/v05/mission-doc` family the judge already performs. The
eight EDN artifacts are byte-identical on two consecutive runs (no wall-clock
field).

## What was broken

`compute-delta-t-mission` (`war_machine.clj:2254-2263`) resolves a mission's
phase through `futon3c.aif.mission-delta-t/delta-t-mission`. futon3c is not on
futon2's classpath, so `requiring-resolve` returns nil and the `{:delta-T 0.0}`
fallback carries no `:mission-phase`. `phase-doable` (`war_machine.clj:2485-2487`)
then takes the `"unknown"` 0.3 from the fiat table for every candidate. C492
section 4b measured the date it started: `:phase` is nil on every ranked mission
row in every trace file from `wm-trace-2026-07-19.edn` onward.

Meanwhile `:mission/phase` is a prop on the same `code/v05/mission-doc`
hyperedge that `mission-doc-index` (`war_machine.clj:1480-1509`) fetches, and
which it discarded.

## The repair, and the declared input

`mission-doc-index` now carries `:phase` (`war_machine.clj:1506`);
`mission-index-phase` (`:1517-1522`) reads it; and the doability factor takes
it **only** when the declared input is on (`:2537-2539`). The input is
`*live-doability?*` / `FUTON_WM_LIVE_DOABILITY=1` (`:221-240`), or the
per-call `:live-doability?` opt (`live-doability?`, `:242-248`). Default OFF.

The delta-t reading is kept as the fallback rather than removed, so the two
carriers can still disagree in a record instead of one erasing the other, and
`:phase-source` (`:2566-2569`) says which carrier supplied the number —
`"unknown" 0.3` from an unreadable phase and 0.3 from a phase that really is
`"map"` are the same scalar and different facts. That field appears only under
the declared input, which is why the default record is byte-identical rather
than "nil in a new key".

## The numbers (133 pinned candidates, weights `{:central 0.20 :strategic 0.50 :doable 0.30}`)

The candidate set and the weights are U22's, unchanged, so the two rows are
comparable; the only thing that varies between the arms is the declared input.

| | default arm | live arm |
|---|---|---|
| `:doable` values | `{0.3 129, 0.0 4}` | `{1.0 25, 0.8 7, 0.6 2, 0.5 14, 0.4 5, 0.3 25, 0.2 35, 0.1 8, 0.0 12}` |
| distinct values | 2 | 9 |
| non-nil `:phase` | 0 of 133 | 128 of 133 |
| rank 1 | M-zaif-harness-v1, doable 0.3 | M-zaif-harness-v1, phase `instantiate`, doable 1.0 |
| ranks moved | — | **129 of 133** |

`:phase-source` on the live arm: `{:mission-doc-hyperedge 128, :unreadable 5}`.
Biggest moves: M-wm-aif-policy-grain-compliance 125 → 36,
M-sci-reproduction 116 → 34, M-sci-reproduction-replay-ledger 117 → 35,
M-omni-wm-runner 110 → 40, M-kangaroo 101 → 33 — every one of them a mission
the field marks `instantiate` or `verify`, which the inert factor had been
pricing at the same 0.3 as a mission nobody has started.

## The second consequence, which the row did not name

**The completion gate went inert on the same date and for the same reason.** It
is `(if (= "complete" phase) 0.0 1.0)` (`war_machine.clj:2545`), and a nil phase
never equals `"complete"`. In the default arm 0 of 133 candidates are
completion-gated; in the live arm 8 are, and they are exactly the 8 the field
marks complete: M-case-studies, M-essay-corpus-substrate, M-essays-edit-cycle,
M-futonzero-grounding, M-memory-retrieval, M-run-produces-its-own-brief,
M-shared-memory-control-build-test, M-typed-holes-lean-handoffs. Since
2026-07-19 those eight have been ranked as ordinary live candidates. Recorded
in `04-rank-moves.edn :gates` and pinned by control C9 and by
`war_machine_test.clj:2502-2513`.

## The cross-carrier check, re-run through the new path

C492 4b's "123 agree, 0 disagree" was measured through a `with-redefs`
substitution in U22's producer. Re-run through the shipped `:live-doability?`
path (`06-phase-agreement.edn`): **`{:agree 123, :doability-phase-absent 10}`,
zero disagreements**, against `{:doability-phase-absent 133}` on the default
path. The two carriers are two independent substrate reads of the same prop —
`mission-epistemic-value/field-readings` on one side, `mission-doc-index` on the
other — so this is a check that the two reading paths agree, not that one number
equals itself. The 10 absences split 5/5: five missions carry the literal string
`"unknown"`, which `readable-phase` refuses by name, and five carry no phase at
all. The diagnostic needs a positive `:epistemic` weight to exist, so it
declares U22's 0.15 for that arm and for nothing else.

## The default is pinned, not asserted

`05-default-pin.edn`: the flag-off ranking after the repair is compared row for
row against `00-preedit-baseline.edn`, produced by the same script's `baseline`
mode against `war_machine.clj` at sha256 `75268c35…` — the file as it stood
before the repair. **133 of 133 rows identical, 0 differing.** Passing
`:live-doability? false` explicitly gives the same judgement as passing nothing.

## Controls (`08-controls.edn`, all pass)

- **C1** the default arm is the inert state: `:phase` nil on all 133, `:doable` in `{0.0, 0.3}`.
- **C2** every live `:doable` is `phase-doability[:phase]` recomputed from the table, or 0.0 where the operator gate fires — 133 checked, 0 violations.
- **C3** the value identity `(0.20·central + 0.50·strategic + 0.30·doable) × gates × decay` recomputed per row in **both** arms — 0 violations at 1e-9.
- **C4** a fabricated mission id appears in no arm and no move.
- **C5** *negative control*: flag ON with `:phase` stripped from the index reproduces the default arm exactly — so the live arm reads `:mission/phase` and not some other carrier that happens to correlate.
- **C6** the env var bound on with no opt equals the opt passed true.
- **C7** the same repair at the **shipped default weights** `{:central 0.25 :strategic 0.45 :doable 0.30 :epistemic 0.0}` moves 128 of 133 ranks, so the result is not an artefact of the declared weights.
- **C8** 29 of the 129 movers have an unchanged `:doable` of their own — they move because rows above them moved. Reported rather than excused.
- **C9** the completion-gate finding: 0 completion-gated in the default arm, 8 in the live arm, and that set equals the set of `"complete"`-phase rows exactly.

## Not done, stated

- **The default is not flipped.** `*live-doability?*` is off and `:epistemic` is
  still 0.0. Flipping either is J7's revisit and Joe's call; the packet is
  `J7-REVISIT-PACKET.md`.
- **`structural-pressure-for-action` is untouched** (`war_machine.clj:2295-2312`).
  It calls `compute-delta-t-mission` for `:mission-T`, a different field of the
  same absent result, and repairing it needs the ΔT model and not just a phase
  string. It still reads `(- 1.0 0.5)` for every related mission.
- **`compute-delta-t-mission` itself is not repaired.** futon3c is still not on
  futon2's classpath; the row asked for the phase read, not for the dependency.
- **The five unreadable phases and the five literal `"unknown"` strings are not
  chased to their writer.** That is an ingest question, like C492 4a's 266
  unresolvable cross-references.
