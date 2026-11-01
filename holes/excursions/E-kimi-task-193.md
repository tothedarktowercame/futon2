# E-kimi-task-193 — finish lemma iteratedFDeriv_taylorPolynomial_exact in apm-lean from banked helpers

**Requisition:** in-progress — dispatched 2026-11-01T07:10:16Z to kimi-2 as invoke-1793517017011-9219-60ac304d

Clocked in by claude-14 for kimi-2 on 2026-11-01 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

From claude-14 (Joe's go, 2026-10-13): prove ONE lemma of an apm-lean blueprint. Other seats are proving the other lemmas of the same problem at the same time, each in its own file.

WHERE
- Worktree ~/worktrees/apm-lean/lemmas (branch `lemmas`; .lake/packages is a symlink, never lake update or rebuild Mathlib). Never touch ~/code/apm-lean.
- Your file: holes/labs/lemmas/P/NAME.lean. It is the problem's Blueprint.lean with your lemma between the lines `-- BEGIN SEAT AREA` and `-- END SEAT AREA`.
- Context: holes/labs/blueprints/P/BLUEPRINT.md (proof sketch, Mathlib names), problems/P/problem.md, informal-solution.md.
- Shared files (2026-10-18): if P is CT.Module, your file is a copy of ConstructionTargets/Module.lean, the proof sketch is ConstructionTargets/Module.blueprint.md, and there is no problems/P; the consumer problems are named in the sketch.
- Compile: `cd ~/worktrees/apm-lean/lemmas && flock /tmp/apm-lean-compile.lock systemd-run --user --scope -q -p MemoryMax=12G lake env lean holes/labs/lemmas/P/NAME.lean`, as often as you like. The flock serialises Lean compiles across seats (disk load, 2026-10-15); keep it. Never run `lake build`.
- A compile of these files takes 2-5 minutes, longer while another seat holds the lock. Append `; echo EXIT=$?` to the command and give the tool call a timeout of at least 15 minutes. A compile counts only when you have seen its `EXIT=` line: no output means the call was cut off, not that the file compiled (2026-10-25: a seat reported PROVED from two cut-off 34-second calls; the file had 9 errors).

RULES (a mechanical checker enforces them; anything else is rejected)
- Edit only between the two markers. Everything outside them must stay byte-identical, including the other lemmas' `sorry`s.
- Inside: replace the `sorry` of NAME with a proof. You may add helper lemmas/defs inside the area, above NAME, with names starting `NAME_aux`. Do not change NAME's statement (anything before its `:= by`).
- No `sorry`, `admit`, `axiom`, `native_decide` in the area. You may not use another sorry'd lemma of the file (it would show sorryAx in NAME's axioms).
- Missing Mathlib theorems: prove them as helpers. Do not stop because Mathlib lacks something; build it.
- Do not commit (other seats share the worktree). Just leave the file.

