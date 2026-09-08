# C589 — U68: candidate action space π stated in Lean

**Row:** `:U68`. **Lean commit:** mathlib4 `738cae3a54`.

## One carrier, two representations

The F12 ruled signature already accepted the relevant mathematical space as a
`Set P`. `CandidateActionSpace P` names that same type, and
`RuledOrganiseType` now uses the name at
`mathlib4:DarkTower/WarMachine/F12RuledCarrier.lean:16-22`. It is an
abbreviation, so no second carrier or conversion obligation was introduced.

Production does not receive a set. Every `select-action` branch receives a
ranked list (`futon2:src/futon2/aif/policy.clj:672-862`). `machinePolicySet`
maps that list extensionally into the shared carrier at
`mathlib4:DarkTower/WarMachine/MachinePolicySet.lean:19-22`.

## The three rules remain visible

`selectionPolicySet` does not pretend that one policy-selection rule runs.
The strategic controller-head branch reads the ranked list, the strategic
full-score-posterior branch reads the scored list, the no-prior actuation
branch reads the ranked list, and the habit-prior actuation branch reads the
scored list (`mathlib4:DarkTower/WarMachine/MachinePolicySet.lean:24-37`),
matching the dispatch at `futon2:src/futon2/aif/policy.clj:567-594` and
`futon2:src/futon2/aif/policy.clj:809-838`.

The production fixture used by the reviewed action carrier is read back at
`mathlib4:DarkTower/WarMachine/MachinePolicySet.lean:39-67`. All three rules
range over action identities 0 and 1 even though the ranked and scored records
carry different score fields. This is a witness on production-shaped values,
not a claim that the rules choose the same action.

## Independent review

Zai-1 reviewed mathlib4 `738cae3a54` under Agency job
`invoke-1788857703186-14750-435bc1ba` and requested no changes. The review
checked the alias against F12, each dispatch against
`futon2:src/futon2/aif/policy.clj:567-594` and
`futon2:src/futon2/aif/policy.clj:809-838`, and re-ran both touched modules:
each exited 0 with no warnings, both files contained zero `sorry` tokens, and
the printed axioms contained no `sorryAx`.

## Scope and gates

No Clojure production source changed and no ruling was written. The registry
row uses the ordinary `:lean`, `:lean-status`, `:lean-at`, and `:lean-note`
fields at `holes/labs/wm-contract/aif-equations.edn:158`. The published Box 7
artifacts are not regenerated under TN §9a; the U35 probe is re-run after the
registry commit so its join can verify 18 of 18 directly.
