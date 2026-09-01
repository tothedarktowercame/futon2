# C302 — first guarded negative-control migration (2026-09-01)

Five high-value glossary witness suites were migrated first, comprising seven
Lean negative fixtures:

- variational F versus expected free energy (C212);
- Bayes threshold versus variational F (C236);
- model-reduction change versus present variational F (C277);
- parameter posterior versus prior and predictive outcome kernels (C270);
- observation vector versus a partial map and a single outcome (C282).

Each failing command now sits under an exact `#guard_msgs` expectation.  The
outer Clojure wrappers require the guarded file to exit zero.  Thus the intended
diagnostic is success, while a missing import, syntax error, additional error,
or changed mismatch is failure.  The registry records semantic purpose and
fixture identity under `:expected-rejection` without duplicating the rendered
diagnostic.

The pattern held for both application mismatches and direct type mismatches.
The Bayes-threshold `#check` form emitted an additional informational message;
using a guarded definition isolated the intended application mismatch.  The
model-reduction fixture also needed its input definition marked
`noncomputable`, removing an incidental diagnostic so the guard covers only the
semantic type mismatch.  These were local fixture-shaping costs, not weakened
controls.

Migration cost for this batch was seven source guards and five small wrapper
inversions; all seven controls execute in roughly 25 seconds on this checkout.
The remaining controls retain the C299 interim audit cadence now recorded in
`RUNBOOK.md`.

The first registry edit also exposed that `:run-sha` is named more broadly than
it is used: contract lint resolves the fixture bytes at that SHA.  Pointing it
at the new wrapper commit made five unchanged fixtures stale.  Their historical
fixture pins were therefore retained; guarded-source identity lives in the new
metadata instead of overloading the fixture pin.

No binding, contract declaration, Q-facing definition, or model area changed.
Glossary coverage remains 31/33.
