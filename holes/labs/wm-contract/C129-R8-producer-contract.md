# C129 — R8 producer-contract migration

Date: 2026-08-31

## Result

New trace records declare `:producer-contract
:r8/stored-f-controller-v1`. Trace schema is v20. The R8 checker now chooses
the current contract from that tag before consulting time.

Unversioned historical records retain an explicit
`:source :filename-day-fallback, :status :legacy-era` branch. This preserves
the pinned 792-form classification exactly. A record declaring the current
contract but omitting stored variational F is malformed; an unknown declared
contract is malformed rather than silently treated as legacy.

The disagreement control uses a current-contract record in a
`wm-trace-2026-07-09.edn` wrapper. It passes as current by the declared tag.
The former filename-day discriminator would have rejected the same record.
The inverse control — declared current contract with no stored F — fails.

No scoring, selection, or historical trace record changed.

## Gates and canonical invocations

```sh
clojure -M:test -m cognitect.test-runner \
  -n r8-f-contract-test -n futon2.aif.trace-test
bb -cp . checks/r8_f_contract.clj --report /tmp/c129-r8.edn
bb -cp . checks/r8_f_contract.clj --negative \
  --report /tmp/c129-r8-negative.edn
bb checks/r8_pinned_snapshot_witness.clj
bb checks/r8_pinned_snapshot_witness.clj --negative-pin
bb checks/r8_pinned_snapshot_witness.clj --negative-census
clojure -T:build ci
cd /home/joe/code/futon3 && clojure -X:test
```

The live gate reports 801 forms and exits 0. The snapshot witness and both
controls exit 0; its source remains 53 files / 792 forms at pin
`c9add16ac96c973ba4fd9a0c61f3b7319780c304424e2d14ea7b477309947880`.
