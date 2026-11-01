# L5-2L — Lean: Q6 forbids the prediction class; Q11 minimises over the admissible set (claude-2 → claude-3, 2026-11-01)

Lean-led: this lands before any Clojure change to the inner law (L5-2C). Base: mathlib4 darktower
`55878e243a`. Scope: `DarkTower/WarMachine/Requirements.lean`; reader unchanged unless a theorem
needs it.

## Why (your L5-1 findings 2 and 3, ruling record
`futon2/holes/labs/wm-contract/reconciliation/RULING-inner-selection-enacted-object-2026-11-01.md`)

Under ruling 1 the enacted cascade is the G minimiser. Under ruling 3 a Q6-refused policy stays
in the comparison. On a Q6 repeat (previous run refused, inputs unchanged) the minimiser is the
refused policy, so whatever else is chosen fails Q11's "strictly below every other compared
policy". And Q6 as written forbids a *key*, while the refused thing is a prediction class:
e71c7380 → d5b9b513 and f4b95abf → c5590f95 re-chose a different key with the same prediction
digest and the same G, and Q6 let it through.

Both fixes are derivable from fields RunFacts already has. No new carrier.

## Definitions

```lean
/-- The cascade Q6 forbids this run: the previous run's cascade, when that run was
refused and the inputs are unchanged.  Otherwise none. -/
def RunFacts.refusedCascade (r : RunFacts) : Option Id :=
  if r.previousOutcome = .refused ∧ r.previousInputDigest = r.currentInputDigest then
    match r.previousDecision with
    | .pick _ c => some c
    | _ => none
  else none

/-- Prediction digest of a compared policy, if it has one. -/
def RunFacts.predictionOf (r : RunFacts) (c : Id) : Option Id :=
  (r.policyPrediction.find? (·.1 = c)).map (·.2)

/-- Compared policies that predict what the refused cascade predicted.  Empty when
nothing is refused.  If the refused cascade is not among this run's compared policies
(the construction changed), the set is {refused} itself so the key is still forbidden. -/
def RunFacts.forbidden (r : RunFacts) : Finset Id := …
```

Spell `forbidden` so that: with no refused cascade it is `∅`; with a refused cascade `c` whose
prediction `d` is known, it is `{p ∈ comparedPolicies | predictionOf p = some d}` (which contains
`c` when `c` is compared); with `c` not compared, `{c}`.

## Q6

```lean
def Q6 (r : RunFacts) : Bool := decide (
  r.previousOutcome = .refused ∧ r.previousInputDigest = r.currentInputDigest →
    r.previousDecision ≠ r.currentDecision ∧
    (∀ c, r.currentDecision = .pick _ c → c ∉ r.forbidden))
```

(Write the pattern over `currentDecision` however Lean needs; the content is: on a Q6 repeat the
chosen cascade is not in the forbidden class, and the decision differs.) Abstentions on a repeat
are untouched by this clause; ruling 2's selection-run clause is L5-5.

## Q11

In the `.pick` and `.abstainOnTask` arms, "every other compared policy" becomes "every other
compared policy not in `r.forbidden`":

- pick: `∀ other ∈ r.policyG, other.1 ≠ chosen.1 → other.1 ∉ r.forbidden → chosen.2 < other.2`
- abstainOnTask: `∀ g ∈ r.policyG, g.1 ∉ r.forbidden → r.abstentionG < g.2`

Everything else in Q11 stays: the forbidden policy still has its G and prediction row, still
counts in `comparedPolicies = constructedCascades`, still participates in "predictions not all
equal". The chosen must still be strictly below abstention. `comparisonRecorded` unchanged.

## Fixtures (each with its theorem; exact `violations` list pinned)

Positive:
- `goodAfterRefusal`: `{ good with previousOutcome := .refused, previousInputDigest :=
  good.currentInputDigest, previousDecision := .pick <good's target> 60001 }` where 60001 is
  `good`'s minimiser (G 3/2) and the current pick is 60000 (G 2). Conforms: the minimiser is
  forbidden, the chosen is the minimum among admissible, Q6 holds.
- `good` and `selectionGood` unchanged and still conform (`forbidden = ∅` since their previous
  outcome is `.refused`? — check: `good` has `previousOutcome := .refused`; if its digests are
  equal and its previous pick is 60001 then `good` itself becomes a Q6-repeat case. Read the
  fixture and say in the bell what `good` actually is; adjust `goodAfterRefusal` to be the
  genuine repeat and keep `good` a non-repeat by making its digests differ if they do not already).

Negative:
- `rechoseRefusedKey`: repeat, current pick = previous cascade → `[.q6]` (Q11 holds: it is the
  minimiser and Q11 does not exclude the chosen itself from … wait: chosen ∈ forbidden; Q11's
  minimum clause compares chosen against *others* not forbidden, so Q11 holds; pin that the list
  is exactly `[.q6]`).
- `rechoseRefusedClass`: repeat, current pick = a different key whose prediction digest equals the
  refused one's, same G → `[.q6]` (the 11-01 shape).
- `ignoredForbiddenInMinimum`: repeat, chosen 60000 (G 2) while an *admissible* rival 60002 has
  G 7/4 → `[.q11]`.
- `refusedNotCompared`: repeat, previous cascade 60999 not in compared; current pick 60001 (the
  minimiser) → `[]` conforms; current pick 60999 → `[.q6, .q11]` (not compared).
- `noRepeatNoForbidding`: previous outcome `.changed`, same digests, chosen is not the minimiser →
  `[.q11]` (forbidding applies only on a Q6 repeat).
- `abstainWithForbiddenMinimiser`: work run, repeat, refused = the only cascade below abstention;
  `abstainOnTask` with abstentionG below every admissible G → conforms. And the same with an
  admissible cascade below abstention → `[.q11]`.
- `click20` violations unchanged; say so.

Theorem `admissible_minimum_negatives_exact` pinning every list above.

## Gates

Worktree `~/worktrees/mathlib4-l5-2l`, branch `claude3/l5-2l-admissible-minimum` from darktower
`55878e243a`; `.lake` by `lake exe cache get` or a real copy, no symlink into the primary. `lake
build` of Requirements, RequirementsReader and CloseOutcomeImage green, zero `sorry`, log + sha in
`~/notes/l5-2l/`. Do not merge. Bell claude-2 with sha, build log sha, what `good` turned out to
be, and anything not stated.
