# R-0: discriminating-environment baseline

Status: **complete**. CLean/Malli validation fired before both producers.

## Positive control — reported first

Every `:no-canonical-ambiguity` record must equal its paired `:aif-full` record; any mismatch stops before treatments.

| Grid | Patchy | Sparse | Snowdrift |
|---|---:|---:|---:|
| 10x10 | 30/30 | 30/30 | 30/30 |
| 24x24 | 30/30 | 30/30 | 30/30 |
| 36x36 | 30/30 | 30/30 | 30/30 |

## Primary readout: paired record identity

| Grid | Scenario | aif-full | no-canonical-ambiguity | no-directed-eig | no-info-gain | no-risk | classic |
|---|---|---:|---:|---:|---:|---:|---:|
| 10x10 | patchy | 30/30 | 30/30 | 27/30 | 28/30 | 3/30 | 0/30 |
| 10x10 | sparse | 30/30 | 30/30 | 30/30 | 27/30 | 8/30 | 0/30 |
| 10x10 | snowdrift | 30/30 | 30/30 | 30/30 | 26/30 | 0/30 | 0/30 |
| 24x24 | patchy | 30/30 | 30/30 | 30/30 | 28/30 | 16/30 | 15/30 |
| 24x24 | sparse | 30/30 | 30/30 | 30/30 | 28/30 | 23/30 | 23/30 |
| 24x24 | snowdrift | 30/30 | 30/30 | 27/30 | 30/30 | 0/30 | 0/30 |
| 36x36 | patchy | 30/30 | 30/30 | 30/30 | 30/30 | 28/30 | 27/30 |
| 36x36 | sparse | 30/30 | 30/30 | 30/30 | 30/30 | 26/30 | 27/30 |
| 36x36 | snowdrift | 30/30 | 30/30 | 23/30 | 29/30 | 1/30 | 0/30 |

Directed-EIG 30/30 survives scaling: **no**.

## Secondary: yield and starvation

