# L5-4d: task-selection menu sources — 2026-11-01

Discovery only. Requested base `fac22f476`; inspected main `afbe293f46eba5cdb44db142c738d62b53e058be` (intervening change adds only the dispatch packet). Paths below are relative to futon2 unless prefixed by another repository. No tests, runtime evaluation, or data writes. Only this report is committed.

## Catalogue

There are **19 pattern files plus README.md** in `futon3/library/task-selection`. Each has metadata `@role`, `@guard needs:`, `@effect` at lines 8–10. The labels are structured; the guards/effects are prose, not executable EDN. No file supplies typed interpreter steps. README.md:121 explicitly says nothing here is executable yet.

| File under task-selection/ | Role at :8 | Step representation |
|---|---|---|
| `a-portfolio-not-a-winner.flexiarg` | compose | Prose guard/effect, :9–10 |
| `bound-the-bet.flexiarg` | reshape | Prose guard/effect, :9–10 |
| `end-in-something-shown.flexiarg` | gate | Prose guard/effect, :9–10 |
| `find-out-before-building.flexiarg` | reshape | Prose guard/effect, :9–10 |
| `finish-what-is-nearly-done.flexiarg` | order | Prose guard/effect, :9–10 |
| `halfway-house.flexiarg` | reshape | Prose guard/effect, :9–10 |
| `hold-with-a-rearm-condition.flexiarg` | source | Prose guard/effect, :9–10 |
| `offer-a-menu-with-a-default.flexiarg` | compose | Prose guard/effect, :9–10 |
| `only-what-can-start-now.flexiarg` | gate | Prose guard/effect, :9–10 |
| `prepare-the-ground.flexiarg` | order | Prose guard/effect, :9–10 |
| `repair-goes-to-the-front.flexiarg` | gate | Prose guard/effect, :9–10 |
| `spend-what-the-seats-have.flexiarg` | gate | Prose guard/effect, :9–10 |
| `take-the-cheap-win.flexiarg` | order | Prose guard/effect, :9–10 |
| `take-the-hard-one-to-learn.flexiarg` | order | Prose guard/effect, :9–10 |
| `the-date-decides.flexiarg` | order | Prose guard/effect, :9–10 |
| `the-operator-asked.flexiarg` | source | Prose guard/effect, :9–10 |
| `what-others-wait-on-first.flexiarg` | order | Prose guard/effect, :9–10 |
| `work-that-pays.flexiarg` | source | Prose guard/effect, :9–10 |
| `write-it-down-instead.flexiarg` | reshape | Prose guard/effect, :9–10 |

The six evidenced compositions name ordered **patterns**, not typed step vectors: README.md:55 Giraffe order; :59 closure under scarce seats; :63 repair click; :65 operator portfolio; :68 uncertain design; :71 dated demonstration. Portfolio includes branching/combination rather than a simple linear list. Pin the README separately: `q2_construction.clj:18–36` manifests only `.flexiarg` files and does not bind these composition rows.

`futon3/library/cascades/select-tasks-from-cascade-live.flexiarg` exists outside the directory. Its :9–16 conclusion and :40–50 THEN prescribe selecting with evidenced cascades; :57 onward provides next-step pointers. **It executes nothing.** It is outside the task-selection manifest. The README's historical pass.clj:300–322 reference has drifted: current Giraffe ordering is at `futon3c/src/futon3c/giraffe/pass.clj:419–435` (status, descending unblocks, descending cone, source order).

## Live makeup versus library

Read and hashed `data/wm-learning/outer-makeup.edn`: `f312583522597c49b4134b08c4a186a013baaaa12a6f1c332fe9875e57f58e3a`.

| Makeup lines / id | Cited files (task-selection/, .flexiarg) | Hand-written executable steps |
|---|---|---|
| :31–34 what-others-wait-on-first | only-what-can-start-now, what-others-wait-on-first | `[:ready-gate :by-cone :by-unblocks]` |
| :35–38 only-what-can-start-now | only-what-can-start-now | `[:ready-gate]` |
| :39–42 find-out-before-building | find-out-before-building | `[{:gate-status :undetermined} :by-cone]` |

**This is the hand-listed menu.** `outer_select.clj:86` consumes `(:cascades makeup)`. Its :40–59 pins supplied pattern names and serializes supplied steps; it does not derive steps from the library. The first cascade uses cone without citing prepare-the-ground, which the README associates with cone. The third substitutes an undetermined gate/cone ordering for a prose discovery/reshape pattern: the library does not declare that equivalence.

`outer_score.clj:31–66` implements ready/status gates, descending cone/unblocks, ascending rank, generic function gates/orders. Its :113–125 serializes generic functions as named/unverifiable. Unknown prose refuses execution. Stable sorts apply left-to-right, so the **last sort is primary** (:16–21): cone then unblocks implements unblocks priority with cone tie-breaking.

