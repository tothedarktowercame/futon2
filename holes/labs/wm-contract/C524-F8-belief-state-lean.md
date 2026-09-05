# C524 — :F8 leg 1, slice 6: the stored belief μ, stated in Lean

**Row:** `:F8` (`:loop-mode :one-slice-per-invocation`), leg 1 (Lean
completion): one Lean statement per class-(a) quantity. Slice 0 was the U35
join refresh (C517), slice 1 Π (C518), slice 2 ε (C519), slice 3 μ-next (C520),
slice 4 F_π (C521), the `:code` pointer sweep C522, and slice 5 o (C523).
**This slice's quantity is μ (`:belief-state`, node R1)** — the second of the
six class-(a) rows that carried no `:lean` key at all, and the last of the
belief-side ones.

**Two seats wrote this slice.** The two Lean modules as first delivered and the
readback are codex-9's, under Agency job
`invoke-1788621630060-7942-3ed37bc8` (mathlib4 `290ce8ae9f`, futon2
`5a553275`); that packet excluded the registry, the ledger and this report by
instruction. The independent production probe below, the review, the five Lean
repairs it produced (mathlib4 `3a8e26f61e`), the readback repair, the registry
declaration, the join re-run and this report are the reviewing seat's.

## 1. Why this slice is not shaped like slices 1–4

`:belief-state` is `:class :stack-defined` and its `:formal` line is

    mu_t := the stored belief after the update at t-1

which is a boundary statement. There is no equation to transcribe and no
reference value to derive from one. As in slice 5, what there is instead is a
**construction** — the belief the machine stores and carries between ticks —
and a set of **claims made about it**, here in docstrings and in the contract
document. The slice states the construction and then states which of those
claims the implementation keeps. One holds; two do not.

## 2. The independent probe, run before codex-9 reported

Every measurement in §3–§6 was produced twice: once by this seat calling
production `futon2.aif.belief` directly **before** codex-9 reported, and once by
codex-9's readback. The probe and its transcript are committed beside the
readback (`runs/F8-belief-state/review-independent-probe.clj` and `.txt`) so the
two derivations can be compared rather than taken on trust. They agree on every
case.

## 3. What the machine actually stores

A map `entity-id → normalised categorical over a SEVEN-member status set`:

- `belief.clj:36-41` — `status-set`, exactly seven: `:spawned :refined
  :strengthened :addressed :falsified :foreclosed :reopened`.
- `belief.clj:44-49` — `uniform-prior`, `1/7` at each. Measured:
  `0.14285714285714285` at all seven, max delta from Lean `0.0`.
- `belief.clj:62-66` — `initial-belief-state`.

This is **not** the carrier the glossary declares. `Holes.lean:6806-6808`
declares `structure BeliefState where mean : Channel → ℝ; variance : Channel →
NonnegativeReal` — moments over the **fourteen observation channels** — and
`beliefUpdate` (`Holes.lean:6839-6865`) constrains it. Production never writes
that structure. `statusIndexDiffersFromChannelIndex` states that the two index
sets are not the same set relabelled, and its docstring says what it does **not**
show: that no map between the two carriers exists is a claim about a corpus,
which Lean cannot settle.

## 4. The `:formal` line is false as written, and the counterexample is exhibited

`mu_t` is not "the stored belief after the update at `t-1`". It is

    reconcile-belief-carry(fresh_t, mu-post_{t-1})            belief.clj:510-522

reached in production at `war_machine.clj:6019-6024` from `(:mu-post
prev-trace-record)`, where `fresh_t` is `bootstrap-from-stack-annotations` over
this tick's entity domain, guarded by `*carry-belief?*` (`belief.clj:995-1001`,
default `true`). The write side is `trace.clj:542` (`:mu-post (:belief
judge-output)`), so the carry is a closed tick-to-tick loop.

The function iterates over `fresh-bootstrap`'s entries, so the result's domain
is **exactly** this tick's bootstrap domain:

| | Lean | measured |
|---|---|---|
| survivor keeps the carried posterior | `survivorKeepsCarried` | `true` |
| new entity gets the fresh uniform prior | `newEntityKeepsFresh` | `true` |
| carried-only entity is dropped | `carriedOnlyEntityIsDropped` | present `false` |
| result equals `fresh` | `carryEqualsNeitherInput` | `false` |
| result equals `carried` | `carryEqualsNeitherInput` | `false` |
| empty carry returns `fresh` | `coldStartReturnsFresh` | `true` |

The result equalling **neither** input is what makes the `:formal` line false
rather than merely imprecise.

`trace.clj:810-825` returns records in chronological order, so
`war_machine.clj:6022`'s `peek` really is the newest record. Checked; not a
defect, and not reported as one.

