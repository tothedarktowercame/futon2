# Initial mission HEAD cascades — 2026-09-30

## Method

The request source is the complete `## HEAD` section when present. Otherwise it is the opening from the title through the byte before the first level-two heading. `scripts/wm_task_reading.py --mission-head` records the choice as `task.source_kind`, plus canonical path and content SHA-256, and emits the unchanged `session_turn_analysis.py` schema. Eight requests were read by the 象 pool and completed with the unchanged validator. Zero patterns and zero connected retractions are reported as failures.

Selection rule: enumerate tracked `M-*.md` mission files in canonical repositories whose top-level path is one of the Futon repositories from futon0 through futon7 (including their canonical lettered components), exclude dispositions that begin closed, superseded, abandoned, complete, finished, archived, or cancelled, sort by the file’s latest commit time with repository/path as tie-break, and take the four newest with an exact `## HEAD` plus the four newest without one.

The pattern graph was generated once by `futon3c/scripts/mined_pattern_graph.py`, then passed by path to every `pattern_retraction.py --k 3` call. Committed graph file SHA-256: `30325e476fe25638415de375bab505f1afd0710fbad0684008ffd0cc7899084d`; 1,431 patterns, 4,654 analysis records. A seed absent from the graph’s edge endpoints is reported and omitted before retraction. If the remaining seeds occupy several components, the connected-retraction count is zero.

Joe has not decided whether `co-rejected` and `next-in-session` may connect
the graph. Every default result below records `weak_edges`; all 22 returned
retractions have `weak_edges=0`. I also reran each seed set with both weak
kinds weighted 100,000. Seven sets remained connected without using a weak
edge; `M-metric-harness` remained disconnected. The high-cost outputs are
committed as `*.strong-retractions.json`, so this conclusion remains
checkable if the default changes.

“Materialized policy set” below is the number of distinct arrangements produced across alternatives and overlap modes plus graph retractions. Rejected patterns are listed as possible adjustment probes but are not counted as policies because 象 explicitly rejected them; admitting one requires a later reading.

## Results

| # | mission / source | fragments / accepted seeds | initial alternatives / overlap | retractions | materialized policies | refused seeds | seat time |
|---:|---|---:|---:|---:|---:|---|---:|
| 1 | `M-象-2000` / HEAD | 17 / 9 | 8 / 4 | 3 | 15 | `futon-theory/rapid-debugging`, `pattern-interpretation/cite-the-source-bytes` | 623.0s |
| 2 | `M-metric-harness` / HEAD | 26 / 10 | 4 / 4 | 0 | 4 | `plos-npt-with-small-n/research-questions-stated-early` | 485.5s |
| 3 | `M-distributed-proofreaders` / HEAD | 8 / 3 | 6 / 6 | 3 | 9 | none | 155.7s |
| 4 | `M-web-arxana-ui-improvements` / HEAD | 2 / 2 | 6 / 3 | 3 | 12 | none | 92.6s |
| 5 | `M-self-documenting-stack` / opening | 16 / 5 | 1 / 1 | 3 | 4 | `coordination/intent-to-mission-binding` | 168.9s |
| 6 | `M-war-machine-aif-completion` / opening | 38 / 10 | 16 / 8 | 3 | 27 | `war-room/wr-15-head-as-escrow-is-a-sanctioned-pattern` | 209.1s |
| 7 | `M-essays-diachronic-model` / opening | 4 / 2 | 4 / 4 | 1 | 5 | `sidecar/fact-lifecycle-event-types` | 75.3s |
| 8 | `M-value-creation-loop` / opening | 7 / 3 | 2 / 2 | 3 | 5 | none | 152.1s |

### Retraction sensitivity and timing

Times include starting Python and loading the pinned graph. “Near” means two
default k=3 results share every edge of the smaller tree except at most one.

