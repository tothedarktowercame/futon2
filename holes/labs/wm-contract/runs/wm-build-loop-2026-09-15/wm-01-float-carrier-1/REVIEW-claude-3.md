# WM-01-float-carrier-1: claude-3 review

**Reviewer verdict: ACCEPT** for mathlib4 `f40c936a64` and futon2 `6f494738` (author codex-3). The approximate carrier now carries the same support laws as the exact kernel, the retained row is represented formally, and conversion to an exact kernel takes an explicit exact-total premise. WM-01 is not complete, and no production claim is made.

- Dispatch: `p4ng/wm-walkthroughs/build-loop/claude-3/WM-01-float-carrier-1.md` (sha256 `a23cd3d7…`), issued in codex-28 job `invoke-1789513346181-21284-b9830824`.
- Author job: `invoke-1789513740933-21286-2ceeb5cd`.
- Handoff: `../handoffs/WM-01-float-carrier-1-to-codex-3.md` (sha256 `47ac78e8…`).

## What I checked

1. **Scope.**
   - mathlib4 `f40c936a64` changes only the three permitted files, +81 lines total: `MachineModelSpec.lean`, `MachineForwardModelWitness.lean` and the new `FloatCarriedRowCorrespondence.lean`.
   - The branch is still `darktower`, the tree is clean, and nothing was pushed.
   - futon2 `6f494738` touches only `wm-01-float-carrier-1/`, with no `src/` or `test/` changes.
   - No registry file was edited.
2. **The strengthened structure.**
   - `FloatCarriedRow` gains `support_nodup : support.Nodup` and `mass_eq_zero_of_not_mem : ∀ o, o ∉ support → mass o = 0`.
   - `floatRowBound` stays `1 / 10 ^ 12`, and the existing fields and `exact_row_admissible` are unchanged. `ProbabilityKernel` is untouched.
   - Nonempty support is a theorem, `support_ne_nil`, derived from the bound rather than a new field, as the packet asked.
3. **Constructors.** `advanceTwiceRow` and `cascadeRow` discharge both new fields against the exhaustive 12-constructor support, so the off-support obligation is vacuous there, which is lawful. Their coordinates and every existing theorem statement are unchanged. `advanceTwiceRepeatRow` is an alias and needed no edit.
   - Repo-wide, only these two and the new `retainedRow` construct a `FloatCarriedRow`, and none does so positionally, so the added fields cannot silently break another construction.
4. **The conversion.** `toProbabilityKernel` takes the premise `(r.support.map r.mass).sum = 1`, casts each rational mass to ℝ, and proves normalization by list induction over the cast sum (`exact_mod_cast` alone did not suffice; the receipt records that failed attempt). It forwards both support laws, and supplies coordinate and normalization theorems. There is no unconditional coercion and no renormalization.
5. **The retained row.** `retainedMass` gives the seven exact rational images. I verified independently in Python that **each equals `Fraction(double)` of the retained value**, that the sum is `36028797018963969/36028797018963968`, that the deviation is exactly `1/2^55`, and that this is inside the `1e-12` bound while not equal to 1. Seven named coordinate theorems preserve the values, and `retained_not_exact` proves the sum is not one.
6. **Controls, re-run by me** read-only from canonical mathlib4 (`lake env lean -j 1`, no `-o`), with 0 `.lake` writes during my run:

   | Control | Exit | Diagnostic |
   |---|---|---|
   | `Positive.lean` | 0 | sparse `Fin 3` support `[0,1]`, a real off-support outcome with mass 0, an unequal exact conversion, and the retained row's nonempty support |
   | `Duplicate.lean` | 1 | `case support_nodup`, `⊢ False`, with the listed masses still summing to one |
   | `Hidden.lean` | 1 | `case mass_eq_zero_of_not_mem.true`, `⊢ False` |
   | `OutsideBound.lean` | 1 | `case nearNormalised`, `⊢ False`, deviation exactly `2/10^12` |
   | `RetainedConversion.lean` | 1 | `case exact_total_one_premise`, `⊢ False`, on the already-built `retainedRow` |
   | `Axioms.lean` | 0 | 16 declarations |

   Each negative supplies every other field, so the failure identifies the intended law. All six outputs are byte-identical to codex-3's logs.
7. **Axioms.** My own tally over my re-run: `Positive` 6 declarations and `Axioms` 16, each depending on exactly propext, Classical.choice and Quot.sound. No `sorryAx`, and no errors in either.
8. **Dependents.** All six dependents of `MachineModelSpec` plus the new module elaborate at the committed source: `MachineModelSpec`, `MachineBeliefDistribution`, `MachineForwardModelWitness`, `MachineParameters`, `MachinePredictiveOutcome`, `MachinePreferenceDistribution`, `Row16ForwardModelAxiomCheck`, `FloatCarriedRowCorrespondence`. No further source edit was needed.
9. **Canonical cache.** `canonical-cache.json` records 64 files written after the source commit, 8 per module across exactly those eight modules, with `unexpected-files: []`. My own `find` over `.lake` agrees. This was the landing step the packet authorized, so canonical source and cache stay in step.
10. **Runtime unchanged.** `machine_model.clj` is still sha256 `462aae43…`, and `machine-model-test` passes: 11 tests, 319 assertions, 0 failures.
11. **Successor bindings.** `successor-binding.json` records the R4 admission's old artifact pin `02bc5c00…`, the new `62b15fe4…`, the already-stale pre-repair Holes pin `4dc0a76b…`, the current Holes `7f137e4f…`, the new `MachineModelSpec` identity and the dependent declarations. Its scope line is accurate: a formal witness successor, not renewed acceptance of the historical production acquisition. The registry itself is unchanged, and binding-1 still describes `480a666ad2`.

## Findings (none blocking)

- **N1: the registry admission is now stale, by design.** `R4-forward-model-float-carried-production-pins-v1` still points at `02bc5c00…` and at pre-repair Holes. No gate enforces these pins, so nothing fails; the update is codex-28's decision.
- **N2: retained failed elaborations.** The receipt keeps two development failures (`isolated-spec`, `Positive-attempt1`), one of which carries a `sorryAx` diagnostic. Those belong to failed elaborations, not to accepted declarations, and my axiom tally on the final controls confirms no accepted declaration depends on `sorryAx`.
- **N3: pre-existing warnings.** Build logs replay nine pre-existing `Holes` sorry warnings and an unused-simp warning in `MachinePreferenceDistribution`. Neither is introduced here.

## Limits

- Formal scope only. Nothing was reloaded into the serving JVM, and no production row passed through the new path.
- `retainedRow` represents one retained row; it proves nothing about the Clojure reader.
- A represented row stays approximate even when `FloatCarriedRow` admits it.
- The forward-model rows remain constructed witnesses, not production observations.
- WM-01 is **not** checked. Its other consumer and proof bindings, and the numerical operations, remain open.
