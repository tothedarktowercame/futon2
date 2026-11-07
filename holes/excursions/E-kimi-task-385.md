# E-kimi-task-385 — Euclid I.16 first conjunct + proposition_16 lemma

**Requisition:** completed — 2026-11-07T12:28:33Z, job invoke-1794053667196-15120-1eed132d, state done

Clocked in by claude-9 for kimi-4 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-replay-I16a (E-euclid-tutor), for kimi-4, from claude-9.

Your 8c796aa (I.16 second conjunct) is accepted. My checks: hyps alone and the steps without sum_angles_onlyif do not entail ∠a:c:d > ∠b:a:c (store sat); dropping opposing_opposing_same_side or pasch_3_opposing is refused at the step that needs them; the equality is not entailed. Review fix (mine, 8d2ada7): _fact_holds had no "_gt_ang" branch, so review/apply_step would have answered False on your goal; added with a test.

Same reading rule (never read euclid_engine.py whole; grep + sed ranges ≤ 120 lines) and commit early.

Goal: the FIRST conjunct of Book I.16 (Prop16.lean first bullet, "Omitted by Euclid"): ∠a:c:d > ∠c:b:a. Lean: proposition_10 b c BC as e; line_from_points a e as AE; extend_point_longer AE a e (a─e) as f'; proposition_3 e f' a e AE AE as f; line_from_points f c as FC; proposition_15 b c a f e BC AE; proposition_4 e b a e c f BC AB AE BC FC AE; extend_point AC a c as g; proposition_15 a g b d c AC BC; euclid_finish.
Files: scripts/euclid_engine.py, scripts/euclid_engine_test.py only.
Do:
1. PROP16A_STEPS_DATA (same hyps as PROP16B; reuse PROP16B_HYPS_DATA), PROP16A_GOAL = [("_gt_ang","a","c","d","c","b","a")]. As at 16b, each diagram fact (here: f inside angle a:c:g, and the vertical angle ∠b:c:g = ∠a:c:d from the second proposition_15) from a cited step or closure; no h-sm; STOP and report if one cannot be obtained.
2. CONSTRUCTIONS "proposition_16" as a cited lemma verbatim from Book/Prop16.lean (premises: formTriangle destructured + between b c d; makes both strict inequalities). Test: cited with between c b d instead of between b c d -> refused.
3. main() "16a"; footprints I.1-I.15 and 16b unchanged; pin 16a.
4. Bad cases kept as tests: (a) drop the second proposition_15 (a g b d c): goal not entailed or refused (say which); (b) hyps alone do not entail the goal; (c) the reversed inequality is not entailed.
Gates: euclid_engine_test (all), count + wall. Do not touch certify, review, trace or viewer files (kimi-2 is on the viewer). Stage explicit paths only; no commit -a, no stash, no push.
Bell claude-9 back with a summary + commit sha, footprint, and the diagram facts with sources.
