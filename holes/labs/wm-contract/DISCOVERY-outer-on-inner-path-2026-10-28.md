# DISCOVERY: the outer loop through the inner path (2026-10-28)

For claude-11's question: trace futon2 **main**'s own inner path from a pattern
library to a chosen action and back, and say what would have to be supplied to
run that same path once with the 19 task-selection patterns
(`futon3/library/task-selection/*.flexiarg`), the Giraffe task field, and
@predicts outcomes. Discovery only; nothing here edits source.

**Head at trace time: `ac4d4bdcc` (main)** — note that the `delete-outer-selection`
merge landed *while this trace was being made* (my first reads were at
`5d3a10a4b`); everything below is re-checked at `ac4d4bdcc`. Post-merge,
`full_loop_runtime.clj:506-511` records the new contract: *"There is no
task-selection policy: without a caller-supplied task the ordinary path is
typed absent."* That is exactly the hole this discovery is about.

---

## 1. Where patterns enter

The inner path never parses `@guard`/`@effect`/`@predicts` prose. A pattern
enters selection through four concrete artifacts:

- **A byte-pinned mined pattern graph** — `pattern_graph_pin.clj:1-40`
  (ns docstring), loaded once per field by
  `wm/family_selection.clj:140-148` (`families-for-field` →
  `graph-pin/load-pinned`); graph shape `{:pattern-ids #{...} :edges [{:a :b
  :kind :weight :evidence}]}` (`pattern_graph_pin.clj:28-33`). Without a
  graph path every target fails `:graph-path-not-supplied`
  (`family_selection.clj:152-153`). The graph supplies *structure only*.
- **A 象 reading per target** — `target_reading_registry.clj`
  (`current-reading`, keyed by excerpt digest; called at
  `family_selection.clj:180-183`). Its `:analysis` carries
  `sentences → fragments → pattern_refs {:id :source_sha256 :status
  "candidate"}`; `target_policy_family.clj:28-47` (`reading-patterns`,
  `reading-slice`) reads exactly those fields and pins each cited pattern to
  a library sha.
- **Pattern text bytes** — `occurrence_square_registry.clj:17-29`
  (`library-resolver`) slurps `<library-root>/<pattern-id>.flexiarg`
  (default root `/home/joe/code/futon3/library`, `:11`) to validate authored
  occurrence squares. This is the only place raw `.flexiarg` bytes are read,
  and it reads them as opaque bytes, not fields.
- **Pattern artifact receipts** — `pattern_registry.clj:71-91`
  (`pattern-artifact-receipt`) pins `id → path+sha` beneath
  `*pattern-library-root*` (`:51`, default `/home/joe/code/futon3/library`);
  `addressable-pattern?` `:103-110` requires path+sha+evidence ids.
- **Pattern θ (learning state)** — `learning_trial_ledger.clj:611`
  (`pattern-theta`, default root `data/wm-learning-trials` `:14`), consumed at
  `cascade_shape_g.clj:182-190` (absent record → θ = 1/2).

**Finding:** the 19 task-selection patterns carry `@role/@guard/@effect/@predicts`
prose; the inner path consumes none of those fields. It consumes pattern
*identity* (graph node), *prose bytes* (square validation), and *θ*. A
selection policy built from these patterns is, to the inner path, a graph over
pattern ids plus per-target readings that cite them — the guard/effect
semantics have to be realised as graph edges and reading citations, or the
inner path cannot see them.

## 2. Construction (the family of candidate cascades)

Two construction routes exist; only the second is the live reading-policy one:

- **`futon2.aif.construction/construct`** (`construction.clj:554-583`) — the
  meta policy-construction loop with caller-injected moves
  (`construction_moves.clj` implements the futon3 cascade-construction moves;
  tests `construction_moves_test.clj`, `construction_test.clj`). Production
  reaches it only via `compiled-construction/construct` when an entry carries a
  compiled cascade (`full_loop_runner.clj:2925`).
