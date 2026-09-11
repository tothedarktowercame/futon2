# Independent contract review — 2026-09-11

Reviewed d1fd9422. Accept the design direction for a separately typed historical
verification implementation; do not admit the current envelope or activate its
packets. No ordinary fresh-author or grounding invariant is relaxed.

Executed qualification: positive and three controls pass, but a missing author,
invented review-job string and a single anonymous passed check also qualify.
That predicate is a proposal illustration, not executable review admission.
Production needs server-derived executed review evidence, distinct nonblank
identities, exact source/finding pins, and a preregistered nonempty check
population with exact coverage, result digests and actual consumer execution.
An unexecuted job, duplicate/missing/foreign checks and changed source must
refuse. Independent review must bind the exact verification output. Existing
code identity remains immutable; the new record describes verification only.

Replaced the tautological Lean theorem with a typed two-transition relation.
Checked with canonical mathlib4 `lake env lean`: exit 0. It now proves no
open-to-resolved transition, resolution requires an awaiting-validation source
and a distinct reviewed/grounded successor, and verification requires complete
qualification. These remain declared abstract obligations, not runtime proofs.

The proposed series file is not executable: it declares :wm/run4-series-v1
and :ordered-trials, while the controller requires :wm/run4-series-pin-v1 and
:trials. Treat both prepared files as planning data; generate canonical pins
with production parser/preflight tests, without activation, after the action
contract is implementable. Target two denotes verification then a distinct
successor, not two successful U88 tasks. Capacity and old evidence unchanged.

Implementation sequence: a disabled reusable verifier with authoritative
ports and meaningful negative controls; independent review; action integration
through normal selection/admissibility/reviewer lifecycle; canonical packet
freeze and actual isolated execution; only then live admission. No new user
approval loop for this already authorized preparation.
