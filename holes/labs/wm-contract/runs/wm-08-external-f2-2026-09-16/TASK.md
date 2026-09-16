# Agreed next task: independently supplied F2 expectation validation

Joe asked codex-9 to settle an agreed next step, advocating a whole WM closure if feasible, otherwise asking an item owner for a task. WM-08 owner zai-16 agreed this task after source/law assessment and the corrections retained alongside it. Implementation has not started.

Author: codex-9. Independent reviewer: claude-3. Checklist: WM-08 C2/C4/C5. Files: find_receipt.clj (or a small sibling namespace) and relevant tests. Preserve codex-7 ownership of receipt_construction.clj.

Implement a required external expectation artifact and distinct validation function. Bind the artifact to independently captured request/occurrence context, target, source digests, repository digest and pinned-at, available before interpretation/finder output. Do not bind a purported pre-output expectation to a completed interpretation-record hash. Each expected F2 row contains clause-kind, acknowledged-clause (text AND lines), route and as-of. Preserve byte/source validation and existing F1/F3/guard/F4 checks.

The external four-field check must compare emitted receipts to required external expected content. Controls: correct independent input accepted; self-supplied author rejected (declared-role control, not authenticated identity); mismatched source/occurrence rejected; unexpected selected pattern rejected; valid but deliberately wrong expected clause rejected. Existing find tests remain green. Run relevant namespaces, clj-kondo, check-parens; commit and obtain independent review.

The current compiled context reads pinned bytes separately from FindResult, but it also supplies the receipt's four comparison fields. This machinery is not credited as the independent external F2 check. The new validator must not have an optional artifact path that silently skips its requirement.

Credit is input/validator machinery only. Actual caller integration is separately owned by codex-7 through codex-28; no edits to that file in this assignment. Neither an unwired validator nor a hermetic run establishes ordinary serving use. F4 qualification/designation authority, current-code conformance, actual source-bound ordinary use and useful downstream consumption still prevent whole WM-08 closure. No serving operation, new occurrence or checkbox change is commissioned here.
