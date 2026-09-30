# Initial mission HEAD cascades — 2026-09-30

## Method

The request source is the complete `## HEAD` section when present. Otherwise it is the opening from the title through the byte before the first level-two heading. `scripts/wm_task_reading.py --mission-head` records the choice as `task.source_kind`, plus canonical path and content SHA-256, and emits the unchanged `session_turn_analysis.py` schema. Eight requests were read by the 象 pool and completed with the unchanged validator. Zero patterns and zero connected retractions are reported as failures.

Selection rule: enumerate tracked `M-*.md` mission files in canonical repositories whose top-level path is one of the Futon repositories from futon0 through futon7 (including their canonical lettered components), exclude dispositions that begin closed, superseded, abandoned, complete, finished, archived, or cancelled, sort by the file’s latest commit time with repository/path as tie-break, and take the four newest with an exact `## HEAD` plus the four newest without one.

The pattern graph was regenerated after the ruling that `co-rejected` edges are not recorded. It contains only `why`, `how`, `co-cited`, `rejected-beside`, and `next-in-session`. It was written once by the current `futon3c/scripts/mined_pattern_graph.py`, then passed by path to every `pattern_retraction.py --k 3` call. Committed file SHA-256: `b392b4a3f363e141a5befdc7a32267e325dd61697132fee1c0dfd86250094286`; 1,431 patterns and 4,662 analysis inputs. Its giant component has 620 patterns; 520 patterns have no edges. `next-in-session` edges are undirected for connection, while their evidence retains the observed `A -> B` order.

“Materialized policy set” is the number of distinct arrangements produced across alternatives and overlap modes plus graph retractions. Rejected patterns are possible adjustment probes but are not policies because 象 rejected them; admitting one requires a later reading.

## Results

| # | mission / source | fragments / seeds | initial alternatives / overlap | retractions | policies | refused / remaining connected | retraction time |
|---:|---|---:|---:|---:|---:|---|---:|
| 1 | `M-象-2000` / HEAD | 17 / 9 | 8 / 4 | 3 | 15 | 2 / yes | 2.017s |
| 2 | `M-metric-harness` / HEAD | 26 / 10 | 4 / 4 | 0 | 4 | 1 / **no** | 0.071s |
| 3 | `M-distributed-proofreaders` / HEAD | 8 / 3 | 6 / 6 | 3 | 9 | 0 / yes | 0.267s |
| 4 | `M-web-arxana-ui-improvements` / HEAD | 2 / 2 | 6 / 3 | 3 | 12 | 0 / yes | 0.162s |
| 5 | `M-self-documenting-stack` / opening | 16 / 5 | 1 / 1 | 3 | 4 | 1 / yes | 0.243s |
| 6 | `M-war-machine-aif-completion` / opening | 38 / 10 | 16 / 8 | 3 | 27 | 1 / yes | 5.804s |
| 7 | `M-essays-diachronic-model` / opening | 4 / 2 | 4 / 4 | 1 | 5 | 1 / yes | 0.050s |
| 8 | `M-value-creation-loop` / opening | 7 / 3 | 2 / 2 | 3 | 5 | 0 / yes | 0.125s |

### 1. M-象-2000

- Initial seeds: `futon-theory/rapid-debugging`, `invariant-coherence/protocol-family-naming`, `math-strategy/corpus-trust-protocol`, `pattern-coherence/causal-clarity`, `pattern-interpretation/cite-the-source-bytes`, `peripherals/read-existing-seam-before-implementing`, `象/双时并记`, `象/视图出于史`, `象/象不忘`.
- Initial arrangements: alternatives 8; overlap 4; 12 distinct across both modes.
- No-edge seed failures: 2 — `futon-theory/rapid-debugging`, `pattern-interpretation/cite-the-source-bytes`.
- Remaining seeds connected: yes.
- Retraction 1: 13 nodes, 12 edges (`why`=3, `how`=1, `co-cited`=3, `next-in-session`=5); nodes: `agent/handoff-preserves-context`, `cascade-construction/add-a-pattern-when-an-item-fits-no-class`, `invariant-coherence/protocol-family-naming`, `math-strategy/corpus-trust-protocol`, `orchestration/consent-gate`, `pattern-coherence/causal-clarity`, `peripherals/read-existing-seam-before-implementing`, `translation/bind-to-the-source`, `workshop/open-proposals-named-adoption`, `象/双时并记`, `象/翻译契约`, `象/视图出于史`, `象/象不忘`.
  - `next-in-session` direction evidence: `2026-08-22_2026-09-21_block025/claude-20-turn-35.json.analysis.json -> 2026-08-22_2026-09-21_block025/claude-20-turn-51.json.analysis.json`; `live/turn-4cK2SO.json.analysis.json -> live/turn-BI6luN.json.analysis.json`; `live/turn-L2e5uG.json.analysis.json -> 2026-09-22_2026-09-26_block001/claude-1-turn-18-20260923195901.json.analysis.json`; `live/turn-bHZV4Z.json.analysis.json -> live/turn-3ecbrN.json.analysis.json`; `live/turn-9umMUN.json.analysis.json -> live/turn-4OobXs.json.analysis.json`.
