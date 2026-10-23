# R20 cost-row Clojure receipt

- Base/source HEAD during execution: futon2 `38c3f2b79a5d22a3737ebc6d7a7aa81c6d7961a2`, with only the new fixture/test added.
- Test/fixture commit: `3d72bfec5`.
- Branch/worktree: `wm-r20-cost-row`, `/home/joe/worktrees/futon2/wm-r20-cost-row`.
- Command: `FUTON3C_INVOKE_JOBS_FILE=$(mktemp) clojure -X:test :nses '[futon2.aif.r20-cost-row-correspondence-test]'`, launched through `futon3c/scripts/bg.py`.
- Green: 5 tests, 14 assertions, 0 failures/errors. Log SHA-256 `4764bbf06fbc0fc92ea13e3617e5e8abcc6e7ea9ade7a2d3952227459eb9b098`.

Negative mutation: temporarily reduce `token-keys` from `[:input-tokens :output-tokens :total-tokens]` to `[:total-tokens]`. The aggregate-input control went red with two failures because the mutated extractor returned `:agrees` and no input mismatch. Exit 1; log SHA-256 `d1a666c5f80999ac7bf995a6f96f34e8149bc538213e41e4489ec821e318613e`. The mutation was reverted; no production diff remains.

Gates: clj-kondo 0 errors/0 warnings (log SHA-256 `3903a3422ddaa1726828bc527020569f36e3a253c9b485f36f09be05abc6825e`); check-parens `OK` (log SHA-256 `a12b7cb43c9d9134b5bb1b35e9096b66775d9e92e7611d1cc92b02edd6782a87`).

Links: formal contract `DarkTower.WarMachine.R20CostRowCorrespondence.runtimeAgrees_iff`, mathlib4 `110192b900`; runtime symbols `apparatus-certificates/cost-row`, `registered-run-telemetry/model-usage`, and `phase-timings`, futon2 `38c3f2b79`; test namespace `futon2.aif.r20-cost-row-correspondence-test`; fixture source run90ca SHA-256 `976d1bfc7983b3cc234dbc5a25ae848e0c73b018fa2076940b31bbfc5dcdc5b9`; p4ng alignment `4bbea64243`.

The negative-integer test explicitly pins that `cost-row` itself accepts internally consistent negative integers even though the producer domain rejects them. No natural clamping or wider production claim is made.
