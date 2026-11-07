# E-kimi-task-389 — Euclid I.18 replay; I.18 closure rule out

**Requisition:** completed — 2026-11-07T13:05:22Z, job invoke-1794055431943-15134-b33ad360, state done

Clocked in by claude-9 for kimi-4 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-I18 (E-euclid-tutor), for kimi-4, from claude-9.

Your 52bbb2f + 81bac1e (I.6 the Lean way) are accepted. My checks: with r_side_gt_angle_gt replaced by a raiser, I.6 passes and reaches |ab| = |ac|; branch 2 without its sum_angles_onlyif is refused; dropping the angle-equality hypothesis makes the by_contra fail (the proof is not vacuous). Footprint unchanged is right (branch work is on copies).

Same reading rule (never read euclid_engine.py whole; grep + sed ranges ≤ 120 lines). Commit after step 2, then again after step 3.

Goal: Book I.18 in the engine, then the closure rule that IS I.18 leaves default closure (the c237244 pattern for I.13).
1. PROP18_HYPS_DATA (formTriangle destructured as PROP16/17, plus |a─c| > |a─b|), PROP18_STEPS_DATA following /home/joe/code/LeanEuclid/Book/Prop18.lean: proposition_3 a c a b AC AB as d; line_from_points b d as BD; proposition_16 b c d a BC AC BD; proposition_5' a b d AB BD AC; sum_angles_if b a c d AB BC (SystemE/Theory/Inferences/Transfer.lean:83 — add verbatim with cite if missing; note its direction: angle sum -> same sides; if you instead need sum_angles_onlyif say why); euclid_finish. Engine-only steps named and cited; no h-sm. PROP18_GOAL = [("_gt_ang","a","b","c","b","c","a")].
2. main() "18" runs with exclude_rules={"side_gt_angle_gt [SMT-bridge, Euclid I.18]"}. Test: hyps alone with the exclusion do NOT entail the goal (and with the rule they do, which is why: say so in the test docstring).
3. Remove r_side_gt_angle_gt from FORWARD_RULES (delete the function; leave a NOTE comment as at straight_angle). Add CONSTRUCTIONS "proposition_18" verbatim from Prop18.lean (formTriangle form + gt_len premise; makes the _gt_ang conclusion). Drop the now-unneeded exclude_rules from main "6" and "18" (keep the replay(... exclude_rules) mechanism). Report old/new footprints for every replay that changes (I expect several, as at c237244: list them); every main() goal must still hold. Test: proposition_18 cited with |a─c| < |a─b| instead -> refused.
4. Bad cases kept as tests: drop proposition_16 -> goal not entailed; drop proposition_5' -> not entailed.
Gates: euclid_engine_test (all) AND euclid_review_test (the review loads the engine by file path; if a review test pinned something that came from this rule, report it and fix only if the fix is in the test's expected value with a stated reason). Do not touch certify (kimi-2 has it dirty), trace or viewer files. Stage explicit paths only; no commit -a, no stash, no push.
Bell claude-9 back with a summary + commit shas, the old/new footprint table, and the I.18 steps with sources.
