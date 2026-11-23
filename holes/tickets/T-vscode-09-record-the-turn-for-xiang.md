# T-vscode-09 — record the turn for 象

**Status:** OPEN
Seed: vscode-port
Parent: M-futon-seams (instance 7)
Repo/paths: as T-vscode-05
Depends on: T-vscode-05
## Scoped task
Reuse `packages/xiang-client`: record the turn at send time and post `happened` at reply end
through the existing `/api/alpha/xiang/*` routes; render the returned marks in the panel. No
client-side recognition.
## Acceptance criteria
- [ ] Test with a fake server: one record call before the stream, one `happened` after; marks from the response appear.
- [ ] Delivery check: `git -C ~/worktrees/futon3c/vscode-port log -1 --format=%H` names a commit on branch `vscode-port` whose changed paths are all under this ticket's Repo/paths; no other repository has a commit from this ticket.

Campaign: VS Code / code-server port, seeded into the full Giraffe field (Joe, 2026-11-23: "seed in some jvm-agency / etc type of tickets so that we get both the real-world behaviour and the skin-in-the-game port criterion"). Work happens in the futon3c worktree `~/worktrees/futon3c/vscode-port` (branch `vscode-port`), never in a shared checkout; server-side changes are tested in that worktree's own JVM and never loaded into a serving JVM. Interaction inventory and endpoint list: futon3c `emacs/` and `src/futon3c/transport/http.clj`; seams: futon3c `holes/missions/M-futon-seams.md`.
