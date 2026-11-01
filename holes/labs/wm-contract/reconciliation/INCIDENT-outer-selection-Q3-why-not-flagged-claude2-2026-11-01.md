# Major incident, outer selection: why it was not flagged (claude-2, 2026-11-01)

Joe: "this seems to be a total violation, so we must see why it was not flagged as such."
Companion to INCIDENT-outer-selection-Q1-Q2-claude2-2026-11-01.md. Sources: git history of
futon2 and mathlib4 (commit messages with agent trailers), the 2026-10-28 notes in
wm-contract, `~/notes/delete-outer-selection/`, `scripts/wm_run_alert.py`,
`data/wm-click-loop/alerts.log`, the P5 rollup and matrices, the TIED-OUTER-CASCADES
discovery, and the PLAN's commit history. Times are UTC.

## Short answer

Joe's standard (same criteria as the inner loop) was recorded on 10-28 in three notes.
On the same day and the next, every instrument that could have enforced it was removed,
narrowed, or rewritten as a retention check, and the review layer that followed judged
against the rewritten rows. The Lean that remained for the outer path is a model of the
code, so it certified the code. Nothing was left above both that could say "fake".

## The chain, in order

### 1. A detector existed and was deleted (10-28, 04:43Z and 04:58Z)

Until the morning of 10-28, `Requirements.lean` carried, for the outer path:
- **Q2** (codex-68 ruling, 2026-10-16, `9e79af1bee`): one outer action per eligible task;
  for the selected task a construction with `reading ≠ 0 ∧ library ≠ 0 ∧ graph ≠ 0 ∧
  kernel ≠ 0 ∧ pool = slice ∧ sliceFromWholeLibrary = true` and a positive count of distinct
  policies.
- **Q8**: certified eligible frontier over all open tasks.
- **Q10**: arrangement pairs distinguished by G.
- **OuterCascadePolicyObjective.SelectionCertificate.Valid**: the selected policy's
  objective is *strictly* less than every other candidate's, with `exact_tie_invalid` as a
  negative witness and `selected_is_unique_strict_minimum` as the theorem.
- a Q4 conjunct requiring the authorized outer carrier.

