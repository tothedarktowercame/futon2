# C590 — F11: draft Lean signature extension for `find` (FOR JOE'S REVIEW)

2026-09-08, claude-1, per the Item-7 ruling (RULINGS-walkthrough-2026-09-08.md):
"we want the signature extended." Lean edits are Joe's by name, so this is a
draft, not an edit — nothing in Holes.lean changes until Joe accepts or amends.

## The gap being closed

The executable `find` (futon3:checks/find_organise.clj:227-253) takes a
caller-supplied `fires?` predicate on `[pattern context]` and an optional
receipt constructor that may ADD fields but cannot remove `:route` or
`:warrant`. Holes.lean's `Tension` (context/want/however, :126-130) and
`Repository` (patterns/standsOn/acyclic, :121-124) expose neither operation,
so laws F1–F4 cannot be STATED in Lean against the real operation.

## Draft

```lean
/-- A find query joins a tension to its domain-specific antecedent evaluator.
    `fires?` is the whole of what is domain-specific (find_organise.clj:230);
    it reads a pattern against the tension's context, nothing else. -/
structure FindQuery (State P : Type*) where
  tension : Tension State
  fires? : P → State → Prop

/-- A receipt names its route and its warrant. Extension fields live in
    `extra`; `route` and `warrant` are structure fields, so no caller can
    make a receipt self-certifying by omission — F3 by construction, the
    same way the clj receipt constructor "may add fields; it may not remove
    :route or :warrant". `Warrant P` is the standsOn-derived justification;
    its exact carrier is the one open choice this draft leaves to review. -/
structure Receipt (P Extra : Type*) where
  pattern : P
  route : Route
  warrant : Warrant P
  extra : Extra

structure FindResult (State P Extra : Type*) where
  selected : List P
  receipts : ∀ p ∈ selected, Receipt P Extra

def find (q : FindQuery State P) (R : Repository P) : FindResult State P Extra
```

## How F1–F4 become statable

- **F1 containment**: theorem `∀ p ∈ (find q R).selected, p ∈ R.patterns` —
  provable by construction if `find` filters `R.patterns`, exactly as the clj
  comment argues ("the candidate set IS repository.patterns").
- **F2 receipted**: the dependent field `receipts : ∀ p ∈ selected, …` makes
  an unreceipted selection unrepresentable, strictly stronger than the clj
  (which pairs maps and could in principle drop a key).
- **F3 non-self-certifying**: `warrant : Warrant P` is computed from
  `R.standsOn`, not from the query; the query's only degree of freedom is
  `fires?`, which cannot reach the warrant.
- **F4 falsifiable**: theorem `∀ p ∈ (find q R).selected, q.fires? p
  q.tension.context` — any selected pattern whose predicate does not fire
  refutes the implementation.

## Open choices for review

1. **`Route`**: an inductive (`| structuredAntecedent | …`) or an opaque
   parameter. The clj default is `:structured-antecedent`; an inductive pins
   the vocabulary, a parameter defers it.
2. **`Warrant P` carrier**: the clj `warrant` derives from repository edges;
   candidates are `List P` (the standsOn chain) or a `standsOn`-indexed
   proof-carrying structure. This decides how much of F3 is enforced by type
   versus by theorem.
3. **Whether `Tension` itself gains `fires?`** instead of the wrapper
   `FindQuery`. The wrapper keeps the P-validated-R5 §3e Tension untouched
   (its CLOSED-BY-RECORD marker survives); folding it in reopens that record.
   Draft prefers the wrapper for that reason.
4. **Decidability**: `fires? : P → State → Prop` states the laws;
   `DecidablePred` (or `Bool`) is needed if `find` is to compute inside Lean.
   The clj side only needs the Prop-level laws, so the draft stays with Prop.
