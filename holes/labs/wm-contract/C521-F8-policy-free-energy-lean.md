# C521 — :F8 leg 1, slice 4: the policy free energy F_π, stated in Lean

**Row:** `:F8` (`:loop-mode :one-slice-per-invocation`), leg 1 (Lean
completion): one Lean statement per class-(a) quantity. Slice 0 was the U35
join refresh (C517), slice 1 was Π (C518), slice 2 was ε (C519), slice 3 was
μ-next (C520). **This slice's quantity is F_π (`:policy-free-energy`, node
R8)** — the last of the seven remaining class-(a) rows that carried an explicit
`:lean-status :missing`; the other six carry no `:lean` key at all.

**Two seats wrote this slice, and the split decides who checked what.** The two
Lean modules and the readback script are codex-9's, under Agency job
`invoke-1788617707096-7930-72dc77c1` (mathlib4 `3783d50968`, futon2
`928b898c90`); that packet excluded the registry, the ledger and this report by
instruction. The review below, the two Lean repairs it produced (mathlib4
`d7a45a358a`), the readback's provenance header, the registry declaration, the
`:RUN9` supersession, the join re-run and this report are the reviewing seat's.

**Commits.** mathlib4 `3783d50968` (the modules as delivered) + mathlib4
`d7a45a358a` (the two review repairs) + futon2 `928b898c90` (the readback) +
this futon2 commit (the readback's provenance header, the registry
declaration, the ledger, this report).

## 1. What was in Lean before, and what was not

Nothing. `aif-equations.edn:99-107` recorded `:lean nil :lean-status :missing`.
The only F_π material in the corpus was the H4 hole
`policyPosteriorImportsPolicyF` (`Holes.lean:7684, 7918`), which is a claim
about whether the term reaches a default-path posterior — a different question
from what the term *is*.

## 2. The transcendental term, and why this slice cannot do what slices 1–3 did

Slices 1, 2 and 3 each gave **exact rational** reference values and measured
production at delta 0.0 against them. That is not available here. F_π's
per-channel term is `½(ln(2πv) + r²/v)`, and `ln(2πv)` is transcendental, so no
rational states it.

The decision taken, and it is the honest one rather than the convenient one:
**Lean keeps `Real.log` symbolic and proves the algebraic part exactly**; the
readback evaluates only that one factor in Clojure and reports the measured
whole-term delta. What makes this a measurement rather than a restatement is
the provenance rule now written into the readback's header: **every `-expected`
in the readback is transcribed from a named Lean theorem**, not derived a
second time from the inputs. Without that rule a delta of 0.0 would only say
that Clojure agrees with Clojure — the same formula evaluated twice in the same
language — which is not what the F3 certificate form asks for (a reference
derived from the carriers independently of the thing under test).

## 3. The registry's `:formal` line is one branch of three

`F_π := Σ_k ½(ln(2π v_k) + (o_k − μ_k)²/v_k)` is the **positive-variance branch
only**. `varianceTrichotomy` states the whole table
(`policy_free_energy.clj:119-142`):

- `v < 0` — always rejected, `:invalid-variance`. Never floored, never scored.
- `v > 0` — the Gaussian term above.
- `v = 0` — a **deterministic claim**: contributes exactly 0 when
  `|residual| ≤ :deterministic-tolerance`, and otherwise rejects with
  `:deterministic-mismatch`. It never returns an infinity for the density.

**And not every zero means the same thing.** A zero carrying
`:variance-status {:status :absent}` was written by the action model to mean
*no prediction*, and may be replaced by `:variance-floor`; a bare zero is a real
deterministic claim and still rejects, even in `:floor` mode.
`absentZeroFloored` and `bareZeroStillRejectsUnderFloor` are the two sides.
This is not a corner: the Clojure docstring records that on a real tick **12 of
14 channels** carry an action-model absent zero and 7 of 14 move between ticks
(`policy_free_energy.clj:59-79`), so a statement that collapsed the two kinds of
zero would have stated a function that cannot be run on WM data at all.

