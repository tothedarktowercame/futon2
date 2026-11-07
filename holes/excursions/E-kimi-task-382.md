# E-kimi-task-382 — Euclid I.15 replay (vertical angles)

**Requisition:** in-progress — dispatched 2026-11-07T11:51:08Z to kimi-4 as invoke-1794052268566-15111-81dd6cab

Clocked in by claude-9 for kimi-4 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-replay-I15 (E-euclid-tutor), for kimi-4, from claude-9.

Your f109366 (I.14) is accepted. My checks: hypotheses alone are sat and not contradictory; with the inner split removed the outer branch does not contradict; each inner branch with its steps removed is refused (neither is vacuous); my bad case ∠a:b:c = ∠a:b:d in place of the 2∟ sum is refused; all 14 main replays pass; 93 engine tests.

Same reading rule: never read euclid_engine.py whole (grep -n, sed ranges ≤ 120 lines); commit as soon as the replay and its tests pass.

Goal: replay Book I.15 (/home/joe/code/LeanEuclid/Book/Prop15.lean, 16 lines) in the engine:
  proposition_15 : dpl a b AB ∧ dpl c d CD ∧ e on AB ∧ e on CD ∧ CD ≠ AB ∧ between d e c ∧ between a e b → ∠a:e:c = ∠d:e:b ∧ ∠c:e:b = ∠a:e:d
Lean: proposition_13 a e c d AB CD; proposition_13 d e a b CD AB; proposition_13 c e a b CD AB; euclid_finish.
Files: scripts/euclid_engine.py, scripts/euclid_engine_test.py only. Pattern: PROP14_* / main()'s "14".
Do:
1. PROP15_HYPS_DATA (Lean's hypotheses exactly; say in a comment if neq_line is needed in both directions and why that follows), PROP15_STEPS_DATA (the three citations; any extra engine-only steps such as between_symm_parts named and explained), PROP15_GOAL (both angle equalities, checked by LA entailment).
2. main() gains "15"; footprints I.1-I.14 unchanged; add I.15's to the pin.
3. Bad cases kept as tests: (a) drop the second proposition_13: the second equality (or the first; say which) is no longer entailed; (b) between a e b replaced by between e a b: refused or goal not entailed (say which); (c) one of my own choosing: the WRONG equality ∠a:e:c = ∠c:e:b is not entailed.
Gates: euclid_engine_test (all); report count + wall. Do not touch certify (kimi-2 has euclid_certify.py dirty), review, trace or viewer files. Stage explicit paths only; no commit -a, no stash, no push.
Bell claude-9 back with a summary + commit sha and the I.15 footprint.
