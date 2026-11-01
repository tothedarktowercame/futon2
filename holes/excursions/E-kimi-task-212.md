# E-kimi-task-212 — complete reformed statement m94J02 in apm-lean

**Requisition:** in-progress — dispatched 2026-11-01T21:18:29Z to kimi-3 as invoke-1793567909324-9742-94b43df6

Clocked in by claude-14 for kimi-3 on 2026-11-01 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Complete a corrected statement in apm-lean. Worktree ~/worktrees/apm-lean/lemmas, branch lemmas. Edit only problems/m94J02/lean/Main.lean. Do not commit and do not touch the registry. Read problems/m94J02/problem.md and problems/m94J02/PRELIM-DEFECT.md first.

The printed bound ‖fg‖ ≤ ‖f‖‖g‖ is false and `apm_m94j02` correctly proves it with the sharp constant 5/4. An outside grader says the rest is true but under-stated. Extend `apm_m94j02`, unweakened otherwise, with these clauses:
(1) Part (a) says X is a Banach space. The theorem states only completeness. Add that the coefficient sequences in `apm_m94J02_mem` are closed under addition and scalar multiplication, and the norm axioms: nonnegative, zero only for the zero sequence, homogeneous for complex scalars, triangle inequality.
(2) `apm_m94J02_mul` must be tied to pointwise multiplication: for z in the closed unit disk, eval (mul a b) z = eval a z * eval b z.
(3) `T` must be tied to the printed operator: eval (T a) z = eval a (z / 2) on the closed disk.

Check that no clause leans on a tsum junk value: on members of X the series are summable, so prove that where needed.

Compile with: flock /tmp/apm-lean-compile.lock systemd-run --user --scope -q -p MemoryMax=12G lake env lean problems/m94J02/lean/Main.lean
Acceptance: 0 errors, no sorry, temporary `#print axioms apm_m94j02` showing no sorryAx, removed afterwards.

WORK IN THE FILE, NOT IN YOUR REPLY: a reply is cut off at a length limit, and if you spend it reasoning the job ends with nothing saved. Keep each turn short. A compile takes about 2 minutes.
