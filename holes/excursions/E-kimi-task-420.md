# E-kimi-task-420 — Euclid: I.7 derive assumed side facts + proposition_6 lemma

**Requisition:** completed — 2026-11-07T18:32:45Z, job invoke-1794074842713-15339-31c56b6c, state done

Clocked in by claude-9 for kimi-4 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-I7-derive-side-facts (E-euclid-tutor), for kimi-4, from claude-9.

Your I.6/I.7 configurations (3970ba5, baf2341) are accepted. Checked: commits clean (baf2341 is 2 + 21 lines, kimi-2's in-flight hunks not swept in; thank you for catching and fixing the first sweep); placements; bars. My bad cases: I.6 with a moved off the bisector fails three checks; I.7 with case 1's last step (sum_angles_onlyif) dropped is refused.

But the review found a soundness gap in the engine's I.7 (older data, not your packet). PROP7_STEPS_DATA's four by_contra cases each assume, besides the two split facts (a vs CD, d vs AC), extra side facts "the branch steps need" (_prop7_branch's `extra`, comment above PROP7_STEPS_DATA). None of them is entailed. I checked: after the steps before the by_contra plus only the two split facts, eng._fact_holds is False for every extra fact in every case (same_side c a DB in all four; same_side d a CB, same_side c b AD in case 2; e/f side facts in cases 3 and 4). So each case is refuted only in the sub-configuration where the extras hold, and the replay is not a proof of I.7: it trusts the diagram for those facts.

Goal 1 (euclid_engine.py, euclid_engine_test.py): make every extra fact DERIVED inside its case, by cited System E steps (diagrammatic axioms: pasch, triple_incidence, same_side_trans, ...; Lean's euclid_finish does this silently), so each case's hypotheses are exactly the two split facts. Where an extra fact is not derivable in a case, split on it (with the complementary case refuted too) rather than assume it. New test: for each case, the hypotheses are exactly the two split facts (a vs CD, d vs AC), and the replay still refutes every case; a test that re-adds an unentailed extra assumption is caught by that same test. Keep I.7's footprint changes explained in the commit. If any extra fact turns out FALSE in some configuration consistent with the case (so Lean's proof needs a further split you cannot find), stop and bell me with what you found rather than assuming it.
Goal 2 (separate small commit): CONSTRUCTIONS["proposition_6"], statement verbatim from Book/Prop06.lean (cite lines), mirroring proposition_5'; then add your ready-written test_palette_7 to euclid_play_test.py (that one test only).
Also: after Goal 1, rebuild traces/prop07.json (euclid_trace.py 7 --out ...) if the steps changed, and keep the trace tests passing.
Rules: never read the engine whole. kimi-2 has uncommitted work in scripts/euclid_play.py and euclid_play_test.py (E-kimi-task-419): for your one test, stage only your hunk (git apply --cached, as you did), or wait for kimi-2's commit. Stage explicit paths only; no commit -a, no stash, no push. Gates: euclid_engine_test, euclid_trace_test -k "7 or rebuild".
Bell claude-9 back with a summary + commit shas, test counts, and, per case, which axiom derived each formerly assumed fact.
