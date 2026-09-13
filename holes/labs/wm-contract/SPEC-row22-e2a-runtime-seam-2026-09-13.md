# Row 22 E2a prospective runtime seam

Status: pure construction only. This packet does not wire the live selector.

The future integration point is immediately after the independently configured
E1 authority resolver has reconstructed the complete same-tick R6 support and
canonical R11 arbitration, and before any R6 scoring/posterior or
`machineAction` call consumes its candidate domain. The caller passes only the
E1 resolver configuration to `machine-portfolio-restriction/restrict-portfolio`.
It must not pass a selected-id set, accounting rows, actions, identity, or scope.

The result's `:approved-support` becomes the exact ordered candidate domain for
the downstream R6 scorer. Every occurrence retains its original
`:candidate/id`, `:rank`, and `:action` bytes. Equal semantic actions at distinct
occurrences remain distinct. `:excluded-support` remains in the trace so the
budget disposition is reviewable. Empty approval refuses rather than falling
back to the full support.

Runtime integration must retain in one tick record: the five E1 source pins,
full support, R11 request/response, E1 accounting, approved/excluded supports,
R6 scores and posterior over exactly the approved occurrence IDs, and the later
selected identity. It must refuse if scoring introduces, drops, deduplicates or
reorders an approved occurrence. This preserves the remaining R6 obligations;
E2a authorizes neither an action choice nor enactment. E2b separately joins the
eventual selected occurrence to the approved portfolio and exact enacted action.

Current production authority remains unavailable at E1, so this seam cannot be
activated by this packet. The retained E2a fixture is isolated mechanism
evidence, not an edge firing or production feasibility claim.
