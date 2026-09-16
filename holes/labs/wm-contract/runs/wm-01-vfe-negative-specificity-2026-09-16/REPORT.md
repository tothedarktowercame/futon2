# WM-01 weakened-positive specificity repair

Author codex-7. Implements WM-01-vfe-negative-specificity-1 and F1 of review 68c2cbc1. Independent review is deferred until the already commissioned WM-09 final review concludes. This is executable gate correctness, not WM-01 closure, R4 admission or a new normalization claim.

The weakened-positive mode now first validates the unmodified canonical receipt, expected fixture and positive witness elaboration. Only then does it require a genuinely changed source, successful elaboration of that weakening, and an overridden receipt report with pass? false and exactly [:positive-source-drift]. Extra or unrelated failures, or a passing override, cannot count as detecting the intended weakening.

Structured status distinguishes baseline-failed, baseline-elaboration-failed, mutation-unchanged, mutation-elaboration-failed, unexpected-mutation-result, and expected-source-drift-detected. Exit 1 indicates baseline/setup failure, exit 2 an unexpected mutation result, and exit 0 only the intended detection. Positive, value/type negatives and unrelated-edit modes retain their behavior. The wrapper can now be required without running main; direct bb invocation remains exercised.

Focused tests use the actual retained receipt, fixture, source slices and shared validator, stubbing only expensive toolchain/elaboration process boundaries except deliberate report overrides. They cover valid baseline and actual weakening; stale receipt source hash; unrelated fixture hash failure; current source drift using an in-memory read override; unchanged mutation; non-elaborating baseline/mutation; unexpected/additional validation failures; and an override that passes. Canonical files are never corrupted. Four tests / 20 assertions pass.

Separately executed all five REAL wrapper modes serially: positive, negative-value, negative-type, negative-weakened-positive, unrelated-positive-edit. All exit 0. The weakened-mode log retains passing unmodified baseline and exactly the expected mutation source drift. No lake build or cache regeneration was run. Lint and check-parens pass; GATES.json contains exact commands/exits, with raw logs beside it.

IMMUTABLE-PINS.json and IMMUTABLE-VERIFIED.json record unchanged canonical receipt/fixture and Lean witness/negative/dependency source hashes, verified before and after runs. The shared validator is unchanged and pinned in SOURCE-PINS.json. No Lean, canonical receipt, schema, registry, known-stale, WM-09, DAG/checklist, runtime, serving or publication changes.
