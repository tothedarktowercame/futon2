# C372 — third independent restoration-tool review

Date: 2026-09-01. Reviewed `scripts/writer_fence_restore.py` at `e16311b`
using temporary fixtures and injected backends only. No live fence, coordinator,
unit, key, manifest, or restoration ledger was touched.

## Verdict

The C367 repair closes C365's two reported failures, but the tool does **not**
yet survive independent attack. One material authentication defect and one
output-contract defect remain.

## Findings

### 1. Attempt rows are fence-labelled, not authenticated

An attempt row copies `manifest-hmac-sha256` from the readable manifest. It has
no HMAC over its own ordinal, target, action, status, or timestamp. Consequently
a process able to write the attempt JSONL does not need the key to manufacture
the prerequisite for reconciliation.

The fixture copied the public manifest tag into a fabricated
`inverse-attempt-recorded` row, placed the target in its externally restored
state, and invoked `restore`. The tool executed no inverse and emitted a durable
`status: restored` outcome with reconciliation
`observed-restored-outcome-record-missing`.

This does preserve the deliberately narrow assurance `final-state-observed`;
it does not establish that this tool attempted the inverse. But the attempt
record is used as authoritative evidence to distinguish “restored without an
attempt” from “restored after a possible crash.” Because that discriminator is
forgeable without the key, the three-way judgement is not authenticated.

A natural crash after the attempt append but before execution behaves safely
when the target remains parked: retry executes the inverse once and records the
outcome. If another actor restores it first, retry reconciles without execution;
that is safe for final state but causally unattributed, exactly as the residual
limitation requires.

### 2. The residual limitation is absent from refusal verdicts

Successful `restore` results always include:

```text
assurance=final-state-observed
residual-limitation=compare-before-act-narrows-race-but-does-not-prove-event-freedom
```

Failure output contains only `{"ok": false, "reason": ...}`. Thus the machine-
readable limitation does not survive “every verdict”; it disappears on the
paths where an operator most needs to know whether refusal says anything about
intervening events. This is an output-contract gap, not evidence that a failed
restore changed state.

## Requested boundaries that held

- A mode-`0600`, current-owner regular key is accepted. Group/world-readable
  modes reject; a symlink rejects through `O_NOFOLLOW`; a directory rejects as
  non-regular; missing, short, and wrong keys remain loud. On a filesystem that
  cannot represent the required mode/owner, the check fails closed. A writable
  parent can still cause replacement/availability attacks, but a distinct UID's
  replacement fails the owner check and does not authenticate.
- Empty or missing attempt history is valid before the first inverse. It is a
  progress ledger, not the restoration subject; the independently nonempty,
  validated park journal supplies the work. Empty attempts led to normal
  execution. Malformed attempts rejected as `journal-invalid-line:1`, and a
  foreign-fence attempt rejected as `restore-attempt-invalid`.
- The three reconciliation states are distinct: attempt + parked executes;
  attempt + restored reconciles; no attempt + restored rejects. A neither-
  parked-nor-restored state rejects.
- The focused committed suite passed 10 tests.

## Residual race

Compare-before-act still proves only endpoint/final state, not event-freedom.
The observation and inverse are separate operations. This is now stated on
successful results and remains an honest architectural limit; closing it needs
a target-side compare-and-swap/revision or an independently held writer fence.

This delivery reports only; it changes no restoration implementation.
