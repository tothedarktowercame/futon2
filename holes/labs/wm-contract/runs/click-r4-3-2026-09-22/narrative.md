# Run 2026-09-22-1790053967

A narrative of retained evidence; no selection, observation, or actuation was rerun.

## time-step

The run began at 2026-09-22T05:12:55.046637705Z with trigger :duree-click-on-demand. Its opportunity is duree-click-on-demand/2026-09-22T05:12:56.240315324Z/a2fb841e-cc19-49af-9002-29e9f7744db4, and the recorded semantic epoch is :full-loop-real-actuation-v6.

The machine perceived in mode :stop-the-line, with stop-the-line active. Its recorded free-energy account is controller-score 0.41147533886837134, preference-gap-score 0.3318595106227832, coverage-uncertainty-pressure 0.5593333056101779; active avoided channels are :stack-pct, :consulting-pct. It received 14 observation channels; the largest recorded preference gaps give :stack-pct = 0.9812142662673564 (gap 0.7312142662673564), :mission-health = 0.039627039627039624 (gap 0.4603729603729604), :support-coverage = 0.4 (gap 0.4), :loop-health = 0.42666673597455523 (gap 0.3733332640254448). 417 of 417 belief rows changed from mu-pre to mu-post (including added or removed rows). The recorded route is :R20 → :R12 → :R2 → :R7 → :R3 → :R8 → :TRACE.

- Source: `./data/wm-trace/wm-trace-2026-09-22.edn` — `[:form 2 :mode]`.
- Source: `./data/wm-trace/wm-trace-2026-09-22.edn` — `[:form 2 :free-energy]`.
- Source: `./data/wm-trace/wm-trace-2026-09-22.edn` — `[:form 2 :observation]`.
- Source: `./data/wm-trace/wm-trace-2026-09-22.edn` — `[:form 2 :mu-pre]`.
- Source: `./data/wm-trace/wm-trace-2026-09-22.edn` — `[:form 2 :mu-post]`.
- Source: `./data/wm-trace/wm-trace-2026-09-22.edn` — `[:form 2 :wm/route]`.

[Retained scan account](</home/joe/code/futon2/data/wm-runs/tick-run-record-2026-09-22-1790053967.scan.md>), SHA-256 `b2c8810095f6b58539ccc54727ccdddf2450b0eff18015ca799e130f331c1e6f`. This is the saved perceive-stage account, not a fresh scan.
- Source: `./data/wm-runs/tick-run-record-2026-09-22-1790053967.edn` — `[:scan-report]`.

Cited facts:
- Source: `./data/wm-full-loop-machinery-70/wm-contract-machinery-70-v1/attempt-002/001-time-step.edn` — `[:payload :judgment]`.
- Source: `./data/wm-runs/tick-run-record-2026-09-22-1790053967.edn` — `[:startedAt]`.

Recorded phase durations: agent-readiness 4126 ms; code-state 4954 ms; substrate-preflight 6042 ms.
- Source: `./data/wm-full-loop-phases.edn.log` — `[:form 11503 :duration-ms]`.
- Source: `./data/wm-full-loop-phases.edn.log` — `[:form 11505 :duration-ms]`.
- Source: `./data/wm-full-loop-phases.edn.log` — `[:form 11507 :duration-ms]`.

## selection

The machine scored 3 admitted cascades; enumeration completeness is :incomplete. 284 targets were considered; 282 were refused (17 :no-admitted-interpretation missing :interpretations, 1 :no-constructed-candidate missing [:new-wanted-token-within-horizon], 1 :universe-not-admitted missing :locators, 263 :universe-not-admitted missing :universes); proposal supply retained 91 proposals and 0 admissions. It chose M-aif-policy-conditioned-eig (cascade :C3); the checkpoint's selected-mission field is M-aif-policy-conditioned-eig. The computed full G spread is 4.076024992016869E-4 nats. The policy comparison records decided-by :tie-break, near-tie :threshold-undeclared, threshold {:status :undeclared}; The action comparison records decided-by :tie-break.
C reached 4 of 468 source tokens, with 5 projected outcome tokens; mission-hole census retained 585 holes and projected 116.

C preference audit (source budget: :global-deduplicated-live-source-inventory, 468 source entries): C prefers ["M-aif-policy-conditioned-eig" :hole/h6378c65a4012] present over absent by 1.001306 : 1; ["M-aif-policy-conditioned-eig" :hole/h0e270aa090bc] present over absent by 1.001306 : 1; ["M-aif-policy-conditioned-eig" :hole/h42fceb4ad48b] present over absent by 1.001306 : 1.

Discovered focus: WM; candidate classes: M-aif-policy-conditioned-eig/:C2 = focus; M-aif-policy-conditioned-eig/:C3 = focus; M-f11-find-production-successor/:C1 = focus (record-only).
Expected parameter information gain (record-only, not in G): M-aif-policy-conditioned-eig/:C2: unavailable (:route-unavailable) under :illustrative; M-aif-policy-conditioned-eig/:C3: unavailable (:route-unavailable) under :illustrative; M-f11-find-production-successor/:C1: unavailable (:route-unavailable) under :illustrative.

