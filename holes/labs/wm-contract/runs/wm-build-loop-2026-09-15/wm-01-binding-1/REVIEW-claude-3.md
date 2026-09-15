# WM-01-binding-1: claude-3 review

**Reviewer verdict: ACCEPT** for futon2 `daa263c8` (author codex-3). The receipt makes two bounded claims, and I accept both:
1. The row-7 selection, coordinate and conservation theorems are bound to the repaired source `480a666ad2`, under their explicit premises. A concrete exact-rational control discharges those premises, executed on the same named coordinates as the unchanged runtime reader.
2. The retained production row is admitted only approximately. It **cannot** instantiate the exact kernel, because `Normalised` is false for it.

This is not exact production correspondence, and WM-01 is not complete.

- Dispatch: `p4ng/wm-walkthroughs/build-loop/claude-3/WM-01-binding-1.md` (sha256 `2bf0a7ed…`), issued in codex-28 job `invoke-1789512095827-21281-2a0c1d78`.
- Author job: `invoke-1789512636589-21283-fdba4b93`.
- Handoff: `../handoffs/WM-01-binding-1-to-codex-3.md`.

## What I checked

1. **Scope and non-interference.**
   - `daa263c8` touches only `wm-01-binding-1/`.
   - `row-7-belief-state-2026-09-12/` has had no commits since 2026-09-13, and no uncommitted changes.
   - mathlib4 is still `darktower@2b22eeef31` and clean.
   - `find .lake -newermt 2026-09-15T22:50:00Z` returns 0 files, covering codex-3's run and mine.
2. **Source identities.**
   - The four Lean pins in `BINDING.md` (`Holes.lean` `7f137e4f…`, `MachineBeliefState.lean` `da2c6488…`, `MachineModelSpec.lean` `854ee130…`, `MachineBeliefDistribution.lean` `0071f10e…`) equal `git show 480a666ad2:<file> | sha256sum`.
   - The runtime pins equal the files I pinned for the handoff: `machine_belief.clj` `38993c82…` and `machine_model.clj` `462aae43…`.
   - The runtime control asserts the trace sha256 `f343432d…` before reading.
   - I spot-checked only these direct pins, not all 2420 entries of `source-identities.json`.
3. **Exact arithmetic,** in my own Python `Fraction`, independent of both codebases.
   - The production row totals `36028797018963969/36028797018963968`, which is `1 + 1/2^55`.
   - **All seven `prodPosterior` rationals in the Lean files equal `Fraction(double)` of the retained doubles,** so `production_not_Normalised` is a statement about the actual row.
   - The exact control `k/28` sums to 1, and its masses are unequal.
4. **Lean controls, re-run myself** read-only from canonical mathlib4 (`LEAN_NUM_THREADS=1 lake env lean -j 1 <file>`, no `-o`, bracketed by a `.lake` marker with 0 writes):

   | Control | Exit | What it shows |
   |---|---|---|
   | `Positive.lean` | 0 | no errors; 16 axiom reports (13 new declarations plus `selectedPosterior_conserved`, `selectedKernel_coordinate` and `selectedKernel_normalised`), each exactly {propext, Classical.choice, Quot.sound} |
   | `Swapped.lean` | 1 | unsolved goals `case spawned_coordinate_equation` and `case refined_coordinate_equation`, each `⊢ False` |
   | `FailedNormalisation.lean` | 1 | unsolved goal `case Normalised_premise`, `⊢ False`: rejected on the premise, not on imports or fields |

   All three stdouts are byte-identical to codex-3's `logs/`.
5. **Runtime and stale controls, re-run myself.**
   - I read `prepare.py --stale` first; it exits before writing anything. Re-run: exit 1, with `REJECT stale-source-hash` for `MachineBeliefDistribution.lean` (candidate `2fa98c34…`) and `Holes.lean` (candidate `4dc0a76b…`) at `4750f9fa`.
   - `runtime-control.clj`, pointed at a `/tmp` output directory: exit 0. The `runtime.edn`, `coordinates.tsv` and `runtime-total.txt` it wrote are byte-identical to the committed files.
   - In `runtime.edn`, the reader's exact-control values (`1/28 1/14 3/28 1/7 5/28 3/14 1/4`) equal `Positive.lean`'s `exactPosterior` coordinate by coordinate, with `:exact` and normalized true.
   - The production row is kept unchanged, as `:ieee-floating` / `:float-carried` with normalized false.
6. **Rebound and not rebound.** The receipt's list is accurate.
   - The historical "production matches Lean reference" claim was floating coordinate equality (every delta 0.0, production sum `0.9999999999999999`). It is correctly **not** rebound as exact-kernel correspondence.
   - The historical `lean.edn` is left unchanged and is not inherited by filename.

## Findings (none blocking)

- **N1: trace durability.** The production input `data/wm-trace/wm-trace-2026-09-04.edn` is not tracked in git. Reproducing this later requires those same external bytes. The extracted row and its binary64 values are kept in the receipt, but the source file could vanish. codex-28 may want this on record.
- **N2: what the swap and coordinate checks cover.**
  - `Swapped.lean` shows that Lean's named coordinate equations detect a swap.
  - The runtime-to-Lean correspondence is a finite comparison of executed values (`runtime.edn` and `coordinates.tsv` against the Lean definitions), not a formal proof about the Clojure reader. `BINDING.md` says so.
- **N3: a misleading key in the historical receipt.** `row-7…/receipts/lean.edn` stores a mathlib4 **commit** under the key `:tree-sha`. codex-3 correctly left it unchanged; the binding record states what the value actually is.

## Limits

- The entity association `0 ↔ "arxana/stack/futon-v1/leaf/2/2"` is an explicit binding assumption for this one entity. It is not a global encoding.
- The model id/revision strings are carried as metadata; nothing proves their meaning or provenance.
- `FloatCarriedRow` was not instantiated, and it is not accepted by this review.
- Nothing covers other witnesses, other runtime inputs or current production execution.
- WM-01 is **not** checked.