BLOCKED: if your lemma cannot reasonably be proved without another lemma of the file that is still `sorry` (the blueprint's dependency layers), do not prove that lemma yourself: stop at once and reply `BLOCKED-ON <name>`. It will be re-queued after <name> lands. Facts you can prove in passing are not a reason to block.

DONE when the file compiles and `#print axioms NAME` (add it temporarily, then remove it) shows no sorryAx.
If the episode ends first, leave your best partial file (compiling, with the remaining gap as a `sorry` inside the area) and say exactly what remains. A partial must bank work: state each sub-step you finished as a proved `NAME_aux_*` helper above NAME, and the unfinished sub-steps as separate `NAME_aux_*` lemmas with `sorry`, so the next seat can take them in parallel. Commented-out attempts do not count (two round-1 partials left nothing usable).
PARTIAL QUALITY (Joe, 2026-10-29; several partials that day banked nothing usable):
- NAME itself must not end in `sorry`. Write NAME's proof as the assembly that calls your `NAME_aux_*` leaves, so the only `sorry`s are in leaves. A partial whose main lemma is `sorry` has not shown that its leaves suffice.
- Every banked helper must be USED by that assembly. Do not bank side facts.
- Compile the partial and see `EXIT=0` before replying. Never report a helper you have not compiled.
- No `sorry` followed by a `/- ... -/` proof attempt. If an attempt does not compile, keep only the parts that do, as proved helpers.
- Before stating a leaf, check the file for a lemma that already says it (grep the statement's key names). Use that lemma instead of restating it.
- If you found no usable decomposition, reply PARTIAL with the file untouched and two or three sentences on what you tried and where it failed. That is more useful than thin helpers.

REPLY: bell claude-14: PROVED or PARTIAL, line count of the area, and for PARTIAL the precise remaining goal.
- SEARCH SCOPE (claude-14, 2026-10-15): search only inside ~/worktrees/apm-lean/lemmas, e.g. `rg PATTERN ConstructionTargets holes/labs/blueprints .lake/packages/mathlib/Mathlib`. Never run rg/grep/find over ~ (/home/joe), /, or /tmp: two seats ran rg over /home/joe for an hour and loaded the disk. A lemma you banked in your own seat area is in your own file, so look there.
- NO SORRY IN DEFS (claude-14, 2026-10-15): never write a def/abbrev/instance whose body is or contains sorry. Downstream proofs then carry sorryAx and the checker rejects them. When a construction needs an unproved fact, make that fact an explicit hypothesis argument of the def and prove it as a separate lemma.
- NO PLACEHOLDER LEAVES (claude-14, 2026-10-15): never state a lemma as `True`, or anything else trivially provable, to mark a step. A leaf must state the actual mathematical claim the assembly consumes.

THIS JOB: P = CT.SardWhitneyExtension, NAME = iteratedFDeriv_taylorPolynomial_exact. Your file: holes/labs/lemmas/CT.SardWhitneyExtension/iteratedFDeriv_taylorPolynomial_exact.lean. It already holds banked helpers from an earlier seat, including the hard symmetry lemma _aux_symm; keep them and finish the main lemma. First compile it to confirm it builds, since the last helpers were removed without a recompile. Below is that seat report, with the route that was checked and one route that fails. Follow it.

PARTIAL — E-kimi-task-189, `iteratedFDeriv_taylorPolynomial_exact` (CT.SardWhitneyExtension). Not proved; main lemma is still `sorry`.

**File state** (`holes/labs/lemmas/CT.SardWhitneyExtension/iteratedFDeriv_taylorPolynomial_exact.lean`, edits only inside seat area, ~150 lines added): banked helpers that were all compiling in the last full compile before I added the final two helpers (which I then deleted to restore that state; I did not get a final EXIT=0 after the deletion, so treat as high-confidence-but-unverified):

- `..._aux_cast`: `M p (fun i ↦ v (Fin.cast h i)) = M r v` for `h : p = r` (proof: `subst h; rfl`).
- `..._aux_append`: `Sum.elim u z ∘ finSumFinEquiv.symm = Fin.append u z` (via `Fin.addCases`).
- `..._auxEval`/`_apply`/`_eq_apply`: bundled evaluation `z ↦ ContinuousMultilinearMap.apply ℝ _ F z` as a `ContinuousMultilinearMap` (via `MultilinearMap.mkContinuous`; needed `(𝕜:=ℝ)(ι:=Fin q)(E:=..)(G:=..)` pins in `refine` to avoid stuck TC).
- `..._auxCurried`/`_apply`: `currySum (M.domDomCongr finSumFinEquiv.symm)`, with `curried M u z = M (Fin.append u z)`.
- `..._aux_symm` (the hard part, fully proved): for symmetric `M : E[×(k+(q+1))]→L[ℝ] F`, `M (update (append u (fun _ : Fin (q+1) ↦ c)) (natAdd k j) w) = M (fun i ↦ append (cons w u) (fun _ : Fin q ↦ c) (Fin.cast (by omega) i))`, via explicit permutation `σf i = if i=0 then k+j else if i≤k+j then i-1 else i`, `Equiv.ofBijective`, `Finite.injective_iff_surjective`, and pointwise val-case analysis.

**✘ Failed route (recorded for next seat):** computing the Fréchet derivative of the map-valued function `y ↦ (u ↦ J (k+q) a (append u (fun _ ↦ y-a)))` via `HasFDerivAt.multilinear_comp` applied to a *named def* (`auxEval`) whose codomain is a CLM space fails: elaboration of `multilinear_comp`'s implicit `f` compares the def's stored instances (`Real.semiring`, `ContinuousLinearMap.addCommMonoid/module`) against the lemma's `NontriviallyNormedField`-projection instances and does not unify. The scratch test showed `multilinear_comp` works fine when `f` is a *local variable / theorem argument* (like `J (k+q) a` directly).

**Validated remaining route** (all ingredients existence-checked): induct on `k` (generalizing `x`, `v`, `hk`). Step uses `iteratedFDeriv_succ_apply_left` (it is `rfl`), then `fderiv ℝ (iteratedFDeriv k P) x (v 0) (tail v) = fderiv ℝ (fun y ↦ S y (tail v)) x (v 0)` where `S` is the IH formula. Justify by: (1) `P = taylorPolynomial m J a` is `ContDiff ℝ ⊤`: each term via `(ContinuousMultilinearMap.contDiff _).comp (contDiff_pi.2 fun _ ↦ contDiff_id.sub contDiff_const)` (pattern confirmed in `FourierTransformDeriv.lean:392`), then sum/smul; hence `G = iteratedFDeriv k P` is `DifferentiableAt` (`ContDiffAt.differentiableAt_iteratedFDeriv`). (2) `ContinuousLinearMap.comp_hasFDerivAt` with `Φ = ContinuousMultilinearMap.apply ℝ _ F (tail v)` turns map-valued `fderiv G x w (tail v)` into scalar `fderiv (fun y ↦ G y (tail v)) x w`; rewrite by IH funext. (3) Scalar derivative of each term by `HasFDerivAt.multilinear_comp (J (k+q) a) hg` with `g i y = if i.val < k then u _ else y - a`; split the `Fin (k+q)` sum via `Fin.sum_univ_add` (additive version of `Fin.prod_univ_add`, BigOperators/Fin.lean:159); first block vanishes (`map_update_zero`), second block's `q` terms are all equal by `aux_symm`. (4) Reindex `Finset.sum_range_succ'`, cancel `((q+1)!)⁻¹ • (q+1) • X = (q!)⁻¹ • X` (`Nat.factorial_succ`, `mul_inv_rev`), close casts with `aux_cast`, and `Fin.cons_self_tail` to get `v` back.

Nothing committed; scratch file deleted.