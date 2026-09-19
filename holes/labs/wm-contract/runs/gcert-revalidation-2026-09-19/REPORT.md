# LF-certificate-witness-checker: existing lane revalidated

Verifier: codex-32, 2026-09-19. Independent acceptance remains with claude-4.
No checker, generator, production code or selection certificate was changed.

Warrant: `test-registry-c52af2af5c2a5164f2ecc2be85cf0296b72dfec31f4b3a3eeb269a1a51ee94f6`.
It resolves through `check-record!` with `:warrant? true` and is bound to
`LF-certificate-witness-checker` through `futon3c.test-registry.validation`.
`binding.edn` records the binding and successful check. `warrant-summary.edn`
is explicitly a summary; the complete warrant and SHA chain live in the registry.
`registry-config.edn` is the exact registration configuration.

## Measurements

- Existing generator replayed the emitted certificate at
  `runs/gcert-witness-2026-09-18/certificate.edn`. Its output `regenerated.lean`
  is byte-identical (`cmp` exit 0) to mathlib4's committed
  `DarkTower/AIF/Witness/GCertFixture20260918.lean`.
- Registered `lake build DarkTower.AIF.Witness.GCertFixture20260918`: exit 0,
  14,956 ms, 0 errors. Mathlib4 HEAD:
  `4199d8ebe02c01c54753d4c62720986b084be0c8`.
- Direct `lake env lean regenerated.lean`: exit 0. The existing theorems
  establish `GCertificate.valid` at tolerance 1e-9 and
  `riskComputedThroughout` via the two soundness theorems.
- Tampered total: original total + 1, original steps unchanged. Generated with
  the existing generator; Lean exit 1, `native_decide` reports
  `GCertificateQ.check ε cert = true` is false.
- Dishonest zero reduction: first-step ambiguity changed from 0 to 1, and total
  also increased by 1 so the arithmetic stays within tolerance. The original
  `reducedIdenticallyZero` status is retained. Lean exit 1, same checker rejection.
- Deliberately wrong guard: the valid certificate followed by
  `#guard cert.check ε = false`. Lean exit 1 at that guard: expression did not
  evaluate to true. The positive run therefore did not pass by being empty.

`executions.json` records all four direct commands, exits, source hashes and
full stdout/stderr. The generated Lean inputs and both mutated EDN inputs are
retained alongside it. These direct controls supplement the registered positive
module build; they are not falsely represented as tests inside that module.

The registry reported 8,506 build jobs and 9 existing sorry warnings, all named
in `DarkTower/WarMachine/Holes.lean`. A Lake job count is not a theorem/test count.
Neither certificate soundness theorem nor the witness proof was replaced.
The checker certifies the recorded predicates, not the truth of upstream
provenance or the correctness of the Clojure risk calculation.

## Scope and gates

All Lake commands ran from the canonical mathlib4 source checkout, which retains
its own `.lake/packages` directory. No APM worktree, runner probe, machine-data
edit, shared JVM load or restart occurred. No Clojure/Lisp files were changed;
clj-kondo/check-parens are therefore not applicable to this evidence-only commit.
The two policy holes remain untouched. No further contradiction of the corrected
packet was found. R9 acceptance is for its independent reviewer, not this verifier.
