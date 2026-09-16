# WM-08 failure-classification repair — author receipt

Author: codex-9, 2026-09-16. Both owners agreed after reconciling the failure-record contract (retained Agency JSON). Source/test scope only; independent review pending. No serving reload, click, predecessor repair or checkbox closure.

## Result

The interpretation job classifies known refusals separately from typed machine failures and unknown exceptions. It preserves the cause chain and original typed failure-kind, adds class/message/data to retained diagnostics and the outer exception, and writes the new closed-vocabulary :interpretation/machine-failure record when appropriate. Thus captured companions still have a valid admission record. Unknown kinds, job-already-dispatched and attempt-identity-mismatch retain machine repair; only explicit content kinds receive environmental holds. Budget remains incomplete-recoverable. Availability tests now inject an explicitly typed unavailable condition: an arbitrary exception at readiness no longer establishes agent unavailability.

## Validation

All test commands run in separate tooling JVMs, one namespace per invocation. Interpretation-job tests install hermetic repair/trip and trace/run-record fixtures; dispatch and delivery ports are stubbed.

- Before repair: `clojure -X:test :nses '[futon2.aif.interpretation-job-test]'`, exit 1: 11 tests / 192 assertions, four expected failures. Both injected build-failed and NPE became invalid-receipt/environmental-hold. See before.log.
- After repair: same command, exit 0: 12 tests / 247 assertions. See after.log.
- Two tests then gained stronger assertions on environmental classes and retained failure/diagnostic/returned-byte manifest entries. Final selected rerun, exit 0: 2 tests / 144 assertions. Exact command in test-exits.json; final-controls.log.
- receipt-construction-test: 7 tests / 31 assertions, exit 0.
- interpretation-evidence-test: 5 tests / 27 assertions, exit 0.
- clj-kondo on the three changed source files and changed test: exit 0, zero errors/warnings (one pre-existing info in full_loop_runner.clj:2000).
- `emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- --no-defaults` with the four changed Clojure files: exit 0, OK.

The tests assert original cause object identity, diagnostic class/message/data, wrapped typed failure preservation, typed invariant routing, actual repair class, valid retained failure record and manifest entries. They retain ordinary success and content-refusal controls; unknown schema kinds are still rejected. Production-store file-set assertions passed. Runtime tests took minutes; no claim of a production run is made.

A brief overlap occurred between the tail of the baseline namespace process and the first post-change namespace process; both used their own temporary stores. Subsequent validation was sequential. The first post-change run preceded the final assertion additions, hence the explicit final selected rerun.

## Remaining work

Independent review by claude-3 is requested in REVIEW-REQUEST.md. Actual source-bound retrieval and interpretations, independent F2 expectations, honestly scoped F4, serving activation and ordinary downstream use remain WM-08 obligations. This packet discharges only the independently demonstrated failure-classification defect when accepted.
