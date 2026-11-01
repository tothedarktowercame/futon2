# GR-1 — Giraffe: a completed prerequisite is met, not "out of scope" (claude-2 → codex-5, 2026-11-01)

Authority: Joe, 2026-11-01 ~21:10Z: "if the field is confused we should fix that as a matter of
priority. Giraffe was supposed to do that. I guess it is not totally done." Repo: futon3c
(`master`; worktree `~/worktrees/futon3c/codex5-gr-1`, branch `codex5/gr-1`; never check out a branch
in `/home/joe/code/futon3c`, it is the serving JVM's source). Mode: work. One behaviour.

## The defect, from pass `~/code/storage/giraffe/20261101T195517.913Z-c38bc009`

`futon3c/src/futon3c/giraffe/excursions.clj` `combined-pass` (~line 176–184) folds every
`depends-on` row of `(:out-of-scope reading-evidence)` into `missing`, so the reading member becomes
`:undetermined` with `:absence :out-of-scope-prerequisite`. 42 of the 52 undetermined tasks are
undetermined for this reason. Of their 33 distinct prerequisite targets, resolved against
`input.edn :items` by id: **14 are `:completed`, 5 are `:cancelled`, 14 are absent from the
population**. Worked case: `M-interim-director` is undetermined because its prerequisite
`M-interest-network-coupling` is "out of scope"; that mission's header reads
`**Status:** CLOSED — CLOSED 2026-05-29` (`futon4/holes/missions/M-interest-network-coupling.md`).
A met dependency is being read as unknown, and it holds rank 1 of the whole field.

## Goal

A prerequisite target that the population knows as `:completed` is **met**: it does not enter
`missing`, and the reading member's readiness is computed from its remaining prerequisites. A
target known as `:cancelled` keeps the member `:undetermined` with the typed reason
`:prerequisite-cancelled` (whether a cancelled predecessor blocks is Joe's call; do not guess).
A target absent from every repo root stays `:out-of-scope-prerequisite`. Each resolution is
recorded on the member's `:graph-terms :prerequisites` row as `:resolution :met | :cancelled |
:absent` with the target's `:source-pin` when known.

Find out first and say in the report: why `M-interest-network-coupling` is absent from
`input.edn :items` although `futon4` is in `missions/default-repos` (is `holes/missions/` scanned?
are CLOSED missions dropped before the reading?). If closed missions never reach `:items`, the
resolution must read the target's status from its file (pinned) rather than from `:items`.

## Tests (one live pin each, from this pass's `input.edn`)

1. `M-interim-director`'s prerequisite resolves `:met` and the member is no longer undetermined
   for that reason (it may still be undetermined or ready for other reasons; assert the reason).
2. A member whose only prerequisite is a `:cancelled` item → `:undetermined`,
   `:reason :prerequisite-cancelled`.
3. A member whose prerequisite is absent from every root → unchanged `:out-of-scope-prerequisite`.
4. Report line: undetermined count before/after on this pass (expected 52 → about 38 or fewer).

Gates: clj-kondo, `futon4/dev/check-parens.el`, `clojure -M:test -n futon3c.giraffe.<touched>`
(the two `t3-frozen-608` failures in `ticket-admission-test` are pre-existing). Commit on your
branch; no merge, no reload, no `storage/giraffe` writes. Bell claude-2 with the sha, the test
receipt, and the before/after counts.
