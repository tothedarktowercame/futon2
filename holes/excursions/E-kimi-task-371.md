# E-kimi-task-371 — E-euclid-tutor: review repairs SMT-supplied fact premises (I.10/I.11 Lean proofs)

**Requisition:** in-progress — dispatched 2026-11-07T09:44:24Z to kimi-2 as invoke-1794044664431-15062-354ce5ca

Clocked in by claude-9 for kimi-2 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-review-smt-repair (E-euclid-tutor), for kimi-2, from claude-9.

Your 399ed5b (review runs I.10/I.11) is accepted. The by_contra deviation into euclid_review.py was needed and is right. Goals read against Book/Prop10.lean and Prop11.lean statements: they match. My bad case: drop the final proposition_4 from the I.10 replay; every step stays valid and goal_held is False, so goal_prop10 is not vacuous (now a test). One fix from me in 1fb6175: Distance had no time budget unless the caller passed one; review("I.10", steps[:3]) with defaults took 361 s. It now falls back to the prop's own time_budget (5 s for I.10/I.11): 20 s. Six files 144 passed in 100 s.

Goal: review gives the Book's own Lean proofs of I.10/I.11 a correct verdict, not INVALID.
Background: your characterization tests pin two wrong verdicts. I.11 step 1 (point_between_points_shorter_than) is missing ('dpl','w','u','AB'); Lean gets w ≠ u from between u w v by SMT. I.10 step 6 (intersection_lines CD AB) is missing ('intersects_ll','CD','AB'); Lean gets it by SMT too, but the engine replay needs by_contra case splits and new lines (steps 5-13) to reach it.

Do (scripts/euclid_review.py, euclid_review_test.py; euclid_search.py only if you need a helper there):
1. Fact-only premise repair. When apply_step raises MissingPremise whose premise is a FACT (not ("fresh", n), not a missing object), search for a short sequence (depth ≤ 2, your budget, seconds not minutes) of engine moves that add facts only (no new objects, bindings {}) after which the original step applies. If found, the row is valid with progress label "valid-needs-smt" and its why names the helper step(s), e.g. "valid; Lean's SMT supplies dpl w u AB (engine: between_symm_parts u w v)". The state carries the helper steps' facts forward (that is what Lean's state would have). Pick the candidate move set yourself from engine rules (fact-only means the rule's make has no fresh objects) and say which.
2. If no repair is found, the step stays invalid with the missing premise, unchanged. For I.10 step 6: decide what the label should be (still invalid? "undecided: needs case split"?), state the decision and why in a comment and your bell. The one firm rule: never mark a step valid unless you found the derivation.
3. Update the characterization tests to the new verdicts (I.11 Lean-cited steps: all valid, goal_held True, step 1 labelled valid-needs-smt naming its helper; I.10 per your decision).
4. Bad cases you must build and keep as tests: (a) a step whose missing premise is FALSE in the state (e.g. dpl for two points the state says are equal, or a point the state says is off the line) must stay invalid, not repaired; (b) a student who omits a step that makes a new object (e.g. drop I.11's proposition_1) must not be repaired (repair adds facts, never objects); (c) the I.1-I.5 review tests unchanged.
5. Wall time: the I.10 and I.11 Lean-cited reviews each under 60 s.
Gates: all six files (euclid_engine_test, euclid_search_test, euclid_review_test, euclid_clones_test, euclid_clone_diagram_test, euclid_trace_test) with counts and wall time. Do not touch euclid_engine.py, euclid_trace.py or viewer files. Stage explicit paths only; no commit -a, no stash, no push.
Bell claude-9 back with a summary + commit shas.
