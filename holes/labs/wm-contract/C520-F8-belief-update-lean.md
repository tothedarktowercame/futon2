# C520 — :F8 leg 1, slice 3: the belief update μ-next, stated in Lean

**Row:** `:F8` (`:loop-mode :one-slice-per-invocation`), leg 1 (Lean
completion): one Lean statement per class-(a) quantity. Slice 0 was the U35
join refresh (C517), slice 1 was Π (C518), slice 2 was ε (C519). **This slice's
quantity is μ-next (`:belief-update`, node R3)**, one of the eight rows slice 2
left with no declared Lean carrier.

**Two seats wrote this slice.** The Lean modules, the readback script and its
artifacts were produced by codex-9 under Agency job
`invoke-1788615738834-7918-7d1b19aa` (mathlib4 `d15325c004`, futon2
`69154b1d`); its packet deliberately excluded the registry, the ledger and this
report, which are this commit. The carrier name, the registry declaration, the
re-run of the join, the independent checks in §6 and every finding below §2 are
this seat's, and §6 says what was re-run rather than accepted.

**Commits.** mathlib4 `d15325c004` (the two modules) + mathlib4 `0e89cc1cb5`
(the carrier name, this seat) + futon2 `69154b1d` (the readback) + this futon2
commit (the registry declaration, this report, the ledger).

## 1. What was in Lean before, and what was not

Nothing. `aif-equations.edn:108-112` recorded `:lean nil :lean-status
:missing`, and unlike ε — whose `:formal` line was already in Lean as
`Holes.predictionError` — there was no spec-form declaration to carry the row
either. The registry's `:formal` line is `μ ← μ + α Π ε`, buckley2017 eq. 59
under the stated reduction "one gradient step with fixed step size α".

## 2. The registry line is not the map the machine runs

There is no real-valued μ that a step `α Π ε` is added to. The implementation's
belief is a **per-entity categorical posterior over the seven-status set**
(`belief.clj:37-42`), and this tick's evidence reaches it as the **tempering
exponent** of a Bayesian filter step:

    q⁻ = B q ;  q ∝ A(o|·)^κ(w) · q⁻ ;  κ(w) = log₂(1 + w)

`belief.clj:297-346` (`predict-step`, `update-step`,
`categorical-filter-step`), reached from production through
`apply-arena-belief-events` (`war_machine.clj:821-827`), whose likelihood mode
is `:aif` unless the `FUTON_WM_LIKELIHOOD_MODE` env hatch selects `:legacy` or
`:a-matrix` (`war_machine.clj:803-812`). The Lean states the `:aif` path, which
is the production default; the other two modes are named and not stated.

`registryAdditiveFormIsNotGeneral` is the counterexample rather than the
assertion: at κ = 0 the Bayesian map leaves the prior fixed while a nonzero
`α Π ε` moves it, exhibited on two states.

## 3. What `α Π ε` becomes on the way in

Three stages, all stated in `MachineBeliefUpdate.lean`:

1. **Aggregation** (`belief.clj:1122-1197`). The per-channel errors become ONE
   signed driver, the precision-weighted average
   `Σ(sign_c × precision_c × error_c) / Σ(precision_c)` over contributing
   channels, with the health signs at `belief.clj:1003-1021`. The producer
   returns a **typed record with two statuses and never a third**: `:present`
   carries a driver, `:unknown` carries a reason and no number, and there is
   deliberately no `:refused` (AC2, Joe's 2026-09-02 ruling on C130 §2).
   `unknownDriverAppliesNoEvent` is that shape in Lean: an absent driver yields
   event weight 0, so the tick applies no event rather than a fabricated zero.
2. **Anneal and clamp** (`war_machine.clj:6157-6159`).
   `eventWeight = min(1,|driver|) × max(0, 1 − step/3) × 0.1`.
3. **Per-entity attribution** (`war_machine.clj:6172-6187`). The event type is
   `:strengthened` if the driver is positive and `:foreclosed` otherwise; each
   entity's inconsistency with that direction is `1 − h_e` or `h_e` for its
   expected health `h_e` (`belief.clj:625-638`); the weights are normalised so
   their **mean** is the event weight, so the aggregate magnitude is preserved
   while entities move unequally.

## 4. Three properties of that path, proved

**(a) The magnitude saturates.** `base-weight` is `min(1, |driver|)`
(`war_machine.clj:6158`), so a driver of 1 and a driver of 50 produce the same
event weight (`driverMagnitudeSaturates`, and `saturatedFifty` in the witness).
Above 1, the size of the surprise is discarded and only its sign survives into
belief.

**(b) The multichannel flag changes the driver's units.** Scaling every
channel's precision coherently leaves the multichannel driver unchanged,
because the precisions divide out (`precisionDividesOutInMultichannel`). In the
`:annotation-health-only` path the driver **is** that channel's weighted error,
so the same scaling multiplies it (`precisionScalesSingleChannel`). The
readback measures both against production: driver ¼ either way in multichannel,
1 against 4 in single-channel. So under `*r3d-multichannel?*` false the same
precision state drives belief harder — the flag is not only a channel selector.

**(c) The anneal floor is unreachable.** `annealFactor 3 3 = 0`
(`annealTerminates`), but the inner loop exits at `(inc step) >= r3-max-steps`
with `r3-max-steps = 3` (`war_machine.clj:6060, 6224`), so production only ever
sees steps 0, 1, 2 (`productionStepsDoNotReachAnnealTermination`). The zero is
in the formula and not on any path the machine takes — the same shape as slice
1's two dead guards, found by a different route.

**And the update is not sign-symmetric**, measured rather than assumed: a
`:strengthened` and a `:foreclosed` event of the same weight on a uniform prior
move expected health by +0.081169374433631700 and −0.063251314849317670, summing
to +0.017918059584314028 (`updateIsNotSignSymmetric`). The asymmetry is in the
observation model's columns, not in the filter: A's `:strengthened` column puts
0.2597 on `:strengthened` and 0.0909 on `:falsified`, while its `:foreclosed`
column puts 0.2597 on `:foreclosed` and 0.1299 on both `:strengthened` and
`:falsified`.

## 5. The reference values, against production

`f8_belief_update_readback.clj` loads the real `futon2.aif.belief` and calls
`r3d-aggregate-driver`, `update-entity-belief` (mode `:aif`) and
`entity-expected-health`; the event-weight and attribution arithmetic lives
inside the private War Machine tick, so the script reproduces
`war_machine.clj:6157-6187` expression by expression and says so rather than
pretending those let-bindings are callable. Artifact
`runs/F8-belief-update/clojure-readback.txt`, **byte-identical on a second run**
(§6).

Delta 0.0 on every scalar and structural case: the two drivers and their scaled
twins (¼, ¼, 1, 4), the three reachable anneal steps (1/10, 1/15, 1/30 — the
last two carrying the 1.4e-17 and 6.9e-18 the decimal expansion of a third
forces), the saturated driver, the unreachable step-3 zero, and the `:unknown`
record carrying `:reason :no-channel-supplied` with no `:driver` key at all. The
two categorical posteriors are stated as exact rationals in the witness
(`strengthenedPosterior`, `foreclosedPosterior` over 2193329 and 21198491) and
match production to 1e-12.

## 6. What this seat re-ran rather than accepted

- **The readback reproduces.** Re-ran `f8_belief_update_readback.clj` against
  the committed artifact: identical, byte for byte.
- **The posteriors match componentwise, not only in projection.** The committed
  readback compares the two posteriors through `entity-expected-health`, which
  is a scalar projection of a 7-vector — several posteriors give the same
  health. Compared all seven components of both production posteriors against
  the witness rationals: every component agrees within 1e-12, and the witness's
  likelihood columns are exactly `observation-model-v1`'s columns
  (20/77 = 0.2597…, 1/8, 13/86, 10/83, 10/77, 7/77).
- **The elaboration and the axioms.** `lake env lean` on both modules: exit 0,
  no diagnostics, 0 warnings. `#print axioms` on **all 49 declarations**:
  `[propext, Classical.choice, Quot.sound]` on 47, `[propext, Quot.sound]` on
  one, no axioms at all on one; **no `sorryAx`** and no other output.
- **The production likelihood mode.** `update-entity-belief`'s default is
  `:legacy`, and the readback passes `:aif` explicitly — so whether the Lean
  states the map the machine runs turns on what production passes.
  `arena-likelihood-mode` (`war_machine.clj:803-812`) returns `:aif` unless the
  env hatch is set, and `apply-arena-belief-events` is the only route from the
  R3 tick into the filter. Checked, not assumed.
- **The unreachable anneal step.** `r3-max-steps` is 3 and the loop's exit test
  is `(>= (inc step) r3-max-steps)` — read at `war_machine.clj:6060` and
  `war_machine.clj:6224` rather than taken from the packet's summary.
- **No signature bookkeeping was owed, and that was checked.** Parsed every row
  of `worklist.edn` and looked for a `:covers-key` naming
  `[:equations {:id :belief-update}]` anywhere in its tree: none (the only
  equation-covering rows name `:ambiguity`, `:free-energy` and `:precision`
  twice). Unlike slice 1, which had to supersede C10, this slice mints no C row.

## 7. The carrier name, and why this seat added one

The delivered modules stated the map but left **no single identifier the
registry row could declare**. Their top-level names are `categoricalUpdate`,
`kappa`, `normalise`, `eventWeight`, `inconsistency` — all bare symbols.
`lean_state_probe.bb:185-191` builds its corpus index as *name → first defining
site* and keeps the first, so declaring `categoricalUpdate` would leave this
row's carrier one same-named declaration away from silently resolving into
another module. That is leg 2's subject arriving in leg 1 for the second slice
running (slice 2 met it as the two precisions sharing one key).

So mathlib4 `0e89cc1cb5` adds `machineBeliefUpdate` — the composite the row
means, the entity's attributed weight entering the filter as the tempering
exponent — and `machineBeliefUpdateZeroInconsistencyIsNoOp`, which joins the two
halves already proved: a tick in which every entity is already consistent with
the error direction has zero total inconsistency, hence κ = 0, hence the
posterior is the prior (`zeroTotalInconsistencyMovesNothing` +
`kappaZeroIsNoOp`). No existing declaration was renamed or edited.

## 8. The registry movement

`aif-equations.edn:108-112`, **four fields and no others**: `:lean nil →
"machineBeliefUpdate"`, `:lean-status :missing → :closed`, plus new
`:lean-note` and `:lean-at "mathlib4 0e89cc1cb5"`. Verified by reading the row
out of `git show HEAD:` and out of the working tree and diffing key by key: two
keys added, none removed, two changed, **every other equation byte-identical
and every non-`:equations` top-level key byte-identical**. `:imports`,
`:formal`, `:eq` and `:code` did not move, so nothing TN 9a's second-reader gate
binds was touched.

`bb lean_state_probe.bb --no-typecheck --out
runs/F8-belief-update/lean-state-join-check.edn` resolves `machineBeliefUpdate`
to `MachineBeliefUpdate.lean:181`, kind `def`, and the equation join moves **10
of 18 carriers declared / 10 resolving → 11 of 18 declared / 11 resolving**,
`:carrier-declared-but-not-found []`. The seven remaining class-(a) rows are
`:observe :policy-free-energy :belief-state :policy-set :depth :temperature
:action`.

## 9. Found and not repaired

**(a) This row's `:code` pointer does not go where it says** — the same defect
class slice 2 found one row over, and this is the second consecutive quantity
whose `:code` resolves onto unrelated code. `:code "war_machine.clj:4375-4388,
4429-4431"` lands inside `scan-blocks`, the Block-footer commit scanner across
the 14-repo manifest, and in the markdown table renderer's row formatter.
Neither has anything to do with belief. The aggregation is
`belief.clj:1122-1197`, the tick's inner step is `war_machine.clj:6147-6190`,
and the filter is `belief.clj:297-346`. Not repaired because `:code` is exactly
the field TN 9a's second-reader gate binds; it is named in the `:lean-note` so a
registry reader meets it, and it wants a signature rather than a quiet edit.
**Two in two slices is a pattern worth a sweep of all 18 rows' `:code`
pointers**, which is a slice of its own and is not done here.

**(b) The Lean expected health drops production's clamp.**
`MachineBeliefUpdateWitness.expectedHealth` is `(raw + 1)/2`;
`entity-expected-health` is `max(0, min(1, (raw + 1)/2))`
(`belief.clj:625-638`). On a normalised posterior — the only input the filter
produces — `raw ∈ [−1, 1]` and the clamp is inert, so the two agree everywhere
the readback measures. Off that domain they are different functions and the
Lean one is not the implementation. Stated here rather than repaired, because
the honest repair is a hypothesis on the argument, which is a Lean edit this
slice's second reader should see rather than one written under it.

**(c) The published section is now three rows behind.**
`runs/U35-lean-state/lean-state-report.edn` and `p4ng/sec-lean-state-generated.tex`
still say 8 of 18 and still render `:precision`, `:prediction-error` and
`:belief-update` as not in Lean. Not refreshed on purpose: regeneration into a
publish happens after review (TN §9a), so the join above was written to a
slice-local path and nothing published moved.

**(d) C517 §2's `:forward-model` finding is still untouched:**
`aif-equations.edn:114` still declares `PredictiveOutcomeKernel :carrier-only`
while `MachineQ.lean:187` declares `machinePredictiveOutcomeKernel`, named by no
row.

## 10. Not claimed

No live tick, no run lock taken, nothing written under `data/`. No `src/` change
in futon2, so no test suite is owed (clj-kondo is run because the slice's `.clj`
script is on the board). `gen_aif_dag.bb` NOT run and nothing regenerated into a
publish. **No ruling** — `aif-equations.edn :choices` and
`control-map-edges.edn :decisions` are byte-unchanged. Nothing is claimed about
the R8 errors that feed the driver (closed by slice 2), about the `:legacy` and
`:a-matrix` likelihood modes, or about whether the saturation, the
flag-dependent units or the sign asymmetry should change.

## 11. Gates

Bare exits, run at this commit:

- `lake env lean` on `MachineBeliefUpdate.lean` and
  `MachineBeliefUpdateWitness.lean`: exit 0, 0 errors, 0 warnings.
- `lake build DarkTower.WarMachine.MachineBeliefUpdateWitness`: 2708/2708,
  success.
- `#print axioms` on all 49 declarations: no `sorryAx` (§6).
- `bb lean_state_probe.bb --no-typecheck`: 77 modules, 858 declarations, 12966
  source lines; sorry 10 source / axioms 0 — unchanged, these modules add
  neither.
- `clj-kondo` on `f8_belief_update_readback.clj`: 0 errors, 0 warnings.
- `futon4/dev/check-parens.sh` on the script, `aif-equations.edn` and
  `worklist.edn`: OK.
- `bash p4ng/empirics-futon/negative_controls.sh`: PASS.
- `bb p4ng/empirics-futon/pointer_check.bb`: 0 unresolved.
- `bb worklist_check.bb`: exit 0.
