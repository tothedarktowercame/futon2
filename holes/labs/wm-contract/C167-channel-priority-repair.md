# C167 — channel priority typed-absence repair and v20 diagnostic

Date: 2026-08-31

`channel-priority-result` now partitions non-numeric gaps before numeric
sorting. Present positive gaps retain the existing descending ranking.
Unknown gaps are excluded as explicit `:channel-gap-exclusion` records with
`:reason :gap-absent` and their available status/reason metadata; they are not
coerced to zero and never reach the comparator.

The focused control supplies gaps 0.8, nil, and 0.2. It ranks the present gaps
0.8 then 0.2 and emits one named exclusion for the nil channel. A source sweep
found no other filter-after-sort instance over a nillable `:gap`; the Python
E5 probe sorts only after explicitly converting its input to `double`, and the
cascade lane uses an explicit numeric fallback.

The first post-repair diagnostic completed through
`stub:first-ranked-authorized-mission` but exposed a second diagnostic-only
gap: `run_tick_once` had asked `judge` to write the trace before attaching any
`:wm-version`, so that successful record was unversioned. It remains in the
corpus as evidence. The diagnostic path now constructs the same
`wm-version-stamp` contract as the scheduled runner, with trigger
`:diagnostic-run-tick-once` and `:live-wire? false`, and passes it into the
judgement before trace writing.

The second post-repair diagnostic wrote the first v20 record. All six v15-v20
fields read back as present. Every trace reader agrees on 803 records, with
one v20 record. The wrapper measured clean resources (peak 74, max-event delta
0), and the operational certificate passes using recorded run id
`00f4bf58-4da6-42bc-bb1d-5687e889e717`, pinned topology, nine declared hops,
and the committed resource receipt.

No production operator loop or live actuation ran. Joe's operator-triggered
run remains unspent.

Canonical invocations:

```sh
clojure -M:test -m cognitect.test-runner \
  -v futon2.report.war-machine-test/channel-priorities-exclude-unknown-gaps-before-sorting-test
cd /home/joe/code/futon3c
python3 scripts/bg.py launch-test \
  "clojure -M -m futon2.run-tick-once 14" \
  --agent wm-verbs --label c167-v20-stamped-diagnostic \
  --dir /home/joe/code/futon2 --window control
cd /home/joe/code/futon2
clojure -M -m checks.trace-schema-compatibility
bb -cp . checks/wm_operational_certificate.clj \
  --run holes/labs/wm-contract/tick-run-record-2026-08-31.edn \
  --resource holes/labs/wm-contract/C167-v20-certificate-resource.edn \
  --certificate holes/labs/wm-contract/C167-v20-operational-certificate.edn
```