- **The reading-policy family route** (what `wm/cascade_decision.clj` calls):
  `target_policy_family.clj:94` (`policy-family`) turns the reading's analysis
  cascades (`analysis_cascade.clj`) + retraction over the pinned graph into
  one target's policies; `cascade_shape_g.clj:170-203`
  (`arranged->candidate`) materialises each into the canonical candidate
  `{:kind :cascade-candidate :id [target policy-id] :precedence {:co-apply
  {:units <order> :descent [[from to]…] :patterns {id {:guard {:clauses
  [{:present #{token}]}] :produces #{token} :theta θ}}}}}`. **The guard is
  constructed structurally** — `needs` = done-tokens of incoming graph edges
  (`:183-190`) — not read from the library.

State/belief the scorer needs: callers pass `{:cascade-belief {#{ } 1}}`
(the empty-set-start belief) — `cascade_shape_g.clj:318`,
`wm/family_canonical_g.clj:89`.

## 3. Scoring

- `cascade_shape_g.clj:271` (`score-arranged`) builds an exact-enumeration
  observation model over the candidate's own token universe
  (`:311-326`, progress preference `:224-240`) and calls
  `cascade_observation_scoring.clj` (`scorer/rank-cascade-actions`,
  required at `cascade_shape_g.clj:22`).
- The canonical field comparison is `cascade_coapply_g.clj:98-112`
  **`compare-at-horizon [state candidates opts]`**: per-candidate
  class-emission observation models (`:observation-model-of`, shared class
  preference, horizon must match `:horizon-steps`), pattern counts from
  `cascade_pattern_counts.clj:31-36` (`:co-apply` shape), ruled kernel check,
  raw epoch sums. Reached in production through
  `wm/family_canonical_g.clj:89` (`rescore`) and `:127` (`certify-score`),
  which `wm/family_selection.clj` invokes when `:g-law :canonical-coapply`
  (`select-over-families` → `canonical-g/rescore`, `family_selection.clj`
  call at `:368` region; defn `:308`).
- `active_horizon_g.clj:169` (`active-horizon-g`) is the :evaluate-g the
  *construction* loop's docstring names (`construction.clj:26-28`); the
  reading-policy route does not pass it. **Production passes
  compare-at-horizon via family-canonical-g, not active-horizon-g.**

Inputs the scorer needs per candidate: belief `{:cascade-belief {#{ } 1}}`,
one class-emission observation model per candidate (synthetic models are
built by `cascade_shape_g.clj` itself, provenance `:synthetic`), preference C
(ruled 55/35/5/5 class kernel + progress preference), pattern counts [a b].

## 4. Choice

`wm/family_selection.clj:308` (`select-over-families`) scores every family
(`score-family-field :263`), then calls `policy/select-action-cascades`
**once** over the union (`family_selection.clj:433`). `policy.clj:351`
(`select-action-cascades`) selects by
`cascade_selection.clj:54` (`selection-posterior`: σ(ln E − F − G/β),
E = habit prior mass, must be > 0) and
`cascade_selection.clj` (`bayes-choice`) over each cascade's first acting
pattern (`policy.clj:364-366`). Habit: ranked entries carry `:habit 1`,
provenance `:pending-selector-attachment` (`family_selection.clj:66-67`);
the selector reads habit through `policy.clj:256-284`
(`selection-input`), fed by `cascade_habit_store.clj` (`policy-view`,
`attach-habits`; default store `data/wm-habit/cascade-prior.edn` `:16-17`).
β has no default (`policy.clj:358-362`).

## 5. Learning (counts and habits after an outcome)

- **Habit:** `cascade_habit_reinforcement.clj:57` (`evaluate`) — a pure rule
  over the frozen token-outcome comparison receipt
  (`:wm/token-outcome-comparison-v1`, `valid-comparison? :34-53`); reinforces
  (+1) only when a predicted token was observed; `close! :85-93` writes via
  `cascade_habit_store.clj` (`record-reinforcement! :187`). The runner calls
  `selection-event` and `evaluate` on every tick
  (`full_loop_runner.clj:1353-1355`) and `close!` at the close boundary
  (`full_loop_runner.clj:7076-7097`).
