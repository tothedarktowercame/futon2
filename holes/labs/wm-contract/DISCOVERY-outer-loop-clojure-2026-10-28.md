# Outer-loop Clojure correspondence (2026-10-28)

Read-only discovery for the outer task-selection loop specified by
`DarkTower/WarMachine/R20Step.lean` at `d845c84fb`.  This note distinguishes
reusable computations from similarly named remnants which do not carry the
new loop's state.

## 1. G: risk, ambiguity, and novelty

The direct Clojure counterpart of `CascadeEFE.stepG` is
`futon2.aif.cascade-g/step-g`.  The namespace explicitly describes itself as
finite categorical G over supplied model inputs and says it performs no model
discovery, authority minting, enactment, or selection
(`src/futon2/aif/cascade_g.clj:1-6`).  Its one argument is a map
`{:q ... :a ... :c ... :context ...}`; `q` and `a` use the
`categorical-ambiguity` contracts, `c` is
`{:model :support :mass :provenance}`, and context must contain
`:policy/id`, `:occurrence/id`, and `:point`
(`src/futon2/aif/cascade_g.clj:50-66`).  It validates the supplied finite
distributions, constructs the joint and outcome marginal
(`src/futon2/aif/cascade_g.clj:67-78`), and returns rational enclosures for
`:risk`, `:ambiguity`, and their sum `:g`
(`src/futon2/aif/cascade_g.clj:79-102`).

This is a pure calculation apart from typed refusal exceptions.  It can score
the outer model without modification if the caller translates
`doable | blocked` into `q`'s state support, translates `(ending class, gain)`
into the outcome support, and supplies the outer observation kernel and learned
preference as `a` and `c`.  It does **not** build or certify those inputs.  For a
multi-point schedule, `cascade-g/total-g` maps `step-g` over an explicitly
supplied nonempty schedule and sums it (`src/futon2/aif/cascade_g.clj:104-125`).

The retained `PatternThetaNovelty` analogue is
`futon2.aif.cascade-coapply-g/beta-novelty`, which computes the documented
positive-count approximation `1 / (2 * (alpha + beta))`
(`src/futon2/aif/cascade_coapply_g.clj:14-21`).  Its
`expected-novelty` weights that quantity by a pre-step belief over enabled
frontiers (`src/futon2/aif/cascade_coapply_g.clj:47-54`), and the existing
inner scorer subtracts the result from raw risk plus ambiguity
(`src/futon2/aif/cascade_coapply_g.clj:56-94`).  `beta-novelty` is reusable for
R20Step's state/gain Beta counts.  `expected-novelty` is not directly reusable:
it expects an inner co-application candidate, occurrence counts, and
`cascade-model-manifest/enabled-frontier`, rather than the outer state and gain
tables (`src/futon2/aif/cascade_coapply_g.clj:30-54`).  The outer adapter must
perform R20Step's own expectation using `beta-novelty`; it must not call the
inner frontier wrapper and relabel the result.

The retained inner `compare-at-horizon` also enforces the fixed ruled class
schedule (`src/futon2/aif/cascade_coapply_g.clj:98-110`), so it is not the new
outer scorer.  The fixed terminal kit itself remains
`{:focused 55/100 :related 35/100 :unrelated 5/100 :stop-the-line 5/100}`
(`src/futon2/aif/cascade_shape_g.clj:31-57`).

## 2. Giraffe facts

`futon3c.giraffe.missions/missions->input` reads mission documents and an edge
file and returns the pass input plus typed rejects and status-mapping evidence
(`../futon3c/src/futon3c/giraffe/missions.clj:230-275`).  Its default source is
the canonical repository set under `/home/joe/code`
(`../futon3c/src/futon3c/giraffe/missions.clj:44-51`).

`futon3c.giraffe.pass/pass` is the pure computation.  Its output contains a
per-item `:readiness` map, `:cones`, `:unblocks`, and an ordered summary
(`../futon3c/src/futon3c/giraffe/pass.clj:323-336`).  Each `:order` row carries
the readiness status and the numeric counts of its cone and one-step unblocks
(`../futon3c/src/futon3c/giraffe/pass.clj:300-321`).  A caller can therefore
form `GiraffeFacts` as:

* `ready`: `true` only when `(get-in pass [:readiness id :status])` is
  `:ready`; retain `:not-ready` and `:undetermined` separately before the final
  Boolean projection;
* `unblocks`: `(count (get-in pass [:unblocks id]))`;
* `cone`: `(count (get-in pass [:cones id]))`.

The command runner composes `missions->input`, `pass`, and `write-run!`
(`../futon3c/src/futon3c/giraffe/run.clj:280-322`).  `write-run!` persists
`input.edn`, `rejected.edn`, `pass.edn`, and `report.md` with hashes in a
manifest (`../futon3c/src/futon3c/giraffe/run.clj:205-246`), and `verify-run`
recomputes those hashes and reports missing or changed bytes
(`../futon3c/src/futon3c/giraffe/run.clj:248-271`).  Thus the recorded run
`/home/joe/code/storage/giraffe/20261028T154151.285Z-b916cd4e` can be consumed
from `pass.edn` only after its `manifest.edn` has passed `verify-run`.

