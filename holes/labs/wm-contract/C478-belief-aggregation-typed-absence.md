# C478 — AC2: the R3d belief aggregator stops substituting zeros

Worklist row `:AC2` (class I). C130 §2 migration at the site the census records
as `src/futon2/aif/belief.clj:1040-1052`, tally instance `:belief-diagnostics`.
Decided by Joe's 2026-09-02 ruling (`DECISIONS-PENDING.md`, futon2 `2f34c26`):
typed absence, loud malformed, no fabricated values, and every abstention
persisted as a typed present-only record.

## What the site did before

`r3d-aggregate-driver` returned a bare double and reached for two defaults:

```clojure
we   (double (:weighted-error err-map 0.0))
prec (double (:precision err-map 0.0))
```

plus `(get channel-health-signs ch 0)`, and `0.0` from the whole function when
`total-precision` was not positive or (flag OFF) when `:annotation-health` was
missing. A channel whose entry lacked a weighted error therefore entered the
signed numerator as an error of zero, and a driver that could not be computed
was indistinguishable from one that measured zero.

## What it does now

Three verdicts per channel entry, and which one it emits is the decision
(`src/futon2/aif/belief.clj:1058-1120`):

- **`:contributing`** — finite `:weighted-error`, finite `:precision` in
  multichannel mode, and a health sign to orient the channel by.
- **`:omitted`** — honestly absent: an upstream `:status :absent` record
  (AC1's shape), or a channel with no entry in `channel-health-signs`, which
  this aggregator has no direction to read. It enters neither sum. The
  aggregator's own verdict stays under `:reason`; the upstream record's reason
  travels beside it as `:upstream-reason`.
- **`:rejected`** — malformed: an upstream `:status :refused`, or a required
  field missing or non-finite. `:offending` names every offending member, with
  a `:legacy-era` / `:malformed` provenance on a missing field, the same split
  `precision/error-field-status` makes (`src/futon2/aif/precision.clj:57-67`).

Rejection is per **entry**. The surviving channels still aggregate, so the
collection is never refused as a whole — that is what separates this site from
AC1's caller, which refuses the entire update when any channel is refused.
There is deliberately **no `:refused` status** on this producer.

The record (`src/futon2/aif/belief.clj:1122-1197`) has two statuses:

- `:present` — `:driver`, `:mode`, `:contributing`, `:producer-contract`.
- `:unknown` — a `:reason` (`:no-channel-supplied`, `:every-channel-omitted`,
  `:every-channel-rejected`, `:no-positive-precision`) and **no `:driver` key
  at all**. Absent evidence does not become a driver of zero.

`:omitted` and `:rejected` are present-only on both.

## Behaviour change, not a no-op

Two changes a caller can observe:

1. A channel entry missing `:weighted-error` used to contribute an error of
   zero to the numerator. It is now rejected and contributes nothing, and the
   remaining channels' precision-weighted average is over a smaller
   denominator only if the rejected entry had a finite precision.
2. When no channel contributes, the tick applies **no belief event** where it
   previously applied one computed from a `0.0` driver
   (`scripts/futon2/report/war_machine.clj:4874-4886`). `aggregated-magnitude`
   is 0.0 in that case, so `event-weight` is 0, `events` is nil, and belief
   passes through unchanged. That 0.0 is a gate, never persisted as a
   measurement.

## Self-repair condition

`:aggregated-signed-error` in the micro-step trace is now **present-only** and
keeps its original position in the step map (built by `merge` of array-maps,
not a trailing `cond->` assoc), so a step where every channel contributed
writes the same map in the same order as before. A step where the driver is
`:unknown` writes no such key and carries `:aggregated-driver-unknown <reason>`
instead. Per-step counts `:belief-aggregation-omitted` /
`:belief-aggregation-rejected` are appended present-only.

The typed records themselves leave the inner loop as
`:belief-aggregation-events` and persist present-only at
`src/futon2/aif/trace.clj:539-546`, beside AC1's `:prediction-triple-events`.
As in AC1 this carries the **terminal** step's records; the per-step counts in
`micro-step-trace` are what covers the earlier steps. AC8's harvester is what
turns these into work items; this row only guarantees they exist and are typed.

## Gates

- **clj-kondo** on the four changed files: 0 errors, 1 warning — "Redundant let
  expression" at `test/futon2/aif/belief_test.clj:85:7`, pre-existing (my diff
  starts at line 722).
- **check-parens** (`futon4/dev/check-parens.el`): OK on all four files.
- **Tests**: `clojure -M:test -m cognitect.test-runner -d test/futon2` —
  979 tests, 5929 assertions, 0 failures, 0 errors (was 971/5854 at AC1:
  +8 deftests). `futon2.report.war-machine-test` requires
  `futon2.report.war-machine`, so the caller edit is compiled by the suite.

## Planted cases (`test/futon2/aif/belief_test.clj:791-985`)

Absent: an upstream `:absent` record omitted with its reason and `:paths`
preserved; a channel with no health sign; every channel absent →
`:every-channel-omitted`; flag-OFF with a typed absence record.

Malformed: `:weighted-error` missing from a contract-stamped entry
(`:malformed`) and from an unstamped one (`:legacy-era`); `:precision` missing;
NaN, `+Infinity` and a string `:weighted-error`; an infinite `:precision` that
would otherwise have dominated the denominator; a non-map entry; an upstream
AC1 `:refused` record; every channel malformed → `:every-channel-rejected` and
still not `:refused`; all-zero precision → `:no-positive-precision`.

Also asserted directly: one bad entry does not refuse the collection, and no
`:unknown` record carries a `:driver` key.

**A planted case found a real defect during this row.** The first version used
`(select-keys err-map [:reason :offending])` to carry the upstream record's
fields, which silently overwrote this producer's own `:reason` with the
upstream's. `r3d-upstream-refusal-is-rejected-entry-not-collection-refusal`
failed on it. Fixed by splitting the two reasons into `:reason` and
`:upstream-reason` — two producers, two reasons, neither clobbering the other.

## Lint and tally

`checks/absence-coercion-dispositions.edn:52-56` flips `:blocked` →
`:fix-now` with the control. `bb checks/preemptive_absence_coercion_lint.clj`:
findings 6 → 5, the belief.clj site's finding gone, the five remaining being
AC3–AC7's sites; the negative control still rejects its mutation.

p4ng `empirics-futon/defect-repair-tally.edn` row `:belief-diagnostics`
`:open` → `:repaired`. Totals over the fixed 61-instance population: 51
repaired / 9 open / 1 partial → **52 / 8 / 1**. (AC1's evidence field states
50/10/1; the tally at that commit reads 51/9/1. Recorded here as an
observation, not corrected — the tally row is authoritative and it is right.)

## Pre-existing red, not mine

`bb p4ng/empirics-futon/pointer_check.bb`: 549 pointers in 3 files, 2
unresolved — both the same `fulab.clj:81` pointer in AC7's worklist row, which
the checker cannot resolve because `src/futon2/aif/adapters/` is not on its
root allowlist. `negative_controls.sh` fails on that one pointer and on nothing
this row added. Same defect class AC1's evidence field already recorded.

## Not done here

The census `:at` key stays `belief.clj:1040-1052` — it is the join key the
lint and the C12 census share, and AC1 set the precedent of naming the new
lines in the `:control` prose rather than moving the key. The figure is not
regenerated (publish-time, TN §9a gate rule).
