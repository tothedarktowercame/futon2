# Packet 5 — combined WM entrypoint receipt

Base: `5df5853eaeeaf62557e43784aa2da8df4c8540f8`.

## Determination and production call graph

Before this change `:wm-scheduled` reached
`full-loop-cli tick -> full-loop-runtime/run-opportunity! ->
full-loop-runner/run-opportunity!`, but the CLI supplied only a trigger and
optional casts. It did not require the outer-policy evidence provider or a
pinned scheduling authority. The historical `wm_scheduled_run.clj` route is a
judgement/trace diagnostic and does not run the combined lifecycle.

The canonical scheduled route is now:

`wm_full_loop_cron.sh -> :wm-scheduled -> combined-wm-entrypoint scheduled ->
preflight -> full-loop-runtime/run-opportunity! -> full-loop-runner`.

The adapter delegates once and owns no selection/build/review logic. It binds
one run/opportunity id, explicit mode and trigger, all three cast identities,
the exact evidence-provider path, a content-pinned config, a pinned deployment
opportunity authority, and an explicitly zero ordinary-click budget. Runtime
refusal/incompleteness is returned unchanged inside the process receipt and
has nonzero process semantics. Unit logic never calls `System/exit`; only
`-main` translates `:exit-code` at the process edge.

The authoritative serving route remains
`POST /api/alpha/wm/click -> futon3c wm.runner-service/click! ->
full-loop-runtime/run-opportunity!`; futon3c continues to own ordinary-click
admission, budget issuance, and click/run binding. The direct `:wm-combined
on-demand` surface is explicitly configured and has zero ordinary-click
authority; it does not impersonate that serving route.

`wm_scheduled_run/run-judgement-only!` remains available only through
`:wm-judgement-only` or the adapter's explicit `diagnostic` mode.

## Deployment / re-arm

No production authority file was fabricated or committed. Scheduling remains
deliberately disarmed until deployment supplies `FUTON_WM_RUN_CONFIG` pointing
to a sealed `:wm/combined-entrypoint-config-v1` with readable evidence-provider
path, explicit casts, correct trigger/mode, pinned opportunity authority, and
`:ordinary-click-budget 0`, and installs/enables the scheduler. Missing or
drifted configuration refuses; there is no judgement-only fallback.

No live click was launched. No full suite was run.

## Focused evidence

- `futon2.aif.combined-wm-entrypoint-test`: 6 tests, 34 assertions, 0 failures,
  0 errors.
- Includes scheduled dispatch, diagnostic separation, missing configuration,
  mode/trigger/cast transport, one identity, exactly one runtime invocation,
  zero click budget, faithful refusal, and nonzero process semantics.
- Static gates and the post-commit registry warrant are recorded in the final
  delivery after the commit that fixes this receipt's scope.
