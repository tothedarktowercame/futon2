# U59 — one outcome vocabulary, captured on the step path

Worklist row `:U59` (class `:I`), `EPIC-run-era.md`. Spec:
`C511-repair-or-elaborate.md` section 3, the `:elaborate-only` verdict for
`:rationale-regret`, refuted-with-datum.

**What this closes.** Three key vocabularies named one quantity and no reader
saw any writer, so outcome-learning had been dark since 2026-07-06. There is
now one schema, the historical spelling is read and marked as historical, the
outcome is observed on the step path one accepted step after the decision it is
about, and the pair that observation names is what `:rationale-regret` scores.

**One clause of the acceptance is NOT met and is stated in full below**: on the
demonstrating sequence the check still deposits `:typed-absence`, for a
different and now measured reason. See *What was not reached*.

---

## 1. The one schema

`:wm/realized-outcome-v1`, declared with its accessors in a new namespace,
`src/futon2/aif/realized_outcome.clj:50-140`:

    {:schema :wm/realized-outcome-v1
     :policy <id> :tick <t>
     :expected-score <n> :realized-score <n>   ; gamma's calibration pair
     :outcome <categorical>                    ; what the decay and tripwires branch on
     :scale <keyword>}

**The two numeric legs and the categorical outcome are different answers, not
two spellings of one.** That is the finding rather than a detail: the legs are
what `selection_gain.clj:174-224` folds, the categorical is what
`war_machine.clj:2425-2439` and `tripwire.clj:190` test. Unifying on either one
alone would have left the other reader exactly as dark as before. Carrying both
under one key is the unification.

`:expected-G` / `:realized-G` is the July 2026 spelling of the two legs and
nothing else. It is READ, marked `:july-2026-delta-g`
(`realized_outcome.clj:54-64`), and never written — a vocabulary is retired by
making it readable, not by rewriting the records that used it. `normalize`
(`:120-132`) copies the historical legs to their v1 names and LEAVES THE
ORIGINALS IN PLACE: a reading that deleted the bytes it read from would make the
next reader's disagreement unfindable.

Registered as a free hand: `aif-equations.edn :choices :realized-outcome-schema`,
`:status :observed-not-decided`. It states what the machine is now made to do
and rules on nothing (TN §1.3).

## 2. One accessor, not three copies

- `war_machine.clj:2425-2439` — `trace-outcome` calls
  `realized-outcome/categorical-outcome`. The second hand-inlined copy of the
  same three `get-in`s, inside `previous-selection-non-progress?`, is gone and
  calls the same function (`:2441-2456`).
- `u39_selection_retrospective.bb:41,66-69` — this script used to MIRROR
  `war-machine/trace-outcome` with a comment naming the line it mirrored. It
  now adds `src` to the babashka classpath and requires the production
  namespace, so the lab reader and the JVM reader are one file and cannot
  drift. `realized_outcome.clj` is pure Clojure with no JVM-only dependency
  precisely so that this works.
- `selection_gain.clj:174-224` — gamma reads the legs through the accessors
  instead of by key.

## 3. The post-accept observation pass

`wm_step_observe.bb` (new), called from `wm_step.sh:540` between the run store
write and the check battery, and available on its own as
`wm_step.sh observe <work> <run-id>` (`:595-599`).

**Why one step later.** A decision's outcome does not exist when the decision is
written, so it cannot be a key on the record — the seam that writes the record
and its RE4 rationale together is `war_machine.clj:2284-2306`, at decision time.
It is observed at the NEXT accept and joined by `:run/id`.

**Written into the OBSERVING run's store, never the observed one**
(`wm_step_observe.bb:227-235`). Adding a file to an already-deposited run's
store would change what its deposit receipt lists it as holding, and a replayed
deposit would then diverge for an existing `(run-id, check-id)` and be refused
by the append-only ledger (`run_era_ledger.bb:235-245`). That is the trap `:U58`
named, and control 8n asserts it did not fire.

**The two legs are one quantity measured twice**, which is the scale-match pin
gamma's contract requires (`selection_gain.clj:83-97`): `:expected-score` is the
chosen action's `:G-core` in the observed step's own ranking, `:realized-score`
is the SAME action's `:G-core` in the observing step's ranking
(`wm_step_observe.bb:84-91`). Same scorer, same units, two times. Nothing is
recomputed; both numbers are read off records.

**The categorical outcome is mission-scoped and not repo-scoped, deliberately**
(`wm_step_observe.bb:93-98`, `:186-190`). It is decided by the chosen mission's
own `:open-hole-count` on the two records — strictly down ⇒ `:grounded-change`,
from the closed vocabulary at `full_loop_cohort.clj:31`. The step's world record
(`:world/commit-census`, `:world/files`, `:world/mana-age`) is recorded beside it
as CONTEXT and is explicitly not the basis: a repo-wide census moves when anyone
commits anything, so reading a mission's outcome off it would attribute an
operator's commit to the machine's chosen action. **This is the one place the
design could most easily have produced a flattering number, and it is where the
weaker dial was refused.**

