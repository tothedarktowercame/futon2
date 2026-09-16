# Independent review request: WM-08 failure classification

Joe commissioned codex-9 to implement, validate, commit, and obtain independent Agency review. Both WM-08 and WM-09 owners agreed the three-source-file scope and honest machine-fault failure envelope; replies are retained here. Reviewer requested: claude-3, who identified the original defect. Review the implementation commit supplied in the Agency bell and any intervening changes to these files.

Please independently accept/reject the actual classification and retained evidence, not just the test results:

- Typed machine failures (including a wrapped typed cause) and untyped faults during receipt construction keep machine repair; original exception class/message/data and cause survive.
- Known content refusals stay environmental holds; budget expiry stays incomplete-recoverable. Unknown interpretation kinds and the explicit dispatch/identity invariants must not receive environmental discharge.
- The new closed-vocabulary machine member is a stage-of-failure record retaining captured sources, not a content-refusal or successful interpretation claim. It must not permit interpreter self-reporting to weaken the repair contract.
- Attempt close admission retains schema-valid failure, diagnostic and rejected receipt bytes. No orphaned companion files or invalid-receipt relabeling workaround.
- The runner edit is limited to its interpretation mapping. No predecessor-history repair, serving activation, click or checklist closure belongs to this packet.

Use the existing namespace fixtures (hermetic repair/trip stores and traces); never call run-case bare. Rerun relevant controls independently and inspect meaningful counterexamples. If adding a disposable probe, explicitly wrap it in both hermetic fixtures. Do not live-load or run a production click. Put review evidence in this directory (new REVIEW-claude-3.md and reviewer-named logs). Return verdict, exact findings, scope, tested commands and commit identities to codex-9. Do not dispatch other agents. Small concrete fixes may be committed separately with gates; substantial issues return to codex-9.
