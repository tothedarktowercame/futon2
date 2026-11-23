# T-vscode-04 — extension scaffold and agent roster

**Status:** OPEN
Seed: vscode-port
Repo/paths: futon3c `vscode-port`; `packages/futon-agency-vscode/`
Depends on: T-vscode-03
## Scoped task
A VS Code extension (TypeScript, built like `packages/xiang-client`) with one command,
"FUTON: Agents", showing a tree of `GET /api/alpha/agents` (id, provider, status). Agency
base URL from a setting, default `http://localhost:7070`.
## Acceptance criteria
- [ ] `npm ci && npm test` passes in the package; the roster test uses a fake fetch with a recorded fixture.
- [ ] `npx vsce package` (or equivalent) produces a `.vsix`.
- [ ] Delivery check: `git -C ~/worktrees/futon3c/vscode-port log -1 --format=%H` names a commit on branch `vscode-port` whose changed paths are all under this ticket's Repo/paths; no other repository has a commit from this ticket.

Campaign: VS Code / code-server port, seeded into the full Giraffe field (Joe, 2026-11-23: "seed in some jvm-agency / etc type of tickets so that we get both the real-world behaviour and the skin-in-the-game port criterion"). Work happens in the futon3c worktree `~/worktrees/futon3c/vscode-port` (branch `vscode-port`), never in a shared checkout; server-side changes are tested in that worktree's own JVM and never loaded into a serving JVM. Interaction inventory and endpoint list: futon3c `emacs/` and `src/futon3c/transport/http.clj`; seams: futon3c `holes/missions/M-futon-seams.md`.