Refusals are written, not omitted (`:132-140`, `:143-165`): three typed absences
with their own reasons, so a store never merely lacks an observation.

## 4. The pair

`wm_step.sh` runs one tick per step, so a run's own records never hold two
ranking-carrying records and `:rationale-regret` deposited `:typed-absence` BY
CONSTRUCTION. `u39_selection_retrospective.bb:731-772` reads the observation,
takes the run it names, and scores across the two. **The pairing is not
inferred**: the observation record says which run it observed, and it says so
because the pin's `:pin/accepted-steps` ordered them.

`retrospective-verdict` (`:342-364`, `:365-…`) gains an OUTCOME leg and an
`:rationale-upheld` branch: upheld iff the refutation leg does not fire and the
outcome leg was observed to be a `:grounded-change`. Refutation still wins over
an upheld outcome — a rival that closed the recorded margin on its own movement
refutes the claim about the RANKING whatever the chosen action then produced.

Every new receipt key is carried ONLY when the run observed something
(`:826-834`, `:850-…`), for the ledger reason in §3.

## 5. Measured, on the sequence and on the corpus

`u59_outcome_vocabulary.clj` (new), run as
`clojure -M:test holes/labs/wm-contract/u59_outcome_vocabulary.clj 2026-09-05-u59-a 2026-09-05-u59-b`.
Artifact: `u59-outcome-vocabulary.edn` beside this file. No wall-clock field.

| measurement | value |
|---|---|
| corpus | 57 files, 889 records |
| records carrying a realized outcome | 88, **all** `:july-2026-delta-g` |
| readable by the pre-U59 key test | **0** |
| of the 88, both legs numeric ⇒ fold | **77** (11 carry a nil leg and correctly do not) |
| records carrying a categorical outcome, any of the three places | **0** |
| gamma folding the 77, from a fresh state | 1.0 → **1.3375092522591208**, mean perf 0.4195488714331731, marked `:last-outcome-vocabulary :july-2026-delta-g` |
| gamma folding the sequence's observation | **1 offered, 1 folded** (gain stays 1.0: burn-in is 5 samples, `selection_gain.clj:73`) |
| flag-off equivalence, 889 records | categorical answer **identical**, 0 disagreements; foldable 0 → 77 |
| decay on the sequence, flag off vs bound | **identical**, 0 records moved |
| decay on the live corpus, flag off | counts `{0 69, 1 29}`, decays `{0.5 29, 1.0 69}` |
| decay sensitivity (synthetic `:grounded-change` everywhere) | counts `{0 98}`, decays `{1.0 98}` — **29 of 98 ranking records would move** |

**The number the row asked for is 77, not 88**, and the difference is stated
rather than rounded away: 88 records carry a realized outcome and every one is
the July spelling, but 11 of them carry a nil `:realized-G`. Reading those would
feed gamma a leg that was never measured, so they stay unreadable and the test
`test/futon2/aif/realized_outcome_test.clj:48-52` pins that they do.

**The decay difference on the sequence is ZERO and that measures nothing**, so
the sensitivity row is there instead: it binds a synthetic `:grounded-change` to
every record to get the upper bound. It is labelled `:synthetic true` in the
artifact and is NOT a proposal. What it shows is that the 29 records carrying a
non-progress count today carry it SOLELY because no outcome is readable — the
walk reaches the previous selection of the same mission, cannot ask whether it
progressed, and counts it as non-progress (`war_machine.clj:2463-2474`).

**Not enabled.** `war-machine/*observed-outcomes*` (`:2410-2423`) defaults to
`{}`; the only `binding` of it in the repository is in this measurement, and
control 8r asserts that. Making the input readable and letting it move scoring
are two acts, and the second one is Joe's.

## 6. The sequence

Two accepted steps from one pin, work dir `/tmp/wm-step-u59`, run lock held
across each tick and released, trace/rationale/receipt redirected to the
sandbox, nothing under `data/` written.

- **step 001-u59-a** → run `2026-09-05-u59-a`, tick run-id
  `feec6327-e0b0-41fc-9697-2fc46bff2830`, tick exit 0, normalized sha
  `b2aea1c3722f99fca96a69ae7c096a55cb4b8809796afe16866508a39a2da5d8`. Its
  observation is the typed absence `:no-previous-accepted-step` — the first
  accepted step of a pin has no predecessor, and the pass says so rather than
  writing nothing (`runs/2026-09-05-u59-a/observation/realized-outcome-none.edn`).
- **step 002-u59-b** → run `2026-09-05-u59-b`, tick run-id
  `62f229b5-14f2-442b-9358-d936e1dc05a5`, tick exit 0, normalized sha
  `9b4a93f30b6b8536b74ae3da658de2c5596ab07beec342764465d7d7d11bdbda`. Its observation
  (`runs/2026-09-05-u59-b/observation/realized-outcome-2026-09-05-u59-a.edn`):
  `:observation/status :observed`, policy `M-zaif-harness-v1`,
  `:expected-score 6.879411221866079`, `:realized-score 7.013215250842563`,
  `:scale :g-core`, `:outcome :grounded-no-change` on
  `:open-hole-count 1 → 1`.
