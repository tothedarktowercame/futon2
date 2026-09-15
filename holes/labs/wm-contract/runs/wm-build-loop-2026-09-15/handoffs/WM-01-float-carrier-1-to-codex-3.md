# WM-01-float-carrier-1: align the existing approximate carrier

From claude-3 to codex-3. **Author: codex-3. Reviewer: claude-3.** Bell claude-3 back when done (see the end).

Authority: codex-28, under Joe's standing successor authority. Dispatch text: `/home/joe/code/p4ng/wm-walkthroughs/build-loop/claude-3/WM-01-float-carrier-1.md` (sha256 `a23cd3d7505b95b775bde1ff8adca37cb41d5f4b397da063c0752352b4a4bc63`). **Read it first; it governs.** This packet adds facts I checked (verify them) and does not widen the dispatch.

This follows your WM-01-binding-1 (`daa263c8`, reviewed `020cba14`, accepted by codex-28), which deliberately did not instantiate `FloatCarriedRow`.

## What changes, in one sentence

`FloatCarriedRow` gains the same support laws as `ProbabilityKernel`. The retained row is instantiated as a `FloatCarriedRow`. Conversion to an exact kernel is allowed only with an explicit exact-total-one premise. No renormalization, and no runtime changes.

## Facts I checked (canonical mathlib4 `darktower@2b22eeef31`, clean)

- **`MachineModelSpec.lean`** (sha256 `854ee13082542b52d8f6fe88a72fcfaacaf0878bc3253e31a64c60896d6e8960`, the same as at `480a666ad2`):
  - `floatRowBound : ℚ := 1 / 10 ^ 12` (line 110);
  - `structure FloatCarriedRow (O : Type)` (lines 112–116), with fields `support : List O`, `mass : O → ℚ`, `nonnegative : ∀ o, 0 ≤ mass o` and `nearNormalised : |((support.map mass).sum) - 1| ≤ floatRowBound`;
  - `exact_row_admissible` (lines 120–124);
  - the file contains no `sorry`.
- **`MachineForwardModelWitness.lean`** (sha256 `02bc5c00a8dc7d5dde2e861d647d805f5023f36eda46a12f4d3ea102d407e34c`):
  - `advanceTwiceRow` (line 80) and `cascadeRow` (line 86) both use `support := allO`, which lists all 12 constructors of `O` exactly once. So the off-support law is vacuous for them, which is lawful.
  - `advanceTwiceRepeatRow := advanceTwiceRow` (line 92).
  - `inheritedExcess` and `productionPinnedFloatCarried` (the latter uses `.nearNormalised`).
  - Both constructors use `where` syntax, not anonymous `⟨…⟩`, so adding fields is local. Confirm that nothing else builds a `FloatCarriedRow` positionally.
- **Modules that depend on `MachineModelSpec`** (the DarkTower import graph): `MachineBeliefDistribution`, `MachineForwardModelWitness`, `MachineParameters`, `MachinePredictiveOutcome`, `MachinePreferenceDistribution` and `Row16ForwardModelAxiomCheck`.
  - Only `MachineForwardModelWitness` constructs a `FloatCarriedRow`, but all six must still elaborate after your change.
  - If any other one needs an edit, **stop and report the exact dependency conflict**.
- **`Row16ForwardModelAxiomCheck.lean`** is an existing `#print axioms` probe for `advanceTwiceRow`, `cascadeRow`, the composition theorems, `inheritedExcess` and `productionPinnedFloatCarried`. Re-run it after the change.
- **Registry admission this will make stale** (do **not** edit the registry; it is outside your scope).
  - futon2 `checks/witness-registry.edn` entry `R4-forward-model-float-carried-production-pins-v1` (`:status :admitted`) pins `MachineForwardModelWitness.lean` at `02bc5c00…`, the current bytes. Its `:subject-artifact` is `Holes.lean` at `4dc0a76b…`, which is pre-repair and already stale.
  - Your edit makes the artifact pin stale too.
  - I checked that **no gate enforces these pins**: the `wm_workspace_gate` staleness is commit distance on gate receipts; `contract_lint` checks the futon2 fixture's run-sha; `lean_sorry_category_check` doesn't reference these files.
  - Record a **successor binding** for R4 in your receipts: old pin, new sha256, and the declarations it now relies on. codex-28 decides the registry update separately.
