# E-kimi-task-176 — prove lemma apm_t02A04_quotientComplex_model_aux_cross in apm-lean, one leaf

**Requisition:** completed — 2026-10-31T18:22:51Z, job invoke-1793470019890-8856-09f086f2, state done

Clocked in by claude-14 for kimi-2 on 2026-10-31 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

From claude-14 (Joe's go, 2026-10-13): prove ONE lemma of an apm-lean blueprint. Other seats are proving the other lemmas of the same problem at the same time, each in its own file.

WHERE
- Worktree ~/worktrees/apm-lean/lemmas (branch `lemmas`; .lake/packages is a symlink, never lake update or rebuild Mathlib). Never touch ~/code/apm-lean.
- Your file: holes/labs/lemmas/P/NAME.lean. It is the problem's Blueprint.lean with your lemma between the lines `-- BEGIN SEAT AREA` and `-- END SEAT AREA`.
- Context: holes/labs/blueprints/P/BLUEPRINT.md (proof sketch, Mathlib names), problems/P/problem.md, informal-solution.md.
- Shared files (2026-10-18): if P is CT.Module, your file is a copy of ConstructionTargets/Module.lean, the proof sketch is ConstructionTargets/Module.blueprint.md, and there is no problems/P; the consumer problems are named in the sketch.
- Compile: `cd ~/worktrees/apm-lean/lemmas && flock /tmp/apm-lean-compile.lock systemd-run --user --scope -q -p MemoryMax=12G lake env lean holes/labs/lemmas/P/NAME.lean`, as often as you like. The flock serialises Lean compiles across seats (disk load, 2026-10-15); keep it. Never run `lake build`.
- A compile of these files takes 2-5 minutes, longer while another seat holds the lock. Append `; echo EXIT=$?` to the command and give the tool call a timeout of at least 15 minutes. A compile counts only when you have seen its `EXIT=` line: no output means the call was cut off, not that the file compiled (2026-10-25: a seat reported PROVED from two cut-off 34-second calls; the file had 9 errors).

RULES (a mechanical checker enforces them; anything else is rejected)
- Edit only between the two markers. Everything outside them must stay byte-identical, including the other lemmas' `sorry`s.
- Inside: replace the `sorry` of NAME with a proof. You may add helper lemmas/defs inside the area, above NAME, with names starting `NAME_aux`. Do not change NAME's statement (anything before its `:= by`).
- No `sorry`, `admit`, `axiom`, `native_decide` in the area. You may not use another sorry'd lemma of the file (it would show sorryAx in NAME's axioms).
- Missing Mathlib theorems: prove them as helpers. Do not stop because Mathlib lacks something; build it.
- Do not commit (other seats share the worktree). Just leave the file.

BLOCKED: if your lemma cannot reasonably be proved without another lemma of the file that is still `sorry` (the blueprint's dependency layers), do not prove that lemma yourself: stop at once and reply `BLOCKED-ON <name>`. It will be re-queued after <name> lands. Facts you can prove in passing are not a reason to block.

DONE when the file compiles and `#print axioms NAME` (add it temporarily, then remove it) shows no sorryAx.
If the episode ends first, leave your best partial file (compiling, with the remaining gap as a `sorry` inside the area) and say exactly what remains. A partial must bank work: state each sub-step you finished as a proved `NAME_aux_*` helper above NAME, and the unfinished sub-steps as separate `NAME_aux_*` lemmas with `sorry`, so the next seat can take them in parallel. Commented-out attempts do not count (two round-1 partials left nothing usable).
PARTIAL QUALITY (Joe, 2026-10-29; several partials that day banked nothing usable):
- NAME itself must not end in `sorry`. Write NAME's proof as the assembly that calls your `NAME_aux_*` leaves, so the only `sorry`s are in leaves. A partial whose main lemma is `sorry` has not shown that its leaves suffice.
- Every banked helper must be USED by that assembly. Do not bank side facts.
- Compile the partial and see `EXIT=0` before replying. Never report a helper you have not compiled.
- No `sorry` followed by a `/- ... -/` proof attempt. If an attempt does not compile, keep only the parts that do, as proved helpers.
- Before stating a leaf, check the file for a lemma that already says it (grep the statement's key names). Use that lemma instead of restating it.
- If you found no usable decomposition, reply PARTIAL with the file untouched and two or three sentences on what you tried and where it failed. That is more useful than thin helpers.

REPLY: bell claude-14: PROVED or PARTIAL, line count of the area, and for PARTIAL the precise remaining goal.
- SEARCH SCOPE (claude-14, 2026-10-15): search only inside ~/worktrees/apm-lean/lemmas, e.g. `rg PATTERN ConstructionTargets holes/labs/blueprints .lake/packages/mathlib/Mathlib`. Never run rg/grep/find over ~ (/home/joe), /, or /tmp: two seats ran rg over /home/joe for an hour and loaded the disk. A lemma you banked in your own seat area is in your own file, so look there.
- NO SORRY IN DEFS (claude-14, 2026-10-15): never write a def/abbrev/instance whose body is or contains sorry. Downstream proofs then carry sorryAx and the checker rejects them. When a construction needs an unproved fact, make that fact an explicit hypothesis argument of the def and prove it as a separate lemma.
- NO PLACEHOLDER LEAVES (claude-14, 2026-10-15): never state a lemma as `True`, or anything else trivially provable, to mark a step. A leaf must state the actual mathematical claim the assembly consumes.

THIS JOB: P = t02A04, NAME = apm_t02A04_quotientComplex_model_aux_cross. Your file: holes/labs/lemmas/t02A04/apm_t02A04_quotientComplex_model_aux_cross.lean. Proof sketch: the docstring above the lemma in that file, and the review notes in holes/labs/blueprints/README.md.
