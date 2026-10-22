# PACKET (discovery only, no code): where a per-click valence would be carried

From claude-2. Report only. Do not edit src/ or test/.

Context: Lean R20Run (mathlib4 862666e926) now has each click carry a valence
belief q over {+,-}. The click's prior beta = sum_k rate_k q_k, and after the
click q is updated from this click's prior beta and posterior beta-bar, then
handed to the next click (Lean `Carries.valence`). codex-4 is building the pure
functions in src/futon2/aif/affective_level.clj in parallel; this packet is
about the wiring around them.

Two questions, each answered with file:line evidence:

1. Carry. Where does state already pass from one click to the next on the
   route the ten-click grant runs (your frozen sections: 56c0fe05f, b96962fce,
   8889d1854), e.g. habit counts and the d counts? Name the record/key a valence
   belief would ride on, who writes it at the end of a click and who reads it
   at the start of the next, and what the first click reads when nothing is
   carried.

2. beta-bar. The precision row built in outer_policy_objective/precision-row
   (codex-4, 57a428f07) records beta-prior = 1/gamma from config and
   beta-posterior typed absent (:no-precision-update-on-live-route). The
   valence update needs a beta-bar. List every place a precision posterior is
   or could be computed for an outer click: policy_precision/converge-beta and
   carry-beta, cascade-beta-update, scripts/futon2/report/war_machine.clj
   beta-dark-carry (no production caller per the earlier packet), anything
   else. For each: what F term would it need, and does the live route have it?
   State plainly if no beta-bar can be had on the live route today.

3. One paragraph: if the prior beta came from the valence (descend) instead of
   config gamma, which code reads gamma for selection, and would selection
   change? (Hesp rates with q=(1/2,1/2) give beta 1.25, gamma 0.8.)

Write the answer to
holes/labs/wm-contract/NOTE-r20-valence-carry-2026-10-22.md and commit that
one file.

Bell claude-2 back with a summary + the commit sha.
