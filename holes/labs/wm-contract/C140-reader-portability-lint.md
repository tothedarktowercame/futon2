# C140 — persisted-file reader portability lint

Date: 2026-08-31

`checks/reader_portability_lint.bb` rejects bare `read-string` directly
composed with `slurp`. A genuine Clojure-source read may be exempted only by a
nearby annotation of the form:

```clojure
;; reader-portability: allow-source-read reason=<nonempty-reason>
```

The real scan currently exits 1 with **12 findings across six files**:

- `checks/holder_check.clj` — 1;
- `scripts/edge_census.bb` — 3;
- `scripts/merge_edges.bb` — 2;
- `scripts/merge_witnesses.bb` — 3;
- `scripts/work_units.bb` — 2;
- `scripts/futon2/report/war_machine.clj` — 1.

The last is a correction to C135's five-boundary census: the production report
reader loads daily-scan `.edn` frames with source `read-string` and maps a
parse exception to `nil`. It is a sixth live persisted-file boundary, not a
source-reading exemption. This pass records it and does not migrate it.

Two independent controls ship with the lint. A bare persisted read is found;
a deliberate `.clj` source read with a nonempty reason annotation is exempted.
Exit convention is `0=pass, 1=real findings, 2=control slipped`.

## Scoped round-trip boundary

The proposed normal-gate round trip should cover the cross-runtime artefacts
named by C135: WM trace records, preference-stack/serialized witnesses, fold
escrow and repair-obligation records, operational run/resource receipts, edge
fragments plus their merged schema, and witness fragments plus their merged
registry. It certifies only that those named artefacts preserve equal values
through JVM `pr-str` → Babashka `clojure.edn/read` and Babashka `pr-str` → JVM
`clojure.edn/read`; it does **not** certify arbitrary repository EDN, tagged
literal semantics outside the configured readers, JSON, or Clojure source.

The complete 1,160-file sweep remains a nightly/CI check because it costs
about one minute across both runtimes. No round-trip implementation or reader
migration is included in C140.

## Canonical invocations

```sh
bb checks/reader_portability_lint.bb                         # exit 1: 12 named findings
bb checks/reader_portability_lint.bb --control-bare          # exit 0
bb checks/reader_portability_lint.bb --control-exempt-source # exit 0
clojure -T:build ci
cd /home/joe/code/futon3 && clojure -X:test
```
