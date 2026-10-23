# R20 independence-row correspondence test receipt

Date: 2026-10-23 UTC.

- Base/source HEAD during the red and green executions: futon2 `41e719d97e42a920b9d5fd91f913da83c964e9c4`, with the new fixture and test as the only ordinary working-tree additions.
- Those exact additions were committed as `b7e9a1882` before this receipt.
- Branch/worktree: `wm-r20-independence-row`, `/home/joe/worktrees/futon2/wm-r20-independence-row`.
- Environment: isolated process in the worktree; `FUTON3C_INVOKE_JOBS_FILE=$(mktemp)`; no shared-JVM reload, click, or live-store write.
- Command: `clojure -X:test :nses '[futon2.aif.r20-independence-row-correspondence-test]'`.

## Negative mutation

The sole temporary production mutation changed

```clojure
(not= (:identity author) (:identity reviewer))
```

to `true`, forcing an agreeing verdict for identical supplied identities. The test exited 1: `equal-supplied-identities-disagree` observed `:agrees/:author-is-not-reviewer` where it required `:disagrees/:same-agent`. The mutation was then reverted; `git diff` confirms no production change remains.

- Red log: `/home/joe/notes/wm-next/independence-row/futon2-forced-agree-red.log`
- Red log SHA-256: `bb9c9380f78003c75026f0f7801d33802543a5c4eabf5307546b803f383ec4ac`
- Exit record: `/home/joe/notes/wm-next/independence-row/futon2-forced-agree-red.exit` (`1`)

## Green result and gates

- Test: 3 tests, 9 assertions, 0 failures, 0 errors.
- Green log: `/home/joe/notes/wm-next/independence-row/futon2-green.log`, SHA-256 `0f1312aab011220a3c09e50b9009c57b0b862ebb20d3db836667ebbb3f64ccaf`.
- `clj-kondo --lint test/futon2/aif/r20_independence_row_correspondence_test.clj`: 0 errors, 0 warnings. Log SHA-256 `3903a3422ddaa1726828bc527020569f36e3a253c9b485f36f09be05abc6825e`.
- `emacs --batch -l /home/joe/code/futon4/dev/check-parens.el test/futon2/aif/r20_independence_row_correspondence_test.clj`: `OK`. Log SHA-256 `a12b7cb43c9d9134b5bb1b35e9096b66775d9e92e7611d1cc92b02edd6782a87`.

## Symbol and evidence links

- Logic/Lean: `DarkTower.WarMachine.R20Certificate.IndependenceRow` and `IndependenceConforms`, mathlib4 base `d80d90471a`; correspondence theorem `R20IndependenceRowCorrespondence.adapt_conforms_iff`, mathlib4 `cdfc643b90`.
- Code: `futon2.aif.wm.apparatus-certificates/identity-at` and `independence-row`, futon2 `41e719d97`, `src/futon2/aif/wm/apparatus_certificates.clj:42-56`.
- Test: `futon2.aif.r20-independence-row-correspondence-test`, futon2 `b7e9a1882`.
- Evidence: `test/resources/wm/r20-independence-row/run90ca-projection.edn`, projected verbatim from run90ca SHA-256 `976d1bfc7983b3cc234dbc5a25ae848e0c73b018fa2076940b31bbfc5dcdc5b9`.
- Paper/alignment: p4ng `plop-2026-alignment.json` at `4bbea64243`, R20 alignment entry around lines 2972-3037.

This is evidence that the real Clojure extractor conforms on the present/unequal, present/equal, and missing-role cases. The Lean theorem is the contract. Neither establishes external authenticity, producer independence, whole-certificate certification, nor run certification.
