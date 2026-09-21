# Run 2026-09-21-1790033693

A narrative of retained evidence; no selection, observation, or actuation was rerun.

## time-step

The run began at 2026-09-21T23:34:59.806625249Z with trigger :duree-click-on-demand. Its opportunity is duree-click-on-demand/2026-09-21T23:35:01.068178094Z/32fa2171-1061-4d92-b651-23a96f19ffc8, and the recorded semantic epoch is :full-loop-real-actuation-v6.

The machine perceived in mode :stop-the-line, with stop-the-line active. Its recorded free-energy account is controller-score 0.5475346253482014, preference-gap-score 0.33134761072261076, coverage-uncertainty-pressure 0.9490247953671556; active avoided channels are :stack-pct, :consulting-pct. It received 14 observation channels; the largest recorded preference gaps give :attack-coverage = 0.0 (gap 0.8), :support-coverage = 0.0 (gap 0.8), :stack-pct = 0.9791666666666666 (gap 0.7291666666666666), :loop-health = 0.12743801158211138 (gap 0.6725619884178886). 417 of 417 belief rows changed from mu-pre to mu-post (including added or removed rows). The recorded route is :R20 → :R12 → :R2 → :R7 → :R3 → :R8 → :TRACE.

- Source: `./data/wm-trace/wm-trace-2026-09-21.edn` — `[:form 4 :mode]`.
- Source: `./data/wm-trace/wm-trace-2026-09-21.edn` — `[:form 4 :free-energy]`.
- Source: `./data/wm-trace/wm-trace-2026-09-21.edn` — `[:form 4 :observation]`.
- Source: `./data/wm-trace/wm-trace-2026-09-21.edn` — `[:form 4 :mu-pre]`.
- Source: `./data/wm-trace/wm-trace-2026-09-21.edn` — `[:form 4 :mu-post]`.
- Source: `./data/wm-trace/wm-trace-2026-09-21.edn` — `[:form 4 :wm/route]`.

[Retained scan account](</home/joe/code/futon2/data/wm-runs/tick-run-record-2026-09-21-1790033693.scan.md>), SHA-256 `d916d8d04b4b6bcc74f5fbc1861e7f9946089bccec7caa79d3bd5af0efc79fe7`. This is the saved perceive-stage account, not a fresh scan.
- Source: `./data/wm-runs/tick-run-record-2026-09-21-1790033693.edn` — `[:scan-report]`.

Cited facts:
- Source: `./data/wm-full-loop-machinery-69/wm-contract-machinery-69-v1/attempt-002/001-time-step.edn` — `[:payload :judgment]`.
- Source: `./data/wm-runs/tick-run-record-2026-09-21-1790033693.edn` — `[:startedAt]`.

Recorded phase durations: agent-readiness 3793 ms; code-state 4332 ms; substrate-preflight 5210 ms.
- Source: `./data/wm-full-loop-phases.edn.log` — `[:form 11389 :duration-ms]`.
- Source: `./data/wm-full-loop-phases.edn.log` — `[:form 11391 :duration-ms]`.
- Source: `./data/wm-full-loop-phases.edn.log` — `[:form 11393 :duration-ms]`.

## selection

The machine scored 2 admitted cascades; enumeration completeness is :incomplete. 282 targets were considered; 280 were refused (17 :no-admitted-interpretation missing :interpretations, 1 :no-constructed-candidate missing [:new-wanted-token-within-horizon], 1 :universe-not-admitted missing :locators, 261 :universe-not-admitted missing :universes); proposal supply retained 89 proposals and 0 admissions. It chose M-aif-policy-conditioned-eig (cascade :C1); the checkpoint's selected-mission field is M-aif-policy-conditioned-eig. The computed full G spread is 0.001264875853721037 nats. The policy comparison records decided-by :habit, near-tie :threshold-undeclared, threshold {:status :undeclared}; The action comparison records decided-by :robust.
C reached 3 of 467 source tokens, with 5 projected outcome tokens; mission-hole census retained 585 holes and projected 116.

C preference audit (source budget: :global-deduplicated-live-source-inventory, 467 source entries): C prefers ["M-aif-policy-conditioned-eig" :hole/h6378c65a4012] present over absent by 1.001309 : 1; ["M-aif-policy-conditioned-eig" :hole/h0e270aa090bc] present over absent by 1.001309 : 1; ["M-aif-policy-conditioned-eig" :hole/h42fceb4ad48b] present over absent by 1.001309 : 1.

