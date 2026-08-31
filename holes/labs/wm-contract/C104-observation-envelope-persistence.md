# C104 — observation envelope persistence

Date: 2026-08-31

## Delivery

`trace-record` now persists `:observation-envelope` beside the compatible
numeric `:observation`. Both are derived from the **same** observation object
already present in `judge-output`; the trace boundary does not observe again.
Thus the record cannot drift from what scoring saw, and scoring/selection are
unchanged. Trace schema version 17 records this additive contract change.

The envelope distinguishes, for every channel:

- `{:variant :observed :value 0.0}` — a measured zero; and
- `{:variant :absent :reason ... :paths ... :coerced-to 0.0 :value 0.0}` —
  an unavailable input whose compatible numeric projection is zero.

The falsifier uses those two numerically identical observations and requires
different persisted variants. A second control writes and reads the trace and
requires exact envelope preservation.

## Schema decision

**Yes: traces should carry lossless envelopes by default whenever a producer
has semantically richer information than its convenient numeric/projection
view.** Per-field opt-in has already discarded replay inputs, preference-stack
provenance, and observation presence. The default is not “wrap every value”:
it is “do not discard producer distinctions at a persistence boundary.” Keep a
projection for compatible readers, persist its source envelope once per tick,
and content-address or deduplicate large repeated structures.

Measured UTF-8 EDN sizes for the 14-channel observation are 306 bytes for the
numeric projection, 2,141 bytes for an all-absent envelope, and 2,061 bytes
with one present channel. The worst measured increment is therefore 1,835
bytes per tick. This is bounded by channel count and is stored once, unlike a
per-candidate copy. The cost is schema growth and dual read compatibility;
schema versioning makes that cost explicit. If future envelopes are large,
store one content-pinned envelope and reference it rather than dropping it.

## Verification

- `clojure -X:test :nses '[futon2.aif.trace-test]'` — 27 tests, 74 assertions,
  0 failures/errors.
- `clojure -X:test` in futon2 — 1,031 tests, 6,180 assertions, 0
  failures/errors.
- `clojure -X:test` in futon3 — 248 tests, 1,518 assertions, 0 failures/errors.
- `bb -cp . checks/preemptive_absence_coercion_lint.clj` — expected exit 1,
  exactly 15 known findings (`{:futon2 15, :futon3 0, :p4ng 0}`); unchanged.
- Size measurement:
  `clojure -M -e "(require '[futon2.aif.observation :as o]) ..."` over
  `o/observe {}` and one-present-channel input; figures above are byte counts
  of `pr-str` encoded as UTF-8.

No scoring, ranking, policy, or selection source was changed.

## Automatability

This implementation unit scores **7/7**: its input and persisted output are
typed; acceptance and mutation are named; the controls execute; the trace
reader is the named consumer; the producer object is used directly and
absence is loud; the additive schema change is bounded/backward-readable; and
the only policy decision (default envelope retention) is recorded above.
