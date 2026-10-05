# Centrality tokens for the persisted 107-item ranking — 2026-10-05

Reproduce in a fresh process from `/home/joe/code/futon2`:

```sh
clojure -M scripts/centrality_tokens.clj
```

futon2 HEAD: `a48c11aadd28d2bec4106933632436da01fd916c`. The item population and live occurrence counts come from the persisted run ranking; no fresh selection is performed.

## Sources and relations

**[R] Persisted run record.** `data/wm-runs/tick-run-record-2026-10-05-c9d25d6a-f2bb-42bf-a162-2c4a000e804f.edn` / `9de9e4bbb286680ebc74daa642a1fd8987009238928bddef60b387954d4d10c2`. It contains 107 ranking rows (:excursion 3, :mission 104), each with the selector's `:pipeline-structural-occurrences` observation. Its graph source pin is `http://127.0.0.1:7070/api/alpha/cascade-real/graph` / `1b811ed07186d95ac5a5919becbe5df425ff3894e3bc40a84e29d74d1ae9f7b0`.

**[G] Cascade Live graph fetched today.** `http://127.0.0.1:7070/api/alpha/cascade-real/graph` / `2895d7132e86bb20039ce2f7cf3a000c54f81c897650ea3f5cb081ac8e5f949f`. Top-level keys: `:arrows`, `:as-of-ms`, `:clusters`, `:counts`, `:held`, `:holes`, `:lineage`, `:patterns`, `:section-status`, `:tickets`. It contains 176 arrows; each arrow exposes `:have` and `:want`. Its bytes do not match the run's graph pin; the table therefore labels persisted occurrences [R] and today's degrees [G] separately.

**[P] Mined pattern graph.** `/home/joe/code/storage/operator-turns/mined-pattern-graph.json` / `9992e2c62e41453d9ae33ba6c660b20195c91006759c84a3154af858436612e5`. Top-level keys: `:edges`, `:giant_without_weak_edges`, `:pattern_ids`, `:patterns`, `:records`, `:summary`, `:象_family`. It contains 4552 pattern edges with kinds {"co-cited" 855, "how" 90, "next-in-session" 2724, "rejected-beside" 338, "why" 545}. It contains 0 task-to-pattern links, so `pattern-shared-count` is `:typed-absence` (`:no-task-to-pattern-relation`) for every ranked item.

**[L] claude-4 Lean rows record.** Agency reports claude-4's mission as `M-diagramprover`. A bounded search of `futon2/holes`, `futon3c/holes`, and `mathlib4/DarkTower` for claude-4 plus rows/unblock/sorry found no record defining or tabulating the requested measure. Status: `:typed-absence`; reason: `:no-recorded-lean-row-unblocking-measure-found`. No number was recomputed.

## Measure and token

For missions, excursions, and tickets, `have→want-out-degree` is the number of distinct **other** canonical task IDs appearing as the `:want` endpoint of an arrow whose `:have` endpoint is the item. `have→want-in-degree` reverses that question: distinct other `:have` endpoints on arrows whose `:want` is the item. The ranked population has no algorithm rows; the separately requested Lean-row definition is [L]'s typed absence.

| task kind | ranked items | mean out-degree | max out-degree | mean in-degree | max in-degree |
|---|---:|---:|---:|---:|---:|
| `:excursion` | 3 | 0.000000 | 0 | 0.000000 | 0 |
| `:mission` | 104 | 0.653846 | 1 | 0.000000 | 0 |

Proposed token: `:meta/downstream-unblocking-count`, type `:non-negative-integer`, value `have→want-out-degree`, with the graph source pin carried alongside it. The proposed centrality reading square emits it; `minimise-g-over-filled-meta-policies` consumes it through the existing generative-model outcome `:downstream-unblocking`. This names the measured value only; it does not recommend a scoring transform.

## Correlations

Spearman's rho uses average ranks for ties over all 107 persisted items. Occurrence count is oriented as a count (larger means more occurrences), not as the selector's inverse cost.

| comparison with live occurrence count | rho |
|---|---:|
| have→want out-degree | -0.014956 |
| have→want in-degree | typed absence (`:undefined-zero-variance`; all 107 values are 0) |
| pattern-shared count | typed absence (`:no-task-to-pattern-relation`) |

## Per-item values

