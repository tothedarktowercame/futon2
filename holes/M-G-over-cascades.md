# Mission: G is over Cascades, not Patterns (M-G-over-cascades)

**Date:** 2026-06-22
**Status:** HEAD + IDENTIFY + MAP done; now in **exploratory DERIVE**. Per Joe (2026-06-23): slice-1 (reviewed PASS) and slice-2a are *exploratory probes*, **not** INSTANTIATE — only from their findings will we commit to a design, ARGUE it, VERIFY, then INSTANTIATE the real artifact. slice-2a probe in flight (claude-1). Continuation of [[M-wm-policies]].
**Owner:** Joe + claude-2 (build/review); claude-1 (AIF theory, via whistle coordination).
**Repos:** futon2 (`futon2.aif.rollout` — the rollout engine), futon3a (`meme.gates`; the lab experiments under
  `holes/labs/M-memes-arrows/`), futon3/library (the pattern language), futon6 (`holes/closure-folds.edn` — ground truth).
**Cross-ref:** M-wm-policies (the parent; this *operationalises* its "over policies = distributions over CASCADES"),
  E-fold-engine (futon3a/holes/missions — the excursion that surfaced this), F-wm-piloted-2026-06-12 §362 (the ratified
  definition), the wm-flight anatomy (futon6/holes/anatomy-of-a-wm-flight.md).

---

## HEAD (operator intake)

*Provenance: Joe, emacs-repl, 2026-06-23, on reading the slice-2 rationale; this section preserves his
live framing before IDENTIFY hardens it. Lifecycle: futon4/holes/mission-lifecycle.md.*

**Operator-voice anchor.** "§1 Thesis is indeed the key gap, because previously we've defined `G` over an
argmax of next actions, or more recently mockup-`G` over patterns. **`G` over cascades needs cascades to be
*defined*.** And once we've defined what we mean by cascades, we have to define what we mean by `G` over them
— which may involve rigorously understanding how we are going to *use* them. We're not in such bad shape here."

**What's already felt to be true (the raw material exists).** Cascades are not built from nothing:

- **Patterns exist** in `futon3/library/` and **link to each other** (descent / co_app phylogeny) **and to
  missions** (the pattern↔mission adjacency graph), and **all of that lives in an embedding**. That is a real,
  populated seed graph — the thing slice-1's move-prior and phylogeny bonus already read.
- **We've run 100s of missions**, so the usage signal for *defining `G`* is already on disk: we can look not
  only at **which** patterns a mission used (the adjacency graph) but **how** it used them. `G` over cascades
  can be grounded in observed mission usage, not postulated.

**The interesting part (seed ≠ space).** The existing material only *seeds* the possible cascades — it does not
bound them. Two open dimensions:

1. **The pattern set is extensible** — we just did this, planting patterns for the cascade→wiring fold
   (`structure-learned-patterns/cascade-fold-{compile,search,family}.flexiarg`).
2. **The ways of connecting patterns are endless.** Right now, in a suitably **Alexandrian** way, we use only
   the *existing* interconnections — direct pattern↔pattern, or through missions. **But those links are
   editable.** Who's to say we shouldn't revise them — add **"causal" connections** or other **typed links**?
   Maybe we should. A cascade is a path through a graph we are also allowed to *rewrite*.

**Anti-glibness discipline.** Don't let "`G` over cascades" stay a slogan: a cascade must be a *concretely
constructed object over the real seed graph* (patterns + their actual links + mission usage), and `G` must be
*defined by how cascades are used*, evaluated by **discharge** on held-out holes (§5 success criterion) — not
by a hand-picked link set or a metric tuned to the demo. If we add typed links, their warrant is that they
*improve discharge-grounded recovery*, measured, not asserted.

