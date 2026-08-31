# C181 — keep the lane-staleness acceptance at one

Date: 2026-08-31.

Decision: retain the exact `lane-registry` acceptance for one
`:stale-holding`. Do not raise it to match the commissioner's observed habit of
letting two or three completed lanes wait during a delivery round.

The boundary is a service level for dispatcher attention, not a statistical
description of recent workflow. One completed lane awaiting record/redispatch
is the explicitly tolerated handoff interval. A second means useful capacity
is being left idle and the dispatcher has accumulated a queue. That is the
condition C75 was built to expose. Raising the number after repeated lapses
would make acceptance growth normalize the finding it is supposed to keep
visible.

Therefore:

- exactly one `:stale-holding` remains `DEGRADED-AS-EXPECTED` while its
  acceptance is live;
- two or more do not match the exact signature and become `DEGRADED-NEW`;
- the response is to record or redispatch completed lanes, not edit the
  accepted count;
- this red describes dispatcher throughput, not source invalidity, so it does
  not enter the repository workspace gate.

This is intentionally different from tuning a capacity limit from measured
demand. The dispatcher controls this queue directly; accommodating a larger
queue would remove the feedback that Joe asked the lane registry to provide.
