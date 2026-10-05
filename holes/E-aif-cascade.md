# E-aif-cascade — the cascade as formation: blends, tokens, causal theories, on the outer loop and the R-nodes

**Status:** IDENTIFY → MAP (opened 2026-10-05). Live. The unchecked items
are the work, in order; each is one packet for a worker seat (codex-33)
with its own acceptance bar and a review by the owner between packets.

**Driver:** War Machine. **Operator:** Joe. **Owner:** claude-2.
**Worker:** codex-33. **Repositories:** `futon2` (notes, reports, data),
`mathlib4/DarkTower/WarMachine` (receipts), `futon7/scripts` (figure-side
scripts), `futon3/library/meta` (the outer contract; edits there are Joe's).

## IDENTIFY

Joe, 2026-10-05: the formal pieces for a convincing cascade exist and are
scattered — the 3/2-category story (a pattern is a conceptual blend with
emergent structure; a cascade is pasted blends: Goguen 1999, 2006), the
wants-token story (`ConstructionReceipt.lean`: an edge carries
`produces ∩ needs`, non-empty), and claude-17's small-causal-world story
(`futon3c/notebooks/daxiang_live.clj` §5–8: a pattern compiles to an open
causal theory, glued by shared names). The excursion brings them together
on real cascades, not invented ones.

Two readings Joe gave that govern the work:

- *A proof is not a tree.* The tree is a parse of the proof, not its
  formation. A cascade can be flattened to a do-this-then-this recipe, but
  the recipe is work beyond the formation and carries no evidence of its
  own: a linear extension, one of possibly many, chosen at a cost of
  log₂(#extensions) bits and no token (CASCADE-SPEC-v2's ordering
  ambiguity). *The cascade is constructed, not chosen.*
- *One pattern is already a diamond.* IF/context = the generic space and
  the tokens accepted; HOWEVER = the two forces, the input spaces; THEN =
  the blend, whose emergent structure is the held tension, and the token
  emitted; BECAUSE = why the square commutes. A cascade composes that inner
  structure with the outer one; a cascade that reduces to a diamond can be
  restated as a pattern. Too many alcoves (Alexander/Salingaros) is the
  off-diagonal `¬IF ∧ THEN`, a solution without its problem, which no
  score charges today.
- *Refinement.* The War Machine's own iterations add either requirements
  (a new square pasted in) or better structure (a higher blend over the
  same inputs; Crumb's crossroads). The outer-loop cascade is to be
  elaborated this way until, in Joe's words, "it's not even possible to
  conceive of it as other than a cascade".

Two real cascades carry the work:

1. **The outer-loop META policy.** `futon3/library/meta/meta-outer-policy-cascade.edn`
   is a diamond: observe → {fill, injury} → minimise; four token-carrying
   edges, two linear extensions. Minimising G there is parametric in the
   META item (the diagram is fixed; G orders the cones over `minimise`'s
   square), where the tactical loop constructs the diagram per problem.
   What the click runs today is not the diamond but
   `meta_pipeline_selector/select`, a pairwise ranking on
   `1/(1+occurrence count)` over the Cascade Live map
   (run c9d25d6a: 107 ranked of 676, selected by centrality cost alone).
2. **The R-nodes.** The harness figure (`futon7a/about.html` →
   `harness.html`, drawn by `futon7/scripts/harness-figure.bb` from
   `p4ng/empirics-futon/aif-lean-dag.edn`) is an elaborated prototype of
   an R-cascade: 17 nodes, 43 term-carrying edges with Lean classes. It is
   a wiring diagram, cyclic (11 nodes in one strongly connected
   component); the cascade is its simplification — one click's formation,
   obtained by a cut at the tick boundary, with its meets.

Authority and record: `holes/labs/wm-contract/NOTE-outer-cascade-as-pasted-blends-2026-10-05.md`
(the three readings on the outer diamond, with measurements folded in);
`CASCADE-SPEC-v3-draft-2026-10-05.md` (the blend square under each unit;
two decisions open for Joe); `CASCADE-SPEC-v2-2026-10-01.md` (three
relations; a missing edge is not simultaneity); `E-outer-loop-improvement.md`
(every added outer-loop signal needs a replayable ablation);
`mathlib4/DarkTower/WarMachine/{CascadeOrder,ConstructionReceipt,Proof2/CoApplicationKernel}.lean`.

## MAP — done so far (2026-10-05)

| step | result | where |
|---|---|---|
| slot census of the 107 ranked items | 103 fill all six slots (= in the field and one unchecked want at HEAD); 2 fill four; 2 absent from the field; reading square forced for 4 of 107; the missing thing is the quality order over 103 near-identical cones | futon2 `597a03288`, `REPORT-meta-slot-census-2026-10-05.md` |
| the diamond as a `ConstructionReceipt` | both linear extensions valid over one support; the chain's fill–injury edge rejected, with or without a forged token | mathlib4 `e5fa4352a5` |
| four numbers: diamond / two chains / bag | G 1.280 / 1.517 / 1.517 / 0.393; with ordering term 2.280 / 1.517 / 1.517 / 4.978; three of the four are not constructible from the units' tokens | futon2 `ce0d84041`, `REPORT-diamond-four-numbers-2026-10-05.md` |
| R-cascade discovery (cut, meets, extensions, receipt projection) | drawn graph: one SCC of 11; after an 11-edge cut two 2-cycles remain (R4⇄R7, R6⇄R17) and the script refuses; with R4 → model/rollout and R6 → candidates/select as data, a 19-unit DAG: 5 sources (R13 R14 R19 R2 R4-model), 4 sinks (CTAU-CLASS R16 R3 SCAN), 23 meets under `CascadeOrder.IsMeet`, 63 incomparable pairs with no common lower bound, 951,616,092 linear extensions (log₂ 29.83), of which **0** respect the five stage columns (7 edges run against them); 7 edges `:absent`-class, token-less in the receipt | futon7 `f8d24291c`, futon2 `b237e82a0`, `REPORT-r-cascade-2026-10-05.md`, `r-cascade/r-cascade.edn` |

| B1 centrality: what the sources hold | Cascade Live `:arrows` are mission → its own next hole (176/176/176; no want is a task) and `:lineage` is dispatch lineage — no inter-item want relation at HEAD; the mined pattern graph has no task→pattern link; no claude-4 rows measure found in a bounded search. The one inter-item relation is shared applied patterns (`:patterns :edges`, 1,715 links, 249 missions): per item (a) distinct patterns, (b) other missions sharing ≥1, (c) incidences; 46 of 107 ranked items have none. Spearman ρ against the live occurrence count: (a) 0.896, (b) 0.824, (c) 0.840 — the live selector's "centrality" is largely the applied-pattern count. Token proposed: `:meta/downstream-unblocking-count`, value (b) or (c), chosen at B3 | `REPORT-centrality-tokens-2026-10-05.md` |

| B2 operator-turn load | Commit-trailer attribution exists only since the signing hook (futon3c `3981fb38`, 2026-09-30 02:32Z); the first run counted pre-hook commits as operator touches (share 0.984, an artefact). Restricted to the hook era: 4 of 107 items have any attributable history (`M-interim-director-proxy-metric-inventory`, `M-daily-scan`, `M-daily-scan-multi-axis-queue`, `E-cascade-real`), all four machine-only (share 0.0; `wm-author`, codex-11/12/13); 103 are `:no-attributable-history`; ρ undefined. Text markers (`needs Joe`, `HIT`, `🈸`, …): 14 documents, 61 lines — the only HEAD-level operator-load signal. Token proposed `:meta/operator-load-share` (ratio + raw counts); support-change vs preference-term consumption left to B3 | `REPORT-operator-load-2026-10-05.md` |

| B1b inter-item `@why` | (i) 961 cross-references among the 107 documents, 72 cued, 45 directed edges after review fixes (hyphen boundary; `successor` ambiguous in both directions — "X's successor is Y" vs "X is the successor of Y" — cue heuristic cannot settle it); most items have no directed edge; ρ with occurrence count 0.052. (ii) `mission-lifecycle.md` asks for "Relationship to other missions" in prose and defines no field; 16 of 107 fill a prose header (`Parent`, `Predecessor`, `Consumer mission`, …). (iii) 545 library `why` edges lift through applied patterns to 422 method-level item pairs — derived, about methods. Conclusion: the semantics were not written down (Joe's reading); declaration proposal in the report; B1c is the first cut | `REPORT-unblocks-relation-2026-10-05.md` |

**Found in A1, needs a ruling (Lean owner: claude-2).** The two Lean
modules disagree on which way a meet points. `CascadeOrder.IsMeet r a b m`
has `m` *reaching* `a` and `b` (a common ancestor in descent, the nearest
one), so `hasMeets` says every pair shares an origin — consistent with Q1,
one cascade per problem, one root. `ConstructionReceipt.MeetWitness` has
paths from the operands *to* the meet (a common descendant), and the
diamond example of mathlib4 `e5fa4352a5` and the note's "minimise is the
meet of fill and injury" follow that direction. In Alexander's order
(larger patterns above) the first is the join and the second the meet.
codex-33 refused to forge reversed paths and left the receipt's `:meets`
empty. Under `CascadeOrder`'s reading the R-cascade's five sources are a
measurable defect — 63 pairs with no common origin — mended by one root
unit, the click's input state (T, τ, C, o, A/B), which is also what the
cut produced. Recommendation: `CascadeOrder` is the semilattice Joe means;
amend `MeetWitness` to meet → operands and re-prove the diamond with
observe as the meet of fill and injury (minimise their join). Joe to
confirm; until then both readings are reported, neither asserted.

## The work

Each item is one packet. A packet's acceptance bar is written here so that
the worker can take the next item without a new brief; the owner reviews
each result (record check, test adequacy, selective re-execution) before
the next starts, and folds the numbers into the note named. No edits to
`futon3/library/meta/*.edn` or `*.flexiarg` by the worker: proposed
contract changes go in the report as a diff for Joe.

### A. The R-nodes as a cascade

- [x] **A1. Discovery** (done; see MAP table). `r-cascade.bb`: SCCs before the cut;
  the cut as data with a reason per edge (`:cross-tick-state`
  `:learned-parameter` `:fit-feedback` `:split-unit`); refuse a surviving
  cycle rather than split R6/R4 silently, then a second pass with the
  split as data; meets per `CascadeOrder.IsMeet`; linear extensions
  against the five stage columns; `:absent`-class edges flagged token-less
  in the receipt projection. Report written for Joe to rule on the cut.
  *Accept:* counts match `aif-lean-dag.edn` (17/43; 23/10/6/1); both
  passes on record; every cut edge justified from its term.
- [ ] **A2. Rulings** (Joe). (i) The cut: the 11 edges of the report's
  table (3 cross-tick state: `u`, `u`/`world`, `s-next`; 4 learned
  parameters `A`,`B` from R4; 2 fit feedbacks `Pi`, `F-pi` into R3; 2
  split-unit). (ii) The splits: R6 → candidates (`interp` in; `pi`,`r`
  out) / select (`G`,`E`,`F-pi`,`tau`,`pi` in; `Q-pi`,`pi` out); R4 →
  model (`A`,`B`; a source, i.e. a have) / rollout (`mu`,`interp`,`T`,
  `pi`,`r`,`rates` in; `Q-o-pi`,`A` out). Owner's judgement: both splits
  follow the term names; the one question is whether R4-model is a unit
  at all or the click's initial have. (iii) Meet direction, above.
  (iv) Whether to add the root unit (click input state) so that the
  R-cascade has meets for every pair.
- [ ] **A3. Lean receipt.** The ruled R-cascade in `ConstructionReceipt.lean`
  as the outer diamond was done: `valid = true` for the ruled receipt;
  `valid = false` for the receipt that includes an `:absent`-class edge
  with its token set empty. *Accept:* `lake build` of the module; one
  theorem per claim; the bad case constructed and rejected.
- [ ] **A4. Drawing.** The R-cascade as a Hasse diagram (and as an
  S-expression), not stage columns; the five-stage order shown as one
  parse of it. Generated by script from `r-cascade.edn`; not wired into
  `about.html` (claude-17's page is not to be disturbed). *Accept:* the
  drawing is regenerated from the edn by one command and the edn's
  source-pins match the inputs.
- [ ] **A5. Each R-unit read as a blend square.** For the units that have
  apparatus flexiargs (`futon3/library/apparatus/`), a table: IF /
  HOWEVER / THEN / BECAUSE against in-tokens / out-tokens from the
  receipt; units whose flexiarg and tokens disagree listed. *Accept:*
  every row cites the flexiarg and the edge; disagreements are typed, not
  smoothed.

### B. Elaborating the outer-loop cascade

Joe, 2026-10-05: centrality (as in claude-4's Lean4-rows work — the term
that unblocks the most rows first) and feasibility (an item needing many
operator turns is not a candidate for overnight automation) belong in the
outer loop. Where they stand now: centrality is the live selector's only
scored channel (`1/(1+occurrences)` on the Cascade Live map) and appears
in the diamond's outcome vocabulary as `downstream-unblocking`;
feasibility exists in the live selector as the `:automated-feasibility`
support filter (`:supported :infeasible :unknown`) and in the diamond as
the machine-side `injury` arm; **operator-turn load is in neither.**

- [x] **B1. Discovery: centrality.** *Done, with amendment (futon2 `e578e4259`, `5237a7598`, `5804aea37`; MAP table).* What "unblocks the most" means per
  task kind on today's field: for missions/excursions, the count of other
  items whose wants name this item's produced tokens (from the Cascade
  Live graph's `:arrows` have/want and the pattern graph); for Lean rows,
  claude-4's measure, read from their record (cite it; do not recompute).
  Output: the centrality token a square would emit, its source, and the
  value for each of the 107 ranked items. *Accept:* one value per item with
  a source pin or a typed absence; the live selector's occurrence count
  reported beside it.
**Ruling (Joe, 2026-10-05, after B1):** "shares patterns" and "unblocks"
have different semantics. *Shares patterns* generalises `@how` (toward the
specific: the methods two items have in common — coupling through the
library); *unblocks* generalises `@why` (toward the general: what rests on
this item, what goes from unanswered to answered when it completes). B1's
measure is the first; the centrality Joe means is the second; they are two
tokens, not one. Also ruled: a simplified cascade based on Cascade Live is
the outer-loop driver (restating 2026-09-25).

- [x] **B1b. Discovery: an inter-item `@why` relation.** *Done, with review fixes (futon2 `911fbefb0`, `8cd60cd9a`; MAP table).* No source holds
  one at HEAD (B1). Find what could: (i) cross-references in the 107
  items' documents to other M-/E-/T- ids, classified by the cue around
  them (`depends on`, `after`, `blocked by`, `unblocks`, `feeds`,
  `prerequisite`, `see`), with the resulting directed graph's in/out
  degrees; (ii) the mission lifecycle's own fields, if any name a
  predecessor or a consumer; (iii) the library's `why` edges (mined graph:
  545) lifted to items through applied patterns, reported as a *derived*
  relation and kept apart from (i). Output: per item, `unblocks-count`
  from (i) with the cue, or a typed absence; and a one-paragraph proposal
  for how items would *declare* what rests on them (`cascades/declared-skeleton`:
  declare only what you would defend in review), since a derived relation
  is at best a prompt for a declaration. *Accept:* every edge cites the
  line it was read from; (i) and (iii) never merged.
**Joe, 2026-10-05, after B2:** unblocks "might be written down somewhere
… but I haven't put a great deal of effort into clarifying those
semantics", so failing to recover it may mean it is not there. Infer it
instead from behaviour: that Joe is working on `E-aif-cascade` now says he
holds it high priority and thinks it unblocks something. And distinguish
*enabler* from *blocker*: `M-diagramprover` is connected to many things
and need not be finished to be useful; Joe often works two things at once
(diagramprover upgraded for Lean work and cascade work together). A
rigorous study of which missions depend on which, and how, is a big
project; a first cut is not. Minimising G is "subject to constraints": a
deep dive to plan all the work across all time is not a good use of time
— the outer cascade is improved over time (§5a of the note).

- [ ] **B1c. Discovery: revealed attention and co-work.** The first cut
  Joe describes, from records that exist: (i) Cascade Live `:lineage` —
  126 dispatch records `{agent target session at dispatched-by}` (B1
  found them; top targets `diagramprover` 25, `apm-demonstration` 22,
  `the-perfect-crime` 14) — per item: dispatches, distinct sessions,
  distinct dispatchers, last dispatch; (ii) operator turns: the turn
  store's record of the mission the operator was working under (the
  `turn-traced` view on `futon7a/about.html`; `futon3c.xiang.turn-record`)
  — per item: operator turns under it in the last 14 days; (iii)
  *co-work*: items targeted by the same session, or by the same agent
  within one day, or named together in one operator turn — an undirected
  *enabler* relation, kept apart from any directed unblocks; report
  `M-diagramprover`'s co-work set as the worked example. Output: per
  item, revealed-attention counts with source pins or typed absences; the
  co-work graph's degree per item; ρ of attention against the persisted
  occurrence count and against B1 (b). Proposal, one paragraph: attention
  as the `unblocks` token's first-cut value (an *enabler weight*, not a
  dependency), to be superseded by declarations (B1b) where they exist.
  *Accept:* every count cites its record; the three sources never merged;
  no claim that attention is dependency.
- [x] **B2. Discovery: operator-turn load.** *Done, with amendment (futon2 `b0405aa8c`, `0c56efe08`, `45af3c447`; MAP table).* From the run records and the
  mission documents: per item, the share of its last N touches that were
  operator turns vs machine-authored (`:last-touch :state`, commit
  trailers), and any mission text that names an operator step
  (`HIT`, "Joe decides", `🈸`). Output: a feasibility token with its
  source. *Accept:* one value or typed absence per item; the rule stated
  as data, not prose.
- [ ] **B3. The extended diamond, as a proposal.** New squares pasted in
  as the blend reading requires — each with its I₁, I₂, G, B and the
  token it emits, and the edge it adds (unblocks (`@why`): observe →
  unblocks-reading → minimise, consumed by the generative model's
  `downstream-unblocking` term, emitting a typed absence until B1b's
  relation exists or is declared; shares-patterns (`@how`): a separate
  square and token, consumed only if Joe wants coupling scored at all;
  feasibility: observe → operator-load → {fill, minimise}, as a
  support change like `injury` or as a preference term — both written up,
  Joe chooses). The result as an edn diff against
  `meta-outer-policy-cascade.edn` and as a `ConstructionReceipt` example;
  its meets and linear extensions. *Accept:* the edn validates under
  `meta_outer_policy/contract-errors` (run in a fresh process on the
  proposed edn); the receipt is `valid`.
- [ ] **B4. Does the extension change the picture?** Re-run the
  four-number script on the extended diamond and its bag; and, with the
  provisional prior (`meta-outer-provisional-prior-v1.edn`) and the B1/B2
  tokens as the only candidate-conditioned inputs, rank the 103 cones and
  compare with the live selector's ranking of the same run. *Accept:*
  both rankings on record with the Kendall distance between them; the
  ablation rule of `E-outer-loop-improvement` honoured (each new signal
  removable by a flag, and the ranking with it removed recorded).
- [ ] **B5. The restated pattern.** `meta/select-the-meta-item` as a
  flexiarg draft (IF = observe; HOWEVER = ordinary work against
  self-repair, and now against operator load; THEN = argmin G over the
  admitted family; BECAUSE = G over policies with predicted consequences),
  in the report, for Joe to promote or not.

### C. The causal reading and the chain comparison

- [ ] **C1. Per-click unmet table.** Compile the outer diamond's four (or
  six) patterns with claude-17's `ot/theory` / `ot/glue`, link theories
  running down the diamond, and evaluate against a run record; output the
  table the note §4 gave by hand. *Held* until claude-17's page has
  settled; uses his compiler unchanged.
