# Incremental test warrants

After merging to `main`, run:

```sh
scripts/warrant_suite.py
```

The script asks the serving JVM to re-hash each namespace's recorded load
closure. Fresh namespaces are skipped; only missing or stale namespaces run.
Passing namespaces receive a new test-registry warrant. Failing namespaces do
not, so they run again on the next invocation until fixed.

Use the serial default unless the host has enough memory and native-thread
headroom for nested registry and test JVMs. `--dry-run` reports currency
without executing tests.

Do not routinely substitute a full-suite run. A full run is needed when the
execution environment changes in a way the recorded closure cannot represent
(for example JVM/toolchain, dependency resolution, service configuration, or
test-runner semantics). After such a run, reseed warrants once with this
script; subsequent unchanged runs should complete in seconds by checking the
warrants.

Registry authority is
`/home/joe/code/storage/test-registry/warrant-index.sqlite`; immutable logs are
in `/home/joe/code/storage/registry-ledger`. The local
`data/test-warrants/index.json` only maps namespaces to registry entry IDs and
may be rebuilt by running the script.
