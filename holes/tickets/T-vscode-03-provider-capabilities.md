# T-vscode-03 — provider capabilities without parsing agent ids

**Status:** OPEN
Seed: vscode-port
Parent: M-futon-seams (instance 4)
Repo/paths: futon3c `vscode-port`; the agents route handler and its test
Depends on: T-vscode-01
## Scoped task
`GET /api/alpha/agents` includes, per agent, the capabilities a client needs (`:stream`,
`:interrupt`, `:restore`, `:reset`) taken from the registry, so no client branches on a
`claude-`/`codex-`/`zai-` prefix.
## Acceptance criteria
- [ ] Handler test shows capabilities for one seat of each provider in the registry fixture.
- [ ] Existing agents-route tests still pass.
- [ ] Delivery check: `git -C ~/worktrees/futon3c/vscode-port log -1 --format=%H` names a commit on branch `vscode-port` whose changed paths are all under this ticket's Repo/paths; no other repository has a commit from this ticket.
Excluded: no change to how seats are registered or invoked.

Campaign: VS Code / code-server port, seeded into the full Giraffe field (Joe, 2026-11-23: "seed in some jvm-agency / etc type of tickets so that we get both the real-world behaviour and the skin-in-the-game port criterion"). Work happens in the futon3c worktree `~/worktrees/futon3c/vscode-port` (branch `vscode-port`), never in a shared checkout; server-side changes are tested in that worktree's own JVM and never loaded into a serving JVM. Interaction inventory and endpoint list: futon3c `emacs/` and `src/futon3c/transport/http.clj`; seams: futon3c `holes/missions/M-futon-seams.md`.
