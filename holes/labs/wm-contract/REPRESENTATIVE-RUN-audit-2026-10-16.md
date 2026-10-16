# Final bounded representative run: requirement audit (2026-10-16)

Requested by codex-68 (invoke-1792117378277-15025-a497063b) after the ordered
Q7→Q5→Q6→Q8→Q4→Q2→Q9→Q10 repairs (futon2 `1074eaff6`, mathlib4
`9e79af1bee`). Branch: futon2 `claude-10-q5-family`. Lean unchanged.

## The run

`test/fixtures/representative-run/run.clj` runs three consecutive production
clicks through `run-opportunity!`. This is the seam the Q8 and Q10 route
tests use: select-live outer selection, the real reading-family decision
for the selected task, and one selector call per run. Stores and traces
are hermetic and the learning ledger is empty. There is no hot load and no
live click.

**Reading.** The reading cites p/a in one fragment, then p/a and p/b over
one span. The selected task's construction then holds four policies:
- a chain over {p/a} (the reading's alternatives);
- three arrangements of {p/a, p/b}: the reading's precedence chain
  (alternatives), its overlap reading (overlap mode, p/a twice), and the
  retraction's overlap.

I found this reading by probing `target-policy-family/policy-family`
(`~/notes/q10final/family-probe.clj`). The earlier p/a p/b reading gives
only two arrangements of one pattern set. Q6 then removes one of them on
the repeat, so Q10 could not hold on any click where Q8 holds.

**Outputs.** For each record the harness writes these to
`test/fixtures/representative-run/out/`:
- carrier verification for Q2, Q8, Q9 and Q10, and the `:q10` census
  (`RunN/summary.edn`);
- the export from `wm-run-facts/facts-for-record` (`RunN/run-facts.json`);
- the report card from `wm-report-card/generate!` (`RunN/card/report-card.md`);
- the Lean source from `wm-check-run-facts/lean-source` (`RepRunN.lean`).

The report card is generated with an empty preregistration root and an empty
snapshot, so nothing outside the fixture is read. Q1 and the other
snapshot-dependent fields come from the fixture's world. The card's `.edn`
and `.html` renderings are deleted before commit; they are reproducible.

## Defects found and repaired

The first attempt did not produce a record. Repairing the run surfaced three
runtime defects that are not experimental work. Each was repaired
structurally, tested, and checked with a planted bug.

1. **The decision gate refused the selector's Bayes action**
   (`decision_gate.clj`, `first-acting-pattern`).
   - Since `7c2fd649a` the selector keys a co-application cascade by its
     root frontier, so cascades whose roots coincide pool their mass.
   - The gate's independent recomputation still took
     `(first (:precedence action))`. For a `{:co-apply …}` map that is the
     whole `[:co-apply {...}]` entry, so each cascade got its own key and
     the pooled mass was split.
   - On this reading, the chain over {p/a} and the {p/a, p/b} chains share
     p/a's root. Both clicks threw `:chosen-not-bayes-action` (chosen 0.166,
     best 0.417) and recorded `:no-chosen-action`.
   - The gate now computes the root frontier itself, still without sharing
     the selector's code, as its own comment requires.
   - `gate-and-selector-agree-on-what-counts-as-an-action` now includes
     co-apply chains, independent units, a partial descent and empty units.
2. **A reached selection was recorded as `:selection-not-reached`**
   (`full_loop_runner.clj`).
   - Q5 holds a selected reading policy until its interpretation debt
     clears. The decision then travels on the `:no-selection` sorry cell,
     and the record's `:decision` reads it from there (`retained-decision`).
   - `:selection-event` still read only the judgment cell, so every
     reading-family click refused downstream recorded
     `{:status :absent :reason :selection-not-reached}`.
   - Q7's exporter counts that absence, giving `pathAbsenceCount = 1` and
     Q7 violated on every such click. This is why the earlier Q10 fixtures
     show Q7 violated.
   - It now reads `retained-decision`. Abstentions are unchanged.
3. **Q2, Q9 and Q10 read "the selected policy" from the posterior's mode**
   (`:per-policy-argmax`).
   - The selector chooses by the action marginal, and the selector's own
     comments say this can overrule the mode. It did so here: run 1 chose
     the {p/a} chain (pooled marginal), while the mode was the retraction
     overlap.
   - Q10's census recorded the wrong `:selected`, and Q9's carrier the wrong
     `:selected` policy.
   - Q2's target and reading checks used the mode's interpretation debt.
     That gave the same result here only because all of a task's policies
     share one reading.
   - `q2/chosen-action` now resolves the record's `:chosen` (id and target)
     to its compared posterior entry. Q9 and Q10 use it.
   - A `:chosen` that names no compared entry is a new Q2 reason,
     `:chosen-action-not-compared`. Q10 carries it, and Q9 reports
     `:selected-policy-not-compared`.
   - Until fix 1, the gate refused every decision in which mode and choice
     differ, so no record had shown this.

