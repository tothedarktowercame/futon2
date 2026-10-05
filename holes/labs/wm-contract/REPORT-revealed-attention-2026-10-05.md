# Revealed attention and co-work — 2026-10-05

Reproduce from `/home/joe/code/futon2` in a fresh process:

```sh
clojure -M scripts/revealed_attention.clj
```

futon2 HEAD before generation: `ff22a287621599254816b9c65d565edc19e00fe8`. Ranked population: `data/wm-runs/tick-run-record-2026-10-05-c9d25d6a-f2bb-42bf-a162-2c4a000e804f.edn` / `9de9e4bbb286680ebc74daa642a1fd8987009238928bddef60b387954d4d10c2`.

## Sources

**(i) Dispatch lineage.** Cascade Live `:lineage`, `http://127.0.0.1:7070/api/alpha/cascade-real/graph` / `bfc3b14b4c26271eae481a09e2be00932beb015a0c65d81c39cfe475d081d860`, contains 126 records over 29 canonical targets. Counts of absent ranked items are zero with this pin.

Reference counts at this read are `{"M-diagramprover" 25, "M-apm-demonstration" 22, "M-the-perfect-crime" 14, "E-aif-cascade" 1}` (the earlier hand reference was `{:diagramprover 25 :apm-demonstration 22 :the-perfect-crime 14 :E-aif-cascade 1}`).

**(ii) Operator turns.** `turn-traced` is backed by operator chat-turn evidence shaped by `/home/joe/code/futon3c/src/futon3c/xiang/turn_record.clj` / `4fab30ec8e90d2f0f2fcccb712e8ee97bbfcaf6eca9a2421a734faf02e94c69d` and served by futon1b's `/api/alpha/evidence` endpoint. This run paged 6 response(s), aggregate pin `http://127.0.0.1:7073/api/alpha/evidence?author=joe&since=2026-09-22T00%3A00%3A00Z&limit=1000&before=<pagination>` / `03e908e715092fe2d1250d18ec590e53df39f7a5cece3fdda71dfab92ce26f79`, for the UTC calendar window `[2026-09-22T00:00:00Z, 2026-10-06T00:00:00Z)`. It found 4087 operator chat turns; 3339 carry a mission-like field. Fields inspected, in precedence order, were `[:clocked-target :clocked-mission :mission-id :mission :excursion-id :ticket-id]`; populated-field frequencies were `{:clocked-target 3339, :excursion-id 634, :clocked-mission 2697, :mission-id 2697, :ticket-id 8}`. The older local export `/home/joe/code/storage/operator-turns/operator-turns.jsonl` / `4d952ba5ef4461ddf81127724d2af6a4cf6b0160c1cb1f07a2662f7d1acd3179` was inspected as the harvester output but not counted because it ends before the reporting date. An item with no matching turn receives zero because the mission relation exists.

**(iii) Co-work.** The undirected graph spans all 29 lineage targets and has 54 edges. An edge records one or more of `:same-session`, `:same-agent-utc-day`, or `:operator-turn-co-mention`; its degree is an enabler association, not dependency evidence.

## Per-item values

