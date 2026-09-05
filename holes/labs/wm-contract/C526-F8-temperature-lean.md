# C526 — :F8 leg 1, slice 8: the selection temperature τ, stated in Lean

**Row:** `:F8` (`:loop-mode :one-slice-per-invocation`), leg 1 (Lean
completion): one Lean statement per class-(a) quantity. Slice 0 was the U35 join
refresh (C517), slice 1 Π (C518), slice 2 ε (C519), slice 3 μ-next (C520), slice
4 F_π (C521), the `:code` pointer sweep C522, slice 5 o (C523), slice 6 μ (C524)
and slice 7 T (C525). **This slice's quantity is τ (`:temperature`, node R14)** —
the second of the three rows left after slice 7, chosen over `:policy-set` and
`:action` on readiness: it is the only one of the three whose `:code` already
names the functions that compute it.

**Two seats wrote this slice.** The two Lean modules as first delivered are
codex-9's, under Agency job `invoke-1788624258823-7961-16c1fefa` (mathlib4
`d7c43bca25`); that packet excluded the readback, the registry, the ledger and
this report by instruction. The independent production probe below, the review,
the four repairs it produced (mathlib4 `b31e5db2e4`), the readback, the registry
declaration, the join re-run and this report are the reviewing seat's.

**The dispatch did not borrow an id this time.** C525 recorded a coordination
failure caused by sending with `--from claude-1`: codex-9's bell routed to a
real seat, which dispatched a duplicate. This packet was sent with
`--from wm-build-loop`, which is not on the roster, and said so in its first
paragraph — *"Do NOT rely on a completion bell reaching me: I poll
`GET /api/alpha/invoke/jobs/<id>` myself."* The job was polled to `done` and
nothing routed anywhere. This seat is a `claude -p` one-shot under
`wm-build-loop.sh` and cannot park; polling is the correct protocol for it.

## 1. The registry line, and what is wrong with it

`aif-equations.edn`, `:id :temperature`, node R14, `:class :stack-defined`:

    :formal "tau := commitment temperature (inverse precision gamma of policy selection)"
    :eq     "friston2017 eq. 2.1 (γ = 1/β); dacosta2020 A.2"

It names **one** quantity and cites **one** equation. The machine has three
laws, and the equation is true of one of them. The carrier says so in its type:

    machineTemperature : TemperatureOpts → Except TemperatureError ℝ

— a dispatch over three arms, partial exactly where production throws.

`CommitmentTemperature.lean` already existed and is about R14's τ-to-*action*
channel with an abstract `Nat` temperature; it is imported and cited here, not
duplicated. Its `live_selector_does_not_govern` is the behavioural result; §6
below is the numeric reason for it.

## 2. The independent probe, run before codex-9 delivered

Every measurement in §3–§6 was produced twice: once by this seat calling
production `futon2.aif.policy` and `futon2.report.war-machine` directly
**before** codex-9's modules landed, and once by the readback. The probe and its
transcript are committed beside the readback
(`runs/F8-temperature/review-independent-probe.clj` and `.txt`) so the two
derivations can be compared rather than taken on trust. They agree on every
number.

## 3. There is no single τ

`effective-temperature` (`policy.clj:77-146`) dispatches on `:tau-mode`:

| mode | law | pointer |
|---|---|---|
| `:spread` | τ_eff = τ_spread / g | `policy.clj:133` |
| `:selection-gain-only` | τ_eff = 1 / g | `policy.clj:134` |
| `:variational-beta-gamma` | τ_eff = β | `policy.clj:135-141` |

with `g = (max tau-min selection-gain)` (`:132`) and
τ_spread = `max(tau-min, range(G)/k)`, tau-min `0.01`, k `5.0`
(`policy.clj:33-45`).

`threeLawsDisagree` states this on **one** input at which every law is defined —
`g-totals [1.0 2.0 3.5 0.5]`, gain `2.0`, β `0.25`, production's `tau-min` and
`k`. **Measured** at HEAD:

| law | τ | delta vs theorem |
|---|---|---|
| `:spread` | 0.3 | 0.0 |
| `:selection-gain-only` | 0.5 | 0.0 |
| `:variational-beta-gamma` | 0.25 | 0.0 |

Pairwise distinct. `τ` does not denote until a mode is named.

## 4. The `:eq` line is true of one arm

The score divides by τ (`policy.clj:209`), so the precision the machine applies
is γ = 1/τ. `variationalGammaIsInverseBeta` gives γ = 1/β exactly under the
variational arm. `engineeringGammaIsNotInverseBeta` gives the other two: on the
same input, where β = 1/4 is present and 1/β = 4, the engineering γ's are 10/3
and 2. **Measured:** `3.3333333333333335`, `2.0`, `4.0`, delta 0.0 on all three.
`friston2017 eq. 2.1` describes an arm of the dispatch, not the row's quantity.

## 5. The two defaults name different laws

