# Selective loader review and route decision — codex-26

Reviewed2de72ab8/9cd96667/dc10cbca and c269ea7c receipts. All four source/test/runner/HTTP pins match; retained13assertions, induced14/onefailure exit1, kondo0 and actualparensOK inspected without rerun.

The reported guarantees exceed the implementation:

* Production activate-http-retention! accepts an arbitrary caller-supplied closure whose map claims independently-measured. Repeating the same claimed generation is not an independently installed ingress fence. The generic transaction also permits any target namespace. Missing verifier refusal is not a hard live-use prohibition.
* Rollback test fail-after1 occurs before newly-interned is evaluated, so nil afterwards does not exercise removal of a created Var. Unbound roots and dynamic Var state are not covered. var-get/alter-var-root plus metadata restoration alone are not proof of exact restoration for these cases.
* Loaded-form evidence hashes supplied forms; it does not measure installed roots. PushbackReader is not a line-numbering reader, so advertised source line identity is not established. The isolated success fixture installs simple defs, not the actual selected HTTP dependency graph.

Lead decision: reject this as a deployment mechanism. Do not continue expanding selective hot-loading to satisfy these additional operational assumptions. Keep historical artifact as experimental offline work; disable production entry/targets so current source cannot advertise caller-asserted live authorization. Document guarantees narrowly. The accepted retention runtime implementation itself remains accepted independently.

Next prepare a concrete controlled-restart procedure/script from a separate operator shell, using actual discovered service and upstream ingress controls, exact source/dependency snapshot, complete task quiescence, durable backups, archive preflight and restart/recovery verification. Queue holds alone are insufficient. No restart or live control action is authorized in preparation; actual execution remains later after review. Do not ask Joe to invent the procedure. If available host controls cannot establish a precondition, report the exact unavailable control rather than emitting a ready-to-run command that assumes it.