Futon2 has no source dependency on futon3c.  In the serving futon3c JVM both
repositories are on the classpath; the existing futon2 pattern is to resolve a
futon3c producer dynamically and fall back to an external read, as shown by
`ticket-readiness-inputs/fetch-pipeline-snapshot`
(`src/futon2/aif/ticket_readiness_inputs.clj:36-67`).  Therefore an adapter in
that JVM can use `requiring-resolve` for
`futon3c.giraffe.missions/missions->input` and
`futon3c.giraffe.pass/pass`.  A standalone futon2 process cannot assume those
namespaces exist and should consume the verified run files instead.  Neither
route is currently wired into `run-ordinary-opportunity!`.

## 3. Relation to focus and reach

The retained relation producer is `futon2.aif.focus-receipt/classify-target`.
It is explicitly shared by scoring and close-receipt paths and derives mission,
ticket-parent, and repair-target relations, returning `:unknown` rather than
guessing when it cannot resolve one (`src/futon2/aif/focus_receipt.clj:270-278`).
Its result records `:target`, `:class`, `:relation`, and `:derived-via`
(`src/futon2/aif/focus_receipt.clj:362-377`).  The scorer projection is
`:focus -> :focused`, `:associated -> :related`, and
`:useful-elsewhere -> :unrelated`
(`src/futon2/aif/focus_receipt.clj:108-122`).  The carried/discovered current
focus is resolved by `decision-focus` and `decision-focus-info`
(`src/futon2/aif/focus_receipt.clj:466-512`).

A task-level reach probability is absent.  `classify-target` returns no reach
field (`src/futon2/aif/focus_receipt.clj:362-377`), and the Giraffe output is
limited to readiness, cone, unblocks, cycles, anomalies, and order
(`../futon3c/src/futon3c/giraffe/pass.clj:323-336`).  The current outer-model
design accordingly says reach is supplied from outside for now
(`holes/labs/wm-contract/NOTE-outer-G-computation-2026-10-28.md:29-39`).
Inner cascade reach/prediction data is about tactical policy execution and is
not a task-level producer.

## 4. Learned C

There is no retained runtime carrier for learned ending-class counts.  The
current class model still installs the fixed terminal 55/35/5/5 distribution
at the terminal horizon (`src/futon2/aif/cascade_shape_g.clj:31-57`), and the
inner co-application scorer explicitly describes and checks that ruled fixed
schedule (`src/futon2/aif/cascade_coapply_g.clj:98-110`).  Those values are a
model input, not updated counts.

The deletion commit `a04b157d2` removed
`src/futon2/aif/outer_learning_runtime.clj` together with
`outer_target_belief.clj`, `outer_target_score.clj`, `outer_loop.clj`, and the
outer selector (the deletion is listed in that commit's name-status record).
R20Step's learned `c`, state counts, and gain counts therefore need a new
carrier conforming to the new step function; reusing the fixed class schedule
would silently discard learning.

## 5. Carried state and persistence

`futon2.aif.outer-learning-manifest` survived the deletion.  Its default file
is `data/wm-learning/run-record-manifest.edn`, and it describes itself as an
append-only authority over admitted run records
(`src/futon2/aif/outer_learning_manifest.clj:1-21`).  `record!` pins a completed
run record and its derived `.learning.edn` projection by hash, refuses drift,
and appends the immutable entry (`src/futon2/aif/outer_learning_manifest.clj:42-97`).
The runtime supplies this path to task-backed runs
(`src/futon2/aif/full_loop_runtime.clj:520-533`), and terminal publication calls
`record!` only after the run record exists
(`src/futon2/aif/full_loop_runner.clj:9260-9269`).

That manifest is historical publication, not a per-click state ledger: its
entries contain record/projection paths and hashes, not state counts or gain
counts (`src/futon2/aif/outer_learning_manifest.clj:23-28,52-62`).  The old
stateful implementation `outer_learning_runtime.clj` was deleted by
`a04b157d2`; the manifest was not.  A new implementation can derive the next
R20Step state by folding the manifest's pinned projections, or add a dedicated
atomically written, source-pinned state ledger beside it.  It must not mutate
old manifest entries or treat their mere presence as the new counts.

## 6. Entry into the ordinary run

The exact seam is the absent branch of
`run-ordinary-opportunity!`.  Today it checks `(get-in opts [:task :id])`,
returns `{:absent :no-task-selection-policy}` when missing, and otherwise calls
`run-ordinary-opportunity-with-task!` unchanged
(`src/futon2/aif/full_loop_runtime.clj:502-512`).  The task-selection step
belongs in that missing-task branch:

1. obtain the current outer state and observation;
2. run the R20Step-equivalent selector once;
3. on a selected id call
   `run-ordinary-opportunity-with-task!` with
   `(assoc opts :task {:id selected-id})`;
4. retain a typed no-selection/refusal when no cascade picks a task.

This placement is before the task-backed function mints the run id and installs
the feedback, graph, learning-manifest, and timing defaults
(`src/futon2/aif/full_loop_runtime.clj:514-539`).  Caller-supplied tasks should
continue directly down the existing branch; the new selector must not replace
or rescore them.

## Reuse boundary

Reusable now: `cascade-g/step-g`, `cascade-coapply-g/beta-novelty`,
`focus-receipt/classify-target`, Giraffe's mission adapter and pure pass, and
the append-only run/projection manifest.  Missing now: task reach, learned C,
the new outer state/gain carriers, their persistence/fold, and the call from
the missing-task branch.  The deleted outer-selector namespaces should not be
restored: `a04b157d2` removed a different architecture that built/scored inner
work before selecting the task.
