# TN row 15: reason-bearing live selector specification

Read-only discovery at futon2 `d9fddee2` and the sibling futon3c source on
2026-09-12. This note specifies a future extension; it changes no production or
Lean source.

## 1. The law that actually selected the pinned action

The on-demand runner resolves
`futon3c.peripheral.live-wm-selection/validated-selection`, marking the seam
`"live"` (`scripts/futon2/run_tick_once.clj:18-20,102-109`). If resolution
fails it substitutes the explicitly labelled controller-head diagnostic stub,
not a second unnamed live law (`run_tick_once.clj:111-150`). The chosen selector
is passed as the judge's required `:strategic-selection-fn`
(`run_tick_once.clj:246-276`) and the tick calls the complete judge with it
(`run_tick_once.clj:278-298`). The full-loop runner uses the same request shape
and either an injected selector or its bounded HTTP bridge
(`src/futon2/aif/full_loop_runner.clj:3031-3047`).

War Machine invokes that function with two rankings and a trace id:
`:scheduler-habit-ranking`, restricted locally to three named strategic
missions; the controller decision's complete `:controller-ranking`; and
`:trace-id` (`scripts/futon2/report/war_machine.clj:6558-6575`). A missing
function refuses rather than defaulting (`war_machine.clj:5898-5906`). The live
implementation validates only the scheduler-habit ranking: it must be a
nonempty vector of at most three nonblank mission ids, all in the Phase-1--4
allow-list (`futon3c/src/futon3c/peripheral/live_wm_selection.clj:356-383`).
`current-selection` does not consume the passed controller ranking. It combines
the scheduler-habit counterfactual with frozen Phase-5/6/7/rung-2 inputs and the
shared evidence store (`live_wm_selection.clj:326-354`).

The successful selection law is deterministic:

1. construct admitted strategic policies from ready Phase-5 frontier missions
   and ready short cascades; policy identity is the hash of the ordered mission
   vector (`strategic_policies.clj:29-39,41-95`);
2. fit `E_S`, join exactly one predicted `G_S` row to every policy, and compute
   `log E_S - G_S / temperature` (`strategic_policies.clj:210-239`);
3. normalize by stable exponentiation and sort descending shadow probability,
   breaking equal probability by ascending `:policy-id`
   (`strategic_policies.clj:240-269`);
4. from the shadow trace named by `strategic-decision-id`, choose the first
   ranked policy (`live_wm_selection.clj:127-150`).

Every comparison ranking must be a permutation of the Phase-5 candidate set;
otherwise selection refuses (`live_wm_selection.clj:93-99,124-145`). A success
must have a selected policy with a complete explanation and nonempty memory
support (`live_wm_selection.clj:146-150`). Authorization additionally proves
the selected mission set is nonempty and a subset of the candidate domain and
that the serving-cache latency gate passed (`live_wm_selection.clj:275-324`).
Thus a successful call always selects one ranked, supported strategic policy;
it does not return an abstain value. Invalid, empty, unsupported, incomplete,
or unready cases throw. It is read-only and does not execute a click
(`live_wm_selection.clj:326-332`).

War Machine maps the selected mission ids back into its admissible action list
and refuses if no actionable member or authorization is present
(`war_machine.clj:6577-6599`). It replaces the controller decision's action,
marks the boundary `:reason-bearing-strategic-policy`, and retains selected
identity, authorization and a strategic-memory summary
(`war_machine.clj:6600-6641`). `strategic-selection-law` separately joins the
chosen durable action identity to controller rank, which produced the pinned
`:consulted-ranking :live-selector-id`, head rank 1, chosen rank 139 and moved
true (`war_machine.clj:5908-5931`).

## 2. What schema 28 retains and what it cannot reconstruct

`strip-decision` removes only the non-stringable softmax map and embedded
ranked list, so the remaining live-selector summary survives in `:decision`
(`src/futon2/aif/trace.clj:224-236,644-646`). Today that includes:

- the selected action, selected policy id and mission ids;
- boundary, requested/applied controller law, controller-head and chosen ranks,
  movement flag and consulted-ranking tag;
