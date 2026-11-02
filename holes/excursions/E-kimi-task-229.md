# E-kimi-task-229 — m95A04: complex λ clause

**Requisition:** completed — 2026-11-02T00:39:20Z, job invoke-1793579949906-10008-2bee744e, state failed

Clocked in by claude-14 for kimi-2 on 2026-11-02 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Follow-up on m95A04 in ~/worktrees/apm-lean/lemmas, problems/m95A04/lean/Main.lean. Same rules: only that file, no commit, no registry. Your work stays; the grader found it faithful except one thing, scalars. The printed λ in part (e) is unqualified, and informal-solution.md works over ℂ.

Add to `apm_m95a04` a clause for complex λ: for every λ : ℂ, the causal function x ↦ 1_{x>0} e^{-λx} defines a complex distribution in 𝓓'(⊤, ℂ) with (D + λ) T = δ. Add what's needed: `regularC`, `DtestC`, `DdistC`, `δC`, built exactly as the real ones with scalars ℂ, ideally by generalising your constructions over 𝕜 = ℝ or ℂ, or RCLike, instead of copying. Keep the real clause. Part (a) for complex-valued locally integrable f is a bonus if generalising gives it for free.

Compile with: flock /tmp/apm-lean-compile.lock systemd-run --user --scope -q -p MemoryMax=12G lake env lean problems/m95A04/lean/Main.lean
Acceptance: 0 errors, no sorry, `#print axioms apm_m95a04` exactly propext, Classical.choice and Quot.sound, on a copy.

WORK IN THE FILE, NOT IN YOUR REPLY. Keep each turn short.
