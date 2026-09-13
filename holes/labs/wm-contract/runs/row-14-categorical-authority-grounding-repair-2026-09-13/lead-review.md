# Independent review of categorical grounding repair

Reviewed source b5419475caaee625ed0d181d13e1d338a906ccc9 and receipt commit 3b4bf1ee. All four source-pins.sha256 entries match current bytes. Retained raw gates show 6 tests/32 assertions, zero failures/errors, kondo zero warnings/errors and actual explicit-path parens OK. These passing checks were not rerun.

The previous payload, authoritative timing, required identity and envelope limitation repairs hold at the injected resolver boundary. Close attachment remains blocked by two independently executed counterexamples:

1. `subject` and `subject-digest` bind candidate claim references but omit resolved claim digests. Replacing the externally resolved bytes under the same reference, with the same categorical assertion, leaves old reviewer acceptance usable. Retaining the new digest in the output does not make that digest reviewed.
2. `validate-evidence!` does not validate evidence-claim scope or provenance. An explicitly test-scoped claim qualifies inside a production-scoped envelope when observer/reviewer records are production-scoped.

`lead-counterexamples.clj` uses only isolated fixture files. Command: `clojure -M:test holes/labs/wm-contract/runs/row-14-categorical-authority-grounding-repair-2026-09-13/lead-counterexamples.clj`. Exit 0; two assertions establish the erroneous qualification. Exact stdout and empty stderr are retained. New review script kondo returned zero warnings/errors; explicit arxana-check-parens-cli returned OK.

Next packet must resolve and freeze the complete review subject before matching acceptance: exact claim digests, observer origin, independent context identity/timing, authority scope/provenance and acquisition limitations. Require explicit evidence scope/provenance matching that context. Add stale-review same-reference replacement and cross-scope controls, plus positive acceptance of the exact newly resolved subject. Preserve semantic separation between reviewed annotations and physical ground truth. No live labels, attachment, counts or admission.
