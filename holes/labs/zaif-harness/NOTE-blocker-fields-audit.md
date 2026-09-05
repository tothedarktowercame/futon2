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