- Retraction 2: 15 nodes, 14 edges (`why`=5, `how`=1, `co-cited`=5, `next-in-session`=3); nodes: `agent/state-is-hypothesis`, `cascade-construction/add-a-pattern-when-an-item-fits-no-class`, `invariant-coherence/protocol-family-naming`, `math-strategy/corpus-trust-protocol`, `orchestration/consent-gate`, `pattern-coherence/causal-clarity`, `peripherals/read-existing-seam-before-implementing`, `translation/bind-to-the-source`, `translation/choose-the-equivalence-check`, `translation/revise-the-source-under-pressure`, `workshop/open-proposals-named-adoption`, `象/双时并记`, `象/翻译契约`, `象/视图出于史`, `象/象不忘`.
  - `next-in-session` direction evidence: `live/turn-L2e5uG.json.analysis.json -> 2026-09-22_2026-09-26_block001/claude-1-turn-18-20260923195901.json.analysis.json`; `live/turn-bHZV4Z.json.analysis.json -> live/turn-3ecbrN.json.analysis.json`; `live/turn-9umMUN.json.analysis.json -> live/turn-4OobXs.json.analysis.json`.
- Retraction 3: 16 nodes, 15 edges (`why`=5, `how`=1, `co-cited`=7, `next-in-session`=2); nodes: `agent/state-is-hypothesis`, `cascade-construction/add-a-pattern-when-an-item-fits-no-class`, `invariant-coherence/protocol-family-naming`, `math-strategy/corpus-trust-protocol`, `orchestration/consent-gate`, `pattern-coherence/causal-clarity`, `peripherals/read-existing-seam-before-implementing`, `proof-search/typed-hole-as-frontier`, `translation/bind-to-the-source`, `translation/choose-the-equivalence-check`, `translation/revise-the-source-under-pressure`, `workshop/open-proposals-named-adoption`, `象/双时并记`, `象/翻译契约`, `象/视图出于史`, `象/象不忘`.
  - `next-in-session` direction evidence: `live/turn-L2e5uG.json.analysis.json -> 2026-09-22_2026-09-26_block001/claude-1-turn-18-20260923195901.json.analysis.json`; `live/turn-bHZV4Z.json.analysis.json -> live/turn-3ecbrN.json.analysis.json`.
- Rejected-pattern adjustment probes:
  - `invariant-coherence/state-snapshot-witness` from query “as-of database rewind time travel history”.
  - `enrichment/rational-reconstruction` from query “Elephant 2000 McCarthy as-of bitemporal”.
- Further patterns introduced by retractions: `agent/handoff-preserves-context`, `agent/state-is-hypothesis`, `cascade-construction/add-a-pattern-when-an-item-fits-no-class`, `orchestration/consent-gate`, `proof-search/typed-hole-as-frontier`, `translation/bind-to-the-source`, `translation/choose-the-equivalence-check`, `translation/revise-the-source-under-pressure`, `workshop/open-proposals-named-adoption`, `象/翻译契约`.
- Available arrangements now: 15 materialized policies (12 fragment-derived + 3 connected graph retractions).

### 2. M-metric-harness

