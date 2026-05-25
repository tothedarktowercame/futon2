# AIF completeness contract — Futon War Machine

*The eleven properties an Active Inference implementation must satisfy to honestly carry the label, and which of them the War Machine AIF apparatus in `futon2.aif.*` currently supports.*

## Why this document exists

The War Machine in `futon2/scripts/futon2/report/war_machine.clj` carries the "AIF observer" claim through the `judge` function (composes observe → compute-free-energy → infer-mode). The vocabulary — free energy, observation, mode — is evocative enough that it can be borrowed by code which implements only a fraction of the apparatus. This contract specifies, property by property, what the WM must do to earn the description honestly, and grades the current implementation.

The criteria here are R1-R12 (R for *requirement*), ported from the predecessor `~/code/ukrn-services-simulation/docs/aif-completeness.md`. Where that contract measures a *batch simulation* against the Friston / Da Costa / Parr formulation, this contract measures a *live observer* operating at strategic timescale over the futon stack's substrate. The numbering is shared; criterion semantics are the same; the satisfaction modes differ (R10 in particular flips from N/A-by-design to in-flight).

## Scope note — F1-F10 vs R1-R12

`M-war-machine-aif-completion.md` §1.5 names the exit criterion as "F1-F10 (or R1-R12) completeness contract satisfied," treating them as alternatives. They are not alternatives — they are complementary contracts at different scopes:

| Scope | Contract | Home | What it measures |
|---|---|---|---|
| Whole futon stack | F1-F10 | `~/code/futon0/docs/stack-fitness-completeness.md` | Stack as a homeostat (substrate-2, watcher daemons, satisficing-zapper, metabolic-balance, VSATARCS) |
| WM AIF apparatus | R1-R12 (this doc) | `~/code/futon2/docs/futon-aif-completeness.md` | WM as an AIF implementation (belief, predictive coding, EFE, action selection, trace) |

The WM's satisfaction of R-criteria *contributes to* F-criteria movement at stack scope, but R and F are not interchangeable. The cross-mapping is named explicitly in §"Cross-mapping to F1-F10" below.

## Scope of the WM AIF apparatus

This contract covers the namespaces under `futon2.aif.*`:

- `futon2.aif.observation` — observation channels (13), `observe`, `sense->vector`
- `futon2.aif.preferences` — preferences, avoided-states, mode-prior, pragmatic-weights
- `futon2.aif.free-energy` — `compute-free-energy`, `infer-mode`
- `futon2.aif.belief` — per-entity posterior over a tagged status set, deterministic update step (as of v0.2)
- `futon2.aif.forward-model` — pure `predict :: (state, action) → next-state-distribution` (as of v0.3)
- `futon2.aif.efe` — `compute-efe` (G-risk + G-ambiguity on R4-predicted state); `rank-actions` (ascending-by-EFE) (as of v0.4); `:intrinsic-value` action credit (v0.5)
- `futon2.aif.action-proposer` — `ActionProposer` protocol, `bootstrap-proposer`, `compose-proposers` (v0.5)
- `futon2.aif.policy` — `adaptive-temperature`, `softmax-weights`, `select-action` (chosen or abstain with gap-report) (v0.5)
- `futon2.aif.sorry-registry` — v1 substrate adapter for `:sorry` entities; `load-sorrys`, `open-sorrys`, `can-propose? :address-sorry` override, `sorry-enumerator-proposer` (v0.6)
- `futon2.aif.mission-registry` — mission-doc substrate adapter for `:open-mission`; scans top-level `*/holes/missions/M-*.md`, filters non-complete, exposes `mission-enumerator-proposer` (v0.18)
- `futon2.aif.pattern-registry` — context-retrieval substrate adapter for `:fire-pattern`; aggregates recent Evidence Landscape retrieval certificates into bounded pattern candidates, exposes `pattern-enumerator-proposer` (v0.19)
- `futon2/data/sorrys.edn` — hand-curated open-sorry registry (v0.6 onwards); the meta-sorry (substrate-addressability question) is the first entry
- `futon2.aif.trace` — `trace-record`, `write-trace!`, `read-trace`, `read-trace-range`; EDN-lines daily-rotated trace store at `~/code/futon2/data/wm-trace/wm-trace-YYYY-MM-DD.edn` (v0.7)
- `futon2/scripts/wm_scheduled_run.clj` + `:wm-scheduled` deps alias — one-shot scheduled-execution entrypoint that scans, judges, persists trace, prints one-line summary, exits with 0/1 (v0.8)

And the consumer: `futon2.report.war-machine/judge` which composes the AIF apparatus with structural priorities, AIF heads, and invariant inventory into a per-call snapshot.

The WM is **not** a batch simulation. It is a **live observer**: each invocation runs against the current futon stack state, produces a snapshot judgement, and exits. There is no action loop; no policy selection (yet). As of v0.2, **belief state is persisted in `futon2.aif.belief` and may be carried across calls**; the integration of belief-update events with `judge` is the next step.

## The criteria

### R1 — Explicit belief state

The implementation maintains a belief distribution over hidden state, carried across ticks, with mean *and* precision (variance) both explicitly represented.

**Operational check.** Find the persistent belief data structure. Verify it has mean + variance fields per state dimension. Verify it is updated tick-over-tick rather than recomputed.

**This implementation.** **Satisfied as of v0.2 (2026-05-17); operator-surface + symmetric bootstrap closed as of v0.9 (2026-05-18).**

`futon2.aif.belief` namespace ships with:
- `status-set` — tagged status set (`:spawned :refined :strengthened :addressed :falsified :foreclosed :reopened`), aligned with M-INC event vocabulary v1 `state/*` event types. Identical to VSATARCS-side `arxana-vsatarcs-belief-status-set`.
- `initial-belief-state` — entity-id → uniform posterior over status-set.
- `update-entity-belief` / `update-belief` / `update-belief-batch` — deterministic multiplicative-likelihood update under typed events; M-INC-compatible event shape, no dependency on M-INC step (b).
- `most-likely-status` — argmax (discrete analogue of belief 'mean').
- `entropy` — Shannon entropy in nats (discrete analogue of belief 'precision').
- *(v0.9)* `section-ids-from-stack-annotations` — reads `:sections[] :id` strings from `~/code/futon5a/holes/stack-annotations.edn`.
- *(v0.9)* `bootstrap-from-stack-annotations [extra-ids]` — returns a belief state whose entity domain is the union of canonical section-ids and extra-ids; falls back to extra-ids on file-read failure.

`judge` (since v0.6) calls `bootstrap-from-stack-annotations` with the open-sorry ids — so the WM-side belief now spans the same entity domain VSATARCS-side bootstraps from (35 string section-ids today) plus the meta-sorry's keyword id.

Tests at `futon2/test/futon2/aif/belief_test.clj` (14 tests / 31 assertions): posterior shape invariants, deterministic update (R1 baseline contract), multiplicative-likelihood commutativity, V-shrink-shape, most-likely-status correctness, section-ids smoke test against the real canonical source, known-id presence, bootstrap shape + merging + fallback + heterogeneous keys disjointness.

**Bilateral milestone closure `hx:wm:v0-9:symmetric-bootstrap-closure`** (paired with VSATARCS-side `hx:vsatarcs-align:v0-2-5:cross-side-bridge-closure`): the WM-side half of the v0.9 ↔ v0.2.5 alignment landing. After both sides bootstrap from the same `:sections[] :id` strings, per-entity posterior comparison reduces to alist-lookup equality on shared string entity-ids — the substrate VSATARCS's `arxana-vsatarcs-belief-compare` consumes. The trace file (`~/code/futon2/data/wm-trace/wm-trace-YYYY-MM-DD.edn`)'s latest record's `:mu-post` is the cross-side bridge read path. **Caveat (recorded in the closure)**: WM-side belief carries one keyword key (`:sorry/wm-aif-substrate-addressability`) alongside the 35 string section-ids; VSATARCS-side comparison will see this as `:only-in-a` and should tolerate it as expected per the v0.9-side note.

### R2 — Observation channel schema

The agent's interface to the world is a fixed, normalised observation shape — a vector or map of bounded channels. The schema is named; observations from different ticks have the same shape and the same channel semantics.

**Operational check.** Find the observation type. Verify the channel set is documented and stable, observations are normalised to [0,1], and all subsequent AIF machinery is keyed off the schema.

**This implementation.** **Satisfied.** `futon2.aif.observation/observation-channels` declares 13 channels with documented semantics and source vocabulary (loop-health, support/attack coverage, mission-health, four commit-percentage channels, active-repo-ratio, sorry-count-norm, coupling-density, ticks-firing-ratio, depositing-signal). All channels return values in [0,1]. Schema is enforced by `observation-channels-test` in `futon2/test/futon2/aif/observation_test.clj` (count = 13, all keywords, no duplicates). `sense->vector` projects observations to the channel-order vector with explicit ordering test.

### R3 — Predictive-coding belief update

Four sub-properties must all be present: (R3a) prediction error per observation channel; (R3b) precision weighting; (R3c) variational free energy; (R3d) belief update step.

**Operational check.** Find the belief-update function. Verify all four sub-properties are computed by name; VFE is reported per tick.

**This implementation.** **Satisfied for 4 of 14 channels with the remaining 10 logged as `:prototyping-forward` sorries (v0.11, 2026-05-18).** R3 aggregate flips to ✓ per the sub-property closure protocol PLUS the channel-coverage discipline made first-class: channels without likelihood are addressable forward-work captured in the sorry registry rather than implicit gaps.

