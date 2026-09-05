# C527 — :F8 leg 1, slice 9: the selected action u, stated in Lean

**Row:** `:F8` (`:loop-mode :one-slice-per-invocation`), leg 1 (Lean
completion): one Lean statement per class-(a) quantity. Slice 0 was the U35 join
refresh (C517), slice 1 Π (C518), slice 2 ε (C519), slice 3 μ-next (C520), slice
4 F_π (C521), the `:code` pointer sweep C522, slice 5 o (C523), slice 6 μ (C524),
slice 7 T (C525) and slice 8 τ (C526). **This slice's quantity is u
(`:action`, node R16)** — of the two rows slice 8 left, it is the one whose
`:code` names the functions that compute it; `:policy-set` still names none.

**Two seats wrote this slice.** The Lean modules as first delivered are
codex-9's, under Agency job `invoke-1788625574653-7968-de9a648a`; that packet
excluded the registry, the ledger and this report by instruction. The
independent production probe below, the review, the repairs it produced, the
registry declaration, the join re-run and this report are the reviewing seat's.

**The dispatch again did not borrow a roster id.** Sent `--from wm-build-loop`,
which is not on the roster; the packet said so and asked for no bellback, and
the job was polled to `done`. This seat is a `claude -p` one-shot under
`wm-build-loop.sh` and cannot park, so polling is its correct protocol.

## 1. The registry line, and what is wrong with it

`aif-equations.edn`, `:id :action`, node R16, `:class :theory-defined`:

    :formal "u_t := argmax_u sum_pi delta(u, pi_t) Q(pi)"
    :eq     "dacosta2020 eq. 11 (Bayesian-model-average argmax); friston2017 eq. 2.3 ..."

It names **one** argmax over **one** posterior. The machine has **three**
selection rules under that name, the default one never reads the posterior, the
two argmax implementations in the file break ties in opposite directions, and
the action that is *enacted* is computed by a fourth function that does not
consult the selected action at all.

## 2. The independent probe, run before codex-9 delivered

Every measurement in §3–§8 was produced twice: once by this seat calling
production `futon2.aif.policy` directly **before** codex-9's modules landed, and
once by the readback. The probe and its transcript are committed beside the
readback (`runs/F8-action/review-independent-probe.clj` and `.txt`).

The probe's fixture is a three-candidate ranked list in production shape —
`rank-actions` sorts by `:controller-score` **ascending**, so the head is the
lowest G:

| entry | `:controller-score` (G) | `:habit-prior-bias` (ln E) |
|---|---|---|
| `:alpha` | 0.0 | 0.0 |
| `:beta`  | 1.0 | 3.0 |
| `:no-op` | 5.0 | 0.0 |

τ is taken from the live arena default `:selection-gain-only` with gain 1.0
(slice 8, C526 §5), so τ = 1 and the scores are `−G + ln E`.

## 3. There is no single u: three rules, measured

`select-action` (`policy.clj:672-862`) branches on `:selection-boundary`, and
the `:actuation` branch branches again on whether any habit prior is nonzero:

| branch | rule | pointer | **measured on the fixture** |
|---|---|---|---|
| `:actuation`, all priors zero | `best (first ranked-actions)` — the G-head, no τ, no argmax | `policy.clj:809-813` | `:alpha` |
| `:actuation`, a nonzero prior | `(apply max-key scores (range (count scores)))` over `−G/τ + ln E` | `policy.clj:833-838` | `:beta` |
| `:strategic-recommendation` | `strategic-recommendation`, which dispatches again on `selection-law` | `policy.clj:538-593` | `:alpha` (head law) |

The first and second disagree on one shared input. The candidate list, the
temperature and the priors are identical; only the boundary differs.

## 4. The default law does not read the posterior it records

`selection-laws` is the closed set `#{:controller-head :full-score-posterior}`
(`policy.clj:503-518`) and `default-selection-law` is `:controller-head`
(`policy.clj:520-524`). Under it `chosen-idx = head-idx`, the index of the first
non-`:no-op` entry in the G-ordered list (`policy.clj:569-570`, `:593`), and the
docstring says the F_π-bearing posterior at `:softmax-weights` is **recorded and
never read**. Only `:full-score-posterior` argmaxes the posterior's own score
vector.

