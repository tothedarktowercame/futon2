# C593 — F10 seeded-C typed absence

This is C591 slice 3 only. When disposition risk is enabled, the ranking
boundary classifies the seeded-C input before it scores any included policy
(`src/futon2/aif/efe.clj:973-992`). A missing or nil input produces one typed
`:absent` record naming `:seeded-c-not-supplied`, whether the key was present,
and whether a candidate made the input required
(`src/futon2/aif/disposition_risk.clj:6-27`).

When that absent input is required, the ranking is empty and carries the
refusal record as `:disposition-risk-events`; no candidate is scored and no
preference mass appears in the record (`src/futon2/aif/efe.clj:976-992`). The
layer remains default-disabled, and seeded-C no longer has an implicit default
at the scorer boundary (`src/futon2/aif/efe.clj:623-627`).

C591 t2 exercises both a missing key and a present nil value. Both produce no
ranking, exactly one typed record, and no `:value` field
(`test/futon2/aif/disposition_risk_test.clj:33-49`). The positive disposition
tests now provide the ruled seed explicitly
(`test/futon2/aif/disposition_risk_test.clj:18-31`). This implements the slice
specified at `holes/labs/wm-contract/C591-C-as-calculation-proposal.md:76-77`
and `holes/labs/wm-contract/C591-C-as-calculation-proposal.md:90-99`.

Validation: the three focused namespaces ran 41 tests / 174 assertions / 0
failures / 0 errors. clj-kondo reported 0 errors / 0 warnings on the new
boundary and its test; check-parens reported OK on all three touched Clojure
files. No live tick, run lock, registry choice/decision, or publish
regeneration was involved.
