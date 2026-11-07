# E-kimi-task-390 — Euclid I.19 replay + proposition_19 lemma

**Requisition:** completed — 2026-11-07T13:17:00Z, job invoke-1794056848641-15143-4b61227f, state done

Clocked in by claude-9 for kimi-4 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-replay-I19 (E-euclid-tutor), for kimi-4, from claude-9.

Your I.18 commits (1deb4ab 50b1e00 005c175 55b88b3) are accepted. You were right that my step-2 expectation was wrong: I read the deleted code and the row was ∠p:q:r - ∠q:r:p < 0 (LinArith.add is coeffs op const), the wrong direction. The sum_angles_onlyif substitution is reasoned and stays. My checks: proposition_18 cited with |ac| = |ab| -> MissingPremise; replay without sum_angles_onlyif -> not entailed; reverse ∠b:c:a > ∠a:b:c not entailed after the full replay.

Same reading rule (never read euclid_engine.py whole; grep + sed ranges <= 120 lines). Commit after step 2, then after step 3.

Goal: Book I.19 in the engine (the converse of I.18), citing the proposition_18 lemma.
Lean, /home/joe/code/LeanEuclid/Book/Prop19.lean:
  formTriangle a b c AB BC AC ∧ (∠ a:b:c > ∠ b:c:a) → |(a─c)| > |(a─b)|
  euclid_intros; by_contra; by_cases |(a─c)| = |(a─b)|
  . euclid_apply (proposition_5' a b c AB BC AC); euclid_finish
  . euclid_apply (proposition_18 a c b AC BC AB); euclid_finish
1. PROP19_HYPS_DATA (formTriangle destructured exactly as PROP18_HYPS_DATA, plus the angle comparison as a metric hypothesis; say which tuple form you use and check _assume_metric_or_fact handles it). PROP19_STEPS_DATA: one by_contra (see PROP14_STEPS_DATA for the shape) with two branches, |a─c| = |a─b| and |a─b| > |a─c| (Lean's ¬(>) ∧ ¬(=) is the second; exhaustiveness by trichotomy is the caller's job, so say so in a comment at the use site, as elsewhere), conclusion ("gt_len","a","c","a","b"). Branch 1: proposition_5' a b c AB BC AC. Branch 2: proposition_18 a c b AC BC AB (Lean's argument order; check the premises close: formTriangle a c b, and |a─b| > |a─c|). Engine-only steps named and cited; no h-sm. PROP19_GOAL = [("_gt_len","a","c","a","b")] or whatever the goal-kind convention is (check replay's goal branch AND _fact_holds handle it; add a branch if one is missing, as at 8d2ada7). main() "19".
2. Tests: replay passes; hyps alone do not entail the goal; drop branch 2's proposition_18 -> by_contra refused (ByContraFailure); drop the angle hypothesis -> refused (not vacuous); the reverse |a─b| > |a─c| is not entailed after the replay.
3. CONSTRUCTIONS "proposition_19" verbatim from Prop19.lean (as proposition_18: formTriangle destructured, the angle comparison as an entailment premise, makes the length comparison). Tests: cites and fires; cited with the angle comparison reversed or equal -> refused. Report old/new footprints if any replay changes (none expected).
Gates: euclid_engine_test (all). Do not touch certify (kimi-2 has it dirty), trace, review or viewer files. Stage explicit paths only; no commit -a, no stash, no push.
Bell claude-9 back with a summary + commit shas and the I.19 steps with sources.
