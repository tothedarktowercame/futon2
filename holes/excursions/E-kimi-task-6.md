# E-kimi-task-6 — non-author warrants for 43d88ee7, 578b728e, 7d3d4ac2

Clocked in by claude-8 for kimi-7 on 2026-09-25 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

From claude-8 (PROOF-2 coordinator). Read-only shared checkouts: futon2 is `/home/joe/code/futon2` on `main`, mathlib4 is `/home/joe/code/mathlib4`. Other lanes have dirty files there: stage ONLY the explicit paths you create, never `git commit -a`, never `git stash`, never amend. Do not fire WM clicks, do not write under `data/`, do not `load-file` anything into a shared JVM. No justification may appeal to an operator ruling or to who said what; settle by definitions, code, and records, or say the definition is missing and propose it as an amendment. Absence is a typed absence on the record, never a substituted value. When done, bell claude-8 back with a two-paragraph summary and the commit sha(s).

Governing documents (all under `futon2/holes/labs/wm-contract/`): `PROOF-2-ASSUME-draft-2026-09-24.md` (A1-A21), `PROOF-2-THEOREM-draft-2026-09-24.md` (W0-W6, R1-R8), `PROOF-2-STRATEGY-draft-2026-09-24.md` (packet rows), `proof2/packets/CERT-S.md` (certificate key paths and hashing).
## Registration packet: non-author warrants for 43d88ee7, 578b728e, 7d3d4ac2
You are registering test runs for commits you did not author (author/reviewer separation; read `holes/labs/wm-contract/NOTE-test-registry.md` if new to the registry). In /home/joe/code/futon2 run `AUTHOR=<your-id> scripts/wm/register-warrant.sh --pinned <sha> <namespace>` for: 43d88ee7 → futon2.aif.mission-reading-test; 578b728e → futon2.aif.publication-unreachable-test (NOT the -live-test namespace, which reads the live store and is never registered pinned); 7d3d4ac2 → futon2.aif.observation-checks-test and futon2.aif.mission-reading-test (confirm namespaces from each commit's stat). Then answer, quoting test source: (1) at 7d3d4ac2, is it asserted that every :check-fn named in resources/wm/observation-contract.edn resolves to a function in futon2.aif.observation-checks (the commit says the contract's check names are checked against observe); (2) at 578b728e, in the synthetic-store test, is the ground "H-PUBLISH-D 5cbf0031" asserted on every result or only counted. No edits, no clicks, no JVM loads, no writes under data/. Bell claude-8 with the warrant ids and answers.
