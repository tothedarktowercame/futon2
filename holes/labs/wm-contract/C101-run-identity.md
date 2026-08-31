# C101 — explicit tick run identity

Date: 2026-08-31

`run_tick_once` now generates a UUID at tick start and passes it unchanged into
the emitted `TickRunRecord` as `:run/id`.  The value is producer-issued rather
than derived from receipt bytes, so reserialising the same receipt cannot
rename the run.

The operational certificate records both the selected ID and its provenance:
`:recorded-run-id` for new receipts, or `:content-sha256-fallback` for legacy
receipts such as the committed 2026-08-30 run.  The fallback remains explicit
so historical evidence stays checkable without pretending it had a field it
did not carry.  `certificate-matches-run?` independently recomputes the
identity from the source record; `--negative-run-id` tampers only the
certificate ID and is rejected.

The Lean `TickRunRecord` gained `runId`, and the `wmRunsOnce` falsifier received
a dated amendment.  Its original "currently firing" wording remains legible
but is marked historical: it described the pre-WM-RUN1 diagnostic selector
failure, not the production operator loop.  Mathlib commits are
`36c9f087f8` (source) and `cbe7148a5f` (regenerated contract).

Canonical focused gates:

```sh
clojure -M:test -m cognitect.test-runner \
  -n wm-operational-certificate-test -n run-tick-once-test
bb -cp . checks/wm_operational_certificate.clj \
  --run holes/labs/wm-contract/tick-run-record-2026-08-30.edn \
  --resource test/fixtures/wm-operational-certificate/resource-clean.edn \
  --certificate /tmp/C101-certificate.edn
bb -cp . checks/wm_operational_certificate.clj --negative-run-id \
  --run holes/labs/wm-contract/tick-run-record-2026-08-30.edn \
  --resource test/fixtures/wm-operational-certificate/resource-clean.edn \
  --certificate /tmp/C101-certificate-bad-id.edn
bb -cp . checks/wm_runs_once_witness.clj \
  holes/labs/wm-contract/tick-run-record-2026-08-30.edn
```
