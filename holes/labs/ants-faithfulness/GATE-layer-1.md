# Reviewer's gate — layer 1, the render (codex-8, futon6 `4d2002e` / mathlib4 `2996312`)

Verified by re-running, not accepted on the summary.

| check | result |
|---|---|
| generated Lean builds | `lake env lean` exit 0 |
| sorries | **0** |
| declarations | 19 theorems/defs |
| regeneration determinism | byte-identical to the committed artifact |
| proof-mode regression | regenerated SHA `db2bf812…` — matches codex-8's before/after exactly |

## The negative test — the gate that matters

```
refused tests/fixtures/slice5-nonnavigable-treatment.clean.edn:
  non-navigable treatment refused: arm :no-directed-eig ablates
  :directed-eig without :role :positive-control
EXIT=1
```

Reproduced by me. **A render that has never refused anything is not a gate**;
this one refuses, with a message naming the arm and the axis.

## The laundering test — my own, not theirs

I warned in the brief that "a render that lets anything through by tagging it
positive-control is worthless." So I took the refused fixture and simply added
`:role :positive-control` to the offending arm:

```
refused /tmp/launder/laundered.clean.edn:
  positive-control arm and axis must be declared together
```

The tag alone does not launder it — the **axis** must declare the role too, and
codex-8 reports a sole-positive-control constraint on top. The gate is not a
rubber stamp.

## The limitation codex-8 reported rather than hid

`Registration.obligations` generates an `axisNavigable` obligation for *every*
axis of every arm — including the positive control's, whose non-navigability is
the prediction. The generated Lean therefore proves both that the obligation is
present **and that its content is false**.

Consequence: the existing types can *store* the ProspectiveRegistration but
**cannot construct `ProspectiveReadyToRun` for it**. codex-8 declined to fix this
by silently omitting the axis, which would have been dishonest, and instead named
the parent-structure change required: an **arm role**, and a **distinct
positive-control obligation requiring predicted non-navigability**.

This is the same shape as `original_obligation_undischargeable` in the
hand-written predecessor — an obligation that cannot be discharged — except here
it bites the *legitimate* case. The formalism cannot yet express "this axis is
deliberately inert and that is the hypothesis." That is a real contribution back
to DarkTower and it is the next parent change.

## Separate finding: the committed CLeanProofs.lean is stale

Regenerating proof mode produces 26 proofs and a file that differs from the
committed `DarkTower/CLeanProofs.lean`. The regenerated SHA matches codex-8's
before/after value exactly, so **this predates the extension** and is not a
regression from it — but the committed artifact has drifted from its sources and
should be regenerated separately.

## Verdict

**All three layers pass.** For the first time, the CLean vocabulary is enforced
rather than declared: the render refuses a non-navigable treatment, the malli
boundary refuses an unregistered arm, and the checker catches a one-part-in-2.8-billion
perturbation. The confirmation run is dispatchable.

Pilot artifacts stay labelled pilot.

---

## Parent change verified (codex-8, mathlib4 `084930e` / futon6 `9a25e8c`)

The limitation reported above is closed. Verified by attack, not by report.

**The guard is real.** `Discharged` now reads:

```lean
| Obligation.axisNavigable a              => a.Navigable
| Obligation.axisPredictedNonNavigable a => ¬ a.Navigable
```

A *proof obligation*, symmetric in difficulty with the treatment case — not a
suppression. `Registration.obligations` routes by `ArmRole`: a positive control's
axes generate the new obligation **instead of** `axisNavigable`, which is what
made the old arrangement unsatisfiable.

**The laundering attack, at the type level.** I declared a genuinely navigable
axis (`score := fun x => x`) and tried to pass it off as a positive control:

- `liveAxis.Navigable` — proves by `norm_num`. It is a real treatment.
- `¬ liveAxis.Navigable` — requires `sorry`. **Unprovable.**

So tagging a live treatment as a control swaps one proof burden for another and
the wrong one cannot be discharged. This is stronger than the renderer's dynamic
refusal: the *type* prevents it, so a hand-written registration bypassing the
generator is caught too.

| check | result |
|---|---|
| `ExperimentPreregistration` / `ExperimentalDesign` | exit 0, 0 sorries, 0 errors |
| `CLeanSlice5Confirmation` | exit 0, 0 sorries — and constructs `prospectiveReadyToRun` at :512 |
| `Slice5Preregistration` (hand-written predecessor) | exit 0, 0 sorries — comparison point survives |
| refusal: non-navigable treatment | exit 1 |
| refusal: arm tagged control without the axis | exit 1 |
| proof-mode SHA | `db2bf812…` unchanged |

`mem_obligations_axisNavigable` now requires proof that the arm is *not* a
positive control, and all call sites were updated — the signature change is
honest rather than papered over.

**Registration #1 is now ProspectiveReadyToRun.** The confirmation run is
dispatchable as a fully Lean-verified Clojure experiment, and registration #3
(claude-7's E2 confirmation, whose incidental-ablation arm is a predicted-null
contrast) is unblocked ahead of need.
