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
