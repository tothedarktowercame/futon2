-- Minimal shim of the DarkTower signatures the CLean render touches.
-- Signature-identical to mathlib4/DarkTower/{BV,TypedHole,Discharge}.lean, but
-- with no `import Mathlib`, so this file compiles with core `lean`. The real
-- emit (--mode real) targets the Mathlib-backed modules instead.
universe u v
namespace DarkTower

inductive BV (A : Type u) where
  | unit | atom : A -> BV A | seq : BV A -> BV A -> BV A
  | copar : BV A -> BV A -> BV A | par : BV A -> BV A -> BV A
namespace BV
inductive Cong {A : Type u} : BV A -> BV A -> Prop where
  | seq_assoc (S T U : BV A) : Cong (seq (seq S T) U) (seq S (seq T U))
  | copar_comm (S T : BV A) : Cong (copar S T) (copar T S)
end BV

inductive SatietyGrade | parse | payoff | canon | bundling | role
  deriving DecidableEq, Repr
inductive DischargeKind | sorryProof | queryAnswer | ungroundedBinder
  deriving DecidableEq, Repr

structure PFunctor where
  A : Type u
  B : A -> Type v
structure TypedHole where
  poly : PFunctor
  satiety : poly.A -> SatietyGrade
namespace TypedHole
def holeType (T : TypedHole) (a : T.poly.A) : Type v := T.poly.B a
end TypedHole

end DarkTower

open DarkTower

namespace CLeanProof_m_evaluate_policies
-- m-evaluate-policies: Evaluation-layer honesty loop: typed scores, gated arena, bounded experiments, durable evidence, PAR close

/-- The iching/CT method tags appearing in this proof's spine. -/
inductive Method where
  | structuredObservationVector
  | candidatePatternActionSpace
  | taskShapeValidation
  | budgetBoundsExploration
  | sessionDurabilityCheck
  | parAsObligation
  deriving DecidableEq, Repr

/-- The pattern-tagged method spine (the informal reading), a BV.seq chain. -/
def spine : BV Method :=
  BV.seq (BV.atom Method.structuredObservationVector) (BV.seq (BV.atom Method.candidatePatternActionSpace) (BV.seq (BV.atom Method.taskShapeValidation) (BV.seq (BV.atom Method.budgetBoundsExploration) (BV.seq (BV.atom Method.sessionDurabilityCheck) (BV.atom Method.parAsObligation)))))

/-- The proof steps (the comb positions). -/
inductive Stp where
  | s1
  | s2
  | s3
  | s4
  | s5
  | s6
  deriving DecidableEq, Repr

/-- The direction a still-open typed hole exposes: an outstanding proof. -/
inductive Obligation where
  | proof
  deriving DecidableEq, Repr

/-- The typed-hole interface: each step is a position; a step exposes an
    `Obligation` iff it carries an open hole, else `Empty` (discharged). -/
def holes : TypedHole where
  poly :=
    { A := Stp
      B := fun
        | Stp.s1 => Empty
        | Stp.s2 => Empty
        | Stp.s3 => Obligation
        | Stp.s4 => Empty
        | Stp.s5 => Empty
        | Stp.s6 => Obligation
    }
  satiety := fun
    | Stp.s1 => SatietyGrade.canon
    | Stp.s2 => SatietyGrade.canon
    | Stp.s3 => SatietyGrade.parse
    | Stp.s4 => SatietyGrade.canon
    | Stp.s5 => SatietyGrade.canon
    | Stp.s6 => SatietyGrade.payoff

/-- The comb wiring (construct -> consume edges). -/
def wires : List (Stp × Stp) := [(Stp.s1, Stp.s2), (Stp.s2, Stp.s3), (Stp.s3, Stp.s4), (Stp.s4, Stp.s5), (Stp.s5, Stp.s6)]

/-- The two readings held together (M-typed-holes copar): informal ∥ formal. -/
inductive Reading where
  | informal | formal
  deriving DecidableEq, Repr
def readings : BV Reading :=
  BV.copar (BV.atom Reading.informal) (BV.atom Reading.formal)

-- Self-verifying checks: the render is type-correct and computes. 0 sorry.
example : holes.holeType Stp.s1 = Empty := rfl
example : holes.holeType Stp.s2 = Empty := rfl
example : holes.holeType Stp.s3 = Obligation := rfl
example : holes.holeType Stp.s4 = Empty := rfl
example : holes.holeType Stp.s5 = Empty := rfl
example : holes.holeType Stp.s6 = Obligation := rfl
example : holes.satiety Stp.s1 = SatietyGrade.canon := rfl
example : holes.satiety Stp.s2 = SatietyGrade.canon := rfl
example : holes.satiety Stp.s3 = SatietyGrade.parse := rfl
example : holes.satiety Stp.s4 = SatietyGrade.canon := rfl
example : holes.satiety Stp.s5 = SatietyGrade.canon := rfl
example : holes.satiety Stp.s6 = SatietyGrade.payoff := rfl
/-- Every open hole is a sorry/proof hole. -/
example : DischargeKind.sorryProof = DischargeKind.sorryProof := rfl
/-- The spine prefix reassociates by BV structural congruence (well-shaped comb). -/
example : BV.Cong
    (BV.seq (BV.seq (BV.atom Method.structuredObservationVector) (BV.atom Method.candidatePatternActionSpace)) (BV.atom Method.taskShapeValidation))
    (BV.seq (BV.atom Method.structuredObservationVector) (BV.seq (BV.atom Method.candidatePatternActionSpace) (BV.atom Method.taskShapeValidation))) :=
  BV.Cong.seq_assoc (BV.atom Method.structuredObservationVector) (BV.atom Method.candidatePatternActionSpace) (BV.atom Method.taskShapeValidation)
/-- The informal ∥ formal copar reading commutes (one object, two readings). -/
example : BV.Cong readings (BV.copar (BV.atom Reading.formal) (BV.atom Reading.informal)) :=
  BV.Cong.copar_comm (BV.atom Reading.informal) (BV.atom Reading.formal)

end CLeanProof_m_evaluate_policies
