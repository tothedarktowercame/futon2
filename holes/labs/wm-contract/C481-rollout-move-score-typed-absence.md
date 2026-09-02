# C481 — AC5: the rollout step producer stops substituting zero for a missing score

Date: 2026-09-02. Row: `worklist.edn :AC5` (class I).
Decided by: Joe's 2026-09-02 ruling on C130 (futon2 `2f34c26`), item §5.
Census site: `src/futon2/aif/rollout.clj:129` — tally instance
`:rollout-action-score`.

## The site

`renormalize-priors` computes the per-node PUCT branching weights. It prefers
the producer's sharpened `:prior`, and falls back to `softmax(:score)` when not
every survivor carries a positive one. The fallback read

```clojure
#(Math/exp (double (or (:score %) 0.0)))
```

so a move that nobody scored received `exp(0.0) = 1.0` — exactly the weight of
a move that scored precisely zero. The two were indistinguishable downstream,
and the unscored move went on to compete for the top-k.

## The defect, measured both directions

Two reachable moves, neither carrying a `:prior`, so the softmax fallback is
the live branch: `u` with no `:score`, `s` with `:score 0.0`.

- Pre-AC5: both weigh `exp(0.0) = 1.0`; each takes prior `0.5`; both rank.
- Post-AC5: `u` is `:unscored` and does not enter the population; `s` is
  `:scored` with `:value 0.0` and takes the whole renormalized mass (`1.0`).

The control the row turns on is the pair, not either half: a **supplied** `0.0`
is a score and still ranks, while the same number arrived at by defaulting does
not. Asserted at `test/futon2/aif/rollout_test.clj:187-199` (the control) and
`:231-245` (the exclusion), including a direct assertion that the pre-AC5
expression returns the same number for both.

## The typed record

`move-score-record` (`src/futon2/aif/rollout.clj:149-205`) classifies every
proposed move. Three statuses, stamped `:producer-contract
:rollout-move-score/v1`:

- **`:scored`** — a finite `:score`, carried as `:value`. A supplied `0.0`
  lands here.
- **`:unscored`** — no `:score` at all. **No `:value` key**; `:absent` names the
  field and records `:key-present?`, so a key present-and-nil and a key that was
  never written stay apart.
- **`:refused`** — a partial map with a numeric fallback: `:score` or `:prior`
  present but not a finite number (a string, a keyword, `true`, a collection,
  NaN, an infinity), or the move is not a map at all. `:offending` names the
  field and the value it was given.

Absence and malformation are split here exactly as in AC1's
`compute-prediction-error`: a producer that emitted no score has a gap, a
producer that emitted `"0.4"` has a defect and must stay loud. `nil` and `"x"`
in the same field give different statuses and different reasons
(`rollout_test.clj:222-229`).

`:prior` is validated even though the census row names `:action-score`, because
the same expression reads it: `have-prior?` called `(double p)`, which **threw**
a `ClassCastException` on a non-number. That throw is now a named refusal.

## When the score is required

Exactly when the sharpened-`:prior` path is unavailable — that is, when some
move lacks a positive finite `:prior` and the weights therefore fall back to
`softmax(:score)`. On the prior path no `:score` is read at all, so an unscored
move is not a defect there and still ranks; its record still reports the
absence, with `:score-required? false` and no `:excluded-from-ranking?` key
(`rollout_test.clj:247-260`). This is the AC4 discipline — the record rides on
every validation, not only on the ones that changed an outcome, so a reader can
tell an inconsequential absence from the one that removed a candidate.

When the score IS required, only `:scored` moves enter the renormalized
population. That is the ranking-population change C130 §5 predicted:
"validation changes which moves can enter ranking."

## Blast radius, read not assumed

`renormalize-priors` is reached only through `ranked-survivors` →
`expand-policies` → `score-policies`, and thence `best-rollout`,
`greedy-one-step`, `arguing-worlds/generate-buildout` and
`temporal-hierarchy/hierarchical-rollout`. The one production caller is
`cascade_lane/policy-rollout` (`scripts/futon2/report/cascade_lane.clj:362-397`),
the act gate's ΔG leg.

**Measured on both real move-sets** (`futon6/data/diffsub-moves-stub.edn`, 19
moves; `futon6/data/diffsub-moves.edn`, 55 moves): every move validates
`:scored` — 19/19 and 55/55 — and every move also carries a positive `:prior`,
so the softmax fallback is not even the live branch there. Running the pre-AC5
namespace and the post-AC5 namespace side by side in one process over both
sets, `score-policies` returns **equal** collections and `best-rollout` equal
maps (stub G = −7.317000000000001, v2 G = −0.0539219775 under both). No
`:move-score-events` key is written on either. The row bites on a producer that
emits a partial move, which is what it is for.

## Self-repair condition

The records leave the search present-only:

