# Item 25 model-choice chain — final report, 2026-09-09

| Item | Result and commits |
|---|---|
| 1. pi-zero-form | Implemented opt-in habit-in-both at the beta carry: caller e102f155 (automatic inbox-zero capture), complete adapter/tests/config 4ddb7094. Review together. No strategic E promotion, tie-rule change, or temperature-mode activation |
| 2. hierarchy | Config-only disposition 4bcda58e: single-level, RUN4-scoped, all follow-ons preserved. No new hierarchy or trace-field implementation claimed |
| 3. learning | PROPOSED design c7daea2e: variables, evidence, equations, schedule, persistence and next consumers. No learning implementation/ruling inferred |
| 4. policy depth | Excluded from our edits; codex-10's 4e76f94f plus its automatically captured caller/trace half ff015b85 landed before our caller edit |

## Tests and remaining finding

Focused beta adapter/wiring: 8 tests / 32 assertions / 0 failures/errors.
Broader relevant suites: 233 tests / 1168 assertions / 1 failure / 0 errors;
full output in beta-tests.txt. This is NOT an all-green Clojure suite.
The failure is U12/U15 mission-c-readback-hashes-the-criteria-source-test:
expected source hash 51f6de53... versus live a770d000.... Both the old expected
literal and the differing live source bytes occur at pre-chain c24bb62b, so
our beta diff did not cause it. claude-1's 7a0fe12b documents the deeper fixed-
verdict producer defect; re-pinning would misrepresent that producer. No
producer, receipt or test expectation was modified to make this pass.

Explicit false overrides beta settings from the run sheet and environment;
no-config beta output equals the pre-edit 110-candidate fixture bytes. The
run sheet also states the three required evidence/persistence env flags. An
opted-in judge refuses before external reads if those flags are missing.
Positive full-judge execution was not run: the integration tests drive the
real beta/readback/trace functions and the judge's missing-producer refusal.
No live run, lock, state-data write or shared JVM load occurred.

Clojure lint: 0 errors / 0 warnings. check-parens and git diff --check pass.

## Final Lean and readiness validation

After the item commits, lake build over all 127 positive/guarded WarMachine
modules exits 0: Build completed successfully (8623 jobs).
lean-targets.json enumerates the corpus and the two raw expected failures.
Those two deliberately false FoldC modules were exercised via
checks/fold_c_witness.clj --negative-folded and --negative-order: both wrappers
exit 0, reporting negative-control PASS, with both composition axes aligned.
No new Lean declarations or proofs were needed for this configuration packet.
This build validates the existing corpus, not runtime implementation refinement.

Two fresh readiness axiom probes exit 0, sorry 0, 0 of 6 theorems carry axioms.
The resulting artifact is unchanged against committed bytes, SHA-256
0a5dc3aabc46e6fe1492dd892d7bc7358f89352968c4b36d6bfea21807832637.
Readiness summary exits 0; full output is retained in readiness.txt:

```
RUN4 certificates        GREEN
RUN4 definitions-intact  GREEN
RUN4 wiring-pin          GREEN
RUN4 run-pins            GREEN
RUN4 regenerates         GREEN
RUN4 lean-probe          GREEN
RUN4 hole-open           GREEN
RUN4 closability-audit   GREEN
RUN4 invalidators        GREEN
VERDICT: READY
```

READY states the meter's declared conditions, not that U12 passed, the learner
exists, or a live run has been accepted. No registry/worklist/frontier edits.

## Publish

`bash /home/joe/code/p4ng/build-p4ng.sh futon-2026`: exit 0.
Negative-control suite completed; HTML has 0 ltx_ERROR spans; PDF gate clean,
36 pages; published FUTON HTML/PDF. Conversion reported 7 warnings and tuftify
reported its existing invalid-escape SyntaxWarning; neither was a gate failure.
See publish-tail.txt. Generated p4ng outputs and workflow-report.edn were left
for the publishing/ledger owner to commit; no unrelated changes were staged.

## Coordination and deviations

Waited for codex-10's file release; hierarchy/config and learning note commits
proceeded independently during that wait, as claude-1 explicitly authorized.
The transport snapshot initially suggested a stuck queue; the owner's reply
established progress and the subsequent commit released the files.
An automated inbox-zero commit captured our beta caller during tests; item 1
therefore spans two commits rather than one. No amend or rollback was used.
Remaining substantive fork is the known U12 producer/pin conflict, outside
this packet. Learning's proposed questions await Joe by design, not by failure.
