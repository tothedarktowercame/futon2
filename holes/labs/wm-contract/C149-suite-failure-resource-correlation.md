# C149 — suite failure/resource correlation

Date: 2026-08-31

The bounded test wrapper now joins every failing outer verdict to the resource
measurements in that same run's receipt.  The receipt and durable log state the
command exit, `pids.events:max` delta, native-thread marker, and an independent
`:resource-status` of `:clean`, `:dirty`, or `:unavailable`.

An induced command failure (exit 7) under clean resources produced outer exit
125 and reported:

```text
command-exit=7 resource-status=:clean pids.events:max-delta=0
```

The already measured C91 tiny-budget receipt (green inner command,
`pids.events:max` delta 2, outer exit 125) reports:

```text
command-exit=0 resource-status=:dirty pids.events:max-delta=2
```

This is a run-level correlation, not per-test attribution.  It can establish
whether resource pressure occurred anywhere during the failing run; it cannot
identify which test caused that pressure, nor does a clean run-level receipt
exonerate one test's timing or logic.  The limitation is serialized in the
correlation and printed in its summary line.  No timeout policy or resource
measurement changed.

## Canonical invocations

```sh
cd /home/joe/code/futon3c
python3 -m unittest scripts/test_bounded_test_job.py
python3 scripts/bounded_test_job.py --receipt RECEIPT --output LOG \
  "printf 'induced inner failure\\n'; exit 7" # expected exit 125

cd /home/joe/code/futon2
bb checks/reader_portability_lint.bb # expected exit 1, findings=11
clojure -T:build ci
cd /home/joe/code/futon3 && clojure -X:test
```
