# C518 — :F8 leg 1, slice 1: the precision map Π, stated in Lean

**Row:** `:F8` (`:loop-mode :one-slice-per-invocation`), leg 1 (Lean
completion): "one Lean statement per class-(a) quantity … Slice discipline: one
quantity or one coherent cluster per slice" (`EPIC-run-era.md:788-793`;
`worklist.edn:1304`). Slice 0 was the U35 join refresh (C517). **This slice's
quantity is Π (`:precision`, node R7)**, one of the ten rows C517 §3 left with
no declared Lean carrier.

**Commits.** mathlib4 `e2e8ee9649` (the two Lean modules) + this futon2 commit
(the registry declaration, the readback script and artifact, this report, the
ledger).

## 1. What was in Lean before, and what was not

`Holes.PrecisionMap` (`mathlib4/DarkTower/WarMachine/Holes.lean:6820`) is an
`abbrev` for `Channel → NonnegativeReal` — the **carrier**, a nonnegative
channel-indexed weight, with nothing said about where the weight comes from.
`PrecisionWitness.lean` witnesses only that precision and prediction error are
not interchangeable inside `variationalFreeEnergy`. So the R7 equation had no
Lean statement, and `aif-equations.edn:83` recorded that honestly as
`:lean nil :lean-status :missing`.

## 2. What the implementation computes

Read out of the source, not from the registry's summary
(`futon2/src/futon2/aif/precision.clj`, `update-channel-precision` at `:116-150`
under the production `:salience-mode :separate`):

```
window := the last windowSize prediction errors of the channel   (:136-140)
V      := (priorStrength·priorVariance + Σ_{e ∈ window} e²)
          / (priorStrength + |window|)                            (:73-86)
Π      := min (max (1 / max V minVariance) floor) cap             (:142, :145-146)
```

Defaults, `precision.clj:42-54`: window 20, minVariance 0.01, priorVariance 1.0,
priorStrength 1.0, floor 0.1, cap 200.0.

The registry's `:formal` line is `Pi_k := 1 / max(Var(eps_k), eps0)`. Three
things it does not carry, all visible above: the clamp; that `V` is a posterior
mean squared error under a variance prior rather than a sample variance; and
that the sum is over a bounded window rather than the whole history. (The row's
`:note` does say "bounded rolling variance", so the second and third are
recorded in prose beside a `:formal` line that omits them.)

## 3. The Lean statement

`mathlib4/DarkTower/WarMachine/MachinePrecision.lean` (`e2e8ee9649`, 272 lines; the witness is 132)
declares `windowOf`, `regularizedErrorVariance`, `varianceComponent`,
`machinePrecision` and `machinePrecisionMap` — the last returning a
`Holes.PrecisionMap`, so the R7→R8 seam into `Holes.variationalFreeEnergy` is
typed rather than described, with nonnegativity discharged by the floor rather
than assumed.

**`registryFormOfUnclamped`** (`MachinePrecision.lean:175-186`) is the exact
relation between the registry line and the implementation: they agree on
histories whose variance component already lies in `[floor, cap]`, and the
clamp is the whole of the difference.

## 4. The finding: two of the three guards are dead under the declared defaults

Proved, not asserted.

| guard | status under the defaults | theorem |
|---|---|---|
| `max V minVariance` | **never taken.** `V ≥ 1/21 ≈ 0.0476 > 0.01` on every history | `defaultsMinVarianceInert` |
| `min … cap` | **never taken.** `Π ≤ 21 < 200` on every history | `defaultsCapInert`, `defaultsLeTwentyOne` |
| `max … floor` | reachable | `floorIsReached` (witness) |

The reason is the prior against the window: `V ≥ priorStrength·priorVariance /
(priorStrength + windowSize) = 1/21` (`regularizedErrorVariance_ge`), so Π
cannot exceed 21, an order of magnitude under the declared cap of 200, and `V`
cannot dip to the 0.01 floor. **So the `eps0` in this row's own `:formal` line
is unreachable code under the parameters production runs with**, and
`defaultsRange` records what survives of the clamp: one bound, `[1/10, 21]`,
not three.

The inertness is a property of the **parameters**, not of the formula.
`MachinePrecisionWitness.wideWindowReachesMinVariance` exhibits a parameter set
differing from production only in its window (200 instead of 20) under which
the `minVariance` branch does fire, so the two inertness theorems are not
vacuous claims about an expression that could never take that branch.

**Not a ruling.** Whether the two dead guards should change is not decided here
and nothing is written to `:choices`.

## 5. The reference values, exact against production

`MachinePrecisionWitness.lean` proves ten values; `f8_precision_readback.clj`
reads the same ten out of `futon2.aif.precision` itself. Every history is
dyadic, so the Clojure double and the Lean rational are the same number and the
comparison needs no tolerance.

