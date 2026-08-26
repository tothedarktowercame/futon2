# E-cascade-sampler-four — four cascades side by side, 2026-08-26

*Joe's ask (2026-08-26): "create a reasonable selection of cascades … a cascade
of WR patterns, a cascade from the current APM mathematical work, a cascade
that's on record as having been created and used by the WM, and maybe one more
created from operator turns as a mockup of what we might be able to build with
AIR." Prior art: `E-cascade-sampler-sampler.md`. Built by claude-13.*

**Yardstick discipline carried over from the sampler-sampler.** Generation may
use `C` (wholeness) as a proxy, with eyes open; **judgment never uses `C`.**
The table below reports `C` because the constructor emits it, NOT as a ranking.
Nothing here is scored. Diversity is reported, never credited.

## The four

Constructed with `cascade_serve.py <psi> 20 0.15` (budget 20, epsilon 0.15)
against `minilm_pattern_embeddings.json` as refreshed 2026-08-26 (1371 patterns).
Cascade C is not constructed — it is declared, and is included for exactly that
contrast.

| # | cascade | psi source | chars | size | C | H | T | descent | top rel | warrant (≥0.45) |
|---|---|---|---|---|---|---|---|---|---|---|
| A | War Room | all 28 `library/war-room/*.flexiarg`, metadata stripped | 950 | **1** | 0.398 | 1.000 | 0.398 | 0 | 0.3980 | no |
| B | APM mathematical work | the two f34/f35 coined-pattern library files | 950 | **1** | 0.502 | 1.000 | 0.502 | 0 | 0.5020 | **yes** |
| C | WM, on record | `phase5-outer-cascade.edn` — declared, not constructed | — | 4 | — | — | — | 3–4 | — | n/a |
| D | Operator turns (AIR mockup) | 8 of Joe's turns, this session | 940 | **6** | 2.152 | 0.905 | 2.377 | 0 | 0.4380 | no |

Members:

- **A** — `campaign-coherence/campaign-as-temporary-institution` (0.398). One pattern.
- **B** — `ai4ci/process-vs-exposition` (0.502). One pattern, and the only
  constructed cascade here that clears the warrant threshold.
- **C** — `p4ng/R9-independent-witness`, `p4ng/R6-candidate-pattern-action-space`,
  `p4ng/R5-policy-evaluation`, `p4ng/R10-liveness`; descent
  R9→R6, R6→R5, R6→R10; two `:unrepresented-control` policy holes, both
  `recover-from-misleading-retrieval-seed`.
- **D** — `gauntlet/modeline-persists-across-worlds` (0.438),
  `aif/candidate-pattern-action-space` (0.397),
  `gauntlet/world-is-hypergraph` (0.392), `agent/sense-deliberate-act` (0.390),
  `structure/interest-event-vocabulary` (0.384),
  `aif/expected-free-energy-scorecard` (0.376).

## What the sampler shows

