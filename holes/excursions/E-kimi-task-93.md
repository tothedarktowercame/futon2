# E-kimi-task-93 — WM01-FAIL-D: why wm01-bindings-test refuses :class-unknown-no-scalar-g (discovery, no fix)

**Requisition:** in-progress — dispatched 2026-09-27T02:24:15Z to kimi-5 as invoke-1790475855264-25241-d8d8c0bc

Clocked in by claude-8 for kimi-5 on 2026-09-27 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# WM01-FAIL-D: why does `futon2.report.wm01-bindings-test` fail with `:class-unknown-no-scalar-g`? (discovery only; finding from WIRE-24-A2's review, PROOF-2a-PLAN ⟨2⟩3 LOG)

WM01-FAIL-D (claude-8 → kimi-5), discovery under the coding-handoff protocol: you find out, claude-8 reads; bell claude-8 back with the report below. NO commit, NO fix, NO edit to any tracked file, no flight, no click, no load into :6768, nothing under `data/`. Work from `/home/joe/code/futon2` at HEAD (read-only; `git status` must be as you found it when you finish).

The fact: `clojure -M:test -n futon2.report.wm01-bindings-test` (run once, in futon2's own JVM from `/home/joe/code/futon2`) errors with `cascade decision refused` `:kind :class-unknown-no-scalar-g`, thrown at `scripts/futon2/report/war_machine.clj:6844`. codex-3 reproduced it with the parent of e1138e819 too (test-registry evidence `test-registry-d2420a24e30001516554f3092758fc0e58f8831a3e54c9de6693b7472321b556`). The test's last PASSING warrant in `futon3c/data/test-registry/namespace-ledger.edn` is 2026-09-17 20:03Z at its own commit 3b9a955a1; nothing was registered for it since, so nobody knows when it broke.

What to establish, by reading and by one test run per question (record the exact command and the exact output lines you rely on):
1. Where the refusal is raised (`src/futon2/aif/observation_model.clj` around :216, `refuse! :class-unknown-no-scalar-g`, from the Proof 1.3 class observation scorer landed 2026-09-22 at daf2124e3 / 62fcfa1e7 / 11203e5d2 / 84f81cb42) and what condition it names: which target, which class value, which table lacks it. Quote the refusal map the test run prints.
2. Whether the cause is the TEST FIXTURE (a target/problem built before the class scorer existed, with no class the scalar-G table knows) or the PRODUCTION PATH (a real target that the tick would also refuse). Decide by reading what `classify-target` returns for the fixture's target and for the production targets the other tests in `test/futon2/report/` use (say which tests you read); if a production-shaped target also refuses, that is the larger finding — say so plainly.
3. `git bisect`-free dating: which commit between 3b9a955a1 (2026-09-17) and HEAD introduced the condition — reason from `git log --format='%h %ci %s' -- src/futon2/aif/observation_model.clj scripts/futon2/report/war_machine.clj test/futon2/report/wm01_bindings_test.clj` and the refusal's own code, not from running old shas (never `git stash`, never check out another revision in the shared tree; `git show <sha>:<path>` to read old source is fine).
4. Whether any OTHER `test/futon2/report/*_test.clj` namespace has no warrant in the ledger since 2026-09-20 (list them from the ledger by `:namespace` and latest `:ran-at`; do not run them — that is a separate packet if wanted).

Report (bell claude-8): the refusal map as printed; the answer to 2 (fixture vs production) with the evidence; the introducing commit or the shortest candidate list with your reasoning; the list from 4; the one-line fix you would recommend and what it would cost (test-side fixture change vs a production change), without making it. Say what you checked and what you did not.
