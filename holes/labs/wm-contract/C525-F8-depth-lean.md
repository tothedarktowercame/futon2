# C525 — :F8 leg 1, slice 7: the temporal policy depth T, stated in Lean

**Row:** `:F8` (`:loop-mode :one-slice-per-invocation`), leg 1 (Lean
completion): one Lean statement per class-(a) quantity. Slice 0 was the U35 join
refresh (C517), slice 1 Π (C518), slice 2 ε (C519), slice 3 μ-next (C520), slice
4 F_π (C521), the `:code` pointer sweep C522, slice 5 o (C523) and slice 6 μ
(C524). **This slice's quantity is T (`:depth`, node R13)** — the first of the
four rows left after slice 6, and the first `:theory-defined` row since slice 4.

**Two seats wrote this slice.** The two Lean modules as first delivered are
codex-9's, under Agency job `invoke-1788622857675-7953-3f07508b` (mathlib4
`955bffc561`); that packet excluded the readback, the registry, the ledger and
this report by instruction. The independent production probe below, the review,
the five Lean repairs it produced (mathlib4 `a4f5f77e96`), the carrier
definition (`9eef38b6a1`), the readback repair, the registry declaration, the
join re-run and this report are the reviewing seat's.

**One coordination failure is recorded here because it cost a job.** The
dispatching seat sent the first packet with `--from claude-1`, borrowing a
registered agent's id because it has none of its own. Codex-9's bail-out bell
therefore routed to the real `claude-1`, which dispatched its own continuation
of the same slice. Cancelling the redundant queued job took the agent-level
interrupt and killed that continuation instead. Its partial work (the first
`f8_depth_readback.clj`) survived on disk and is the basis of §7's readback.
A seat that is not on the roster should say so in the packet rather than
borrow an id that routes.

## 1. The registry line, and why it is false

`aif-equations.edn`, `:id :depth`, node R13, `:class :theory-defined`:

    :formal "T := temporal policy depth (the range of the sums in Q(o|pi) and G)"
    :eq     "dacosta2020 eq. 42 (sum over τ)"

It asks for **one** range. The machine does not have one. The carrier declared
on the row says so in its type:

    abbrev machineDepth := Option Nat → EfeDepths

— a map from the requested `:horizon-steps` to the **four** depths at which one
evaluation of G evaluates its four terms. `efeDepths` is the machine's
inhabitant of that type.

`Holes.lean` declares no depth or horizon quantity at all (grep for
`horizon`/`depth` is empty), so unlike slices 1–4 there is no glossary carrier
to compare against, and unlike slices 5–6 not even a competing structure.
Nothing is claimed about one.

## 2. The independent probe, run before codex-9 delivered

Every measurement in §3–§6 was produced twice: once by this seat calling
production `futon2.aif.efe`, `futon2.aif.forward-model` and `futon2.aif.rollout`
directly **before** codex-9's modules landed, and once by the readback. The
probe and its transcript are committed beside the readback
(`runs/F8-depth/review-independent-probe.clj` and `.txt`) so the two derivations
can be compared rather than taken on trust. They agree on every number.

## 3. Inside one evaluation of G, two terms follow the horizon and two do not

- `efe.clj:629-631` — the multi-horizon path runs only when `:horizon-steps` is
  present **and** `≥ 2`.
- `efe.clj:633-636` — `next-mean` becomes the depth-K final-state observation;
  **`next-var` is always the FIRST step's variance**, whatever K is.
- `efe.clj:641`, `:706` — risk (through `fe-on-predicted`) and
  `homeostatic-pressure` read `next-mean`: depth K.
- `efe.clj:701`, `:705` — `ambiguity-by-channel` and `predictability-bonus` read
  `next-var`: depth 1.

`gTermsDisagreeOnDepth` states this as one proposition rather than leaving it to
be assembled. **Measured** on a stressed state (`:sorry-count-norm 0.85`,
`:mission-health 0.2`; action `{:type :address-sorry :target :m1}`), K=1 vs K=3:

| term | K=1 | K=3 | delta |
|---|---|---|---|
| `:G-risk` | 85.937817842853430 | 83.526293368911200 | **−2.4115244739422366** |
| `:homeostatic-pressure` | 0.73000000000000000 | 0.49000000000000005 | **−0.23999999999999994** |
| `:G-ambiguity` | −100.48350494781259 | −100.48350494781259 | **0.0000000000000000** |
| `:predictability-bonus` | 12.985000000000001 | 12.985000000000001 | **0.0000000000000000** |

`:controller-score` moves −2.6995244739422370. The two zeros are exact.

## 4. The epistemic half of G has no τ-sum at all

