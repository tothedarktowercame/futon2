# E-kimi-task-395 — Euclid I.22 discovery + replay

**Requisition:** in-progress — dispatched 2026-11-07T13:53:45Z to kimi-4 as invoke-1794059625807-15162-9fcf3b90

Clocked in by claude-9 for kimi-4 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-replay-I22 (E-euclid-tutor), for kimi-4, from claude-9.

Your e0d9066 + f934cb1 (I.21) are accepted; you were right that the order is b-d-e (my packet said b-e-d). One fix by me, 4706801: the comment cited between_points at Diagrammatic.lean:95; it is :87 (95 is between_not_trans). My checks: reversed/equality forms not entailed; helper on equal sums -> False; drop either proposition_16 -> conjunct 2 not entailed; the b-e-d branch without its pasch_3_opposing -> ByContraFailure; proposition_21 cited with a,d opposing across BC -> MissingPremise.

Same reading rule (never read euclid_engine.py whole; grep + sed ranges <= 120 lines).

Goal: Book I.22 (the triangle from three lengths), main theorem only (not 22' / 22''), in the engine. This is a DISCOVERY-then-implement packet: if step 1 shows something hard, stop after step 1, commit nothing in the engine, and bell back the finding.
Lean, /home/joe/code/LeanEuclid/Book/Prop22.lean (the first theorem): hyps are three dpl's and the three triangle inequalities (plain gt_sum_len, which _assume_metric_or_fact already takes); goal ∃ k f g, |f─k| = |a─a'| ∧ |f─g| = |b─b'| ∧ |k─g| = |c─c'|. Steps: arbitrary_point, distinct_points, line_from_points, three extend_point_longer, three proposition_3, two circle_from_points, intersection_circle_line_extending_points KLH DE g h as i, intersection_circles KLH DKL as k, use k f g, point_on_circle_onlyif x2, euclid_finish.
1. Discovery (report before building): the hard premise is intersects_cc KLH DKL for intersection_circles (Intersections.lean:17). The engine derives intersects_cc only by r_icc2 (Diagrammatic.lean:319: a on α, b inside α, a inside β, b on β), which needs inside facts; Lean's SMT gets them from the triangle-inequality hypotheses through the transfer axioms (inside_circle from length < radius, Transfer.lean). Find: which points play a and b of Diagrammatic.lean:319 (or another intersection rule nearby — list them with lines), which inside/on facts each needs, and which transfer axiom (cite, verbatim) turns each length comparison into an inside fact. Say whether the engine already has those transfer rules (grep) and, if not, whether each can be added as a CITED step (preferred: per-use, like triple_incidence_1) rather than a closure rule. Also whether the existing goal kinds express the ∃ (the replay's named objects k f g suffice; say).
2. If step 1 is clean: PROP22_HYPS/STEPS/GOAL with Lean's argument orders, the cited transfer steps from step 1, main() "22", tests (replay passes; hyps alone do not entail; drop one triangle-inequality hypothesis -> intersection_circles refused (this is the test that matters: say which inequality is consumed where); drop a point_on_circle_onlyif -> goal not entailed). Commit, report footprints (no other replay should change).
Gates: euclid_engine_test (all). Do not touch certify (kimi-2 has it for I.15), trace, review or viewer files. Stage explicit paths only; no commit -a, no stash, no push.
Bell claude-9 back with a summary (the step-1 findings with cites in any case) + commit shas if any.