- [ ] **C2. A meaningful chain-versus-cascade comparison.** The four-number
  measurement compared one cascade with three non-cascades. Find or
  construct a real four-unit example in which a token-carrying chain and a
  token-carrying non-chain both exist over the same units (candidates: the
  tactical cascades of `NOTE-g-over-head-cascades-2026-09-30.md`), and
  score both with the ordering term in its own column. *Accept:* both
  receipts `valid`; the two G values and the two ordering terms on record.
- [ ] **C3. Charge the other off-diagonal.** *Proposal written: note §5b (futon2 `0ec367ad6`); awaits Joe.* A proposal, not code: how
  `¬IF ∧ THEN` (the token emitted with no consuming want; the house of
  alcoves) enters the score — via v2's rule that an unconsumed token earns
  nothing — with the bag measurement as the test case it must fail.

## DERIVE — exit criteria

- The R-cascade exists as data, receipt and drawing, with the cut ruled.
- The outer-loop diamond is extended with centrality and operator-load
  squares as a proposal Joe has seen, with the measured effect on the
  ranking of a real run.
- The three readings are shown on one cascade end to end: blend squares
  per unit, token-carrying edges with a `valid` receipt, and a per-click
  unmet table from the causal compile.
- A chain-versus-cascade G comparison exists in which both arrangements
  are constructible.

## Re-entry rule

A worker takes the next unchecked item in order within its section;
sections A and B may proceed in parallel on two seats; C1 waits for the
owner's word. Every result is a commit plus a bell to the owner with the
sha; the owner reviews before the next item in that section starts. Joe's
rulings (A2; the B3 choice; B5 promotion; the CASCADE-SPEC-v3 decisions)
are recorded here when given.
