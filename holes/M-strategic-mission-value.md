# M-strategic-mission-value — make the strategic forward model discriminate on mission value

**Status:** IDENTIFY → handoff to codex-3, reviewed by claude-6.
**Owner mission:** `~/code/futon7/holes/M-war-machine-aif-completion.md`
**Date:** 2026-07-17. Operator: Joe. Ground Control: claude-6.

## Problem (verified against attempt-022)

The WM outer loop selected rank-9-by-G (`M-learning-loop`, nearly the *worst*
G) because the learned habit prior singled it out over an EFE core that barely
discriminates. Root cause, confirmed in code:

- Strategic `G-efe = risk + ambiguity` (`efe/compute-efe`), computed from
  `forward-model/predict`'s predicted next-observation.
- `predict-effects :advance-mission` (forward_model.clj:133) scales its
  obs-delta by `advance-mission-ordinal-factor(open-hole-count)` — i.e. the
  strategic generative model's *entire theory of mission value is hole count*
  (`N/(1+N)`). Two missions with equal hole count → identical prediction →
  identical G-efe.
- `predict-effects :fire-pattern` (forward_model.clj:177) has a **constant**
  obs-delta — zero target sensitivity — so pattern candidates tie *exactly*
  (attempt-022 ranks 1–3 all G-efe = 12.153909).
- Net: G-efe spans only 0.16 nats, selection collapses to the habit prior,
  which favours the most-previously-worked mission = stale-preference lock-in
  = a version of the AIF **Dark Room** problem.

## Design decision (settled with Joe)

This is the **strategic** level of the paper's two-level hierarchy (R13/R15,
`p4ng/main-2026.tex:438`): `G(a)` chooses *which* mission; the tactical `S(π)`
pattern-cascade rollout is a *separate* level. We fix the strategic forward
model **independently of** the tactical rollout (B, deferred).

Replace the hole-count mission-value theory with **tension × load-bearing**,
computed from signals that are progress-sensitive (anti-Dark-Room) and
contextual (not a local count):

- **tension(mission)** = phase-T from `futon3c.aif.mission-delta-t/delta-t-mission`
  (`phase-t-table`: head 1.0 → complete 0.0). Decays as the mission advances a
  phase — real work lowers its own pull.
- **centrality(mission)** = `c_joint` from the centrality map
  (`centrality-joint-map` in the judge; `forward-model-centrality-path`).
  "Load-bearing relative to context", per Joe — NOT hole count.
- **non-progress decay** = multiplicative penalty in (0,1]: if the mission was
  selected in a recent trace record and its belief `mu` did not move (or the
  outcome was not `:grounded-change`), decay its value. This closes the
  stuck-but-central Dark-Room door (high centrality + can't advance a phase →
  would otherwise keep its tension high forever). Suggested form:
  `1 / (1 + k · consecutive-non-progress-selections)`, k≈1, tunable.

`mission-value-factor = normalize(tension × centrality) × non-progress-decay`.

Because the forward model now predicts that advancing a high-tension,
load-bearing mission reduces free energy **more**, its risk (KL to preferences)
is genuinely lower → **strategic G-efe discriminates honestly**. This respects
`main-2026.tex:440` — G-efe stays pure `risk+ambiguity`; we are NOT smuggling
controller terms into it, and NOT touching the D-1d structural-pressure /
habit-prior relocation.

## Change points

1. **`scripts/futon2/report/war_machine.clj`** — add
   `enrich-candidates-with-mission-value` alongside
   `enrich-candidates-with-structural-pressure` (call site: the
   `wm-enriched-candidates` thread, ~line 4035). It attaches `:tension`,
   `:centrality`, and `:non-progress-decay` onto each `:advance-mission` /
   `:fire-pattern` action, sourcing them from `centrality-joint-map`,
   `compute-delta-t-mission`, and `prev-trace-record` (the same prev-trace the
   habit-prior state is read from, ~line 3812). All substrate reads live HERE.
   For `:fire-pattern`, source value from the pattern's `retrieval-score`
   and/or its related missions' tension×centrality.

2. **`src/futon2/aif/forward_model.clj`** — in `predict-effects :advance-mission`
   and `:fire-pattern`, consume a `:mission-value-factor` (or the raw
   `:tension`/`:centrality`/`:non-progress-decay`) from the action map and scale
   the obs-delta by it. Keep the function **pure** (action-map fields only; no
   substrate reads). Preserve `*effects-mode* :constant` → factor 1.0
   (byte-identical). **Fallback:** when the mission-value signal is absent on the
   action, fall back to today's `advance-mission-ordinal-factor(open-hole-count)`
   so un-enriched callers/tests are unchanged (regression-safe; the live arena
   always enriches, so live behaviour uses the new factor).

## Acceptance bar

- New test: two `:advance-mission` candidates with **equal** `open-hole-count`
  but different `(tension, centrality)` produce **distinct** predicted obs-delta
  and distinct `G-efe`. A candidate flagged non-progress scores strictly lower
  value than an otherwise-identical fresh one.
- New test: two `:fire-pattern` candidates with different retrieval/related-
  mission value no longer tie on `G-efe`.
- Judge-level check (or a fixture replaying attempt-022's candidate set):
  strategic `G-efe` is distinct across the mission candidates, and the
  top-G pick is no longer forced by the habit prior alone.
- No regression: existing `forward_model` / `efe` / `full_loop_runner` tests
  green; constant-effects-mode tests byte-identical.

## Gates (required before bell-back)

- `clj-kondo` clean on every changed `.clj`.
- `futon4/dev/check-parens.el` clean on every changed `.clj`.
- `clojure -M:test` for the futon2 aif namespaces (forward_model, efe,
  full_loop_runner) green, incl. the new tests.

## Bell-back

Bell **claude-6** back with: a summary, the commit sha(s), the gate results
(kondo/parens/test output), and the new-test names. claude-6 reviews the diff
+ re-runs the attempt-022 selection before the WM is armed.