- Initial seeds: `data-mining/smoke-before-the-paid-run`, `futon-theory/honest-map-over-flattering-counter`, `futon-theory/progress-signal`, `measurement/error-needs-structure-to-teach`, `musn/declare-scope`, `plos-npt-with-small-n/absence-as-evidence`, `plos-npt-with-small-n/research-questions-stated-early`, `system-coherence/facet-before-aggregating`, `vsatelier/decision-provenance`, `writing-coherence/hedged-lift`.
- Initial arrangements: alternatives 4; overlap 4; 4 distinct across both modes.
- No-edge seed failures: 1 — `plos-npt-with-small-n/research-questions-stated-early`.
- Remaining seeds connected: **no (failure)**.
- Retractions: **0 (failure)**.
- Rejected-pattern adjustment probes:
  - `futon-theory/counter-ratchet` from query “counter ratchet only moves one way”.
  - `process-coherence/stuck-means-signal` from query “each repetition should measurably improve; if it does not, find out why”.
  - `data-mining/smoke-before-the-paid-run` from query “substrate should compound; held-out case grounds better as corpus grows”.
  - `writing-coherence/citation-density-load-bearing-claim` from query “cite the source of a lesson being generalized”.
- Further patterns introduced by retractions: none.
- Available arrangements now: 4 materialized policies (4 fragment-derived + 0 connected graph retractions).

### 3. M-distributed-proofreaders

- Initial seeds: `apparatus/new-failure-class-is-a-design-defect`, `ukrns/publication-cadence`, `war-machine/operational-not-decorative`.
- Initial arrangements: alternatives 6; overlap 6; 6 distinct across both modes.
- No-edge seed failures: 0.
- Remaining seeds connected: yes.
- Retraction 1: 5 nodes, 4 edges (`co-cited`=1, `next-in-session`=3); nodes: `agent/handoff-preserves-context`, `apparatus/new-failure-class-is-a-design-defect`, `peeragogy/use-or-make`, `ukrns/publication-cadence`, `war-machine/operational-not-decorative`.
  - `next-in-session` direction evidence: `2026-08-22_2026-09-21_block008/claude-3-turn-2.json.analysis.json -> 2026-08-22_2026-09-21_block008/claude-3-turn-3.json.analysis.json`; `live/turn-l8ZcTX.json.analysis.json -> live/turn-G32zp8.json.analysis.json`; `2026-08-22_2026-09-21_block027/claude-8-turn-5.json.analysis.json -> 2026-08-22_2026-09-21_block027/claude-8-turn-9.json.analysis.json`.
- Retraction 2: 5 nodes, 4 edges (`co-cited`=1, `next-in-session`=3); nodes: `agent/handoff-preserves-context`, `apparatus/new-failure-class-is-a-design-defect`, `relationship-coherence/rupture-repair`, `ukrns/publication-cadence`, `war-machine/operational-not-decorative`.
  - `next-in-session` direction evidence: `2026-08-22_2026-09-21_block008/claude-3-turn-2.json.analysis.json -> 2026-08-22_2026-09-21_block008/claude-3-turn-3.json.analysis.json`; `live/turn-l8ZcTX.json.analysis.json -> live/turn-G32zp8.json.analysis.json`; `live/turn-AIxeop.json.analysis.json -> live/turn-AL0y9C.json.analysis.json`.
- Retraction 3: 6 nodes, 5 edges (`co-cited`=3, `next-in-session`=2); nodes: `agent/handoff-preserves-context`, `apparatus/new-failure-class-is-a-design-defect`, `peeragogy/use-or-make`, `relationship-coherence/rupture-repair`, `ukrns/publication-cadence`, `war-machine/operational-not-decorative`.
  - `next-in-session` direction evidence: `live/turn-l8ZcTX.json.analysis.json -> live/turn-G32zp8.json.analysis.json`; `live/turn-AIxeop.json.analysis.json -> live/turn-AL0y9C.json.analysis.json`.
- Rejected-pattern adjustment probes:
  - `workday/external-project-index` from query “index by concepts, documents are occurrence sites”.
  - `mmca/perceivable-target` from query “iterative loop: measure the loss, fix the worst defect class, re-run, never perfect”.
  - `snatch/re-enter-after-observed-repair` from query “iterative loop: measure the loss, fix the worst defect class, re-run, never perfect”.
  - `invariant-coherence/state-snapshot-witness` from query “quality is a property of the revision process not a single snapshot”.