**Measured** on the fixture: head law selects `:alpha`; posterior law selects
`:beta`; the head law's decision nonetheless *records*
`:softmax-weights {alpha 0.119, beta 0.880, no-op 0.0008}` — a posterior whose
argmax is `:beta`, alongside a chosen action of `:alpha`.

## 5. The `:formal` line is the non-default arm, and that is provable

`softmax-weights` is `selection-scores` normalised (`policy.clj:215-236`), and
normalisation is strictly monotone, so `argmax_π Q(π) = argmax_π
selection-scores(π)`. The registry's Bayesian-model-average argmax therefore
**is** the `:full-score-posterior` rule — and is not the default rule.
**Measured:** scores `[0.0 2.0 −5.0]`, Q `[0.119 0.880 0.0008]`, argmax index 1
for both.

This is the analogue of slice 8's `variationalGammaIsInverseBeta` /
`engineeringGammaIsNotInverseBeta` pair: the citation describes an arm of a
dispatch, not the quantity the row names.

## 6. The sharpest: two argmaxes in one file that break ties oppositely

`first-argmax` (`policy.clj:525-536`) returns the **first** maximal index, and
its own docstring names the hazard:

> "(`max-key` would keep the LAST maximum, which would make a tie look like a
> law change.)"

One seam later, the `:actuation` habit-prior branch does exactly
`(apply max-key scores (range (count scores)))` (`policy.clj:838`), and
Clojure's `max-key` keeps the **last** maximum — its reduction advances on `>=`.

**Measured** at HEAD on the tied score vector `[0.0 7.0 7.0 1.0]`:
`first-argmax` → index **1**, `max-key` → index **2**.

And the disagreement reaches production actions, not just indices. On a
two-candidate tie (`:alpha` G 0 ln E 0, `:beta` G 1 ln E 1, scores
`[0.0 0.0 −9.0]`): the `:actuation` habit-prior branch selects **`:beta`** (the
last maximum) and the strategic `:full-score-posterior` law selects **`:alpha`**
(the first). The file states the hazard in a docstring and commits it one
definition later.

## 7. A requested law can become the head law — and the record says so

`full-score? (and (= :full-score-posterior selection-law) f-pi-entered?)`
(`policy.clj:582`): when F_π did not enter this tick, the law falls back to the
head it would otherwise have taken. So "the law is `:full-score-posterior`" does
not entail "u was selected by the posterior"; the pair (law, F_π-entered) does.

**This fallback is not silent, and the dispatch packet was wrong to call it
that.** Measured, the decision carries:

    :selection-law {:requested :full-score-posterior
                    :applied   :controller-head
                    :refusal   {:reason :no-f-pi-opts
                                :effect :fell-back-to-controller-head}
                    :moved-from-controller-head? false}

The machine records the substitution honestly. The statement to make is the
conditional one — the law name alone does not determine the rule — not a claim
of concealment. Same shape as slice 8's mode/β-source pair, with the opposite
verdict on the record's honesty.

## 8. The enacted action is not the selected action

`close-loop!` (`enact.clj:294-339`) runs act gates over
`(:ranked-actions judgement)` (`:305`) and enacts
`(first (filter #(= :pass (:verdict %)) gates))` (`:320`) — the first
gate-**passing** entry in G-rank order. **`enact.clj` contains no reference to
the judgement's `:action` at any line** (checked by grep over the whole file:
zero hits). The selected u is a recorded quantity that the enactment path never
reads, which is C448 5b stated as a property of the code rather than as a note.

## 9. The review was a real gate and found six things

All six were fixed by this seat rather than re-dispatched (mathlib4
`4617204890`). Codex-9's own gate claim was verified first, not taken: both
modules were read in full and `lake env lean` re-run on each at the delivered
sha `c2ab8bb42b` — exit 0, 0 errors, 0 warnings, 0 `sorryAx` — before any
repair.

