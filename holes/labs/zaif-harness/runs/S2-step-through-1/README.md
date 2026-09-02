# S2 step-through 1 — the outer loop's selection, by hand, unprejudiced

claude-1, 2026-09-02. Row S2. Every number's origin stated; substitutions
declared. Baseline only — no prejudice applied (that is S3).

## Stages and artifacts

1. **Enumerate** (`01-candidates.edn`): `futon2.aif.mission-registry/open-missions`
   run in a fresh local clojure process (never the shared JVM; never the full
   tick, which dispatches authors). 133 live candidates from the
   `*/holes/missions/M-*.md` scan. M-zaif-harness-v1 IS among them — its
   registration this morning (git mv into `futon2/holes/missions/`, leading
   status token OPEN) was the declared candidacy act, observed working.
2. **Weight** (`02-weights.edn`): the three factors computed by hand with the
   real inputs and the real transforms, per S1's map:
   - centrality: `c_joint` from `futon7/holes/M-futon-forward-model.centrality.json`,
     normalized by the max (war_machine.clj:1039-1050, 1532-1540);
   - strategic: spine 1.0 / terminal 0.7 / member 0.4 / absent 0.0 against
     `futon7/holes/M-futon-forward-model.backlog-cascade-merged-v0.edn`
     (:1514-1530), mission ids read from box text;
   - doable: **substitution, declared** — phase "unknown" 0.3 applied to every
     row. The live path reads phase per mission from the `code/v05/mission-doc`
     hyperedge's endpoint vertex (futon3c mission_delta_t.clj:237-254); walking
     the substrate for 133 missions was not done by hand. The 09-01 trace shows
     the live path itself getting `:phase nil` → 0.3 for its selected mission,
     so the substitution matches observed live behaviour more than it distorts.
   - blended: 0.25·central + 0.45·strategic + 0.30·doable (the default weights,
     :1400-1403). Completion/operator gates: no candidate in this pass carries a
     parsed `**Gate:** operator-…` line in the enumerated set; non-progress
     decay not applicable (no previous-selection state consulted — declared).
3. **Rank** (`03-ranking.edn`): sort by blended, descending.
4. **Select** (this file): the top of the ranking.

## The baseline result

| rank | blended | central | strategic | mission |
|---|---|---|---|---|
| 1 | 0.5762 | 0.685 | 0.7 | **M-expressions-of-interest** ← baseline selection |
| 2 | 0.4333 | 0.653 | 0.4 | M-distributed-frontiermath |
| 3 | 0.3786 | 0.434 | 0.4 | M-symbol-grounding |
| … | | | | |
| **91** | **0.0900** | **0.000** | **0.0** | **M-zaif-harness-v1** |

(133 candidates; full table in 02-weights.edn.)

Scope note: this ranks the *mission-value* blend (step ⑬). The live controller
additionally blends G_efe and the engineering augmentation (⑭–⑮) before
selection (⑯); those need a real tick and are S4's territory. At mission-value
grain, the baseline selector picks M-expressions-of-interest and our mission is
nowhere close — which is the honest starting line.

## What this makes precise for S3 (the declared levers, priced)

M-zaif-harness-v1 scores 0.09 because it is invisible to all three factor
sources:

- **strategic 0.0** — not in the backlog cascade. Spine membership is worth
  0.45 of blended value on its own. The cascade file is an *authored operator
  strategy artifact* (authored claude-2 + joe): adding this mission to the
  spine is exactly Joe's declared mark, and it is the single biggest lever.
- **doable 0.3-unknown** — no mission-doc hyperedge, so no endpoint, so no
  phase. Registering the hyperedge with the phase the doc states (INSTANTIATE
  is running as worklist rows → 1.0) is worth up to +0.21.
- **central 0.0** — absent from the forward-model centrality census. Honest
  path is linking the mission and re-running the census, not editing a JSON.

Priced outcome: spine (0.45) + instantiate phase (0.30) = 0.75 > 0.5762 —
with Joe's one authored act plus one registration, the mission tops the board
through declared inputs only, no rank edited. That re-run is S3.