- `score-policies` (`rollout.clj:398-416`) attaches `:move-score-events` to
  every returned policy when the expansion produced any. They are a property of
  the **search**, not of one node — the same unscored move is reachable from
  several prefixes and it is the producer that needs repairing either way — so
  they are collected across the expansion and deduplicated
  (`expand-policies-with-records`, `rollout.clj:364-391`).
- `cascade_lane` projects them onto the lane entry as `:policy-rollout-events`
  (`cascade_lane.clj:362-397` for the memoized read, `:443-461` and `:509-527`
  for the two entry builders). The memo cache now holds
  `{:score … :events …}` so the ΔG leg and the records come from the *same*
  rollout rather than two.
- `close_loop/act-gate-for` (`src/futon2/aif/close_loop.clj:118-135`) and
  `enact/act-gates-with-shown` (`src/futon2/aif/enact.clj:207-217`) carry them
  onto the act-gate, and `close-loop!` onto the verdict
  (`src/futon2/aif/enact.clj:307-319`), which
  `src/futon2/aif/trace.clj:591-592` already persists as `:act-gate-verdicts`.

Present-only at every hop: no key means the rollout's move population
validated, which is a different claim from "the producer did not report". AC8's
harvester is what turns the records into work items.

## Planted cases

All in `test/futon2/aif/rollout_test.clj:165-308`.

- **ABSENT** — key missing and key-present-but-nil, both `:unscored`, no
  `:value` key, `:absent` recording which (`:172-185`).
- **MALFORMED** — `"0.4"`, a keyword, `true`, `[]`, `{}`, NaN, `+Infinity`,
  `-Infinity` in `:score`; a string `:prior`; a move that is not a map. Each
  refused with `:offending` naming field and value, and no `:value` key
  (`:201-220`).
- **SEPARATION** — `nil` versus `"x"` in the same field (`:222-229`).
- **THE CONTROL** — supplied `0.0` scores and ranks; unsupplied does neither;
  plus a direct assertion that the pre-AC5 expression returns the same number
  for both (`:187-199`).
- **REQUIREMENT CONDITION** — on the prior path the score is not read, the
  unscored move still ranks, and its record says `:score-required? false`
  (`:247-260`).
- **REGRESSION FLOOR** — a fully scored population renormalizes to the same
  softmax weights and writes no events key at all (`:262-279`).
- **PROJECTION** — the records ride out on `best-rollout`; `move-score-events`
  returns `[]` over a clean record collection (`:281-294`).

## Gates

- clj-kondo: 0 errors, 0 warnings on the five changed files.
- `futon4/dev/check-parens.sh`: OK on all five.
- `clojure -M:test -m cognitect.test-runner -d test/futon2`: **1007 tests, 6184
  assertions, 0 failures, 0 errors** (was 998/6100 at AC4: +9 deftests, +84
  assertions).
- C130 lint: `checks/absence-coercion-dispositions.edn:51-54` flipped
  `:blocked` → `:fix-now` with the control (`:summary :fix-now` 13 → 14,
  `:blocked` 3 → 2); `bb checks/preemptive_absence_coercion_lint.clj` findings
  3 → 2, this site's finding gone, the two remaining being AC6's and AC7's
  sites.
- p4ng tally over the fixed 61-instance population: 54 repaired / 6 open / 1
  partial → **55 / 5 / 1**.

## Pre-existing red, not mine

`pointer_check` reports 2 unresolved pointers, both the same adapters pointer
carried in AC7's worklist row, which the checker cannot resolve because
`src/futon2/aif/adapters/` is not on its root allowlist.
`negative_controls.sh` fails on that pointer and on nothing this row added.

## Not done here

- The census `:at` key stays `src/futon2/aif/rollout.clj:129` — it is the join
  key the lint and C12 share, and AC1–AC4 set the precedent of naming the new
  lines in the `:control` prose instead of re-anchoring the row. That pointer
  now lands inside this row's own producer block rather than on the repaired
  expression, which is the cost of the precedent; worth raising once for all
  five rows rather than diverging on the fifth.
- **The refuse floor is AC6's, not this row's.** When exclusion empties the
  candidate set, `ranked-survivors` returns `[]` and the expansion stops with
  the prefix it had — the same shape it already had for a state with no
  reachable moves. Telling those two apart ("refuse when exclusion empties the
  candidate set, or when the rollout authorizes an action rather than
  diagnosing") is exactly AC6's statement, and it is deliberately left open.
  The records still ride out, so the empty set is at least explicable
  (`rollout_test.clj:296-308`).
- `move-cost` (`rollout.clj:298-310`) is untouched. It is AC6's site
  (`rollout.clj:158`), and its input is a different one: `:step-score-delta`
  first, then the absolute `:score`. Worth recording for AC6: **`:step-score-delta`
  is absent on 19/19 and 55/55 real moves** — both sets carry `:delta-g`
  instead — so that site's first fallback fires on every real move today.
- `trace-schema-version` is not bumped, matching AC1–AC4.
