# Habit and initial-belief input receipts

Implementation `591a6cdd`, main merge `66f627a1`.

The habit producer now reads and strictly decodes a single UTF-8 snapshot,
parses that snapshot, and hashes its bytes. Each run retains all successful
reads at `:habit-reads :occurrences`: state, source path, snapshot EDN, SHA-256,
read purpose and occurrence index. Missing stores are explicitly `:absent`,
reason `:store-missing`, with the cold-start state actually consumed.

There are MORE than two reads: each per-target lane can invoke selection,
then the final joint selection reads again, then persistence reads under its
existing lock before updating. The one-target test records three occurrences:
`:selection-scoring`, `:joint-selection`, `:selection-update`. Each successful
scoring read records consumption with the FULL candidate maps, policy keys and
habit masses. Final candidates join to the joint-selection occurrence by their
full maps and consumed quantities, not nested C0/C1 labels. Read logging does
not change serialized policy decisions or the store's update/locking behavior.

The initial belief receipt is at `:decision :initial-belief-receipt`. It hashes
the captured assembled target/fact inputs, identifies origin
`:assembled-target-facts` and derivation
`:target-qualified-true-facts-point-mass-v1`, and records the q0 produced by that
same computation and passed to the scorer. It is not merely a hash of q0.

The existing provenance vocabulary is preserved. An instrumented run with zero
habit reads is `:absent` / `:never-read`; a writer without read instrumentation
is `:not-observed`. Removing the log is detectable. `validate-record` checks
receipt presence, snapshot hashes, origin and derivation, q0 against recorded
incoming beliefs, and consumed habit counts/alpha/samples/masses against the
recorded joint-menu snapshot, without consulting a store.

## Validation

The narrow receipt namespace passes 3 tests / 34 assertions. Controls exercise
the actual cascade decision, scorer, selector, habit store/update, and run
writer; the runner's core actuator/status ports are stubbed. All habit writes
are through the production store API into temporary directories. Tests retain
successive store snapshots, prove count/hash differences, distinguish earlier
scoring reads from later update reads, and reject removed receipts, a false
initial-belief origin, or substitution of a later store snapshot.

clj-kondo 0 errors / 0 warnings; check-parens OK. The existing good record
`tick-run-record-2026-09-19-1789854206.edn` has byte-identical wm_run_validity
output before/after.

The additional cascade-habit-accumulation namespace has one PRE-EXISTING failed
assertion (5 tests / 32 assertions): frozen decision-byte fixtures omit the
previously added `:node-evaluation-traces`. Unchanged main at `a6256c70`
reproduces the same failure. Baseline and changed logs are retained. The existing
check and fixtures were not weakened or changed; the real cascade-decision
comparison and other assertions pass. This warrant covers the new receipt
namespace, not a claim that the older fixture namespace is green.

No WM click or serving reload was performed, and no live data was hand-edited.
A canonical-source serving reload is REQUIRED for activation, including the new
scoring-input-receipts namespace, cascade-habit-store, war-machine and
full-loop-runner. This is a different reload from the earlier declaration fix.

Warrant: `test-registry-b938177f9bd3498953ffa6df50b36ccf311a390ea01e021facf6c998e809ce1d`, bound to `EV-scoring-input-receipts`.
HTTP check returned `warrant? true`.
