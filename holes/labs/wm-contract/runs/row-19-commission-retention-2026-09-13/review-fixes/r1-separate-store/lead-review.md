# Separate archive review — codex-26, 2026-09-13

Subject: futon3c `009cb3fb..2e4ed7ef`, receipts `c5829fb4`.
Disposition: changes requested before retention acceptance or genesis admission.

All three source/runner SHA256 pins match actual bytes (lead-pins.json).
Retained raw counters show 14 passing assertions, induced failure exits 1,
kondo zero warnings/errors, and the actual parens entrypoint returns OK.
No tests rerun during this source-and-receipt review.
The keyed store removes archive bodies from the ordinary hot ledger; requested
archived IDs refuse reuse. Those original defects are repaired at this scope.

Remaining concrete failure paths in persist-commission-archive!:

1. After Files/move succeeds, directory force may fail. Hot deletion correctly
   aborts once; on retry the existing-file branch returns without forcing the
   directory, so the durability barrier is bypassed. Newly created archive
   directories also require their parent entry made durable. Exercise failure
   after publication, not only a stub throwing before archive work starts.
2. Existing-file retry compares maps after removing archive-digest and archived-at,
   without validating the stored digest. Corrupt only the archive-digest of an
   otherwise identical prepublished record: retry accepts it and expiry deletes
   the good hot evidence, then the read API refuses the sole remaining archive.
   Validate existing archive schema, identity and digests before permitting drop;
   malformed/corrupt archive must preserve hot evidence with a typed refusal.
3. The hot/archive disagreement check compares commission and request digest only.
   Equal commissions with differing trace-id/artifact-ref or other retained join
   fields still return the archived join labelled hot-ledger. Compare the full
   retained immutable projection, or refuse coexistence if equivalence is not
   established. Add equal-commission/different-join negative control.

These are source-derived counterexamples, not claimed executed probes.
Next packet should cover interrupted publication/retry, corrupt existing digest,
and equal-commission join disagreement, with failure-sensitive retained output.
The current test establishes a pre-write failure and an ordinary successful retry;
it does not cover the failure boundary above. No live reload or anchor created.
