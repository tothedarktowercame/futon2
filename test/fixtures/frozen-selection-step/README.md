# First repeatable section: evidence → selected executable

This fixture comes from real run
`2026-10-21-752b48a7-6d82-4a0e-b888-9966e8c46116` (2026-10-21).
The complete original run ended incomplete. Its successful selection section
is the starting boundary here, not evidence of a completed task.

The compressed EDN contains the full six-entry menu and policy evidence,
recorded tie draw, original selected action, and expected scores for every
policy. It requires no access to the original run file or current mission
files. Its `:source` retains the original run ID and SHA-256 of the captured
run text.

From the futon2 checkout root:

```sh
clojure -M scripts/wm_frozen_selection_step.clj replay test/fixtures/frozen-selection-step/752b48a7.edn.gz /tmp/wm-selection-step-1
clojure -M scripts/wm_frozen_selection_step.clj replay test/fixtures/frozen-selection-step/752b48a7.edn.gz /tmp/wm-selection-step-2
```

Each command starts a fresh JVM, calls the production
`outer-policy-objective/evaluate`, seals the resulting choice, and calls
`selected-outer-cascade-policy/decision`. Success requires:

- a selected action, never abstention or partial selection;
- exact equality with all six historical score/posterior rows;
- the original selected target, policy and canonical cascade digest;
- a verified production handoff into the downstream decision shape.

The command writes `result.edn` and `selected-boundary.edn.gz`. Compare the
`:boundary-sha256` values to establish repeatability. The boundary file carries
the full selected-policy carrier and projected decision for the next section.
The result names the code revision and script hash. It says
`:task-executed false`: this procedure executes selection and handoff, not the
selected mission or an author/reviewer dispatch.

## Explicit offline import

The historical menu seal does not verify on the deserialized menu under the
code used for this exercise. The original evidence verifies and its menu
reference agrees with the recorded menu reference. The tool preserves the
original seal and failure under `:import`, and issues a NEW offline menu seal
and corresponding evidence pin over the imported values. No menu entry,
scoring input, or recorded draw is changed. This is a value replay with
explicit import provenance; it is **not** a successful validation of the
historical menu seal. Production verification is not modified or disabled.

An independently reviewed portability fix for historical sealing is still
needed before claiming an authenticated historical replay. Do not send this
offline boundary directly to a live runner as if it carried live authority.

## Extending this procedure with Zai

The next packet should take `selected-boundary.edn.gz`, load the same selected
cascade in a fresh process, and produce its concrete operational construction
using the existing production functions. Freeze the required readings and
reviewed authoring inputs first. The packet is complete when it produces a
verified construction for this task; an abstention leaves it unfinished.
Then carry that construction forward to bounded execution and verification.

Keep the selected identity fixed. Give one agent one section, the input path,
one command, and the useful output required by the next section. Review the
output by rerunning the command. Advance the boundary only after success.

To regenerate the input from the original record, when available:

```sh
clojure -M scripts/wm_frozen_selection_step.clj freeze data/wm-runs/tick-run-record-2026-10-21-752b48a7-6d82-4a0e-b888-9966e8c46116.edn /tmp/752b48a7.edn.gz
```

Retained run evidence is under
`holes/labs/wm-contract/runs/frozen-selection-step-2026-10-22/`.