Discovered focus: WM; candidate classes: M-aif-policy-conditioned-eig/:C1 = focus; M-f11-find-production-successor/:C1 = focus (record-only).
Expected parameter information gain (record-only, not in G): M-aif-policy-conditioned-eig/:C1: unavailable (:route-unavailable) under :illustrative; M-f11-find-production-successor/:C1: unavailable (:route-unavailable) under :illustrative.

| Target | Cascade | G (nats) | Posterior | Habit | F consumed |
|---|---|---:|---:|---:|---:|
| M-aif-policy-conditioned-eig | :C1 | 9.704757850639144 | 0.6669476909087854 | 0.6666666666666666 | not recorded |
| M-f11-find-production-successor | :C1 | 9.706022726492865 | 0.33305230909121525 | 0.3333333333333333 | not recorded |

![Selection: relative G and posterior](<narrative.selection.svg>)

G and posterior are read separately from the certificate and selection law; legacy checkpoint G-efe is not treated as G.

Cited facts:
- Source: `./data/wm-full-loop-machinery-69/wm-contract-machinery-69-v1/attempt-002/002-selection.edn` — `[:payload :judgment]`.
- Source: `./data/wm-full-loop-machinery-69/wm-contract-machinery-69-v1/attempt-002/002-selection.edn` — `[:payload :judgment :controller-decision :selection-certificate :candidates]`.
- Source: `./data/wm-full-loop-machinery-69/wm-contract-machinery-69-v1/attempt-002/002-selection.edn` — `[:payload :judgment :controller-decision :selection-law]`.
- Source: `./data/wm-trace/wm-trace-2026-09-21.edn` — `[:form 4 :cascade-problems]`.
- Source: `./data/wm-runs/tick-run-record-2026-09-21-1790033693.edn` — `[:live-c-coverage]`.
- Source: `./data/wm-runs/tick-run-record-2026-09-21-1790033693.edn` — `[:mission-hole-coverage]`.

Docstring correspondence, not runtime Lean execution: `src/futon2/aif/cascade_selection.clj` namespace docstring cites `PolicySelection.lean` at `a434947c63`, `selectionPosterior`.

Recorded phase durations: selection 47390 ms.
- Source: `./data/wm-full-loop-phases.edn.log` — `[:form 11399 :duration-ms]`.

## construction

The cascade contains 1 pattern(s), in precedence order: :apparatus/one-authority-per-question. Its recorded construction method is :hand-admitted; retrieval method: not recorded for this selected cascade. Its observed structure is a singleton, with no wires. shape: singleton (basis: declared need-support; authority structure not recorded).


![Cascade: precedence and wanted-token outcomes](<narrative.cascade.svg>)
The following build plan is quoted from the retained author prompt.

> Recorded cascade plan (limit 24000 chars).
> pattern 1: :apparatus/one-authority-per-question
>   flexiarg: futon3/library/apparatus/one-authority-per-question.flexiarg
>   sha256: 42371c5db7fa2cfbd82688b63aac9ab85518cc0baad7eeb69ccca94aca9bcda1
>   reading: For the shared-updater task, make one A4a/BMR updater the authority for both hypothetical and observed posterior changes, route both callers through it, and demonstrate equal posterior state and provenance on a pinned observation plus refusal of an unknown outcome before recording this task complete.
>   scope limit: This reading covers the shared-updater task only; it neither supplies the generative experiment model nor completes the typed-Q or calibration tasks.
>   observation limit: C4 observes the mission's checked completion declaration, not the truth of its supporting evidence. Completion requires the mission's acceptance work; merely ticking the checkbox is not the action admitted here.
>   guards:
>   - M-aif-policy-conditioned-eig / :admission/task-stated expected true: established (recorded status: :established); check :C4; observed true; resolved sha cb2045b8279853fbd48d6a07c1e2da952f2e338f; looked for line - [ ] **Mint the shared posterior updater, then unify the two paths through; evidence :file-present = true; :path = holes/missions/M-aif-policy-conditioned-eig.md; :repo = futon2; :resolved-sha = cb2045b8279853fbd48d6a07c1e2da952f2e338f; :sha = cb2045b8279853fbd48d6a07c1e2da952f2e338f
>   - M-aif-policy-conditioned-eig / :hole/h6378c65a4012 expected false: established (recorded status: :established); check :C4; observed false; resolved sha abde70b9c3c4caa72d0a48b93889c7fe696705b4; looked for line - [x] **Mint the shared posterior updater, then unify the two paths through; evidence :file-present = true; :path = holes/missions/M-aif-policy-conditioned-eig.md; :repo = futon2; :resolved-sha = abde70b9c3c4caa72d0a48b93889c7fe696705b4; :sha = HEAD
>   produces: M-aif-policy-conditioned-eig / :hole/h6378c65a4012
> 
> holes: none
> wires: none

