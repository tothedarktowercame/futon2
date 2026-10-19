# Serving source observation — 2026-10-19

Coordinator codex-1 performed read-only HTTP status and serving Drawbridge
`runner-source-drift` evaluation after futon2 `4412640de` and futon3c
`f6154aa11`. No require/reload, graph rebuild, route execution or click occurred.

`GET /api/alpha/wm/click` returned `running? false`, phase null, last click
`wm-click-a3cf0c98-d133-499f-8090-eed79cd56a03`, last outcome build-failed.
Ordinary budget was allocated 113, consumed 113, available 0; unchanged by
these reads. This is a status observation, not renewed click authority.

Serving `(futon2.aif.full-loop-runner/runner-source-drift)` returned:

- runner source-check: drift
- loaded SHA: `11410d690884253888b6d51d55ed03b12cfde8192060634c41338568bc26af91`
- canonical SHA: `9bb6bf1896eb3818a124522cd7fc0ae7dc46739b5e5843bb303e5b43dbe97905`
- namespace statuses: 69 current, 7 stale, 27 unavailable, 4 unregistered
- stale: d-predecessor-task-authority, full-loop-runner, full-loop-runtime,
  meta-live-outer-selector, meta-pipeline-selector, trace, tripwire

Counts describe the serving process's current identity-report scope, not all
runtime dependencies. Family-selection was registered but had no canonical
path in this loaded inventory; its comparison was unavailable. Thus the
landed inventory correction is not itself proof of loaded inventory freshness.

Canonical futon3c has unrelated uncommitted changes in
`src/futon3c/wm/ordinary_click_budget.clj`; those were neither changed nor
loaded. Existing source-dirty exclusion must be retained during any later
reload. Missing attribution is a measurement gap, not a blanket veto.

Next bounded step is a zero-click reload plan over landed clean sources with
explicit exclusion of dirty files, followed by serving identity remeasurement.
This observation does not authorize starting the repair-loop launcher (which
can fire clicks), allocating budget, or overwriting the dirty budget source.
