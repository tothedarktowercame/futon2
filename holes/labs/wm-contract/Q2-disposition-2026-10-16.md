# Q2 disposition: outer actions and the selected task's construction (2026-10-16)

Requested by codex-68 (invoke-1792111744965-14927-13664aaf), following the
diagnosis `Q2-diagnosis-2026-10-16.md` (`962cea8fd`). Ruling: option (c),
bounded by Q8 eligibility. Branches: futon2 `claude-10-q5-family` from
`962cea8fd`; mathlib4 `darktower` from `6cd7a19a59`.

## Theory (mathlib4 `9e79af1bee`)

`Requirements.lean`:

- **`OuterAction`** `{task, action}`: one task-level action in the support
  the outer selector chose from. Its docstring says outer selection precedes
  construction, so an outer action is not a constructed policy.
- **`TargetConstruction`** gains `action`, `reading`, `library`, `graph`,
  `kernel` (pins and digests; `0` is a recorded absence) and `policies` (the
  arranged structural keys). `pool` is now "the patterns the constructed
  policies actually arrange".
- **`RunFacts.outerActions : List OuterAction`**.
- **`Q2`** (ruling recorded in its comment):
  - outer coverage: every outer action's task is in
    `frontierCensus.eligible`, every eligible task has one, there are as
    many actions as eligible tasks, and action identities are distinct. An
    open task Q8 excludes carries no action; its typed exclusion accounts
    for it;
  - the selected task (`frontierSelection.selected.task`) is eligible and is
    `selectedCascade.selectedTask`;
  - exactly one construction entry, with `targets = {selected}`, an `action`
    equal to the selected task's outer action, nonzero reading, library,
    graph and kernel, `pool = slice`, `sliceFromWholeLibrary`,
    `slice.card ≤ libraryPatternCount`, `0 < policyCount =
    policies.card`, `policies = selectedCascade.constructed`, and
    `comparedPolicies ⊆ policies`;
  - `constructorPatternCount` = the card of the union of pools.

  Q2 reads Q8's recorded fields (`frontierCensus`, `frontierSelection`,
  `selectedCascade`) and Q4's `comparedPolicies`, not the Q8 or Q4 Bool.
- **Witnesses.** `good` now has outer actions for tasks 1–3, one
  construction for task 1 (`goodConstruction`), and a selected cascade over
  its two compared policies (`goodCascade`), so Q8's and Q4's identities
  agree with Q2's. Theorems (by `decide`):
  - true: `good_Q2`, `excluded_task_without_action_Q2`;
  - false: `action_for_excluded_task`, `omitted_outer_action`,
    `duplicate_outer_action`, `shared_outer_action_identity`,
    `foreign_outer_action`, `unselected_task_constructed`,
    `wrong_selected_task`, `wrong_selected_action`, `no_construction`,
    `omitted_policy`, `invented_policy`, `duplicate_policy_counted`,
    `zero_policy_family`, `pool_not_slice`, `slice_not_from_library`,
    `reading_unpinned`, `compared_outside_construction`.

  All earlier theorems still hold, including `good_conforms`,
  `click20_violations` and every Q4/Q8 witness.
- **Build.** `lake build DarkTower.WarMachine.Requirements
  DarkTower.WarMachine.RequirementsReader DarkTower.WarMachine.OuterTaskObjective`
  succeeds; its two warnings were already there.

## Runtime (futon2)

