# C298 — classify five workspace-gate discoveries

Date: 2026-09-01. Owner: `wm-organization`.

The five files were known implementations accidentally omitted from the
separately maintained `known-check-files` inventory. None was renamed, moved,
or semantically unknown.

| File | Introduced | Classification |
|---|---|---|
| `parameter_prior_kernel_witness.clj` | `8fb951e`, 00:06:11Z | Gate positive plus two semantic controls; gate-executed. |
| `wm_click_resource_observer.clj` | `088f7ee`, 00:07:09Z | Deliberately manual Joe-only production-click command; its injected rehearsal and argument refusal are tested elsewhere. It must never be auto-executed by the gate. |
| `parameter_posterior_kernel_witness.clj` | `d2b43fb`, 00:13:17Z | Gate positive plus two semantic controls; gate-executed. |
| `model_reduction_free_energy_change_witness.clj` | `ab33ba4`, 00:21:50Z | Gate positive plus value/type controls; gate-executed. |
| `observation_vector_witness.clj` | `2972f9f`, 00:28:02Z | Gate positive plus partial/outcome controls; gate-executed. |

Inventory completeness first stopped at `8fb951e` (00:06:11Z). That commit
added the parameter-prior file and its gate commands, but not its inventory
entry. Each later addition repeated the split update. Earlier reports of
`{:unknown ()}` describe gates before that commit; they do not cover this
five-file interval.

The four witness files are now inventory-classified by their already-present
positive and falsifier commands. The observer is an explicit manual exclusion
with reason `:joe-only-command-that-performs-production-click`; merely invoking
it would mutate production, so manual is substantive rather than a convenience.

The runbook now makes inventory classification a same-delivery closure step
and gives a focused invocation. The inventory remains loud discovery: adding a
file without this step still makes the gate red.

During the focused check, C297 concurrently added a sixth discovery,
`mutable_read_set.clj` (`0c88124`, 00:47:29Z). Inspection established that it
is a support namespace rather than a standalone command: its real-temp-file
movement control lives in `mutable-read-set-test`, and the gate exercises its
two consumers (`r17_generator_disposer_check` and `wm_route_conformance`) and
their controls. It is therefore classified as the explicit manual/non-command
entry `:mutable-read-set-library`, not silently appended as a gate executable.
