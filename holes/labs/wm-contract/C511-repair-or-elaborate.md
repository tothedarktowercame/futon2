# C511 — repair or elaborate: the survey over the four standing typed absences

Worklist row `:U56` (class `:D`, discovery). Spec: `EPIC-run-era.md:204-246`,
"The :incomplete response ruling — repair or elaborate (Joe, 2026-09-05)".

**What this is.** For each of the four checks that deposit `:typed-absence` on
every deposited run — `:flip-readiness`, `:per-node-runtime-validation`,
`:rationale-regret`, `:tensions-cashed` — this states what evidence the check
needs and when it must be taken, then either DEMONSTRATES that the evidence is
reconstructible from committed artifacts or REFUTES it by naming the datum that
was never captured, then names the capture point in the stepper's path where the
evidence would be taken live.

**What this is not.** No check code, no `src/`, no `scripts/` file was changed.
No ledger row was deposited and no machine run was taken; no run lock was held.
No `aif-equations.edn :choices` entry and no `control-map-edges.edn :decisions`
entry: this row measures, it does not rule. `gen_aif_dag.bb` was NOT run and
nothing was regenerated into a publish (TN §9a).

---

## 0. The precedent in the row's own premise does not say what the row says

The `:U56` statement and `EPIC-run-era.md:221-225` both cite `:contract-pin` as
"the precedent being :contract-pin's typed-absence-to-green repair", and read it
as evidence that an as-of-sha re-read can turn a past absence green. Read against
the code and the ledger, it is not that.

- `:contract-pin` on `2026-09-01-s5` is STILL `:typed-absence` (ledger seq 2), and
  the ledger is append-only: `run_era_ledger.bb:241-243` throws on a divergent row
  for an existing `(run-id, check-id)`. No past row was turned green and none can be.
- The check still has no as-of-sha mode. Its own seq-2 note says so in as many
  words, and `checks/contract_authority_current.clj:79-89` describes the deposit as
  a search of the run store, not a re-read at a sha.
- What actually changed between s5 and re5 is CAPTURE, in two parts. RE4 began
  writing a rationale record at decision time that carries the contract authority
  sha (`src/futon2/aif/selection_rationale.clj:59-63` names exactly that sha and
  exactly this reason), and the store scan was made recursive so it could see the
  `rationale/` subdirectory (`checks/contract_authority_current.clj:91-98`).

So `:contract-pin` is a worked ELABORATE that made the NEXT run green, not a
repair of an old one. Stated because the difference decides what the follow-on
rows can be: for an already-deposited run, a repair has nowhere to land.

---

## 1. `:flip-readiness` — `:repairable` on the accepted run, `:neither` on s5

### What the check needs, and when

Six lines per flip, each derived from a file read at the moment of asking
(`flip_readiness_check.bb:21-49`): the mathlib4 contract JSON plus a `git log -1`
on `Holes.lean` (`:104-120`), the futon2 accounting (`:70`), the U27 hole audit
(`:71`), the U36 catalog (`:69`), the p4ng defect tally (`:72`) and the p4ng
status receipt (`:73`). The deposit asks the run store instead and finds nothing
from this check (`:453-471`, `:477-486`, verdict at `:510-513`) — hence the absence.

The timing requirement is that the six be read at the state the run was taken at.
All six are overridable by environment variable (`:69-75`), which is what makes an
as-of re-derivation possible without touching the check.

### REPAIR — demonstrated for `2026-09-04-010-accepted`

The accepted run records `:step/futon2-sha "b1246f214315d56f6185bb810b1829115e2de5a7"`
(`holes/labs/wm-contract/runs/2026-09-04-010-accepted/step.edn:11`), and its `world-before.edn` hashes
17 cross-repo inputs including the mathlib4 contract JSON. Sourcing the six:

| line's source | as-of provenance | measured |
|---|---|---|
| U36 catalog | `git show b1246f21:…/RUNTIME-VALIDATION-CATALOG.edn` | blob at run sha == worktree; no commit since |
| accounting | `git show b1246f21:…/variable-situation-accounting.edn` | blob at run sha == worktree; no commit since |
| U27 audit | `git show b1246f21:…/U27-hole-closability/audit.edn` | blob at run sha == worktree; no commit since |
| p4ng tally | p4ng `e508ece` (last commit touching it, 2026-09-03T23:50) | no commit and no working-tree change since the run |
| p4ng receipt | p4ng `90d58c02` (last commit touching it, 2026-09-03T22:50) | no commit and no working-tree change since the run |
| mathlib4 contract | mathlib4 `4bbc7111` | sha256 `4e1feed9…` == the sha256 `world-before.edn` recorded |

