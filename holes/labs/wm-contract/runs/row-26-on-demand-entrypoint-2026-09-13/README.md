# Row 26 bounded on-demand entrypoint

The real click framework already exists in Futon3c. `runner-service/click!`
enforces one in-process flight and invokes Futon2 `run-opportunity!` once.
This packet adds a small operator client for that HTTP boundary and passes its
explicit run id through the existing handler. It does not introduce another
runner.

After separate authorization and a reviewed concrete config, the proposed
command is:

```sh
clojure -M -m futon2.aif.on-demand-entrypoint \
  holes/labs/wm-contract/runs/row-26-on-demand-entrypoint-2026-09-13/run-config.authorized.edn
```

Do not run that command from this packet. The explicit full-loop readiness
prohibition remains unresolved. `run-config.example.edn` is a shape, not an
authorization record, and renaming or editing it cannot authorize execution.

The client sends one POST, then GETs only the returned click identity until its
terminal result. HTTP refusal, single-flight conflict, missing/cross-click
identity, timeout, and terminal loop refusal remain typed; a non-grounded
terminal result is returned unchanged.

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
