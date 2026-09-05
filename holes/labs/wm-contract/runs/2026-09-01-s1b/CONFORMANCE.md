# RUN3 — conformance of S1b against the drawn topology

`bb holes/labs/wm-contract/run3_conformance.bb holes/labs/wm-contract/runs/2026-09-01-s1b/wm-trace-s1b.edn`

**CONFORMANT.** 20 records, 20 routes, 180 hops, 9 distinct. Exit 0.

Every hop is an edge of the checked topology, and no `:code`-retired route-grain
edge was traversed. All twenty ticks took the same route, which is itself worth
saying: the run exercises one path twenty times, not twenty paths.

| class | count | hops |
|---|---|---|
| refutation | 0 | — |
| ruling-unrealised | 1 | `R5→R6` |
| excluded, dependency-grain | 1 | `R2→R7` |
| drawn | 2 | `R7→R3`, `R8→R5` |
| route-measured | 5 | `R20→R12`, `R12→R2`, `R3→R8`, `R6→R14`, `R14→TRACE` |
| unmapped | 0 | — |

**The two retired edges are not refutations, and they fail to be for different
reasons.** `R2→R7` was retired on a *dependency* claim while the same pair is
drawn as a measured route; a route is a sequence of tag stamps, not the
dependency DAG, so it cannot refute a dependency claim — excluded and reported
as excluded. `R5→R6` was retired by an operator ruling that no code was changed
to match, so the traversal says **the ruling is unrealised** and opens a build
row (`I4`), not that the figure is wrong.

**Correction (2026-09-05, I4/C514).** The `R5→R6` route hop does not show
that the `:r6-r14-order` ruling was unrealised. The hop's `R6` tag names
`futon2.aif.policy/select-action` (the ranking-to-selection relation; current
`scripts/futon2/report/war_machine.clj:6450-6470`), whereas the retired
`[:R5 :R6]` edge names catalogue R6-C, the candidate action space. That
candidate space is constructed before R5 and is not route-tagged. RUN3's
classifier at `run3_conformance.bb:116-124` applies its grain proxy only
when a `:code` retirement is also in `:route-measured-drawn`; it cannot
distinguish R6-C from the Q(π)/selection R6 relation for this
`:ruling` retirement. Therefore `:ruling-unrealised [["R5" "R6"]]` is a
mechanically reproduced historical field but is not evidence that the
retired catalogue relation ran. The still-unrealised clause—construct the
cascade for the committed target—has no route signature and was not exercised
by this run because advisory lanes were disabled. See
`C475-cascade-order-disposition.md:54-96`.

**19 of 22 drawn edges never fired.** That is not a conformance failure — the
figure draws the apparatus, the run exercises one tick's path through it — but
it bounds what this run can validate. Learning (`R1→R4`, `R3→R1`), actuation
(`R9→R16`, `R15→R16`, `R16→R2`) and the retired-but-still-drawn pairs are all
untouched by a shadow diagnostic tick.

**Also confirmed here**, since S1b did not check it: `:f-pi-by-candidate-id` is
present on 20/20 records with `:status :present` on 20/20 — the first tick had a
predecessor to score against because the aborted run left one, so this is not
evidence that a cold start is handled; the earlier S1 showed that (`:absent` at
tick 1) and it is the correct behaviour.

## Controls

`bb holes/labs/wm-contract/run3_conformance_controls.bb` plants one defect each
into a copy of the real S1b route; all three are refused, exit 1:

1. a `:code`-retired **route-grain** edge (`R7→R14`) → **1 refutation**;
2. an **unknown pair** (`R4→R20`) → unmapped;
3. a drawn edge traversed in **reverse** (`R3→R7`, where only `R7→R3` is drawn)
   → unmapped. Direction is checked, not just adjacency.

A fourth control was specified and is **not implemented**: a legitimate edge
traversed *out of order*. The drawing gives no total order over edges, so there
is nothing to be out of order against — the check would need an expected
sequence the registry does not carry. That absence is the finding, not an
omission: "every hop is an edge" and "the hops form the route we drew" are
different claims, and only the first is currently checkable.