Re-running the check against those six reproduces a verdict:

```
GATE: 7 flips | 0 READY | 7 BLOCKED | lines per flip: contract-pin box2-holes figure5-partials mission-gauges per-node-tests flag-chain
```

identical to the live derivation the seq-21 deposit recorded ("PASS with 0 of 7
flips READY"). The command is in §6.

**The content hash caught a wrong sha, which is what makes it a pin rather than a
label.** The first extraction used `69721b12` — the commit the run's rationale
record names as the CONTRACT AUTHORITY, i.e. the last commit touching
`Holes.lean`. The contract JSON was re-emitted in the *next* commit `4bbc7111`, so
that extraction gave sha256 `f0ecb897…` against the `4e1feed9…` `world-before.edn`
recorded. The hash rejected it. A repo sha alone would not have.

**Discrimination control.** Substituting the pre-RE7 contract emission
(mathlib4 `c4ccafed`) adds `contract-pin` to every flip's blocked-on list, so the
harness reads the overridden inputs and the answer moves with them. A second
control — substituting the s5-era accounting — does NOT move the verdict, because
`box2-holes` blocks under both; an input can move without the verdict moving, and
the one control alone would not have shown the reading is live.

**Residual, stated rather than absorbed.** Two things the record cannot settle.
(a) `:step/futon2-tree-dirty?` is `true` and it is a BOOLEAN: `tree_dirty()` at
`wm_step.sh:123` reduces `git status --porcelain` to true/false, so which files
were dirty at the tick is not recorded. The three futon2 sources are provably
unchanged by any commit since `b1246f21` and equal to the worktree today, so the
only way the run read other bytes is a dirty-then-reverted-without-a-commit path
— unlikely, and not excluded by the record. (b) The `:per-node-tests` line reads
LIVE git with no override: `moved-since-head` (`:215-226`) is
`git diff --name-only <catalog-head> HEAD` unioned with `git status --porcelain`,
both at now. That line does not appear in any flip's blocked-on list in either
derivation, so it does not decide the verdict here; it would decide it in a tree
where the other five lines pass.

### REPAIR — refuted for `2026-09-01-s5`, and not for want of a datum

s5 ran at 2026-09-01T22:54 (`runs/2026-09-01-s5/tick-run-record-…-28da19d2….edn`)
at futon2 `5a664114` — recorded in README prose only, not in any machine-written
field of that store. At that sha, four of the five futon2 sources did not exist:

| file | first commit | after s5 by |
|---|---|---|
| `runs/RUNTIME-VALIDATION-CATALOG.edn` | `c2a29647` 2026-09-03T11:57 | ~37 h |
| `runs/U27-hole-closability/audit.edn` | `a390b69c` 2026-09-03T16:22 | ~41 h |
| `runs/FLIP-READINESS.md` | `1e886ff4` 2026-09-03T18:04 | ~43 h |
| `runs/U32-flip-readiness/flip-readiness.edn` | `1e886ff4` 2026-09-03T18:04 | ~43 h |
| `flip_readiness_check.bb` | `1e886ff4` 2026-09-03T18:04 | ~43 h |

The check and most of its inputs postdate the run. There is nothing to point an
as-of derivation at, so s5's absence is not a missed capture — it is a check that
did not exist yet. The s5 store additionally names no p4ng or mathlib4 identity
at all (measured: no file in it contains the string `p4ng` or `mathlib4`).

### ELABORATE — the capture point

`wm_step.sh:239` (`bb "$RECORDS" world "$dir/world-before.edn"`), whose file list
is `wm_step_records.bb:150-172`. That list is C509 item R7's cross-repo inputs; the
six flip-readiness sources are a different set and only one of them (the mathlib4
contract, `wm_step_records.bb:167`) is on it. Adding the other five there gives
every future step a content pin for all six, at the moment before the tick, with
no new producer and no check change. `wm_step.sh:501` already copies
`world-before.edn` into the run store, so the plumbing to the store exists.

A second capture, cheaper and coarser: `wm_step.sh:123` `tree_dirty()` could
record the porcelain LIST rather than a boolean, which is what closes residual (a)
above for every check that re-reads futon2 at the run sha.

### Recommendation: `:both`

`:repairable` from the accepted run forward, because `step.edn` and
`world-before.edn` already carry enough; `:elaborate` because the repair leans on
three cross-repo files staying still, which is luck rather than a pin, and because
`:per-node-tests` has no as-of seam at all.

**Follow-on rows implied.** (i) Add the five sources to `wm_step_records.bb`'s
`world-files` and the dirty-file list to `tree_dirty`. (ii) Give
`flip_readiness_check.bb` an as-of mode that takes a world record, so the
re-derivation is the check's own code path rather than a hand-assembled env block.
Both are implementation rows; neither is this row's to write.

---

## 2. `:per-node-runtime-validation` — `:elaborate-only`

### What the check needs, and when

One input, the U36 catalog (`runtime_validation_check.bb:43`), plus resolution of
every pointer in it against the live tree (`:16-27`). The verdict is the
schema/vocabulary/pointer/flip/node validation of that catalog at the moment of
asking. The deposit asks the run store (`:192-198`, verdict at `:221-224`) and the
receipt's own `:why-the-catalog-cannot-be-run-scoped` (`:233-238`) states the
reason: the catalog's rows are keyed by node and axis, its `:test-runs` by
namespace and date, and no tick writes into it.

### REPAIR — partially demonstrated, and the gap is measured not presumed

The catalog input reproduces exactly. Its blob at `b1246f21` equals the worktree
and no commit has touched it since, and re-running the check now yields

```
COUNTS: 84 rows (59 per-node, 25 global-run) | nodes 19/19 | status exists=69 exists-but-stale=4 red=3 named-gap=8 | flips=7 | test namespaces 47/50 green | pointers=181
```

byte-identical to the COUNTS line the seq-22 deposit recorded.

The pointer-resolution leg does not reproduce, and the reason is countable. The
catalog's 181 pointers reach four repositories — futon2 157, futon3c 11, p4ng 8,
mathlib4 5 — so 24 of 181 (13%) resolve outside the one repository whose sha the
run records. For those three the accepted run's store holds, at most, one
identity each and not a repo sha: p4ng `e508ece` for `control-map-edges.edn` only
(`holes/labs/wm-contract/runs/2026-09-04-010-accepted/u49/00-source.edn:5`), a content hash for the
mathlib4 contract JSON in `world-before.edn`, and nothing at all for futon3c. A
pointer's LINE RANGE resolving today is not evidence it resolved at the run.

**The datum that was never captured: a per-repo identity for futon3c, p4ng and
mathlib4 at tick time.** Named, not presumed — `world-before.edn` hashes three
individual files across those repos and no repo head, and `step.edn` carries
`:step/futon2-sha` and no sibling.

There is also a prior question a repair cannot answer: even a perfect as-of
re-derivation produces a property of the TREE, and the check's absence is not
about staleness, it is that the catalog carries no run identity. Re-deriving it at
the run sha would give a tree property with a run-id attached — the thing
`:453-471` of the flip check and `:233-238` here both refuse to do.

### ELABORATE — the capture point

`wm_step.sh:239`/`:259` again, extended two ways: hash the catalog into
`world-before.edn` (it is a futon2 file the pin does not copy, since the pin is the
trace corpus), and record `git rev-parse HEAD` for futon3c, p4ng and mathlib4
beside `:step/futon2-sha` at `wm_step.sh:292`. `wm_step_records.bb:174-227` already
walks 16 sibling repos for the commit census, so the enumeration exists and only
the head sha is missing from what it records.

That makes the catalog's inputs as-of-able. It does not make the CHECK run-scoped,
which needs a `--as-of <world-record>` mode on `runtime_validation_check.bb` — an
implementation row, not a capture.

### Recommendation: `:elaborate-only`

Repair is refuted as a route to a green: the reproducible part (the catalog) was
never the missing part, and the missing part (a run identity in a tree artifact)
cannot be reconstructed because nothing ever wrote it.

**Follow-on rows implied.** (i) Record sibling-repo heads in `step.edn` and hash
the catalog into the world record. (ii) Decide what a run-scoped verdict for a
tree artifact even means — whether the check should deposit "the catalog as it
stood at this run's inputs" or should be removed from the per-run catalogue and
folded as a tree-era check instead. (ii) is a design question, and it belongs to
Joe or to a `:D` row, not to an implementation seat.

---

## 3. `:rationale-regret` — `:elaborate-only`, repair refuted with the datum named

### What the check needs, and when

Two things, at two different times.

- **A pair of consecutive ranking-carrying records of the same run.** The rule
  evaluates a claim projected from one record against the NEXT tick
  (`u39_selection_retrospective.bb:673-687` selects the run's records by the tick
  ids its store's receipts name; `:789-791` deposits `:typed-absence` when fewer
  than two carry a ranking).
- **An outcome leg, at some time AFTER the decision.** `:779-781` says UPHELD is
  unreachable without one; `:794-797` counts the carriers, and it is that count
  which makes the absence a measurement.

The outcome accessor is `trace-outcome` (`u39_selection_retrospective.bb:55-59`,
mirroring `scripts/futon2/report/war_machine.clj:2425-2428`): `(:outcome m)`, or
`[:enactment :outcome]`, or `[:realized-outcome :outcome]`.

### REPAIR — refuted, and the refutation has a shape worth reading

Census over the whole live corpus, 57 daily files and 889 records:

| | count |
|---|---|
| records carrying a controller ranking | 98 |
| records reaching `trace-outcome` (any of the three) | **0** |
| records carrying `:outcome` at top level | 0 |
| records carrying `:enactment` | 88 |
| … of those, with an `[:enactment :outcome]` | 0 |
| records carrying `:realized-outcome` | 88 |
| … of those, with a `[:realized-outcome :outcome]` | **0** |
| … with a non-nil `:realized-G` | 85 |
| … with a non-nil `:expected-G` | 80 |

The 0 for `trace-outcome` corroborates U51's independent measurement, quoted at
`war_machine.clj:2687-2690` ("889 records and 0 of them carrying an outcome").

**The datum that was never captured, for these runs specifically: an outcome record
on any September tick.** All 88 outcome-carrying records are in five July files
(`data/wm-trace/wm-trace-2026-07-02.edn` … `-06.edn`). The three deposited runs are
2026-09-01 and 2026-09-04. So no re-read, at any sha, of any committed artifact
produces the outcome leg for them.

**Three key vocabularies for one quantity, which is why elaborating the capture
alone would not lift the absence.**

1. The 88 recorded outcomes have shape `{:policy :expected-G :realized-G :tick}` —
   measured over the corpus, one key-set, no `:outcome` key on any of them.
2. Today's producer writes `{:policy :expected-score :realized-score :tick}`
   (`src/futon2/aif/fold_realized.clj:9`, `:96-100`).
3. `trace-outcome` reads `[:realized-outcome :outcome]` — matching neither.

So the recorded corpus is invisible to the reader, and a newly captured outcome
would be invisible too. `selection_gain/fold-realized-outcome`
(`src/futon2/aif/selection_gain.clj:197-205`) requires `:expected-score` and
`:realized-score`, so the 88 July records are also invisible to γ.

**Why the tick produces none.** `with-realized-outcome`
(`fold_realized.clj:183`) has exactly one caller, `src/futon2/aif/enact.clj:329`,
and `futon2.aif.enact` is one of the seven store-owning namespaces C509 measured
as OUTSIDE `run-tick-once`'s transitive require closure
(`C509-inter-tick-state-boundary.md:13-23`). The tick stamps itself
`:live-wire? false` at `scripts/futon2/run_tick_once.clj:277`. The flag
`*live-wire?*` (`fold_realized.clj:31-37`) has defaulted true since 2026-07-08 and
is inert here because the caller is never reached.

**And a structural absence for every future step.** `wm_step.sh` runs exactly one
tick per step (`:248-252`), `accept` deposits that one step as one run
(`:471-520`), and `run-records` filters to the run's own tick ids. So every
accepted step yields one record, no pair, and `:rationale-regret` deposits
`:typed-absence` by construction — which is precisely what seq 24 says. This is
independent of the outcome leg: fixing outcomes alone leaves it absent.

### ELABORATE — the capture points

Two, because the check has two unmet needs.

- **The outcome leg**: `scripts/futon2/report/war_machine.clj:2284-2306`
  (`write-trace-and-clock!`), the seam where RE4's rationale is written from the
  record just persisted. This is the seam that turned `:contract-pin` green, so
  the precedent is the same one, correctly read this time as a capture. The
  outcome cannot be written there at decision time — it does not exist yet — so
  what belongs there is the JOIN KEY, and what belongs in the step path is a
  later observation. Concretely: a `wm_step.sh` sub-command that, given an
  accepted step, re-observes and appends a `:realized-outcome` beside that run's
  record, run between `accept` (`wm_step.sh:471`) and `deposit`
  (`wm_step.sh:586`). A one-key reconciliation across the three vocabularies is a
  prerequisite of either.
- **The pair**: `wm_step.sh:413-465` (`cmd_battery`) runs the checks against one
  step. Either steps become multi-tick, or `:rationale-regret` scores a claim from
  step N against step N+1 — which the accepted-run pin generation
  (`holes/labs/wm-contract/runs/2026-09-04-010-accepted/step.edn:10`, `:step/pin-generation`) already orders, and which no code reads.

### Recommendation: `:elaborate-only`

**Follow-on rows implied.** (i) Reconcile the outcome key vocabulary across
`fold_realized.clj`, `war_machine.clj:2425-2428` and `selection_gain.clj:197-205`,
with the 88 recorded July records as the fixture — a repair of the READER, which
is a different thing from a repair of a run and does not touch the ledger. (ii)
Decide whether a step's rationale is scored against the next step; that is a rule
change, so it is a `:choices` question, not an implementation row. (iii) An
outcome-observation pass in the step path. (i) is the one that can be taken now
and is the only one of the three that is purely mechanical.

**Noted, out of scope, not ruled on:** `recent-non-progress-count`
(`war_machine.clj:2437-2453`) branches on `trace-outcome` and therefore treats all
889 records as non-progress. That is a live consequence of the same key mismatch
and it reaches the decay term, not just this check. It is recorded here because
this row measured it; disposing of it is not this row's business.

---

## 4. `:tensions-cashed` — `:repairable` for s5 and re5, and the semantics question survives in a different form

### What the check needs, and when

An attribution from a tension or an event to a named run. The scan
(`u41_tension_ledger.bb:467-484`) looks for three things in the committed ledger:
the run-id string, any of the run's tick ids (read off the store's receipt
filenames, `:456-465`), and any run-carrying key in the schema. The green branch
is `(or run-id-appears-in-ledger? (seq tick-ids-appearing))` (`:500-505`). The
receipt's `:why-the-ledger-cannot-be-run-scoped` (`:516-524`) records the finding
the deposits rest on: the `:event` schema (`tension-ledger.edn:528-537`) has
`:event/at`, `:event/by`, `:event/row` and no run field.

