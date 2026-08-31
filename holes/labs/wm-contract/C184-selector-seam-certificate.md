# C184 — selector-seam certificate compatibility

Date: 2026-08-31

The operational certificate did not read `:selectorSeam` before this pass. Its
verdict depends on run identity/timestamp, route-to-topology conformance,
topology pins, and resource health. It does not use the mission-selection seam
to derive identity or traversal.

C110's production selector returned `:status :verified-live-selection` through
the Agency HTTP strategic-selection endpoint. The production-shaped seam used
here is therefore the explicit string
`"agency-http:verified-live-selection"`, rather than the diagnostic
`"stub:first-ranked-authorized-mission"`.

The control read C167's exact run record, changed only `:selectorSeam`, and
certified it with C167's exact resource input:

```sh
bb -cp . checks/wm_operational_certificate.clj \
  --run /tmp/C184-production-shaped-run.edn \
  --resource holes/labs/wm-contract/C167-v20-certificate-resource.edn \
  --certificate /tmp/C184-production-shaped-certificate.edn
```

It exited 0 with the same run UUID, topology, traversal counts (9 total, 3
original, 6 measured, 0 undeclared), clean resource status, and `:pass`
verdict. The selector seam does not alter certification.

The certificate now nevertheless preserves the field as evidence:

```clojure
{:selector-seam {:status :present
                 :value "agency-http:verified-live-selection"}}
```

A second control removed only `:selectorSeam`. Certificate arithmetic remained
seam-independent and passed, but recorded
`{:status :absent :reason :not-recorded}` rather than nil. The downstream
`wm_runs_once_witness.clj` rejected that same record with
`FAIL blank-string`, exit 1. Thus seam presence remains mandatory at the
TickRunRecord/witness boundary while the operational certificate neither
mistakes selector provenance for route evidence nor silently erases absence.

No production tick, selection, scoring, or actuation ran.
