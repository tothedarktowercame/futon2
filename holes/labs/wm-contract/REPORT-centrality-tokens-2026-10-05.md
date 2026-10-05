# Centrality tokens for the persisted 107-item ranking — 2026-10-05

Reproduce in a fresh process from `/home/joe/code/futon2`:

```sh
clojure -M scripts/centrality_tokens.clj
```

futon2 HEAD before generation: `5237a7598454df1b6ab76b69ce59340ce84418f3`. The 107 IDs and occurrence counts are read from the persisted ranking; no fresh selection is performed.

## Sources and relations

**[R] Persisted run record.** `data/wm-runs/tick-run-record-2026-10-05-c9d25d6a-f2bb-42bf-a162-2c4a000e804f.edn` / `9de9e4bbb286680ebc74daa642a1fd8987009238928bddef60b387954d4d10c2`. Its ranking rows carry `:pipeline-structural-occurrences`; their graph source pin is `http://127.0.0.1:7070/api/alpha/cascade-real/graph` / `1b811ed07186d95ac5a5919becbe5df425ff3894e3bc40a84e29d74d1ae9f7b0`.

**[G] Cascade Live graph fetched today.** `http://127.0.0.1:7070/api/alpha/cascade-real/graph` / `6d6a639dec5777746aebb135a7a8afd0b2cc004f11c9dc2103e89607f9cd9025`. It has 176 arrows, 176 distinct haves, 176 distinct wants, and 0 wants that canonicalise to a persisted task ID. The arrows relate each mission to its own next hole, not tasks to other tasks. Both requested degrees are therefore `:typed-absence` (`:arrows-are-mission-to-own-hole`). `:lineage` has 126 agent/target/session dispatch records; it is dispatch lineage, not predecessor/successor.

[G] `[:patterns :edges]` has 1715 rows, of which 1715 are `"applied"`; canonicalisation retained 1715 rows over 249 missions. Missions with no pattern edge get zero. The graph moved from reviewed SHA `2895d7132e86b388861e5d1b9a40588b4166082e197049ebd22632e996396858`; current SHA is `6d6a639dec5777746aebb135a7a8afd0b2cc004f11c9dc2103e89607f9cd9025`. The current `M-interim-director-proxy-metric-inventory` reference is (a) 34, (b) 68, matching 34/68.

**[P] Mined pattern graph.** `/home/joe/code/storage/operator-turns/mined-pattern-graph.json` / `9992e2c62e41453d9ae33ba6c660b20195c91006759c84a3154af858436612e5`. Its 4552 edges are pattern-to-pattern relations and contain no task-to-pattern relation: `:typed-absence` (`:no-task-to-pattern-relation`). It is not used to approximate [G].

**[L] claude-4 Lean rows record.** Agency reports mission `M-diagramprover`. A bounded search of `futon2/holes`, `futon3c/holes`, and `mathlib4/DarkTower` for claude-4 plus rows/unblock/sorry found no defining or tabulating record: `:typed-absence` (`:no-recorded-lean-row-unblocking-measure-found`). No number was recomputed.

## Measures and token

(a) counts distinct applied patterns; (b) counts distinct **other** missions sharing at least one pattern; (c) sums, over the item's patterns, the number of other missions applying that pattern. Thus (c) can count one mission repeatedly.

| task kind | items | mean (a) | mean (b) | mean (c) |
|---|---:|---:|---:|---:|
| `:excursion` | 3 | 0.000000 | 0.000000 | 0.000000 |
| `:mission` | 104 | 3.048077 | 12.240385 | 17.038462 |

Proposed token: `:meta/downstream-unblocking-count`, type `:non-negative-integer`, emitted by the centrality reading square and consumed by `minimise-g-over-filled-meta-policies` through `:downstream-unblocking`. Its value is either (b) or (c), to be chosen at B3; this report does not choose.

## Correlations

Spearman rho uses average ranks for ties over 107 items. No rho is reported for arrow degrees: the task-to-task relation is absent, and treating absence as zero would manufacture a measure.

