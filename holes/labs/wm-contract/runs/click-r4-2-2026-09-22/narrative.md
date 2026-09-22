# Run 2026-09-22-1790037762

A narrative of retained evidence; no selection, observation, or actuation was rerun.

## time-step

The run began at 2026-09-22T00:42:48.577764228Z with trigger :duree-click-on-demand. Its opportunity is duree-click-on-demand/2026-09-22T00:42:49.724531819Z/30c1509d-e166-4477-8bb1-900416b7b72f, and the recorded semantic epoch is :full-loop-real-actuation-v6.

The machine perceived in mode :stop-the-line, with stop-the-line active. Its recorded free-energy account is controller-score 0.41771712146568163, preference-gap-score 0.33187988008196734, coverage-uncertainty-pressure 0.577129141178294; active avoided channels are :stack-pct, :consulting-pct. It received 14 observation channels; the largest recorded preference gaps give :stack-pct = 0.9812957441040933 (gap 0.7312957441040933), :mission-health = 0.039627039627039624 (gap 0.4603729603729604), :loop-health = 0.38217714705426514 (gap 0.4178228529457349), :support-coverage = 0.4 (gap 0.4). 417 of 417 belief rows changed from mu-pre to mu-post (including added or removed rows). The recorded route is :R20 → :R12 → :R2 → :R7 → :R3 → :R8 → :TRACE.

- Source: `./data/wm-trace/wm-trace-2026-09-22.edn` — `[:form 1 :mode]`.
- Source: `./data/wm-trace/wm-trace-2026-09-22.edn` — `[:form 1 :free-energy]`.
- Source: `./data/wm-trace/wm-trace-2026-09-22.edn` — `[:form 1 :observation]`.
- Source: `./data/wm-trace/wm-trace-2026-09-22.edn` — `[:form 1 :mu-pre]`.
- Source: `./data/wm-trace/wm-trace-2026-09-22.edn` — `[:form 1 :mu-post]`.
- Source: `./data/wm-trace/wm-trace-2026-09-22.edn` — `[:form 1 :wm/route]`.

[Retained scan account](</home/joe/code/futon2/data/wm-runs/tick-run-record-2026-09-22-1790037762.scan.md>), SHA-256 `058352c563cf986af7fbccce2ba0d70b54426486eb813ca4640f9a9ef997e767`. This is the saved perceive-stage account, not a fresh scan.
- Source: `./data/wm-runs/tick-run-record-2026-09-22-1790037762.edn` — `[:scan-report]`.

Cited facts:
- Source: `./data/wm-full-loop-machinery-70/wm-contract-machinery-70-v1/attempt-001/001-time-step.edn` — `[:payload :judgment]`.
- Source: `./data/wm-runs/tick-run-record-2026-09-22-1790037762.edn` — `[:startedAt]`.

Recorded phase durations: agent-readiness 3930 ms; code-state 4595 ms; substrate-preflight 6689 ms.
- Source: `./data/wm-full-loop-phases.edn.log` — `[:form 11451 :duration-ms]`.
- Source: `./data/wm-full-loop-phases.edn.log` — `[:form 11453 :duration-ms]`.
- Source: `./data/wm-full-loop-phases.edn.log` — `[:form 11455 :duration-ms]`.

## selection

The machine scored 1 admitted cascades; enumeration completeness is :incomplete. 283 targets were considered; 282 were refused (17 :no-admitted-interpretation missing :interpretations, 2 :no-constructed-candidate missing [:new-wanted-token-within-horizon], 1 :universe-not-admitted missing :locators, 262 :universe-not-admitted missing :universes); proposal supply retained 90 proposals and 0 admissions. It chose M-f11-find-production-successor (cascade :C1); the checkpoint's selected-mission field is M-f11-find-production-successor. The computed full G spread is 0.0 nats. The policy comparison records decided-by :no-competing-policy, near-tie :threshold-undeclared, threshold {:status :undeclared}; The action comparison records decided-by :robust.
C reached 1 of 467 source tokens, with 2 projected outcome tokens; mission-hole census retained 585 holes and projected 116.

C preference audit (source budget: :global-deduplicated-live-source-inventory, 467 source entries): C prefers ["M-f11-find-production-successor" :hole/h9ab212b3281d] present over absent by 1.000043 : 1; ["M-f11-find-production-successor" :hole/h2045faa0e7cc] present over absent by 1.000043 : 1.

