# C247 — quiet-window drain and operator-run checklist

Date: 2026-08-31. Owner: `wm-organization`.

This is an ordered, fail-closed checklist. Preparation commands may be run by
the coordinator. The serving-JVM reload and production click are Joe's actions.
No step was performed while writing this document.

## External observation boundary

C264/C271 closed the former resource-receipt blocker. The operator must submit
the click through `wm_click_resource_observer.clj`, not through a bare `curl`.
That separate process samples the shared serving-JVM cgroup before, during and
after the click, then binds its envelope to the exact click and run IDs. A
suite/gate receipt remains invalid evidence about the click.

## 1. Declare and prove quiescence

Stop dispatching first. Then require all of the following simultaneously.

```sh
for repo in /home/joe/code/futon2 /home/joe/code/futon3c \
            /home/joe/code/mathlib4 /home/joe/code/p4ng \
            /home/joe/code/futon3; do
  echo "== $repo"
  git -C "$repo" status --porcelain=v1
  git -C "$repo" rev-parse HEAD
done

cd /home/joe/code/futon2
bb checks/lane_registry_check.clj
python3 /home/joe/code/futon3c/scripts/bg.py list
python3 /home/joe/code/futon3c/scripts/bg.py test-list
```

Acceptance:

- every porcelain section is empty;
- every lane is explicitly idle and the lane check exits 0;
- no ordinary background job is running for a delivery;
- no bounded unit has `ActiveState` `active` or `activating`;
- record the five printed HEADs in the operator note.

If a tree changes, a lane is holding work, or a job is active, the window has
not started: wait/drain and repeat step 1. Do not “accept” the moving state.

### C290 live drain list (2026-09-01T00:39Z)

This is an observation to execute, not a permanent allowance. Re-run the
commands above after every item; new work replaces this list rather than being
silently outside it.

1. Freeze new dispatches.
2. Let `wm-organization` finish `C290-window-cost`; do not kill it. The other
   three recorded jobs have already finished.
3. Record the completed transitions for `wm-nouns/C291`, `wm-verbs/C293`, and
   `wm-evidence/C292`, then record `wm-organization/C290` after this delivery.
   All four registry rows must finish explicitly idle.
4. Resolve Futon2's modified
   `holes/labs/wm-contract/workflow-report.edn` through its owning generator or
   commit; do not discard it as “just generated.”
5. Resolve Futon3c's four untracked
   `pattern-library-zai-scribe-{f65-b01J04,f66-b03J01,f68-b90A03,f69-b93A01}.md`
   files by their owning lane.
6. Resolve p4ng's modified `sec-lane-campaign-generated.tex`,
   `sec-lean-holes-generated.tex`, and `sec-model-coverage-generated.tex`, plus
   untracked `aif-control-map-live.pdf`, through their owning generators and
   commits.
7. Mathlib4 and Futon3 were clean at observation time; verify them again rather
   than carrying that fact forward.
8. No bounded job was active after the C290 measurement. If `test-list` shows
   a later active job, let it reach a terminal receipt; do not kill it merely
   to make the list empty.
9. Classify every workspace-gate inventory discovery before the quiet run. The
   measurement saw five newly unclassified check files; a green inventory must
   establish their committed disposition rather than exclude them ad hoc.

## 2. Run the cross-repository gate once, bounded

```sh
cd /home/joe/code/futon2
make workspace-gate
```

Acceptance is a terminal `RECEIPT` with inner exit 0, outer exit 0, verdict
`pass`, resource status `clean`, and stable clean repository basis. In this
window, `repository-basis-changed` means quiescence failed: close the window
and restart at step 1. A named gate failure on a stable basis means stop and
fix that check before opening another window.

The raw gate also records `PROVENANCE` at start, `PROVENANCE-FINISH` at end,
and a `BASIS` comparison. `BASIS-NOT-STABLE` is loud even when all individual
checks pass; it qualifies the raw verdict rather than changing its exit code.
During this quiet window, any raw `:moved` or `:unavailable` basis has the same
meaning as the bounded wrapper's `repository-basis-changed`: the window
closed, so restart at step 1.

The gate includes the reload → click → certificate rehearsal and its mismatch
control. It does not execute production.

## 3. Produce suite receipts for the settled trees

Futon2 supplies the receipt required by `runner-reload-preflight`:

```sh
python3 /home/joe/code/futon3c/scripts/bg.py launch-test \
  'clojure -T:build ci' --agent quiet-window --label quiet-futon2-ci \
  --dir /home/joe/code/futon2 --window production
```

Futon3 supplies the separate receipt required by `run-readiness`:

```sh
python3 /home/joe/code/futon3c/scripts/bg.py launch-test \
  'clojure -X:test' --agent quiet-window --label quiet-futon3-suite \
  --dir /home/joe/code/futon3 --window production
```

For each returned job id, wait with:

```sh
python3 /home/joe/code/futon3c/scripts/bg.py test-status JOB_ID
```

Acceptance is inner 0, outer 0, `verdict=pass`, `resource-status=clean`, with
identical clean start/finish repository bases matching the HEAD recorded in
step 1. A test or resource failure means stop and fix. A changed basis means
the window closed; restart at step 1.

## 4. Release the reload command

```sh
cd /home/joe/code/futon2
make runner-reload-preflight
```

