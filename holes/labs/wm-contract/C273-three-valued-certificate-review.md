# C273 — adversarial review of the three-valued certificate

Date: 2026-09-01

Scope: isolated fixtures and focused tests only. No production click, restamp,
certificate modification, or full workspace gate was run.

## Verdict matrix

The fixture base is the 2026-08-31 nine-hop record, with a matching external
resource receipt. Each row changes one fact.

| Mutation | Resource status | Execution status | Certificate verdict |
|---|---:|---:|---:|
| grounded click outcome, trace written, terminal TRACE | clean | complete | `pass` |
| `traceWritten false` | clean | incomplete | `incomplete` |
| remove terminal TRACE hop | clean | incomplete | `incomplete` |
| grounded outcome, empty route | clean | incomplete | `fail` |
| missing route | clean | incomplete | `fail` |
| click outcome `:rehearsed` | clean | incomplete | `incomplete` |
| click outcome `:incomplete` | clean | incomplete | `incomplete` |
| dirty resource, otherwise complete execution | dirty | complete | `fail` |
| unavailable resource, otherwise complete execution | unavailable | complete | `fail` |
| resource/run identity mismatch | clean | complete | `fail` |
| undeclared hop | clean | incomplete | `fail` |
| topology content-pin mismatch | clean | complete | `fail` |
| non-click receipt, no terminal outcome | clean | **complete** | **`pass`** |

The click-path completion fix works on all three stated legs. No click receipt
reached `pass` without `traceWritten true`, a terminal observed TRACE hop, and a
grounded terminal outcome. Empty and missing routes fail rather than passing
vacuously. No false `incomplete` was found for the stated completed-click
vocabulary (`:grounded-change`, `:grounded-no-change`).

## Finding 1 — grounded completion is conditional on the resource schema

`execution-complete?` requires a grounded outcome only when
`:source-schema :wm-click-resource-v1`; every other resource schema bypasses the
outcome test (`checks/wm_operational_certificate.clj:38-48`). An isolated
`:futon-bounded-test-v1` receipt with resource status clean, command exit 0,
`traceWritten true`, and terminal TRACE — but **no terminal outcome at all** —
produced:

```clojure
{:execution-status {:status :complete :terminal-outcome nil}
 :verdict :pass}
```

This contradicts C268's unqualified statement that completion requires a
grounded terminal outcome. It is also present in the focused test suite's
legacy clean-resource path: the suite still expects a non-click partial-route
fixture to certify.

The defect is narrower than C267: click receipts are protected, but the
certificate type itself still permits a pass without grounded completion. A
consumer reading only `:verdict` cannot know which completion contract was
used.

## Finding 2 — provenance failure is outside the certificate verdict

With a fully complete click fixture and an intentionally wrong `--run-sha256`,
the command correctly exited 1 and printed `fixture-pins :valid? false`, but the
written certificate retained:

```clojure
{:execution-status {:status :complete ...}
 :verdict :pass}
```

Fixture-pin validity is calculated in `main`, after `certificate` constructs
its verdict, and is never incorporated into the written certificate
(`checks/wm_operational_certificate.clj:142-185`). Thus provenance invalidity
is reachable as a command failure but **not** as certificate `:verdict :fail`.
This is another class-9 record: the certificate says pass while its invocation
says the certificate failed provenance.

Topology artefact pins are different: their mismatch is inside `base-valid?`
and produced `:verdict :fail`.

## Failure reachability

- **Topology:** reachable. Both undeclared traversal and a topology content-pin
  mismatch produced `fail`.
- **Identity:** resource/run mismatch produced `fail`. The certificate's own
  recorded identity is derived from the input and cannot independently
  disagree; CLI `--negative-run-id` is a separate post-generation identity
  control.
- **Resource evidence:** dirty, unavailable, missing, and run-ID-mismatched
  resource evidence produced `fail`.
- **Provenance:** command failure is reachable, but certificate failure is not;
  the certificate remains `pass` under an invalid fixture pin.

## Status independence

Resource health and execution completion are now computed independently:

- clean resource + incomplete execution => certificate `incomplete`;
- dirty resource + complete execution => certificate `fail`, while
  `:execution-status` remains `complete`.

This is the intended separation. Neither status mechanically forces the other.
The remaining non-click bypass is in the definition of completion, not a
resource/execution coupling.

## Focused invocations

```sh
bb -cp . /tmp/c273-certificate-matrix.clj

bb -cp . checks/wm_operational_certificate.clj \
  --run holes/labs/wm-contract/tick-run-record-2026-08-31.edn \
  --resource /tmp/c273-grounded-resource.edn \
  --run-sha256 0000000000000000000000000000000000000000000000000000000000000000 \
  --certificate /tmp/c273-bad-provenance-cert.edn
# exit 1; written certificate verdict :pass

clojure -X:test :nses '[wm-operational-certificate-test]'
# 6 tests, 24 assertions, 0 failures, 0 errors
```

## Verdict

C268 closes C267 for the live click schema and its three-valued split behaves
correctly under the requested click mutations. It does **not** yet establish
the stronger certificate-wide claim: non-click receipts bypass grounded
terminal outcomes, and fixture provenance can invalidate an invocation without
invalidating the certificate record it writes.

