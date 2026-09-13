# Independent lifecycle reconciliation review

Reviewed 7309d81c and receipts 416e9b20; both source pins match actual bytes. Retained raw gates show 3 tests/15 assertions, zero failures/errors, kondo clean and actual parens OK; induced failure exits 1. No passing suite was rerun. The independently retained review of lead 232df039 closes that earlier source-review dependency.

The current reconciler correctly rejects tested nonterminal/trace/count mismatches, but is not ready as complete drain evidence. An executed isolated control omits queue job-ids, execution job-ids, delivery records and deferred order/records; it still returns complete-census, zero-in-flight=true and a nil deferred ID list. Nil collections are silently treated as empty. Full stdout and script retained in lead-empty-census-control.*; clojure -M script returned 0 with both erroneous-result assertions, script kondo clean and explicit parens OK.

Further source findings: duplicate queue/execution IDs are collapsed by set construction, arbitrary values are string-coerced; deferred schema alone is checked without generation/order/payload consistency; UTF-8 decoding replaces malformed sequences instead of refusing. Source pins authenticate supplied bytes but there is no separately bound complete accepted-job universe/coverage witness, nor isolated/production scope distinction. Simultaneously omitted jobs and delivery rows can therefore look complete.

Next packet must require all source fields and strict types/unique IDs, strict UTF-8, validated deferred projection/generation, explicit scope/provenance, and an independently configured complete-census authority bound to the exact source/generation/job universe. A candidate completeness boolean is not enough. Until an actual production authority exists, production reconciliation must refuse while isolated fixtures demonstrate schema and joins. Preserve restart-authorized=false. Do not normalize missing evidence to zero.

The live-file census remains non-atomic discovery only, not new production drain evidence or serving retention. No HTTP integration, local listener, deployment or first-installation readiness follows.