**Working-economy position.** This underwrites [[M-wm-policies]] (gives its "select over distributions of
cascades" a buildable object) and the fold engine (E-fold-engine). It is underwritten by the live seed graph
(futon3/library + the pattern↔mission adjacency + the embedding) and by `closure-folds.edn` as discharge
ground truth.

**Carried-forward tensions** (for IDENTIFY/DERIVE to pick up, not bury):

- **T1 — Define *cascade*.** A cascade = an admissible composition over the seed graph (patterns, their links,
  mission usage). Open: is the link set *fixed-as-found* (Alexandrian, use what's there) or *revisable* (mint
  causal/typed links)? Default to fixed-as-found for slice-2; treat link-editing as its own gated experiment
  whose warrant is measured discharge lift.
- **T2 — Define *`G` over cascades*.** Ground it in **how missions actually use patterns**, mined from the
  100s of completed missions (which patterns × how). Open: what exactly we read off mission usage and how it
  becomes a scoreable `G(π)` — this is the rigorous "how we are going to use them" Joe flags.

---

## IDENTIFY — theoretical anchoring: Ostrom / IAD (prior art)

*Flagged by Joe, 2026-06-23: we've been framing this purely from CS; but Elinor Ostrom's Institutional
Analysis & Development (IAD) is built out of **patterns** as its blocks (she says so), which suggests a
**cascade ≈ an Ostromian "institutional design."** Read first: `~/Downloads/corneli2016institutional.pdf`
(Corneli 2016, "An institutional approach to computational social creativity" — Joe's own ICCC paper, which
already transposes Ostrom's 8 design principles into computing terms via the 4Ps + the notion of "tests").*

The mapping is load-bearing, not decorative — it gives T1 and T2 a vocabulary and a warrant:

- **What a cascade IS (T1), in Ostrom's terms.** Ostrom's *institution* = "the shared concepts used by humans
  in repetitive situations organized by **rules, norms, and strategies**" (Ostrom 2010). **Patterns are the
  shared concepts; a cascade is their organization into a working arrangement = an institutional design.** So
  a cascade isn't just a path through the seed graph — it's a *governed* composition (the rules/norms/strategies
  are the typed links).

- **The ADICO grammar = our typed-edge grammar + the admissibility filter.** Ostrom defines a rule as
  "**ATTRIBUTES** of participants who are OBLIGED, FORBIDDEN, or **PERMITTED to ACT** (or affect an outcome)
  under specified **CONDITIONS**, OR ELSE" (the ADICO syntax: Attributes·Deontic·aIm·Conditions·Or-else). This
  is a ready-made grammar for the **typed links** T1's open horn asks for — and "PERMITTED to ACT under
  CONDITIONS" is *precisely* `admissible_step` (a move is permitted when its `consumes` are on the frontier).
  Our IF/HOWEVER/THEN is the informal twin; ADICO is the typed one.

- **"Links are editable" = Ostrom Principle 3 + 8, not an ad-hoc move.** The HEAD's open question — should we
  revise links, add causal/typed ones? — is Ostrom's **collective-choice arrangement** (Principle 3: "most
  individuals affected by the operational rules can participate in modifying them") layered with **nested
  enterprises** (Principle 8: governance organized in multiple nested layers). So link-editing is the
  *collective-choice layer of the institution*, and the typed-link experiment (T1's gated second horn) inherits
  a principled warrant: a good institution is one that can revise its own rules. (Cf. Conway's Law in the paper:
  the cascade's structure will mirror the mission-organization that produced it.)

- **The 8 design principles are framed as TESTS → that is discharge.** Joe's Table 2 transposes Ostrom's
  principles into computational *tests* (assertions / contracts / TDD). That is exactly our **discharge** commit
  (§3): "good institutional design" supplies adequacy criteria, and they're already cast as runnable tests. This
  is the bridge from Ostrom's normative principles to our non-gameable commit signal.

- **IAD's action situation ≈ the rollout; SPECS ≈ our three definitions.** IAD frames every situation as
  **context → action → outcome**; the paper aligns this with SPECS as Definition / Criteria / Evaluation. That
  triple maps onto **define-cascade (T1) / define-G (T2) / discharge-evaluate (§5)** — a structural check that
  our three open pieces are the right three.

**Deep-research follow-up (deferred, Joe's call):** a deeper Ostrom pass — IAD's seven clusters of
action-situation variables (participants · positions · potential outcomes · action-outcome linkages ·
participant control · information · costs/benefits; Ostrom 2009 p.14), the full ADICO grammar, and the 8
principles as a design-adequacy checklist — to harden T2's "how `G(π)` is actually scored." Start point is the
paper above; broaden via `/deep-research` if we want primary Ostrom sources. **Not yet run.**

---

## 1. The thesis

**Expected free energy `G` is a property of CASCADES (composed policies), not of individual patterns.** A cascade
`π` is an ordered set of pattern-moves folded into a wiring; `G(π)` is the path integral over that composition.
Scoring or committing on a *single pattern* is the degenerate length-1 case, and it discards exactly the
information that matters.

This is not new — it is M-wm-policies' own ratified definition ("over policies = distributions over cascades",
F-wm-piloted §362). This note records why the fold work *regressed below* that level, why it matters, and the
rationale for the next build.

## 2. Why it matters — the same error, three times

The cascade→sorry→wiring work (E-fold-engine) kept committing to an isolated atom where the meaningful object is
the composite whole. The identical correction has now appeared at three levels:

| level | atom (the error) | composite (the fix) |
|---|---|---|
| rollout | single next-step | multi-step policy (M-wm-policies) |
| commit | commit-on-prior (retrieval-peak) | commit-on-discharge (the gate) |
| **unit of account** | **`G` per pattern** | **`G` per cascade** (this note) |

**Why per-pattern scoring fails — the flat-field explanation.** Discharge is **non-additive**: a wiring discharges
as a whole composition — terminals chain end-to-end, types compose across the wiring, the `THEN`s actually connect.
Per-pattern scoring assumes value *factorises* into a sum of per-pattern scores, which **marginalises out the
interaction terms that carry the signal**. That *is* the flat field we measured: a pattern like
`route-exploration-and-pivot` is generic in isolation because *alone it composes nothing*. The metric was never
weak; we were asking it about the wrong object.

## 3. The method (AIF-warranted; claude-1-ratified)

The general method is a four-job stack, and **only one job commits**:

1. **Retrieval similarity = the PRIOR** (a prior over *moves* — which pattern to add to a cascade-in-progress).
   Ranks/proposes; never commits.
2. **Admissibility typecheck = the FILTER** (answer-independent prune of non-composing moves). A hard prior.
3. **Discharge = the COMMIT criterion *and* the precision source** — does the folded wiring actually pass the gate /
   run / close the sorry. The only non-gameable signal; it commits *and* trains the prior.
4. **Combining-methods disagreement = the overfit DETECTOR** — locates the binding constraint, doesn't commit.

This is AlphaZero exactly: policy-net (retrieval) proposes → legal-move filter (typecheck) prunes → game result
(discharge) is the ground-truth value that commits *and trains the prior*. **You cannot make a static score do a
dynamic loop's job** — committing on retrieval is the same error as argmax-over-G, one level up.

## 4. Evidence so far (why this is grounded, not guessed)

- **Move-prior works (pattern level).** `discharge_experiment.py` over real closed holes (`closure-folds.edn`):
  retrieval recall@20 = **50% vs 1.75% random = 29×**. *And* the cosine-artifact that retrieval ranks #4/#12
  **fails to discharge** — proof retrieval is a prior, not a commit.
- **The honest AIF pass abstains on a flat field — correctly.** Soft cosine and a hardened endpoint cut + null
  both refuse to commit (gaps ≪ null margin); the apparatus declines to confabulate. (`alexandrian_aif.py`,
  `harden_pragmatic.py`.)
- **Structure-learning recovery is real but the metric is the wall.** Authoring the missing resolver patterns
  surfaced them #1 in the soft channel (3/5) but they still did not peak under the hard cut — so the binding
  constraint is discrimination, not the language (`structure_learn_test.py`).
- **Slice-1 cascade-rollout — built (claude-1) + reviewed (claude-2, real gate; numbers re-run).**
  (`cascade_rollout.py` + `cascade_rollout_test.py` 8/8 + `cascade_recovery_experiment.py`.)
  - cascade-recovery **25% vs 0.26% random = 95×** on the true `:used` SETS;
  - the cosine-artifact (rank 4/12) scores **0 discharge** at cascade level → rejected;
  - **honest edges:** "beats per-pattern (12.5%)" is **n=1** (one hole, head-sigil); the new chaining filter is
    unit-tested but did **zero work on real data** (recovered holes were all `|U|=1`); and the misses are
    **move-prior misses** — the true patterns rank **116–661** by retrieval.

**The standing conclusion:** the level-shift structure (cascade-as-unit + incremental admissibility) is sound and
tested, and the experiments *localise the remaining work to the move-prior* — exactly what the theory predicts
("the prior is weak until **discharge-trained**"). This is a specific, named constraint, not a flat-field mystery.

## 5. The rationale for continuing — slice-2

Slice-1's warrant for continuing is firm: the bottleneck is precisely the **move-prior** (true patterns ranked
116–661 by retrieval), and the AIF-warranted fix is dictated — *discharge-train the prior*, don't hand-tune a
cut. But slice-1's three honest edges (per-pattern beat n=1, chaining inert because recovered holes were
`|U|=1`, weak prior) are all artifacts of the **tiny** ground truth — `closure-folds.edn`, 15 records. The fix
is not a better metric over the same 15 folds; it is a **bigger, realer corpus of cascades-with-discharge.**

### 5a. Slice-2a — mine real mission usage (the primary path)

**Redesign (Joe, 2026-06-23):** run slice-2's concepts *in the course of mining how the 100s of completed
futonic missions have actually used patterns.* A completed mission **is** an Ostromian institutional design —
patterns organized by the lifecycle (rules), PSR/PUR (the collective-choice record of which pattern was chosen
and how it performed), and IF/HOWEVER/THEN/BECAUSE (strategies). And critically, **each mission carries a real
discharge label**: it reached COMPLETE, or it was RE-OPENED / BLOCKED / abandoned. That is the discharge-trained
move-prior's return channel (R2/R3) grounded in real outcomes, not the synthetic `want-coverage` proxy.

This is **Ostrom's own method, not just her vocabulary.** Ostrom derived her design principles by *empirically
studying which real commons-institutions succeeded.* Slice-2a is that move on our corpus: observe the missions
that discharged, extract the patterns-of-organization (the typed links / co-usage) that distinguish them from
the ones that didn't. So slice-2a doesn't merely *train* a prior — it **constructs T2's definition of `G(π)`**:
the expected discharge of a pattern-composition, estimated from observed institutional outcomes.

**The corpus already exists — the MAP is essentially pre-satisfied (Joe, 2026-06-23).** Formally-marked PSR/PUR
is thin, but nearly every mission cites patterns by name and that is already modelled. Three assembled artifacts
in `futon6/data/` carry everything slice-2a needs over **194 missions / 490 patterns**:

- **WHICH + the move-prior — `mission-pattern-scopes.edn`.** Per mission: `:applied [...]` = the patterns the
  mission actually applied (the citation graph, by name); `:try-candidates [{:pattern … :cos …}]` = the
  retrieval candidates *with cosine already computed* (the slice-1 move-prior, pre-baked). So WHICH and the
  untrained prior are both in hand; no PSR/PUR grep needed.
- **DISCHARGE label — `mission-wholeness.edn`** (Salingaros, `L = T·H` over the scope-tree centres). Per
  mission: `:class` (**87 alive / 43 mess / 44 pipeline / 20 stub**) + graded `:L`, `:T`, `:H`, `:pathology`.
  **This is the discharge signal Joe proposed: alive-vs-mess gives labelled "went-well vs didn't" examples.**
  Critically it is computed over scope-tree *structure*, **not** over the patterns — so scoring pattern-
  compositions against it is **non-circular**. Rendered as the EFE field: `futon6/data/mission-efe-field-embed.html`
  (best-of-class), with `ctrl-rand*/ctrl-shuf*` null-control variants already generated.
- **Phylogeny move-prior — `mission-phylogeny.edn`** (`:generativity-index` + 625 HGT edges): the structural
  `P(next | current)` slice-1's rollout already consumes as the phylogeny bonus.

**The core analysis (Ostrom's empirical method, now computable): contrast alive vs mess.** With labelled
positive/negative institutions in hand, ask *which patterns and which compositions distinguish alive missions
from mess ones* — then **discharge-train the move-prior** by reweighting toward the `:applied` sets of *alive*
missions and away from *mess* ones. Re-run the cascade-recovery experiment at corpus scale: reconstruct a
**held-out** mission's `:applied` set from its problem text via want-directed beam over `futon2.aif.rollout`,
scored against ground truth — same shape as slice-1's `cascade_recovery_experiment.py`, but N≈150 (not 8),
`|U|>1` the norm (so chaining actually fires), and discharge a real structural-quality label (not the
`want-coverage` proxy). This resolves all three of slice-1's honest edges by construction, and the learned
reweighting *is* a first cut at T2's `G(π)` = expected aliveness of a pattern-composition.

**Honest caveat on the label.** Salingaros "alive" measures structural coherence of the mission's scope-tree —
a *proxy* for "went well," not ground-truth success (Joe's own word: "seemingly"). A mission can be structurally
alive yet have missed its goal, or vice versa. Mitigations: (i) keep the label graded (`:L`), not just the
class; (ii) optionally triangulate against mission `Status:` (COMPLETE) — agreement = strong signal, disagreement
= a diagnostic (combining-methods-as-diagnostic); (iii) lean on the existing null controls so any learned lift is
checked against shuffle/random. The `?`/stub missions (incl. `wm-policies`, `memes-arrows-patterns-diagrams`) are
unlabelled — held out of training, not coerced.

### 5b. Slice-2b — live `meme.gates` discharge (deferred)

What slice-2a defers cleanly: slice-2a learns from **recorded** discharge (mission history). Wiring the **live**
`meme.gates` commit (terminals-match + cascade-warrant + grounding on a *freshly folded* cascade) is the
forward-looking, micro-grain commit — useful for scoring *new* folds the corpus hasn't seen. Separating
"learn-from-history" (2a) from "gate-live-folds" (2b) is cleaner than the original single slice, and 2b is only
worth building once 2a shows the learned prior actually lifts recovery.

### Success criterion (so we don't relaunch "called the degenerate thing `G`")

Slice-2a succeeds iff the discharge-trained, beam-searched cascade-rollout lifts **held-out** mission-pattern
recovery **above slice-1's 25%**, with the lift surviving the generalisation tests (answer-independence ·
held-out missions · demo-parameter invariance · non-gameability). If discharge-training does *not* lift it,
that is itself a real finding (the pattern language / link structure, not the prior, is the limit) — and it
directly motivates T1's typed-link experiment (revise the links, per Ostrom Principle 3) as the next lever.

## MAP findings — slice-2a corpus + alive/mess triangulation (2026-06-23)

Lab: `futon3a/holes/labs/M-memes-arrows/mission_aliveness_contrast.py` (the contrast) and
`mission_status_triangulation.py` (cut d). Read-only over the `futon6/data/` artifacts.

**Corpus is healthy.** 194 missions join cleanly; alive `|applied|` avg 4.9 with **61% having |U|>1** (mess avg
2.8, 35% multi; pipelines ~0.3 — mostly pattern-less). Slice-1's "chaining inert" edge resolves here.

**The contrast (alive vs mess `:applied`).** Clean separation: alive missions are enriched in futon-*native*
project/research-management & meta patterns (`expected-free-energy-scorecard`, `structured-observation-vector`,
`single-source-of-truth`, `par-as-obligation`, `what-problem-is-this-actually-solving`,
`unresolved-tensions-at-closure`); mess missions are enriched in generic *math/proof* patterns (`local-to-global`,
`unfold-the-definition`, `construct-an-explicit-witness`, `reduce-to-known-result`, `split-into-cases`).

**Two confounds, named not buried.** (1) **Domain** — the mess side is dominated by math/proof-reconstruction
missions, so the contrast partly reads *stack-engineering vs math-proof*, not good-vs-bad design. (2) **Volume** —
mess missions apply few patterns (42% apply any), so top-alive patterns trivially have `M=0` and the smoothed
lift inflates them; the only two-sided-support signal is *math-skews-mess*.

**Joe's reframe (2026-06-23): the domain skew is partly the MECHANISM, not just a confound.** In math domains he
leans on math patterns and *drops the general management patterns that keep a mission on the rails*, getting lost
in detail. Prediction: mess missions should also be disproportionately *unfinished*.

**Triangulation (cut d) — class × Status.** Finished-rate (done+archived): **alive 52% · mess 40% · pipeline
39%**. Directionally supports the reframe, but a ~12-pt gap, not decisive — caveats: "archived" is ambiguous
(maybe shelved-incomplete; alive 22 vs mess 6), and many alive missions are perpetual/open by design.
Mechanism check: mess applies a management pattern in **0%** of missions vs alive **36%** — the exact shape Joe
predicted, **but largely definitional** (MGMT/MATH were derived from the same contrast), so it illustrates rather
than independently confirms.

**Cut (a) — domain-stratify (done, 2026-06-23): domain is NOT the confound.** Math is only 9/194 missions;
removing them barely moves the contrast (stack stratum: alive applies a mgmt pattern 39% vs mess 0%). The earlier
"mess is dominated by math" was wrong — 41/43 mess missions are stack-domain. The live confound shifted to
**volume** (stack alive `|applied|`=4.8 vs mess 2.3). Lab: `mission_domain_stratify.py`.

**Cut (b) — volume-normalize (done, 2026-06-23): the signal is COMPOSITION, not volume.** At every matched-volume
threshold the mess management-rate stays **0%** — even mess missions applying **12–32** patterns carry *zero*
management/meta patterns — while alive rises 58%→74%; volume-normalized, alive devotes 20% of its bag to
management vs mess 0%. The 11 pattern-rich mess negatives are demonstrably *technique-heavy, management-absent*
(e.g. `M-interim-director-proxy-metric-inventory`: 32 technique/math patterns, no management) — the literal
"lost in detail." Caveat: the MGMT-set *membership* check is partly definitional; the non-circular evidence is
those rich-mess pattern lists themselves. Lab: `mission_volume_normalize.py`.

**Verdict: the discharge label is trustworthy.** The alive/mess pattern signal survives domain- *and*
volume-stratification, so slice-2a's label is not an artifact. High-confidence training core: **34 clean
positives** (alive ∧ `|applied|`≥3 ∧ ≥1 mgmt) vs **11 clean negatives** (mess ∧ `|applied|`≥3 ∧ no mgmt).

**Pivot (Joe, 2026-06-23): quantitative → STRUCTURAL.** Bag-of-patterns has gone as far as it can — it names
*which* patterns, not *how they compose*, and cascades are structured objects. The next phase analyses the
**induced subgraph of each mission's `:applied` set over the pattern↔pattern graph in
`futon6/data/pattern-phylogeny-edges.json`** (`co_app` 2721 weighted edges + `descent` 561, over 506 patterns):
connectivity, whether a management pattern sits as the hub/anchor with technique patterns chained off it, and
recurring motifs = candidate **cascade-formation patterns**. That structural signature — not the bag — is what
slice-2a needs to recreate cascades and to define `G(π)`; null-control calibration (cut c) folds into it.
(NB `mission-phylogeny.edn` is a *mission* tree, not pattern edges — distinct artifact.)

## Reuse: the existing semilattice clustering (E-pipeline-pipecleaner, 2026-06-23)

Joe: the prep-work already did hierarchical clustering — reuse it, don't rebuild. Found:
`futon3c/holes/excursions/pipeline-semilattice-clusters.edn` (+ `.md` writeup) from
`futon3c/scripts/stack_semilattice_clusters.py`, the `E-pipeline-pipecleaner` excursion. It clusters **229
missions** (BGE embeddings → basins M0–M11 + subclusters), **1010 library patterns** (→ constellations P0–P17 +
subclusters), **550 resolved mission→pattern warrant links**, and a **temporal ordering L0–L11**. This is the
hierarchy to reuse; `pattern-phylogeny-edges.json` (co_app/descent) only refines *within* a cluster where needed.

**Ask 1 — do the MGMT patterns sit in one cluster or span several? Several — by design.** They span six library
categories (`aif`, `futon-theory`, `coordination`, `structure`, `gauntlet`, `f6`) → the "stack-meta" pattern
constellations **P1** (futon-theory/coordination), **P2** (aif/agent), **P4** (f6), **P6** (invariant/structure).
MATH, by contrast, concentrates in essentially **one** constellation, **P13** (math-informal/math-formalization).
So "management" is not a single cluster — it is a **cross-cutting selection across the stack-meta region**, whereas
mess/math draws repeatedly from one cluster. (That cross-cutting breadth is itself a candidate alive-signature.)

**Bonus — the semilattice already encodes the alive/mess split structurally.** The writeup's "M0 Breakdown by
dominant cited pattern cluster" refactors the big mission basin by which pattern-constellation each mission cites
most. **M0P13** (dominantly cites the *math* cluster) is exactly our clean-negative / mess set —
`M-interim-director-proxy-metric-inventory`, `M-P3/P7/P8-rational-reconstruction`, `M-diagramprover`. **M0P1 /
M0P2 / M0P6** (cite the *management* clusters) are the alive ones. Independent corroboration of cuts (a)/(b),
from a clustering built before this analysis existed.

**Ask 2 — hierarchical → first guesses at cascade formation.** A cascade = a **path through the semilattice**:
a mission-cluster basin → its warranting pattern-constellations → concrete patterns, *ordered by temporal level*
(L0–L11). The strong cluster-warrant edges are the backbone (M0→P2 w=118, M0→P13 w=94, M0→P1 w=86). "Picking
elements from the hierarchy" = exactly this traversal; cascade-formation patterns are recurring traversal shapes.

**Ask 3 — whole-stack-as-cascade is the right frame (adopted).** This artifact treats the *entire stack* as one
upward cascade (mission clusters → pattern constellations → patterns, over temporal levels), building toward
`M-capability-star-map`. So a per-mission `:applied` set is **not** a standalone cascade — it is a **sub-path of
the one stack-wide cascade**, and any cascade slice-2a recreates *joins* that larger cascade. T1's "what is a
cascade" resolves here: a cascade is a connected sub-path of the stack semilattice, and `G(π)` scores such
sub-paths. This supersedes the per-mission framing in §5a — the unit is the stack cascade; missions are windows
onto it.

## Structural phase finding — the cascade traversal-shape (2026-06-23)

Lab: `mission_traversal_shape.py`, reusing the semilattice clustering (100% of applied patterns resolve to
constellations P0–P17). It asked: how is a mission's `:applied` set distributed across constellations, and does
that distinguish alive from mess *volume-robustly*?

**Strong signal — cluster occupancy.** Where applied patterns land: **alive** → aif/agent 32% + futon-theory/coord
20% (stack-meta ≈52%), MATH 9%. **mess** → **MATH 51%** (half of all mess pattern-usage in the single
math/technique constellation).

**Hypothesis correction — it's the ANCHOR, not breadth.** The "alive = cross-cutting, mess = stuck in one
cluster" guess was wrong. Volume-controlled (|applied|≥3) both classes are moderately concentrated (alive
top-cluster share 0.65, mess 0.71) and similar in breadth (norm 0.43 vs 0.36). The volume-robust discriminator is
**which constellation anchors the cascade**: mess is math-dominated **45%** vs alive **11%** (4×). So:
**alive cascades anchor in the stack-meta region (aif/futon-theory); mess cascades anchor in the technique
constellation (P13).**

Two refinements:
- **Domain-independent.** Mess missions are mostly stack-domain *by name* yet **math-anchored by pattern usage**
  (e.g. `M-interim-director-proxy-metric-inventory`: a stack mission, 32 technique patterns). "Math-anchored" is
  the structural signature of "lost in detail" — distinct from "is a math mission." Cleanest statement of the
  mechanism so far.
- **"Touches the management region" does NOT discriminate** — 82% of clean negatives touch P1/P2/P4/P6 somewhere.
  What discriminates is whether management is the **anchor/spine** vs peripheral.

**First cascade-formation pattern (the structural deliverable):** *alive cascade = anchored in a stack-meta
constellation with management patterns as the spine; mess / lost-in-detail cascade = anchored in the technique
constellation, management not the spine.* Implication for `G(π)`: reward sub-paths threaded through / anchored in
the management constellations; the slice-2a discharge-trained prior reweights toward management-anchored
traversals (not toward raw breadth). MAP is complete; the discharge label and its structural signature are both
characterised and trustworthy → slice-2a build is warranted.

## Slice-2a — re-scoped probe spec (2026-06-23, → claude-1 build / claude-2 review)

**Framing (Joe, 2026-06-23): this is an exploratory DERIVE *probe*, not INSTANTIATE.** Its job is to produce
findings (does a discharge-trained, management-anchored rollout actually recover held-out cascades?) that tell us
what to *then* design, ARGUE, VERIFY, and only afterwards INSTANTIATE as the committed artifact.

Supersedes §5a's per-mission framing. **Unit = a connected sub-path of the stack semilattice** (constellations
P0–P17 + `co_app`/`descent` pattern edges); a mission's `:applied` set is a *window* onto the stack cascade.

**Goal.** A discharge-trained, *management-anchored* cascade-rollout over the semilattice, plus an eval showing it
(i) recovers held-out missions' `:applied` patterns above slice-1's 25%, (ii) reproduces the alive anchor-
signature, and (iii) beats null controls.

**Read-only inputs.**
- `futon6/data/mission-pattern-scopes.edn` — per mission `:applied` + `:try-candidates {:pattern :cos}` (the base retrieval prior).
- `futon6/data/mission-wholeness.edn` — `:class` (alive/mess/pipeline/stub) + `:L` (the discharge label).
- `futon3c/holes/excursions/pipeline-semilattice-clusters.edn` — `:pattern-membership` (pattern→constellation), warrant edges, temporal levels.
- `futon6/data/pattern-phylogeny-edges.json` — `co_app` (2721) + `descent` (561) pattern edges.
- Reuse slice-1 core: `futon3a/holes/labs/M-memes-arrows/cascade_rollout.py` (`move_interface`, `admissible_step`, `want_coverage`, `rollout`) + `cascade_recovery_experiment.py`. MAP labs in the same dir give the validated facts.

**Build (all in `futon3a/holes/labs/M-memes-arrows/`).**
1. `cascade_semilattice.py` — pattern→constellation map (bare-name join, handle the 5 collisions); move graph over `co_app`/`descent`; `anchor(subpath)` = dominant constellation; `management_anchored?` (anchor ∈ {1,2,4,6} or spine threads them).
2. Discharge-trained move-prior (the R2/R3 return channel): from alive-vs-mess `:applied`, learn smoothed log-odds weights up-weighting moves in ALIVE traversals, down-weighting MESS; **combine with** the `:cos` base prior. Bias toward management-anchored traversals — **not** raw breadth (the corrected finding).
3. Want-directed **beam** (extend `rollout` to keep top-B admissible partial sub-paths) over the move graph.
4. `slice2a_experiment.py` — train on a held-out split, reconstruct `:applied` via beam, report recovery vs slice-1 25% / per-pattern / random; anchor-signature reproduction; and the **two null controls** (label-shuffle-trained prior, random prior).

**Acceptance bar.** Held-out recovery beats slice-1's 25% AND both nulls by a clear, reported margin (with n + a shuffle/permutation null); recovered alive sub-paths are management-anchored at a rate clearly above recovered mess sub-paths; honest edges reported (n, variance, failures); **no tuning the prior on the held-out set**; pytest green for the pure pieces (semilattice map, anchor predicate, trained-prior direction, beam admissibility).

**Discipline.** Lab files are untracked (commit-when-asked) — do not commit unless asked. Author ≠ reviewer:
claude-1 builds, claude-2 re-runs everything and checks the numbers as a real gate.

## Preregistration (DERIVE) — what slice-2a should teach us (2026-06-23, BEFORE results)

Written while claude-1 is still building and we are blind to the numbers, so the review is prediction-error
against a fixed prior, not post-hoc storytelling (no HARKing). On-theme: this prereg *is* our prior over outcomes;
the surprise on the bellback is the learning signal. Aligns with the stack's preregistration discipline
(cf. M-ukrns-wp-preregistration).

**Endpoints fixed in advance** (no post-hoc metric switching). All on a held-out mission split; the prior is
trained only on the train split.
- **Primary (isolates the thing slice-2a tests):** the *discharge-trained* move-prior beats the *untrained* `:cos`
  prior on held-out `:applied` recovery, **paired, same beam, same split**. This isolates the discharge-training
  effect (the R2/R3 return channel — the slice-1 bottleneck where true patterns ranked 116–661).
- **Secondary (is the gain real?):** trained beats both nulls — random prior and **label-shuffle**-trained prior —
  by at least the trained-vs-untrained margin (so the gain comes from label *content*, not reweighting magnitude).
- **Structural (does the anchor finding transfer to generation?):** recovered *alive*-mission sub-paths are
  management-anchored at a clearly higher rate than recovered *mess*-mission sub-paths.
- **Contextual only (cross-dataset, not rigorous):** absolute recovery vs slice-1's 25% — different ground truth
  (mission corpus vs closure-folds), so directional context, not a pass/fail bar.

**Predictions (priors we can be wrong about; stated so we can be).**
- Primary: trained lifts recovery over untrained by ~**+5 to +15 pp**. (Genuine prior — could be smaller.)
- Secondary: trained clearly > shuffle-null; shuffle-null ≈ untrained or only marginally above.
- Structural: alive management-anchored recovery rate ≥ ~**2×** mess.
- Mechanism: trained prior improves the *median rank* of true patterns vs `:cos` (the 116–661 baseline tightens).

**Decision rules (what each outcome means → next move).**
- **Primary ✓ AND Secondary ✓** → the discharge-trained, management-anchored prior IS the lever → commit DERIVE:
  design the real artifact around it, then ARGUE / VERIFY / INSTANTIATE.
- **Primary ✗ (no lift over untrained)** → the prior is not the bottleneck-breaker; the limit is the *link
  structure* → pivot to the **T1 typed-link experiment** (revise links, Ostrom Principle 3). (slice-2a's own
  stated fallback.)
- **Secondary ✗ (lift over untrained but not over shuffle-null)** → the gain is a reweighting artifact, not label
  content → discard this prior, re-examine.
- **Structural ✗ while Primary ✓** → recovery works but not via the anchor mechanism → the theory (`G` is
  anchor-driven) needs revising, even though the engineering works.

**Validity threats / review checklist (operational hooks I will actually check on the bellback).**
- **Leakage:** confirm the prior never saw held-out missions (train/test split honoured).
- **Small n on negatives:** only ~11 clean mess negatives → wide CIs; require reported CIs / a permutation null,
  not point estimates alone.
- **Anchor-predicate non-circularity:** "management-anchored" must not be defined using the same patterns the
  prior was trained to favour, or the structural endpoint is circular.
- **Null construction:** label-shuffle must shuffle alive/mess labels then retrain (not shuffle the output).
- **Bare-name collisions** (the ~5) and **pipeline/stub handling** stated, not silently dropped.
- **No held-out tuning:** any hyperparameter (beam width B, smoothing, prior-mix weight) fixed on train only.

## Slice-2a probe result — ROBUST NEGATIVE, reviewed (2026-06-23)

claude-1 built it; claude-2 reviewed as a real gate. **Review verdict: PASS** (the work is correct, honest,
well-tested; the negative is robust). What I checked: re-ran pytest **18/18**; reproduced the experiment;
**independently verified** the load-bearing disjointness claim with my own parser (not claude-1's code) —
`applied ∩ try-candidates = 0` across all 94 missions / 560 patterns; audited methodology (no leakage — prior
trained on train fold only; nulls correctly constructed; hyperparams a-priori; confounds honestly flagged). One
nit fixed by claude-2: beam tie-breaks were `PYTHONHASHSEED`-dependent → sorted neighbours for determinism
(trained recall now stable 3.75%; verdict unchanged).

**Numbers (k-fold, n=94 held-out, post-fix).** trained beam **3.75%** · label-shuffle null **7.47%** · random
null 1.46% · per-pattern(cos) **0.00%** · random 1.23%. Anchor: alive 92% vs mess 100% management-anchored.
LOO structure probe: **7.79% vs 1.16% random (~7×)**, n=539.

**Prereg scorecard** (against the predictions fixed before results):
- **Primary FAILS (informatively).** "trained > untrained `:cos`" passes only *vacuously* — cos recall is 0% **by
  the disjointness**, so it can't seed. The informative test (does discharge-training help recovery?) is
  trained-vs-shuffle: trained **loses** (3.75% < 7.47%). Discharge-training the *global* prior is worse than
  chance relabelling for recovery.
- **Secondary FAILS.** Trained beats the random-prior null but **loses to the label-shuffle null** → the signal
  is not just absent, it is anti-helpful for recovery.
- **Structural NOT reproduced.** alive 92% < mess 100% — confounded: the global prior + anchor bias push *every*
  recovered path to management hubs regardless of class (robust with W_ANCHOR=0). Not a clean signal.
- **My predicted failure-mode was WRONG** — the prereg said a primary-fail would mean "the limit is link
  structure → pivot to T1." It is **not** link structure: reachability is 96% (applied reachable from seeds).
  The prereg did its job by being falsifiable about *why* it would fail; I was wrong about the mechanism.

**The real diagnosis (independently confirmed, the deliverable):**
1. **The retrieval seed is structurally incapable** — `try-candidates` are a "what-else-to-try" list, **disjoint**
   from `:applied` (0/560). The spec's "reconstruct `:applied` from the pre-baked candidates" assumed an overlap
   the data does not have.
2. **The global discharge-trained prior is mission-GENERIC** — it rewards patterns frequent across *all* alive
   missions (management hubs), orthogonal to a *specific* mission's set → the beam converges to the same hubs for
   every mission → loses to shuffle and confounds the anchor signal.
3. **The deep finding (ties to the arc):** "what makes a cascade ALIVE" (the global discharge signal — REAL,
   MAP-validated, survives domain+volume) is a **different object** from "which cascade THIS mission used"
   (specific recovery). The harness conflates them — *a fresh instance of measuring the wrong object* (cf.
   argmax→G, retrieval-peak→commit, pattern→cascade). **This does NOT impugn the MAP alive/mess finding;** it
   says that signal does not translate into per-mission recovery via this harness.
4. **LOO shows real internal coherence** — a cascade's own members predict each other ~7× random — so
   mission-specific structure *is* recoverable; the disjoint seed + generic prior were starving it.

**Next DERIVE levers (Joe's call; the negative re-scopes, doesn't dead-end):**
(a) **mission-specific retrieval** — seed/score from full-embedding mission-text-vs-ALL-patterns (as slice-1 had),
not the disjoint pre-baked candidates. Biggest lever — current seed is structurally incapable.
(b) **co_app-WEIGHTED expansion** — the build used an *unweighted* graph, dropping the co_app weights; weighting
likely lifts the 7× LOO (mission-specific structure).
(c) measure the anchor-signature **without** the trained-prior confound (else circular).
(d) T1 typed-link experiment (Ostrom P3) **only if** weighted internal coherence stays weak — *not* yet motivated
(reachability is fine).

## DERIVE — designing G(π), the gap-filler (2026-06-23)

The meaningful DERIVE step (Joe: exploration must *serve* this): commit a design for `G(π)` that fills T2
(define `G` over cascades) so [[M-wm-policies]] gets a scoreable "select over distributions of cascades."
Grounded in the exploratory findings, having ruled out two wrong turns.

**Settled inputs.**
- **T1 (cascade):** a connected sub-path of the stack semilattice. ✓ (MAP)
- **Eval correction (slice-2a deep finding):** `G` = expected **aliveness**, validated by **discrimination**
  (rank alive > mess; intact > degraded), **not recovery** — recovery is a different object the global signal
  cannot do.

**Focused DERIVE probe (`derive_coherence_probe.py`) — ruled out a candidate term.** Hypothesis: `G` needs a
co_app-weighted internal-coherence term as its cascade-level non-additive piece. **Refuted.** Raw co_app density
is trivially 1.0 (circular — co_app is built from co-application); corroborated (weight≥2, non-circular): **alive
0.39 vs mess 0.61, AUC 0.34, z=−1.90** — alive cascades are *less* co_app-coherent. co_app coherence merely
re-encodes within-constellation concentration (mess concentrates in math, whose patterns routinely co-occur;
alive spreads cross-constellation). **Term dropped.** (The slice-2a LOO ~7× was about *recovery* — internal
predictability — which is not the same as *class discrimination*.)

**The committed design — `G(π)` = a constellation-traversal-shape score.**
- **+** anchoring / threading the **stack-meta constellations {1,2,4,6}**; **−** anchoring in the **technique
  constellation {13}**; secondary **+** cross-cutting breadth across the stack-meta region.
- The cascade-level, **non-additive** term is the **anchor** — a property of the whole sub-path's constellation
  distribution, not a per-pattern bag-sum and not pattern co_app. This is *why* `G` genuinely lives at the
  cascade level (§1 thesis made operational), and it is the MAP-validated, volume-robust discriminator
  (math-dom 45% mess vs 11% alive).
- **Per-pattern alive log-odds** may enter only as a held-out-validated component — it is circular as an *eval*
  (trained on the label), so it cannot be the yardstick.

**Eval (VERIFY target), not recovery:** held-out class **discrimination** — does `G` rank alive > mess (AUC),
beating a label-shuffle null — plus a **degradation** ablation (G drops when the sub-path's stack-meta anchoring
is broken / constellations shuffled). This plays to the validated strength and avoids the recovery trap.

**How it fills the gap.** M-wm-policies' "select over distributions of cascades" = prefer sub-paths with high `G`
(stack-meta-anchored, cross-cutting). §1 thesis operational: `G` is defined on the sub-path's constellation
distribution (non-additive) → cannot reduce to per-pattern scoring. **Two wrong turns ruled out on the way:** the
recovery objective (slice-2a) and the co_app-coherence term (this probe).

**Remaining before ARGUE/VERIFY:** formalize the discrimination AUC of the constellation-shape `G` on held-out
missions + the degradation ablation (one focused VERIFY spike).

### DERIVE refinement — `G(π)` as two coupled axes (Joe, 2026-06-23)

Joe: `G(π)` is semantically *related to* the `mission-coherence` and `stack-coherence` pattern families (without
bowing to them) — a useful cascade has both **mission-specific** and **stack-wide** implications. The library
bears this out: `mission-coherence` (logic-model/invariants for *this* mission) sits in constellation **1**;
`stack-coherence` (evidence-ledger, blocker-detection, bridge-health, sync — coherence across the *whole* stack)
sits mostly in constellation **5**.

**Sharp consequence — our discharge label is BLIND to the stack-wide axis.** `mission-wholeness.edn` is
"Salingaros L=T·H over *scope-tree centres*" — it scores a mission's *internal* coherence, not its stack-wide
reach. So a `G` built only on alive/mess **cannot see** stack-coherence (constellation 5 was neutral in the MAP
occupancy, *consistently* with this). This is the same thing as "any cascade joins the bigger stack cascade":
**the join *is* the stack-wide axis.**

So `G(π)` = a **coupling of two axes**, not one anchor term:
- **Axis 1 — mission-specific coherence** (does the cascade hang together for this mission; `mission-coherence`,
  constellation 1). *Proxied by the alive label — empirically validated (MAP).*
- **Axis 2 — stack-wide coherence** (does the cascade keep the whole stack coherent / have stack-wide reach;
  `stack-coherence`, constellation 5). ***Not* captured by our label — semantically grounded, needs its own
  signal** (e.g. the cascade's warrant-connectivity to *other* mission-clusters in the semilattice, or presence
  of stack-coherence patterns). A useful cascade scores on **both**; `G` rewards the coupling.

**Honest split:** Axis 1 is empirically grounded; Axis 2 is a *design choice* motivated by the pattern-language
semantics, **not** captured by our label.

**Generalisation (Joe, 2026-06-23): `G(π)` = coherence across SCALES.** This is Alexander (*A Pattern Language:
Towns, Buildings, Construction* — the subtitle is the scale ladder; every pattern names its larger and smaller
neighbours). Mission+stack is just the **2-scale demo**; a proof would be inference→proof→theory, etc. The design
must not hardcode "2" — Axis-1/Axis-2 are the bottom/top of a scale stack, and `G` rewards a cascade that coheres
across the scales it touches.

**Focused probe result (`derive_axis_independence_probe.py`): the two scales are ORTHOGONAL — and mildly
anti-correlated.** Axis-1 (management-anchored) discriminates alive/mess (AUC 0.652, validated). Axis-2 does
**not** track the label: `reach` (cross-mission usage) AUC **0.485** (decisive null — alive 5.03 vs mess 5.17),
`c5_any` (stack-coherence presence) AUC **0.554** (barely; 11% of alive do any, 0% of mess). And the axes
**anti-correlate**: corr(c5, mgmt-anchored) **−0.30**; c5-presence 0.02 among anchored vs 0.19 among non-anchored.
Two consequences:
1. **`G` genuinely needs both terms**, and **Axis-2 needs its OWN ground truth** — the Salingaros scope-tree label
   is blind to it (AUC 0.485). Candidate Axis-2 signal: stack-coherence *outcomes* (evidence-ledger / no-blockers
   / bridge-health), not the alive label.
2. **"A useful cascade couples both scales" is a NORMATIVE target, not current practice** — in the corpus,
   missions do *either* mission-specific anchoring *or* stack-coherence work, rarely both (anti-correlated). So a
   `G` that rewards the coupling pushes toward valuable-but-currently-rare cascades — a *stronger* motivation for
   the artifact.

**Caveats:** small n (18 mess); Axis-2 proxies are crude (c5 low base rate). Orthogonality is solid (`reach` is a
clean null); the anti-correlation (−0.30) is directional on small n.

**Net for DERIVE:** `G(π)` = a multi-scale coherence score (≥2 coupled, orthogonal scale-terms; 2-scale demo =
mission-anchor × stack-coherence). The mission/anchor term has ground truth (alive/mess); **the stack-wide term's
ground truth is a recognised gap** (needs stack-coherence outcomes). This supersedes the single-anchor `G`.

### DERIVE synthesis — `G(π)` = expected tension-discharge across scales (Joe, 2026-06-23)

Joe's resolution of the Axis-2 ground-truth gap: a **geometric tension** measure — stack-wide but computed
locally — from [[project_substrate_2]] / **M-live-geometric-stack** (COMPLETE 2026-04-28; `geometric_layer_phase2.clj`
delivers `T, ∇T, ΔT, drift`). The War Machine goes to high-tension regions and does work that alleviates them.
This both closes the gap and **unifies the whole design**:

- **Both axes are the same quantity (tension) at different scales.** Salingaros is literally `C = T·(10−H)` — `T`
  *is* mission-scale tension (so Axis-1's alive/mess label is already a tension reading). substrate-2's `T(v,c)`
  is stack-scale tension (Axis-2). So `G` is not two unlike terms but **tension across scales** — exactly the
  Alexander cross-scale generalisation, made concrete and scale-parametric (add inference/proof/theory scales the
  same way).
- **Rigor guardrail (from M-wm-policies §58–67): tension is a FIELD (value per point); `G(π)` is over PATHS.** So
  tension cannot *be* `G`. But **tension-DISCHARGE along the path** is a genuine path quantity, and it is
  **non-additive** because tension is conserved (reduce T at the target region, raise it elsewhere — M-live-
  geometric-stack's own dynamic). This is precisely what makes `G` legitimately cascade-level rather than a
  per-point field — and it answers M-wm-policies' own field-vs-path objection.
- **`G(π)` = expected tension-discharge across the scales the cascade touches.** Axis-2 ground truth = stack-`T`
  discharge (`ΔT` / `∂T/∂t` a cascade effects in substrate-2); Axis-1 ground truth = mission-`T` (Salingaros).
  The WM loop (go to high-`T`, apply cascade, discharge `T`) *is* the policy that `G` scores. Reconnects the whole
  arc to [[M-wm-policies]] and the War Machine.

**The metric is a first-class DERIVE decision, NOT an implementation detail (Joe, 2026-06-23).** The original
stack-`T` was a crude `{0,1}` scalar (M-live-geometric-stack Gap B) → degenerate `G` (∇T ≡ 0, no real discharge).
The fix is a real differential-geometry metric: **Ollivier-Ricci curvature**. `misfit_rung3_curvature_demo.py`
computes κ (Sinkhorn-OT) on the file-co-mission graph, pre-registered + passing: κ **non-degenerate**, and
**negative-κ = interpretable cross-concern bridges** — exactly stack-wide-but-local *tension* (bridges/bottlenecks
= where the WM does work); its baseline test confirms those bridges are **new intent-structure, not the
dependency graph** (the orthogonal signal Axis-2 needs). So: **Axis-2 tension = negative Ollivier-Ricci curvature;
discharge = Ricci-flow-style smoothing.** The metric *determines what `G` means* (different metric → different `G`
→ different cascades favoured), so it is a design choice, not a detail. It is also scale-parametric — κ computes
on any graph at any scale (file-co-mission, pattern-co-mission, inference/proof/theory), matching the Alexander
cross-scale generalisation.

**Honest dependencies / caveats:**
- Ollivier-Ricci exists and is **validated in isolation** (the rung-3 demo) but is **not yet wired into the live
  substrate-2** as the queryable `T`-field — the dark-tower dictionary flags this exact seam
  (`futon5a/.../TN-joe-dt-explainer.md`: *"curvature … rigorous in isolation, but not yet wired to the Poly
  substrate … [open]"*). Making κ the live Axis-2 field is the **VERIFY/INSTANTIATE dependency** (no `T`/geometry
  endpoint on :7070; currently script-computed).
- Must hold the field-vs-path line: implement `G` as the **discharge integral over the path**, never as the raw
  field value.

**Lifecycle effect:** this *resolves* the Axis-2 ground-truth gap in principle (tension), turning it from "open
gap" into a "VERIFY-time dependency on the substrate-2 T-field." `G(π)` is now a single coherent object —
multi-scale tension-discharge — ready for ARGUE.

## ARGUE — why this design is right (2026-06-23)

Kept short + strategic (operational hooks live in VERIFY, per the argue-strategic/verify-operational discipline).

**Plain-language argument (no jargon).** Good work makes things fit together — not just the task in front of you,
but the whole system around it, at every level. Some places in a system are under strain: pieces that don't quite
fit, bridges holding apart things that pull together. We can measure that strain locally, everywhere. The best
sequence of moves is the one that relieves the most strain *across levels at once* — fixing the local thing and
easing the system-wide bridge it sits on. So instead of "which single step looks best," we ask "which whole
sequence discharges the most tension, across scales." That is the score; the work that earns it is the work worth
doing.

**Why it feels inevitable (each constraint forces a piece).** `G` must be over *paths* not points → the unit is a
cascade. Aliveness is non-additive → the term must be a *whole-path* quantity (discharge, conserved). The signal
must be stack-wide yet locally computable → a *field*. The field must be meaningful, not `{0,1}` → a real metric
(**Ollivier-Ricci**). It must be non-circular w.r.t. the alive label → *tension*, which the label cannot see. It
must serve the IDENTIFY theory → *expected free energy over policies* (cascades). Compose those forcings and you
get exactly: **`G(π)` = expected tension-discharge across scales.** Little was free to choose.

**Theoretical coherence (serves the IDENTIFY anchoring).**
- **AIF / EFE:** `G(π)` *is* expected free energy over cascades (the §1–3 level-shift); tension is the
  prediction-error potential, discharge is its minimisation. The WM's go-to-tension / do-work loop is
  free-energy descent on the stack. Coheres with the 4-job stack (retrieval=prior, typecheck=filter,
  discharge=commit, combining=detector).
- **Ostrom / IAD:** a cascade = an institutional design; tension-discharge = the institution easing strain on its
  commons; cross-scale = **nested enterprises** (Principle 8). The metric-as-design-decision = rules are
  first-class (collective-choice, Principle 3).
- **Alexander:** the cross-scale framing is *A Pattern Language*'s scale ladder; `G` rewards a cascade coherent
  across the scales it touches.

**Pattern cross-reference (futon3/library — design rests on / aligns with).**
| pattern | where it applies | how |
|---|---|---|
| `futon-theory/structural-tension-as-observation` | the whole design | tension is the primary observable; `G` scores its discharge |
| `aif/expected-free-energy-scorecard` · `aif/candidate-pattern-action-space` | `G(π)` itself | `G` = EFE over cascade action-space |
| `aif/admissibility` · `aif/no-self-certification` · `aif/off-continuity-null-discriminates` | eval | discrimination + null controls; no self-grading |
| `aif/niche-construction` | the WM loop | high-tension promotion grows the stack boundary |
| `mission-coherence` / `stack-coherence` | Axis-1 / Axis-2 | the two demo scales |
| `combining-methods-as-diagnostic` | throughout MAP/DERIVE | disagreement of methods located each constraint |

**Trade-offs (what we gave up, why).** (i) Dropped *recovery* as the objective (slice-2a) → gained a
non-circular, theory-aligned one (discharge); cost: we can't claim to reconstruct historical cascades. (ii)
Dropped the `{0,1}` field for Ollivier-Ricci → gained a non-degenerate metric; cost: heavier compute (OT/Sinkhorn)
and a live-wiring dependency. (iii) Designed scale-parametric (N scales) but demo only 2 → cost: generality
unproven beyond mission+stack until a second domain (proof: inference/proof/theory) is tried.

**Generalisation.** Nothing in the design is mission-specific: κ computes on any graph at any scale, so the same
`G` applies to a proof (inference→proof→theory), a paper, a campaign — wherever a pattern-language-like scale
ladder exists.

**ARGUE exit:** the plain-language argument stands alone; the design is forced by its constraints rather than
merely workable. → VERIFY.

## VERIFY — in progress (2026-06-23)

Three operational hooks (argue-strategic/verify-operational split):
- **Hook (1) — κ-field readiness (claude-1, in flight).** Produce a non-degenerate Ollivier-Ricci κ tension-field
  over the *pattern* graph (pattern-co-mission + co_app), reusing the rung-3 OR code
  (`futon5a/.../misfit_rung3_curvature_demo.py`, which did it for files). Output node→κ JSON
  (`futon3a/.../pattern-curvature.json`) + honest temporal-feasibility verdict. claude-1 builds / claude-2 reviews.
  Job `invoke-1782218044882-43-7c53cbbf`.
- **Hook (2) — discrimination (claude-2, harness built).** `verify_g_eval.py`: does a tension-`G` separate
  alive vs mess, beating a label-shuffle null? No train/test split needed — tension is *label-independent* (the
  Axis-2 point). Built + smoke-tested on a placeholder; machinery (AUC, shuffle null) validated. **Awaiting real
  κ.**
- **Hook (3) — degradation/specificity (claude-2, harness built).** With node-κ: specificity (real cascade vs
  random same-size). **True path-breaking degradation needs hook-(1) edge-κ + the graph** — gated on that.

### VERIFY result (2026-06-23)

- **Hook (1) κ-field — PASS (claude-1 built, claude-2 reviewed: re-ran, independently checked).** Non-degenerate
  (node-κ stdev ~0.21, 74–82 distinct, edge frac_neg 0.55–0.63), deterministic (byte-identical), and **negative-κ
  = genuine management↔math cross-concern bridges** — `structural-tension-as-observation ↔
  transport-across-isomorphism` κ=−0.73, etc.; the futon-theory "tension" pattern is the top bridge hub. Two
  graphs agree (jaccard 0.69, all co_mission negatives ⊂ co_app). pytest 27/27. **The Axis-2 tension *field* is
  verified.** (`pattern_curvature.py`, `pattern-curvature.json`.)
- **Hook (2) discrimination — weak, and conceptually mis-targeted.** Tension-engagement trends right (alive
  engages more: G_engage AUC 0.61 co_mission) but n.s. (z=+1.5, n=18 mess). **Key point: tension is Axis-2, which
  the axis-independence probe already showed is ORTHOGONAL to the alive label (Axis-1).** So weak discrimination is
  *consistent with the two-axis design*, not a failure — and it means **the alive label cannot verify the tension
  term.** (The harness — `verify_g_eval.py`, claude-2 — was testing the wrong target; honest self-finding.)
- **Hook (3) specificity — weak-positive.** Cascades engage more tension than random same-size sets (+0.03–0.04):
  cascades aren't tension-neutral, but modest.

**What's gated (cannot be checked with current ground truth):**
- **Tension-DISCHARGE (∂κ/∂t)** — the actual claim — needs *temporal* κ: the `[open]` Poly↔curvature seam,
  deferred to INSTANTIATE.
- **Coupled-`G` beats single-axis** needs an **Axis-2 usefulness ground truth** (the recognized gap — the alive
  label is Axis-1 only).

**VERIFY verdict (honest, partial): the Axis-2 tension field is verified (structurally + qualitatively); the
discharge dynamics and the coupled-`G`-improves-usefulness claim are NOT yet verifiable.** Two named blockers to
clear before a meaningful INSTANTIATE: (i) temporal κ (wire OR to the live substrate), (ii) an Axis-2 usefulness
signal. This is a real stop-the-line: don't INSTANTIATE the coupled `G` until at least one is closed.

### VERIFY follow-up — retrospective ∂κ spike (dispatched 2026-06-23, claude-1)

**The two blockers are one.** Measuring tension-**discharge** over time (∂κ/∂t) *is*, by our design, the Axis-2
stack-usefulness ground truth (a stack-useful cascade = one that discharges stack tension). So one spike clears
both ⛔ — *retrospectively* (the live wiring stays INSTANTIATE).

**Approach (Joe's hint): XTDB bitemporal `as-of`.** substrate-2/futon1a is XTDB with live valid-time
(`xtdb_backend.clj`). Query the **code-substrate graph** (`code/v05/*` hyperedges) `as-of` T_before vs T_after a
mission's window via **Drawbridge** (HTTP `/api/alpha/hyperedges` currently returns "unknown endpoint"; go
through the JVM). Run Ollivier-Ricci κ (reuse `pattern_curvature.py`) on each snapshot; Δκ at the mission's
touched regions. On the *code* graph (unlike co-mission) a mission's commits are the real intervention, so
before/after is a legitimate measurement. **Phase-0 de-risk first:** does `as-of` give distinct historical
graphs, and is the watcher's history deep enough to cover the corpus? If not → fall back to a git co-edit graph.
**Phase-1:** population test — do alive/high-Salingaros missions discharge tension at their targets more than
mess (with a null)? Then the coupling test (does each axis add independent predictive value).
Job `invoke-1782223659836-49-0086cfe8`; claude-1 builds, claude-2 reviews + does the coupling analysis.

### Phase A + D result (2026-06-23): real per-commit ∂κ — apparatus built, discharge UNDERPOWERED-NULL

Joe ratified the **real** solution (no proxy): backfill substrate-2 with real per-commit code structure. claude-2
built it (offline halves) — **Phase A** (`extract_code_graph_asof.py`): reconstructs the real namespace
dependency graph as-of any date from git (read-only `git show`/`ls-tree`, no checkout); validated — graphs evolve
(futon3c 76→235→412 ns Feb→Jun). **Phase D** (`verify_g_discharge.py`): Ollivier-Ricci κ on the as-of graph,
neighborhood discharge measure (κ rise at the mission's landing-zone) with **drift control** (subtract repo-wide
mean Δκ).

**Result: an UNDERPOWERED NULL.** Drift-corrected mean Δκ alive −0.17 vs mess −0.04; AUC(alive>mess)=0.28,
z=−1.22 — **no discharge signal.** But honestly it *can't* yet: of 54 candidate missions only **n=13** (alive 9 /
mess 4) have namespaces present in *both* before/after monthly snapshots (most missions *create* code → no
"before" at their ns; the monthly grid is too coarse). n=4 mess ⇒ AUC is uninformative — **underpowered, not a
refutation.** Two honest signals: (i) a mild *consistent* direction (alive landing-zones trend *tenser*),
hinting the **code ns-dependency graph may be the wrong scale** for "aliveness tension" (hook-1 found it at the
*pattern/concern* scale, not code structure); (ii) only refactor-type missions have a clean same-ns before/after.

**Implication:** the git-monthly-grid lacks the resolution to power the test → this *motivates* the **B+C
substrate backfill** (native per-commit valid-time data = the resolution), and/or measuring discharge on the
**pattern-scale** graph rather than code ns-dependency. The apparatus (A+D) is ready for finer input.

### Phase-0 verdict (2026-06-23): XTDB route is a verified dead-end → git co-edit PILOT ratified. The substrate
code-graph is a **HEAD-snapshot** (`commit_ingest.clj:10`: `code/v05/edits` = "v0 HEAD-snapshot"), independently
confirmed. So `db-as-of` returns the *current* code structure at any time — the commit spine carries timestamps,
but the code hanging off it is HEAD. **This is a real substrate-2 defect** (Joe: "a temporal database … using to
point to HEAD, while we're trying to model what's actually in the code … doesn't really add up"): substrate-2's
temporality covers the commit spine + evidence, **not** code contents. So Joe's bitemporal hint cannot be honored
*through the substrate* until that is fixed. **Follow-on:** version `code/v05/edits` per-commit (the M-live-
geometric-stack Gap-A/B successor) so historical code-as-of queries work — **chartered as
`futon3c/holes/excursions/E-substrate-2-timetravel.md`** (diagnosis + fix sketch + the κ time-travel it enables).
**Pilot (Joe-ratified):** git co-edit
graph (JVM-free, every commit = a valid-time) until substrate-2 is fixed. Phase-1 dispatched, job
`invoke-1782225289572-53-613e9097`.

## 6. Scope / non-goals

- **In:** the discharge-trained move-prior; want-directed beam over `futon2.aif.rollout`; wiring real `meme.gates`
  as the commit; the held-out generalisation evaluation.
- **Out:** hand-tuning the admissibility cut until authored needles commit (overfit by construction);
  lens-in-Poly *for this demo's sake* (build it only if it is a genuine answer-independent compositional invariant
  worth having regardless); committing on the retrieval prior (the error this whole note exists to retire).

## 7. Status & next

- **Slice-1: DONE + reviewed (PASS).** Lab artifacts under `futon3a/holes/labs/M-memes-arrows/` (untracked;
  commit-when-asked). The cascade level-shift is proven as a mechanism and its constraint localised.
- **MAP: DONE (2026-06-23).** Corpus assembled (194 missions / 490 patterns); discharge label = Salingaros
  alive/mess (`mission-wholeness.edn`), non-circular, survives domain- and volume-stratification; structural
  signature = management-anchored vs technique-anchored traversal over the reused semilattice. Labs:
  `mission_aliveness_contrast.py`, `mission_status_triangulation.py`, `mission_domain_stratify.py`,
  `mission_volume_normalize.py`, `mission_traversal_shape.py`.
- **Slice-2a: DONE — ROBUST NEGATIVE, reviewed PASS (2026-06-23).** claude-1 built / claude-2 reviewed (re-ran,
  independently confirmed the disjointness, fixed a determinism nit). Diagnosis: global-aliveness ≠
  specific-recovery (measuring the wrong object again); seed disjoint + prior generic; reachability fine.
  Next DERIVE levers (mission-specific retrieval + weighted co_app) recorded above, gated on Joe.
- **Lifecycle position: DERIVE design committed + refined (2026-06-23).** `G(π)` = a **multi-scale coherence**
  score (Alexander; scale-parametric, not hardcoded to 2) — ≥2 coupled, **orthogonal** scale-terms; 2-scale demo
  = mission-anchor (Axis-1, ground truth = alive/mess, validated AUC 0.652) × stack-coherence (Axis-2, orthogonal
  to the label, AUC 0.485). Eval = held-out **discrimination**, not recovery. Three wrong turns ruled out by
  probes (recovery objective; co_app-coherence term; label-as-Axis-2). **Unified (2026-06-23):** `G(π)` =
  **expected tension-discharge across scales** — both axes are the same quantity (tension) at different scales
  (Salingaros mission-`T` × substrate-2 stack-`T`); `G` = discharge integral over the path (field-vs-path
  respected). Axis-2 ground-truth gap → **VERIFY-time dependency** on the M-live-geometric-stack `T`-field
  (COMPLETE but has Gap A/B — needs readiness check). Metric = **Ollivier-Ricci curvature** (first-class
  decision, validated in isolation). **ARGUE done (2026-06-23)** — design forced by its constraints; plain-language
  argument stands alone. **VERIFY done — partial (2026-06-23):** Axis-2 tension *field* PASS (κ non-degenerate,
  bridges = management↔math seams, reviewed); but the **discharge dynamics (∂κ/∂t) and coupled-`G`-usefulness
  claim are NOT verifiable yet** — gated on (i) temporal κ (live-substrate seam) and (ii) an Axis-2 usefulness
  ground truth. **Stop-the-line before INSTANTIATE** until one blocker clears.
