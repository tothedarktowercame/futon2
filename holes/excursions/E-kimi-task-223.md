# E-kimi-task-223 — continue statement repair m00A04 in apm-lean

**Requisition:** completed — 2026-11-02T00:20:35Z, job invoke-1793575493682-9892-067e7978, state done

Clocked in by claude-14 for kimi-1 on 2026-11-01 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Continue m00A04 in ~/worktrees/apm-lean/lemmas, problems/m00A04/lean/Main.lean, from your saved state. Same rules: only that file, no commit, no registry, do not regenerate it. One `sorry` is left, the stub `apm_m00A04_positive_dimensional_weak_solution`. Follow the plan from your report:

1. Rebuild the energy space as `PiLp 2 (fun _ : Option (Fin n) ↦ Lp ℝ 2 (volume.restrict U))`, value at `none`, gradients at `some i`, porting `testPair`, `testSubmodule`, `H10` and their add, smul and zero lemmas via `PiLp.ext`.
2. Forcing functional `innerSL ℝ e₀`; Riesz via `InnerProductSpace.toDual`.
3. The weak-gradient clause, by orthogonality of test pairs to J(φ, i) from your restricted integration by parts, plus `Submodule.topologicalClosure_minimal` over `ker (innerSL ℝ J)`.
4. The approximation clause via `Metric.mem_closure_iff`, `real_inner_self_eq_norm_sq`, `PiLp.inner_apply`, `Fintype.sum_option`, `Lp.coeFn_sub` and `MemLp.coeFn_toLp`.
5. The variational identity, then rewire the main theorem and delete the stub.

Compile with: flock /tmp/apm-lean-compile.lock systemd-run --user --scope -q -p MemoryMax=12G lake env lean problems/m00A04/lean/Main.lean
Acceptance: 0 errors, no sorry, `#print axioms apm_m00a04` exactly propext, Classical.choice and Quot.sound.

WORK IN THE FILE, NOT IN YOUR REPLY. Keep each turn short. If you run out, leave what is proved and say what's left.
