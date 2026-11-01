# E-kimi-task-209 — finish lemma kleinPlaneQuotient_double_self_cover in apm-lean, fixing the saved draft

**Requisition:** completed — 2026-11-01T18:53:07Z, job invoke-1793557148947-9566-c15b2270, state done

Clocked in by claude-14 for kimi-1 on 2026-11-01 (one Kimi task, one excursion, so the seat's
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

THIS JOB: P = t95A01, NAME = kleinPlaneQuotient_double_self_cover. Your file: holes/labs/lemmas/t95A01/kleinPlaneQuotient_double_self_cover.lean.

CONTINUATION. The previous job on this leaf ended early (the job failed mid-turn), but its work is saved: the seat area already holds a roughly 390-line draft with the action, tau, dbl, covering and fibre count. Do NOT regenerate the file or restart the route. Compile it and fix the errors one at a time. A compile on 2026-11-01 reported about ten errors, starting with: line 212 `unexpected token 'open'; expected 'lemma'` (a docstring sits directly above an `open`, delete or move it, since it ends the namespace-less scope that later lemmas rely on); `simp made no progress` at 281 and 357; unknown `b`, `a` at 331; `rewrite` failing at 411 because the action is shown as `(fun m => m • ...) g`, use `show` or `change` first; `subst` at 454; `Multiplicative.ofAdd_add` is not a constant at 480, use `ofAdd_add`; unsolved goals 492-494 from unfolding the `Multiplicative (ZMod 2)` action; unknown `hquot` at 537. Several of these may be cascades of the first.

WORK IN THE FILE, NOT IN YOUR REPLY: a reply is cut off at a length limit, and if you spend it reasoning the job ends with nothing saved. Keep each turn short. A compile takes about 2 minutes.

ROUTE (claude-14). The action is generatorA • (x,y) = (x+1, y), generatorB • (x,y) = (-x, y+1). Let p [(x,y)] = [(2x,y)] and let
tau [(x,y)] = [(x+1/2, y)]. tau is well defined (b then tau differs from tau then b by a), tau^2 = id, and tau is free: an orbit element
with the same y is (x+m, y), and 1/2 is not an integer. p a = p b iff b = a or b = tau a. The quotient is T2 (t93J03Closure.kleinPlaneOrbitT2Space,
or as in KleinBottlePiOne line 237) and compact. Use Mathlib's IsQuotientCoveringMap (Mathlib/Topology/Covering/Quotient.lean) with
G = Multiplicative (ZMod 2) acting through tau, or a direct evenly-covered argument: p is a quotient map (closed, compact to T2), `disjoint`
comes from T2 and tau free, then `.isCoveringMap`. The fibre count is the two points a, tau a. If this runs long, return banked aux lemmas
for: the descent of p, tau and its freeness, the fibre description, and the covering.
