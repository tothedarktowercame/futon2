# E-kimi-task-9 — non-author warrants for the AR-42 commits (b6e8f76e, 61ab13eb)

**Requisition:** completed — 2026-09-25T01:09:27Z, job invoke-1790298043815-24081-9b3b89d0, state done

Clocked in by claude-8 for kimi-2 on 2026-09-25 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

From claude-8 (PROOF-2 coordinator). Read-only shared checkouts: futon2 is `/home/joe/code/futon2` on `main`, mathlib4 is `/home/joe/code/mathlib4`. Other lanes have dirty files there: stage ONLY the explicit paths you create, never `git commit -a`, never `git stash`, never amend. Do not fire WM clicks, do not write under `data/`, do not `load-file` anything into a shared JVM. No justification may appeal to an operator ruling or to who said what; settle by definitions, code, and records, or say the definition is missing and propose it as an amendment. Absence is a typed absence on the record, never a substituted value. When done, bell claude-8 back with a two-paragraph summary and the commit sha(s).

Governing documents (all under `futon2/holes/labs/wm-contract/`): `PROOF-2-ASSUME-draft-2026-09-24.md` (A1-A21), `PROOF-2-THEOREM-draft-2026-09-24.md` (W0-W6, R1-R8), `PROOF-2-STRATEGY-draft-2026-09-24.md` (packet rows), `proof2/packets/CERT-S.md` (certificate key paths and hashing).
## Registration packet: non-author warrants for the AR-42 commits
You are registering test runs for commits you did not author (author/reviewer separation; read `holes/labs/wm-contract/NOTE-test-registry.md` if new to the registry). futon2: `AUTHOR=<your-id> scripts/wm/register-warrant.sh --pinned b6e8f76e futon2.aif.observation-checks-test`. futon3c: the same script from /home/joe/code/futon3c if it exists there, else the registry's own run subcommand (read scripts/wm/ and futon3c/src/futon3c/test_registry.clj `-main` to find how futon3c namespaces are registered; claude-9 registered one at 61ab13eb, so a recorded command exists in the ledger to copy), for `futon3c.test-registry-test` (or the namespace 30335825/61ab13eb's stat names) pinned at 61ab13eb. Then answer, quoting test source at b6e8f76e: (1) is it asserted that a :scan-window-exhausted answer from the endpoint makes C8 REFUSE (kind?) rather than observe false; (2) is it asserted that a record returned by the lookup naming a DIFFERENT namespace reads :namespace-mismatch. And at 61ab13eb: (3) is the two-runs case (older warranted, newer failing, newer returned) asserted with real minted records or synthetic ones. No edits, no clicks, no JVM loads, no writes under data/. Bell claude-8 with warrant ids and answers.
