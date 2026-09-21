# fix-10: a grounded commit is not an observed wanted outcome

Discovery by codex-12, 2026-09-21. Code references below are relative to futon2 at base `980fc2bb`; historical prompt references explicitly use `7a9daa0f`. No production changes, live loads, clicks, HTTP writes, or store writes.

## Findings

The selected model predicted the shared-updater completion token, but the build delivered a narrower arithmetic consolidation. Post-build measurement **did notice that the completion declaration remained false**. The missing operation is to compare this measurement with the prediction and consume observations in token belief. Neither substrate grounding nor the separate seven-status entity-belief snapshot performs that operation.

Correction to the starting premise: `007-closed.edn` contains both `[:payload :close-retention]` and `[:payload :close-evidence-manifest]`. They are absent from the judgment, not from the checkpoint. The manifest has six entries, the preceding checkpoints 001–006. The retention block explicitly records `:state {:status :absent :reason :independent-observation-unavailable}` and `:model {:status :absent :reason :declared-model-identity-unthreaded}`. Its state is not populated from the D-task observations. Those observations live in a separate retained record, not among these six manifest entries.

## What the close actually does

Read these sources together:

- `src/futon2/aif/full_loop_runner.clj:2514` (`ground-commit!`), `:4549` (grounded outcome), `:3481` (delivery QA), `:3505` (entity state), `:3542` (retention inputs), `:3560` (D-task completion), `:3581` (cohort close).
- `src/futon2/aif/full_loop_cohort.clj:445`: closes the immutable checkpoint by validating retention inputs, building the retention block, verifying manifest agreement and evidence timestamps, and renaming the input markers to retained keys.
- `src/futon2/aif/close_retention.clj`: validates occurrence and seven-status state/model evidence. It is not a token posterior updater.
- `src/futon2/aif/d_predecessor_task_authority.clj:61,85,185,263`: re-observation, claim, verification, completion.
- `src/futon2/aif/delivery_qa.clj:37,77`: constructs a Field Desk note and requires successful API acceptance. This is delivery-note publication, not execution of a mission acceptance test.

Grounding writes an implementation entity and a discharge entity to Futon1b, then reads the implementation back. `:resolved?` compares the returned implementation commit with the requested commit; `:dial-moved?` means no implementation entity existed before and one exists afterward. After artifact/build gates and independent review, the conjunction yields `:grounded-change`. There is no conjunction over selected wanted tokens. The reference witness has both booleans true, implementation `full-loop/implementation/7a9daa0f2ef9850aff01f1631dbc93c59625b586`, and the historical discharge `full-loop/discharge/attempt-001`. Fix-12 separately changes discharge identity.

Delivery QA derives its witness label from the implementation ID and its progress prose from the feature card. An accepted note cannot establish updater completion. `close!` computes the entity snapshot and retention inputs, calls `d-task/complete!`, includes that result in the run result, and closes the cohort. The D-task result does not modify the already assembled close judgment, its outcome, or its state/model retention inputs. The retained D-task verification for this run is `:admitted`, with scope `:executed-with-artifacts`; it explicitly does not establish portfolio membership or machine-enactment correspondence.

### Where the missing belief row was supposed to come from

At selection, `full_loop_runner.clj:3843` captures `(:belief judgement)` in `selected-entity-belief`. At close, `entity-state-at-close` (`:2582`) looks up the selected entity and requires a map of seven numeric status masses. It preserves that **selection-time** judgment; it never estimates a post-build row.

The historical `selected-target` preferred action `:id` over `:target`, choosing `:C1`. Fix-1 corrects that identity error, but does not fix the belief-domain mismatch. Trace form 3 has **417** `:mu-post` entity rows; neither `:C1` nor `"M-aif-policy-conditioned-eig"` is a key. Representative keys are stack annotations such as `"arxana/stack/futon-v1/devmap/futon2/P9"`. Thus even the corrected mission ID cannot supply this seven-status row. Do not substitute a token probability, a categorical success flag, or a nearby historical row.

