# C163 — diagnostic v20 tick attempt

Date: 2026-08-31

The bounded diagnostic command used `futon2.run-tick-once` and resolved its
declared seam to `stub:first-ranked-authorized-mission`; the Futon3c live
selector remains absent from Futon2's classpath. No production operator loop
was invoked.

The tick failed before trace emission. `channel-priorities` sorts
`:free-energy :per-channel` by `:gap` at `war_machine.clj:4037` before its
subsequent positive-gap filter. At least one current tri-state channel has a
nil gap, so numeric `>` reached nil and TimSort raised
`NullPointerException`. The stack reaches this site through `judge` and
`generate-war-machine`, before `run_tick_once` can write its receipt.

The bounded wrapper independently reports clean resources: command exit 1,
wrapper exit 125, `pids.events:max` delta 0, no native-thread marker, peak 72.
This is a functional failure, not resource exhaustion.

No partial evidence was accepted. The existing daily trace remained 1,568,426
bytes with its 14:30:41Z mtime and no `tick-run-record-2026-08-31.edn` exists.
Therefore no v20 record was available for reader readback or operational
certification. Those acceptance items remain blocked at emission rather than
being simulated or certified from another record.

The durable measured summary is
`C163-diagnostic-tick-failure.edn`. It records hashes of the wrapper and JVM
failure receipts, the unchanged trace identity, and the three blocked gates.
No runtime repair is included in C163.

Canonical invocation:

```sh
cd /home/joe/code/futon3c
python3 scripts/bg.py launch-test \
  "clojure -M -m futon2.run-tick-once 14" \
  --agent wm-verbs --label c163-v20-diagnostic \
  --dir /home/joe/code/futon2 --window control
```
