# E-kimi-task-220 — finish reform a96A04 in apm-lean

**Requisition:** completed — 2026-11-01T23:21:56Z, job invoke-1793574059439-9860-f7980833, state failed

Clocked in by claude-14 for kimi-3 on 2026-11-01 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Finish a96A04 in ~/worktrees/apm-lean/lemmas, problems/a96A04/lean/Main.lean. Edit only that file and PRELIM-DEFECT.md; no commit, no registry; do not regenerate it. It is a compiling draft with two `sorry`s in `apm_a96a04`: clause (c), the L² limit, and clause (a), smoothness. Clause (b) is proved.

(c): weighted global L² estimate, then Gaussian dilation and dominated convergence; the route is in PRELIM-DEFECT.md, and its helpers sit above the theorem.
(a): no Faa di Bruno. Prove one step: if g is C¹ with g and fderiv g bounded by a Gaussian envelope and f is in L², then x ↦ ∫ g(x-y) f(y) dy has Frechet derivative x ↦ ∫ (fderiv g)(x-y) f(y) dy, by `hasFDerivAt_integral_of_dominated_of_fderiv_le` with Cauchy-Schwarz. Then induct with `contDiff_succ_iff_fderiv` over polynomial × exp(-‖x‖²/4t), which fderiv preserves.

Compile with: flock /tmp/apm-lean-compile.lock systemd-run --user --scope -q -p MemoryMax=12G lake env lean problems/a96A04/lean/Main.lean
Acceptance: 0 errors, no sorry, axioms exactly propext, Classical.choice and Quot.sound.

WORK IN THE FILE, NOT IN YOUR REPLY. Keep each turn short. Write lemma statements with sorry first, compile, then fill them one at a time. If you run out, leave what is proved, and say what's left.
