# T-vscode-02 — the stream conforms to the contract

**Status:** OPEN
Seed: vscode-port
Repo/paths: futon3c `vscode-port`; `test/futon3c/invoke_stream_contract_test.clj` (new)
Depends on: T-vscode-01
## Scoped task
A test that runs the existing invoke-stream handler against a fake agent (as current
transport tests do) and checks each emitted event against the T-vscode-01 schema.
## Acceptance criteria
- [ ] Test passes in the worktree; it fails if an event field the schema requires is removed from the handler (show by a local mutation, reverted).
- [ ] Delivery check: `git -C ~/worktrees/futon3c/vscode-port log -1 --format=%H` names a commit on branch `vscode-port` whose changed paths are all under this ticket's Repo/paths; no other repository has a commit from this ticket.

Campaign: VS Code / code-server port, seeded into the full Giraffe field (Joe, 2026-11-23: "seed in some jvm-agency / etc type of tickets so that we get both the real-world behaviour and the skin-in-the-game port criterion"). Work happens in the futon3c worktree `~/worktrees/futon3c/vscode-port` (branch `vscode-port`), never in a shared checkout; server-side changes are tested in that worktree's own JVM and never loaded into a serving JVM. Interaction inventory and endpoint list: futon3c `emacs/` and `src/futon3c/transport/http.clj`; seams: futon3c `holes/missions/M-futon-seams.md`.
