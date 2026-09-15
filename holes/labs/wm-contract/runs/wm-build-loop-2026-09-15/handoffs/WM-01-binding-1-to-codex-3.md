# WM-01-binding-1: rebind the row-7 witness to the repaired carriers

From claude-3 to codex-3. **Author: codex-3. Reviewer: claude-3.** Bell claude-3 back when done (see the end).

Authority: codex-28, under Joe's standing successor authority. Dispatch text: `/home/joe/code/p4ng/wm-walkthroughs/build-loop/claude-3/WM-01-binding-1.md` (sha256 `2bf0a7edf1505401ab115cc4766b614b769b416bff5b7bad379b043431bb8316`). **Read it first; it governs.** This packet adds the facts I checked (verify them, don't trust them) and does not widen the dispatch.

This follows your WM-01-acceptance-1 review of repair `480a666ad2`, which codex-28 accepted. It also builds on codex-2's runtime work, numeric-1 (`ac821857`, `7f546b63`) and support-1 (`9aad9adf`), all accepted at source/test scope.

## The clause

WM-01: "bind dependent proofs and admitted witnesses to the repaired source; avoid inheriting old admissions by filename." The relevant cascade steps are K4 (numeric admission is not exact equality) and K6 (bind proofs and acceptance to the repaired subject). This packet covers **one witness, row 7**. It does not repeat the 38-module repair review or survey other witnesses.

## Facts I checked

### The historical row-7 witness (do not edit any of it)

It lives in futon2 `holes/labs/wm-contract/runs/row-7-belief-state-2026-09-12/`.
- **`receipts/lean.edn`** records `:command "lake env lean DarkTower/WarMachine/MachineBeliefDistribution.lean"`, `:exit 0` and `:tree-sha "4750f9fa17eb09bb3d70c0ae12e4256cf3dcf5ec"`.
  - Despite the key name, `4750f9fa` is a mathlib4 **commit**: `git cat-file -t` gives `commit`. It is "row 7: state belief distribution preservation", 2026-09-12 18:47:38Z, 91 commits before `480a666ad2` and an ancestor of it.
- **Changes between `4750f9fa` and `480a666ad2`** in row 7's import closure:
  - `Holes.lean`, +43/-9 (the repair);
  - `MachineBeliefDistribution.lean`, +2: the repair added the `support_nodup` and `mass_eq_zero_of_not_mem` fields to `selectedKernel`;
  - `MachineModelSpec.lean`, +21, from commit `2f46171d0b` ("Contract v1.1: FloatCarriedRow declared numeric admission mirror"). **This is not part of the 38-file repair you accepted.**
  - So the old receipt's subject is stale. It must not be inherited by filename.
- **`lean.txt`** failed on a missing `MachineModelSpec.olean`. `lean-after-dependency-build.txt` records exit 0 with no output.
- **`readback.edn`** compares production coordinates against "lean-reference" floats: every delta is `0.0`, with `:production-sum 0.9999999999999999` against `:lean-reference-sum 1.0`. That is floating coordinate equality, not a discharge of `Normalised`.
- **`readback.clj`** used the model context `{:id "wm-production" :revision "2026-09-12-row-7"}`.
- **`input.edn`** pins two retained rows for entity `"arxana/stack/futon-v1/leaf/2/2"`:
  - the **carried** `:mu-post` row: `data/wm-trace/wm-trace-2026-09-04.edn`, form 0, sha256 `f343432d772986ddc2f7fe38107913afc997201ebae9b6240dff152e235b1120`;
  - the bootstrapped `:mu-pre` row: `data/wm-trace/wm-trace-2026-05-23.edn`, sha256 `78fe7aaf…`.
  - Both files exist today and match their pins, but **they are untracked in git** (`data/wm-trace/` is not version-controlled). Verify the hash at run time. The required production subject is the carried row; include the bootstrapped row only if you say why.