**(a) The sharpest: `fullScoreIsPosteriorArgmax` was a tautology on two
numerals.** As delivered it read

    theorem fullScoreIsPosteriorArgmax :
        (if Real.exp 0 < Real.exp 2 then (1 : Nat) else 0) = 1 := by norm_num

with a docstring claiming "exponentiation and positive normalization preserve
its unique score maximum". Nothing about argmax, normalisation or the machine is
in that statement; the claim lived entirely in the declaration's *name*. This is
the **fifth consecutive slice** whose review found a claim asserted by a name and
not by a statement (slice 5's five repairs, slice 6's four theorems with no
conjunction, slice 7's `gTermsDisagreeOnDepth` gap, slice 8's undeclared three
laws). Replaced by `selectionScore`, `posteriorWeight` and
`posteriorOrderIsScoreOrder` — the real content, §5 — with
`fullScoreIsPosteriorArgmax` now stating it on the production fixture.

**(b) The production defaults were nowhere in the module.** `StrategicLaw` and
`SelectionBoundary` had two constructors each and nothing said which one runs.
The central fact about u — that by default it is the G-head and the recorded
posterior is not read — was therefore unstatable. Added
`productionDefaultBoundary` (`policy.clj:753`), `productionDefaultLaw`
(`policy.clj:520-524`), `registryLaw`, `defaultRuleIsNotTheRegistryRule` and
`defaultSelectionIsTheHead`. This is slice 8's `temperatureDefaultsDisagree`
lesson applied: a theorem tied to no named machine constant says nothing about
the machine.

**(c) `strategicAndActuationDisagree` exhibited only the tie rule.** Its fixture
is a tie, so it re-proved §6 under a §3 name and the habit prior's own effect
went unstated. Added `habitPriorAloneMovesTheChoice` on a **strict** maximum.

**(d) `noOpCandidateSetsDiffer` compared two lists, not two decisions.** The
machine consequence — that the actuation branch's argmax lands on `:no-op` and
the abstain test converts it, while the strategic boundary selects an action —
was missing, and with it the fact that **u can be `:abstain`, which is not in the
range of the registry's argmax at all**. Added `abstains`,
`noOpArgmaxAbstains` and `noOpExclusionMovesTheChoice`.

**(e) The fallback was called silent, and it is not.** §7. The dispatch packet
said so and codex-9 wrote the docstring from it; the probe measured the
`:refusal` map and the docstring is corrected. Added
`requestedPosteriorDependsOnFPiEntered`, which is the claim that survives.

**(f) Four docstring pointers were wrong**: `503-520` truncated
`default-selection-law`; `823-839` pointed at the no-prior branch rather than
the `max-key` line at `838`; `672-839` cut `select-action` short of `862`;
`enact.clj:294-316` excluded the first-passing-gate line at `320`. All four
re-derived at HEAD and corrected.

**And the readback as delivered measured five values with both of its
disagreement lines one-sided** — it ran the last-max arm of the tie and the
strategic arm of the no-op, and never the opposites, so each line was named
after a two-sided theorem and measured one side. Replaced by one that measures
**both arms of every disagreement**: 21 lines, 0 `MISMATCH`, deterministic.
While rewriting it, the readback caught an error of this seat's own — a
`defaultSelectionIsTheHead` line run on the prior-bearing fixture, i.e. against
the theorem's hypothesis — which is the gate working in the direction it was
built for.

## 10. The join

`bb lean_state_probe.bb` resolves `machineAction` and the equation join moves
**16 of 18 carriers declared / 16 resolving → 17 of 18 / 17**,
`:carrier-declared-but-not-found []`, at `DarkTower/WarMachine/MachineAction.lean:49`,
kind `def`. `:no-carrier-declared` goes from `[:policy-set :action]` to
`[:policy-set]`. Corpus: **89 modules** (87 before), 1077 declarations, 14650
source lines; **89/89 modules elaborate exit 0, 0 error diagnostics**; sorry 10
source / 10 Lean-reported (exact-by-location) and axioms 0 — unchanged, these
modules add neither.