`effective-temperature`'s own opt default is `:spread` (`policy.clj:128-129`),
and `temperature-source` agrees (`:selection-gain-spread` for an absent
`:tau-mode`, `policy.clj:57-75`). The **live** default is not that:
`tau-mode-of` maps an unset `FUTON_WM_TAU_MODE` — and every unrecognised value —
to `:selection-gain-only` (`war_machine.clj:865-869`), and that reaches the
temperature opts at `:1002`. **Measured:** env unset, arena
`:selection-gain-only`, policy default `:spread`. `temperatureDefaultsDisagree`
states it over two named definitions carrying their pointers, per slice 7's
lesson that a theorem tied to no machine constant says nothing about the
machine.

**Two further facts about the laws, both measured.** The variational arm is
partial and never falls back: missing, zero, negative, NaN and Infinity all
throw (`finite-pos?` at `policy.clj:47-55`, the raise at `:136-141`) rather than
reverting to 1/g — all five refuse, none returns a number. And the floor is the
gain's, not β's: `g = 0` gives `1/tau-min = 100.0`
(`gainFloorPreventsDivisionByZero`) while β = `0.001`, below `tau-min`, survives
unchanged (`betaIsNotFloored`).

**"Variational τ" does not entail a solve this tick.** For
`:variational-beta-gamma`, `temperature-source` reports `carry-beta`'s own
`:beta-source` — `:converged-posterior | :held-unsolved | :held-absent |
:initial` (`policy_precision.clj:497-560`, `:551`, `:443`). Three of the four
are a **held** β, the previous tick's posterior or β₀, wearing the name of the
solved quantity. The mode names the law; the source says whether the number was
solved; only the pair determines what τ is.

## 6. What τ does to the scores, and when it can reach a choice

Raising τ contracts the gap between two candidates' scores
(`largerTemperatureFlattensScores`). **Measured** on `G = [1, 0]`: gap `1.0` at
τ = 1, `0.1` at τ = 10, delta 0.0 on both.

