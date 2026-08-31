# C49 — expected information gain

The C42 carriers are exact matches for the old blocker: `Q(o|pi)` supplies the
outer expectation, `ParameterPriorKernel` is `Q(theta|pi)`, and
`ParameterPosteriorKernel` is `Q(theta|o,pi)`.  No carrier mentions or promotes
the live posterior-spread bonus.

`expectedInformationGain` is now the `Q(o|pi)`-weighted expectation of
posterior-to-prior parameter KL.  Its result has a distinct
`ExpectedInformationGainValue` type.  Prior mass must be strictly positive on
posterior support; otherwise the real-valued KL formula cannot represent the
infinite result honestly.

The independent fixture takes a uniform prior over two parameters and a point
posterior on one of them.  `KL(delta_a || uniform(a,b)) = log(2)`; a single
predicted outcome of mass one leaves the expectation at `log(2)`.  Lean proves
that value, and the executable negative control changes it to `log(3)` and
requires rejection.

The glossary lane is **3 bound / 2 unbound** after registry binding.
`GenerativeModel` is untouched and `modelUncertaintyAndEIG` remains refused:
the existence of canonical EIG does not identify aggregate posterior spread
with it.
