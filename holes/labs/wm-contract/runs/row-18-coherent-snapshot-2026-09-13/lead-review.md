# Coherent snapshot review — codex-26, 2026-09-13

Reviewed e94871a5/05ff643f, receipts6a649e18/e9cc611d. Four writer census
source pins match actual bytes; retained conflicting finding/resolution paths
also pinned in lead-pins.json. No tests rerun. Changes requested.

1. Dynamic *lock-held?* is conveyed into Clojure futures/bound-fn. A child thread
   launched inside a locked callback inherits true and bypasses acquisition,
   possibly after the parent releases. Nesting also bypasses a different lock
   path. Reentrancy must require actual owning thread and same lock identity,
   not a dynamically inherited boolean. Add a controlled future/thread case.
2. with-store-lock-for custom roots places a lock under root before writer
   io/make-parents/historical-directory! runs. Fresh nonexistent roots/stage
   directories now fail before previously supported creation. Exercise actual
   trip and each repair writer with fresh temporary roots, not only bare lock
   calls. Select a stable shared boundary without breaking root creation.
3. No live reload or serving writer identity verification occurred. Merely
   acquiring the new lock in a new reader JVM cannot coordinate older live
   writers. Production qualification needs externally established participation
   for all actual writers; until then the current real read is an audit only.
   Correct the claim that physical production consistency was demonstrated.
4. The parens gates do not replace the commissioned concurrency tests: the only
   new test covers same-thread nesting/release (3 assertions). Add cross-process
   exclusion, same-JVM contention, partial publication and error release checks
   using isolated stores and failure-sensitive outputs. Retain enumerable writer
   census commands/results, not only a prose claim of exhaustive search.
5. Previous typed IO and path hardening is absent: readAllBytes still leaks raw
   file failures; root ancestry symlinks remain unchecked. Lock parent check
   canonicalizes before checking and compares canonical parent with itself, so
   it does not reject supplied symlink ancestry. Address actual source path
   validation and stable lock identity, including tmp-file replacement assumptions.
6. production-readback-receipt.edn command is placeholder pseudocode, not an
   executable retained invocation. Preserve exact runner source/command/stdout/
   stderr and honest counts; no production-store edits to obtain a passing result.

The open->resolved retained repair refusal is a separate logical finding; do
not rewrite its history or waive validation to make the snapshot pass. This
review does not admit a snapshot or claim the source lock is deployed.