| mission | default / high-weak-cost time | connected without weak links | default/high weak-edge counts | near result pairs |
|---|---:|---|---|---|
| M-象-2000 | 1.519s / 2.949s | yes | 0,0,0 / 0,0,0 | ranks 2–3 share 13/14 smaller-tree edges |
| M-metric-harness | 0.051s / 0.050s | **no** | none / none | none |
| M-distributed-proofreaders | 0.194s / 0.247s | yes | 0,0,0 / 0,0,0 | ranks 2–3 share 3/4 smaller-tree edges |
| M-web-arxana-ui-improvements | 0.114s / 0.220s | yes | 0,0,0 / 0,0,0 | none |
| M-self-documenting-stack | 0.130s / 0.280s | yes | 0,0,0 / 0,0,0 | none |
| M-war-machine-aif-completion | **4.729s** / **3.866s** | yes | 0,0,0 / 0,0,0 | ranks 1–3 share 15/16 smaller-tree edges |
| M-essays-diachronic-model | 0.050s / 0.050s | yes | 0 / 0 | only one result |
| M-value-creation-loop | 0.124s / 0.224s | yes | 0,0,0 / 0,0,0 | none |

The 10-seed `M-war-machine-aif-completion` call is the slow case and its
rank 1/rank 3 trees are near-identical. The 7 usable-seed M-象-2000 and
3-seed M-distributed-proofreaders sets also have a near-identical pair. Their
complete seed sets are recorded in their sections below for the retraction
owner.

### 1. M-象-2000

- Initial seeds: `futon-theory/rapid-debugging`, `invariant-coherence/protocol-family-naming`, `math-strategy/corpus-trust-protocol`, `pattern-coherence/causal-clarity`, `pattern-interpretation/cite-the-source-bytes`, `peripherals/read-existing-seam-before-implementing`, `象/双时并记`, `象/视图出于史`, `象/象不忘`.
- Initial arrangements: alternatives 8; overlap 4; 12 distinct across both modes.
- Refused graph seeds (no edges): `futon-theory/rapid-debugging`, `pattern-interpretation/cite-the-source-bytes`.
- Retraction 1: 13 nodes, 12 edges, kinds `co-cited`, `how`, `next-in-session`, `why`, `weak_edges=0`; nodes: `agent/handoff-preserves-context`, `cascade-construction/add-a-pattern-when-an-item-fits-no-class`, `invariant-coherence/protocol-family-naming`, `math-strategy/corpus-trust-protocol`, `orchestration/consent-gate`, `pattern-coherence/causal-clarity`, `peripherals/read-existing-seam-before-implementing`, `translation/bind-to-the-source`, `workshop/open-proposals-named-adoption`, `象/双时并记`, `象/翻译契约`, `象/视图出于史`, `象/象不忘`.
- Retraction 2: 15 nodes, 14 edges, kinds `co-cited`, `how`, `next-in-session`, `why`, `weak_edges=0`; nodes: `agent/state-is-hypothesis`, `cascade-construction/add-a-pattern-when-an-item-fits-no-class`, `invariant-coherence/protocol-family-naming`, `math-strategy/corpus-trust-protocol`, `orchestration/consent-gate`, `pattern-coherence/causal-clarity`, `peripherals/read-existing-seam-before-implementing`, `translation/bind-to-the-source`, `translation/choose-the-equivalence-check`, `translation/revise-the-source-under-pressure`, `workshop/open-proposals-named-adoption`, `象/双时并记`, `象/翻译契约`, `象/视图出于史`, `象/象不忘`.
- Retraction 3: 16 nodes, 15 edges, kinds `co-cited`, `how`, `next-in-session`, `why`, `weak_edges=0`; nodes: `agent/state-is-hypothesis`, `cascade-construction/add-a-pattern-when-an-item-fits-no-class`, `invariant-coherence/protocol-family-naming`, `math-strategy/corpus-trust-protocol`, `orchestration/consent-gate`, `pattern-coherence/causal-clarity`, `peripherals/read-existing-seam-before-implementing`, `proof-search/typed-hole-as-frontier`, `translation/bind-to-the-source`, `translation/choose-the-equivalence-check`, `translation/revise-the-source-under-pressure`, `workshop/open-proposals-named-adoption`, `象/双时并记`, `象/翻译契约`, `象/视图出于史`, `象/象不忘`.
- Rejected-pattern adjustment probes:
  - `invariant-coherence/state-snapshot-witness` from query “as-of database rewind time travel history”.
  - `enrichment/rational-reconstruction` from query “Elephant 2000 McCarthy as-of bitemporal”.
