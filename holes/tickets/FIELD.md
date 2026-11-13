# WM ladder field policy

Authorized by Joe, 2026-11-11.

The field advances by committed ticket status, one checkpoint per closed
ticket:

1. At checkpoint zero only `T-ladder-01-loop-turns-over` is live.
2. After ticket 1 is accepted and committed `DONE`, expose ticket 2 only.
3. Repeat sequentially through ticket 5.
4. After ticket 5 is accepted and committed `DONE`, expose tickets 6 through
   10 together so the War Machine chooses among them.
5. Tickets 11 and 12 remain operator-gated for this experiment.

Before retrying a ticket after an apparatus repair, restore experiment state
to the checkpoint following the last accepted ticket (or checkpoint zero),
then apply the separately committed apparatus repair. Partial work and run
artifacts from the failed attempt are not retry inputs.

Every live ticket already projects the War Machine's ticket-closure want from
its committed `HEAD`: the observable closure is the exact line
`**Status:** DONE`. No parallel hand-written want declaration is used.
