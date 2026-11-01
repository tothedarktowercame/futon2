# Why 1586 had no readiness job ledger and 8c8 did

Discovery only, 2026-10-28, codex-1 for claude-11. No reload, dispatch, test, click, source change, or live-store write. Source citations below use futon2 `66a1e3ef49fc758d4fb99d483475f0f6279a397e` and futon3c `f3d7133da46af757d89a279fdce36c84d49dbc40`, except explicitly historical comparisons.

## Finding

The difference is established by retained loaded-source hashes and the corresponding git source: **37db58d96641a006fa8a47bcc8498c5af2a094c0, “Bind ticket readiness dispatch to live runner context,” committed 2026-10-27 14:23:42Z**. It is not c560's `6f1f8543c` repair.

Before 37db58d96, `production-defaults` constructed the readiness contact immediately, closing over opts **before** `full-loop-runner/run-opportunity!` allocated `:registered-run/job-ledger`. Later adding the ledger to the runner's opts did not change that captured immutable map. The readiness phase called that prebuilt contact, so `dispatch!` received the old map. This is not evidence that the runner failed to allocate its ledger or that a subsequent `dissoc` removed it.

37db58d96 replaces the prebuilt readiness map with a factory accepting live runner opts, merging `(merge opts runner-opts)`, and calls that factory inside the readiness phase. The second run retained that exact runtime source, loaded at 14:23:50.496044196Z. Its readiness dispatch is also retained in its run-local job ledger. c560's 16:37:07Z patch only adds the analogous guard in `strategic-selection!`; it does not supply this context.

## Record comparison and exact source match

Records under `/home/joe/code/futon2/data/wm-runs/`:

* `tick-run-record-2026-10-27-1586bf55-9ad7-4d72-aa70-827be7506212.edn`, SHA256 `8a19a4d80344f89a0d4ac35fffc235cd63f6e83866813970fd9abd608a63a2e4`.
* `tick-run-record-2026-10-27-8c8ee35f-54da-41f9-8e51-1d51e523a98f.edn`, SHA256 `45877a2a92e7366a9bb32bc38d8f4862d623a011a6fc7a853b28ffd90ca4427f`.

First record:

```edn
:startedAt "2026-10-27T14:16:34.933095917Z"
:click/id "wm-retry-repair/2f6826382458c5b36338189a8623f41abb120f92-2-patch-02e9d5e7e11e92479d586ad078ce960902c36fa1"
;; :failure
{:kind :wm-job-ledger-unavailable :stage :ticket-readiness-refresh
 :error "WM dispatch lacks its run-local job ledger context"}
;; :registered-run/model-usage
{:status :typed-missing :source :wm-run-local-agency-ledger
 :unit :tokens :jobs [] :reason :no-wm-jobs-dispatched}
```

Second record:

```edn
:startedAt "2026-10-27T16:38:29.753465278Z"
:click/id "wm-retry-repair/ad68c5681da28c85d5635d1a1446cf437c423833-1-patch-6f1f8543cc4223e782bb8acdab3713eaef7daaef"
;; :failure (projection)
{:kind :invalid-author-refusal :stage :author-wait
 :error "Author refusal lacks a reason or claims an artifact"}
;; first :registered-run/model-usage :jobs entry (projection)
{:job-id "invoke-1793119192416-5620-354be23f"
 :phase :ticket-readiness-refresh :status :typed-missing
 :reason :provider-usage-missing-or-invalid}
```

Both records' `[:runner/source :namespaces futon2.aif.full-loop-runtime]` say `:status :current` and name the canonical source path. Their `:loaded-source` values are:

| Run | captured-at | sha256 |
|---|---|---|
| 1586 | 2026-10-27T13:54:22.026208570Z | 02004511cb3953f719d69f518521336cf3642b5dd99538579ef80a66713aaa47 |
| 8c8 | 2026-10-27T14:23:50.496044196Z | 87a3086acf0ea272fc2905f683ab71f8fd179eba92fb2663a1f4e5f9be1933ca |

Read-only commands (exit 0) establish those are exactly the before/after source:

```sh
git -C futon2 show 37db58d96^:src/futon2/aif/full_loop_runtime.clj | sha256sum
git -C futon2 show 37db58d96:src/futon2/aif/full_loop_runtime.clj | sha256sum
# respectively the two hashes above
git -C futon2 show 37db58d96^:src/futon2/aif/full_loop_runner.clj | sha256sum
# 64f6b73a8a0707510527b1ed6d9b4e28fd7b8b256320e39c4402997a853de871
git -C futon2 show 6f1f8543c:src/futon2/aif/full_loop_runner.clj | sha256sum
# 21e86bea4f7dad6c9be3ab76917d8886f47167f56022396eedc5b3d756284182
git -C futon2 show 37db58d96 -- src/futon2/aif/full_loop_runtime.clj src/futon2/aif/full_loop_runner.clj
```

The latter runner hashes also exactly match the records' loaded runner hashes, captured respectively at `2026-10-27T14:16:06.775438399Z` and `2026-10-27T16:37:58.852270433Z`. Thus the second execution included the new readiness phase factory call as well as the new runtime factory. No assumption that a commit alone implies a reload is needed.