### REPAIR — demonstrated for `2026-09-01-s5` and `2026-09-04-re5`

**The row's premise that "no tension was minted during these runs" is false as of
the current ledger.** Two of the seven committed tensions are born of these runs
and cite them:

- `:wm-ladder/re5-8ae111bc-zero-support` — `:tension/born-of :refused-prediction`,
  `:tension/minted-by {:row :U52 …}`, provenance pointer
  `"…/runs/2026-09-04-re5/wm-trace-re5.edn (run 8ae111bc-d758-45f3-9c5b-f98832e10bb6)"`.
- `:wm-ladder/s5-4e35e740-zero-support` — same shape, pointer
  `"…/runs/2026-09-01-s5/wm-trace-s5.edn (run 4e35e740-8c9f-42c1-b8a9-0cdfc024e9c8)"`.

Both were minted by U52 on 2026-09-04 (ledger events seq 14 and 15), AFTER the
re5 and s5 deposits at 12:44 — which is why those deposits truthfully reported 5
tensions / 13 events and no match. The ledger now holds 7 and 15.

Replaying the check's own scan against the current committed ledger, without
depositing:

| run | run-id in ledger? | tick ids sought / appearing | green branch |
|---|---|---|---|
| `2026-09-01-s5` | true | 4 / `["4e35e740-8c9f-42c1-b8a9-0cdfc024e9c8"]` | **fires** |
| `2026-09-04-re5` | true | 4 / `["8ae111bc-d758-45f3-9c5b-f98832e10bb6"]` | **fires** |
| `2026-09-04-010-accepted` | false | 1 / `[]` | does not fire |

