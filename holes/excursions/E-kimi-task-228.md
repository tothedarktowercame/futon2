# E-kimi-task-228 — statement repair m96J05 in apm-lean: physical difference operators

**Requisition:** completed — 2026-11-02T00:38:27Z, job invoke-1793579270000-9994-9b858c79, state failed

Clocked in by claude-14 for kimi-1 on 2026-11-02 (one Kimi task, one excursion, so the seat's
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

THIS JOB: ID = m96J05. Grades file: holes/labs/statement-registry/backlog-1024/grades-06.md, held.md, triage note g3.md. Gap: Lean works wholly in Fourier coordinates and assumes `hD` and `hT`, the multiplier identities for the finite difference and translation, which carry the main content. It never defines the physical centered difference or Fourier conjugation. The printed text has a missing parenthesis: read it as (f(x+ε) - f(x-ε))/(2ε), and record that as a misprint in the docstring.

Repair: define the translation and centered-difference operators on L²(ℝ) physically, and prove their Fourier multipliers from the Fourier transform on L² in this Mathlib version: first search for what it has (`MeasureTheory.Lp.fourierTransform`, Plancherel, `Real.fourierIntegral_comp_add_right` or similar). If Mathlib has no L² Fourier transform, state so plainly and stop, reporting what would have to be built. Then re-derive the printed clauses without `hD` and `hT`.

WORK IN THE FILE, NOT IN YOUR REPLY: a reply is cut off at a length limit, and if you spend it reasoning the job ends with nothing saved. Keep each turn short. Write definitions and statements with sorry first, compile, then fill them one at a time. If you run out, leave what is proved, and say what's left.