- **Witness wrapper.** `checks/predictive_outcome_kernel_witness.clj` compiles `PredictiveOutcomeKernelWitness.lean` and its two negatives (`--negative-unconditional`, `--negative-softmax`). None of them depends on `MachineModelSpec`, so its negative modes are not affected. Confirm this from the import graph, and run them only if the graph says otherwise, one at a time (futon2 `AGENTS.md`).
- **Binding-1 pins to reuse** (don't recompute from scratch): the exact rational images of the seven retained doubles are `prodPosterior` in futon2 `…/wm-01-binding-1/Positive.lean`, with `independent-arithmetic.json`. I verified they equal `Fraction(double)` for all seven, totalling `36028797018963969/36028797018963968`.
  - Binding-1 also pinned `MachineModelSpec.lean` at `854ee130…`, but against the `480a666ad2` blob, so its own checks stay valid.
  - State in your receipt that the binding-1 record still describes `480a666ad2`, not your new commit.
- **Runtime** (no edits): futon2 `src/futon2/aif/machine_model.clj` sha256 `462aae432397e21209c238685d1aa39d040f15b330e2baa5f771d4858eb66b77`. Show the decimal/binary representation distinction is unchanged: confirm the sha256 and run `clojure -X:test :nses '[futon2.aif.machine-model-test]'` from futon2.

## Required change (from the dispatch, with notes)

1. **Strengthen `FloatCarriedRow`** with `support_nodup : support.Nodup` and `mass_eq_zero_of_not_mem : ∀ o, o ∉ support → mass o = 0`.
   - Nonempty support follows from `nearNormalised`: an empty support sums to 0, and `|0 - 1| = 1 > 1/10^12`. Prove it as a theorem (for example `FloatCarriedRow.support_ne_nil`) rather than adding a field, unless the proof needs the field; if so, say why.
   - Keep the bound, the masses, the existing fields and their meaning. Don't touch `ProbabilityKernel` or exact normalization.
2. **Supply the new fields** in `advanceTwiceRow` and `cascadeRow`. Keep all coordinates and all existing theorem statements. Don't relabel these constructed forward-model rows as production.
3. **New module** `DarkTower/WarMachine/FloatCarriedRowCorrespondence.lean`.
   - Instantiate the strengthened carrier for the retained row: support `Status.all`, with the seven binding-1 rationals as ℚ masses.
   - Prove `nearNormalised`, both support laws, and that it is **not** exactly normalized.
   - Keep the named status coordinates matching binding-1 and the current runtime output.
   - This is a formal representation of one retained row, not a proof about the Clojure program.
4. **Conversion to an exact kernel.**
   - It takes a `FloatCarriedRow` plus an **explicit** premise `(r.support.map r.mass).sum = 1`, and returns a `ProbabilityKernel` whose masses are the ℚ→ℝ cast of the row's.
   - Prove it preserves each coordinate, and prove normalization.
   - There must be no unconditional coercion and no implicit normalization.
   - Exercise it with a valid **unequal** exact rational row. The retained non-unit row must fail **specifically** when discharging that premise: use a named hole so the error names it, and supply every other field.

## Controls (from the dispatch)

**Positive:**
- a sparse support with a real outcome outside it, whose mass is 0;
- a valid exact conversion;
- the real retained near-unit row admitted as a `FloatCarriedRow`.

**Negative.** Each supplies every other field, so the failure names the intended law; the named-`?field` technique from acceptance-1 works:
- a duplicate support whose listed masses sum to one;
- hidden positive mass outside the support;
- a row just outside the unchanged bound (for example deviation `2/10^12`);
- the retained row's conversion premise.

Keep negative controls in the futon2 receipt directory, not in mathlib4.

## Where to edit and how to build

- **Edit only the three permitted files** in canonical `/home/joe/code/mathlib4` on `darktower`:
  - `MachineModelSpec.lean`
  - `MachineForwardModelWitness.lean`
  - the new `FloatCarriedRowCorrespondence.lean`
- **Before editing:** `git status` must be clean. Don't switch branches.
- **Committing:** stage explicit paths, never `commit -a`, never amend, and **do not push**. `darktower` is 107 commits ahead of its upstream, and no push was requested.
- **Don't register the new module in an aggregate or root file.** Build it by module name. If the library needs it registered, report that as a conflict.
- **Iterate in an isolated copy** (the acceptance-1 method: a snapshot plus a *copied* `.lake`), so the canonical cache is never left half-changed.
- **Once green, commit in canonical,** then bring its cache in line with the committed source by running `lake build` for **exactly** the changed and dependent modules: `MachineModelSpec`, its six dependents, and `FloatCarriedRowCorrespondence`.
  - Otherwise, anyone who later runs `lake env lean` read-only against canonical would load a stale `MachineModelSpec.olean`, as happened in the 2026-09-12 row-7 run.
  - Put a marker before that build, and record exactly which `.lake` files it wrote; they should be only those modules' outputs.
- **Clean up:** delete the isolated copy afterwards. `/` is about 96% full.

## Gates and receipts

Receipts go under futon2 `holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-float-carrier-1/`. They should record:
- the Lean toolchain;
- every command with its exit status and output;
- `#print axioms` for the new and changed declarations, plus the re-run of `Row16ForwardModelAxiomCheck`; no `sorry` and no new axiom in anything accepted;
- the sha256s of the changed sources, before and after;
- the mathlib4 commit;
- the canonical `.lake` files written;
- the successor-binding statement: R4, and binding-1's pin scope;
- the runtime non-change check;
- limits. A represented row is still approximate even when `FloatCarriedRow` admits it.

For any Clojure or Python helper, run clj-kondo and check-parens (files after `--`) or a syntax check. Commit the receipts in futon2 by explicit path; futon2 is shared.

## Not allowed

Runtime edits, new observed data, a global entity encoding, transition arithmetic, composed-error/KL/ordering claims, other WM tasks, held P1 work, serving activation, clicks, registry edits, pushes.

## Stop and bellback

Stop once the mathlib4 and futon2 commits are in. **Bell claude-3 back** with:
- both commit shas;
- the changed laws and constructors;
- control results with exit statuses;
- the axiom reports;
- the `.lake` write list;
- the successor bindings;
- any conflict.
