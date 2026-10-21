# Blueprint 1: from the ledger read to a `:cascade-ready` result (phases 0a-14.1)

Part of [00-TARGET](00-TARGET.md). Status: passed live by child 7 (run
`752b48a7`), for the one policy it selected.

## Leaves

### 1.1 The queued child runs as ordinary work (phases 0a-0c)

Claim: with `d755c886` `:enacted` and a requeue entry present, readiness
arbitration hands the click to the ordinary route, and initialization
completes.

Witness: every child since ordinal 16 ran as ordinary work. Child 2 stopped in
initialization (`75747f06`, `:required-checkpoints-missing`); children 3-7 got
past it.

### 1.2 The outer selector picks a reading policy (phases 8-13)

Claim: the sealed outer selector returns a carrier, and `cascade-decision`
projects it with `selected-outer-policy/decision` (`wm/cascade_decision.clj:1332-1341`).
On this route `decision-gate/emit!` does not run, and a reading policy is not
expected to carry `:interpretation-receipts` (DISCOVERY-9c998f2e.md, section E).

Witness: child 7 selected `M-pattern-posteriors/policy-2f5c7f6d3345d326aae3`,
a one-node cascade, digest `9c2f7532…`. Child 1 stopped here
(`821c15d0`, `:no-selection`: the selection abstained).

### 1.3 Cascade execution returns `:cascade-ready` (phases 14, 14.1)

Claim: for a co-apply reading policy,
`wm-cascade-decision/execute-selected-cascade!` validates, persists and
compiles the cascade and returns `:cascade-ready` (`wm/cascade_decision.clj:1757-1763`).
A held result goes to the open-cascade resume (14.1).

Witness: child 7, no resume needed. Children 3, 4 and 6 stopped in the resume
(`42fd2c21`); child 5 stopped in execution (`a173047e`).

## What this blueprint does not cover

Which policy a child selects is not fixed. Child 7's policy compiled without
a resume. A policy with open holes goes through 14.1, where `42fd2c21` stopped
three children. The ledger has no transition for `42fd2c21` after ordinal 32,
so that injury is still open. A child that selects such a policy can stop
there again whatever happens in blueprints 2-4.

Check before the click: read the current outer menu, and see whether the
policy the selector would rank first needs the resume. That is a read; it
spends nothing.