The two zeros above are not an accident of this state. `predict` computes its
variance as `(predict-effects nil action)` — `forward_model.clj:334` passes
`nil` where the state would go — and every `:obs-variance` arm is a literal
keyed by action type (`:138`, `:148`, `:165`, `:172`, `:184`, `:193`, `:202`).
The variance is therefore a function of the **action alone**.

**Measured:** the step-3 variance of a `:address-sorry` rollout equals the
step-1 variance, max delta over channels `0.0`, maps equal.

So ambiguity and the predictability bonus are functions of a state-independent
variance: they **cannot** vary with K at any horizon
(`epistemicTermsAreHorizonBlind`, stated for an arbitrary epistemic functional).
The registry line's "the range of the sums in Q(o|π) and G" is not merely two
ranges instead of one — for the epistemic half there is no sum over τ to have a
range.

## 5. The `:kl` density mixes two depth indices, and the mixing changes no number

Under the live-arena default `:risk-mode :kl` (`efe.clj:611-612`) the
per-channel Gaussian takes `mu` from `next-mean` (depth K) and `s2` from
`next-var` (depth 1) — `efe.clj:684-685`. `klRiskIsNotAnySingleDepthDensity`
says the scored density carries no single depth **index**.

It says nothing more than that, and the docstring says so: by §4 the depth-K
variance the code discards **equals** the depth-1 variance it keeps, so the
mixed density's **values** coincide with the depth-K density's
(`mixedDensityValuesCollapse`). The mixing rule is real; its effect on the
number is nil. This qualification is the second review repair (§8) — the first
draft's theorem, read as a numerical claim, would have overstated production.

## 6. Four call sites, four depths; and a requested depth of 1 is not a depth

- `war_machine.clj:6283-6285` — `3`, and only when the anticipation snapshot is
  `:events-loaded?` with non-empty `:events`; passed on at `:6327`.
- `rollout.clj:474-479` — `rollout-horizon`, default `2`. **Measured: 2.**
- `forward_model.clj:278` — `default-horizon-steps` `3`, reachable only through
  the 2-arity at `:294`. **Measured: 3**, both directly and through the 2-arity.
- `cascade_lane.clj:381` — `best-rollout … :depth 5` on a live report lane,
  reaching `rollout-horizon` at `rollout.clj:694`. **Measured: 5.** *This fourth
  depth was found by the reviewing seat; the dispatch packet named three.*

`declaredDepthsDisagree` names the three disagreements and
`forwardDefaultAgreesWithLiveTick` names the one agreement, so the theorem is
not read as a claim of four distinct depths.

`someOneEqualsNone`: the `≥ 2` guard makes `:horizon-steps 1` the same
computation as no horizon at all. **Measured:** recorded `:horizon-steps`
`nil / nil / 3` for requests `nil / 1 / 3`; `:controller-score` delta between
`nil` and `1` exactly `0.0`.

And whether any depth above one is used is decided by **data**, not by the
model: no loaded anticipation events ⇒ depth 1 (`liveDepth`,
`noLoadedEventsMeansDepthOne`). The live tick's literal `3` is not callable from
the readback; it is recorded there as **not measured** rather than asserted.

Finally, `varyingPolicyNotConstantAction`: da Costa eq. 42 sums over τ with a
policy naming an action per τ, while `predict-multi-horizon` repeats **one**
action (`forward_model.clj:296-310`), so the two-step plan `false`-then-`true`
is reached by neither constant-action trajectory (proved on an action-sensitive
step, so the statement is not vacuous). This does **not** say the machine cannot
represent plans anywhere — `rollout.clj` scores ordered policies — only that the
depth-K object EFE scores is a repetition.

## 7. The conjunction

`machineHasNoSingleDepth` conjoins the depth mismatches in one proposition: risk
at K and ambiguity at 1 within one call; the `:kl` density at no single index; a
requested 1 equal to none; rollout ≠ live tick; cascade lane ≠ live tick; and no
loaded events ⇒ 1. The last three slice reviews each found a slice whose finding
existed only as separate theorems, so a reader could check every piece and never
find the claim the slice exists to make.

## 8. The review was a real gate and found five things

All five were fixed by this seat rather than re-dispatched (mathlib4
`a4f5f77e96`).

**(a) The sharpest: a theorem about numerals, not about the machine.**
`declaredDepthDefaultsDisagree` was `(3 : Nat) ≠ 2 ∧ (3 : Nat) = 3`, and the
witness module's `forwardDefaultReference` and `rolloutDefaultReference` were
`(3:Nat)=3` and `(2:Nat)=2` — proved by `rfl`, referring to nothing. Nothing
tied `3` to `default-horizon-steps` or `2` to `rollout-horizon`. The four depths
are now named definitions carrying their pointers, and every reference and the
disagreement are stated over those names.