## 4. Which arm is production — checked, not assumed

`f-pi-for-candidate`'s **own default** is `:absent-variance :reject`. Its **only
production caller** is `f-pi-dark-readback`, which passes
`{:absent-variance :floor}` and therefore takes the default `:variance-floor`
0.01 (`war_machine.clj:528-533`). So the floored arm is the map the machine
runs and the `:reject` arm is reachable and unused. This is the same class of
check slice 3 made on the likelihood mode (`:aif` production default against
`update-entity-beliefs`' own `:legacy`), and it is recorded here because a Lean
module that stated only the function's declared default would have stated a map
the machine does not run.

## 5. F_π enters the score unscaled, proved

`selection-scores` is *the one place the score expression is written*
(`policy.clj:156-211`): `ln E(a) − G(a)/τ [− F_π(a)]`, with `:f-pi-scaling`
`:unscaled` (default) or `:by-tau`. `policyFreeEnergyEntersUnscaled` exhibits
the asymmetry — under `:unscaled` the F_π contribution is `−f` at every τ while
the G contribution moves with τ; under `:by-tau` it is `−f/τ`. The registry
`:note` already records that this is **settled by source and not chosen**
(friston2017 eq. 2.7 scales G by γ alone; B.9 carries no temperature), so the
Lean states a reduction rather than a preference.

## 6. The review, and the two repairs it produced

What was re-run rather than accepted:

- **The readback reproduces the committed artifact byte for byte**, and its
  SHA-256 `43d82594…be45251` matches the one codex-9 reported.
- `lake env lean` on both modules: exit 0, **empty output** — 0 errors, 0
  warnings, checked by byte count and not by reading a summary.
- `#print axioms` driven from an independently written file over **all 21
  declarations by name**: 0 `sorryAx`.
- `machinePolicyFreeEnergy` and every other name the modules introduce
  (`channelPolicyFreeEnergy`, `selectionScore`, `VarianceStatus`,
  `AbsentVarianceMode`, `ChannelDatum`, `FPiScaling`, `PolicyFreeEnergyError`)
  have **exactly one defining site** in `DarkTower/` — leg 2's rule, checked
  rather than assumed, because `lean_state_probe.bb` keeps the *first* site for
  a repeated name.
- The registry's `:code` pointers, re-resolved at HEAD before the packet was
  written — §9(a).

**Two findings, both fixed by this seat rather than re-dispatched** (mathlib4
`d7a45a358a`):

**(a) `successfulTotalIsFinite` was a tautology.** Its proof term was
`⟨total, h⟩`: the hypothesis *was* the conclusion's witness. It stood in the
slot for the Clojure's "never returns Infinity or NaN" claim
(`policy_free_energy.clj:56-57`) and established nothing about the
implementation. That claim is about IEEE doubles, and these totals live in `ℝ`,
which has no Infinity and no NaN to return — so it holds in the model by
construction and carries no information. The theorem is deleted and the reason
is stated in the module header instead, together with what *can* be said: the
division by `effectiveVariance` happens only on the branch where it is strictly
positive, so it is total here, and double overflow is a property of the machine
arithmetic that the readback measures rather than something this model proves.

**(b) The declared carrier had no witness.** All eight witness theorems were
about `channelPolicyFreeEnergy` (one channel) or `selectionScore`. The
composite `machinePolicyFreeEnergy` — the identifier the registry row names —
was evaluated nowhere in Lean, so the readback's two-channel total and both
`f-pi-vector` entries, which are the only multi-channel numbers measured, had
no Lean counterpart at all. `machineTwoChannelTotal` evaluates the carrier on
exactly the readback's `positive` candidate (`{:a 0.5 :b -0.25}` against
`{:a 1.0 :b 0.25}`, variances `{:a 0.25 :b 1.0}`), which is what makes §2's
provenance rule true of the composite and not only of its parts.

