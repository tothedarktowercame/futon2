# E-kimi-task-210 — statement repair a02J07 in apm-lean: piecewise smooth paths

**Requisition:** completed — 2026-11-01T21:43:34Z, job invoke-1793567254147-9711-58801d8f, state done

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

THIS JOB: ID = a02J07. Grades file: holes/labs/statement-registry/backlog-1024/grades-11.md, triage note g1.md. Edit only problems/a02J07/lean/Main.lean. Keep the existing `SlitPath` theorems and add beside them.

Gap: `SlitPath` assumes differentiability at every point of (0,1] and interval integrability of deriv gamma / gamma. The original says only piecewise smooth. Two Codex attempts stopped at this size of work and rightly would not add a disguised boundedness field. This is a long proof to grind, not a hard idea.

Plan, in order, keeping each step a separate lemma:
1. `PiecewiseSmoothSlitPath`: keep the start and avoidance conditions word for word from `SlitPath`, ContinuousOn on Icc 0 1, a strictly increasing partition `t : Fin (n+1) → ℝ` of [0,1], and for each piece a C¹ function agreeing with gamma on that closed piece. Nothing else; no bound, no integrability.
2. Finite corner set; off it gamma has a derivative equal to that of the matching piece, and `deriv gamma` is continuous there.
3. A uniform bound on `deriv gamma` off the corners, from compactness of each closed piece.
4. `‖gamma t‖` bounded below on [0,1], by compactness and nonvanishing, including gamma 0 = π.
5. AEStronglyMeasurable of `deriv gamma` on the interval: `measurable_deriv` works for any function into a complete space.
6. IntervalIntegrable of deriv gamma / gamma, from 3, 4, 5.
7. The FTC step: `MeasureTheory.integral_eq_of_hasDerivAt_off_countable` in Mathlib/MeasureTheory/Integral/DivergenceTheorem.lean, with the corners as the countable exception set. Adapt the existing `logIntegral_im_eq_argStar_diff` and `logIntegral_im_bounds` for the new structure.
8. `apm_a02j07` stating the original with the new structure.

WORK IN THE FILE, NOT IN YOUR REPLY: a reply is cut off at a length limit, and if you spend it reasoning the job ends with nothing saved. Keep each turn short. Draft the structure and lemma statements with sorry within your first few turns, compile, then fill them one at a time.
