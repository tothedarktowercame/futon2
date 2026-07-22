# M-wm-three-factor-mission-value — wire the central × strategic × doable value into the WM enrichment

**Status:** IDENTIFY → handoff, reviewed by claude-6.
**Owner mission:** `~/code/futon7/holes/M-war-machine-aif-completion.md`
**Date:** 2026-07-18. Operator: Joe. Ground Control: claude-6.
**Reference prototype (WORKS live in the serving JVM):** `/tmp/wm-policy-proto3.clj`.
**Design memory:** `project_wm_mission_value_three_factor` (in the auto-memory).

## What changes

Replace the current two-factor mission-value in
`enrich-candidates-with-mission-value` (scripts/futon2/report/war_machine.clj)
— today `normalize(tension × centrality) × non-progress-decay`, where tension is
the local phase scalar — with the settled **three-factor additive blend**:

```
mission-value-factor(m | π_strat) =
      ( w_c · central(m)              ; normalize(c_joint) — structural geometry, [0,1]
      + w_s · strategic(m | π_strat)  ; cascade role: spine 1.0 / terminal 0.7 / in 0.4 / off 0.0
      + w_d · doable(phase m) )       ; automatability, PEAKED at INSTANTIATE
    × completion-gate(m)              ; 0 if phase = complete else 1
    × non-progress-decay(m)           ; existing 1/(1+n), unchanged
```

- **central(m)** = `c_joint(m) / cmax`, `cmax` = max c_joint over `centrality-joint-map`
  (GLOBAL normalization — absolute, not batch-relative; 0.0 if m absent from the map).
- **strategic(m | π_strat)** = read the active cascade EDN: extract mission tokens
  from each `:box` (`M-\S+` regex, missions can be compound), then
  `spine 1.0 / terminal 0.7 / in-cascade 0.4 / off-cascade 0.0`, keyed by
  `normalize-mission-id`.
- **doable(phase)** = `{head 0.1 identify 0.2 map 0.3 derive 0.5 argue 0.6 verify 0.8
  instantiate 1.0 document 0.4 complete 0.0 unknown 0.3}` (nil → 0.3). Phase comes
  from the delta-t result already computed at this seam (`:mission-phase`) — do NOT
  add a second substrate read.
- **weights** default `w_c 0.25 / w_s 0.45 / w_d 0.30` (sum 1.0 keeps the factor in
  [0,1]); overridable via opts/env (`FUTON_WM_VALUE_WEIGHTS` or similar).
- **cascade** default `~/code/futon7/holes/M-futon-forward-model.backlog-cascade-merged-v0.edn`;
  overridable via opts/env (`FUTON_WM_STRATEGY_CASCADE`). This is the **policy** —
  swapping it (e.g. `…-stars-v0.edn`) must reorder selection (G-over-policies).

## Critical invariants

- **Output stays in [0,1]** — `forward_model.clj` clamps `:mission-value-factor` to
  [0,1] and multiplies obs-deltas by it. The blend (weights sum 1, factors in [0,1],
  gates ≤1) is already in [0,1]. Do NOT break this.
- **Do NOT touch `forward_model.clj`** or the factor-consumer math — this is purely
  how the judge COMPUTES the factor. forward_model still reads `:mission-value-factor`.
- **Additive, NOT multiplicative** — the whole point: the strategy spine has
  `c_joint = 0`, so `0 × boost = 0` erases it. Missions with only strategic or only
  doable signal MUST still get a non-zero factor. (This also broadens coverage: the
  ~114 missions without centrality now still score via doable + strategic.)
- **Attach the components** (`:central :strategic :doable :phase :mission-value-factor`)
  to the action for trace/QA visibility, the way `:tension`/`:centrality` are today.
- Keep the non-progress-decay and its cross-run persistence exactly as landed
  (commit `8361512`).

## Acceptance bar

- **Live probe in the serving JVM** (the only runtime where delta-t resolves —
  reference `/tmp/wm-policy-proto3.clj`): enriching real candidates reproduces the
  three-factor ordering — the central+terminal+INSTANTIATE mission
  (`expressions-of-interest`) on top; a spine-but-IDENTIFY mission (`joe-reflection`,
  strategic 1.0 but doable 0.2) present but demoted; a completed mission → factor 0.
- **Policy swap**: `FUTON_WM_STRATEGY_CASCADE=…stars-v0.edn` yields a different
  top-ordering than merged-v0 over the same candidates.
- **Hermetic test** (mock `centrality-joint-map`, cascade EDN, and phase via
  `with-redefs`): asserts (1) a spine mission with `c_joint = 0` still gets a
  non-zero factor from strategic+doable; (2) INSTANTIATE → higher doable than HEAD;
  (3) complete → factor 0; (4) two different cascades → different factors for the
  same mission. Must not hit the live substrate.
- Existing futon2 tests stay green (incl. the `8361512` mission-value tests, updated
  for the new formula).

## Gates (before bell-back)

- `clj-kondo` clean; `futon4/dev/check-parens.el` clean;
  `clojure -X:test` green for the affected futon2 nses incl. the new hermetic test.

## Bell-back

Bell **claude-6** with: summary, commit sha(s), gate results, new/updated test
names, and the **live serving-JVM enrichment probe output** (three-factor ordering
+ a merged-v0-vs-stars-v0 policy-swap comparison). Do NOT arm cron or run a live
full-loop click.
