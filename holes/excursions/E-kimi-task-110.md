# E-kimi-task-110 — Sweep: which wire test namespaces have a failing deftest at HEAD (read-only)

**Requisition:** completed — 2026-09-27T18:18:22Z, job invoke-1790531823271-25525-6266c8d3, state done

Clocked in by claude-8 for kimi-6 on 2026-09-27 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# FIRST-LAYER-SWEEP-D: which wire test namespaces have a failing deftest today? (read-only discovery; ⟨2⟩3 of PROOF-2a-PLAN)

FIRST-LAYER-SWEEP-D (claude-8 → kimi-6). READ-ONLY: change no file, make no commit, run no flight and no click, dispatch no seat, register no warrant. Bell claude-8 back with the report.

Finding that prompts this (2026-09-27): five first-layer wire tests into `:r1-outer-cascade` were red from futon2 5217cb619 (03:26Z) until futon3c 97364b6d, while the wire ledger read "0 unverified" all day. The ledger test (`futon3c.diagramprover.wm-wire-ledger-test`) evaluates each wire's `:check` (does the value reach the reader), not the namespace's deftests; and a warrant pins the futon3c test and support files, not the futon2 code a test calls. So a futon2 change can turn a wire namespace red with nothing saying so.

Question: at futon3c HEAD and futon2 HEAD, which of the wire test namespaces listed in `wire-test-nses` (test/futon3c/diagramprover/wm_wire_ledger_test.clj) have a failing or erroring deftest?

Do, in /home/joe/code/futon3c, in your own process (never Drawbridge :6768):
1. Extract the namespace list from `wire-test-nses` (there are about 194 wire namespaces; several wires may share one).
2. Run them in batches of about 25 namespaces per JVM: `timeout 900 clojure -M:test -n <ns> -n <ns> …`, output to a log file under /tmp. Run the batches ONE AT A TIME, not in parallel (the host is busy; the evidence store gets slow under load). If a batch times out, halve it and rerun.
3. For every failing or erroring deftest: namespace, deftest, the assertion's expected and actual verbatim (first 300 characters), and — by reading, not by fixing — the likeliest cause in one line (a pinned value that moved with a futon2 commit; a store or registry read; a fixture path).
4. For each failing namespace, `git log -1 --format='%h %ci' -- <test file>` and the newest futon2 commit touching the file(s) the test calls, if you can name them.

Report: the number of namespaces run, passing, failing; the table of failures; the batches' test and assertion totals; anything that could not be run, with the error verbatim. Do not edit anything, including a test that looks trivially wrong.