### The repaired Lean at `480a666ad2`

These files are byte-identical at canonical HEAD `darktower@2b22eeef31`, where `480a..HEAD` changes only CascadeEFE/CascadeEFEPolicies/GOverCascades.

- **`MachineBeliefState.lean`:**
  - `Status` is ordered `spawned refined strengthened addressed falsified foreclosed reopened`; the runtime `mb/state-support` uses the same order.
  - `abbrev Entity := Nat`, `Posterior := Status → ℝ`, `machineBeliefState := Entity → Option Posterior`.
  - Line 61: `def Normalised (posterior : Posterior) : Prop := (Status.all.map posterior).sum = 1`.
- **`MachineBeliefDistribution.lean`** (sha256 `0071f10e…`, in your acceptance hash table):
  - `EntityContext {entity : Entity, modelId : String, modelRevision : String}`;
  - `selectedKernel posterior (hn : ∀ s, 0 ≤ posterior s) (h₁ : Normalised posterior) : ProbabilityKernel Unit Status`;
  - `selectedKernel_normalised`;
  - `selectedKernel_coordinate`, which holds by `rfl`;
  - `selectedPosterior_conserved stored context posterior (hp : stored context.entity = some posterior) hn h₁`.
- **`MachineModelSpec.lean`** is imported. It defines `floatRowBound : ℚ := 1/10^12` and `FloatCarriedRow O` (`support`, `mass : O → ℚ`, `nonnegative`, `nearNormalised : |sum - 1| ≤ floatRowBound`), with `exact_row_admissible`.
  - It is a formal version of the runtime float admission, and could state the "approximately admitted" side of the production row in Lean.
  - Optional. If you use it, report its axioms yourself: `2f46171d0b` was not in the accepted review.

### Accepted evidence you can reuse (do not re-run it)

WM-01-acceptance-1 (futon2 `3fc00fe1` and `1f373e4b`, reviewed `9fcf1af1`, accepted by codex-28) covers:
- a fresh build of `MachineBeliefDistribution.lean` at the subject, exit 0;
- `#print axioms` for **`selectedKernel` only**: {propext, Classical.choice, Quot.sound}.

It did **not** cover `selectedPosterior_conserved`, `selectedKernel_coordinate`, `selectedKernel_normalised` or the MachineModelSpec additions. Report axioms for any of those you rely on.

### Runtime

- futon2 `9aad9adf`; HEAD `0e5f5545` has no source change since.
  - `src/futon2/aif/machine_belief.clj`: sha256 `38993c823ccbd9dd10b6daa51902de4ae9bc7d08feaaa826c26a3bc2f332908b`
  - `src/futon2/aif/machine_model.clj`: sha256 `462aae432397e21209c238685d1aa39d040f15b330e2baa5f771d4858eb66b77`
- `belief-state-distribution` returns `:numeric-admission` with `:values`, `:representation`, `:exact-total`, `:exact-deviation`, `:exactly-normalized?`, `:criterion` and `:support`.
- The retained carried row's exact represented total is `36028797018963969/36028797018963968`: deviation `1/2^55`, admitted as `:float-carried` with `:exactly-normalized? false`.

## What to build (all under the receipt directory)

**1. The binding record** (for example `BINDING.edn` plus `BINDING.md`). It should include:
- the theorem names;
- the Lean source sha256s and import identities at `480a666ad2`, plus toolchain versions;
- the runtime source sha256s;
- the reader input and output;
- the State support and its order;
- the entity and model id/revision.

**The entity association.** Runtime entities are strings; Lean's `Entity` is `Nat`. State the association for **this single entity only**, for example `0 ↔ "arxana/stack/futon-v1/leaf/2/2"`, as a binding assumption. Do not assert a global encoding.

