# Independent author-side review of lead patch 232df039

Reviewed without rerunning accepted checks. The two-file diff requires every
stored and recovered deferred record to have a nonempty string identity, map
payload, pending status, and payload requested-job-id equal to the record key.
This closes the recovery-side identity gap as well as the write path.

Current file SHA-256 values exactly match lead-projection-gates.json:

- invoke_ingress_controller.clj: `966dba69d13c3f39431dd73369716e53d07a171e09b09818529113c00db52756`
- invoke_ingress_controller_test.clj: `2d8f83ca7398631a47a8a045b2144a1ba95d52a6aa11357cb354305d42ad9cee`

Retained raw output records 11 tests, 38 assertions, zero failures/errors;
kondo zero warnings/errors; actual explicit-path parens `OK`. The patch is
accepted for the isolated controller machinery only and makes no serving or
deployment claim.
