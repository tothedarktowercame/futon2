# Implementation records: typed close terminal — 2026-09-19

Two authorized `record-implementation!` calls succeeded. Runner-eligible count **7 → 5**. Both findings are now **:awaiting-validation**, not resolved. No refusals, no resolve! call, no clicks or other findings changed.

## Independently observed evidence

Read `grounded-review-evidence?` in `src/futon2/aif/repair_obligation.clj:112-135` before assembling the input. Live GETs to Agency `/api/alpha/invoke/jobs/<id>` established:

- Review job `invoke-1789844336541-22473-82d2ceff`: codex-23, done, executed true, 16 tool events / 16 command events.
- Author job `invoke-1789844101215-22472-8091d765`: codex-2, done, execution window 2026-09-19T18:55:02.137086412Z through 18:58:04.077710309Z.
- Exact fix `06310fcb16d74c0432125f643dc203983b404576` exists in `/home/joe/code/futon2`, committed at 18:57:15Z within that author window. Git confirmed it descends from parent `78607c2c3d4f2f9492915ff4ab1e741dec3639c4` and is an ancestor of observed HEAD `bdc0f776ffca421b5ed43167b3bc954ea88c0d31`. The diff names the cohort source, regression test, and registration spec.

`REVIEW-typed-terminal-retention-2026-09-19.md` at **bdc0f776** approves that exact fix and prerequisite `2c90fa41`. Its **Witness attestation** section supplies the bounded :resolved? / :dial-moved? grounds for the warranted regression. These flags do not assert store resolution or a later production successor. Review validity was derived from the completed executed review job plus the committed approval; artifact-binding flags came from the Git/window observations above. Reduced live observations and the exact implementation input are retained under `runs/implementation-records-typed-terminal-2026-09-19/`.

## Store results and paths

```
:before-eligible 7
:recorded "repair-ea1-259a7a93d9a9b63a45020e0ad646c508b9be83097dac6898106a063c1a1eef2e--attempt-001-close-exception" :status :awaiting-validation :path "/home/joe/code/futon2/data/wm-repair-obligations/implementations/repair-ea1-259a7a93d9a9b63a45020e0ad646c508b9be83097dac6898106a063c1a1eef2e--attempt-001-close-exception.edn"
:recorded "repair-initialization-56485d40-2f54-4882-8917-15a59a473494-initialization-failed" :status :awaiting-validation :path "/home/joe/code/futon2/data/wm-repair-obligations/implementations/repair-initialization-56485d40-2f54-4882-8917-15a59a473494-initialization-failed.edn"
:after-eligible 5
```

The original immutable findings remain intact. Byte copies of the two accepted implementation records are retained beside `implementation-input.edn`, `observations.json`, and stdout/stderr. The implementation attempt is `typed-terminal-retention-2026-09-19`, distinct from both failed attempts. Each stored witness references the review note and warrant `test-registry-8a13ae4b965011c4794f1b8f02ee0082490f68c008ee8b88048dc2a0b725e22d`.

The production-shaped successor-validation limb remains open. No test suite was re-executed; this packet only observed existing evidence and performed the two authorized implementation writes.
