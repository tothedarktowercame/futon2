# Checked-map named-declaration alignment receipt

Branch: `checked-map-alignment`  
Worktree: `/home/joe/worktrees/futon2/checked-map-alignment`  
Base source pin exercised: futon2 `c5db4719ee8fb3b857939d6a4899e4442be2f5e4`; mathlib4
`3836beefbc68cc2060ff54f88447322f64f8ad77`.

## Commands and results

| command | exit | result / log |
|---|---:|---|
| `python3 -I holes/labs/wm-contract/logic-model/alignment/test_align.py` | 0 | 36 tests, `OK`; `/home/joe/notes/wm-next/hypergraph-implementation/alignment-code-logs/test-align-final.log` |
| `clj-kondo --lint holes/labs/wm-contract/logic-model/alignment/code_sites.clj` | 0 | 0 errors, 0 warnings; `.../alignment-code-logs/kondo-final.log` |
| `emacs --batch -l /home/joe/code/futon4/dev/check-parens.el holes/labs/wm-contract/logic-model/alignment/code_sites.clj` | 0 | `OK`; `.../alignment-code-logs/check-parens-final.log` |
| pinned `bb code_sites.clj` + `python3 -I align.py ... --lean-source ... --require-lean ...` | 0 | 123 named entities from the three pinned modules; 15 K definition rows; `.../alignment-code-logs/pinned-extractor.log` |
| `git diff --check` | 0 | clean |

The checked source enumeration was invoked with
`/home/joe/code/futon2=c5db4719ee8fb3b857939d6a4899e4442be2f5e4`; it reads
committed content and did not load any application namespace.

## Required negative controls

- **Missing named symbol, red:** requiring
  `def=DarkTower.WarMachine.CheckedProblemMap.renamed` exited `2` before a
  `hypergraph.json` was written, with
  `alignment error: required def ... renamed is missing`
  (`.../alignment-code-scratch/missing.stderr`).
- **Renamed/unsupported anchor controls:** the focused suite covers an
  unresolvable `(K typo)` anchor (required mode fails before output), an
  unpinned source specification, private declarations, duplicate named
  declarations, comments/strings, and a no-anchor declaration that produces
  no edge.  The green suite restores the supported parser and succeeds.

This receipt establishes generator mechanics only.  No Lean annotation was
added, no generated canonical artifact was regenerated, and no edge asserts
runtime proof authority or proof admission.

## Follow-up lexical/pin repair (`8d5ae0b31` review hold)

Reviewer red evidence before this repair is retained at
`/home/joe/notes/wm-next/hypergraph-implementation/alignment-review-run/probe.json`:
both a multiline string and a nested documentation comment incorrectly made
`Demo.fake` a required definition, and `HEAD` was recorded literally.

After the repair:

| command | exit | result / log |
|---|---:|---|
| `python3 -I holes/labs/wm-contract/logic-model/alignment/test_align.py` | 0 | 40 tests, including multiline-string/doc-looking ghost, nested-doc ghost, fail-before-output, and `HEAD` → full-commit controls; `/home/joe/notes/wm-next/hypergraph-implementation/alignment-followup-logs/test-align-final.log` |
| pinned extractor using short `3836beefbc` | 0 | recorded `3836beefbc68cc2060ff54f88447322f64f8ad77`, not `HEAD`/short SHA; `.../alignment-followup-logs/pinned-short-revision.log` |
| `git diff --check` | 0 | clean |

The supported parser remains intentionally narrow: named `def`/`theorem`,
namespace plus section/end scopes, dotted declaration names, and
`noncomputable def`.  Private declarations, unmatched/mismatched ends,
outside-namespace declarations, and documentation comments with trailing
source are typed unsupported findings; required entries reject rather than
invent public symbols.  It is not a Lean elaborator.
