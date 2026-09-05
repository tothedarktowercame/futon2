# C523 — :F8 leg 1, slice 5: the structured observation o, stated in Lean

**Row:** `:F8` (`:loop-mode :one-slice-per-invocation`), leg 1 (Lean
completion): one Lean statement per class-(a) quantity. Slice 0 was the U35 join
refresh (C517), slice 1 was Π (C518), slice 2 was ε (C519), slice 3 was μ-next
(C520), slice 4 was F_π (C521), and the `:code` pointer sweep between them was
C522. **This slice's quantity is o (`:observe`, node R2)** — the first of the
six remaining class-(a) rows, and the first that carried no `:lean` key at all
rather than an explicit `:lean-status :missing`.

**Two seats wrote this slice.** The two Lean modules as first delivered and the
readback script are codex-9's, under Agency job
`invoke-1788620259563-7937-0a78a5d3` (mathlib4 `169662b196`, futon2
`402b8cfdfe`); that packet excluded the registry, the ledger and this report by
instruction. The review below, the five Lean repairs it produced (mathlib4
`a4c2276d51`), the two readback measurements those repairs made available, the
registry declaration, the join re-run and this report are the reviewing seat's.

**Commits.** mathlib4 `169662b196` (the modules as delivered) + mathlib4
`a4c2276d51` (the review repairs) + futon2 `402b8cfdfe` (the readback) + the
futon2 commit that ADDS this file (`git log --diff-filter=A` on this path
resolves it; no slice may write "[this commit]" again — slice 2 did and made its
own ledger entry unreadable).

## 1. Why this slice is not shaped like slices 1–4

`:observe` is `:class :stack-defined` and its `:formal` line is

    o_t <- structured observation of the world after action u_{t-1}

which is a boundary statement. There is no equation to transcribe and no
reference value to derive. Slices 1–3 measured production against exact
rationals and slice 4 against a transcendental factor held symbolic; none of
that applies.

What there is instead is a **construction** — `observe`
(`futon2:src/futon2/aif/observation.clj:103-146`) maps raw scan data to a fixed
fourteen-channel vector — and a set of **claims the construction makes about
itself**, in its own docstrings and in the tagged envelope that travels beside
it. The slice states the construction, and then states which of those claims the
implementation keeps. One does; two do not.

The dossier had asked for exactly this. `PROBLEMS-r2-r8-r7-batch2.md:44-48`
records the gap as "a Lean observation carrier plus a mirror check that the
runtime channel set, bounds, and absence variants inhabit it". The channel set
inhabits it. The bounds and one absence variant do not, and that is the finding.

## 2. What already existed in Lean, and what this slice did not redeclare

`Holes.lean` already carries `Channel` (`:1105-1110`), `Channel.all`
(`:1112-1116`) and `ObservationVector` (`:6811-6812`), and
`ObservationVectorWitness.lean` a fourteen-coordinate fixture. So a *carrier*
existed and the registry row still read "not in Lean", because nothing stated
how the machine **builds** one or what travels beside it. Both modules import
`Holes` and build on those declarations; no second observation type was
declared.

## 3. The [0,1] bound is asserted in four places and enforced at two channels

