# C42 — normalized glossary carrier family

## Outcome gate

`Outcome` is already settled by the ratified R5 definition: `Outcome Obs :=
Sigma Obs`, where `Obs : Vertex → Type` and the four vertices are people,
money, organisations, and evidence.  Thus an outcome is an observation carrying
the vertex that determines its coordinate type.  This agrees with R3's typed,
heterogeneous evidence-channel account; it does not guess one global outcome
enumeration.

## Carriers

All six aliases reuse the repaired finite-support `ProbabilityKernel`.  Its
normalisation field is `sum (support s).map (mass s) = 1`, so every condition
mentions the mass it constrains:

- `PredictiveOutcomeKernel PolicyIndex Obs = PolicyIndex ⇝ Outcome Obs` for
  `Q(o|pi)`;
- `ParameterPriorKernel PolicyIndex Parameter = PolicyIndex ⇝ Parameter` for
  `Q(theta|pi)`;
- `ParameterPosteriorKernel PolicyIndex Obs Parameter =
  (PolicyIndex × Outcome Obs) ⇝ Parameter` for `Q(theta|o,pi)`;
- `TransitionKernel State Action = (State × Action) ⇝ State` for `B`;
- `PolicyPriorKernel PolicyIndex = Unit ⇝ PolicyIndex` for `E`; and
- `PreferenceDistribution Obs = Unit ⇝ Outcome Obs` for normalized preferred
  outcomes `C`, kept distinct from the existing vertex-local pragmatic cost.

## Effect on the holes

No hole is discharged in C42.  `GenerativeModel`, `expectedFreeEnergy`, and
`expectedInformationGain` are now carrier-complete and can receive separate
definitions and falsifiers.  Their remaining work is respectively the joint
factorisation law, risk/ambiguity (including KL and entropy), and the
outcome-weighted posterior/prior KL.  `modelUncertaintyAndEIG` remains refused:
the carriers make canonical EIG stateable, but do not turn the live aggregate
posterior-spread bonus into EIG.

The glossary lane therefore remains **1 bound / 4 unbound**.  The contract is
88 declarations: 56 closed and 32 holes.