- Further patterns introduced by retractions: `agent/handoff-preserves-context`, `peeragogy/use-or-make`, `relationship-coherence/rupture-repair`.
- Available arrangements now: 9 materialized policies (6 fragment-derived + 3 connected graph retractions).

### 4. M-web-arxana-ui-improvements

- Initial seeds: `hygiene/observe-the-authority`, `problems/operator-turns-become-inference-observations`.
- Initial arrangements: alternatives 6; overlap 3; 9 distinct across both modes.
- No-edge seed failures: 0.
- Remaining seeds connected: yes.
- Retraction 1: 4 nodes, 3 edges (`co-cited`=1, `rejected-beside`=1, `next-in-session`=1); nodes: `agent/student-dispatch`, `hygiene/observe-the-authority`, `orchestration/recorded-handoff`, `problems/operator-turns-become-inference-observations`.
  - `next-in-session` direction evidence: `live/turn-R4xoNZ.json.analysis.json -> 2026-09-22_2026-09-26_block005/claude-12-turn-84.json.analysis.json`.
- Retraction 2: 4 nodes, 3 edges (`co-cited`=1, `rejected-beside`=1, `next-in-session`=1); nodes: `cascades/on-the-fly-cascade`, `hygiene/observe-the-authority`, `problems/operator-turns-become-inference-observations`, `proof-search/typed-hole-as-frontier`.
  - `next-in-session` direction evidence: `2026-08-22_2026-09-21_block009/emacs-6e209ae364420eb9dc201d7ddacc321b.json.analysis.json -> 2026-08-22_2026-09-21_block009/codex-18-turn-1.json.analysis.json`.
- Retraction 3: 7 nodes, 6 edges (`why`=5, `next-in-session`=1); nodes: `cycle-machine/disruption-soak`, `cycle-machine/step-machine`, `futon-theory/durability-first`, `hygiene/observe-the-authority`, `problems/operator-turns-become-inference-observations`, `war-room/wr-16-operationalised-exploit-loops-are-first-class-observation-channels`, `war-room/wr-27-a-loop-is-born-instrumented-for-its-gain`.
  - `next-in-session` direction evidence: `live/turn-89O9ZV.json.analysis.json -> live/turn-VwDCEt.json.analysis.json`.
- Rejected-pattern adjustment probes:
  - `apparatus/model-upstream-and-coupled` from query “preserve the live shape of intent upstream of formalization”.
  - `invariant-coherence/shape-first-identify` from query “capture the raw operator voice before process hardens it into a formal statement”.
- Further patterns introduced by retractions: `agent/student-dispatch`, `cascades/on-the-fly-cascade`, `cycle-machine/disruption-soak`, `cycle-machine/step-machine`, `futon-theory/durability-first`, `orchestration/recorded-handoff`, `proof-search/typed-hole-as-frontier`, `war-room/wr-16-operationalised-exploit-loops-are-first-class-observation-channels`, `war-room/wr-27-a-loop-is-born-instrumented-for-its-gain`.
- Available arrangements now: 12 materialized policies (9 fragment-derived + 3 connected graph retractions).

### 5. M-self-documenting-stack

- Initial seeds: `coordination/intent-to-mission-binding`, `futon-theory/mission-lifecycle`, `futon-theory/mission-scoping`, `orchestration/consent-gate`, `war-machine/advanceability`.
- Initial arrangements: alternatives 1; overlap 1; 1 distinct across both modes.
- No-edge seed failures: 1 — `coordination/intent-to-mission-binding`.
- Remaining seeds connected: yes.
- Retraction 1: 5 nodes, 4 edges (`co-cited`=1, `rejected-beside`=1, `next-in-session`=2); nodes: `coordination/assignment-binding`, `futon-theory/mission-lifecycle`, `futon-theory/mission-scoping`, `orchestration/consent-gate`, `war-machine/advanceability`.
  - `next-in-session` direction evidence: `live/turn-1QzzO5.json.analysis.json -> live/turn-9umMUN.json.analysis.json`; `live/turn-vttPhQ.json.analysis.json -> live/turn-0eoQWs.json.analysis.json`.
