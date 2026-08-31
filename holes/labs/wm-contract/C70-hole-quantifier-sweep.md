# C70 — hole quantifier sweep

Date: 2026-08-31

The 23 holes in contract authority `acf9c07053863b25e7d1e4d2b6bb33dc56c54977` were checked for universally quantified functions or types admitting a degenerate counterexample. After repairing `valueEvidenceRequiresL2`, **2 of the remaining 22 holes are provably false by that mechanism**:

1. `nonDegenerateAblationLaw`: it quantifies over every `Prior`, `Policy`, policy list, and pair of grading functions, then requires two distinct argmin policies. `Policy := Empty` makes the existential impossible; a singleton policy carrier or identical constant grading functions also defeats the distinctness conclusion.
2. `findF4Falsifiable`: it quantifies over every repository and requires a repository member omitted by `find`. `P := Empty` or `repo.patterns := ∅` makes the existential impossible, independently of the refused `find` implementation.

No other remaining hole has a degenerate universal instantiation that makes its stated proposition false. The `findF1`–`findF3` laws constrain the refused implementation but remain satisfiable; their empty domains make implications vacuous rather than contradictory. `r9VerdictConsultsChecker` has an existing proof over arbitrary parts. The R2/R8/R9 laws quantify only over pinned fixture members. The remaining `Prop` holes are empirical claims or explicit implementation/meta-claim refusals rather than universal function laws.

Per C70, neither newly found false declaration is restated here. Each needs a separate scope amendment and falsifier.