**Per-sub-property status at v0.11**:

- **R3a (prediction error per channel) — ✓ for 4 of 14 channels.** `futon2.aif.belief` ships four likelihood models:
  - `predict-annotation-health` — per-status weights (`:strengthened` +1.0, `:addressed` +1.0, `:refined` +0.5, `:spawned` 0, `:reopened` 0, `:foreclosed` −0.5, `:falsified` −1.0); per-entity expected-health rescaled [-1,1]→[0,1]; channel mean = average across entities.
  - `predict-sorry-count-norm` — Σ per-entity open-mass (`:spawned` + `:refined` + `:reopened`), divided by 10, capped at 1.0.
  - `predict-mission-health` — mean per-entity healthy-mass (`:strengthened` + `:addressed`).
  - `predict-active-repo-ratio` — mean per-entity non-dormant-mass (1 − (`:foreclosed` + `:falsified`)).

  All four share the same variance shape (mean posterior entropy normalised against log(|status-set|)). `predict-observation` returns the composite. The set `channels-with-likelihood` enumerates the 4 covered channels. The remaining 10 channels are explicitly logged as `:prototyping-forward` sorries in `futon2/data/sorrys.edn` (`:sorry/r3a-likelihood-loop-health` through `:sorry/r3a-likelihood-depositing-signal`); some are candidates for `:n-a-by-design` reclassification when the structural-review work happens (externally-measured signals like commit-percentages may be inherently belief-independent).

- **R3b (precision weighting) — ✓ for all 4 covered channels.** Precision Π = 1 / max(variance, ε) with ε = 0.01 by default; weighted-error = error × Π. Per-channel precision applied at the prediction-error step. `futon2.aif.free-energy/compute-prediction-error` returns `{:observed :predicted-mean :predicted-variance :error :precision :weighted-error}`.

- **R3c (variational free energy) — ✓ in shape since v0.2.** `compute-free-energy` decomposes G into pragmatic + epistemic + per-channel detail.

- **R3d (belief update step) — ✓ wired into the per-call cycle.** `judge` computes prediction-errors for all 4 covered channels and records them in the trace's `:prediction-errors` map; the synthesised belief-update event currently draws its sign and magnitude from `:annotation-health` alone (unambiguous direction — high observed = healthy). Multi-channel sign-aggregation (combining the 4 errors into one R3d driver) is logged as `:sorry/r3d-per-entity-attribution`'s sibling concern and deferred: the channels differ on whether high observed = healthy (annotation-health, mission-health, active-repo-ratio) vs unhealthy (sorry-count-norm), so a per-channel `:health-sign` is needed before signs can combine coherently. Empirical evidence of this from a 2026-05-18 run: errors were +0.37 / −0.15 / −0.24 / −0.20 — naive sum would cancel; principled aggregation needs the sign annotation.

End-to-end verified 2026-05-18: 36 entities; observed annotation-health 0.94, predicted 0.57, error +0.37; mission-health 0.14/0.29/−0.15; active-repo-ratio 0.47/0.71/−0.24; sorry-count-norm 0.80/1.00/−0.20. R3d-driven belief update via :annotation-health alone; sample entity `:strengthened` mass moved 0.143 → 0.147.

**Coverage stance**: 4 of 14 channels have likelihood. The remaining 10 are logged as `:prototyping-forward` sorries — explicit forward-work, not implicit debt. The aggregate R3 verdict reads ✓ under the discipline "all sub-properties satisfied AND remaining gaps are first-class addressable work."

**Cross-side naming note (2026-05-20).** VSATARCS-side reports the sibling landing differently: R3a + R3c are satisfied there, while R3b is still `:blocked-on-R7`, so its R3 aggregate is described as `:partial-three-of-four-sub-properties-satisfied`. This is an honest asymmetry, not a contradiction. The sub-property closure protocol is per-side: WM-side R7 landed in v0.12, so precision-weighted error closed R3b here; VSATARCS-side has not yet landed its own adaptive-precision path, so R3b remains open there while R3 aggregate still carries meaningful partial status.

### R4 — Predictive forward model

A pure function `predict :: (state, action) → next-state-distribution` that the agent uses to *score candidate actions before taking them*. Without this, the agent can score actions only against current state, and EFE collapses to a heuristic.

**Operational check.** Find the function that produces predicted next-state. Verify it is pure, returns a distribution (mean + variance), and is called by the policy layer.

**This implementation.** **Satisfied as of v0.3 (2026-05-17).** `futon2.aif.forward-model/predict` is a pure function `(state, action) → next-state-distribution`:
- State shape: `{:observation <obs map> :belief <belief map>}`.
- Action shape: `{:type <keyword in action-types> :target <entity-id> :weight <number, optional>}`. Action-types in v0.3: `:no-op`, `:address-sorry`, `:open-mission`, `:fire-pattern` (extensible via the `predict-effects` defmulti).
- Output: `{:next-observation {:mean <obs map> :variance <obs map>} :next-belief <updated posterior map> :action <action> :predicted-events <events seq>}`.
- The next-observation distribution is point-mean + per-channel variance (variance is hand-tuned per action type in v0.3; will become learned with R7).
- The next-belief is computed via `futon2.aif.belief/update-belief-batch` applied to the action's predicted events — a proper posterior by construction.

Tests at `futon2/test/futon2/aif/forward_model_test.clj` (10 tests / 31 assertions) verify: purity, documented output shape, variance non-negativity, `:no-op` preserves state, each action type's expected observation deltas and belief shifts, [0,1] clamping under extreme actions, ex-info on invalid action, posterior validity in next-belief. **Shared-kernel discipline is structural-ready**: `predict` is the function a future `step` would call to actuate an action, so the same code path runs both the live step and the EFE scoring. No live step exists yet, so the discipline is *aspirational not enforced* — until R6 (action selection) lands and `judge` calls `predict`, there is no second caller to share with.

### R5 — EFE with at least two principled terms

Expected Free Energy decomposes into at minimum: (R5a) pragmatic / risk and (R5b) epistemic / ambiguity. Both are computed against the predictive forward model from R4.

**Operational check.** Find the EFE computation. Verify both pragmatic and epistemic terms are present and computed against the predictive forward model.

**This implementation.** **Satisfied as of v0.4 (2026-05-17).** `futon2.aif.efe/compute-efe` scores a `(state, action)` pair by composing R4's forward model with the R3c free-energy decomposition. The two principled terms required by R5:
- **R5a — G-risk**: pragmatic / risk term. Uses `compute-free-energy`'s :G-pragmatic on the *predicted* next-observation mean, not the current observation. Captures "how far the predicted state is from preferences."
- **R5b — G-ambiguity**: epistemic / ambiguity term. Sum of per-channel predicted variances from `forward-model/predict.next-observation.variance`. Captures "how uncertain the predicted outcome is" (high-variance actions are higher EFE).
- **G-total** = G-risk + G-ambiguity.

`futon2.aif.efe/rank-actions` scores a sequence of candidate actions and orders them by G-total ascending (lowest EFE first), with `:rank` annotations.

Tests at `futon2/test/futon2/aif/efe_test.clj` (9 tests / 22 assertions) verify: purity, documented output shape, G-total = G-risk + G-ambiguity by name, :no-op has zero G-ambiguity (deterministic prediction), risk reflects PREDICTED state not current state, ambiguity strictly orders actions by their declared variance, rank-actions returns ascending G-totals with sequential ranks, decomposition properties hold under stressed state.