It must report all six checks passing, `READY`, and print—not withhold—the
reload command. A `tested-commit` failure means step 3 did not produce a clean
receipt for the current Futon2 HEAD. Any other failure is a stop-and-fix
condition. Never copy the `withheld-command` from a refused report.

## 5. Joe reloads the runner

The command the preflight is expected to release is:

```sh
cd /home/joe/code/futon3c && \
clojure -M:dev-admin load-file \
  /home/joe/code/futon2/src/futon2/aif/full_loop_runner.clj
```

This is Joe-only. Immediately afterward, run readiness through the bounded
service so the roughly 1 GB rehearsal/gate JVM does not compete with Agency:

```sh
python3 /home/joe/code/futon3c/scripts/bg.py launch-test \
  'make run-readiness' --agent quiet-window --label post-reload-readiness \
  --dir /home/joe/code/futon2 --window measurement
```

Inspect its terminal receipt and log. `serving-runner-code` must change from
`UNAVAILABLE` to an available, clean, stable identity equal to the tested
Futon2 commit. Remaining `UNAVAILABLE` means the reload was a silent no-op;
identity mismatch means `UNVERIFIED`. Both are stop-and-fix conditions; do not
click.

Historical implementation note (superseded): `run-readiness` reran the
workspace gate rather than consuming step 2's receipt, so the apparatus ran the
gate twice.

2026-08-31 amendment (C259): `run-readiness`
now consumes step 2's bounded receipt and does not rerun the workspace gate.
Reuse requires an exact clean content basis for all four repositories covered
by the gate (`futon2`, `mathlib4`, `p4ng`, and `futon3`), plus a passing
inner/outer verdict and clean resource status. A missing legacy provenance
record, a dirty current tree, or any repository-basis mismatch is `UNVERIFIED`;
readiness refuses rather than guessing or rerunning.

## 6. Joe clicks only on READY

Use the live-selected reviewer printed by readiness. Choose a new explicit
receipt filename ending in `.receipt.json`, then run:

```sh
cd /home/joe/code/futon2
bb -cp . checks/wm_click_resource_observer.clj \
  holes/labs/wm-contract/wm-click-resource-YYYYMMDDTHHMMSS.receipt.json \
  SELECTED
```

This is Joe-only and performs the POST and polling. Require a clean receipt
with scope `shared-serving-jvm`, exact `click-id`/`run-id`, a terminal result,
and a readable matching run record. Save the printed `run-id`; do not guess
“latest”. Any unavailable/dirty observation, lifecycle failure, or identity
mismatch is stop-and-fix; never click again merely to obtain a cleaner result.

## 7. Certify the exact run

Use the exact ID printed by the observer:

```sh
cd /home/joe/code/futon2
make certify-run RUN_ID=<exact-uuid-from-TickRunRecord>
```

Acceptance will be a uniquely matched run and resource receipt, valid topology
pins, zero undeclared hops, clean resource status, and certificate verdict
`pass`. Missing/ambiguous evidence or a failing certificate means stop and
preserve the run; do not re-click and do not manufacture a receipt.

## Failure classification

- **Window closed; restart at step 1:** any repository change, new dispatch,
  active unexpected job, or `repository-basis-changed` after quiescence.
- **Stop and fix before another window:** stable-basis gate/test failure,
  resource-limit failure, refused preflight, reload no-op/mismatch, readiness
  not READY, click service failure/non-closure, or certificate failure.
- **Operator choice, then recheck without changing code:** no reviewer is
  available. Select an actually available reviewer and rerun readiness; if any
  tree moved meanwhile, restart at step 1.

## Duration and production-only boundaries

The C290 component measurements sum to **6 min 24 s from the quiescence probe
through certification, excluding reload and click**:

| Component | Observed wall time |
|---|---:|
| Quiescence probes | 0.67 s |
| Bounded workspace gate, 119 results | 215.78 s |
| Bounded Futon2 CI | 90.38 s |
| Bounded Futon3 suite | 36.55 s |
| Reload preflight | 2.77 s |
| Readiness (consuming, not rerunning, the gate receipt) | 37.46 s |
| Certificate generation | 0.04 s |

The workspace gate is the dominant cost: **3 min 36 s, 56%** of measured
pre-click work. Its run was intentionally the sole bounded job. It was not a
valid gate verdict: concurrent commits moved Futon2 and p4ng, and inventory
found five unclassified checks. The elapsed time remains an observed cost;
the verdict does not become evidence for a settled tree. The suite timings are
likewise execution-cost observations, not substitutes for the clean receipts
required above.

Reserve **10 minutes after drain to reach READY**, then add the production
reload and click time. Ten minutes gives roughly 56% headroom over the measured
6:24 preparation and is a planning allowance, not a timeout. There is no
defensible end-to-end bound yet for the Joe-only part: authenticated reload and
the production selector/author/reviewer path have never run together. The
observer polls terminal status for about 60 seconds after click acceptance,
but its initial HTTP request has no independently demonstrated production
latency bound; do not present that polling budget as a bound on the click.

Never executed together against production: authenticated Drawbridge reload,
serving-JVM identity transition, live Agency HTTP click, live selector/author/
reviewer dispatch, live store writes, the external shared-serving-JVM resource
envelope, and certification of that exact production run. The throwaway
rehearsal proves the internal seams, complete/incomplete distinction, temporal
enclosure, and identity-mismatch refusal; it does not turn these
production-only boundaries into witnessed facts.
