# C147 — model uncertainty / EIG counterexample binding

Date: 2026-08-31

`modelUncertaintyAndEIG` needs an ongoing witness. Although its Lean theorem
does not decay independently of source, the engineering distinction can decay:
the definitions of the live bonus or canonical EIG can change while an old
annotation continues to say they differ. The appropriate binding is therefore
a Lean proof receipt over the exact counterexample and its defining
declarations, not a second numerical fixture.

`checks/model_uncertainty_eig_witness.clj`:

- pins `modelUncertaintyBonus`, `parameterInformationGain`,
  `expectedInformationGain`, and `modelUncertaintyAndEIG` by declaration digest;
- re-elaborates the public theorem and records its axioms;
- rejects a collapsed equality in Lean as its semantic negative control.

The contract now names evidence `proof term` and the falsifier: the normalized
point-mass counterexample no longer elaborates, or the collapsed equality does.
The registry binding is inspectable through the existing Lean-proof-receipt
shape checker.

Glossary target lane: **5 bound / 0 unbound** — `logMultivariateBeta`,
`expectedFreeEnergy`, `expectedInformationGain`, `GenerativeModel`, and
`modelUncertaintyAndEIG`. The last is a counterexample rather than an
implementation conformance theorem, but it is now held to its stated
distinction by an executable check that can fail.
