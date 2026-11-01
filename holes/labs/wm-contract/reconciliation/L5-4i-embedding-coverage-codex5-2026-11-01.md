# L5-4i — structure-embedding coverage and regeneration discovery

Discovery by codex-5, 2026-11-01. No producer was run and no data file was
written. Counts use `witnesses/witnesses-open-tasks-2026-11-01.edn` (626 ids)
and the files currently pinned by `resources/wm/focus/commit-facets-v1.json`.

## Finding

The retained corpus is not an embedding of the repositories' current open
holes. It is an embedding of the 198 mission names present in a substrate-2
scope snapshot written 2026-06-09. `mission_structure_embed.py` never scans a
hole directory. Consequently there is no existing command that regenerates
this corpus over the current 626 open ids across the Futon repositories. The
executable pipeline can refresh substrate-2's mission scopes and embed them;
covering the open-task witness first requires an ingestion/feature definition
for hole files, especially excursions and tickets.

## 1. What the embedding is

`futon6/scripts/mission_structure_embed.py` declares five inputs: the scope
dump, mission-wholeness classes, applied-pattern scopes, MiniLM mission vectors,
and mined moves (`mission_structure_embed.py:21-26`, constants at lines 38-44).
Only the first three determine the structural matrix. MiniLM is a comparison
baseline and the moves file is used only for two printed witness-neighbour
examples (`mission_structure_embed.py:148-174,183-228`).

The node population is `group-by :mission` over
`futon6/data/diffsub-scopes.json`; an item without a `mission` field does not
enter (`mission_structure_embed.py:76-83`). The scope dump itself is obtained
from substrate-2's `mission-scope/<binder>` hyperedges at localhost:7071, not
from Markdown. It retains the scope id, binder, detached state, a truncated
passage, capability/concepts, and mission node
(`diffsub_scope_dump.py:19-24,27-31,43-66`). Thus “structure” means features of
the retained mission scope tree, not a dependency graph and not a text-model
embedding:

- eight canonical phases present and detached (8 + 8 bits);
- one of five wholeness classes;
- an 11-binder normalized histogram;
- seven size/frontier/shape scalars;
- detached-phase transition bigrams found in the corpus;
- applied-pattern presence for patterns occurring in at least two missions.

Those blocks are constructed at `mission_structure_embed.py:85-135`. Columns
are z-normalized across the corpus and rows are L2-normalized
(`mission_structure_embed.py:137-145`). There is no learned model on this path.
The retained result is 198 rows by 161 float64 dimensions: 118 pattern columns,
4 bigram columns, and 39 fixed columns. `mission-embed.json` contains parallel
`stems` and `meta` arrays (class, scope count, detached count, present phases),
the dimension counts, class-match summary, and two witness-provenance results;
the numeric rows are in `structure-embeddings.npy`
(`mission_structure_embed.py:230-235`). It contains no source-repository or
hole-path field because no repository holes were scanned.

The relation consumer reads little-endian, C-order float64 NPY itself
(`focus_receipt.clj:203-218`). It strips `M-` or `E-` from a target, looks that
stem up at the same index in JSON and NPY, and computes cosine to every
declared-row target whose stem is also present (`focus_receipt.clj:224-280`).
“Rowed” means the map `{target -> one of the nine declared relation rows}`, not
an embedding row. The nearest member of `rowed ∩ embedded` supplies the
relation; the receipt retains neighbour, cosine, runner-up, all candidates,
pins, and floor.

## 2. Coverage of the 626 open ids

Direct stem intersection, independent of whether a declared relation makes an
embedding lookup unnecessary:

| Prefix | Open | Nodes in embedding | Absent |
|---|---:|---:|---:|
| `M-` | 181 | 87 | 94 |
| `E-` | 255 | 1 (`E-superpod-mark3`) | 254 |
| `T-` | 190 | 0 | 190 |

The L5-4g census reports 89, rather than 94, unclassified `M-` targets because
five open non-nodes have direct declared rows and do not need embedding
derivation. The sole embedded open excursion is an accidental stem collision:
the scope dump has mission stem `superpod-mark3`; the reader deliberately
strips either `M-` or `E-` (`focus_receipt.clj:249-253`). The scope dump has no
`E-` mission names at all, so this is not excursion ingestion.

Of the 94 open mission non-nodes, Git history says 78 hole files were first
added on or after 2026-06-25 and 16 predate it. Restricting to the 89 that lack
a direct row gives 73 later additions and the same 16 older omissions. This
supports two causes: most did not exist when the artifact was produced, while
the older group lived in repository holes but never had a substrate-2 mission
scope in the June 9 snapshot. Their locations span futon0, futon1b, futon2,
futon3a, futon3c, futon4, futon5/futon5a, futon6, and futon7; repository is not
recorded in the embedding.

## 3. Regeneration and the missing step

The existing executable pipeline is:

```sh
cd /home/joe/code/futon6
.venv/bin/python scripts/diffsub_scope_dump.py
.venv/bin/python scripts/mission_structure_embed.py --out data/mission-structure-embed
```

