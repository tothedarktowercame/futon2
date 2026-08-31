# C69 — measured routes in Figure 4

Recorded 2026-08-31 by `wm-verbs`.

Figure 4 now displays the eight routes in the separately counted
`:route-measured-drawn` population. A rust-coloured dash-dot stroke and the
legend text `measured route` distinguish them from the original grey control
edges and light support relationships. `R3a` is shown as the pairing-derived
mediator and `TRACE` as the assurance store; neither is reclassified as an
R-node in the control-stage census.

The C19 agreement check now compares this SVG layer with the sibling EDN
population as well as comparing the SVG with the tracked PDF. The published
21-page `plop-2026.pdf` was inspected with `pdftotext -layout`; it contains
`R3a`, `TRACE`, `measured route`, and `observe construction`.

The edge census remains 63 distinct endpoint pairs. Source counts are: 22
original drawn, 26 derived, 9 WM-RUN2 receipts, 3 measured-original-drawn,
8 route-measured-drawn (including 2 pairing decompositions), 11 measured
union, 4 node-to-node specified deliveries, 4 specified deliveries involving
a non-node, and 13 role-play edges. The denominator did not move; the figure
layer did.

`wmRunConformsToWiring` is now dischargeable but is not rebound here. Its
checker consumes the original and measured figure layers separately and the
nonempty WM-RUN2 receipt has zero unmapped hops. Its negative control appends
an unmapped hop and is rejected. Before C69 the checker only required a
nonempty route and therefore could print PASS with unmapped hops; that vacuous
acceptance is removed.

Canonical invocations:

```sh
bb checks/control_map_figure_agreement_check.clj
bb checks/control_map_figure_agreement_check.clj --negative
bb scripts/edge_census.bb
bb checks/wm_route_conformance.clj holes/labs/wm-contract/tick-run-record-2026-08-30.edn
bb checks/wm_route_conformance.clj --negative holes/labs/wm-contract/tick-run-record-2026-08-30.edn
bb scripts/merge_edges.bb --check
bb -cp . checks/hyper_edge_exemplar_check.clj
bash /home/joe/code/p4ng/build-p4ng.sh plop-2026
pdftotext -layout /var/www/zone.hyperreal.enterprises/wip/plop-2026.pdf -
```
