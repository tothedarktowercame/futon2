# Row 16 R5 ambiguity bridge-or-divergence

The new module keeps the registry's categorical `Holes.ambiguity` unchanged and
names production's separate engineering quantity
`gaussianChannelAmbiguity`. The latter is the sum—not mean—over every
non-learn-action channel of `1/2 * log(2*pi*e*max(variance,1e-9))`. The
learn-action branch instead takes the zone evidence's scalar predictive
variance and is explicitly outside that declaration.

The concrete divergence pairs a deterministic categorical observation kernel
(entropy zero) with a one-channel unit-variance Gaussian (strictly positive
differential entropy). A genuine but isolated agreement exists: the same
deterministic categorical kernel and Gaussian variance `1/(2*pi*e)` both have
zero entropy. This does not define a carrier conversion or justify relabelling
the Gaussian calculation as categorical ambiguity.

Recommendation for the later R5 estimator decision: build the declared
categorical estimator from row 9's policy-conditioned predicted-state
distribution and row 6's admitted observation kernel A. Keep the existing
Gaussian variance lane as a separately registered engineering quantity, with
its non-learn-action and learn-action-zone branches named distinctly. Do not
use the isolated zero-entropy equality as an adapter between them.

No registry or witness fragment change belongs to this packet. The R5
measurement remains blocked until that estimator choice is implemented and
captured.
