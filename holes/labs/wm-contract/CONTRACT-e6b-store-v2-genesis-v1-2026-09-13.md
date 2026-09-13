# E6b isolated store-v2 genesis contract

Status: implemented for fresh temporary isolated stores only. This contract
does not authenticate production authority, authorize restart, or migrate v1.

The durable genesis record is exactly `:wm/e6b-state-genesis-v2`, generation
zero, with an empty prior, no application, an exact
`:wm/e6b-store-state-carrier-v1`, its UTF-8 `pr-str` SHA-256, a nonblank state
revision equal to the carrier revision, a typed fixture authority, and an RFC
3339 committed time. The complete record must strict-EDN round-trip before its
object or HEAD is published. Recovery runs the same validator and additionally
joins any child transaction's prior revision and carrier digest.

The authority is exactly:

```clojure
{:schema :wm/e6b-genesis-authority-v1
 :scope :isolated-test
 :status :fixture-only
 :verifier/source-sha256 <64 lowercase hex>
 :evidence-source-sha256s {<keyword role> <64 lowercase hex>}}
```

This is retained test provenance, never authentication. `FORMAT.edn` must
identify `:wm/e6b-store-format-v2`; an unmarked legacy/interrupted store
refuses and is never silently upgraded. The v1 namespace and schemas remain
unchanged.

Implementation pins are recorded with the focused execution receipt under
`runs/row-22-e6b-store-v2-2026-09-13/genesis-repair/`.
