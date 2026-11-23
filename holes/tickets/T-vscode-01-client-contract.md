# T-vscode-01 — client contract for chat (schema and fixtures)

**Status:** OPEN
Seed: vscode-port
Parent: M-futon-seams (instance 7)
Repo/paths: futon3c `vscode-port`; `resources/agency-client-contract/` (new), `test/futon3c/agency_client_contract_test.clj` (new)
Depends on: none
## Scoped task
Write a versioned schema (EDN, Malli or plain predicates, as futon3c already uses) for
`ChatSession`, `TranscriptEvent`, `SessionStatus`, `InboundTurn`, `DeliveryStatus`, and three
fixtures recorded from current `POST /api/alpha/invoke-stream` and `GET /api/alpha/agents`
output (recorded by GET or from existing test fixtures; no live invoke).
## Acceptance criteria
- [ ] `clojure -M:test -n futon3c.agency-client-contract-test` passes in the worktree and checks every fixture against the schema.
- [ ] A deliberately broken fixture (an event with no `:type`) is rejected by the test (construct the bad case).
- [ ] Delivery check: `git -C ~/worktrees/futon3c/vscode-port log -1 --format=%H` names a commit on branch `vscode-port` whose changed paths are all under this ticket's Repo/paths; no other repository has a commit from this ticket.
Excluded: no change to `transport/http.clj` or any route.

Campaign: VS Code / code-server port, seeded into the full Giraffe field (Joe, 2026-11-23: "seed in some jvm-agency / etc type of tickets so that we get both the real-world behaviour and the skin-in-the-game port criterion"). Work happens in the futon3c worktree `~/worktrees/futon3c/vscode-port` (branch `vscode-port`), never in a shared checkout; server-side changes are tested in that worktree's own JVM and never loaded into a serving JVM. Interaction inventory and endpoint list: futon3c `emacs/` and `src/futon3c/transport/http.clj`; seams: futon3c `holes/missions/M-futon-seams.md`.
