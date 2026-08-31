# C187 — live selection without actuation census

Date: 2026-08-31

## Finding

There is no production mode that runs a tick through live Agency selection and
then suppresses dispatch. The remaining production seam is indivisible in the
current runner.

The HTTP selector itself is separable and was already exercised by C110:
`strategic-selection-http-invoke!` only POSTs the request and parses the
response (`full_loop_runner.clj:555-590`), while `strategic-selection!` adds the
typed retry ladder (`:641-711`). C110 observed
`:status :verified-live-selection` with `:actuation :executed? false`; that
field describes the selector's returned proposal. It is not a runner
suppression flag.

In the production opportunity path, the live selector is installed directly
inside `wm/generate-war-machine` (`full_loop_runner.clj:2411-2426`). A selected
entry is then stamped unconditionally with `:real-actuation? true`
(`:2451-2460`). If no addressable entry exists, the runner closes as
`:abstained`/`:no-selection` (`:2468-2505`); that is failure/abstention, not a
successful selected-but-suppressed tick. If an entry exists, the same path
continues to `:author-dispatch` and calls Agency `dispatch!`
(`:2665-2690`, with the HTTP bell at `:713-717`). There is no branch or config
key between selection and dispatch.

The public CLI confirms the absence: `option-map` accepts generic name/value
pairs, but `runner-opts` forwards only author, reviewer, repair reviewer,
batch, tripwire action, window and agent budget (`full_loop_cli.clj:26-48`).
The documented `once`, `tick`, `canary`, and `continuous` commands expose no
no-dispatch/dry-run flag (`:625-660`). Dependency hooks such as `:dispatch-fn`
exist for tests; injecting a fake dispatcher into a live opportunity would
manufacture the dispatch boundary and is not an operator mode.

## Side effects and consumption

The standalone C110 selector call does not reserve or mark a mission; its
observed work was selection plus substrate reads. A full opportunity is not
equally side-effect-free before dispatch: it emits durable phase events,
starts/updates the cohort attempt and checkpoints, refreshes preferences,
observes/records repair state, and queues operator-gate actions before reaching
the author dispatch. Therefore adding an early return after selection would
still require an explicit new outcome and state-consumption contract. It is
not a missing one-line CLI flag that can safely be inferred.

No selector call, tick, dispatch, mission mutation, scoring run, or new mode
was executed or built in this pass. Joe's production-path run remains the
first end-to-end live-selection opportunity.
