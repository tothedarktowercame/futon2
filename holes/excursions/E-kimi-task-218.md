# E-kimi-task-218 — statement repair m00A04 in apm-lean: vacuous PDE encoding

**Requisition:** completed — 2026-11-01T23:24:31Z, job invoke-1793572011566-9842-0a26622a, state done

Clocked in by claude-14 for kimi-1 on 2026-11-01 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Statement repair in apm-lean. This problem has a finished, sorry-free proof, but an outside grader graded its statement narrower, so it is not registered and does not count. Close the gap the grader named, without narrowing anything.

Worktree: ~/worktrees/apm-lean/lemmas (branch lemmas). Edit only problems/ID/lean/Main.lean, for the ID below. Do not touch problems/STATEMENTS.json, and do not commit: claude-14 reviews and sends the result for a new outside grade.

Read problems/ID/problem.md, which is the authority, and the grader's row for ID in the grades file named below. Then add one theorem `apm_<id>`, with the id in lower case, at root namespace after the existing development, stating every clause problem.md asks for as a single conjunction, with no hypothesis the problem does not give. Prove it from the existing theorems plus whatever the missing clause needs. Keep the existing lemmas; do not weaken any of them. If the problem as printed turns out false or the gap is larger than described, stop and say so rather than adjust the statement.

Compile with: flock /tmp/apm-lean-compile.lock systemd-run --user --scope -q -p MemoryMax=12G lake env lean problems/ID/lean/Main.lean
Acceptance: 0 errors, no sorry, and a temporary `#print axioms apm_<id>` showing no sorryAx, removed afterwards.

Bell claude-14 back with: PROVED or the obstacle, the theorem text, and the lines added.

Hold the statement to the standard outside graders have applied: scalars not restricted by the original stay complex, Lebesgue measurable is not Borel, every clause is stated, no hypothesis the original lacks, and no clause may lean on Lean's junk values (tsum, or a Bochner integral of a non-integrable function). See holes/labs/statement-registry/triage-1101/ for the triage note on this ID.

The axiom check must show exactly propext, Classical.choice and Quot.sound. native_decide adds another axiom and is not allowed; use decide +kernel or norm_num.

THIS JOB: ID = m00A04. Grades file: holes/labs/statement-registry/backlog-1024/grades-02.md, triage note g2.md. Gap, and this one is serious: `IsCompact (support φ)` on the raw support makes every smooth test function zero in positive dimension, since the raw support is open and a nonempty compact open set in ℝⁿ does not exist. So the encoded H¹₀ collapses and u = g = 0 solves every forcing: the PDE clause is vacuous. The Hilbert minimization part is faithful.

Repair: replace with HasCompactSupport, or compact closure of the support, use a genuine Sobolev completion or a quotient by a.e. equality, and redo the PDE existence proof. Read the original closely. If Mathlib lacks what you need, define H¹₀ as the closure of C_c^∞ in the graph norm inside L² × L²ⁿ, or the completion, as honestly as possible. Prove the Poincaré inequality if the problem needs it, and Lax-Milgram or Riesz for existence.

WORK IN THE FILE, NOT IN YOUR REPLY: a reply is cut off at a length limit, and if you spend it reasoning the job ends with nothing saved. Keep each turn short. Write definitions and statements with sorry first, compile, then fill them one at a time. If you run out, leave every finished lemma proved, and say what is left.