- Retraction 2: 5 nodes, 4 edges (`co-cited`=1, `next-in-session`=3); nodes: `futon-theory/mission-lifecycle`, `futon-theory/mission-scoping`, `orchestration/consent-gate`, `peeragogy/use-or-make`, `war-machine/advanceability`.
  - `next-in-session` direction evidence: `live/turn-vttPhQ.json.analysis.json -> live/turn-0eoQWs.json.analysis.json`; `live/turn-mRXZUN.json.analysis.json -> live/turn-sguiyy.json.analysis.json`; `live/turn-1GNPpj.json.analysis.json -> live/turn-F8sjMa.json.analysis.json`.
- Retraction 3: 5 nodes, 4 edges (`co-cited`=1, `rejected-beside`=1, `next-in-session`=2); nodes: `futon-theory/mission-lifecycle`, `futon-theory/mission-scoping`, `orchestration/consent-gate`, `social/explicit-exit-over-abandonment`, `war-machine/advanceability`.
  - `next-in-session` direction evidence: `live/turn-vttPhQ.json.analysis.json -> live/turn-0eoQWs.json.analysis.json`; `live/turn-7o7zDa.json.analysis.json -> live/turn-5PJcyp.json.analysis.json`.
- Rejected-pattern adjustment probes:
  - `war-machine/advanceability` from query “the previous cycle worked but exposed the next gap”.
  - `data-mining/smoke-before-the-paid-run` from query “recovery pass after external review surfaced blockers”.
- Further patterns introduced by retractions: `coordination/assignment-binding`, `peeragogy/use-or-make`, `social/explicit-exit-over-abandonment`.
- Available arrangements now: 4 materialized policies (1 fragment-derived + 3 connected graph retractions).

### 6. M-war-machine-aif-completion

- Initial seeds: `cascade-construction/read-what-exists-first`, `enrichment/extend-not-rewrite`, `futon-theory/mission-dependency`, `problems/r4-forward-model`, `problems/r6-candidate-action-space-and-selection`, `structure/whose-question-is-this`, `system-coherence/bind-open-questions-to-closure-mechanisms`, `war-room/wr-11-external-applications-carry-predecessor-exemplar-relationships`, `war-room/wr-15-head-as-escrow-is-a-sanctioned-pattern`, `war-room/wr-16-operationalised-exploit-loops-are-first-class-observation-channels`.
- Initial arrangements: alternatives 16; overlap 8; 24 distinct across both modes.
- No-edge seed failures: 1 — `war-room/wr-15-head-as-escrow-is-a-sanctioned-pattern`.
- Remaining seeds connected: yes.
- Retraction 1: 17 nodes, 16 edges (`why`=5, `co-cited`=3, `rejected-beside`=4, `next-in-session`=4); nodes: `aif/candidate-pattern-action-space`, `cascade-construction/hand-over-when-acting-is-worth-more`, `cascade-construction/read-what-exists-first`, `enrichment/extend-not-rewrite`, `exotic/immutable-vision-mutable-plan`, `futon-theory/mission-dependency`, `peeragogy/use-or-make`, `peripherals/read-existing-seam-before-implementing`, `problems/r4-forward-model`, `problems/r6-candidate-action-space-and-selection`, `structure/whose-question-is-this`, `system-coherence/bind-open-questions-to-closure-mechanisms`, `war-room/wr-0-organise-without-apparatus`, `war-room/wr-11-external-applications-carry-predecessor-exemplar-relationships`, `war-room/wr-16-operationalised-exploit-loops-are-first-class-observation-channels`, `war-room/wr-19-tension-must-generate-not-only-rank`, `war-room/wr-4-inhabit-before-building`.
  - `next-in-session` direction evidence: `2026-08-22_2026-09-21_block027/codex-13-turn-8.json.analysis.json -> 2026-08-22_2026-09-21_block027/codex-13-turn-13.json.analysis.json`; `2026-08-22_2026-09-21_block029/claude-4-turn-268.json.analysis.json -> 2026-08-22_2026-09-21_block029/claude-4-turn-269.json.analysis.json`; `live/turn-rWbYPW.json.analysis.json -> live/turn-5wo21f.json.analysis.json`; `2026-08-22_2026-09-21_block023/claude-15-turn-168.json.analysis.json -> 2026-08-22_2026-09-21_block024/claude-15-turn-195.json.analysis.json`.