After both repairs: `lake env lean` 0/0 on both modules, `lake build` 2709/2709,
`#print axioms` over 21 declarations with 0 `sorryAx`, readback numbers
unchanged.

## 7. The registry movement

`aif-equations.edn`, **four fields on one row and no others**: `:lean nil →
"machinePolicyFreeEnergy"`, `:lean-status :missing → :closed`, plus new
`:lean-note` and `:lean-at "mathlib4 d7a45a358a"`. Verified by reading the
registry out of `git show HEAD:` and out of the working tree and diffing key by
key: **one row changed, two keys added, none removed, two changed; every other
equation byte-identical and every non-`:equations` top-level key
byte-identical.** `:imports`, `:formal`, `:eq`, `:note` and `:code` did not
move, so nothing TN §9a's second-reader gate binds was touched.

`bb lean_state_probe.bb --no-typecheck --out
runs/F8-policy-free-energy/lean-state-join-check.edn` resolves
`machinePolicyFreeEnergy` to `MachinePolicyFreeEnergy.lean:59`, kind `def`, and
the equation join moves **11 of 18 carriers declared / 11 resolving → 12 of 18
declared / 12 resolving**, `:carrier-declared-but-not-found []`. Corpus: 79
modules, 879 declarations, 13136 source lines; sorry 10 source / axioms 0,
unchanged — these modules add neither. **Six class-(a) rows remain:**
`:observe :belief-state :policy-set :depth :temperature :action`.

## 8. The ledger movements this slice was forced into, and why

**`:RUN9` is superseded by `:F8`.** `worklist_check` refused the registry edit:
`:RUN9` is a `:done` row whose signature at futon2 `9867157` covers
`[:equations {:id :policy-free-energy}]`, so changing that entry made the
signature stale. TN §9a leaves two doors — set the signed row back to
`:done-unreviewed`, or supersede it. Setting `:RUN9` back would discard codex's
2026-09-01 review of an entire live-wiring stage (211 tests / 906 assertions,
the S4 and S2 replay controls at delta 0.0, both real-tick preflights) over a
registry field that review never bound. So it is superseded, and `:RUN9` now
carries a `:superseded-note` saying so.

**What that costs, stated plainly rather than left for the count to swallow —
the same cost `:RUN9` itself recorded when it superseded C7.** Supersession is
row-scoped, so the signatures on `[:equations {:id :free-energy}]` and
`[:equations {:id :policy-posterior}]` lapse with it. This slice touched
neither, and §7's key-by-key diff is the evidence that neither moved.
Re-signing them is a small follow-up, not something this slice did.

**`:F8`'s `:covers-key :none` was false and is corrected.** The ledger defines
`:covers-key :none` as a *declaration* that the row touched no registry entry.
Leg 1 slices 1, 2 and 3 had each already declared a Lean carrier on an
equation entry (`:precision`, `:prediction-error`, `:belief-update`), so three
entries had been changed under a declaration that none had. Slice 4 added the
fourth and met the consequence, which is the check working rather than a new
problem. The row now names the four entries it has actually moved, with a
`:covers-key-note` recording the correction; it carries no `:review-covers`
because it is `:open` and unreviewed, and `worklist_check` demands a signature
sha only of `:done` rows, so the entries are **held for F8's review** rather
than counted as signed. This is a code-backed correction — the four commits are
in git — and not a ruling.

## 9. Found and not repaired