| Grid | Scenario | Arm | Yield mean [95% CI] | Starvation [95% CI] |
|---|---|---|---:|---:|
| 10x10 | patchy | aif-full | 173.9267 [126.8218, 221.0315] | 0.0000 [0.0000, 0.0000] |
| 10x10 | patchy | no-canonical-ambiguity | 173.9267 [126.8218, 221.0315] | 0.0000 [0.0000, 0.0000] |
| 10x10 | patchy | no-directed-eig | 176.4467 [129.5081, 223.3852] | 0.0000 [0.0000, 0.0000] |
| 10x10 | patchy | no-info-gain | 179.9233 [132.5794, 227.2672] | 0.0000 [0.0000, 0.0000] |
| 10x10 | patchy | no-risk | 209.8133 [159.2426, 260.3841] | 0.0667 [-0.0279, 0.1613] |
| 10x10 | patchy | classic | 230.8833 [178.2289, 283.5378] | 0.1000 [-0.0138, 0.2138] |
| 10x10 | sparse | aif-full | 34.2067 [2.7780, 65.6353] | 0.2667 [0.0990, 0.4344] |
| 10x10 | sparse | no-canonical-ambiguity | 34.2067 [2.7780, 65.6353] | 0.2667 [0.0990, 0.4344] |
| 10x10 | sparse | no-directed-eig | 34.2067 [2.7780, 65.6353] | 0.2667 [0.0990, 0.4344] |
| 10x10 | sparse | no-info-gain | 35.9800 [4.1529, 67.8071] | 0.2667 [0.0990, 0.4344] |
| 10x10 | sparse | no-risk | 31.8033 [-2.9485, 66.5552] | 0.6667 [0.4879, 0.8454] |
| 10x10 | sparse | classic | 52.5000 [8.0329, 96.9671] | 0.8000 [0.6483, 0.9517] |
| 10x10 | snowdrift | aif-full | 253.9600 [244.8386, 263.0814] | 0.0000 [0.0000, 0.0000] |
| 10x10 | snowdrift | no-canonical-ambiguity | 253.9600 [244.8386, 263.0814] | 0.0000 [0.0000, 0.0000] |
| 10x10 | snowdrift | no-directed-eig | 253.9600 [244.8386, 263.0814] | 0.0000 [0.0000, 0.0000] |
| 10x10 | snowdrift | no-info-gain | 240.5200 [224.0756, 256.9644] | 0.0000 [0.0000, 0.0000] |
| 10x10 | snowdrift | no-risk | 293.3233 [284.2208, 302.4259] | 0.0000 [0.0000, 0.0000] |
| 10x10 | snowdrift | classic | 315.0000 [315.0000, 315.0000] | 0.0000 [0.0000, 0.0000] |
| 24x24 | patchy | aif-full | 93.4967 [7.5929, 179.4004] | 0.5000 [0.3104, 0.6896] |
| 24x24 | patchy | no-canonical-ambiguity | 93.4967 [7.5929, 179.4004] | 0.5000 [0.3104, 0.6896] |
| 24x24 | patchy | no-directed-eig | 93.4967 [7.5929, 179.4004] | 0.5000 [0.3104, 0.6896] |
| 24x24 | patchy | no-info-gain | 93.0300 [7.0636, 178.9964] | 0.5000 [0.3104, 0.6896] |
| 24x24 | patchy | no-risk | 76.7433 [-8.7104, 162.1971] | 0.6667 [0.4879, 0.8454] |
| 24x24 | patchy | classic | 151.2000 [36.5772, 265.8228] | 0.7333 [0.5656, 0.9010] |
| 24x24 | sparse | aif-full | 16.3333 [-15.0103, 47.6770] | 0.8000 [0.6483, 0.9517] |
| 24x24 | sparse | no-canonical-ambiguity | 16.3333 [-15.0103, 47.6770] | 0.8000 [0.6483, 0.9517] |
| 24x24 | sparse | no-directed-eig | 16.3333 [-15.0103, 47.6770] | 0.8000 [0.6483, 0.9517] |
| 24x24 | sparse | no-info-gain | 16.7767 [-15.6683, 49.2216] | 0.8000 [0.6483, 0.9517] |
| 24x24 | sparse | no-risk | 27.6733 [-23.3049, 78.6516] | 0.8333 [0.6920, 0.9746] |
| 24x24 | sparse | classic | 56.6067 [-15.2874, 128.5007] | 0.9000 [0.7862, 1.0138] |
| 24x24 | snowdrift | aif-full | 510.9533 [454.3315, 567.5752] | 0.0000 [0.0000, 0.0000] |
| 24x24 | snowdrift | no-canonical-ambiguity | 510.9533 [454.3315, 567.5752] | 0.0000 [0.0000, 0.0000] |
| 24x24 | snowdrift | no-directed-eig | 495.1333 [438.6425, 551.6241] | 0.0000 [0.0000, 0.0000] |
| 24x24 | snowdrift | no-info-gain | 510.9533 [454.3315, 567.5752] | 0.0000 [0.0000, 0.0000] |
| 24x24 | snowdrift | no-risk | 718.3167 [696.5968, 740.0365] | 0.0000 [0.0000, 0.0000] |
| 24x24 | snowdrift | classic | 756.0000 [756.0000, 756.0000] | 0.0000 [0.0000, 0.0000] |
| 36x36 | patchy | aif-full | 75.3667 [-31.4576, 182.1909] | 0.9000 [0.7862, 1.0138] |
| 36x36 | patchy | no-canonical-ambiguity | 75.3667 [-31.4576, 182.1909] | 0.9000 [0.7862, 1.0138] |
| 36x36 | patchy | no-directed-eig | 75.3667 [-31.4576, 182.1909] | 0.9000 [0.7862, 1.0138] |
| 36x36 | patchy | no-info-gain | 75.3667 [-31.4576, 182.1909] | 0.9000 [0.7862, 1.0138] |
| 36x36 | patchy | no-risk | 37.5900 [-39.0703, 114.2503] | 0.9333 [0.8387, 1.0279] |
| 36x36 | patchy | classic | 37.8233 [-39.3626, 115.0093] | 0.9333 [0.8387, 1.0279] |
| 36x36 | sparse | aif-full | 0.3500 [-0.1798, 0.8798] | 0.8667 [0.7378, 0.9956] |
| 36x36 | sparse | no-canonical-ambiguity | 0.3500 [-0.1798, 0.8798] | 0.8667 [0.7378, 0.9956] |
| 36x36 | sparse | no-directed-eig | 0.3500 [-0.1798, 0.8798] | 0.8667 [0.7378, 0.9956] |
| 36x36 | sparse | no-info-gain | 0.3500 [-0.1798, 0.8798] | 0.8667 [0.7378, 0.9956] |
| 36x36 | sparse | no-risk | 39.8533 [-35.6934, 115.4000] | 0.9000 [0.7862, 1.0138] |
| 36x36 | sparse | classic | 37.8000 [-39.3383, 114.9383] | 0.9333 [0.8387, 1.0279] |
| 36x36 | snowdrift | aif-full | 847.0700 [750.6602, 943.4798] | 0.0000 [0.0000, 0.0000] |
| 36x36 | snowdrift | no-canonical-ambiguity | 847.0700 [750.6602, 943.4798] | 0.0000 [0.0000, 0.0000] |
| 36x36 | snowdrift | no-directed-eig | 828.2633 [721.1530, 935.3736] | 0.0000 [0.0000, 0.0000] |
| 36x36 | snowdrift | no-info-gain | 847.7000 [751.7101, 943.6899] | 0.0000 [0.0000, 0.0000] |
| 36x36 | snowdrift | no-risk | 1081.1967 [1048.0326, 1114.3607] | 0.0000 [0.0000, 0.0000] |
| 36x36 | snowdrift | classic | 1134.0000 [1134.0000, 1134.0000] | 0.0000 [0.0000, 0.0000] |

## Preconditions and reproduction

All 54 per-cell effective-lambda manipulation checks passed. Ticks scaled 300/720/1080 with grid diameter; ants and patch counts remained fixed as registered.

Two independently executed producers were byte-identical before reproduction metadata. Producer SHA-256: `268d2dc7fa75876b5a1fb7b93c8f1470b753f4537446ed3639e8dbbba71a43c9`. Final EDN SHA-256: `bbf9b521bb2603f340d6b5d69240b24dd5ad4d6890f4326aa0467de947631053`.

## Re-run

```bash
clojure -M -m ants.aif.experiment r0 holes/labs/ants-faithfulness/r0-environment.md
```
