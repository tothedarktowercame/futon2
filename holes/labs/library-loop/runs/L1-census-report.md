# L1 — Rationale-reachability census (receipt)

Row `:L1`, class `:M` (map/discovery). Library parsed read-only at futon3 git
sha `9ef7bc181549fef54d3598c2e063cd6c3a324a1a`, from an isolated worktree
(`git worktree add --detach /tmp/futon3-L1-9ef7bc18 9ef7bc1815…`) so the
census reproduces from the committed tree. Nothing under
`/home/joe/code/futon3` was modified. The canonical working tree additionally
holds one UNTRACKED pattern
(`library/math-formalization/fixed-field-equality-via-degree-and-automorphism-count.flexiarg`)
not part of that commit: it carries no `@why`/`@how` and no edges, so every
count below is identical in both trees except `patterns total`
(1257 canonical / 1256 committed). Artifacts here are generated from the
committed tree.

Artifacts (both byte-identical on rerun, md5 over three runs from two differently named isolated worktrees (/tmp/l1wt-alpha, /home/joe/code/futon3-L1-rerun-zai1) — serialized provenance is path-independent (repo-relative subdir + HEAD sha), so any checkout of the same commit yields a byte-identical receipt):
- `L1-census-receipt.edn` — counts, full annotation-key histogram, unresolved refs
- `L1-census-graph.edn` — graph as nodes+edges EDN (3830 edges, `{:from :kind :to :resolved}`)
- `l1_census.py` — the parser (deterministic: sorted walks, sorted emission; takes the library root as argv[1], defaulting to the canonical checkout, and records that root's HEAD plus any dirty/untracked .flexiarg)

## Headline counts

| measure | count |
|---|---|
| patterns total (`.flexiarg` parsed, committed tree) | 1256 (1257 incl. 1 untracked, see above) |
| carrying `@why` | 83 |
| carrying `@why-posthoc` only | 7 |
| carrying `@how` | 21 |
| carrying `@why` or `@how` (unique files) | 99 |
| problem-stating nodes (`library/problems/*`) | 6 |
| WR nodes (`library/war-room/wr-*`) | 28 |

## Grep floor: confirmed at 106

    cd /home/joe/code/futon3/library && grep -rlE '@(why|how)' --include='*.flexiarg' .

returns 106 files in the committed tree (the untracked 1257th pattern adds nothing) (91 with `@why`, 22 with `@how`, 7 overlap). The bare grep
slightly over- and under-counts real annotations: it matches `@why-posthoc`
(8 lines) and prose/comment mentions of `@why` (e.g.
`problems/refusal-prediction-error-v1--source-field-missing.flexiarg`, which
mentions `@why` only in comments and carries none). The census parser takes
only line-leading `@key value` annotations, giving 83 files with `@why`
proper + 7 with `@why-posthoc` only = 90 files with rationale annotations.

## @why reachability — both readings reported

No `@why` in the library points at a `problems/*` node, so the direction of
"reachable from a problem-stating node via @why" decides the number. Both
readings are computed from the same serialized graph; the choice between them
is a decision reserved elsewhere (feeds L4/F7/F8).

- **Reading A (upward)** — pattern's own `@why` chain terminates at a
  problem-stating node: 14/1256 from `problems/*`; 34/1256 if `war-room/wr-*`
  nodes also count as problem-stating.
- **Reading B (downward)** — a problem-stating node justifies the pattern
  along `@why` "justified-by" edges: 6/1256 (the problem nodes themselves —
  nothing's `@why` targets them); 53/1256 with WR nodes included.

Under every reading the ruling's observation holds: the overwhelming majority
of the 1256 committed patterns (≥95%) are unreachable via `@why` from any
problem-stating node.

## Other census facts worth the receipt

- 28 files have no `@flexiarg` id line (id taken from path; listed in receipt).
- 2004 of 3830 extracted reference edges do not resolve to an existing pattern
  id — most are `@references`/`@see-also` shorthand and external names; full
  list in the receipt under `:unresolved-refs`.
- Full annotation-key histogram (86 distinct keys, including the exotype block,
  `@holds-at`, `@verdict`, `@governance`, …) is in the receipt; this row
  extracts ALL annotations, not only `@how`/`@why`.
