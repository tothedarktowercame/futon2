# T-vscode-07 — jobs view

**Status:** OPEN
Seed: vscode-port
Repo/paths: as T-vscode-05
Depends on: T-vscode-04
## Scoped task
A tree of `GET /api/alpha/invoke/jobs` for a chosen agent; selecting a job shows
`GET /api/alpha/invoke/jobs/:id` (state, events).
## Acceptance criteria
- [ ] Hermetic test with recorded job fixtures.
- [ ] Delivery check: `git -C ~/worktrees/futon3c/vscode-port log -1 --format=%H` names a commit on branch `vscode-port` whose changed paths are all under this ticket's Repo/paths; no other repository has a commit from this ticket.

Campaign: VS Code / code-server port, seeded into the full Giraffe field (Joe, 2026-11-23: "seed in some jvm-agency / etc type of tickets so that we get both the real-world behaviour and the skin-in-the-game port criterion"). Work happens in the futon3c worktree `~/worktrees/futon3c/vscode-port` (branch `vscode-port`), never in a shared checkout; server-side changes are tested in that worktree's own JVM and never loaded into a serving JVM. Interaction inventory and endpoint list: futon3c `emacs/` and `src/futon3c/transport/http.clj`; seams: futon3c `holes/missions/M-futon-seams.md`.
