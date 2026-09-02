# C476 — D61a: the count-only BMR experiment gains an acceptance that can fail

**Item.** `worklist.edn` `:D61a` (class V). Figure-4 tally instance
`:count-only-bmr`, defect class 1 ("acceptance that cannot fail"),
`p4ng/empirics-futon/defect-repair-tally.edn:17`. Catalogue entry:
`futon2/holes/problems/P-defect-classes.md:39-40`.

**What the defect was.** `holes/labs/slush-demo/bmr_constellation_experiment.py`
scored all 6903 pairs and printed `Accepted (dF <= -3): 6903 / 6903`
(`findings/bmr_constellation_experiment.out.txt:10-11`), then exited 0. There
was no assertion, no stated population, and no baseline, so no state of the
world made the script report failure — including a scorer that accepts
everything. A 100% accept rate was read as a result; nothing distinguished it
from an instrument that cannot reject.

## What changed

`bmr_constellation_experiment.py` gained an ACCEPTANCE section
(`bmr_constellation_experiment.py:319-489`) with ten assertions, run after the
three existing experiments; `main` returns 1 if any is red
(`bmr_constellation_experiment.py:534-547`). Experiments 1-3 print exactly what
they printed before: `out.txt` lines 1-81 are byte-identical to the committed
artefact, so the pointers `out.txt:10-11` (`P-defect-classes.md:40`) and
`out.txt:5-20` (`P-evidence-apex.md:426`) still name what they named.

**Population, stated** (`out.txt:86-93`): the 6903 unordered pairs of the 118
patterns occupying ≥ 2 of the 94 missions in
`futon6/data/mission-pattern-scopes.edn` (560 edges), scored at prior 0.1,
accept iff ΔF ≤ −3.

**The ten checks** (`out.txt:95-131`), each with the state that would make it red:

| # | check | result | red would mean |
|---|---|---|---|
| 1 | `population-nonempty-and-enumerated` | 118 patterns → 6903 pairs | vacuous acceptance over an empty population |
| 2 | `population-predicate-holds` | 0 members occupy < 2 *distinct* missions | selection counts edges, so a pattern twice in one mission would be mis-included |
| 3 | `constellation-coverage-complete` | 0 uncovered | exp. 2/3 grouping over an incomplete map |
| 4 | `scorer-accepts-identical-rows` | ΔF = −20.37 | the scorer cannot accept |
| 5 | `scorer-rejects-evidence-free-rows` | ΔF = 0.00 for two all-prior rows | the scorer accepts anything, and 6903/6903 is an artefact of the instrument |
| 6 | `least-mergeable-accepted-pair-is-disjoint` | ΔF = −6.45, 0 shared missions | `out.txt:18`'s "even disjoint patterns" sentence is false |
| 7 | `dimension-drives-acceptance` | 0.0000 at k=2 → 1.0000 at k=94, monotone | a flat 1.0000 would refute "collapses in 94-dim" |
| 8 | `accept-count-is-not-evidence` | null reproduces 6903/6903 in 20/20 | the accept count would be informative after all |
| 9 | `mean-delta-f-is-evidence` | observed −11.41 < null min −11.28 | the corpus is not more mergeable than its degree-matched null |
| 10 | `within-across-gap-beats-its-null` | gap −1.5454, p = 0.0000 (0/200) | exp. 3's "PARTIALLY validates" is unsupported |

**Two expected-if-null baselines**, both seeded (`SEED = 20260902`):
- Degree-preserving corpus shuffle, 20 replicates (`out.txt:118-124`): permuting
  the mission column of the edge list keeps every pattern's and every mission's
  edge count and destroys which meets which. Accept rate `[1.0000, 1.0000]`,
  mean ΔF `[-11.28, -11.17]` against the observed `-11.41`.
- Constellation-label permutation, 200 shuffles (`out.txt:126-129`): null mean
  gap `+0.0368`, 5th percentile `-0.5618`, observed `-1.5454`.

**Dimension control** (`out.txt:108-116`): the same 6903 pairs rescored on the
top-k missions by edge count — k=2: 0/6903; k=4: 97; k=8: 1380; k=16: 4449;
k=32: 6707; k=94: 6903.

## Mutation test — the acceptance does go red

Three mutants of the script, run from copies (`/tmp/mut/`, not committed):

| mutant | exit | red checks |
|---|---|---|
| `score_pair_delta_f` returns −50 unconditionally (the class-1 defect itself) | 1 | 5, 6, 7, 9, 10 |
| population predicate `c >= 999` (empty population) | 1 | 1, by the guard at `bmr_constellation_experiment.py:507-521` |
| corpus replaced by its own shuffled null | 1 | 2, 9 |

The empty-population mutant initially died with an `IndexError` inside
experiment 1 rather than stating a verdict; the guard was added so the
degenerate case reports red and exits 1 with a named check.

## What this does and does not settle

The measured consequence for the sentence at `out.txt:17-18`: **the accept
count is not the evidence.** The degree-preserving null also accepts
6903/6903, so "6903/6903" separates the corpus from no structure at all by
nothing. What does separate them is the mean ΔF (−11.41 against a null floor of
−11.28, check 9) and the within/across gap (−1.5454, p = 0.0000, check 10);
what shows the collapse is dimension-driven is the sweep, not the count
(check 7). This is a finding about the instrument, recorded here with
pointers; it is not a ruling, and no registry `:choices` or `:decisions` entry
was written.

Unchanged: the numbers experiments 1-3 print, and the reading that embeddings
carry structure BMR-on-counts does not. Not addressed here (outside D61a's
acceptance): whether R17's live merge gate uses this scorer in this regime —
`p4ng/vetting/CLEANUP-QUEUE.md:79` files that under C9/O22 separately.

## Reproduction

`python3 bmr_constellation_experiment.py` from
`futon2/holes/labs/slush-demo`, ~9 s, exit 0, output byte-identical over two
runs. Stdlib only; the previous usage line named
`/home/joe/code/gflownet/.venv/bin/python`, which does not exist on this
machine (checked 2026-09-02), and was replaced by `python3`.
