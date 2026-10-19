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

/-!
## Revision 2 (claude-2, 2026-10-19): after the fix

The fix is futon2 `df2209a33` (`route!` reports a refused prepare as
`:meta-self-heal/preparation-refused`) together with futon3c `ddb3fcb79`.
In futon3c, `meta_self_heal_adapters.clj` `work-order-identities` derives the
identities from a ready readiness receipt, and `runner_service.clj`
`fill-absent` / `configure-self-heal-ports` forwards them. The pre-fix tables
are kept as the historical state of click `wm-click-c78d0f27`, with names
suffixed `_c78d0f27`. The file reads before and after over one incident.

Nuances, transcribed from the fixed source:
* **Sessions are claim identities.** They take the form `<seat>/<UUID>`, minted
  per click. They are not Agency session ids. `verify-work-order` needs them
  non-blank and pairwise distinct. The ledger needs them unused by any earlier
  claim, which a fresh UUID satisfies.
* **`:repair-reviewer` changed.** `fill-absent` runs inside
  `configured-runner-opts`, before cast resolution, so it now takes the
  adapter's default `"wm-repair-reviewer"`. Before the fix, cast resolution set
  it to the reviewer. Repair review is therefore now done by a separate seat.
* **`:target-worktree` is the repository path**, the canonical futon2 checkout,
  not a separate worktree. No production code reads `:target-worktree`;
  `verify-work-order` only requires it to be non-blank. `enact!` reads
  `:target-repository`. The field is filled to pass verification, not because
  anything consumes it.
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

/-- Before the fix (click `wm-click-c78d0f27`): the fields production supplied
to `route!` on a click whose acceptance test was admitted. -/
def productionSupplied_c78d0f27 : List Field :=
  [ diagnosisEvidencePin, acceptanceTest, author, reviewer, repairReviewer ]

/-- Required fields that are not supplied. -/
def missing (supplied : List Field) : List Field :=
  required.filter (fun f => !(supplied.contains f))

/-- `prepare` returns a transaction exactly when no required field is missing. -/
def prepareSucceeds (supplied : List Field) : Bool := (missing supplied).isEmpty

/-- **The hole.** In `SelfHealRoute`, the L1 edge `readinessReady → routeEnacted`
has a precondition the table does not state: production preparation
succeeds. With the wiring before the fix that precondition was false, so no click could
reach enactment, whatever happened upstream. -/
theorem production_prepare_refuses_c78d0f27 :
    prepareSucceeds productionSupplied_c78d0f27 = false := by
  decide

/-- What is missing, by name. This is the repair's to-do list. -/
theorem production_missing_c78d0f27 :
    missing productionSupplied_c78d0f27 =
      [authorSession, reviewerSession, repairReviewerSession,
       targetRepository, targetWorktree] := by
  decide

/-- The repair the incident calls for: thread these five fields into the
work-order options. Once they are threaded, the same statement is decided
`true`. -/
def repairedSupplied : List Field :=
  productionSupplied_c78d0f27 ++
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
  | valid | diagnosisMismatch | acceptanceMismatch | prepareRefused
  deriving DecidableEq, Repr

/-- `route!` before the fix. On a refusal, `(:transaction (coordinator/prepare …))`
is nil.
* With a diagnosis `:available`, `diagnosis-transaction-binding` compares it
  with that absent work order and reports a mismatch.
* With a diagnosis not newly produced, that check passes, and
  `acceptance-transaction-binding` compares an `:accepted` acceptance
  observation with the absent work order. It reports
  `acceptance-transaction-mismatch`, a second masked path.

This model assumes acceptance was admitted by review (`:accepted`). With a
supplied (`:present`) test, the code would instead go on to coordinate a nil
transaction, which is not modelled here. Revision 1 (`f84a0cc96`) gave
`.valid` for the diagnosis-not-available case. That was wrong, and it is
corrected here. A prepared transaction whose pins agree is valid; this model
assumes they agree, which claude-5 checked for this click. -/
def routeBinding_c78d0f27 : PrepareOutcome → (diagnosisAvailable : Bool) → BindingVerdict
  | .refused, true => .diagnosisMismatch
  | .refused, false => .acceptanceMismatch
  | .prepared, _ => .valid

/-- The reason the record should carry: the refusal of preparation itself. -/
def routeBindingHonest : PrepareOutcome → (diagnosisAvailable : Bool) → BindingVerdict
  | .refused, _ => .prepareRefused
  | _, _ => .valid

/-- This click, as recorded: `prepare` refused while the diagnosis was available,
and the record carries a mismatch. -/
theorem click_c78d0f27_masked : routeBinding_c78d0f27 .refused true = .diagnosisMismatch := by
  decide

/-- The masking is the defect: the pre-fix verdict differs from the honest one on
exactly this input. -/
theorem masking_is_the_defect :
    routeBinding_c78d0f27 .refused true ≠ routeBindingHonest .refused true := by
  decide

/-- Chaining the two facts: with production's fields, `prepare` refuses, so
any click that reaches it with an available diagnosis is recorded as a
mismatch. -/
theorem every_such_click_reports_mismatch :
    prepareSucceeds productionSupplied_c78d0f27 = false ∧
      routeBinding_c78d0f27 .refused true = .diagnosisMismatch := by
  decide

/-! ## After the fix (futon2 `df2209a33`, futon3c `ddb3fcb79`) -/

/-- The fields production now supplies to `route!`. `run-opportunity!` gives the
diagnosis pin and the admitted acceptance test. `work-order-identities`, through
`fill-absent`, gives the three cast seats, the three claim sessions, and the
repository as both target repository and target worktree. -/
def productionSupplied_fixed : List Field :=
  [ diagnosisEvidencePin, acceptanceTest, author, reviewer, repairReviewer,
    authorSession, reviewerSession, repairReviewerSession,
    targetRepository, targetWorktree ]

/-- (1) With the fields production now supplies, preparation succeeds. -/
theorem fixed_prepare_succeeds : prepareSucceeds productionSupplied_fixed = true := by
  decide

/-- The landed fix is exactly the repair revision 1 proposed. -/
theorem fix_is_the_proposed_repair : productionSupplied_fixed = repairedSupplied := by
  decide

/-- `route!` after `df2209a33`. When there is no transaction, the binding step is
`(refusal :meta-self-heal/preparation-refused {:prepare …})`, so neither binding
check runs against an absent work order. -/
def routeBindingFixed : PrepareOutcome → (diagnosisAvailable : Bool) → BindingVerdict
  | .refused, _ => .prepareRefused
  | .prepared, _ => .valid

/-- (2) `route!` now computes the honest verdict on every input. -/
theorem fixed_is_honest (o : PrepareOutcome) (d : Bool) :
    routeBindingFixed o d = routeBindingHonest o d := by
  cases o <;> cases d <;> rfl

/-- Click `wm-click-c78d0f27`'s input, replayed against the fixed code, reports
its real reason. Neither mismatch path remains. -/
theorem click_c78d0f27_now_reported :
    routeBindingFixed .refused true = .prepareRefused ∧
      routeBindingFixed .refused false = .prepareRefused := by
  decide

end DarkTower.WarMachine.SelfHealPrepare
