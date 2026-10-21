# Proposal: a focus-class term in the outer target objective (2026-10-21)

Does not change the specification. It brings the outer selector into line with
`mathlib4/DarkTower/WarMachine/R15StrategicTarget.lean` (rev `a394feabff`).
Author: claude-2. Status: a question for the selection owner (codex-4), not a
build order.

## What Joe said (2026-10-21)

- The focus is declared separately. A target is not focused because it was
  selected. The focus in force carries over between rounds.
- The class preference rewards staying on the declared focus and penalises
  drifting off it. It does not forbid an off-focus choice.
- The focus gives the target space a geometry. Work next to the focus is
  adjacent, not off-focus. Repairing an injury is adjacent when the repair
  is what lets the focused work continue, in the same way that Lean work is
  adjacent to the Clojure build it specifies. "I don't see repairing an
  injury as massively changing the focus because it should just allow us to
  get back to work."

R15 now models this: `withRepairs` scores a repair of the focused work
`related` (35/100), not `unrelated` (5/100), and only under the focus it
unblocks (`repair_adjacency_depends_on_focus`).

## Where the code differs

1. **No class term where targets compete.** `outer_target_score.clj` scores a
   target from its predicted outcomes, resource cost, `:recent-selection-cost`
   (0.25) and `:continuation-credit` (0.35) (`resources/wm/outer-learning-authority.edn`).
   Nothing in it reads the declared focus. The class term that does exist
   (`cascade_shape_g.clj/class-emission-model`) ranks reading policies for
   one already-selected target, where every policy has the same class.
2. **Repair is a separate branch.** When an injury is declared,
   `meta_live_outer_selector.clj:229-237` returns `:status :advisory`,
   `:scores-consumed false`, `:selection-basis :machine-injury-repair-cost`.
   Repair targets are not compared with focused work at all.
3. **An open repair is classed `focus`, under any focus.**
   `focus_receipt.clj/classify-target` gives an open repair obligation
   `:relation "focus" :facet "WM"`, and `eligible` short-circuits on
   `active-repair?`, so the facet check against the declared focus never
   runs. Under Joe's reading it is `associated`, and only when what it
   unblocks is related to the focus in force.

## Proposed changes, one handoff each, in this order

A. **Class term in `outer_target_score`.** Classify each candidate with
   `focus-receipt/classify-target` against `focus-receipt/decision-focus-info`
   (the retained focus when no window covers now, the same call
   `interpretation-ask-classifier` already makes). Add the class's C mass to
   the score as an explicit, recorded term. `:unknown` stays unscored, as in
   R15. Acceptance test: two otherwise identical candidates, one focused and
   one useful-elsewhere, are ordered focused-first; swapping the declared
   focus reverses the order.

B. **Repair class in `classify-target`.** An open repair obligation is
   `associated` when the target it repairs classifies as focus or associated
   under the current focus, and otherwise takes that target's class.
   Acceptance test: the same open obligation is `:associated` under a WM
   focus and `:useful-elsewhere` under an unrelated declared focus.

C. **Retire the injury branch** once A and B are in: an injury lowers the
   predicted chance of reaching the focused targets, so focused candidates
   score worse and the related repair wins in the same comparison. This is
   the largest change and needs evidence first: a replay through the serving
   JVM's code path of recent injury rounds, showing the single objective
   picks the repair the branch picked. If it does not, that is a finding
   to bring back to Joe, not a reason to keep the branch silently.

## Questions for codex-4

- Is `decision-focus-info` the right focus read for the outer selector, or
  does the outer selector run before the focus for the round is settled?
- Does the outer score already carry a preference over outcomes that the
  class mass should combine with (the `:preference-distribution`), rather
  than adding a separate term? R15 puts the class in C; adding it to the
  existing C over outcomes may be the cleaner place.
- Do you see a reason the injury branch must stay separate that this note
  misses?
