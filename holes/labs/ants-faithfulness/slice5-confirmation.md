# Registered Slice 5 confirmation

Status: **complete**. The executable design was read from `futon6/holes/clean/slice5-confirmation.clean.edn` and passed its derived Malli schema before the executor was entered.

## Positive control — reported before treatment contrasts

The registered stop rule compares complete paired run records. Any difference abandons the run before treatment cells begin.

| Scenario | Full − no-canonical-ambiguity [95% CI] | Record-identical? |
|---|---:|---:|
| patchy | 0.0000 [0.0000, 0.0000] | yes |
| sparse | 0.0000 [0.0000, 0.0000] | yes |
| snowdrift | 0.0000 [0.0000, 0.0000] | yes |

## Per-arm yield and starvation

| Scenario | Arm | Yield mean [95% CI] | Starvation share [95% CI] |
|---|---|---:|---:|
| patchy | aif-full | 174.5100 [132.4205, 216.5995] | 0.0000 [0.0000, 0.0000] |
| patchy | no-canonical-ambiguity | 174.5100 [132.4205, 216.5995] | 0.0000 [0.0000, 0.0000] |
| sparse | aif-full | 45.2200 [8.8927, 81.5473] | 0.1333 [0.0044, 0.2622] |
| sparse | no-canonical-ambiguity | 45.2200 [8.8927, 81.5473] | 0.1333 [0.0044, 0.2622] |
| snowdrift | aif-full | 257.5533 [247.0886, 268.0180] | 0.0000 [0.0000, 0.0000] |
| snowdrift | no-canonical-ambiguity | 257.5533 [247.0886, 268.0180] | 0.0000 [0.0000, 0.0000] |
| patchy | no-directed-eig | 170.4733 [129.0432, 211.9034] | 0.0000 [0.0000, 0.0000] |
| patchy | no-info-gain | 175.1867 [132.5402, 217.8331] | 0.0000 [0.0000, 0.0000] |
| patchy | no-risk | 146.0433 [93.6854, 198.4012] | 0.1000 [-0.0138, 0.2138] |
| patchy | classic | 220.4300 [165.7880, 275.0720] | 0.1667 [0.0254, 0.3080] |
| sparse | no-directed-eig | 45.2200 [8.8927, 81.5473] | 0.1333 [0.0044, 0.2622] |
| sparse | no-info-gain | 45.0333 [8.6742, 81.3924] | 0.1667 [0.0254, 0.3080] |
| sparse | no-risk | 42.4900 [3.3312, 81.6488] | 0.6333 [0.4506, 0.8161] |
| sparse | classic | 65.5433 [18.0769, 113.0098] | 0.7000 [0.5262, 0.8738] |
| snowdrift | no-directed-eig | 257.5533 [247.0886, 268.0180] | 0.0000 [0.0000, 0.0000] |
| snowdrift | no-info-gain | 240.7533 [221.9710, 259.5357] | 0.0000 [0.0000, 0.0000] |
| snowdrift | no-risk | 299.6000 [292.3008, 306.8992] | 0.0000 [0.0000, 0.0000] |
| snowdrift | classic | 315.0000 [315.0000, 315.0000] | 0.0000 [0.0000, 0.0000] |

## Paired yield contrasts against AIF full

| Scenario | Contrast | Full − arm mean [95% CI] |
|---|---|---:|
| patchy | full−no-directed-eig | 4.0367 [-4.1571, 12.2304] |
| patchy | full−no-info-gain | -0.6767 [-6.8852, 5.5318] |
| patchy | full−no-risk | 28.4667 [-39.2090, 96.1423] |
| patchy | full−classic | -45.9200 [-100.5527, 8.7127] |
| sparse | full−no-directed-eig | 0.0000 [0.0000, 0.0000] |
| sparse | full−no-info-gain | 0.1867 [-0.1623, 0.5357] |
| sparse | full−no-risk | 2.7300 [-35.2000, 40.6600] |
| sparse | full−classic | -20.3233 [-50.9685, 10.3218] |
| snowdrift | full−no-directed-eig | 0.0000 [0.0000, 0.0000] |
| snowdrift | full−no-info-gain | 16.8000 [2.5554, 31.0446] |
| snowdrift | full−no-risk | -42.0467 [-54.2804, -29.8129] |
| snowdrift | full−classic | -57.4467 [-67.9114, -46.9820] |

## Variance

- patchy: one-way yield eta-squared = `0.0316`.
- sparse: one-way yield eta-squared = `0.0058`.
- snowdrift: one-way yield eta-squared = `0.4518`.

## Verdict

**No registered ablation establishes a yield benefit on patchy or sparse; the target-environment explore/exploit regulator is not established, while snowdrift shows a mixed off-target response.**

Raw EDN SHA-256: `964462c707120c1ea48a9ba04ca40a2996d49b6753d7dfe6929c6c5257f64d88`.

## Re-run

```bash
clojure -M -m ants.aif.experiment confirmation holes/labs/ants-faithfulness/slice5-confirmation.md
```