`observation.clj` states the range at the namespace level ("a 14-channel
observation vector in [0,1]", `:5-7`), on every one of the fourteen channel
lines (`[0,1]`, `:18-32`), and in `observe`'s own docstring ("Returns a map of
channel-id → [0,1] value", `:105-106`).

In the body, two channels are clamped, both from above only: `:sorry-count-norm`
by `(min 1.0 …)` at `:133`, `:coupling-density` by `(min 1.0 …)` at `:136-137`.
No channel is clamped from below. Measured at futon2 HEAD:

| input | channel | value |
|---|---|---|
| `{:graph {:dynamics {:commit-percentages {:stack 70.0}}}}` | `:stack-pct` | `70.0` |
| `{:loop-health {:overall -3.0}}` | `:loop-health` | `-3.0` |
| `{:graph {:summary {:active-repos 20 :total-repos 4}}}` | `:active-repo-ratio` | `5.0` |

The third is the one that matters most, because it is not a pass-through:
`observe` **computes** that quotient at `:129-132` and never checks the
denominator against the numerator.

So `ObservationVector.value : Channel → ℝ` is the honest carrier and was left
alone. `BoundedObservation` is a **predicate** an observation may or may not
satisfy; `clampedChannelsAreBoundedAbove` proves the upper bound holds at the
two clamped channels *for every raw reading*; `stackPctIsNotClamped` shows the
same construction passes the other twelve straight through; and the three
measured values above are the three counterexamples. A `[0,1]`-refined carrier
would have stated a guarantee the machine does not make.

## 4. The absence envelope promises a coercion one channel cannot perform

`channel-statuses` writes `{:variant :absent :reason :source-field-missing
:paths [...] :coerced-to 0.0}` at every absent channel, unconditionally
(`:69-73`). For thirteen channels that is what happens. For `:active-repo-ratio`
it is not: its requirement paths include `[:graph :summary :total-repos]`
(`:57-58`), but `observe` reads `(:total-repos summary)` at `:129` **with no
default**, so `(pos? nil)` raises.

    (obs/observe {:graph {:summary {:active-repos 3}}})
    ;; java.lang.NullPointerException at futon2.aif.observation/observe
    ;;   (observation.clj:129)

**The evidence that this is an oversight and not a design is one character
wide.** `:coupling-density` reads the *same key* at `:134` as
`(:total-repos summary 0)`, with a default, and coerces to `0.0` exactly as the
envelope promises. In Lean the two are `activeRepoReading` and
`couplingDensityReading`, and the promise is separated from its keeping:
`EnvelopePromises` holds as written, `PromiseKept` fails, and
`incompleteActiveRepoSummaryBreaksTheCoercionPromise` is the conjunction.

**And the coupling side is not observable through `observe` at all.** The input
that would exercise `:coupling-density`'s default raises at `:active-repo-ratio`
first, so one channel's missing default makes the other thirteen coordinates
unobservable on that input. The readback records that refusal instead of a
value, and says in its header that it is doing so.

`(obs/observe {})` does **not** throw: `summary` is `nil` there and the
`(and summary …)` guard short-circuits. The crash needs a `:summary` map that is
present and incomplete, which is why the six existing tests do not see it —
their `sample-data` carries a complete summary
(`observation_test.clj:31-35`) and their empty case carries no `:graph` at all
(`:95-99`).

## 5. The two claims that do hold

**Totality.** `observe` returns a literal fourteen-key map (`:120-145`), so no
channel is ever missing and `(observe {})` gives `0.0` at all fourteen.
`machineObservation` is total over `Channel` by construction, so what is
measurable is the count: `emptyObservationHasFourteenZeros` against the
readback's fourteen coordinates at delta 0.0.

**The guarded vector boundary.** `sense->vector` refuses to produce the ordered
numeric vector unless the caller hands it the exact matching envelope
(`:148-158`); that refusal is what keeps absence provenance attached where EDN
metadata would drop it. In Lean it is a **signature, not a theorem**:
`senseToVector` takes the matching proof as an argument, so the vector is
unconstructible without it.

## 6. The review, and the five repairs it made

Author ≠ reviewer is the point of the handoff, and the review found five things.
All were fixed by the reviewing seat rather than re-dispatched (mathlib4
`a4c2276d51`).

**(a) The sharpest one: `EnvelopeMatches` compared the wrong thing, and it was
the wrong thing at exactly the case under measurement.** The first draft defined
it as equality of the numeric coordinates. But `sense->vector` recomputes the
whole tagged envelope and compares it entire (`:152-156`), and **the refusal the
readback exhibits is a variant mismatch at identical coordinates**: the empty
observation's envelope and the envelope of `{:loop-health {:overall 0.0}}` agree
at all fourteen numeric coordinates — both are `0.0` everywhere — and differ
only in `:loop-health`'s variant (`:absent` with `:reason :source-field-missing`
against `:observed`). Measured: `:values-equal true`, `:envelopes-equal false`,
`:differing-channels [:loop-health]`. A value-only test would have **accepted**
the pair the implementation refuses, so the module would have stated a boundary
the machine does not run while the readback beside it printed a green refusal.
`EnvelopeMatches` now compares variant and value together, and
`readbackRefusalPairAgreesNumerically` / `readbackRefusalPairIsRefused` are the
two halves.

**(b) `senseToVectorRequiresMatchingEnvelope` was `P → P`.** Its conclusion was
`envelope.value = o.value`, `EnvelopeMatches` unfolds to exactly that, and its
proof was the hypothesis `h`. This is the same shape as
`successfulTotalIsFinite`, which the slice-4 review deleted at mathlib4
`d7a45a358a` — and the dispatch packet named that commit and asked for it to be
avoided. Deleted; the content is in `senseToVector`'s signature and the header
now says so.

**(c) `CoercionPromise` was declared and never used.** Finding §4 was encoded as
`incompleteActiveRepoSummaryDoesNotEstablishPromise : ¬ (isSome (some a) ∧
isSome none)` — a fact about `Option` that mentions no channel, no observation
and no coercion, true of any two options in that shape. The module had the
pieces for its own centrepiece finding and did not join them. §4's theorems are
the repair.

**(d) `upperClamp` was tied to no channel.** `upperClamp x ≤ 1` is true of every
`x` and says nothing about *which* channels are clamped, so the by-construction
half of the `[0,1]` claim was not stated. `clampedChannels` names the two,
`clampAtClamped` is `observe`'s shape, and `clampedChannelsAreBoundedAbove` is
the statement.

**(e) `sorryAtCap` and `couplingAtCap` were the same proposition** (`upperClamp
1 = 1`) under two names, so four cap witnesses were three. Collapsed to
`clampAtCap`, `clampAboveCapTwo`, `clampAboveCapFive`.

One thing was checked and left: `emptyObservationHasFourteenZeros`'s first
conjunct was true by definition of `machineObservation`. It was restated as the
`Channel.all`-order list, where the fourteen is the content.