- authorization status; and
- selected policy `E_S`, selected `G_S`, hard support and provenance, plus
  relation contributions, path diversity, budget, blockers, calibration,
  serving-cache evidence, three counterfactual rankings and actuation details
  (`war_machine.clj:6600-6641`).

That is enough to retain the observed choice and the already-admitted
divergence, but not enough to replay the live selector. The persisted record
omits the complete candidate domain, the ordered policies and mission vectors,
every losing policy's `E_S`, `G_S`, log potential and shadow probability, the
temperature, the policy-id tie-break inputs, the fixed/typed/outcome-conditioned
rankings, and the frozen Phase-7 decision/fixture identity. Those values exist
before projection (`live_wm_selection.clj:124-140,151-212` and
`strategic_policies.clj:287-328`) but are discarded when War Machine constructs
`:strategic-memory`. The controller ranking is retained, but it is not an input
to this law. Therefore the pinned schema-27 record cannot support a positive
theorem that rank 139 was the declared live-selector output; it supports only
the divergence already admitted.

## 3. Frozen-file-compatible Lean extension

Create a new `DarkTower/WarMachine/ReasonBearingSelector.lean` importing frozen
`MachineAction.lean`; do not edit `MachineAction.lean` or its witness. The new
module should define:

- `ReasonBearingPolicy`: stable policy id, ordered mission ids, retained
  log-habit value, predicted strategic cost, and admissibility/support witness;
- `ReasonBearingInput`: nonempty candidate domain, temperature, complete
  policy table and its exact support/order contract;
- `ReasonBearingBoundary`, with `machineActionBoundary` and
  `reasonBearingStrategicPolicy`; and
- `reasonBearingAction`, whose new branch computes
  `ln E_S - G_S / temperature`, chooses the maximum, and resolves ties by the
  lexicographically least stable policy id. Its output is an option only to
  type the refusal boundary; well-formed-input theorems prove it is `some` and
  belongs to the declared candidate domain.

The compatibility theorem must quantify over the full old argument list and
state that `reasonBearingAction .machineActionBoundary ...` equals frozen
`MachineAction.machineAction ...`. A second theorem states the positive witness
shape: for a pinned complete `ReasonBearingInput`, the retained selected policy
and selected mission/action equal the extension's output. Supporting theorems
must prove deterministic tie behavior, output membership, and refusal on empty,
incomplete or support-mismatched inputs.

The admitted negative divergence remains valid: it says the 17:28 live result
is not an instance of the old controller-head branch. The extension gives that
different boundary a truthful name; it does not rewrite history or assert the
old branch was wrong. No live wiring belongs in the Lean packet.

## 4. Packet split

1. **Producer retention envelope, one futon3c behavior.** Add a compact
   `:selection-proof-input` to successful `validated-selection` results,
   containing algorithm/revision, frozen decision identity, temperature,
   complete ordered policy rows (`policy-id`, mission ids, `E_S`, `G_S`, log
   potential, shadow probability, support/provenance), candidate domain,
   tie-break rule, and selected policy id. Refuse construction if coverage or
   order is incomplete. Witness: replay the production selector and reconstruct
   its selected id exactly; missing/extra policy and order mutation refuse.

2. **Trace retention, one futon2 behavior.** Pass that envelope unchanged
   through `war_machine` into the decision and validate it before append;
   `strip-decision` already preserves present fields. A redirected full-judge
   write/read must equal the selector output byte-for-byte. Dropped half,
   candidate/policy support mismatch and selected-id-not-in-table each refuse
   before append. This is additive and needs the schema ledger treatment current
   at implementation time.

3. **Lean extension and compatibility.** Add only
   `ReasonBearingSelector.lean` with the types, law, compatibility equation,
   determinism, membership and refusal theorems above. Existing frozen modules
   and admitted divergence remain unchanged.

4. **Positive post-capture witness.** Pin the first production record carrying
   the complete envelope, independently recompute every policy score/rank and
   the selected id, and instantiate the new Lean law. The claim is scoped to
   that record and boundary. It must retain float residuals honestly and keep
   any downstream enactment claim out of scope.

No registry edit, live wiring, admission promotion, or modification to the
existing divergence claim is part of any discovery work here.