| Lean theorem | history | Lean | Clojure | delta |
|---|---|---|---|---|
| `oneStillError` | `[0]` | 2 | 2.0 | 0.0 |
| `threeStillErrors` | 3×`0` | 4 | 4.0 | 0.0 |
| `sevenStillErrors` | 7×`0` | 8 | 8.0 | 0.0 |
| `fifteenStillErrors` | 15×`0` | 16 | 16.0 | 0.0 |
| `fullWindowOfStillErrors` | 20×`0` | 21 | 21.0 | 0.0 |
| `windowBinds` | 25×`0` | 21 | 21.0 | 0.0 |
| `unitError` | `[1]` | 1 | 1.0 | 0.0 |
| `halfError` | `[1/2]` | 8/5 | 1.6 | 0.0 |
| `tripleError` | `[3]` | 1/5 | 0.2 | 0.0 |
| `floorIsReached` | 20×`10` | 1/10 | 0.1 | 0.0 |

Artifact: `runs/F8-precision/clojure-readback.txt`, byte-identical on a second
run of the script. `windowBinds` is the window made visible: five further still
ticks do not move Π, because the oldest five errors have left the window — an
unbounded history would give 26. `registryFormFailsAtTheFloor` is the refusing
half of the pair: on the 20×`10` history the registry's `:formal` line and the
implementation's map give **different values**, and
`registryFormOfUnclamped`'s hypothesis is exhibited failing there.

## 6. The registry movement

`aif-equations.edn:83` `:precision`: `:lean nil :lean-status :missing` →
`:lean "machinePrecision" :lean-status :closed`, with a `:lean-note` carrying
§3–§5 and a `:lean-at` naming mathlib4 `e2e8ee9649`.

This writes `:lean`, `:lean-status`, `:lean-note`, `:lean-at` and **no other
field**. TN §9a's second-reader gate binds edits to `:code`, `:realised` and
`:imports`; none of those moved, and neither did `:choices` or
`control-map-edges.edn :decisions`, which are byte-unchanged.

Verified rather than assumed: `bb lean_state_probe.bb --no-typecheck --out
runs/F8-precision/lean-state-join-check.edn` resolves `machinePrecision` to
`DarkTower/WarMachine/MachinePrecision.lean:101` (kind `def`), and the equation
join moves **8 of 18 carriers declared / 8 resolving → 9 of 18 declared / 9
resolving**, `:carrier-declared-but-not-found []`. `:precision` leaves
`:no-carrier-declared`, which is now the nine remaining class-(a) rows:
`:observe`, `:prediction-error`, `:policy-free-energy`, `:belief-update`,
`:belief-state`, `:policy-set`, `:depth`, `:temperature`, `:action`.

## 7. Two things left standing, stated

**(a) The U35 receipt and the published section are one row behind this
commit.** `runs/U35-lean-state/lean-state-report.edn` and
`p4ng/sec-lean-state-generated.tex` still say 8 of 18 and still list
`:precision` among the rows rendered "not in Lean", and the corpus counts there
(71 modules / 725 declarations / 11762 lines) are now 73 / 759 / 12166. **Not
refreshed here on purpose**: regeneration into a publish happens after review
(the prompt's gate rule, TN §9a), and the U35 receipt is slice 0's artifact, not
this slice's. The check above was written to a slice-local path instead so that
nothing published moved. Refreshing both is a one-command step for whoever
takes the next slice or leg 3.

**(b) C517 §2's `:forward-model` finding is untouched.** `aif-equations.edn:114`
still declares `:lean "PredictiveOutcomeKernel" :lean-status :carrier-only`
while F1 slice 1's `MachineQ.lean:187` declares
`machinePredictiveOutcomeKernel`, which no registry row names. That is a
different row's quantity and stays where C517 left it.

## 8. Gates

| gate | result |
|---|---|
| `lake env lean DarkTower/WarMachine/MachinePrecision.lean` | exit 0, **0 errors, 0 warnings** |
| `lake env lean DarkTower/WarMachine/MachinePrecisionWitness.lean` | exit 0, **0 errors, 0 warnings** |
| `lake build DarkTower.WarMachine.MachinePrecisionWitness` | 2706/2706 built, success |
| `#print axioms` on all 13 new theorems and definitions | `[propext, Classical.choice, Quot.sound]` on every one; **no `sorryAx`** |
| `bb lean_state_probe.bb --no-typecheck` | 73 modules, 759 declarations, 12166 lines; **sorry 10 source, axioms 0** (unchanged — these modules add neither) |
| `clj-kondo --lint f8_precision_readback.clj` | **0 errors, 0 warnings** |
| `futon4/dev/check-parens.sh` | **OK** on `f8_precision_readback.clj`, `aif-equations.edn`, `worklist.edn` |
| `bash p4ng/empirics-futon/negative_controls.sh` | see the ledger row |
| `bb p4ng/empirics-futon/pointer_check.bb` | see the ledger row |
| `bb worklist_check.bb` | see the ledger row |

Every `precision.clj` line pointer written into the Lean docstrings and this
report was checked against the file rather than copied from the registry — the
registry's own `:code` pointer for this row reads `precision.clj:116-135`, and
`update-channel-precision` in fact runs to `:158`. That pointer is left as it
stands (it resolves, and correcting `:code` is the field TN §9a gates).

**Not claimed.** No live tick, no run lock taken, nothing written under `data/`.
No `src/` change in futon2, so no test run is claimed; `clj-kondo` is run
because a `.clj` script was added. `gen_aif_dag.bb` was not run and nothing was
regenerated into a publish. No ruling: `:choices` and `:decisions` byte-unchanged.