The serving report initializes that entity domain from stack annotations plus sorry IDs, optionally reconciles previous `:mu-post`, applies morning-brief events, and runs its entity updates (`scripts/futon2/report/war_machine.clj:6475–6669`); it exposes the result as `:belief` (`:6880`), serialized as trace `:mu-post`. This is distinct from the target-qualified Boolean-token belief used by cascade selection.

### Actual post-build observations

Record: `/home/joe/code/futon3c/data/wm-d-task-enactment/action-a5d2326d-a73a-447c-8bc7-7f77c8bbc187.edn`, SHA256 recorded by the run: `b4e759a9cdb1be48ffad5b82fcb3c7fb13e3608c3c332ee92478a9831f5e7895`.

`artifact-tokens` traverses the captured source declarations and the joint token universe, not just the selected target. For same-repository C3/C4 locators it replaces the revision with the verified artifact commit. Different repositories and unsupported check classes receive typed missing measurements. This run has **11 measurements: 6 true, 5 false, zero missing**. No C5/C6 checks occur.

| Target | Token | Check | At `7a9daa0f` |
|---|---|---|---|
| M-aif-policy-conditioned-eig | `:admission/task-stated` | C4 | true |
| same | `:hole/h6378c65a4012` shared updater | C4 | **false** |
| same | `:hole/h0e270aa090bc` typed Q boundary | C4 | false |
| same | `:hole/h42fceb4ad48b` calibration packet | C4 | false |
| M-f11-find-production-successor | `:admission/task-stated` | C4 | true |
| same | `:hole/h9ab212b3281d` ordinary acceptance | C4 | false |
| same | `:hole/h2045faa0e7cc` successor link | C4 | false |
| M-wm-08-external-f2 | `:external-expectation-validator-exists` | C4 | true |
| same | `:frozen-occurrence-context-captured` | C3 | true |
| same | `:independent-expectations-written` | C3 | true |
| same | `:route-a-rehearsal-reported` | C3 | true |

Important loss of information: `verify!` replays these measurements but exports only true affirmations into `:present`. It exports `:absent #{}` and makes the other five tokens `:unknown`. The raw false results survive in `:after-token-evidence`; the admitted authority does not assert negative observations. It requires some positive evidence, not the selected produce. Its `:before-evidence :not-measured` and `:causal-attribution :independent-check-required` also prohibit calling these changes caused by this action. In particular, the task-stated locator was originally pinned to the admission revision; its artifact-revision recheck is a different temporal question and should not overwrite that original historical fact without a declared observation schedule.

Fresh read-only replay with the actual `observation-checks/check-path-exists` and `check-decl-in-file` in a separate JVM reproduced all eleven results at the artifact commit. Replaying with HEAD resolved all eleven to `9635abbf98d5b889473be11e026dcd25464b1054`, again 6 true / 5 false. The updater checkbox remains unchecked. These checks only run Git reads; no store or network calls are involved.

## Prediction versus observation, and the next tick

The selected single pattern is `:apparatus/one-authority-per-question`. Its interpreted guard needs the task-stated token and forbids updater-complete; its add-only transition produces updater-complete. Starting from the recorded true task-stated and false completion facts, the first-enabled model step predicts completion. A fresh JVM replay using the retained occurrence action, `model/observed-belief` on the task-stated singleton and `model/rollout` for one step returned `{#{["M-aif-policy-conditioned-eig" :admission/task-stated] ["M-aif-policy-conditioned-eig" :hole/h6378c65a4012]} 1}` (exit 0). The retained pattern uses the documented default theta of 1. This is a model prediction, not evidence from the implementation commit. The other two mission wants are not produced by this order. A comparison must include all three wanted tokens: updater predicted true / observed false; typed-Q predicted false / observed false; calibration predicted false / observed false. Also record separately the chosen pattern's intended output subset so “all wants” cannot silently shrink to that subset.