- Retraction 2: 17 nodes, 16 edges (`why`=5, `co-cited`=3, `rejected-beside`=4, `next-in-session`=4); nodes: `aif/candidate-pattern-action-space`, `cascade-construction/read-what-exists-first`, `enrichment/extend-not-rewrite`, `exotic/immutable-vision-mutable-plan`, `futon-theory/mission-dependency`, `orchestration/recorded-handoff`, `peeragogy/use-or-make`, `peripherals/read-existing-seam-before-implementing`, `problems/r4-forward-model`, `problems/r6-candidate-action-space-and-selection`, `structure/whose-question-is-this`, `system-coherence/bind-open-questions-to-closure-mechanisms`, `war-room/wr-0-organise-without-apparatus`, `war-room/wr-11-external-applications-carry-predecessor-exemplar-relationships`, `war-room/wr-16-operationalised-exploit-loops-are-first-class-observation-channels`, `war-room/wr-19-tension-must-generate-not-only-rank`, `war-room/wr-4-inhabit-before-building`.
  - `next-in-session` direction evidence: `2026-08-22_2026-09-21_block029/claude-4-turn-268.json.analysis.json -> 2026-08-22_2026-09-21_block029/claude-4-turn-269.json.analysis.json`; `live/turn-rWbYPW.json.analysis.json -> live/turn-5wo21f.json.analysis.json`; `2026-08-22_2026-09-21_block023/claude-15-turn-168.json.analysis.json -> 2026-08-22_2026-09-21_block024/claude-15-turn-195.json.analysis.json`; `2026-08-22_2026-09-21_block026/codex-13-turn-6.json.analysis.json -> 2026-08-22_2026-09-21_block027/codex-13-turn-8.json.analysis.json`.
- Retraction 3: 19 nodes, 18 edges (`why`=7, `co-cited`=3, `rejected-beside`=4, `next-in-session`=4); nodes: `aif/candidate-pattern-action-space`, `cascade-construction/hand-over-when-acting-is-worth-more`, `cascade-construction/read-what-exists-first`, `enrichment/extend-not-rewrite`, `exotic/immutable-vision-mutable-plan`, `futon-theory/mission-dependency`, `peeragogy/use-or-make`, `peripherals/read-existing-seam-before-implementing`, `problems/r4-forward-model`, `problems/r6-candidate-action-space-and-selection`, `process/built-but-not-wired-invisibility`, `structure/whose-question-is-this`, `system-coherence/bind-open-questions-to-closure-mechanisms`, `war-room/wr-0-organise-without-apparatus`, `war-room/wr-11-external-applications-carry-predecessor-exemplar-relationships`, `war-room/wr-16-operationalised-exploit-loops-are-first-class-observation-channels`, `war-room/wr-19-tension-must-generate-not-only-rank`, `war-room/wr-27-a-loop-is-born-instrumented-for-its-gain`, `war-room/wr-4-inhabit-before-building`.
  - `next-in-session` direction evidence: `2026-08-22_2026-09-21_block027/codex-13-turn-8.json.analysis.json -> 2026-08-22_2026-09-21_block027/codex-13-turn-13.json.analysis.json`; `2026-08-22_2026-09-21_block029/claude-4-turn-268.json.analysis.json -> 2026-08-22_2026-09-21_block029/claude-4-turn-269.json.analysis.json`; `live/turn-rWbYPW.json.analysis.json -> live/turn-5wo21f.json.analysis.json`; `2026-08-22_2026-09-21_block023/claude-15-turn-168.json.analysis.json -> 2026-08-22_2026-09-21_block024/claude-15-turn-195.json.analysis.json`.
