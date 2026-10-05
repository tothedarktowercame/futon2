# NOTE — the outer-loop cascade, read three ways (2026-10-05)

claude-2, on Joe's instruction of 2026-10-05: bring the 3/2-category
story, the wants-token story and claude-17's small-causal-world story
together on the one cascade we actually have, the outer-loop policy that
selects META items, rather than on an invented example. This is a reading
of what exists; it changes no code.

## 1. What exists: two outer loops, one of them running

**(a) The contract.** `futon3/library/meta/meta-outer-policy-cascade.edn`
(`:meta/outer-policy-cascade-v1`, 2026-10-02) names four patterns from
`futon3/library/meta/` and four precedence edges:

```
observe-the-meta-field ──→ fill-meta-policy-slots ──→ minimise-g-over-filled-meta-policies
          │                                                      ↑
          └──────────→ injury-routes-to-self-heal ───────────────┘
```

A diamond. `fill` and `injury` are incomparable; their meet is `minimise`;
their join is `observe`. `:precedence` is the edge set, not a list, so the
contract already records the semilattice. It has two linear extensions
(`O F I M`, `O I F M`); neither is in the file. Six typed slots
(`task-kind target next-move resource-envelope evidence-channel
stopping-rule`); selection `argmin-G` over filled candidates;
`:boundary {:produces :selected-meta-policy :then
:construct-fresh-task-local-tactical-cascade}`. Its pure evaluator is
`futon2/src/futon2/aif/meta_outer_policy.clj`; its slot constructor is
`meta_policy_constructor.clj`. **Nothing in the click calls either**
(grep over `futon2/src`: no caller outside the `meta_*` namespaces).
`meta-outer-predictive-authority-decision-v1.edn` records the reason: the
candidate-conditioned generative model is `:refused-insufficient-…evidence`;
only the operator's provisional prior exists (`meta-outer-provisional-prior-v1.edn`,
Joe 2026-10-02).