The smallest honest close change is a **new token comparison receipt**, not filling the absent seven-status row. Freeze selected action, selection-time token belief, full target want set, model/source identity, declared horizon, token meanings, and observation schedule before dispatch. Use the same model rollout to retain predicted marginal probabilities per wanted token (not merely the union of `:produces`, which ignores disabled guards). After artifact binding, evaluate each scheduled locator at its declared temporal scope, retain true/false/typed missing, artifact SHA and evidence digest, then compare. Unknown is never false. Grounding and wanted-outcome status must remain separate fields. Record a typed mismatch for this run; do not rewrite its historical grounded result.

For continuation, the existing insertion point is `token-belief-predecessor/input-receipt`, then `joint-q0 (:continuation-belief token-belief-input)` in `scripts/futon2/report/war_machine.clj:6157–6168`. `token-belief-carry/stage` retains domain inputs and a prospective carry but says `:conditioning-status :not-wired`. `token-belief-predecessor.clj:57–85` can verify D-task carry, yet even admitted carry returns `:conditioning-consumption-not-wired`, empty observation updates, and fresh fact initialization. For the reference run the retained input says `:not-run`, `:carry-admission-refused`, and `:fresh-fact-initialization-after-carry-refusal`. No claim is made here that a later tick consumed this run.

Wire a versioned, independently checked observation input at that boundary only after declaring its update law. A deterministic checkable token observation can set its observed Boolean marginal; missing observations need an explicit prior/fresh-initialization policy. Preserve domain/meaning/revision checks and reject incompatible carry. A later fresh observation supersedes an older artifact observation only under an explicit temporal ordering rule. Do not turn simulated produced tokens into facts, impose a seven-status mapping, or weaken the existing receipt validators: both `valid-stage?` and predecessor validation currently attest the no-conditioning behavior and need deliberate schema evolution with replay tests. The entity-belief carry via previous `:mu-post` is a separate mechanism, not a substitute.

## What the author was asked to do

Historical source at `7a9daa0f`, `full_loop_runner.clj` functions `prompt-selected-action`, `prompt-construction`, and `author-prompt`, projected away precedence, produces, observation locators, and interpretation receipts. The selected target was the erroneous `:C1`. The prompt asked for “one bounded, substantive advancement,” included mission context, and a feature card. It did not explicitly supply the selected updater token/locator and its scoped reading. The retained dispatch gives job ID `invoke-1789964800905-22897-b86bd017` and an Agency prompt reference, not inline prompt bytes. This conclusion is based on the historical rendering code, not a claim to have recovered the exact job prompt. Fix-2 now renders the cascade/produces; it cannot retroactively inform that author.

The author reported “single EIG authority” and “coherent parameter delivery.” `git show 7a9daa0f --stat` shows only `parameter_delivery.clj` and its test changed (55 insertions, 29 deletions). The review approved canonical KL/EIG delegation, typed refusal preservation, and unchanged G; it reported 15 tests / 42 assertions. That is meaningful narrower work, not the mission's demanded shared A4a/BMR posterior updater. Mission lines 195–206 require hypothetical and observed evidence through that updater, equal posterior state **and provenance** on a pinned fixture, and fail-closed unknown outcomes. Arithmetic delegation alone cannot justify checking that box.

The unchecked box is therefore appropriate, not an author omission to repair mechanically. C4 can witness only the completion **claim**. Actual completion needs the updater parity/provenance/unknown-outcome tests and independently reviewed evidence pinned to the artifact commit. The hand-admitted interpretation itself says merely ticking the checkbox is not the admitted action. Keep declaration observation and acceptance evidence separate; without a declared verifier for the latter, retain “unestablished,” not “completed.”

## Proposed slices (not implemented)

