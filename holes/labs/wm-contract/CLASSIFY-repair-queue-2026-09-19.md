# Classification of the 20 runner-eligible repair findings

Status: read-only classification follow-up to
`FINDING-repair-branch-bypass-2026-09-19.md` at commit `5cc4f958`.
No click, store transition, repair, or production change is authorized here.

Classification cutoff: 2026-09-19, after futon2 commit `5cc4f958`.  A row is
“dispatcher artifact” only when the retained job never executed and failed in
dispatch/transport (including `input_too_large`), per the requested definition.
Quota loss after tools ran is operational rather than substantive, but is not
called a dispatcher artifact under that strict definition.

## One-row-per-finding classification

| Repair id / finding file | Failure type | Retained underlying cause | Dispatcher artifact? | What a real repair would require if substantive |
|---|---|---|---|---|
| `repair-ea1-a7a5fc7c81ad45251922d33718170eba32a100cda770df0af9bcd759e28df913--attempt-001-artifact-binding-mismatch` — `data/wm-repair-obligations/findings/repair-ea1-a7a5fc7c81ad45251922d33718170eba32a100cda770df0af9bcd759e28df913--attempt-001-artifact-binding-mismatch.edn` | `:artifact-binding-mismatch`, build resolution | A retained test-shaped job (`retention-author`, commit `abc123`) claimed `/repo`, while repository resolution was a temp path `/tmp/debug-standing-readback…`; error: “Observed author artifact resolved outside the target repository.” | No; it executed 3 tool/3 command events. | This is fixture leakage into the live default repair store, not evidence of a product repair. A substantive fix would make the test/store boundary hermetic and prove no default-root writes, then retire this immutable finding through an authorized transition rather than pretending `abc123` is a repair. |
| `repair-ea1-b28b40fe3c109454107faa2309cfcf0e51abf79e39c436fda76a2d7d665f1913--attempt-001-artifact-binding-mismatch` — `data/wm-repair-obligations/findings/repair-ea1-b28b40fe3c109454107faa2309cfcf0e51abf79e39c436fda76a2d7d665f1913--attempt-001-artifact-binding-mismatch.edn` | `:artifact-binding-mismatch`, build resolution | Same test-shaped `retention-author`/`abc123` record; repository resolved to `/tmp/debug-baseline…`, not claimed `/repo`. | No; it executed 3 tool/3 command events. | Same fixture-isolation repair and independently authorized ledger disposition; no code artifact named `abc123` can substantively discharge it. |
| `repair-ea1-259a7a93d9a9b63a45020e0ad646c508b9be83097dac6898106a063c1a1eef2e--attempt-001-close-exception` — `data/wm-repair-obligations/findings/repair-ea1-259a7a93d9a9b63a45020e0ad646c508b9be83097dac6898106a063c1a1eef2e--attempt-001-close-exception.edn` | `:close-exception` | Close fallback itself rejected `invalid close outcome`. It shares attempt/time with the typed `:revision-unchanged` row below, so it is the containment-level consequence, not a second independent action failure. | No. | If still reproducible, accept the typed close-failure outcome through the cohort writer exactly once and retain the originating refusal without minting a second generic finding; demonstrate a durable 007 close. |
| `repair-ea1-259a7a93d9a9b63a45020e0ad646c508b9be83097dac6898106a063c1a1eef2e--attempt-001-revision-unchanged` — `data/wm-repair-obligations/findings/repair-ea1-259a7a93d9a9b63a45020e0ad646c508b9be83097dac6898106a063c1a1eef2e--attempt-001-revision-unchanged.edn` | `:revision-unchanged`, close | Limb evidence refused at `[:revision-pair]`: before and after were byte-identical. | No. | Not a machine-code repair unless validation is wrong: deposit honestly distinct before/after subject bytes, or omit the revision claim; then prove admission without weakening `:revision-unchanged`. |
| `repair-initialization-56485d40-2f54-4882-8917-15a59a473494-initialization-failed` — `data/wm-repair-obligations/findings/repair-initialization-56485d40-2f54-4882-8917-15a59a473494-initialization-failed.edn` | `:initialization-failed` | Initialization captured `clojure.lang.ExceptionInfo`, message `invalid close outcome`; this is the outer initialization record for the same close-contract class, with no author dispatch. | No. | If independently reproducible, make initialization retain and accept the typed close terminal rather than collapsing it to initialization failure; prove one terminal/finding. |
| `repair-ea1-259a7a93d9a9b63a45020e0ad646c508b9be83097dac6898106a063c1a1eef2e--attempt-002-build-failed` — `data/wm-repair-obligations/findings/repair-ea1-259a7a93d9a9b63a45020e0ad646c508b9be83097dac6898106a063c1a1eef2e--attempt-002-build-failed.edn` | `:build-failed`, author wait | Agency HTTP 429 weekly/monthly quota exhaustion for `zai-5`; job shows 18 tools, 0 commands before terminal failure. | No under the strict definition because execution began; operational/seat-capacity artifact. | No product repair follows from the payload. Retry with an authorized available seat after quota reset; only a repeated correctly provisioned failure could justify mechanism work. |
| `repair-ea1-259a7a93d9a9b63a45020e0ad646c508b9be83097dac6898106a063c1a1eef2e--attempt-003-build-failed` — `data/wm-repair-obligations/findings/repair-ea1-259a7a93d9a9b63a45020e0ad646c508b9be83097dac6898106a063c1a1eef2e--attempt-003-build-failed.edn` | `:build-failed`, author wait | Agency HTTP 429 quota exhaustion for `zai-5`; `:execution {:executed false :tool-events 0 :command-events 0}`. | **Yes**: never-executed service/dispatch availability failure. | No substantive repair evidenced. Replay on an authorized available seat and dispose of the historical finding through the ledger contract. |
| `repair-ea1-9b6ce3a47b9bdc24a5fc6a9b9aab4f270af86a4fcb54550d94b89737bb5e4f35--attempt-001-artifact-binding-mismatch` — `data/wm-repair-obligations/findings/repair-ea1-9b6ce3a47b9bdc24a5fc6a9b9aab4f270af86a4fcb54550d94b89737bb5e4f35--attempt-001-artifact-binding-mismatch.edn` | `:artifact-binding-mismatch`, artifact binding | Author completed real commit `05d88989975d…`, but text artifact ref was abbreviated `05d88989`; retained binder set `text-artifact-sha nil`, `disagreement? true`, and refused despite observed HEAD being that commit. | No; 30 tools/30 commands executed. | Preserve exact commit identity across the response/binder boundary (or safely resolve an unambiguous abbreviation under the ruled contract), with freshness and repository checks; validate the actual authored commit rather than substituting HEAD. |
| `repair-ea1-9b6ce3a47b9bdc24a5fc6a9b9aab4f270af86a4fcb54550d94b89737bb5e4f35--attempt-003-evidence-not-single-edn` — `data/wm-repair-obligations/findings/repair-ea1-9b6ce3a47b9bdc24a5fc6a9b9aab4f270af86a4fcb54550d94b89737bb5e4f35--attempt-003-evidence-not-single-edn.edn` | `:evidence-not-single-edn`, close | Unreferenced `data/wm-full-loop-machinery-55/wm-contract-machinery-55-v1/attempt-003/evidence/derived.stderr` was treated as an EDN record and refused. | No. | Deposit no stray file: reference every companion from exactly one validated record, or omit it. A code repair is warranted only if a valid referenced companion is still misclassified. |
| `repair-ea1-dd81768f87a081e5f36b45c302421bae20a283f07243dff7ce9fc53e9f099df2--attempt-001-build-failed` — `data/wm-repair-obligations/findings/repair-ea1-dd81768f87a081e5f36b45c302421bae20a283f07243dff7ce9fc53e9f099df2--attempt-001-build-failed.edn` | `:build-failed`, author wait | `codex-23` terminal `Exit 1: [No assistant message returned]`; zero tools/commands. | **Yes**: never executed. | No substantive repair evidenced; replay after dispatcher/session handling is available, retaining this historical failure. |
| `repair-ea1-dd81768f87a081e5f36b45c302421bae20a283f07243dff7ce9fc53e9f099df2--attempt-002-build-failed` — `data/wm-repair-obligations/findings/repair-ea1-dd81768f87a081e5f36b45c302421bae20a283f07243dff7ce9fc53e9f099df2--attempt-002-build-failed.edn` | `:build-failed`, author wait | Same `No assistant message returned`, zero tools/commands. | **Yes**: never executed. | Same: bounded replay, not invented implementation evidence. |
| `repair-ea1-51a783ff43ce1fb432b0928b2f142d3f07450872ba4b96c005f50d1b29ddcddc--attempt-001-build-failed` — `data/wm-repair-obligations/findings/repair-ea1-51a783ff43ce1fb432b0928b2f142d3f07450872ba4b96c005f50d1b29ddcddc--attempt-001-build-failed.edn` | `:build-failed`, author wait | Same `No assistant message returned`, zero tools/commands. | **Yes**: never executed. | Same: replay through the corrected transport/session boundary; no substantive code claim exists. |
| `repair-ea1-51a783ff43ce1fb432b0928b2f142d3f07450872ba4b96c005f50d1b29ddcddc--attempt-002-untyped-failure` — `data/wm-repair-obligations/findings/repair-ea1-51a783ff43ce1fb432b0928b2f142d3f07450872ba4b96c005f50d1b29ddcddc--attempt-002-untyped-failure.edn` | `:untyped-failure`, selection | Java NPE: `Cannot invoke clojure.lang.Named.getName() because x is null`; backtrace stops in selection. | No. | Identify the nil selection value and replace unsafe `name` coercion with a typed refusal or valid value at its producer; retain a regression using the exact nil-bearing shape. |
| `repair-initialization-8ceaae14-d8b5-4ceb-a8bf-ce9a4d4eed6a-tripwire-tripped` — `data/wm-repair-obligations/findings/repair-initialization-8ceaae14-d8b5-4ceb-a8bf-ce9a4d4eed6a-tripwire-tripped.edn` | `:tripwire-tripped`, initialization/opportunity | T8 `duplicate-finding livelock`. | No. | Establish why the same unresolved cause is repeatedly minted/selected, then enforce one durable finding per causal occurrence or an authorized retry/supersession rule without disabling T8. |
| `repair-initialization-82224aab-86f0-40eb-bede-b377b4216671-tripwire-tripped` — `data/wm-repair-obligations/findings/repair-initialization-82224aab-86f0-40eb-bede-b377b4216671-tripwire-tripped.edn` | `:tripwire-tripped`, initialization/opportunity | T8 `duplicate-finding livelock`. | No. | Same substantive T8/causal-deduplication requirement; this is a separate retained occurrence, not evidence of a separate root cause. |
| `repair-ea1-4e68d87bb2cfd67bd81841528e073bf8b73eb890903618a08da4de51c7a0e564--attempt-001-build-failed` — `data/wm-repair-obligations/findings/repair-ea1-4e68d87bb2cfd67bd81841528e073bf8b73eb890903618a08da4de51c7a0e564--attempt-001-build-failed.edn` | `:build-failed`, author wait | `codex-23` `No assistant message returned`; zero tools/commands. | **Yes**: never executed. | No substantive repair evidenced; replay only after corrected dispatch/session handling. |
| `repair-initialization-4e120541-560a-4c43-b0b4-dabb95a5e7fd-tripwire-tripped` — `data/wm-repair-obligations/findings/repair-initialization-4e120541-560a-4c43-b0b4-dabb95a5e7fd-tripwire-tripped.edn` | `:tripwire-tripped`, initialization/opportunity | T8 `duplicate-finding livelock`. | No. | Same T8 causal-deduplication/authorized retry requirement. |
| `repair-initialization-d31f5b22-a76d-4431-9c1f-6f0300d34a2f-tripwire-tripped` — `data/wm-repair-obligations/findings/repair-initialization-d31f5b22-a76d-4431-9c1f-6f0300d34a2f-tripwire-tripped.edn` | `:tripwire-tripped`, initialization/opportunity | T8 `duplicate-finding livelock`. | No. | Same T8 causal-deduplication/authorized retry requirement. |
| `repair-ea1-4e68d87bb2cfd67bd81841528e073bf8b73eb890903618a08da4de51c7a0e564--attempt-002-build-failed` — `data/wm-repair-obligations/findings/repair-ea1-4e68d87bb2cfd67bd81841528e073bf8b73eb890903618a08da4de51c7a0e564--attempt-002-build-failed.edn` | `:build-failed`, author retry wait | `codex-23` `No assistant message returned`; zero tools/commands. | **Yes**: never executed/resumed transport failure. | No substantive repair evidenced; replay through the corrected dispatcher/session boundary. |
| `repair-ea1-b82bec4361658ac9fea3bf2acf2b155ee690554056cb43982b320c2554a19bb7--attempt-001-build-failed` — `data/wm-repair-obligations/findings/repair-ea1-b82bec4361658ac9fea3bf2acf2b155ee690554056cb43982b320c2554a19bb7--attempt-001-build-failed.edn` | `:build-failed`, author retry wait | Explicit `input_too_large`: 1,690,401 characters exceeded 1,048,576; zero tools/commands. | **Yes**. This is the exact oversized resumed-session class reported fixed by futon3c `7829ea83`; this note does not independently certify deployment of that commit. | No underlying implementation ran. Re-run after the corrected dispatcher is actually loaded/used; then classify any real author result separately. |

