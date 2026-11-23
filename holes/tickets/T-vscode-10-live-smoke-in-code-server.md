# T-vscode-10 — live smoke in code-server

**Status:** OPEN
Seed: vscode-port
Gate: awaiting Joe (installs the extension into the running code-server and invokes a real seat)
Depends on: T-vscode-04 to T-vscode-09
## Scoped task
Install the `.vsix` into code-server at zone `/code/`, open the roster, send one prompt to a
test seat Joe names, see the reply stream and the job appear in the jobs view.
## Acceptance criteria
- [ ] Screenshot or Playwright trace of the reply and the job, plus the job id in `GET /api/alpha/invoke/jobs`.

Campaign: VS Code / code-server port, seeded into the full Giraffe field (Joe, 2026-11-23: "seed in some jvm-agency / etc type of tickets so that we get both the real-world behaviour and the skin-in-the-game port criterion"). Work happens in the futon3c worktree `~/worktrees/futon3c/vscode-port` (branch `vscode-port`), never in a shared checkout; server-side changes are tested in that worktree's own JVM and never loaded into a serving JVM. Interaction inventory and endpoint list: futon3c `emacs/` and `src/futon3c/transport/http.clj`; seams: futon3c `holes/missions/M-futon-seams.md`.
