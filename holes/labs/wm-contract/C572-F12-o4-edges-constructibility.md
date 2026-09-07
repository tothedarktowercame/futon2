# C572 — F12 O4-and-edges constructibility measurement

## 1. Measured preconditions

This checker restates the ten-record slice-5 corpus at
`futon2:holes/labs/wm-contract/f12_o4_edges_constructibility.bb:8-18` and reads
futon3 records at the recorded pin shown in
`futon2:holes/labs/wm-contract/runs/F12-organise/21-o4-edges-constructibility.edn:42-45`.
It enumerates `:find :selected` sets and run `:cascade :members` sets separately;
the extraction keeps the two fields distinct at
`futon2:holes/labs/wm-contract/f12_o4_edges_constructibility.bb:61-75`.

The gate's four rule-table ids and the two-member threshold are read from
`futon3c:scripts/zaif_cascade_gate.clj:206-249` and
`futon3c:scripts/zaif_cascade_gate.clj:444-445`.  Their pinned library files all
exist with measured lengths 22, 95, 32 and 30 lines, recorded at
`futon2:holes/labs/wm-contract/runs/F12-organise/21-o4-edges-constructibility.edn:3354-3394`.

## 2. Existing member sets meet both conditions

The top-level result is true at
`futon2:holes/labs/wm-contract/runs/F12-organise/21-o4-edges-constructibility.edn:1`.
Four rows meet both preconditions: the budget and marginal kangaroo cascades in
each of the retrodiction and per-clause retrodiction records.  Their paths,
50/60-member counts, two rule carriers, and exact 1/2-edge vectors are recorded
at `futon2:holes/labs/wm-contract/runs/F12-organise/21-o4-edges-constructibility.edn:46-134`.

Those member sets do not lack rule carriers or authored edges.  What their
records lack is an O4 play measurement: both records carry the literal
`:not-exercised-nothing-is-played`, emitted unconditionally at
`futon3:checks/construct_retrodiction_cascade.clj:335`.  The per-candidate
readings, record pointers, and emitter classification are at
`futon2:holes/labs/wm-contract/runs/F12-organise/21-o4-edges-constructibility.edn:2-41`.

Zaif remains one carrier short in each of its three sets.  Its refusal is at
`futon3:checks/zaif-cascade.edn:167`, and the independently checked premise is
recorded at
`futon2:holes/labs/wm-contract/runs/F12-organise/21-o4-edges-constructibility.edn:172-183`.

## 3. Mining is a distinct representation

The mining record contains three rule-bearing members and two edges, but its
rules are prose `:attested` interpretations at
`futon2:holes/labs/library-loop/runs/mining-exemplar/cascade.edn:44-65`, not the
gate's executable `:if`, `:however`, and `:then` predicates.  The checker keeps
it as an explicit excluded-representation row rather than silently omitting it;
that row is at
`futon2:holes/labs/wm-contract/runs/F12-organise/21-o4-edges-constructibility.edn:3298-3353`.

## 4. Pair-shape scope

If the four candidate member sets were exercised through the measured gate,
their before/after precedence pair would be emitted as vectors at
`futon3c:scripts/zaif_cascade_gate.clj:555-558`.  Slice 5 recognises only two
maps over one key set at
`futon2:holes/labs/wm-contract/f12_o4_edges_census.bb:53-57`; consequently its
pair detector would not see the gate-emitted vector shape.  This scope result is
carried on every candidate row at
`futon2:holes/labs/wm-contract/runs/F12-organise/21-o4-edges-constructibility.edn:46-134`.

## 5. Source-grounding and controls

Validity re-derives the complete report from source bytes and resolves member,
selected and edge pointers back to lines carrying those keys at
`futon2:holes/labs/wm-contract/f12_o4_edges_constructibility.bb:137-150`.
Six plants alter a carrier while preserving its count, reverse an edge while
preserving its count, concatenate selected into members, shift a pointer, alter
O4, and drop a member set.  All landed and all are caught; the first two also
demonstrate failures invisible to a count-only predicate.  The plant table is at
`futon2:holes/labs/wm-contract/runs/F12-organise/21-o4-edges-constructibility.edn:135-171`.
The unmutated verdict is true at
`futon2:holes/labs/wm-contract/runs/F12-organise/21-o4-edges-constructibility.edn:3410`.