**Tests.** `wm.q10-arrangement-census-route-test` gains
`a-shared-root-reading-keeps-a-control-through-q6`, which runs the three
production clicks. It checks:
- one selector call per click;
- each click passes the gate and is recorded as `:cascade-selected`, with
  Q7 passing;
- the census's selected policy is the chosen one, not the mode, and Q9
  agrees;
- the run-by-run counts and verdicts in the table below.

Forgeries moved from the mode to `:chosen`:
- Q9: chosen names a foreign policy → `:selected-policy-not-compared`.
- Q10: run 2's choice set to run 1's, which Q6 excluded, with Q2 resealed
  → `:chosen-action-not-compared`.
- Q2: the chosen posterior entry's analysis digest is made stale →
  `:construction-reading-not-the-chosen-action's`.
- Q2, new: chosen names no compared policy → `:chosen-action-not-compared`.

`reading-route/fixture` accepts a vector of pattern ids for a fragment that
cites several. A fragment with a single pattern keeps byte-identical text.

**Planted bugs** (`~/notes/q10final/plants/plant.py`, logs beside it):

| plant | tests run | failures |
|---|---|---|
| G1 gate keys co-apply by its container | q10 route + decision-gate | 29 |
| R1 `:selection-event` from the judgment cell only | q10 route | 9 |
| S1 chosen read from the posterior's mode | q10 route | 9 |
| S2 `:chosen-action-not-compared` unchecked | q10 route + q2 route | 4 |

The old gate code with the new gate test: 6 failures.

## Results

Three runs on one stream. Q6 exclusion on each run is the previous run's
refused choice.

| | run 1 | run 2 | run 3 |
|---|---|---|---|
| run id | `…d0fe29b4…` | `…f25a86d2…` | `…dab4da9f…` |
| chosen | {p/a} chain `0d5ed…` | retraction overlap `b9951…` | {p/a} chain `0d5ed…` |
| Q6-excluded | none | `0d5ed…` | `b9951…` |
| compared | 4 | 3 | 3 |
| Q10 pairs over {p/a, p/b} | 3 controls | 3 controls | 1 control, 2 refused (`:member-not-compared`) |
| Q10 counts | 3, 3 | 3, 3 | 1, 1 |
| selector calls | 1 | 1 | 1 |

The G values of the {p/a, p/b} arrangements are 1.749 (reading chain),
1.237 (overlap reading) and 2.157 (retraction overlap). Each value is that
policy's own candidate row and matches its family summary, and every pair
differs.

**Lean** (`lake env lean`, run from mathlib4 `9e79af1bee`, exit 0 for
each). `alert` lists the requirements that do not hold:

| file | alert |
|---|---|
| `RepRun1.lean` | q2 unverifiable, q6 unverifiable, q8 unverifiable, q9 violated |
| `RepRun2.lean` | q9 violated |
| `RepRun3.lean` | q9 violated |

**Report card** (`wm-report-card`, verdict rows):

| | Q1 | Q2 | Q3 | Q4 | Q5 | Q6 | Q7 | Q8 | Q9 | Q10 |
|---|---|---|---|---|---|---|---|---|---|---|
| run 1 | pass | NR | pass | pass | pass | NR | pass | NR | fail | pass |
| run 2 | pass | pass | pass | pass | pass | pass | pass | NR* | fail | pass |
| run 3 | pass | pass | pass | pass | pass | pass | pass | NR* | fail | pass |