## Proposed constructed menu, and what is missing

This section describes a possible implementation contract, not existing functionality or authorization to implement it.

1. **Whole manifest exists:** `q2_construction.clj:18–36`, called by `outer_select.clj:34–38`, hashes all pattern files under the directory into a sorted id→sha map with count and manifest digest. Separately pin README bytes for composition evidence.
2. **Outer retrieval/slice is missing:** evaluate the whole manifest against pinned task circumstances and declared guards, retaining query, source pins and each inclusion/exclusion rationale. No such retrieval/composition call exists in the inspected selector path. A fixed list renamed “slice” would repeat the defect. Giraffe does not supply every seat/date/repair/stop-rule fact the prose guards request; unavailable inputs must be explicit, not guessed.
3. **Pool equals retrieved slice:** construct evidenced README compositions with unit shas and explicit ordered/branch edges. Check the union of arranged patterns equals the slice. Do not silently discard retrieved units or add unqueried ones. Unsupported construction should refuse. `q2_construction.clj:42–54` can census inner provenance; it does not construct the outer menu.
4. **Executable mappings are missing:** reviewed versioned mappings must bind pattern/README semantics to typed steps. Reuse the interpreter. Ready/cone/unblocks are expressible now; repair priority, seat capacity, bounded discovery, portfolios and reshapes have no faithful Giraffe-only implementation there. Preserve unsupported steps as named/unverifiable or refuse execution; do not replace them with convenient rankings.
5. **Reading pin exists:** exact pass bytes, retained by `outer_observation.clj:114–120,173–180`. **Graph/input bytes exist:** Giraffe `input.edn` is manifest-pinned (`futon3c/src/futon3c/giraffe/run.clj:228,355–370`), but the outer observation currently exposes pass/manifest hashes, not a separate construction graph pin. **Kernel carrier/pin is missing:** retain canonical retraction/landing and transition parameters with code/schema identity. Makeup alone is insufficient: relation→ending and unblocks→gain live in `outer_score.clj:79–89,128–143`; stopped/picked kernels at :91–110; task reach prior in `outer_observation.clj:83–103`. Makeup supplies initial count kits/rates, not all kernel semantics.
6. **Projection must consume construction identity:** record slice/pool/units/edges and reading/graph/kernel pins beside all compared policies. `scripts/wm_run_facts.clj:823–845` currently recovers names from pinned makeup by score position. `mathlib4/DarkTower/WarMachine/Requirements.lean:35–59,338–353` states construction fields and Q2 equalities. Its graph field is described as a *pattern graph* pin: using Giraffe input as this dispatch specifies must declare that outer interpretation and separately bind composition edges, rather than imply Giraffe contains pattern-composition edges.

Byte hashes verified this turn for run `20261101T044121.422Z-730407c5`: pass `a9ad27a8cb6fd0c808413ce639019b117aed746fbcb6b2945560cfc71423ff91`; input `730407c514e2fd21905da2b533d92e046a454372c919fbd89089fc953ebf1667`. Both match manifest entries. No current source in the inspected outer path emits the complete construction tuple.

## Learning state: correction to the question's premise

There is **no stored q keyed by cascade name** here. `outer_state.clj:115–121` initializes global ending counts `:c`, readiness-status-keyed `:state-counts`, gain counts and click count. `outer_score.clj:128–143` derives q from the picked task's status counts. `outer_state.clj:193–212` updates those counts. Retain learned counts and pending/consumed bookkeeping in state; retain initializer kits, eta/omega rates and precision in pinned model configuration, separate from the menu. Makeup :4–27 contains these parameters. Its :29 default reach is ignored by observation (:178), although `outer_state.clj:82–84` still validates that key.

Library-derived ids will not invalidate a nonexistent per-name q table. They **will affect pending learning**: `outer_state.clj:175–183` reconstructs chosen steps by makeup index and checks cascade id; :326–334 checks pending makeup hash; :339–352 refuses a changed makeup. Current migration (:284–314) supports only the precision-only extension. Removing menu from makeup needs explicit state/config migration and retained old construction/steps for pending consumption; preserve learned counts rather than reset them. No migration performed here.

## Undetermined-cone incompatibility

The live first/second cascades gate ready; the third already gates undetermined before sorting cone (`outer-makeup.edn:34,38,42`). Missing metrics refuse at `outer_score.clj:22–29`. A status gate does not create cone. Rank could make ordering computable, but **outer-model :136 also reads unblocks** for the chosen task when its relation yields an ending; changing only the sort does not establish scoreability.

README Giraffe order (:55), scarce seats (:59) and repair click (:63) explicitly include only-what-can-start-now before later relevant ordering. The first is expressible with existing Giraffe facts; the other two still require seat/repair/demonstration semantics. Portfolio (:65), uncertain design (:68) and dated demonstration (:71) do not specify a readiness/status gate. Adding one is a semantic decision, not transcription. Uncertain design is not necessarily undetermined readiness, and its three reshape patterns are not “undetermined then rank.” Do not invent zero metrics or claim all six become executable by adding gates.

