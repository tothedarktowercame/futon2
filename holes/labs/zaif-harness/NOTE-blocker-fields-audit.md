# Audit note: S4-S7 assert :blocked with no :blocker field (2026-09-05)

From claude-1 (a bell to claude-2 was attempted first and failed
agent-not-found). Found by claude-7's audit, relaying Joe.

Rows S4, S5, S6, S7 on this board are `:status :blocked` with **no
`:blocker` field** (S4 carries `:depends-on [S2]`; the other three carry
nothing). The conventions rule (futon2/holes/N-process-trap-recording-
conventions.md) — a blocked/absence claim carries its evidence — fails on
these four rows on this board, while the wm-contract board honours it
exhaustively.

This now has a surfacing consequence: needs-joe in voxterm's backlog panel
and in the bulletin reads ruling-shaped `:blocker` TEXT (voxterm server.py
+ futon2 bulletin.clj, commits of 2026-09-05). A row blocked on a ruling
of Joe's with no `:blocker` field is invisible to that channel.

Action for this lane's owner: backfill the four `:blocker` fields with the
measured reason (or re-open the rows if the reason no longer holds).

## Correction (2026-09-06, claude-1)

The diagnosis above was wrong, and the error mirrors the surfacing bug the
note was written about: S4-S7 did NOT lack evidence for their blocked state
-- each carried a `:moved-to` field with a ruling-shaped reason (Joe
2026-09-03: queue on wm-build-loop) and a declared closing rule ("closes
when that closes"). The audit read `:blocker` and stopped; the evidence
lived one field over. Found by claude-2 while seeding the Box-12 track
(futon2 35d6d644, S4 :blocker text).

The real defect was different: every `:moved-to` target had since closed
(U21/U23/U25 done+reviewed; U22/U24 superseded by J7/J8, done on Joe's
rulings), so all four conditions were MET and merely unexecuted. Closed
2026-09-06 by claude-1 executing each row's own recorded rule; blockers
renamed `:blocker-history`.

Lesson, same shape as [[surfacing-reads-blocker-fields]]: a reader that
checks ONE conventional field treats every other field as absence. The
convention is "an absence claim carries its evidence" -- but the auditor
must read where the evidence is allowed to live, not where it usually does.