| comparison with persisted occurrence count | rho |
|---|---:|
| (a) distinct patterns | 0.895713 |
| (b) shared missions | 0.823658 |
| (c) shared incidences | 0.839647 |

## Per-item values

| id | kind | occurrence | arrow out | arrow in | (a) | (b) | (c) | [P] relation |
|---|---|---:|---|---|---:|---:|---:|---|
| `M-interim-director-proxy-metric-inventory` | `:mission` | 33 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 34 [G] | 68 [G] | 151 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-categorical-code` | `:mission` | 23 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 14 [G] | 53 [G] | 67 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-or-training-as-learning-system` | `:mission` | 20 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 15 [G] | 32 [G] | 42 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-simulating-or-training-as-learning-system` | `:mission` | 17 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 15 [G] | 37 [G] | 80 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-the-perfect-crime` | `:mission` | 19 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 2 [G] | 2 [G] | 2 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-futonzero-mvp` | `:mission` | 15 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 13 [G] | 32 [G] | 45 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-vsatarcs-writer` | `:mission` | 13 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 11 [G] | 33 [G] | 68 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-vsatarcs-invariants-integration` | `:mission` | 13 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 11 [G] | 58 [G] | 82 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-weird-modernism` | `:mission` | 12 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 10 [G] | 35 [G] | 67 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-futon-enrichment` | `:mission` | 11 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 9 [G] | 48 [G] | 62 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-stack-stereolithography` | `:mission` | 9 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 7 [G] | 18 [G] | 31 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-essays-edit-cycle` | `:mission` | 9 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-trip-journal` | `:mission` | 8 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 6 [G] | 26 [G] | 39 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-pattern-application-diagnostic` | `:mission` | 8 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 6 [G] | 35 [G] | 53 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-patterns-done-right` | `:mission` | 8 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 6 [G] | 12 [G] | 16 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-joe-told-me-about-futon` | `:mission` | 7 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 5 [G] | 20 [G] | 21 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-a-sorry-enterprise` | `:mission` | 7 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 4 [G] | 27 [G] | 34 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-expressions-of-interest` | `:mission` | 8 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 6 [G] | 15 [G] | 24 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-web-arxana-ui-improvements` | `:mission` | 6 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 4 [G] | 16 [G] | 21 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-goals-and-holes` | `:mission` | 13 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 11 [G] | 38 [G] | 50 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-the-futon-stack` | `:mission` | 6 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 4 [G] | 48 [G] | 60 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-custom-harness` | `:mission` | 10 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 8 [G] | 41 [G] | 51 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-stack-geometry` | `:mission` | 5 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 2 [G] | 7 [G] | 8 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-pattern-posteriors` | `:mission` | 5 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 4 [G] | 24 [G] | 30 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-value-creation-loop` | `:mission` | 6 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 6 [G] | 5 [G] | 11 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-recommendation-bindings` | `:mission` | 4 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 3 [G] | 24 [G] | 26 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-pattern-retrieval-calibration` | `:mission` | 4 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 3 [G] | 10 [G] | 10 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-aif-a-matrix-faithfulness` | `:mission` | 6 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 6 [G] | 32 [G] | 43 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-pattern-mining` | `:mission` | 4 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 2 [G] | 10 [G] | 10 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-digital-nomad-patterns` | `:mission` | 5 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 5 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-formal-war-machine` | `:mission` | 5 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 5 [G] | 36 [G] | 36 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-self-improvement-loop` | `:mission` | 3 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 1 [G] | 4 [G] | 4 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-war-machine-wiring` | `:mission` | 3 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 1 [G] | 11 [G] | 11 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-mission-coherence-patterns` | `:mission` | 4 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 2 [G] | 9 [G] | 9 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-hypergraph-operator` | `:mission` | 3 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 2 [G] | 2 [G] | 2 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-essay-corpus-substrate` | `:mission` | 3 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 2 [G] | 11 [G] | 11 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-futonzero-generative` | `:mission` | 3 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-zaif-harness-v1` | `:mission` | 3 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 3 [G] | 1 [G] | 1 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `E-kimi-task-28` | `:excursion` | 3 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-codex-sorry-loop` | `:mission` | 3 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 2 [G] | 10 [G] | 10 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-daily-scan` | `:mission` | 3 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 1 [G] | 5 [G] | 5 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-interim-director` | `:mission` | 3 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 2 [G] | 20 [G] | 21 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-shared-memory-control-build-test` | `:mission` | 3 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 2 [G] | 20 [G] | 20 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-war-machine-pilot` | `:mission` | 20 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 18 [G] | 41 [G] | 69 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-warrant-limit` | `:mission` | 3 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-zai-learning-loop` | `:mission` | 3 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-differentiable-code` | `:mission` | 2 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 1 [G] | 1 [G] | 1 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-inbox-zero-claim-lifecycle` | `:mission` | 2 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 1 [G] | 1 [G] | 1 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-aif-faithfulness` | `:mission` | 2 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-operational-vocabulary` | `:mission` | 2 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 1 [G] | 1 [G] | 1 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-autoclock-in` | `:mission` | 2 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-autonomous-doc-maintenance` | `:mission` | 2 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 1 [G] | 10 [G] | 10 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-essays-diachronic-model` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-pudding-peradams` | `:mission` | 2 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-war-machine-frontend-upgrade1` | `:mission` | 2 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 1 [G] | 35 [G] | 35 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-landing-practice` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-mission-scopes-into-substrate-2` | `:mission` | 9 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 7 [G] | 54 [G] | 76 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-action-cost-modelling` | `:mission` | 9 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 8 [G] | 64 [G] | 91 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-tpg-coupling-evolution` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-xor-coupling-probe` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-webarxana` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-war-machine-vsatarcs-interop` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-web-arxana-missions` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-fulab-wiring-survey` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-coupling-as-constraint` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-essays-retraction-visibility` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-arxana-roundtrip` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-editorial-assistant` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-reachable-from-boot` | `:mission` | 4 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 2 [G] | 40 [G] | 46 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `E-cascade-real` | `:excursion` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `E-kimi-task-70` | `:excursion` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-a-wmc-scaling` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-aif-ants-port` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-aif-policy-conditioned-eig` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-another-university` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-buyer-discovery` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-daily-scan-multi-axis-queue` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-interim-director-long-tail` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-signal-roll-up` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-turns-first` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-war-machine-aif-last-mile` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-state-snapshot-witness` | `:mission` | 3 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 1 [G] | 10 [G] | 10 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-xenotype-its` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 1 [G] | 3 [G] | 3 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-single-entry-point` | `:mission` | 3 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 1 [G] | 3 [G] | 3 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-typed-memories` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 1 [G] | 7 [G] | 7 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-bounded-disposition` | `:mission` | 3 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 1 [G] | 10 [G] | 10 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-stack-hud-refactor` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-superpod-mark3` | `:mission` | 10 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 8 [G] | 15 [G] | 27 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-aif4iad` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-open-learning-system` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-patchboard-viz` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 1 [G] | 2 [G] | 2 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-reflective-discipline` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-wm-aif-policy-grain-compliance` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 1 [G] | 13 [G] | 13 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-smart-emacs-cursor` | `:mission` | 2 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 1 [G] | 15 [G] | 15 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-chipwitz-corps` | `:mission` | 2 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 1 [G] | 1 [G] | 1 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-artificial-stack-exchange` | `:mission` | 6 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 4 [G] | 10 [G] | 15 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-metric-harness` | `:mission` | 2 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 1 [G] | 2 [G] | 2 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-intent-curvature` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-federated-agency-hardening` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-differentiable-substrate` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-canon-fingerprint-store` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-differentiable-math` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-prior-mathematics` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-superpod-mark2` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-hyperreal-dictionary-planning` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-paper-reverse-morphogenesis` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |
| `M-distributed-frontiermath` | `:mission` | 1 [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | 0 [G] | 0 [G] | 0 [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |

## What the numbers say

Across the 107 rows, occurrence count has Spearman rho 0.895713, 0.823658, and 0.839647 with (a), (b), and (c). [G] supplies these via applied-pattern edges. Its arrows and lineage describe different relations; [P] and [L] remain typed absences.
