# Finding → ordinary ticket (slice 2)

Branch `fix/finding-to-ticket`, base `bbae7593`. No live clicks, serving-JVM
evaluation, backfill, or live finding/ticket/queue writes were performed.

Both finding creation verbs now publish an ordinary OPEN Markdown ticket and
append `{ :ticket "T-<finding-id>", :inserted-at <first opened-at> }` through the
origin-blind `ticket-queue/enqueue!` writer. The ticket describes the failed
behaviour, task scope and ordinary acceptance evidence. Environmental holds
produce verify/restore-precondition tasks. No special role or two-stage
successor requirement is copied into the ticket.

The store records **`:finding/ticket`** in
`<store>/ticket-links/<finding-id>.edn`, schema `:wm/finding-ticket-v1`.
Its value contains `:id`, `:path`, `:finding-path`, `:finding-sha256`,
`:queue-path`. Original finding bytes and return values are unchanged. The
receipt retains the first publication's byte pin even if the store subsequently
accepts a legacy equivalent serialization on replay. It is provenance, not a
field parsed by the ticket registry or queue.

The canonical store publishes under `/home/joe/code/futon2/holes/tickets` and
`/home/joe/code/futon2/resources/wm/ticket-queue.edn`. Other explicit store roots
own an isolated `ticket-publication/` tree by default. A third argument to each
creation verb accepts `{:ticket-dir ... :queue-path ...}` for explicitly
configured destinations. This changes location only: all roots execute the
same publication, validation and locking logic, with no disabled publisher.

## Publication and recovery

- Ticket bytes are forced in a temporary file, then published by an atomic
  hard-link creation which cannot replace an existing ticket. Existing edits
  are preserved. The temporary link is removed after publication.
- Queue updates use a per-path JVM monitor and the existing cross-process
  lock authority, validate the entire declaration, and atomically replace it
  from a forced temporary file. Duplicate identical entries are no-ops;
  conflicting insertion timestamps or malformed declarations refuse.
- A crash after ticket publication leaves the finding and complete ticket;
  retry appends the missing queue entry. A queue-only partial state also
  completes the ticket. Neither publication requires the provenance receipt
  to have been written previously; retry finishes that final step too.
- Store-side receipt publication uses the existing store lock. Finding
  creation wraps publication in the store's existing contention handling.
  Unsupported atomic publication or lock contention is a failure to retry,
  not permission to use a non-atomic fallback.
- Symlink destinations refuse; the adversarial test uses a real symlink and
  verifies its target remains byte-identical. A process crash may leave an
  unreferenced `.ticket-publication-*.tmp` file; it cannot leave a partial
  visible ticket or queue declaration. No live reconciliation sweep is added.

New click-path namespaces `finding-ticket` and `ticket-publication-io` register
load identity and are included in `required-sources`. The selection law and
queue planner are unchanged. Tests call the real planner to verify that the
new entry occupies the front stratum, without manufacturing cascade admission.

## Tests and negative control

Frozen fixtures are byte copies of the real explanation-invalid finding
`repair-occ-6c6ecaf858aff5c7f9aeef1b92ad205e7ed31ed29882e89fa52f2724d3d039c8`
and environmental guardrail-refusal finding
`repair-occ-917bbee700382c988dce8f4d2d69de454062adbeb5de7070eb61973a301af1f6`.
The registry reads the generated ticket from a temporary checkout as an
ordinary live ticket. Tests cover exactly-one publication, first timestamp,
provenance digest, byte-identical replay, crash between writes, queue-only
recovery, preserved human edits, review-failure creation, environmental tasks,
concurrent ordinary queue additions, conflicting retries, malformed declarations
and symlink refusal. Live repair-store and ticket file counts are checked
before/after publication; all mutation ports use temporary roots.

`baseline.txt` runs the new boundary test with the **unchanged**
`bbae7593:src/futon2/aif/repair_obligation.clj` first on an isolated CLI process's
classpath. No production guard was disabled. The old boundary fails:

```
expected: (.isFile (ticket-path opts record))
  actual: false
expected: (.isFile (io/file (:queue-path opts)))
  actual: false
{:test 1, :pass 0, :fail 2, :error 0}
```

The same test passes against this branch. The baseline process exits zero only
when those two expected failures and zero errors are observed.

Commands (cwd `/home/joe/code/futon2-finding-to-ticket`):

```
clojure -M:test -m cognitect.test-runner -n futon2.aif.finding-ticket-test
# 6 tests, 38 assertions, 0 failures/errors
clojure -M:test -m cognitect.test-runner -n futon2.aif.repair-obligation-test
# 30 tests, 196 assertions, 0 failures/errors
clojure -M:test -m cognitect.test-runner -n futon2.aif.ticket-queue-test
# 8 tests, 43 assertions, 0 failures/errors
clojure -M:test -m cognitect.test-runner -n futon2.aif.repair-proposals-test
# 5 tests, 31 assertions, 0 failures/errors
clojure -M:test -m cognitect.test-runner -n futon2.aif.load-identity-test
# 3 tests, 12 assertions, 0 failures/errors
```

Total: 52 tests / 320 assertions. clj-kondo on all six changed/new Clojure files:
`errors: 0, warnings: 0`. `futon4/dev/check-parens.el` on the same files: `OK`.
`git diff --check`: clean. An initial unmatched parenthesis in the new test was
corrected before these passing gates.

Registry check returned `:missing-entry`. The first registration command was
refused as `:explicit-namespace-required` because it used the CLI's `-m`
spelling; `registry.edn` now uses the documented logical namespace command
which the registry executes through its recording runner. Registration follows
the implementation commit; its result will be retained beside this report.

## Later slices (not implemented)

- **Backfill 44:** explicitly run the same publisher over a reviewed snapshot,
  with retained provenance and idempotent completion; no sweep was added here.
- **Cascade sources:** authored wants, locators, interpretations and constructed
  candidates remain necessary; fix generic T-target dispatch and remove repair
  supply withholding/origin routing. A ticket file alone does not admit a policy.
- **Closure:** implement Joe's new rule: ordinary reviewed ticket closure
  resolves its finding, with no second distinct validation run. Replace the old
  store discharge contract and special runner finalizer through a store-owned
  provenance bridge. This slice neither implements nor designs around the old
  two-stage rule; the existing finding payload remains historical evidence.
- **Stop the line:** change the generic queue eligibility/gate to hold when the
  unresolved front ticket cannot proceed, as Joe ruled. The merged queue planner
  currently skips unadmitted strata; this slice does not alter it.

## Registered warrant

Implementation commit: `29e4d7fc48c78108b61acb53eb534316594686b3`.

`test-registry-eeefcd2eb9a5480c47f644591d2ab0c3f220d7eb963c8298f7ef0e3ca1af7f5c`:
`:warrant? true`, postcheck `:matched`, 6 tests / 38 assertions, zero
failures/errors, exit 0. Registration, content-pinned log and load closure are
retained adjacent. This scoped warrant covers finding-ticket publication;
the four other namespace runs above are separately reported CLI validations.

Command, cwd `/home/joe/code/futon3c` (independent CLI JVM):

```
clojure -M -m futon3c.test-registry run /home/joe/code/futon2-finding-to-ticket/holes/labs/wm-contract/runs/finding-to-ticket-2026-09-22/registry.edn
```

The authorized test-registry evidence registration is the only evidence-store
write; no live repair store, ticket directory or queue declaration was changed.
