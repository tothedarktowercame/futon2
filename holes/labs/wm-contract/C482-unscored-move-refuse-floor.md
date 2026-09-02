# C482 — AC6: the rollout cost leg stops substituting zero, and exclusion gets a floor

Date: 2026-09-02. Row: `worklist.edn :AC6` (class I).
Decided by: Joe's 2026-09-02 ruling on C130 (futon2 `2f34c26`), item §6.
Census site: `src/futon2/aif/rollout.clj:158` — tally instance
`:rollout-unscored`. Depends on `:AC5` (futon2 `e76c51c`), which landed first.

## The site

`move-cost` is the local `g(s_t)` the rollout accumulator sums. It read

```clojure
(cond
  (:truncated? state) 0.0
  (= :satisfied status) 0.0
  :else (double (or (:step-score-delta move) (- (double (or (:score move) 0.0))))))
```

Four different things produced the identical number `0.0`: a truncated state,
an already-satisfied capability, a move that genuinely scored zero, and a move
nobody costed at all. AC5 stopped the *ranking* leg reading `(or (:score m)
0.0)`; the *cost* leg read the same field through its own default one step
later.

## The ruling, and what it changed from C130 §6

C130 §6's reading was **B** (refuse the rollout) "until a producer states why
exclusion preserves completeness". Joe's 2026-09-02 ruling took **A with a
refuse floor**: "unscored moves: exclude-and-continue with a refuse floor
(empty set, or authorizing rollout)". So this row implements A and then names
exactly the two conditions under which A is not allowed to stand.

## The typed record

`move-cost-record` (`src/futon2/aif/rollout.clj:297-372`) classifies the cost
of every candidate. Stamped `:producer-contract :rollout-move-cost/v1`:

- **`:present`** — a cost was determined, carried as `:value`, with **`:basis`
  naming where the number came from**: `:truncated-state`,
  `:satisfied-capability`, `:step-score-delta`, or `:negated-score`. That is
  the distinction the bare `0.0` destroyed, and it is asserted directly —
  four zeros, four bases (`test/futon2/aif/rollout_test.clj:352-374`). A
  **supplied** `0.0` in either field lands here.
- **`:uncosted`** — **neither** field is supplied. No `:value` key; `:absent`
  is a two-element vector naming both fields with `:key-present?` each, so a
  key present-and-nil and a key never written stay apart
  (`rollout_test.clj:333-350`).
- **`:refused`** — a field is present but not a finite number, or the move is
  not a map. `:offending` names the field and the value it was given.

Two ordering choices, both deliberate:

- The `map?` check runs **before** the structural short-circuits, so a
  malformed move is loud even in a truncated state, where the old expression
  never read the move at all (`rollout_test.clj:403-408`). The practical
  difference is nil — a non-map never reaches the cost leg from the search —
  but "loud malformed" should not be conditional on state.
- The delta is checked before the score, because the old expression preferred
  it. A **malformed** delta is therefore refused rather than falling through
  to a perfectly good `:score` — absence omits, malformation refuses, the AC1
  split applied to a two-field fallback chain (`rollout_test.clj:398-401`).

`move-cost` itself (`rollout.clj:451-466`) is now the numeric projection
`(:value (move-cost-record state move))` — a double, or **nil**. It never
returns a fabricated zero.

## Exclude-and-continue

`ranked-survivors-with-records` (`rollout.clj:409-444`) costs the population
that survived AC5's score validation and drops the ones it cannot cost, marking
each dropped record `:excluded-from-rollout?`. The surviving `:prior`s are
**rescaled** (`renormalize-prior-mass`, `rollout.clj:382-392`) so the node's
branching weights still sum to 1 — a pure rescale, so survivor order is
unchanged; it only repairs the numbers each survivor carries. The rescale is
applied **only when something was excluded**, so an unexcluded population is
left bit-for-bit alone (`rollout_test.clj:522-538`).

Measured (`rollout_test.clj:419-444`): two reachable moves on the sharpened-
`:prior` path — so AC5 excludes nothing and what is observed is AC6's leg —
one with no cost fields and one with `:score 1.0`. The uncostable move is
dropped, the other takes the whole renormalized mass, the search runs to a
policy over it, and the record rides out on `best-rollout`.

## The refuse floor

`rollout-refusal` (`rollout.clj:608-643`) returns a typed record
(`:producer-contract :rollout-refusal/v1`) or nil. Two conditions, exactly the
two the ruling names:

