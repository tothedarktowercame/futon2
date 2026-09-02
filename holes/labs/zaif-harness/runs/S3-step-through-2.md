# S3 step-through 2 — declared inputs applied; rank 91 → 2; not yet selected

claude-1, 2026-09-02. Row S3, in progress. Weight table:
`S3-step-through-2-weights.edn` (same transforms as S2; phases now fetched
per mission from the futon1b mission-doc endpoint vertices — 36 of 133 have a
readable phase, the rest honestly unknown→0.3).

## The two declared acts, and what each moved

1. **Mission-doc ingest** (canonical path:
   `futon3c.watcher.file-ingest/ingest-mission-doc!`, invoked in the futon3c
   JVM): minted `hx:code/v05/mission-doc:futon2-d/mission/zaif-harness-v1`
   with endpoint vertex carrying `:mission/phase "instantiate"` — parsed from
   the doc, not asserted by hand. Doability 0.3-unknown → 1.0: **+0.21**.
2. **Spine membership** (futon7 `c5b7288`): box :m34 added to the backlog
   cascade with provenance quoting Joe's in-session authorization verbatim.
   Strategic 0.0 → 1.0: **+0.45**.

Delta: 0.09 (rank 91) → **0.7500 (rank 2 of 133)**. Every point of the move
names its declared input. No rank or weight was edited.

## Why it is not selected, and the honest closers

The incumbent `M-expressions-of-interest` also sits at instantiate (real
phase, fetched) and scores 0.7862 = 0.25·0.685 (centrality) + 0.45·0.7
(cascade terminal) + 0.30·1.0. The gap is **0.036, entirely centrality** —
our mission is absent from the forward-model centrality census.

Checked and ruled out today: the live path's non-progress decay would not
demote the incumbent (it only fires on repeat-selection of the *previous*
target; the last real selection was M-aif-policy-conditioned-eig).

Three honest closers, in ascending bluntness:

- **(a) Centrality census re-run** with the new mission linked (its
  cross-refs exist; the census is computed, not authored). Slowest, most
  organic: the mission earns rank as the graph learns it exists.
- **(b) Joe's weight declaration**: `FUTON_WM_VALUE_WEIGHTS` is an on-the-
  record operator input. `{:central 0.20 :strategic 0.50 :doable 0.30}`
  gives ours 0.80 vs the incumbent's 0.787 — a global statement that
  operator strategy currently outweighs graph centrality, which is arguably
  exactly what this week is.
- **(c) Accept the machine's preference** and note that at mission-value
  grain the selector currently, defensibly, prefers a high-centrality
  instantiate-phase mission — then let selection happen anyway by operator
  fiat outside the loop. Least satisfying: it abandons the row's purpose
  (evidence the LOOP can select it).

The choice among (a)/(b)/(c) is an operator ruling — S3's own design says
Joe's declared priority is the input that decides. Row stays open pending it.