**2. The positive exact control.** Take a seven-status exact-rational row with **unequal** masses summing to 1; `k/28` for k = 1..7 works.
- Run the unchanged runtime reader on it.
- Write a Lean control that:
  - defines the same posterior on the same named coordinates;
  - proves `hn` and `Normalised`;
  - instantiates `selectedKernel`;
  - proves each named coordinate equals the reader's exact value;
  - instantiates `selectedPosterior_conserved` for a concrete `stored` at the associated entity.
- Keep the exact outputs and the `#print axioms` reports.

This is a constructed correspondence control, not a newly acquired production observation.

**3. The retained production row.**
- Read the pinned trace bytes (check the sha256 first) and run the unchanged reader.
- Recompute the exact represented values and total **independently**, not through the code under test, for example with Python `Fraction(float)`.
- Record the true classification: the numeric admission passes, but the premise `Normalised` of the unchanged exact-kernel construction **fails**.
- In Lean, prove this as a positive statement where you can: for example `¬ Normalised prodPosterior`, with the posterior built from the exact rational images of the seven doubles. That is cleaner than an expected compile failure.
- Do not normalize, change a tolerance, replace the row, weaken Lean's kernel, or call this exact production correspondence.

**4. Controls that must be distinguished.** The dispatch requires all four:

| Control | What it must show |
|---|---|
| (a) stale source hash | Your binding check rejects the historical `4750f9fa` blobs, e.g. of `MachineBeliefDistribution.lean` or `Holes.lean`, against the pinned `480a666ad2` identities |
| (b) swapped coordinate association | Swapping two unequal masses between named states is caught; the rejection must name the coordinate equation |
| (c) failed exact normalization on the retained floating row | The rejection is about the `Normalised` premise, not missing imports, fields or syntax |
| (d) valid exact-rational control | It passes, so that refusing everything cannot pass |

**5. Rebinding statement.** Say precisely which old claims can now be rebound to the repaired source, and on what premises, and which cannot. For example, the historical "production matches Lean reference" claim is floating coordinate equality and cannot rebind as exact-kernel correspondence. A limitation record alone does not satisfy this; the positive exact control in item 2 is required.

## How to run the Lean controls without touching the canonical checkout

- Your control files live in the receipt directory. Compile them read-only against the canonical build, with no `-o`, so no output is written. For example:

  ```
  cd /home/joe/code/mathlib4
  LEAN_NUM_THREADS=1 lake env lean <receipt-dir>/Control.lean
  ```

  In the acceptance review I re-ran your controls this way, and the output was byte-identical to your fresh-build logs. It is valid only while the imported closure is byte-identical to `480a666ad2`: confirm that for `MachineBeliefDistribution`'s closure.
- Before and after, run `touch` on a marker file, then check `find .lake -newer <marker>` shows 0 files. Do not switch the canonical checkout.
- If an olean is missing or stale (the 2026-09-12 run hit exactly this), **do not build into the canonical `.lake`**. Use the isolated snapshot method from acceptance-1, and delete it afterwards. `/` is about 96% full.

## Allowed edits and gates

- Write only under `futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-binding-1/`. Isolated temporary output is allowed.
- Not allowed: production or Lean source edits, historical receipt edits, dependency upgrades, persistence, serving reload, genesis, clicks, or a full proof census.
- For any Clojure control: clj-kondo, and check-parens with the files after `--`:

  ```
  emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval "(arxana-check-parens-cli)" -- <files>
  ```

- Syntax-check any Python.
- For every control, keep the command, exit status and output, and don't pipe gate output.
- Commit only the receipt directory, by explicit path. futon2 is shared: never `commit -a`, never amend, and check `git log -1` first.
- If this reveals a needed source change or a new formal relation, **stop** and report the exact conflict. Do not broaden.

## Stop and bellback

Stop once the receipts are committed. **Bell claude-3 back** with:
- the commit sha;
- the binding record's path;
- the controls with their exit statuses;
- the claims rebound and not rebound;
- the remaining premises;
- any conflict.
