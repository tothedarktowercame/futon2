# E-kimi-task-227 — finish statement repair m00A04 in apm-lean

**Requisition:** in-progress — dispatched 2026-11-02T00:20:55Z to kimi-1 as invoke-1793578855877-9986-2755da7a

Clocked in by claude-14 for kimi-1 on 2026-11-02 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Finish m00A04 in ~/worktrees/apm-lean/lemmas, problems/m00A04/lean/Main.lean, from your saved state. Same rules: only that file, no commit, no registry, do not regenerate it. Two `sorry`s are left, as your report says:

1. The final conversion block in `apm_m00A04_variational_of_riesz`: rewrite each inner product to an integral with `apm_m00A04_inner_toLp` and `MemLp.coeFn_toLp`, split the goal's integral with `integral_add`, `integral_finset_sum` and `MemLp.integrable_mul`, finish with `add_comm` and `exact hvar`.
2. Assemble `apm_m00A04_positive_dimensional_weak_solution`: obtain w0 from `riesz_element`; the value is w0 none, the gradients w0 (some i); L2On clauses from `Lp.memLp` and `memLp_two_iff_integrable_sq`; then `weakGradient_of_H10`, `energy_approx_of_H10` and `variational_of_riesz`. Delete the stub `sorry`.

Compile with: flock /tmp/apm-lean-compile.lock systemd-run --user --scope -q -p MemoryMax=12G lake env lean problems/m00A04/lean/Main.lean
Acceptance: 0 errors, no sorry, `#print axioms apm_m00a04` exactly propext, Classical.choice and Quot.sound, checked on a copy.

WORK IN THE FILE, NOT IN YOUR REPLY. Keep each turn short.
