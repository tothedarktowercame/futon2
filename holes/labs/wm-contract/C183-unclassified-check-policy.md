# C183 — unclassified checks remain loud

Date: 2026-08-31.

Decision: do not accept an unknown check merely because a lane is active, even
when the inventory filename is exact and every already-known check passes.

An active holding does not establish that the file belongs to that lane, that
its semantics were reviewed, or whether it belongs in the positive gate,
controls, or a reasoned manual boundary. More importantly, “all other checks
pass” excludes the unknown check by definition. Returning green would make the
workspace gate knowingly claim completeness while omitting an executable.

The red has one short action: classify the named file. Add its positive and
semantic-control invocations, or record why it is manual. Once that committed
classification lands, inventory clears. No status acceptance is created.

C179 demonstrated the distinction. `machine_vocabulary_witness.clj` was added
to the known filename set, but that alone did not execute its four terms. C183
wires `control`, `aliveness`, `act-gate`, and `cohort` in both positive and
negative-control modes. Classification is an execution disposition, not merely
removal from the unknown list.

A future transient classification would need typed lane outputs naming the
exact path and intended disposition before file creation. The current lane
registry has no such field. “A lane is active” is too weak and is nearly always
true, so it would turn a narrow-looking exception into a permanent bypass.
