# Frozen meta-injury ledger fixture

This is a verbatim copy of the live meta-injury ledger used by the standing
authority and ordinary-author-refusal tests.

Source: `/home/joe/code/futon2/data/wm-meta-injuries-live-2026-10-22/ledger.edn`,
copied 2026-11-03 when the canonical futon2 HEAD was
`021f30a90098ab5011e7e5ef7e46b64d4f0132f5`.

Why copied: both tests pinned historical states while reading the growing live
store directly. The historical bytes were not recoverable from the archived
ledgers, run records, storage, or test-registry artifacts. This copy freezes
the current 156-entry state so the tests are hermetic. It does not track the
live store.

sha256 at copy time:

```text
123d972144b0ca4fd3216a5ada100b413e7087cad56365d3f973170e01356eeb  live-2026-11-03-156.edn
```