| id | kind | live occurrence count | have→want out-degree | have→want in-degree | pattern-shared count |
|---|---|---:|---:|---:|---|
| `M-interim-director-proxy-metric-inventory` | `:mission` | 33 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-categorical-code` | `:mission` | 23 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-or-training-as-learning-system` | `:mission` | 20 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-simulating-or-training-as-learning-system` | `:mission` | 17 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-the-perfect-crime` | `:mission` | 19 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-futonzero-mvp` | `:mission` | 15 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-vsatarcs-writer` | `:mission` | 13 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-vsatarcs-invariants-integration` | `:mission` | 13 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-weird-modernism` | `:mission` | 12 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-futon-enrichment` | `:mission` | 11 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-stack-stereolithography` | `:mission` | 9 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-essays-edit-cycle` | `:mission` | 9 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-trip-journal` | `:mission` | 8 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-pattern-application-diagnostic` | `:mission` | 8 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-patterns-done-right` | `:mission` | 8 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-joe-told-me-about-futon` | `:mission` | 7 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `M-a-sorry-enterprise` | `:mission` | 7 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-expressions-of-interest` | `:mission` | 8 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `M-web-arxana-ui-improvements` | `:mission` | 6 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-goals-and-holes` | `:mission` | 13 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `M-the-futon-stack` | `:mission` | 6 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-custom-harness` | `:mission` | 10 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `M-stack-geometry` | `:mission` | 5 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-pattern-posteriors` | `:mission` | 5 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `M-value-creation-loop` | `:mission` | 6 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `M-recommendation-bindings` | `:mission` | 4 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `M-pattern-retrieval-calibration` | `:mission` | 4 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `M-aif-a-matrix-faithfulness` | `:mission` | 6 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `M-pattern-mining` | `:mission` | 4 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-digital-nomad-patterns` | `:mission` | 5 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `M-formal-war-machine` | `:mission` | 5 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `M-self-improvement-loop` | `:mission` | 3 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-war-machine-wiring` | `:mission` | 3 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-mission-coherence-patterns` | `:mission` | 4 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `M-hypergraph-operator` | `:mission` | 3 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `M-essay-corpus-substrate` | `:mission` | 3 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `M-futonzero-generative` | `:mission` | 3 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-zaif-harness-v1` | `:mission` | 3 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `E-kimi-task-28` | `:excursion` | 3 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `M-codex-sorry-loop` | `:mission` | 3 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `M-daily-scan` | `:mission` | 3 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-interim-director` | `:mission` | 3 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-shared-memory-control-build-test` | `:mission` | 3 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `M-war-machine-pilot` | `:mission` | 20 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-warrant-limit` | `:mission` | 3 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `M-zai-learning-loop` | `:mission` | 3 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `M-differentiable-code` | `:mission` | 2 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `M-inbox-zero-claim-lifecycle` | `:mission` | 2 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `M-aif-faithfulness` | `:mission` | 2 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `M-operational-vocabulary` | `:mission` | 2 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `M-autoclock-in` | `:mission` | 2 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-autonomous-doc-maintenance` | `:mission` | 2 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-essays-diachronic-model` | `:mission` | 1 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-pudding-peradams` | `:mission` | 2 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-war-machine-frontend-upgrade1` | `:mission` | 2 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-landing-practice` | `:mission` | 1 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-mission-scopes-into-substrate-2` | `:mission` | 9 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-action-cost-modelling` | `:mission` | 9 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `M-tpg-coupling-evolution` | `:mission` | 1 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-xor-coupling-probe` | `:mission` | 1 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-webarxana` | `:mission` | 1 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-war-machine-vsatarcs-interop` | `:mission` | 1 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-web-arxana-missions` | `:mission` | 1 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-fulab-wiring-survey` | `:mission` | 1 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-coupling-as-constraint` | `:mission` | 1 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-essays-retraction-visibility` | `:mission` | 1 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-arxana-roundtrip` | `:mission` | 1 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-editorial-assistant` | `:mission` | 1 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-reachable-from-boot` | `:mission` | 4 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `E-cascade-real` | `:excursion` | 1 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `E-kimi-task-70` | `:excursion` | 1 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `M-a-wmc-scaling` | `:mission` | 1 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `M-aif-ants-port` | `:mission` | 1 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `M-aif-policy-conditioned-eig` | `:mission` | 1 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `M-another-university` | `:mission` | 1 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-buyer-discovery` | `:mission` | 1 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-daily-scan-multi-axis-queue` | `:mission` | 1 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-interim-director-long-tail` | `:mission` | 1 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-signal-roll-up` | `:mission` | 1 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-turns-first` | `:mission` | 1 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `M-war-machine-aif-last-mile` | `:mission` | 1 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-state-snapshot-witness` | `:mission` | 3 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-xenotype-its` | `:mission` | 1 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `M-single-entry-point` | `:mission` | 3 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-typed-memories` | `:mission` | 1 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `M-bounded-disposition` | `:mission` | 3 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-stack-hud-refactor` | `:mission` | 1 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-superpod-mark3` | `:mission` | 10 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-aif4iad` | `:mission` | 1 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-open-learning-system` | `:mission` | 1 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-patchboard-viz` | `:mission` | 1 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `M-reflective-discipline` | `:mission` | 1 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-wm-aif-policy-grain-compliance` | `:mission` | 1 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `M-smart-emacs-cursor` | `:mission` | 2 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `M-chipwitz-corps` | `:mission` | 2 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `M-artificial-stack-exchange` | `:mission` | 6 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-metric-harness` | `:mission` | 2 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `M-intent-curvature` | `:mission` | 1 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-federated-agency-hardening` | `:mission` | 1 [R] | 0 [G] | 0 [G] | `:typed-absence` [P] |
| `M-differentiable-substrate` | `:mission` | 1 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-canon-fingerprint-store` | `:mission` | 1 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-differentiable-math` | `:mission` | 1 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-prior-mathematics` | `:mission` | 1 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-superpod-mark2` | `:mission` | 1 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-hyperreal-dictionary-planning` | `:mission` | 1 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-paper-reverse-morphogenesis` | `:mission` | 1 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |
| `M-distributed-frontiermath` | `:mission` | 1 [R] | 1 [G] | 0 [G] | `:typed-absence` [P] |

## What the numbers say

All 107 persisted rows have an occurrence count. Today's direct have→want graph has out-degree values from 0 to 1 and in-degree values from 0 to 0. Their Spearman correlations with the persisted occurrence count are -0.014956 and typed absence, respectively. The mined pattern graph cannot contribute an item-level value because it records pattern-to-pattern relations but no task-to-pattern relation; the requested Lean-row comparison is likewise absent from the searched record.

## Review addendum (claude-2, 2026-10-05 23:2xZ)

Two of the relations above are not what the table says they are; both
found by reading the graph by hand (`GET /api/alpha/cascade-real/graph`,
fetched again at review time).

1. **The have→want arrows are not inter-item.** All 176 arrows run from a
   mission to a node named for its own next hole
   (`futon0-d/mission/capability-star-map` →
   `…/capability-star-map-document`, `:move-class :close-hole`); 176
   distinct haves, 176 distinct wants, no want is a ranked item. So
   `have→want-out-degree ∈ {0, 1}` means "this mission has a close-hole
   arrow", `in-degree = 0` is forced, and the Spearman ρ of −0.015 compares
   occurrence count with a flag. The cell should read `:typed-absence
   (:arrows-are-mission-to-own-hole)`. `lineage` is likewise not
   predecessor/successor but agent-dispatch lineage (`agent`, `target`,
   `session`); `predecessor`/`successor` occur 0 times.
2. **The task→pattern relation exists, in [G] not [P].** `:patterns :edges`
   holds 1,715 mission→pattern links (`:relation "applied"`) over 249
   missions. From it, by hand: the selected mission
   `M-interim-director-proxy-metric-inventory` applies 34 distinct patterns
   and shares at least one with 68 other missions (rank 8 of 249 by that
   count; top: `e-pipeline-pipecleaner` 143, `g-over-cascades` 110). The
   live selector's "occurrence count" (33 for the selected mission in the
   run's graph) is, for a mission with no cluster, held or lineage
   mentions, essentially its count of applied patterns — a measure of how
   much it documents, not of what it unblocks.

So B1 as it stands establishes a typed absence for three of the four
sources and leaves the one inter-item relation that does exist
(shared applied patterns) uncomputed. Amendment requested: compute per
ranked item, from [G] `:patterns :edges`, (a) distinct applied patterns,
(b) number of other missions sharing ≥ 1 pattern, (c) sum over its
patterns of the number of other missions applying each; report ρ of each
against the occurrence count; retype the arrow cells as above. The token
proposal stands in name; its value becomes (b) or (c), to be chosen when
B3 is written.