**One class-(a) row remains: `:policy-set`.** It is the harder one: it carries
no `:code` key at all, so there is no production function to state.

Worth noting beside §8: `Holes.lean:7676` already carries
`def enactedEqualsSelectedWhenRankOneGated : Prop := sorry` — a contract hole
about exactly when selection and enactment *do* coincide. This slice states the
disagreement; that hole stays open and is untouched.

**No ledger supersession was needed, and the absence was established rather than
assumed:** all 52 rows carrying a collection `:covers-key` were enumerated, and
the only one naming `[:equations {:id :action}]` is `:F8` itself, which is
`:open` and unsigned. **`:F8`'s `:covers-key` is unchanged** — the C522 `:code`
sweep had already put `:action` on it.

## 11. Gates, bare exits

- `lake env lean` on `MachineAction.lean` and `MachineActionWitness.lean` —
  exit 0, 0 errors, 0 warnings each, at the repaired sha (and re-run at the
  delivered sha before repair, also 0/0).
- `lake build DarkTower.WarMachine.MachineActionWitness` — exit 0, 8498/8498.
- `#print axioms` on **all 33 declarations**, enumerated from the sources
  (codex-9 delivered 19) — 25 axiom-free, 1 `[propext]`, 7
  `[propext, Classical.choice, Quot.sound]`. **0 `sorryAx`**, 0 `sorry` tokens.
  Every declared name is printed and no printed name is undeclared, checked by
  `comm` over two generated lists, empty in both directions.
- The registry edit verified key by key against `git show HEAD:` read as EDN:
  **one** row changed, **four** keys added, none removed, no value moved on any
  existing key, row order preserved, no top-level key other than `:equations`
  touched — `:choices` and `:references` byte-unchanged.
- The readback — exit 0, 0 `MISMATCH` on 21 lines, run twice with identical
  `sha256` `d7858f8958497bbadeb134f11724693370580bb0222426a2c5ee6c9a779a41e5`.
- `clj-kondo` — 0 errors, 0 warnings on the readback and on the review probe.
- `check-parens.el` — OK on the readback, the probe, `aif-equations.edn` and
  `worklist.edn`.
- `bb p4ng/empirics-futon/pointer_check.bb` — **1632 pointers in 3 files, 0
  unresolved** (1613 before).
- `bash p4ng/empirics-futon/negative_controls.sh` — **PASS, 88 negative / 45
  positive**, shared registries untouched.

## 12. Found and not repaired

**(a) `policy.clj` and `enact.clj` are untouched, on purpose.** The three rules
under one name (§3), the recorded-but-unread posterior (§4), the opposite
tie-breaks (§6) and the severed selection-to-enactment channel (§8) are *stated*
here and left standing. Changing any of them is a change to the machine, and
this is a leg-1 "state it in Lean" slice; they are findings for an I-track item,
not fixes smuggled under this row.

**(b) The row's `:note` pointer has drifted, and is left byte-unchanged rather
than moved.** It cites `enact.clj:287-316`; at HEAD `close-loop!` is `294-339`
and the first-passing-gate line is `320`, outside the cited range. Stated in
`:lean-note`, not repaired — moving it would be a fifth key changed on a row
this slice is only declaring a carrier on.

**(c) `selectedAndEnactedDisagree` is not measured against production.**
`close-loop!` is the mutating enactment boundary and the readback does not call
it; the claim rests on the source at `enact.clj:305,320` and on the grep showing
`enact.clj` contains no reference to the judgement's `:action` (0 hits). The
readback carries its own `NOT MEASURED` line saying exactly this.

**(d) The published section was not regenerated.** TN §9a gates regeneration;
`gen_aif_dag.bb` was not run.

**Not claimed.** No live tick, no run lock, nothing written under `data/`. No
production source changed. No ruling; `:choices` is byte-unchanged. Legs 2 (name
discipline) and 3 (convergence ledger) remain unstarted.
