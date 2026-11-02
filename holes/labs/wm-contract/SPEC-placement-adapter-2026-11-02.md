# SPEC: the placement adapter — ant placements reach the outer selection

2026-11-02 23:5xZ, claude-2. Joe (23:50Z): "please develop the adapter and
test it out w/ an outer-selection-only run. You can use Codex agents for the
WM cast (such as it is in outer-only)."

## What is true today (read 23:50Z–23:55Z, source cited)

- The WM's outer field is a verified Giraffe run (`full_loop_runtime.clj:1206`
  `find-giraffe-run`, root `storage/giraffe`). `outer_observation/giraffe-field`
  (`outer_observation.clj:136-215`) turns it into `:field` (candidates
  `{:target :reach :reach-source}`) and `:facts` (target → Giraffe row with
  `:status :cone :unblocks :rank :pin :reach ...`), plus typed exclusions from
  a closed enum (`task-exclusion-reasons`, `:79`: evidence gaps only).
- Selection (`outer_select/select`, `:256`) scores every executable cascade in
  `futon3/library/task-selection/executable-steps.edn`; a cascade is a vector
  of steps applied by `outer_score/apply-selection-step` (`:106-133`), which
  mirrors Lean `applySelectionStep` (`DarkTower/WarMachine/R20Step.lean:107`).
  Known steps: `:ready-gate :by-cone :by-unblocks :by-rank` and
  `{:status-gate kw}`; Lean also has the general `.gate (keep : Candidate → Bool)`.
- Nothing reads the ant placements (`futon3c/holes/excursions/ant-reads/
  A1-placements.tsv`, 106 per-stem rows; `classes.tsv`, one class of 163).
  Checked: no reference under `futon2/src` or `futon3c/src`.
- Latest Giraffe run `20261102T013012.768Z-26ad19e6`: 508 ready, 60 placed,
  48 of those `work`; 28 ready items are `E-kimi-task-*` requisitions.

## What the adapter does

One new fact per target and one new gate step. No new exclusion reason, no new
score term, no change to Giraffe.

1. **Fact.** `futon2.aif.placement-adapter` reads the TSV and the class file,
   pins both by sha256, and gives every admitted target a `:placement` fact:
   either the row (`:status :ways :wing :action :need-goes-to :finish :cite
   :source :pin`) or the typed absence `{:status :absent :reason :no-placement-yet}`.
   A per-stem row wins over a class row. A missing or unreadable file is a
   typed absence at the source (`:placement/source {:status :absent ...}`) and
   every fact carries the absence; nothing throws at load.
2. **Gate.** `:placed-gate` in `apply-selection-step`: keep a candidate iff its
   `:placement` is a row with `:status "open"`, at least one way, and action
   `"work"` or `cluster:<name>`. `audit`, `held`, `close`, `retire`,
   `merge-into:*`, `unsure` and the absence are dropped. A fact with **no
   `:placement` key at all** (adapter not wired) refuses with
   `:outer-score/placement-unavailable` — absence must not read as a value.
   Lean: `placedGate keep := SelectionStep.gate keep` (an instance of the
   general gate; `applySelectionStep` on it is `field.filter keep`).
3. **Cascade.** README row 7 "Serves a way": only-what-can-start-now →
   serves-a-way → what-others-wait-on-first; executable steps
   `[:ready-gate :placed-gate :by-cone :by-unblocks]`. A new pattern
   `serves-a-way.flexiarg` (role gate). The selection-why for a pick under it
   names the way served and the action, read from the placement fact.
4. **Wiring.** `full_loop_runtime` annotates the observation after
   `outer-observation` builds it (path from `:outer/placements-path`, default
   the futon3c file), and retains `:placement/source` in the selection record
   evidence beside `:field-facts`.

What the adapter does NOT do: it does not exclude requisitions by name
(unplaced requisitions fail the gate like any unplaced item); it does not
touch the ratings (`per-task-ratings` stays as it is, Joe's "a reason not a
number" ruling is served by the gate and the rendered reason, not by removing
the number tonight); it does not write anything back to the TSV.

## Acceptance: the outer-selection-only run

Arm `[:after :selection-world]` in the live JVM (`futon2.aif.wm.debugger`),
fire one click with three Codex seats as the cast, read the retained outer
selection at the stop, then `continue! run :abort`. Pass if: the run's
selection record carries `:placement/source` with the TSV sha, the
`:serves-a-way` cascade appears in `:cascades` with a pick whose placement is
open/work with a cited way, and the rendered why names the way. The pick
itself is whatever G chooses; the adapter's claim is that the reason exists
and is read from the record, not that `:serves-a-way` wins.

## Packets

- A (codex-8): `placement_adapter.clj` + test. Data only.
- B (codex-12): `:placed-gate` step, menu/state validation, README row 7,
  `serves-a-way.flexiarg`, `executable-steps.edn` v3, `render-step`, tests.
- claude-2: Lean `placedGate`, runtime wiring, merge, reload, the run.