- **Outer actions** (`q2-construction/outer-actions`, called by
  `meta-live-outer-selector/select-live` beside Q8's frontier): from the
  exact candidates the effective selection compared, each candidate the
  selector did not exclude as infeasible gets `{:type :target}` formed as
  the receipt's chosen `:action` is, and its digest. Sealed at
  `:q2-outer-actions` on the outer receipt.
- **Construction census** (`q2-construction/construction-census`, called by
  `cascade-decision` in place of the `:reading-policy-construction-census-owed`
  marker): per family in the inner field (production forms it for the
  selected task only), the reading's excerpt and analysis digests, the
  graph digest, the retraction and kernel parameters, the slice (each
  candidate pattern with the library sha256 the reading cited, now kept in
  the family provenance by `target-policy-family`), every distinct keyed
  policy (`family-selection/constructed-key`, now public, without a score)
  and every failure (the family's, plus any policy whose arrangement cannot
  be keyed). With it, the whole pattern library pinned by bytes at
  construction (`library-manifest`: every `<root>/<pattern>.flexiarg` and its
  sha256; root `:pattern-library-root`, default
  `selected-reading/default-library-root`, the root the debt validator
  pins). Sealed at `[:selection-certificate :q2-construction]`.
- **Certificate** (`q2-construction/certificate`), sealed by the runner at
  `:q2` from the record it is about to write, exactly as `verify-record`
  recomputes it. It derives the pool (patterns the keys arrange), the
  policy count (recorded keys) and slice-from-whole-library (every slice
  pin equal to the manifest's pin for that pattern) from the retained pins.
  An entry's action is the receipt's chosen action, and only for the chosen
  task's entry. Not recomputable when:
  - the outer actions or census are absent or tampered;
  - the census does not record the field the decision kept: graph pin,
    tasks, reading digests, graph digest, slice, kernel, a family failure
    missing (`:construction-failure-hidden`), a policy not in the field, or
    a field policy omitted;
  - the decision's chosen action is for another task than the receipt's,
    or its interpretation debt names another reading than the construction.
- **Abstention.** A zero-policy family abstains, and the record used to
  keep no selection certificate, so the construction and its failure were
  lost. The runner now keeps `:q2-construction` and `:target-policy-field`
  on an abstention.
- **Exporter** (`wm_run_facts/q2-facts`): on the reading route,
  `outerActions`, `targetConstruction`, `constructorPatternCount` and
  `libraryPatternCount` (the manifest pinned at construction) come only from
  a verified carrier; otherwise NR with the reason. Policy identities use the
  Q8 and Q4 exporters' `pr-str` of the key. Other routes keep the historical
  reader, with `outerActions` NR.
- **Lean term generator** (`wm_check_run_facts`): Q2 renders `outerActions`,
  the amended `targetConstruction`, and the Q8/Q4 fields it reads; an absent
  pin renders `0`.
- **Report card**: the Q2 verdict mirrors the amended Lean Q2.

## Evidence

Tests ran with `load-identity/required-sources` pointed at the worktree
(`~/notes/q2/run-tests.clj`), as the Q4/Q8 generators do.

**`wm.q2-construction-route-test`: 12 tests, 104 assertions, 0 failures,**
on the production runner seam (`run-opportunity!`, select-live in live
mode, the real reading-family decision), plus one test at select-live's own
boundary.

- **Positive** (the Q8 route's second click): carrier certified and equal to
  the one recomputed from the record. Outer actions for exactly Q8's
  eligible `M-current`, `M-other`, `M-third`; none for `M-off` (excluded,
  not on the map). The selected task's outer action is the digest of the
  receipt's chosen `:action`. One construction, for `M-current`: all four
  pins present, library of 4 patterns, slice = pool = `p/a p/b`, slice from
  the library, 2 policies equal to Q8's constructed set and containing the
  compared policy. The Lean mirror holds; the exporter emits the four Q2
  fields, the Lean term generator renders them, the report card passes.
  Run one: carrier certified, Q2 NR (Q8 has no prior frontier).
- **Real failure beside a family** (`p/a p/b p/d`, `p/d` outside the graph):
  `:seed-not-in-graph` is retained; Q2 holds.
- **An infeasible candidate** (`select-live` directly): excluded in Q8's
  census as `:automated-feasibility/infeasible`, no outer action.
- **Production cases Q2 refuses**, each carrier certified:
  - `p/a + p/c`: the retraction runs through `p/b`, which the reading did not
    name: pool `p/a p/b p/c` ≠ slice `p/a p/c`; Q2 false, report card fails;
  - a library file edited after the reading: slice not from the library;
  - no library at the configured root: library absent, slice unproven;
  - a stale reading (HEAD edited): the family is empty, the decision
    abstains, the record keeps the construction with policy count 0 and
    `:stale-target-reading`; Q2 not true.
- **Forgeries.** Resealed consistently, Lean (mirror) false: omitted,
  duplicate, foreign, excluded-task and shared-identity outer actions; a
  construction for another action; omitted, invented, duplicated and zero
  policies in the carrier's content; a pool not the slice; a library pin
  changed in the census. Not recomputable (typed reason checked): the
  receipt's action for another task; a census naming another task, omitting,
  inventing or duplicating a policy, hiding a failure, or with a stale
  reading, graph pin, graph digest, slice or kernel; a chosen action whose
  debt names another reading. Edited, unbound and absent carriers are
  refused, and the exporter then emits NR.

**Planted bugs** (`~/notes/q2/plants/plant.py`, logs beside it), each caught:

| plant | failures + errors |
|---|---|
| P1 runner does not attach `:q2` | 42 |
| P2 outer action for an infeasible candidate | 2 |
| P3 action identity not task-specific | 6 |
| P4 census drops family failures | 8 |
| P5 pool taken from the slice | 3 |
| P6 library membership without bytes | 3 |
| P7 verify accepts an unbound carrier | 2 |
| P8 hidden-failure binding unchecked | 2 |
| P9 abstention drops the census | 3 |
| P10 census records each policy twice | 50 |

**Lean.** `test/fixtures/q2-lean/generate.clj` exports through the real
runner; checked with `lake env lean`:

- `Q2First`: Q2 unverifiable;
- `Q2Repeat`: Q2 holds, as do Q4, Q5, Q6 and Q8;
- `Q2Steiner`, `Q2LibraryChanged`, `Q2ExcludedTaskAction` (the excluded
  `M-off` given an action, resealed): Q2 violated.

The committed Q4–Q8 fixtures still elaborate under the amended Lean with
their earlier outcomes. Regenerated through the current runner (scratch,
`~/notes/q2/regen`): Q4 outcomes identical; Q5, Q6 and Q8 keep their own
outcomes. Q2 now holds on `Q8Repeat` and `Q8Changed`, and the Q8 fixtures
are replaced by the regenerated ones. Q3 and Q4 also hold now in the
regenerated Q5/Q6/Q8 runs; that comes from the Q8 retained-decision and Q4
carrier commits, which those fixtures predate. The Q4, Q5 and Q6 fixtures are
left as committed.

**Affected controls,** 142 test namespaces that require a changed namespace,
branch vs base `962cea8fd`:
- branch: 1239 tests, 7202 passes, 741 failures, 43 errors;
- base: 1228 tests, 7104 passes, 741 failures, 43 errors.

The failing and erroring tests are identical (counted per test and file).
They were all there before (mostly `full-loop-runner-test`, as recorded in
the Q8 disposition). The branch adds 11 tests and 98 passes; the twelfth Q2
test was added after this run.

**Static gates.**
- clj-kondo on the changed files: 0 errors, 4 warnings, the same 4 as base.
- check-parens (`arxana-check-parens-cli`, checked to catch a planted
  unmatched bracket): OK on the 12 changed files.
- `git diff --check` clean in both repositories.

Not run: full CI, live click, hot load.

## Open

- **Run 1 is NR.** Q2 reads Q8's census and selection, which are NR until
  there is a prior frontier.
- **Library edits after a reading.** Slice-from-library compares the library
  bytes at construction with those the reading cited. Editing any cited
  pattern makes Q2 false for that task until it is read again. This is
  faithful to "the pool is the query-time slice", but it will occur.
- **Steiner points.** A retraction tree connecting reading patterns through
  a pattern the reading did not name makes pool ≠ slice, so Q2 is false for
  such families. Production does this whenever seeds are not adjacent.
  Whether the slice should include Steiner points the graph supplies, or
  retraction should be restricted to the slice, is a theory question I have
  not settled.
- **What is not proven.** That 象 queried the *whole* library is not
  recorded on the reading; what is proven is that every slice pattern is a
  member of the whole library pinned at construction, with the bytes the
  reading cited.
- **No current readings on Zone.** `/home/joe/code/storage/wm-target-readings`
  does not exist, so a live click now builds a zero-policy family and
  abstains.
- **Report card Q8** still mirrors the pre-Q8 historical breadth clauses; I
  left it.
- **Library pinned per click.** Every click hashes every `.flexiarg` file
  (about 1,431) and retains the manifest in the record.
