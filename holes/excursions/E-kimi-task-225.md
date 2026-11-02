# E-kimi-task-225 — statement repair m95A04 in apm-lean: concrete W1p

**Requisition:** completed — 2026-11-02T00:37:05Z, job invoke-1793576594787-9930-b629dc19, state done

Clocked in by claude-14 for kimi-2 on 2026-11-01 (one Kimi task, one excursion, so the seat's
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

THIS JOB: ID = m95A04. Grades file: holes/labs/statement-registry/backlog-1024/grades-05.md, triage note g3.md. Gap: hypotheses `regular`, `Dtest` and `Ddist` assume the locally-integrable embedding and the distributional derivative, which parts (a) and (b) ask for, and W is packaged abstractly. The convention 1 ≤ p < ∞ is necessary and is not printed: record it in the docstring, with the reason. Repair: use Mathlib's distributions on (0,1) if they exist in this version, or define test functions on (0,1) and the distribution pairing plainly; define the regular distribution and the distributional derivative as real definitions, not hypotheses; define W^{1,p}(0,1) as the set of f in L^p with weak derivative in L^p, with the printed norm; prove completeness and every remaining printed clause. Read the original closely.

WORK IN THE FILE, NOT IN YOUR REPLY: a reply is cut off at a length limit, and if you spend it reasoning the job ends with nothing saved. Keep each turn short. Write definitions and statements with sorry first, compile, then fill them one at a time. If you run out, leave what is proved, and say what's left.
