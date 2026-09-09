# RUN4 effective policy depths

## Discovery before implementation

The two horizons have distinct consumers. These are pre-change line numbers.

| Path | Effective source |
| --- | --- |
| war_machine.clj:6300,6345 -> efe.clj:635 -> forward_model.clj:294 | Events loaded and nonempty selects 3, otherwise single prediction (1). The forward-model default 3 is bypassed by the explicit K argument. |
| cascade_lane.clj:381 -> rollout.clj:691,661,550 | Caller passes depth 5, top-k 3 and gamma 0.9; rollout's default 2 is not effective here. Both cascade-lane (:473/479) and gap-lane (:539/541) use this shared memoized result. |
| rollout.clj:683 | greedy-one-step explicitly uses 1; independent diagnostic API, not RUN4's cascade caller. |
| arguing_worlds.clj:94; temporal_hierarchy.clj:184 | Separate callers supply their own rollout options; not the supervised stepped RUN4 path. |
| scripts/kill_test_rollout.clj:37; scripts/t1_rent.clj:20–21 | Diagnostics explicitly use 3 and 2 respectively; not RUN4. |

RUN4's stepped entry is run_tick_once.clj:290 -> generate-war-machine -> judge.
The scheduled runner and ants rollout are separate paths, not default-flip targets.
The lane cache must distinguish configured depths from legacy stem-only entries.
Expansion stops on exhausted depth, no survivors, truncation or terminal move;
completed move count is not necessarily the requested horizon.

## Capability and inspection

Joe's revised ruling is anticipation 3 / cascade 5 (SESSION-model-choices,
section 6); the earlier 3/3 choice is historical. This is fixed run-scoped
configuration, not adaptive depth or an optimality claim.

Pass `:policy-depth {:anticipation 3 :cascade-rollout 5}` in judge options,
or set `FUTON_WM_RUN_CONFIG` to the run sheet for the stepped CLI. The reader
accepts top-level `:policy-depth` or the sheet's `[:flags :policy-depth]`.
Both values must be positive integers; malformed supplied configuration
refuses before the judge runs. No configuration or live run was activated here.

The judgement and persisted trace carry `:policy-depth`: configuration,
per-ranked-action anticipation effective depth/fallback reason, and cascade
rollout events collected from both advisory lanes. Anticipation without loaded
events remains effective 1 and records the reason. Cascade events contain the
effective expansion horizon and every search leaf's actual move count and
ending (`:horizon`, `:terminal`, `:truncated`, `:empty`). Terminal moves also
set the substrate's truncated flag; their recorded cause is `:terminal`.
Initially truncated configured inputs stop at zero actual moves. No padding.
Errors and no matching moves are explicitly `:error`/`:not-invoked`, without
claiming an effective completed rollout. The existing memo cache reuses results
within a stem/depth configuration; these events describe that search, not a
claim that a cache hit re-executed it.

Legacy calls retain stem-only cache keys and no new output fields. Configured
calls use separate cache keys. No default horizon was changed. A separate JVM
loaded the committed pre-change rollout producer and compared its serialized
depth-5 output against this change: byte-identical on the five-move fixture.
The configured RUN4 test uses 3/5. A separate cascade-depth-3 control must
change the result and recorded horizon, so agreement at five cannot hide an
ignored configuration or a cache collision. Early-ending tests deliberately
use a horizon of three to check the original one/two-move boundary.
Focused tests also compare serialized scorer outputs with absent configuration
and explicit 3 (the legacy event-loaded horizon).

## Gate status / authorized U12 bit-rot fallback (2026-09-09)

The requested existing rollout + war-machine suite reports 124 tests / 791
assertions, with one failure and zero errors: the pre-existing U12 source pin
in `war_machine_test.clj:1772` expects `51f6de53d7e95d42a42bb3a599e8430d35c29c34c692e27e239884541fa0c846`
for `holes/missions/M-wm-aif-policy-grain-compliance.md`, whose current hash is
`a770d0005af53aa483f2fe2094d363c502eee3c7842d3663af01cf37353ef08e`.
That test requires artifact re-measurement and pin update together, not an
isolated expected-value edit. The cause of this source drift is the B2 adoption
in `27a6dd5b`. Claude-1 subsequently authorized re-measurement, or a wiring-only
commit with the known failure recorded if the U12 producer is bit-rotted
(`invoke-1788983288017-16921-3913a217`). This commit uses that fallback;
the suite is NOT claimed green and neither the pin nor artifact is changed.

The producer executes, but cannot currently produce a truthful refreshed U12
report: its hardcoded clause-c verdict and defect narrative still say the
unsatisfied double scores the same as the satisfied field. The actual current
measurement is `10.000045398900184` versus `4.539889921682063E-5`.
`measurements.edn` repeats the stale reason
`:unsatisfied-pole-unreachable-from-double-observables`. U18 (`5af75e69`)
introduced declared-binarization semantics after the original U12 measurement.
Refreshing this contradictory artifact would require repairing its verdict
producer, not just updating a digest. That repair is stopped for the owner.

The replay uses the unchanged producer forms, excluding its automatic final
`(apply -main *command-line-args*)`, then binds `corpus-files` to just
`data/wm-trace/wm-trace-2026-09-02.edn` before calling `-main`. This preserves
the registered three-record corpus (0a18c4f7, 4abad68c, 801976e7), rather than
silently adding the newer records its default date scan now selects. Outputs
are diagnostic only under `/tmp/u12-remeasure-gIjsJB`; no data files or
historical U12 artifacts are written. Reproduction from the futon2 root:

```sh
clojure -M -e '(require (quote clojure.java.io)) (with-open [r (java.io.PushbackReader. (clojure.java.io/reader "holes/labs/wm-contract/u12_c_mis_falsifier.clj"))] (loop [] (let [form (read {:eof ::eof} r)] (when-not (= ::eof form) (when-not (= (first form) (quote apply)) (eval form)) (recur))))) (with-redefs [corpus-files (fn [] ["data/wm-trace/wm-trace-2026-09-02.edn"])] (-main "/tmp/u12-remeasure-gIjsJB"))'
```

Two fresh JVM runs exited 0 and produced byte-identical contents in all 43
output files (SHA-256 comparison). Deterministic output does not make the
contradictory verdict valid. The existing rollout + war-machine suite was
re-run after this discovery: 124 tests, 791 assertions, exactly the same one
U12 digest failure, zero errors.

The observed mission digest is the current `a770d000...53ef08e` above;
the S4 digest remains
`b85cb1dade5acecccfcb5188106a82908aa61a70f60b1f4b9f6af46873a5f2a9`.

Focused policy-depth + existing rollout + cascade decision/menu tests:
38 tests, 283 assertions, zero failures/errors after the 3/5 revision. clj-kondo: 0 errors/warnings;
check-parens: OK; git diff --check: clean. No registry, worklist, ledger or
data writes. Unrelated untracked files remain untouched.
