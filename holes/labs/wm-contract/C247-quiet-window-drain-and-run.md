# C247 — quiet-window drain and operator-run checklist

Date: 2026-08-31. Owner: `wm-organization`.

This is an ordered, fail-closed checklist. Preparation commands may be run by
the coordinator. The serving-JVM reload and production click are Joe's actions.
No step was performed while writing this document.

## Important current blocker

The sequence cannot yet honestly reach an operational certificate for the
approved HTTP click.

`checks/certify_live_run.clj` accepts only a bounded receipt whose interval
encloses the run and whose command contains `run-tick-once`. The approved
production action is `POST /api/alpha/wm/click`; it executes the already-loaded
runner inside the serving Futon3c JVM and currently emits no per-click bounded
resource receipt. A bounded suite/gate receipt is not evidence about the click
and must not be substituted.

Therefore steps 1–6 below prepare and execute the run, but step 7 is a **STOP**
until the HTTP click has a real resource-receipt producer or the certificate's
typed resource input is deliberately redesigned. This is precisely the
“real bounded resource envelope around the click” boundary C230 listed as not
rehearsed.

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

Use exactly the filled command printed by readiness, including its live-selected
reviewer. Its shape is:

```sh
curl -s -X POST localhost:7070/api/alpha/wm/click \
  -H 'Content-Type: application/json' \
  -d '{"reviewer":"SELECTED","trigger":"duree-click-on-demand"}'
```

This is Joe-only. Save the returned click id and start time. Poll without
starting another click:

```sh
curl -s localhost:7070/api/alpha/wm/click
```

Require `running? false` and a non-null terminal `last-result`. A service
failure or a lifecycle that does not close is stop-and-fix; never click again
merely to obtain a cleaner result. Identify the emitted `TickRunRecord` by its
recorded start time and capture its exact `:run/id`; do not guess “latest”.

## 7. STOP: certificate awaits the missing click resource receipt

Once that producer exists, the intended command is:

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

Reserve **20 minutes after drain**, plus the click's actual work time. Measured
components are Futon2 CI about 102–106 s; the older 39-check workspace gate
about 33–41 s, with the current 78-check bounded run observed at about 149 s;
the chain rehearsal adds 5.5–5.7 s; preflight is seconds. Futon3 suite, the
second gate currently embedded in readiness, reload transport, and live click
duration add variable time. Twenty minutes is a window budget, not a timeout.

Never executed together against production: authenticated Drawbridge reload,
serving-JVM identity transition, live Agency HTTP click, live selector/author/
reviewer dispatch, live store writes, and a per-click bounded resource receipt.
The throwaway rehearsal proves the internal seams and mismatch refusal; it does
not turn these production-only boundaries into witnessed facts.