- **Pattern θ / [a b]:** `attempt_learning.clj:60` (`receipt`) and `:166`
  (`pattern-receipt`, per-occurrence pattern keys) recorded into the
  learning-trial ledger by the runner (`full_loop_runner.clj:5399-5414`,
  `learning_trial_ledger.clj:26 record!`, default root `:14`);
  `close-b-update` at `full_loop_runner.clj:6773-6786`;
  `pattern-theta` reads back at `learning_trial_ledger.clj:611` for the next
  construction (`cascade_shape_g.clj:182-190`).

## 6. The production entry point (post-merge)

`futon2.aif.full-loop-runtime/run-opportunity!` (runtime `:444`
`production-defaults`, runner `futon2.aif.full-loop-runner/run-opportunity!`)
→ `run-ordinary-opportunity!` (`full_loop_runtime.clj:504-511`): **without a
caller-supplied `:task` the ordinary path is typed absent** — this is the seam
the deleted outer selector used to feed. With a task:
selection phase → `construct-for-decision` (`full_loop_runner.clj:2919-2926`;
default `construct-selected-action` → `futon2.report.cascade-lane/cascade-lane`,
the R1→R16 lane in `scripts/futon2/report/cascade_lane.clj:127`, on the
classpath via `deps.edn:1` `:paths [… "scripts"]`) → enactment
`wm/cascade_decision.clj:1532` `execute-selected-cascade!` (runner call
`full_loop_runner.clj:7624`) → learning hops of §5 at close.

