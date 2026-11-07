# E-kimi-task-409 — replay I.24 first half

**Requisition:** completed — 2026-11-07T16:22:00Z, job invoke-1794066922033-15218-63a3a44b, state done

Clocked in by claude-9 for kimi-4 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-replay-I24a (E-euclid-tutor), for kimi-4, from claude-9.

Your 85db9c3 + 00cbd2a (I.23' replay + lemma) are accepted. My checks: lemma = Prop23.lean:35-37; the "either" kind in _fact_holds (disjunction stored, or one disjunct holds) and the recursive _concrete are sound; use_witness's new L argument is sound (f renames b, so b on L gives f on L). My bad cases: x on AB -> stuck at not_on_line x AB in the main branch; main branch citing proposition_22' instead of 22'' -> not-entailed either; use_witness with L = CD -> stuck at on_line b CD; real final store not contradictory.

Same reading rule (never read euclid_engine.py whole; grep + sed ranges <= 120 lines). Commit after each step, before belling. Commit early if the job is getting long (Kimi API timeouts hit long jobs).

Goal: the first half of I.24 (the hinge theorem; /home/joe/code/LeanEuclid/Book/Prop24.lean). This packet: the common part (lines 18-34) and the case Euclid treats (line 35's by_cases, first bullet, lines 36-39). The second bullet ("Omitted by Euclid", lines 40-59) is the NEXT packet: in this one, replay it as a by_cases whose second branch is left out, i.e. step 1 replays with d.sameSide g EF added as a branch hypothesis, and say so plainly in the comment and test name (e.g. PROP24A_*), so nobody mistakes it for the whole proof.
1. Lines 18-34 in Lean's arg orders: proposition_23' d e a b c f DE AB AC as g'; line_from_points d g'; extend_point_longer DG d g' (a─c) as g''; proposition_3 d g'' a c DG AC as g; line_from_points e g, f g; proposition_4 a b c d e g ...; proposition_5' d g f DG FG DF; the euclid_asserts at 28, 29, 34 and the `have hgf` by_contra (lines 30-33, citing sum_angles_onlyif). Each euclid_assert is something Lean's SMT proves: render it with System E citations as before and say what each one needed (28 should come from the angle hypothesis and the proposition_4 equality; 29 and 34 are diagrammatic — find the axioms). The proposition_23' disjunction (g' on AB? no: g'.onLine DE ∨ g'.sameSide f DE) must be split or resolved: say how.
2. Branch d.sameSide g EF (lines 36-39): the two angle asserts, proposition_19 e f g EF FG EG; goal |b─c| > |e─f| (via proposition_4's |b─c| = |e─g|). Tests: replay passes; hyps alone do not entail; drop proposition_5' -> refused; drop the hgf by_contra -> refused where its conclusion is used; store not contradictory.
Gates: euclid_engine_test (all). Do not touch certify, trace, review or viewer files (euclid_certify_test.py is mine, dirty: leave it). Stage explicit paths only; no commit -a, no stash, no push.
Bell claude-9 back with a summary + commit shas, the steps with sources, and what each euclid_assert needed.
