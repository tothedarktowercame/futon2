# Narrative fix-3: retain job prompts and final replies

Branch `fix/narrative-3`, based on main `0850f88d`. No shared checkout edits,
live loading, clicks or merges. Prompt builders are untouched for fix-2.

## Implementation and checkpoint placement

`job-text-retention/retain-job-texts!` writes separate UTF-8 files directly in
the attempt directory, beside the checkpoint EDNs. Names include a digest of
role/job identity and the text's byte SHA-256. References carry path, SHA-256,
encoding, byte count and status. Missing, blank or non-string text has a typed
absence and no placeholder file. Repeated identical retention verifies existing
bytes; changed text creates a new immutable file. Corrupted existing bytes
refuse rather than being overwritten. Role/job text cannot escape the directory.

The runner wraps its injected/default dispatch, poll and read ports once after
creating the attempt identity. The wrapper retains values already returned;
it makes no additional Agency calls. `dispatch!` attaches its exact post-D-task
binding prompt as response metadata, preserving response map equality and its
wire representation. Polling retains `:result`, never the shortened summary.

`dispatch` and `build` checkpoint judgments contain `:job-texts`, a vector of
per-job records with role, agent, phase, job-id, prompt-ref, prompt and reply
references. Failure sorries carry `[:sorry :job-texts]`. Dispatch naturally
records a typed absent reply until polling has returned one. Build includes
initial and replacement jobs rather than only the final author/reviewer.
Non-cohort runs use `<run-record-dir>/<run-id>/<attempt-id>/` and retain the
references in their returned/persisted run record.

**Late-job placement:** measured acquisition dispatches a standing-completion
reviewer AFTER the immutable build checkpoint. Its text files are retained by
the same ports; its references appear in the closed checkpoint and final run
record, not retroactively in build. Normal close and both close-failure paths
carry the current references. This preserves checkpoint order and immutability;
it is an explicit exception to demanding build references for every later job.

Port census in full_loop_runner.clj:

- initial author and reviewer (including repair reviewer substitution);
- author infrastructure retry;
- build-cure author;
- revision author and re-reviewer;
- interpretation job through its injected callbacks;
- standing-completion reviewer;
- deferred/recovered job reads.

All call sites use the wrapped opts, including prompt-opts passed into revision
and cure helpers. Recovery without a known role is labeled `:recovered-job`.

## Agency record findings

Read current futon3c `transport/http.clj`, especially `trim-stream-event`,
`finalize-invoke-job!`, and `invoke-job-public-view`:

- `:result` receives the unshortened final string. The 8000-character cap is on
  bellback `:result-text`; the public-view comment claiming the full result is
  capped is stale. The dossier preserves the value actually returned.
- Prompt events are truncated after 1500 characters with ` …[trimmed]`.
  A known exact dispatch prompt always wins over that preview. A recovered
  trimmed prompt becomes `:agency-prompt-truncated`, not a purported full text.
  An untrimmed recovered prompt can be retained from the event.
- Job `:events` can contain streamed text, tool-use names/input previews and
  bounded outputs joined to tool-use events. These are not complete tool-by-tool
  transcripts: text is capped at 2000, input hints at 120, output at 4608; events
  can also expire. This fix retains none of those tool transcripts.
- A later compacted record cannot erase an already retained full prompt/reply.

## Tests and red evidence

`job-text-retention-test`: 10 tests / 98 assertions, all pass. Covers exact
Unicode and >8000-character replies, hashes verified from file bytes, absence,
idempotence, immutable old references, corruption refusal, safe identity paths,
all actor roles/repeated revision-style dispatches, no additional calls,
truncated recovery, and compacted-record preservation.

`job-text-dispatch-test`: 1 test / 3 assertions, all pass. Calls production
`dispatch!` with only the HTTP post port stubbed, uses the real D-task binding,
and checks the exact sent prompt and unchanged response map.

Red test against the base implementation: extract only the `dispatch!` defn
from `git show 0850f88d:src/futon2/aif/full_loop_runner.clj`, evaluate that defn
in the runner namespace of a separate test JVM, then run job-text-dispatch-test.
No shared JVM or source guard was involved. `old-dispatch-red.log`: 1 failure,
0 errors; expected bound prompt metadata, actual nil.

The actual reference `004-dispatch.edn` also fails the checkpoint assertion
(`recorded-dispatch-red.log`):

```
expected: (string? (get-in checkpoint [:payload :judgment :job-texts 0 :prompt :sha256]))
actual: (not (string? nil))
```

`job-text-retention-runner-test` adds the requested end-to-end stubbed
opportunity: author/reviewer prompt and reply files under the attempt directory,
checkpoint paths/hashes and dispatch-time reply absence. It was attempted but
**cannot execute its assertions in this worktree**: the unchanged source guard
refuses before the opportunity. `runner-source-refusal.log` records 1 error
with `:failure-kind :stale-runner-source`. Per the handoff, the reviewer must
run this namespace after merge in the canonical checkout. No source guard was
mocked, disabled or weakened. The broader runner namespace has the same known
source-authority blocker and was not rerun here.

## Validation commands

Working directory `/home/joe/code/futon2-narrative-3`; OpenJDK 21.0.11,
Clojure CLI 1.12.5.1664, project Clojure 1.11.1.

```
clojure -M:test -m cognitect.test-runner -n futon2.aif.job-text-retention-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.job-text-dispatch-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.job-text-retention-runner-test
clj-kondo --lint src/futon2/aif/job_text_retention.clj src/futon2/aif/full_loop_runner.clj test/futon2/aif/job_text_retention_test.clj test/futon2/aif/job_text_retention_runner_test.clj test/futon2/aif/job_text_dispatch_test.clj
emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- --no-defaults src/futon2/aif/job_text_retention.clj src/futon2/aif/full_loop_runner.clj test/futon2/aif/job_text_retention_test.clj test/futon2/aif/job_text_retention_runner_test.clj test/futon2/aif/job_text_dispatch_test.clj
```

Kondo: 0 errors/warnings (one pre-existing informational str notice). Parens:
OK. Fresh namespace outputs and red evidence are beside this note. Registry
configs cover the two directly executable namespaces; integration is not claimed
as warranted.
