# E-kimi-task-226 — a96A04 clause (a): heat kernel smoothness

**Requisition:** completed — 2026-11-02T00:38:17Z, job invoke-1793578739621-9985-f28f2bba, state failed

Clocked in by claude-14 for kimi-3 on 2026-11-02 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Continue a96A04 in ~/worktrees/apm-lean/lemmas, problems/a96A04/lean/Main.lean, from your saved state. Same rules: only that file and PRELIM-DEFECT.md, no commit, no registry, do not regenerate it. Clauses (b) and (c) are proved. One `sorry` is left, `heatConvN_contDiff`, clause (a). Your plan from the last report is right; follow it, writing each step into the file as you go:

1. Kernel bounds ‖iteratedFDeriv ℝ j K z‖ ≤ C_j e^{-‖z‖²/8t}, by `norm_iteratedFDeriv_comp_le` applied to exp ∘ Q, with a small induction for ‖iteratedFDeriv ℝ i Complex.exp w‖ ≤ exp(w.re).
2. One step: `hasFDerivAt_integral_of_dominated_of_fderiv_le` for F_g x = ∫ g(x-y) f(y) dy, with the Gaussian envelope and Cauchy-Schwarz domination.
3. Induction on k: for every smooth g with Gaussian bounds on all derivatives, ContDiff ℝ k F_g, stepping by `contDiff_succ_iff_fderiv`, using `norm_iteratedFDeriv_fderiv`, and reducing fderiv F_g to scalar convolutions with `contDiff_clm_apply`.
4. Final assembly by `contDiff_infty`.

Compile with: flock /tmp/apm-lean-compile.lock systemd-run --user --scope -q -p MemoryMax=12G lake env lean problems/a96A04/lean/Main.lean
Acceptance: 0 errors, no sorry, `#print axioms apm_a96a04` exactly propext, Classical.choice and Quot.sound, checked on a copy. Then update PRELIM-DEFECT.md if it no longer matches.

WORK IN THE FILE, NOT IN YOUR REPLY. Keep each turn short. Compiles are expensive, so test new lemmas in a small scratch file under scratch/ that imports what it needs.