So the evidence the check wanted now exists in a committed artifact, for two of
three runs, and the check's existing code path finds it. That is the repair,
demonstrated. It is refuted for the accepted run: nothing in the ledger names it.

**Two things that qualify the demonstration, both measured.**

- The match is a SUBSTRING scan over the ledger text (`:476-479`), landing on
  U52's `:tension/provenance :pointers` prose. It is evidence a reader can follow
  to a specific tension, but the schema still has no run field, so any prose
  mentioning a run-id would satisfy it equally. The green means "the ledger names
  this run", not "the ledger structurally attributes a tension to this run".
- All 7 tensions are `:carried`. Zero are `:cashed`, and no `:cashed` event has
  ever been written (event types: `:carried` 7, `:evidence-added` 8). So a green
  `:tensions-cashed` row would be deposited over a ledger in which none of the
  run's tensions is cashed. The check-id names one thing and the green branch
  tests another.

### ELABORATE — the capture point

The mint payload that carries a run identity already exists and is used by nobody:
U39's tension-mint payload writes
`:tension/provenance {:records [run-id run-id]}`
(`u39_selection_retrospective.bb:418-421`), and `u41_tension_ledger.bb:448-452`
says in the code that no committed tension was minted that way. The measured
`:tension/provenance` shapes confirm it — the two run-born tensions carry
`{:who :when :pointers}`, with the run named inside a pointer STRING rather than
in a `:records` field.

