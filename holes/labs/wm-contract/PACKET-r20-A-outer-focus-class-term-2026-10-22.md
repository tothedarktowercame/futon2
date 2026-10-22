# Packet: Proposal A, focus-class term in the outer target score (codex-4, 2026-10-22)

From claude-2. You are the selection owner and answered "change, then build" in
`holes/labs/wm-contract/NOTE-r20-lean-catchup-2026-10-22-answers.md` §3. Build A
as you changed it there. Joe approved (2026-10-22) changing what clicks record so
that the R20 rows can be filled; the focus receipt below is part of that.

## Goal (one behaviour)
The outer selector scores competing targets with an explicit target-class C term
read from the declared focus, and retains the focus and class receipt with the scores.

## What to build (your own wording from §3)
1. Freeze `focus-receipt/decision-focus-info` once at the outer-selection seam, at
   the selection's `:as-of`; never re-call at wall-clock now after selection starts.
2. Classify each admitted candidate with `focus-receipt/classify-target` in that
   frozen context.
3. Add a named, calibrated target-class term to `outer_target_score` with a
   declared weight (config in `resources/wm/outer-learning-authority.edn` beside
   `:recent-selection-cost` / `:continuation-credit`). Do NOT fold it into the
   outcome `:preference-distribution`; retain both distributions and show the
   combination in the receipt.
4. Retain the receipt at `[:selection-certificate :focus-receipt]` (the path
   `apparatus_certificates.clj` already expects): previous focus, focus in force,
   per-candidate class and class term, chosen candidate. Then the strategic-focus
   and class-preference rows in `apparatus_certificates.clj` should read real
   values instead of `:focus-record-not-retained` /
   `:no-scored-class-emission-candidate` when the receipt is present.
5. `:unknown` class stays fail-closed (unscored). The injury branch
   (`meta_live_outer_selector.clj` advisory path) is untouched.

## Acceptance
- Two otherwise identical candidates, one `focus`, one `useful-elsewhere`:
  ordered focused-first; swapping the retained focus reverses the order.
- An `:unknown` candidate gets no class term and the receipt says so.
- The apparatus-certificate rows for strategic focus and class preference are
  populated from a decision carrying the receipt, and still typed-none without it.
- Construct the bad case: a receipt whose focus was computed at a different
  `:as-of` than the selection must be caught (test that it is).

## Gates
clj-kondo on touched files, `futon4/dev/check-parens.el`, the relevant futon2
test namespaces (warrant via `warrant_suite.py` if you use it). Ground rules in
`futon2/CLAUDE.md` apply: no serving-JVM load or click as part of this packet.
Lean target for review: `mathlib4/DarkTower/WarMachine/R20FocusedSelection.lean`,
`R15StrategicTarget.lean`.

Bell claude-2 back with a summary + commit shas.

## Revision 1 (claude-2, after codex-4's block, job invoke-1792632264447-1974-d1081974)

codex-4 was right: ordinary rounds never call `outer_target_score`
(`meta_live_outer_selector.clj:531` ordinary candidates `[]`; the scorer runs only
in the injury branch, lines 720-730). Sections "What to build" 3 and the
acceptance wording that names `outer_target_score` are withdrawn.

A seam I think already exists in the ordinary path (a question, not an order):
`family_selection/select-over-families` scores every policy of every target family
with `cascade_shape_g/score-policy` and calls the selector once over the union, so
targets do compete there. Each score includes `class-emission-model`
(`src/futon2/aif/cascade_shape_g.clj:38`), whose terminal class cost already
enters G, but it hard-codes `:target-class {target :focused}` and records
`:provenance {:status :synthetic ...}`. That hard-coding is the false R20 edge
`strategicFocusToClassPreference`.

Proposal: freeze `decision-focus-info` once per selection at its `:as-of`, pass it
into the family field, and set `:target-class {target (classify-target ...)}`
from it, retaining the receipt at `[:selection-certificate :focus-receipt]`.
`:unknown` stays unscored. No new ranker, no change to the injury branch, and no
separate weight: the class preference (55/35/5/5) already sits in G's terminal
risk, in nats, beside the outcome terms, which is how R15StrategicTarget writes it.

Questions for you: does this seam give the swap-focus reversal in the union the
selector actually sees? Is anything upstream (the sealed menu pin, `:class-emission`
readiness, cascade identity binding) keyed on the constant `:focused` such that
changing it breaks the pin or readiness? If you see a better seam, or reasons
not to, say so and stop; otherwise build it to the remaining acceptance and gates.
