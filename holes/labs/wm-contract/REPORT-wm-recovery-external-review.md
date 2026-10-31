# War Machine recovery: external review handoff

Snapshot: 2026-10-30/31 operational session. This is a coordinator handoff, not a claim that recovery is complete. Read this with [the committed evidence summary](REPORT-wm-recovery-evidence.json) and [the authoritative deficit tracker](criteria-completeness-deficits.tsv). No access to `~/notes` is needed to understand the issue or review the code; raw live-state reproduction still requires the pinned ledger and receipt artifacts described below.

## What Joe wants

A criteria-compliant machine doing useful work, with repairs between individual runs—not an interminable preflight/repair chain. Joe authorized 15 additional credits, sequential clicks, pushing commits for external review, and (most recently) restoring the exact prior authority context **and fixing the cause of its brittleness**. Codex-11 coordinates; Zai agents implement/review to conserve Codex usage.

**Current state: 15 credits available (243 allocated, 228 consumed). Further clicks are held on a demonstrated authority-validation failure.** Six of 24 deficit rows are explicitly CLOSED. The other 18 include historical failures and missing live evidence; they are not 18 launch blockers.

## Concrete progress

- Open-task coverage Q1 reached **608/608** (181 missions, 239 excursions, 188 tickets), zero missing/extras at its pinned snapshot. Scoped graph terms: 556 known, 52 unknown; unknown candidates retain typed exclusions and knowledge-producer requests, never numeric defaults.
- Latest charged click: `wm-click-3c02ad82-d6fd-45ec-97d7-7b24aa7730de`; run `2026-10-30-3415b671-88f6-4662-a2c1-48e3630f6116`. It reselected M-peradam-grounding after Q6 excluded M-typed-memories and ended **grounded-progress**, not verified task completion. Real work is futon7 `de7bf835`; wants were not shown reached. Do not infer approval/falsifier execution merely from outcome labels or absent keys.
- Outer learning consumed the old pending run f74df9cc exactly once, count 0→1, and retained the new pending selection. E2's written condition is satisfied and CLOSED in `59f5200c473db302b356658e335363837d2f4400`. Observation SHA: `ff4cc8edd414faa3c3903b63603ee0eb7b9e19933e77e3a9d7c1a2431e447bca`; post-click outer-state SHA: `a6e9b39383c2cfa74da77f8e14c76c9cb12118cd8524078a46dc7a8b29b7a3fd`.
- E3 reader metadata was restored by recreating stale registered callables through the existing WM cast installer. Implementation readiness is not the outstanding live unchanged/changed-input witness pair.
- Completed pre-construction failures now retain classification (futon2 merge `577f4aa7f`). Its live dispatch-failure closure witness remains outstanding. Historical records were not backfilled.

## The present blocker, in plain language

The injury ledger is the machine's append-only repair/history record. Reading it checks not only bytes but whether historical medic decisions had valid authority. Current serving validation rejects historical entries because the default authority composition cannot recover their earlier approved context. **The word `tampered` is the validator's refusal label, not evidence that someone altered history.**

The post-click ledger has 129 entries. The earlier 128 entries—including the disputed ones—are unchanged; the last click added one successful-task-outcome observation. Default-option probes were initially confused with actual serving probes; later diagnosis exercised the real serving path and reproduced the refusal. Effective injury count is therefore **not computable through that path**, not zero.

Thirteen medic-repaired observations fail authority checks: ordinals **38–44, 46, 48, 50, 71, 118, 125**. There are two distinct problems:

1. **Missing composition:** the default `resolve-medic-authority` branch in `full_loop_runtime.clj` used only the raw out-of-band verifier. The other resolved-context branches already used `meta-medic-authority/with-retained-authorities`. Existing entry-local verifiers for cause reattribution (118) and ordinary author refusal (125) were omitted by the default.
2. **Lost process-local context:** the other eleven historical bundle approvals require an explicit trust predicate. Their retained manifests identify **33 exact scope tuples** (evaluation/review/authorization, each bound to identity, intent digest, and receipt pin). There is no supported persistent standing install wired into the ordinary click default. This is not a license to trust all receipts from a named agent.

Three adoption-time evidence files (ordinals 41, 43, 50) changed or disappeared on current disk. Their pinned bytes are recoverable from Git commits `7ca70e10e`, `ae9e71584`, `86024a594`. Receipt/operation/trust checks passing does NOT mean those current mutable artifacts also pass. Recover historical bytes separately and check original pins; never overwrite current source or rewrite old receipts.

