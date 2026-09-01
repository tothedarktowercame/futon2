# C395 — quiet-run state-machine adversarial review

Date: 2026-09-01. Review only; no implementation was changed.

## Verdict

The state machine does **not** yet establish that a certified run happened.
Starting from a legitimately reached `tested-commit`, caller-authored reload,
click-issued, click-terminal, and certificate files reached `certified` with
exit 0 without invoking a runner, observer, or certificate producer.

The documented caller boundary covers `reload-recorded` and `click-issued`.
The successful attack is therefore not the existence of those two seams. The
finding is that the same authority continues through `click-terminal` and
`certified`, where the records purport to attest a run and its certificate.

## Successful attacks

### 1. `certified` without a run

After the producer-bound tested phase, the public transition API accepted:

```text
reload-recorded  handwritten READY report                         exit 0
click-issued     handwritten click id and parseable start time    exit 0
click-terminal   handwritten wm-click-resource-v1                 exit 0
certified        handwritten {:verdict :pass :run/id ...} EDN     exit 0
final state                                                       certified
```

The terminal validator checks schema and joins the caller-supplied click/run
ids. The certificate validator checks only `:verdict :pass` and that same
caller-supplied run id (`scripts/wm_quiet_run_state.py`,
`evidence_click_terminal` and `evidence_certified`). Neither resolves its
artifact through the producer that is meant to have emitted it. This is the
same shape C380 identified at the evidence boundary, now demonstrated through
the current public transition API.

### 2. A valid history can be truncated and forked

A valid certified ledger was truncated by its last two rows and extended with
a different handwritten terminal receipt and certificate. Both transitions
returned exit 0 and the new chain loaded as `certified` with the replacement
run id.

Per-row and predecessor hashes prove the integrity of the rows that remain.
They do not prove history length or bind the current head to an external
append-only authority. A complete certified ledger also loaded unchanged after
being copied to a second ledger path while its absolute evidence paths remained
available. Consequently replay, rollback, and two ledgers sharing one fence id
are not distinguished. The fence id is checked for consistency within a
ledger, not uniqueness across ledgers/windows.

### 3. Restoration can have no active subject

An HMAC-valid manifest containing one known but initially inactive systemd
unit, plus empty journal and outcome files and `{"ok": true}`, advanced a
certified ledger to `restored` with exit 0. `load_manifest` rejects a map with
zero targets, but the state machine derives the required restoration population
only from targets whose captured pre-state says they were active. A nonempty
manifest can therefore yield an empty required population.

This does not alter the `certified` attack above, but it means the later
restoration state can also be reached without representing the changed target
population. Authentication proves who could write the manifest, not that the
capture contains the enforced parking population.

### 4. The systemd monotonic value is presence-checked, not used

`ExecMainStartTimestampMonotonic` values `1` and
`999999999999999999` both passed `tested-commit`. The implementation checks
only that the field is positive decimal text. It does not compare the value
with the fence observation or gate interval. Thus the statement that systemd's
monotonic start *defeats timestamp editing* is not supported by this field.

The underlying freshness result nevertheless held for a different reason:
ingestion computes `now() - fence.finished-at`, and a fence older than 300
seconds is rejected. Editing the gate receipt's `started-at` did not freshen an
old fence. The monotonic field is currently decorative evidence, not the basis
of that refusal.

## Attacks that were rejected

The committed 13-control suite passed. In particular:

- skipped states and early release were rejected;
- a synthesized mid-chain first row was rejected;
- mutation of a recorded evidence file was detected by digest revalidation;
- a changed parking specification required reinitialization;
- handwritten bounded-job receipts were rejected in favour of the producer
  registry;
- a stale fence could not be freshened by editing a gate timestamp;
- gate and Futon2 suite receipts from different commits were rejected;
- an incomplete active restoration population was rejected;
- changed fence ids within one chain and malformed/trailing evidence remain
  rejected by their current validators.

These controls show that order, retained-row integrity, the producer-bound
tested phase, and current-time fence freshness are real. They do not compensate
for producer-free terminal/certificate evidence or for the lack of a durable
ledger-head/attempt identity.

## Character of the findings

The sequence machine is not vacuous: next-state ordering and several producer
boundaries fire. The failures are evidence-authority failures. Shape-compatible
artifacts are accepted where event-produced artifacts are required, and the
hash chain has no authority for completeness of history. The immediate blocker
for Joe's run is finding 1: `certified` can be reached without a run having
happened.

## Delivery inventory

```sh
bb -cp . -e "(require '[checks.wm-workspace-gate :as g]) (prn (g/inventory-result))"
```

Result: `{:name :check-inventory, :exit 0, :unknown (), :missing ()}`.