\* By design the card never decides Q8 ("Nested certificate validity
requires the generated Lean checker"). Lean decides it, and it holds on
runs 2 and 3.

## Audit, requirement by requirement

The deciding records are runs 2 and 3, where every prior-state requirement
has its prior.

- **Q7, holds** (runs 1–3).
  - Carrier: the record's absence census under `:decision`,
    `:selection-event`, `:interpretation-ask`, `:terminal-receipt` and
    `:failure` (`wm-run-facts`, `absence-paths`).
  - `pathAbsenceCount = 0`, and the card and Lean agree.
  - Before repair 2 this was 1 on every reading-family click.
- **Q5, holds** (runs 1–3).
  - Carrier: the decision's interpretation debt and hold.
  - `interpretationOrder = selectionBeforeInterpretation`. Only the selected
    policy owes interpretation, and its debt holds the run (outcome
    `refused`).
- **Q6, holds** (runs 2–3); run 1 is unverifiable because there is no
  earlier run.
  - Carrier: `previous-run` terminal and predecessor, and the decision's Q6
    exclusion.
  - The previous choice was refused on the same input digest
    (`c27093bd…` on both sides), was excluded, and a different policy was
    chosen.
- **Q8, holds** (runs 2–3, Lean); run 1 is unverifiable (`:no-prior-frontier`).
  - Carrier: `:q8`, `:wm/q8-frontier`, verified by `q8/verify-record`.
  - The policy stages equal those the Q10 census reads.
- **Q4, holds** (runs 1–3).
  - Carrier: the certified preference receipts. Every compared policy has
    risk, ambiguity and information terms (4/4, then 3/3), and the horizon
    and steps are graded.
- **Q2, holds** (runs 2–3).
  - Carrier: `:q2`, certified and verified.
  - Run 1 is unverifiable. The carrier is certified, but Q2 reads Q8's
    frontier census and selection, and the Q8 carrier certifies those only
    together with the refresh, which needs a prior frontier.
  - This is the limitation the Q2 disposition lists as open. I did not
    change it here: it is a question of how the Q8 carrier groups facts,
    not a false record, and Q2 holds on the deciding runs. Making Q2
    decidable on a first click would mean certifying census and selection
    apart from the refresh.
- **Q9, truthfully violated** (runs 1–3).
  - Carrier: `:q9`, `:factorization-absent`, verified.
  - The production C has no relevance coordinate, completion is not
    lexicographically prior, and there is no calibration or factorization
    authority. So `factorizedPreference = none` and both authorization
    lists are empty.
  - This is ruling (a) in `Q9-disposition-2026-10-16.md`. Making Q9 true is
    experimental. It needs an externally commissioned calibration and
    factorization authority, and a selection-law ruling that puts
    completion first and adds a relevance coordinate. Neither exists, and
    none was fabricated.
- **Q10, holds** (runs 1–3).
  - Carrier: `:q10`, `:counted`, verified.
  - Run 2 has 3 distinguished controls beside a certified Q8. On run 3, Q6
    excludes a member of a control, its two pairs are refused, and one
    control remains (1, 1).

**Classification.**
- **Hold:** Q2, Q4, Q5, Q6, Q7, Q8 and Q10, on the deciding records.
- **Violated under current production semantics:** Q9 only.
- **Experimental, needing new authority or a ruling:**
  - making Q9 true (above);
  - the Steiner question from Q2, which decides how often production forms
    Q10 controls;
  - whether "distinguished by G" requires different values. They differ
    here, so the verdicts do not depend on it.
- **Nonexperimental defects remaining in the representative record:** none.
  The three found are repaired above. The first-click NR of Q2, Q6 and Q8
  comes from their definitions or from the Q8 carrier's grouping, not from
  a false record.

**Observation.** Q5 debt is never cleared in a hermetic run. Q6 excludes
only the immediately previous refused choice, so the stream alternates
between the two refused policies (`0d5ed` → `b9951` → `0d5ed`). This is what
Q6 as stated allows. Whether a choice refused two runs back should also be
excluded is a question for Theory, not something this audit settles.

## Commands

```
# worktree ~/worktrees/futon2/claude-10-q5-family
clojure -M:test test/fixtures/representative-run/run.clj      # REP_OUT defaults to test/fixtures/representative-run/out
rm test/fixtures/representative-run/out/Run*/card/report-card.{html,edn}
cd ~/code/mathlib4 && lake env lean <worktree>/test/fixtures/representative-run/out/RepRun{1,2,3}.lean
clojure -M:test ~/notes/q9/run-tests.clj futon2.aif.wm.q10-arrangement-census-route-test \
  futon2.aif.wm.q2-construction-route-test futon2.aif.wm.q9-factorization-route-test \
  futon2.aif.wm.q8-frontier-route-test futon2.aif.decision-gate-test
~/notes/q10final/run-focused.sh <worktree> <log>                # focused, branch vs base
```

(`~/notes/q9/run-tests.clj` points load-identity's required sources at the
worktree, as the earlier dispositions did.)

## Evidence

- Q10 route test alone, final: 9 tests, 167 assertions, 0 failures.
- Focused branch vs base (`~/notes/q10final/focused.txt`: the Q10 list plus
  every namespace that reads `:selection-event`, the gate rule or the
  chosen action; base `1074eaff6` in `~/worktrees/futon2/q10final-base`):

  - branch: 410 tests, 3099 passes, 39 failures, 3 errors;
  - base: 409 tests, 3053 passes, 39 failures, 3 errors.

  The failing tests are identical on both sides
  (`~/notes/q10final/fails-{base,branch}.txt`). All of them are in
  namespaces this list adds (measured-a decision, token-belief carry,
  uniform run record, habit-reinforcement runner, bad-case rates, the
  existing Lean checker's process call), and none is in the Q2, Q8, Q9 or
  Q10 route tests or the gate test.
- **Static:**
  - clj-kondo shows no new warnings. `full_loop_runner` keeps its warning
    from base, and the harness has the three unresolved-namespace warnings
    the in-ns script pattern always gives.
  - check-parens: OK on all 11 changed files.
  - `git diff --check`: clean.
- **Not run:** full CI, a live click, a hot load. The committed Q2, Q8, Q9
  and Q10 Lean fixtures were not regenerated. Their Q7 verdict
  ("violated") predates repair 2.
