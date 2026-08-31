# C148 — `wmRunConformsToWiring` evidence shape

Date: 2026-08-31

The evidence is serializable: the binding points to the pinned EDN
`tick-run-record-2026-08-30.edn`, whose `:route` is the observed WM-RUN2
traversal.  `wm_route_conformance.clj` separately checks the live invariant that
the nonempty route's hops are present in the original or measured Figure 4
layers.  C97's operational certificate consumes the same receipt bytes plus a
resource receipt and topology.

`contract_lint` now maps evidence type `TickRunRecord` to its receipt-shape
predicate.  The predicate requires the recorded timestamps and selector seam,
natural/count constraints, sample-within-limit, five preference layers, a
written trace, and a nonempty route whose every hop has string `fromNode`,
`toNode`, `via`, and `at_` fields.  This is shape validation; map conformance
remains the named live invariant rather than being duplicated inside the lint.

The semantic negative control removes `toNode` from a route hop while retaining
valid EDN and all other receipt fields.  `contract_lint_test` requires the
`TickRunRecord` shape checker to reject it.  The same test also retains the
empty-route falsifier.  Thus the control breaks the evidence interface rather
than merely breaking the parser.

The live strict report now classifies `wmRunConformsToWiring` as `:conformant`.
The status category `binding-passed-shape-unchecked` therefore reaches zero;
there is no permanent uninspectable exception to record.

