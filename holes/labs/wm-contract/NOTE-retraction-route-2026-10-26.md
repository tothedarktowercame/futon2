# Tech note: what the retraction route was for, and what unwiring it leaves

claude-9, 2026-10-26. Written for decision sheet item 4 (U5), where Joe took
option (b) "for now" and asked for this note so the unwired code is not
another built-not-wired piece nobody can explain.

## 1. What a retraction is

`futon3c/scripts/pattern_retraction.py` (fc418d6ec, 2026-09-30, "compute
pinned graph retractions") computes, for one reading of a mission, a small
connected sub-graph of the mined pattern graph that contains the patterns the
reading cited. The inputs are:

- the **seeds**: the pattern ids the 象 reading marked `candidate` in the
  mission's analysis record (`retraction_cascade/reading-pattern-order`);
- the **pinned pattern graph** (`pattern_graph_pin`): library patterns as
  nodes, with edges of kind `why`, `how`, `used-together`, `co-cited`,
  `rejected-beside`, `next-in-session` (and `co-rejected` in older files),
  each kind priced by a weight (interim choice by claude-2, 2026-10-05, not
  a ruling: why/how 1, used-together/co-cited 2, rejected-beside/
  next-in-session 3, co-rejected 8);
- `k`, the number of alternatives (3 on the canonical route).

The computation is a Steiner tree over the weighted pair graph connecting
the seeds, then up to k-1 cheapest alternatives; non-seed leaves are pruned.
An authored `why`/`how` relation gives an edge a direction; otherwise the
edge is an overlap. `retraction_cascade/from-retraction` (82198db5e,
2026-09-30) turns one such tree into an arranged cascade: nodes are the
tree's pattern ids, directed edges become `:precedes`, undirected become
`:overlap`, unit order is reading order then a target-stable hash.
`futon2.aif.pattern-retraction` is the Clojure parity implementation; the
Python stays the cross-language oracle (`pattern_retraction_test`).

So a retraction answers: *given the patterns this reading saw in the
mission, which library patterns sit between them, by the relations the
library itself records?* It is the mined graph "retracted" onto the reading's
seeds.

## 2. What it was for

`NOTE-wiring-reading-to-decision-2026-09-30.md` §3 chose to compute
retractions in-process (option 3) so that "all policies still enter the same
decision gate and joint selector". The intent, in
`NOTE-g-over-head-cascades-2026-09-30.md`, was an **adjustment set** for the
HEAD reading: beside the reading's own cascades (the `alternatives` and
`overlap` materialisations of what 象 read), the machine would also price
cascades that connect the same patterns through library relations the
reading did not cite, so that a cascade the reading missed could win on G.
The lab table of 2026-09-30 shows the kind of result wanted: for
M-self-documenting-stack, `retraction-3` had the lowest G of four distinct
policies and introduced `social/explicit-exit-over-abandonment`, a pattern
the reading had not placed. The same note records the limit: the pattern the
machine actually used in grounded change `891001d`
(`coordination/mandatory-psr`) was in none of the four, so the adjustment
set did not contain the real cascade either.

`target_policy_family/policy-family` (d8cf98f38, 2026-09-30) made this the
production shape: one family per target = reading policies + retraction
policies, deduplicated by structural identity, with counted failures for
seeds that are not in the graph or have no edges.

## 3. Why it cannot bind on the canonical route

Since the reading-era route (R14, U5; `E-outer-selection-landscape.md`
2026-10-25/26), a policy binds only when every node has an **occurrence
identity**: `[pattern fragment-index]`, the place in the mission text where
象 read that pattern, with a validated three-halves square for it.
`executable_cascade/occurrence-node` makes nodes only from such occurrences.
A retraction's nodes are bare pattern ids from the graph: the patterns it
adds were, by construction, *not* read at any fragment, so there is no index
to carry. `cascade_shape_g/occurrence-arrangement` falls back to
`:occurrence-id = pattern`, and `selection_cascade_identity/bind-entry`
refuses each such policy `:occurrence-identity-absent` at
`:pre-selector-identity-check`, before any structural build. On the
2026-10-24 click that was 9 of 9 retraction policies (three per bound
target); on the R14 fixture 3 of 3. Each was constructed, deduplicated,
scored by `score-family-field`, and counted in the family's
`:distinct-count` first.

The two designs pull apart at exactly one point: the adjustment set wants to
add patterns the reading did not see, and the canonical route wants every
pattern to have been seen somewhere. A retraction is, by definition, the
part of the cascade that was not read.

## 4. What option (b) does and does not do

Does: `policy-family` stops reporting retraction policies on the canonical
route; `:reported-count` and `:distinct-count` count reading policies only;
`:provenance :retraction` records that retractions were computed and
excluded, with the reason (`:occurrence-identity-absent`) and this decision
(sheet 2026-10-26 item 4 (b)), so a reader of the family record can see
what was left out and why. The retraction computation itself is untouched.

Does not: delete or move `pattern_retraction.clj`, `retraction_cascade.clj`,
the Python tool or the lab reader in `cascade_shape_g` (which still reads
`.retractions.json` beside lab artifacts and is not the canonical route).
Those stay as the lab's instrument for the question in §2.

## 5. What option (a) would take, if a retraction is ever wanted

A retraction node needs an occurrence identity. The honest way to give it one
is not graph arithmetic: it is a reading act. The added pattern would have to
be placed at a fragment of the mission text by 象 (a nominated occurrence,
with a three-halves square built by `three-halves-square/build-from-situation-maps`
and published through `occurrence-square-registry/publish!`), after which it
is a reading policy like any other. That is the design the sheet called
"new design"; nothing in (b) forecloses it, and the retraction computation
would then be a *proposal generator* for 象 (which patterns to go and look
for in the text) rather than a policy source for the selector.

## 6. Tests that pin the old contract

`target_policy_family_test`: `reading-and-retraction-form-one-family`,
`one-isolated-seed-does-not-remove-the-other-seeds-retractions`,
`lab-reader-and-policy-family-share-retraction-structures`, and the
`retraction-policy` helper used by the direction/overlap tests. These are
re-pinned by the unwiring packet to the new contract (family excludes
retractions; provenance names them), or moved to `pattern_retraction_test`
where they test the computation rather than the family.
