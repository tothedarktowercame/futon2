# Keep the outer target's G unaltered: move engineering costs into the policy prior and drop the clamp

**Status:** OPEN
Parent: M-wm-strategic-mission-selection

## Observed failure

R5 of the PLoP catalogue says engineering terms are a named augmentation
"reported beside that core and never folded into it". In
`src/futon2/aif/outer_target_score.clj` at `b04e2df1c8db` the value the live outer
selector ranks (placed on `:outer-target-outcome-cost` by
`meta_live_outer_selector.clj`) is

    :g = canonical-g / 2 + weighted-resource-cost + cadence-cost - continuation-credit,
         clamped to [0, 1]

So the pick moves on the sum, not on G; and the clamp maps every score past
either bound to the same value, which manufactures exact ties.

Also: `ambiguity` is divided by ln|O| while `risk` is not, so one term of
G is rescaled relative to the other.

## Scoped task

1. Rank on G = risk + ambiguity (less novelty, once
   `T-outer-target-novelty-term.md` lands), with both terms in nats.
2. Carry resource, cadence and continuation terms as an explicit log policy
   prior, ln E, combined in the active-inference form
   pi proportional to softmax(ln E - gamma G), and record E's terms beside G.
3. Remove the [0, 1] clamp.

## Acceptance evidence

- A regression where two targets differ only in resource cost: G is equal,
  ln E differs, and the selection follows E with both recorded.
- A regression where unclamped scores beyond 1.0 used to tie: they no longer
  tie.
- A check that risk and ambiguity are in the same units (nats) in the
  persisted terms.

## Provenance

Found by claude-4 while annotating R5 for the plop2026 docbook
(`p4ng/plop-2026-code-annotations.json`), 2026-10-18. Written as a ticket at
Joe's request so the War Machine may select it.
