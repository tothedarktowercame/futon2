import Mathlib.Data.List.Basic

/-!
# JIT: why the self-heal route cannot reach enactment (click `wm-click-c78d0f27`)

Just-in-time formalisation by claude-2, 2026-10-19, for the live incident in
which click `wm-click-c78d0f27` (closed 19:43:49Z) refused with
`:meta-self-heal/diagnosis-transaction-mismatch`. The diagnosis observation
and the ledger agree, so the reported reason is not the defect. claude-5's
account (PACKETS-2026-10-19, futon2 `ce46da523`) is that
`coordinator/prepare` refuses, and the refusal is then reported as a
mismatch. This file states that account as decidable facts over transcribed
tables, so a reviewer can check it field by field.

Transcribed from source, never from a run. Read at futon2 `ce46da523` and
futon3c `8c00d29b9`:

* `futon2 meta_repair_coordinator.clj` `verify-work-order`: the caller-supplied
  fields a work order needs. The `:identities`, `:sessions/distinct`,
  `:sessions/present`, `:diagnosis-evidence` and `:acceptance-test` defects.
  `prepare` itself computes `injury-id`, `repair-id`, `attempt`, the cause pin
  and the ledger pins.
* `futon2 meta_self_heal_production.clj` `work-order-options`: the keys `route!`
  passes on to `prepare`.
* What production actually puts in `route!`'s options:
  - `full_loop_runtime.clj` `run-opportunity!` adds `:diagnosis-evidence-pin`
    and an admitted `:acceptance-test`.
  - `futon3c runner_service.clj` resolves the click's cast, adding `:author`,
    `:reviewer` and `:repair-reviewer`. The repair reviewer defaults to the
    reviewer.
  - `configure-self-heal-ports` forwards only port functions.
  - No production code supplies the three sessions, `:target-repository` or
    `:target-worktree`. Only `meta_self_heal_harness.clj` does, so tests pass.

Two corrections to claude-5's prose account, found by transcribing it:
`:repair-reviewer` *is* supplied (it defaults to the reviewer), and
`:target-repository` is *not* (claude-5's JVM probe passed it by hand).
-/

namespace DarkTower.WarMachine.SelfHealPrepare

/-- Caller-supplied work-order fields named in `verify-work-order`. -/
inductive Field where
  | diagnosisEvidencePin | acceptanceTest
  | author | reviewer | repairReviewer
  | authorSession | reviewerSession | repairReviewerSession
  | targetRepository | targetWorktree
  deriving DecidableEq, Repr

open Field

/-- Every field `verify-work-order` requires to be present. Sessions must also
be distinct; that condition is not modelled here, because absence already
fails. -/
def required : List Field :=
  [ diagnosisEvidencePin, acceptanceTest, author, reviewer, repairReviewer,
    authorSession, reviewerSession, repairReviewerSession,
    targetRepository, targetWorktree ]

/-- The fields production supplies to `route!` on a click whose acceptance
test was admitted. -/
def productionSupplied : List Field :=
  [ diagnosisEvidencePin, acceptanceTest, author, reviewer, repairReviewer ]

/-- Required fields that are not supplied. -/
def missing (supplied : List Field) : List Field :=
  required.filter (fun f => !(supplied.contains f))

/-- `prepare` returns a transaction exactly when no required field is missing. -/
def prepareSucceeds (supplied : List Field) : Bool := (missing supplied).isEmpty

/-- **The hole.** In `SelfHealRoute`, the L1 edge `readinessReady → routeEnacted`
has a precondition the table does not state: production preparation
succeeds. With today's wiring that precondition is false, so no click can
reach enactment, whatever happens upstream. -/
theorem production_prepare_refuses : prepareSucceeds productionSupplied = false := by
  decide

/-- What is missing, by name. This is the repair's to-do list. -/
theorem production_missing :
    missing productionSupplied =
      [authorSession, reviewerSession, repairReviewerSession,
       targetRepository, targetWorktree] := by
  decide

/-- The repair the incident calls for: thread these five fields into the
work-order options. Once they are threaded, the same statement is decided
`true`. -/
def repairedSupplied : List Field :=
  productionSupplied ++
    [authorSession, reviewerSession, repairReviewerSession, targetRepository, targetWorktree]

theorem repaired_prepare_succeeds : prepareSucceeds repairedSupplied = true := by
  decide

/-! ## Why the record said "mismatch" -/

/-- The outcome of `coordinator/prepare` as `route!` receives it. -/
inductive PrepareOutcome where
  | prepared | refused
  deriving DecidableEq, Repr

/-- The verdicts `route!`'s binding step can report. -/
inductive BindingVerdict where
  | valid | diagnosisMismatch | prepareRefused
  deriving DecidableEq, Repr

/-- `route!` today: `(:transaction (coordinator/prepare …))` is nil on a
refusal, and `diagnosis-transaction-binding` compares an `:available`
diagnosis with that absent work order. A prepared transaction whose pins agree
is valid; this model assumes they agree, which claude-5 checked for this click. -/
def routeBindingToday : PrepareOutcome → (diagnosisAvailable : Bool) → BindingVerdict
  | .refused, true => .diagnosisMismatch
  | _, _ => .valid

/-- The reason the record should carry: the refusal of preparation itself. -/
def routeBindingHonest : PrepareOutcome → (diagnosisAvailable : Bool) → BindingVerdict
  | .refused, _ => .prepareRefused
  | _, _ => .valid

/-- This click, as recorded: `prepare` refused while the diagnosis was available,
and the record carries a mismatch. -/
theorem click_c78d0f27_masked : routeBindingToday .refused true = .diagnosisMismatch := by
  decide

/-- The masking is the defect: today's verdict differs from the honest one on
exactly this input. -/
theorem masking_is_the_defect :
    routeBindingToday .refused true ≠ routeBindingHonest .refused true := by
  decide

/-- Chaining the two facts: with production's fields, `prepare` refuses, so
any click that reaches it with an available diagnosis is recorded as a
mismatch. -/
theorem every_such_click_reports_mismatch :
    prepareSucceeds productionSupplied = false ∧
      routeBindingToday .refused true = .diagnosisMismatch := by
  decide

end DarkTower.WarMachine.SelfHealPrepare
