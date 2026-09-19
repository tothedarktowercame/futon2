# WIRE-f-on-tick — selection certificate emission

Author: codex-32. Implementation: d8047a30b41ae4bcb28f37fabf66502a36e30765.
Independent review is pending; this is implementation evidence, not acceptance.

The selector now records the consumed F and E, their input presence (absent,
null, false or present), and the reason for a neutral value. Per-policy finite
SelectionCertificate fields accompany the candidate records. Computed infinity
stays in those candidate records; consumed F is 0, with
`computed-not-attached/non-finite-under-identity-a`. The existing computation,
ranking, posterior and actuation fields are unchanged. No runtime gate added.

Warrant: `test-registry-1ef986623fd9683af9ec1f6d3ca2840bffe5f1df3d621962405bdc06a610b5ce`.
Subject binding: `WIRE-f-on-tick`, using `futon3c.test-registry.validation`.
The warrant was checked successfully before binding. See `warrant.edn` and
`binding.edn`; the registry durably stores the test log by content.

Measured: 4 tests, 53 assertions, 0 failures, 0 errors, 4072 ms registered
execution. Six before/after decision byte comparisons passed: two candidate
families at beta 0.25, 1 and 2, including tick-001's four recorded G scores and
a scorer-produced two-candidate infinite-F field. Both comparison arms cross
the same EDN read boundary; this matters because a set can change printing
order on read. The comparison removes only the newly emitted certificate.
All existing selection fields, including posterior probabilities, are compared.

The existing `DarkTower.AIF.SelectionCertificateQ.check` accepted 2 emitted
certificates and rejected 3 mutations: beta = 0, declared-neutral habit = 2,
and computed-not-attached consumed F = 1. `receipt.edn` is extracted from the
registered test log and records the complete selection, generated Lean source,
and Lean exit 0. This is an offline replay through the production selector,
not a production click. Zero full-loop runner invocations or shared-JVM loads.
The Lean check ran from the canonical mathlib4 checkout at 4199d8ebe0; its own
packages directory was retained. The Clojure warrant pins its Clojure load
closure; it does not itself fingerprint the external Lean toolchain.

clj-kondo: 0 errors, 0 warnings on both changed Clojure files.
futon4/dev/check-parens.el: OK on both files. git diff --check: clean.

## Findings against the packet

The non-finite-F guard is not generally unreachable: explicitly attached
positive infinity, negative infinity and NaN all produce the existing typed
`invalid-free-energy` refusal (3 assertions). The scorer deliberately omits
non-finite F upstream, so those particular inputs consume neutral F by design.
The change makes that decision observable; it does not revive or modify the guard.

Lean's checker validates positivity and honesty of neutral values; it does not
prove the upstream computation or authenticate provenance. Attached input status
maps explicitly to Lean's `computed`; the accompanying input record preserves
what selection was actually supplied. Neither `policyPosteriorImportsPolicyF`
nor `policyPrecisionIsGammaFromBeta` is discharged.
