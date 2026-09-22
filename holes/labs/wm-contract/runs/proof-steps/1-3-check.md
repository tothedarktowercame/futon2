# Proof ⟨1⟩3 CHECK — read-only, through the LIVE serving JVM

zai-1, 2026-09-22. Evaluated against the serving JVM via
`futon3c/scripts/proof-eval.sh` (freshly reloaded: 42 namespaces current),
on the frozen reference input (`runs/proof-reference-field/`, target
T-repair-occ-444fb018…, candidates :C1 recheck / :C2 declare-the-split).
The probe replicates the production joint-decision wiring (the qualifier,
the class model, T, Joe's C) and calls the real contracted functions:
`efe/rank-actions`, `cascade-selection/selection-posterior`,
`cascade-selection/bayes-choice`. No files changed except this note.

## 1. The reference ticket's classification (live)

`{:class :focus, :kind :ticket-parent, :focus-status :retained,
:focus "WM"}` — the ticket classifies **:focus** through its Parent
(M-aif-policy-conditioned-eig's corpus relation, WM/focus), derivation
recorded `:ticket-parent`; the focus context at the decision time is
**:retained WM** (decision after the last window's validity; original
evidence date 2026-09-22T17:31:44Z).

## 2. Per candidate at T = 4

| | :C2 (declare the split) | :C1 (recheck) |
|---|---|---|
| E (habit) | cold start, uniform 1.0 (no history for either family) | same |
| F | **absent with reason** `{:value 0.0 :status :not-supplied :reason :class-model-unconditioned-at-selection}` — not recorded-as-0, explicitly not supplied | same |
| γ (= 1/β, β declared 1) | 1 | 1 |
| G | **0.5978** (= ln(1/0.55)) | **2.9957** (= ln 20) |
| per-step g | τ1=0, τ2=0, τ3=0, **τ4=0.5978** | τ1=0, τ2=0, τ3=0, **τ4=2.9957** |

Every intermediate step is exactly 0; the whole risk is the horizon's
class KL. C1's recheck guard fails on today's evidence-absent q₀ (its
obstruction-observation is stop-the-line at the horizon); C2 reaches
restoration-accepted on the focus-classified ticket.

## 3. The action marginal

First actions differ (:C2 → `:aif/declare-the-conditioning`,
:C1 → `:apparatus/done-is-observed-running`). The summed marginal over
first actions is **{`:aif/declare-the-conditioning` 0.9167,
`:apparatus/done-is-observed-running` 0.0833} — a UNIQUE maximum** at
C2's first action (posterior {C2 0.9167, C1 0.0833}).

## 4. Full law vs log E − F alone

- **Full law σ(ln E − F − γG):** chooses :C2
  (`:aif/declare-the-conditioning`), mass 0.9167 — a genuine maximum, no
  tie.
- **log E − F alone (G suppressed, same γ):** posterior is
  **{C2 0.5, C1 0.5} — an exact tie** (E uniform, F not supplied, so the
  score is constant). The name-order tie-break resolves the tie to
  `:aif/declare-the-conditioning` — the alphabetically first action name
  ("aif" < "apparatus"), which happens to be C2's. So log E − F alone
  does NOT choose on its own; the tie-break chooses.

## 5. Was the tie-break needed?

- Under the **full law: NO** — the maximum is unique (0.9167 vs 0.0833);
  the tie-break rule is recorded but not exercised.
- Under **log E − F alone: YES** — 0.5/0.5 tie, resolved by
  `:action-name-ascending`.

So G's contribution is exactly Joe's amended reading: it turns a
tie-break-resolved coin-flip into a unique maximum. (Correction to my
earlier design notes: name order picks C2 here, not C1 — "aif/…" sorts
before "apparatus/…".)

## 6. Horizon authority and C provenance

- The tick's horizon: **T = 4**, authority
  `[{:source "T-repair-occ-444fb018.edn" :horizon-steps 4}]` — the
  source-declared common horizon, lifted and named by file
  (war_machine.clj:6821 reads `:horizon-steps-declarations`).
- The scoring spec: `:c {:status :class-observation :source "Joe
  2026-09-22 ruling (55/35/5/5; unmeasured → stop-the-line)"}`, with
  live-c derived and recorded, never scored.

## Honest limitations of this check

- The probe replicates the production wiring by hand around the real
  contracted functions (the tick's own cascade-decision requires a live
  click context); every function called is the production one, and the
  wiring constants match the committed production code.
- Habit E is a cold-start uniform (both families have zero recorded
  trials); the live path's habit read would return the same neutral
  state today.
- F is explicitly not-supplied by the class model at selection
  (unconditioned); the score therefore did not subtract an F term, and
  the record says so rather than silently zeroing it.
