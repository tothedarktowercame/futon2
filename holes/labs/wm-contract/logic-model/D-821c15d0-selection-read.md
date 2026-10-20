# D-821c15d0 — retry child selected no addressable action

Read at futon2 `main` on 2026-10-20. This is a source-and-record discovery;
it does not claim that another click was run.

## 1. Why `:selection-is-not-outcome` occurred

The child run `2026-10-20-881a8839-d725-493e-95cf-eb06dd71b92c` records, verbatim,
`:selection-event {:event :selection-abstained :target nil :cascade-id nil :policy-key nil
:reinforcement :none :reason :selection-is-not-outcome}` and
`:failure {:kind :no-selection :stage :selection-world ...}`.  The same record's
world census records 166 missions, 449 excursions, and 155 tickets; those are
available inputs, not a proof that the selected decision had an addressable action.

`cascade_habit_reinforcement.clj:20-27` emits `:selection-abstained` whenever
the supplied decision has no `:action`, and unconditionally labels that event
`:selection-is-not-outcome`.  `full_loop_runner.clj:2450-2467` then sees a nil
selected entry and closes with `:failure-kind :no-selection` unless the decision
status itself is `:abstained`.  Thus the reason means *the decision carrier lacked
an action*, not that the mission population was empty.

Whether a second child would do this identically is **not established**.  The
record's per-run `:world-at-selection :selection-input-digest` is
`"9bafb95f16cf71ebda529fb4ff34d981f9799eeaa55dd5cb580a34ca9e83fff8"`, but
the selection computation also has per-run inputs/timing and no record proves
determinism from that digest alone.

## 2. Why the terminal kind differs from 2026-10-19

The child decision is not `:abstained`; its terminal record says `:failure-kind
:no-selection`.  `full_loop_runner.clj:2420-2422` makes that exact distinction:
only `(= :abstained (get-in judgement [:decision :status]))` yields `:abstained`;
every other nil-entry case yields `:no-selection`.  `selection-terminal-condition`
uses the same test at `full_loop_runner.clj:2450-2459`.

The cited 2026-10-19 records do not establish the proposed comparison: the
`be38a3a9` record inspected here carries `:failure {:kind :carry-identity-unavailable
:stage :author-dispatch ...}` and the `bd1f106d` record carries `:failure {:kind
:fold-output-invalid :stage :construction ...}`.  They do not supply a
comparable `[:decision :status] :abstained` field in the quoted terminal slice.
Therefore the claimed historical contrast is **cannot tell from these two cited
records**, rather than evidence that the repair regressed it.

The repair patch `b463d85cc` changed `wm/loop_node_exercise.clj:16-55` to fall
back from certificate fields to retained decision fields, and
`wm/terminal_receipt.clj:104-119` to type missing click/outcome values.  It did
not change `full_loop_runner.clj`'s `:abstained`/`:no-selection` branch.  So the
answer to “is 821c15d0 the repair's own regression?” is **no evidence of that
from the diff; the deciding branch was untouched**.

## 3. Can a re-queued child reach the loop node unchanged?

Not reliably.  The child terminated at selection-world before loop-node exercise,
because its nil action selected the existing `:no-selection` terminal path.
Re-queueing without changing the decision/carrier conditions has no recorded
evidence that it can reach the loop node.

Smallest proposal only: in `src/futon2/aif/full_loop_runner.clj`, preserve and
classify the nil-action decision as a typed, retry-inconclusive selection result
instead of opening a new injury as `:no-selection`; this does not create an
action, so a separate selection-carrier repair is still required before a retry
can exercise the loop node.
