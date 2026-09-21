# Guard observation locators by production class

Author: codex-5. Review owner: claude-12.
Finding: repair-occ-7737547f116c5976ba54e7cad66e32de8f1fb7c41bf88f36e9a9a8a5783a5be6.
Request: invoke-1789961383253-22840-39530df4.

The decision gate checks every present/absent guard token in every posterior
candidate, including unselected candidates. Invalid locators throw the existing
inadmissible-decision exception with reason `:missing-observation-locators`,
sorted missing tokens, and per-token typed diagnostics. Non-map values are
`:invalid-observation-locator`; unknown/missing classes are
`:no-mechanical-check`; missing/nonstring/blank required fields are `:no-locator`
with the field vector. The pooled-nil action refusal is unchanged.

Requirements were transcribed from the production handlers' locator-refusal
calls in src/futon2/aif/observation_checks.clj:

| Class | Handler | Required nonblank string fields |
| --- | --- | --- |
| C3 | check-path-exists | repo, sha, path |
| C4 | check-decl-in-file | repo, sha, path, decl |
| C5 | check-registry-entry | repo, sha, bundle-path, entry |
| C6 | check-witness-reference | repo, sha, path |

C6's embedded witness contents are a later observation check, not additional
outer locator fields. Extra fields are allowed, matching the handlers. No
observation handler or reference resolver was changed, and the gate performs
no git/observation I/O. Admitting a locator does not assert its artifact exists.

Tests build decisions through the real selection function. Every class has a
passing locator and an absent/nil/empty/whitespace/nonstring case for every
required field. Each malformed case is checked against the real production
handler with no ports stubbed. Valid locators also reach the real handlers:
C3/C4 can observe files in the current checkout; C5's missing bundle reaches
`:bundle-not-found`, and C6's missing witness is observed false. Those latter
outcomes explicitly separate valid locators from positive artifact evidence.
The gate admits all four valid locators. The exact rejected implementation's
counterexamples are pinned: C4 without decl refuses; generic C5 path fields
refuse; C5 with bundle-path/entry and no path passes. Both present and absent
guard tokens, nil/empty/unsupported locators, extra fields, absent locator maps,
and an unselected invalid candidate are covered.

Validation uses fresh tooling JVMs. The intended evidence roles are pre-commit
validation followed by post-commit registered warrants, with hashes compared.
The execution receipt records the actual outcomes, never an inferred pass.

Serving JVM: committing this change does NOT load it. No serving JVM was
reloaded by this packet. Whoever ticks next must reload
futon2.aif.decision-gate from the canonical checkout after review.
