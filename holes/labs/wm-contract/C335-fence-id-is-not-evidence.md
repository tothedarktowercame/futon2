# C335 — a fence ID is not evidence

Date: 2026-09-01. Owner: `wm-evidence`.

## Finding and correction

The counterexample was in `scripts/wm_preflight.clj`, not in the world-observing
bundle: `--writer-fence arbitrary-id` was sufficient to emit `:event-free?
true`. The evidence bundle itself observed the live world, but its attestations
were associated with a window only by filename.

Both trust boundaries now require evidence:

- `checks/writer_fence_evidence.py` accepts a fence ID only together with a
  structured, current attestation whose content names the same ID, exact writer
  population, intended state, acknowledgers, and validity interval. It records
  the canonical attestation content SHA-256 and still observes the world twice.
- `scripts/wm_preflight.clj` requires both `--writer-fence ID` and
  `--writer-fence-evidence RECEIPT`. It validates the receipt and reruns the
  world-observing bundle with the receipt's embedded attestation. A bare ID,
  stale receipt, foreign receipt, prior breach, or current breach leaves
  `:event-free? :unverified` and exits nonzero.

Therefore an ID names a window but never establishes one. `:event-free? true`
is reachable only when a prior content-bound receipt and a fresh live
`FENCE-VERIFIABLE` observation agree. This is content binding, not a digital
signature; the named humans remain attestations rather than machine-observed
facts.

## Controls

`python3 checks/writer_fence_evidence.py --self-test` reaches all three
outcomes (`FENCE-VERIFIABLE`, `FENCE-BREACH`, `FENCE-INDETERMINATE`) and rejects
foreign and stale attestation content.

`clojure -X:test :nses '[wm-preflight-test]'` demonstrates that a bare ID is
unverified, matching content plus a fresh verifiable observation is accepted,
and a fresh live breach defeats a previously verifiable receipt.

The original live counterexample was repeated with a syntactically complete
attestation for `fabricated-c335`. The unfenced world produced exit `1`,
`FENCE-BREACH`, with 9 findings at the first observation and 10 at the second;
the arbitrary ID did not override them. Separately,
`clojure -M:wm-preflight --writer-fence fabricated` produced exit `1` and
`:event-free? :unverified`.

## Canonical sequence

```sh
python3 checks/writer_fence_evidence.py \
  --fence-id "$FENCE_ID" --attestations "$ATTESTATION" > "$FENCE_RECEIPT"
clojure -M:wm-preflight \
  --writer-fence "$FENCE_ID" --writer-fence-evidence "$FENCE_RECEIPT" MISSION
```

The second command deliberately re-observes the fence rather than trusting the
first receipt alone. Operator procedures that supplied only `--writer-fence`
must be updated before use.

Inventory at delivery: `{:name :check-inventory, :exit 0, :unknown (),
:missing ()}`.
