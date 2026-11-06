# E-kimi-task-347 — T-formal-patterns-unblock: make M-formal-patterns next items selectable, questions for Joe

**Requisition:** in-progress — dispatched 2026-11-06T06:23:28Z to kimi-1 as invoke-1793946208261-13623-2f819ee8

Clocked in by claude-6 for kimi-1 on 2026-11-06 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# T-formal-patterns-unblock (claude-6 -> Kimi seat, for Joe)

Read first: `/home/joe/code/futon2/holes/tickets/T-formal-patterns-unblock.md`.
It holds Joe's definition (verbatim), the starting state, and the outcome
required. Joe's words govern; this packet adds procedure only.

Goal: M-formal-patterns' next items become concretely available for War
Machine selection, and Joe gets clear technical questions for what only he can
decide. Do NOT do the items themselves.

Read:
- `/home/joe/code/futon5/holes/M-formal-patterns.md` (whole file; note the
  acceptance checklist at the top, the slices S0-S4, the freeze section).
- `/home/joe/code/futon5/TN-baldwin-reboot.md` §10 (the locked register),
  and the sections it points to for the open items: §14 (N2b), §19 and §21
  (N3), §15 (H1 re-scope, done), H2 and H5 rows. Also check `git log` in futon5
  for today's commits b0b8e37, ac8a057, 84409a9.
- The current Giraffe reading of the mission: line 2 of
  `/home/joe/notes/wm-next/giraffe-reads-2026-11-04/corpus/batch-old-52.jsonl`.
- The reading procedure:
  `/home/joe/code/futon2/holes/labs/wm-contract/reconciliation/Q1-GIRAFFE-FIELD-STALENESS-zai6-2026-10-31.md`
  and `/home/joe/notes/wm-next/giraffe-reads-2026-11-04/REPORT.md`.

Do:
1. For each open register item (N2b, N3, H2, H5, and anything else not
   discharged), find out what concretely blocks it. Write numbered questions
   for Joe in the ticket's "Questions for Joe" section. Each question: the
   decision, the options (at least two, stated in technical terms), what each
   option changes (files, paper sections, experiments), and the evidence
   (file:line). No "may I", no permission requests.
2. Make the mission self-contained about the freeze: in
   `futon5/holes/M-formal-patterns.md`, under the freeze heading, add a short
   paragraph defining slice, Nightmare (N), Headache (H), and a table of the
   register items with their current status copied from TN §10, with the date.
   Keep the freeze notice text; do not lift the freeze (that is Joe's decision
   and should be one of your questions if it is one).
3. Restate the acceptance checklist so the WM can select the next work: each
   item not blocked on Joe is an unchecked box with a concrete locator (file,
   test, or command that shows it done). Items blocked on Joe say
   "blocked: question N in T-formal-patterns-unblock". Do not tick anything
   you did not verify. Do not add a `**Gate:**` line (that removes the whole
   mission from selection), unless after step 1 nothing at all is available,
   in which case say so in the ticket and stop before adding it.
4. Record whether 84409a9 (S4 translation) was inside or outside the freeze
   scope, as a fact with evidence, in the ticket's Result section. Do not revert it.
5. Prepare a replacement Giraffe reading record for M-formal-patterns that
   follows the Q1 procedure (new source sha, every quote re-verified, edges
   resolved where the mission now names task documents; a dependency on a TN
   stays unresolved only if it still is). Write it to
   `/home/joe/notes/wm-next/formal-patterns-unblock/reading-M-formal-patterns.jsonl`
   (one line). Do NOT edit the live corpus: clicks read it while running;
   claude-6 promotes it between clicks.
6. Fill the ticket's Result section: commits, the staged reading path, and
   what the next selectable items are.

Constraints:
- Commit in futon5 only `holes/M-formal-patterns.md`, and in futon2 only
  `holes/tickets/T-formal-patterns-unblock.md`, each with
  `git commit --only -- <path>`. The WM may commit in futon5 while you work:
  never reset, stash or rebase others' work; if your commit conflicts, stop
  and report.
- Do not edit TN-baldwin-reboot.md, any code, Lean, run data, or
  `~/code/storage/giraffe`.
- No /tmp for anything you keep.

Bell claude-6 back with: the questions (numbered, short), the commit shas, and
the staged reading path.
