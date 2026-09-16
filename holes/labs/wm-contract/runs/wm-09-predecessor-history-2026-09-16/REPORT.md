# WM-09-predecessor-history-1: executable counterexample and authority gap

Author codex-7. Return under deliverable 4 of the packet (p4ng `15aae6d`).
**Repair blocked on historical-format recognition authority; no production repair
claimed.** Only this receipt directory is changed. No serving actions, live-store
probes, migration metadata or guessed markers were introduced.

## Executed evidence

`probe.clj` creates fresh temporary stores using the existing modern history
fixture, calls the real `previous!`, and deletes its stores in finally blocks.
`before.edn` retains the raw outcomes:

| Case | Observed result |
|---|---|
| Valid modern latest | Carries newer epoch |
| Modern carrier removed | Incorrect legacy reset |
| Carrier and manifest removed | Incorrect legacy reset, validation bypassed |
| Carrier malformed (unsealed edit) | Refuses manifest/source mismatch |
| Latest :cascade removed | Incorrectly exposes older valid epoch |
| Latest construction file missing | Incorrectly exposes older valid epoch |
| Manifest missing | Refuses manifest shape |
| Occurrence missing | Refuses previous-occurrence-unavailable |
| No prior records | Legitimate initial cascade |

The assertions pin the *unfixed* counterexample; passing them is diagnostic
reproduction, never admission or repair acceptance. Existing isolated namespace:
7 tests / 31 assertions, zero failures/errors. Kondo: zero errors/warnings after
removing an unused probe import. Parens: OK on production source, its test and
the probe. Raw outputs, commands/statuses and source hashes are retained.

## Why no safe legacy predicate was selected

The reviewed legacy example is the explicitly named retained July attempt:
`data/wm-full-loop/wm-outer-loop-41-v1/attempt-043/003-construction.edn` and its
close. It records event/schema-version 1, a selected-policy cascade and no
receipted carrier. Current `full_loop_cohort.clj` still emits event/schema-version
1. Current `full_loop_runner.clj` still emits that nested cascade shape and adds
receipted-construction conditionally. Those positive fields do not discriminate.

Git source on both sides of receipt-mode introduction (`24dc6068^` and
`24dc6068`) declares the same semantic epoch, `:full-loop-real-actuation-v6`.
Thus an epoch cutoff at introduction is not available either. The old-shaped
unit fixture's `:legacy` epoch is test-created, not historical format authority.
Absence of modern fields, timestamps and fixture labels cannot supply the
positive historical-format evidence required by the packet.

This is a bounded source finding, not proof that no external historical authority
exists. No inspected schema/producer/epoch binding licenses treating an arbitrary
carrierless candidate as legacy. Supporting such a reset while guaranteeing
refusal for damaged modern history requires an authoritative discriminator or
an explicit decision about unsupported history. Inventing one is outside scope.

## Exact next decision / evidence needed

Supply an existing source-bound historical-format discriminator and its accepted
recognition contract, or explicitly decide how ambiguous history must be refused
and how legacy compatibility is to be handled. Then the repair must both validate
before any reset and keep damaged latest candidates in discovery, rather than
allowing missing carrier/cascade/files to erase them. No implementation of that
new contract or successor is commissioned by this return.

The counterexample is independently rerunnable without production stores. Since
source behavior was not changed, no after-repair controls or affected caller
acceptance are claimed. Coordinator routes independent review to claude-3 and
owner assessment to wm-09 as specified in the packet.