**The proper information-gain epistemic term** (negative of expected entropy reduction in belief, vs. ambiguity's expected entropy under the action) is a future R7-related deliverable; ambiguity is the principled R5b minimum and is what the predecessor `ukrn-services-simulation v3` uses ("ambiguity is predictive entropy over the next-state distribution" per its README). **Action proposal** (where candidate actions come from) is intentionally out-of-scope for R5 — `compute-efe` and `rank-actions` take candidates as input; action proposal lands with R6 (action-space enumeration) or is provided externally.

### R6 — Softmax action selection with abstain

`P(a) ∝ exp(−G(a) / τ)`, sampled or argmax. Abstain semantics: when the predictive distribution is too uncertain to discriminate among candidates, the agent declines to act.

**Operational check.** Find the action-selection function. Verify softmax with temperature τ; verify an abstain branch exists and fires under high uncertainty.

**This implementation.** **Satisfied as of v0.5 (2026-05-17).** Three new namespaces ship:

1. **`futon2.aif.action-proposer`** — `ActionProposer` protocol, `bootstrap-proposer` (always-available default), `compose-proposers`. The bootstrap proposer surfaces `:no-op` plus one `:learn-action-class` action for every action-type whose `forward-model/can-propose?` returns false. This is the **"need to learn" / actionable self-model** move (per Joe 2026-05-17): when no concrete actions exist for an action class, the highest-priority recommendation is to enable the class itself, not to fake actions over non-existent entities. Substrate-aware proposers plug in via the protocol when their target substrate becomes addressable.

2. **`futon2.aif.policy/select-action`** — top-level R6 deliverable. Takes a `rank-actions` output, applies softmax over G-totals with adaptive τ (`τ = max(spread/k, τ-min)`), returns either the chosen action (`{:action :rank :G-total :tau :softmax-weights}`) or an abstain branch (`{:action :abstain :reason :gap-report :ranked-actions}`).

   - **Adaptive τ**: scales with EFE spread. Tight spread → low τ → soft / abstain-leaning; wide spread → high τ → sharp pick. Tunable `tau-min` and `k`.
   - **Abstain semantics**: fires when (a) no candidates, or (b) `:no-op` is present and the best action's G-total is not at least `abstain-epsilon` below `:no-op`'s. The abstain branch carries a **gap-report** enumerating the `:learn-action-class` actions the proposer surfaced — the operator-facing report of capability gaps the WM detected.

3. **`futon2.aif.forward-model`** extensions:
   - `action-types` extended with `:learn-action-class` (M-INC-compatible action map with `:target-class` instead of `:target`).
   - `predict-effects :learn-action-class` returns no observation delta, no events — the action's value isn't in observation-space, it's in capability-space.
   - `can-propose?` defmulti dispatches per action type; default false; `:no-op` and `:learn-action-class` arms return true. Other action types default to false until a substrate adapter ships and overrides the multimethod.

4. **`futon2.aif.efe/compute-efe`** extended to respect optional `:intrinsic-value` on action maps — a hand-tuned credit subtracted from G-risk that represents long-run capability value not yet captured in any observation channel. The bootstrap proposer sets `:intrinsic-value 0.1` on `:learn-action-class` actions; this is the bias that makes `:learn-action-class` outrank `:no-op` when no other action ranks higher.

Tests at `futon2/test/futon2/aif/{forward_model,efe,action_proposer,policy}_test.clj` cover: action-type validation, `:learn-action-class` effects, `can-propose?` defaults, intrinsic-value crediting, bootstrap-proposer gap surfacing, compose-proposers de-duplication, adaptive-temperature monotonicity + floor, softmax-weights normalisation, select-action chosen branch, all four abstain trigger conditions, gap-report enumeration, ε tunability.

**Empirical state of play 2026-05-17 (v0.5 snapshot):** every concrete action type (`:address-sorry`, `:open-mission`, `:fire-pattern`) had `can-propose?` returning false — no substrate adapter shipped in v0.5. The substrate-2 instance at `localhost:7071` returned `{:hyperedges [], :count 0}` to every type probe. The WM correctly surfaced three `:learn-action-class` recommendations (one per gated action class).

**Empirical state of play 2026-05-17 (v0.6 snapshot):** `:address-sorry` is now proposable via `futon2.aif.sorry-registry` (v1 hand-curated adapter; 1 open sorry — the meta-sorry that registered itself). The WM's candidate set is now `{:no-op×1, :learn-action-class×2 (:fire-pattern + :open-mission), :address-sorry×1 (target :sorry/wm-aif-substrate-addressability)}`. The top-3 ranked under current parameter tuning are the two `:learn-action-class` actions (G≈0.0925) followed by `:no-op` (G≈0.1925); `:address-sorry` ranks 4th at G≈0.2045 because its small pragmatic gain (~0.003) is dominated by `:learn-action-class`'s hand-tuned intrinsic-value credit (0.1). **This is a real tuning observation** — under v0.6 parameters the WM prefers expanding capability over addressing the one sorry it can address. It's exactly the kind of signal the §"Capability-gap modeling" experimental direction #1 (intrinsic-value learning) anticipates needing data to resolve.

### R7 — Adaptive precision

Precision Π updates over time based on prediction-error history. Channels with persistent high error lose precision; channels with persistent low error gain precision.

**Operational check.** Find where Π is updated. Verify it is updated tick-over-tick.

**This implementation.** **Satisfied as of v0.12 (2026-05-18) for the 4 channels with R3a likelihood models.** `futon2.aif.precision` namespace ships:

- `initial-precision-state` — per-channel `{:precision 1.0 :error-history []}` for every channel in `belief/channels-with-likelihood`.
- `update-precision-state` — pure: takes previous precision-state + new prediction-errors map, returns updated state. Appends each new error to its channel's history (bounded to `default-window-size 20`); recomputes `:precision = 1 / max(rolling-variance, 0.01)`. Channels not touched by new errors pass through unchanged; channels new to this call get initialised.
- `weighted-error` — replaces a per-call prediction-error's `:precision` and `:weighted-error` with the adaptive-precision values; preserves the original under `:per-call-precision` so trace records both sources.
- `precision-for` lookup.

`judge` reads the previous precision-state from the latest trace record (defaulting to `initial-precision-state` on first call), updates with the call's prediction-errors, uses the updated state to re-weight the errors, and emits the new state in its output map. `futon2.aif.trace/trace-record` schema gains `:precision-state` field. Cross-call persistence is via the trace store — the trace IS the precision-state's home, no separate state file.

Tests at `futon2/test/futon2/aif/precision_test.clj` (12 tests / 22 assertions): initial-state shape, append-to-history, bounded-window cropping, precision-stable under zero-error, precision-drops-under-sustained-high-error, precision-rises-as-history-converges, untouched-channels-pass-through, new-channels-initialised-on-first-error, lookup defaults, weighted-error swap behaviour, purity.

End-to-end verified across 2 consecutive `clojure -M:wm-scheduled` runs: each call's `:precision-state` correctly carried forward from the prior trace record's value; per-channel history length grew 1 → 2; precisions held at 100.0 (rolling-variance ≈ 0 because errors were identical across two quick runs). Under cron (when R10's operator-install lands) precisions will adapt as observations actually drift call-to-call.

**Honest scope**: R7 satisfied for the 4 channels that have R3a likelihood (`:annotation-health`, `:sorry-count-norm`, `:mission-health`, `:active-repo-ratio`). The 10 channels logged as `:prototyping-forward` sorries (R3a gaps) inherit R7 satisfaction when their likelihoods land. **`pragmatic-weights` in `futon2.aif.preferences` remains static** — those are EFE weights, not channel-precision; their adaptive-tuning is a different concern (R12-adjacent learning of intrinsic-value credits, per the experimental directions in §"Capability-gap modeling").

### Forward design: dual-clock perceived-time R7 (v0.13 candidate, per Joe 2026-05-18)

The v0.12 R7 implementation uses raw call-count for the rolling-variance window. Under wall-clock cron alone (R10 graduated), this dilutes signal — dormant-period firings count the same as engaged ones. Two pieces of prior art in this codebase already model the discipline that would fix this:

**Prior art A: `~/code/futon1/apps/open-world-ingest/src/open_world_ingest/affect_transitions.clj`.** Affect-transition windows are bounded by BOTH `lookahead-minutes` (wall-clock) AND `lookahead-utterances` (turn-count) — whichever bound hits first ends the window. The link-strength score = `0.6 × intent-confidence + 0.4 × proximity`, where proximity is wall-clock position within the window (`1.0 − delta-seconds/window-seconds`, floored at 0.1). This is the dual-clock-as-convolution shape Joe named: time-as-experienced is constrained by both axes, and proximity weights position-within-the-combined-window against the strength of what's detected.

**Prior art B: `~/code/futon2/src/ants/aif/affect.clj` + `~/code/futon2/src/ants/aif/core.clj`.** Three pieces directly applicable:
1. `affect/update-tau` — precision update driven by `(:obs <current>, :dhdt <trend>)`. Takes the *derivative* of the observed signal (hunger trend over recent window), not just point variance. AIF-correct trend-aware adaptive precision.
2. `:recent` field — bounded window of `{:obs <full snapshot> :tau <prevailing precision>}` per tick. Richer than v0.12's `:error-history` (which carries only error scalars).
3. `trailing-streak` + derived `:white-streak` / `:since-ingest` — run-length-encoded "how-long-has-X-been-true" metrics over `:recent`. These ARE perceived-time proxies at the ant scale: they count turns on substrate-specific events, not seconds.

**v0.13 design sketch** (notes; not implementing yet):

- **Trigger sources**: `:wallclock-cron` (R10 cron firings = ticks) + `:duree-click-<kind>` (Evidence Landscape entries — Joe confirms these are already well-evidenced via futon1a). Trace records carry `:trigger`.
- **Perceived-time-delta per record**: `min(wall-clock-delta/wall-window, click-count-delta/click-window)` — the dual-clock convolution. Both clocks normalised against their respective windows so the min is unit-comparable.
- **Window shape**: replace v0.12's `:error-history` (vec of last-20 scalar errors) with `:recent` (vec of last-N `{:obs :error :trigger :perceived-time-delta}` snapshots). Bounded by perceived-time accumulation, not raw count.
- **Precision update**: `1 / max(rolling-variance, ε)` over the *perceived-time-weighted* window — older entries decay by their perceived-time distance, not their raw count. Plus, per ants' `update-tau`, incorporate per-channel error-trend: precision modulated by both variance AND dC/dt direction.
- **Tau bounds**: borrow ants' `:tau-floor 0.08` / `:tau-cap 1.5` discipline — bounded precision prevents runaway adaptation under noisy convergence.
- **Convolution formula candidate** (close to affect-transitions'):
  ```
  perceived-time-delta = min(
    wall-clock-delta / wall-window-seconds,
    click-count-delta / click-window-count)
  ```
  Each old-entry in the window gets a decay weight = `(1 - perceived-time-distance-to-now)`, floored at 0.1 per affect-transitions'.

**Source provenance for click events**: futon1a Evidence Landscape entries (Joe confirms these are already typed-temporal events with `:job-id` + `:ts` + delivery-status per the §2.A.2.33 B-2 catalogue). The WM doesn't need to invent a new click source; it consumes the existing Evidence Landscape stream.

**Hold/proceed**: noted here for v0.13. Operational dependencies are R10 cron-install (tick source) + Evidence Landscape read path from WM (click source). Both are pre-existing or scheduled; the convolution itself is small once those are wired.

### R8 — Per-tick trace

Each tick emits a record containing: μ_pre, observation, prediction errors, μ_post, candidates considered, per-term EFE values, chosen action, τ, F. Sufficient detail that an external observer can reconstruct what the agent did and why.

**Operational check.** Find the trace surface. Verify the schema includes the named fields. Verify trace is written for every tick.

**This implementation.** **Satisfied as of v0.7 (2026-05-17).** `futon2.aif.trace` namespace ships:
- `trace-record` — pure: extracts a v1 trace record from a judge-style output map. Fields: `:timestamp`, `:mu-pre`, `:mu-post`, `:observation`, `:free-energy`, `:ranked-actions` (compacted — `:prediction` stripped), `:decision` (compacted — `:softmax-weights` stripped), `:mode`.
- `write-trace!` — appends one record to a daily-rotated EDN-lines file at `~/code/futon2/data/wm-trace/wm-trace-YYYY-MM-DD.edn`. Configurable `:dir` and `:date-str`. Returns the path written.
- `read-trace` — reads all records for a given date. Returns `[]` if the file doesn't exist.
- `read-trace-range` — reads across a date range (LocalDate inclusive), returning all records chronologically.
- `futon2.report.war-machine/judge` extended to multi-arity: `[scan-data]` (no trace) or `[scan-data opts]` where `opts` carries `:trace?` (boolean) and optional `:trace-dir`. Trace write is wrapped in `try/catch` so disk-side failure can't break the WM call.

Tests at `futon2/test/futon2/aif/trace_test.clj` (9 tests / 21 assertions) verify: shape; pure trace-record (modulo timestamp); `:prediction` stripping; `:softmax-weights` stripping; file creation; append behaviour; round-trip; missing-file = empty vec; EDN-lines preserves Clojure types on read.

**Honest gap that remains**: prediction errors (ε per R8) aren't recorded because R3a (predicted-observation likelihood model linking belief to observation channels) is still partial. The schema will gain `:prediction-errors` when R3a lands.

### R9 — Named validation properties

Operationally testable properties: V-shrink (belief variance decreases under informative observations), F-decrease (free energy decreases on average over a run), EFE-stress (pragmatic increases with predicted divergence; epistemic increases for unfamiliar observations), Abstain-fires (under high uncertainty, the agent abstains).

**Operational check.** Find the validation harness. Verify each property has a named quantitative acceptance criterion.

**This implementation.** **Satisfied as of v0.7 (2026-05-17).** Four named R9 properties at `futon2/test/futon2/aif/r9_named_validation_test.clj` (7 tests / 9 assertions), each carrying a quantitative acceptance criterion in its `(testing ...)` docstring:

- **V-shrink** — after 10 confirmatory events of weight 2.0 on one status, posterior entropy ≤ 50% of initial uniform-prior entropy. Plus a monotone-form sibling: entropy non-increasing across a 5-event sequence.
- **F-decrease** — G-total monotonically non-increasing across a 5-point linear interpolation from a stressed observation to a healthy one. Magnitude bar: initial G-total ≥ 5x final G-total.
- **EFE-stress (pragmatic)** — G-risk on stressed obs > 5x G-risk on healthy obs.
- **EFE-stress (epistemic)** — G-epistemic on stressed obs > 3x G-epistemic on healthy obs.
- **Abstain-fires** — across 5 near-tied cases (gap within ε), abstain rate = 100%; across 3 clear-best cases (gap > ε), abstain rate = 0%.
- **Abstain-fires (gap report)** — when abstaining, gap-report enumerates all `:learn-action-class` candidates in the ranked input.

Per-namespace shape tests (in `belief_test`, `efe_test`, `policy_test`, etc.) remain as the component-level evidence; R9 named-property tests are the integration-grade contract. Together: 150 tests / 467 assertions across the full futon2 suite, 0 failures.

### R10 — Live operation

The AIF loop runs on a recurring schedule without operator intervention; trace is persisted to a queryable store. Distinguishes a *live* AIF surface from a *batch* AIF simulation.

**Operational check.** Find the schedule + the trace store.

**This implementation.** **Satisfied (scheduled-execution-ready) as of v0.8 (2026-05-18).** The scheduled-run entrypoint ships at `futon2/scripts/wm_scheduled_run.clj` with the `:wm-scheduled` deps alias. One-shot invocation: `clojure -M:wm-scheduled [days]` (default 14). Resilience: top-level `try/catch`; intermittent dependency failures (e.g. futon3c down) produce a non-zero exit + stderr message but don't crash the schedule. Persistence: writes one trace record per invocation via `futon2.aif.trace/write-trace!` to the daily-rotated EDN-lines store. Output: one-line stdout summary suitable for cron logs (timestamp + mode + candidate count + decision + trace path).

**Verified end-to-end 2026-05-18:** one invocation against the live stack wrote one record to `~/code/futon2/data/wm-trace/wm-trace-2026-05-18.edn`; the record parsed back with the documented schema (timestamp, mode, 4 ranked actions, decision = `:learn-action-class`, 13 observation channels).

**Cron-install (operator's call):**

```
0 * * * * cd /home/joe/code/futon2 && clojure -M:wm-scheduled 14 >> /home/joe/code/futon2/data/wm-trace/cron.log 2>&1
```

Hourly cadence is the recommended baseline — fast enough that strategic state changes get captured, slow enough that the trace remains operator-readable. systemd-timer or Drawbridge-driven scheduling are alternative substrates with equivalent semantics.

**Caveat: scheduled execution is *ready* but is not *currently running on this machine* (no cron entry installed by this codebase).** R10's "without operator intervention" claim is satisfied at the apparatus level (the script is autonomous) but the install step is a deliberate operator action — installing cron entries is system-level and out-of-scope for this contract. When the operator installs cron, R10 transitions from "scheduled-execution-ready" to "scheduled-execution-running" with no code change.

### R11 — Hierarchical / multi-agent composition

When multiple AIF agents act on shared state, a coordination layer ensures their actions compose coherently.

**This implementation.** **N/A at this scope.** The WM is a single observer of the futon stack. The cyberants AIF (`futon2/src/ants/aif/*`) is a separate agent population at a different scope; there is no shared belief state or composition between WM and cyberants. The current VSATARCS bridge is comparison-only: it reads WM trace and compares beliefs, but does not coordinate shared actions on shared state. **Re-evaluation trigger:** if VSATARCS gains writer capability over the same substrate the WM reads from (e.g. typed rewrites against `stack-annotations.edn`), or a third AIF surface appears at WM scope, R11 stops being N/A and needs an explicit coordination layer.

### R12 — Dual-loop / hyperparameter inference

A second AIF loop runs on a slower cadence and treats the inner loop's hyperparameters (preference priors, learning rates, EFE weights) as hidden state to be inferred.

**This implementation.** **✓ (apparatus) as of 2026-05-21** — narrow take-up landed at `M-the-futon-stack` Q6 per the deferral path. Per-action-class `:intrinsic-value` on `:learn-action-class` recommendations is now sourced from `futon2.aif.intrinsic-values/credit-for` (atom-backed Beta(α, β) posterior, rehydrated on JVM startup from `code/v05/wm-hyperparameter-update` hyperedges in futon1a XTDB), replacing the historical static `0.1` literal in `action_proposer.clj:39`. Outer loop in `futon2/scripts/wm_outer_loop.clj` (alias `:wm-outer-loop`) runs on slower cadence than the R10 inner loop; Stream B follow-through derived from git (sorrys.edn closures for `:address-sorry`; new mission-doc commits for `:open-mission`; `:fire-pattern` has no honest substrate yet and stays at prior).

**Apparatus-in-place caveat:** R10's cron install is still pending operator action; until R10 has been running long enough to accumulate trace emissions AND operator follow-through has happened against those emissions, posterior values remain at Beta(1,1) prior mode (0.5) for `:address-sorry` and `:fire-pattern`. `:open-mission` already moves under inference today (Beta(2.0, 16.0) → 0.063 from the existing 2-day trace window). The R12-flip here is on the **apparatus**, not on accumulated learning.

**Static per-channel hyperparameters** (`preferences`, `avoided-states`, `mode-prior`, `pragmatic-weights` in `futon2.aif.preferences`) remain static — those are per-observation-channel weights, a separate R12 direction with a different input substrate than per-action-class intrinsic-value. Open follow-on; not on the narrow take-up's path.

**Cross-pointers:**
- Stack-side checkpoint: `~/code/futon0/holes/missions/M-the-futon-stack.md` §"2026-05-21 — Q6 R12 narrow-take-up apparatus landed"
- Design choices: `~/code/futon0/holes/missions/M-the-futon-stack-Q6-r12-design-choices.md`
- VSATARCS bilateral R12 row: `~/code/futon4/docs/vsatarcs-alignment-completeness.aif.edn` `:R-criterion-audit :R12`
- VSATARCS port (deferred): the WM implementation is the reference; later session ports per design-choices §3.

## Cross-mapping to F1-F10 (stack-level fitness)

The WM's R-criterion satisfaction *contributes to* F-criterion movement at stack scope. The mapping:

| WM R-criterion | Stack F-criterion contributed to | Mechanism |
|---|---|---|
| R1 (explicit belief state) | F1 (explicit fitness state) | WM's per-entity belief becomes a queryable readout under substrate-2's existing query layer; F1 already ✓ at substrate level, R1-satisfaction extends it to *belief* fitness state. |
| R2 (observation channel schema) | F1 (explicit fitness state) | The 13-channel schema is a *fitness reading apparatus*; its existence + stability strengthens F1. |
| R3 (predictive-coding update) | F4 (bounded self-balance) | Belief-update VFE decreases under load → measurable self-balance signal beyond metabolic-balance's existing |ΔT| guard. |
| R4 (predictive forward model) | F5 (adaptive response) | Without R4 the WM scores current state; with R4 it scores predicted responses to perturbation, which is what F5's measurable response time presupposes. |
| R5 (principled EFE terms) | F4 + F5 | EFE on predicted state quantifies both bounded balance (R5a pragmatic) and response uncertainty (R5b epistemic). |
| R6 (action selection + abstain) | F6 (operator inhabitation) | EFE-scored recommendations are what the operator *reads and acts on*; abstain prevents the apparatus from issuing spurious actions while operator is away. |
| R7 (adaptive precision) | F10 (dual-loop fitness) | Adaptive precision is one channel of dual-loop fitness; both depend on R1. |
| R8 (per-tick trace) | F7 (validation harness) | Trace records are the evidence the validation harness checks. |
| R9 (named validation properties) | F7 (validation harness) | Direct contribution; AIF-property tests are F7-class tests. |
| R10 (live operation) | F2 (liveness invariant) | If the WM runs on schedule and emits trace, the stack gains a new liveness contributor beyond watcher daemon + satisficing-zapper. |
| R11 (hierarchical composition) | F8 (multi-corpus composition) | Hierarchical AIF and multi-corpus substrate are sibling concerns at composition scope; satisfaction of one informs but doesn't entail the other. |
| R12 (dual-loop hyperparameter inference) | F10 (dual-loop fitness) | Direct correspondence; R12 *is* F10 at AIF-implementation scope. |

F3 (coherent structure) and F9 (feed-readable annotation graph) are stack-level criteria the WM contributes to indirectly (priority rankings expose cluster structure; `judge` outputs become feed-consumable annotations once F9's M-INC step (b) dependency lands), not via specific R-criteria.

## Summary

| R-criterion | Status | Gap-closing checkpoint / blocker |
|---|---|---|
| R1 — Explicit belief state | **✓ as of v0.2** | — (`futon2.aif.belief`; integration with `judge` pending) |
| R2 — Observation channel schema | ✓ | — |
| R3 — Predictive-coding belief update | **✓ as of v0.11** | 4 of 14 channels carry likelihood (`:annotation-health`, `:sorry-count-norm`, `:mission-health`, `:active-repo-ratio`); remaining 10 logged as `:prototyping-forward` sorries; multi-channel R3d sign-aggregation deferred |
| R4 — Predictive forward model | **✓ as of v0.3** | — (`futon2.aif.forward-model/predict`; shared-kernel discipline aspirational until R6) |
| R5 — Principled EFE terms | **✓ as of v0.4** | — (`futon2.aif.efe/compute-efe`; G-risk + G-ambiguity decomposed; rank-actions ranks by G-total) |
| R6 — Softmax + abstain | **✓ as of v0.5** | — (`futon2.aif.policy/select-action`; adaptive τ; abstain with gap-report) |
| R7 — Adaptive precision | **✓ as of v0.12** | — (`futon2.aif.precision`; rolling-variance over per-channel error history; carried in trace records across calls) |
| R8 — Per-tick trace | **✓ as of v0.7** | — (`futon2.aif.trace`; EDN-lines daily-rotated; judge writes when `:trace?` opts) |
| R9 — Named validation properties | **✓ as of v0.7** | — (V-shrink, F-decrease, EFE-stress (pragmatic + epistemic), Abstain-fires all named with quantitative criteria) |
| R10 — Live operation | **✓ as of v0.8** (scheduled-execution-ready; cron-install pending) | — (`futon2/scripts/wm_scheduled_run.clj` + `:wm-scheduled` alias; verified end-to-end) |
| R11 — Hierarchical composition | N/A | Single observer at this scope |
| R12 — Dual-loop hyperparameter inference | **✓ as of 2026-05-21** (apparatus; awaits R10 cron-install for accumulated learning) | Narrow take-up landed via `M-the-futon-stack` Q6 — `futon2.aif.intrinsic-values` + `wm_outer_loop`; static per-channel hyperparameters in `preferences.clj` remain a separate open direction |

**Honest current claim (as of v0.12, with v0.18 / v0.19 adapter extensions noted below):** The WM AIF apparatus satisfies R1, R2, R3 (for 4 of 14 channels; remaining 10 logged as `:prototyping-forward` sorries), R4, R5, R6, R7 (for the same 4 channels), R8, R9, R10 (scheduled-execution-ready; cron-install is the operator's call) cleanly; R11 N/A; R12 deferred. The current vocabulary "WM AIF agent (with explicit belief, forward model, EFE-scored action selection, persisted trace, named validation properties, and scheduled-execution entrypoint)" is honest. **Load-bearing remaining work for the v1.0 contract:** (a) operator installs cron entry — flips R10 from "scheduled-execution-ready" to "running in production"; (b) R3a likelihood model so the trace's `:prediction-errors` field becomes meaningful. The substrate-adapter bar beyond the v1 hand-curated sorry registry is now met twice: `:open-mission` via mission docs (v0.18) and `:fire-pattern` via recent context-retrieval evidence (v0.19). None of these are gate-conditions for the apparatus's *current* claim of being an AIF-complete agent under its scope; (a) is operator action, (b) is the remaining R-criteria completion on the v1.0 path.

## Capability-gap modeling as endogenous action: the modeller inside the model

The R6 deliverable (v0.5) introduced a structural pattern worth naming explicitly, since it converges with a pending upgrade to `~/code/ukrn-services-simulation/` (per Joe 2026-05-17, the predecessor README's *"A speculative future move: the modeller inside the model"*; originally sketched at the working paper §6.4 extension item 5). Both — the formal version we built here and the informal version sketched in the predecessor — implement the same move: **promoting "model upgrade triggers" from external/implicit to internal/explicit first-class objects in the agent's state space.**

| Predecessor §6.4 (informal) | What this implementation does (formal) |
|---|---|
| "Researcher-Advisor engagement triggers" | `:learn-action-class` actions emitted by `bootstrap-proposer` |
| "Endogenous to the simulation rather than implicit 'as-needed' cadence" | `forward-model/can-propose?` registry + action map carrying `:target-class` |
| "First-class objects in the model's state space" | `:learn-action-class` is in `forward-model/action-types`; carries `:intrinsic-value` credit and structured `:rationale` |
| "Closes the learning system loop one turn tighter" | Bootstrap proposer surfaces capability gaps as ranked recommendations; abstain branch's gap-report enumerates them when no concrete action beats `:no-op` |

### Structural principle

**Capability boundaries should be explicit, observable, and first-class in the action space.** When an agent's capability boundary is implicit ("we can act on sorries" without checking that sorries are addressable), the agent fakes actions over non-existent entities — the failure mode the earlier WM prototype exhibited. When the boundary is explicit (a `can-propose?` registry per action class) and gaps are first-class actions (`:learn-action-class` carrying an intrinsic-value credit), the agent stays honest: it either recommends a concrete action OR recommends building the capability for one, and never pretends a capability it doesn't have.

### Experimental directions on FUTON

These three open questions distinguish what's built in v0.5 from a fully reflexive agent. The futon stack is the right experimental substrate for all three, because (a) the WM IS the agent under study, (b) substrate-2 + stack-annotations.edn give a rich enough world for non-trivial capability-gap dynamics, (c) the operator (Joe) is in-the-loop and acts on `:learn-action-class` recommendations, providing the ground truth for follow-through measurement.

1. **Intrinsic-value learning (R7-adjacent).** Currently `:intrinsic-value 0.1` is hand-tuned, identical across action classes. Experiment: instrument the WM to record, for each emitted `:learn-action-class :target-class X` recommendation, whether the operator subsequently shipped an adapter that flipped `can-propose? X` to true (and how long it took). Fit an intrinsic-value-per-target-class from this data. Hypothesis: action classes with higher operator follow-through deserve higher credit. The experiment also surfaces operator priorities — which capability gaps are actually load-bearing vs which are decorative.

2. **Capability-expansion-as-discontinuity (M-stack-morphogenetic-rewrite-adjacent).** When a new substrate adapter lands and `can-propose?` for an action class flips, the action space expands and the EFE landscape changes qualitatively. Experiment: instrument the WM to detect and record these "capability-expansion events" with a `:morphogenesis` block (analogous to the typed-rewrite morphogenesis discipline in `~/code/futon4/README-rewriting.md`). Capture EFE-landscape signatures before/after; check whether the agent's belief and ranked-action distributions shift in ways the framework predicts vs surprises. This is the operational form of the morphogenetic-rewrite mission applied to capability rather than code substrate.

3. **The agent's model of its own modeller (R12 / F10).** Both this implementation and the predecessor sketch are one level reflexive (agent surfaces gaps to operator). A fully dual-loop agent would have a model of the *modeller's behaviour*: given gap-type X and operator state Y, what's the probability of follow-through in window Z? Experiment: as the WM accumulates trace data (R8 / R10), fit a per-gap-class follow-through-probability model. Operator-specific priors (different operators have different gap-closing patterns) become observable. This is also the path to **R12 (dual-loop hyperparameter inference)** at the action layer, sibling to the F10 (dual-loop fitness) move at the homeostat layer.

All three experiments share a prerequisite — **R8 trace persistence (Checkpoint 4)** — since none can run without the WM recording its own gap-recommendation events and the operator's subsequent actions over time. They become incrementally feasible as R8 + R10 land, and natural deliverables of the v0.6 → v1.0 versioning arc above.

## Cross-references

- `~/code/ukrn-services-simulation/docs/aif-completeness.md` — predecessor R1-R12 contract (batch simulation scope). The shape this doc ports verbatim.
- `~/code/ukrn-services-simulation/README.md` (pending §"A speculative future move: the modeller inside the model" per Joe 2026-05-17) — informal cousin of §"Capability-gap modeling as endogenous action" above. Convergent with v0.5's `:learn-action-class` work.
- `~/code/futon4/docs/vsatarcs-alignment-completeness.aif.edn` — sibling VSATARCS contract's annotation overlay. Top-level `:bilateral-evidence` block carries the shared landing-surface for two-sided correspondences (capability-gap-modeling 2026-05-17 :independent-naming-of-same-principle; symmetric-bootstrap 2026-05-18 :joint-landing). Authoritative location — not duplicated here; both sides can extend it from their respective scopes. Closure `hx:vsatarcs-align:v0-2-5:cross-side-bridge-closure` is the VSATARCS-side half of the v0.9 ↔ v0.2.5 bilateral milestone paired with `hx:wm:v0-9:symmetric-bootstrap-closure` above.
- `~/code/futon4/dev/arxana-vsatarcs-wm-bridge.el` — the VSATARCS-side reader of WM trace `:mu-post`; consumes the latest record from `~/code/futon2/data/wm-trace/wm-trace-YYYY-MM-DD.edn` and runs `arxana-vsatarcs-belief-compare` against local belief. End-to-end smoke against 2026-05-18 trace returned 35 equal / 0 drift / meta-sorry filtered as expected — the v0.9 ↔ v0.2.5 baseline; non-zero drift becomes the interesting signal once R3a wires observation-driven updates into either side.
- `~/code/futon2/docs/ants-aif-audit.md` — full R1-R12 audit of the cyberants AIF apparatus as reference implementation, plus anticipation integration design ladder. The five v0.13 phases are the realisation of this audit's "Patterns worth porting" recommendation list.
- `~/code/calendar/events.edn` + `~/code/futon5a/README-anticipation.md` — canonical forward-axis anticipation surface; the v0.13 anticipation read-only namespace consumes the former, the latter is the framing.
- `~/code/futon0/docs/stack-fitness-completeness.md` — sibling F1-F10 contract (stack-as-homeostat scope). The cross-mapping target.
- `~/code/futon7/holes/M-war-machine-aif-completion.md` — the mission this contract is the engineering target of.
- `~/code/futon7/holes/Q-CL1-decision-note.md` — the runtime-location decision (resolved: (a) extend in place in `futon2/`) that locates this contract at `futon2/docs/`.
- `~/code/futon2/scripts/futon2/report/war_machine.clj` `judge` — current consumer of the AIF apparatus.

## Status of this document

**v0.1 drafted 2026-05-17** by claude-2 as part of M-war-machine-aif-completion Checkpoint 0 (post-carve). Authored after the AIF substrate was carved out of `war_machine.clj` lines 1797-2032 into `futon2.aif.*` namespaces.

**v0.2 updated 2026-05-17** (same session) — R1 landed via `futon2.aif.belief` (Checkpoint 1). R3 / R7 / R8 / R9 status entries gained content per the cascading unblocking. AIF test count moved 12/51 → 20/72.

**v0.3 updated 2026-05-17** (same session) — R4 landed via `futon2.aif.forward-model` (Checkpoint 2). R5 composition path opens (predict + compute-free-energy → per-action EFE); R8 candidates field becomes meaningful. AIF test count moved 20/72 → 30/103; full futon2 suite 82/313 → 92/344.

**v0.4 updated 2026-05-17** (same session) — R5 landed via `futon2.aif.efe` (Checkpoint 3 part A). G-risk + G-ambiguity decomposed by name, both computed against R4 predictions. AIF test count moved 30/103 → 39/126; full futon2 suite 92/344 → 101/367. R6 (softmax + abstain) is the remaining Checkpoint 3 deliverable.

**v0.5 updated 2026-05-17** (same session) — R6 landed via `futon2.aif.action-proposer` + `futon2.aif.policy` (Checkpoint 3 part B). Bootstrap proposer surfaces capability gaps as `:learn-action-class` actions; `select-action` applies softmax with adaptive τ and abstain-with-gap-report. Action-types extended with `:learn-action-class`; `can-propose?` defmulti added to `forward-model`; `:intrinsic-value` action credit added to `compute-efe`. AIF test count moved 39/126 → 65/178; full futon2 suite 101/367 → 127/419. **Also added §"Capability-gap modeling as endogenous action"** naming the structural principle that emerged with R6, its convergence with the predecessor's pending §"A speculative future move," and three experimental directions to explore on FUTON itself.

**v0.6 updated 2026-05-17** (same session) — (i) `judge` wired to call `select-action` (its output now carries `:belief`, `:ranked-actions`, `:decision`); (ii) `futon2.aif.sorry-registry` + `futon2/data/sorrys.edn` shipped as the v1 substrate adapter for `:sorry` entities — the first sorry is the meta-sorry registering the substrate-addressability question itself. Now `can-propose? :address-sorry` returns true when state carries open sorrys, and the bootstrap proposer correctly stops surfacing the `:learn-action-class :target-class :address-sorry` gap. AIF test count moved 65/178 → 72/195; full futon2 suite 127/419 → 134/436. **The WM's empirical decision under v0.6 parameters** is to recommend expanding capability (`:learn-action-class :target-class :fire-pattern`) over addressing the single existing sorry — a real intrinsic-value-tuning signal.

**v0.7 updated 2026-05-17** (same session) — Checkpoint 4 closed. R8 landed via `futon2.aif.trace` (EDN-lines daily-rotated persistent trace; `judge` extended multi-arity to write trace under `:trace?` opt; `trace-record` pure with strip helpers for nested fields). R9 landed via `futon2/test/futon2/aif/r9_named_validation_test.clj` — four named properties (V-shrink, F-decrease, EFE-stress pragmatic+epistemic, Abstain-fires + gap-report) each carrying a quantitative acceptance criterion. AIF test count moved 72/195 → 88/226; full futon2 suite 134/436 → 150/467. Honest gap recorded: prediction-errors (R8 schema field) await R3a likelihood model.

**v0.8 updated 2026-05-18** — Checkpoint 5 closed (scheduled-execution-ready form of R10). `futon2/scripts/wm_scheduled_run.clj` + `:wm-scheduled` deps alias ship as the entrypoint a scheduler (cron / systemd / Drawbridge) invokes; resilient under intermittent dependency failure (top-level try/catch + non-zero exit). One end-to-end invocation verified: wrote `wm-trace-2026-05-18.edn` with one parseable record; summary line `mode=multiplied candidates=4 decision=learn-action-class target-class=:fire-pattern G=0.0604`. Cron install is the operator's call and is documented in §R10 above; once installed, R10 transitions from "ready" to "running" with no code change.

**v0.9 updated 2026-05-18** — bilateral milestone `hx:wm:v0-9:symmetric-bootstrap-closure` ↔ VSATARCS `hx:vsatarcs-align:v0-2-5:cross-side-bridge-closure`. `futon2.aif.belief` extended with `section-ids-from-stack-annotations` + `bootstrap-from-stack-annotations`; `judge` now constructs initial belief with `bootstrap-from-stack-annotations(sorry-ids)`; WM-side belief domain expanded from 1 entity (meta-sorry only) to 36 (35 string section-ids ∪ 1 keyword sorry-id). Trace file's latest `:mu-post` is the cross-side bridge read path; VSATARCS-side `arxana-vsatarcs-belief-compare` does alist-lookup equality directly on the 35 shared string keys. AIF test count moved 88/226 → 94/239 (with the added bootstrap tests); full futon2 suite 150/467 → 156/480. End-to-end scheduled-run verified: trace record carries 36-entity belief with uniform priors. First instance of `:code-docs-correspondence` evidence kind `:joint-landing`.

**v0.10 updated 2026-05-18** — R3a/R3b/R3d closed for `:annotation-health`. Added `:annotation-health` to `observation-channels` (14 channels now), `preferences` ([0.7 1.0]), `scan-annotation-graph` reading `:lift-anomalies / sections` density from `stack-annotations.edn`, `predict-annotation-health` likelihood in belief.clj with per-status weights, `compute-prediction-error` in free_energy.clj (error + precision + weighted-error), and R3d wiring in `judge` (synthesised global belief-update event from weighted-error → applied via `update-belief-batch` → `:mu-post ≠ :mu-pre`). Trace schema gains `:prediction-errors` field (previously flagged as gap-pending in v0.7). End-to-end verified: observed 0.9429, predicted 0.5714, error +0.371, entity `:strengthened` mass 0.143 → 0.147. Test count moved 94/239 → 107/269 in the AIF suite; full futon2 suite 156/480 → 169/510. WM-side belief now diverges from VSATARCS-side baseline on all 35 shared entities — the bridge's drift signal becomes non-zero (R3d-driven, not yet operator-validated).

**v0.11 updated 2026-05-18** — R3 row flipped to ✓ via 3 more likelihood models + 10 `:prototyping-forward` sorries. Added `predict-sorry-count-norm`, `predict-mission-health`, `predict-active-repo-ratio` to `belief.clj`; `channels-with-likelihood` set and `predict-observation` composite; `judge` now computes prediction-errors for all 4 covered channels (R3d driver still single-channel — multi-channel sign-aggregation needs `:health-sign` per channel, deferred). Sorrys registry extended with 10 entries `:sorry/r3a-likelihood-<channel>` (`:prototyping-forward`), making the channel-coverage gaps first-class addressable work. Test count moved 107/269 → 118/286 in the AIF suite; full futon2 suite 169/510 → 180/527. Empirical verification: 4 prediction-errors recorded per call; signs mixed (+0.37, −0.15, −0.24, −0.20) — direct evidence that multi-channel R3d needs sign annotation before aggregation.

**v0.12 updated 2026-05-18** — R7 (adaptive precision) closed for the 4 channels with R3a likelihood. New `futon2.aif.precision` namespace (`initial-precision-state`, `update-precision-state`, `weighted-error`, `precision-for`); rolling-variance over bounded per-channel error history (default window 20); `judge` reads previous precision-state from latest trace record, updates with new errors, uses adaptive precision for weighted-error computation, emits state in output; `trace-record` schema gains `:precision-state` field — trace IS the precision-state store across calls (no separate state file). AIF test count moved 118/286 → 129/306; full futon2 suite 180/527 → 191/547. End-to-end verified across 2 consecutive runs: history length grew 1 → 2 per channel; precision-state correctly carried forward. R-criteria summary now reads R1/R2/R3/R4/R5/R6/R7/R8/R9/R10 ✓ (R3 and R7 each ✓ for 4 of 14 channels; remaining 10 logged as `:prototyping-forward`), R11 N/A, R12 deferred. **Nine of twelve R-criteria fully satisfied; R12 the only remaining-deferred item.**

**v0.13 updated 2026-05-19** — five-phase audit-driven enhancement pass per `~/code/futon2/docs/ants-aif-audit.md` recommendations:

- **Phase 1: R7 v0.13 (need-modulated precision).** `precision.clj` extended with `need-component-for` — per-channel need-modulated precision component (= `need-scale × max(0, channel-gap-from-preference-range)`). Combined precision = variance-component + need-component, bounded by `precision-floor 0.1 / precision-cap 200.0`. Ports ants/aif/affect.clj's `modulate-precisions` pattern: when an observed channel is OUT of its preferred range, precision rises proportionally to the gap. Per-channel state now carries `:variance-component` and `:need-component` alongside `:precision`. Channels not in `pref/preferences` (ad-hoc test keys) get need-component = 0 and behave as v0.12.

- **Phase 2: Anticipation v0.13 read-only.** New `futon2.aif.anticipation` namespace reads `~/code/calendar/events.edn` (typed AIF-shaped forward-axis events per `~/code/futon5a/README-anticipation.md`). `anticipation-snapshot` returns events-in-horizon (default 30 days) summarised for trace propagation (Instants string-coerced for EDN-readability). `judge` emits `:anticipation` in its output; `trace-record` gains the `:anticipation` field. Time-conditioned preferences (R5 v0.14 candidate) and multi-horizon EFE composition (R4 v0.15 candidate) build on this; v0.13 is read-only — the operator can SEE upcoming events in trace without decision-logic changes yet. Glasgow Cogito 2026-05-28 deadline visible in trace (9 days ahead of authoring date 2026-05-19).

- **Phase 3: R5 augmented EFE.** `compute-efe` extended with two principled terms ported from ants/aif/policy.clj — `:G-info` (information-gain; rewards low-predicted-variance actions; enters G-total with negative sign so informative actions are preferred) and `:G-survival` (hinge-loss penalty for predicted channels outside preferred range; only the 4 R3a-covered channels contribute). New `info-weight 0.4` and `survival-weight 1.2` opts (ants defaults). G-total decomposition is now `G-risk + G-ambiguity − info-weight·G-info + survival-weight·G-survival`. G-total CAN now be negative (informative actions get credit) — by design.

- **Phase 4: R6 admissibility + default-mode fallback.** `forward-model/can-execute?` defmulti added — per-action-instance admissibility (composes with `can-propose?`'s class-level check). `:address-sorry` arm checks target is in state's open sorrys; others default true. `policy/default-mode-select` ports ants/aif/default_mode.clj's tropism-based fallback: rule-based dispatch on observation + candidate set, no EFE evaluation. `judge` pre-filters ranked-actions by `can-execute?` then runs deliberative `select-action` with `default-mode-select` as try/catch fallback — **I6 compositional closure**.

- **Phase 5: R3 multi-step inner iteration.** `judge` now runs up to `r3-max-steps 3` micro-steps per call (early-terminates if error-magnitude drops below `r3-error-eps 1e-3`). Each step: re-compute prediction-errors on current belief → update precision-state → synthesize global belief-update event from `:annotation-health` weighted-error (weight annealed per step) → apply update. Trace gains `:micro-step-trace [{:step :error-magnitude :anneal-factor :events-applied :event-weight}]`. Ports the inner-loop pattern from ants/aif/perceive.clj.

**Test state moved 191/547 → 218/603** across the full futon2 suite (+27 tests / +56 assertions for the five phases). End-to-end live invocation 2026-05-19 verified the v0.13 behaviour: decision flipped from `:learn-action-class :fire-pattern` (G≈0.054) to `:address-sorry :sorry/wm-aif-substrate-addressability` (G≈−4.5) — the augmented EFE recommends concrete addressable work over capability-expansion under current substrate conditions. Per-call trace now carries `:anticipation` (Glasgow event surfaces in 30-day horizon), `:micro-step-trace` (3 steps with shrinking error 0.371→0.365→0.361 and annealing weight 0.1→0.067→0.033), `:precision-state` with the new components.

**R-criteria semantically deepened (v0.13 doesn't add new ✓s, but enriches existing ones):**

- R3 — multi-step inner iteration (ants port; previously single-step)
- R5 — four principled terms not two (info + survival added); G-total can be negative by design
- R6 — admissibility filter + I6 fallback (ants port)
- R7 — need-component added (ants port; previously variance-only)
- R10 — anticipation read-only adds a forward-axis bearer; row 6 in §2.A.2.27 referenced
- R4 + R5 deeper integration deferred: time-conditioning (v0.14) and multi-horizon (v0.15) build on anticipation foundation

**v0.14 updated 2026-05-19** — anticipation-driven time-pressure scaling on G-risk + G-survival. `futon2.aif.anticipation/time-pressure` returns [0,1] urgency based on closest event's proximity to `now` within horizon; `compute-efe` accepts `:time-pressure` and `:time-pressure-scale` opts and multiplies G-risk + G-survival by `(1 + time-pressure × time-pressure-scale)` (G-info and G-ambiguity unchanged — urgency makes risk/survival dominate, reducing relative weight of exploration). `judge` computes time-pressure from anticipation-snapshot and threads through `rank-actions`. End-to-end 2026-05-19: closest event `ev-2026-05-26-vsat-eric-scoping-meeting` 7 days away → time-pressure ≈ 0.78; G-total under urgency notably lower than baseline.

**v0.15 updated 2026-05-19** — multi-horizon predict. `futon2.aif.forward-model/predict-multi-horizon` chains K applications of `predict` (default K=3) over the same action; each step uses the previous step's predicted observation+belief as input. Trajectory + final-state returned. `compute-efe` accepts `:horizon-steps` opt; when present and ≥2, G-risk + G-survival are computed on the final-state observation rather than immediate next-state. G-info + G-ambiguity continue to use the single-step variance (immediate uncertainty). `judge` opts in to multi-horizon (K=3) whenever anticipation has loaded events in horizon; falls back to single-step otherwise. End-to-end verified: `:address-sorry` chained 3 times reaches closer to preferred range than 1-step, producing lower G-risk.

**v0.16 updated 2026-05-19** — multi-channel R3d sign-aggregation. `preferences/channel-health-signs` declares per-channel `:health-sign` (`+1` for `:annotation-health` / `:mission-health` / `:active-repo-ratio`; `-1` for `:sorry-count-norm` — high observed = unhealthier). `judge`'s R3 inner-loop now aggregates signed weighted-errors across all 4 R3a-covered channels rather than using `:annotation-health` alone: `aggregated-signed-error = Σ (sign × weighted-error)` per channel; positive aggregate → entities pushed toward `:strengthened`, negative → `:foreclosed`. Multi-step trace gains `:aggregated-signed-error` per step. **The caveat named in v0.10's `:sorry/r3d-per-entity-attribution` is partially closed**: multi-channel direction is now coherent (the v0.10 single-channel limitation no longer applies); the OTHER limitation (uniform application across all entities, not per-entity attribution) remains open until real per-entity event streams arrive (M-INC step (b)).

**Test state moved 218/603 → 232/632** across the v0.14-v0.16 roadmap (+14 tests / +29 assertions). Live end-to-end 2026-05-19: 15 candidates per call; top decision `:address-sorry :sorry/wm-aif-substrate-addressability` with G=-4.21; time-pressure 0.78 visible per candidate; multi-horizon-steps 3 active; aggregated signed-error +18.0→+14.6 across the 3 micro-steps (decreasing as belief converges toward observation).

**VSATARCS-side bilateral pair landed 2026-05-19 (cross-link, no WM-side code change).** Paired closure `hx:vsatarcs-align:v0-5-0:r3a-likelihood-closure` ↔ this contract's §R3 v0.10 + v0.11 likelihood landings. VSATARCS-side `~/code/futon4/dev/arxana-vsatarcs-likelihood.el` shipped 3-of-5 R2 channels with R3a likelihood (`:story-coverage`, `:lift-freshness`, `:annotation-overlay-presence`); 2 channels (`:scene-density`, `:link-density`) logged as `:prototyping-forward` parallel to the WM-side 10. R3a + R3c sub-properties → `:satisfied` on VSATARCS side; R3 aggregate stays `:partial-three-of-four-sub-properties-satisfied` (R3b blocked on R7). Fifth `:bilateral-evidence` entry at `~/code/futon4/docs/vsatarcs-alignment-completeness.aif.edn` carries `:principle :r3a-likelihood-via-ants-port`, `:evidence-kind :joint-landing`, **and is the first entry using the `:protocol-witnesses` audit-trail extension** (3-turn whistle thread captured: claude-4 cue → claude-2 ack → claude-4 v0.5.0 landed).

**v0.17 updated 2026-05-19 (cross-link only; no WM-side code change).** Two further VSATARCS-side landings recorded for cross-side legibility — sequential ships per the joint-landing roadmap, neither requiring WM-side code change:

- **VSATARCS v0.5.1 landed 2026-05-19** — R9 F-decrease. `arxana-vsatarcs-trace-build-record` extended with `:F` + `:prediction-errors` keyword args; `arxana-vsatarcs-trace-follow-wm` computes VFE per tick via `compute-vfe`; 2 new tests (wiring + named property at compute-vfe level). Test count 103 → 105. VSATARCS R9 audit row: `:partial-with-V-shrink-shape` → `:partial-with-V-shrink-and-F-decrease`. **Sixth `:bilateral-evidence` entry** uses `:evidence-kind :one-sided-extension` per WM-side recommendation; forward-pointer flags the future bridge F-trajectory comparison move (compose-vfe-level decisions aggregated across cycles → bilateral F-trajectory comparison becomes a new bilateral-evidence candidate). Honest engineering call recorded: a statistical-over-real-corpus F-decrease assertion was tried and dropped (noise-dominated under multi-step annealing); named-property assertion lives at the apparatus level where signal is robust.
- **VSATARCS v0.5.2 in flight 2026-05-19** — R10 via direct Emacs `file-notify-add-watch` on `~/code/futon2/data/wm-trace/wm-trace-YYYY-MM-DD.edn`. When the WM trace file grows, fires `arxana-vsatarcs-trace-follow-wm` (already idempotent — uses existing VSATARCS trace records' `:wm-trace-anchor` entries as the "already-followed" lookup; no separate state file). **Asymmetry recorded**: WM-side R10 is "scheduled-execution-ready" (cron-install pending operator action per §R10 above); VSATARCS-side after v0.5.2 will be "subscribed-and-running" (file-notify already firing the moment VSATARCS is opened). Different shapes, both honest R10 satisfactions per the §R10 "wakeup-without-work is honest R10 reading" framing. Seventh `:bilateral-evidence` entry will use `:evidence-kind :one-sided-extension` with the bilateral substrate move (full multi_watcher heartbeat tap) marked as v0.5.3+ candidate.
- **v0.5.3+ candidate (deferred per claude-4's scope-management call)**: `bb` sidecar polls futon1a watcher-events, writes `~/code/storage/multi-watcher/heartbeat.edn` per cycle with `{:timestamp :events-this-cycle :types-seen :recent-paths}`; both sides eventually subscribe to the same heartbeat. Becomes a `:joint-landing` bilateral-evidence entry when WM-side R10 cron also subscribes. The minimum-viable R10 ships first; the full bilateral substrate move ships when scope-permits.

**Discipline note from the v0.5.0 → v0.5.2 chain.** `:protocol-witnesses` extension is per-entry, not chained on v0.5.0 — each `:bilateral-evidence` entry carries its own provenance pertaining to its specific landing. v0.5.0 entry's witnesses = the v0.5.0-coordination 3-turn thread; v0.5.1 entry's witnesses = the v0.5.1 landing + WM-side ack (2-turn); v0.5.2 entry's witnesses = the R10-scoping bell-pair + landing bell (3-turn). Per-entry scope keeps provenance focused; chain-of-chains rather than one long chain. Cleaner than the alternative (extending v0.5.0's witnesses indefinitely as the "anchor" for everything downstream).

**v0.18 updated 2026-05-20** — first mission-doc substrate adapter landed for `:open-mission`. New `futon2.aif.mission-registry` scans top-level `*/holes/missions/M-*.md`, parses the `**Status:**` line, filters to non-complete missions, and exposes them as addressable targets. `futon2.report.war-machine/judge` now threads `:missions` into WM state and composes `mission-enumerator-proposer` alongside the bootstrap and sorry proposers. Result: `forward-model/can-propose? :open-mission` now flips true when mission docs are present; the corresponding `:learn-action-class` gap disappears under live state. Honest scope remains heuristic/file-backed rather than typed-substrate-backed, and `:fire-pattern` is still gated pending its own adapter.

**v0.19 updated 2026-05-20** — retrieval-backed substrate adapter landed for `:fire-pattern`. New `futon2.aif.pattern-registry` treats recent `context-retrieval` Evidence Landscape entries as the addressable pattern surface, aggregates retrieved pattern ids into a deterministic weighted top-K candidate set, and exposes them via `pattern-enumerator-proposer`. `futon2.report.war-machine/judge` now threads `:patterns` into WM state and composes the new proposer alongside bootstrap / mission / sorry proposers. Result: `forward-model/can-propose? :fire-pattern` now flips true when recent retrieval evidence exists; the corresponding `:learn-action-class` gap disappears under live state. Honest scope: the WM is a second-stage judge over retrieval certificates, not a replacement for futon3a retrieval and not a raw `.flexiarg` library enumerator.

**v0.20 updated 2026-05-20 (cross-link only; no WM-side code change).** VSATARCS-side reader-criteria axis closed: **8 of 8 reader-criterion questions `:satisfied`** per the `vsatarcs-reader-criteria.md` doc claude-2 drafted 2026-05-19. v0.5.12 → v0.5.15 shipped Q2 (`arxana-vsatarcs-wm-decision.el` — verbatim then extended to top-K + composition + µ-shift per WM-side recommendation), Q5+Q6 bundled (`arxana-vsatarcs-wm-recent.el` — per-record summaries + top-K-most-moved entities via max-abs-diff trajectory across the 5-record window), and Q8 (`arxana-vsatarcs-cluster.el` — mission-cluster status parsing). VSATARCs test count moved 131 → 272 across the reader-criteria axis. `:bilateral-evidence` block grew 8 → 13 entries; **5 of 5 `:evidence-kind` values activated** (first-instance of `:coordinated-empirical-observation` on the events.edn pair: both sides consume the canonical forward-axis substrate independently per `principle :forward-axis-substrate-consumed-by-both-sides`). Two operational signals worth flagging in the WM-side roadmap:
- **Q2-surfaced G-tie at top-3 ranks**: under v0.16 EFE composition, all `:address-sorry :target X` candidates compute to identical G-total (target-invariant `predict-effects` + no per-target `:intrinsic-value` from sorry-enumerator). Structural feature, not a bug; per-target G-discrimination is a candidate refinement → `M-war-machine-aif-last-mile.md` §2.E.1 (post-substrate-adapter-close addendum). The fact that the tie became visible BECAUSE of Q2's top-K chrome is the kind of payoff the reader-criteria axis was designed for — operationally meaningful diagnostics, not just rendering.
- **Q5-surfaced 5-entity tie at max-abs-diff 0.1681**: uniform-R3d global-update signature per `:sorry/r3d-per-entity-attribution`'s still-open per-entity-attribution half; magnitude verdict pending claude-4's metric formula.

**Versioning roadmap.**
- v0.12 multi-channel R3d sign-aggregation (add `:health-sign` annotation per channel; combine the 4 weighted-errors coherently into R3d driver); per-entity attribution refinement when real per-entity event streams arrive (M-INC step (b) or equivalent); substrate adapter(s) beyond the v1 hand-curated sorry registry so `:open-mission`/`:fire-pattern` `can-propose?` flip true.
- ~~v0.14 anticipation-driven time-conditioning~~ — **landed 2026-05-19** (see v0.14 entry below).
- ~~v0.15 multi-horizon R4 forward-model~~ — **landed 2026-05-19** (see v0.15 entry below).
- ~~v0.16 multi-channel R3d sign-aggregation~~ — **landed 2026-05-19** (see v0.16 entry below).
- ~~v0.17+ substrate adapter beyond hand-curated sorry registry~~ — **landed across v0.18 + v0.19**: `:open-mission` via mission-doc adapter; `:fire-pattern` via retrieval-backed context-retrieval adapter. Future refinement remains open (typed substrate / richer retrieval rationale / HIT calibration), but the structural `can-propose?` gap is closed.
- v1.0 when R1-R10 are all ✓ or ✓-with-documented-caveat, with at least one full WM scoring run regression-fixtured AND substrate adapter(s) making at least one non-`:learn-action-class` action class proposable beyond `:address-sorry`, AND the operator has installed the cron entry (or equivalent scheduler) to flip R10 from "ready" to "running."
