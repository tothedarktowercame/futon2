# Row 26 bounded on-demand entrypoint

The real click framework already exists in Futon3c. `runner-service/click!`
enforces one in-process flight and invokes Futon2 `run-opportunity!` once.
This packet adds a small operator client for that HTTP boundary and passes its
explicit run id through the existing handler. It does not introduce another
runner.

Settlement `1d84fd66` permits one reviewed bounded build-phase machinery test.
It does not make an unreviewed config adequate. After lead review resolves the
preflight gaps below, the concrete command is:

```sh
clojure -M -m futon2.aif.on-demand-entrypoint \
  holes/labs/wm-contract/runs/row-26-on-demand-entrypoint-2026-09-13/run-config.preflight.edn
```

Do not run that command from this packet. `run-config.preflight.edn` is an
execution proposal, not evidence that the loaded process has the required
settings. The older blanket-prohibition sentence is superseded by the bounded
settlement; the concrete setting gaps remain blockers until reviewed.

The client sends one POST with an explicit transport timeout, then GETs only the
returned click identity within one whole-client deadline. It never retries an
ambiguous POST. Acceptance/status/terminal JSON and server run-id observations
are validated. Timeout means observation stopped; it explicitly does not claim
the daemon worker was cancelled. HTTP refusal, single-flight conflict,
missing/cross-click identity, malformed response, timeout, and terminal loop
refusal remain typed; a non-grounded terminal result is returned unchanged.

The request's `:run/id` is reported as `:run/requested-id`. Only the service's
terminal `:run-id-observation` can populate result `:run/id`. This works with
the already-live endpoint and does not depend on deploying the later HTTP
run-id passthrough commit.

## Concrete preflight

`run-config.preflight.edn` fixes one opportunity and the seats `zai-5`
(author), `claude-15` (independent reviewer), and `codex-26` (repair reviewer).
Read-only exact-agent GETs on 2026-09-13 found all three invoke-ready; readiness
must be sampled again immediately before execution because it is mutable.

The required section-5 settings are anticipation horizon 3, cascade rollout
depth 5, `FUTON_WM_TRACE_POLICY_DETAILS=1`, `FUTON_WM_FPI_DARK=1`, and
`FUTON_WM_FPI_POSTERIOR=1`. Source establishes those names and the 3/5 ruling,
but no permitted read-only endpoint establishes their effective values in the
loaded process. The click status reports serving runner code identity
`:unavailable`. These are the remaining preflight blockers; absence of an
observable setting is not evidence of either its value or its default.

Expected output destinations from source are the phase log, Futon2 run-record
directory, cohort attempt directory, trace path, repair and Morning Brief
stores, plus Futon3c click-run bindings and terminal/historical projections.
Their effective runtime paths also need a pre-click readback; code defaults do
not prove loaded-process configuration.

## Same-run artifacts available from the existing call

The runner can emit phase events/log rows, cohort selection/construction/build/
adjudication checkpoints, a terminal run record keyed by `:run/id` and
`:click/id`, trace output when produced, repair obligations and their histories,
Morning Brief items, construction/build/review job evidence, grounding witness,
and the Futon3c durable click-to-run binding plus RUN4 terminal/historical
projections. Availability depends on the phase reached; one invocation does not
guarantee every artifact or closure.

Rows 13/14/15/23 still require acquisition and qualification beyond this
entrypoint: complete depth/horizon records; machine-Q/C and EFE injection at the
actual scoring consumer; belief/action/selector measurement records including
the reason-bearing envelope; and exact selected-to-enacted correspondence.
Existing R9 independence and actuation refusals remain in force. This client
does not supply, weaken, or reinterpret them.
