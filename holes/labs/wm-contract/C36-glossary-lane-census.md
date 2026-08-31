# CLEANUP C36 — corpus pin and glossary-lane census (2026-08-31)

## C36 pin repair

The variance-input record is re-pinned to the live corpus on which its
statistics were computed: **54 files, 799 forms**, digest
`84f356c44a5c9afe5bf3870ca398f9424176a8ba2fe246a718e4280365890866`
under `:sha256-over-newline-joined-sorted-form-sha256`. I chose re-pinning,
not recomputation at the old baseline. Per-channel sample counts remain lower
where a trace did not contain that channel. The C32 reasoning is unchanged:
these observations justify named defaults only by showing non-identifiability;
they are not treated as parameter estimates.

## Glossary lane: bound count

The lane now has **five contract holes, zero bound and five unbound (0/5)**.
This morning's measure was 0/8. Three former glossary holes are now closed, and
two new closed declarations (`beliefUpdate`, `observationKernelRowMass`) were
registered, but none of that creates a witness binding for a remaining hole.
The binding measure therefore remains zero. The update's Lean falsifiers witness
the closed update relation itself; they do not discharge an adjacent glossary
hole.

### Remaining holes

- `logMultivariateBeta` — **dischargeable now**, after importing the Mathlib
  Gamma/log-Gamma theory, deciding the positive-concentration domain explicitly,
  and supplying a numerical/reference identity fixture for
  `LogMultivariateBetaWitness`. The current `List ℝ` signature admits invalid
  concentration values, so the domain must be made part of the carrier rather
  than silently assumed.
- `GenerativeModel` — **blocked** on a named transition kernel and normalized
  policy-prior carrier, plus a factorization fixture. `observationKernel` now
  supplies only the observation factor; there is no joint artefact for
  `GenerativeModelWitness` to check.
- `expectedFreeEnergy` — **blocked** on normalized `Q(o∣π)` and the preference
  distribution it is compared with. The current argument called
  `outcomeKernel` is an unnormalized bare function, and defining risk plus
  ambiguity while ignoring it would repeat C7's vacuous-kernel defect.
- `expectedInformationGain` — **blocked** on `Q(o∣π)` and a parameter
  prior/posterior kernel. `Outcome` is now typed, but an outcome type alone is
  not the missing predictive distribution.
- `modelUncertaintyAndEIG` — **blocked/refused**, sharing the same `Q(o∣π)`
  and parameter-kernel blocker. Its refusal correctly prevents posterior spread
  from being relabelled expected information gain.

There is no `cannot-be-witnessed-by-construction` row yet. Four holes name
potential evidence kinds but have no registry binding or artefact; they are
unwitnessed, not inherently unwitnessable. The fifth is an explicit refusal
with no evidence kind. If a future binding points to executable source rather
than a serialized witness, it should receive the same `cannot-shape-check`
classification as C27's `PreferenceStackWitness`; none currently does.
