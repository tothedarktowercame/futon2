# Actual historical branch with cohort enabled — changes required

Reviewed 354fd881/3c17b578. Independent full runner suite: 129 tests,
610 assertions, zero failures/errors. The new historical branch test uses
isolated-runner-opts, which disables cohort recording.

Retained reproduction historical-cohort-repro.clj enables a disposable
activated explicit cohort, using the actual run-opportunity! wrapper, core,
checkpoint and close writers. Agency and side effects use existing hermetic
test ports. Result:

    {:execution-count 1, :outcome :incomplete,
     :error "invalid close outcome", :attempt-count 1, :closed-count 0}

full-loop-cohort/outcome-kinds excludes historical-verification-awaiting-validation.
The admission action therefore executes before its close is rejected. The
exception-driven return also runs the generic failure reporting path, assigning
failure semantics and parking a transition that actually means verification
was admitted. A mere outcome-set addition is insufficient: define the explicit
non-resolution result, evidence-bearing adjudication and close, truthful
not-performed author/build cells, and failure-free control flow on that result.
Malformed/missing admission evidence must still fail closed. Align tripwire,
run-record, projection, terminal classification, recording and visibility.
No grounded-change or U88 success claim may be substituted.

Check RUN4 identity too: stop-line selection bypasses resolve-pinned-selection;
the historical branch must retain valid authenticated requested-pin provenance
and actual selected action without inventing an executed U88 task. The complete
materialized serving path, with real cohort enabled and actual store transition,
is the gate. The core-only test cannot establish this boundary.

Original failed attempts, stores and capacity untouched. This reproduction
used disposable cohort activation only; no service/restart/credential access.