The eleven-entry inventory, receipt SHAs, operations, and original coordinator authorization chain are included in REPORT-wm-recovery-evidence.json. Those are identifiers and reported verification results, not substitute raw artifacts. The JSON deliberately distinguishes that evidence boundary.

## Code available for external review

Repositories: `tothedarktowercame/futon2`, `tothedarktowercame/futon3c`, `tothedarktowercame/mathlib4` on GitHub.

| Repository/ref | Pushed snapshot | Meaning |
|---|---|---|
| futon2 main | `59f5200c473db302b356658e335363837d2f4400` before this report | Integrated work and E2 closure |
| futon2 repair/ledger-authority-composition | `039342915fa8a5cb6a3e981d46d8661825c09006` | **Partial** fix, not merged; default uses existing retained-authority union with deny-all external authority |
| futon3c master | `130b272dba04c775bbcfcb71b81e36df62c73964` | Legacy Giraffe verifier compatibility integrated |
| mathlib4 darktower | `b5c7da28f46854d74b647d47edba358b007489a9` | Existing Lean model snapshot; no new trust theorem claimed |

Start with the [partial fix comparison](https://github.com/tothedarktowercame/futon2/compare/59f5200c473db302b356658e335363837d2f4400...039342915fa8a5cb6a3e981d46d8661825c09006). It changes one semantic line plus cleanup; **it reduces 13 failures to 11, not to zero**. Independent review approved it as partial: new tests 4/19; neighboring suites 5/23 and 17/82, all reported green. They prove existing entry-local behavior and fail-closed controls, not restoration of all historical trust.

Useful code search targets in futon2: `resolve-medic-authority`, `with-retained-authorities`, `resolve-context`, `verify-historical`, `verify-genuine`, `retained-authority`, `load-durable-ledger`, `effective-active-injuries`, and `carried-bindings-observation`. Relevant history: `5a1de6f3b` authority composition, `f04177cd9` cause reattribution, `1ce1efbea` ordinary-author-refusal retained authority. Inspect definitions at pinned commits rather than relying on shifting line numbers.

## Authorized recovery and brittleness work—in progress, not completed

Joe explicitly approved persistent use of precisely the previous 33 pinned trust entries covering eleven approvals, and required diagnosing/fixing why this became brittle. Zai-4 owns isolated recovery implementation; Zai-5 owns causal diagnosis/prevention design and independent review. New review-branch commits are to be pushed. No live installation, merge, or click is authorized by this REPORT itself.

Required result:

- Reuse existing `resolve-context` and grammar checks; retain exact operation/injury/prefix/receipt binding and unique-match rules. Unknown future scopes, overlaps, missing or changed bytes must fail closed.
- Retain historical evidence by pin in durable storage, independent of current source files, cwd, or a long-running JVM's memory. Original ledger and receipt bytes stay unchanged.
- Use one shared authority resolution path for actual ordinary arbitration, all supported loaders, and preflight. A refused load must not be reported as absence of injury.
- Prove the unchanged 129-entry ledger validates through the four supported consumers in a **fresh JVM**, then after restart; test wrong injury/operation, forged checked status, changed bytes, missing evidence, unsupported future scope and ambiguous overlap.
- Separate legacy recovery from future-adoption retention obligations. Restoring these approvals must not silently authorize future approvals.

The active diagnosis must distinguish established causes (default composition omission, process-local trust, mutable historical artifacts) from hypotheses. Earlier reports attributed success to old loaded ledger code and an unknown reload; **the exact JVM timeline is not established and is not a premise for the fix**.

## Why we became stuck; lessons reviewers should test

A copied ordinary-path preview bypassed arbitration, while `carried-bindings-observation` mapped any non-computed active set to `no-active-injury`. Neither proved that actual serving authority could load the ledger. The arbitration layer itself fails closed. A correction must preserve this distinction and test the actual entry point, not just validators in isolation.

A separate Giraffe change added `work-clusters.json` to a hard-coded verification baseline, rejecting old manifests pinned by pending learning. Review initially missed that. Repair `1ccf08a4a` (merged `130b272db`) selects the baseline by producer git ancestry and verifies all listed hashes. Limits: it depends on resolving producer history from the repository/cwd; content verification is not authentication. A relabeled manifest is caught only where its external manifest-byte pin is checked. Do not broaden those claims.

Two synthetic test runs once escaped hermetic fixtures because `require :reload` reset a default var inside `with-redefs`. Both were quarantined, strict synthetic tails removed with backups, and data integrity independently verified. The executor's quiescence argument was inadequate; that protocol departure was retained. Tests must set temporary roots before load and must not reload through fixture bindings. No synthetic run earns criterion credit.

## Questions for the external reviewer

1. Is the restored authority genuinely the exact prior approved context, or does any fallback broaden trust?
2. Can each historical dependency be revalidated by its original pin without restoring old source over current code or editing history?
3. Does the actual serving default and every loader use the same resolver after a cold start? Do tests exercise arbitration, not only a manually supplied verifier?
4. Are refusals preserved as typed refusals, and is credit charged only after required preconditions pass?
5. What minimal future-adoption retention contract prevents recurrence without a broad self-heal redesign?

Please prioritize a bounded recovery recommendation. Reporting polish, Q9 exporter incompleteness, old fixture drift, and skipped-run replay are separate work—not new launch gates unless they cause a concrete execution failure. E4 redesign stays parked. After reviewed recovery and actual-serving validation, resume **one click**, inspect its evidence, then continue; checkpoint after three further charged clicks. Never bypass validation merely to spend credits.

## Evidence availability and confidence

This report and its JSON inventory are Git-tracked. Raw ledger snapshots, process-local probe outputs, and many retained bundles currently live in local notes/data; their absence from an external checkout is a known reproducibility limitation, not proof they validate. The recovery branch must package the necessary pinned evidence or provide a supported reproducible fixture before claiming external reproduction. Do not copy admin tokens, session files, or unrelated private logs into review artifacts.

The committed tracker is authoritative; supplementary notes are not a replacement. Six closed rows are listed in the companion JSON. Q1 equality is snapshot-specific; standing algorithm A entries and broad live A4/B1 claims are not implied by 608/608 M/E/T coverage.

### Inventory validation correction

The first handoff copied some incomplete/inexact SHA strings from the local decision report. The companion JSON now preserves those reported strings and supplies independently recomputed SHA-256 values for all eleven retained authorization files. Its correction list names each discrepancy. This establishes file-byte hashes, not independent agreement with the ledger pins; an external review must not conflate those checks. The initial validation failure did not stop the first documentation push; this forward correction preserves that history.

## Brittleness diagnosis and accepted prevention scope

The subsequent design distinguishes established causes from hypotheses:

1. The no-options default skipped the existing retained-authority union. Tests supplied explicit contexts, so they missed the production default. This accounts for two rejected receipt types; `039342915` is the independently reviewed partial correction.
2. Eleven historical bundle approvals depended on a process-local TRUST predicate reconstructed during adoption. Their retained evidence also references mutable filesystem locations. Keeping receipt bytes alone did not preserve a restart-safe authorization context or all historical artifact dependencies.
3. Authority composition is repeated across runtime resolution, diagnosis, and loader boundaries; inconsistent defaults allowed the first defect.
4. `carried-bindings-observation` maps non-computed active sets to absence. This obscured the refusal in diagnostics; arbitration itself fails closed.
5. The claimed old-code/reload timeline remains unproved. It is not needed to justify recovery.

The approved prevention scope is a single shared authority resolver; a sealed, content-addressed record of precisely the approved trust scopes with their evidence; preservation of typed ledger refusal instead of absence; diagnostic resolver identity; and fresh-process/restart, moved-evidence, tamper, unknown-scope and pre-charge-refusal tests. This is implementation scope, **not a claim that it has landed**. New adoption writers must retain evidence needed for later revalidation; this does not automatically authorize new trust scopes. If writer changes exceed the existing retention boundary, return a concrete gap rather than silently expanding policy.

The local causal-design report is pinned in the companion JSON. External reviewers should check the implementation against these claims, especially actual no-options production entry and receipt dependencies after a cold start.

## Independent review of recovery commit 32f93e83e

**Not shippable; not integrated.** Independent review found that the policy and all 78 evidence blobs under `resources/wm/standing-authority/` were ignored and absent from this commit. A coordinator `git ls-tree` check independently confirms zero tracked files at that path in `32f93e83e`. A fresh checkout therefore cannot reproduce the worktree result: it lacks the policy and fails closed. This is a recurrence of the evidence-portability defect, not a completed recovery.

The reviewer reports 10 tests / 51 assertions passing in the populated worktree, but the advertised restart test resets context in the same JVM. It does not establish cold-process or fresh-checkout recovery. No live installation or click follows from those results.

The correction packet requires the exact evidence bundle to be tracked (approximately 152 MB, largest blob 21 MB; ordinary Git chosen, subject to sensitive-content inspection), with a narrow ignore exception. Tests must run from committed archive contents in a new JVM, without the author's notes or worktree on the classpath: the no-options production resolver and all four loaders over the retained ledger, missing/corrupt evidence and unknown-scope refusals, and independence from original mutable evidence paths. The shared-resolver and refused-versus-absent corrections remain required. Independent review must assess the resulting committed artifact before integration.

Recovery stays bounded to the eleven historical approvals and their exact 33 trust tuples. The ledger remains unchanged; 15 click credits remain. Local detailed review: `run001/renewed-click-01b/standing-authority-independent-review.json`. This section carries its actionable findings into Git so external reviewers need not rely on access to that local file.

## Review update: bb735c405 (supersedes the missing-resource blocker)

The recovery branch now tracks all 79 authority resources: policy plus 78 evidence blobs. The coordinator and independent reviewer both verified the Git tree count. `64eff9e98` fixes the ignored-resource defect; `51d3f7e1c` adds shared default resolution, typed ledger refusal reporting and authority-identity stamps. The reviewer reports that implementation approval stands. Neither approval nor tracked resources constitutes live installation: the branch remains unmerged.

The remaining review verdict at `bb735c405` is **code-correct where it runs; external reproduction insufficient**. Its added cold-check script hard-codes Joe's ledger path, does not assert the expected ledger SHA, and sends four loader checks to the original rather than its temporary copy. Its original-path check uses a phantom `/tmp/sa-archive` prefix, and its ordinal-50 absence check depends on this host. The reviewer source-read this script but did not execute it against the live ledger; earlier author-reported archive runs must be understood with these limits.

The bounded correction is already assigned: accept an explicit ledger input and expected SHA, verify before use, run every loader against the verified temporary copy, remove vacuous/host-specific assertions, and document how an external reviewer obtains the pinned fixture and required sibling dependencies. These are verification-script corrections, not new trust semantics. A corrected archive/fresh-process witness and independent review remain the release gate. No merge, serving reload, ledger mutation or click has been authorized by this review; 15 credits remain.

## Update 2026-10-31: admission (not probe) path fixed; one click executed

**Correction of an earlier false completion implication.** The live probes
(verifier-less `:ready` twice) exercised opts *without* an installed verifier
and so could not see the admission defect: with no medic-authority provider
installed, `futon3c` `acquire-medic-authority` injected a deny-all verifier
into the **admission** opts, and `meta-self-heal-preflight!` runs *before* the
runtime dissolves caller verifiers. Live run `2026-10-31-bc036e43…` refused
`:meta-self-heal/ledger-unreadable` pre-ration on valid pinned bytes.
Probe readiness never established admission readiness; the reconciliation's
"safe by construction" claim was wrong at this ordering.

- Root cause and fix: futon3c `fix/preflight-standing-verifier`
  `b132d287875fd0ce1b13d39a20169334fd24fbc9` — install **no** verifier when no
  provider exists (standing resolver composes downstream, still fail-closed);
  provider path unchanged. Independently approved; merged `--no-ff` to
  futon3c master as `657aa7d1e` (+ reload record `9ca62572e`), pushed. The
  serving namespace was reloaded from canonical via the recorded loader
  (`apm-reload.sh`, condition C-20 @ `657aa7d1e`); all `defonce` state
  (status, completion, acceptance-lock, provider atom) preserved.
- Live admission witness before clicking: actual `acquire-medic-authority`
  output on ordinary-route opts with the real `{generation 0, provider nil}`
  state — **no verifier injected**, `:authority {:status :absent}` — fed to
  the real `meta-self-heal-preflight!`: passed, no refusal. Standing context
  `:ready`, 11 scopes; all four ledger loaders `:loaded` 129/0 on the pinned
  ledger; effective injuries `:computed`, 0 active; code composition
  `:current`.
- **The click**: `wm-click-eaa9bc3a-9689-4f6c-8fd7-3bacdc47814a`
  (2026-10-31T01:10:40Z, caller codex-11, ask-seats zai-1/2/3). Charged:
  budget 243/229/**14**. Ran selection → cascade-resume →
  meta-terminal-injury-ingest and reached **terminal `outcome: incomplete`**
  (no retry). It opened a new injury `injury/adf04df0f31ab8727ac57d9c7f2496d8fa28598c`
  (`:author-delivery-wrong-repository`, stage `:cascade-resume`, attempt 1,
  ordinal 130). Post-click ledger validates `:loaded` 130 entries under the
  standing verifier. Run record
  `data/wm-runs/tick-run-record-2026-10-31-f805ca4b-6c35-45cf-accc-f95dd54b0fc8.edn`;
  run binding `wm-click-run-bindings/click-run-binding-wm-click-eaa9bc3a….edn`.
  Route-check producer verdict: `:not-checked` (`ordinary-selection-not-established`).
- Protected-bytes note: ledger.edn (129→130 append), outer-state.edn and
  consumption.jsonl changed as **legitimate run writes** (charge + learning
  observation); outer-makeup.edn unchanged. Pre-click all four matched their
  pins exactly.
- Deficit-row closure: none claimed here; each closure still requires its own
  written condition review. The pre-ration refusal class itself
  (`:meta-self-heal/ledger-unreadable` on valid no-provider admission) is
  demonstrated fixed by the paired cold-boot witness
  (`test/f4a-cold-boot-no-provider` red `df714ad4d` / green `3b6dbc5cf`).
  The reviewed test-only delta was merged to futon3c master as `7eaaa635f`
  (one new 177-line test file, no source changes) at the idle boundary after
  the click; no reload performed. Review boundary: the service entry
  `runner-service/click!`, not the HTTP transport.

## Update 2026-10-31 (later): descriptor typing fix merged + reloaded; review corrections

- **Corrections to the direct-repair packet, per zai-5's independent review**
  (`run001/renewed-click-02-admission-fixed/descriptor-typing-review-2026-10-31.md`,
  dispatch codex-11 8531; approval of `c96bd9871` relayed in 8533):
  - The author contract requiring the **full 40-hex SHA was already explicit
    in the pre-fix prompt** (`open-cascade-author-prompt`, main `9d43b702b`);
    the 9-char `8561ecdc7` was a violation of a written contract. The
    machine's `:author-delivery-wrong-repository` classification remains
    **false** (bytes were on canonical main ~19.5 h pre-click). We do **not**
    claim the actually dispatched author prompt was retained or re-examined;
    the contract statement is cited from source at the pinned revision.
  - Red-count correction: on unpatched base `9d43b702b` the descriptor test
    fails **8 assertions, not 6** (reviewer's isolated rerun; the two
    `:supplied-commit`-retention / no-location-scan control assertions at
    lines 65–66 also fail pre-fix). Green on fix: 4 tests / 22 assertions.
    The reviewer's earlier "2 effective injuries" is **retracted** — it
    counted the two top-level map keys of the accessor result; the correct
    effective active count is **1** (`injury/adf04df0…`), matching the
    serving resolution.
- **Merge + serving reload (dispatch codex-11 8533):** approved exact SHA
  `c96bd9871a7f9af95f35d73528dd67dacc8b52e8` merged `--no-ff` to futon2 main
  as `f9d40b6e8`, pushed; concurrent untracked evidence files preserved
  (merge touched only the three committed paths). Under the actual serving
  acceptance-lock/idleness guard (`:running? false`, `:phase nil`,
  provider `{generation 0, provider nil}`) the two changed production
  namespaces were reloaded from canonical (`(require 'ns :reload)` via the
  drawbridge loader; JVM classpath resolves `futon2.aif.*` to
  `/home/joe/code/futon2/src` at `f9d40b6e8`): `futon2.aif.wm.cascade-patch-delivery`
  and `futon2.aif.full-loop-runtime`. Neither namespace holds top-level
  mutable state or `defonce` (all atoms are function-local), so the reload is
  state-neutral. Live code identity verified: `commit-descriptor-error` present
  with the adf04df0 docstring; the emitting check contains
  `:author-delivery-invalid-descriptor`; the loaded author prompt states the
  full-SHA rule.
- **Protected bytes:** no ledger mutation, POST, credit spend or repair
  attempt in this packet. Ledger re-read after reload: `:status :loaded`,
  130 entries, sha256 `ca73914f0c26fb1435a0497398a191302c46da705dc09da7724b4f4998371a44`
  (identical to the post-click pin). Budget unchanged 243/229/14.
- **Terminal-classification followup stays separate:** the reviewer's typed
  abstention proposal (`:outcome :abstained` for descriptor misses) conflicts
  with the Joe rule that WM must not abstain, and is **not implemented**.
  The existing bounded correction mechanism (exact `[injury-id ordinal]`
  medic corrections) remains the supported relabeling path; nothing widened
  here. A future descriptor miss will mint a new typed injury
  (`:author-delivery-invalid-descriptor`) rather than a false wrong-repo one.