- Further patterns introduced by retractions: `agent/handoff-preserves-context`, `agent/state-is-hypothesis`, `cascade-construction/add-a-pattern-when-an-item-fits-no-class`, `orchestration/consent-gate`, `proof-search/typed-hole-as-frontier`, `translation/bind-to-the-source`, `translation/choose-the-equivalence-check`, `translation/revise-the-source-under-pressure`, `workshop/open-proposals-named-adoption`, `象/翻译契约`.
- Available arrangements now: 15 materialized policies (12 fragment-derived arrangements + 3 connected graph retractions); rejected probes can enlarge this only after a new reading admits them.

### 2. M-metric-harness

- Initial seeds: `data-mining/smoke-before-the-paid-run`, `futon-theory/honest-map-over-flattering-counter`, `futon-theory/progress-signal`, `measurement/error-needs-structure-to-teach`, `musn/declare-scope`, `plos-npt-with-small-n/absence-as-evidence`, `plos-npt-with-small-n/research-questions-stated-early`, `system-coherence/facet-before-aggregating`, `vsatelier/decision-provenance`, `writing-coherence/hedged-lift`.
- Initial arrangements: alternatives 4; overlap 4; 4 distinct across both modes.
- Refused graph seeds (no edges): `plos-npt-with-small-n/research-questions-stated-early`.
- Retractions: **0 (failure)**; the usable seeds are split across graph components.
- Rejected-pattern adjustment probes:
  - `futon-theory/counter-ratchet` from query “counter ratchet only moves one way”.
  - `process-coherence/stuck-means-signal` from query “each repetition should measurably improve; if it does not, find out why”.
  - `data-mining/smoke-before-the-paid-run` from query “substrate should compound; held-out case grounds better as corpus grows”.
  - `writing-coherence/citation-density-load-bearing-claim` from query “cite the source of a lesson being generalized”.
- Further patterns introduced by retractions: none.
- Available arrangements now: 4 materialized policies (4 fragment-derived arrangements + 0 connected graph retractions); rejected probes can enlarge this only after a new reading admits them.

### 3. M-distributed-proofreaders

- Initial seeds: `apparatus/new-failure-class-is-a-design-defect`, `ukrns/publication-cadence`, `war-machine/operational-not-decorative`.
- Initial arrangements: alternatives 6; overlap 6; 6 distinct across both modes.
- Retraction 1: 5 nodes, 4 edges, kinds `co-cited`, `next-in-session`, `weak_edges=0`; nodes: `agent/handoff-preserves-context`, `apparatus/new-failure-class-is-a-design-defect`, `peeragogy/use-or-make`, `ukrns/publication-cadence`, `war-machine/operational-not-decorative`.
- Retraction 2: 5 nodes, 4 edges, kinds `co-cited`, `next-in-session`, `weak_edges=0`; nodes: `agent/handoff-preserves-context`, `apparatus/new-failure-class-is-a-design-defect`, `relationship-coherence/rupture-repair`, `ukrns/publication-cadence`, `war-machine/operational-not-decorative`.
- Retraction 3: 6 nodes, 5 edges, kinds `co-cited`, `next-in-session`, `weak_edges=0`; nodes: `agent/handoff-preserves-context`, `apparatus/new-failure-class-is-a-design-defect`, `peeragogy/use-or-make`, `relationship-coherence/rupture-repair`, `ukrns/publication-cadence`, `war-machine/operational-not-decorative`.
- Rejected-pattern adjustment probes:
  - `workday/external-project-index` from query “index by concepts, documents are occurrence sites”.
  - `mmca/perceivable-target` from query “iterative loop: measure the loss, fix the worst defect class, re-run, never perfect”.
  - `snatch/re-enter-after-observed-repair` from query “iterative loop: measure the loss, fix the worst defect class, re-run, never perfect”.
  - `invariant-coherence/state-snapshot-witness` from query “quality is a property of the revision process not a single snapshot”.
- Further patterns introduced by retractions: `agent/handoff-preserves-context`, `peeragogy/use-or-make`, `relationship-coherence/rupture-repair`.
- Available arrangements now: 9 materialized policies (6 fragment-derived arrangements + 3 connected graph retractions); rejected probes can enlarge this only after a new reading admits them.

### 4. M-web-arxana-ui-improvements

