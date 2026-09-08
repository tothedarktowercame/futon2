# C590 — F11: revised Lean signature extension for `find` (FOR JOE'S REVIEW)

2026-09-08, wm-build-work, F11 slice 13. This revision incorporates the
second-reader findings in `C591-F11-find-signature-draft-review.md:11-68` and
Joe's later disposition of the original draft's four open questions in
`RULINGS-walkthrough-2026-09-08.md:211-219`. It is still a draft: the Lean
edit is Joe's by name, and this slice changes neither `Holes.lean` nor a
registry choice.

## What is now fixed

Joe chose an inductive route vocabulary, a `standsOn` descent-chain warrant,
the `FindQuery` wrapper, and Prop-level laws
(`RULINGS-walkthrough-2026-09-08.md:211-219`). The existing typed absence is
retained (`mathlib4/DarkTower/WarMachine/Holes.lean:234-250`). F4 remains the
authoritative exclusion property — at least one repository member is not
returned — rather than the first draft's antecedent-soundness substitution
(`holes/problems/P-validated-R5.md:484-488`).

The receipt exposes the three kinds of evidence F2 names: acknowledged
IF/HOWEVER clause, retrieval route, and as-of
(`holes/problems/P-validated-R5.md:486`). Its citation is separately either
pattern text or an authored `standsOn` descent, matching F3's stated
alternatives (`holes/problems/P-validated-R5.md:487`). This does not decide
which concrete clause or timestamp carrier the runtime should serialise; the
parameters keep those representations explicit.

## Applicable replacement block

This is a replacement for the current `Receipt`, `FindResult`, and opaque
`find` declarations at `mathlib4/DarkTower/WarMachine/Holes.lean:240-264`, not
an additional declaration with colliding names. `find` remains `opaque`: Joe
selected Prop-level laws, so this block states the interface and does not
pretend that a `Set` can be filtered computationally without decidability
(`RULINGS-walkthrough-2026-09-08.md:211-219`). The executable implementation
remains the Clojure function at `futon3:checks/find_organise.clj:227-253`.

```lean
inductive FindRoute where
  | structuredAntecedent
  deriving DecidableEq, Repr

inductive FindClauseKind where
  | ifClause
  | howeverClause
  deriving DecidableEq, Repr

structure FindQuery (State P : Type*) where
  tension : Tension State
  fires : P → State → Prop

/-- An authored descent beginning at `pattern`.  An empty tail cites the
    pattern itself; a nonempty tail cites authored repository edges. -/
structure FindWarrant {P : Type*} (R : Repository P) (pattern : P) where
  tail : List P
  members : ∀ p ∈ tail, p ∈ R.patterns
  descent : List.Chain R.standsOn pattern tail

inductive FindCitation {P : Type*} (R : Repository P) (pattern : P)
    (TextCitation : Type*) where
  | patternText : TextCitation → FindCitation R pattern TextCitation
  | authoredEdges : FindWarrant R pattern → FindCitation R pattern TextCitation

structure Receipt {P : Type*} (R : Repository P) (pattern : P)
    (Clause AsOf TextCitation : Type*) where
  clauseKind : FindClauseKind
  acknowledgedClause : Clause
  route : FindRoute
  asOf : AsOf
  citation : FindCitation R pattern TextCitation

def Receipt.nonSelfCertifying {P : Type*} {R : Repository P} {pattern : P}
    {Clause AsOf TextCitation : Type*}
    (_receipt : Receipt R pattern Clause AsOf TextCitation) : Prop :=
  True

structure FindResult (P Clause AsOf TextCitation : Type*)
    (R : Repository P) where
  selected : Set P
  receipts : ∀ p, p ∈ selected → Receipt R p Clause AsOf TextCitation
  absence : Option TypedAbsence

opaque find {State P Clause AsOf TextCitation : Type*} :
  FindQuery State P →
  (R : Repository P) →
  FindResult P Clause AsOf TextCitation R

def findF1Containment {State P Clause AsOf TextCitation : Type*}
    (q : FindQuery State P) (R : Repository P) : Prop :=
  (find (Clause := Clause) (AsOf := AsOf) (TextCitation := TextCitation)
      q R).selected ⊆ R.patterns ∧
  ((find (Clause := Clause) (AsOf := AsOf) (TextCitation := TextCitation)
      q R).selected = ∅ →
    (find (Clause := Clause) (AsOf := AsOf) (TextCitation := TextCitation)
      q R).absence = some .noPatternAddressesThisTension)

def findF2Receipted {State P Clause AsOf TextCitation : Type*}
    (q : FindQuery State P) (R : Repository P) : Prop :=
  ∀ p, p ∈ (find (Clause := Clause) (AsOf := AsOf)
    (TextCitation := TextCitation) q R).selected →
    Nonempty (Receipt R p Clause AsOf TextCitation)

def findF3NonSelfCertifying {State P Clause AsOf TextCitation : Type*}
    (q : FindQuery State P) (R : Repository P) : Prop :=
  ∀ p, p ∈ (find (Clause := Clause) (AsOf := AsOf)
    (TextCitation := TextCitation) q R).selected →
    ∃ receipt : Receipt R p Clause AsOf TextCitation,
      receipt.nonSelfCertifying

/-- The authoritative F4 statement. It deliberately does not identify a
    zero-mass designation; that registered reading remains evidence-sensitive. -/
def findF4Falsifiable {State P Clause AsOf TextCitation : Type*}
    (q : FindQuery State P) (R : Repository P) : Prop :=
  ∃ p, p ∈ R.patterns ∧
    p ∉ (find (Clause := Clause) (AsOf := AsOf)
      (TextCitation := TextCitation) q R).selected
```

## Review boundary

This block is syntactically applicable only as a replacement, and its laws
are propositions to prove of the eventual implementation; it does not claim
those proofs here. `Receipt.nonSelfCertifying := True` records that the
carrier makes a score-only citation unconstructible, not that arbitrary
receipt data are truthful. The real implementation must still be measured:
the generic Clojure merge currently lets caller receipt fields overwrite
`:route` and `:warrant` (`futon3:checks/find_organise.clj:245-251`), whereas
the shipped snatch caller does not (`futon3:checks/find_snatch.clj:56-65`).
That mismatch is evidence for the implementation slice, not something this
signature draft silently erases.
