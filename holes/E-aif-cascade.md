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

| B1c revealed attention | Lineage: 126 dispatch records over 29 targets; 15 of the 107 ranked items have any (top `M-the-perfect-crime` 14; the selected `M-interim-director-proxy-metric-inventory` 0). Operator turns under a mission exist in futon1b evidence (`:clocked-target`, 3,339 of 4,087 turns in the 14-day window carry one): 11 of 107 have any (`M-the-perfect-crime` 305, `M-a-wmc-scaling` 136, `E-cascade-real` 133, `M-autoclock-in` 94; the selected mission 0). Co-work graph over 29 targets, 54 edges; `M-diagramprover`'s set: 11 items by same-session / same-day / operator co-mention. ρ of dispatches vs occurrence count −0.044, vs B1 coupling −0.207: the live selector's centrality is uncorrelated with where attention goes. Proposal: attention as the `unblocks` token's first-cut *enabler weight*, superseded by declaration | `REPORT-revealed-attention-2026-10-05.md` |

| B1d cheat sheets | 107 one-line sheets, classical counts only. `M-weird-modernism`: register 0.209 (bottom quartile; body sections 0.04–0.25, "Methodology note: the mission as a wyrd-engineered artifact" 0.056), history trajectory flat over 2 commits — the abstract spin-off reads. `M-the-perfect-crime`: register 0.463, IDENTIFY 377 lines at 0.472, history +0.262 over 16 commits — the return to concrete reads, in the history direction. Mission register median 0.474 (q1 0.330, q3 0.529); ρ of register vs B2 marker count 0.224, vs B1 coupling 0.040; the two trajectories disagree on 8 sheets, flagged. Fields proposed for the `observe` square: scope counts, keywords, applied-pattern order, register and both trajectories, status, phase | `REPORT-cheat-sheets-2026-10-05.md` |

| A3 R-cascade receipt (ruled defaults) | 19 units (root `click-input` producing A B C T U-t o τ), 40 edges, 34 token-carried in support, 104 `MeetWitness` over 108 incomparable pairs, `rCascadeReceipt_valid` and the absent-edge bad case both by `native_decide`, lake 585 jobs. **Four pairs have no meet, all with R4:** (R4,R16) (R4,R17) (R4,R6-select) (R4,SCAN). Cause, read in the edn: R4's only outgoing term edges, `R4→R5` (A, Q-o-pi) and `R4→R8` (A), are `:absent` class — the Lean model does not carry the forward model's outputs — so in the receipt R4 is a sink, incomparable with everything downstream of R5, and its incomparable ancestors R13 and R2 leave those pairs without a greatest common origin. The missing meets are the Lean model's missing edges, seen as order structure; the mend is in `aif-lean-dag.edn`'s `:absent` rows, not in the cut | `REPORT-r-cascade-2026-10-05.md` (ruled pass), `r-cascade/r-cascade-ruled.edn`, `mathlib4/DarkTower/WarMachine/RCascadeReceipt.lean` |

