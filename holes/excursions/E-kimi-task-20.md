# E-kimi-task-20 — two one-assertion pins and three non-author registrations

**Requisition:** completed — 2026-09-25T02:46:59Z, job invoke-1790304033160-24112-dd4f2c85, state done

Clocked in by claude-8 for kimi-6 on 2026-09-25 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

From claude-8 (PROOF-2 coordinator). Read-only shared checkouts: futon2 is `/home/joe/code/futon2` on `main`, mathlib4 is `/home/joe/code/mathlib4`. Other lanes have dirty files there: stage ONLY the explicit paths you create, never `git commit -a`, never `git stash`, never amend. Do not fire WM clicks, do not write under `data/`, do not `load-file` anything into a shared JVM. No justification may appeal to an operator ruling or to who said what; settle by definitions, code, and records, or say the definition is missing and propose it as an amendment. Absence is a typed absence on the record, never a substituted value. When done, bell claude-8 back with a two-paragraph summary and the commit sha(s).

Governing documents (all under `futon2/holes/labs/wm-contract/`): `PROOF-2-ASSUME-draft-2026-09-24.md` (A1-A21), `PROOF-2-THEOREM-draft-2026-09-24.md` (W0-W6, R1-R8), `PROOF-2-STRATEGY-draft-2026-09-24.md` (packet rows), `proof2/packets/CERT-S.md` (certificate key paths and hashing).
Standing ruling (Joe, 2026-09-25 03:25Z): the machine is NOT to be run. No flight, no click on any mission, no `--run`, no writes under `data/`, no loads into shared JVMs (:6768/:7070). Work is on one hole of PROOF-2a (`futon2/holes/labs/wm-contract/PROOF-2a-THEOREM-draft-2026-09-24.md`, "Holes" table and the clause it serves); test the component in its own process. Explicit-path commits only (`git commit -- <paths>`), never amend, never stash. Register test runs with `AUTHOR=<your-id> scripts/wm/register-warrant.sh --pinned <sha> <ns>` from the repo's own checkout. Bell claude-8 with shas, warrant ids, and the answers asked for.
## Pin and registration packet (small, several one-assertion items)
In /home/joe/code/futon2. (1) At a98f5879 the scorer's ":universe absent is a no-op" claim is test-unwitnessed: add one test in `test/futon2/report/war_machine_universe_test.clj` calling `rank-cascade-actions` without `:universe` and asserting it equals the family-universe computation (read a98f5879's diff to see the pre-fix path it preserves); bad case: pass a wrong explicit `:universe` and assert inequality. (2) In `test/futon2/wm/extract_outcomes_test.clj` `e1-different-instances-not-merged` asserts only a count of 2: add assertions that BOTH sentences are cued before the not-merged assertion. One commit for both, explicit paths, run each namespace once, register both with AUTHOR=<your-id>. (3) Non-author registrations you did not author: `--pinned 5c9b9d45 futon2.wm.extract-outcomes-test` and `--pinned ead8aa3a futon2.aif.mission-criteria-test` (check the namespace from the commit's stat). No clicks, no data/ writes.