- Initial seeds: `hygiene/observe-the-authority`, `problems/operator-turns-become-inference-observations`.
- Initial arrangements: alternatives 6; overlap 3; 9 distinct across both modes.
- Retraction 1: 4 nodes, 3 edges, kinds `co-cited`, `next-in-session`, `rejected-beside`, `weak_edges=0`; nodes: `agent/student-dispatch`, `hygiene/observe-the-authority`, `orchestration/recorded-handoff`, `problems/operator-turns-become-inference-observations`.
- Retraction 2: 4 nodes, 3 edges, kinds `co-cited`, `next-in-session`, `rejected-beside`, `weak_edges=0`; nodes: `cascades/on-the-fly-cascade`, `hygiene/observe-the-authority`, `problems/operator-turns-become-inference-observations`, `proof-search/typed-hole-as-frontier`.
- Retraction 3: 7 nodes, 6 edges, kinds `next-in-session`, `why`, `weak_edges=0`; nodes: `cycle-machine/disruption-soak`, `cycle-machine/step-machine`, `futon-theory/durability-first`, `hygiene/observe-the-authority`, `problems/operator-turns-become-inference-observations`, `war-room/wr-16-operationalised-exploit-loops-are-first-class-observation-channels`, `war-room/wr-27-a-loop-is-born-instrumented-for-its-gain`.
- Rejected-pattern adjustment probes:
  - `apparatus/model-upstream-and-coupled` from query “preserve the live shape of intent upstream of formalization”.
  - `invariant-coherence/shape-first-identify` from query “capture the raw operator voice before process hardens it into a formal statement”.
- Further patterns introduced by retractions: `agent/student-dispatch`, `cascades/on-the-fly-cascade`, `cycle-machine/disruption-soak`, `cycle-machine/step-machine`, `futon-theory/durability-first`, `orchestration/recorded-handoff`, `proof-search/typed-hole-as-frontier`, `war-room/wr-16-operationalised-exploit-loops-are-first-class-observation-channels`, `war-room/wr-27-a-loop-is-born-instrumented-for-its-gain`.
- Available arrangements now: 12 materialized policies (9 fragment-derived arrangements + 3 connected graph retractions); rejected probes can enlarge this only after a new reading admits them.

### 5. M-self-documenting-stack

- Initial seeds: `coordination/intent-to-mission-binding`, `futon-theory/mission-lifecycle`, `futon-theory/mission-scoping`, `orchestration/consent-gate`, `war-machine/advanceability`.
- Initial arrangements: alternatives 1; overlap 1; 1 distinct across both modes.
- Refused graph seeds (no edges): `coordination/intent-to-mission-binding`.
- Retraction 1: 5 nodes, 4 edges, kinds `co-cited`, `next-in-session`, `rejected-beside`, `weak_edges=0`; nodes: `coordination/assignment-binding`, `futon-theory/mission-lifecycle`, `futon-theory/mission-scoping`, `orchestration/consent-gate`, `war-machine/advanceability`.
- Retraction 2: 5 nodes, 4 edges, kinds `co-cited`, `next-in-session`, `weak_edges=0`; nodes: `futon-theory/mission-lifecycle`, `futon-theory/mission-scoping`, `orchestration/consent-gate`, `peeragogy/use-or-make`, `war-machine/advanceability`.
- Retraction 3: 5 nodes, 4 edges, kinds `co-cited`, `next-in-session`, `rejected-beside`, `weak_edges=0`; nodes: `futon-theory/mission-lifecycle`, `futon-theory/mission-scoping`, `orchestration/consent-gate`, `social/explicit-exit-over-abandonment`, `war-machine/advanceability`.
- Rejected-pattern adjustment probes:
  - `war-machine/advanceability` from query “the previous cycle worked but exposed the next gap”.
  - `data-mining/smoke-before-the-paid-run` from query “recovery pass after external review surfaced blockers”.
- Further patterns introduced by retractions: `coordination/assignment-binding`, `peeragogy/use-or-make`, `social/explicit-exit-over-abandonment`.
- Available arrangements now: 4 materialized policies (1 fragment-derived arrangements + 3 connected graph retractions); rejected probes can enlarge this only after a new reading admits them.

### 6. M-war-machine-aif-completion

