# War Machine application-order audit — 2026-10-11

## Result

The runtime does not carry an application order distinct from construction
precedence.  No sound pre-G application-order certificate can be added until
the specification defines that carrier and its relation to observed execution.

## Evidence

- `DarkTower/WarMachine/ThreeHalvesBlend.lean` defines
  `Policy.applicationOrder : List (Nat × Nat × Nat)` and proves
  `applicationOrder_independent`: changing it affects neither the structural
  cascade verdict nor the construction order.  It does not yet define
  well-formedness, execution meaning, or correspondence for those triples.
- `src/futon2/aif/interpretation_construction.clj` constructs one
  `:precedence` vector and uses it to build the candidate and model rollout.
- `src/futon2/aif/wm/cascade_decision.clj` uses that same `:precedence` as the
  policy family supplied to G scoring.
- `src/futon2/aif/flight_runner.clj` executes the chosen `:precedence` in vector
  order.
- `src/futon2/aif/cascade_evaluation_trace.clj` declares the evaluated model as
  `:first-enabled-union-theta-v1` with that same `:precedence`.
- Repository search found no runtime `:application-order` or
  `applicationOrder` producer/validator.

Thus the present runtime collapses at least three notions: construction linear
extension, model firing precedence, and enactment sequence.  Copying
`:precedence` into a newly named field would not establish the independent
carrier required by Lean.

## Minimal next specification

Define the meaning of one application-order row `(before, after, witness)`:

1. what the two square/pattern identities denote;
2. what the witness identifies (shared object, produced/consumed token, or
   observed transition evidence);
3. whether the relation is a partial order, total schedule, or dynamic
   refinement constraint;
4. how it constrains model firing and actual enactment; and
5. the typed refusal when construction order, application order, predicted
   firing, and observed execution diverge.

Only then can runtime emit a source-pinned `:wm/application-order-v1` receipt
and Lean state its validity and selected-to-enacted correspondence.  This audit
does not alter production behavior or infer application semantics from the
existing precedence vector.
