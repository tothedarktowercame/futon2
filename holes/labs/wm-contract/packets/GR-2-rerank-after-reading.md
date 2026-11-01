# GR-2 — Giraffe: re-rank after the reading stage demotes a task (claude-2 → codex-5, after GR-1)

Authority: Joe, 2026-11-01 ~21:10Z (field confusion is the priority). Repo futon3c, same worktree
rules as GR-1. Mode: work. One behaviour.

## The defect

`pass.clj` sorts open items `[ready? 0/1, −unblocks, −cone, source-order]` and assigns `:rank`.
`excursions.clj combined-pass` then rewrites rows to `:undetermined` with `:cone nil :unblocks nil`
**and keeps the rank**. In pass `20261101T195517.913Z-c38bc009`, 42 of the 52 undetermined rows hold
ranks inside the ready region; `M-interim-director` is rank 1 with no unblocks and no cone. The
`find-out-before-building` cascade (`{:status-gate :undetermined}` then `:by-rank`) therefore
picks it on every click.

## Goal

After the reading stage, re-sort the rows with the same key over the **post-reading** status:
ready first by −unblocks, −cone, source order; undetermined next by source order; not-ready last by
source order; ranks reassigned 1..n. `:reason` for undetermined rows stays `:needs/dependency-reading`.
Record `:rank-basis :after-reading` on every row so a consumer can tell the two conventions apart.

## Tests

1. On this pass: no undetermined row has a rank lower than any ready row; `M-interim-director`'s
   rank > the number of ready rows.
2. Ready rows keep their relative order from the base pass.
3. `report.md` "Giraffe order" column agrees with `:order`.

Gates as GR-1. Bell claude-2 with sha, receipt, and the new top ten.
