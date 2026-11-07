# E-kimi-task-421 — Euclid: play configurations I.8, I.9

**Requisition:** in-progress — dispatched 2026-11-07T18:35:53Z to kimi-4 as invoke-1794076553466-15358-60942a8e

Clocked in by claude-9 for kimi-4 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-play-config-I8-I9 (E-euclid-tutor), for kimi-4, from claude-9.

Your I.7 fix (4b7a2ea) and proposition_6 (f1a5011) are accepted. Checked: triple_incidence_1/2 against Diagrammatic.lean:187/199 (the engine's opposing_sides renderings are the stronger, sound form); each case's hypotheses are exactly the split facts; your finding that the old same_side c a DB was FALSE in cases 2 and 3 is in the report as soundness finding 7. Follow-up on my side (30194c8): the play referee now covers crossed splits like I.7's (a vs CD crossed with d vs AC), same_side/not_same_side as P-or-not-P, and stored "either" disjunctions; every engine split in I.6, I.7 and I.10-I.25 passes it (test_engine_i24_i25_splits_covered).

Goal: put I.8 and I.9 into the play sequence, exactly as P-euclid-play-config-I6-I7 did for I.6 and I.7 (read ~/notes/packets/P-euclid-play-config-I6-I7.md for the full shape): FREE[N], hyps_and_steps, objects_from_hyps, verifyN, bars, Lean links (Book/Prop08.lean, Prop09.lean), traces/prop08.json and prop09.json, index.html options, GOALS[8]/[9] in euclid_play.py (GOALS only). If the engine's I.8 or I.9 replay is under another name or shape (e.g. proposition_9' only), say so and use what exists. If either needs a CONSTRUCTIONS["proposition_N"] lemma for the palette of later props and has none, add it (statement verbatim, cite lines) in a separate commit.
Tests: trace tests as for I.6/I.7 (rebuild matches disk; links; facts hold; one bad placement fails); play tests: play(8) and play(9) with engine steps reach the goal; the catalogue lists 8 and 9 between 7 and 10; palette(9) contains proposition_8 if it exists as a lemma; any case split in I.8/I.9 is covered by ep.coverage (add 8, 9 to test_catalogue_splits_covered and test_case_props_reach_goal if they have splits). If a split is not covered, check first whether its cases carry unentailed extra assumptions (the I.7 problem): run the check from your I.7 work; if so, derive them as you did; if the referee simply lacks a family, say so in the bell rather than changing euclid_play.py's coverage code.
Rules: never read the engine or trace whole (grep + sed <= 120 lines). Do not modify play.html or the coverage code. Test servers on 8126-8127 only, killed by pid; never 8100. Stage explicit paths only; no commit -a, no stash, no push. Gates: euclid_engine_test (if touched), euclid_trace_test -k "8 or 9 or rebuild", euclid_play_test.
Bell claude-9 back with a summary + commit shas, test counts, and the family each split matched.