The first command queries localhost:7071 and overwrites
`data/diffsub-scopes.json`; the second deterministically overwrites the JSON and
NPY (`diffsub_scope_dump.py:34-76`; `mission_structure_embed.py:177-235`). A
2026-10-31 read-only rehearsal recorded seconds for the dump when the service
is available and 0.25 s for embedding 198 stems on the stale dump
(`TIED-OUTER-CASCADES-zai2-2026-10-31.md:226-239`). Runtime is therefore
sub-second for the numeric build and ordinarily seconds for the bounded HTTP
snapshot, excluding any missing ingestion of repository holes.

There is **no exact current command** for the requested “current open set
across the Futon repos' holes.” `diffsub_scope_dump.py` only asks the service
for existing hyperedges and `mission_structure_embed.py` only groups that
dump. Neither reads the open-task witness or `holes/{missions,excursions,
tickets}`. A regeneration packet must therefore first specify and implement
how every open id becomes comparable structural features; merely running the
two commands above will not add the 343 missing M/E targets.

The current structural inputs, which a reproducible packet should pin, are:

| Input | Current SHA-256 |
|---|---|
| `futon6/data/diffsub-scopes.json` | `b9c0872705f6193c8fdb368fa13affa690b74f344bb8a95d30296ad8993fb051` |
| `futon6/data/mission-wholeness.edn` | `73c791f7b7d94ff5226569dd719b74e9f214d5ff2ae8f0242fb04fb01cfab07c` |
| `futon6/data/mission-pattern-scopes.edn` | `d1e0f4f3f8dcab516d43a9c5ff720affb1a02514acbb65964b689e50f54f4f3a` |

The producer currently emits no input manifest. The latter two inputs have
changed since the June output, so even rebuilding over the unchanged scope
dump is expected to change the feature vocabulary/dimension and both output
hashes. Adding nodes also changes corpus means and standard deviations
(`mission_structure_embed.py:137-143`), hence every existing row can change.
The 85 currently embedding-derived classifications must be recomputed: their
cosines can change and a newly admitted node does not itself become a candidate
unless it has a declared relation row, but normalization changes and newly
embedded anchors can change the nearest anchor. The scale is small enough for
an exact before/after census; no reliable cosine-distribution estimate follows
from node count alone.

## 4. Pins, update boundary, and consumers

The authoritative output pins are
`resources/wm/focus/commit-facets-v1.json:4438-4448`:

- `mission-embed.json` — `57ab4aa46319eed4f29a5b0d9f84c86e14f0d30c8ddca906ef45ca0d9ac100fb`;
- `structure-embeddings.npy` — `0c2d76be8ed53d4bdc2c52e3cd551baf43d7a31f62d510d0791633ad4ac3abe4`.

`focus_receipt.clj` selects those paths by suffix and hashes both at read time;
a missing file or mismatch returns typed absence rather than using the data
(`focus_receipt.clj:233-247`). Updating the corpus therefore requires computing
both new hashes and changing both pin rows with the artifact update before a
click consumes it. The JSON/NPY pair must also be treated atomically because
the JSON index orders the NPY rows. The resource's short `:embedding :nodes`
list is descriptive only; its declared authority is “node presence only; not
adjacency permission” (`commit-facets-v1.json:4424-4465`).

A grep across the canonical `~/code/futon*` checkouts found two production
readers of this mission corpus:

1. `futon2/src/futon2/aif/focus_receipt.clj:224-280` — pinned relation
   derivation.
2. `futon6/scripts/mission_mine_moves.py:27-48` — loads both files directly,
   without the futon2 pins, to borrow move priors from structural neighbours.

There are two non-production readers: the retained one-off diagnostic
`futon2/holes/labs/wm-contract/runs/fixlist-2026-09-21/improve-7-evidence/discover.py:28-36`
and a futon3c test that creates a tiny same-named fixture rather than reading
the canonical files (`futon3c/test/futon3c/diagramprover/wm_wire_class_products.clj:45-53`).
`clean_structure_embed.py` and `clean_graph_export.py` use the generic filename
`structure-embeddings.npy` for the separate CLean proof corpus; they are not
mission-corpus consumers.

## 5. The nine relation rows

There are exactly nine declared relations at
`commit-facets-v1.json:4287-4409`: six focus and three associated rows. They
remain the only labelled anchors after regeneration unless that reviewed
corpus is separately changed. In the retained 198-node embedding, only the
three associated targets (`M-action-cost-modelling`, `M-futonzero-generative`,
`M-aif4iad`) are nodes; none of the six focus targets is. Thus today's nearest
neighbour derivation chooses among three anchors. A corpus that admits every
current open mission could make five currently open focus rows embeddable (the
sixth focus target is not in the 626-open witness), yielding three to eight
usable anchors for that population, while the declared corpus remains nine.

This is a coverage finding, not a proposed relation rule: adding hundreds of
nodes without adding reviewed rows makes many more tasks inherit from the same
small three-to-nine-anchor set. Coverage and label diversity are separate
properties.