- Source: `/home/joe/code/futon2/data/wm-full-loop-machinery-69/wm-contract-machinery-69-v1/attempt-002/retained/job-127dd684d17b53ef014aab9a240b981c9f276e6d7f410b1784bfddabd82e4d63-prompt-33a2e6109671c0344cea1a38dd9f46f60e58a744b2b7e144ae0909ee0c51282e.txt` — `[:text "PATTERN CASCADE"]`.

Cited facts:
- Source: `./data/wm-full-loop-machinery-69/wm-contract-machinery-69-v1/attempt-002/003-construction.edn` — `[:payload :judgment]`.

Recorded phase durations: construction 10417 ms.
- Source: `./data/wm-full-loop-phases.edn.log` — `[:form 11401 :duration-ms]`.

## dispatch

The runner dispatched agent wm-author as job invoke-1790033860838-23088-716e9791. Its prompt reference is agency-job:invoke-1790033860838-23088-716e9791, with availability recorded as :invoke-ready.

Cited facts:
- Source: `./data/wm-full-loop-machinery-69/wm-contract-machinery-69-v1/attempt-002/004-dispatch.edn` — `[:payload :judgment]`.

Recorded phase durations: author-dispatch 7618 ms.
- Source: `./data/wm-full-loop-phases.edn.log` — `[:form 11403 :duration-ms]`.

## build

The author produced commit(s) ["aeb352f87368fb3280b3fb92ddd8e7f7c5fccc08"]. The recorded changed artifacts are ["holes/missions/M-aif-policy-conditioned-eig.md" "src/futon2/aif/a4a.clj" "test/futon2/aif/a4a_test.clj"]. Author execution evidence records 24 command events; reviewer execution records 18.

Final review statement:

> FULL_LOOP_REVIEW: APPROVE
> FULL_LOOP_REVIEWER_NOTE: Substantive shared-updater implementation; scoped C5 evidence is complete.
> 
> Commit `aeb352f87368fb3280b3fb92ddd8e7f7c5fccc08`:
> 
> - Introduces one authoritative A4a Dirichlet updater.
> - Routes hypothetical and corpus-observed updates through it.
> - Preserves provenance and typed unknown-outcome refusal.
> - Adds focused equivalence and corpus-path tests.
> - Does not claim the separate generative-model, typed-Q, or calibration work.
> 
> Executed gates:
> 
> - `clj-kondo --lint src/futon2/aif/a4a.clj` — PASS, 0 errors/warnings.
> - `clj-kondo --lint test/futon2/aif/a4a_test.clj` — PASS, 0 errors/warnings.
> - `emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- --no-defaults src/futon2/aif/a4a.clj` — PASS.
> - Same command for `test/futon2/aif/a4a_test.clj` — PASS.
> - `clojure -M:test -n futon2.aif.a4a-test` — FAIL: this repository’s alias interpreted `-n` as a filename.
> - Fresh-JVM equivalent `clojure -X:test :nses '[futon2.aif.a4a-test]'` — PASS: 19 tests, 69 assertions, 0 failures/errors.
> 
> Deposited and EDN-validated `wm-reviewer-standing.edn` with decision `:approve`.

Full review text (including any earlier discussion) lives in:

- Source: `./data/wm-full-loop-machinery-69/wm-contract-machinery-69-v1/attempt-002/005-build.edn` — `[:payload :judgment :validation :review-text]`.
- Source: `/home/joe/code/futon2/data/wm-full-loop-machinery-69/wm-contract-machinery-69-v1/attempt-002/retained/job-bdc6fec27de0681292a274bb56b4274cab3abccbfa102934f435ff6ecad2bfc7-reply-31211924f888adcc69df9db48fde3c6a158b7f807f140aea7dd6d40e44471878.txt` — `[:text]`.

Retained reply for :author job invoke-1790033860838-23088-716e9791:

> FULL_LOOP_FEATURE_CARD: {:built "shared A4a posterior updater" :want-coverage "C5" :matches-intent? true :things-to-try ["clojure -M:test -n futon2.aif.a4a-test -> 19 tests pass"]}
> 
> Enacted pattern: `:apparatus/one-authority-per-question`.
> 
> Implemented one authoritative A4a Dirichlet updater shared by hypothetical and observed paths. Added versioned posterior/observation schemas, provenance receipts, typed refusal for unknown outcomes, and routed the existing corpus path through the updater. Marked the bounded mission task complete.
> 
> Validations:
> 
> - A4a/BMR/R17: 28 tests, 104 assertions, 0 failures/errors
> - Scoped registry: 19 tests, 69 assertions, warrant `test-registry-95ca257a5871eecd6eb3d9262af4795ce5163aca2939d364d83af696c430a754`
> - clj-kondo: 0 errors, 0 warnings
> - check-parens: OK
> - `git diff --check`: clean
> - Worktree clean
> 
> FULL_LOOP_AUTHOR: DONE aeb352f87368fb3280b3fb92ddd8e7f7c5fccc08
- Source: `/home/joe/code/futon2/data/wm-full-loop-machinery-69/wm-contract-machinery-69-v1/attempt-002/retained/job-127dd684d17b53ef014aab9a240b981c9f276e6d7f410b1784bfddabd82e4d63-reply-e6630b0fe80ecc2bc218fbe3e2155902287ae5eab51557451e29a7e7f7d00298.txt` — `[:text]`.

Full review text for job invoke-1790034134160-23090-98ae7b08 is retained at the following source.

- Source: `/home/joe/code/futon2/data/wm-full-loop-machinery-69/wm-contract-machinery-69-v1/attempt-002/retained/job-bdc6fec27de0681292a274bb56b4274cab3abccbfa102934f435ff6ecad2bfc7-reply-31211924f888adcc69df9db48fde3c6a158b7f807f140aea7dd6d40e44471878.txt` — `[:text]`.

Cited facts:
- Source: `./data/wm-full-loop-machinery-69/wm-contract-machinery-69-v1/attempt-002/005-build.edn` — `[:payload :judgment]`.

Recorded phase durations: author-wait 249479 ms; build-resolution 3938 ms.
- Source: `./data/wm-full-loop-phases.edn.log` — `[:form 11405 :duration-ms]`.
- Source: `./data/wm-full-loop-phases.edn.log` — `[:form 11407 :duration-ms]`.

## adjudication

The recorded build match says review-approved true for commit aeb352f87368fb3280b3fb92ddd8e7f7c5fccc08. Grounding returned implementation full-loop/implementation/aeb352f87368fb3280b3fb92ddd8e7f7c5fccc08 and discharge full-loop/discharge/cohort/wm-contract-machinery-69-v1/run/2026-09-21-1790033693/attempt/attempt-002; its dial-moved claim is true.

Cited facts:
- Source: `./data/wm-full-loop-machinery-69/wm-contract-machinery-69-v1/attempt-002/006-adjudication.edn` — `[:payload :judgment]`.

Recorded phase durations: reviewer-wait 133062 ms; grounding 5787 ms.
- Source: `./data/wm-full-loop-phases.edn.log` — `[:form 11411 :duration-ms]`.
- Source: `./data/wm-full-loop-phases.edn.log` — `[:form 11413 :duration-ms]`.

## closed

The attempt closed with outcome :build-failed after 600884 ms. The recorded entity state at close has status not recorded and reason not recorded. Its Morning Brief reference is not recorded.

Route and attestation: none declared.

Here, grounded change means a reviewed commit recorded in futon1b, not wanted-token completion.
- Source: `src/futon2/aif/full_loop_runner.clj` — `[ground-commit!]`.
Not recorded in this run: token-outcome comparison receipt. Not recorded in this run: D-task record.
- Source: `./data/wm-full-loop-machinery-69/wm-contract-machinery-69-v1/attempt-002/002-selection.edn` — `[:payload :judgment :controller-decision :selection-certificate :token-belief-stage :domain-inputs]`.
- Source: `./data/wm-full-loop-machinery-69/wm-contract-machinery-69-v1/attempt-002/002-selection.edn` — `[:payload :judgment :selected-action :precedence]`.
- Source: `./data/wm-runs/tick-run-record-2026-09-21-1790033693.edn` — `[:d-task-enactment :source]`.

Cited facts:
- Source: `./data/wm-full-loop-machinery-69/wm-contract-machinery-69-v1/attempt-002/007-closed.edn` — `[:payload :judgment]`.

Not recorded in this run: matching phase duration.

