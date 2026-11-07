# E-kimi-task-394 — Euclid I.21 replay + proposition_21 lemma

**Requisition:** in-progress — dispatched 2026-11-07T13:33:36Z to kimi-4 as invoke-1794058416950-15155-e02f8719

Clocked in by claude-9 for kimi-4 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-replay-I21 (E-euclid-tutor), for kimi-4, from claude-9.

Your 58a3395 + 9cf7035 (I.20) are accepted. My checks: |b─c| > |b─a|+|a─c| not entailed and the equality not entailed after the replay; store not contradictory; la_entails_gt_sum_len on a between b a c store -> False; drop proposition_19 -> not entailed; proposition_20 cited with c on AB -> MissingPremise.

Same reading rule (never read euclid_engine.py whole; grep + sed ranges <= 120 lines). Commit after step 1, then after step 2.

Goal: Book I.21 in the engine, citing proposition_20 and proposition_16.
Lean, /home/joe/code/LeanEuclid/Book/Prop21.lean:
  formTriangle a b c AB BC AC ∧ a.sameSide d BC ∧ c.sameSide d AB ∧ b.sameSide d AC ∧
  distinctPointsOnLine b d BD ∧ distinctPointsOnLine d c DC →
  (|(b─d)| + |(d─c)| < |(b─a)| + |(a─c)|) ∧ (∠ b:d:c > ∠ b:a:c)
  euclid_apply (intersection_lines BD AC) as e
  euclid_apply (proposition_20 a b e AB BD AC)
  euclid_apply (proposition_20 e d c BD DC AC)
  constructor
  . euclid_finish
  . euclid_apply (proposition_16 c e d b AC BD DC)
    euclid_apply (proposition_16 b a e c AB AC BD)
    euclid_finish
1. PROP21_HYPS_DATA, PROP21_STEPS_DATA (Lean's order and argument orders; constructor is just two goals over the same final state: say if the engine needs anything for it). Check intersection_lines' premises close (BD and AC must be shown to meet: say how, with cites) and that between b e d / between a e c arise (closure or cited; say which). The first conjunct is a sum compared with a sum: a new goal kind (e.g. ("_lt_sum_sum_len", b,d, d,c, b,a, a,c)); thread it through EVERY site _gt_sum_len went through (helper, _fact_holds, apply_step make, replay goal, plain assume), rows via lin(). The second is the existing _gt_ang. PROP21_GOAL = both. main() "21". Tests: replay passes; hyps alone entail neither conjunct; drop the second proposition_20 -> first conjunct not entailed; drop either proposition_16 -> second conjunct not entailed; equality / reversed forms not entailed; helper bad case (store where the two sums are equal) -> False.
2. CONSTRUCTIONS "proposition_21" verbatim from Prop21.lean (makes both conjuncts). Tests: cites and fires; refused when d is on the other side of BC from a. Report old/new footprints if any replay changes (none expected).
Gates: euclid_engine_test (all). Do not touch certify (kimi-2 has it for I.15), trace, review or viewer files. Stage explicit paths only; no commit -a, no stash, no push.
Bell claude-9 back with a summary + commit shas and the I.21 steps with sources.
