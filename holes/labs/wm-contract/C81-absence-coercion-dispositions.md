# C81 — absence-coercion dispositions

Date: 2026-08-31. Authority: `C12-absence-census.edn`. Machine-readable record: `checks/absence-coercion-dispositions.edn`.

The pre-change lint population was 18 grouped sites. Every site now has exactly one disposition: **1 fix-now, 1 exempt-with-reason, 16 blocked**. The lint checks exact coverage against C12 and fails loudly if either population moves.

The fix-now row is `observation.clj:117-145`, implemented at `f94df39`: raw channel presence is retained as tagged provenance, `observation-status` distinguishes a present numeric zero from reason-bearing absence, and `observation-envelope` serializes that distinction. This is the smallest producer-boundary repair and does not reconstruct historical zeros.

The explicit exemption is `trace.clj:249-260`: empty maps/vectors are the documented reader compatibility representation for older or partial serialized records. It is machine-readable as `:versioned-read-compatibility`, not granted by path. Schema-version migration remains named work; the exemption says only that this reader boundary is not a numeric measurement or score.

The sixteen blocked rows are listed individually in the EDN with named dependencies. They form these migrations:

- observation/status consumption: projection vector, risk, avoidance, epistemic diagnostics, infer-mode, policy, belief diagnostics;
- validated producer records: prediction triples, precision error records, rollout steps, unscored moves, adapter error measurements;
- model distinctions: deterministic prediction versus absent variance.

Changing any of these locally would alter live scoring or selection, or manufacture a value the producer never recorded. They remain lint findings rather than being defaulted away.

## Result and controls

Canonical scan: `bb -cp . checks/preemptive_absence_coercion_lint.clj`. Count moved **18 → 16**, all futon2, and exits 1 honestly. Negative control: append `--negative`; it exits 0 only when a synthetic missing-to-zero mutation is rejected. The combined instrument gate remains `bb -cp . checks/preemptive_repair_suite.clj`.

Historical values are unchanged and unreconstructable. C50's counterfactual remains impossible; only post-envelope observations can support new aggregates.

Suite evidence: futon2 `clojure -X:test` completed at **1,022 tests / 6,148 assertions, 0 failures, 0 errors** (the packet's 1,021 baseline plus C81's coverage test). Futon3 `clojure -X:test` reached `library-coherence-test`, reported live evidence-index pagination through 125 pages, then made no further progress for several minutes; it was stopped with exit 130. Therefore the requested futon3 246-test gate is **cannot-verify in this run**, not reported green. It would be settled by a complete offline/pinned evidence-index fixture or a subsequent authoritative run that terminates.