**(b) An overstatement of production** — `klRiskIsNotAnySingleDepthDensity`
read as a numerical claim (§5).

**(c) The sharper true statement was missing entirely** —
`epistemicTermsAreHorizonBlind` (§4). The first draft had no notion of the
variance model at all.

**(d) Not one declaration carried a docstring.** Every finding lived in the
module header or nowhere. Every definition and theorem now carries its
`file:line`, and those that could be over-read say what they do not show.

**(e) The readback measured neither the homeostatic leg, nor the fourth depth,
nor the state-blind variance that explains the two zeros**, and named no witness
theorem on any line — so a `0.0` in it could not be traced to the theorem it was
supposed to be transcribed from. It now names a theorem per line.

## 9. What the reviewing seat checked, so the review is auditable

- Production probed **independently by this seat before codex-9 delivered**;
  probe and transcript committed (`runs/F8-depth/review-independent-probe.*`).
  The two derivations agree on every number.
- Both Lean modules read in full at the delivered sha.
- `lake env lean` re-run on both at the repaired sha.
- `#print axioms` **re-enumerated from the sources**, not taken from the report:
  codex-9 reported 27 declarations on its delivered state; this seat counts 48
  on the repaired state.
- The readback run twice, identical `sha256`.
- The registry edit verified key by key against `git show HEAD:` read as EDN.
- The join probe run before and after, both committed.

## 10. The join, and where leg 1 stands

`bb lean_state_probe.bb` (full, with type-check) resolves `machineDepth` and the
equation join moves **14 of 18 carriers declared / 14 resolving → 15 of 18 / 15**,
`:carrier-declared-but-not-found []`. Corpus: 85 modules, 1004 declarations,
13976 source lines; **85/85 modules elaborate exit 0, 0 error diagnostics**;
sorry 10 source / 10 Lean-reported, axioms 0 — unchanged, these modules add
neither. Before and after are committed as
`runs/F8-depth/lean-state-join-{before,after}.edn` (the "before" is slice 6's
committed after-state, since the two new modules were already on disk by the
time this seat could probe).

**Three class-(a) rows remain:** `:policy-set`, `:temperature`, `:action`.

No ledger supersession was needed, and the absence was established rather than
assumed: all 52 rows carrying a collection `:covers-key` were enumerated, and
the only one naming `[:equations {:id :depth}]` is `:F8` itself, which is
`:open` and unsigned. (`:depth` was already in `:F8`'s `:covers-key` list, added
by the C522 `:code` sweep.)

## 11. Gates, bare exits

- `lake env lean` on `MachineDepth.lean` and `MachineDepthWitness.lean` — exit
  0, 0 errors, 0 warnings each, at the repaired sha.
- `lake build DarkTower.WarMachine.MachineDepthWitness` — exit 0, 8497/8497.
- `#print axioms` on **all 48 declarations**, enumerated from the sources — 43
  depend on no axioms, 4 give `[propext, Quot.sound]`, 1 gives `[propext]`.
  **0 `sorryAx`.**
- `clj-kondo --lint f8_depth_readback.clj` — 0 errors, 0 warnings; the review
  probe also 0/0.
- `check-parens.el` — OK on the readback, the probe, `aif-equations.edn` and
  `worklist.edn`.
- `bb p4ng/empirics-futon/pointer_check.bb` — 1610 pointers in 3 files, **0
  unresolved** (1589 before).
- `bash p4ng/empirics-futon/negative_controls.sh` — **PASS, 88 negative / 45
  positive**, shared registries untouched.
- The readback — exit 0, run twice, identical `sha256`
  `44e3306827137cf5e32276ad7b2777a9de607456fe8d72aeba8c75779242e848`.

## 12. Found and not repaired

**(a) `efe.clj`, `forward_model.clj`, `rollout.clj` and `war_machine.clj` are
untouched, on purpose.** The depth disagreement of §3, the horizon-blind
epistemic terms of §4 and the four uncoordinated depths of §6 are *stated* here
and left standing. Changing any of them is a change to the machine, and this is
a leg-1 "state it in Lean" slice; they are findings for an I-track item, not
fixes smuggled under this row.

**(b) The mixed-depth `:kl` density is left as it is** (§5) — the honest reading
is that the mixing costs nothing *today* only because the variance model is
state-blind. A learned variance model would make it a defect immediately. Stated,
not ruled on.

**(c) The published section was not regenerated.** TN §9a gates regeneration;
`gen_aif_dag.bb` was not run.

**Not claimed.** No live tick, no run lock, nothing written under `data/`. No
production source changed. No ruling; `:choices` is byte-unchanged. Legs 2 (name
discipline) and 3 (convergence ledger) remain unstarted.
