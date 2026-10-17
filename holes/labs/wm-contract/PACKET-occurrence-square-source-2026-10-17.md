# Occurrence-square source before G — 2026-10-17

## Claim

The reading-family route now has a content-bound, occurrence-keyed source for
authored four-map squares.  This source is read while policy families are
constructed, before G compares those policies.  The later runner field named
`:action` remains only an envelope for the selected cascade policy; it is not
the scoring grain and is not an authority for square construction.

## Carrier and checks

`futon2.aif.occurrence-square-registry` stores
`:wm/occurrence-square-registry-v1` records under the target and target-reading
analysis digest.  Every entry is keyed by `[pattern-id fragment-index]` and
binds that same occurrence id, the pattern id, the pattern source SHA and one
`:wm/three-halves-square-v1` value.  Publication and lookup check:

- occurrence identity is not collapsed to pattern identity;
- every square passes `three-halves-square/validate`;
- all four objects carry the declared pattern pin;
- all four object texts equal the corresponding written pattern parts;
- the registry seal covers the complete record.

`target-policy-family` attaches these squares to the matching occurrence nodes
before the policies reach scoring.  Missing records remain typed
`:occurrence-squares-not-authored`; maps are never inferred from reading roles.

## Gate

The focused registry test uses the real `publication-cadence` pattern bytes
and exact four written parts.  It covers publish/load/projection, the default
library resolver, a wrong pin, a pattern-grain key and typed absence.  The
existing target-policy-family, family-selection and reading-family-decision
tests are the regression boundary.

## Remaining live-readiness condition

This packet installs the verified store and the pre-G consumer.  It does not
pretend that existing target readings contain the four authored maps: they do
not.  Live readiness therefore remains conditional on the upstream
reading-family authoring phase producing and publishing one validated entry
for every occurrence required by a candidate policy.  Until that producer and
its same-phase redecision are wired, absence is evidence of unfinished
construction, not permission to fabricate a square and not evidence that G is
being evaluated over atomic actions.
