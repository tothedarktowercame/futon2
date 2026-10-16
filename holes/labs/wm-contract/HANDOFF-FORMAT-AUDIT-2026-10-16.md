# Live handoff format audit — 2026-10-16

Scope: external producer/consumer boundaries reachable from the production
runner.  The audit compares what the producer is shown with what the consumer
actually validates.  Transport handling is centralized in
`futon2.aif.handoff-contract/exchange`; this audit is about the semantic format
above that transport.

| Handoff | Producer-visible contract | Consumer | Finding |
|---|---|---|---|
| selected target → `wm-author` reading | Python `acceptance_contract()` embedded in the immutable request and prompt | `session_turn_analysis.validate` | **Repaired.** The contract previously omitted exact fragment/source correspondence and several array/text constraints. It now publishes fragment, rejection, reusable-cue and R-node rules beside the validator. |
| selected policy → reading answer | `selected-reading/acceptance-contract` names the exact owed occurrence set, verdict vocabulary, reason, source digest and author | `selected-reading/validate` | Aligned. Exact occurrence identity and issued library pins are both stated and enforced. |
| unmet want → interpretation answer | `want-interpretation/acceptance-contract` plus its explicit EDN reply grammar | `want-interpretation/validate-response` | Aligned at the boundary. Source pins and semantic gates are named; response parsing requires one schema-tagged EDN form. |
| legacy action → interpretation evidence | `interpretation-evidence/acceptance-contract` | `interpretation-evidence` validators | Aligned but intentionally coarse: the contract names the four versioned semantic gates and exact source/attempt binding. This route is retired from reading-family selection. |
| reviewer → standing decision | `limb-evidence/standing-decision-contract` printed by both completion prompts | `limb-evidence` readback | Aligned. Exact target, distinct reviewer/author roles and record shape share one contract constructor. |

## Format variants exercised

The common exchange accepts a direct map, an Agency terminal `:result` JSON
string, the legacy terminal `:text` JSON string, and typed no-contact or
non-terminal results.  It never consumes `:result-summary`.  The selected
target tests also exercise the live template round trip (JSON strings become
keyword-bearing Clojure data before the common contract is printed).

## Regression rule

The authoritative selected-target validator and its advertised contract live
in the same Python module.  A regression test now pins every validator
boundary category, and the Clojure runner test proves that those fields reach
the actual `wm-author` request.  A future validator rule that introduces a new
boundary category must update this contract test; prose-only prompt changes do
not satisfy it.

No new click was run during this audit.
