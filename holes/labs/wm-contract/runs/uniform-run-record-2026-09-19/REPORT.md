# EV-uniform-run-record — decision quantities on the durable record

Author: codex-31. Implementation commit: `88419699`. Review/acceptance pending.
No click or shared-JVM reload was performed.

The runner now writes its retained selection's selection law, selection
certificate and enumeration-completeness record under `:decision`. The existing
G decomposition moves there unchanged, with its original name. Selection now
retains each ranked candidate's C and rates provenance from that candidate's
scorer certificate, in an indexed `:scoring` map. No quantity is recomputed or
changed, and the writer does not search historical data for absent quantities.
Runs without selection still write a record with missing evidence; no gate was
added. The external-repair path was not changed.

## Measured acceptance

`offline-replay-run-record.edn` reports **VALID (3/5 ok)** under:

```
bb scripts/wm_run_validity.bb holes/labs/wm-contract/runs/uniform-run-record-2026-09-19/offline-replay-run-record.edn
```

All five printed paths start with `:decision`:

| Field | Verdict | Path suffix after `:decision` |
|---|---|---|
| C source | flagged: derived-no-overlap | `:selection-certificate :scoring 0 :c` |
| rates provenance | ok: identity-default | `:selection-certificate :scoring 0 :rates-provenance` |
| posterior | flagged: computed F not attached for 4 policies | `:selection-certificate` |
| U37 | ok: recorded verdict incomplete | `:enumeration-completeness` |
| G decomposition | ok | `:g-term-decomposition` |

The checker ran its negative controls and exited 0. See `checker-output.txt`.
The replay uses the committed tick-001 cascade fixture through the production
assembly, cascade decision, U37 carry, and run-record writer. The live-C source
snapshot, locator records and enumeration corpus are explicitly test inputs;
the receipt does not claim a production click. Live C is derived by the real
producer, which records its actual no-overlap fallback. U37 scans a temporary
corpus and records the missing member `M-present`; that diff survives unchanged.
The writer's historical selectorSeam label remains unchanged, and must not be
read as a live-actuation claim in this explicitly offline artifact.

`receipt.edn` is extracted from the registered test log, including the read-back
run record and checker output. `offline-replay-run-record.edn` is that same
record, extracted for direct CLI checking. No file under `futon2/data/` was
edited. All runner probes use `with-hermetic-stores`: production repair file
counts **200 → 200**, trip counts **293 → 293**. Trace writing is false.

## Gates and warrants

Primary warrant, checked and bound through `test-registry.validation` to
`EV-uniform-run-record`:

`test-registry-617bb5ce91010f286ad47a757a0a69ddcf605c2809620406114685d439ea1791`

**3 tests / 36 assertions / 0 failures / 0 errors**, 3282 ms registered execution.
The tests preserve each policy's distinct C/rates, all carried values, both
flags, U37's membership diff, the root relocation, and invalid-but-written
behavior when this run has no selection but a historical decision exists.
The checker script is included explicitly in the warrant's code scope.

The existing G replay test was updated for the new location and re-warranted:

`test-registry-d571df6424583605ae0a619db11059b2c8589746db706a622ccef8ef1fa60ab6`

**6 tests / 78 assertions / 0 failures / 0 errors**, 3256 ms. This includes the
six unchanged selection-decision byte comparisons and independent A probe.

All four changed Clojure files passed clj-kondo with **0 errors / 0 warnings**,
`futon4/dev/check-parens.el` returned OK, and `git diff --check` passed.

## Findings and limits

The corrected dispatch premises hold at `327d6f18`. No new contradiction was
found. The measured result is **3/5**, not the illustrative 4/5: C's no-overlap
fallback is flagged alongside F. U37's deliberately incomplete membership is
reported `ok` by the existing checker because its verdict is present. Thus
VALID here means the five quantities are recorded under the accepted root; it
does not mean enumeration is complete or F was consumed. Those values and the
checker were not altered.

A future repair/operator path that bypasses cascade selection will still have
missing quantities and report INVALID. This change transports existing values;
it cannot make an unexecuted selection produce witnesses. No blanket claim is
made that the next authorized click must be valid, and no production data was
backfilled.
