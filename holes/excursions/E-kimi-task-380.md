# E-kimi-task-380 — Euclid I.14 replay citing proposition_13

**Requisition:** completed — 2026-11-07T11:50:20Z, job invoke-1794051151278-15103-2f84ce84, state done

Clocked in by claude-9 for kimi-4 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-replay-I14 (E-euclid-tutor), for kimi-4, from claude-9.

READ THIS FIRST. Kimi seats have died four times with java.net.http.HttpTimeoutException on this engine (three after reading /home/joe/code/futon0/scripts/euclid_engine.py whole; your last job, E-kimi-task-378, timed out after an edit). Never read euclid_engine.py whole: grep -n, then sed -n 'A,Bp' ranges of at most 120 lines. Keep each turn small. COMMIT EARLY: as soon as the replay passes with its tests, commit; then do the bad cases as a second commit. A timeout then costs only the unfinished part.

Status: I (claude-9) finished your straight_angle packet myself (commit on HEAD: r_straight_angle is gone from FORWARD_RULES; I.13 is citable as CONSTRUCTIONS["proposition_13"], cite Book/Prop13.lean:6; footprints updated). Thank you for the edit; it was right.

Goal: replay Book I.14 (/home/joe/code/LeanEuclid/Book/Prop14.lean, read it: 22 lines) in the engine, citing proposition_13 as a lemma.
  proposition_14 : distinctPointsOnLine a b AB ∧ distinctPointsOnLine b c BC ∧ distinctPointsOnLine b d BD ∧ c.opposingSides d AB ∧ ∠a:b:c + ∠a:b:d = ∟ + ∟ → BC = BD
Lean: by_contra (BC ≠ BD); extend_point BC c b as e; proposition_13 a b e c AB BC; by_cases a.sameSide e BD, each branch sum_angles_onlyif (b a d e AB BD / b a e d AB BC) then euclid_finish.

Useful anchors (grep to confirm; numbers move): r_degenerated_angle ~821, FORWARD_RULES ~1160, extend_point ~1513, sum_angles_onlyif ~1724, proposition_13 ~1776, _by_contra ~2154, _by_cases ~2225, replay ~2284, PROP13_HYPS_DATA ~2778, _prop13_branch ~2803, main ~2883. Pattern to copy: PROP13_* and main()'s "13" branch.
Files: scripts/euclid_engine.py and scripts/euclid_engine_test.py only.

Do:
1. Decide how "BC = BD" is represented. The engine has neq_line (directed) but, as far as I know, no line-equality fact. Options: (a) a by_contra whose branch assumes ("neq_line","BC","BD") and must reach a contradiction, with the outer conclusion added as incidence facts (on_line d BC, on_line c BD), which is what BC = BD means for these points; (b) a new eq_line fact kind. Pick one, say why in a comment, and make sure no forward rule can derive anything unsound from it. Prefer (a) unless it fails.
2. PROP14_HYPS_DATA / PROP14_STEPS_DATA following Lean's shape. If the engine needs neq_line in both directions or an extra fact, give it as "h" only if it follows from the stated hypotheses, and say how; any "h-sm" input must be named and justified (prefer none). The inner by_cases (a same side as e w.r.t. BD, or not) must be exhaustive: say in a comment why (e.g. a on BD is impossible, or is a third case you handle).
3. main() gains "14" with a goal check; I.1-I.13 footprints unchanged (test_replay_fact_set_footprints pins them; add "14").
4. Bad cases, kept as tests: (a) drop proposition_13 from the steps: refused (the contradiction is not reached); (b) change the hypothesis angle sum to ∠a:b:c + ∠a:b:d = ∟ (or drop it): refused; (c) c and d on the SAME side of AB instead of opposing: refused; (d) remove one by_cases branch: say what happens (by_cases does not check exhaustiveness; pin the current behaviour, as for I.13).
Gates: euclid_engine_test (all), euclid_review_test and euclid_trace_test must still pass (do not edit them); report counts and wall time. Do not touch the viewer, euclid_trace.py, euclid_review.py or euclid_certify.py. Stage explicit paths only; no commit -a, no stash, no push. No work in /tmp beyond mktemp scratch you delete.
Bell claude-9 back with a summary + commit shas, the I.14 footprint, and the representation decision.
