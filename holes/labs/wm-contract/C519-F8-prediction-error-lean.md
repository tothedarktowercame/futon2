# C519 — :F8 leg 1, slice 2: the prediction error ε, stated in Lean

**Row:** `:F8` (`:loop-mode :one-slice-per-invocation`), leg 1 (Lean
completion): "one Lean statement per class-(a) quantity … Slice discipline: one
quantity or one coherent cluster per slice" (`EPIC-run-era.md:788-793`;
`worklist.edn:1309`). Slice 0 was the U35 join refresh (C517); slice 1 was Π
(C518). **This slice's quantity is ε (`:prediction-error`, node R8)**, one of
the nine rows C518 §6 left with no declared Lean carrier.

**Commits.** mathlib4 `1282b75e32` (the two Lean modules) + this futon2 commit
(the registry declaration, the readback script and artifacts, this report, the
ledger).

## 1. What was in Lean before, and what was not

`Holes.predictionError` (`mathlib4/DarkTower/WarMachine/Holes.lean:6815-6817`)
is the registry's `:formal` line and nothing more — `ε_k := o_k − μ_k`, a total
function `ObservationVector → (Channel → ℝ) → Channel → ℝ`.
`PredictionErrorWitness` witnesses that it is the signed difference and equals
neither operand; `PredictionErrorNegative` refuses the reversed sign. So the
**spec** form was already in Lean and the map the implementation runs was not,
and `aif-equations.edn:79` recorded that as `:lean nil :lean-status :missing`.

## 2. What the implementation computes

`compute-prediction-error` (`futon2/src/futon2/aif/free_energy.clj:203-278`) is
**not a function into ℝ.** It returns one of three typed records and which one
it returns is the decision (AC1, Joe's 2026-09-02 ruling on C130 §2, quoted in
the docstring at `:207-212`). Its `cond` has three arms and their order is what
the module reproduces:

```
offending := [mean, variance] members that are missing or non-finite,
             then observed if it is present but not a finite number  (:243-249)
if offending nonempty      -> :refused, naming every offending member  (:253-256)
else if observed is nil    -> :absent, carrying the envelope's reason  (:259-263)
else                       -> :present with
      :error      = observed − predicted-mean                          (:269)
      :precision  = 1 / max(predicted-variance, min-variance)          (:270)
      :weighted-error = error × precision                              (:278)
```

`mathlib4/DarkTower/WarMachine/MachinePredictionError.lean` (`1282b75e32`, 338
lines; the witness is 172) declares `Field` — the producer's input space, since
ℝ has no NaN and "missing" and "malformed" have to be constructors —
`Prediction`, `Offence`, `PresentRecord`, `Outcome`, `presentRecord` and
`machineChannelPredictionError`, and then the vector level: `channelOutcomes`
and `scoredErrors`.

## 3. Three reductions the `:formal` line does not carry

**(a) The producer is three-valued, and refusal dominates absence.**
`refusalDominatesAbsence`: a channel this tick did not observe, whose
likelihood *also* failed to produce a mean, is **refused, not omitted** — the
first `cond` arm is reached before the `(nil? observed)` arm, and the missing
observation is not even reported. So a tick's omission count is not its count
of unobserved channels. `absence_requires_wellFormedModel` is the converse: the
producer omits a channel only when both model members are finite numbers and
the observation alone is missing.

**(b) The subtracted term is not μ.** It is `:predicted-mean`, one member of a
likelihood output that `belief/predict-observation` (`belief.clj:1199-1238`)
computes from the belief state. The implementation's belief is an
**entity-indexed categorical map**, not the per-channel `Channel → ℝ` mean that
this `:formal` line and `Holes.BeliefState` both assume; the per-channel mean is
a *readout* of it (`belief.clj:640-737`, e.g. `predict-annotation-health`'s mean
is the average per-entity expected health across tracked entities).
`registryFormOfPresent` states exactly where the registry line holds: on a
channel whose predicted mean **is** the belief mean, where the record's `:error`
is literally `Holes.predictionError`. Whether that hypothesis holds of the
running stack is a question about `predict-observation`, and **no claim is made
about it here**.

**(c) A refusal of one channel refuses the whole update.** At the vector level
(`war_machine.clj:6110-6127`) an absent channel is omitted and the rest are
scored, but a single refused channel empties the error map for **every**
channel — `oneRefusalEmptiesTheUpdate`. `scoredErrors_present` is the other
half: what survives is present and nothing else, so an absent channel is
omitted rather than entered at zero. And ε is attempted on only the **eight**
channels with a likelihood model (`belief.clj:933-934`); the other six produce
no record at all, not an absent one (`channelsWithoutLikelihood_eq`, checked
against the Clojure set in §5).

## 4. The finding: a negative predicted variance is scored, not refused

Proved, not asserted.

`prediction-member` (`free_energy.clj:191-201`) classifies a model member as
missing, not-finite, or present — it checks **finiteness and not sign**, so
`-4.0` is present and reaches the scored arm exactly as `1/4` does
(`negativeVarianceIsNotRefused`, which needs no hypothesis on the variance).
The `max` at `:270` then floors it to `min-variance`, so a variance that cannot
exist yields the **largest per-call precision the floor allows** — `100` under
the declared default of `0.01` (`subMinVarianceIsFloored`).