So the capture point is not in `wm_step.sh` at all: it is the mint call site, and
what is missing is that a run-born mint should populate `:tension/provenance
:records` with the run-id. The place the stepper would trigger it is
`wm_step.sh:455` (the `rationale-regret` battery slot, where U39's mint payload is
produced) and `wm_step.sh:456` (the `tensions-cashed` slot, which would then find a
structured field instead of a substring).

### The semantics question for Joe — restated after measurement

The row anticipated a question of the form "no tension was minted during these
runs — vacuously cashed, or can't-see?". Measurement changes it: tensions WERE
minted from two of these runs' records, and none of the seven is `:cashed`. So the
question is not about emptiness. It is: **what does a `:tensions-cashed` green
assert?** Three readings, each already implementable:

- **(A) The run's tensions are all cashed.** What the check-id says. Measured
  per run (`u56_fold_consequences.bb`, MEASURED INPUTS): 1 tension attributed to
  s5, 1 to re5, 0 to the accepted run; 0 of them cashed, and 0 `:cashed` events
  in the whole ledger. So (A) can only speak about s5 and re5, and where it
  speaks it says NOT-all-cashed — never a green, and not until some `:cashed`
  event is written. Which non-green it says is a second question the reading
  does not settle: a `:red` (the check can see an uncashed tension) or a
  `:typed-absence` (no `:cashed` event has ever been written, so cashing is
  unobservable rather than failed). Both are folded below. The accepted run,
  with nothing attributed, is outside (A)'s scope entirely — that empty case is
  reading (C), which is why (A) and (C) are not one scenario.