| id | kind | dispatches | sessions | dispatchers | first dispatch | last dispatch | operator turns 14d | co-work degree |
|---|---|---:|---:|---:|---|---|---:|---:|
| `M-interim-director-proxy-metric-inventory` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-categorical-code` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-or-training-as-learning-system` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-simulating-or-training-as-learning-system` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-the-perfect-crime` | `:mission` | 14 [G] | 0 [G] | 1 [G] | 2026-09-25T03:05:29.376Z [G] | 2026-09-25T12:56:01.244Z [G] | 305 [O] | 7 [C] |
| `M-futonzero-mvp` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-vsatarcs-writer` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-vsatarcs-invariants-integration` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-weird-modernism` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-futon-enrichment` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-stack-stereolithography` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-essays-edit-cycle` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-trip-journal` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-pattern-application-diagnostic` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-patterns-done-right` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-joe-told-me-about-futon` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-a-sorry-enterprise` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 8 [O] | 0 [C] |
| `M-expressions-of-interest` | `:mission` | 1 [G] | 0 [G] | 1 [G] | 2026-09-02T14:03:09.880Z [G] | 2026-09-02T14:03:09.880Z [G] | 0 [O] | 7 [C] |
| `M-web-arxana-ui-improvements` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-goals-and-holes` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-the-futon-stack` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-custom-harness` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-stack-geometry` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-pattern-posteriors` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-value-creation-loop` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-recommendation-bindings` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-pattern-retrieval-calibration` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-aif-a-matrix-faithfulness` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-pattern-mining` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-digital-nomad-patterns` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-formal-war-machine` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-self-improvement-loop` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-war-machine-wiring` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-mission-coherence-patterns` | `:mission` | 1 [G] | 1 [G] | 1 [G] | 2026-09-28T19:34:55.046Z [G] | 2026-09-28T19:34:55.046Z [G] | 49 [O] | 0 [C] |
| `M-hypergraph-operator` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-essay-corpus-substrate` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-futonzero-generative` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-zaif-harness-v1` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `E-kimi-task-28` | `:excursion` | 3 [G] | 3 [G] | 1 [G] | 2026-09-25T13:15:15.252Z [G] | 2026-09-25T13:17:11.448Z [G] | 14 [O] | 0 [C] |
| `M-codex-sorry-loop` | `:mission` | 1 [G] | 1 [G] | 1 [G] | 2026-08-01T11:21:55.580Z [G] | 2026-08-01T11:21:55.580Z [G] | 0 [O] | 2 [C] |
| `M-daily-scan` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-interim-director` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-shared-memory-control-build-test` | `:mission` | 1 [G] | 1 [G] | 1 [G] | 2026-07-23T09:54:27.847Z [G] | 2026-07-23T09:54:27.847Z [G] | 0 [O] | 0 [C] |
| `M-war-machine-pilot` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-warrant-limit` | `:mission` | 2 [G] | 2 [G] | 1 [G] | 2026-09-28T18:21:04.800Z [G] | 2026-09-28T18:21:09.085Z [G] | 39 [O] | 5 [C] |
| `M-zai-learning-loop` | `:mission` | 3 [G] | 3 [G] | 1 [G] | 2026-07-27T14:25:56.984Z [G] | 2026-08-12T11:40:21.783Z [G] | 0 [O] | 1 [C] |
| `M-differentiable-code` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-inbox-zero-claim-lifecycle` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 13 [O] | 0 [C] |
| `M-aif-faithfulness` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-operational-vocabulary` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-autoclock-in` | `:mission` | 1 [G] | 1 [G] | 1 [G] | 2026-09-25T20:38:05.532Z [G] | 2026-09-25T20:38:05.532Z [G] | 94 [O] | 7 [C] |
| `M-autonomous-doc-maintenance` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-essays-diachronic-model` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-pudding-peradams` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-war-machine-frontend-upgrade1` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-landing-practice` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-mission-scopes-into-substrate-2` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-action-cost-modelling` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-tpg-coupling-evolution` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-xor-coupling-probe` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-webarxana` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-war-machine-vsatarcs-interop` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-web-arxana-missions` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-fulab-wiring-survey` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-coupling-as-constraint` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-essays-retraction-visibility` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-arxana-roundtrip` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-editorial-assistant` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-reachable-from-boot` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `E-cascade-real` | `:excursion` | 1 [G] | 1 [G] | 1 [G] | 2026-10-01T01:08:45.627Z [G] | 2026-10-01T01:08:45.627Z [G] | 133 [O] | 4 [C] |
| `E-kimi-task-70` | `:excursion` | 1 [G] | 1 [G] | 1 [G] | 2026-09-26T16:43:22.182Z [G] | 2026-09-26T16:43:22.182Z [G] | 10 [O] | 0 [C] |
| `M-a-wmc-scaling` | `:mission` | 1 [G] | 0 [G] | 1 [G] | 2026-09-28T23:03:55.269Z [G] | 2026-09-28T23:03:55.269Z [G] | 136 [O] | 7 [C] |
| `M-aif-ants-port` | `:mission` | 1 [G] | 1 [G] | 1 [G] | 2026-07-14T22:05:51.400Z [G] | 2026-07-14T22:05:51.400Z [G] | 0 [O] | 0 [C] |
| `M-aif-policy-conditioned-eig` | `:mission` | 1 [G] | 1 [G] | 1 [G] | 2026-09-29T04:07:23.894Z [G] | 2026-09-29T04:07:23.894Z [G] | 6 [O] | 7 [C] |
| `M-another-university` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-buyer-discovery` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-daily-scan-multi-axis-queue` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-interim-director-long-tail` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-signal-roll-up` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-turns-first` | `:mission` | 1 [G] | 1 [G] | 1 [G] | 2026-09-14T23:05:37.797Z [G] | 2026-09-14T23:05:37.797Z [G] | 0 [O] | 4 [C] |
| `M-war-machine-aif-last-mile` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-state-snapshot-witness` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-xenotype-its` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-single-entry-point` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-typed-memories` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-bounded-disposition` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-stack-hud-refactor` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-superpod-mark3` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-aif4iad` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-open-learning-system` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-patchboard-viz` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-reflective-discipline` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-wm-aif-policy-grain-compliance` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-smart-emacs-cursor` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-chipwitz-corps` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-artificial-stack-exchange` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-metric-harness` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-intent-curvature` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-federated-agency-hardening` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-differentiable-substrate` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-canon-fingerprint-store` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-differentiable-math` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-prior-mathematics` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-superpod-mark2` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-hyperreal-dictionary-planning` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-paper-reverse-morphogenesis` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |
| `M-distributed-frontiermath` | `:mission` | 0 [G] | 0 [G] | 0 [G] | — [G] | — [G] | 0 [O] | 0 [C] |

## `M-diagramprover` co-work example

- `E-aif-cascade`: `#{:operator-turn-co-mention}`.
- `E-convert-operator-turns-to-patterns`: `#{:same-session}`.
- `M-G-wm-wiring`: `#{:operator-turn-co-mention}`.
- `M-a-wmc-scaling`: `#{:same-session}`.
- `M-apm-demonstration`: `#{:operator-turn-co-mention}`.
- `M-autoclock-in`: `#{:operator-turn-co-mention}`.
- `M-expressions-of-interest`: `#{:same-session}`.
- `M-futon-seams`: `#{:operator-turn-co-mention}`.
- `M-futonzero-prelim-practice`: `#{:same-session}`.
- `M-the-perfect-crime`: `#{:same-session :operator-turn-co-mention}`.
- `M-wm-wiring`: `#{:operator-turn-co-mention}`.

## Correlations

Spearman rho uses average ranks for ties over all 107 persisted items.

| comparison with dispatch count | rho |
|---|---:|
| persisted occurrence count | -0.044481 |
| B1 (b), shared-pattern missions | -0.207106 |

## Proposal

As a first-cut `unblocks` token, the observation square could emit dispatch attention (raw dispatch count, sessions, dispatchers, recency, and co-work degree) as an **enabler weight**: repeated or joint work is evidence that an item is useful alongside other work. It is explicitly not a dependency and must not assert that completing one item makes another possible. Where a reviewed B1b-style `Unblocks:` declaration exists, that declaration supersedes the inferred attention value for the directed relation.
