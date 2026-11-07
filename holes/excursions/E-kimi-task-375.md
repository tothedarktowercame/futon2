# E-kimi-task-375 — E-euclid-tutor: replay Book I.13 without the built-in straight-angle rule

**Requisition:** in-progress — dispatched 2026-11-07T10:10:35Z to kimi-4 as invoke-1794046235310-15068-dc3c0ed5

Clocked in by claude-9 for kimi-4 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-replay-I13-v2 (E-euclid-tutor), for kimi-4, from claude-9.

READ THIS FIRST. Three earlier seats (kimi-1 twice, kimi-3) died on this packet with java.net.http.HttpTimeoutException right after reading /home/joe/code/futon0/scripts/euclid_engine.py whole (2700 lines) with read_file. Never read that file whole. Use grep -n to find things and sed -n 'A,Bp' for ranges of at most 150 lines. Useful anchors (line numbers at HEAD 3cd15f3): FORWARD_RULES ~1160, r_straight_angle 877, CONSTRUCTIONS proposition_10 1321, exists_point_opposite 1339, perpendicular_if 1720, sum_angles_onlyif 1732, _by_contra 2044, run_steps 2081, replay 2093, PROP12_* 2545, main() near the end. Tests: ~/code/futon6/.venv/bin/python -m pytest -q <file>, run from scripts/.

Background (found by claude-9, 10:15Z). The engine already contains Book I.13 as a closure rule: r_straight_angle ([SMT-bridge]: between u v w, x off the line => ∠u:v:x + ∠x:v:w = 2∟). From I.13's hypotheses alone (with neq_line in both directions) closure entails the goal with no steps. I checked that no replay I.1-I.12 needs it: with r_straight_angle removed from FORWARD_RULES all twelve main() replays still pass. So an honest I.13 replay must prove the goal WITHOUT r_straight_angle, the way Book/Prop13.lean does.

Goal: replay Book I.13 (/home/joe/code/LeanEuclid/Book/Prop13.lean) in the engine with r_straight_angle disabled for that replay: ∠c:b:a + ∠a:b:d = ∟ + ∟ from AB ≠ CD, dpl a b AB, dpl c d CD, between d b c.
Lean's proof: by_cases ∠c:b:a = ∠a:b:d; branch 1 euclid_finish; branch 2 proposition_11' d c b a CD as e, line_from_points b e as BE, euclid_finish. Lean's SMT does the angle arithmetic, including the case analysis of where a lies relative to BE.
Files: scripts/euclid_engine.py and scripts/euclid_engine_test.py only.

Do:
1. A way to run a replay with a named forward rule excluded (e.g. replay(..., exclude_rules={"straight_angle [SMT-bridge]"}), threaded through close()). Default behaviour unchanged.
2. Rules, each verbatim from its source with the file:line in "cite": proposition_11' (Book/Prop11.lean:24: dpl a b AB, between a c b, not_on_line x AB => f with same_side f x AB and ∠a:c:f = ∟), perpendicular_onlyif (SystemE/Theory/Inferences/Transfer.lean:107), and sum_angles_if (Transfer.lean:83) only if you need it. sum_angles_onlyif and perpendicular_if exist already.
3. A by_cases construct, next to _by_contra: ("by_cases", branches, conclusions), where each branch is (assumption facts, steps) run on a copy, and each branch must ENTAIL every conclusion (not reach a contradiction); then the conclusions are added to the outer state. Exhaustiveness of the assumptions is the caller's responsibility, documented at the use site, as for by_contra. Tests: a by_cases whose one branch does not reach the conclusion is refused (an exception like ByContraFailure, with the branch named), and the outer state is unchanged.
4. PROP13_HYPS_DATA / PROP13_STEPS_DATA. Suggested shape (yours to change): proposition_11' d c b a CD as e; line_from_points b e as BE; then by_cases on where a lies relative to BE: a on BE / a same side as c / a same side as d, each branch reaching the goal row with sum_angles_onlyif, perpendicular_if / _onlyif. State in a comment why the three cases are exhaustive. If you need Lean's by_cases on ∠c:b:a = ∠a:b:d instead, say why. The hypothesis AB ≠ CD may be given in both directions (the engine's neq_line is directed); say so in a comment. Any h-sm input must be named and justified; prefer none.
5. main() gains "13", run with r_straight_angle excluded, checking the goal by LA entailment; footprints for I.1-I.12 unchanged (37/8, 112/38, 60/22, 198/79, 442/227, 163/51, 318/158, 178/68 for 1,2,3,4,5,10,11,12; check the others are unchanged too). Report I.13's.
6. Bad cases you must build and keep as tests: (a) drop proposition_11' (e never exists): the replay fails; (b) the wrong sum (= ∟) is not entailed; (c) drop between d b c from the hypotheses: the replay fails; (d) one by_cases branch removed: refused or the conclusion not added (say which and why); (e) proposition_11' with x on AB refused.
Gates: all six files (euclid_engine_test, euclid_search_test, euclid_review_test, euclid_clones_test, euclid_clone_diagram_test, euclid_trace_test), counts and wall time. kimi-2 is editing euclid_search.py / euclid_review.py / euclid_review_test.py: do not touch or stage them; if a failure there is clearly theirs, say so. Stage explicit paths only; no commit -a, no stash, no push.
Bell claude-9 back with a summary + commit shas.