`zeroAndNegativeVarianceAgree` is the point: the record built on variance `0`
and the record built on variance `-4` have the **same** `perCallPrecision` and
the **same** `weightedError`, and differ only in the `:predicted-variance`
nothing downstream consults. A negative variance is therefore not merely
accepted; after the floor it is invisible.

**Not a ruling.** Whether the producer should refuse a negative variance is not
decided here and nothing is written to `:choices`.

## 5. The reference values, exact against production

`MachinePredictionErrorWitness.lean` proves eight scored values and five typed
outcomes; `f8_prediction_error_readback.clj` reads the same cases out of
`futon2.aif.free-energy` itself.

| Lean theorem | o | mean | var | Lean ε / Π / w | Clojure | delta |
|---|---|---|---|---|---|---|
| `basicTriple` | 3/4 | 1/4 | 1/4 | 1/2, 4, 2 | 0.5, 4.0, 2.0 | 0.0 |
| `unitVariance` | 3/4 | 1/4 | 1 | 1/2, 1, 1/2 | 0.5, 1.0, 0.5 | 0.0 |
| `emptyBeliefReadsTheObservation` | 3/4 | 0 | 1 | 3/4, 1, 3/4 | 0.75, 1.0, 0.75 | 0.0 |
| `negativeError` | 1/4 | 3/4 | 1/4 | −1/2, 4, −2 | −0.5, 4.0, −2.0 | 0.0 |
| `exactZeroError` | 1/2 | 1/2 | 1/4 | 0, 4, 0 | 0.0, 4.0, 0.0 | 0.0 |
| `eighthVariance` | 3/4 | 1/4 | 1/8 | 1/2, 8, 4 | 0.5, 8.0, 4.0 | 0.0 |
| `zeroVarianceFloored` | 3/4 | 1/4 | 0 | 1/2, 100, 50 | 0.5, 100.0, 50.0 | 0.0 |
| `negativeVarianceFloored` | 3/4 | 1/4 | −4 | 1/2, 100, 50 | 0.5, 100.0, 50.0 | 0.0 |

| Lean theorem | production status | offending |
|---|---|---|
| `unobservedIsOmitted` | `:absent` | — |
| `brokenModelRefuses` | `:refused` | `[[:mean :missing]]` |
| `unobservedWithBrokenModelRefuses` | `:refused` | `[[:mean :missing]]` |
| `malformedObservationRefuses` | `:refused` | `[[:observed :not-finite]]` |
| `bothModelMembersNamed` | `:refused` | `[[:mean :missing] [:variance :not-finite]]` |

`emptyBeliefReadsTheObservation` is worth reading twice: `belief.clj:651-652`
returns `{:mean 0.0 :variance 1.0}` for a belief with no entities, so on the
first tick of a fresh belief the machine's prediction error on a likelihood
channel **is the raw observation** and the per-call precision is 1.

Artifact: `runs/F8-prediction-error/clojure-readback.txt`, byte-identical on a
second run (sha256 `8f22cae8…`).

**Exactness, with its one exception stated.** Every observation, mean and
variance above is dyadic, so the Clojure double and the Lean rational are the
same number — **except** in the two floored cases, whose arithmetic goes
through `min-variance` `0.01`, which no double represents exactly (the double
is `0.01000000000000000020816681711721685132943093776702880859375`). There the
Lean `100` and the Clojure `100.0` agree because `1.0 / 0.01` rounds back to
exactly `100.0`, not because the operands are representable. That is a rounding
fact, and the artifact prints the expansion so a reader can see it rather than
take the word "exact" on trust.

## 6. The second finding: two precisions, one key

The `:precision` this producer stamps is `1 / max(predicted-variance,
min-variance)` — derived from the **likelihood's** variance for this one call.
R7's Π (`MachinePrecision.machinePrecision`, slice 1) is derived from the
channel's **error history**. They are different numbers and they occupy the
same `:precision` key at different points in one tick:
`precision/weighted-error` (`precision.clj:213-233`) overwrites the first with
the second and preserves it as `:per-call-precision`.

Measured on one channel in one tick (readback §3, and
`perCallPrecisionIsNotMachinePrecision`): the producer stamps `:precision` 4.0
and `:weighted-error` 2.0; after the overwrite the same two keys read **1.6 and
0.8**, and 1.6 is `8/5` — `MachinePrecisionWitness.halfError`, slice 1's
theorem for the history `[1/2]`. The number the belief update consumes is not
the number the producer emitted.

The Lean module therefore names its field `perCallPrecision`. Spelling it
`precision` would reproduce in Lean exactly the bare-symbol collision leg 2's
concordance checker is meant to refuse. **Not a ruling**: nothing is decided
here about whether the two should share a key.

## 7. The registry movement