- Rejected-pattern adjustment probes:
  - `war-room/wr-5-war-machine-is-not-a-mission` from query “mission header declares status, owner, timebox and exit criterion”.
  - `capability/hinge-capability-extraction` from query “header marks the text as machine-written scaffold, distinct from operator words”.
  - `process-coherence/status-refresh-before-work` from query “exit criterion satisfied claim in mission status line”.
  - `war-room/wr-25-good-news-gets-the-same-evidence-discipline-as-bad` from query “exit criterion satisfied claim in mission status line”.
  - `musn/declare-scope` from query “mission header declares status, owner, timebox and exit criterion”.
  - `war-room/wr-18-war-machine-is-demonstrated-not-hypothesised` from query “current state is a reading surface with no forward model, no belief, no action ranking”.
  - `control/effort-estimation` from query “estimate effort in weeks with the final cap left to the operator”.
  - `problems/r1-belief-state` from query “completeness contract enumerating required parts of an active inference agent”.
  - `pattern-discipline/pattern-to-code-receipts` from query “operationalised exploit loops as observation channels”.
  - `problems/r1-belief-state` from query “current state is a reading surface with no forward model, no belief, no action ranking”.
  - `war-machine/half-blind-observation` from query “sixteen channel portfolio observation surface”.
  - `war-room/wr-12-essay-health-is-the-aif-observation-channel` from query “belief updates typed rewrites operadic by construction”.
  - `capability/hinge-capability-extraction` from query “authors listed with pending verbatim answers from the operator”.
- Further patterns introduced by retractions: `aif/candidate-pattern-action-space`, `cascade-construction/hand-over-when-acting-is-worth-more`, `exotic/immutable-vision-mutable-plan`, `orchestration/recorded-handoff`, `peeragogy/use-or-make`, `peripherals/read-existing-seam-before-implementing`, `process/built-but-not-wired-invisibility`, `war-room/wr-0-organise-without-apparatus`, `war-room/wr-19-tension-must-generate-not-only-rank`, `war-room/wr-27-a-loop-is-born-instrumented-for-its-gain`, `war-room/wr-4-inhabit-before-building`.
- Available arrangements now: 27 materialized policies (24 fragment-derived + 3 connected graph retractions).

### 7. M-essays-diachronic-model

- Initial seeds: `futon-theory/mission-lifecycle`, `sidecar/fact-lifecycle-event-types`.
- Initial arrangements: alternatives 4; overlap 4; 4 distinct across both modes.
- No-edge seed failures: 1 — `sidecar/fact-lifecycle-event-types`.
- Remaining seeds connected: yes.
- Retraction 1: 1 nodes, 0 edges (none); nodes: `futon-theory/mission-lifecycle`.
- Rejected-pattern adjustment probes:
  - `data-mining/golden-is-curated-not-raw` from query “golden/live dataset discipline with lifecycle event semantics”.
  - `process-coherence/status-refresh-before-work` from query “mission status: specified, implementation not started”.
  - `apparatus/one-authority-per-question` from query “assign mission ownership to responsible components”.
- Further patterns introduced by retractions: none.
- Available arrangements now: 5 materialized policies (4 fragment-derived + 1 connected graph retractions).

### 8. M-value-creation-loop

- Initial seeds: `coordination/artifact-registration`, `hygiene/route-to-who-can-act`, `problems/operator-turns-become-inference-observations`.
- Initial arrangements: alternatives 2; overlap 2; 2 distinct across both modes.
- No-edge seed failures: 0.
- Remaining seeds connected: yes.
- Retraction 1: 5 nodes, 4 edges (`rejected-beside`=1, `next-in-session`=3); nodes: `coordination/artifact-registration`, `hygiene/route-to-who-can-act`, `inbox-zero/escalate-by-who-can-act`, `problems/operator-turns-become-inference-observations`, `system-coherence/turn-design-into-checks`.
  - `next-in-session` direction evidence: `2026-08-22_2026-09-21_block014/claude-15-turn-124.json.analysis.json -> 2026-08-22_2026-09-21_block014/claude-15-turn-126.json.analysis.json`; `2026-08-22_2026-09-21_block004/claude-13-turn-283.json.analysis.json -> 2026-08-22_2026-09-21_block004/claude-13-turn-285.json.analysis.json`; `2026-09-22_2026-09-26_block005/claude-12-turn-84.json.analysis.json -> live/turn-mJnSVt.json.analysis.json`; `live/turn-R4xoNZ.json.analysis.json -> 2026-09-22_2026-09-26_block005/claude-12-turn-84.json.analysis.json`.