1. **Retain one comparison receipt at close** (~150–250 production lines plus tests). Add a pure token-outcome comparison namespace and thread its output into the runner close before manifest freeze. Inputs are frozen selection prediction and explicit post-build measurements; retain all wants, evidence references and typed missing results. No change to G, outcome label, belief, or habit. Include the comparison and measurement bytes in the manifest, with timestamps before close. First test: in temporary Git repositories, freeze the real updater declaration and interpreted singleton, run the actual model, create a reviewed-looking unrelated code commit leaving the checkbox unchecked, and use actual C4. Assert updater predicted true / observed false / mismatch, other wants retained, and a grounded substrate witness cannot turn that into success. Include an unavailable revision case asserting missing rather than false. This tests the missing behavior without inventing a successful updater.
2. **Expose signed token observations under a narrow authority** (~100–180 lines plus tests). Version the D-task observation projection: retain C3/C4 false separately from unknown, bind schedules/token meanings/artifact identity, keep historical task-stated checks distinct, preserve all execution-verification requirements and no causal claim. Acceptance: missing file/decl and missing revision are distinguishable according to existing check semantics; wrong domain/identity refuses. Do not silently change the old positive-only authority.
3. **Consume admitted observations in the next token initialization** (~180–300 lines plus tests). Declare the update/temporal policy, version stage/input receipts and validators, connect through the existing predecessor boundary, retain provenance of every update. Two-tick hermetic test: the unproduced updater is still false at tick two despite tick one's predicted true; unknown receives no fabricated update; stale, wrong-domain, and wrong-occurrence observations refuse. Test actual selection input, not just a receipt helper. Leave seven-status entity belief untouched.
4. **Add updater-specific acceptance evidence** (size depends on the updater work, separate assignment). A pinned evaluator for hypothetical/observed posterior and provenance equality plus unknown-outcome refusal. Acceptance: a checked box without this evidence cannot be called verified completion; arithmetic delegation fails the completion criterion. Prompt rendering can reference this contract once it exists. This is not a reason to delay the truthful false measurement in slice 1.

## Validation and reproduction

Environment: own worktree `/home/joe/code/futon2-fix-10`, branch `fix/narrative-10`, base `980fc2bb`; installed Clojure CLI, separate JVM. No production/test namespace edited, so no test-registry warrant or failing-on-main test is claimed. This discovery's evidence is record parsing, historical code/diff inspection and fresh read-only observation replay. Final `git diff --check` is the documentation gate.

The replay command, run from the worktree (adding `shutdown-agents` avoids the shell-future pool's idle exit delay):

```sh
clojure -M -e '
(require (quote [clojure.edn :as e])
         (quote [futon2.aif.observation-checks :as o]))
(let [r (e/read-string (slurp "/home/joe/code/futon3c/data/wm-d-task-enactment/action-a5d2326d-a73a-447c-8bc7-7f77c8bbc187.edn"))]
  (doseq [reference ["7a9daa0f2ef9850aff01f1631dbc93c59625b586" "HEAD"]]
    (let [xs (mapv (fn [x]
                     (let [l (assoc (:after-locator x) :sha reference)
                           v ((if (= :C3 (:class l))
                                o/check-path-exists o/check-decl-in-file) l)]
                       [(:token x) (:observed v)
                        (get-in v [:evidence :resolved-sha])]))
                   (:after-token-evidence r))]
      (prn {:reference reference :observations xs
            :counts (frequencies (map second xs))}))))
(shutdown-agents)'
```

Fresh output summaries: artifact `{true 6, false 5}`; HEAD at the SHA above `{true 6, false 5}`. Per-token output matches the table. Shared HEAD may advance; the artifact replay is immutable. Parse all three EDN trace forms with `clojure.edn/read` on a PushbackReader; `read-string` alone would silently inspect the earlier run.

Model replay command (fresh output quoted above):

```sh
clojure -M -e '
(require (quote [clojure.edn :as e])
         (quote [futon2.aif.cascade-model-manifest :as m]))
(let [r (e/read-string (slurp "/home/joe/code/futon3c/data/wm-d-task-enactment/action-a5d2326d-a73a-447c-8bc7-7f77c8bbc187.edn"))
      a (get-in r [:dispatch :occurrence :action/value])
      q (m/observed-belief #{["M-aif-policy-conditioned-eig" :admission/task-stated]})]
  (prn :one-step (m/rollout (constantly (:precedence a)) q 1)))
(shutdown-agents)'
```
