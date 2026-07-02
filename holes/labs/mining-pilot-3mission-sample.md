# Mining pilot — 3-mission sample (R19-PROOF-JOIN discharge recovery)

**Date:** 2026-07-02 · **Owner:** claude-3 · **Lane:** claude-11's R19-PROOF-JOIN + mining pilot
(Joe-ratified). **Status:** first sample recovered (read-only); per-goal grading + executor-reach
landing (R14) remain. **No writes yet** — dry-run to claude-11 before any `--execute`.

## Scope (from the handoff, sharpened)
Grow durable `:discharged-by` toward the **70 uncovered mission-directed c-entries** (of 189
`:outcome-ref`; 119 already reach a `code/v05/mined-move` — claude-11 step 3). Target the
highest-marginal-value canonical missions; CPU only; A-next recovery (doc+code+git+XTDB), graded
honestly. Sample = the top 3 canonical uncovered missions: **aif2 ×11, first-flights ×9,
typed-holes ×5 = 25 c-entries.**

## Verified target (read-only Drawbridge join, reproduces claude-11's coverage exactly)
`189 :outcome-ref → 119 covered → 70 uncovered`. Ranked uncovered canonical missions:
aif2 (11) · first-flights (9) · typed-holes (5) · substrate-metric (3) · {web-arxana-missions,
emacs-cursor-peripheral, vsatarcs-invariants-integration, stack-inhabitation} (2 each) · ~12
single-entry missions. **Excluded + flagged:** two targets are raw UUIDs (`644e2d96…` ×9,
`357f5b3c…` ×3) — non-canonical `:outcome-ref` targets (likely claude-11's "5 unresolved"); not
recovered.

## Raw-material finding (why the recovery is what it is)
The uncovered c-entries are **`:reach` flavour goals**, not the `:mess` preference entries in
`futon6/data/c-vector/c-store-overlay.edn`. Each is `{:flavour reach :kind goal :ref-id "M-<x>"
:status open :entry-edn {:discharged-by nil …}}`, source `c-vector-promote`, id = a UUID, name =
`c-entry/reach/pair-<hash>` (derived from an E-have-want pair). So "recovery" = **post-hoc discharge
determination for open goals**: read the mission's doc+code+git, decide what (if anything) discharged
each goal, grade `:discharged-by <sha>` / `:open` / `:research` — no fabricated closure.

## The sample — mission-level recovery (doc-attested)
| mission | maturity (doc) | discharge method (candidate, canonical) | buildable-as-mined-move? |
|---|---|---|---|
| **futon2-d/mission/aif2** | **CLOSED/DELIVERED 2026-06-02** | slice-1 tension-proposer + β playful-precision, built+green, **live-installed in `judge`** consuming E1 curvature | **YES** — the slice-install / judge-consumes-curvature is a concrete forward method |
| **futon3c-d/mission/first-flights** | **PHASE A COMPLETE, PASS (ckpt 20)** | flight-as-derivation INSTANTIATE (`repl_spec_verify.clj`; "measurement-as-discharge already has its rule here") | **YES** — Phase-A artifacts; already near the discharge-rule layer |
| **futon3c-d/mission/typed-holes** | **CLOSED 2026-06-15** | the **Fill/Discharge** op itself (DarkTower `TypedHole/Fill/Discharge`, 0-sorry, `lake build` green) | **YES (canonical)** — typed-holes *is* the discharge move |

**Structural insight (the pilot's real payoff so far):** the uncovered tail's head is dominated by
**stale c-vector entries on missions that are actually discharged** — the mission closed with
built+green artifacts, but the `:reach` entry never had its `:discharged-by` updated. So growing
`:discharged-by` here is **cheap + high-precision + all buildable-as-mined-move** — exactly the
highest-value slice claude-11 pointed at.

## Precision — honest status
- **Mission-level: 3/3 recoverable, doc-attested** (each mission doc explicitly states closure +
  the built method). That's a real, high first signal but it is the *coarse* denominator.
- **Per-goal: NOT yet graded** — and it matters, because a closed mission can carry **open residue**:
  M-aif2 §8 explicitly names residue (beats-baseline value-proof #5 + E2/E3 signal upgrades) that is
  **NOT** discharged. So some of aif2's 11 `:reach` goals map to the discharged slice-1, others to
  that residue → honest grade `:open`/`:research`. Per-goal precision requires mapping each
  `c-entry/reach/pair-<hash>` to its E-have-want pair's *want* and checking it against the delivered
  artifacts. That is the next step; I will NOT report a per-goal precision number I have not measured.

## R14 variance workstream (claude-11 scope addition)
All 3 sample missions are **buildable-as-mined-move**, so each is a candidate to **land as executor
reach** — a `fold_engine.clj` RULES entry (honest NL→rule extraction from the method's actual content)
or a mined-move the rollout consumes. `typed-holes`'s Fill/Discharge is the cleanest (it is the
universal discharge op). Goal of the workstream: after landing, a scheduled tick yields a **non-zero
perf sample → a live trace with γ ≠ 1.0**, closing R14. Not attempted yet — rides on the per-goal
recovery below.

## Remaining (this pilot)
1. **Per-goal grade** the 25 `:reach` goals (pair `want` ↔ delivered artifact) → candidate
   `:discharged-by` relations, canonical endpoints, honestly graded. Dry-run + precision spot-checks
   **to claude-11 before any bulk `--execute` (>50)**.
2. **Land the buildable methods as executor reach** (fold_engine RULES / mined-move) — check-parens +
   sim fold test; drive toward the γ≠1.0 trace record (R14 closure).
3. Extend from the 3-mission sample toward the ~12-mission selection covering ~45 of the 70.

## Reusable artifacts
- Uncovered-tail query (read-only Drawbridge): the join of `[?r :relation/type :outcome-ref]` `from`/`to`
  filtered against the `#{}` of `:code/v05/mined-move` `:hx/endpoints`, then `remove` the covered — the
  same query claude-11 persisted in E-C-vector-live §11, `remove`d instead of `filter`ed for the tail.
- c-entry content source: `futon6/data/c-vector/c-store-overlay.edn` (`:mess`) + the `c-vector-promote`
  `:reach` entities on :7071 (`:entity/props :entry-edn`).