| Target | Cascade | G (nats) | Posterior | Habit | F consumed |
|---|---|---:|---:|---:|---:|
| M-aif-policy-conditioned-eig | :C2 | 9.704306500045462 | 0.3333786194228307 | 0.3333333333333333 | not recorded |
| M-aif-policy-conditioned-eig | :C3 | 9.704306500045462 | 0.3333786194228307 | 0.3333333333333333 | not recorded |
| M-f11-find-production-successor | :C1 | 9.704714102544663 | 0.3332427611543394 | 0.3333333333333333 | not recorded |

![Selection: relative G and posterior](<narrative.selection.svg>)

G and posterior are read separately from the certificate and selection law; legacy checkpoint G-efe is not treated as G.

Cited facts:
- Source: `./data/wm-full-loop-machinery-70/wm-contract-machinery-70-v1/attempt-002/002-selection.edn` — `[:payload :judgment]`.
- Source: `./data/wm-full-loop-machinery-70/wm-contract-machinery-70-v1/attempt-002/002-selection.edn` — `[:payload :judgment :controller-decision :selection-certificate :candidates]`.
- Source: `./data/wm-full-loop-machinery-70/wm-contract-machinery-70-v1/attempt-002/002-selection.edn` — `[:payload :judgment :controller-decision :selection-law]`.
- Source: `./data/wm-trace/wm-trace-2026-09-22.edn` — `[:form 2 :cascade-problems]`.
- Source: `./data/wm-runs/tick-run-record-2026-09-22-1790053967.edn` — `[:live-c-coverage]`.
- Source: `./data/wm-runs/tick-run-record-2026-09-22-1790053967.edn` — `[:mission-hole-coverage]`.

Docstring correspondence, not runtime Lean execution: `src/futon2/aif/cascade_selection.clj` namespace docstring cites `PolicySelection.lean` at `a434947c63`, `selectionPosterior`.

Recorded phase durations: selection 59256 ms.
- Source: `./data/wm-full-loop-phases.edn.log` — `[:form 11513 :duration-ms]`.

## construction

The cascade contains 1 pattern(s), in precedence order: :aif/two-layer-calibration. Its recorded construction method is :hand-admitted; retrieval method: not recorded for this selected cascade. Its observed structure is a singleton, with no wires. shape: singleton (basis: declared need-support; authority structure not recorded).


![Cascade: precedence and wanted-token outcomes](<narrative.cascade.svg>)
The following build plan is quoted from the retained author prompt.

> Recorded cascade plan (limit 24000 chars).
> pattern 1: :aif/two-layer-calibration
>   flexiarg: futon3/library/aif/two-layer-calibration.flexiarg
>   sha256: 0b9c0c25c8e9aef566d1d8f23f95c49f6abf200966271958f1bf17287b90b03f
>   reading: The requested EIG shadow can validate its own arithmetic while failing to predict evidence. Apply the pattern's separation of consistency from witnessed outcomes: keep coherent model-relative EIG/reduction checks and off-mode byte-identical selection replay in one layer, and evaluate preregistered held-out evidence with log-loss/Brier and predicted-versus-realised entropy reduction in a separately labelled empirical layer. Persist prior entropy, expected posterior entropy, EIG, model/source pins, degeneracy reasons and later realised information gain, together with winner/abstain/scale effects, in the mission's shadow packet. Use the shared updater for hypothetical and observed evidence. Neither an internal consistency pass nor a winner-changing ablation substitutes for the held-out packet.
>   scope limit: Collect and report a default-off shadow only; Joe retains the later decision to replace or weight the proxy. This reading does not author F1's generative experiment model, invent missing probabilities or held-out outcomes, claim causal calibration from a self-produced score, or complete the separate typed-risk boundary. If the declared model or prospective evidence is unavailable, retain that absence and leave this task open; preparation alone does not produce its completion token.
>   observation limit: C4 observes the mission's completion declaration only. Require both the retained shadow/replay artifacts and actual held-out calibration evidence before checking it; a held or missing empirical layer is not completion.
>   guards:
>   - M-aif-policy-conditioned-eig / :hole/h6378c65a4012 expected true: established (recorded status: :established); check :C4; observed true; resolved sha 16d4482c2c1e9d47d22f9e849ce4990fed92139a; looked for line - [x] **Mint the shared posterior updater, then unify the two paths through; evidence :file-present = true; :path = holes/missions/M-aif-policy-conditioned-eig.md; :repo = futon2; :resolved-sha = 16d4482c2c1e9d47d22f9e849ce4990fed92139a; :sha = HEAD
>   - M-aif-policy-conditioned-eig / :hole/h42fceb4ad48b expected false: established (recorded status: :established); check :C4; observed false; resolved sha 16d4482c2c1e9d47d22f9e849ce4990fed92139a; looked for line - [x] **Collect a default-off EIG shadow and calibration packet.** Prior; evidence :file-present = true; :path = holes/missions/M-aif-policy-conditioned-eig.md; :repo = futon2; :resolved-sha = 16d4482c2c1e9d47d22f9e849ce4990fed92139a; :sha = HEAD
>   produces: M-aif-policy-conditioned-eig / :hole/h42fceb4ad48b
> 
> holes: none
> wires: none