**`:candidate-set-emptied-by-exclusion`** — some node's reachable set was not
empty and validation (AC5's *or* AC6's) excluded all of it. This is the case
AC5 deliberately left open and asserted as a boundary: a node exhausted by
exclusion returns the prefix it happened to have, which is the **same shape** a
genuinely terminal node returns, and `best-rollout` then takes an argmin over a
branch a producer defect silently truncated. Both directions are measured
(`rollout_test.clj:446-472`): two uncostable reachable moves refuse, with
`:reachable-count 2` and the emptied node's prefix; a state with **nothing
reachable** does not refuse and scores `0.0` as before, because its candidate
set was empty to begin with rather than emptied.

**`:exclusion-under-authorizing-rollout`** — the caller declared `:authority
:authorize` and any move was excluded. A reduced candidate set changes which
action is chosen. The control is that the *same population* stands under
`:diagnose` and refuses under `:authorize`, and that an authorizing rollout
with nothing excluded stands (`rollout_test.clj:474-503` — the floor is about
exclusion, not about authority).

A refusal is not a partial answer. `score-policies` (`rollout.clj:645-677`)
returns a single map carrying `:rollout/refusal` and the events, with **no**
`:policy-rollout-score` and no `:policy`. `cascade_lane/policy-rollout` then
reads nil, and the act gate abstains — the shape it already uses for "ΔG
genuinely unavailable, not zero" (`close_loop.clj:10` "Missing either leg
abstains"). `select-policy` (`rollout.clj:709-733`) returns `{:decision
:refuse …}` rather than sorting over a missing number: the AC4 shape
(abstain and return control) at this site.

`project-policy` (`rollout.clj:487-539`) refuses too, on a different ground: a
policy is an **ordered plan**, so a step that cannot be costed cannot be
dropped with the rest kept. Exclude-and-continue is a candidate-set rule, not a
within-policy one. The search never reaches that branch — the candidates were
already filtered, and the only state changes the walk makes (`:truncated?`, a
cap becoming `:satisfied`) can only turn a record `:present` — but
`project-policy` is called directly on caller-supplied policies
(`arguing_worlds.clj:143`, `scripts/kill_test_rollout.clj:22`).

## Authority is declared, not inferred

`rollout-authority` (`rollout.clj:591-606`). `:diagnose` — the default — is the
claim that the result's `:policy` will **not** be enacted, only its score read.
`:authorize` says the rollout is choosing. Anything else throws.

The refusal record carries `:authority-declared?`, because **a default is not a
claim**: AC8's harvester can then see a rollout running on an undeclared
authority rather than reading the default as an audited `:diagnose`
(`rollout_test.clj:495-499`).

**Audited: there is no authorizing caller today.** The one production caller is
`cascade_lane/policy-rollout-result` (`scripts/futon2/report/cascade_lane.clj:362-390`),
which reads only `:score` and never touches `:policy` — it now declares
`:authority :diagnose` explicitly. `arguing_worlds/generate-buildout`
(`src/futon2/aif/arguing_worlds.clj:89-109`) does call `select-policy` and take
the selected `:policy`, but on a copied state with no live writes ("Buildout
generated on a copied state; no live writes.", `arguing_worlds.clj:108`), and
its `lens-move` (`arguing_worlds.clj:62-68`) coerces both `:score` and
`:step-score-delta` through `(or % 0.0)` of its own, so no uncostable move
reaches the rollout from there. Like AC7's fulab seam, the `:authorize` floor
is typed now so that a later connection cannot silently run a choosing rollout
on a reduced candidate set. **`arguing_worlds`' own `(or % 0.0)` is not in the
C12 census and is not repaired here.**

## Self-repair condition

The records leave the search present-only, on the channel AC5 built:

- `score-policies` attaches `:move-cost-events` (and `:move-score-events`)
  when the expansion produced any, and `:rollout/refusal` when it refused.
  They are a property of the **search** — the same uncostable move is reachable
  from several prefixes — so they are collected across the expansion and
  deduplicated (`expand-policies-with-records`, `rollout.clj:541-589`, which
  also collects `:emptied-nodes` as `{:prefix …ids… :reachable-count n}`,
  ids not states, so the record stays small enough to persist).
- `cascade_lane/policy-rollout-result` folds all three into the **same**
  `:policy-rollout-events` the AC5 records already ride
  (`cascade_lane.clj:362-390`). Each record carries its own
  `:producer-contract` — `:rollout-move-score/v1`, `:rollout-move-cost/v1`,
  `:rollout-refusal/v1` — which is how a consumer tells them apart. This is
  why `close_loop.clj:118-133`, `enact.clj:207-217` / `:305-319` and
  `trace.clj:591-592` needed **no change**: they carry and persist the vector.

Present-only at every hop: no key means the rollout's candidates all costed.

## Planted cases

All in `test/futon2/aif/rollout_test.clj:311-538`, plus the AC5 boundary test
at `:296-310`, which this row converts from "asserted so the boundary is
visible" into the refusal assertion.

- **ABSENT** — both keys missing, and both present-but-nil; `:uncosted`, no
  `:value`, `:absent` recording which (`:333-350`).
- **MALFORMED** — `"0.4"`, a keyword, `true`, `[]`, `{}`, NaN, `±Infinity` in
  each of the two fields; a malformed delta beside a good score; a non-map, in
  a truncated state as well (`:384-408`).
- **SEPARATION** — `nil` versus `"x"` in the same field (`:410-417`).
- **THE CONTROL** — four zeros, four bases, plus a direct assertion that the
  pre-AC6 expression returns `0.0` for all of them (`:352-374`).
- **THE FALLBACK CHAIN** — delta preferred over score, score entering negated
  (`:376-382`).
- **EXCLUDE-AND-CONTINUE** — the uncostable candidate dropped, the prior mass
  rescaled onto the survivor, the search continuing (`:419-444`).
- **FLOOR 1, BOTH DIRECTIONS** — exclusion emptying a non-empty reachable set
  refuses; nothing reachable does not (`:446-472`).
- **FLOOR 2** — same population, `:diagnose` stands and `:authorize` refuses;
  `:authorize` with no exclusion stands; an undeclared authority is reported as
  undeclared; an unknown authority throws (`:474-503`).
- **ORDERED PLAN** — `project-policy` refuses at the offending step index;
  `select-policy` returns `:refuse` (`:505-520`).
- **REGRESSION FLOOR** — a fully costable population is unchanged, priors
  bit-for-bit, no events key, no refusal key (`:522-538`).

## Measured on the real move-sets

Running the **pre-AC5** namespace (futon2 `dc3c0b2`) and the post-AC6 namespace
side by side in one process over both real sets:

| set | moves | `:step-score-delta` | `:score` | cost records | `score-policies` | `best-rollout` | G |
|---|---|---|---|---|---|---|---|
| `diffsub-moves-stub.edn` | 19 | 0/19 | 19/19 | 19 `:present`, all `:negated-score` | equal (6 = 6) | equal | −7.317000000000001 |
| `diffsub-moves.edn` | 55 | 0/55 | 55/55 | 55 `:present`, all `:negated-score` | equal (27 = 27) | equal | −0.0539219775 |

No key is added to either result. This also confirms AC5's measured claim that
`:step-score-delta` is absent on every real move, so `:negated-score` — the
branch that held the fabricated zero — is the live basis today, and the row
bites on a producer that emits a move with neither field, which is what it is
for.

## Gates

- clj-kondo: 0 errors, 0 warnings on the three changed code files.
- `futon4/dev/check-parens.sh`: OK on all three.
- `clojure -M:test -m cognitect.test-runner -d test/futon2`: **1017 tests, 6312
  assertions, 0 failures, 0 errors** (was 1007/6184 at AC5: +10 deftests, +128
  assertions).
- C130 lint: `checks/absence-coercion-dispositions.edn:56-60` flipped
  `:blocked` → `:fix-now` with the control (`:summary :fix-now` 14 → 15,
  `:blocked` 2 → 1); `bb checks/preemptive_absence_coercion_lint.clj` findings
  2 → 1, this site's finding gone, the one remaining being AC7's.
- p4ng tally over the fixed 61-instance population: 55 repaired / 5 open / 1
  partial → **56 / 4 / 1**.

## Pre-existing red, not mine

`pointer_check`: 595 pointers in 3 files, 2 unresolved — both the same
`adapters/fulab.clj:81` pointer carried in AC7's worklist row, which the
checker cannot resolve because `src/futon2/aif/adapters/` is not on its root
allowlist. `negative_controls.sh` fails on that pointer and on nothing this row
added. Identical before and after this commit.

## Not done here

- The census `:at` key stays `src/futon2/aif/rollout.clj:158` — the join key
  the lint and C12 share, per the AC1–AC5 precedent of naming the new lines in
  the `:control` prose instead of re-anchoring the row. That pointer now lands
  inside this row's own producer block. Worth raising once for all six rows.
- `arguing_worlds/lens-move`'s own `(or % 0.0)` on `:score` and
  `:step-score-delta` (`arguing_worlds.clj:62-68`) is a second coercion at a
  site the C12 census does not list. It means no uncostable move can reach the
  rollout through that path, which is why the `:authorize` floor has no live
  trigger today. Naming it, not repairing it — it is outside this row.
- `softmax` (`rollout.clj:699-707`) still reads `:policy-rollout-score`
  unguarded. `select-policy` no longer hands it a refusal, so the path is
  closed, but the public function would still NPE on a map without the key.
  Pre-existing, unchanged.
- AC8 (the tail-eater) is what turns these records into work items. This row
  only guarantees they reach the trace.
- `trace-schema-version` is not bumped, matching AC1–AC5.
