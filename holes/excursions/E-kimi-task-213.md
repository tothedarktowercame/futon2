# E-kimi-task-213 — finish reformed statement m93J06 in apm-lean

**Requisition:** completed — 2026-11-01T22:21:58Z, job invoke-1793568367946-9757-0004c7f9, state done

Clocked in by claude-14 for kimi-3 on 2026-11-01 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Finish a reformed statement in apm-lean. Worktree ~/worktrees/apm-lean/lemmas, branch lemmas. Edit only problems/m93J06/lean/Main.lean and PRELIM-DEFECT.md. No commit, no registry. Read problem.md and PRELIM-DEFECT.md first.

Part (d) of the printed problem is false and will be negated in Lean. The other parts are the standard ODE theorems, but the current `apm_m93j06` assumes a global Lipschitz constant everywhere, which excludes y' = y², and part (b) assumes a global flow. An outside grader rejected that. Keep the existing theorems under other names. `apm_m93j06_picard_lindelof_local` is already in the file. Install a new `apm_m93j06` with these clauses, as separate lemmas, in this order:

(a) f continuous, locally Lipschitz in y; for each y₀ there is ε > 0 and a solution on (-ε, ε), unique there.
(b) f C¹, no global flow assumed. For each y₁ there are ε, r > 0 and a local flow on (-ε, ε) × (y₁ - r, y₁ + r), with y ↦ φ(t, y) C¹ for |t| < ε. Route: cut f off smoothly in y so it is globally Lipschitz, apply the existing global theorem, and use uniqueness to identify solutions while they stay on the plateau.
(c) f locally Lipschitz in y with |f(t, y)| ≤ C|y|: the solution from every y₀ exists for all t. Route: truncate f outside |y| ≤ R with R from Gronwall's bound |y(t)| ≤ |y₀| exp(C|t|).
(d) For every α in (0,1) there is a continuous α-Hölder f and no selection of solutions y₀ ↦ y(·) continuous at y₀ = 0 for t ≠ 0. Candidate: f(y) = sign(y)|y|^α.
(e) f C¹ with |f| ≤ C|y|: the flow exists and y ↦ φ(t, y) is C¹ for every t. And the Hölder failure persists under the growth bound, with a field such as min(|y-1|^α, 1)·min(|y|, 1). The exp(L|t|) Lipschitz estimate stays as a clause.

Compile with: flock /tmp/apm-lean-compile.lock systemd-run --user --scope -q -p MemoryMax=12G lake env lean problems/m93J06/lean/Main.lean
Acceptance: 0 errors, no sorry, temporary `#print axioms apm_m93j06` showing no sorryAx, removed afterwards.

WORK IN THE FILE, NOT IN YOUR REPLY: a reply is cut off at a length limit, and if you spend it reasoning the job ends with nothing saved. Keep each turn short. Draft the clause statements with sorry first, compile, then fill them one at a time. If you run out, leave every finished clause proved, and say which are left.
