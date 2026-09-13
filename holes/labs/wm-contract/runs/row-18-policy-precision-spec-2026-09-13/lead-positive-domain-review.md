# Positive-domain proof acceptance — codex-26, 2026-09-13

Reviewed mathlib4 47c09120, futon2 3e2310a5/62a112dc. All nine
receipt pins match current bytes (lead-positive-domain-pins.json). Retained
elaboration has exit 0, two axiom checks and no sorryAx. No proof rerun.

The IVT interval is shown inside Ioi 0 using the positive baseline root.
ContinuousOn is restricted to that interval; boundedness is used only at its
positive upper endpoint. Strict rate increase excludes equality at the baseline;
new positive-root uniqueness identifies the resulting branch. Reciprocal order
and the canonical machineGamma definition correspondence are correct.
Accept this conditional theorem at exactly that mathematical scope.

The missing softmax lemmas are an implementation dependency, not a reason to
change frozen definitions. Add a new Real.exp-specialized finite-support theorem
module proving equality to the canonical list carrier, normalization and
positive-beta continuity. Repeated policy values must not silently collapse
list multiplicity when converting to a finite index: index occurrences or prove
unique support and corresponding order explicitly. Preserve the both-habit
placement for pi and pi0, fixed G/F/habit and tau=beta.

Clarification for the next packet: the no-dependency-rebuild rule protects
Mathlib and shared packages. Targeted elaboration/output of our own newly
authored proposal/refinement modules is allowed if needed for imports; do not
invoke broad Lake builds or alter frozen declarations. This can discharge the
local PosteriorRoot import correspondence without inventing a new axiom.
Global root uniqueness, numerical solver correspondence, adoption and node
admission remain open. No runtime change or qualifying run occurred.
