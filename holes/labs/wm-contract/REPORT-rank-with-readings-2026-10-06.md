# Ranking the 103 filled META candidates with recorded readings — 2026-10-06

Reproduce in a fresh process from `/home/joe/code/futon2`:

```sh
clojure -M scripts/rank_with_readings.clj
```

futon2 HEAD before generation: `fd24b92da3b37faa0478680dde4afbc01b36b0b0`. The evaluator is `futon2.aif.meta-outer-policy/evaluate`; construction and evaluation both run in this fresh process.

## Sources and pins

| source | SHA-256 |
|---|---|
| `:run-record`: `data/wm-runs/tick-run-record-2026-10-05-c9d25d6a-f2bb-42bf-a162-2c4a000e804f.edn` | `9de9e4bbb286680ebc74daa642a1fd8987009238928bddef60b387954d4d10c2` |
| `:measurement-contract`: `holes/labs/wm-contract/proposals/meta-outer-policy-cascade-measurement-v1.edn` | `1d6d6898680b5340849a0b1ff8e907793d23787867c9dcc55da04fee41e3c20f` |
| `:prior-v2`: `holes/labs/wm-contract/proposals/meta-outer-provisional-prior-v2-proposal.edn` | `e93764e3f07a529032f2362c8e44758066f94350e7c4947710ae755c4825c257` |
| `:sheet-report`: `holes/labs/wm-contract/REPORT-cheat-sheets-2026-10-05.md` | `f67ff17b48b9b698031c22f337609dcd0037d38378b6444de895190de64145c5` |
| `:operator-load-report`: `holes/labs/wm-contract/REPORT-operator-load-2026-10-05.md` | `3cbb9c3f32a13b15768e8d23e5a1566a51d06baa8519417828b04a144b923e4f` |
| `:attention-report`: `holes/labs/wm-contract/REPORT-revealed-attention-2026-10-05.md` | `f2dcbb809c96534b20712f06df08a52d97cbcaf77ce5182666c81cc5cdb6e140` |
| `:coupling-report`: `holes/labs/wm-contract/REPORT-centrality-tokens-2026-10-05.md` | `6d52b9f15d90f0ab3abaa8412e6b3e3294c7ac18377a1cd4e3561f59eac9fe41` |
| `:mapping-script`: `scripts/rank_with_readings.clj` | `1bdca9e618531d7ce483f5446e43bbcc12a51b279611eadf3a807ae2603ccdd8` |

The measurement contract is passed unchanged and its pin is `:contract-source`. Coupling B1(b) travels in the readings map and report only; it is not included in any predicted mean or G input.

The field observation remains whole. Its 381 rows outside the persisted fully-filled 103 are supplied to the evaluator as typed construction exclusions with reason `:outside-persisted-fully-filled-103`; this preserves exact field coverage while measuring only the requested candidates.

## Prediction mapping

This is a **default setting, tune later**, not an empirical calibration:

```clojure
{:status :default-setting-tune-later, :authority {:actor "joe", :date "2026-10-06", :policy :defaults}, :outcome-vocabulary [:closure :grounded-progress :downstream-unblocking :abstention-or-failure :elapsed-budget-fraction :token-budget-fraction :operator-demand], :means {:closure "0.1 + 0.4*register", :grounded-progress "clamp(0.2 + 0.5*register + 0.2*I(dispatches>0))", :downstream-unblocking "clamp(0.1 + 0.3*I(dispatches>0) + 0.3*I(operator-turns-14d>0) + 0.3*min(1,co-work-degree/5))", :abstention-or-failure "0.5 - 0.4*register", :elapsed-budget-fraction 0.5, :token-budget-fraction 0.5, :operator-demand "clamp(min(1,marker-lines/10) + 0.2*I(operator-turns-14d>0))"}, :variances {:all 0.25, :claim :wide-no-calibration}, :ablation {:no-sheet {:register :median-over-103, :marker-lines 0}, :no-attention {:dispatches 0, :operator-turns-14d 0, :co-work-degree 0}}, :coupling {:shared-missions :receipt-only, :scored false}, :uninformed-resources [:elapsed-budget-fraction :token-budget-fraction]}
```

The median register used by `--no-sheet` is 0.481000000. Both resource-fraction means are 0.5 in every scenario because the readings contain no candidate-conditioned elapsed-time or token-use prediction. Every predicted variance is 0.25.

The zero-information model is:

```clojure
{:prior {:only-state 1.0}, :predicted-observations {:no-new-observation 1.0}, :posteriors {:no-new-observation {:only-state 1.0}}}
```

The real EIG kernel returns **0.000000000**: its one predicted observation leaves the singleton prior unchanged, so this is an honest zero rather than an omitted term.

## Evaluator receipts and live comparison

Kendall tau-b is computed over the 103 common items. The live ranking has no ties; measurement-score ties are retained and enter tau-b's tie denominator. Canonical ID breaks score ties only for displaying a total order and selecting a policy.