Summary: **7/20** satisfy the strict dispatcher-artifact definition: one
never-executed 429 row, five never-executed `No assistant` rows, and one
never-executed explicit `input_too_large` row.  One additional 429 row is
operational/seat-capacity failure after
execution began.  The remaining twelve are retained fixture leakage,
validation/deposit failures, binding defects, close containment, selection
NPE, or T8 duplicate-finding incidents.  “Dispatcher artifact” is causal
classification only; it does not itself authorize deletion or resolution.

## Reconciliation of the six feature-card descendants

There is no discrepancy in the eligible-20 snapshot.  The target
`repair-ea1-504ad8630070adf67604aab739c0c0184b5ccf632f552ec6d2b55fac7ad71c87--attempt-001-feature-card-missing-or-invalid`
does not appear because it has an implementation/verification-side record and
`open-obligations` reports it as `:awaiting-validation`, not `:open`.  The six
**child findings whose `:target` equals that id** are all present in the table:

1. `repair-ea1-dd81768f…--attempt-001-build-failed`
2. `repair-ea1-dd81768f…--attempt-002-build-failed`
3. `repair-ea1-51a783ff…--attempt-001-build-failed`
4. `repair-ea1-4e68d87b…--attempt-001-build-failed`
5. `repair-ea1-4e68d87b…--attempt-002-build-failed`
6. `repair-ea1-b82bec43…--attempt-001-build-failed`

Their exact finding paths are given in their rows above.  None has a
resolution file at this cutoff, none is superseded, and each remains
`:repair/status :open`; hence all six are runner-eligible.  The apparent miss
came from looking for the **target id as a queue row** rather than looking at
the `:target` field of its six descendants.

Of those six, all show `:execution {:executed false :tool-events 0
:command-events 0}`.  Five retain the generic terminal `No assistant message
returned`; the sixth retains the precise `input_too_large` refusal.  Therefore
claude-4's count of six at approximately 17:10Z and the eligible-20 snapshot
are consistent.