Discovered focus: WM; candidate classes: M-f11-find-production-successor/:C1 = focus (record-only).
Expected parameter information gain (record-only, not in G): M-f11-find-production-successor/:C1: unavailable (:route-unavailable) under :illustrative.

| Target | Cascade | G (nats) | Posterior | Habit | F consumed |
|---|---|---:|---:|---:|---:|
| M-f11-find-production-successor | :C1 | 4.158883083818247 | 1.0 | 1.0 | not recorded |

![Selection: relative G and posterior](<narrative.selection.svg>)

G and posterior are read separately from the certificate and selection law; legacy checkpoint G-efe is not treated as G.

Cited facts:
- Source: `./data/wm-full-loop-machinery-70/wm-contract-machinery-70-v1/attempt-001/002-selection.edn` — `[:payload :judgment]`.
- Source: `./data/wm-full-loop-machinery-70/wm-contract-machinery-70-v1/attempt-001/002-selection.edn` — `[:payload :judgment :controller-decision :selection-certificate :candidates]`.
- Source: `./data/wm-full-loop-machinery-70/wm-contract-machinery-70-v1/attempt-001/002-selection.edn` — `[:payload :judgment :controller-decision :selection-law]`.
- Source: `./data/wm-trace/wm-trace-2026-09-22.edn` — `[:form 1 :cascade-problems]`.
- Source: `./data/wm-runs/tick-run-record-2026-09-22-1790037762.edn` — `[:live-c-coverage]`.
- Source: `./data/wm-runs/tick-run-record-2026-09-22-1790037762.edn` — `[:mission-hole-coverage]`.

Docstring correspondence, not runtime Lean execution: `src/futon2/aif/cascade_selection.clj` namespace docstring cites `PolicySelection.lean` at `a434947c63`, `selectionPosterior`.

Recorded phase durations: selection 48940 ms.
- Source: `./data/wm-full-loop-phases.edn.log` — `[:form 11461 :duration-ms]`.

## construction

The cascade contains 1 pattern(s), in precedence order: :apparatus/done-is-observed-running. Its recorded construction method is :hand-admitted; retrieval method: not recorded for this selected cascade. Its observed structure is a singleton, with no wires. shape: singleton (basis: declared need-support; authority structure not recorded).


![Cascade: precedence and wanted-token outcomes](<narrative.cascade.svg>)
The following build plan is quoted from the retained author prompt.

> Recorded cascade plan (limit 24000 chars).
> pattern 1: :apparatus/done-is-observed-running
>   flexiarg: futon3/library/apparatus/done-is-observed-running.flexiarg
>   sha256: 301a19b722382997e8e1d455be1c40605ebc987c998aedd5ce5302bcb4a70653
>   reading: For F11's acceptance task, require the finder to act at its applied interface on the committed library and retain the ordinary gate and runtime evidence before recording completion; a committed reconciliation module or a stopped run is not that observation.
>   scope limit: This is the activation-and-live-evidence reading of the pattern, not a claim that its standing-comparator mechanism or F1-F4 laws have been implemented. It does not produce the separate repair-024 successor disposition.
>   observation limit: C4 observes the mission's checked completion declaration, not the truth of its supporting evidence. Completion requires the mission's acceptance work; merely ticking the checkbox is not the action admitted here.
>   guards:
>   - M-f11-find-production-successor / :admission/task-stated expected true: established (recorded status: :established); check :C4; observed true; resolved sha cb2045b8279853fbd48d6a07c1e2da952f2e338f; looked for line - [ ] Complete F11's ordinary acceptance and persist its runtime validation evidence.; evidence :file-present = true; :path = holes/missions/M-f11-find-production-successor.md; :repo = futon2; :resolved-sha = cb2045b8279853fbd48d6a07c1e2da952f2e338f; :sha = cb2045b8279853fbd48d6a07c1e2da952f2e338f
>   - M-f11-find-production-successor / :hole/h9ab212b3281d expected false: established (recorded status: :established); check :C4; observed false; resolved sha 3134b61f11cad3ae18d1d676f2070b5782d225cf; looked for line - [x] Complete F11's ordinary acceptance and persist its runtime validation evidence.; evidence :file-present = true; :path = holes/missions/M-f11-find-production-successor.md; :repo = futon2; :resolved-sha = 3134b61f11cad3ae18d1d676f2070b5782d225cf; :sha = HEAD
>   produces: M-f11-find-production-successor / :hole/h9ab212b3281d
> 
> holes: none
> wires: none

