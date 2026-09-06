# C541 — F12 slice 3: O4 over the library is not reachable, and why

**Row:** `:F12` slice 3 (`worklist.edn`, `:loop-mode :one-slice-per-invocation`).
Slice 1 is `C539-F12-organise-census.md`, slice 2 is
`C540-F12-zaif-cascadediff-witness.md`; this is the slice C539 §3 names.

**What this is.** The measurement that had to come before the construction C539
§3 asked for, and which says that construction would not have produced a
witness. Every number is recomputed by `f12_o4_reachability.clj` into
`runs/F12-organise/02-o4-reachability.edn`; none is read off a record's own
`:o4` field.

**What this is NOT.** No `futon3` file and no `futon3c` file is changed — the
rule table is read through its namespace rather than copied, and the cascade
records are read where they lie. No Lean is written; slice 3 is not a Lean
slice. No `:choices` entry and no `:decisions` entry: §4 raises two questions
and takes neither. `Holes.lean:861` is untouched.

---

## 1. What C539 §3 asked for, and the two things wrong with it

C539 §3 specified slice 3 as "a `futon3` construction whose members carry at
least two play-grain rules, so the precedence change has something to reorder;
produces an `:o4` map on a 1239-pattern record." Both halves of that fail, for
reasons that are measurable today and were not measured then.

### C541-a. Two rule-carrying members is necessary but not sufficient