- **(B) The ledger can attribute a tension to this run.** What the green branch at
  `u41_tension_ledger.bb:500-505` actually tests. Under (B) s5 and re5 are green
  today, on the evidence in §4.
- **(C) The run minted no tension, so the check is vacuously satisfied.** Speaks
  only about the accepted run, the one run with nothing attributed to it — and
  even there it is not establishable on the present schema: `:event/at` is a DATE
  (`u41_tension_ledger.bb:521-523` makes exactly this point), so "minted by this
  run" and "minted by an operator the same day" cannot be separated. Two events
  fall on 2026-09-04, the day of two of the three runs. The fold below therefore
  gives (C)'s value IF GRANTED, not a value the ledger can currently support.

**Fold consequences, computed** by applying `run_era_ledger.bb:432-451`'s rule to
the ledger's rows with the verdicts substituted (no row was written). Each
reading gets its own scenario, and which runs a reading is even in scope for is
derived from the measurement above, not assigned by hand
(`u56_fold_consequences.bb:97-105`; the attribution it rests on is measured at
`u56_fold_consequences.bb:46-63`):

| scenario | s5 | 010-accepted | re5 |
|---|---|---|---|
| S0 as-is | `:red` (selection-discrimination) | `:incomplete` (4 absences) | `:incomplete` (4 absences) |
| S1a (A), not-all-cashed ⇒ `:red` | `:red` (2 causes: selection-discrimination, tensions-cashed) | `:incomplete` (4) — outside (A)'s scope | **`:red`** |
| S1b (A), not-all-cashed ⇒ `:typed-absence` | `:red` | `:incomplete` (4) | `:incomplete` (4) |
| S2 (B) green where the ledger names the run | `:red` | `:incomplete` (4) | `:incomplete` (3) |
| S3 (C) vacuous green where nothing is attributed | `:red` | `:incomplete` (3) | `:incomplete` (4) — outside (C)'s scope |
| S4 (A)-with-a-red composed with (C) | `:red` (2 causes) | `:incomplete` (3) | **`:red`** |
| S5 all four checks green on the accepted run | `:red` | **`:green`** | `:incomplete` (4) |