- Source: `/home/joe/code/futon2/data/wm-full-loop-machinery-70/wm-contract-machinery-70-v1/attempt-001/retained/job-56e93f9bbdb6654700e3c6970295eeaf6aad0a8413caaff554805a6fef1607b0-prompt-c73165eb135902edf0e9e9c4cfd9313e75ffd4b0ef2766cffcd32967f09589ad.txt` — `[:text "PATTERN CASCADE"]`.

Cited facts:
- Source: `./data/wm-full-loop-machinery-70/wm-contract-machinery-70-v1/attempt-001/003-construction.edn` — `[:payload :judgment]`.

Recorded phase durations: construction 13575 ms.
- Source: `./data/wm-full-loop-phases.edn.log` — `[:form 11463 :duration-ms]`.

## dispatch

The runner dispatched agent wm-author as job invoke-1790037927615-23101-8e4750f0. Its prompt reference is agency-job:invoke-1790037927615-23101-8e4750f0, with availability recorded as :invoke-ready.

Cited facts:
- Source: `./data/wm-full-loop-machinery-70/wm-contract-machinery-70-v1/attempt-001/004-dispatch.edn` — `[:payload :judgment]`.

Recorded phase durations: author-dispatch 7332 ms.
- Source: `./data/wm-full-loop-phases.edn.log` — `[:form 11465 :duration-ms]`.

## build

Not recorded in this run: checkpoint. The remaining evidence does not establish this stage's outcome.

Cited facts:
- Source: `./data/wm-full-loop-machinery-70/wm-contract-machinery-70-v1/attempt-001/005-build.edn` — `[:payload :judgment]`.

Recorded phase durations: author-wait 194222 ms; build-resolution 4108 ms.
- Source: `./data/wm-full-loop-phases.edn.log` — `[:form 11467 :duration-ms]`.
- Source: `./data/wm-full-loop-phases.edn.log` — `[:form 11469 :duration-ms]`.

## adjudication

Not recorded in this run: checkpoint. The remaining evidence does not establish this stage's outcome.

Cited facts:
- Source: `./data/wm-full-loop-machinery-70/wm-contract-machinery-70-v1/attempt-001/006-adjudication.edn` — `[:payload :judgment]`.

Recorded phase durations: reviewer-wait 86318 ms.
- Source: `./data/wm-full-loop-phases.edn.log` — `[:form 11473 :duration-ms]`.

## closed

The attempt closed with outcome :guardrail-refusal after 509704 ms. The recorded entity state at close has status :absent and reason :in-force-belief-row-unavailable. Its Morning Brief reference is /home/joe/code/futon2/data/wm-morning-brief/items/ea1-49a2d5c07252413fddedecfde64cb8e17303d129aed72ae11608ae3805c22309--attempt-001.edn.

Route and attestation: none declared.

Here, grounded change means a reviewed commit recorded in futon1b, not wanted-token completion.
- Source: `src/futon2/aif/full_loop_runner.clj` — `[ground-commit!]`.
The retained token-outcome comparison receipt is preferred; its status is :compared, with model prediction rule :positive-marginal-support.
- Source: `./data/wm-full-loop-machinery-70/wm-contract-machinery-70-v1/attempt-001/007-closed.edn` — `[:payload :judgment :token-outcome-comparison]`.

Selected target: M-f11-find-production-successor.

| Wanted token | model prediction | Observed after build | Establishing check |
|---|---|---|---|
| :hole/h2045faa0e7cc | 0 | {:status :missing, :kind :measurement-unavailable} | not recorded |
| :hole/h9ab212b3281d | 1 | {:status :missing, :kind :measurement-unavailable} | not recorded |

Not recorded in this run: after-build measurement for :hole/h2045faa0e7cc.

Not recorded in this run: after-build measurement for :hole/h9ab212b3281d.
Attempt learning trial for cascade :C1 → ["M-f11-find-production-successor" :hole/h9ab212b3281d]: held (observation-missing).

Cited facts:
- Source: `./data/wm-full-loop-machinery-70/wm-contract-machinery-70-v1/attempt-001/007-closed.edn` — `[:payload :judgment]`.

Recorded phase durations: opportunity 509880 ms.
- Source: `./data/wm-full-loop-phases.edn.log` — `[:form 11478 :duration-ms]`.

