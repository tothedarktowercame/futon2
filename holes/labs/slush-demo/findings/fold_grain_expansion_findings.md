# Fold-grain expansion (run-here pass): 10 → 12 adjudicated, and the ceiling it exposed

**Driver:** Fable (direct, 2026-07-10, local box) · **Artifacts:**
`futon6/holes/fold-turn-adjudications.edn` (21-deposit census, 2 adjudicated),
`futon3a/.../fold_ground_truth.py` (merged loader), gate2 now runs on the merge.

## What the deposit corpus yielded

The 21 escrow deposits (`futon6/data/fold-turns/`) each carry the cascade's
patterns, the resolver-blind want (psi authored from the mission doc before the
fold), and ΔG — but **19/21 are plans whose outcomes were never realized**
(mission pre-complete, still chartered, or machinery exercise). Only the flown
ones adjudicate:

- `ft-action-vocabulary-005` → **FAIL** (P1 deposit → P2 dark impl → P3
  negative ×2 against the pre-registered target → mission STOPPED BY KILL
  CRITERION, 2026-07-05).
- `ft-peradam-mechanization-006` → **PASS** (machinery landed per plan-of-record;
  refusal census correct on 6 real + 4 synthetic; operator rulings pending).

Census finding: deposits become labels only when flown — the label stream and
the CH2 emission wiring are the same gap, seen from the other side.

## The result that matters (n=12: 9 success / 3 fail, 11 measurable)

**The adjudicated FAILURE scores accuracy 0.45 — above 6 of the 9 measurable
positives** (which range 0.00–0.71). AUC(acc) 0.542 ≈ chance. The witness list
shows the mechanism: `picks←pick`, `scores←scoring`, `targets←target` — the
want was authored from the mission doc and the cascade was retrieved to match
it, so interface coverage measures **plan–want vocabulary alignment**, which a
well-authored doomed plan has in abundance. Within-source ordering is right
(peradam-PASS 0.54 > action-vocab-FAIL 0.45; closure-folds positives ≥
negatives modulo R2) but the cross-source signal is confounded by want length
and cascade size.

## Verdict — the text-side accuracy limb has a Goodhart ceiling

Even directional, obligation-unified, fold-grain interface coverage cannot
separate *well-formed* from *works*: the action-vocabulary cascade covered its
want's vocabulary and missed its substance (the operator's value structure —
exactly what P3 falsified). This is not fixable by more measure engineering;
it is information the pattern/want texts do not contain.

**Reward-before-generator gate: NOT PASSED. Slice 2 stays gated.**

## The sharpened route (consistent with everything the stack already says)

1. **Labels from flown folds** — CH2 emission at the fold_escrow verdict seam
   (schema done, futon3a `32dcb09`; wiring = JVM window). closure-folds' own
   header already states the discipline; `cascade_learn` already consumes
   `:success false` (used-and-didn't-close → β+=1).
2. **Learn the reward instead of engineering it**: pattern/cascade reliability
   posteriors updated by fold outcomes (the M-bayesian-structure-learning
   line — reliability posteriors, send-to-scale = EIG), with the Slice-1.5
   coverage term as a *feature*, not the reward. The GFN then samples from a
   learned, outcome-grounded aliveness — which is what "self-evidencing" meant
   all along.
3. Text-side coverage keeps two legitimate jobs: the anti-`1=1`/bloated-shell
   controls (it kills both, verified again on n=12) and mission-conditioning.
