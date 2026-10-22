# Packet A2: score each menu policy with its target's class under the frozen focus (build)

From claude-2, 2026-10-22. Follows A1 (`aaa85f9b4`, reviewed: the live
outer selector now receives `:selection-as-of`, refuses without it, and keeps
it at the top level of its receipt). This is the second half of
`PACKET-r20-A-outer-focus-class-term-2026-10-22.md` Revision 1.

## Goal (one behaviour)

On the live route, every policy in the sealed pre-choice menu is scored with
the class of its own target, read from one focus context frozen at the
receipt's `:selection-as-of`, in place of the hard-coded
`:target-class {target :focused}` (`cascade_shape_g.clj:48`).

## What to build

1. **Freeze the focus once, in `select-live`.** Before `policy-menu-fn` runs
   (`meta_live_outer_selector.clj:631`), call `focus-receipt/read-inputs` once
   and `focus-receipt/decision-focus-info` once at `selection-as-of`. Classify
   each on-map target with `focus-receipt/classify-target` in that context.
   No other frame reads the focus or the clock for this selection. If
   `selection-as-of` is absent on this path, nothing is classified and the menu
   is not built (refuse, typed; A1 already refuses earlier on the production
   path, this is for direct `select-live` callers that reach the menu).
2. **Carry the classes into scoring.** Give `sealed-pre-choice-menu` the
   per-target classes (through `field` or `join`, your choice; say which).
   `score-family-field` attaches each target's class to its policies, and
   `score-policy` passes it into `score-arranged`.
3. **Map relation to scorer class with the existing table.**
   `apparatus_certificates.clj:114` (`scorer-class`: focus→focused,
   associated→related, useful-elsewhere→unrelated), which is Lean
   `scorerClass` (`mathlib4/DarkTower/WarMachine/CTauClassPreference.lean:133`).
   Move it somewhere both namespaces can require, or require it where it is.
   Do not write a second table.
4. **`:unknown` is refused before `score-arranged`.** A policy whose target
   classifies `:unknown` gets a typed scoring failure
   (`:kind :target-class-unknown`, carrying the classifier's `:reason`), so it
   lands in the menu's `:failures` through the existing `score-failures` route
   and never reaches the observation model. Other targets still score.
5. **Production scoring requires a class.** `score-policy` refuses, typed
   (`:target-class-required`), when its policy has no class. No fallback to
   `:focused` on the menu route.
6. **The readiness default stays.** `class-emission-readiness` and the
   3-arity `score-arranged` used by `scripts/*_four_numbers.clj` keep
   `:focused`, still with `:provenance {:status :synthetic ...}`. When the class
   comes from the frozen focus, the class model's provenance says so instead
   (the focus `:as-of`, the relation source, `:calibrated false` still).
7. **Record what G read, inside the sealed body.** Put a focus/class receipt in
   the sealed menu body, so `sealed-menu-pin` covers the classes that entered
   G: `:as-of` (must equal `:selection-as-of`), the focus in force, its
   status/origin, and per target `{:class … :scorer-class … | :reason …}`.
   Add a class census to the body: count per class, and `:unknown` count by
   reason. Old sealed menus without it must still verify (typed absent, not a
   verify failure).
8. **Bump `scorer-version`** (`cascade_shape_g.clj:511`) to a new keyword
   naming the change, because the scoring rule changed.

Out of scope here: the apparatus-certificate rows in
`apparatus_certificates.clj` reading this receipt (A3), the injury branch,
`outer_target_score`, weights, and the preference numbers (55/35/5/5 stay).

## Acceptance

- Two otherwise identical targets in one sealed menu, one `focus` and one
  `useful-elsewhere`: the focus target's policies have lower G and rank
  first; swapping which target is the focus reverses the order. Drive this
  through `sealed-pre-choice-menu` (or `select-live` with injected inputs),
  not by calling `score-arranged` directly, so the test shows the class
  reaching the union the selector sees.
- An `:unknown` target: no class model is built for its policies, it appears
  in `:failures` with `:target-class-unknown` and the classifier's reason, and
  the census counts it under that reason. The other target still scores.
- Construct the bad case: a focus context whose `:as-of` differs from
  `:selection-as-of` is refused (test it), and a policy reaching `score-policy`
  on the menu route without a class is refused (test it).
- `class-emission-readiness` still returns `:ready` with `:focused`.
- `verify-sealed-menu` and the selected-policy seal verify for a new menu, and
  an existing pre-A2 sealed menu fixture still verifies.
- Report, do not act: with the frozen focus, how many targets in a menu built
  from the current pipeline state would be `:unknown`, by reason. You may
  compute it in a test JVM from read-only inputs. No serving-JVM load, no
  click. If most targets are unknown, say so before anyone clicks: the menu
  then drops them, and an all-unknown field ends in
  `:no-canonically-bound-policy`.

## Gates

clj-kondo on touched files, `futon4/dev/check-parens.el`, the tests for the
namespaces you touch (`cascade-shape-g`, `family-selection`,
`meta-live-outer-selector`, `focus-receipt`, and anything that pins
`scorer-version`). Ground rules in `futon2/CLAUDE.md` apply.

If you think the class belongs somewhere else on this route, say so and stop
before building.

Bell claude-2 back with a summary, the commit shas, and the `:unknown` count.
