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
`futon2:holes/labs/wm-contract/runs/F12-organise/21-o4-edges-constructibility.edn:3829-3855`.

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
`futon2:holes/labs/wm-contract/runs/F12-organise/21-o4-edges-constructibility.edn:2-40`.

Zaif remains one carrier short in each of its three sets.  Its refusal is at
`futon3:checks/zaif-cascade.edn:167`, and the independently checked premise is
recorded at
`futon2:holes/labs/wm-contract/runs/F12-organise/21-o4-edges-constructibility.edn:196-207`.

## 3. Mining is a distinct representation

The mining record contains three rule-bearing members and two edges, but its
rules are prose `:attested` interpretations at
`futon2:holes/labs/library-loop/runs/mining-exemplar/cascade.edn:44-65`, not the
gate's executable `:if`, `:however`, and `:then` predicates.  The checker keeps
it as an explicit excluded-representation row rather than silently omitting it;
that row is at
`futon2:holes/labs/wm-contract/runs/F12-organise/21-o4-edges-constructibility.edn:3759-3821`.

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

Validity reads the record files and the gate rule table and refuses the report
as given, claim by claim, at
`futon2:holes/labs/wm-contract/f12_o4_edges_constructibility.bb:153-208`.  For
each row it navigates the row's own `:structural-path` in the record's bytes and
requires the recorded members, selected, edges and rule-table carriers to equal
what stands there; it requires each carrier to be both a gate rule-table id and a
member of that set; it requires each pointer to resolve to a line still carrying
its key; it requires the record's `:o4` to equal the bytes; and it requires the
row count to cover every member-set node in the record.  It does not recompute
the report and compare, which is recorded on the artifact at
`futon2:holes/labs/wm-contract/runs/F12-organise/21-o4-edges-constructibility.edn:3878-3883`.

Eight plants swap a carrier for a non-rule keyword at equal count, swap it for a
rule-table id that is not a member, reverse an edge at equal count, drop one of
two edges, concatenate selected into members, shift a pointer, alter O4, and drop
a member set.  All eight land, and each is refused for a named source reason
rather than for differing from a recomputation; the reasons are recorded per
plant at
`futon2:holes/labs/wm-contract/runs/F12-organise/21-o4-edges-constructibility.edn:135-195`.
Six of the eight are missed by a count-only predicate, which is computed here
rather than asserted (`:naive-count-only-would-catch?` on the same rows).  Every
plant that can be aimed is aimed at the retrodiction member sets the answer rests
on.  The unmutated verdict is true at
`futon2:holes/labs/wm-contract/runs/F12-organise/21-o4-edges-constructibility.edn:3884`,
and it now requires the four premises to hold as well as the plants to be caught.

## 6. Review of the slice-6 delivery, and what it changed

The delivery (futon2 60f7e0bf, codex-17) was reviewed here and three defects were
found and repaired in this repo rather than re-dispatched.  The measured answer
of §2 is unchanged by all three: it was re-derived independently from the record
bytes before the repair, and the four candidate rows, their 50/60 member counts,
their two carriers and their 1/2 edges all stand.

1. **The plant column measured nothing.**  The predicate opened with
   `(= r (core-report))`, which is false for every landed plant by construction,
   since a plant is a mutation of the report the predicate recomputes.  Probed
   directly, the source-grounded half of the conjunction caught one plant of six;
   the other five passed it untouched.  This is the same defect found in review
   of slice 5 and repaired at futon2 b9e85cf3, reintroduced in the same shape.
   Repaired as described in §5.

2. **The plants were aimed away from the answer.**  They addressed
   `[:records 7 :member-sets 1]` and `[:records 7 :member-sets 2]`, which are the
   agency-desktop-save member sets, not the kangaroo sets the verdict turns on
   (rows 4 and 5).  Row 1 carries no carriers and no edges, so
   `:swap-carrier-same-count` in fact moved a carrier count from 0 to 2, and
   `:reverse-edge-same-count` appended an empty list to an empty edge vector.
   Neither did what its name said, and no plant perturbed a candidate row.
   Repaired: the plants are retargeted at rows 4 and 5, and two were added.

3. **`:naive-count-only-would-catch?` was an assertion, not a measurement.**  It
   was a hardcoded set naming two plants.  Computed against an actual count-only
   predicate, it is wrong for `:shift-pointer` and `:change-o4`, which a
   count-only check also misses.

Two further findings are recorded on the artifact rather than repaired, because
repairing either is new measurement and not this slice's question.

- **Precondition B reads only an `:edges` vector under the node.**  Records that
  state their authored-edge count under `:cascade-edges` instead read as
  edge-free: zaif carries one cascade edge per run at
  `futon3:checks/zaif-cascade.edn:298` and `futon3:checks/zaif-cascade.edn:464`,
  and construct carries five at `futon3:checks/construct-cascade.edn:183` and
  `futon3:checks/construct-cascade.edn:262`, while both read
  `:meets-precondition-b? false` here.  The verdict is unaffected — a sweep of
  the corpus for member sets with two or more rule carriers returns only the four
  retrodiction rows, so no candidate is hidden — but it misdescribes what zaif is
  missing.  Zaif is the one section that is actually played through the gate, and
  it is short one rule carrier, not short edges.  Each record now carries the
  lines where it states a `:cascade-edges` count, listed and not zipped to nodes,
  and each row carries `:precondition-b-reads-only`.
- **The member, selected and edge pointers are record-grain, not row-grain.**
  Each is the first line of the record carrying that key
  (`futon2:holes/labs/wm-contract/f12_o4_edges_constructibility.bb:68-70`), so
  the two kangaroo rows share the edges pointer
  `futon3:checks/retrodiction-cascade.edn:6535` although the 60-member set's own
  edges are at `futon3:checks/retrodiction-cascade.edn:6742-6744`.  Every row now
  carries `:pointer-grain :record-level-first-occurrence-of-the-key` so the
  pointers are not read for more than they locate.

What the review checked, so it is auditable: the checker was read in full and
re-run; the artifact reproduces byte-identically across runs (sha256
`623369a48984a5ecf283e356461d516f82a77eb1774c6dfc6367cf07cc1dd28f`); the four
candidate rows were re-measured independently out of the pinned record bytes
(50 members/1 edge and 60 members/2 edges, carriers
`:agent/budget-bounds-exploration` and `:agent/pause-is-not-failure`, in both
retrodiction encodings); the corpus was swept independently for any other member
set with two or more carriers and none exists; the gate lines 206, 218, 234, 249,
444, 445, 552 and 555-558 were printed and read; `:o4` occurs exactly once in
each retrodiction record (34090 and 33974), so the artifact's use of the last
`:o4` line as the pointer and the first as the reading does not misalign for the
candidates; and the emitter literal at
`futon3:checks/construct_retrodiction_cascade.clj:335` was read in place.
