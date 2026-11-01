# L5-23-D relation coverage census — codex-15, 2026-11-01

## Basis and scope

Read-only discovery against the first landed 2026-11-01 selection record carrying
`[:outer/selection :per-task-rating]`:

`/home/joe/code/futon2/data/wm-runs/selection-record-2026-11-01-wm-click-c62fb963-18a8-400f-8f04-03a5961b02a4.edn`

The record started at `2026-11-01T21:48:39.368776385Z`, names focus `"WM"`, and pins Giraffe run
`20261101T215017.254Z-0c21acba`. It contains **588** per-task-rating rows, not the packet's
provisional 591. Ranks have gaps and reach 603; the census counts actual retained rows. I replayed
`focus-receipt/classify-target` over those exact 588 targets with `read-inputs`, the record's
`startedAt`, retained focus `WM`, `relation-context "/home/joe/code"`, and the pinned Giraffe run
directory. Replay class counts equal the recorded rating classes exactly.

## 1. Coverage census

| Outcome | Count | Share of 588 |
|---|---:|---:|
| `:focus` | 6 | 1.0% |
| `:associated` | 154 | 26.2% |
| `:useful-elsewhere` | 0 | 0.0% |
| absent `:embedding-node-not-retained` | 232 | 39.5% |
| absent `:repair-target-absent` | 100 | 17.0% |
| absent `:embedding-below-floor` | 44 | 7.5% |
| absent `:no-parent-relation` | 33 | 5.6% |
| absent `:repaired-target-unknown` | 18 | 3.1% |
| absent `:relation-not-declared` | 1 | 0.2% |
| **Total** | **588** | **100%** |

The dominant absence is **`:embedding-node-not-retained` (232)**. Unknowns total 428; known
relations total 160. The derivation-source census is: direct/no derivation 316, open repair
obligation 150, Giraffe graph 105, embedding neighbour 10, ticket parent 7. Thus the premise
"589 of 591 unknown" describes the earlier click-1 situation, not this landed record: the graph
route now supplies most of the 160 known rows.

## 2. Anchors and stated-relation hops

The frozen focus input contains **9 classified anchor rows**:

1. `M-G-wm-wiring`
2. `M-action-cost-modelling`
3. `M-aif-policy-conditioned-eig`
4. `M-aif4iad`
5. `M-apm-capability-ratchet`
6. `M-f11-find-production-successor`
7. `M-futonzero-generative`
8. `M-wm-08-external-f2`
9. `M-wm-aif-policy-grain-compliance`

Seven anchors occur in the 588-row admitted field. Replaying only the stated-relations route with
`relation-hop-bound` rebound to 2, 3, then 4 found no additional target reaching an anchor:

| Stated hop bound | Derived tasks reaching an anchor | Including 7 direct admitted anchors |
|---:|---:|---:|
| 2 | 0 | 7 |
| 3 | 0 | 7 |
| 4 | 0 | 7 |

For this field, widening the stated hop bound buys **zero coverage** and costs additional file
reads/BFS work on the few relation-bearing documents. The productive multi-hop route in the live
result is the separately declared Giraffe graph (`:graph-hop-bound 3`), which derives 105 rows.

## 3. Stated relations in files

Using the producer's exact primary-file lookup (`<repo>/holes/missions/<target>.md`, then
`<repo>/holes/<target>.md`):

| File/Relations property | Tasks |
|---|---:|
| admitted targets | 588 |
| task text found by the producer | 216 |
| readable `## Relations` section | 10 |
| non-empty `## Relations` section | 10 |
| section names an `M-` target in the admitted field | 6 |
| relation edges named | 25 |
| relation edges whose target is in the admitted field | 7 |

Only 10/588 tasks expose a Relations section to this producer, and none connects to a classified
anchor within four hops. Most tasks state no readable relation, so no stated-hop increase can
materially improve coverage. Notice also that the lookup does not search `holes/excursions/`, which
contributes to the 372 targets for which this route finds no task text.

## 4. Embedding route

The click's current frozen input does declare `:embedding-floor 0.2`; it also declares graph bound
3, link kinds `relates-to`, `part-of`, `advances`, and basis `declared`. The embedding route runs:
10 rows are derived by `:embedding-neighbour`, 44 retained embedding nodes are below 0.2, and 232
targets have no retained embedding node. The floor is therefore already on at Joe's ruled value.

For diagnostic contrast only, replaying the same pinned input with floor 0.0 changes the census to
6 focus, 209 associated, 373 unknown. It derives 54 embedding neighbours: +44 known rows from the
current below-floor group, while 232 missing embedding nodes remain untouched. Lowering the floor
would therefore trade the ruled semantic threshold for only 7.5 percentage points of coverage;
it does not address the dominant absence.

## 5. Cost

Measured inside one tooling JVM over all 588 rows, with graph input memoization behaving as it
does for a click:

| Run | Wall time |
|---|---:|
| first pass | 2065 ms |
| warm pass | 1017 ms |
| floor-0 diagnostic pass | 980 ms |

The producer currently costs about **1.0 s warm / 2.1 s cold per click** for this field.

## Recommendation

**Extend the pinned structure embedding to the 232 admitted targets currently reported as
`:embedding-node-not-retained`, while retaining the declared 0.2 floor and graph parameters.**
This attacks the largest absence directly. The observed retained-node pass rate is 10/(10+44) =
18.5% at floor 0.2; applying that measured rate to 232 newly retained nodes predicts about **43
additional known relations**, raising known coverage from 160/588 (27.2%) to approximately
203/588 (34.5%). Widening stated hops predicts zero gain, and lowering the floor contradicts the
0.2 ruling while adding only the already-measured 44 rows.
