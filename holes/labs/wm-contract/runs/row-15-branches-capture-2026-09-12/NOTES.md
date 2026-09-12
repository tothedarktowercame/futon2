# Row 15 internal machineAction branch capture

This packet captures proof inputs only. `capture_branches.clj` performs one
isolated machinery tick with `:trace? false` and policy details dynamically
enabled. It writes no daily trace, cohort record, or serving-JVM state.

The natural tick is retained separately from direct calls to the production
pure function `futon2.aif.policy/select-action`. Each direct artifact is labelled
`:direct-invocation-of-production-fn`; it is not represented as a tick. The
ranked actions and habit state originate in the one machinery capture. Direct
records vary the selector options needed to expose controller-head,
full-score/first-max, habit/last-max, no-op abstention, and the requested
posterior with F_pi absent. The F-present invocation uses a zero F vector over
the real captured candidate domain and labels it a commissioned zero control;
it is not claimed as a naturally observed F_pi vector.

The tie control duplicates one real controller score onto a second real
candidate and records both indices and the score. This is a commissioned
mutation retained to expose deterministic tie behavior, not an observed tie.