The **field-selection** route (`wm/cascade_decision.clj:1340`
`cascade-decision` → `:1003 target-policy-field` → `family-selection` →
`select-over-families` → `select-action-cascades`) is retained and tested but
after the merge **has no caller in src/** (grep for `cascade-decision/` /
`select-over-families` outside its own namespaces: only
`cascade_decision.clj:1240` and a docstring in `q2_construction.clj:58`). Its
last production driver was the outer selector the merge deleted. So "the same
functions the inner loop uses" for *selecting a task from a field* are live
library functions with a currently-unused production driver — the caller who
supplies `:task` (or calls `cascade-decision` directly) is the missing glue.

### Hop confirmations (own process, `cd futon2; clojure -M:test -n …`)

At HEAD `ac4d4bdcc` unless noted:

```
clojure -M:test -n futon2.aif.wm.family-selection-test -n futon2.aif.cascade-coapply-g-test \
          -n futon2.aif.cascade-selection-test -n futon2.aif.construction-test
→ Ran 53 tests containing 269 assertions. 0 failures, 0 errors.
clojure -M:test -n futon2.aif.wm.cascade-decision-isolation-test \
          -n futon2.aif.cascade-habit-reinforcement-test -n futon2.aif.cascade-pattern-counts-test \
          -n futon2.aif.learning-trial-ledger-b-update-test
→ Ran 12 tests containing 96 assertions. 0 failures, 0 errors.
clojure -M:test -n futon2.aif.cascade-shape-g-test -n futon2.aif.wm.family-selection-canonical-g-test \
          (run pre-merge 5d3a10a4b, same namespaces unchanged by the merge)
→ Ran 34 tests containing 184 assertions. 0 failures, 0 errors.
clojure -M:test -n futon2.report.cascade-lane-decision-target-test
→ Ran 6 tests containing 10 assertions. 0 failures, 0 errors.
clojure -M:test -n futon2.aif.cascade-habit-reinforcement-runner-test
→ Ran 2 tests containing 18 assertions. 3 failures, 0 errors.
```

The last is a **pre-existing failure on main**, not caused by this discovery:
`reviewer-falsifier-refusal-cannot-touch-the-legacy-store`
(`cascade_habit_reinforcement_runner_test.clj:54-57`) expects
`:build-failed/:reviewer-falsifier-failed/:reviewer-wait`, gets
`:incomplete/:pre-author-baseline-unavailable/:construction`. No hop in the
chain above lacks a test. UNESTABLISHED: whether the full
`full_loop_runner` namespace's own suite passes at HEAD (its suite is long and
binds live ports; the merge commit message itself records "canonical-path
drift and a hardcoded historical worktree expectation" in focused runner
checks).

---

## What must be supplied to run this path once for task selection

The smallest call that would **select one task** through the unchanged inner
path is the field route, not the runner:

```clojure
;; (all inputs below must exist first — see gap list)
(let [graph-path  <pinned graph over the 19 pattern ids>          ; gap 1
      field       (family-selection/families-for-field            ; §1–2
                    {:target-sources <13 Giraffe-open rows>       ; gap 3
                     :reading-root  <象 root for Giraffe tasks>    ; gap 2
                     :graph-path graph-path})
      rows        (family-selection/score-family-field field)     ; §3
      {:keys [scored ranked]} (family-selection/construct-ranked rows)
      decision    (policy/select-action-cascades ranked {:beta <declared>})] ; §4
  decision)
```

`state/belief` needs nothing new (`{:cascade-belief {#{ } 1}}` is internal);
observation models are built synthetically per candidate (`§3`); θ defaults to
1/2 and habit to 1 (neutral, recorded as such), so **first-call selection
needs no learning state**. Learning afterwards (§5) needs the token-outcome
comparison receipt — gap 4.

### Gap list, ordered by what the first call needs first

1. **A pinned pattern graph over the 19 task-selection ids**
   (`pattern_graph_pin.clj` shape). `GET
   localhost:7070/api/alpha/cascade-real/graph` (futon3c
   `transport/http.clj:9770-9781`, backed by
   `futon3c/logic/cascade_real_live.clj`) serves the live cascade structure,
   but its JSON shape is NOT the pin shape, and UNESTABLISHED whether its
   nodes even include the `task-selection/*` ids (they are one day old). A
   miner run + `load-pinned` pin over these ids does not exist. The
   `@references` fields give authored edge candidates, but the pin wants
   *mined* edges with kind/weight/evidence.
2. **象 readings for the candidate tasks citing the 19 patterns** with library
   shas (`target_policy_family.clj:28-47` consumes `pattern_refs`). None exist
   for Giraffe tasks; without a reading the family fails
   `:no-current-target-reading` and the field abstains. This is agent
   interpretation work (the `interpretation-*` pipeline), not code.
3. **Target-source declaration rows** for the 13 Giraffe-open tasks
   (`cascade_sources.clj:230 load-declared`; `wm/cascade_decision.clj:1003-1012`
   consumes `:target-sources`). The Giraffe pass (`futon3c/src/futon3c/giraffe/pass.clj`)
   gives the open set and each item's readiness/unblocks, but no adapter from a
   Giraffe run to `{:target-id :source-path :source-kind}` rows exists.
4. **Outcome observations from `@predicts`**: the learner consumes
   `:wm/token-outcome-comparison-v1` receipts over *tokens*
   (`cascade_habit_reinforcement.clj:34-53`). No writer maps "@predicts:
   every selected task can begin when dispatched" onto candidate tokens with a
   frozen prediction + observed verdict. Until it exists, θ stays 1/2 and
   habit stays 1 — selection works, learning does not.
5. **A driver**: `run-ordinary-opportunity!` needs `{:task {:id …}}`
   (`full_loop_runtime.clj:506-511`), and the field route needs a caller for
   `cascade-decision`/`select-over-families` (deleted along with the outer
   selector). Supplying one task id is enough for the runner seam; supplying
   the field is enough for the field seam.

Not gaps: belief state, observation-model construction, preference C,
class-emission models, β plumbing (caller-declared), neutral θ/habit defaults.

### What the inner machinery cannot see (findings, not paper-overs)

- The 19 patterns' `@guard`/`@effect` semantics are inert to this path: guards
  are *constructed from graph edges* (`cascade_shape_g.clj:183-190`), so a
  selection cascade ordered "gate → order" exists only if the graph encodes
  those edges. The Giraffe readiness/unblock numbers the guards talk about
  enter nowhere.
- Giraffe's own order (`pass.clj` `:order`) is already a selection cascade in
  spirit (its README says so) but in a different algebra (needs-edges over
  tasks, not pattern co-application); nothing converts one to the other.
- Learning over task outcomes requires token predictions the current freeze
  machinery produces only for pattern/want tokens of constructed cascades.