- **u39's receipt for u59-b** (`RE6-check-deposits/rationale-regret-2026-09-05-u59-b.edn`):
  `:records-of-this-run 2`, `:records-carrying-a-ranking 2`,
  **`:pairs-evaluated 1`**, `:paired-with {:runs ["2026-09-05-u59-a"]}`. The
  one-tick-no-pair absence-by-construction is dissolved.
- **determinism**: two further steps from the same pin, `003-det-a` and
  `004-det-b`, both normalized sha
  `a26d642b72f2ea783d4656157395ba9594673b4faf290fc4915b6447dff46f67`,
  `compare: identical`, 0 of 17 hashed inputs moved.

## 7. What was NOT reached, stated

**The acceptance clause "u39 producing a real (not typed-absent) retrospective
row" is NOT met on this sequence.** The deposit for `2026-09-05-u59-b` is
`:typed-absence`. The mechanism is in place and both legs are measured; the data
does not decide. The pair's verdict is `:rationale-untestable`, reason
`:overtake-attributable-to-the-chosen-candidates-own-non-progress-decay`, and
the outcome leg is `:grounded-no-change`.

**The reason the absence is now a different finding, and the note says so.**
Before this row: "fewer than two records carrying a ranking, so no pair to
evaluate and no verdict about this run exists." Now: one pair, evaluated, both
legs measured, UPHELD REACHABLE AND DID NOT FIRE. The old note would have said
UPHELD was unreachable for want of an outcome leg, which is false for this run;
`deposit-notes` was changed to say the true thing when an observation exists
(`u39_selection_retrospective.bb:876-…`).

**Why the demonstration cannot be made to land by stepping.** UPHELD needs the
chosen mission to have strictly fewer open holes at the next accepted step.
Two steps ninety seconds apart cannot close a hole, and the refutation leg is
equally data-dependent (it did fire on the recorded 2026-09-02 pair). **An arm
that read `:grounded-no-change` plus an overtake as a refutation was considered
and refused**: concluding that a rationale is refuted because a mission did not
close a hole in ninety seconds would deposit a red the record does not carry,
which is the failure the existing rule's `:refused-at-mint` arm exists to avoid.
So the row is left `:blocked` on this clause rather than reported as met.

**Also not done, and not attempted:** `wm_step.sh deposit` was NOT run, so no
run-era ledger row landed for either U59 run; the battery's first-pass receipts
are committed as evidence and are undeposited. `gen_aif_dag.bb` was not run and
nothing was regenerated into a publish (TN §9a). No
`control-map-edges.edn :decisions` entry. The three deposited runs are
untouched.

## 8. An audit fact the reviewer needs

**`scripts/futon2/report/war_machine.clj` is not in this row's commit.** At
2026-09-05 05:39:22 UTC a background `inbox-zero` sweeper committed the file out
from under the edit, as `a491b2d9 "inbox-zero: promote 1 path(s) for claude-1"`
— a message that names neither U59 nor what changed. The content at that sha is
the final content (the tree is clean against it and the tests and controls were
run against it), so nothing half-finished shipped; but a reader looking for the
`trace-outcome` change in this row's commit will not find it, and a reader of
`a491b2d9` will not learn why it was made. Recorded here because a ledger that
quietly loses a wrong attribution is worth less than one that shows it. This is
the shared-checkout hazard TN §9a describes, in the other direction: there, one
seat's `git add` sweeps another's half-finished work into its own commit.

## 9. Gates

- clj-kondo **0 errors 0 warnings** on each of `realized_outcome.clj`,
  `selection_gain.clj`, `war_machine.clj`, `realized_outcome_test.clj`,
  `wm_step_observe.bb`, `u39_selection_retrospective.bb`,
  `u59_outcome_vocabulary.clj` — **linted separately**. Linting
  `u59_outcome_vocabulary.clj` together with `src scripts` raises the repo-wide
  count from 74 to 76, and both extra warnings are `duplicate require` reports
  against `scripts/p3_retry_enriched.clj` and `scripts/wm_full_loop_cohort.clj`,
  pre-existing files this row does not touch — the same cross-file artefact
  `:U57` recorded.
- `futon4/dev/check-parens.sh` OK on all seven.
- `bash -n` OK on `wm_step.sh` and `negative_controls.sh`.
- `negative_controls.sh` **PASS, 46 negative / 28 positive** (8n–8s added).
- `pointer_check.bb` **1357 pointers / 0 unresolved** (1342 before this row).
- Tests: `futon2.aif.realized-outcome-test` 5/34, `futon2.aif.selection-gain-test`
  28/115, `futon2.aif.fold-realized-test` 6/51, `futon2.report.war-machine-test`
  97/546 — 0 failures, 0 errors in each.
- `r6_zero_post_preflight.clj` PASS before the first step (0 POSTs, 0
  `.admintoken` reads, 1663 paths read).
- `run_era_ledger.bb --check` green after each accept; `worklist_check.bb`
  exit 0.
