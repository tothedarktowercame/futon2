-- Proposed lifecycle only; no runtime correspondence or admission claim.
inductive RepairState where
  | open | awaitingValidation | resolved
  deriving DecidableEq

structure HistoricalVerification where
  exactFinding : Prop
  ancestry : Prop
  freshIndependentReview : Prop
  completeQualification : Prop
  newVerificationIdentity : Prop

def CanAwaitValidation (e : HistoricalVerification) : Prop :=
  e.exactFinding ∧ e.ancestry ∧ e.freshIndependentReview ∧
  e.completeQualification ∧ e.newVerificationIdentity

inductive Transition (e : HistoricalVerification)
    (distinctReviewedGroundedSuccessor : Prop) : RepairState → RepairState → Prop where
  | verify : CanAwaitValidation e →
      Transition e distinctReviewedGroundedSuccessor .open .awaitingValidation
  | validate : distinctReviewedGroundedSuccessor →
      Transition e distinctReviewedGroundedSuccessor .awaitingValidation .resolved

theorem historical_verification_does_not_resolve
    (e : HistoricalVerification) (successor : Prop) :
    ¬ Transition e successor .open .resolved := by
  intro h
  cases h

theorem resolution_requires_distinct_successor
    (e : HistoricalVerification) (successor : Prop) (s : RepairState)
    (h : Transition e successor s .resolved) :
    s = .awaitingValidation ∧ successor := by
  cases h with
  | validate proof => exact ⟨rfl, proof⟩

theorem verification_requires_complete_qualification
    (e : HistoricalVerification) (successor : Prop)
    (h : Transition e successor .open .awaitingValidation) :
    e.completeQualification := by
  cases h with
  | verify proof => exact proof.2.2.2.1
