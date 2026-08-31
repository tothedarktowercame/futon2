# CLEANUP C30/C31 — belief-update follow-up (2026-08-31)

## C30 — closed

`observationKernelRowMass` and `beliefUpdate` are now emitted as closed
declarations owned by `P-glossary-mathematics`. The registry therefore contains
82 declarations: 49 closed and 33 holes. `holder_check` resolves both through
the existing by-record owner and reports zero orphaned declarations.

## C31 — blocked on the variance law

The earlier equality `posterior.variance = prior.variance` was wrong: it made
inertness part of the alleged update. It has been removed. `BeliefState` still
requires a nonnegative variance for every channel, but `beliefUpdate` does not
yet constrain how that variance changes.

The paper determines only the family of a rule: an exponential moving average
of squared prediction error with a sensor-noise floor. It does not determine the
EMA rate, the sensor-noise floor per channel, or the mapping from evidence class
(`:self` versus independent) to the precision that should control shrinkage.
Those three values are the `:blocked-on`. Choosing them here would invent the
very confidence policy the falsifier is meant to test.

Accordingly no variance negative control is claimed yet. The existing C16
mutation remains meaningful and passes: a shape-correct posterior whose mean
does not update is rejected. C31 closes only when the three missing inputs are
recorded and a second mutation proves that precision-invariant variance is
rejected.
