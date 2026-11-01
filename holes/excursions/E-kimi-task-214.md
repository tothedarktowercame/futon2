# E-kimi-task-214 — statement repair m95J02 in apm-lean: dimension one

**Requisition:** in-progress — dispatched 2026-11-01T21:43:58Z to kimi-2 as invoke-1793569438087-9790-dfe3297e

Clocked in by claude-14 for kimi-2 on 2026-11-01 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Statement repair in apm-lean. Worktree ~/worktrees/apm-lean/lemmas, branch lemmas. Edit only problems/m95J02/lean/Main.lean. No commit, no registry.

`apm_m95j02` has `hn : 2 ≤ n`, and the original covers every dimension. Keep the existing theorem renamed `apm_m95j02_two_le`, and add `apm_m95j02 (n : ℕ) (hn : 1 ≤ n)` with the same three clauses, by adding the case n = 1. Check first what `apm_m95J02_fundamental 1 x` and `apm_m95J02_poissonSolution 1 f` evaluate to; they should give ‖x‖/2 with sign ΔΦ = δ, and if the definitions don't, fix the n = 1 branch of the definition without changing n ≥ 2.

For n = 1, transport across the volume-preserving equivalence EuclideanSpace ℝ (Fin 1) ≃ ℝ (`EuclideanSpace.volume_preserving_measurableEquiv` and friends, or `MeasurableEquiv.funUnique`). Then the real-line facts:
- ∫ t, (|t|/2) φ''(t) dt = φ(0), by integration by parts on each half-line, for compactly supported smooth φ.
- u(x) = ∫ (|x - y|/2) f(y) dy satisfies u'' = f: split the integral at x and differentiate twice by the fundamental theorem, for compactly supported smooth f.
- The radial clause is direct: on Fin 1, the Laplacian of v‖z‖ is v''(‖x‖), and (n-1)/‖x‖ vanishes.

Compile with: flock /tmp/apm-lean-compile.lock systemd-run --user --scope -q -p MemoryMax=12G lake env lean problems/m95J02/lean/Main.lean
Acceptance: 0 errors, no sorry, temporary `#print axioms apm_m95j02` showing exactly propext, Classical.choice and Quot.sound, removed afterwards. No native_decide.

WORK IN THE FILE, NOT IN YOUR REPLY: a reply is cut off at a length limit, and if you spend it reasoning the job ends with nothing saved. Keep each turn short. Write lemma statements with sorry first, compile, then fill them one at a time.