**(b) The live selector.** `meta_live_outer_selector.clj` →
`meta_pipeline_selector/select`, which is what `full_loop_runner` loads.
It takes the Cascade Live graph as the field (Joe 2026-09-25: "the outer
cascade should be based on Cascade Live"), excludes what is off the map,
held, or in standing conflict, and ranks the rest by pairwise sum of
"task-state cost channels"; `:epistemic-value-nats 0.0`; no draw.

Run `2026-10-05-c9d25d6a`, outer stage, from its record:

| | count |
|---|---:|
| items in the field | 676 |
| excluded `:pipeline/not-on-current-map` | 506 |
| excluded repair-ticket reasons (root missing / attached / not current) | 52 |
| excluded ownership (held / ambiguous owner) | 9 |
| excluded standing (conflict / document source unavailable) | 2 |
| **candidates ranked** | **107** |
| with `:pipeline-structural-centrality-cost` | 107 |
| with `:pipeline-freshness-cost` | 79 |
| with `:declared-priority-cost` | 0 |

Selected `M-interim-director-proxy-metric-inventory`, reason
`:minimum-pairwise-task-state-G`; its one scored channel is centrality
cost `1/(1+33) = 0.0294`. So the META item was chosen by the lowest
centrality cost among 107, with freshness as a tie-breaker where present.
That is a ranking, not G = risk + ambiguity − information, and it is not
the diamond: of the diamond's four patterns the click performed the first
in part (a census with exclusions, no injury observation: the record
contains no `:injury-observation` and no `meta-outer-policy-receipt`),
and none of the other three.

## 2. The blend reading (Goguen 3/2-pushouts)

Each of the four patterns is a blend square, with its generic space and
output read off the pattern's own text (`CASCADE-SPEC-v3-draft §2`):

| unit | I₁ | I₂ | G (shared) | B (the blend) | emergent in B |
|---|---|---|---|---|---|
| observe | the M/E/T/A enumeration ("what exists") | interoception ("the machine's own condition") | the current state the policy is selected under | one source-pinned observation: census + exclusions + injury | "absence of injury evidence is an explicit absence" — neither input has a typed absence |
| fill | the reusable skeleton with typed holes | current source-pinned evidence (observe's B) | the six slot names | a filled policy instance with its slot-to-source receipt | the receipt *is the identity* of the policy; neither skeleton nor evidence has an identity for an act |
| injury | a typed injury observation (from observe's B) | approved A items with declared repair targets | the capability name (`injured-capability` = `repairs-capability`) | a guarded support set with its re-arm observation | the re-arm condition; a support change that is reversible by observation |
| minimise | the admitted, filled family (fill's and injury's B) | the generative model: outcome vocabulary + preferences | the common outcome vocabulary | the selection receipt: G terms, chosen, nearest alternative | the *reason* for the chosen task (nearest alternative + decomposition), which no candidate carries alone |

Pasting (Goguen 2006 Prop 8/9, Thm 13): observe's B is an input space of
both fill and injury — one object, two gluing maps, which is why those two
are incomparable and sit in one frontier; fill's B and injury's B are the
two inputs of minimise, which is their meet. The whole is the colimit of a
connected four-square diagram. This is exactly the diamond of §1(a),
derived from what the squares share rather than declared.

What Joe's observation ("minimising G in the outer loop is parametric in
the selection of META item") says in these terms: in the tactical loop the
*diagram* is constructed per problem — which squares, glued where — and G
orders diagrams. In the outer loop the diagram is fixed, and G orders the
*cones* over one square (minimise's): each filled candidate is a consistent
cone over (family, model, vocabulary), the 3/2-pushout is the one the
quality order picks, and Goguen's "two different 3/2-pushouts need not be
isomorphic" is the statement that two META items are different blends of
the same inputs — houseboat and boathouse. The `target` slot is the
parameter; the squares do not move.

When would the diagram have to move? Exactly when the fixed diagram has no
consistent cone, or only a degenerate one:

- a required slot cannot be filled from HEAD for a candidate. Then a
  *reading* square must be pasted in before `fill` — Joe's 2026-09-25
  step kinds (read-criteria / ask-interpretation / defer), with epistemic
  value when the fact is absent. That is a structural change, and it is
  the "live, can get unstuck" property, not an exception to the cascade.
  *Measured (codex-33, `REPORT-meta-slot-census-2026-10-05.md`, futon2
  `597a03288`): of the 107 items c9d25d6a ranked, 105 are in the HEAD
  field observation (484 rows), 103 fill all six slots, 2 fill four
  (`E-cascade-real`, `M-joe-told-me-about-futon`:
  `:no-current-false-checkable-want` — the latter has no checkbox and no
  criterion in 1,938 lines), 2 fill none (`E-kimi-task-28`, `-70`: not in
  the field). So the reading square is forced for 4 of 107, not for most.
  Read the "six" with care: `task-kind` and `target` are the row's
  identity, `resource-envelope` is the caller's constant, and discovery
  sets `next-move :advance` and `stopping-rule :grounded-progress` for
  every ordinary row it admits; the one slot that carries information
  from the item is `evidence-channel`, the locator of the first current
  false checkable want. "Fills all six" therefore means "is in the field
  and has an unchecked want at HEAD".* The consequence for §2: on today's
  field the diamond has a non-degenerate cone family of 103 near-identical
  policies differing in `target` and one locator, and the quality order
  over them is what is missing (the predictive authority decision of
  2026-10-02 refuses candidate-conditioned prediction). Structure is not
  the bottleneck on this field; the order is;
- the admitted family is a singleton (`:singleton-policy :typed-bypass`) —
  minimise's square degenerates to an identity;
- injury with no matching A item (`:no-matching-algorithm → :abstain`) —
  injury's square has inputs but no blend.

So the answer to "do we really need to change it structurally depending
on circumstances" is: the four squares are fixed; a fifth (reading) square
is pasted in under a stated absence, and which absence it is can be read
from the record. The circumstances are enumerable at HEAD: for each of the
107 on-map items, which of the six slots `meta_policy_constructor/construct`
can fill. That census is the first thing to compute, and it needs no new
machinery.

## 2a. One pattern is already a diamond (Joe, 2026-10-05, after §1–6)

Joe's addition: the three stories are readable inside a single pattern,
and a pattern is itself a tiny diamond; a cascade composes that inner
structure with the outer one. Read off the flexiarg form:

| flexiarg part | blend | tokens | causal (claude-17) |
|---|---|---|---|
| IF / context | the generic space G: the situation in which both forces act | the tokens the pattern *accepts* | `k-if` |
| HOWEVER | the two input spaces I₁, I₂ — the forces in conflict | — | the two inputs of the mechanism |
| THEN | the blend B; the emergent structure is **the held tension** — a configuration that keeps both forces and belongs to neither input (an alcove is neither the family room nor a private room) | the token the pattern *emits* (something is, or is not, an ALCOVE) | `k-then` |
| BECAUSE | why the square commutes: the mechanism by which B satisfies both maps from G | — | the pattern's own structural equation, which the generic `k-unmet = k-if ∧ ¬k-then` abstracts |

Diamond: IF at the top, the two forces incomparable, THEN their meet.
So a pattern is a blend square *and* a four-node semilattice, and a
cascade is diamonds pasted along tokens — the inner structure of each
unit and the outer structure of §2 are the same shape at two grains.

**Too many alcoves** (Alexander/Salingaros): a house of alcoves emits the
ALCOVE token where no tension calls for it. In claude-17's terms that is
the *other* off-diagonal: `unmet = IF ∧ ¬THEN` is a problem unaddressed;
`¬IF ∧ THEN` is a solution without its problem. A per-step C that rewards
tokens held credits the second (it is the bag problem of §5 at the grain
of one pattern); the token story handles it the way v2 handles missing
edges — an emitted token with no consuming want earns nothing. Both
off-diagonals should cost, and only one does today.

**A cascade that reduces to a diamond can be restated as a pattern.** The
outer-loop diamond of §1(a) is the example: IF = `observe` (the field and
the machine's own condition); HOWEVER = ordinary work against self-repair,
which is literally `injury`'s HOWEVER ("merely adding a large penalty to
M/E/T leaves unsafe acts in support … treating every failure as injury
creates a self-repair dark room"); THEN = `minimise` (argmin G over the
admitted filled family, with the nearest alternative as the reason);
BECAUSE = G is defined over policies with predicted consequences. That is
a complete flexiarg, `meta/select-the-meta-item`, and the four units are
its parts. Not written into the library here; whether it belongs there is
Joe's.

## 3. The token reading (wants, `ConstructionReceipt.lean`)

Each square's blend B is a token; a precedence edge carries
`produces(source) ∩ needs(target)`, non-empty, exactly `edgeValid`:

| edge | tokens |
|---|---|
| observe → fill | `{field-observation}` |
| observe → injury | `{field-observation, injury-observation}` |
| fill → minimise | `{filled-candidates, typed-exclusions}` |
| injury → minimise | `{admitted-support, rearm-slot}` |

meets: `(fill, injury) ⊓ = minimise`, with the two one-edge paths. The
outer loop's want is `selected-meta-policy` (the contract's
`:boundary :produces`); the inner loop's construction begins from that
token. The gluing object of §2 and the token of this table are the same
thing seen from the two sides, which is the bridge CASCADE-SPEC-v3 §3
left as an obligation ("shared object = gluing object until the meet laws
are proved"). On this cascade the bridge is concrete: the object shared by
`observe`'s square and `fill`'s square *is* the field observation, and it
is what the edge carries.

This gives the first non-toy instance for `ConstructionReceipt.lean`
(today's example is two units, P produces q, Q needs q): four units, four
edges, one meet witness, `valid = true`; and two further receipts, the
linear extensions `O F I M` and `O I F M` with a forged `fill → injury`
(or `injury → fill`) edge, which `valid` must reject because the token set
is empty. That rejection is the formal content of "the recipe is a parse,
not the formation": the chain adds an edge no token supports.

## 4. The causal reading (claude-17, `daxiang_live.clj` §7)

claude-17's compile is generic: `k-unmet = k-if ∧ ¬k-then`, glued by
shared names. For the outer cascade the link theories run *down* the
diamond (a square's IF needs its predecessors' THEN), where the
`@why`-cone of §7 runs up with OR; both are `ot/glue`-able. Evaluated
against the c9d25d6a record, with the observables read as in §1(b):

| pattern | IF | THEN | unmet? |
|---|---|---|---|
| observe | M/E/T/A items and runtime evidence exist: yes | one source-pinned observation with census, exclusions **and typed injury**: census and exclusions yes, injury absent | yes (partial) |
| fill | a skeleton names the six slots: yes (the edn) | six slots bound per candidate: no — the live selector binds none | yes |
| injury | an injury observation names a capability: no observation was made | — | not applicable (IF unknown, not false) |
| minimise | predicted outcomes per filled policy: no | argmin G with receipt: no — a pairwise channel sum was taken instead | yes |

So beside the run's open ports one would show: the outer cascade's
`fill` and `minimise` unmet, `observe` partly met, `injury` unobserved.
That is claude-17's §9 item 3 ("run the cascade per turn … the unmet
patterns shown beside its open ports") applied to a click instead of an
operator turn, with no change to his compiler. The counterfactual "had
`fill` fired, would `minimise` have chosen differently from the centrality
argmin" is the question the provisional prior exists to answer and
cannot yet (the predictive authority decision of 2026-10-02 refuses
candidate-conditioned prediction for want of registered-run evidence).

## 5. Chain, list, bag: what is measured and what is not

In the code the "list" and the "chain" are one object: the list kernel
fires one pattern per step in the given order. So the two measurements we
have are already cascade-versus-chain, both under a per-step preference:

- NOTE-g-over-head-cascades (2026-09-30): three patterns, same order,
  chain vs one edge first→last: G 3.074890 vs 1.414694;
- construction-time G on c9d25d6a's shape (2026-10-05): list 10.715 vs
  frontier 9.465.

Neither includes formation cost, and under selection's terminal-only C the
two kernels give equal G (1.0498…, measured by codex-10). Not measured at
all: the bag (no edges). Under the frontier kernel a bag co-applies
everything at step one and would score best on a per-step C — which is why
CASCADE-SPEC-v2's rule, "absence of a precedence edge is not an assumption
of simultaneous success", must enter the score before any bag comparison
is meaningful.

Formation cost as an accounting statement, not yet a measurement:
forming the chain = forming the cascade + choosing one linear extension;
the choice adds log₂(#extensions) bits (v2's ordering-ambiguity term; one
bit for the diamond) and no token. That is Joe's "recipe is strictly
additional work beyond the formation" and his "a proof is not a tree",
made countable. The experiment that would turn it into a measurement is
small and runs on this real cascade: score the diamond, its two linear
extensions and its bag under construction's per-step C with the ordering
term included, and report the four numbers.

## 5a. Refinement: the object is alive (Joe, 2026-10-05)

Joe's second addition, the informal-proof story: the War Machine itself
went through many iterations, each adding structure and more explicit
requirements; some steps are *additional requirements*, some are *better
structure* — Alexander's street corner still has its newsstand,
greengrocer and crosswalk, and now also a hydrant and a manhole; and R.
Crumb's *A Short History of America* (1979), the one crossroads drawn
panel by panel over a century: the object is refined as detail is added
and is also a living object that changes.

My reading in the terms above. The two kinds of step are two moves on the
diagram. An additional requirement pastes in a new square (a new want
token, a new input space); better structure replaces a blend by one higher
in Goguen's quality order over the *same* inputs — the 3/2-pushout is the
maximum among consistent cones, and refinement is movement toward it. A
living object is one whose inputs also change, so the maximum moves. This
is the same distinction as "a proof is not a tree": the sequence of
panels is the formation; any one panel is a parse. For the WM's own
record, a first classification: 09-15 → v2 (three relations, ordering
ambiguity) is better structure over the same wants; v2 → v3 (the blend
square under each unit) is better structure again; Requirements Q1–Q10
and the pattern-graph diff were additional requirements. Whether that
classification is right is checkable against the spec history, and the
check belongs after §6's items.

## 6. Next checks, in order, each one dispatchable alone

1. **Slot census at HEAD** (discovery, no code): for the 107 on-map items
   of c9d25d6a, run `meta_policy_constructor/construct` on the field
   observation and tabulate fillable slots per item. Output: which absence
   would force a reading square, and for how many items. Settles whether
   the diamond ever has a non-degenerate cone on today's field.
   *Done: codex-33, futon2 `597a03288`, report
   `REPORT-meta-slot-census-2026-10-05.md`, script
   `scripts/meta_slot_census.clj`; numbers and their reading in §2.
   Reviewed by claude-2: ids taken from the record's `:ranking`; per-slot
   rows sum to 107; refusals typed; spot-checked
   `M-interim-director-proxy-metric-inventory` (four unchecked
   checkboxes at HEAD, lines 2427–2430) and `M-joe-told-me-about-futon`
   (none).*
2. **Lean receipt** (small): the diamond and its two forged chains in
   `ConstructionReceipt.lean`; `valid` true / false / false by
   `native_decide`. *Done: mathlib4 `e5fa4352a5` — `diamondReceipt_valid`,
   `diamondReceiptInjuryFirst_valid` (the other linear extension over the
   same support), `chainReceipt_invalid`, `chainReceiptForgedToken_invalid`;
   `lake build` 584 jobs, success.*
3. **Per-click unmet table** (small, claude-17's compiler as is): compile
   the four meta patterns with downward link theories and evaluate against
   a run record; output §4's table from the record rather than by hand.
4. **Four-number experiment** of §5 on the diamond.
5. Only after 1–4: whether the live selector is replaced by the diamond
   with a reading square, which is an operating change and Joe's call.

Not in this note: the daxiang_live page is not touched; CASCADE-SPEC-v3's
two open decisions stand as reported.