## 7. What the reviewing seat checked, so the review is auditable

- Read both Lean modules and the readback script in full at `169662b196` /
  `402b8cfdfe`, and re-derived each of the three dispatched findings against
  futon2 HEAD independently before accepting codex-9's report that they
  reproduced.
- Re-ran the readback twice: `sha256` of `runs/F8-observe/clojure-readback.txt`
  identical across runs
  (`7d6e5528e8d6e1d824403519b485dee876b68021f0666a0fb537473f4cef19f8`).
- Re-ran `clojure -X:test :nses '[futon2.aif.observation-test]'` **before** any
  change (6 tests / 30 assertions / 0 failures / 0 errors) and after, so the
  green count is a baseline and not just an outcome. codex-9's packet asked for
  `-M:test -n`, which this repo's `:exec-fn` alias treats as a filename; that
  was the dispatcher's error and codex-9 reported both exits rather than
  reporting the failing one as a test failure.
- Verified the registry edit key by key against `git show HEAD:` read as EDN:
  **one row changed, four keys added, none removed, no key's value moved on any
  other row, row order preserved, and no top-level key other than `:equations`
  touched** — so `:choices` and `:references` are byte-unchanged.
- Ran the join probe before and after the declaration, so the movement is
  measured rather than asserted.

## 8. The join, and where leg 1 stands

`bb lean_state_probe.bb --no-typecheck --out
runs/F8-observe/lean-state-join-check.edn` resolves `machineObservation` and the
equation join moves **12 of 18 carriers declared / 12 resolving → 13 of 18
declared / 13 resolving**, `:carrier-declared-but-not-found []`. Corpus: 81
modules, 921 declarations, 13423 source lines; sorry 10 source / axioms 0,
unchanged — these modules add neither. **Five class-(a) rows remain:**
`:belief-state :policy-set :depth :temperature :action`.

No ledger supersession was needed: no `:done`, non-superseded row's
`:covers-key` names `[:equations {:id :observe}]`, so no signature went stale.

## 9. Gates

- `lake env lean` on `MachineObservation.lean` and
  `MachineObservationWitness.lean` — exit 0, 0 errors, 0 warnings each.
- `lake build DarkTower.WarMachine.MachineObservationWitness` — exit 0,
  2706/2706.
- `#print axioms` on **all 42 declarations** of the two modules — exit 0,
  `[propext, Classical.choice, Quot.sound]` throughout, **0 `sorryAx`**.
- `clj-kondo --lint f8_observation_readback.clj` — 0 errors, 0 warnings.
- `check-parens` (`futon4/dev/check-parens.el`) on the readback script and on
  `aif-equations.edn` and `worklist.edn` — OK, exit 0.
- `clojure -X:test :nses '[futon2.aif.observation-test]'` — 6 tests, 30
  assertions, 0 failures, 0 errors, before and after.
- `bb p4ng/empirics-futon/pointer_check.bb` — 1580 pointers in 3 files, 0
  unresolved, exit 0 (1577 before this slice; the three added are in the new
  `:lean-note`).
- `bash p4ng/empirics-futon/negative_controls.sh` — PASS, 88 negative / 45
  positive, shared registries untouched, exit 0.
- `bb worklist_check.bb` and `bb ancestry_check.bb` — run after the ledger
  commit; see the row's `:progress`.
- The readback itself — exit 0, deterministic, every numeric delta `0.0`.

## 10. Found and not repaired

**(a) `observation.clj` is untouched, on purpose.** The `[0,1]` gap of §3 and
the `:active-repo-ratio` crash of §4 are *stated* here and left standing.
Repairing either is a change to the machine, and this is a leg-1 "state it in
Lean" slice; they are findings for an I-track item, not fixes to be smuggled
under this row. The crash is the more urgent of the two: it is a
`NullPointerException` on a shape of scan data — a `:summary` map present but
incomplete — that no test constructs and nothing upstream forbids.

**(b) The bounds question the Lean does not settle.** `BoundedObservation` is
now a predicate with three counterexamples, but nothing here says whether the
predicate *should* hold — whether the fix is to clamp, to widen the documented
range, or to leave the channels unbounded and delete the claim. That is a
ruling and no ruling is made here; `:choices` is byte-unchanged.

**(c) The R2 dossier's other item is untouched.** `PROBLEMS-r2-r8-r7-batch2.md`
`:36-41` records that the operator's turns are persisted and never read into the
observation vector, and that both a model extension and an implementation are
required. This slice states what the observation *is*; it says nothing about
what it should include.

**(d) The published section is five rows behind and no generator was run**
(TN §9a gates regeneration; `gen_aif_dag.bb` was not run and nothing was
regenerated into a publish).

**Not claimed.** No live tick, no run lock, nothing written under `data/`. No
ruling. No production source changed. The five remaining class-(a) quantities
(`:belief-state`, `:policy-set`, `:depth`, `:temperature`, `:action`) are
untouched. Leg 2 (name discipline) and leg 3 (convergence ledger) remain
unstarted.