O4's acting order is read over the gate's PRIMARY rounds — a round carrying both
a transcript and a recorded v0 decision whose oracle label the record determines
(`futon3c:scripts/zaif_cascade_gate.clj:435`, `:559-562`; repairs 2 and 3 of
that gate's second pre-run review). Two members carrying rules move that acting
order only if their rules CONTEND on a round *inside that denominator*. On the
only cohort the library scale has, they do not:

| | rounds |
|---|---|
| cohort rounds | 103 |
| with a transcript | 102 |
| paired (transcript **and** a v0 decision) | 49 |
| **primary** (paired **and** the oracle label is determined) | **29** |
| `war-machine/ambient-pattern-retrieval` live | 102 |
| `math-strategy/missing-dependency-protocol` live | **3** — rounds 3, 4, 7 |
| both live (contention) | 3 — rounds 3, 4, 7 |
| **of those, primary** | **0** |

"Live" is the antecedent holding **and** the THEN returning a value, because
that is what `fo/fire` (`futon3:checks/find_organise.clj:406`) takes the first
of in precedence order; an antecedent that holds over a THEN that returns `nil`
cannot be reordered. The `missing-dependency-protocol` rule is a one-shot
three-phase sequence (`zaif_cascade_gate.clj:129-151`), and its whole live
window — rounds 3, 4, 7 — falls in the gap between primary rounds 2 and 33.
The primary rounds are 2, 33, 41, 54, 55, 58, 61, 63, 66, 72, 73, 75–78, 80,
83–87, 90–92, 94, 95, 98, 100, 102.

### C541-b. No recorded cascade carries two rule-bearing members at all

Recomputed over all nine records, for every run they carry. For
`construct-cascade.edn`, whose runs record no `:cascade`, members are recomputed
as O1's own union — `find`'s selected plus the run's admitted — and the count is
cross-checked against the `:members` count the run records (control 5, §5).

| record | run | members | rule-carrying |
|---|---|---|---|
| `construct-cascade.edn` | `:widen-to-a-budget` | 20 | **1** — `war-machine/ambient-pattern-retrieval` |
| `construct-cascade.edn` | `:widen-to-the-marginal-gain-floor` | 13 | 0 |
| `zaif-cascade.edn` | `:widen-to-a-budget` | 20 | **1** — `math-strategy/missing-dependency-protocol` |
| `zaif-cascade.edn` | `:widen-to-the-marginal-gain-floor` | 17 | **1** — same |
| `ants-cascade.edn` | both | 5, 4 | 0 |
| `alfworld-cascade.edn` | both | 6, 3 | 0 |

The maximum over every recorded run is **one**, and the two library-scale
records carry a *different* one each. So "the members carry at least two
play-grain rules" is not a property any recorded cascade has, and reaching it
means either a new construction or a new encoding — both of which are choices
made with the law in view, which is §4.

---

## 2. The six numbers, and the fact that the denominator decides the verdict

The probe applies `fo/o4-precedence-governance`
(`futon3:checks/find_organise.clj:531`) to the six numbers the precedence
exchange produces, through the gate's own `play` (`:311`) rather than a second
spelling of it. The exchange is the one the gate performs
(`zaif_cascade_gate.clj:444-450`): with exactly two rules it is well defined
without a cascade.

| denominator | rounds | acting order changed? | score | O4 |
|---|---|---|---|---|
| **primary** (the gate's) | 29 | **no** | 14 → 14 | **false** |
| paired | 49 | yes (round 7) | 34 → 33 | true |
| with a transcript | 102 | yes (rounds 3, 4, 7) | 83 → 81 | true |

**This is not a witness and the output says so in the file.** No constructor
selected both patterns, so there is no cascade for the row to be *of*; these are
the numbers a construction carrying both would record. What they establish is
that the construction C539 §3 asked for would have recorded `:exercised? true`
and `:holds? false`, and that the failure would have been the denominator's
rather than the cascade's — which is the checkpoint this slice exists instead
of. Under the two wider denominators the same exchange satisfies O4 through its
acting-order disjunct alone.

The gate's own `coverage` reports **zero** contentions, and will keep reporting
zero however the rounds fall: its contention is filtered to rules the cascade
CONTAINS (`zaif_cascade_gate.clj:646-647`), and only one member carries a rule.
That filter is right for what `coverage` is for — gating the rule table before a
run — and it is exactly the filter this file drops, because the question here is
what a cascade carrying both WOULD do.

---

## 3. The second blocker, found while measuring the first: the gate does not pass

`require-pass!` (`zaif_cascade_gate.clj:579-582`) aborts at HEAD with
`:rule-does-not-encode-an-authored-then` for **three of the four rules**. Their
`:then-source` spans no longer fall inside the `+ THEN:` blocks they cite,
because `then-correspondence` (`:263`) re-reads those blocks from the library on
every run and the library moved under them:

| rule | cited span | `+ THEN:` block now |
|---|---|---|
| `math-strategy/missing-dependency-protocol` | `:59-77` | `[59, 79]` — the span starts *on* the header line |
| `agent/budget-bounds-exploration` | `:19-19` | `[20, 23]` — the span is *before* the block |
| `agent/pause-is-not-failure` | `:19-19` | `[20, 23]` — same |

The dates are the whole of the diagnosis. The rule table was committed
2026-09-02 (`futon3c` `31260dd4`, "the play-grain rule table and the cohort,
committed BEFORE the run"). The patterns it cites were rewritten 2026-09-05 by
the L8 and L10 rationale backfills (`futon3` `2a91028`, `5704359`), which moved
the blocks. So the gate that would have to record an O4 row does not run at
HEAD, whatever cascade it is given.

**Recorded, not repaired.** Moving a citation is an edit to a rule table whose
whole standing is that it was committed before the run and reviewed by someone
other than its author; a slice re-pointing it after the fact is the one thing
that table may not have done to it. Control 6 (§5) plants the one-line repair to
show the measurement moves, and reverts it.

Two docstring pointers in the same files have drifted the same way and are
recorded here rather than repaired: `zaif_cascade_gate.clj` cites `fo/fire` as
`find_organise.clj:398` (it is `:406`), and `construct_ants_cascade.clj:110`
cites `fo/ordered` as `find_organise.clj:382` (it is `:390`).

---

## 4. The two questions this raises, neither taken here

**D2 — which denominator O4's acting order is read over.** §2: the same
precedence exchange over the same rules gives `false` over the 29 primary rounds
and `true` over the 49 paired or the 102 transcript rounds. The primary
denominator is right for the *comparison* the gate makes — an agreement number
needs a determined oracle label — but O4 says nothing about an oracle: it says
that where the precedence changed, the acting order or the score changed with
it, and an acting order is something the cascade does whether or not the record
can grade it. Options: (i) read the acting order over the transcript rounds and
the score over the primary ones, (ii) keep both on primary and accept that O4 is
unexercisable on this cohort, (iii) state O4 of the full play and report the
denominator with it. This sits beside, and is not the same as, slice 2's
question of which FIELD O3 quantifies over (`C540` §2) and slice 1's D1 of which
CARRIER the four laws are stated of (`C539` §4).

**D3 — whether an O4 witness may be reached by an encoding written after the
law.** §1: reaching two rule-carrying members needs either a construction over a
tension chosen so the find selects two rule-bearing patterns, or a new
play-grain encoding of a THEN a member already carries. Both are the apparatus
being changed to make a law testable, and the zaif table's stated un-fitting
guard — "a table written to make the cascade win would not carry rules for
patterns the cascade does not contain" (`zaif_cascade_gate.clj:53-57`) — does
not cover fitting to a *law* rather than to a win. If the answer is yes, the
guard has to be restated to say what it covers.

Slice 4 (the carrier reconciliation and the `sorry`) remains blocked on D1 and
is not touched. O4 over the library is blocked on D2 and D3, and on §3 being
resolved by whoever owns that rule table.

---

## 5. What was checked, so the account is auditable

- `f12_o4_reachability.clj` recomputes every number above. Two runs over
  unchanged trees are byte-identical (`diff -q` exit 0).
- **A positive control inside the file.** With the precedence NOT exchanged,
  `precedence-before = precedence-after` and `fo/o4-precedence-governance` must
  hold through its first disjunct whatever the acting order does. It reports
  `:o4-holds? true`, `:by-which-disjunct :precedence-unchanged`. Without it the
  `false` in §2 could be a broken predicate rather than an unmoved acting order.
- **Four negative controls, each with the plant verified to have landed first**
  (slice 1's lesson: a plant that did not take reads exactly like a control that
  failed to fire). The env overrides `F12_COHORT`, `F12_CHECKS`, `F12_OUT` exist
  for this; the rule table is planted by putting a copy of `scripts/` ahead of
  `futon3c`'s on the classpath, so no real file is touched.

  1. **Record plant.** `war-machine/ambient-pattern-retrieval` added to
     `zaif-cascade.edn`'s floor arm in a copy, with the `:members` count bumped
     to match (verified: 18 members, recorded 18, pattern present) — the maximum
     rule-carrying count moves **1 → 2** and that run reports both rules. Caught.
     And it is the clearest statement of §2: even carrying both, the primary
     probe still reports `O4 false`.
  2. **Record plant, incoherent.** The same member added *without* bumping the
     count (verified: 18 members, count left at 17) — hard failure
     `:recorded-member-count-disagrees-with-the-recomputed-union`, exit 1.
     Caught. This is what stops the recomputed union from silently overriding
     what a record says about itself.
  3. **Rule-table plant.** `ambient-pattern-retrieval`'s antecedent replaced by
     `(fn [_] false)` in a classpath copy (verified in the planted file) — its
     live-round count goes **102 → 0**, contending rounds **[3 4 7] → []**, and
     the run fails with `:a-selected-rule-is-live-on-no-round`, exit 1. Caught.
  4. **Cohort plant, the one that matters.** In a copy, the turn's only store
     consultation is moved off round 2 and onto round 32 (verified: round 2's
     calls become `[:search]`, round 32's `[:run_shell :memory_search]`), so
     `missing-dependency-protocol`'s one-shot window lands on primary round 33.
     Contending rounds move **[3 4 7] → [33 34 37]**, contending-in-primary
     **[] → [33]**, and the primary probe's verdict flips **false → true** with
     the score unchanged at 14 → 14 — that is, through the acting-order disjunct
     alone. Caught, and it is what shows the `false` in §2 is a fact about where
     this turn's rounds fall and not a constant of the apparatus.
  5. **Gate-citation plant.** `missing-dependency-protocol`'s `:then-source`
     repaired `:59-77 → :60-77` in a classpath copy (verified in the planted
     file) — `then-correspondence` failures move **3 → 2**. Caught, and it shows
     §3 is a measurement rather than a constant. Reverted; no real file changed.
- `futon3c` and `futon3` are unchanged by this slice (`git status` on both, and
  the two `coverage`/`report` calls this file makes are the pure functions, not
  the gate's `-main`, which is what writes).
- Gates: `clj-kondo --lint f12_o4_reachability.clj` 0 errors 0 warnings;
  `check-parens`; `negative_controls.sh`; `pointer_check.bb`;
  `worklist_check.bb` — results in the commit message and the row's `:evidence`.
- Not run: `gen_aif_dag.bb` (TN §9a gate rule). No Lean elaborated, so no `lake
  build`: this slice writes none.

## 6. Where the row stands after this slice

Slice 3 does not produce the `:o4` map C539 §3 asked for, and the measurement
says that map could not have been a witness. What it produces instead is the
three facts a decision needs — no recorded cascade carries two rule-bearing
members, the two rules never contend inside the denominator O4 is read over, and
the gate that would record the row aborts at HEAD — plus the six numbers under
each denominator. Slice 4 stays blocked on D1. O4 over the library is now
blocked on D2 and D3 as well, and the row stays `:open`.
