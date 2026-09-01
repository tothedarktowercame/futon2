# C396 — what the operational certificate proves about topology

Date: 2026-09-01. This is a read-only lineage audit of
`checks/wm_operational_certificate.clj`, `checks/certify_live_run.clj`, the
production run-record writer, the click resource observer, and Futon3c's
serving-code identity port. No certificate implementation changed.

## Verdict

The certificate proves that the route recorded by the completed run contains
no hop outside the pinned p4ng topology data. It does **not** prove that the
topology-bearing code loaded in the serving JVM is the code or topology that
was checked.

The current claim is therefore **observed traversal conforms to pinned topology
artifacts**, not **the checked topology was the topology loaded and run**.

## What is bound

The production runner derives `TickRunRecord.:route` from the live `:wm/route`
tags accumulated as stages execute. It adds the terminal TRACE hop only after
the trace writer returns a path. The record carries `:run/id`, `:click/id`,
start time, route, and `:traceWritten`.

The external click observer independently binds its receipt to that click and
run ID, encloses the run in time, records terminal outcome and shared-serving-
JVM resource status, and copies the terminal service's
`:serving-runner-code` observation.

`certify_live_run.clj` locates exactly one run record and one enclosing
resource receipt for the run ID. The operational certificate then:

- hashes and validates the pinned p4ng SVG and edge-data files;
- classifies each observed run-record hop against those edge sets;
- rejects undeclared hops;
- binds run and resource identity;
- requires clean resources and observed execution completion; and
- persists hashes of the exact run and normalized resource inputs.

Those are substantive bindings. The topology identifier in the certificate is
not caller supplied: it is recomputed from the two p4ng files.

## The missing binding

The serving JVM is able to report the canonical Futon2 runner file, Git head,
tree SHA, dirty flag, content SHA, load time, PID, and reload stability recorded
when `load-file-recorded!` actually loaded it. `runner-service/status` attaches
that observation to terminal click status, and the external click receipt
retains it as `serving-runner-code`.

The binding stops there. `certificate-resource` omits
`serving-runner-code`, and neither `certify_live_run.clj` nor
`wm_operational_certificate.clj` reads or checks it. The certificate contains
no loaded-code identity and makes no comparison between:

1. the source identity recorded by the serving process at reload;
2. the tested Futon2 tree;
3. the code that emitted the observed route; and
4. the pinned p4ng topology artifacts used for conformance checking.

Even the available serving identity covers the targeted runner source file,
not a closed dependency manifest for all topology-bearing Vars dynamically
resolved by the run. A clean checkout or a p4ng content hash cannot reconstruct
what was already loaded into the long-lived JVM.

Thus a coherent run record can pass against coherent pinned p4ng files while
the serving JVM is running a different runner reload. The observed route may
still conform, but the stronger identity claim remains unestablished.

## Required shape of a later repair

A later change must carry the serving-JVM identity through normalization into
the persisted certificate and compare it with independently tested code
identity. It must also state the identity's scope: one recorded runner file is
not automatically the complete loaded topology. If the dependency closure
cannot be observed, the certificate must report that limitation rather than
promote file identity into topology identity.

No repair is made in this packet, per C396.