But for any positive τ the map `g ↦ −g/τ` is strictly antitone, so **every**
positive τ induces the same order on the candidates. With a zero habit prior —
the production default — no argmax over the τ-scaled scores can move at any
temperature (`zeroPriorOrderIsTemperatureInvariant`). τ reaches the choice only
through the **unscaled** habit prior, which is the commitment stated at
`policy.clj:225-229` ("controller temperature modulates G, never the habit
prior"). `habitPriorMakesOrderTemperatureDependent` exhibits it: cautious
⟨lnE 0, G 0⟩ against habitual ⟨lnE 1, G 2⟩. **Measured:** production selects
`:cautious` at τ = 0.5 and `:habitual` at τ = 8.0.

**Measured on the live strategic boundary:** the selected action is the same at
τ = 0.1 and τ = 9.0 while the recorded `:tau` and `:softmax-weights` both move —
the quantitative counterpart of `CommitmentTemperature.record_sensitivity_is_not_governance`.

## 7. The review was a real gate and found four things

All four were fixed by this seat rather than re-dispatched (mathlib4
`b31e5db2e4`).

**(a) The sharpest: the module dispatched over three laws and never stated that
they are three.** There was no theorem anywhere that two arms differ on a shared
input. The finding the slice exists for was *defined* — as three constructors of
`TauMode` — and not *stated*. This is the fourth consecutive slice whose review
found exactly that shape (slice 6's four theorems with no conjunction, slice 7's
`gTermsDisagreeOnDepth` gap, slice 5's five repairs). Added `threeLawsDisagree`
and, in the witness, `threeLawsReference`.

**(b) The registry's own γ = 1/β was not mentioned in either module.** Nothing
connected the development to the `:eq` line it is about. Added `machineGamma`,
`variationalGammaIsInverseBeta` and `engineeringGammaIsNotInverseBeta`.

**(c) `adaptive-temperature` was an opaque input field**
(`TemperatureOpts.spreadTemperature`) rather than a computed function, although
it is a named production function with a floor of its own and is one of the
three sites the row's `:code` names. That left the floor's stated purpose — that
identical EFE inputs cannot divide τ to zero — unstatable, and the degenerate
and empty cases unproved. Added `gRange`, `adaptiveTemperature`,
`adaptiveTemperatureReference`, `emptySpreadIsTheFloor` and
`degenerateSpreadIsTheFloor`.

**(d) `betaIsNotFloored` had nothing to contrast with.** The gain floor was in
the definition and never shown to bite, so "β is not floored" was half a
contrast. Added `gainFloorPreventsDivisionByZero` and its production witness,
plus concrete `zeroBetaReference` and `negativeBetaReference` — two of the four
things `finite-pos?` rejects that the witness module never exercised.

**A softer fifth, left standing.** `selectionLawControlsTemperatureChannel` is
true by `decide` of a Boolean the module itself defines, so it *transcribes* the
dataflow rather than deriving it. That is a legitimate transcription under slice
7's standard, and `CommitmentTemperature.lean`'s `factorsThroughDiscard`
vocabulary was available to do better. Rather than delete it, this seat added
`zeroPriorOrderIsTemperatureInvariant` beside it — the derivation of *why* the
head law is τ-blind, which is a statement about the machine's expression rather
than about a Boolean.

## 8. What the reviewing seat checked, so the review is auditable

- Production probed **independently by this seat before codex-9 delivered**;
  probe and transcript committed (`runs/F8-temperature/review-independent-probe.*`).
  The two derivations agree on every number.
- Both modules read in full at the delivered sha `d7c43bca25`, and `lake env
  lean` re-run on both at the delivered sha (exit 0, 0 errors, 0 warnings each)
  before any repair — so codex-9's own gate claim was verified rather than
  taken.
- `#print axioms` **re-enumerated from the sources** on all 40 declarations at
  the repaired sha (codex-9 delivered 21). Every declared name is printed and no
  printed name is undeclared, checked by `comm` over two generated lists.
- The registry edit verified key by key against `git show HEAD:` read as EDN:
  **one** row changed, **four** keys added, none removed, no value moved on any
  existing key, row order preserved, no top-level key other than `:equations`
  touched — so `:choices` and `:references` are byte-unchanged.
- The readback run twice with identical `sha256`
  `ee17ddf07d1c5bac65188fcd785e238582eaac8621d05030e70d5d23cbbd751b`; 34 lines,
  0 `MISMATCH`.
- The join probe run after; the "before" is slice 7's committed after-state,
  since codex-9's modules were already on disk by the time this seat could
  probe. Both committed.

## 9. The join

`bb lean_state_probe.bb` resolves `machineTemperature` and the equation join
moves **15 of 18 carriers declared / 15 resolving → 16 of 18 / 16**,
`:carrier-declared-but-not-found []`, at
`DarkTower/WarMachine/MachineTemperature.lean:42`, kind `def`. Corpus: **87
modules** (85 before), 1044 declarations, 14358 source lines; **87/87 modules
elaborate exit 0, 0 error diagnostics**; sorry 10 source / 10 Lean-reported and
axioms 0 — unchanged, these modules add neither.

**Two class-(a) rows remain:** `:policy-set` and `:action`.

**No ledger supersession was needed, and the absence was established rather than
assumed:** all 52 rows carrying a collection `:covers-key` were enumerated, and
the only one naming `[:equations {:id :temperature}]` is `:F8` itself, which is
`:open` and unsigned. **`:F8`'s `:covers-key` is unchanged** — the C522 `:code`
sweep had already put `:temperature` on it, so the row had already declared the
entry it moved, which is what the list is for.

## 10. Gates, bare exits

- `lake env lean` on `MachineTemperature.lean` and `MachineTemperatureWitness.lean`
  — exit 0, 0 errors, 0 warnings each, at the repaired sha.
- `lake build DarkTower.WarMachine.MachineTemperatureWitness` — exit 0, 8498/8498.
- `#print axioms` on **all 40 declarations**, enumerated from the sources — 9
  depend on no axioms, 2 give `[propext]`, 29 give
  `[propext, Classical.choice, Quot.sound]`. **0 `sorryAx`.**
- `clj-kondo` — 0 errors, 0 warnings on `f8_temperature_readback.clj` and on the
  review probe (linted separately; linting both together reports a spurious
  `user/ranked-hp` redefinition, since both are `user`-namespace scripts).
- `check-parens.el` — OK on the readback, the probe, `aif-equations.edn` and
  `worklist.edn`.
- `bb p4ng/empirics-futon/pointer_check.bb` — 1613 pointers in 3 files, **0
  unresolved** (1604 before).
- `bash p4ng/empirics-futon/negative_controls.sh` — **PASS, 88 negative / 45
  positive**, shared registries untouched.
- The readback — exit 0, run twice, identical `sha256`.

## 11. Found and not repaired

**(a) `policy.clj`, `war_machine.clj` and `policy_precision.clj` are untouched,
on purpose.** The three laws under one name (§3), the two disagreeing defaults
(§5) and the severed τ-to-action channel (§6) are *stated* here and left
standing. Changing any of them is a change to the machine, and this is a leg-1
"state it in Lean" slice; they are findings for an I-track item, not fixes
smuggled under this row.

**(b) `tau-mode-of` maps every unrecognised string to `:selection-gain-only`**
rather than refusing it, so a typo in `FUTON_WM_TAU_MODE` silently runs the
gain-only law and the record says `:selection-gain-only` truthfully. Measured
(`tau-mode-of "nonsense"` → `:selection-gain-only`) and stated, not ruled on.
The `:tau-mode` dispatch one seam later *does* refuse an unknown value, so the
two ends of the same closed set disagree about what to do with garbage.

**(c) The published section was not regenerated.** TN §9a gates regeneration;
`gen_aif_dag.bb` was not run.

**Not claimed.** No live tick, no run lock, nothing written under `data/`. No
production source changed. The live tick's own τ is not callable from the
readback and is recorded as not measured. No ruling; `:choices` is
byte-unchanged. Legs 2 (name discipline) and 3 (convergence ledger) remain
unstarted.