- Source: `/home/joe/code/futon2/data/wm-full-loop-machinery-70/wm-contract-machinery-70-v1/attempt-002/retained/job-230af14b77078b952cea2e5f3ea3299eb74e3215694fdae7b79d882b4585e2f7-prompt-92924f21b94a0f15d5290b9b4a7db46b8655466ea09d2b7e7a6514572f271a83.txt` — `[:text "PATTERN CASCADE"]`.

Cited facts:
- Source: `./data/wm-full-loop-machinery-70/wm-contract-machinery-70-v1/attempt-002/003-construction.edn` — `[:payload :judgment]`.

Recorded phase durations: construction 12601 ms.
- Source: `./data/wm-full-loop-phases.edn.log` — `[:form 11515 :duration-ms]`.

## dispatch

The runner dispatched agent wm-author as job invoke-1790054143486-23122-c7b47820. Its prompt reference is agency-job:invoke-1790054143486-23122-c7b47820, with availability recorded as :invoke-ready.

Cited facts:
- Source: `./data/wm-full-loop-machinery-70/wm-contract-machinery-70-v1/attempt-002/004-dispatch.edn` — `[:payload :judgment]`.

Recorded phase durations: author-dispatch 7292 ms.
- Source: `./data/wm-full-loop-phases.edn.log` — `[:form 11517 :duration-ms]`.

## build

Not recorded in this run: checkpoint. The remaining evidence does not establish this stage's outcome.

Cited facts:
- Source: `./data/wm-full-loop-machinery-70/wm-contract-machinery-70-v1/attempt-002/005-build.edn` — `[:payload :judgment]`.

Recorded phase durations: author-wait 293718 ms; build-resolution 4218 ms.
- Source: `./data/wm-full-loop-phases.edn.log` — `[:form 11519 :duration-ms]`.
- Source: `./data/wm-full-loop-phases.edn.log` — `[:form 11521 :duration-ms]`.

## adjudication

Not recorded in this run: checkpoint. The remaining evidence does not establish this stage's outcome.

Cited facts:
- Source: `./data/wm-full-loop-machinery-70/wm-contract-machinery-70-v1/attempt-002/006-adjudication.edn` — `[:payload :judgment]`.

Recorded phase durations: reviewer-wait 87004 ms.
- Source: `./data/wm-full-loop-phases.edn.log` — `[:form 11525 :duration-ms]`.

## closed

The attempt closed with outcome :guardrail-refusal after 636358 ms. The recorded entity state at close has status :absent and reason :in-force-belief-row-unavailable. Its Morning Brief reference is /home/joe/code/futon2/data/wm-morning-brief/items/ea1-49a2d5c07252413fddedecfde64cb8e17303d129aed72ae11608ae3805c22309--attempt-002.edn.

Route and attestation: none declared.

Here, grounded change means a reviewed commit recorded in futon1b, not wanted-token completion.
- Source: `src/futon2/aif/full_loop_runner.clj` — `[ground-commit!]`.
The retained token-outcome comparison receipt is preferred; its status is :compared, with model prediction rule :positive-marginal-support.
- Source: `./data/wm-full-loop-machinery-70/wm-contract-machinery-70-v1/attempt-002/007-closed.edn` — `[:payload :judgment :token-outcome-comparison]`.

Selected target: M-aif-policy-conditioned-eig.

| Wanted token | model prediction | Observed after build | Establishing check |
|---|---|---|---|
| :hole/h0e270aa090bc | 0 | {:status :missing, :kind :measurement-unavailable} | not recorded |
| :hole/h42fceb4ad48b | 1 | {:status :missing, :kind :measurement-unavailable} | not recorded |
| :hole/h6378c65a4012 | 1 | {:status :missing, :kind :measurement-unavailable} | not recorded |

Not recorded in this run: after-build measurement for :hole/h0e270aa090bc.

Not recorded in this run: after-build measurement for :hole/h42fceb4ad48b.

Not recorded in this run: after-build measurement for :hole/h6378c65a4012.
Attempt learning trial for cascade :C3 → ["M-aif-policy-conditioned-eig" :hole/h42fceb4ad48b]: held (observation-missing).

Cited facts:
- Source: `./data/wm-full-loop-machinery-70/wm-contract-machinery-70-v1/attempt-002/007-closed.edn` — `[:payload :judgment]`.

Recorded phase durations: opportunity 636640 ms.
- Source: `./data/wm-full-loop-phases.edn.log` — `[:form 11530 :duration-ms]`.