- Initial seeds: `cascade-construction/read-what-exists-first`, `enrichment/extend-not-rewrite`, `futon-theory/mission-dependency`, `problems/r4-forward-model`, `problems/r6-candidate-action-space-and-selection`, `structure/whose-question-is-this`, `system-coherence/bind-open-questions-to-closure-mechanisms`, `war-room/wr-11-external-applications-carry-predecessor-exemplar-relationships`, `war-room/wr-15-head-as-escrow-is-a-sanctioned-pattern`, `war-room/wr-16-operationalised-exploit-loops-are-first-class-observation-channels`.
- Initial arrangements: alternatives 16; overlap 8; 24 distinct across both modes.
- Refused graph seeds (no edges): `war-room/wr-15-head-as-escrow-is-a-sanctioned-pattern`.
- Retraction 1: 17 nodes, 16 edges, kinds `co-cited`, `next-in-session`, `rejected-beside`, `why`, `weak_edges=0`; nodes: `aif/candidate-pattern-action-space`, `cascade-construction/hand-over-when-acting-is-worth-more`, `cascade-construction/read-what-exists-first`, `enrichment/extend-not-rewrite`, `exotic/immutable-vision-mutable-plan`, `futon-theory/mission-dependency`, `peeragogy/use-or-make`, `peripherals/read-existing-seam-before-implementing`, `problems/r4-forward-model`, `problems/r6-candidate-action-space-and-selection`, `structure/whose-question-is-this`, `system-coherence/bind-open-questions-to-closure-mechanisms`, `war-room/wr-0-organise-without-apparatus`, `war-room/wr-11-external-applications-carry-predecessor-exemplar-relationships`, `war-room/wr-16-operationalised-exploit-loops-are-first-class-observation-channels`, `war-room/wr-19-tension-must-generate-not-only-rank`, `war-room/wr-4-inhabit-before-building`.
- Retraction 2: 17 nodes, 16 edges, kinds `co-cited`, `next-in-session`, `rejected-beside`, `why`, `weak_edges=0`; nodes: `aif/candidate-pattern-action-space`, `cascade-construction/read-what-exists-first`, `enrichment/extend-not-rewrite`, `exotic/immutable-vision-mutable-plan`, `futon-theory/mission-dependency`, `orchestration/recorded-handoff`, `peeragogy/use-or-make`, `peripherals/read-existing-seam-before-implementing`, `problems/r4-forward-model`, `problems/r6-candidate-action-space-and-selection`, `structure/whose-question-is-this`, `system-coherence/bind-open-questions-to-closure-mechanisms`, `war-room/wr-0-organise-without-apparatus`, `war-room/wr-11-external-applications-carry-predecessor-exemplar-relationships`, `war-room/wr-16-operationalised-exploit-loops-are-first-class-observation-channels`, `war-room/wr-19-tension-must-generate-not-only-rank`, `war-room/wr-4-inhabit-before-building`.
- Retraction 3: 19 nodes, 18 edges, kinds `co-cited`, `next-in-session`, `rejected-beside`, `why`, `weak_edges=0`; nodes: `aif/candidate-pattern-action-space`, `cascade-construction/hand-over-when-acting-is-worth-more`, `cascade-construction/read-what-exists-first`, `enrichment/extend-not-rewrite`, `exotic/immutable-vision-mutable-plan`, `futon-theory/mission-dependency`, `peeragogy/use-or-make`, `peripherals/read-existing-seam-before-implementing`, `problems/r4-forward-model`, `problems/r6-candidate-action-space-and-selection`, `process/built-but-not-wired-invisibility`, `structure/whose-question-is-this`, `system-coherence/bind-open-questions-to-closure-mechanisms`, `war-room/wr-0-organise-without-apparatus`, `war-room/wr-11-external-applications-carry-predecessor-exemplar-relationships`, `war-room/wr-16-operationalised-exploit-loops-are-first-class-observation-channels`, `war-room/wr-19-tension-must-generate-not-only-rank`, `war-room/wr-27-a-loop-is-born-instrumented-for-its-gain`, `war-room/wr-4-inhabit-before-building`.
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
- Available arrangements now: 27 materialized policies (24 fragment-derived arrangements + 3 connected graph retractions); rejected probes can enlarge this only after a new reading admits them.

### 7. M-essays-diachronic-model

