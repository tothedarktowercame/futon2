# E-kimi-task-392 — Euclid I.20 replay + proposition_20 lemma

**Requisition:** completed — 2026-11-07T13:32:55Z, job invoke-1794057458825-15147-c42c63ae, state done

Clocked in by claude-9 for kimi-4 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-replay-I20 (E-euclid-tutor), for kimi-4, from claude-9.

Your 306a1ae + 312b665 (I.19) are accepted. My checks: the gt_ang hypothesis row entails ∠a:b:c > ∠b:c:a and not the reverse (the old I.18 rule's sign bug, checked for the new assume branch); hypothesis reversed -> ByContraFailure; branch 2 citing proposition_5' instead of proposition_18 -> ByContraFailure; proposition_19 cited with the angles equal -> MissingPremise.

Same reading rule (never read euclid_engine.py whole; grep + sed ranges <= 120 lines). Commit after step 1, then after step 2.

Goal: Book I.20 (triangle inequality) in the engine, citing proposition_19.
Lean, /home/joe/code/LeanEuclid/Book/Prop20.lean:
  formTriangle a b c AB BC AC → |(b─a)| + |(a─c)| > |(b─c)|
  euclid_apply (extend_point_longer AB b a (c─a)) as d'
  euclid_apply (proposition_3 a d' a c AB AC) as d
  euclid_apply (line_from_points d c) as DC
  euclid_apply (proposition_5' a c d AC DC AB)
  euclid_apply (sum_angles_onlyif c b d a BC DC)
  euclid_apply (proposition_19 b c d BC DC AB)
  euclid_finish
1. PROP20_HYPS_DATA (formTriangle as PROP18/19), PROP20_STEPS_DATA with Lean's argument orders; engine-only steps (between_symm_parts etc.) named and cited; no h-sm. The goal is a new kind, a length SUM compared with a length: pick a tuple form (e.g. ("_gt_sum_len", b,a, a,c, b,c)) and add it to EVERY place a goal kind must be handled: replay's goal loop, _fact_holds, and an la helper (la_entails_...) — grep for how _lt_sum_rt_ang was threaded through (e98131f/05931d5) and do the same. Check how |b─d| = |b─a| + |a─d| arises (between b a d; closure or a cited length-sum axiom — say which, with cite). main() "20". Tests: replay passes; hyps alone do not entail the goal; drop proposition_19 -> not entailed; drop sum_angles_onlyif -> not entailed; the goal with ≥ replaced by equality (|ba|+|ac| = |bc|) is not entailed; for the new helper, a bad case of its own (a store where the sum is only ≥, or the comparison runs the other way) -> False.
2. CONSTRUCTIONS "proposition_20" verbatim from Prop20.lean (formTriangle destructured; makes the sum comparison; check the apply_step make loop handles the new tuple, add a branch if not). Tests: cites and fires; refused when c is on AB. Report old/new footprints if any replay changes (none expected).
Gates: euclid_engine_test (all). Do not touch certify, trace, review or viewer files (kimi-2 has the trace/viewer files for I.15). Stage explicit paths only; no commit -a, no stash, no push.
Bell claude-9 back with a summary + commit shas and the I.20 steps with sources.
