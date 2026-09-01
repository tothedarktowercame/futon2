# C353 — adversarial review of writer-fence restoration

Date: 2026-09-01. Reviewer: `wm-evidence`. Reviewed implementation `b02bd0a`.
Assessment only. All executions used injected fixture backends; no live writer
was parked, resumed, re-armed, started, stopped, or reconfigured.

## Verdict

The structural journal checks hold, but the tool is not yet safe as the sole
abort authority. Four counterexamples survived: empty authority passes, a
foreign window pair passes, the unkeyed digest permits coordinator
reclassification, and a partial restore cannot be resumed. There is also an
unclosed observation-to-execution race.

## What held

- A park command that reports success but leaves the target observably
  unparked is rejected by `record` as `park-not-observed`; no row is appended.
- A target that reverted before `restore` observation is rejected as
  `journal-action-not-observed`.
- A journal ordinal gap, duplicate target, reordered rows, and a truncated
  final JSON line all reject before the first inverse.
- Swapping only the manifest while retaining the original journal rejects via
  the manifest digest carried by each row.
- With valid input and no failures, a prefix restores in reverse order and
  does not touch targets absent from that prefix.

## Counterexamples

### 1. Missing and empty authority are successful no-ops

`load_journal` returns `[]` when the path does not exist. `restore(manifest,
[], backend)` validates nothing, executes nothing, and returns success. A
zero-byte journal behaves identically. A digest-valid manifest with an empty
`:targets` map plus an empty journal also returns success.

These states are indistinguishable from “capture succeeded and nothing was
parked,” but they also cover “the journal was never created/lost” and “the
manifest population was truncated before digesting.” C319 explicitly calls an
empty journal a safe no-op; the tool has no evidence with which to establish
that claim. This is the empty-population acceptance defect.

Syntactically empty or malformed JSON does reject. The gap is semantic
emptiness, not parser permissiveness.

### 2. The digest binds bytes, not a window authority

A manifest and journal from another fence, with internally matching digests
and currently matching parked states, restore successfully. `restore` accepts
no expected fence ID, so it cannot reject a foreign pair. The row digest only
detects mixing one window's manifest with another window's rows.

The SHA-256 is also unkeyed and recomputable. A fixture changed the terminal
coordinator's class from `terminal-watchdog` to `running-coordinator`, supplied
a matching captured-running pre-state, recomputed the manifest digest, and
created a correspondingly bound row. The tool accepted
`resume-coordinator` for `jit-queue:jit-m94A03-retry-v3`. This is the exact verb
the class boundary exists to keep away from the completed coordinator.

The capture producer initially assigns classes from constant identities, but
`load_manifest` does not re-establish that identity/class invariant. Therefore
the digest is tamper evidence only against accidental edits whose editor does
not recompute it; it is not restoration authority.

### 3. Validation and execution are separated by a race

`validate_rows` observes every target first; only afterward does `restore`
execute the reversed actions. A fixture returned the required parked state
during validation, reverted the terminal watchdog state before `execute`, and
the tool still invoked `rearm-terminal-coordinator` and returned success. The
backend receives no observation token or expected current state.

A revert before validation is caught; a revert between validation and its
inverse is not. This is a classic check/use window. It is especially dangerous
for a coordinator whose semantic class can change while the fence is already
compromised.

### 4. Partial restoration is not resumable

With two journal rows, the first reversed inverse succeeded and the second
failed. The tool raised `restore-action-failed`, but did not append a durable
restoration outcome or retire the completed row. On retry, validation examined
the already-restored first target, found it no longer parked, and rejected the
entire journal before reaching the still-pending inverse.

Thus the tool handles a partial **parking** prefix but not a partial
**restoration** prefix. The failure mode it exists to recover from can leave the
campaign partly restored with no executable continuation.

## Requested cases, classified

| Case | Result |
|---|---|
| Claimed park never became observable | rejected, no row |
| Park observed then reverted before restore | rejected |
| Revert after validation, before inverse | accepted; race remains |
| Wrong verb in an unchanged manifest | rejected |
| Terminal coordinator reclassified with recomputed digest | accepted |
| Manifest-only swap | rejected |
| Matching foreign manifest+journal pair | accepted |
| Gap / duplicate / reorder / truncated final line | rejected |
| Missing or zero-row journal | accepted as `[]` |
| Digest-valid zero-target manifest | accepted with zero-row journal |
| Failure after one inverse | partial mutation, retry refuses |

## Inventory

The delivery inventory command is:

```sh
bb -cp . -e "(require '[checks.wm-workspace-gate :as g]) (prn (g/inventory-result))"
```

Result: `{:name :check-inventory, :exit 0, :unknown (), :missing ()}`. The
author's four focused tests also remain green.
