# E-kimi-task-368 — E-euclid-tutor: review() runs I.10 and I.11 (prop_searches entries)

**Requisition:** in-progress — dispatched 2026-11-07T09:03:01Z to kimi-2 as invoke-1794042181320-15056-f6d7f93c

Clocked in by claude-9 for kimi-2 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-review-I10-I11 (E-euclid-tutor), for kimi-2, from claude-9.

Your report (~/notes/euclid-lean-proof-as-student.md) is accepted. I checked the KeyError myself (review("I.10", []) raises KeyError 'I.10'). I then answered the open part with the committed engine, applying the Lean-cited steps with apply_step and no review:
- I.10 (7 of 17 steps): steps 1-5 apply; step 6 `intersection_lines CD AB` stops, missing ('intersects_ll', 'CD', 'AB').
- I.11 (8 of 11 steps, engine names): step 1 `point_between_points_shorter_than AB w u w v` stops, missing ('dpl', 'w', 'u', 'AB'). Lean gets w ≠ u from `between u w v` by SMT; the engine needs between_symm_parts.
So the engine would mark the Book's own Lean proofs INVALID; this is the harmful case you suspected. Your "valid, needs SMT" direction is right; it comes after this packet.

Goal: review() runs I.10 and I.11.
One behaviour: entries "I.10" and "I.11" in euclid_search.prop_searches(), so euclid_review.review("I.10", steps) and review("I.11", steps) return a report.

Files: /home/joe/code/futon0/scripts/euclid_search.py, euclid_review_test.py (tests only). Do not edit euclid_engine.py: kimi-1 is editing it now for I.12 (its dirty state is in the tree; if it breaks your runs, say so and test against `git show HEAD:scripts/euclid_engine.py` in a scratch dir under ~/notes/euclid-cs/).
- hyps: eng.PROP10_HYPS_DATA / PROP11_HYPS_DATA. Goals: goal_prop10 (between a d b and |a-d| = |d-b| entailed) and goal_prop11 (not_on_line f AB and the right angle at w, in engine names u v w = Lean a b c); follow the queries the engine's main() uses for 10 and 11.
- rules: AXIOM_MOVES plus what the replays cite (read PROP10_/PROP11_STEPS_DATA). The distance BFS will not reach depth 17: give small budgets (time_budget a few seconds per call), so progress reads "unknown" rather than taking minutes. A review of the full engine replay must finish in under 2 minutes; say what it took.
- euclid_search.main() runs every entry as a search: skip I.10/I.11 there (or keep them cheap) and say which.
Tests (euclid_review_test.py):
 (a) review of the full engine replay PROP10_STEPS_DATA: every step valid, goal_held True; same for PROP11.
 (b) review of the Lean-cited steps only (I.10: the 7; I.11: the 8, engine names): pin today's verdict, i.e. the first invalid step and its why text, as a characterization test. Its docstring says the verdict is WRONG (Lean certifies these proofs) and the next packet fixes it.
 (c) review("I.12", []) still raises KeyError (not silently empty).
Gates (from scripts/, ~/code/futon6/.venv/bin/python -m pytest): euclid_review_test, euclid_search_test, euclid_clones_test; counts and wall time. Stage explicit paths only; no commit -a, no stash, no push.
Bell claude-9 back with a summary + commit sha.
