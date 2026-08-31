# C93 — P-validated-R5 hole census

Date: 2026-08-31  
Contract authority: `86186c37444ac9f1b9d54818b092bbbc586854f4`

This pass changes no declaration or binding.  The live contract has 94
declarations, 22 holes, and exactly the following three holes under the named
P-validated-R5 owners.

## `C` — not separable from its refusal

`C` is the refused declaration itself, not a law adjacent to a second refused
implementation.  Its type asks for a concrete preference function for every
pragmatic vertex (`Holes.lean:67-68`).  No proposition beside it states a law
that could be witnessed independently, and the record names no observation
that selects one such function.

Evidence that would change this finding must name a concrete observation
carrier and a provenance-bearing selection of the preference map used by the
machine.  That would be implementation evidence, not a proof of the present
opaque definition.  The doc tag and `holeDeclarations` entry repeat metadata
for one declaration; the contract does not carry two `C` claims.

## `nonDegenerateAblationLaw` — separable and already witnessed

This is a proposition about one supplied policy list and two supplied graders,
not an implementation of `C` (`Holes.lean:102-111`).  It is therefore fully
separable from the `C` refusal.

C86 already provides the matching evidence and method: the exact dyadic table
records both complete minimizer sets, Lean theorem
`wmRecordedAblationNonDegenerate` proves they are disjoint, and the negative
control makes the graders share minimizers and is rejected.  The current
binding is fresh and `contract_lint` judges it `:conformant`.  No new evidence
kind is needed; a later discharge may replace the remaining `sorry` or amend
the declaration status, but this census does neither.

## `find` — not separable from its refusal

`find` is likewise the refused implementation itself (`Holes.lean:178-179`),
not a second law beside it.  Its neighboring F1–F4 declarations are the
separable laws: containment/typed absence, receipts, non-self-certification,
and falsifiability.  Their `FindReceiptTable` evidence cannot construct the
opaque `find` function and therefore must not be reused as evidence for it.

Evidence that would lift this refusal must supply an actual deterministic
implementation from `Tension × Repository` to `FindResult`, plus executable
examples and the existing F1–F4 falsifiers.  Until then, the doc tag and
`holeDeclarations` entry are two records of one declined claim, not duplicate
contract declarations.

## Gate

At the pinned authority, strict lint remains PASS with counts unchanged:
`72 closed-by-record`, `16 conformant`, `5 refused-implementation`, and
`1 witnessed`; the contract remains `94 declarations / 22 holes`.

Canonical invocation:

```sh
AUTH=$(jq -r '.source["git-sha"]' /home/joe/code/mathlib4/DarkTower/WarMachine/holes-contract.json)
bb -cp . checks/contract_lint.clj --strict \
  --contract /home/joe/code/mathlib4/DarkTower/WarMachine/holes-contract.json \
  --registry checks/witness-registry.edn \
  --report /tmp/C93-contract-lint.edn \
  --authority "$AUTH"
```
