# E-kimi-task-224 — m95J02: Euler step clauses, retry

**Requisition:** in-progress — dispatched 2026-11-01T23:37:53Z to kimi-2 as invoke-1793576273613-9922-73056fea

Clocked in by claude-14 for kimi-2 on 2026-11-01 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Follow-up on m95J02 in ~/worktrees/apm-lean/lemmas, problems/m95J02/lean/Main.lean. Same rules: only that file, no commit, no registry. Your n = 1 work stays; the outside grader passed it.

The grader says narrower, for a reason outside your job until now: the Euler step of the printed problem is missing from the theorem. Extend `apm_m95j02` as follows, keeping every n ≥ 1.

1. Clause 1 requires `ContDiff ℝ 2 v` globally, which excludes r^(2-n) and log r, the very functions it should apply to. Weaken it to `ContDiffAt ℝ 2 v ‖x‖`, using the existing `apm_m95J02_laplacian_radial` around line 446.
2. Add the Euler step. For r > 0 and real s: r² (d²/dr² + ((n-1)/r) d/dr) r^s = s(s - 1 + n - 1) r^s, so r^s is harmonic on r > 0 exactly when s = 0 or s = 2 - n. In dimension 2 the second solution is log r; state that it is harmonic.
3. State that Φ = `apm_m95J02_fundamental n` is harmonic away from 0, for every n ≥ 1: Laplacian at x ≠ 0 is 0. For n = 1, Φ = |x|/2 is affine on each side.
4. State the explicit formula for Φ in the three cases, n = 1, n = 2, n ≥ 3, as clauses, if `apm_m95J02_fundamental`'s definition doesn't already say so plainly.

Keep smoothness of the Poisson solution optional; add it only if cheap.

Compile with: flock /tmp/apm-lean-compile.lock systemd-run --user --scope -q -p MemoryMax=12G lake env lean problems/m95J02/lean/Main.lean
Acceptance: 0 errors, no sorry, `#print axioms apm_m95j02` exactly propext, Classical.choice and Quot.sound.

WORK IN THE FILE, NOT IN YOUR REPLY. Keep each turn short.
