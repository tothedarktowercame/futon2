# D 2b production admission

Scope follows the owner's EV-enactment ruling (`76a84bf0`). There is no
production predecessor capable of authorizing token-transition B. The runner
now explicitly inspects previous-trace candidate fields, records each refusal,
and consumes the resulting fresh initialization through
`:selection-certificate :token-belief-input :continuation-belief`.

`token-belief-predecessor/production-authority` consults the real production
E2b verifier. It accepts only the existing typed authority-unavailable result
as grounds for this refusal branch. Other verifier errors propagate; a future
successful production verifier requires the token-transition join to be
reviewed, not automatically admitted. No isolated-test mode is used.

The input receipt is `:wm/token-belief-input-v1`, separate from the frozen
observation-update schema. It records `:carry-no-predecessor` (or explicit
`:carry-domain-changed`), snapshots/digests of the inspected candidates,
`:conditioning-status :not-run`, fresh initialization and continuation.
The inspection scope is explicitly `:previous-trace-only`, not a claim of a
complete history search. Missing fields are recorded individually. The current
full-loop runner writes its trace before its construction checkpoint; that
checkpoint's self-comparison is therefore absent from the normal prior trace,
not recovered from a hypothetical execution. The real self-comparison helper
is exercised separately as a negative control.

There is no observation-update receipt, no assertion that an observation was
missing, and no claim that conditioning ran. The prospective 2a carry remains
recorded and unconsumed. The existing initializer-origin and incoming-value
checks remain intact; the new refusal chain adds validation.

Pre-commit validation passed: predecessor 3 tests/25 assertions, carry 4/30,
scoring receipts 3/45; no failures/errors. The new negative control calls the
real `full-loop-runner/selection-enaction-record`, rejects its `:match`, also
rejects plan/simulation/self-asserted authority, and demonstrates identical
selection/D outcomes despite a disagreeing prior. Other controls cover changed
universe, tampered receipts, and actual trace serialization. Existing scoring
receipt tests exercise the run-record writer. clj-kondo has 0 errors/warnings;
check-parens passes. Registered executions follow the implementation commit;
the execution comparison records both roles and hashes.

No live tick was actuated and no shared JVM was loaded. No positive production
admission fixture exists: that requires the real 2c execution-evidence producer.
