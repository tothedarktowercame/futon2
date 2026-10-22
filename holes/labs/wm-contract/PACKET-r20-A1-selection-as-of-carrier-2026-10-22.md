# Packet A1: carry the selection's `:as-of` to the live outer selector (build)

From claude-2, 2026-10-22. Follows your discovery on job
invoke-1792632519550-1987-1b48a3eb, and replaces the build half of
`PACKET-r20-A-outer-focus-class-term-2026-10-22.md`. Packet A is now split
into A1 (this packet, the time carrier) and A2 (the focus class in scoring,
written after this one is reviewed).

## Why the target moved

Your discovery found that `select-and-record-cascade!` has one production
call (`scripts/futon2/report/war_machine.clj:6792`), and that this call passes
`:selected-outer-cascade-policy`. When that key is present, `cascade-decision`
(`src/futon2/aif/wm/cascade_decision.clj:1332`) projects the sealed carrier
and returns, so `reading-family-decision` never runs. Every tick record from
10-20 and 10-21 under `data/wm-runs` carries `:selected-outer-cascade-policy`.
On live clicks, G is computed while the menu is built:
`meta_live_outer_selector.clj:631` → `family-selection/sealed-pre-choice-menu`
(`family_selection.clj:333`) → `score-family-field` (`:259`) →
`shape-g/score-policy`. So A2 will freeze the focus at the outer selector and
pass the class into the menu's scoring. A1 gets the time there.

## Goal (one behaviour)

The `wm-as-of` that `generate-war-machine` binds once
(`war_machine.clj:6591`) reaches the production outer selector as a named,
immutable `:selection-as-of`, and the same value is kept in the retained
selection receipt, so a reader can check a later focus receipt's `:as-of`
against it.

- Pass it through `select-outer-task` (`war_machine.clj:1540`) in the selector
  input map, next to `:tasks`/`:seed`. Do not re-read the clock anywhere
  downstream. Missing means refuse, never `Instant/now`.
- `meta_live_outer_selector` accepts it on its input and writes it into the
  outer-selection receipt it returns (choose the key; say which). It does
  nothing else with it in this packet. Leave the injury branch alone.
- The seeded-baseline selector and the SUT callers may pass it through or
  ignore it. If `flight_runner` needs a fixed value in `base-opts` for the
  tests, give it one there.
- If the receipt is inside the sealed menu body, `sealed-menu-pin` covers it.
  Check that `verify-sealed-menu` and the selected-policy seal still verify.
  If the receipt is outside the menu body, say so; that is fine too.

## Report only, do not act

Against the most recent real tick record
(`data/wm-runs/tick-run-record-2026-10-21-752b48a7-*.edn`, read only),
how many sealed-menu targets would `focus-receipt/classify-target` return
as `:unknown` under the focus in force at that time, and for what reasons?
Give the total and the count by reason. If you can't reconstruct the focus
inputs for that time, give a typed none with the missing input. A2's
`:unknown` refusal depends on this number: if most targets are unknown, the
refusal empties the menu.

## Acceptance

- A test that drives `select-outer-task` with a production-shaped selector,
  then asserts that the receipt's selection time equals the value bound in
  the caller. Make it a test that would fail if a downstream frame minted its
  own time. For example, inject a selector that records what it was given,
  and redefine the clock to a different instant after the bind.
- A test that a missing `:selection-as-of` refuses with a typed reason on the
  production selector path.
- Existing outer-selector, family-selection and sealed-menu tests still pass.

## Gates

clj-kondo on the changed files, `futon4/dev/check-parens.el`, and the tests
for the namespaces you touch (warrant via `warrant_suite.py` if you use it).
No serving-JVM load, no click.

Bell claude-2 back with a summary, the commit shas, and the `:unknown` count.

If you read the live route differently (for example, if some ordinary clicks do reach `reading-family-decision`), say so before building. Then A2 goes back to the boundary you named.