**(a) This row's `:code` pointers do not go where they say — the THIRD
consecutive quantity with the defect** (slice 2 found it for ε, slice 3 for
μ-next). All four `war_machine.clj` ranges miss at HEAD: `123-155` lands in the
`*focus-reconcile?*` docstring, `213-336` begins inside a different flag's
docstring, `384-461` is `variational-tau-preconditions!`, and `5095-5096` is a
comment block about storage manifests. The real sites are `war_machine.clj:286`
(`f-pi-posterior-preconditions!`), `:441` (`f-pi-dark-readback`, calling
`f-pi-vector` at `:528`), `:612` (`f-pi-posterior-opts`) and the tick call sites
at `:6381`, `:6429`, `:6447`. The two `policy_free_energy.clj` ranges and
`policy.clj:147-203` do resolve. Not repaired: `:code` is exactly the field TN
§9a's second-reader gate binds. **Three in three slices is no longer a pattern
to note but a sweep of all 18 rows' `:code` pointers**, and it is a slice of its
own. Note that `pointer_check.bb` does not catch this class at all — it checks
that a file exists and that the line range is inside it, not that the range
names what the row says it names.

**(b) C517 §2's `:forward-model` finding is still untouched**, now four slices
old: `aif-equations.edn` declares `PredictiveOutcomeKernel :carrier-only` while
`MachineQ.lean:187` declares `machinePredictiveOutcomeKernel`, named by no row.

**(c) The published section is now four rows behind.**
`runs/U35-lean-state/lean-state-report.edn` and
`p4ng/sec-lean-state-generated.tex` still say 8 of 18. Not refreshed on
purpose: regeneration into a publish happens after review (TN §9a), so the join
above was written to a slice-local path and nothing published moved.

## 10. Not claimed

**The tick-level wiring around F_π is not in Lean and this slice does not
pretend otherwise.** COMPLETE-OR-OFF — the coverage rule at
`f-pi-posterior-opts` (`war_machine.clj:612-690`) under which a single
uncovered candidate turns the term off for the whole tick — and the horizon-one
retrospective join by action identity (`f-pi-dark-readback`,
`war_machine.clj:441-560`) are named in the registry `:note` and stated nowhere
in Lean. They are the wiring around the quantity, not the quantity, and they
are the obvious next thing for this row.

No live tick, no run lock taken, nothing written under `data/`; the flag chain
(`FUTON_WM_FPI_POSTERIOR` / `FUTON_WM_FPI_DARK` /
`FUTON_WM_TRACE_POLICY_DETAILS`) was not exercised. No `src/` change in futon2,
so no test suite is owed. `gen_aif_dag.bb` NOT run and nothing regenerated into
a publish. **No ruling** — `aif-equations.edn :choices` and
`control-map-edges.edn :decisions` are byte-unchanged. Nothing is claimed about
whether the flag should be default-on, whether the `:by-tau` arm should reach
the live path, or about the H4 hole's disposition.

## 11. Gates

Bare exits, re-run **after** the review repairs and after this text was written:

- `lake env lean` on `MachinePolicyFreeEnergy.lean` and
  `MachinePolicyFreeEnergyWitness.lean`: exit 0, empty output (0 errors, 0
  warnings).
- `lake build DarkTower.WarMachine.MachinePolicyFreeEnergyWitness`: 2709/2709,
  success.
- `#print axioms` over all 21 declarations by name: 0 `sorryAx`.
- `bb lean_state_probe.bb --no-typecheck`: 79 modules, 879 declarations, 13136
  source lines; sorry 10 source / axioms 0 — unchanged.
- Readback re-run: exit 0, byte-identical to the committed artifact,
  SHA-256 `43d82594879c86e3aaee4038842e6af766ff6243c2761842825553279be45251`.
- `clj-kondo` on `f8_policy_free_energy_readback.clj`: 0 errors, 0 warnings.
- `futon4/dev/check-parens.sh` on the script, `aif-equations.edn` and
  `worklist.edn`: OK.
- `bash p4ng/empirics-futon/negative_controls.sh`: PASS (88 negative, 45
  positive).
- `bb p4ng/empirics-futon/pointer_check.bb`: 1540 pointers in 3 files, 0
  unresolved.
- `bb worklist_check.bb`: exit 0 — 175 items OK, 28 signed registry entries
  verified unchanged, 23 superseded and skipped.
