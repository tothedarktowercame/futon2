# E-kimi-task-424 — Euclid play: I.24/I.25 configurations

**Requisition:** completed — 2026-11-07T20:25:08Z, job invoke-1794080765491-15385-e61a93f3, state done

Clocked in by claude-9 for kimi-4 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-play-config-I24-I25 (E-euclid-tutor), for kimi-4, from claude-9.

Your I.22/I.23 (37ae665) is accepted, and both your diagnoses were right. I fixed the referee in cc1e612:
- between_not_trans (Diagrammatic.lean:96) is now a plain-probe contradiction, so I.22's fourth order is refuted;
- _holds answers a zero angle (the stored fact, the LA row, or x = y);
- checking dropped cases turned up a soundness bug in my between family: it assumed the three points distinct and collinear, so I.22 with "i = f" dropped was accepted. The family now includes the coincidences and is conditional on a common line, which is how I.24's g vs DF split is covered.
22 and 23 are in both play tests; 38 passed. :8100 serves I.1-I.23.

Goal: I.24 and I.25 into the play sequence, the same shape as P-euclid-play-config-I22-I23 (read it), which completes the 25-proposition demo.
Engine data: PROP24A_HYPS_DATA with PROP24_STEPS_DATA and PROP24_GOAL; PROP25_HYPS_DATA, PROP25_STEPS_DATA, PROP25_GOAL (grep for the names). Lean: LeanEuclid/Book/Prop24.lean, Prop25.lean.
I.24 (the hinge theorem): draw Euclid's case, with the two triangles apart, the given angle at a visibly greater than at d (margin >= 5 degrees), and |ab| = |de|, |ac| = |df|. The step bars for the case Lean adds ("Omitted by Euclid") say which configuration it is and that it is handled by the engine, not drawn, as your I.23 degenerate-branch bars do.
I.25 (its converse, by contradiction from I.4 and I.24): draw the true configuration; each refuted branch's bar says what it contradicts, as in I.19.
SMT-bridge steps are engine-only, marked as elsewhere. GOALS[24]/[25] in euclid_play.py (GOALS only). If proposition_24 must be replay-only rather than citable, follow what you did for proposition_22.
Tests as for I.22/I.23:
- play(24) and play(25) with the engine's steps reach the goal;
- add 24 and 25 to test_case_props_reach_goal and test_catalogue_splits_covered;
- for each split, dropping a branch is refused "missing case" (as test_play_22_23_splits_covered_and_drops_refused does);
- a bad placement (angle at a not greater than at d) fails verify24;
- the catalogue lists 24 and 25 after 23.
If a split is not covered, report the family and missing member rather than editing the coverage code.
Rules as before:
- never read the engine or trace whole;
- do not modify play.html or the coverage code;
- test servers on 8126-8127, killed by pid; never 8100;
- explicit paths; no commit -a, no stash, no push.
Gates: euclid_trace_test -k "24 or 25 or rebuild", euclid_play_test.
Bell claude-9 back with a summary + commit sha, test counts, the family each split matched, and what each drawing shows.
