# Review of 9942a900: closure repaired, admission contract incomplete

The original fed0efcb reproduction independently exits zero and records one
closed cohort attempt, with :historical-verification-refused. This fixes the
invalid-close-outcome failure for that malformed transition.

The retained historical-transition-review.clj changes only the execution port
result to include its requested execution identity. It intentionally still
omits :verification-artifact. Actual runner and activated disposable cohort
accept it as :historical-verification-awaiting-validation, with adjudication
ground {:kind :historical-verification-admission :evidence nil}.
The runner guard validates schema, repair ID, status and attempt identity but
not the verification artifact or its binding to the candidate.

The same result carries :failure-kind
:historical-verification-awaiting-validation, :failure-stage :construction,
and :error "Historical repair verification admitted for validation".
The exception still goes through the generic catch and failure-kind-from;
suppressing the morning brief failure field does not clean the result data.

Required correction: validate the authoritative admission shape and exact
candidate/artifact identities, reject missing/foreign artifact evidence, and
close normal historical admission without synthesizing exception/failure data.
Use a real immutable store transition in the positive runner test.

The materialized serving gate remains unperformed. Install server-owned action
ports and separately retain authenticated requested-pin provenance and actual
selected historical action. Never label that action as executed U88. Verify
cohort close, run record, durable typed evidence, recording and visibility with
the real async wrapper. Any unsupported classification remains unknown.

Both reproductions use disposable cohort storage and hermetic repair ports.
No live capacity, failed attempt, service or repair record was changed.

Independent full runner suite: 129 tests / 611 assertions, zero failures or
errors. Retained reproduction lint: zero errors/warnings.
