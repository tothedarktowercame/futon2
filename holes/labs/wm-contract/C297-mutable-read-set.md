# C297 — shared mutable read-set

Date: 2026-09-01

## Substrate

`checks.mutable-read-set` captures each input into one byte array. Text, byte
count, and SHA-256 are derived from that same array, so a check can report the
exact bytes it examined. After capture it compares every live input with the
captured digest and returns one of two states:

- `:stable` — the captured read-set may produce a verdict;
- `:moved` — the comparison names changed or unavailable inputs, and a caller
  can report movement instead of producing a hybrid verdict.

`require-stable!` is the verdict-required policy. Checks whose subject is
movement can consume the same comparison without requiring stability. Thus the
two remedies share a substrate without pretending they are the same policy.

## Two conversions

1. `r17_generator_disposer_check.clj` is the minimal case. Its SHA and text
   previously came from two `slurp` calls on one path. Both now derive from one
   captured byte array, and all guard inputs belong to one compared read-set.
2. `wm_route_conformance.clj` is the cross-repository case. Its p4ng topology
   and futon2 tick record are captured together. If either moves before the
   comparison, the check reports `UNAVAILABLE` rather than route conformance.

## Controls and focused gates

```text
clojure -X:test :nses '[mutable-read-set-test]'
  2 tests, 6 assertions, 0 failures, 0 errors

bb -cp . checks/r17_generator_disposer_check.clj --report /tmp/c297-r17.edn
  exit 0, dormant-guarded
bb -cp . checks/r17_generator_disposer_check.clj --negative --report /tmp/unused.edn
  exit 0, live all-pairs mutation rejected

bb -cp . checks/wm_route_conformance.clj \
  holes/labs/wm-contract/tick-run-record-2026-08-31.edn
  exit 0, 9/9 conformant
bb -cp . checks/wm_route_conformance.clj --negative \
  holes/labs/wm-contract/tick-run-record-2026-08-31.edn
  exit 0, injected unmapped hop rejected
```

The movement control changes a real temporary file between capture and
comparison. It returns `:moved`, names `:changed`, and `require-stable!` refuses
the verdict. No retry is involved.

## Agency limit and remaining population

The Agency job API supplies independent per-job responses and no roster
snapshot token or shared revision. A caller can timestamp the observation
interval and detect some changes by rereading, but it cannot prove several job
states coexisted; ABA transitions remain invisible. A truthful instantaneous
multi-job verdict therefore needs a service-issued snapshot/revision token.
That is a service change, not implemented here.

C293's remaining population is unchanged: 13 other known high-risk checks and
the 90-candidate tail have not been converted.
