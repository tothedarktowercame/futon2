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

The first execution attempt generated an in-memory judgement and then refused
while projecting it through `trace/trace-record`: the rank-join boundary found
`rank/149` in the ranked list but absent from the decision softmax map. The
attempt wrote no capture artifact and touched no external trace. The failure is
retained in `failed-attempt.edn`. The follow-up removes that unnecessary second
projection: this packet requires the complete in-memory decision and ranked
actions, which the machinery result already supplies, and is not a trace-writer
test. The mismatch remains a typed finding rather than being hidden.
