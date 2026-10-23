# b2b enacted other-injury retry

Worktree /home/joe/worktrees/futon2/b2b-enacted-other-retry; branch b2b-enacted-other-retry; base 3e787825c from canonical main (this repo has no master branch).

Premises confirmed in meta_terminal_injury.clj:249–251,268–297 and meta_injury_ledger.clj:17. One production change: the :enacted branch now also requires (not other-injury-retry?). Another enacted injury's retry therefore falls through to the existing active-injury branch, retaining repeated-active-injury for A and retry-inconclusive for B. Same-injury and unowned mismatch paths remain unchanged.

New namespace futon2.aif.b2b-enacted-other-injury-retry-test uses a verbatim classification value from b2a-replay.edn, substitutes fixture pins/control click IDs and reseals it, and constructs two enacted injuries using real ledger transitions. It tests both appended observations, pending-requeue, valid ledger, duplicate replay, wrong retry mismatch, and same-injury reopening. Fixture provenance is in test/resources/b2b/README.md.

Red command (exit 1):
JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/home/joe/notes/b2b/tmp clojure -X:test :nses '[futon2.aif.b2b-enacted-other-injury-retry-test]'
3 tests / 40 assertions: exactly 2 failures, 0 errors — appended expected but refused; pending-requeue expected nonnil but nil. Controls passed.

Green command (exit 0):
JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/home/joe/notes/b2b/tmp clojure -X:test :nses '[futon2.aif.b2b-enacted-other-injury-retry-test futon2.aif.meta-terminal-injury-test futon2.aif.f43-other-injury-requeue-test futon2.aif.f46-repeated-injury-requeue-test]'
All four namespaces passed: 17 tests / 290 assertions, 0 failures, 0 errors.

clj-kondo on changed Clojure: 0 errors/0 warnings. check-parens OK. git diff --check passed. Logs under /home/joe/notes/b2b/{red,green,kondo,parens}.log, also committed under evidence/b2b-enacted-other-retry/.

No merge, JVM reload, retained-record ingestion, live-store write, or click. The existing ledger still requires a separately authorized replay after review.
