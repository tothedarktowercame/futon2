# C334 — event-claim consumer audit

Date: 2026-09-01

All six event declarations were traced through their consumers. Five already
preserved their interval semantics; the workspace gate collapsed its
constituent's fence condition, and the lane/Agency boundary did not expose its
known instantaneous-snapshot limitation. Both gaps are now explicit.

| Event-shaped program | Consumer | Result |
|---|---|---|
| `contract_authority_current.clj` | `wm_workspace_gate.clj` | C326 made the constituent fence-conditional. C334 makes the gate summary itself carry the fence ID, gate observation interval, `:event-free?`, and `:verdict-qualification`; the condition now survives into the bounded receipt. |
| `lane_registry_check.clj` | `quiescence_check.py`, then `writer_fence_evidence.py` | Sequential Agency reads now emit `:instantaneous? :unverified` with reason `:agency-revision-token-absent`. Quiescence preserves that qualification. Its outer stable sandwich and writer-fence attestations do not pretend to supply the missing service revision token. |
| `quiescence_check.py` | writer-fence evidence and the drain procedure | Already honest: two complete snapshots, movement/unavailability exit 3, dirty state exit 1. `QUIESCENT` is explicitly an interval result; its conditions now also name Agency instantaneous state as unverified. |
| `wm_click_resource_observer.clj` | operational certificate | Already honest: receipt has start/finish, before/after counters, observation scope, and unavailable/dirty states. Certificate validation checks the run lies inside that envelope and keeps resource and execution status separate. |
| `wm_workspace_gate.clj` | bounded receipt, `run-readiness`, and operator | The four-repository before/after basis was already preserved. C334 adds the missing upward event claim: unfenced stable output is content-only/event-free-unverified; a named fence is `:fence-conditional`; movement is false and `:repository-basis-moved`. |
| `writer_fence_evidence.py` | C319 operator procedure | Already honest: two observations plus attestations; breach, indeterminate movement, and verifiable fence are distinct. Cross-authority ABA remains explicitly under `unverifiable`. |

## Controls

The mutable-verdict claims checker now has three independent mutation modes:

- `missing` removes a member declaration;
- `stale` adds a declaration outside the generated population;
- `overlap` assigns one member to two classes.

All three are rejected. A synthetic gate claim also proves the upward
vocabulary: a stable named fence yields `:event-free? true`; an unfenced stable
interval yields `:unverified`; repository movement yields `false` even when a
fence was declared.

## C327 replacement wording

During verification a concurrent delivery added
`checks/positive_proof_receipt.clj`. The population guard failed immediately
with `:undeclared-member`; inspection classified it as a content-shaped
source/fixture/toolchain proof receipt, and it was added explicitly. Population
v1 therefore now has **69 members: 62 content, six event, one neither**. This is
the guard detecting real drift, not a changed criterion.

> Mutable-verdict population v1 is complete and guarded: 62 named content
> claims, six named interval/event claims, and one non-verdict library. The
> receipt preserves the workspace gate's repository interval and named-fence
> condition. Quiescence and writer-fence evidence remain interval-scoped;
> Agency multi-job instantaneous consistency remains unverified until the
> service-issued revision token designed in C301 is deployed.