| scenario | flags | selected policy | selected target | nearest alternative | tau-b vs live | n | measurement ties |
|---|---|---|---|---|---:|---:|---:|
| full | `none` | `:meta-policy/M-warrant-limit` | `M-warrant-limit` | `:meta-policy/M-autoclock-in` | 0.033365148 | 103 | 16 |
| no sheet | `--no-sheet` | `:meta-policy/M-a-wmc-scaling` | `M-a-wmc-scaling` | `:meta-policy/M-aif-policy-conditioned-eig` | 0.083768480 | 103 | 3928 |
| no attention | `--no-attention` | `:meta-policy/M-warrant-limit` | `M-warrant-limit` | `:meta-policy/M-intent-curvature` | -0.028982782 | 103 | 17 |
| neither | `--no-sheet --no-attention` | `:meta-policy/M-a-sorry-enterprise` | `M-a-sorry-enterprise` | `:meta-policy/M-a-wmc-scaling` | 0.000000000 | 103 | 5253 |

## Top ten side by side

| rank | live selector | full | no sheet | no attention | neither |
|---:|---|---|---|---|---|
| 1 | `M-interim-director-proxy-metric-inventory` | `M-warrant-limit` | `M-a-wmc-scaling` | `M-warrant-limit` | `M-a-sorry-enterprise` |
| 2 | `M-categorical-code` | `M-autoclock-in` | `M-aif-policy-conditioned-eig` | `M-intent-curvature` | `M-a-wmc-scaling` |
| 3 | `M-or-training-as-learning-system` | `M-the-perfect-crime` | `M-autoclock-in` | `M-operational-vocabulary` | `M-action-cost-modelling` |
| 4 | `M-simulating-or-training-as-learning-system` | `M-aif-policy-conditioned-eig` | `M-the-perfect-crime` | `M-goals-and-holes` | `M-aif-a-matrix-faithfulness` |
| 5 | `M-the-perfect-crime` | `M-codex-sorry-loop` | `M-warrant-limit` | `M-interim-director-proxy-metric-inventory` | `M-aif-ants-port` |
| 6 | `M-futonzero-mvp` | `M-a-wmc-scaling` | `M-expressions-of-interest` | `M-war-machine-aif-last-mile` | `M-aif-faithfulness` |
| 7 | `M-vsatarcs-writer` | `M-aif-ants-port` | `M-turns-first` | `M-signal-roll-up` | `M-aif-policy-conditioned-eig` |
| 8 | `M-vsatarcs-invariants-integration` | `M-expressions-of-interest` | `M-mission-coherence-patterns` | `M-autonomous-doc-maintenance` | `M-aif4iad` |
| 9 | `M-weird-modernism` | `M-mission-coherence-patterns` | `M-codex-sorry-loop` | `M-essay-corpus-substrate` | `M-another-university` |
| 10 | `M-futon-enrichment` | `M-shared-memory-control-build-test` | `M-zai-learning-loop` | `M-aif-faithfulness` | `M-artificial-stack-exchange` |

## Proxy-metric-inventory position and readings

Its recorded readings are `{:register 0.706, :markers 1, :coupling 68, :dispatches 0, :operator-turns 0, :co-work 0, :last-dispatch "— [G]"}`.

| live | full | no sheet | no attention | neither |
|---:|---:|---:|---:|---:|
| 1 | 17 | 54 | 5 | 48 |

## G terms for each scenario's top three

| scenario | rank | id | risk | ambiguity | epistemic value | G |
|---|---:|---|---:|---:|---:|---:|
| full | 1 | `M-warrant-limit` | 6.534963369 | 5.080539469 | 0.000000000 | 11.615502838 |
| full | 2 | `M-autoclock-in` | 6.582795988 | 5.080539469 | 0.000000000 | 11.663335456 |
| full | 3 | `M-the-perfect-crime` | 6.583539319 | 5.080539469 | 0.000000000 | 11.664078788 |
| no sheet | 1 | `M-a-wmc-scaling` | 6.580879999 | 5.080539469 | 0.000000000 | 11.661419468 |
| no sheet | 2 | `M-aif-policy-conditioned-eig` | 6.580879999 | 5.080539469 | 0.000000000 | 11.661419468 |
| no sheet | 3 | `M-autoclock-in` | 6.580879999 | 5.080539469 | 0.000000000 | 11.661419468 |
| no attention | 1 | `M-warrant-limit` | 6.644538369 | 5.080539469 | 0.000000000 | 11.725077838 |
| no attention | 2 | `M-intent-curvature` | 6.644771559 | 5.080539469 | 0.000000000 | 11.725311028 |
| no attention | 3 | `M-operational-vocabulary` | 6.653673088 | 5.080539469 | 0.000000000 | 11.734212556 |
| neither | 1 | `M-a-sorry-enterprise` | 6.700104999 | 5.080539469 | 0.000000000 | 11.780644468 |
| neither | 2 | `M-a-wmc-scaling` | 6.700104999 | 5.080539469 | 0.000000000 | 11.780644468 |
| neither | 3 | `M-action-cost-modelling` | 6.700104999 | 5.080539469 | 0.000000000 | 11.780644468 |

## Full rankings

