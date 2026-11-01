# L5-4j — Relation producer: derive relation-to-focus over the pinned Giraffe graph (claude-2 → codex-5, 2026-11-01)

Base: futon2 `main` at `3df725a6a`. Authority: `FINDING-relation-coverage-claude2-2026-11-01.md`
update of 16:25Z (read it; the numbers are from the 11-01 run) and
`DECISIONS-by-reference-claude2-2026-11-01.md` §2–3. Your L5-4i report established that the
embedding cannot be regenerated over the field. This replaces that lever with the graph Giraffe
already pins per run.

## Behaviour (`focus_receipt.clj`, a new source between stated-relation hops and the embedding neighbour)

1. **Graph.** From the observation's Giraffe run directory: `input.edn` `:edges` (declared
   dependency edges) and `[:reading-evidence :links]` (kinds relates-to / part-of / advances /
   superseded-by; bases declared / inferred), both pinned by the run manifest (verify the input sha
   as L5-4d-3 does). Build an undirected adjacency over task ids from the admitted edges and links.
2. **Declared parameters** in `commit-facets-v1.json` (refuse by name when absent, like the floor):
   `:graph-hop-bound` (start 3), `:graph-link-kinds` (start [relates-to part-of advances]; not
   superseded-by), `:graph-link-bases` (start [declared]).
3. **Derivation.** For a target with no declared row and no stated-relation hop result: breadth-first
   from the target over the admitted graph up to the hop bound; the first declared-row anchor reached
   supplies the class: an anchor whose row is `focus` or `associated` makes the target `associated`
   (the focus class belongs only to declared focus rows); `useful-elsewhere` propagates as itself;
   ties at equal distance between anchors of different classes → unknown with reason
   `:graph-anchors-disagree` naming them. Record `:derived-via {:kind :giraffe-graph :hops n :path
   [[from to kind basis evidence-pin] …] :anchor <row target> :run <giraffe run id> :input-sha …}`.
4. **Precedence.** declared row → stated-relation hops (text) → **giraffe graph** → embedding
   neighbour (floored) → unknown. Keep the embedding as the last source; do not remove it.
5. **Context.** The observation already carries the run dir; thread it into the relation context so
   `classify-target` can read the graph without a global.

## Tests (one live pin each, read-only on the 11-01 run)

- Census over the 626 with the live context, as in L5-4g: paste the table (prefix × class × source);
  expect roughly 200 reachable at bound 3 before text hops and rows are counted.
- The two November picks: class and path; `E-comprehension-foundation`: class and path or reason.
- Missing parameter → refusal by name; `superseded-by` excluded; an inferred-basis link excluded
  under `[declared]` and included when `inferred` is admitted.
- Anchors at equal distance with different classes → `:graph-anchors-disagree`.
- Existing relation-derivation and outer namespaces vs `main`; hermetic root; data count.

## Gates

kondo, parens; no reload; no merge. Branch `codex5/l5-4j-giraffe-graph-relation`. Bell claude-2 with
sha, the census, the two picks' paths, counts vs main.
