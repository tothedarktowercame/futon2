# Registry example: executed countercheck and hold

Predeclaration/code commit: b9b3047c, before executing record_check.py.

Four constructed cases (clean/buggy × dense/sparse), ten sampled indices each: **40/40 confidently true verdicts from the complete retained result**, independent of the spot-check. Both full rerun and record verification decide the constructed ground truth. This claim assumes an actual executed complete record, retained here; matching hashes alone do not prove execution. The Python check validates changed source pins refuse.

With the decisive record removed, a passing random draw under the sparse-bug hypothesis gives P(clean | pass)=0.5/(0.5+0.5×0.9)=0.526315789474. It does **not** cross the preregistered 0.95 threshold. A failed draw diagnoses a bug in this constructed test domain.

Consequently dispatch 2's proposed sparse-bug exposure did not describe normal verification of a complete deterministic registry record. zai-7 confirmed that the intended alternative needs an explicitly **partial record**, with inadequate coverage accepted in error at rate e; adequacy rejection must trigger full rerun or refusal. That is a failure to enforce completeness, not valid-registry verification. A truthful observation alphabet, preferences, cost and fallback model must be specified before computing G for that scenario.

**Held at zai-7's instruction** (job invoke-1789490848397-21057-f88d8ff6): zai-8 is considering switching the entire example to the Emacs buffer cleaner. No full A/B or fuel tables were computed, no adequacy-error figures invented, and no blog page changed. Resume only after the domain decision. The countercheck is complete; the original full-scoring deliverable remains unfinished.

Source: `holes/labs/wm-contract/NOTE-test-registry.md:20–41` specifies retained run results and the record/adequacy/selective-reexecution discipline. Reproduce from futon2 with `python3 holes/labs/wm-contract/runs/cascade-worked-example-2-2026-09-15/record_check.py`. Exact constructed records, likelihoods and sampled outcomes are in results.json; execution output and source/record hashes are alongside.
