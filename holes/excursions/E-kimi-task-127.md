# E-kimi-task-127 — HGT-E-D: target-grain E for Clause T, discovery (read-only)

**Requisition:** completed — 2026-09-28T21:08:19Z, job invoke-1790629387980-26330-1eba05d7, state done

**VERDICT (2026-10-09, provisional):** DONE — Requisition completed with state done; HGT-E-D discovery delivered. _(WM status classification by zai-4, high confidence; not yet confirmed by the author.)_

Clocked in by claude-1 for kimi-3 on 2026-09-28 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# HGT-E-D — what would target-grain E be, and where does it come from? (discovery, read only)

From claude-1 (PROOF-2a lead). Bell claude-1 back with a summary. READ ONLY: no edits to any repository, no commits, no tests run, nothing reloaded into any JVM. Write only your report file.

## Why
PROOF-2a plan row ⟨1⟩3, Holes row H-G-target: Clause T's target selection is a mixture posterior p(t) ∝ E_t·e^(−ΔG_t). G is per-target and wired. E is not: `futon2/src/futon2/aif/outer_cascade.clj` lines 9-10, 90-92 and 138-152 admit only `{:basis :uniform-no-data}` ("nothing sums the enactment habit to target grain, and no enactment has passed W_c"). The remaining work named at HEAD is: sum the enactment fold to target-grain E and pass that basis to the selector. Before anyone writes that, I need the facts below. A second packet will implement it after I review this one.

## Read
- futon2 `src/futon2/aif/outer_cascade.clj` (whole file: `mixture-posterior`, `select`, how E enters and is recorded).
- futon2 `src/futon2/aif/enactment_habit.clj`, `enactment_fold_source.clj`, `habit_prior.clj`, `target_field.clj`, and their tests under `test/futon2/aif/`.
- mathlib4 `/home/joe/code/mathlib4/DarkTower/WarMachine/Proof2/EnactmentHabit.lean`, `TargetGrainG.lean`, `TargetUniverse.lean`, and whichever module defines Clause T's `selectionPosterior` (search for it).
- futon2 `holes/labs/wm-contract/PROOF-2a-THEOREM-draft-2026-09-24.md`: Clause T, the Holes rows H-E and H-G-target (around lines 555-565), and anything it says about E at target grain.
- The registry equation row for the enactment habit in futon2 `holes/labs/wm-contract/aif-equations.edn` (key `:enactment-habit` or similar).

## Questions
1. **What the fold holds.** What does the enactment fold record, at what grain (per instance, candidate, policy key, mission, target?), and keyed by what? Quote the record shape from code. Which function produces it and which reads it today?
2. **What the theorem and the Lean say E is at target grain.** Quote the definition, or say there is none. Is E_t a sum, a normalised count, a Dirichlet-style posterior, a pseudo-count prior? What is the prior when a target has no enactment?
3. **The join.** How does an entry in the fold map to a target id in the outer cascade's support? Is there a field on both sides that joins them, and is it the same identity (mission id, target id, path)? If the join needs something that does not exist, say exactly what.
4. **W_c.** "No enactment has passed W_c": what is W_c's check, where is it (file:line), and does the fold admit only enactments that passed it? Is there any recorded enactment anywhere (tracked data, store) that passed it? Count them if you can by reading tracked files; say "not checked" for stores you did not read.
5. **The degenerate case.** With no admitted enactment, what must E be so that today's behaviour (uniform over the support) is preserved exactly? Is that what the Lean definition gives in the empty case?
6. **Test surface.** Which existing tests pin `select`'s posterior and the `:uniform-no-data` record (namespace and deftest names)? Which of them would have to change when E becomes data-derived?

## Rules
- Evidence is file:line or a sha plus quoted text. Say "not found" when you looked and did not find; "not checked" when you did not look. A plan or LOG claim is a claim, not evidence.
- No implementation proposal beyond answering question 5; I will write the implementation packet.

## Output
`/home/joe/code/storage/proof-2a/hgt-e-d/REPORT.md`: one section per question, each claim marked Read. End with a list of anything that contradicts the plan's statement of the remaining work.
