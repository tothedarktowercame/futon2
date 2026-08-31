# C106 — bounded resource receipt to operational certificate

Date: 2026-08-31

The bounded test wrapper now emits an atomic EDN companion to its JSON receipt
from the same measured command exit, `pids.events:max` delta, native-thread
markers, and task peak.  `bg.py launch-test` records that companion as
`:certificate-resource-file`; there is no hand-written translation between
the execution and certificate boundaries.

No production tick was executed.  The positive was a bounded replay of the
committed 2026-08-30 `TickRunRecord` witness.  Its wrapper-emitted resource
receipt was `:clean`, command exit 0, max-event delta 0, and peak 5; the
operational certificate passed end to end.  A second bounded control ran a
command that exited 7.  The same wrapper emitted `:dirty` with wrapper exit
125, and the certificate wrote `:verdict :fail` and exited 1.

Canonical invocations:

```sh
cd /home/joe/code/futon3c
python3 scripts/bg.py launch-test \
  "bb -cp . checks/wm_runs_once_witness.clj holes/labs/wm-contract/tick-run-record-2026-08-30.edn" \
  --agent wm-verbs --label c106-replay --dir /home/joe/code/futon2 --window control
python3 scripts/bg.py launch-test \
  "printf 'diagnostic failure control\\n'; exit 7" \
  --agent wm-verbs --label c106-dirty --dir /home/joe/code/futon2 --window control
python3 -m unittest scripts/test_bounded_test_job.py

cd /home/joe/code/futon2
bb -cp . checks/wm_operational_certificate.clj \
  --run holes/labs/wm-contract/tick-run-record-2026-08-30.edn \
  --resource /tmp/futon-bounded-tests/bounded-1788199511618-c106-replay.resource.edn \
  --certificate /tmp/c106-certificate.edn
bb -cp . checks/wm_operational_certificate.clj \
  --run holes/labs/wm-contract/tick-run-record-2026-08-30.edn \
  --resource /tmp/futon-bounded-tests/bounded-1788199524627-c106-dirty.resource.edn \
  --certificate /tmp/c106-dirty-certificate.edn
```

The second certificate command is expected to exit 1: a dirty measured
receipt is rejected.  This proves the resource control with wrapper output,
not the old fixture.  The operator-triggered production run remains unspent.