Three things the split shows that the single "(A)/(C) green everywhere" line hid:

- **No reading of `:tensions-cashed` makes any run `:green`.** Only S5 does, and
  that takes all four standing absences. Settling the reading does not unblock a
  run.
- **One reading moves a run the other way.** Under S1a, re5 goes from
  `:incomplete` to **`:red`**, and s5's red gains a second cause. (A) is the
  reading the check-id asserts, so the reading that best matches the name is the
  one that would worsen the fold, not improve it. The choice is therefore not
  cosmetic even though it unblocks nothing.
- **S1b reproduces the deposited ledger exactly** (compare S0 and S1b: identical
  rows). The three deposits as they stand are already consistent with reading (A)
  read as can't-see. What they are NOT consistent with is (B), under which re5's
  row would have been green.

The choice between (A), (B) and (C) — and, if (A), between its red and its
absence — is Joe's: it is a `:choices` entry, and this row does not make it.

### Recommendation: `:both`

`:repairable` for s5 and re5 as demonstrated, subject to the caveat that the
repair cannot be DEPOSITED (`run_era_ledger.bb:241-243`; §0), so what it can
produce is a receipt and a reading, not a row. `:elaborate` because the structured
field exists and is unused, and because reading (B) is what the code tests while
(A) is what its name says.

**Follow-on rows implied.** (i) Populate `:tension/provenance :records` at the
run-born mint site, so the attribution is a field rather than prose. (ii) Once
Joe rules between (A)/(B)/(C), align the green branch with the ruling and rename
the check if it lands on (B). (iii) Independently: what a repair of an already-
deposited run's row may do at all, given append-only — see §5.

---

## 5. The finding that spans all four: a repair has nowhere to land

`run-era-ledger.edn`'s declared rules allow exactly one row per
`(run-id, check-id)` and `run_era_ledger.bb:241-243` throws on a divergent second.
So for the three runs already deposited, a demonstrated repair — §1's flip-readiness
re-derivation, §4's tension attribution — cannot become a ledger row. It can be a
committed receipt that a reader joins to the absence row by hand, and nothing more.

Three ways out, none of them this row's to choose:

1. Repairs are receipts only; the ledger keeps its absences and the fold keeps
   saying `:incomplete` for runs taken before the capture existed. Honest, and it
   means the `:incomplete` on s5 and re5 is permanent by design.
2. A repair is deposited as a NEW run-id (e.g. `2026-09-04-re5-repaired`), which
   keeps append-only intact and adds a run to the fold that was never taken.
3. The schema gains a `:row/supersedes` field, which is a change to the seam file
   and to the append API.

This is a `:choices`/`:decisions` question about the ledger's own contract. It is
recorded here because all four checks meet it, and it is stated, not answered.

---

## 6. Commands, so every number above is re-runnable

