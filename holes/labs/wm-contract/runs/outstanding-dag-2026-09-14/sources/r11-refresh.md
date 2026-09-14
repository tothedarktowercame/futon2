# R11 census refresh discovery — 2026-09-13

## Finding

The published R11 `0/7` is **not a ratio of arbitrations to seven run
opportunities**.  Its denominator is the seven fixed process-lifecycle columns
`commissioned, dispatched, parked, returned, checked, recorded, surfaced` in
`ALIGN-rnode-process-census.md:212-223`.  The R11 row contains seven literal
`absent [A11]` cells at line 222.  The meaning of `absent` is fixed at lines
8-12, and the node-specific evidence at lines 232-249 calls the result an
“obligated lifecycle ladder” of 0/7.

This is the typed-absence row referenced by mandatory negative-scope inventory
item 16: `TN-row24-scoping-2026-09-12.md:195-219`, specifically line 218.
That item combines R11 and R15 lifecycle validation; it is not a count of WM
ticks or cohort attempts.

## How the row was produced

The discovery and source/corpus census was
`v7_r11_node_sim.clj`.  Its counting domains are explicit:

- code files beneath the Futon2 checkout are enumerated at lines 27-56;
- the retained runtime corpus is only `data/wm-trace/wm-trace-YYYY-MM-DD.edn`
  at lines 97-118;
- R11 route and output-marker predicates are at lines 124-135;
- the requirer result is assembled at lines 231-240.

Its retained output is `runs/V7-R11-node-sim/00-r11.edn`.  The reviewed summary
in `ALIGN-rnode-process-census.md:237-245` records 631 code files, zero
production callers, and zero matching records among 892 records in 58 WM trace
files.  That corpus finding justified typing each of the seven lifecycle cells
`absent`; it did not define seven arbitration opportunities.

The matrix row itself was committed by futon2 `59941798` and rendered by the
existing p4ng dossier consumer.  The retained execution receipt is
`runs/row-20-r11-census-2026-09-12/execution-receipts.edn`: its canonical render
observed `r11-strip "-------"`; its schema control observed seven `"absent"`
cells.  The parser reads the matrix at
`p4ng/empirics-futon/gen_rnode_dossiers.py:249-275`, requires all seven cells
to be `exists` for plumbing at lines 477-486, and renders the typed glyph strip
at lines 503-511.  It does not inspect run records or count opportunities.

## Current evidence period inspected read-only

The requested period exists: cohort 47 has attempt directories 001-003,
cohort 48 has 001-002, and the earlier r1 click supplies the sixth on-demand
machinery run.  The corresponding 2026-09-13 click-binding directory contains
six records.  Those files are a new evidence population, but neither the V7
producer nor the row-20 renderer consumes cohort directories or click-binding
records.

Changing `v7_r11_node_sim.clj` to read those two new stores would create a new
counting method and a new opportunity definition.  Treating the six runs as
additional denominator units would also mix run opportunities with lifecycle
cells.  Both violate the instruction to use the original counting rule.

## Disposition

Refresh stopped at discovery.  The old published result remains seven typed
absence cells, conventionally written `0/7`; no defensible `0/N` or `k/N` run
count exists under the original procedure.  No census artifact, producer,
matrix, generated publication, serving JVM, cohort record, or click record was
changed.  A future refresh requires an explicit new contract that maps retained
cohort/click evidence to each of the seven lifecycle cells; it cannot be
obtained by appending six runs to the denominator.
