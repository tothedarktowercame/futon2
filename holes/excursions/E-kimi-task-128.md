# E-kimi-task-128 — PROGRESS-D: intermediate-progress checker, discovery (read-only)

**Requisition:** completed — 2026-09-28T21:09:41Z, job invoke-1790629428341-26336-72955f7a, state done

Clocked in by claude-1 for kimi-2 on 2026-09-28 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# PROGRESS-D — the intermediate-progress checker: what must it check, on what record? (discovery, read only)

From claude-1 (PROOF-2a lead). Bell claude-1 back with a summary. READ ONLY: no edits to any repository, no commits, no tests run, nothing reloaded into any JVM. Write only your report file.

## Why
PROOF-2a plan row ⟨1⟩4: Joe settled (2026-09-27 14:36Z, decision item 3, P6) that "the progress criterion is the theorem's clause, 'produces a token later consumed'; the checker is built in ⟨2⟩5/⟨3⟩2, distinct from flight.clj's wants-flip". An audit at HEAD found no such checker in futon2 or futon3c. Before anyone writes it I need the facts below; an implementation packet follows my review.

## Read
- futon2 `holes/labs/wm-contract/PREFERENCE-AUDIT.md` (lines 30-80, the P6 row and its authority marks).
- futon2 `holes/labs/wm-contract/PROOF-2a-THEOREM-draft-2026-09-24.md`: the clause that states intermediate progress (search "later consumed", "progress", "token").
- futon2 `holes/labs/wm-contract/PROOF-2a-PLAN.md`: steps ⟨1⟩4, ⟨2⟩5, ⟨3⟩2 (their ACCEPT lines).
- futon2 `src/futon2/aif/flight.clj` around lines 432-448 (the existing false-to-true want movement check, which the checker must be distinct from), `src/futon2/aif/construction.clj` 125-170 (tokens produced and needed; containment order), and the flight run record writer (`flight_runner.clj`, `record-summary`).
- mathlib4 `/home/joe/code/mathlib4/DarkTower/WarMachine/Proof2/`: any definition of tokens produced/consumed or progress (search "produce", "consum", "progress").

## Questions
1. **The criterion, quoted.** The exact wording of the progress clause in the theorem and in Joe's ruling. What is "a token", "produced" and "later consumed" in the machine's own terms (quote definitions from code or Lean)?
2. **Where the evidence would be.** Which record(s) of a flight carry which tokens each step produced and which each later step consumed (guards read)? Quote the field paths from the writer code. If a flight record does not carry what the check needs, say exactly what is missing.
3. **Existing near-misses.** How does flight.clj's wants-flip check differ from this criterion? Is there any other code that already pairs a produced token with a later consumer (e.g. in construction.clj's containment order)? file:line.
4. **Inputs and outputs of the checker.** From 1-3: what must the checker read, and what should its verdict distinguish (e.g. progress witnessed / no token produced / produced but never consumed / record lacks the fields)? Only what the sources support; say where they are silent.
5. **Test surface.** Is there any tracked flight record (futon2 `data/`, `runs/`, `holes/labs/M-wm-wiring/spike/`, futon3c fixtures) that the checker could be run on? List paths and say for each whether it carries the needed fields.

## Rules
- Evidence is file:line or a sha plus quoted text. "not found" when you looked and did not find; "not checked" when you did not look. A plan or LOG claim is a claim, not evidence.
- No implementation beyond question 4's statement of inputs and verdicts.

## Output
`/home/joe/code/storage/proof-2a/progress-d/REPORT.md`: one section per question, each claim marked Read.
