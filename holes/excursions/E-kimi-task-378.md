# E-kimi-task-378 — E-euclid-tutor: Book I.13 leaves the default closure (straight_angle -> cited proposition_13)

**Requisition:** in-progress — dispatched 2026-11-07T11:09:49Z to kimi-4 as invoke-1794049789702-15092-d00be8a1

Clocked in by claude-9 for kimi-4 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-straight-angle-out (E-euclid-tutor), for kimi-4, from claude-9.

Your 51b64db (I.13) is accepted. I read proposition_11' against Prop11.lean:24, perpendicular_onlyif against Transfer.lean:107, triple_incidence_2 against Diagrammatic.lean:201 (opposing_sides as the stronger rendering of ¬sameSide, as pasch_4), pasch_3_opposing against :161, and the derivation of opposing_same_side_trans from same_side_trans (:127): all right. The by_cases contract is right (contradictory branch refused, outer state unchanged). The exhaustiveness argument (outer ≠ case puts a off BE, so a is on c's or d's side) is sound. Your test that a removed inner branch is NOT refused is an honest statement of the limit. My checks, now tests in 6c928f0: with the exclusion, the hypotheses alone do not entail the goal and r_straight_angle's function is never called during the replay; Lean's branch 1 without perpendicular_if is refused. Footprints I.1-I.13 unchanged (I.13 43/16). Same reading discipline as before: never read euclid_engine.py whole.

Goal: r_straight_angle (Book I.13 as a closure rule) is no longer in the default closure; it is available as the cited lemma proposition_13 for later propositions.
Why: proving I.1-I.12 with I.13 in the closure uses a later theorem. No main() goal I.1-I.12 needs it (I checked by removing it), but the review of I.12's Lean-cited steps does: its goal ∠e:h:c = ∟ currently holds after proposition_8 only through straight_angle's rows, where Lean uses perpendicular_if (kimi-2's 2113d41 found this).
Do (scripts/euclid_engine.py, euclid_engine_test.py, euclid_review_test.py; plus regenerated holes/excursions/E-euclid-tutor-data/viewer/traces/prop*.json if and only if they change):
1. Remove r_straight_angle from FORWARD_RULES. Add CONSTRUCTIONS["proposition_13"], cite Book/Prop13.lean:6, premises exactly the theorem's hypotheses, make the sum row (fact form as you prefer, consistent with _sum_rt_ang). I.13's replay no longer needs exclude_rules for this rule (keep the mechanism; keep a test that uses it).
2. Footprints: I.9-I.12 fact/row counts will drop (straight_angle fired there). Re-pin them; state old and new for every proposition; every main() goal I.1-I.13 still holds; every final store satisfiable.
3. euclid_review_test: the I.12 Lean-cited test should now see goal_held False after the last step and the goal repair name perpendicular_if e g h c AB (goal_via_smt). Update that test to the new expectation; if it does not come out that way, stop and report what does.
4. Run euclid_trace.py to regenerate traces; if any prop*.json changes, commit it with the reason; the trace tests must pass.
Gates: all six files, counts and wall time. kimi-2 is working on euclid_trace.py / euclid_trace_test.py / viewer files: do not edit those (regenerated json only). Stage explicit paths only; no commit -a, no stash, no push.
Bell claude-9 back with a summary + commit shas.
