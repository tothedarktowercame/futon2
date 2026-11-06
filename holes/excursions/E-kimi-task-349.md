# E-kimi-task-349 — T-giraffe-undetermined-triage group B

**Requisition:** in-progress — dispatched 2026-11-06T07:08:45Z to kimi-2 as invoke-1793948925719-13717-a1cef2e7

Clocked in by claude-6 for kimi-2 on 2026-11-06 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# Undetermined triage, group B (claude-6 -> kimi-2, for Joe)

Read first: `/home/joe/code/futon2/holes/tickets/T-giraffe-undetermined-triage.md`.
It holds Joe's request, the item list (your group is **Group B**), and the
per-item format. A worked example of the outcome Joe wants is
`/home/joe/code/futon2/holes/tickets/T-formal-patterns-unblock.md` (its
questions and Joe's answers).

For each item in Group B:
1. Find its document (the Giraffe reading names the source path: search
   `/home/joe/notes/wm-next/giraffe-reads-2026-11-04/corpus/*.jsonl` for
   `"id": "<item>"`; or `find /home/joe/code -path '*/holes/*' -name '<item>.md'`).
2. Read it, its reading record (edges, unresolved), and enough of what it
   points to, to give the verdict. Check git log of the files it names for
   recent activity.
3. Write the item's entry under "## Group B" in the ticket, in the per-item
   format. Questions for Joe are technical and plain: no "may I", no shortcodes
   without a one-line explanation of what they are.

Constraints: discovery only. Edit only the Group B section of the ticket and
commit it with `git commit --only -- holes/tickets/T-giraffe-undetermined-triage.md`
in /home/joe/code/futon2. Other seats edit other sections of the same file at
the same time: before committing, re-read the file and apply your section to
the current content; never overwrite another group's section; if git refuses,
retry once after re-reading, then report. No other edits, no code, no run data,
no /tmp for anything kept.

Bell claude-6 back with one line per item: item, verdict, and the question
count.
