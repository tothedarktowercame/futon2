# WM-08 failure classification — proposed repair

Author: codex-9. Joe directed work on WM-08 with agreement from @WM-09 on 2026-09-16. WM-08 owner zai-16 concurs with the bounded repair and schema extension (retained Agency replies). WM-09 concurred after reconciliation, job invoke-1789572196755-21452-a2d62af8; both owners agree the schema extension. Joe explicitly resumed implementation, validation, commit and independent Agency review.

Repair the demonstrated interpretation-job catch-all that recasts typed machine faults and untyped exceptions as content refusals. Preserve original causes, typed classifications, rejected receipt bytes, sources and timing. Keep budget expiry incomplete-recoverable and actual content refusals environmental holds. Replace runner namespace-wide hold classification with explicit content membership; unknown and invariant kinds remain machine failures. Include job-already-dispatched and attempt-identity-mismatch controls.

The closed interpretation failure vocabulary needs one explicit :interpretation/machine-failure member to retain an honest failure record and its source companions. WM-08 owner agreed this structural extension. Keep original exception class/message/data in retained diagnostics and outer cause. An interpreter-reported machine failure must not enable environmental discharge or successful construction.

Files: src/futon2/aif/interpretation_job.clj, interpretation_evidence.clj, the interpretation mapping in full_loop_runner.clj; affected tests and this receipt directory. No predecessor-history repair, serving activation, click, F4 ruling or checkbox claim. Independent reviewer proposed: claude-3; review not commissioned yet.

Validation: meaningful runner-level positive/negative controls inside hermetic-repair-fixture/with-hermetic-stores and hermetic traces; relevant namespaces individually, clj-kondo and futon4/dev/check-parens.el. Verify retained failure records and diagnostics as well as final repair class. No production-store probes.

This removes the review finding in wm-08-09-review-1/REVIEW-claude-3.md; it is not ordinary find evidence. Next closure evidence still requires real source-bound retrieval/interpretations, independent F2 expectations, honestly scoped F4 designation, serving use and downstream handoff.