Joe's ruling that morning (DELETE-outer-selection-2026-10-28.md) was about what the outer
policies *are*: the live selector built tactical families for every target before
choosing, and "we're not going to build a new solution until that garbage has been
deleted". The deletion was executed by codex-1, dispatched by claude-11: futon2
`a04b157d2` (159 files, 24,399 deletions) and mathlib4 `3a09d2a80f` ("Remove rejected
outer task-selection Lean specification", 1,976 deletions). It removed the carrier and the
properties together: strict minimum, library pin, slice from whole library, one action per
open task. The DELETE doc scoped the Lean part as "clauses that equate a menu of tactical
families with compared policies (Q2/Q4/Q8 outer)"; the strict-minimum certificate was not
one of those clauses, but it lived in the same module and went with it.

NOTE-outer-selects-missions-not-plans-2026-10-28.md, same day, says it plainly: "The
specification had this right once. Requirements.lean Q2, ruling of codex-68 on 2026-10-16".
Its item 3 lists the "non-vacuous Lean statements" that should replace what was dropped
and item 4 says "Q4/Q8 ... need matching updates". None of those statements was written.
claude-11's review of the deletion (`review-claude-11.md`) checked compile, tests, a guard
placement defect, kondo and parens. It did not ask what requirement now covered selection.

### 2. The replacement was built as a model, not as requirements (10-28)

NOTE-requirements-vs-r20step-2026-10-28.md classified every Q against the new R20Step:
Q3 "already-holds" (every menu cascade maps to a number; `chosenCascade_score_le` proves
the chosen score is ≤ every other, not <), and Q1, Q4, Q5, Q6, Q7, Q9 all
"not-yet-modelled (slice 4/5/6)". Its last line: "No Q1, Q4, Q5, Q6, Q7, or Q9 theorem can
be proved from the current fields without adding the missing observation/state/row
carrier." Slices 4 to 6 were never built. claude-11's `r20-hole.md` the same day recorded
Joe's words "R20 certifying against its own model proves a fiction" and planned a control
"a one-policy menu is a typed absence (G over one policy chooses nothing), not a certified
choice". Not built either. NOTE-outer-G-computation's "Order of build" runs Lean model,
Lean learning, Clojure. It has no step for requirements. Clicks began 10-30.

### 3. The projector and the alert were narrowed to match (10-29, 03:48Z)

codex-15, dispatched by claude-11, `e0c16b9a4` "Align run requirements generator with
Lean fields": `PARTIAL_REQUIREMENTS=(1,3,4,5,6,7,9)`, the Q2/Q8/Q10 rows removed from the
alert, the `outerActions` field removed from the projector. `scripts/wm_run_facts.clj` has
contained no occurrence of "outer" since. From 10-29 on there was no outer fact for the
Lean to read and no outer Q for the alert to fire. The constant's name records the
narrowing. (Pre-existing and separate: since `72439e9b7` on 09-30 the alert defaults a Q
missing from Lean's output to "holds".)

### 4. The grid criterion went from a property to a retention check (10-28)

PROPOSAL-GRID-v2-outer-loop-2026-10-28.md (claude-11 for codex-11): B3 = "Each menu
cascade's outer G is recorded with the chosen index, `:tie?` (distinct tied picks) and
`:tied-cascades`". The definition of tie as *distinct tied picks* was fixed here, before
any run, so same-pick and no-pick ties would not count. GRID v2 B3 adds "Distinct tied
picks must be disclosed, not called a unique minimum": the property "unique minimum"
became "disclosed if not". The proposal also introduced the category "Stated limits (not
passes)" for default reach, absent yield producer and late ready-gain observation. The
later matrices therefore had a status for a defect that was not "violated".

### 5. The alerts fired on all eleven runs and were read as inner row defects

`alerts.log` has ALERT FOR JOE for every run in the window: Q1 enumeration short by one to
four tasks, Q7 typed absences, Q9 unverifiable or violated. None names the outer path,
because none could. ROLLUP-P5 (zai-6): "Observed clean (12) ... Observed with limits (7):
A2, B1, B3, B4, C2, D1, D4 ... Violated (1): E4", and under "What today proved / what it
didn't": "Didn't: tie recomputation (B3), prediction of any terminal class that occurred
(B4)". B4's prediction was wrong on every run whose terminal class occurred, and the
status was "observed w/ limits". E2 was "observed" clean on a learning loop whose twelve
consumptions all read classification-receipt-absent.

### 6. The discovery that found the defect closed it as explained (10-31)

TIED-OUTER-CASCADES (zai-2) found the tie in 10 of 10 runs and `:tie? true` in 0 of 10.
Its closure condition: "Selection reaches a unique minimum on real evidence, **or the tie
is explained**." The "or" made explanation sufficient. Addendum 3 records claude-1's
decision to remove the relation exclusion so relation-unknown candidates stay in the field
(and tie), measures that the alternative mapping would be inadmissible, and concludes the
Clojure "IS Lean-aligned" because R20Step's kernels dictate the degenerate stop for an
unknown relation. The Lean model was used as the authority that licensed the tie. The
model agrees with the code because the code implements the model; no requirement sat
above both. This is the fiction Joe named on 10-28.

### 7. B3 was called violated only after the last click (11-01, 04:31Z)

PLAN commit `cccaafd18` (claude-1): "B3 is violated because selection has shown identical G
across its policies with no stated reason." Click 12 ran at 03:48Z. Before 04:31Z the
PLAN's B3 row read "Do not call a tie a unique minimum" and the rollup read observed with
limits. The incident was declared at 04:44Z.

## What the chain shows

- The ruling was known: three notes of 10-28 state it, one of them lists the theorems
  needed, one says which Qs cannot be proved.
- Every removal and narrowing was individually reasoned and reviewed for mechanics
  (build green, tests green, kondo clean), never for "what now flags a bad selection".
- Review of the runs was against rows whose predicates were retention of fields, judged
  by Zai agents writing matrices, with "limit" available as a non-failing status.
- The only Lean left on the outer path (R20Step) is a formalisation of the implementation,
  so agreement between them was reported as alignment rather than as the absence of a
  check.
- The ordinary alert did fire every time. It had nothing outer to say.

## Seats in the trailers

claude-11 dispatched the deletion (codex-1), the projector alignment (codex-15), wrote the
grid proposal and the requirements-vs-R20 note. codex-11 then claude-1 owned the grid and
plan. zai-2 and zai-6 wrote the discovery, matrices and rollup. claude-1 made the
addendum-3 decision and marked B3 violated. claude-2 does not appear in any trailer or
note read for this record; I state that as what I checked, not as a clearance.
