# C47 — expected free energy

`expectedFreeEnergy` now computes `KL[Q(o|pi) || C] + ambiguity` over the
predictive kernel's declared finite support.  It requires preferred-outcome
mass to be strictly positive on that support: zero preference against positive
prediction has infinite KL and cannot honestly inhabit the declaration's
real-valued result type.

The independent one-point fixture uses `KL(delta_x || delta_x)=0`; ambiguity
2 therefore gives expected free energy 2.  The Lean witness proves both this
value and the decomposition bridge.  The executable negative control changes
the expected value to 3 and must reject it.

`G` and `expectedFreeEnergy` return the same `ExpectedFreeEnergyValue`, but are
not unconditionally equal.  `G_eq_expectedFreeEnergy` proves equality under
the exact bridge assumptions: `G`'s risk argument equals the kernel-derived KL
and `ambiguity(pi) = -epistemicGain(pi)`.  Without those assumptions they are
two input interfaces to the same semantic quantity, not interchangeable
functions.

After binding, the glossary lane is **2 bound / 3 unbound**.  C47 leaves
`GenerativeModel` and `expectedInformationGain` untouched and preserves the
`modelUncertaintyAndEIG` refusal.
