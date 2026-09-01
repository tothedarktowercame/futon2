# C304 — known mutable read-set conversions

Date: 2026-09-01

## Converted in this pass

Five of C293's thirteen remaining known checks cleanly fit the C297 substrate:

| Check | Policy and captured population |
|---|---|
| `r8_pinned_snapshot_witness.clj` | Verdict-required; fixture EDN, generated Lean, and mathlib source are one stable read-set. |
| `r9_proof_receipt_check.clj` | Verdict-required; after receipt-driven path discovery, receipt and named Lean source are recaptured together. A changed source pointer is loud. |
| `holder_check.clj` | Verdict-required for files; contract JSON and holder registry are captured together. The Agency roster remains a separately typed reachable/unreachable observation rather than being falsely included in the file snapshot. |
| `control_organization_check.clj` | Verdict-required; organization discovery is followed by one capture containing organization, stages, edges, and every pinned `:reads` target. Pin hashes use captured bytes. |
| `control_map_figure_agreement_check.clj` | Verdict-required; EDN, SVG, and PDF bytes are captured together. `pdftotext` runs on a temporary copy of the captured PDF, not the mutable original path. |

Every converted check returns no semantic verdict if its read-set moves.

## Eight deliberately not converted

| Check or family | Why C297 does not fit without a semantic change | Required next boundary |
|---|---|---|
| preemptive repair corpus | Dynamic tracked population across three dirty worktrees | Four-/three-repository basis sandwich around enumeration, then `UNAVAILABLE` on movement. |
| `control_map_lint.clj` | Edge file plus a dynamically discovered node-record population | Enumerate, capture the complete discovered population, then verify enumeration basis did not move. |
| `contract_lint.clj` | Mixes live authority/registry with immutable pinned Git blobs and multiple negative modes | Separate live read-set from immutable object reads before conversion. |
| `lane_registry_check.clj` | Multiple live Agency job reads cannot be made one snapshot by a caller | C301 service-issued ledger revision; interval-only reporting until reload. |
| `reader_portability_lint.bb` | Dynamic repository population is the subject | Repository basis sandwich, not a fixed path list. |
| `r2_channel_contract.clj` | Discovers trace/source population and revisits sources in subchecks | One enumerated corpus object plus basis sandwich. |
| `absent_is_loud_lint.clj` | AST scan over a dynamic tracked corpus with HEAD annotations | Capture corpus and dirty-byte basis together; HEAD alone is insufficient. |
| `q_interface_completeness_check.clj` | Live source is interpreted using historical `git log/show` evidence | Bind the live digest to the historical explanation and report movement; a file-only snapshot would cover only half the claim. |

This is refusal by boundary, not deferral by size. Applying `observe-files` to
only the obvious paths in these eight would make their output look protected
while leaving the dynamic or service population outside the observation.

## Focused results

```text
R8: positive exit 0; pin mutation rejected exit 0
R9: positive exit 0; tampered source rejected exit 0
holder: positive exit 0, 119 declarations/0 orphaned; dead-holder mutation rejected exit 0
control organization: positive exit 0, 22 edges; wrong-column mutation rejected exit 0
figure agreement: positive exit 0, 20 nodes/10 control/12 support/8 measured;
                  broken-label mutation rejected exit 0
clj-kondo: 0 errors, 0 warnings on all touched checks
```

The 90-candidate C293 tail remains untouched and explicitly unqualified.