```bash
cd ~/code/futon2/holes/labs/wm-contract
# §1 as-of re-derivation of flip-readiness for 2026-09-04-010-accepted
AS=/tmp/u56-asof-010; mkdir -p $AS/futon2 $AS/U27 $AS/U32
S=b1246f214315d56f6185bb810b1829115e2de5a7
git -C ~/code/futon2 show $S:holes/labs/wm-contract/runs/RUNTIME-VALIDATION-CATALOG.edn      > $AS/futon2/CATALOG.edn
git -C ~/code/futon2 show $S:holes/labs/wm-contract/variable-situation-accounting.edn        > $AS/futon2/accounting.edn
git -C ~/code/futon2 show $S:holes/labs/wm-contract/runs/U27-hole-closability/audit.edn      > $AS/U27/audit.edn
git -C ~/code/futon2 show $S:holes/labs/wm-contract/runs/FLIP-READINESS.md                   > $AS/futon2/FLIP-READINESS.md
git -C ~/code/futon2 show $S:holes/labs/wm-contract/runs/U32-flip-readiness/flip-readiness.edn > $AS/U32/flip-readiness.edn
git -C ~/code/p4ng show e508ece:empirics-futon/defect-repair-tally.edn   > $AS/tally.edn
git -C ~/code/p4ng show 90d58c02:empirics-futon/wm-status-receipt.json   > $AS/receipt.json
git -C ~/code/mathlib4 show 4bbc7111:DarkTower/WarMachine/holes-contract.json > $AS/holes-contract.json
sha256sum $AS/holes-contract.json   # must be 4e1feed965e962dd3f4c033feaaaceeccda9890048c8e4487a65036fb3c7e5d8
CATALOG=$AS/futon2/CATALOG.edn ACCOUNTING=$AS/futon2/accounting.edn HOLE_AUDIT=$AS/U27/audit.edn \
TALLY=$AS/tally.edn RECEIPT=$AS/receipt.json CONTRACT_JSON=$AS/holes-contract.json \
FLIP_MD=$AS/futon2/FLIP-READINESS.md FLIP_EDN=$AS/U32/flip-readiness.edn \
  bb flip_readiness_check.bb --summary
# discrimination control: the pre-RE7 contract emission adds contract-pin to every flip
git -C ~/code/mathlib4 show c4ccafed:DarkTower/WarMachine/holes-contract.json > /tmp/u56-old-contract.json
CATALOG=$AS/futon2/CATALOG.edn ACCOUNTING=$AS/futon2/accounting.edn HOLE_AUDIT=$AS/U27/audit.edn \
TALLY=$AS/tally.edn RECEIPT=$AS/receipt.json CONTRACT_JSON=/tmp/u56-old-contract.json \
FLIP_MD=$AS/futon2/FLIP-READINESS.md FLIP_EDN=$AS/U32/flip-readiness.edn \
  bb flip_readiness_check.bb --summary
# §2 catalog COUNTS line
bb runtime_validation_check.bb --summary
# §3 outcome census over the live corpus (read-only)
bb u56_outcome_census.bb
# §4 tension provenance scan, no deposit
bb u56_tension_scan.bb
# §4 fold consequences under each reading, no row written
bb u56_fold_consequences.bb
# every file:line in this artifact resolves
bb u56_pointer_check.bb
```

The four `u56_*.bb` scripts are committed beside this file. They are read-only:
they take no run lock, write nothing, deposit nothing, and read only committed
artifacts plus `data/wm-trace` (§3's census). `u56_pointer_check.bb` resolves the
50 distinct `file:line` pointers in this artifact, 0 unresolved.

---

## 7. Summary table

| check | repair | elaborate capture point | recommendation |
|---|---|---|---|
| `:flip-readiness` | **demonstrated** on the accepted run (7 flips / 0 READY, identical to the deposit-time derivation); **refuted** on s5 — the check and 4 of 5 sources postdate it by 37–43 h | `wm_step.sh:239` + `wm_step_records.bb:150-172`; `tree_dirty` at `wm_step.sh:123` | `:both` |
| `:per-node-runtime-validation` | catalog leg reproduces byte-identically; 24 of 181 pointers reach three repos with **no repo identity ever captured** | `wm_step.sh:292` (sibling heads) + `wm_step_records.bb:150-172` (catalog hash) | `:elaborate-only` |
| `:rationale-regret` | **refuted** — 0 of 889 records reach `trace-outcome`; the 88 that carry an outcome are all July and under a third key vocabulary; and one tick per step means no pair, structurally | `war_machine.clj:2284-2306` (join key) + a post-accept observation pass in `wm_step.sh:471-520`; pairing at `wm_step.sh:413-465` | `:elaborate-only` |
| `:tensions-cashed` | **demonstrated** for s5 and re5 — the check's own green branch fires against the current ledger; refuted for the accepted run | the run-born mint site: `u39_selection_retrospective.bb:418-421`'s `:records` field, reached from `wm_step.sh:455-456` | `:both` |

Open for Joe: the `:tensions-cashed` reading (A)/(B)/(C) and, under (A), whether
a not-all-cashed run gets a `:red` or a `:typed-absence` — §4, where each is
folded separately and (A)-with-a-red is the one that makes re5 worse; and what a
repair of an already-deposited run may do at all, §5.
