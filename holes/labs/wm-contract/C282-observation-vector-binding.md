# C282 — complete observation-vector binding

Date: 2026-09-01

The seventh notation collision is `o`: a single vertex-tagged `Outcome` and the
complete fourteen-coordinate observation vector are different objects. The new
nominal `ObservationVector` also refuses a `Channel -> Option Real` partial map,
so typed absence cannot be silently defaulted into a complete update input.

The independent fixture enumerates every channel exactly once and assigns
values 0 through 13 in declaration order. `predictionError` and `beliefUpdate`
now consume the nominal vector, while producer-side absence remains in its
measurement envelope. This witnesses completeness, not observation semantics.

Q-facing definitions are unchanged; the Q pin is refreshed after verification
to `fef255009bf391b75900fb0fee35bcb7073f21660f89ad8d7df6c6e54753cdf1`.
`ObservationVector` is explicitly classified in the `:belief` model area.
Its owner uses the stable glossary paragraph anchor; regeneration caught and
removed the initially introduced line-number drift before publication.