| scenario | rank | id | G |
|---|---:|---|---:|
| full | 1 | `M-warrant-limit` | 11.615502838 |
| full | 2 | `M-autoclock-in` | 11.663335456 |
| full | 3 | `M-the-perfect-crime` | 11.664078788 |
| full | 4 | `M-aif-policy-conditioned-eig` | 11.665275528 |
| full | 5 | `M-codex-sorry-loop` | 11.682894388 |
| full | 6 | `M-a-wmc-scaling` | 11.682987488 |
| full | 7 | `M-aif-ants-port` | 11.686433118 |
| full | 8 | `M-expressions-of-interest` | 11.706562238 |
| full | 9 | `M-mission-coherence-patterns` | 11.710123818 |
| full | 10 | `M-shared-memory-control-build-test` | 11.714066281 |
| full | 11 | `M-turns-first` | 11.714965036 |
| full | 12 | `M-intent-curvature` | 11.725311028 |
| full | 13 | `M-inbox-zero-claim-lifecycle` | 11.725488636 |
| full | 14 | `M-zai-learning-loop` | 11.728871688 |
| full | 15 | `M-operational-vocabulary` | 11.734212556 |
| full | 16 | `M-goals-and-holes` | 11.740613488 |
| full | 17 | `M-interim-director-proxy-metric-inventory` | 11.746923561 |
| full | 18 | `M-war-machine-aif-last-mile` | 11.746961838 |
| full | 19 | `M-signal-roll-up` | 11.747639056 |
| full | 20 | `M-autonomous-doc-maintenance` | 11.748774736 |
| full | 21 | `M-a-sorry-enterprise` | 11.749116918 |
| full | 22 | `M-essay-corpus-substrate` | 11.750928718 |
| full | 23 | `M-aif-faithfulness` | 11.754658656 |
| full | 24 | `M-action-cost-modelling` | 11.755839936 |
| full | 25 | `M-buyer-discovery` | 11.755988238 |
| full | 26 | `M-simulating-or-training-as-learning-system` | 11.757281161 |
| full | 27 | `M-interim-director` | 11.757432028 |
| full | 28 | `M-differentiable-substrate` | 11.757583036 |
| full | 29 | `M-state-snapshot-witness` | 11.759253538 |
| full | 30 | `M-vsatarcs-writer` | 11.761095568 |
| full | 31 | `M-xor-coupling-probe` | 11.763897088 |
| full | 32 | `M-tpg-coupling-evolution` | 11.765790418 |
| full | 33 | `M-zaif-harness-v1` | 11.766585361 |
| full | 34 | `M-editorial-assistant` | 11.767064038 |
| full | 35 | `M-differentiable-math` | 11.768507761 |
| full | 36 | `M-single-entry-point` | 11.770125436 |
| full | 37 | `M-federated-agency-hardening` | 11.772352881 |
| full | 38 | `M-mission-scopes-into-substrate-2` | 11.772578668 |
| full | 39 | `M-war-machine-vsatarcs-interop` | 11.772578668 |
| full | 40 | `M-or-training-as-learning-system` | 11.772743356 |
| full | 41 | `M-superpod-mark2` | 11.772743356 |
| full | 42 | `M-daily-scan` | 11.773007361 |
| full | 43 | `M-futonzero-mvp` | 11.773073161 |
| full | 44 | `M-war-machine-pilot` | 11.773403536 |
| full | 45 | `M-categorical-code` | 11.774730736 |
| full | 46 | `M-distributed-frontiermath` | 11.774730736 |
| full | 47 | `M-web-arxana-missions` | 11.775899518 |
| full | 48 | `M-prior-mathematics` | 11.777075281 |
| full | 49 | `M-fulab-wiring-survey` | 11.777412496 |
| full | 50 | `M-patchboard-viz` | 11.777484738 |
| full | 51 | `M-custom-harness` | 11.777581318 |
| full | 52 | `M-differentiable-code` | 11.777581318 |
| full | 53 | `M-daily-scan-multi-axis-queue` | 11.778662496 |
| full | 54 | `M-the-futon-stack` | 11.779277368 |
| full | 55 | `M-artificial-stack-exchange` | 11.779447756 |
| full | 56 | `M-reachable-from-boot` | 11.779447756 |
| full | 57 | `M-war-machine-frontend-upgrade1` | 11.779447756 |
| full | 58 | `M-hypergraph-operator` | 11.780187018 |
| full | 59 | `M-futon-enrichment` | 11.780473081 |
| full | 60 | `M-metric-harness` | 11.780473081 |
| full | 61 | `M-patterns-done-right` | 11.780473081 |
| full | 62 | `M-superpod-mark3` | 11.780644468 |
| full | 63 | `M-coupling-as-constraint` | 11.781848161 |
| full | 64 | `M-pattern-posteriors` | 11.782020688 |
| full | 65 | `M-stack-hud-refactor` | 11.784625681 |
| full | 66 | `M-vsatarcs-invariants-integration` | 11.785325761 |
| full | 67 | `M-stack-geometry` | 11.786028121 |
| full | 68 | `M-self-improvement-loop` | 11.788682278 |
| full | 69 | `M-web-arxana-ui-improvements` | 11.788860361 |
| full | 70 | `M-recommendation-bindings` | 11.791360938 |
| full | 71 | `M-pudding-peradams` | 11.793540556 |
| full | 72 | `M-reflective-discipline` | 11.794269136 |
| full | 73 | `M-interim-director-long-tail` | 11.794999996 |
| full | 74 | `M-futonzero-generative` | 11.795366281 |
| full | 75 | `M-typed-memories` | 11.797391038 |
| full | 76 | `M-formal-war-machine` | 11.802056881 |
| full | 77 | `M-smart-emacs-cursor` | 11.802056881 |
| full | 78 | `M-war-machine-wiring` | 11.805280528 |
| full | 79 | `M-pattern-mining` | 11.807196028 |
| full | 80 | `M-chipwitz-corps` | 11.808159121 |
| full | 81 | `M-hyperreal-dictionary-planning` | 11.808159121 |
| full | 82 | `M-aif-a-matrix-faithfulness` | 11.810679838 |
| full | 83 | `M-digital-nomad-patterns` | 11.812047121 |
| full | 84 | `M-bounded-disposition` | 11.812243018 |
| full | 85 | `M-value-creation-loop` | 11.812243018 |
| full | 86 | `M-paper-reverse-morphogenesis` | 11.812635238 |
| full | 87 | `M-wm-aif-policy-grain-compliance` | 11.813028028 |
| full | 88 | `M-webarxana` | 11.815992121 |
| full | 89 | `M-arxana-roundtrip` | 11.816190868 |
| full | 90 | `M-landing-practice` | 11.816190868 |
| full | 91 | `M-canon-fingerprint-store` | 11.816389756 |
| full | 92 | `M-stack-stereolithography` | 11.818586938 |
| full | 93 | `M-aif4iad` | 11.826928336 |
| full | 94 | `M-pattern-application-diagnostic` | 11.826928336 |
| full | 95 | `M-trip-journal` | 11.827548088 |
| full | 96 | `M-xenotype-its` | 11.828376418 |
| full | 97 | `M-essays-diachronic-model` | 11.828999161 |
| full | 98 | `M-open-learning-system` | 11.828999161 |
| full | 99 | `M-weird-modernism` | 11.832552268 |
| full | 100 | `M-essays-retraction-visibility` | 11.839137481 |
| full | 101 | `M-essays-edit-cycle` | 11.844329761 |
| full | 102 | `M-another-university` | 11.852272081 |
| full | 103 | `M-pattern-retrieval-calibration` | 11.911732761 |
| no sheet | 1 | `M-a-wmc-scaling` | 11.661419468 |
| no sheet | 2 | `M-aif-policy-conditioned-eig` | 11.661419468 |
| no sheet | 3 | `M-autoclock-in` | 11.661419468 |
| no sheet | 4 | `M-the-perfect-crime` | 11.661419468 |
| no sheet | 5 | `M-warrant-limit` | 11.661419468 |
| no sheet | 6 | `M-expressions-of-interest` | 11.667669468 |
| no sheet | 7 | `M-turns-first` | 11.672619468 |
| no sheet | 8 | `M-mission-coherence-patterns` | 11.672669468 |
| no sheet | 9 | `M-codex-sorry-loop` | 11.685219468 |
| no sheet | 10 | `M-zai-learning-loop` | 11.692869468 |
| no sheet | 11 | `M-aif-ants-port` | 11.701419468 |
| no sheet | 12 | `M-shared-memory-control-build-test` | 11.701419468 |
| no sheet | 13 | `M-a-sorry-enterprise` | 11.729394468 |
| no sheet | 14 | `M-inbox-zero-claim-lifecycle` | 11.729394468 |
| no sheet | 15 | `M-action-cost-modelling` | 11.780644468 |
| no sheet | 16 | `M-aif-a-matrix-faithfulness` | 11.780644468 |
| no sheet | 17 | `M-aif-faithfulness` | 11.780644468 |
| no sheet | 18 | `M-aif4iad` | 11.780644468 |
| no sheet | 19 | `M-another-university` | 11.780644468 |
| no sheet | 20 | `M-artificial-stack-exchange` | 11.780644468 |
| no sheet | 21 | `M-arxana-roundtrip` | 11.780644468 |
| no sheet | 22 | `M-autonomous-doc-maintenance` | 11.780644468 |
| no sheet | 23 | `M-bounded-disposition` | 11.780644468 |
| no sheet | 24 | `M-buyer-discovery` | 11.780644468 |
| no sheet | 25 | `M-canon-fingerprint-store` | 11.780644468 |
| no sheet | 26 | `M-categorical-code` | 11.780644468 |
| no sheet | 27 | `M-chipwitz-corps` | 11.780644468 |
| no sheet | 28 | `M-coupling-as-constraint` | 11.780644468 |
| no sheet | 29 | `M-custom-harness` | 11.780644468 |
| no sheet | 30 | `M-daily-scan` | 11.780644468 |
| no sheet | 31 | `M-daily-scan-multi-axis-queue` | 11.780644468 |
| no sheet | 32 | `M-differentiable-code` | 11.780644468 |
| no sheet | 33 | `M-differentiable-math` | 11.780644468 |
| no sheet | 34 | `M-differentiable-substrate` | 11.780644468 |
| no sheet | 35 | `M-digital-nomad-patterns` | 11.780644468 |
| no sheet | 36 | `M-distributed-frontiermath` | 11.780644468 |
| no sheet | 37 | `M-editorial-assistant` | 11.780644468 |
| no sheet | 38 | `M-essay-corpus-substrate` | 11.780644468 |
| no sheet | 39 | `M-essays-diachronic-model` | 11.780644468 |
| no sheet | 40 | `M-essays-edit-cycle` | 11.780644468 |
| no sheet | 41 | `M-essays-retraction-visibility` | 11.780644468 |
| no sheet | 42 | `M-federated-agency-hardening` | 11.780644468 |
| no sheet | 43 | `M-formal-war-machine` | 11.780644468 |
| no sheet | 44 | `M-fulab-wiring-survey` | 11.780644468 |
| no sheet | 45 | `M-futon-enrichment` | 11.780644468 |
| no sheet | 46 | `M-futonzero-generative` | 11.780644468 |
| no sheet | 47 | `M-futonzero-mvp` | 11.780644468 |
| no sheet | 48 | `M-goals-and-holes` | 11.780644468 |
| no sheet | 49 | `M-hypergraph-operator` | 11.780644468 |
| no sheet | 50 | `M-hyperreal-dictionary-planning` | 11.780644468 |
| no sheet | 51 | `M-intent-curvature` | 11.780644468 |
| no sheet | 52 | `M-interim-director` | 11.780644468 |
| no sheet | 53 | `M-interim-director-long-tail` | 11.780644468 |
| no sheet | 54 | `M-interim-director-proxy-metric-inventory` | 11.780644468 |
| no sheet | 55 | `M-landing-practice` | 11.780644468 |
| no sheet | 56 | `M-metric-harness` | 11.780644468 |
| no sheet | 57 | `M-mission-scopes-into-substrate-2` | 11.780644468 |
| no sheet | 58 | `M-open-learning-system` | 11.780644468 |
| no sheet | 59 | `M-operational-vocabulary` | 11.780644468 |
| no sheet | 60 | `M-or-training-as-learning-system` | 11.780644468 |
| no sheet | 61 | `M-paper-reverse-morphogenesis` | 11.780644468 |
| no sheet | 62 | `M-patchboard-viz` | 11.780644468 |
| no sheet | 63 | `M-pattern-application-diagnostic` | 11.780644468 |
| no sheet | 64 | `M-pattern-mining` | 11.780644468 |
| no sheet | 65 | `M-pattern-posteriors` | 11.780644468 |
| no sheet | 66 | `M-pattern-retrieval-calibration` | 11.780644468 |
| no sheet | 67 | `M-patterns-done-right` | 11.780644468 |
| no sheet | 68 | `M-prior-mathematics` | 11.780644468 |
| no sheet | 69 | `M-pudding-peradams` | 11.780644468 |
| no sheet | 70 | `M-reachable-from-boot` | 11.780644468 |
| no sheet | 71 | `M-recommendation-bindings` | 11.780644468 |
| no sheet | 72 | `M-reflective-discipline` | 11.780644468 |
| no sheet | 73 | `M-self-improvement-loop` | 11.780644468 |
| no sheet | 74 | `M-signal-roll-up` | 11.780644468 |
| no sheet | 75 | `M-simulating-or-training-as-learning-system` | 11.780644468 |
| no sheet | 76 | `M-single-entry-point` | 11.780644468 |
| no sheet | 77 | `M-smart-emacs-cursor` | 11.780644468 |
| no sheet | 78 | `M-stack-geometry` | 11.780644468 |
| no sheet | 79 | `M-stack-hud-refactor` | 11.780644468 |
| no sheet | 80 | `M-stack-stereolithography` | 11.780644468 |
| no sheet | 81 | `M-state-snapshot-witness` | 11.780644468 |
| no sheet | 82 | `M-superpod-mark2` | 11.780644468 |
| no sheet | 83 | `M-superpod-mark3` | 11.780644468 |
| no sheet | 84 | `M-the-futon-stack` | 11.780644468 |
| no sheet | 85 | `M-tpg-coupling-evolution` | 11.780644468 |
| no sheet | 86 | `M-trip-journal` | 11.780644468 |
| no sheet | 87 | `M-typed-memories` | 11.780644468 |
| no sheet | 88 | `M-value-creation-loop` | 11.780644468 |
| no sheet | 89 | `M-vsatarcs-invariants-integration` | 11.780644468 |
| no sheet | 90 | `M-vsatarcs-writer` | 11.780644468 |
| no sheet | 91 | `M-war-machine-aif-last-mile` | 11.780644468 |
| no sheet | 92 | `M-war-machine-frontend-upgrade1` | 11.780644468 |
| no sheet | 93 | `M-war-machine-pilot` | 11.780644468 |
| no sheet | 94 | `M-war-machine-vsatarcs-interop` | 11.780644468 |
| no sheet | 95 | `M-war-machine-wiring` | 11.780644468 |
| no sheet | 96 | `M-web-arxana-missions` | 11.780644468 |
| no sheet | 97 | `M-web-arxana-ui-improvements` | 11.780644468 |
| no sheet | 98 | `M-webarxana` | 11.780644468 |
| no sheet | 99 | `M-weird-modernism` | 11.780644468 |
| no sheet | 100 | `M-wm-aif-policy-grain-compliance` | 11.780644468 |
| no sheet | 101 | `M-xenotype-its` | 11.780644468 |
| no sheet | 102 | `M-xor-coupling-probe` | 11.780644468 |
| no sheet | 103 | `M-zaif-harness-v1` | 11.780644468 |
| no attention | 1 | `M-warrant-limit` | 11.725077838 |
| no attention | 2 | `M-intent-curvature` | 11.725311028 |
| no attention | 3 | `M-operational-vocabulary` | 11.734212556 |
| no attention | 4 | `M-goals-and-holes` | 11.740613488 |
| no attention | 5 | `M-interim-director-proxy-metric-inventory` | 11.746923561 |
| no attention | 6 | `M-war-machine-aif-last-mile` | 11.746961838 |
| no attention | 7 | `M-signal-roll-up` | 11.747639056 |
| no attention | 8 | `M-autonomous-doc-maintenance` | 11.748774736 |
| no attention | 9 | `M-essay-corpus-substrate` | 11.750928718 |
| no attention | 10 | `M-aif-faithfulness` | 11.754658656 |
| no attention | 11 | `M-action-cost-modelling` | 11.755839936 |
| no attention | 12 | `M-buyer-discovery` | 11.755988238 |
| no attention | 13 | `M-simulating-or-training-as-learning-system` | 11.757281161 |
| no attention | 14 | `M-interim-director` | 11.757432028 |
| no attention | 15 | `M-differentiable-substrate` | 11.757583036 |
| no attention | 16 | `M-state-snapshot-witness` | 11.759253538 |
| no attention | 17 | `M-vsatarcs-writer` | 11.761095568 |
| no attention | 18 | `M-aif-ants-port` | 11.762958118 |
| no attention | 19 | `M-xor-coupling-probe` | 11.763897088 |
| no attention | 20 | `M-tpg-coupling-evolution` | 11.765790418 |
| no attention | 21 | `M-zaif-harness-v1` | 11.766585361 |
| no attention | 22 | `M-editorial-assistant` | 11.767064038 |
| no attention | 23 | `M-differentiable-math` | 11.768507761 |
| no attention | 24 | `M-single-entry-point` | 11.770125436 |
| no attention | 25 | `M-federated-agency-hardening` | 11.772352881 |
| no attention | 26 | `M-mission-scopes-into-substrate-2` | 11.772578668 |
| no attention | 27 | `M-war-machine-vsatarcs-interop` | 11.772578668 |
| no attention | 28 | `M-or-training-as-learning-system` | 11.772743356 |
| no attention | 29 | `M-superpod-mark2` | 11.772743356 |
| no attention | 30 | `M-daily-scan` | 11.773007361 |
| no attention | 31 | `M-futonzero-mvp` | 11.773073161 |
| no attention | 32 | `M-war-machine-pilot` | 11.773403536 |
| no attention | 33 | `M-categorical-code` | 11.774730736 |
| no attention | 34 | `M-distributed-frontiermath` | 11.774730736 |
| no attention | 35 | `M-web-arxana-missions` | 11.775899518 |
| no attention | 36 | `M-inbox-zero-claim-lifecycle` | 11.776738636 |
| no attention | 37 | `M-prior-mathematics` | 11.777075281 |
| no attention | 38 | `M-fulab-wiring-survey` | 11.777412496 |
| no attention | 39 | `M-patchboard-viz` | 11.777484738 |
| no attention | 40 | `M-custom-harness` | 11.777581318 |
| no attention | 41 | `M-differentiable-code` | 11.777581318 |
| no attention | 42 | `M-codex-sorry-loop` | 11.777919388 |
| no attention | 43 | `M-daily-scan-multi-axis-queue` | 11.778662496 |
| no attention | 44 | `M-the-futon-stack` | 11.779277368 |
| no attention | 45 | `M-artificial-stack-exchange` | 11.779447756 |
| no attention | 46 | `M-reachable-from-boot` | 11.779447756 |
| no attention | 47 | `M-war-machine-frontend-upgrade1` | 11.779447756 |
| no attention | 48 | `M-hypergraph-operator` | 11.780187018 |
| no attention | 49 | `M-futon-enrichment` | 11.780473081 |
| no attention | 50 | `M-metric-harness` | 11.780473081 |
| no attention | 51 | `M-patterns-done-right` | 11.780473081 |
| no attention | 52 | `M-superpod-mark3` | 11.780644468 |
| no attention | 53 | `M-coupling-as-constraint` | 11.781848161 |
| no attention | 54 | `M-pattern-posteriors` | 11.782020688 |
| no attention | 55 | `M-autoclock-in` | 11.782885456 |
| no attention | 56 | `M-the-perfect-crime` | 11.783753788 |
| no attention | 57 | `M-stack-hud-refactor` | 11.784625681 |
| no attention | 58 | `M-aif-policy-conditioned-eig` | 11.785150528 |
| no attention | 59 | `M-vsatarcs-invariants-integration` | 11.785325761 |
| no attention | 60 | `M-stack-geometry` | 11.786028121 |
| no attention | 61 | `M-self-improvement-loop` | 11.788682278 |
| no attention | 62 | `M-web-arxana-ui-improvements` | 11.788860361 |
| no attention | 63 | `M-recommendation-bindings` | 11.791360938 |
| no attention | 64 | `M-pudding-peradams` | 11.793540556 |
| no attention | 65 | `M-reflective-discipline` | 11.794269136 |
| no attention | 66 | `M-interim-director-long-tail` | 11.794999996 |
| no attention | 67 | `M-futonzero-generative` | 11.795366281 |
| no attention | 68 | `M-shared-memory-control-build-test` | 11.795366281 |
| no attention | 69 | `M-typed-memories` | 11.797391038 |
| no attention | 70 | `M-a-sorry-enterprise` | 11.800366918 |
| no attention | 71 | `M-formal-war-machine` | 11.802056881 |
| no attention | 72 | `M-smart-emacs-cursor` | 11.802056881 |
| no attention | 73 | `M-war-machine-wiring` | 11.805280528 |
| no attention | 74 | `M-a-wmc-scaling` | 11.805662488 |
| no attention | 75 | `M-pattern-mining` | 11.807196028 |
| no attention | 76 | `M-chipwitz-corps` | 11.808159121 |
| no attention | 77 | `M-hyperreal-dictionary-planning` | 11.808159121 |
| no attention | 78 | `M-aif-a-matrix-faithfulness` | 11.810679838 |
| no attention | 79 | `M-digital-nomad-patterns` | 11.812047121 |
| no attention | 80 | `M-bounded-disposition` | 11.812243018 |
| no attention | 81 | `M-value-creation-loop` | 11.812243018 |
| no attention | 82 | `M-paper-reverse-morphogenesis` | 11.812635238 |
| no attention | 83 | `M-wm-aif-policy-grain-compliance` | 11.813028028 |
| no attention | 84 | `M-webarxana` | 11.815992121 |
| no attention | 85 | `M-arxana-roundtrip` | 11.816190868 |
| no attention | 86 | `M-landing-practice` | 11.816190868 |
| no attention | 87 | `M-canon-fingerprint-store` | 11.816389756 |
| no attention | 88 | `M-stack-stereolithography` | 11.818586938 |
| no attention | 89 | `M-zai-learning-loop` | 11.819246688 |
| no attention | 90 | `M-mission-coherence-patterns` | 11.823848818 |
| no attention | 91 | `M-expressions-of-interest` | 11.825487238 |
| no attention | 92 | `M-aif4iad` | 11.826928336 |
| no attention | 93 | `M-pattern-application-diagnostic` | 11.826928336 |
| no attention | 94 | `M-trip-journal` | 11.827548088 |
| no attention | 95 | `M-xenotype-its` | 11.828376418 |
| no attention | 96 | `M-essays-diachronic-model` | 11.828999161 |
| no attention | 97 | `M-open-learning-system` | 11.828999161 |
| no attention | 98 | `M-turns-first` | 11.829415036 |
| no attention | 99 | `M-weird-modernism` | 11.832552268 |
| no attention | 100 | `M-essays-retraction-visibility` | 11.839137481 |
| no attention | 101 | `M-essays-edit-cycle` | 11.844329761 |
| no attention | 102 | `M-another-university` | 11.852272081 |
| no attention | 103 | `M-pattern-retrieval-calibration` | 11.911732761 |
| neither | 1 | `M-a-sorry-enterprise` | 11.780644468 |
| neither | 2 | `M-a-wmc-scaling` | 11.780644468 |
| neither | 3 | `M-action-cost-modelling` | 11.780644468 |
| neither | 4 | `M-aif-a-matrix-faithfulness` | 11.780644468 |
| neither | 5 | `M-aif-ants-port` | 11.780644468 |
| neither | 6 | `M-aif-faithfulness` | 11.780644468 |
| neither | 7 | `M-aif-policy-conditioned-eig` | 11.780644468 |
| neither | 8 | `M-aif4iad` | 11.780644468 |
| neither | 9 | `M-another-university` | 11.780644468 |
| neither | 10 | `M-artificial-stack-exchange` | 11.780644468 |
| neither | 11 | `M-arxana-roundtrip` | 11.780644468 |
| neither | 12 | `M-autoclock-in` | 11.780644468 |
| neither | 13 | `M-autonomous-doc-maintenance` | 11.780644468 |
| neither | 14 | `M-bounded-disposition` | 11.780644468 |
| neither | 15 | `M-buyer-discovery` | 11.780644468 |
| neither | 16 | `M-canon-fingerprint-store` | 11.780644468 |
| neither | 17 | `M-categorical-code` | 11.780644468 |
| neither | 18 | `M-chipwitz-corps` | 11.780644468 |
| neither | 19 | `M-codex-sorry-loop` | 11.780644468 |
| neither | 20 | `M-coupling-as-constraint` | 11.780644468 |
| neither | 21 | `M-custom-harness` | 11.780644468 |
| neither | 22 | `M-daily-scan` | 11.780644468 |
| neither | 23 | `M-daily-scan-multi-axis-queue` | 11.780644468 |
| neither | 24 | `M-differentiable-code` | 11.780644468 |
| neither | 25 | `M-differentiable-math` | 11.780644468 |
| neither | 26 | `M-differentiable-substrate` | 11.780644468 |
| neither | 27 | `M-digital-nomad-patterns` | 11.780644468 |
| neither | 28 | `M-distributed-frontiermath` | 11.780644468 |
| neither | 29 | `M-editorial-assistant` | 11.780644468 |
| neither | 30 | `M-essay-corpus-substrate` | 11.780644468 |
| neither | 31 | `M-essays-diachronic-model` | 11.780644468 |
| neither | 32 | `M-essays-edit-cycle` | 11.780644468 |
| neither | 33 | `M-essays-retraction-visibility` | 11.780644468 |
| neither | 34 | `M-expressions-of-interest` | 11.780644468 |
| neither | 35 | `M-federated-agency-hardening` | 11.780644468 |
| neither | 36 | `M-formal-war-machine` | 11.780644468 |
| neither | 37 | `M-fulab-wiring-survey` | 11.780644468 |
| neither | 38 | `M-futon-enrichment` | 11.780644468 |
| neither | 39 | `M-futonzero-generative` | 11.780644468 |
| neither | 40 | `M-futonzero-mvp` | 11.780644468 |
| neither | 41 | `M-goals-and-holes` | 11.780644468 |
| neither | 42 | `M-hypergraph-operator` | 11.780644468 |
| neither | 43 | `M-hyperreal-dictionary-planning` | 11.780644468 |
| neither | 44 | `M-inbox-zero-claim-lifecycle` | 11.780644468 |
| neither | 45 | `M-intent-curvature` | 11.780644468 |
| neither | 46 | `M-interim-director` | 11.780644468 |
| neither | 47 | `M-interim-director-long-tail` | 11.780644468 |
| neither | 48 | `M-interim-director-proxy-metric-inventory` | 11.780644468 |
| neither | 49 | `M-landing-practice` | 11.780644468 |
| neither | 50 | `M-metric-harness` | 11.780644468 |
| neither | 51 | `M-mission-coherence-patterns` | 11.780644468 |
| neither | 52 | `M-mission-scopes-into-substrate-2` | 11.780644468 |
| neither | 53 | `M-open-learning-system` | 11.780644468 |
| neither | 54 | `M-operational-vocabulary` | 11.780644468 |
| neither | 55 | `M-or-training-as-learning-system` | 11.780644468 |
| neither | 56 | `M-paper-reverse-morphogenesis` | 11.780644468 |
| neither | 57 | `M-patchboard-viz` | 11.780644468 |
| neither | 58 | `M-pattern-application-diagnostic` | 11.780644468 |
| neither | 59 | `M-pattern-mining` | 11.780644468 |
| neither | 60 | `M-pattern-posteriors` | 11.780644468 |
| neither | 61 | `M-pattern-retrieval-calibration` | 11.780644468 |
| neither | 62 | `M-patterns-done-right` | 11.780644468 |
| neither | 63 | `M-prior-mathematics` | 11.780644468 |
| neither | 64 | `M-pudding-peradams` | 11.780644468 |
| neither | 65 | `M-reachable-from-boot` | 11.780644468 |
| neither | 66 | `M-recommendation-bindings` | 11.780644468 |
| neither | 67 | `M-reflective-discipline` | 11.780644468 |
| neither | 68 | `M-self-improvement-loop` | 11.780644468 |
| neither | 69 | `M-shared-memory-control-build-test` | 11.780644468 |
| neither | 70 | `M-signal-roll-up` | 11.780644468 |
| neither | 71 | `M-simulating-or-training-as-learning-system` | 11.780644468 |
| neither | 72 | `M-single-entry-point` | 11.780644468 |
| neither | 73 | `M-smart-emacs-cursor` | 11.780644468 |
| neither | 74 | `M-stack-geometry` | 11.780644468 |
| neither | 75 | `M-stack-hud-refactor` | 11.780644468 |
| neither | 76 | `M-stack-stereolithography` | 11.780644468 |
| neither | 77 | `M-state-snapshot-witness` | 11.780644468 |
| neither | 78 | `M-superpod-mark2` | 11.780644468 |
| neither | 79 | `M-superpod-mark3` | 11.780644468 |
| neither | 80 | `M-the-futon-stack` | 11.780644468 |
| neither | 81 | `M-the-perfect-crime` | 11.780644468 |
| neither | 82 | `M-tpg-coupling-evolution` | 11.780644468 |
| neither | 83 | `M-trip-journal` | 11.780644468 |
| neither | 84 | `M-turns-first` | 11.780644468 |
| neither | 85 | `M-typed-memories` | 11.780644468 |
| neither | 86 | `M-value-creation-loop` | 11.780644468 |
| neither | 87 | `M-vsatarcs-invariants-integration` | 11.780644468 |
| neither | 88 | `M-vsatarcs-writer` | 11.780644468 |
| neither | 89 | `M-war-machine-aif-last-mile` | 11.780644468 |
| neither | 90 | `M-war-machine-frontend-upgrade1` | 11.780644468 |
| neither | 91 | `M-war-machine-pilot` | 11.780644468 |
| neither | 92 | `M-war-machine-vsatarcs-interop` | 11.780644468 |
| neither | 93 | `M-war-machine-wiring` | 11.780644468 |
| neither | 94 | `M-warrant-limit` | 11.780644468 |
| neither | 95 | `M-web-arxana-missions` | 11.780644468 |
| neither | 96 | `M-web-arxana-ui-improvements` | 11.780644468 |
| neither | 97 | `M-webarxana` | 11.780644468 |
| neither | 98 | `M-weird-modernism` | 11.780644468 |
| neither | 99 | `M-wm-aif-policy-grain-compliance` | 11.780644468 |
| neither | 100 | `M-xenotype-its` | 11.780644468 |
| neither | 101 | `M-xor-coupling-probe` | 11.780644468 |
| neither | 102 | `M-zai-learning-loop` | 11.780644468 |
| neither | 103 | `M-zaif-harness-v1` | 11.780644468 |

## What changed

The full winner, `M-warrant-limit`, has readings `{:register 0.867, :markers 0, :coupling 0, :dispatches 2, :operator-turns 39, :co-work 5, :last-dispatch "2026-09-28T18:21:09.085Z [G]"}`; the live winner has `{:register 0.706, :markers 1, :coupling 68, :dispatches 0, :operator-turns 0, :co-work 0, :last-dispatch "— [G]"}` and lands 17th in the full measurement. Removing the sheet replaces register with the cohort median and clears marker lines, so closure, progress, failure, and operator-demand lose item-specific variation. Removing attention clears dispatch, operator-turn, and co-work inputs, so progress and downstream-unblocking lose their attention increments and operator-demand loses its turn increment. The tables show the resulting movements; coupling remains visible but cannot cause any movement because R6 keeps it outside G.
