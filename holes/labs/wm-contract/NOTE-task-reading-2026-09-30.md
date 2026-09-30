# Task reading experiment — 2026-09-30

## Arrangement rule

`futon2.aif.analysis-cascade/analysis->cascades` now accepts `{:mode :alternatives}` (the original S3 rule and default) or `{:mode :overlap}`. In overlap mode, every validated `pattern_ref` attached to one exact fragment is retained in one cascade and every pair is joined by an `:overlap` edge. This represents co-application to the same span and shared state. Recorded rejections still fork readings, but never become nodes. Between consecutive fragments, all nodes are connected in text order; the source and target role vectors qualify the edge (`:justified-by`, `:contrasts-with`, `:dependency-enables`, `:condition-enables`, `:context-enables`, `:goal-precedes`, or `:precedes`). Nodes and edges retain fragment indices, text, roles, rationales, and pattern source hashes.

This remains provisional pending M-象-cascade's connected-cascade/retraction machinery. It uses exactly the structure the current 象 analysis records and does not depend on predeclared edges between pattern files.

## Corpus comparison

The corrected gate reads only published `turn-*.json.analysis.json` records
that carry both `request_file` and `status`; bare
`turn-*.analysis.json` drafts are excluded. At the final 09:24Z gate there were 891 such
records (the live count continued to grow after the 880 reported by
M-象-cascade).

The 880-record membership list was not saved, so it cannot be reconstructed
faithfully after new publications arrive. I have not selected an arbitrary
880-record subset. The table states the exact published input count read by
the gate; the test prints that count with every distribution.

| mode | analyses yielding 0 / 1 / 2+ cascades | node-count distribution | edge-count distribution | bare singleton cascades | fragment failures |
|---|---:|---|---|---:|---:|
| alternatives | 521 / 240 / 130 | 1:341, 2:169, 3:63, 4:3, 5:2, 7:1 | 0:341, 1:169, 2:63, 3:3, 4:2, 6:1 | 341 | 2317 |
| overlap | 521 / 256 / 114 | 1:319, 2:160, 3:53, 4:13, 5:2, 7:1 | 0:319, 1:160, 2:43, 3:13, 4:11, 5:1, 6:1 | 319 | 2317 |

Overlap therefore preserves more co-applicable nodes in a single arrangement and removes 22 bare singletons. The 521 zero-cascade analyses and 2317 fragments without a validated ref remain counted failures; the mode does not turn them into outcomes.

Plant control: in a scratch copy I discarded fragment/role data, made every
edge `:precedes`, and alphabetised the nodes. The namespace exited 1 at
`roles-distinguish-the-same-pattern-set`: the two same-pattern readings then
had identical topology. The scratch change was removed.

## Task request and selection

`scripts/wm_task_reading.py` takes the first unchecked checklist item (or an explicitly selected unchecked line), includes the complete Markdown section containing it, and emits the unchanged `session_turn_analysis.py` request schema. Sentence spans and task-item spans are exact Unicode-codepoint offsets into `source_text`. Task metadata records target id, canonical path, full-file SHA-256, original line, section bounds, and item bounds.

Selection rule: among tracked `M-*.md` files under `holes/` in the canonical Futon repositories (`futon0`, `futon1`, `futon1b`, `futon2`, `futon3a`, `futon3b`, `futon3c`, `futon4`, `futon5`, `futon5a`, `futon6`), exclude files whose first status says closed, superseded, abandoned, complete, finished, archived, or cancelled; sort the remainder that contain an unchecked item by that file's most recent commit time, then repository/path as a deterministic tie-break; take the first unchecked item from the first six files. The six request and validated analysis files are in `task-readings-2026-09-30/` beside this note.

The unchanged result schema records a retrieval `query` on each rejection, but not on accepted `pattern_refs`; accepted refs instead carry the fit rationale and pinned source SHA. The artifacts preserve all those rationales. The lists below therefore give every accepted id and every rejected id with the query that caused it to be considered; inventing an accepted-query field would have changed the contract.

## Six readings

### 1. M-象-2000 DOCUMENT acceptance item

- Source: `futon3c/holes/missions/M-象-2000.md:11`; job `invoke-1790758281081-28979-c9b4a80a`; 象-2; 250.0 s.
- 3 fragments. Roles: `goal+condition`; `condition+action`; `condition+dependency`.
- Accepted: `apparatus/done-is-observed-running` (twice, on the evidence account and user demonstration), `test-registry/judge-adequacy`, `war-room/wr-26-a-capability-switched-off-carries-its-re-arm-condition-in-writing-at-the-switch`, `writing-coherence/hedged-lift`.
- Rejected/query: `pattern-mining/probe-the-claimed-property-not-the-acceptance-proxy` ← “acceptance checklist items with observable evidence”; `hygiene/exempt-the-in-use` ← “defer items with written re-arm conditions”.
- Cascades: alternatives 16 × (3 nodes, 2 edges); overlap 4 × (5 nodes, 8 edges).

### 2. M-essays-diachronic-model mint-live-round item

- Source: `futon4/holes/missions/M-essays-diachronic-model.md:11`; job `invoke-1790758281135-28980-b11fd631`; 象-3; 144.5 s.
- 3 fragments. Roles: `action+goal`; `action+dependency`; `condition`.
- Accepted: `software-design/agent-command-pattern`, `exotic/immutable-vision-mutable-plan`.
- Rejected/query: `musn/pattern-action-helper-command` ← “name a command that performs one well-defined action”; `exotic/live-sync-source-truth` ← “derive a mutable working copy from an immutable source without modifying the original”.
- Cascades: both modes 4 × (2 nodes, 1 edge). The third fragment has no validated ref and is counted as a failure.