## 5. The sharpest consequence: re-entry loses history

An entity present at t0 with a peaked posterior, **absent from the bootstrap
domain at t1**, and present again at t2 comes back at the **uniform prior**. The
carry runs along a *continuously present* entity only, because the sole thing
the next tick reads is the immediately preceding record's `:mu-post`. One tick
out of the entity domain erases that entity's belief history irrecoverably —
there is no path by which t2 can reach t0's posterior.

`reentryLosesHistory` proves the three-tick composition; the readback and the
independent probe measure the same three ticks (`t0-peaked=true t1-absent=true
t2-uniform=true`, and t2's seven coordinates all `0.14285714285714285`).

## 6. The R1 criterion, and the (argmax, entropy) collision

`docs/futon-aif-completeness.md:49-68` states the criterion: a belief
distribution "carried across ticks, with mean *and* precision (variance) both
explicitly represented", with the operational check "Verify it has mean +
variance fields per state dimension". The same document at `:63-64` answers it
by offering `most-likely-status` as the "mean" and `entropy` as the
"precision", and records R1 satisfied as of v0.2.

The stored carrier has neither field. It holds the whole categorical;
`most-likely-status` (`belief.clj:447-452`, the argmax) and `entropy`
(`belief.clj:454-462`) are derived on demand. **No ruling is made** on whether
that satisfies the criterion — that is Joe's, and `:choices` is byte-unchanged.

What is provable is stated: `momentPairIsNotSufficient`. Two **distinct**
normalised posteriors over the seven statuses share the same strict argmax and
the same entropy, so the pair `(most-likely-status, entropy)` does not determine
the posterior and is not a sufficient statistic for it the way a genuine
mean/variance pair would be. Measured through production on both members:

    argmax        :spawned / :spawned
    entropy       1.0296530140645737 / 1.0296530140645737, A−B delta 0.0
    mass          1.0 / 1.0
    distinct      true

The strictness is not decoration. `most-likely-status` is `(key (apply max-key
val posterior))`, whose value on a tie is whichever entry `max-key` reaches
first; a non-strict maximum would not force the `:spawned` the readback
measures.

## 7. The claim that holds

`reconcile-belief-carry` performs **no arithmetic** — every value in its result
is a value of one of its two inputs — so it preserves normalisation exactly
(`carryPreservesNormalisation`) and cannot manufacture a degenerate posterior.
codex-9 stated the matching limit rather than burying it: it also does not
*repair* a degenerate input.

## 8. The review, and the five repairs it produced

All five were fixed by this seat (mathlib4 `3a8e26f61e`) rather than
re-dispatched.

**The sharpest.** The collision of §6 existed only as four separate theorems —
`collisionDistinct`, `collisionSpawnedIsArgmax`, `collisionSameEntropy` and a
reference value — and **nothing stated their conjunction**. A reader could check
every piece and never find, anywhere in the file, the claim the slice exists to
make. This is the third consecutive slice whose review found a finding *defined
but not stated*: slice 4 had a vacuous finiteness theorem, slice 5 had
`CoercionPromise` declared and never used, and this is the same shape one level
up.

**`Normalised` was never discharged.** The carrier module defines `Normalised`
and proves `carryPreservesNormalisation`, but no concrete posterior in either
module was ever shown to satisfy it. Two consequences: the preservation
theorem's hypotheses were met by nothing in the development, and the two
counterexamples of §6 were never shown to be *distributions*, so the collision
was a fact about two arbitrary functions. `uniformPrior`, `peaked`, `collisionA`
and `collisionB` now carry their proofs, and the readback measures both masses.

**The argmax was non-strict**, as above.

**`carriedOnlyEntityIsDropped` had an unused hypothesis** (`_hc : carried e =
some posterior`), so the theorem proved `fresh e = none → result e = none` for
*any* `carried` — true, but not about a carried-only entity, which is what the
name says. The conclusion now carries `carried e ≠ none` and the hypothesis is
used.

**The Status/Channel mismatch lived only in a docstring.**
`statusIndexDiffersFromChannelIndex` states it.

**The readback did not measure its own finding.** It compared each collision
entropy to the Lean reference and left A = B to be inferred. It now prints the
A−B difference, the distinctness and both masses.

## 9. A lab record contradicted by the code, stated and not repaired

`FUNDAMENTALS.edn`'s `:fundamental/belief-to-state-distribution` reads, at
`belief.clj:640`:

> COUNTED across futon2 src/, scripts/, checks/, test/ and holes/: 36
> references in test/…, 10 in prose … and 2 in comments inside
> scripts/futon2/report/war_machine.clj (:3789, :6170). **NO caller.** The
> nearest thing the runtime has to this map is dead code.

and its `:basis` says "the belief does not reach the prediction at all".

At HEAD the four `predict-*` functions **are** called. `predict-observation`
(`belief.clj:1199-1226`) calls all four at `:1217-1220`, and
`war_machine.clj:6099-6105` calls `predict-observation` in its three-arity form
**inside the tick's inner loop**; its output is what `fe/channel-prediction-error`
compares against the observation at `:6111-6113`. The call site dates from
`cbab6051` (2026-05-30), so this is a grep that missed the wrapper, not a change
in the machine. The two `war_machine.clj` sites the entry did find (`:3789`,
`:6170`) are indeed comments.

**The `:verdict :proxy-only` is unaffected** — moment pairs over the 8 of 14
channels in `channels-with-likelihood` are still not a distribution over the
model's `State` — and the entry's reading of `forward_model.clj:333` is correct:
`predict-effects` is called there with a `nil` state. Only the sub-claim is
false. That entry carries `:worklist-row :F1`, not `:F8`, so this slice states
the contradiction with its pointers and leaves the file alone.

## 10. The join, and where leg 1 stands

`bb lean_state_probe.bb --no-typecheck` resolves `machineBeliefState` at
`MachineBeliefState.lean:25` and the equation join moves **13 of 18 carriers
declared / 13 resolving → 14 of 18 declared / 14 resolving**,
`:carrier-declared-but-not-found []`. Corpus: 83 modules, 956 declarations,
13619 source lines; sorry 10 source / axioms 0, unchanged — these modules add
neither. Before and after are committed as
`runs/F8-belief-state/lean-state-join-{before,after}.edn`. **Four class-(a) rows
remain:** `:policy-set :depth :temperature :action`.

No ledger supersession was needed. All 52 rows carrying a collection
`:covers-key` were scanned and **none** — `:done`, `:open` or superseded — names
`[:equations {:id :belief-state}]`, so no signature went stale.

## 11. Gates, bare exits

- `lake env lean` on `MachineBeliefState.lean` and
  `MachineBeliefStateWitness.lean` — exit 0, 0 errors, 0 warnings each, at the
  repaired sha.
- `lake build DarkTower.WarMachine.MachineBeliefStateWitness` — exit 0,
  2706/2706.
- `#print axioms` on **all 35 declarations**, enumerated from the sources rather
  than taken from the report — 31 give `[propext, Classical.choice, Quot.sound]`,
  `statusIndexDiffersFromChannelIndex` gives `[propext]`, and `Status`,
  `Status.all` and `Entity` depend on no axioms. **0 `sorryAx`.** (codex-9
  reported 30 on its delivered state; 30 + 6 new − 1 dropped = 35.)
- `clj-kondo --lint f8_belief_state_readback.clj` — 0 errors, 0 warnings.
- `check-parens.el` — OK on the readback, `aif-equations.edn` and
  `worklist.edn`.
- `clojure -X:test :nses '[futon2.aif.belief-test]'` — 78 tests / 2030
  assertions / 0 failures / 0 errors, **before any change and after**.
- `bb p4ng/empirics-futon/pointer_check.bb` — 0 unresolved.
- `bash p4ng/empirics-futon/negative_controls.sh` — PASS, shared registries
  untouched.
- The readback — exit 0, run twice, identical sha256
  `44034da92bbd0d63e5850a6cf5d657e0f4d6dc6bdfcf98f57f4dc9c2a2cdd51b`.

The registry edit was verified key by key against `git show HEAD:` read as EDN:
**one row changed, four keys added, none removed, no key's value moved on any
other row, row order preserved, and no top-level key other than `:equations`
touched** — so `:choices` and `:references` are byte-unchanged.

## 12. Found and not repaired

**(a) `belief.clj` and `war_machine.clj` are untouched, on purpose.** The
domain-restricted carry of §4 and the re-entry amnesia of §5 are *stated* here
and left standing. Changing either is a change to the machine, and this is a
leg-1 "state it in Lean" slice; they are findings for an I-track item, not fixes
smuggled under this row.

**(b) `normalise` returns the uniform prior on a zero-sum posterior**
(`belief.clj:51-60`) rather than raising — a floor that silently replaces a
degenerate posterior with a maximally uncertain one. Stated, not ruled on.

**(c) `FUNDAMENTALS.edn` is not edited** — see §9.

**(d) The published section is six rows behind and no generator was run.**
TN §9a gates regeneration; `gen_aif_dag.bb` was not run and nothing was
regenerated into a publish.

**Not claimed.** No live tick, no run lock, nothing written under `data/`. No
production source changed. No ruling. The four remaining class-(a) quantities
(`:policy-set`, `:depth`, `:temperature`, `:action`) are untouched, and legs 2
(name discipline) and 3 (convergence ledger) remain unstarted.
