# Ants AIF as reference implementation — R1-R12 audit

*Cross-walk of the cyberants AIF apparatus (`futon2/src/ants/aif/`) against
the R1-R12 criteria from `futon2/docs/futon-aif-completeness.md`. The
ants are the first reference implementation in this codebase — predates
both `ukrn-services-simulation` and the WM AIF apparatus — and they ship
patterns worth porting forward.*

**Audit date: 2026-05-18**, against WM contract v0.12 and ants source as
of this commit. Audited modules: `observe.clj`, `perceive.clj`,
`policy.clj`, `affect.clj`, `default_mode.clj`, `pattern_efe.clj`,
`pattern_sense.clj`, `core.clj`.

## Cross-walk summary

| Criterion | Ants implementation | WM v0.12 | Pattern worth porting? |
|---|---|---|---|
| **R1 — Explicit belief state** | `mu` per ant: `:sens` (predicted observations) + `:h` (hunger) + `:pos` + `:goal`; multi-field, tick-over-tick | Per-entity posterior over status-set; uniform-prior bootstrap | Augment WM belief with predicted-observation state alongside status-posterior (would let R3a likelihood be a structural bridge) |
| **R2 — Observation channel schema** | 14 channels at ant scale (food, pher, traces, prox, hunger, ingest, novelty, dist, reserve, cargo, friendly-home, trail-grad) | 14 channels at strategic scale | No structural gap; both R2-satisfied |
| **R3 — Predictive-coding update** | `perceive/perceive` runs **5 micro-steps** with annealed τ; per-channel prediction errors weighted by per-channel precision; weighted MSE as free-energy estimate; trace per micro-step | Single-step belief update on `:annotation-health` only | **Port: multi-step micro-iteration with annealed τ** — ants iterate 5 inner steps within one outer call; each step recomputes errors against improving belief; WM does one shot |
| **R4 — Predictive forward model** | `predict-outcome` per action — myopic one-step with action-specific drift targets (forage, return, pheromone, hold each have detailed effects on each channel) | `forward-model/predict` similar shape; variance per channel | Roughly equivalent; ants' per-action effect detail is richer |
| **R5 — EFE principled terms** | **Six** terms: pragmatic (risk via NLL against preference Gaussians) + ambiguity (precision-weighted variance) + info (novelty + gradient gain) + colony (reserve-aware non-return penalty) + survival (hunger + dist + ingest pressure) + pattern (per active cyber-pattern) + action-prior-cost; all λ-weighted | **Two** terms (risk + ambiguity) + optional intrinsic-value | **Port: info-gain term, survival-cost term, action-prior-cost** — info uses (novelty-before − novelty-after + 0.25 × gradient-gain), survival is a long-horizon penalty term, action-prior reflects "this action is expensive given current state"; all augment EFE without replacing R5a/b |
| **R6 — Softmax + abstain** | Softmax with τ; **admissible-action filtering** (cargo/home/food state pre-prunes the candidate set); **mode-conditioned priors** via `policy/infer-mode` (q(m\|o)) shape the EFE; **default-mode fallback** if deliberative EFE fails | Softmax with adaptive τ; abstain when no candidate beats `:no-op` by ε; no admissibility filter; no mode-conditioning; no fallback path | **Port: admissible-action filtering** (state-conditioned pre-pruning of candidates), **default-mode fallback** (tropism-based action if EFE breaks — gives I6 compositional closure), **mode-conditioned priors** (already partially present via WM's existing `free-energy/infer-mode` — wiring it into action shaping is the missing step) |
| **R7 — Adaptive precision** | `affect/update-tau`: τ shifts by `(need-gain × need-error + dhdt-gain × hunger-trend + reserve-term)`; bounded by `tau-floor 0.08` / `tau-cap 1.5`; `affect/modulate-precisions` adjusts per-channel Π based on hunger and home-proximity | Rolling-variance over per-channel error history; precision = `1 / max(rolling-variance, ε)`; carried in trace records | **Port: trend-aware precision** (`dhdt` term — uses derivative of need-trend, not just point variance) **+ need-error term** (precision modulated by gap between observation and preference) **+ τ-floor/cap discipline**. Already noted as v0.13 in `futon-aif-completeness.md` §R7 "Forward design" — ants is the reference implementation to port |
| **R8 — Per-tick trace** | Multi-step trace inside `perceive` (5 entries per call recording τ, h, error per micro-step); persisted via `:recent` window on the ant; no external file | Daily-rotated EDN-lines external file; one record per call | Different concerns; both R8-valid. Pattern worth: **internal micro-step trace** could augment WM debug-ability — record the 5 inner steps if R3 multi-step iteration is ported |
| **R9 — Named validation properties** | Tests exist but not framed as named R9 properties with quantitative acceptance criteria | V-shrink + F-decrease + EFE-stress + Abstain-fires with quantitative criteria | **WM ahead.** Could port WM's R9 framework to test ants similarly — would catch ants regressions per the same discipline |
| **R10 — Live operation** | Batch simulation; multi-tick run; not on cron | Scheduled-execution-ready; cron pending | Different operational shape; not directly comparable |
| **R11 — Hierarchical composition** | Multi-ant colony with shared state (reserves, pheromone field); ant ↔ colony hierarchy; the `:reserve-home` channel exposes colony-state into individual ants' observations | Single observer; N/A by design | **Port-target if/when WM grows multi-observer composition** — shared-state pattern (each observer sees colony-level signals) |
| **R12 — Dual-loop hyperparameter inference** | Hyperparameters are config-driven, not inferred (preferences, λ-weights, τ-bounds, action-cost configs) | Same | Neither satisfies R12; no port-target |

## Patterns worth porting, ranked by leverage

### 1. R7 trend-aware precision (highest leverage, already planned)

The ants' `affect/update-tau` formula:
```
τ' = τ + need-gain × need-error
       + dhdt-gain × max(0, hunger-trend)
       + reserve-term  (cliff-shaped on reserve-home)
```
Bounded between `tau-floor 0.08` and `tau-cap 1.5`. The `dhdt` term is what makes precision trend-aware — it captures the *direction* of need-evolution, not just point variance.

For WM porting:
- Replace v0.12's `1 / max(rolling-variance, ε)` with a need-aware update.
- Per-channel: define need-error (gap from preference range) and need-trend (derivative over recent window).
- Apply ants-style `update-tau` per channel.
- Borrow `tau-floor` / `tau-cap` to bound runaway adaptation.

Already documented in `futon-aif-completeness.md` §R7 "Forward design: dual-clock perceived-time R7" as v0.13 candidate. **The ants are the reference implementation to port from.**

### 2. R5 additional EFE terms (info-gain + survival + action-prior)

WM's `efe/compute-efe` has only risk + ambiguity. The ants have four more:

- **info-gain** = `(novelty-before − novelty-after) + 0.25 × (gradient-after − gradient-before)`. For WM: novelty could be "entity not yet addressed by belief"; gradient could be "channel approaching preference range." Pure exploration signal — favours actions that REDUCE structural uncertainty.
- **survival-cost** = `hunger-weight × max(0, hunger − threshold) + dist-weight × dist + (ingest-buffer − ingest)`. For WM: aggregate "how far from preferences is the predicted state" with weighted terms.
- **action-prior-cost** = per-action base-cost penalising specific actions in specific states (e.g. empty-handed return when far from home). For WM: equivalent to the v0.5 `:intrinsic-value` but more state-conditioned.

Effort: medium. None require new substrate; all compose with existing prediction-error + forward-model machinery.

### 3. R6 admissible-action filtering + default-mode fallback (medium leverage)

The ants' `policy/admissible-actions` pre-prunes the candidate set based on state (e.g. forbid `:return` when empty-handed-and-food-here-and-away-from-home). This avoids the EFE machinery wasting cycles on actions that are state-impossible.

For WM: a `(can-execute? state action) → bool` predicate per action-type. Composes with existing `can-propose?` (which answers "is this action class proposable at all?"); `can-execute?` would answer "is this concrete instance executable in this state?"

The ants' `default_mode/select-action` is a pre-deliberative tropism-based fallback (if EFE fails, the agent still acts via simple gradient-following). For WM: if `select-action` throws or abstains under unusual conditions, fall back to a simple rule (e.g. always recommend the highest-`:intrinsic-value` `:learn-action-class` action). This gives **I6 compositional closure**: baseline behaviour persists even if deliberative apparatus breaks.

### 4. R3 multi-step micro-iteration (medium leverage, R3-completion path)

The ants' `perceive/perceive` runs 5 micro-steps per call: each iteration recomputes errors against an improving belief, annealing τ from `1.5 × target` down to `target`. This is standard predictive-coding inner-loop.

For WM: instead of one belief-update event per call (v0.10), iterate K times. Each iteration: compute prediction-error → synthesize event → update belief → recompute prediction. Convergence over K micro-steps gives a more honest single-call belief.

This is the R3 completion-arc — R3a/R3b/R3c/R3d are all present today but operate single-step. Ants show the multi-step version.

### 5. Mode-conditioned action priors (low-effort, high-clarity gain)

The ants' `policy/infer-mode` returns `q(m | o)` — a posterior over ant-modes (`:outbound`/`:homebound`/`:maintain`) given observation. The EFE is then weighted by mode-conditioned preferences `C_m`. WM already has `free-energy/infer-mode` (mode classifier) but doesn't use it to shape action priors.

For WM: extend `efe/compute-efe` to weight risk by `mode-prior(mode_inferred)` — strategic modes (`:multiplied`, `:depositing`, `:hermit`, etc.) carry mode-specific preferences. The plumbing exists; the wiring doesn't.

## Anticipation integration (the forward-axis missing piece)

`~/code/futon5a/README-anticipation.md` names the gap: AIF is "considerably disabled" without typed priors over future events. The piano-roll gives the retrospective axis; `~/code/calendar/events.edn` is the prospective axis. The WM apparatus currently looks only backward (trace + rolling-window precision).

**What anticipation would change in WM:**

1. **R4 multi-horizon prediction.** `forward-model/predict` is myopic (single-step). With anticipation: extend to `predict-horizon :: (state, action, horizon) → trajectory`, conditioned on which anticipated events are likely to fire within the horizon. For each candidate anticipated event with `:event/p-fires` and `:event/at`, the trajectory branches: `(state, action, t)` for `t ∈ [now, horizon]` is shaped by whether `event/at < t` and which branch of `:event/efe-sketch` is most likely.

2. **R5 time-conditioned preferences.** Current `:annotation-health [0.7 1.0]` is a static range. With anticipation: preferences shift as events approach. E.g. "annotation-health must be ≥ 0.8 by 2026-05-28 (Glasgow deadline)" → preference range tightens as the deadline approaches. The pragmatic risk term in EFE becomes time-weighted: gap from preference is more costly closer to the deadline.

3. **R6 action shaping by anticipated branches.** Each `:event/efe-sketch :if-submit / :if-not-submit` is itself an action-conditioned outcome. The WM's candidate-action ranking should compose with anticipated-decision-branches: candidate WM-action × candidate anticipation-branch → joint EFE.

4. **R10 cron firing tied to anticipation cadence.** Beyond fixed-hourly cron, anticipated events themselves are firing-triggers — a candidate "tick" source per the v0.13 perceived-time-convolution. An upcoming `:event/at` becoming "soon-fires" is itself a click-like event.

5. **Trace records carry anticipation context.** Each WM call's trace should record what events were upcoming, their `:event/p-fires`, and which branches of their `:event/efe-sketch` were the dominant terms in EFE scoring. Without this, future R9 properties on long-horizon behaviour can't be defined.

**Concrete integration points:**

- `judge` reads `~/code/calendar/events.edn` at start (similar to how it reads `stack-annotations.edn` for symmetric bootstrap and `sorrys.edn` for the registry).
- A new namespace `futon2.aif.anticipation` exposes `(load-anticipations)` + `(events-in-horizon now horizon)` + `(branch-probabilities event)`. Pure reads over the canonical source.
- `forward-model/predict` extended to accept `:anticipation` opt; per-action prediction shifts by anticipated-event effects within horizon.
- `efe/compute-efe` extended to time-weight pragmatic gap by proximity-to-deadline.
- Trace gains `:anticipation-state` field — which events were upcoming, with what `:event/p-fires`.

**Honest scope ladder for anticipation in WM:**

- **v0.13 (small)**: `futon2.aif.anticipation/load-anticipations` + `events-in-horizon`. Read-only; passes the result through to trace. Doesn't change EFE yet. Bearer-row catalogued in inventory §2.A.2.27 as row 6 (prospective). Operationally: WM call output gains `:anticipated-events <vec>` so the operator can SEE what's upcoming, even before time-conditioning lands.
- **v0.14 (medium)**: time-conditioned `:annotation-health` preference (and possibly others). Preference range tightens as a related anticipated event's `:event/at` approaches. Honest scope: one channel × one anticipated event linkage; expand later.
- **v0.15 (larger)**: multi-horizon `predict-horizon` + EFE composition with `:event/efe-sketch` branches. This is the structurally interesting move — the WM starts reasoning about its own actions across the anticipation surface.

## Recommendations (high-leverage moves first)

1. **R7 v0.13 (perceived-time convolution + ants `update-tau` port)** — already named in `futon-aif-completeness.md` §R7 Forward design. The ants ARE the reference implementation; porting `update-tau` semantics is a small lift on top of v0.12's precision-state. Direct unblock for the dormant-cron-firing-dilutes-signal failure mode.

2. **Anticipation v0.13 (read-only)** — `load-anticipations` + `events-in-horizon` + trace-pass-through. Gives the WM a forward axis at minimum cost; bearer-row in inventory §2.A.2.27 row 6. Operator-visible output without changing decision logic yet.

3. **R5 info-gain + survival-cost** — augment `efe/compute-efe` with two additional EFE terms ported from ants. Composes with v0.11's R5a/R5b; doesn't require new substrate.

4. **R6 admissible-action filtering** — `can-execute?` predicate alongside `can-propose?`. Cheap to add; cleans up candidate sets so EFE doesn't waste cycles on state-impossible actions.

5. **Default-mode fallback** — port `default_mode/select-action` shape so the WM has I6 compositional closure if EFE-deliberation breaks.

6. **R3 multi-step inner iteration** (R3-completion candidate) — port `perceive/perceive`'s 5-micro-step inner loop with annealed τ. Closes R3 to the next level of fidelity.

7. **(Deferred) R12** — neither ants nor WM satisfies. No port-target until a meta-learning surface exists.

## Cross-references

- `~/code/futon2/src/ants/aif/` — the reference implementation audited here.
- `~/code/futon2/docs/futon-aif-completeness.md` — WM v0.12 contract (R-criteria status).
- `~/code/futon5a/README-anticipation.md` — anticipation framing the ants don't have (forward-axis).
- `~/code/calendar/events.edn` — canonical anticipation source.
- `~/code/futon7/holes/M-interim-director-proxy-metric-inventory.md` §2.A.2.27 — typed-temporal-bearers table; row 6 is the prospective bearer.
- `~/code/futon7/holes/M-interim-director-proxy-metric-inventory.md` §2.A.2.38 — anticipation full design.

## Status

**v0.1 drafted 2026-05-18** by claude-2 in response to Joe's request for "a full review of the ants code against our R1-R12 criteria since it was the first reference implementation." Audit covers all R1-R12 + anticipation integration. Notes are forward-design; nothing in this audit changes the WM contract's current verdicts.