### 3. M-symbol-grounding Layer 1 item

- Source: `futon6/holes/missions/M-symbol-grounding.md:341`; job `invoke-1790758281285-28982-1db126e9`; 象-4; 290.6 s.
- 7 fragments. Roles: `context`; five `goal+condition` or `goal+action` fragments; final `goal+condition`.
- Accepted: `forward-model/seal-the-graduation-criteria` (four uses), `futon-theory/counter-ratchet` (two), `nomad/self-certifying-artifact`.
- Rejected/query: `cascade-construction/run-it-on-a-real-case` ← “demo must visibly show the feature working on real data”; `enrichment/extend-not-rewrite` ← “new layer must match coverage of previous layer before replacing it”; `data-mining/smoke-before-the-paid-run` ← “precision measured against held-out sample”; `pattern-mining/probe-the-claimed-property-not-the-acceptance-proxy` ← “preregistered acceptance criteria with measurable thresholds before doing the work”; `agent/pause-is-not-failure` ← “stopping rule or name the next bottleneck”.
- Cascades: alternatives 96 × (7 nodes, 6 edges); overlap 24 × (9 nodes, 11 edges).

### 4. M-symbol-grounding-scaling-plan Wikipedia-gold item

- Source: `futon6/holes/missions/M-symbol-grounding-scaling-plan.md:370`; job `invoke-1790758281196-28981-0f12fec6`; 象; 214.4 s.
- 9 fragments. Roles: `context`; goals; conditions/dependencies; final `condition+action` approval gate.
- Accepted: `data-mining/gates-as-code` (five uses), `data-mining/golden-is-curated-not-raw`, `aif/evidence-precision-registry`, `pattern-mining/probe-the-claimed-property-not-the-acceptance-proxy`, `data-mining/smoke-before-the-paid-run`, `orchestration/consent-gate`.
- Rejected/query: `plos-npt-with-small-n/small-n-is-a-design-feature` ← “small verified sample before trusting the bulk extraction”; `mmca/threshold-shaped-events` ← “threshold that a strategy must clear to count”.
- Cascades: alternatives 8 × (9 nodes, 8 edges); overlap 4 × (10 nodes, 11 edges).

### 5. M-superpod-mark3 arXiv tagging item

- Source: `futon6/holes/missions/M-superpod-mark3.md:710`; job `invoke-1790758469620-28993-1ed5d94d`; 象-3; 81.4 s.
- 3 fragments. Roles: `action+goal`; `condition+goal`; `condition`.
- Accepted: `data-mining/constrain-extraction-to-the-downstream-vocabulary`, `data-mining/golden-is-curated-not-raw`, `measurement/warrant-travels-with-the-number`.
- Rejected/query: `hygiene/classify-by-remedy-judgement` and `data-mining/smoke-before-the-paid-run` ← “classify items with a domain-aware prompt and a hierarchical tag set”; `ukrns/measure-where-least-sure` ← “measure agreement between machine labels and human hand-tagging”.
- Cascades: both modes 6 × (3 nodes, 2 edges).

### 6. M-superpod-mark2 200k-paper item

- Source: `futon6/holes/missions/M-superpod-mark2.md:206`; job `invoke-1790758469558-28992-908d070b`; 象-sonnet; 125.9 s.
- 14 fragments. Roles: heading context; corpus condition; threshold goals/conditions; pilot conditions; service/retrieval goals; five context/dependency table fragments.
- Accepted: `forward-model/seal-the-graduation-criteria`, `data-mining/the-first-checkpoint-is-a-quality-probe`, `data-mining/gates-as-code` (twice), `measurement/warrant-travels-with-the-number`, and `futon-theory/mission-dependency` (five uses). Four fragments have no validated ref and remain counted failures.
- Rejected/query: `data-mining/gates-as-code` ← “numeric quality gate encoded as a checkable threshold”; `process/fake-done-via-binary-closure` ← “acceptance criteria stated as checkable conditions before the work counts as done”; `data-mining/smoke-before-the-paid-run` ← “run the full pipeline at corpus scale before declaring it works”; `devmap-coherence/prototype-alignment-embedding` ← “embedding similarity spread as a collapse check on learned vectors”; `pattern-mining/probe-the-claimed-property-not-the-acceptance-proxy` ← “validation accuracy too high means the test is too easy”; `peripherals/pilot-plus-ground-control` ← “a pilot record is the evidence that a method works before scaling”; `exotic/semi-robust-baselines` and `cycle-machine/toolchain-port` ← “retrieval precision measured against a fixed earlier baseline”; `test-registry/bind-the-subject` ← “service must serve an index whose quality is measured, not asserted”; `data-mining/classify-over-one-clean-source` ← “table of missions and how each relates: consumer, shares resource, future consumer”; `stack-coherence/stack-blocker-detection` ← “state the relationship between missions so blocked and shared work is visible”.
- Cascades: both modes 192 × (10 nodes, 9 edges).

## What task reading cannot observe

A task supplies desired future conditions and context. Unlike a completed turn episode, it has no subsequent event showing what happened next. 象 can split the text, assign roles, retrieve and reject patterns, and derive candidate arrangements, but it cannot use the task alone to validate that an action caused an outcome, that a predicted transition occurred, or that the acceptance condition was met. Those claims require later execution evidence. The large alternative counts also show that each recorded rejection currently multiplies variants; this experiment reports that behavior rather than treating every generated variant as equally warranted.