| B4 part 2, premise check (codex-33 stop, 2026-10-06 05:0xZ) | The contract (`meta-outer-policy-cascade.edn`, 11 raw outcomes: `:elapsed-ms`, `:token-use`, `:abstention`, …) and the provisional prior (`meta-outer-provisional-prior-v1.edn`, 5 normalised outcomes: `:elapsed-budget-fraction`, `:token-budget-fraction`, `:abstention-or-failure`, …), both dated 2026-10-02, do not share a vocabulary; `evaluate` would refuse `:outcome-vocabulary-invalid` on any candidate built from the prior. So the diamond could never have been evaluated as written. Settled in-lane under the defaults policy: a measurement contract, `proposals/meta-outer-policy-cascade-measurement-v1.edn`, identical to the contract except that its vocabulary is the seven outcomes that have a declared preference (the prior's five + `:downstream-unblocking` and `:operator-demand` with proposal-prior-v2 rows); `contract-errors` → `[]`. The five contract outcomes without a preference are excluded for prior-v1's own reasons. Tuning item: one vocabulary for contract and prior in futon3 (Joe's edit) | this row; the proposal edn |
| B4 part 2 (codex-33 `bd8086364`, reviewed) | Real `evaluate`, measurement contract, 103 cones, readings as the only candidate-conditioned inputs: full selects `M-warrant-limit` (τ-b 0.033 vs live, n 103); live winner falls to 17th (attention 0 demotes it; sheet register 0.706 promotes it); `--no-attention` τ −0.029 (live winner 5th), `--no-sheet` τ 0.084, both τ 0 with all pairs tied. EIG honest 0; ambiguity constant (default variances) so risk alone orders. Findings: a 103-way tie is returned as `:selected` by string order (evaluator types no tie); readings do not travel on the receipt under the measurement contract | `REPORT-rank-with-readings-2026-10-06.md`; `proposals/meta-outer-provisional-prior-v2-proposal.edn` |
| A4 drawing (codex-33 `d42c24bcf`/`a3a15b44e`, reviewed) | Hasse diagram of the ruled R-cascade from `r-cascade-ruled.edn` by one command (`bb futon7/scripts/r-cascade-draw.bb`), byte-identical on rerun, pins in every header. Transitive reduction 34 → 26 edges (dropped: `R1→R3`, five `R2→…`, `R6-candidates→R6-select`, `click-input→R4`); 6 longest-path layers; `click-input` and `R16` alone in theirs; the four no-meet pairs marked; stages as colours with the 0-extensions caption | `r-cascade/{r-cascade.dot,r-cascade.svg,r-cascade.sexp,r-cascade-stages.svg,DRAW.md}` |
| A5 blend squares (codex-33 `7cb4abaa8`, reviewed + note) | 14/19 units have a content-matched flexiarg (all `aif/`; `apparatus/` none; CTAU-*, SCAN, click-input, R19 without). Agreement after retyping: agrees 3 (R5, R8, R14), silent 3 (R1, R2, R7), disagrees 5 (R13, R17, R3a, R6-candidates, R6-select: token is a wire, THEN names the product), boundary artefacts 3 (R3, R16 cut; R4 Lean-absent), no-flexiarg 5. R10 working answer holds where tokens are products, bends where the token list was cut for the Lean DAG | `REPORT-r-blend-squares-2026-10-06.md` |
| R8/C3 consumption measurement (codex-33 `146ec2675`, reviewed) | Rule (a) on §5 + alcove + B4 extended: diamond = chains = `[0 1 4 4]`; bag and extended bag refused at τ=1; alcove adds nothing; extended = its chain. Today's rule scores the alcove *better* than the diamond (1.007 vs 1.280): fired-pattern C rewards an unneeded pattern. G(a) `:unavailable` — scorer state lacks consumption provenance; unapplied diff proposed | `REPORT-consumption-four-numbers-2026-10-06.md`, `scripts/consumption_four_numbers.clj` |

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
- [x] **A3. Lean receipt.** *Done under the defaults (futon7 `51212a893`, futon2 `1d530b653`, mathlib4 `5319205cd`; MAP table). Reviewed by claude-2: defaults applied as data; `click-input → R4` carries {A, B, T} because `edgeValid` wants the exact intersection and R4 also needs T — right, not a weakening; token Nats spot-checked (R1→R4 mu=14, R13→CTAU-CLASS T=9, click-input→R4 {0 1 9}); no `sorry`; the absent-edge bad case rejected.* The ruled R-cascade in `ConstructionReceipt.lean`
  as the outer diamond was done: `valid = true` for the ruled receipt;
  `valid = false` for the receipt that includes an `:absent`-class edge
  with its token set empty. *Accept:* `lake build` of the module; one
  theorem per claim; the bad case constructed and rejected.
- [x] **A4. Drawing.** *Done (codex-33, futon7 `d42c24bcf` script, futon2 `a3a15b44e` outputs + `r-cascade/DRAW.md`; reviewed claude-2: reduction, dropped edges and layers recomputed independently and identical; regenerated once more, trees clean). 34 support edges reduce to 26; 6 layers, `click-input` alone at the top and `R16` alone at the bottom; the eight dropped edges include `click-input → R4` (A, B, T) and five of R2's outputs, all implied by longer paths. Stage order drawn as a colouring with the recorded 0-extensions fact as caption; no graphviz on the host, so the SVG is laid out in babashka.* The R-cascade as a Hasse diagram (and as an
  S-expression), not stage columns; the five-stage order shown as one
  parse of it. Generated by script from `r-cascade.edn`; not wired into
  `about.html` (claude-17's page is not to be disturbed). *Accept:* the
  drawing is regenerated from the edn by one command and the edn's
  source-pins match the inputs.
- [x] **A5. Each R-unit read as a blend square.** *Done (codex-33, futon2 `7cb4abaa8`, `REPORT-r-blend-squares-2026-10-06.md`; reviewed claude-2 with a retyping note appended: three of the eight `:disagrees` were the receipt's boundary — R3 and R16 lose their outputs to the cross-tick cut, R4 to the Lean-absent edges — so the counts are agrees 3, silent 3, disagrees 5, boundary 3, no-flexiarg 5). Fourteen of 19 units have a content-matched library flexiarg (predicted 12), all in `aif/`; none in `apparatus/`. The five real disagreements (R13, R17, R3a, R6-candidates, R6-select) are where the receipt's token is a wire and the flexiarg's THEN names what the wire stands for; R10's working answer holds where tokens are the pattern's products (R5, R8, R14) and bends where the token vocabulary came from the Lean DAG.* For the units that have
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

- [x] **B1c. Discovery: revealed attention and co-work.** *Done (futon2 `0a6f806c8`; MAP table).* The first cut
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
**Joe, 2026-10-05, after B1b:** reading from HEAD is "like jazz or free
jazz" — it will not say whether a mission has spun off into abstraction
(`M-weird-modernism`) or come back from abstraction to something concrete
(`M-the-perfect-crime`). Beside HEAD we want a chord "cheat sheet": a very
quick distillation of the scopes and keywords from across the mission.

- [x] **B1d. The cheat sheet per item.** *Done as first cut (futon2 `24fd97fac`); two defects noted in the report for the next pass (bare relative tokens attributed to the document's repo; appended 30-Sept checklists inflate the section trajectory).* One line per item, computed by
  script from the whole document and its history, not from HEAD alone:
  (a) *scopes* — the repos and directories its paths touch, by count;
  (b) *keywords* — top terms by tf-idf over the 107 documents, plus the
  document's own `@keywords`/header keywords where present; (c) *the
  changes* — the applied patterns (Cascade Live `:patterns :edges`) in
  order of first mention in the document, the harmonic progression under
  the tune; (d) *register* — a concreteness score per section: share of
  lines carrying a path, a sha, a number, a checkbox or a date, against
  prose lines — and its *trajectory*, early sections vs late and first
  commits vs last (`git log` by window), so that spinning off into
  abstraction and coming back read as a direction, not a level; (e)
  status line and lifecycle phase. Worked examples: `M-weird-modernism`
  (1,037 lines, 101 paths, 5 checkboxes, "perpetual-projection mode") and
  `M-the-perfect-crime` (557 lines, 100 paths, 6 checkboxes, 13 commit
  shas). Format: a table row and a one-line sheet, e.g.
  `| scope futon3c/agency ×41 · futon2/aif ×12 | keys clock, lineage, dispatch | changes p1 → p2 → p3 | register 0.62 ↑ |`.
  Output: `REPORT-cheat-sheets-2026-10-05.md` with the 107 lines, the
  register distribution by kind, ρ of register against B2's marker count
  and B1's coupling, and one paragraph on which parts of the sheet the
  `observe` square should emit as tokens (register and trajectory are the
  feasibility signal B2 could not get from commit history). *Accept:* the
  two worked examples read right to Joe; every number traceable to a
  script line; no LLM summarisation — classical counts only, so that the
  sheet is the same on every run.
- [x] **B2. Discovery: operator-turn load.** *Done, with amendment (futon2 `b0405aa8c`, `0c56efe08`, `45af3c447`; MAP table).* From the run records and the
  mission documents: per item, the share of its last N touches that were
  operator turns vs machine-authored (`:last-touch :state`, commit
  trailers), and any mission text that names an operator step
  (`HIT`, "Joe decides", `🈸`). Output: a feasibility token with its
  source. *Accept:* one value or typed absence per item; the rule stated
  as data, not prose.
- [ ] **B3. The extended diamond, as a proposal.** *Written (claude-2): `PROPOSAL-outer-diamond-extended-2026-10-06.md` + `proposals/meta-outer-policy-cascade-extended-proposal.edn`; seven units, ten edges, 120 linear extensions; awaits Joe's two choices (preference term vs support change; coupling scored vs recorded) — validation and receipt go to B4.* New squares pasted in
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
- [x] **B4. Does the extension change the picture?** *Part 1 done (codex-33, futon2 `8b6a1afef`): the real `contract-errors` returns `[]` for the proposal and for the contract (the validator does not object to the added `:tokens`/`:conditioning` keys or the new outcome); 120 linear extensions, ten meets in each direction; scores with the same scorer — original diamond 1.2797 (control reproduced), extended shape 0.7321 (P(all done) 0.52 at horizon 7), extended bag 0.2381, an extended chain 1.3105: bag < cascade < chain again, with the same caveat that only the cascade is constructible from the tokens. Part 2 done (codex-33, futon2 `bd8086364`, `REPORT-rank-with-readings-2026-10-06.md`, `scripts/rank_with_readings.clj`, reviewed claude-2: τ recomputed independently from the report and the run record, 0.033365148 exact; prior-v2 = v1's five channels verbatim + the two ruled rows; futon3 untouched): the real `evaluate` ranked the 103 cones under the measurement contract with the B1c/B1d readings as the only candidate-conditioned inputs. Full: selects `M-warrant-limit`, τ-b vs live 0.033 (n 103, 16 measurement ties); the live winner `M-interim-director-proxy-metric-inventory` (register 0.706, markers 1, dispatches 0, operator turns 0, co-work 0) lands 17th — the sheet reading promotes it, the attention reading demotes it (5th with `--no-attention`, 54th with `--no-sheet`). Ablations: `--no-sheet` τ 0.084 (3928 ties), `--no-attention` τ −0.029, both τ 0 with all 5253 pairs tied. Epistemic value is an honest 0 (singleton model through the real EIG kernel); ambiguity is constant 5.08 because every predicted variance is the default 0.25, so the order is the risk term alone. Two findings: (a) with both readings removed every candidate has the same G and `evaluate` still returns `:status :selected`, `:selection-reason :minimum-canonical-G`, choosing by string order — a tied minimum is not typed (futon2 `meta_outer_policy.clj` `sort-by (juxt :g (comp str :id))`); a tuning item for the evaluator, not for this lane. (b) The readings condition G but do not travel on the receipt: the measurement contract keeps the original `:receipt` keys, so the extended proposal's `:item-sheets`/`:item-attention` receipt fields were not exercised; B5's THEN held in its selection clause, not in its receipt clause.* Re-run the
  four-number script on the extended diamond and its bag; and, with the
  provisional prior (`meta-outer-provisional-prior-v1.edn`) and the B1/B2
  tokens as the only candidate-conditioned inputs, rank the 103 cones and
  compare with the live selector's ranking of the same run. *Accept:*
  both rankings on record with the Kendall distance between them; the
  ablation rule of `E-outer-loop-improvement` honoured (each new signal
  removable by a flag, and the ranking with it removed recorded).
- [ ] **B5. The restated pattern.** *Draft flexiarg written at the end of `PROPOSAL-outer-diamond-extended-2026-10-06.md`; promotion is Joe's.* `meta/select-the-meta-item` as a
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
- [x] **C2. A meaningful chain-versus-cascade comparison.** *Closed by argument (note §5c): over fixed units every valid support is a sub-relation of the token relation, so there are never two cascades over one unit set to compare — a chain is the relation itself, a lossy path in it, or forged; the question is about formation or about different unit sets. Lean obligation discharged: mathlib4 `80cb146f0e` — `tokenRelated`/`tokenRelation`, `valid_support_tokenRelated` (every valid support lies inside the token relation with its token sets determined), `diamondSupport_is_tokenRelation`, `chain_step_not_tokenRelated`; `lake build` 584 jobs, success.* The four-number
  measurement compared one cascade with three non-cascades. Find or
  construct a real four-unit example in which a token-carrying chain and a
  token-carrying non-chain both exist over the same units (candidates: the
  tactical cascades of `NOTE-g-over-head-cascades-2026-09-30.md`), and
  score both with the ordering term in its own column. *Accept:* both
  receipts `valid`; the two G values and the two ordering terms on record.
- [x] **C3. Charge the other off-diagonal.** *Proposal written: note §5b (futon2 `0ec367ad6`); ruled default (a) under R8; measured (codex-33, futon2 `146ec2675`, `REPORT-consumption-four-numbers-2026-10-06.md`, reviewed claude-2: no `src/` edits, controls reproduced, every prediction matched). Under rule (a) the diamond and both chains give the same consumption series `[0 1 4 4]`, the bag and the extended bag are refused at τ=1 (their roots' typed needs are not held), the house of alcoves gives the diamond's series padded, and the extended shape equals its chain `[0 1 6 6 6 6 6]`. Under today's fired-pattern rule the house of alcoves scores G 1.007 against the diamond's 1.280 (lower is better; horizon 5 vs 4), so the current C rewards adding a pattern nobody needs — the Salingaros point, measured. G under (a) is typed `:unavailable`: `score-arranged` builds `:progress-tokens` from `:pattern-done` tokens (cascade_shape_g.clj:250-255) and the rollout state is a held-token set with no consumption provenance, so the rule cannot be fed as data; the report carries the unapplied `src/` diff (an outcome function on predictive states) as the proposal for the scorer's owner.* A proposal, not code: how
  `¬IF ∧ THEN` (the token emitted with no consuming want; the house of
  alcoves) enters the score — via v2's rule that an unconsumed token earns
  nothing — with the bag measurement as the test case it must fail.

## Rulings — Joe's items (written 2026-10-06 00:20Z at Joe's request)

**Joe, 2026-10-06 ~04:50Z: "What if instead of 'rulings' we follow your
suggestions as a 'default setting' and sort out the tuning and
improvements later?"** Taken as the ruling on all ten: each
recommendation below is the default setting, in force from this commit;
each `→ Ruling:` line says so; tuning is a later pass and any default can
be revisited by name (R1…R10). Items were written as: Each item says what is being
decided, what the record shows, the options, and the owner's
recommendation. Joe records the ruling on the `→ Ruling:` line (a word is
enough); the owner then dispatches what follows. Items are independent
and can be ruled in any order or batch.

### R1. The R-cascade cut (A2 i)

*Decided:* which of the harness figure's 43 term edges carry state across
the tick boundary and so are not part of one click's formation.
*Record:* `REPORT-r-cascade-2026-10-05.md` §"Applied cut": 11 edges, each
with its term and a reason — cross-tick state `R16→R1` (u), `R16→R2`
(u, world), `R3→R1` (s-next); learned parameters `R4→{R1,R2,R3,R3a}`
(A, B); fit feedback `R7→R3` (Pi), `R8→R3` (F-pi); split-unit `R6→R4`
(pi, r), `R8→R6` (F-pi). Without the cut the 11 loop nodes are one
strongly connected component. *Options:* accept the 11 as tabled; strike
or add edges (name them). *Recommendation:* accept; the three reasons
follow from the term names and no edge was argued over.
→ Ruling: default — accept the 11 edges as tabled.

### R2. The R6 and R4 splits (A2 ii)

*Decided:* whether two drawn boxes are each two units.
*Record:* after the cut two 2-cycles remain, `R4⇄R7` (A out, rates back)
and `R6⇄R17` (pi out, E back); the script refuses the unsplit graph and,
with the splits as data, yields a 19-unit DAG. Proposed: R6 →
*candidates* (interp in; pi, r out) and *select* (G, E, F-pi, tau, pi in;
Q-pi, pi out); R4 → *model* (A, B; a source) and *rollout* (mu, interp, T,
pi, r, rates in; Q-o-pi, A out). *Options:* accept both; accept R6 only;
treat R4-model not as a unit but as the click's initial have (then R4 is
not split, its parameters are inputs). *Recommendation:* accept the R6
split; take the third option for R4 — `A`, `B` are read, not produced,
within a click, and the figure's own "Forward model" box is the rollout.
→ Ruling: default — split R6 into candidates/select; R4 is not split: `A`, `B` are the click's initial have (R4-model is not a unit), the unit is the rollout.

### R3. Meet direction (A2 iii) — owner's Lean modules disagree

*Decided:* which way `⊓` points in the Lean, and hence what "the
semilattice" means in every note.
*Record:* `CascadeOrder.IsMeet r a b m` has `m` reaching `a` and `b` —
the nearest common *origin* in descent, so `hasMeets` = every pair shares
a root (Q1, one cascade per problem). `ConstructionReceipt.MeetWitness`
has paths from the operands *to* the meet — a common *descendant*. The
outer diamond example (mathlib4 `e5fa4352a5`) and the note's "minimise is
the meet of fill and injury" follow the receipt's direction; in
Alexander's order (larger above) that is the join. codex-33 refused to
forge reversed paths and left the R-cascade receipt's `:meets` empty.
*Options:* (a) `CascadeOrder`'s direction is right; amend `MeetWitness`,
re-prove the diamond with `observe` as the meet of `fill` and `injury`;
(b) the receipt's direction is right; amend `CascadeOrder` (and the
reading of Q1); (c) keep both as join and meet of one lattice, named so.
*Recommendation:* (a). Under it the R-cascade's five sources (R13 R14 R19
R2 R4-model) are a defect in a measurable sense — 63 pairs with no common
origin — and R4 is the mend.
→ Ruling: default — (a): `CascadeOrder`'s direction; `MeetWitness` amended mathlib4 `7f497d3bd1`, diamond re-proved with `observe` as the meet of `fill` and `injury`.

### R4. A root unit for the R-cascade (A2 iv)

*Decided:* whether the click's input state is a unit.
*Record:* the cut severs exactly the edges that would make one: T (R13),
τ (R14), C (R19), o (R2), A/B (R4-model) are what the previous click left.
Adding one unit *click-input* that produces those five tokens gives every
pair a common origin under R3(a) and makes the stage columns' "PERCEIVE
first" true of one unit. *Options:* add it; leave five sources and read
them as the click's haves (as `ConstructionReceipt` already does for
haves). *Recommendation:* add it, so that the R-cascade and the outer
diamond have the same shape — one root, a frontier, one sink — and the
Lean receipt for A3 has one `MeetWitness` per incomparable pair.
→ Ruling: default — add the root unit `click-input` producing T, τ, C, o, A, B.

### R5. Operator load: preference term or support change (B3 i)

*Decided:* how the feasibility reading enters selection.
*Record:* B2 — attributable commit history exists for 4 of 107 items (all
machine-only); the text markers (`needs Joe`, `HIT`, `🈸`, …) exist for 14
documents, 61 lines; B1d — register and its two trajectories exist for
every in-field item. *Options:* (a) a dispreferred outcome
`:operator-demand` conditioned on the sheet (the proposal as written);
(b) an arm like `injury` that excludes operator-bound items from overnight
support; (c) both, with (b) guarded on a marker threshold.
*Recommendation:* (a). Excluding on an absence would empty the field
today; a preference can disprefer mildly and sharpen as history
accumulates under the hook.
→ Ruling: default — (a) preference term: dispreferred outcome `:operator-demand` conditioned on the item sheet.

### R6. Library coupling: recorded or scored (B3 ii)

*Decided:* whether "shares patterns" (`@how` generalised) affects G.
*Record:* B1 — the live selector's occurrence count is ρ 0.896 with an
item's applied-pattern count, so the current click scores coupling by
accident; B1c — coupling is uncorrelated with where attention goes
(ρ −0.207 with dispatches). *Options:* (a) `:item-coupling` on the
receipt only, not in `minimise`'s needs (the proposal); (b) scored, as a
term Joe names; (c) dropped from the cascade. *Recommendation:* (a) —
visible, so that the pattern-graph diff and library work can read it,
and not steering selection, so that the last click's choice is not
reproduced on purpose.
→ Ruling: default — (a) `:item-coupling` recorded on the receipt, not in `minimise`'s needs.

### R7. The restated pattern (B5)

*Decided:* whether `meta/select-the-meta-item` goes into
`futon3/library/meta/` beside the four it is made of.
*Record:* draft flexiarg at the end of
`PROPOSAL-outer-diamond-extended-2026-10-06.md`; its HOWEVER is ordinary
work against self-repair and documenting against doing.
*Options:* promote as drafted; promote after B4 part 2 shows the
extension changes a real ranking; do not promote (the diamond is enough).
*Recommendation:* the middle — a pattern claiming a THEN should carry one
run where the THEN held.
→ Ruling: default — promote only after B4 part 2 shows one run where the THEN held.

### R8. Inert tokens: no credit or a cost (C3)

*Decided:* how the other off-diagonal (`¬IF ∧ THEN`, the house of alcoves,
the token emitted with no consumer) enters the score.
*Record:* note §5b; the bag of §5 (G 0.393 under per-step C) is the test
case the rule must fail. *Options:* (a) no credit — a token counts toward
progress only once a unit or the want consumes it; (b) a positive cost per
inert token (Salingaros's complexity reading), which needs its own
measurement; (c) leave the per-step C as is and rely on construction
never offering the bag. *Recommendation:* (a) now; (b) only if a run
shows repeated patterns being rewarded under (a).
→ Ruling: default — (a) no credit: a token counts toward progress only once consumed.

### R9. Where the quality order enters G (CASCADE-SPEC-v3 §4, first decision)

*Decided:* the route by which Goguen's order on blends reaches the score.
*Record:* two positions on record in the v3 draft. c17: the order enters
F as fit, per occurrence, and the number of maximal blends the order
cannot rank enters ambiguity beside v2's linear-extension term. c10: the
order is what construction searches over and reaches G only through the
policy family it admits or an explicit prior; later evidence about fit
enters F, the categorical order itself does not. *Recommendation:* c10
for the order, c17 for the count — construction searches the order
(c10), and when it returns several maximal blends it cannot separate,
that number is a possibility count and belongs with the ordering term
(c17's second half). The "fit in F per occurrence" part is already how
`fit-evidence` works in `cascade_shape_g` and need not be re-decided.
→ Ruling: default — c10 for the order (construction searches it), c17's count of unrankable maximal blends in ambiguity beside the ordering term; fit-in-F stays as `fit-evidence` already has it.

### R10. Is the house/boat order the order for library patterns (v3 §4, second decision)

*Decided:* whether content / axioms / inclusiveness, Goguen's three
criteria for ranking blends of houseboat and boathouse, is the order to
apply to blends of library patterns, or whether the library needs its own.
*Record:* v3 §4 last bullet, open; §7 lists "which parts of a library
pattern are its inputs and ground" as unsettled, and the order cannot be
applied before that is. *Options:* adopt Goguen's three as a working
order; state a library order (e.g. tokens kept, BECAUSE preserved,
HOWEVER still held); defer until §7's first item is settled.
*Recommendation:* defer, with the §2a reading (IF = ground, HOWEVER =
inputs, THEN = blend) as the working answer to §7's first item, and
revisit when A5 has read a dozen R-units that way.
→ Ruling: default — deferred; §2a's reading (IF = ground, HOWEVER = inputs, THEN = blend) is the working answer to v3 §7's first item; revisit after A5. *A5 result (2026-10-06): holds for the three units whose tokens are the pattern's products; bends for five where the receipt token is a wire named for the Lean DAG; the revisit is a token-vocabulary question (name tokens after what the pattern produces), not a change to the reading.*

### What each ruling releases

| ruling | releases |
|---|---|
| R1–R4 | A3 (Lean receipt for the R-cascade), A4 (drawing), A5 (R-units as blend squares) |
| R3 | re-proof of the outer diamond's `MeetWitness`; the seven-unit receipt |
| R5, R6 | B4 part 2 (rank the 103 cones with the readings; compare with the live selector; ablation flags) |
| R7 | nothing until B4 part 2; then the library edit (Joe's) |
| R8 | measured 2026-10-06 (`REPORT-consumption-four-numbers-2026-10-06.md`); remaining: the scorer change (outcome function over predictive states with consumption provenance), owner of futon2 `src/` |
| R9, R10 | CASCADE-SPEC-v3 moves from draft to v3; its Lean obligations join the list |

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
rulings are the ten items of the Rulings section above; each is recorded
on its `→ Ruling:` line when given, and the owner dispatches what the
table under them says it releases.
