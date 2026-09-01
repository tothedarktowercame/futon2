# C289 — preemptive-repair gate snapshot

Date: 2026-09-01

## Classification

The C280 failure was not a worker-ordering race like C231 and not a stable
assertion mismatch. It was a live-repository observation race.

`checks.preemptive-repair-suite/gate-result` invoked
`checks.preemptive-repair-lint/run` once for each of six defect classes.
Every invocation independently ran `git ls-files` and read the current files
from the futon2, futon3, and p4ng worktrees. A concurrent edit or commit could
therefore be present in one class's corpus and absent in another. The resulting
gate verdict described no single repository state. C280's bounded wrapper then
correctly reported that the repository basis changed, and the same lint passed
on the final tree.

The shared thing was the three live tracked worktrees, not a receipt directory
or test fixture. Moving worktree state is legitimate during concurrent lane
work, but mixing six observations of it into one verdict is not.

## Repair

One gate verdict now eagerly captures one in-memory corpus and passes that same
population to all six scanners. Standalone lint commands retain their existing
single-scanner behaviour. This does not certify a moving repository basis:
the bounded suite wrapper remains responsible for rejecting a run whose basis
changes. It ensures only that the inner gate does not manufacture a hybrid
verdict from six independently observed populations.

## Control and focused verification

The focused control replaces `corpus` with a source that returns a different
population after its first call. Before the repair the gate called it six times;
after the repair it calls once, so later concurrent state cannot leak into the
captured verdict.

```text
clojure -X:test :nses '[preemptive-repair-gate-test]' \
  :vars '[preemptive-repair-gate-test/one-gate-verdict-captures-the-live-corpus-once]'
  1 test, 2 assertions, 0 failures, 0 errors

clojure -X:test :nses '[preemptive-repair-lint-test]'
  4 tests, 14 assertions, 0 failures, 0 errors

bb -cp . checks/preemptive_repair_suite.clj --negative-gate
  exit 0; injected finding rejected
```

The full positive gate was not claimed green during this packet: it observed a
separate tracked C284 prose finding (`C284-verifiable-format-proof-and-gate.md`,
line 42). That is concurrent live state outside C289, not retried or absorbed.
