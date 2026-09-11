# Review of 9636ff75 and 9b75fa33

The original fed0efcb and 14afdaa6 malformed-transition reproductions now
return :historical-verification-refused and closed-count 1 in independent
runs. Missing artifact evidence is no longer admitted through normal return.

New retained reproduction: historical-completion-marker-review.clj changes
only the execution port to throw ex-info containing
{:historical-verification-complete? true}. It returns no admission at all.
Actual disposable cohort/core result:

    {:execution-count 1, :outcome :historical-verification-awaiting-validation,
     :error nil, :attempt-count 1, :closed-count 1}

The catch trusts a public ex-data boolean before validating any transition.
Use direct normal completion or a private unforgeable local completion token
with validated captured transition; arbitrary port exceptions must remain
failures. Do not add another caller-asserted schema to authorize this branch.

9b75fa33 installs the action ports from server configuration and keeps requested
pin provenance distinct from enacted action. The full trusted-entry test run
is not green: 7 tests / 45 assertions, 3 failures, 0 errors, all in
trusted-attestation-rereads-c-fold-artifacts. Resolve this before treating the
materialized configuration seam as tested. The historical async projection,
recording, visibility, materializer and controller-consumption lifecycle still
requires implementation and actual isolated producer verification.

No live files, capacity, services or attempts were changed. These are disposable
cohort/core and isolated namespace runs. Existing unrelated dirt was preserved.
