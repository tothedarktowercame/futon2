# L5-2C-a — Inner law: enact the strict G minimiser over prediction classes (claude-2 → claude-3, 2026-11-01)

First Clojure change to the inner selection path under the 2026-11-01 incident. Authority: Joe's
ruling 1 (record `reconciliation/RULING-inner-selection-enacted-object-2026-11-01.md`); Lean
`Requirements.lean` at `221609fe66` (Q11: chosen G strictly below every other admissible compared
policy's and below abstention; same-prediction variants are one policy). Your L5-1 report
(`reconciliation/L5-1-inner-law-path-claude3-2026-11-01.md`) is the map; cite it, not my summary.
Ruling 3 (refused stays compared) is the next packet, L5-2C-b; do not fold it in.

Base: futon2 `main` at `518d03631` or later.

## Behaviour

At hop 7–8 of your path (`policy.clj:509` `bayes-choice`, `:524` `chosen-entry`), the enacted
policy becomes: the compared policy with the strictly smallest G, where policies with the same
prediction digest (the L4c-1 digest, `wm_run_facts.clj` `candidate-prediction-row`'s object:
progress steps' predictions plus class-emission steps' predictions) form one class and the class
is represented by one entry. Concretely:

1. **Classes.** Group the scored candidates by prediction digest. Within a class, G must agree
   (your L4b: it does in all 13 runs); if it does not, refuse `:same-prediction-different-G`
   naming the class, since the model then disagrees with itself.
2. **Minimum.** Among classes, the one with the strictly smallest G. An exact tie among classes at
   the minimum refuses `:tie-at-minimum` naming the tied digests and G. No name order, no list
   order, no action order. (`:tie-break-rule` is removed from the law; see readers.)
3. **Representative.** The enacted policy is the class's first member in ranked order, and the
   record says so: `:class-members [keys…]`, `:enacted <key>`, `:representative-rule
   :first-in-ranked-order-within-one-prediction-class`. This is not a selection among policies,
   since the members are one policy under the model; it is a choice of arrangement, and it is
   recorded as such.
4. **Abstention floor.** Where a stopped-kernel G exists (not yet on the inner path, L5-3), the
   minimum must also be strictly below it. Until then the record carries
   `:abstention {:status :absent :reason :no-stopped-kernel-g}` as it does today; do not invent
   a value.
5. **Posterior kept as a record.** σ(ln E − F − G/β) is still computed and written
   (`:posterior`, `:softmax-weights`) but decides nothing. `:action-marginal` and
   `:action-comparison` are no longer computed; write
   `:selection-law {:law :strict-G-minimum-over-prediction-classes :retired [:action-marginal
   :tie-break-rule] …}`.
6. **Truthful `:decided-by`.** `cascade_selection.clj:337-346`'s `:policy-comparison :decided-by`
   describes the enacted class against the runner-up class by G; the term that favoured it is
   still worth recording, but only between those two.
7. **Gate agrees.** `decision_gate.clj:262` `check-cascade-decision!` stops recomputing the
   marginal and checks: the chosen is a member of the strictly minimal class, and no class ties
   it. Its refusals `:chosen-not-bayes-action` / `:chosen-mass-not-marginal` become
   `:chosen-not-minimum` / `:minimum-not-unique`.
8. **Terminal receipt.** `wm/terminal_receipt.clj:64` reports the *enacted* policy's G, not the
   argmax's; with this law they coincide, but read from the enacted entry.
9. **`full_loop_runner.clj:2971`** throws when `:tie-break-rule` changes while the posterior has
   a tie; with the law's tie now a refusal, change that check to assert the new law's name and
   remove the tie-break dependence. Do not leave a throw that fires on every run.

## Readers (from your §7; change or exempt each, and say which)

- futon3c `live_recommendation.clj:81`, `war_machine_pilot.clj:278` read `:softmax-weights`:
  **exempt**, the key stays.
- futon3c `loop_learning.clj`, `stack_generator.clj` mention `selection-law`: read them; if they
  read `:action-marginal` or `:tie-break-rule`, list them as L5-2C-c work, do not change futon3c
  in this packet.
- `wm/cascade_decision.clj:350` (R14 lane) second caller: say whether it goes through the same
  hop and so changes with it.
- `controlled_report_card.clj`, `wm_report_card.clj`: if they render `:action-comparison`, make
  them render the class comparison or print a typed absence; do not let them crash.

## Tests (your §3, by deftest; one live pin each)

Change: `selection-law-candidate-test/the-candidate-is-the-chosen-action-not-the-posterior-mode`
(now: the candidate is the minimum class's representative; equals the mode when E, F are flat),
`decision-gate-test/non-bayes-choice-is-refused` → `non-minimum-choice-is-refused` and
`tied-minimum-is-refused`, `selection-certificate-test/co-application-marginal-pools-cascades-with-the-same-roots`
→ same-prediction cascades form one class, `ranked-candidates-test/ranking-retains-more-than-ten-and-uses-action-tie-rule`
→ twelve equal-G distinct-prediction classes refuse `:tie-at-minimum`,
`wm01-bindings-test/decision-law-and-revision-bindings` (law name),
`enacted-step-test/enacted-steps-recorded-on-the-selection-law`,
`ticket-queue-test/front-ticket-wins-with-full-family-evidence`, and the two golden-decision tests
(regenerate the goldens from the new law and say what changed in them). Add: on the real
376d76ff certificate (fixture subset like `ac5bc477-carriers.edn`), the enacted policy is
`policy-34c8…` (G 1.4034), not `policy-7d91…` (your L4b example), and `:decided-by` names G.

## Gates

clj-kondo and `check-parens.el` on every touched file; the test namespaces above plus
`futon2.aif.wm-run-facts-test`, `wm-check-run-facts-test`; `python3 test/scripts/wm_run_alert_test.py`.
Then project the 376d76ff fixture's decision through the real pipeline where you can: at least,
hand-build the work export with the new `:chosen` and run the checker and Lean; Q11 stays
unverifiable (no abstention G) but `currentDecision` must be the minimiser's key. Branch
`claude3/l5-2c-a-inner-argmin` from `main` in `~/worktrees/futon2-l5-2c-a`. No `data/` writes. Do
not merge. Bell claude-2 with sha, test output, the readers table, the golden diff summary, and
anything that does not fit. If the packet is larger than it looks once you are in the code, stop
at step 5 with everything green and say so.
