# C408 — reconstructed census basis

C407's commit choices are admissible as **reconstructed re-evaluation anchors**,
but not as the census author's observation basis. Presenting them in the same
scalar map as the Futon2 artifact commit was therefore the wrong representation,
even though the selected commits are useful.

Registry schema v2 makes the distinction machine-readable. Futon2 is
`:provenance :artifact-commit` with `:correspondence :verified`. Futon3c and
p4ng are `:provenance :reconstructed-at-artifact-time`, carry the exact method
`:latest-commit-not-after-artifact-commit-time`, and state
`:correspondence :unverified`. The checker validates these fields and renders a
`:basis-assessment` in every result. A comment no longer carries the caveat.

The reconstructed anchors do not block the gate while their epistemic status is
reviewed. They support the narrower statement “these subjects differ or agree
relative to the reconstructed commit,” not “these are the bytes the census
author observed.” A future census must author-record every cross-repository
basis; reconstruction is a repair for historical inspectability, not an allowed
substitute at creation time.

Focused verification: the live census is `:possibly-stale`, exit 0, and exposes
both reconstructed bases as correspondence-unverified. The existing control now
also removes the required reconstruction method and is rejected as unavailable.
Clj-kondo is clean; inventory is `{:exit 0, :unknown (), :missing ()}`.