**1. Cascade size measures the TOPICAL BREADTH of psi, not its length.** This
corrects a claim made in this session on 2026-08-26 ("psi construction needs
~900 characters of specific prose — that's the regime where size and wholeness
appear"). A, B and D are all ~950 characters. A and B return **one** pattern; D
returns six. The epsilon-0.15 coverage-saturation rule stops as soon as the psi
is covered, and a thematically tight psi is covered by one pattern however long
it is. The War Room corpus is 28 patterns that all argue about the same thing,
so it saturates immediately.

Requirement restated: **psi must SPAN the concerns you want the cascade to
cover.** Length is a proxy for that and a poor one.

**2. That is a structural argument for operator turns as psi, not merely an
empirical one.** A session's operator turns naturally range across concerns —
D spans scribes, cascades, session-mode, operator→AIF transfer and pattern
mining — which is precisely the input shape that produces a multi-member
cascade. Mission docs behave similarly (3–5 members) because they too cover
context, aims and phases. Slugs and single sentences do not.

**3. Breadth and decisiveness trade off.** D has the largest size and the
highest `C` of anything measured in this session, and it produces **no warrant**
(0.438 < the 0.45 `*warrant-threshold*` in `futon3c.aif.chipwitz`). B is a
single pattern and *does* warrant. A cascade that covers a region is not a
cascade that determines a choice. If the inner loop wants a warrant, psi should
be assembled per choice-point; if it wants orientation, per session.

**4. Descent edges are rare and are not a function of psi.** A, B and D all
return 0. The `M-shared-memory-control-build-test` mission doc returns 7 and
`M-apm-demonstration` returns 0. Descent comes from `load_phylogeny()` —
declared relations between the specific patterns retrieved — so a constructed
cascade has structure only when the shelf already declares it. The only cascade
here with reliable internal structure is **C, the declared one**.

**5. The "on record and used" cascade is declared, and I could not find a
constructed one in the wild.** C's provenance is a fixture dated 2026-07-23;
its *use* is live and receipted — `strategic-cascade/outer-frontier-v1` ran it
in this session's selection and returned
`M-shared-memory-control-build-test`. Separately, `portfolio/effect.clj`'s
`:acquire-patterns` path emits a `proposed cascade for |psi=…>` note when the
WM constructs one; a text search over the evidence landscape (173,652 rows;
df: cascade 2903, wholeness 113, psi 109, semilattice 205) surfaced no such
note. So the WM's *constructed*-cascade path has no record of having fired.
That is a status finding, not a measurement.

## What this suggests for preparing the substrate

The gap between D and C is the interesting one. D has members and no structure;
C has structure and a frozen origin. A substrate of operator turns with keyword
and pattern associations would let psi be **assembled to span a chosen set of
concerns** rather than sampled from whatever a session happened to contain —
which is the lever on point 1 — and, if turn→turn edges carry order (the
timestamp argument in `E-operator-turn-modelling-2026-08-25.md`), a source of
descent that does not depend on the shelf having declared it, which is the lever
on point 4.

Neither lever is exercised here. This is a sampler, not a result.

---

## Correction, same day — cascade B was built from the wrong psi

*Joe: "item B, one pattern `ai4ci/process-vs-exposition` doesn't match what I'd
have expected — I'd have thought we'd have patterns from these libraries:
math-formalization … math-strategy … and maybe, uniquely to the mathematics
domain, some 'leaf memories' that are not patterns."*

Both halves of that are right, and the second overturns a conclusion above.

### B was psi error, not retrieval failure

The math libraries are all present in the index: **121 patterns across all 15**
(`math-formalization` 22, `-CA` 23, `-CV` 6, `-FA` 2, `-GN` 2, `-GR` 1, `-MG` 1,
`math-informal` 24, `-CA` 6, `-CO` 2, `-CT` 7, `-LO` 1, `-NA` 2, `-RA` 2,
`math-strategy` 20). Nothing was missing.

The psi was. I took the first 950 characters of the f34/f35 coined-pattern
files, which is file-header bookkeeping — *"Created because no existing math
library pattern fits the mined rules. This file is ingested explicitly by
`scripts/apm-ingest-coined-pattern-files.sh`…"*. `ai4ci/process-vs-exposition`
is a perfectly good match for that text. It is not mathematics.

Rebuilt from the mined rules themselves (f39 + f34 + f35 bodies, boilerplate
stripped — monotonicity-lemma variants, instance synthesis, argument order):

| | psi | chars | size | C | H | descent | top rel | warrant |
|---|---|---|---|---|---|---|---|---|
| B (retracted) | f34/f35 file headers | 950 | 1 | 0.502 | 1.000 | 0 | 0.5020 | yes |
| **B2** | f39/f34/f35 mined mathematical rules | 950 | **8** | **2.420** | 0.923 | **4** | 0.4360 | no |

B2's members, 7 of 8 in the math libraries:

    0.4360  math-strategy/compose-independent-lemmas
    0.3770  math-informal/induction-and-well-ordering
    0.3120  math-informal/parametric-tension-dissolution
    0.3110  math-strategy/exhaustion-as-theorem
    0.3020  agent/reduction-to-kernel
    0.3010  math-informal/split-into-cases
    0.2910  math-strategy/non-circularity-check
    0.2900  math-informal/find-the-right-abstraction

### This overturns point 4 above

Point 4 said descent edges are rare, come only from declared phylogeny, and so
"the only cascade here with reliable internal structure is C, the declared one."
**B2 has 4 descent edges from the constructor.** The maths libraries declare
phylogeny among themselves; the general shelf largely does not. So a constructed
cascade DOES get structure — in a domain whose library was built with relations.
The correct statement is that structure is a property of the LIBRARY, not of the
constructor or the psi, and the maths domain is the one that has it.

It also strengthens point 1: B2 is the largest and most coherent constructed
cascade measured (size 8, C 2.420, H 0.923), from the same 950 characters that
produced size 1 when they were bookkeeping. Breadth of concern, not length.

And the breadth/decisiveness trade-off in point 3 survives: B2 does **not**
warrant (0.436 < 0.45) where the single-pattern B did.

### Leaf memories are real, are 682, and are invisible to this constructor

Joe's "leaf memories that are not patterns" exist and are specific to the maths
domain: **683 `memory/assert` hyperedges, 682 distinct entries**, attached
across **69 distinct `math-*` patterns**. Ids are the mined rules themselves —
`e-codexpilot-bound-polynomial-sum-degree-by-a-common-summand-bound`,
`e-codexpilot-derive-integrable-from-nonzero-bochner-integral`,
`e-codexpilot-bridge-radial-integrand-order-with-pointwise-commutativity`.
Prefixes are all APM (`codexpilot` 123, `apm` 35, then problem ids).

**There are two cascade constructors with different universes, and only one
reaches these:**

| constructor | universe | reaches leaf memories? | used by |
|---|---|---|---|
| `conductor.clj` memory cascade | substrate traversal: reviewed `memory/assert` → `pattern/has-semantic-why` | **yes** | APM / the Zai student |
| `cascade_construct.py` | `minilm_pattern_embeddings.json`, 1371 patterns (line 32) | **no** | the WM inner loop, via `cascade-policy-for` |

So the WM's on-the-fly cascade can only ever return patterns. The 682 leaf
memories — the specific, actionable rules — are structurally unreachable from
it. The maths domain has both constructors; the WM has only the pattern one.

**Consequence for the operator-model work.** If Joe-Scribe deposits operator
corrections as leaf memories attached to patterns (the destination the role card
specifies), they land in the substrate where the APM constructor can see them
and the WM inner cascade cannot. Getting operator-derived material into the
inner loop needs either (a) the deposits to reach the embedding file's universe,
or (b) the inner constructor to traverse the substrate the way `conductor.clj`
does. That is a wiring requirement the role card does not currently name, and it
belongs in its "Wiring this card needs" section.