`aif-equations.edn:79` `:prediction-error`: `:lean nil :lean-status :missing` →
`:lean "machineChannelPredictionError" :lean-status :closed`, with a
`:lean-note` carrying §3–§6 and a `:lean-at` naming mathlib4 `1282b75e32`.

This writes `:lean`, `:lean-status`, `:lean-note`, `:lean-at` and **no other
field** — checked by reading the row out of `git show HEAD:` and out of the
working tree and diffing key by key: two keys added, none removed, two changed,
every other equation byte-identical and every non-`:equations` top-level key
byte-identical. TN §9a's second-reader gate binds `:code`, `:realised` and
`:imports`; none moved, and neither did `:choices` or `control-map-edges.edn`
`:decisions`.

**No signature bookkeeping was owed and that was checked, not assumed.** No
`:done` row in the ledger carries a `:covers-key` naming
`[:equations {:id :prediction-error}]` (scanned over every row's `:covers-key`),
so unlike slice 1 — which had to supersede C10 — this slice mints no `C` row.

Verified rather than assumed: `bb lean_state_probe.bb --no-typecheck --out
runs/F8-prediction-error/lean-state-join-check.edn` resolves
`machineChannelPredictionError` to
`DarkTower/WarMachine/MachinePredictionError.lean:160` (kind `def`), and the
equation join moves **9 of 18 carriers declared / 9 resolving → 10 of 18
declared / 10 resolving**, `:carrier-declared-but-not-found []`.
`:prediction-error` leaves `:no-carrier-declared`, which is now the eight
remaining rows: `:observe`, `:policy-free-energy`, `:belief-update`,
`:belief-state`, `:policy-set`, `:depth`, `:temperature`, `:action`.

## 8. Found and not repaired

**(a) This row's `:code` pointer does not go where it says.** It reads
`war_machine.clj:4368-4379`, which at futon2 HEAD is inside `scan-blocks`
(`:4363`), the Block-footer commit scanner across the 14-repo manifest —
unrelated to ε in every respect. The producer is `free_energy.clj:203-278` and
the tick's call site is `war_machine.clj:6110-6127`. This is worse than slice
1's case, where the pointer merely under-reached the function it named: this one
resolves onto code of a different subject, so it reads plausibly and supports
nothing. **Not repaired**, because `:code` is precisely the field TN §9a's
second-reader gate binds; it is named in the `:lean-note` so a registry reader
meets it, and it wants a second reader's sign-off rather than a quiet edit.

**(b) The U35 receipt and the published section are two rows behind.**
`runs/U35-lean-state/lean-state-report.edn` and
`p4ng/sec-lean-state-generated.tex` still say 8 of 18 and still list both
`:precision` and `:prediction-error` among the rows rendered "not in Lean"; the
corpus counts there (71 modules / 725 declarations / 11762 lines) are now
**75 / 809 / 12667**. Not refreshed here, for slice 1's reason: regeneration
into a publish happens after review (TN §9a), so the check above was written to
a slice-local path and nothing published moved.

**(c) C517 §2's `:forward-model` finding is still untouched.**
`aif-equations.edn:114` still declares `PredictiveOutcomeKernel :carrier-only`
while `MachineQ.lean:187` declares `machinePredictiveOutcomeKernel`, named by no
row. A different quantity; it stays where C517 left it.

## 9. Gates

| gate | result |
|---|---|
| `lake env lean DarkTower/WarMachine/MachinePredictionError.lean` | exit 0, **0 errors, 0 warnings** |
| `lake env lean DarkTower/WarMachine/MachinePredictionErrorWitness.lean` | exit 0, **0 errors, 0 warnings** |
| `lake build DarkTower.WarMachine.MachinePredictionErrorWitness` | 2708/2708 built, success |
| `#print axioms` on all **50** new declarations | 43 give `[propext, Classical.choice, Quot.sound]`, 7 depend on no axioms; **no `sorryAx`**, no other output |
| `bb lean_state_probe.bb --no-typecheck` | 75 modules, 809 declarations, 12667 lines; **sorry 10 source, axioms 0** (unchanged — these modules add neither) |
| `clj-kondo --lint f8_prediction_error_readback.clj` | see the ledger row |
| `futon4/dev/check-parens.sh` | see the ledger row |
| `bash p4ng/empirics-futon/negative_controls.sh` | see the ledger row |
| `bb p4ng/empirics-futon/pointer_check.bb` | see the ledger row |
| `bb worklist_check.bb` | see the ledger row |

Every `free_energy.clj`, `belief.clj`, `precision.clj` and `war_machine.clj`
line pointer in the Lean docstrings and in this report was read out of the file
at HEAD rather than copied from the registry — which is how §8(a) was found.

**Not claimed.** No live tick, no run lock taken, nothing written under
`data/`. No `src/` change in futon2, so no test run is claimed; `clj-kondo` is
run because a `.clj` script was added. `gen_aif_dag.bb` was not run and nothing
was regenerated into a publish. No ruling: `:choices` and `:decisions`
byte-unchanged. Leg 2 (name discipline) and leg 3 (convergence ledger) remain
untouched; the eight remaining class-(a) quantities are the next leg-1 slices.