- Initial seeds: `futon-theory/mission-lifecycle`, `sidecar/fact-lifecycle-event-types`.
- Initial arrangements: alternatives 4; overlap 4; 4 distinct across both modes.
- Refused graph seeds (no edges): `sidecar/fact-lifecycle-event-types`.
- Retraction 1: 1 nodes, 0 edges, kinds none, `weak_edges=0`; nodes: `futon-theory/mission-lifecycle`.
- Rejected-pattern adjustment probes:
  - `data-mining/golden-is-curated-not-raw` from query “golden/live dataset discipline with lifecycle event semantics”.
  - `process-coherence/status-refresh-before-work` from query “mission status: specified, implementation not started”.
  - `apparatus/one-authority-per-question` from query “assign mission ownership to responsible components”.
- Further patterns introduced by retractions: none.
- Available arrangements now: 5 materialized policies (4 fragment-derived arrangements + 1 connected graph retractions); rejected probes can enlarge this only after a new reading admits them.

### 8. M-value-creation-loop

- Initial seeds: `coordination/artifact-registration`, `hygiene/route-to-who-can-act`, `problems/operator-turns-become-inference-observations`.
- Initial arrangements: alternatives 2; overlap 2; 2 distinct across both modes.
- Retraction 1: 5 nodes, 4 edges, kinds `next-in-session`, `rejected-beside`, `weak_edges=0`; nodes: `coordination/artifact-registration`, `hygiene/route-to-who-can-act`, `inbox-zero/escalate-by-who-can-act`, `problems/operator-turns-become-inference-observations`, `system-coherence/turn-design-into-checks`.
- Retraction 2: 5 nodes, 4 edges, kinds `next-in-session`, `rejected-beside`, `weak_edges=0`; nodes: `coordination/artifact-registration`, `hygiene/route-to-who-can-act`, `orchestration/recorded-handoff`, `problems/operator-turns-become-inference-observations`, `system-coherence/turn-design-into-checks`.
- Retraction 3: 7 nodes, 6 edges, kinds `next-in-session`, `why`, `weak_edges=0`; nodes: `coordination/artifact-registration`, `hygiene/route-to-who-can-act`, `problems/operator-turns-become-inference-observations`, `process/built-but-not-wired-invisibility`, `system-coherence/turn-design-into-checks`, `war-room/wr-16-operationalised-exploit-loops-are-first-class-observation-channels`, `war-room/wr-27-a-loop-is-born-instrumented-for-its-gain`.
- Rejected-pattern adjustment probes:
  - `cascades/on-the-fly-cascade` from query “prospective pattern cascade as the strategy for building”.
  - `inbox-zero/promote-at-turn-end` from query “mint a mission from a live design conversation while it is still warm”.
- Further patterns introduced by retractions: `inbox-zero/escalate-by-who-can-act`, `orchestration/recorded-handoff`, `process/built-but-not-wired-invisibility`, `system-coherence/turn-design-into-checks`, `war-room/wr-16-operationalised-exploit-loops-are-first-class-observation-channels`, `war-room/wr-27-a-loop-is-born-instrumented-for-its-gain`.
- Available arrangements now: 5 materialized policies (2 fragment-derived arrangements + 3 connected graph retractions); rejected probes can enlarge this only after a new reading admits them.

## Computing G over these policies

`futon2.aif.cascade-observation-scoring/rank-cascade-actions` is the existing family scorer (`src/futon2/aif/cascade_observation_scoring.clj:155-190`). For each candidate, `score-candidate` rolls its `:precedence` through the real model at every horizon step, queries the observation model, sums per-step `:g`, and records `:G-efe`, `:G-cascade`, and `[:certificate :consumed-g]` (`:91-150`). To score the HEAD-derived arrangements, the next wiring must turn every arranged node into the pattern’s documented transition interpretation, supply the initial belief, observation model, horizon, and preference schedule, and preserve the arrangement as candidate precedence/topology. Current admission rejects a candidate when any node lacks an interpretation or receipt (`src/futon2/aif/wm/cascade_decision.clj:1230-1281`). Joe delegated how the post-selection target-specific interpretation updates that documented meaning; this packet therefore does not compute G or manufacture missing transition operators.

## Failure counts and artifacts

- One mission (`M-metric-harness`) had usable seeds in more than one graph component and therefore produced zero connected retractions.
- Six seeds across five readings were refused because they have no graph edges.
- No mission produced zero initial cascades.
- Every request, validated analysis, retraction result, and the pinned graph is under `holes/labs/wm-contract/mission-head-cascades-2026-09-30/`.
