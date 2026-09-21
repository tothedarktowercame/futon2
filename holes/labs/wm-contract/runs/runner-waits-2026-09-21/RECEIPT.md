# Runner waits — implementation and validation

Implementation: `385f54dcf9179cc8e3820a7776f65ea654501b3e`.

The runner polls until Agency returns a terminal state. Legacy job-budget
options no longer end polling. Observed silence (default 45 minutes;
`:agent-silence-ms` overrides it) records job ID, duration, activity basis and
waiting state. First-poll time is explicitly identified when Agency timestamps
are missing. One observation is emitted per silent interval; a new activity
resets the interval. The phase channel durably records it, T9 consumes it with
halting disabled for this observation, the terminal writer retains it under
`:job-liveness`, and the validity report displays it without a new veto.

Failed review closes now carry the available artifact binding. The enactment
producer receives the actual dispatch route independently of outcome and can
also recover the retained binding from the build checkpoint. It distinguishes
`:binding-not-retained`, `:dispatch-not-retained`, and actual `:recovery`.
The categorical recovered-artifact refusal remains unchanged.

## Tick B replay: corrected attribution, not manufactured admission

The literal historical dossier replays as `:fresh-author` with its binding
retained and its review still REQUEST_CHANGES. The independent verifier's next
refusal is `:job-occurrence-binding-unestablished`, before review verification.
The initial probe expected the later review refusal and failed that assertion;
its source, log and exit remain here. The confirmed replay asserts the actual
refusal, preserves it, and writes only into a fresh /tmp directory. No old claim
or run record was changed. The permanent retained-dossier fixture checks the
same result; the separate synthetic fixture with a valid occurrence binding
reaches `:independent-review-not-passed`. Nothing was weakened to make the
literal history pass. The occurrence-binding mismatch is a separate finding.

Source: machinery-68 attempt-002's 004-dispatch.edn and 005-build.edn; original
claim action-357124fc-c58f-4a84-984a-6dc277cdf39b.edn, sha256
5a9d871eb6885a736bff1b7ca0807ffc8abe321349fff72861f8e66cbaded9f3.
`test/fixtures/tick-b-enactment.edn` retains its dispatch, final binding and
compact jobs (prompts, execution summaries and terminal text; tool output is
omitted). Its observation ports are stubbed to avoid live reads in the suite;
the standalone literal replay also exercised the actual observation readers.

## Validation and checkout concurrency

Initial ordinary executions passed: runner 180 tests / 1005 assertions,
authority 8 / 41 after adding the retained-dossier test, tripwire 41 / 129.
Three earlier launcher attempts did not execute tests: futon2's local alias
needs explicit `-m cognitect.test-runner`. Their errors/exits are retained.
Registry specs correctly use `-n` because the registry supplies its own runner.

Two registered executions are requested for each namespace. Shared-tree
authority and tripwire registrations passed. The shared-tree runner tests also
passed, but registration refused `:scope-not-committed` at load-closure
postcheck after another lane changed eight loaded paths. Its second registration
refused before execution because the declared scope was uncommitted. Both
refusals are retained, including the durable API record and exception data.

Validation therefore uses a clean detached checkout of the implementation:
`/home/joe/code/futon2-codex2-runner-waits`, pinned at 385f54dc. The final warrants
refer to that checkout, not to the concurrently changing main checkout. It is
kept available for independent warrant checking. No other lane's changes were
reverted, staged or committed. Global main-checkout cleanliness cannot be
claimed while their work is present.

Pinned clj-kondo: zero errors/warnings. Check-parens: OK. Validity checker
selftest: PASS. A separate /tmp persistence probe verifies that the actual
terminal writer retains the stalled-job record; the validity smoke output
prints its job ID and silent duration. No WM click, serving reload or live
repair-store mutation was performed.

## Route disposition

See ROUTE-PROPOSAL.md. Recommend dormant **deferred completion**, subject to an
explicit outstanding-job/artifact disposition and legacy-record compatibility.
Piece 3 stops at proposal: no retirement, renaming or admission was enacted.

Serving reload list (not performed): `futon2.aif.full-loop-runner`,
`futon2.aif.d-predecessor-task-authority`, `futon2.aif.tripwire`.
The standalone validity script is invoked afresh.

## Two registered executions per namespace (pinned implementation)

| Namespace | Run | Warrant | Result |
|---|---:|---|---|
| full-loop-runner | 1 | `test-registry-a6735d2ed5c3dd866698196a8d612bbc4b53505618db610f1d0e4e735ed2cc0f` | `{:assertions 1005, :duration-ms 190738, :errors 0, :exit 0, :failures 0, :tests 180}` |
| full-loop-runner | 2 | `test-registry-9adb61e45a2b3f0776446e12c08bb50f58a6413a18a65f36df10c3ed55f3899f` | `{:assertions 1005, :duration-ms 185360, :errors 0, :exit 0, :failures 0, :tests 180}` |
| d-predecessor-task-authority | 1 | `test-registry-a250d21e12dea819896ca8705a4b4aacaaced664ad00544fdb6ca23feee69bfe` | `{:assertions 41, :duration-ms 4270, :errors 0, :exit 0, :failures 0, :tests 8}` |
| d-predecessor-task-authority | 2 | `test-registry-c4f0d3ab6c687e8e005288603b4fe6585da8e8beae96ab4922c2e9d791ec6635` | `{:assertions 41, :duration-ms 2327, :errors 0, :exit 0, :failures 0, :tests 8}` |
| tripwire | 1 | `test-registry-2d72386dade28c04d473fb51913c784df19ff84ba77cfc34d2fa7ea9f296b7c3` | `{:assertions 129, :duration-ms 19138, :errors 0, :exit 0, :failures 0, :tests 41}` |
| tripwire | 2 | `test-registry-233b7900a33754f9a980f602249eba40b9b492bb35132006c7a9817b9329cbeb` | `{:assertions 129, :duration-ms 16883, :errors 0, :exit 0, :failures 0, :tests 41}` |

The second execution is bound to each `wm-runner/waits-<namespace>` subject.
`report-before-final-binding.txt` is a fresh-JVM durable report: all three
subjects are current. It was started before the second runner binding, so its
runner line names the first pinned warrant; the second binding is retained in
`full-loop-runner-isolated-2/stdout.edn`. No additional test execution occurred.
