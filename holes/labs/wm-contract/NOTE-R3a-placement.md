# NOTE-R3a-placement — basis for the U50 row (mint at next loop pause)

Date: 2026-09-04. Author: claude-1. Trigger: Joe, reading plop-2026 Figure 2
on his phone: "There's a very bizarre-looking node called R3a... beyond the
ACT column... says Stage Absent from Registry... edges spanning all the way
back to R2 and R7... is very confusing."

## What R3a is (all receipts already in the registries)

- Node: `:R3a` in `p4ng/empirics-futon/control-map-edges.edn:11` (node list),
  with two measured edges (`:130-139`): R2→R3a via
  `futon2.aif.free-energy/compute-prediction-error`, R3a→R7 via
  `futon2.aif.precision/update-precision-state`; receipt
  `futon2/holes/labs/wm-contract/pair/R2-R7-delivery.edn`.
- Meaning: the pairing decomposition of the measured R2→R7 route — "R2's
  14-channel observation is projected by R3a before R7; no direct R2-to-R7
  call exists." R7 consumes the 8-channel post-R3a prediction-error
  projection.
- Why unplaced: `control-stages.edn` is generated from the hand-drawn base
  figure (WR-8 — never edited from this side); R3a was born from measurement,
  not drawing, so `gen_live_topology.bb:133-138` parks it red at x=1110
  ("stage absent from registry") rather than guessing a column.

## The row to mint (:U50, class :D, owner :any)

Statement: place R3a in the generated stage registry through a code-side
placement with stated basis, so both papers' live figures render it in a
column instead of unplaced. Precedent mechanism:
`gen_control_stages.py:48-56` EXTERNAL_MEMBERS places TRACE with
stage/band/coords + a code-citing basis (note: its comment scopes it to
non-R-numbered endpoints, so the worker may extend it with a
measured-members list rather than overloading it).

Placement question the worker must ARGUE, not assume (this ambiguity is why
the generator refused to guess): BELIEVE (R3a is a sub-step of R3 belief
update; both its consumer R7 and namesake R3 sit there) vs PERCEIVE (it
computes a mismatch, like R8 present-fit mismatch). Decide from the call
chain (where compute-prediction-error sits between observation assembly and
precision update) and the drawing's own column semantics; if code reading
cannot settle it, that is a :decisions entry, not a coin flip.

Acceptance: R3a renders inside its argued column (loop band) with a
code-citing basis; the unplaced list is empty; `gen_live_topology.bb
--check` green (bare exit); both control-map-live consumers regenerate;
negative control shown: a genuinely unknown node id still renders unplaced
red. If Joe later draws R3a onto the base figure, the drawing-derived
assignment supersedes the code-side one — say so in the placement's basis.

Interim (done 2026-09-04): plop-2026's Figure 2 caption names R3a and its
two measured edges, so the red node is explained where it is seen.
