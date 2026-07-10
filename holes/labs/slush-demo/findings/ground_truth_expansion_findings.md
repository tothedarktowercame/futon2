# Task B — discharge ground-truth expansion: findings

**Driver:** Fable (direct, no bells — Joe 2026-07-10) · **Code:**
`futon3a/holes/labs/M-memes-arrows/aliveness_v3_corpus_gate2.py`
**Artifact:** `findings/ground_truth_mission_grain.json` (83 records)

## What was tried: mission-grain expansion (the obvious 10 → 83)

Joined `futon6/data/mission-wholeness.edn` (:class labels) with
`mission-pattern-scopes.edn` (:applied) → 83 scored records (65 alive / 18 mess),
want-text via `identify_psi(mission doc)`, scored with the LOCKED Slice-1.5
measure. The mess-negatives are selection-realistic (patterns actually chosen,
mission still went to mess).

## Result: the mission grain is INVALID as discharge ground truth

| | nonzero | AUC(alive>mess) | null-95 |
|---|---|---:|---:|
| locked measure (T1+T2+T3) | 83/83 | 0.449 | 0.632 |
| T1+T2 (no semantic tier) | 82/83 | 0.430 | 0.631 |
| T1-only (strict lexical) | 82/83 | 0.399 | 0.634 |

Two facts pin the diagnosis to GRAIN, not to the measure:
1. **Even strict T1 lexical coverage is dense at mission grain** (82/83
   nonzero) — long IDENTIFY texts × several applied patterns saturate coverage.
   Sparsity-vs-density was never about unification tiers at this grain.
2. **Nothing discriminates at any tier** (AUC ≈ chance in every row). Mission
   wholeness class is a property of the whole mission; applied-pattern
   discharge is a property of individual folds. Different grains, no shared
   signal. (Also matches claude-4's corpus-gate finding from the semantic side.)

So the 83-record artifact is kept (it is a fine relevance/selection corpus) but
it CANNOT serve as the discharge reward's ground truth. Joe's framing gets a
precise refinement: the expansion looked procedural, but the only procedural
route (mission-grain join) is invalid — valid expansion is fold-grain only.

## Where fold-grain records can come from (the unlock)

Current stock: 16 closure-folds records (10 usable in GROUND) + **4** CH2 events.

1. **Prospective (the real fix): CH2 discharge events.** `meme.ch2` already
   emits fold-grain events (move-id + sorry-ref) — but only 4 exist, and the
   schema **hardcodes `:discharged? true`** (`discharge-event?` rejects
   anything else): failed folds are UNREPRESENTABLE, directly violating the
   recording discipline stated in closure-folds.edn's own header ("record
   FAILED folds too" — claude-1's β/discrimination unlock, 2026-06-10).
   Fix = extend the schema+validator to `:discharged? false` events and wire
   emission into the live-loop fold path. Touches production meme.ch2 +
   gates_test.clj → **operator decision, not unilaterally done here.**
2. **Retrospective: curation, not mechanics.** The first 10 GROUND records were
   hand-adjudicated by claude-1 with resolver-blind problem texts. More exist in
   mission/git history, but each needs judgment (what was the hole, what was
   used, did it close) — a per-record curation task, honestly not batchable.

## Standing state of the reward gate after Slices 1.5 + Task B

- Fold grain: measure locked and validity mostly recovered (6/7 positives > 0,
  anti-1=1 PASS, residuals R1/R2 documented) — n=9 measurable, underpowered.
- Mission grain: powered but invalid (this file).
- **Slice 2 therefore stays gated on fold-grain ground truth at n≈60+**, whose
  bottleneck is the CH2-negatives schema + live-loop emission (plus optional
  retro-curation), not on any further measure design.
