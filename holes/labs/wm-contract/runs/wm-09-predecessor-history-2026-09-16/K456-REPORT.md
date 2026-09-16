# WM-09 predecessor repair: K4/K5/K6 reconciliation

Author: codex-7. Implementation returned for independent review, not accepted delivery.

This supplements K23-REPORT.md and applies the packet through revision 4 and K6. Only predecessor source, its tests and this receipt directory changed.

## Behavior

- K4: a producer invalid-checkpoint-cell construction sorry remains a candidate. Latest matching rejected construction refuses with construction path, SHA-256, original sorry/cell errors and target evidence. Controls exercise both absence and presence of an older valid predecessor; neither initializes or falls back.
- K5: coherent different-target checkpoint evidence permits relevance exclusion without requiring a modern carrier. Envelope, attempt identity, ordering, all available target identities and present integrity bindings are checked. Exclusion records retain paths/digests and identity sources. This is producer-recorded relevance evidence, not cryptographic authentication or predecessor admission. Matching old construction still refuses current requirements. The same distinction applies to rejected construction.
- K6: discovery compares keyword targets with their exact colon/namespace-bearing string rendering. Original field path/type/value evidence is retained, including requested target. Strings remain literal; namespace mismatch, colon removal and unsupported types refuse. No records or admission identities are rewritten.
- K2/K3 remain distinct positive non-construction exclusions, with recorded provenance. A missing construction file is not such an exclusion.

## Executed gates

K456-GATES.json retains commands and exit statuses. clj-kondo and check-parens pass. Isolated receipt-construction tests pass: 16 tests, 190 assertions. Focused interpretation-job caller tests pass: 2 tests, 16 assertions. The retained hermetic classification script passes: typed discovery refusal remains environmental-hold; injected untyped code fault remains machine-failure, with no legacy constructor calls.

Negative comparison: the current tests against isolated source c664f402 fail with 11 failures and 2 errors (16 tests, 167 assertions), exit 1. This is expected rejection of the pre-K4/K5/K6 implementation; see k456-before-controls.log and K456-NEGATIVE.json. The full caller namespace was previously run for the initial repair; this follow-up reruns focused callers, not that full namespace.

All nine original diagnostic artifacts are byte-identical to 7e738d53; K456-SOURCE-PINS.json lists them and hashes the reviewed source/test/packet inputs.

## Limits

Unsupported actual matching history remains blocked. No root narrowing, reset, migration, recovery mechanism, serving action or runtime classifier change was performed. The earlier reviewer census is historical diagnostic evidence only. New closes alone do not establish recovery from an older history blockage. No DAG/checklist edits. Independent acceptance remains owed on this exact implementation.