## Reader inventory

Search: `rg` for `:cascades` and `:tied-cascades` over src/, scripts/, test/, and futon3c/src, followed by inspection of outer-specific uses. Textual inventory, not proof against dynamically computed keys.

| Reader | Required consideration |
|---|---|
| `src/futon2/aif/outer_select.clj:86,130–173` | Menu, scored members, tie ids, legacy arrays; feed constructed identities. |
| `src/futon2/aif/outer_state.clj:85–96,175–183` | Makeup menu validation and index/id recovery for learning; preserve old pending replay. |
| `scripts/wm_run_facts.clj:813–845` | Pinned makeup reopening and position→name mapping; modern construction projection plus historical fallback. |
| `test/futon2/aif/outer_select_test.clj:10,90,107–115,143,154–188,217–226` | Menu fixtures, ties, identities, all scored and forbidden members. |
| `test/futon2/aif/outer_relation_admission_test.clj:30–31,89` | Historical menu/tie names. |
| `test/futon2/aif/outer_selection_wiring_test.clj:61,117,146,208–239` | Menu fixtures, old tie fixture, reselection retention. |
| `test/futon2/aif/outer_observation_test.clj:215–223,274–280` | Fixture menus and per-cascade task facts. |
| `test/futon2/aif/outer_state_update_test.clj:20`, `outer_state_wiring_test.clj:15` | Menu-bearing learning fixtures. |

No direct outer makeup-menu or tied-cascades reader was found in futon3c/src; `futon3c/aif/flight_record.clj:111` carries inner-flight cascades. Remaining matches below: grep suggests analysis/inner-flight/pilot structures or fixtures, not outer makeup readers. Do not migrate by keyword alone.

Complete matching-file inventory (including writers, fixtures and unrelated structures; line numbers at inspected heads):

- `/home/joe/code/futon3c/src/futon3c/aif/flight_record.clj`: 79, 87, 111.
- `scripts/futon2/wm/extract_outcomes.clj`: 865, 951.
- `scripts/wm04/pilot_result.clj`: 38.
- `scripts/wm_reverse_morphogenesis.clj`: 84.
- `scripts/wm_run_facts.clj`: 832.
- `src/futon2/aif/analysis_cascade.clj`: 163.
- `src/futon2/aif/cascade_prior.clj`: 148.
- `src/futon2/aif/cascade_shape_g.clj`: 640.
- `src/futon2/aif/flight_driver.clj`: 119.
- `src/futon2/aif/flight_runner.clj`: 817.
- `src/futon2/aif/outer_select.clj`: 86, 139, 154, 160, 170, 171, 173.
- `src/futon2/aif/outer_state.clj`: 85, 86, 87, 93, 94, 95, 177.
- `src/futon2/aif/pattern_reliability.clj`: 52, 67.
- `src/futon2/aif/repair_cascade_g.clj`: 171, 178.
- `src/futon2/aif/served_by_reading.clj`: 119.
- `src/futon2/aif/target_field.clj`: 149.
- `src/futon2/aif/target_policy_family.clj`: 74.
- `src/futon2/aif/target_reading_registry.clj`: 124, 125.
- `test/futon2/aif/analysis_cascade_test.clj`: 35, 50, 51, 61, 75, 129, 132, 133.
- `test/futon2/aif/cascade_shape_g_test.clj`: 72.
- `test/futon2/aif/flight_driver_test.clj`: 137, 139, 142, 144.
- `test/futon2/aif/meta_terminal_injury_dispatch_saturation_test.clj`: 87.
- `test/futon2/aif/outer_observation_test.clj`: 215, 220, 221, 222, 223, 274, 280.
- `test/futon2/aif/outer_relation_admission_test.clj`: 30, 31, 83, 89, 92, 94.
- `test/futon2/aif/outer_select_test.clj`: 10, 80, 86, 90, 107, 114, 115, 143, 154, 155, 161, 162, 163, 169, 173, 178, 179, 182, 186, 188, 205, 217, 226.
- `test/futon2/aif/outer_selection_wiring_test.clj`: 61, 117, 146, 208, 219, 229, 239.
- `test/futon2/aif/outer_state_update_test.clj`: 20.
- `test/futon2/aif/outer_state_wiring_test.clj`: 15.
- `test/futon2/aif/pattern_reliability_test.clj`: 12, 35, 37.
- `test/futon2/aif/target_policy_family_test.clj`: 249, 250.
- `test/futon2/aif/wm/cascade_runner_seam_test.clj`: 116.
- `test/futon2/aif/wm/reading_occurrence_identity_test.clj`: 61.
