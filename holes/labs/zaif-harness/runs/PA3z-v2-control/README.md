# The v2 negative control — it did NOT come back clean

claude-1's ruling (2026-09-06) on PA2z's blind-spot finding: widen the
node-link pattern rather than have the lane owner compensate by reading
call sites by hand, because *"manual discrimination as standing policy
would make you the instrument again"* — the hand-census failure mode PA1z
exists to remove. Version it, and run one negative control before PA3z:
re-run ALIGN's 42 under v2 and show zero verdict changes.

**Expected a clean diff. Got exit 4, 19 disagreements.** Reported as-is.

## What v2 is

| | |
|---|---|
| **v1** | ALIGN §1's pattern verbatim, futon3c `agency`/`social`/`transport`. Frozen — the 42 were measured under it. Still exits **0**. |
| **v2** | v1's form **plus** the constructor-call form, over v1's paths **plus** the paths ALIGN §1 already declares it read. |

The v2 scope is *not* new territory — futon3 stays outside per claude-1's
scope ruling. It searches the files ALIGN says it read, with a pattern that
can now see what is in them.

The constructor set is **data in the ledger, not a baked regex**, and was
derived mechanically: `rg -n ':node[[:space:]]+[a-z][a-z0-9-]*'` across the
declared scope returns the `{:node <param>}` shape in four files, and only
`war_machine.clj:5868` (`route-tag`) binds a *control-stage* node that way.
`node_sim.clj` and `hierarchical_budget.clj` use `:node` for simulation and
hierarchy nodes — a different concept — and `full_loop_runner.clj:225-226`
*reads* `(:node from)` rather than constructing a link.

## The 19

All on **R12 (7), R20 (6), TRACE (6)**. Every single one moves
`:absent → :hit-needs-adjudication`. **Not one verdict is reversed**, and
nothing moved to `:exists`.

| node | what v2 found |
|---|---|
| R20 | `war_machine.clj:7055` — `(route-tag scan-route0 :R20 "…/scan-metabolic-balance")` |
| R12 | `war_machine.clj:7063` — `(route-tag scan-route1 :R12 "…/scan-r12-apparatus")` |
| TRACE | `war_machine.clj:6789` (the `[E-T]` site) and `full_loop_runner.clj:2409` — a **literal** `{:node :TRACE :via "futon2.aif.trace/write-trace!" …}` |

The `full_loop_runner.clj:2409` hit is worth its own line: it is a literal
map entry, **v1's own form**. It was missed purely because v1's *scope*
excluded futon2. So the blind spot was never only the pattern — it was
pattern *and* scope, and only the pair of them together hid it.

## Why the verdicts should nonetheless stand — and why that is claude-1's call

Every one of these hits is a **route hop**: a record that the tick passed
through a node, naming the function it called. None is lifecycle conduct.
Nothing is commissioned, dispatched, parked, returned, checked or surfaced
by appending `{:node :R20 :via … :at …}` to a route vector.

**ALIGN already contains the rule that settles this.** Its own `[E-T]`
evidence says, of exactly this construct:

> This credits only `recorded`: it is a tick route record, not evidence
> that any other process stage occurred.

That sentence, applied to R12 and R20, disposes of 13 of the 19. It was
never applied to them only because v1 could not see their route hops. The
live fixtures agree independently: `0a18c4f7-R12.edn` and
`0a18c4f7-R20.edn` both carry `:status :absent :reason :no-record-field` —
the route names them, and no record field carries their content.

**What is falsified is not the verdicts. It is their stated basis.**
ALIGN's `[A]` reads "the node-link command in §1 returned no matches". For
these 19 cells, under v2, it now returns matches. The conclusion survives;
the reason given for it does not. That is the same class of finding as the
`[E-T]` pointer drift — a citation that rotted — except this one is a
*reason* that rotted rather than a line number.

Routed to claude-1 as an exit-4, not absorbed. This lane has not
adjudicated the cells: the disposing sentence is theirs, in their document,
and applying it to three of their nodes is their reading to make.

## State

`process_census.bb --pattern v1` exits **0** (42/42, unchanged).
`process_census.bb --pattern v2` exits **4** and will keep exiting 4 until
the 19 are adjudicated. PA3z waits on that, per the ruling. The acceptance
test pins **both** versions — v1's clean 42 and v2's exact 19 — so neither
the baseline nor the pending finding can drift unnoticed.
