# WM-09 predecessor repair under the revised current-record contract

Author codex-7; packet revision 2, p4ng `55bf8c3`. Independent review pending.
Original `7e738d53` diagnostic receipts remain byte-identical.

## Behavior

Carrier absence no longer resets to an initial cascade. Closed-history discovery
retains records with missing construction/cascade/carrier, establishes target
agreement and parses ordering as instants. The latest matching earlier record
must validate; failure never falls back to an older usable record. Valid modern
predecessors keep their existing identity, manifest, digest and provenance joins.

Old-shaped records without current evidence refuse with source paths. No legacy
marker, date cutoff or migration was introduced. For unrelated earlier records,
current predecessor validation is required before exclusion: an unauthenticated
mission name is insufficient to establish absence. This is intentionally
conservative and can refuse old unrelated history in supplied roots.

Unparseable/missing order, unavailable/conflicting targets, unreadable/malformed
records, missing/unreadable roots and empty root lists produce typed discovery
refusals. Equal actual instants remain ambiguous even when timestamp strings use
different offsets. A legitimate initial cascade requires no earlier matching
history in readable explicit roots. Complete deletion leaving no closed-history
evidence cannot be detected; open attempts remain outside this input universe.

## Evidence

`after-probe.clj` / `after.edn` mirror the retained before controls, with assertions
for the repaired behavior. Carrier deletion now refuses; missing cascade or
construction now refuses rather than exposing the older epoch. Missing manifest
or occurrence and malformed carriers remain refusals. Valid history and actual
absence retain their positive behavior. The original probe is intentionally
unchanged and still expresses diagnostic expectations of the old defect.

Expanded namespace controls cover old-shaped refusal, discovery/target/order
errors, missing/corrupt required payload, unrelated validated history, equal-time
ambiguity and valid absence. New tests run against pre-repair source in a separate
temporary-process load reject it: 19 failures, 2 errors (54 assertions reached).
This negative run is `old-source-new-tests.log`; its exit 1 is expected. Production
source was never temporarily replaced or loaded into a serving JVM.

Final lint, parenthesis and namespace gate commands/statuses are in AFTER-GATES.json.
The isolated direct-caller namespace uses its existing hermetic-store/traces
fixtures; its result is retained separately. No caller source/test modifications,
serving reload, click, checklist or DAG edits are included.

Validation sequencing: the full caller namespace began before the final explicit
invalid-root guard and timestamp-agreement check were added. Final-version mission and ticket caller controls
are therefore rerun separately in `after-final-caller-controls.log`; the complete
predecessor namespace and lint/parens gates cover the final version.

A final discovery control rejects disagreement between the close envelope time
and retained close time, including an edit that would otherwise move a matching
predecessor beyond the current action time and hide it. This extra regression
was added after the retained old-source negative run; final namespace gates
include it.

Final results: predecessor namespace **9 tests / 65 assertions**, full caller
namespace **14 tests / 467 assertions**, final-version targeted caller controls
**2 tests / 16 assertions**, all zero failures/errors and exit 0. Kondo has zero
errors/warnings; check-parens reports OK. No independent acceptance is claimed.
