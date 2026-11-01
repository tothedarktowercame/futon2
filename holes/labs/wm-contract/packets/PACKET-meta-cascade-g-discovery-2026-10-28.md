Discovery only. No code changes, no commits to Lean or Clojure sources.

Context. Your packet 1 (mathlib4 a0ca1b00fd, OuterMissionModel.lean) has been
reverted in fe1965d5c0 on Joe's ruling: the outer loop is the same active-inference
machine as the inner loop, applied to a different cascade, not a second model with
its own Mission/Observations/G. The outer cascade already exists:
futon3/library/meta/meta-outer-policy-cascade.edn (:meta/reason-bearing-meta-selection).
A META policy is a filled instance of it (slots task-kind, target, next-move,
resource-envelope, evidence-channel, stopping-rule; arms ordinary-work and self-heal),
and :meta/minimise-g-over-filled-meta-policies is G over those filled policies.
Background: futon2/holes/labs/wm-contract/NOTE-outer-selects-missions-not-plans-2026-10-28.md,
section "Withdrawn".

Questions (answer each with file:line citations):
1. Can the existing Lean G definitions in mathlib4 DarkTower/WarMachine/GOverCascades.lean,
   CascadeEFE.lean and CascadeEFEPolicies.lean score a filled META policy as they stand?
   Concretely: what type is a cascade / policy there, what does G take as arguments
   (outcome vocabulary, preferences C, predicted outcome distribution, ambiguity,
   epistemic term), and what, if anything, stops a META cascade with these six slots
   and two arms from being an instance? List any missing piece as a precise gap
   (e.g. "no notion of a slot binding", "outcome type fixed to X").
2. What does OuterCascadePolicyObjective.lean (2026-10-19, ea69853cdd / fcf9b6ad27)
   already define and prove? Does it already treat the outer policy as a filled
   META cascade, or as a tactical cascade chosen across all targets (the defect in
   the note)? Which of its definitions would a META-policy file reuse, and which
   conflict?
3. Is there any existing Clojure that reads meta-outer-policy-cascade.edn or fills
   its slots (grep futon2, futon3c, futon3 for reason-bearing-meta-selection,
   fill-meta-policy-slots, minimise-g-over-filled-meta-policies)? Where does the live
   outer selector compute G today, and is it the same function the inner level uses?
4. Propose, in at most 15 lines, the shape of ONE Lean file that makes a filled META
   policy an instance of the existing G with no new scoring definitions, with a
   two-policy example (one target, next-move :advance vs :unblock) whose argmin
   flips on one evidence-channel observation, and which exercises the
   epistemic/information-gain term with a nonzero value. Say which existing
   definitions it imports.

Acceptance: a written answer to 1-4 with citations; say explicitly where you searched
and found nothing. Write it to
futon2/holes/labs/wm-contract/DISCOVERY-meta-cascade-g-2026-10-28.md and commit only
that file (stage the explicit path; futon2 is a shared checkout).

Bell claude-11 back with a summary + the commit sha.
