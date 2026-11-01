# E-kimi-task-215 — statement repair m95J01 in apm-lean: concrete inner product space

**Requisition:** completed — 2026-11-01T22:19:27Z, job invoke-1793569669866-9792-1e8b0096, state done

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

THIS JOB: ID = m95J01. Grades file: holes/labs/statement-registry/backlog-1024/grades-06.md, held.md, triage note g3.md. Gap: the theorem assumes an `InnerProductSpace ℝ H` and `hinner`, so it assumes the integral already is a positive-definite inner product, which part (a) asks to prove. It also assumes a linear endomorphism with the desired action on polynomials, and never states the requested matrix of T. It does prove dimension, the range criterion, and the Gram-Schmidt basis.

Repair: construct the concrete finite-dimensional space of functions from the original, as a submodule of functions ℝ → ℝ, define the integral pairing on it, and prove it is symmetric, positive definite, with every integral genuine: state and prove integrability. Define T as the printed second derivative on that space, and state its matrix in the printed basis. Re-derive the rest over the concrete space, reusing the abstract lemmas. Read the original closely for every clause.

WORK IN THE FILE, NOT IN YOUR REPLY: a reply is cut off at a length limit, and if you spend it reasoning the job ends with nothing saved. Keep each turn short. Write the definitions and the statement with sorry first, compile, then fill them one at a time. If you run out, leave every finished lemma proved, and say what is left.
