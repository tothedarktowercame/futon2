# L5-15 — Completion discovery is not a selection baseline

Base: f12c1e513; branch: codex11/l5-15. No merge, reload, live click or canonical data write.

## Change

`task-completion-surface/describe` emits `:wm/completion-surface-v2` with `:baseline-phase :work` and `:baseline-producer :selected-task-revision-baseline`, replacing the unconditional absent baseline. Discovery classes, counts and source pin remain unchanged. The selection exporter counts v2 `:completion-definition-not-found` as a deficit at `:completion-class`. It does not mask baseline absences: malformed v2 with an absent baseline still counts. Required status, rank, cone, unblocks, reach and pin absences remain visible. Work-side absence traversal is untouched and tested.

## Before/after re-export

Retained input: selection-record-2026-11-01-wm-click-7adb0f36-8dce-43b1-933b-95eaedd52f2a.edn (click 11), pick E-comprehension-foundation. Entire historical export is structurally equal on main and branch, including its two absences. No historical certification changes.

For a fresh fixture copy, only the picked completion is replaced by the real producer's discovery output. Source has changed since click 11: original bytes were recovered read-only from futon6 commit 01163dd, path holes/excursions/E-comprehension-foundation.md, and verified against the retained sha256 6863756378922c1aa289e75c0c750f04bc66e797e04dae721f3c9e39b1afb663. Both arms use those same recovered bytes under the notes directory, not the changed canonical source.

| Export | Main | Branch |
| --- | --- | --- |
| Retained click 11 pathAbsenceCount | 2 | 2 |
| Fresh fixture pathAbsenceCount | 2 | 1 |
| Fresh completion baseline absence | present | absent |
| Learning ending classification absence | present | present |

Only fresh RunFacts differences are pathAbsenceCount and selectionPath.receipt.observed (the section digest changes with the new discovery carrier). Relation, reach and remaining path data are unchanged. No Q7-holds claim: learning classification absence remains. Re-export scripts and full outputs are retained under /home/joe/notes/l5-15/ (export.clj, main-export.edn, branch-export.edn, export-comparison.edn).

## Readers and owner finding

- futon3c/src/futon3c/giraffe/excursions.clj:29,60 calls describe for excursions/tickets; combined-pass at line 205 copies completion opaquely.
- futon2/src/futon2/aif/outer_observation.clj:195 carries completion into field facts without schema dispatch.
- scripts/wm_run_facts.clj selection-absence-paths traverses retained v1 as before; the only new interpretation is v2 undefined completion. Work-side absence-paths is unchanged.
- Giraffe missions.clj:62–84 omits completion while E/T carry discovery. This census inconsistency belongs to the Giraffe owner; no census changes here.
- Giraffe test/futon3c/giraffe/ticket_admission_test.clj:48 explicitly expects v1 for freshly produced carriers. Owner must update that schema expectation to v2; producer/class assertions remain compatible. excursions_test.clj:33,39–42 checks producer/class, not the schema. No cross-repository edits or Giraffe test run in this packet.

## Validation

Hermetic runner /home/joe/notes/l5-15/run-tests.py routes discovered FUTON_WM_* path variables to scratch. Full outer namespaces plus wm-run-facts-test and wm-check-run-facts-test: main and branch each 180 tests / 775 assertions, 19 failures / 2 errors, identical failure-name multiplicities. These are baseline failures, not a green suite claim. New task-completion-surface-test: 4 tests / 17 assertions, zero failures/errors. An initial test expected four unchecked boxes from a fixture containing one checked box; corrected expectation to three, then reran successfully.

Canonical data file count before/after each run: 45942 → 45942. Final clj-kondo: zero errors/warnings; check-parens.el: OK; git diff --check clean. No production data writes or service evaluation.

## Evidence sha256

- main.log: `45a4b3c89a9a608d69e8313f06d0d9e85fac9734cbafa9eddb92615e30a9d97b`
- branch.log: `4f41a20adf9af30fa42704569f67e02585bf62bff855e2e566f172f44c9644f1`
- named-final2.log: `8cc65686a09db8255b8851b306028332d97f668dac505159ed97610e4a3593bd`
- main-export.edn: `54a68458859c637c26f2d0ae662f26f7fbce8d35ef47fa80f4a5693628d599dd`
- branch-export.edn: `e0d1f35f9c6fadfaea11e93f1690abc30821cf356133b520139ade349f69c979`
- export-comparison.edn: `d645807fe2832876fdd5a18ec107ce90c9039ca1c2c30c3626df6a3e06ec85d5`
- kondo-final.log: `ccc0643f1ca2620c167b431279a3d9e6ba34c8a776558e7cfff3511d442a12b6`
- parens-final.log: `a12b7cb43c9d9134b5bb1b35e9096b66775d9e92e7611d1cc92b02edd6782a87`