The service acceptance files under `/home/joe/code/futon3c/data/wm-click-acceptances/`, named `click-` + these click IDs with `/` replaced by `_` + `.edn`, both say `:status :completed`. First: `:started-at "2026-10-27T14:16:28.387324428Z"`, `:completed-at "2026-10-27T14:18:19.842203125Z"`. Second: `:started-at "2026-10-27T16:38:23.267389362Z"`, `:completed-at "2026-10-27T17:27:57.648500038Z"`. These are separate retry children, not one debugger continuation. Those acceptance records do not preserve the full opts, original shell invocation, or issuer; I do not infer which operator command launched their parents. Run-local readiness job's recorded caller is `"wm-full-loop"`.

Records were read in a separate `bb` process with `clojure.edn/read-string`, projecting `:click/id`, `:startedAt`, `:failure`, the two namespace entries under `:runner/source`, and `:registered-run/model-usage`. No serving JVM evaluation was used.

## Options path (current source; historical difference stated explicitly)

1. The optional parent launcher `scripts/wm_click_repair_loop.py:443–451` invokes `scripts/wm_click.sh --run` with issuer/cast arguments. `wm_click.sh:456–467` builds the payload and POSTs `/api/alpha/wm/click`. This is a possible parent entry, not a claim about the retained shell invocation.
2. futon3c `transport/http.clj:10064–10170`, `handle-wm-click-start`, constructs `legacy-opts`, merges prepared options and issuer provenance, and selects `runner-service/click!`. Neither shell payload nor HTTP options creates the run-local job ledger.
3. Actual retry-child entry: futon3c `wm/runner_service.clj:1364–1371`, `close-click!`, dequeues the retry after parent closure and calls `click!`. `retry-child-opts:1315–1329` preserves parent options, removes `:ordinary-click/issue!`, `:idempotency-key`, `:run-id`, and sets retry click identity and `:meta-repair-retry`. It does not allocate or remove a job ledger.
4. `click!:1470–1489` admits identities, applies configured runner opts and medic authority/preflight; the worker at `:1559` calls `run-click!`. `configured-runner-opts:675–688` calls `machinery-execution-cohort/apply-binding` (`machinery_execution_cohort.clj:55–72`, assoc cohort unless present), `configure-self-heal-ports` (`runner_service.clj:201–244`, assoc paths/queue ports and merge identities/ports), and runner `config`. `config` (`full_loop_runner.clj:331–373`) merges defaults then opts; arbitrary existing opts survive.
5. Service `run-click!:1404–1427` reacquires authority/config, removes only service/callback keys, assocs `:click-id`, phase logger and strategic selector, then calls `full-loop-runtime/run-opportunity!`.
6. Runtime `run-opportunity!:717–725` resolves authority; `run-opportunity-with-authority!:634–715` removes a caller verifier, adds validated verifier/store paths and conditional diagnosis/acceptance bindings. On retry, `:707–710` calls `run-ordinary-opportunity! effective-opts`; it does not call a special readiness resume path.
7. `run-ordinary-opportunity!:581–632` mints run identity, adds feedback/graph/learning paths and selection timing, calls `production-defaults` at `:607`, then assocs its readiness port and other ports into runner opts and calls runner `run-opportunity!`. **Historical pre-fix `production-defaults:493–495` called `(ticket-readiness-refresh-config opts)` here**, before step 8. Current `:493–496` returns the factory instead.
8. Runner `run-opportunity!:9123–9177` creates `:registered-run/job-ledger (atom {:order [] :jobs {}})` at `:9152` and passes the augmented map to `run-opportunity-core!`. Core `:6099–6110` reapplies `config` and assocs phase state, preserving the ledger. Nothing here retroactively updates functions previously closed over a different opts map.
9. Phase `:7315–7318` calls `ticket-readiness-refresh-step` (`:1991–1996`). Current factory is invoked with these live opts, and runtime `:496` merges live values over early values. Before 37db58d96 the phase directly called `refresh-one!` on the already-built config; see commit diff (old phase lines7276 onward).
10. Runtime `ticket-readiness-refresh-config:187–217` constructs `contact` at `:200` using `designated-agent-answer-fn`. That function `:151–173` captures opts in `flight-runner/agency-answer-fn`, adding only agency-base/invoke-mode. Config `:207–209` calls that contact from its answer port. `ticket_readiness/refresh-one!:360–379` invokes this answer port for the chosen ticket.
11. `flight_runner/agency-answer-fn:138–158` assocs invoke-mode on its captured opts and calls runner `dispatch!`. `full_loop_runner/dispatch!:1998–2015` checks click ID and ledger before POST; `:2026` records dispatched jobs in that ledger. The failing record retains a nonblank click ID, so its guard failure was the nil ledger arm, not missing click identity.

## Limits and consequence

The source versions and retained dispatch establish the context-capture explanation; there is no need to posit random opts loss or a different caller. The records do not retain a complete opts snapshot or the exact reload command, so I cannot report the shell operator or which reload tool installed the fixed source. They do retain canonical loaded paths, capture times and matching hashes.

This establishes why readiness could dispatch in 8c8, **not** that readiness produced a valid assessment, that the whole retry succeeded, or that c560's own guard-only patch fixed the cause. The later record still fails at author-wait. No change to retry-success or deferral semantics is made or implied here.