- Retraction 2: 5 nodes, 4 edges (`rejected-beside`=1, `next-in-session`=3); nodes: `coordination/artifact-registration`, `hygiene/route-to-who-can-act`, `orchestration/recorded-handoff`, `problems/operator-turns-become-inference-observations`, `system-coherence/turn-design-into-checks`.
  - `next-in-session` direction evidence: `2026-08-22_2026-09-21_block014/claude-15-turn-124.json.analysis.json -> 2026-08-22_2026-09-21_block014/claude-15-turn-126.json.analysis.json`; `2026-08-22_2026-09-21_block004/claude-13-turn-283.json.analysis.json -> 2026-08-22_2026-09-21_block004/claude-13-turn-285.json.analysis.json`; `live/turn-R4xoNZ.json.analysis.json -> 2026-09-22_2026-09-26_block005/claude-12-turn-84.json.analysis.json`.
- Retraction 3: 7 nodes, 6 edges (`why`=3, `next-in-session`=3); nodes: `coordination/artifact-registration`, `hygiene/route-to-who-can-act`, `problems/operator-turns-become-inference-observations`, `process/built-but-not-wired-invisibility`, `system-coherence/turn-design-into-checks`, `war-room/wr-16-operationalised-exploit-loops-are-first-class-observation-channels`, `war-room/wr-27-a-loop-is-born-instrumented-for-its-gain`.
  - `next-in-session` direction evidence: `2026-08-22_2026-09-21_block014/claude-15-turn-124.json.analysis.json -> 2026-08-22_2026-09-21_block014/claude-15-turn-126.json.analysis.json`; `2026-08-22_2026-09-21_block004/claude-13-turn-278.json.analysis.json -> 2026-08-22_2026-09-21_block004/claude-13-turn-283.json.analysis.json`; `2026-08-22_2026-09-21_block004/claude-13-turn-283.json.analysis.json -> 2026-08-22_2026-09-21_block004/claude-13-turn-285.json.analysis.json`.
- Rejected-pattern adjustment probes:
  - `cascades/on-the-fly-cascade` from query “prospective pattern cascade as the strategy for building”.
  - `inbox-zero/promote-at-turn-end` from query “mint a mission from a live design conversation while it is still warm”.
- Further patterns introduced by retractions: `inbox-zero/escalate-by-who-can-act`, `orchestration/recorded-handoff`, `process/built-but-not-wired-invisibility`, `system-coherence/turn-design-into-checks`, `war-room/wr-16-operationalised-exploit-loops-are-first-class-observation-channels`, `war-room/wr-27-a-loop-is-born-instrumented-for-its-gain`.
- Available arrangements now: 5 materialized policies (2 fragment-derived + 3 connected graph retractions).

## Computing G over these policies

`futon2.aif.cascade-observation-scoring/rank-cascade-actions` is the existing family scorer (`src/futon2/aif/cascade_observation_scoring.clj:155-190`). For each candidate, `score-candidate` rolls its `:precedence` through the real model at every horizon step, queries the observation model, sums per-step `:g`, and records `:G-efe`, `:G-cascade`, and `[:certificate :consumed-g]` (`:91-150`). To score the HEAD-derived arrangements, the next wiring must turn every arranged node into the pattern’s documented transition interpretation, supply the initial belief, observation model, horizon, and preference schedule, and preserve the arrangement as candidate precedence/topology. Current admission rejects a candidate when any node lacks an interpretation or receipt (`src/futon2/aif/wm/cascade_decision.clj:1230-1281`). Joe delegated how the post-selection target-specific interpretation updates that documented meaning; this packet therefore does not compute G or manufacture missing transition operators.

## Failure counts and artifacts

- One mission (`M-metric-harness`) has remaining seeds in several graph components and therefore produces zero connected retractions.
- Six seeds across five readings are refused because they have no graph edges.
- No mission produced zero initial cascades.
- Every request, validated analysis, current retraction result, and the pinned five-kind graph is under `holes/labs/wm-contract/mission-head-cascades-2026-09-30/`.
