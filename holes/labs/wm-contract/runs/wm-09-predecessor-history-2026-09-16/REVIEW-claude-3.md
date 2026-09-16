# WM-09-predecessor-history-1 diagnostic return: claude-3 review

**Reviewer verdict: ACCEPT the diagnostic evidence, and ACCEPT the stated blocker as sufficient under packet revision 1 (p4ng `15aae6d`). This is not repair acceptance.** futon2 `7e738d53` (author codex-7) reproduces three false predecessor results through the real `previous!` in isolated temporary stores. Its bounded source argument holds: none of the inspected schema, producer or epoch fields tells legacy history apart from damaged modern history. My extra controls strengthen the diagnosis in two ways. First, the resets happen even when the manifest is consistent. Second, when no older record exists, a damaged latest record returns a legitimate-looking initial cascade.

The blocker has since been overtaken. Revision 2 of the packet (p4ng `55bf8c3`, relaying Joe) drops the legacy-preservation condition and authorizes codex-7 to implement now. The legacy-recognition question this return blocks on is therefore no longer on the path. This review says nothing about that implementation, which was in progress in the shared worktree during review (see Limits).

Requested by codex-28 (bell `invoke-1789574078443-21496-7dc3b6d8`). Item owner wm-09 / zai-17.

## Scope and pins

- **Commit scope.** `7e738d53` adds 9 files, all under this receipt directory, and changes no source or test. Neither `receipt_construction.clj` nor its test has any commit after `7e738d53`.
- **Pins.**
  - `receipt_construction.clj` `1b4f766c…`, its test `72f4876f…` and `full_loop_cohort.clj` `619281d6…` equal the committed blobs.
  - The two July legacy files `8c4aa77a…` and `99ec6d5d…` match on disk.
  - `full_loop_runner.clj` `aa258d74…` was pinned from a working tree that differed from `b61f1a6d`, as `SOURCE-PINS.json` states. It was read, not tested, so this doesn't affect the diagnosis.
- **Epoch comparison.** `(def semantic-epoch :full-loop-real-actuation-v6)` is present at `24dc6068^`, at `24dc6068` and at `7e738d53`. The string was introduced in `cf7e5389` on 2026-07-16.

## Reruns (commit-exact source)

During review the shared worktree's `receipt_construction.clj` was being rewritten by the revision-2 implementation (sha256 `4674b63d…`). My first reviewer-probe run loaded that copy and returned refusal kinds that do not exist at `7e738d53`, so I discarded it. I then prepended `git show 7e738d53:` copies of the source and test to the `-M:test` classpath and ran each check in a short-lived JVM (`review-claude-3/commands.txt`). Nothing else on `receipt_construction`'s require graph differs from the commit.

- **Author probe:** exit 0, empty stderr. Output is identical to `before.edn` apart from temporary paths (`author-probe-rerun.out`).
- **Namespace tests:** `receipt-construction-test` gives 7 tests and 31 assertions, 0 failures and 0 errors (`namespace-tests-at-7e738d53.log`).
- **Gates:** clj-kondo 0/0 and check-parens exit 0 on `probe.clj` and on my probe, each linted separately (`gates.log`).
- **Cleanup:** no temporary directories remain after the runs.

## Do the controls distinguish the defects?

Yes, with one caveat. `valid-modern` carries epoch `:newer`, the cascade-removed and construction-missing cases carry `:older`, and the carrier-removed cases return `:legacy-predecessor-no-ruled-admissions`. The outcomes differ from each other, and the three assertions pin the wrong results, so the probe cannot pass once the repair changes them. The report labels this as reproduction, not acceptance.

The caveat: the author's mutations edit construction files **without resealing the close manifest**. So `carrier-malformed` refuses with `:previous-manifest-source-mismatch`, a digest check, and does not exercise carrier validation. The unsealed edits also leave open whether the other resets depend on inconsistent evidence. My probe rebuilds the manifest after each mutation (`reviewer-probe.clj`, `.out`):

| Reviewer case | Result at `7e738d53` | What it adds |
|---|---|---|
| Resealed, unchanged | carries `:newer` | The resealing helper itself is sound |
| Resealed, carrier removed (older valid present) | legacy reset | The reset never reads the manifest; the defect is not an artefact of digest mismatch |
| Resealed, carrier `{}` | refuses `:shape-invalid` at `[:identity]` | Carrier validation does refuse, but the error carries no construction-level refusal kind or file |
| Resealed, `:cascade` removed (older valid present) | carries **`:older`** | Exposure of an older record is independent of sealing |
| Carrier removed, no older record | legacy reset | — |
| `:cascade` removed, no older record | **`:first-attempt-no-admissions`, status `:none`** | Damaged history looks like a genuine absence of history |
| Construction file deleted, no older record | **`:first-attempt-no-admissions`, status `:none`** | Same |
| Close file truncated | untyped `RuntimeException` | Discovery failure is not a typed refusal |
| Close `:recorded-at` removed | `NullPointerException` | Same |

The last four rows are not in the report. They bear directly on revision 2's deliverable 4, which asks for typed discovery refusals and says initial cascade requires established absence. The no-older rows are the case where the defect is least visible: the result carries no legacy status to notice.

## Historical format argument

I checked each step against the files.

- **The July example.** `wm-outer-loop-41-v1/attempt-043` has `:event/schema-version 1` on both construction and close. Its judgment keys are `:cascade :deposit :mission :patterns :sorries :trace-path :wiring`, with no carrier. The close payload has only `:ground :judgment`: no retention block, manifest or occurrence.
- **The producer.** `full_loop_cohort.clj` still writes `:event/schema-version 1` and validates `(= 1 …)`.
- **The epoch.** The July start event records `:full-loop-real-actuation-v6`. That is the same epoch declared on both sides of receipt mode's introduction.
- **Local census** (`local-epoch-census.txt`), over the `001-time-step.edn` files under `data/wm-full-loop*`: 80 are v6, and 1, 3, 1 and 1 are v1, v3, v4 and v5. Of the 85 `003-construction.edn` files there, **0** carry `:receipted-construction`.

So every distinguishing feature I found is an absence of modern fields, or a date, which the packet excludes. The fields present on legacy records (schema version, epoch) are shared with receipt-mode records. The argument is bounded as the report says. It covers inspected source, one named legacy attempt and these local roots, not an external authority. I accept it at that scope.

## Note for the revision-2 repair and owner

Because 0 of 85 local retained constructions carry a carrier, removing the reset means that any current target whose latest retained history is among them will refuse, not start fresh. Revision 2 asks for exactly that ("Old records that cannot satisfy these checks refuse with source provenance"). The owner should expect it to affect every target with local history until modern receipted closes exist. This is an observation for the owner, not a finding against `7e738d53`.

## Limits

- The review covers `7e738d53` only. I did not read or assess the uncommitted revision-2 edits except to discard the one run that loaded them.
- No production stores were probed, and all histories were temporary. The census read retained local files without writing.
- No shared-JVM load, click, source or test edit, DAG or checklist change, or dispatch. No successor selected.
- The external-authority question stays open, as the report says.
