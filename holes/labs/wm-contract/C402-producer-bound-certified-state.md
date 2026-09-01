# C402 — `certified` requires a serving-JVM run

Date: 2026-09-01.

The state name remains `certified`; its evidence boundary is now strong enough
to support that claim.

`click-issued` and `click-terminal` both consume the external observer receipt
but do not trust it by shape. At each transition the state machine queries the
serving JVM's click-status endpoint and requires:

- terminal status for the same click ID;
- a present run ID from the runner return;
- verified durable click/run binding;
- a present run record;
- the same terminal outcome; and
- independently readable binding and run-record files whose click and run IDs
  agree with the service and observer receipt.

Thus the producer path is reachable without a bounded wrapper: the serving JVM
is the lifecycle and identity producer, while the external observer supplies
the temporal/resource envelope.

The `certified` transition no longer accepts a certificate file. It invokes
`checks/certify_live_run.clj` itself, using the recorded click receipt and the
Futon2 CI bounded-job ID already recorded at `tested-commit`, and persists the
generated certificate as transition evidence. The generated certificate must
pass and its nested run identity must match the producer-bound run ID.

The C395 counterexample is now a control: after a legitimate `tested-commit`,
handwritten READY and click artifacts with no serving-JVM event refuse at
`click-issued`; the ledger remains at `reload-recorded` and cannot reach
`certified`.

This packet does not address C395's four other findings.
