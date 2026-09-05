# C533 — `:F9`: the cascade lane wired to the decision

**Row:** `worklist.edn :F9` (`:class :F`, `:owner :any`, epic `EPIC-run-era.md`).
Successor to `:I4` leg 1, whose acceptance C475 §6 found unreachable as written
and re-stated as this row's build. `:F7` built the construction and the scoring;
this row connects them to the tick's committed decision.

**Bar (the row's own `:acceptance`):** target-equality test green on a stepped
run, red-by-construction before, shown both ways; the advisory lane on without
breaking the default tick, with a flag-off control; no selection or enactment
claim beyond what runs; clj-kondo 0/0, check-parens OK, relevant tests green,
bare exits.

---

## 1. What was wrong, stated as a measurement rather than a reading

`cascade-lane`'s own docstring said entry #1 is "the judge's actual top
decision … so the gate checks what the machine decided". The code it documents
took `(first ranked-actions)` (`decision-entry`,
`futon2/scripts/futon2/report/cascade_lane.clj:437`), which is the head of the
**ranking**. C474 §1 and C475 §3 established that from the call graph — at
futon2 `2e0f2cc4` `cascade-lane` was called at
`futon2/scripts/futon2/report/war_machine.clj:6396` over `wm-ranked`, and
`policy/select-action` was not reached until `:6449`.

The measurement C475 did not make, and this row did: **over the 48 recorded
S1b/S2/S4/S5 ticks the two targets disagree 48 times out of 48.** The ranking
head is `M-expressions-of-interest` on every one of them; the committed
decision is `M-aif-policy-conditioned-eig` or
`M-wm-aif-policy-grain-compliance`, alternating. Receipt:
`runs/F9-cascade-decision/00-corpus-target-gap.edn` (`:rank1-equals-decision 0`,
`:record-count 48`). So this was not a latent mis-wiring that happened to agree
in practice — had the lane been on, it would have gated the wrong mission on
every recorded tick.

The same receipt records the other half: `:error/no-cascade-constructed` 48/48,
because `:include-advisory-lanes?` was `false` at futon2 `2e0f2cc4`
`futon2/scripts/futon2/run_tick_once.clj:255`. That is
the "red 24/24 BY CONSTRUCTION" the row's `:statement` names, extended to 48.

## 2. What changed

**`cascade_lane.clj`.** `decision-entry` gains a 2-arity taking the decision
explicitly (`futon2/scripts/futon2/report/cascade_lane.clj:421-441`), and
`cascade-lane` gains a `:decision` option that supplies it (destructured at
`futon2/scripts/futon2/report/cascade_lane.clj:460`, read at `:500`). Omit it
and the behaviour is the pre-`:F9` one, byte-for-byte: the 1-arity still reads
rank-1. The `:open-mission` exclusion
(`futon2/scripts/futon2/report/cascade_lane.clj:440-441`) and the `already-in?`
dedup (`futon2/scripts/futon2/report/cascade_lane.clj:502-503`) apply to a
supplied decision exactly as they applied to rank-1, so a decision target that
is already in the open-mission side-stream yields one entry, not two.

**`war_machine.clj`.** The `cascade-policies` / `cascade-actions` /
`wm-ranked+cascades` bindings moved from above `select-action` to below
`wm-decision` (which binds at
`futon2/scripts/futon2/report/war_machine.clj:6506`; the moved block is
`:6545-6588`), and the lane is now called with `{:n 3 :budget 6 :decision
wm-decision}` (`futon2/scripts/futon2/report/war_machine.clj:6569-6571`).
Nothing else moved: the three later readers of `wm-ranked+cascades` were
already below `wm-decision` — the mission-C readback
(`futon2/scripts/futon2/report/war_machine.clj:6658`), `:ranked-actions`
(`futon2/scripts/futon2/report/war_machine.clj:6732`) and `:cascade-policies`
(`futon2/scripts/futon2/report/war_machine.clj:6741`).

**`run_tick_once.clj`.** `:include-advisory-lanes?` `false` → `true`
(`futon2/scripts/futon2/run_tick_once.clj:266`).

### 2a. The β / F_π placement, which C475 §6.2 required be settled first

The one binding that read `wm-ranked+cascades` *above* the decision was the
RUN8/S3 pair — `f-pi-dark-readback` and `beta-dark-carry`. They cannot move down
with the cascade block: under `FUTON_WM_TAU_MODE=variational-beta-gamma` the
solved β **is** the selection temperature, so they have to be computed before
selection. They therefore had to be re-pointed, and the settlement is that they
run over **`wm-ranked`** (`futon2/scripts/futon2/report/war_machine.clj:6400-6428`):

- **The pool β is solved over should be the pool selection ranges over.**
  `wm-admissible` — what `select-action` receives, and what
  `f-pi-posterior-opts` already joins against — is filtered from `wm-ranked`
  (`futon2/scripts/futon2/report/war_machine.clj:6433`). Cascade rows are
  `:held-for-arming? true`
  (`futon2/scripts/futon2/report/war_machine.clj:6587`) and are never selectable
  on any tick, so a β solved with them in the pool is a temperature for a field the
  selector does not see.
- **It removes the cycle C474 §5 warned of.** With the lane constructed for the
  committed decision, a β solved over cascade rows would be a temperature that
  depends on the decision it sets the temperature for. Prospective before this
  row; actual the moment the block moves.
- **The S2/S3 β series stays comparable by identity, not by assertion.** Every
  recorded tick ran with the advisory lane off, so `cascade-actions` was always
  `[]` and `wm-ranked+cascades` was `wm-ranked` with the ranks re-asserted. The
  field the readbacks now name is the field those runs actually used.

Only `beta-dark-carry` was ever sensitive to the difference
(`futon2/scripts/futon2/report/war_machine.clj:591-593` passes `current-ranked`
to `carry-beta` with `:score-fn :controller-score`, and cascade rows carry one
at `futon2/scripts/futon2/report/war_machine.clj:6582`). `f-pi-dark-readback` uses `current-ranked` for identity
matching and two provenance counters, and a previous-tick cascade row carries no
`:prediction-mean`, so its per-candidate values could not have moved either way.

## 3. The check, and why counting is not it

`f9_cascade_target_check.bb` asks C475 §6.4's question of a committed record:
does the cascade the tick CONSTRUCTED belong to the target it COMMITTED to? Both
are in the record — `:decision :action :target` and the lane's output — so no
live machine is needed to re-run it.

**A defect in the checker, found by running it against a real record rather than
by reading the code.** The first version read `:cascade-policies` and reported
`:error/no-cascade-constructed` on the wired run, which HAD constructed a
cascade. `futon2.aif.trace/trace-record` is an explicit projection and does not
carry that key; what a trace record carries is the `:apply-cascade` rows the
lane's output was lifted into, inside `:ranked-actions`. The checker reads both carriers now
(`futon2/holes/labs/wm-contract/f9_cascade_target_check.bb:78-101`) and each
result says which one answered. This is worth naming
because the wrong version was green on nothing and red on everything, which is
the failure shape that looks like a working check.

Three ways to be green without having wired anything, all refused:
`:error/no-cascade-constructed` (nothing built — vacuous),
`:error/target-absent` (built for another mission), and
`:error/target-cascade-not-constructed` (the right target on an entry with no
patterns and no score — a label, not a cascade), at
`futon2/holes/labs/wm-contract/f9_cascade_target_check.bb:125-128`. An empty
input is a failure, not a 0/0 pass
(`futon2/holes/labs/wm-contract/f9_cascade_target_check.bb:143`).

`--rank1` asks the same question of the ranking head, so the two modes measure
the substitution this row makes rather than asserting it.

## 4. The runs

Three stepped runs in `data/wm-step/w1`, each from the same pin, each holding
the live run lock (RUN12), each writing only into the sandbox. The R6 pre-flight
was run first: 0 POSTs, 0 `.admintoken` reads.

| step | code | lane | target check | normalized sha |
|---|---|---|---|---|
| `016-f9-base-flagoff` | HEAD `2e0f2cc4` | off | **FAIL** `:error/no-cascade-constructed` | `dd2463575149…` |
| `017-f9-wired-flagon` | wired | on | **PASS** `ok 1/1` | `e95e8b1900b7…` |
| `018-f9-newcode-flagoff` | wired | off | (control) | `98cb1d8861ec…` |

The wired tick constructed a cascade for `M-zaif-harness-v1`, the mission it
committed to — 3 patterns, `:cascade-score -0.277`, appended at rank 147 with
`:act-gate :pass? false`. Receipts `01-step-016-before.edn`,
`02-step-017-after.edn`, and the records themselves, in
`runs/F9-cascade-decision/`.

### 4a. What the live run does NOT discriminate, said plainly

On this pinned world the ranking head and the committed decision are the **same
mission**, so step 017 would have been green under the old wiring too. The live
run establishes that the wiring works end to end — the lane runs below the
decision, the constructor is reached, the record carries the cascade — and it
does not by itself distinguish the two sources. The discrimination is elsewhere
and is deliberate: §1's 48/48 corpus gap, control 15f
(`p4ng/empirics-futon/negative_controls.sh:1847-1855`, which plants a record
where the two disagree and shows `--decision` refusing while `--rank1` passes),
and the six unit tests in
`futon2/test/futon2/report/cascade_lane_decision_target_test.clj:51-103`, whose fixture is
built so the two targets differ. Neutering the wiring reddens 6 of those 6.

### 4b. The flag-off control, and the one field that cannot be identical

016 and 018 differ at **exactly one path**, `[:wm-version :git-dirty?]` —
`false` in 016, `true` in 018, because 016 ran before the edit and 018 after it.
Every quantity the tick computes is equal. Receipt:
`03-flag-off-control-compare.edn` (the stepper's own comparator, which already
excludes `:startedAt`, `:timestamp`, `:run/id` and the route `:at`/`:at_`).

Full byte-identity across a code change is not available and it is worth saying
why rather than reporting a near miss: the record stamps the code that produced
it (`:wm-version :git-sha`, `:git-dirty?`). A post-commit control would have
diverged at `:git-sha` instead. Both steps here carry the same `:git-sha`, so
the dirty flag is the only stamp that could move, and it is the only thing that
did.

`world-drift` reports 1 of 17 hashed inputs moved between the two steps — the
mana snapshot, whose age advanced 3.97 → 2.87 min; the commit census is
unchanged at 3989 (`04-flag-off-control-world-drift.edn`).

## 5. What this row does not claim

- **Nothing is selected or enacted.** The cascade rows are appended after
  `wm-decision` is final and are `:held-for-arming? true`
  (`futon2/scripts/futon2/report/war_machine.clj:6587`); `wm-admissible` is
  filtered from `wm-ranked` (`futon2/scripts/futon2/report/war_machine.clj:6433`)
  and never sees them. The wired run's own act gate
  reads `:pass? false`. Execution remains Part B / operator arming.
- **No ruling.** `aif-equations.edn :choices` and `control-map-edges.edn
  :decisions` are untouched. In particular the R6-C-vs-R6-Q(π) fork C475 §6
  leaves with Joe is not resolved here, and this row does not need it: the
  acceptance test is the target equality, not a route hop.
- **No `FUNDAMENTALS.edn` verdict moves.** The row's acceptance permits moving
  limits "only with evidence", and the evidence this row produced is about
  *which target the lane builds for*, which is not a fundamental's subject.
- **Nothing regenerated into a publish** (TN §9a): `gen_aif_dag.bb` not run.
- **The 2026-07-06 operator ruling is still registry-invisible.** C475 §5's
  finding — it lives only in a `def`'s docstring — stands. This row implements
  it at the right step; recording it in the registry is a ruling and is Joe's.

## 6. Gates

- clj-kondo 0/0 on `war_machine.clj`, `cascade_lane.clj`, `run_tick_once.clj`,
  `f9_cascade_target_check.bb` and the new test.
- `check-parens` OK on all three source files.
- `run-tick-once-test` 11 tests / 35 assertions, 0 failures 0 errors.
- `futon2.report` namespaces 106 tests / 573 assertions, **1 failure**, and it
  is pre-existing and not this row's: `mission-c-readback-hashes-the-criteria-source-test`
  pins the sha256 of `holes/missions/M-wm-aif-policy-grain-compliance.md`, which
  commit `27a6dd5b` (2026-09-05, ":B2 adoption") edited from `51f6de53…` to
  `a770d000…`; the pin is at
  `futon2/test/futon2/report/war_machine_test.clj:1715-1724` and the assertion
  at `futon2/test/futon2/report/war_machine_test.clj:1746`. The test's own docstring says a failure here "is NOT a code
  fault" and prescribes re-measuring U12 and updating artifact and pin together
  — another row's work, and touching the pin alone is what that docstring
  forbids.
- `negative_controls.sh` PASS, 125/51 → **133 negative / 53 positive**, section
  15 (`p4ng/empirics-futon/negative_controls.sh:1803-1873`). Three of the nine were mutation-tested, each against the specific defect
  it defends: neutering the `constructed?` guard reddens 15c, dropping the
  vacuity guard reddens 15e, and reading only the `:cascade-policies` carrier
  reddens the positive control 15i. The suite was PASS at 125/51 before the
  section was added, measured rather than quoted.
- `test/futon2/aif` 1155 tests / 10343 assertions, **6 failures / 9 errors**,
  all pre-existing and none of them reachable from this change: 17 assertions in
  `actuator_a3_test.clj`, 16 in `fold_realized_test.clj` and 6 in
  `fold_escrow_test.clj`, all from futon6 fold-turn deposits and pin-1B prompt
  reconstruction. None of the three namespaces requires
  `futon2.report.war-machine`, `futon2.report.cascade-lane` or
  `futon2.run-tick-once` — checked, not assumed; the only occurrence of the
  string in any of them is a fold-turn id at
  `futon2/test/futon2/aif/fold_escrow_test.clj:153`. The same 6/9 was the state
  recorded by `:F1` slice 2 on 2026-09-05, before this row started.
- `pointer_check.bb` 1693 pointers in 5 files, 0 unresolved;
  `u56_pointer_check.bb` 26 pointers in this note, 0 unresolved.
